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
package org.xronos.systemc

import java.text.SimpleDateFormat
import java.util.ArrayList
import java.util.Arrays
import java.util.Date
import java.util.HashMap
import java.util.List
import java.util.Map
import net.sf.orcc.backends.ir.BlockFor
import net.sf.orcc.df.Actor
import net.sf.orcc.df.Connection
import net.sf.orcc.df.Entity
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
import net.sf.orcc.ir.ExprBinary
import net.sf.orcc.ir.InstAssign
import net.sf.orcc.ir.InstCall
import net.sf.orcc.ir.InstLoad
import net.sf.orcc.ir.InstReturn
import net.sf.orcc.ir.InstStore
import net.sf.orcc.ir.OpBinary
import net.sf.orcc.ir.Param
import net.sf.orcc.ir.Procedure
import net.sf.orcc.ir.Var
import net.sf.orcc.util.util.EcoreHelper
import org.eclipse.emf.common.util.EMap
import net.sf.orcc.ir.TypeList
import net.sf.orcc.ir.Type
import java.lang.reflect.Array
import net.sf.orcc.ir.util.ValueUtil
import java.math.BigInteger

class InstancePrinter extends SystemCTemplate {

	protected var Actor actor

	protected var Instance instance

	protected String name
	
	protected static Boolean actionAsProcess = false
	
	protected static boolean addScope = true
	
	
	private var Map<Port, List<String>> fanoutPortConenction

	def setInstance(Instance instance) {
		this.instance = instance
		this.actor = instance.getActor()
		this.name = instance.simpleName
	}

