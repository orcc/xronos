/*
 * Copyright (c) 2013, Ecole Polytechnique Fédérale de Lausanne
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
package org.xronos.orcc.backend

import java.io.File
import java.util.HashMap
import java.util.Map
import net.sf.orcc.df.Actor
import net.sf.orcc.df.Entity
import net.sf.orcc.df.Network
import net.sf.orcc.df.Port
import net.sf.orcc.df.util.DfVisitor
import net.sf.orcc.graph.Vertex
import net.sf.orcc.util.OrccUtil

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
			val file = new File(path + File::separator + actor.simpleName+"_clock_controller" + ".v")
			OrccUtil::printFile(content, file)
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
			 clock_enable <= «FOR port: actor.outputs SEPARATOR " && "»«IF portFanout.get(port) > 1»«FOR idx : 0 ..< portFanout.get(port) SEPARATOR " && "»«port.name»_enable[«idx»]«ENDFOR»«ELSE»«port.name»_enable«ENDIF»«ENDFOR»;
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