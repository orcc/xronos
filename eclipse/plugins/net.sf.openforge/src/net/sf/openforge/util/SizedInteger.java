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

package net.sf.openforge.util;

import java.math.BigInteger;

/**
 * <code>SizedInteger</code> is a class for performing integer computations of a
 * fixed size in either signed or unsigned ranges. This is useful for emulating
 * operations of primitive programming types, e.g. addition of signed 32-bit
 * numbers, subtraction of unsigned 8-bit numbers, etc.
 * <P>
 * For each operation, the size and sign range of the result is the same as that
 * of the operands. For most binary operations, the sign range and size of the
 * operands must agree. Note that instances of this class are immutable.
 * 
 * @version $Id: SizedInteger.java 2 2005-06-09 20:00:48Z imiller $
 */
public class SizedInteger {

	/**
	 * Always positive -- basically just an array of two's-complement bits; bits
	 * at index >= size are never set, as if they don't exist.
	 */
	private BigInteger bits;

	/**
	 * The number of significant bits; if isSigned is true, the MSB is a sign
	 * bit
	 */
	private int size;

	/** True if this is a signed integer, and the MSB is treated as a sign bit */
	private boolean isSigned;

	/**
	 * Gets a new <code>SizedInteger</code> representation.
	 * 
	 * @param number
	 *            the bits of the number
	 * @param size
	 *            the number of significant bits in the return value
	 * @param isSigned
	 *            the sign range of the return value
	 * @return a new <code>SizedInteger</code> for the specified value
	 */
	public static SizedInteger valueOf(long number, int size, boolean isSigned) {
		return valueOf(BigInteger.valueOf(number), size, isSigned);
	}

	/**
	 * Gets a new <code>SizedInteger</code> representation. This form is useful
	 * when there are more bits than will fit in a <code>long</code>.
	 * 
	 * @param number
	 *            the bits of the number
	 * @param size
	 *            the number of significant bits in the return value
	 * @param isSigned
	 *            the sign range of the return value
	 * @return a new <code>SizedInteger</code> for the specified value
	 */
	public static SizedInteger valueOf(BigInteger number, int size,
			boolean isSigned) {
		BigInteger bits = BigInteger.ZERO;
		for (int i = 0; i < size; i++) {
			if (number.testBit(i)) {
				bits = bits.setBit(i);
			}
		}
		return new SizedInteger(bits, size, isSigned);
	}

	/**
	 * Gets the result of converting this value to one with a given size and
	 * sign.
	 * 
	 * @param size
	 *            the size to conver to
	 * @param isSigned
	 *            true if the converted value is signed, false otherwise
	 * @return the converted value, or <code>this</code> if no conversion is
	 *         necessary
	 */
	public SizedInteger convert(int size, boolean isSigned) {
		return ((size == getSize()) && (isSigned == isSigned()) ? this
				: valueOf(numberValue(), size, isSigned));
	}

	/**
	 * Tests whether this value is negative.
	 * 
	 * @return true if this value is signed and negative, false otherwise
	 */
	public boolean isNegative() {
		return isSigned() && bits.testBit(size - 1);
	}

	/**
	 * Tests whether the value of this SizedInteger is 0.
	 * 
	 * @return true if the value of this integer is 0, false otherwise
	 */
	public boolean isZero() {
		return bits.equals(BigInteger.ZERO);
	}

	/**
	 * Tests whether this value is signed.
	 * 
	 * @return true if this value is signed, false otherwise
	 */
	public boolean isSigned() {
		return isSigned;
	}

	/**
	 * Gets the number of bits in this value.
	 * 
	 * @return the number of bits
	 */
	public int getSize() {
		return size;
	}

	/**
	 * Tests whether this object is equal to another.
	 * 
	 * @param object
	 *            the object to test
	 * @return true if the given object is a <code>SizedInteger</code> with the
	 *         same attributes as this object
	 */
	@Override
	public boolean equals(Object object) {
		if (object instanceof SizedInteger) {
			final SizedInteger s = (SizedInteger) object;
			return s.bits.equals(bits) && (size == s.getSize())
					&& (isSigned() == s.isSigned());
		}
		return false;
	}

	@Override
	public int hashCode() {
		return bits.hashCode();
	}

	/**
	 * Tests whether the bit at a given position is set.
	 * 
	 * @param i
	 *            the bit position, 0 <= <code>i</code> < {@link #getSize()}
	 * @return true if the bit is set, false if it is clear
	 * @exception IndexOutOfBoundsException
	 *                if the given position is out of range
	 */
	public boolean testBit(int i) throws IndexOutOfBoundsException {
		if ((i >= getSize()) || (i < 0)) {
			throw new IndexOutOfBoundsException("invalid bit: " + i);
		}

		return bits.testBit(i);
	}

