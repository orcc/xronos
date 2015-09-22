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
package org.xronos.orcc.backend.embedded

import java.text.SimpleDateFormat
import java.util.Date
import java.util.HashSet
import java.util.Map
import net.sf.orcc.df.Actor
import net.sf.orcc.df.Network
import net.sf.orcc.df.Port
import net.sf.orcc.ir.Var
import net.sf.orcc.util.FilesManager

class EmbeddedNetwork extends ExprAndTypePrinter {

	protected Network network

	private Boolean v7Profiling = false
	
	new(Network network, Map<String, Object> options) {
		this.network = network
		if(options.containsKey("org.xronos.orcc.ARMv7Profiling")){
			v7Profiling = options.get("org.xronos.orcc.ARMv7Profiling") as Boolean
		}
	}

	def printNetwork(String targetFolder) {
		val content = compileNetworkClass
		FilesManager.writeFile(content, targetFolder, network.simpleName + ".h")
	}

	def printMain(String targetFolder) {
		val content = compileMain
		FilesManager.writeFile(content, targetFolder, "main.cpp")
	}

	def compileMain() '''
		// -- CPP Lib Headers
		#include "get_opt.h"
		#include "actor.h"
		#include "fifo.h"
		
		// -- Actors Headers
		«FOR actor : network.children.filter(typeof(Actor))»
			#include "«actor.name».h"
		«ENDFOR»
		
		«IF network.hasAttribute("network_shared_variables")»
			// -- Shared Variables
			«FOR v : network.getAttribute("network_shared_variables").objectValue as HashSet<Var>»
				«v.type.doSwitch» «v.name»«FOR dim : v.type.dimensions»[«dim»]«ENDFOR»;
			«ENDFOR»
		«ENDIF»
		
		int main(int argc, char *argv[]){
			
			// Actors
			«FOR actor : network.children.filter(typeof(Actor))»
				«actor.name» *act_«actor.name» = new «actor.name»(«FOR variable : actor.parameters SEPARATOR ", "»«variable.initialValue.doSwitch»«ENDFOR»);
			«ENDFOR»
			
			// FIFO Queues
			«FOR actor : network.children.filter(typeof(Actor))»
				«FOR edges : actor.outgoingPortMap.values»
					Fifo<«edges.get(0).sourcePort.type.doSwitch», «edges.get(0).getAttribute("nbReaders").objectValue»> *fifo_«edges.get(0).getAttribute("idNoBcast").objectValue» = new Fifo<«edges.get(0).sourcePort.type.doSwitch», «edges.get(0).getAttribute("nbReaders").objectValue»>«IF edges.get(0).size == null»(«fifoSize»)«ELSE»(«edges.get(0).size»)«ENDIF»;
				«ENDFOR»
			«ENDFOR»
			
			// Connections
			«FOR e : network.connections»
				act_«(e.source as Actor).name»->port_«e.sourcePort.name» = fifo_«e.getAttribute("idNoBcast").objectValue»;
				act_«(e.target as Actor).name»->port_«e.targetPort.name» = fifo_«e.getAttribute("idNoBcast").objectValue»;
			«ENDFOR»
			
			// -- Get Input Arguments
			GetOpt options = GetOpt(argc, argv);
			options.getOptions();
		
			// -- Initialize Actors
			«FOR actor : network.children.filter(typeof(Actor))»
				act_«actor.name»->initialize();
			«ENDFOR»
		
			// -- Run 
			EStatus status = None;
			do{
				status = None;
				«FOR actor : network.children.filter(typeof(Actor))»
					act_«actor.name»->action_selection(status);
				«ENDFOR»
			}while (status != None);
		
		
			«FOR actor : network.children.filter(typeof(Actor))»
				delete act_«actor.name»;
			«ENDFOR»
			
			«FOR actor : network.children.filter(typeof(Actor))»
				«FOR edges : actor.outgoingPortMap.values»
					delete fifo_«edges.get(0).getAttribute("idNoBcast").objectValue»;
				«ENDFOR»
			«ENDFOR»
		
			return 0;
		
			//EOF
		}
	'''


