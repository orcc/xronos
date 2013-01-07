package org.xronos.orcc.backend.debug

import java.io.File
import net.sf.orcc.backends.c.InstancePrinter
import net.sf.orcc.df.Actor
import net.sf.orcc.df.Port
import net.sf.orcc.ir.BlockIf
import net.sf.orcc.ir.BlockWhile
import net.sf.orcc.ir.ExprVar
import net.sf.orcc.ir.Expression
import net.sf.orcc.ir.InstPhi
import net.sf.orcc.ir.Procedure
import org.eclipse.emf.ecore.EObject
import org.xronos.orcc.ir.BlockMutex
import org.xronos.orcc.ir.InstPortPeek
import org.xronos.orcc.ir.InstPortRead
import org.xronos.orcc.ir.InstPortStatus
import org.xronos.orcc.ir.InstPortWrite
import net.sf.orcc.ir.InstSpecific
import net.sf.orcc.backends.ir.InstCast
import org.xronos.orcc.design.ResourceCache

class DebugPrinter extends InstancePrinter {
	
	ResourceCache resourceCache;
	
	def printProcedure(String targetFolder, Procedure procedure, String name) {
		val content = print(procedure)
		val file = new File(targetFolder + File::separator + name + ".c")
		
		if(needToWriteFile(content, file)) {
			printFile(content, file)
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
			printFile(content, file)
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
	
	
	override caseBlockIf(BlockIf block)'''
	«IF resourceCache.getBlockInput(block) != null»
	// Block If
	// Inputs[«FOR variable: resourceCache.getBlockInput(block) SEPARATOR ","»«variable.indexedName» «ENDFOR»]
	// Outputs[«FOR variable: resourceCache.getBlockOutput(block) SEPARATOR ","»«variable.indexedName» «ENDFOR»]
	«ENDIF»
	«super.caseBlockIf(block)»
	«IF !block.joinBlock.instructions.empty»
		// PHI
		«FOR instruction: block.joinBlock.instructions»
			«IF instruction instanceof InstPhi»
				«printPhi(block.condition,instruction as InstPhi)»
			«ENDIF»
		«ENDFOR»
	«ENDIF»
	'''
	
	
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
	
	
	override caseBlockWhile(BlockWhile blockWhile)'''
	«IF (blockWhile.joinBlock != null)»
		«blockWhile.joinBlock.doSwitch»
	«ENDIF»
	«super.caseBlockWhile(blockWhile)»
	'''
	
}