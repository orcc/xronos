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

import java.util.Iterator;

/**
 * MemoryDeclaration declares a memory. All {@link Register Registers} added to
 * the declaration must of the same bit-width and address depth.
 * <P>
 * Example:<BR>
 * <CODE>
 * reg [31:0] memA [255:0]; // declares a 32-bit wide memory which is 256 words deep
 * </CODE>
 * 
 * <P>
 * 
 * Created: Fri Feb 09 2001
 * 
 * @author abk
 * @version $Id: MemoryDeclaration.java 2 2005-06-09 20:00:48Z imiller $
 */
public class MemoryDeclaration extends NetDeclaration implements Statement {

	int upper;
	int lower;

	/**
	 * Constucts a new MemoryDeclaration for a Register with specified upper and
	 * lower addresses.
	 * 
	 * @param memory
	 *            the Register base for the memory
	 * @param upper
	 *            the upper address limit
	 * @param lower
	 *            the lower address limit
	 */
	public MemoryDeclaration(Register memory, int upper, int lower) {
		super(memory);

		this.upper = upper;
		this.lower = lower;

	} // MemoryDeclaration()

	public MemoryDeclaration(Register[] memories, int upper, int lower) {
		this(memories[0], upper, lower);

		for (int i = 1; i < memories.length; i++) {
			add(memories[i]);
		}
	} // MemoryDeclaration()

	public MemoryDeclaration(InitializedMemory mem) {
		this(mem, mem.depth() - 1, 0);
	}

	public void add(Register memory) {
		super.add(memory);
	}

	@Override
	public Lexicality lexicalify() {
		Lexicality lex = new Lexicality();

		lex.append(type);

		//
		// Only declare the net, output, input, wire, register, etc as
		// a vector if the width is greater than 1 bit.
		//
		if (width > 1) {
			lex.append(new Range(msb, lsb));
		}

		for (Iterator<Net> it = nets.iterator(); it.hasNext();) {
			lex.append(it.next().getIdentifier());
			lex.append(new Range(upper, lower));
			if (it.hasNext())
				lex.append(Symbol.COMMA);
		}

		lex.append(Symbol.SEMICOLON);

		return lex;

	} // lexicalify()

	@Override
	public String toString() {
		return lexicalify().toString();
	}

} // end of class MemoryDeclaration
