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
 * If you modify this Program, or any covered work, by linking or combining it
 * with Eclipse (or a modified version of Eclipse or an Eclipse plugin or 
 * an Eclipse library), containing parts covered by the terms of the 
 * Eclipse Public License (EPL), the licensors of this Program grant you 
 * additional permission to convey the resulting work.  Corresponding Source 
 * for a non-source form of such a combination shall include the source code 
 * for the parts of Eclipse libraries used as well as that of the  covered work.
 * 
 */
package org.xronos.orcc.forge.scheduler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.sf.orcc.df.Action;
import net.sf.orcc.df.Actor;
import net.sf.orcc.df.FSM;
import net.sf.orcc.df.Port;
import net.sf.orcc.df.State;
import net.sf.orcc.df.Transition;
import net.sf.orcc.df.util.DfVisitor;
import net.sf.orcc.graph.Edge;
import net.sf.orcc.ir.Block;
import net.sf.orcc.ir.BlockBasic;
import net.sf.orcc.ir.BlockIf;
import net.sf.orcc.ir.Def;
import net.sf.orcc.ir.ExprInt;
import net.sf.orcc.ir.Expression;
import net.sf.orcc.ir.InstCall;
import net.sf.orcc.ir.InstLoad;
import net.sf.orcc.ir.InstStore;
import net.sf.orcc.ir.IrFactory;
import net.sf.orcc.ir.OpBinary;
import net.sf.orcc.ir.Procedure;
import net.sf.orcc.ir.Type;
import net.sf.orcc.ir.Var;
import net.sf.orcc.ir.util.IrUtil;

import org.xronos.orcc.generic.Pair;
import org.xronos.orcc.ir.BlockMutex;
import org.xronos.orcc.ir.InstPortRead;
import org.xronos.orcc.ir.InstPortWrite;
import org.xronos.orcc.ir.XronosIrFactory;

/**
 * This visitor construct a list of blocks that handles the action selection
 * 
 * @author Endri Bezati
 * 
 */
public class ActionSelection extends DfVisitor<List<Block>> {

	Map<Action, Pair<State, State>> actionStateToState;

	/**
	 * The Actions Scheduler procedure
	 */
	Procedure scheduler;

	static int tempIndex = 0;

	public ActionSelection(Procedure scheduler) {
		this.scheduler = scheduler;
	}

	private BlockIf actionSelection(List<Action> actions, State state) {
		BlockIf firstBlockIf = null;
		BlockIf lastBlockIf = null;
		for (Action action : actions) {
			// -- Create the Fire-ability condition (isSchedulable &&
			// hasTokens)
			Var isSchedulable = scheduler.getLocal(action.getName()
					+ "IsSchedulable");
			Var hasTokens = scheduler.getLocal(action.getName() + "HasTokens");
			Expression E1 = IrFactory.eINSTANCE.createExprVar(isSchedulable);
			Expression E2 = IrFactory.eINSTANCE.createExprVar(hasTokens);

			Expression condition = IrFactory.eINSTANCE.createExprBinary(E1,
					OpBinary.LOGIC_AND, E2,
					IrFactory.eINSTANCE.createTypeBool());
			// -- Create BlockIf
			BlockIf blockIf = IrFactory.eINSTANCE.createBlockIf();
			blockIf.setCondition(condition);

			// -- Add action call and output pattern resolution
			blockIf.getThenBlocks().addAll(doSwitch(action));

			// Set firstBlockIf and lastBlockif
			if (firstBlockIf == null) {
				firstBlockIf = blockIf;
				lastBlockIf = blockIf;
			} else {
				lastBlockIf.getElseBlocks().add(blockIf);
				lastBlockIf = blockIf;
			}
		}

		// -- Else Block for reading
		if (SchedulerUtil.actorHasInputPortWithRepeats(actor)) {
			BlockBasic block = IrFactory.eINSTANCE.createBlockBasic();

			Map<Port, Integer> portDepth = new HashMap<Port, Integer>();
			Set<Port> inPorts = new HashSet<Port>();
			for (Action action : actions) {
				for (Port port : action.getInputPattern().getPorts()) {
					int numTokens = action.getInputPattern().getNumTokensMap()
							.get(port);
					if (portDepth.containsKey(port)) {
						if (portDepth.get(port) < numTokens) {
							portDepth.put(port, numTokens);
						}
					} else {
						portDepth.put(port, numTokens);
					}
				}
				inPorts.addAll(action.getInputPattern().getPorts());
			}

			for (Port port : inPorts) {
				if (portDepth.containsKey(port)) {
					int numTokens = portDepth.get(port);
					if (numTokens > 1) {
						Var portIndex = actor.getStateVar(port.getName()
								+ "TokenIndex");
						ExprInt value = IrFactory.eINSTANCE.createExprInt(0);
						InstStore store = IrFactory.eINSTANCE.createInstStore(
								portIndex, value);
						block.add(store);

						// Get MaxTokenIndex for this action
						Var maxTokenIndex = actor.getStateVar(port.getName()
								+ "MaxTokenIndex");
						value = IrFactory.eINSTANCE.createExprInt(numTokens);
						store = IrFactory.eINSTANCE.createInstStore(
								maxTokenIndex, value);
						block.add(store);
					}
				}
			}

			// -- Store current state to the old state
			Var stateVar = scheduler.getLocal("s_fsmState_" + state.getName());
			Var target = actor.getStateVar("fsmOldState_" + actor.getName());
			InstStore store = IrFactory.eINSTANCE.createInstStore(target,
					stateVar);
			block.add(store);

			// -- Now store to the current state the read state
			Var readVar = scheduler.getLocal("s_fsmState_read_"
					+ actor.getName());
			target = actor.getStateVar("fsmState_" + actor.getName());
			store = IrFactory.eINSTANCE.createInstStore(target, readVar);
			block.add(store);
			lastBlockIf.getElseBlocks().add(block);
		}

		return firstBlockIf;
	}

