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
 
package org.xronos.orcc.backend

import java.util.HashMap
import java.util.Map
import net.sf.orcc.df.Actor
import net.sf.orcc.df.Entity
import net.sf.orcc.df.Network
import net.sf.orcc.df.Port
import net.sf.orcc.df.util.DfVisitor
import net.sf.orcc.graph.Vertex
import net.sf.orcc.util.FilesManager

/**
 * A simple clock enable circuit 
 * @author Endri Bezati
 */
class ClockEnabler extends DfVisitor<CharSequence> {
	
	var String path;
	
	new(String path){
		this.path = path;
	}
	
	override caseNetwork(Network network){
		for (Vertex vertex : network.getVertices()) {
			doSwitch(vertex);
		}
		'''
		'''
	}
	
	override caseActor(Actor actor) {
		if(actor.outputs.size > 0 ){
			val content = printActorClockEnabler(actor)
			FilesManager.writeFile(content, path, actor.simpleName+"_clock_controller" + ".v")
		}
		'''
		'''
	}
	
	def printActorClockEnabler(Actor actor){
		var Map<Port,Integer> portFanout = new HashMap<Port,Integer>();
		for(Port port: actor.outputs){
			portFanout.put(port,actor.getAdapter((typeof(Entity))).getOutgoingPortMap().get(port).size);
		}
		
		'''
		// __  ___ __ ___  _ __   ___  ___ 
		// \ \/ / '__/ _ \| '_ \ / _ \/ __|
		//  >  <| | | (_) | | | | (_) \__ \
		// /_/\_\_|  \___/|_| |_|\___/|___/
		// 
		
		`timescale 1ns/1ps
		
		module «actor.simpleName»_clock_controller(«actorsOutput(actor,"almost_full")», «actorsOutput(actor,"full")», en, clk, reset, clk_out);
		
		input en;
		input clk;
		input reset;
		
		«FOR port: actor.outputs SEPARATOR "\n"»
			input«IF portFanout.get(port) > 1»[«portFanout.get(port)-1»:0]«ENDIF» «port.name»_almost_full;
			input«IF portFanout.get(port) > 1»[«portFanout.get(port)-1»:0]«ENDIF» «port.name»_full;
		«ENDFOR»


		output clk_out;

		«FOR port: actor.outputs SEPARATOR "\n"»
			wire«IF portFanout.get(port) > 1»[«portFanout.get(port)-1»:0]«ENDIF» «port.name»_enable;
		«ENDFOR»

		reg clock_enable;
		
		wire buf_enable;
	
		«FOR port: actor.outputs SEPARATOR " \n "»
			«IF portFanout.get(port) > 1»
				«FOR idx : 0 ..< portFanout.get(port) SEPARATOR " \n "»
					controller c_«port.name»_«idx»(.almost_full(«port.name»_almost_full[«idx»]), .full(«port.name»_full[«idx»]), .enable(«port.name»_enable[«idx»]), .clk(clk), .reset(reset));
				«ENDFOR»
			«ELSE»
				controller c_«port.name» (.almost_full(«port.name»_almost_full),.full(«port.name»_full),.enable(«port.name»_enable), .clk(clk), .reset(reset));
			«ENDIF»
		«ENDFOR»

		always @(posedge clk)
		begin
			 clock_enable <= «FOR port: actor.outputs SEPARATOR " | "»«IF portFanout.get(port) > 1»(«FOR idx : 0 ..< portFanout.get(port) SEPARATOR " & "»«port.name»_enable[«idx»]«ENDFOR»)«ELSE»«port.name»_enable«ENDIF»«ENDFOR»;
		end 

		assign buf_enable = en ? clock_enable : 1;
		BUFGCE clock_enabling (.I(clk), .CE(buf_enable), .O(clk_out));

		endmodule
		'''
	}
	
	
	def actorsOutput(Actor actor, String suffix){
		'''«FOR port : actor.outputs SEPARATOR ","»«port.name»_«suffix»«ENDFOR»'''
	}
	
}