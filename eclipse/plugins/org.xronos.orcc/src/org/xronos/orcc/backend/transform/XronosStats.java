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

import java.util.HashSet;
import java.util.Set;

import net.sf.orcc.df.Action;
import net.sf.orcc.df.Actor;
import net.sf.orcc.df.Network;
import net.sf.orcc.df.util.DfVisitor;
import net.sf.orcc.graph.Vertex;
import net.sf.orcc.ir.BlockIf;
import net.sf.orcc.ir.BlockWhile;
import net.sf.orcc.ir.ExprBinary;
import net.sf.orcc.ir.ExprUnary;
import net.sf.orcc.ir.InstAssign;
import net.sf.orcc.ir.InstCall;
import net.sf.orcc.ir.InstLoad;
import net.sf.orcc.ir.InstStore;
import net.sf.orcc.ir.OpBinary;
import net.sf.orcc.ir.OpUnary;
import net.sf.orcc.ir.Procedure;
import net.sf.orcc.ir.Var;
import net.sf.orcc.ir.util.AbstractIrVisitor;

/**
 * 
 * @author Endri Bezati
 */
public class XronosStats extends DfVisitor<Void> {

	private int actorCounter;

	private int actionCounter;

	private int functionCounter;

	private int addSubCounter;

	private int multCounter;

	private int divCounter;

	private int shiftCounter;

	private int logicCounter;

	private int compareCounter;

	private int ifCounter;

	private int whileCounter;

	private int scalarStateCounter;

	private int arrayStatCounter;

	private int storeCounter;

	private int loadCounter;

	private int callCounter;

	private int assignCounter;

	private static boolean debug = true;

	private ActorStats stats;
	
	private Set<Procedure> procs;

	public class ActorStats extends AbstractIrVisitor<Void> {

		public ActorStats() {
			super(true);
		}

		@Override
		public Void caseVar(Var var) {
			if (var.isGlobal()) {
				if (var.getType().isList()) {
					arrayStatCounter++;
				} else {
					scalarStateCounter++;
				}
			}
			return null;
		}

		@Override
		public Void caseInstAssign(InstAssign assign) {
			assignCounter++;
			return super.caseInstAssign(assign);
		}

		@Override
		public Void caseInstLoad(InstLoad load) {
			loadCounter++;
			return super.caseInstLoad(load);
		}

		@Override
		public Void caseInstStore(InstStore store) {
			storeCounter++;
			return super.caseInstStore(store);
		}

		@Override
		public Void caseBlockIf(BlockIf blockIf) {
			ifCounter++;
			return super.caseBlockIf(blockIf);
		}

		@Override
		public Void caseBlockWhile(BlockWhile blockWhile) {
			whileCounter++;
			return super.caseBlockWhile(blockWhile);
		}

		@Override
		public Void caseExprBinary(ExprBinary expr) {
			if (expr.getOp() == OpBinary.BITAND) {
				logicCounter++;
			} else if (expr.getOp() == OpBinary.BITOR) {
				logicCounter++;
			} else if (expr.getOp() == OpBinary.BITXOR) {
				logicCounter++;
			} else if (expr.getOp() == OpBinary.DIV) {
				divCounter++;
			} else if (expr.getOp() == OpBinary.DIV_INT) {
				divCounter++;
			} else if (expr.getOp() == OpBinary.EQ) {
				compareCounter++;
			} else if (expr.getOp() == OpBinary.GE) {
				compareCounter++;
			} else if (expr.getOp() == OpBinary.GT) {
				compareCounter++;
			} else if (expr.getOp() == OpBinary.LE) {
				compareCounter++;
			} else if (expr.getOp() == OpBinary.LOGIC_AND) {
				logicCounter++;
			} else if (expr.getOp() == OpBinary.LOGIC_OR) {
				logicCounter++;
			} else if (expr.getOp() == OpBinary.LT) {
				compareCounter++;
			} else if (expr.getOp() == OpBinary.MINUS) {
				addSubCounter++;
			} else if (expr.getOp() == OpBinary.MOD) {
				divCounter++;
			} else if (expr.getOp() == OpBinary.NE) {
				compareCounter++;
			} else if (expr.getOp() == OpBinary.PLUS) {
				addSubCounter++;
			} else if (expr.getOp() == OpBinary.SHIFT_LEFT) {
				shiftCounter++;
			} else if (expr.getOp() == OpBinary.SHIFT_RIGHT) {
				shiftCounter++;
			} else if (expr.getOp() == OpBinary.TIMES) {
				multCounter++;
			}
			return null;
		}

		@Override
		public Void caseExprUnary(ExprUnary expr) {
			if (expr.getOp() == OpUnary.BITNOT) {
				logicCounter++;
			} else if (expr.getOp() == OpUnary.LOGIC_NOT) {
				logicCounter++;
			} else if (expr.getOp() == OpUnary.MINUS) {
				addSubCounter++;
			}
			return null;
		}

		@Override
		public Void caseInstCall(InstCall call) {
			callCounter++;
			if(!procs.contains(call.getProcedure())){
				functionCounter++;
				procs.add(call.getProcedure());
			}
			doSwitch(call.getProcedure());
			return super.caseInstCall(call);
		}

	}

	@Override
	public Void caseNetwork(Network network) {
		actorCounter = 0;

		actionCounter = 0;

		functionCounter = 0;

		addSubCounter = 0;

		multCounter = 0;

		divCounter = 0;

		shiftCounter = 0;

		logicCounter = 0;

		compareCounter = 0;

		ifCounter = 0;

		whileCounter = 0;

		scalarStateCounter = 0;

		arrayStatCounter = 0;

		loadCounter = 0;

		storeCounter = 0;
		callCounter = 0;
		assignCounter = 0;
		procs = new HashSet<Procedure>();
		stats = new ActorStats();

		for (Vertex vertex : network.getVertices()) {
			if (vertex instanceof Actor) {
				doSwitch(vertex);
			}
		}

		if (debug) {
			System.out.println("Actors: " + actorCounter);
			System.out.println("Actions: " + actionCounter);
			System.out.println("Scalars: " + scalarStateCounter);
			System.out.println("Lists: " + arrayStatCounter);
			System.out.println("Procedures/Functions: " + functionCounter);
			System.out.println("Addition/Substractions: " + addSubCounter);
			System.out.println("Multiplications: " + multCounter);
			System.out.println("Division/Modulos: " + divCounter);
			System.out.println("Logic: " + logicCounter);
			System.out.println("Shift: " + shiftCounter);
			System.out.println("Comparators: " + compareCounter);
			System.out.println("Branches: " + ifCounter);
			System.out.println("Calls " + callCounter);
			System.out.println("While: " + whileCounter);
			System.out.println("Assign: " + assignCounter);
			System.out.println("Load: " + loadCounter);
			System.out.println("Store: " + storeCounter);
		}

		return null;
	}

	@Override
	public Void caseActor(Actor actor) {

		actorCounter++;

		for (Var stateVar : actor.getStateVars()) {
			stats.doSwitch(stateVar);
		}

		for (Procedure procedure : actor.getProcs()) {
			if(!procedure.getName().contains("scheduler"))
				stats.doSwitch(procedure);
		}

		for (Action action : actor.getActions()) {
			if (!action.getName().contains("fillBuffers"))
				actionCounter++;
			doSwitch(action);
		}

		return super.caseActor(actor);
	}

	@Override
	public Void caseAction(Action action) {

		stats.doSwitch(action.getScheduler());
		stats.doSwitch(action.getBody());
		return null;
	}

}
