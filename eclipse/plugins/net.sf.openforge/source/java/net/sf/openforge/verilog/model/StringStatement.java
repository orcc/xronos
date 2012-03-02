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

import java.util.Collection;
import java.util.Collections;

/**
 * StringToken.java
 * 
 * 
 * <p>
 * Created: Fri Aug 23 09:12:32 2002
 * 
 * @author imiller, last modified by $Author: imiller $
 * @version $Id: StringStatement.java 2 2005-06-09 20:00:48Z imiller $
 */
public class StringStatement implements Statement {

	private StringToken state;

	public StringStatement(String value) {
		super();
		this.state = new StringToken(value);
	}

	public Collection getNets() {
		return Collections.EMPTY_SET;
	}

	public Lexicality lexicalify() {
		Lexicality lex = new Lexicality();
		lex.append(Symbol.QUOTE);
		lex.append(state);
		lex.append(Symbol.QUOTE);
		return lex;
	}

	public String toString() {
		return lexicalify().toString();
	}

	public static final class StringToken extends Token {
		public static final int TYPE = 7;

		private String token;

		public StringToken(String value) {
			super();
			this.token = value;
		}

		public String getToken() {
			return this.token;
		}

		public int getType() {
			return TYPE;
		}

	} // end of inner class StringToken

}// StringStatement
