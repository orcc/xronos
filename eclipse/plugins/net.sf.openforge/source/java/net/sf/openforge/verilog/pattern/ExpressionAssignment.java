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
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import net.sf.openforge.lim.Bus;
import net.sf.openforge.lim.Component;
import net.sf.openforge.lim.Exit;
import net.sf.openforge.lim.Port;
import net.sf.openforge.verilog.model.Assign;
import net.sf.openforge.verilog.model.Expression;
import net.sf.openforge.verilog.model.Net;
import net.sf.openforge.verilog.model.NetFactory;
import net.sf.openforge.verilog.model.Wire;

/**
 * A ExpressionAssignment is a verilog assignment statement, which combines a
 * LIM {@link Component} based Expression with an assignment to a wire.
 * <P>
 * 
 * Created: Tue Mar 12 09:46:58 2002
 * 
 * @author <a href="mailto:andreas.kollegger@xilinx.com">Andy Kollegger</a>
 * @version $Id: ExpressionAssignment.java 2 2005-06-09 20:00:48Z imiller $
 */

public abstract class ExpressionAssignment extends StatementBlock implements
		ForgePattern {

	List operands = new ArrayList();
	Wire resultWire;

	public ExpressionAssignment(Component component) {
		List ports = component.getDataPorts();
		assert ports.size() >= 1 : "Expression assignmnet must have at least 1 operand";

		for (Iterator it = ports.iterator(); it.hasNext();) {
			Port r_port = (Port) it.next();
			assert (r_port.isUsed()) : "operand port in math operation is set to unused.";
			// Bus r_bus = r_port.getBus();
			// assert (r_bus != null) : "operand port " + r_port +
			// ") in math operation not attached to a bus.";
			// assert (r_bus.getSource().getValue() != null) :
			// "operand port's source bus (" + r_bus.getSource() +
			// ") has no value.";
			assert (r_port.getValue() != null) : "Right operand port in math operation does not have a value.";
			operands.add(new PortWire(r_port));
		}

		Exit ex = component.getExit(Exit.DONE);

		/*
		 * When used as the left hand side of an assignment, a Wire should
		 * always return its identifier when lexicalified. A BusWire won't do
		 * this; if it's constant, it will return a representation of the
		 * constant value. Since you can't assign to a constant, this won't
		 * work. Therefore, use the BusWire constructor as a convenience to get
		 * the identifier and width of the Bus, but use a raw Wire as the actual
		 * left hand side.
		 */
		Net busWire = NetFactory.makeNet((Bus) ex.getDataBuses().iterator()
				.next());
		this.resultWire = new Wire(busWire.getIdentifier(), busWire.getWidth());

		if (_pattern.db)
			_pattern.d.ln("Expression Assignment: " + busWire.getIdentifier());
		add(new Assign.Continuous(resultWire, makeExpression(operands)));
	}

	protected abstract Expression makeExpression(List operands);

	public Collection getConsumedNets() {
		Set consumed = new HashSet();
		for (Iterator it = operands.iterator(); it.hasNext();) {
			consumed.addAll(((Expression) it.next()).getNets());
		}
		return consumed;
	}

	public Collection getProducedNets() {
		return Collections.singleton(resultWire);
	}

} // class ExpressionAssignment
