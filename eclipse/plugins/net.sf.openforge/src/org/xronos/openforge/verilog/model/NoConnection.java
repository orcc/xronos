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

/**
 * NoConnection represents a no connection to a module port in a ModuleInstance
 * 
 * Created: Tue Mar 12 16:20:29 2002
 * 
 * @author ysyu
 * @version $Id: NoConnection.java 2 2005-06-09 20:00:48Z imiller $
 */

public class NoConnection implements VerilogElement {

	Net port;

	/**
	 * Create a no connection between the specified port
	 * 
	 * @param port
	 *            the module instance port
	 */
	public NoConnection(Net port) {
		this.port = port;
	}

	public Identifier getPortID() {
		return port.getIdentifier();
	}

	public Net getPort() {
		return port;
	}

	/**
	 * 
	 * @return <description>
	 */
	@Override
	public Lexicality lexicalify() {
		Lexicality lex = new Lexicality();

		lex.append(Symbol.DOT);
		lex.append(port.getIdentifier());
		lex.append(Symbol.OPEN_PARENTHESIS);
		lex.append(Symbol.CLOSE_PARENTHESIS);

		return lex;
	}

	@Override
	public String toString() {
		return lexicalify().toString();
	}
}
