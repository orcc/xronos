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

package org.xronos.openforge.forge.api.pin;

import org.xronos.openforge.forge.api.ipcore.IPCore;

/**
 * The <code>PinOutTS</code> class is used to model output ports from a design
 * or used to supply input values to an IPCore (ie an output from the design to
 * the core). Values may only be supplied to this class, and thus there are no
 * <code>get</code> methods implemented. Pins of this class may be tri-stated,
 * forcing them into the high-impedence state.
 * 
 * @see org.xronos.openforge.forge.api.pin.Buffer
 */
public final class PinOutTS extends Buffer {
	/**
	 * Creates a new <code>PinOutTS</code> with the given name and bit width
	 * that will drive the specified resetValue subsequent to reset.
	 * 
	 * @param name
	 *            a <code>String</code> that is the name of the pin
	 * @param size
	 *            an <code>int</code> that is the bit width of this pin
	 * @param resetValue
	 *            a <code>long</code> value that is driven on the pin subsequent
	 *            to reset
	 */
	public PinOutTS(String name, int size, long resetValue) {
		super(name, size, resetValue);
	}

	/**
	 * Creates a new <code>PinOutTS</code> that is used to supply values to an
	 * IPCore. The name of this pin becomes the name of the IPCore input port
	 * and the width of the pin is the width of the core port. Subsequent to
	 * reset the pin will be driving the supplied resetValue.
	 * 
	 * @param coreObj
	 *            an <code>IPCore</code> object that this pin is associated with
	 * @param name
	 *            a <code>String</code> that is the name of the pin
	 * @param size
	 *            an <code>int</code> that is the bit width of this pin
	 * @param resetValue
	 *            a <code>long</code> value that is driven on the pin subsequent
	 *            to reset
	 */
	public PinOutTS(IPCore coreObj, String name, int size, long resetValue) {
		super(coreObj, name, size, resetValue);
	}

	/**
	 * Creates a new <code>PinOutTS</code> with the specified pin name and width
	 * that will come out of reset in the high-impedence state.
	 * 
	 * @param name
	 *            a <code>String</code> that is the name of the pin
	 * @param size
	 *            an <code>int</code> that is the bit width of this pin
	 */
	public PinOutTS(String name, int size) {
		super(name, size, 0, false, false);
	}

	/**
	 * Creates a new <code>PinOutTS</code> that is used to supply values to the
	 * given IPCore object. The name of this pin becomes the name of the IPCore
	 * input port and the width of the pin is the width of the core port.
	 * Subsequent to reset the pin will be in the high-impedence state.
	 * 
	 * @param coreObj
	 *            an <code>IPCore</code> object that this pin is associated with
	 * @param name
	 *            a <code>String</code> that is the name of the pin
	 * @param size
	 *            an <code>int</code> that is the bit width of this pin
	 */
	public PinOutTS(IPCore coreObj, String name, int size) {
		super(coreObj, name, size, 0, false, false);
	}

	/**
	 * Creates a new <code>PinOutTS</code> with size 32 and the given name that
	 * is in the high-impedence state subsequent to reset.
	 * 
	 * @param name
	 *            a <code>String</code> that is the name of the pin
	 */
	public PinOutTS(String name) {
		super(name, 32, 0, false, false);
	}

	/**
	 * Creates a new <code>PinOutTS</code> that is used in supplying values to
	 * the given IPCore. The name of the pin becomes the name of the input port
	 * of the IPCore, the width of the port will be 32 bits, and the pin will be
	 * in the high-impedence state subsequent to reset.
	 * 
	 * @param coreObj
	 *            an <code>IPCore</code> object that this pin is associated with
	 * @param name
	 *            a <code>String</code> that is the name of the pin
	 */
	public PinOutTS(IPCore coreObj, String name) {
		super(coreObj, name, 32, 0, false, false);
	}

	/**
	 * Assigns a value to this <code>Buffer</code> within the current clock
	 * cycle without changing the drive/release state. If this
	 * <code>Buffer</code> is currently not driven (in a <code>release</code>
	 * state), then the value doesn't make it to the output of the
	 * <code>Buffer</code> and is effectively lost if the buffer is not turned
	 * on via a call to <code>driveNow</code> during this same clock cycle.
	 * 
	 * @param value
	 *            a <code>long</code> that is sent to the pins output buffer.
	 */
	public final void setNow(long value) {
		writeValueNow(mapValueToSize(value));
	}

	/**
	 * Assigns a value to this <code>Buffer</code> within the current clock and
	 * turns on the tri-state driver.
	 * 
	 * @param value
	 *            a <code>long</code> that is sent to the pins output buffer.
	 */
	public final void assertNow(long value) {
		setNow(value);
		driveNow();
	}

	/**
	 * Assigns a value to this <code>Buffer</code> after the next active clock
	 * edge without changing the drive/release state. If this
	 * <code>Buffer</code> is currently not driven (in a <code>release</code>
	 * state), then the value doesn't make it to the output of the
	 * <code>Buffer</code>, but will be the value driven once <code>drive</code>
	 * is called on this <code>Buffer</code>.
	 * 
	 * @param value
	 *            a <code>long</code> that is sent to the pins output buffer.
	 */
	public final void setNext(long value) {
		writeValueNext(mapValueToSize(value));
	}

