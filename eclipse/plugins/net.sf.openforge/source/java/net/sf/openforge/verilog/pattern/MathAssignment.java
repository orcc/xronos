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

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import net.sf.openforge.lim.Port;
import net.sf.openforge.lim.op.AddMultiOp;
import net.sf.openforge.lim.op.AddOp;
import net.sf.openforge.lim.op.BinaryOp;
import net.sf.openforge.lim.op.DivideOp;
import net.sf.openforge.lim.op.ModuloOp;
import net.sf.openforge.lim.op.MultiplyOp;
import net.sf.openforge.lim.op.SubtractOp;
import net.sf.openforge.util.naming.ID;
import net.sf.openforge.verilog.model.Assign;
import net.sf.openforge.verilog.model.Expression;
import net.sf.openforge.verilog.model.Net;
import net.sf.openforge.verilog.model.NetFactory;
import net.sf.openforge.verilog.model.SignedWire;

/**
 * A MathAssignment is a Verilog math operation based on a LIM op which assigns
 * the result to a wire.
 * <P>
 * 
 * Created: Tue Mar 12 09:46:58 2002
 * 
 * @author <a href="mailto:andreas.kollegger@xilinx.com">Andy Kollegger</a>
 * @version $Id: MathAssignment.java 2 2005-06-09 20:00:48Z imiller $
 */

public abstract class MathAssignment extends BinaryOpAssignment {

	public MathAssignment(BinaryOp bo) {
		super(bo);
	}

	protected MathAssignment() {
	}

	// //////////////////////////////////////////////
	//
	// inner classes
	//

	public static final class Add extends MathAssignment {
		public Add(AddOp add) {
			super(add);
		}

		protected Expression makeOpExpression(Expression left, Expression right) {
			return (new net.sf.openforge.verilog.model.Math.Add(left, right));
		}
	} // class Add

	public static final class AddMulti extends MultiPortOpAssignment {
		public AddMulti(AddMultiOp add) {
			super(add);
		}

		protected Expression makeOpExpression(List expressions) {
			List exprs = new LinkedList(expressions);
			assert exprs.size() >= 2;

			Expression result = new net.sf.openforge.verilog.model.Math.Add(
					(Expression) exprs.remove(0), (Expression) exprs.remove(0));

			while (exprs.size() > 0) {
				result = new net.sf.openforge.verilog.model.Math.Add(result,
						(Expression) exprs.remove(0));
			}

			return result;
		}
	}

	public static final class Divide extends MathAssignment {
		public Divide(DivideOp div) {
			super(div);
		}

		protected Expression makeOpExpression(Expression left, Expression right) {
			return (new net.sf.openforge.verilog.model.Math.Divide(left, right));
		}
	} // class Divide

	public static final class Modulo extends MathAssignment {
		public Modulo(ModuloOp mod) {
			super(mod);
		}

		protected Expression makeOpExpression(Expression left, Expression right) {
			return (new net.sf.openforge.verilog.model.Math.Modulus(left, right));
		}
	} // class Modulo

	public static final class Multiply extends MathAssignment {
		private Collection myNets = new ArrayList();

		public Multiply(MultiplyOp mul) {
			super();

			Port l_port = mul.getLeftDataPort();
			assert (l_port.isUsed()) : "Left operand port in math operation is set to unused.";
			assert (l_port.getValue() != null) : "Left operand port in math operation does not have a value.";

			Port r_port = mul.getRightDataPort();
			assert (r_port.isUsed()) : "Right operand port in math operation is set to unused.";
			assert (r_port.getValue() != null) : "Right operand port in math operation does not have a value.";

			// Leave the operands the same so that we report the
			// correct 'consumed nets'
			left_operand = new PortWire(l_port, true);
			right_operand = new PortWire(r_port, true);

			result_wire = NetFactory.makeNet(mul.getResultBus());
			String baseName = ID.toVerilogIdentifier(ID.showLogical(mul
					.getResultBus()));
			Net aWire = new SignedWire(baseName + "_a_signed",
					this.left_operand.getWidth());
			Net bWire = new SignedWire(baseName + "_b_signed",
					this.right_operand.getWidth());

			add(new Assign.Continuous(aWire, left_operand));
			add(new Assign.Continuous(bWire, right_operand));
			add(new Assign.Continuous(result_wire, makeOpExpression(aWire,
					bWire)));

			myNets.add(aWire);
			myNets.add(bWire);
			myNets.add(result_wire);
		}

		protected Expression makeOpExpression(Expression left, Expression right) {
			// In actual usage, this method should not be called as it
			// does not support
			return (new net.sf.openforge.verilog.model.Math.Multiply(left,
					right));
		}

		/**
		 * Override the super so that we write out all the nets we generate.
		 * 
		 * @return a value of type 'Collection'
		 */
		public Collection getProducedNets() {
			return myNets;
		}

	} // class Multiply

	public static final class Subtract extends MathAssignment {
		public Subtract(SubtractOp sub) {
			super(sub);
		}

		protected Expression makeOpExpression(Expression left, Expression right) {
			return (new net.sf.openforge.verilog.model.Math.Subtract(left,
					right));
		}
	} // class Subtract

} // class MathAssignment
