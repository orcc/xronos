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

import net.sf.openforge.lim.Exit;
import net.sf.openforge.lim.memory.MemoryAccess;

/**
 * MemAccessVar maintains the various global variables for handling all types of
 * memory accesses (read or write). The variables it maintains are for the
 * access pending state of the access, which is essentially just a flop.
 * Additional methods are provided for assembling the correct syntax for reading
 * the memory and updating the state bearing variables.
 * 
 * 
 * <p>
 * Created: Wed Mar 2 21:21:30 2005
 * 
 * @author imiller, last modified by $Author: imiller $
 * @version $Id: MemAccessVar.java 99 2006-02-02 20:09:53Z imiller $
 */
public class MemAccessVar extends OpHandle implements StateVar {

	private final MemoryAccess acc;
	private final MemoryVar memVar;

	/**
	 * Constructs a new MemAccessVar based on the identified
	 * {@link MemoryAccess} and which is linked to the specific
	 * {@link CycleTranslator#MemoryVar}
	 * 
	 * @param acc
	 *            a non-null MemoryAccess
	 * @param memVar
	 *            a non-null MemoryVar
	 * @param cache
	 *            a CNameCache used to uniquify naming
	 */
	public MemAccessVar(MemoryAccess acc, MemoryVar memVar, CNameCache cache) {
		super(acc, cache);
		this.acc = acc;
		assert memVar != null : "Null MemoryVar";
		this.memVar = memVar;
	}

	/**
	 * Writes declarations for any stateful variables necessary for this
	 * {@link StateVar} to the specified stream.
	 * 
	 * @param ps
	 *            a non-null PrintStream
	 * @throws NullPointerException
	 *             if ps is null
	 */
	@Override
	public void declareGlobal(PrintStream ps) {
		ps.println(StateVar.STORAGE_CLASS + "char " + getPendingIn() + " = 0;");
		ps.println(StateVar.STORAGE_CLASS + "char " + getPendingOut() + " = 0;");
	}

	/**
	 * Writes update statements for the stateful variables of this
	 * {@link StateVar} to the specified stream
	 * 
	 * @param ps
	 *            a non-null PrintStream
	 * @throws NullPointerException
	 *             if ps is null
	 */
	@Override
	public void writeTick(PrintStream ps) {
		this.memVar.writeTick(ps);
		// For each access move the done_in->done_out
		ps.println("\t" + getPendingOut() + " = " + getPendingIn() + ";");
	}

	/**
	 * Returns a correctly formatted and name read access to the backing memory
	 * for this memory access, taking into account any necessary translation of
	 * the address based on access size.
	 * 
	 * @return a String of form: <code>((type *)memName)[addr/divisor]</code>
	 */
	public String getMemoryRead() {
		// output = ((type *)target)[addr/divisor];
		final int width = this.acc.getWidth();
		final String type = getTypeDeclaration(width, true);
		int divisor = 0;
		if (width <= 8)
			divisor = 1;
		else if (width <= 16)
			divisor = 2;
		else if (width <= 32)
			divisor = 4;
		else if (width <= 64)
			divisor = 8;
		assert divisor > 0 : "Illegal divisor in memory access";

		return ("((" + type + " *)" + this.memVar.getMemoryBaseName() + ")["
				+ getMemStateVar() + ".addr_current/" + divisor + "]");
	}

	public String getPendingIn() {
		return getPendingOut() + "_next";
	}

	public String getPendingOut() {
		return getBusName(this.acc.getExit(Exit.DONE).getDoneBus())
				+ "_current";
	}

	public String getMemStateVar() {
		return this.memVar.getStateStructName(acc.getMemoryPort());
	}

}// MemAccessVar
