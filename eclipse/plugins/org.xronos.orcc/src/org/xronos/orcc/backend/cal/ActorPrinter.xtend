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
 
package org.xronos.orcc.backend.cal

import net.sf.orcc.backends.CommonPrinter
import net.sf.orcc.df.Actor
import java.io.File
import net.sf.orcc.util.OrccUtil
import net.sf.orcc.df.Port
import net.sf.orcc.ir.TypeBool
import net.sf.orcc.ir.TypeFloat
import net.sf.orcc.ir.TypeInt
import net.sf.orcc.ir.TypeUint
import net.sf.orcc.ir.TypeString
import net.sf.orcc.ir.TypeList
import net.sf.orcc.df.Action
import net.sf.orcc.df.Pattern
import net.sf.orcc.ir.Var
import net.sf.orcc.ir.Procedure
import net.sf.orcc.ir.BlockBasic
import net.sf.orcc.ir.InstLoad
import net.sf.orcc.ir.InstStore
import net.sf.orcc.ir.InstAssign
import org.eclipse.emf.ecore.EObject
import net.sf.orcc.backends.ir.InstCast
import org.eclipse.emf.common.util.EMap


class ActorPrinter extends CommonPrinter{
	
	protected Actor actor
	
	
	private EMap<Var,Port> inVarToPort;
	
	private EMap<Var,Port> outVarToPort;
	
	private String actorPackage
	
	new (Actor actor, String actorPackage){
		this.actor = actor
		this.actorPackage = actorPackage
	}
	
	
	def printActor(String targetFolder){
		val content = compileActor
		val file = new File(targetFolder + File::separator + actor.simpleName + ".cal")
		
		if(needToWriteFile(content, file)) {
			OrccUtil::printFile(content, file)
			return 0
		} else {
			return 1
		}
			
	}
		
	
	def compileActor()'''
	package «actorPackage»;
	
	actor «actor.simpleName»(«actorParameters»)
		«actorInputs»
			==>
				«actorOutputs»

		«stateVars»
		
		«FOR action: actor.actions»
			«actorAction(action)»
		«ENDFOR»

	end
	'''
	
	def actorParameters()'''
	'''
	
	
	def actorInputs()'''
		«FOR port: actor.inputs SEPARATOR ", "»
			«actorPort(port)»
		«ENDFOR»
	'''
	
	def actorPort(Port port)'''
		«port.type.doSwitch»  «port.name»
	'''
	
	
	def actorOutputs()'''
		«FOR port: actor.outputs SEPARATOR ", " AFTER ":"»
			«actorPort(port)»
		«ENDFOR»
	'''
	
	// State Variables
	
	def stateVars() '''
	'''
	
	// Actions
	
	def actorAction(Action action){
		inVarToPort = action.inputPattern.varToPortMap
		outVarToPort = action.outputPattern.varToPortMap
	'''
		«action.name» : action 
							«actionPorts(action.inputPattern)» 
								==> 
									«actionPorts(action.outputPattern)»
		«IF !action.body.locals.empty»«actionLocals(action.body)»«ENDIF»
		do
			«actionBody(action.body)»
		end
	'''}
	
	def actionPorts(Pattern pattern)'''
		«FOR port: pattern.ports SEPARATOR ", "»
			«actionPort(port,pattern.portToVarMap.get(port))»
		«ENDFOR»
	'''
	
	def actionPort(Port port, Var portVar)
		'''«port.name»:[«portVar.indexedName»]'''
	
	def actionGuard(Action action)'''
	'''
	
	def actionLocals(Procedure procedure)'''
		var
			«FOR local: procedure.locals SEPARATOR ", "»
				«local.type.doSwitch» «local.indexedName»
			«ENDFOR»
	'''
	
	def actionBody(Procedure proc)'''
		«proc.doSwitch»
	'''
	
	override caseBlockBasic(BlockBasic block) '''
		«FOR instr : block.instructions»
			«instr.doSwitch»
		«ENDFOR»
	'''
	
	override defaultCase(EObject object)'''
		«IF object instanceof InstCast »
			«caseInstCast(object as InstCast)»
		«ENDIF»
	'''
	
	
	override caseInstLoad(InstLoad inst) '''
		«inst.target.variable.indexedName» := «inst.source.variable.name»«IF !isSinglePortToken(inst.source.variable)»«FOR index : inst.indexes»[«index.doSwitch»]«ENDFOR»«ENDIF»;
	'''
	
	def isSinglePortToken(Var variable){
		if (inVarToPort.containsKey(variable) || outVarToPort.containsKey(variable))
			true
		else
			false
	}
	
	
	override caseInstStore(InstStore inst) '''
		«inst.target.variable.name»«IF !isSinglePortToken(inst.target.variable)»«FOR index : inst.indexes»[«index.doSwitch»]«ENDFOR»«ENDIF» := «inst.value.doSwitch»;
	'''
	
	override caseInstAssign(InstAssign inst) '''
		«inst.target.variable.name» := «inst.value.doSwitch»;
	'''
	
	def caseInstCast(InstCast inst)'''
		«inst.target.variable.name» := «inst.source.variable.name»;
	'''
	
	// CAL types
	
	override caseTypeBool(TypeBool type) {
		'''bool'''
	}
	
	override caseTypeFloat(TypeFloat type) {
		'''float'''
	}
	
	override caseTypeInt(TypeInt type) {
		'''int(size=«type.size»)'''
	}
	
	override caseTypeUint(TypeUint type) {
		'''uint(size=«type.size»)'''
	}
	
	override caseTypeString(TypeString type) {
		"String"
	}
	
	override caseTypeList(TypeList type) {
		'''«IF type.size == 1»«type.type.doSwitch»«ELSE»List(type:«type.type.doSwitch», size=«type.size»)«ENDIF»'''
	}
	
	
	
}