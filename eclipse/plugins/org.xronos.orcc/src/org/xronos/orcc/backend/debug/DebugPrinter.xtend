package org.xronos.orcc.backend.debug

import java.io.File
import net.sf.orcc.backends.c.InstancePrinter
import net.sf.orcc.ir.Procedure
import org.eclipse.emf.ecore.EObject
import org.xronos.orcc.ir.BlockMutex
import org.xronos.orcc.ir.InstPortStatus
import net.sf.orcc.df.Port

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
	
	override defaultCase(EObject object){
		if (object instanceof BlockMutex){
			caseBlockMutex(object as BlockMutex);
		} else if (object instanceof InstPortStatus){
			caseInstPortStatus(object as InstPortStatus);
		}
		return null;
	}
	
	def caseBlockMutex(BlockMutex blockMutex)'''
		// Paralle Block
		«FOR block : blockMutex.blocks»
				«block.doSwitch»
		«ENDFOR»
	'''
	
	def caseInstPortStatus(InstPortStatus portStatus) {
	val Port port = portStatus.port as Port;
	'''
		«portStatus.target.variable» = portStatus(«port.name»);
	'''
	}
}