	def setActor(Actor actor) {
		this.actor = actor
		this.name = actor.simpleName
		getFanoutPortNames
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
	
		// -- Scheduler States
		enum state_t { // enumerate states
			«FOR state : actor.fsm.states SEPARATOR ", "»s_«state.label»«ENDFOR»«IF !actor.inputs.empty», s_READ«ENDIF»«IF !actor.
	outputs.empty», s_WRITE«ENDIF»
		};
		
		state_t state, old_state;
		
		«FOR stateVar: actor.stateVars»
			«declareStateVar(stateVar)»
		«ENDFOR»
	
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
			«IF actionAsProcess»
			
			// -- Actions Registration
				«FOR action : actor.actions SEPARATOR "\n"»
					SC_CTHREAD(«action.body.name», clk.pos());
					reset_signal_is(reset, true);
				«ENDFOR»
			«ENDIF»
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
		// -- Initialize Members
		void intializeMembers();
		
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
			«IF actionAsProcess»
				«IF !action.body.blocks.empty»
					«getActionBodyContentAsProcess(action.body)»
				«ENDIF»
			«ELSE»
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
		// -- Initialize Members
		«getMemberIntializaion»
		
		// --------------------------------------------------------------------------
		// -- Action(s) Scheduler
		«schedulerContent»
	'''
	
	def getPortDeclaration(String direction, Port port) '''
		«IF fanoutPortConenction.containsKey(port)»
			«FOR name: fanoutPortConenction.get(port)»
				sc_fifo_«direction»< «port.type.doSwitch» > «name»;
			«ENDFOR»
		«ELSE»
			sc_fifo_«direction»< «port.type.doSwitch» > «port.name»;
		«ENDIF»
	'''

	def getStateVariableDeclarationContent(Var variable) '''
		«declare(variable)»
	'''

	def getActionBodyContentAsProcess(Procedure procedure) '''
		«procedure.returnType.doSwitch» «IF addScope»«this.name»::«ENDIF»«procedure.name»(«procedure.parameters.join(", ")[declare]») {
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
	
		def getActionBodyContent(Procedure procedure) '''
		«procedure.returnType.doSwitch» «IF addScope»«this.name»::«ENDIF»«procedure.name»(«procedure.parameters.join(", ")[declare]») {
		#pragma HLS inline off
			«FOR variable : procedure.locals»
				«variable.declare»;
			«ENDFOR»
			«FOR block : procedure.blocks»
				«block.doSwitch»
			«ENDFOR»
		}
	'''


	def getProcedureContent(Procedure procedure) '''
		«procedure.returnType.doSwitch» «IF addScope»«this.name»::«ENDIF»«procedure.name»(«procedure.parameters.join(", ")[declare]») {
			«FOR variable : procedure.locals»
				«variable.declare»;
			«ENDFOR»
		
			«FOR block : procedure.blocks»
				«block.doSwitch»
			«ENDFOR»
		}
	'''

	def getMemberIntializaion(){
		'''
		void «IF addScope»«this.name»::«ENDIF»intializeMembers(){
			«FOR variable : actor.stateVars»
				«IF variable.initialized»
					«IF variable.type.list»
					«getMemberInitializationArray(variable)»
					«ELSE»
						«variable.name» = «variable.initialValue.doSwitch»; 
					«ENDIF»
				«ENDIF»
			«ENDFOR»
		}
		'''
	}
	
	def getMemberInitializationArray(Var v){
		var List<String> array = new ArrayList<String>
		var TypeList typeList = (v.type as TypeList);
		var Type type = typeList.getInnermostType();
		var List<Integer> listDimension = new ArrayList<Integer>(
					typeList.getDimensions());
		var Object obj = v.getValue();
		var String varName = v.name
		if(addScope){
			varName = this.name + "::" + v.name 
		}
		makeArray(varName, "", array,obj, listDimension,type)
		'''
			«FOR value: array»
				«value»
			«ENDFOR»
		'''
	}
	
	@SuppressWarnings("Object")
	def makeArray(String name, String prefix, List<String> array, Object obj, List<Integer> dimension, Type type){
		if(dimension.size() > 1){
			
			var List<Integer> newListDimension = new ArrayList<Integer>(dimension);
			var Integer firstDim = dimension.get(0);
			newListDimension.remove(0);
			for(int i : 0 ..< firstDim ){
				var String newPrefix = prefix + "[" + i + "]";
				makeArray(name, newPrefix, array, Array.get(obj, i), newListDimension,type);
			}
		}else{
			if (dimension.get(0).equals(1)) {
				var BigInteger value = BigInteger.valueOf(0);
				if (type.isBool()) {
					if ( ValueUtil.get(type, obj, 0) as Boolean){
						value = BigInteger.valueOf(1);
					}else{
						value =  BigInteger.valueOf(0);
					}
				}
				var String valueString = value.toString();
				array.add(valueString);
			}else{
				for(int i : 0 ..< dimension.get(0) ){
					var BigInteger value = BigInteger.valueOf(0);
					if (type.isBool()) {
						if ( ValueUtil.get(type, obj, 0) as Boolean){
							value = BigInteger.valueOf(1);
						}else{
							value =  BigInteger.valueOf(0);
						}
					}else{
						value = ValueUtil.get(type, obj, i) as BigInteger;
					}
					var String valueString = name + prefix+ "["+ i+ "]" + " = " +value.toString() +";";
					array.add(valueString);	
				}
			}
		}
		
	}

	

	def getSchedulerContent() '''
		void «IF addScope»«this.name»::«ENDIF»scheduler(){
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
			
			// -- Initialize Members
			intializeMembers();
			 
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
								if(p_«port.name»_consume && ( «port.name».num_available() > 0 ) ){
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
										«FOR name : fanoutPortConenction.get(port)»
											«name».write(p_«port.name»[i]);
										«ENDFOR»
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
					if( p_«port.name»_token_index < «maxPortTokens.get(port)» ){
						p_«port.name»_token_index_read = «maxPortTokens.get(port)» - p_«port.name»_token_index;
						p_«port.name»_consume = true;
					}
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
							
				«IF actionAsProcess»
					do { wait(); } while ( !done_«action.body.name».read() );
				«ELSE»
					«action.body.name»();
				«ENDIF»
				
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

	override caseBlockWhile(BlockWhile blockWhile){ 
		val Integer loopIndex = blockWhile.getAttribute("loopLabel").objectValue as Integer 
	'''
		loop_«loopIndex»: while («blockWhile.condition.doSwitch») {
			«FOR block : blockWhile.blocks»
				«block.doSwitch»
			«ENDFOR»
		}
	'''
	
	}

	override caseBlockFor(BlockFor blockFor){ 
		val Integer loopIndex = blockFor.getAttribute("loopLabel").objectValue as Integer
	'''
		loop_«loopIndex»: for («blockFor.init.join(", ")['''«toExpression»''']» ; «blockFor.condition.doSwitch» ; «blockFor.
			step.join(", ")['''«toExpression»''']») {
			«FOR contentBlock : blockFor.blocks»
				«contentBlock.doSwitch»
			«ENDFOR»
		}
	'''
	}
	// -- Ir Instructions
	override caseInstAssign(InstAssign inst) '''
		«inst.target.variable.name» = «inst.value.doSwitch»;
	'''

	override caseInstCall(InstCall call) {
		var Map<Param,Arg> paramArg = new HashMap
		var int i = 0
		for (param : call.procedure.parameters){
			paramArg.put(param,call.arguments.get(i))
			i++
		}
	'''
		«IF call.print»
			#ifndef __SYNTHESIS__
				std::cout << «FOR arg : call.arguments SEPARATOR " << "»«arg.coutArg»«ENDFOR»;
			#endif
		«ELSE»
			«IF !call.hasAttribute("memberCall")»
				«IF call.target != null»«call.target.variable.name» = «ENDIF»«call.procedure.name»(«FOR param: paramArg.keySet SEPARATOR ", "»«printWithCast(param,paramArg.get(param))»«ENDFOR»);
			«ELSE»
				«call.arguments.get(0).print».«call.procedure.name»();
			«ENDIF»
		«ENDIF»
	'''
	}
	override caseInstLoad(InstLoad load) {
		val target = load.target.variable
		val source = load.source.variable
		'''
			«target.name» = «IF addScope»«this.name»::«ENDIF»«source.name»«load.indexes.printArrayIndexes»;
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
			«IF addScope»«this.name»::«ENDIF»«target.name»«store.indexes.printArrayIndexes» = «store.value.doSwitch»;
		'''
	}
	
	
	
	override caseExprBinary(ExprBinary expr) {
		val op = expr.op
		var nextPrec = if (op == OpBinary::SHIFT_LEFT || op == OpBinary::SHIFT_RIGHT) {

				// special case, for shifts always put parentheses because compilers
				// often issue warnings
				Integer::MIN_VALUE;
			} else {
				op.precedence;
			}
		val nextOpBin = if (expr.e2 instanceof ExprBinary) { true } else {false} 
		val resultingExpr = '''«expr.e1.printExpr(nextPrec, 0)» «op.stringRepresentation» «IF nextOpBin»(«expr.e2.type.doSwitch») («ENDIF»«expr.e2.printExpr(nextPrec, 1)»«IF nextOpBin» )«ENDIF»'''

		if (op.needsParentheses(precedence, branch)) {
			'''(«resultingExpr»)'''
		} else {
			resultingExpr
		}
	}
	

	// -- Helper Methods
	def private print(Arg arg) {
		if (arg.byRef) {
			"&" + (arg as ArgByRef).use.variable.name + (arg as ArgByRef).indexes.printArrayIndexes
		} else {
			(arg as ArgByVal).value.doSwitch
		}
	}
	
	def printWithCast(Param param, Arg arg){
		val variable = param.variable	
		if (arg.byRef) {
			'''(«variable.type.doSwitch») &«(arg as ArgByRef).use.variable.name»«(arg as ArgByRef).indexes.printArrayIndexes»'''
		} else {
			'''(«variable.type.doSwitch») «(arg as ArgByVal).value.doSwitch»'''
		}
	}
	def dispatch coutArg(ArgByRef arg) {
		'''&«arg.use.variable.doSwitch»«FOR index : arg.indexes»[«index.doSwitch»]«ENDFOR»'''
	}
	
	def dispatch coutArg(ArgByVal arg) {
		arg.value.doSwitch
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
	
	def declareStateVar(Var variable){
		val type = variable.type
		val dims = variable.type.dimensionsExpr.printArrayIndexes
		val end = if(variable.global) ";"
		'''«type.doSwitch» «variable.name»«dims»«end»'''
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
	
	def getFanoutPortNames(){
		var Map<Port, List<Connection>> portConnection = actor.getAdapter((typeof(Entity))).getOutgoingPortMap()
		fanoutPortConenction = new HashMap
		for(port : actor.outputs){
			if (portConnection.get(port).size > 1 ){
				var List<String> portNames = new ArrayList
				for(connection: portConnection.get(port)){
					if(connection.target instanceof Actor){
						portNames.add(port.name+"_f_"+ (connection.target as Actor).name);
					}else{
						portNames.add(port.name+"_f_"+ (connection.target as Port).name);
					}					
				}
				fanoutPortConenction.put(port,portNames)
			}else{
				fanoutPortConenction.put(port, Arrays.asList(port.name))
			}
		}
	}
	
}
