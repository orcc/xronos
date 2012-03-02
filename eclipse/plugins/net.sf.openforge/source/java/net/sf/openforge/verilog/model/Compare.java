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
 * Compare provides a collection of nested classes to represent comparison
 * operations. These operations return a single bit result of 0 or 1.
 * 
 * <P>
 * 
 * Created: Fri Mar 02 2001
 * 
 * @author abk
 * @version $Id: Compare.java 2 2005-06-09 20:00:48Z imiller $
 */
public abstract class Compare extends Operation {
	@SuppressWarnings("unused")
	private static final String _RCS_ = "RCS_REVISION: $Rev: 2 $";

	public Compare(Symbol op, Expression left, Expression right) {
		super(op, left, right);
	} // Compare()

	public int getWidth() {
		return 1;
	}

	// //////////////////////////////////////////////
	//
	// inner classes
	//

	public static final class GT extends Compare {
		public GT(Expression left, Expression right) {
			super(Symbol.GREATER_THAN, left, right);
		}

		public int precedence() {
			return GT_PRECEDENCE;
		}

		public boolean isOrdered() {
			return true;
		}
	} // end of inner class GT

	public static final class LT extends Compare {
		public LT(Expression left, Expression right) {
			super(Symbol.LESS_THAN, left, right);
		}

		public int precedence() {
			return LT_PRECEDENCE;
		}

		public boolean isOrdered() {
			return true;
		}
	} // end of inner class LT

	public static final class GTEQ extends Compare {
		public GTEQ(Expression left, Expression right) {
			super(Symbol.GTEQ, left, right);
		}

		public int precedence() {
			return GTEQ_PRECEDENCE;
		}

		public boolean isOrdered() {
			return true;
		}
	} // end of inner class GTEQ

	public static final class LTEQ extends Compare {
		public LTEQ(Expression left, Expression right) {
			super(Symbol.LTEQ, left, right);
		}

		public int precedence() {
			return LTEQ_PRECEDENCE;
		}

		public boolean isOrdered() {
			return true;
		}
	} // end of inner class LTEQ

	public static final class EQ extends Compare {
		public EQ(Expression left, Expression right) {
			super(Symbol.EQ, left, right);
		}

		public int precedence() {
			return LOGICAL_EQUALITY_PRECEDENCE;
		}

		public boolean isOrdered() {
			return true;
		}
	} // end of inner class EQ

	public static final class Equals extends Compare {
		public Equals(Expression left, Expression right) {
			super(Symbol.EQ, left, right);
		}

		public int precedence() {
			return LOGICAL_EQUALITY_PRECEDENCE;
		}

		public boolean isOrdered() {
			return true;
		}
	} // end of inner class Equals

	public static final class NEQ extends Compare {
		public NEQ(Expression left, Expression right) {
			super(Symbol.NEQ, left, right);
		}

		public int precedence() {
			return LOGICAL_INEQUALITY_PRECEDENCE;
		}

		public boolean isOrdered() {
			return true;
		}
	} // end of inner class NEQ

	public static final class CASE_EQ extends Compare {
		public CASE_EQ(Expression left, Expression right) {
			super(Symbol.CASE_EQUALITY, left, right);
		}

		public int precedence() {
			return CASE_EQUALITY_PRECEDENCE;
		}

		public boolean isOrdered() {
			return true;
		}
	} // end of inner class CASE_EQ

	public static final class CASE_NEQ extends Compare {
		public CASE_NEQ(Expression left, Expression right) {
			super(Symbol.CASE_INEQUALITY, left, right);
		}

		public int precedence() {
			return CASE_INEQUALITY_PRECEDENCE;
		}

		public boolean isOrdered() {
			return true;
		}
	} // end of inner class CASE_NEQ

} // end of class Compare
