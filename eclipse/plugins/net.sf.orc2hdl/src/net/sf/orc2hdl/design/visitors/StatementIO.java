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
import net.sf.orcc.ir.BlockBasic;
import net.sf.orcc.ir.BlockIf;
import net.sf.orcc.ir.BlockWhile;
import net.sf.orcc.ir.ExprVar;
import net.sf.orcc.ir.Expression;
import net.sf.orcc.ir.InstAssign;
import net.sf.orcc.ir.InstLoad;
import net.sf.orcc.ir.InstPhi;
import net.sf.orcc.ir.Instruction;
import net.sf.orcc.ir.Var;
import net.sf.orcc.ir.util.AbstractIrVisitor;

/**
 * This class finds the inputs and outputs of an If and While block
 * 
 * @author Endri Bezati
 * 
 */
public class StatementIO extends AbstractIrVisitor<Void> {

	/** Design Resources **/
	private ResourceCache cache;

	/** The current visited If Block **/
	private Block currentBlock;

	/** Map of a Branch Else Block Input Variables **/
	private Map<Block, List<Var>> elseInputs;

	/** Map of a Branch Else Block Output Variables **/
	private Map<Block, List<Var>> elseOutputs;

	private Map<Block, List<Var>> probableThenOutputs;

	/** Map of a Branch Else Block Output Variables **/
	private Map<Block, List<Var>> probableElseOutputs;

	/** Map containing the join node **/
	private Map<Block, Map<Var, List<Var>>> joinVarMap;

	/** Map of a LoopBody Block Input Variables **/
	private Map<Block, List<Var>> loopBodyInputs;

	/** Map of a LoopBody Block Output Variables **/
	private Map<Block, List<Var>> loopBodyOutputs;

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

	private LinkedList<Block> nestedBlock;

	private Boolean thenVisit;

	public StatementIO(ResourceCache cache) {
		super(true);
		this.cache = cache;
		// Initialize
		thenInputs = new HashMap<Block, List<Var>>();
		thenOutputs = new HashMap<Block, List<Var>>();
		elseInputs = new HashMap<Block, List<Var>>();
		elseOutputs = new HashMap<Block, List<Var>>();
		probableThenOutputs = new HashMap<Block, List<Var>>();
		probableElseOutputs = new HashMap<Block, List<Var>>();
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
		probableThenOutputs.put(nodeIf, new ArrayList<Var>());
		probableElseOutputs.put(nodeIf, new ArrayList<Var>());
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
		thenInputs.get(nodeIf).addAll(
				getVars(true, false, nodeIf.getThenBlocks()));
		thenOutputs.get(nodeIf).addAll(
				getVars(false, false, nodeIf.getThenBlocks()));
		thenVisit = true;
		doSwitch(nodeIf.getThenBlocks());
		otherStmIO(visitedBlocks, nodeIf, nodeIf.getThenBlocks());

		// Visit the Else Block
		/** Visit Else Block **/
		currentBlock = nodeIf;
		if (!nodeIf.getElseBlocks().isEmpty()) {
			// Visit the Then Block
			elseInputs.get(nodeIf).addAll(
					getVars(true, false, nodeIf.getElseBlocks()));
			elseOutputs.get(nodeIf).addAll(
					getVars(false, false, nodeIf.getElseBlocks()));
			thenVisit = false;
			doSwitch(nodeIf.getElseBlocks());
			otherStmIO(visitedBlocks, nodeIf, nodeIf.getElseBlocks());
		}

		// Add the Input vars of the Input of then and else blocks, iff they are
		// not already included
		stmAddVars(nodeIf, stmInputs, thenInputs);
		stmAddVars(nodeIf, stmInputs, elseInputs);

		stmOutputs.get(nodeIf).addAll(
				ifOutputStm(nodeIf, joinVarMap, thenOutputs, elseOutputs));

		// Add to cache
		cache.addBranch(nodeIf, stmDecision, stmInputs, stmOutputs, thenInputs,
				thenOutputs, elseInputs, elseOutputs, joinVarMap);
		visitedBlocks.add(nodeIf);

		// Fix currentBlock
		int indexOfLastBlock = nestedBlock.lastIndexOf(nodeIf);
		if (indexOfLastBlock != 0) {
			if (nestedBlock.get(indexOfLastBlock - 1) == nodeIf.eContainer()) {
				currentBlock = nestedBlock.get(indexOfLastBlock - 1);
			}
		}
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
		if (stmDecision.get(nodeWhile).isEmpty()) {
			Var condVar = ((ExprVar) nodeWhile.getCondition()).getUse()
					.getVariable();
			stmDecision.get(nodeWhile).add(condVar);
		}

		List<Var> blkInputs = getVars(true, false, nodeWhile.getBlocks());
		List<Var> blkOutputs = getVars(false, false, nodeWhile.getBlocks());
		resolveWhileIO(nodeWhile, blkInputs, blkOutputs, joinVarMap,
				loopBodyInputs, loopBodyOutputs, stmInputs, stmOutputs);
		doSwitch(nodeWhile.getBlocks());
		otherStmIO(visitedBlocks, nodeWhile, nodeWhile.getBlocks());

		// Add to cache
		cache.addLoop(nodeWhile, stmDecision, stmInputs, stmOutputs,
				loopBodyInputs, loopBodyOutputs, joinVarMap);
		visitedBlocks.add(nodeWhile);

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
		List<Var> phiVars = new ArrayList<Var>();
		Var target = phi.getTarget().getVariable();

		// Get the Phi Vars, First Value Group 0, Second Value Group 1
		for (Expression expr : phi.getValues()) {
			Var value = ((ExprVar) expr).getUse().getVariable();
			phiVars.add(value);
		}

		// Fill up the JoinVar Map
		joinVarMap.get(currentBlock).put(target, phiVars);
		return null;
	}