	/**
	 * Assigns a value to this <code>Buffer</code> after the next active clock
	 * edge and changes the drive/release state to true. If this
	 * <code>Buffer</code> is currently not driven (in a <code>release</code>
	 * state), then the tri-state driver will be turned on during the next
	 * cycle.
	 * 
	 * @param value
	 *            a <code>long</code> that is sent to the pins output buffer.
	 */
	public final void assertNext(long value) {
		setNext(value);
		driveNext();
	}

	/**
	 * Assigns a <code>float</code> value to this <code>Buffer</code> within the
	 * current clock without changing the drive/release state.
	 * 
	 * @param value
	 *            a <code>float</code> that is sent to the pins output buffer.
	 */
	public final void setNow(float value) {
		writeFloatValueNow(value);
	}

	/**
	 * Assigns a <code>float</code> value to this <code>Buffer</code> within the
	 * current clock and turns on the tri-state driver.
	 * 
	 * @param value
	 *            a <code>float</code> that is sent to the pins output buffer.
	 */
	public final void assertNow(float value) {
		setNow(value);
		driveNow();
	}

	/**
	 * Assigns a <code>float</code> value to this <code>Buffer</code> after the
	 * next active clock edge without changing the drive/release state. If this
	 * <code>Buffer</code> is currently not driven (in a <code>release</code>
	 * state), then the value doesn't make it to the output of the
	 * <code>Buffer</code>, but will be the value driven once <code>drive</code>
	 * is called on this <code>Buffer</code>.
	 * 
	 * @param value
	 *            a <code>float</code> that is sent to the pins output buffer.
	 */
	public final void setNext(float value) {
		writeFloatValueNext(value);
	}

	/**
	 * Assigns a <code>float</code> value to this <code>Buffer</code> after the
	 * next active clock edge and changes the drive/release state to true. If
	 * this <code>Buffer</code> is currently not driven (in a
	 * <code>release</code> state), then the tri-state driver will be turned on
	 * during the next cycle.
	 * 
	 * @param value
	 *            a <code>float</code> that is sent to the pins output buffer.
	 */
	public final void assertNext(float value) {
		setNext(value);
		driveNext();
	}

	/**
	 * Assigns a <code>double</code> value to this <code>Buffer</code> within
	 * the current clock without changing the drive/release state.
	 * 
	 * @param value
	 *            a <code>double</code> that is sent to the pins output buffer.
	 */
	public final void setNow(double value) {
		writeDoubleValueNow(value);
	}

	/**
	 * Assigns a <code>double</code> value to this <code>Buffer</code> within
	 * the current clock and turns on the tri-state driver.
	 * 
	 * @param value
	 *            a <code>double</code> that is sent to the pins output buffer.
	 */
	public final void assertNow(double value) {
		setNow(value);
		driveNow();
	}

	/**
	 * Assigns a <code>double</code> value to this <code>Buffer</code> after the
	 * next active clock edge without changing the drive/release state. If this
	 * <code>Buffer</code> is currently not driven (in a <code>release</code>
	 * state), then the value doesn't make it to the output of the
	 * <code>Buffer</code>, but will be the value driven once <code>drive</code>
	 * is called on this <code>Buffer</code>.
	 * 
	 * @param value
	 *            a <code>double</code> that is sent to the pins output buffer.
	 */
	public final void setNext(double value) {
		writeDoubleValueNext(value);
	}

	/**
	 * Assigns a <code>double</code> value to this <code>Buffer</code> after the
	 * next active clock edge and changes the drive/release state to true. If
	 * this <code>Buffer</code> is currently not driven (in a
	 * <code>release</code> state), then the tri-state driver will be turned on
	 * during the next cycle.
	 * 
	 * @param value
	 *            a <code>double</code> that is sent to the pins output buffer.
	 */
	public final void assertNext(double value) {
		setNext(value);
		driveNext();
	}

	/**
	 * Informs this <code>Buffer</code> to stop actively driving during the
	 * current clock cycle. The hardware response is to stop driving and allow
	 * the port to float to a value of high impedance (Z). If the
	 * <code>Buffer</code> was already in an undriven state then this routine
	 * has no effect. If this <code>Buffer</code> doesn't support three stating,
	 * this call is ignored and will generate a warning message.
	 * 
	 */
	public void releaseNow() {
		changeDriveNow(false);
	}

	/**
	 * Informs this <code>Buffer</code> to stop actively driving at the after
	 * next clock edge. The hardware response is to stop driving and allow the
	 * port to float to a value of high impedance (Z). If the
	 * <code>Buffer</code> was already in an undriven state then this routine
	 * has no effect. If this <code>Buffer</code> doesn't support three stating,
	 * this call is ignored and will generate a warning message.
	 * 
	 */
	public void releaseNext() {
		changeDriveNext(false);
	}

	/**
	 * Informs this <code>Buffer</code> to actively drive the last value
	 * assigned to it at the during the current clock. This method allows the
	 * user to separate the functions of assigning a value to a
	 * <code>Buffer</code> from controlling the three state enable for the
	 * <code>Buffer</code>.
	 * 
	 */
	public void driveNow() {
		changeDriveNow(true);
	}

	/**
	 * Informs this <code>Buffer</code> to actively drive the last value
	 * assigned to it at the next clock edge. This method allows the user to
	 * separate the functions of assigning a value to a <code>Buffer</code> from
	 * controlling the three state enable for the <code>Buffer</code>.
	 * 
	 */
	public void driveNext() {
		changeDriveNext(true);
	}

}
