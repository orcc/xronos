package org.xronos.orcc.backend.debug

import java.io.File
import net.sf.orcc.backends.c.InstancePrinter
import net.sf.orcc.ir.Procedure
import net.sf.orcc.util.OrccUtil
import org.eclipse.emf.ecore.EObject
import org.xronos.orcc.ir.BlockMutex
import net.sf.orcc.ir.InstAssign
import net.sf.orcc.ir.InstStore
import net.sf.orcc.ir.InstLoad
import net.sf.orcc.df.Actor
import net.sf.orcc.ir.BlockBasic
import java.util.Map
import net.sf.orcc.ir.Var
import org.xronos.openforge.lim.Port
import org.xronos.openforge.lim.Bus
import net.sf.orcc.ir.BlockIf
import net.sf.orcc.ir.BlockWhile

class XronosDebug extends InstancePrinter {

	def printProcedure(String targetFolder, Procedure procedure) {
		val file = new File(targetFolder + File::separator + procedure.name + ".c")
		val content = print(procedure)
		if (needToWriteFile(content, file)) {
			OrccUtil::printFile(content, file)
			return 0
		} else {
			return 1
		}
	}

	def printActor(Actor actor, Procedure scheduler) {
		'''
			«FOR action : actor.actions»
				«print(action.body)»
			«ENDFOR»
			«IF scheduler != null»
				«print(scheduler)»
			«ENDIF»
		'''
	}

	def printActor(String targetFolder, Actor actor, Procedure scheduler) {
		val file = new File(targetFolder + File::separator + actor.name + ".c")
		val content = printActor(actor, scheduler);

		if (needToWriteFile(content, file)) {
			OrccUtil::printFile(content, file)
			return 0
		} else {
			return 1
		}
	}

	def caseBlockMutex(BlockMutex blockMutex) {
		val inputs = blockMutex.getAttribute("inputs").objectValue as Map<Var,Port>
		val outputs = blockMutex.getAttribute("outputs").objectValue as Map<Var,Bus>
		'''
			// -- Mutex Block
			// Inputs: [«FOR variable : inputs.keySet SEPARATOR ","»«variable.name» «ENDFOR»]
			// Outputs: [«FOR variable : outputs.keySet SEPARATOR ","»«variable.name» «ENDFOR»]
			«FOR block : blockMutex.blocks»
				«block.doSwitch»
			«ENDFOR»
			// -- End Mutex Block
		'''
	}

	override caseBlockBasic(BlockBasic block) {
		val inputs = block.getAttribute("inputs").objectValue as Map<Var,Port>
		val outputs = block.getAttribute("outputs").objectValue as Map<Var,Bus>
		'''
			// -- Block Basic
			// Inputs: [«FOR variable : inputs.keySet SEPARATOR ","»«variable.name» «ENDFOR»]
			// Outputs: [«FOR variable : outputs.keySet SEPARATOR ","»«variable.name» «ENDFOR»]
			«super.caseBlockBasic(block)»
			// -- End Block basic
		'''
	}

	override caseBlockIf(BlockIf block) {
		val inputs = block.getAttribute("inputs").objectValue as Map<Var,Port>
		val outputs = block.getAttribute("outputs").objectValue as Map<Var,Bus>
		'''
			// -- Block If
			// Inputs: [«FOR variable : inputs.keySet SEPARATOR ","»«variable.name» «ENDFOR»]
			// Outputs: [«FOR variable : outputs.keySet SEPARATOR ","»«variable.name» «ENDFOR»]
			«super.caseBlockIf(block)»
			// -- End Block If
		'''
	}

	override caseBlockWhile(BlockWhile block) {
		val inputs = block.getAttribute("inputs").objectValue as Map<Var,Port>
		val outputs = block.getAttribute("outputs").objectValue as Map<Var,Bus>
		'''
			// -- Block While
			// Inputs: [«FOR variable : inputs.keySet SEPARATOR ","»«variable.name» «ENDFOR»]
			// Outputs: [«FOR variable : outputs.keySet SEPARATOR ","»«variable.name» «ENDFOR»]
			«super.caseBlockWhile(block)»
			// -- End Block While
		'''
	}

	override defaultCase(EObject object) {
		'''
			«IF (object instanceof BlockMutex)»
				«caseBlockMutex(object as BlockMutex)»
			«ENDIF»
		'''
	}

	override caseInstAssign(InstAssign inst) '''
		// Assign
		«super.caseInstAssign(inst)»
	'''

	override caseInstStore(InstStore inst) '''
		// Store
		«super.caseInstStore(inst)»
	'''

	override caseInstLoad(InstLoad inst) '''
		// Load
		«super.caseInstLoad(inst)»
	'''

}
