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

import net.sf.openforge.lim.TriBuf;
import net.sf.openforge.verilog.model.Assign;
import net.sf.openforge.verilog.model.BinaryConstant;
import net.sf.openforge.verilog.model.BinaryNumber;
import net.sf.openforge.verilog.model.Conditional;
import net.sf.openforge.verilog.model.Expression;
import net.sf.openforge.verilog.model.Group;
import net.sf.openforge.verilog.model.Net;
import net.sf.openforge.verilog.model.NetFactory;

/**
 * A simple conditional which output's z's if the enable is false.
 */
public class TriBufOp extends StatementBlock implements ForgePattern {

	Set<Net> produced_nets = new HashSet<Net>();
	Set<Net> consumed_nets = new HashSet<Net>();

	/**
	 * Constructs a TriBuf based on either of two selection Nets, which specify
	 * which input data Net will be assigned to the result Net.
	 * 
	 * @param result
	 *            The result wire to which the 2-1 mux will be assigned
	 * @param sel
	 *            Expression which selects data_a if true and data_b if false as
	 *            the output value
	 * @param data_a
	 *            The first input value
	 * @param data_b
	 *            The second input value
	 */
	public TriBufOp(TriBuf tbuf) {
		Expression input_operand = new PortWire(tbuf.getInputPort());
		Expression enable_operand = new PortWire(tbuf.getEnablePort());

		consumed_nets.add((Net) input_operand);
		consumed_nets.add((Net) enable_operand);

		Net result_wire = NetFactory.makeNet(tbuf.getResultBus());
		/*
		 * The boolean test expression is grouped in parens because ModelTech
		 * can't parse it when it's a literal value (ie, "1 'h 1?").
		 */
		Conditional conditional = new Conditional(new Group(enable_operand),
				input_operand, new BinaryNumber(new BinaryConstant("z", tbuf
						.getResultBus().getValue().getSize())));

		produced_nets.add(result_wire);

		add(new Assign.Continuous(result_wire, conditional));
	}

	@Override
	public Collection<Net> getConsumedNets() {
		return consumed_nets;
	}

	/**
	 * Provides the collection of Nets which this statement of verilog produces
	 * as output signals.
	 */
	@Override
	public Collection<Net> getProducedNets() {
		return produced_nets;
	}

} // TriBuf

