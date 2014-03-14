package org.xronos.orcc.backend.embedded

import java.io.File
import java.util.Map
import net.sf.orcc.df.Instance
import net.sf.orcc.df.Network
import net.sf.orcc.util.OrccUtil

class EmbeddedNetwork extends ExprAndTypePrinter {

	protected Network network

	new(Network network, Map<String, Object> options) {
		this.network = network
	}

	def printNetwork(String targetFolder) {
		val content = compileNetwork
		val file = new File(targetFolder + File::separator + network.simpleName + ".h")

		if (needToWriteFile(content, file)) {
			OrccUtil::printFile(content, file)
			return 0
		} else {
			return 1
		}
	}

	def printMain(String targetFolder) {
		val content = compileMain
		val file = new File(targetFolder + File::separator + "main.cpp")

		if (!file.exists) {
			OrccUtil::printFile(content, file)
			return 0
		} else {
			return 1
		}
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
	
		«FOR instance : network.children.filter(typeof(Instance))»
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
				inst_«(e.source as Instance).name»->port_«e.sourcePort.name» = fifo_«e.getAttribute("idNoBcast").objectValue»;
				inst_«(e.target as Instance).name»->port_«e.targetPort.name» = fifo_«e.getAttribute("idNoBcast").objectValue»;
			«ENDFOR»
	
			EStatus status = None;
			do{
		status = None;
		«FOR instance : network.children.filter(typeof(Instance))»
			inst_«instance.name»->schedule(status);
		«ENDFOR»
			}while(status != None);
	
		// Clean Actors
		«FOR instance : network.children.filter(typeof(Instance))»
		delete «instance.name»;
		«ENDFOR»
	
		// Clean FIFOs
		«FOR instance : network.children.filter(typeof(Instance))»
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
		«FOR instance : network.children.filter(typeof(Instance))»
			src/«instance.name».h
		«ENDFOR»
		)
	
		set(libraries EmbeddedCPP)
		
		
		set(libraries ${libraries} ${CMAKE_THREAD_LIBS_INIT})
		target_link_libraries(«network.simpleName» ${libraries})
	'''

	def printCMakeLists(String targetFolder) {
		val content = compileCmakeLists
		val file = new File(targetFolder + File::separator + "CMakeLists.txt")

		if (needToWriteFile(content, file)) {
			OrccUtil::printFile(content, file)
			return 0
		} else {
			return 1
		}
	}

}
