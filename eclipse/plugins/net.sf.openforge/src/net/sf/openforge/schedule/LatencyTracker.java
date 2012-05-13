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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.sf.openforge.lim.And;
import net.sf.openforge.lim.Bus;
import net.sf.openforge.lim.Component;
import net.sf.openforge.lim.Exit;
import net.sf.openforge.lim.InBuf;
import net.sf.openforge.lim.Latch;
import net.sf.openforge.lim.Latency;
import net.sf.openforge.lim.Module;
import net.sf.openforge.lim.Mux;
import net.sf.openforge.lim.Or;
import net.sf.openforge.lim.Reg;
import net.sf.openforge.lim.Scoreboard;
import net.sf.openforge.lim.Stallboard;
import net.sf.openforge.lim.Value;

/**
 * LatencyTracker records control information for various points in a LIM graph.
 * It allows a control Bus to be specified for the entry to each
 * {@link Component} and for every {@link Exit} of a {@link Component}. It can
 * also fetch new scheduling hardware components from its internal
 * {@link OpCache}. The control Buses of these components are updated
 * automatically when they are fetched.
 * 
 * @version $Id: LatencyTracker.java 538 2007-11-21 06:22:39Z imiller $
 */
public class LatencyTracker implements LatencyCache {
	private static final boolean debug = false;

	/** The cache from which to retrieve new hardware components */
	private OpCache cache = null;

	/** Map of Exit to control Bus */
	private Map<Exit, Bus> exitControlBusMap = new HashMap<Exit, Bus>();

	/** Map of Component to control Bus */
	private Map<Component, Bus> entryControlBusMap = new HashMap<Component, Bus>();

	/** Map of control Bus to Latency */
	private Map<Bus, Latency> latencyMap = new HashMap<Bus, Latency>();

	/** Set of Latches created by this tracker. */
	private Set<Latch> latches = new HashSet<Latch>();

	/** Set of Enable Regs created by this tracker. */
	private Set<Reg> enableRegs = new HashSet<Reg>();

	/**
	 * Creates a new latency tracker with an empty OpCache.
	 */
	public LatencyTracker() {
		this(new OpCache());
	}

	/**
	 * Creates a new LatencyTracker with the same OpCache as the 'basis'
	 * LatencyTracker.
	 * 
	 * @param basis
	 *            a value of type 'LatencyTracker'
	 */
	public LatencyTracker(LatencyTracker basis) {
		this(basis.getOpCache());
	}

	/**
	 * Retrieves the current OpCache.
	 * 
	 * @return a value of type 'OpCache'
	 */
	public OpCache getOpCache() {
		return cache;
	}

	/**
	 * Sets the control Bus for an Exit.
	 */
	void setControlBus(Exit exit, Bus controlBus) {
		exitControlBusMap.put(exit, controlBus);
	}

	/**
	 * Gets the control Bus for an Exit.
	 */
	Bus getControlBus(Exit exit) {
		return exitControlBusMap.get(exit);
	}

	/**
	 * Gets the control Bus for a Bus.
	 */
	Bus getControlBus(Bus bus) {
		return getControlBus(bus.getOwner());
	}

	/**
	 * Sets the control Bus for the entry point of a Component.
	 */
	void setControlBus(Component component, Bus controlBus) {
		entryControlBusMap.put(component, controlBus);
	}

	/**
	 * Gets the control Bus for the entry point of a Component.
	 */
	@Override
	public Bus getControlBus(Component component) {
		return entryControlBusMap.get(component);
	}

	/**
	 * Defines a new control Bus.
	 * 
	 * @param bus
	 *            the bus that indicates the flow of control
	 * @param latency
	 *            the latency of the bus
	 */
	void defineControlBus(Bus bus, Latency latency) {
		assert bus != null;
		assert latency != null;
		latencyMap.put(bus, latency);
		setControlBus(bus.getOwner(), bus);
	}

