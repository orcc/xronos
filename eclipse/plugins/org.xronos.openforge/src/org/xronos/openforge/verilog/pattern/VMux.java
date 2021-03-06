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
package org.xronos.openforge.verilog.pattern;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.xronos.openforge.lim.Port;
import org.xronos.openforge.lim.op.ShortcutIfElseOp;
import org.xronos.openforge.lim.primitive.EncodedMux;
import org.xronos.openforge.lim.primitive.Mux;
import org.xronos.openforge.verilog.model.Expression;
import org.xronos.openforge.verilog.model.Net;
import org.xronos.openforge.verilog.model.NetFactory;
import org.xronos.openforge.verilog.model.Statement;


/**
 * A VMux uses one of two mux implementations based on a LIM mux object. For a
 * two-input mux, a {@link Two2OneMux} is used, otherwise a {@link Many2OneMux}
 * is used.
 * <P>
 * 
 * Created: May 7, 2002
 * 
 * @author <a href="mailto:andreas.kollegger@xilinx.com">Andreas Kollegger</a>
 * @version $Id: VMux.java 18 2005-08-12 20:32:51Z imiller $
 */
public class VMux extends StatementBlock implements ForgePattern {

	Statement many2one_statement;

	private Set consumed_nets = new HashSet();
	private Net result;

	/**
	 * Constructs a VMux based on a LIM Mux.
	 * 
	 * @param mux
	 *            the LIM mux upon which to base the verilog implementation
	 */
	public VMux(Mux mux) {
		Set<Map.Entry<Port, Port>> entries = mux.getMuxEntries();
		Expression[][] pairs = new Expression[entries.size()][2];

		int i = 0;
		for (Map.Entry<Port, Port> me : entries) {
			Port select = me.getKey();
			Port data = me.getValue();
			pairs[i][0] = new PortWire(select);
			pairs[i][1] = new PortWire(data);
			consumed_nets.add(select);
			consumed_nets.add(data);
			i++;
		}

		assert (mux.getResultBus().getValue() != null) : "Mux's result Bus is missing a value.";
		result = NetFactory.makeNet(mux.getResultBus());

		if (entries.size() == 2) {
			add(new Two2OneMux(result, pairs));
		} else {
			assert entries.size() > 0;
			add(new Many2OneMux(result, pairs));
		}

		// if (entries.size() > 2)
		// {
		// add(new Many2OneMux(result, pairs));
		// }
		// else
		// {
		// add(new Two2OneMux(result, pairs));
		// }

	} // VMux()

	/**
	 * Construct a VMux based on a LIM ShortcutIfElseOp.
	 */
	public VMux(ShortcutIfElseOp sifOp) {
		result = NetFactory.makeNet(sifOp.getResultBus());
		PortWire sel = new PortWire(sifOp.getDataPorts().get(0));
		PortWire dataA = new PortWire(sifOp.getDataPorts().get(1));
		PortWire dataB = new PortWire(sifOp.getDataPorts().get(2));
		consumed_nets.add(sel);
		consumed_nets.add(dataA);
		consumed_nets.add(dataB);
		add(new Two2OneMux(result, sel, dataA, dataB));
	}

	/**
	 * Construct a VMux for a 2-input encoded mux
	 * 
	 * @param emux
	 *            an {@link EncodedMux}
	 */
	public VMux(EncodedMux emux) {
		assert emux.getDataPorts().size() == 2;
		result = NetFactory.makeNet(emux.getResultBus());
		PortWire sel = new PortWire(emux.getSelectPort());
		PortWire lsb = new PortWire(emux.getDataPorts().get(0));
		PortWire msb = new PortWire(emux.getDataPorts().get(1));
		consumed_nets.add(sel);
		consumed_nets.add(lsb);
		consumed_nets.add(msb);
		add(new Two2OneMux(result, sel, msb, lsb));
	}

	/**
	 * Provides the collection of Nets which this statement of verilog uses as
	 * input signals.
	 */
	@Override
	public Collection getConsumedNets() {
		return consumed_nets;
	}

	/**
	 * Provides the collection of Nets which this statement of verilog produces
	 * as output signals.
	 */
	@Override
	public Collection<Net> getProducedNets() {
		return Collections.singleton(result);
	}

} // VMux