	/**
	 * Gets the result of adding a <code>SizedInteger</code> to this one.
	 * 
	 * @param n
	 *            a <code>SizedInteger</code> value
	 * @return the result of the addition
	 * @throws IllegalArgumentException
	 *             if <code>n</code> does not have the same sign range or sign
	 *             as this instance
	 */
	public SizedInteger add(SizedInteger n) {
		checkArgs(this, n);
		final BigInteger result = numberValue().add(n.numberValue());
		return valueOf(result, size, isSigned);
	}

	/**
	 * Gets the logical and of <code>SizedInteger</code> and this instance.
	 * 
	 * @param n
	 *            a <code>SizedInteger</code> value
	 * @return the result of the logical and
	 * @throws IllegalArgumentException
	 *             if <code>n</code> does not have the same sign range or sign
	 *             as this instance
	 */
	public SizedInteger and(SizedInteger n) {
		checkArgs(this, n);
		final BigInteger result = bits.and(n.bits);
		return new SizedInteger(result, size, isSigned);
	}

	/**
	 * Compares this SizedInteger with the specified SizedInteger. This method
	 * is provided in preference to individual methods for each of the six
	 * boolean comparison operators (<, ==, >, >=, !=, <=). The suggested idiom
	 * for performing these comparisons is: (x.compareTo(y) <op> 0), where <op>
	 * is one of the six comparison operators.
	 * 
	 * @param n
	 *            SizedInteger to which this instance is to be compared
	 * @return -1, 0 or 1 as this SizedInteger is numerically less than, equal
	 *         to, or greater than <code>n</code>
	 * @throws IllegalArgumentException
	 *             if <code>n</code> does not have the same sign range or sign
	 *             as this instance
	 */
	public int compareTo(SizedInteger n) {
		checkArgs(this, n);
		return numberValue().compareTo(n.numberValue());
	}

	/**
	 * Gets the value of dividing this SizedInteger by a given SizedInteger.
	 * 
	 * @param n
	 *            the SizedInteger by which this instance is to be divided
	 * @return the result of the division
	 * @throws IllegalArgumentException
	 *             if <code>n</code> does not have the same sign range or sign
	 *             as this instance
	 */
	public SizedInteger divide(SizedInteger n) {
		checkArgs(this, n);
		final BigInteger result = numberValue().divide(n.numberValue());
		return valueOf(result, size, isSigned);
	}

	/**
	 * Gets the value of taking the modulus of this SizedInteger by a given
	 * SizedInteger.
	 * 
	 * @param n
	 *            the SizedInteger by which this instance is to be mod'd
	 * @return the result of the mod
	 * @throws IllegalArgumentException
	 *             if <code>n</code> does not have the same sign range or sign
	 *             as this instance
	 */
	public SizedInteger mod(SizedInteger n) {
		checkArgs(this, n);
		final BigInteger result = numberValue().remainder(n.numberValue());
		return valueOf(result, size, isSigned);
	}

	/**
	 * Gets the value of multiplying this SizedInteger by a given SizedInteger.
	 * 
	 * @param n
	 *            the SizedInteger by which this instance is to be multiplied
	 * @return the result of the multiplication
	 * @throws IllegalArgumentException
	 *             if <code>n</code> does not have the same sign range or sign
	 *             as this instance
	 */
	public SizedInteger multiply(SizedInteger n) {
		checkArgs(this, n);
		final BigInteger result = numberValue().multiply(n.numberValue());
		return valueOf(result, size, isSigned);
	}

	/**
	 * Gets the value of mathematically negating this SizedInteger.
	 * 
	 * @return the result of the negation
	 */
	public SizedInteger negate() {
		final BigInteger result = numberValue().negate();
		return valueOf(result, size, isSigned);
	}

	/**
	 * Gets the logical complement of this value.
	 * 
	 * @return the result of the complement
	 */
	public SizedInteger not() {
		final BigInteger result = numberValue().not();
		return valueOf(result, size, isSigned);
	}

	/**
	 * Gets the logical or of this SizedInteger with a given SizedInteger.
	 * 
	 * @param n
	 *            the SizedInteger with which this instance is or'd
	 * @return the result of the or
	 */
	public SizedInteger or(SizedInteger n) {
		checkArgs(this, n);
		final BigInteger result = bits.or(n.bits);
		return new SizedInteger(result, size, isSigned);
	}

