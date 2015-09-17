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
import java.util.Map
import net.sf.orcc.df.Action
import net.sf.orcc.df.Actor
import net.sf.orcc.df.FSM
import net.sf.orcc.df.Port
import net.sf.orcc.df.State
import net.sf.orcc.df.Transition
import net.sf.orcc.ir.ArgByRef
import net.sf.orcc.ir.ArgByVal
import net.sf.orcc.ir.BlockBasic
import net.sf.orcc.ir.BlockIf
import net.sf.orcc.ir.BlockWhile
import net.sf.orcc.ir.Expression
import net.sf.orcc.ir.InstAssign
import net.sf.orcc.ir.InstCall
import net.sf.orcc.ir.InstLoad
import net.sf.orcc.ir.InstReturn
import net.sf.orcc.ir.InstStore
import net.sf.orcc.ir.Procedure
import net.sf.orcc.ir.Type
import net.sf.orcc.ir.TypeList
import net.sf.orcc.ir.Var
import net.sf.orcc.util.FilesManager
import java.util.HashSet

class EmbeddedActor extends ExprAndTypePrinter {
	
	protected Actor actor
	
	Boolean v7Profiling = false
	
	new (Actor actor, Map<String, Object> options) {
		this.actor = actor
		if(options.containsKey("org.xronos.orcc.ARMv7Profiling")){
			v7Profiling = options.get("org.xronos.orcc.ARMv7Profiling") as Boolean
		}
		
	}
	
