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
import java.util.HashMap
import java.util.Map
import net.sf.orcc.backends.ir.BlockFor
import net.sf.orcc.df.Actor
import net.sf.orcc.df.Instance
import net.sf.orcc.df.Port
import net.sf.orcc.df.State
import net.sf.orcc.df.Transition
import net.sf.orcc.ir.Arg
import net.sf.orcc.ir.ArgByRef
import net.sf.orcc.ir.ArgByVal
import net.sf.orcc.ir.BlockBasic
import net.sf.orcc.ir.BlockIf
import net.sf.orcc.ir.BlockWhile
import net.sf.orcc.ir.InstAssign
import net.sf.orcc.ir.InstCall
import net.sf.orcc.ir.InstLoad
import net.sf.orcc.ir.InstReturn
import net.sf.orcc.ir.InstStore
import net.sf.orcc.ir.Param
import net.sf.orcc.ir.Procedure
import net.sf.orcc.ir.Var
import org.eclipse.emf.common.util.EMap
import net.sf.orcc.util.util.EcoreHelper

class InstancePrinter extends SystemCTemplate {

	private var Actor actor

	private var Instance instance

	private String name

	def setInstance(Instance instance) {
		this.instance = instance
		this.actor = instance.getActor()
		this.name = instance.simpleName
	}

	def setActor(Actor actor) {
		this.actor = actor
		this.name = actor.simpleName
	}

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
			// This file is generated automatically by Xronos HLS
			// ----------------------------------------------------------------------------
			// Xronos SystemC, Actor Generator
			// Actor: «this.name» 
			// Date: «dateFormat.format(date)»
			// ----------------------------------------------------------------------------
		'''
	}

	def getActorHeaderContent() '''
	«fileHeader»
	
	#ifndef SC_«this.name»_H
	#define SC_«this.name»_H
	
	#include "systemc.h"
	
	SC_MODULE(«this.name»){
	
		// --------------------------------------------------------------------------
		// -- Control Ports
		sc_in<bool>   clk;
		sc_in<bool>   reset;
		sc_in<bool>   start;
		sc_out<bool>  done;
		
		// --------------------------------------------------------------------------
		// -- Actor Input Ports
		«FOR port : actor.inputs»
			«getPortDeclaration("in", port)»
		«ENDFOR»
	
		// --------------------------------------------------------------------------
		// -- Actor Output Ports
		«FOR port : actor.outputs»
			«getPortDeclaration("out", port)»
		«ENDFOR»
		
		// --------------------------------------------------------------------------
		// -- Start / Done Actions signals
		«FOR action : actor.actions SEPARATOR "\n"»
			sc_signal<bool> start_«action.body.name»;
			sc_signal<bool> done_«action.body.name»;
		«ENDFOR»
	
		// -- Scheduler States
		enum state_t { // enumerate states
			«FOR state : actor.fsm.states SEPARATOR ", "»s_«state.label»«ENDFOR»«IF !actor.inputs.empty», s_READ«ENDIF»«IF !actor.
	outputs.empty», s_WRITE«ENDIF»
		};
		
		state_t state, old_state;
	
		// --------------------------------------------------------------------------
		// -- Constructor
		SC_CTOR(«this.name»)
		:clk("clk")
		,reset("reset")
		,start("start")
		,done("done")
		{
			// -- Actions Scheduler Registration
			SC_CTHREAD(scheduler, clk.pos());
			reset_signal_is(reset, true);
			
			// -- Actions Registration
			«FOR action : actor.actions SEPARATOR "\n"»
				SC_CTHREAD(«action.body.name», clk.pos());
				reset_signal_is(reset, true);
			«ENDFOR»
		}
		
		«IF !actor.procs.empty»
		// --------------------------------------------------------------------------
		// -- Procedure / Functions
		«FOR procedure : actor.procs»
				«declare(procedure)»
			«ENDFOR»
		«ENDIF»
	
		// --------------------------------------------------------------------------
		// -- Actions Body
		«FOR action : actor.actions SEPARATOR "\n"»
			«IF !action.body.blocks.empty»
				«declare(action.body)»
			«ENDIF»
		«ENDFOR»
	
		// --------------------------------------------------------------------------
		// -- Actions isSchedulable
		«FOR action : actor.actions SEPARATOR "\n"»
			«IF !action.body.blocks.empty»
				«declare(action.scheduler)»
			«ENDIF»
		«ENDFOR»
	
		// --------------------------------------------------------------------------
		// -- Action(s) Scheduler
		void scheduler();
	
	};
	
