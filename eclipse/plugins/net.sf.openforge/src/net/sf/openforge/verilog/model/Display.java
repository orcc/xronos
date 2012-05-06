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
import java.util.HashSet;
import java.util.Set;

/**
 * Display.java
 * 
 * 
 * <p>
 * Created: Wed Jan 8 12:20:09 2003
 * 
 * @author imiller, last modified by $Author: imiller $
 * @version $Id: Display.java 2 2005-06-09 20:00:48Z imiller $
 */
public class Display implements Statement {

	protected Statement expr;

	public Display(Statement stat) {
		expr = stat;
	}

	@Override
	public Lexicality lexicalify() {
		Lexicality lex = new Lexicality();

		lex.append(Symbol.DOLLAR);
		lex.append(Keyword.DISPLAY);

		if (expr != null) {
			lex.append(Symbol.OPEN_PARENTHESIS);
			lex.append(expr);
			lex.append(Symbol.CLOSE_PARENTHESIS);
		}

		lex.append(Symbol.SEMICOLON);

		return lex;
	} // lexicalify()

	@Override
	public Collection<Net> getNets() {
		Set<Net> nets = new HashSet<Net>();

		if (expr != null)
			nets.addAll(expr.getNets());

		return nets;
	} // getNets();

	@Override
	public String toString() {
		return lexicalify().toString();
	}

}// Display
