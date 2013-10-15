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
package org.xronos.orcc.backend.transform;

import java.util.List;

import net.sf.orcc.ir.Block;
import net.sf.orcc.ir.BlockIf;
import net.sf.orcc.ir.Expression;
import net.sf.orcc.ir.util.AbstractIrVisitor;
import net.sf.orcc.ir.util.ValueUtil;
import net.sf.orcc.util.util.EcoreHelper;

public class XronosDeadCodeElimination extends AbstractIrVisitor<Void> {

	@Override
	public Void caseBlockIf(BlockIf blockIf) {
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

					// 3. Add the blocks to the parents one
					parentBlocks.addAll(indexBlock, thenBlocks);

					// 4. Remove all blocks from the else
					parentBlocks.remove(blockIf);

				} else {
					// 1. Get parent Blocks
					List<Block> parentBlocks = EcoreHelper
							.getContainingList(blockIf);
					if (!blockIf.getElseBlocks().isEmpty()) {

						// 2. Get then Blocks
						List<Block> elseBlocks = blockIf.getElseBlocks();

						// 3. Add the blocks to the parents one
						parentBlocks.addAll(indexBlock, elseBlocks);
					}
					// 5. Remove all blocks from the else
					parentBlocks.remove(blockIf);
				}
			}
		}

		return null;
	}
}