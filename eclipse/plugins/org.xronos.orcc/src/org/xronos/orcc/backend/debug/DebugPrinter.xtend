package org.xronos.orcc.backend.debug

import java.io.File
import net.sf.orcc.backends.c.InstancePrinter
import net.sf.orcc.df.Port
import net.sf.orcc.ir.BlockWhile
import net.sf.orcc.ir.Procedure
import org.eclipse.emf.ecore.EObject
import org.xronos.orcc.ir.BlockMutex
import org.xronos.orcc.ir.InstPortRead
import org.xronos.orcc.ir.InstPortStatus
import org.xronos.orcc.ir.InstPortWrite
import net.sf.orcc.ir.ExprVar
import org.xronos.orcc.ir.InstPortPeek

class DebugPrinter extends InstancePrinter {
	
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