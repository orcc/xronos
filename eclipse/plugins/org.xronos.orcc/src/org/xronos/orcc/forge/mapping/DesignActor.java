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

package org.xronos.orcc.forge.mapping;

import net.sf.orcc.df.Action;
import net.sf.orcc.df.Actor;
import net.sf.orcc.df.util.DfVisitor;

import org.xronos.openforge.app.EngineThread;
import org.xronos.openforge.app.GenericJob;
import org.xronos.openforge.app.OptionRegistry;
import org.xronos.openforge.lim.Call;
import org.xronos.openforge.lim.Design;
import org.xronos.openforge.lim.Exit;
import org.xronos.openforge.lim.Task;
import org.xronos.orcc.backend.debug.XronosDebug;
import org.xronos.orcc.forge.scheduler.ActionScheduler;

/**
 * This Visitor will construct a LIM Design from an Actor
 * 
 * @author Endri Bezati
 * 
 */
public class DesignActor extends DfVisitor<Design> {

	private static boolean debug = false;

	@Override
	public Design caseActor(Actor actor) {
		// Copy Actor and work to the copied actors
		// Eliminate the given attributes once the
		// transformation has finished
		Actor cActor = actor;// IrUtil.copy(actor);

		String designName = cActor.getName();

		// Create a New Design
		Design design = new Design();
		design.setIDLogical(designName);

		GenericJob job = EngineThread.getGenericJob();
		job.getOption(OptionRegistry.TOP_MODULE_NAME).setValue(
				design.getSearchLabel(), designName);

		// Construct Design Ports
		DesignPorts designPorts = new DesignPorts(design);
		designPorts.doSwitch(cActor);

		// Allocate Memory
		DesignMemory designMemory = new DesignMemory(design, false);
		long t0 = System.currentTimeMillis();
		designMemory.doSwitch(cActor);
		long t1 = System.currentTimeMillis();
		if (debug) {
			System.out.println(("\t - DesignMemory took : " + (float) (t1 - t0)
					/ 1000 + "s"));
		}

		// Build Tasks (Action to Tasks)
		for (Action action : cActor.getActions()) {
			t0 = System.currentTimeMillis();
			Task task = new DesignAction().doSwitch(action);
			t1 = System.currentTimeMillis();
			design.addTask(task);
			if (debug) {
				System.out.println(("\t -Action: " + action.getName()
						+ " LIMed in: " + (float) (t1 - t0) / 1000 + "s"));
			}

		}

		// Set attribute design to actor
		cActor.setAttribute("design", design);

		// Build Action Scheduler
		ActionScheduler actionScheduler = new ActionScheduler();
		t0 = System.currentTimeMillis();
		Task scheduler = actionScheduler.doSwitch(cActor);
		design.addTask(scheduler);
		t1 = System.currentTimeMillis();
		if (debug) {
			System.out.println(("\t -Action: LIMed in: " + (float) (t1 - t0)
					/ 1000 + "s"));
		}
		new XronosDebug().printActor("/tmp", cActor,
				actionScheduler.getScheduler());

		// Activate the production of GO/Done for each task
		for (Task task : design.getTasks()) {
			Call call = task.getCall();
			if (call.getExit(Exit.DONE).getDoneBus().isConnected()) {
				call.getProcedure().getBody().setProducesDone(true);
			}
			if (call.getGoPort().isConnected()) {
				call.getProcedure().getBody().setConsumesGo(true);
			}
		}

		return design;
	}

}
