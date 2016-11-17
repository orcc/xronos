/* 
 * XRONOS-EXELIXI
 * 
 * Copyright (C) 2011-2016 EPFL SCI STI MM
 *
 * This file is part of XRONOS-EXELIXI.
 *
 * XRONOS-EXELIXI is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * XRONOS-EXELIXI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with XRONOS-EXELIXI. If not, see <http://www.gnu.org/licenses/>.
 * 
 * Additional permission under GNU GPL version 3 section 7
 * 
 * If you modify this Program, or any covered work, by linking or combining it
 * with Eclipse (or a modified version of Eclipse or an Eclipse plugin or 
 * an Eclipse library), containing parts covered by the terms of the 
 * Eclipse Public License (EPL), the licensors of this Program grant you 
 * additional permission to convey the resulting work.  Corresponding Source 
 * for a non-source form of such a combination shall include the source code 
 * for the parts of Eclipse libraries used as well as that of the covered work.
 * 
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

/**
 * 
 * @author Endri Bezati
 */
public class LoopIO extends AbstractIrVisitor<Void> {

	private BlockWhile blockWhile;

	private Map<Block, List<Var>> bodyBlocksInputs;

	private Map<Block, List<Var>> bodyBlocksOutputs;

	private Map<BlockWhile, List<Var>> decisionInputs;
	private Map<BlockWhile, List<Var>> decisionOutputs;

	private Map<BlockWhile, Map<Var, List<Var>>> loopPhi;

	public LoopIO(BlockWhile blockWhile) {
		super(true);
		this.blockWhile = blockWhile;
		decisionInputs = new HashMap<BlockWhile, List<Var>>();
		decisionOutputs = new HashMap<BlockWhile, List<Var>>();
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
			decisionOutputs.put(blockWhile, new ArrayList<Var>());

			loopPhi.put(blockWhile, new HashMap<Var, List<Var>>());

			// Visit the Join Block
			doSwitch(blockWhile.getJoinBlock());

			// Get the decisions inputs
			Var decisionVar = ((ExprVar) blockWhile.getCondition()).getUse()
					.getVariable();
			decisionOutputs.get(blockWhile).add(decisionVar);

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
				// The output takes the target of Phi
				outputs.add(target);
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
					if (!containsOutputVar(block, previousBlocks, var)) {
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
					if (!containsInputVar(block, restOfBlocks, var)) {
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

			// Add to the Attribute of the Block While
			blockWhile.setAttribute("inputs", inputs);
			blockWhile.setAttribute("outputs", outputs);
			blockWhile.setAttribute("decisionInputs",
					decisionInputs.get(blockWhile));
			blockWhile.setAttribute("decisionOutputs",
					decisionOutputs.get(blockWhile));
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

	private boolean containsInputVar(Block currentBlock, List<Block> blocks,
			Var var) {
		boolean contains = false;
		if (!blocks.isEmpty()) {
			for (Block block : blocks) {
				List<Var> blockInVars = bodyBlocksInputs.get(block);
				if (blockInVars.contains(var)) {
					return true;
				}
			}
		} else {
			// First or Last Block of Blocks
			List<Var> vars = bodyBlocksInputs.get(currentBlock);
			if (vars.contains(var)) {
				return true;
			}
		}
		return contains;
	}

	private boolean containsOutputVar(Block currentBlock, List<Block> blocks,
			Var var) {
		boolean contains = false;
		if (!blocks.isEmpty()) {
			for (Block block : blocks) {
				List<Var> blockInVars = bodyBlocksOutputs.get(block);
				if (blockInVars.contains(var)) {
					return true;
				}
			}
		} else {
			// First or Last Block of Blocks
			List<Var> vars = bodyBlocksOutputs.get(currentBlock);
			if (vars.contains(var)) {
				return true;
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
	public List<Var> getDecisionOutputs() {
		if (!blockWhile.hasAttribute("decisionOutputs")) {
			doSwitch(blockWhile);
		}
		return (List<Var>) blockWhile.getAttribute("decisionOutputs")
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
