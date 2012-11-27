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

package org.xronos.openforge.forge.api.pin;

/**
 * ResetPin is a specific type of <code>Buffer</code> that represents a reset
 * signal input to the design. <code>ResetPin</code>s are identified by the
 * specific name used to create them.
 */
public class ResetPin extends ControlPin {
	/**
	 * Constructs a new active high ResetPin with default name RESET.
	 */
	protected ResetPin() {
		super("RESET");
	}

	/**
	 * Constructs a new active high named ResetPin.
	 * 
	 * @param name
	 */
	protected ResetPin(String name) {
		super(name);
	}
}
