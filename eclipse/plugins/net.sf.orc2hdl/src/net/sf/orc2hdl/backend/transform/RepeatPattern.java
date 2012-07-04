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
import net.sf.orcc.ir.IrFactory;
import net.sf.orcc.ir.Type;
import net.sf.orcc.ir.TypeList;
import net.sf.orcc.ir.Var;

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

	private Map<Port, Var> inputList = new HashMap<Port, Var>();

	@Override
	public Void caseAction(Action action) {

		for (Port port : action.getInputPattern().getPorts()) {
			Var pinReaVar = action.getInputPattern().getPortToVarMap()
					.get(port);
			for (Entry<Port, Integer> numTokens : action.getInputPattern()
					.getNumTokensMap().entrySet()) {
				// If the repeat index is > 1
				if (numTokens.getValue() > 1) {
					// If the inputList contains this port change it size if
					// possible
					if (inputList.containsKey(numTokens.getKey())) {
						Var var = inputList.get(numTokens.getKey());
						List<Integer> dim = var.getType().getDimensions();
						Integer newSize = numTokens.getValue();
						if (dim.get(0) < newSize) {
							Type type = IrFactory.eINSTANCE.createTypeList(
									newSize, ((TypeList) pinReaVar.getType())
											.getInnermostType());
							var.setType(type);
						}
					} else {
						Type type = pinReaVar.getType();
						Var var = IrFactory.eINSTANCE.createVar(type,
								pinReaVar.getName(), true, 0);
						inputList.put(port, var);
					}
				}
			}
		}

		return null;
	}

	@Override
	public Void caseActor(Actor actor) {
		/** Visit all actions **/
		for (Action action : actor.getActions()) {
			doSwitch(action);
		}
		/** Add the I/O List for the repeat pattern **/

		return null;
	}

	@Override
	public Void casePattern(Pattern pattern) {
		return null;
	}

}
