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
 * PortConnection represents a connection to a module port in a ModuleInstance
 * 
 * 
 * Created: Tue Mar 12 16:20:29 2002
 * 
 * @author <a href="mailto:abk@ladd">Andy Kollegger</a>
 * @version $Id: PortConnection.java 280 2006-08-11 17:00:32Z imiller $
 */

public class PortConnection implements VerilogElement {

	Net port;
	Expression e;

	/**
	 * Create a connection between the specified port and an expression.
	 * 
	 * @param port
	 *            the module instance port
	 * @param e
	 *            the expression connected to the port
	 */
	public PortConnection(Net port, Expression e) {
		if (port.getWidth() == e.getWidth()) {
			this.port = port;
			this.e = e;
		} else {
			throw new VerilogSyntaxException("Mismatched port assignment: "
					+ port.toString() + "(" + port.getWidth() + " bits)"
					+ " != " + e.toString() + "(" + e.getWidth() + " bits)");
		}

	} // PortConnection()

	public Identifier getPortID() {
		return port.getIdentifier();
	} // getPort()

	public Net getPort() {
		return port;
	}

	public Expression getExpression() {
		return e;
	}

	public Collection getNets() {
		return getExpression().getNets();
	}

	/**
	 * 
	 * @return <description>
	 */
	public Lexicality lexicalify() {
		Lexicality lex = new Lexicality();

		lex.append(Symbol.DOT);
		lex.append(port.getIdentifier());
		lex.append(Symbol.OPEN_PARENTHESIS);
		lex.append(e);
		lex.append(Symbol.CLOSE_PARENTHESIS);

		return lex;
	} // lexicalify()

	public String toString() {
		return lexicalify().toString();
	}

}// PortConnection
