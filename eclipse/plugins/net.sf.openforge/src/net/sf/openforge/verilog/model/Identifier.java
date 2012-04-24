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

package net.sf.openforge.verilog.model;

/**
 * An Identifier should be used wherever a valid Verilog identifier is needed.
 * Identifiers are guaranteed to comply with the following syntax:
 * <P>
 * identifier ::= [a-z][A-Z][a-zA-Z_$0-9]
 * <P>
 * Created: Fri Feb 02 2001
 * 
 * @author abk
 * @version $Id: Identifier.java 2 2005-06-09 20:00:48Z imiller $
 */
public class Identifier extends Token {

	String id;

	public static final int TYPE = 3;

	/**
	 * Constucts a verified verilog identifier, trimmed of any leading or
	 * trailing whitespace.
	 * 
	 */
	public Identifier(String id) {
		id = id.trim();

		if (verify(id)) {
			this.id = id;
		} else {
			throw new InvalidVerilogIdentifierException(id);
		}
	} // Identifier()

	/**
	 * Verifies that a given string can be used as a valid Verilog identifier.
	 * 
	 */
	public static boolean verify(String id) {
		boolean valid = true;

		char[] id_chars = id.toCharArray();

		if (id_chars.length > 0) {
			if (!(Character.isLetter(id_chars[0]) || (id_chars[0] == '_')))
				valid = false;

			if (valid) {
				for (int i = 1; i < id_chars.length; i++) {
					char c = id_chars[i];

					if (!(Character.isLetter(c) || Character.isDigit(c)
							|| (c == '_') || (c == '$'))) {
						valid = false;
						break;
					}
				}
			}

			if (valid) {
				// make sure this isn't a Verilog keyword
				valid = !Keyword.isKeyword(id);
			}
		}

		return valid;

	} // verify()

	// ////////////////////////////
	// VerilogElement interface

	public String getToken() {
		return id;
	}

	public int getType() {
		return TYPE;
	}

	// //////////////////////////////
	// nested class for exception

	public class InvalidVerilogIdentifierException extends
			VerilogSyntaxException {

		/**
		 * 
		 */
		private static final long serialVersionUID = 5205338149120554139L;

		public InvalidVerilogIdentifierException(String identifier) {
			super(new String("Invalid verilog identifier: " + identifier));
		} // InvalidVerilogIdentifierException()

	} // end of nested class InvalidVerilogIdentifierException

} // end of class Identifier

