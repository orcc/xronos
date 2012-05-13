/*
 * Copyright (c) 2012, Ecole Polytechnique Fédérale de Lausanne
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 *   * Redistributions of source code must retain the above copyright notice,
 *     this list of conditions and the following disclaimer.
 *   * Redistributions in binary form must reproduce the above copyright notice,
 *     this list of conditions and the following disclaimer in the documentation
 *     and/or other materials provided with the distribution.
 *   * Neither the name of the Ecole Polytechnique Fédérale de Lausanne nor the names of its
 *     contributors may be used to endorse or promote products derived from this
 *     software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY
 * WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 */

package net.sf.orc2hdl.design;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import net.sf.openforge.lim.Bus;
import net.sf.openforge.lim.Component;
import net.sf.openforge.lim.Module;
import net.sf.openforge.lim.Port;
import net.sf.openforge.util.naming.ID;
import net.sf.orcc.ir.Var;

/**
 * PortCache maintains mappings from {@link net.sf.orcc.df.Port}s to the
 * {@link Port} or {@link Bus} object created for that Port.
 * 
 * @author Endri Bezati
 * 
 */
public class PortCache {

	private final Map<Bus, Var> sourceCache = new HashMap<Bus, Var>();

	private final Map<Port, Var> targetCache = new HashMap<Port, Var>();

	private final Map<Component, Bus> doneBusComponents = new HashMap<Component, Bus>();

	public PortCache() {
	}

	/**
	 * Checks if a Var has been declared in the target and source Cache
	 * 
	 * @param var
	 * @return
	 */
	public Boolean varExists(Var var) {
		return targetCache.containsValue(var) && sourceCache.containsValue(var);
	}

	public Bus getDoneBus(Component component) {
		return doneBusComponents.get(component);
	}

	/**
	 * Returns the output {@link Bus} that was defined for the
	 * {@link net.sf.orcc.df.Port}
	 * 
	 * @param orccPort
	 * @return
	 */
	public Bus getSource(Var var) {
		Bus bus = null;
		for (Bus value : sourceCache.keySet()) {
			if (var == sourceCache.get(value)) {
				bus = value;
			}
		}
		return bus;
	}

	/**
	 * Returns the output {@link Port} that was defined for the
	 * {@link net.sf.orcc.df.Port}
	 * 
	 * @param orccPort
	 * @return
	 */
	public Port getTarget(Var var) {
		Port port = null;
		for (Port value : targetCache.keySet()) {
			if (var == targetCache.get(value)) {
				port = value;
			}
		}
		return port;
	}

	public void putDoneBus(Component component, Bus bus) {
		doneBusComponents.put(component, bus);
	}

	/**
	 * Defines the specified {@link Bus} as the implementation of the specified
	 * {@link net.sf.orcc.df.Port}, overwriting any previous association for the
	 * Node.
	 * 
	 * @param orccPort
	 * @param bus
	 */
	public void putSource(Var var, Bus bus) {
		sourceCache.put(bus, var);
	}

	/**
	 * Defines the specified {@link Port} as the implementation of the specified
	 * {@link net.sf.orcc.df.Port}, overwriting any previous association for the
	 * Node.
	 * 
	 * @param orccPort
	 * @param limPort
	 */
	public void putTarget(Var var, Port limPort) {
		targetCache.put(limPort, var);
	}

	/**
	 * Redefines the implementation for a given net.sf.orcc.df.Port from the
	 * 'original' bus to the 'replacement' bus.
	 * 
	 * @param original
	 *            the Bus to be replaced
	 * @param replacement
	 *            the new Bus to be associated
	 */
	public void replaceSource(Bus original, Bus replacement) {
		Var value = null;
		for (Entry<Bus, Var> entry : sourceCache.entrySet()) {
			if (entry.getKey() == original) {
				value = entry.getValue();
				break;
			}
		}
		putSource(value, replacement);
	}

	/**
	 * Redefines the implementation for a given net.sf.orcc.df.Port from the
	 * 'original' port to the 'replacement' port.
	 * 
	 * @param original
	 *            the Port to be replaced
	 * @param replacement
	 *            the new Port to be associated
	 */
	public void replaceTarget(Port original, Port replacement) {
		Var value = null;
		for (Entry<Port, Var> entry : targetCache.entrySet()) {
			if (entry.getKey() == original) {
				value = entry.getValue();
				break;
			}
		}
		if (value != null) {
			putTarget(value, replacement);
		}
	}

	public void publish(Module module) {
		Set<ID> modulePorts = new HashSet<ID>(module.getPorts());
		modulePorts.addAll(module.getBuses());
		for (Entry<Bus, Var> entry : sourceCache.entrySet()) {
			if (modulePorts.contains(entry.getValue())) {
				putSource(entry.getValue(), entry.getKey());
			}
		}
		for (Entry<Port, Var> entry : targetCache.entrySet()) {
			if (modulePorts.contains(entry.getValue())) {
				putTarget(entry.getValue(), entry.getKey());
			}
		}

	}
}
