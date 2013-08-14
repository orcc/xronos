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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sf.orcc.backends.ir.InstCast;
import net.sf.orcc.backends.ir.IrSpecificFactory;
import net.sf.orcc.df.Action;
import net.sf.orcc.df.Actor;
import net.sf.orcc.df.DfFactory;
import net.sf.orcc.df.Pattern;
import net.sf.orcc.df.Port;
import net.sf.orcc.ir.BlockBasic;
import net.sf.orcc.ir.ExprBinary;
import net.sf.orcc.ir.ExprVar;
import net.sf.orcc.ir.Expression;
import net.sf.orcc.ir.InstAssign;
import net.sf.orcc.ir.InstLoad;
import net.sf.orcc.ir.InstReturn;
import net.sf.orcc.ir.InstStore;
import net.sf.orcc.ir.IrFactory;
import net.sf.orcc.ir.OpBinary;
import net.sf.orcc.ir.Procedure;
import net.sf.orcc.ir.Type;
import net.sf.orcc.ir.Var;
import net.sf.orcc.ir.util.IrUtil;
import net.sf.orcc.util.util.EcoreHelper;

import org.eclipse.emf.common.util.EMap;
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

	private ExtractOperatorsIO opIO;

	private int stage;

	private List<String> inputs;

	private List<String> outputs;

	private List<Integer> operators;

	private Map<Port, String> portToStringMap;

	public PipelineActor(Action action, ExtractOperatorsIO opIO, int stage,
			List<String> inputs, List<String> outputs, List<Integer> operators) {
		this.action = action;
		this.opIO = opIO;
		this.stage = stage;
		this.inputs = inputs;
		this.outputs = outputs;
		this.operators = operators;
		portToStringMap = new HashMap<Port, String>();
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
		for (String portName : inputs) {
			Type type = EcoreUtil.copy(opIO.getVariableType(portName));
			Port inPort = dfFactory.createPort(type, portName + "_p");
			portToStringMap.put(inPort, portName);
			actor.getInputs().add(inPort);
		}

		// Create the Actor Output(s)
		for (String portName : outputs) {
			Type type = EcoreUtil.copy(opIO.getVariableType(portName));
			Port outPort = DfFactory.eINSTANCE
					.createPort(type, portName + "_p");
			portToStringMap.put(outPort, portName);
			actor.getOutputs().add(outPort);
		}

		// Create IO patterns of the action
		Pattern inputPattern = createInputPattern();
		Pattern outputPattern = createOutputPattern();
		// Empty peek Pattern for the moment
		Pattern peekedPattern = createPeekPattern();

		// Create the action scheduler and body
		Procedure scheduler = createScheduler();
		Procedure body = createBody(inputPattern, outputPattern);

		// Create the new action
		String actionStageName = action.getName() + "_stage_" + stage;
		Action actionStage = dfFactory.createAction(actionStageName,
				inputPattern, outputPattern, peekedPattern, scheduler, body);

		actor.getActions().add(actionStage);
	}

	private Procedure createBody(Pattern inputPattern, Pattern outputPattern) {
		Procedure body = irFactory.createProcedure();
		// Give the body procedure a name
		String bodyStageName = action.getName() + "_stage_" + stage;
		body.setName(bodyStageName);

		// Create scheduler Body
		BlockBasic block = irFactory.createBlockBasic();

		// Get the input port var map
		EMap<Port, Var> portVarMap = inputPattern.getPortToVarMap();

		// Create all the load instructions for reading from the input ports
		for (Port port : inputPattern.getPorts()) {
			String name = portToStringMap.get(port);
			Type type = IrUtil.copy(port.getType());
			Var read = irFactory.createVar(type, name, true, 0);

			// Add to locals
			body.getLocals().add(read);

			List<Expression> indexes = new ArrayList<Expression>();

			Expression index0 = irFactory.createExprInt(0);
			indexes.add(index0);

			InstLoad load = irFactory.createInstLoad(read,
					portVarMap.get(port), indexes);

			block.add(load);
		}

		// Add instructions from the operations list
		for (Integer nbrOp : operators) {
			PipelineOperator pipelineOperator = opIO.getOperator(nbrOp);
			if (pipelineOperator == PipelineOperator.ASSIGN) {
				Var target = null;
				Expression value = null;
				List<String> inputs = opIO.getInputs(nbrOp);
				// test if we have to assign a constant
				if (inputs.get(0).equals("") && inputs.get(1).equals("")) {
					Expression expr = opIO.getConstantExpression(nbrOp);
					value = EcoreUtil.copy(expr);

				} else {
					Var source = null;
					if (body.getLocal(inputs.get(0)) == null) {
						Type type = opIO.getVariableType(inputs.get(0));
						source = irFactory.createVar(type, inputs.get(0), true,
								0);
						body.getLocals().add(source);
					} else {
						source = body.getLocal(inputs.get(0));
					}

					value = irFactory.createExprVar(source);
				}

				String output = opIO.getOutput(nbrOp);
				Type typeOutput = opIO.getVariableType(output);
				if (body.getLocal(output) == null) {
					target = irFactory.createVar(typeOutput, output, true, 0);
					body.getLocals().add(target);
				} else {
					target = body.getLocal(output);
				}

				InstAssign assign = irFactory.createInstAssign(target, value);
				block.add(assign);

			} else if (pipelineOperator == PipelineOperator.CAST) {

				Var source = null;
				Var target = null;

				List<String> inputs = opIO.getInputs(nbrOp);
				if (body.getLocal(inputs.get(0)) == null) {
					Type type = opIO.getVariableType(inputs.get(0));
					source = irFactory.createVar(type, inputs.get(0), true, 0);
					body.getLocals().add(source);
				} else {
					source = body.getLocal(inputs.get(0));
				}

				String output = opIO.getOutput(nbrOp);
				Type typeOutput = opIO.getVariableType(output);
				if (body.getLocal(output) == null) {
					target = irFactory.createVar(typeOutput, output, true, 0);
					body.getLocals().add(target);
				} else {
					target = body.getLocal(output);
				}

				InstCast cast = IrSpecificFactory.eINSTANCE.createInstCast(
						source, target);
				block.add(cast);

			} else {
				// Get Orcc operator
				OpBinary op = pipelineOperator.getOrccOperator();

				Var inputE1 = null;
				Var inputE2 = null;
				Var target = null;
				// Get inputs and output
				List<String> inputs = opIO.getInputs(nbrOp);
				if (body.getLocal(inputs.get(0)) == null) {
					Type type = opIO.getVariableType(inputs.get(0));
					inputE1 = irFactory.createVar(type, inputs.get(0), true, 0);
					body.getLocals().add(inputE1);
				} else {
					inputE1 = body.getLocal(inputs.get(0));
				}
				if (body.getLocal(inputs.get(1)) == null) {
					Type type = opIO.getVariableType(inputs.get(1));
					inputE2 = irFactory.createVar(type, inputs.get(1), true, 0);
					body.getLocals().add(inputE2);
				} else {
					inputE2 = body.getLocal(inputs.get(1));
				}

				String output = opIO.getOutput(nbrOp);
				Type typeOutput = opIO.getVariableType(output);
				if (body.getLocal(output) == null) {
					target = irFactory.createVar(typeOutput, output, true, 0);
					body.getLocals().add(target);
				} else {
					target = body.getLocal(output);
				}

				ExprVar e1 = irFactory.createExprVar(inputE1);
				ExprVar e2 = irFactory.createExprVar(inputE2);

				ExprBinary value = irFactory.createExprBinary(e1, op, e2,
						EcoreUtil.copy(typeOutput));

				InstAssign assign = irFactory.createInstAssign(target, value);
				block.add(assign);

			}

		}

		// Get the output port var map
		portVarMap = outputPattern.getPortToVarMap();
		// Create all the store instructions for reading from the input ports

		for (Port port : outputPattern.getPorts()) {
			String name = portToStringMap.get(port);
			Var write = body.getLocal(name);

			List<Expression> indexes = new ArrayList<Expression>();

			Expression index0 = irFactory.createExprInt(0);
			indexes.add(index0);

			InstStore store = irFactory.createInstStore(portVarMap.get(port),
					indexes, write);

			block.add(store);
		}

		// Finally this basic block to the
		body.getBlocks().add(block);

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
		// Set the scheduler procedure name
		String schedulerStageName = "isSchedulable_" + action.getName()
				+ "_stage_" + stage;
		scheduler.setName(schedulerStageName);

		// Create the result Variable
		Type typeBool = irFactory.createTypeBool();
		Var result = irFactory.createVar(typeBool, "result", true, 0);

		scheduler.getLocals().add(result);

		// Create scheduler Body

		BlockBasic block = irFactory.createBlockBasic();

		Expression exprTrue = irFactory.createExprBool(true);

		InstAssign assign = irFactory.createInstAssign(result, exprTrue);

		block.add(assign);

		Expression exprVar = irFactory.createExprVar(result);

		InstReturn returnResult = irFactory.createInstReturn(exprVar);

		block.add(returnResult);

		// Add to the procedure blocks

		scheduler.getBlocks().add(block);

		return scheduler;
	}

	public Actor getActor() {
		return actor;
	}

}
