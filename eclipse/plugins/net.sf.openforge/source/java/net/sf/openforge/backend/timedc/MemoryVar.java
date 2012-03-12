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

package net.sf.openforge.backend.timedc;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import net.sf.openforge.lim.memory.LogicalMemoryPort;

/**
 * MemoryVar maintains all the stateful variables for a design level memory.
 * 
 * <p>
 * Created: Wed Mar 2 21:21:30 2005
 * 
 * @author imiller, last modified by $Author: imiller $
 * @version $Id: MemoryVar.java 99 2006-02-02 20:09:53Z imiller $
 */
class MemoryVar implements StateVar {
	private String baseName;
	private String init;
	private String memType; // The memory type, ie char, short, etc
	private List<LogicalMemoryPort> memPorts;
	private boolean tickWritten = false;

	public MemoryVar(String baseName, String init, String memType,
			Collection<LogicalMemoryPort> memoryPorts) {
		this.baseName = baseName;
		this.init = init;
		this.memType = memType;
		this.memPorts = new ArrayList<LogicalMemoryPort>(memoryPorts);
	}

	public void declareGlobal(PrintStream ps) {
		ps.println(StateVar.STORAGE_CLASS + this.memType + " " + this.baseName
				+ " [] = " + this.init + ";");
		// Now declare the memStateStruct for each memory port.
		for (LogicalMemoryPort memPort: memPorts) {
			String name = getStateStructName(memPort);
			ps.println(StateVar.STORAGE_CLASS + "struct memStateStruct " + name
					+ " = {0,0,0,0,0,0};");
			// ps.println("struct memStateStruct " + getStateStructName() +
			// " = {0,0,0,0,0,0};");
		}
	}

	public void writeTick(PrintStream ps) {
		if (!this.tickWritten) {
			this.tickWritten = true;
			for (LogicalMemoryPort memPort : this.memPorts) {
				String stateVar = this.getStateStructName(memPort);
				ps.println("\t" + stateVar + ".addr_current = " + stateVar
						+ ".addr_next;");
				/*
				 * if (we_next != 0 && en_next != 0) { if (size_next == 0) ((int
				 * *)memory)[addr_next/4] = data_next; else if (size_next == 1)
				 * ((char *)memory)[addr_next/1] = data_next; else if (size_next
				 * == 2) ((short *)memory)[addr_next/2] = data_next; else if
				 * (size_next == 3) ((long long *)memory)[addr_next/8] =
				 * data_next; } en_next = 0; we_next = 0; size_next = 0;
				 */
				ps.println("\tif ((" + stateVar + ".we_next != 0) && ("
						+ stateVar + ".en_next != 0)) {");
				ps.println("\t\tif (" + stateVar + ".size_next == 0) {");
				ps.println("\t\t\t((int *)" + getMemoryBaseName() + ")["
						+ stateVar + ".addr_current/4] = " + stateVar
						+ ".data_next;");
				ps.println("\t\t} else if (" + stateVar + ".size_next == 1) {");
				ps.println("\t\t\t((char *)" + getMemoryBaseName() + ")["
						+ stateVar + ".addr_current/1] = " + stateVar
						+ ".data_next;");
				ps.println("\t\t} else if (" + stateVar + ".size_next == 2) {");
				ps.println("\t\t\t((short *)" + getMemoryBaseName() + ")["
						+ stateVar + ".addr_current/2] = " + stateVar
						+ ".data_next;");
				ps.println("\t\t} else if (" + stateVar + ".size_next == 3) {");
				ps.println("\t\t\t((long long *)" + getMemoryBaseName() + ")["
						+ stateVar + ".addr_current/8] = " + stateVar
						+ ".data_next;");
				ps.println("\t\t}");
				ps.println("\t}");
				ps.println("\t" + stateVar + ".en_next = 0;");
				ps.println("\t" + stateVar + ".we_next = 0;");
				ps.println("\t" + stateVar + ".size_next = 0;");
			}
		}
	}

	public String getStateStructName(LogicalMemoryPort port) {
		String base = getMemoryBaseName() + "_state";
		if (this.memPorts.size() > 1) {
			assert this.memPorts.contains(port) : "Unknown memory port";
			base += "_port_" + this.memPorts.indexOf(port);
		}
		return base;
	}

	public String getMemoryBaseName() {
		return this.baseName;
	}

	/**
	 * Declare the types for the memory state struct. Note that the data_next
	 * variable is used for memory writes of all types, thus it must be large
	 * enough to handle any atomic memory write, so it is declared as an
	 * unsigned long long.
	 * 
	 * @param ps
	 *            the PrintStream to which the struct is declared.
	 */
	public static void declareType(PrintStream ps) {
		ps.println("struct memStateStruct {");
		ps.println("\tint addr_next;");
		ps.println("\tint addr_current;");
		ps.println("\tunsigned long long data_next;");
		ps.println("\tchar en_next;");
		ps.println("\tchar we_next;");
		ps.println("\tchar size_next;");
		ps.println("};");
	}
}
