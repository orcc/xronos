/* 
 * XRONOS-EXELIXI
 * 
 * Copyright (C) 2011-2016 EPFL SCI STI MM
 *
 * This file is part of XRONOS-EXELIXI.
 *
 * XRONOS-EXELIXI is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * XRONOS-EXELIXI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with XRONOS-EXELIXI. If not, see <http://www.gnu.org/licenses/>.
 * 
 * Additional permission under GNU GPL version 3 section 7
 * 
 * If you modify this Program, or any covered work, by linking or combining it
 * with Eclipse (or a modified version of Eclipse or an Eclipse plugin or 
 * an Eclipse library), containing parts covered by the terms of the 
 * Eclipse Public License (EPL), the licensors of this Program grant you 
 * additional permission to convey the resulting work.  Corresponding Source 
 * for a non-source form of such a combination shall include the source code 
 * for the parts of Eclipse libraries used as well as that of the covered work.
 * 
 */
package org.xronos.orcc.forge.scheduler;

import net.sf.orcc.df.Actor;
import net.sf.orcc.df.util.DfVisitor;
import net.sf.orcc.ir.Block;
import net.sf.orcc.ir.BlockBasic;
import net.sf.orcc.ir.InstLoad;
import net.sf.orcc.ir.IrFactory;
import net.sf.orcc.ir.Procedure;
import net.sf.orcc.ir.Var;

/**
 * This visitor creates a Block that contains all the load instructions for
 * loading the current value of all states.
 * 
 * @author Endri Bezati
 *
 */
public class LoadCurrentStateBlock extends DfVisitor<Block> {

	/**
	 * The Actions Scheduler procedure
	 */
	Procedure scheduler;

	public LoadCurrentStateBlock(Procedure scheduler) {
		this.scheduler = scheduler;
	}

	@Override
	public Block caseActor(Actor actor) {
		BlockBasic block = IrFactory.eINSTANCE.createBlockBasic();
		
		Var currentState = IrFactory.eINSTANCE.createVar(
				IrFactory.eINSTANCE.createTypeInt(),
				"fsmCurrentState_" + actor.getName(), true, 0);
		scheduler.addLocal(currentState);
		Var state = actor.getStateVar("fsmState_" + actor.getName());
		
		InstLoad load = IrFactory.eINSTANCE.createInstLoad(currentState, state);
		block.add(load);
		
		return block;
	}

}
