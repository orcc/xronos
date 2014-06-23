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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sf.orcc.ir.BlockBasic;
import net.sf.orcc.ir.BlockIf;
import net.sf.orcc.ir.BlockWhile;
import net.sf.orcc.ir.Procedure;
import net.sf.orcc.ir.Var;
import net.sf.orcc.ir.util.AbstractIrVisitor;

import org.xronos.openforge.lim.Block;
import org.xronos.openforge.lim.Bus;
import org.xronos.openforge.lim.Component;
import org.xronos.openforge.lim.Port;

/**
 * This visitor transforms the body blocks of an procedure to a LIM
 * {@link Block} and resolve the dependencies between them.
 * 
 * @author Endri Bezati
 * 
 */
public class ProcedureToBlock extends AbstractIrVisitor<Block> {

	/**
	 * Set of Block inputs
	 */
	Map<Var, Port> inputs;

	/**
	 * Set of Block outputs
	 */
	Map<Var, Bus> outputs;

	/**
	 * If this procedure is the body of an Action
	 */
	boolean isActionBody;

	public ProcedureToBlock(boolean isActionBody) {
		this.isActionBody = isActionBody;
	}

	@Override
	public Block caseBlockBasic(BlockBasic block) {
		BlockBasicToBlock blockBasicToBlock = new BlockBasicToBlock();
		return (Block) blockBasicToBlock.doSwitch(block);
	}

	@Override
	public Block caseBlockIf(BlockIf blockIf) {
		// TODO Auto-generated method stub
		return super.caseBlockIf(blockIf);
	}

	@Override
	public Block caseBlockWhile(BlockWhile blockWhile) {
		// TODO Auto-generated method stub
		return super.caseBlockWhile(blockWhile);
	}

	@Override
	public Block caseProcedure(Procedure procedure) {
		// TODO: Resolve Dependencies for each block
		Map<net.sf.orcc.ir.Block, Component> blockComponents = new HashMap<net.sf.orcc.ir.Block, Component>();
		List<Component> sequence = new ArrayList<Component>();
		for (net.sf.orcc.ir.Block block : procedure.getBlocks()) {
			Component component = doSwitch(block);
			blockComponents.put(block, component);
			sequence.add(component);
		}

		Block proceduralBlock = new Block(sequence, isActionBody);

		return proceduralBlock;
	}

}
