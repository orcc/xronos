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
			is the place where all SystemC testbenches are stored
				- traces,
					where you should put all the traces, golden references
					provided by Orcc C backend or the Orcc simulator, a 
					refactoring of the traces name should be given 
				- network,
					where the network testbench is located
				- actors,
					where all the actor testbenched are located
					
		- scripts,
			where all tcl scipts for launching the HLS synthesis and simulation
		
		Tools that you need:
			- G++ or Clnag
			- SystemC 2.3.1 or later, for abstract simulation of the generated code
			- Vivado HLS 2014.3 or later for HLS synthesis (Vivado System Edition or
			   Web Edition)
			- Calypto Catapult, Cynthesizer, CyberWorkBench, C-To-Verilog or other Tool
		
		We provide scripts only for Vivado HLS for the moment, if other tool tested, 
		we are going to add the necessary tcl scripts.
		
		
		HowTo Vivado HLS :

		Launch Vivado HLS from command line
			- On Linux : Open the command prompt
			- On Windows : On start menu choose Vivado HLS "Version" Command Prompt
		
		On the terminal type
			> vivado_hls tcl_"name of the top or actor".tcl
		
		This script will launch the SystemC simulation for verification, Synthesis, 
		Co-Simulation and will export the design.
		
		If you would like another clk period than 10ns, and not all the steps described 
		above you can comment with "#" the necessary lines. 
		
		
		For all questions on SystemC code generator please contact :
			- endri.bezati@epfl.ch
		
	'''
}