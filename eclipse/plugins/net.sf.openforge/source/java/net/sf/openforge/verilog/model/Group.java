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

import java.util.*;

/**
 * Group is an expression wrapper which surrounds an expression in parenthesis
 * symbols.
 * 
 * <P>
 * 
 * Created: Fri Mar 02 2001
 * 
 * @author abk
 * @version $Id: Group.java 2 2005-06-09 20:00:48Z imiller $
 */
public class Group implements Expression {

	private Expression base;

	public Group(Expression base) {
		this.base = base;
	}

	public int getWidth() {
		return base.getWidth();
	}

	public Collection<Expression> getNets() {
		return base.getNets();
	}

	public Lexicality lexicalify() {
		Lexicality lex = new Lexicality();

		lex.append(Symbol.OPEN_PARENTHESIS);
		lex.append(base);
		lex.append(Symbol.CLOSE_PARENTHESIS);

		return lex;
	} // lexicalify()

	public String toString() {
		return lexicalify().toString();
	}

} // end of class Group
