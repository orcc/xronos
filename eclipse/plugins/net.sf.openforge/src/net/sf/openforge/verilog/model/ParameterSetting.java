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
 *
 */
package net.sf.openforge.verilog.model;

/**
 * ParameterSetting represents a parameter value specification in a module
 * instantiation
 * 
 * 
 * Created: Thu Aug 08 16:20:29 2006
 * 
 * @author <a href="mailto:imiller@xilinx.com">Ian Miller</a>
 * @version $Id: ParameterSetting.java 284 2006-08-15 15:43:34Z imiller $
 */

public class ParameterSetting implements VerilogElement {

	private ArbitraryString paramName;
	private VerilogElement e;

	public ParameterSetting(String paramName, String e) {
		this(paramName, new ArbitraryString(e));
	}

	/**
	 * Create a Parameter setting to the name of the specified expression
	 * 
	 * @param paramName
	 *            the string name of the parameter to be set.
	 * @param e
	 *            the expression that defines the value of the parameter
	 */
	public ParameterSetting(String paramName, VerilogElement e) {
		this.paramName = new ArbitraryString(paramName);
		this.e = e;
	}

	/**
	 * 
	 * @return <description>
	 */
	@Override
	public Lexicality lexicalify() {
		Lexicality lex = new Lexicality();

		lex.append(Symbol.DOT);
		lex.append(paramName);
		lex.append(Symbol.OPEN_PARENTHESIS);
		lex.append(e);
		lex.append(Symbol.CLOSE_PARENTHESIS);

		return lex;
	} // lexicalify()

	@Override
	public String toString() {
		return lexicalify().toString();
	}

}
