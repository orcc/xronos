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

import org.xronos.openforge.lim.Bus;
import org.xronos.openforge.lim.Call;
import org.xronos.openforge.lim.IPCoreCall;
import org.xronos.openforge.lim.Port;
import org.xronos.openforge.util.naming.ID;
import org.xronos.openforge.verilog.model.Expression;
import org.xronos.openforge.verilog.model.ModuleInstance;
import org.xronos.openforge.verilog.model.Net;
import org.xronos.openforge.verilog.model.NetFactory;

/**
 * An IPCoreCallInstance is block of statements containing a ModuleInstance
 * based upon a LIM {@link IPCoreCall}. It does not extend ModuleInstance but
 * creates one. This is because there is a bus re-naming that must happen for
 * each outbuf or output pin that the call output connects to. This is
 * accomplished by creating a new assign statement for each.
 * 
 * Created: Tue Mar 12 09:46:58 2002
 * 
 * @author ysyu
 * @version $Id: IPCoreCallInstance.java 2 2005-06-09 20:00:48Z imiller $
 */

public class IPCoreCallInstance extends ForgeStatement implements ForgePattern {

	/**
	 * Construct an IPCoreCallInstance based on a {@link IPCoreCall}, and using
	 * a Bus-Net map for externally defined signals. The Buses for the Module
	 * must be defined in the external bus-net map.
	 * 
	 * @param call
	 *            the Call which is being instantiated
	 * @param external_netmap
	 *            a Bus-Net map of external signals
	 */
	public IPCoreCallInstance(Call call) {
		super();

		ModuleInstance mi = new ModuleInstance(ID.toVerilogIdentifier(ID
				.showLogical(call.getProcedure())), ID.toVerilogIdentifier(ID
				.showLogical(call)));
		add(mi);

		// Connect call control ports and data ports
		// Iterate through call ports
		for (Port cport : call.getPorts()) {
			if (cport.isUsed()) {
				assert (cport.getValue() != null) : "Missing value information on port "
						+ cport + " in call " + call.toString();

				Port pport = call.getProcedurePort(cport); // get corresponding
															// Procedure port
				Bus pportBus = pport.getPeer(); // get procedure inbuf bus

				assert (pport != null) : "Missing associated procedure port for port "
						+ cport + " on call " + call.toString();
				assert (pportBus != null) : "Missing related procedure port's bus for port "
						+ cport + " on call " + call.toString();
				assert (pportBus.getValue() != null) : "Missing value information on bus "
						+ pportBus + " in call " + call.toString();

				// Upper level Module
				Expression input_wire = new PortWire(cport);

				// Called Module
				Net module_port = NetFactory.makeNet(pportBus);

				// Connect them, connected ports or no connect ports
				assert (module_port != null && input_wire != null) : "Missing Verilog nets or input wire for call: "
						+ call.toString();
				if (((IPCoreCall) call).getNoConnectPorts().contains(cport)) {
					mi.noConnect(module_port);
				} else {
					mi.connect(module_port, input_wire);
				}
			}
		}

		// Connecting call buses
		for (Bus bus : call.getBuses()) {
			if (bus.isUsed()) {
				Net output_wire = NetFactory.makeNet(bus);

				produced_nets.add(output_wire);

				Net module_port = new BusOutput(call.getProcedureBus(bus));

				assert (module_port != null && output_wire != null) : "Missing Verilog nets or output wire for call: "
						+ call.toString();

				mi.connect(module_port, output_wire);
			}
		}
	}
}
