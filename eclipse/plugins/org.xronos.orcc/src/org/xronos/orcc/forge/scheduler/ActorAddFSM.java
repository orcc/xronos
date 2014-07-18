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

import java.util.Iterator;

import net.sf.orcc.df.Action;
import net.sf.orcc.df.Actor;
import net.sf.orcc.df.DfFactory;
import net.sf.orcc.df.FSM;
import net.sf.orcc.df.State;
import net.sf.orcc.df.Transition;
import net.sf.orcc.df.util.DfVisitor;
import net.sf.orcc.ir.util.IrUtil;

/**
 * This visitor will add an FSM to an actor if it does not have one. Will also
 * add all initialized actions to a state that is going to be executed only
 * once.
 * 
 * @author Endri Bezati
 *
 */
public class ActorAddFSM extends DfVisitor<Void> {

	@Override
	public Void caseActor(Actor actor) {
		// -- Add a finite state machine if an actor does not have one
		if (!actor.hasFsm()) {
			FSM fsm = DfFactory.eINSTANCE.createFSM();
			State sActor = DfFactory.eINSTANCE.createState(actor.getName());
			fsm.getStates().add(sActor);
			for(Action action: actor.getActionsOutsideFsm()){
				Transition t = DfFactory.eINSTANCE.createTransition(sActor, action, sActor);
				fsm.add(t);
			}
			fsm.setInitialState(sActor);
			actor.setFsm(fsm);
			actor.getActionsOutsideFsm().clear();
		}
		
		
		// -- Check if actor has initialize action, if yes add a state
		if(!actor.getInitializes().isEmpty()){
			FSM fsm = actor.getFsm();
			State init = DfFactory.eINSTANCE.createState(actor.getName()+"Init");
			fsm.getStates().add(init);
			State oldInitState = fsm.getInitialState();
			
			Iterator<Action> iter = actor.getInitializes().listIterator();
			
			while(iter.hasNext()){
				Action action = iter.next();
				Transition t = null;
				Action cAction = IrUtil.copy(action);
				if(iter.hasNext()){
					 t = DfFactory.eINSTANCE.createTransition(init, cAction, init);
				}else{
					 t = DfFactory.eINSTANCE.createTransition(init, cAction, oldInitState);
				}
				fsm.add(t);
				actor.getActions().add(cAction);
			}
			
			fsm.setInitialState(init);
			actor.getInitializes().clear();
		}
		
		return super.caseActor(actor);
	}

}
