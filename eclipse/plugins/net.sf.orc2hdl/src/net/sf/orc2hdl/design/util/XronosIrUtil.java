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

package net.sf.orc2hdl.design.util;

import java.util.List;

import net.sf.orcc.ir.Block;
import net.sf.orcc.ir.BlockBasic;
import net.sf.orcc.ir.BlockIf;
import net.sf.orcc.ir.ExprVar;
import net.sf.orcc.ir.Instruction;
import net.sf.orcc.ir.IrFactory;
import net.sf.orcc.ir.Var;

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

	public static BlockIf createBlockIf(Var condition, Block block) {
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

}
