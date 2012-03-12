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

package net.sf.openforge.forge.api.pin;

/**
 * ClockPin is a specific type of <code>Buffer</code> that represents a clock
 * input to the design. <code>ClockPin</code>s are identified by the specific
 * name used to create them and their speed, measured in Hz.
 */
public class ClockPin extends ControlPin {
	/** A constant to indicate an undefined clock frequency. */
	public final static long UNDEFINED_HZ = -1;

	/** The frequency of this clock pin. */
	private long Hz;

	/**
	 * Constructs a new ClockPin with default name CLK which is active high and
	 * has and undefined frequency.
	 */
	protected ClockPin() {
		super("CLK");
		setFrequency(UNDEFINED_HZ);
	}

	/**
	 * Constructs a new named ClockPin which is active high and has and
	 * undefined frequency.
	 * 
	 * @param name
	 *            , the String name of this ClockPin
	 */
	protected ClockPin(String name) {
		super(name);
		setFrequency(UNDEFINED_HZ);
	}

	/**
	 * Constructs a new named ClockPin, with specified frequency
	 * 
	 * @param name
	 *            , the String name of this ClockPin
	 * @param Hz
	 *            , the frequency of this ClockPin in Hz
	 */
	protected ClockPin(String name, long Hz) {
		super(name);
		setFrequency(Hz);
	}

	/**
	 * Set the frequency of the clock.
	 * 
	 * @param Hz
	 *            the frequency in Hz
	 */
	public void setFrequency(long Hz) {
		this.Hz = Hz;
	}

	/**
	 * Gets the specified frequency for this clock, expressed in Hz.
	 * 
	 * @return the frequency in Hz
	 */
	public long getFrequency() {
		return Hz;
	}

	/**
	 * Gets the {@link ResetPin} which is in the same domain as this clock.
	 */
	public ResetPin getResetPin() {
		return getDomain().getResetPin();
	}
}
