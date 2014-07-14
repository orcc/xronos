package org.xronos.orcc.backend.debug

import java.io.File
import net.sf.orcc.backends.c.InstancePrinter
import net.sf.orcc.ir.Procedure
import net.sf.orcc.util.OrccUtil

class XronosDebug extends InstancePrinter {


	def printProcedure(String targetFolder, Procedure procedure){
		val file = new File(targetFolder + File::separator + procedure.name + ".c")
		val content = print(procedure)
		if(needToWriteFile(content, file)) {
			OrccUtil::printFile(content, file)
			return 0
		} else {
			return 1
		}
	}	
}