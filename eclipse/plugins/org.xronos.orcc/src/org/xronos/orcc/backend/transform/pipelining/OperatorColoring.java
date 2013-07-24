/*
 * Copyright (c) 2011, Ecole Polytechnique Fédérale de Lausanne
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

/**
 * 
 * @author Endri Bezati
 * 
 */
public class OperatorColoring {

	private int[] asapLevels;

	private int[] alapLevels;

	private int[] opFreedom;

	private int[] order;

	private int[] firstOrder;

	private int[] opColor;

	private int[] maxColor;

	private int[] bestColor;

	private int[] worstColor;

	private int[] firstBestColor;

	private int[] stageRegisters;

	private int[] stageRegistersWidth;

	private int[] transmRegisters;

	private int[] transmRegistersWidth;

	private int[] outputPorts;

	private int minRegisterWidth = 100000;

	private int maxRegisterWidth = 0;

	private int asapRegisterWidth = 0;

	private int alapRegisterWidth = 0;

	private int nbrOperators;

	private int nbrVariables;

	private int maxColors = 0;

	private int L = 0;

	private int coloringCount = 0;

	private int[] inputs;

	private int[] outputs;

	private int[] transmission;

	private int[][] inVarLists;

	private int[] inVarCounts;

	private int[][] outVarLists;

	private int[] outVarCounts;

	public OperatorColoring(int nbrOperators, int nbrVariables) {
		this.nbrOperators = nbrOperators;
		this.nbrVariables = nbrVariables;

		opColor = new int[nbrOperators];
		bestColor = new int[nbrOperators];
		worstColor = new int[nbrOperators];
		firstBestColor = new int[nbrOperators];
		maxColor = new int[nbrOperators];
		stageRegisters = new int[nbrOperators];
		stageRegistersWidth = new int[nbrOperators];
		transmRegisters = new int[nbrOperators];
		transmRegistersWidth = new int[nbrOperators];
		asapLevels = new int[nbrOperators];
		alapLevels = new int[nbrOperators];
		opFreedom = new int[nbrOperators];
		order = new int[nbrOperators];
		firstOrder = new int[nbrOperators];
		inputs = new int[nbrVariables];
		outputs = new int[nbrVariables];
		outputPorts = new int[nbrVariables];
		transmission = new int[nbrVariables];
		outputPorts = new int[nbrVariables];
		inVarLists = new int[nbrOperators][nbrVariables];
		inVarCounts = new int[nbrOperators];
		outVarLists = new int[nbrOperators][nbrVariables];
		outVarCounts = new int[nbrOperators];
	}

