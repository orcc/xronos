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
package net.sf.openforge.util;

import java.io.FilterWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.LinkedList;

/**
 * Helper debug class.
 * 
 */
public class GlobalDebug {

	/**
	 * Used for older debug statements you wish to keep, but not display. If no
	 * level is specified, these will not display.
	 */
	public final static long OLD = 0x4000000000000000l;
	public final static long LEGACY = OLD;

	/**
	 * Default set of levels enabled when visible is true.
	 */
	public final static long DEFAULT_LEVELS = 0x3fffffffffffffffl;

	private final long levels;
	private final String tag;
	private PrintWriter pw;
	private GlobalDebug.Writer dw;
	private boolean visible;

	public GlobalDebug(String tag, boolean visible, long levels, OutputStream os) {
		this.levels = levels;
		this.tag = tag;
		this.visible = visible;
		if (tag == null)
			throw new IllegalArgumentException(
					"Must specify tag for debug class!");
		pw = new PrintWriter(dw = new GlobalDebug.Writer(tag, os), true);
		if (visible) {
			pw.println(this);
		}
	}

	public GlobalDebug(String tag, boolean visible, long levels) {
		this(tag, visible, levels, System.out);
	}

	public GlobalDebug(String tag, boolean visible, OutputStream os) {
		this(tag, visible, DEFAULT_LEVELS, System.out);
	}

	public GlobalDebug(String tag, boolean visible) {
		this(tag, visible, System.out);
	}

	/**
	 * test if a level f or this debug set should be visible
	 * 
	 * @param level
	 *            a value of type 'long'
	 * @return a value of type 'boolean'
	 */
	private final boolean show(long level) {
		return (visible) && ((level & levels) != 0);
	}

	public final void ln(Object v) {
		ln(DEFAULT_LEVELS, v);
	}

	public final void ln() {
		ln(DEFAULT_LEVELS, "");
	}

	/**
	 * Workalike for println(Object), with level specified
	 * 
	 * @param level
	 *            level of this output
	 * @param v
	 *            object to .toString() and print
	 */
	public final void ln(long level, Object v) {
		if (show(level)) {
			pw.println(v);
		}
	}

	public final void ln(long level) {
		ln(level, "");
	}

	/**
	 * Workalike for print(Object); uses default levels
	 * 
	 * @param v
	 *            a value of type 'Object'
	 */
	public final void o(Object v) {
		o(DEFAULT_LEVELS, v);
	}

	/**
	 * Workalike for print(Object), with level specified
	 * 
	 * @param level
	 *            level of this output
	 * @param v
	 *            object to .toString() and print
	 */
	public final void o(long level, Object v) {
		if (show(level)) {
			pw.print(v.toString());
		}
	}

	public String toString() {
		return ("** Tag: [" + tag + "] is: " + (visible ? "ON" : "OFF")
				+ " Levels: 0x" + Long.toHexString(levels) + " **");
	}

	public static void inc() {
		GlobalDebug.Writer.inc();
	}

	public static void dec() {
		GlobalDebug.Writer.dec();
	}

	public static void decAll() {
		GlobalDebug.Writer.decAll();
	}

	/**
	 * Push a new print preface onto the stack and use it
	 * 
	 * @param s
	 *            a value of type 'String'
	 */
	public void pushPreface(String s) {
		dw.pushPreface(s);
	}

	/**
	 * Pop MRU preface off stack
	 * 
	 */
	public void popPreface() {
		dw.popPreface();
	}

	public final static class Writer extends FilterWriter {
		private boolean needsPreface = true;
		private LinkedList prefaces = new LinkedList();
		private String tag;

		public Writer(String tag, OutputStream os) {
			super(new OutputStreamWriter(os));
			pushPreface("");
			this.tag = tag;
		}

		/**
		 * Write a single character.
		 * 
		 * @exception IOException
		 *                If an I/O error occurs
		 */
		public void write(int c) throws IOException {
			if (needsPreface) {
				needsPreface = false;
				preface();
			}
			if (c != (int) '\n') {
				// System.out.print("{"+b+"}");
				super.write(c);
			}
			eolCheck(c);
		}

		/**
		 * Write a portion of an array of characters.
		 * 
		 * @param cbuf
		 *            Buffer of characters to be written
		 * @param off
		 *            Offset from which to start reading characters
		 * @param len
		 *            Number of characters to be written
		 * 
		 * @exception IOException
		 *                If an I/O error occurs
		 */
		public void write(char cbuf[], int off, int len) throws IOException {
			for (int i = 0; i < len; i++)
				write(cbuf[off + i]);
		}

		/**
		 * Write a portion of a string.
		 * 
		 * @param str
		 *            String to be written
		 * @param off
		 *            Offset from which to start reading characters
		 * @param len
		 *            Number of characters to be written
		 * 
		 * @exception IOException
		 *                If an I/O error occurs
		 */
		public void write(String str, int off, int len) throws IOException {
			for (int i = 0; i < len; i++)
				write(str.charAt(off + i));
		}

		private void preface() throws IOException {
			String s = tag + prefaces.getFirst() + ": " + getIndentString();
			write(s);
		}

		private void eolCheck(int c) {
			if (c == (int) '\n')
				needsPreface = true;
		}

		// ----------------------------------
		// Global indention management
		// ----------------------------------
		private static int cachedIndentCount = (-1);
		private static String cachedIndentString = "";
		private static int indentCount = 0;

		private static String getIndentString() {
			if (cachedIndentCount != indentCount) {
				cachedIndentString = "";
				for (int i = 0; i < indentCount; i++)
					cachedIndentString = cachedIndentString + "  ";
				cachedIndentCount = indentCount;
			}
			return cachedIndentString;
		}

		public static void inc() {
			indentCount++;
		}

		public static void dec() {
			indentCount = Math.max(0, indentCount - 1);
		}

		public static void decAll() {
			indentCount = 0;
		}

		/**
		 * Push a new print preface onto the stack and use it
		 * 
		 * @param s
		 *            a value of type 'String'
		 */
		public void pushPreface(String s) {
			prefaces.addFirst(s);
		}

		/**
		 * Pop MRU preface off stack
		 * 
		 */
		public void popPreface() {
			prefaces.removeFirst();
		}
	}
}
