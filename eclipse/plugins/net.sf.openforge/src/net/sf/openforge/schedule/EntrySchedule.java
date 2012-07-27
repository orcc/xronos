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
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import net.sf.openforge.lim.Bus;
import net.sf.openforge.lim.Component;
import net.sf.openforge.lim.Dependency;
import net.sf.openforge.lim.Entry;
import net.sf.openforge.lim.Exit;
import net.sf.openforge.lim.Latency;
import net.sf.openforge.lim.Module;
import net.sf.openforge.lim.Port;
import net.sf.openforge.lim.op.Constant;
import net.sf.openforge.lim.op.SimpleConstant;
import net.sf.openforge.lim.primitive.Mux;
import net.sf.openforge.lim.primitive.Or;

/**
 * EntrySchedule derives the scheduled {@link Bus} for each {@link Port}
 * according to the {@link Dependency Dependencies} of a single {@link Entry}.
 * 
 * @version $Id: EntrySchedule.java 2 2005-06-09 20:00:48Z imiller $
 */
class EntrySchedule {

	/** The entry whose dependencies are being scheduled */
	protected Entry entry;

	/** The LatencyTracker to use for scheduling this entry */
	protected LatencyTracker tracker;

	/** The control bus calculated for this entry */
	protected Bus controlBus = null;

	/** Map of Port to the Bus that was scheduled for it, if any */
	private Map<Port, Bus> busMap;

	/** True if a balanced schedule is being produced */
	private boolean isBalancing;

	/**
	 * Constructs an EntrySchedule for a given {@link Entry}.
	 * 
	 * @param entry
	 *            the entry to be scheduled
	 * @param tracker
	 *            the current {@link LatencyTracker}
	 */
	EntrySchedule(Entry entry, LatencyTracker tracker, boolean isBalancing) {
		this.entry = entry;
		this.tracker = tracker;
		busMap = new HashMap<Port, Bus>(entry.getOwner().getPorts().size());
		this.isBalancing = isBalancing;
	}

	/**
	 * Constructor for unit testing.
	 */
	EntrySchedule() {
		tracker = new LatencyTracker();
		busMap = new LinkedHashMap<Port, Bus>();
		isBalancing = false;
	}

	/**
	 * Gets the {@link Entry} being schedule.
	 */
	Entry getEntry() {
		return entry;
	}

	Bus getControlBus() {
		return controlBus;
	}

