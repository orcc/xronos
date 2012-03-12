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
 * ResultPin is a specific type of Buffer that is used as an output, supplying
 * the calculated result of an entry method.
 * 
 */
public class ResultPin extends Buffer {
	/**
	 * The default result signal used whenever a result is needed but not
	 * explicitly provided.
	 */
	public final static ResultPin GLOBAL = new ResultPin("RESULT");

	/**
	 * Constructs a new ResultPin with default name RESULT.
	 */
	public ResultPin() {
		/**
		 * Sizing is initially set to 64 bits, but will ultimately be determined
		 * by the natural size of a method's return type and may possibly be
		 * reduced through optimizations.
		 */
		super("RESULT", 64);
	}

	/**
	 * Constructs a new named ResultPin.
	 * 
	 * @param name
	 */
	public ResultPin(String name) {
		/**
		 * Sizing is initially set to 64 bits, but will ultimately be determined
		 * by the natural size of a method's return type and may possibly be
		 * reduced through optimizations.
		 */
		super(name, 64);
	}
}
