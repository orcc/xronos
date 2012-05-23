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

package net.sf.orc2hdl.design.visitors;

import net.sf.orc2hdl.design.ResourceCache;
import net.sf.orcc.ir.Block;
import net.sf.orcc.ir.BlockBasic;
import net.sf.orcc.ir.BlockIf;
import net.sf.orcc.ir.ExprVar;
import net.sf.orcc.ir.Expression;
import net.sf.orcc.ir.InstAssign;
import net.sf.orcc.ir.Var;
import net.sf.orcc.ir.util.AbstractIrVisitor;

/**
 * This visitor finds the Input and Output Vars of an IfBlock
 * 
 * @author Endri Bezati
 * 
 */
public class BranchIOFinder extends AbstractIrVisitor<Void> {

	/** Design Resources **/
	private final ResourceCache resources;

	private Block currentIfBlock = null;

	private final Block currentBlock = null;

	public BranchIOFinder(ResourceCache resources) {
		this.resources = resources;
	}

	@Override
	public Void caseBlockIf(BlockIf blockIf) {
		currentIfBlock = blockIf;
		Expression condExpr = blockIf.getCondition();
		Var condVar = ((ExprVar) condExpr).getUse().getVariable();
		resources.addBlockIfInput(blockIf, condVar);
		// Visit thenBlock
		doSwitch(blockIf.getThenBlocks());
		// Visit elseBlock
		doSwitch(blockIf.getElseBlocks());
		return null;
	}

	@Override
	public Void caseBlockBasic(BlockBasic block) {
		// Visit only the instruction of the If block
		if (block.eContainer() == currentIfBlock) {
			super.caseBlockBasic(block);
		}
		return null;
	}

	@Override
	public Void caseInstAssign(InstAssign assign) {
		return null;
	}

}
