/*
 * Copyright (c) 2011, Ecole Polytechnique Fédérale de Lausanne
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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sf.orcc.backends.ir.InstCast;
import net.sf.orcc.ir.BlockBasic;
import net.sf.orcc.ir.ExprBinary;
import net.sf.orcc.ir.ExprVar;
import net.sf.orcc.ir.InstAssign;
import net.sf.orcc.ir.Instruction;
import net.sf.orcc.ir.OpBinary;
import net.sf.orcc.ir.Procedure;
import net.sf.orcc.ir.Var;
import net.sf.orcc.ir.util.AbstractIrVisitor;

import org.eclipse.emf.ecore.EObject;

/**
 * This class is creating two matrix that defines the DFG of an Procedure by
 * giving the Matrix of input-operators and output-operators.
 * 
 * @author Endri Bezati
 * 
 */
public class OperatorsIO extends AbstractIrVisitor<Void> {

	@SuppressWarnings("unused")
	private static Map<PipelineOperator, List<Float>> WEIGHTS;

	static {
		Map<PipelineOperator, List<Float>> weights = new HashMap<PipelineOperator, List<Float>>();
		weights.put(PipelineOperator.ASSIGN, Arrays.asList(0.0f, 0.0f));
		weights.put(PipelineOperator.BITAND, Arrays.asList(0.15f, 0.5f));
		weights.put(PipelineOperator.BITOR, Arrays.asList(0.15f, 0.5f));
		weights.put(PipelineOperator.CAST, Arrays.asList(0.0f, 0.0f));
		weights.put(PipelineOperator.DIV, Arrays.asList(2.0f, 10.0f));
		weights.put(PipelineOperator.EQ, Arrays.asList(0.1f, 0.2f));
		weights.put(PipelineOperator.GE, Arrays.asList(0.1f, 0.2f));
		weights.put(PipelineOperator.GT, Arrays.asList(0.1f, 0.2f));
		weights.put(PipelineOperator.LE, Arrays.asList(0.1f, 0.2f));
		weights.put(PipelineOperator.LT, Arrays.asList(0.1f, 0.2f));
		weights.put(PipelineOperator.LOGIC_AND, Arrays.asList(0.5f, 0.2f));
		weights.put(PipelineOperator.LOGIC_OR, Arrays.asList(0.5f, 0.2f));
		weights.put(PipelineOperator.MINUS, Arrays.asList(1.0f, 2.0f));
		weights.put(PipelineOperator.MOD, Arrays.asList(2.0f, 10.0f));
		weights.put(PipelineOperator.NE, Arrays.asList(0.1f, 0.2f));
		weights.put(PipelineOperator.PLUS, Arrays.asList(1.0f, 1.0f));
		weights.put(PipelineOperator.SHIFT_LEFT, Arrays.asList(0.1f, 0.2f));
		weights.put(PipelineOperator.SHIFT_RIGHT, Arrays.asList(0.1f, 0.2f));
		weights.put(PipelineOperator.STATE_LOAD, Arrays.asList(5.0f, 5.0f));
		weights.put(PipelineOperator.STATE_STORE, Arrays.asList(5.0f, 5.0f));
		weights.put(PipelineOperator.TIMES, Arrays.asList(1.0f, 5.0f));

		WEIGHTS = Collections.unmodifiableMap(weights);
	}

	/**
	 * The current number of the instruction
	 */
	private int currentIntruction;

	/**
	 * The matrix that defines the inputs of operators
	 */
	private int[][] inputOperators;

	/**
	 * Number of instructions without PortRead and PortWrite
	 */
	private int nbrInstructions;

	/**
	 * The List of Operation
	 */
	private List<PipelineOperator> operators;

	/**
	 * The matrix that defines the outputs of the operators
	 */
	private int[][] outputOperators;

	/**
	 * The List of variables
	 */
	private List<Var> variables;

	@Override
	public Void caseBlockBasic(BlockBasic block) {

		for (Instruction instruction : block.getInstructions()) {
			if (instruction instanceof InstAssign
					|| instruction instanceof InstCast) {
				nbrInstructions++;
			}
		}

		inputOperators = new int[nbrInstructions][variables.size()];
		outputOperators = new int[nbrInstructions][variables.size()];

		return super.caseBlockBasic(block);
	}

	@Override
	public Void caseExprBinary(ExprBinary expr) {
		// Get operator
		OpBinary opBinary = expr.getOp();
		operators.add(PipelineOperator.getPipelineOperator(opBinary));

		// Input Variables
		Var varE1 = ((ExprVar) expr.getE1()).getUse().getVariable();
		Var varE2 = ((ExprVar) expr.getE2()).getUse().getVariable();

		inputOperators[currentIntruction][variables.indexOf(varE1)] = 1;
		inputOperators[currentIntruction][variables.indexOf(varE2)] = 1;

		return null;
	}

	@Override
	public Void caseExprVar(ExprVar object) {
		// Get the variable
		Var source = object.getUse().getVariable();
		inputOperators[currentIntruction][variables.indexOf(source)] = 1;

		return null;
	}

	@Override
	public Void caseInstAssign(InstAssign assign) {
		// Visit Value
		doSwitch(assign.getValue());

		if (assign.getValue().isExprInt()) {
			operators.add(PipelineOperator.ASSIGN);
		}

		// Output variables
		Var target = assign.getTarget().getVariable();
		outputOperators[currentIntruction][variables.indexOf(target)] = 1;

		// Increment the Instruction counter
		currentIntruction++;
		return null;
	}

	public Void caseInstCast(InstCast cast) {
		// Equivalent operator
		operators.add(PipelineOperator.CAST);

		// Input variables
		Var source = cast.getSource().getVariable();
		inputOperators[currentIntruction][variables.indexOf(source)] = 1;

		// Output variables
		Var target = cast.getTarget().getVariable();
		outputOperators[currentIntruction][variables.indexOf(target)] = 1;

		// Increment the Instruction counter
		currentIntruction++;
		return null;
	}

	@Override
	public Void caseProcedure(Procedure procedure) {
		// Init this procedure
		this.procedure = procedure;
		operators = new ArrayList<PipelineOperator>();
		variables = new ArrayList<Var>();
		nbrInstructions = 0;
		currentIntruction = 0;

		// Get the local variables
		for (Var var : procedure.getLocals()) {
			variables.add(var);
		}

		// Now Visit the Blocks
		doSwitch(procedure.getBlocks());
		return null;
	}

	@Override
	public Void defaultCase(EObject object) {
		if (object instanceof InstCast) {
			return caseInstCast((InstCast) object);
		}
		return null;
	}

}
