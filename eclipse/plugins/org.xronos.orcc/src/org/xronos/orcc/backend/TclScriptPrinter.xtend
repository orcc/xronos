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
 * If you modify this Program, or any covered work, by linking or 
 * combining it with Eclipse libraries (or a modified version of that 
 * library), containing parts covered by the terms of EPL,
 * the licensors of this Program grant you additional permission to convey 
 * the resulting work. {Corresponding Source for a non-source form of such 
 * a combination shall include the source code for the parts of Eclipse 
 * libraries used as well as that of the  covered work.}
 */
 
package org.xronos.orcc.backend

import java.text.SimpleDateFormat
import java.util.Date
import java.util.HashMap
import java.util.Map
import net.sf.orcc.df.Actor
import net.sf.orcc.df.Network
import net.sf.orcc.graph.Vertex

/*
 * A ModelSim TCL script printer
 * 
 * @author Endri Bezati
 */
class TclScriptPrinter {

	var String rtlPath = "";
	var Boolean xilinxPrimitives = false;
	var Boolean testbench = false;
	var Boolean generateGoDone = false;
	var Boolean doubleBuffering = false;
	var Boolean schedulerInformation = false;
	
	var Map<String, Integer> clockDomainsIndex;
	var String DEFAULT_CLOCK_DOMAIN = "CLK";

	var Network network;

	def headerComments(Object object, String string) {
		var dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
		var date = new Date();
		'''
			## ############################################################################
			## __  ___ __ ___  _ __   ___  ___ 
			## \ \/ / '__/ _ \| '_ \ / _ \/ __|
			##  >  <| | | (_) | | | | (_) \__ \
			## /_/\_\_|  \___/|_| |_|\___/|___/
			## ############################################################################
			## Xronos synthesizer
			«IF object instanceof Network»
				## «string» TCL Script file for Network: «(object as Network).simpleName» 
			«ELSEIF object instanceof Actor»
				## «string» TCL Script file for Actor: «(object as Actor).simpleName» 
			«ENDIF»
			## Date: «dateFormat.format(date)»
			## ############################################################################
		'''
	}

	def setPathandLibrariesLibraries() {
		'''
			## Set paths
			set Lib "../lib/"
			«IF testbench»set LibSim "../lib/simulation"«ENDIF»
			set Rtl "../rtl"
			set RtlGoDone "../rtl/rtlGoDone"
			«IF testbench»set Testbench "vhd"«ENDIF»
			
			«IF xilinxPrimitives»
				## Create the xilinxPrimitives design library
				vlib xilinxPrimitives
				vmap xilinxPrimitives xilinxPrimitives
				vlog -work xilinxPrimitives $Lib/xilinxPrimitives/*.v
				
			«ENDIF»
			## Create SystemBuilder design library
			vlib SystemBuilder
			vmap SystemBuilder SystemBuilder
			
			## Compile the SystemBuilder Library from sources
			vcom -reportprogress 300 -work SystemBuilder $Lib/systemBuilder/vhdl/sbtypes.vhdl
			vcom -reportprogress 300 -work SystemBuilder $Lib/systemBuilder/vhdl/sbfifo.vhdl
			vcom -reportprogress 300 -work SystemBuilder $Lib/systemBuilder/vhdl/sbfifo_behavioral.vhdl
		'''
	}

	def setWorkLibrary(Vertex vertex) {
		var String name;
		if (vertex instanceof Actor) {
			name = (vertex as Actor).simpleName;
		} else if (vertex instanceof Network) {
			name = (vertex as Network).simpleName;
		}
		var String workName;
		if (testbench) {
			workName = "work_" + name;
		} else {
			workName = "work";
		}
		'''
			## Create the work design library
			if {[file exist «workName»]} {vdel -all -lib «workName»}
			vlib «workName»
			vmap «workName» «workName»
			
			## Compile the glbl constans given by Xilinx 
			vlog -work «workName» ../lib/simulation/glbl.v
			
			«IF doubleBuffering»
				vlog -work «workName» $Lib/xronos/controller.v
			«ENDIF»
			
			«IF testbench»
				# Compile sim package
				vcom -93 -reportprogress 30 -work «workName» $LibSim/sim_package.vhd
			«ENDIF»
			## Compile network instances and add them to work library	
			«IF vertex instanceof Network»
				«FOR netVertex : network.vertices»
					«IF netVertex instanceof Actor»
						vlog -work «workName» «rtlPath»/«(netVertex as Actor).simpleName».v
						«IF doubleBuffering»
							«IF (netVertex as Actor).outputs.size > 0»
								vlog -work «workName» $Rtl/«(netVertex as Actor).simpleName»_clock_controller.v
							«ENDIF»
						«ENDIF»
					«ENDIF» 
				«ENDFOR»
			«ELSEIF vertex instanceof Actor»
				vlog -work «workName» «rtlPath»/«(vertex as Actor).simpleName».v
			«ENDIF»
			
			«IF vertex instanceof Network»
				## Compile the Top Network
				vcom -93 -check_synthesis -quiet -work «workName» «rtlPath»/«name».vhd
			«ENDIF»
			
			«IF testbench»
				## Compile the Testbench VHD
				vcom -93 -check_synthesis -quiet -work «workName» $Testbench/«name»_tb.vhd
			«ENDIF»
		'''
	}

