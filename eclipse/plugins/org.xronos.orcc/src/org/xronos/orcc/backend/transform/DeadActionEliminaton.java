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

package org.xronos.orcc.backend.transform;

import java.util.ArrayList;
import java.util.List;

import net.sf.orcc.OrccRuntimeException;
import net.sf.orcc.df.Action;
import net.sf.orcc.df.Actor;
import net.sf.orcc.df.State;
import net.sf.orcc.df.Transition;
import net.sf.orcc.df.util.DfVisitor;
import net.sf.orcc.graph.Edge;
import net.sf.orcc.ir.BlockBasic;
import net.sf.orcc.ir.BlockIf;
import net.sf.orcc.ir.Expression;
import net.sf.orcc.ir.InstAssign;
import net.sf.orcc.ir.InstLoad;
import net.sf.orcc.ir.InstReturn;
import net.sf.orcc.ir.Instruction;
import net.sf.orcc.ir.Procedure;
import net.sf.orcc.ir.Type;
import net.sf.orcc.ir.TypeList;
import net.sf.orcc.ir.Var;
import net.sf.orcc.ir.util.AbstractIrVisitor;
import net.sf.orcc.ir.util.IrUtil;
import net.sf.orcc.ir.util.ValueUtil;
import net.sf.orcc.util.OrccLogger;

/**
 * This transformation eliminates dead actions
 * 
 * @author Endri Bezati
 * 
 */
public class DeadActionEliminaton extends DfVisitor<Void> {

	/**
	 * This class evaluates statically guards
	 * 
	 * @author Endri Bezati
	 * 
	 */
	public class GuardEvaluator extends AbstractIrVisitor<Object> {

		Object result = null;

		@Override
		public Object caseBlockBasic(BlockBasic block) {
			List<Instruction> instructions = block.getInstructions();
			for (Instruction instruction : instructions) {
				Object result = doSwitch(instruction);
				if (result != null) {
					return result;
				}
			}
			return null;
		}

		@Override
		public Object caseBlockIf(BlockIf blockIf) {
			Expression condition = blockIf.getCondition();
			Object resultCondition = doSwitch(condition);
			if (resultCondition == null) {
				return null;
			} else {
				if (ValueUtil.isBool(resultCondition)) {
					Boolean value = (Boolean) resultCondition;
					if (value) {
						return doSwitch(blockIf.getThenBlocks());
					} else {
						return doSwitch(blockIf.getElseBlocks());
					}
				}
			}

			return null;
		}

		@Override
		public Object caseInstAssign(InstAssign assign) {
			Var target = assign.getTarget().getVariable();
			Object value = exprInterpreter.doSwitch(assign.getValue());
			target.setValue(value);
			return null;
		}

		@Override
		public Object caseInstLoad(InstLoad load) {
			Var target = load.getTarget().getVariable();
			Var source = load.getSource().getVariable();
			if (source.isAssignable()) {
				return null;
			}
			if (load.getIndexes().isEmpty()) {
				if (source.getValue() != null) {
					target.setValue(source.getValue());
				}
			} else {
				Object array = source.getValue();
				Object[] indexes = new Object[load.getIndexes().size()];
				int i = 0;
				for (Expression index : load.getIndexes()) {
					indexes[i++] = exprInterpreter.doSwitch(index);
				}
				Type type = ((TypeList) source.getType()).getInnermostType();
				try {
					Object value = ValueUtil.get(type, array, indexes);
					target.setValue(value);
				} catch (IndexOutOfBoundsException e) {
					target.setValue(null);
				} catch (OrccRuntimeException e) {
					target.setValue(null);
				}
			}
			return null;
		}

		@Override
		public Object caseInstReturn(InstReturn instr) {
			if (instr.getValue() == null) {
				return null;
			}
			return exprInterpreter.doSwitch(instr.getValue());
		}

		@Override
		public Object caseProcedure(Procedure procedure) {
			this.procedure = procedure;
			return doSwitch(procedure.getBlocks());
		}

	}

	/**
	 * The expression evaluator
	 */
	private XronosExprEvaluator exprInterpreter;

	Boolean debug;

	public DeadActionEliminaton() {
		this(false);
	}

	public DeadActionEliminaton(Boolean debug) {
		this.debug = debug;
		exprInterpreter = new XronosExprEvaluator();
	}

	@Override
	public Void caseActor(Actor actor) {
		List<Action> toBeEliminated = new ArrayList<Action>();

		for (Action action : actor.getActions()) {
			GuardEvaluator guardEvaluator = new GuardEvaluator();
			Object eliminate = guardEvaluator.doSwitch(action.getScheduler());
			if (ValueUtil.isBool(eliminate)) {
				Boolean value = (Boolean) eliminate;
				// if the result is false then eliminate
				if (!value) {
					if (debug) {
						OrccLogger.warnln("Xronos: action \""
								+ action.getName()
								+ "\" is unreachable, eliminating!");
					}
					toBeEliminated.add(action);
				}
			}
		}

		// Delete from FSM
		if (actor.getFsm() != null) {
			for (Action action : actor.getActions()) {
				if (toBeEliminated.contains(action)) {
					for (State state : actor.getFsm().getStates()) {
						List<Edge> edges = new ArrayList<Edge>();
						for (Edge edge : state.getOutgoing()) {
							Transition transition = (Transition) edge;
							if (transition.getAction() == action) {
								edges.add(edge);
							}
						}
						state.getOutgoing().removeAll(edges);
					}
				}
			}
		}

		// Delete from the actions list
		for (Action action : toBeEliminated) {
			if (!actor.getActionsOutsideFsm().isEmpty()) {
				actor.getActionsOutsideFsm().remove(action);
			}
			actor.getActions().remove(action);
			IrUtil.delete(action);
		}

		return null;
	}

}
