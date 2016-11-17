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
 * This class contains the output(s) of the operators
 * 
 * @author Anatoly Prihozhy
 * 
 */
public class OperatorOutputs {

	/**
	 * The inputs of operators
	 */
	public int[] objOuts;

	/**
	 * Number of Operators
	 */
	public int N;

	/**
	 * Number of Variables
	 */
	public int M;

	public OperatorOutputs(TestBench tB) {
		create(tB);
	}

	/**
	 * Create the input objects
	 * 
	 * @param tB
	 */
	public void create(TestBench tB) {
		N = tB.N;
		M = tB.M;
		objOuts = new int[N * M];
		for (int i = 0; i < N; i++) {
			for (int j = 0; j < M; j++) {
				objOuts[i * M + j] = tB.H[i * M + j];
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
			out.write("MatrixH:" + "\n");
			for (int i = 0; i < N; i++) {
				for (int j = 0; j < M; j++) {
					out.write(objOuts[i * M + j] + " ");
				}
				out.write("\n");
			}

			// out.close();
		} catch (IOException e) {
		}
		return true;
	}
}
