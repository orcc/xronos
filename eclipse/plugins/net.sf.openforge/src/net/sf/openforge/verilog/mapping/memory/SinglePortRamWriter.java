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

package net.sf.openforge.verilog.mapping.memory;

import java.util.BitSet;
import java.util.Collections;

import net.sf.openforge.app.EngineThread;
import net.sf.openforge.lim.CodeLabel;
import net.sf.openforge.lim.Latency;
import net.sf.openforge.lim.memory.MemoryBank;
import net.sf.openforge.util.XilinxDevice;
import net.sf.openforge.verilog.mapping.MappedModule;
import net.sf.openforge.verilog.mapping.MemoryMapper;
import net.sf.openforge.verilog.model.Always;
import net.sf.openforge.verilog.model.Assign;
import net.sf.openforge.verilog.model.Bitwise;
import net.sf.openforge.verilog.model.CaseBlock;
import net.sf.openforge.verilog.model.Comment;
import net.sf.openforge.verilog.model.Compare;
import net.sf.openforge.verilog.model.Decimal;
import net.sf.openforge.verilog.model.EventControl;
import net.sf.openforge.verilog.model.EventExpression;
import net.sf.openforge.verilog.model.Expression;
import net.sf.openforge.verilog.model.Group;
import net.sf.openforge.verilog.model.HexConstant;
import net.sf.openforge.verilog.model.HexNumber;
import net.sf.openforge.verilog.model.InlineComment;
import net.sf.openforge.verilog.model.Input;
import net.sf.openforge.verilog.model.Module;
import net.sf.openforge.verilog.model.ModuleInstance;
import net.sf.openforge.verilog.model.Net;
import net.sf.openforge.verilog.model.NetFactory;
import net.sf.openforge.verilog.model.Output;
import net.sf.openforge.verilog.model.ProceduralTimingBlock;
import net.sf.openforge.verilog.model.Register;
import net.sf.openforge.verilog.model.SequentialBlock;
import net.sf.openforge.verilog.model.Unary;
import net.sf.openforge.verilog.model.Wire;
import net.sf.openforge.verilog.pattern.MemoryInstance;
import net.sf.openforge.verilog.pattern.MemoryModule;
import net.sf.openforge.verilog.pattern.PortWire;

/**
 * SinglePortRamWriter.java
 * 
 * @version $Id: SinglePortRamWriter.java 538 2007-11-21 06:22:39Z imiller $
 */
public class SinglePortRamWriter extends VerilogMemory {

	protected Input clkPort = new Input(clk, 1);
	protected Input renPort = new Input(ren, 1);
	protected Input wenPort = new Input(wen, 1);
	protected Input adrPort;
	protected Input dinPort;
	protected Output doutPort;
	protected Output donePort = new Output(done, 1);

	private String moduleName;

	protected int memory_instance_id = 0;

	public SinglePortRamWriter(MemoryBank memory) {
		super(memory);
		adrPort = new Input(adr, getAddrWidth());
		dinPort = new Input(din, getDataWidth());
		doutPort = new Output(dout, getDataWidth());
		// this.moduleName = "forge_memory_" + getDepth() + "x" + getDataWidth()
		// + "_"+ memory_module_id++;
		moduleName = memory.showIDLogical() + "_" + memory_module_id++;
	}

	@Override
	public String getName() {
		return moduleName;
	}

	/**
	 * Retrieves the Latency of a read access to this memory implementation.
	 */
	protected Latency getReadLatency() {
		return getMemBank().getImplementation().getReadLatency();
	}

	/**
	 * Retrieves the Latency of a write access to this memory implementation.
	 */
	protected Latency getWriteLatency() {
		return getMemBank().getImplementation().getWriteLatency();
	}

