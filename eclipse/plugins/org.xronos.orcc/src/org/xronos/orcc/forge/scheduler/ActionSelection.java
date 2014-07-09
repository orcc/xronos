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
import java.util.List;
import java.util.Map;

import net.sf.orcc.df.Action;
import net.sf.orcc.df.Actor;
import net.sf.orcc.df.FSM;
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
import net.sf.orcc.ir.ExprInt;
import net.sf.orcc.ir.Expression;
import net.sf.orcc.ir.InstAssign;
import net.sf.orcc.ir.InstCall;
import net.sf.orcc.ir.InstLoad;
import net.sf.orcc.ir.InstStore;
import net.sf.orcc.ir.IrFactory;
import net.sf.orcc.ir.OpBinary;
import net.sf.orcc.ir.Procedure;
import net.sf.orcc.ir.Type;
import net.sf.orcc.ir.Var;
import net.sf.orcc.ir.util.IrUtil;
import net.sf.orcc.util.util.EcoreHelper;

import org.xronos.orcc.generic.Pair;
import org.xronos.orcc.ir.BlockMutex;
import org.xronos.orcc.ir.InstPortRead;
import org.xronos.orcc.ir.InstPortStatus;
import org.xronos.orcc.ir.InstPortWrite;
import org.xronos.orcc.ir.XronosIrFactory;

/**
 * This visitor construct a list of blocks that handles the action selection
 * 
 * @author Endri Bezati
 * 
 */
public class ActionSelection extends DfVisitor<List<Block>> {

	/**
	 * The Actions Scheduler procedure
	 */
	Procedure scheduler;

	Map<Action, Pair<State, State>> actionStateToState;

	public ActionSelection(Procedure scheduler) {
		this.scheduler = scheduler;
	}

	@Override
	public List<Block> caseActor(Actor actor) {
		this.actor = actor;

		List<Block> blocks = new ArrayList<Block>();
		if (!actor.hasFsm()) {
			blocks.add(actionSelection(actor.getActionsOutsideFsm()));
		} else {
			blocks.addAll(actionSelectionFSM(actor.getFsm()));
		}
		return blocks;
	}