	private List<Block> actionSelectionFSM(FSM fsm) {
		List<Block> blocks = new ArrayList<Block>();

		for (State state : actor.getFsm().getStates()) {
			List<Action> actions = new ArrayList<Action>();
			actionStateToState = new HashMap<Action, Pair<State, State>>();

			// -- Put all actions outside FSM if any
			// actions.addAll(actor.getActionsOutsideFsm());

			for (Edge edge : state.getOutgoing()) {
				Transition transition = ((Transition) edge);
				State source = transition.getSource();
				State target = transition.getTarget();
				Pair<State, State> pair = Pair.of(source, target);
				Action action = transition.getAction();
				actions.add(action);
				actionStateToState.put(action, pair);
			}

			// -- Create if block of the state add all actions
			Var currentState = scheduler.getLocal("fsmCurrentState_"
					+ actor.getName());
			Expression E1 = IrFactory.eINSTANCE.createExprVar(currentState);

			Var sStateVar = scheduler.getLocal("s_fsmState_" + state.getName());
			Expression E2 = IrFactory.eINSTANCE.createExprVar(sStateVar);

			Expression condition = IrFactory.eINSTANCE.createExprBinary(E1,
					OpBinary.EQ, E2, IrFactory.eINSTANCE.createTypeBool());

			BlockIf blockIf = IrFactory.eINSTANCE.createBlockIf();
			blockIf.setCondition(condition);
			blockIf.getThenBlocks().add(actionSelection(actions, state));

			// -- Add blockIf to blocks
			blocks.add(blockIf);
		}

		return blocks;
	}

