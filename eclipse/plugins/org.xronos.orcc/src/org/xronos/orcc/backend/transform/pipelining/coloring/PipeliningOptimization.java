/* 
 * XRONOS, High Level Synthesis of Streaming Applications
 * 
 * Copyright (C) 2014 EPFL SCI STI MM
 *
 * This file is part of XRONOS.
 *
 * XRONOS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * XRONOS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with XRONOS.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.xronos.orcc.backend.transform.pipelining.coloring;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * This class contains static methods for launching the pipeline optimization
 * 
 * @author Anatoly Prihozhy
 * 
 */
public class PipeliningOptimization {

	/**
	 * The testbench to apply the pipelining
	 */
	private TestBench testBench;

	/**
	 * The log path
	 */
	private String logPath;

	/**
	 * The stage inputs
	 */

	private List<List<String>> stageInputs;

	/**
	 * The stage outputs
	 */
	private List<List<String>> stageOutputs;

	/**
	 * The stage operators
	 */
	private List<List<Integer>> stageOperators;

	/**
	 * The number of stages that are going top be generated from the pipeline
	 * optimization
	 */
	private Integer nbrStages;

	public PipeliningOptimization(TestBench testBench, String logPath) {
		this.testBench = testBench;
		this.logPath = logPath;
	}

	/**
	 * Get the number of stages
	 * 
	 * @return
	 */
	public Integer getNbrStages() {
		return nbrStages;
	}

	/**
	 * Get stage inputs
	 * 
	 * @return
	 */
	public List<String> getStageInputs(int stage) {
		return stageInputs.get(stage);
	}

	/**
	 * Get stage Operators
	 * 
	 * @return
	 */
	public List<Integer> getStageOperators(int stage) {
		return stageOperators.get(stage);
	}

	/**
	 * Get stage outputs
	 * 
	 * @return
	 */
	public List<String> getStageOutputs(int stage) {
		return stageOutputs.get(stage);
	}

	public void run() {
		try {
			File file = new File(logPath);
			FileWriter fileWriter;
			fileWriter = new FileWriter(file);
			BufferedWriter out = new BufferedWriter(fileWriter);
			// Matrix F - Operator Inputs
			OperatorInputs F = new OperatorInputs(testBench);
			F.print(out);

			// Matrix H output - Operator Outputs
			OperatorOutputs H = new OperatorOutputs(testBench);
			H.print(out);

			// MatrixP - Precedence Relation
			OperatorPrecedence P = new OperatorPrecedence(testBench, F, H);
			P.print(out);

			// VectorOP - Operator types and parameters
			OperatorParameters Op = new OperatorParameters(testBench);
			Op.print(out);

			// VectorVs - Variable parameters
			VariableParameters Vs = new VariableParameters(testBench);
			Vs.print(out);

			// MatrixG - Longest operator paths
			LongestOperPath G = new LongestOperPath(Op, P, testBench.stageTime);
			G.print(out);

			// MatrixCop - Operator conflicts
			OperatorConflicts Cop = new OperatorConflicts(G,
					testBench.stageTime);
			Cop.print(out);

			// VectorColO - Operator coloring
			P.transitiveClosure();
			P.print(out);

			stageInputs = new ArrayList<List<String>>();
			stageOutputs = new ArrayList<List<String>>();
			stageOperators = new ArrayList<List<Integer>>();
			OperatorColoring ColO = new OperatorColoring(testBench, out);
			ColO.optimizePipeline(Cop, F, H, P, Op, Vs, stageInputs,
					stageOutputs, stageOperators);
			nbrStages = ColO.getNbrStages();
			out.close();

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
