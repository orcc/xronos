/*******************************************************************************
 * Copyright 2002-2009  Xilinx Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
/*
 * 
 *
 * 
 */

package net.sf.openforge.lim.op;

import java.util.Collections;
import java.util.Map;

import net.sf.openforge.app.EngineThread;
import net.sf.openforge.lim.Bit;
import net.sf.openforge.lim.Bus;
import net.sf.openforge.lim.Emulatable;
import net.sf.openforge.lim.Port;
import net.sf.openforge.lim.Value;
import net.sf.openforge.lim.Visitor;
import net.sf.openforge.util.SizedInteger;

/**
 * A unary type-casting operation.
 * 
 * Created: Thu Mar 08 16:39:34 2002
 * 
 * @author Conor Wu
 * @version $Id: CastOp.java 226 2006-07-14 18:21:19Z imiller $
 */
public class CastOp extends UnaryOp implements Emulatable {
	private int castSize;
	private boolean castSigned;

	/**
	 * Creates a new Cast operation with the given size and signedness of the
	 * type being casted to.
	 * 
	 * @param size
	 *            the size being casted TO.
	 * @param signed
	 *            true if the type being casted to is signed.
	 */
	public CastOp(int size, boolean signed) {
		super();
		castSize = size;
		castSigned = signed;
	}

	/**
	 * Accept method for the Visitor interface
	 */
	@Override
	public void accept(Visitor visitor) {
		visitor.visit(this);
	}

	/**
	 * Used for cloning
	 * 
	 * @param size
	 *            a value of type 'int'
	 */
	private void setSize(int size) {
		castSize = size;
	}

	/**
	 * Used for cloning
	 * 
	 * @param isSigned
	 *            a value of type 'boolean'
	 */
	private void setSigned(boolean isSigned) {
		castSigned = isSigned;
	}

	public int getCastSize() {
		return castSize;
	}

	public boolean isCastSigned() {
		return castSigned;
	}

	/**
	 * Performs a high level numerical emulation of this component.
	 * 
	 * @param portValues
	 *            a map of owner {@link Port} to {@link SizedInteger} input
	 *            value
	 * @return a map of {@link Bus} to {@link SizedInteger} result value
	 */
	@Override
	public Map<Bus, SizedInteger> emulate(Map<Port, SizedInteger> portValues) {
		final SizedInteger inVal = portValues.get(getDataPort());
		final Value resultValue = getResultBus().getValue();
		final SizedInteger outVal = SizedInteger.valueOf(inVal.numberValue(),
				resultValue.getSize(), resultValue.isSigned());
		return Collections.singletonMap(getResultBus(), outVal);
	}

	/*
	 * =================================================== Begin new constant
	 * prop rules implementation.
	 */

	/**
	 * The value on the data input is propagated to the output and either sign
	 * extended or truncated to the result size.
	 */
	@Override
	public boolean pushValuesForward() {
		boolean mod = false;

		Value inValue = getDataPort().getValue();
		Value newValue = new Value(getCastSize(), isCastSigned());

		for (int i = 0; i < newValue.getSize(); i++) {
			if (i < getDataPort().getSize()) {
				Bit incoming = getDataPort().getValue().getBit(i);
				newValue.setBit(i, incoming);
			} else {
				if (inValue.isSigned()) {
					Bit signBit = getDataPort().getValue().getBit(
							getDataPort().getSize() - 1);
					newValue.setBit(i, signBit);
				} else {
					newValue.setBit(i, Bit.ZERO);
				}
			}
		}

		mod |= getResultBus().pushValueForward(newValue);
		return mod;
	}