	def startSimulation(Vertex vertex) {
		var String name;
		if (vertex instanceof Actor) {
			name = (vertex as Actor).simpleName;
		} else if (vertex instanceof Network) {
			name = (vertex as Network).simpleName;
		}
		var String workName;
		var String simName;
		if (testbench) {
			workName = "work_" + name;
			simName = name + "_tb";
		} else {
			workName = "work";
			simName = name;
		}

		'''
			## Start VSIM
			«IF (xilinxPrimitives)»
				vsim -voptargs="+acc" -L xilinxPrimitives -t ns «workName».glbl «workName».«simName»
			«ELSE»
				vsim -voptargs="+acc" -L unisims_ver -L simprims_ver -t ns «workName».glbl «workName».«simName»
			«ENDIF»	
				
			## Add clock(s) and reset signal
			add wave -noupdate -divider -height 20 "CLK & RESET"
			
			«IF vertex instanceof Network»
				«FOR string : clockDomainsIndex.keySet»
					add wave sim:/«simName»/«string»
					«ENDFOR»
				«ELSE»
					add wave sim:/«simName»/CLK
				«ENDIF»
				add wave sim:/«simName»/RESET
				
				«IF doubleBuffering»
					## Add clock gating enable signal
					add wave -noupdate -divider -height 20 "CLOCK GATE ENABLE"
					add wave sim:/«simName»/CG_EN
				«ENDIF»
				
				## Change radix to decimal
				radix -decimal
			'''
	}

	def addInstanceIO(Network network, Actor actor) {
		var String name = network.simpleName;
		var String simName;
		if (testbench) {
			simName = name + "_tb/i_" + network.simpleName;
		} else {
			simName = name;
		}
		'''
			add wave -noupdate -divider -height 20 i_«actor.simpleName»
			add wave sim:/«simName»/i_«actor.simpleName»/CLK
			«FOR port : actor.inputs SEPARATOR "\n"»
				add wave sim:/«simName»/i_«actor.simpleName»/«port.name»_DATA
				add wave sim:/«simName»/i_«actor.simpleName»/«port.name»_ACK
				add wave sim:/«simName»/i_«actor.simpleName»/«port.name»_SEND
			«ENDFOR» 
			«FOR port : actor.outputs SEPARATOR "\n"»
				add wave sim:/«simName»/i_«actor.simpleName»/«port.name»_DATA
				add wave sim:/«simName»/i_«actor.simpleName»/«port.name»_ACK
				add wave sim:/«simName»/i_«actor.simpleName»/«port.name»_SEND
				add wave sim:/«simName»/i_«actor.simpleName»/«port.name»_RDY
			«ENDFOR» 
			«IF generateGoDone»
				«FOR action : actor.actions»
					add wave -label «action.name»_go sim:/«simName»/i_«actor.simpleName»/«action.name»_go
					add wave -label «action.name»_done sim:/«simName»/i_«actor.simpleName»/«action.name»_done
				«ENDFOR»
			«ENDIF»
			«IF schedulerInformation»
				«FOR action : actor.actions»
					«IF !action.inputPattern.ports.empty»
						add wave -label ta_«action.name» sim:/«simName»/i_«actor.simpleName»/ta_«action.name»
					«ENDIF»
				«ENDFOR»
				add wave -label Idle sim:/«simName»/i_«actor.simpleName»/idle_«actor.simpleName»
				add wave -label Idle_Action sim:/«simName»/i_«actor.simpleName»/idle_action_«actor.simpleName»
			«ENDIF»
		'''

	}

	def addNetworkSignalsToWave(Network network) {
		'''
			«IF testbench»
				«addNetworkInputSignals(network)»
			«ENDIF»
			
			«addNetworkOuputSignals(network)»
			
			«FOR vertex : network.vertices SEPARATOR "\n"»
				«IF vertex instanceof Actor»
					«addInstanceIO(network, vertex as Actor)»
				«ENDIF»
			«ENDFOR»	
			
			«addNetworkFullFifoSignal(network)»
		'''
	}

