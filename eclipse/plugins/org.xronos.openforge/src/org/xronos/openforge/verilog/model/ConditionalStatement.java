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
package org.xronos.openforge.verilog.model;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * A ConditionalStatement is the traditional if-else branching construct. As
 * with the simplified Conditional (a?b:c) construct, an expression with width>1
 * will be grouped into a compare against non-zero.
 * <P>
 * Example:<BR>
 * <CODE>
 * if (a!=32'h0) <BR>
 *     b<=16'hbabe;<BR>
 * else<BR>
 *     b<=16'h0;<BR>
 * </CODe>
 * <P>
 * Created: Tue Jun 26 15:25:21 2001
 * 
 * @author <a href="mailto: ">Andy Kollegger</a>
 * @version $Id: ConditionalStatement.java 2 2005-06-09 20:00:48Z imiller $
 */

public class ConditionalStatement implements Statement {

	Expression condition;
	Statement trueBranch;
	Statement falseBranch;

	public ConditionalStatement(Expression condition, Statement trueBranch) {
		this(condition, trueBranch, null);
	}

	public ConditionalStatement(Expression condition, Statement trueBranch,
			Statement falseBranch) {
		if (condition.getWidth() > 1) {
			this.condition = new Group(new Compare.NEQ(condition,
					new HexNumber(0, condition.getWidth())));
		} else {
			this.condition = condition;
		}

		this.trueBranch = trueBranch;
		this.falseBranch = falseBranch;
	}

	@Override
	public Collection<Net> getNets() {
		Set<Net> nets = new HashSet<Net>();

		nets.addAll(condition.getNets());
		nets.addAll(trueBranch.getNets());
		if (falseBranch != null) {
			nets.addAll(falseBranch.getNets());
		}

		return nets;
	}

	@Override
	public Lexicality lexicalify() {
		Lexicality lex = new Lexicality();

		lex.append(Keyword.IF);
		lex.append(Symbol.OPEN_PARENTHESIS);
		lex.append(condition);
		lex.append(Symbol.CLOSE_PARENTHESIS);
		lex.append(trueBranch);

		if (falseBranch != null) {
			lex.append(Keyword.ELSE);
			lex.append(falseBranch);
		}
		return lex;
	} // lexicalify()

	@Override
	public String toString() {
		return lexicalify().toString();
	}

}// end of class ConditionalStatement