	public boolean coloringAllSolutions(OperatorsIO opIO,
			OperatorConflicts opCon, OperatorPrecedence opPre) {
		// To order the operators - the first operators have to constitute the
		// longest stage path
		int i;
		int j;
		int k;
		int r;
		int c;
		int regW;
		int minC0;
		int maxC0;
		int minC1;
		int maxC1;
		int opc;
		int lasap;
		int lalap;
		int top = 0;
		opColor[top] = asapLevels[order[0]] - 1;
		maxColor[top] = alapLevels[order[0]];
		for (;;) {
			if (top < 0) {
				break;
			}
			if (top >= nbrOperators) {
				coloringCount++;

				regW = 0;
				for (r = 0; r < nbrVariables; r++) {
					transmission[r] = outputPorts[r];
				}
				for (i = maxColors; i > 1; i--) {
					for (r = 0; r < nbrVariables; r++) {
						inputs[r] = 0;
						outputs[r] = 0;
					}
					for (j = 0; j < nbrOperators; j++) {
						if (i == opColor[firstOrder[j]]) {
							for (k = 0; k < inVarCounts[j]; k++) {
								inputs[inVarLists[j][k]] = 1;
							}
							for (k = 0; k < outVarCounts[j]; k++) {
								outputs[outVarLists[j][k]] = 1;
							}
						}
					}
					for (k = 0; k < nbrVariables; k++) {
						if ((transmission[k] == 1 || inputs[k] == 1)
								&& outputs[k] == 0) {
							regW += opIO.getVariableWidth(k);
							transmission[k] = 1;
						} else {
							transmission[k] = 0;
						}
					}
					// System.out.printf("1: regW = %d\n",regW);
				}

				if (maxRegisterWidth < regW) {
					maxRegisterWidth = regW;
					for (i = 0; i < nbrOperators; i++) {
						worstColor[i] = opColor[i];
					}
				}
				if (minRegisterWidth > regW) {
					minRegisterWidth = regW;
					for (i = 0; i < nbrOperators; i++) {
						bestColor[i] = opColor[i];
					}
				}
				// if(ColoringCount < 11) print();
				// if(ColoringCount > 1000000) break; // debug

				top--;
			} else {
				k = order[top];
				c = opColor[top] + 1;
				if (c > maxColor[top]) {
					top--;
				} else {
					opColor[top] = c;
					top++;
					if (top < nbrOperators) {
						k = order[top];

						lasap = asapLevels[k];
						lalap = alapLevels[k];

						minC0 = 0;
						maxC0 = nbrOperators;
						minC1 = 0;
						maxC1 = nbrOperators;
						// if(top==41) {
						// System.out.printf("before \n");
						// }
						for (i = 0; i < top; i++) {
							opc = opColor[i];
							j = order[i];

							if (opCon.getOperatorConflict(j, k) == 1) {
								if (minC0 < opc) {
									minC0 = opc;
								}
							} else if (opPre.getPrecedenceOp(j, k) == 1) {
								if (minC1 < opc) {
									minC1 = opc;
								}
							}
							if (opCon.getOperatorConflict(k, j) == 1) {
								if (maxC0 > opc) {
									maxC0 = opc;
								}
							} else if (opPre.getPrecedenceOp(k, j) == 1) {
								if (maxC1 > opc) {
									maxC1 = opc;
								}
							}
						}
						// if(top==41) {
						// System.out.printf("after \n");
						// }
						minC0++;
						maxC0--;
						if (minC0 < minC1) {
							minC0 = minC1;
						}
						if (maxC0 > maxC1) {
							maxC0 = maxC1;
						}
						if (minC0 < lasap) {
							minC0 = lasap;
						}
						if (maxC0 > lalap) {
							maxC0 = lalap;
						}
						if (maxC0 < minC0) {
							top--;
						} else {
							opColor[top] = minC0 - 1;
							maxColor[top] = maxC0;
						}
					}
				}
			}
		}
		return true;
	}

	private void coloringStep(int top, OperatorConflicts opCon) {
		if (top > nbrOperators) {
			coloringCount++;
		} else {
			int k = order[top];
			int late = alapLevels[k];
			int minColor = minColor(top, k, opCon);
			for (int c = minColor; c <= late; c++) {
				opColor[top] = c;
				coloringStep(top + 1, opCon);
			}
		}
	}

	public void construct(OperatorsIO opIO, OperatorPrecedence opPre,
			OperatorConflicts opCon) {
		generateInputOutputLists(opIO);
		fillOutputPorts(opIO);

		levelsASAP(opCon);
		asapRegisterWidth = registerWidth(asapLevels, opIO);

		levelsALAP(opCon);
		alapRegisterWidth = registerWidth(alapLevels, opIO);

		freedomOrdering();

		coloringAllSolutions(opIO, opCon, opPre);

		System.out.println("PIPELINING: Total colors: " + coloringCount);

		estimateRegisters(bestColor, opIO, "!BEST!");
		estimateRegisters(worstColor, opIO, "!WORST!");

		float diff = maxRegisterWidth;

		System.out.println("PIPELINING: Total Min Registers Width: "
				+ minRegisterWidth);

		System.out.println("PIPELINING: Total ASAP Registers width : "
				+ asapRegisterWidth);

		System.out.println("PIPELINING: Total ALAP Registers width : "
				+ alapRegisterWidth);

		System.out
				.println("PIPELINING: Total MAX Registers width : "
						+ maxRegisterWidth + " (" + diff / minRegisterWidth
						+ " times)");

	}

