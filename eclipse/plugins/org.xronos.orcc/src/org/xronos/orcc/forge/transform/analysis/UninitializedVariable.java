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
package org.xronos.orcc.forge.transform.analysis;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Set;

import net.sf.orcc.ir.Block;
import net.sf.orcc.ir.BlockBasic;
import net.sf.orcc.ir.Cfg;
import net.sf.orcc.ir.CfgNode;
import net.sf.orcc.ir.Expression;
import net.sf.orcc.ir.InstAssign;
import net.sf.orcc.ir.IrFactory;
import net.sf.orcc.ir.Procedure;
import net.sf.orcc.ir.Type;
import net.sf.orcc.ir.Var;
import net.sf.orcc.ir.util.AbstractIrVisitor;
import net.sf.orcc.util.OrccLogger;
import net.sf.orcc.util.Void;

/**
 * This transformation finds out which variable is not initialized and it
 * initialize them with 0, it uses CFG and Liveness.
 * 
 * @author Endri Bezati
 *
 */
public class UninitializedVariable extends AbstractIrVisitor<Void> {

	boolean debug;

	public UninitializedVariable(boolean debug) {
		this.debug = debug;
	}

	public UninitializedVariable() {
		this(false);
	}

	@Override
	public Void caseProcedure(Procedure procedure) {
		Cfg cfg = procedure.getCfg();

		Block block = ((CfgNode) cfg.getEntry().getSuccessors().get(0))
				.getNode();
		if (block.isBlockBasic()) {
			@SuppressWarnings("unchecked")
			Set<Var> varKill = (Set<Var>) block.getAttribute("Kill")
					.getObjectValue();
			@SuppressWarnings("unchecked")
			Set<Var> liveOut = (Set<Var>) block.getAttribute("LiveOut")
					.getObjectValue();

			for (Var var : liveOut) {
				if (!varKill.contains(var)) {
					if (debug) {
						OrccLogger.warnln("Variable: " + var.getName()
								+ ", line : " + procedure.getLineNumber()
								+ ", is not initialized, initializing !!!");
					}
					Type type = var.getType();
					Expression expr;
					if (type.isBool()) {
						expr = IrFactory.eINSTANCE.createExprBool(false);
					} else if (type.isFloat()) {
						expr = IrFactory.eINSTANCE
								.createExprFloat(BigDecimal.ZERO);
					} else if (type.isInt() || type.isUint()) {
						expr = IrFactory.eINSTANCE
								.createExprInt(BigInteger.ZERO);
					} else if (type.isString()) {
						expr = IrFactory.eINSTANCE.createExprString("");
					} else {
						throw new NullPointerException("No known Type");
					}
					InstAssign assign = IrFactory.eINSTANCE.createInstAssign(
							var, expr);
					((BlockBasic) block).add(0, assign);

				}
			}

		} else {

		}

		return null;
	}

}
