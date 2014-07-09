package org.xronos.orcc.forge.scheduler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sf.orcc.df.Action;
import net.sf.orcc.df.Actor;
import net.sf.orcc.df.util.DfVisitor;
import net.sf.orcc.ir.Block;
import net.sf.orcc.ir.Def;
import net.sf.orcc.ir.ExprVar;
import net.sf.orcc.ir.Expression;
import net.sf.orcc.ir.InstAssign;
import net.sf.orcc.ir.InstLoad;
import net.sf.orcc.ir.InstReturn;
import net.sf.orcc.ir.IrFactory;
import net.sf.orcc.ir.Procedure;
import net.sf.orcc.ir.Use;
import net.sf.orcc.ir.Var;
import net.sf.orcc.ir.util.AbstractIrVisitor;
import net.sf.orcc.ir.util.IrUtil;

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

	private class SchedulerBlocks extends AbstractIrVisitor<List<Block>> {

		private Map<Var, Var> localsCopyMap;

		@Override
		public List<Block> caseInstReturn(InstReturn returnInstr) {
			Var isSchedulableVar = IrFactory.eINSTANCE.createVar(
					IrFactory.eINSTANCE.createTypeBool(), procedure.getName(),
					true, 0);
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
		public List<Block> caseProcedure(Procedure procedure) {
			this.procedure = procedure;

			// -- Copy Locals
			localsCopyMap = new HashMap<Var, Var>();
			for (Var var : procedure.getLocals()) {
				Var copyVar = IrUtil.copy(var);
				copyVar.setName(currentAction.getName() + "IsSchedulable");
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

		return blocks;
	}

}
