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
import java.util.Collections;

/**
 * MemoryElement is a NetLValue which represents a single word of memory.
 * <P>
 * Created: Thu Feb 08 2001
 * 
 * @author abk
 * @version $Id: MemoryElement.java 2 2005-06-09 20:00:48Z imiller $
 */
// public class MemoryElement implements NetLValue
public class MemoryElement extends Net {

	Register memory;
	Expression address;

	/**
	 * Constructs a MemoryElement.
	 * 
	 * @param memory
	 *            the Register identifier of the memory
	 * @param address
	 *            an expression which selects a word in the memory
	 */
	public MemoryElement(Register memory, Expression address) {
		super(Keyword.REG, memory.getIdentifier(), memory.getWidth());
		this.memory = memory;
		this.address = address;
	} // MemoryElement()

	/**
	 * Constructs a new MemoryElement which uses an integer constant to select
	 * the address.
	 * 
	 * @param memory
	 *            the Register identifier of the memory
	 * @param address
	 *            the integer which selects a word in the memory
	 */
	public MemoryElement(Register memory, int address) {
		this(memory, new Constant(address));
	}

	public Identifier getIdentifier() {
		// return memory.getIdentifier();
		return new Identifier(lexicalify().toString());
	}

	public Expression getAddress() {
		return address;
	}

	public int getWidth() {
		return memory.getWidth();
	}

	public Collection getNets() {
		// return Collections.singleton(memory);
		return Collections.EMPTY_LIST;
	}

	public Expression getBitSelect(int position) {
		return new MemoryElementRange(position, position);
	}

	public Expression getRange(int msb, int lsb) {
		return new MemoryElementRange(msb, lsb);
	}

	public Expression getFullRange() {
		return getRange(getMSB(), getLSB());
	}

	public Lexicality lexicalify() {
		Lexicality lex = new Lexicality();

		lex.append(memory.getIdentifier());
		lex.append(Symbol.OPEN_SQUARE);
		lex.append(address);
		lex.append(Symbol.CLOSE_SQUARE);

		return lex;
	}

	public String toString() {
		return lexicalify().toString();
	}

	/**
	 * MemoryElement.Range is a MemoryElement which presents the fully qualified
	 * MemoryElement (identifier plus a range). If MSB==LSB, then the Range is
	 * presented as a bit select.
	 * <P>
	 * Example:<BR>
	 * <CODE><PRE>
	 * a[32:0]
	 * a[14]
	 * </PRE>
	 * </CODE>
	 * <P>
	 * 
	 * Created: Tue Feb 13 2001
	 * 
	 * @author abk
	 * @version $Id: MemoryElement.java 2 2005-06-09 20:00:48Z imiller $
	 */
	public class MemoryElementRange implements NetLValue {

		Range range;

		public MemoryElementRange(int msb, int lsb) {
			if ((msb > MemoryElement.this.getWidth()) || (lsb < 0)) {
				throw new IllegalBitRange(msb, lsb);
			}

			if (msb == MemoryElement.this.getMSB()
					&& lsb == MemoryElement.this.getLSB())
				range = null;
			else
				range = new Range(msb, lsb);
		} // MemoryElementRange()

		public int getWidth() {
			if (range == null)
				return 0;

			return range.getWidth();
		}

		public Collection getNets() {
			return Collections.EMPTY_LIST;
			/*
			 * Collection c = new HashSet(1); c.add(this); return c;
			 */
		}

		public Lexicality lexicalify() {
			Lexicality lex = MemoryElement.this.lexicalify();

			if (range != null)
				lex.append(range);

			return lex;
		} // lexicalify()

		public String toString() {
			return lexicalify().toString();
		}

	} // end of class Range

	// //////////////////
	// Nested exceptions
	//

	public class IllegalBitRange extends VerilogSyntaxException {

		public IllegalBitRange(int msb, int lsb) {
			super(new String("Illegal bit range -- msb:lsb " + msb + ":" + lsb));
		} // InvalidVerilogIdentifierException()

	} // end of nested class InvalidVerilogIdentifierException

} // end of class MemoryElement
