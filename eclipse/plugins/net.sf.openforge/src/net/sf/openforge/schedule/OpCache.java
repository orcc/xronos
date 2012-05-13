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

package net.sf.openforge.schedule;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.sf.openforge.lim.And;
import net.sf.openforge.lim.Bus;
import net.sf.openforge.lim.Latch;
import net.sf.openforge.lim.Mux;
import net.sf.openforge.lim.Or;
import net.sf.openforge.lim.Port;
import net.sf.openforge.lim.Reg;
import net.sf.openforge.lim.Scoreboard;
import net.sf.openforge.lim.Stallboard;
import net.sf.openforge.optimize.constant.TwoPassPartialConstant;

/**
 * OpCache is a simple and convenient way to keep track of structures that have
 * already been created in the LIM and reuse them when applicable. Any
 * scheduling artifacts (scoreboards, registers, etc) created during scheduling
 * should use the cache to avoid duplicate nodes.
 * 
 * 
 * Created: Tue May 14 10:23:00 2002
 * 
 * @author imiller
 * @version $Id: OpCache.java 280 2006-08-11 17:00:32Z imiller $
 */
public class OpCache {

	private Map<Bus, Reg> eregMap = new HashMap<Bus, Reg>();
	private Map<Set<Bus>, Scoreboard> scoreMap = new HashMap<Set<Bus>, Scoreboard>();
	private Map<Bus, Latch> latchMap = new HashMap<Bus, Latch>();
	private Map<Bus, Reg> regMap = new HashMap<Bus, Reg>();
	private Map<Set<Bus>, Or> orMap = new HashMap<Set<Bus>, Or>();
	private Map<Set<Bus>, And> andMap = new HashMap<Set<Bus>, And>();
	private Map<Set<Bus>, Mux> muxMap = new HashMap<Set<Bus>, Mux>();

	public OpCache() {
	}

	/**
	 * Returns an enabled (but NOT reset) registered version of the specified
	 * bus
	 * 
	 * @param dataBus
	 *            a <code>Bus</code> value
	 * @param enableBus
	 *            a <code>Bus</code> value
	 * @return a <code>Reg</code> value
	 */
	public Reg getEnableReg(Bus dataBus, Bus enableBus) {
		Reg reg = eregMap.get(dataBus);
		if (reg == null) {
			reg = Reg.getConfigurableReg(Reg.REGE, "syncEnable");
			reg.getDataPort().setBus(dataBus);
			reg.getEnablePort().setBus(enableBus);
			TwoPassPartialConstant.propagateQuiet(reg);
			eregMap.put(dataBus, reg);
		}
		assert reg.getEnablePort().getBus() == enableBus;
		return reg;
	}

	public Latch getLatch(Bus dataBus, Bus enableBus) {
		Latch latch = latchMap.get(dataBus);
		if (latch == null) {
			latch = new Latch();
			latch.getDataPort().setBus(dataBus);
			latch.getEnablePort().setBus(enableBus);
			TwoPassPartialConstant.propagateQuiet(latch);
			latchMap.put(dataBus, latch);
		}
		assert latch.getEnablePort().getBus() == enableBus;
		return latch;
	}

	Scoreboard getScoreboard(Collection<Bus> controlBuses) {
		final Set<Bus> uniqueControlBuses = new LinkedHashSet<Bus>(controlBuses);
		Scoreboard scoreboard = scoreMap.get(uniqueControlBuses);
		if (scoreboard == null) {
			scoreboard = new Scoreboard(uniqueControlBuses);
			TwoPassPartialConstant.propagateQuiet(scoreboard);
			scoreMap.put(uniqueControlBuses, scoreboard);
		}
		return scoreboard;
	}

	/**
	 * Stallboards are never cached because their stall inputs are determined
	 * post-scheduling.
	 * 
	 * @param controlBuses
	 *            a Collection of Bus objects to be stallboarded.
	 * @return a non-null, unique Stallboard
	 */
	Stallboard getStallboard(Collection<Bus> controlBuses) {
		final Set<Bus> uniqueControlBuses = new LinkedHashSet<Bus>(controlBuses);
		Stallboard stallboard = new Stallboard(uniqueControlBuses);
		TwoPassPartialConstant.propagateQuiet(stallboard);
		return stallboard;
	}

