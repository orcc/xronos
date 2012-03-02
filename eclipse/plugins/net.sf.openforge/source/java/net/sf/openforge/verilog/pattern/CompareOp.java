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
package net.sf.openforge.verilog.pattern;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import net.sf.openforge.lim.Port;
import net.sf.openforge.lim.op.BinaryOp;
import net.sf.openforge.lim.op.EqualsOp;
import net.sf.openforge.lim.op.GreaterThanEqualToOp;
import net.sf.openforge.lim.op.GreaterThanOp;
import net.sf.openforge.lim.op.LessThanEqualToOp;
import net.sf.openforge.lim.op.LessThanOp;
import net.sf.openforge.lim.op.NotEqualsOp;
import net.sf.openforge.util.naming.ID;
import net.sf.openforge.verilog.model.Assign;
import net.sf.openforge.verilog.model.Compare;
import net.sf.openforge.verilog.model.Expression;
import net.sf.openforge.verilog.model.Net;
import net.sf.openforge.verilog.model.NetFactory;
import net.sf.openforge.verilog.model.SignedWire;
import net.sf.openforge.verilog.model.Wire;

/**
 * A CompareOp is a verilog assignment statement, based on a {@link BinaryOp},
 * which assigns the result to a wire.
 * 
 * We use signed compare operations for every case. Currently Icarus simulates
 * signed <= and >= wrong. This is correct verilog code that simulates correctly
 * with modelsim.
 * <P>
 * 
 * Created: Tue Mar 12 09:46:58 2002
 * 
 * @author <a href="mailto:andreas.kollegger@xilinx.com">Andy Kollegger</a>
 * @version $Id: CompareOp.java 2 2005-06-09 20:00:48Z imiller $
 */

public abstract class CompareOp extends StatementBlock implements ForgePattern {
	Net result_wire;
	Wire left_operand;
	Wire right_operand;
	BinaryOp binaryOp;

	Set produced_nets = new HashSet();

	public CompareOp(BinaryOp bo) {
		this.binaryOp = bo;

		/**
		 * Here we create two singed wires and plugin the compareop
		 */
		Port l_port = bo.getLeftDataPort();
		assert (l_port.isUsed()) : "Left operand port in compare operation is set to unused.";
		assert (l_port.getValue() != null) : "Left operand port in compare operation does not have a value.";

		Port r_port = bo.getRightDataPort();
		assert (r_port.isUsed()) : "Right operand port in compare operation is set to unused.";
		assert (r_port.getValue() != null) : "Right operand port in compare operation does not have a value.";

		left_operand = new PortWire(l_port, true);
		right_operand = new PortWire(r_port, true);

		String baseName = ID.toVerilogIdentifier(ID.showLogical(bo
				.getResultBus()));
		int size = 0;

		if (left_operand.getWidth() > right_operand.getWidth()) {
			size = left_operand.getWidth();
		} else {
			size = right_operand.getWidth();
		}

		// System.out.println( bo +"Size = "+ size);

		Net aWire = null;
		if (l_port.getValue().isSigned()) {
			aWire = new SignedWire(baseName + "_a_signed", size);
		} else {
			aWire = new Wire(baseName + "_a_unsigned", size);
		}

		Net bWire = null;
		if (r_port.getValue().isSigned()) {
			bWire = new SignedWire(baseName + "_b_signed", size);
		} else {
			bWire = new Wire(baseName + "_b_unsigned", size);
		}

		add(new Assign.Continuous(aWire, left_operand.getRange(size - 1, 0)));
		add(new Assign.Continuous(bWire, right_operand.getRange(size - 1, 0)));

		result_wire = NetFactory.makeNet(bo.getResultBus());

		add(new Assign.Continuous(result_wire, makeExpression(aWire, bWire)));

		produced_nets.add(aWire);
		produced_nets.add(bWire);
		produced_nets.add(result_wire);

	}

	protected abstract Expression makeExpression(Expression left,
			Expression right);

	// not equal inner class
	public static final class NotEquals extends CompareOp {
		public NotEquals(NotEqualsOp op) {
			super(op);
		}

		protected Expression makeExpression(Expression left, Expression right) {
			/* Compose the Verilog expression for this operation. */
			return new Compare.NEQ(left, right);
		}
	}

	// equals inner class
	public static final class Equals extends CompareOp {
		public Equals(EqualsOp op) {
			super(op);
		}

		protected Expression makeExpression(Expression left, Expression right) {
			/* Compose the Verilog expression for this operation. */
			return new Compare.EQ(left, right);
		}
	}

	// less than inner class
	public static final class LessThan extends CompareOp {
		public LessThan(LessThanOp op) {
			super(op);
		}

		protected Expression makeExpression(Expression left, Expression right) {
			return new Compare.LT(left, right);
		}
	}

	// less than equals inner class
	public static final class LessThanEqualTo extends CompareOp {
		public LessThanEqualTo(LessThanEqualToOp op) {
			super(op);
		}

