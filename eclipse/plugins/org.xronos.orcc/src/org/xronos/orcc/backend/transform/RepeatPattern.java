/*
 * Copyright (c) 2012, Ecole Polytechnique Fédérale de Lausanne
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 *   * Redistributions of source code must retain the above copyright notice,
 *     this list of conditions and the following disclaimer.
 *   * Redistributions in binary form must reproduce the above copyright notice,
 *     this list of conditions and the following disclaimer in the documentation
 *     and/or other materials provided with the distribution.
 *   * Neither the name of the Ecole Polytechnique Fédérale de Lausanne nor the names of its
 *     contributors may be used to endorse or promote products derived from this
 *     software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY
 * WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 */
package org.xronos.orcc.backend.transform;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sf.orcc.df.Action;
import net.sf.orcc.df.Actor;
import net.sf.orcc.df.Pattern;
import net.sf.orcc.df.Port;
import net.sf.orcc.df.util.DfVisitor;
import net.sf.orcc.ir.BlockBasic;
import net.sf.orcc.ir.BlockIf;
import net.sf.orcc.ir.BlockWhile;
import net.sf.orcc.ir.ExprInt;
import net.sf.orcc.ir.ExprVar;
import net.sf.orcc.ir.Expression;
import net.sf.orcc.ir.InstLoad;
import net.sf.orcc.ir.InstStore;
import net.sf.orcc.ir.IrFactory;
import net.sf.orcc.ir.OpBinary;
import net.sf.orcc.ir.Procedure;
import net.sf.orcc.ir.Type;
import net.sf.orcc.ir.Use;
import net.sf.orcc.ir.Var;
import net.sf.orcc.ir.util.AbstractIrVisitor;
import net.sf.orcc.ir.util.IrUtil;
import net.sf.orcc.util.util.EcoreHelper;

import org.xronos.orcc.design.ResourceCache;
import org.xronos.orcc.design.util.XronosIrUtil;
import org.xronos.orcc.design.util.XronosMathUtil;
import org.xronos.orcc.design.visitors.io.CircularBuffer;
import org.xronos.orcc.ir.InstPortStatus;
import org.xronos.orcc.ir.InstPortWrite;
import org.xronos.orcc.ir.XronosIrFactory;

/**
 * 
 * This class visits the actor and it finds the biggest input and output repeat
 * index on the inputPattern and outputPattern. Then it creates a single state
 * variable list for the I/O pattern and it replace it for each action. After
 * that it creates the necessary stores for the pinRead component and loads for
 * the pinStore. This transformation does not change the actors scheduler.
 * 
 * @author Endri Bezati
 * 
 */
public class RepeatPattern extends DfVisitor<Void> {

	private class RepeatFinder extends DfVisitor<Void> {

		@Override
		public Void caseActor(Actor actor) {
			this.actor = actor;
			/** Visit all actions **/
			for (Action action : actor.getActions()) {
				doSwitch(action);
			}
			return null;
		}

		@Override
		public Void caseAction(Action action) {
			for (Port port : action.getInputPattern().getPorts()) {
				int numTokens = action.getInputPattern().getNumTokensMap()
						.get(port);
				if (numTokens > 1) {
					if (portMaxRepeatSize.containsKey(port)) {
						if (portMaxRepeatSize.get(port) < numTokens) {
							portMaxRepeatSize.put(port, numTokens);
						}
					} else {
						portMaxRepeatSize.put(port, numTokens);
					}
				}
			}

			for (Port port : action.getOutputPattern().getPorts()) {
				int numTokens = action.getOutputPattern().getNumTokensMap()
						.get(port);
				if (numTokens > 1) {
					if (portMaxRepeatSize.containsKey(port)) {
						if (portMaxRepeatSize.get(port) < numTokens) {
							portMaxRepeatSize.put(port, numTokens);
						}
					} else {
						portMaxRepeatSize.put(port, numTokens);
					}
				}
			}
			return null;
		}

	}

