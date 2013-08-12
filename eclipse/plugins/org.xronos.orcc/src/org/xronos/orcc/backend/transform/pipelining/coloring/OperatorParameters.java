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
 * This class defines the Operators parameters
 * 
 * @author Anatoly Prihozhy
 * 
 */
public class OperatorParameters {

	/**
	 * Array of operators types
	 */
	private OperatorType[] operatorTypes;

	/**
	 * Number of different operator types
	 */
	private int T;

	/**
	 * Number of Operators
	 */
	private int N;

	/**
	 * Array of operators names
	 */
	private String[] optNames;

	public OperatorParameters(TestBench tB) {
		T = tB.T;
		N = tB.N;

		operatorTypes = new OperatorType[T];
		for (int i = 0; i < T; i++) {
			operatorTypes[i] = tB.opT[i];
		}

		optNames = new String[N];
		for (int i = 0; i < N; i++) {
			optNames[i] = tB.opN[i];
		}
	}

	/**
	 * Get the time of an operator
	 * 
	 * @param op
	 * @return
	 */
	public float getTime(int op) {
		for (int i = 0; i < T; i++) {
			if (optNames[op] == operatorTypes[i].getName()) {
				return operatorTypes[i].getTime();
			}
		}
		return 0.0f;
	}

	/**
	 * Print the operators parameters
	 * 
	 * @return
	 */
	public boolean print(BufferedWriter out) {
		if (T == 0 || N == 0) {
			return false;
		}
		try {
			out.write("Operator type descriptions:\n");
			for (int i = 0; i < T; i++) {
				operatorTypes[i].print(out);
				out.write("\n");
			}
			out.write("\n");
			out.write("Types of all operators:\n");
			for (int i = 0; i < N; i++) {
				if (i % 10 == 0) {
					out.write("\n");
				}
				int j = i + 1;
				out.write(j + ":" + optNames[i] + " ");
			}
			out.write("\n");
		} catch (IOException e) {
			return false;
		}

		return true;
	}
}
