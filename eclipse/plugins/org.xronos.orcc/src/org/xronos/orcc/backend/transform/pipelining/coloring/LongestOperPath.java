/* 
 * XRONOS-EXELIXI
 * 
 * Copyright (C) 2011-2016 EPFL SCI STI MM
 *
 * This file is part of XRONOS-EXELIXI.
 *
 * XRONOS-EXELIXI is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * XRONOS-EXELIXI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with XRONOS-EXELIXI. If not, see <http://www.gnu.org/licenses/>.
 * 
 * Additional permission under GNU GPL version 3 section 7
 * 
 * If you modify this Program, or any covered work, by linking or combining it
 * with Eclipse (or a modified version of Eclipse or an Eclipse plugin or 
 * an Eclipse library), containing parts covered by the terms of the 
 * Eclipse Public License (EPL), the licensors of this Program grant you 
 * additional permission to convey the resulting work.  Corresponding Source 
 * for a non-source form of such a combination shall include the source code 
 * for the parts of Eclipse libraries used as well as that of the covered work.
 * 
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
