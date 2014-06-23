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

package org.xronos.orcc.forge.mapping;

import net.sf.orcc.df.Action;
import net.sf.orcc.df.util.DfVisitor;
import net.sf.orcc.ir.Procedure;

import org.xronos.openforge.lim.Task;

/**
 * This class visits all actions of an Actor and translates them to Tasks and
 * finally adds them to the Design
 * 
 * @author Endri Bezati
 * 
 */
public class DesignAction extends DfVisitor<Task> {

	@Override
	public Task caseAction(Action action) {
		Procedure procedure = action.getBody();
		TaskProcedure taskProcedure = new TaskProcedure(false);
		Task task = taskProcedure.doSwitch(procedure);

		// Add task attribute to action
		action.setAttribute("task", task);

		return task;
	}

}
