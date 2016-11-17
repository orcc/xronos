/* 
 * XRONOS-EXELIXI
 * 
 * Copyright (C) 2011-2016 EPFL SCI STI MM
 *
 * This file is part of XRONOS-EXELIXI.
 *
 * XRONOS-EXELIXI is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * XRONOS-EXELIXI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with XRONOS-EXELIXI. If not, see <http://www.gnu.org/licenses/>.
 * 
 * Additional permission under GNU GPL version 3 section 7
 * 
 * If you modify this Program, or any covered work, by linking or combining it
 * with Eclipse (or a modified version of Eclipse or an Eclipse plugin or 
 * an Eclipse library), containing parts covered by the terms of the 
 * Eclipse Public License (EPL), the licensors of this Program grant you 
 * additional permission to convey the resulting work.  Corresponding Source 
 * for a non-source form of such a combination shall include the source code 
 * for the parts of Eclipse libraries used as well as that of the covered work.
 * 
 */
package org.xronos.orcc.systemc

import java.text.SimpleDateFormat
import java.util.Date
import java.util.List
import net.sf.orcc.df.Actor
import net.sf.orcc.df.Network
import net.sf.orcc.df.Port

/**
 * SystemC Testbench Printer for Network and Actor
 * 
 * @author Endri Bezati
 */
class TestbenchPrinter extends SystemCTemplate {

	private var Network network

	private var Actor actor

	private var String name
	
