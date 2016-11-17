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
