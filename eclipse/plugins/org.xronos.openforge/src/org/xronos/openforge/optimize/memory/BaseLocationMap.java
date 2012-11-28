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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.xronos.openforge.lim.memory.Allocation;
import org.xronos.openforge.lim.memory.LValue;
import org.xronos.openforge.lim.memory.Location;
import org.xronos.openforge.lim.memory.LogicalMemory;
import org.xronos.openforge.lim.memory.LogicalValue;
import org.xronos.openforge.lim.memory.NonRemovableRangeException;


/**
 * BaseLocationMap is a Mapping of all Locations associated with a given
 * absolute base location, thus all the Locations that share a common
 * LogicalValue. The Locations are grouped in LocationClusters which represent a
 * collection of Locations that overlap a region of memory. The map provides
 * methods to determine the largest amount that the contained LocationClusters
 * can be moved by while preserving alignment.
 * 
 * <p>
 * Created: Fri Aug 29 09:49:58 2003
 * 
 * @author imiller, last modified by $Author: imiller $
 * @version $Id: BaseLocationMap.java 29 2005-09-15 15:18:38Z imiller $
 */
class BaseLocationMap {

	private Location absoluteLoc = null;

	/* a list of LocationClusters which have the same base location */
	private List<LocationCluster> clusters = new ArrayList<LocationCluster>();

	/**
	 * Generates a new BaseLocationMap for holding Locations with the given
	 * Location 'loc' as their absolute base location
	 * 
	 * @param loc
	 *            a 'Location' which is its own base.
	 * @throws IllegalArgumentException
	 *             if loc is null or loc.getBaseLocation() != loc.
	 */
	BaseLocationMap(Location loc) {
		if (loc == null) {
			throw new IllegalArgumentException("requires non-null location.");
		}
		if (!(loc.getBaseLocation() == loc)) {
			throw new IllegalArgumentException(
					"requires location to be its own base.");
		}

		absoluteLoc = loc;
	}

	/**
	 * Provides access to the base location that makes this map
	 * 
	 * @return base location of this map
	 */
	Location getAbsoluteBase() {
		return absoluteLoc;
	}

	/**
	 * Adds the given Location to this map.
	 * 
	 * <p>
	 * modifies: this
	 * 
	 * @param loc
	 *            a non-null 'Location'
	 * @throws IllegalArgumentException
	 *             if loc is null or if loc.getAbsoluteBase() != the location
	 *             this map was constructed with.
	 */
	public void addLocation(Location loc) {
		if (loc == null) {
			throw new IllegalArgumentException("requires non-null location.");
		}

		if (!(loc.getAbsoluteBase() == absoluteLoc)) {
			throw new IllegalArgumentException(
					"requires absolute base location to be this location.");
		}

		LocationCluster newCluster = new LocationCluster();
		if (clusters.isEmpty()) {
			newCluster.addLocation(loc);
			clusters.add(newCluster);
		} else {
			// If the location overlaps with any location
			// in a cluater, add this location to that cluster.
			Iterator<LocationCluster> clusterIter = clusters.iterator();
			boolean locOverlaps = false;

			while (clusterIter.hasNext()) {
				LocationCluster cluster = clusterIter.next();

				if (cluster.overlaps(loc)) {
					locOverlaps = true;
					cluster.addLocation(loc);
					break;
				}
			}

			if (!locOverlaps) {
				// If the location does not overlap with any location
				// in the current cluaters, add this location to a new cluster.
				newCluster.addLocation(loc);
				clusters.add(newCluster);
			}
		}
	}

	/**
	 * Returns the maximum number of bytes that every Location in this map can
	 * be shifted left while preserving both correct alignment and all necessary
	 * data values. This means that there exists N empty bytes to the left
	 * (smaller delta) of each Location or that the Location to the left of the
	 * current Location is also movable by N.
	 * 
	 * <p>
	 * modifies: this
	 * <p>
	 * effects: returns integer value by which the Locations in this Map may be
	 * moved.
	 * 
	 * @return a non-negative 'int' value by which the Locations in this Map may
	 *         be moved.
	 */
	public int isMovableBy() {
		// This really comes down to a test of each LocationCluster
		// for this map. The return value is the largest multiple of
		// the largest Location.size() such that the multiple is
		// still less than the smallest offset (range) of any
		// Cluster. ie there must exist N blank bytes at the start of
		// the LogicalValue to shift down into and everything must be
		// shifted by an integral multiple of the largest
		// Location.size()
		LocationCluster firstCluster = clusters.iterator().next();
		int minLeftEmptyByteCount = firstCluster.getRange().getMin();
		int maxLocationSize = firstCluster.maxSize();
		for (LocationCluster cluster : clusters) {
			minLeftEmptyByteCount = Math.min(minLeftEmptyByteCount, cluster
					.getRange().getMin());
			maxLocationSize = Math.max(maxLocationSize, cluster.maxSize());
		}

		int maxMultiple = (int) Math.floor(((double) minLeftEmptyByteCount)
				/ ((double) maxLocationSize));

		return (maxLocationSize * maxMultiple);
	}

