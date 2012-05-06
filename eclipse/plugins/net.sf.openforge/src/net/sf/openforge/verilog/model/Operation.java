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

import java.util.Collection;
import java.util.HashSet;

/**
 * Operation is an expression which joins two sub-expression with an operator.
 * 
 * <P>
 * 
 * Created: Thu Mar 01 2001
 * 
 * @author abk
 * @version $Id: Operation.java 2 2005-06-09 20:00:48Z imiller $
 */
public abstract class Operation implements Expression {

	private Symbol op;
	private Expression left;
	private Expression right;

	/**
	 * Precedence constants are listed from lowest to highest. In the source,
	 * different precedence levels are separated by a newline, while operations
	 * with the same precedence are bunched together.
	 */
	public final static int CONDITIONAL_PRECEDENCE = 0;

	public final static int LOGICAL_OR_PRECEDENCE = CONDITIONAL_PRECEDENCE + 1;

	public final static int LOGICAL_AND_PRECEDENCE = LOGICAL_OR_PRECEDENCE + 1;

	public final static int BITWISE_OR_PRECEDENCE = LOGICAL_AND_PRECEDENCE + 1;

	public final static int BITWISE_XNOR_PRECEDENCE = BITWISE_OR_PRECEDENCE + 1;
	public final static int BITWISE_XOR_PRECEDENCE = BITWISE_OR_PRECEDENCE + 1;

	public final static int BITWISE_AND_PRECEDENCE = BITWISE_XOR_PRECEDENCE + 1;

	public final static int CASE_INEQUALITY_PRECEDENCE = BITWISE_AND_PRECEDENCE + 1;
	public final static int CASE_EQUALITY_PRECEDENCE = BITWISE_AND_PRECEDENCE + 1;
	public final static int LOGICAL_INEQUALITY_PRECEDENCE = BITWISE_AND_PRECEDENCE + 1;
	public final static int LOGICAL_EQUALITY_PRECEDENCE = BITWISE_AND_PRECEDENCE + 1;

	public final static int GTEQ_PRECEDENCE = LOGICAL_EQUALITY_PRECEDENCE + 1;
	public final static int GT_PRECEDENCE = LOGICAL_EQUALITY_PRECEDENCE + 1;
	public final static int LTEQ_PRECEDENCE = LOGICAL_EQUALITY_PRECEDENCE + 1;
	public final static int LT_PRECEDENCE = LOGICAL_EQUALITY_PRECEDENCE + 1;

	public final static int RIGHT_SHIFT_PRECEDENCE = LT_PRECEDENCE + 1;
	public final static int LEFT_SHIFT_PRECEDENCE = LT_PRECEDENCE + 1;

	public final static int BINARY_MINUS_PRECEDENCE = LEFT_SHIFT_PRECEDENCE + 1;
	public final static int BINARY_PLUS_PRECEDENCE = LEFT_SHIFT_PRECEDENCE + 1;

	public final static int MODULUS_PRECEDENCE = BINARY_PLUS_PRECEDENCE + 1;
	public final static int DIVIDE_PRECEDENCE = BINARY_PLUS_PRECEDENCE + 1;
	public final static int MULTIPLY_PRECEDENCE = BINARY_PLUS_PRECEDENCE + 1;

	public final static int REDUCTION_NOR_PRECEDENCE = MULTIPLY_PRECEDENCE + 1;
	public final static int REDUCTION_OR_PRECEDENCE = MULTIPLY_PRECEDENCE + 1;
	public final static int REDUCTION_XNOR_PRECEDENCE = MULTIPLY_PRECEDENCE + 1;
	public final static int REDUCTION_XOR_PRECEDENCE = MULTIPLY_PRECEDENCE + 1;
	public final static int REDUCTION_NAND_PRECEDENCE = MULTIPLY_PRECEDENCE + 1;
	public final static int REDUCTION_AND_PRECEDENCE = MULTIPLY_PRECEDENCE + 1;
	public final static int UNARY_BITWISE_NEGATION_PRECEDENCE = MULTIPLY_PRECEDENCE + 1;
	public final static int UNARY_LOGICAL_NEGATION_PRECEDENCE = MULTIPLY_PRECEDENCE + 1;
	public final static int UNARY_MINUS_PRECEDENCE = MULTIPLY_PRECEDENCE + 1;
	public final static int UNARY_PLUS_PRECEDENCE = MULTIPLY_PRECEDENCE + 1;

