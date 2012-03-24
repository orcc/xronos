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
 * A binary arithmetic operation in a form of -.
 * 
 * Created: Thu Mar 08 16:39:34 2002
 * 
 * @author Conor Wu
 * @version $Id: SubtractOp.java 2 2005-06-09 20:00:48Z imiller $
 */
public class SubtractOp extends BinaryOp implements Emulatable {

	/**
	 * Constructs an arithmetic subtract operation.
	 * 
	 */
	public SubtractOp() {
		super();
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
		return Collections.singletonMap(getResultBus(), lval.subtract(rval));
	}

	/*
	 * =================================================== Begin new constant
	 * prop rules implementation.
	 */

	/**
	 * Pushes size, care, and constant information forward through this
	 * SubtractOp according to this rule:
	 * 
	 * All result bit are cares.
	 * 
	 * @return a value of type 'boolean'
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
	 * Reverse constant prop on a SubtractOp simply propagates the result bus
	 * value back to the Ports. Any bits in the output produce care bits on the
	 * inputs.
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

	public String getReplacementMethodName() {
		// return OperationReplacementVisitor.SUBNAME;

		// FIXME: Do this until Ian resolves the problems with
		// OperationReplacementVisitor.
		return "sub";
	}
}