	private void otherStmIO(Set<Block> visitedBlocks, Block currentBlock,
			List<Block> currentStmBlocks) {
		// First Look if we need to process a set of blocks
		Set<Block> blockToProcess = new HashSet<Block>();
		for (Block visitedBlock : visitedBlocks) {
			if (currentStmBlocks.contains(visitedBlock)) {
				blockToProcess.add(visitedBlock);
			}
		}

		if (!blockToProcess.isEmpty()) {
			// Get all Var targets of assign instructions
			Set<Var> assignTargets = new HashSet<Var>();
			for (Block block : currentStmBlocks) {
				if (block.isBlockBasic()) {
					for (Instruction inst : ((BlockBasic) block)
							.getInstructions()) {
						if (inst.isInstAssign()) {
							Var target = ((InstAssign) inst).getTarget()
									.getVariable();
							assignTargets.add(target);
						} else if (inst.isInstLoad()) {
							Var target = ((InstLoad) inst).getTarget()
									.getVariable();
							assignTargets.add(target);
						}
					}

				}
			}
			// First the inputs
			// Get all inputs of the blocks to be processed
			Set<Var> nestedBlockInputs = new HashSet<Var>();
			for (Block block : blockToProcess) {
				if (stmInputs.get(block) != null) {
					for (Var var : stmInputs.get(block)) {
						nestedBlockInputs.add(var);
					}
				}
			}

			// See if the nestedBlockInputs has inputs that are not on the
			// current visited Block
			for (Var var : nestedBlockInputs) {
				if (!assignTargets.contains(var)) {
					if (currentBlock.isBlockIf()) {
						stmInputs.get(currentBlock).add(var);
						if (thenVisit) {
							thenInputs.get(currentBlock).add(var);
						} else {
							elseInputs.get(currentBlock).add(var);
						}
					} else if (currentBlock.isBlockWhile()) {
						loopBodyInputs.get(currentBlock).add(var);
						if (!stmOutputs.get(currentBlock).contains(var)) {
							stmInputs.get(currentBlock).add(var);
						}
					}
				}
			}
		}

	}

	private List<Var> ifOutputStm(Block block,
			Map<Block, Map<Var, List<Var>>> phi,
			Map<Block, List<Var>> thenOutput, Map<Block, List<Var>> elseOutput) {
		Set<Var> outputVars = new HashSet<Var>();
		for (Var kVar : phi.get(block).keySet()) {
			List<Var> values = phi.get(block).get(kVar);

			for (Var tVar : thenOutput.get(block)) {
				if (values.contains(tVar)) {
					outputVars.add(kVar);
				}
			}

			for (Var eVar : elseOutput.get(block)) {
				if (values.contains(eVar)) {
					outputVars.add(kVar);
				}
			}
		}

		List<Var> vars = new ArrayList<Var>();
		for (Var var : outputVars) {
			vars.add(var);
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
					loopBodyOutputs.get(block).add(oVar);
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

	private List<Var> getVars(Boolean input, Boolean deepSearch,
			List<Block> blocks) {
		List<Var> vars = new ArrayList<Var>();
		if (!blocks.isEmpty()) {
			for (Block block : blocks) {
				Set<Var> blkVars = new BlockVars(input, deepSearch)
						.doSwitch(block);
				for (Var var : blkVars) {
					if (!vars.contains(var)) {
						vars.add(var);
					}
				}
			}
		}
		return vars;
	}
}
