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
package org.xronos.openforge.lim;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * AndLatency represents the latency of a scoreboard-style combination of input
 * latencies. That is, it is the latency that is always the maximum of any of
 * its input latencies.
 * <P>
 * Each Latency is associated with a key Object that can differentiate the use
 * of that Latency when accessed as a component in a different OrLatency.
 * 
 * @version $Id: AndLatency.java 127 2006-04-03 16:47:38Z imiller $
 */
class AndLatency extends Latency {

	/** Set of constituent Latency objects */
	private Set<Latency> latencies;
	/**
	 * Cached state for the isOpen method. Because latency objects are immutable
	 * a latency will be either open or not open for its lifetime
	 */
	private boolean openState;

	public Set<Latency> __getSet() {
		return latencies;
	}

	@Override
	public boolean isOpen() {
		return openState;
	}

	@Override
	public boolean isGT(Latency latency) {
		for (Latency next : latencies) {
			if (next.isGT(latency)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean isGE(Latency latency) {
		if (equals(latency)) {
			return true;
		}

		if (latency instanceof AndLatency) {
			// Two special cases if we are testing against another
			// AndLatency. If this latency contains (at a minimum)
			// all the testLatency constituents, then we are
			// guaranteed to be at least GE.
			// The second case is similar, but a bit more generic
			// (could only test for 2nd case). If every latency in
			// the test latency is an ancestor of one or more
			// latencies in this context then we are similarly
			// guaranteed to be at least GE.
			if (latencies.containsAll(((AndLatency) latency).latencies)) {
				return true;
			}

			// Same test but using isDescendentOf. ie if all of the
			// constituent parts of latency are ancestors of some part
			// of this.latencies then isGE is true.
			boolean allCovered = true;
			for (Latency testLat : ((AndLatency) latency).latencies) {
				boolean coverFound = false;
				for (Latency localLat : latencies) {
					if (localLat.isDescendantOf(testLat)) {
						coverFound = true;
						break;
					}
				}
				allCovered &= coverFound;
			}
			if (allCovered)
				return true;
		}

		for (Latency next : latencies) {
			if (next.isGE(latency)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Returns false, and latencies are never fixed.
	 * 
	 * @return false
	 */
	@Override
	public boolean isFixed() {
		return false;
	}

	/**
	 * Gets the result of adding the whole of this latency to a given latency.
	 * Used by Modules to compute their output latency.
	 * 
	 * @param latency
	 *            the latency at the input to the Module
	 * @return the latency at the output of the Module
	 */
	@Override
	public Latency addTo(Latency latency) {
		Set<Latency> newLatencies = new HashSet<Latency>(latencies.size());
		for (Latency next : latencies) {
			newLatencies.add(next.addTo(latency));
		}
		return new AndLatency(newLatencies, getKey());
	}

	@Override
	public boolean equals(Object object) {
		if (object == this) {
			return true;
		} else if (object instanceof AndLatency) {
			AndLatency latency = (AndLatency) object;
			// return latencies.equals(latency.latencies) &&
			// getKey().equals(latency.getKey());
			// In an AND latency, the source is irrelevant in
			// determining equality. This is because regardless of
			// the source (and/or decisions made to traverse that
			// source) the runtime clock ticks will always match
			// between two AND latencies with the same constituents.
			// Thus, for the purposes of equality the key may be
			// safely ignored. Also in hashCode below.
			return latencies.equals(latency.latencies);
		} else {
			return false;
		}
	}

	@Override
	public int hashCode() {
		// See note in equals method.
		// return latencies.hashCode() + getKey().hashCode();
		return latencies.hashCode();
	}

	@Override
	boolean isDescendantOf(Latency latency) {
		for (Latency next : latencies) {
			if (next.isDescendantOf(latency)) {
				return true;
			}
		}
		return false;
	}

	@Override
	protected Latency increment(int minClocks, int maxClocks) {
		Set<Latency> newLatencies = new HashSet<Latency>(latencies.size());
		for (Latency next : latencies) {
			newLatencies.add(next.increment(minClocks, maxClocks));
		}
		return new AndLatency(newLatencies, getKey());
	}

	@Override
	protected Latency increment(int minClocks, LatencyKey key) {
		Set<Latency> newLatencies = new HashSet<Latency>(latencies.size());
		for (Latency next : latencies) {
			newLatencies.add(next.increment(minClocks, key));
		}
		// The key for the AndLatency is the current key. The 'open'
		// key is factored into each of the constituent latencies
		return new AndLatency(newLatencies, getKey());
	}

	AndLatency(Set<Latency> lats, LatencyKey key) {
		super(getMinClocks(lats), getMaxClocks(lats), key);

		latencies = flatten(lats);

		// Cache the 'open' state
		openState = false;
		for (Latency next : latencies) {
			if (next.isOpen()) {
				openState = true;
			}
		}
	}

	AndLatency(Latency l1, Latency l2, LatencyKey key) {
		this(createSet(l1, l2), key);
	}

	private static Set<Latency> createSet(Latency val1, Latency val2) {
		Set<Latency> set = new HashSet<Latency>(3);
		set.add(val1);
		set.add(val2);
		return set;
	}

	/**
	 * Convert any 'nested' AndLatencies into a flattened AndLatency structure
	 * to improve runtime performance (many fewer instantiations of iterators).
	 * 
	 * @param latencies
	 *            a Set of Latency objects
	 * @return a flattened Set of Latency objects
	 */
	private static Set<Latency> flatten(Set<Latency> lats) {
		final Set<Latency> newSet = new HashSet<Latency>();
		for (Latency next : lats) {
			if (next instanceof AndLatency) {
				newSet.addAll(((AndLatency) next).latencies);
			} else {
				newSet.add(next);
			}
		}
		return newSet;
	}

	private static int getMinClocks(Collection<Latency> lats) {
		boolean minValid = false;
		int min = 0;
		for (Latency latency : lats) {
			int clocks = latency.getMinClocks();
			min = minValid ? Math.max(min, clocks) : clocks;
			minValid = true;
		}
		return min;
	}

	private static int getMaxClocks(Collection<Latency> lats) {
		boolean maxValid = false;
		int max = 0;
		for (Latency latency : lats) {
			int clocks = latency.getMaxClocks();
			if (clocks == Latency.UNKNOWN) {
				return clocks;
			}
			max = maxValid ? Math.max(max, clocks) : clocks;
			maxValid = true;
		}
		return max;
	}

	@Override
	public String toString() {
		String ret = "AndLatency<" + getKey() + "> {";
		for (Iterator<Latency> iter = latencies.iterator(); iter.hasNext();) {
			ret += iter.next();

			if (iter.hasNext()) {
				ret += ", ";
			}
		}
		ret += "}";
		return ret;
	}

	/**
	 * Returns a semi-shallow clone of this latency object in which the map of
	 * latencies has been cloned, but the latencies contained in the map have
	 * not been cloned.
	 * 
	 * @return an Object of type AndLatency
	 * @exception CloneNotSupportedException
	 *                if an error occurs
	 */
	@Override
	public Object clone() throws CloneNotSupportedException {
		AndLatency clone = (AndLatency) super.clone();

		clone.latencies = new HashSet<Latency>();

		for (Latency lat : latencies) {
			clone.latencies.add(lat);
		}

		return clone;
	}

}
