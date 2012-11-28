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
import org.xronos.openforge.lim.Latency;
import org.xronos.openforge.lim.memory.MemoryBank;
import org.xronos.openforge.util.XilinxDevice;
import org.xronos.openforge.verilog.mapping.MappedModule;
import org.xronos.openforge.verilog.mapping.MemoryMapper;
import org.xronos.openforge.verilog.model.Always;
import org.xronos.openforge.verilog.model.Assign;
import org.xronos.openforge.verilog.model.Bitwise;
import org.xronos.openforge.verilog.model.Comment;
import org.xronos.openforge.verilog.model.Compare;
import org.xronos.openforge.verilog.model.EventControl;
import org.xronos.openforge.verilog.model.EventExpression;
import org.xronos.openforge.verilog.model.Expression;
import org.xronos.openforge.verilog.model.Group;
import org.xronos.openforge.verilog.model.HexConstant;
import org.xronos.openforge.verilog.model.HexNumber;
import org.xronos.openforge.verilog.model.InlineComment;
import org.xronos.openforge.verilog.model.Module;
import org.xronos.openforge.verilog.model.ModuleInstance;
import org.xronos.openforge.verilog.model.Net;
import org.xronos.openforge.verilog.model.ParameterSetting;
import org.xronos.openforge.verilog.model.ProceduralTimingBlock;
import org.xronos.openforge.verilog.model.Register;
import org.xronos.openforge.verilog.model.SequentialBlock;
import org.xronos.openforge.verilog.model.Wire;
import org.xronos.openforge.verilog.pattern.MemoryModule;


/**
 * DualPortBlockWriter implements the defineModule method specific to
 * instantiating a Block ram based implementation for either dual ported RAM or
 * dual ported ROM. All accesses (read or write) take 1 clock cycle.
 * 
 * <p>
 * Created: Tue Dec 3 12:58:53 2002
 * 
 * @author imiller, last modified by $Author: imiller $
 * @version $Id: DualPortBlockWriter.java 538 2007-11-21 06:22:39Z imiller $
 */
public class DualPortBlockWriter extends DualPortWriter {

	public DualPortBlockWriter(MemoryBank memory) {
		super(memory);
	}

