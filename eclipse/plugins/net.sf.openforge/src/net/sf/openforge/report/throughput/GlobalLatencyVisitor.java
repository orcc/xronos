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

package net.sf.openforge.report.throughput;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import net.sf.openforge.lim.Bus;
import net.sf.openforge.lim.Call;
import net.sf.openforge.lim.Component;
import net.sf.openforge.lim.DataFlowVisitor;
import net.sf.openforge.lim.Dependency;
import net.sf.openforge.lim.Design;
import net.sf.openforge.lim.Entry;
import net.sf.openforge.lim.Exit;
import net.sf.openforge.lim.InBuf;
import net.sf.openforge.lim.Latch;
import net.sf.openforge.lim.Latency;
import net.sf.openforge.lim.Mux;
import net.sf.openforge.lim.OutBuf;
import net.sf.openforge.lim.Port;
import net.sf.openforge.lim.Scoreboard;
import net.sf.openforge.lim.Task;
import net.sf.openforge.lim.Visitor;
import net.sf.openforge.lim.memory.MemoryRead;
import net.sf.openforge.lim.memory.MemoryWrite;

/**
 * GlobalLatencyVisitor will calculate the latency of every component in the LIM
 * (actually the latency of every Bus in the LIM) and store it in a Map. This in
 * intended to be used by the ThroughtputAnalyzer which means that the data
 * isn't even calculated unless the design is balanced during scheduling. This
 * class makes use of the latency information annotated on the Exits of each
 * component by scheduling.
 * 
 * 
 * <p>
 * Created: Wed Jan 15 15:28:55 2003
 * 
 * @author imiller, last modified by $Author: imiller $
 * @version $Id: GlobalLatencyVisitor.java 109 2006-02-24 18:10:34Z imiller $
 */
public class GlobalLatencyVisitor extends DataFlowVisitor implements Visitor {

	/** A map of Bus -> Latency for every bus in the design. */
	private Map<Bus, Latency> latencyMap = new HashMap<Bus, Latency>();
	/** A map of Port -> Latency for each procedure body port. */
	private Map<Port, Latency> procedurePortLatency = new HashMap<Port, Latency>();

	/**
	 * A map of Component -> Latency for every component. The latency is the
	 * calculated latest latency for any input of that component.
	 */
	private Map<Component, Latency> inputLatencyMap = new HashMap<Component, Latency>();

	public GlobalLatencyVisitor() {
		setRunForward(true);
	}

	public Map<Bus, Latency> getLatencyMap() {
		return latencyMap;
	}

	public Map<Component, Latency> getInputLatencyMap() {
		return inputLatencyMap;
	}

	@Override
	public void visit(Design des) {
		// if (_throughput.db) _throughput.d.launchXGraph(des, false);
		super.visit(des);
	}

	@Override
	public void visit(Task task) {
		if (_throughput.db)
			_throughput.d.launchXGraph(task.getCall().getProcedure().getBody(),
					"foo", false);
		if (_throughput.db)
			_throughput.d.launchGraph(task.getCall().getProcedure().getBody(),
					"foo", net.sf.openforge.util.Debug.GR_DEFAULT, false);
		super.visit(task);
	}

	@Override
	public void visit(Latch comp) {
		preFilter(comp);
		// Treat the latch as atomic.
		// traverse(comp);
		postFilter(comp);
	}

	@Override
	public void visit(Mux comp) {
		preFilter(comp);
		Map<?, Latency> latencies = Latency.getLatest(getInputLatencies(comp));
		Latency latency = Latency.or(new HashSet<Latency>(latencies.values()),
				comp.getExit(Exit.DONE));
		inputLatencyMap.put(comp, latency);
		updateBuses(comp, latency);
	}

	@Override
	public void visit(Scoreboard comp) {
		preFilter(comp);
		// Treat the scoreboard as atomic.
		// traverse(comp);
		Map<?, Latency> latencies = Latency.getLatest(getInputLatencies(comp));
		Latency latency = Latency.and(new HashSet<Latency>(latencies.values()),
				comp.getExit(Exit.DONE));
		inputLatencyMap.put(comp, latency);
		updateBuses(comp, latency);
	}

	@Override
	public void visit(Call comp) {
		preFilter(comp);
		for (Port port : comp.getPorts()) {
			Port procPort = comp.getProcedurePort(port);
			Latency portLatency = getLatency(port);
			procedurePortLatency.put(procPort, portLatency);
		}
		traverse(comp);
		postFilter(comp);
	}

	@Override
	public void visit(InBuf comp) {
		preFilter(comp);
		traverse(comp);

		if (_throughput.db)
			_throughput.d.ln(_throughput.GLV, "Comp: " + comp.show());
		Latency goLatency = getLatency(comp.getGoBus().getPeer());
		for (Bus bus : comp.getBuses()) {
			Port port = bus.getPeer();
			Latency lat = getLatency(port);

			//
			// A module is enabled by its GO which, by definition,
			// must take into account all (real) inputs. Sideband
			// data may come in via an input as well, but it will seem
			// as though it arrives at time 0 since it comes straight
			// from the top level inbuf. In truth it comes in at the
			// time that correlates with the time that the 'fetch'
			// signal was sent to the global.
			//
			if (goLatency.isGT(lat))
				lat = goLatency;
			putLatency(bus, lat);
		}
	}

