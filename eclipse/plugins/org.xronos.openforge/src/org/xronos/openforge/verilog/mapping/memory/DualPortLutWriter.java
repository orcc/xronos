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

import java.util.Collections;

import org.xronos.openforge.app.EngineThread;
import org.xronos.openforge.lim.CodeLabel;
import org.xronos.openforge.lim.memory.MemoryBank;
import org.xronos.openforge.util.XilinxDevice;
import org.xronos.openforge.verilog.mapping.MappedModule;
import org.xronos.openforge.verilog.mapping.MemoryMapper;
import org.xronos.openforge.verilog.model.Always;
import org.xronos.openforge.verilog.model.Assign;
import org.xronos.openforge.verilog.model.Bitwise;
import org.xronos.openforge.verilog.model.CaseBlock;
import org.xronos.openforge.verilog.model.Comment;
import org.xronos.openforge.verilog.model.Compare;
import org.xronos.openforge.verilog.model.Decimal;
import org.xronos.openforge.verilog.model.EventControl;
import org.xronos.openforge.verilog.model.EventExpression;
import org.xronos.openforge.verilog.model.Expression;
import org.xronos.openforge.verilog.model.Group;
import org.xronos.openforge.verilog.model.HexConstant;
import org.xronos.openforge.verilog.model.HexNumber;
import org.xronos.openforge.verilog.model.InlineComment;
import org.xronos.openforge.verilog.model.Input;
import org.xronos.openforge.verilog.model.Module;
import org.xronos.openforge.verilog.model.ModuleInstance;
import org.xronos.openforge.verilog.model.Net;
import org.xronos.openforge.verilog.model.ParameterSetting;
import org.xronos.openforge.verilog.model.ProceduralTimingBlock;
import org.xronos.openforge.verilog.model.Register;
import org.xronos.openforge.verilog.model.SequentialBlock;
import org.xronos.openforge.verilog.model.Unary;
import org.xronos.openforge.verilog.model.Wire;
import org.xronos.openforge.verilog.pattern.MemoryModule;


/**
 * DualPortLutWriter implements the defineModule method specific to
 * instantiating a LUT ram based implementation for either dual ported RAM or
 * dual ported ROM. All read accesses are combinational (take 0 clock cycles)
 * and all write accesses take a single clock cycle.
 * 
 * <p>
 * Created: Tue Dec 3 12:58:53 2002
 * 
 * @author imiller, last modified by $Author: imiller $
 * @version $Id: DualPortLutWriter.java 490 2007-06-15 16:37:00Z imiller $
 */
public class DualPortLutWriter extends DualPortWriter {

	public DualPortLutWriter(MemoryBank memory) {
		super(memory);
		assert !mpA.writes() || !mpB.writes() : "Lut based dual ports must have at least 1 port that is read only";
	}

