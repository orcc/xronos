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
 */
package org.xronos.orcc.backend.transform;

import java.util.List;

import net.sf.orcc.ir.Block;
import net.sf.orcc.ir.BlockBasic;
import net.sf.orcc.ir.BlockIf;
import net.sf.orcc.ir.BlockWhile;
import net.sf.orcc.ir.ExprVar;
import net.sf.orcc.ir.Expression;
import net.sf.orcc.ir.InstAssign;
import net.sf.orcc.ir.InstPhi;
import net.sf.orcc.ir.IrFactory;
import net.sf.orcc.ir.Procedure;
import net.sf.orcc.ir.Var;
import net.sf.orcc.ir.util.AbstractIrVisitor;
import net.sf.orcc.ir.util.IrUtil;
import net.sf.orcc.ir.util.ValueUtil;
import net.sf.orcc.util.OrccLogger;
import net.sf.orcc.util.Void;
import net.sf.orcc.util.util.EcoreHelper;

public class XronosDeadCodeElimination extends AbstractIrVisitor<Void> {

	private class PhiToAssignBlock extends AbstractIrVisitor<BlockBasic> {

		private Integer phiIndex;

		private PhiToAssignBlock(Integer phiIndex) {
			this.phiIndex = phiIndex;
		}

		private BlockBasic phiToAssignBlock;

		@Override
		public BlockBasic caseBlockIf(BlockIf blockIf) {
			phiToAssignBlock = IrFactory.eINSTANCE.createBlockBasic();
			doSwitch(blockIf.getJoinBlock());
			return phiToAssignBlock;
		}

		@Override
		public BlockBasic caseBlockWhile(BlockWhile blockWhile) {
			phiToAssignBlock = IrFactory.eINSTANCE.createBlockBasic();
			doSwitch(blockWhile.getJoinBlock());
			return phiToAssignBlock;
		}

		@Override
		public BlockBasic caseInstPhi(InstPhi phi) {
			Var target = phi.getTarget().getVariable();
			ExprVar sourceExpr = (ExprVar) phi.getValues().get(phiIndex);
			Var source = sourceExpr.getUse().getVariable();
			Expression expr = IrFactory.eINSTANCE.createExprVar(source);
			InstAssign assign = IrFactory.eINSTANCE.createInstAssign(target,
					expr);
			phiToAssignBlock.add(assign);
			return null;
		}

	}

	Boolean debug;

	Boolean modified;

	Boolean phiToAssign;

	public XronosDeadCodeElimination() {
		this(false);
	}

	public XronosDeadCodeElimination(Boolean debug) {
		this.debug = debug;
	}

	public XronosDeadCodeElimination(Boolean debug, Boolean phiToAssign) {
		this.debug = debug;
		this.phiToAssign = phiToAssign;
	}

	@Override
	public Void caseBlockIf(BlockIf blockIf) {
		doSwitch(blockIf.getThenBlocks());
		doSwitch(blockIf.getElseBlocks());
		Expression condition = blockIf.getCondition();
		XronosExprEvaluator exprEvaluator = new XronosExprEvaluator();
		Object value = exprEvaluator.doSwitch(condition);
		if (value != null) {
			if (ValueUtil.isBool(value)) {
				Boolean val = (Boolean) value;
				if (val) {
					// 1. Get parent Blocks
					List<Block> parentBlocks = EcoreHelper
							.getContainingList(blockIf);

					// 2. Get then Blocks
					List<Block> thenBlocks = blockIf.getThenBlocks();
					Integer thenBlocksSize = thenBlocks.size();
					// 3. Add the blocks to the parents one
					parentBlocks.addAll(indexBlock, thenBlocks);

					// Add the Phi Blocks
					if (phiToAssign) {
						PhiToAssignBlock phiToAssignBlock = new PhiToAssignBlock(
								0);
						BlockBasic truePhiBlock = phiToAssignBlock
								.doSwitch(blockIf);
						parentBlocks.add(indexBlock + thenBlocksSize,
								truePhiBlock);
					}
					// 4. Remove all blocks from the else
					parentBlocks.remove(blockIf);
					IrUtil.delete(blockIf);
					modified = true;
					if (debug) {
						OrccLogger.warnln("Xronos: BlockIf line: "
								+ blockIf.getLineNumber()
								+ " removed, all then blocks copied");
					}

				} else {
					// 1. Get parent Blocks
					List<Block> parentBlocks = EcoreHelper
							.getContainingList(blockIf);
					if (!blockIf.getElseBlocks().isEmpty()) {

						// 2. Get then Blocks
						List<Block> elseBlocks = blockIf.getElseBlocks();
						Integer elseBlocksSize = elseBlocks.size();
						// 3. Add the blocks to the parents one
						parentBlocks.addAll(indexBlock, elseBlocks);

						// 4. Add the Phi to Assign Blocks
						// Add the Phi Blocks
						if (phiToAssign) {
							PhiToAssignBlock phiToAssignBlock = new PhiToAssignBlock(
									1);
							BlockBasic truePhiBlock = phiToAssignBlock
									.doSwitch(blockIf);
							parentBlocks.add(indexBlock + elseBlocksSize,
									truePhiBlock);
						}
						modified = true;
						if (debug) {
							OrccLogger.warnln("Xronos: BlockIf line: "
									+ blockIf.getLineNumber()
									+ " removed, all else blocks copied");
						}
					} else {
						if (phiToAssign) {
							if (!blockIf.getJoinBlock().getInstructions()
									.isEmpty()) {
								PhiToAssignBlock phiToAssignBlock = new PhiToAssignBlock(
										1);
								BlockBasic truePhiBlock = phiToAssignBlock
										.doSwitch(blockIf);
								parentBlocks.add(indexBlock, truePhiBlock);
							}
						}
					}
					// 5. Remove all blocks from the else
					parentBlocks.remove(blockIf);
					IrUtil.delete(blockIf);
					modified = true;
					if (debug) {
						OrccLogger.warnln("Xronos: BlockIf line: "
								+ blockIf.getLineNumber() + " removed");
					}
				}
			}
		}

		return null;
	}

	@Override
	public Void caseBlockWhile(BlockWhile blockWhile) {
		// Visit the loop to find if there is another branch or loop to be
		// eliminated
		doSwitch(blockWhile.getBlocks());

		// Now do the work
		Expression condition = blockWhile.getCondition();
		XronosExprEvaluator exprEvaluator = new XronosExprEvaluator();
		Object value = exprEvaluator.doSwitch(condition);
		if (value != null) {
			if (ValueUtil.isBool(value)) {
				Boolean val = (Boolean) value;
				if (!val) {
					// 1. Get parent Blocks
					List<Block> parentBlocks = EcoreHelper
							.getContainingList(blockWhile);
					// 2. Remove all blocks from the else
					parentBlocks.remove(blockWhile);
					IrUtil.delete(blockWhile);
					modified = true;
					if (debug) {
						OrccLogger.warnln("Xronos: Loop line: "
								+ blockWhile.getLineNumber() + " removed");
					}
				}
			}
		}
		return null;
	}

	@Override
	public Void caseProcedure(Procedure procedure) {
		do {
			modified = false;
			super.caseProcedure(procedure);
		} while (modified);

		return null;
	}

}