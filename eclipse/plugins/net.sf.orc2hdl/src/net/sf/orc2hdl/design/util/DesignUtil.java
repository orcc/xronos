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
package net.sf.orc2hdl.design.util;

import java.lang.reflect.Array;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import net.sf.openforge.frontend.slim.builder.SLIMConstants;
import net.sf.openforge.lim.Block;
import net.sf.openforge.lim.Call;
import net.sf.openforge.lim.Component;
import net.sf.openforge.lim.ControlDependency;
import net.sf.openforge.lim.DataDependency;
import net.sf.openforge.lim.Exit;
import net.sf.openforge.lim.OffsetMemoryAccess;
import net.sf.openforge.lim.Port;
import net.sf.openforge.lim.memory.AddressStridePolicy;
import net.sf.openforge.lim.memory.AddressableUnit;
import net.sf.openforge.lim.memory.Location;
import net.sf.openforge.lim.memory.LocationConstant;
import net.sf.openforge.lim.memory.LogicalValue;
import net.sf.openforge.lim.memory.Record;
import net.sf.openforge.lim.memory.Scalar;
import net.sf.openforge.lim.op.AddOp;
import net.sf.openforge.lim.op.CastOp;
import net.sf.orcc.ir.ExprInt;
import net.sf.orcc.ir.Type;
import net.sf.orcc.ir.TypeList;
import net.sf.orcc.ir.Var;
import net.sf.orcc.ir.util.ValueUtil;

/**
 * This class contains several methods for building branches, loops and its
 * decisions
 * 
 * @author Endri Bezati
 * 
 */
public class DesignUtil {
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
	 * Constructs a LogicalValue from a Variable
	 * 
	 * @param var
	 *            the variable
	 * @return
	 */
	public static LogicalValue makeLogicalValue(Var var) {
		LogicalValue logicalValue = null;
		if (var.getType().isList()) {

			TypeList typeList = (TypeList) var.getType();
			Type type = typeList.getInnermostType();

			List<Integer> listDimension = typeList.getDimensions();
			Object varValue = var.getValue();
			logicalValue = makeLogicalValueObject(varValue, listDimension, type);
		} else {
			Type type = var.getType();
			if (var.isInitialized()) {
				String valueString = Integer.toString(((ExprInt) var
						.getInitialValue()).getIntValue());
				logicalValue = makeLogicalValue(valueString, type);
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
			List<Integer> newListDimension = dimension;
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
	 * This method builds the necessary AddressBlock control of a Memory access
	 * 
	 * @param memAccess
	 *            the memory access
	 * @param targetLocation
	 *            the Location of the target
	 * @param otherComps
	 *            other components to add to the block
	 * @return
	 */
	public static Block buildAddressedBlock(OffsetMemoryAccess memAccess,
			Location targetLocation, List<Component> otherComps) {
		final LocationConstant locationConst = new LocationConstant(
				targetLocation, SLIMConstants.MAX_ADDR_WIDTH, targetLocation
						.getAbsoluteBase().getLogicalMemory()
						.getAddressStridePolicy());
		final AddOp adder = new AddOp();
		final CastOp cast = new CastOp(SLIMConstants.MAX_ADDR_WIDTH, false);

		final Block block = new Block(false);
		final Exit done = block.makeExit(0, Exit.DONE);
		final List<Component> comps = new ArrayList<Component>();
		comps.add(locationConst);
		comps.add(cast);
		comps.add(adder);
		comps.add(memAccess);
		comps.addAll(otherComps);
		ModuleUtil.modulePopulate(block, comps);
		final Port index = block.makeDataPort();

		// Now build the dependencies
		cast.getEntries()
				.get(0)
				.addDependency(cast.getDataPort(),
						new DataDependency(index.getPeer()));
		adder.getEntries()
				.get(0)
				.addDependency(adder.getLeftDataPort(),
						new DataDependency(locationConst.getValueBus()));
		adder.getEntries()
				.get(0)
				.addDependency(adder.getRightDataPort(),
						new DataDependency(cast.getResultBus()));

		memAccess
				.getEntries()
				.get(0)
				.addDependency(memAccess.getBaseAddressPort(),
						new DataDependency(adder.getResultBus()));

		done.getPeer()
				.getEntries()
				.get(0)
				.addDependency(
						done.getDoneBus().getPeer(),
						new ControlDependency(memAccess.getExit(Exit.DONE)
								.getDoneBus()));

		return block;
	}

	/**
	 * This method sets the sizes of the clock,reset and go ports of a call
	 * 
	 * @param call
	 *            the call
	 */
	public void topLevelInitialization(Call call) {
		call.getClockPort().setSize(1, false);
		call.getResetPort().setSize(1, false);
		call.getGoPort().setSize(1, false);
	}

}
