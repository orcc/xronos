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

package org.xronos.orcc.backend.transform;

import java.util.List;

import net.sf.orcc.ir.Block;
import net.sf.orcc.ir.BlockIf;
import net.sf.orcc.ir.BlockWhile;
import net.sf.orcc.ir.InstPhi;
import net.sf.orcc.ir.Procedure;
import net.sf.orcc.ir.Var;
import net.sf.orcc.ir.util.AbstractIrVisitor;
import net.sf.orcc.ir.util.IrUtil;

/**
 * A dead phi code elimination,
 * 
 * @author Endri Bezati
 * 
 */
public class DeadPhiElimination extends AbstractIrVisitor<Void> {

	boolean changed = false;

	@Override
	public Void caseBlockIf(BlockIf blockIf) {
		// visit Join Block
		doSwitch(blockIf.getJoinBlock());
		if (changed) {
			changed = false;
			doSwitch(blockIf);
		}
		doSwitch(blockIf.getThenBlocks());
		if (changed) {
			changed = false;
			doSwitch(blockIf);
		}

		doSwitch(blockIf.getElseBlocks());
		if (changed) {
			changed = false;
			doSwitch(blockIf);
		}

		return null;
	}

	@Override
	public Void caseBlockWhile(BlockWhile blockWhile) {
		// Visit the join block in the begining
		doSwitch(blockWhile.getJoinBlock());

		doSwitch(blockWhile.getBlocks());
		if (changed) {
			changed = false;
			doSwitch(blockWhile);
		}
		return null;
	}

	@Override
	public Void caseInstPhi(InstPhi phi) {
		Var target = phi.getTarget().getVariable();
		if ((target != null) && !target.isUsed()) {
			IrUtil.delete(phi);
			changed = true;
			indexInst--;
		}
		return null;
	}

	@Override
	public Void caseProcedure(Procedure procedure) {
		this.procedure = procedure;
		return super.caseProcedure(procedure);
	}

	@Override
	public Void doSwitch(List<Block> blocks) {
		return visitBlocksReverse(blocks);
	}

}
