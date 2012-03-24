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

public class REM {

	/**
	 * Implements a functionally correct signed remainder(%) according to the
	 * JVM specification. When forging this code constant propagation must be
	 * turned on and, optionally, loop unrolling. If loop unrolling is turned
	 * on, then this method may be balance pipeline scheduled.
	 */
	public static int rem(int num, int den) {
		// If true, then the result needs to be negative.
		boolean flipResult = false;

		if (num < 0) {
			num = -num;
			flipResult = true;
		}

		if (den < 0) {
			den = -den;
		}

		int remainder = num;
		// Cast the denominator to a long so we can use 33 bits. This gets us
		// around the MIN_INT problem since 33 bits can represent -(MIN_INT).
		long denom = den & 0xFFFFFFFFL;

		for (int i = 0; i < 32; i++) {
			// Cast the numerator to a long so we can use 33 bits. This gets us
			// around the MIN_INT problem since 33 bits can represent
			// -(MIN_INT).
			long numer = (remainder >>> (31 - i)) & 0xFFFFFFFFL;

			if (numer >= denom) {
				remainder = (remainder - (den << (31 - i)));
			}

		}

		// If the numerator was negative, make the remainder negative.
		if (flipResult) {
			remainder = -remainder;
		}

		return remainder;
	}

	/**
	 * A fully functional impelentation of a 'long' (64 bit) signed remainder
	 * (%). This implementation conforms to the JVM specification for long
	 * remainder.
	 */
	public static long rem(long num, long den) {
		// If the numerator is negative then negate the result.
		boolean flipResult = false;

		if (num < 0) {
			num = -num;
			flipResult = true;
		}

		if (den < 0) {
			den = -den;
		}

		long remainder = num;

		// Split the denominator into 2 halves so that we can
		// correctly represent -(MIN_LONG) as a positive value.
		long upperDen = den >>> 32;
		long lowerDen = den & 0x00000000FFFFFFFFL;

		for (int i = 0; i < 64; i++) {
			long numer = remainder >>> (63 - i);

			// Split the numerator into 2 halves so that we can
			// correctly represent -(MIN_LONG) as a positive value.
			long upperNum = numer >>> 32;
			long lowerNum = numer & 0x00000000FFFFFFFFL;

			if ((upperNum > upperDen)
					|| ((upperNum == upperDen) && (lowerNum >= lowerDen))) {
				remainder = (remainder - (den << (63 - i)));
			}

		}

		// If the numerator was negative then negate the remainder.
		if (flipResult) {
			remainder = -remainder;
		}

		return remainder;
	}
}
