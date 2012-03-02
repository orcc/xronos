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
 * Always is simply an always block with a related statement.
 * 
 * <P>
 * Example:<BR>
 * <CODE>
 * always<BR>
 *   a_reg <= 0;
 * </CODE>
 * <P>
 * Created: Tue Mar 06 2001
 * 
 * @author abk
 * @version $Id: Always.java 2 2005-06-09 20:00:48Z imiller $
 */
public class Always implements Statement {

	@SuppressWarnings("unused")
	private static final String _RCS_ = "RCS_REVISION: $Rev: 2 $";

	Statement body;

	public Collection getNets() {
		return body.getNets();
	}

	public Always(Statement body) {
		this.body = body;
	}

	public Lexicality lexicalify() {
		Lexicality lex = new Lexicality();

		lex.append(Keyword.ALWAYS);
		lex.append(body);

		return lex;
	} // lexicalify()

	public String toString() {
		return lexicalify().toString();
	}

} // end of class Always
