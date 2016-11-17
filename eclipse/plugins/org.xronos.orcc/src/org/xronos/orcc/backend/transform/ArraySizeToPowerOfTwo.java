/* 
 * XRONOS-EXELIXI
 * 
 * Copyright (C) 2011-2016 EPFL SCI STI MM
 *
 * This file is part of XRONOS-EXELIXI.
 *
 * XRONOS-EXELIXI is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * XRONOS-EXELIXI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with XRONOS-EXELIXI. If not, see <http://www.gnu.org/licenses/>.
 * 
 * Additional permission under GNU GPL version 3 section 7
 * 
 * If you modify this Program, or any covered work, by linking or combining it
 * with Eclipse (or a modified version of Eclipse or an Eclipse plugin or 
 * an Eclipse library), containing parts covered by the terms of the 
 * Eclipse Public License (EPL), the licensors of this Program grant you 
 * additional permission to convey the resulting work.  Corresponding Source 
 * for a non-source form of such a combination shall include the source code 
 * for the parts of Eclipse libraries used as well as that of the covered work.
 * 
 */

package org.xronos.orcc.backend.transform;

import net.sf.orcc.df.Action;
import net.sf.orcc.df.Actor;
import net.sf.orcc.df.util.DfVisitor;
import net.sf.orcc.ir.ExprInt;
import net.sf.orcc.ir.Expression;
import net.sf.orcc.ir.IrFactory;
import net.sf.orcc.ir.Procedure;
import net.sf.orcc.ir.TypeList;
import net.sf.orcc.ir.Var;
import net.sf.orcc.ir.util.AbstractIrVisitor;
import net.sf.orcc.ir.util.ValueUtil;

import org.xronos.orcc.design.util.XronosMathUtil;

/**
 * This visitor transforms the size of an List array to a power of 2
 * 
 * @author Endri Bezati
 * 
 */
public class ArraySizeToPowerOfTwo extends DfVisitor<Void> {

	public class LocalArraySizeToPowerOfTwo extends AbstractIrVisitor<Void> {

		@Override
		public Void caseProcedure(Procedure procedure) {
			for (Var var : procedure.getLocals()) {
				if (var.getType().isList()) {
					sizeToPowerOfTwo((TypeList) var.getType());
				}
			}
			return null;
		}

	}

	@Override
	public Void caseActor(Actor actor) {
		for (Var var : actor.getStateVars()) {
			// Check if is a List
			if (var.getType().isList()) {
				// Fix Size if necessary
				sizeToPowerOfTwo((TypeList) var.getType());
			}
		}

		// Now check for local List declared in actions
		for (Action action : actor.getActions()) {
			LocalArraySizeToPowerOfTwo arraySizeToPowerOfTwo = new LocalArraySizeToPowerOfTwo();
			arraySizeToPowerOfTwo.doSwitch(action.getBody());
		}

		return null;
	}

	private void sizeToPowerOfTwo(TypeList type) {
		if (type.getType().isList()) {
			sizeToPowerOfTwo((TypeList) type.getType());
		}

		Expression sizeExpr = type.getSizeExpr();
		if (sizeExpr.isExprInt()) {
			ExprInt intExpr = (ExprInt) sizeExpr;
			if (!ValueUtil.isPowerOfTwo(intExpr)) {
				Integer newValue = XronosMathUtil.nearestPowTwo(intExpr
						.getIntValue());
				ExprInt newSize = IrFactory.eINSTANCE.createExprInt(newValue);
				type.setSizeExpr(newSize);
			}

		}
	}

}
