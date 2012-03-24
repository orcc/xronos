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

import java.util.Map;

import net.sf.openforge.util.naming.ID;

/**
 * Variable is an implementation of {@link Location} which describes a memory
 * region that is named by a variable in the user application.
 * 
 * @version $Id: Variable.java 107 2006-02-23 15:46:07Z imiller $
 */
public abstract class Variable extends ID implements Location {

	/** The size, in addressable units, of this variable */
	private int size;

	/** The memory to which this location refers */
	private LogicalMemory logicalMemory;

	/**
	 * Constructs a new variable.
	 * 
	 * @param logicalMemory
	 *            the memory to which this variable refers
	 * @param size
	 *            the size in addressable units of the variable
	 * @throws IllegalArgumentException
	 *             if <code>size</code> is negative
	 * @throws NullPointerException
	 *             if <code>logicalMemory</code> is null
	 */
	public Variable(LogicalMemory logicalMemory, int size) {
		if (size < 0) {
			throw new IllegalArgumentException("illegal size: " + size);
		}
		if (logicalMemory == null) {
			throw new NullPointerException("null logicalMemory");
		}
		this.size = size;
		this.logicalMemory = logicalMemory;
	}

	/**
	 * Gets the size of this location in memory.
	 * 
	 * @return the number of continguous addressable units in memory that this
	 *         location represents
	 */
	@Override
	public int getAddressableSize() {
		return size;
	}

	/**
	 * Gets the {@link LogicalMemory} to which this Location refers.
	 * 
	 * @return the logical memory containing this location
	 */
	@Override
	public LogicalMemory getLogicalMemory() {
		return logicalMemory;
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
		return ((size == getAddressableSize()) && (delta == 0)) ? this
				: new Offset(size, this, delta);
	}

	/**
	 * Creates a new Location that represents a location at an unspecified index
	 * offset from this location. For instance, an array access of unknown index
	 * with this location as a base.
	 * 
	 * @param size
	 *            the size of the new location in addressable units.
	 * @return a location of the given size that is an unspecified number of
	 *         addressable units from this location; may return
	 *         <code>this</code> if <code>size</code> is 0.
	 */
	@Override
	public Location createIndex(int size) {
		return new Index(size, this);
	}

	/**
	 * Gets the original Location from which this Location was derived.
	 * 
	 * @return the base location, possibly <code>this</code>
	 */
	@Override
	public Location getBaseLocation() {
		return this;
	}

