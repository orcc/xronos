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

package org.xronos.orcc.backend.transform;

import java.util.HashMap;
import java.util.Map;

import net.sf.orcc.df.Action;
import net.sf.orcc.df.Actor;
import net.sf.orcc.df.Port;
import net.sf.orcc.df.util.DfVisitor;
import net.sf.orcc.ir.BlockBasic;
import net.sf.orcc.ir.Def;
import net.sf.orcc.ir.Expression;
import net.sf.orcc.ir.InstAssign;
import net.sf.orcc.ir.InstLoad;
import net.sf.orcc.ir.InstStore;
import net.sf.orcc.ir.IrFactory;
import net.sf.orcc.ir.Var;
import net.sf.orcc.ir.util.AbstractIrVisitor;
import net.sf.orcc.ir.util.IrUtil;

import org.xronos.orcc.design.ResourceCache;
import org.xronos.orcc.design.visitors.io.CircularBuffer;
import org.xronos.orcc.ir.InstPortRead;
import org.xronos.orcc.ir.InstPortWrite;
import org.xronos.orcc.ir.XronosIrFactory;

/**
 * This class visits an Action and when it finds a Load or Store operation on a
 * port it replace it with portRead and portWrite instruction.
 * 
 * @author Endri Bezati
 * 
 */
public class ScalarPortIO extends DfVisitor<Void> {

	private class InnerVisitor extends AbstractIrVisitor<Object> {

		@Override
		public Object caseInstLoad(InstLoad load) {
			Var sourceVar = load.getSource().getVariable();
			if (varToPortMap.containsKey(sourceVar)) {
				Port port = varToPortMap.get(sourceVar);
				Var target = load.getTarget().getVariable();
				Def def = IrFactory.eINSTANCE.createDef(target);

				if (portRead) {
					InstPortRead portRead = XronosIrFactory.eINSTANCE
							.createInstPortRead();
					portRead.setPort(port);
					portRead.setTarget(def);
					portRead.setBlocking(false);
					portRead.setLineNumber(load.getLineNumber());

					BlockBasic block = load.getBlock();
					int index = load.getBlock().indexOf(load);

					block.add(index, portRead);

				} else {
					Var portPeekVar = null;
					if (actor.getStateVar("portPeek_" + port.getName()) != null) {
						portPeekVar = actor.getStateVar("portPeek_"
								+ port.getName());
					} else {
						portPeekVar = IrFactory.eINSTANCE.createVar(
								port.getType(), "portPeek_" + port.getName(),
								true, 0);
						actor.getStateVars().add(portPeekVar);
					}

					Var loadTarget = load.getTarget().getVariable();

					InstAssign peekAssign = IrFactory.eINSTANCE
							.createInstAssign(loadTarget, portPeekVar);
					BlockBasic block = load.getBlock();
					int index = load.getBlock().indexOf(load);
					block.add(index, peekAssign);
				}
				IrUtil.delete(load);
			}

			return null;
		}

		@Override
		public Object caseInstStore(InstStore store) {
			Var targetVar = store.getTarget().getVariable();
			if (varToPortMap.containsKey(targetVar)) {
				Port port = varToPortMap.get(targetVar);
				Expression value = store.getValue();
				InstPortWrite portWrite = XronosIrFactory.eINSTANCE
						.createInstPortWrite();
				portWrite.setPort(port);
				portWrite.setValue(value);
				if (portTokens.containsKey(port)) {
					if (portTokens.get(port) > 1) {
						portWrite.setBlocking(true);
					} else {
						portWrite.setBlocking(false);
					}
				} else {
					portWrite.setBlocking(false);
				}
				portWrite.setLineNumber(store.getLineNumber());
				BlockBasic block = store.getBlock();
				int index = store.getBlock().indexOf(store);

				block.add(index, portWrite);
				IrUtil.delete(store);
			}
			return null;
		}
	}

	private Map<Port, CircularBuffer> CircularBufferInput;

	private final InnerVisitor innerVisitor = new InnerVisitor();

	/** Change Load to portRead if true, change to portPeek otherwise **/
	private Boolean portRead;

	private Map<Port, Integer> portTokens;

	private final ResourceCache resourceCache;

	private final Map<Var, Port> varToPortMap = new HashMap<Var, Port>();

	public ScalarPortIO(ResourceCache resourceCache) {
		super();
		this.resourceCache = resourceCache;
	}

	@Override
	public Void caseAction(Action action) {
		if (!action.getInputPattern().isEmpty()
				|| !action.getOutputPattern().isEmpty()) {
			for (Port port : action.getInputPattern().getPorts()) {
				if (CircularBufferInput.get(port) == null) {
					Var portReadVar = action.getInputPattern()
							.getPortToVarMap().get(port);
					varToPortMap.put(portReadVar, port);
				}
			}

			for (Port port : action.getPeekPattern().getPorts()) {
				if (CircularBufferInput.get(port) == null) {
					Var portReadVar = action.getPeekPattern().getPortToVarMap()
							.get(port);
					varToPortMap.put(portReadVar, port);
				}
			}
			portTokens = new HashMap<Port, Integer>();
			for (Port port : action.getOutputPattern().getPorts()) {
				portTokens.put(port,
						action.getOutputPattern().getNumTokens(port));
				Var portWriteVar = action.getOutputPattern().getPortToVarMap()
						.get(port);
				varToPortMap.put(portWriteVar, port);
			}
			// Visit the action
			portRead = true;
			innerVisitor.doSwitch(action.getBody());
			portRead = false;
			innerVisitor.doSwitch(action.getScheduler());
		}
		return null;
	}

	@Override
	public Void caseActor(Actor actor) {
		this.actor = actor;
		CircularBufferInput = resourceCache.getActorInputCircularBuffer(actor);

		for (Action action : actor.getActions()) {
			doSwitch(action);
		}

		return null;
	}

}
