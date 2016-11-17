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

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

import net.sf.orcc.ir.ExprBinary;
import net.sf.orcc.ir.ExprBool;
import net.sf.orcc.ir.ExprFloat;
import net.sf.orcc.ir.ExprInt;
import net.sf.orcc.ir.ExprString;
import net.sf.orcc.ir.Expression;
import net.sf.orcc.ir.InstAssign;
import net.sf.orcc.ir.InstLoad;
import net.sf.orcc.ir.InstStore;
import net.sf.orcc.ir.IrFactory;
import net.sf.orcc.ir.util.ValueUtil;

/**
 * An constant Folding Transformation
 * 
 * @author Endri Bezati
 * 
 */
public class XronosConstantFolding extends XronosExprEvaluator {

	@Override
	public Object caseExprBinary(ExprBinary expr) {
		Object val1 = doSwitch(expr.getE1());
		Object val2 = doSwitch(expr.getE2());
		Object result = getValue(val1, val2, expr.getOp());

		if (expr.getE1().isExprBinary()) {
			if (val1 != null) {
				Expression e = getExpressionFromValue(val1);
				expr.setE1(e);
			}
		}

		if (expr.getE2().isExprBinary()) {
			if (val2 != null) {
				Expression e = getExpressionFromValue(val2);
				expr.setE2(e);
			}
		}

		return result;
	}

	@Override
	public Void caseInstAssign(InstAssign assign) {
		Object value = doSwitch(assign.getValue());
		if (value != null) {
			assign.setValue(getExpressionFromValue(value));
		}
		return null;
	}

	@Override
	public Object caseInstLoad(InstLoad load) {
		if (!load.getIndexes().isEmpty()) {
			Map<Integer, Expression> indexes = new HashMap<Integer, Expression>();
			int i = 0;
			for (Expression index : load.getIndexes()) {
				Object value = doSwitch(index);
				if (value != null) {
					Expression e = getExpressionFromValue(value);
					indexes.put(i, e);
				}
				i++;
			}

			// Replaces indexes
			for (Integer idx : indexes.keySet()) {
				load.getIndexes().set(idx, indexes.get(idx));
			}
		}
		return null;
	}

	@Override
	public Object caseInstStore(InstStore store) {
		Object value = doSwitch(store.getValue());
		if (value != null) {
			store.setValue(getExpressionFromValue(value));
		}
		return null;
	}

	public Expression getExpressionFromValue(Object value) {
		Expression expression = null;
		if (ValueUtil.isBool(value)) {
			ExprBool exprBool = IrFactory.eINSTANCE
					.createExprBool((Boolean) value);
			return exprBool;
		} else if (ValueUtil.isInt(value)) {
			ExprInt exprInt = IrFactory.eINSTANCE
					.createExprInt((BigInteger) value);
			return exprInt;
		} else if (ValueUtil.isFloat(value)) {
			ExprFloat exprFloat = IrFactory.eINSTANCE
					.createExprFloat((BigDecimal) value);
			return exprFloat;
		} else if (ValueUtil.isString(value)) {
			ExprString exprString = IrFactory.eINSTANCE
					.createExprString((String) value);
			return exprString;
		}
		return expression;
	}

}