	/**
	 * The value on the result bus is propagated to the input port, truncated to
	 * the input size if needed.
	 */
	@Override
	public boolean pushValuesBackward() {
		boolean mod = false;

		// Value resultBusValue = getResultBus().getValue();

		Value newPushBackValue = new Value(getDataPort().getSize(),
				getDataPort().getValue().isSigned());

		// This loop handles all bits except the MSB of the data
		// port. The MSB is handled below. While 'i' is within the
		// overlapping range we back propagate care/dont care
		// information. If the port is outside the range of the
		// result then we back propagate dont care (truncation).
		for (int i = 0; i < newPushBackValue.getSize() - 1; i++) {
			if (i < getResultBus().getValue().getSize()) {
				Bit bit = getResultBus().getValue().getBit(i);
				if (!bit.isCare()) {
					newPushBackValue.setBit(i, Bit.DONT_CARE);
				}
			} else {
				newPushBackValue.setBit(i, Bit.DONT_CARE);
			}
		}

		// For the MSB of the data port we need special handling. If
		// the data port is larger than the result bus we can simply
		// set the MSB as dont care (it is a truncation). However, if
		// the data port is smaller than the result bus we may need to
		// sign extend the MSB (if this is a signed cast). In that
		// case, we can only mark the MSB as dont care if ALL the more
		// significant bits of the result bus are dont care.
		// Two cases of port vs result size:
		// | a | b | c | d | e | f | Port
		// | g | h | i | Result
		//
		// | j | k | l | Port
		// | m | n | o | p | q | r | Result
		//
		// In the first case (port > result) we simply back propagate
		// by position (i->f, h->e, and g->d). a, b, and c are all
		// dont care bits (truncation)
		// In the second case (port > result) it matters whether this
		// is a signed cast or not. If it is a signed propagation, we
		// just do the positional back propagation (r->l, q->k, and
		// p->j). If it is unsigned, then the state for 'j' depends
		// on the states of m, n, o and p. If {mnop} are all dont
		// care then we will back propagate dont care, otherwise we
		// back propagate care. {mnop}->j
		//

		int portSize = newPushBackValue.getSize();
		int resultSize = getResultBus().getValue().getSize();
		if (portSize > resultSize || !isCastSigned()) {
			// Simple back propagation of the corresponding bit
			// happened above. The MSB (bit 'a' in example) is simply
			// a dont care if portsize>resultsize, or the
			// corresponding bit if portsize <= resultsize (and we are
			// unsigned)
			Bit msbBit = Bit.DONT_CARE;
			if (portSize <= resultSize) {
				msbBit = getResultBus().getValue().getBit(portSize - 1)
						.isCare() ? Bit.CARE : Bit.DONT_CARE;
			}
			newPushBackValue.setBit(portSize - 1, msbBit);
		} else // if (portSize <= resultSize)
		{
			// The loop starts at portSize-1 and thus handles the portSize ==
			// resultSize case
			Bit portMSB = Bit.DONT_CARE;
			for (int i = portSize - 1; i < resultSize; i++) {
				Bit bit = getResultBus().getValue().getBit(i);
				if (bit.isCare()) {
					portMSB = Bit.CARE;
					break;
				}
			}
			newPushBackValue.setBit(portSize - 1, portMSB);
		}

		mod |= getDataPort().pushValueBackward(newPushBackValue);

		return mod;
	}

	/*
	 * End new constant prop rules implementation.
	 * =================================================
	 */

	/**
	 * Clones this CastOp and correctly set's the 'resultBus'
	 * 
	 * @return a UnaryOp clone of this operations.
	 * @exception CloneNotSupportedException
	 *                if an error occurs
	 */
	@Override
	public Object clone() throws CloneNotSupportedException {
		CastOp clone = (CastOp) super.clone();
		clone.setSize(castSize);
		clone.setSigned(castSigned);
		return clone;
	}

	private static class FloatCastOp extends CastOp {
		FloatCastOp(int size, boolean signed) {
			super(size, signed);
		}

		@Override
		public boolean isFloat() {
			return true;
		}
	}

	private static final class CastF2B extends FloatCastOp {
		CastF2B(int size, boolean signed) {
			super(size, signed);
		}
	}

	private static final class CastF2C extends FloatCastOp {
		CastF2C(int size, boolean signed) {
			super(size, signed);
		}
	}

	private static final class CastF2S extends FloatCastOp {
		CastF2S(int size, boolean signed) {
			super(size, signed);
		}
	}

	private static final class CastF2I extends FloatCastOp {
		CastF2I(int size, boolean signed) {
			super(size, signed);
		}
	}

	private static final class CastF2L extends FloatCastOp {
		CastF2L(int size, boolean signed) {
			super(size, signed);
		}
	}

	private static final class CastD2B extends FloatCastOp {
		CastD2B(int size, boolean signed) {
			super(size, signed);
		}
	}

	private static final class CastD2C extends FloatCastOp {
		CastD2C(int size, boolean signed) {
			super(size, signed);
		}
	}

	private static final class CastD2S extends FloatCastOp {
		CastD2S(int size, boolean signed) {
			super(size, signed);
		}
	}

	private static final class CastD2I extends FloatCastOp {
		CastD2I(int size, boolean signed) {
			super(size, signed);
		}
	}

	private static final class CastD2L extends FloatCastOp {
		CastD2L(int size, boolean signed) {
			super(size, signed);
		}
	}

	private static final class CastD2F extends FloatCastOp {
		CastD2F(int size, boolean signed) {
			super(size, signed);
		}
	}

	private static final class CastB2F extends FloatCastOp {
		CastB2F(int size, boolean signed) {
			super(size, signed);
		}
	}

	private static final class CastC2F extends FloatCastOp {
		CastC2F(int size, boolean signed) {
			super(size, signed);
		}
	}

	private static final class CastS2F extends FloatCastOp {
		CastS2F(int size, boolean signed) {
			super(size, signed);
		}
	}

