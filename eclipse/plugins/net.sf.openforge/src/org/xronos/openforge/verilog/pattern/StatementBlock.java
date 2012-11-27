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
package org.xronos.openforge.verilog.pattern;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.xronos.openforge.verilog.model.Lexicality;
import org.xronos.openforge.verilog.model.Net;
import org.xronos.openforge.verilog.model.Statement;
import org.xronos.openforge.verilog.model.VerilogElement;


/**
 * A StatementBlock is a simple sequence of Statements.
 * <P>
 * 
 * Created: Tue Mar 12 09:46:58 2002
 * 
 * @author <a href="mailto:andreas.kollegger@xilinx.com">Andy Kollegger</a>
 * @version $Id: StatementBlock.java 2 2005-06-09 20:00:48Z imiller $
 */

public class StatementBlock implements Statement, VerilogElement {

	protected List<Statement> statements = new ArrayList<Statement>();

	public StatementBlock() {
	} // StatementBlock

	/**
	 * Append a statement to the sequence.
	 * 
	 * @param s
	 *            the statement to be appended
	 */
	public void add(Statement s) {
		statements.add(s);
	} // add()

	/**
	 * 
	 * @return <description>
	 */
	@Override
	public Lexicality lexicalify() {
		Lexicality lex = new Lexicality();
		for (Statement stt : statements) {
			lex.append(stt);
		}

		return lex;
	} // lexicalify()

	/**
	 * 
	 * @return <description>
	 */
	@Override
	public Collection<Net> getNets() {
		Set<Net> nets = new HashSet<Net>();

		for (Iterator<Statement> it = statements.iterator(); it.hasNext();) {
			nets.addAll(it.next().getNets());
		}
		return nets;
	} // getNets()

	@Override
	public String toString() {
		return lexicalify().toString();
	}

} // class StatementBlock
