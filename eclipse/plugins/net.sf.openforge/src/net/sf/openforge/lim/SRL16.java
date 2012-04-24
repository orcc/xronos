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

package net.sf.openforge.lim;

import java.util.Iterator;
import java.util.List;

import net.sf.openforge.report.FPGAResource;
import net.sf.openforge.util.naming.ID;

/**
 * SRL16
 * 
 * A SRL16 now has input ports for n-bit data, 4-bits shifting stage(from 1 to
 * 16), one output port for n-bit result with optional enable port.
 * 
 * Created: Mon Oct 21 15:04:27 2002
 * 
 * @author cwu
 * @version $Id: SRL16.java 2 2005-06-09 20:00:48Z imiller $
 */
public class SRL16 extends Primitive {

	public static final int SRL16 = 0x0;
	public static final int SRL16E = 0x1;
	public static final int SRL16_1 = 0x2;

	public static final int MAX_STAGE = 16;

	private int type;

	/** A list of regs being compacted and replaced by SRL16/SRL16E */
	private List regs;

	/**
	 * Constructs a SRL16 with stages equals size of registers chain and given
	 * id
	 * 
	 */
	private SRL16(List regs_chain, String id) {
		// always create 1 data port, 4 stage ports, and 1 optional
		// enable port.
		// Create just the data port and the optional enable port.
		super(2);
		setIDLogical(id);

		regs = regs_chain;

		// ports are always used.
		for (Iterator iter = getDataPorts().iterator(); iter.hasNext();) {
			((Port) iter.next()).setUsed(true);
		}

		// the clock is always used, and must be connected by the user
		getClockPort().setUsed(true);

		getExit(Exit.DONE).setLatency(Latency.get(getStages()));
		getResultBus().setIDLogical(ID.showLogical(this) + "_result");
	}

	public static SRL16 createSRL16(List chain) {
		SRL16 srl_16 = new SRL16(chain, "SRL16");

		srl_16.type = SRL16;

		// disable enable port
		srl_16.getEnablePort().setUsed(false);

		return srl_16;
	}

	public static SRL16 createSRL16E(List chain) {
		SRL16 srl_16e = new SRL16(chain, "SRL16E");

		srl_16e.type = SRL16E;

		return srl_16e;
	}

	public static SRL16 createSRL16_1(List chain) {
		SRL16 srl_16_1 = new SRL16(chain, "SRL16_1");

		srl_16_1.type = SRL16_1;

		// disable enable port
		srl_16_1.getEnablePort().setUsed(false);

		return srl_16_1;
	}

	public int getStages() {
		return regs.size();
	}

	public List getCompactedRegs() {
		return regs;
	}

	public int getType() {
		return type;
	}

	public Port getInDataPort() {
		return getDataPorts().get(0);
	}

	public Port getEnablePort() {
		return getDataPorts().get(1);
	}

	@Override
	public void accept(Visitor v) {
		v.visit(this);
	}

	/**
	 * Gets the FPGA hardware resource usage of this component.
	 * 
	 * @return a FPGAResource objec
	 */
	@Override
	public FPGAResource getHardwareResourceUsage() {
		int srl16Count = 0;

		Value inputValue = getInDataPort().getValue();
		for (int i = 0; i < inputValue.getSize(); i++) {
			Bit inputBit = inputValue.getBit(i);
			if (inputBit.isCare()) {
				srl16Count++;
			}
		}

		FPGAResource hwResource = new FPGAResource();
		hwResource.addSRL16(srl16Count);

		return hwResource;
	}

	/*
	 * =================================================== Begin new constant
	 * prop rules implementation.
	 */

	/**
	 * Bit states propagate straight through, except that pass through bits
	 * become care bits.
	 */
	@Override
	public boolean pushValuesForward() {
		boolean mod = false;

		Value drivenValue = getInDataPort().getValue();

		Value newValue = new Value(drivenValue.getSize(),
				drivenValue.isSigned());

		for (int i = 0; i < drivenValue.getSize(); i++) {
			if (!drivenValue.getBit(i).isCare()
					|| drivenValue.getBit(i).isConstant()) {
				newValue.setBit(i, drivenValue.getBit(i));
			}
		}

		/*
		 * Don't neglect the enable Port.
		 */
		final Port enablePort = getEnablePort();
		if (enablePort.isUsed() && enablePort.isConnected()) {
			enablePort.setBus(enablePort.getBus());
		}

		mod |= getResultBus().pushValueForward(newValue);

		return mod;
	}

	/**
	 * Any bit of the consumed output that is dont care or constant becomes a
	 * dont care or that constant on the inputs, all other bits are care.
	 */
	@Override
	public boolean pushValuesBackward() {
		boolean mod = false;

		Value resultBusValue = getResultBus().getValue();

		Value newValue = new Value(resultBusValue.getSize(),
				resultBusValue.isSigned());

		for (int i = 0; i < resultBusValue.getSize(); i++) {
			if (!resultBusValue.getBit(i).isCare()) {
				newValue.setBit(i, Bit.DONT_CARE);
			}
		}

		mod |= getInDataPort().pushValueBackward(newValue);

		return mod;
	}

	/*
	 * End new constant prop rules implementation.
	 * =================================================
	 */

	@Override
	public String toString() {
		String ret = super.toString();

		return ret;
	}
}
