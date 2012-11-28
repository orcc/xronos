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
package org.xronos.openforge.lim;

/**
 * A PinBuf is a {@link Resource} provided by a {@link Pin}. It is the entity
 * that actually allows the reading or writing of the Pin.
 * 
 * @author Stephen Edwards
 * @version $Id: PinBuf.java 538 2007-11-21 06:22:39Z imiller $
 */
public abstract class PinBuf extends Resource {

	private Pin pin;

	public Pin getPin() {
		return pin;
	}

	protected PinBuf(Pin pin) {
		super();
		this.pin = pin;
	}

	public abstract boolean consumesClock();

	public abstract boolean consumesReset();

	@Override
	public int getSpacing(Referencer from, Referencer to) {
		throw new UnsupportedOperationException(
				"PinBuf is obsolete, unexpected call to getSpacing");
	}

	/**
	 * Returns -1 indicating that the referencers must be scheduled using the
	 * default DONE to GO spacing.
	 */
	@Override
	public int getGoSpacing(Referencer from, Referencer to) {
		return -1;
	}

}