	/**
	 * Defines a Verilog memory module for a LUT based dual ported memory.
	 * 
	 * @return a value of type 'Module'
	 */
	@Override
	public Module defineModule() {
		// OK, we are going to map to a memory configuration and hard
		// instantiate the primitives necessary along with
		// initialization values.

		XilinxDevice xd = EngineThread.getGenericJob().getPart(
				CodeLabel.UNSCOPED);

		DualPortRam match = (DualPortRam) getLowestCost(DualPortRam
				.getDualPortMappers(xd, isLUT()));
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

		// Create the necessary primitives and assign intial values
		RamImplementation ramImpl = getImplementationRams(match, mpA, mpB);
		DualPortRam[][] ram_array = ramImpl.getRamArray();
		Net[] extra_douta_wires = ramImpl.getSpareA();
		Net[] extra_doutb_wires = ramImpl.getSpareB();

		// Add the necessary ports to the module.
		memoryModule.addPort(clkPort);
		memoryModule.addPort(mpA.ren);
		if (mpA.writes()) {
			memoryModule.addPort(mpA.wen);
			memoryModule.addPort(mpA.din);
		}
		memoryModule.addPort(mpB.ren);
		if (mpB.writes()) {
			memoryModule.addPort(mpB.wen);
			memoryModule.addPort(mpB.din);
		}
		memoryModule.addPort(mpA.adr);
		memoryModule.addPort(mpB.adr);
		if (mpA.reads()) {
			memoryModule.addPort(mpA.dout);
		}
		if (mpB.reads()) {
			memoryModule.addPort(mpB.dout);
		}
		memoryModule.addPort(mpA.done);
		memoryModule.addPort(mpB.done);

		// Declare internal wires
		int result_depth = (int) java.lang.Math.ceil((double) getDepth()
				/ (double) match.getDepth());
		Net[] wea = new Wire[result_depth];
		Net[] web = new Wire[result_depth];
		Net[] pre_douta = new Wire[result_depth];
		Net[] pre_doutb = new Wire[result_depth];

		for (int d = 0; d < result_depth; d++) {
			wea[d] = new Wire("wea_" + d, 1);
			if (mpA.writes()) {
				memoryModule.declare(wea[d]);
			}
			web[d] = new Wire("web_" + d, 1);
			if (mpB.writes()) {
				memoryModule.declare(web[d]);
			}
			pre_douta[d] = new Wire("pre_douta_" + d, getDataWidth());
			if (mpA.reads()) {
				memoryModule.declare(pre_douta[d]);
			}
			pre_doutb[d] = new Wire("pre_doutb_" + d, getDataWidth());
			if (mpB.reads()) {
				memoryModule.declare(pre_doutb[d]);
			}
			if (extra_douta_wires[d] != null
					&& extra_douta_wires[d].getIdentifier().getToken().length() > 0) {
				memoryModule.declare(extra_douta_wires[d]);
			}
			if (extra_doutb_wires[d] != null
					&& extra_doutb_wires[d].getIdentifier().getToken().length() > 0) {
				memoryModule.declare(extra_doutb_wires[d]);
			}
		}
		// Write out the small amount of logic then the memory array
		int elementAddrWidth = ram_array[0][0].getLibAddressWidth();
		int extra_address_bits = getAddrWidth() - elementAddrWidth;
		stateWriteEnables(mpA, wea, memoryModule, elementAddrWidth);
		stateWriteEnables(mpB, web, memoryModule, elementAddrWidth);

		final boolean regRead = isLUT()
				&& (getMemBank().getImplementation().getReadLatency()
						.getMinClocks() > 0);
		stateDones(mpA, memoryModule, regRead, "a");
		stateDones(mpB, memoryModule, regRead, "b");

		if (mpA.reads()) {
			Net mux_outa = new Register("mux_outa", getDataWidth());
			memoryModule.declare(mux_outa);

			Always always = muxPreWires(mpA.adr, pre_douta, mux_outa,
					extra_address_bits);
			memoryModule.state(always);

			stateDout(mpA, mux_outa, memoryModule, regRead, "a");
		}

		if (mpB.reads()) {
			Net mux_outb = new Register("mux_outb", getDataWidth());
			memoryModule.declare(mux_outb);

			Always always = muxPreWires(mpB.adr, pre_doutb, mux_outb,
					extra_address_bits);
			memoryModule.state(always);

			stateDout(mpB, mux_outb, memoryModule, regRead, "b");
		}

		debugContents(memoryModule);

		for (int c = 0; c < ram_array[0].length; c++) {
			for (int r = 0; r < ram_array.length; r++) {
				String mem_inst_comment = "Memory array element: COL: " + c
						+ ", ROW: " + r;
				memoryModule.state(new InlineComment(mem_inst_comment,
						Comment.SHORT));
				DualPortRam ram = ram_array[r][c];
				memoryModule.state(ram.initialize());
				ModuleInstance instance = ram.instantiate();
				if (ram.isBlockRam16()) {
					instance.addParameterValue(new ParameterSetting(
							"WRITE_MODE_A", "\"" + mpA.getWriteMode() + "\""));
					instance.addParameterValue(new ParameterSetting(
							"WRITE_MODE_B", "\"" + mpB.getWriteMode() + "\""));
				}

				memoryModule.state(instance);

			}
		}

		return memoryModule;
	}

	/**
	 * Writes out the logic for the write enable signals for each bank of
	 * memory.
	 * 
	 * @param mp
	 *            a value of type 'MemoryPortDef'
	 * @param enables
	 *            a value of type 'Net[]'
	 * @param memoryModule
	 *            a value of type 'Module'
	 * @param elementAddrWidth
	 *            an int, the width of the address ports of the ram primitives.
	 */
	private void stateWriteEnables(MemoryPortDef mp, Net[] enables,
			Module memoryModule, int elementAddrWidth) {
		if (mp.writes()) {
			for (int d = 0; d < enables.length; d++) {
				if (enables.length > 1) {
					int extraAddrBits = getAddrWidth() - elementAddrWidth;
					HexNumber hex_d = new HexNumber(new HexConstant(
							Integer.toHexString(d), extraAddrBits));
					Expression addr_equal = new Compare.Equals(
							mp.adr.getRange(getAddrWidth() - 1, getAddrWidth()
									- extraAddrBits), hex_d);
					Expression weA_and_addr_equal = new Bitwise.And(mp.wen,
							new Group(addr_equal));
					memoryModule.state(new Assign.Continuous(enables[d],
							weA_and_addr_equal));
				} else {
					memoryModule
							.state(new Assign.Continuous(enables[d], mp.wen));
				}
			}
		}
	}

