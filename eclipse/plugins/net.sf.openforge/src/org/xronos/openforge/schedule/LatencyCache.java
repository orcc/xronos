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

package org.xronos.openforge.schedule;

import org.xronos.openforge.lim.Bus;
import org.xronos.openforge.lim.Component;
import org.xronos.openforge.lim.Latency;

/**
 * LatencyCache is a simple interface that provides methods that can be used to
 * obtain information about the latency of buses and components in a graph.
 * 
 * @version $Id: LatencyCache.java 88 2006-01-11 22:39:52Z imiller $
 */
public interface LatencyCache {
	/**
	 * Gets the latency of a Bus, which is the latency of its associated control
	 * Bus.
	 */
	public Latency getLatency(Bus bus);

	/**
	 * Gets the latency at the entry point of a Component, which is the the
	 * latency of the control Bus for the entry point.
	 */
	public Latency getLatency(Component component);

	/**
	 * Gets the control Bus for the entry point of a Component.
	 */
	public Bus getControlBus(Component component);

}
