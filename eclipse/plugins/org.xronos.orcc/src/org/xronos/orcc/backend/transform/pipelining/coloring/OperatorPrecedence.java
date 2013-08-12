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
 * This class constructs and contains the operator precedence
 * 
 * @author Anatoly Prihozhy
 * 
 */
public class OperatorPrecedence {

	/**
	 * The inputs of operators
	 */
	public int[] oPreced;

	/**
	 * Number of Operators
	 */
	public int N;

	/**
	 * Number of Variables
	 */
	public int M;

	public OperatorPrecedence(TestBench tB, OperatorInputs F, OperatorOutputs H) {
		N = tB.N;
		M = tB.M;
		oPreced = new int[N * N];
		evaluate(F, H);
	}

	/**
	 * Evaluate the operator precedence
	 * 
	 * @param F
	 *            the operator inputs
	 * @param H
	 *            the operator outputs
	 */
	public void evaluate(OperatorInputs F, OperatorOutputs H) {
		for (int i = 0; i < N; i++) {
			for (int j = 0; j < N; j++) {
				for (int k = 0; k < M; k++) {
					if (H.objOuts[i * M + k] != 0 && F.objIns[j * M + k] != 0) {
						oPreced[i * N + j] = 1;
					}
				}
			}
		}
	}

	/**
	 * Print the input objects
	 * 
	 * @return
	 */
	public boolean print(BufferedWriter out) {
		if (N == 0) {
			return false;
		}
		try {
			out.write("MatrixP:" + "\n");
			for (int i = 0; i < N; i++) {
				for (int j = 0; j < N; j++) {
					out.write(oPreced[i * N + j] + " ");
				}
				out.write("\n");
			}

		} catch (IOException e) {
		}
		return true;
	}

	/**
	 * Calculate the transitive closure of the operator precedence
	 * 
	 * @return
	 */
	public boolean transitiveClosure() {
		boolean flag = true;
		while (flag) {
			flag = false;
			for (int i = 0; i < N; i++) {
				for (int j = 0; j < N; j++) {
					if (oPreced[i * N + j] == 0) {
						for (int k = 0; k < N; k++) {
							if (oPreced[i * N + k] == 1
									&& oPreced[k * N + j] == 1) {
								oPreced[i * N + j] = 1;
								flag = true;
							}
						}
					}
				}
			}
		}
		return true;
	}

}
