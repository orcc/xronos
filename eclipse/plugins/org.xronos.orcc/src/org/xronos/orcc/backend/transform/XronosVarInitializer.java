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

import java.math.BigDecimal;
import java.math.BigInteger;

import net.sf.orcc.df.Actor;
import net.sf.orcc.df.util.DfVisitor;
import net.sf.orcc.ir.Expression;
import net.sf.orcc.ir.Type;
import net.sf.orcc.ir.TypeList;
import net.sf.orcc.ir.Var;
import net.sf.orcc.ir.util.ValueUtil;

public class XronosVarInitializer extends DfVisitor<Void> {

	XronosExprEvaluator exprEvaluator;

	public XronosVarInitializer() {
		exprEvaluator = new XronosExprEvaluator();
	}

	@Override
	public Void caseActor(Actor actor) {
		// initialize parameters
		for (Var var : actor.getParameters()) {
			initializeVar(var);
		}

		// initializes state variables
		for (Var stateVar : actor.getStateVars()) {
			if (!stateVar.isAssignable() || stateVar.isInitialized()) {
				initializeVar(stateVar);
			}
		}
		return super.caseActor(actor);
	}

	/**
	 * Initializes the given variable.
	 * 
	 * @param variable
	 *            a variable
	 */
	protected void initializeVar(Var variable) {
		Type type = variable.getType();
		Expression initConst = variable.getInitialValue();
		if (initConst == null) {
			Object value;
			if (type.isBool()) {
				value = false;
			} else if (type.isFloat()) {
				value = BigDecimal.ZERO;
			} else if (type.isInt() || type.isUint()) {
				value = BigInteger.ZERO;
			} else if (type.isList()) {
				value = ValueUtil.createArray((TypeList) type);
			} else if (type.isString()) {
				value = "";
			} else {
				value = null;
			}
			variable.setValue(value);
		} else {
			// evaluate initial constant value
			if (type.isList()) {
				exprEvaluator.setType((TypeList) type);
			}
			variable.setValue(exprEvaluator.doSwitch(initConst));
		}
	}

}
