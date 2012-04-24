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
package net.sf.openforge.lim;

import java.util.Iterator;

import net.sf.openforge.lim.op.AndOp;
import net.sf.openforge.report.FPGAResource;

/**
 * An And accepts multiple 1-bit signals, and'ing them together to generate a
 * 1-bit result. The go port and done bus of an And are unused.
 * 
 * Created: April 25, 2002
 * 
 * @author <a href="mailto:andreas.kollegger@xilinx.com">Andreas Kollegger</a>
 * @version $Id: And.java 2 2005-06-09 20:00:48Z imiller $
 */
public class And extends Primitive {

	/**
	 * Constructor for the And object
	 * 
	 * @param goCount
	 *            The number of data inputs to be and'd
	 */
	public And(int goCount) {
		super(goCount);
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
		return log2(getMaxCareBits());
	}

	/**
	 * Gets the FPGA hardware resource usage of this component.
	 * 
	 * @return a FPGAResource objec
	 */
	@Override
	public FPGAResource getHardwareResourceUsage() {
		int terms = getDataPorts().size();
		int groupedCount = 0;

		while (terms > 1) {
			groupedCount += terms >> 2;
			terms = (terms >> 2) + (terms % 4);
			if (terms < 4) {
				groupedCount += 1;
				break;
			}
		}

		final int maxCareBits = getMaxCareBits();
		final int lutCount = maxCareBits * groupedCount;

		FPGAResource hwResource = new FPGAResource();
		hwResource.addLUT(lutCount);

		return hwResource;
	}

	@Override
	public void accept(Visitor v) {
		v.visit(this);
	}

	/*
	 * =================================================== Begin new constant
	 * prop rules implementation.
	 */

	/**
	 * Defers to {@link AndOp#pushValueForwardAnd} for rules.
	 */
	@Override
	public boolean pushValuesForward() {
		boolean mod = false;

		assert getDataPorts().size() > 0;

		Iterator<Port> dpIter = getDataPorts().iterator();
		Value andValue = dpIter.next().getValue();
		while (dpIter.hasNext()) {
			Value nextValue = dpIter.next().getValue();
			andValue = AndOp.pushValuesForwardAnd(andValue, nextValue);
		}
		mod |= getResultBus().pushValueForward(andValue);
		return mod;
	}

	/**
	 * Reverse constant prop on an AndOp simply propagates the consumed value
	 * back to the Ports. Any constant/dc bits in the output produce don't care
	 * bits on the inputs.
	 */
	@Override
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

} // class And

