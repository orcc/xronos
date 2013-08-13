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
import java.util.List;

/**
 * This class colors the DFG and it applies heuristics for creating pipeline
 * stages
 * 
 * @author Anatoly Prihozhy
 * 
 */
@SuppressWarnings("unused")
public class OperatorColoring {

	private class Srec {
		public int rp;
		public int stage;
		public int bound;
		public int early;
		public int late;
		public int mob;
		public int[] rank;
		public int[] dW;
	}

	private float Alfa;

	private float Beta;

	private int[] level_asap;

	private int[] level_alap;

	private int[] opfreedom;

	private int[] order;

	private int[] order1;

	private int[] opcolor;

	private int[] maxcolor;

	private int[] bestcolor;

	private int[] worstcolor;

	private int[] bestcolor1;

	private int[] stageRegisters;

	private int[] stageRegistersW;

	private int[] transmRegisters;

	private int[] transmRegistersW;

	private int[] outputPorts;

	private int[] inputPorts;

	private int minRegistersW;

	private int maxRegistersW;

	private int ASAP_registersW;

	private int ALAP_registersW;

	private int N;

	private int M;

	private int maxColor;

	private int L; // Color count

	private long ColoringCount;

	private long CutCount;

	private long cutc;

	private long ConflictCount;

	private int confc;

	private long SolutionCount;

	private int solc;

	private int[] ins;

	private int[] outs;

	private int[] transmission;

	// Operator input and output lists
	private int[] inVarLists;

	private int[] inVarCounts;

	private int[] outVarLists;

	private int[] outVarCounts;

	private int[] produce; // OUTP producers

	private int[] prod_count; // outp_count

	private int[] consume; // INP consumers

	private int[] cons_count; // inp_count

	private int[] lbrw;

	private int[] cPred;

	private int[] cP_count;

	private int[] ncPred;

	private int[] ncP_count;

	private Srec[] Stack;

	private float[] weight;

	private int[] diffio;

	private int[] specific;

	private int figcount;

	private float[] figures;

	private int[] stcount;

	BufferedWriter out;

	public OperatorColoring(TestBench tB, BufferedWriter out) {
		N = tB.N;
		M = tB.M;

		opcolor = new int[N];
		bestcolor = new int[N];
		worstcolor = new int[N];
		bestcolor1 = new int[N];
		maxcolor = new int[N];
		stageRegisters = new int[N];
		stageRegistersW = new int[N];
		transmRegisters = new int[N];
		transmRegistersW = new int[N];
		level_asap = new int[N];
		level_alap = new int[N];
		opfreedom = new int[N];
		order = new int[N];
		order1 = new int[N];
		ins = new int[M];
		outs = new int[M];
		transmission = new int[M];
		outputPorts = new int[M];
		inputPorts = new int[M];
		inVarLists = new int[N * M];
		inVarCounts = new int[N];
		outVarLists = new int[N * M];
		outVarCounts = new int[N];
		produce = new int[M * N];
		prod_count = new int[M];
		consume = new int[M * N];
		cons_count = new int[M];
		lbrw = new int[N];
		cPred = new int[N * N];
		cP_count = new int[N];
		ncPred = new int[N * N];
		ncP_count = new int[N];
		Stack = new Srec[N];
		weight = new float[N];
		diffio = new int[N];
		specific = new int[N];

		figures = new float[N * M];
		stcount = new int[N * M];

		minRegistersW = 1000000;
		maxRegistersW = 0;
		ASAP_registersW = 0;
		ALAP_registersW = 0;
		maxColor = 0;
		L = 0;
		ColoringCount = 0;
		CutCount = 0;
		ConflictCount = 0;
		SolutionCount = 0;
		solc = 0;
		cutc = 0;
		confc = 0;

		this.out = out;
	}

