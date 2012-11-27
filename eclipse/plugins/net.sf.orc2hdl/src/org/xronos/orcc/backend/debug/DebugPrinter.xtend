package org.xronos.orcc.backend.debug

import net.sf.orcc.backends.c.InstancePrinter
import java.io.File
import net.sf.orcc.ir.Procedure

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
}