	/**
	 * Gets the result of shifting this SizedInteger to the left by a given
	 * SizedInteger number of bits.
	 * 
	 * @param n
	 *            the number of bits by which this value is to be shifted left
	 * @return the result of the shift
	 */
	public SizedInteger shiftLeft(SizedInteger n) {
		BigInteger result = bits;
		final BigInteger magnitude = n.numberValue();
		for (BigInteger i = BigInteger.ZERO; i.compareTo(magnitude) < 0; i = i
				.add(BigInteger.ONE)) {
			result = result.shiftLeft(1).clearBit(size);
		}
		return valueOf(result, size, isSigned);
	}

	/**
	 * Gets the result of shifting this SizedInteger to the right by a given
	 * SizedInteger number of bits. If this value is signed, then sign extension
	 * will be performed; otherwise it will not.
	 * 
	 * @param n
	 *            the number of bits by which this value is to be shifted right
	 * @return the result of the shift
	 */
	public SizedInteger shiftRight(SizedInteger n) {
		return shiftRight(n, true);
	}

	/**
	 * Gets the result of shifting this SizedInteger to the right by a given
	 * SizedInteger number of bits. Sign extension is not performed; zero is
	 * used to fill.
	 * 
	 * @param n
	 *            the number of bits by which this value is to be shifted right
	 * @return the result of the shift
	 */
	public SizedInteger shiftRightUnsigned(SizedInteger n) {
		return shiftRight(n, false);
	}

	/**
	 * Gets the result of subtracting a SizedInteger from this SizedInteger.
	 * 
	 * @param n
	 *            the SizedInteger to be subtracted from this instance
	 * @return the result of the subtraction
	 * @throws IllegalArgumentException
	 *             if <code>n</code> does not have the same sign range or sign
	 *             as this instance
	 */
	public SizedInteger subtract(SizedInteger n) {
		checkArgs(this, n);
		final BigInteger result = numberValue().subtract(n.numberValue());
		return valueOf(result, size, isSigned);
	}

	/**
	 * Gets the result of performing an exclusive-or of this SizedInteger with a
	 * given SizedInteger.
	 * 
	 * @param n
	 *            the SizedInteger to be xor'd with this one
	 * @return the result of the xor
	 * @throws IllegalArgumentException
	 *             if <code>n</code> does not have the same sign range or sign
	 *             as this instance
	 */
	public SizedInteger xor(SizedInteger n) {
		checkArgs(this, n);
		final BigInteger result = bits.xor(n.bits);
		return new SizedInteger(result, size, isSigned);
	}

	/**
	 * Gets the numerical value of this SizedInteger.
	 * 
	 * @return the numerical value represented by this instance
	 */
	public BigInteger numberValue() {
		BigInteger result = bits;

		if (isNegative()) {
			/*
			 * If negative, use two's-complement conversion to get the magnitude
			 * and negate it.
			 */
			result = BigInteger.ZERO;
			for (int i = 0; i < size; i++) {
				if (!bits.testBit(i)) {
					result = result.setBit(i);
				}
			}
			result = BigInteger.ZERO.subtract(result.add(BigInteger.ONE));
		}

		return result;
	}

	public String toString(int radix) {
		return numberValue().toString(radix);
	}

	@Override
	public String toString() {
		return toString(10);
	}

	private SizedInteger(BigInteger bits, int size, boolean isSigned) {
		this.bits = bits;
		this.size = size;
		this.isSigned = isSigned;
	}

	private SizedInteger shiftRight(SizedInteger n, boolean isSignExtended) {
		BigInteger result = bits;
		final BigInteger magnitude = n.numberValue();
		for (BigInteger i = BigInteger.ZERO; i.compareTo(magnitude) < 0; i = i
				.add(BigInteger.ONE)) {
			final boolean isNegative = isNegative();
			result = result.shiftRight(1);
			if (isNegative && isSignExtended) {
				result = result.setBit(size - 1);
			}
		}
		return valueOf(result, size, isSigned);
	}

	private static void checkArgs(SizedInteger i, SizedInteger j)
			throws IllegalArgumentException {
		if (i.getSize() != j.getSize()) {
			throw new IllegalArgumentException("incompatible sizes: "
					+ i.getSize() + ", " + j.getSize());
		}

		if (i.isSigned() != j.isSigned()) {
			throw new IllegalArgumentException("incompatible sign: " + i
					+ " signed=" + i.isSigned() + ", " + j + " signed="
					+ j.isSigned());
		}
	}

}