	@Override
	public List<Block> caseAction(Action action) {
		List<Block> blocks = new ArrayList<Block>();

		// -- Block Basic with InstCall
		BlockBasic block = IrFactory.eINSTANCE.createBlockBasic();
		InstCall instCall = IrFactory.eINSTANCE.createInstCall();
		instCall.setProcedure(action.getBody());
		block.add(instCall);

		// -- add store instruction from saving state if any
		if (actionStateToState.containsKey(action)) {
			// Get source and target states
			State state = actionStateToState.get(action).getB();

			if (!SchedulerUtil.actorHasOutputPortWithRepeats(actor)) {
				Var source = scheduler
						.getLocal("s_fsmState_" + state.getName());
				Expression value = IrFactory.eINSTANCE.createExprVar(source);

				Var target = actor.getStateVar("fsmState_" + actor.getName());

				InstStore store = IrFactory.eINSTANCE.createInstStore(target,
						value);
				block.add(store);
			} else {
				// -- Store current state to the old state
				Var stateVar = scheduler.getLocal("s_fsmState_"
						+ state.getName());
				Var target = actor
						.getStateVar("fsmOldState_" + actor.getName());
				InstStore store = IrFactory.eINSTANCE.createInstStore(target,
						stateVar);
				block.add(store);

				// -- Now store to the current state the read state
				Var readVar = scheduler.getLocal("s_fsmState_write_"
						+ actor.getName());
				target = actor.getStateVar("fsmState_" + actor.getName());
				store = IrFactory.eINSTANCE.createInstStore(target, readVar);
				block.add(store);

			}
		}

		// -- Initialize at Zero the portTokenIndex for the input pattern
		if (action.getInputPattern() != null) {

			for (Port port : action.getInputPattern().getPorts()) {
				if (action.getInputPattern().getNumTokens(port) > 1) {
					Var tokenIndex = actor.getStateVar(port.getName()
							+ "TokenIndex");
					ExprInt value = IrFactory.eINSTANCE.createExprInt(0);
					InstStore store = IrFactory.eINSTANCE.createInstStore(
							tokenIndex, value);
					block.add(store);
				}
			}
		}

		// -- Create portTokenIndex for each port on the output pattern
		if (action.getOutputPattern() != null) {
			for (Port port : action.getOutputPattern().getPorts()) {
				if (action.getOutputPattern().getNumTokens(port) > 1) {
					Var portIndex = actor.getStateVar(port.getName()
							+ "TokenIndex");
					ExprInt value = IrFactory.eINSTANCE.createExprInt(0);
					InstStore store = IrFactory.eINSTANCE.createInstStore(
							portIndex, value);
					block.add(store);

					// Get MaxTokenIndex for this action
					Var maxTokenIndex = actor.getStateVar(port.getName()
							+ "MaxTokenIndex");
					int numTokens = action.getOutputPattern()
							.getNumTokens(port);
					value = IrFactory.eINSTANCE.createExprInt(numTokens);
					store = IrFactory.eINSTANCE.createInstStore(maxTokenIndex,
							value);
					block.add(store);
				}
			}
		}

		// -- Add block to blocks
		blocks.add(block);

		return blocks;
	}

	@Override
	public List<Block> caseActor(Actor actor) {
		this.actor = actor;

		List<Block> blocks = new ArrayList<Block>();

		BlockMutex blockMutex = XronosIrFactory.eINSTANCE.createBlockMutex();
		blockMutex.getBlocks().addAll(actionSelectionFSM(actor.getFsm()));

		// -- Create BlockIf for reading on ports with repeats
		if (SchedulerUtil.actorHasInputPortWithRepeats(actor)) {
			BlockIf readIf = IrFactory.eINSTANCE.createBlockIf();
			Var currentState = scheduler.getLocal("fsmCurrentState_"
					+ actor.getName());
			Expression E1 = IrFactory.eINSTANCE.createExprVar(currentState);

			Var readState = scheduler.getLocal("s_fsmState_read_"
					+ actor.getName());
			Expression E2 = IrFactory.eINSTANCE.createExprVar(readState);

			Expression condition = IrFactory.eINSTANCE.createExprBinary(E1,
					OpBinary.EQ, E2, IrFactory.eINSTANCE.createTypeBool());

			readIf.setCondition(condition);
			readIf.getThenBlocks().add(
					constructReadWriteBlock(actor.getInputs(), true));

			blockMutex.getBlocks().add(readIf);
		}

		// -- Create BlockIf for writing on ports with repeats
		if (SchedulerUtil.actorHasOutputPortWithRepeats(actor)) {
			BlockIf writeIf = IrFactory.eINSTANCE.createBlockIf();
			Var currentState = scheduler.getLocal("fsmCurrentState_"
					+ actor.getName());
			Expression E1 = IrFactory.eINSTANCE.createExprVar(currentState);

			Var readState = scheduler.getLocal("s_fsmState_write_"
					+ actor.getName());
			Expression E2 = IrFactory.eINSTANCE.createExprVar(readState);

			Expression condition = IrFactory.eINSTANCE.createExprBinary(E1,
					OpBinary.EQ, E2, IrFactory.eINSTANCE.createTypeBool());

			writeIf.setCondition(condition);
			writeIf.getThenBlocks().add(
					constructReadWriteBlock(actor.getOutputs(), false));

			blockMutex.getBlocks().add(writeIf);
		}

		blocks.add(blockMutex);
		return blocks;
	}

