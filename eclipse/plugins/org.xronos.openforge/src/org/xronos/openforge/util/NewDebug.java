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
/*---  (c) ---*/

package org.xronos.openforge.util;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.StringReader;
import java.util.HashSet;
import java.util.Iterator;
import java.util.StringTokenizer;

/**
 * Helper debug class.
 * 
 * Include these lines in a class which is a good central point for the package:
 * 
 * <pre>
 * public final static boolean dbg=true|false;
 * public final static Debug d=new Debug(class,"MyTag",....,dbg);
 * </pre>
 * 
 * Then use the contruct if(<class>.debug) { <class>.d.ln("Stuff"); }
 * 
 * This allows for code to be compield out (is debug is false) or cmpiled in. If
 * it is compiled in, then debug output will be printed. However, you can
 * control the levels of debug by selecting and deselecting the bitmask of
 * levels via System.property settings. 2 settings are used:
 * 
 * In each case, first the System properties are checked, and if it is not found
 * there, the property name has it's periods (.) replaced with underscores (_)
 * and the environment is checked iff a Properties object was given at
 * construction.
 * 
 * Each line of output is of the form:
 * 
 * <pre>
 * <tag><preface>: <debug text>
 * </pre>
 * 
 * and multiple lines will be printed as individual lines.
 * 
 * @author cschanck
 * @version $Id: NewDebug.java 2 2005-06-09 20:00:48Z imiller $
 */
public class NewDebug {

	// ************************************************************
	// * *
	// * public fields *
	// * *
	// ************************************************************
	// NONE

	// ************************************************************
	// * *
	// * private/public fields *
	// * *
	// ************************************************************

	private OutputStream os = System.out;
	private PrintStream ps = new PrintStream(os);

	public final boolean compiledIn;
	private String tag = "UNKNOWN";
	private String preface = "";
	private String controlClassName;
	private int indentCount = 0;
	private HashSet<String> levelSet = null;
	private boolean levelInverted = false;
	private final static String DEFAULT_LEVEL = "! OLD";

	/**
	 * Describe constructor here.
	 * 
	 * @param controlClass
	 *            Class containing initialization of this object
	 * @param tag
	 *            Tag to use to preface each line of output
	 * @param compiledIn
	 *            true/false final value as to whether debug should be compiled
	 *            in
	 * @param os
	 *            OutputStream to output debug to
	 */
	public NewDebug(Class<TestClass> controlClass, String tag, OutputStream os,
			String levels, boolean compiledIn) {
		if (controlClass != null) {
			controlClassName = controlClass.getName();
		} else {
			controlClassName = "Unknown Class!";
		}

		if (tag == null)
			throw new IllegalArgumentException(
					"Must specify tag for debug class: " + controlClassName);
		this.compiledIn = compiledIn;
		this.tag = tag;
		recordLevelSet(levels);
		setOutputStream(os);
		checkActive();
	}

	/**
	 * Default indention of 1 tab
	 * 
	 * @param src
	 *            String to indent
	 * @return indeted string
	 */
	public static String indent(String src) {
		return indent("   ", src);
	}

	/**
	 * Indent a block of text, represented as a string, by a string. Multiline
	 * strings will be indented per line.
	 * 
	 * @param pad
	 *            text to use to indent
	 * @param src
	 *            source text to indent
	 * @return indented string
	 */
	public static String indent(String pad, String src) {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		PrintStream ps = new PrintStream(baos);

		BufferedReader br = new BufferedReader(new StringReader(src));

		try {
			for (String line = br.readLine(); line != null; line = br
					.readLine()) {
				ps.print(pad);
				ps.println(line);
			}
		} catch (IOException ioe) {
			System.err.println("In debug.indent: " + ioe);
		}

		ps.flush();

		return baos.toString();
	}

	private int cachedIndentCount = (-1);
	private String cachedIndentString = "";

	private String getIndentString() {
		if (cachedIndentCount != indentCount) {
			cachedIndentString = "";
			for (int i = 0; i < indentCount; i++)
				cachedIndentString = cachedIndentString + "  ";
			cachedIndentCount = indentCount;
		}
		return cachedIndentString;
	}

	public void inc() {
		indentCount++;
	}

	public void dec() {
		indentCount = Math.max(0, indentCount - 1);
	}

	public void decAll() {
		indentCount = 0;
	}

	/**
	 * Accessor for the output PrintStream
	 * 
	 * @return PrintStream
	 */
	public PrintStream getPrintStream() {
		return ps;
	}

	/**
	 * Set the current preface.
	 * 
	 * @param preface
	 *            String value for the preface.
	 */
	public final void setPreface(String preface) {
		this.preface = preface;
	}

	/**
	 * Work-alike for PrintStream.print();
	 * 
	 * @param v
	 *            Object to print out.
	 */
	public final void o(Object v) {
		o("", v);
	}

	/**
	 * Work-alike for PrintStream.print(Object);
	 * 
	 * @param level
	 *            level of this debug statement
	 * @param v
	 *            Object to print out.
	 */
	public void o(String level, Object v) {
		if ((compiledIn) && (checkLevel(level))) {
			ps.print(v.toString());
		}
	}