	private class TransformCircularBufferLoadStore extends
			AbstractIrVisitor<Object> {

		public TransformCircularBufferLoadStore(Procedure procedure) {
			super(true);
			this.procedure = procedure;
		}

		@Override
		public Object caseInstLoad(InstLoad load) {
			Var sourceVar = load.getSource().getVariable();
			if (oldInputMap.containsKey(sourceVar)) {
				List<Expression> indexes = load.getIndexes();
				if (indexes.size() == 1) {
					Port port = oldInputMap.get(sourceVar);
					Var newSourceVar = circularBufferInputs.get(port)
							.getBuffer();

					Var cbTmpHead = circularBufferInputs.get(port).getTmpHead();
					int sizePowTwo = circularBufferInputs.get(port)
							.getSizePowTwo();

					ExprVar cbHeadExprVar = IrFactory.eINSTANCE
							.createExprVar(cbTmpHead);

					Expression index = indexes.get(0);

					Type exrpType = IrFactory.eINSTANCE.createTypeInt(32);
					Expression indexAdd;
					if (index instanceof ExprInt) {
						int value = ((ExprInt) index).getIntValue();
						if (value == 0) {
							indexAdd = cbHeadExprVar;
						} else {
							indexAdd = IrFactory.eINSTANCE.createExprBinary(
									cbHeadExprVar, OpBinary.PLUS, index,
									exrpType);
						}
					} else {
						indexAdd = IrFactory.eINSTANCE.createExprBinary(
								cbHeadExprVar, OpBinary.PLUS, index, exrpType);
					}

					ExprInt exprIntSize = IrFactory.eINSTANCE
							.createExprInt(sizePowTwo - 1);

					Expression indexAddAndSize = IrFactory.eINSTANCE
							.createExprBinary(indexAdd, OpBinary.BITAND,
									exprIntSize, exrpType);
					IrUtil.delete(indexes);
					indexes.add(indexAddAndSize);
					Use newUse = IrFactory.eINSTANCE.createUse(newSourceVar);
					load.setSource(newUse);
					// int loadIndex = load.getBlock().getInstructions()
					// .indexOf(load);
					// Expression eFalse = IrFactory.eINSTANCE
					// .createExprBool(false);
					// Var cbStart = circularBufferInputs.get(port).getStart();
					// InstStore storeStart =
					// IrFactory.eINSTANCE.createInstStore(
					// cbStart, eFalse);
					// load.getBlock().add(loadIndex, storeStart);

				}
			}
			return null;
		}

		@Override
		public Object caseInstStore(InstStore store) {
			Var targetVar = store.getTarget().getVariable();
			if (oldOutputMap.containsKey(targetVar)) {
				Port port = oldOutputMap.get(targetVar);
				Expression value = store.getValue();
				InstPortWrite insPortWrite = XronosIrFactory.eINSTANCE
						.createInstPortWrite();
				insPortWrite.setPort(port);
				insPortWrite.setValue(value);
				BlockBasic block = store.getBlock();
				int index = block.getInstructions().indexOf(store);
				block.add(index, insPortWrite);
				IrUtil.delete(store);

				BlockWhile blockWhile = EcoreHelper.getContainerOfType(block,
						BlockWhile.class);
				if (blockWhile != null) {
					Var portStatus = IrFactory.eINSTANCE.createVar(
							port.getType(), "portStatus_" + port.getName(),
							true, 0);
					procedure.getLocals().add(portStatus);
					InstPortStatus instPortStatus = XronosIrUtil
							.createInstPortStatus(portStatus, port);

					BlockBasic portStatusBlock = IrFactory.eINSTANCE
							.createBlockBasic();
					portStatusBlock.add(instPortStatus);

					Expression eFalse = IrFactory.eINSTANCE
							.createExprBool(false);
					Expression statusWhileCondition = XronosIrUtil
							.createExprBinaryNotEqual(portStatus, eFalse);

					BlockIf statusIf = XronosIrUtil.createBlockIf(
							statusWhileCondition, block);

					blockWhile.getBlocks().add(0, portStatusBlock);
					blockWhile.getBlocks().add(statusIf);
				}
			}
			return null;
		}
	}

	private RepeatFinder repeatFinder = new RepeatFinder();

	private Map<Var, Port> oldInputMap = new HashMap<Var, Port>();

	private Map<Var, Port> oldOutputMap = new HashMap<Var, Port>();

