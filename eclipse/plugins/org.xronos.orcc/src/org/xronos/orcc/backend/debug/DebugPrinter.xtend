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
package org.xronos.orcc.backend.debug

import java.io.File
import java.util.List
import net.sf.orcc.backends.c.InstancePrinter
import net.sf.orcc.backends.ir.InstCast
import net.sf.orcc.df.Actor
import net.sf.orcc.df.Port
import net.sf.orcc.ir.BlockIf
import net.sf.orcc.ir.BlockWhile
import net.sf.orcc.ir.ExprVar
import net.sf.orcc.ir.Expression
import net.sf.orcc.ir.InstPhi
import net.sf.orcc.ir.Procedure
import net.sf.orcc.ir.Var
import net.sf.orcc.util.OrccUtil
import org.eclipse.emf.ecore.EObject
import org.xronos.orcc.design.ResourceCache
import org.xronos.orcc.design.visitors.stmIO.BranchIO
import org.xronos.orcc.design.visitors.stmIO.LoopIO
import org.xronos.orcc.ir.BlockMutex
import org.xronos.orcc.ir.InstPortPeek
import org.xronos.orcc.ir.InstPortRead
import org.xronos.orcc.ir.InstPortStatus
import org.xronos.orcc.ir.InstPortWrite
import org.xronos.orcc.ir.InstSimplePortWrite

class DebugPrinter extends InstancePrinter {
	
	ResourceCache resourceCache;
	
	def printProcedure(String targetFolder, Procedure procedure, String name) {
		val content = print(procedure)
		val file = new File(targetFolder + File::separator + name + ".c")
		
		if(needToWriteFile(content, file)) {
			OrccUtil::printFile(content, file)
			return 0
		} else {
			return 1
		}
	}
	
	
	def printActor(String targetFolder, Actor actor, Procedure scheduler, String name, ResourceCache resourceCache){
		this.resourceCache = resourceCache;
		val file = new File(targetFolder + File::separator + name + ".c")
		val content = printActorBody(actor,scheduler);
		if(needToWriteFile(content, file)) {
			OrccUtil::printFile(content, file)
			return 0
		} else {
			return 1
		}
	}
	
	def printActorBody(Actor actor, Procedure scheduler){
	'''
		// Actions
		«FOR action: actor.actions»
			«print(action)»
		«ENDFOR»
		// Scheduler
		«print(scheduler)»
	'''	
	}
	
	def caseInstCast(InstCast instCast)'''
	«instCast.target.variable.name» = «instCast.source.variable.name»;
	'''
	
	
	override defaultCase(EObject object){'''
		«IF (object instanceof BlockMutex)»
			«caseBlockMutex(object as BlockMutex)»
		«ELSEIF (object instanceof InstPortPeek)»
			«caseInstPortPeek(object as InstPortPeek)»	
		«ELSEIF (object instanceof InstPortRead)»
			«caseInstPortRead(object as InstPortRead)»
		«ELSEIF (object instanceof InstPortStatus)»
			«caseInstPortStatus(object as InstPortStatus)»
		«ELSEIF (object instanceof InstPortWrite)»
			«caseInstPortWrite(object as InstPortWrite)»
		«ELSEIF (object instanceof InstSimplePortWrite)»
			«caseInstSimplePortWrite(object as InstSimplePortWrite)»
		«ELSEIF object instanceof InstCast»
			«caseInstCast(object as InstCast)»
		«ENDIF»
	'''
	}
	
	
	override caseBlockIf(BlockIf block){
		var BranchIO branchIO = new BranchIO(block);
		var List<Var> inputs = branchIO.inputs;
		var List<Var> outputs = branchIO.outputs;
		var List<Var> thenInputs = branchIO.thenInputs;
		var List<Var> thenOutputs = branchIO.thenOutputs;
		var List<Var> elseInputs = branchIO.elseInputs;
		var List<Var> elseOutputs = branchIO.elseOutputs;
		
	'''
		// Block Branch : «block.lineNumber»
		// Inputs[«FOR variable: inputs SEPARATOR ","»«variable.name» «ENDFOR»]
		// Outputs[«FOR variable: outputs SEPARATOR ","»«variable.name» «ENDFOR»]
		// thenInputs[«FOR variable: thenInputs SEPARATOR ","»«variable.name» «ENDFOR»]
		// thenOutputs[«FOR variable: thenOutputs SEPARATOR ","»«variable.name» «ENDFOR»]
		// elseInputs[«FOR variable: elseInputs SEPARATOR ","»«variable.name» «ENDFOR»]
		// elseOutputs[«FOR variable: elseOutputs SEPARATOR ","»«variable.name» «ENDFOR»]
		«super.caseBlockIf(block)»
		«IF !block.joinBlock.instructions.empty»
			// Branch PHI  : «block.lineNumber»
			«FOR instruction: block.joinBlock.instructions»
				«IF instruction instanceof InstPhi»
					«printPhi(block.condition,instruction as InstPhi)»
				«ENDIF»
			«ENDFOR»
		«ENDIF»
	'''
	}
	
