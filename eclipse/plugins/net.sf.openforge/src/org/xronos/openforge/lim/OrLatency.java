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
 * OrLatency consists of two or more Latencies that represent a choice of
 * possibilities. Typically such a Latency would result from the merging of
 * control paths, each of which had a different latency.
 * <P>
 * Each Latency is associated with a key Object that can differentiate the use
 * of that Latency when accessed as a component in a different OrLatency.
 * 
 * @author Stephen Edwards
 * @version $Id: OrLatency.java 109 2006-02-24 18:10:34Z imiller $
 */
class OrLatency extends Latency implements Cloneable {

	/** Set of constituent Latency objects */
	private Set<Latency> latencies;
	/**
	 * Cached state for the isOpen method. Because latency objects are immutable
	 * a latency will be either open or not open for its lifetime
	 */
	private boolean openState;

	@Override
	public boolean isOpen() {
		return openState;
	}

	@Override
	public boolean isGT(Latency latency) {
		for (Latency next : latencies) {
			if (!next.isGT(latency)) {
				return false;
			}
		}
		return true;
	}

	@Override
	public boolean isGE(Latency latency) {
		if (equals(latency)) {
			return true;
		}

		for (Latency next : latencies) {
			if (!next.isGE(latency)) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Returns false, or latencies are never fixed.
	 * 
	 * @return false
	 */
	@Override
	public boolean isFixed() {
		return false;
	}

	@Override
	public Latency addTo(Latency latency) {
		Set<Latency> newSet = new HashSet<Latency>(latencies.size());
		for (Latency next : latencies) {
			newSet.add(next.addTo(latency));
		}
		return new OrLatency(newSet, getKey());
	}

	@Override
	public boolean equals(Object object) {
		if (object == this) {
			return true;
		} else if (object instanceof OrLatency) {
			OrLatency latency = (OrLatency) object;
			return latencies.equals(latency.latencies)
					&& getKey().equals(latency.getKey());
		} else {
			return false;
		}
	}

	@Override
	public int hashCode() {
		return latencies.hashCode() + getKey().hashCode();
	}

	@Override
	boolean isDescendantOf(Latency latency) {
		for (Latency next : latencies) {
			if (!next.isDescendantOf(latency)) {
				return false;
			}
		}
		return true;
	}

	@Override
	protected Latency increment(int minClocks, int maxClocks) {
		Set<Latency> newSet = new HashSet<Latency>(latencies.size());
		for (Latency next : latencies) {
			newSet.add(next.increment(minClocks, maxClocks));
		}
		return new OrLatency(newSet, getKey());
	}

	@Override
	protected Latency increment(int minClocks, LatencyKey key) {
		Set<Latency> newSet = new HashSet<Latency>(latencies.size());
		for (Latency next : latencies) {
			newSet.add(next.increment(minClocks, key));
		}
		// The key for the OrLatency is the current key. The 'open'
		// key is factored into each of the constituent latencies
		return new OrLatency(newSet, getKey());
	}

	OrLatency(Set<Latency> lats, LatencyKey key) {
		super(getMinClocks(lats), getMaxClocks(lats), key);

		latencies = flatten(lats);

		// Cache the 'open' state
		openState = false;
		for (Latency latency : latencies) {
			if (latency.isOpen()) {
				openState = true;
			}
		}
	}

	OrLatency(Latency l1, Latency l2, LatencyKey key) {
		this(createSet(l1, l2), key);
	}

	private static Set<Latency> createSet(Latency l1, Latency l2) {
		Set<Latency> set = new HashSet<Latency>(3);
		set.add(l1);
		set.add(l2);
		return set;
	}

	private static Set<Latency> flatten(Set<Latency> lats) {
		final Set<Latency> newSet = new HashSet<Latency>();
		for (Latency next : lats) {
			if (next instanceof OrLatency) {
				newSet.addAll(((OrLatency) next).latencies);
			} else {
				newSet.add(next);
			}
		}
		return newSet;
	}

	private static int getMinClocks(Collection<Latency> latencies) {
		boolean minValid = false;
		int min = 0;
		for (Latency latency : latencies) {
			int clocks = latency.getMinClocks();
			min = minValid ? Math.min(min, clocks) : clocks;
			minValid = true;
		}
		return min;
	}

	private static int getMaxClocks(Collection<Latency> latencies) {
		boolean maxValid = false;
		int max = 0;
		for (Latency latency : latencies) {
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
		String ret = "OrLatency<" + getKey() + "> {";
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
	 * @return an Object of type OrLatency
	 * @exception CloneNotSupportedException
	 *                if an error occurs
	 */
	@Override
	public Object clone() throws CloneNotSupportedException {
		OrLatency clone = (OrLatency) super.clone();

		clone.latencies = new HashSet<Latency>();

		for (Latency latency : latencies) {
			clone.latencies.add(latency);
		}

		return clone;
	}

}
