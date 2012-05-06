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
import java.util.List;
import java.util.Set;

import net.sf.openforge.lim.Bus;
import net.sf.openforge.lim.Component;
import net.sf.openforge.lim.Port;
import net.sf.openforge.verilog.model.Assign;
import net.sf.openforge.verilog.model.Expression;
import net.sf.openforge.verilog.model.Net;
import net.sf.openforge.verilog.model.NetFactory;

/**
 * A MultiPortOpAssignment is a verilog assignment statement, based on a
 * {@link Component} with an unknown number of data ports which assigns the
 * result to a wire.
 * <P>
 * 
 * <p>
 * Created: Wed Nov 6 10:33:24 2002
 * 
 * @author imiller, last modified by $Author: imiller $
 * @version $Id: MultiPortOpAssignment.java 2 2005-06-09 20:00:48Z imiller $
 */

public abstract class MultiPortOpAssignment extends StatementBlock implements
		ForgePattern {

	List<Expression> expressions;
	Net result_wire;

	public MultiPortOpAssignment(net.sf.openforge.lim.Operation op) {
		expressions = new ArrayList<Expression>();
		for (Port port : op.getDataPorts()) {
			assert port.isUsed() : (op.getDataPorts().indexOf(port))
					+ " operand port in math operation is set to unused.";
			// Bus bus = port.getBus();
			// if (bus == null)
			// continue;
			// assert (bus != null) : (op.getDataPorts().indexOf(port)) +
			// " operand port in math operation not attached to a bus.";
			expressions.add(new PortWire(port));
		}

		Bus resultBus = Component.getDataBus(op);
		result_wire = NetFactory.makeNet(resultBus);

		add(new Assign.Continuous(result_wire, makeOpExpression(expressions)));
	}

	protected abstract Expression makeOpExpression(List<Expression> expressions);

	@Override
	public Collection<Net> getConsumedNets() {
		Set<Net> consumed = new HashSet<Net>();
		for (Expression expresion : expressions) {
			consumed.addAll(expresion.getNets());
		}
		return consumed;
	}

	@Override
	public Collection<Net> getProducedNets() {
		return Collections.singleton(result_wire);
	}

} // class MultiPortOpAssignment
