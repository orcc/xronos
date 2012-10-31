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

import net.sf.orcc.ir.util.IrSwitch
import net.sf.orcc.df.Instance
import net.sf.orcc.df.Network
import java.util.Map
import java.text.SimpleDateFormat
import java.util.Date
import net.sf.orcc.graph.Vertex

/*
 * A ModelSim TCL script printer
 * 
 * @author Endri Bezati
 */
class TclScriptPrinter extends IrSwitch {
	
	var String rtlPath = "";
	var Boolean xilinxPrimitives = false;
	var Boolean testbench = false;
	
	var Network network;
	
	def headerComments(Object object){
		var dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
		var date = new Date();
		'''
		## ############################################################################
		## Chronos synthesizer
		«IF object instanceof Network »
		## TCL Script file for «(object as Network).simpleName» Network
		«ELSEIF object instanceof Instance»
		## TCL Script file for «(object as Instance).simpleName» Instance
		«ENDIF»
		## Date : Date :  «dateFormat.format(date)»
		## ############################################################################
		'''	
		}
	
	def setPathandLibrariesLibraries(){
		'''
		## Set paths
		set Lib "../lib/"
		set LibSim "../lib/simulation"
		set Rtl "../rtl"
		set RtlGoDone "../rtl/rtlGoDone"
		
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
	
	def setWorkLibrary(Network network){
		'''
		## Create the work design library
		if {[file exist work]} {rm -r work}
		vlib work
		vmap work work
		
		## Compile the glbl constans given by Xilinx 
		vlog ../lib/simulation/glbl.v
		
		## Compile network instances and add them to work library	
		«FOR vertex : network.vertices»
		«IF vertex instanceof Instance»
		vlog «rtlPath»/«(vertex as Instance).simpleName».v
		«ENDIF» 
		«ENDFOR»
		
		## Compile the Top Network
		vcom -93 -check_synthesis -quiet -work work «rtlPath»/«network.simpleName».vhd
		
		'''
	}
	
	def startSimulation(Network network){
		'''
		«IF(xilinxPrimitives)»
		vsim -L xilinxPrimitives -t ns work.glbl work.«network.simpleName»
		«ELSE»
		vsim -L unisims_ver -L simprims_ver -t ns work.glbl work.«network.simpleName»
		«ENDIF»		
		## Add clock(s) and reset signal
		add wave sim:/«network.simpleName»/CLK
		add wave sim:/«network.simpleName»/RESET
		
		## Change radix to decimal
		radix -decimal
		
		'''
	}
	
	def addInstanceIO(Instance instance){
		'''
		add wave -noupdate -divider -height 20 i_«instance.simpleName»
		«FOR port : instance.actor.inputs SEPARATOR "\n"»
		add wave sim:/«network.simpleName»/i_«instance.simpleName»/«port.name»_DATA
		add wave sim:/«network.simpleName»/i_«instance.simpleName»/«port.name»_ACK
		«ENDFOR» 
		«FOR port : instance.actor.outputs SEPARATOR "\n"»
		add wave sim:/«network.simpleName»/i_«instance.simpleName»/«port.name»_DATA
		add wave sim:/«network.simpleName»/i_«instance.simpleName»/«port.name»_SEND
		add wave sim:/«network.simpleName»/i_«instance.simpleName»/«port.name»_RDY
		«ENDFOR» 
		'''
		
	}
	
	def addNetworkSignalsToWave(Network network){
		// add instance IO signals
		for (Vertex vertex: network.vertices){
			if(vertex instanceof Instance){
				addInstanceIO(vertex as Instance);
			}
		}
		
		// add network outputs port signals and full queue instance signal
		'''
		«FOR port : network.outputs SEPARATOR "\n"»
		add wave -noupdate -divider -height 20 no_«port.name»
		add wave sim:/«network.simpleName»/«port.name»_DATA
		«IF (!port.native)»
		add wave sim:/«network.simpleName»/«port.name»_SEND
		add wave sim:/«network.simpleName»/«port.name»_ACK
		add wave sim:/«network.simpleName»/«port.name»_RDY
		«ENDIF»
		«ENDFOR»

		## FIFO FULL
		add wave -noupdate -divider -height 20 "FIFO FULL"
		«FOR vertex : network.vertices»
		«IF vertex instanceof Instance»
		«FOR port : (vertex as Instance).actor.inputs»
		add wave sim:/«network.simpleName»/q_ai_«(vertex as Instance).simpleName»_«port.name»/fifo/msync_full 
		«ENDFOR»
		«ENDIF»
		«ENDFOR»
		
		'''
	}
	
	def printNetworkTclScript(Network network, Map<String, Object> options){
		var Boolean generateGoDone = false;
		
		this.network = network;
		
		testbench = false;
		
		if (options.containsKey("generateGoDone")) {
			generateGoDone= options.get("generateGoDone") as Boolean;
		}
		
		if (options.containsKey("xilinxPrimitives")) {
			xilinxPrimitives = options.get("xilinxPrimitives") as Boolean;
		}
		
		if (generateGoDone){
			rtlPath = "$RtlGoDone";
		}else{
			rtlPath = "$Rtl";
		}
					
		headerComments(network);
		setPathandLibrariesLibraries();
		setWorkLibrary(network);
		startSimulation(network);
		addNetworkSignalsToWave(network);
		
		'''
		«IF (!network.inputs.empty)»
		force -freeze sim:/<network.simpleName>/CLK 1 0, 0 {50 ns} -r 100
		force -freeze sim:/<network.simpleName>/RESET 1 0
		run 500ns
		force -freeze sim:/<network.simpleName>/RESET 0 0
		run 10us
		wave zoom full
		«ENDIF»		
		'''
	}
	
	def printNetworkTestbenchTclScript(Network network, Map<String, Object> options){
		headerComments(network);
		setPathandLibrariesLibraries();
		
	}
	
	def printInstanceTestbenchTclScript(Instance instance, Map<String, Object> options){
		testbench = true;
		
		headerComments(instance);
		setPathandLibrariesLibraries();
	}
	
}