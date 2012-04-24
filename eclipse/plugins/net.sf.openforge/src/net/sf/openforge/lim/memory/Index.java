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

/**
 * Index is an implementation of {@link Location} which describes a memory
 * region in terms of an unspecified offset from an existing {@link Location}.
 * The index itself is described in terms of the base {@link Location} and a
 * size, but the actual number of addressable units of offset is unspecified.
 * This class is useful for representing array access locations for which the
 * size and base are known but the actual index is not. It represents a possible
 * access of the specified size at any index of the array memory.
 * 
 * @version $Id: Index.java 70 2005-12-01 17:43:11Z imiller $
 */
public class Index extends Variable {
	private Location baseLocation;

	@Override
	public boolean equals(Object object) {
		if (object instanceof Index) {
			final Index index = (Index) object;
			return baseLocation.equals(index.baseLocation)
					&& (getAddressableSize() == index.getAddressableSize());
		}
		return false;
	}

	@Override
	public int hashCode() {
		return baseLocation.hashCode() + getAddressableSize();
	}

	/**
	 * Constructs a new Index.
	 * 
	 * @param size
	 *            the size in addressable units of the location
	 * @param baseLocation
	 *            the location serving as the base of the offset
	 */
	Index(int size, Location baseLocation) {
		super(baseLocation.getLogicalMemory(), size);
		this.baseLocation = baseLocation;
	}

	/**
	 * Creates and returns a <code>Location</code> that is a duplicate of this
	 * one using a given <code>Location</code> as a base.
	 * 
	 * @param baseLocation
	 *            the base for the duplicate location
	 * @return the duplicated location
	 */
	@Override
	public Location duplicateForBaseLocation(Location baseLocation) {
		return baseLocation.createIndex(getAddressableSize());
	}

	/** @inheritDoc */
	@Override
	public Location createIndex(int size) {
		/*
		 * Defer to the base location to reduce unnecessary indirection.
		 */
		return getBaseLocation().createIndex(size);
	}

	/**
	 * Gets the base location.
	 * 
	 * @return the {@link Location location} from which this indexed offset is
	 *         calculated
	 */
	@Override
	public Location getBaseLocation() {
		return baseLocation;
	}

	/**
	 * Gets the minmum delta.
	 * 
	 * @return the minimum number of addressable units beyond the start of the
	 *         base to which this location refers
	 */
	@Override
	public int getMinDelta() {
		// slide by base's absolute min delta..
		return 0 - getBaseLocation().getAbsoluteMinDelta();
	}

	/**
	 * Gets the maximum delta.
	 * 
	 * <p>
	 * requires: base location not null, base location != this,
	 * <p>
	 * modifies: none
	 * <p>
	 * effects: returns the max number of addressable units beyond the start of
	 * the base to which this location can access.
	 * 
	 * @return the maximum number of addressable units beyond the start of the
	 *         base to which this location refers
	 */
	@Override
	public int getMaxDelta() {
		// absolute's size, minus my size (this is the last byte on the absolute
		// sace readable
		// - baselocations absoluteMinDelta() (slide it for the base location's
		// start)
		return getAbsoluteBase().getAddressableSize() - getAddressableSize()
				- getBaseLocation().getAbsoluteMinDelta();
	}

	/**
	 * Returns the LogicalValue that represents the initial value for this
	 * Location, throws UnsupportedOperationException because there is no
	 * defined LogicalValue for an Index Location because the offset is
	 * undetermined.
	 * 
	 * @return none throws an UnsupportedOperationException when called.
	 * @throws {@link Location#IllegalInitialValueContextException} because the
	 *         initial value of a variable index access is undefined.
	 */
	@Override
	public LogicalValue getInitialValue() {
		throw new Location.IllegalInitialValueContextException(
				"Variable index access");
	}

	/**
	 * Throws a NullPointerException if <code>loc</code> is null or an
	 * UnsupportedOperationException otherwise.
	 */
	@Override
	public void chopStart(Location loc, int units) {
		super.chopStart(loc, units);
		throw new UnsupportedOperationException(
				"cannot modify the base of a variably indexed access");
	}

	@Override
	public String debug() {
		return "<" + getAbsoluteBase().debug() + "> index siz: "
				+ getAddressableSize();
	}

	@Override
	public String toString() {
		return "[Index(" + getAddressableSize() + "U)@"
				+ Integer.toHexString(hashCode()) + "=" + baseLocation + "]";
	}
}
