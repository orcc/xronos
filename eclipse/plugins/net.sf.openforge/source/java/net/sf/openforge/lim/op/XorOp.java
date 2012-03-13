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
 * A binary bitwise and logical operation in a form of ^.
 * 
 * Created: Thu Mar 08 16:39:34 2002
 * 
 * @author Conor Wu
 * @version $Id: XorOp.java 2 2005-06-09 20:00:48Z imiller $
 */
public class XorOp extends BinaryOp implements Emulatable {

	/**
	 * Constructs a bitwise xor operation.
	 * 
	 */
	public XorOp() {
		super();
	}

	/**
	 * Accept method for the Visitor interface
	 */
	public void accept(Visitor visitor) {
		visitor.visit(this);
	}

	/**
	 * Gets the gate depth of this component. This is the maximum number of
	 * gates that any input signal must traverse before reaching an {@link Exit}
	 * .
	 * 
	 * @return a non-negative integer
	 */
	public int getGateDepth() {
		return isBitwisePassthrough() ? 0 : 1;
	}

	/**
	 * Gets the FPGA hardware resource usage of this component.
	 * 
	 * @return a FPGAResource objec
	 */
	public FPGAResource getHardwareResourceUsage() {
		int lutCount = 0;

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

		FPGAResource hwResource = new FPGAResource();
		hwResource.addLUT(lutCount);

		return hwResource;
	}

	/**
	 * Performes a high level numerical emulation of this component.
	 * 
	 * @param portValues
	 *            a map of owner {@link Port} to {@link SizedInteger} input
	 *            value
	 * @return a map of {@link Bus} to {@link SizedInteger} result value
	 */
	public Map<Bus, SizedInteger> emulate(Map<Port, SizedInteger> portValues) {
		final SizedInteger lval = (SizedInteger) portValues
				.get(getLeftDataPort());
		final SizedInteger rval = (SizedInteger) portValues
				.get(getRightDataPort());
		return Collections.singletonMap(getResultBus(), lval.xor(rval));
	}

	/*
	 * =================================================== Begin new constant
	 * prop rules implementation.
	 */

	/**
	 * Pushes size, care, and constant information forward through this XorOp
	 * according to these rules:
	 * 
	 * <pre>
	 * x ^ * = x
	 * 0 ^ c = c
	 * 1 ^ c = c
	 * 0 ^ 1 = 1
	 * 0 ^ 0 = 0
	 * 1 ^ 1 = 0
	 * Result size is input size
	 * </pre>
	 * 
	 * @return a value of type 'boolean'
	 */
	public boolean pushValuesForward() {
		boolean mod = false;

		Value in0 = getLeftDataPort().getValue();
		Value in1 = getRightDataPort().getValue();

		int newSize = Math.max(in0.getSize(), in1.getSize());
		boolean isSigned = in0.isSigned() && in1.isSigned();

		Value newValue = new Value(newSize, isSigned);

		for (int i = 0; i < newSize; i++) {
			Bit bit0 = in0.getBit(i);
			Bit bit1 = in1.getBit(i);

			if (!bit0.isCare() || !bit1.isCare()) {
				newValue.setBit(i, Bit.DONT_CARE);
			} else if (bit0.isConstant() && bit1.isConstant()) {
				if (bit0 == bit1) {
					newValue.setBit(i, Bit.ZERO);
				} else {
					newValue.setBit(i, Bit.ONE);
				}
			} else if (bit0.isConstant() && !bit0.isOn()) {
				newValue.setBit(i, bit1);
			} else if (bit1.isConstant() && !bit1.isOn()) {
				newValue.setBit(i, bit0);
			}
		}

		// update all bits above the carry out bit to be signed
		// extended of carry out bit
		if (getResultBus().getValue() != null) {
			if (!in0.isConstant() || !in1.isConstant()) {
				int compactedSize = Math.min(newSize, Math.max(
						in0.getCompactedSize(), in1.getCompactedSize()));
				Bit carryoutBit = getResultBus().getValue().getBit(
						compactedSize - 1);

				for (int i = compactedSize; i < newSize; i++) {
					if (newValue.getBit(i) != Bit.DONT_CARE)
						newValue.setBit(i, carryoutBit);
				}
			}
		}

		mod |= getResultBus().pushValueForward(newValue);

		for (int i = 0; i < newSize; i++) {
			Bit bit0 = in0.getBit(i);
			Bit bit1 = in1.getBit(i);

			if (bit0.isConstant() && bit0.isOn() && !bit1.isGlobal()) {
				/*
				 * Set the inversion shortcut if appropriate.
				 */
				getResultBus().getValue().getBit(i).setInvertedBit(bit1);
			} else if (bit1.isConstant() && bit1.isOn() && !bit0.isGlobal()) {
				/*
				 * Set the inversion shortcut if appropriate.
				 */
				getResultBus().getValue().getBit(i).setInvertedBit(bit0);
			}
		}
		return mod;
	}

	/**
	 * Reverse constant prop on a XorOp simply propagates the consumed value
	 * back to the Ports. Any constant/dc bits in the output produce don't care
	 * bits on the inputs.
	 * 
	 * @return a value of type 'boolean'
	 */
	public boolean pushValuesBackward() {
		boolean mod = false;

		Value resultBusValue = getResultBus().getValue();

		Value newValue = new Value(resultBusValue.getSize(),
				resultBusValue.isSigned());

		for (int i = 0; i < resultBusValue.getSize(); i++) {
			if (resultBusValue.getBit(i).isConstant()
					|| !resultBusValue.getBit(i).isCare()) {
				newValue.setBit(i, Bit.DONT_CARE);
			}
		}

		for (Port port : getDataPorts()) {
			mod |= port.pushValueBackward(newValue);
		}

		return mod;
	}

	/*
	 * End new constant prop rules implementation.
	 * =================================================
	 */

	protected boolean isSignificant(Bit leftBit, Bit rightBit) {
		if ((leftBit == Bit.ZERO) || (rightBit == Bit.ZERO)) {
			return false;
		} else if ((leftBit == Bit.DONT_CARE) || (rightBit == Bit.DONT_CARE)) {
			return false;
		} else {
			// return (leftBit == Bit.CARE) || (rightBit == Bit.CARE);
			final boolean leftSig = !leftBit.isConstant() && leftBit.isCare();
			final boolean rightSig = !rightBit.isConstant()
					&& rightBit.isCare();
			return (leftSig || rightSig);
		}
	}
}
