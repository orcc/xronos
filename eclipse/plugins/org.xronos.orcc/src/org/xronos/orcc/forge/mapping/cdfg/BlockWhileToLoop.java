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

package org.xronos.orcc.forge.mapping.cdfg;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.sf.orcc.ir.BlockWhile;
import net.sf.orcc.ir.Var;
import net.sf.orcc.ir.util.AbstractIrVisitor;

import org.xronos.openforge.lim.Block;
import org.xronos.openforge.lim.Bus;
import org.xronos.openforge.lim.Component;
import org.xronos.openforge.lim.Loop;
import org.xronos.openforge.lim.Port;

/**
 * This Visitor transforms a {@link BlockWhile} to a LIM {@link Loop}
 * 
 * @author Endri Bezati
 * 
 */
public class BlockWhileToLoop extends AbstractIrVisitor<Loop> {

	/**
	 * Set of Block inputs
	 */
	Map<Var, Port> inputs;

	/**
	 * Set of Block outputs
	 */
	Map<Var, Bus> outputs;

	@Override
	public Loop caseBlockWhile(BlockWhile blockWhile) {
		Loop loop = null;

		List<Component> sequence = new ArrayList<Component>();

		for (net.sf.orcc.ir.Block block : blockWhile.getBlocks()) {
			Component component = null;
			if (block.isBlockBasic()) {
				component = new BlockBasicToBlock().doSwitch(block);
			} else if (block.isBlockWhile()) {
				component = new BlockWhileToLoop().doSwitch(block);
			} else if (block.isBlockIf()) {

			}
			sequence.add(component);
		}

		Block loopBodyBlock = new Block(sequence);

		return loop;
	}

}
