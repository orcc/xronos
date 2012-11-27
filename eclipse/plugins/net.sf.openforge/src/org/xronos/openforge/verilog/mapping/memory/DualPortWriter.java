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

package org.xronos.openforge.verilog.mapping.memory;

import java.util.BitSet;
import java.util.HashSet;
import java.util.Set;

import org.xronos.openforge.lim.memory.MemoryBank;
import org.xronos.openforge.lim.memory.MemoryBank.BankPort;
import org.xronos.openforge.verilog.model.Expression;
import org.xronos.openforge.verilog.model.Input;
import org.xronos.openforge.verilog.model.ModuleInstance;
import org.xronos.openforge.verilog.model.Net;
import org.xronos.openforge.verilog.model.NetFactory;
import org.xronos.openforge.verilog.model.Output;
import org.xronos.openforge.verilog.model.Wire;
import org.xronos.openforge.verilog.pattern.MemoryInstance;
import org.xronos.openforge.verilog.pattern.PortWire;


/**
 * DualPortWriter is the super class for all Dual Ported memory writers and
 * allocates ports for each input/output, is responsible for instantiation of
 * the module, and contains utility code for cloning out the necessary RAM
 * primitives and assigning initial values to each.
 * 
 * <p>
 * Created: Tue Dec 3 12:58:53 2002
 * 
 * @author imiller, last modified by $Author: imiller $
 * @version $Id: DualPortWriter.java 493 2007-06-15 18:52:19Z imiller $
 */
public abstract class DualPortWriter extends VerilogMemory {

	private static final String aPort = "A";
	private static final String bPort = "B";

	protected Input clkPort = new Input(clk, 1);
	protected MemoryPortDef mpA;
	protected MemoryPortDef mpB;

	private String moduleName;
	private int memory_instance_id = 0;

	public DualPortWriter(MemoryBank memBank) {
		super(memBank);

		MemoryBank.BankPort pA = getPortA(memBank);
		MemoryBank.BankPort pB = getPortB(memBank);

		mpA = new MemoryPortDef(pA, aPort, 0);
		mpB = new MemoryPortDef(pB, bPort, 1);

		// this.moduleName = "forge_dualport_memory_" + getDepth() + "x" +
		// getDataWidth() + "_"+ memory_module_id++;
		// this.moduleName = "forge_dualport_memory_" + getDepth() + "x" +
		// getDataWidth() + "_"+ memory_module_id++;
		// this.moduleName = memBank.showIDLogical() + "_"+
		// memory_instance_id++;
		moduleName = memBank.showIDLogical() + "_" + memory_module_id++;
	}

	private MemoryBank.BankPort getPortA(MemoryBank bank) {
		assert bank.getBankPorts().size() == 2;
		MemoryBank.BankPort pA = bank.getBankPorts().get(0);
		MemoryBank.BankPort pB = bank.getBankPorts().get(1);
		// If the A port is read only and this is going into a LUT
		// then swap the ports if the B port isn't read only, so the
		// read/write port always ends up on the A port.
		if (bank.getImplementation().isLUT() && pB.isWrite()) {
			return pB;
		}
		return pA;
	}

	private MemoryBank.BankPort getPortB(MemoryBank bank) {
		assert bank.getBankPorts().size() == 2;
		Set<BankPort> ports = new HashSet<BankPort>(bank.getBankPorts());
		ports.remove(getPortA(bank));
		assert ports.size() == 1;
		return ports.iterator().next();
	}

	@Override
	public String getName() {
		return moduleName;
	}

	/**
	 * Instantiates a memory access in the form of {@link ModuleInstance}
	 * 
	 * @return a value of type 'ModuleInstance'
	 */
	@Override
	public ModuleInstance instantiate(MemoryBank bank) {
		MemoryInstance memoryInstance = new MemoryInstance(getName(), getName()
				+ "_instance" + memory_instance_id++);

		assert bank.getBankPorts().size() == getMemBank().getBankPorts().size();

		// Net clkWireA = new BusWire(implementationA.getClockPort().getBus());
		// Net clkWire = new BusWire(getMemBank().getClockPort().getBus());
		Net clkWire = NetFactory.makeNet(bank.getClockPort().getBus());
		memoryInstance.connect(clkPort, clkWire);

		MemoryBank.BankPort pA = getPortA(bank);
		MemoryBank.BankPort pB = getPortB(bank);

		MemoryPortDef defA = new MemoryPortDef(pA, "A", 0);
		MemoryPortDef defB = new MemoryPortDef(pB, "B", 1);

		connectPort(memoryInstance, defA);
		connectPort(memoryInstance, defB);

		return memoryInstance;
	}

