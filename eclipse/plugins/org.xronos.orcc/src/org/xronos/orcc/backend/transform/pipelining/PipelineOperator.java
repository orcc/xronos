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
		weights.put(PipelineOperator.STATE_LOAD, Arrays.asList(0.0f, 0.0f));
		weights.put(PipelineOperator.STATE_STORE, Arrays.asList(0.0f, 0.0f));
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

	public OpBinary getOrccOperator() {
		switch (this) {
		case BITAND:
			return OpBinary.BITAND;
		case BITOR:
			return OpBinary.BITOR;
		case BITXOR:
			return OpBinary.BITXOR;
		case DIV:
			return OpBinary.DIV;
		case EQ:
			return OpBinary.EQ;
		case GE:
			return OpBinary.GE;
		case GT:
			return OpBinary.GT;
		case LE:
			return OpBinary.LE;
		case LOGIC_AND:
			return OpBinary.LOGIC_AND;
		case LOGIC_OR:
			return OpBinary.LOGIC_OR;
		case LT:
			return OpBinary.LT;
		case MINUS:
			return OpBinary.MINUS;
		case MOD:
			return OpBinary.MOD;
		case NE:
			return OpBinary.NE;
		case PLUS:
			return OpBinary.PLUS;
		case SHIFT_LEFT:
			return OpBinary.SHIFT_LEFT;
		case SHIFT_RIGHT:
			return OpBinary.SHIFT_RIGHT;
		case TIMES:
			return OpBinary.TIMES;
		default:
			return null;
		}
	}

	/**
	 * Get the Resource weight of an operator
	 * 
	 * @param op
	 * @return
	 */
	public float getResourceWeight() {
		return WEIGHTS.get(this).get(1);
	}

	/**
	 * Get the Time weight of an operator
	 * 
	 * @param op
	 * @return
	 */
	public float getTimeWeight() {
		return WEIGHTS.get(this).get(0);
	}

	@Override
	public String toString() {
		return label;
	}

}
