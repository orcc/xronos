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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sf.orc2hdl.design.ResourceCache;
import net.sf.orcc.ir.BlockBasic;
import net.sf.orcc.ir.BlockIf;
import net.sf.orcc.ir.Def;
import net.sf.orcc.ir.ExprBinary;
import net.sf.orcc.ir.ExprVar;
import net.sf.orcc.ir.Expression;
import net.sf.orcc.ir.InstPhi;
import net.sf.orcc.ir.Var;
import net.sf.orcc.ir.util.AbstractIrVisitor;

import org.eclipse.emf.ecore.EObject;

/**
 * This visitor finds the Input and Phi Vars of an IfBlock
 * 
 * @author Endri Bezati
 * 
 */
public class BranchIOFinder extends AbstractIrVisitor<Void> {

	private BlockBasic currentBlock = null;

	private BlockIf currentIfBlock = null;

	private List<Var> currentVars = new ArrayList<Var>();

	private Map<Var, List<Var>> phiMapVar = new HashMap<Var, List<Var>>();

	/** Design Resources **/
	private final ResourceCache resources;

	public BranchIOFinder(ResourceCache resources) {
		super(true);
		this.resources = resources;
	}

	@Override
	public Void caseBlockBasic(BlockBasic block) {
		// Visit only the instruction of the If block
		if (block.eContainer() == currentIfBlock) {
			currentBlock = block;
			super.caseBlockBasic(block);
		}
		return null;
	}

	@Override
	public Void caseBlockIf(BlockIf blockIf) {
		// Add decision condition Var
		currentIfBlock = blockIf;
		Expression condExpr = blockIf.getCondition();
		Var condVar = ((ExprVar) condExpr).getUse().getVariable();
		resources.addBranchDecisionInput(blockIf, condVar);

		// Visit thenBlock
		currentVars = new ArrayList<Var>();
		doSwitch(blockIf.getThenBlocks());
		resources.addBranchThenInput(blockIf, currentVars);

		// Visit elseBlock
		if (!blockIf.getElseBlocks().isEmpty()) {
			currentVars = new ArrayList<Var>();
			doSwitch(blockIf.getElseBlocks());
			resources.addBranchElseInput(blockIf, currentVars);
		}

		// Visit Phi
		phiMapVar = new HashMap<Var, List<Var>>();
		doSwitch(blockIf.getJoinBlock());
		resources.addBranchPhi(blockIf, phiMapVar);
		return null;
	}

	@Override
	public Void caseExprBinary(ExprBinary expr) {
		// Get e1 var and if it defined not in this visited block added as an
		// input
		Var varE1 = ((ExprVar) expr.getE1()).getUse().getVariable();
		if (definedInOtherBlock(varE1, currentBlock)) {
			if (!currentVars.contains(varE1)) {
				currentVars.add(varE1);
			}
		}
		// Get e2 var and if it defined not in this visited block added as an
		// input
		Var varE2 = ((ExprVar) expr.getE2()).getUse().getVariable();
		if (definedInOtherBlock(varE2, currentBlock)) {
			if (!currentVars.contains(varE2)) {
				currentVars.add(varE2);
			}
		}
		return null;
	}

	@Override
	public Void caseInstPhi(InstPhi phi) {
		List<Var> phiVars = new ArrayList<Var>();
		Var target = phi.getTarget().getVariable();

		for (Expression expr : phi.getValues()) {
			Var value = ((ExprVar) expr).getUse().getVariable();
			phiVars.add(value);
		}
		phiMapVar.put(target, phiVars);
		return null;
	}

	private Boolean definedInOtherBlock(Var var, BlockBasic block) {
		for (Def def : var.getDefs()) {
			EObject container = def.eContainer();
			while (!(container instanceof BlockBasic)) {
				container = container.eContainer();
			}
			if (container != block) {
				return true;
			}
		}
		return false;
	}

}
