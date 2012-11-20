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
package net.sf.orc2hdl.design.visitors;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sf.orc2hdl.design.ResourceCache;
import net.sf.orc2hdl.design.util.XronosIrUtil;
import net.sf.orc2hdl.design.visitors.io.CircularBuffer;
import net.sf.orc2hdl.ir.InstPortStatus;
import net.sf.orc2hdl.ir.XronosIrSpecificFactory;
import net.sf.orcc.df.Action;
import net.sf.orcc.df.Actor;
import net.sf.orcc.df.Pattern;
import net.sf.orcc.df.Port;
import net.sf.orcc.df.util.DfVisitor;
import net.sf.orcc.ir.Block;
import net.sf.orcc.ir.BlockBasic;
import net.sf.orcc.ir.BlockIf;
import net.sf.orcc.ir.BlockWhile;
import net.sf.orcc.ir.Def;
import net.sf.orcc.ir.ExprBool;
import net.sf.orcc.ir.ExprInt;
import net.sf.orcc.ir.ExprVar;
import net.sf.orcc.ir.Expression;
import net.sf.orcc.ir.InstAssign;
import net.sf.orcc.ir.InstCall;
import net.sf.orcc.ir.InstLoad;
import net.sf.orcc.ir.InstReturn;
import net.sf.orcc.ir.InstStore;
import net.sf.orcc.ir.IrFactory;
import net.sf.orcc.ir.OpBinary;
import net.sf.orcc.ir.Procedure;
import net.sf.orcc.ir.Type;
import net.sf.orcc.ir.Var;

/**
 * Xronos Action scheduler class
 * 
 * @author Endri Bezati
 * 
 */
public class XronosScheduler extends DfVisitor<Procedure> {
	/**
	 * This inner visor constructs a list of blocks that contains the conditions
	 * for the schedulability of an action
	 * 
	 * @author Endri Bezati
	 * 
	 */
	public class ActionSchedulability extends DfVisitor<Block> {

		private BlockBasic block;

		private Action action;

		private Var tokenAvailability;

		private Map<Port, Integer> portRequestSize;

		public ActionSchedulability(Actor actor, ResourceCache resourceCache) {
			super();
			block = irFactory.createBlockBasic();
			tokenAvailability = null;
			portRequestSize = new HashMap<Port, Integer>();
		}

		@Override
		public Block caseAction(Action action) {
			this.action = action;
			// Visit inputPattern
			doSwitch(action.getInputPattern());

			Procedure actionScheduler = action.getScheduler();
			Type typeBool = irFactory.createTypeBool();

			Var isSchedulableReturn = irFactory.createVar(typeBool, "return_"
					+ actionScheduler.getName(), true, 0);
			xronosSchedulerLocals.add(isSchedulableReturn);
			InstCall instCall = irFactory.createInstCall(isSchedulableReturn,
					actionScheduler, null);
			block.add(instCall);

			Var actionGo = irFactory.createVar(typeBool,
					"actionGo_" + action.getName(), true, 0);
			xronosSchedulerLocals.add(isSchedulableReturn);

			if (tokenAvailability == null) {
				InstAssign instAssign = irFactory.createInstAssign(actionGo,
						isSchedulableReturn);
				block.add(instAssign);
			} else {
				ExprVar evIsSchedulableReturn = irFactory
						.createExprVar(isSchedulableReturn);
				ExprVar evTokenAvailability = irFactory
						.createExprVar(tokenAvailability);
				Expression exprActionGo = irFactory.createExprBinary(
						evIsSchedulableReturn, OpBinary.LOGIC_AND,
						evTokenAvailability, typeBool);
				InstAssign instAssign = irFactory.createInstAssign(actionGo,
						exprActionGo);
				block.add(instAssign);
			}
			actionSchedulability.put(action, actionGo);
			actionInputPortRequestSize.put(action, portRequestSize);
			return block;
		}

