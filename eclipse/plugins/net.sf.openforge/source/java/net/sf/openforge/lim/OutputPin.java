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

import java.util.Collection;
import java.util.Collections;

/**
 * A pin to which the {@link Design} may write external data.
 * 
 * @author Stephen Edwards
 * @version $Id: OutputPin.java 2 2005-06-09 20:00:48Z imiller $
 */
public class OutputPin extends Pin {

	private OutPinBuf buf;
	private Port port;
	
	@SuppressWarnings("unused")
	private int portWidth = -1;

	/** True iff this pin was derived implicitly from a Port */
	private boolean isInferred = false;

	/**
	 * Constructs a new OutputPin based on a Bus, and connects the OutputPin's
	 * Port to the Bus.
	 * <p>
	 * XXX FIXME. The OutputPin structure isn't quite correct. For Character
	 * data types the Pin will report it's width as 16 bits but the Port
	 * attached to it will be 17 bits. We rely on the fact that Verilog allows a
	 * smaller wire to be connected to a larger port on the instantiation of the
	 * entry method to resolve this discrepancy which is a bad idea!
	 * 
	 * @param bus
	 *            the Bus upon which to base the OutputPin
	 */
	public OutputPin(Bus bus) {
		this(bus.getValue().getSize(), bus.getValue().isSigned());
		getPort().setBus(bus);
		this.isInferred = true;
	}

	/**
	 * 
	 * @param pinWidth
	 * @param portWidth
	 */
	public OutputPin(int pinWidth, int portWidth, boolean isSigned) {
		super(Math.min(pinWidth, portWidth), isSigned);

		this.portWidth = portWidth;
		this.buf = new OutPinBuf(this);
		this.port = makeDataPort();
	}

	/**
	 * Constructs a new OutputPin with explicit width and signedness.
	 * 
	 * @param width
	 * @param isSigned
	 */
	public OutputPin(int width, boolean isSigned) {
		this(width, width, isSigned);
	}

	public OutPinBuf getOutPinBuf() {
		return buf;
	}

	public Collection<OutPinBuf> getPinBufs() {
		return Collections.singleton(getOutPinBuf());
	}

	public Port getPort() {
		return port;
	}

	public int getWidth() {
		// Because the width is the default size when this pin is
		// created , and it never got changed after the constant
		// propagation. On the other hand, the translator uses the
		// port's compacted value, and the testBench uses the default
		// size. In order to let testBenchWriter generate the
		// size-matching test vectors, we choose the minimum of
		// default size and compacted value size.
		if (getPort() != null && getPort().getValue() != null) {
			return Math.min(super.getWidth(), getPort().getValue()
					.getCompactedSize());
		} else {
			return super.getWidth();
		}
	}

	/**
	 * Augments the default behavior by also forcing the logical ID of the Bus
	 * attached to the OutputPin's Port to match.
	 * 
	 * @param s
	 *            the new logical ID
	 */
	public void setIDLogical(String s) {
		super.setIDLogical(s);
	}

	public boolean removeDataPort(Port port) {
		if (super.removeDataPort(port)) {
			if (port == this.port)
				this.port = null;
			return true;
		}
		return false;
	}

	/*
	 * =================================================== Begin new constant
	 * prop rules implementation.
	 */

	/**
	 * Returns false, does nothing.
	 */
	public boolean pushValuesForward() {
		return false;
	}

	/**
	 * Looks at the Value on the input Port and replaces the all the MSB
	 * constants with don't cares (except for a leading sign bit, if necessary).
	 */
	public boolean pushValuesBackward() {
		boolean mod = false;

		final Value portValue = getPort().getValue();
		final Value newValue = new Value(portValue, portValue.isSigned());

		for (int i = newValue.getCompactedSize(); i < newValue.getSize(); i++) {
			newValue.setBit(i, Bit.DONT_CARE);
		}

		mod = getPort().pushValueBackward(newValue);

		return mod;

	}

	/**
	 * Tests whether this pin was inferred or created explicitly by the user.
	 * 
	 * @return true if this pin was created with
	 *         {@link OutputPin#OutputPin(Bus)}, false otherwise
	 */
	public boolean isInferred() {
		return isInferred;
	}

	/*
	 * End new constant prop rules implementation.
	 * =================================================
	 */

	/**
	 * Returns a full copy of this Pin
	 * 
	 * @return an OutputPin object.
	 * @exception CloneNotSupportedException
	 *                if an error occurs
	 */
	public Object clone() throws CloneNotSupportedException {
		throw new CloneNotSupportedException();
	}

}