	private Map<Port, Integer> portMaxRepeatSize = new HashMap<Port, Integer>();

	private Map<Port, CircularBuffer> circularBufferInputs = new HashMap<Port, CircularBuffer>();

	private Map<Port, CircularBuffer> circularBufferOutputs = new HashMap<Port, CircularBuffer>();

	private ResourceCache resourceCache;

	public RepeatPattern(ResourceCache resourceCache) {
		super();
		this.resourceCache = resourceCache;
	}

	@Override
	public Void caseAction(Action action) {

		BlockBasic firstBodyBlock = action.getBody().getFirst();
		BlockBasic lastBodyBlock = action.getBody().getLast();

		/** InputPattern **/
		for (Port port : action.getInputPattern().getPorts()) {
			if (circularBufferInputs.get(port) != null) {
				// Create Load instruction head
				// Load(tmpHead, head)
				CircularBuffer circularBuffer = circularBufferInputs.get(port);
				circularBuffer.addToLocals(action.getBody());
				Var cbStart = circularBuffer.getStart();
				// Expression eFalse =
				// IrFactory.eINSTANCE.createExprBool(false);
				// InstStore storeStart = IrFactory.eINSTANCE.createInstStore(
				// cbStart, eFalse);
				// firstBodyBlock.add(0, storeStart);

				Var target = circularBuffer.getTmpHead();
				Var source = circularBuffer.getHead();
				InstLoad instLoad = IrFactory.eINSTANCE.createInstLoad(target,
						source);
				firstBodyBlock.add(0, instLoad);

				// Create Store instruction for head
				// Store(head, (tmpHead + numReads) & (size - 1))
				int numReads = action.getInputPattern().getNumTokens(port);
				Var pinReadVar = action.getInputPattern().getPortToVarMap()
						.get(port);
				Var cbHead = circularBuffer.getTmpHead();
				int sizePowTwo = circularBuffer.getSizePowTwo();

				ExprVar cbHeadExprVar = IrFactory.eINSTANCE
						.createExprVar(cbHead);

				ExprInt exprIntNumReads = IrFactory.eINSTANCE
						.createExprInt(numReads);

				Type exrpType = IrFactory.eINSTANCE.createTypeInt(32);
				Expression indexAdd = IrFactory.eINSTANCE
						.createExprBinary(cbHeadExprVar, OpBinary.PLUS,
								exprIntNumReads, exrpType);

				ExprInt exprIntSize = IrFactory.eINSTANCE
						.createExprInt(sizePowTwo - 1);

				Expression value = IrFactory.eINSTANCE.createExprBinary(
						indexAdd, OpBinary.BITAND, exprIntSize, exrpType);
				target = circularBuffer.getHead();
				InstStore instStore = IrFactory.eINSTANCE.createInstStore(
						target, value);
				int instIndex = lastBodyBlock.getInstructions().size() - 1;
				lastBodyBlock.add(instIndex, instStore);

				// Create Load instruction for count
				target = circularBuffer.getTmpCount();
				source = circularBuffer.getCount();
				InstLoad instLoadCount = IrFactory.eINSTANCE.createInstLoad(
						target, source);
				instIndex = lastBodyBlock.getInstructions().size() - 1;
				firstBodyBlock.add(0, instLoadCount);

				// Create Store instruction for count
				Var count = circularBuffer.getCount();
				Var tmpCount = circularBuffer.getTmpCount();
				ExprVar exprVarTmpCount = IrFactory.eINSTANCE
						.createExprVar(tmpCount);
				exprIntNumReads = IrFactory.eINSTANCE.createExprInt(numReads);
				Expression valueCount = IrFactory.eINSTANCE.createExprBinary(
						exprVarTmpCount, OpBinary.MINUS, exprIntNumReads,
						exrpType);
				InstStore instStoreCount = IrFactory.eINSTANCE.createInstStore(
						count, valueCount);
				instIndex = lastBodyBlock.getInstructions().size() - 1;
				lastBodyBlock.add(instIndex, instStoreCount);

				Expression eTrue = IrFactory.eINSTANCE.createExprBool(true);
				InstStore storeStart = IrFactory.eINSTANCE.createInstStore(
						cbStart, eTrue);
				instIndex = lastBodyBlock.getInstructions().size() - 1;
				lastBodyBlock.add(instIndex, storeStart);
				oldInputMap.put(pinReadVar, port);
			}
		}

		BlockBasic actionSchedulerFirstBlock = action.getScheduler().getFirst();

		/** Peek pattern **/
		for (Port port : action.getPeekPattern().getPorts()) {
			if (circularBufferInputs.get(port) != null) {
				// Add to locals the tmp variables
				CircularBuffer circularBuffer = circularBufferInputs.get(port);

				// Add Load to the firstBlock
				Var target = circularBuffer.getTmpHead();
				Var source = circularBuffer.getHead();

				// Put target to Locals
				action.getScheduler().getLocals().add(target);

				InstLoad instLoad = IrFactory.eINSTANCE.createInstLoad(target,
						source);
				actionSchedulerFirstBlock.add(0, instLoad);

				Var peekVar = action.getPeekPattern().getPortToVarMap()
						.get(port);
				oldInputMap.put(peekVar, port);
			}
		}

		/** OutputPattern **/
		for (Port port : action.getOutputPattern().getPorts()) {
			if (circularBufferOutputs.get(port) != null) {
				// Create Load instruction head
				// Load(tmpHead, head)
				CircularBuffer circularBuffer = circularBufferOutputs.get(port);
				circularBuffer.addToLocals(action.getBody());
				Var pinWriteVar = action.getOutputPattern().getPortToVarMap()
						.get(port);
				oldOutputMap.put(pinWriteVar, port);
			}
		}

		// Now change the Load/Store of an action
		TransformCircularBufferLoadStore circularBufferLoadStore = new TransformCircularBufferLoadStore(
				action.getBody());
		circularBufferLoadStore.doSwitch(action.getBody().getBlocks());
		circularBufferLoadStore = new TransformCircularBufferLoadStore(
				action.getScheduler());
		circularBufferLoadStore.doSwitch(action.getScheduler().getBlocks());
		return null;
	}