	/**
	 * Defines a Verilog memory module
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

		Register mux_outa = new Register("mux_outa", getDataWidth());
		if (mpA.reads()) {
			memoryModule.declare(mux_outa);
		}
		Register mux_outb = new Register("mux_outb", getDataWidth());
		if (mpB.reads()) {
			memoryModule.declare(mux_outb);
		}

		// Write out the small amount of logic then the memory array
		int extra_address_bits = getAddrWidth()
				- ram_array[0][0].getLibAddressWidth();
		for (int d = 0; d < result_depth; d++) {
			if (mpA.writes()) {
				if (extra_address_bits > 0) {
					HexNumber hex_d = new HexNumber(new HexConstant(
							Integer.toHexString(d), extra_address_bits));
					Expression addrA_equal = new Compare.Equals(
							mpA.adr.getRange(getAddrWidth() - 1, getAddrWidth()
									- extra_address_bits), hex_d);
					Expression weA_and_addrA_equal = new Bitwise.And(mpA.wen,
							new Group(addrA_equal));
					memoryModule.state(new Assign.Continuous(wea[d],
							weA_and_addrA_equal));
				} else {
					memoryModule.state(new Assign.Continuous(wea[d], mpA.wen));
				}
			}
			if (mpB.writes()) {
				if (extra_address_bits > 0) {
					HexNumber hex_d = new HexNumber(new HexConstant(
							Integer.toHexString(d), extra_address_bits));
					Expression addrB_equal = new Compare.Equals(
							mpB.adr.getRange(getAddrWidth() - 1, getAddrWidth()
									- extra_address_bits), hex_d);
					Expression weB_and_addrB_equal = new Bitwise.And(mpB.wen,
							new Group(addrB_equal));
					memoryModule.state(new Assign.Continuous(web[d],
							weB_and_addrB_equal));
				} else {
					memoryModule.state(new Assign.Continuous(web[d], mpB.wen));
				}
			}
		}

		/*
		 * if (extra_address_bits > 0) { if (mpA.reads()) { // We need an output
		 * mux // ABKTODO: the original expression used ranges... //
		 * EventExpression event_list = new
		 * EventExpression(mpA.adr.getRange(getAddrWidth()-1,
		 * getAddrWidth()-extra_address_bits));
		 * 
		 * EventExpression event_list = new EventExpression(mpA.adr);
		 * 
		 * for (int d = 0; d < result_depth; d++) {
		 * event_list.add(pre_douta[d]); }
		 * 
		 * EventControl event_control = new EventControl(event_list);
		 * 
		 * CaseBlock case_block = new
		 * CaseBlock(mpA.adr.getRange(getAddrWidth()-1,
		 * getAddrWidth()-extra_address_bits)); for (int d = 0; d <
		 * result_depth; d++) { Decimal case_value = new Decimal(d,
		 * extra_address_bits); case_block.add(case_value.toString(), new
		 * Assign.Blocking(mux_outa, pre_douta[d])); } HexConstant unknown = new
		 * HexConstant("0", getDataWidth()); case_block.add("default", new
		 * Assign.Blocking(mux_outa, new HexNumber(unknown)));
		 * 
		 * SequentialBlock sequential_block = new SequentialBlock(case_block);
		 * 
		 * Always always = new Always(new ProceduralTimingBlock(event_control,
		 * sequential_block)); memoryModule.state(always); } if (mpB.reads()) {
		 * // We need an output mux // ABKTODO: again the original used ranges
		 * in the event expression... // EventExpression event_list = new
		 * EventExpression(mpB.adr.getRange(getAddrWidth()-1,
		 * getAddrWidth()-extra_address_bits)); EventExpression event_list = new
		 * EventExpression(mpB.adr);
		 * 
		 * for (int d = 0; d < result_depth; d++) {
		 * event_list.add(pre_doutb[d]); }
		 * 
		 * EventControl event_control = new EventControl(event_list);
		 * 
		 * CaseBlock case_block = new
		 * CaseBlock(mpB.adr.getRange(getAddrWidth()-1,
		 * getAddrWidth()-extra_address_bits)); for (int d = 0; d <
		 * result_depth; d++) { Decimal case_value = new Decimal(d,
		 * extra_address_bits); case_block.add(case_value.toString(), new
		 * Assign.Blocking(mux_outb, pre_doutb[d])); } HexConstant unknown = new
		 * HexConstant("0", getDataWidth()); case_block.add("default", new
		 * Assign.Blocking(mux_outb, new HexNumber(unknown)));
		 * 
		 * SequentialBlock sequential_block = new SequentialBlock(case_block);
		 * 
		 * Always always = new Always(new ProceduralTimingBlock(event_control,
		 * sequential_block)); memoryModule.state(always); } } else { if
		 * (mpA.reads()) { // We need just an assign EventExpression[]
		 * pre_dout_event = new EventExpression[result_depth]; for (int d = 0; d
		 * < result_depth; d++) { pre_dout_event[d] = new
		 * EventExpression(pre_douta[d]); } EventControl pre_dout_ec = new
		 * EventControl(new EventExpression(pre_dout_event)); SequentialBlock
		 * sequential_block = new SequentialBlock(); sequential_block.add(new
		 * Assign.NonBlocking(mux_outa, pre_douta[0])); Always always_block =
		 * new Always(new ProceduralTimingBlock(pre_dout_ec, sequential_block));
		 * memoryModule.state(always_block); } if (mpB.reads()) { // We need
		 * just an assign EventExpression[] pre_dout_event = new
		 * EventExpression[result_depth]; for (int d = 0; d < result_depth; d++)
		 * { pre_dout_event[d] = new EventExpression(pre_doutb[d]); }
		 * EventControl pre_dout_ec = new EventControl(new
		 * EventExpression(pre_dout_event)); SequentialBlock sequential_block =
		 * new SequentialBlock(); sequential_block.add(new
		 * Assign.NonBlocking(mux_outb, pre_doutb[0])); Always always_block =
		 * new Always(new ProceduralTimingBlock(pre_dout_ec, sequential_block));
		 * memoryModule.state(always_block); } }
		 */
		//
		// Ok, yes, I put this fatal error here because we found that
		// the address had to be registered before it was used to mux
		// the pre_douts (if read latency > 0) for the single port
		// case and I expect the same to be true for the dual port
		// case. However, we currently dont generate dual port
		// memories so I cant test it. If you have found this fatal
		// error, then you have figured out how to generate dual port
		// memories, and you can determine if the same muxing code
		// will work. If not, the above commented out code is what
		// used to be here, but you need to break the combinational
		// path through ADDR (and ADDR should be delayed to match the
		// timing of the dout)
		// IDM Jun 13, 2007. Checked and OK.
		// EngineThread.getEngine().fatalError("Untested code, unexpectedly reached");
		Latency readLatency = getMemBank().getImplementation().getReadLatency();
		final boolean registerLutRead = ((getMemBank().getImplementation()
				.getReadLatency().getMinClocks() > 0) && getMemBank()
				.getImplementation().isLUT());

