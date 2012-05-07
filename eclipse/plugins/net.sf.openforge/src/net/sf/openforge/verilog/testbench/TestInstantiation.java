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

package net.sf.openforge.verilog.testbench;

import java.util.Set;

import net.sf.openforge.lim.Bus;
import net.sf.openforge.lim.Design;
import net.sf.openforge.lim.Exit;
import net.sf.openforge.lim.InputPin;
import net.sf.openforge.lim.Pin;
import net.sf.openforge.lim.Port;
import net.sf.openforge.lim.Task;
import net.sf.openforge.util.naming.ID;
import net.sf.openforge.verilog.model.Input;
import net.sf.openforge.verilog.model.Module;
import net.sf.openforge.verilog.model.ModuleInstance;
import net.sf.openforge.verilog.model.Output;
import net.sf.openforge.verilog.model.PortConnection;
import net.sf.openforge.verilog.model.Wire;

/**
 * TestInstantiation is the instantiation of the design being tested and
 * connecting the ports up to the correct logic (including clk and reset)
 * 
 * <p>
 * Created: Thu Jan 9 14:16:45 2003
 * 
 * @author imiller, last modified by $Author: imiller $
 * @version $Id: TestInstantiation.java 282 2006-08-14 21:25:33Z imiller $
 */
public class TestInstantiation {

	private final Design design;
	private final Set<TaskHandle> taskHandles;

	public TestInstantiation(Design design, Set<TaskHandle> taskHandles) {
		this.design = design;
		this.taskHandles = taskHandles;
	}

	/**
	 * Creates the instantiation and connects up all the ports/buses.
	 */
	public void stateLogic(Module module, StateMachine mach) {
		ModuleInstance instance = new ModuleInstance(getVerilogName(design),
				"test");

		/*
		 * Connect clock and reset if needed.
		 */
		for (Design.ClockDomain domain : design.getAllocatedClockDomains()) {
			instance.add(new PortConnection(new Input(getVerilogName(domain
					.getClockPin()), 1), mach.getClock()));
			if (domain.getResetPin() != null) {
				instance.add(new PortConnection(new Input(getVerilogName(domain
						.getResetPin()), 1), mach.getReset()));
			}
		}

		for (TaskHandle th : taskHandles) {
			Task task = th.getTask();
			for (Port port : task.getCall().getPorts()) {
				InputPin pin = (InputPin) design.getPin(port);
				Wire portWire = th.getWireForConnection(port);
				if (portWire == null) {
					continue;
				}

				// Use the Port's bus to agree with InputPinPort and
				// VerilogNamer, but output pins use the actual pin.
				PortConnection pc = new PortConnection(new Input(
						getVerilogName(pin.getBus()), pin.getWidth()), portWire);
				instance.add(pc);
			}

			for (Bus bus : task.getCall().getExit(Exit.DONE).getBuses()) {
				Wire busWire = th.getWireForConnection(bus);
				if (busWire == null) {
					continue;
				}

				Pin pin = design.getPin(bus);
				PortConnection pc = new PortConnection(new Output(
						getVerilogName(pin), pin.getWidth()), busWire);
				instance.add(pc);
			}

			Pin goPin = design.getPin(task.getCall().getGoPort());
			if (goPin != null) {
				instance.add(new PortConnection(new Input(
						getVerilogName(((InputPin) goPin).getBus()), 1), th
						.getGoWire()));
			}

			Pin donePin = design.getPin(task.getCall().getExit(Exit.DONE)
					.getDoneBus());
			if (donePin != null) {
				instance.add(new PortConnection(new Output(
						getVerilogName(donePin), 1), th.getDoneWire()));
			}
		}

		module.state(instance);
	}

	private static String getVerilogName(Object obj) {
		return ID.toVerilogIdentifier(ID.showLogical(obj));
	}

}// TestInstantiation
