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
import net.sf.openforge.verilog.model.Concatenation;
import net.sf.openforge.verilog.model.Expression;
import net.sf.openforge.verilog.model.InlineComment;
import net.sf.openforge.verilog.model.ModuleInstance;
import net.sf.openforge.verilog.model.ParameterSetting;
import net.sf.openforge.verilog.model.Wire;
import net.sf.openforge.verilog.pattern.StatementBlock;

/**
 * A base class of all of the XST supported lut ram models.
 * <P>
 * 
 * Created: Tue Jun 18 12:37:06 2002
 * 
 * @author cwu
 * @version $Id: LutRam.java 443 2007-05-02 17:53:47Z imiller $
 */

public abstract class LutRam extends Ram {

	private static int instancecnt = 0;

	@Override
	public abstract String getName();

	@Override
	public abstract int getWidth();

	@Override
	public abstract int getDepth();

	@Override
	public abstract int getCost();

	@Override
	public boolean isDataOutputRegistered() {
		return (false);
	}

	@Override
	public StatementBlock initialize() {
		StatementBlock initial_block = new StatementBlock();
		initial_block
				.add(new InlineComment(
						" Initialization of LUT ram is accomplished through explicit parameter setting.",
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

	@Override
	public ModuleInstance instantiate() {
		String instance_name = getName() + "_instance_" + instancecnt++;

		ModuleInstance moduleInstance = new ModuleInstance(getName(),
				instance_name);

		initialize(moduleInstance);

		BinaryNumber zero = new BinaryNumber(0, 1);

		Expression clk_exp = zero;
		if (clkName.length() > 0) {
			clk_exp = new Wire(clkName, 1);
		}
		moduleInstance.connect(new Wire("WCLK", 1), clk_exp);

		Expression we_exp = zero;
		if (weName.length() > 0) {
			we_exp = new Wire(weName, 1);
		}
		moduleInstance.connect(new Wire("WE", 1), we_exp);

		for (int i = 0; i < getLibAddressWidth(); i++) {
			Expression addr_exp = zero;
			// Determine if we have an address bit to attach, if not
			// put in a constant 0
			if (i < addrWidth) {
				int addr_bit = addrStartBit + i;
				addr_exp = new Wire(addrName, addrWidth);
				if (!addrScalar) {
					addr_exp = ((Wire) addr_exp).getRange(addr_bit, addr_bit);
				}
			}
			moduleInstance.connect(new Wire("A" + i, 1), addr_exp);
		}

		Wire dataIn = new Wire(dataInName, 256);
		Wire dataOut = new Wire(dataOutName, 256);

		if (getWidth() >= 8) {
			// The data buses are written as busses for 8 and more
			// concatenate with 0s if we don't have enough bits to
			// attach
			Expression d_exp = new BinaryNumber(0, getWidth());
			Expression o_exp = new Wire("", getWidth());
			if (getWidth() > dataAttachWidth) {
				int zerobits = getWidth() - dataAttachWidth;
				BinaryNumber zero_padding = new BinaryNumber(0, zerobits);
				if (dataInName.length() > 0) {
					Concatenation padded_dataIn = new Concatenation();
					padded_dataIn.add(zero_padding);
					if (!datScalar) {
						int msb = dataAttachWidth + dataInStartBit - 1;
						int lsb = dataInStartBit;
						padded_dataIn.add(dataIn.getRange(msb, lsb));
					} else {
						dataIn = new Wire(dataInName, 1);
						padded_dataIn.add(dataIn);
					}
					d_exp = padded_dataIn;
				}

				if (dataOutName.length() > 0) {
					Wire dataOutExtra = new Wire(dataOutExtraName, zerobits);
					Concatenation padded_dataOut = new Concatenation();
					padded_dataOut.add(dataOutExtra);
					if (!datScalar) {
						int msb = dataAttachWidth + dataOutStartBit - 1;
						int lsb = dataOutStartBit;
						padded_dataOut.add(dataOut.getRange(msb, lsb));
					} else {
						dataOut = new Wire(dataOutName, 1);
						padded_dataOut.add(dataOut);
					}
					o_exp = padded_dataOut;
				}
			} else {
				if (dataInName.length() > 0) {
					if (!datScalar) {
						int msb = dataInStartBit + getWidth() - 1;
						int lsb = dataInStartBit;
						d_exp = dataIn.getRange(msb, lsb);
					} else {
						dataIn = new Wire(dataInName, 1);
						d_exp = dataIn;
					}
				}

				if (dataOutName.length() > 0) {
					if (!datScalar) {
						int msb = dataOutStartBit + getWidth() - 1;
						int lsb = dataOutStartBit;
						o_exp = dataOut.getRange(msb, lsb);
					} else {
						dataOut = new Wire(dataOutName, 1);
						o_exp = dataOut;
					}
				}
			}
			moduleInstance.connect(new Wire("D", getWidth()), d_exp);
			moduleInstance.connect(new Wire("O", getWidth()), o_exp);
		} else {
			// The data buses for <8 memories are individual
			for (int i = 0; i < getWidth(); i++) {
				Expression di_exp = new BinaryNumber(0, 1);
				if (i < dataAttachWidth) {
					if (!datScalar) {
						di_exp = dataIn.getRange(dataInStartBit + i,
								dataInStartBit + i);
					} else {
						dataIn = new Wire(dataInName, 1);
						di_exp = dataIn;
					}
				}
				if (getWidth() == 1) {
					moduleInstance.connect(new Wire("D", 1), di_exp);
				} else {
					moduleInstance.connect(new Wire("D" + i, 1), di_exp);
				}
			}
			for (int i = 0; i < getWidth(); i++) {
				Expression do_exp = new Wire("", 1);
				if (i < dataAttachWidth) {
					if (!datScalar) {
						do_exp = dataOut.getRange(dataOutStartBit + i,
								dataOutStartBit + i);
					} else {
						dataOut = new Wire(dataOutName, 1);
						do_exp = dataOut;
					}
				}
				if (getWidth() == 1) {
					moduleInstance.connect(new Wire("O", 1), do_exp);
				} else {
					moduleInstance.connect(new Wire("O" + i, 1), do_exp);
				}
			}
		}
		return moduleInstance;
	}
}
