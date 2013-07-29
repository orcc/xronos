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
import java.util.List;

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

	/**
	 * The current number of the instruction
	 */
	private int currentIntruction;

	/**
	 * The matrix that defines the inputs of operators
	 */
	private int[][] inputOp;

	private List<List<String>> inputOpString;

	/**
	 * Number of instructions without PortRead and PortWrite
	 */
	private int nbrOperators;

	/**
	 * The List of Operation
	 */
	private List<PipelineOperator> operators;

	/**
	 * The matrix that defines the outputs of the operators
	 */
	private int[][] outputOp;

	private List<String> outputOpString;

	/**
	 * The List of variables
	 */
	private List<Var> variables;

	@Override
	public Void caseBlockBasic(BlockBasic block) {

		for (Instruction instruction : block.getInstructions()) {
			if (instruction instanceof InstAssign
					|| instruction instanceof InstCast) {
				nbrOperators++;
			}
		}

		inputOp = new int[nbrOperators][variables.size()];
		outputOp = new int[nbrOperators][variables.size()];

		inputOpString = new ArrayList<List<String>>();
		outputOpString = new ArrayList<String>();

		super.caseBlockBasic(block);

		return null;
	}

	@Override
	public Void caseExprBinary(ExprBinary expr) {
		// Get operator
		OpBinary opBinary = expr.getOp();
		operators.add(PipelineOperator.getPipelineOperator(opBinary));

		// Input Variables
		Var varE1 = ((ExprVar) expr.getE1()).getUse().getVariable();
		Var varE2 = ((ExprVar) expr.getE2()).getUse().getVariable();

		inputOp[currentIntruction][variables.indexOf(varE1)] = 1;
		inputOp[currentIntruction][variables.indexOf(varE2)] = 1;
		List<String> inputs = new ArrayList<String>();
		inputs.add(varE1.getIndexedName());
		inputs.add(varE1.getIndexedName());
		inputOpString.add(inputs);

		return null;
	}

	@Override
	public Void caseExprVar(ExprVar object) {
		// Get the variable
		Var source = object.getUse().getVariable();
		inputOp[currentIntruction][variables.indexOf(source)] = 1;
		operators.add(PipelineOperator.ASSIGN);

		List<String> inputs = new ArrayList<String>();
		inputs.add(source.getIndexedName());
		inputOpString.add(inputs);

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
		outputOp[currentIntruction][variables.indexOf(target)] = 1;
		outputOpString.add(target.getIndexedName());

		// Increment the Instruction counter
		currentIntruction++;
		return null;
	}

	public Void caseInstCast(InstCast cast) {
		// Equivalent operator
		operators.add(PipelineOperator.CAST);

		// Input variables
		Var source = cast.getSource().getVariable();
		inputOp[currentIntruction][variables.indexOf(source)] = 1;

		List<String> inputs = new ArrayList<String>();
		inputs.add(source.getIndexedName());
		inputOpString.add(inputs);

		// Output variables
		Var target = cast.getTarget().getVariable();
		outputOp[currentIntruction][variables.indexOf(target)] = 1;
		outputOpString.add(target.getIndexedName());

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
		nbrOperators = 0;
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

	/**
	 * Get the input operation matrix
	 * 
	 * @return
	 */
	public int[][] getInputOp() {
		return inputOp;
	}

	/**
	 * Get the input operation by index
	 * 
	 * @return
	 */
	public int getInputOp(int i, int j) {
		return inputOp[i][j];
	}

	/**
	 * Get the number of operators
	 * 
	 * @return
	 */
	public int getNbrOperators() {
		return nbrOperators;
	}

	/**
	 * Get the number of variables
	 */
	public int getNbrVariables() {
		return variables.size();
	}

	public PipelineOperator getOperator(int index) {
		return operators.get(index);
	}

	/**
	 * Get the output operation matrix
	 * 
	 * @return
	 */
	public int[][] getOutputOp() {
		return outputOp;
	}

	/**
	 * Get the output operation by index
	 * 
	 * @return
	 */
	public int getOutputOp(int i, int j) {
		return outputOp[i][j];
	}

	public String getVariableName(int index) {
		return variables.get(index).getIndexedName();
	}

	public int getVariableWidth(int index) {
		return variables.get(index).getType().getSizeInBits();
	}

	public void printTablesForCTestbench() {
		System.out.println("#define NC " + nbrOperators);
		System.out.println("#define MC " + variables.size());

		System.out.println("static char *OpNN[NC] = {");
		for (PipelineOperator pipeOP : operators) {
			System.out.print("\"" + pipeOP.toString() + "\", ");

		}
		System.out.println("};");

		System.out.println("static char *VaNN[MC] = {");
		for (Var var : variables) {
			System.out.print("\"" + var.getIndexedName() + "\", ");
		}
		System.out.println("};");

		System.out.println("int VaWW[MC] = {");
		for (Var var : variables) {
			System.out.print(var.getType().getSizeInBits() + ", ");
		}
		System.out.println("};");

		System.out.println("static char *chFF[NC][2] = {");
		for (List<String> ins : inputOpString) {
			if (ins.size() == 1) {
				System.out.print("{\"" + ins.get(0) + "\"" + ",\"\"}, ");
				System.out.print("\n");
			} else if (ins.size() == 2) {
				System.out.print("{\"" + ins.get(0) + "\"" + ",\"" + ins.get(1)
						+ "\"}, ");
				System.out.print("\n");
			}

		}
		System.out.println("};");

		System.out.println("static char *chHH[NC] = {");
		for (String outputs : outputOpString) {
			System.out.print("\"" + outputs + "\", ");
		}
		System.out.println("};");
	}

}
