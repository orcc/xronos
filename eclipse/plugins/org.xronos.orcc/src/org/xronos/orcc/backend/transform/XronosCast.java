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

package org.xronos.orcc.backend.transform;

import net.sf.orcc.backends.ir.InstCast;
import net.sf.orcc.backends.ir.IrSpecificFactory;
import net.sf.orcc.backends.transform.CastAdder;
import net.sf.orcc.df.Port;
import net.sf.orcc.ir.Expression;
import net.sf.orcc.ir.Type;
import net.sf.orcc.ir.Var;

import org.eclipse.emf.ecore.EObject;
import org.xronos.orcc.ir.BlockMutex;
import org.xronos.orcc.ir.InstPortPeek;
import org.xronos.orcc.ir.InstPortRead;
import org.xronos.orcc.ir.InstPortWrite;

/**
 * Adding Cast for Xronos port extension
 * 
 * @author endrix
 * 
 */
public class XronosCast extends CastAdder {

	public XronosCast(boolean castToUnsigned, boolean createEmptyBlockBasic) {
		super(castToUnsigned, createEmptyBlockBasic);
	}

	@Override
	public Expression defaultCase(EObject object) {
		if (object instanceof BlockMutex) {
			doSwitch(((BlockMutex) object).getBlocks());

		} else if (object instanceof InstPortRead) {

			InstPortRead instPortRead = (InstPortRead) object;

			Type uncastedType = ((Port) instPortRead.getPort()).getType();

			Var target = instPortRead.getTarget().getVariable();

			if (needCast(target.getType(), uncastedType)) {
				Var castedTarget = procedure.newTempLocalVariable(
						target.getType(), "casted_" + target.getName());
				castedTarget.setIndex(1);

				target.setType(uncastedType);

				InstCast cast = IrSpecificFactory.eINSTANCE.createInstCast(
						target, castedTarget);
				instPortRead.getBlock().add(indexInst + 1, cast);
			}
		} else if (object instanceof InstPortPeek) {

			InstPortPeek instPortPeek = (InstPortPeek) object;

			Type uncastedType = ((Port) instPortPeek.getPort()).getType();

			Var target = instPortPeek.getTarget().getVariable();

			if (needCast(target.getType(), uncastedType)) {
				Var castedTarget = procedure.newTempLocalVariable(
						target.getType(), "casted_" + target.getName());
				castedTarget.setIndex(1);

				target.setType(uncastedType);

				InstCast cast = IrSpecificFactory.eINSTANCE.createInstCast(
						target, castedTarget);
				instPortPeek.getBlock().add(indexInst + 1, cast);
			}
		} else if (object instanceof InstPortWrite) {
			InstPortWrite instPortWrite = (InstPortWrite) object;
			Type oldParentType = parentType;
			parentType = ((Port) instPortWrite.getPort()).getType();
			instPortWrite.setValue(doSwitch(instPortWrite.getValue()));
			parentType = oldParentType;
		}
		return null;
	}

}
