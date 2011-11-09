/*
 * Copyright (c) 2011, Ecole Polytechnique Fédérale de Lausanne
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

package net.sf.orc2hdl.backend;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.sf.orcc.OrccException;
import net.sf.orcc.df.Attribute;
import net.sf.orcc.df.Connection;
import net.sf.orcc.df.Instance;
import net.sf.orcc.df.Network;
import net.sf.orcc.df.Port;
import net.sf.orcc.df.Vertex;
import net.sf.orcc.ir.ExprString;
import net.sf.orcc.ir.Expression;

/**
 * This class is giving the necessary information for the XLIM Network
 * generation, like Input Network and the Output actors broadcast and storing
 * the clock domain of each actor
 * 
 * @author Endri Bezati
 * 
 */
public class TopNetworkTemplateData {

	public static final String DEFAULT_CLOCK_DOMAIN = "CLK";

	/**
	 * Contains a Map which indicates the clock domain of each actor
	 */

	private Map<Instance, Integer> clockDomainMap;

	/**
	 * Contains a Map which indicates the number of the broadcasted actor
	 */

	private Map<Connection, Integer> countBroadcastConnectionsMap;

	/**
	 * Contains a Map which indicates the number of a Network Port broadcasted
	 */

	private Map<Port, Integer> countNetwokPortBroadcastMap;

	/**
	 * Count the Broadcast of an Actor Output port
	 * 
	 * @param network
	 */

	public void computeActorOutputPortBroadcast(Network network) {
		for (Instance instance : network.getInstances()) {
			Map<Port, List<Connection>> map = instance.getOutgoingPortMap();
			for (List<Connection> ports : map.values()) {
				int cp = 0;
				for (Connection connection : ports) {
					countBroadcastConnectionsMap.put(connection, cp++);
				}
			}
		}

	}

	/**
	 * Store Actors clock domains
	 * 
	 * @param network
	 */
	private void computeActorsClockDomains(Network network) {
		List<Instance> networkInstances = network.getInstances();

		for (Instance instance : networkInstances) {
			if (instance.isActor()) {

			}
		}
	}

	/**
	 * Count the Broadcast of an Network Input Port
	 * 
	 * @param network
	 */
	public void computeNetworkInputPortBroadcast(Network network) {
		List<Port> inputs = network.getInputs();
		for (Vertex vertex : network.getVertices()) {
			if (vertex.isPort()) {
				Port port = (Port) vertex;
				if (inputs.contains(port)) {
					countNetwokPortBroadcastMap.put(port, vertex.getOutgoing()
							.size());
				}
				int cp = 0;
				for (Connection connection : vertex.getOutgoing()) {
					countBroadcastConnectionsMap.put(connection, cp++);
				}

			}
		}
	}

	/**
	 * build all informations needed in the template data.
	 * 
	 * @param network
	 *            a network
	 */
	public void computeTemplateMaps(Network network) {
		countNetwokPortBroadcastMap = new HashMap<Port, Integer>();
		countBroadcastConnectionsMap = new HashMap<Connection, Integer>();
		clockDomainMap = new HashMap<Instance, Integer>();

		computeNetworkInputPortBroadcast(network);
		computeActorOutputPortBroadcast(network);

		computeActorsClockDomains(network);
	}

	/**
	 * Return a Map which contains a connection and an associated number
	 */
	public Map<Connection, Integer> getCountBroadcastConnectionsMap() {
		return countBroadcastConnectionsMap;
	}

	/**
	 * Return a Map which contains a connection and an associated number
	 */
	public Map<Port, Integer> getCountNetwokPortBroadcastMap() {
		return countNetwokPortBroadcastMap;
	}

	private String getPartNameAttribute(Instance instance) throws OrccException {
		String clockDomain = DEFAULT_CLOCK_DOMAIN;
		Attribute attr = instance.getAttribute("clockDomain");
		if (attr != null) {
			Expression expr = (Expression) attr.getValue();
			clockDomain = ((ExprString) expr).getValue();
		}
		return clockDomain;
	}
}
