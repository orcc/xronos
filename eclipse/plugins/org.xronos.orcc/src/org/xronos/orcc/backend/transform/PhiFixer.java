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

import net.sf.orcc.ir.BlockBasic;
import net.sf.orcc.ir.ExprVar;
import net.sf.orcc.ir.Expression;
import net.sf.orcc.ir.InstAssign;
import net.sf.orcc.ir.InstPhi;
import net.sf.orcc.ir.IrFactory;
import net.sf.orcc.ir.Procedure;
import net.sf.orcc.ir.Var;
import net.sf.orcc.ir.util.AbstractIrVisitor;
import net.sf.orcc.util.Void;
import net.sf.orcc.util.util.EcoreHelper;

/**
 * This visitor add missing definition of variables found in the Phi Values
 * after the SSA transformation.
 * 
 * @author Endri Bezati
 * 
 */
public class PhiFixer extends AbstractIrVisitor<Void> {

	private void addAssign(Var var) {
		// Create its Zero value
		Expression value = null;
		if (var.getType().isBool()) {
			value = IrFactory.eINSTANCE.createExprBool(false);
		} else if (var.getType().isInt() || var.getType().isUint()) {
			value = IrFactory.eINSTANCE.createExprInt(0);
		} else if (var.getType().isFloat()) {
			value = IrFactory.eINSTANCE.createExprFloat(0);
		}

		// Create the assign instruction
		InstAssign assign = IrFactory.eINSTANCE.createInstAssign(var, value);

		// Create a BlockBasic and add the assign instruction
		BlockBasic blockBasic = IrFactory.eINSTANCE.createBlockBasic();
		blockBasic.add(assign);

		// Add the BlockBasic to the procedure
		Procedure procedure = EcoreHelper.getContainerOfType(var,
				Procedure.class);
		procedure.getBlocks().add(0, blockBasic);
	}

	@Override
	public Void caseExprVar(ExprVar object) {
		Var var = object.getUse().getVariable();
		if (var.getDefs().isEmpty() && !var.getType().isString()) {
			addAssign(var);
		}
		return null;
	}

	@Override
	public Void caseInstPhi(InstPhi phi) {
		for (Expression expr : phi.getValues()) {
			doSwitch(expr);
		}
		return null;
	}

}