	/**
	 * Gets the latency of a Bus, which is the latency of its associated control
	 * Bus.
	 */
	@Override
	public Latency getLatency(Bus bus) {
		return getLatency(bus.getOwner());
	}

	/**
	 * Gets the latency of an Exit, which is the latency of its associated
	 * control Bus.
	 */
	Latency getLatency(Exit exit) {
		return latencyMap.get(getControlBus(exit));
	}

	/**
	 * Gets the latency at the entry point of a Component, which is the the
	 * latency of the control Bus for the entry point.
	 */
	@Override
	public Latency getLatency(Component component) {
		return latencyMap.get(getControlBus(component));
	}

	/**
	 * Gets the result of inserting a Reg to a given data {@link Bus}. A control
	 * Bus is calculated for the Reg's Exit.
	 * 
	 * @param dataBus
	 *            the data bus to Reg
	 * @param module
	 *            the module to which the Reg will be added
	 * @return the Reg
	 */
	Reg getEnableReg(Bus dataBus, Module module) {
		final Bus enableBus = getControlBus(dataBus);
		final Component busOwner = dataBus.getOwner().getOwner();
		if (enableRegs.contains(busOwner)
				&& (getControlBus(busOwner) == enableBus)) {
			/*
			 * Instead of latching the output of a latch with the same enable
			 * bus, just return the original latch.
			 */
			return (Reg) busOwner;
		}

		final Reg reg = cache.getEnableReg(dataBus, enableBus);
		enableRegs.add(reg);
		if (reg.getOwner() == null) {
			module.addComponent(reg);
		}
		module.setConsumesGo(true); // CRSS if we add an enabled, we need the go
		setControlBus(reg, enableBus);
		final Reg controlReg = getControlReg(enableBus, module);
		setControlBus(reg.getResultBus().getOwner(), controlReg.getResultBus());
		return reg;
	}

	/**
	 * Gets the result of latching a given data {@link Bus}. A control Bus is
	 * calculated for the Latch's Exit.
	 * 
	 * @param dataBus
	 *            the data bus to latch
	 * @param module
	 *            the module to which the latch will be added
	 * @return the latch
	 */
	Latch getLatch(Bus dataBus, Module module) {
		final Bus enableBus = getControlBus(dataBus);
		final Component busOwner = dataBus.getOwner().getOwner();
		if (latches.contains(busOwner)
				&& (getControlBus(busOwner) == enableBus)) {
			/*
			 * Instead of latching the output of a latch with the same enable
			 * bus, just return the original latch.
			 */
			return (Latch) busOwner;
		}

		final Latch latch = cache.getLatch(dataBus, enableBus);
		latches.add(latch);
		if (latch.getOwner() == null) {
			module.addComponent(latch);
		}
		module.setConsumesGo(true); // CRSS if we add an enabled, we need the go
		setControlBus(latch, enableBus);
		setControlBus(latch.getResultBus().getOwner(), enableBus);
		return latch;
	}

	/**
	 * Gets the result of scoreboarding a collection of control {@link Bus
	 * Buses}. A control Bus is calculated for the Scoreboard's Exit.
	 * 
	 * @param controlBuses
	 *            a collection of control {@link Bus Buses}
	 * @param module
	 *            the module to which the scoreboard will be added
	 * @return the scoreboard
	 */
	Scoreboard getScoreboard(Collection<Bus> controlBuses, Module module) {
		final Scoreboard scoreboard = cache.getScoreboard(controlBuses);
		if (scoreboard.getOwner() == null) {
			module.addComponent(scoreboard);
		}
		final Exit exit = scoreboard.getDoneBus().getOwner();

		final Set<Latency> latencies = new HashSet<Latency>(controlBuses.size());
		// final Map latencies = new LinkedHashMap(controlBuses.size());
		for (Bus bus : controlBuses) {
			// latencies.put(bus, getLatency(bus));
			latencies.add(getLatency(bus));
		}
		defineControlBus(scoreboard.getResultBus(),
				Latency.and(latencies, exit));
		return scoreboard;
	}