	private Block constructReadWriteBlock(List<Port> ports, Boolean isInput) {
		BlockMutex mutex = XronosIrFactory.eINSTANCE.createBlockMutex();

		// -- Get for all actors ports if it has repeats on inputs and outputs
		Map<Port, Boolean> portHasRepeat = SchedulerUtil
				.getPortWithRepeats(actor);
		for (Port port : ports) {
			if (portHasRepeat.get(port)) {
				// -- Create blockIf condition
				Var tmpTokenIndex = scheduler.getLocal("tmp_" + port.getName()
						+ "TokenIndex");
				Var tmpMaxTokenIndex = scheduler.getLocal("tmp_"
						+ port.getName() + "MaxTokenIndex");
				Expression E1 = IrFactory.eINSTANCE
						.createExprVar(tmpTokenIndex);
				Expression E2 = IrFactory.eINSTANCE
						.createExprVar(tmpMaxTokenIndex);

				Expression condition = IrFactory.eINSTANCE.createExprBinary(E1,
						OpBinary.LT, E2, IrFactory.eINSTANCE.createTypeBool());
				// -- Create blockIf
				BlockIf blockIf = IrFactory.eINSTANCE.createBlockIf();
				blockIf.setCondition(condition);

				// --Create blockIf then block
				BlockBasic block = IrFactory.eINSTANCE.createBlockBasic();

				Type type = IrUtil.copy(port.getType());
				Var target = null;
				if (scheduler.getLocal(port.getName() + "Portvalue") != null) {
					target = scheduler.getLocal(port.getName() + "Portvalue");
				} else {
					target = IrFactory.eINSTANCE.createVar(type, port.getName()
							+ "Portvalue", true, 0);
				}
				scheduler.addLocal(target);

				if (isInput) {
					Def def = IrFactory.eINSTANCE.createDef(target);
					// -- Read from the input port
					InstPortRead portRead = XronosIrFactory.eINSTANCE
							.createInstPortRead();
					portRead.setPort(port);
					portRead.setTarget(def);
					block.add(portRead);

					// -- Store the port value to the list var port
					Var storeTarget = actor.getStateVar(port.getName());
					Expression index = IrFactory.eINSTANCE
							.createExprVar(tmpTokenIndex);

					InstStore store = IrFactory.eINSTANCE.createInstStore(
							storeTarget, Arrays.asList(index), target);
					block.add(store);
				} else {
					Var source = actor.getStateVar(port.getName());
					Expression index = IrFactory.eINSTANCE
							.createExprVar(tmpTokenIndex);
					InstLoad load = IrFactory.eINSTANCE.createInstLoad(target,
							source, Arrays.asList(index));
					block.add(load);

					InstPortWrite portWrite = XronosIrFactory.eINSTANCE
							.createInstPortWrite();
					portWrite.setPort(port);

					Expression value = IrFactory.eINSTANCE
							.createExprVar(target);
					portWrite.setValue(value);
					block.add(portWrite);
				}

				// -- TokenIndex++
				Var tokenIndex = actor.getStateVar(port.getName()
						+ "TokenIndex");
				Expression indexPlusOne = IrFactory.eINSTANCE.createExprBinary(
						IrFactory.eINSTANCE.createExprVar(tmpTokenIndex),
						OpBinary.PLUS, IrFactory.eINSTANCE.createExprInt(1),
						IrFactory.eINSTANCE.createTypeInt());
				InstStore store = IrFactory.eINSTANCE.createInstStore(
						tokenIndex, indexPlusOne);
				block.add(store);

				// -- Status If
				Var portStatus = scheduler.getLocal(port.getName() + "Status");
				condition = IrFactory.eINSTANCE.createExprVar(portStatus);
				BlockIf statusIf = IrFactory.eINSTANCE.createBlockIf();
				statusIf.setCondition(condition);
				statusIf.getThenBlocks().add(block);

				// -- Add statusIf to then block
				blockIf.getThenBlocks().add(statusIf);

				// -- Construct Else Block

				block = IrFactory.eINSTANCE.createBlockBasic();
				Var source = actor
						.getStateVar("fsmOldState_" + actor.getName());
				Var temp = IrFactory.eINSTANCE.createVar(
						IrFactory.eINSTANCE.createTypeInt(),
						"temp_" + source.getName() + "_" + tempIndex, true, 0);
				scheduler.addLocal(temp);
				tempIndex++;

				InstLoad load = IrFactory.eINSTANCE
						.createInstLoad(temp, source);
				block.add(load);

				target = actor.getStateVar("fsmState_" + actor.getName());
				store = IrFactory.eINSTANCE.createInstStore(target, temp);
				block.add(store);

				// -- Add else block to if block
				blockIf.getElseBlocks().add(block);

				// -- Add if block to the mutex
				mutex.getBlocks().add(blockIf);
			}
		}
		return mutex;
	}
}
