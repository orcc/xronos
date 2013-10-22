/*
 * Copyright (c) 2012, Ecole Polytechnique Fédérale de Lausanne
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 *   * Redistributions of source code must retain the above copyright notice,
 *     this list of conditions and the following disclaimer.
 *   * Redistributions in binary form must reproduce the above copyright notice,
 *     this list of conditions and the following disclaimer in the documentation
 *     and/or other materials provided with the distribution.
 *   * Neither the name of the Ecole Polytechnique Fédérale de Lausanne nor the names of its
 *     contributors may be used to endorse or promote products derived from this
 *     software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY
 * WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
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