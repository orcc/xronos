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
package net.sf.orc2hdl.backend.transform;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.sf.orcc.ir.Block;
import net.sf.orcc.ir.BlockBasic;
import net.sf.orcc.ir.BlockIf;
import net.sf.orcc.ir.BlockWhile;
import net.sf.orcc.ir.ExprBinary;
import net.sf.orcc.ir.ExprVar;
import net.sf.orcc.ir.Expression;
import net.sf.orcc.ir.InstAssign;
import net.sf.orcc.ir.InstLoad;
import net.sf.orcc.ir.InstPhi;
import net.sf.orcc.ir.InstStore;
import net.sf.orcc.ir.Procedure;
import net.sf.orcc.ir.Var;
import net.sf.orcc.ir.util.AbstractIrVisitor;

/**
 * 
 * @author Endri Bezati
 * 
 */
public class DeadPhiRemover extends AbstractIrVisitor<Void> {

	private class BlockBasicVars extends AbstractIrVisitor<Set<Var>> {

		private Set<Var> blockBasicVars;

		public BlockBasicVars() {
			super(true);
		}

		@Override
		public Set<Var> caseBlockBasic(BlockBasic block) {
			blockBasicVars = new HashSet<Var>();
			super.caseBlockBasic(block);
			return blockBasicVars;
		}

		@Override
		public Set<Var> caseExprBinary(ExprBinary expr) {
			Var varE1 = ((ExprVar) expr.getE1()).getUse().getVariable();
			blockBasicVars.add(varE1);
			Var varE2 = ((ExprVar) expr.getE2()).getUse().getVariable();
			blockBasicVars.add(varE2);
			return null;
		}

		@Override
		public Set<Var> caseInstAssign(InstAssign assign) {
			// Take the target
			Var target = assign.getTarget().getVariable();
			blockBasicVars.add(target);
			// Now visit the rest
			super.caseInstAssign(assign);
			return null;
		}

		@Override
		public Set<Var> caseInstLoad(InstLoad load) {
			Var loadIndexVar = null;
			List<Expression> indexes = load.getIndexes();
			for (Expression expr : new ArrayList<Expression>(indexes)) {
				loadIndexVar = ((ExprVar) expr).getUse().getVariable();
				blockBasicVars.add(loadIndexVar);
			}
			return null;
		}

		@Override
		public Set<Var> caseInstStore(InstStore store) {
			Var loadIndexVar = null;
			List<Expression> indexes = store.getIndexes();
			for (Expression expr : new ArrayList<Expression>(indexes)) {
				loadIndexVar = ((ExprVar) expr).getUse().getVariable();
				blockBasicVars.add(loadIndexVar);
			}
			return null;
		}

	}

	private class JoinBlockAssignVars extends AbstractIrVisitor<Set<Var>> {

		private Set<Var> assignVars;

		public JoinBlockAssignVars() {
			super(true);
		}

		@Override
		public Set<Var> caseBlockBasic(BlockBasic block) {
			assignVars = new HashSet<Var>();
			super.caseBlockBasic(block);
			return assignVars;
		}

		@Override
		public Set<Var> caseExprBinary(ExprBinary expr) {
			Var varE1 = ((ExprVar) expr.getE1()).getUse().getVariable();
			assignVars.add(varE1);
			Var varE2 = ((ExprVar) expr.getE2()).getUse().getVariable();
			assignVars.add(varE2);
			return null;
		}

		@Override
		public Set<Var> caseInstAssign(InstAssign assign) {
			Var target = assign.getTarget().getVariable();
			assignVars.add(target);
			super.caseInstAssign(assign);
			return null;
		}

	}

	Map<BlockWhile, Set<Var>> blockWhileUsedVars;
	BlockWhile currentBlock;
	private LinkedList<Block> nestedBlock;

	Map<BlockWhile, Set<InstPhi>> phiToBeRemoved = new HashMap<BlockWhile, Set<InstPhi>>();

	@Override
	public Void caseBlockIf(BlockIf nodeIf) {
		// Visit only then and else blocks, not the join Block
		doSwitch(nodeIf.getThenBlocks());
		doSwitch(nodeIf.getElseBlocks());
		return null;
	}