	/**
	 * Work-alike for PrintStream.println(Object); Uses the DEFAULT level.
	 * 
	 * @param v
	 *            Object to print out.
	 */
	public final void ln(Object v) {
		ln("", v);
	}

	/**
	 * Work-alike for PrintStream.println(Object);
	 * 
	 * @param level
	 *            level of this debug statement
	 * @param v
	 *            Object to print out.
	 */
	public void ln(String level, Object v) {
		if ((compiledIn) && (checkLevel(level))) {
			prefacedLn(v.toString());
		}
	}

	/**
	 * Used to display a stack trace from the current location.
	 * 
	 * @param v
	 *            Displayable object to makr where you are
	 */
	public final void whereAmI(Object v) {
		whereAmI("", v);
	}

	/**
	 * Used to display a stack trace from the current location.
	 * 
	 * @param v
	 *            Displayable object to makr where you are
	 * @param level
	 *            level of this debug statement
	 */
	public final void whereAmI(String level, Object v) {
		if ((compiledIn) && (checkLevel(level))) {
			whereAmI(v, ps);
		}
	}

	private void recordLevelSet(String s) {
		levelInverted = false;
		// if we have no levels, reset to null
		if (s == null) {
			s = DEFAULT_LEVEL;
		}
		// uppercase them, get rid of spaces
		s = s.trim().toUpperCase();
		// tokenize
		StringTokenizer st = new StringTokenizer(s);
		while (st.hasMoreTokens()) {
			String tok = st.nextToken();
			// inverted?
			if (tok.equals("!")) {
				levelInverted = true;
			} else {
				// if we have no set allocated, allocate it
				if (levelSet == null) {
					levelSet = new HashSet<String>();
				}
				// add it...
				levelSet.add(tok);
			}
		}
	}

	// check if a level string should be visible
	private final boolean checkLevel(String l) {
		boolean ret = false;
		// if we have no levels, pass everything...
		if (levelSet == null) {
			ret = true;
		} else {
			// if it i a multi, parse them out
			if (l.indexOf(' ') >= 0) {
				StringTokenizer st = new StringTokenizer(l);
				ret = levelInverted; // by default ....
				while (st.hasMoreTokens()) {
					String tok = st.nextToken();
					ret = levelSet.contains(tok.trim().toUpperCase());
					// clever, yes?
					if (ret != levelInverted) {
						break;
					}
				}
			} else {
				// matched?
				ret = levelSet.contains(l.trim().toUpperCase());
			}
		}
		// do we need to invert?
		if (levelInverted) {
			ret = !ret;
		}
		return ret;
	}

	/**
	 * Explicit call which works indepent of any debugging.
	 * 
	 * @param v
	 *            Object to display
	 * @param ps
	 *            PrintStream to write to
	 */
	public final static void whereAmI(Object v, PrintStream ps) {
		Throwable t = new Throwable("WhereAmI? --> " + v.toString());
		t.printStackTrace(ps);
	}

	// print the active status of this debug object...
	private void checkActive() {
		if (compiledIn) {
			ps.print(tag + " ** Debug Compiled In " + controlClassName);
			ps.print(" ** LEVELS:");
			if (levelInverted) {
				ps.print(" !");
			}
			if (levelSet == null) {
				ps.print(" ALL");
			} else {
				for (Iterator<String> it = levelSet.iterator(); it.hasNext();) {
					ps.print(" " + it.next());
				}
			}
			ps.println(" **");
		}
	}

	// properly print out a prefaced stringline
	private StringBuffer sb = new StringBuffer();

	private final void prefacedLn(String src) {
		BufferedReader br = new BufferedReader(new StringReader(src));

		sb.setLength(0);
		sb.append(tag);
		sb.append(preface);
		sb.append(": ");
		sb.append(getIndentString());
		int len = sb.length();
		try {
			for (String line = br.readLine(); line != null; line = br
					.readLine()) {
				sb.setLength(len);
				sb.append(line);
				ps.println(sb);
			}
			br.close();
		} catch (IOException ioe) {
			System.err.println("In debug.prefaced: " + ioe);
		}
	}

	/**
	 * Set the current OutputStream the Specified OutputStream.
	 * 
	 * Default is System.out.
	 * 
	 * @param newos
	 *            new OutputStream
	 */
	private void setOutputStream(OutputStream newos) {
		os = newos;
		ps.flush();
		ps = new PrintStream(os);
	}

	public static void main(String args[]) {
		new TestClass(args);
	}

}

class TestClass {
	public final static boolean dbg = true;
	public final static NewDebug d = new NewDebug(TestClass.class, "Debug Tag",
			System.out, "! test1", dbg);

	public TestClass(String[] args) {
		for (int i = 0; i < args.length; i++) {
			if (dbg) {
				d.ln("test1 test2", "testing1: " + args[i]);
			}
			if (dbg) {
				d.ln("test2", "testing2: " + args[i]);
			}
		}
	}
}
