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

package org.xronos.openforge.util.stats;

/**
 * Simple Counter
 */
public class StatThresholdCounter extends Stat {

	/** DOCUMENT ME! */
	static final String rcs_id = "RCS_REVISION: $Rev: 2 $";
	private long count = 0L;
	private long threshold = 1L;
	private long threshCount = 0L;

	/**
	 * Creates a new StatThresholdCounter object. DOCUMENT ME!
	 * 
	 * @param name
	 *            DOCUMENT ME!
	 * @param threshold
	 *            DOCUMENT ME!
	 */
	public StatThresholdCounter(String name, long threshold) {
		super(name, "Threshold[" + threshold + "]");
		this.threshold = threshold;
	}

	/**
	 * DOCUMENT ME!
	 * 
	 * @param l
	 *            DOCUMENT ME!
	 */
	public void inc(long l) {
		threshCount = threshCount + l;
		while (threshCount >= threshold) {
			count++;
			threshCount = threshCount - threshold;
		}
	}

	/**
	 * DOCUMENT ME!
	 */
	public void inc() {
		inc(1L);
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
	@Override
	public String toString() {
		return getName() + ": " + count + "(+" + threshCount + ")";
	}
}
