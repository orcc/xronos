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
 * A unary conditional operation in a form of !.
 * 
 * Created: Thu Mar 08 16:39:34 2002
 * 
 * @author Conor Wu
 * @version $Id: NotOp.java 2 2005-06-09 20:00:48Z imiller $
 */
public class NotOp extends UnaryOp implements Emulatable {

	/**
	 * Constructs a conditional not operation.
	 * 
	 */
	public NotOp() {
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
		return isPassthrough() ? 0 : 1;
	}

	/**
	 * Gets the FPGA hardware resource usage of this component.
	 * 
	 * @return a FPGAResource objec
	 */
	@Override
	public FPGAResource getHardwareResourceUsage() {
		int lutCount = 0;

		Value inputValue = getDataPort().getValue();
		for (int i = 0; i < inputValue.getSize(); i++) {
			Bit inputBit = inputValue.getBit(i);
			if (!inputBit.isConstant() && inputBit.isCare()) {
				lutCount++;
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
	@Override
	public Map<Bus, SizedInteger> emulate(Map<Port, SizedInteger> portValues) {
		final SizedInteger inval = portValues.get(getDataPort());

		final Value resultValue = getResultBus().getValue();
		final int intValue = (inval.isZero() ? 1 : 0);
		final SizedInteger result = SizedInteger.valueOf(intValue,
				resultValue.getSize(), resultValue.isSigned());

		return Collections.singletonMap(getResultBus(), result);
	}

	/*
	 * =================================================== Begin new constant
	 * prop rules implementation.
	 */

	/**
	 * Pushes size, care, and constant information forward through this NotOp
	 * according to this rule:
	 * 
	 * Result has only 1 care bit.
	 * 
	 * @return a value of type 'boolean'
	 */
	@Override
	public boolean pushValuesForward() {
		boolean mod = false;

		Value inValue = getDataPort().getValue();

		Value newValue = new Value(inValue.getSize(), false);

		Bit bit = inValue.getBit(0);

		if (!bit.isCare()) {
			/*
			 * Don't-cares will be ignored going forward.
			 */
			newValue.setBit(0, Bit.DONT_CARE);
		} else if (bit.isConstant()) {
			/*
			 * Push the inversion of the constant.
			 */
			newValue.setBit(0, bit.isOn() ? Bit.ZERO : Bit.ONE);
		} else {
			/*
			 * Otherwise just push a generic CARE until we're sure that there's
			 * a Value on the result Bus.
			 */
			newValue.setBit(0, Bit.CARE);
		}

		for (int i = 1; i < inValue.getSize(); i++) {
			newValue.setBit(i, Bit.ZERO);
		}

		mod |= getResultBus().pushValueForward(newValue);

		if (!bit.isGlobal() && !getResultBus().getValue().getBit(0).isGlobal()) {
			/*
			 * Set the inversion shortcut if appropriate.
			 */
			getResultBus().getValue().getBit(0).setInvertedBit(bit);
		}

		return mod;
	}

	/**
	 * No rules can be applied on a NotOp.
	 * 
	 * @return a value of type 'boolean'
	 */
	@Override
	public boolean pushValuesBackward() {
		boolean mod = false;

		Value resultBusValue = getResultBus().getValue();

		Value newValue = new Value(resultBusValue.getSize(),
				resultBusValue.isSigned());

		for (int i = resultBusValue.getSize() - 1; i >= 0; i--) {
			Bit bit = resultBusValue.getBit(i);

			if (!bit.isCare()) {
				newValue.setBit(i, Bit.DONT_CARE);
			}
		}

		mod |= getDataPort().pushValueBackward(newValue);

		return mod;
	}

	/*
	 * End new constant prop rules implementation.
	 * =================================================
	 */

	/**
	 * Tests whether this is a passthrough operation.
	 * 
	 * @return true if the output of this component can be reduced to a constant
	 *         or the value of a single input, based upon the current
	 *         {@link Value} of each data {@link Port}
	 */
	private boolean isPassthrough() {
		return getDataPort().getValue().isConstant();
	}
}
