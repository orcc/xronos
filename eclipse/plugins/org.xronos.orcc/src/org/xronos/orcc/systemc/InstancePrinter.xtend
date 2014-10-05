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
import net.sf.orcc.backends.ir.BlockFor
import net.sf.orcc.df.Actor
import net.sf.orcc.df.Instance
import net.sf.orcc.df.Port
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

/**
 * SystemC Instance Printer
 * 
 * @author Endri Bezati
 */
class InstancePrinter extends SystemCTemplate {

	private var Instance instance

	private var Actor actor

	private var Procedure scheduler
	
	private String name

	def setInstance(Instance instance) {
		this.instance = instance

		this.actor = instance.getActor()
		
		this.name = instance.simpleName
	}
	
	def setActor(Actor actor){
		this.actor = actor
		this.name = actor.simpleName
	}

	// -- Get Content For each Top Level
	def getContent() '''
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
			// This file is generated automatically by Xronos HLS
			// ----------------------------------------------------------------------------
			// Xronos SystemC, Actor Generator
			// Actor: «this.name» 
			// Date: «dateFormat.format(date)»
			// ----------------------------------------------------------------------------
		'''
	}

	def getInstanceContent() '''
	#ifndef SC_«this.name»_H
	#define SC_«this.name»_H
	
	#include"systemc.h"
	
	SC_MODULE(«this.name»){
	
		// -- Control Ports
		sc_in<bool>   clock;
		sc_in<bool>   reset;
		sc_in<bool>   start;
		sc_out<bool>  done;

		// -- Actor Input Ports
		«FOR port : actor.inputs»
			«getPortDeclaration("in", port)»
		«ENDFOR»

		// -- Actor Output Ports
		«FOR port : actor.outputs»
			«getPortDeclaration("out", port)»
		«ENDFOR»

		// -- Start / Done Actions signals
		«FOR action: actor.actions SEPARATOR "\n"»
			sc_signal<bool> start_«action.name»;
			sc_signal<bool> done_«action.name»;
		«ENDFOR»

		// -- Constructor
		SC_CTOR(«this.name»)
			:clock("clock")
			,reset("reset")
			,start("start")
			,done("done")
		{
			// Actions Scheduler Registration
			SC_CTHREAD(scheduler, clock.pos());
			reset_signal_is(reset, true);
			
			// Actions Registration
			«FOR action: actor.actions SEPARATOR "\n"»
				SC_CTHREAD(«action.name», clock.pos());
				reset_signal_is(reset, true);
			«ENDFOR»
		}

		// -- State Variable Declaration
		«FOR variable : actor.stateVars»
			«getStateVariableDeclarationContent(variable)»
		«ENDFOR»

		// -- Procedure / Functions
		«FOR procedure : actor.procs»
			«getProcedureContent(procedure)»
		«ENDFOR»

		// -- Actions Body
		«FOR action : actor.actions»
			«IF !action.body.blocks.empty»
				«getActionBodyContent(action.body)»
			«ENDIF»
		«ENDFOR»

		// -- Action(s) Scheduler
		//getProcedureContent(scheduler)
	};

	#endif //SC_«this.name»_H
	'''

	def getPortDeclaration(String direction, Port port) '''
		sc_fifo_«direction»<«port.type.doSwitch»> «port.name»;
	'''

	def getStateVariableDeclarationContent(Var variable) '''
		«declare(variable)»
	'''

	def getActionBodyContent(Procedure procedure)'''
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
		«procedure.returnType.doSwitch» «procedure.name»(«procedure.parameters.join(", ")[declare]») {
			«FOR variable : procedure.locals»
				«variable.declare»;
			«ENDFOR»

			«FOR block : procedure.blocks»
				«block.doSwitch»
			«ENDFOR»
		}
	'''

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

	override caseInstReturn(InstReturn ret) '''
		«IF ret.value != null»
			return «ret.value.doSwitch»;
		«ENDIF»
	'''

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

}
