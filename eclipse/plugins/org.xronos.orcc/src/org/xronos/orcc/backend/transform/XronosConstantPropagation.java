/*
 * Copyright (c) 2013, Ecole Polytechnique Fédérale de Lausanne
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

import net.sf.orcc.OrccRuntimeException;
import net.sf.orcc.ir.ExprBool;
import net.sf.orcc.ir.ExprFloat;
import net.sf.orcc.ir.ExprInt;
import net.sf.orcc.ir.ExprString;
import net.sf.orcc.ir.ExprVar;
import net.sf.orcc.ir.Expression;
import net.sf.orcc.ir.InstLoad;
import net.sf.orcc.ir.IrFactory;
import net.sf.orcc.ir.Type;
import net.sf.orcc.ir.TypeList;
import net.sf.orcc.ir.Use;
import net.sf.orcc.ir.Var;
import net.sf.orcc.ir.util.AbstractIrVisitor;
import net.sf.orcc.ir.util.IrUtil;
import net.sf.orcc.ir.util.ValueUtil;
import net.sf.orcc.util.util.EcoreHelper;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.util.EcoreUtil;

/**
 * A constant propagation transformation
 * 
 * @author Endri Bezati
 * 
 */
public class XronosConstantPropagation extends AbstractIrVisitor<Void> {

	@Override
	public Void caseInstLoad(InstLoad load) {
		Var source = load.getSource().getVariable();
		if (source.isGlobal() && !source.isAssignable()) {
			Object value = null;
			XronosExprEvaluator exprEvaluator = new XronosExprEvaluator();
			if (load.getIndexes().isEmpty()) {
				value = source.getValue();
			} else {
				Object array = source.getValue();
				Object[] indexes = new Object[load.getIndexes().size()];
				int i = 0;
				for (Expression index : load.getIndexes()) {
					indexes[i++] = exprEvaluator.doSwitch(index);
				}
				Type type = ((TypeList) source.getType()).getInnermostType();
				try {
					value = ValueUtil.get(type, array, indexes);
				} catch (IndexOutOfBoundsException e) {
					throw new OrccRuntimeException(
							"Array Index Out of Bound at line "
									+ load.getLineNumber());
				} catch (OrccRuntimeException e) {
					value = null;
				}
			}
			if (value != null) {
				Expression exprValue = getExpressionFromValue(value);
				EList<Use> targetUses = load.getTarget().getVariable()
						.getUses();
				while (!targetUses.isEmpty()) {
					ExprVar expr = EcoreHelper.getContainerOfType(
							targetUses.get(0), ExprVar.class);
					EcoreUtil.replace(expr, IrUtil.copy(exprValue));
					IrUtil.delete(expr);
				}
				IrUtil.delete(load);
				indexInst--;
			}
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
