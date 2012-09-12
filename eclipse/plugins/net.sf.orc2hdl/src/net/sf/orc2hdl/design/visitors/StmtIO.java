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
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.sf.orc2hdl.design.ResourceCache;
import net.sf.orcc.ir.Block;
import net.sf.orcc.ir.BlockIf;
import net.sf.orcc.ir.BlockWhile;
import net.sf.orcc.ir.ExprVar;
import net.sf.orcc.ir.Expression;
import net.sf.orcc.ir.InstPhi;
import net.sf.orcc.ir.Var;
import net.sf.orcc.ir.util.AbstractIrVisitor;

/**
 * This visitor figures out the Input and Output of a Branch or a While for an
 * actor in a SSA form. As for the inputs it is using the BlockVars Visitor and
 * for the outputs it is using the PH
 * 
 * @author Endri Bezati
 * 
 */
public class StmtIO extends AbstractIrVisitor<Void> {

	/** Design Resources **/
	private ResourceCache cache;

	/** The current visited If Block **/
	private Block currentBlock;

	/** Map of a Branch Else Block Input Variables **/
	private Map<Block, List<Var>> elseInputs;

	/** Map of a Branch Else Block Output Variables **/
	private Map<Block, List<Var>> elseOutputs;

	/** Map containing the join node **/
	private Map<Block, Map<Var, List<Var>>> joinVarMap;

	/** Map of a LoopBody Block Input Variables **/
	private Map<Block, List<Var>> loopBodyInputs;

	/** Map of a LoopBody Block Output Variables **/
	private Map<Block, List<Var>> loopBodyOutputs;

	private LinkedList<Block> nestedBlock;

	/** Map of a Decision Module Input Variables **/
	private Map<Block, List<Var>> stmDecision;

	/** Map of a Loop Module Input Variables **/
	private Map<Block, List<Var>> stmInputs;

	/** Map of a Loop Module Output Variables **/
	private Map<Block, List<Var>> stmOutputs;

	/** Map of a Branch Then Block Input Variables **/
	private Map<Block, List<Var>> thenInputs;

	/** Map of a Branch Then Block Output Variables **/
	private Map<Block, List<Var>> thenOutputs;

	private Set<Block> visitedBlocks;

	public StmtIO(ResourceCache cache) {
		super(true);
		this.cache = cache;
		// Initialize
		thenInputs = new HashMap<Block, List<Var>>();
		thenOutputs = new HashMap<Block, List<Var>>();
		elseInputs = new HashMap<Block, List<Var>>();
		elseOutputs = new HashMap<Block, List<Var>>();
		loopBodyInputs = new HashMap<Block, List<Var>>();
		loopBodyOutputs = new HashMap<Block, List<Var>>();
		stmDecision = new HashMap<Block, List<Var>>();
		stmInputs = new HashMap<Block, List<Var>>();
		stmOutputs = new HashMap<Block, List<Var>>();
		joinVarMap = new HashMap<Block, Map<Var, List<Var>>>();
		visitedBlocks = new HashSet<Block>();
		nestedBlock = new LinkedList<Block>();
	}

