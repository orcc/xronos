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

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Latency is a period of time in clock ticks. It is used to measure the
 * realtive durations of hardware execution times. It is characterized by two
 * values: a minimum number of clocks and a maximum number of clocks. For
 * entities whose execution time is known precisely, these two values will be
 * the same. A Latency may also be <i>open</i>, in which case the value of the
 * maximum number of clocks is unknown, and is represented by the constant
 * UNKNOWN.
 * <P>
 * Latencies may be compared for equality. A Latency may also be compared with
 * respect to whether it is greater than another Latency or not. Note that for
 * two latencies M and N, if M is not greater than or equal to N, it
 * <em>does not</em> imply that N is greater than M.
 * <P>
 * Two common Latencies, ZERO and ONE, are provided; these represent a latency
 * of 0 clocks and 1 clock, respectively. Other base Latencies may be created by
 * using one of the methods {@link Latency#get(int) get(clocks)} or
 * {@link Latency#get(int,int) get(min,max)}.
 * <P>
 * An open Latency (one whose maximum number of clocks is unknown) may be
 * created by calling the method {@link Latency#open(LatencyKey)
 * open(LatencyKey)} on an existing Latency. The LatencyKey argument is used to
 * distinguish this open Latency from other open Latencies that have the same
 * base latency.
 * <P>
 * The result of incrementing a Latency by another Latency is obtained with the
 * {@link Latency#addTo(Latency) addTo(Latency)} method.
 * <P>
 * Finally, a measure consisting of a number of possible Latencies can be
 * represented by using the {@link Latency#or(Set) or(Set)} method.
 * <P>
 * Latency is implemented as an abstract class with several tightly coupled,
 * concrete subclasses: {@link AbsoluteLatency}, {@link OpenLatency},
 * {@link AndLatency}, and {@link OrLatency}.
 * 
 * @author Stephen Edwards
 * @version $Id: Latency.java 109 2006-02-24 18:10:34Z imiller $
 */
public abstract class Latency implements Cloneable {

	/** An unknown number of clocks */
	public static final int UNKNOWN = -1;

	/** 0 clocks (i.e. combinational) */
	public static final Latency ZERO;

	/** 1 clock */
	public static final Latency ONE;

	/** The non-negative minimum number of clocks */
	private final int minClocks;

	/** The maximum number of clocks, or UNKNOWN */
	private final int maxClocks;

	/** The uniquifying key, if needed. */
	private LatencyKey key = LatencyKey.BASE;

	/**
	 * Gets the Latency that represents an exact number of clocks.
	 * 
	 * @param clocks
	 *            a non-negative number of clocks
	 */
	public static Latency get(int clocks) {
		if (clocks < 0) {
			throw new IllegalArgumentException("clocks: " + clocks);
		}
		return get(clocks, clocks);
	}

	/**
	 * Gets the Latency that represents a known range of clocks.
	 * 
	 * @param minClocks
	 *            a non-negative number of clocks
	 * @param maxClocks
	 *            a non-negative number of clocks that is at least minClocks
	 */
	public static Latency get(int minClocks, int maxClocks) {
		if (minClocks < 0 || maxClocks < 0 || maxClocks < minClocks) {
			throw new IllegalArgumentException("clocks: " + minClocks + ","
					+ maxClocks);
		}
		return ZERO.increment(minClocks, maxClocks);
	}

	/**
	 * Gets the Latency that represents two or more possible clock ranges.
	 * 
	 * @param set
	 *            a set of Latency objects
	 */
	public static Latency or(Set<Latency> set, LatencyKey key) {
		if (set.size() == 1) {
			return set.iterator().next();
		} else {
			return new OrLatency(set, key);
		}
	}

	/**
	 * Gets the Latency that represents the latest of two or more clock ranges.
	 * 
	 * @param set
	 *            a Set of Latency objects
	 */
	public static Latency and(Set<Latency> set, LatencyKey key) {
		if (set.size() == 1) {
			return set.iterator().next();
		} else {
			return new AndLatency(set, key);
		}
	}

	/**
	 * Returns the latest as defined by getLatest without any preferred entries.
	 * 
	 * @param inputMap
	 *            a value of type 'Map'
	 * @return a value of type 'Map'
	 */
	public static Map<Object, Latency> getLatest(Map<?, Latency> inputMap) {
		return getLatest(inputMap, Collections.emptySet());
	}

	/**
	 * Takes a Map of Object=>Latency pairings and returns a Map with only the
	 * latest (longest latency from zero) latencies (paired with their Objects).
	 * If all latencies are comparable, only the single longest latency will be
	 * returned in the Map (if several have the same 'longest' latency, only one
	 * will be returned). If any latencies are not comparable with the longest
	 * determined latency, then the returned map will contain those
	 * non-comparable latencies in addition to the latency identified as the
	 * 'longest'.
	 * 
	 * @param inputMap
	 *            a 'Map' of Object=>{@link Latency Latency} pairings
	 * @param preferred
	 *            a 'Set' of Objects indicating that if two latencies are equal
	 *            then the one in the 'preferred' set is to be kept. If both are
	 *            in the set, then one is selected randomly.
	 * @return a 'Map' of Object=>{@link Latency Latency} pairings.
	 */
	public static Map<Object, Latency> getLatest(Map<?, Latency> inputMap,
			Set<Object> preferred) {
		if (_lim.db)
			_lim.ln(_lim.LATENCY, "Getting latest latency from map: "
					+ inputMap);
		if (_lim.db) {
			for (Map.Entry<?, Latency> entry : inputMap.entrySet()) {
				_lim.ln(_lim.LATENCY,
						"\t" + entry.getKey() + "=>" + entry.getValue());
			}
		}

		// latestMap is a latency->object map
		Map<Latency, Object> latestMap = new HashMap<Latency, Object>(
				inputMap.size());
		for (Map.Entry<?, Latency> inputEntry : inputMap.entrySet()) {
			Latency latency = inputEntry.getValue();
			Object value = inputEntry.getKey();
			List<Latency> toTrash = new LinkedList<Latency>();
			boolean addCurrent = false;

			if (_lim.db)
				_lim.ln(_lim.LATENCY, "Testing: " + latency);
			for (Map.Entry<Latency, Object> entry : latestMap.entrySet()) {
				Latency savedLatency = entry.getKey();
				assert savedLatency != null;
				Object savedValue = entry.getValue();
				if (_lim.db)
					_lim.ln(_lim.LATENCY, "Testing against saved: "
							+ savedLatency);

				if (latency.equals(savedLatency)) {
					// Any others in the saved map can't be compared
					// against so add this one and break;
					if (_lim.db)
						_lim.ln(_lim.LATENCY, "equal");
					if (preferred.contains(savedValue)) {
						// already in the latestMap.
					} else {
						addCurrent = true;
						toTrash.add(savedLatency);
					}
					break;
				}
				// else if (latency.isGT(savedLatency))
				else if (latency.isGE(savedLatency)) {
					// The new one is later than this saved one!
					// Trash the saved one and mark this one for
					// adding.
					addCurrent = true;
					toTrash.add(savedLatency);
					if (_lim.db)
						_lim.ln(_lim.LATENCY, "latency isGE savedLatency");
				}
				// else if (savedLatency.isGT(latency))
				else if (savedLatency.isGE(latency)) {
					// The saved one is later than the one being
					// tested. Mark the new one to NOT be added and
					// stop looking through the saved list.
					addCurrent = false;
					if (_lim.db)
						_lim.ln(_lim.LATENCY, "savedLatency isGE latency");
					break;
				} else {
					// Not comparable... have to save it unless it
					// compares to something else in the saved map.
					addCurrent = true;
					if (_lim.db)
						_lim.ln(_lim.LATENCY, "not comparable");
				}
			}

			for (Latency lat : toTrash) {
				latestMap.remove(lat);
			}

			if (addCurrent || latestMap.isEmpty()) {
				latestMap.put(latency, value);
			}
		}

		// turn the latency->object latestMap into an object->latency
		// map.
		Map<Object, Latency> retMap = new HashMap<Object, Latency>(
				latestMap.size());
		for (Map.Entry<Latency, Object> me : latestMap.entrySet()) {
			retMap.put(me.getValue(), me.getKey());
		}

		if (_lim.db)
			_lim.ln(_lim.LATENCY, "Return map");
		if (_lim.db) {
			for (Map.Entry<Object, Latency> entry : retMap.entrySet()) {
				_lim.ln(_lim.LATENCY,
						"\t" + entry.getKey() + "=>" + entry.getValue());
			}
		}

		return retMap;
	}

	/**
	 * Gets the Latency that is at least as great as this Latency but whose
	 * maximum number of clocks is not known.
	 * 
	 * @param key
	 *            an object that can be used to distinguish the resulting
	 *            Latency from other open latencies that may be created from
	 *            this Latency
	 */
	public Latency open(LatencyKey key) {
		return increment(0, key);
	}

	/**
	 * Gets the minimum number of clocks specified by this Latency.
	 * 
	 * @return a non-negative number of clocks
	 */
	public int getMinClocks() {
		return minClocks;
	}

	/**
	 * Gets the maximum number of clocks specified by this Latency.
	 * 
	 * @return a non-negative number of clocks, or UNKNOWN if this is an open
	 *         latency
	 */
	public int getMaxClocks() {
		return maxClocks;
	}

	protected LatencyKey getKey() {
		return key;
	}

	/**
	 * Returns true if this latency represents an exact number of clock cycles,
	 * or false if this latency represents multiple values or unknown values.
	 * 
	 * @return a value of type 'boolean'
	 */
	public abstract boolean isFixed();

	/**
	 * Tests whether this Latency is open or if its maximum number of clocks is
	 * known.
	 */
	public abstract boolean isOpen();

	/**
	 * Tests whether this latency is greater than a given latency. If false,
	 * does not imply that this latency must be less than or equal to the given
	 * latency.
	 */
	public abstract boolean isGT(Latency latency);

	/**
	 * Tests whether this latency is greater than or equal to a given latency.
	 * If false, does not imply that this latency must be less than the given
	 * latency.
	 */
	public boolean isGE(Latency latency) {
		return equals(latency) || isGT(latency);
	}

	/**
	 * Gets the result of adding the number of clocks represented by this
	 * latency to another latency.
	 * 
	 * @param latency
	 *            the latency to which this latency should be added
	 * @return the resulting latency
	 */
	public abstract Latency addTo(Latency latency);

	/**
	 * Tests whether this latency can be determined to be greater than a given
	 * latency due to the fact that it was derived from the given latency by one
	 * or more open() operations.
	 */
	abstract boolean isDescendantOf(Latency latency);

	/**
	 * Gets the result of incrementing this latency by a known number of clocks.
	 * 
	 * @param minClocks
	 *            the minimum clock increment
	 * @param maxClocks
	 *            the maximum clock increment
	 * @param key
	 *            an object that can be used to distinguish this Latency from
	 *            another with the same clock values
	 */
	protected abstract Latency increment(int minClocks, int maxClocks);

	/**
	 * Gets the result of incrementing this latency by a known number of minimum
	 * clocks and an unknown number of maximum clocks.
	 */
	protected abstract Latency increment(int minClocks, LatencyKey key);

	Latency(int minClocks, int maxClocks, LatencyKey key) {
		this.minClocks = minClocks;
		this.maxClocks = maxClocks;
		this.key = key;
	}

	static {
		ZERO = new AbsoluteLatency(0, 0);
		ONE = new AbsoluteLatency(1, 1);
	}

	@Override
	public Object clone() throws CloneNotSupportedException {
		return super.clone();
	}

}
