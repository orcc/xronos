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
package org.xronos.orcc.backend.transform;

import net.sf.orcc.ir.BlockBasic;
import net.sf.orcc.ir.ExprVar;
import net.sf.orcc.ir.Expression;
import net.sf.orcc.ir.InstAssign;
import net.sf.orcc.ir.InstPhi;
import net.sf.orcc.ir.IrFactory;
import net.sf.orcc.ir.Param;
import net.sf.orcc.ir.Procedure;
import net.sf.orcc.ir.Var;
import net.sf.orcc.ir.util.AbstractIrVisitor;
import net.sf.orcc.util.Void;
import net.sf.orcc.util.util.EcoreHelper;

/**
 * This visitor add missing definition of variables found in the Phi Values
 * after the SSA transformation.
 * 
 * @author Endri Bezati
 * 
 */
public class PhiFixer extends AbstractIrVisitor<Void> {

	private void addAssign(Var var) {
		// Create its Zero value
		Expression value = null;
		if (var.getType().isBool()) {
			value = IrFactory.eINSTANCE.createExprBool(false);
		} else if (var.getType().isInt() || var.getType().isUint()) {
			value = IrFactory.eINSTANCE.createExprInt(0);
		} else if (var.getType().isFloat()) {
			value = IrFactory.eINSTANCE.createExprFloat(0);
		}

		// Create the assign instruction
		InstAssign assign = IrFactory.eINSTANCE.createInstAssign(var, value);

		// Create a BlockBasic and add the assign instruction
		BlockBasic blockBasic = IrFactory.eINSTANCE.createBlockBasic();
		blockBasic.add(assign);

		// Add the BlockBasic to the procedure
		Procedure procedure = EcoreHelper.getContainerOfType(var,
				Procedure.class);
		procedure.getBlocks().add(0, blockBasic);
	}

	@Override
	public Void caseExprVar(ExprVar object) {
		Var var = object.getUse().getVariable();

		// Test if it is procedure parameter
		if (procedure.getParameters() != null) {
			for (Param parameter : procedure.getParameters()) {
				if (var == parameter.getVariable()) {
					return null;
				}
			}
		}

		if (var.getDefs().isEmpty() && !var.getType().isString()) {
			addAssign(var);
		}
		return null;
	}

	@Override
	public Void caseInstPhi(InstPhi phi) {
		for (Expression expr : phi.getValues()) {
			doSwitch(expr);
		}
		return null;
	}

}
