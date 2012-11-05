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
 
package net.sf.orc2hdl.backend

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Map
import net.sf.orcc.df.Network
import net.sf.orcc.ir.util.IrSwitch
import net.sf.orcc.df.Connection
import java.util.List
import net.sf.orcc.df.Port
import net.sf.orcc.df.Instance
import net.sf.orcc.graph.Vertex
import java.util.HashMap
import java.util.ArrayList

/*
 * A VHDL Network printer
 * 
 * @author Endri Bezati
 */
class NetworkPrinter extends IrSwitch {
	
	var Network network;
	
	var String DEFAULT_CLOCK_DOMAIN = "CLK";
		/**
	 * Map which contains the Clock Domain of a port
	 */
	var Map<Port, String> portClockDomain;

	/**
	 * Map which contains the Clock Domain of an instance
	 */
	var Map<Instance, String> instanceClockDomain;

	/**
	 * Contains a Map which indicates the number of the broadcasted actor
	 */
	var Map<Connection, Integer> networkPortConnectionFanout;

	/**
	 * Contains a Map which indicates the number of a Network Port broadcasted
	 */
	var Map<Port, Integer> networkPortFanout;

	/**
	 * Contains a Map which indicates the index of the given clock
	 */

	var Map<String, Integer> clockDomainsIndex;

	var Map<Connection, List<Integer>> connectionsClockDomain;
	
	/**
	 * Count the fanout of the actor's output port
	 * 
	 * @param network
	 */
	
	def computeActorOutputPortFanout(Network network) {
		for (Vertex vertex : network.getVertices()) {
			if (vertex instanceof Instance) {
				var Instance instance = vertex as Instance;
				var Map<Port, List<Connection>> map = instance.getOutgoingPortMap();
				for (List<Connection> values : map.values()) {
					var int cp = 0;
					for (Connection connection : values) {
						networkPortConnectionFanout.put(connection, cp);
						cp = cp + 1;
					}
				}
			}
		}
	}
	
	
	def computeNetworkInputPortFanout(Network network) {
		for (Port port : network.getInputs()) {
			var int cp = 0;
			for (Connection connection : network.getConnections()) {
				if (connection.getSource() == port) {
					networkPortFanout.put(port, cp + 1);
					networkPortConnectionFanout.put(connection, cp);
					cp = cp + 1;
				}
			}
		}
	}
	
