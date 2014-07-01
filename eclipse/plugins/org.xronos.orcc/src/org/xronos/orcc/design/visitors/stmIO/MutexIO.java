/* 
 * XRONOS, High Level Synthesis of Streaming Applications
 * 
 * Copyright (C) 2014 EPFL SCI STI MM
 *
 * This file is part of XRONOS.
 *
 * XRONOS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * XRONOS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with XRONOS.  If not, see <http://www.gnu.org/licenses/>.
 * 
 * Additional permission under GNU GPL version 3 section 7
 * 
 * If you modify this Program, or any covered work, by linking or 
 * combining it with Eclipse libraries (or a modified version of that 
 * library), containing parts covered by the terms of EPL,
 * the licensors of this Program grant you additional permission to convey 
 * the resulting work. {Corresponding Source for a non-source form of such 
 * a combination shall include the source code for the parts of Eclipse 
 * libraries used as well as that of the  covered work.}
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
import net.sf.orcc.ir.Var;
import net.sf.orcc.ir.util.AbstractIrVisitor;
import net.sf.orcc.util.Attribute;

import org.eclipse.emf.ecore.EObject;
import org.xronos.orcc.ir.BlockMutex;

/**
 * This visitor finds the Inputs and Outputs of Mutex Block
 * 
 * @author Endri Bezati
 * 
 */
public class MutexIO extends AbstractIrVisitor<Void> {

	private BlockMutex blockMutex;

	private Map<Block, List<Var>> bodyBlocksInputs;

	private Map<Block, List<Var>> bodyBlocksOutputs;

	public MutexIO(BlockMutex blockMutex) {
		super(true);
		this.blockMutex = blockMutex;
		bodyBlocksInputs = new HashMap<Block, List<Var>>();
		bodyBlocksOutputs = new HashMap<Block, List<Var>>();
	}

	@SuppressWarnings("unchecked")
	@Override
	public Void caseBlockBasic(BlockBasic block) {
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

			List<Var> inputs = new ArrayList<Var>();
			List<Var> outputs = new ArrayList<Var>();
			for (Block block : blockMutex.getBlocks()) {
				doSwitch(block);
				List<Var> blockInputs = bodyBlocksInputs.get(block);
				List<Var> blockOutputs = bodyBlocksOutputs.get(block);

				// Outputs
				outputs.addAll(blockOutputs);

				// Inputs
				for (Var var : blockInputs) {
					if (!inputs.contains(var)) {
						inputs.add(var);
					}
				}
			}

			blockMutex.setAttribute("inputs", inputs);
			blockMutex.setAttribute("outputs", outputs);

			bodyBlocksInputs.put(blockMutex, inputs);
			bodyBlocksOutputs.put(blockMutex, outputs);

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
	public Void defaultCase(EObject object) {
		if (object instanceof BlockMutex) {
			caseBlockMutex((BlockMutex) object);
		}
		return super.defaultCase(object);
	}

	@SuppressWarnings("unchecked")
	public List<Var> getInputs() {
		if (!blockMutex.hasAttribute("inputs")) {
			doSwitch(blockMutex);
		}
		return (List<Var>) blockMutex.getAttribute("inputs").getObjectValue();
	}

	@SuppressWarnings("unchecked")
	public List<Var> getOutputs() {
		if (!blockMutex.hasAttribute("outputs")) {
			doSwitch(blockMutex);
		}
		return (List<Var>) blockMutex.getAttribute("outputs").getObjectValue();
	}
}
