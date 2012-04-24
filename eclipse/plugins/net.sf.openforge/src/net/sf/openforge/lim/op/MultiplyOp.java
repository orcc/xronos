/*******************************************************************************
 * Copyright 2002-2009  Xilinx Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
/*
 * 
 *
 * 
 */

package net.sf.openforge.lim.op;

import java.util.Collections;
import java.util.Map;

import net.sf.openforge.app.OptionRegistry;
import net.sf.openforge.app.project.Option;
import net.sf.openforge.app.project.OptionInt;
import net.sf.openforge.lim.Bit;
import net.sf.openforge.lim.Bus;
import net.sf.openforge.lim.Emulatable;
import net.sf.openforge.lim.Exit;
import net.sf.openforge.lim.Port;
import net.sf.openforge.lim.Value;
import net.sf.openforge.lim.Visitor;
import net.sf.openforge.report.FPGAResource;
import net.sf.openforge.util.SizedInteger;

/**
 * A binary arithmetic operation in a form of *.
 * 
 * Created: Thu Mar 08 16:39:34 2002
 * 
 * @author Conor Wu
 * @version $Id: MultiplyOp.java 2 2005-06-09 20:00:48Z imiller $
 */
public class MultiplyOp extends BinaryOp implements Emulatable {

	/** The natural size of this operation */
	private int naturalSize = -1;

	/**
	 * Constructs an arithmetic multiply operation.
	 * 
	 */
	public MultiplyOp(int naturalSize) {
		super();
		this.naturalSize = naturalSize;
	}

	/**
	 * Returns the natural size of this operation, used by optimizations to
	 * build shifts for implementation.
	 */
	public int getNaturalSize() {
		return naturalSize;
	}

	/**
	 * Returns the defined number of pipeline stages for this multiplier by the
	 * user via the {@link ScheduleDefiner#SCHEDULE_MULTIPLY_STAGES} preference.
	 * 
	 * @return a non-negative 'int'
	 */
	public int getPipeStages() {
		Option option = getGenericJob().getOption(
				OptionRegistry.SCHEDULE_MULTIPLY_STAGES);
		int stages = ((OptionInt) option).getValueAsInt(getSearchLabel());
		assert stages >= 0;
		return stages;
	}

	/**
	 * Gets the gate depth of this component. This is the maximum number of
	 * gates that any input signal must traverse before reaching an {@link Exit}
	 * .
	 * 
	 * @return a non-negative integer
	 */
	@Override
	public int getGateDepth() {
		final int width = Math.max(getLeftDataPort().getValue().getSize(),
				getRightDataPort().getValue().getSize());
		return 5 * width * width;
	}

	/**
	 * Gets the FPGA hardware resource usage of this component.
	 * 
	 * @return a FPGAResource objec
	 */
	@Override
	public FPGAResource getHardwareResourceUsage() {
		int lutCount = 0;
		// int muxCount = 0;
		int mult18x18Count = 0;

		Value leftValue = getLeftDataPort().getValue();
		Value rightValue = getRightDataPort().getValue();

		for (int i = 0; i < Math.max(leftValue.getSize(), rightValue.getSize()); i++) {
			Bit leftBit = null;
			Bit rightBit = null;
			if (i < leftValue.getSize()) {
				leftBit = leftValue.getBit(i);
			}
			if (i < rightValue.getSize()) {
				rightBit = rightValue.getBit(i);
			}

			if ((leftBit != null) && (rightBit != null)) {
				if (leftBit.isCare() && rightBit.isCare()
						&& (!leftBit.isConstant() || !rightBit.isConstant())) {
					lutCount++;
				}
			}
		}

		// muxCount = lutCount - 2;

		if (lutCount < 18) {
			mult18x18Count = 1;
		} else if (lutCount < 19) {
			mult18x18Count = 3;
		} else if (lutCount < 35) {
			mult18x18Count = 4;
		} else if (lutCount < 52) {
			mult18x18Count = 8;
		} else {
			mult18x18Count = 10;
		}

		FPGAResource hwResource = new FPGAResource();
		hwResource.addLUT(lutCount);
		hwResource.addMULT18X18(mult18x18Count);

		return hwResource;
	}

	/**
	 * Accept method for the Visitor interface
	 */
	@Override
	public void accept(Visitor visitor) {
		visitor.visit(this);
	}

	/**
	 * Performes a high level numerical emulation of this component.
	 * 
	 * @param portValues
	 *            a map of owner {@link Port} to {@link SizedInteger} input
	 *            value
	 * @return a map of {@link Bus} to {@link SizedInteger} result value
	 */
	@Override
	public Map<Bus, SizedInteger> emulate(Map<Port, SizedInteger> portValues) {
		final SizedInteger lval = portValues.get(getLeftDataPort());
		final SizedInteger rval = portValues.get(getRightDataPort());
		return Collections.singletonMap(getResultBus(), lval.multiply(rval));
	}

	/*
	 * =================================================== Begin new constant
	 * prop rules implementation.
	 */

	@Override
	public boolean pushValuesForward() {
		boolean mod = false;

		Value in0 = getLeftDataPort().getValue();
		Value in1 = getRightDataPort().getValue();

		int newSize = Math.max(in0.getSize(), in1.getSize());
		boolean isSigned = in0.isSigned() && in1.isSigned();

		Value newValue = new Value(newSize, isSigned);

		// update all bits above the carry out bit to be signed
		// extended of carry out bit
		if (getResultBus().getValue() != null) {
			int compactedSize = Math.min(newSize,
					in0.getCompactedSize() + in1.getCompactedSize());
			Bit carryoutBit = getResultBus().getValue().getBit(
					compactedSize - 1);
			for (int i = compactedSize; i < newSize; i++) {
				if (newValue.getBit(i) != Bit.DONT_CARE)
					newValue.setBit(i, carryoutBit);
			}
		}

		mod |= getResultBus().pushValueForward(newValue);

		return mod;
	}

	/**
	 * Reverse constant prop on a MultiplyOp simply propagates the consumed
	 * value back to the Ports. Each port has the size not wider than the
	 * consumed value, all bits are care.
	 * 
	 * @return a value of type 'boolean'
	 */
	@Override
	public boolean pushValuesBackward() {
		boolean mod = false;

		Value resultBusValue = getResultBus().getValue();

		for (Port port : getDataPorts()) {
			mod |= port.pushValueBackward(resultBusValue);
		}

		return mod;
	}

	/*
	 * End new constant prop rules implementation.
	 * =================================================
	 */

}
