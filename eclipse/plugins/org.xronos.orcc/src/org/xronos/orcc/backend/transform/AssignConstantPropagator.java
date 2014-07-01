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

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.util.EcoreUtil;

import net.sf.orcc.ir.ExprVar;
import net.sf.orcc.ir.Expression;
import net.sf.orcc.ir.InstAssign;
import net.sf.orcc.ir.InstPhi;
import net.sf.orcc.ir.Procedure;
import net.sf.orcc.ir.Use;
import net.sf.orcc.ir.util.AbstractIrVisitor;
import net.sf.orcc.ir.util.IrUtil;
import net.sf.orcc.util.Void;
import net.sf.orcc.util.util.EcoreHelper;

public class AssignConstantPropagator extends AbstractIrVisitor<Void> {

	Boolean isModified;

	@Override
	public Void caseInstAssign(InstAssign assign) {
		Boolean usedInPhi = false;
		Expression value = assign.getValue();
		if (value.isExprBool() || value.isExprFloat() || value.isExprInt()
				|| value.isExprString()) {
			EList<Use> targetUses = assign.getTarget().getVariable().getUses();
			while (!targetUses.isEmpty() && !usedInPhi) {
				InstPhi phi = EcoreHelper.getContainerOfType(targetUses.get(0),
						InstPhi.class);
				if (phi == null) {
					ExprVar expr = EcoreHelper.getContainerOfType(
							targetUses.get(0), ExprVar.class);
					EcoreUtil.replace(expr, IrUtil.copy(value));
					IrUtil.delete(expr);
				} else {
					usedInPhi = true;
				}
			}
			if (!usedInPhi) {
				isModified = true;
				IrUtil.delete(assign);
				indexInst--;
			}
		}
		return null;
	}

	@Override
	public Void caseProcedure(Procedure procedure) {
		do {
			isModified = false;
			super.caseProcedure(procedure);
		} while (isModified);
		return null;
	}

}
