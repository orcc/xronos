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

package org.xronos.openforge.lim.memory;

import java.math.BigInteger;

import org.xronos.openforge.app.EngineThread;
import org.xronos.openforge.app.OptionRegistry;

/**
 * A Scalar is an implementation of {@link LogicalValue} for scalar numeric
 * values that represent pure numbers, i.e. not pointers. The internal
 * representation is in endian correct order, meaning that the getRep() method
 * returns values in address order.
 * 
 * @version $Id: Scalar.java 70 2005-12-01 17:43:11Z imiller $
 */
public class Scalar implements LogicalValue, MemoryVisitable {

	public static final Scalar buildByteScalar(BigInteger value,
			boolean isSigned, int byteLength, AddressStridePolicy policy) {
		assert policy == AddressStridePolicy.BYTE_ADDRESSING;

		// First we need to convert the value to a byte
		// representation.
		byte byteRep[] = new byte[byteLength];

		/*
		 * Copy the bits from the value to the rep, up to but not including the
		 * sign bit.
		 */
		int byteIndex = 0;
		int bitOffset = 0;
		final int valueBitLength = value.bitLength();
		for (int i = 0; i < valueBitLength; i++) {
			byteIndex = i / 8;
			bitOffset = i % 8;
			if (value.testBit(i) && byteIndex < byteRep.length) {
				byteRep[byteIndex] |= 1 << bitOffset;
			}
		}

		/*
		 * If the type is signed and the value is negative, perform sign
		 * extension to the end of the rep.
		 */
		if (isSigned && value.testBit(valueBitLength + 1)) {
			byteIndex = valueBitLength / 8;
			bitOffset = valueBitLength % 8;
			while (bitOffset < 8 && byteIndex < byteRep.length) {
				byteRep[byteIndex] |= 1 << bitOffset++;
			}

			while (++byteIndex < byteRep.length) {
				byteRep[byteIndex] |= 0xff;
			}
		}

		AddressableUnit[] rep = new AddressableUnit[byteRep.length];
		for (int i = 0; i < rep.length; i++) {
			rep[i] = new AddressableUnit(byteRep[i]);
		}

		return new Scalar(rep, policy);
	}

	public static final Scalar buildScalar(byte[] rep,
			AddressStridePolicy policy) {
		AddressableUnit[] units = new AddressableUnit[rep.length];
		for (int i = 0; i < units.length; i++) {
			// units[i] = new AddressableUnit(BigInteger.valueOf(rep[i]));
			units[i] = new AddressableUnit(rep[i]);
		}

		return new Scalar(units, policy);
	}

	private AddressableUnit[] rep;

	private boolean isLittleEndian;

	private AddressStridePolicy stridePolicy;

	/**
	 * Constructs a new Scalar object which has only a single addressable unit
	 * and the given addressing policy.
	 * 
	 * @param rep
	 *            the single {@link AddressableUnit} making up the value of this
	 *            Scalar.
	 * @param policy
	 *            the AddressStridePolicy for this LogicalValue
	 */
	public Scalar(AddressableUnit rep, AddressStridePolicy policy) {
		this(new AddressableUnit[] { rep }, policy);
	}

	/**
	 * Constructs a new Scalar LogicalValue whose value is indicated by the
	 * array of {@link AddressableUnit} objects and whose addressing is governed
	 * by the the given address policy.
	 * 
	 * @param rep
	 *            the array of bytes, high to low, containing the bitwise
	 *            representation of this value
	 * @param policy
	 *            the AddressStridePolicy for this LogicalValue
	 * @throws NullPointerException
	 *             if <code>rep</code> is null
	 */
	public Scalar(AddressableUnit[] rep, AddressStridePolicy policy) {
		this(rep, true, policy);
	}

	/**
	 * Describe constructor here.
	 * 
	 * @param rep
	 *            the array of initial values, index 0 is the least significant
	 *            bits of the values, index N-1 is the most significant.
	 * @param adjust
	 *            for endianness - if false do not adjust
	 * @param policy
	 *            the AddressStridePolicy for this LogicalValue
	 * @throws NullPointerException
	 *             if <code>rep</code> is null
	 */
	private Scalar(AddressableUnit[] rep, boolean adjust,
			AddressStridePolicy policy) {
		this.rep = new AddressableUnit[rep.length];
		stridePolicy = policy;
		if (policy == null) {
			throw new IllegalArgumentException("Cannot have null stride policy");
		}

		isLittleEndian = EngineThread.getGenericJob()
				.getUnscopedBooleanOptionValue(OptionRegistry.LITTLE_ENDIAN);

		if (!adjust || isLittleEndian) {
			System.arraycopy(rep, 0, this.rep, 0, rep.length);
		} else {
			for (int i = 0; i < rep.length; i++) {
				this.rep[i] = rep[rep.length - i - 1];
			}
		}
	}

