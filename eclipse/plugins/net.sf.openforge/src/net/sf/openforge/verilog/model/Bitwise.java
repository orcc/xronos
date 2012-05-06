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
 * Bitwise provides a collection of nested classes to represent bitwise
 * operations.
 * 
 * <P>
 * 
 * Created: Fri Mar 02 2001
 * 
 * @author abk
 * @version $Id: Bitwise.java 2 2005-06-09 20:00:48Z imiller $
 */
public abstract class Bitwise extends Operation {

	public Bitwise(Symbol op, Expression left, Expression right) {
		super(op, left, right);
	} // Bitwise()

	// //////////////////////////////////////////////
	//
	// inner classes
	//

	public static final class And extends Bitwise {
		public And(Expression left, Expression right) {
			super(Symbol.AND, left, right);
		}

		@Override
		public int precedence() {
			return BITWISE_AND_PRECEDENCE;
		}

		@Override
		public boolean isOrdered() {
			return false;
		}
	} // end of inner class And

	public static final class Or extends Bitwise {
		public Or(Expression left, Expression right) {
			super(Symbol.OR, left, right);
		}

		@Override
		public int precedence() {
			return BITWISE_OR_PRECEDENCE;
		}

		@Override
		public boolean isOrdered() {
			return false;
		}
	} // end of inner class Or

	public static final class Nand extends Bitwise {
		public Nand(Expression left, Expression right) {
			super(Symbol.NAND, left, right);
		}

		@Override
		public int precedence() {
			return REDUCTION_NAND_PRECEDENCE;
		}

		@Override
		public boolean isOrdered() {
			return false;
		}
	} // end of inner class Nand

	public static final class Nor extends Bitwise {
		public Nor(Expression left, Expression right) {
			super(Symbol.NOR, left, right);
		}

		@Override
		public int precedence() {
			return REDUCTION_NOR_PRECEDENCE;
		}

		@Override
		public boolean isOrdered() {
			return false;
		}
	} // end of inner class Nor

	public static final class Xor extends Bitwise {
		public Xor(Expression left, Expression right) {
			super(Symbol.XOR, left, right);
		}

		@Override
		public int precedence() {
			return REDUCTION_XOR_PRECEDENCE;
		}

		@Override
		public boolean isOrdered() {
			return false;
		}
	} // end of inner class Xor

	public static final class Xnor extends Bitwise {
		public Xnor(Expression left, Expression right) {
			super(Symbol.XNOR, left, right);
		}

		@Override
		public int precedence() {
			return REDUCTION_XNOR_PRECEDENCE;
		}

		@Override
		public boolean isOrdered() {
			return false;
		}
	} // end of inner class Xnor

} // end of class Bitwise
