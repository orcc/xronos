/*
 * Copyright (c) 2014, Ecole Polytechnique Fédérale de Lausanne
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
package org.xronos.orcc.design;

import java.util.UUID;

import net.sf.orcc.ir.BlockWhile;
import net.sf.orcc.ir.util.AbstractIrVisitor;
import net.sf.orcc.util.Void;

import org.xronos.openforge.app.EngineThread;
import org.xronos.openforge.app.OptionRegistry;
import org.xronos.openforge.app.project.Option;
import org.xronos.openforge.lim.Loop;

public class OptimizationFlags extends AbstractIrVisitor<Void> {

	@Override
	public Void caseBlockWhile(BlockWhile blockWhile) {

		if (blockWhile.hasAttribute("xronos_unroll")) {
			if (blockWhile.getAttribute("xronos_unroll").hasAttribute("limit")) {
				int limit = Integer.parseInt(blockWhile
						.getAttribute("xronos_unroll").getAttribute("limit")
						.getStringValue());
				if (blockWhile.hasAttribute("limLoop")) {
					Loop loop = (Loop) blockWhile.getAttribute("limLoop")
							.getObjectValue();
					loop.specifySearchScope("loop_line_"
							+ blockWhile.getLineNumber() + "_"
							+ UUID.randomUUID());

					Option op = EngineThread.getGenericJob().getOption(
							OptionRegistry.LOOP_UNROLLING_LIMIT);
					op.setValue(loop.getSearchLabel(), limit);
				}
			}
		}

		return null;
	}

}
