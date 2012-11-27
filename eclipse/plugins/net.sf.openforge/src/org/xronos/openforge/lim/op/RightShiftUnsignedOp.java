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
import org.xronos.openforge.lim.Port;
import org.xronos.openforge.lim.Value;
import org.xronos.openforge.lim.Visitor;
import org.xronos.openforge.util.SizedInteger;


/**
 * A binary bitwise and logical operation in a form of >>>.
 * 
 * Created: Thu Mar 08 16:39:34 2002
 * 
 * @author Conor Wu
 * @version $Id: RightShiftUnsignedOp.java 2 2005-06-09 20:00:48Z imiller $
 */
public class RightShiftUnsignedOp extends ShiftOp implements Emulatable {

	/**
	 * Constructs a logical unsigned right shift operation.
	 * 
	 * @param maxStages
	 *            the maximum number of stages (shifts by 2^n) necessary to
	 *            implement this shift
	 */
	public RightShiftUnsignedOp(int maxStages) {
		super(maxStages);
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
		return Collections.singletonMap(getResultBus(),
				lval.shiftRightUnsigned(rval));
	}

	/*
	 * =================================================== Begin new constant
	 * prop rules implementation.
	 */

	/**
	 * Pushes size, care, and constant information forward through this
	 * RightShiftUnsignedOp according to these rules:
	 * 
	 * If both inputs are care (non-constant): Result size is data input size.
	 * All result bits are care. If shift magnitude is constant value: 1. if we
	 * shift beyond our inputs size, then our output is simply constant 0. 2. if
	 * we shift within our inputs size, then the MSB bits are all constant 0,
	 * and the Next 'y' bits are all passthrough of the top 'y' bits of data
	 * input value. Result is signed if data value is signed.
	 * 
	 * @return a value of type 'boolean'
	 */
	@Override
	public boolean pushValuesForward() {
		boolean mod = false;

		Value in0 = getLeftDataPort().getValue();
		Value in1 = getRightDataPort().getValue();

		Value newValue = new Value(in0.getSize(), in0.isSigned());

		if (in1.isConstant()) {
			int shiftMag = getShiftMagnitude();

			if (newValue.getSize() < shiftMag) {
				// If we shift beyond our inputs size then our output
				// is simply the sign bit of the input.
				for (int i = 0; i < newValue.getSize(); i++) {
					newValue.setBit(i, Bit.ZERO);
				}
			} else {
				int position = 0;
				for (; position < newValue.getSize() - shiftMag; position++) {
					newValue.setBit(position, in0.getBit(position + shiftMag));
				}
				for (; position < newValue.getSize(); position++) {
					newValue.setBit(position, Bit.ZERO);
				}
			}
		} else {
			for (int i = in0.getCompactedSize(); i < in0.getSize(); i++) {
				newValue.setBit(i, in0.getBit(i));
			}
		}

		mod |= getResultBus().pushValueForward(newValue);

		return mod;
	}

	/**
	 * Reverse constant prop on a RightShiftUnsignedOp applies only if the shift
	 * magnitude is a constant value. Constant/don't care/passthrough bits are
	 * taken from result to data port based on shift magnitude offset.
	 * 
	 * @return a value of type 'boolean'
	 */
	@Override
	public boolean pushValuesBackward() {
		boolean mod = false;

		Value resultBusValue = getResultBus().getValue();

		// Value inValue0 = getLeftDataPort().getValue();
		Value inValue1 = getRightDataPort().getValue();

		Value newValue0 = new Value(resultBusValue.getSize(),
				resultBusValue.isSigned());

		if (inValue1.isConstant()) {
			int shiftMag = getShiftMagnitude();

			if (newValue0.getSize() <= shiftMag) {
				for (int i = 0; i < newValue0.getSize(); i++) {
					newValue0.setBit(i, Bit.DONT_CARE);
				}
			} else {
				for (int i = 0; i < (newValue0.getSize() - shiftMag); i++) {
					Bit bit = resultBusValue.getBit(i);
					if (!bit.isCare()) {
						newValue0.setBit(i + shiftMag, Bit.DONT_CARE);
					}
				}
			}
		}

		Value newValue1 = new Value(inValue1.getSize(), inValue1.isSigned());
		for (int i = getMaxStages(); i < newValue1.getSize(); i++) {
			newValue1.setBit(i, Bit.DONT_CARE);
		}

		mod |= getLeftDataPort().pushValueBackward(newValue0);
		mod |= getRightDataPort().pushValueBackward(newValue1);

		return mod;
	}

	/*
	 * End new constant prop rules implementation.
	 * =================================================
	 */
}
