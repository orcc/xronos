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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashSet;
import java.util.Set;

/**
 * 
 * Controls represent special control actions which can be placed in the lexical
 * Token stream.
 * 
 * Project: Forge
 * 
 * Created: abk on Wed May 16 2001 Modified: $Date: 2005-06-09 13:00:48 -0700
 * (Thu, 09 Jun 2005) $ by $Author: imiller $
 * 
 * @author Andreas Kollegger
 * @version $Rev: 2 $
 */

public class Control extends Token {

	private String token;

	public static final int TYPE = 5;

	/** The usual new line meaning. */
	public static Control NEWLINE = new Control.NewLine();

	/** Increase indentation to the next tab stop. */
	public static Control INDENT = new Control("+tab");

	/** Reduce indentation by one tab stop. */
	public static Control OUTDENT = new Control("-tab");

	/** Set the indent tab to current column position. */
	public static Control SETDENT = new Control("%");

	/** Move to next tab stop. */
	public static Control TAB = new Control("tab");

	/** Insert whitespace. */
	public static Control WHITESPACE = new Control(" ");

	private static Set whitespace_words;

	private static Set all_words;

	static {
		whitespace_words = new HashSet();

		whitespace_words.add(NEWLINE);
		whitespace_words.add(TAB);
		whitespace_words.add(WHITESPACE);

		all_words = new HashSet();
		all_words.addAll(whitespace_words);
		all_words.add(INDENT);
		all_words.add(OUTDENT);
		all_words.add(SETDENT);
	}

	private Control(String token) {
		this.token = token;
	} // Control()

	public static boolean isWhitespace(Token t) {
		return whitespace_words.contains(t);
	}

	// ////////////////////////////
	// VerilogElement interface

	public String getToken() {
		return token;
	}

	public int getType() {
		return TYPE;
	}

	public static final class NewLine extends Control {
		public NewLine() {
			super("\n");
		}

		public String getToken() {
			String token = super.getToken();
			try {
				StringWriter sw = new StringWriter();
				PrintWriter printer = new PrintWriter(sw);
				printer.println();
				printer.close();
				token = sw.toString();
				sw.close();
			} catch (Exception e) {
			}

			return token;
		} // getToken()

	} // inner class NewLine

} // end class Control
