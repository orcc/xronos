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

package org.xronos.openforge.optimize.memory;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.xronos.openforge.lim.memory.Location;


/**
 * LocationSet is a special Set implementation which is capable of comparing
 * Locations to determine what regions of bytes are addressed, and considers any
 * 2 Locations which access exactly the same byte range as the same and will
 * only store one of them in the Set. Locations are compared for equality based
 * on thier absolute base location, absolute min/max delta, and size. Thus the
 * type of Location is not relevent, simply the range of accessed bytes and size
 * of access.
 * 
 * <p>
 * Created: Wed Oct 22 10:42:54 2003
 * 
 * @author imiller, last modified by $Author: imiller $
 * @version $Id: LocationSet.java 70 2005-12-01 17:43:11Z imiller $
 */
public class LocationSet {

	/**
	 * The actual Set used to store the Locations added to this class.
	 */
	private Set<Location> backingSet = new HashSet<Location>();

	public LocationSet() {
	}

	/**
	 * Adds the specified {@link Location} to this set iff the location does not
	 * exactly match the byte range specified by any Location already contained
	 * within this Set.
	 * 
	 * @param loc
	 *            a non-null {@link Location}
	 * @return <code>true</code> if this set did not already contain the
	 *         specified Location.
	 * @throws IllegalArgumentException
	 *             if loc is null.
	 */
	public boolean add(Location loc) {
		// If the location is an exact match for any already contained
		// Location, then return without adding the new location.
		for (Iterator<Location> iter = this.iterator(); iter.hasNext();) {
			Location testLoc = iter.next();
			if ((testLoc.getAbsoluteBase() == loc.getAbsoluteBase())
					&& (testLoc.getAbsoluteMaxDelta() == loc
							.getAbsoluteMaxDelta())
					&& (testLoc.getAbsoluteMinDelta() == loc
							.getAbsoluteMinDelta())
					&& (testLoc.getAddressableSize() == loc
							.getAddressableSize())) {
				return false;
			}
		}
		return backingSet.add(loc);
	}

	/**
	 * Returns the number of Locations contained in this Set.
	 * 
	 * @return a non-negative 'int'
	 */
	public int size() {
		return backingSet.size();
	}

	/**
	 * Returns an Iterator over the Location objects contained in this Set.
	 * 
	 * @return an 'Iterator'
	 */
	public Iterator<Location> iterator() {
		return backingSet.iterator();
	}

	@Override
	public String toString() {
		return "LocationSet@" + Integer.toHexString(this.hashCode())
				+ backingSet;

	}

}// LocationSet
