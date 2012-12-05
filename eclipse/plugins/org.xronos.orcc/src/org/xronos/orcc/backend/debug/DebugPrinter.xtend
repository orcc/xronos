package org.xronos.orcc.backend.debug

import java.io.File
import net.sf.orcc.ir.Procedure
import net.sf.orcc.backends.c.InstancePrinter
import org.eclipse.emf.ecore.EObject
import org.xronos.orcc.ir.BlockMutex

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
			doSwitch((object as BlockMutex).blocks);
		}
		return super.defaultCase(object);
	}
	
}