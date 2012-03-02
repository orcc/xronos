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

/**
 * A ProceduralTimingBlock is a procedural timing control statement, containing
 * a sub-statement triggered by event control.
 * 
 * <P>
 * Example:<BR>
 * <CODE>
 * &#64;(posedge CLK or posedge RESET) <BR>
 * a <= 1;<BR>
 * </CODE>
 * <P>
 * Created: Fri Mar 02 2001
 * 
 * @author abk
 * @version $Id: ProceduralTimingBlock.java 2 2005-06-09 20:00:48Z imiller $
 */
public class ProceduralTimingBlock implements Statement {

	EventControl ec;
	Statement body;

	public ProceduralTimingBlock(EventControl ec, Statement body) {
		this.ec = ec;
		this.body = body;

	} // ProceduralTimingBlock()

	public Collection getNets() {
		HashSet nets = new HashSet();

		nets.addAll(ec.getNets());
		nets.addAll(body.getNets());

		return nets;
	} // getNets()

	public Lexicality lexicalify() {
		Lexicality lex = new Lexicality();

		lex.append(ec);
		lex.append(body);

		return lex;
	}

	public String toString() {
		return lexicalify().toString();
	}

} // end of class ProceduralTimingBlock