	Stallboard getStallboard(Collection<Bus> controlBuses, Module module) {
		final Stallboard stallboard = cache.getStallboard(controlBuses);
		if (stallboard.getOwner() == null) {
			module.addComponent(stallboard);
		}
		final Exit exit = stallboard.getDoneBus().getOwner();
		final Set<Latency> latencies = new HashSet<Latency>(controlBuses.size());
		// final Map latencies = new LinkedHashMap(controlBuses.size());
		for (Bus bus : controlBuses) {
			// latencies.put(bus, getLatency(bus));
			latencies.add(getLatency(bus));
		}
		/*
		 * The latency of a stall board is always open because the stall input
		 * is feedback and cannot be compared to any other latency.
		 */
		defineControlBus(stallboard.getResultBus(), Latency
				.and(latencies, exit).open(exit));
		return stallboard;
	}

	/**
	 * Gets the result of registering a given data input Bus. A control Bus is
	 * calculated for the Reg's Exit; a control Reg is created for this purpose.
	 * 
	 * @param inputBus
	 *            the data bus to be registered
	 * @param module
	 *            the module to which the new register (and control flop
	 *            register) will be added
	 */
	Reg getDataReg(Bus inputBus, Module module) {
		final Reg reg = cache.getReg(inputBus);
		if (reg.getOwner() == null) {
			module.addComponent(reg);
		}

		/*
		 * We can only set the size of a bus once, so check to see if it was
		 * done already, since we might be fetching a register that was already
		 * cached.
		 */
		if (reg.getResultBus().getValue() == null) {
			final Value inputValue = inputBus.getValue();
			reg.getResultBus().setSize(inputValue.getSize(),
					inputValue.isSigned());
		}

		final Bus inputControlBus = getControlBus(inputBus);
		final Reg controlReg = getControlReg(inputControlBus, module);
		setControlBus(reg.getResultBus().getOwner(), controlReg.getResultBus());
		return reg;
	}

	/**
	 * Gets the {@link Reg} that results from delaying a given control input
	 * {@link Bus} by a single clock. The output Bus of the Reg will be defined
	 * to be a control Bus.
	 * 
	 * @param inputBus
	 *            the control bus to be delayed by one clock
	 * @param module
	 *            the module to which the register will be added
	 * @return the register used to delay the bus
	 */
	Reg getControlReg(Bus inputBus, Module module) {
		final Reg reg = cache.getReg(inputBus, true);
		// Set the Reg to be done synchronous so that any module using
		// this Reg in its control chain knows that the done that this
		// reg produces is synchronous.
		reg.setIsSyncDone(true);

		if (reg.getOwner() == null) {
			module.addComponent(reg);
		}

		/*
		 * We can only set the size of a bus once, so check to see if it was
		 * done already, since we might be fetching a register that was already
		 * cached.
		 */
		if (reg.getResultBus().getValue() == null) {
			// final Value inputValue = inputBus.getValue();
			reg.getResultBus().setSize(1, false);
		}

		final Latency regLatency = reg.getExit(Exit.DONE).getLatency();
		final Latency exitLatency = regLatency.addTo(getLatency(inputBus));
		defineControlBus(reg.getResultBus(), exitLatency);
		return reg;
	}

	/*
	 * Gets the {@link Bus} that results from delaying a given control {@link
	 * Bus} by a specified number of clocks.
	 * 
	 * @param inputBus the control bus to be delayed
	 * 
	 * @param module the module to which new registers will be added
	 * 
	 * @param clocks the number of clocks by which to delay the bus
	 * 
	 * @return the delayed bus
	 */
	Bus delayControlBus(Bus inputBus, Module module, int clocks) {
		Bus nextBus = inputBus;
		for (int i = 0; i < clocks; i++) {
			nextBus = getControlReg(nextBus, module).getResultBus();
		}
		return nextBus;
	}

