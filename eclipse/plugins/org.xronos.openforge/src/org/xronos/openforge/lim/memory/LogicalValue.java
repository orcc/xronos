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

/**
 * An instance of LogicalValue is the initial value of a given location in a
 * {@link LogicalMemory}. This initial value is specified when allocating a new
 * region of the memory. A LogicalValue can report its size in addressable units
 * as well as the content (numerical value) of those units.
 * 
 * @version $Id: LogicalValue.java 70 2005-12-01 17:43:11Z imiller $
 */
public interface LogicalValue extends MemoryVisitable {

	/**
	 * Gets the size in addressable units of this LogicalValue.
	 * 
	 * @return the number of addressable units needed to represent this value;
	 *         this number may be 0 in the case of an empty data item, such as
	 *         an array of 0 elements
	 */
	public int getSize();

	/**
	 * Returns the number of bits allocated in this logical value
	 */
	public int getBitSize();

	/**
	 * Returns the address stride policy governing this logical value.
	 */
	public AddressStridePolicy getAddressStridePolicy();

	/**
	 * Gets the addressable unit boundry (number of addressable units) on which
	 * this value must be aligned (effectively this is the size of the largest
	 * constituent member of this logical value).
	 * 
	 * @return a value of type 'int'
	 */
	public int getAlignmentSize();

	/**
	 * Gets the bitwise representation of this value.
	 * 
	 * @return an array of {@link AddressableUnit} objects, ordered in Least
	 *         Significant Address to Most Significant Address order (meaning
	 *         the higher the index to the rep, the higher it appears in memory)
	 *         the array may have no elements in the case that this is the value
	 *         of an empty data item, such as a struct with no fields
	 */
	public AddressableUnit[] getRep();

	/**
	 * Returns a new LogicalValue object which has been deep copied from the
	 * current LogicalValue
	 * 
	 * @return a new LogicalValue with the same structure and initial values as
	 *         this LogicalValue.
	 */
	public LogicalValue copy();

	/**
	 * Returns a copy of this LogicalValue in which the range of addressable
	 * units specified by min and max (both inclusive) has been removed. Thus
	 * the returned LogicalValue has size of the original - (max - min + 1).
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
	public LogicalValue removeRange(int min, int max)
			throws NonRemovableRangeException;

	/**
	 * If possible, produces (and returns) the constant value of this logical
	 * value in little endian format. May throw an UnsupportedOperationException
	 * exception if the value can not be determined.
	 * 
	 * @return a Constant
	 * @throws UnsupportedOperationException
	 *             when called on a LogicalValue which is variable at runtime.
	 */
	public MemoryConstant toConstant();

	/**
	 * Gets the {@link Location} denoted by this value.
	 * 
	 * @return the location, or {@link Location#INVALID} if this value does not
	 *         denote a valid location
	 */
	public Location toLocation();

	/**
	 * Gets a {@link LogicalValue} that represents a specific portion of this
	 * value.
	 * 
	 * @param delta
	 *            the number of addressable units from the beginning of this
	 *            value at which the returned value starts
	 * @param size
	 *            the size of the returned value
	 * @return a new value that represents the contents of this value starting
	 *         at <code>delta</code> units from the beginning of this value and
	 *         extending for <code>size</code> bytes
	 */
	public LogicalValue getValueAtOffset(int delta, int size);
}