	/**
	 * For each {@link Port} of a {@link Component}, merge and connect the
	 * scheduled {@link Bus Buses} specified by one or more
	 * {@link EntrySchedule EntrySchedules}. This will also set the entry
	 * {@link ControlState} for the {@link Component} in the
	 * {@link LatencyTracker}.
	 * 
	 * @param component
	 *            the component whose ports are to be connected
	 * @param entrySchedules
	 *            a collection of {@link EntrySchedule}, one for each
	 *            {@link Entry} of the given component
	 * @param tracker
	 *            the current {@link LatencyTracker}
	 * @param isBalancing
	 *            a value of type 'boolean'
	 * @return a Set of the components that were added to the components owner
	 *         in order to schedule that component. The collection may contain
	 *         some of the same components returned by scheduling of other
	 *         components due to hardware caching.
	 */
	static void merge(Component component,
			Collection<EntrySchedule> entrySchedules, LatencyTracker tracker,
			boolean isBalancing) {
		/** Map of component's Port to the Bus that was calculated for it */
		final Map<Port, Bus> connectionMap = new HashMap<Port, Bus>();

		if (entrySchedules.isEmpty()) {
			/*
			 * No entry schedules means there were no dependencies for this
			 * component. Tie off GO and DATA ports to 0.
			 */
			final Module module = component.getOwner();
			final Bus entryControlBus = tracker.getControlBus(module.getInBuf()
					.getExit(Exit.DONE));
			tracker.setControlBus(component, entryControlBus);

			if (component.consumesGo()) {
				final Constant goConst = new SimpleConstant(0, 1, false);
				module.addComponent(goConst);
				connectionMap.put(component.getGoPort(), goConst.getValueBus());
			}

			for (Port port : component.getDataPorts()) {
				final Constant dataConst = new SimpleConstant(0, port
						.getValue().getSize(), false);
				module.addComponent(dataConst);
				connectionMap.put(port, dataConst.getValueBus());
			}
		} else if (entrySchedules.size() == 1) {
			/*
			 * If there is only one Entry, then its EntrySchedule Buses can be
			 * connected directly to the Component's Ports.
			 */
			final EntrySchedule entrySchedule = entrySchedules.iterator()
					.next();
			tracker.setControlBus(component, entrySchedule.getControlBus());
			connectionMap.put(component.getGoPort(),
					entrySchedule.getBus(component.getGoPort()));
			for (Port port : component.getDataPorts()) {
				connectionMap.put(port, entrySchedule.getBus(port));
			}
		} else {
			/*
			 * OR the control Buses to produce a single go Bus.
			 */
			final Port goPort = component.getGoPort();
			final Map<Bus, EntrySchedule> uniqueGoBuses = new LinkedHashMap<Bus, EntrySchedule>();
			for (EntrySchedule entrySchedule : entrySchedules) {
				final Bus controlBus = entrySchedule.getControlBus();
				uniqueGoBuses.put(controlBus, entrySchedule);
			}
			assert uniqueGoBuses.size() == entrySchedules.size() : "non-unique go Buses "
					+ uniqueGoBuses.size() + ":" + entrySchedules.size();
			Collection<Bus> goBuses = uniqueGoBuses.keySet();

			/*
			 * If balancing, delay all the go buses to the same length.
			 */
			Map<Bus, Bus> newBusToOldBus = null;
			if (isBalancing) {
				newBusToOldBus = synchronizeControlBuses(goBuses,
						component.getOwner(), tracker);
			}

			/*
			 * Hack: if we are not balancing, newBusToOldBus map is null so map
			 * the goBuses to themselves.
			 */
			if (newBusToOldBus == null) {
				newBusToOldBus = new HashMap<Bus, Bus>(goBuses.size());
				for (Bus goBus : goBuses) {
					newBusToOldBus.put(goBus, goBus);
				}
			}
			final Or or = tracker.getOr(newBusToOldBus.keySet(),
					component.getOwner());
			final Bus orBus = or.getResultBus();
			connectionMap.put(goPort, orBus);
			tracker.setControlBus(component, orBus);

			/*
			 * Merge each set of data Buses using a Mux.
			 */
			for (Port dataPort : component.getDataPorts()) {
				final List<Bus> selectBuses = new LinkedList<Bus>();
				final List<Bus> dataBuses = new LinkedList<Bus>();

				/*
				 * Use the new Buses obtained from
				 * synchronizing-control-buses-step to get the Latency, but use
				 * the original Buses to get entrySchedule.
				 */
				for (Bus newGoBus : newBusToOldBus.keySet()) {

					final Latency goLatency = tracker.getLatency(newGoBus);
					EntrySchedule entrySchedule = uniqueGoBuses
							.get(newBusToOldBus.get(newGoBus));

					/*
					 * Not every Entry has to supply a dependency for every
					 * Port, although at least one Entry should.
					 */
					Bus dataBus = entrySchedule.getBus(dataPort);
					if (dataBus != null) {
						/*
						 * If balancing, synchronize each data Bus with its
						 * (possibly delayed) select Bus.
						 */
						if (isBalancing) {
							final Latency dataLatency = tracker
									.getLatency(dataBus);
							assert dataLatency.isFixed() : "data bus with variable latency";
							dataBus = tracker.delayDataBus(
									dataBus,
									component.getOwner(),
									goLatency.getMaxClocks()
											- dataLatency.getMaxClocks());
						}

						dataBuses.add(dataBus);
						selectBuses.add(newGoBus);
					}
				}

				/*
				 * If there are two or more unique data Buses, create a Mux;
				 * else just use the one unique Bus.
				 */
				if (new HashSet<Bus>(dataBuses).size() > 1) {
					final Mux mux = tracker.getMux(selectBuses, dataBuses,
							component.getOwner());
					connectionMap.put(dataPort, mux.getResultBus());
				} else {
					connectionMap.put(dataPort, dataBuses.iterator().next());
				}
			}
		}

		final Port goPort = component.getGoPort();

		/*
		 * Connect the Ports to the Buses that were found for them.
		 */
		if (component.consumesGo()) {
			goPort.setBus(connectionMap.get(goPort));
		}

		for (Port dataPort : component.getDataPorts()) {
			dataPort.setBus(connectionMap.get(dataPort));
		}
	}

	/**
	 * Gets the {@link Bus} that was scheduled for a given {@link Port}.
	 */
	Bus getBus(Port port) {
		return busMap.get(port);
	}

	/**
	 * Records the {@link Bus} that was scheduled for a given {@link Port}.
	 */
	void setBus(Port port, Bus bus) {
		busMap.put(port, bus);
	}

	protected boolean isBalancing() {
		return isBalancing;
	}

	private static Map<Bus, Bus> synchronizeControlBuses(Collection<Bus> buses,
			Module module, LatencyTracker tracker) {
		final Map<Bus, Latency> busLatencies = new HashMap<Bus, Latency>();
		for (Bus bus : buses) {
			busLatencies.put(bus, tracker.getLatency(bus));
		}
		final Map<Object, Latency> maxLatencies = Latency
				.getLatest(busLatencies);
		assert maxLatencies.size() == 1 : "unable to balance control Buses";

		final Latency maxLatency = maxLatencies.values().iterator().next();
		assert maxLatency.isFixed() : "unable to balance control Bus with unfixed Latency";
		final int maxClocks = maxLatency.getMaxClocks();

		Map<Bus, Bus> syncedBuses = new HashMap<Bus, Bus>(buses.size());
		for (Bus bus : buses) {
			final Latency latency = busLatencies.get(bus);
			syncedBuses.put(
					tracker.delayControlBus(bus, module,
							maxClocks - latency.getMaxClocks()), bus);
		}

		return syncedBuses;
	}
}
