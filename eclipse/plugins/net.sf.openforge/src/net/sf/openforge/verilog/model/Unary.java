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

/**
 * Unary is an expression which prepends an expression with an operator.
 * 
 * <P>
 * 
 * Created: Fri Mar 02 2001
 * 
 * @author abk
 * @version $Id: Unary.java 2 2005-06-09 20:00:48Z imiller $
 */
public class Unary implements Expression {

	private Symbol op;
	private Expression exp;

	protected Unary(Symbol op, Expression exp) {
		this.op = op;
		this.exp = exp;
	} // Unary()

	public Collection getNets() {
		return exp.getNets();
	}

	public Lexicality lexicalify() {
		Lexicality lex = new Lexicality();

		lex.append(op);
		lex.append(exp);

		return lex;
	} // lexicalify()

	public int getWidth() {
		return exp.getWidth();
	}

	public String toString() {
		return lexicalify().toString();
	}

	// //////////////////////////////////////////////
	//
	// inner classes
	//

	/** The logical negate unary operation. */
	public static final class Not extends Unary {
		public Not(Expression exp) {
			super(Symbol.LOGICAL_NEGATE, exp);
		}
	} // end of inner class Not

	/** The bitwise negate unary operation. */
	public static final class Negate extends Unary {
		public Negate(Expression exp) {
			super(Symbol.BITWISE_NEGATE, exp);
		}
	} // end of inner class Negate

	/** The reduction and operation. */
	public static final class And extends Unary {
		public And(Expression exp) {
			super(Symbol.AND, exp);
		}
	} // end of inner class And

	/** The reduction nand operation. */
	public static final class Nand extends Unary {
		public Nand(Expression exp) {
			super(Symbol.NAND, exp);
		}
	} // end of inner class Nand

	/** The reduction or operation. */
	public static final class Or extends Unary {
		public Or(Expression exp) {
			super(Symbol.OR, exp);
		}
	} // end of inner class Or

	/** The reduction nor operation. */
	public static final class Nor extends Unary {
		public Nor(Expression exp) {
			super(Symbol.NOR, exp);
		}
	} // end of inner class Nor

	/** The reduction xor operation. */
	public static final class Xor extends Unary {
		public Xor(Expression exp) {
			super(Symbol.XOR, exp);
		}
	} // end of inner class Xor

	/** The reduction xnor operation. */
	public static final class Xnor extends Unary {
		public Xnor(Expression exp) {
			super(Symbol.XNOR, exp);
		}
	} // end of inner class Xnor

} // end of class Unary
