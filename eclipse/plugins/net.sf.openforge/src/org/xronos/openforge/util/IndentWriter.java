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
package org.xronos.openforge.util;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Writer;

/**
 * A PrintWriter variant which automatically indents each line of output by a
 * dynamically configurable indent String (normally whitespace, such as a tab or
 * space, but it could be any string).
 * 
 * @version $Id: IndentWriter.java 2 2005-06-09 20:00:48Z imiller $
 * @author Andy Kollegger
 * @author Stephen Edwards
 */
public class IndentWriter extends PrintWriter {

	/** The indentString to insert at the start of each line */
	private String indentString = "\t";

	/** The number of times to insert the indentString at the start of each line */
	private int count = 0;

	/** Is an indent required? */
	private boolean isNewline = true;

	/**
	 * Constructs an IndentWriter without, automatic line flushing, from an
	 * existing OutputStream.
	 */
	public IndentWriter(OutputStream out) {
		this(out, true);
	}

	/**
	 * Constructs an IndentWriter from an existing OutputStream.
	 */
	public IndentWriter(OutputStream out, boolean autoFlush) {
		super(out, autoFlush);
	}

	/**
	 * Creates a new IndentWriter without automatic line flushing.
	 */
	public IndentWriter(Writer out) {
		this(out, true);
	}

	/**
	 * Creates a new IndentWriter.
	 */
	public IndentWriter(Writer out, boolean autoFlush) {
		super(out, autoFlush);
	}

	/**
	 * Creates a new IndentWriter from a PrintWriter.
	 * 
	 * @param base_indent
	 *            the initial number of indentation strings to write at the
	 *            start of each line
	 */
	public IndentWriter(int base_indent, PrintWriter printer) {
		this(printer);
		this.count = base_indent;
	}

	/**
	 * Sets the indentation string to be written at the start of each line, once
	 * for each level of indentation. The default indent string is a tab.
	 */
	public void setIndentString(String indentString) {
		this.indentString = indentString;
	}

	/**
	 * Increases the level of indentation by 1.
	 */
	public void increase() {
		count++;
	}

	/**
	 * Same as {@link #increase}, only shorter.
	 */
	public void inc() {
		increase();
	}

	/**
	 * Decreases the level of indentation by 1, with a minimum of 0.
	 */
	public void decrease() {
		if (count > 0) {
			count--;
		}
	}

	/**
	 * Same as {@link #decrease}, only shorter.
	 */
	public void dec() {
		decrease();
	}

	/**
	 * Sets the level of indentation to 0.
	 */
	public void reset() {
		count = 0;
	}

	public void println() {
		super.println();
		isNewline = true;
	}

	public void write(char[] buf) {
		if (isNewline)
			indent();
		super.write(buf);
	}

	public void write(char[] buf, int off, int len) {
		if (isNewline)
			indent();
		super.write(buf, off, len);
	}

	public void write(int c) {
		if (isNewline)
			indent();
		super.write(c);
	}

	public void write(String s) {
		if (isNewline)
			indent();
		super.write(s);
	}

	public void write(String s, int off, int len) {
		if (isNewline)
			indent();
		super.write(s, off, len);
	}

	private void indent() {
		isNewline = false;
		for (int i = 0; i < count; i++) {
			super.print(indentString);
		}
	}

}
