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

package org.xronos.openforge.util.stats;

/**
 * Simple Distibution counter (min/max/avg)
 */
public class StatDistributionCounter extends Stat {

	/** DOCUMENT ME! */
	private long count;
	private float avg;
	private long min = Long.MAX_VALUE;
	private long max = Long.MIN_VALUE;

	/**
	 * Creates a new StatDistributionCounter object. DOCUMENT ME!
	 * 
	 * @param name
	 *            DOCUMENT ME!
	 */
	public StatDistributionCounter(String name) {
		super(name, "Distribution");
	}

	/**
	 * DOCUMENT ME!
	 * 
	 * @param l
	 *            DOCUMENT ME!
	 */
	public void update(long l) {
		avg = (avg * count) + l;
		avg = (avg / (++count));
		if (l < min) {
			min = l;
		}

		if (l > max) {
			max = l;
		}
	}

	/**
	 * DOCUMENT ME!
	 * 
	 * @return DOCUMENT ME!
	 */
	public long getCount() {
		return count;
	}

	/**
	 * DOCUMENT ME!
	 * 
	 * @return DOCUMENT ME!
	 */
	public long getAvg() {
		return (long) avg;
	}

	/**
	 * DOCUMENT ME!
	 * 
	 * @return DOCUMENT ME!
	 */
	public long getMin() {
		return min;
	}

	/**
	 * DOCUMENT ME!
	 * 
	 * @return DOCUMENT ME!
	 */
	public long getMax() {
		return max;
	}

	/**
	 * DOCUMENT ME!
	 * 
	 * @return DOCUMENT ME!
	 */
	@Override
	public String toString() {
		return getName() + ": Count " + getCount() + "/Avg " + getAvg()
				+ "/Min " + getMin() + "/Max" + getMax();
	}
}
