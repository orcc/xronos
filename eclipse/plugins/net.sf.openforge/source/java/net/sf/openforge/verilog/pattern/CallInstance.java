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

import java.util.Iterator;

import net.sf.openforge.lim.Bus;
import net.sf.openforge.lim.Call;
import net.sf.openforge.lim.Port;
import net.sf.openforge.util.naming.ID;
import net.sf.openforge.verilog.model.Expression;
import net.sf.openforge.verilog.model.ModuleInstance;
import net.sf.openforge.verilog.model.Net;
import net.sf.openforge.verilog.model.NetFactory;

/**
 * A CallInstance is block of statements containing a ModuleInstance based upon
 * a LIM {@link Call}. It does not extend ModuleInstance but creates one. This
 * is because there is a bus re-naming that must happen for each outbuf or
 * output pin that the call output connects to. This is accomplished by creating
 * a new assign statement for each.
 * <P>
 * 
 * Created: Tue Mar 12 09:46:58 2002
 * 
 * @author <a href="mailto:andreas.kollegger@xilinx.com">Andy Kollegger</a>
 * @version $Id: CallInstance.java 23 2005-09-09 18:45:32Z imiller $
 */

public class CallInstance extends ForgeStatement implements ForgePattern {

	// private static int call_instanceCount = 0;

	/**
	 * Construct a CallInstance based on a {@link Call}, and using a Bus-Net map
	 * for externally defined signals. The Buses for the Module must be defined
	 * in the external bus-net map.
	 * 
	 * @param call
	 *            the Call which is being instantiated
	 * @param external_netmap
	 *            a Bus-Net map of external signals
	 */
	public CallInstance(Call call) {
		super();

		ModuleInstance mi = new ModuleInstance(ID.toVerilogIdentifier(ID
				.showLogical(call.getProcedure())), ID.toVerilogIdentifier(ID
				.showLogical(call)));
		this.add(mi);

		// Connect call control ports and data ports
		// Iterate through call ports
		for (Port cport : call.getPorts()) {

			if (cport.isUsed()) {
				// Bus cportBus = cport.getBus(); // call port bus
				// assert (cportBus != null) :
				// "Missing connection to call port " + cport + " on call " +
				// call.toString();
				// assert (cportBus.getSource().getValue() != null) :
				// "Missing value information on bus " + cportBus.getSource() +
				// " in call " + call.toString();
				assert (cport.getValue() != null) : "Missing value information on port "
						+ cport + " in call " + call.show();

				Port pport = call.getProcedurePort(cport); // get corresponding
															// Procedure port
				assert (pport != null) : "Missing associated procedure port for port "
						+ cport + " on call " + call.toString();

				Bus pportBus = pport.getPeer(); // get procedure inbuf bus

				assert (pportBus != null) : "Missing related procedure port's bus for port "
						+ cport + " on call " + call.toString();
				// assert (pportBus.getSource().getValue() != null) :
				// "Missing value information on bus " + pportBus.getSource() +
				// " in call " + call.toString();
				assert (pportBus.getValue() != null) : "Missing value information on bus "
						+ pportBus + " in call " + call.toString();

				// Upper level Module
				// Wire input_wire = new BusWire(cportBus);
				Expression input_wire = new PortWire(cport);

				// Called Module
				Net module_port = NetFactory.makeNet(pportBus);

				// Connect them
				assert (module_port != null && input_wire != null) : "Missing Verilog nets or input wire for call: "
						+ call.toString();

				mi.connect(module_port, input_wire);
			}
		}

		// Connecting call buses
		for (Iterator busIter = call.getBuses().iterator(); busIter.hasNext();) {
			Bus bus = (Bus) busIter.next();
			if (bus.isUsed()) {
				Net output_wire = NetFactory.makeNet(bus);

				produced_nets.add(output_wire);

				Net module_port = new BusOutput(call.getProcedureBus(bus));
				assert (module_port != null && output_wire != null) : "Missing Verilog nets or output wire for call: "
						+ call.toString();
				mi.connect(module_port, output_wire);
			}
		}

	} // CallInstance

} // class CallInstance
