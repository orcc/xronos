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

import net.sf.orcc.util.OrccLogger;

/**
 * This class calculates the longest path of operators on on operator precedence
 * matrix
 * 
 * @author Endri Bezati
 * 
 */
public class LongestOpPath {

	/**
	 * The longest path
	 */
	private float[][] longestPath;

	/**
	 * 
	 */
	private int[] flags;

	/**
	 * The number of operators
	 */
	private int nbrOperators;

	public LongestOpPath(int nbOperators) {
		this.nbrOperators = nbOperators;
		longestPath = new float[nbOperators][nbOperators];
		flags = new int[nbOperators];
	}

	/**
	 * Construct the Longest path given
	 * 
	 * @param opIO
	 *            The operators
	 * @param opPre
	 *            the operator precedence
	 * @param StageTime
	 *            the time of a stage
	 */
	public void constructPath(OperatorsIO opIO, OperatorPrecedence opPre,
			float StageTime) {

		for (int i = 0; i < nbrOperators; i++) {
			float opTime = opIO.getOperator(i).getTimeWeight();
			longestPath[i][i] = opTime;
			if (opTime > StageTime) {
				OrccLogger
						.severeln("Error: Pipelining, the StageTime is less than an operator Time !!!");
			}
		}
		// Mark the sources
		MarkSources(opPre);

		// Then evaluate the longest path
		evaluateLongestPath(opPre);
	}

	/**
	 * Evaluate the longest path
	 */

	public void evaluateLongestPath(OperatorPrecedence opPre) {
		boolean update = true;
		while (update) {
			update = false;
			for (int i = 0; i < nbrOperators; i++) {
				if (flags[i] == 0) {
					int flag = 1;
					for (int j = 0; j < nbrOperators; j++) {
						if (opPre.getPrecedenceOp(j, i) != 0 && flags[j] == 0) {
							flag = 0;
							break;
						}
					}
					if (flag == 1) {
						flags[i] = 1;
						update = true;
						// first the paths between direct predecessors and the
						// node are computed
						for (int j = 0; j < nbrOperators; j++) {
							if (opPre.getPrecedenceOp(j, i) != 0) {
								longestPath[j][i] = longestPath[i][i]
										+ longestPath[j][j];
							}
						}
						// second the paths between predecessors of direct
						// predecessors and the node are computed
						for (int j = 0; j < nbrOperators; j++) {
							float max = 0.0f;
							for (int k = 0; k < nbrOperators; k++) {
								if (longestPath[j][k] != 0.0f
										&& opPre.getPrecedenceOp(k, i) != 0) {
									if (max < longestPath[j][k]) {
										max = longestPath[j][k];
									}
								}
							}

							if (max != 0.0f) {
								longestPath[j][i] = max + longestPath[i][i];
							}
						}
					}
				}
			}
		}
	}

	public float getLogestPath(int i, int j) {
		return longestPath[i][j];
	}

	public void MarkSources(OperatorPrecedence opPre) {
		for (int i = 0; i < nbrOperators; i++) {
			int flag = 1;
			for (int j = 0; j < nbrOperators; j++) {
				if (opPre.getPrecedenceOp(j, i) == 0) {
					flag = 0;
					break;
				}
			}
			flags[i] = flag;
		}
	}

}
