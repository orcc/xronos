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

import java.math.BigInteger;

import net.sf.orcc.df.Action;
import net.sf.orcc.df.Actor;
import net.sf.orcc.df.util.DfVisitor;
import net.sf.orcc.ir.BlockBasic;
import net.sf.orcc.ir.ExprBool;
import net.sf.orcc.ir.ExprInt;
import net.sf.orcc.ir.ExprVar;
import net.sf.orcc.ir.Expression;
import net.sf.orcc.ir.InstAssign;
import net.sf.orcc.ir.InstLoad;
import net.sf.orcc.ir.IrFactory;
import net.sf.orcc.ir.Procedure;
import net.sf.orcc.ir.Var;
import net.sf.orcc.ir.util.AbstractIrVisitor;
import net.sf.orcc.ir.util.IrUtil;

/**
 * This class transforms all Loads relate to parameters to assigns with a
 * constant literal
 * 
 * @author Endri Bezati
 * 
 */
public class XronosParameterPropagation extends DfVisitor<Void> {
	private class Propagator extends AbstractIrVisitor<Void> {

		public Propagator() {
			super(true);
		}

		@Override
		public Void caseInstLoad(InstLoad load) {
			Var source = load.getSource().getVariable();
			if (actor.getParameters().contains(source)) {
				Var target = load.getTarget().getVariable();

				Expression exprValue = null;
				if (source.getValue() instanceof ExprVar) {
					Var valueVar = ((ExprVar) source.getValue()).getUse()
							.getVariable();
					if (valueVar.getValue() == null) {
						exprValue = getValue((Expression) valueVar
								.getInitialValue());
					}
				} else {
					if (source.getInitialValue() != null) {
						exprValue = getValue((Expression) source
								.getInitialValue());
					} else {
						if (source.getValue() != null) {
							if (source.getType().isBool()) {
								Boolean value = (Boolean) source.getValue();
								exprValue = factory.createExprBool(value);
							} else if (source.getType().isInt()
									|| source.getType().isUint()) {
								BigInteger value = (BigInteger) source
										.getValue();
								exprValue = factory.createExprInt(value);
							}
						} else {
							throw new NullPointerException(
									"source has no value on load line:"
											+ load.getLineNumber());
						}
					}

				}
				InstAssign assign = factory.createInstAssign(target, exprValue);
				BlockBasic block = load.getBlock();
				int index = block.getInstructions().indexOf(load);
				block.add(index, assign);
				IrUtil.delete(load);
			}
			return null;
		}
	}

	private IrFactory factory = IrFactory.eINSTANCE;

	@Override
	public Void caseActor(Actor actor) {
		this.actor = actor;

		// Visit all actions
		for (Action verifAction : actor.getActions()) {
			Propagator propagator = new Propagator();
			propagator.doSwitch(verifAction.getBody());
			propagator.doSwitch(verifAction.getScheduler());
		}

		// Visit all the procedures
		for (Procedure procedure : actor.getProcs()) {
			Propagator propagator = new Propagator();
			propagator.doSwitch(procedure);
		}

		return null;
	}

	private Expression getValue(Expression inputExpr) {
		Expression expression = null;
		if (inputExpr instanceof ExprInt) {
			int value = ((ExprInt) inputExpr).getIntValue();
			expression = factory.createExprInt(value);
		} else if (inputExpr instanceof ExprBool) {
			boolean value = ((ExprBool) inputExpr).isValue();
			expression = factory.createExprBool(value);
		}
		return expression;
	}

}
