package org.xronos.orcc.backend.debug

import java.io.File
import java.util.HashMap
import net.sf.orcc.backends.c.InstancePrinter
import net.sf.orcc.df.Actor
import net.sf.orcc.ir.BlockBasic
import net.sf.orcc.ir.BlockIf
import net.sf.orcc.ir.BlockWhile
import net.sf.orcc.ir.InstAssign
import net.sf.orcc.ir.InstLoad
import net.sf.orcc.ir.InstStore
import net.sf.orcc.ir.Procedure
import net.sf.orcc.ir.Var
import net.sf.orcc.util.OrccUtil
import org.eclipse.emf.ecore.EObject
import org.xronos.openforge.lim.Bus
import org.xronos.openforge.lim.Port
import org.xronos.orcc.ir.BlockMutex
import org.xronos.orcc.ir.InstPortRead
import org.xronos.orcc.ir.InstPortStatus
import org.xronos.orcc.ir.InstPortWrite
import org.xronos.orcc.ir.InstPortPeek

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
			// -- Procedures
			«FOR procedure : actor.procs»
				«print(procedure)»
			«ENDFOR»
			// -- Actions
			«FOR action : actor.actions»
				«print(action.body)»
			«ENDFOR»
			// -- Scheduler
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
		var inputs = new HashMap<Var, Port>
		if (blockMutex.hasAttribute("inputs")) {
			inputs = blockMutex.getAttribute("inputs").objectValue as HashMap<Var,Port>
		}

		var outputs = new HashMap<Var, Bus>
		if (blockMutex.hasAttribute("outputs")) {
			outputs = blockMutex.getAttribute("outputs").objectValue as HashMap<Var,Bus>
		}
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
		var inputs = new HashMap<Var, Port>
		if (block.hasAttribute("inputs")) {
			inputs = block.getAttribute("inputs").objectValue as HashMap<Var,Port>
		}

		var outputs = new HashMap<Var, Bus>
		if (block.hasAttribute("outputs")) {
			outputs = block.getAttribute("outputs").objectValue as HashMap<Var,Bus>
		}
		'''
			// -- Block Basic
			// Inputs: [«FOR variable : inputs.keySet SEPARATOR ","»«variable.name» «ENDFOR»]
			// Outputs: [«FOR variable : outputs.keySet SEPARATOR ","»«variable.name» «ENDFOR»]
			«super.caseBlockBasic(block)»
			// -- End Block basic
		'''
	}

	override caseBlockIf(BlockIf block) {
		var inputs = new HashMap<Var, Port>
		if (block.hasAttribute("inputs")) {
			inputs = block.getAttribute("inputs").objectValue as HashMap<Var,Port>
		}

		var outputs = new HashMap<Var, Bus>
		if (block.hasAttribute("outputs")) {
			outputs = block.getAttribute("outputs").objectValue as HashMap<Var,Bus>
		}
		'''
			// -- Block If
			// Inputs: [«FOR variable : inputs.keySet SEPARATOR ","»«variable.name» «ENDFOR»]
			// Outputs: [«FOR variable : outputs.keySet SEPARATOR ","»«variable.name» «ENDFOR»]
			«super.caseBlockIf(block)»
			// -- End Block If
		'''
	}

	override caseBlockWhile(BlockWhile block) {
		var inputs = new HashMap<Var, Port>
		if (block.hasAttribute("inputs")) {
			inputs = block.getAttribute("inputs").objectValue as HashMap<Var,Port>
		}

		var outputs = new HashMap<Var, Bus>
		if (block.hasAttribute("outputs")) {
			outputs = block.getAttribute("outputs").objectValue as HashMap<Var,Bus>
		}
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
			«ELSEIF (object instanceof InstPortRead)»
				«caseInstPortRead(object as InstPortRead)»
			«ELSEIF (object instanceof InstPortPeek)»
				«caseInstPortPeek(object as InstPortPeek)»
			«ELSEIF (object instanceof InstPortStatus)»
				«caseInstPortStatus(object as InstPortStatus)»
			«ELSEIF (object instanceof InstPortWrite)»
				«caseInstPortWrite(object as InstPortWrite)»
			«ENDIF»
		'''
	}

	override caseInstAssign(InstAssign inst) '''
		// Assign
		«super.caseInstAssign(inst)»
	'''

	override caseInstStore(InstStore inst) '''
		// Store
		«inst.target.variable.hashCode»
		«super.caseInstStore(inst)»
	'''

	override caseInstLoad(InstLoad inst) '''
		// Load
		«inst.source.variable.hashCode»
		«super.caseInstLoad(inst)»
	'''

	def caseInstPortStatus(InstPortStatus portStatus) {
		val net.sf.orcc.df.Port port = portStatus.port as net.sf.orcc.df.Port;
		'''
			// Port Status
			«portStatus.target.variable.name» = portStatus(«port.name»);
		'''
	}

	def caseInstPortRead(InstPortRead portRead) {
		val net.sf.orcc.df.Port port = portRead.port as  net.sf.orcc.df.Port;
		'''
			// Port Read
			«portRead.target.variable.name» = portRead(«port.name»);
		'''
	}

	def caseInstPortPeek(InstPortPeek portPeek) {
		val net.sf.orcc.df.Port port = portPeek.port as  net.sf.orcc.df.Port;
		'''
			// Port Peek
			«portPeek.target.variable.name» = portPeek(«port.name»);
		'''
	}

	def caseInstPortWrite(InstPortWrite portWrite) {
		val net.sf.orcc.df.Port port = portWrite.port as  net.sf.orcc.df.Port;
		'''
			// Port Write
			portWrite(«port.name», «doSwitch(portWrite.value)»);
		'''
	}

}
