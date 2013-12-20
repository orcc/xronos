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

import net.sf.orcc.ir.ExprBinary;
import net.sf.orcc.ir.ExprInt;
import net.sf.orcc.ir.Expression;
import net.sf.orcc.ir.IrFactory;
import net.sf.orcc.ir.OpBinary;
import net.sf.orcc.ir.Type;
import net.sf.orcc.ir.util.AbstractIrVisitor;
import net.sf.orcc.ir.util.ExpressionEvaluator;
import net.sf.orcc.ir.util.IrUtil;
import net.sf.orcc.ir.util.TypeUtil;
import net.sf.orcc.ir.util.ValueUtil;
import net.sf.orcc.util.Void;

import org.eclipse.emf.ecore.util.EcoreUtil;

/**
 * This class transforms a constant division in a form of
 * <em>a/c = (((2^32)+c-1)/c) * (a) / (2^32)</em>, which costs only one
 * multiplication, ((2^32)+b-1)/b) is recalculated
 * 
 * @author Endri Bezati
 * 
 */
public class ConstantDivision extends AbstractIrVisitor<Void> {

	public ConstantDivision() {
		super(true);
	}

	@Override
	public Void caseExprBinary(ExprBinary expr) {
		super.caseExprBinary(expr);
		if (expr.getOp() == OpBinary.DIV) {
			int val = 0;
			Boolean isInteger = false;
			try {
				val = new ExpressionEvaluator().evaluateAsInteger(expr.getE2());
				isInteger = true;
			} catch (Exception e) {
				isInteger = false;
			}
			if (isInteger) {
				if (!ValueUtil.isPowerOfTwo(val)) {
					BigInteger c = BigInteger.valueOf(val);
					BigInteger one = BigInteger.valueOf(1);
					BigInteger twoPow32 = one.shiftLeft(31);
					BigInteger addC = twoPow32.add(c);
					BigInteger constantValue = addC.subtract(BigInteger
							.valueOf(1));
					BigInteger constantValueDivC = constantValue.divide(c);
					ExprInt exprConstValue = IrFactory.eINSTANCE
							.createExprInt(constantValueDivC);

					Expression a = IrUtil.copy(expr.getE1());

					Type constMultType = TypeUtil.getTypeBinary(OpBinary.TIMES,
							expr.getE1().getType(), IrFactory.eINSTANCE
									.createTypeInt(TypeUtil
											.getSize(constantValueDivC)));

					ExprBinary contMult = IrFactory.eINSTANCE.createExprBinary(
							exprConstValue, OpBinary.TIMES, a, constMultType);

					ExprInt shiftTwoPow32 = IrFactory.eINSTANCE
							.createExprInt(31);

					Expression constDiv = IrFactory.eINSTANCE.createExprBinary(
							contMult, OpBinary.SHIFT_RIGHT, shiftTwoPow32,
							IrFactory.eINSTANCE.createTypeInt());

					EcoreUtil.replace(expr, constDiv);

				}
			}
		}
		return null;
	}

}
