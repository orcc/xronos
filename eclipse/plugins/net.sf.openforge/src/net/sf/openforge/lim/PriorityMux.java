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

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * PriorityMux is a {@link Module} composed of a tree of 2-1 {@link EncodedMux
 * EncodedMuxes}, which protects against any problems resulting from multiple
 * selects being asserted. There is only one data bus, representing the selected
 * data, and a Done bus which reflects the assertion of any select signal.
 * 
 * @version $Id: PriorityMux.java 2 2005-06-09 20:00:48Z imiller $
 */
public class PriorityMux extends Module implements Cloneable {

	private Map<Port, Port> selectToData = new LinkedHashMap<Port, Port>();

	private Bus resultBus;

	/**
	 * Creates and builds the internals of a priority based Mux, in which the
	 * 0th data port is the <b>LEAST</b> priority and the nth data port is the
	 * <b>HIGHEST</b> priority input.
	 */
	public PriorityMux(int inputs) {
		Exit main_exit = makeExit(0);
		resultBus = main_exit.makeDataBus();
		resultBus.setIDLogical("PriorityMux_result");
		Bus done_bus = main_exit.getDoneBus();

		// Build all the ports (data and select) that are necessary.
		List<SelectDataPair> queue = new LinkedList<SelectDataPair>();
		for (int i = 0; i < inputs; i++) {
			Port select_port = makeDataPort();
			Port data_port = makeDataPort();
			selectToData.put(select_port, data_port);

			SelectDataPair sdp = new SelectDataPair(select_port.getPeer(),
					data_port.getPeer());
			queue.add(sdp);
		}

		// Run through the data/select pairs until we have merged them
		// all down to 1.
		while (queue.size() > 1) {
			List<SelectDataPair> subList = new LinkedList<SelectDataPair>();
			while (queue.size() > 1) {
				SelectDataPair low = queue.remove(0);
				SelectDataPair high = queue.remove(0);
				EncodedMux emux = new EncodedMux(2);
				Or or = new Or(2);
				addComponent(or);
				addComponent(emux);

				// wire up the new mux and or
				or.getDataPorts().get(0).setBus(high.getSelect());
				or.getDataPorts().get(1).setBus(low.getSelect());

				emux.getSelectPort().setBus(high.getSelect());
				// Reversed order to eliminate need for a 'not'
				emux.getDataPort(0).setBus(low.getData());
				emux.getDataPort(1).setBus(high.getData());
				subList.add(
						0,
						new SelectDataPair(or.getResultBus(), emux
								.getResultBus()));
			}
			for (SelectDataPair selectDataPair : subList) {
				// This reverses the order so that the highest
				// priority mux stays first in the queue.
				queue.add(0, selectDataPair);
			}
		}

		SelectDataPair last = queue.get(0);
		resultBus.getPeer().setBus(last.getData());
		done_bus.getPeer().setBus(last.getSelect());
	}

	/**
	 * Retrieves the List of select {@link Port Ports} for this encoded mux in
	 * increasing priority order.
	 * 
	 * @return a 'List' of {@link Port Ports}
	 */
	public List<Port> getSelectPorts() {
		return Collections.unmodifiableList(new ArrayList<Port>(selectToData
				.keySet()));
	}

	/**
	 * Retrieves the data port that corresponds to a given select port.
	 */
	public Port getDataPort(Port select) {
		assert (selectToData.containsKey(select)) : "Unknown select Port, can't return a data Port";
		return selectToData.get(select);
	}

	/**
	 * Get's the output result bus for this component.
	 */
	public Bus getResultBus() {
		return resultBus;
	}

	/**
	 * Throws an exception, replacement in this class not supported.
	 */
	@Override
	public boolean replaceComponent(Component removed, Component inserted) {
		throw new UnsupportedOperationException("Cannot replace components in "
				+ getClass());
	}

	/**
	 * Calls the super, then removes any reference to the given bus in this
	 * class.
	 */
	@Override
	public boolean removeDataBus(Bus bus) {
		if (super.removeDataBus(bus)) {
			if (bus == resultBus) {
				resultBus = null;
			}
			return true;
		}
		return false;
	}

	@Override
	public void accept(Visitor v) {
		v.visit(this);
	}

	@Override
	protected void cloneNotify(Module moduleClone,
			Map<Component, Component> cloneMap) {
		super.cloneNotify(moduleClone, cloneMap);
		PriorityMux clone = (PriorityMux) moduleClone;
		clone.selectToData = new LinkedHashMap<Port, Port>();
		for (Map.Entry<Port, Port> entry : selectToData.entrySet()) {
			final Port selectClone = getPortClone(entry.getKey(), cloneMap);
			final Port dataClone = getPortClone(entry.getValue(), cloneMap);
			clone.selectToData.put(selectClone, dataClone);
		}
		clone.resultBus = getBusClone(resultBus, cloneMap);
	}

	private class SelectDataPair {
		Bus select;
		Bus data;

		public SelectDataPair(Bus select, Bus data) {
			this.select = select;
			this.data = data;
		}

		public Bus getSelect() {
			return select;
		}

		public Bus getData() {
			return data;
		}
	}

} // class PriorityMux
