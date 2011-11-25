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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sf.orcc.df.Connection;
import net.sf.orcc.df.Instance;
import net.sf.orcc.df.Network;
import net.sf.orcc.df.Port;

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
	 * Map which contains the Clock Domain of a port
	 */
	private Map<Port, String> portClockDomain;

	/**
	 * Map which contains the Clock Domain of an instance
	 */
	private Map<Instance, String> instanceClockDomain;

	/**
	 * Contains a Map which indicates the number of the broadcasted actor
	 */
	private Map<Connection, Integer> networkPortConnectionFanout;

	/**
	 * Contains a Map which indicates the number of a Network Port broadcasted
	 */
	private Map<Port, Integer> networkPortFanout;

	/**
	 * Contains a Map which indicates the index of the given clock
	 */

	private Map<String, Integer> clockDomainsIndex;

	private Map<Connection, List<Integer>> connectionsClockDomain;

	/**
	 * Count the fanout of the actor's output port
	 * 
	 * @param network
	 */
	public void computeActorOutputPortFanout(Network network) {
		for (Instance instance : network.getInstances()) {
			Map<Port, List<Connection>> map = instance.getOutgoingPortMap();
			for (List<Connection> values : map.values()) {
				int cp = 0;
				for (Connection connection : values) {
					networkPortConnectionFanout.put(connection, cp++);
				}
			}
		}

	}

	/**
	 * This method fills up the portClockDomain and instanceClockDomain Map with
	 * clock domains given by the Mapping configuration tab. By default
	 * Input/Output ports of the network are being given the default clock
	 * domain CLK.
	 * 
	 * @param network
	 */
	private void computeNetworkClockDomains(Network network,
			Map<String, String> clockDomains) {

		// Fill the the portClockDomain with "CLK" for the I/O of the network
		for (Port port : network.getInputs()) {
			portClockDomain.put(port, DEFAULT_CLOCK_DOMAIN);
		}

		for (Port port : network.getOutputs()) {
			portClockDomain.put(port, DEFAULT_CLOCK_DOMAIN);
		}

		// For each instance on the network give the clock domain specified by
		// the mapping configuration tab or if not give the default clock domain
		int clkIndex = 0;
		clockDomainsIndex.put(DEFAULT_CLOCK_DOMAIN, clkIndex++);

		for (String string : clockDomains.values()) {
			if (!string.isEmpty() && !clockDomainsIndex.containsKey(string)) {
				clockDomainsIndex.put(string, clkIndex++);
			}
		}

		for (Instance instance : network.getInstances()) {
			if (clockDomains.keySet().contains("/" + instance.getId())) {
				if (!clockDomains.get("/" + instance.getId()).isEmpty()) {

					instanceClockDomain.put(instance,
							clockDomains.get("/" + instance.getId()));
				} else {
					instanceClockDomain.put(instance, DEFAULT_CLOCK_DOMAIN);
				}
			}
		}

		if (clockDomainsIndex.size() > 1) {
			connectionsClockDomain = new HashMap<Connection, List<Integer>>();
			for (Connection connection : network.getConnections()) {
				if (connection.getSource().isPort()) {
					List<Integer> sourceTarget = new ArrayList<Integer>();
					int srcIndex = clockDomainsIndex.get(portClockDomain
							.get(connection.getSource()));
					int tgtIndex = clockDomainsIndex.get(instanceClockDomain
							.get(connection.getTarget()));
					if (srcIndex != tgtIndex) {
						sourceTarget.add(0, srcIndex);
						sourceTarget.add(1, tgtIndex);
						connectionsClockDomain.put(connection, sourceTarget);
					}
				} else {
					if (connection.getTarget().isPort()) {
						List<Integer> sourceTarget = new ArrayList<Integer>();
						int srcIndex = clockDomainsIndex
								.get(instanceClockDomain.get(connection
										.getSource()));
						int tgtIndex = clockDomainsIndex.get(portClockDomain
								.get(connection.getTarget()));
						if (srcIndex != tgtIndex) {
							sourceTarget.add(0, srcIndex);
							sourceTarget.add(1, tgtIndex);
							connectionsClockDomain
									.put(connection, sourceTarget);
						}
					} else {
						List<Integer> sourceTarget = new ArrayList<Integer>();
						int srcIndex = clockDomainsIndex
								.get(instanceClockDomain.get(connection
										.getSource()));
						int tgtIndex = clockDomainsIndex
								.get(instanceClockDomain.get(connection
										.getTarget()));
						if (srcIndex != tgtIndex) {
							sourceTarget.add(0, srcIndex);
							sourceTarget.add(1, tgtIndex);
							connectionsClockDomain
									.put(connection, sourceTarget);
						}
					}

				}
			}
		}

	}

	/**
	 * Count the fanout of the network's input port
	 * 
	 * @param network
	 */

	public void computeNetworkInputPortFanout(Network network) {
		for (Port port : network.getInputs()) {
			int cp = 0;
			for (Connection connection : network.getConnections()) {
				if (connection.getSource() == port) {
					networkPortFanout.put(port, cp + 1);
					networkPortConnectionFanout.put(connection, cp);
					cp++;
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
	public void computeTemplateMaps(Network network,
			Map<String, String> clockDomains) {
		networkPortFanout = new HashMap<Port, Integer>();
		networkPortConnectionFanout = new HashMap<Connection, Integer>();
		portClockDomain = new HashMap<Port, String>();
		instanceClockDomain = new HashMap<Instance, String>();
		clockDomainsIndex = new HashMap<String, Integer>();
		computeNetworkInputPortFanout(network);
		computeActorOutputPortFanout(network);
		computeNetworkClockDomains(network, clockDomains);
	}

	public Map<String, Integer> getClockDomainsIndex() {
		return clockDomainsIndex;
	}

	/**
	 * Return a Map which contains the Clock Domain of an Instance
	 * 
	 * @return instanceClockDomain
	 */
	public Map<Instance, String> getInstanceClockDomain() {
		return instanceClockDomain;
	}

	/**
	 * Return a Map which contains a connection and an associated number
	 * 
	 * @return networkPortConnectionFanout
	 */
	public Map<Connection, Integer> getNetworkPortConnectionFanout() {
		return networkPortConnectionFanout;
	}

	/**
	 * Return a Map which contains a connection and an associated number
	 * 
	 * @return networkPortFanout
	 */
	public Map<Port, Integer> getNetworkPortFanout() {
		return networkPortFanout;
	}

	/**
	 * Return a Map which contains the Clock Domain of a port
	 * 
	 * @return portClockDomain
	 */
	public Map<Port, String> getPortClockDomain() {
		return portClockDomain;
	}

	/**
	 * Return a Map which contains the Connection and the clock index of the I/O
	 * 
	 * @return portClockDomain
	 */

	public Map<Connection, List<Integer>> getConnectionsClockDomain() {
		return connectionsClockDomain;
	}
}
