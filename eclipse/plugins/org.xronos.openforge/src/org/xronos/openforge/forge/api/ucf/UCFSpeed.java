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

package org.xronos.openforge.forge.api.ucf;

/**
 * The UCF speed attribute for pins. May be FAST or SLOW.
 */
public final class UCFSpeed implements UCFAttribute {

	public final static UCFSpeed FAST = new UCFSpeed("FAST");
	public final static UCFSpeed SLOW = new UCFSpeed("SLOW");

	private String value;

	private UCFSpeed(String value) {
		this.value = value;
	}

	/**
	 * Produces the ucf attribute "value".
	 */
	@Override
	public String toString() {
		return value;
	}

	/**
	 * @see org.xronos.openforge.forge.api.ucf.UCFAttribute#getBit
	 */
	@Override
	public int getBit() {
		return -1;
	}
}
