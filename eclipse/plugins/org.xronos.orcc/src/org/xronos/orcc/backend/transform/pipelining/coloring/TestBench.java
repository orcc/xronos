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

/**
 * 
 * Construct a Pipeline Testbench
 * 
 * @author Endri Bezati
 * 
 */
public class TestBench {

	public int T;

	public int N;

	public int M;

	public int[] F;

	public int[] H;

	public float stageTime;

	public OperatorType[] opT;

	public String[] opN;

	public String[] vaN;

	public int[] vaW;

	int MODE;

	public String[][] chFF;

	public String[][] chHH;

	public int IC;

	public int OC;

	public TestBench(int T, int N, int M, int MODE, float stageTime) {
		this.T = T;
		this.N = N;
		this.M = M;
		this.MODE = MODE;
		this.stageTime = stageTime;
	}

	public void setData(OperatorType[] OpT, int IC, int OC, String[] OpNN,
			String[] VaNN, int[] VaWW, String[][] chFF, String[][] chHH) {

		// Set the different operators Type
		this.opT = OpT;

		this.opN = OpNN;

		this.vaN = VaNN;

		this.vaW = VaWW;

		this.chFF = chFF;

		this.chHH = chHH;

		this.IC = IC;

		this.OC = OC;

		F = new int[N * M];
		H = new int[N * M];

		// Construct F and H matrix

		for (int i = 0; i < N; i++) {
			for (int j = 0; j < M; j++) {
				F[i * M + j] = 0;
				for (int k = 0; k < IC; k++) {
					if (chFF[i][k].equals(VaNN[j])) {
						F[i * M + j] = 1;
					}
					if (chFF[i][k] == null || chFF[i][k] == "") {
						break;
					}
				}
				H[i * M + j] = 0;
				for (int k = 0; k < OC; k++) {
					if (chHH[i][k].equals(VaNN[j])) {
						H[i * M + j] = 1;
					}
					if (chHH[i][k] == null || chHH[i][k] == "") {
						break;
					}

				}
			}
		}

	}
}