	private var String prefix

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
		prefix = "n"
		actor = null
	}

	def setActor(Actor actor) {
		this.actor = actor
		name = actor.simpleName
		prefix = "a"
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
			
			#ifdef __RTL_SIMULATION__
			#include "«name»_rtl_wrapper.h"
			#define «name» «name»_rtl_wrapper 
			#else
			#include "«name».h"
			#endif
			
			#include "tb_kicker.h"
			#include "tb_endsim«IF network != null»_n«ELSE»_a«ENDIF»_«this.name».h"
			«IF !inputs.empty»
				#include "tb_driver.h"
			«ENDIF»
			«IF !outputs.empty»
				#include "tb_compare.h"
			«ENDIF»
			
			int sc_main (int argc , char *argv[]) {
				sc_report_handler::set_actions("/IEEE_Std_1666/deprecated", SC_DO_NOTHING);
				sc_report_handler::set_actions( SC_ID_LOGIC_X_TO_BOOL_, SC_LOG);
				sc_report_handler::set_actions( SC_ID_VECTOR_CONTAINS_LOGIC_VALUE_, SC_LOG);
				// sc_report_handler::set_actions( SC_ID_OBJECT_EXISTS_, SC_LOG);
				
				// -- Control Signals
				sc_signal<bool>    reset;
				sc_signal<bool>    start;
				
				// -- Queues
				«IF !inputs.empty»
					«FOR port : inputs»
						sc_fifo< «port.type.doSwitch» > q_«port.name»;
					«ENDFOR»
				«ENDIF»
				«IF !outputs.empty»
					«FOR port : outputs»
						sc_fifo< «port.type.doSwitch» > q_«port.name»;
					«ENDFOR»
				«ENDIF»
				
				// -- Create a 100ns period clock signal
				sc_clock s_clk("s_clk", 10, SC_NS);
				
				«IF network != null»
					// -- Network module
					«this.name» n_«this.name»("n_«this.name»");
					sc_signal<bool> done_n_«this.name»;
				«ELSE»
					// -- Actor module
					«this.name» a_«this.name»("a_«this.name»");
					sc_signal<bool> done_a_«this.name»;
				«ENDIF»
				
				// -- Testbench Utilities Modules
				tb_kicker i_tb_kicker("i_tb_icker");
				
				tb_endsim«IF network != null»_n«ELSE»_a«ENDIF»_«this.name» i_tb_endsim("i_tb_endsim");
				sc_signal<bool> done_i_tb_endsim;
				
				«IF !inputs.empty»
					// -- Input Drivers
					«FOR port : inputs»
						tb_driver< «port.type.doSwitch» > i_tb_driver_«port.name»("i_tb_driver_«port.name»");
						i_tb_driver_«port.name».set_file_name("../testbench/traces/«IF actor != null»«actor.simpleName»_«ENDIF»«port.name».txt");
						sc_signal<bool> done_i_tb_driver_«port.name»;
					«ENDFOR»
				«ENDIF»
				
				«IF !outputs.empty»
					// -- Compare output with golden reference
					«FOR port : outputs»
						tb_compare< «port.type.doSwitch» > i_tb_compare_«port.name»("i_tb_compare_«port.name»");
						i_tb_compare_«port.name».set_file_name("../testbench/traces/«IF actor != null»«actor.simpleName»_«ENDIF»«port.name».txt");
						i_tb_compare_«port.name».set_port_name("«port.name»");
						sc_signal<bool> done_i_tb_compare_«port.name»;
					«ENDFOR»
				«ENDIF»
			
				// -- Connection of Modules
				
				// -- Generate a reset & start
				i_tb_kicker.clk(s_clk);
				i_tb_kicker.reset(reset);
				i_tb_kicker.start(start);
				
				«IF !inputs.empty»
					// -- Driver Connections
					«FOR port : inputs»
						i_tb_driver_«port.name».clk(s_clk);
						i_tb_driver_«port.name».reset(reset);
						i_tb_driver_«port.name».start(start);
						i_tb_driver_«port.name».done(done_i_tb_driver_«port.name»);
						i_tb_driver_«port.name».dout(q_«port.name»);
					«ENDFOR»
				«ENDIF»
				
				«IF !outputs.empty»
					// -- Compare Connections
					«FOR port : outputs»
						i_tb_compare_«port.name».clk(s_clk);
						i_tb_compare_«port.name».reset(reset);
						i_tb_compare_«port.name».start(start);
						i_tb_compare_«port.name».done(done_i_tb_compare_«port.name»);
						i_tb_compare_«port.name».din(q_«port.name»);
					«ENDFOR»
				«ENDIF»	
				
				i_tb_endsim.clk(s_clk);
				i_tb_endsim.reset(reset);
				i_tb_endsim.start(start);
				i_tb_endsim.done(done_i_tb_endsim);
				«FOR port : outputs»
						i_tb_endsim.done_«port.name»(done_i_tb_compare_«port.name»);
				«ENDFOR»
				
				// -- «this.name» Connections
				«prefix»_«this.name».clk(s_clk);
				«prefix»_«this.name».reset(reset);
				«prefix»_«this.name».start(start);
				«prefix»_«this.name».done(done_«prefix»_«this.name»);
				«IF !inputs.empty»
					«FOR port : inputs»
						«prefix»_«this.name».«port.name»(q_«port.name»);
					«ENDFOR»
				«ENDIF»
				«IF !outputs.empty»
					«FOR port : outputs»
						«prefix»_«this.name».«port.name»(q_«port.name»);
					«ENDFOR»
				«ENDIF»
				
				cout <<"        __  ___ __ ___  _ __   ___  ___ " << endl;
				cout <<"        \\ \\/ / '__/ _ \\| '_ \\ / _ \\/ __|" << endl;
				cout <<"         >  <| | | (_) | | | | (_) \\__ \\" << endl;
				cout <<"        /_/\\_\\_|  \\___/|_| |_|\\___/|___/" << endl;
				cout <<"        CAL to SystemC Code Generator" << endl;
				cout <<"        Copyright (c) 2012-2014 EPFL SCI-STI-MM" << endl;
				
				cout << "\nINFO: Start of Simulating \n" << endl;
				
				// -- Start Simulation 
				sc_start();
				
				cout << "\nINFO: End of Simulating " << endl;
				
				return 0;
			}
		'''
	}

}
