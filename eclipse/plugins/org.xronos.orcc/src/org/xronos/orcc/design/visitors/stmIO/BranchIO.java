/*
 * Copyright (c) 2013, Ecole Polytechnique Fédérale de Lausanne
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
package org.xronos.orcc.design.visitors.stmIO;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sf.orcc.ir.Block;
import net.sf.orcc.ir.BlockBasic;
import net.sf.orcc.ir.BlockIf;
import net.sf.orcc.ir.BlockWhile;
import net.sf.orcc.ir.ExprVar;
import net.sf.orcc.ir.Expression;
import net.sf.orcc.ir.InstPhi;
import net.sf.orcc.ir.Var;
import net.sf.orcc.ir.util.AbstractIrVisitor;
import net.sf.orcc.util.Attribute;
import net.sf.orcc.util.util.EcoreHelper;

public class BranchIO extends AbstractIrVisitor<Void> {

	private BlockIf blockIf;

	private Map<Block, List<Var>> bodyBlocksInputs;

	private Map<Block, List<Var>> bodyBlocksOutputs;

	private Map<BlockIf, Map<Var, List<Var>>> branchPhi;

	private Map<BlockIf, List<Var>> elseInputs;

	private Map<BlockIf, List<Var>> elseOutputs;

	private List<Var> inputs;

	private List<Var> outputs;

	private Map<BlockIf, List<Var>> thenInputs;

	private Map<BlockIf, List<Var>> thenOutputs;

	public BranchIO(BlockIf blockIf) {
		super(true);
		this.blockIf = blockIf;
		bodyBlocksInputs = new HashMap<Block, List<Var>>();
		bodyBlocksOutputs = new HashMap<Block, List<Var>>();
		branchPhi = new HashMap<BlockIf, Map<Var, List<Var>>>();
		elseInputs = new HashMap<BlockIf, List<Var>>();
		elseOutputs = new HashMap<BlockIf, List<Var>>();
		thenInputs = new HashMap<BlockIf, List<Var>>();
		thenOutputs = new HashMap<BlockIf, List<Var>>();
	}

	@SuppressWarnings("unchecked")
	@Override
	public Void caseBlockBasic(BlockBasic block) {
		super.caseBlockBasic(block);
		if (!block.hasAttribute("inputs") && !block.hasAttribute("outputs")) {
			BlockBasicIO blockBasicIO = new BlockBasicIO(block);
			bodyBlocksInputs.put(block, blockBasicIO.getInputs());
			bodyBlocksOutputs.put(block, blockBasicIO.getOutputs());
		} else {
			Attribute input = block.getAttribute("inputs");
			List<Var> blockBasicInputs = (List<Var>) input.getObjectValue();
			bodyBlocksInputs.put(block, blockBasicInputs);

			Attribute output = block.getAttribute("outputs");
			List<Var> blockBasicOutput = (List<Var>) output.getObjectValue();
			bodyBlocksOutputs.put(block, blockBasicOutput);
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	@Override
	public Void caseBlockIf(BlockIf blockIf) {
		if (!blockIf.hasAttribute("inputs") && !blockIf.hasAttribute("outputs")) {
			// Initialize the maps
			inputs = new ArrayList<Var>();
			outputs = new ArrayList<Var>();
			thenInputs.put(blockIf, new ArrayList<Var>());
			thenOutputs.put(blockIf, new ArrayList<Var>());
			elseInputs.put(blockIf, new ArrayList<Var>());
			elseOutputs.put(blockIf, new ArrayList<Var>());
			branchPhi.put(blockIf, new HashMap<Var, List<Var>>());

			// Visit JoinBlock
			doSwitch(blockIf.getJoinBlock());

			// Visit the OtherBlocks of Then blocks
			doSwitch(blockIf.getThenBlocks());

			findBlocksIO(blockIf, blockIf.getThenBlocks(), thenInputs,
					thenOutputs);

			// Visit the OtherBlocks of Else blocks
			doSwitch(blockIf.getElseBlocks());

			findBlocksIO(blockIf, blockIf.getElseBlocks(), elseInputs,
					elseOutputs);

			// Resolve the BlockIf IO

			// Inputs

			// Add decision condition
			Var decisionVar = ((ExprVar) blockIf.getCondition()).getUse()
					.getVariable();
			inputs.add(decisionVar);

			// Add All then Inputs
			inputs.addAll(thenInputs.get(blockIf));

			// Add all else Inputs
			for (Var elseInputVar : elseInputs.get(blockIf)) {
				if (!inputs.contains(elseInputVar)) {
					inputs.add(elseInputVar);
				}
			}

			// Outputs

			outputs.addAll(branchPhi.get(blockIf).keySet());

			// Add attribute
			blockIf.setAttribute("inputs", inputs);
			blockIf.setAttribute("outputs", outputs);
			blockIf.setAttribute("decision", decisionVar);
			blockIf.setAttribute("thenInputs", thenInputs.get(blockIf));
			blockIf.setAttribute("thenOutputs", thenOutputs.get(blockIf));
			blockIf.setAttribute("elseInputs", elseInputs.get(blockIf));
			blockIf.setAttribute("elseOutputs", elseOutputs.get(blockIf));
			blockIf.setAttribute("phi", branchPhi.get(blockIf));

			// Put it to the visited
			bodyBlocksInputs.put(blockIf, inputs);
			bodyBlocksOutputs.put(blockIf, outputs);

		} else {
			Attribute input = blockIf.getAttribute("inputs");
			List<Var> branchInputs = (List<Var>) input.getObjectValue();
			bodyBlocksInputs.put(blockIf, branchInputs);

			Attribute output = blockIf.getAttribute("outputs");
			List<Var> branchOutput = (List<Var>) output.getObjectValue();
			bodyBlocksOutputs.put(blockIf, branchOutput);
		}
		return null;
	}

	private void findBlocksIO(BlockIf blockIf, List<Block> blocks,
			Map<BlockIf, List<Var>> blockInputs,
			Map<BlockIf, List<Var>> blockOutputs) {

		for (Block block : blocks) {
			int indexOfBlock = blocks.indexOf(block);
			List<Block> previousBlocks = blocks.subList(0, indexOfBlock);
			List<Block> restOfBlocks = blocks.subList(indexOfBlock + 1,
					blocks.size());

			// Inputs
			List<Var> inVars = bodyBlocksInputs.get(block);
			for (Var var : inVars) {
				if (!containsVar(previousBlocks, bodyBlocksOutputs, var)) {
					blockInputs.get(blockIf).add(var);
				}
			}
			// Outputs
			List<Var> outVars = bodyBlocksOutputs.get(block);
			for (Var var : outVars) {
				if (!containsVar(restOfBlocks, bodyBlocksInputs, var)) {
					blockOutputs.get(blockIf).add(var);
				}
			}
		}
	}

	private boolean containsVar(List<Block> blocks,
			Map<Block, List<Var>> bodyBlocks, Var var) {
		boolean contains = false;
		if (!blocks.isEmpty()) {
			for (Block block : blocks) {
				List<Var> blockInVars = bodyBlocks.get(block);
				if (blockInVars.contains(var)) {
					return true;
				}
			}
		}
		return contains;
	}

	@SuppressWarnings("unchecked")
	@Override
	public Void caseBlockWhile(BlockWhile blockWhile) {
		if (!blockWhile.hasAttribute("inputs")
				&& !blockWhile.hasAttribute("outputs")) {
			LoopIO loopIO = new LoopIO(blockWhile);
			bodyBlocksInputs.put(blockWhile, loopIO.getInputs());
			bodyBlocksOutputs.put(blockWhile, loopIO.getOutputs());
		} else {
			Attribute input = blockWhile.getAttribute("inputs");
			List<Var> loopInputs = (List<Var>) input.getObjectValue();
			bodyBlocksInputs.put(blockWhile, loopInputs);

			Attribute output = blockWhile.getAttribute("outputs");
			List<Var> loopOutput = (List<Var>) output.getObjectValue();
			bodyBlocksOutputs.put(blockWhile, loopOutput);
		}
		return null;
	}

	@Override
	public Void caseInstPhi(InstPhi phi) {
		List<Var> phiValues = new ArrayList<Var>();
		Var target = phi.getTarget().getVariable();

		// Get the Phi Variables, First Value Group 0, Second Value Group 1
		for (Expression expr : phi.getValues()) {
			Var value = ((ExprVar) expr).getUse().getVariable();
			phiValues.add(value);
		}
		// Fill up the JoinVar Map
		BlockIf branch = EcoreHelper.getContainerOfType(phi, BlockIf.class);
		branchPhi.get(branch).put(target, phiValues);
		return null;
	}

	public List<Var> getInputs() {
		doSwitch(blockIf);
		return inputs;
	}

	public List<Var> getOutputs() {
		doSwitch(blockIf);
		return outputs;
	}
}
