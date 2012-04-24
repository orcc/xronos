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
 * Math provides a collection of nested classes to represent mathematical
 * operations.
 * 
 * <P>
 * 
 * Created: Fri Mar 02 2001
 * 
 * @author abk
 * @version $Id: Math.java 2 2005-06-09 20:00:48Z imiller $
 */
public abstract class Math extends Operation {

	public Math(Symbol op, Expression left, Expression right) {
		super(op, left, right);
	} // Math()

	// //////////////////////////////////////////////
	//
	// inner classes
	//

	public static final class Add extends Math {
		public Add(Expression left, Expression right) {
			super(Symbol.PLUS, left, right);
		}

		public int precedence() {
			return BINARY_PLUS_PRECEDENCE;
		}

		public boolean isOrdered() {
			return false;
		}

	} // end of inner class Add

	public static final class Subtract extends Math {
		public Subtract(Expression left, Expression right) {
			super(Symbol.MINUS, left, right);
		}

		public int precedence() {
			return BINARY_MINUS_PRECEDENCE;
		}

		public boolean isOrdered() {
			return true;
		}
	} // end of inner class Subtract

	public static final class Multiply extends Math {
		public Multiply(Expression left, Expression right) {
			super(Symbol.MULTIPLY, left, right);
		}

		public int precedence() {
			return MULTIPLY_PRECEDENCE;
		}

		public boolean isOrdered() {
			return false;
		}

		public boolean allowUnbalanced() {
			return true;
		}
	} // end of inner class Multiply

	public static final class Divide extends Math {
		public Divide(Expression left, Expression right) {
			super(Symbol.DIVIDE, left, right);
		}

		public int precedence() {
			return DIVIDE_PRECEDENCE;
		}

		public boolean isOrdered() {
			return true;
		}
	} // end of inner class Divide

	public static final class Modulus extends Math {
		public Modulus(Expression left, Expression right) {
			super(Symbol.MODULUS, left, right);
		}

		public int precedence() {
			return MODULUS_PRECEDENCE;
		}

		public boolean isOrdered() {
			return true;
		}
	} // end of inner class Modulus

} // end of class Math
