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
 * This class defines the operator conflicts between stages
 * 
 * @author Anatoly Prihozhy
 * 
 */
public class OperatorConflicts {

	/**
	 * The number of operators
	 */
	public int N;

	/**
	 * The array that contains the operator conflicts
	 */
	public int[] opConfl;

	public OperatorConflicts(LongestOperPath G, float tStage) {
		N = G.N;
		opConfl = new int[N * N];
		create(G, tStage);
	}

	/**
	 * Create the operator conflicts array
	 * 
	 * @param G
	 * @param tStage
	 */
	private void create(LongestOperPath G, float tStage) {
		for (int i = 0; i < N; i++) {
			for (int j = 0; j < N; j++) {
				if (G.longPath[i * N + j] <= tStage) {
					opConfl[i * N + j] = 0;
				} else {
					opConfl[i * N + j] = 1;
				}
			}
		}
	}

	/**
	 * Print the Operation conflict matrix
	 * 
	 * @param out
	 */
	public boolean print(BufferedWriter out) {

		if (N == 0) {
			return false;
		}
		try {
			out.write("MatrixCop:\n");
			for (int i = 0; i < N; i++) {
				for (int j = 0; j < N; j++) {
					out.write(opConfl[i * N + j] + " ");
				}
				out.write("\n");
			}
		} catch (IOException e) {
			return false;
		}
		return true;
	}

	/**
	 * Update the operator conflicts
	 * 
	 * @param G
	 * @param tStage
	 */
	public void update(LongestOperPath G, float tStage) {
		N = G.N;
		create(G, tStage);
	}

}
