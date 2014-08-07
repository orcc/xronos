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
package org.xronos.orcc.forge.scheduler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sf.orcc.df.Action;
import net.sf.orcc.df.Actor;
import net.sf.orcc.df.Port;
import net.sf.orcc.df.util.DfVisitor;
import net.sf.orcc.ir.Block;
import net.sf.orcc.ir.BlockBasic;
import net.sf.orcc.ir.Expression;
import net.sf.orcc.ir.InstStore;
import net.sf.orcc.ir.IrFactory;
import net.sf.orcc.ir.Procedure;
import net.sf.orcc.ir.Var;

import org.xronos.orcc.forge.mapping.DesignMemory;

/**
 * This visitor will create a BlockBasic if an actor contains action that have
 * repeats on input and output.
 * 
 * @author Endri Bezati
 *
 */
public class TokenIndexBlock extends DfVisitor<Block> {

	/**
	 * The Actions Scheduler procedure
	 */
	Procedure scheduler;

	public TokenIndexBlock(Procedure scheduler) {
		this.scheduler = scheduler;
	}

	@Override
	public Block caseActor(Actor actor) {
		Map<Port, Boolean> portHasRepeat = new HashMap<Port, Boolean>();

		// -- Find if a port has repeats on an Action
		for (Action action : actor.getActions()) {
			for (Port port : action.getInputPattern().getPorts()) {
				if (action.getInputPattern().getNumTokens(port) > 1) {
					portHasRepeat.put(port, true);
				} else {
					if (!portHasRepeat.containsKey(port)) {
						portHasRepeat.put(port, false);
					}
				}
			}

			for (Port port : action.getOutputPattern().getPorts()) {
				if (action.getOutputPattern().getNumTokens(port) > 1) {
					portHasRepeat.put(port, true);
				} else {
					if (!portHasRepeat.containsKey(port)) {
						portHasRepeat.put(port, false);
					}
				}
			}
		}

		List<Port> ports = new ArrayList<Port>();
		ports.addAll(actor.getInputs());
		ports.addAll(actor.getOutputs());

		BlockBasic block = IrFactory.eINSTANCE.createBlockBasic();

		// -- Create a store instruction for each TokenIndex
		for (Port port : ports) {
			if (portHasRepeat.containsKey(port)) {
				if (portHasRepeat.get(port)) {
					Var tokenIndex = IrFactory.eINSTANCE.createVar(
							IrFactory.eINSTANCE.createTypeInt(), port.getName()
									+ "TokenIndex", true, 0);
					actor.getStateVars().add(tokenIndex);
					DesignMemory.addToMemory(actor, tokenIndex);

					Expression value = IrFactory.eINSTANCE.createExprInt(0);
					InstStore store = IrFactory.eINSTANCE.createInstStore(
							tokenIndex, value);
					block.add(store);

					// -- Tmp TokenIndex
					Var tmpTokenIndex = IrFactory.eINSTANCE.createVar(
							IrFactory.eINSTANCE.createTypeInt(),
							"tmp_" + port.getName() + "TokenIndex", true, 0);
					scheduler.addLocal(tmpTokenIndex);
					
					// -- Create MaxTokenIndex variable for Input Ports
					Var maxTokenIndex = IrFactory.eINSTANCE.createVar(
							IrFactory.eINSTANCE.createTypeInt(), port.getName()
									+ "MaxTokenIndex", true, 0);
					actor.getStateVars().add(maxTokenIndex);
					DesignMemory.addToMemory(actor, maxTokenIndex);

					value = IrFactory.eINSTANCE.createExprInt(-1);
					store = IrFactory.eINSTANCE.createInstStore(maxTokenIndex,
							value);
					block.add(store);

					// -- Tmp MaxTokenIndex
					Var tmpMaxTokenIndex = IrFactory.eINSTANCE.createVar(
							IrFactory.eINSTANCE.createTypeInt(),
							"tmp_" + port.getName() + "MaxTokenIndex", true, 0);
					scheduler.addLocal(tmpMaxTokenIndex);
					
					// -- Port Enable
					Var portEnable = IrFactory.eINSTANCE.createVar(
							IrFactory.eINSTANCE.createTypeBool(),
							port.getName() + "PortEnable", true, 0);
					actor.getStateVars().add(portEnable);
					DesignMemory.addToMemory(actor, portEnable);
					store = IrFactory.eINSTANCE
							.createInstStore(portEnable,
									IrFactory.eINSTANCE
											.createExprBool(false));
					block.add(store);
					
				}
			}
		}

		return block;
	}

}