	def void computeNetworkClockDomains(Network network,
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
		var int clkIndex = 0;
		clockDomainsIndex.put(DEFAULT_CLOCK_DOMAIN, clkIndex);
		clkIndex = clkIndex + 1;

		for (String string : clockDomains.values()) {
			if (!string.isEmpty() && !clockDomainsIndex.containsKey(string)) {
				clockDomainsIndex.put(string, clkIndex);
				clkIndex = clkIndex + 1;
			}
		}

		for (Vertex vertex : network.getVertices()) {
			if (vertex instanceof Instance) {
				var Instance instance = vertex as Instance;
				if (!clockDomains.isEmpty()) {
					if (clockDomains.keySet().contains(
							instance.getHierarchicalName())) {
						if (!clockDomains.get(instance.getHierarchicalName())
								.isEmpty()) {

							instanceClockDomain.put(instance, clockDomains
									.get(instance.getHierarchicalName()));
						}
					} else {
						instanceClockDomain.put(instance, DEFAULT_CLOCK_DOMAIN);
					}
				} else {
					instanceClockDomain.put(instance, DEFAULT_CLOCK_DOMAIN);
				}

			}

			if (clockDomainsIndex.size() > 1) {
				connectionsClockDomain = new HashMap<Connection, List<Integer>>();
				for (Connection connection : network.getConnections()) {
					if (connection.getSource() instanceof Port) {
						var List<Integer> sourceTarget = new ArrayList<Integer>();
						var int srcIndex = clockDomainsIndex.get(portClockDomain
								.get(connection.getSource()));
						var int tgtIndex = clockDomainsIndex
								.get(instanceClockDomain.get(connection
										.getTarget()));
						if (srcIndex != tgtIndex) {
							sourceTarget.add(0, srcIndex);
							sourceTarget.add(1, tgtIndex);
							connectionsClockDomain
									.put(connection, sourceTarget);
						}
					} else {
						if (connection.getTarget() instanceof Port) {
							var List<Integer> sourceTarget = new ArrayList<Integer>();
							var int srcIndex = clockDomainsIndex
									.get(instanceClockDomain.get(connection
											.getSource()));
							var int tgtIndex = clockDomainsIndex
									.get(portClockDomain.get(connection
											.getTarget()));
							if (srcIndex != tgtIndex) {
								sourceTarget.add(0, srcIndex);
								sourceTarget.add(1, tgtIndex);
								connectionsClockDomain.put(connection,
										sourceTarget);
							}
						} else {
							var List<Integer> sourceTarget = new ArrayList<Integer>();
							var int srcIndex = clockDomainsIndex
									.get(instanceClockDomain.get(connection
											.getSource()));
							var int tgtIndex = clockDomainsIndex
									.get(instanceClockDomain.get(connection
											.getTarget()));
							if (srcIndex != tgtIndex) {
								sourceTarget.add(0, srcIndex);
								sourceTarget.add(1, tgtIndex);
								connectionsClockDomain.put(connection,
										sourceTarget);
							}
						}

					}
				}
			}
		}
	}
	
	def headerComments(){
		var dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
		var date = new Date();
		'''
		-- ----------------------------------------------------------------------------
		--   ___ _  _ ___  ___  _  _  ___  ___ 
		--  / __| || | _ \/ _ \| \| |/ _ \/ __|
		-- | (__| __ |   / (_) | .` | (_) \__ \
		--  \___|_||_|_|_\\___/|_|\_|\___/|___/
		-- ----------------------------------------------------------------------------
		-- Chronos synthesizer
		-- Top level Network: « network.simpleName» 
		-- Date: «dateFormat.format(date)»
		-- ----------------------------------------------------------------------------
		'''	
	}
	
	def printClockInformation(){
		'''
		-- ----------------------------------------------------------------------------
		-- Clock Informations on the Network "«network.simpleName»"
		--
		-- Network input port(s) clock domain:
		«FOR port: network.inputs»
			--	«port.name» --> «portClockDomain.get(port)»
		«ENDFOR»
		-- Network output port(s) clock domain:
		«FOR port: network.outputs»
			-- 	«port.name» --> «portClockDomain.get(port)»
		«ENDFOR»
		-- Instance(s) clock domains:
		«FOR vertex: network.vertices»
			«IF vertex instanceof Instance»
				--	«(vertex as Instance).simpleName» («(vertex as Instance).actor.simpleName») --> «instanceClockDomain.get(vertex as Instance)»
			«ENDIF»
		«ENDFOR»
		'''
	}
	
	def printLibrary(){
		'''
		library ieee;
		library SystemBuilder;
		
		use ieee.std_logic_1164.all;
		'''
	}
	
	def printEntity(){
		'''
		entity «network.simpleName» is
		port(
			 -- XDF Network Input(s)
			 «FOR port: network.inputs»
			 	«addDeclarationPort(port,"in","out")»
			 «ENDFOR»
			 -- XDF Network Output(s)
			 «FOR port: network.outputs»
			 	«addDeclarationPort(port,"out","in")»
			 «ENDFOR»
			 -- Clock(s) and Reset
			 «FOR string: clockDomainsIndex.keySet SEPARATOR "\n"»
			 «string» : in std_logic;
			 «ENDFOR»
			 RESET : in std_logic);
		end entity «network.simpleName»;
		'''
	}
	
