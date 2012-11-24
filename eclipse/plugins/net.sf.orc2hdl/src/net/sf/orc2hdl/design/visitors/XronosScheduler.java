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
import net.sf.orcc.df.State;
import net.sf.orcc.df.Transition;
import net.sf.orcc.df.util.DfVisitor;
import net.sf.orcc.graph.Edge;
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

					Var cbTmpCount = circularBuffer.getTmpCount();

					// Create token availability Expression for this port
					ExprVar evTmpCount = irFactory.createExprVar(cbTmpCount);
					ExprInt eiSize = irFactory.createExprInt(circularBuffer
							.getSize());
					Expression exprCountGeNumTokens = irFactory
							.createExprBinary(evTmpCount, OpBinary.GE, eiSize,
									typeBool);

					Var portTokenAvailability = irFactory.createVar(typeBool,
							"portTokenAvailability_" + action.getName() + "_"
									+ port.getName(), true, 0);
					xronosSchedulerLocals.add(portTokenAvailability);

					InstAssign instAssign = irFactory.createInstAssign(
							portTokenAvailability, exprCountGeNumTokens);
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
			} else {
				Type typeBool = irFactory.createTypeBool();
				Var actionFire = irFactory.createVar(typeBool, "actionFire_"
						+ action.getName(), true, 0);
				xronosSchedulerLocals.add(actionFire);
				ExprBool ebTrue = irFactory.createExprBool(true);
				InstAssign instAssign = irFactory.createInstAssign(actionFire,
						ebTrue);
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
		this.actor = actor;
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
		BlockBasic initBlock = createSchedulerInitBlock(actor, xronosScheduler);
		if (!initBlock.getInstructions().isEmpty()) {
			xronosScheduler.getBlocks().add(initBlock);
		}

		// Create the scheduler Body
		blockWhileBody.addAll(createSchedulerBody(actor, xronosScheduler));

		/** Create the scheduler infinite loop **/
		BlockWhile blockWhile = XronosIrUtil
				.createTrueBlockWhile(blockWhileBody);

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

	private List<Block> createSchedulerBody(Actor actor, Procedure procedure) {
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

		/** Test if the actor has an FSM **/
		if (!actor.hasFsm()) {
			BlockIf lastBlockIf = null;
			for (Action action : actor.getActionsOutsideFsm()) {
				lastBlockIf = createTaskCall(procedure, action, lastBlockIf,
						null);
			}
			blocks.add(lastBlockIf);
		} else {
			// // Load the current state to temporary variable
			// BlockBasic currentStateBlock = irFactory.createBlockBasic();
			// Var currentState = actor.getStateVar("currentState");
			// Var tmpCurrentState = irFactory.createVar(
			// IrFactory.eINSTANCE.createTypeInt(), "tmpCurrentState",
			// true, 0);
			// procedure.getLocals().add(tmpCurrentState);
			// InstLoad loadCurrentState = irFactory.createInstLoad(
			// tmpCurrentState, currentState);
			// currentStateBlock.add(loadCurrentState);
			// blocks.add(currentStateBlock);

			BlockIf lastBlockIf = null;
			if (!actor.getActionsOutsideFsm().isEmpty()) {
				for (Action action : actor.getActionsOutsideFsm()) {
					lastBlockIf = createTaskCall(procedure, action,
							lastBlockIf, null);
				}
				lastBlockIf.getElseBlocks().add(
						screateFsmBlockIf(actor, procedure));
				blocks.add(lastBlockIf);
			} else {
				blocks.add(screateFsmBlockIf(actor, procedure));
			}
		}
		return blocks;
	}

	private List<BlockIf> createFsmBlockIf(Actor actor, Procedure procedure) {
		List<BlockIf> blocks = new ArrayList<BlockIf>();
		for (State state : actor.getFsm().getStates()) {

			BlockIf lastBlockIf = null;
			for (Edge edge : state.getOutgoing()) {
				Transition transition = ((Transition) edge);
				State stateTarget = transition.getTarget();
				Action action = transition.getAction();
				lastBlockIf = createTaskCall(procedure, action, lastBlockIf,
						stateTarget);
			}

			// Create an if block that will contains all the transitions
			Var stateSource = procedure.getLocal("s_" + state.getName());
			Var currentState = procedure.getLocal("tmpCurrentState");
			Expression ifStateCondition = XronosIrUtil.createExprBinaryEqual(
					currentState, stateSource);

			BlockIf blockIf = XronosIrUtil.createBlockIf(ifStateCondition,
					lastBlockIf);
			blocks.add(blockIf);
		}
		return blocks;
	}

	private BlockIf screateFsmBlockIf(Actor actor, Procedure procedure) {
		BlockIf block = null;
		BlockIf lastFSMBlockIf = null;
		for (State state : actor.getFsm().getStates()) {

			BlockIf lastBlockIf = null;
			for (Edge edge : state.getOutgoing()) {
				Transition transition = ((Transition) edge);
				State stateTarget = transition.getTarget();
				Action action = transition.getAction();
				lastBlockIf = createTaskCall(procedure, action, lastBlockIf,
						stateTarget);
			}

			// Create an if block that will contains all the transitions
			Var stateSource = procedure.getLocal("s_" + state.getName());
			Var currentState = procedure.getLocal("tmpCurrentState");
			Expression ifStateCondition = XronosIrUtil.createExprBinaryEqual(
					currentState, stateSource);
			if (block == null) {
				BlockIf blockIf = XronosIrUtil.createBlockIf(ifStateCondition,
						lastBlockIf);
				block = blockIf;
				lastFSMBlockIf = blockIf;
			} else {
				BlockIf blockIf = XronosIrUtil.createBlockIf(ifStateCondition,
						lastBlockIf);
				lastFSMBlockIf.getElseBlocks().add(blockIf);
				lastFSMBlockIf = blockIf;
			}
		}
		return block;
	}

	private BlockBasic createSchedulerInitBlock(Actor actor, Procedure procedure) {
		BlockBasic block = irFactory.createBlockBasic();
		// Load currentState
		Var currentState = actor.getStateVar("currentState");
		Var tmpCurrentState = irFactory
				.createVar(IrFactory.eINSTANCE.createTypeInt(),
						"tmpCurrentState", true, 0);
		procedure.getLocals().add(tmpCurrentState);
		InstLoad loadCurrentState = irFactory.createInstLoad(tmpCurrentState,
				currentState);
		block.add(loadCurrentState);

		// Add States if any
		if (actor.hasFsm()) {
			int i = 0;
			for (State state : actor.getFsm().getStates()) {
				Var fsmState = IrFactory.eINSTANCE.createVar(
						IrFactory.eINSTANCE.createTypeInt(),
						"s_" + state.getName(), false, 0);
				fsmState.setValue(i);
				procedure.getLocals().add(fsmState);
				InstAssign assignStateIndex = irFactory.createInstAssign(
						fsmState, i);
				block.add(assignStateIndex);
				i++;
			}
		}

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

	private BlockIf createTaskCall(Procedure procedure, Action action,
			BlockIf lastBlockIf, State target) {
		BlockIf blockIf = null;
		if (lastBlockIf == null) {
			// Get the fireability and schedulability conditions
			Var schedulability = actionSchedulability.get(action);
			Var fireability = actionFireability.get(action);
			xronosSchedulerLocals.add(schedulability);
			xronosSchedulerLocals.add(fireability);

			// Create fireability thenBlock Basic
			BlockBasic fireabilityThenBlock = irFactory.createBlockBasic();

			// Add circularBuffer start to false, if necessary
			createInstStoreStart(action, false, fireabilityThenBlock);

			// Create Inst call
			InstCall instCall = irFactory.createInstCall();
			instCall.setProcedure(action.getBody());
			fireabilityThenBlock.add(instCall);

			// Create InstStore for the currentState if a target exists
			if (target != null) {
				Var currentState = procedure.getLocal("tmpCurrentState");
				Var targetStateVar = procedure
						.getLocal("s_" + target.getName());
				InstAssign storeCurrentState = irFactory.createInstAssign(
						currentState, targetStateVar);
				fireabilityThenBlock.add(storeCurrentState);
			}

			// Add circularBuffer start to true, if necessary
			createInstStoreStart(action, true, fireabilityThenBlock);
			// Create the fireability BlockIf
			BlockIf fireabilityIf = XronosIrUtil.createBlockIf(fireability,
					fireabilityThenBlock);

			// Create the schedulability BlockIf
			blockIf = XronosIrUtil.createBlockIf(schedulability, fireabilityIf);
		} else {
			// Get the fireability and schedulability conditions
			Var schedulability = actionSchedulability.get(action);
			Var fireability = actionFireability.get(action);
			xronosSchedulerLocals.add(schedulability);
			xronosSchedulerLocals.add(fireability);

			// Create fireability thenBlock Basic
			BlockBasic fireabilityThenBlock = irFactory.createBlockBasic();

			// Add circularBuffer start to false, if necessary
			createInstStoreStart(action, false, fireabilityThenBlock);

			// Create Inst call
			InstCall instCall = irFactory.createInstCall();
			instCall.setProcedure(action.getBody());
			fireabilityThenBlock.add(instCall);

			// Create InstStore for the currentState if a target exists
			if (target != null) {
				Var currentState = actor.getStateVar("currentState");
				Var targetStateVar = procedure
						.getLocal("s_" + target.getName());
				InstStore storeCurrentState = irFactory.createInstStore(
						currentState, targetStateVar);
				fireabilityThenBlock.add(storeCurrentState);
			}

			// Add circularBuffer start to true, if necessary
			createInstStoreStart(action, true, fireabilityThenBlock);
			// Create the fireability BlockIf
			BlockIf fireabilityIf = XronosIrUtil.createBlockIf(fireability,
					fireabilityThenBlock);

			// Create the schedulability BlockIf
			BlockIf schedulabilityIf = XronosIrUtil.createBlockIf(
					schedulability, fireabilityIf);

			lastBlockIf.getElseBlocks().add(schedulabilityIf);
			blockIf = lastBlockIf;
		}
		return blockIf;
	}

	private void createInstStoreStart(Action action, Boolean value,
			BlockBasic block) {
		for (Port port : action.getInputPattern().getPorts()) {
			if (inputCircularBuffer.get(port) != null) {
				CircularBuffer circularBuffer = inputCircularBuffer.get(port);
				Var cbStart = circularBuffer.getStart();
				ExprBool ebValue = irFactory.createExprBool(value);
				InstStore storeStart = irFactory.createInstStore(cbStart,
						ebValue);
				block.add(storeStart);
			}

		}
	}

}
