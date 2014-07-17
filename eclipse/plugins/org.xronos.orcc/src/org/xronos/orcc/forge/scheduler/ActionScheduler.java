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
import java.util.List;

import net.sf.orcc.df.Action;
import net.sf.orcc.df.Actor;
import net.sf.orcc.df.State;
import net.sf.orcc.df.util.DfVisitor;
import net.sf.orcc.ir.Block;
import net.sf.orcc.ir.BlockBasic;
import net.sf.orcc.ir.BlockWhile;
import net.sf.orcc.ir.Expression;
import net.sf.orcc.ir.InstReturn;
import net.sf.orcc.ir.InstStore;
import net.sf.orcc.ir.IrFactory;
import net.sf.orcc.ir.Procedure;
import net.sf.orcc.ir.Type;
import net.sf.orcc.ir.Var;
import net.sf.orcc.ir.transform.BlockCombine;

import org.xronos.openforge.lim.Task;
import org.xronos.orcc.backend.debug.XronosDebug;
import org.xronos.orcc.forge.mapping.DesignMemory;
import org.xronos.orcc.forge.mapping.TaskProcedure;

/**
 * This visitor constructs the scheduling of actions in an actor
 * 
 * @author Endri Bezati
 *
 */
public class ActionScheduler extends DfVisitor<Task> {

	private Procedure scheduler;

	@Override
	public Task caseActor(Actor actor) {

		// -- The Actions Scheduler procedure
		Procedure scheduler = IrFactory.eINSTANCE.createProcedure("scheduler",
				0, IrFactory.eINSTANCE.createTypeVoid());

		// Init Blocks
		List<Block> initBlocks = new ArrayList<Block>();

		// -- Store FSM states
		BlockBasic storeSMStatesBlock = IrFactory.eINSTANCE.createBlockBasic();
		if (actor.hasFsm()) {
			for (State state : actor.getFsm().getStates()) {
				Type typeBool = IrFactory.eINSTANCE.createTypeBool();
				Var fsmState = IrFactory.eINSTANCE.createVar(typeBool,
						"fsmState_" + state.getName(), true, 0);
				Expression value = null;
				if (state == actor.getFsm().getInitialState()) {
					fsmState.setValue(true);
					value = IrFactory.eINSTANCE.createExprBool(true);
				} else {
					fsmState.setValue(false);
					value = IrFactory.eINSTANCE.createExprBool(false);
				}
				actor.getStateVars().add(fsmState);
				// Add to Design Memory
				DesignMemory.addToMemory(actor, fsmState);

				// Create Store Instruction
				InstStore store = IrFactory.eINSTANCE.createInstStore(fsmState,
						value);
				storeSMStatesBlock.add(store);
			}
			initBlocks.add(storeSMStatesBlock);
		}

		Block assignInputTokenIndexBlock = new TokenIndexBlock(scheduler).doSwitch(actor);
		initBlocks.add(assignInputTokenIndexBlock);

		// -- call of initialize action
		// TODO: Create call, input and output port resolution for each
		// initialize action
		for (@SuppressWarnings("unused")
		Action action : actor.getInitializes()) {
		}

		// -- Create loads for each fsm state
		Block assignFSMStatesBlock = actor.hasFsm() ? new LoadFsmStatesBlock(
				scheduler).doSwitch(actor) : null;

		// -- Create the isSchedulable Blocks
		List<Block> isScedulableBlocks = new IsSchedulableBlocks(scheduler)
				.doSwitch(actor);

		// -- Create the Status Block
		Block portStatusBlock = new PortStatusBlock(scheduler).doSwitch(actor);

		// -- Create the hasTokens Block
		Block hasTokensBlock = new HasTokensBlock(scheduler).doSwitch(actor);

		// -- Create the action selection blocks
		List<Block> actionSelection = new ActionSelection(scheduler)
				.doSwitch(actor);

		// -- Create the infinite while block
		BlockWhile inifiniteWhile = IrFactory.eINSTANCE.createBlockWhile();
		Expression condition = IrFactory.eINSTANCE.createExprBool(true);
		inifiniteWhile.setCondition(condition);

		// -- Add isSchedulable, hasToken and action selection blocks
		if (actor.hasFsm()) {
			inifiniteWhile.getBlocks().add(assignFSMStatesBlock);
		}

		inifiniteWhile.getBlocks().addAll(isScedulableBlocks);
		inifiniteWhile.getBlocks().add(portStatusBlock);
		inifiniteWhile.getBlocks().add(hasTokensBlock);
		inifiniteWhile.getBlocks().addAll(actionSelection);

		// -- Add init and infinite while blocks to the scheduler body
		scheduler.getBlocks().addAll(initBlocks);
		scheduler.getBlocks().add(inifiniteWhile);

		// -- Create a Return block and null return instruction on it
		BlockBasic returnBlock = IrFactory.eINSTANCE.createBlockBasic();
		InstReturn instReturn = IrFactory.eINSTANCE.createInstReturn(null);
		returnBlock.add(instReturn);
		// -- Add return block to the scheduler body
		scheduler.getBlocks().add(returnBlock);

		// -- Give a void return type to the scheduler
		Type returnType = IrFactory.eINSTANCE.createTypeVoid();
		scheduler.setReturnType(returnType);

		// Scheduler transformations
		// -- Combine blocks
		new BlockCombine().doSwitch(scheduler);
		this.scheduler = scheduler;
		new XronosDebug().printProcedure("/tmp", scheduler);
		Task schedulerTask = new TaskProcedure(true).doSwitch(scheduler);
		// Debug.depGraphTo(schedulerTask, "scheduler", "/tmp/schdeuler.dot",
		// 1);
		return schedulerTask;
	}

	public Procedure getScheduler() {
		return scheduler;
	}

}
