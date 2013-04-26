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
 * A Location designates a region of memory. It is a symbolic representation,
 * and so only records the size of the region as a number of addressable units.
 * The actual address of the Location is given by the {@link LogicalMemory} to
 * which the Location belongs.
 * 
 * @see {@link LogicalMemory#getAddress (Location location)}
 */
public interface Location {

	/**
	 * This exception is thrown in instances where the initial value for a
	 * Location cannot be determined statically but is requested by some
	 * operation.
	 */
	@SuppressWarnings("serial")
	static class IllegalInitialValueContextException extends RuntimeException {
		public IllegalInitialValueContextException(String msg) {
			super("Illegal context for obtaining initial value: " + msg);
		}
	}

	/** A constant to represent any invalid location, e.g. (int *)1234. */
	public static final Location INVALID = new Location() {
		@Override
		public void chopStart(Location loc, int delta) {
		}

		@Override
		public Location createIndex(int size) {
			return this;
		}

		@Override
		public Location createOffset(int size, int delta) {
			return this;
		}

		@Override
		public String debug() {
			return "INVALID";
		}

		@Override
		public Location duplicateForBaseLocation(Location base) {
			return this;
		}

		@Override
		public Location getAbsoluteBase() {
			return this;
		}

		@Override
		public int getAbsoluteMaxDelta() {
			return 0;
		}

		@Override
		public int getAbsoluteMinDelta() {
			return 0;
		}

		@Override
		public int getAddressableSize() {
			return 0;
		}

		@Override
		public Location getBaseLocation() {
			return this;
		}

		@Override
		public LogicalValue getInitialValue() {
			throw new UnsupportedOperationException("INVALID Location");
		}

		@Override
		public LogicalMemory getLogicalMemory() {
			return null;
		}

		@Override
		public int getMaxDelta() {
			return 0;
		}

		@Override
		public int getMinDelta() {
			return 0;
		}

		@Override
		public boolean overlaps(Location loc) {
			return loc == this;
		}
	};

	/**
	 * The chop start method is used to modify a Location in response to a
	 * change made in the specified parameter loc. The method is called when the
	 * Location <code>loc</code> has had <code>units</code> addressable units
	 * removed from the beginning of its range. If that modification requires a
	 * change in the structure of this Location, then the change is made,
	 * otherwise nothing happens. This method is non-recursive and will
	 * <b>not</b> pass along the call to the base location.
	 * 
	 * @param loc
	 *            a non-null {@link Location}
	 * @param units
	 *            a non-negative integer value.
	 * @throws NullPointerException
	 *             if the loc parameter is null
	 * @throws UnsupportedOperationException
	 *             if the context cannot handle a modification of its base
	 *             location.
	 */
	public void chopStart(Location loc, int units);

	/**
	 * Creates a new Location that represents a location at an unspecified index
	 * offset from this location. For instance, an array access of unknown index
	 * with this location as a base.
	 * 
	 * @param size
	 *            the size of the new location in addressable units
	 * @return a location of the given size that is an unspecified number of
	 *         addressable units from this location; may return
	 *         <code>this</code> if <code>size</code> is 0.
	 */
	public Location createIndex(int size);

	/**
	 * Creates a new Location that represents a fixed offset from this Location.
	 * 
	 * @param size
	 *            the size, in addressable units, of the new location
	 * @param delta
	 *            the number of addressable units beyond the start of this
	 *            location at which the offset location begins
	 * @return the offset location; may be <code>this</code> if
	 *         <code>size</code> is the same as the size of this location and
	 *         <code>delta</code> is 0
	 */
	public Location createOffset(int size, int delta);

	public String debug();

	/**
	 * Creates and returns a <code>Location</code> that is a duplicate of this
	 * one using a given <code>Location</code> as a base.
	 * 
	 * @param baseLocation
	 *            the base for the duplicate location
	 * @return the duplicated location
	 */
	public Location duplicateForBaseLocation(Location baseLocation);

	/**
	 * Gets the Location which is the ultimate root of this Location such that:
	 * Location.getAbsoluteBase().getBaseLocation() ==
	 * Location.getAbsoluteBase().
	 * 
	 * @return the ultimate root Location of this Location.
	 */
	public Location getAbsoluteBase();

	/**
	 * Gets the maximum delta in terms of the absolute base
	 * 
	 * @return the maximum number of addressable units beyond the start of the
	 *         absolute base Location to which this location refers
	 */
	public int getAbsoluteMaxDelta();

	/**
	 * Gets the minmum delta in terms of the absolute base
	 * 
	 * @return the minimum number of addressable units beyond the start of the
	 *         absolute base Location to which this location refers
	 */
	public int getAbsoluteMinDelta();

	/**
	 * Gets the size of this location in memory in terms of the number of
	 * addressable units which comprise this location.
	 * 
	 * @return the number of addressable spaces in memory that this location
	 *         represents
	 */
	public int getAddressableSize();

	/**
	 * Gets the original Location from which this Location was derived.
	 * 
	 * @return the base location, possibly <code>this</code>
	 */
	public Location getBaseLocation();

	/**
	 * Returns the LogicalValue that represents the initial value for this
	 * Location when called on a Location which has a LogicalValue, throws an
	 * UnsupportedOperationException otherwise.
	 * 
	 * @return a LogicalValue representing the initial value of this Location.
	 * @throws UnsupportedOperationException
	 *             when the Location context does not have a LogicalValue to
	 *             represent its initial value.
	 */
	public LogicalValue getInitialValue();

	/**
	 * Gets the {@link LogicalMemory} to which this Location refers.
	 * 
	 * @return the logical memory containing this location
	 */
	public LogicalMemory getLogicalMemory();

	/**
	 * Gets the maximum delta.
	 * 
	 * @return the maximum number of addressable units beyond the start of the
	 *         base Location to which this location refers
	 */
	public int getMaxDelta();

	/**
	 * Gets the minmum delta.
	 * 
	 * @return the minimum number of addressable units beyond the start of the
	 *         base Location to which this location refers
	 */
	public int getMinDelta();

	/**
	 * Determines if the parameter Location represents a region of memory that
	 * is covered by this Location.
	 * 
	 * @param loc
	 *            a Location
	 * @return true if the parameter Location represents a region of memory that
	 *         is covered by this Location.
	 */
	public boolean overlaps(Location loc);
}
