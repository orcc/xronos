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
package org.xronos.orcc.analysis.dataflow;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.sf.orcc.ir.Block;
import net.sf.orcc.ir.BlockBasic;
import net.sf.orcc.ir.Def;
import net.sf.orcc.ir.ExprVar;
import net.sf.orcc.ir.InstAssign;
import net.sf.orcc.ir.InstLoad;
import net.sf.orcc.ir.InstStore;
import net.sf.orcc.ir.Var;
import net.sf.orcc.ir.util.AbstractIrVisitor;
import net.sf.orcc.util.Void;

/**
 * 
 * @author Endri Bezati
 */
public class Liveness extends AbstractIrVisitor<Void> {

	private Map<Block, Set<Var>> blockUses;
	private Map<Block, Set<Var>> blockDefs;

	private Map<Block, Map<Var, List<Def>>> lastDef;

	private Block currentBlock;

	private int blockBasicCounter;
	private int branchCounter;
	private int loopCounter;

	public Liveness() {
		super(true);
		blockUses = new HashMap<Block, Set<Var>>();
		blockDefs = new HashMap<Block, Set<Var>>();

		lastDef = new HashMap<Block, Map<Var, List<Def>>>();

	}

	@Override
	public Void caseBlockBasic(BlockBasic block) {
		// Set Current Block
		currentBlock = block;

		// Set Block Label attribute
		String label = this.procedure.getName() + ".b" + blockBasicCounter;
		blockBasicCounter++;
		block.setAttribute("blockLabel", label);

		// Initialize members
		lastDef.put(block, new HashMap<Var, List<Def>>());
		blockUses.put(block, new HashSet<Var>());
		blockDefs.put(block, new HashSet<Var>());

		// Visit
		super.caseBlockBasic(block);

		// Print Uses Defs
		System.out.println("Block:" + block.getAttribute("blockLabel"));
		System.out.println("Uses:");
		for (Var var : blockUses.get(block)) {
			System.out.println("\t " + var.getName());
		}
		System.out.println("Defs:");
		for (Var var : blockDefs.get(block)) {
			System.out.println("\t " + var.getName());
		}

		return null;
	}

	@Override
	public Void caseInstAssign(InstAssign assign) {
		Var target = assign.getTarget().getVariable();
		addToDefs(target, assign.getTarget());

		return super.caseInstAssign(assign);
	}

	@Override
	public Void caseInstLoad(InstLoad load) {
		Var target = load.getTarget().getVariable();
		addToDefs(target, load.getTarget());

		// Visit indexes expressions in main visitor
		return super.caseInstLoad(load);
	}

	@Override
	public Void caseInstStore(InstStore store) {
		// Visit indexes expressions in main visitor
		// And visit use of store value
		return super.caseInstStore(store);
	}

	@Override
	public Void caseExprVar(ExprVar object) {
		Var var = object.getUse().getVariable();
		addToUses(var);
		return null;
	}

	private void addToDefs(Var var, Def def) {
		// Check For Last definition
		if (lastDef.get(currentBlock).containsKey(var)) {
			List<Def> defs = lastDef.get(currentBlock).get(var);
			defs.add(def);
		} else {
			List<Def> defs = new ArrayList<Def>();
			defs.add(def);
			lastDef.get(currentBlock).put(var, defs);
		}

		// Definition
		if (!blockDefs.get(currentBlock).contains(var)) {
			blockDefs.get(currentBlock).add(var);
		}
	}

	private void addToUses(Var var) {
		if (!blockUses.get(currentBlock).contains(var)) {
			blockUses.get(currentBlock).add(var);
		}
	}

}
