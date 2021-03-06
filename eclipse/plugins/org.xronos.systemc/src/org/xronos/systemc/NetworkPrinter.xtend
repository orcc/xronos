/* 
 * XRONOS, High Level Synthesis of Streaming Applications
 * 
 * Copyright (C) 2014 EPFL SCI STI MM
 *
 * This file is part of XRONOS.
 *
 * XRONOS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * XRONOS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with XRONOS.  If not, see <http://www.gnu.org/licenses/>.
 * 
 * Additional permission under GNU GPL version 3 section 7
 * 
 * If you modify this Program, or any covered work, by linking or combining it
 * with Eclipse (or a modified version of Eclipse or an Eclipse plugin or 
 * an Eclipse library), containing parts covered by the terms of the 
 * Eclipse Public License (EPL), the licensors of this Program grant you 
 * additional permission to convey the resulting work.  Corresponding Source 
 * for a non-source form of such a combination shall include the source code 
 * for the parts of Eclipse libraries used as well as that of the  covered work.
 * 
 */
package org.xronos.systemc

import java.text.SimpleDateFormat
import java.util.Date
import java.util.HashMap
import java.util.List
import java.util.Map
import net.sf.orcc.df.Actor
import net.sf.orcc.df.Connection
import net.sf.orcc.df.Entity
import net.sf.orcc.df.Network
import net.sf.orcc.df.Port
import net.sf.orcc.ir.Type
import net.sf.orcc.ir.util.IrUtil

/**
 * SystemC Network Printer
 * 
 * @author Endri Bezati
 */
class NetworkPrinter extends SystemCTemplate {

	protected var Network network

	protected var String name

	protected var Map<Connection, String> queueNames

	protected var Map<Connection, Type> queueTypes
	
	protected var Integer defaultQueueSize

	protected var Boolean addScope = true
	
	def setNetwork(Network network) {
		this.network = network
		this.name = network.simpleName
		retrieveQueueNames
	}

	override setOptions(Map<String, Object> options) {
		super.setOptions(options)
		if (options.containsKey("net.sf.orcc.fifoSize")) {
			defaultQueueSize = options.get("net.sf.orcc.fifoSize") as Integer;
		}
	}

	def getContent() '''
		«header»
		«networkContent»
	'''

	def getHeader() {
		var dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
		var date = new Date();
		'''
			// ----------------------------------------------------------------------------
			// __  ___ __ ___  _ __   ___  ___ 
			// \ \/ / '__/ _ \| '_ \ / _ \/ __|
			//  >  <| | | (_) | | | | (_) \__ \
			// /_/\_\_|  \___/|_| |_|\___/|___/
			// ----------------------------------------------------------------------------
			// This file is generated automatically by Xronos HLS
			// ----------------------------------------------------------------------------
			// Xronos SystemC, Network Generator
			// Top level Network: «network.simpleName» 
			// Date: «dateFormat.format(date)»
			// ----------------------------------------------------------------------------
		'''
	}

	def getNetworkContent() '''
		#ifndef SC_«this.name»_H
		#define SC_«this.name»_H
		
		#include "systemc.h"
		
		«FOR child : network.children»
				#include "«child.label».h" 
		«ENDFOR»
		
		SC_MODULE(«this.name»){
		
			// -- Control Ports
			sc_in<bool>   clk;
			sc_in<bool>   reset;
			sc_in<bool>   start;
			sc_out<bool>  done;
		
			// -- Network Input Ports
			«FOR port : network.inputs»
				«getPortDeclaration("in", port)»
			«ENDFOR»
			
			// -- Network Output Ports
			«FOR port : network.outputs»
				«getPortDeclaration("out", port)»
			«ENDFOR»
			
			// -- Queues
			«getQueuesDeclarationContent()»
			
			«IF !network.parameters.empty»
				// -- Network Parameters 
				// -- TBD
			«ENDIF»
			
			
			// -- Actors Start / Done signals
			«FOR child : network.children»
				sc_signal<bool> done_«child.label»;
			«ENDFOR»
			
			// -- Actors
			«FOR child : network.children»
				«child.label» i_«child.label»;
			«ENDFOR»
			
			
			«IF !network.inputs.empty || !network.outputs.empty»
				// Queue processes
			«ENDIF»
			«IF !network.inputs.empty»
				«FOR port: network.inputs SEPARATOR "\n"»
					void port_«port.name»_reader();
				«ENDFOR»
			«ENDIF»
		
			«IF !network.outputs.empty»
				«FOR port: network.outputs SEPARATOR "\n"»
					void port_«port.name»_writer();
				«ENDFOR»
			«ENDIF»
		
			// -- Constructor
			SC_CTOR(«this.name»)
				:clk("clk")
				,reset("reset")
				,start("start")
				,done("done")
				// -- Actors
				«FOR child : network.children»
					,i_«child.label»("i_«child.label»")
				«ENDFOR»
				// -- Queues
				«FOR connection : network.connections»
					,«queueNames.get(connection)»("«queueNames.get(connection)»", «IF connection.size == null»«defaultQueueSize»«ELSE»«connection.
			size»«ENDIF»)
				«ENDFOR»
			{
				// -- Connnections
				«FOR child : network.children SEPARATOR "\n"»
					«getChildConnections(child as Actor)»
				«ENDFOR»
				«IF !network.inputs.empty || !network.outputs.empty»
					// -- Port Readers/Writers Process Registration
					«FOR port: network.inputs»
						SC_CTHREAD(port_«port.name»_reader, clk.pos());
						reset_signal_is(reset,true);
					«ENDFOR»
					«FOR port: network.outputs»
						SC_CTHREAD(port_«port.name»_writer, clk.pos());
						reset_signal_is(reset,true);
					«ENDFOR»
				«ENDIF»
			}
			
		};
		
		
		#endif //SC_«this.name»_H
	'''

