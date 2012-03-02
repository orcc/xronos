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

/**
 * Replication is a verilog expression that repeats a given expression a fixed
 * number of times: <code>
 * {N{expr}}
 * </code> Where <code>N</code> is the number of times that <code>expr</code> is
 * repeated.
 * 
 * Created: Tue Mar 12 11:39:28 2002
 * 
 * @author <a href="mailto:abk@ladd">Andy Kollegger</a>
 * @version $Id: Replication.java 2 2005-06-09 20:00:48Z imiller $
 */

public class Replication extends Concatenation {

	private Constant repetition_number;
	private Concatenation repeated_expression;

	public Replication(Constant repetition_number, Expression e) {
		this.repetition_number = repetition_number;
		this.repeated_expression = new Concatenation(e);

		super.add(repetition_number);
		super.add(repeated_expression);
	} // Replication()

	public Replication(int repetition_number, Expression e) {
		this(new Constant(repetition_number), e);
	} // Replication()

	public int getWidth() {
		return (int) (repeated_expression.getWidth() * repetition_number
				.longValue());
	} // getWidth()

	/**
	 * Adding to a replication adds to the internal concatenation which is
	 * getting repeated.
	 * 
	 * @param e
	 *            the Expression to add
	 */
	public void add(Expression e) {
		repeated_expression.add(e);
	} // add()

	public Lexicality lexicalify() {
		Lexicality lex = new Lexicality();

		lex.append(Symbol.OPEN_CURLY);

		lex.append(repetition_number);

		lex.append(repeated_expression);

		lex.append(Symbol.CLOSE_CURLY);

		return lex;

	} // lexicalify()

} // class Replication
