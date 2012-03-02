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
package net.sf.openforge.verilog.pattern;

import java.util.Collection;

import net.sf.openforge.verilog.model.BinaryNumber;
import net.sf.openforge.verilog.model.Concatenation;
import net.sf.openforge.verilog.model.Expression;
import net.sf.openforge.verilog.model.Lexicality;
import net.sf.openforge.verilog.model.Net;
import net.sf.openforge.verilog.model.Replication;

/**
 * SignExtend is a design pattern which implements sign extension of a Net. It
 * will either present the sign extend as a concatenation of the replicated top
 * bit with the remainder of the net, or as a ranged net.
 * <P>
 * Example (in a context):<BR>
 * <CODE>
 * wire [7:0] a;
 * wire [31:0] b;
 * 
 * assign b = {{24{a[7]}}, a[7:0]};
 * 
 * </CODE>
 * <P>
 * Created: Tue Mar 12 11:32:04 2002
 * 
 * @author <a href="mailto:abk@ladd">Andy Kollegger</a>
 * @version $Id: SignExtend.java 2 2005-06-09 20:00:48Z imiller $
 */
public class SignExtend implements Expression {

	Expression extended;

	public SignExtend(Net base, int width) {
		int base_width = base.getWidth();

		if (width > base_width) {
			int extra_bits = width - base_width;

			Replication r = new Replication(extra_bits, base.getRange(
					base_width - 1, base_width - 1));

			Concatenation c = new Concatenation(new Expression[] { r,
					base.getFullRange() });

			extended = c;
		} else {
			extended = base.getRange(width - 1, 0);
		}

	} // SignExtend()

	/**
	 * Overloaded constructor with a dummy boolean parameter just serves the
	 * purpose for creating a unsigned extend
	 * 
	 * @param base
	 * @param width
	 * @param dummy
	 */
	public SignExtend(Net base, int width, boolean dummy) {
		int base_width = base.getWidth();

		if (width > base_width) {
			int extra_bits = width - base_width;

			Replication r = new Replication(extra_bits, new BinaryNumber(0, 1));

			Concatenation c = new Concatenation(new Expression[] { r,
					base.getFullRange() });

			extended = c;
		} else {
			extended = base.getRange(width - 1, 0);
		}

	} // SignExtend()

	/**
	 * 
	 * @return <description>
	 */
	public Lexicality lexicalify() {
		return extended.lexicalify();
	}

	/**
	 * 
	 * @return <description>
	 */
	public Collection getNets() {
		// TODO: implement this net.sf.openforge.verilog.model.Expression method
		return extended.getNets();
	}

	/**
	 * 
	 * @return <description>
	 */
	public int getWidth() {
		return extended.getWidth();
	}

	public String toString() {
		return lexicalify().toString();
	}

} // class SignExtend
