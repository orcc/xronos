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

	def caseBlockMutex(BlockMutex blockMutex) '''
		// Mutex Block
		«FOR block : blockMutex.blocks»
			«block.doSwitch»
		«ENDFOR»
	'''

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
