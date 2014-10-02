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
import net.sf.orcc.df.Port
import net.sf.orcc.ir.Procedure

/**
 * SystemC Instance Printer
 * 
 * @author Endri Bezati
 */
class InstancePrinter extends SystemCTemplate {
	
	private var Instance instance
	
	private var Actor actor
	
	private var Procedure scheduler
	
	def setInstance(Instance instance){
		this.instance = instance
		
		this.actor = instance.getActor()	
	}
	
	def getContent()'''
		«header»
		«instanceContent»
	'''
	
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
			// Xronos SystemC, Instance Generator
			// Top level Network: «instance.simpleName» 
			// Date: «dateFormat.format(date)»
			// ----------------------------------------------------------------------------
		'''
	}
	
	
	def getInstanceContent()'''
	#ifndef SC_«instance.name»_H
	#define SC_«instance.name»_H
	
	SC_MODULE(«instance.name»){
	
		// -- Control Ports
		sc_in <bool>  clock;
		sc_in <bool>  reset;
		sc_in <bool>  start;
		sc_out<bool>  done;
	
		// -- Instance Input Ports
		«FOR port: actor.inputs»
			«getPortDeclaration("in", port)»
		«ENDFOR»
	
		// -- Instance Output Ports
		«FOR port: actor.outputs»
			«getPortDeclaration("out", port)»
		«ENDFOR»
		
		// -- Constructor
		SC_CTOR(«instance.name»)
			:clock("clock")
			,reset("reset")
			,start("start")
			,done("done")
		{	
			// Scheduler Registration
			SC_CTHREAD(scheduler, clock.pos());
			reset_signal_is(reset, true);
		}
	
		// -- State Variable Declaration
		«stateVariableContent»
		
		// -- Procedure / Functions
		
		// -- Actions Body
		«FOR action: actor.actions»
			«getProcedureContent(action.body)»
		«ENDFOR»
	
		// -- Scheduler
		«schedulerContent»
	}
	
	#endif //SC_«instance.name»_H
	'''
	def getPortDeclaration(String direction, Port port)'''
		sc_fifo_«direction»<«port.type»> «port.name»;
	'''	
	
	def getStateVariableContent()'''
	'''

	def getProcedureContent(Procedure procedure)'''
	'''
	
	def getSchedulerContent()'''
	'''


}