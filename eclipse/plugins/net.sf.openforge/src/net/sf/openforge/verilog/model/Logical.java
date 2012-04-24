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
package net.sf.openforge.verilog.model;

/**
 * Logical provides a collection of nested classes to represent comparison
 * operations. These operations return a single bit result of 0 or 1.
 * <P>
 * 
 * Created: Fri Mar 02 2001
 * 
 * @author abk
 * @version $Id: Logical.java 2 2005-06-09 20:00:48Z imiller $
 */
public abstract class Logical extends Operation {

	public Logical(Symbol op, Expression left, Expression right) {
		super(op, left, right);
	} // Logical()

	public int getWidth() {
		return 1;
	}

	public boolean isOrdered() {
		return false;
	}

	// //////////////////////////////////////////////
	//
	// inner classes
	//

	public static final class And extends Logical {
		public And(Expression left, Expression right) {
			super(Symbol.LOGICAL_AND, left, right);
		}

		public int precedence() {
			return LOGICAL_AND_PRECEDENCE;
		}
	} // end of inner class And

	public static final class Or extends Logical {
		public Or(Expression left, Expression right) {
			super(Symbol.LOGICAL_OR, left, right);
		}

		public int precedence() {
			return LOGICAL_OR_PRECEDENCE;
		}
	} // end of inner class Or

} // end of class Logical
