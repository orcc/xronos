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
import net.sf.orcc.ir.Procedure;
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
		List<Var> blkInputs = getVars(true, false, nodeIf.getThenBlocks());
		List<Var> blkOutputs = getVars(false, false, nodeIf.getThenBlocks());
		resovleStmIO(nodeIf, blkInputs, blkOutputs, 0);

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
			blkInputs = getVars(true, false, nodeIf.getElseBlocks());
			blkOutputs = getVars(false, false, nodeIf.getElseBlocks());
			resovleStmIO(nodeIf, blkInputs, blkOutputs, 1);
		}

		// Add the Input vars of the Input of then and else blocks, iff they are
		// not already included
		stmAddVars(nodeIf, stmInputs, thenInputs);
		stmAddVars(nodeIf, stmInputs, elseInputs);

		// stmOutputs.get(nodeIf).addAll(
		// ifOutputStm(nodeIf, joinVarMap, thenOutputs, elseOutputs));

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
		List<Var> blkInputs = getVars(true, false, nodeWhile.getBlocks());
		List<Var> blkOutputs = getVars(false, false, nodeWhile.getBlocks());
		resovleStmIO(nodeWhile, blkInputs, blkOutputs, 2);
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
					loopBodyInputs.get(block).add(kVar);
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
				Set<Var> blkVars = new BlockVars(input, deepSearch, blocks)
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

	private void resovleStmIO(Block block, List<Var> blkInputs,
			List<Var> blkOutputs, Integer type) {
		if (type == 2) {
			// Get only BlockWhile and BlockIf children
			for (Block cBlock : ((BlockWhile) block).getBlocks()) {
				if ((cBlock instanceof BlockWhile)
						|| (cBlock instanceof BlockIf)) {
					// Firstly Get the input/output of this Block
					List<Var> childBlockInputs = stmInputs.get(cBlock);
					List<Var> childBlockOutputs = stmOutputs.get(cBlock);

					List<Block> pBlocks = new ArrayList<Block>();
					List<Block> previousChildBlocks = new ArrayList<Block>();
					// Check if the container is Procedure or Stm Block
					if (block.eContainer() instanceof Procedure) {
						Procedure cProcedure = (Procedure) block.eContainer();
						pBlocks = new ArrayList<Block>(cProcedure.getBlocks());
						// Get its child blocks

					} else if (block.eContainer() instanceof BlockWhile) {
						// Get its container blocks
						BlockWhile blockWhile = (BlockWhile) block.eContainer();
						pBlocks = new ArrayList<Block>(blockWhile.getBlocks());
					} else if ((block.eContainer() instanceof BlockIf)) {
						BlockIf blockIf = (BlockIf) block.eContainer();
						if (thenVisit) {
							pBlocks = new ArrayList<Block>(
									blockIf.getThenBlocks());
						} else {
							pBlocks = new ArrayList<Block>(
									blockIf.getElseBlocks());
						}
					}

					// Get its child blocks
					for (Block blk : ((BlockWhile) block).getBlocks()) {
						if (blk == cBlock) {
							break;
						}
						previousChildBlocks.add(blk);
					}

					if (!pBlocks.isEmpty()) {
						for (Block rBlock : new ArrayList<Block>(pBlocks)) {
							if (block == rBlock) {
								break;
							}
							pBlocks.remove(rBlock);
						}
						// Get the used Variables
						List<Var> usedVars = getVars(true, false, pBlocks);

						// Resolve the needs of the Parent
						for (Var var : usedVars) {
							if (childBlockInputs.contains(var)) {
								if (joinVarMap.get(block).containsKey(var)) {
									Var target = var;
									Var groupZero = joinVarMap.get(block)
											.get(var).get(0);
									Var groupOne = joinVarMap.get(block)
											.get(var).get(1);
									loopBodyInputs.get(block).add(target);
									loopBodyOutputs.get(block).add(groupOne);
									stmInputs.get(block).add(groupZero);
									stmOutputs.get(block).add(target);
									childBlockInputs.remove(target);
									childBlockOutputs.remove(groupOne);
								}
							}

						}

						Set<Var> definedVar = new BlockVars(true)
								.doSwitch(previousChildBlocks);

						// Resolve the needs of the children
						for (Var var : childBlockInputs) {
							// The var is defined inside this block so deleted
							if (blkOutputs.contains(var)) {
								blkOutputs.remove(var);
							} else if (blkInputs.contains(var)
									|| joinVarMap.get(block).containsKey(var)) {
								Var target = var;
								Var groupZero = joinVarMap.get(block).get(var)
										.get(0);
								Var groupOne = joinVarMap.get(block).get(var)
										.get(1);
								loopBodyInputs.get(block).add(target);
								loopBodyOutputs.get(block).add(groupOne);
								stmInputs.get(block).add(groupZero);
								stmOutputs.get(block).add(target);
								blkInputs.remove(target);
								blkOutputs.remove(groupOne);
							} else if (!definedVar.contains(var)) {
								// The var is defined in this block, make input
								loopBodyInputs.get(block).add(var);
								stmInputs.get(block).add(var);
							}
						}
					}

				}
			}

		} else if (type == 0 || type == 1) {
			// Get its container Blocks
			List<Block> parentBlocks = new ArrayList<Block>();
			if (block.eContainer() instanceof Procedure) {
				Procedure cProcedure = (Procedure) block.eContainer();
				parentBlocks = new ArrayList<Block>(cProcedure.getBlocks());
			} else if (block.eContainer() instanceof BlockWhile) {
				BlockWhile blockWhile = (BlockWhile) block.eContainer();
				parentBlocks = new ArrayList<Block>(blockWhile.getBlocks());
			} else if (block.eContainer() instanceof BlockIf) {
				BlockIf blockIf = (BlockIf) block.eContainer();
				if (type == 0) {
					parentBlocks = new ArrayList<Block>(blockIf.getThenBlocks());
				} else {
					parentBlocks = new ArrayList<Block>(blockIf.getElseBlocks());
				}
			}

			// Get the Blocks after the current Block and the ones after
			List<Block> definedBlock = new ArrayList<Block>();
			List<Block> usedBlock = new ArrayList<Block>();
			Boolean found = false;
			for (Block blk : new ArrayList<Block>(parentBlocks)) {
				if (block == blk) {
					found = true;
				}
				if (found) {
					if (block != blk) {
						usedBlock.add(blk);
					}
				} else {
					definedBlock.add(blk);
				}
			}

			// Resolves the needs from his Parent
			List<Var> definedVars = getVars(true, definedBlock);
			for (Var var : new ArrayList<Var>(blkInputs)) {
				if (!definedVars.contains(var)) {
					if (type == 0) {
						thenInputs.get(block).add(var);
					} else if (type == 1) {
						elseInputs.get(block).add(var);
					}
					blkInputs.remove(var);
				}
			}

			List<Var> usedVars = getVars(true, usedBlock);
			for (Var var : new ArrayList<Var>(blkOutputs)) {
				if (usedVars.contains(var)
						|| containsPhiValue(var, joinVarMap.get(block))) {
					Var target = getPhiTarget(var, joinVarMap.get(block));
					if (type == 0) {
						thenOutputs.get(block).add(var);
					} else if (type == 1) {
						elseOutputs.get(block).add(var);
					}
					if (!stmOutputs.get(block).contains(target)) {
						stmOutputs.get(block).add(target);
					}
					blkOutputs.remove(var);
				}
			}

			List<Block> childrenBlocks = new ArrayList<Block>();
			List<Block> previousChildrenBlocks = new ArrayList<Block>();
			if (type == 0) {
				childrenBlocks = ((BlockIf) block).getThenBlocks();
			} else if (type == 1) {
				childrenBlocks = ((BlockIf) block).getElseBlocks();
			}

			for (Block cBlock : childrenBlocks) {
				if ((cBlock instanceof BlockWhile)
						|| (cBlock instanceof BlockIf)) {
					for (Block blk : childrenBlocks) {
						if (blk == cBlock) {
							break;
						}
						previousChildrenBlocks.add(blk);
					}

					definedVars = getVars(true, previousChildrenBlocks);

					// Firstly Get the input/output of this child Block
					List<Var> childBlockInputs = stmInputs.get(cBlock);
					List<Var> childBlockOutputs = stmOutputs.get(cBlock);

					// Resolve the needs of the Parent
					// Get the used Variables
					usedVars = getVars(true, false, usedBlock);

					for (Var var : usedVars) {
						if (joinVarMap.get(block).containsKey(var)) {
							Var target = var;
							Var groupZero = joinVarMap.get(block).get(var)
									.get(0);
							Var groupOne = joinVarMap.get(block).get(var)
									.get(1);
							if (type == 0) {
								thenOutputs.get(block).add(groupZero);
								if (!stmOutputs.get(block).contains(target)) {
									stmOutputs.get(block).add(target);
								}
							} else if (type == 1) {
								elseOutputs.get(block).add(groupOne);
								if (!stmOutputs.get(block).contains(target)) {
									stmOutputs.get(block).add(target);
								}
							} else if (type == 2) {

							}

						}

					}

				}
			}
		}
	}

	private Var getPhiTarget(Var var, Map<Var, List<Var>> phiMap) {
		for (Var key : phiMap.keySet()) {
			if (phiMap.get(key).contains(var)) {
				return key;
			}
		}
		return null;
	}

	private Boolean containsPhiValue(Var var, Map<Var, List<Var>> phiMap) {
		for (Var key : phiMap.keySet()) {
			List<Var> values = phiMap.get(key);
			if (values.contains(var)) {
				return true;
			}
		}
		return false;
	}
}
