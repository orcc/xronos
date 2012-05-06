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

package net.sf.openforge.verilog.pattern;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import net.sf.openforge.verilog.model.Expression;
import net.sf.openforge.verilog.model.Lexicality;
import net.sf.openforge.verilog.model.Net;
import net.sf.openforge.verilog.model.Statement;
import net.sf.openforge.verilog.model.Symbol;
import net.sf.openforge.verilog.model.VerilogElement;

/**
 * CommaDelimitedStatement represents a sequence of {@link VerilogElement
 * VerilogElements} seperated by commas. This is particularly usefull with the
 * FStatement.FWrite class.
 * 
 * <p>
 * Created: Fri Aug 23 09:31:04 2002
 * 
 * @author imiller, last modified by $Author: imiller $
 * @version $Id: CommaDelimitedStatement.java 2 2005-06-09 20:00:48Z imiller $
 */
public class CommaDelimitedStatement implements Statement {

	private List<VerilogElement> elements = new ArrayList<VerilogElement>();

	public CommaDelimitedStatement(Collection<VerilogElement> elements) {
		this.elements = new ArrayList<VerilogElement>(elements);
	}

	public CommaDelimitedStatement() {
	}

	/**
	 * Adds another element the sequence of comma delimited elements.
	 */
	public void append(VerilogElement element) {
		elements.add(element);
	}

	public void prepend(VerilogElement element) {
		elements.add(0, element);
	}

	@Override
	public Collection<Net> getNets() {
		Set<Net> nets = new HashSet<Net>();
		for (Iterator<VerilogElement> iter = elements.iterator(); iter
				.hasNext();) {
			VerilogElement ve = iter.next();
			if (ve instanceof Statement)
				nets.addAll(((Statement) ve).getNets());
			else if (ve instanceof Expression)
				nets.addAll(((Expression) ve).getNets());
			else if (ve instanceof Net)
				nets.add((Net) ve);
		}
		return nets;
	}

	@Override
	public Lexicality lexicalify() {
		Lexicality lex = new Lexicality();
		for (Iterator<VerilogElement> iter = elements.iterator(); iter
				.hasNext();) {
			lex.append(iter.next());
			if (iter.hasNext())
				lex.append(Symbol.COMMA);
		}
		return lex;
	}

	@Override
	public String toString() {
		return lexicalify().toString();
	}

}// CommaDelimitedStatement
