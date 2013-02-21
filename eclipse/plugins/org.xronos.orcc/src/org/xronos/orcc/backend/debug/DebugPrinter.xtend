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
import net.sf.orcc.ir.InstSpecific
import net.sf.orcc.ir.Procedure
import net.sf.orcc.ir.Var
import org.eclipse.emf.ecore.EObject
import org.xronos.orcc.design.ResourceCache
import org.xronos.orcc.design.visitors.stmIO.BranchIO
import org.xronos.orcc.design.visitors.stmIO.LoopIO
import org.xronos.orcc.ir.BlockMutex
import org.xronos.orcc.ir.InstPortPeek
import org.xronos.orcc.ir.InstPortRead
import org.xronos.orcc.ir.InstPortStatus
import org.xronos.orcc.ir.InstPortWrite
import net.sf.orcc.util.OrccUtil

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
	
	override caseInstSpecific(InstSpecific inst)'''
	«IF inst instanceof InstCast»
		«caseInstCast(inst as InstCast)»
	«ENDIF»
	'''
	
	def caseInstCast(InstCast instCast)'''
	«instCast.target.variable.indexedName» = «instCast.source.variable.indexedName»;
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
		// Inputs[«FOR variable: inputs SEPARATOR ","»«variable.indexedName» «ENDFOR»]
		// Outputs[«FOR variable: outputs SEPARATOR ","»«variable.indexedName» «ENDFOR»]
		// thenInputs[«FOR variable: thenInputs SEPARATOR ","»«variable.indexedName» «ENDFOR»]
		// thenOutputs[«FOR variable: thenOutputs SEPARATOR ","»«variable.indexedName» «ENDFOR»]
		// elseInputs[«FOR variable: elseInputs SEPARATOR ","»«variable.indexedName» «ENDFOR»]
		// elseOutputs[«FOR variable: elseOutputs SEPARATOR ","»«variable.indexedName» «ENDFOR»]
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
	«instPhi.target.variable.indexedName» = «doSwitch(condition)» ? «(instPhi.values.get(0) as ExprVar).use.variable.indexedName» : «(instPhi.values.get(1) as ExprVar).use.variable.indexedName»;
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
		«portStatus.target.variable.indexedName» = portStatus(«port.name»);
	'''
	}
	
	def caseInstPortRead(InstPortRead portRead) {
	val Port port = portRead.port as Port;
	'''
		«portRead.target.variable.indexedName» = portRead(«port.name»);
	'''
	}
	
	def caseInstPortPeek(InstPortPeek portPeek) {
	val Port port = portPeek.port as Port;
	'''
		«portPeek.target.variable.indexedName» = portPeek(«port.name»);
	'''
	}
	
	def caseInstPortWrite(InstPortWrite portWrite) {
	val Port port = portWrite.port as Port;
	'''
		portWrite(«port.name», «(portWrite.value as ExprVar).use.variable.indexedName»);
	'''
	}
	
	
	override caseBlockWhile(BlockWhile blockWhile){
		var LoopIO loopIO = new LoopIO(blockWhile);
		var List<Var> inputs = loopIO.inputs;
		var List<Var> outputs = loopIO.outputs;
	'''
		// Block Loop : «blockWhile.lineNumber»
		// Inputs[«FOR variable: inputs SEPARATOR ","»«variable.indexedName» «ENDFOR»]
		// Outputs[«FOR variable: outputs SEPARATOR ","»«variable.indexedName» «ENDFOR»]
		«IF (blockWhile.joinBlock != null)»
			«blockWhile.joinBlock.doSwitch»
		«ENDIF»
		«super.caseBlockWhile(blockWhile)»
		«IF !blockWhile.joinBlock.instructions.empty»
			// Loop PHI : «blockWhile.lineNumber»
			«FOR instruction: blockWhile.joinBlock.instructions»
				«IF instruction instanceof InstPhi»
					«printPhi(blockWhile.condition,instruction as InstPhi)»
				«ENDIF»
			«ENDFOR»
		«ENDIF»
	'''
	}
	
}