	/**
	 * Generates and returns an Always block that is used to combine the
	 * pre_dout wires into a single result for the memory based upon the address
	 * being supplied to the memory. This may use a case statement if there are
	 * multiple banks or a simple assignment.
	 * 
	 * @param adr
	 *            a value of type 'Input'
	 * @param pre_dout
	 *            a value of type 'Net[]'
	 * @param mux
	 *            a value of type 'Net'
	 * @param extra_address_bits
	 *            a value of type 'int'
	 * @return a value of type 'Always'
	 */
	private Always muxPreWires(Input adr, Net[] pre_dout, Net mux,
			int extra_address_bits) {
		assert pre_dout.length > 0 : "Must have at least one RAM primitive in memory";

		final boolean doCase = pre_dout.length > 1;

		final EventExpression event_list = new EventExpression(pre_dout[0]);
		for (int i = 1; i < pre_dout.length; i++) {
			event_list.add(pre_dout[i]);
		}
		if (doCase) {
			event_list.add(adr);
		}

		SequentialBlock block = null;
		if (doCase) {
			// We need an output mux handled in a case statement
			CaseBlock case_block = new CaseBlock(adr.getRange(
					getAddrWidth() - 1, getAddrWidth() - extra_address_bits));
			for (int d = 0; d < pre_dout.length; d++) {
				Decimal case_value = new Decimal(d, extra_address_bits);
				case_block.add(case_value.toString(), new Assign.Blocking(mux,
						pre_dout[d]));
			}
			HexConstant unknown = new HexConstant("0", getDataWidth());
			case_block.add("default", new Assign.Blocking(mux, new HexNumber(
					unknown)));
			block = new SequentialBlock(case_block);
		} else {
			// We need a simple assignment of the pre_dout wire to the
			// mux wire.
			assert pre_dout.length == 1;
			block = new SequentialBlock();
			block.add(new Assign.NonBlocking(mux, pre_dout[0]));
		}

		EventControl event_control = new EventControl(event_list);
		// SequentialBlock sequential_block = new SequentialBlock(block);
		Always always = new Always(new ProceduralTimingBlock(event_control,
				block));
		return always;
	}

	/**
	 * Generate the logic for the DONE signal of a given 'side' of the dual
	 * ported memory. The logic for the DONE depends on whether or not we are
	 * registering the reads.
	 * 
	 * @param mp
	 *            a value of type 'MemoryPortDef'
	 * @param memoryModule
	 *            a value of type 'Module'
	 * @param regRead
	 *            a value of type 'boolean'
	 * @param X
	 *            a String used to distinguish one port from the other
	 */
	private void stateDones(MemoryPortDef mp, Module memoryModule,
			boolean regRead, String X) {
		// The done equation depends on the specific behavior.
		// Combinational read: ren & !we | delay_we
		// Sequential read: delay_ re | delay_we

		// First, register the wen and ren as needed
		SequentialBlock delayBlock = new SequentialBlock();
		Register weDone = new Register("we" + X + "_done", 1);
		if (mp.writes()) {
			delayBlock.add(new Assign.NonBlocking(weDone, mp.wen));
		}
		Register reDone = new Register("re" + X + "_done", 1);
		if (mp.reads() && regRead) {
			delayBlock.add(new Assign.NonBlocking(reDone, mp.ren));
		}

		EventControl clkEvent = new EventControl(new EventExpression.PosEdge(
				clkPort));
		memoryModule.state(new Always(new ProceduralTimingBlock(clkEvent,
				delayBlock)));

		// Second, state the logic for the done signals
		if (mp.writes() && mp.reads()) {
			if (regRead) { // ren & !we | delay_we
				memoryModule.state(new Assign.Continuous(mp.done,
						new Bitwise.Or(new Bitwise.And(mp.ren, new Unary.Not(
								mp.wen)), weDone)));
			} else { // delay_ren | delay_wen
				memoryModule.state(new Assign.Continuous(mp.done,
						new Bitwise.Or(reDone, weDone)));
			}
		} else if (mp.writes()) {
			memoryModule.state(new Assign.Continuous(mp.done, weDone));
		} else {
			Net ren_done = (regRead) ? (Net) reDone : (Net) mp.ren;
			memoryModule.state(new Assign.Continuous(mp.done, ren_done));
		}
	}

	/**
	 * Connects the DOUT of the forge_dualport_memory to the driving source.
	 * This driving source may be the 'mux' of the banks of RAM primitives or a
	 * registered version of that mux based upon the boolean 'regRead'.
	 * 
	 * @param mp
	 *            a value of type 'MemoryPortDef'
	 * @param mux_out
	 *            a value of type 'Net'
	 * @param memoryModule
	 *            a value of type 'Module'
	 * @param regRead
	 *            a value of type 'boolean'
	 * @param X
	 *            a String used to distinguish one port from the other
	 */
	private void stateDout(MemoryPortDef mp, Net mux_out, Module memoryModule,
			boolean regRead, String X) {
		Net dout = mux_out;

		if (regRead) {
			// Register the muxed signal, then assign that to the
			// output
			Register delayed = new Register("mux" + X + "out_delayed",
					mux_out.getWidth());
			SequentialBlock delayBlock = new SequentialBlock();
			delayBlock.add(new Assign.NonBlocking(delayed, mux_out));

			EventControl clkEvent = new EventControl(
					new EventExpression.PosEdge(clkPort));
			memoryModule.state(new Always(new ProceduralTimingBlock(clkEvent,
					delayBlock)));

			dout = delayed;
		}

		memoryModule.state(new Assign.Continuous(mp.dout, dout));
	}

}// DualPortLutWriter

