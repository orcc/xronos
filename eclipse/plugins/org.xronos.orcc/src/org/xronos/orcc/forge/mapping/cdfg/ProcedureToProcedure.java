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
package org.xronos.orcc.forge.mapping.cdfg;

import org.xronos.openforge.lim.Block;
import org.xronos.openforge.lim.Procedure;
import org.xronos.openforge.util.naming.IDSourceInfo;

import net.sf.orcc.df.Actor;
import net.sf.orcc.ir.util.AbstractIrVisitor;
import net.sf.orcc.util.util.EcoreHelper;

/**
 * This visitor takes an Procedure and it transforms it to a LIM Procedure
 * @author Endri Bezati
 *
 */
public class ProcedureToProcedure extends AbstractIrVisitor<Procedure> {

	@Override
	public Procedure caseProcedure(net.sf.orcc.ir.Procedure procedure) {
		String methodName = procedure.getName();
		
		ProcedureToBlock procedureToModule = new ProcedureToBlock(true);
		Block body = procedureToModule.doSwitch(procedure);
		
		boolean hasReturnValue = false;
		if(!procedure.getReturnType().isVoid()){
			hasReturnValue = true;
		}
		
		Procedure proc = new Procedure(body, hasReturnValue);
		proc.setIDLogical(methodName);
		
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
		return proc;
	}

	
	
}
