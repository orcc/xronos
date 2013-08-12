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
					if (chFF[i][k] == VaNN[j]) {
						F[i * M + j] = 1;
					}
					if (chFF[i][k] == null || chFF[i][k] == "") {
						break;
					}
				}
				H[i * M + j] = 0;
				for (int k = 0; k < OC; k++) {
					if (chHH[i][k] == VaNN[j]) {
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
