/* 
 * XRONOS-EXELIXI
 * 
 * Copyright (C) 2011-2016 EPFL SCI STI MM
 *
 * This file is part of XRONOS-EXELIXI.
 *
 * XRONOS-EXELIXI is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * XRONOS-EXELIXI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with XRONOS-EXELIXI. If not, see <http://www.gnu.org/licenses/>.
 * 
 * Additional permission under GNU GPL version 3 section 7
 * 
 * If you modify this Program, or any covered work, by linking or combining it
 * with Eclipse (or a modified version of Eclipse or an Eclipse plugin or 
 * an Eclipse library), containing parts covered by the terms of the 
 * Eclipse Public License (EPL), the licensors of this Program grant you 
 * additional permission to convey the resulting work.  Corresponding Source 
 * for a non-source form of such a combination shall include the source code 
 * for the parts of Eclipse libraries used as well as that of the covered work.
 * 
 */
package org.xronos.orcc.backend.debug

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
import net.sf.orcc.util.FilesManager
import org.eclipse.emf.ecore.EObject
import org.xronos.openforge.lim.Bus
import org.xronos.openforge.lim.Port
import org.xronos.orcc.ir.BlockMutex
import org.xronos.orcc.ir.InstPortPeek
import org.xronos.orcc.ir.InstPortRead
import org.xronos.orcc.ir.InstPortStatus
import org.xronos.orcc.ir.InstPortWrite

/**
 * 
 * @author Endri Bezati
 */
class XronosDebug extends InstancePrinter {

	def printProcedure(String targetFolder, Procedure procedure) {
		val content = print(procedure)
		FilesManager.writeFile(content, targetFolder, procedure.name + ".c")
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
		val content = printActor(actor, scheduler);
		FilesManager.writeFile(content, targetFolder, actor.name + ".c")
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
