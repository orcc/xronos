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

import net.sf.orcc.df.Action;
import net.sf.orcc.df.Port;
import net.sf.orcc.df.util.DfVisitor;
import net.sf.orcc.ir.Block;
import net.sf.orcc.ir.BlockWhile;
import net.sf.orcc.ir.InstStore;
import net.sf.orcc.ir.Procedure;
import net.sf.orcc.ir.util.AbstractIrVisitor;
import net.sf.orcc.util.OrccLogger;

/**
 * This transformation will add a local list variable and while loop if an
 * action has a repeat
 * 
 * @author Endri Bezati
 * 
 */
public class XronosRepeatFixer extends DfVisitor<Void> {

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

	@Override
	public Void caseAction(Action action) {
		DetectPhalogicalRepeat detect = new DetectPhalogicalRepeat();
		Boolean result = detect.doSwitch(action);
		// If yes add the necessary missing local variable and loop
		if (result) {
			OrccLogger.warnln("Xronos: Action" + " " + action.getName()
					+ ", pathological repeat detected!");
		}
		return null;
	}

}
