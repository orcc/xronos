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
import org.xronos.openforge.util.SizedInteger;


/**
 * A unary arithmetic negation operation in a form of -.
 * 
 * Created: Thu Mar 08 16:39:34 2002
 * 
 * @author Conor Wu
 * @version $Id: MinusOp.java 2 2005-06-09 20:00:48Z imiller $
 */
public class MinusOp extends UnaryOp implements Emulatable {

	/**
	 * Constructs an arithmetic negation minus operation.
	 * 
	 */
	public MinusOp() {
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
		final int width = getDataPort().getValue().getSize();
		return (3 * (width - 1)) + 2;
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
		final SizedInteger inval = portValues.get(getDataPort());
		return Collections.singletonMap(getResultBus(), inval.negate());
	}

	/*
	 * =================================================== Begin new constant
	 * prop rules implementation.
	 */

	/**
	 * Pushes size, care, and constant information forward through this MinusOp
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

		boolean keepGoing = true;

		Value inValue = getDataPort().getValue();
		int newSize = inValue.getSize();

		Value newValue = new Value(newSize, inValue.isSigned());

		long value = -inValue.getValueMask();

		for (int i = 0; i < newSize && keepGoing; i++) {
			Bit bit0 = inValue.getBit(i);

			if (bit0.isConstant()) {
				if (((value >>> i) & 0x1L) != 0)
					newValue.setBit(i, Bit.ONE);
				else
					newValue.setBit(i, Bit.ZERO);
			} else {
				keepGoing = false;
			}
		}

		// update all bits above the carry out bit to be signed
		// extended of carry out bit
		if (getResultBus().getValue() != null) {
			if (!inValue.isConstant()) {
				int compactedSize = Math.min(newSize,
						inValue.getCompactedSize() + 1);
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
	 * Reverse constant prop on an MinusOp simply propagates the result bus
	 * value back to the Ports. Any bits in the output produce care bits on the
	 * inputs.
	 * 
	 * @return a value of type 'boolean'
	 */
	@Override
	public boolean pushValuesBackward() {
		boolean mod = false;

		Value resultBusValue = getResultBus().getValue();

		Value newValue = new Value(resultBusValue.getSize(),
				resultBusValue.isSigned());
		for (int i = 0; i < resultBusValue.getSize(); i++) {
			Bit bit = resultBusValue.getBit(i);
			if (!bit.isCare() || bit.isConstant()) {
				newValue.setBit(i, Bit.DONT_CARE);
			}
		}

		if (!getDataPort().getValue().isConstant()) {
			mod |= getDataPort().pushValueBackward(newValue);
		}

		return mod;
	}

}
