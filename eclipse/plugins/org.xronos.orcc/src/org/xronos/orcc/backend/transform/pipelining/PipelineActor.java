/*
 * Copyright (c) 2013, Ecole Polytechnique Fédérale de Lausanne
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

package org.xronos.orcc.backend.transform.pipelining;

import net.sf.orcc.df.Action;
import net.sf.orcc.df.Actor;
import net.sf.orcc.df.DfFactory;
import net.sf.orcc.df.Pattern;
import net.sf.orcc.df.Port;
import net.sf.orcc.ir.IrFactory;
import net.sf.orcc.ir.Procedure;
import net.sf.orcc.ir.Type;
import net.sf.orcc.ir.Var;
import net.sf.orcc.util.util.EcoreHelper;

import org.eclipse.emf.ecore.util.EcoreUtil;

/**
 * 
 * @author Endri Bezati
 * 
 */
public class PipelineActor {

	private static DfFactory dfFactory = DfFactory.eINSTANCE;
	private static IrFactory irFactory = IrFactory.eINSTANCE;

	private Action action;

	private Actor actor;

	private OperatorColoring opColoring;

	private OperatorsIO opIO;

	private int stage;

	public PipelineActor(Action action, OperatorsIO opIO,
			OperatorColoring opColoring, int stage) {
		this.action = action;
		this.opIO = opIO;
		this.opColoring = opColoring;
		this.stage = stage;
		createActor();
	}

	/**
	 * Create an actor based on the stage of the operation coloring
	 */
	public void createActor() {
		// Get the parent actor
		Actor parentActor = EcoreHelper.getContainerOfType(action, Actor.class);

		// Set a new name for the new actor
		String name = parentActor.getName() + "_" + action.getName()
				+ "_stage_" + stage;
		// Create the new actor
		actor = dfFactory.createActor();
		actor.setName(name);

		// Create the Actor Input(s)
		for (Var varToPort : opColoring.getInputPorts(stage)) {
			String portName = varToPort.getIndexedName();
			Type type = EcoreUtil.copy(varToPort.getType());
			Port port = dfFactory.createPort(type, portName);
			actor.getInputs().add(port);
		}

		// Create the Actor Output(s)
		for (Var varToPort : opColoring.getOutputPorts(stage)) {
			String portName = varToPort.getIndexedName();
			Type type = EcoreUtil.copy(varToPort.getType());
			Port port = DfFactory.eINSTANCE.createPort(type, portName);
			actor.getOutputs().add(port);
		}

		// Create IO patterns of the action
		Pattern inputPattern = createInputPattern();
		Pattern outputPattern = createOutputPattern();
		// Empty peek Pattern for the moment
		Pattern peekedPattern = createPeekPattern();

		// Create the action scheduler and body
		Procedure scheduler = createScheduler();
		Procedure body = createBody();

		// Create the new action
		Action action = dfFactory.createAction("stage_" + stage, inputPattern,
				outputPattern, peekedPattern, scheduler, body);

		actor.getActions().add(action);
	}

	private Procedure createBody() {
		Procedure body = irFactory.createProcedure();
		return body;
	}

	/**
	 * Create a single token based input pattern
	 * 
	 * @return
	 */
	private Pattern createInputPattern() {
		Pattern pattern = dfFactory.createPattern();
		for (Port port : actor.getInputs()) {
			Type portVarType = irFactory.createTypeList(1, port.getType());
			pattern.setNumTokens(port, 1);
			pattern.setVariable(port,
					irFactory.createVar(portVarType, port.getName(), true, 0));
		}
		return pattern;
	}

	/**
	 * Create a single token based output pattern
	 * 
	 * @return
	 */
	private Pattern createOutputPattern() {
		Pattern pattern = dfFactory.createPattern();
		for (Port port : actor.getOutputs()) {
			Type portVarType = irFactory.createTypeList(1, port.getType());
			pattern.setNumTokens(port, 1);
			pattern.setVariable(port,
					irFactory.createVar(portVarType, port.getName(), true, 0));
		}
		return pattern;
	}

	/**
	 * Crate an empty peek pattern
	 * 
	 * @return
	 */
	private Pattern createPeekPattern() {
		Pattern pattern = dfFactory.createPattern();
		return pattern;
	}

	private Procedure createScheduler() {
		Procedure scheduler = irFactory.createProcedure();
		return scheduler;
	}

}