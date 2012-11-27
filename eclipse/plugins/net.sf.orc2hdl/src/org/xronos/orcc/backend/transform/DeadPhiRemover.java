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
package org.xronos.orcc.backend.transform;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.sf.orcc.ir.Block;
import net.sf.orcc.ir.BlockBasic;
import net.sf.orcc.ir.BlockIf;
import net.sf.orcc.ir.BlockWhile;
import net.sf.orcc.ir.ExprVar;
import net.sf.orcc.ir.InstPhi;
import net.sf.orcc.ir.Procedure;
import net.sf.orcc.ir.Use;
import net.sf.orcc.ir.Var;
import net.sf.orcc.ir.util.AbstractIrVisitor;
import net.sf.orcc.ir.util.IrUtil;

import org.eclipse.emf.ecore.EObject;
import org.xronos.orcc.design.visitors.BlockVars;

/**
 * This visitor finds and deletes the unused Phis on Block While
 * 
 * @author Endri Bezati
 * 
 */
public class DeadPhiRemover extends AbstractIrVisitor<Void> {

	Block currentBlock;
	Map<Block, List<Var>> usedVariables;
	Map<Block, List<Var>> defVariables;
	Map<Block, List<InstPhi>> phiToBeRemoved;
	private LinkedList<Block> nestedBlock;

	@Override
	public Void caseProcedure(Procedure procedure) {
		phiToBeRemoved = new HashMap<Block, List<InstPhi>>();
		usedVariables = new HashMap<Block, List<Var>>();
		defVariables = new HashMap<Block, List<Var>>();
		nestedBlock = new LinkedList<Block>();
		return super.caseProcedure(procedure);
	}

	@Override
	public Void caseBlockIf(BlockIf nodeIf) {
		doSwitch(nodeIf.getThenBlocks());
		doSwitch(nodeIf.getElseBlocks());
		return null;
	}

	@Override
	public Void caseBlockWhile(BlockWhile nodeWhile) {
		// Initialize Variables
		currentBlock = nodeWhile;
		List<InstPhi> rPhi = new ArrayList<InstPhi>();
		phiToBeRemoved.put(nodeWhile, rPhi);
		List<Block> parentBlocks = new ArrayList<Block>();

		if (nestedBlock.isEmpty()) {
			nestedBlock.addFirst(nodeWhile);
		} else {
			nestedBlock.add(nodeWhile);
		}

		// Test if the container is a procedure
		if (nodeWhile.eContainer() instanceof Procedure) {
			parentBlocks = new ArrayList<Block>(procedure.getBlocks());

		} else if (nodeWhile.eContainer() instanceof BlockWhile) {
			BlockWhile parent = (BlockWhile) nodeWhile.eContainer();
			parentBlocks = new ArrayList<Block>(parent.getBlocks());
		} else if (nodeWhile.eContainer() instanceof BlockIf) {
			BlockIf parent = (BlockIf) nodeWhile.eContainer();
			List<Block> thenBlocks = parent.getThenBlocks();
			List<Block> elseBlocks = parent.getElseBlocks();
			if (thenBlocks.contains(nodeWhile)) {
				parentBlocks = thenBlocks;
			} else if (elseBlocks.contains(nodeWhile)) {
				parentBlocks = elseBlocks;
			}

		}

		// Get the last index of nodeWhile
		int lastIndexOf = parentBlocks.lastIndexOf(nodeWhile);

		// Remove all blocks before the nodeWhile
		List<Block> usedBlocks = parentBlocks.subList(lastIndexOf + 1,
				parentBlocks.size());
		List<Block> defBlocks = parentBlocks.subList(0, lastIndexOf);
		usedVariables.put(nodeWhile, getVars(true, false, usedBlocks, null));
		defVariables.put(nodeWhile, getVars(true, defBlocks));

		super.caseBlockWhile(nodeWhile);

		// Delete the unused Phis
		for (InstPhi phi : phiToBeRemoved.get(nodeWhile)) {
			IrUtil.delete(phi);
		}

		// Fix currentBlock
		int indexOfLastBlock = nestedBlock.lastIndexOf(nodeWhile);
		if (indexOfLastBlock != 0) {
			if (nestedBlock.get(indexOfLastBlock - 1) == nodeWhile.eContainer()) {
				currentBlock = nestedBlock.get(indexOfLastBlock - 1);
			}
		}
		nestedBlock.remove(nodeWhile);
		return null;
	}

	@Override
	public Void caseInstPhi(InstPhi phi) {
		Var target = phi.getTarget().getVariable();

		// Get the Phi Vars, First Value Group 0, Second Value Group 1
		Var valueZero = ((ExprVar) phi.getValues().get(0)).getUse()
				.getVariable();
		Var valueOne = ((ExprVar) phi.getValues().get(0)).getUse()
				.getVariable();

		if (target.getUses().isEmpty() || valueZero.getDefs().isEmpty()
				|| valueOne.getDefs().isEmpty()) {
			// This target is not used anywhere it should be deleted
			phiToBeRemoved.get(currentBlock).add(phi);
		} else {
			if (!usedVariables.get(currentBlock).contains(target)) {
				if (usedOnlyInPhi(target)
						&& !defVariables.get(currentBlock).contains(valueZero)) {
					phiToBeRemoved.get(currentBlock).add(phi);
				}
			}
		}
		super.caseInstPhi(phi);
		return null;
	}

	private List<Var> getVars(Boolean input, Boolean deepSearch,
			List<Block> blocks, Block phiBlock) {
		List<Var> vars = new ArrayList<Var>();
		if (!blocks.isEmpty()) {
			for (Block block : blocks) {
				Set<Var> blkVars = new BlockVars(input, deepSearch, blocks,
						phiBlock).doSwitch(block);
				if (blkVars != null)
					for (Var var : blkVars) {
						if (!vars.contains(var)) {
							vars.add(var);
						}
					}
			}
		}
		return vars;
	}

	private List<Var> getVars(Boolean definedVar, List<Block> blocks) {
		List<Var> vars = new ArrayList<Var>();
		if (!blocks.isEmpty()) {
			for (Block block : blocks) {
				Set<Var> blkVars = new BlockVars(definedVar, null)
						.doSwitch(block);
				if (blkVars != null)
					for (Var var : blkVars) {
						if (!vars.contains(var)) {
							vars.add(var);
						}
					}
			}
		}
		return vars;
	}

	private Boolean usedOnlyInPhi(Var var) {
		Map<Use, Boolean> useMap = new HashMap<Use, Boolean>();
		for (Use use : var.getUses()) {
			EObject container = use.eContainer();
			// Get the BlockBasic container
			while (!(container instanceof Block)) {
				container = container.eContainer();
				if (container instanceof InstPhi) {
					useMap.put(use, true);
					break;
				}
			}
			if (container instanceof BlockBasic) {
				useMap.put(use, false);
			}

			if (container instanceof BlockIf) {
				useMap.put(use, false);
			}
			if (container instanceof BlockWhile) {
				useMap.put(use, false);
			}
		}

		if (!useMap.containsValue(false)) {
			return true;
		} else {
			return false;
		}

	}

}
