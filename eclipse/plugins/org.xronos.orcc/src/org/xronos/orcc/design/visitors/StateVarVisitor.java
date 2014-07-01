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
 * If you modify this Program, or any covered work, by linking or combining it
 * with Eclipse (or a modified version of Eclipse or an Eclipse plugin or 
 * an Eclipse library), containing parts covered by the terms of the 
 * Eclipse Public License (EPL), the licensors of this Program grant you 
 * additional permission to convey the resulting work.  Corresponding Source 
 * for a non-source form of such a combination shall include the source code 
 * for the parts of Eclipse libraries used as well as that of the  covered work.
 * 
 */

package org.xronos.orcc.design.visitors;

import java.lang.reflect.Array;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.sf.orcc.ir.ExprBool;
import net.sf.orcc.ir.ExprInt;
import net.sf.orcc.ir.ExprVar;
import net.sf.orcc.ir.Type;
import net.sf.orcc.ir.TypeList;
import net.sf.orcc.ir.Var;
import net.sf.orcc.ir.util.AbstractIrVisitor;
import net.sf.orcc.ir.util.ValueUtil;

import org.xronos.openforge.lim.memory.AddressStridePolicy;
import org.xronos.openforge.lim.memory.AddressableUnit;
import org.xronos.openforge.lim.memory.LogicalValue;
import org.xronos.openforge.lim.memory.Record;
import org.xronos.openforge.lim.memory.Scalar;

/**
 * This class visits the state variables of an actor and it creates a
 * LogicalValue for a Design
 * 
 * @author Endri Bezati
 * 
 */
public class StateVarVisitor extends AbstractIrVisitor<Object> {

	/** A Design stateVars Map **/
	private final Map<LogicalValue, Var> stateVars;

	public StateVarVisitor(Map<LogicalValue, Var> stateVars) {
		super(true);
		this.stateVars = stateVars;
	}

	@Override
	public Object caseVar(Var var) {
		if (var.isGlobal()) {
			if (var.getType().isList() || var.isAssignable()) {
				stateVars.put(makeLogicalValue(var), var);
			}
		}
		return null;
	}

	/**
	 * Constructs a LogicalValue from a String value given its type
	 * 
	 * @param stringValue
	 *            the numerical value
	 * @param type
	 *            the type of the numerical value
	 * @return
	 */
	private LogicalValue makeLogicalValue(String stringValue, Type type) {
		LogicalValue logicalValue = null;
		final BigInteger value;
		Integer bitSize = type.getSizeInBits();
		if (stringValue.trim().toUpperCase().startsWith("0X")) {
			value = new BigInteger(stringValue.trim().substring(2), 16);
		} else {
			value = new BigInteger(stringValue);
		}
		AddressStridePolicy addrPolicy = new AddressStridePolicy(bitSize);
		logicalValue = new Scalar(new AddressableUnit(value), addrPolicy);
		return logicalValue;
	}

	/**
	 * Constructs a LogicalValue from a Variable
	 * 
	 * @param var
	 *            the variable
	 * @return
	 */
	private LogicalValue makeLogicalValue(Var var) {
		LogicalValue logicalValue = null;
		if (var.getType().isList()) {

			TypeList typeList = (TypeList) var.getType();
			Type type = typeList.getInnermostType();

			List<Integer> listDimension = new ArrayList<Integer>(
					typeList.getDimensions());
			Object varValue = var.getValue();
			logicalValue = makeLogicalValueObject(varValue, listDimension, type);
		} else {
			Type type = var.getType();
			if (var.isInitialized()) {
				if (type.isBool()) {
					if (var.getInitialValue() instanceof ExprVar) {
						ExprVar exprVar = (ExprVar) var.getInitialValue();
						Integer value = (Boolean) exprVar.getUse()
								.getVariable().getValue() ? 1 : 0;
						String valueString = Integer.toString(value);
						logicalValue = makeLogicalValue(valueString, type);
					} else {
						Integer value = ((ExprBool) var.getInitialValue())
								.isValue() ? 1 : 0;
						String valueString = Integer.toString(value);
						logicalValue = makeLogicalValue(valueString, type);
					}
				} else if (type.isInt() || type.isUint()) {
					if (var.getInitialValue() instanceof ExprVar) {
						ExprVar exprVar = (ExprVar) var.getInitialValue();
						String valueString = Integer.toString((Integer) exprVar
								.getUse().getVariable().getValue());
						logicalValue = makeLogicalValue(valueString, type);
					} else {
						String valueString = Integer.toString(((ExprInt) var
								.getInitialValue()).getIntValue());
						logicalValue = makeLogicalValue(valueString, type);
					}
				} else {
					logicalValue = makeLogicalValue("0", type);
				}
			} else {
				logicalValue = makeLogicalValue("0", type);
			}
		}

		return logicalValue;
	}

	/**
	 * Constructs a LogicalValue from a uni or multi-dim Object Value
	 * 
	 * @param obj
	 *            the object value
	 * @param dimension
	 *            the dimension of the object value
	 * @param type
	 *            the type of the object value
	 * @return
	 */
	private LogicalValue makeLogicalValueObject(Object obj,
			List<Integer> dimension, Type type) {
		LogicalValue logicalValue = null;

		if (dimension.size() > 1) {
			List<LogicalValue> subElements = new ArrayList<LogicalValue>(
					dimension.get(0));
			List<Integer> newListDimension = new ArrayList<Integer>(dimension);

			Integer firstDim = dimension.get(0);
			newListDimension.remove(0);
			for (int i = 0; i < firstDim; i++) {
				subElements.add(makeLogicalValueObject(Array.get(obj, i),
						newListDimension, type));
			}

			logicalValue = new Record(subElements);
		} else {
			if (dimension.get(0).equals(1)) {
				BigInteger value = BigInteger.valueOf(0);
				if (type.isBool()) {
					int boolValue = ((Boolean) ValueUtil.get(type, obj, 0)) ? 1
							: 0;
					value = BigInteger.valueOf(boolValue);
				} else {
					value = (BigInteger) ValueUtil.get(type, obj, 0);
				}
				String valueString = value.toString();
				logicalValue = makeLogicalValue(valueString, type);
			} else {
				List<LogicalValue> subElements = new ArrayList<LogicalValue>(
						dimension.get(0));
				for (int i = 0; i < dimension.get(0); i++) {
					BigInteger value = BigInteger.valueOf(0);
					if (type.isBool()) {
						int boolValue = ((Boolean) ValueUtil.get(type, obj, i)) ? 1
								: 0;
						value = BigInteger.valueOf(boolValue);
					} else {
						value = (BigInteger) ValueUtil.get(type, obj, i);
					}

					String valueString = value.toString();
					subElements.add(makeLogicalValue(valueString, type));
				}
				logicalValue = new Record(subElements);
			}

		}

		return logicalValue;
	}

}
