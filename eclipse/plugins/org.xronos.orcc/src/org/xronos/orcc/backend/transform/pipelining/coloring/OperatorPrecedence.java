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
 * 
 * Additional permission under GNU GPL version 3 section 7
 * 
 * If you modify this Program, or any covered work, by linking or combining it
 * with Eclipse (or a modified version of Eclipse or an Eclipse plugin or 
 * an Eclipse library), containing parts covered by the terms of the 
 * Eclipse Public License (EPL), the licensors of this Program grant you 
 * additional permission to convey the resulting work.  Corresponding Source 
 * for a non-source form of such a combination shall include the source code 
 * for the parts of Eclipse libraries used as well as that of the  covered work.
 * 
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
