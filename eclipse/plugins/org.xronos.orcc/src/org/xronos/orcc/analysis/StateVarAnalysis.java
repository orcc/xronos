/*
 * Copyright (c) 2013, Ecole Polytechnique Fédérale de Lausanne
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 *   * Redistributions of source code must retain the above copyright notice,
 *     this list of conditions and the following disclaimer.
 *   * Redistributions in binary form must reproduce the above copyright notice,
 *     this list of conditions and the following disclaimer in the documentation
 *     and/or other materials provided with the distribution.
 *   * Neither the name of the Ecole Polytechnique Fédérale de Lausanne nor the names of its
 *     contributors may be used to endorse or promote products derived from this
 *     software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY
 * WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
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
			Var target = load.getSource().getVariable();
			if (!stateVarOrderOfAppearace.containsKey(target)) {
				// Create the first entry on the Map
				Map<Procedure, List<Instruction>> procInst = new HashMap<Procedure, List<Instruction>>();
				List<Instruction> instructions = new ArrayList<Instruction>();
				instructions.add(load);
				procInst.put(procedure, instructions);

				// Put the variable on the Map
				stateVarOrderOfAppearace.put(target, procInst);
			} else {
				List<Instruction> instructions = stateVarOrderOfAppearace.get(
						target).get(procedure);
				instructions.add(load);
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
				List<Instruction> instructions = stateVarOrderOfAppearace.get(
						target).get(procedure);
				instructions.add(store);
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

}
