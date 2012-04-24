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
 * A binary arithmetic operation in a form of +.
 * 
 * Created: Thu Mar 08 16:39:34 2002
 * 
 * @author Conor Wu
 * @version $Id: AddOp.java 2 2005-06-09 20:00:48Z imiller $
 */
public class AddOp extends BinaryOp implements Emulatable {

	/**
	 * Constructs an arithmetic add operation.
	 * 
	 */
	public AddOp() {
		super();
	}

	/**
	 * returns false, an indication that this add op has more than 2 inputs.
	 */
	public boolean hasMulti() {
		return false;
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
		return (3 * (width - 1)) + 2;
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

		// muxCount = lutCount - 1;

		FPGAResource hwResource = new FPGAResource();
		hwResource.addLUT(lutCount);

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
		return Collections.singletonMap(getResultBus(), lval.add(rval));
	}

	/*
	 * =================================================== Begin new constant
	 * prop rules implementation.
	 */

	/**
	 * Pushes size, care, and constant information forward through this AddOp
	 * according to this rule:
	 * 
	 * All result bit are cares. Any consecutive constant lsb bits on both
	 * inputs can be pre-calculated by doing the addition.
	 * 
	 * @return a value of type 'boolean'
	 */
	@Override
	public boolean pushValuesForward() {
		boolean mod = false;

		boolean carry = false;
		boolean keepAdding = true;

		Value in0 = getLeftDataPort().getValue();
		Value in1 = getRightDataPort().getValue();

		int newSize = Math.max(in0.getSize(), in1.getSize());
		boolean isSigned = in0.isSigned() && in1.isSigned();
		Value newValue = new Value(newSize, isSigned);

		for (int i = 0; i < newSize; i++) {
			Bit bit0 = in0.getBit(i);
			Bit bit1 = in1.getBit(i);

			if (!bit0.isCare() && !bit1.isCare()) {
				newValue.setBit(i, Bit.DONT_CARE);
				keepAdding = false;
			} else {
				if (bit0.isConstant() && bit1.isConstant() && keepAdding) {
					int numOnes = 0;
					if (bit0.isOn())
						numOnes++;
					if (bit1.isOn())
						numOnes++;
					if (carry)
						numOnes++;

					newValue.setBit(i, ((numOnes & 0x1) != 0) ? Bit.ONE
							: Bit.ZERO);
					carry = numOnes > 1;
				} else {
					newValue.setBit(i, Bit.CARE);
					keepAdding = false;
				}
			}
		}

		// update all bits above the carry out bit to be signed
		// extended of carry out bit
		if (getResultBus().getValue() != null) {
			if (!in0.isConstant() || !in1.isConstant()) {
				int compactedSize = Math.min(newSize, Math.max(
						in0.getCompactedSize(), in1.getCompactedSize()) + 1);
				Bit carryoutBit = getResultBus().getValue().getBit(
						compactedSize - 1);

				for (int i = compactedSize; i < newSize; i++) {
					if (newValue.getBit(i) != Bit.DONT_CARE)
						newValue.setBit(i, carryoutBit);
				}
			}
		}

		mod |= getResultBus().pushValueForward(newValue);

		return mod;
	}

	/**
	 * Reverse constant prop on an AddOp simply propagates the result bus value
	 * back to the Ports. Any bits in the output produce care bits on the
	 * inputs.
	 * 
	 * @return a value of type 'boolean'
	 */
	@Override
	public boolean pushValuesBackward() {
		boolean mod = false;

		Value resultBusValue = getResultBus().getValue();
		for (Port dataPort : getDataPorts()) {
			Value newPushBackValue = new Value(dataPort.getValue().getSize(),
					dataPort.getValue().isSigned());
			for (int i = 0; i < newPushBackValue.getSize(); i++) {
				if (i >= resultBusValue.getCompactedSize()) {
					Bit bit = resultBusValue.getBit(i);
					if (!bit.isCare() || bit.isConstant()) {
						newPushBackValue.setBit(i, Bit.DONT_CARE);
					}
				} else {
					newPushBackValue.setBit(i, dataPort.getValue().getBit(i));
				}
			}

			mod |= dataPort.pushValueBackward(newPushBackValue);

		}

		return mod;
	}

	/*
	 * End new constant prop rules implementation.
	 * =================================================
	 */
}
