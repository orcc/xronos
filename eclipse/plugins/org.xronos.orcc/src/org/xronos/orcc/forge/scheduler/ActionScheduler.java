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
import net.sf.orcc.ir.IrFactory;
import net.sf.orcc.ir.Procedure;
import net.sf.orcc.ir.Type;
import net.sf.orcc.ir.Var;

import org.xronos.openforge.lim.Task;
import org.xronos.orcc.forge.mapping.DesignMemory;

/**
 * This visitor constructs the scheduling of actions in an actor
 * 
 * @author Endri Bezati
 *
 */
public class ActionScheduler extends DfVisitor<Task> {

	@Override
	public Task caseActor(Actor actor) {

		Procedure scheduler = IrFactory.eINSTANCE.createProcedure("scheduler",
				0, IrFactory.eINSTANCE.createTypeVoid());

		// Create actor FSM states if any
		if(actor.hasFsm()){
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
				// Add to Design Memory
				DesignMemory.addToMemory(actor, fsmState);
			}
		}
		
		// -- Create the InitBlock, FSM states and call of initialize action
		List<Block> initBlocks = new ArrayList<Block>();
		for(Action action: actor.getInitializes()){
		}
		BlockBasic initFSMStatesBlock = IrFactory.eINSTANCE.createBlockBasic();
		
		
		List<Block> isScedulableBlocks = new IsSchedulableBlocks(scheduler)
				.doSwitch(actor);
		
		List<Block> actionSelection = new ActionSelection(scheduler).doSwitch(actor);

		Task schedulerTask = null;
		return schedulerTask;
	}

}