	public void estimateRegisters(int[] colors, OperatorsIO opIO, String title) {
		int reg = 0;
		int regWidth = 0;

		System.out.print("\nPIPELINING: " + title);

		for (int r = 0; r < nbrVariables; r++) {
			transmission[r] = outputPorts[r];
		}

		for (int i = maxColors; i > 1; i--) {
			// Initialize the input and output array
			for (int r = 0; r < nbrVariables; r++) {
				inputs[r] = 0;
				outputs[r] = 0;
			}
			for (int j = 0; j < nbrOperators; j++) {
				if (i == colors[firstOrder[j]]) {
					for (int k = 0; k < nbrVariables; k++) {
						if (opIO.getInputOp(j, k) == 1) {
							inputs[k] = 1;
						}
						if (opIO.getOutputOp(j, k) == 1) {
							outputs[k] = 1;
						}
					}
				}
			}
			System.out.println("\nPIPELINING: stage: " + i);
			System.out.println("\nInputs");
			for (int w = 0; w < nbrVariables; w++) {
				if (inputs[w] == 1) {
					System.out.print(opIO.getVariableName(w) + " ");
				}
			}

			System.out.println("\nOutputs:");
			for (int w = 0; w < nbrVariables; w++) {
				if (outputs[w] == 1) {
					System.out.print(opIO.getVariableName(w) + " ");
				}
			}

			System.out.println("\nTransmissions");
			for (int w = 0; w < nbrVariables; w++) {
				if (transmission[w] == 1) {
					System.out.print(opIO.getVariableName(w) + " ");
				}
			}

			stageRegisters[i - 1] = 0;
			stageRegistersWidth[i - 1] = 0;
			transmRegisters[i - 1] = 0;
			transmRegistersWidth[i - 1] = 0;

			for (int k = 0; k < nbrVariables; k++) {
				if (outputs[k] == 0) {
					if (inputs[k] == 1) {
						stageRegisters[i - 1]++;
						stageRegistersWidth[i - 1] += opIO.getVariableWidth(k);
						System.out.println(opIO.getVariableName(k) + " ");
					}
				} else {
					if (transmission[k] == 1) {
						transmRegisters[i - 1]++;
						transmRegistersWidth[i - 1] += opIO.getVariableWidth(k);
						System.out.println(opIO.getVariableName(k) + "# ");
					}
				}
				if ((transmission[k] == 1 || inputs[k] == 1) && outputs[k] == 0) {
					transmission[k] = 1;
				} else {
					transmission[k] = 0;
				}
			}

			reg += stageRegisters[i - 1] + transmRegisters[i - 1];
			regWidth += stageRegisters[i - 1] + transmRegistersWidth[i - 1];
			System.out.println("PIPELINING: reg = " + stageRegisters[i - 1]
					+ " transmission = " + transmRegisters[i - 1]);

		}
		System.out.println("PIPELINING: total registers = " + reg);
		System.out.println("PIPELINING: total registers width = " + regWidth);
	}

	private void fillOutputPorts(OperatorsIO operIo) {
		for (int r = 0; r < nbrVariables; r++) {
			int flag = 1;
			for (int i = 0; i < nbrOperators; i++) {
				if (operIo.getInputOp(i, r) == 1) {
					flag = 0;
				}
				outputPorts[r] = 0;
			}
			outputPorts[r] = flag;
		}
	}