	@Override
	public Void caseBlockWhile(BlockWhile nodeWhile) {
		currentBlock = nodeWhile;
		if (nestedBlock.isEmpty()) {
			nestedBlock.addFirst(nodeWhile);
		} else {
			nestedBlock.add(nodeWhile);
		}

		Set<InstPhi> listPhi = new HashSet<InstPhi>();
		phiToBeRemoved.put(nodeWhile, listPhi);

		Set<Var> joinVars = new JoinBlockAssignVars().doSwitch(nodeWhile
				.getJoinBlock());
		blockWhileUsedVars.put(nodeWhile, joinVars);

		// Get the var of its container
		if (nodeWhile.eContainer() instanceof Procedure) {
			Procedure pContainer = (Procedure) nodeWhile.eContainer();
			// Get the blocks of the procedure
			List<Block> procedureBlocks = pContainer.getBlocks();

			// Now take all the blocks that after this nodeWhile
			int index = procedureBlocks.indexOf(nodeWhile);
			int maxIndex = procedureBlocks.size();
			//
			List<Block> blockToTakeVars = new ArrayList<Block>();
			if ((index + 1) == maxIndex) {
				// No other blocks! All Phi except the one used for the
				// condition should be removed
			} else {
				// Take the next Blocks Vars
				for (int i = index + 1; i < maxIndex; i++) {
					blockToTakeVars.add(procedureBlocks.get(i));
					Block blk = procedureBlocks.get(i);
					BlockBasicVars vis = new BlockBasicVars();
					blockWhileUsedVars.get(nodeWhile).addAll(vis.doSwitch(blk));
				}
			}
		} else if (nodeWhile.eContainer() instanceof BlockWhile) {
			BlockWhile wContainer = (BlockWhile) nodeWhile.eContainer();
			List<Block> procedureBlocks = wContainer.getBlocks();

			// Now take all the blocks that after this nodeWhile
			int index = procedureBlocks.indexOf(nodeWhile);
			int maxIndex = procedureBlocks.size();
			//
			List<Block> blockToTakeVars = new ArrayList<Block>();
			if ((index + 1) == maxIndex) {
				// No other blocks! All Phi except the one used for the
				// condition should be removed
			} else {
				// Take the next Blocks Vars
				for (int i = index + 1; i < maxIndex; i++) {
					blockToTakeVars.add(procedureBlocks.get(i));
					Block blk = procedureBlocks.get(i);
					BlockBasicVars vis = new BlockBasicVars();
					blockWhileUsedVars.get(nodeWhile).addAll(vis.doSwitch(blk));
				}
			}
		}

		// Visit JoinBlock
		doSwitch(nodeWhile.getJoinBlock());

		// Visit his child Blocks
		doSwitch(nodeWhile.getBlocks());

		// Fix currentBlock
		int indexOfLastBlock = nestedBlock.lastIndexOf(nodeWhile);
		if (indexOfLastBlock != 0) {
			if (nestedBlock.get(indexOfLastBlock - 1) == nodeWhile.eContainer()) {
				currentBlock = (BlockWhile) nestedBlock
						.get(indexOfLastBlock - 1);
			}
		}
		nestedBlock.remove(nodeWhile);
		// Remove the instructions
		for (InstPhi instPhi : phiToBeRemoved.get(nodeWhile)) {
			if (nodeWhile.getJoinBlock().getInstructions().contains(instPhi)) {
				nodeWhile.getJoinBlock().getInstructions().remove(instPhi);
			}
		}

		return null;
	}

	@Override
	public Void caseInstPhi(InstPhi phi) {

		Var target = phi.getTarget().getVariable();
		if (!blockWhileUsedVars.get(currentBlock).contains(target)) {
			phiToBeRemoved.get(currentBlock).add(phi);
		}

		return null;
	}

	@Override
	public Void caseProcedure(Procedure procedure) {
		nestedBlock = new LinkedList<Block>();
		blockWhileUsedVars = new HashMap<BlockWhile, Set<Var>>();
		return super.caseProcedure(procedure);
	}

}
