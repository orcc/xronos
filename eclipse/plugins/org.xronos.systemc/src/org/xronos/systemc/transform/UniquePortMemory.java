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
package org.xronos.systemc.transform;

import java.util.HashMap;
import java.util.Map;

import net.sf.orcc.df.Action;
import net.sf.orcc.df.Actor;
import net.sf.orcc.df.Port;
import net.sf.orcc.df.impl.PatternImpl;
import net.sf.orcc.df.util.DfVisitor;
import net.sf.orcc.ir.Def;
import net.sf.orcc.ir.InstLoad;
import net.sf.orcc.ir.InstStore;
import net.sf.orcc.ir.IrFactory;
import net.sf.orcc.ir.Use;
import net.sf.orcc.ir.Var;
import net.sf.orcc.ir.util.AbstractIrVisitor;
import net.sf.orcc.ir.util.IrUtil;
import net.sf.orcc.util.Void;
import net.sf.orcc.util.util.EcoreHelper;

/**
 * This transformations replaces all port variables with a unique one. With the
 * purpose of the scheduler to attribute values on this new variables.
 * 
 * @author Endri Bezati
 *
 */
public class UniquePortMemory extends DfVisitor<Void> {

	Integer maxRepeatSize;

	public UniquePortMemory(Integer fifosize) {
		this.maxRepeatSize = fifosize;
	}

	private class ReplaceVar extends AbstractIrVisitor<Void> {

		@Override
		public Void caseInstLoad(InstLoad load) {
			Var source = load.getSource().getVariable();
			PatternImpl pattern = EcoreHelper.getContainerOfType(source,
					PatternImpl.class);
			if (pattern != null) {
				Port port = (Port) pattern.getVarToPortMap().get(source);
				if (newPortVar.containsKey(port)) {
					Var var = newPortVar.get(port);
					Use use = IrFactory.eINSTANCE.createUse(var);
					load.setSource(use);
				}
			}
			return null;
		}

		@Override
		public Void caseInstStore(InstStore store) {
			Var target = store.getTarget().getVariable();
			PatternImpl pattern = EcoreHelper.getContainerOfType(target,
					PatternImpl.class);
			if (pattern != null) {
				Port port = (Port) pattern.getVarToPortMap().get(target);
				if (newPortVar.containsKey(port)) {
					Var var = newPortVar.get(port);
					Def def = IrFactory.eINSTANCE.createDef(var);
					store.setTarget(def);
				}
			}

			return null;
		}

	}

	private class MaxPortDepth extends DfVisitor<Map<Port, Integer>> {

		/**
		 * Maximum number of the repeat depth for each port
		 */
		private Map<Port, Integer> portDepth;

		@Override
		public Map<Port, Integer> caseAction(Action action) {

			for (Port port : action.getInputPattern().getPorts()) {
				int numTokens = action.getInputPattern().getNumTokensMap()
						.get(port);
				maxDepth(port, numTokens);
			}

			for (Port port : action.getOutputPattern().getPorts()) {
				int numTokens = action.getOutputPattern().getNumTokensMap()
						.get(port);
				maxDepth(port, numTokens);
			}
			return null;
		}

		@Override
		public Map<Port, Integer> caseActor(Actor actor) {
			portDepth = new HashMap<Port, Integer>();
			super.caseActor(actor);
			return portDepth;
		}

		private void maxDepth(Port port, int numTokens) {
			if (portDepth.containsKey(port)) {
				if (portDepth.get(port) < numTokens) {
					portDepth.put(port, numTokens);
				}
			} else {
				portDepth.put(port, numTokens);
			}
		}

	}

	Map<Port, Var> newPortVar;

	@Override
	public Void caseActor(Actor actor) {

		// -- Retrieve the port depth for each
		Map<Port, Integer> portDepth = new MaxPortDepth().doSwitch(actor);

		newPortVar = new HashMap<Port, Var>();
		for (Port port : portDepth.keySet()) {
			int nbrElements = portDepth.get(port);
			Var var = IrFactory.eINSTANCE.createVar(
					IrFactory.eINSTANCE.createTypeList(nbrElements,
							IrUtil.copy(port.getType())),
					"p_" + port.getName(), true, 0);
			var.setAttribute("portStateVar", true);
			actor.getStateVars().add(var);
			newPortVar.put(port, var);
		}

		// -- Now modify each Load and store associate to a Port
		for (Action action : actor.getActions()) {
			new ReplaceVar().doSwitch(action.getScheduler());
			new ReplaceVar().doSwitch(action.getBody());
		}

		actor.setAttribute("portDepth", portDepth);
		
		return null;
	}

}