	def addInstanceSignalsToWave(Actor actor) {
		var String simName = actor.simpleName + "_tb";
		'''
			add wave -noupdate -divider -height 20  "Inputs: i_«actor.simpleName»"
			«FOR port : actor.inputs SEPARATOR "\n"»
				add wave -label «port.name»_DATA sim:/«simName»/i_«actor.simpleName»/«port.name»_DATA
				add wave -label «port.name»_ACK sim:/«simName»/i_«actor.simpleName»/«port.name»_ACK 
				add wave -label «port.name»_SEND sim:/«simName»/i_«actor.simpleName»/«port.name»_SEND 
			«ENDFOR» 
			add wave -noupdate -divider -height 20 "Outputs: i_«actor.simpleName»"
			«FOR port : actor.outputs SEPARATOR "\n"»
				add wave -label «port.name»_DATA sim:/«simName»/i_«actor.simpleName»/«port.name»_DATA 
				add wave -label «port.name»_SEND sim:/«simName»/i_«actor.simpleName»/«port.name»_ACK
				add wave -label «port.name»_SEND sim:/«simName»/i_«actor.simpleName»/«port.name»_SEND
				add wave -label «port.name»_RDY sim:/«simName»/i_«actor.simpleName»/«port.name»_RDY
			«ENDFOR» 
			add wave -noupdate -divider -height 20 "Go & Done" 
			«IF generateGoDone»
				«FOR action : actor.actions»
					add wave -noupdate -divider -height 20 "Action: «action.name»" 
					add wave -label Go sim:/«simName»/i_«actor.simpleName»/«action.name»_go
					add wave -label Done sim:/«simName»/i_«actor.simpleName»/«action.name»_done
				«ENDFOR»
			«ENDIF»
			«IF schedulerInformation»
				«FOR action : actor.actions»
					«IF !action.inputPattern.ports.empty»
						add wave -label ta_«action.name» sim:/«simName»/i_«actor.simpleName»/ta_«action.name»
					«ENDIF»
				«ENDFOR»
				add wave -label Idle sim:/«simName»/i_«actor.simpleName»/idle_«actor.simpleName»
				add wave -label Idle_Action sim:/«simName»/i_«actor.simpleName»/idle_action_«actor.simpleName»
			«ENDIF»
		'''
	}

	def addNetworkInputSignals(Network network) {
		var String name = network.simpleName;
		var String simName;
		if (testbench) {
			simName = name + "_tb";
		} else {
			simName = name;
		}
		'''
			«FOR port : network.inputs SEPARATOR "\n"»
				add wave -noupdate -divider -height 20 ni_«port.name»
				add wave sim:/«simName»/«port.name»_DATA
				«IF (!port.native)»
					add wave sim:/«simName»/«port.name»_ACK
					add wave sim:/«simName»/«port.name»_SEND
				«ENDIF»
			«ENDFOR»
		'''
	}

	def addNetworkOuputSignals(Network network) {
		var String name = network.simpleName;
		var String simName;
		if (testbench) {
			simName = name + "_tb";
		} else {
			simName = name;
		}
		'''
			«FOR port : network.outputs SEPARATOR "\n"»
				add wave -noupdate -divider -height 20 no_«port.name»
				add wave sim:/«simName»/«port.name»_DATA
				«IF (!port.native)»
					add wave sim:/«simName»/«port.name»_SEND
					add wave sim:/«simName»/«port.name»_ACK
					add wave sim:/«simName»/«port.name»_RDY
					«IF (!testbench)»
						## Freeze ACK and RDY at 1
						force -freeze sim:/«simName»/«port.name»_ACK 1 0
						force -freeze sim:/«simName»/«port.name»_RDY 1 0
					«ENDIF»
				«ENDIF»
			«ENDFOR»
		'''
	}

	def addNetworkFullFifoSignal(Network network) {
		var String name = network.simpleName;
		var String simName;
		if (testbench) {
			simName = name + "_tb/i_" + network.simpleName;
		} else {
			simName = name;
		}
		'''
			## FIFO FULL
			add wave -noupdate -divider -height 20 "FIFO FULL"
			«FOR vertex : network.vertices»
				«IF vertex instanceof Actor»
					«FOR port : (vertex as Actor).inputs»
						add wave -label «(vertex as Actor).simpleName»_«port.name»_full sim:/«simName»/q_ai_«(vertex as Actor).simpleName»_«port.
				name»/full
						add wave -label «(vertex as Actor).simpleName»_«port.name»_almost_full sim:/«simName»/q_ai_«(vertex as Actor).
				simpleName»_«port.name»/almost_full
					«ENDFOR»
				«ENDIF»
			«ENDFOR»
			
			«IF doubleBuffering»
				## CLOCK CONTROLLER
				add wave -noupdate -divider -height 20 "CC"
				«FOR vertex : network.vertices»
					«IF vertex instanceof Actor»
						«IF (vertex as Actor).outputs.size > 0»
							add wave -label cc_«(vertex as Actor).simpleName»_clk_out sim:/«simName»/cc_«(vertex as Actor).simpleName»/clk_out
						«ENDIF»
					«ENDIF»
				«ENDFOR»
			«ENDIF»
		'''
	}