	private static final class CastI2F extends FloatCastOp {
		CastI2F(int size, boolean signed) {
			super(size, signed);
		}
	}

	private static final class CastL2F extends FloatCastOp {
		CastL2F(int size, boolean signed) {
			super(size, signed);
		}
	}

	private static final class CastB2D extends FloatCastOp {
		CastB2D(int size, boolean signed) {
			super(size, signed);
		}
	}

	private static final class CastC2D extends FloatCastOp {
		CastC2D(int size, boolean signed) {
			super(size, signed);
		}
	}

	private static final class CastS2D extends FloatCastOp {
		CastS2D(int size, boolean signed) {
			super(size, signed);
		}
	}

	private static final class CastI2D extends FloatCastOp {
		CastI2D(int size, boolean signed) {
			super(size, signed);
		}
	}

	private static final class CastL2D extends FloatCastOp {
		CastL2D(int size, boolean signed) {
			super(size, signed);
		}
	}

	private static final class CastF2D extends FloatCastOp {
		CastF2D(int size, boolean signed) {
			super(size, signed);
		}
	}

	// YES, it is possible. Try explicitly casting a float/double
	// constant to its own type.
	private static final class CastF2F extends FloatCastOp {
		CastF2F(int size, boolean signed) {
			super(size, signed);
		}
	}

	private static final class CastD2D extends FloatCastOp {
		CastD2D(int size, boolean signed) {
			super(size, signed);
		}
	}

	/**
	 * Retrieves the correct cast op for the given types. Types may be one of
	 * 'byte', 'char', 'short', 'int', 'long', 'float', or 'double'.
	 * 
	 * @param inType
	 *            a value of type 'String'
	 * @param outType
	 *            a value of type 'String'
	 * @param size
	 *            an int, the width of the cast
	 * @param signed
	 *            a boolean, true if the cast is a signed operation
	 * @return a value of type 'CastOp'
	 */
	public static CastOp getCastOp(String inType, String outType, int size,
			boolean signed) {
		final String b = "byte";
		final String c = "char";
		final String s = "short";
		final String i = "int";
		final String l = "long";
		final String f = "float";
		final String d = "double";

		if (inType.equals(f) && outType.equals(b))
			return new CastF2B(size, signed);
		else if (inType.equals(f) && outType.equals(c))
			return new CastF2C(size, signed);
		else if (inType.equals(f) && outType.equals(s))
			return new CastF2S(size, signed);
		else if (inType.equals(f) && outType.equals(i))
			return new CastF2I(size, signed);
		else if (inType.equals(f) && outType.equals(l))
			return new CastF2L(size, signed);

		else if (inType.equals(d) && outType.equals(b))
			return new CastD2B(size, signed);
		else if (inType.equals(d) && outType.equals(c))
			return new CastD2C(size, signed);
		else if (inType.equals(d) && outType.equals(s))
			return new CastD2S(size, signed);
		else if (inType.equals(d) && outType.equals(i))
			return new CastD2I(size, signed);
		else if (inType.equals(d) && outType.equals(l))
			return new CastD2L(size, signed);
		else if (inType.equals(d) && outType.equals(f))
			return new CastD2F(size, signed);

		else if (inType.equals(b) && outType.equals(f))
			return new CastB2F(size, signed);
		else if (inType.equals(c) && outType.equals(f))
			return new CastC2F(size, signed);
		else if (inType.equals(s) && outType.equals(f))
			return new CastS2F(size, signed);
		else if (inType.equals(i) && outType.equals(f))
			return new CastI2F(size, signed);
		else if (inType.equals(l) && outType.equals(f))
			return new CastL2F(size, signed);

		else if (inType.equals(b) && outType.equals(d))
			return new CastB2D(size, signed);
		else if (inType.equals(c) && outType.equals(d))
			return new CastC2D(size, signed);
		else if (inType.equals(s) && outType.equals(d))
			return new CastS2D(size, signed);
		else if (inType.equals(i) && outType.equals(d))
			return new CastI2D(size, signed);
		else if (inType.equals(l) && outType.equals(d))
			return new CastL2D(size, signed);
		else if (inType.equals(f) && outType.equals(d))
			return new CastF2D(size, signed);

		else if (inType.equals(f) && outType.equals(f))
			return new CastF2F(size, signed);
		else if (inType.equals(d) && outType.equals(d))
			return new CastD2D(size, signed);

		else {
			if (inType.equals(f) || inType.equals(d) || outType.equals(f)
					|| outType.equals(d)) {
				EngineThread.getEngine().fatalError(
						"Unknown floating point cast from " + inType + " to "
								+ outType);
			}
			return new CastOp(size, signed);
		}

	}

}
