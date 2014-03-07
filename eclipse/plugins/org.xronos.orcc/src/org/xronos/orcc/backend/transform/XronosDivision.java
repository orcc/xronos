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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sf.orcc.df.Action;
import net.sf.orcc.df.Actor;
import net.sf.orcc.df.util.DfVisitor;
import net.sf.orcc.ir.Block;
import net.sf.orcc.ir.BlockBasic;
import net.sf.orcc.ir.BlockIf;
import net.sf.orcc.ir.BlockWhile;
import net.sf.orcc.ir.ExprBinary;
import net.sf.orcc.ir.ExprInt;
import net.sf.orcc.ir.ExprUnary;
import net.sf.orcc.ir.Expression;
import net.sf.orcc.ir.InstAssign;
import net.sf.orcc.ir.InstCall;
import net.sf.orcc.ir.InstReturn;
import net.sf.orcc.ir.IrFactory;
import net.sf.orcc.ir.OpBinary;
import net.sf.orcc.ir.OpUnary;
import net.sf.orcc.ir.Procedure;
import net.sf.orcc.ir.Type;
import net.sf.orcc.ir.Var;
import net.sf.orcc.ir.util.AbstractIrVisitor;
import net.sf.orcc.ir.util.ExpressionEvaluator;
import net.sf.orcc.ir.util.IrUtil;
import net.sf.orcc.ir.util.TypeUtil;
import net.sf.orcc.ir.util.ValueUtil;
import net.sf.orcc.util.Void;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.util.EcoreUtil;

/**
 * This visitor transforms the division operator to a set of simple operations
 * and a loop. The loop counter is based on the maximum size of the
 * numerator/denominator. This division are taking from the openForge resources,
 * which are almost identical to the Texas Instruments division algorithms used
 * on DSPs.
 * 
 * @author Endri Bezati
 * 
 */
public class XronosDivision extends DfVisitor<Void> {

	private class Substitutor extends AbstractIrVisitor<Void> {
		public Substitutor() {
			super(true);
		}

		@Override
		public Void caseExprBinary(ExprBinary expr) {
			// Is this expression operator a division ?
			if (expr.getOp() == OpBinary.DIV) {
				// Replace this division iff its denominator is not power of 2
				if (!isConstant(expr.getE2())) {
					// Replace this division by adding a division function
					int sizeOfE1 = expr.getE1().getType().getSizeInBits();
					int sizeOfE2 = expr.getE2().getType().getSizeInBits();
					int maxSize = Math.max(sizeOfE1, sizeOfE2);

					// Check if we have a procedure with the same maxSize
					Procedure division = null;
					if (xronosDivision != null) {
						if (xronosDivision.containsKey(maxSize)) {
							division = xronosDivision.get(maxSize);
						} else {
							division = divisionII(maxSize);
							xronosDivision.put(maxSize, division);
						}
					} else {
						xronosDivision = new HashMap<Integer, Procedure>();
						division = divisionII(maxSize);
						xronosDivision.put(maxSize, division);
					}

					Var varNum = procedure.newTempLocalVariable(
							IrFactory.eINSTANCE.createTypeInt(maxSize), "num");
					Var varDenum = procedure.newTempLocalVariable(
							IrFactory.eINSTANCE.createTypeInt(maxSize), "den");
					Var varResult = procedure.newTempLocalVariable(
							IrFactory.eINSTANCE.createTypeInt(maxSize),
							"result");

					InstAssign assign0 = IrFactory.eINSTANCE.createInstAssign(
							varNum, expr.getE1());
					InstAssign assign1 = IrFactory.eINSTANCE.createInstAssign(
							varDenum, expr.getE2());

					List<Expression> parameters = new ArrayList<Expression>();
					parameters.add(IrFactory.eINSTANCE.createExprVar(varNum));
					parameters.add(IrFactory.eINSTANCE.createExprVar(varDenum));

					InstCall call = IrFactory.eINSTANCE.createInstCall(
							varResult, division, parameters);
					IrUtil.addInstBeforeExpr(expr, assign0);
					IrUtil.addInstBeforeExpr(expr, assign1);
					IrUtil.addInstBeforeExpr(expr, call);

					EcoreUtil.replace(expr,
							IrFactory.eINSTANCE.createExprVar(varResult));

				} else {
					// Replace this division by a multiplication and shift
					if (!ValueUtil.isPowerOfTwo(expr.getE2())) {
						int val = new ExpressionEvaluator()
								.evaluateAsInteger(expr.getE2());
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

						Type constMultType = TypeUtil.getTypeBinary(
								OpBinary.TIMES, expr.getE1().getType(),
								IrFactory.eINSTANCE.createTypeInt(TypeUtil
										.getSize(constantValueDivC)));

						ExprBinary contMult = IrFactory.eINSTANCE
								.createExprBinary(exprConstValue,
										OpBinary.TIMES, a, constMultType);

						ExprInt shiftTwoPow32 = IrFactory.eINSTANCE
								.createExprInt(31);

						Expression constDiv = IrFactory.eINSTANCE
								.createExprBinary(contMult,
										OpBinary.SHIFT_RIGHT, shiftTwoPow32,
										IrFactory.eINSTANCE.createTypeInt());

						EcoreUtil.replace(expr, constDiv);
					}
				}
			}
			return null;
		}

