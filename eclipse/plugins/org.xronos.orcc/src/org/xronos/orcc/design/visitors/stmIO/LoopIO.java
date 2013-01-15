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

import org.eclipse.emf.ecore.EObject;
import org.xronos.orcc.ir.BlockMutex;

public class LoopIO extends AbstractIrVisitor<Void> {

	private BlockWhile blockWhile;

	private Map<Block, List<Var>> bodyBlocksInputs;

	private Map<Block, List<Var>> bodyBlocksOutputs;

	private Map<BlockWhile, List<Var>> decisionInputs;
	private Map<BlockWhile, Map<Var, List<Var>>> loopPhi;

	public LoopIO(BlockWhile blockWhile) {
		super(true);
		this.blockWhile = blockWhile;
		decisionInputs = new HashMap<BlockWhile, List<Var>>();
		bodyBlocksInputs = new HashMap<Block, List<Var>>();
		bodyBlocksOutputs = new HashMap<Block, List<Var>>();
		loopPhi = new HashMap<BlockWhile, Map<Var, List<Var>>>();
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
			BranchIO branchIO = new BranchIO(blockIf);
			bodyBlocksInputs.put(blockIf, branchIO.getInputs());
			bodyBlocksOutputs.put(blockIf, branchIO.getOutputs());
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

	@SuppressWarnings("unchecked")
	public Void caseBlockMutex(BlockMutex blockMutex) {
		if (!blockMutex.hasAttribute("inputs")
				&& !blockMutex.hasAttribute("outputs")) {
			MutexIO mutexIO = new MutexIO(blockMutex);
			bodyBlocksInputs.put(blockMutex, mutexIO.getInputs());
			bodyBlocksOutputs.put(blockMutex, mutexIO.getOutputs());
		} else {
			Attribute input = blockMutex.getAttribute("inputs");
			List<Var> loopInputs = (List<Var>) input.getObjectValue();
			bodyBlocksInputs.put(blockMutex, loopInputs);

			Attribute output = blockMutex.getAttribute("outputs");
			List<Var> loopOutput = (List<Var>) output.getObjectValue();
			bodyBlocksOutputs.put(blockMutex, loopOutput);
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	@Override
	public Void caseBlockWhile(BlockWhile blockWhile) {
		if (!blockWhile.hasAttribute("inputs")
				&& !blockWhile.hasAttribute("outputs")) {

			// Initialize the maps
			decisionInputs.put(blockWhile, new ArrayList<Var>());

			loopPhi.put(blockWhile, new HashMap<Var, List<Var>>());

			// Visit the Join Block
			doSwitch(blockWhile.getJoinBlock());

			// Get the decisions inputs
			Var decisionVar = ((ExprVar) blockWhile.getCondition()).getUse()
					.getVariable();
			decisionInputs.get(blockWhile).add(decisionVar);

			// Add the decision inputs from the joint Block
			BlockBasicIO blockBasicIO = new BlockBasicIO(
					blockWhile.getJoinBlock());
			for (Var usedDecisionVars : blockBasicIO.getInputs()) {
				if (!decisionInputs.get(blockWhile).contains(usedDecisionVars)) {
					decisionInputs.get(blockWhile).add(usedDecisionVars);
				}
			}

			// Visit the OtherBlocks and get its IOs
			doSwitch(blockWhile.getBlocks());

			// Initialize the inputs and outputs of the BlochWhile
			List<Var> inputs = new ArrayList<Var>();
			List<Var> outputs = new ArrayList<Var>();
			List<Var> bodyInputs = new ArrayList<Var>();
			List<Var> bodyOutputs = new ArrayList<Var>();

			// Input and Output from Phi
			for (Var target : loopPhi.get(blockWhile).keySet()) {
				Var valueZero = loopPhi.get(blockWhile).get(target).get(0);
				Var valueOne = loopPhi.get(blockWhile).get(target).get(1);

				// The input takes the zero value of Phi
				inputs.add(valueZero);
				// The body Input takes also the target of Phi
				bodyInputs.add(target);
				// The bodyOutput takes the target and the first value of Phi
				bodyOutputs.add(valueOne);
			}

			// Resolve LoopBody Inputs
			for (Block block : blockWhile.getBlocks()) {
				int indexOfBlock = blockWhile.getBlocks().indexOf(block);
				List<Block> previousBlocks = blockWhile.getBlocks().subList(0,
						indexOfBlock);
				List<Block> restOfBlocks = blockWhile.getBlocks().subList(
						indexOfBlock + 1, blockWhile.getBlocks().size());

				// Inputs

				List<Var> inVars = bodyBlocksInputs.get(block);
				for (Var var : inVars) {
					if (!containsOutputVar(previousBlocks, var)) {
						// block Inputs are also the input of the loopBody
						if (!bodyInputs.contains(var)) {
							bodyInputs.add(var);
						}
						if (loopPhi.get(blockWhile).keySet().contains(var)) {
							Var valueZero = loopPhi.get(blockWhile).get(var)
									.get(0);
							Var valueOne = loopPhi.get(blockWhile).get(var)
									.get(1);

							// This is a LoopBody input and a Loop output
							if (!outputs.contains(var)) {
								outputs.add(var);
							}
							if (!inputs.contains(valueZero)) {
								inputs.add(valueZero);
							}
							if (!bodyOutputs.contains(valueOne)) {
								bodyOutputs.add(valueOne);
							}
						} else {
							// The input that has been left should just be
							// latched, so add a direct
							// connection
							if (!inputs.contains(var)) {
								inputs.add(var);
							}
						}
					}
				}

				// Outputs

				List<Var> outVars = bodyBlocksOutputs.get(block);

				for (Var var : outVars) {
					if (!containsInputVar(restOfBlocks, var)) {
						for (Var targetPhi : loopPhi.get(blockWhile).keySet()) {
							List<Var> values = loopPhi.get(blockWhile).get(
									targetPhi);
							if (values.get(1) == var) {
								if (!inputs.contains(values.get(0))) {
									inputs.add(values.get(0));
								}
								if (!outputs.contains(targetPhi)) {
									outputs.add(targetPhi);
								}
								if (!bodyInputs.contains(targetPhi)) {
									bodyInputs.add(targetPhi);
								}
								if (!bodyOutputs.contains(var)) {
									bodyOutputs.add(var);
								}
							}
						}
					}
				}
			}

			// Resolve Decision Inputs

			for (Var var : decisionInputs.get(blockWhile)) {
				if (!inputs.contains(var) && !bodyInputs.contains(var)) {
					// This is really an output variable
					inputs.add(var);
					bodyInputs.add(var);
				}
			}
			// Delete the Condition Var from inputs and bodyInputs
			decisionVar = ((ExprVar) blockWhile.getCondition()).getUse()
					.getVariable();
			if (inputs.contains(decisionVar)) {
				inputs.remove(decisionVar);
			}

			if (bodyInputs.contains(decisionVar)) {
				bodyInputs.remove(decisionVar);
			}

			// Add to the Attribute of the Block While
			blockWhile.setAttribute("inputs", inputs);
			blockWhile.setAttribute("outputs", outputs);
			blockWhile.setAttribute("decisionInputs",
					decisionInputs.get(blockWhile));
			blockWhile.setAttribute("bodyInputs", bodyInputs);
			blockWhile.setAttribute("bodyOutputs", bodyOutputs);
			blockWhile.setAttribute("phi", loopPhi.get(blockWhile));

			// Put it to the visited
			bodyBlocksInputs.put(blockWhile, inputs);
			bodyBlocksOutputs.put(blockWhile, outputs);

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

		// Get the Phi Vars, First Value Group 0, Second Value Group 1
		for (Expression expr : phi.getValues()) {
			Var value = ((ExprVar) expr).getUse().getVariable();
			phiValues.add(value);
		}
		BlockWhile loop = EcoreHelper.getContainerOfType(phi, BlockWhile.class);
		loopPhi.get(loop).put(target, phiValues);
		return null;
	}

	private boolean containsInputVar(List<Block> blocks, Var var) {
		boolean contains = false;
		if (!blocks.isEmpty()) {
			for (Block block : blocks) {
				List<Var> blockInVars = bodyBlocksInputs.get(block);
				if (blockInVars.contains(var)) {
					return true;
				}
			}
		}
		return contains;
	}

	private boolean containsOutputVar(List<Block> blocks, Var var) {
		boolean contains = false;
		if (!blocks.isEmpty()) {
			for (Block block : blocks) {
				List<Var> blockInVars = bodyBlocksOutputs.get(block);
				if (blockInVars.contains(var)) {
					return true;
				}
			}
		}
		return contains;
	}

	@Override
	public Void defaultCase(EObject object) {
		if (object instanceof BlockMutex) {
			caseBlockMutex((BlockMutex) object);
		}
		return super.defaultCase(object);
	}

	@SuppressWarnings("unchecked")
	public List<Var> getBodyInputs() {
		if (!blockWhile.hasAttribute("bodyInputs")) {
			doSwitch(blockWhile);
		}
		return (List<Var>) blockWhile.getAttribute("bodyInputs")
				.getObjectValue();
	}

	@SuppressWarnings("unchecked")
	public List<Var> getBodyOutputs() {
		if (!blockWhile.hasAttribute("bodyOutputs")) {
			doSwitch(blockWhile);
		}
		return (List<Var>) blockWhile.getAttribute("bodyOutputs")
				.getObjectValue();
	}

	public Var getDecision() {
		return ((ExprVar) blockWhile.getCondition()).getUse().getVariable();
	}

	@SuppressWarnings("unchecked")
	public List<Var> getDecisionInputs() {
		if (!blockWhile.hasAttribute("decisionInputs")) {
			doSwitch(blockWhile);
		}
		return (List<Var>) blockWhile.getAttribute("decisionInputs")
				.getObjectValue();
	}

	@SuppressWarnings("unchecked")
	public List<Var> getInputs() {
		if (!blockWhile.hasAttribute("inputs")) {
			doSwitch(blockWhile);
		}
		return (List<Var>) blockWhile.getAttribute("inputs").getObjectValue();
	}

	@SuppressWarnings("unchecked")
	public List<Var> getOutputs() {
		if (!blockWhile.hasAttribute("outputs")) {
			doSwitch(blockWhile);
		}
		return (List<Var>) blockWhile.getAttribute("outputs").getObjectValue();
	}

	@SuppressWarnings("unchecked")
	public Map<Var, List<Var>> getPhi() {
		if (!blockWhile.hasAttribute("phi")) {
			doSwitch(blockWhile);
		}
		return (Map<Var, List<Var>>) blockWhile.getAttribute("phi")
				.getObjectValue();
	}

}
