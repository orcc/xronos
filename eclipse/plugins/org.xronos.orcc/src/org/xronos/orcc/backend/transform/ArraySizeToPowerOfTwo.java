/*
 * Copyright (c) 2014, Ecole Polytechnique Fédérale de Lausanne
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

import net.sf.orcc.df.Action;
import net.sf.orcc.df.Actor;
import net.sf.orcc.df.util.DfVisitor;
import net.sf.orcc.ir.ExprInt;
import net.sf.orcc.ir.Expression;
import net.sf.orcc.ir.IrFactory;
import net.sf.orcc.ir.Procedure;
import net.sf.orcc.ir.TypeList;
import net.sf.orcc.ir.Var;
import net.sf.orcc.ir.util.AbstractIrVisitor;
import net.sf.orcc.ir.util.ValueUtil;

import org.xronos.orcc.design.util.XronosMathUtil;

/**
 * This visitor transforms the size of an List array to a power of 2
 * 
 * @author Endri Bezati
 * 
 */
public class ArraySizeToPowerOfTwo extends DfVisitor<Void> {

	public class LocalArraySizeToPowerOfTwo extends AbstractIrVisitor<Void> {

		@Override
		public Void caseProcedure(Procedure procedure) {
			for (Var var : procedure.getLocals()) {
				if (var.getType().isList()) {
					sizeToPowerOfTwo((TypeList) var.getType());
				}
			}
			return null;
		}

	}

	@Override
	public Void caseActor(Actor actor) {
		for (Var var : actor.getStateVars()) {
			// Check if is a List
			if (var.getType().isList()) {
				// Fix Size if necessary
				sizeToPowerOfTwo((TypeList) var.getType());
			}
		}

		// Now check for local List declared in actions
		for (Action action : actor.getActions()) {
			LocalArraySizeToPowerOfTwo arraySizeToPowerOfTwo = new LocalArraySizeToPowerOfTwo();
			arraySizeToPowerOfTwo.doSwitch(action.getBody());
		}

		return null;
	}

	private void sizeToPowerOfTwo(TypeList type) {
		if (type.getType().isList()) {
			sizeToPowerOfTwo((TypeList) type.getType());
		}

		Expression sizeExpr = type.getSizeExpr();
		if (sizeExpr.isExprInt()) {
			ExprInt intExpr = (ExprInt) sizeExpr;
			if (!ValueUtil.isPowerOfTwo(intExpr)) {
				Integer newValue = XronosMathUtil.nearestPowTwo(intExpr
						.getIntValue());
				ExprInt newSize = IrFactory.eINSTANCE.createExprInt(newValue);
				type.setSizeExpr(newSize);
			}

		}
	}

}
