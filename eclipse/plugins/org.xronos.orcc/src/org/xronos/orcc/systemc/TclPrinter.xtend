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
import java.util.Map
import net.sf.orcc.df.Actor
import net.sf.orcc.df.Network
import net.sf.orcc.df.util.DfVisitor
import net.sf.orcc.graph.Vertex

/**
 * SystemC TCL Script Printer for Network and Actor
 * 
 * @author Endri Bezati
 */
class TclPrinter extends DfVisitor<Void> {

	private var Vertex vertex

	private String name

	private Map<String, Object> options

	def setActor(Actor actor) {
		this.vertex = actor
		this.name = actor.simpleName
	}

	def setNetwork(Network network) {
		this.vertex = network
		this.name = network.simpleName
	}

	def setOptions(Map<String, Object> options) {
		this.options = options
	}

	def private getHeader(String hlsTool) {
		var dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
		var date = new Date();
		'''
			## ############################################################################
			## __  ___ __ ___  _ __   ___  ___ 
			## \ \/ / '__/ _ \| '_ \ / _ \/ __|
			##  >  <| | | (_) | | | | (_) \__ \
			## /_/\_\_|  \___/|_| |_|\___/|___/
			## ############################################################################
			## This file is generated automatically by Xronos HLS
			## ############################################################################
			## Xronos SystemC, «hlsTool» TCL Script
			«IF vertex instanceof Network»
				## TCL Script file for Network: «this.name» 
			«ELSE»
				## TCL Script file for Actor: «this.name» 
			«ENDIF»
			## Date: «dateFormat.format(date)»
			## ############################################################################
		'''
	}
	
	// -- Vivado HLS
	def getContentForVivado() '''
		«getHeader("Vivado HLS")»
		set SrcPath "../../src/"
		set SrcHeaderPath "../../src/header"
		set TbPath "../../testbench"
		set SrcTbPath "../../testbench/src"
		set HeaderTbPath "../../testbench/src/header"
		
		## -- Create Project
		open_project proj_«this.name»
		
		## -- Add Design Files
		«IF vertex instanceof Network»
			## -- Actor Modules
			«FOR child : vertex.children»
				add_files $SrcPath/«child.label».cpp -cflags "-I$SrcHeaderPath"
			«ENDFOR»
			## -- Network Top Module
			add_files $SrcPath/«this.name».cpp -cflags "-I$SrcHeaderPath"
		«ELSE»
			add_files $SrcPath/«this.name».cpp -cflags "-I$SrcHeaderPath"
		«ENDIF»
		
		## -- Add TestBench File
		add_files -tb $HeaderTbPath/tb_kicker.h
		add_files -tb $HeaderTbPath/tb_driver.h
		add_files -tb $HeaderTbPath/tb_compare.h
		add_files -tb $HeaderTbPath/tb_endsim«IF vertex instanceof Network»_n«ELSE»_a«ENDIF»_«this.name».h
		add_files -tb $SrcTbPath/tb_«this.name».cpp -cflags "-I$SrcHeaderPath -I$HeaderTbPath/"
		
		## -- Set Top Level
		set_top «this.name»
		
		## -- Create Solution
		open_solution -reset xronos_solution_1
		
		## -- Define Xilinx Technology (TBD: add option of FPGA technology)
		set_part  {xc7z020clg484-1}
		
		## -- Define Clock period
		create_clock -period 10 -name clk
		
		## -- Compilation and Pre Synthesis
		csim_design
		
		# -- Run Synthesis
		csynth_design
		
		## -- RTL Simulation
		cosim_design -rtl systemc
		
		## -- Export RTL implementation
		export_design -format ip_catalog
		
	'''
	// -- Catapult
	def getContentForCatapult() '''
		«getHeader("Catapult")»
	'''
	
	// -- Modelsim
	def getContentForModelSim() '''
		«getHeader("ModelSim")»
		
		## -- Set RTL Folder
		set RTL "../../rtl"
		set TB "../../testbench/vhdl"
		
		## -- Create the work design library
		if {[file exist work_«name»]} {vdel -all -lib work_«name»}
		vlib work_«name»
		vmap work_«name» work_«name»
		
		
		## -- Compile network and actors add them to work library
		«IF vertex instanceof Network»
			vcom -93 -check_synthesis -quiet -work work_«name» $RTL/*.vhd
			##vlog -work work_«name» $RTL/*.v
		«ELSE»
			vcom -93 -check_synthesis -quiet -work work_«name» $RTL/«name»*.vhd
			##vlog -work work_«name»  $RTL/«name»*.v
		«ENDIF»
		
		## -- Compile Sim Package
		vcom -93 -check_synthesis -quiet -work work_«name» $TB/sim_package.vhd
		
		## -- Compile the Tesbench
		vcom -93 -check_synthesis -quiet -work work_«name» $TB/«name»_tb.vhd
		
		## -- Start VSIM
		vsim -voptargs="+acc" -t ns work_«name».«name»_tb
		
		## Add clock(s) and reset signal
		add wave -noupdate -divider -height 20 "CLK & RESET"
		add wave sim:/«name»_tb/CLK
		
	'''
	

}
