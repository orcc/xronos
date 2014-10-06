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
import net.sf.orcc.df.Instance
import net.sf.orcc.df.Network
import net.sf.orcc.df.util.DfVisitor
import net.sf.orcc.graph.Vertex
import java.util.List
import net.sf.orcc.df.Port

/**
 * SystemC Testbench Printer for Network and Actor
 * 
 * @author Endri Bezati
 */
class TestbenchPrinter extends SystemCTemplate {

	private var Network network

	private var Instance instance

	private var Actor actor

	private var String name

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
			// Xronos SystemC, Testbench Generator
			// For «IF network != null»Top level Network: «name»«ELSE» Actor: «name»«ENDIF» 
			// Date: «dateFormat.format(date)»
			// ----------------------------------------------------------------------------
		'''
	}

	def setNetwork(Network network) {
		this.network = network
		name = network.simpleName
		actor = null
	}

	def setActor(Actor actor) {
		this.actor = actor
		name = actor.simpleName
		network = null
	}

	def getContent() {
		var List<Port> inputs = null
		var List<Port> outputs = null
		if (network != null) {
			inputs = network.inputs
			outputs = network.outputs
		} else {
			inputs = actor.inputs
			outputs = actor.outputs
		}
		'''
			«header»
			#include "systemc.h"
			
			#include "«name».h"
			
			int sc_main (int argc , char *argv[]) {
				sc_report_handler::set_actions("/IEEE_Std_1666/deprecated", SC_DO_NOTHING);
				sc_report_handler::set_actions( SC_ID_LOGIC_X_TO_BOOL_, SC_LOG);
				sc_report_handler::set_actions( SC_ID_VECTOR_CONTAINS_LOGIC_VALUE_, SC_LOG);
				sc_report_handler::set_actions( SC_ID_OBJECT_EXISTS_, SC_LOG);
				
				// -- Control Signals
				sc_signal<bool>    reset;
				sc_signal<bool>    start;
				
				// Create a 100ns period clock signal
				sc_clock s_clk1("s_clk1", 50, SC_NS);
				
				«IF network != null»
					// -- Network module
					«this.name» n_«this.name»("n_«this.name»");
				«ELSE»
					// -- Actor module
					«this.name» a_«this.name»("a_«this.name»");
				«ENDIF»
				
				// Testbench utilities Modules
				tb_kicker i_tb_kicker("i_tb_icker");
				«IF !inputs.empty»
					«FOR port : inputs»
						tb_driver i_tb_driver_«port.name»("i_tb_driver_«port.name»");
					«ENDFOR»
				«ENDIF»
				«IF !outputs.empty»
					«FOR port : inputs»
						tb_compare i_tb_compare_«port.name»("i_tb_compare_«port.name»");
					«ENDFOR»
				«ENDIF»
			}
		'''
	}

}
