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
package org.xronos.orcc.forge.transform;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sf.orcc.ir.BlockBasic;
import net.sf.orcc.ir.InstLoad;
import net.sf.orcc.ir.InstStore;
import net.sf.orcc.ir.Instruction;
import net.sf.orcc.ir.Var;
import net.sf.orcc.ir.util.AbstractIrVisitor;
import net.sf.orcc.ir.util.IrUtil;
import net.sf.orcc.util.Void;

/**
 * This transformation eliminates instructions that defines a variable before
 * its used.
 * 
 * @author Endri Bezati
 *
 */
public class RedundantLoadElimination extends AbstractIrVisitor<Void> {

	private Map<Var, Boolean> varModified;

	private List<Instruction> toBeDeleted;

	private Map<Var, Var> targetSource;

	public RedundantLoadElimination() {
		super(true);
	}

	@Override
	public Void caseBlockBasic(BlockBasic block) {
		varModified = new HashMap<Var, Boolean>();
		targetSource = new HashMap<Var, Var>();
		toBeDeleted = new ArrayList<Instruction>();
		
		super.caseBlockBasic(block);
		
		for (Instruction instruction : toBeDeleted) {
			IrUtil.delete(instruction);
		}
		
		return null;
	}

	@Override
	public Void caseInstLoad(InstLoad load) {
		Var target = load.getTarget().getVariable();
		Var source = load.getSource().getVariable();
		if (source.getType().isList()) {
			// TODO:
		} else {
			if (targetSource.containsKey(target)) {
				if (targetSource.get(target) == source) {
					if (varModified.containsKey(source)) {
						Boolean isModified = varModified.get(source);
						if (!isModified) {
							toBeDeleted.add(load);
						} else {
							varModified.put(source, false);
						}
					} else {
						varModified.put(source, false);
					}
				}
			} else {
				targetSource.put(target, source);
				varModified.put(source, false);
			}
		}
		return null;
	}

	@Override
	public Void caseInstStore(InstStore store) {
		Var target = store.getTarget().getVariable();
		varModified.put(target, true);
		return null;
	}

}