		@Override
		public Block casePattern(Pattern pattern) {
			/** Create the token availability condition **/
			Expression exprTokenAvailability = null;
			Type typeBool = irFactory.createTypeBool();
			for (Port port : pattern.getPorts()) {
				if (inputCircularBuffer.get(port) != null) {
					// Multiple token
					CircularBuffer circularBuffer = inputCircularBuffer
							.get(port);
					int numTokens = pattern.getNumTokensMap().get(port);
					portRequestSize.put(port, numTokens);
					// Create the start test Expression
					Var cbTmpStart = circularBuffer.getTmpStart();

					ExprVar evTmpStart = irFactory.createExprVar(cbTmpStart);
					ExprBool exprFalse = irFactory.createExprBool(false);
					Expression exprStartEqualsFalse = irFactory
							.createExprBinary(evTmpStart, OpBinary.EQ,
									exprFalse, typeBool);

					Var cbTmpCount = circularBuffer.getTmpCount();

					// Create token availability Expression for this port
					ExprVar evTmpCount = irFactory.createExprVar(cbTmpCount);
					ExprInt eiNumTokens = irFactory.createExprInt(numTokens);

					Expression exprCountGeNumTokens = irFactory
							.createExprBinary(evTmpCount, OpBinary.GE,
									eiNumTokens, typeBool);

					Var portTokenAvailability = irFactory.createVar(typeBool,
							"portTokenAvailability_" + action.getName() + "_"
									+ port.getName(), true, 0);
					xronosSchedulerLocals.add(portTokenAvailability);

					Expression exprPortTokenAvailability = irFactory
							.createExprBinary(exprCountGeNumTokens,
									OpBinary.LOGIC_AND, exprStartEqualsFalse,
									typeBool);
					InstAssign instAssign = irFactory.createInstAssign(
							portTokenAvailability, exprPortTokenAvailability);
					block.add(instAssign);

					// Update the final Expression
					if (exprTokenAvailability == null) {
						exprTokenAvailability = irFactory
								.createExprVar(portTokenAvailability);
					} else {
						ExprVar exprVar = irFactory
								.createExprVar(portTokenAvailability);
						exprTokenAvailability = irFactory.createExprBinary(
								exprTokenAvailability, OpBinary.LOGIC_AND,
								exprVar, typeBool);
					}
				} else {
					// Single token
					InstPortStatus instPortStatus = XronosIrSpecificFactory.eINSTANCE
							.createInstPortStatus();
					instPortStatus.setPort(port);

					// Create the portStatus variable and add it to the locals
					Var portStatus = irFactory.createVar(
							typeBool,
							"portStatus_" + action.getName() + "_"
									+ port.getName(), true, 0);
					xronosSchedulerLocals.add(portStatus);

					Def target = irFactory.createDef(portStatus);
					instPortStatus.setTarget(target);

					// Add this instruction to the block
					block.add(instPortStatus);

					// Update the final Expression
					if (exprTokenAvailability == null) {
						exprTokenAvailability = irFactory
								.createExprVar(portStatus);
					} else {
						ExprVar exprVar = irFactory.createExprVar(portStatus);
						exprTokenAvailability = irFactory.createExprBinary(
								exprTokenAvailability, OpBinary.LOGIC_AND,
								exprVar, typeBool);
					}
				}
			}

			if (exprTokenAvailability != null) {
				tokenAvailability = irFactory.createVar(typeBool,
						"tokenAvailability_" + action.getName(), true, 0);
				xronosSchedulerLocals.add(tokenAvailability);
				InstAssign instAssign = irFactory.createInstAssign(
						tokenAvailability, exprTokenAvailability);
				block.add(instAssign);
			}

			return null;
		}
	}

	/**
	 * This inner visor constructs a list of blocks that contains the conditions
	 * for the fireability of an action
	 * 
	 * @author Endri Bezati
	 * 
	 */
	public class ActionFireability extends DfVisitor<Block> {

		private BlockBasic block;

		private Action action;

		private Var spaceAvailability;

		private Map<Port, Integer> portRequestSize;

		public ActionFireability(Actor actor, ResourceCache resourceCache) {
			super();
			block = irFactory.createBlockBasic();
			portRequestSize = new HashMap<Port, Integer>();
		}

		@Override
		public Block caseAction(Action action) {
			this.action = action;
			// Visit inputPattern
			doSwitch(action.getOutputPattern());
			if (spaceAvailability != null) {
				Type typeBool = irFactory.createTypeBool();
				Var actionFire = irFactory.createVar(typeBool, "actionFire_"
						+ action.getName(), true, 0);
				xronosSchedulerLocals.add(actionFire);
				InstAssign instAssign = irFactory.createInstAssign(actionFire,
						spaceAvailability);
				block.add(instAssign);
				actionFireability.put(action, actionFire);
				actionOutputPortRequestSize.put(action, portRequestSize);
			}
			return block;
		}

