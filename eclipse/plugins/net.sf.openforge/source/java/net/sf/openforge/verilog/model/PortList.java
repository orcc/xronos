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
import java.util.List;

/**
 * PortList is a comma-separated list of Identifiers.
 * <P>
 * Example:<BR>
 * <CODE>
 * ARG0, ARG1, RESULT, CLK, RESET
 * </CODE> Created: Fri Feb 09 2001
 * 
 * @author abk
 * @version $Id: PortList.java 2 2005-06-09 20:00:48Z imiller $
 */
public class PortList implements VerilogElement {

	List ports = new ArrayList();
	HashSet nets = new HashSet();

	/**
	 * Constructs an empty PortList.
	 */
	public PortList() {
	} // PortList()

	/**
	 * Construct a PortList which extracts the Identifiers from an array of
	 * Nets.
	 */
	public PortList(Net[] netlist) {
		for (int i = 0; i < netlist.length; i++) {
			add(netlist[i]);
		}
	} // PortList(NetList)

	public void add(Net net) {
		Identifier id = net.getIdentifier();

		if (!hasPort(id)) {
			ports.add(id);
			nets.add(net);
		} else {
			throw new VerilogSyntaxException(
					"Identifier already contained in list: " + id);
		}

	} // add(Net)

	/**
	 * Checks whether the port list contains a port with a particular name.
	 * 
	 * @return true if the port list constains the port
	 */
	public boolean hasPort(Identifier id) {
		return ports.contains(id);
	}

	public Collection getNets() {
		return nets;
	}

	public int size() {
		return ports.size();
	}

	public Lexicality lexicalify() {
		Lexicality lex = new Lexicality();
		for (Iterator it = ports.iterator(); it.hasNext();) {
			lex.append((Identifier) it.next());
			if (it.hasNext())
				lex.append(Symbol.COMMA);
		}
		return lex;
	} // lexicalify()

	public String toString() {
		return lexicalify().toString();
	}

} // end of class PortList