	private boolean branchAndBoundLSAlgorithm(OperatorConflicts Cop,
			OperatorInputs F, OperatorOutputs H, OperatorPrecedence P,
			VariableParameters Vs) {
		// To order the operators - the first operators have to constitute the
		// longest stage path
		int i;
		int j, u = 0;
		int k, k1;
		int c;
		int minC0;
		int maxC0;
		int minC1;
		int maxC1;
		int opc;
		int lasap;
		int lalap;
		int s, ord, da, dc, outv, minS, maxS, p, inv, rp;
		int max_stage, asapk, max_asap, oinv;
		int lbrwdW = 0;
		int dW, mindW;
		int top = 0;
		int stal, stas;

		Srec St0 = Stack[0];

		St0.rp = -1;
		St0.bound = regWidthInitialEstimation(F, H, Vs);
		try {
			out.write("RegWidthInitialEstimation = " + Stack[0].bound + "\n");
		} catch (IOException e) {
		}

		St0.early = 1;
		St0.late = 1;
		St0.mob = 1;
		St0.rank[0] = 1;
		St0.dW[0] = 0;

		long t0 = System.currentTimeMillis();

		while (true) {
			if (top < 0) {
				break;
			}
			if (top >= N) {
				ColoringCount++;
				minRegistersW = lbrwdW;
				// fprintf(outputfile,"ColoringCount=%d   PrunCount=%d   minRegistersW=%d\n",ColoringCount,CutCount,minRegistersW);
				for (i = 0; i < N; i++) {
					St0 = Stack[i];
					bestcolor[i] = St0.rank[St0.rp];
				}
				top--;
				// if(ColoringCount>=1) return true;
			} else {
				k = order[top];
				St0 = Stack[top];
				rp = St0.rp + 1;
				if (specific[k] > 1 && rp == 1 || rp >= St0.mob) {
					top--;
				} else {
					St0.rp = rp;
					St0.stage = St0.rank[rp];
					lbrwdW = St0.bound + St0.dW[rp];
					// if(ColoringCount==0)
					// fprintf(outputfile,"1: top=%d  k=%d  c=%d dW=%d lbrwdW=%d\n",top,k,c,dW,lbrwdW);
					if (lbrwdW >= minRegistersW) {
						CutCount++; // += Stack[top].mob - rp;
						if (CutCount == 0x7FFFFFFF) {
							cutc++;
							CutCount = 0;
							if (cutc >= 16) {
								return true;
							}
						}
						if (CutCount % 1000000 == 0) {
							long t1 = System.currentTimeMillis();
							if ((t1 - t0) / 1000 > 5) {
								return false;
							}
						}
						top--;
					} else {
						top++;
						// if(top>59)
						// k=1;
						if (top < N) {
							k = order[top];
							lasap = level_asap[k];
							lalap = level_alap[k];
							minC0 = 0;
							maxC0 = N;
							minC1 = 0;
							maxC1 = N;
							for (i = 0; i < cP_count[top]; i++) {
								p = cPred[top * N + i];
								k1 = order[p];
								opc = Stack[p].stage;
								if (k1 < k) {
									if (minC0 < opc) {
										minC0 = opc;
									}
								} else {
									if (maxC0 > opc) {
										maxC0 = opc;
									}
								}
							}
							for (i = 0; i < ncP_count[top]; i++) {
								p = ncPred[top * N + i];
								k1 = order[p];
								opc = Stack[p].stage;
								if (k1 < k) {
									if (minC1 < opc) {
										minC1 = opc;
									}
								} else {
									if (maxC1 > opc) {
										maxC1 = opc;
									}
								}
							}
							// if(ColoringCount==0)
							// fprintf(outputfile,"top=%d minC0=%d maxC0=%d minC1=%d maxC1=%d\n",top,minC0,maxC0,minC1,maxC1);
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
								ConflictCount++;
							} else {
								St0 = Stack[top];
								St0.rp = -1;
								St0.bound = lbrwdW;
								St0.early = minC0;
								St0.late = maxC0;
								St0.mob = maxC0 - minC0 + 1;
								for (i = 0; i < St0.mob; i++) {
									St0.rank[i] = minC0 + i;
									St0.dW[i] = 0;
								}
								for (j = 0; j < outVarCounts[k]; j++) {
									outv = outVarLists[k * M + j];
									stal = maxColor;
									for (u = 0; u < prod_count[outv]; u++) {
										p = produce[outv * N + u];
										if (p != k) {
											ord = order1[p];
											if (ord < top) {
												s = Stack[ord].stage;
											} else {
												s = level_alap[p];
											}
											if (stal > s) {
												stal = s;
											}
										}
									}
									if (outputPorts[outv] == 1) {
										stas = maxColor;
									} else {
										stas = -1;
										for (u = 0; u < cons_count[outv]; u++) {
											p = consume[outv * N + u];
											ord = order1[p];
											if (ord < top) {
												s = Stack[ord].stage;
											} else {
												s = level_asap[p];
											}
											if (stas < s) {
												stas = s;
											}
										}
									}
									da = level_alap[k];
									da = da < stal ? da : stal;
									da = stas - da;
									da = da > 0 ? da : 0;
									for (i = 0; i < Stack[top].mob; i++) {
										dc = minC0 + i;
										dc = dc < stal ? dc : stal;
										dc = stas - dc;
										dc = dc > 0 ? dc : 0;
										St0.dW[i] += Vs.varWidths[outv]
												* (dc - da);
									}
								}
								for (j = 0; j < inVarCounts[k]; j++) {
									inv = inVarLists[k * M + j];
									if (outputPorts[inv] == 1) {
										stas = maxColor;
									} else {
										stas = 0;
										for (u = 0; u < cons_count[inv]; u++) {
											p = consume[inv * N + u];
											if (p != k) {
												ord = order1[p];
												if (ord < top) {
													s = Stack[ord].stage;
												} else {
													s = level_asap[p];
												}
												if (stas < s) {
													stas = s;
												}
											}
										}
									}
									stal = maxColor;
									if (inputPorts[inv] == 1) {
										stal = 1;
									} else {
										for (u = 0; u < prod_count[inv]; u++) {
											p = produce[inv * N + u];
											ord = order1[p];
											if (ord < top) {
												s = Stack[ord].stage;
											} else {
												s = level_alap[p];
											}
											if (stal > s) {
												stal = s;
											}
										}
									}
									da = level_asap[k];
									da = da > stas ? da : stas;
									da = da - stal;
									da = da > 0 ? da : 0;
									for (i = 0; i < Stack[top].mob; i++) {
										dc = minC0 + i;
										dc = dc > stas ? dc : stas;
										dc = dc - stal;
										dc = dc > 0 ? dc : 0;
										St0.dW[i] += Vs.varWidths[inv]
												* (dc - da);
									}
								}
								if (inVarCounts[k] == 0) {
									j = St0.mob - 1;
									St0.rank[0] = St0.rank[j];
									St0.dW[0] = St0.dW[j];
									St0.mob = 1;
								} else {
									for (i = 0; i < St0.mob - 1; i++) {
										mindW = 100000000;
										for (j = i; j < St0.mob; j++) {
											dW = St0.dW[j];
											if (mindW > dW) {
												mindW = dW;
												u = j;
											}
										}
										if (u != i) {
											St0.dW[u] = St0.dW[i];
											St0.dW[i] = mindW;
											j = St0.rank[u];
											St0.rank[u] = St0.rank[i];
											St0.rank[i] = j;
										}
									}
								}
							}
						}
					}
				}
			}
		}
		return true;
	}

	private void c_nc_Predecessors(OperatorConflicts Cop, OperatorPrecedence P) {
		int i, j, k, r;
		for (i = 0; i < N; i++) {
			for (j = 0; j <= i; j++) {
				if (Cop.opConfl[j * N + N - 1 - i + j] == 1) {
					P.oPreced[j * N + N - 1 - i + j] = 3;
					for (k = 0; k < N; k++) {
						if (Cop.opConfl[j * N + k] == 1
								&& Cop.opConfl[k * N + N - 1 - i + j] == 1) {
							Cop.opConfl[j * N + N - 1 - i + j] = 2;
							break;
						}
					}
				}
				if (P.oPreced[j * N + N - 1 - i + j] == 1) {
					for (k = 0; k < N; k++) {
						if (P.oPreced[j * N + k] == 1
								&& P.oPreced[k * N + N - 1 - i + j] == 1) {
							P.oPreced[j * N + N - 1 - i + j] = 2;
							break;
						}
					}
				}
			}
		}

		for (i = 0; i < N; i++) {
			r = order1[i];
			for (j = 0; j <= i; j++) {
				k = order1[j];
				if (Cop.opConfl[j * N + i] == 1) {
					if (k < r) {
						cPred[r * N + cP_count[r]++] = k;
					} else {
						cPred[k * N + cP_count[k]++] = r;
					}
				} else if (Cop.opConfl[j * N + i] > 1) {
					Cop.opConfl[j * N + i] = 1;
				}
				if (P.oPreced[j * N + i] == 1) {
					if (k < r) {
						ncPred[r * N + ncP_count[r]++] = k;
					} else {
						ncPred[k * N + ncP_count[k]++] = r;
					}
				} else if (P.oPreced[j * N + i] > 1) {
					P.oPreced[j * N + i] = 1;
				}
			}
		}
		print_cPred(cPred, cP_count);
		print_ncPred(ncPred, ncP_count);

	}

	private void estimateRegisters(int[] colors, OperatorInputs F,
			OperatorOutputs H, VariableParameters Vs, String title,
			List<String> stageInputs, List<String> stageOutputs,
			List<Integer> stageOperators) {
		int reg = 0;
		int regW = 0;
		try {
			out.write("\n" + title + "\n");
			for (int r = 0; r < M; r++) {
				transmission[r] = inputPorts[r];
			}

			out.write("Input tokens: ");
			for (int r = 0; r < M; r++) {
				transmission[r] = inputPorts[r];
				if (inputPorts[r] == 1) {
					out.write(Vs.varNames[r] + " ");
				}
			}
			out.write("\n");
			for (int i = 1; i <= maxColor; i++) {
				out.write("STAGE " + i + "\n");
				for (int r = 0; r < M; r++) {
					ins[r] = 0;
					outs[r] = transmission[r];
				}
				for (int j = 0; j < N; j++) {
					if (i == colors[order1[j]]) {
						for (int k = 0; k < M; k++) {
							if (F.objIns[j * M + k] == 1) {
								ins[k] = 1;
							}
							if (H.objOuts[j * M + k] == 1) {
								outs[k] = 1;
							}
						}
					}
				}
				for (int k = 0; k < M; k++) {
					if (ins[k] == 1 && outs[k] == 0) {
						out.write("     NO VALUE - " + Vs.varNames[k] + "\n");
					}
				}
				out.write("     Operators: ");
				for (int j = 0; j < N; j++) {
					if (i == colors[order1[j]]) {
						out.write(j + " ");
					}
				}
				out.write("\n");
				for (int w = i + 1; w <= maxColor; w++) {
					for (int j = 0; j < N; j++) {
						if (w == colors[order1[j]]) {
							for (int k = 0; k < M; k++) {
								if (outs[k] == 1 || transmission[k] == 1) {
									if (F.objIns[j * M + k] == 1) {
										transmission[k] = 2;
									}
								}
							}
						}
					}
				}
				for (int k = 0; k < M; k++) {
					if (outs[k] == 1 || transmission[k] == 1) {
						if (outputPorts[k] == 1) {
							transmission[k] = 2;
						}
					}
				}
				for (int k = 0; k < M; k++) {
					if (transmission[k] == 2) {
						transmission[k] = 1;
					} else {
						if (transmission[k] == 1) {
							transmission[k] = 0;
						}
					}
				}
				if (i < maxColor) {
					out.write("     Pipeline registers: ");
				} else {
					out.write("     Output tokens: ");
				}
				for (int k = 0; k < M; k++) {
					if (transmission[k] == 1) {
						out.write(Vs.varNames[k] + " ");
						if (i < maxColor) {
							reg++;
							regW += Vs.varWidths[k];
						}
					}
				}
				out.write("\n");
			}
			out.write("total reg = " + reg + "\n");
			out.write("total regW = " + regW + "\n\n");
		} catch (IOException e) {

		}
	}

	/**
	 * Estimate the freedom order
	 * 
	 * @param Vs
	 */
	private void freedom_ordering(VariableParameters Vs) {
		// BEGIN: OPERATOR ORDERING - TO COMMENT WITHOUT ORDERING !!!!!

		int maxmob = 0;
		int maxdio = 0, din, dout, inv, outv;
		int i = 0, j, k = 0, d, m;
		float min;
		int t;
		boolean flag;
		for (i = 0; i < N; i++) {
			opfreedom[i] = level_alap[i] - level_asap[i] + 1;
			order[i] = i;
		}
		for (i = 0; i < N - 1; i++) {
			j = i;
			flag = true;
			for (; j < N; j++) {
				k = order[j];
				if (level_asap[k] == 1 && level_alap[k] == 1) {
					flag = false;
					break;
				}
			}
			if (flag) {
				break;
			}
			t = opfreedom[j];
			opfreedom[j] = opfreedom[i];
			opfreedom[i] = t;
			t = order[j];
			order[j] = order[i];
			order[i] = t;
			diffio[i] = 0;
			weight[i] = 0.0f;
		}
		for (; i < N - 1; i++) {
			j = i;
			flag = true;
			for (; j < N; j++) {
				if (opfreedom[j] == 1) {
					flag = false;
					break;
				}
			}
			if (flag) {
				break;
			}
			t = opfreedom[j];
			opfreedom[j] = opfreedom[i];
			opfreedom[i] = t;
			t = order[j];
			order[j] = order[i];
			order[i] = t;
			diffio[i] = 0;
			weight[i] = 0.0f;
		}
		for (j = i; j < N; j++) {
			m = opfreedom[j];
			if (maxmob < m) {
				maxmob = m;
			}
			t = order[j];
			din = 0;
			for (k = 0; k < inVarCounts[t]; k++) {
				inv = inVarLists[t * M + k];
				din += Vs.varWidths[inv];
			}
			dout = 0;
			for (k = 0; k < outVarCounts[t]; k++) {
				outv = outVarLists[t * M + k];
				dout += Vs.varWidths[outv];
			}
			d = din - dout;
			d = d >= 0 ? d : -d;
			diffio[j] = d;
			if (maxdio < d) {
				maxdio = d;
			}
		}
		float Alfa = 0.50f;
		float Beta = 0.50f;
		for (j = i; j < N; j++) {
			weight[j] = Alfa
					* ((float) (maxmob - opfreedom[j]) / (float) maxmob) + Beta
					* ((float) diffio[j] / (float) maxdio);
		}
		for (; i < N - 1; i++) {
			min = -1;
			k = -1;
			for (j = i; j < N; j++) {
				t = order[j];
				if (specific[t] == 0) {
					if (min < weight[j]) {
						min = weight[j];
						k = j;
					}
				}
			}
			if (k == -1) {
				break;
			}
			if (k != i) {
				min = weight[k];
				weight[k] = weight[i];
				weight[i] = min;
				t = opfreedom[k];
				opfreedom[k] = opfreedom[i];
				opfreedom[i] = t;
				t = order[k];
				order[k] = order[i];
				order[i] = t;
			}
		}
		for (j = i; j < N; j++) {
			k = order[j];
			int vi = inVarLists[k * M];
			diffio[j] = Vs.varWidths[vi];
		}
		for (; i < N - 1; i++) {
			min = -1;
			for (j = i; j < N; j++) {
				if (min < diffio[j]) {
					min = diffio[j];
					k = j;
				}
			}
			if (k != i) {
				min = diffio[k];
				diffio[k] = diffio[i];
				diffio[i] = (int) min;
				t = opfreedom[k];
				opfreedom[k] = opfreedom[i];
				opfreedom[i] = t;
				t = order[k];
				order[k] = order[i];
				order[i] = t;
			}
		}

		// END: OPERATOR ORDERING
		for (i = 0; i < N; i++) {
			order1[order[i]] = i;
		}
	}

	private void generate_F_H_lists(OperatorInputs F, OperatorOutputs H,
			OperatorParameters Op, VariableParameters Vs) {

		for (int i = 0; i < N; i++) {
			inVarCounts[i] = 0;
			outVarCounts[i] = 0;
		}

		for (int i = 0; i < N; i++) {
			for (int r = 0; r < M; r++) {
				if (F.objIns[i * M + r] == 1) {
					inVarLists[i * M + inVarCounts[i]] = r;
					inVarCounts[i]++;
				}
				if (H.objOuts[i * M + r] == 1) {
					outVarLists[i * M + outVarCounts[i]] = r;
					outVarCounts[i]++;
				}
			}
		}
		for (int i = 0; i < N; i++) {
			// if(i==67)
			// i=i;
			int ci = inVarCounts[i];
			int co = outVarCounts[i];
			if (ci == 1 && ci == co) {
				int vi = inVarLists[i * M];
				int vo = outVarLists[i * M];
				if (Vs.varWidths[vi] == Vs.varWidths[vo]) {
					float tm = Op.getTime(i);
					if (tm == 0.0f) {
						specific[i] = 2;
					} else {
						specific[i] = 1;
					}
				}
			}
		}
		print_FH_lists(Vs);
	}

	/**
	 * Get the input and output ports
	 * 
	 * @param F
	 * @param H
	 * @param Vs
	 */
	private void get_input_output_ports(OperatorInputs F, OperatorOutputs H,
			VariableParameters Vs) {
		for (int r = 0; r < M; r++) {
			outputPorts[r] = 0;
		}
		for (int r = 0; r < M; r++) {
			int flag = 1;
			for (int i = 0; i < N; i++) {
				if (F.objIns[i * M + r] == 1) {
					flag = 0;
					// outputPorts[r] = 0;
				}
			}
			outputPorts[r] = flag;
		}
		for (int r = 0; r < M; r++) {
			inputPorts[r] = 0;
		}
		for (int r = 0; r < M; r++) {
			int flag = 1;
			for (int i = 0; i < N; i++) {
				if (H.objOuts[i * M + r] == 1) {
					flag = 0;
					// inputPorts[r] = 0;
				}
			}
			inputPorts[r] = flag;
		}
		print_output_ports(Vs);
	}

	public Integer getNbrStages() {
		return maxColor;
	}

	/**
	 * Construct the ALAP levels
	 * 
	 * @param Cop
	 */

	private void levelsALAP(OperatorConflicts Cop) {
		L = maxColor;
		boolean next = true;
		while (next) {
			next = false;
			for (int i = 0; i < N; i++) {
				if (level_alap[i] == 0) {
					int flag = 1;
					for (int j = 0; j < N; j++) {
						if (Cop.opConfl[i * N + j] == 1) {
							if (level_alap[j] == 0 || level_alap[j] == L) {
								flag = 0;
								break;
							}
						}
					}
					if (flag == 1) {
						level_alap[i] = L;
						next = true;
					}
				}
			}
			L--;
		}
	}

	/**
	 * Construct the ASAP levels
	 * 
	 * @param Cop
	 */
	private void levelsASAP(OperatorConflicts Cop) {
		L = 1;
		boolean next = true;
		while (next) {
			next = false;
			for (int i = 0; i < N; i++) {
				if (level_asap[i] == 0) {
					int flag = 1;
					for (int j = 0; j < N; j++) {
						if (Cop.opConfl[j * N + i] == 1) {
							if (level_asap[j] == 0 || level_asap[j] == L) {
								flag = 0;
								break;
							}
						}
					}
					if (flag == 1) {
						level_asap[i] = L;
						next = true;
					}
				}
			}
			L++;
		}
		maxColor = L - 2;
	}

	public void optimizePipeline(OperatorConflicts Cop, OperatorInputs F,
			OperatorOutputs H, OperatorPrecedence P, OperatorParameters Op,
			VariableParameters Vs, List<String> stageInputs,
			List<String> stageOutputs, List<Integer> stageOperators) {
		generate_F_H_lists(F, H, Op, Vs);
		get_input_output_ports(F, H, Vs);
		levelsASAP(Cop);
		print_levels_asap();
		ASAP_registersW = registerWidth(level_asap, F, H, Vs);
		levelsALAP(Cop);
		print_levels_alap();
		ALAP_registersW = registerWidth(level_alap, F, H, Vs);
		freedom_ordering(Vs);
		print_freedom_order();
		for (int i = 0; i < N; i++) {
			Stack[i] = new Srec();
			Stack[i].rank = new int[maxColor];
			Stack[i].dW = new int[maxColor];
		}
		c_nc_Predecessors(Cop, P);

		boolean globalOptimum = branchAndBoundLSAlgorithm(Cop, F, H, P, Vs);
		try {

			if (globalOptimum) {
				out.write("Global Optimum\n");
			} else {
				out.write("Suboptimal solution\n");
			}

			out.write("ColoringCount = " + ColoringCount + " CutCount = "
					+ CutCount + " cutc = " + cutc + " ConflictCount = "
					+ ConflictCount + " confc = " + confc);

			print();
			estimateRegisters(bestcolor, F, H, Vs, "BEST PIPELINE:",
					stageInputs, stageOutputs, stageOperators);
			float diff = maxRegistersW;

			out.write("total minRegistersW = " + minRegistersW + "\n");

			out.write("OPT/ASAP = "
					+ ((float) ASAP_registersW / (float) minRegistersW - 1)
					* 100 + " OPT/ALAP = "
					+ ((float) ALAP_registersW / (float) minRegistersW - 1)
					* 100 + "\n");

			out.write("total ASAPregistersW = " + ASAP_registersW + "\n");
			out.write("total ALAPregistersW = " + ALAP_registersW + "\n");

			out.write("total maxRegistersW = " + maxRegistersW + " (" + diff
					/ minRegistersW + "times)\n");

			out.write("Number of stages = " + maxColor + "\n");

			out.write("SolutionCount = " + SolutionCount + " solc = " + solc
					+ "\n");

			double solutions = (double) solc * (double) 0x7fffffff
					+ SolutionCount;
			out.write("solutions = " + solutions + "(2**32)-1 = "
					+ (double) 0x7fffffff + "\n");
		} catch (IOException e) {
		}
	}

	public boolean print() {
		if (N == 0) {
			return false;
		}
		try {
			out.write("\nBest coloring:\n");
			for (int i = 0; i < N; i++) {
				bestcolor1[order[i]] = bestcolor[i];
			}
			for (int i = 0; i < N; i++) {
				int j = i + 1;
				out.write(j + ":" + bestcolor1[i] + " ");
			}
			out.write("\n");

		} catch (IOException e) {
		}
		return true;
	}

	/**
	 * Print Cop Matrix
	 * 
	 * @param out
	 * @param Cop
	 * @return
	 */
	public boolean print_Cop(BufferedWriter out, OperatorConflicts Cop) {
		if (N == 0) {
			return false;
		}
		try {
			out.write("MatrixCop-antitransitive:\n");
			for (int i = 0; i < N; i++) {
				for (int j = 0; j < N; j++) {
					out.write(Cop.opConfl[i * N + j] + " ");
				}
				out.write("\n");
			}
			out.write("\n");
		} catch (IOException e) {
		}
		return true;
	}

	/**
	 * Print cPred list
	 * 
	 * @param cPred
	 * @param cP_count
	 * @return
	 */
	public boolean print_cPred(int[] cPred, int[] cP_count) {
		try {
			out.write("ncPred:\n");
			for (int i = 0; i < N; i++) {
				out.write(i + " ");
				for (int j = 0; j < cP_count[i]; j++) {
					out.write(i + ":" + cPred[i * N + j] + " ");
				}
				out.write("\n");
			}
		} catch (IOException e) {
		}
		return true;
	}

	/**
	 * Print intput F and output H matrixes
	 * 
	 * @param Vs
	 */
	public void print_FH_lists(VariableParameters Vs) {
		try {
			out.write("List of operator inputs:\n");

			for (int i = 0; i < N; i++) {
				out.write(i + " ");
				for (int j = 0; j < inVarCounts[i]; j++) {
					out.write(Vs.varNames[inVarLists[i * M + j]] + " ");
				}
			}
			out.write("\n");
			out.write("List of operator outputs:\n");
			for (int i = 0; i < N; i++) {
				out.write(i + " ");
				for (int j = 0; j < outVarCounts[i]; j++) {
					out.write(Vs.varNames[outVarLists[i * M + j]] + " ");
				}
			}
			out.write("\n");
			out.write("Specific:\n");
			for (int i = 0; i < N; i++) {
				out.write(Vs.varNames[i + specific[i]] + " ");
			}
			out.write("\n");
		} catch (IOException e) {
		}
	}

	/**
	 * Print the figure stages
	 * 
	 */
	public void print_figures_stages() {
		try {
			out.write("stage delays - stage counts:\n");
			out.write("figcount = " + figcount + ":\n");
			for (int i = 0; i < figcount; i++) {
				out.write(figures[i] + "-" + stcount[i] + " ");
			}
			out.write("\n");
		} catch (IOException e) {
		}
	}

	/**
	 * Print freedom order
	 * 
	 */
	public void print_freedom_order() {
		try {
			out.write("Freedom: \n");
			for (int i = 0; i < N; i++) {
				out.write(i + ":" + opfreedom[i] + " ");
			}
			out.write("\n");

			out.write("In-Out-Difference: \n");
			for (int i = 0; i < N; i++) {
				out.write(i + ":" + diffio[i] + " ");
			}
			out.write("\n");

			out.write("Weight: \n");
			for (int i = 0; i < N; i++) {
				out.write(i + ":" + weight[i] + " ");
			}
			out.write("\n");

			out.write("Operator Order: \n");
			for (int i = 0; i < N; i++) {
				out.write(i + ":" + order[i] + " ");
			}
			out.write("\n");

			out.write("Operator Order1: \n");
			for (int i = 0; i < N; i++) {
				out.write(i + ":" + order1[i] + " ");
			}
			out.write("\n");

		} catch (IOException e) {
		}
	}

	/**
	 * Print alap levels
	 * 
	 */
	public void print_levels_alap() {
		try {
			out.write("Levels ALAP:\n");
			for (int i = 0; i < N; i++) {
				out.write(i + ":" + level_alap[i] + " ");
			}
			out.write("\n");
		} catch (IOException e) {
		}
	}

	/**
	 * Print asap levels
	 * 
	 */
	public void print_levels_asap() {
		try {
			out.write("\nLevels ASAP:\n");
			for (int i = 0; i < N; i++) {
				out.write(i + ":" + level_asap[i] + " ");
			}
			out.write("\n");
		} catch (IOException e) {
		}
	}

	/**
	 * Print ncPred
	 * 
	 * @param ncPred
	 * @param ncP_count
	 * @return
	 */
	public boolean print_ncPred(int[] ncPred, int[] ncP_count) {
		try {
			out.write("ncPred:\n");
			for (int i = 0; i < N; i++) {
				out.write(i + " ");
				for (int j = 0; j < ncP_count[i]; j++) {
					out.write(i + ":" + ncPred[i * N + j] + " ");
				}
				out.write("\n");
			}
		} catch (IOException e) {
		}
		return true;
	}

	/**
	 * Print input and output ports
	 * 
	 * @param Vs
	 */
	public void print_output_ports(VariableParameters Vs) {
		try {
			out.write("Input Tokens:\n");
			for (int i = 0; i < M; i++) {
				if (inputPorts[i] == 1) {
					out.write(Vs.varNames[i] + " ");
				}
			}
			out.write("\n");
			out.write("Output Tokens:\n");
			for (int i = 0; i < M; i++) {
				if (outputPorts[i] == 1) {
					out.write(Vs.varNames[i] + " ");
				}
			}
		} catch (IOException e) {
		}
	}

	/**
	 * Print the operator precedence Matrix
	 * 
	 * @param P
	 * @return
	 */
	public boolean print_P(OperatorPrecedence P) {
		if (N == 0) {
			return false;
		}
		try {
			out.write("MatrixP-antitransitive:\n");
			for (int i = 0; i < N; i++) {
				for (int j = 0; j < N; j++) {
					out.write(P.oPreced[i * N + j] + " ");
				}
				out.write("\n");
			}
			out.write("\n");
		} catch (IOException e) {
		}
		return true;
	}

	/**
	 * Print the list of variable producers and consumers
	 * 
	 * @param Vs
	 */
	public void print_produce_consume(VariableParameters Vs) {
		try {
			out.write("List of variable producers:\n");
			for (int i = 0; i < M; i++) {
				out.write(i + Vs.varNames[i] + " ");
				for (int j = 0; j < prod_count[i]; j++) {
					out.write(produce[i * N + j] + " ");
				}
				out.write("\n");
			}
			out.write("\n");
			out.write("List of variable consumers:\n");
			for (int i = 0; i < M; i++) {
				out.write(i + Vs.varNames[i] + " ");
				for (int j = 0; j < cons_count[i]; j++) {
					out.write(consume[i * N + j] + " ");
				}
				out.write("\n");
			}
			out.write("\n");
		} catch (IOException e) {
		}
	}

	/**
	 * For each color get the registers width
	 * 
	 * @param colors
	 * @param F
	 * @param H
	 * @param Vs
	 * @return
	 */
	private int registerWidth(int colors[], OperatorInputs F,
			OperatorOutputs H, VariableParameters Vs) {
		int regW = 0;
		for (int r = 0; r < M; r++) {
			transmission[r] = outputPorts[r];
		}
		for (int i = maxColor; i > 1; i--) {
			for (int r = 0; r < M; r++) {
				ins[r] = 0;
				outs[r] = 0;
			}
			for (int j = 0; j < N; j++) {
				if (i == colors[j]) {
					for (int k = 0; k < M; k++) {
						if (F.objIns[j * M + k] == 1) {
							ins[k] = 1;
						}
						if (H.objOuts[j * M + k] == 1) {
							outs[k] = 1;
						}
					}
				}
			}
			for (int k = 0; k < M; k++) {
				if ((transmission[k] == 1 || ins[k] == 1) && outs[k] == 0) {
					regW += Vs.varWidths[k];
					transmission[k] = 1;
				} else {
					transmission[k] = 0;
				}
			}
		}
		return regW;
	}

	/**
	 * Get the initial register estimation
	 * 
	 * @param F
	 * @param H
	 * @param Vs
	 * @return
	 */
	private int regWidthInitialEstimation(OperatorInputs F, OperatorOutputs H,
			VariableParameters Vs) {
		int i, j, k, sb, se;
		int InitialRegWidth = 0;
		for (j = 0; j < M; j++) {
			// outP[j] = -1;
			prod_count[j] = 0;
			cons_count[j] = 0;
			for (i = 0; i < N; i++) {
				if (H.objOuts[i * M + j] == 1) {
					produce[j * N + prod_count[j]] = i;
					prod_count[j]++;
				}
				if (F.objIns[i * M + j] == 1) {
					consume[j * N + cons_count[j]] = i;
					cons_count[j]++;
				}
			}
		}
		print_produce_consume(Vs);
		for (j = 0; j < M; j++) {
			// if(outP[j]!=-1) sb = level_alap[outP[j]]; else sb = 1;
			if (prod_count[j] == 0) {
				sb = 1;
			} else {
				sb = maxColor;
			}
			for (i = 0; i < prod_count[j]; i++) {
				k = level_alap[produce[j * N + i]];
				if (sb > k) {
					sb = k;
				}
			}
			se = -1;
			for (i = 0; i < cons_count[j]; i++) {
				k = level_asap[consume[j * N + i]];
				if (se < k) {
					se = k;
				}
			}
			if (outputPorts[j] == 1) {
				se = maxColor;// + 1;
			}
			if (sb < se) {
				InitialRegWidth += (se - sb) * Vs.varWidths[j];
			}
		}
		return InitialRegWidth;
	}

}
