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
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import net.sf.openforge.lim.Bus;
import net.sf.openforge.lim.Port;
import net.sf.openforge.lim.op.ComplementOp;
import net.sf.openforge.lim.op.MinusOp;
import net.sf.openforge.lim.op.NotOp;
import net.sf.openforge.lim.op.ReductionOrOp;
import net.sf.openforge.lim.op.UnaryOp;
import net.sf.openforge.util.naming.ID;
import net.sf.openforge.verilog.model.Assign;
import net.sf.openforge.verilog.model.BaseNumber;
import net.sf.openforge.verilog.model.Expression;
import net.sf.openforge.verilog.model.HexConstant;
import net.sf.openforge.verilog.model.HexNumber;
import net.sf.openforge.verilog.model.Net;
import net.sf.openforge.verilog.model.NetFactory;
import net.sf.openforge.verilog.model.Wire;

/**
 * A UnaryOpAssignment is a verilog assignment statement, based on a
 * {@link UnaryOp}, which assigns the result to a wire.
 * <P>
 * 
 * Created: Tue Mar 12 09:46:58 2002
 * 
 * @author <a href="mailto:andreas.kollegger@xilinx.com">Andy Kollegger</a>
 * @version $Id: UnaryOpAssignment.java 2 2005-06-09 20:00:48Z imiller $
 */

public abstract class UnaryOpAssignment extends StatementBlock implements
		ForgePattern {

	Expression operand;
	Net result_wire;

	public UnaryOpAssignment(UnaryOp uo, boolean checkBalance) {
		Iterator<Port> ports = uo.getDataPorts().iterator();
		Port d_port = ports.next();
		assert (d_port.isUsed()) : "Operand port in unary operation is set to unused.";
		// Bus d_bus = d_port.getBus();
		// assert (d_bus != null) :
		// "Operand port in unary operation not attached to a bus.";
		assert (d_port.getValue() != null) : "Operand port in unary operation does not have a value.";
		operand = new PortWire(d_port);

		result_wire = NetFactory.makeNet(uo.getResultBus());

		add(new Assign.Continuous(result_wire, makeOpExpression(operand),
				checkBalance));
	}

	public UnaryOpAssignment(UnaryOp uo) {
		this(uo, true);
	}

	protected abstract Expression makeOpExpression(Expression operand);

	@Override
	public Collection<Net> getConsumedNets() {
		return Collections.singleton((Net) operand);
	}

	@Override
	public Collection<Net> getProducedNets() {
		return Collections.singleton(result_wire);
	}

	// //////////////////////////////////////////////
	//
	// inner classes
	//

	public static final class Negate extends UnaryOpAssignment {
		public Negate(ComplementOp negate) {
			super(negate);
		}

		@Override
		protected Expression makeOpExpression(Expression operand) {
			return (new net.sf.openforge.verilog.model.Unary.Negate(operand));
		}
	} // class Negate

	public static final class SignExtend extends StatementBlock implements
			ForgePattern {
		Wire operand;
		Net result_wire;

		Set<Net> produced_nets = new HashSet<Net>();

		public SignExtend(UnaryOp cast) {
			Iterator<Port> ports = cast.getDataPorts().iterator();
			Port d_port = ports.next();
			assert (d_port.isUsed()) : "Operand port in unary operation is set to unused.";
			Bus d_bus = d_port.getBus();
			assert (d_bus != null) : "Operand port in unary operation not attached to a bus.";
			PortWire pwire = new PortWire(d_port);
			if (pwire.getExpression() instanceof BaseNumber) {
				// input is a constant value, so the resize should be a no-op?
			} else {
				if (pwire.getExpression() instanceof Wire) {
					operand = (Wire) pwire.getExpression();
				} else {
					operand = new Wire(ID.toVerilogIdentifier(ID
							.showLogical(cast)), d_port.getValue().getSize());
					add(new Assign.Continuous(operand, new PortWire(d_port)));
					produced_nets.add(operand);
				}

				result_wire = NetFactory.makeNet(cast.getResultBus());
				produced_nets.add(result_wire);

				int result_width = result_wire.getWidth();

				/** unsigned value will be padded with zeros **/
				add(new Assign.Continuous(result_wire,
						new net.sf.openforge.verilog.pattern.SignExtend(
								operand, result_width)));
			}
		}

		@Override
		public Collection<Net> getConsumedNets() {
			return Collections.singleton((Net) operand);
		}

		@Override
		public Collection<Net> getProducedNets() {
			return produced_nets;
		}
	}

	public static final class Minus extends UnaryOpAssignment {
		public Minus(MinusOp minus) {
			super(minus);
		}

		@Override
		protected Expression makeOpExpression(Expression operand) {
			Expression subLeftExp = new net.sf.openforge.verilog.model.Unary.Negate(
					operand);
			Expression subRightExp = new HexNumber(new HexConstant("1",
					operand.getWidth()));
			return new net.sf.openforge.verilog.model.Math.Add(subLeftExp,
					subRightExp);
		}
	}

	public static final class Not extends UnaryOpAssignment {
		public Not(NotOp not) {
			super(not);
		}

		@Override
		protected Expression makeOpExpression(Expression operand) {
			return (new net.sf.openforge.verilog.model.Unary.Not(operand));
		}
	} // class Not

	public static final class Or extends UnaryOpAssignment {
		public Or(ReductionOrOp reductionOr) {
			super(reductionOr, false);
		}

		@Override
		protected Expression makeOpExpression(Expression operand) {
			return (new net.sf.openforge.verilog.model.Unary.Or(operand));
		}
	} // class Or

} // class UnaryOpAssignment
