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
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringReader;

import org.xronos.openforge.lim.Design;
import org.xronos.openforge.lim.Module;
import org.xronos.openforge.lim.Visitable;
import org.xronos.openforge.lim.graph.LDependencyGraph;
import org.xronos.openforge.lim.graph.LGraph;
import org.xronos.openforge.lim.graph.LWireGraph;
import org.xronos.openforge.lim.graph.LXGraph;
import org.xronos.openforge.lim.graph.LXModularGraph;


/**
 * Helper debug class.
 * 
 * Include these lines in a class which is a good central point for the package:
 * 
 * <pre>
 * public final static boolean debug=Debug.COMPILED_OUT;
 * public final static Debug d=new Debug("MyTag",debug,true|false,...);
 * </pre>
 * 
 * Then use the contruct if(<class>.debug) { <class>.d.ln("Stuff"); }
 * 
 * This allows for code to be compield out (is debug is false) or cmpiled in. If
 * it is compiled in, then debug output will be printed. However, you can
 * control the levels of debug by selecting and deselecting the bitmask of
 * levels via System.property settings. 2 settings are used:
 * 
 * debug.internal.<tag>=? true for visible, false for invisible
 * debug.internal.<tag>.levels=? integer bitmask of levels to display.
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
 * @version $Id: Debug.java 107 2006-02-23 15:46:07Z imiller $
 */
public class Debug {

	/**
	 * Literal constant for Compiling Out code
	 */
	public final static boolean COMPILED_OUT = false;

	/**
	 * Literal constant for Compiling In code
	 */
	public final static boolean COMPILED_IN = true;

	/**
	 * Literal constant for visible debug output
	 */
	public final static boolean VISIBLE = true;

	/**
	 * Literal constant for invisible debug output
	 */
	public final static boolean INVISIBLE = false;

	// default supplied levels

	/**
	 * Used for older debug statements you wish to keep, but not display. If no
	 * level is specified, these will not display.
	 */
	public final static int OLD = 0x40000000;
	public final static int LEGACY = 0x40000000;

	/**
	 * low level debug - not likely to be useful after the module is debugged
	 */
	public final static int LOW = 0x20000000;

	/**
	 * Used for temporary debug statements. If no level is specified, these will
	 * display.
	 */
	public final static int TEMP = 0x00000001;

	/**
	 * Default Debug level
	 */
	public final static int DEFAULT = 0x00000002;

	/**
	 * Default set of levels enabled when visible is true.
	 */
	public final static int DEFAULT_LEVELS = 0x3fffffff;

	private final static String propertyPreface = "debug.internal.";
	private final static String levelsSuffix = ".levels";

	// graph parameters - need to be the same as LGraph etc definitions
	/** show data connections */
	public static final int GR_DATA = 0x1;
	/** show control connections */
	public static final int GR_CONTROL = 0x2;
	/** show logical connections */
	public static final int GR_LOGICAL = 0x4;
	/** show structural connections */
	public static final int GR_STRUCTURAL = 0x8;
	/** used internally to denote any of the above */
	public static final int GR_DEPENDENCY = GR_DATA | GR_CONTROL | GR_LOGICAL
			| GR_STRUCTURAL;

	/** show clock & reset connections */
	public static final int GR_CLOCKRESET = 0x20;
	/** show physical connections (bus/port) */
	public static final int GR_PHYSICAL = 0x40;
	/**
	 * show detailed primitive contents: if set the insides of Selectors and
	 * other pass-through modules are to be graphed; if false, they will be
	 * graphed as opaque boxes
	 */
	public static final int GR_DETAIL = 0x80;
	/** print in landscape mode */
	public static final int GR_LANDSCAPE = 0x100;
	/** Include hashcode in compnent name */
	public static final int GR_HASHCODES = 0x200;

	public static final int GR_DEFAULT = GR_DATA | GR_CONTROL | GR_LOGICAL
			| GR_STRUCTURAL | GR_PHYSICAL;

	private int indentCount = 0;

	private ObjectInspector oi = new ObjectInspector();

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
	private boolean visible;
	private String tag = "UNKNOWN";
	private String preface = "";
	private String controlClassName;
	private int levels;

	// ************************************************************
	// * *
	// * Constructors *
	// * *
	// ************************************************************

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
	 * @param visible
	 *            true/false flag as to whether debug is displayed
	 * @param levels
	 *            debug level mask to use
	 * @param os
	 *            OutputStream to output debug to
	 */
	public Debug(Class<?> controlClass, String tag, boolean compiledIn,
			boolean visible, int levels, OutputStream os) {
		if (controlClass != null) {
			controlClassName = controlClass.getName();
		} else {
			controlClassName = "Unknown Class!";
		}

		if (tag == null)
			throw new IllegalArgumentException(
					"Must specify tag for debug class: " + controlClassName);
		this.compiledIn = compiledIn;
		this.visible = visible;
		this.tag = tag;
		this.levels = levels;
		setOutputStream(os);
		checkActive();
	}

