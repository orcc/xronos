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
 */
package org.xronos.orcc.design.visitors;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sf.orcc.backends.transform.Inliner;
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
import net.sf.orcc.ir.Instruction;
import net.sf.orcc.ir.IrFactory;
import net.sf.orcc.ir.OpBinary;
import net.sf.orcc.ir.Procedure;
import net.sf.orcc.ir.Type;
import net.sf.orcc.ir.Var;
import net.sf.orcc.ir.transform.BlockCombine;

import org.xronos.orcc.backend.transform.XronosConstantPropagation;
import org.xronos.orcc.design.ResourceCache;
import org.xronos.orcc.design.util.XronosIrUtil;
import org.xronos.orcc.design.visitors.io.CircularBuffer;
import org.xronos.orcc.ir.BlockMutex;
import org.xronos.orcc.ir.InstPortPeek;
import org.xronos.orcc.ir.InstPortStatus;
import org.xronos.orcc.ir.InstSimplePortWrite;
import org.xronos.orcc.ir.XronosIrFactory;

/**
 * Xronos Action scheduler class
 * 
 * @author Endri Bezati
 * 
 */
public class XronosScheduler extends DfVisitor<Procedure> {
	/**
	 * This inner visor constructs a list of blocks that contains the conditions
	 * for the fireability of an action
	 * 
	 * @author Endri Bezati
	 * 
	 */
	public class ActionFireability extends DfVisitor<Block> {

		private Action action;

		private final BlockBasic block;

		private final Map<Port, Integer> portRequestSize;

		private Var spaceAvailability;

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
			spaceAvailability = null;
			return block;
		}

