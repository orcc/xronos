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
 * This class contains the variable parameters
 * 
 * @author Anatoly Prihozhy
 * 
 */
public class VariableParameters {

	/**
	 * The number of variables
	 */
	public int M;

	/**
	 * The array that contains the variables names
	 */
	public String[] varNames;

	/**
	 * The array that contains the variables widths
	 */
	public int[] varWidths;

	public VariableParameters(TestBench tB) {
		M = tB.M;
		varNames = new String[M];
		varWidths = new int[M];

		for (int i = 0; i < M; i++) {
			varNames[i] = tB.vaN[i];
			varWidths[i] = tB.vaW[i];
		}
	}

	public boolean print(BufferedWriter out) {
		try {
			if (M == 0) {
				return false;
			}
			out.write("Variable names:\n");
			for (int i = 0; i < M; i++) {
				if (i % 10 == 0) {
					out.write("\n");
				}
				int j = i + 1;
				out.write(j + ". " + varNames[i] + "\n");
			}
			out.write("\n");
			return true;
		} catch (IOException e) {
			return false;
		}
	}

}
