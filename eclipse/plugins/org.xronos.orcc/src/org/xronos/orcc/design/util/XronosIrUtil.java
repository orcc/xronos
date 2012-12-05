/*
 * Copyright (c) 2012, Ecole Polytechnique Fédérale de Lausanne
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

package org.xronos.orcc.design.util;

import java.util.List;

import net.sf.orcc.df.Port;
import net.sf.orcc.ir.Block;
import net.sf.orcc.ir.BlockBasic;
import net.sf.orcc.ir.BlockIf;
import net.sf.orcc.ir.BlockWhile;
import net.sf.orcc.ir.Def;
import net.sf.orcc.ir.ExprBinary;
import net.sf.orcc.ir.ExprBool;
import net.sf.orcc.ir.ExprInt;
import net.sf.orcc.ir.ExprVar;
import net.sf.orcc.ir.Expression;
import net.sf.orcc.ir.InstStore;
import net.sf.orcc.ir.Instruction;
import net.sf.orcc.ir.IrFactory;
import net.sf.orcc.ir.OpBinary;
import net.sf.orcc.ir.Type;
import net.sf.orcc.ir.Var;

import org.xronos.orcc.ir.InstPortRead;
import org.xronos.orcc.ir.InstPortStatus;
import org.xronos.orcc.ir.XronosIrFactory;

/**
 * This class contains several methods for manipulating the Orcc Ir EMF model
 * 
 * @author Endri Bezati
 * 
 */
public class XronosIrUtil {

	public static BlockIf createBlockIf(Var condition) {
		BlockIf blockIf = IrFactory.eINSTANCE.createBlockIf();
		ExprVar evCondition = IrFactory.eINSTANCE.createExprVar(condition);
		blockIf.setCondition(evCondition);

		// Put an empty join Block
		BlockBasic emptyBlock = IrFactory.eINSTANCE.createBlockBasic();
		blockIf.setJoinBlock(emptyBlock);
		return blockIf;
	}

	public static BlockIf createBlockIf(Expression condition) {
		BlockIf blockIf = IrFactory.eINSTANCE.createBlockIf();
		blockIf.setCondition(condition);

		// Put an empty join Block
		BlockBasic emptyBlock = IrFactory.eINSTANCE.createBlockBasic();
		blockIf.setJoinBlock(emptyBlock);
		return blockIf;
	}

	public static BlockIf createBlockIf(Var condition, Block singleThenBlock) {
		BlockIf blockIf = createBlockIf(condition);

		blockIf.getThenBlocks().add(singleThenBlock);
		return blockIf;
	}

	public static BlockIf createBlockIf(Var condition, Block singleThenBlock,
			Block singleElseBlock) {
		BlockIf blockIf = createBlockIf(condition);

		blockIf.getThenBlocks().add(singleThenBlock);
		blockIf.getElseBlocks().add(singleElseBlock);
		return blockIf;
	}

	public static BlockIf createBlockIf(Expression condition, Block block) {
		BlockIf blockIf = createBlockIf(condition);

		blockIf.getThenBlocks().add(block);
		return blockIf;
	}

	public static BlockIf createBlockIf(Var condition,
			Instruction singleThenInstruction) {
		BlockIf blockIf = createBlockIf(condition);

		BlockBasic thenBlock = IrFactory.eINSTANCE.createBlockBasic();
		thenBlock.add(singleThenInstruction);

		blockIf.getThenBlocks().add(thenBlock);
		return blockIf;
	}

	public static BlockIf createBlockIf(Var condition,
			List<Instruction> thenInstructions) {
		BlockIf blockIf = createBlockIf(condition);

		BlockBasic thenBlock = IrFactory.eINSTANCE.createBlockBasic();
		for (Instruction instruction : thenInstructions) {
			thenBlock.add(instruction);
		}
		blockIf.getThenBlocks().add(thenBlock);
		return blockIf;
	}

	public static BlockIf createBlockIf(Expression condition,
			List<Instruction> thenInstructions) {
		BlockIf blockIf = createBlockIf(condition);

		BlockBasic thenBlock = IrFactory.eINSTANCE.createBlockBasic();
		for (Instruction instruction : thenInstructions) {
			thenBlock.add(instruction);
		}
		blockIf.getThenBlocks().add(thenBlock);
		return blockIf;
	}

	public static BlockIf createBlockIf(Var condition,
			List<Instruction> thenInstructions,
			List<Instruction> elseInstructions) {
		BlockIf blockIf = createBlockIf(condition, thenInstructions);

		BlockBasic elseBlock = IrFactory.eINSTANCE.createBlockBasic();
		for (Instruction instruction : thenInstructions) {
			elseBlock.add(instruction);
		}
		blockIf.getElseBlocks().add(elseBlock);
		return blockIf;
	}