	private void connectPort(MemoryInstance instance, MemoryPortDef def) {
		MemoryBank.BankPort port = def.getBankPort();

		Net enWire = new PortWire(port.getEnablePort());
		instance.connect(def.ren, enWire);

		if (def.writes()) {
			Net weWire = new PortWire(port.getWriteEnablePort());
			instance.connect(def.wen, weWire);

			Net diWire = new PortWire(port.getDataInPort());
			instance.connect(def.din, diWire);
		}

		Expression addrWire = new PortWire(port.getAddressPort());
		instance.connect(def.adr, addrWire);

		if (def.reads()) {
			Net doWire = NetFactory.makeNet(port.getDataOutBus());
			instance.connect(def.dout, doWire);
			instance.addProducedNet(doWire);
		}

		// The done is now calculated in the StructuralMemory.
		// Net doneOutWireA = new
		// BusWire(implementationA.getExit(com.xilinx.hllc.lim.Exit.DONE).getDoneBus());
		// instance.connect(mpA.done, doneOutWireA);
		// instance.addProducedNet(doneOutWireA);
		instance.noConnect(def.done);
	}

	/**
	 * Utility method for copying out the rams and populating each with the
	 * appropriate initial bits.
	 * 
	 * @param type
	 *            the {@link DualPortRam} that will be cloned to populate the
	 *            implementation.
	 * @param initVals
	 *            a 'BitSet' of the initial values for the impelmentation
	 * @param portA
	 *            the {@link MemoryPortDef} for port A.
	 * @param portB
	 *            the {@link MemoryPortDef} for port B.
	 * @return a 'RamImplementation' containing the array of implementation Rams
	 *         and any extra bit wires.
	 */
	protected RamImplementation getImplementationRams(DualPortRam type,
			MemoryPortDef portA, MemoryPortDef portB) {
		int resultWidth = (int) java.lang.Math.ceil((double) getDataWidth()
				/ (double) type.getWidth());
		int resultDepth = (int) java.lang.Math.ceil((double) getDepth()
				/ (double) type.getDepth());

		DualPortRam[][] ram_array = new DualPortRam[resultWidth][resultDepth];
		Net[] extra_douta_wires = new Net[resultDepth];
		Net[] extra_doutb_wires = new Net[resultDepth];
		for (int w = 0; w < resultWidth; w++) {
			for (int d = 0; d < resultDepth; d++) {
				DualPortRam new_element;

				new_element = (DualPortRam) type.clone();

				ram_array[w][d] = new_element;

				// Configure names while we are at it
				new_element.setClkName(clk, portA.whichPort());
				new_element.setClkName(clk, portB.whichPort());
				new_element.setOeName(portA.getRENName(), portA.whichPort());
				new_element.setOeName(portB.getRENName(), portB.whichPort());

				if (portA.writes()) {
					new_element.setWeName("wea_" + d, portA.whichPort());
				}
				if (portB.writes()) {
					new_element.setWeName("web_" + d, portB.whichPort());
				}

				// The element will never grab more address bits that
				// its largest will allow, so don't worry about giving
				// them all to it.
				new_element.setAddr(portA.getADRName(), getAddrWidth(), 0,
						getAddrWidth() == 1, portA.whichPort());
				new_element.setAddr(portB.getADRName(), getAddrWidth(), 0,
						getAddrWidth() == 1, portB.whichPort());

				// We don't give the memory a signal if we aren't
				// given one, then it will write out zeros for us
				if (portA.writes()) {
					new_element.setDataInName(portA.getDINName(),
							portA.whichPort());
				}
				if (portB.writes()) {
					new_element.setDataInName(portB.getDINName(),
							portB.whichPort());
				}
				if (portA.reads()) {
					new_element.setDataOutName("pre_douta_" + d,
							portA.whichPort());
					new_element.setDataOutExtraName("extrasa_" + d,
							portA.whichPort());
				}
				if (portB.reads()) {
					new_element.setDataOutName("pre_doutb_" + d,
							portB.whichPort());
					new_element.setDataOutExtraName("extrasb_" + d,
							portB.whichPort());
				}

				new_element.setDataInStartBit(w * new_element.getWidth(),
						portA.whichPort());
				new_element.setDataInStartBit(w * new_element.getWidth(),
						portB.whichPort());
				new_element.setDataOutStartBit(w * new_element.getWidth(),
						portA.whichPort());
				new_element.setDataOutStartBit(w * new_element.getWidth(),
						portB.whichPort());

				if (((w + 1) * new_element.getWidth()) <= getDataWidth()) {
					new_element.setDataAttachWidth(new_element.getWidth(),
							portA.whichPort());
					new_element.setDataAttachWidth(new_element.getWidth(),
							portB.whichPort());
				} else {
					int totalBits = new_element.getWidth() * resultWidth;
					int spareBits = totalBits - getDataWidth();

					new_element.setDataAttachWidth(new_element.getWidth()
							- spareBits, portA.whichPort());
					new_element.setDataAttachWidth(new_element.getWidth()
							- spareBits, portB.whichPort());

					// We have spare bits, store the extra name to
					// declare later. Not used???
					if (portA.reads()) {
						extra_douta_wires[d] = new Wire("extrasa_" + d,
								spareBits);
					}
					if (portB.reads()) {
						extra_doutb_wires[d] = new Wire("extrasb_" + d,
								spareBits);
					}
				}

				new_element.setDataScalar(getDataWidth() == 1,
						portA.whichPort());
				new_element.setDataScalar(getDataWidth() == 1,
						portB.whichPort());
			}
		}

		// Pass out the initialization values, assume the data bus is
		// packed leaving all the unconnected bits at the top of the
		// array
		// BitSet initVals = getMemory().getInitValuesAsBitSet();
		BitSet initVals = getInitValuesAsBitSet();
		int currentBit = 0;

		for (int d = 0; d < resultDepth; d++) {
			for (int ed = 0; ed < ram_array[0][0].getDepth(); ed++) {
				for (int w = 0; w < resultWidth; w++) {
					Ram element = ram_array[w][d];
					for (int b = 0; b < element.getWidth(); b++) {
						if ((w * element.getWidth() + b) < getDataWidth()
								&& (d * element.getDepth() + ed) < getDepth()) {
							element.setNextInitBit(initVals.get(currentBit) ? 1
									: 0);
							currentBit++;
						} else {
							// We are into fill bits
							element.setNextInitBit(0);
						}
					}
				}
			}
		}

		return new RamImplementation(ram_array, extra_douta_wires,
				extra_doutb_wires);
	}

