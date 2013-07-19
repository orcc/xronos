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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sf.orcc.backends.ir.InstCast;
import net.sf.orcc.ir.Expression;
import net.sf.orcc.ir.InstAssign;
import net.sf.orcc.ir.InstLoad;
import net.sf.orcc.ir.InstStore;
import net.sf.orcc.ir.Instruction;
import net.sf.orcc.ir.OpBinary;

/**
 * An Operator enumeration that extends the Orcc's one, used only for pipelining
 * 
 * @author Endri Bezati
 * 
 */
public enum PipelineOperator {
	/**
	 * Assign =
	 */
	ASSIGN(0, "ASSIGN"),

	/**
	 * Bitand &
	 */
	BITAND(1, "BITAND"),

	/**
	 * Bitor |
	 */
	BITOR(2, "BITOR"),

	/**
	 * Bitxor ^
	 */
	BITXOR(3, "BITXOR"),

	/**
	 * Cast (cast)
	 */
	CAST(4, "CAST"),

	/**
	 * Div /
	 */
	DIV(5, "DIV"),

	/**
	 * EQ ==
	 */
	EQ(6, "EQ"),

	/**
	 * Greater equal >=
	 */
	GE(7, "GE"),

	/**
	 * Greater than >
	 */
	GT(8, "GT"),

	/**
	 * Less Then equal <=
	 */
	LE(9, "LE"),

	/**
	 * Logic AND &
	 */
	LOGIC_AND(10, "LOGIC_AND"),

	/**
	 * Logic OR |
	 */
	LOGIC_OR(11, "LOGIC_OR"),

	/**
	 * Less Then equal <=
	 */
	LT(12, "LT"),

	/**
	 * Minus -
	 */
	MINUS(13, "MINUS"),

	/**
	 * Modulo %
	 */
	MOD(14, "MOD"),

	/**
	 * Not equal !=
	 */
	NE(15, "NE"),

	/**
	 * Plus +
	 */
	PLUS(16, "PLUS"),

	/**
	 * Shift left <<
	 */
	SHIFT_LEFT(17, "SHIFT_LEFT"),

	/**
	 * Shift right <<
	 */
	SHIFT_RIGHT(18, "SHIFT_RIGHT"),

	/**
	 * State load
	 */
	STATE_LOAD(19, "STATE_LOAD"),

	/**
	 * State store
	 */
	STATE_STORE(20, "STATE_STORE"),

	/**
	 * TIMES *
	 */
	TIMES(21, "TIMES");

	/**
	 * Map of the Weight of the operators
	 */
	private static Map<PipelineOperator, List<Float>> WEIGHTS;

	static {
		Map<PipelineOperator, List<Float>> weights = new HashMap<PipelineOperator, List<Float>>();
		weights.put(PipelineOperator.ASSIGN, Arrays.asList(0.0f, 0.0f));
		weights.put(PipelineOperator.BITAND, Arrays.asList(0.15f, 0.5f));
		weights.put(PipelineOperator.BITOR, Arrays.asList(0.15f, 0.5f));
		weights.put(PipelineOperator.CAST, Arrays.asList(0.0f, 0.0f));
		weights.put(PipelineOperator.DIV, Arrays.asList(2.0f, 10.0f));
		weights.put(PipelineOperator.EQ, Arrays.asList(0.1f, 0.2f));
		weights.put(PipelineOperator.GE, Arrays.asList(0.1f, 0.2f));
		weights.put(PipelineOperator.GT, Arrays.asList(0.1f, 0.2f));
		weights.put(PipelineOperator.LE, Arrays.asList(0.1f, 0.2f));
		weights.put(PipelineOperator.LT, Arrays.asList(0.1f, 0.2f));
		weights.put(PipelineOperator.LOGIC_AND, Arrays.asList(0.5f, 0.2f));
		weights.put(PipelineOperator.LOGIC_OR, Arrays.asList(0.5f, 0.2f));
		weights.put(PipelineOperator.MINUS, Arrays.asList(1.0f, 2.0f));
		weights.put(PipelineOperator.MOD, Arrays.asList(2.0f, 10.0f));
		weights.put(PipelineOperator.NE, Arrays.asList(0.1f, 0.2f));
		weights.put(PipelineOperator.PLUS, Arrays.asList(1.0f, 1.0f));
		weights.put(PipelineOperator.SHIFT_LEFT, Arrays.asList(0.1f, 0.2f));
		weights.put(PipelineOperator.SHIFT_RIGHT, Arrays.asList(0.1f, 0.2f));
		weights.put(PipelineOperator.STATE_LOAD, Arrays.asList(5.0f, 5.0f));
		weights.put(PipelineOperator.STATE_STORE, Arrays.asList(5.0f, 5.0f));
		weights.put(PipelineOperator.TIMES, Arrays.asList(1.0f, 5.0f));

		WEIGHTS = Collections.unmodifiableMap(weights);
	}

	/**
	 * Get the operator from an Orcc OpBinary or from an Orcc Instruction
	 * 
	 * @param obj
	 * @return
	 */
	public static PipelineOperator getPipelineOperator(Object obj) {
		if (obj instanceof OpBinary) {
			OpBinary op = (OpBinary) obj;
			switch (op) {
			case BITAND:
				return PipelineOperator.BITAND;
			case BITOR:
				return PipelineOperator.BITOR;
			case BITXOR:
				return PipelineOperator.BITXOR;
			case DIV:
				return PipelineOperator.DIV;
			case EQ:
				return PipelineOperator.EQ;
			case GE:
				return PipelineOperator.GE;
			case GT:
				return PipelineOperator.GT;
			case LE:
				return PipelineOperator.LE;
			case LOGIC_AND:
				return PipelineOperator.LOGIC_AND;
			case LOGIC_OR:
				return PipelineOperator.LOGIC_OR;
			case LT:
				return PipelineOperator.LT;
			case MINUS:
				return PipelineOperator.MINUS;
			case MOD:
				return PipelineOperator.MOD;
			case NE:
				return PipelineOperator.NE;
			case PLUS:
				return PipelineOperator.PLUS;
			case SHIFT_LEFT:
				return PipelineOperator.SHIFT_LEFT;
			case SHIFT_RIGHT:
				return PipelineOperator.SHIFT_RIGHT;
			case TIMES:
				return PipelineOperator.TIMES;
			default:
				return null;
			}
		} else {
			if (obj instanceof Instruction) {
				if (obj instanceof InstCast) {

				} else if (obj instanceof InstAssign) {
					InstAssign assign = (InstAssign) obj;
					Expression value = assign.getValue();
					if (value.isExprInt() || value.isExprVar()) {
						return PipelineOperator.ASSIGN;
					} else {
						return null;
					}
				} else if (obj instanceof InstLoad) {
					return PipelineOperator.STATE_LOAD;
				} else if (obj instanceof InstStore) {
					return PipelineOperator.STATE_STORE;
				}
			}
		}
		return null;
	}

	@SuppressWarnings("unused")
	private int code;

	private String label;

	private PipelineOperator(int code, String label) {
		this.code = code;
		this.label = label;
	}

	/**
	 * Get the Resource weight of an operator
	 * 
	 * @param op
	 * @return
	 */
	public float getResourceWeight(PipelineOperator op) {
		return WEIGHTS.get(op).get(1);
	}

	/**
	 * Get the Time weight of an operator
	 * 
	 * @param op
	 * @return
	 */
	public float getTimeWeight(PipelineOperator op) {
		return WEIGHTS.get(op).get(0);
	}

	@Override
	public String toString() {
		return label;
	}

}
