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

package org.xronos.openforge.lim.op;

import org.xronos.openforge.lim.Exit;
import org.xronos.openforge.lim.Visitor;

/**
 * A ternary conditional operation in a form of " ? : ", which is the shorthand
 * if-else.
 * 
 * Created: Thu Mar 08 16:39:34 2002
 * 
 * @author Conor Wu
 * @version $Id: ShortcutIfElseOp.java 2 2005-06-09 20:00:48Z imiller $
 */
public class ShortcutIfElseOp extends TernaryOp {

	/**
	 * Constructs a conditional shorthand if-else operation.
	 * 
	 */
	public ShortcutIfElseOp() {
		super();
	}

	/**
	 * Accept method for the Visitor interface
	 */
	@Override
	public void accept(Visitor visitor) {
		visitor.visit(this);
	}

	/**
	 * Gets the gate depth of this component. This is the maximum number of
	 * gates that any input signal must traverse before reaching an {@link Exit}
	 * .
	 * 
	 * @return a non-negative integer
	 */
	@Override
	public int getGateDepth() {
		/*
		 * XXX: This isn't necessarily correct. Only one of the second and third
		 * operands is evaluated.
		 */
		return 2;
	}

	/*
	 * =================================================== Begin new constant
	 * prop rules implementation.
	 */
	@Override
	public boolean pushValuesForward() {
		boolean mod = false;

		// No rules

		return mod;
	}

	@Override
	public boolean pushValuesBackward() {
		boolean mod = false;

		// No rules

		return mod;
	}
}
