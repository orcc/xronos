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

package net.sf.openforge.report.throughput;

import java.io.PrintStream;

/**
 * ThroughputLimit is an interface used by classes which track any limitation on
 * how often new data can be applied to a given task.
 * 
 * <p>
 * Created: Thu Jan 30 09:46:19 2003
 * 
 * @author imiller, last modified by $Author: imiller $
 * @version $Id: ThroughputLimit.java 2 2005-06-09 20:00:48Z imiller $
 */
public interface ThroughputLimit {
	/**
	 * Retrieves the minimum clocks that you must wait between consecutive
	 * assertions of the GO because of a limitation imposed by an instance of
	 * this class.
	 */
	public int getLimit();

	/**
	 * Writes reporting information to the given stream based on the throughput
	 * limitation tracked here.
	 */
	public void writeReport(PrintStream ps, int tabDepth);

	@Override
	public String toString();

}// ThroughputLimit
