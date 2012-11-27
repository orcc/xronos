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
package org.xronos.openforge.verilog.model;

import java.util.Collection;
import java.util.HashSet;

/**
 * A BaseNumber expression presents a fully qualified representation of a
 * constant, including the constant's size and explicit base.
 * 
 * <P>
 * Ex., <BR>
 * 4'b1011 <BR>
 * 16'hFACE
 * <P>
 * Created: Wed Feb 28 2001
 * 
 * @author abk
 * @version $Id: BaseNumber.java 2 2005-06-09 20:00:48Z imiller $
 */
public class BaseNumber implements VerilogElement, Expression {

	private Constant number;

	public BaseNumber(Constant number) {
		this.number = number;

	} // BaseNumber()

	public BaseNumber(long l, int size) {
		this(new Constant(l, size));
	} // BaseNumber()

	@Override
	public int getWidth() {
		return number.getSize();
	}

	@Override
	public Collection<Net> getNets() {
		return new HashSet<Net>(1);
	}

	/**
	 * 
	 * @return the tokens comprising this base number
	 */
	@Override
	public Lexicality lexicalify() {
		Lexicality lex = new Lexicality();

		lex.append(new Constant(number.getSize()));

		switch (number.getRadix()) {
		case Constant.DECIMAL:
			lex.append(Symbol.DECIMAL_BASE);
			break;
		case Constant.HEX:
			lex.append(Symbol.HEX_BASE);
			break;
		case Constant.OCTAL:
			lex.append(Symbol.OCTAL_BASE);
			break;
		case Constant.BINARY:
			lex.append(Symbol.BINARY_BASE);
			break;
		}

		lex.append(number);

		return lex;

	} // lexicalify()

	@Override
	public String toString() {
		return lexicalify().toString();
	}

} // end of class BaseNumber
