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
package org.xronos.openforge.lim.op;

import java.math.BigInteger;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.xronos.openforge.lim.Bit;
import org.xronos.openforge.lim.Emulatable;
import org.xronos.openforge.lim.Value;
import org.xronos.openforge.lim.memory.AddressableUnit;


/**
 * An operation that generates a fixed constant value. All Ports and Buses are
 * marked unused, except the single data bus, whose value is constant. A simple
 * constant is created with a given value that is not affected by the endianness
 * of the compilation. When constructed from a primitive value (int or long) the
 * byte ordering is assumed to be little endian regardless of the endianess of
 * the compilation.
 * 
 * @author Stephen Edwards
 * @version $Id: SimpleConstant.java 568 2008-03-31 17:23:31Z imiller $
 */
public class SimpleConstant extends Constant implements Emulatable {

	/**
	 * The value representation. All AURep objects must be fully locked and
	 * determinate at construction.
	 */
	private AURepBundle rep;

	/**
	 * Constructs a signed int constant with specific width
	 * 
	 * @param ival
	 *            the int value of the constant
	 * @param width
	 *            the int value of a specified width (in bits)
	 * @deprecated use SimpleConstant(long lval, int width, boolean isSigned)
	 */
	@Deprecated
	public SimpleConstant(int ival, int width) {
		this((long) ival, width);
	}

	/**
	 * Constructs a signed long constant with specific width. If
	 * <code>lval</code> is negative, then the constant will produce a signed
	 * value; otherwise it will be unsigned. Use
	 * {@link SimpleConstant#SimpleConstant(long,int,boolean)} to create a
	 * signed positive constant.
	 * 
	 * @param lval
	 *            the long value of the constant
	 * @param width
	 *            the int value of a specified width (in bits)
	 * @deprecated use SimpleConstant(long lval, int width, boolean isSigned)
	 */
	@Deprecated
	public SimpleConstant(long lval, int width) {
		this(lval, width, lval < 0);
	}

	/**
	 * Constructs a long constant with specific width
	 * 
	 * @param lval
	 *            the long value of the constant
	 * @param width
	 *            the int value of a specified bit width
	 * @param isSigned
	 *            true if the constant value is signed
	 */
	public SimpleConstant(long lval, int width, boolean isSigned) {
		this(BigInteger.valueOf(lval), width, isSigned);
	}

	/**
	 * Constructs a simple constant based on the specified number of bits from
	 * the 'bigInt' value.
	 * 
	 * @param bigInt
	 *            , a non-null BigInteger which contains the numerci value of
	 *            the constant being created.
	 * @param width
	 *            , the bit width of this constant
	 * @param isSigned
	 *            , true if this is a signed value
	 */
	public SimpleConstant(BigInteger bigInt, int width, boolean isSigned) {
		super(width, isSigned);

		final int byteWidth = (int) Math.ceil(width / 8.0);
		if (byteWidth <= 0)
			throw new IllegalArgumentException("Illegal width for constant "
					+ width);

		final BigInteger mask = new BigInteger("FF", 16);

		AddressableUnit[] aurep = new AddressableUnit[byteWidth];
		for (int i = 0; i < byteWidth; i++) {
			final BigInteger byteValue = bigInt.shiftRight(8 * i).and(mask);
			aurep[i] = new AddressableUnit((int) byteValue.longValue(), true);
		}
		rep = new AURepBundle(aurep, 8);

		setIDLogical("const_" + bigInt.toString());

		pushValuesForward();
	}

	/**
	 * Creates an unsigned SimpleConstant from an array of bytes.
	 * 
	 * @param bytes
	 *            the byte array to use in constructing a SimpleConstant
	 * @return the representative SimpleConstant, will ALWAYS be unsigned.
	 * @throws UnsupportedOperationException
	 *             for an invalid byte array (null, empty or more bytes than can
	 *             be converted into a long).
	 */
	public static SimpleConstant createConstant(byte[] bytes) {
		if ((bytes == null) || (bytes.length == 0)) {
			throw new UnsupportedOperationException(
					"can't create long without valid byte array");
		}
		int maxLength = org.xronos.openforge.app.TypeLimits.C.getLongLongSize() / 8;
		if (bytes.length > maxLength) {
			throw new UnsupportedOperationException(
					"too many bytes to create a long value");
		}
		long longval = 0;
		int bitwidth = 0;

		for (int i = 0; i < bytes.length; i++) {
			longval |= (((long) bytes[i] & 0xFF) << bitwidth);
			bitwidth += 8;
		}
		return new SimpleConstant(longval, bitwidth, false);
	}