	@Override
	public Module defineModule() {
		// OK, we are going to map to a memory configuration and hard
		// instantiate the primitives necessary along with
		// initialization values.

		XilinxDevice xd = EngineThread.getGenericJob().getPart(
				CodeLabel.UNSCOPED);

		// We assume speed mapping, but if the synth_opt flow is
		// verilog_area.opt then put area as the highest priority
		// boolean opt_for_speed = true;

		Ram match = getLowestCost(Ram.getMappers(xd, isLUT()));

		int result_width = (int) java.lang.Math.ceil((double) getDataWidth()
				/ (double) match.getWidth());
		int result_depth = (int) java.lang.Math.ceil((double) getDepth()
				/ (double) match.getDepth());

		// System.out.println("************* match: " + match.getName() +
		// " ***********************");

		// add the primitive we are instantiating to the mappedModule list
		String sim_include_file = MemoryMapper.SIM_INCLUDE_PATH
				+ match.getName() + ".v";
		String synth_include_file = MemoryMapper.SYNTH_INCLUDE_PATH
				+ "unisim_comp.v";
		MemoryModule memoryModule = new MemoryModule(getName(),
				Collections.singleton(new MappedModule(match.getName(),
						sim_include_file, synth_include_file)));

		// We need to clone out the array in preparation for configuration
		Ram[][] ram_array = new Ram[result_width][result_depth];
		Net[] extra_dout_wires = new Net[result_depth];

		for (int w = 0; w < result_width; w++) {
			for (int d = 0; d < result_depth; d++) {
				Ram new_element;
				new_element = (Ram) match.clone();
				ram_array[w][d] = new_element;

				// Configure names while we are at it
				new_element.setClkName(clk);
				new_element.setOeName(ren);
				new_element.setWeName("we_" + d);

				// The element will never grab more address bits that
				// its largest will allow, so don't worry about giving
				// them all to it.
				new_element.setAddr("ADDR", getAddrWidth(), 0,
						getAddrWidth() == 1);

				new_element.setDataInName("DIN");
				new_element.setDataOutName("pre_dout_" + d);
				new_element.setDataOutExtraName("extras_" + d);

				new_element.setDataInStartBit(w * new_element.getWidth());
				new_element.setDataOutStartBit(w * new_element.getWidth());

				if (((w + 1) * new_element.getWidth()) <= getDataWidth()) {
					new_element.setDataAttachWidth(new_element.getWidth());
				} else {
					int totalBits = new_element.getWidth() * result_width;
					int spareBits = totalBits - getDataWidth();

					new_element.setDataAttachWidth(new_element.getWidth()
							- spareBits);

					// We have spare bits, store the extra name to
					// declare later
					extra_dout_wires[d] = new Wire("extras_" + d, spareBits);
				}

				new_element.setDataScalar(getDataWidth() == 1);
			}
		}

		// Pass out the initialization values, assume the data bus is
		// packed leaving all the unconnected bits at the top of the
		// array
		BitSet initVals = getInitValuesAsBitSet();
		int currentBit = 0;

		for (int d = 0; d < result_depth; d++) {
			for (int ed = 0; ed < ram_array[0][0].getDepth(); ed++) {
				for (int w = 0; w < result_width; w++) {
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

		// boolean register_output = !ram_array[0][0].isDataOutputRegistered();

		memoryModule.addPort(clkPort);
		memoryModule.addPort(renPort);
		memoryModule.addPort(wenPort);
		memoryModule.addPort(adrPort);
		memoryModule.addPort(dinPort);
		memoryModule.addPort(doutPort);
		memoryModule.addPort(donePort);

		// Declare internal wires
		Net[] we = new Wire[result_depth];
		Net[] pre_dout = new Wire[result_depth];

		for (int d = 0; d < result_depth; d++) {
			we[d] = new Wire("we_" + d, 1);
			memoryModule.declare(we[d]);
			pre_dout[d] = new Wire("pre_dout_" + d, getDataWidth());
			memoryModule.declare(pre_dout[d]);

			if ((extra_dout_wires[d] != null)
					&& (extra_dout_wires[d].getIdentifier().getToken().length() > 0)) {
				memoryModule.declare(extra_dout_wires[d]);
			}
		}

		Register mux_out = new Register("mux_out", getDataWidth());
		memoryModule.declare(mux_out);

		// Write out the small amount of logic then the memory array
		int extra_address_bits = getAddrWidth()
				- ram_array[0][0].getLibAddressWidth();
		for (int d = 0; d < result_depth; d++) {
			if (result_depth > 1) {
				HexNumber hex_d = new HexNumber(new HexConstant(
						Integer.toHexString(d), extra_address_bits));
				Expression addr_equal = new Compare.Equals(
						adrPort.getRange(getAddrWidth() - 1, getAddrWidth()
								- extra_address_bits), hex_d);
				Expression we_and_addr_equal = new Bitwise.And(wenPort,
						new Group(addr_equal));
				memoryModule.state(new Assign.Continuous(we[d],
						we_and_addr_equal));
			} else {
				memoryModule.state(new Assign.Continuous(we[d], wenPort));
			}
		}

		final boolean registerLutRead = ((getMemBank().getImplementation()
				.getReadLatency().getMinClocks() > 0) && getMemBank()
				.getImplementation().isLUT());
		mergeResults(extra_address_bits, result_depth, getAddrWidth(),
				getDataWidth(), pre_dout, mux_out, memoryModule, adrPort,
				clkPort, getReadLatency(), registerLutRead, "");

		// Delay the read and/or write by 1 cycle depending on
		// implementation characteristics
		Expression renDone = renPort;
		Expression wenDone = wenPort;
		SequentialBlock doneBlock = new SequentialBlock();
		boolean writeAlways = false;
		if (getReadLatency().getMinClocks() > 0) {
			renDone = new Register("ren_done", 1);
			doneBlock.add(new Assign.NonBlocking((Register) renDone, renPort));
			writeAlways = true;
		}
		if (getWriteLatency().getMinClocks() > 0) {
			wenDone = new Register("wen_done", 1);
			doneBlock.add(new Assign.NonBlocking((Register) wenDone, wenPort));
			writeAlways = true;
		}
		if (writeAlways) {
			EventControl clkEvent = new EventControl(
					new EventExpression.PosEdge(clkPort));
			memoryModule.state(new Always(new ProceduralTimingBlock(clkEvent,
					doneBlock)));
		}

		memoryModule.state(new Assign.Continuous(doutPort, mux_out));

		// the ren is always asserted for either memory access, so in
		// the combinational read case we need to qualify it with the
		// !we signal so a write (ren & wen) doesn't generate a
		// combinational done signal
		if (getReadLatency().getMinClocks() == 0) {
			memoryModule
					.state(new Assign.Continuous(donePort, new Bitwise.Or(
							new Bitwise.And(renPort, new Unary.Not(wenPort)),
							wenDone)));
		} else {
			memoryModule.state(new Assign.Continuous(donePort, new Bitwise.Or(
					renDone, wenDone)));
		}

		debugContents(memoryModule);

		for (int c = 0; c < ram_array[0].length; c++) {
			for (int r = 0; r < ram_array.length; r++) {
				String mem_inst_comment = " Memory array element: COL: " + c
						+ ", ROW: " + r;
				memoryModule.state(new InlineComment(mem_inst_comment,
						Comment.SHORT));
				memoryModule.state(ram_array[r][c].initialize());
				memoryModule.state(ram_array[r][c].instantiate());
			}
		}

		return memoryModule;
	}

	/**
	 * This code generates the necessary logic to 'merge' the result values from
	 * each 'stacked' instantiated ram by muxing them together based on the MSB
	 * bits of the address. Note, however that if the read latency of the memory
	 * is > 0 we must delay the address by the correct number of cycles so that
	 * it aligns with the data coming out. This also breaks combinational paths
	 * through the memory.
	 * 
	 * @param extra_address_bits
	 *            a value of type 'int'
	 * @param result_depth
	 *            a value of type 'int'
	 * @param pre_dout
	 *            a value of type 'Net[]'
	 * @param mux_out
	 *            a value of type 'Register'
	 * @param memoryModule
	 *            a value of type 'MemoryModule'
	 */
	public static void mergeResults(int extra_address_bits, int result_depth,
			int adrWidth, int dataWidth, Net[] pre_dout, Register mux_out,
			MemoryModule memoryModule, Net adrPort, Net clkPort,
			Latency readLatency, boolean insertReadReg, String portId) {
		Net adrNet = adrPort;
		if (readLatency.getMinClocks() > 0) {
			// Register the Address wire so that the address is in
			// sync with the data coming out of the memory.
			// always(@posedge CLK)
			// adrReg <= addr;
			if (readLatency.getMinClocks() != 1)
				throw new UnsupportedOperationException(
						"Memory cannot have read latency > 1");

			Register adrReg = new Register(adrPort.getIdentifier().toString()
					+ "_reg", adrPort.getWidth());
			memoryModule.declare(adrReg);

			EventControl clkEvent = new EventControl(
					new EventExpression.PosEdge(clkPort));
			SequentialBlock seqBlock = new SequentialBlock();
			seqBlock.add(new Assign.NonBlocking(adrReg, adrPort));
			Always always = new Always(new ProceduralTimingBlock(clkEvent,
					seqBlock));
			memoryModule.state(always);
			adrNet = adrReg;
		}

		if (insertReadReg) {
			// XXX Think about this... we are explicitly making the
			// read of LUT based memories 'registered'. Not a bad
			// thing as this increased fmax. The warning is just an
			// annoyance and was intended to be just a reminder to us.
			// EngineThread.getGenericJob().warn("Adding registers to the data out port of a LUT based memory");

			// Define registers for each pre_dout, which come directly
			// from the RAM primitives. ie:
			// always @(posedge clk) begin
			// pre_dout0_reg <= pre_dout0;
			// ...
			// end

			// First, create the always block which will be populated.
			EventControl clkEvent = new EventControl(
					new EventExpression.PosEdge(clkPort));
			SequentialBlock seqBlock = new SequentialBlock();

			Net[] pre_dout_reg = new Net[pre_dout.length];
			for (int i = 0; i < pre_dout.length; i++) {
				Net inNet = pre_dout[i];
				Register reg = new Register(inNet.toString() + "_reg",
						inNet.getWidth());
				seqBlock.add(new Assign.NonBlocking(reg, inNet));
				pre_dout_reg[i] = reg;
			}

			Always always = new Always(new ProceduralTimingBlock(clkEvent,
					seqBlock));
			memoryModule.state(always);

			// Re-set the pre_dout nets to be the registered version now.
			pre_dout = pre_dout_reg;
		}

		if (result_depth > 1) {
			// We need an output mux ie:
			// case (addr or pre_dout*)
			// 0: dout <= pre_dout0;
			// 1: dout <= pre_dout1;
			// ...
			// endcase
			EventExpression event_list = new EventExpression(adrNet);

			for (int d = 0; d < result_depth; d++) {
				event_list.add(pre_dout[d]);
			}

			EventControl event_control = new EventControl(event_list);

			CaseBlock case_block = new CaseBlock(adrNet.getRange(adrWidth - 1,
					adrWidth - extra_address_bits));
			for (int d = 0; d < result_depth; d++) {
				Decimal case_value = new Decimal(d, extra_address_bits);
				case_block.add(case_value.toString(), new Assign.Blocking(
						mux_out, pre_dout[d]));
			}
			HexConstant unknown = new HexConstant("0", dataWidth);
			case_block.add("default", new Assign.Blocking(mux_out,
					new HexNumber(unknown)));

			SequentialBlock sequential_block = new SequentialBlock(case_block);

			Always always = new Always(new ProceduralTimingBlock(event_control,
					sequential_block));
			memoryModule.state(always);
		} else {
			// We need just an assign
			EventExpression[] pre_dout_event = new EventExpression[result_depth];
			for (int d = 0; d < result_depth; d++) {
				pre_dout_event[d] = new EventExpression(pre_dout[d]);
			}
			EventControl pre_dout_ec = new EventControl(new EventExpression(
					pre_dout_event));
			SequentialBlock sequential_block = new SequentialBlock();
			sequential_block.add(new Assign.NonBlocking(mux_out, pre_dout[0]));
			memoryModule.state(new Always(new ProceduralTimingBlock(
					pre_dout_ec, sequential_block)));
		}

	}

	/**
	 * Instantiates a memory instance in the form of {@link MemoryInstance}
	 * 
	 * @return a value of type 'ModuleInstance'
	 */
	@Override
	public ModuleInstance instantiate(MemoryBank bank) {
		assert getMemBank().getBankPorts().size() == 1;
		assert bank.getBankPorts().size() == getMemBank().getBankPorts().size();

		MemoryInstance memoryInstance = new MemoryInstance(getName(), getName()
				+ "_instance" + memory_instance_id++);

		// MemoryBank.BankPort bankPort =
		// (MemoryBank.BankPort)getMemBank().getBankPorts().get(0);
		MemoryBank.BankPort bankPort = bank.getBankPorts().get(0);

		// Net clkWire = new BusWire(getMemBank().getClockPort().getBus());
		Net clkWire = NetFactory.makeNet(bank.getClockPort().getBus());
		memoryInstance.connect(clkPort, clkWire);

		if (renPort != null) {
			// Net enWire = new BusWire(bankPort.getEnablePort().getBus());
			Net enWire = new PortWire(bankPort.getEnablePort()); // enWire=ARG2[32]
			memoryInstance.connect(renPort, enWire);
		}

		if (wenPort != null) {
			// Net weWire = new BusWire(bankPort.getWriteEnablePort().getBus());
			Net weWire = new PortWire(bankPort.getWriteEnablePort());
			memoryInstance.connect(wenPort, weWire);
		}

		Expression addrWire = new PortWire(bankPort.getAddressPort());
		memoryInstance.connect(adrPort, addrWire);

		if (dinPort != null) {
			Expression dataInWire = new PortWire(bankPort.getDataInPort());
			memoryInstance.connect(dinPort, dataInWire);
		}

		if (doutPort != null) {
			Net dataOutWire = NetFactory.makeNet(bankPort.getDataOutBus());
			memoryInstance.connect(doutPort, dataOutWire);
			memoryInstance.addProducedNet(dataOutWire);
		}

		// The done is now calculated in the StructuralMemory.
		// if (donePort != null)
		// {
		// Net doneOutWire = new
		// BusWire(bankPort.getExit(com.xilinx.hllc.lim.Exit.DONE).getDoneBus());
		// memoryInstance.connect(donePort, doneOutWire);
		// memoryInstance.addProducedNet(doneOutWire);
		// }
		memoryInstance.noConnect(donePort);

		return memoryInstance;
	}

}// SinglePortRamWriter
