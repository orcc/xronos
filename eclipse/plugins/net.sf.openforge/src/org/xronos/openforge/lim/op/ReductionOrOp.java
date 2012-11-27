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

import org.xronos.openforge.lim.Bit;
import org.xronos.openforge.lim.Exit;
import org.xronos.openforge.lim.Port;
import org.xronos.openforge.lim.Value;
import org.xronos.openforge.lim.Visitor;
import org.xronos.openforge.report.FPGAResource;

/**
 * An or reduction operation in a form of |.
 * 
 * Created: Mon May 19 16:39:34 2003
 * 
 * @author Conor Wu
 * @version $Id: ReductionOrOp.java 2 2005-06-09 20:00:48Z imiller $
 */
public class ReductionOrOp extends ReductionOp {

	/**
	 * Constructs an or reduction operation.
	 * 
	 */
	public ReductionOrOp() {
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

	/*
	 * =================================================== Begin new constant
	 * prop rules implementation.
	 */

	/**
	 * Pushes size, care, and constant information forward through this
	 * ReductionOrOp according to this rule:
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

		for (int i = 1; i < inValue.getSize(); i++) {
			newValue.setBit(i, Bit.ZERO);
		}

		mod |= getResultBus().pushValueForward(newValue);

		return mod;
	}

	/**
	 * No rules can be applied on a ReductionOrOp.
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

	/**
	 * Tests whether this is a passthrough operation.
	 * 
	 * @return true if the output of this component can be reduced to a constant
	 *         or the value of a single input, based upon the current
	 *         {@link Value} of each data {@link Port}
	 */
	private boolean isPassthrough() {
		return false;
	}
}
