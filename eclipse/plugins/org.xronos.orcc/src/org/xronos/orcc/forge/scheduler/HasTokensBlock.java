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

import net.sf.orcc.df.Action;
import net.sf.orcc.df.Actor;
import net.sf.orcc.df.Port;
import net.sf.orcc.df.util.DfVisitor;
import net.sf.orcc.ir.Block;
import net.sf.orcc.ir.BlockBasic;
import net.sf.orcc.ir.Expression;
import net.sf.orcc.ir.InstAssign;
import net.sf.orcc.ir.InstLoad;
import net.sf.orcc.ir.IrFactory;
import net.sf.orcc.ir.OpBinary;
import net.sf.orcc.ir.Procedure;
import net.sf.orcc.ir.Var;

/**
 * This visitor visits an actor and it creates a block that will contain for
 * each action a Boolean variable that satisfies the condition of number of
 * tokens necessary to execute an action.
 * 
 * @author Endri Bezati
 *
 */
public class HasTokensBlock extends DfVisitor<Block> {

	/**
	 * The Actions Scheduler procedure
	 */
	Procedure scheduler;

	public HasTokensBlock(Procedure scheduler) {
		this.scheduler = scheduler;
	}

	@Override
	public Block caseActor(Actor actor) {
		BlockBasic block = IrFactory.eINSTANCE.createBlockBasic();

		for (Action action : actor.getActions()) {
			Expression value = null;
			if (action.getInputPattern().getPorts().size() > 0) {
				for (Port port : action.getInputPattern().getPorts()) {
					if (action.getInputPattern().getNumTokens(port) == 1) {
						Var status = scheduler.getLocal(port.getName()
								+ "Status");
						Expression E = IrFactory.eINSTANCE
								.createExprVar(status);
						if (value == null) {
							value = E;
						} else {
							value = IrFactory.eINSTANCE.createExprBinary(value,
									OpBinary.LOGIC_AND, E,
									IrFactory.eINSTANCE.createTypeBool());
						}

					} else {
						Var tmpMaxportIndex = scheduler.getLocal("tmp_"
								+ port.getName() + "MaxTokenIndex");
						Var maxPortIndrex = actor.getStateVar(port.getName()
								+ "MaxTokenIndex");

						InstLoad load = IrFactory.eINSTANCE.createInstLoad(
								tmpMaxportIndex, maxPortIndrex);
						block.add(load);

						Var tmpPortIndex = scheduler.getLocal("tmp_"
								+ port.getName() + "TokenIndex");

						Var portIndrex = actor.getStateVar(port.getName()
								+ "TokenIndex");

						load = IrFactory.eINSTANCE.createInstLoad(tmpPortIndex,
								portIndrex);
						block.add(load);

						int nbrTokens = action.getInputPattern().getNumTokens(
								port);

						Expression E1 = IrFactory.eINSTANCE
								.createExprVar(tmpPortIndex);
						Expression E2 = IrFactory.eINSTANCE
								.createExprInt(nbrTokens);

						if (value == null) {
							value = IrFactory.eINSTANCE.createExprBinary(E1,
									OpBinary.EQ, E2,
									IrFactory.eINSTANCE.createTypeBool());
						} else {
							Expression nextE2 = IrFactory.eINSTANCE
									.createExprBinary(E1, OpBinary.EQ, E2,
											IrFactory.eINSTANCE
													.createTypeBool());
							value = IrFactory.eINSTANCE.createExprBinary(value,
									OpBinary.LOGIC_AND, nextE2,
									IrFactory.eINSTANCE.createTypeBool());
						}
					}
				}
			} else {
				value = IrFactory.eINSTANCE.createExprBool(true);
			}

			Var hasTokens = null;
			if (scheduler.getLocal(action.getName() + "HasTokens") != null) {
				hasTokens = scheduler.getLocal(action.getName() + "HasTokens");
			} else {
				hasTokens = IrFactory.eINSTANCE.createVar(
						IrFactory.eINSTANCE.createTypeBool(), action.getName()
								+ "HasTokens", true, 0);
				scheduler.addLocal(hasTokens);
			}

			InstAssign assign = IrFactory.eINSTANCE.createInstAssign(hasTokens,
					value);
			block.add(assign);
			
			
			if (action.getOutputPattern().getPorts().size() > 0) {
				for (Port port : action.getOutputPattern().getPorts()) {
					if (action.getOutputPattern().getNumTokens(port) > 1) {
						Var tmpMaxportIndex = scheduler.getLocal("tmp_"
								+ port.getName() + "MaxTokenIndex");
						Var maxPortIndrex = actor.getStateVar(port.getName()
								+ "MaxTokenIndex");

						InstLoad load = IrFactory.eINSTANCE.createInstLoad(
								tmpMaxportIndex, maxPortIndrex);
						block.add(load);

						Var tmpPortIndex = scheduler.getLocal("tmp_"
								+ port.getName() + "TokenIndex");

						Var portIndrex = actor.getStateVar(port.getName()
								+ "TokenIndex");

						load = IrFactory.eINSTANCE.createInstLoad(tmpPortIndex,
								portIndrex);
						block.add(load);
					}
				}
			}
			
		}

		return block;
	}
}
