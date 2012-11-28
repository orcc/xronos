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
import java.util.Set;

import org.xronos.openforge.lim.memory.Location;


/**
 * LocationCluster is a collection of {@link Location Locations} such that for
 * any two locations A and B, either A and B overlap, or else there is a chain
 * of locations, L0 to Ln, in the cluster such that A overlaps L0, L0 overlaps
 * L1, ... Ln-1 overlaps Ln, and Ln overlaps B. In other words, there is an
 * "overlap chain" linking any two Locations in a cluster. This also implies
 * that all Locations in a cluster will have the same absolute base Location.
 * <P>
 * LocationCluster provides facilities for determining the absolute range of
 * bytes from the absolute base Location that are represented and for obtaining
 * the maximum size of any Location in the cluster.
 * 
 * <p>
 * Created: Fri Aug 29 10:40:17 2003
 * 
 * @author imiller, last modified by $Author: imiller $
 * @version $Id: LocationCluster.java 70 2005-12-01 17:43:11Z imiller $
 */
class LocationCluster {

	/* set of all locations */
	private Set<Location> cluster = new HashSet<Location>(3);

	LocationCluster() {
	}

	/**
	 * Adds a Location to this cluster
	 * 
	 * @param loc
	 *            a value of type 'Location'
	 * @throws IllegalArgumentException
	 *             if cluster contains at least one Location and loc does not
	 *             overlap at least one previously contained Location.
	 */
	public void addLocation(Location loc) {
		if (cluster.isEmpty() || overlaps(loc)) {
			cluster.add(loc);
		} else {
			throw new IllegalArgumentException(
					"requires Location to overlap with at least one location in the cluster");
		}
	}

	/**
	 * Returns true if loc overlaps ({@link Location#overlaps}) any Location
	 * previously added to the LocationCluster.
	 * 
	 * <p>
	 * effects : returns true if loc overlaps any Location in this cluster.
	 * 
	 * @param loc
	 *            a non-null 'Location'
	 * @return true if loc overlaps a contained Location.
	 * @throws IllegalArgumentException
	 *             if loc is null
	 */
	public boolean overlaps(Location loc) {
		if (loc == null) {
			throw new IllegalArgumentException("null loc");
		}

		for (Location location : cluster) {
			if (loc.overlaps(location)) {
				return true;
			}
		}

		return false;
	}

	/**
	 * Retrieves the range of bytes covered by all Locations that have been
	 * added to this LocationCluster
	 * 
	 * @return a value of type 'ByteRange'
	 */
	public ByteRange getRange() {
		assert (!cluster.isEmpty());

		int min = cluster.iterator().next().getAbsoluteMinDelta();
		int max = 0;

		for (Location location : cluster) {
			min = Math.min(min, location.getAbsoluteMinDelta());
			max = Math.max(
					max,
					location.getAbsoluteMaxDelta()
							+ location.getAddressableSize());
		}

		return new ByteRange(min, max);
	}

	/**
	 * Returns the maximum size of any Location in this cluster.
	 * 
	 * @return a non-negative 'int'
	 */
	public int maxSize() {
		int maxSize = 0;

		for (Location location : cluster) {
			maxSize = Math.max(maxSize, location.getAddressableSize());
		}

		return maxSize;
	}

	/**
	 * A lightweight class to represent a range values.
	 */
	class ByteRange {
		private int min = 0;
		private int max = 0;

		ByteRange(int min, int max) {
			assert min >= 0 && max >= 0;
			this.min = min;
			this.max = max;
		}

		public int getMin() {
			return min;
		}

		public int getMax() {
			return max;
		}
	}

}// LocationCluster

