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

package org.xronos.openforge.util.naming;

import java.io.PrintStream;

import org.xronos.openforge.util.Debug;


/**
 * The naming package is designed to provide a consistent, human readable,
 * unique naming framework for an entire system. Basically it is used like this.
 * <P>
 * 
 * Classes which need to have logical ids, should all extend ID.
 * <P>
 * 
 * To display the Global or Logical id's for a given object, use
 * ID.showLogical(o) or ID.showGLobal(o);
 * 
 */
public class _naming {

	public final static String TAG = "Naming";
	public final static boolean db = Debug.COMPILED_OUT;
	public final static boolean VISIBLE = true;

	private final static Debug _debug = new Debug(_naming.class, TAG, db,
			VISIBLE);

	/**
	 * Accessor for the output PrintStream
	 * 
	 * @return PrintStream
	 */
	public static PrintStream getPrintStream() {
		return _debug.getPrintStream();
	}

	/**
	 * Set the current preface.
	 * 
	 * @param preface
	 *            String value for the preface.
	 */
	public static void setPreface(String preface) {
		_debug.setPreface(preface);
	}

	/**
	 * use this to turn on (true) or off (false) _debug
	 */
	public static void setVisible(boolean vis) {
		_debug.setVisible(vis);
	}

	/**
	 * Set the _debug bitmask levels
	 * 
	 * @param level
	 *            a value of type 'int'
	 */
	public static void setLevels(int level) {
		_debug.setLevels(level);
	}

	/**
	 * Work-alike for PrintStream.println(); Uses the DEFAULT level.
	 * 
	 */
	public static void ln() {
		_debug.ln();
	}

	/**
	 * Work-alike for PrintStream.print(); USes the DEFAULT level
	 * 
	 * @param v
	 *            Object to print out.
	 */
	public static void o(Object v) {
		_debug.o(v);
	}

	/**
	 * Work-alike for PrintStream.println(Object); Uses the DEFAULT level.
	 * 
	 * @param v
	 *            Object to print out.
	 */
	public static void ln(Object v) {
		_debug.ln(v);
	}

	/**
	 * Work-alike for PrintStream.println();
	 * 
	 * @param level
	 *            level of this _debug statement
	 */
	public static void ln(int level) {
		_debug.ln(level);
	}

	/**
	 * Work-alike for PrintStream.print(Object);
	 * 
	 * @param level
	 *            level of this _debug statement
	 * @param v
	 *            Object to print out.
	 */
	public static void o(int level, Object v) {
		_debug.o(level, v);
	}

	/**
	 * Work-alike for PrintStream.println(Object);
	 * 
	 * @param level
	 *            level of this _debug statement
	 * @param v
	 *            Object to print out.
	 */
	public static void ln(int level, Object v) {
		_debug.ln(level, v);
	}

	/**
	 * Used to display a stack trace from the current location. Uses level
	 * DEFAULT.
	 * 
	 * @param v
	 *            Displayable object to makr where you are
	 */
	public static void whereAmI(Object v) {
		_debug.whereAmI(v);
	}

	/**
	 * Used to display a stack trace from the current location. Uses level
	 * DEFAULT.
	 * 
	 * @param v
	 *            Displayable object to makr where you are
	 * @param level
	 *            level of this _debug statement
	 */
	public static void whereAmI(int level, Object v) {
		_debug.whereAmI(level, v);
	}
}
