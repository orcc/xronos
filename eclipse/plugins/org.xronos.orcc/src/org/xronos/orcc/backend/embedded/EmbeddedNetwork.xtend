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

import java.util.Map
import net.sf.orcc.df.Actor
import net.sf.orcc.df.Instance
import net.sf.orcc.df.Network
import net.sf.orcc.util.FilesManager

class EmbeddedNetwork extends ExprAndTypePrinter {

	protected Network network

	new(Network network, Map<String, Object> options) {
		this.network = network
	}

	def printNetwork(String targetFolder) {
		val content = compileNetwork
		FilesManager.writeFile(content, targetFolder, network.simpleName + ".h")
	}

	def printMain(String targetFolder) {
		val content = compileMain
		FilesManager.writeFile(content, targetFolder, "main.cpp")
	}

	def compileMain() '''
		#include "«network.simpleName».h"
		
		int main(int argc, char *argv[]){
			/*
				Your Code Goes Here
			*/
			
			return 0;
		}
	'''

	def compileNetwork() '''
		#include <map>
		#include <string>
		
		#include "fifo.h"
		#include "actor.h"
		
		#ifdef _WIN32	
		#undef IN
		#undef OUT
		#endif
	
		«FOR instance : network.children.filter(typeof(Actor))»
		#include "«instance.name».h"
		«ENDFOR»
	
		int «network.simpleName»(«compileParameters»);
		
		int «network.simpleName»(«compileParameters»){
		«FOR instance : network.children.filter(typeof(Instance))»
			«instance.name» *inst_«instance.name» = new «instance.name»(«FOR arg : instance.arguments SEPARATOR ", "»«arg.value.
		doSwitch»«ENDFOR»);
		«ENDFOR»
	
			«FOR instance : network.children.filter(typeof(Instance))»
				«FOR edges : instance.outgoingPortMap.values»
					Fifo<«edges.get(0).sourcePort.type.doSwitch», «edges.get(0).getAttribute("nbReaders").objectValue»> *fifo_«edges.
		get(0).getAttribute("idNoBcast").objectValue» = new Fifo<«edges.get(0).sourcePort.type.doSwitch», «edges.get(0).
		getAttribute("nbReaders").objectValue»>«IF edges.get(0).size != null»(«edges.get(0).size»)«ENDIF»;
				«ENDFOR»
			«ENDFOR»
	
			«FOR e : network.connections»
				inst_«(e.source as Actor).name»->port_«e.sourcePort.name» = fifo_«e.getAttribute("idNoBcast").objectValue»;
				inst_«(e.target as Actor).name»->port_«e.targetPort.name» = fifo_«e.getAttribute("idNoBcast").objectValue»;
			«ENDFOR»
	
			EStatus status = None;
			do{
		status = None;
		«FOR instance : network.children.filter(typeof(Actor))»
			inst_«instance.name»->schedule(status);
		«ENDFOR»
			}while(status != None);
	
		// Clean Actors
		«FOR instance : network.children.filter(typeof(Actor))»
		delete «instance.name»;
		«ENDFOR»
	
		// Clean FIFOs
		«FOR instance : network.children.filter(typeof(Actor))»
		«FOR edges : instance.outgoingPortMap.values»
			delete fifo_«edges.get(0).getAttribute("idNoBcast").objectValue»;
		«ENDFOR»
		«ENDFOR»
	
		return 0; 
		}
	'''

	def compileParameters() 
		'''«FOR param : network.parameters SEPARATOR ", "»«param.type.doSwitch» «FOR dim : param.
		type.dimensions»*«ENDFOR»«param.name»«ENDFOR»'''

	def compileCmakeLists() '''
		cmake_minimum_required (VERSION 2.8)
	
		project («network.simpleName»)
	
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
		# set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -fpermissive")
	
		set(EMBEDDEDCPP_INCLUDE_DIR ./lib/include)
		subdirs(./lib)
	
		include_directories(${EMBEDDEDCPP_INCLUDE_DIR})
	
		add_executable ( «network.simpleName»
		src/main.cpp
		src/«network.simpleName».h
		«FOR instance : network.children.filter(typeof(Actor))»
			src/«instance.name».h
		«ENDFOR»
		)
	
		set(libraries EmbeddedCPP)
		
		
		set(libraries ${libraries} ${CMAKE_THREAD_LIBS_INIT})
		target_link_libraries(«network.simpleName» ${libraries})
	'''

	def printCMakeLists(String targetFolder) {
		val content = compileCmakeLists
		FilesManager.writeFile(content, targetFolder, "CMakeLists.txt")
	}

}
