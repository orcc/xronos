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

import net.sf.orcc.df.Network

/**
 * A README Printer
 * 
 * @author Endri Bezati
 */
class ReadMePrinter {
	var Network network
	
	def setNetwork(Network network) {
		this.network = network
	}
	
	
	def getContent()'''
		// ----------------------------------------------------------------------------
		// __  ___ __ ___  _ __   ___  ___ 
		// \ \/ / '__/ _ \| '_ \ / _ \/ __|
		//  >  <| | | (_) | | | | (_) \__ \
		// /_/\_\_|  \___/|_| |_|\___/|___/
		// ----------------------------------------------------------------------------
		// This file is generated automatically by Xronos HLS
		// ----------------------------------------------------------------------------
		// README for Network: «this.network.simpleName»
		// ----------------------------------------------------------------------------
		
		You have generated code using the Xronos synthesizable SystemC Code generation
		
		Xronos generates three folders src, rtl and testbench
		
		- src, 
			is where your generated code is stored
		
		- rtl, 
			is the place when all the synthesizable code 
			after HLS synthesis(Catapult, Cynthesizer, Vivado HLS, ...)
		
		- tesbench, 
			is the place where all SystemC testbenches are stored also
				- queueTraces,
					where you should put all the traces, golden references
					provided by Orcc C backend
				- network,
					where the network testbench is located
				- actors,
					where all the actor testbenched are located
					
		- scripts,
			where all tcl scipts for launching the HLS synthesis and simulation
		
		
		For all questions on SystemC code generator please contact :
			- endri.bezati@epfl.ch
		
	'''
}