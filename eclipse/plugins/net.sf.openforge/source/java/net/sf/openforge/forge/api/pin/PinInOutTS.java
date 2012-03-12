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
 * The <code>PinInOutTS</code> class is used to model combined input/output pins
 * in a design or a IP Core object. All operations on this buffer are supported.
 * 
 * @see net.sf.openforge.forge.api.pin.Buffer
 */
public final class PinInOutTS extends Buffer {
	/**
	 * Creates a new <code>PinInOutTS</code> with the given name and size that
	 * drives its resetValue during and subsequent to reset.
	 * 
	 * @param name
	 *            a <code>String</code>, the pin name
	 * @param size
	 *            an <code>int</code>, the bit width of the pin
	 * @param resetValue
	 *            a <code>long</code>, the value driven following reset.
	 */
	public PinInOutTS(String name, int size, long resetValue) {
		super(name, size, resetValue);
	}

	/**
	 * Creates a new <code>PinInOutTS</code> that is used to interact with the
	 * specified IPCore. The creation of this pin causes a bidirectional port to
	 * be created on the IPCore with the given name and size. During and
	 * subsequent to reset the pin drives the specified value (effectively
	 * supplying the reset value to the design).
	 * 
	 * @param coreObj
	 *            the <code>IPCore</code> object that this pin is associated
	 *            with.
	 * @param name
	 *            a <code>String</code>, the name of this pin
	 * @param size
	 *            an <code>int</code> value
	 * @param resetValue
	 *            a <code>long</code> value
	 */
	public PinInOutTS(IPCore coreObj, String name, int size, long resetValue) {
		super(coreObj, name, size, resetValue);
	}

	/**
	 * Creates a new <code>PinInOutTS</code> with the given name that drives the
	 * resetValue during, and subsequent to, reset and has the given input
	 * pipeline depth.
	 * 
	 * @param name
	 *            a <code>String</code>, the name of this pin
	 * @param size
	 *            an <code>int</code>, the bit width of this pin
	 * @param resetValue
	 *            a <code>long</code> value
	 * @param pipeDepth
	 *            an <code>int</code> value
	 */
	public PinInOutTS(String name, int size, long resetValue, int pipeDepth) {
		super(name, size, resetValue);
		setInputPipelineDepth(pipeDepth);
	}

	/**
	 * Creates a new <code>PinInOutTS</code> instance that drives the resetValue
	 * during reset and has the given input pipeline depth.
	 * 
	 * @param name
	 *            a <code>String</code>, the name of this pin
	 * @param size
	 *            an <code>int</code>, the bit width of this pin
	 * @param resetValue
	 *            a <code>long</code> value, the value driven by the pin after
	 *            reset.
	 * @param pipeDepth
	 *            an <code>int</code>, the number of register stages between the
	 *            pin and the value supplied to the design.
	 */
	public PinInOutTS(IPCore coreObj, String name, int size, long resetValue,
			int pipeDepth) {
		super(coreObj, name, size, resetValue);
		setInputPipelineDepth(pipeDepth);
	}

	/**
	 * Creates a new <code>PinInOutTS</code> instance with the given size that
	 * comes out of reset in the high-impedence state.
	 * 
	 * @param name
	 *            a <code>String</code>, the name of this pin
	 * @param size
	 *            an <code>int</code>, the bit width of this pin
	 */
	public PinInOutTS(String name, int size) {
		super(name, size);
	}

	/**
	 * Creates a new <code>PinInOutTS</code> that is used to interact with an
	 * IPCore. The pin will create a bidirectional port on the IPCore with the
	 * given name and will be in the high-impedence state after reset.
	 * 
	 * @param coreObj
	 *            the <code>IPCore</code> object that this pin is associated
	 *            with.
	 * @param name
	 *            a <code>String</code>, the name of this pin
	 * @param size
	 *            an <code>int</code>, the bit width of this pin
	 */
	public PinInOutTS(IPCore coreObj, String name, int size) {
		super(coreObj, name, size);
	}

	/**
	 * Creates a new <code>PinInOutTS</code> that is in the high-impedence state
	 * after reset and has a bit width of 32 bits.
	 * 
	 * @param name
	 *            a <code>String</code>, the name of this pin
	 */
	public PinInOutTS(String name) {
		super(name, 32);
	}

	/**
	 * Creates a new <code>PinInOutTS</code> that is used to interact with the
	 * given IPCore that is in the high-impedence state after reset and has a
	 * default bit width of 32 bits.
	 * 
	 * @param coreObj
	 *            the <code>IPCore</code> object that this pin is associated
	 *            with.
	 * @param name
	 *            a <code>String</code>, the name of this pin
	 */
	public PinInOutTS(IPCore coreObj, String name) {
		super(coreObj, name, 32);
	}

