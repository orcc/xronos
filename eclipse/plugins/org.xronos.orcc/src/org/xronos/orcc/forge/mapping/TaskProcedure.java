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

import net.sf.orcc.df.Actor;
import net.sf.orcc.ir.Procedure;
import net.sf.orcc.ir.util.AbstractIrVisitor;
import net.sf.orcc.util.util.EcoreHelper;

import org.xronos.openforge.app.project.SearchLabel;
import org.xronos.openforge.lim.Block;
import org.xronos.openforge.lim.Call;
import org.xronos.openforge.lim.CodeLabel;
import org.xronos.openforge.lim.Exit;
import org.xronos.openforge.lim.Task;
import org.xronos.openforge.util.naming.IDSourceInfo;
import org.xronos.orcc.forge.mapping.cdfg.ProcedureToBlock;

public class TaskProcedure extends AbstractIrVisitor<Task> {

	boolean requiresKicker;

	public TaskProcedure(boolean requiresKicker) {
		this.requiresKicker = requiresKicker;
	}

	@Override
	public Task caseProcedure(Procedure procedure) {
		String methodName = procedure.getName();

		// Build Task Module, for each block in the procedure
		ProcedureToBlock procedureToModule = new ProcedureToBlock(true);
		Block block = procedureToModule.doSwitch(procedure);
		
		org.xronos.openforge.lim.Procedure proc = new org.xronos.openforge.lim.Procedure(
				block);

		// Get Actor information
		Actor actor = EcoreHelper.getContainerOfType(procedure, Actor.class);
		if (actor != null) {
			// Set the IDSourceInfo
			String fileName = actor.getFileName();
			String packageName = actor.getPackage();
			String className = actor.getFile().getProjectRelativePath()
					.removeFirstSegments(1).removeFileExtension().toString()
					.replace("/", ".");
			int line = procedure.getLineNumber();
			proc.setIDSourceInfo(new IDSourceInfo(fileName, packageName,
					className, methodName, "", line, 0));
		} else {
			proc.setIDSourceInfo(new IDSourceInfo(null, null, null, methodName,
					"", -1, -1));
		}

		// Create LIM CALL
		Call call = proc.makeCall();
		SearchLabel sl = new CodeLabel(proc, methodName);
		proc.setSearchLabel(sl);
		call.setSourceName(methodName);
		call.setIDLogical(methodName);
		//  Sets the sizes of the clock,reset and go ports of a call
		call.getClockPort().setSize(1, false);
		call.getResetPort().setSize(1, false);
		call.getGoPort().setSize(1, false);

		// Create LIM Task
		Task task = new Task(call);
		task.setKickerRequired(requiresKicker);
		task.setSourceName(methodName);
		task.setIDLogical(methodName);

		return task;
	}

}