	#endif //SC_«this.name»_H
	'''
	
	
	def getActorSourceContent() 
	'''
		«fileHeader»
		#include "«this.name».h"
		
		
		// --------------------------------------------------------------------------
		// -- State Variable Declaration
		«FOR variable : actor.stateVars»
			«declare(variable)»
		«ENDFOR»
		
		«IF !actor.procs.empty»
		// --------------------------------------------------------------------------
		// -- Procedure / Functions
		«FOR procedure : actor.procs»
			«getProcedureContent(procedure)»
		«ENDFOR»
		«ENDIF»
		
		// --------------------------------------------------------------------------
		// -- Actions Body
		«FOR action : actor.actions SEPARATOR "\n"»
			«IF !action.body.blocks.empty»
				«getActionBodyContent(action.body)»
			«ENDIF»
		«ENDFOR»
		
		// --------------------------------------------------------------------------
		// -- Actions isSchedulable
		«FOR action : actor.actions SEPARATOR "\n"»
			«IF !action.body.blocks.empty»
			«getProcedureContent(action.scheduler)»
			«ENDIF»
		«ENDFOR»
		
		// --------------------------------------------------------------------------
		// -- Action(s) Scheduler
		«schedulerContent»
	'''
	
	def getPortDeclaration(String direction, Port port) '''
		sc_fifo_«direction»< «port.type.doSwitch» > «port.name»;
	'''

	def getStateVariableDeclarationContent(Var variable) '''
		«declare(variable)»
	'''

	def getActionBodyContent(Procedure procedure) '''
		«procedure.returnType.doSwitch» «this.name»::«procedure.name»(«procedure.parameters.join(", ")[declare]») {
			«FOR variable : procedure.locals»
				«variable.declare»;
			«ENDFOR»
			// -- Reset Done
			done_«procedure.name» = false; 
			wait();
			while(true){
				do { wait(); } while ( !start_«procedure.name».read() );
				«FOR block : procedure.blocks»
					«block.doSwitch»
				«ENDFOR»
		
				done_«procedure.name» = true;
				wait(); 
			}
		}
	'''

	def getProcedureContent(Procedure procedure) '''
		«procedure.returnType.doSwitch» «this.name»::«procedure.name»(«procedure.parameters.join(", ")[declare]») {
			«FOR variable : procedure.locals»
				«variable.declare»;
			«ENDFOR»
		
			«FOR block : procedure.blocks»
				«block.doSwitch»
			«ENDFOR»
		}
	'''

	def getSchedulerContent() '''
		void «this.name»::scheduler(){
			// -- Ports indexes
			«FOR port : actor.inputs»
				sc_uint<32> p_«port.name»_token_index = 0;
				sc_uint<32> p_«port.name»_token_index_read = 0;
				bool p_«port.name»_consume = false;
			«ENDFOR»
			«FOR port : actor.outputs»
				sc_uint<32> p_«port.name»_token_index = 0;
				sc_uint<32> p_«port.name»_token_index_write = 0;
				bool p_«port.name»_produce = false;
			«ENDFOR»
			
			// -- Action guards
			«FOR action : actor.actions»
				bool guard_«action.name»;
			«ENDFOR»
			
			done = false; 
			wait();
			
			state = s_«actor.fsm.initialState.label»;
			old_state = s_«actor.fsm.initialState.label»;
			
			// -- Wait For Start singal
			do { wait(); } while ( !start.read() );
			
			while(true){
				// -- Calculate all guards
				«FOR action : actor.actions»
					guard_«action.name» = «action.scheduler.name»();
				«ENDFOR»
		
				switch (state){
					«IF !actor.inputs.empty»
						case (s_READ):
							«FOR port : actor.inputs»
								if(p_«port.name»_consume){
									for(int i = 0; i < p_«port.name»_token_index_read; i++){
										p_«port.name»[i] = «port.name».read();
										p_«port.name»_token_index++;
										p_«port.name»_consume = false;
									}
								}
							«ENDFOR»
							state = old_state;
						break;
					«ENDIF»
					
					«IF !actor.outputs.empty»
						case (s_WRITE):
							«FOR port : actor.outputs»
								if(p_«port.name»_produce){
									for(int i = 0; i < p_«port.name»_token_index_write; i++){
										«port.name».write(p_«port.name»[i]);
										p_«port.name»_token_index++;
									}
									p_«port.name»_produce = false;
								}
							«ENDFOR»
							state = old_state;
						break;
					«ENDIF»
					
					«FOR state : actor.fsm.states SEPARATOR "\n"»
						«getStateContent(state)»
					«ENDFOR»
					
					default:
						state = s_«actor.fsm.initialState.label»;
					break;
				}
				
				wait();
			}
		
			done = true;
		}
	'''

	// -- Scheduler helper methods
	def getStateContent(State state){ 
		var Map<Port, Integer> maxPortTokens = new HashMap
		var Boolean actionsHaveInputPrts = false
		for(edge : state.outgoing){
			var action = (edge as Transition).action
			if(!action.inputPattern.ports.empty){
				var EMap<Port, Integer> inputNumTokens = action.inputPattern.numTokensMap
				for(port : inputNumTokens.keySet){
					if(maxPortTokens.containsKey(port)){
						var oldValue = maxPortTokens.get(port)
						if(oldValue < inputNumTokens.get(port)){
							maxPortTokens.put(port,inputNumTokens.get(port))
						}
					}else{
						maxPortTokens.put(port,inputNumTokens.get(port))
					}
				}
				actionsHaveInputPrts = true;
			}
		}
	'''
		case(s_«state.label»):
			«FOR edge : state.outgoing SEPARATOR " else"»
			«getTransitionContent(edge as Transition)»
			}«ENDFOR»«IF actionsHaveInputPrts» else {
				«FOR port: maxPortTokens.keySet»
					p_«port.name»_token_index_read = «maxPortTokens.get(port)»;
					p_«port.name»_consume = true;
				«ENDFOR»
				old_state = s_«state.label»;
				state = s_READ;
			}«ENDIF»
		break;
	'''
	}
	def getTransitionContent(Transition transition) {
		var action = transition.action
		var tState = transition.target
		var EMap<Port, Integer> inputNumTokens = action.inputPattern.numTokensMap
		var EMap<Port, Integer> outputNumTokens = action.outputPattern.numTokensMap
		'''
			if(guard_«action.name»«IF !inputNumTokens.empty» && «FOR port : inputNumTokens.keySet SEPARATOR " && "»p_«port.
				name»_token_index == «inputNumTokens.get(port)»«ENDFOR»«ENDIF»){
				// -- Start action : «action.name»
				start_«action.body.name» = true;
				do { wait(); } while ( !done_«action.body.name».read() );
				// -- Reset start
				start_«action.body.name» = false;
				«IF !inputNumTokens.empty»
					«FOR port : inputNumTokens.keySet»
							p_«port.name»_token_index = 0;
					«ENDFOR»
				«ENDIF»
				«IF outputNumTokens.empty»
					state = s_«tState.label»;
				«ELSE»
					«FOR port : outputNumTokens.keySet»
						p_«port.name»_token_index_write = «outputNumTokens.get(port)»;
						p_«port.name»_produce = true;
					«ENDFOR»
					old_state = s_«tState.label»;
					state = s_WRITE;
				«ENDIF»
		'''
	}

	// -- Ir Blocks
	override caseBlockBasic(BlockBasic block) '''
		«FOR instr : block.instructions»
			«instr.doSwitch»
		«ENDFOR»
	'''

	override caseBlockIf(BlockIf block) '''
		if («block.condition.doSwitch») {
			«FOR thenBlock : block.thenBlocks»
				«thenBlock.doSwitch»
			«ENDFOR»
		}«IF block.elseRequired» else {
			«FOR elseBlock : block.elseBlocks»
				«elseBlock.doSwitch»
			«ENDFOR»
				}
		«ENDIF»
	'''

	override caseBlockWhile(BlockWhile blockWhile) '''
		loop_«blockWhile.lineNumber»: while («blockWhile.condition.doSwitch») {
			«FOR block : blockWhile.blocks»
				«block.doSwitch»
			«ENDFOR»
		}
	'''

	override caseBlockFor(BlockFor blockFor) '''
		loop_«blockFor.lineNumber»: for («blockFor.init.join(", ")['''«toExpression»''']» ; «blockFor.condition.doSwitch» ; «blockFor.
			step.join(", ")['''«toExpression»''']») {
			«FOR contentBlock : blockFor.blocks»
				«contentBlock.doSwitch»
			«ENDFOR»
		}
	'''

	// -- Ir Instructions
	override caseInstAssign(InstAssign inst) '''
		«inst.target.variable.name» = «inst.value.doSwitch»;
	'''

	override caseInstCall(InstCall call) '''
		«IF call.print»
			#ifndef __SYNTHESIS__
				cout<< «call.arguments.printfArgs.join(", ")» <<endl;
			#endif
		«ELSE»
			«IF !call.hasAttribute("memberCall")»
				«IF call.target != null»«call.target.variable.name» = «ENDIF»«call.procedure.name»(«call.arguments.join(", ")[print]»);
			«ELSE»
				«call.arguments.get(0).print».«call.procedure.name»();
			«ENDIF»
		«ENDIF»
	'''

	override caseInstLoad(InstLoad load) {
		val target = load.target.variable
		val source = load.source.variable
		'''
			«target.name» = «source.name»«load.indexes.printArrayIndexes»;
		'''
	}

	override caseInstReturn(InstReturn ret){ 
		var procedure = EcoreHelper.getContainerOfType(ret, Procedure)
		var type = procedure.returnType
		'''
			«IF ret.value != null»
				return ( «type.doSwitch» ) («ret.value.doSwitch»);
			«ENDIF»
		'''
	}
	
	override caseInstStore(InstStore store) {
		val target = store.target.variable
		'''
			«target.name»«store.indexes.printArrayIndexes» = «store.value.doSwitch»;
		'''
	}

	// -- Helper Methods
	def private print(Arg arg) {
		if (arg.byRef) {
			"&" + (arg as ArgByRef).use.variable.name + (arg as ArgByRef).indexes.printArrayIndexes
		} else {
			(arg as ArgByVal).value.doSwitch
		}
	}

	// -- Declaration Methods
	def private declare(Param param) {
		val variable = param.variable
		'''«variable.type.doSwitch» «variable.name»«variable.type.dimensionsExpr.printArrayIndexes»'''
	}

	override declare(Var variable) {
		val const = if(!variable.assignable && variable.global) "const "
		val global = if(variable.global) "static "
		val type = variable.type
		val dims = variable.type.dimensionsExpr.printArrayIndexes
		val init = if(variable.initialized) " = " + variable.initialValue.doSwitch
		val end = if(variable.global) ";"
			
			'''«global»«const»«type.doSwitch» «variable.name»«dims»«init»«end»'''
	}
	
	def declareSource(Var variable) {
		val const = if(!variable.assignable && variable.global) "const "
		val type = variable.type
		val dims = variable.type.dimensionsExpr.printArrayIndexes
		val init = if(variable.initialized) " = " + variable.initialValue.doSwitch
		val end = if(variable.global) ";"
		if(variable.initialized){
			'''«const»«type.doSwitch» «variable.name»«dims»«init»«end»'''
		}
	}
	
	
	def declareNoInit(Var variable) {
		val const = if(!variable.assignable && variable.global) "const "
		val type = variable.type
		val dims = variable.type.dimensionsExpr.printArrayIndexes
		val end = if(variable.global) ";"

		'''«const»«type.doSwitch» «variable.name»«dims»«end»'''
	}
	
	
	
	def declare(Procedure procedure)'''
		«procedure.returnType.doSwitch» «procedure.name»(«procedure.parameters.join(", ")[declare]»);
	'''
	
}