	class RamImplementation {
		DualPortRam[][] rams;
		Net[] spareA;
		Net[] spareB;

		public RamImplementation(DualPortRam[][] ramArray, Net[] spA, Net[] spB) {
			rams = ramArray;
			spareA = spA;
			spareB = spB;
		}

		public DualPortRam[][] getRamArray() {
			return rams;
		}

		public Net[] getSpareA() {
			return spareA;
		}

		public Net[] getSpareB() {
			return spareB;
		}
	}

	class MemoryPortDef {
		Input ren;
		Input wen;
		Input adr;
		Input din;
		Output dout;
		Output done;

		private MemoryBank.BankPort mp;
		private int which;

		public MemoryPortDef(MemoryBank.BankPort mp, String id, int which) {
			this.mp = mp;
			this.which = which;

			ren = new Input(VerilogMemory.ren + id, 1);
			wen = new Input(VerilogMemory.wen + id, 1);
			adr = new Input(VerilogMemory.adr + id, getAddrWidth());
			din = new Input(VerilogMemory.din + id, getDataWidth());
			dout = new Output(VerilogMemory.dout + id, getDataWidth());
			done = new Output(VerilogMemory.done + id, 1);
		}

		public boolean reads() {
			return mp.isRead();
		}

		public boolean writes() {
			return mp.isWrite();
		}

		public int whichPort() {
			return which;
		}

		public MemoryBank.BankPort getBankPort() {
			return mp;
		}

		public String getRENName() {
			return ren.getIdentifier().getToken();
		}

		public String getWENName() {
			return wen.getIdentifier().getToken();
		}

		public String getADRName() {
			return adr.getIdentifier().getToken();
		}

		public String getDINName() {
			return din.getIdentifier().getToken();
		}

		public String getDOUTName() {
			return dout.getIdentifier().getToken();
		}

		public String getDONEName() {
			return done.getIdentifier().getToken();
		}

		public String getWriteMode() {
			return mp.getWriteMode();
		}

	}
}// DualPortWriter
