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
package net.sf.openforge.lim;

/**
 * LatencyKey is an interface implemented by any object which can uniquely
 * identify the 'source' of a latency. The latency source must be sufficeintly
 * unique to differentiate two otherwise identical latencies based on the
 * conditions upon which those latencies, during runtime, will evaluate to a
 * specific (exact) number of clock ticks.
 * 
 * @author imiller
 * @version $Id: LatencyKey.java 109 2006-02-24 18:10:34Z imiller $
 */
public interface LatencyKey {

	public static final LatencyKey BASE = new LatencyKey() {
		@Override
		public String toString() {
			return "BASE";
		}
	};
}
