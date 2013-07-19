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

package org.xronos.orcc.backend.transform.pipelining;

public class OperatorPrecedence {

	/**
	 * The operation precedence matrix
	 */
	private int precedenceOp[][];

	/**
	 * The operation input matrix
	 */
	private int inputOp[][];

	/**
	 * The operation output matrix
	 */
	private int outputOp[][];

	/**
	 * The number of operators
	 */
	private int nbrOperators;

	/**
	 * The number of variables
	 */
	private int nbrVariables;

	public OperatorPrecedence(int nbrOperators, int nbrVariables,
			int inputOp[][], int outputOp[][]) {
		this.nbrOperators = nbrOperators;
		this.nbrVariables = nbrVariables;
		this.inputOp = inputOp;
		this.outputOp = outputOp;

		// Initialize the precedence of operators matrix
		precedenceOp = new int[nbrOperators][nbrOperators];
	}

	/**
	 * Evaluate the precedence of the operators given the output and input
	 * matrixes
	 */

	public void evaluate() {
		for (int i = 0; i < nbrOperators; i++) {
			for (int j = 0; j < nbrOperators; j++) {
				for (int k = 0; k < nbrVariables; k++) {
					if (outputOp[i][k] != 0 && inputOp[j][k] != 0) {
						precedenceOp[i][j] = 1;
					}
				}
			}
		}
	}

	public Boolean transitiveClosure() {
		Boolean flag = true;
		while (flag) {
			flag = false;
			for (int i = 0; i < nbrOperators; i++) {
				for (int j = 0; j < nbrOperators; j++) {
					if (precedenceOp[i][j] == 0) {
						for (int k = 0; k < nbrVariables; k++) {
							if (precedenceOp[i][k] == 1
									&& precedenceOp[k][j] == 1) {
								precedenceOp[i][j] = 1;
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
