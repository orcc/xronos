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

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.sf.orcc.graph.GraphPackage;
import net.sf.orcc.graph.Vertex;
import net.sf.orcc.graph.visit.Ordering;
import net.sf.orcc.graph.visit.ReversePostOrder;
import net.sf.orcc.ir.Arg;
import net.sf.orcc.ir.Block;
import net.sf.orcc.ir.BlockBasic;
import net.sf.orcc.ir.BlockIf;
import net.sf.orcc.ir.BlockWhile;
import net.sf.orcc.ir.Cfg;
import net.sf.orcc.ir.CfgNode;
import net.sf.orcc.ir.ExprVar;
import net.sf.orcc.ir.Expression;
import net.sf.orcc.ir.InstAssign;
import net.sf.orcc.ir.InstCall;
import net.sf.orcc.ir.InstLoad;
import net.sf.orcc.ir.InstStore;
import net.sf.orcc.ir.Procedure;
import net.sf.orcc.ir.Var;
import net.sf.orcc.ir.util.AbstractIrVisitor;
import net.sf.orcc.util.Void;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.xronos.orcc.ir.BlockMutex;
import org.xronos.orcc.ir.InstPortPeek;
import org.xronos.orcc.ir.InstPortRead;
import org.xronos.orcc.ir.InstPortStatus;
import org.xronos.orcc.ir.InstPortWrite;

/**
 * This visitor computes the liveness for each block, a procedure should have a
 * CFG before applying this analysis. This algorithm is taken from the book
 * "Engineering a Compiler" by Keith Cooper.
 * 
 * @author Endri Bezati
 *
 */
public class Liveness extends AbstractIrVisitor<Void> {

	private Set<Var> ueVar;

	private Set<Var> varKill;

	private static boolean DEBUG = true;

	public Liveness() {
		super(true);
	}

	@Override
	public Void caseInstAssign(InstAssign assign) {
		doSwitch(assign.getValue());

		Var target = assign.getTarget().getVariable();
		varKill.add(target);

		return null;
	}

	@Override
	public Void caseInstLoad(InstLoad load) {
		for (Expression expr : load.getIndexes()) {
			doSwitch(expr);
		}

		Var target = load.getTarget().getVariable();
		varKill.add(target);

		return null;
	}

	@Override
	public Void caseInstStore(InstStore store) {
		doSwitch(store.getValue());

		for (Expression expr : store.getIndexes()) {
			doSwitch(expr);
		}

		Var target = store.getTarget().getVariable();
		varKill.add(target);

		return null;
	}

	@Override
	public Void caseInstCall(InstCall call) {
		for (Arg arg : call.getArguments()) {
			doSwitch(arg);
		}

		if (call.getTarget() != null) {
			Var target = call.getTarget().getVariable();
			varKill.add(target);
		}
		return null;
	}

	@Override
	public Void caseExprVar(ExprVar object) {
		Var var = object.getUse().getVariable();
		if (!varKill.contains(var)) {
			ueVar.add(var);
		}
		return null;
	}

	@Override
	public Void caseBlockBasic(BlockBasic block) {
		ueVar = new HashSet<Var>();
		varKill = new HashSet<Var>();

		visitInstructions(block.getInstructions());

		block.setAttribute("UEVar", ueVar);
		block.setAttribute("VarKill", varKill);
		return null;
	}

	public Void caseInstPortRead(InstPortRead instPortRead) {
		Var target = instPortRead.getTarget().getVariable();
		varKill.add(target);

		return null;
	}

	public Void caseInstPortPeek(InstPortPeek instPortPeek) {
		Var target = instPortPeek.getTarget().getVariable();
		varKill.add(target);

		return null;
	}

	public Void caseInstPortStatus(InstPortStatus instPortStatus) {
		Var target = instPortStatus.getTarget().getVariable();
		varKill.add(target);
		return null;
	}

	public Void caseInstPortWrite(InstPortWrite instPortWrite) {
		doSwitch(instPortWrite.getValue());
		return null;
	}

	@Override
	public Void caseBlockIf(BlockIf blockIf) {
		ueVar = new HashSet<Var>();
		varKill = new HashSet<Var>();

		doSwitch(blockIf.getCondition());

		blockIf.setAttribute("UEVar", ueVar);
		blockIf.setAttribute("VarKill", varKill);

		doSwitch(blockIf.getThenBlocks());
		doSwitch(blockIf.getElseBlocks());
		return null;
	}

