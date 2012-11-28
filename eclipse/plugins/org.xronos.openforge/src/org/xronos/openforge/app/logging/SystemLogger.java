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

package org.xronos.openforge.app.logging;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.logging.Level;

import org.xronos.openforge.app.EngineThread;


/**
 * a wrapper for the ForgeLogger which can replace System.err and System.out in
 * user code user prints will be spooled up and sent in full lines to the
 * ForgeLogger.
 * 
 * note that the level of OUT and ERR are defined in println()
 * 
 * @author Jim Jensen
 * @version $Id: SystemLogger.java 2 2005-06-09 20:00:48Z imiller $
 */

public class SystemLogger extends PrintStream {

	private ForgeLogger logger;
	private Level level;
	private StringBuffer currentMsg;
	private boolean linePrinted = false;

	public SystemLogger(OutputStream out) {
		super(out);
		throw new RuntimeException(
				"SystemLogger must be instantiated with the SystemLogger(int warnLevel) constructor");
	}

	public SystemLogger(OutputStream out, boolean autoFlush) {
		super(out);
		throw new RuntimeException(
				"SystemLogger must be instantiated with the SystemLogger(int warnLevel) constructor");
	}

	public SystemLogger(OutputStream out, boolean autoFlush, String encoding) {
		super(out);
		throw new RuntimeException(
				"SystemLogger must be instantiated with the SystemLogger(int warnLevel) constructor");
	}

	/**
	 * constructs a SystemLogger to log at the appropriate level
	 * 
	 * @param level
	 *            one of verbose|info|warn|error
	 */
	public SystemLogger(String level) {
		this(EngineThread.getGenericJob().getLogger(), level);
	}

	public SystemLogger(ForgeLogger logger, String level) {
		super(new ByteArrayOutputStream()); // never used
		this.logger = logger;
		this.level = ForgeLogger.getLevelFromString(level);
		currentMsg = new StringBuffer(80);
	}

	/**
	 * not supported
	 */
	@Override
	public void flush() {
		println();
	}

	@Override
	public void close() {
		flush();
	}

	/**
	 * not supported
	 */
	@Override
	public boolean checkError() {
		return false;
	}

	// this is an oddity
	// essentially, this really doesn't work for anything but string output
	// so convert the write's into prints, converting to chars
	@Override
	public void write(int b) {
		print((char) b);
	}

	@Override
	public void write(byte buf[]) {
		write(buf, 0, buf.length);
	}

	@Override
	public void write(byte buf[], int off, int len) {
		for (int i = off; i < len; i++) {
			write(buf[i]);
		}
	}

	@Override
	public void print(boolean b) {
		currentMsg.append(b);
	}

	@Override
	public void print(char c) {
		// do line matching
		if (c == '\n')
			println();
		else
			currentMsg.append(c);
	}

	@Override
	public void print(int i) {
		currentMsg.append(i);
	}

	@Override
	public void print(long l) {
		currentMsg.append(l);
	}

	@Override
	public void print(float f) {
		currentMsg.append(f);
	}

	@Override
	public void print(double d) {
		currentMsg.append(d);
	}

	@Override
	public void print(char s[]) {
		currentMsg.append(s);
	}

	@Override
	public void print(String s) {
		currentMsg.append(s);
	}

	@Override
	public void print(Object obj) {
		currentMsg.append(obj);
	}

	@Override
	public void println() {
		logger.log(level, currentMsg.toString());
		linePrinted = true;
		currentMsg = new StringBuffer(80);
	}

	@Override
	public void println(boolean x) {
		currentMsg.append(x);
		println();
	}

	@Override
	public void println(char x) {
		currentMsg.append(x);
		println();
	}

	@Override
	public void println(int x) {
		currentMsg.append(x);
		println();
	}

	@Override
	public void println(long x) {
		currentMsg.append(x);
		println();
	}

	@Override
	public void println(float x) {
		currentMsg.append(x);
		println();
	}

	@Override
	public void println(double x) {
		currentMsg.append(x);
		println();
	}

	@Override
	public void println(char x[]) {
		currentMsg.append(x);
		println();
	}

	@Override
	public void println(String x) {
		currentMsg.append(x);
		println();
	}

	@Override
	public void println(Object x) {
		currentMsg.append(x);
		println();
	}

	public boolean hasPrinted() {
		return linePrinted;
	}
}