	def getNetworkContentSource() '''
		«header»
		
		#include "«this.name».h"
		
		«IF !network.inputs.empty || !network.outputs.empty»
			// -- Queue Readers / Writers Processes
		«ENDIF»
		«IF !network.inputs.empty»
			«inputQueueReaders»
		«ENDIF»
		
		«IF !network.outputs.empty»
			«outputQueueWriters»
		«ENDIF»
	''' 


	def getPortDeclaration(String direction, Port port) '''
		sc_fifo_«direction»< «port.type.doSwitch» > «port.name»;
	'''

	def getQueuesDeclarationContent() '''
		«FOR connection : network.connections»
			sc_fifo< «queueTypes.get(connection).doSwitch» > «queueNames.get(connection)»;
		«ENDFOR»
	'''
	
	def getInputQueueReaders(){
		'''
		«FOR port: network.inputs SEPARATOR "\n"»
			void «IF addScope»«this.name»::«ENDIF»port_«port.name»_reader(){
				do {
					wait();
				} while (!start.read());
				while (true) {
					«port.type.doSwitch» data = «port.name».read();
					«FOR connection: network.connections»
						«IF connection.source.equals(port)»
							«queueNames.get(connection)».write(data);				
					 	«ENDIF»
					«ENDFOR»
					wait();
				}
			}
		«ENDFOR»
		'''
	}
	
	def getOutputQueueWriters(){
		'''
		«FOR port: network.outputs SEPARATOR "\n"»
			«FOR connection: network.connections»
				«IF connection.target.equals(port)»
					void «IF addScope»«this.name»::«ENDIF»port_«port.name»_writer(){
						do {
							wait();
						} while (!start.read());
						while (true) {
							«port.name».write(«queueNames.get(connection)».read());
							wait();
						}
					}
				 «ENDIF»
			«ENDFOR»	
		«ENDFOR»
		'''
	}
	
	def getChildConnections(Actor child){
		var Map<Port, List<Connection>> portConnection = child.getAdapter((typeof(Entity))).getOutgoingPortMap()
		'''
			i_«child.label».clk(clk);
			i_«child.label».reset(reset);
			i_«child.label».start(start);
			i_«child.label».done(done_«child.label»);
			«FOR connection: child.incoming»
				i_«child.label».«(connection as Connection).targetPort.name»(«queueNames.get(connection)»);
			«ENDFOR»
			«FOR port: child.outputs»
				«IF portConnection.get(port).size > 1»
					«FOR connection: portConnection.get(port)»
						«IF connection.target instanceof Actor»
							i_«child.label».«port.name»_f_«(connection.target as Actor).name»(«queueNames.get(connection)»);
						«ELSE»
							i_«child.label».«port.name»_f_«(connection.target as Port).name»(«queueNames.get(connection)»);
						«ENDIF»
					«ENDFOR»
				«ELSE»
					i_«child.label».«port.name»(«queueNames.get(portConnection.get(port).get(0))»);
				«ENDIF»
			«ENDFOR»
		'''
	}
	// -- Helper Methods
	def retrieveQueueNames() {
		queueNames = new HashMap<Connection, String>
		queueTypes = new HashMap<Connection, Type>
		for (connection : network.connections) {
			if (connection.source instanceof Port) {
				if (connection.target instanceof Actor) {
					queueNames.put(connection,
						"q_" + (connection.source as Port).name + "_" + (connection.target as Actor).name + "_" +
							connection.targetPort.name)
					queueTypes.put(connection, IrUtil.copy((connection.source as Port).type))
				}
			} else if (connection.source instanceof Actor) {
				if (connection.target instanceof Port) {
					queueNames.put(connection,
						"q_" + (connection.source as Actor).name + "_" + connection.sourcePort.name + "_" +
							(connection.target as Port).name)
					queueTypes.put(connection, IrUtil.copy((connection.sourcePort as Port).type))
				} else if (connection.target instanceof Actor) {
					queueNames.put(connection,
						"q_" + (connection.source as Actor).name + "_" + connection.sourcePort.name + "_" +
							(connection.target as Actor).name + "_" + connection.targetPort.name)
					queueTypes.put(connection, IrUtil.copy((connection.sourcePort as Port).type))
				}

			}
		}
	}
	
	
	
	
}