		@Override
		public Block casePattern(Pattern pattern) {
			Expression exprSpaceAvailability = null;
			Type typeBool = irFactory.createTypeBool();
			for (Port port : pattern.getPorts()) {
				if (outputCircularBuffer.get(port) != null) {
					// Multiple token
					CircularBuffer circularBuffer = inputCircularBuffer
							.get(port);
					// TODO: Implement a real circular Buffer on the Output
					Integer numTokens = pattern.getNumTokensMap().get(port);
					portRequestSize.put(port, numTokens);

					// Create the start test Expression
					Var cbTmpStart = circularBuffer.getTmpStart();

					ExprVar evTmpStart = irFactory.createExprVar(cbTmpStart);
					ExprBool exprFalse = irFactory.createExprBool(false);
					Expression exprStartEqualsFalse = irFactory
							.createExprBinary(evTmpStart, OpBinary.EQ,
									exprFalse, typeBool);

					Var cbTmpCount = circularBuffer.getTmpCount();

					// Create Count equals ? 0 expression
					ExprVar evTmpCount = irFactory.createExprVar(cbTmpCount);
					ExprInt eiZero = irFactory.createExprInt(0);

					Expression exprCountEmpty = irFactory.createExprBinary(
							evTmpCount, OpBinary.EQ, eiZero, typeBool);

					Var portSpaceAvailability = irFactory.createVar(typeBool,
							"portTokenAvailability_" + action.getName() + "_"
									+ port.getName(), true, 0);
					xronosSchedulerLocals.add(portSpaceAvailability);

					Expression exprPortSpaceAvailability = irFactory
							.createExprBinary(exprCountEmpty,
									OpBinary.LOGIC_AND, exprStartEqualsFalse,
									typeBool);
					InstAssign instAssign = irFactory.createInstAssign(
							portSpaceAvailability, exprPortSpaceAvailability);
					block.add(instAssign);

					// Update the final Expression
					if (exprSpaceAvailability == null) {
						exprSpaceAvailability = irFactory
								.createExprVar(portSpaceAvailability);
					} else {
						ExprVar exprVar = irFactory
								.createExprVar(portSpaceAvailability);
						exprSpaceAvailability = irFactory.createExprBinary(
								exprSpaceAvailability, OpBinary.LOGIC_AND,
								exprVar, typeBool);
					}
				} else {
					// Single token
					InstPortStatus instPortStatus = XronosIrSpecificFactory.eINSTANCE
							.createInstPortStatus();
					instPortStatus.setPort(port);

					// Create the portStatus variable and add it to the locals
					Var portStatus = irFactory.createVar(
							typeBool,
							"portStatus_" + action.getName() + "_"
									+ port.getName(), true, 0);
					xronosSchedulerLocals.add(portStatus);

					Def target = irFactory.createDef(portStatus);
					instPortStatus.setTarget(target);

					// Add this instruction to the block
					block.add(instPortStatus);

					// Update the final Expression
					if (exprSpaceAvailability == null) {
						exprSpaceAvailability = irFactory
								.createExprVar(portStatus);
					} else {
						ExprVar exprVar = irFactory.createExprVar(portStatus);
						exprSpaceAvailability = irFactory.createExprBinary(
								exprSpaceAvailability, OpBinary.LOGIC_AND,
								exprVar, typeBool);
					}
				}
			}
			if (exprSpaceAvailability != null) {
				spaceAvailability = irFactory.createVar(typeBool,
						"tokenAvailability_" + action.getName(), true, 0);
				xronosSchedulerLocals.add(spaceAvailability);
				InstAssign instAssign = irFactory.createInstAssign(
						spaceAvailability, exprSpaceAvailability);
				block.add(instAssign);
			}

			return null;
		}
	}

	private IrFactory irFactory = IrFactory.eINSTANCE;

	private ResourceCache resourceCache;

	private List<Var> xronosSchedulerLocals;

	private Map<Port, CircularBuffer> inputCircularBuffer;

	private Map<Port, CircularBuffer> outputCircularBuffer;

	private Map<Action, Var> actionSchedulability;

	private Map<Action, Var> actionFireability;

	private Map<Action, Map<Port, Integer>> actionInputPortRequestSize;

	private Map<Action, Map<Port, Integer>> actionOutputPortRequestSize;