	@Override
	public Void caseBlockIf(BlockIf nodeIf) {
		currentBlock = nodeIf;
		if (nestedBlock.isEmpty()) {
			nestedBlock.addFirst(nodeIf);
		} else {
			nestedBlock.add(nodeIf);
		}

		// Initialize the maps
		stmDecision.put(nodeIf, new ArrayList<Var>());
		thenInputs.put(nodeIf, new ArrayList<Var>());
		thenOutputs.put(nodeIf, new ArrayList<Var>());
		elseInputs.put(nodeIf, new ArrayList<Var>());
		elseOutputs.put(nodeIf, new ArrayList<Var>());
		stmInputs.put(nodeIf, new ArrayList<Var>());
		stmOutputs.put(nodeIf, new ArrayList<Var>());
		// Get branch Condition
		Expression condExpr = nodeIf.getCondition();
		Var condVar = ((ExprVar) condExpr).getUse().getVariable();
		stmDecision.get(nodeIf).add(condVar);
		stmInputs.get(nodeIf).add(condVar);

		// Visit Join Block, get the Branch the Output
		joinVarMap.put(nodeIf, new HashMap<Var, List<Var>>());

		doSwitch(nodeIf.getJoinBlock());

		// Visit the Then Block
		doSwitch(nodeIf.getThenBlocks());
		thenInputs.get(nodeIf).addAll(
				getVars(true, false, nodeIf.getThenBlocks(),
						nodeIf.getJoinBlock()));
		thenOutputs.get(nodeIf).addAll(
				getVars(false, false, nodeIf.getThenBlocks(),
						nodeIf.getJoinBlock()));

		// Visit the Else Block
		/** Visit Else Block **/
		currentBlock = nodeIf;
		if (!nodeIf.getElseBlocks().isEmpty()) {
			// Visit the Then Block
			doSwitch(nodeIf.getElseBlocks());
			elseInputs.get(nodeIf).addAll(
					getVars(true, false, nodeIf.getThenBlocks(),
							nodeIf.getJoinBlock()));
			elseOutputs.get(nodeIf).addAll(
					getVars(false, false, nodeIf.getThenBlocks(),
							nodeIf.getJoinBlock()));
		}

		resovleStmIO(nodeIf);
		// Add to Stm input
		stmAddVars(nodeIf, stmInputs, thenInputs);
		stmAddVars(nodeIf, stmInputs, elseInputs);

		// Add to cache
		cache.addBranch(nodeIf, stmDecision, stmInputs, stmOutputs, thenInputs,
				thenOutputs, elseInputs, elseOutputs, joinVarMap);
		// Fix currentBlock
		int indexOfLastBlock = nestedBlock.lastIndexOf(nodeIf);
		if (indexOfLastBlock != 0) {
			if (nestedBlock.get(indexOfLastBlock - 1) == nodeIf.eContainer()) {
				currentBlock = nestedBlock.get(indexOfLastBlock - 1);
			}
		}
		visitedBlocks.add(nodeIf);
		nestedBlock.remove(nodeIf);
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

		// Initialize the maps
		stmDecision.put(nodeWhile, new ArrayList<Var>());
		stmInputs.put(nodeWhile, new ArrayList<Var>());
		stmOutputs.put(nodeWhile, new ArrayList<Var>());
		loopBodyInputs.put(nodeWhile, new ArrayList<Var>());
		loopBodyOutputs.put(nodeWhile, new ArrayList<Var>());

		/** Visit the Join Block **/
		joinVarMap.put(nodeWhile, new HashMap<Var, List<Var>>());

		doSwitch(nodeWhile.getJoinBlock());
		// Get decision inputs
		if (stmDecision.get(nodeWhile).isEmpty()) {
			Var condVar = ((ExprVar) nodeWhile.getCondition()).getUse()
					.getVariable();
			stmDecision.get(nodeWhile).add(condVar);
		}
		// Get Other Decision inputs
		List<Var> otherDecisionVars = otherStmDecisionVars(nodeWhile
				.getJoinBlock());
		stmDecision.get(nodeWhile).addAll(otherDecisionVars);

		// Visit other Blocks
		doSwitch(nodeWhile.getBlocks());

		// Now Find its Inputs and Outputs
		List<Var> blkInputs = getVars(true, false, nodeWhile.getBlocks(),
				nodeWhile.getJoinBlock());
		List<Var> blkOutputs = getVars(false, false, nodeWhile.getBlocks(),
				nodeWhile.getJoinBlock());
		resovleStmIO(nodeWhile);
		resolveWhileIO(nodeWhile, blkInputs, blkOutputs, joinVarMap,
				loopBodyInputs, loopBodyOutputs, stmInputs, stmOutputs);

		// Check if the Decision variables are external
		for (Var var : otherDecisionVars) {
			if (!stmInputs.get(nodeWhile).contains(var)
					&& !loopBodyInputs.get(nodeWhile).contains(var)) {
				stmInputs.get(nodeWhile).add(var);
				loopBodyInputs.get(nodeWhile).add(var);
			}
		}

		// Add to cache
		cache.addLoop(nodeWhile, stmDecision, stmInputs, stmOutputs,
				loopBodyInputs, loopBodyOutputs, joinVarMap);

		// Fix currentBlock
		int indexOfLastBlock = nestedBlock.lastIndexOf(nodeWhile);
		if (indexOfLastBlock != 0) {
			if (nestedBlock.get(indexOfLastBlock - 1) == nodeWhile.eContainer()) {
				currentBlock = nestedBlock.get(indexOfLastBlock - 1);
			}
		}
		visitedBlocks.add(nodeWhile);
		nestedBlock.remove(nodeWhile);
		return null;
	}

	@Override
	public Void caseInstPhi(InstPhi phi) {
		List<Var> phiValues = new ArrayList<Var>();
		Var target = phi.getTarget().getVariable();

		// Get the Phi Vars, First Value Group 0, Second Value Group 1
		for (Expression expr : phi.getValues()) {
			Var value = ((ExprVar) expr).getUse().getVariable();
			phiValues.add(value);
		}

		// Create the Output
		stmOutputs.get(currentBlock).add(target);
		Var valueZero = phiValues.get(0);
		Var valueOne = phiValues.get(1);

		if (currentBlock instanceof BlockIf) {
			if (((BlockIf) currentBlock).getThenBlocks().isEmpty()) {
				stmInputs.get(currentBlock).add(valueZero);
			} else {
				thenOutputs.get(currentBlock).add(valueZero);
			}

			if (((BlockIf) currentBlock).getElseBlocks().isEmpty()) {
				stmInputs.get(currentBlock).add(valueOne);
			} else {
				elseOutputs.get(currentBlock).add(valueOne);
			}

		} else if (currentBlock instanceof BlockWhile) {
			loopBodyInputs.get(currentBlock).add(target);
			loopBodyOutputs.get(currentBlock).add(valueOne);
			stmInputs.get(currentBlock).add(valueZero);
		}

		// Fill up the JoinVar Map
		joinVarMap.get(currentBlock).put(target, phiValues);
		return null;
	}