	@Override
	public Void caseActor(Actor actor) {
		this.actor = actor;
		repeatFinder.doSwitch(actor);

		// Create the Buffers
		for (Port port : actor.getInputs()) {
			if (portMaxRepeatSize.containsKey(port)) {
				int size = portMaxRepeatSize.get(port);
				if (size > 1) {
					Type type = port.getType();
					// Find the nearest Power of two
					int sizePowTwo = XronosMathUtil.nearestPowTwo(size);
					Type typeList = IrFactory.eINSTANCE.createTypeList(
							sizePowTwo, type);
					Var buffer = IrFactory.eINSTANCE.createVar(typeList,
							"circularBufferIn_" + port.getName(), true, 0);
					CircularBuffer circularBuffer = new CircularBuffer(port,
							buffer, size);
					circularBuffer.addToStateVars(actor, false);
					circularBufferInputs.put(port, circularBuffer);
				} else {
					circularBufferInputs.put(port, null);
				}
			}
		}
		for (Port port : actor.getOutputs()) {
			if (portMaxRepeatSize.containsKey(port)) {
				int size = portMaxRepeatSize.get(port);
				if (size > 1) {
					Type type = port.getType();
					// Find the nearest Power of two
					int sizePowTwo = XronosMathUtil.nearestPowTwo(size);
					Type typeList = IrFactory.eINSTANCE.createTypeList(
							sizePowTwo, type);
					Var buffer = IrFactory.eINSTANCE.createVar(typeList,
							"circularBufferOut_" + port.getName(), true, 0);
					CircularBuffer circularBuffer = new CircularBuffer(port,
							buffer, size);
					circularBuffer.addToStateVars(actor, true);
					circularBufferOutputs.put(port, circularBuffer);
				} else {
					circularBufferOutputs.put(port, null);
				}
			}
		}

		// Visit all actions
		for (Action action : actor.getActions()) {
			doSwitch(action);
		}
		resourceCache.setActorInputCircularBuffer(actor, circularBufferInputs);
		resourceCache
				.setActorOutputCircularBuffer(actor, circularBufferOutputs);
		return null;
	}

	@Override
	public Void casePattern(Pattern pattern) {
		return null;
	}

}
