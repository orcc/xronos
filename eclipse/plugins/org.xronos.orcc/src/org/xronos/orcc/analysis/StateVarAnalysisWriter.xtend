package org.xronos.orcc.analysis

import java.io.File
import net.sf.orcc.ir.util.IrSwitch
import net.sf.orcc.df.Actor
import net.sf.orcc.util.OrccUtil
import net.sf.orcc.ir.InstLoad

class StateVarAnalysisWriter extends IrSwitch {
	
	
	private StateVarAnalysis stateVars = new StateVarAnalysis();
	
	def print(Actor actor, String targetFolder) {
		
		stateVars.doSwitch(actor)
		val content = varUsedByTwoOrMoreActions(actor)
		val file = new File(targetFolder + File::separator + actor.simpleName+"_stateVar.txt")
		
		OrccUtil::printFile(content, file)
	}
	
	
	def actorVarInformation(Actor actor)'''
	Actor : actor.simpleName
	
	State Vars
	«FOR stateVar: actor.stateVars»
		«stateVar.name» - «stateVar.type»
		«IF  stateVars.orderOfAppearance.containsKey(stateVar)»
			«FOR procedure: stateVars.orderOfAppearance.get(stateVar).keySet»
			Procedure «procedure.name»
				«FOR instruction: stateVars.orderOfAppearance.get(stateVar).get(procedure)»
					«IF instruction instanceof InstLoad» load «ENDIF»
					«IF instruction instanceof InstLoad» store «ENDIF»
				«ENDFOR»
			«ENDFOR»	
		«ENDIF»
	«ENDFOR»
	---
	'''
		
	def varUsedByTwoOrMoreActions(Actor actor)'''
	Actor : actor.simpleName
	
	State Vars
	«FOR stateVar: actor.stateVars»
		«IF  stateVars.orderOfAppearance.containsKey(stateVar)»
			«IF stateVar.assignable»
				«IF stateVars.orderOfAppearance.get(stateVar).size > 2»
				--- «stateVar.name» - «stateVar.type»
					«FOR procedure: stateVars.orderOfAppearance.get(stateVar).keySet»
					Procedure «procedure.name»
						«FOR instruction: stateVars.orderOfAppearance.get(stateVar).get(procedure)»
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