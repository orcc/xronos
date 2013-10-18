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

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import net.sf.orcc.df.Action;
import net.sf.orcc.df.Actor;
import net.sf.orcc.df.Port;
import net.sf.orcc.df.util.DfVisitor;
import net.sf.orcc.ir.Block;
import net.sf.orcc.ir.BlockBasic;
import net.sf.orcc.ir.BlockIf;
import net.sf.orcc.ir.BlockWhile;
import net.sf.orcc.ir.Def;
import net.sf.orcc.ir.ExprBinary;
import net.sf.orcc.ir.ExprInt;
import net.sf.orcc.ir.ExprVar;
import net.sf.orcc.ir.Expression;
import net.sf.orcc.ir.InstAssign;
import net.sf.orcc.ir.InstLoad;
import net.sf.orcc.ir.InstStore;
import net.sf.orcc.ir.Instruction;
import net.sf.orcc.ir.IrFactory;
import net.sf.orcc.ir.OpBinary;
import net.sf.orcc.ir.Procedure;
import net.sf.orcc.ir.Type;
import net.sf.orcc.ir.Use;
import net.sf.orcc.ir.Var;
import net.sf.orcc.ir.util.AbstractIrVisitor;
import net.sf.orcc.ir.util.IrUtil;

import org.eclipse.emf.common.util.EList;
import org.xronos.orcc.design.util.XronosIrUtil;
import org.xronos.orcc.ir.InstPortStatus;
import org.xronos.orcc.ir.InstPortWrite;

/**
 * This transformation will add a local list variable and while loop if an
 * action has a repeat
 * 
 * @author Endri Bezati
 * 
 */
public class OutputRepeatPattern extends DfVisitor<Void> {

	public class DetectPhalogicalRepeat extends DfVisitor<Boolean> {

		public class Detector extends AbstractIrVisitor<Boolean> {

			@Override
			public Boolean caseBlockWhile(BlockWhile blockWhile) {
				return false;
			}

			@Override
			public Boolean caseInstStore(InstStore store) {
				return false;
			}

			@Override
			public Boolean caseProcedure(Procedure procedure) {
				this.procedure = procedure;

				// Test if The procedure contains a BlockWhile before size - 1
				int blockSize = procedure.getBlocks().size();
				if (blockSize > 1) {
					Block block = procedure.getBlocks().get(blockSize - 2);
					if (block.isBlockWhile()) {
						// If it is check if an additional loop should be added
						doSwitch(block);
					} else {
						return true;
					}
				}

				return false;
			}

		}

		@Override
		public Boolean caseAction(Action action) {
			for (Port port : action.getOutputPattern().getPorts()) {
				int numTokens = action.getOutputPattern().getNumTokensMap()
						.get(port);
				if (numTokens > 1) {
					Detector detector = new Detector();
					return detector.doSwitch(action.getBody());
				}
			}
			return false;
		}
	}

	private class FindMaxRepeat extends DfVisitor<Map<Port, Integer>> {

		@Override
		public Map<Port, Integer> caseAction(Action action) {
			for (Port port : action.getOutputPattern().getPorts()) {
				int numTokens = action.getOutputPattern().getNumTokensMap()
						.get(port);
				if (portRepeat.containsKey(port)) {
					if (portRepeat.get(port) < numTokens) {
						portRepeat.put(port, numTokens);
					}
				} else {
					portRepeat.put(port, numTokens);
				}

			}
			return null;
		}

		@Override
		public Map<Port, Integer> caseActor(Actor actor) {
			portRepeat = new HashMap<Port, Integer>();
			super.caseActor(actor);
			return portRepeat;
		}

	}

	private Map<Port, Integer> portRepeat;

	private Map<Port, Var> newPortToVarMap;