	public Operation(Symbol op, Expression left, Expression right) {
		assert (left != null) : "null left operand in Operation.";
		assert (right != null) : "null right operand in Operation.";
		if ((left.getWidth() != right.getWidth()) && !allowUnbalanced()) {
			throw new UnbalancedOperationException(left, right);
		}

		this.op = op;

		if (left instanceof Operation) {
			if ((((Operation) left).precedence() >= precedence())
					&& (!isOrdered())) {
				this.left = left;
			} else {
				this.left = new Group(left);
			}
		} else {
			this.left = left;
		}

		if (right instanceof Operation) {
			if ((((Operation) right).precedence() >= precedence())
					&& (!isOrdered())) {
				this.right = right;
			} else {
				this.right = new Group(right);
			}
		} else {
			this.right = right;
		}

	} // Operation()

	@SuppressWarnings("serial")
	public static class UnbalancedOperationException extends
			VerilogSyntaxException {

		public UnbalancedOperationException(Expression left, Expression right) {
			super(new String("Operation on unbalanced expressions: "
					+ left.toString() + "(" + left.getWidth() + ")"
					+ " does not match " + right.toString() + "("
					+ right.getWidth() + ")"));
		} // UnbalancedOperationException()

	} // end of nested class UnbalancedAssignmentException

	public abstract int precedence();

	public abstract boolean isOrdered();

	/**
	 * If sub classes override this method then we allow the input sizes to not
	 * match. This was added as a means of overcoming an XST deficiency. XST
	 * does not optimize multipliers with leading 0s, so we trim those leading
	 * zeros as needed. This can cause the multiply to end up with unbalanced
	 * inputs.
	 * 
	 * @return false
	 */
	protected boolean allowUnbalanced() {
		return false;
	}

	@Override
	public int getWidth() {
		return left.getWidth();
	}

	@Override
	public Collection<Net> getNets() {
		Collection<Net> c = new HashSet<Net>();

		c.addAll(left.getNets());
		c.addAll(right.getNets());

		return c;
	} // getNets()

	@Override
	public Lexicality lexicalify() {
		Lexicality lex = new Lexicality();

		lex.append(left);
		lex.append(op);
		lex.append(right);

		return lex;
	} // lexicalify()

	@Override
	public String toString() {
		return lexicalify().toString();
	}

	// //////////////////////////////////////////////
	//
	// inner classes
	//

	public static final class ShiftLeft extends Operation {
		public ShiftLeft(Expression left, Expression right) {
			super(Symbol.LEFT_SHIFT, left, right);
		}

		@Override
		public int precedence() {
			return LEFT_SHIFT_PRECEDENCE;
		}

		@Override
		public boolean isOrdered() {
			return true;
		}
	} // end of inner class ShiftLeft

	public static final class SHL extends Operation {
		public SHL(Expression left, Expression right) {
			super(Symbol.LEFT_SHIFT, left, right);
		}

		@Override
		public int precedence() {
			return LEFT_SHIFT_PRECEDENCE;
		}

		@Override
		public boolean isOrdered() {
			return true;
		}
	} // end of inner class SHL

	public static final class ShiftRight extends Operation {
		public ShiftRight(Expression left, Expression right) {
			super(Symbol.RIGHT_SHIFT, left, right);
		}

		@Override
		public int precedence() {
			return RIGHT_SHIFT_PRECEDENCE;
		}

		@Override
		public boolean isOrdered() {
			return true;
		}
	} // end of inner class ShiftRight

	public static final class SHR extends Operation {
		public SHR(Expression left, Expression right) {
			super(Symbol.RIGHT_SHIFT, left, right);
		}

		@Override
		public int precedence() {
			return RIGHT_SHIFT_PRECEDENCE;
		}

		@Override
		public boolean isOrdered() {
			return true;
		}
	} // end of inner class SHR

} // end of class Operation

