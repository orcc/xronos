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

/**
 * DelayStatement adds a relative delay to a statement of the form:
 * 
 * <pre>
 * #<delay> <statement>
 * </pre>
 * 
 * <p>
 * Created: Thu Aug 22 15:01:52 2002
 * 
 * @author imiller, last modified by $Author: imiller $
 * @version $Id: DelayStatement.java 2 2005-06-09 20:00:48Z imiller $
 */
public class DelayStatement implements Statement {

	private int ticks;
	private Statement statement;

	/**
	 * Creates a new delay statement with the given number of ticks by which to
	 * delay the statement
	 * 
	 * @param toDelay
	 *            a value of type 'Statement'
	 * @param ticksToDelay
	 *            a value of type 'int'
	 */
	public DelayStatement(Statement toDelay, int ticksToDelay) {
		ticks = ticksToDelay;
		statement = toDelay;
	}

	@Override
	public Lexicality lexicalify() {
		Lexicality lex = new Lexicality();

		lex.append(Symbol.DELAY);
		lex.append(new Constant(ticks));
		lex.append(statement);

		return lex;
	}

	@Override
	public Collection<Net> getNets() {
		return statement.getNets();
	}

	@Override
	public String toString() {
		return lexicalify().toString();
	}

}
