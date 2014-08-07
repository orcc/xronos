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

package org.xronos.orcc.forge.transform.memory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.util.EcoreUtil;

import net.sf.orcc.df.Actor;
import net.sf.orcc.ir.BlockBasic;
import net.sf.orcc.ir.ExprVar;
import net.sf.orcc.ir.Expression;
import net.sf.orcc.ir.InstAssign;
import net.sf.orcc.ir.InstCall;
import net.sf.orcc.ir.InstLoad;
import net.sf.orcc.ir.InstStore;
import net.sf.orcc.ir.Instruction;
import net.sf.orcc.ir.IrFactory;
import net.sf.orcc.ir.Param;
import net.sf.orcc.ir.Procedure;
import net.sf.orcc.ir.Use;
import net.sf.orcc.ir.Var;
import net.sf.orcc.ir.util.AbstractIrVisitor;
import net.sf.orcc.ir.util.IrUtil;
import net.sf.orcc.util.Void;
import net.sf.orcc.util.util.EcoreHelper;

/**
 * This transformation finds state variables that are used during a procedure,
 * it loads their value in the beginning of a procedure and it replaces all
 * loads and stores that follows up until the end of the procedure. Once it has
 * finished it adds a stores of these variables at the last block basic of the
 * procedure. This transformation might increase critical path in hardware
 * generation.
 * 
 * @author Endri Bezati
 *
 */
public class ScalarRedundancyElimination extends AbstractIrVisitor<Void> {

	private class RetrieveStateVars extends AbstractIrVisitor<Void> {

		@Override
		public Void caseInstLoad(InstLoad load) {
			Var source = load.getSource().getVariable();
			if (!source.getType().isList()) {
				procStateVarUsed.add(source);
				Procedure procedure = EcoreHelper.getContainerOfType(load,
						Procedure.class);
				if (procedureStoreVarMap.containsKey(procedure)) {
					procedureStoreVarMap.get(procedure).add(source);
				} else {
					Set<Var> vars = new HashSet<Var>();
					vars.add(source);
					procedureStoreVarMap.put(procedure, vars);
				}
			}
			return null;
		}

		@Override
		public Void caseInstStore(InstStore store) {
			Var target = store.getTarget().getVariable();
			if (!target.getType().isList()) {
				procStateVarUsed.add(target);
				storedVars.add(target);

				Procedure procedure = EcoreHelper.getContainerOfType(store,
						Procedure.class);
				if (procedureStoreVarMap.containsKey(procedure)) {
					procedureStoreVarMap.get(procedure).add(target);
				} else {
					Set<Var> vars = new HashSet<Var>();
					vars.add(target);
					procedureStoreVarMap.put(procedure, vars);
				}

			}
			return null;
		}

		@Override
		public Void caseInstCall(InstCall call) {
			return doSwitch(call.getProcedure());
		}
	}

	private class CalledReplaceAndPropagate extends AbstractIrVisitor<Void> {

		Map<Var, Var> storeParamMap;

		public CalledReplaceAndPropagate(Map<Var, Var> storeParamMap) {
			this.storeParamMap = storeParamMap;
		}

		@Override
		public Void caseInstLoad(InstLoad load) {
			Var source = load.getSource().getVariable();
			if (storeParamMap.containsKey(source)) {
				EList<Use> targetUses = load.getTarget().getVariable()
						.getUses();
				while (!targetUses.isEmpty()) {
					ExprVar expr = EcoreHelper.getContainerOfType(
							targetUses.get(0), ExprVar.class);
					ExprVar repExpr = IrFactory.eINSTANCE
							.createExprVar(storeParamMap.get(source));
					EcoreUtil.replace(expr, IrUtil.copy(repExpr));
					IrUtil.delete(expr);
				}
				IrUtil.delete(load);
			}
			return null;
		}

		@Override
		public Void caseInstStore(InstStore store) {
			Var target = store.getTarget().getVariable();
			if (storeParamMap.containsKey(target)) {
				Var pVar = storeParamMap.get(target);
				Expression value = IrUtil.copy(store.getValue());
				InstAssign assign = IrFactory.eINSTANCE.createInstAssign(pVar,
						value);
				IrUtil.addInstBeforeExpr(store.getValue(), assign);
				IrUtil.delete(store);
			}
			return null;
		}

	}

