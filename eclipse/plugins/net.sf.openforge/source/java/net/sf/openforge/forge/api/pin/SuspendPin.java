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
 * SuspendPin
 */
public class SuspendPin extends ControlPin {
	/**
	 * The default suspend signal used whenever a suspend is needed but not
	 * explicitly provided.
	 */
	public final static SuspendPin GLOBAL = new SuspendPin("SUSPEND");

	/**
	 * Constructs a new active high SuspendPin with default name SUSPEND.
	 */
	public SuspendPin() {
		super("SUSPEND");
	}

	/**
	 * Constructs a new active high named SuspendPin.
	 * 
	 * @param name
	 */
	public SuspendPin(String name) {
		super(name);
	}
}