	public XronosScheduler(ResourceCache resourceCache) {
		super();
		this.resourceCache = resourceCache;
		this.xronosSchedulerLocals = new ArrayList<Var>();
		actionInputPortRequestSize = new HashMap<Action, Map<Port, Integer>>();
		actionOutputPortRequestSize = new HashMap<Action, Map<Port, Integer>>();
		actionFireability = new HashMap<Action, Var>();
		actionSchedulability = new HashMap<Action, Var>();
	}

	@Override
	public Procedure caseActor(Actor actor) {
		// Initialize input/output circularBuffer
		inputCircularBuffer = resourceCache.getActorInputCircularBuffer(actor);
		outputCircularBuffer = resourceCache
				.getActorOutputCircularBuffer(actor);

		/** Create the Xronos scheduler procedure **/
		Procedure xronosScheduler = irFactory.createProcedure();

		// Set name
		xronosScheduler.setName("scheduler");

		/** populate the scheduler body **/
		List<Block> blockWhileBody = new ArrayList<Block>();

		// Initialize circular Buffer variables
		BlockBasic initBlock = createSchedulerInitBlock(actor);
		if (!initBlock.getInstructions().isEmpty()) {
			blockWhileBody.add(initBlock);
		}

		// Create the scheduler Body
		blockWhileBody.addAll(createSchedulerBody(actor));

		/** Create the scheduler infinite loop **/
		BlockWhile blockWhile = XronosIrUtil
				.createTrueBlockWhile(blockWhileBody);
		/** Get the first circulaBuffer request size, if any **/
		if (actor.getActionsOutsideFsm() != null) {
			xronosScheduler.getBlocks()
					.add(createRequestSizeBlock(actor.getActionsOutsideFsm()
							.get(0)));
		}

		/** Put the while loop into the procedure body **/
		xronosScheduler.getBlocks().add(blockWhile);

		/** Create a BlockBasic to put the return **/
		BlockBasic returnBlock = irFactory.createBlockBasic();

		/** Add locals **/
		for (Var var : xronosSchedulerLocals) {
			xronosScheduler.getLocals().add(var);
		}

		// Create a Return Instruction
		InstReturn instReturn = irFactory.createInstReturn(null);
		returnBlock.add(instReturn);

		/** Put the returnBody into the procedure body **/
		xronosScheduler.getBlocks().add(returnBlock);
		Type returnType = irFactory.createTypeVoid();
		xronosScheduler.setReturnType(returnType);

		return xronosScheduler;
	}

	private List<Block> createSchedulerBody(Actor actor) {
		List<Block> blocks = new ArrayList<Block>();

		/** For each CircularBuffer Load the stateVars to temporary one **/
		BlockBasic cbLoadBlock = irFactory.createBlockBasic();
		for (Port port : actor.getInputs()) {
			if (inputCircularBuffer.get(port) != null) {
				CircularBuffer circularBuffer = inputCircularBuffer.get(port);
				// Count
				Var cbCount = circularBuffer.getCount();
				Var cbTmpCount = circularBuffer.getTmpCount();
				xronosSchedulerLocals.add(cbTmpCount);
				InstLoad instLoadCount = irFactory.createInstLoad(cbTmpCount,
						cbCount);
				// Start
				Var cbStart = circularBuffer.getStart();
				Var cbTmpStart = circularBuffer.getTmpStart();
				xronosSchedulerLocals.add(cbTmpStart);
				InstLoad instLoadStart = irFactory.createInstLoad(cbTmpStart,
						cbStart);

				// Add all instructions
				cbLoadBlock.add(instLoadCount);
				cbLoadBlock.add(instLoadStart);
			}
		}

		for (Port port : actor.getOutputs()) {
			if (outputCircularBuffer.get(port) != null) {
				CircularBuffer circularBuffer = outputCircularBuffer.get(port);
				// Count
				Var cbCount = circularBuffer.getCount();
				Var cbTmpCount = circularBuffer.getTmpCount();
				xronosSchedulerLocals.add(cbTmpCount);
				InstLoad instLoadCount = irFactory.createInstLoad(cbTmpCount,
						cbCount);
				// Start
				Var cbStart = circularBuffer.getStart();
				Var cbTmpStart = circularBuffer.getTmpStart();
				xronosSchedulerLocals.add(cbTmpStart);
				InstLoad instLoadStart = irFactory.createInstLoad(cbTmpStart,
						cbStart);

				// Add all instructions
				cbLoadBlock.add(instLoadCount);
				cbLoadBlock.add(instLoadStart);
			}
		}
		blocks.add(cbLoadBlock);

		// Add schedulability and fireability blocks
		ActionSchedulability actionSchedulability = new ActionSchedulability(
				actor, resourceCache);
		ActionFireability actionFireability = new ActionFireability(actor,
				resourceCache);
		for (Action action : actor.getActions()) {
			blocks.add(actionSchedulability.doSwitch(action));
			blocks.add(actionFireability.doSwitch(action));
		}

		BlockIf lastBlockIf = null;
		/** Test if the actor has an FSM **/
		if (actor.getFsm() == null) {
			for (Action action : actor.getActionsOutsideFsm()) {
				lastBlockIf = createTaskCall(action, lastBlockIf);
			}
		} else {

		}
		blocks.add(lastBlockIf);
		return blocks;
	}

