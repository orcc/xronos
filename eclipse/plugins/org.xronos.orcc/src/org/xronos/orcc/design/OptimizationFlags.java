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
 * If you modify this Program, or any covered work, by linking or combining it
 * with Eclipse (or a modified version of Eclipse or an Eclipse plugin or 
 * an Eclipse library), containing parts covered by the terms of the 
 * Eclipse Public License (EPL), the licensors of this Program grant you 
 * additional permission to convey the resulting work.  Corresponding Source 
 * for a non-source form of such a combination shall include the source code 
 * for the parts of Eclipse libraries used as well as that of the  covered work.
 * 
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