	/**
	 * Implementation of the MemoryVisitable interface.
	 * 
	 * @param memVis
	 *            a non null 'MemoryVisitor'
	 * @throws NullPointerException
	 *             if memVis is null
	 */
	@Override
	public void accept(MemoryVisitor memVis) {
		memVis.visit(this);
	}

	/**
	 * Returns a new LogicalValue object (Scalar) which has the same numerical
	 * representation as this Scalar object.
	 * 
	 * @return a new LogicalValue with the same structure and initial values as
	 *         this LogicalValue.
	 */
	@Override
	public LogicalValue copy() {
		return new Scalar(rep, false, stridePolicy);
	}

	/**
	 * Returns the address stride policy governing this pointer.
	 */
	@Override
	public AddressStridePolicy getAddressStridePolicy() {
		return stridePolicy;
	}

	/**
	 * Returns the size of this logical value in addressable units (as a Scalar
	 * has only one constituent member, itself)
	 * 
	 * @return a value of type 'int'
	 */
	@Override
	public int getAlignmentSize() {
		return getSize();
	}

	/**
	 * Returns the number of bits allocated based on analysis of the
	 * AddressableUnit representation.
	 */
	@Override
	public int getBitSize() {
		return getSize() * stridePolicy.getStride();
	}

	/**
	 * Gets the bitwise representation of this value.
	 * 
	 * @return an array of {@link AddressableUnit}s, ordered in Least
	 *         Significant Address to Most Significant Address order (meaning
	 *         the higher the index to the rep, the higher it appears in memory)
	 *         the array may have no elements in the case that this is the value
	 *         of an empty data item, such as a struct with no fields
	 */
	@Override
	public AddressableUnit[] getRep() {
		final AddressableUnit[] copy = new AddressableUnit[rep.length];
		System.arraycopy(rep, 0, copy, 0, rep.length);
		return copy;
	}

	/**
	 * Gets the size in addressable units of this value.
	 * 
	 * @return the number of addresses needed to represent this value; this
	 *         number is at least 1 for a scalar value
	 */
	@Override
	public int getSize() {
		return rep.length;
	}

	/** @inheritDoc */
	@Override
	public LogicalValue getValueAtOffset(int delta, int size) {
		if (delta == 0 && size == getSize()) {
			return this;
		}

		return new Slice(this, delta, size);
	}

	/**
	 * Returns a copy of this LogicalValue in which the range of
	 * AddressableUnits specified by min and max (both inclusive) has been
	 * removed. Thus the returned LogicalValue has size of the original - (max -
	 * min + 1).
	 * 
	 * @param min
	 *            the offset of the least significant addressable unit of the
	 *            range to delete. 0 based offset.
	 * @param max
	 *            the offset of the most significant addressable unit to of the
	 *            range to delete. 0 based offset.
	 * @return a new LogicalValue object whose value is based on the current
	 *         LogicalValue with the specified range removed
	 * @throws NonRemovableRangeException
	 *             if any part of the range cannot be removed because the
	 *             resulting context would be non-meaningful (eg removal of a
	 *             portion of a pointers value)
	 */
	@Override
	public LogicalValue removeRange(int min, int max)
			throws NonRemovableRangeException {
		AddressableUnit[] rep = getRep();

		if (min < 0 || max < 0) {
			throw new NonRemovableRangeException(
					"Illegal range: requires positive range.");
		} else if (min > max) {
			throw new NonRemovableRangeException(
					"Illegal range: min is greater than max.");
		} else if (min > rep.length - 1 || max > rep.length - 1) {
			throw new NonRemovableRangeException(
					"Illegal range: range out of bound.");
		}

		int newSize = rep.length - (max - min + 1);
		AddressableUnit[] newRep = new AddressableUnit[newSize];

		for (int i = 0, j = 0; i < rep.length; i++) {
			if (i < min || i > max) {
				newRep[j] = rep[i];
				j++;
			}
		}

		return new Scalar(newRep, false, stridePolicy);
	}

	/**
	 * Returns a Constant which is little endian in representation.
	 * 
	 * @see org.xronos.openforge.lim.memory.LogicalValue#toConstant()
	 */
	@Override
	public MemoryConstant toConstant() {
		AddressableUnit[] rep = getRep();
		return new ScalarConstant(rep, getBitSize(), false,
				getAddressStridePolicy());
	}

	/**
	 * Gets the {@link Location} denoted by this value.
	 * 
	 * @return the location, or {@link Location#INVALID} if this value does not
	 *         denote a valid location
	 */
	@Override
	public Location toLocation() {
		return Location.INVALID;
	}

	@Override
	public String toString() {
		final StringBuffer buf = new StringBuffer();
		for (int i = 0; i < rep.length; i++) {
			// buf.append(Integer.toHexString(0xff & rep[i]));
			buf.append(rep[i].getValue().toString(16));
			if (i < rep.length - 1) {
				buf.append(".");
			}
		}
		return buf.toString();
	}

}
