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

package net.sf.openforge.lim;

/**
 * An Operation is a an executable {@link Component}. Operations are assembled
 * into {@link Module Modules}.
 * 
 * @version $Id: Operation.java 2 2005-06-09 20:00:48Z imiller $
 */
public abstract class Operation extends Component {

	/**
	 * Constructs a new Operation.
	 * 
	 * @param argCount
	 *            the number of data {@link Port Ports} to create
	 * @param resultCount
	 *            the number of data {@link Bus Buses} to create
	 */
	public Operation(int argCount) {
		super(argCount);
	}

	/**
	 * Constructs a new Operation with no data {@link Port Ports}.
	 * 
	 */
	public Operation() {
		this(0);
	}

	/**
	 * Tests whether this component contains a wait directive. If so, everything
	 * prior to it must complete before it can execute, and everyhing after it
	 * must wait until it completes before executing.
	 */
	public boolean hasWait() {
		return false;
	}

	/**
	 * Returns true if the implementation of this operation is floating point,
	 * always returns false, overidden by operations that may be floats. Being a
	 * 'float' operation means that this Component processes floating point
	 * values by consuming float/double inputs <b>and/or</b> generating a
	 * float/double output. It does not necessarily mean that it produces a
	 * float output (see the comparison ops).
	 */
	public boolean isFloat() {
		return false;
	}

	/**
	 * Asserts false until rule is supported.
	 */
	@Override
	protected boolean pushValuesForward() {
		assert false : "new pushValuesForward propagation of constants through "
				+ this.getClass() + " not yet supported";
		return false;
	}

	/**
	 * Asserts false until rule is supported.
	 */
	@Override
	protected boolean pushValuesBackward() {
		assert false : "new pushValuesBackward propagation of constants through "
				+ this.getClass() + " not yet supported";
		return false;
	}

	/*
	 * End new constant prop rules implementation.
	 * =================================================
	 */
}
