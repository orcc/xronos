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

package org.xronos.orcc.forge.mapping;

import java.lang.reflect.Array;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sf.orcc.df.Action;
import net.sf.orcc.df.Actor;
import net.sf.orcc.df.util.DfVisitor;
import net.sf.orcc.ir.ExprBool;
import net.sf.orcc.ir.ExprInt;
import net.sf.orcc.ir.ExprVar;
import net.sf.orcc.ir.Expression;
import net.sf.orcc.ir.Procedure;
import net.sf.orcc.ir.Type;
import net.sf.orcc.ir.TypeList;
import net.sf.orcc.ir.Var;
import net.sf.orcc.ir.util.ValueUtil;

import org.xronos.openforge.lim.Design;
import org.xronos.openforge.lim.memory.AddressStridePolicy;
import org.xronos.openforge.lim.memory.AddressableUnit;
import org.xronos.openforge.lim.memory.Allocation;
import org.xronos.openforge.lim.memory.LogicalMemory;
import org.xronos.openforge.lim.memory.LogicalValue;
import org.xronos.openforge.lim.memory.Record;
import org.xronos.openforge.lim.memory.Scalar;
import org.xronos.orcc.backend.transform.XronosExprEvaluator;
import org.xronos.orcc.preference.Constants;

/**
 * This Visitor builds the Memory allocations for each Memory found on actor,
 * StateVars and Vars found on patters
 * 
 * @author Endri Bezati
 * 
 */
public class DesignMemory extends DfVisitor<Void> {