	/**
	 * Gets a new SimpleConstant based on the type of a java.lang.Number and
	 * with the specified width.
	 * 
	 * @return a {@link SimpleConstant}
	 */
	public static SimpleConstant getConstant(Number num, int width) {
		/*
		 * There isn't a need to set the width for floating point constants
		 * because their widths, 32 bits for float and 64 bits for double, are
		 * fixed.
		 */
		if (num instanceof Float) {
			return getFloatConstant(num.floatValue());
		} else if (num instanceof Double) {
			return getDoubleConstant(num.doubleValue());
		}

		return new SimpleConstant(num.longValue(), width);
	}

	public static SimpleConstant getFloatConstant(float lval) {
		SimpleConstant con = new SimpleConstant(Float.floatToRawIntBits(lval),
				32, false);
		con.getValueBus().setFloat(true);
		return con;
	}

	public static SimpleConstant getDoubleConstant(double lval) {
		SimpleConstant con = new SimpleConstant(
				Double.doubleToRawLongBits(lval), 64, false);
		con.getValueBus().setFloat(true);
		return con;
	}

	/**
	 * Returns an AURepBundle which is fully 'locked' and which specify the
	 * value (as constructed) of this constant. The unit ordering is always
	 * 'little endian'.
	 * 
	 * @return a value of type 'AURepBundle'
	 */
	@Override
	public AURepBundle getRepBundle() {
		return rep;
	}

	/**
	 * Returns a single element, non modifiable list containing this constant as
	 * its only constituent is itself.
	 * 
	 * @return a List containing only this constant
	 */

	@Override
	public List<SimpleConstant> getConstituents() {
		return Collections.unmodifiableList(Collections.singletonList(this));
	}

	/**
	 * Returns a single element non modifiable set containing only this object.
	 * 
	 * @return a singleton Set containing this object.
	 */
	@Override
	public Set<SimpleConstant> getContents() {
		return Collections.unmodifiableSet(Collections.singleton(this));
	}

	/**
	 * Value bus size is already set at creation.
	 */
	@Override
	public boolean pushValuesForward() {
		boolean mod = false;

		Value oldValue = getValueBus().getValue();

		Value newValue = new Value(oldValue.getSize(), oldValue.isSigned());
		AURepBundle repBundle = getRepBundle();
		for (int i = 0; i < oldValue.getSize(); i++) {
			// int byteIndex = i / 8;
			// int bitIndex = i % 8;
			int byteIndex = i / repBundle.getBitsPerUnit();
			int bitIndex = i % repBundle.getBitsPerUnit();
			assert repBundle.getRep()[byteIndex].isLocked();
			// if (((repBundle.getRep()[byteIndex].value() >>> bitIndex) & 0x1)
			// != 0)
			if (repBundle.getRep()[byteIndex].getBit(bitIndex) != 0) {
				newValue.setBit(i, Bit.ONE);
			} else {
				newValue.setBit(i, Bit.ZERO);
			}
		}

		mod |= getValueBus().pushValueForward(newValue);

		return mod;
	}

	/**
	 * Reverse constant prop has no rules applied on a Constant.
	 * 
	 * @return false
	 */
	@Override
	public boolean pushValuesBackward() {
		return false;
	}

	/**
	 * @return the value that this constant represents for naming
	 */
	private long getLval() {
		long value = 0;
		long mask = 0;
		for (int i = 0; i < rep.getBitsPerUnit(); i++)
			mask = (mask << 1) | 1L;

		for (int i = 0; i < rep.getLength(); i++) {
			// value |= (this.rep.getRep()[i].value() << (8*i));
			value |= ((rep.getRep()[i].getValue().longValue() & mask) << rep
					.getBitsPerUnit() * i);
		}

		return value;
	}

	@Override
	public String toString() {
		String ret = super.toString();
		if (getValueBus() != null && getValueBus().getValue() != null) {
			long valueBits = getValueBus().getValue().getConstantMask()
					& getValueBus().getValue().getValueMask();
			if (valueBits != getLval()) {
				ret += " lVal= " + getLval() + ">";
			}
		}
		return ret;
	}

}
