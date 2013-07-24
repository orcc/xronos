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
import net.sf.orcc.df.util.DfVisitor;

/**
 * The pipelining engine transformation
 * 
 * @author Endri Bezati
 * 
 */
public class Pipelining extends DfVisitor<Void> {

	/**
	 * Define the number of stages
	 */
	private int stages;

	/**
	 * Define the time of a Stage
	 */
	private float stageTime;

	public Pipelining(int stages, float stageTime) {
		this.stages = stages;
		this.stageTime = stageTime;
	}

	@Override
	public Void caseAction(Action action) {
		// Apply iff the action has the xronos_pipeline tag
		if (action.hasAttribute("xronos_pipeline")) {
			float stageTime = 3.0f;
			// Get the Input and Output matrix of the operators found on the
			// BlockBasic of the action
			OperatorsIO opIO = new OperatorsIO();
			opIO.doSwitch(action.getBody());

			// Operator precedence
			int nbrOperators = opIO.getNbrOperators();
			int nbrVariables = opIO.getNbrVariables();
			int inputOp[][] = opIO.getInputOp();
			int outputOp[][] = opIO.getOutputOp();

			OperatorPrecedence opPre = new OperatorPrecedence(nbrOperators,
					nbrVariables, inputOp, outputOp);
			opPre.evaluate();

			// Longest Operation path
			LongestOpPath longestOpPath = new LongestOpPath(nbrOperators);
			longestOpPath.constructPath(opIO, opPre, stageTime);

			// Operator conflicts
			OperatorConflicts opCon = new OperatorConflicts(nbrOperators);
			opCon.create(longestOpPath, stageTime);
			// operatorConflicts.print();

			// Operation coloring
			opPre.transitiveClosure();
			OperatorColoring opCol = new OperatorColoring(nbrOperators,
					nbrVariables);
			opCol.construct(opIO, opPre, opCon);

		}
		return null;
	}
}
