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

package net.sf.openforge.forge.library.ops;

public class MUL {

	/**
	 * Implements a basic iterative multiplier. The multiply takes 32 cycles to
	 * complete and implements a full 32x32=32 bit signed multiply as specified
	 * in the JVM specification.
	 */
	public static int mult(int a, int b) {
		int result = 0;

		int shiftA = a;
		int shiftB = b;

		for (int i = 0; i < 32; i++) {
			if ((shiftB & 0x1) != 0) {
				result = result + shiftA;
			}

			shiftB = shiftB >>> 1;
			shiftA = shiftA << 1;
		}

		return result;
	}

	/**
	 * Implements a basic long iterative multiplier. The multiply takes 64
	 * cycles to complete and implements a full 64x64=64 bit signed multiply as
	 * specified in the JVM specification for a long valuemultiplier
	 */
	public static long mult(long a, long b) {
		long result = 0;

		long shiftA = a;
		long shiftB = b;

		for (int i = 0; i < 64; i++) {
			if ((shiftB & 0x1) != 0) {
				result = result + shiftA;
			}

			shiftB = shiftB >>> 1;
			shiftA = shiftA << 1;
		}

		return result;
	}

}