		/**
		 * Creates a branch like this if (var < 0) {var = -var; flipResult ^=
		 * 1;}
		 * 
		 * @param var
		 * @param flipResult
		 * @return
		 */
		public BlockIf checkNegVar(Var var, Var flipResult) {
			BlockIf blockIf = IrFactory.eINSTANCE.createBlockIf();
			// Give an empty Join Node
			BlockBasic join = IrFactory.eINSTANCE.createBlockBasic();
			blockIf.setJoinBlock(join);

			// Create Block IF condition (var < 0)
			Expression ifCondition = IrFactory.eINSTANCE.createExprBinary(
					IrFactory.eINSTANCE.createExprVar(var), OpBinary.LT,
					IrFactory.eINSTANCE.createExprInt(0),
					IrFactory.eINSTANCE.createTypeBool());
			// Set the condition to the blockIf
			blockIf.setCondition(ifCondition);

			// Create the "then" basic block
			BlockBasic blockBasic = IrFactory.eINSTANCE.createBlockBasic();

			// Create the following assign var = -var
			ExprUnary minus = IrFactory.eINSTANCE.createExprUnary(
					OpUnary.MINUS, IrFactory.eINSTANCE.createExprVar(var),
					IrFactory.eINSTANCE.createTypeInt(var.getType()
							.getSizeInBits()));
			InstAssign minusAssign = IrFactory.eINSTANCE.createInstAssign(var,
					minus);
			// Add assign to the block
			blockBasic.add(minusAssign);

			// Create the following assign flipResult ^= 1
			ExprBinary xor = IrFactory.eINSTANCE.createExprBinary(
					IrFactory.eINSTANCE.createExprVar(flipResult),
					OpBinary.BITXOR, IrFactory.eINSTANCE.createExprInt(1),
					IrFactory.eINSTANCE.createTypeInt(flipResult.getType()
							.getSizeInBits()));
			InstAssign xorAssign = IrFactory.eINSTANCE.createInstAssign(
					flipResult, xor);
			// Add assign to the block
			blockBasic.add(xorAssign);

			// Finally add the block basic to the blockif
			blockIf.getThenBlocks().add(blockBasic);

			return blockIf;
		}

