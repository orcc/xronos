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
 * The <code>PinIn</code> class is used to model input pins to both of IP Cores
 * and designs. The only accessor methods provided to a <code>PinIn</code> are
 * <code>get</code> methods as data may only be retrived from a
 * <code>PinIn</code>
 * 
 * @see net.sf.openforge.forge.api.pin.Buffer
 */
public final class PinIn extends Buffer {

	/**
	 * Creates a new <code>PinIn</code> instance with the given name and size.
	 * 
	 * @param name
	 *            a <code>String</code>, the name of this pin.
	 * @param size
	 *            an <code>int</code>, the bit width of this pin.
	 */
	public PinIn(String name, int size) {
		super(name, size);
	}

	/**
	 * Creates a new <code>PinIn</code> instance with the given coreObj, name,
	 * and size. The PinIn becomes an <b>output</b> of the given IPCore (think
	 * of the pin as an input to the design from the IPCore) with the specified
	 * name and size.
	 * 
	 * @param coreObj
	 *            the <code>IPCore</code> object that this pin is associated
	 *            with.
	 * @param name
	 *            a <code>String</code>, the name of the output port on the
	 *            IPCore.
	 * @param size
	 *            an <code>int</code>, the bit width of the pin.
	 */
	public PinIn(IPCore coreObj, String name, int size) {
		super(coreObj, name, size);
	}

	/**
	 * Creates a new <code>PinIn</code> with the given name and size of 32.
	 * 
	 * @param name
	 *            a <code>String</code>, the name of the pin.
	 */
	public PinIn(String name) {
		super(name, 32);
	}

	/**
	 * Creates a new <code>PinIn</code> instance with the given coreObj, name,
	 * and default size of 32. The PinIn becomes an <b>output</b> of the given
	 * IPCore (think of the pin as an input to the design from the IPCore) with
	 * the specified name and size.
	 * 
	 * @param coreObj
	 *            the <code>IPCore</code> object to which this pin is
	 *            associated.
	 * @param name
	 *            a <code>String</code>, the name of the output port on the
	 *            IPCore.
	 */
	public PinIn(IPCore coreObj, String name) {
		super(coreObj, name, 32);
	}

	/**
	 * Creates a new <code>PinIn</code> instance with the given name, size, and
	 * number of pipeline stages (registers) between the pin and the read value.
	 * 
	 * @param name
	 *            a <code>String</code>, the name of the pin.
	 * @param size
	 *            an <code>int</code>, the bit width of the pin.
	 * @param pipeDepth
	 *            an <code>int</code>, the number of register stages between the
	 *            pin and the value supplied to the design.
	 */
	public PinIn(String name, int size, int pipeDepth) {
		super(name, size);
		setInputPipelineDepth(pipeDepth);
	}

	/**
	 * Creates a new <code>PinIn</code> instance with the given coreObj, name,
	 * size, and input pipeline stages. The PinIn becomes an <b>output</b> of
	 * the given IPCore (think of the pin as an input to the design from the
	 * IPCore) with the specified name, size, and pipeline stages between the
	 * core and the value supplied to the design.
	 * 
	 * @param coreObj
	 *            the <code>IPCore</code> object that this pin is associated
	 *            with.
	 * @param name
	 *            a <code>String</code>, the name of the output of the IPCore.
	 * @param size
	 *            an <code>int</code>, the bit width of this pin
	 * @param pipeDepth
	 *            an <code>int</code>, the number of register stages between the
	 *            pin and the value supplied to the design.
	 */
	public PinIn(IPCore coreObj, String name, int size, int pipeDepth) {
		super(coreObj, name, size);
		setInputPipelineDepth(pipeDepth);
	}

	/**
	 * <code>setInputPipelineDepth</code> sets the input to have the given
	 * number of pipeline registers inserted prior to the user's access point.
	 * This is usefull when sampling asynchronous signals and synchronization
	 * flops are required.
	 * 
	 * @param depth
	 *            an <code>int</code>, the number of register stages between the
	 *            pin and the value supplied to the design.
	 */
	public void setInputPipelineDepth(int depth) {
		inputPipelineDepth = depth;
	}

	/**
	 * Retrieves the value from this <code>Buffer</code> as an <code>int</code>.
	 * This routine is identical to <code>getInt</code>, it is a shorthand
	 * method for Java's preferred primitive type.
	 * 
	 * @return an <code>int</code> that is the current value of the pin.
	 */
	public final int get() {
		return getInt();
	}

	/**
	 * Retrieves the value from this <code>Buffer</code> as an <code>int</code>.
	 * The value returned is truncated or sign extended from the
	 * <code>Buffer's</code> natural bit size to fit into an <code>int</code>.
	 * 
	 * @return an <code>int</code> that is the current value of the pin.
	 */
	public final int getInt() {
		return (int) mapValueToSize();
	}

