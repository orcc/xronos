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
 * A Latency whose minimum and maximum number of clocks are known.
 * 
 * @author Stephen Edwards
 * @version $Id: AbsoluteLatency.java 566 2008-03-31 17:17:36Z imiller $
 */
class AbsoluteLatency extends Latency implements Cloneable {

	/** An Object that can be used to differentiate this latency */
	// private Object key;

	@Override
	public boolean isOpen() {
		return false;
	}

	@Override
	public boolean isGT(Latency latency) {
		// An absolute latency cannot descend from an open latency. There
		// no way for this latency to be GT the open latency.
		if (latency.isOpen()) {
			return false;
		} else {
			return getMinClocks() > latency.getMaxClocks();
		}
	}

	@Override
	public boolean isGE(Latency latency) {
		if (latency.isOpen()) {
			return false;
		} else if (equals(latency)) {
			return true;
		} else {
			return getMinClocks() >= latency.getMaxClocks();
		}
	}

	/**
	 * Returns true if the min clocks and max clocks are equal.
	 * 
	 * @return a value of type 'boolean'
	 */
	@Override
	public boolean isFixed() {
		return (getMinClocks() == getMaxClocks());
	}

	@Override
	public Latency addTo(Latency latency) {
		return latency.increment(getMinClocks(), getMaxClocks());
	}

	@Override
	public boolean equals(Object object) {
		if (object instanceof AbsoluteLatency) {
			AbsoluteLatency latency = (AbsoluteLatency) object;
			return (getMinClocks() == latency.getMinClocks()
					&& getMaxClocks() == latency.getMaxClocks() && getKey() == latency
						.getKey());
		} else {
			return false;
		}
	}

	@Override
	public int hashCode() {
		// return getMinClocks() + getMaxClocks() + (getKey() == null ? 0 :
		// getKey().hashCode());
		return getMinClocks() + getMaxClocks() + getKey().hashCode();
	}

	@Override
	public String toString() {
		return "AbsLat{" + getMinClocks() + "," + getMaxClocks() + "}";
	}

	AbsoluteLatency(int minClocks, int maxClocks, LatencyKey key) {
		super(minClocks, maxClocks, key);
		// this.key = key;
	}

	AbsoluteLatency(int minClocks, int maxClocks) {
		this(minClocks, maxClocks, LatencyKey.BASE);
	}

	AbsoluteLatency(int clocks) {
		this(clocks, clocks, null);
	}

	AbsoluteLatency() {
		this(0);
	}

	@Override
	boolean isDescendantOf(Latency latency) {
		return false;
	}

	@Override
	protected Latency increment(int minClocks, int maxClocks) {
		if (minClocks == 0 && maxClocks == 0) {
			return this;
		} else {
			return new AbsoluteLatency(getMinClocks() + minClocks,
					getMaxClocks() + maxClocks);
		}
	}

	@Override
	protected Latency increment(int minClocks, LatencyKey key) {
		return new OpenLatency(this, key, minClocks);
	}

	// private Object getKey ()
	// {
	// return key;
	// }

	/**
	 * Returns a clone of this latency object.
	 * 
	 * @return an Object of type AbsoluteLatency
	 * @exception CloneNotSupportedException
	 *                if an error occurs
	 */
	@Override
	public Object clone() throws CloneNotSupportedException {
		if (this == Latency.ZERO || this == Latency.ONE) {
			return this;
		}
		return super.clone();
	}

}