	public Debug(Class<?> controlClass, String tag, boolean compiledIn,
			boolean visible, OutputStream os) {
		this(controlClass, tag, compiledIn, visible, DEFAULT_LEVELS, System.out);
	}

	public Debug(Class<?> controlClass, String tag, boolean compiledIn,
			boolean visible, int levels) {
		this(controlClass, tag, compiledIn, visible, levels, System.out);
	}

	public Debug(Class<?> controlClass, String tag, boolean compiledIn,
			boolean visible) {
		this(controlClass, tag, compiledIn, visible, System.out);
	}

	// ************************************************************
	// * *
	// * Public Static methods (main at bottom) *
	// * *
	// ************************************************************

	/**
	 * Default indention of 1 tab
	 * 
	 * @param src
	 *            String to indent
	 * @return indeted string
	 */
	public static String indent(String src) {
		return indent("\t", src);
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
	 * Convenience method for returning whether we are compiled in.
	 * 
	 * @return true if debug compied in, false otherwise.
	 */
	public final boolean on() {
		return compiledIn;
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
	 * use this to turn on (true) or off (false) debug
	 */
	public void setVisible(boolean vis) {
		visible = vis;
	}

	/**
	 * Set the debug bitmask levels
	 * 
	 * @param level
	 *            a value of type 'int'
	 */
	public void setLevels(int level) {
		levels = level;
	}

	/**
	 * Work-alike for PrintStream.println(); Uses the DEFAULT level.
	 * 
	 */
	public final void ln() {
		ln(DEFAULT);
	}

	/**
	 * Work-alike for PrintStream.print(); USes the DEFAULT level
	 * 
	 * @param v
	 *            Object to print out.
	 */
	public final void o(Object v) {
		o(DEFAULT, v);
	}

	/**
	 * Work-alike for PrintStream.println(Object); Uses the DEFAULT level.
	 * 
	 * @param v
	 *            Object to print out.
	 */
	public final void ln(Object v) {
		ln(DEFAULT, v);
	}

	/**
	 * Work-alike for PrintStream.println();
	 * 
	 * @param level
	 *            level of this debug statement
	 */
	public void ln(int level) {
		if (visible && compiledIn && ((levels & level) != 0)) {
			ps.println();
		}
	}

	/**
	 * Work-alike for PrintStream.print(Object);
	 * 
	 * @param level
	 *            level of this debug statement
	 * @param v
	 *            Object to print out.
	 */
	public void o(int level, Object v) {
		if (visible && compiledIn && ((levels & level) != 0)) {
			ps.print(v.toString());
		}
	}

	/**
	 * Work-alike for PrintStream.println(Object);
	 * 
	 * @param level
	 *            level of this debug statement
	 * @param v
	 *            Object to print out.
	 */
	public void ln(int level, Object v) {
		if (visible && compiledIn && ((levels & level) != 0)) {
			prefacedLn(v.toString());
		}
	}

	/**
	 * Used to display a stack trace from the current location. Uses level
	 * DEFAULT.
	 * 
	 * @param v
	 *            Displayable object to makr where you are
	 */
	public final void whereAmI(Object v) {
		whereAmI(DEFAULT, v);
	}

	/**
	 * Used to display a stack trace from the current location. Uses level
	 * DEFAULT.
	 * 
	 * @param v
	 *            Displayable object to makr where you are
	 * @param level
	 *            level of this debug statement
	 */
	public final void whereAmI(int level, Object v) {
		if (visible && compiledIn && ((levels & level) != 0)) {
			whereAmI(v, ps);
		}
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

	/**
	 * write out a html page describing an object
	 * 
	 * @param o
	 *            object of interest
	 * @param file
	 *            filename to write
	 */
	public void inspect(Object o, String fileName) {
		if (visible) {
			try {
				oi.output(o, new FileWriter(fileName));
			} catch (Exception e) {
				System.err.println("Could not write " + fileName
						+ " to inspect " + o.getClass().getName() + ": error: "
						+ e);
			}
		}
	}

	/**
	 * write out a graphviz file describing an object
	 * 
	 * @param o
	 *            object of interest
	 * @param title
	 *            title of the graph
	 * @param file
	 *            filename to write (process with dotty after running)
	 * @param landscape
	 *            true if graph should be printed in landscape mode
	 */
	public void graph(Visitable o, String title, String fileName, int flags) {
		if (visible) {
			lgraphTo(o, title, fileName, flags);
		}
	}

	/**
	 * write out a graphviz file describing an object
	 * 
	 * @param o
	 *            object of interest
	 * @param title
	 *            title of the graph
	 * @param file
	 *            filename to write (process with dotty after running)
	 * @param landscape
	 *            true if graph should be printed in landscape mode
	 */
	public void lxgraph(Module o, String title, String fileName) {
		if (visible) {
			lxGraphTo(o, title, fileName);
		}
	}

	public void lxgraph(Design o, String fileName) {
		if (visible) {
			lxGraphTo(o, "", fileName);
		}
	}

	/**
	 * write a graphviz file describing the wire connections of an object
	 * 
	 * @param o
	 *            object of interest
	 * @param title
	 *            of graph
	 * @param file
	 *            to write to
	 */
	public void wireGraph(Visitable o, String title, String filename) {
		if (visible) {
			wireGraphTo(o, title, filename);
		}
	}

	public void dependencyGraph(Visitable o, String title, String fileName,
			int flags) {
		if (visible) {
			depGraphTo(o, title, fileName, flags);
		}
	}

	public void launchGraph(Visitable o) {
		launchGraph(o, "no title", GR_DEFAULT, false);
	}

	public void launchGraph(Visitable o, String title, int flags) {
		launchGraph(o, title, flags, true);
	}

	public void launchGraph(Visitable o, String title, int flags, boolean wait) {
		if (visible && o != null) {
			try {
				File file = File.createTempFile("debugDotty", ".dot");
				file.deleteOnExit();
				LDependencyGraph lgraph = new LDependencyGraph(title, o, flags);
				lgraph.print(new PrintWriter(new FileOutputStream(file)));
				String fileName = file.getAbsolutePath();
				System.out.println("Launching dotty on " + fileName);
				Process launch = Runtime.getRuntime().exec("dotty " + fileName);
				if (wait) {
					launch.waitFor();
				}
			} catch (Exception exception) {
				System.out.println("Excpetion: " + exception);
			}
		}
	}

	public void launchXGraph(Module top, String title, boolean wait) {
		launchXGraph(top, title, wait, -1);
	}

	public void launchXGraph(Module top, String title, boolean wait, int layers) {
		if (visible && top != null) {
			try {
				File file = File.createTempFile("xDebugDotty", ".dot");
				file.deleteOnExit();
				LXGraph graph = new LXGraph(title, top, 12, layers);
				graph.print(new PrintWriter(new FileOutputStream(file)));
				String fileName = file.getAbsolutePath();
				System.out.println("Launching dotty on " + fileName);
				Process launch = Runtime.getRuntime().exec("dotty " + fileName);
				if (wait) {
					launch.waitFor();
				}
			} catch (Exception exception) {
				System.out.println("Excpetion: " + exception);
				exception.printStackTrace();
			}
		}
	}

	public void launchXGraph(Design top, boolean wait) {
		if (visible && top != null) {
			try {
				File file = File.createTempFile("xDebugDotty", ".dot");
				file.deleteOnExit();
				LXGraph graph = new LXGraph(top, 12);
				graph.print(new PrintWriter(new FileOutputStream(file)));
				String fileName = file.getAbsolutePath();
				System.out.println("Launching dotty on " + fileName);
				Process launch = Runtime.getRuntime().exec("dotty " + fileName);
				if (wait) {
					launch.waitFor();
				}
			} catch (Exception exception) {
				System.out.println("Excpetion: " + exception);
				exception.printStackTrace();
			}
		}
	}

	public void graphTo(Design top, String fileName) {
		LXGraph.graphTo(top, fileName);
	}

	/**
	 * write out a LGraph graphviz file describing an object
	 * 
	 * @param o
	 *            object of interest
	 * @param title
	 *            title of the graph
	 * @param file
	 *            filename to write (process with dotty after running)
	 * @param landscape
	 *            true if graph should be printed in landscape mode
	 */
	public static void lgraphTo(Visitable o, String title, String fileName,
			int flags) {
		try {
			LGraph lgraph = new LGraph(title, o, flags);
			lgraph.print(new PrintWriter(new FileOutputStream(fileName)));
		} catch (FileNotFoundException exception) {
			System.err.println("Could not write " + fileName + " to graph "
					+ o.getClass().getName() + ": error: " + exception);
		}
	}

	/**
	 * write out an lgraph based on a design
	 * 
	 * @param o
	 *            a object of either Design or Module
	 * @param fileName
	 *            a value of type 'String'
	 */
	public static void lxGraphTo(Object o, String title, String fileName) {
		try {
			LXGraph lgraph;
			if (o instanceof Design)
				lgraph = new LXGraph((Design) o, 12);
			else
				lgraph = new LXGraph(title, (Module) o, 12);
			lgraph.print(new PrintWriter(new FileOutputStream(fileName)));
		} catch (FileNotFoundException exception) {
			System.err.println("Could not write " + fileName + " to graph "
					+ o.getClass().getName() + ": error: " + exception);
		}
	}

	/**
	 * write an LWireGraph graphviz file describing the wire connections of an
	 * object
	 * 
	 * @param o
	 *            object of interest
	 * @param title
	 *            of graph
	 * @param file
	 *            to write to
	 */
	public static void wireGraphTo(Visitable o, String title, String filename) {
		try {
			LWireGraph lgraph = new LWireGraph(title, o, LWireGraph.CONTROL);
			lgraph.print(new PrintWriter(new FileOutputStream(filename)));
		} catch (FileNotFoundException exception) {
			System.err.println("Could not write " + filename + " to graph "
					+ o.getClass().getName() + ": error: " + exception);
		}
	}

	/**
	 * Write out a LDependency graph
	 * 
	 * @param o
	 *            a value of type 'Visitable'
	 * @param title
	 *            a value of type 'String'
	 * @param fileName
	 *            a value of type 'String'
	 * @param flags
	 *            a value of type 'int'
	 */
	public static void depGraphTo(Visitable o, String title, String fileName,
			int flags) {
		try {
			LDependencyGraph lgraph = new LDependencyGraph(title, o, flags);
			lgraph.print(new PrintWriter(new FileOutputStream(fileName)));
		} catch (FileNotFoundException exception) {
			System.err.println("Could not write " + fileName + " to graph "
					+ o.getClass().getName() + ": error: " + exception);
		}
	}

	public static void modGraph(Visitable o, String dir) {
		File dirFile = new File(dir);
		dirFile.mkdirs();
		LXModularGraph lxm = new LXModularGraph(dirFile);
		o.accept(lxm);
	}

	// ************************************************************
	// * *
	// * Public Instance Accessor methods (get/set/is) *
	// * *
	// ************************************************************
	// NONE

	// ************************************************************
	// * *
	// * Public Instance methods *
	// * *
	// ************************************************************
	// NONE

	// ************************************************************
	// * *
	// * Public Static methods *
	// * *
	// ************************************************************
	// NONE

	// ************************************************************
	// * *
	// * Protected Instance methods *
	// * *
	// ************************************************************
	// NONE

	// ************************************************************
	// * *
	// * Private Static methods *
	// * *
	// ************************************************************
	// NONE

	// ************************************************************
	// * *
	// * Private Instance methods *
	// * *
	// ************************************************************

	private String lastPropertyName = "";

	private String getProperty(String suffix) {
		lastPropertyName = propertyPreface + suffix;
		lastPropertyName = lastPropertyName.toLowerCase();
		String result = System.getProperty(lastPropertyName);
		if (result == null) {
			lastPropertyName = lastPropertyName.replace('.', '_');
			lastPropertyName = lastPropertyName.toUpperCase();
			result = Environment.getEnv(lastPropertyName);
		}
		return result;
	}

	private void checkActive() {
		if (compiledIn) {
			ps.println(tag + " ** Debug Info Potentially Compiled In Class: "
					+ controlClassName + " **");
			String result = getProperty(tag);
			if (result != null) {
				visible = Boolean.valueOf(result).booleanValue();
				ps.println(tag + " ** Property " + lastPropertyName
						+ " overriding: visible=" + visible + " **");
			}
			if (visible) {
				// now check for levels
				result = getProperty(tag + levelsSuffix);
				if (result != null) {
					levels = getIntegerValue(result);
					ps.println(tag + " ** Property " + lastPropertyName
							+ " overriding: levels=0x" + HF.hex(levels) + " **");
				}

			}
			ps.println(tag + " ** Debug [" + tag + "] visible: " + visible
					+ "; levels: 0x" + HF.hex(levels) + " **");
		}
	}

	private int getIntegerValue(String s) {
		s = s.toLowerCase();
		if (s.startsWith("-"))
			throw new IllegalArgumentException(
					"Invalid value for debug level: " + s);
		if (s.startsWith("0x")) {
			// parse as hex
			s = s.substring(2);
			return Integer.parseInt(s, 16);
		} else {
			return Integer.parseInt(s);
		}
	}

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

	// ************************************************************
	// * *
	// * Public static void main(String[]) *
	// * *
	// ************************************************************
	// NONE

}