	/**
	 * Retrieves the value from this <code>Buffer</code> as a <code>long</code>.
	 * The value returned is truncated or sign extended from the
	 * <code>Buffer's</code> natural bit size to fit into a <code>long</code>.
	 * 
	 * @return a <code>long</code> that is the current value of the pin.
	 */
	public final long getLong() {
		return mapValueToSize();
	}

	/**
	 * Retrieves the value from this <code>Buffer</code> as a
	 * <code>boolean</code>. The value returned is true if the least significant
	 * bit of the <code>Buffer</code> is a 1 and false if it is a 0.
	 * 
	 * @return a <code>boolean</code> that is the current value of the pin.
	 */
	public final boolean getBoolean() {
		return (readValue() & 1L) == 1L;
	}

	/**
	 * Retrieves the value from this <code>Buffer</code> as an <code>int</code>
	 * representing a boolean. The value return is the least significant bit of
	 * the port, either 1 or 0.
	 * 
	 * @return a <code>int</code> value
	 */
	public final int getBooleanAsInt() {
		return (int) (readValue() & 1L);
	}

	/**
	 * Retrieves the value from this <code>Buffer</code> as a <code>byte</code>.
	 * The value returned is truncated or sign extended from the
	 * <code>Buffers</code> natural bit size to fit into a <code>byte</code>.
	 * 
	 * @return a <code>byte</code> that is the current value of the pin.
	 */
	public final byte getByte() {
		return (byte) mapValueToSize();
	}

	/**
	 * Retrieves the value from this <code>Buffer</code> as a <code>short</code>
	 * . The value returned is truncated or sign extended from the
	 * <code>Buffers</code> natural bit size to fit into a <code>short</code>.
	 * 
	 * @return a <code>short</code> that is the current value of the pin.
	 */
	public final short getShort() {
		return (short) mapValueToSize();
	}

	/**
	 * Retrieves the value from this <code>Buffer</code> as a <code>char</code>.
	 * The value returned is truncated or sign extended from the
	 * <code>Buffers</code> natural bit size to fit into a <code>char</code>.
	 * 
	 * @return a <code>char</code> that is that current value of the pin.
	 */
	public final char getChar() {
		return (char) mapValueToSize();
	}

	/**
	 * Retrieves the value from this <code>Buffer</code> as an <code>int</code>.
	 * This routine is identical to <code>getUnsignedInt</code>, it is a
	 * shorthand method for Java's preferred primitive type.
	 * 
	 * @return an <code>int</code> that is the current value.
	 */
	public final int getUnsigned() {
		return getUnsignedInt();
	}

	/**
	 * Retrieves the value from this <code>Buffer</code> as an <code>int</code>.
	 * The value returned is truncated or zero extended from the
	 * <code>Buffers</code> natural bit size to fit into an <code>int</code>.
	 * 
	 * @return an <code>int</code> that is that current value of the pin.
	 */
	public final int getUnsignedInt() {
		return (int) (readValue() & getMask());
	}

	/**
	 * Retrieves the value from this <code>Buffer</code> as a <code>long</code>.
	 * The value returned is truncated or zero extended from the
	 * <code>Buffers</code> natural bit size to fit into a <code>long</code>.
	 * 
	 * @return a <code>long</code> that is that current value of the pin.
	 */
	public final long getUnsignedLong() {
		return readValue() & getMask();
	}

	/**
	 * Retrieves the value from this <code>Buffer</code> as a <code>byte</code>.
	 * The value returned is truncated or zero extended from the
	 * <code>Buffers</code> natural bit size to fit into a <code>byte</code>.
	 * 
	 * @return a <code>byte</code> that is that current value of the pin.
	 */
	public final byte getUnsignedByte() {
		return (byte) (readValue() & getMask());
	}

	/**
	 * Retrieves the value from this <code>Buffer</code> as a <code>short</code>
	 * . The value returned is truncated or zero extended from the
	 * <code>Buffers</code> natural bit size to fit into a <code>short</code>.
	 * 
	 * @return a <code>short</code> that is that current value of the pin.
	 */
	public final short getUnsignedShort() {
		return (short) (readValue() & getMask());
	}

	/**
	 * Retrieves the value from this <code>Buffer</code> as a <code>char</code>.
	 * The value returned is truncated or zero extended from the
	 * <code>Buffers</code> natural bit size to fit into a <code>char</code>.
	 * 
	 * @return a <code>char</code> that is that current value of the pin.
	 */
	public final char getUnsignedChar() {
		return (char) (readValue() & getMask());
	}

	/**
	 * Retrieves the value from this <code>Buffer</code> as a <code>float</code>
	 * without performing any type conversion, ie the value asserted to the pin
	 * must also be <code>float</code> encoded.
	 * 
	 * @return a <code>float</code>value
	 */
	public final float getFloat() {
		return readFloatValue();
	}

	/**
	 * Retrieves the value from this <code>Buffer</code> as a
	 * <code>double</code> without performing any type conversion, ie the value
	 * asserted to the pin must also be <code>double</code> encoded.
	 * 
	 * @return a <code>double</code>value
	 */
	public final double getDouble() {
		return readDoubleValue();
	}
}
