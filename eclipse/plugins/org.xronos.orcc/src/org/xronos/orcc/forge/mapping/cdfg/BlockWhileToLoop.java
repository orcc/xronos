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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import net.sf.orcc.ir.BlockWhile;
import net.sf.orcc.ir.Var;
import net.sf.orcc.ir.util.AbstractIrVisitor;

import org.xronos.openforge.lim.Block;
import org.xronos.openforge.lim.Bus;
import org.xronos.openforge.lim.Component;
import org.xronos.openforge.lim.Decision;
import org.xronos.openforge.lim.Dependency;
import org.xronos.openforge.lim.Exit;
import org.xronos.openforge.lim.InBuf;
import org.xronos.openforge.lim.Loop;
import org.xronos.openforge.lim.OutBuf;
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

		// Construct decision from the block while condition
		Block decisionBlock = (Block) new ExprToComponent().doSwitch(blockWhile
				.getCondition());

		Component decisionComponent = findDecisionComponent(decisionBlock);

		Decision decision = new Decision(decisionBlock, decisionComponent);

		// Construct Loop Body Block from the block while blocks

		Map<Var, Port> lbInputs = new HashMap<Var, Port>();
		Map<Var, Bus> lbOutputs = new HashMap<Var, Bus>();
		Component loopBody = new BlocksToBlock(lbInputs, lbOutputs, false)
				.doSwitch(blockWhile.getBlocks());

		return loop;
	}

	private Component findDecisionComponent(Block decisionBlock) {
		// Decision block contains olny one result bus
		Bus resultBus = decisionBlock.getExit(Exit.DONE).getDataBuses().get(0);
		Port resultBusPeer = resultBus.getPeer();

		for (Component component : decisionBlock.getComponents()) {
			if (!(component instanceof InBuf) && !(component instanceof OutBuf)) {
				for (Bus bus : component.getExit(Exit.DONE).getDataBuses()) {
					Collection<Dependency> deps = bus.getLogicalDependents();
					for (Dependency dep : deps) {
						Port port = dep.getPort();
						if (port == resultBusPeer) {
							return component;
						}
					}
				}
			}
		}

		return null;
	}

}
