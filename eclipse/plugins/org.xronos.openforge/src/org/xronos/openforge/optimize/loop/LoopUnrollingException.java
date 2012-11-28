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

package org.xronos.openforge.optimize.loop;

import org.xronos.openforge.lim.Loop;

/**
 * Thrown to indicate that a {@link Loop} that is being analyzed is not
 * unrollable.
 * 
 * @version $Id: LoopUnrollingException.java 2 2005-06-09 20:00:48Z imiller $
 */
class LoopUnrollingException extends Exception {
	private static final long serialVersionUID = 2086416081850522334L;

	/**
	 * Creates a new <code>LoopUnrollingException</code> instance.
	 * 
	 * @param message
	 *            a description of the exceptional condition
	 */
	LoopUnrollingException(String message) {
		super(message);
	}
}
