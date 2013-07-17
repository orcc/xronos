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

import org.eclipse.emf.common.util.Enumerator;
import org.eclipse.emf.ecore.EObject;

public class TestBenchIO extends AbstractIrVisitor<Void> {

	private static Map<Enumerator, List<Float>> WEIGHTS;

	static {
		Map<Enumerator, List<Float>> weights = new HashMap<Enumerator, List<Float>>();
		weights.put(OpBinary.BITAND, Arrays.asList(0.15f, 0.5f));
		weights.put(OpBinary.BITOR, Arrays.asList(0.15f, 0.5f));
		weights.put(OpBinary.DIV, Arrays.asList(2.0f, 10.0f));
		weights.put(OpBinary.DIV_INT, Arrays.asList(2.0f, 10.0f));
		weights.put(OpBinary.EQ, Arrays.asList(0.1f, 0.2f));
		weights.put(OpBinary.GE, Arrays.asList(0.1f, 0.2f));
		weights.put(OpBinary.GT, Arrays.asList(0.1f, 0.2f));
		weights.put(OpBinary.LE, Arrays.asList(0.1f, 0.2f));
		weights.put(OpBinary.LT, Arrays.asList(0.1f, 0.2f));
		weights.put(OpBinary.LOGIC_AND, Arrays.asList(0.5f, 0.2f));
		weights.put(OpBinary.LOGIC_OR, Arrays.asList(0.5f, 0.2f));
		weights.put(OpBinary.MINUS, Arrays.asList(1.0f, 2.0f));
		weights.put(OpBinary.MOD, Arrays.asList(2.0f, 10.0f));
		weights.put(OpBinary.NE, Arrays.asList(0.1f, 0.2f));
		weights.put(OpBinary.PLUS, Arrays.asList(1.0f, 1.0f));
		weights.put(OpBinary.SHIFT_LEFT, Arrays.asList(0.1f, 0.2f));
		weights.put(OpBinary.SHIFT_RIGHT, Arrays.asList(0.1f, 0.2f));
		weights.put(OpBinary.TIMES, Arrays.asList(1.0f, 5.0f));

		WEIGHTS = Collections.unmodifiableMap(weights);
	}

	/**
	 * Define the number of stages
	 */
	private int stages;

	/**
	 * Define the time of a Stage
	 */
	private float stageTime;

	/**
	 * The List of Operation
	 */
	private List<Enumerator> operators;

	/**
	 * The List of variables
	 */
	private List<Var> variables;

	/**
	 * The matrix that defines the inputs of operators
	 */
	private int[][] inputOperators;

	/**
	 * The matrix that defines the outputs of the operators
	 */
	private int[][] outputOperators;

	/**
	 * Number of instructions without PortRead and PortWrite
	 */
	private int nbrInstructions;

	/**
	 * The current number of the instruction
	 */
	private int currentIntruction;

	public TestBenchIO(int stages, float stageTime) {
		this.stages = stages;
		this.stageTime = stageTime;
	}

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
		operators.add(opBinary);

		// Input Variables
		Var varE1 = ((ExprVar) expr.getE1()).getUse().getVariable();
		Var varE2 = ((ExprVar) expr.getE2()).getUse().getVariable();
		return null;
	}

	@Override
	public Void caseInstAssign(InstAssign assign) {
		// Visit Value
		doSwitch(assign.getValue());

		// Output variables
		Var target = assign.getTarget().getVariable();

		return null;
	}

	public Void caseInstCast(InstCast cast) {
		// Equivalent operator
		operators.add(OpBinary.EQ);

		// Output variables
		Var target = cast.getTarget().getVariable();

		// Input variables
		Var source = cast.getSource().getVariable();

		return null;
	}

	@Override
	public Void caseProcedure(Procedure procedure) {
		// Init this procedure
		this.procedure = procedure;
		operators = new ArrayList<Enumerator>();
		variables = new ArrayList<Var>();
		nbrInstructions = 0;
		currentIntruction = 0;

		// Get the local variables
		for (Var var : procedure.getLocals()) {
			variables.add(var);
		}

		// Now Visit the Blocks
		doSwitch(procedure.getBlocks());

		// Build Input - Operator Matrix

		// Buidle Output - Operator Matrix

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
