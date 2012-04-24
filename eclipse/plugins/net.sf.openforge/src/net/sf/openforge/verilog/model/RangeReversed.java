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

package net.sf.openforge.verilog.model;

/**
 * RangeReversed provides a bit select in the 'inverted' form, where the lsb is
 * listed first, as in [7] or [0:31]. The only difference in this class from
 * {@link Range} is in the lexicalify method which reversed the ordering of the
 * lsb/msb values.
 * 
 * 
 * <p>
 * Created: Fri Aug 6 12:29:40 2004
 * 
 * @author imiller, last modified by $Author: imiller $
 * @version $Id: RangeReversed.java 2 2005-06-09 20:00:48Z imiller $
 */
public class RangeReversed extends Range {

	/**
	 * A Range based on width presumes to run from (width - 1) to 0. If the
	 * width is zero, the result is a bit-select of bit 0.
	 * 
	 */
	public RangeReversed(int width) {
		super(width);
	}

	/**
	 * A Range from msb to lsb, producing a part-select if msb is greater than
	 * lsb, or a bit select if msb is equal to lsb.
	 */
	public RangeReversed(int msb, int lsb) {
		super(msb, lsb);
	}

	public Lexicality lexicalify() {
		Lexicality lex = new Lexicality();

		lex.append(Symbol.OPEN_SQUARE);
		lex.append(new Constant(getLSB()));
		if (getMSB() != getLSB()) {
			lex.append(Symbol.COLON);
			lex.append(new Constant(getMSB()));
		}
		lex.append(Symbol.CLOSE_SQUARE);

		return lex;
	} // lexicalify()

}// RangeReversed
