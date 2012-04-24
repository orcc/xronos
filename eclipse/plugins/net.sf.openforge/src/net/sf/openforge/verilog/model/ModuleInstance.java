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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * ModuleInstance is an instantiation of a module. It is a blending of the
 * strict module_instance and module_instantiation structures.
 * <P>
 * 
 * 
 * Created: Tue Mar 12 15:07:38 2002
 * 
 * @author <a href="mailto:abk@ladd">Andy Kollegger</a>
 * @version $Id: ModuleInstance.java 284 2006-08-15 15:43:34Z imiller $
 */
public class ModuleInstance implements Statement {

	private Identifier name_of_module;
	private Identifier name_of_instance;
	private List port_connections = new ArrayList();
	private HashSet port_names = new LinkedHashSet();
	private Set paramValues = new LinkedHashSet();
	private Module module;

	public ModuleInstance(Identifier name_of_module, Identifier name_of_instance) {
		this.name_of_module = name_of_module;
		this.name_of_instance = name_of_instance;

	} // ModuleInstance()

	public ModuleInstance(String name_of_module, String name_of_instance) {
		this(new Identifier(name_of_module), new Identifier(name_of_instance));
	}

	public ModuleInstance(Module module, Identifier name_of_instance) {
		this(module.getIdentifier(), name_of_instance);

		this.module = module;
	}

	public ModuleInstance(Module module, String name_of_instance) {
		this(module, new Identifier(name_of_instance));
	}

	public Identifier getIdentifier() {
		return name_of_instance;
	}

	/**
	 * 
	 * @param pc
	 *            a port connection complete with Port to Net
	 */
	public void add(PortConnection pc) {
		if (!port_names.contains(pc.getPortID())) {
			if (module != null) {
				if (!module.hasPort(pc.getPortID())) {
					throw new VerilogSyntaxException("Module \""
							+ module.getIdentifier().toString() + "\""
							+ " doesn't have a port named \"" + pc.getPortID()
							+ "\"");
				}
			}

			port_connections.add(pc);
			port_names.add(pc.getPortID());
		} else {
			throw new VerilogSyntaxException("Duplicate port connection: "
					+ pc.toString());
		}
	} // add()

	/**
	 * 
	 * @param pc
	 *            a no connect with only a port
	 */
	public void add(NoConnection pc) {
		if (!port_names.contains(pc.getPortID())) {
			if (module != null) {
				if (!module.hasPort(pc.getPortID())) {
					throw new VerilogSyntaxException("Module \""
							+ module.getIdentifier().toString() + "\""
							+ " doesn't have a port named \"" + pc.getPortID()
							+ "\"");
				}
			}

			port_connections.add(pc);
			port_names.add(pc.getPortID());
		} else {
			throw new VerilogSyntaxException("Duplicate port connection: "
					+ pc.toString());
		}
	}

	/**
	 * Creates a port connection and adds it to the instance.
	 * <P>
	 * Example port connection:<BR>
	 * .ARG0(iadd_4)
	 * 
	 * @param port
	 *            the name of the module port
	 * @param e
	 *            the expression to connect to the port
	 */
	public void connect(Net port, Expression e) {
		PortConnection p = new PortConnection(port, e);
		add(p);
	} // connect()

	/**
	 * Creates a no connect port and adds it to the instance.
	 * <p>
	 * Example port connection:<br>
	 * .ARG0()
	 * 
	 * @param port
	 *            the name of the module port
	 */
	public void noConnect(Net port) {
		NoConnection n = new NoConnection(port);
		add(n);
	}

	public void addParameterValue(ParameterSetting psetting) {
		paramValues.add(psetting);
	}

	public Collection getNets() {
		HashSet nets = new HashSet();
		for (Iterator it = port_connections.iterator(); it.hasNext();) {
			PortConnection portConnection = (PortConnection) it.next();
			nets.addAll(portConnection.getNets());
		}
		return nets;
	} // getNets()

	public Lexicality lexicalify() {
		Lexicality lex = new Lexicality();
		final int MAX_WIDTH = 72;
		int widthCount = 0;

		lex.append(name_of_module);
		widthCount += name_of_module.toString().length();
		if (!this.paramValues.isEmpty()) {
			lex.append(Symbol.PARAM_HASH);
			lex.append(Symbol.OPEN_PARENTHESIS);
			for (Iterator iter = paramValues.iterator(); iter.hasNext();) {
				VerilogElement element = (VerilogElement) iter.next();
				lex.append(element);
				if (iter.hasNext()) {
					lex.append(Symbol.COMMA);
				}
				widthCount += element.toString().length();
				if (widthCount > MAX_WIDTH) {
					lex.append(Control.NEWLINE);
					lex.append(Control.WHITESPACE);
					lex.append(Control.WHITESPACE);
					widthCount = 2; // 2 for the whitespace
				}
			}
			lex.append(Symbol.CLOSE_PARENTHESIS);
		}

		lex.append(name_of_instance);
		widthCount += name_of_instance.toString().length();

		lex.append(Symbol.OPEN_PARENTHESIS);

		for (Iterator it = port_connections.iterator(); it.hasNext();) {
			VerilogElement element = (VerilogElement) it.next();
			lex.append(element);

			widthCount += element.toString().length();
			if (it.hasNext()) {
				lex.append(Symbol.COMMA);
				// Only insert a new line if we have more to print
				if (widthCount > MAX_WIDTH) {
					lex.append(Control.NEWLINE);
					lex.append(Control.WHITESPACE);
					lex.append(Control.WHITESPACE);
					widthCount = 2; // 2 for the whitespace
				}
			}
		}

		lex.append(Symbol.CLOSE_PARENTHESIS);

		lex.append(Symbol.SEMICOLON);

		return lex;
	} // lexicalify()

	public String toString() {
		return lexicalify().toString();
	}

}// ModuleInstance
