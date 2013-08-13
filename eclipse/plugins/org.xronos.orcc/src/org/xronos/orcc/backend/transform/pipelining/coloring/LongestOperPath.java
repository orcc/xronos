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
package org.xronos.orcc.backend.transform.pipelining.coloring;

import java.io.BufferedWriter;
import java.io.IOException;

/**
 * This class constructs the longest path of operators
 * 
 * @author Anatoly Prihozhy
 * @author Endri Bezati
 * 
 */
public class LongestOperPath {

	/**
	 * The longest path
	 */
	public float[] longPath;

	/**
	 * An array of flags
	 */
	public int[] flags;

	/**
	 * The number of operators
	 */
	public int N;

	public LongestOperPath(OperatorParameters opParameters,
			OperatorPrecedence opPrecedence, float stageTime) {
		N = opPrecedence.N;
		longPath = new float[N * N];
		flags = new int[N];
		for (int i = 0; i < N; i++) {
			for (int j = 0; j < N; j++) {
				longPath[i * N + j] = 0.0f;
			}
			flags[i] = 0;
			longPath[i * N + i] = opParameters.getTime(i);
			if (longPath[i * N + i] > stageTime) {
				try {
					throw new PipelineException(
							"ERROR: StageTime is less than an operator time !!!");
				} catch (PipelineException e) {
					e.printStackTrace();
				}
			}
		}

		markSources(opPrecedence);
		evaluateLP(opPrecedence);
	}

	/**
	 * Evaluate the longest Path
	 * 
	 * @param opPrecedence
	 */
	private void evaluateLP(OperatorPrecedence opPrecedence) {
		boolean update = true;
		while (update) {
			update = false;
			for (int i = 0; i < N; i++) {
				if (flags[i] == 0) {
					int flag = 1;
					for (int j = 0; j < N; j++) {
						if (opPrecedence.oPreced[j * N + i] != 0
								&& flags[j] == 0) {
							flag = 0;
							break;
						}
					}
					if (flag == 1) {
						flags[i] = 1;
						update = true;
						// first the paths between direct predecessors and the
						// node are computed
						for (int j = 0; j < N; j++) {
							if (opPrecedence.oPreced[j * N + i] != 0) {
								longPath[j * N + i] = longPath[i * N + i]
										+ longPath[j * N + j];
							}
						}
						// second the paths between predecessors of direct
						// predecessors and the node are computed
						for (int j = 0; j < N; j++) {
							float max = 0.0f;
							for (int k = 0; k < N; k++) {
								if (longPath[j * N + k] != 0.0f
										&& opPrecedence.oPreced[k * N + i] != 0) {
									if (max < longPath[j * N + k]) {
										max = longPath[j * N + k];
									}
								}
							}
							if (max != 0.0f) {
								longPath[j * N + i] = max + longPath[i * N + i];
							}
						}
					}
				}
			}
			// print_flags();
		}
	}

	/**
	 * Mark sources
	 * 
	 * @param opPrecedence
	 */
	private void markSources(OperatorPrecedence opPrecedence) {
		for (int i = 0; i < N; i++) {
			int flag = 1;
			for (int j = 0; j < N; j++) {
				if (opPrecedence.oPreced[j * N + i] != 0) {
					flag = 0;
					break;
				}
			}
			flags[i] = flag;
		}
	}

	/**
	 * Print out the longest path
	 * 
	 * @param out
	 * @return
	 */
	public boolean print(BufferedWriter out) {
		if (N == 0) {
			return false;
		}
		try {
			out.write("\nMatrixG:\n");
			for (int i = 0; i < N; i++) {
				for (int j = 0; j < N; j++) {
					out.write(" " + longPath[i * N + j]);
				}
			}
			out.write("\n");
			return true;
		} catch (IOException e) {
			return false;
		}
	}

	/**
	 * Pint out the longest path flags
	 * 
	 * @param out
	 * @return
	 */
	public boolean printFlags(BufferedWriter out) {
		if (N == 0) {
			return false;
		}
		try {
			for (int i = 0; i < N; i++) {
				int j = i + 1;
				out.write(j + ":" + flags[i]);
			}
			out.write("\n");
			return true;
		} catch (IOException e) {
			return false;
		}
	}
}
