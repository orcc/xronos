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
 * If you modify this Program, or any covered work, by linking or 
 * combining it with Eclipse libraries (or a modified version of that 
 * library), containing parts covered by the terms of EPL,
 * the licensors of this Program grant you additional permission to convey 
 * the resulting work. {Corresponding Source for a non-source form of such 
 * a combination shall include the source code for the parts of Eclipse 
 * libraries used as well as that of the  covered work.}
 */
package org.xronos.orcc.analysis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sf.orcc.df.Action;
import net.sf.orcc.df.util.DfVisitor;
import net.sf.orcc.ir.InstLoad;
import net.sf.orcc.ir.InstStore;
import net.sf.orcc.ir.Instruction;
import net.sf.orcc.ir.Procedure;
import net.sf.orcc.ir.Var;
import net.sf.orcc.ir.util.AbstractIrVisitor;

public class StateVarAnalysis extends DfVisitor<Void> {

	private class VariableAnalyzer extends AbstractIrVisitor<Void> {

		@Override
		public Void caseInstLoad(InstLoad load) {
			Var source = load.getSource().getVariable();
			if (!stateVarOrderOfAppearace.containsKey(source)) {
				// Create the first entry on the Map
				Map<Procedure, List<Instruction>> procInst = new HashMap<Procedure, List<Instruction>>();
				List<Instruction> instructions = new ArrayList<Instruction>();
				instructions.add(load);
				procInst.put(procedure, instructions);

				// Put the variable on the Map
				stateVarOrderOfAppearace.put(source, procInst);
			} else {

				if (!stateVarOrderOfAppearace.get(source)
						.containsKey(procedure)) {
					List<Instruction> instructions = new ArrayList<Instruction>();
					instructions.add(load);
					stateVarOrderOfAppearace.get(source).put(procedure,
							instructions);
				} else {
					List<Instruction> instructions = stateVarOrderOfAppearace
							.get(source).get(procedure);
					instructions.add(load);
				}
			}
			return null;
		}

		@Override
		public Void caseInstStore(InstStore store) {
			Var target = store.getTarget().getVariable();
			if (!stateVarOrderOfAppearace.containsKey(target)) {
				// Create the first entry on the Map
				Map<Procedure, List<Instruction>> procInst = new HashMap<Procedure, List<Instruction>>();
				List<Instruction> instructions = new ArrayList<Instruction>();
				instructions.add(store);
				procInst.put(procedure, instructions);

				// Put the variable on the Map
				stateVarOrderOfAppearace.put(target, procInst);
			} else {
				if (!stateVarOrderOfAppearace.get(target)
						.containsKey(procedure)) {
					List<Instruction> instructions = new ArrayList<Instruction>();
					instructions.add(store);
					stateVarOrderOfAppearace.get(target).put(procedure,
							instructions);
				} else {
					List<Instruction> instructions = stateVarOrderOfAppearace
							.get(target).get(procedure);
					instructions.add(store);
				}
			}
			return null;
		}

		@Override
		public Void caseProcedure(Procedure procedure) {
			this.procedure = procedure;
			return super.caseProcedure(procedure);
		}

	}

	/**
	 * State Variables
	 */
	private Map<Var, List<Procedure>> stateVariables;

	/**
	 * Order of appearance of a local variable in an action
	 */
	private Map<Var, Map<Procedure, List<Instruction>>> localVarOrderOfAppearace;

	/**
	 * Order of appearance of a state variable in an action
	 */

	private Map<Var, Map<Procedure, List<Instruction>>> stateVarOrderOfAppearace;

	public StateVarAnalysis() {
		stateVariables = new HashMap<Var, List<Procedure>>();
		localVarOrderOfAppearace = new HashMap<Var, Map<Procedure, List<Instruction>>>();
		stateVarOrderOfAppearace = new HashMap<Var, Map<Procedure, List<Instruction>>>();
	}

	@Override
	public Void caseAction(Action action) {

		// Analyze the variables
		VariableAnalyzer variableAnalyzer = new VariableAnalyzer();
		variableAnalyzer.doSwitch(action.getBody());

		return null;
	}

	public Map<Var, Map<Procedure, List<Instruction>>> getOrderOfAppearance() {
		return stateVarOrderOfAppearace;
	}

}