		/**
		 * Creates an integer division with a result of maxSize bits
		 * 
		 * @param maxSize
		 * @return
		 */
		public Procedure divisionII(Integer maxSize) {
			Procedure division = IrFactory.eINSTANCE.createProcedure("divII_"
					+ maxSize, 0, IrFactory.eINSTANCE.createTypeInt());

			// Create parameters
			Var num = IrFactory.eINSTANCE.createVarInt("num", true, 0);
			num.setType(IrFactory.eINSTANCE.createTypeInt(maxSize));
			Var den = IrFactory.eINSTANCE.createVarInt("den", true, 0);
			den.setType(IrFactory.eINSTANCE.createTypeInt(maxSize));
			division.getParameters().add(IrFactory.eINSTANCE.createParam(num));
			division.getParameters().add(IrFactory.eINSTANCE.createParam(den));

			// Create local variables
			Var result = division.newTempLocalVariable(
					IrFactory.eINSTANCE.createTypeInt(maxSize), "result");
			Var i = division.newTempLocalVariable(
					IrFactory.eINSTANCE.createTypeInt(), "i");
			Var flipResult = division.newTempLocalVariable(
					IrFactory.eINSTANCE.createTypeInt(), "flipResult");

			Var remainder = division.newTempLocalVariable(
					IrFactory.eINSTANCE.createTypeInt(maxSize), "remainder");
			Var denom = division.newTempLocalVariable(
					IrFactory.eINSTANCE.createTypeInt(maxSize * 2), "denom");
			Var mask = division.newTempLocalVariable(
					IrFactory.eINSTANCE.createTypeInt(), "mask");
			Var numer = division.newTempLocalVariable(
					IrFactory.eINSTANCE.createTypeInt(maxSize * 2), "numer");

			// Division procedures block
			EList<Block> blocks = division.getBlocks();

			BlockBasic initBlock_0 = IrFactory.eINSTANCE.createBlockBasic();

			InstAssign initResult = IrFactory.eINSTANCE.createInstAssign(
					result, IrFactory.eINSTANCE.createExprInt(0));
			initBlock_0.add(initResult);

			InstAssign initFlipResult = IrFactory.eINSTANCE.createInstAssign(
					flipResult, IrFactory.eINSTANCE.createExprInt(0));
			initBlock_0.add(initFlipResult);

			// Add to the procedures blocks
			blocks.add(initBlock_0);

			// Check signs of num and den
			blocks.add(checkNegVar(num, flipResult));
			blocks.add(checkNegVar(den, flipResult));

			// Create a Block Basic for the following instructions
			BlockBasic initBlock_1 = IrFactory.eINSTANCE.createBlockBasic();

			// Assign num to remainder aka, remainder = num;
			InstAssign assign_0 = IrFactory.eINSTANCE.createInstAssign(
					remainder, num);
			initBlock_1.add(assign_0);

			// Cast Size depending on the maxSize
			BigInteger castSize = BigInteger.valueOf((1 << maxSize) - 1);

			// Assign denom the MIN_INT aka, denom = den & 0x000000FF;
			Expression and_0 = IrFactory.eINSTANCE.createExprBinary(
					IrFactory.eINSTANCE.createExprVar(den), OpBinary.BITAND,
					IrFactory.eINSTANCE.createExprInt(castSize),
					IrFactory.eINSTANCE.createTypeInt(den.getType()
							.getSizeInBits()));
			InstAssign assign_1 = IrFactory.eINSTANCE.createInstAssign(denom,
					and_0);
			initBlock_1.add(assign_1);

			// Assign mask value of 1 << (maxSize-1)
			Expression value_0 = IrFactory.eINSTANCE
					.createExprInt(1 << (maxSize - 1));
			InstAssign assign_2 = IrFactory.eINSTANCE.createInstAssign(mask,
					value_0);
			initBlock_1.add(assign_2);

			// Assign i = 0
			Expression intZero = IrFactory.eINSTANCE.createExprInt(0);
			InstAssign assign_3 = IrFactory.eINSTANCE.createInstAssign(i,
					intZero);
			initBlock_1.add(assign_3);

			blocks.add(initBlock_1);

			// Create Loop, loop until reach maxSize on i
			BlockWhile blockWhile = IrFactory.eINSTANCE.createBlockWhile();
			BlockBasic joinBlockWhile = IrFactory.eINSTANCE.createBlockBasic();
			blockWhile.setJoinBlock(joinBlockWhile);

			// Create Loop condition
			Expression loopCondition = IrFactory.eINSTANCE.createExprBinary(
					IrFactory.eINSTANCE.createExprVar(i), OpBinary.LT,
					IrFactory.eINSTANCE.createExprInt(maxSize),
					IrFactory.eINSTANCE.createTypeBool());
			// Set loop condition
			blockWhile.setCondition(loopCondition);

			// Create first, blockBasic of Loop
			// numer = (remainder & ((1 << maxSize) - 1)) >> (maxSize - i)));
			BlockBasic loopBlockBasic_0 = IrFactory.eINSTANCE
					.createBlockBasic();

			// Expression (remainder & ((1 << maxSize) - 1))
			Expression inner_E1 = IrFactory.eINSTANCE.createExprBinary(
					IrFactory.eINSTANCE.createExprVar(remainder),
					OpBinary.BITAND, IrFactory.eINSTANCE
							.createExprInt((1 << maxSize) - 1),
					IrFactory.eINSTANCE.createTypeInt(remainder.getType()
							.getSizeInBits()));
			// Expression (maxSize - i))
			Expression inner_E2 = IrFactory.eINSTANCE.createExprBinary(
					IrFactory.eINSTANCE.createExprInt(maxSize - 1),
					OpBinary.MINUS, IrFactory.eINSTANCE.createExprVar(i),
					IrFactory.eINSTANCE.createTypeInt());

			Expression inner_final = IrFactory.eINSTANCE.createExprBinary(
					inner_E1, OpBinary.SHIFT_RIGHT, inner_E2,
					IrFactory.eINSTANCE.createTypeInt(numer.getType()
							.getSizeInBits()));

			InstAssign assign_4 = IrFactory.eINSTANCE.createInstAssign(numer,
					inner_final);
			loopBlockBasic_0.add(assign_4);

			blockWhile.getBlocks().add(loopBlockBasic_0);

			// Create Second, blockIf Like the following code
			// if (numer >= denom) { result |= mask;
			// remainder = (remainder - (den << (maxSize - i)));}

			BlockIf loop_blockIf_0 = IrFactory.eINSTANCE.createBlockIf();
			BlockBasic joinBlockIf_0 = IrFactory.eINSTANCE.createBlockBasic();
			loop_blockIf_0.setJoinBlock(joinBlockIf_0);

			Expression blockIf_0Condition = IrFactory.eINSTANCE
					.createExprBinary(IrFactory.eINSTANCE.createExprVar(numer),
							OpBinary.GE,
							IrFactory.eINSTANCE.createExprVar(denom),
							IrFactory.eINSTANCE.createTypeBool());
			loop_blockIf_0.setCondition(blockIf_0Condition);

			// BlockBasic if block
			BlockBasic blockIf_OBlock = IrFactory.eINSTANCE.createBlockBasic();

			// Create expression, result |= mask;
			Expression bf_expr_0 = IrFactory.eINSTANCE.createExprBinary(
					IrFactory.eINSTANCE.createExprVar(result), OpBinary.BITOR,
					IrFactory.eINSTANCE.createExprVar(mask),
					IrFactory.eINSTANCE.createTypeInt(result.getType()
							.getSizeInBits()));
			InstAssign assign_5 = IrFactory.eINSTANCE.createInstAssign(result,
					bf_expr_0);
			blockIf_OBlock.add(assign_5);

			// Create assign, remainder = remainder - (den << ((maxSize-1) -
			// i));
			Expression bf_expr_inner_inner = IrFactory.eINSTANCE
					.createExprBinary(
							IrFactory.eINSTANCE.createExprInt(maxSize - 1),
							OpBinary.MINUS,
							IrFactory.eINSTANCE.createExprVar(i),
							IrFactory.eINSTANCE.createTypeInt());

			Expression bf_expr_inner = IrFactory.eINSTANCE.createExprBinary(
					IrFactory.eINSTANCE.createExprVar(den),
					OpBinary.SHIFT_LEFT, bf_expr_inner_inner,
					IrFactory.eINSTANCE.createTypeInt(maxSize));

			Expression bf_expr_1 = IrFactory.eINSTANCE.createExprBinary(
					IrFactory.eINSTANCE.createExprVar(remainder),
					OpBinary.MINUS, bf_expr_inner,
					IrFactory.eINSTANCE.createTypeInt(maxSize));

			InstAssign assign_6 = IrFactory.eINSTANCE.createInstAssign(
					remainder, bf_expr_1);
			blockIf_OBlock.add(assign_6);

			// Finally add blockIf_OBlock to the then blocks of blockIf_0
			loop_blockIf_0.getThenBlocks().add(blockIf_OBlock);

			blockWhile.getBlocks().add(loop_blockIf_0);

			// Create third block
			BlockBasic loopBlockBasic_1 = IrFactory.eINSTANCE
					.createBlockBasic();

			Expression bb_expr_0 = IrFactory.eINSTANCE.createExprBinary(
					IrFactory.eINSTANCE.createExprVar(mask),
					OpBinary.SHIFT_RIGHT, IrFactory.eINSTANCE.createExprInt(1),
					IrFactory.eINSTANCE.createTypeInt(mask.getType()
							.getSizeInBits()));
			Expression bb_expr_1 = IrFactory.eINSTANCE.createExprBinary(
					bb_expr_0, OpBinary.BITAND, IrFactory.eINSTANCE
							.createExprInt(0x7fffffff), IrFactory.eINSTANCE
							.createTypeInt(mask.getType().getSizeInBits()));

			InstAssign assign_7 = IrFactory.eINSTANCE.createInstAssign(mask,
					bb_expr_1);
			loopBlockBasic_1.add(assign_7);

			// i = i + 1;
			InstAssign iPlusPlus = IrFactory.eINSTANCE.createInstAssign(i,
					IrFactory.eINSTANCE.createExprBinary(
							IrFactory.eINSTANCE.createExprVar(i),
							OpBinary.PLUS,
							IrFactory.eINSTANCE.createExprInt(1),
							IrFactory.eINSTANCE.createTypeInt()));
			loopBlockBasic_1.add(iPlusPlus);

			// Finally add loopBlockBasic_1 to the blocks of blockWhile
			blockWhile.getBlocks().add(loopBlockBasic_1);

			// Add the Loop to the procedure block
			blocks.add(blockWhile);

			// Create final block If
			BlockIf blockIfSign = IrFactory.eINSTANCE.createBlockIf();
			BlockBasic joinBlockIfSign = IrFactory.eINSTANCE.createBlockBasic();
			blockIfSign.setJoinBlock(joinBlockIfSign);

			// blockIfSign condition, flipResult != 0
			Expression blockIfSignCondition = IrFactory.eINSTANCE
					.createExprBinary(
							IrFactory.eINSTANCE.createExprVar(flipResult),
							OpBinary.NE, IrFactory.eINSTANCE.createExprInt(0),
							IrFactory.eINSTANCE.createTypeBool());

			blockIfSign.setCondition(blockIfSignCondition);

			// Create a new BlockBasic
			BlockBasic blockIfSignBlock = IrFactory.eINSTANCE
					.createBlockBasic();

			Expression exprNegResult = IrFactory.eINSTANCE.createExprUnary(
					OpUnary.MINUS, IrFactory.eINSTANCE.createExprVar(result),
					IrFactory.eINSTANCE.createTypeInt(maxSize));

			InstAssign assign_8 = IrFactory.eINSTANCE.createInstAssign(result,
					exprNegResult);

			blockIfSignBlock.add(assign_8);
			// Add it to then Blocks
			blockIfSign.getThenBlocks().add(blockIfSignBlock);
			// Add the final if to the procedures blocks
			blocks.add(blockIfSign);

			// Create return instruction
			BlockBasic blockReturn = IrFactory.eINSTANCE.createBlockBasic();
			InstReturn instReturn = IrFactory.eINSTANCE
					.createInstReturn(IrFactory.eINSTANCE.createExprVar(result));
			blockReturn.add(instReturn);
			division.setReturnType(IrFactory.eINSTANCE.createTypeInt(maxSize));
			blocks.add(blockReturn);

			return division;
		}

		/**
		 * Returns true if an expression is an integer value
		 * 
		 * @param expr
		 * @return
		 */
		public boolean isConstant(Expression expr) {
			try {
				new ExpressionEvaluator().evaluateAsInteger(expr);
				return true;
			} catch (Exception e) {
				return false;
			}
		}
	}

	/**
	 * A Map of division functions with different max integer size
	 */
	private Map<Integer, Procedure> xronosDivision;

	@Override
	public Void caseActor(Actor actor) {
		this.actor = actor;

		for (Procedure proc : actor.getProcs()) {
			Substitutor substitutor = new Substitutor();
			substitutor.doSwitch(proc);
		}

		for (Action verifAction : actor.getActions()) {
			Substitutor substitutor = new Substitutor();
			substitutor.doSwitch(verifAction.getBody());
			substitutor.doSwitch(verifAction.getScheduler());
		}

		if (xronosDivision != null) {
			for (Procedure procedure : xronosDivision.values()) {
				actor.getProcs().add(procedure);
			}
		}
		return null;
	}
}