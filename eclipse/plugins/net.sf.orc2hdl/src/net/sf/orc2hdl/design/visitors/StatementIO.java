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
import net.sf.orcc.ir.Def;
import net.sf.orcc.ir.ExprBinary;
import net.sf.orcc.ir.ExprVar;
import net.sf.orcc.ir.Expression;
import net.sf.orcc.ir.InstAssign;
import net.sf.orcc.ir.InstLoad;
import net.sf.orcc.ir.InstPhi;
import net.sf.orcc.ir.Instruction;
import net.sf.orcc.ir.Var;
import net.sf.orcc.ir.util.AbstractIrVisitor;

import org.eclipse.emf.ecore.EObject;

/**
 * This class finds the inputs and outputs of an If and While block
 * 
 * @author Endri Bezati
 * 
 */
public class StatementIO extends AbstractIrVisitor<Void> {

	/** The current block Inputs **/
	private Map<Block, List<Var>> blkInputs;

	/** The current block Outputs **/
	private Map<Block, List<Var>> blkOutputs;

	/** Design Resources **/
	private ResourceCache cache;

	/** The current visited If Block **/
	private Block currentBlock;

	/** The current visited Block **/
	private BlockBasic currentBlockBasic;

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

	private Boolean phiVisit;

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
		blkInputs = new HashMap<Block, List<Var>>();
		blkOutputs = new HashMap<Block, List<Var>>();
		joinVarMap = new HashMap<Block, Map<Var, List<Var>>>();
		visitedBlocks = new HashSet<Block>();
		nestedBlock = new LinkedList<Block>();
	}

	@Override
	public Void caseBlockBasic(BlockBasic block) {
		// Visit only the instruction of the If block
		if (block.eContainer() == currentBlock) {
			currentBlockBasic = block;
			super.caseBlockBasic(block);
		}
		return null;
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
		phiVisit = true;

		doSwitch(nodeIf.getJoinBlock());
		phiVisit = false;

		// Initialize the other IO of this If Block
		blkInputs.put(nodeIf, new ArrayList<Var>());
		blkOutputs.put(nodeIf, new ArrayList<Var>());

		// Visit the Then Block
		doSwitch(nodeIf.getThenBlocks());
		otherStmIO(visitedBlocks, nodeIf, nodeIf.getThenBlocks());

		stmAddVars(nodeIf, thenInputs, blkInputs);
		stmProbableAddVars(nodeIf, thenOutputs, blkOutputs, probableThenOutputs);
		stmAddVars(nodeIf, thenOutputs, blkOutputs);

		// Visit the Else Block
		/** Visit Else Block **/
		currentBlock = nodeIf;
		if (!nodeIf.getElseBlocks().isEmpty()) {
			// Initialize the other IO of this If Block
			blkInputs.put(nodeIf, new ArrayList<Var>());
			blkOutputs.put(nodeIf, new ArrayList<Var>());

			// Visit the Then Block
			doSwitch(nodeIf.getElseBlocks());
			otherStmIO(visitedBlocks, nodeIf, nodeIf.getElseBlocks());
			stmAddVars(nodeIf, elseInputs, blkInputs);
			stmProbableAddVars(nodeIf, elseOutputs, blkOutputs,
					probableElseOutputs);
			stmAddVars(nodeIf, elseOutputs, blkOutputs);
		}

		// Add the Input vars of the Input of then and else blocks, iff they are
		// not already included
		stmAddVars(nodeIf, stmInputs, thenInputs);
		stmAddVars(nodeIf, stmInputs, elseInputs);

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
		phiVisit = true;
		doSwitch(nodeWhile.getJoinBlock());
		if (stmDecision.get(nodeWhile).isEmpty()) {
			Var condVar = ((ExprVar) nodeWhile.getCondition()).getUse()
					.getVariable();
			stmDecision.get(nodeWhile).add(condVar);
		}

		phiVisit = false;
		blkInputs.put(nodeWhile, new ArrayList<Var>());
		blkOutputs.put(nodeWhile, new ArrayList<Var>());
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
	public Void caseExprBinary(ExprBinary expr) {
		// Get e1 var and if it defined not in this visited block added as an
		// input
		Var varE1 = ((ExprVar) expr.getE1()).getUse().getVariable();

		if (phiVisit) {
			if (definedInOtherBlock(varE1, currentBlockBasic)
					|| joinVarMap.get(currentBlock).containsKey(varE1)) {
				stmDecision.get(currentBlock).add(varE1);
			}

		} else {
			if (definedInOtherBlock(varE1, currentBlockBasic)) {
				if (currentBlock.isBlockIf()) {
					blkInputs.get(currentBlock).add(varE1);
				} else if (currentBlock.isBlockWhile()) {
					loopBodyInputs.get(currentBlock).add(varE1);
					stmInputs.get(currentBlock).add(varE1);
				}
			}

		}

		// Get e2 var and if it defined not in this visited block added as an
		// input
		Var varE2 = ((ExprVar) expr.getE2()).getUse().getVariable();
		if (phiVisit) {
			if (definedInOtherBlock(varE2, currentBlockBasic)
					|| joinVarMap.get(currentBlock).containsKey(varE2)) {
				stmDecision.get(currentBlock).add(varE2);
			}

		} else {
			if (definedInOtherBlock(varE2, currentBlockBasic)) {
				if (currentBlock.isBlockIf()) {
					blkInputs.get(currentBlock).add(varE2);
				} else if (currentBlock.isBlockWhile()) {
					loopBodyInputs.get(currentBlock).add(varE2);
					stmInputs.get(currentBlock).add(varE2);
				}
			}

		}
		return null;
	}

	@Override
	public Void caseExprVar(ExprVar exprVar) {
		Var var = exprVar.getUse().getVariable();
		if (!phiVisit) {
			if (definedInOtherBlock(var, currentBlockBasic)) {
				blkInputs.get(currentBlock).add(var);
			}
		}
		return null;
	}

	@Override
	public Void caseInstAssign(InstAssign assign) {
		super.caseInstAssign(assign);
		Var target = assign.getTarget().getVariable();
		if (!currentBlock.isBlockWhile() && !phiVisit) {
			blkOutputs.get(currentBlock).add(target);
		}
		return null;
	}

	@Override
	public Void caseInstLoad(InstLoad load) {
		Var loadIndexVar = null;
		List<Expression> indexes = load.getIndexes();
		for (Expression expr : new ArrayList<Expression>(indexes)) {
			loadIndexVar = ((ExprVar) expr).getUse().getVariable();
		}
		if (definedInOtherBlock(loadIndexVar, currentBlockBasic)) {
			blkInputs.get(currentBlock).add(loadIndexVar);
		}
		return null;
	}

	@Override
	public Void caseInstPhi(InstPhi phi) {
		List<Var> phiVars = new ArrayList<Var>();
		Var target = phi.getTarget().getVariable();

		// Add to the Block output
		stmOutputs.get(currentBlock).add(target);

		// Get the Phi Vars, First Value Group 0, Second Value Group 1
		for (Expression expr : phi.getValues()) {
			Var value = ((ExprVar) expr).getUse().getVariable();
			phiVars.add(value);
		}

		// If currentBlock is Block If, then find the output of the then and
		// else outputs
		if (currentBlock.isBlockIf()) {
			if (((BlockIf) currentBlock).getElseBlocks().isEmpty()) {
				stmInputs.get(currentBlock).add(phiVars.get(0));
			} else {
				probableThenOutputs.get(currentBlock).add(phiVars.get(0));
			}

			// If the Else Block is empty then Group 1 comes from the Input
			if (((BlockIf) currentBlock).getElseBlocks().isEmpty()) {
				stmInputs.get(currentBlock).add(phiVars.get(1));
			} else {
				probableElseOutputs.get(currentBlock).add(phiVars.get(1));
			}
		} else if (currentBlock.isBlockWhile()) {
			// The loopBody Input is the same as the Loop output, phi resolution
			loopBodyInputs.get(currentBlock).add(target);
			// The loopBody Output is the Group 1 of the phi
			loopBodyOutputs.get(currentBlock).add(phiVars.get(1));
			// The Loop input is the the Group 0 of the phi
			stmInputs.get(currentBlock).add(phiVars.get(0));
		}

		// Fill up the JoinVar Map
		joinVarMap.get(currentBlock).put(target, phiVars);
		return null;
	}

	private Boolean definedInOtherBlock(Var var, BlockBasic block) {
		for (Def def : var.getDefs()) {
			EObject container = def.eContainer();
			while (!(container instanceof BlockBasic)) {
				container = container.eContainer();
			}
			if (container != block && container.eContainer() != currentBlock) {
				return true;
			}
		}
		return false;
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
						blkInputs.get(currentBlock).add(var);
					} else if (currentBlock.isBlockWhile()) {
						loopBodyInputs.get(currentBlock).add(var);
						if (!stmOutputs.get(currentBlock).contains(var)) {
							stmInputs.get(currentBlock).add(var);
						}
					}
				}
			}

			// Now the outputs
			// Set<Var> nestedBlockOutputs = new HashSet<Var>();
			// for (Block block : blockToProcess) {
			// if (stmOutputs.get(block) != null) {
			// for (Var var : stmOutputs.get(block)) {
			// nestedBlockOutputs.add(var);
			// }
			// }
			// }
			//
			// for (Var var : nestedBlockOutputs) {
			// if (!assignTargets.contains(var)) {
			// if (currentBlock.isBlockIf()) {
			// blkOutputs.get(currentBlock).add(var);
			// } else if (currentBlock.isBlockWhile()) {
			// loopBodyOutputs.get(currentBlock).add(var);
			// stmOutputs.get(currentBlock).add(var);
			// }
			// }
			// }

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

	private void stmProbableAddVars(Block block, Map<Block, List<Var>> target,
			Map<Block, List<Var>> source, Map<Block, List<Var>> probableSource) {
		for (Var var : source.get(block)) {
			if (probableSource.get(block).contains(var)) {
				if (!target.get(block).contains(var)) {
					target.get(block).add(var);
				}
			}
		}
	}
}
