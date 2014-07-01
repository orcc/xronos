/* 
 * XRONOS, High Level Synthesis of Streaming Applications
 * 
 * Copyright (C) 2014 EPFL SCI STI MM
 *
 * This file is part of XRONOS.
 *
 * XRONOS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * XRONOS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with XRONOS.  If not, see <http://www.gnu.org/licenses/>.
 * 
 * Additional permission under GNU GPL version 3 section 7
 * 
 * If you modify this Program, or any covered work, by linking or combining it
 * with Eclipse (or a modified version of Eclipse or an Eclipse plugin or 
 * an Eclipse library), containing parts covered by the terms of the 
 * Eclipse Public License (EPL), the licensors of this Program grant you 
 * additional permission to convey the resulting work.  Corresponding Source 
 * for a non-source form of such a combination shall include the source code 
 * for the parts of Eclipse libraries used as well as that of the  covered work.
 * 
 */
package org.xronos.orcc.analysis

import java.io.File
import net.sf.orcc.df.Actor
import net.sf.orcc.ir.InstLoad
import net.sf.orcc.util.OrccUtil

class StateVarAnalysisWriter {

	private StateVarAnalysis stateVars = new StateVarAnalysis();

	def print(Actor actor, String targetFolder) {

		stateVars.doSwitch(actor)
		val content = varUsedByTwoOrMoreActions(actor)
		val file = new File(targetFolder + File::separator + actor.simpleName + "_stateVar.txt")

		OrccUtil::printFile(content, file)
	}

	def actorVarInformation(Actor actor) '''
		Actor : actor.simpleName
		
		State Vars
		«FOR stateVar : actor.stateVars»
			«stateVar.name» - «stateVar.type»
			«IF stateVars.orderOfAppearance.containsKey(stateVar)»
				«FOR procedure : stateVars.orderOfAppearance.get(stateVar).keySet»
					Procedure «procedure.name»
						«FOR instruction : stateVars.orderOfAppearance.get(stateVar).get(procedure)»
							«IF instruction instanceof InstLoad» load «ENDIF»
							«IF instruction instanceof InstLoad» store «ENDIF»
						«ENDFOR»
				«ENDFOR»	
			«ENDIF»
		«ENDFOR»
		---
	'''

	def varUsedByTwoOrMoreActions(Actor actor) '''
		Actor : actor.simpleName
		
		State Vars
		«FOR stateVar : actor.stateVars»
			«IF stateVars.orderOfAppearance.containsKey(stateVar)»
				«IF stateVar.assignable»
					«IF stateVars.orderOfAppearance.get(stateVar).size > 2»
						--- «stateVar.name» - «stateVar.type»
							«FOR procedure : stateVars.orderOfAppearance.get(stateVar).keySet»
								Procedure «procedure.name»
									«FOR instruction : stateVars.orderOfAppearance.get(stateVar).get(procedure)»
										«IF instruction instanceof InstLoad» load «ENDIF»
										«IF instruction instanceof InstLoad» store «ENDIF»
									«ENDFOR»
							«ENDFOR»
							
					«ENDIF»	
				«ENDIF»	
			«ENDIF»
		«ENDFOR»
		---
	'''

}