	def printPhi(Expression condition, InstPhi instPhi)'''
	«instPhi.target.variable.name» = «doSwitch(condition)» ? «(instPhi.values.get(0) as ExprVar).use.variable.name» : «(instPhi.values.get(1) as ExprVar).use.variable.name»;
	'''
	
	def caseBlockMutex(BlockMutex blockMutex)'''
		// Parallel Block
		«FOR block : blockMutex.blocks»
				«block.doSwitch»
		«ENDFOR»
	'''
	
	def caseInstPortStatus(InstPortStatus portStatus) {
	val Port port = portStatus.port as Port;
	'''
		«portStatus.target.variable.name» = portStatus(«port.name»);
	'''
	}
	
	def caseInstPortRead(InstPortRead portRead) {
	val Port port = portRead.port as Port;
	'''
		«portRead.target.variable.name» = portRead(«port.name»);
	'''
	}
	
	def caseInstPortPeek(InstPortPeek portPeek) {
	val Port port = portPeek.port as Port;
	'''
		«portPeek.target.variable.name» = portPeek(«port.name»);
	'''
	}
	
	def caseInstPortWrite(InstPortWrite portWrite) {
	val Port port = portWrite.port as Port;
	'''
		portWrite(«port.name», «(portWrite.value as ExprVar).use.variable.name»);
	'''
	}
	
	def caseInstSimplePortWrite(InstSimplePortWrite simplePortWrite) {
	val String name = simplePortWrite.name as String;
	'''
		simplePortWrite(«name», «simplePortWrite.value.doSwitch»);
	'''
	}
	
	
	override caseBlockWhile(BlockWhile blockWhile){
		var LoopIO loopIO = new LoopIO(blockWhile);
		var List<Var> inputs = loopIO.inputs;
		var List<Var> outputs = loopIO.outputs;
	'''
		// Block Loop : «blockWhile.lineNumber»
		// Inputs[«FOR variable: inputs SEPARATOR ","»«variable.name» «ENDFOR»]
		// Outputs[«FOR variable: outputs SEPARATOR ","»«variable.name» «ENDFOR»]
		«IF (blockWhile.joinBlock != null)»
			«blockWhile.joinBlock.doSwitch»
		«ENDIF»
		while («blockWhile.condition.doSwitch») {
			«FOR block : blockWhile.blocks»
				«block.doSwitch»
			«ENDFOR»
		«IF !blockWhile.joinBlock.instructions.empty»
			// Loop PHI : «blockWhile.lineNumber»
			«FOR instruction: blockWhile.joinBlock.instructions»
				«IF instruction instanceof InstPhi»
					«printPhi(blockWhile.condition,instruction as InstPhi)»
				«ENDIF»
			«ENDFOR»
		«ENDIF»
		}
	'''
	}
	
}