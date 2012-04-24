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

package net.sf.openforge.lim.memory;

import net.sf.openforge.app.EngineThread;
import net.sf.openforge.app.OptionRegistry;

/**
 * A Pointer is a {@link LogicalValue} which refers to a {@link Location} in a
 * {@link LogicalMemory}.
 * 
 * @version $Id: Pointer.java 556 2008-03-14 02:27:40Z imiller $
 */
public class Pointer implements LogicalValue, MemoryVisitable,
		LocationValueSource {

	private Location target = null;

	private AddressStridePolicy stridePolicy = null;

	/** The value used to initialize the null pointer. */
	public static final int NULL_ADDRESS = -1;

	/**
	 * Creates a new <code>Pointer</code> instance with
	 * {@link AddressStridePolicy#BYTE_ADDRESSING} addressing policy
	 */
	public Pointer(Location target) {
		this(target, AddressStridePolicy.BYTE_ADDRESSING);
	}

	/**
	 * Creates a new <code>Pointer</code> instance.
	 * 
	 * @param target
	 *            the location referenced by this pointer; null if this is a
	 *            null pointer
	 * @param policy
	 *            the particular addressing policy used to govern how this value
	 *            is constructed
	 */
	public Pointer(Location target, AddressStridePolicy policy) {
		this.target = target;
		stridePolicy = policy;
	}

	/**
	 * Constructs a null <code>Pointer</code>. A target may be set later with
	 * {@link Pointer#setTarget(Location)}.
	 */
	public Pointer() {
		this((Location) null);
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
	 * Gets the size in addressable units of a Pointer.
	 * 
	 * @return the number of addressable units needed to represent this pointer.
	 */
	@Override
	public int getSize() {
		return getBitSize() / stridePolicy.getStride();
	}

	@Override
	public int getBitSize() {
		return net.sf.openforge.app.TypeLimits.C.getPointerSize();
	}

	/**
	 * Returns the address stride policy governing this pointer.
	 */
	@Override
	public AddressStridePolicy getAddressStridePolicy() {
		return stridePolicy;
	}

	/**
	 * Returns the size of this logical value in addressable units
	 * 
	 * @return a value of type 'int'
	 */
	@Override
	public int getAlignmentSize() {
		return getSize();
	}

	/**
	 * Gets the bitwise representation of this value, which is the address of
	 * the target location in the containing memory.
	 * 
	 * @return an array of {@link AddressableUnit}s, ordered in Least
	 *         Significant Address to Most Significant Address order (meaning
	 *         the higher the index to the rep, the higher it appears in memory)
	 *         the array may have no elements in the case that this is the value
	 *         of an empty data item, such as a struct with no fields
	 */
	@Override
	public AddressableUnit[] getRep() {
		AddressableUnit rep[] = new AddressableUnit[getSize()];
		Location target = getTarget();
		// assert rep.length <= 4 : "Pointer size too wide";
		assert getBitSize() <= 32 : "Pointer size too wide";
		int address;
		if (target == null)
			address = NULL_ADDRESS;
		else
			address = target.getLogicalMemory().getAddress(target);

		assert stridePolicy.getStride() <= 64;
		int stride = stridePolicy.getStride();
		int mask = 0;
		for (int i = 0; i < stride; i++)
			mask |= (1 << i);

		for (int i = 0; i < rep.length; i++) {
			int currentValue = (address >>> (stride * i)) & mask;
			rep[i] = new AddressableUnit(currentValue);
		}

		if (EngineThread.getGenericJob().getUnscopedBooleanOptionValue(
				OptionRegistry.LITTLE_ENDIAN)) {
			return rep;
		} else // need to reverse order if big endian
		{
			AddressableUnit rev[] = new AddressableUnit[rep.length];
			for (int i = 0; i < rep.length; i++) {
				rev[i] = rep[rep.length - i - 1];
			}
			return rev;
		}
	}

	/**
	 * Gets the target {@link Location} of this pointer.
	 * 
	 * @return the symbolic location which this pointer references
	 */
	@Override
	public Location getTarget() {
		return target;
	}

	/**
	 * Sets the target {@link Location} of this pointer.
	 * 
	 * @param location
	 *            the symbolic location which this pointer references
	 */
	@Override
	public void setTarget(Location location) {
		target = location;
	}

	/**
	 * Tests whether this is a null pointer.
	 * 
	 * @return true if this is a null pointer (has no target)
	 */
	public boolean isNull() {
		return target == null;
	}

	/**
	 * Returns a new LogicalValue object (Pointer) which points to the same
	 * Location object as the current LogicalValue.
	 * 
	 * @return a new LogicalValue which points to the same Location object.
	 */
	@Override
	public LogicalValue copy() {
		return new Pointer(target);
	}

	/**
	 * Throws a NonRemovableRangeException because no portion of a Pointer may
	 * be eliminated while still preserving the validity of the pointer.
	 * 
	 * @param min
	 *            the offset of the least significant byte of the range to
	 *            delete. 0 based offset.
	 * @param max
	 *            the offset of the most significant byte to of the range to
	 *            delete. 0 based offset.
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
		throw new NonRemovableRangeException(
				"No portion of a pointer may be eliminated");
	}

	/**
	 * Returns a LocationConstant for this pointer.
	 * 
	 * @see net.sf.openforge.lim.memory.LogicalValue#toConstant()
	 */
	@Override
	public MemoryConstant toConstant() {
		return new LocationConstant(getTarget(), getBitSize(),
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
		return getTarget();
	}

	/** @inheritDoc */
	@Override
	public LogicalValue getValueAtOffset(int delta, int size) {
		if ((delta == 0) && (size == getSize())) {
			return this;
		}

		return new Slice(this, delta, size);
	}

	@Override
	public String toString() {
		return "Pointer<" + Integer.toHexString(hashCode()) + ">->"
				+ getTarget();
	}

}
