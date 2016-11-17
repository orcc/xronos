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

package org.xronos.orcc.forge.mapping.cdfg;

import java.util.HashMap;
import java.util.Map;

import net.sf.orcc.ir.Procedure;
import net.sf.orcc.ir.Var;
import net.sf.orcc.ir.util.AbstractIrVisitor;

import org.xronos.openforge.lim.Block;
import org.xronos.openforge.lim.Bus;
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
	 * If this procedure is the body of an Action
	 */
	boolean isActionBody;

	/**
	 * Set of Block outputs
	 */
	Map<Var, Bus> outputs;

	/**
	 * Var target, data bus of a CAL function
	 */

	Var target;

	public ProcedureToBlock(boolean isActionBody) {
		this.isActionBody = isActionBody;
		this.inputs = new HashMap<Var, Port>();
		this.outputs = new HashMap<Var, Bus>();
	}

	/**
	 * Create a Block from a procedure
	 * 
	 * @param isActionBody
	 */

	public ProcedureToBlock(Var target) {
		this(true);
		this.target = target;
	}

	@Override
	public Block caseProcedure(Procedure procedure) {

		Block proceduralBlock = (Block) ((target == null) ? new BlocksToBlock(
				inputs, outputs, isActionBody).doSwitch(procedure.getBlocks())
				: new BlocksToBlock(inputs, outputs, target).doSwitch(procedure
						.getBlocks()));

		procedure.setAttribute("inputs", inputs);
		procedure.setAttribute("outputs", outputs);
		return proceduralBlock;
	}

}
