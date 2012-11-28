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

package org.xronos.openforge.lim.op;

import java.util.Collections;
import java.util.Map;

import org.xronos.openforge.lim.Bit;
import org.xronos.openforge.lim.Bus;
import org.xronos.openforge.lim.Emulatable;
import org.xronos.openforge.lim.Exit;
import org.xronos.openforge.lim.Port;
import org.xronos.openforge.lim.Value;
import org.xronos.openforge.lim.Visitor;
import org.xronos.openforge.report.FPGAResource;
import org.xronos.openforge.util.SizedInteger;


/**
 * A binary relational operation in a form of >=.
 * 
 * Created: Thu Mar 08 16:39:34 2002
 * 
 * @author Conor Wu
 * @version $Id: GreaterThanEqualToOp.java 2 2005-06-09 20:00:48Z imiller $
 */
public class GreaterThanEqualToOp extends ConditionalOp implements Emulatable {

	/**
	 * Constructs a relational greater than or equal to operation.
	 * 
	 */
	public GreaterThanEqualToOp() {
		super();
	}

	/**
	 * Accept method for the Visitor interface
	 */
	@Override
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
	@Override
	public int getGateDepth() {
		final int width = Math.max(getLeftDataPort().getValue().getSize(),
				getRightDataPort().getValue().getSize());
		return width + log2(width);
	}

	/**
	 * Gets the FPGA hardware resource usage of this component.
	 * 
	 * @return a FPGAResource objec
	 */
	@Override
	public FPGAResource getHardwareResourceUsage() {
		int lutCount = 1;

		Value leftValue = getLeftDataPort().getValue();
		Value rightValue = getRightDataPort().getValue();

		for (int i = 0; i < Math.max(leftValue.getSize(), rightValue.getSize()) - 1; i++) {
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

		// final int muxCount = lutCount - 1;

		FPGAResource hwResource = new FPGAResource();
		hwResource.addLUT(lutCount);

		return hwResource;
	}

	/**
	 * Performs a high level numerical emulation of this component.
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

		final Value resultValue = getResultBus().getValue();
		final int intValue = (lval.compareTo(rval) >= 0 ? 1 : 0);
		final SizedInteger result = SizedInteger.valueOf(intValue,
				resultValue.getSize(), resultValue.isSigned());

		return Collections.singletonMap(getResultBus(), result);
	}

	/*
	 * =================================================== Begin new constant
	 * prop rules implementation.
	 */

	/**
	 * Pushes size, care, and constant information forward through this
	 * GreaterThanEqualToOp according to this rule:
	 * 
	 * Result has only 1 care bit.
	 * 
	 * @return a value of type 'boolean'
	 */
	@Override
	public boolean pushValuesForward() {
		boolean mod = false;

		Value newValue = new Value(1, false);

		mod |= getResultBus().pushValueForward(newValue);

		return mod;
	}

	/**
	 * No rules can be applied on a GreaterThanEqualToOp.
	 * 
	 * @return a value of type 'boolean'
	 */
	@Override
	public boolean pushValuesBackward() {
		boolean mod = false;

		// No rules.

		return mod;
	}

	/*
	 * End new constant prop rules implementation.
	 * =================================================
	 */
}