		protected Expression makeExpression(Expression left, Expression right) {
			/*
			 * due to an icarus bug involving <= and >= we change these
			 * operations from a <= b to b > a
			 */
			// return new Compare.GT(right, left);
			return new Compare.LTEQ(left, right);
		}
		// protected Expression makeExpression(Expression left, Expression
		// right)
		// {
		// if (binaryOp.isSigned())
		// {
		// /* Compose the Verilog expression for this operation. */
		// // sign compare such a pain in verilog
		//
		// int msbLeft = left.getWidth()-1; //left_operand.getMSB();
		// int lsbLeft = 0; //left_operand.getLSB();
		// int msbRight = right.getWidth()-1; //right_operand.getMSB();
		// int lsbRight = 0; //right_operand.getLSB();
		//
		// // Compose the Verilog expressions for this operation.
		// // 1. Xor sign bits and negate the result
		// Expression xorMSB =
		// new Bitwise.Xor(
		// left_operand.getRange(msbLeft, msbLeft),
		// right_operand.getRange(msbRight, msbRight));
		// Expression parenXorMSB = new Group(xorMSB);
		// Expression negateXorMSB = new Unary.Not(parenXorMSB);
		// Expression eqExpr = negateXorMSB;
		// assert msbLeft == msbRight : "Inputs of LTEQ must be same size";
		// if (msbLeft > 0)
		// {
		// // 2. Generate the comparison expression
		// Expression compareExp =
		// new Compare.LTEQ(
		// left_operand.getRange(msbLeft - 1, lsbLeft),
		// right_operand.getRange(msbRight - 1, lsbRight));
		// Expression parenCompareExp = new Group(compareExp);
		// // 3. Logical-and 1 with 2
		// Expression andExp1 =
		// new Logical.And(negateXorMSB, parenCompareExp);
		// Expression parenAndExp1 = new Group(andExp1);
		// eqExpr = parenAndExp1;
		// }
		// // 4. Logical-and MSB of the left with negated MSB of the right
		// Expression negateMSB =
		// new Unary.Not(right_operand.getRange(msbRight, msbRight));
		// Expression andExp2 =
		// new Logical.And(
		// left_operand.getRange(msbLeft, msbLeft),
		// negateMSB);
		// Expression parenAndExp2 = new Group(andExp2);
		// // 5. Finally, logical-or 3 with 4
		// //return new Logical.Or(parenAndExp1, parenAndExp2);
		// return new Logical.Or(eqExpr, parenAndExp2);
		// }
		// else
		// {
		// return new Compare.LTEQ(left, right);
		// }
		//
		//
		// }
	}

	// greater than inner class
	public static final class GreaterThan extends CompareOp {
		public GreaterThan(GreaterThanOp op) {
			super(op);
		}

		protected Expression makeExpression(Expression left, Expression right) {

			return new Compare.GT(left, right);
		}
	}

	// greater than equal to inner class
	public static final class GreaterThanEqualTo extends CompareOp {
		public GreaterThanEqualTo(GreaterThanEqualToOp op) {
			super(op);
		}

		protected Expression makeExpression(Expression left, Expression right) {
			/*
			 * due to an icarus bug involving <= and >= we change these
			 * operations from a >= b to b < a
			 */
			// return new Compare.LT(right, left);
			return new Compare.GTEQ(left, right);
		}

		// protected Expression makeExpression(Expression left, Expression
		// right)
		// {
		// if (binaryOp.isSigned())
		// {
		// /* Compose the Verilog expression for this operation. */
		// // sign compare such a pain in verilog
		//
		// int msbLeft = left.getWidth()-1; //left_operand.getMSB();
		// int lsbLeft = 0; //left_operand.getLSB();
		// int msbRight = right.getWidth()-1; //right_operand.getMSB();
		// int lsbRight = 0; //right_operand.getLSB();
		//
		// // Compose the Verilog expressions for this operation.
		// // 1. Xor sign bits and negate the result
		// Expression xorMSB =
		// new Bitwise.Xor(
		// left_operand.getRange(msbLeft, msbLeft),
		// right_operand.getRange(msbRight, msbRight));
		// Expression parenXorMSB = new Group(xorMSB);
		// Expression negateXorMSB = new Unary.Not(parenXorMSB);
		// Expression eqExpr = negateXorMSB;
		// assert msbLeft == msbRight : "Inputs of GTEQ must be same size";
		// if (msbLeft > 0)
		// {
		// // 2. Generate the comparison expression
		// Expression compareExp =
		// new Compare.GTEQ(
		// left_operand.getRange(msbLeft - 1, lsbLeft),
		// right_operand.getRange(msbRight - 1, lsbRight));
		// Expression parenCompareExp = new Group(compareExp);
		// // 3. Logical-and 1 with 2
		// Expression andExp1 =
		// new Logical.And(negateXorMSB, parenCompareExp);
		// Expression parenAndExp1 = new Group(andExp1);
		// eqExpr = parenAndExp1;
		// }
		// // 4. Logical-and negated MSB of the left with MSB of the right
		// Expression negateMSB =
		// new Unary.Not(left_operand.getRange(msbLeft, msbLeft));
		// Expression andExp2 =
		// new Logical.And(
		// negateMSB,
		// right_operand.getRange(msbRight, msbRight));
		// Expression parenAndExp2 = new Group(andExp2);
		// // 5. Finally, logical-or 3 with 4
		// //return new Logical.Or(parenAndExp1, parenAndExp2);
		// return new Logical.Or(eqExpr, parenAndExp2);
		// }
		// else
		// {
		// return new Compare.GTEQ(left,right);
		// }
		//
		// }
	}

	public Collection getConsumedNets() {
		Set consumed = new HashSet();
		consumed.add(left_operand);
		consumed.add(right_operand);
		return consumed;
	}

	public Collection getProducedNets() {
		return produced_nets;
	}

} // class CompareOp