	private void freedomOrdering() {
		for (int i = 0; i < nbrOperators; i++) {
			opFreedom[i] = asapLevels[i] - alapLevels[i];
			order[i] = i;
		}

		for (int i = 0; i < nbrOperators - 1; i++) {
			int min = maxColors;
			int k = 0;
			for (int j = i; j < nbrOperators; j++) {
				if (min > opFreedom[j]) {
					min = opFreedom[j];
					k = j;
				}
			}
			int t = opFreedom[k];
			opFreedom[k] = opFreedom[i];
			opFreedom[i] = t;
			t = order[k];
			order[k] = order[i];
			order[i] = t;
		}

		for (int i = 0; i < nbrOperators; i++) { // BUG
			firstOrder[order[i]] = i;
		}
	}

	private void generateInputOutputLists(OperatorsIO operIo) {
		for (int i = 0; i < nbrOperators; i++) {
			for (int j = 0; j < nbrVariables; j++) {
				if (operIo.getInputOp(i, j) == 1) {
					inVarLists[i][inVarCounts[i]] = j;
					inVarCounts[i]++;
				}

				if (operIo.getOutputOp(i, j) == 1) {
					outVarLists[i][outVarCounts[i]] = j;
					outVarCounts[i]++;
				}
			}
		}
	}

	private void levelsALAP(OperatorConflicts opConflicts) {
		L = maxColors;
		boolean next = true;
		while (next) {
			next = false;
			for (int i = 0; i < nbrOperators; i++) {
				if (alapLevels[i] == 0) {
					boolean flag = true;
					for (int j = 0; j < nbrOperators; j++) {
						if (opConflicts.getOperatorConflict(i, j) == 1) {
							if (alapLevels[j] == 0 || alapLevels[j] == L) {
								flag = false;
								break;
							}
						}
					}
					if (flag) {
						alapLevels[i] = L;
						next = true;
					}
				}
			}
			L--;
		}
	}

	private void levelsASAP(OperatorConflicts opConflicts) {
		L = 1;
		boolean next = true;
		while (next) {
			next = false;
			for (int i = 0; i < nbrOperators; i++) {
				if (asapLevels[i] == 0) {
					boolean flag = true;
					for (int j = 0; j < nbrOperators; j++) {
						if (opConflicts.getOperatorConflict(j, i) == 1) {
							if (asapLevels[j] == 0 || asapLevels[j] == L) {
								flag = false;
								break;
							}
						}
					}
					if (flag) {
						asapLevels[i] = L;
						next = true;
					}
				}
			}
			L++;
		}
		maxColors = L - 2;
	}

	public int minColor(int top, int k, OperatorConflicts opCon) {
		int minC = 0;
		for (int i = 0; i < top; i++) {
			int j = order[i];
			if (opCon.getOperatorConflict(j, k) == 1) {
				if (minC < opColor[i]) {
					minC = opColor[i];
				}
			}
		}
		minC++;
		if (minC < asapLevels[k]) {
			minC = asapLevels[k];
		}
		return minC;
	}

	private int registerWidth(int[] colors, OperatorsIO opIO) {
		int regWidth = 0;

		for (int r = 0; r < nbrVariables; r++) {
			transmission[r] = outputPorts[r];
		}

		for (int i = maxColors; i > 1; i--) {
			for (int r = 0; r < nbrVariables; r++) {
				inputs[r] = 0;
				outputs[r] = 0;
			}
			for (int j = 0; j < nbrOperators; j++) {
				if (i == colors[j]) {
					for (int k = 0; k < nbrVariables; k++) {
						if (opIO.getInputOp(j, k) == 1) {
							inputs[k] = 1;
						}
						if (opIO.getOutputOp(j, k) == 1) {
							outputs[k] = 1;
						}
					}
				}
			}

			for (int k = 0; k < nbrVariables; k++) {
				if ((transmission[k] == 1 || inputs[k] == 1) && outputs[k] == 0) {
					regWidth += opIO.getVariableWidth(k);
					transmission[k] = 1;
				} else {
					transmission[k] = 0;
				}
			}

		}
		return regWidth;
	}
}
