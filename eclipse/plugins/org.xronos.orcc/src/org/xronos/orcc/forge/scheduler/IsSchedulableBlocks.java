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
package org.xronos.orcc.forge.scheduler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sf.orcc.df.Action;
import net.sf.orcc.df.Actor;
import net.sf.orcc.df.Port;
import net.sf.orcc.df.impl.PatternImpl;
import net.sf.orcc.df.util.DfVisitor;
import net.sf.orcc.ir.Block;
import net.sf.orcc.ir.BlockBasic;
import net.sf.orcc.ir.Def;
import net.sf.orcc.ir.ExprVar;
import net.sf.orcc.ir.Expression;
import net.sf.orcc.ir.InstAssign;
import net.sf.orcc.ir.InstCall;
import net.sf.orcc.ir.InstLoad;
import net.sf.orcc.ir.InstReturn;
import net.sf.orcc.ir.IrFactory;
import net.sf.orcc.ir.Procedure;
import net.sf.orcc.ir.Use;
import net.sf.orcc.ir.Var;
import net.sf.orcc.ir.util.AbstractIrVisitor;
import net.sf.orcc.ir.util.IrUtil;
import net.sf.orcc.util.util.EcoreHelper;

import org.xronos.orcc.ir.InstPortPeek;
import org.xronos.orcc.ir.XronosIrFactory;

/**
 * This visitor constructs the isSchedulabe blocks for each action
 * 
 * @author Endri Bezati
 *
 */
public class IsSchedulableBlocks extends DfVisitor<List<Block>> {

	/**
	 * The Actions Scheduler procedure
	 */
	Procedure scheduler;

	/**
	 * Map of Action that contains the return variable
	 */
	private Map<Action, Var> isSchedulableActionVar;

	private Action currentAction;

	public IsSchedulableBlocks(Procedure scheduler) {
		this.scheduler = scheduler;
	}

	private class TransformLoadZeroToPeek extends AbstractIrVisitor<Void> {

		@Override
		public Void caseInstLoad(InstLoad load) {
			Var source = load.getSource().getVariable();

			PatternImpl pattern = EcoreHelper.getContainerOfType(source,
					PatternImpl.class);
			if (pattern != null) {
				Port port = pattern.getVarToPortMap().get(source);
				if (pattern.getNumTokens(port) == 1) {
					Def def = load.getTarget();
					InstPortPeek peek = XronosIrFactory.eINSTANCE
							.createInstPortPeek();
					peek.setPort(port);
					peek.setTarget(def);

					BlockBasic block = EcoreHelper.getContainerOfType(load,
							BlockBasic.class);
					int idx = block.getInstructions().indexOf(load);
					IrUtil.delete(load);

					block.add(idx, peek);
				}
			}
			return null;
		}

	}

	private class SchedulerBlocks extends AbstractIrVisitor<List<Block>> {

		private SchedulerBlocks(){
			super(true);
		}
		
		private Map<Var, Var> localsCopyMap;

		@Override
		public List<Block> caseInstReturn(InstReturn returnInstr) {
			Var isSchedulableVar = IrFactory.eINSTANCE.createVar(
					IrFactory.eINSTANCE.createTypeBool(), procedure.getName(),
					true, 0);
			scheduler.addLocal(isSchedulableVar);
			Expression value = IrUtil.copy(returnInstr.getValue());
			// -- Create the new assign
			InstAssign assign = IrFactory.eINSTANCE.createInstAssign(
					isSchedulableVar, value);
			// -- Revisit the new assign for changing its value
			doSwitch(assign);

			// -- Add instruction before the InstReturn and delete InstReturn
			IrUtil.addInstBeforeExpr(returnInstr.getValue(), assign);
			IrUtil.delete(returnInstr);

			// -- Save the current isSchedulable variable
			isSchedulableActionVar.put(currentAction, isSchedulableVar);
			return null;
		}

		@Override
		public List<Block> caseInstAssign(InstAssign assign) {
			doSwitch(assign.getValue());
			Var target = assign.getTarget().getVariable();
			if (localsCopyMap.containsKey(target)) {
				Var copyTarget = localsCopyMap.get(target);
				Def def = IrFactory.eINSTANCE.createDef(copyTarget);
				assign.setTarget(def);
			}
			return null;
		}

		@Override
		public List<Block> caseInstLoad(InstLoad load) {
			super.caseInstLoad(load);
			Var target = load.getTarget().getVariable();
			if (localsCopyMap.containsKey(target)) {
				Var copyTarget = localsCopyMap.get(target);
				Def def = IrFactory.eINSTANCE.createDef(copyTarget);
				load.setTarget(def);
			}
			return null;
		}

		@Override
		public List<Block> caseExprVar(ExprVar object) {
			Var var = object.getUse().getVariable();
			if (localsCopyMap.containsKey(var)) {
				Var copyTarget = localsCopyMap.get(var);
				Use use = IrFactory.eINSTANCE.createUse(copyTarget);
				object.setUse(use);
			}
			return null;
		}
		
		

		@Override
		public List<Block> caseInstCall(InstCall call) {
			super.caseInstCall(call);
			Var target = call.getTarget().getVariable();
			if (localsCopyMap.containsKey(target)) {
				Var copyTarget = localsCopyMap.get(target);
				Def def = IrFactory.eINSTANCE.createDef(copyTarget);
				call.setTarget(def);
			}
			return null;
		}

		@Override
		public List<Block> caseProcedure(Procedure procedure) {
			this.procedure = procedure;

			// -- Copy Locals
			localsCopyMap = new HashMap<Var, Var>();
			for (Var var : procedure.getLocals()) {
				Var copyVar = IrUtil.copy(var);
				copyVar.setName(currentAction.getName()+"_"+copyVar.getName());
				scheduler.addLocal(copyVar);
				localsCopyMap.put(var, copyVar);
			}

			List<Block> blocks = new ArrayList<Block>();
			doSwitch(procedure.getBlocks());

			for (Block block : procedure.getBlocks()) {
				blocks.add(IrUtil.copy(block));
			}

			return blocks;
		}

	}

	@Override
	public List<Block> caseActor(Actor actor) {
		isSchedulableActionVar = new HashMap<Action, Var>();
		List<Block> blocks = new ArrayList<Block>();

		for (Action action : actor.getActions()) {
			currentAction = action;
			Procedure actionScheduler = IrUtil.copy(action.getScheduler());
			List<Block> schedulerBlocks = new SchedulerBlocks()
					.doSwitch(actionScheduler);

			// Copy Blocks to procedure
			blocks.addAll(schedulerBlocks);
		}

		// Transform Load on head of an input to a Peek instruction
		new TransformLoadZeroToPeek().doSwitch(blocks);

		return blocks;
	}

}