	def print(String targetFolder) {
		val content = compileInstance
		FilesManager.writeFile(content, targetFolder, actor.name + ".h")
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
			// This file is generated automatically by Xronos C++ 
			// ----------------------------------------------------------------------------
			// Xronos C++, Actor Generator
			// Actor: «actor.simpleName» 
			// Date: «dateFormat.format(date)»
			// ----------------------------------------------------------------------------
		'''
	}
	
	
	def compileInstance()  {
		'''
		«getFileHeader»
		#ifndef __«actor.name.toUpperCase»_H__
		#define __«actor.name.toUpperCase»_H__
		
		#include <iostream>
		#include "actor.h"
		#include "fifo.h"
		#include "fifo.h"
		«IF v7Profiling»
			#include "v7_pmu.h"
		«ENDIF»
		
		«IF actor.hasAttribute("actor_shared_variables")»
			// -- Shared Variables
			«FOR v : actor.getAttribute("actor_shared_variables").objectValue as HashSet<Var>»
				extern «v.type.doSwitch» «v.name»«FOR dim : v.type.dimensions»[«dim»]«ENDFOR»;
			«ENDFOR»
		«ENDIF»
		
		«FOR proc : actor.procs.filter(p | p.native && !"print".equals(p.name))»
			«proc.compileNativeProc»
		«ENDFOR»
		
		class «actor.name»: public Actor
		{
		public:
			«IF v7Profiling»
				// ARM v7 Profiling
				unsigned int v7_cycles[«actor.actions.size»];
				unsigned int v7_overflows[«actor.actions.size»];
				unsigned int v7_executions[«actor.actions.size»];
			«ENDIF»
			«actor.name»(«FOR variable : actor.parameters SEPARATOR ", "»«variable.type.doSwitch» «variable.name»«FOR dim : variable.type.dimensions»[«dim»]«ENDFOR»«ENDFOR»)
				«FOR variable : actor.parameters BEFORE ":" SEPARATOR "\n,"»  «variable.name»(«variable.name»)«ENDFOR»
			{
				«FOR v : actor.stateVars.filter(v|v.initialValue != null)»«compileArg(v.type, v.name, v.initialValue)»«ENDFOR»
				«IF actor.fsm != null»state_ = state_«actor.fsm.initialState.name»;«ENDIF»
				
				«IF v7Profiling»
					«FOR i : 0 .. actor.actions.size-1»
						v7_executions[«i»] = 0;
					«ENDFOR»
					«FOR i : 0 .. actor.actions.size-1»
						v7_cycles[«i»] = 0;
					«ENDFOR»
				«ENDIF»
			}
		
			«getPorts»

			«actor.procs.filter(p | !p.native).map[compileProcedure].join»
			«actor.initializes.map[compileAction].join»
		
			
			void initialize()
			{
				«FOR action : actor.initializes»
					«action.name»();
				«ENDFOR»
			}
		
			«actor.actions.map[compileAction].join»
		
			«actor.compileScheduler»
			
			
			«IF v7Profiling»
			void printActorProfiling(){
				std::cout << "Profiling for Actor: «actor.simpleName» " << std::endl;
				std::cout << "Actions : "<< std::endl;
				«FOR action: actor.actions»
					std::cout << "\t- «action.name», Mean Cycles:" <<  v7_cycles[«actor.actions.indexOf(action)»] / v7_executions[«actor.actions.indexOf(action)»] <<", Executions: " << v7_executions[«actor.actions.indexOf(action)»] << std::endl; 
				«ENDFOR»
			}
			
			void printActorProfilingCSV(){
				«FOR action: actor.actions»
					std::cout << "«actor.simpleName»; «action.name»; " << v7_cycles[«actor.actions.indexOf(action)»] / v7_executions[«actor.actions.indexOf(action)»] << "; " << v7_executions[«actor.actions.indexOf(action)»] << std::endl;
				«ENDFOR»
			}
			
			
			
			«ENDIF»
		
			void getState()
			{
				«IF actor.hasFsm»
					switch(state_) {
					«FOR state : actor.fsm.states»
						case state_«state.name»:
							std::cout << "Actor :«actor.simpleName», Last State: «state.name»" << std::endl;
							break;
					«ENDFOR»
					}
				«ELSE»
					std::cout << "Actor :«actor.simpleName» has no state" << std::endl;
				«ENDIF»
			}
			
		private:	
			«FOR param : actor.parameters SEPARATOR "\n"»«param.varDecl»;«ENDFOR»
			«FOR variable: actor.stateVars SEPARATOR "\n"»«IF !variable.hasAttribute("shared")»«variable.varDecl»;«ENDIF»«ENDFOR»
			«FOR port : actor.inputs SEPARATOR "\n"»int status_«port.name»_;«ENDFOR»
			«FOR port : actor.outputs SEPARATOR "\n"»int status_«port.name»_;«ENDFOR»
			«IF actor.fsm != null»
			enum states
			{
				«FOR state : actor.fsm.states SEPARATOR ","»
					state_«state.name»
				«ENDFOR»
			} state_;
			«ENDIF»
		};
		#endif
		'''
	}
	
	
	def getPorts(){
		val connectedOutput = [ Port port | actor.outgoingPortMap.get(port) != null ]
		'''
			«FOR port : actor.inputs»
				«IF actor.incomingPortMap.get(port) != null»
					«port.compilePort(actor.incomingPortMap.get(port).getAttribute("nbReaders").objectValue)»
				«ENDIF»
			«ENDFOR»			
			
			«FOR port : actor.outputs.filter(connectedOutput)»
				«IF actor.outgoingPortMap.get(port) != null»
					«port.compilePort(actor.outgoingPortMap.get(port).size)»
				«ENDIF»
			«ENDFOR»
		'''		
	}
	
	def dispatch compileArg(Type type, String name, Expression expr) '''
		«name» = «expr.doSwitch»;
	'''

	def dispatch compileArg(TypeList type, String name, Expression expr) '''
		// C++11 allows array initializer in initialization list but not yet implemented in VS10... use that instead.
		«type.doSwitch» tmp_«name»«FOR dim:type.dimensions»[«dim»]«ENDFOR» = «expr.doSwitch»;
		memcpy(«name», tmp_«name», «FOR dim : type.dimensions SEPARATOR "*"»«dim»«ENDFOR»*sizeof(«type.innermostType.doSwitch»));
	'''
	
	override caseProcedure(Procedure procedure) '''
		«FOR variable : procedure.locals SEPARATOR "\n"»«variable.varDeclWithInit»;«ENDFOR»
		«FOR node : procedure.blocks»«node.doSwitch»«ENDFOR»
	'''

	override caseBlockBasic(BlockBasic node) '''
		«FOR inst : node.instructions»«inst.doSwitch»«ENDFOR»
	'''

	override caseBlockIf(BlockIf node) '''
		if(«node.condition.doSwitch»)
		{
			«FOR then : node.thenBlocks»«then.doSwitch»«ENDFOR»	
		} 
		«IF !node.elseBlocks.empty»
		else
		{
			«FOR els : node.elseBlocks»«els.doSwitch»«ENDFOR»
		}«ENDIF»
		«node.joinBlock.doSwitch»
	'''

	override caseBlockWhile(BlockWhile node) '''
		while(«node.condition.doSwitch»)
		{
			«FOR whil : node.blocks»«whil.doSwitch»«ENDFOR»
		}
		«node.joinBlock.doSwitch»
	'''
	
	override caseInstAssign(InstAssign inst) '''
		«inst.target.variable.name» = «inst.value.doSwitch»;
	'''
	override caseInstCall(InstCall inst) {
	if(inst.print) {
	'''
		std::cout << «FOR arg : inst.arguments SEPARATOR " << "»(«arg.compileArg»)«ENDFOR»;
	'''
	} else {
	'''
		«IF inst.target!=null»«inst.target.variable.name» = «ENDIF»«inst.procedure.name»(«FOR arg : inst.getArguments SEPARATOR ", "»«arg.compileArg»«ENDFOR»);
	'''
	}
}
	override caseInstLoad(InstLoad inst) '''
		«inst.target.variable.name» = «inst.source.variable.name»«FOR index : inst.indexes»[«index.doSwitch»]«ENDFOR»;
	'''

	override caseInstReturn(InstReturn inst) '''
		«IF inst.value != null»return «inst.value.doSwitch»;«ENDIF»
	'''

	override caseInstStore(InstStore inst) '''
		«inst.target.variable.name»«FOR index : inst.indexes»[«index.doSwitch»]«ENDFOR» = «inst.value.doSwitch»;
	'''
	
	def compilePort(Port port, Object nbReaders) '''
		Fifo<«port.type.doSwitch», «nbReaders»>* port_«port.name»;
	'''

	def compileNativeProc(Procedure proc) '''
		extern «proc.returnType.doSwitch» «proc.name» («FOR param : proc.parameters SEPARATOR ", "»«param.variable.varDecl»«ENDFOR»);
	'''

	def compileProcedure(Procedure proc) '''
		«proc.returnType.doSwitch» «proc.name» («FOR param : proc.parameters SEPARATOR ", "»«param.variable.varDecl»«ENDFOR»)
		{
			«proc.doSwitch»
		}
	'''
	
	def compileAction(Action action) '''
		«action.scheduler.returnType.doSwitch» «action.scheduler.name» ()
		{
			«FOR e : action.peekPattern.numTokensMap»
				«e.key.type.doSwitch»* «e.key.name» = port_«e.key.name»->read_address(«actor.incomingPortMap.get(e.key).getAttribute("fifoId").objectValue»«IF e.value > 1», «e.value»«ENDIF»);
			«ENDFOR»
			«action.scheduler.doSwitch»
		}

		«action.body.returnType.doSwitch» «action.body.name» ()
		{
			«FOR e : action.inputPattern.numTokensMap»
				«IF actor.incomingPortMap.get(e.key) != null»
					«e.key.type.doSwitch»* «e.key.name» = port_«e.key.name»->read_address(«actor.incomingPortMap.get(e.key).getAttribute("fifoId").objectValue»«IF e.value > 1», «e.value»«ENDIF»);
				«ELSE»
					«e.key.type.doSwitch» «e.key.name»[«e.value»];
				«ENDIF»
			«ENDFOR»
			«FOR port : action.outputPattern.ports»
				«IF actor.outgoingPortMap.get(port) != null»
					«port.type.doSwitch»* «port.name» = port_«port.name»->write_address();
				«ELSE»
					«port.type.doSwitch» «port.name»«port.type»«FOR dim:port.type.dimensions»[«dim»]«ENDFOR»;
				«ENDIF»
			«ENDFOR»
			
			«action.body.doSwitch»
			//std::cout<<"«actor.simpleName»:«action.body.name»"<<std::endl;
			«FOR e : action.inputPattern.numTokensMap»
				«IF actor.incomingPortMap.get(e.key) != null»
					port_«e.key.name»->read_advance(«actor.incomingPortMap.get(e.key).getAttribute("fifoId").objectValue»«IF e.value > 1», «e.value»«ENDIF»);
					status_«e.key.name»_ -= «e.value»;
				«ENDIF»
			«ENDFOR»
			«FOR e : action.outputPattern.numTokensMap»
				«IF actor.outgoingPortMap.get(e.key) != null»
					port_«e.key.name»->write_advance(«IF e.value > 1»«e.value»«ENDIF»);
					status_«e.key.name»_ -= «e.value»;
				«ENDIF»
			«ENDFOR»
		}
		
	'''
	
	def private varDecl(Var v) {
		'''«v.type.doSwitch» «v.name»«FOR dim : v.type.dimensions»[«dim»]«ENDFOR»'''
	}
	
	def private varDeclWithInit(Var v) {
		'''«v.type.doSwitch» «v.name»«FOR dim : v.type.dimensions»[«dim»]«ENDFOR»«IF v.initialValue != null» = «v.initialValue.doSwitch»«ENDIF»'''
	}
	
	def dispatch compileArg(ArgByRef arg) {
		'''&«arg.use.variable.doSwitch»«FOR index : arg.indexes»[«index.doSwitch»]«ENDFOR»'''
	}
	
	def dispatch compileArg(ArgByVal arg) {
		arg.value.doSwitch
	}
	
	def compileScheduler(Actor actor) '''	
		void action_selection(EStatus& status)
		{	
			bool res = true;
			while (res) {
				«IF actor.fsm!=null»
				res = false;
				«FOR action : actor.actionsOutsideFsm BEFORE "if" SEPARATOR "\nelse if"»«action.compileScheduler(null)»«ENDFOR»
				if(!res) {
					switch(state_) {
						«actor.fsm.compilerScheduler»
					}
				}
				«ELSE»
				res = false;
				«FOR action : actor.actions BEFORE "if" SEPARATOR "\nelse if"»«action.compileScheduler(null)»«ENDFOR»
				«ENDIF»
			}
		}

	'''
	
	def compileScheduler(Action action, State state) '''
		(«FOR e : action.inputPattern.numTokensMap»«IF actor.incomingPortMap.get(e.key) != null»port_«e.key.name»->count(«actor.incomingPortMap.get(e.key).getAttribute("fifoId").objectValue») >= «e.value» && «ENDIF»«ENDFOR»«action.scheduler.name»())
		{
			«IF !action.outputPattern.empty»
			if(«FOR e : action.outputPattern.numTokensMap SEPARATOR " &&" »«IF actor.outgoingPortMap.get(e.key) != null»port_«e.key.name»->rooms() >= «e.value» «ENDIF»«ENDFOR») {
				«IF v7Profiling»
					reset_ccnt();                   // Reset the CCNT (cycle counter)
					reset_pmn();                    // Reset the configurable counters
					«action.body.name»();
					v7_cycles[«actor.actions.indexOf(action)»] += read_ccnt();     // Read CCNT
					v7_overflows[«actor.actions.indexOf(action)»] = read_flags(); // Check for overflow flag
					v7_executions[«actor.actions.indexOf(action)»]++;
				«ELSE»
					«action.body.name»();
				«ENDIF»
				res = true;
				status = hasExecuted;
				«IF state != null»state_ = state_«state.name»;«ENDIF»
			}
			«ELSE»
			«IF v7Profiling»
					reset_ccnt();                   // Reset the CCNT (cycle counter)
					reset_pmn();                    // Reset the configurable counters
					«action.body.name»();
					v7_cycles[«actor.actions.indexOf(action)»] += read_ccnt();     // Read CCNT
					v7_overflows[«actor.actions.indexOf(action)»] = read_flags(); // Check for overflow flag
					v7_executions[«actor.actions.indexOf(action)»]++;
				«ELSE»
					«action.body.name»();
			«ENDIF»
			res = true;
			status = hasExecuted;
			«IF state != null»state_ = state_«state.name»;«ENDIF»
			«ENDIF»
		}'''

	def compilerScheduler(FSM fsm) '''
		«FOR state : fsm.states»
		case state_«state.name»:
			«FOR edge : state.outgoing BEFORE "if" SEPARATOR "\nelse if"»«(edge as Transition).action.compileScheduler(edge.target as State)»«ENDFOR»
			break;
		«ENDFOR»
	'''

}
