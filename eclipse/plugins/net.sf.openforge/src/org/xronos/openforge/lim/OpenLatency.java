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

/**
 * A Latency whose maximum number of clocks is unknown and is represented with
 * the constant value UNKNOWN (the value of an unknown minimum number of clocks
 * may be represented simply as 0).
 * <P>
 * OpenLatency is constructed as an offset from an existing base Latency, and
 * may include an increment to the minimum clock value. It includes an Object
 * key that is used to distinguish it from other open latencies that may have
 * the base latency and number of minimum clocks.
 * 
 * @author Stephen Edwards
 * @version $Id: OpenLatency.java 124 2006-03-31 17:24:55Z imiller $
 */
class OpenLatency extends Latency implements Cloneable {

	/** The latency from which this latency was created */
	private Latency base;

	/** An Object that can be used to differentiate this latency */
	// private Object key;

	@Override
	public boolean isOpen() {
		return true;
	}

	/**
	 * Gets the result of adding the number of clocks represented by the
	 * parameter latency to this open latency. The resulting latency is an open
	 * latency with the same key as this one. Any key of the parameter latency
	 * is ignored.
	 * 
	 * @param latency
	 *            the latency which is to be added to this latency
	 * @return the resulting latency
	 */
	@Override
	public Latency addTo(Latency latency) {
		return latency.increment(getMinClocks(), getKey());
	}

	@Override
	public boolean isGT(Latency latency) {
		if (latency.isOpen()) {
			return isDescendantOf(latency)
					&& (getMinClocks() > latency.getMinClocks());
		} else {
			return getMinClocks() > latency.getMaxClocks();
		}
	}

	@Override
	public boolean isGE(Latency latency) {
		if (latency.equals(this)) {
			return true;
		}

		if (latency.isOpen()) {
			return isDescendantOf(latency)
					&& (getMinClocks() >= latency.getMinClocks());
		} else {
			return getMinClocks() >= latency.getMaxClocks();
		}
	}

	/**
	 * Returns false, open latencies are never fixed.
	 * 
	 * @return false
	 */
	@Override
	public boolean isFixed() {
		return false;
	}

	@Override
	public boolean equals(Object object) {
		if (object instanceof OpenLatency) {
			OpenLatency latency = (OpenLatency) object;
			return (getMinClocks() == latency.getMinClocks()
					&& base.equals(latency.base) && getKey().equals(
					latency.getKey()));
		} else {
			return false;
		}
	}

	@Override
	public int hashCode() {
		return base.hashCode() + getKey().hashCode() + getMinClocks();
	}

	@Override
	public String toString() {
		return "OpenLat{Op minclks=" + getMinClocks() + " key=" + getKey()
				+ " base=" + base + "}";
	}

	OpenLatency(Latency base, LatencyKey key, int minClocks) {
		super(base.getMinClocks() + minClocks, UNKNOWN, key);
		this.base = base;
		// this.key = key;
	}

	OpenLatency(Latency base, LatencyKey key) {
		this(base, key, 0);
	}

	@Override
	boolean isDescendantOf(Latency latency) {
		return base.equals(latency) || base.isDescendantOf(latency);
	}

	@Override
	protected Latency increment(int minClocks, int maxClocks) {
		return new OpenLatency(this, getKey(), minClocks);
	}

	@Override
	protected Latency increment(int minClocks, LatencyKey key) {
		return new OpenLatency(this, key, minClocks);
	}

	/**
	 * Returns a shallow clone of this latency object in which the base latency
	 * and identifying object have not been cloned.
	 * 
	 * @return an Object of type OpenLatency
	 * @exception CloneNotSupportedException
	 *                if an error occurs
	 */
	@Override
	public Object clone() throws CloneNotSupportedException {
		return super.clone();
	}

}