	/*
	 * Gets the {@link Bus} that results from delaying a given data {@link Bus}
	 * by a specified number of clocks.
	 * 
	 * @param inputBus the control bus to be delayed
	 * 
	 * @param module the module to which new registers will be added
	 * 
	 * @param clocks the number of clocks by which to delay the bus
	 * 
	 * @return the delayed bus
	 */
	Bus delayDataBus(Bus inputBus, Module module, int clocks) {
		Bus nextBus = inputBus;
		for (int i = 0; i < clocks; i++) {
			nextBus = getDataReg(nextBus, module).getResultBus();
		}
		return nextBus;
	}

	/**
	 * Gets the result of ORing a collection of control {@link Bus Buses}. A
	 * control Bus for the Exit of the Or will also be calculated.
	 * 
	 * @param controlBuses
	 *            a collection of control {@link Bus Buses}
	 * @param module
	 *            the module to which the Or will be added
	 * @return the new OR gate as retrieved from the cache
	 */
	Or getOr(Collection<Bus> controlBuses, Module module) {
		final Or or = cache.getOr(controlBuses);
		if (or.getOwner() == null) {
			module.addComponent(or);
		}
		final Exit exit = or.getResultBus().getOwner();
		final Set<Latency> latencies = new HashSet<Latency>(controlBuses.size());
		// final Map latencies = new LinkedHashMap(controlBuses.size());
		for (Bus bus : controlBuses) {
			// latencies.put(bus, getLatency(bus));
			latencies.add(getLatency(bus));
		}

		defineControlBus(or.getResultBus(), Latency.or(latencies, exit));
		return or;
	}

	/**
	 * Gets the result of ANDing a collection of control {@link Bus Buses}. A
	 * control Bus for the Exit of the And will also be calculated.
	 * 
	 * @param controlBuses
	 *            a collection of control {@link Bus Buses}
	 * @param module
	 *            the module to which the And will be added
	 * @return the new AND gate as retrieved from the cache
	 */
	And getAnd(Collection<Bus> controlBuses, Module module) {
		final And and = cache.getAnd(controlBuses);
		if (and.getOwner() == null) {
			module.addComponent(and);
		}
		final Exit exit = and.getResultBus().getOwner();

		final Set<Latency> latencies = new HashSet<Latency>(controlBuses.size());
		// final Map latencies = new LinkedHashMap(controlBuses.size());
		for (Bus bus : controlBuses) {
			// latencies.put(bus, getLatency(bus));
			latencies.add(getLatency(bus));
		}
		defineControlBus(and.getResultBus(), Latency.and(latencies, exit));
		return and;
	}

	/**
	 * Gets the result of MUXing a collection of data {@link Bus Buses}. A
	 * control Bus will also be calculated for the Exit of the Mux.
	 * 
	 * @param selectBuses
	 *            a list of control {@link Bus Buses}, one for each data bus
	 * @param dataBuses
	 *            a list of data {@link Bus Buses}, one for each select bus
	 * @param module
	 *            the module to which the Mux will be added
	 * @return the new MUX as retrieved from the cache
	 */
	Mux getMux(List<Bus> selectBuses, List<Bus> dataBuses, Module module) {
		final Mux mux = cache.getMux(selectBuses, dataBuses);
		if (mux.getOwner() == null) {
			module.addComponent(mux);
		}
		final Exit exit = mux.getResultBus().getOwner();

		final Map<Bus, Latency> latencies = new LinkedHashMap<Bus, Latency>(
				selectBuses.size());
		for (Bus bus : selectBuses) {
			latencies.put(bus, getLatency(bus));
		}

		final Or or = getOr(selectBuses, module);
		setControlBus(exit, or.getResultBus());
		return mux;
	}