	private class ReplaceAndPropagate extends AbstractIrVisitor<Void> {

		@Override
		public Void caseInstCall(InstCall call) {
			Procedure procedure = call.getProcedure();

			if (procedureStoreVarMap.containsKey((procedure))) {
				Procedure copy = IrUtil.copy(procedure);
				copy.setName(procedure.getName() + "_copy");
				Actor actor = EcoreHelper.getContainerOfType(procedure,
						Actor.class);
				actor.getProcs().add(copy);

				Set<Var> vars = procedureStoreVarMap.get(procedure);
				Map<Var, Var> storeParamMap = new HashMap<Var, Var>();
				for (Var var : vars) {
					Var pVar = IrFactory.eINSTANCE.createVar(
							IrUtil.copy(var.getType()),
							"param_" + var.getName(), true, 0);
					Param param = IrFactory.eINSTANCE.createParam(pVar);
					copy.getParameters().add(param);
					storeParamMap.put(var, pVar);
				}
				// -- Now replace all stores/loads with the given pVar
				new CalledReplaceAndPropagate(storeParamMap).doSwitch(copy);

				// -- Create a new InstCall with the new procedure
			}

			return null;
		}

		@Override
		public Void caseInstLoad(InstLoad load) {
			Var source = load.getSource().getVariable();
			if (procStateVarUsed.contains(source)) {
				EList<Use> targetUses = load.getTarget().getVariable()
						.getUses();
				while (!targetUses.isEmpty()) {
					ExprVar expr = EcoreHelper.getContainerOfType(
							targetUses.get(0), ExprVar.class);
					ExprVar repExpr = IrFactory.eINSTANCE
							.createExprVar(procedure.getLocal("temp_"
									+ source.getName()));
					EcoreUtil.replace(expr, IrUtil.copy(repExpr));
					IrUtil.delete(expr);
					
				}
				IrUtil.delete(load);
			}
			return null;
		}

		@Override
		public Void caseInstStore(InstStore store) {
			Var target = store.getTarget().getVariable();
			if (procStateVarUsed.contains(target)) {
				Var tempVar = procedure.getLocal("temp_" + target.getName());
				Expression value = IrUtil.copy(store.getValue());
				InstAssign assign = IrFactory.eINSTANCE.createInstAssign(
						tempVar, value);
				IrUtil.addInstBeforeExpr(store.getValue(), assign);
				IrUtil.delete(store);
			}
			return null;
		}

	}

	Set<Var> procStateVarUsed;
	Set<Var> storedVars;
	Map<Procedure, Set<Var>> procedureStoreVarMap;

	@Override
	public Void caseProcedure(Procedure procedure) {
		// -- Initialize
		this.procedure = procedure;
		procStateVarUsed = new HashSet<Var>();
		storedVars = new HashSet<Var>();
		procedureStoreVarMap = new HashMap<Procedure, Set<Var>>();

		// -- Retrieve state vars used by loads and stores
		new RetrieveStateVars().doSwitch(procedure);

		// -- Create a Load block with all the retrieved state vars
		BlockBasic block = IrFactory.eINSTANCE.createBlockBasic();
		for (Var var : procStateVarUsed) {
			Var temp = IrFactory.eINSTANCE.createVar(
					IrUtil.copy(var.getType()), "temp_" + var.getName(), true,
					0);
			procedure.addLocal(temp);
			InstLoad laod = IrFactory.eINSTANCE.createInstLoad(temp, var);
			block.add(laod);
		}

		// -- Replace all intermediate store and loads
		new ReplaceAndPropagate().doSwitch(procedure);

		// -- Add load block
		procedure.getBlocks().add(0, block);

		// -- For all vars that needs a store add them to the lasr Block Basic
		for (Var var : storedVars) {
			Var temp = procedure.getLocal("temp_" + var.getName());
			InstStore store = IrFactory.eINSTANCE.createInstStore(var, temp);
			block = procedure.getLast();
			EList<Instruction> lastBlock = procedure.getLast()
					.getInstructions();
			int lastInstIndex = lastBlock.size() - 1;
			lastBlock.add(lastInstIndex, store);
		}

		return null;
	}

}
