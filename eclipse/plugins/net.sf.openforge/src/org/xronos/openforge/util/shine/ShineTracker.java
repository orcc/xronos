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

package org.xronos.openforge.util.shine;

class ShineTracker {
	static final String rcs_id = "RCS_REVISION: $Rev: 2 $";
	private int counter = 0;
	private int id = 0;

	ShineTracker() {
	}

	synchronized void inc() {
		counter++;
	}

	synchronized void dec() {
		counter--;
	}

	synchronized int getCounter() {
		return counter;
	}

	synchronized int getNextID() {
		return ++id;
	}

	synchronized void clear() {
		counter = 0;
	}
}
