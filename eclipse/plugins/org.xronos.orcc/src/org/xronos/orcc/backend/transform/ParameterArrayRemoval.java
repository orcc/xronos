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

package org.xronos.orcc.backend.transform;

import net.sf.orcc.df.Actor;
import net.sf.orcc.df.util.DfVisitor;
import net.sf.orcc.ir.Def;
import net.sf.orcc.ir.IrFactory;
import net.sf.orcc.ir.Use;
import net.sf.orcc.ir.Var;

import org.eclipse.emf.common.util.EList;

/**
 * This transformation transforms parameter arrays to state variables
 * 
 * @author Endri Bezati
 * 
 */
public class ParameterArrayRemoval extends DfVisitor<Void> {

	@Override
	public Void caseActor(Actor actor) {

		for (Var var : actor.getParameters()) {
			if (var.getType().isList()) {
				Var newVar = IrFactory.eINSTANCE.createVar(var.getType(),
						var.getName() + "_param", true, var.getIndex());
				newVar.setInitialValue(var.getInitialValue());
				newVar.setValue(var.getValue());
				newVar.setAssignable(false);

				EList<Use> uses = var.getUses();
				while (!uses.isEmpty()) {
					uses.get(0).setVariable(newVar);
				}
				EList<Def> defs = var.getDefs();
				while (!defs.isEmpty()) {
					defs.get(0).setVariable(newVar);
				}
				actor.getStateVars().add(newVar);
			}
		}
		return null;
	}

}
