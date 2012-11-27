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
public class StatSimpleCounter extends Stat {

	/** DOCUMENT ME! */
	private long count = 0L;

	/**
	 * Creates a new StatSimpleCounter object. DOCUMENT ME!
	 * 
	 * @param name
	 *            DOCUMENT ME!
	 */
	public StatSimpleCounter(String name) {
		super(name, "Counter");
	}

	/**
	 * DOCUMENT ME!
	 * 
	 * @param l
	 *            DOCUMENT ME!
	 */
	public final void inc(long l) {
		count = count + l;
	}

	/**
	 * DOCUMENT ME!
	 */
	public final void inc() {
		count++;
	}

	/**
	 * DOCUMENT ME!
	 * 
	 * @param token
	 *            DOCUMENT ME!
	 * @param l
	 *            DOCUMENT ME!
	 */
	public final void inc(Object token, long l) {
		count = count + l;
		addToken(token);
	}

	/**
	 * DOCUMENT ME!
	 * 
	 * @param token
	 *            DOCUMENT ME!
	 */
	public final void inc(Object token) {
		count++;
		addToken(token);
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
		return getName() + ": " + count;
	}
}
