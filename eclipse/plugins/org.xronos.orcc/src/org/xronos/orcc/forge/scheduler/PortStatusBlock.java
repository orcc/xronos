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

import net.sf.orcc.df.Actor;
import net.sf.orcc.df.Port;
import net.sf.orcc.df.util.DfVisitor;
import net.sf.orcc.ir.Block;
import net.sf.orcc.ir.BlockBasic;
import net.sf.orcc.ir.Def;
import net.sf.orcc.ir.IrFactory;
import net.sf.orcc.ir.Procedure;
import net.sf.orcc.ir.Var;

import org.xronos.orcc.ir.InstPortStatus;
import org.xronos.orcc.ir.XronosIrFactory;

/**
 * This visitor visits an actor and it creates a block that will contain for
 * each action a InstPortStatus if the action has any ports.
 * @author Endri Bezati
 *
 */
public class PortStatusBlock extends DfVisitor<Block> {

	/**
	 * The Actions Scheduler procedure
	 */
	Procedure scheduler;
	
	public PortStatusBlock(Procedure scheduler){
		this.scheduler = scheduler;
	}
	
	
	@Override
	public Block caseActor(Actor actor) {
		BlockBasic block = IrFactory.eINSTANCE.createBlockBasic();
		
		List<Port> ports = new ArrayList<Port>();
		// Get inputs and output port of the actor		
		ports.addAll(actor.getInputs());
		ports.addAll(actor.getOutputs());
		
		for(Port port: ports){
			// -- Create status Variable and add it as local to the scheduler
			Var status =  IrFactory.eINSTANCE.createVar(
					IrFactory.eINSTANCE.createTypeBool(), port.getName()
					+ "Status", true, 0);
			scheduler.addLocal(status);
			Def def = IrFactory.eINSTANCE.createDef(status);
			
			// -- Create InstPortStatus
			InstPortStatus portStatus = XronosIrFactory.eINSTANCE.createInstPortStatus();
			portStatus.setPort(port);
			portStatus.setTarget(def);
			
			// -- Add InstPortStatus to block
			block.add(portStatus);
		}
		return block;
	}	
}
