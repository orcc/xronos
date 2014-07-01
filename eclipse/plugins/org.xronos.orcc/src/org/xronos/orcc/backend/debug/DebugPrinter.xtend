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
import net.sf.orcc.df.Action

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
	
	override profileEnd(Action action) ''''''
	override profileStart(Action action) ''''''
	
	
	
}