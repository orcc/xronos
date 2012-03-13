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
import net.sf.openforge.lim.Port;
import net.sf.openforge.lim.Value;
import net.sf.openforge.lim.Visitor;
import net.sf.openforge.util.SizedInteger;

/**
 * A binary bitwise and logical operation in a form of <<.
 * 
 * Created: Thu Mar 08 16:39:34 2002
 * 
 * @author Conor Wu
 * @version $Id: LeftShiftOp.java 2 2005-06-09 20:00:48Z imiller $
 */
public class LeftShiftOp extends ShiftOp implements Emulatable {

	/**
	 * Constructs a logical left shift operation with a given maximum number of
	 * stages.
	 * 
	 * @param maxStages
	 *            the maximum number of stages (shifts by 2^n) necessary to
	 *            implement this shift
	 */
	public LeftShiftOp(int maxStages) {
		super(maxStages);
	}

	/**
	 * Accept method for the Visitor interface
	 */
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
	public Map<Bus, SizedInteger> emulate(Map<Port, SizedInteger> portValues) {
		final SizedInteger lval = (SizedInteger) portValues
				.get(getLeftDataPort());
		final SizedInteger rval = (SizedInteger) portValues
				.get(getRightDataPort());

		final int rvalSize = rval.getSize();
		final SizedInteger one = SizedInteger.valueOf(1, rvalSize, true);
		final SizedInteger leftSize = SizedInteger.valueOf(getLeftDataPort()
				.getValue().getSize(), rvalSize, true);
		final SizedInteger maskedRval = rval.and(leftSize.subtract(one));

		return Collections.singletonMap(getResultBus(),
				lval.shiftLeft(maskedRval));
	}

	/*
	 * =================================================== Begin new constant
	 * prop rules implementation.
	 */

	/**
	 * Pushes care, and constant information forward through this LeftShiftOp
	 * according to these rules:
	 * 
	 * If both inputs are all care bits (or have some care bits): All result
	 * bits are care.
	 * 
	 * If shift magnitude is a constant value: LSB bits are all 0's. Next 'y'
	 * bits are all passthrough of the data input value. y is the width of the
	 * data input up to a max of 'result width' total bits (including lsb 0's)
	 * 
	 * @return a value of type 'boolean'
	 */
	public boolean pushValuesForward() {
		boolean mod = false;

		Value in0 = getLeftDataPort().getValue();
		Value in1 = getRightDataPort().getValue();

		boolean isSigned = in0.isSigned();

		Value newValue = new Value(in0.getSize(), isSigned);

		if (in1.isConstant()) {
			int shiftMag = getShiftMagnitude();
			if (newValue.getSize() < shiftMag) {
				// If we shift beyond what is consumed then we
				// effectively produce a constant 0.
				for (int i = 0; i < newValue.getSize(); i++) {
					newValue.setBit(i, Bit.ZERO);
				}
			} else {
				int position = 0;
				for (; position < shiftMag; position++) {
					newValue.setBit(position, Bit.ZERO);
				}
				int offset = 0;
				for (; position < newValue.getSize(); position++) {
					newValue.setBit(position, in0.getBit(offset++));
				}
			}
		}

		mod |= getResultBus().pushValueForward(newValue);
		return mod;
	}

	/**
	 * Reverse constant prop on a LeftShiftOp has two cases: 1. If both inputs
	 * are all care bits (or have some care bits), port 0 size is result size.
	 * 2. If shift magnitude is a constant value, result (consumed) bit states
	 * can push back directly to port states shifted by 'n' positions.
	 * 
	 * ABK: Also, the shift magnitude can be limited to the maximum stages
	 * annotated on the shift.
	 * 
	 * @return a value of type 'boolean'
	 */

	public boolean pushValuesBackward() {
		boolean mod = false;

		Value resultBusValue = getResultBus().getValue();
		@SuppressWarnings("unused")
		Value inValue0 = getLeftDataPort().getValue();
		Value inValue1 = getRightDataPort().getValue();

		Value newValue0 = new Value(resultBusValue.getSize(),
				resultBusValue.isSigned());

		if (inValue1.isConstant()) {
			//
			// If the shift amount is a constant value then we want
			// to:
			// - Back propagate any don't care bits from the consumed
			// value
			//

			int shiftMag = getShiftMagnitude();
			if (newValue0.getSize() <= shiftMag) {
				// If we are shifting by more than anyone consumes
				// then our result is 0 and we don't need any of the
				// input bits.
				// Since this operation should be replaced by a
				// constant 0 the next pass through half constant,
				// we'll do nothing to inValue0.
			} else {
				for (int i = 0; i < (newValue0.getSize() - shiftMag); i++) {
					Bit bit = resultBusValue.getBit(i + shiftMag);
					if (!bit.isCare()) {
						newValue0.setBit(i, Bit.DONT_CARE);
					}
				}
				// Rest of the higher order bits become DONT_CARE
				for (int i = (newValue0.getSize() - shiftMag); i < newValue0
						.getSize(); i++) {
					newValue0.setBit(i, Bit.DONT_CARE);
				}
			}
		} else {
			/*
			 * Since we're shifting left, we can keep chop off don't-care bits
			 * from the left end.
			 */
			for (int i = newValue0.getSize() - 1; i > 0; i--) {
				final Bit bit = resultBusValue.getBit(i);
				if (!bit.isCare()) {
					newValue0.setBit(i, Bit.DONT_CARE);
				} else {
					break;
				}
			}
		}

		Value newValue1 = new Value(inValue1.getSize(), inValue1.isSigned());
		if (!inValue1.isConstant()) {
			for (int i = getMaxStages(); i < newValue1.getSize(); i++) {
				newValue1.setBit(i, Bit.DONT_CARE);
			}
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
