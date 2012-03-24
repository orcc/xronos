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

import java.util.List;

/**
 * An Not accepts a single 1-bit input and generates a 1-bit result which is the
 * inverse of the input. The go port and done bus of an Not are unused.
 * 
 * Created: April 25, 2002
 * 
 * @author <a href="mailto:andreas.kollegger@xilinx.com">Andreas Kollegger</a>
 * @version $Id: Not.java 2 2005-06-09 20:00:48Z imiller $
 */
public class Not extends Primitive {

	/**
	 * Constructor for the Not object
	 * 
	 */
	public Not() {
		super(1);
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
		return getDataPort().getValue().isConstant() ? 0 : 1;
	}

	public Port getDataPort() {
		return getDataPorts().get(0);
	}

	@Override
	public Port makeDataPort() {
		List<Port> dps = getDataPorts();
		assert (dps.size() == 0) : "Not only accepts a single data input.";

		return super.makeDataPort();
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
	 * Performs forward constant propagation through this component. This
	 * component will fetch the incoming {@link Value} from each {@link Port}
	 * using {@link Port#_getValue()}. It will then compute a new outgoing
	 * {@link Value} for each {@link Bus} and set it with
	 * {@link Bus#pushValueForward(Value)}.
	 * <P>
	 * In particular, a constant {@link Bit} will be propragated as its
	 * inversion. If the input {@link Bit} is non-global, then the output will
	 * be the local {@link Bit} of the result {@link Bus}, and the input
	 * {@link Bit} will be set as its inversion using
	 * {@link Bit#setInvertedBit(Bit)}.
	 * 
	 * @return true if any of the bus values was modified, false otherwise
	 */
	@Override
	public boolean pushValuesForward() {
		boolean mod = false;

		final Value inValue = getDataPort().getValue();
		assert (inValue.getSize() == 1) : ("invalid not input size: " + inValue
				.getSize());
		final Value pushValue = new Value(1, false);

		final Bit inBit = inValue.getBit(0);
		if (!inBit.isCare()) {
			/*
			 * Don't-cares will be ignored going forward.
			 */
			pushValue.setBit(0, Bit.DONT_CARE);
		} else if (inBit.isConstant()) {
			/*
			 * Push the inversion of the constant.
			 */
			pushValue.setBit(0, inBit.isOn() ? Bit.ZERO : Bit.ONE);
		} else {
			/*
			 * Otherwise just push a generic CARE until we're sure that there's
			 * a Value on the result Bus.
			 */
			pushValue.setBit(0, Bit.CARE);
		}

		mod |= getResultBus().pushValueForward(pushValue);

		if (!inBit.isGlobal()
				&& !getResultBus().getValue().getBit(0).isGlobal()) {
			/*
			 * Set the inversion shortcut if appropriate.
			 */
			getResultBus().getValue().getBit(0).setInvertedBit(inBit);
		}

		return mod;
	}

	/**
	 * No rules can be applied on a Not.
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

} // class Not