	/**
	 * Gets the Location which is the ultimate root of this Location such that:
	 * Location.getAbsoluteBase().getBaseLocation() ==
	 * Location.getAbsoluteBase().
	 * 
	 * <p>
	 * requires: Location.getBaseLocation() never null
	 * <p>
	 * modifies: none
	 * <p>
	 * effects: returns the root Location
	 * 
	 * @return the ultimate root Location of this Location.
	 */
	@Override
	public Location getAbsoluteBase() {
		Location absBase = this;
		while (absBase.getBaseLocation() != absBase) {
			absBase = absBase.getBaseLocation();
		}

		return absBase;
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

	/**
	 * Gets the minmum delta in terms of the absolute base
	 * 
	 * @return the minimum number of addressable units beyond the start of the
	 *         absolute base Location to which this location refers
	 */
	@Override
	public int getAbsoluteMinDelta() {
		// if the absolute is different from this location, then recursive call
		if (getAbsoluteBase() != this) {
			// my min data, plus my parent locs absolute min delta
			return getMinDelta() + getBaseLocation().getAbsoluteMinDelta();
		}
		return getMinDelta();
	}

	/**
	 * Gets the maximum delta.
	 * 
	 * @return the maximum number of addressable units beyond the start of the
	 *         base to which this location refers
	 */
	@Override
	public int getMaxDelta() {
		return 0;
	}

	/**
	 * Gets the maximum delta in terms of the absolute base
	 * 
	 * @return the maximum number of addressable units beyond the start of the
	 *         absolute base Location to which this location refers
	 */
	@Override
	public int getAbsoluteMaxDelta() {
		// if the absolute is different from this location, then recursive call
		if (getAbsoluteBase() != this) {
			// my max data, plus my parent locs absolute max delta
			return getMaxDelta() + getBaseLocation().getAbsoluteMaxDelta();
		}
		return getMaxDelta();
	}

	/**
	 * Determines if the parameter {@link Location} represents a region of
	 * memory that is covered by this Location.
	 * 
	 * <p>
	 * requires: none
	 * <p>
	 * modifies: none
	 * <p>
	 * effects: returns true if loc overlaps this Location, false otherwise
	 * 
	 * @param loc
	 *            a Location
	 * @return true if the parameter Location represents a region of memory that
	 *         is covered by this Location.
	 * @throws IllegalArgumenException
	 *             if 'loc' is null
	 */
	@Override
	public boolean overlaps(Location loc) {
		if (loc == null) {
			throw new IllegalArgumentException(
					"Null location cannot test for overlap");
		}

		final Location thisBase = getAbsoluteBase();
		final Location testBase = loc.getAbsoluteBase();

		if (thisBase != testBase)
			return false;

		Location thisLoc = this;
		int thisMinDelta = thisLoc.getAbsoluteMinDelta();
		int thisMaxDelta = thisLoc.getAbsoluteMaxDelta();

		Location testLoc = loc;
		int testMinDelta = testLoc.getAbsoluteMinDelta();
		int testMaxDelta = testLoc.getAbsoluteMaxDelta();

		// Define the 'window' or range of addressable units
		// (relative to the common base) that each Location may access.
		final int thisWindowMin = thisMinDelta;
		final int thisWindowMax = thisMaxDelta + getAddressableSize();
		final int testWindowMin = testMinDelta;
		final int testWindowMax = testMaxDelta + loc.getAddressableSize();

		// Overlap is true if those windows overlap at all.
		if (thisWindowMin >= testWindowMin && thisWindowMin < testWindowMax) {
			return true;
		}

		if (testWindowMin >= thisWindowMin && testWindowMin < thisWindowMax) {
			return true;
		}

		return false;
	}

	/**
	 * Does nothing except throw a NullPointerException if <code>loc</code> is
	 * null, or an IllegalArgumentException if units is < 0.
	 */
	@Override
	public void chopStart(Location loc, int units) {
		if (loc == null)
			throw new NullPointerException("null location not allowed");
		if (units < 0)
			throw new IllegalArgumentException(
					"cannot chop a negative number of addressable units from an allocation");
	}

	/**
	 * This method generates or retrieves a 'correlated' Location for the given
	 * Location, meaning that the correlated Location has exactly the same
	 * structure and 'base' hierarchy as the given Location, but is based off of
	 * a different base Location as specified by the correlation Map. The 'base'
	 * hierarchy of the given 'oldLoc' Location is traversed until a Location is
	 * found that is a valid key into the correlation map. The value stored in
	 * the map for that key is the new base on which to build the correlated
	 * Location. From that new base a sequence of Offset and Index Locations is
	 * created that parallels the structure found in the old location. Thus the
	 * returned correlated Location is identical to the old location in every
	 * respect except for the 'base' location.
	 * 
	 * <p>
	 * The Map and oldLoc are not modified by this method.
	 * 
	 * @param correlation
	 *            a non-null Map of Location to Location
	 * @param oldLoc
	 *            a non-null Location
	 * @return a correlated Location
	 * @throws NullPointerException
	 *             if oldLoc is null
	 * @throws IllegalArgumentException
	 *             if at least one Location of the 'base' hierarchy of 'oldLoc'
	 *             is not contained in correlation. Or thrown if any Location is
	 *             found in which the maxDelta is less than the minDelta.
	 */
	public static Location getCorrelatedLocation(
			Map<Location, Location> correlation, Location oldLoc) {
		if (oldLoc == null) {
			throw new NullPointerException(
					"Null Location not allowed in this context");
		}

		// If the map contains the correlated location already the
		// simply return that one to avoid creating too many copies of
		// the same thing.
		if (correlation.containsKey(oldLoc)) {
			return correlation.get(oldLoc);
		}

		Location baseLocation = oldLoc.getBaseLocation();
		Location correlatedBase = correlation.get(baseLocation);

		if (correlatedBase == null) {
			if (baseLocation == oldLoc) {
				throw new IllegalArgumentException(
						"Correlation map for location correlation does not "
								+ "contain a necessary base location.");
			} else {
				correlatedBase = getCorrelatedLocation(correlation,
						baseLocation);
			}
		}
		assert correlatedBase != null : "No base correlation found for location";

		return oldLoc.duplicateForBaseLocation(correlatedBase);
	}

}
