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
import net.sf.orcc.df.Action;
import net.sf.orcc.ir.BlockBasic;
import net.sf.orcc.ir.ExprBinary;
import net.sf.orcc.ir.ExprVar;
import net.sf.orcc.ir.Expression;
import net.sf.orcc.ir.InstAssign;
import net.sf.orcc.ir.Instruction;
import net.sf.orcc.ir.OpBinary;
import net.sf.orcc.ir.Procedure;
import net.sf.orcc.ir.Type;
import net.sf.orcc.ir.Use;
import net.sf.orcc.ir.Var;
import net.sf.orcc.ir.util.AbstractIrVisitor;
import net.sf.orcc.util.util.EcoreHelper;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.xronos.orcc.backend.transform.pipelining.coloring.OperatorType;
import org.xronos.orcc.backend.transform.pipelining.coloring.TestBench;

/**
 * This class is creating two matrix that defines the DFG of an Procedure by
 * giving the Matrix of input-operators and output-operators.
 * 
 * @author Endri Bezati
 * 
 */
public class ExtractOperatorsIO extends AbstractIrVisitor<Void> {

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

	private Map<Integer, Expression> constantInstructions;

	private Integer nbrReads;

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

		constantInstructions = new HashMap<Integer, Expression>();

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
		inputs.add(varE2.getIndexedName());
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

		if (assign.getValue().isExprInt() || assign.getValue().isExprBool()
				|| assign.getValue().isExprFloat()) {
			operators.add(PipelineOperator.ASSIGN);
			List<String> inputs = new ArrayList<String>();
			inputs.add("");
			inputs.add("");
			inputOpString.add(inputs);
			Integer position = assign.getBlock().getInstructions()
					.indexOf(assign)
					- nbrReads;
			constantInstructions.put(position, assign.getValue());
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

		// Get the number of tokens reads
		Action action = EcoreHelper.getContainerOfType(procedure, Action.class);
		nbrReads = action.getInputPattern().getPorts().size();

		// Get the local variables
		for (Var var : procedure.getLocals()) {
			// Add and clean variables
			for (Use use : var.getUses()) {
				Procedure procContainsVar = EcoreHelper.getContainerOfType(use,
						Procedure.class);
				if (procContainsVar != null) {
					if (procContainsVar == procedure) {
						if (!variables.contains(var)) {
							variables.add(var);
						}
					}
				}
			}
		}

		// Now Visit the Blocks
		doSwitch(procedure.getBlocks());
		return null;
	}

	public void cleanVariables() {
		List<Var> varToBeDeleted = new ArrayList<Var>();

		for (Var var : variables) {
			String name = var.getIndexedName();
			if (!containsInput(name) && !outputOpString.contains(name)) {
				varToBeDeleted.add(var);
			}
		}

		for (Var var : varToBeDeleted) {
			variables.remove(var);
		}
	}

	public boolean containsInput(String name) {
		for (List<String> ins : inputOpString) {
			for (String in : ins) {
				if (in.equals(name)) {
					return true;
				}
			}
		}
		return false;
	}

	public TestBench createTestBench(float stageTime) {
		// An operator has maximum two inputs
		int IC = 2;
		// An operator has maximum one output
		int OC = 1;
		// Create the OperatorType OpT array
		OperatorType[] OpT = new OperatorType[operators.size()];

		int i = 0;
		for (PipelineOperator pipelineOperator : operators) {
			String name = pipelineOperator.name();

			// Test if this operator is already in the OpT array
			boolean contains = false;
			for (int j = 0; j < i; j++) {
				String opName = OpT[j].getName();
				if (name.equals(opName)) {
					contains = true;
					break;
				}
			}
			if (!contains) {
				float time = pipelineOperator.getTimeWeight();
				float cost = pipelineOperator.getResourceWeight();

				OpT[i] = new OperatorType(name, time, cost);
				i++;
			}
		}
		// Number of Operators Type
		int T = i;

		// Number of Operators
		int NC = nbrOperators;

		// Create the Operator Name array
		i = 0;
		String[] OpNN = new String[nbrOperators];
		for (PipelineOperator pipelineOperator : operators) {
			OpNN[i] = pipelineOperator.toString();
			i++;
		}

		// Number of variables
		int MC = variables.size();

		// Create the Variables array
		i = 0;
		String[] VaNN = new String[variables.size()];
		for (Var var : variables) {
			VaNN[i] = var.getIndexedName();
			i++;
		}

		// Create the Variables width array
		i = 0;
		int[] VaWW = new int[variables.size()];
		for (Var var : variables) {
			VaWW[i] = var.getType().getSizeInBits();
			i++;
		}

		// Create the Operators Inputs array
		i = 0;
		String[][] chFF = new String[NC][IC];
		for (List<String> ins : inputOpString) {
			if (ins.size() == 1) {
				chFF[i][0] = ins.get(0);
				chFF[i][1] = "";
			} else if (ins.size() == 2) {
				chFF[i][0] = ins.get(0);
				chFF[i][1] = ins.get(1);
			}
			i++;
		}
		// Create the Operators Outputs array
		i = 0;
		String[][] chHH = new String[NC][OC];
		for (String output : outputOpString) {
			chHH[i][0] = output;
			i++;
		}
		TestBench testbench = new TestBench(T, NC, MC, 2, stageTime);
		testbench.setData(OpT, IC, OC, OpNN, VaNN, VaWW, chFF, chHH);
		return testbench;
	}

	@Override
	public Void defaultCase(EObject object) {
		if (object instanceof InstCast) {
			return caseInstCast((InstCast) object);
		}
		return null;
	}

	public Expression getConstantExpression(Integer position) {
		return constantInstructions.get(position);
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

	public List<String> getInputs(int index) {
		return inputOpString.get(index);
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

	public List<Var> getOpInputVariables(int index) {
		List<Var> inputs = new ArrayList<Var>();

		for (int i = 0; i < getNbrVariables(); i++) {
			if (inputOp[index][i] == 1) {
				inputs.add(variables.get(i));
			}
		}
		return inputs;
	}

	public Var getOpOutputVariable(int index) {
		Var output = null;
		for (int i = 0; i < getNbrVariables(); i++) {
			if (inputOp[index][i] == 1) {
				return variables.get(i);
			}
		}
		return output;
	}

	public String getOutput(int index) {
		return outputOpString.get(index);
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

	public Type getVariableType(String name) {
		for (Var var : variables) {
			if (var.getIndexedName().equals(name)) {
				return EcoreUtil.copy(var.getType());
			}
		}

		return null;

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