	// -- Get Content For each Top Level
	def getFileHeader() {
		var dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
		var date = new Date();
		'''
			// ----------------------------------------------------------------------------
			// __  ___ __ ___  _ __   ___  ___ 
			// \ \/ / '__/ _ \| '_ \ / _ \/ __|
			//  >  <| | | (_) | | | | (_) \__ \
			// /_/\_\_|  \___/|_| |_|\___/|___/
			// ----------------------------------------------------------------------------
			// This file is generated automatically by Xronos C++ 
			// ----------------------------------------------------------------------------
			// Xronos C++, Network Generator
			// Network: «network.simpleName» 
			// Date: «dateFormat.format(date)»
			// ----------------------------------------------------------------------------
		'''
	}


	def compileNetworkClass(){
		'''
		«getFileHeader»
		
		#ifndef __«network.simpleName.toUpperCase»_H__
		#define __«network.simpleName.toUpperCase»_H__
		#include <map>
		#include <string>
		
		#include "fifo.h"
		«IF v7Profiling»
			#include "v7_pmu.h"
		«ENDIF»
		
		«FOR actor : network.children.filter(typeof(Actor))»
		#include "«actor.name».h"
		«ENDFOR»
		
		class «network.simpleName»{
		
		private:
			// Actors
			«FOR actor : network.children.filter(typeof(Actor))»
				«actor.name» *act_«actor.name»;
			«ENDFOR»
			
			// FIFO Queues
			«FOR actor : network.children.filter(typeof(Actor))»
				«FOR edges : actor.outgoingPortMap.values»
					Fifo<«edges.get(0).sourcePort.type.doSwitch», «edges.get(0).getAttribute("nbReaders").objectValue»> *fifo_«edges.get(0).getAttribute("fifoName").stringValue»;
				«ENDFOR»
			«ENDFOR»
		
		public:
			«network.simpleName»(){
				«FOR actor : network.children.filter(typeof(Actor))»
					act_«actor.name» = new «actor.name»(«FOR variable : actor.parameters SEPARATOR ", "»«variable.initialValue.doSwitch»«ENDFOR»);
				«ENDFOR»
				
				«FOR actor : network.children.filter(typeof(Actor))»
					«FOR edges : actor.outgoingPortMap.values»
						fifo_«edges.get(0).getAttribute("fifoName").stringValue» = new Fifo<«edges.get(0).sourcePort.type.doSwitch», «edges.get(0).getAttribute("nbReaders").objectValue»>«IF edges.get(0).size != null»(«edges.get(0).size»)«ENDIF»;
					«ENDFOR»
				«ENDFOR»
				
				«FOR e : network.connections»
					«IF (e.source instanceof Actor && e.target instanceof Actor)»
						act_«(e.source as Actor).name»->port_«e.sourcePort.name» = fifo_«e.getAttribute("fifoName").stringValue»;
						act_«(e.target as Actor).name»->port_«e.targetPort.name» = fifo_«e.getAttribute("fifoName").stringValue»;
					«ELSEIF (e.source instanceof Port && e.target instanceof Actor)»
						act_«(e.target as Actor).name»->port_«e.targetPort.name» = fifo_«(e.source as Port).name»;
					«ELSEIF (e.source instanceof Actor && e.target instanceof Port)»
						act_«(e.source as Actor).name»->port_«e.sourcePort.name» = fifo_«(e.target as Port).name»;
					«ENDIF»
				«ENDFOR»
			}
			
			~«network.simpleName»(){
				«FOR actor : network.children.filter(typeof(Actor))»
					delete act_«actor.name»;
				«ENDFOR»
				
				«FOR actor : network.children.filter(typeof(Actor))»
					«FOR edges : actor.outgoingPortMap.values»
						delete fifo_«edges.get(0).getAttribute("fifoName").stringValue»;
					«ENDFOR»
				«ENDFOR»
			}
			
			«IF !network.inputs.empty || !network.outputs.empty»// Network«ENDIF»«IF !network.inputs.empty» Input«ENDIF»«IF !network.outputs.empty» «IF !network.inputs.empty»/«ENDIF»Output«ENDIF»
			«FOR port: network.inputs»
				Fifo<«port.type.doSwitch», «port.getAttribute("nbReaders").objectValue»> *fifo_«port.name»;
			«ENDFOR»
			«FOR port: network.outputs»
				Fifo<«port.type.doSwitch», 1> *fifo_«port.name»;
			«ENDFOR»
			
			
			
			void run(){
				«IF v7Profiling»
					enable_pmu();              // Enable the PMU
					ccnt_divider(0);           // Cycle Accurate without Divider
				«ENDIF»
				EStatus status = None;
				do{
					status = None;
					«FOR actor : network.children.filter(typeof(Actor))»
						act_«actor.name»->action_selection(status);
					«ENDFOR»
				}while (status != None);
			
			}
			
			void printProfiling(){
				«IF v7Profiling»
					// Print Profiling
					std::cout << "Actor; Action; Cycles; Execution" << std::endl;
					«FOR actor : network.children.filter(typeof(Actor))»
						act_«actor.name»->printActorProfilingCSV();
					«ENDFOR»
				«ENDIF»
			}
		
		};
		#endif //__«network.simpleName.toUpperCase»_H__
		
		
		'''
	}