		@Override
		public Block casePattern(Pattern pattern) {
			Expression exprSpaceAvailability = null;
			Type typeBool = irFactory.createTypeBool();
			for (Port port : pattern.getPorts()) {

				// Get portStatus from Locals
				Var portStatus = XronosIrUtil.getVarFromList("portStatus_"
						+ port.getName(), xronosSchedulerLocals);

				// Update the final Expression
				if (exprSpaceAvailability == null) {
					exprSpaceAvailability = irFactory.createExprVar(portStatus);
				} else {
					ExprVar exprVar = irFactory.createExprVar(portStatus);
					exprSpaceAvailability = irFactory.createExprBinary(
							exprSpaceAvailability, OpBinary.LOGIC_AND, exprVar,
							typeBool);
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

	/**
	 * This inner visor constructs a list of blocks that contains the conditions
	 * for the schedulability of an action
	 * 
	 * @author Endri Bezati
	 * 
	 */
	public class ActionSchedulability extends DfVisitor<Block> {

		private Action action;

		private final BlockBasic block;

		private final Map<Port, Integer> portRequestSize;

		private Var tokenAvailability;

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
			tokenAvailability = null;
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
					ExprInt eiNumTokens = irFactory.createExprInt(numTokens);
					Expression exprCountGeNumTokens = irFactory
							.createExprBinary(evTmpCount, OpBinary.GE,
									eiNumTokens, typeBool);

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
					// Get PortSatus form locals
					Var portStatus = XronosIrUtil.getVarFromList("portStatus_"
							+ port.getName(), xronosSchedulerLocals);
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
				if (schedulerInformation) {
					InstSimplePortWrite instSimplePortWrite = XronosIrFactory.eINSTANCE
							.createInstSimplePortWrite();
					instSimplePortWrite.setName("ta_" + action.getName());
					ExprVar tVar = irFactory.createExprVar(tokenAvailability);
					instSimplePortWrite.setValue(tVar);
					block.add(instSimplePortWrite);
				}
			}

			return null;
		}
	}

	private final Map<Action, Var> actionFireability;

	private final Map<Action, Map<Port, Integer>> actionInputPortRequestSize;

	private final Map<Action, Map<Port, Integer>> actionOutputPortRequestSize;

	private final Map<Action, Var> actionSchedulability;

	private Map<Port, CircularBuffer> inputCircularBuffer;

	private final IrFactory irFactory = IrFactory.eINSTANCE;

	private final ResourceCache resourceCache;

	private final List<Var> xronosSchedulerLocals;

	private BlockIf firstBlockIf;

	private Boolean schedulerInformation;

	public XronosScheduler(ResourceCache resourceCache,
			Boolean schedulerInformation) {
		super();
		this.resourceCache = resourceCache;
		this.schedulerInformation = schedulerInformation;
		xronosSchedulerLocals = new ArrayList<Var>();
		actionInputPortRequestSize = new HashMap<Action, Map<Port, Integer>>();
		actionOutputPortRequestSize = new HashMap<Action, Map<Port, Integer>>();
		actionFireability = new HashMap<Action, Var>();
		actionSchedulability = new HashMap<Action, Var>();
	}

	@Override
	public Procedure caseActor(Actor actor) {
		this.actor = actor;

		if (schedulerInformation) {
			// Add stateVar related to the actor Idle
			Var idleVar = IrFactory.eINSTANCE.createVar(
					IrFactory.eINSTANCE.createTypeBool(),
					"idle_" + actor.getName(), true, 0);
			idleVar.setValue(false);
			idleVar.setInitialValue(IrFactory.eINSTANCE.createExprBool(false));
			actor.getStateVars().add(idleVar);

			// Add stateVar related to the actor Idle Action Pin
			Var idleAction = IrFactory.eINSTANCE.createVar(IrFactory.eINSTANCE
					.createTypeInt(actor.getActions().size()), "idle_action_"
					+ actor.getName(), true, 0);
			idleAction.setValue(Integer.valueOf(0));
			idleAction.setInitialValue(IrFactory.eINSTANCE.createExprInt(0));
			actor.getStateVars().add(idleAction);
		}

		if (actor.getFsm() != null) {
			// Add state Vars
			for (State state : actor.getFsm().getStates()) {
				Type typeBool = IrFactory.eINSTANCE.createTypeBool();
				Var fsmState = IrFactory.eINSTANCE.createVar(typeBool, "state_"
						+ state.getName(), true, 0);
				if (state == actor.getFsm().getInitialState()) {
					fsmState.setValue(true);
				} else {
					fsmState.setValue(false);
				}
				actor.getStateVars().add(fsmState);
			}
		}

		// Initialize input/output circularBuffer
		inputCircularBuffer = resourceCache.getActorInputCircularBuffer(actor);

		/** Create the Xronos scheduler procedure **/
		Procedure xronosScheduler = irFactory.createProcedure();

		// Set name
		xronosScheduler.setName("scheduler");
		xronosScheduler.addAttribute("scheduler");

		/** populate the scheduler body **/
		List<Block> blockWhileBody = new ArrayList<Block>();

		// Initialize circular Buffer variables
		BlockBasic initBlock = createSchedulerInitBlock(actor, xronosScheduler);
		if (!initBlock.getInstructions().isEmpty()) {
			xronosScheduler.getBlocks().add(initBlock);
		}

		// Loads of states
		if (actor.hasFsm()) {
			BlockBasic stateBlock = irFactory.createBlockBasic();
			for (State state : actor.getFsm().getStates()) {
				Var fsmState = actor.getStateVar("state_" + state.getName());
				Var sState = irFactory.createVar(fsmState.getType(), "s_"
						+ state.getName(), true, 0);
				xronosScheduler.getLocals().add(sState);
				InstLoad instaLoad = irFactory.createInstLoad(sState, fsmState);
				stateBlock.add(instaLoad);
			}
			blockWhileBody.add(stateBlock);
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

		actor.getProcs().add(xronosScheduler);

		Inliner inliner = new Inliner(false, true, true);
		inliner.doSwitch(xronosScheduler);

		BlockCombine blockCombine = new BlockCombine(false);
		blockCombine.doSwitch(xronosScheduler);

		//LoadOnce loadOnce = new LoadOnce();
		//loadOnce.doSwitch(xronosScheduler);

		XronosConstantPropagation xronosConstantPropagation = new XronosConstantPropagation();
		xronosConstantPropagation.doSwitch(xronosScheduler);

		return xronosScheduler;
	}

	private List<BlockIf> createFsmBlockIf(Actor actor, Procedure procedure) {
		List<BlockIf> blocks = new ArrayList<BlockIf>();
		for (State state : actor.getFsm().getStates()) {
			BigInteger actionMask = BigInteger.ZERO;
			BlockIf lastBlockIf = null;
			BlockIf oldFirstBlock = null;
			if (!actor.getActionsOutsideFsm().isEmpty()) {
				for (Action action : actor.getActionsOutsideFsm()) {
					lastBlockIf = createTaskCallOutFSM(procedure, action,
							lastBlockIf);
				}
				oldFirstBlock = firstBlockIf;
			}
			for (Edge edge : state.getOutgoing()) {
				Transition transition = ((Transition) edge);
				State stateTarget = transition.getTarget();
				State stateSource = transition.getSource();
				Action action = transition.getAction();
				lastBlockIf = createTaskCall(procedure, action, lastBlockIf,
						stateSource, stateTarget);

				// Update action Mask
				if (!action.getInputPattern().isEmpty()) {
					actionMask = actionMask.add((BigInteger.valueOf(2))
							.pow(actor.getActions().indexOf(action)));
				}
			}
			if (schedulerInformation) {
				lastBlockIf.getElseBlocks().add(createIdleBlock(actionMask));
			}
			// Create an if block that will contains all the transitions
			Var stateSource = procedure.getLocal("s_" + state.getName());

			Expression ifStateCondition = irFactory.createExprVar(stateSource);
			if (oldFirstBlock == null) {
				oldFirstBlock = firstBlockIf;
			}
			BlockIf blockIf = XronosIrUtil.createBlockIf(ifStateCondition,
					oldFirstBlock);
			blocks.add(blockIf);
		}
		return blocks;
	}

	private Block createIdleBlock(BigInteger reason) {
		BlockBasic blockBasic = irFactory.createBlockBasic();

		ExprBool exprBool = irFactory.createExprBool(true);

		InstStore storeIdle = irFactory.createInstStore(
				actor.getStateVar("idle_" + actor.getName()), exprBool);
		blockBasic.add(storeIdle);

		// Add scheduling information, only if the actions have an input
		Expression exprNumber = IrFactory.eINSTANCE.createExprInt(reason);

		InstStore storeIdleAction = irFactory
				.createInstStore(
						actor.getStateVar("idle_action_" + actor.getName()),
						exprNumber);
		blockBasic.add(storeIdleAction);

		return blockBasic;
	}

	private List<Block> createSchedulerBody(Actor actor, Procedure procedure) {
		List<Block> blocks = new ArrayList<Block>();

		/** For each CircularBuffer Load the stateVars to temporary one **/
		BlockBasic cbLoadBlock = irFactory.createBlockBasic();
		BlockBasic statusPeekBlock = irFactory.createBlockBasic();
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
			} else {
				InstPortStatus instPortStatus = XronosIrFactory.eINSTANCE
						.createInstPortStatus();
				instPortStatus.setPort(port);
				Type typeBool = irFactory.createTypeBool();
				// Create the portStatus variable and add it to the locals
				Var portStatus = irFactory.createVar(typeBool, "portStatus_"
						+ port.getName(), true, 0);
				xronosSchedulerLocals.add(portStatus);

				Def target = irFactory.createDef(portStatus);
				instPortStatus.setTarget(target);

				statusPeekBlock.add(instPortStatus);

				if (actor.getStateVar("portPeek_" + port.getName()) != null) {
					Var portPeekVar = actor.getStateVar("portPeek_"
							+ port.getName());
					InstPortPeek portPeek = XronosIrFactory.eINSTANCE
							.createInstPortPeek();
					portPeek.setPort(port);
					Var peekVar = irFactory.createVar(port.getType(),
							"peekVar", true, 0);
					xronosSchedulerLocals.add(peekVar);
					Def def = irFactory.createDef(portPeekVar);
					portPeek.setTarget(def);

					// InstStore storePeek = irFactory.createInstStore(
					// portPeekVar, peekVar);

					statusPeekBlock.add(portPeek);
					// statusPeekBlock.add(storePeek);
				}
			}
		}

		for (Port port : actor.getOutputs()) {
			InstPortStatus instPortStatus = XronosIrFactory.eINSTANCE
					.createInstPortStatus();
			instPortStatus.setPort(port);
			Type typeBool = irFactory.createTypeBool();
			// Create the portStatus variable and add it to the locals
			Var portStatus = irFactory.createVar(typeBool,
					"portStatus_" + port.getName(), true, 0);
			xronosSchedulerLocals.add(portStatus);

			Def target = irFactory.createDef(portStatus);
			instPortStatus.setTarget(target);

			statusPeekBlock.add(instPortStatus);
		}

		if (!cbLoadBlock.getInstructions().isEmpty()) {
			blocks.add(cbLoadBlock);
		}

		if (!statusPeekBlock.getInstructions().isEmpty()) {
			blocks.add(statusPeekBlock);
		}

		// Add schedulability and fireability blocks
		ActionSchedulability actionSchedulability = new ActionSchedulability(
				actor, resourceCache);
		ActionFireability actionFireability = new ActionFireability(actor,
				resourceCache);
		for (Action action : actor.getActions()) {
			blocks.add(actionSchedulability.doSwitch(action));
			blocks.add(actionFireability.doSwitch(action));
		}

		// Add scheduling information
		if (schedulerInformation) {
			BlockBasic blockBasic = irFactory.createBlockBasic();

			// Create Load instruction for idle
			Var tmpIdle = null;
			if (procedure.getLocals().contains("tmpIdle_" + actor.getName())) {
				tmpIdle = procedure.getLocal("tmpIdle_" + actor.getName());
			} else {
				tmpIdle = irFactory.createVar(irFactory.createTypeBool(),
						"tmpIdle_" + actor.getName(), true, 0);
				procedure.getLocals().add(tmpIdle);
			}

			InstLoad loadIdle = irFactory.createInstLoad(tmpIdle,
					actor.getStateVar("idle_" + actor.getName()));

			blockBasic.add(loadIdle);

			Expression boolValue = IrFactory.eINSTANCE.createExprVar(tmpIdle);
			InstSimplePortWrite idlePortWrite = XronosIrFactory.eINSTANCE
					.createInstSimplePortWrite();
			idlePortWrite.setName("idle_" + actor.getSimpleName());
			idlePortWrite.setValue(boolValue);

			blockBasic.add(idlePortWrite);

			// Create Load instruction for idle Action
			Var tmpIdleAction = null;
			if (procedure.getLocals().contains(
					"tmpIdleAction_" + actor.getName())) {
				tmpIdleAction = procedure.getLocal("tmpIdleAction_"
						+ actor.getName());
			} else {
				tmpIdleAction = irFactory.createVar(
						irFactory.createTypeInt(actor.getActions().size()),
						"tmpIdleAction_" + actor.getName(), true, 0);
				procedure.getLocals().add(tmpIdleAction);
			}

			InstLoad loadIdleAction = irFactory.createInstLoad(tmpIdle,
					actor.getStateVar("idle_action_" + actor.getName()));

			blockBasic.add(loadIdleAction);

			Expression bigIntegerValue = IrFactory.eINSTANCE
					.createExprVar(tmpIdle);
			InstSimplePortWrite idleActionPortWrite = XronosIrFactory.eINSTANCE
					.createInstSimplePortWrite();
			idleActionPortWrite.setName("idle_action_" + actor.getSimpleName());
			idleActionPortWrite.setValue(bigIntegerValue);

			blockBasic.add(idleActionPortWrite);

			blocks.add(blockBasic);
		}

		/** Test if the actor has an FSM **/
		if (!actor.hasFsm()) {
			BlockIf lastBlockIf = null;
			BigInteger actionMask = BigInteger.ZERO;
			for (Action action : actor.getActionsOutsideFsm()) {
				lastBlockIf = createTaskCallOutFSM(procedure, action,
						lastBlockIf);
				if (!action.getInputPattern().isEmpty()) {
					actionMask = actionMask.add((BigInteger.valueOf(2))
							.pow(actor.getActions().indexOf(action)));
				}
			}
			if (schedulerInformation) {
				lastBlockIf.getElseBlocks().add(createIdleBlock(actionMask));
			}
			blocks.add(firstBlockIf);
		} else {
			BlockMutex blockMutex = XronosIrFactory.eINSTANCE
					.createBlockMutex();
			blockMutex.getBlocks().addAll(createFsmBlockIf(actor, procedure));

			blocks.add(blockMutex);
		}
		return blocks;
	}

