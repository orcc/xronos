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
/* $Rev: 2 $ */

package net.sf.openforge.forge.api.timing;

/**
 * The <code>Timing</code> class contains API primitive method calls for
 * defining user-specified timing information to a design.
 */

public class Timing {
	/**
	 * Wait for all instructions before this call to complete before continuing
	 * with the following instructions. The idea is to synchronize all parallel
	 * executing paths that the scheduler has created prior to this
	 * <code>waitSync()</code> call.
	 */
	public static void waitSync() {
	}

	/**
	 * Wait for all instructions before this call to complete and then waits for
	 * the next clock edge before continuing with the following instructions.
	 * All parallel executing paths are made to synchronize at this point before
	 * being allowed to continue processing.
	 */
	public static void waitClock() {
	}

	/**
	 * Wait for all instructions before this call to complete before continuing
	 * with the following instructions, only within the scope of the current
	 * method. The idea is to synchronize all parallel executing paths that the
	 * scheduler has created prior to this <code>waitSyncLocal()</code> call.
	 */
	public static void waitSyncLocal() {
	}

	/**
	 * Wait for all instructions before this call to complete and then waits for
	 * the next clock edge before continuing with the following instructions,
	 * only within the scope of the current method. All parallel executing paths
	 * are made to synchronize at this point before being allowed to continue
	 * processing.
	 */
	public static void waitClockLocal() {
	}

	/**
	 * Wait for at least the specified clock cycle(s) after calling the
	 * enclosing method before calling the method again. This value is used by
	 * the scheduler to determine how often data can sent into a given method,
	 * possibly before the previous result is generated if the method is
	 * pipelined. Normal code that is being Forged will not use this method, it
	 * is intended to provide scheduling hints for methods that access external
	 * devices such as IPCores or Pins on the design.
	 * 
	 * @param i
	 *            number of clock cycle(s) to wait after calling method before
	 *            another call can be made
	 */
	public static void throughputLocal(int i) {
	}
}
