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
import java.util.Set;

import net.sf.openforge.verilog.pattern.BusRegister;

/**
 * NetDeclaration declares one or more nets.
 * <P>
 * Example:<BR>
 * <CODE>
 * wire [31:0] a, b;
 * </CODE>
 * 
 * <P>
 * 
 * Created: Fri Feb 09 2001
 * 
 * @author abk
 * @version $Id: NetDeclaration.java 2 2005-06-09 20:00:48Z imiller $
 */
public class NetDeclaration implements Statement {

	List<Net> nets;
	Set<String> names;

	Keyword type;
	int width;
	int msb;
	int lsb;

	public NetDeclaration() {
		nets = new ArrayList<Net>();
		names = new HashSet<String>();
	}

	public NetDeclaration(Net net) {
		this();

		nets.add(net);
		names.add(net.getIdentifier().toString());

		width = net.getWidth();
		lsb = net.getLSB();
		msb = net.getMSB();
		type = net.getType();

	} // NetDeclaration()

	public NetDeclaration(Net[] nets) {
		this(nets[0]);

		for (int i = 1; i < nets.length; i++) {
			add(nets[i]);
		}
	} // NetDeclaration()

	public void add(Net net) {
		if ((net.getWidth() == width) && (net.getType() == type)
				&& (net.getLSB() == lsb) && (net.getMSB() == msb)) {
			if (!names.contains(net.getIdentifier().toString())) {
				nets.add(net);
				names.add(net.getIdentifier().toString());
			} else {
				throw new VerilogSyntaxException(
						"Duplicate wire name in declaration.");
			}
		} else {
			throw new VerilogSyntaxException("Mismatched nets in declaration.");
		}
	}

	public void remove(Net net) {
		nets.remove(net);
		names.remove(net.getIdentifier().toString());
	}

	@Override
	public Collection<Net> getNets() {
		return nets;
	}

	public Keyword getType() {
		return type;
	}

	@Override
	public Lexicality lexicalify() {
		if (nets.size() == 0) {
			throw new VerilogSyntaxException(
					"NetDeclaration contains no nets to declare.");
		}
		Lexicality lex = new Lexicality();

		lex.append(type);

		//
		// Only declare the net, output, input, wire, register, etc as
		// a vector if the width is greater than 1 bit. Also no width
		// is specified if the type of net is integer or real
		//
		if (width > 1 && type != Keyword.INTEGER && type != Keyword.REAL) {
			lex.append(getRange());
		}

		for (Iterator<Net> it = nets.iterator(); it.hasNext();) {
			Net net = it.next();
			lex.append(net.getIdentifier());
			if (net instanceof BusRegister) {
				Expression declarationInitial = ((BusRegister) net)
						.getDeclarationInitial();
				lex.append(Symbol.BLOCKING_ASSIGN);
				lex.append(declarationInitial);
			}
			if (it.hasNext())
				lex.append(Symbol.COMMA);
		}

		lex.append(Symbol.SEMICOLON);

		return lex;

	} // lexicalify()

	/**
	 * Returns the Range used for bit select for this NetDeclaration. This
	 * method allows subclasses to override the behavior of how this range
	 * appears in the resulting HDL.
	 * 
	 * @return a value of type 'Range'
	 */
	protected Range getRange() {
		return new Range(msb, lsb);
	}

	@Override
	public String toString() {
		return lexicalify().toString();
	}

} // end of class NetDeclaration
