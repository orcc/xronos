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
		this.exprInterpreter = new XronosExprEvaluator();
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
