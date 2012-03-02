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

import net.sf.openforge.verilog.model.BinaryNumber;
import net.sf.openforge.verilog.model.Comment;
import net.sf.openforge.verilog.model.Expression;
import net.sf.openforge.verilog.model.InlineComment;
import net.sf.openforge.verilog.model.ModuleInstance;
import net.sf.openforge.verilog.model.ParameterSetting;
import net.sf.openforge.verilog.model.PortConnection;
import net.sf.openforge.verilog.model.Wire;
import net.sf.openforge.verilog.pattern.StatementBlock;

/**
 * A base class of all of the XST supported dual-port lut ram models.
 * <P>
 * 
 * Created: Tue Jun 18 12:37:06 2002
 * 
 * @author cwu
 * @version $Id: DualPortLutRam.java 538 2007-11-21 06:22:39Z imiller $
 */

public abstract class DualPortLutRam extends DualPortRam {

	private static int instancecnt = 0;

	public abstract String getName();

	public abstract int getWidth();

	public abstract int getDepth();

	public abstract int getCost();

	public boolean isBlockRam16() {
		return false;
	}

	public boolean isDataOutputRegistered() {
		return (false);
	}

	public StatementBlock initialize() {
		StatementBlock initial_block = new StatementBlock();
		initial_block
				.add(new InlineComment(
						" Initialization of Dual Port LUT ram now done through explicit parameter setting.",
						Comment.SHORT));
		return initial_block;
	}

	private void initialize(ModuleInstance instance) {
		// The init strings for LUT memories use the syntax INIT for
		// X1 memories, and INIT_NM where NM is the output data bit
		// the init value corresponds to. Each INIT string is written
		// as hex values and is as long as the memory is deep. The
		// ordering is MSB to LSB. The array generator will always
		// supply us with all the init bets to fill our memory, even
		// if some of the data bits are not used.

		// loop for each INIT string
		for (int w = 0; w < getWidth(); w++) {
			byte[] hexDigits = new byte[getDepth() / 4];
			int hexIndex = 0;
			int shift = 0;

			// loop through 1 data output
			for (int index = w; index < (getWidth() * getDepth()); index = index
					+ getWidth()) {
				hexDigits[hexIndex] |= (getInitBit(index) << shift);
				shift++;
				if (shift == 4) {
					hexIndex++;
					shift = 0;
				}
			}
			// We have everything we need, compose the INIT string
			String initParam = "INIT";
			String initValue = getDepth() + "'h";
			if (getWidth() > 1) {
				initParam += ("_0" + w);
			}

			for (int digit = (hexDigits.length - 1); digit >= 0; digit--) {
				initValue += Integer.toHexString(hexDigits[digit] & 0xf);
			}
			instance.addParameterValue(new ParameterSetting(initParam,
					initValue));
		}
	}

	public ModuleInstance instantiate() {
		String instance_name = getName() + "_instance_" + instancecnt++;

		ModuleInstance moduleInstance = new ModuleInstance(getName(),
				instance_name);
		initialize(moduleInstance);

		BinaryNumber zero = new BinaryNumber(0, 1);

		// Lut dual ports only has one side that can write, we'll
		// default it to the PORTA side, both sides can read.
		Expression clk_exp = zero;
		if (clkName[PORTA].length() > 0) {
			clk_exp = new Wire(clkName[PORTA], 1);
		}
		moduleInstance.connect(new Wire("WCLK", 1), clk_exp);

		Expression we_exp = zero;
		if (weName[PORTA].length() > 0) {
			we_exp = new Wire(weName[PORTA], 1);
		}
		moduleInstance.connect(new Wire("WE", 1), we_exp);

		for (int i = 0; i < getLibAddressWidth(); i++) {
			Expression addr_exp = zero;
			// Determine if we have an address bit to attach, if not
			// put in a constant 0
			if (i < addrWidth) {
				int addr_bit = addrStartBit[PORTA] + i;
				addr_exp = new Wire(addrName[PORTA], addrWidth);
				if (!addrScalar[PORTA]) {
					addr_exp = ((Wire) addr_exp).getRange(addr_bit, addr_bit);
				}
			}
			moduleInstance.connect(new Wire("A" + i, 1), addr_exp);
		}

		Wire dataInA = new Wire(dataInName[PORTA], 256);
		Wire dataOutA = new Wire(dataOutName[PORTA], 256);
		Wire dataOutB = new Wire(dataOutName[PORTB], 256);

		for (int i = 0; i < getWidth(); i++) {
			Expression di_exp = new BinaryNumber(0, 1);
			if (dataInName[PORTA].length() > 0) {
				if (i < dataAttachWidth[PORTA]) {
					if (!datScalar[PORTA]) {
						di_exp = dataInA.getRange(dataInStartBit[PORTA] + i,
								dataInStartBit[PORTA] + i);
					} else {
						dataInA = new Wire(dataInName[PORTA], 1);
						di_exp = dataInA;
					}
				}
			}
			if (getWidth() == 1) {
				moduleInstance.connect(new Wire("D", 1), di_exp);
			} else {
				moduleInstance.connect(new Wire("D" + i, 1), di_exp);
			}
		}

		for (int i = 0; i < getWidth(); i++) {
			if (dataOutName[PORTA] == "") // In case port A is a write only port
			{
				// Despite that this would make
				// sense... ModuleInstance doesn't handle it.
				// moduleInstance.add(new NoConnection(new Wire("SPO",1)));
				moduleInstance.add(new PortConnection(new Wire("SPO", 1),
						new Wire("", 1)));
			} else {
				Expression spo_exp = new Wire("", 1);
				if (i < dataAttachWidth[PORTA]) {
					if (!datScalar[PORTA]) {
						spo_exp = dataOutA.getRange(dataOutStartBit[PORTA] + i,
								dataOutStartBit[PORTA] + i);
					} else {
						dataOutA = new Wire(dataOutName[PORTA], 1);
						spo_exp = dataOutA;
					}
				}
				if (getWidth() == 1) {
					moduleInstance.connect(new Wire("SPO", 1), spo_exp);
				} else {
					moduleInstance.connect(new Wire("SPO" + i, 1), spo_exp);
				}
			}
		}

		for (int i = 0; i < getLibAddressWidth(); i++) {
			Expression addr_exp = zero;
			// Determine if we have an address bit to attach, if not
			// put in a constant 0
			if (i < addrWidth) {
				int addr_bit = addrStartBit[PORTB] + i;
				addr_exp = new Wire(addrName[PORTB], addrWidth);
				if (!addrScalar[PORTB]) {
					addr_exp = ((Wire) addr_exp).getRange(addr_bit, addr_bit);
				}
			}
			moduleInstance.connect(new Wire("DPRA" + i, 1), addr_exp);
		}

		for (int i = 0; i < getWidth(); i++) {
			Expression dpo_exp = new Wire("", 1);
			if (i < dataAttachWidth[PORTB]) {
				if (!datScalar[PORTB]) {
					dpo_exp = dataOutB.getRange(dataOutStartBit[PORTB] + i,
							dataOutStartBit[PORTB] + i);
				} else {
					dataOutB = new Wire(dataOutName[PORTB], 1);
					dpo_exp = dataOutB;
				}
			}
			if (getWidth() == 1) {
				moduleInstance.connect(new Wire("DPO", 1), dpo_exp);
			} else {
				moduleInstance.connect(new Wire("DPO" + i, 1), dpo_exp);
			}
		}

		return moduleInstance;
	}
}