	private BlockBasic createSchedulerInitBlock(Actor actor) {
		BlockBasic block = irFactory.createBlockBasic();
		// CircularBuffer inputs
		for (Port port : actor.getInputs()) {
			if (inputCircularBuffer.get(port) != null) {
				CircularBuffer circularBuffer = inputCircularBuffer.get(port);
				// Store(cbStart, true);
				Var cbStart = circularBuffer.getStart();
				InstStore instStoreStart = XronosIrUtil.createInstStore(
						cbStart, true);
				block.add(instStoreStart);
			}
		}

		for (Port port : actor.getOutputs()) {
			if (outputCircularBuffer.get(port) != null) {
				CircularBuffer circularBuffer = outputCircularBuffer.get(port);
				// Store(cbStart, true);
				Var cbStart = circularBuffer.getStart();
				InstStore instStoreStart = XronosIrUtil.createInstStore(
						cbStart, true);
				block.add(instStoreStart);
			}
		}
		return block;
	}

	private BlockIf createTaskCall(Action action, BlockIf lastBlockIf) {
		BlockIf blockIf = null;
		if (lastBlockIf == null) {
			// Get the fireability and schedulability conditions
			Var schedulability = actionSchedulability.get(action);
			Var fireability = actionFireability.get(action);
			xronosSchedulerLocals.add(schedulability);
			xronosSchedulerLocals.add(fireability);

			// Create Inst call
			InstCall instCall = irFactory.createInstCall();
			instCall.setProcedure(action.getBody());

			// Create the fireability BlockIf
			BlockIf fireabilityIf = XronosIrUtil.createBlockIf(fireability,
					instCall);
			// Create the schedulability BlockIf
			blockIf = XronosIrUtil.createBlockIf(schedulability, fireabilityIf);
		} else {
			// Get the fireability and schedulability conditions
			Var schedulability = actionSchedulability.get(action);
			Var fireability = actionFireability.get(action);
			xronosSchedulerLocals.add(schedulability);
			xronosSchedulerLocals.add(fireability);

			// Create Inst call
			InstCall instCall = irFactory.createInstCall();
			instCall.setProcedure(action.getBody());

			// Create the fireability BlockIf
			BlockIf fireabilityIf = XronosIrUtil.createBlockIf(fireability,
					instCall);

			// Create the schedulability BlockIf
			BlockIf schedulabilityIf = XronosIrUtil.createBlockIf(
					schedulability, fireabilityIf);

			// Create a block with store for each
			lastBlockIf.getElseBlocks().add(createRequestSizeBlock(action));

			lastBlockIf.getElseBlocks().add(schedulabilityIf);
			blockIf = lastBlockIf;
		}
		return blockIf;
	}

	private BlockBasic createRequestSizeBlock(Action action) {
		BlockBasic blockBasic = irFactory.createBlockBasic();
		Map<Port, Integer> inputRequestSizeMap = actionInputPortRequestSize
				.get(action);

		if (inputRequestSizeMap != null) {
			for (Port port : inputRequestSizeMap.keySet()) {
				if (inputCircularBuffer.get(port) != null) {
					CircularBuffer circularBuffer = inputCircularBuffer
							.get(port);
					Integer numTokens = inputRequestSizeMap.get(port);
					Var cbRequestSize = circularBuffer.getRequestSize();
					InstStore instStore = irFactory.createInstStore(
							cbRequestSize, numTokens);
					blockBasic.add(instStore);
				}
			}
		}
		// TODO: See if RequestSize is necessary for the output
		return blockBasic;
	}
}
