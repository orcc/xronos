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
 * ModuleDeclaration is a verilog statement which declares a module using an
 * identifier and some ports. Technically, the ModuleDeclaration should include
 * everything inside the module and the closing
 * <code>endmodule<code> keyword. But restricting
 * the declaration to a single statement makes it easier to manage
 * a module as a container of statements.
 * 
 * <P>
 * Example:<BR>
 * <CODE>
 * module foo(ARG0, ARG1, RESULT);
 * </CODE> Created: Wed Feb 07 2001
 * 
 * @author abk
 * @version $Id: ModuleDeclaration.java 2 2005-06-09 20:00:48Z imiller $
 */
public class ModuleDeclaration implements Statement {

	Identifier identifier;

	PortList ports;

	public ModuleDeclaration(Identifier identifier) {
		this(identifier, new PortList());
		this.identifier = identifier;
	} // ModuleDeclaration()

	public ModuleDeclaration(Identifier identifier, Net[] ports) {
		this(identifier, new PortList(ports));
	} // ModuleDeclaration()

	public ModuleDeclaration(Identifier identifier, PortList ports) {
		this.identifier = identifier;
		this.ports = ports;

	} // ModuleDeclaration()

	public ModuleDeclaration(String identifier) {
		this(new Identifier(identifier));
	}

	public ModuleDeclaration(String identifier, PortList ports) {
		this(new Identifier(identifier), ports);
	}

	public Identifier getIdentifier() {
		return identifier;
	}

	public void add(Input in) {
		ports.add(in);
	}

	public void add(Output out) {
		ports.add(out);
	}

	public void add(Inout io) {
		ports.add(io);
	}

	/**
	 * Checks whether the module declaration has a port with a particular name.
	 * 
	 * @return true if the module declaration has the named port
	 */
	public boolean hasPort(Identifier id) {
		return ports.hasPort(id);
	}

	public PortList getPorts() {
		return ports;
	}

	public Collection getNets() {
		return ports.getNets();
	}

	// ////////////////////////////
	// VerilogElement interface

	public Lexicality lexicalify() {
		Lexicality lex = new Lexicality();

		lex.append(Keyword.MODULE);
		lex.append(identifier);
		lex.append(Symbol.OPEN_PARENTHESIS);
		if (ports.size() > 0) {
			lex.append(ports);
		}
		lex.append(Symbol.CLOSE_PARENTHESIS);
		lex.append(Symbol.SEMICOLON);

		return lex;
	} // lexicalify()

	public String toString() {
		return lexicalify().toString();
	}

} // end of class ModuleDeclaration
