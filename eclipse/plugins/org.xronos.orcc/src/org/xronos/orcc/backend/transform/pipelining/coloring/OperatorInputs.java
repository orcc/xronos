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
 * This class contains the input of the operators
 * 
 * @author Anatoly Prihozhy
 * 
 */
public class OperatorInputs {

	/**
	 * The inputs of operators
	 */
	public int[] objIns;

	/**
	 * Number of Operators
	 */
	public int N;

	/**
	 * Number of Variables
	 */
	public int M;

	public OperatorInputs(TestBench tB) {
		N = tB.N;
		M = tB.M;
		objIns = new int[N * M];

		create(tB);
	}

	/**
	 * Create the input objects
	 * 
	 * @param tB
	 */
	private void create(TestBench tB) {
		for (int i = 0; i < N; i++) {
			for (int j = 0; j < M; j++) {
				objIns[i * M + j] = tB.F[i * M + j];
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
		if (M == 0) {
			return false;
		}
		try {
			out.write("MatrixF:" + "\n");
			for (int i = 0; i < N; i++) {
				for (int j = 0; j < M; j++) {
					out.write(objIns[i * M + j] + " ");
				}
				out.write("\n");
			}

			// out.close();
		} catch (IOException e) {
		}
		return true;
	}
}
