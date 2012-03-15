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

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import net.sf.openforge.util.SizedInteger;
import net.sf.openforge.util.naming.ID;

/**
 * Latch is so not a subclass of reg. Rather, latch is the composition of of an
 * enabled reg with reset, and a 2:1 mux. Out out damn reg relation!
 * 
 * @version $Id: Latch.java 280 2006-08-11 17:00:32Z imiller $
 */
public class Latch extends Module implements Composable, Emulatable {

	private static final int GATE_DEPTH = 2;

	/** The register which holds the value */
	private Reg reg;

	/** The mux which selects the output value */
	private Mux mux;

	private Bus resultBus;

	//private boolean initialized = false;

	public Reg getRegister() {
		return reg;
	}

	public Mux getMux() {
		return mux;
	}

	/**
	 * A latch does not require a connection to its GO, but it does require a
	 * control signal to enable it.
	 * 
	 * @return a value of type 'boolean'
	 */
	public boolean consumesGo() {
		return true;
	}

	/**
	 * Constructs a new Latch.
	 */
	public Latch() {
		// ABK -- No. There is no Done. There is no Go.
		// There is only the explicit enable port.
		Exit exit = makeExit(0);

		// No reset needed, data path element only and no reset value
		addComponent(this.reg = Reg.getConfigurableReg(Reg.REGE,
				makeName("_reg")));
		addComponent(this.mux = new Mux(2)); // create 2 input mux
		this.mux.getResultBus().setIDLogical(makeName("_out"));
		this.resultBus = (Bus) exit.makeDataBus();
		resultBus.setUsed(true);

		// make two data ports
		makeDataPort();
		// makeDataPort();

		// set them used
		getEnablePort().setUsed(true);
		getDataPort().setUsed(true);

		// clock and reset are used ...
		getClockPort().setUsed(true);
		// getResetPort().setUsed(true);

		exit.getDoneBus().setUsed(false);
		// getGoPort().setUsed(false);

		// Registers have a latency of 1, but a latch has a
		// combinational path through it whenever the enable is held
		// high, thus it's latency is 0, but it has the nice feature
		// of remembering it's data.
		exit.setLatency(Latency.ZERO);

		// now, construct the guts of this.
		// create a sync reg with enable and reset
		//Entry regEntry = reg.makeEntry(getInBuf().getExit(Exit.DONE));

		reg.getClockPort().setBus(getClockPort().getPeer()); // connect clock
		reg.getDataPort().setBus(getDataPort().getPeer()); // connect data
		reg.getEnablePort().setBus(getEnablePort().getPeer()); // connect enable

		//Entry muxEntry = mux.makeEntry(reg.getResultBus().getOwner());
		List<Port> muxEnablesList = mux.getGoPorts();
		// 0th entry is DataPort if EnablePort
		// 1th entry is r.resultBus is !EnablePort

		// 0th
		Port sel = (Port) muxEnablesList.get(0);
		Port data = mux.getDataPort(sel);
		sel.setBus(getEnablePort().getPeer());
		data.setBus(getDataPort().getPeer());

		// 1th
		sel = (Port) muxEnablesList.get(1);
		data = mux.getDataPort(sel);

		// This sel will NOT be used b/c this is a 2:1 mux and
		// translation only uses the 0th select port
		sel.setBus(getEnablePort().getPeer());
		data.setBus(reg.getResultBus());

		// finally, connect the resultbus from the mux to resultbus for the
		// module
		resultBus.getPeer().setBus(mux.getResultBus());
		//Entry outEntry = resultBus.getPeer().getOwner()
		//		.makeEntry(mux.getResultBus().getOwner());

		resultBus.setIDLogical(makeName("_result"));
	}

	public void accept(Visitor v) {
		v.visit(this);
	}

	/**
	 * Gets the data {@link Port Port} which is the 0'th port in the
	 * getDataPorts() list.
	 * 
	 * @return a value of type 'Port'
	 */
	public Port getDataPort() {
		return (Port) getDataPorts().get(0);
	}