	/**
	 * Returns the number of trailing bytes of the absolute base location
	 * represented by this map that can be removed due to a lack of accesses.
	 * 
	 * @return a non-negative int
	 */
	public int trimmableBytes() {
		int maxRange = 0;

		for (LocationCluster cluster : clusters) {
			maxRange = Math.max(maxRange, cluster.getRange().getMax());
		}

		int fullSize = absoluteLoc.getInitialValue().getSize();

		return fullSize - maxRange;
	}

	/**
	 * Returns the maximum size of any Location that has been added to this map.
	 * Any call to move must be an integral multiple of this value.
	 * 
	 * @return a non-negative 'int'
	 */
	public int maxSize() {
		int maxSize = 0;

		for (LocationCluster cluster : clusters) {
			maxSize = Math.max(maxSize, cluster.maxSize());
		}

		return maxSize;
	}

	/**
	 * Performs a non-destructive move of this Map by generating a new
	 * LogicalValue in which all the non-used bytes which are overwritten by
	 * moving one or more Locations by 'delta' have been eliminated, and all
	 * remaining bytes have been shifted left accordingly. No change is actually
	 * made to any existing data structure. All trailing bytes which have no
	 * Location associated with them are removed regardless of the value of
	 * 'delta'.
	 * 
	 * @param delta
	 *            a non-negative 'int', the number of bytes to move each
	 *            Location represented in the Map by.
	 * @return a deep copy of the {@link LogicalValue} upon which this Map is
	 *         based, in which bytes have been eliminated and remaining bytes
	 *         have been shifted.
	 * @throws IllegalArgumentException
	 *             if 'delta' is not an integral multiple of getSize().
	 * @throws NonRemovableRangeException
	 *             if the LogicalValue upon which this Map is based cannot be
	 *             modified by delta.
	 * 
	 */
	public LogicalValue move(int delta) throws NonRemovableRangeException {
		if ((delta % maxSize()) != 0) {
			throw new IllegalArgumentException(
					"requires delta to be an integral multiple of BaseLocationMap maxSize()");
		}

		LogicalValue copy = absoluteLoc.getInitialValue().copy();

		// Basically, by definition there must be 'delta' unused bytes
		// at offset 0 of the absolute base. Then all
		// LocationClusters can be simply shifted left by delta.
		// Clone the absolute base's LogicalValue then call
		// removeRange(0, delta).

		// truncate the initial value of non-accessed tail segment
		int trimBytes = trimmableBytes();
		if (trimBytes > 0) {
			int copySize = copy.getSize();
			// Both ends of removeRange are 0 offset and inclusive
			copy = copy.removeRange(copySize - trimBytes, copySize - 1);
		}

		// left shift the initial value of non-accessed memory by delta
		if (delta > 0) {
			copy = copy.removeRange(0, delta - 1);
		}

		return copy;
	}

	/**
	 * Builds one BaseLocationMap for each Allocation in the given memory,
	 * returning them in a Set view.
	 * 
	 * @param mem
	 *            a non-null 'LogicalMemory'
	 * @return a 'Set' of BaseLocationMap objects.
	 * @throws NullPointerException
	 *             if mem is null
	 */
	public static Set<BaseLocationMap> buildMaps(LogicalMemory mem) {
		Set<BaseLocationMap> baseLocationMaps = new HashSet<BaseLocationMap>();

		for (Iterator<Allocation> iter = mem.getAllocations().iterator(); iter
				.hasNext();) {
			Allocation absoluteBase = (Allocation) iter.next()
					.getAbsoluteBase();
			baseLocationMaps.add(new BaseLocationMap(absoluteBase));
		}

		for (BaseLocationMap blm : baseLocationMaps) {

			for (LValue lvalue : mem.getLValues()) {
				for (Location location : mem.getAccesses(lvalue)) {
					if (blm.getAbsoluteBase() == location.getAbsoluteBase()) {
						blm.addLocation(location);
					}
				}
			}
		}

		return baseLocationMaps;
	}

	@Override
	public String toString() {
		return "BaseLocationMap@" + Integer.toHexString(hashCode()) + ":"
				+ absoluteLoc + "=>" + clusters;
	}

}// BaseLocationMap

