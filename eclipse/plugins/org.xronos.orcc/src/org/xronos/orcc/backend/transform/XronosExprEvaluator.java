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

import java.math.BigInteger;
import java.util.List;

import net.sf.orcc.OrccRuntimeException;
import net.sf.orcc.ir.ExprBinary;
import net.sf.orcc.ir.ExprBool;
import net.sf.orcc.ir.ExprFloat;
import net.sf.orcc.ir.ExprInt;
import net.sf.orcc.ir.ExprList;
import net.sf.orcc.ir.ExprString;
import net.sf.orcc.ir.ExprUnary;
import net.sf.orcc.ir.ExprVar;
import net.sf.orcc.ir.Expression;
import net.sf.orcc.ir.OpBinary;
import net.sf.orcc.ir.Type;
import net.sf.orcc.ir.TypeList;
import net.sf.orcc.ir.Var;
import net.sf.orcc.ir.util.AbstractIrVisitor;
import net.sf.orcc.ir.util.ValueUtil;

/**
 * An expression evaluator that accepts null values
 * 
 * @author Endri Bezati
 * 
 */
public class XronosExprEvaluator extends AbstractIrVisitor<Object> {

	private TypeList typeList;

	public XronosExprEvaluator() {
		super(true);
	}

	@Override
	public Object caseExprBinary(ExprBinary expr) {
		Object val1 = doSwitch(expr.getE1());
		Object val2 = doSwitch(expr.getE2());
		Object result = null;
		try {
			result = getValue(val1, val2, expr.getOp());
		} catch (ArithmeticException e) {
			return null;
		}
		return result;
	}

	@Override
	public Object caseExprBool(ExprBool expr) {
		return expr.isValue();
	}

	@Override
	public Object caseExprFloat(ExprFloat expr) {
		return expr.getValue();
	}

	@Override
	public Object caseExprInt(ExprInt expr) {
		return expr.getValue();
	}

	@Override
	public Object caseExprList(ExprList expr) {
		if (typeList == null) {
			// if no type has been defined, use the expression's type
			typeList = (TypeList) expr.getType();
		}

		Object array = ValueUtil.createArray(typeList);
		computeInitValue(array, typeList, expr);

		// reset the type for future calls
		typeList = null;

		return array;
	}

	@Override
	public Object caseExprString(ExprString expr) {
		// note the difference with the caseExprString method from the
		// expression printer: here we return the string without quotes
		return expr.getValue();
	}

	@Override
	public Object caseExprUnary(ExprUnary expr) {
		Object value = doSwitch(expr.getExpr());
		if (value == null) {
			return null;
		}
		Object result = ValueUtil.compute(expr.getOp(), value);

		return result;
	}

	@Override
	public Object caseExprVar(ExprVar expr) {
		Var var = expr.getUse().getVariable();
		Object value = var.getValue();

		return value;
	}

	/**
	 * Evaluates the given expression and copy it into the given array.
	 * 
	 * @param array
	 *            the array to visit
	 * @param type
	 *            type of the current dimension
	 * @param expr
	 *            expression associated with the current dimension
	 * @param indexes
	 *            indexes that lead to the current dimension (empty for the
	 *            outermost call)
	 */
	private void computeInitValue(Object array, Type type, Expression expr,
			Object... indexes) {
		if (type.isList()) {
			TypeList typeList = (TypeList) type;
			List<Expression> list = ((ExprList) expr).getValue();

			Type eltType = typeList.getType();

			Object[] innerIndexes = new Object[indexes.length + 1];
			System.arraycopy(indexes, 0, innerIndexes, 0, indexes.length);
			for (int i = 0; i < list.size() && i < typeList.getSize(); i++) {
				innerIndexes[indexes.length] = i;
				computeInitValue(array, eltType, list.get(i), innerIndexes);
			}
		} else {
			ValueUtil.set(type, array, doSwitch(expr), indexes);
		}
	}

	/**
	 * Evaluates this expression and return its value as an integer.
	 * 
	 * @param expr
	 *            an expression to evaluate
	 * @return the expression evaluated as an integer
	 * @throws OrccRuntimeException
	 *             if the expression cannot be evaluated as an integer
	 */
	public int evaluateAsInteger(Expression expr) {
		Object value = doSwitch(expr);
		if (ValueUtil.isInt(value)) {
			return ((BigInteger) value).intValue();
		}

		// evaluated ok, but not as an integer
		throw new OrccRuntimeException("expected integer expression");
	}

	protected Object getValue(Object val1, Object val2, OpBinary op) {
		Object result = null;
		if (val1 == null && val2 == null) {
			return null;
		} else if (val1 != null && val2 == null) {
			if (ValueUtil.isBool(val1)) {
				Boolean value = (Boolean) val1;
				if (op == OpBinary.LOGIC_AND) {
					if (value) {
						return null;
					} else {
						return false;
					}
				} else if (op == OpBinary.LOGIC_OR) {
					if (value) {
						return true;
					} else {
						return null;
					}
				}
			}
		} else if (val1 == null && val2 != null) {
			if (ValueUtil.isBool(val2)) {
				Boolean value = (Boolean) val2;
				if (op == OpBinary.LOGIC_AND) {
					if (value) {
						return null;
					} else {
						return false;
					}
				} else if (op == OpBinary.LOGIC_OR) {
					if (value) {
						return true;
					} else {
						return null;
					}
				}
			}
		} else {
			try {
				result = ValueUtil.compute(val1, op, val2);
			} catch (ArithmeticException e) {
				return null;
			}
		}
		return result;
	}

	public void setType(TypeList typeList) {
		this.typeList = typeList;
	}

}