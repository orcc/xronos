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
import net.sf.openforge.lim.memory.MemoryBank;
import net.sf.openforge.util.XilinxDevice;
import net.sf.openforge.verilog.mapping.MappedModule;
import net.sf.openforge.verilog.mapping.MemoryMapper;
import net.sf.openforge.verilog.model.Assign;
import net.sf.openforge.verilog.model.Comment;
import net.sf.openforge.verilog.model.HexNumber;
import net.sf.openforge.verilog.model.InlineComment;
import net.sf.openforge.verilog.model.Module;
import net.sf.openforge.verilog.model.Net;
import net.sf.openforge.verilog.model.Register;
import net.sf.openforge.verilog.model.Wire;
import net.sf.openforge.verilog.pattern.MemoryModule;

/**
 * SinglePortRam.java
 * 
 * 
 * <p>
 * Created: Tue Dec 3 10:40:14 2002
 * 
 * @author imiller, last modified by $Author: imiller $
 * @version $Id: SinglePortRomWriter.java 538 2007-11-21 06:22:39Z imiller $
 */
public class SinglePortRomWriter extends SinglePortRamWriter {

	public SinglePortRomWriter(MemoryBank memory) {
		super(memory);
		wenPort = null;
		dinPort = null;
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
		// System.out.println("************* result_width " + result_width +
		// " result_depth " + result_depth);

		// add the memory primitive we are instantiating to the mappedModule
		// list
		String sim_include_file = MemoryMapper.SIM_INCLUDE_PATH
				+ match.getName() + ".v";
		String synth_include_file = MemoryMapper.SYNTH_INCLUDE_PATH
				+ "unisim_comp.v";
		MemoryModule memoryModule = new MemoryModule(getName(),
				Collections.singleton(new MappedModule(match.getName(),
						sim_include_file, synth_include_file)));

		// We need to clone out the array in preparation for
		// configuration
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
				// new_element.setWeName("we_" + d);

				// The element will never grab more address bits that
				// its largest will allow, so don't worry about giving
				// them all to it.
				new_element
						.setAddr(adr, getAddrWidth(), 0, getAddrWidth() == 1);

				new_element.setDataInName(din);
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
		// memoryModule.addPort(wenPort);
		memoryModule.addPort(adrPort);
		// memoryModule.addPort(dinPort);
		memoryModule.addPort(doutPort);
		memoryModule.addPort(donePort);

		// Declare internal wires
		// Net[] we = new Wire[result_depth];
		Net[] pre_dout = new Wire[result_depth];

		for (int d = 0; d < result_depth; d++) {
			// we[d] = new Wire("we_" + d, 1);
			// memoryModule.declare(we[d]);
			pre_dout[d] = new Wire("pre_dout_" + d, getDataWidth());
			memoryModule.declare(pre_dout[d]);

			if ((extra_dout_wires[d] != null)
					&& (extra_dout_wires[d].getIdentifier().getToken().length() > 0)) {
				memoryModule.declare(extra_dout_wires[d]);
			}
		}

		Register mux_out = new Register("mux_out", getDataWidth());
		memoryModule.declare(mux_out);
		Wire fake_din = new Wire(din, getDataWidth());
		memoryModule.declare(fake_din);
		memoryModule.state(new Assign.Continuous(fake_din, new HexNumber(0,
				getDataWidth())));

		// Write out the small amount of logic then the memory array
		int extra_address_bits = getAddrWidth()
				- ram_array[0][0].getLibAddressWidth();
		for (int d = 0; d < result_depth; d++) {
			// memoryModule.state(new Assign.Continuous(we[d], new
			// HexNumber(0,1)));
			// if (extra_address_bits >0)
			// {
			// HexNumber hex_d = new HexNumber(new
			// HexConstant(Integer.toHexString(d), extra_address_bits));
			// Expression addr_equal = new
			// Compare.Equals(adrPort.getRange(getAddrWidth()-1,
			// getAddrWidth()-extra_address_bits), hex_d);
			// Expression we_and_addr_equal = new Bitwise.And(wenPort, new
			// Group(addr_equal));
			// memoryModule.state(new Assign.Continuous(we[d],
			// we_and_addr_equal));
			// }
			// else
			// {
			// memoryModule.state(new Assign.Continuous(we[d], wenPort));
			// }
		}

		final boolean registerLutRead = ((getMemBank().getImplementation()
				.getReadLatency().getMinClocks() > 0) && getMemBank()
				.getImplementation().isLUT());

		mergeResults(extra_address_bits, result_depth, getAddrWidth(),
				getDataWidth(), pre_dout, mux_out, memoryModule, adrPort,
				clkPort, getReadLatency(), registerLutRead, "");

		// Expression doneExpr = new Bitwise.Or(renPort, wenPort);
		memoryModule.state(new Assign.Continuous(doutPort, mux_out));
		memoryModule.state(new Assign.Continuous(donePort, renPort));

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

}// SinglePortRomWriter