		SinglePortRamWriter.mergeResults(extra_address_bits, result_depth,
				getAddrWidth(), getDataWidth(), pre_douta, mux_outa,
				memoryModule, mpA.adr, clkPort, readLatency, registerLutRead,
				"A");

		SinglePortRamWriter.mergeResults(extra_address_bits, result_depth,
				getAddrWidth(), getDataWidth(), pre_doutb, mux_outb,
				memoryModule, mpB.adr, clkPort, readLatency, registerLutRead,
				"B");

		SequentialBlock doneBlock = new SequentialBlock();
		// register ren and/or wen. Done is or of both registers.
		Register weaDone = new Register("wea_done", 1);
		Register webDone = new Register("web_done", 1);
		Register reaDone = new Register("rea_done", 1);
		Register rebDone = new Register("reb_done", 1);
		if (mpA.writes())
			doneBlock.add(new Assign.NonBlocking(weaDone, mpA.wen));
		if (mpA.reads())
			doneBlock.add(new Assign.NonBlocking(reaDone, mpA.ren));
		if (mpB.writes())
			doneBlock.add(new Assign.NonBlocking(webDone, mpB.wen));
		if (mpB.reads())
			doneBlock.add(new Assign.NonBlocking(rebDone, mpB.ren));

		EventControl clkEvent = new EventControl(new EventExpression.PosEdge(
				clkPort));
		memoryModule.state(new Always(new ProceduralTimingBlock(clkEvent,
				doneBlock)));

		if (mpA.writes() && mpA.reads())
			memoryModule.state(new Assign.Continuous(mpA.done, new Bitwise.Or(
					weaDone, reaDone)));
		else if (mpA.writes())
			memoryModule.state(new Assign.Continuous(mpA.done, weaDone));
		else
			memoryModule.state(new Assign.Continuous(mpA.done, reaDone));

		if (mpB.writes() && mpB.reads())
			memoryModule.state(new Assign.Continuous(mpB.done, new Bitwise.Or(
					webDone, rebDone)));
		else if (mpB.writes())
			memoryModule.state(new Assign.Continuous(mpB.done, webDone));
		else
			memoryModule.state(new Assign.Continuous(mpB.done, rebDone));

		if (mpA.reads()) {
			memoryModule.state(new Assign.Continuous(mpA.dout, mux_outa));
		}
		if (mpB.reads()) {
			memoryModule.state(new Assign.Continuous(mpB.dout, mux_outb));
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

}// DualPortBlockWriter

