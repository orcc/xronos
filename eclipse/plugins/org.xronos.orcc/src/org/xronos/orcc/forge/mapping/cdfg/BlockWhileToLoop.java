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
package org.xronos.orcc.forge.mapping.cdfg;

import java.util.ArrayList;
import java.util.List;

import net.sf.orcc.ir.BlockWhile;
import net.sf.orcc.ir.util.AbstractIrVisitor;

import org.xronos.openforge.lim.Block;
import org.xronos.openforge.lim.Component;
import org.xronos.openforge.lim.Loop;

/**
 * This Visitor transforms a BlockWhile to a LIM Loop
 * 
 * @author Endri Bezati
 * 
 */
public class BlockWhileToLoop extends AbstractIrVisitor<Loop> {

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