	/**
	 * Calculates a control Bus for each Exit of a given Component based upon
	 * its entry control Bus and the relative latencies of its Exits.
	 */
	void updateExitStates(Component component) {
		/*
		 * Look at each Exit separately.
		 */
		final Bus entryControlBus = getControlBus(component);
		final Latency entryLatency = getLatency(component);
		for (Exit exit : component.getExits()) {
			/*
			 * Get the component's internal latency and add it to the entry
			 * latency to get the cumulative latency for the Exit.
			 */
			final Latency exitLatency = exit.getLatency();
			// IDM. 01/13/2005 I would expect MOST exit latencies to
			// be ZERO (combinational components). Instead of
			// creating a new latency for the 'cumulative' latency,
			// just use the entry latency if this is the case. Saves
			// time and memory.
			final Latency cumulativeLatency;
			if (exitLatency.equals(Latency.ZERO)) {
				cumulativeLatency = entryLatency;
			} else {
				cumulativeLatency = exitLatency.addTo(entryLatency);
			}
			if (debug)
				System.out.println("updateExitState " + component + " " + exit
						+ " exitLat " + exitLatency + " entryLat "
						+ entryLatency + " cumLat " + cumulativeLatency);

			/*
			 * Determine the done control bus for the exit.
			 */
			Bus exitControlBus = entryControlBus;

			if (component.producesDone()) {
				if (component.isDoneSynchronous()
						|| isPlaceholder(entryControlBus)) {
					if (debug)
						System.out.println("\town done and isSync");
					/*
					 * If the component produces a clocked done or if the entry
					 * bus doesn't supply a control signal, then use the exit's
					 * done bus.
					 */
					defineControlBus(exit.getDoneBus(), cumulativeLatency);
					exitControlBus = exit.getDoneBus();
				} else {
					if (debug)
						System.out
								.println("\town done and NOT Sync, generating AND");
					/*
					 * Otherwise AND the component's done bus with the entry's
					 * control bus to get a valid done bus for the exit.
					 */
					final Collection<Bus> andBuses = new ArrayList<Bus>(2);
					andBuses.add(exit.getDoneBus());
					andBuses.add(entryControlBus);
					final And and = cache.getAnd(andBuses);
					if (and.getOwner() == null) {
						component.getOwner().addComponent(and);
					}
					defineControlBus(and.getResultBus(), cumulativeLatency);
					exitControlBus = and.getResultBus();
				}
			} else {
				if (debug)
					System.out.println("\tno done, creating flop chaing");
				/*
				 * If the component does not produce a done, the Exit must have
				 * a fixed latency. A chain of zero or more flops is created in
				 * parallel with the component to represent the flow of control.
				 * This chain may end up getting removed if it is not used
				 * further on in the control flow.
				 */
				assert exitLatency.isFixed() : "Exit has no done, but Latency = "
						+ exitLatency + " on " + component;
				for (int i = 0; i < exitLatency.getMinClocks(); i++) {
					final Reg flop = getControlReg(exitControlBus,
							component.getOwner());
					exitControlBus = flop.getResultBus();
				}
			}

			setControlBus(exit, exitControlBus);
		}
	}

	/**
	 * Tests whether a given ControlState is just a placeholder. That is, its
	 * {@link Bus} is not an active control bus; it is just there to record the
	 * {@link Latency} at that point. Currently, the only placeholder state that
	 * is created is for {@link InBuf InBufs} that do not have an active done
	 * {@link Bus}.
	 * 
	 * @param state
	 *            a control state
	 * @return true if the state's bus is the done of an {@link InBuf} whose
	 *         owner does not consume a go
	 */
	static boolean isPlaceholder(Bus bus) {
		final Component busOwner = bus.getOwner().getOwner();
		final Module module = busOwner.getOwner();
		return (busOwner == module.getInBuf()) && !module.consumesGo();
	}

	private LatencyTracker(OpCache cache) {
		super();
		this.cache = cache;
	}
}