	public static void addToMemory(Actor actor, Var var) {
		@SuppressWarnings("unchecked")
		Map<Integer, LogicalMemory> memories = (Map<Integer, LogicalMemory>) actor
				.getAttribute("memories").getObjectValue();
		LogicalValue logicalValue = makeLogicalValue(var);
		var.setAttribute("logicalValue", logicalValue);

		// Allocate each LogicalValue (State Variable) in a memory
		// with a matching address stride. This provides consistency
		// in the memories and allows (if activated) for state vars to be
		// co-located
		// if area is of concern.
		int stride = logicalValue.getAddressStridePolicy().getStride();
		LogicalMemory mem = memories.get(stride);
		if (mem == null) {
			// 32 should be more than enough for max address
			// width
			mem = new LogicalMemory(Constants.MAX_ADDR_WIDTH);
			mem.createLogicalMemoryPort();
			mem.setIDLogical("stateVar_" + var.getName());
			Design design = (Design) actor.getAttribute("design")
					.getObjectValue();
			design.addMemory(mem);
		}
		// Create a 'location' for the stateVar that is
		// appropriate for its type/size.
		Allocation location = mem.allocate(logicalValue);
		var.setAttribute("location", location);
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
	public static LogicalValue makeLogicalValue(String stringValue, Type type) {
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
	 * Creates a Record of "0" logicalValues
	 * 
	 * @param type
	 * @param nbrElements
	 * @return
	 */
	public static LogicalValue makeLogicalValue(Type type, int nbrElements) {
		List<LogicalValue> subElements = new ArrayList<LogicalValue>(
				nbrElements);
		for (int i = 0; i < nbrElements; i++) {
			subElements.add(makeLogicalValue("0", type));
		}
		return new Record(subElements);
	}

	/**
	 * Constructs a LogicalValue from a Variable
	 * 
	 * @param var
	 *            the variable
	 * @return
	 */
	public static LogicalValue makeLogicalValue(Var var) {
		LogicalValue logicalValue = null;
		if (var.getType().isList()) {
			XronosExprEvaluator exprEvaluator = new XronosExprEvaluator();
			TypeList typeList = (TypeList) var.getType();
			Type type = typeList.getInnermostType();

			List<Integer> listDimension = new ArrayList<Integer>(
					typeList.getDimensions());
			exprEvaluator.setType((TypeList) typeList);
			Object varValue = null;
			Expression initConst = var.getInitialValue();
			if (initConst == null) {
				varValue = ValueUtil.createArray((TypeList) typeList);
			} else {
				varValue = exprEvaluator.doSwitch(initConst);
			}
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
	public static LogicalValue makeLogicalValueObject(Object obj,
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

	/**
	 * Colocate Var that have the same stride
	 */
	private boolean colocateVars;

	/**
	 * Design
	 */
	private Design design;

	private Map<Integer, LogicalMemory> memories;

	public DesignMemory(Design design, boolean colocateVars) {
		this.design = design;
		this.colocateVars = colocateVars;
	}

	@Override
	public Void caseActor(Actor actor) {
		memories = new HashMap<Integer, LogicalMemory>();

		for (Var var : actor.getStateVars()) {
			// Create Logical Value for each
			LogicalValue logicalValue = makeLogicalValue(var);
			var.setAttribute("logicalValue", logicalValue);

			// Allocate each LogicalValue (State Variable) in a memory
			// with a matching address stride. This provides consistency
			// in the memories and allows (if activated) for state vars to be
			// co-located
			// if area is of concern.
			int stride = logicalValue.getAddressStridePolicy().getStride();
			LogicalMemory mem = memories.get(stride);
			if (mem == null) {
				// 32 should be more than enough for max address
				// width
				mem = new LogicalMemory(Constants.MAX_ADDR_WIDTH);
				mem.createLogicalMemoryPort();
				mem.setIDLogical("stateVar_" + var.getName());
				design.addMemory(mem);
				if (colocateVars) {
					memories.put(stride, mem);
				}
			}
			// Create a 'location' for the stateVar that is
			// appropriate for its type/size.
			Allocation location = mem.allocate(logicalValue);
			var.setAttribute("location", location);
		}


		// Actions local list Variables
		for (Action action : actor.getActions()) {
			Procedure procedure = action.getBody();
			for (Var var : procedure.getLocals()) {
				if (var.getType().isList()) {
					LogicalValue logicalValue = makeLogicalValue(var);
					var.setAttribute("logicalValue", logicalValue);
					int stride = logicalValue.getAddressStridePolicy()
							.getStride();
					LogicalMemory mem = memories.get(stride);
					if (mem == null) {
						// 32 should be more than enough for max address
						// width
						mem = new LogicalMemory(Constants.MAX_ADDR_WIDTH);
						mem.createLogicalMemoryPort();
						mem.setIDLogical(action.getName() + "_localVar_"
								+ var.getName());
						design.addMemory(mem);
						if (colocateVars) {
							memories.put(stride, mem);
						}
					}
					// Create a 'location' for the stateVar that is
					// appropriate for its type/size.
					Allocation location = mem.allocate(logicalValue);
					var.setAttribute("location", location);
				}
			}
		}

		for (Procedure procedure : actor.getProcs()) {
			for (Var var : procedure.getLocals()) {
				if (var.getType().isList()) {
					LogicalValue logicalValue = makeLogicalValue(var);
					var.setAttribute("logicalValue", logicalValue);
					int stride = logicalValue.getAddressStridePolicy()
							.getStride();
					LogicalMemory mem = memories.get(stride);
					if (mem == null) {
						// 32 should be more than enough for max address
						// width
						mem = new LogicalMemory(Constants.MAX_ADDR_WIDTH);
						mem.createLogicalMemoryPort();
						mem.setIDLogical(procedure.getName() + "_procLocalVar_"
								+ var.getName());
						design.addMemory(mem);
						if (colocateVars) {
							memories.put(stride, mem);
						}
					}
					// Create a 'location' for the stateVar that is
					// appropriate for its type/size.
					Allocation location = mem.allocate(logicalValue);
					var.setAttribute("location", location);
				}
			}
		}
		actor.setAttribute("memories", memories);
		return null;
	}

}
