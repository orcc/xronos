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

import net.sf.openforge.lim.GlobalReset;

/**
 * ResetVar is a work-around to the fact that we do not have the internal GSR
 * signal to kick off the simulation. To emulate this behavior, we 'steal' the
 * first 5 cycles of the simulation and assert our internal RESET high. To
 * accomplish this we create an array of initial data for the reset signal and
 * increment the index into that array. This defines our internal RESET signal
 * and provides the necessary signal for starting our kickers and thus enabling
 * the circuit.
 * 
 * 
 * <p>
 * Created: Wed Mar 2 21:21:30 2005
 * 
 * @author imiller, last modified by $Author: imiller $
 * @version $Id: ResetVar.java 103 2006-02-15 21:35:17Z imiller $
 */
public class ResetVar extends IndexedVar implements StateVar {

	private GlobalReset rst;
	private int resetDelay;

	/**
	 * Constructs a new ResetVar based on the specified GlobalReset object (of
	 * which there should only be one, which is the RESET of our design).
	 * 
	 * @param grst
	 *            a value of type 'GlobalReset'
	 * @param cache
	 *            a CNameCache for name uniquifying
	 */
	public ResetVar(GlobalReset grst, CNameCache cache, int delay) {
		super(grst, grst.getBus(), cache);
		this.resetDelay = delay;
		this.rst = grst;
	}

	/**
	 * Writes declarations for any stateful variables necessary for this
	 * {@link StateVar} to the specified stream, including the array
	 * initialization data for the reset memory and the index counter for that
	 * array.
	 * 
	 * @param ps
	 *            a non-null PrintStream
	 * @throws NullPointerException
	 *             if ps is null
	 */
	public void declareGlobal(PrintStream ps) {
		String resetString = "{";
		for (int i = 0; i < this.resetDelay; i++)
			resetString += "1,";
		for (int i = 0; i < 2; i++)
			resetString += "0,";
		resetString += "0}";

		ps.println(StateVar.STORAGE_CLASS + "char " + getMemName() + "["
				+ (this.resetDelay + 3) + "] = " + resetString + ";");
		ps.println(StateVar.STORAGE_CLASS + "char " + getCountName() + " = 0;");
	}

	/**
	 * Writes update statements for the stateful variables of this
	 * {@link StateVar} to the specified stream.
	 * 
	 * @param ps
	 *            a non-null PrintStream
	 * @throws NullPointerException
	 *             if ps is null
	 */
	public void writeTick(PrintStream ps) {
		/*
		 * if (counter < DEPTH-1) counter++;
		 */
		ps.println("\tif (" + getCountName() + " < "
				+ (this.resetDelay + 3 - 1) + ") {");
		ps.println("\t\t" + getCountName() + "++;");
		ps.println("\t}");
	}

	public String getMemName() {
		return getBaseName();
	}

	public String getCountName() {
		return getBaseName() + "_count";
	}

	private String getBaseName() {
		return getDefaultBusName(this.rst.getBus());
	}

	protected String getIndex() {
		return this.getCountName();
	}

}// ResetVar
