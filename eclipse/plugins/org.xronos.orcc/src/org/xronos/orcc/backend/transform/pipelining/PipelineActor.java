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

package org.xronos.orcc.backend.transform.pipelining;

import java.io.File;
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

	private List<String> inputs;

	private List<Integer> operators;

	private ExtractOperatorsIO opIO;

	private List<String> outputs;

	private String packageName;

	private String path;

	private Map<Port, String> portToStringMap;

	private int stage;

	public PipelineActor(String path, String packageName, Action action,
			ExtractOperatorsIO opIO, int stage, List<String> inputs,
			List<String> outputs, List<Integer> operators) {

		this.path = path;
		this.packageName = packageName;
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
		actor.setLabel(packageName + "." + name);
		actor.setFileName(path + File.separator + name);

		// Create the Actor Input(s)
		for (String portName : inputs) {
			Type type = EcoreUtil.copy(opIO.getVariableType(portName));
			Port inPort = dfFactory.createPort(type, portName + "_pI");
			portToStringMap.put(inPort, portName);
			actor.getInputs().add(inPort);
		}

		// Create the Actor Output(s)
		for (String portName : outputs) {
			Type type = EcoreUtil.copy(opIO.getVariableType(portName));
			Port outPort = DfFactory.eINSTANCE.createPort(type, portName
					+ "_pO");
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
		actor.getActionsOutsideFsm().add(actionStage);
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

			// Add to locals
			body.getLocals().add(portVarMap.get(port));

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
