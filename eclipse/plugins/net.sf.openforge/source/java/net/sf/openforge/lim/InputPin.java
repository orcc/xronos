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
 * A pin from which the {@link Design} may read external data.
 * 
 * @author Stephen Edwards
 * @version $Id: InputPin.java 2 2005-06-09 20:00:48Z imiller $
 */
public class InputPin extends Pin {
	private static final String rcs_id = "RCS_REVISION: $Rev: 2 $";

	private InPinBuf buf;
	private Bus bus;

	/** True iff this pin was derived implicitly from a Port */
	private boolean isInferred = false;

	/**
	 * Construct a new InputPin based on a Port Note: the Port is expected to
	 * have a {@link Value} which provides the sizing information.
	 * 
	 * @param port
	 *            the Port upon which to base the InputPin
	 */
	public InputPin(Port port) {
		this(port.getValue().getSize(), port.getValue().isSigned());
		this.isInferred = true;
	}

	/**
	 * Constructs a new InputPin with explicit width and signedness.
	 * 
	 * @param width
	 */
	public InputPin(int width, boolean isSigned) {
		this(width, width, isSigned);
	}

	/**
	 * Constructs a new InputPin. The pin may have a different width than the
	 * it's bus, though the signedness must match. Optimization may have
	 * downsized the data path leading from the Pin, but the pin may have an
	 * explicit width requirement, even if all the bits aren't used internally.
	 * 
	 * @param pinWidth
	 * @param busWidth
	 * @param isSigned
	 */
	private InputPin(int pinWidth, int busWidth, boolean isSigned) {
		super(Math.min(pinWidth, busWidth), isSigned);
		this.buf = new InPinBuf(this);
		Exit exit = makeExit(0);
		this.bus = (Bus) exit.makeDataBus();
		bus.setSize(pinWidth, isSigned);
	}

	/**
	 * Tests whether this pin was inferred or created explicitly by the user.
	 * 
	 * @return true if this pin was created with {@link InputPin#InputPin(Port)}
	 *         , false otherwise
	 */
	public boolean isInferred() {
		return isInferred;
	}

	public InPinBuf getInPinBuf() {
		return buf;
	}

	public Collection getPinBufs() {
		return Collections.singleton(getInPinBuf());
	}

	public Bus getBus() {
		return bus;
	}

	public int getWidth() {
		// Because the width is the default size when this pin is
		// created , and it never got changed after the constant
		// propagation. On the other hand, the translator uses the
		// bus's compacted value, and the testBench uses the default
		// size. In order to let testBenchWriter generate the
		// size-matching test vectors, we choose the minimum of
		// default size and compacted value size.
		if (getBus() != null && getBus().getValue() != null) {
			return Math.min(super.getWidth(), getBus().getValue().getSize());
		} else {
			return super.getWidth();
		}
	}

	/**
	 * Augments the default behavior by also forcing the logical ID of the
	 * InputPin's Bus to match. If you change this method make sure that you are
	 * somehow still enforcing the names to be the same, or making it so that
	 * they no longer have to be the same.
	 * 
	 * @param s
	 *            the new logical ID
	 */
	public void setIDLogical(String s) {
		super.setIDLogical(s);
		if (getBus() != null) {
			getBus().setIDLogical(s);
		}
	}

	/**
	 * Calls the super, then removes any reference to the given bus in this
	 * class.
	 */
	public boolean removeDataBus(Bus bus) {
		if (super.removeDataBus(bus)) {
			if (bus == this.bus)
				this.bus = null;
			return true;
		}
		return false;
	}

	/*
	 * =================================================== Begin new constant
	 * prop rules implementation.
	 */

	/**
	 * Pushes the size of the pin onto the Bus as all care bits.
	 */
	public boolean pushValuesForward() {
		return false;
	}

	public boolean pushValuesBackward() {
		return false;
	}

	/*
	 * End new constant prop rules implementation.
	 * =================================================
	 */

	/**
	 * Returns a full copy of this Pin
	 * 
	 * @return an InputPin object.
	 * @exception CloneNotSupportedException
	 *                if an error occurs
	 */
	public Object clone() throws CloneNotSupportedException {
		throw new CloneNotSupportedException();
	}

}
