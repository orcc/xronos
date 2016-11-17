/* 
 * XRONOS-EXELIXI
 * 
 * Copyright (C) 2011-2016 EPFL SCI STI MM
 *
 * This file is part of XRONOS-EXELIXI.
 *
 * XRONOS-EXELIXI is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * XRONOS-EXELIXI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with XRONOS-EXELIXI. If not, see <http://www.gnu.org/licenses/>.
 * 
 * Additional permission under GNU GPL version 3 section 7
 * 
 * If you modify this Program, or any covered work, by linking or combining it
 * with Eclipse (or a modified version of Eclipse or an Eclipse plugin or 
 * an Eclipse library), containing parts covered by the terms of the 
 * Eclipse Public License (EPL), the licensors of this Program grant you 
 * additional permission to convey the resulting work.  Corresponding Source 
 * for a non-source form of such a combination shall include the source code 
 * for the parts of Eclipse libraries used as well as that of the covered work.
 * 
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
import org.xronos.orcc.ir.InstPortWrite;
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

	public static BlockIf createBlockIf(Expression condition,
			List<Block> thenBlocks) {
		BlockIf blockIf = createBlockIf(condition);

		blockIf.getThenBlocks().addAll(thenBlocks);
		// Put an empty join Block
		BlockBasic emptyBlock = IrFactory.eINSTANCE.createBlockBasic();
		blockIf.setJoinBlock(emptyBlock);

		return blockIf;
	}

	public static BlockIf createBlockIf(Var condition, List<Block> thenBlocks) {
		BlockIf blockIf = createBlockIf(condition);

		blockIf.getThenBlocks().addAll(thenBlocks);
		// Put an empty join Block
		BlockBasic emptyBlock = IrFactory.eINSTANCE.createBlockBasic();
		blockIf.setJoinBlock(emptyBlock);

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

	public static BlockWhile createBlockWhile(Expression condition, Block body) {
		BlockWhile blockWhile = IrFactory.eINSTANCE.createBlockWhile();
		// Set the condition
		blockWhile.setCondition(condition);

		// Set body
		blockWhile.getBlocks().add(body);

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

	public static InstPortRead createInstPortRead(Var target, Port port, Boolean blocking) {
		InstPortRead instPortRead = XronosIrFactory.eINSTANCE
				.createInstPortRead();
		Def def = IrFactory.eINSTANCE.createDef(target);
		instPortRead.setTarget(def);
		instPortRead.setPort(port);
		instPortRead.setBlocking(blocking);
		return instPortRead;
	}

	public static InstPortWrite createInstPortWrite(Port port, Var source, Boolean blocking) {
		InstPortWrite instPortWrite = XronosIrFactory.eINSTANCE
				.createInstPortWrite();
		ExprVar exprVar = IrFactory.eINSTANCE.createExprVar(source);
		instPortWrite.setValue(exprVar);
		instPortWrite.setPort(port);
		instPortWrite.setBlocking(blocking);
		return instPortWrite;
	}

	public static InstPortStatus createInstPortStatus(Var target, Port port) {
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

	public static ExprBinary createExprBinaryMinus(Var var1, Integer value,
			Type type) {
		ExprVar e1 = IrFactory.eINSTANCE.createExprVar(var1);
		ExprInt e2 = IrFactory.eINSTANCE.createExprInt(value);

		return IrFactory.eINSTANCE.createExprBinary(e1, OpBinary.MINUS, e2,
				type);
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

	public static ExprBinary createExprBinaryNotEqual(Var var, Integer value) {
		ExprVar e1 = IrFactory.eINSTANCE.createExprVar(var);
		ExprInt e2 = IrFactory.eINSTANCE.createExprInt(value);

		return IrFactory.eINSTANCE.createExprBinary(e1, OpBinary.NE, e2,
				IrFactory.eINSTANCE.createTypeBool());
	}

	public static ExprBinary createExprBinaryNotEqual(Var var, Expression value) {
		ExprVar e1 = IrFactory.eINSTANCE.createExprVar(var);

		return IrFactory.eINSTANCE.createExprBinary(e1, OpBinary.NE, value,
				IrFactory.eINSTANCE.createTypeBool());
	}

	public static ExprBinary createExprBinaryEqual(Var var1, Var var2) {
		ExprVar e1 = IrFactory.eINSTANCE.createExprVar(var1);
		ExprVar e2 = IrFactory.eINSTANCE.createExprVar(var2);

		return IrFactory.eINSTANCE.createExprBinary(e1, OpBinary.EQ, e2,
				IrFactory.eINSTANCE.createTypeBool());
	}

	public static ExprBinary createExprBinaryLessThan(Var var1, Var var2) {
		ExprVar e1 = IrFactory.eINSTANCE.createExprVar(var1);
		ExprVar e2 = IrFactory.eINSTANCE.createExprVar(var2);

		return IrFactory.eINSTANCE.createExprBinary(e1, OpBinary.LT, e2,
				IrFactory.eINSTANCE.createTypeBool());
	}

	public static ExprBinary createExprBinaryLessThan(Var var, Integer value) {
		ExprVar e1 = IrFactory.eINSTANCE.createExprVar(var);
		ExprInt e2 = IrFactory.eINSTANCE.createExprInt(value);

		return IrFactory.eINSTANCE.createExprBinary(e1, OpBinary.LT, e2,
				IrFactory.eINSTANCE.createTypeBool());
	}

	public static Var getVarFromList(String name, List<Var> vars) {
		Var found = null;

		for (Var var : vars) {
			if (var.getName().equals(name)) {
				found = var;
				break;
			}
		}

		return found;
	}

}
