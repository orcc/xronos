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

import org.xronos.openforge.lim.Call;


/**
 * ThroughputLocalLimit reflects the limitation on throughput imposed by a user
 * specified limit in the task.
 */
public class ThroughputLocalLimit implements ThroughputLimit {
	private Call resource;

	public ThroughputLocalLimit(Call call) {
		resource = call;
	}

	/**
	 * Returns the maximum number of cycles that you must wait before asserting
	 * new data to the method being characterized by this instance.
	 */
	@Override
	public int getLimit() {
		int limit = resource.getThroughputLocal();
		return limit;
	}

	/**
	 * Writes a report string to the stream indicated detailing the constraints
	 * that this method call puts on throughput.
	 */
	@Override
	public void writeReport(PrintStream ps, int tabDepth) {
		ps.println("Resource: " + resource.showIDLogical()
				+ " method/function "
				+ resource.getProcedure().getBody().showIDLocation());
		final int limit = getLimit();
		String string = (limit < 0) ? "indeterminate" : Integer.toString(limit);
		ps.println("\tclocks: " + string);
	}

	@Override
	public String toString() {
		return "ThroughputLocalLimit for " + resource + " " + getLimit()
				+ " clocks";
	}
}