	@Override
	public void visit(OutBuf comp) {
		// Scheduling should leave the exit latencies correct for each
		// module. So, when we process each module as a component
		// (during postFilter) we'll use that latency. Thus we don't
		// need to push our calculated latency out.
		super.visit(comp);
	}

	/**
	 * Override the method in DataFlowVisitor because we don't want to traverse
	 * the physical component. Scheduling only annotates the Latency on the
	 * actual MemoryRead component and not on anything in the physical
	 * implementation. This overridden method ignores the physical, and thus
	 * acts on the same componentry as scheduling.
	 */
	@Override
	public void visit(MemoryRead memoryRead) {
		preFilter(memoryRead);
		traverse(memoryRead);
		postFilter(memoryRead);
	}

	/**
	 * Override the method in DataFlowVisitor because we don't want to traverse
	 * the physical component. Scheduling only annotates the Latency on the
	 * actual MemoryWrite component and not on anything in the physical
	 * implementation. This overridden method ignores the physical, and thus
	 * acts on the same componentry as scheduling.
	 */
	@Override
	public void visit(MemoryWrite memoryWrite) {
		preFilter(memoryWrite);
		traverse(memoryWrite);
		postFilter(memoryWrite);
	}

	@Override
	protected void postFilterAny(Component comp) {
		super.postFilterAny(comp);

		final Map<?, Latency> latest = Latency
				.getLatest(getInputLatencies(comp));
		assert latest.keySet().size() == 1 : comp + " " + comp.showOwners()
				+ " " + latest;
		final Latency inputLatency = latest.values().iterator().next();

		inputLatencyMap.put(comp, inputLatency);

		updateBuses(comp, inputLatency);
	}

	private void updateBuses(Component comp, Latency inputLatency) {
		if (_throughput.db)
			_throughput.d.ln(_throughput.GLV, "Comp: " + comp.show());
		for (Bus bus : comp.getBuses()) {
			final Exit exit = bus.getOwner();
			if (_throughput.db)
				_throughput.d.ln(_throughput.GLV, "  exit " + exit.getTag());
			if (exit.getTag() != null
					&& exit.getTag().getType() == Exit.SIDEBAND)
				continue;
			final Latency exitLatency = bus.getOwner().getLatency();
			if (_throughput.db)
				_throughput.d.ln(_throughput.GLV, "  ->bus " + bus + " exit "
						+ exit + " lat " + exitLatency);
			final Latency busLatency = exitLatency.addTo(inputLatency);
			putLatency(bus, busLatency);
		}
	}

	private Map<Object, Latency> getInputLatencies(Component comp) {
		final Map<Object, Latency> map = new HashMap<Object, Latency>();
		for (Port port : comp.getPorts()) {
			final Latency portLatency = getLatency(port);
			if (_throughput.db)
				_throughput.d.ln(_throughput.GLV, " p: " + port + " lat "
						+ portLatency);
			map.put(port, portLatency);
		}

		// The latencies of all dependecies must be considered as well
		// since scheduling may not create an explicit bus for each
		// dependency if it has figured out that a bus isn't needed
		// (eg a register read is 'always on' and thus needs no
		// enables). We could probably get away with using just
		// resource and wait dependencies, but we are using all deps
		// to be safe.
		for (Port port : comp.getPorts()) {
			for (Entry entry : comp.getEntries()) {
				for (Dependency dep : entry.getDependencies(port)) {
					Latency depLat = getLatency(dep.getLogicalBus());
					if (_throughput.db)
						_throughput.d.ln(_throughput.GLV, " d: " + dep
								+ " lat " + depLat);
					map.put(dep, depLat);
				}
			}
		}

		if (map.keySet().size() == 0) {
			map.put(new Object(), Latency.ZERO);
		}

		return map;
	}

	private void putLatency(Bus bus, Latency lat) {
		latencyMap.put(bus, lat);
		if (_throughput.db)
			_throughput.d.ln(_throughput.GLV, "\tBus " + bus + " latency "
					+ lat);
	}

	private Latency getLatency(Port port) {
		Bus bus = port.getBus();

		if (bus == null) {
			// May be a procedure port, otherwise, default to 0.
			Latency latency = procedurePortLatency.get(port);
			if (latency == null) {
				latency = Latency.ZERO;
			}

			return latency;
		}

		return getLatency(bus);
	}

	private Latency getLatency(Bus bus) {
		Latency lat = latencyMap.get(bus);

		if (lat == null) {
			lat = Latency.ZERO;
			latencyMap.put(bus, lat);
		}

		return lat;
	}

}// GlobalLatencyVisitor