	public static BlockIf createBlockIf(Expression condition,
			List<Instruction> thenInstructions,
			List<Instruction> elseInstructions) {
		BlockIf blockIf = createBlockIf(condition, thenInstructions);

		BlockBasic elseBlock = IrFactory.eINSTANCE.createBlockBasic();
		for (Instruction instruction : elseInstructions) {
			elseBlock.add(instruction);
		}
		blockIf.getElseBlocks().add(elseBlock);
		return blockIf;
	}

	public static BlockWhile createBlockWhile(Expression condition,
			List<Block> body) {
		BlockWhile blockWhile = IrFactory.eINSTANCE.createBlockWhile();
		// Set the condition
		blockWhile.setCondition(condition);

		// Set body
		blockWhile.getBlocks().addAll(body);

		// Set empty join Block
		blockWhile.setJoinBlock(IrFactory.eINSTANCE.createBlockBasic());

		return blockWhile;
	}

	public static BlockWhile createTrueBlockWhile(List<Block> body) {
		ExprBool exprTrue = IrFactory.eINSTANCE.createExprBool(true);

		return createBlockWhile(exprTrue, body);
	}

	public static InstStore createInstStore(Var target, Boolean value) {
		ExprBool exprBoool = IrFactory.eINSTANCE.createExprBool(value);
		InstStore instStore = IrFactory.eINSTANCE.createInstStore(target,
				exprBoool);
		return instStore;
	}

	public static InstPortRead creaInstPortRead(Var target, Port port) {
		InstPortRead instPortRead = XronosIrFactory.eINSTANCE
				.createInstPortRead();
		Def def = IrFactory.eINSTANCE.createDef(target);
		instPortRead.setTarget(def);
		instPortRead.setPort(port);
		return instPortRead;
	}

	public static InstPortStatus creaInstPortStatus(Var target, Port port) {
		InstPortStatus instPortStatus = XronosIrFactory.eINSTANCE
				.createInstPortStatus();
		Def def = IrFactory.eINSTANCE.createDef(target);
		instPortStatus.setTarget(def);
		instPortStatus.setPort(port);
		return instPortStatus;
	}

	public static ExprBinary createExprBinaryPlus(Var var1, Var var2, Type type) {
		ExprVar e1 = IrFactory.eINSTANCE.createExprVar(var1);
		ExprVar e2 = IrFactory.eINSTANCE.createExprVar(var2);

		return IrFactory.eINSTANCE
				.createExprBinary(e1, OpBinary.PLUS, e2, type);
	}

	public static ExprBinary createExprBinaryPlus(Var var1, Integer value,
			Type type) {
		ExprVar e1 = IrFactory.eINSTANCE.createExprVar(var1);
		ExprInt e2 = IrFactory.eINSTANCE.createExprInt(value);

		return IrFactory.eINSTANCE
				.createExprBinary(e1, OpBinary.PLUS, e2, type);
	}

	public static ExprBinary createExprBinaryBitAnd(Expression e1,
			Integer value, Type type) {
		ExprInt e2 = IrFactory.eINSTANCE.createExprInt(value);

		return IrFactory.eINSTANCE.createExprBinary(e1, OpBinary.BITAND, e2,
				type);
	}

	public static ExprBinary createExprBinaryLogicAnd(Var var1, Var var2) {
		ExprVar e1 = IrFactory.eINSTANCE.createExprVar(var1);
		ExprVar e2 = IrFactory.eINSTANCE.createExprVar(var2);
		return IrFactory.eINSTANCE.createExprBinary(e1, OpBinary.LOGIC_AND, e2,
				IrFactory.eINSTANCE.createTypeBool());
	}

	public static ExprBinary createExprBinaryLogicAnd(Expression e1,
			Expression e2) {
		return IrFactory.eINSTANCE.createExprBinary(e1, OpBinary.LOGIC_AND, e2,
				IrFactory.eINSTANCE.createTypeBool());
	}

	public static ExprBinary createExprBinaryEqual(Var var, Integer value) {
		ExprVar e1 = IrFactory.eINSTANCE.createExprVar(var);
		ExprInt e2 = IrFactory.eINSTANCE.createExprInt(value);

		return IrFactory.eINSTANCE.createExprBinary(e1, OpBinary.EQ, e2,
				IrFactory.eINSTANCE.createTypeBool());
	}

	public static ExprBinary createExprBinaryEqual(Var var1, Var var2) {
		ExprVar e1 = IrFactory.eINSTANCE.createExprVar(var1);
		ExprVar e2 = IrFactory.eINSTANCE.createExprVar(var2);

		return IrFactory.eINSTANCE.createExprBinary(e1, OpBinary.EQ, e2,
				IrFactory.eINSTANCE.createTypeBool());
	}

	public static ExprBinary createExprBinaryLessThan(Var var, Integer value) {
		ExprVar e1 = IrFactory.eINSTANCE.createExprVar(var);
		ExprInt e2 = IrFactory.eINSTANCE.createExprInt(value);

		return IrFactory.eINSTANCE.createExprBinary(e1, OpBinary.LT, e2,
				IrFactory.eINSTANCE.createTypeBool());
	}

}