	/**
	 * <code>setInputPipelineDepth</code> sets the input to have the given
	 * number of pipeline registers inserted prior to the user's access point.
	 * This is usefull when sampling asynchronous signals and synchronization
	 * flops are required.
	 * 
	 * @param depth
	 *            an <code>int</code> value
	 */
	public void setInputPipelineDepth(int depth) {
		inputPipelineDepth = depth;
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
	 * Retrieves the value from this <code>Buffer</code> as an <code>int</code>.
	 * This routine is identical to <code>getInt</code>, it is a shorthand
	 * method for Java's preferred primitive type.
	 * 
	 * @return an <code>int</code> that is the current value of the pin.
	 */
	public final int get() {
		return (getInt());
	}

	/**
	 * Retrieves the value from this <code>Buffer</code> as an <code>int</code>.
	 * The value returned is truncated or sign extended from the
	 * <code>Buffers</code> natural bit size to fit into an <code>int</code>.
	 * 
	 * @return an <code>int</code> that is the current value of the pin
	 */
	public final int getInt() {
		return ((int) mapValueToSize());
	}

	/**
	 * Retrieves the value from this <code>Buffer</code> as a <code>long</code>.
	 * The value returned is truncated or sign extended from the
	 * <code>Buffers</code> natural bit size to fit into a <code>long</code>.
	 * 
	 * @return a <code>long</code> that is the current value of the pin
	 */
	public final long getLong() {
		return (mapValueToSize());
	}

	/**
	 * Retrieves the value from this <code>Buffer</code> as a
	 * <code>boolean</code>. The value returned is true if the least significant
	 * bit of the <code>Buffer</code> is a 1 and false if it is a 0.
	 * 
	 * @return a <code>boolean</code> that is the current value of the pin
	 */
	public final boolean getBoolean() {
		return ((readValue() & 1L) == 1L);
	}

	/**
	 * Retrieves the value from this <code>Buffer</code> as an <code>int</code>
	 * representing a boolean. The value return is the least significant bit of
	 * the port, either 1 or 0.
	 * 
	 * @return a <code>int</code> that is the current value of the pin
	 */
	public final int getBooleanAsInt() {
		return ((int) (readValue() & 1L));
	}

	/**
	 * Retrieves the value from this <code>Buffer</code> as a <code>byte</code>.
	 * The value returned is truncated or sign extended from the
	 * <code>Buffers</code> natural bit size to fit into a <code>byte</code>.
	 * 
	 * @return a <code>byte</code> that is the current value of the pin
	 */
	public final byte getByte() {
		return ((byte) mapValueToSize());
	}

	/**
	 * Retrieves the value from this <code>Buffer</code> as a <code>short</code>
	 * . The value returned is truncated or sign extended from the
	 * <code>Buffers</code> natural bit size to fit into a <code>short</code>.
	 * 
	 * @return a <code>short</code> that is the current value of the pin
	 */
	public final short getShort() {
		return ((short) mapValueToSize());
	}

	/**
	 * Retrieves the value from this <code>Buffer</code> as a <code>char</code>.
	 * The value returned is truncated or sign extended from the
	 * <code>Buffers</code> natural bit size to fit into a <code>char</code>.
	 * 
	 * @return a <code>char</code> that is the current value of the pin
	 */
	public final char getChar() {
		return ((char) mapValueToSize());
	}

	/**
	 * Retrieves the value from this <code>Buffer</code> as an <code>int</code>.
	 * This routine is identical to <code>getUnsignedInt</code>, it is a
	 * shorthand method for Java's preferred primitive type.
	 * 
	 * @return an <code>int</code> that is the current value of the pin.
	 */
	public final int getUnsigned() {
		return (getUnsignedInt());
	}

	/**
	 * Retrieves the value from this <code>Buffer</code> as an <code>int</code>.
	 * The value returned is truncated or zero extended from the
	 * <code>Buffers</code> natural bit size to fit into an <code>int</code>.
	 * 
	 * @return an <code>int</code> that is the current value of the pin
	 */
	public final int getUnsignedInt() {
		return ((int) (readValue() & getMask()));
	}

	/**
	 * Retrieves the value from this <code>Buffer</code> as a <code>long</code>.
	 * The value returned is truncated or zero extended from the
	 * <code>Buffers</code> natural bit size to fit into a <code>long</code>.
	 * 
	 * @return a <code>long</code> that is the current value of the pin
	 */
	public final long getUnsignedLong() {
		return (readValue() & getMask());
	}

	/**
	 * Retrieves the value from this <code>Buffer</code> as a <code>byte</code>.
	 * The value returned is truncated or zero extended from the
	 * <code>Buffers</code> natural bit size to fit into a <code>byte</code>.
	 * 
	 * @return a <code>byte</code> that is the current value of the pin
	 */
	public final byte getUnsignedByte() {
		return ((byte) (readValue() & getMask()));
	}

	/**
	 * Retrieves the value from this <code>Buffer</code> as a <code>short</code>
	 * . The value returned is truncated or zero extended from the
	 * <code>Buffers</code> natural bit size to fit into a <code>short</code>.
	 * 
	 * @return a <code>short</code> that is the current value of the pin
	 */
	public final short getUnsignedShort() {
		return ((short) (readValue() & getMask()));
	}

	/**
	 * Retrieves the value from this <code>Buffer</code> as a <code>char</code>.
	 * The value returned is truncated or zero extended from the
	 * <code>Buffers</code> natural bit size to fit into a <code>char</code>.
	 * 
	 * @return a <code>char</code> that is the current value of the pin
	 */
	public final char getUnsignedChar() {
		return ((char) (readValue() & getMask()));
	}

	/**
	 * Retrieves the value from this <code>Buffer</code> as a <code>float</code>
	 * without performing any type conversion, ie the value asserted to the pin
	 * must also be <code>float</code> encoded.
	 * 
	 * @return a <code>float</code> that is the current value of the pin
	 */
	public final float getFloat() {
		return readFloatValue();
	}

	/**
	 * Retrieves the value from this <code>Buffer</code> as a
	 * <code>double</code> without performing any type conversion, ie the value
	 * asserted to the pin must also be <code>double</code> encoded.
	 * 
	 * @return a <code>double</code> that is the current value of the pin
	 */
	public final double getDouble() {
		return readDoubleValue();
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
