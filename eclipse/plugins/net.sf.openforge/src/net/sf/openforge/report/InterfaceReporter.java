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

package net.sf.openforge.report;

import java.io.PrintStream;

import net.sf.openforge.app.EngineThread;
import net.sf.openforge.lim.io.BlockDescriptor;
import net.sf.openforge.lim.io.BlockElement;
import net.sf.openforge.lim.io.BlockIOInterface;

import org.eclipse.core.runtime.jobs.Job;

/**
 * InterfaceReporter is a very simple reporting mechanism for looking at the
 * data organization on the block interfaces.
 * 
 * 
 * <p>
 * Created: Fri Mar 5 08:48:11 2004
 * 
 * @author imiller, last modified by $Author: imiller $
 * @version $Id: InterfaceReporter.java 129 2006-04-05 13:40:46Z imiller $
 */
public class InterfaceReporter {

	/**
	 * Number of 'double' spaces to put at the beginning of each call to
	 * println.
	 */
	private int tabs = 0;

	/** The printstream we will use to output the report. */
	private PrintStream ps;

	/**
	 * Constructs a new Interface Reporter with the specified PrintStream to be
	 * used to output the report.
	 */
	public InterfaceReporter(PrintStream ps) {
		this.ps = ps;
	}

	/**
	 * Constructs a new Interface Reporter which will use the {@link Job#info}
	 * mechanism for outputting the report.
	 */
	public InterfaceReporter() {
		this(null);
	}

	public void reportStreams() {
		println("");
		println("Interface specification...");
		for (String fxn : BlockIOInterface.getFunctionNames()) {

			println("for Entry Function: " + fxn);
			indent();

			for (BlockDescriptor desc : BlockIOInterface.getDescriptors(fxn)) {
				println("IO Link " + desc.getInterfaceID() + ": "
						+ desc.getByteWidth() + " byte wide "
						+ (desc.isSlave() ? "input" : "output"));

				indent();
				BlockElement elems[] = desc.getBlockElements();

				/*
				 * Useful for debugging for (int i=0; i < elems.length; i++) {
				 * println("element " + i + " " + elems[i].getFormalName() +
				 * " allocated size " + elems[i].getAllocatedSize() +
				 * " stream length " + elems[i].getStreamFormat().length); }
				 * println("----------");
				 */

				int[] org = desc.getBlockOrganization();
				for (int i = 0; i < org.length;) {
					int index = org[i];
					int count = 1;
					while (((i + count) < org.length)
							&& org[i + count] == index) {
						count++;
					}
					println("block " + elems[index].getFormalName() + ": "
							+ count + " transfers");
					i += count;
				}
				outdent();
			}

			outdent();
		}
		println("");
	}

	private void println(String s) {
		if (s.length() > 0) {
			for (int i = 0; i < tabs; i++) {
				s = "  " + s;
			}
		}

		if (ps != null) {
			ps.println(s);
		} else {
			EngineThread.getGenericJob().info(s);
		}
	}

	private void indent() {
		tabs++;
	}

	private void outdent() {
		tabs--;
	}

}// InterfaceReporter
