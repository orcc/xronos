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
package org.xronos.orcc.systemc

import java.text.SimpleDateFormat
import java.util.Date
import net.sf.orcc.df.Actor
import net.sf.orcc.df.Network
import net.sf.orcc.df.Port

/**
 * SystemC Network Printer
 * 
 * @author Endri Bezati
 */
class NetworkPrinter extends SystemCTemplate {

	var Network network

	var String name

	def setNetwork(Network network) {
		this.network = network
		this.name = network.simpleName
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
		
		#include"systemc.h"
		
		SC_MODULE(«this.name»){
		
			// -- Control Ports
			
			sc_in<bool>   clock;
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
			
			// -- Actors
			«FOR child : network.children»
				«child.label» i_«child.label»;
			«ENDFOR»
			
			// -- Constructor
			SC_CTOR(«this.name»)
				:clock("clock")
				,reset("reset")
				,start("start")
				,done("done")
			{

			}
	'''

	def getPortDeclaration(String direction, Port port) '''
		sc_fifo_«direction»<«port.type.doSwitch»> «port.name»;
	'''

	// -- TODO: Name to be calculated before printing
	def getQueuesDeclarationContent()'''
		«FOR connection : network.connections»
			«IF connection.source instanceof Port»
				«IF connection.target instanceof Actor»
					sc_fifo<«(connection.source as Port).type.doSwitch»> q_«(connection.source as Port).name»_«(connection.target as Actor).name»_«connection.targetPort.name»(«connection.size»);
				«ENDIF»
			«ELSEIF connection.source instanceof Actor»
				«IF connection.target instanceof Port»
					sc_fifo<«(connection.sourcePort as Port).type.doSwitch»> q_«(connection.source as Actor).name»_«connection.sourcePort.name»_«(connection.target as Port).name»(«connection.size»);
				«ELSEIF connection.target instanceof Actor»
					sc_fifo<«(connection.sourcePort as Port).type.doSwitch»> q_«(connection.source as Actor).name»_«connection.sourcePort.name»_«(connection.target as Actor).name»_«connection.targetPort.name»(«connection.size»);
				«ENDIF»
			«ENDIF»
		«ENDFOR»
	'''

}
