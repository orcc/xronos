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
 * If you modify this Program, or any covered work, by linking or 
 * combining it with Eclipse libraries (or a modified version of that 
 * library), containing parts covered by the terms of EPL,
 * the licensors of this Program grant you additional permission to convey 
 * the resulting work. {Corresponding Source for a non-source form of such 
 * a combination shall include the source code for the parts of Eclipse 
 * libraries used as well as that of the  covered work.}
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