	def printNetworkTclScript(Network network, Map<String, Object> options) {
		this.network = network;

		testbench = false;

		if (options.containsKey("generateGoDone")) {
			generateGoDone = options.get("generateGoDone") as Boolean;
		}

		if (options.containsKey("xilinxPrimitives")) {
			xilinxPrimitives = options.get("xilinxPrimitives") as Boolean;
		}

		if (options.containsKey("doubleBuffering")) {
			doubleBuffering = options.get("doubleBuffering") as Boolean;
		}

		if (generateGoDone) {
			rtlPath = "$RtlGoDone";
		} else {
			rtlPath = "$Rtl";
		}

		var Map<String, String> clkDomains = new HashMap<String, String>();

		if (options.containsKey("clkDomains")) {
			clkDomains = options.get("clkDomains") as Map<String,String>;
		}
		computeNetworkClockDomains(network, clkDomains);
		''' 			
			«headerComments(network, "Simulation Launch")»
			
			«setPathandLibrariesLibraries()»
			
			«setWorkLibrary(network)»
			
			«startSimulation(network)»
			
			«addNetworkSignalsToWave(network)»
			
			«simRun(network)»
		'''
	}

	def simRun(Network network) {
		'''
			«IF (network.inputs.empty)»
				force -freeze sim:/«network.simpleName»/CLK 1 0, 0 {50 ns} -r 100
				force -freeze sim:/«network.simpleName»/RESET 1 0
				run 500ns
				force -freeze sim:/«network.simpleName»/RESET 0 0
				run 10us
				wave zoom full
			«ENDIF»		
		'''
	}

	def printNetworkTestbenchTclScript(Network network, Map<String, Object> options) {
		this.network = network;

		testbench = true;

		if (options.containsKey("generateGoDone")) {
			generateGoDone = options.get("generateGoDone") as Boolean;
		}
		
		if (options.containsKey("schedulerInformation")) {
			schedulerInformation = options.get("schedulerInformation") as Boolean;
		}

		if (options.containsKey("xilinxPrimitives")) {
			xilinxPrimitives = options.get("xilinxPrimitives") as Boolean;
		}

		if (options.containsKey("doubleBuffering")) {
			doubleBuffering = options.get("doubleBuffering") as Boolean;
		}

		if (generateGoDone) {
			rtlPath = "$RtlGoDone";
		} else {
			rtlPath = "$Rtl";
		}

		var Map<String, String> clkDomains = new HashMap<String, String>();

		if (options.containsKey("clkDomains")) {
			clkDomains = options.get("clkDomains") as Map<String,String>;
		}
		computeNetworkClockDomains(network, clkDomains);

		''' 			
			«headerComments(network, "Testbench")»
			
			«setPathandLibrariesLibraries()»
			
			«setWorkLibrary(network)»
			
			«startSimulation(network)»
			
			«addNetworkSignalsToWave(network)»
		'''
	}

	def printInstanceTestbenchTclScript(Actor actor, Map<String, Object> options) {
		this.network = network;

		testbench = true;

		if (options.containsKey("generateGoDone")) {
			generateGoDone = options.get("generateGoDone") as Boolean;
		}

		if (options.containsKey("schedulerInformation")) {
			schedulerInformation = options.get("schedulerInformation") as Boolean;
		}
		
		if (options.containsKey("xilinxPrimitives")) {
			xilinxPrimitives = options.get("xilinxPrimitives") as Boolean;
		}

		if (generateGoDone) {
			rtlPath = "$RtlGoDone";
		} else {
			rtlPath = "$Rtl";
		}
		''' 			
			«headerComments(actor, "Testbench")»
			
			«setPathandLibrariesLibraries()»
			
			«setWorkLibrary(actor)»
			
			«startSimulation(actor)»
			
			«addInstanceSignalsToWave(actor)»
		'''
	}

	def void computeNetworkClockDomains(Network network, Map<String, String> clockDomains) {
		clockDomainsIndex = new HashMap<String, Integer>();

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
	}

}
