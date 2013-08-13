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
