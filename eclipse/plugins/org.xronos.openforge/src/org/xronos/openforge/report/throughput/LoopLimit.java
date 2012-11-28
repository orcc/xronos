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

package org.xronos.openforge.report.throughput;

import java.io.PrintStream;

import org.xronos.openforge.lim.Latency;
import org.xronos.openforge.lim.Loop;
import org.xronos.openforge.util.naming.ID;


/**
 * LoopLimit reflects the limitation on throughput imposed by a loop in the
 * task. The limitation is equal to the latency of the loop
 */
public class LoopLimit implements ThroughputLimit {
	private Loop resource;
	private ID location;

	public LoopLimit(Loop loop, ID loc) {
		resource = loop;
		location = loc;
	}

	/**
	 * Returns the maximum number of cycles that you must wait before asserting
	 * new data to the loop being characterized by this instance.
	 */
	@Override
	public int getLimit() {
		Latency lat = resource.getLatency();
		int limit = (lat == null) ? -1 : lat.getMaxClocks();
		return limit;
	}

	/**
	 * Writes a report string to the stream indicated detailing the constraints
	 * that this loop puts on throughput.
	 */
	@Override
	public void writeReport(PrintStream ps, int tabDepth) {
		// IDSourceInfo info = location.getIDSourceInfo();
		ps.println("Resource: " + resource.showIDLocation()
				+ " in method/function '" + location.showIDLogical() + "'");
		final int limit = getLimit();
		String string = (limit < 0) ? "indeterminate" : Integer.toString(limit);
		ps.println("\tclocks: " + string);
	}

	@Override
	public String toString() {
		return "LoopLimit for " + resource + " " + getLimit() + " clocks";
	}
}
