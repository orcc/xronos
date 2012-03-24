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

package net.sf.openforge.forge.api.ucf;

/**
 * The LOC attribute for pins. For a full description of the LOC constraints,
 * please see the Constraints Guide section of the online documentation located
 * at:"<path-to-ISE>/doc/usenglish/manuals.pdf".
 */
public final class UCFLocation implements UCFAttribute {
	private int bit;
	private String value;

	/**
	 * Creates a new UCFLocation, specifying the location for a particular bit.
	 */
	public UCFLocation(int bit, String value) {
		this.bit = bit;
		this.value = value;
	}

	/**
	 * @see net.sf.openforge.forge.api.ucf.UCFAttribute#getBit
	 */
	@Override
	public int getBit() {
		return bit;
	}

	/**
	 * Produces the ucf attribute "LOC=value".
	 */
	@Override
	public String toString() {
		return "LOC=\"" + value + "\"";
	}
}