	Reg getReg(Bus inputBus) {
		return getReg(inputBus, false);
	}

	Reg getReg(Bus inputBus, boolean resetable) {
		// NOTE: This assumes that the type of register will always be
		// the same for a given input bus
		Reg reg = regMap.get(inputBus);
		if (reg == null) {
			if (resetable) {
				reg = Reg.getConfigurableReg(Reg.REGR, null);
				// We have to pick up the reset to the internal reset
				// port. So grab it from the bus owners (owner) module
				reg.getInternalResetPort().setBus(
						inputBus.getOwner().getOwner().getOwner()
								.getResetPort().getPeer());
			} else {
				reg = Reg.getConfigurableReg(Reg.REG, null);
			}
			reg.getDataPort().setBus(inputBus);
			reg.propagateValuesForward();
			regMap.put(inputBus, reg);
		}
		// Ensure consistency
		if ((reg.getType() & Reg.RESET) != 0)
			assert resetable : "Changed type of register for input bus";
		else
			assert !resetable : "Changed type of register for input bus";

		return reg;
	}

	/**
	 * returns an or with the set of buses as inputs
	 * 
	 * @param buses
	 *            collection of buses which will be connected to the input of
	 *            the Or
	 * @return the Or, with buses connected. NOTE there will be no dependencies
	 *         created or connected
	 */
	public Or getOr(Collection<Bus> buses) {
		final Set<Bus> uniqueBuses = new LinkedHashSet<Bus>(buses);
		Or or = orMap.get(uniqueBuses);
		if (or == null) {
			or = new Or(uniqueBuses.size());
			int i = 0;
			for (Bus bus : uniqueBuses) {
				Port port = or.getDataPorts().get(i);
				port.setBus(bus);
				i++;
			}
			or.propagateValuesForward();
			orMap.put(uniqueBuses, or);
		}
		return or;
	}

	/**
	 * Returns an And with the set of buses as inputs
	 * 
	 * @param buses
	 *            collection of buses which will be connected to the input of
	 *            the And
	 * @return the And, with its Ports connected to the given Buses
	 */
	public And getAnd(Collection<Bus> buses) {
		final Set<Bus> uniqueBuses = new LinkedHashSet<Bus>(buses);
		And and = andMap.get(uniqueBuses);
		if (and == null) {

			// and = new And(buses.size());
			and = new And(uniqueBuses.size());
			int i = 0;
			for (Bus bus : uniqueBuses) {
				Port port = and.getDataPorts().get(i);
				port.setBus(bus);
				i++;
			}
			and.propagateValuesForward();
			andMap.put(uniqueBuses, and);
		}
		return and;
	}

	/**
	 * returns a Mux with the set of buses as inputs
	 * 
	 * @param goBuses
	 *            list of go buses which will be connected to the go inputs of
	 *            the Mux
	 * @param dataBuses
	 *            list of data buses which will be connected to the data inputs
	 *            of the Mux
	 * @return the Mux, with buses connected. NOTE there will be no dependencies
	 *         created or connected
	 */
	public Mux getMux(List<Bus> goBuses, List<Bus> dataBuses) {
		if (goBuses.size() != dataBuses.size()) {
			throw new IllegalArgumentException(
					"mismatched number of go and data buses");
		}

		final Set<Bus> uniqueBuses = new LinkedHashSet<Bus>(goBuses);
		uniqueBuses.addAll(dataBuses);
		Mux mux = muxMap.get(uniqueBuses);
		if (mux == null) {
			mux = new Mux(goBuses.size());
			List<Port> goPorts = mux.getGoPorts();

			for (int i = 0; i < goPorts.size(); i++) {
				Port goPort = goPorts.get(i);
				Bus bus = goBuses.get(i);
				goPort.setBus(bus);

				Port dataPort = mux.getDataPort(goPort);
				bus = dataBuses.get(i);
				dataPort.setBus(bus);
			}
			mux.propagateValuesForward();
			muxMap.put(uniqueBuses, mux);
		}
		return mux;
	}

}
