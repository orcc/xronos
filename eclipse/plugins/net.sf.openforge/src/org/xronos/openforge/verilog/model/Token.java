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

/**
 * Token is a verilog leaf element, an atomic object which has a direct string
 * representation.
 * 
 * <P>
 * 
 * Created: Wed Feb 07 2001
 * 
 * @author abk
 * @version $Id: Token.java 2 2005-06-09 20:00:48Z imiller $
 */
public abstract class Token implements VerilogElement {

	public static final int TYPE = -1;

	public Token() {
		super();
	}

	/**
	 * Gets the string representation of the token.
	 * 
	 * 
	 */
	public abstract String getToken();

	@Override
	public String toString() {
		return getToken();
	}

	@Override
	public int hashCode() {
		return getToken().hashCode();
	}

	@Override
	public boolean equals(Object o) {
		if (super.equals(o))
			return true;

		if (o instanceof Token) {
			return getToken().equals(((Token) o).getToken());
		}

		return false;
	}

	@Override
	public Lexicality lexicalify() {
		Lexicality lex = new Lexicality();

		lex.append(this);

		return lex;
	} // lexicalify()

	public int getType() {
		return TYPE;
	}

} // end class Token

