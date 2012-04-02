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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import net.sf.openforge.lim.Bus;
import net.sf.openforge.lim.Port;
import net.sf.openforge.util.naming.ID;

/**
 * PortCache maintains mappings from {@link net.sf.orcc.df.Port}s to the
 * {@link Port} or {@link Bus} object created for that Port.
 * 
 * @author Endri Bezati
 * 
 */
public class PortCache {

	private final Map<net.sf.orcc.df.Port, Bus> sourceCache = new HashMap<net.sf.orcc.df.Port, Bus>();

	private final Map<net.sf.orcc.df.Port, Port> targetCache = new HashMap<net.sf.orcc.df.Port, Port>();

	public PortCache() {
	}

	/**
	 * Returns the output {@link Bus} that was defined for the
	 * {@link net.sf.orcc.df.Port}
	 * 
	 * @param orccPort
	 * @return
	 */
	public Bus getSource(net.sf.orcc.df.Port orccPort) {
		return sourceCache.get(orccPort);
	}

	/**
	 * Returns the output {@link Port} that was defined for the
	 * {@link net.sf.orcc.df.Port}
	 * 
	 * @param orccPort
	 * @return
	 */
	public Port getTarget(net.sf.orcc.df.Port orccPort) {
		return targetCache.get(orccPort);
	}

	/**
	 * Defines the specified {@link Bus} as the implementation of the specified
	 * {@link net.sf.orcc.df.Port}, overwriting any previous association for the
	 * Node.
	 * 
	 * @param orccPort
	 * @param bus
	 */
	public void putSource(net.sf.orcc.df.Port orccPort, Bus bus) {
		sourceCache.put(orccPort, bus);
	}

	/**
	 * Defines the specified {@link Port} as the implementation of the specified
	 * {@link net.sf.orcc.df.Port}, overwriting any previous association for the
	 * Node.
	 * 
	 * @param orccPort
	 * @param limPort
	 */
	public void putTarget(net.sf.orcc.df.Port orccPort, Port limPort) {
		targetCache.put(orccPort, limPort);
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
		net.sf.orcc.df.Port key = null;
		for (Entry<net.sf.orcc.df.Port, Bus> entry : sourceCache.entrySet()) {
			if (entry.getValue() == original) {
				key = entry.getKey();
				break;
			}
		}
		assert key != null : "Could not replace source bus";
		putSource(key, replacement);
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
		net.sf.orcc.df.Port key = null;
		for (Entry<net.sf.orcc.df.Port, Port> entry : targetCache.entrySet()) {
			if (entry.getValue() == original) {
				key = entry.getKey();
				break;
			}
		}
		if (key != null) {
			putTarget(key, replacement);
		}
	}

	public void publish(PortCache cache, Collection<ID> externallyVisiblePorts) {
		// TODO Auto-generated method stub

	}
}
