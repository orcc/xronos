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

import java.util.List;

import org.xronos.openforge.forge.api.ForgeApiException;
import org.xronos.openforge.forge.api.ucf.UCFAttribute;


/**
 * A <code>RestrictedPin</code> provides restricted access to another Buffer. It
 * does not support any read or write operations, but does allow the
 * manipulation of pin attributes (UCF, size, name, etc.).
 */
public class RestrictedPin {
	/**
	 * The original pin to which this RestrictedPin provides restriced access.
	 */
	Buffer original;

	/**
	 * Creates a new <code>RestrictedPin</code> for an existing design pin.
	 */
	public RestrictedPin(Buffer original) {
		this.original = original;
	}

	/**
	 * Adds the specified {@link UCFAttribute} to this RestrictedPin. (Defers to
	 * the original pin)
	 * 
	 * @param ucf
	 *            the {@link UCFAttribute} to add
	 */
	public void addUCFAttribute(UCFAttribute ucf) {
		original.addUCFAttribute(ucf);
	}

	/**
	 * Gets the List of {@link UCFAttribute UCFAttributes} for this
	 * RestrictedPin. (Defers to the original pin)
	 */
	public List<UCFAttribute> getUCFAttributes() {
		return original.getUCFAttributes();
	}

	/**
	 * Returns the bit width of this <code>RestrictedPin</code>. (Defers to the
	 * original pin)
	 * 
	 * @return an <code>int</code> value, always greater than 0.
	 */
	public int getSize() {
		return original.getSize();
	}

	/**
	 * Returns whether this RestrictedPin uses signed values. (Defers to the
	 * original pin)
	 * 
	 * @return true for signed values, false for unsigned
	 */
	public boolean isSigned() {
		return original.isSigned();
	}

	/**
	 * Sets whether this RestrictedPin uses signed values. (Defers to the
	 * original pin)
	 * 
	 * @param isSigned
	 *            true for signed, false for unsigned
	 */
	public void setSigned(boolean isSigned) {
		original.setSigned(isSigned);
	}

	/**
	 * <code>getInputPipelineDepth</code> returns the current input pipeline
	 * depth setting. (Defers to the original pin)
	 * 
	 * @return a non-negative <code>int</code> value
	 */
	public int getInputPipelineDepth() {
		return original.getInputPipelineDepth();
	}

	/**
	 * Returns a <code>String</code> representation of this
	 * <code>RestrictedPin</code>. (Defers to the original pin)
	 * 
	 * @return a <code>String</code> value
	 */
	@Override
	public String toString() {
		return original.toString();
	}

	/**
	 * Returns the name of this <code>RestrictedPin</code>. (Defers to the
	 * original pin)
	 * 
	 * @return a <code>String</code> value
	 */
	public String getName() {
		return original.getName();
	}

	/**
	 * Sets the name of this <code>RestrictedPin</code>. (Defers to the original
	 * pin)
	 * 
	 * @param name
	 *            a <code>String</code> value
	 */
	public void setName(String name) {
		original.setName(name);
	}

	/**
	 * Sets the bit width of this <code>RestrictedPin</code> (Defers to the
	 * original pin)
	 * 
	 * @param size
	 *            a value of type 'int'
	 */
	public void setSize(int size) {
		original.setSize(size);
	}

	/**
	 * Returns the name of this <code>RestrictedPin</code> with vector size
	 * notation appended. (Defers to the original pin)
	 * 
	 * @return a <code>String</code> value
	 */
	protected String getFullName() {
		return original.getFullName();
	}

	/**
	 * Returns the defined Reset Value of this <code>RestrictedPin</code>.
	 * (Defers to the original pin)
	 * 
	 * @return an <code>int</code> value
	 */
	public long getResetValue() {
		return original.getResetValue();
	}

	/**
	 * Identifies the reset behavior of this RestrictedPin. The RestrictedPin
	 * will behave in one of two ways depending on how it is constructed.
	 * <ul>
	 * <li>If this method returns true, the RestrictedPin will come out of Reset
	 * in the active state. The output buffer will not be tri-stated and the
	 * specified reset value will be driven on the port until changed via a call
	 * to a <code>set</code> method, or until this RestrictedPin is tri-stated
	 * via a call to a <code>release</code> method on this RestrictedPin.
	 * <li>If this method returns false, the RestrictedPin will come out of
	 * Reset in the high impedence (tri-state) state until the first call to a
	 * <code>drive</code> method on this RestrictedPin.
	 * </ul>
	 * (Defers to the original pin)
	 * 
	 * @return true if this RestrictedPin is to drive its value during and
	 *         subsequent to Reset, or false if the RestrictedPin is tri-stated.
	 */
	public boolean getDriveOnReset() {
		return original.getDriveOnReset();
	}

	/**
	 * Returns if this <code>RestrictedPin</code> had a reset value supplied by
	 * the user during construction. (Defers to the original pin)
	 * 
	 * @return a <code>boolean</code> value
	 */
	public boolean getResetValueSupplied() {
		return original.getResetValueSupplied();
	}

	/**
	 * Sets the {@link ClockDomain} with which this is associated. (Defers to
	 * the original pin)
	 * 
	 * @param domain
	 *            the associated clock domain
	 */
	public void setDomain(ClockDomain domain) {
		original.setDomain(domain);
	}

	/**
	 * Gets the {@link ClockDomain} with which this is associated. (Defers to
	 * the original pin)
	 * 
	 * @return the associated clock domain
	 */
	public ClockDomain getDomain() {
		return original.getDomain();
	}

	/**
	 * Maps the supplied value to a sign extended version that fits into the bit
	 * width of this <code>RestrictedPin</code>.
	 * <P>
	 * Restricted. Throws a runtime exception.
	 * 
	 * @param dat
	 *            an <code>int</code> value
	 * @return an <code>int</code> value
	 */
	protected final long mapValueToSize(long dat) {
		throw new ForgeApiException("RestrictedPin doesn't support any access.");
	}

	// Primitive method calls used by sub-classes to interact with the
	// RestrictedPin.

	protected void writeValueNow(long value) {
		throw new ForgeApiException("RestrictedPin doesn't support any access.");
	}

	protected void writeValueNext(long value) {
		throw new ForgeApiException("RestrictedPin doesn't support any access.");
	}

	protected long readValue() {
		throw new ForgeApiException("RestrictedPin doesn't support any access.");
	}

	protected void changeDriveNow(boolean enable) {
		throw new ForgeApiException("RestrictedPin doesn't support any access.");
	}

	protected void changeDriveNext(boolean enable) {
		throw new ForgeApiException("RestrictedPin doesn't support any access.");
	}

	protected void writeFloatValueNow(float value) {
		throw new ForgeApiException("RestrictedPin doesn't support any access.");
	}

	protected void writeFloatValueNext(float value) {
		throw new ForgeApiException("RestrictedPin doesn't support any access.");
	}

	protected float readFloatValue() {
		throw new ForgeApiException("RestrictedPin doesn't support any access.");
	}

	protected void writeDoubleValueNow(double value) {
		throw new ForgeApiException("RestrictedPin doesn't support any access.");
	}

	protected void writeDoubleValueNext(double value) {
		throw new ForgeApiException("RestrictedPin doesn't support any access.");
	}

	protected double readDoubleValue() {
		throw new ForgeApiException("RestrictedPin doesn't support any access.");
	}

}