	private List<Var> getVars(Boolean input, Boolean deepSearch,
			List<Block> blocks, Block phiBlock) {
		List<Var> vars = new ArrayList<Var>();
		if (!blocks.isEmpty()) {
			for (Block block : blocks) {
				Set<Var> blkVars = new BlockVars(input, deepSearch, blocks,
						phiBlock).doSwitch(block);
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
				Set<Var> blkVars = new BlockVars(definedVar).doSwitch(block);
				for (Var var : blkVars) {
					if (!vars.contains(var)) {
						vars.add(var);
					}
				}
			}
		}
		return vars;
	}

	private void resovleStmIO(Block block) {
		if (block instanceof BlockIf) {
			BlockIf blockIf = (BlockIf) block;

			// Visit then blocks for IO
			List<Block> definedThenBlocks = blockIf.getThenBlocks();
			List<Var> definedThenVar = getVars(true, definedThenBlocks);
			for (Block childBlock : blockIf.getThenBlocks()) {
				if (childBlock.isBlockIf() || childBlock.isBlockWhile()) {
					List<Var> childInputs = stmInputs.get(childBlock);
					for (Var var : childInputs) {
						if (!definedThenVar.contains(var)) {
							thenInputs.get(block).add(var);
							stmInputs.get(block).add(var);
						}
					}
				}
			}

			// Visit else blocks for IO
			List<Block> definedElseBlocks = blockIf.getElseBlocks();
			List<Var> definedElseVar = getVars(true, definedElseBlocks);
			for (Block childBlock : blockIf.getElseBlocks()) {
				if (childBlock.isBlockIf() || childBlock.isBlockWhile()) {
					List<Var> childInputs = stmInputs.get(childBlock);
					for (Var var : childInputs) {
						if (!definedElseVar.contains(var)) {
							elseInputs.get(block).add(var);
							stmInputs.get(block).add(var);
						}
					}
				}
			}

		} else if (block instanceof BlockWhile) {
			BlockWhile blockWhile = (BlockWhile) block;
			List<Block> definedBlocks = blockWhile.getBlocks();
			List<Var> definedVar = getVars(true, definedBlocks);
			for (Block childBlock : blockWhile.getBlocks()) {
				if (childBlock.isBlockIf() || childBlock.isBlockWhile()) {
					List<Var> childInputs = stmInputs.get(childBlock);
					for (Var var : childInputs) {
						if (!definedVar.contains(var)) {
							loopBodyInputs.get(block).add(var);
							stmInputs.get(block).add(var);
						}
					}
				}
			}
		}

	}

	private void stmAddVars(Block block, Map<Block, List<Var>> target,
			Map<Block, List<Var>> source) {
		for (Var var : source.get(block)) {
			if (!target.get(block).contains(var)) {
				target.get(block).add(var);
			}
		}
	}

	private List<Var> otherStmDecisionVars(Block block) {
		List<Var> vars = new ArrayList<Var>();

		Set<Var> blkVars = new BlockVars(currentBlock, joinVarMap)
				.doSwitch(block);
		for (Var var : blkVars) {
			if (!vars.contains(var)) {
				vars.add(var);
			}
		}

		return vars;
	}

	private void resolveWhileIO(Block block, List<Var> blkInputs,
			List<Var> blkOutputs, Map<Block, Map<Var, List<Var>>> phi,
			Map<Block, List<Var>> loopBodyInputs,
			Map<Block, List<Var>> loopBodyOutputs,
			Map<Block, List<Var>> stmInputs, Map<Block, List<Var>> stmOutputs) {

		List<Var> resolvedInputs = new ArrayList<Var>(blkInputs);
		// First resolve blkInput
		for (Var iVar : blkInputs) {
			if (phi.get(block).keySet().contains(iVar)) {
				// This is a LoopBody input and a Loop output
				loopBodyInputs.get(block).add(iVar);
				stmOutputs.get(block).add(iVar);
				// Get the Group 0 Input
				stmInputs.get(block).add(phi.get(block).get(iVar).get(0));
				if (!loopBodyOutputs.get(block).contains(
						phi.get(block).get(iVar).get(1))) {
					loopBodyOutputs.get(block).add(
							phi.get(block).get(iVar).get(1));
				}
				resolvedInputs.remove(iVar);
			}
		}

		// The input that has been left should just be latched, so add a direct
		// connection
		for (Var dVar : resolvedInputs) {
			loopBodyInputs.get(block).add(dVar);
			stmInputs.get(block).add(dVar);
		}

		// Now resolve blkOutput
		for (Var oVar : blkOutputs) {
			for (Var kVar : phi.get(block).keySet()) {
				List<Var> values = phi.get(block).get(kVar);
				// Add the Feedback Output
				if (values.get(1) == oVar) {
					if (!stmInputs.get(block).contains(values.get(0))) {
						stmInputs.get(block).add(values.get(0));
					}
					if (!stmOutputs.get(block).contains(kVar)) {
						stmOutputs.get(block).add(kVar);
					}
					loopBodyInputs.get(block).add(kVar);
					loopBodyOutputs.get(block).add(oVar);
				}
			}
		}

	}
}