	def compileParameters() 
		'''«FOR param : network.parameters SEPARATOR ", "»«param.type.doSwitch» «FOR dim : param.
		type.dimensions»*«ENDFOR»«param.name»«ENDFOR»'''

	def compileCmakeLists() '''
		cmake_minimum_required (VERSION 2.8)
	
		project («network.simpleName»)
	
		find_package(Threads REQUIRED)
		if(NOT NO_DISPLAY)
			find_package(SDL REQUIRED)	
		endif()
	
	
		find_package(Threads REQUIRED)
	
		if(MSVC)
		set(CMAKE_CXX_FLAGS_DEBUG "/D_DEBUG /MTd /ZI /Ob0 /Od /RTC1")
		set(CMAKE_CXX_FLAGS_RELEASE "/MT /O2 /Ob2 /D NDEBUG")
		endif()
	
		if(CMAKE_COMPILER_IS_GNUCXX)
		set(CMAKE_CXX_FLAGS_DEBUG "${CMAKE_CXX_FLAGS_DEBUG} -O0 -g")
		set(CMAKE_CXX_FLAGS_RELEASE "${CMAKE_CXX_FLAGS_RELEASE} -O3")
		endif()
		
		# Use this flag if unsigned / signed conversions produces errors
		set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -fpermissive")
	
		set(EMBEDDEDCPP_INCLUDE_DIR ./lib/include)
		subdirs(./lib)
	
		include_directories(${EMBEDDEDCPP_INCLUDE_DIR})
	
		if(NOT NO_DISPLAY)
			include_directories(${SDL_INCLUDE_DIR})
		endif()
	
		add_executable ( «network.simpleName»
		src/main.cpp
		src/«network.simpleName».h
		«FOR instance : network.children.filter(typeof(Actor))»
			src/«instance.name».h
		«ENDFOR»
		)
	
		set(libraries EmbeddedCPP)
	
		if(NOT NO_DISPLAY)
			set(libraries ${libraries} ${SDL_LIBRARY})
		endif()
	
		set(libraries ${libraries} ${CMAKE_THREAD_LIBS_INIT})
		target_link_libraries(«network.simpleName» ${libraries})
	'''

	def printCMakeLists(String targetFolder) {
		val content = compileCmakeLists
		FilesManager.writeFile(content, targetFolder, "CMakeLists.txt")
	}

}
