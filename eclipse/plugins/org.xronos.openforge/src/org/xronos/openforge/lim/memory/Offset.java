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
 * Offset is an implementation of {@link Location} which describes a memory
 * region in terms of an offset from an existing {@link Location}. The offset
 * itself is decscribed in terms of the base {@link Location} and an increment
 * that represents a number of addressable units past the base {@link Location}.
 * 
 * @version $Id: Offset.java 70 2005-12-01 17:43:11Z imiller $
 */
public class Offset extends Variable {

	/** The location from which this offset is calculated */
	private Location baseLocation;

	/**
	 * The number of addressable units past the start of the base location
	 */
	private int delta;

	/**
	 * Constructs a new Offset.
	 * 
	 * @param size
	 *            the size in addressable units of the variable; a size of 0
	 *            indicates that this is a pure location (ie, a pointer)
	 * @param baseLocation
	 *            the base location; the delta is applied to the start address
	 *            of this location to obtain the start of this location
	 * @param delta
	 *            the number of addresses past the start of
	 *            <code>baseLocation</code> at which this location begins; the
	 *            value may be negative to indicate a backwards offset
	 * @throws IllegalArgumentException
	 *             if <code>size</code> is negative
	 * @throws NullPointerException
	 *             if <code>baseLocation</code> is null
	 */
	Offset(int size, Location baseLocation, int delta) {
		super(baseLocation.getLogicalMemory(), size);
		this.baseLocation = baseLocation;
		this.delta = delta;
	}

	/**
	 * If the specified Location <code>loc</code> is the base location of this
	 * Offset, then the min/max delta of this Location will be reduced by the
	 * <code>units</code> parameter, otherwise this method does nothing.
	 */
	@Override
	public void chopStart(Location loc, int units) {
		super.chopStart(loc, units); // tests for null
		Location base = getBaseLocation();
		if (loc.getAbsoluteBase() == base.getAbsoluteBase()
				&& loc.getAbsoluteMinDelta() == base.getAbsoluteMinDelta()
				&& loc.getAbsoluteMaxDelta() == loc.getAbsoluteMaxDelta()
				&& loc.getAddressableSize() == base.getAddressableSize()) {
			if (units > delta) {
				throw new IllegalArgumentException(
						"Cannot shift location by more addressable units than in its base");
			}
			delta = delta - units;
		}
	}

	/**
	 * Creates a Location that represents an offset from this Location.
	 * 
	 * @param size
	 *            the size in addressable units of the new location
	 * @param delta
	 *            the number of addressable units beyond the start of this
	 *            location at which the offset location begins
	 * @throws IllegalArgumentException
	 *             if <code>size</code> is negative
	 */
	@Override
	public Location createOffset(int size, int delta) {
		return size == getAddressableSize() && delta == 0 ? this
				: getBaseLocation().createOffset(size, getMinDelta() + delta);
	}

	@Override
	public String debug() {
		String ret = "";
		ret += "<" + getAbsoluteBase().debug() + ">";
		ret += " min: " + getAbsoluteMinDelta();
		ret += " max: " + getAbsoluteMaxDelta();
		ret += " siz: " + getAddressableSize();
		return ret;
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
		return baseLocation.createOffset(getAddressableSize(), delta);
	}

	@Override
	public boolean equals(Object object) {
		if (object instanceof Offset) {
			final Offset offset = (Offset) object;
			return delta == offset.delta
					&& baseLocation.equals(offset.baseLocation)
					&& offset.getAddressableSize() == getAddressableSize();
		}
		return false;
	}

	/**
	 * Gets the base location.
	 * 
	 * @return the {@link Location location} from which this offset is
	 *         calculated
	 */
	@Override
	public Location getBaseLocation() {
		return baseLocation;
	}

	/**
	 * Returns the LogicalValue that represents the initial value for this
	 * Location. The LogicalValue is generated by obtaining the LogicalValue for
	 * the base location and then constructing a new LogicalValue for this
	 * Location based on the delta.
	 * 
	 * @return a LogicalValue representing the initial value of this Location.
	 * @throws {@link Location#IllegalInitialValueContextException} when the
	 *         Location context does not have a LogicalValue to represent its
	 *         initial value.
	 * @throws an
	 *             UnsupportedOperationException when byte representation of the
	 *             LogicalValue is invalid (null, zero length, or greater than
	 *             the size of a long).
	 */
	@Override
	public LogicalValue getInitialValue() {
		return getBaseLocation().getInitialValue().getValueAtOffset(delta,
				getAddressableSize());
	}

	/**
	 * Gets the maximum delta.
	 * 
	 * @return the maximum number of addressable units beyond the start of the
	 *         base to which this location refers
	 */
	@Override
	public int getMaxDelta() {
		return delta;
	}

	/**
	 * Gets the minmum delta.
	 * 
	 * @return the minimum number of addressable units beyond the start of the
	 *         base to which this location refers
	 */
	@Override
	public int getMinDelta() {
		return getMaxDelta();
	}

	@Override
	public int hashCode() {
		return baseLocation.hashCode() + delta + getAddressableSize();
	}

	@Override
	public String toString() {
		StringBuffer buf = new StringBuffer();
		buf.append("Offset(");
		buf.append(getAddressableSize());
		buf.append("B)");
		buf.append(Integer.toHexString(hashCode()));
		buf.append("{(");
		buf.append(getBaseLocation());
		buf.append(") + ");
		buf.append(getMaxDelta());
		buf.append("}");
		return buf.toString();
	}

}
