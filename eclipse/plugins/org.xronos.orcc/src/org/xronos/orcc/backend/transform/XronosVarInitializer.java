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

import net.sf.orcc.df.Actor;
import net.sf.orcc.df.util.DfVisitor;
import net.sf.orcc.ir.Expression;
import net.sf.orcc.ir.Type;
import net.sf.orcc.ir.TypeList;
import net.sf.orcc.ir.Var;
import net.sf.orcc.ir.util.ValueUtil;

public class XronosVarInitializer extends DfVisitor<Void> {

	XronosExprEvaluator exprEvaluator;

	public XronosVarInitializer() {
		exprEvaluator = new XronosExprEvaluator();
	}

	@Override
	public Void caseActor(Actor actor) {
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
		return super.caseActor(actor);
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
				exprEvaluator.setType((TypeList) type);
			}
			variable.setValue(exprEvaluator.doSwitch(initConst));
		}
	}

}
