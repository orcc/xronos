/*
 * Copyright (c) 2012, Ecole Polytechnique Fédérale de Lausanne
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 *   * Redistributions of source code must retain the above copyright notice,
 *     this list of conditions and the following disclaimer.
 *   * Redistributions in binary form must reproduce the above copyright notice,
 *     this list of conditions and the following disclaimer in the documentation
 *     and/or other materials provided with the distribution.
 *   * Neither the name of the Ecole Polytechnique Fédérale de Lausanne nor the names of its
 *     contributors may be used to endorse or promote products derived from this
 *     software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY
 * WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 */
package net.sf.orc2hdl.backend.transform;

import java.util.HashMap;
import java.util.Map;

import net.sf.orc2hdl.design.ResourceCache;
import net.sf.orc2hdl.design.visitors.io.CircularBuffer;
import net.sf.orc2hdl.ir.ChronosIrSpecificFactory;
import net.sf.orc2hdl.ir.InstPortRead;
import net.sf.orc2hdl.ir.InstPortWrite;
import net.sf.orcc.df.Action;
import net.sf.orcc.df.Actor;
import net.sf.orcc.df.Port;
import net.sf.orcc.df.util.DfVisitor;
import net.sf.orcc.ir.BlockBasic;
import net.sf.orcc.ir.Def;
import net.sf.orcc.ir.Expression;
import net.sf.orcc.ir.InstLoad;
import net.sf.orcc.ir.InstStore;
import net.sf.orcc.ir.IrFactory;
import net.sf.orcc.ir.Var;
import net.sf.orcc.ir.util.AbstractIrVisitor;
import net.sf.orcc.ir.util.IrUtil;

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
				InstPortRead portRead = ChronosIrSpecificFactory.eINSTANCE
						.createInstPortRead();
				portRead.setPort(port);
				portRead.setTarget(def);
				portRead.setLineNumber(load.getLineNumber());
				BlockBasic block = load.getBlock();
				int index = load.getBlock().indexOf(load);

				block.add(index, portRead);
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
				InstPortWrite portWrite = ChronosIrSpecificFactory.eINSTANCE
						.createInstPortWrite();
				portWrite.setPort(port);
				portWrite.setValue(value);
				portWrite.setLineNumber(store.getLineNumber());
				BlockBasic block = store.getBlock();
				int index = store.getBlock().indexOf(store);

				block.add(index, portWrite);
				IrUtil.delete(store);
			}
			return null;
		}

	}

	private ResourceCache resourceCache;
	private InnerVisitor innerVisitor = new InnerVisitor();
	private Map<Var, Port> varToPortMap = new HashMap<Var, Port>();

	private Map<Port, CircularBuffer> CircularBufferInput;

	private Map<Port, CircularBuffer> CircularBufferOutput;

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

			for (Port port : action.getOutputPattern().getPorts()) {
				if (CircularBufferOutput.get(port) == null) {
					Var portWriteVar = action.getOutputPattern()
							.getPortToVarMap().get(port);
					varToPortMap.put(portWriteVar, port);
				}
			}
			// Visit the action
			innerVisitor.doSwitch(action.getBody());
		}
		return null;
	}

	@Override
	public Void caseActor(Actor actor) {
		CircularBufferInput = resourceCache.getActorInputCircularBuffer(actor);
		CircularBufferOutput = resourceCache
				.getActorOutputCircularBuffer(actor);

		for (Action action : actor.getActions()) {
			doSwitch(action);
		}

		return null;
	}
}
