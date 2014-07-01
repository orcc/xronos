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
 * If you modify this Program, or any covered work, by linking or 
 * combining it with Eclipse libraries (or a modified version of that 
 * library), containing parts covered by the terms of EPL,
 * the licensors of this Program grant you additional permission to convey 
 * the resulting work. {Corresponding Source for a non-source form of such 
 * a combination shall include the source code for the parts of Eclipse 
 * libraries used as well as that of the  covered work.}
 */

package org.xronos.orcc.forge.mapping;

import net.sf.orcc.df.Actor;
import net.sf.orcc.df.util.DfVisitor;
import net.sf.orcc.ir.Procedure;

import org.xronos.openforge.lim.Task;

/**
 * This Visitor will build a LIM {@link Task} of an {@link Actor} action
 * scheduler
 * 
 * @author Endri Bezati
 * 
 */
public class DesignActionScheduler extends DfVisitor<Task> {

	@Override
	public Task caseActor(Actor actor) {
		// Construct the action scheduler
		Procedure scheduler = null;

		// Build the Task of the scheduler
		TaskProcedure taskProcedure = new TaskProcedure(true);
		Task schedulerTask = taskProcedure.doSwitch(scheduler);
		return schedulerTask;
	}

}