	private BlockBasic createSchedulerInitBlock(Actor actor, Procedure procedure) {
		BlockBasic block = irFactory.createBlockBasic();

		// Add States if any
		if (actor.hasFsm()) {
			for (State state : actor.getFsm().getStates()) {
				Var fsmState = actor.getStateVar("state_" + state.getName());
				if (actor.getFsm().getInitialState() == state) {
					Expression exprTrue = irFactory.createExprBool(true);
					InstStore stateStore = irFactory.createInstStore(fsmState,
							exprTrue);
					block.add(stateStore);
				} else {
					Expression exprFalse = irFactory.createExprBool(false);
					InstStore stateStore = irFactory.createInstStore(fsmState,
							exprFalse);
					block.add(stateStore);
				}
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
		return block;
	}

	private BlockIf createTaskCall(Procedure procedure, Action action,
			BlockIf lastBlockIf, State source, State target) {

		List<Instruction> requestInsts = new ArrayList<Instruction>();
		if (action.getAttribute("fillBuffer") != null) {
			Map<Port, Integer> portsReads = new HashMap<Port, Integer>();
			// Find the actions
			List<Action> actions = new ArrayList<Action>();
			for (Edge edge : source.getOutgoing()) {
				Transition transition = ((Transition) edge);
				actions.add(transition.getAction());
			}
			actions.addAll(actor.getActionsOutsideFsm());
			for (Action outFsmAction : actions) {
				for (Port port : outFsmAction.getInputPattern().getPorts()) {
					if (inputCircularBuffer.containsKey(port)) {
						Integer numReads = outFsmAction.getInputPattern()
								.getNumTokens(port);
						if (portsReads.containsKey(port)) {
							Integer oldNumRead = portsReads.get(port);
							if (oldNumRead < numReads) {
								portsReads.put(port, numReads);
							}
						} else {
							portsReads.put(port, numReads);
						}
					}
				}
			}

			for (Port port : portsReads.keySet()) {
				CircularBuffer circularBuffer = inputCircularBuffer.get(port);
				Var cbRequestSize = circularBuffer.getRequestSize();
				Integer numReads = portsReads.get(port);

				InstStore requestStore = irFactory.createInstStore(
						cbRequestSize, numReads);
				requestInsts.add(requestStore);
			}
		}

		BlockIf blockIf = null;
		if (lastBlockIf == null) {
			// Get the fireability and schedulability conditions
			Var schedulability = actionSchedulability.get(action);
			Var fireability = actionFireability.get(action);
			xronosSchedulerLocals.add(schedulability);
			xronosSchedulerLocals.add(fireability);

			// Create fireability thenBlock Basic
			BlockBasic fireabilityThenBlock = irFactory.createBlockBasic();

			if (!requestInsts.isEmpty()) {
				for (Instruction inst : requestInsts) {
					fireabilityThenBlock.add(inst);
				}
			}

			if (schedulerInformation) {
				resetIdleBlock(fireabilityThenBlock);
			}

			// Create Inst call
			InstCall instCall = irFactory.createInstCall();
			instCall.setProcedure(action.getBody());
			fireabilityThenBlock.add(instCall);

			// Create InstStore for the currentState if a target exists
			if ((target != source)) {
				Expression exprTrue = irFactory.createExprBool(true);
				Expression exprFalse = irFactory.createExprBool(false);

				Var targetState = actor
						.getStateVar("state_" + target.getName());
				Var sourceState = actor
						.getStateVar("state_" + source.getName());

				InstStore targetAtTrue = irFactory.createInstStore(targetState,
						exprTrue);
				fireabilityThenBlock.add(targetAtTrue);

				InstStore targetAtFalse = irFactory.createInstStore(
						sourceState, exprFalse);
				fireabilityThenBlock.add(targetAtFalse);
			}

			// Create the fireability BlockIf
			BlockIf fireabilityIf = null;

			if (schedulerInformation) {
				fireabilityIf = XronosIrUtil.createBlockIf(fireability,
						fireabilityThenBlock, createIdleBlock(BigInteger.ZERO));
			} else {
				fireabilityIf = XronosIrUtil.createBlockIf(fireability,
						fireabilityThenBlock);
			}

			// Create the schedulability BlockIf
			blockIf = XronosIrUtil.createBlockIf(schedulability, fireabilityIf);
			firstBlockIf = blockIf;
		} else {
			// Get the fireability and schedulability conditions
			Var schedulability = actionSchedulability.get(action);
			Var fireability = actionFireability.get(action);
			xronosSchedulerLocals.add(schedulability);
			xronosSchedulerLocals.add(fireability);

			// Create fireability thenBlock Basic
			BlockBasic fireabilityThenBlock = irFactory.createBlockBasic();

			if (!requestInsts.isEmpty()) {
				for (Instruction inst : requestInsts) {
					fireabilityThenBlock.add(inst);
				}
			}

			if (schedulerInformation) {
				resetIdleBlock(fireabilityThenBlock);
			}

			// Create Inst call
			InstCall instCall = irFactory.createInstCall();
			instCall.setProcedure(action.getBody());
			fireabilityThenBlock.add(instCall);

			// Create InstStore for the currentState if a target exists
			if ((target != source)) {
				Expression exprTrue = irFactory.createExprBool(true);
				Expression exprFalse = irFactory.createExprBool(false);

				Var targetState = actor
						.getStateVar("state_" + target.getName());
				Var sourceState = actor
						.getStateVar("state_" + source.getName());

				InstStore targetAtTrue = irFactory.createInstStore(targetState,
						exprTrue);
				fireabilityThenBlock.add(targetAtTrue);

				InstStore targetAtFalse = irFactory.createInstStore(
						sourceState, exprFalse);
				fireabilityThenBlock.add(targetAtFalse);
			}

			// Create the fireability BlockIf
			BlockIf fireabilityIf = null;

			if (schedulerInformation) {
				fireabilityIf = XronosIrUtil.createBlockIf(fireability,
						fireabilityThenBlock, createIdleBlock(BigInteger.ZERO));
			} else {
				fireabilityIf = XronosIrUtil.createBlockIf(fireability,
						fireabilityThenBlock);
			}

			// Create the schedulability BlockIf
			BlockIf schedulabilityIf = XronosIrUtil.createBlockIf(
					schedulability, fireabilityIf);

			lastBlockIf.getElseBlocks().add(schedulabilityIf);
			blockIf = schedulabilityIf;
		}
		return blockIf;
	}

	private BlockIf createTaskCallOutFSM(Procedure procedure, Action action,
			BlockIf lastBlockIf) {
		BlockIf blockIf = null;

		List<Instruction> requestInsts = new ArrayList<Instruction>();
		if (action.getAttribute("fillBuffer") != null) {
			Map<Port, Integer> portsReads = new HashMap<Port, Integer>();
			for (Action outFsmAction : actor.getActionsOutsideFsm()) {
				for (Port port : outFsmAction.getInputPattern().getPorts()) {
					if (inputCircularBuffer.containsKey(port)) {
						Integer numReads = outFsmAction.getInputPattern()
								.getNumTokens(port);
						if (portsReads.containsKey(port)) {
							Integer oldNumRead = portsReads.get(port);
							if (oldNumRead < numReads) {
								portsReads.put(port, numReads);
							}
						} else {
							portsReads.put(port, numReads);
						}
					}
				}
			}

			for (Port port : portsReads.keySet()) {
				CircularBuffer circularBuffer = inputCircularBuffer.get(port);
				Var cbRequestSize = circularBuffer.getRequestSize();
				Integer numReads = portsReads.get(port);

				InstStore requestStore = irFactory.createInstStore(
						cbRequestSize, numReads);
				requestInsts.add(requestStore);
			}
		}

		if (lastBlockIf == null) {
			// Get the fireability and schedulability conditions
			Var schedulability = actionSchedulability.get(action);
			Var fireability = actionFireability.get(action);
			xronosSchedulerLocals.add(schedulability);
			xronosSchedulerLocals.add(fireability);

			// Create fireability thenBlock Basic
			BlockBasic fireabilityThenBlock = irFactory.createBlockBasic();

			List<Block> schedulabilityThenBlocks = new ArrayList<Block>();

			// Add circularBuffer start to false
			BlockBasic stopStartBlockBasic = irFactory.createBlockBasic();

			schedulabilityThenBlocks.add(stopStartBlockBasic);

			if (!requestInsts.isEmpty()) {
				for (Instruction inst : requestInsts) {
					fireabilityThenBlock.add(inst);
				}
			}

			if (schedulerInformation) {
				resetIdleBlock(fireabilityThenBlock);
			}

			// Create Inst call
			InstCall instCall = irFactory.createInstCall();
			instCall.setProcedure(action.getBody());
			fireabilityThenBlock.add(instCall);

			// Add circularBuffer start to true, if necessary

			// Create the fireability BlockIf
			BlockIf fireabilityIf = null;

			if (schedulerInformation) {
				fireabilityIf = XronosIrUtil.createBlockIf(fireability,
						fireabilityThenBlock, createIdleBlock(BigInteger.ZERO));
			} else {
				fireabilityIf = XronosIrUtil.createBlockIf(fireability,
						fireabilityThenBlock);
			}

			schedulabilityThenBlocks.add(fireabilityIf);
			BlockIf schedulabilityIf = XronosIrUtil.createBlockIf(
					schedulability, schedulabilityThenBlocks);
			firstBlockIf = schedulabilityIf;
			blockIf = schedulabilityIf;
		} else {
			// Get the fireability and schedulability conditions
			Var schedulability = actionSchedulability.get(action);
			Var fireability = actionFireability.get(action);
			xronosSchedulerLocals.add(schedulability);
			xronosSchedulerLocals.add(fireability);

			// Create fireability thenBlock Basic
			BlockBasic fireabilityThenBlock = irFactory.createBlockBasic();

			if (!requestInsts.isEmpty()) {
				for (Instruction inst : requestInsts) {
					fireabilityThenBlock.add(inst);
				}
			}

			if (schedulerInformation) {
				resetIdleBlock(fireabilityThenBlock);
			}

			// Create Inst call
			InstCall instCall = irFactory.createInstCall();
			instCall.setProcedure(action.getBody());
			fireabilityThenBlock.add(instCall);

			// Create the fireability BlockIf
			BlockIf fireabilityIf = XronosIrUtil.createBlockIf(fireability,
					fireabilityThenBlock);
			if (schedulerInformation) {
				fireabilityIf.getElseBlocks().add(
						createIdleBlock((BigInteger.valueOf(2)).pow(actor
								.getActions().indexOf(action))));
			}

			// Create the schedulability BlockIf
			BlockIf schedulabilityIf = XronosIrUtil.createBlockIf(
					schedulability, fireabilityIf);

			lastBlockIf.getElseBlocks().add(schedulabilityIf);
			blockIf = schedulabilityIf;
		}
		return blockIf;
	}

	private void resetIdleBlock(BlockBasic block) {

		ExprBool exprBool = irFactory.createExprBool(false);

		InstStore storeIdle = irFactory.createInstStore(
				actor.getStateVar("idle_" + actor.getName()), exprBool);
		block.add(storeIdle);

		// Add scheduling information, only if the actions have an input
		Expression exprNumber = IrFactory.eINSTANCE.createExprInt(0);

		InstStore storeIdleAction = irFactory
				.createInstStore(
						actor.getStateVar("idle_action_" + actor.getName()),
						exprNumber);
		block.add(storeIdleAction);

		InstSimplePortWrite idlePortWrite = XronosIrFactory.eINSTANCE
				.createInstSimplePortWrite();
		idlePortWrite.setName("idle_action_" + actor.getSimpleName());
		idlePortWrite.setValue(irFactory.createExprBool(false));

		block.add(idlePortWrite);

		InstSimplePortWrite idleActionPortWrite = XronosIrFactory.eINSTANCE
				.createInstSimplePortWrite();
		idleActionPortWrite.setName("idle_action_" + actor.getSimpleName());
		idleActionPortWrite.setValue(irFactory.createExprInt(BigInteger.ZERO));
		block.add(idleActionPortWrite);
	}
}
