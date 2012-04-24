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

import net.sf.openforge.lim.Port;
import net.sf.openforge.lim.op.BinaryOp;
import net.sf.openforge.verilog.model.Assign;
import net.sf.openforge.verilog.model.Expression;
import net.sf.openforge.verilog.model.Net;
import net.sf.openforge.verilog.model.NetFactory;

/**
 * A BinaryOpAssignment is a verilog assignment statement, based on a
 * {@link BinaryOp}, which assigns the result to a wire.
 * <P>
 * 
 * Created: Tue Mar 12 09:46:58 2002
 * 
 * @author <a href="mailto:andreas.kollegger@xilinx.com">Andy Kollegger</a>
 * @version $Id: BinaryOpAssignment.java 2 2005-06-09 20:00:48Z imiller $
 */

public abstract class BinaryOpAssignment extends StatementBlock implements
		ForgePattern {

	Expression left_operand;
	Expression right_operand;
	Net result_wire;

	public BinaryOpAssignment(BinaryOp bo) {
		Iterator ports = bo.getDataPorts().iterator();
		Port l_port = (Port) ports.next();
		assert (l_port.isUsed()) : "Left operand port in math operation is set to unused.";
		// Bus l_bus = l_port.getBus();
		// assert (l_bus != null) :
		// "Left operand port in math operation not attached to a bus.";
		assert (l_port.getValue() != null) : "Left operand port in math operation does not have a value.";
		left_operand = new PortWire(l_port);

		Port r_port = (Port) ports.next();
		assert (r_port.isUsed()) : "Right operand port in math operation is set to unused.";
		// Bus r_bus = r_port.getBus();
		// assert (r_bus != null) :
		// "Right operand port in math operation not attached to a bus.";
		assert (r_port.getValue() != null) : "Right operand port in math operation does not have a value.";
		right_operand = new PortWire(r_port);

		result_wire = NetFactory.makeNet(bo.getResultBus());

		add(new Assign.Continuous(result_wire, makeOpExpression(left_operand,
				right_operand)));
	}

	protected BinaryOpAssignment() {
	}

	protected abstract Expression makeOpExpression(Expression left,
			Expression right);

	public Collection getConsumedNets() {
		Set consumed = new HashSet();
		consumed.addAll(left_operand.getNets());
		consumed.addAll(right_operand.getNets());
		return consumed;
	}

	public Collection getProducedNets() {
		return Collections.singleton(result_wire);
	}

} // class BinaryOpAssignment