	def addDeclarationPort(Port port, String dirA, String dirB){
		'''
		«IF port.type.bool || port.type.sizeInBits == 1»
		«port.name»_data : «dirA» std_logic;
		«ELSE»
		«port.name»_data : «dirA» std_logic_vector(«port.type.sizeInBits - 1» downto 0);
		«ENDIF»
		«port.name»_send : «dirA» std_logic;
		«port.name»_ack : «dirB» std_logic;
		«IF dirA.equals("OUT")»
		«port.name»_rdy : «dirB» std_logic;
		«ENDIF»
		«port.name»_count : «dirA» std_logic_vector(15 downto 0);
		'''
	}
	
	def printArchitecture(){
		'''
		architecture rtl of «network.simpleName» is
			-- --------------------------------------------------------------------------
			-- Internal Signals
			-- --------------------------------------------------------------------------
		
			-- Clock(s) and Reset signal
			signal clocks, resets: std_logic_vector(«clockDomainsIndex.size  - 1» downto 0);
		
			-- Network Input Port 
			«FOR port: network.inputs»
				«printSignal(port,"","ni",0,true)»
			«ENDFOR»
			
			-- Network Input Port Fanout(s)
			«FOR port: network.inputs»
				«printSignal(port,"","nif",networkPortFanout.get(port),true)»
			«ENDFOR»
			-- --------------------------------------------------------------------------
			-- Network Instances
			-- --------------------------------------------------------------------------
		end architecture rtl;
		'''
	}
	
	def printSignal(Port port, String owner, String prefix, Integer fanout, Boolean input){
		var String dataSize;
		if(port.type.bool || (port.type.sizeInBits == 1)){
			dataSize = "std_logic";
		}else{
			dataSize = "std_logic_vector("+(port.type.sizeInBits - 1)+" downto 0)";
		}
		var String fanoutSize;
		if(fanout == 0){
			fanoutSize = "std_logic";
		}else{
			fanoutSize = "std_logic_vector("+(fanout - 1)+" downto 0)";
		}
		'''
		«IF port.native»
			signal «port.name»_data : «dataSize»;
		«ELSE»
			signal «prefix»_«owner»«port.name»_DATA : «dataSize»;
			signal «prefix»_«owner»«port.name»_SEND : «fanoutSize»;
			signal «prefix»_«owner»«port.name»_ACK : «fanoutSize»;
			«IF !input»signal «prefix»_«owner»«port.name»_RDY : «fanoutSize»;«ENDIF»
			signal «prefix»_«owner»«port.name»_COUNT : std_logic_vector(15 downto 0);
		«ENDIF»
		'''
	}
	
	def printNetwork(Network network, Map<String,Object> options){
		// Initialize members
		this.network = network; 
		networkPortFanout = new HashMap<Port, Integer>();
		networkPortConnectionFanout = new HashMap<Connection, Integer>();
		portClockDomain = new HashMap<Port, String>();
		instanceClockDomain = new HashMap<Instance, String>();
		clockDomainsIndex = new HashMap<String, Integer>();
		computeNetworkInputPortFanout(network);
		computeActorOutputPortFanout(network);
		var Map<String,String> clkDomains = new HashMap<String,String>(); 
		
		if (options.containsKey("clkDomains")) {
			clkDomains = options.get("clkDomains") as Map<String,String>; 
		}
		computeNetworkClockDomains(network,clkDomains);
		
		'''
		«headerComments()»
		
		«printClockInformation()»
		
		«printLibrary()»
		
		-- ----------------------------------------------------------------------------
		-- Entity Declaration
		-- ----------------------------------------------------------------------------
		«printEntity()»
		
		-- ----------------------------------------------------------------------------
		-- Architecture Declaration
		-- ----------------------------------------------------------------------------
		«printArchitecture()»
		-- ----------------------------------------------------------------------------
		-- ----------------------------------------------------------------------------
		-- ----------------------------------------------------------------------------
		'''
	}
	
}