	public Void caseBlockMutex(BlockMutex blockMutex) {
		doSwitch(blockMutex.getBlocks());
		return null;
	}

	@Override
	public Void caseBlockWhile(BlockWhile blockWhile) {
		ueVar = new HashSet<Var>();
		varKill = new HashSet<Var>();

		doSwitch(blockWhile.getCondition());

		blockWhile.setAttribute("UEVar", ueVar);
		blockWhile.setAttribute("VarKill", varKill);

		doSwitch(blockWhile.getBlocks());

		return null;
	}

	@Override
	public Void caseProcedure(Procedure procedure) {

		doSwitch(procedure.getBlocks());
		Cfg cfg = procedure.getCfg();

		EReference refEdges = GraphPackage.Literals.VERTEX__OUTGOING;
		EReference refVertex = GraphPackage.Literals.EDGE__TARGET;

		Ordering rpo = new ReversePostOrder(cfg, refVertex.getEOpposite(),
				refEdges.getEOpposite(), cfg.getExit());

		List<Vertex> vertices = rpo.getVertices();
		Map<Vertex, Set<Var>> liveOuts = new HashMap<Vertex, Set<Var>>();

		// Initialize live out for each Vertex
		for (Vertex vertex : vertices) {
			liveOuts.put(vertex, new HashSet<Var>());
		}

		if (DEBUG)
			System.out.println("Procedure : " + procedure.getName());

		boolean changed = true;
		int passes = 0;
		while (changed) {
			changed = false;
			for (int i = 1; i < vertices.size() - 1; i++) {
				Set<Var> oldLiveOut = liveOuts.get(vertices.get(i));
				Set<Var> newLiveOut = liveOut(vertices.get(i), liveOuts);// liveOut(vertices,
																			// liveOuts,
																			// i);
				liveOuts.put(vertices.get(i), newLiveOut);
				if (!oldLiveOut.equals(newLiveOut)) {
					changed = true;
				}
			}
			if (DEBUG) {
				// Debug Print
				System.out.println("Pass : " + passes);
				for (Vertex vertex : vertices) {
					System.out.println("Vertex: " + vertex.getLabel());
					for (Var var : liveOuts.get(vertex)) {
						System.out.print("\t" + var.getName() + ", ");
					}
					System.out.print("\n");
				}
				passes++;
			}
		}

		// Store liveness for each Block
		for (Vertex vertex : vertices) {
			CfgNode node = (CfgNode) vertex;
			if (node.getNode() != null) {
				Block block = node.getNode();
				block.setAttribute("LiveOut", liveOuts.get(vertex));
			}
		}

		return null;
	}

	@Override
	public Void defaultCase(EObject object) {
		if (object instanceof BlockMutex) {
			return caseBlockMutex((BlockMutex) object);
		} else if (object instanceof InstPortRead) {
			return caseInstPortRead((InstPortRead) object);
		} else if (object instanceof InstPortWrite) {
			return caseInstPortWrite((InstPortWrite) object);
		} else if (object instanceof InstPortStatus) {
			return caseInstPortStatus((InstPortStatus) object);
		} else if (object instanceof InstPortPeek) {
			return caseInstPortPeek((InstPortPeek) object);
		}
		return null;
	}

	private Set<Var> liveOut(Vertex vertex, Map<Vertex, Set<Var>> liveOuts) {
		Set<Var> newLiveOut = new HashSet<Var>();
		for (Vertex vx : vertex.getSuccessors()) {
			Set<Var> newLiveOutSucc = liveOuts.get(vx);
			if (!vx.getLabel().equals("exit") && !vx.getLabel().equals("join")) {
				Block block = ((CfgNode) vx).getNode();
				@SuppressWarnings("unchecked")
				Set<Var> ueVar = (Set<Var>) block.getAttribute("UEVar")
						.getObjectValue();
				@SuppressWarnings("unchecked")
				Set<Var> varKill = (Set<Var>) block.getAttribute("VarKill")
						.getObjectValue();

				Set<Var> temp = new HashSet<Var>();
				for (Var var : newLiveOutSucc) {
					if (!varKill.contains(var)) {
						temp.add(var);
					}
				}
				temp.addAll(ueVar);

				newLiveOut.addAll(temp);
			}
		}
		return newLiveOut;
	}

}
