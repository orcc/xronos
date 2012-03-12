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
/* $Rev: 2 $ */

package net.sf.openforge.forge.api.pin;

import net.sf.openforge.forge.api.ipcore.IPCore;

/**
 * The <code>PinOut</code> class is used to model output pins from a design or
 * used to supply input values to an IPCore (ie an output from the design to the
 * core). Values may only be supplied to this class, and thus there are no
 * <code>get</code> methods implemented.
 * 
 * @see net.sf.openforge.forge.api.pin.Buffer
 */
public final class PinOut extends Buffer {
	/**
	 * Creates a new <code>PinOut</code> with the given name and size, where the
	 * resetValue is driven by the pin during, and subsequent to, reset.
	 * 
	 * @param name
	 *            a <code>String</code> that is the name of the pin
	 * @param size
	 *            an <code>int</code> that is the bit width of this pin
	 * @param resetValue
	 *            a <code>long</code> value that is driven on the pin subsequent
	 *            to reset
	 */
	public PinOut(String name, int size, long resetValue) {
		super(name, size, resetValue);
	}

	/**
	 * Creates a new <code>PinOut</code> that is used to supply values to the
	 * given IPCore object. The name of the pin will be the name of the input
	 * port on the IP Core and will have the specified size. After reset the pin
	 * will drive the given reset value to the core until changed.
	 * 
	 * @param coreObj
	 *            a <code>IPCore</code> object that this pin is associated with
	 * @param name
	 *            a <code>String</code> that is the name of the pin
	 * @param size
	 *            an <code>int</code> that is the bit width of this pin
	 * @param resetValue
	 *            a <code>long</code> value that is driven on the pin subsequent
	 *            to reset
	 */
	public PinOut(IPCore coreObj, String name, int size, long resetValue) {
		super(coreObj, name, size, resetValue);
	}

	/**
	 * Creates a new <code>PinOut</code> with the given name and size and a
	 * default reset value of 0.
	 * 
	 * @param name
	 *            a <code>String</code> that is the name of the pin
	 * @param size
	 *            an <code>int</code> that is the bit width of this pin
	 */
	public PinOut(String name, int size) {
		super(name, size, 0, true, false);
	}

	/**
	 * Creates a new <code>PinOut</code> that is used to supply values to the
	 * given IPCore object. The name of the pin will be the name of the input
	 * port on the IP Core and will have the specified size. After reset the pin
	 * will have the default value of 0.
	 * 
	 * @param coreObj
	 *            a <code>IPCore</code> object that this pin is associated with
	 * @param name
	 *            a <code>String</code> that is the name of the pin
	 * @param size
	 *            an <code>int</code> that is the bit width of this pin
	 */
	public PinOut(IPCore coreObj, String name, int size) {
		super(coreObj, name, size, 0, true, false);
	}

	/**
	 * Creates a new <code>PinOut</code> with size of 32 bits and that comes out
	 * of reset driving the default value of 0.
	 * 
	 * @param name
	 *            a <code>String</code> that is the name of the pin
	 */
	public PinOut(String name) {
		super(name, 32, 0, true, false);
	}

	/**
	 * Creates a new <code>PinOut</code> that is used to supply values to the
	 * given IPCore object. The name of the pin will be the name of the input
	 * port on the IP Core and will be 32 bits wide. After reset the pin will be
	 * in the high-impedence state.
	 * 
	 * @param coreObj
	 *            a <code>IPCore</code> object that this pin is associated with
	 * @param name
	 *            a <code>String</code> that is the name of the pin
	 */
	public PinOut(IPCore coreObj, String name) {
		super(coreObj, name, 32, 0, true, false);
	}

	/**
	 * Assigns a value to this <code>Buffer</code> within the current clock
	 * cycle.
	 * 
	 * @param value
	 *            a <code>long</code> that is sent to the pins output buffer.
	 */
	public final void setNow(long value) {
		writeValueNow(mapValueToSize(value));
	}

	/**
	 * Assigns a value to this <code>Buffer</code> after the next active clock
	 * edge.
	 * 
	 * @param value
	 *            a <code>long</code> that is sent to the pins output buffer.
	 */
	public final void setNext(long value) {
		writeValueNext(mapValueToSize(value));
	}

	/**
	 * Assigns a <code>float</code> value to this <code>Buffer</code> within the
	 * current clock.
	 * 
	 * @param value
	 *            a <code>float</code> that is sent to the pins output buffer.
	 */
	public final void setNow(float value) {
		writeFloatValueNow(value);
	}

	/**
	 * Assigns a <code>float</code> value to this <code>Buffer</code> after the
	 * next active clock edge.
	 * 
	 * @param value
	 *            a <code>float</code> that is sent to the pins output buffer.
	 */
	public final void setNext(float value) {
		writeFloatValueNext(value);
	}

	/**
	 * Assigns a <code>double</code> value to this <code>Buffer</code> within
	 * the current clock
	 * 
	 * @param value
	 *            a <code>double</code> that is sent to the pins output buffer.
	 */
	public final void setNow(double value) {
		writeDoubleValueNow(value);
	}

	/**
	 * Assigns a <code>double</code> value to this <code>Buffer</code> after the
	 * next active clock edge.
	 * 
	 * @param value
	 *            a <code>double</code> that is sent to the pins output buffer.
	 */
	public final void setNext(double value) {
		writeDoubleValueNext(value);
	}

}
