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
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import net.sf.orcc.df.Action;
import net.sf.orcc.df.Actor;
import net.sf.orcc.df.Pattern;
import net.sf.orcc.df.Port;
import net.sf.orcc.df.util.DfVisitor;
import net.sf.orcc.ir.BlockBasic;
import net.sf.orcc.ir.Def;
import net.sf.orcc.ir.InstLoad;
import net.sf.orcc.ir.InstStore;
import net.sf.orcc.ir.IrFactory;
import net.sf.orcc.ir.Type;
import net.sf.orcc.ir.TypeList;
import net.sf.orcc.ir.Use;
import net.sf.orcc.ir.Var;
import net.sf.orcc.ir.util.AbstractIrVisitor;

/**
 * 
 * This class visits the actor and it finds the biggest input and output repeat
 * index on the inputPattern and outputPattern. Then it creates a single state
 * variable list for the I/O pattern and it replace it for each action. After
 * that it creates the necessary stores for the pinRead component and loads for
 * the pinStore. This transformation does not change the actors scheduler.
 * 
 * @author Endri Bezati
 * 
 */
public class RepeatPattern extends DfVisitor<Void> {

	private class InnerVisitor extends AbstractIrVisitor<Object> {

		@Override
		public Object caseInstLoad(InstLoad load) {
			Var sourceVar = load.getSource().getVariable();
			if (oldInputMap.containsKey(sourceVar) && !inputMap.isEmpty()) {
				Port port = oldInputMap.get(sourceVar);
				Var newSourceVar = inputMap.get(port);
				Use newUse = IrFactory.eINSTANCE.createUse(newSourceVar);
				load.setSource(newUse);
			}
			return null;
		}

		@Override
		public Object caseInstStore(InstStore store) {
			Var targetVar = store.getTarget().getVariable();
			if (oldOutputMap.containsKey(targetVar) && !outputMap.isEmpty()) {
				Port port = oldOutputMap.get(targetVar);
				Var newTargetVar = outputMap.get(port);
				Def newDef = IrFactory.eINSTANCE.createDef(newTargetVar);
				store.setTarget(newDef);
			}
			return null;
		}

	}

	private Map<Port, Var> inputMap = new HashMap<Port, Var>();
	private Map<Port, Var> outputMap = new HashMap<Port, Var>();

	private Map<Var, Port> oldInputMap = new HashMap<Var, Port>();
	private Map<Var, Port> oldOutputMap = new HashMap<Var, Port>();

	private InnerVisitor innerVisitor = new InnerVisitor();

	@Override
	public Void caseAction(Action action) {
		/** InputPattern **/
		for (Port port : action.getInputPattern().getPorts()) {
			Var pinReadVar = action.getInputPattern().getPortToVarMap()
					.get(port);
			oldInputMap.put(pinReadVar, port);
			for (Entry<Port, Integer> repeatPattern : action.getInputPattern()
					.getNumTokensMap().entrySet()) {
				findBiggestRepeatPattern(pinReadVar, port, repeatPattern,
						inputMap);
				addLocalsAndStore(action, port, pinReadVar, "pinRead",
						repeatPattern.getValue());
			}
		}
		/** OutputPattern **/
		for (Port port : action.getOutputPattern().getPorts()) {
			Var pinWriteVar = action.getOutputPattern().getPortToVarMap()
					.get(port);
			oldOutputMap.put(pinWriteVar, port);
			for (Entry<Port, Integer> repeatPattern : action.getOutputPattern()
					.getNumTokensMap().entrySet()) {
				findBiggestRepeatPattern(pinWriteVar, port, repeatPattern,
						outputMap);
				addLocalsAndLoad(action, port, pinWriteVar, "pinWrite",
						repeatPattern.getValue());
			}
		}
		innerVisitor.doSwitch(action.getBody().getBlocks());
		return null;
	}

	private void addLocalsAndStore(Action action, Port port, Var var,
			String prefix, Integer repeatValue) {
		if (repeatValue > 1) {
			BlockBasic bodyNode = action.getBody().getFirst();
			for (int i = 0; i < repeatValue; i++) {
				Type type = ((TypeList) var.getType()).getInnermostType();
				Var localReadVar = IrFactory.eINSTANCE.createVar(type, prefix
						+ "_" + var.getName() + "_" + i, true, 0);
				action.getBody().getLocals().add(localReadVar);
				Var targetVar = inputMap.get(port);
				bodyNode.add(i, IrFactory.eINSTANCE.createInstStore(targetVar,
						i, localReadVar));
			}
		}
	}

	private void addLocalsAndLoad(Action action, Port port, Var var,
			String prefix, Integer repeatValue) {
		if (repeatValue > 1) {
			BlockBasic bodyNode = action.getBody().getLast();
			for (int i = 0; i < repeatValue; i++) {
				Type type = ((TypeList) var.getType()).getInnermostType();
				Var localWriteVar = IrFactory.eINSTANCE.createVar(type, prefix
						+ "_" + var.getName() + "_" + i, true, 0);
				action.getBody().getLocals().add(localWriteVar);
				Var sourceVar = outputMap.get(port);
				bodyNode.add(i, IrFactory.eINSTANCE.createInstLoad(
						localWriteVar, sourceVar, i));
			}
		}
	}

	private void findBiggestRepeatPattern(Var pinReadVar, Port port,
			Entry<Port, Integer> repeatPattern, Map<Port, Var> ioMap) {
		// If the repeat index is > 1
		if (repeatPattern.getValue() > 1) {
			// If the inputList contains this port change it size if
			// possible
			if (ioMap.containsKey(repeatPattern.getKey())) {
				Var var = ioMap.get(repeatPattern.getKey());
				List<Integer> dim = var.getType().getDimensions();
				Integer newSize = repeatPattern.getValue();
				if (dim.get(0) < newSize) {
					Type type = IrFactory.eINSTANCE.createTypeList(newSize,
							((TypeList) pinReadVar.getType())
									.getInnermostType());
					var.setType(type);
				}
			} else {
				Type type = pinReadVar.getType();
				Var var = IrFactory.eINSTANCE.createVar(type,
						pinReadVar.getName(), true, 0);
				ioMap.put(port, var);
			}
		}
	}

	@Override
	public Void caseActor(Actor actor) {
		this.actor = actor;
		/** Visit all actions **/
		for (Action action : actor.getActions()) {
			doSwitch(action);
		}
		/** Add the I/O List for the repeat pattern **/
		for (Port port : inputMap.keySet()) {
			Var inVar = inputMap.get(port);
			actor.getStateVars().add(inVar);
		}

		for (Port port : outputMap.keySet()) {
			Var inVar = outputMap.get(port);
			actor.getStateVars().add(inVar);
		}
		return null;
	}

	@Override
	public Void casePattern(Pattern pattern) {
		return null;
	}

}
