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

import java.math.BigDecimal;
import java.math.BigInteger;
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
import net.sf.orcc.ir.ExprBinary;
import net.sf.orcc.ir.ExprUnary;
import net.sf.orcc.ir.ExprVar;
import net.sf.orcc.ir.Expression;
import net.sf.orcc.ir.InstAssign;
import net.sf.orcc.ir.InstLoad;
import net.sf.orcc.ir.InstReturn;
import net.sf.orcc.ir.Instruction;
import net.sf.orcc.ir.OpBinary;
import net.sf.orcc.ir.Procedure;
import net.sf.orcc.ir.Type;
import net.sf.orcc.ir.TypeList;
import net.sf.orcc.ir.Var;
import net.sf.orcc.ir.util.AbstractIrVisitor;
import net.sf.orcc.ir.util.ExpressionEvaluator;
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
	 * An expression evaluator that accepts null values
	 * 
	 * @author Endri Bezati
	 * 
	 */
	public class ExprEvaluator extends ExpressionEvaluator {
		@Override
		public Object caseExprBinary(ExprBinary expr) {
			Object val1 = doSwitch(expr.getE1());
			Object val2 = doSwitch(expr.getE2());
			Object result = null;
			if (val1 == null && val2 == null) {
				return null;
			} else if (val1 != null && val2 == null) {
				if (ValueUtil.isBool(val1)) {
					Boolean value = (Boolean) val1;
					if (expr.getOp() == OpBinary.LOGIC_AND) {
						if (value) {
							return null;
						} else {
							return false;
						}
					} else if (expr.getOp() == OpBinary.LOGIC_OR) {
						if (value) {
							return true;
						} else {
							return null;
						}
					}
				}
			} else if (val1 == null && val2 != null) {
				if (ValueUtil.isBool(val2)) {
					Boolean value = (Boolean) val2;
					if (expr.getOp() == OpBinary.LOGIC_AND) {
						if (value) {
							return null;
						} else {
							return false;
						}
					} else if (expr.getOp() == OpBinary.LOGIC_OR) {
						if (value) {
							return true;
						} else {
							return null;
						}
					}
				}
			} else {
				result = ValueUtil.compute(val1, expr.getOp(), val2);
			}
			return result;
		}

		@Override
		public Object caseExprUnary(ExprUnary expr) {
			Object value = doSwitch(expr.getExpr());
			if (value == null) {
				return null;
			}
			Object result = ValueUtil.compute(expr.getOp(), value);

			return result;
		}

		@Override
		public Object caseExprVar(ExprVar expr) {
			Var var = expr.getUse().getVariable();
			Object value = var.getValue();

			return value;
		}
	}

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
				target.setValue(source.getValue());
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
					throw new OrccRuntimeException(
							"Array Index Out of Bound at line "
									+ load.getLineNumber());
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
	private ExprEvaluator exprInterpreter;

	public DeadActionEliminaton() {
		this.exprInterpreter = new ExprEvaluator();
	}

	@Override
	public Void caseActor(Actor actor) {
		List<Action> toBeEliminated = new ArrayList<Action>();

		// initialize parameters
		for (Var var : actor.getParameters()) {
			initializeVar(var);
		}

		// initializes state variables
		for (Var stateVar : actor.getStateVars()) {
			if (!stateVar.isAssignable()) {
				initializeVar(stateVar);
			}
		}

		for (Action action : actor.getActions()) {
			GuardEvaluator guardEvaluator = new GuardEvaluator();
			Object eliminate = guardEvaluator.doSwitch(action.getScheduler());
			if (ValueUtil.isBool(eliminate)) {
				Boolean value = (Boolean) eliminate;
				// if the result is false then eliminate
				if (!value) {
					OrccLogger.warnln("\tXronos: action \"" + action.getName()
							+ "\" is unreachable, eliminating!");
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
		if (!actor.getActionsOutsideFsm().isEmpty()) {
			actor.getActionsOutsideFsm().removeAll(toBeEliminated);
		}
		actor.getActions().removeAll(toBeEliminated);

		return null;
	}

	/**
	 * Initializes the given variable.
	 * 
	 * @param variable
	 *            a variable
	 */
	protected void initializeVar(Var variable) {
		Type type = variable.getType();
		Expression initConst = variable.getInitialValue();
		if (initConst == null) {
			Object value;
			if (type.isBool()) {
				value = false;
			} else if (type.isFloat()) {
				value = BigDecimal.ZERO;
			} else if (type.isInt() || type.isUint()) {
				value = BigInteger.ZERO;
			} else if (type.isList()) {
				value = ValueUtil.createArray((TypeList) type);
			} else if (type.isString()) {
				value = "";
			} else {
				value = null;
			}
			variable.setValue(value);
		} else {
			// evaluate initial constant value
			if (type.isList()) {
				exprInterpreter.setType((TypeList) type);
			}
			variable.setValue(exprInterpreter.doSwitch(initConst));
		}
	}
}