	private BlockIf actionSelection(List<Action> actions) {
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
			} else {
				// If lastBlockif is not null, resolve priority by adding
				// the blockIf to the else blockIf
				if (lastBlockIf == null) {
					lastBlockIf = blockIf;
				} else {
					lastBlockIf.getElseBlocks().add(blockIf);
					lastBlockIf = blockIf;
				}
			}
		}
		// -- Handle Input pattern reading and add them to lastBlockIf
		for (Action action : actions) {
			Pattern pattern = action.getInputPattern();
			lastBlockIf.getElseBlocks().addAll(doSwitch(pattern));
		}
		return firstBlockIf;
	}

	private List<Block> actionSelectionFSM(FSM fsm) {
		List<Block> blocks = new ArrayList<Block>();

		for (State state : actor.getFsm().getStates()) {
			List<Action> actions = new ArrayList<Action>();
			actionStateToState = new HashMap<Action, Pair<State, State>>();

			// -- Put all actions outside FSM if any
			actions.addAll(actor.getActionsOutsideFsm());

			for (Edge edge : state.getOutgoing()) {
				Transition transition = ((Transition) edge);
				State target = transition.getTarget();
				State source = transition.getSource();
				Pair<State, State> pair = Pair.of(target, source);
				Action action = transition.getAction();
				actions.add(action);
				actionStateToState.put(action, pair);
			}
			
			// TODO: create state if and add the if of action selection

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

		// TODO: add store instruction from saving state if any
		
		// -- Initialize at Zero the portTokenIndex for the input pattern
		if (action.getInputPattern() != null) {
			for (Port port : action.getInputPattern().getPorts()) {
				Var portIndex = null;
				if (scheduler.getLocal(port.getName() + "TokenIndex") != null) {
					portIndex = scheduler.getLocal(port.getName()
							+ "TokenIndex");
				} else {
					portIndex = IrFactory.eINSTANCE.createVar(
							IrFactory.eINSTANCE.createTypeInt(), port.getName()
									+ "TokenIndex", true, 0);
					scheduler.addLocal(portIndex);
				}
				ExprInt value = IrFactory.eINSTANCE.createExprInt(0);
				InstAssign assign = IrFactory.eINSTANCE.createInstAssign(
						portIndex, value);
				block.add(assign);
			}
		}

		// -- Create portTokenIndex for each port on the output pattern
		if (action.getOutputPattern() != null) {
			for (Port port : action.getOutputPattern().getPorts()) {
				Var portIndex = null;
				if (scheduler.getLocal(port.getName() + "TokenIndex") != null) {
					portIndex = scheduler.getLocal(port.getName()
							+ "TokenIndex");
				} else {
					portIndex = IrFactory.eINSTANCE.createVar(
							IrFactory.eINSTANCE.createTypeInt(), port.getName()
									+ "TokenIndex", true, 0);
					scheduler.addLocal(portIndex);
				}
				ExprInt value = IrFactory.eINSTANCE.createExprInt(0);
				InstAssign assign = IrFactory.eINSTANCE.createInstAssign(
						portIndex, value);
				block.add(assign);
			}
		}

		// -- Add block to blocks
		blocks.add(block);

		// -- Build output pattern while Block
		if (action.getOutputPattern() != null) {
			if (action.getOutputPattern().getPorts().size() == 1) {
				blocks.addAll(doSwitch(action.getOutputPattern()));
			} else {
				BlockMutex mutex = XronosIrFactory.eINSTANCE.createBlockMutex();
				mutex.getBlocks().addAll(doSwitch(action.getOutputPattern()));
				blocks.add(mutex);
			}
		}
		return blocks;
	}

	@Override
	public List<Block> casePattern(Pattern pattern) {
		// -- Check if this an inputPattern
		Action action = EcoreHelper.getContainerOfType(pattern, Action.class);
		if (action == null) {
			throw (new NullPointerException(
					"Pattern is not contained in an action!"));
		}
		boolean isInputPattern = action.getInputPattern() == pattern;

		List<Block> blocks = new ArrayList<Block>();

		for (Port port : pattern.getPorts()) {
			if (pattern.getNumTokens(port) == 1) {
				BlockBasic blockBasic = IrFactory.eINSTANCE.createBlockBasic();
				Var target = null;
				if (scheduler.getLocal(port.getName() + "PortStatus") != null) {
					target = scheduler.getLocal(port.getName() + "PortStatus");
				} else {
					target = IrFactory.eINSTANCE.createVar(
							IrFactory.eINSTANCE.createTypeBool(),
							port.getName() + "PortStatus", true, 0);
					scheduler.addLocal(target);
				}
				// -- Create Port Status
				InstPortStatus portStatus = XronosIrFactory.eINSTANCE
						.createInstPortStatus();
				Def def = IrFactory.eINSTANCE.createDef(target);
				portStatus.setTarget(def);
				portStatus.setPort(port);
				blockBasic.add(portStatus);
				blocks.add(blockBasic);

				// -- Create Block If
				Expression condition = IrFactory.eINSTANCE
						.createExprVar(target);
				BlockIf blockIf = IrFactory.eINSTANCE.createBlockIf();
				blockIf.setCondition(condition);
				// -- Create Block basic for the then Block
				blockBasic = IrFactory.eINSTANCE.createBlockBasic();

				// -- Create Load and then Port Write
				if (scheduler.getLocal(port.getName() + "PortValue") != null) {
					target = scheduler.getLocal(port.getName() + "Portvalue");
				} else {
					Type type = IrUtil.copy(port.getType());
					target = IrFactory.eINSTANCE.createVar(type, port.getName()
							+ "PortValue", true, 0);
					scheduler.addLocal(target);
				}

				def = IrFactory.eINSTANCE.createDef(target);

				if (isInputPattern) {
					// -- Read from the input port to temporary variable
					InstPortRead portRead = XronosIrFactory.eINSTANCE
							.createInstPortRead();
					portRead.setPort(port);
					portRead.setTarget(def);
					blockBasic.add(portRead);

					// -- Store the value to the port var
					Var storeTarget = pattern.getPortToVarMap().get(port);
					InstStore store = IrFactory.eINSTANCE.createInstStore(
							storeTarget, target);
					blockBasic.add(store);

					// -- TokenIndex == 1
					target = scheduler.getLocal(port.getName() + "TokenIndex");
					InstAssign assign = IrFactory.eINSTANCE.createInstAssign(
							target, 1);
					blockBasic.add(assign);
				} else {
					// -- Load Value to a temporary variable
					Var source = pattern.getPortToVarMap().get(port);
					InstLoad load = IrFactory.eINSTANCE.createInstLoad(target,
							source, 0);
					blockBasic.add(load);

					// -- Write the temporary variable to output port
					InstPortWrite portWrite = XronosIrFactory.eINSTANCE
							.createInstPortWrite();
					portWrite.setPort(port);
					Expression value = IrFactory.eINSTANCE
							.createExprVar(target);
					portWrite.setValue(value);
					blockBasic.add(portWrite);
				}

				// -- Add blockBasic to BlockIf then Blocks
				blockIf.getThenBlocks().add(blockBasic);
				// -- Add blockIf to Blocks
				blocks.add(blockIf);

			} else {
				// -- Create While Condition
				Var tokenIndex = scheduler.getLocal(port.getName()
						+ "TokenIndex");
				Expression E1 = IrFactory.eINSTANCE.createExprVar(tokenIndex);
				Expression E2 = IrFactory.eINSTANCE.createExprInt(pattern
						.getNumTokens(port));
				Expression condition = IrFactory.eINSTANCE.createExprBinary(E1,
						OpBinary.LE, E2, IrFactory.eINSTANCE.createTypeBool());

				// -- Create While
				BlockWhile blockWhile = IrFactory.eINSTANCE.createBlockWhile();
				blockWhile.setCondition(condition);

				// Create BlockBasic for PinStatus
				BlockBasic blockBasic = IrFactory.eINSTANCE.createBlockBasic();
				Var target = null;
				if (scheduler.getLocal(port.getName() + "PortStatus") != null) {
					target = scheduler.getLocal(port.getName() + "PortStatus");
				} else {
					target = IrFactory.eINSTANCE.createVar(
							IrFactory.eINSTANCE.createTypeBool(),
							port.getName() + "PortStatus", true, 0);
					scheduler.addLocal(target);
				}
				// -- Create Port Status
				InstPortStatus portStatus = XronosIrFactory.eINSTANCE
						.createInstPortStatus();
				Def def = IrFactory.eINSTANCE.createDef(target);
				portStatus.setTarget(def);
				portStatus.setPort(port);
				blockBasic.add(portStatus);
				blockWhile.getBlocks().add(blockBasic);

				// -- Create Block If
				condition = IrFactory.eINSTANCE.createExprVar(target);
				BlockIf blockIf = IrFactory.eINSTANCE.createBlockIf();
				blockIf.setCondition(condition);
				// -- Create Block basic for the then Block
				blockBasic = IrFactory.eINSTANCE.createBlockBasic();

				// -- Create Load and then Port Write
				if (scheduler.getLocal(port.getName() + "Portvalue") != null) {
					target = scheduler.getLocal(port.getName() + "Portvalue");
				} else {
					Type type = IrUtil.copy(port.getType());
					target = IrFactory.eINSTANCE.createVar(type, port.getName()
							+ "Portvalue", true, 0);
					scheduler.addLocal(target);
				}

				def = IrFactory.eINSTANCE.createDef(target);
				Expression index = IrFactory.eINSTANCE
						.createExprVar(tokenIndex);

				if (isInputPattern) {
					// -- Read from the input port
					InstPortRead portRead = XronosIrFactory.eINSTANCE
							.createInstPortRead();
					portRead.setPort(port);
					portRead.setTarget(def);
					blockBasic.add(portRead);

					// -- Store the port value to the list var port
					Var storeTarget = pattern.getPortToVarMap().get(port);
					InstStore store = IrFactory.eINSTANCE.createInstStore(
							storeTarget, Arrays.asList(index), target);
					blockBasic.add(store);
				} else {
					// -- Load the value from the list var port to temporary
					// variable
					Var source = pattern.getPortToVarMap().get(port);
					InstLoad load = IrFactory.eINSTANCE.createInstLoad(target,
							source, Arrays.asList(index));
					blockBasic.add(load);

					// -- Write the temporary variable to the output port
					InstPortWrite portWrite = XronosIrFactory.eINSTANCE
							.createInstPortWrite();
					portWrite.setPort(port);
					Expression value = IrFactory.eINSTANCE
							.createExprVar(target);
					portWrite.setValue(value);
					blockBasic.add(portWrite);
				}
				// -- Increment the token index
				E1 = IrFactory.eINSTANCE.createExprVar(tokenIndex);
				E2 = IrFactory.eINSTANCE.createExprInt(1);

				InstAssign assign = IrFactory.eINSTANCE.createInstAssign(
						tokenIndex, IrFactory.eINSTANCE.createExprBinary(E1,
								OpBinary.PLUS, E2,
								IrFactory.eINSTANCE.createTypeInt()));
				blockBasic.add(assign);

				// -- Add blockBasic to BlockIf then Blocks
				blockIf.getThenBlocks().add(blockBasic);
				// -- Add to block while blocks
				blockWhile.getBlocks().add(blockIf);
				// -- Add to blocks the while block
				blocks.add(blockWhile);
			}
		}

		return blocks;
	}
}
