package org.xronos.orcc.backend.transform;

import net.sf.orcc.ir.ExprBinary;
import net.sf.orcc.ir.ExprUnary;
import net.sf.orcc.ir.ExprVar;
import net.sf.orcc.ir.OpBinary;
import net.sf.orcc.ir.Var;
import net.sf.orcc.ir.util.ExpressionEvaluator;
import net.sf.orcc.ir.util.ValueUtil;

/**
 * An expression evaluator that accepts null values
 * 
 * @author Endri Bezati
 * 
 */
public class XronosExprEvaluator extends ExpressionEvaluator {
	@Override
	public Object caseExprBinary(ExprBinary expr) {
		Object val1 = doSwitch(expr.getE1());
		Object val2 = doSwitch(expr.getE2());
		Object result = null;
		if (val1 == null && val2 == null) {
			return null;
		} else if (val1 != null && val2 == null) {
			if (ValueUtil.isBool(val1)) {
				Boolean value = (Boolean) val1;
				if (expr.getOp() == OpBinary.LOGIC_AND) {
					if (value) {
						return null;
					} else {
						return false;
					}
				} else if (expr.getOp() == OpBinary.LOGIC_OR) {
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
				if (expr.getOp() == OpBinary.LOGIC_AND) {
					if (value) {
						return null;
					} else {
						return false;
					}
				} else if (expr.getOp() == OpBinary.LOGIC_OR) {
					if (value) {
						return true;
					} else {
						return null;
					}
				}
			}
		} else {
			result = ValueUtil.compute(val1, expr.getOp(), val2);
		}
		return result;
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
}