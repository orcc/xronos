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
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import net.sf.openforge.verilog.model.Expression;
import net.sf.openforge.verilog.model.Lexicality;
import net.sf.openforge.verilog.model.Net;
import net.sf.openforge.verilog.model.Symbol;

/**
 * OrMany is the bitwise 'or' of many terms eg:
 * 
 * <pre>
 * go1 | go2 | done1 | done2
 * </pre>
 * 
 * <p>
 * Created: Fri Aug 23 11:57:40 2002
 * 
 * @author imiller, last modified by $Author: imiller $
 * @version $Id: OrMany.java 2 2005-06-09 20:00:48Z imiller $
 */
public class OrMany implements Expression {

	private List<Expression> exprs;

	public OrMany() {
		exprs = new ArrayList<Expression>();
	}

	public OrMany(Collection<Expression> many) {
		this();
		exprs.addAll(many);
	}

	public void add(Expression expr) {
		exprs.add(expr);
	}

	@Override
	public Collection<Net> getNets() {
		Set<Net> nets = new LinkedHashSet<Net>();
		for (Expression expression : exprs) {
			nets.addAll(expression.getNets());
		}
		return nets;
	}

	@Override
	public int getWidth() {
		int max = 0;
		for (Expression expression : exprs) {
			max = java.lang.Math.max(max, (expression.getWidth()));
		}
		return max;
	}

	@Override
	public Lexicality lexicalify() {
		Lexicality lex = new Lexicality();
		for (Iterator<Expression> iter = exprs.iterator(); iter.hasNext();) {
			lex.append(iter.next());
			if (iter.hasNext()) {
				/*
				 * Changed to bitwise OR because doutConcatOr in StructualMemory
				 * was getting sliced down to a single bit by the boolean OR.
				 * According to Ian, our Verilog should never need anything
				 * other than a bitwise OR. --SGE
				 */
				lex.append(Symbol.OR);
			}
		}
		return lex;
	}

}// OrMany