	@Override
	public Void caseAction(Action action) {
		// FIXME: Do not detect a pathological case for the moment,
		// extra clock cycles

		for (Port port : action.getOutputPattern().getPorts()) {
			int size = action.getOutputPattern().getNumTokensMap().get(port);
			if (size > 1) {
				Var newVar = newPortToVarMap.get(port);
				Var var = action.getOutputPattern().getPortToVarMap().get(port);

				EList<Use> uses = var.getUses();
				while (!uses.isEmpty()) {
					uses.get(0).setVariable(newVar);
				}
				EList<Def> defs = var.getDefs();
				while (!defs.isEmpty()) {
					defs.get(0).setVariable(newVar);
				}

				// Create the Block while for the repeat
				BlockWhile whileBlock = IrFactory.eINSTANCE.createBlockWhile();
				whileBlock.setJoinBlock(IrFactory.eINSTANCE.createBlockBasic());

				// Create the condition
				Type type = IrFactory.eINSTANCE.createTypeInt();
				Var index = IrFactory.eINSTANCE.createVar(type, "rp_Index_"
						+ port.getName(), true, 0);

				action.getBody().getLocals().add(index);

				ExprVar condE0 = IrFactory.eINSTANCE.createExprVar(index);
				ExprInt condE1 = IrFactory.eINSTANCE.createExprInt(size);

				ExprBinary whileCond = IrFactory.eINSTANCE.createExprBinary(
						condE0, OpBinary.LE, condE1,
						IrFactory.eINSTANCE.createTypeBool());

				whileBlock.setCondition(whileCond);

				// Create an if block with a pin Status as condition

				Var pinStatus = IrFactory.eINSTANCE.createVar(
						IrFactory.eINSTANCE.createTypeBool(), "portStatus_"
								+ port.getName(), true, 0);
				action.getBody().getLocals().add(pinStatus);

				// Add portStatus to the first block of the while
				BlockBasic firstWhileBlock = IrFactory.eINSTANCE
						.createBlockBasic();
				InstPortStatus portStatus = XronosIrUtil.createInstPortStatus(
						pinStatus, port);
				firstWhileBlock.add(portStatus);
				whileBlock.getBlocks().add(firstWhileBlock);

				BlockIf blockIf = IrFactory.eINSTANCE.createBlockIf();
				blockIf.setJoinBlock(IrFactory.eINSTANCE.createBlockBasic());

				ExprVar ifCond = IrFactory.eINSTANCE.createExprVar(pinStatus);
				blockIf.setCondition(ifCond);

				// -----------------------------------------------------------
				// Then Block

				// Create While blockBasic
				BlockBasic thenBlock = IrFactory.eINSTANCE.createBlockBasic();

				// Create a Load instruction
				Type tmpLoadType = IrUtil.copy(port.getType());
				Var tmpLoad = IrFactory.eINSTANCE.createVar(tmpLoadType,
						"tmp_rp_" + port.getName(), true, 0);
				action.getBody().getLocals().add(tmpLoad);

				ExprVar loadIndex = IrFactory.eINSTANCE.createExprVar(index);

				InstLoad load = IrFactory.eINSTANCE.createInstLoad(tmpLoad,
						newVar, Arrays.asList((Expression) loadIndex));

				thenBlock.add(load);

				// Create the Port Write instruction
				InstPortWrite portWrite = XronosIrUtil.createInstPortWrite(
						port, tmpLoad, false);

				thenBlock.add(portWrite);

				// Create the instruction assign with index++
				ExprVar assignE0 = IrFactory.eINSTANCE.createExprVar(index);
				ExprInt assignE1 = IrFactory.eINSTANCE.createExprInt(1);

				ExprBinary assignValue = IrFactory.eINSTANCE.createExprBinary(
						assignE0, OpBinary.PLUS, assignE1,
						IrFactory.eINSTANCE.createTypeInt());

				InstAssign assignIndex = IrFactory.eINSTANCE.createInstAssign(
						index, assignValue);
				thenBlock.add(assignIndex);

				// -----------------------------------------------------------

				// Add to thenBlocks the thenBlock
				blockIf.getThenBlocks().add(thenBlock);

				// Add the ifBlock to the Block While
				whileBlock.getBlocks().add(blockIf);

				// Add the BlockWhile to the body of the procedure
				int indexBlock = action.getBody().getBlocks().size();
				Block lastBlock = action.getBody().getBlocks()
						.get(indexBlock - 1);
				if (isSinlgeReturnBlock(lastBlock)) {
					action.getBody().getBlocks()
							.add(indexBlock - 1, whileBlock);
				} else {
					if (lastBlock.isBlockBasic()) {
						BlockBasic basic = (BlockBasic) lastBlock;
						int instrSize = basic.getInstructions().size();
						if (basic.getInstructions().get(instrSize - 1)
								.isInstReturn()) {
							Instruction returnInst = basic.getInstructions()
									.get(instrSize - 1);
							BlockBasic newLastBlock = IrFactory.eINSTANCE
									.createBlockBasic();
							newLastBlock.add(returnInst);
							action.getBody().getBlocks().add(whileBlock);
							action.getBody().getBlocks().add(newLastBlock);
						}
					}
				}
			}

		}

		return null;
	}

	@Override
	public Void caseActor(Actor actor) {
		// Find the maximum output repeat values
		FindMaxRepeat findMaxRepeat = new FindMaxRepeat();
		findMaxRepeat.doSwitch(actor);

		newPortToVarMap = new HashMap<Port, Var>();
		for (Port port : portRepeat.keySet()) {
			int size = portRepeat.get(port);
			if (size > 1) {
				Type typePort = IrUtil.copy(port.getType());
				Type type = IrFactory.eINSTANCE.createTypeList(size, typePort);
				Var var = IrFactory.eINSTANCE.createVar(type,
						"rp_" + port.getName(), false, 0);
				newPortToVarMap.put(port, var);
				// Add to actor state variables
				actor.getStateVars().add(var);
			}
		}
		// Now Change the ator
		super.caseActor(actor);
		return null;
	}

	private boolean isSinlgeReturnBlock(Block block) {
		if (block.isBlockBasic()) {
			BlockBasic basic = (BlockBasic) block;
			if (basic.getInstructions().size() == 1) {
				Instruction instruction = basic.getInstructions().get(0);
				if (instruction.isInstReturn()) {
					return true;
				}
			}
		}

		return false;
	}

}
