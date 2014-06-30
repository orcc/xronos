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
import net.sf.orcc.util.Void;
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
