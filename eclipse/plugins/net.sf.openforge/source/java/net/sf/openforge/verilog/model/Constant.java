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
import java.util.HashSet;

/**
 * Constant is an unsigned decimal number of immutable size and value.
 * 
 * <P>
 * 
 * Created: Wed Feb 28 2001
 * 
 * @author abk
 * @version $Id: Constant.java 2 2005-06-09 20:00:48Z imiller $
 */
public class Constant extends Token implements Expression {

	public static final int TYPE = 4;

	public final static int HEX = 16;
	public final static int DECIMAL = 10;
	public final static int OCTAL = 8;
	public final static int BINARY = 2;

	Number n;
	int size;

	public Constant(Number n) {
		this.n = n;

		if (n instanceof Byte)
			setSize(8);
		else if (n instanceof Double)
			setSize(64);
		else if (n instanceof Float)
			setSize(32);
		else if (n instanceof Integer)
			setSize(32);
		else if (n instanceof Long)
			setSize(64);
		else if (n instanceof Short)
			setSize(16);
		else
			setSize(64);
	}

	public Constant(String s) {
		this(new Long(s));
	}

	public Constant(String s, int size) {
		this(new Long(s));
		setSize(size);
	}

	public Constant(long l) {
		this(new Long(l));
	}

	public Constant(long l, int size) {
		this(new Long(l));
		setSize(size);
	}

	public Constant(int i) {
		this(new Integer(i));
	}

	public Constant(byte b) {
		this(new Byte(b));
	}

	public final int getSize() {
		return size;
	}

	public final Number getValue() {
		return n;
	}

	public int getRadix() {
		return DECIMAL;
	}

	public final long longValue() {
		return n.longValue();
	}

	private void setSize(int size) {
		this.size = size;

		n = new Long(mask(getValue().longValue(), size));
	}

	protected long mask(long value, int size) {
		long mask = 0;
		long power = 1;

		for (int i = 0; i < size; i++) {
			mask |= power;
			power *= 2;
		}

		return (value & mask);
	}

	// ////////////////////////////
	// VerilogElement interface

	public String getToken() {
		String reply = getValue().toString();

		return reply;
	}

	public int getType() {
		return TYPE;
	}

	/**
	 * 
	 * @return <description>
	 */
	public Collection getNets() {
		return new HashSet(1);
	}

	/**
	 * 
	 * @return <description>
	 */
	public int getWidth() {
		return getSize();
	}

} // end of class Constant

