/* 
 * XRONOS, High Level Synthesis of Streaming Applications
 * 
 * Copyright (C) 2014 EPFL SCI STI MM
 *
 * This file is part of XRONOS.
 *
 * XRONOS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * XRONOS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with XRONOS.  If not, see <http://www.gnu.org/licenses/>.
 * 
 * Additional permission under GNU GPL version 3 section 7
 * 
 * If you modify this Program, or any covered work, by linking or 
 * combining it with Eclipse libraries (or a modified version of that 
 * library), containing parts covered by the terms of EPL,
 * the licensors of this Program grant you additional permission to convey 
 * the resulting work. {Corresponding Source for a non-source form of such 
 * a combination shall include the source code for the parts of Eclipse 
 * libraries used as well as that of the  covered work.}
 */

package org.xronos.orcc.backend.transform;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sf.orcc.df.Action;
import net.sf.orcc.df.Actor;
import net.sf.orcc.df.DfFactory;
import net.sf.orcc.df.Pattern;
import net.sf.orcc.df.Port;
import net.sf.orcc.df.State;
import net.sf.orcc.df.util.DfVisitor;
import net.sf.orcc.ir.BlockBasic;
import net.sf.orcc.ir.BlockIf;
import net.sf.orcc.ir.Def;
import net.sf.orcc.ir.ExprBinary;
import net.sf.orcc.ir.ExprInt;
import net.sf.orcc.ir.ExprVar;
import net.sf.orcc.ir.Expression;
import net.sf.orcc.ir.InstAssign;
import net.sf.orcc.ir.InstLoad;
import net.sf.orcc.ir.InstReturn;
import net.sf.orcc.ir.InstStore;
import net.sf.orcc.ir.IrFactory;
import net.sf.orcc.ir.OpBinary;
import net.sf.orcc.ir.Procedure;
import net.sf.orcc.ir.Type;
import net.sf.orcc.ir.Use;
import net.sf.orcc.ir.Var;
import net.sf.orcc.ir.util.AbstractIrVisitor;
import net.sf.orcc.ir.util.IrUtil;

import org.xronos.orcc.design.ResourceCache;
import org.xronos.orcc.design.util.XronosIrUtil;
import org.xronos.orcc.design.util.XronosMathUtil;
import org.xronos.orcc.design.visitors.io.CircularBuffer;
import org.xronos.orcc.ir.BlockMutex;
import org.xronos.orcc.ir.InstPortRead;
import org.xronos.orcc.ir.InstPortStatus;
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
public class InputRepeatPattern extends DfVisitor<Void> {

	private class RepeatFinder extends DfVisitor<Void> {

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

		@Override
		public Void caseActor(Actor actor) {
			this.actor = actor;
			/** Visit all actions **/
			for (Action action : actor.getActions()) {
				doSwitch(action);
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
				}
			}
			return null;
		}

