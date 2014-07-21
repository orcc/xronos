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

import java.util.HashMap;
import java.util.Map;

import org.xronos.orcc.forge.mapping.DesignMemory;

import net.sf.orcc.df.Actor;
import net.sf.orcc.df.State;
import net.sf.orcc.df.util.DfVisitor;
import net.sf.orcc.ir.Block;
import net.sf.orcc.ir.BlockBasic;
import net.sf.orcc.ir.InstAssign;
import net.sf.orcc.ir.InstStore;
import net.sf.orcc.ir.IrFactory;
import net.sf.orcc.ir.Procedure;
import net.sf.orcc.ir.Var;

/**
 * This class enumerates all the states and stores to the scheduler state the
 * initial state
 * 
 * @author Endri Bezati
 *
 */
public class EnumerateFSMStatesBlock extends DfVisitor<Block> {

	/**
	 * The Actions Scheduler procedure
	 */
	Procedure scheduler;

	public EnumerateFSMStatesBlock(Procedure scheduler) {
		this.scheduler = scheduler;
	}

	@Override
	public Block caseActor(Actor actor) {
		BlockBasic stateBlock = IrFactory.eINSTANCE.createBlockBasic();

		Map<Var, Integer> stateIndexes = new HashMap<Var, Integer>();

		int index = 0;
		for (State state : actor.getFsm().getStates()) {
			Var tempState = IrFactory.eINSTANCE.createVar(
					IrFactory.eINSTANCE.createTypeInt(),
					"s_fsmState_" + state.getName(), true, 0);
			stateIndexes.put(tempState, index);
			scheduler.addLocal(tempState);

			InstAssign assign = IrFactory.eINSTANCE.createInstAssign(tempState,
					index);
			stateBlock.add(assign);
			index++;
		}

		if (SchedulerUtil.actorHasInputPortWithRepeats(actor)) {
			Var readStateVar = IrFactory.eINSTANCE.createVar(
					IrFactory.eINSTANCE.createTypeInt(), "s_fsmState_read_"
							+ actor.getName(), false, 0);
			stateIndexes.put(readStateVar, index);
			scheduler.addLocal(readStateVar);

			InstAssign assign = IrFactory.eINSTANCE.createInstAssign(
					readStateVar, index);
			stateBlock.add(assign);

			// -- Create oldState variable
			Var oldState = IrFactory.eINSTANCE.createVar(
					IrFactory.eINSTANCE.createTypeInt(),
					"fsmOldState_" + actor.getName(), true, 0);
			actor.getStateVars().add(oldState);
			DesignMemory.addToMemory(actor, oldState);
			
			Var initState = scheduler.getLocal("s_fsmState_"
					+ actor.getFsm().getInitialState().getName());
			InstStore store = IrFactory.eINSTANCE.createInstStore(oldState, initState);
			stateBlock.add(store);
			index++;
		}

		if (SchedulerUtil.actorHasOutputPortWithRepeats(actor)) {
			Var writeStateVar = IrFactory.eINSTANCE.createVar(
					IrFactory.eINSTANCE.createTypeInt(), "s_fsmState_write_"
							+ actor.getName(), false, 0);
			stateIndexes.put(writeStateVar, index);
			scheduler.addLocal(writeStateVar);

			InstAssign assign = IrFactory.eINSTANCE.createInstAssign(
					writeStateVar, index);
			stateBlock.add(assign);

			// -- Create oldState variable
			if (actor.getStateVar("fsmOldState_" + actor.getName()) == null) {
				Var oldState = IrFactory.eINSTANCE.createVar(
						IrFactory.eINSTANCE.createTypeInt(), "fsmOldState_"
								+ actor.getName(), true, 0);
				actor.getStateVars().add(oldState);
				DesignMemory.addToMemory(actor, oldState);
				

				Var initState = scheduler.getLocal("s_fsmState_"
						+ actor.getFsm().getInitialState().getName());
				InstStore store = IrFactory.eINSTANCE.createInstStore(oldState, initState);
				stateBlock.add(store);
			}
			index++;
		}

		// -- Save stateIndexes attribute
		scheduler.setAttribute("stateIndexes", stateIndexes);

		// -- Create Block Basic with init state store
		Var state = IrFactory.eINSTANCE.createVar(
				IrFactory.eINSTANCE.createTypeInt(),
				"fsmState_" + actor.getName(), true, 0);
		actor.getStateVars().add(state);
		DesignMemory.addToMemory(actor, state);

		Var initState = scheduler.getLocal("s_fsmState_"
				+ actor.getFsm().getInitialState().getName());

		InstStore store = IrFactory.eINSTANCE.createInstStore(state, initState);

		stateBlock.add(store);

		return stateBlock;
	}
}