	/**
	 * Get the data {@link Port Port} which is the 1'th port in the list
	 * 
	 * @return a value of type 'Port'
	 */
	public Port getEnablePort() {
		// return (Port)getDataPorts().get(1);
		return getGoPort();
	}

	/**
	 * Gets the data {@link Bus}.
	 */
	public Bus getResultBus() {
		return resultBus;
	}

	/**
	 * Throws an exception, replacement in this class not supported.
	 */
	public boolean replaceComponent(Component removed, Component inserted) {
		throw new UnsupportedOperationException("Cannot replace components in "
				+ getClass());
	}

	/**
	 * Performes a high level numerical emulation of this component.
	 * 
	 * @param portValues
	 *            a map of {@link Port} to
	 *            {@link SizedInteger} input value
	 * @return a map of {@link Bus} to
	 *         {@link SizedInteger} result value
	 */
	public Map<Bus, SizedInteger> emulate(Map<Port, SizedInteger> portValues) {
		return Collections.singletonMap(getResultBus(),
				portValues.get(getDataPort()));
	}

	/**
	 * Calls the super, then removes any reference to the given bus in this
	 * class.
	 */
	public boolean removeDataBus(Bus bus) {
		if (super.removeDataBus(bus)) {
			if (bus == this.resultBus)
				this.resultBus = null;
			return true;
		}
		return false;
	}

	/**
	 * Gets the gate depth of this component. This is the maximum number of
	 * gates that any input signal must traverse before reaching an {@link Exit}
	 * .
	 * 
	 * @return a non-negative integer
	 */
	public int getGateDepth() {
		return GATE_DEPTH;
	}

	/**
	 * Tests whether this component requires a connection to its clock
	 * {@link Port}.
	 */
	public boolean consumesClock() {
		return true;
	}

	/**
	 * Tests whether this component requires a connection to its reset
	 * {@link Port}. By default, returns the value of
	 * {@link Component#consumesClock()}.
	 */
	public boolean consumesReset() {
		return true;
	}

	/**
	 * Actually Latches shouldn't preclude balanced scheduling since they are
	 * inserted during scheduling in response to a non-balanced condition.
	 * However, the only place they exist pre-scheduling is in Loops and we
	 * account for this in the {@link Loop#isBalanceable}
	 * 
	 * @return false
	 */
	public boolean isBalanceable() {
		return false;
	}

	protected void cloneNotify(Module moduleClone, Map cloneMap) {
		super.cloneNotify(moduleClone, cloneMap);
		Latch clone = (Latch) moduleClone;
		clone.reg = (Reg) cloneMap.get(reg);
		clone.mux = (Mux) cloneMap.get(mux);
		clone.resultBus = getBusClone(resultBus, cloneMap);

		// This is an ugly fix to a bizarre problem. The latches in
		// the blockIO statemachine were being cloned during loop
		// unrolling. b/c the reg was getting its ID information from
		// the original latches reg the resulting 'reg' object in the
		// verilog had the same name for all the cloned copies. This
		// fixes that problem.
		clone.setIDLogical(clone.showIDGlobal());

		clone.reg.setIDLogical(clone.makeName("_reg"));
		clone.reg.getResultBus().setIDLogical(clone.makeName("_reg"));

		clone.resultBus.setIDLogical(clone.makeName("_result"));
		clone.mux.getResultBus().setIDLogical(clone.makeName("_out"));
	}

	private String makeName(String suffix) {
		return ID.showLogical(this) + suffix;
	}

	public String debug() {
		String ret = super.toString();
		for (Port port : getPorts()) {
			if (port == getGoPort())
				ret = ret + " go:" + port;
			else if (port == getClockPort())
				ret = ret + " ck:" + port;
			else if (port == getResetPort())
				ret = ret + " rs:" + port;
			else if (port == getEnablePort())
				ret = ret + " en:" + port;
			else if (port == getDataPort())
				ret = ret + " dat:" + port;
			else
				ret = ret + " ??:" + port;
		}
		return ret;
	}

}// Latch