		@Override
		public Object caseInstStore(InstStore store) {
			Var targetVar = store.getTarget().getVariable();
			if (oldInputMap.containsKey(targetVar)) {
				List<Expression> indexes = store.getIndexes();
				if (indexes.size() == 1) {
					Port port = oldInputMap.get(targetVar);
					Var newTargetVar = circularBufferInputs.get(port)
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
					Def newDef = IrFactory.eINSTANCE.createDef(newTargetVar);
					store.setTarget(newDef);
				}

			}
			return null;
		}

	}

	private RepeatFinder repeatFinder = new RepeatFinder();

	private Map<Var, Port> oldInputMap = new HashMap<Var, Port>();

	private Map<Port, Integer> portMaxRepeatSize = new HashMap<Port, Integer>();

	private Map<Port, CircularBuffer> circularBufferInputs = new HashMap<Port, CircularBuffer>();

	private ResourceCache resourceCache;

	private IrFactory irFactory = IrFactory.eINSTANCE;

	public InputRepeatPattern(ResourceCache resourceCache) {
		super();
		this.resourceCache = resourceCache;
	}

	private void addFillBufferAction(Actor actor) {
		Action fillBuffers = createFillBufferAction(actor);
		// Create the procedure
		if (actor.hasFsm()) {
			if (fillBuffers != null) {
				actor.getActions().add(fillBuffers);
			}
			for (State state : actor.getFsm().getStates()) {
				if (fillBuffers != null) {
					actor.getFsm().addTransition(state, fillBuffers, state);
				}
			}
		} else {
			if (fillBuffers != null) {
				actor.getActions().add(fillBuffers);
				actor.getActionsOutsideFsm().add(fillBuffers);
			}
		}

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

		// Now change the Loads of an action
		TransformCircularBufferLoadStore circularBufferLoadStore = new TransformCircularBufferLoadStore(
				action.getBody());
		circularBufferLoadStore.doSwitch(action.getBody().getBlocks());

		// Now change the Loads of an action schedueler
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
					circularBuffer.addToStateVars(actor, true);
					circularBufferInputs.put(port, circularBuffer);
				} else {
					circularBufferInputs.put(port, null);
				}
			}
		}

		// Visit all actions
		for (Action action : actor.getActions()) {
			doSwitch(action);
		}
		resourceCache.setActorInputCircularBuffer(actor, circularBufferInputs);
		addFillBufferAction(actor);

		return null;
	}

	@Override
	public Void casePattern(Pattern pattern) {
		return null;
	}

	private Action createFillBufferAction(Actor actor) {
		boolean hasRepeats = false;

		for (Port port : actor.getInputs()) {
			if (circularBufferInputs.containsKey(port)) {
				hasRepeats = true;
				break;
			}
		}

		if (hasRepeats) {
			String name = actor.getSimpleName() + "_fillBuffers";
			Type typeVoid = IrFactory.eINSTANCE.createTypeVoid();
			// Create the Procedure
			Procedure body = irFactory.createProcedure(name, 0, typeVoid);

			BlockBasic statusAndRequestBlock = irFactory.createBlockBasic();
			// Fill statusAndRequestBlock
			for (Port port : actor.getInputs()) {
				if (circularBufferInputs.containsKey(port)) {
					Type typeBool = irFactory.createTypeBool();
					Var portStatusVar = irFactory.createVar(typeBool,
							"portStatus_" + port.getName(), true, 0);
					body.getLocals().add(portStatusVar);
					InstPortStatus portStatus = XronosIrUtil
							.createInstPortStatus(portStatusVar, port);

					Var cbTmpRequestSize = circularBufferInputs.get(port)
							.getTmpRequestSize();
					Var cbRequestSize = circularBufferInputs.get(port)
							.getRequestSize();
					body.getLocals().add(cbTmpRequestSize);

					InstLoad requestLoad = irFactory.createInstLoad(
							cbTmpRequestSize, cbRequestSize);

					Var cbTmpCount = circularBufferInputs.get(port)
							.getTmpCount();
					Var cbCount = circularBufferInputs.get(port).getCount();
					body.getLocals().add(cbTmpCount);

					InstLoad countLoad = irFactory.createInstLoad(cbTmpCount,
							cbCount);

					statusAndRequestBlock.add(portStatus);
					statusAndRequestBlock.add(requestLoad);
					statusAndRequestBlock.add(countLoad);
				}
			}

			body.getBlocks().add(statusAndRequestBlock);

			List<BlockIf> mutexIfs = new ArrayList<BlockIf>();
			BlockBasic readIfConditionBlock = irFactory.createBlockBasic();

			for (Port port : actor.getInputs()) {
				if (circularBufferInputs.containsKey(port)) {
					CircularBuffer circularBuffer = circularBufferInputs
							.get(port);
					Var portStatusVar = body.getLocal("portStatus_"
							+ port.getName());
					Expression portStatusExprVar = irFactory
							.createExprVar(portStatusVar);

					Var cbTmpCount = circularBuffer.getTmpCount();
					Var cbTmpRequestSize = circularBuffer.getTmpRequestSize();

					Expression countLessThanRequest = XronosIrUtil
							.createExprBinaryLessThan(cbTmpCount,
									cbTmpRequestSize);

					Expression readIfConditionExpr = XronosIrUtil
							.createExprBinaryLogicAnd(portStatusExprVar,
									countLessThanRequest);
					Type typeBool = irFactory.createTypeBool();
					Var readIfConditionVar = irFactory.createVar(typeBool,
							"readIf_" + port.getName(), true, 0);
					body.getLocals().add(readIfConditionVar);

					InstAssign assignReadIf = irFactory.createInstAssign(
							readIfConditionVar, readIfConditionExpr);

					readIfConditionBlock.add(assignReadIf);

					// Create thenblock
					BlockBasic thenBlock = irFactory.createBlockBasic();
					Var cbHead = circularBuffer.getHead();
					Var cbTmpHead = circularBuffer.getTmpHead();
					InstLoad headLoad = irFactory.createInstLoad(cbTmpHead,
							cbHead);

					// PortRead(token, port)
					Var token = irFactory.createVar(port.getType(), "token_"
							+ port.getName(), true, 0);
					body.getLocals().add(token);
					InstPortRead portRead = XronosIrUtil.createInstPortRead(
							token, port, true);

					// Store( circularBuffer[cbHead + cbCount &
					// (cbSizePowTwo-1),
					// token)
					Integer cbSize = circularBuffer.getSizePowTwo();
					ExprInt eiSizeMinusOne = irFactory
							.createExprInt(cbSize - 1);

					Type typeInt32 = irFactory.createTypeInt(32);
					ExprBinary ebHeadPlusCount = XronosIrUtil
							.createExprBinaryPlus(cbTmpHead, cbTmpCount,
									typeInt32);

					Expression cbIndex = irFactory.createExprBinary(
							ebHeadPlusCount, OpBinary.LOGIC_AND,
							eiSizeMinusOne, typeInt32);

					Var buffer = circularBuffer.getBuffer();

					InstStore bufferStore = irFactory.createInstStore(buffer,
							Arrays.asList(cbIndex), token);

					Expression countPlusOne = XronosIrUtil
							.createExprBinaryPlus(cbTmpCount, 1, typeInt32);

					InstAssign countAssign = irFactory.createInstAssign(
							cbTmpCount, countPlusOne);
					Var cbCount = circularBuffer.getCount();
					InstStore countStore = irFactory.createInstStore(cbCount,
							cbTmpCount);

					thenBlock.add(headLoad);
					thenBlock.add(portRead);
					thenBlock.add(bufferStore);
					thenBlock.add(countAssign);
					thenBlock.add(countStore);

					// Create the statusIf
					Expression readIfCondition = irFactory
							.createExprVar(readIfConditionVar);

					BlockIf readIf = XronosIrUtil.createBlockIf(
							readIfCondition, thenBlock);
					mutexIfs.add(readIf);
				}
			}
			BlockMutex blockMutex = XronosIrFactory.eINSTANCE
					.createBlockMutex();
			blockMutex.getBlocks().addAll(mutexIfs);

			body.getBlocks().add(readIfConditionBlock);
			body.getBlocks().add(blockMutex);

			/** Create Return Block **/
			BlockBasic returnBlock = IrFactory.eINSTANCE.createBlockBasic();
			InstReturn instReturn = IrFactory.eINSTANCE.createInstReturn(null);
			returnBlock.add(instReturn);
			body.getBlocks().add(returnBlock);
			Type returnType = IrFactory.eINSTANCE.createTypeVoid();
			body.setReturnType(returnType);

			returnType = IrFactory.eINSTANCE.createTypeBool();
			Procedure scheduler = irFactory.createProcedure("isSchedulable_"
					+ actor.getSimpleName() + "_fillBuffer", 0, returnType);

			BlockBasic schedulerReturnBlock = IrFactory.eINSTANCE
					.createBlockBasic();
			Expression trueExpr = irFactory.createExprBool(true);
			instReturn = IrFactory.eINSTANCE.createInstReturn(trueExpr);
			schedulerReturnBlock.add(instReturn);
			scheduler.getBlocks().add(schedulerReturnBlock);
			returnType = IrFactory.eINSTANCE.createTypeBool();
			scheduler.setReturnType(returnType);

			Pattern inputPattern = DfFactory.eINSTANCE.createPattern();
			Pattern outputPattern = DfFactory.eINSTANCE.createPattern();
			Pattern peekPattern = DfFactory.eINSTANCE.createPattern();

			Action action = DfFactory.eINSTANCE.createAction(name,
					inputPattern, outputPattern, peekPattern, scheduler, body);
			action.addAttribute("fillBuffer");
			return action;
		} else {
			return null;
		}
	}

}
