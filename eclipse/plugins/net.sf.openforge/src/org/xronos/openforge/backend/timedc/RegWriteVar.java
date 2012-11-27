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

package org.xronos.openforge.backend.timedc;

import java.io.PrintStream;

import org.xronos.openforge.lim.Exit;
import org.xronos.openforge.lim.RegisterWrite;


/**
 * RegWriteVar maintains the state of the write, ie the go/done path.
 * 
 * 
 * <p>
 * Created: Thu Mar 10 10:11:11 2005
 * 
 * @author imiller, last modified by $Author: imiller $
 * @version $Id: RegWriteVar.java 99 2006-02-02 20:09:53Z imiller $
 */
public class RegWriteVar extends OpHandle implements StateVar {

	private final RegisterWrite write;
	private final RegisterVar var;

	public RegWriteVar(RegisterWrite regWrite, RegisterVar regVar,
			CNameCache cache) {
		super(regWrite, cache);
		this.write = regWrite;
		this.var = regVar;
	}

	@Override
	public void declareGlobal(PrintStream ps) {
		ps.println(StateVar.STORAGE_CLASS + "char " + getPendingIn() + " = 0;");
		ps.println(StateVar.STORAGE_CLASS + "char " + getPendingOut() + " = 0;");
	}

	@Override
	public void writeTick(PrintStream ps) {
		this.var.writeTick(ps);
		ps.println("\t" + getPendingOut() + " = " + getPendingIn() + ";");
	}

	public String getPendingIn() {
		return getPendingOut() + "_next";
	}

	public String getPendingOut() {
		return getBusName(this.write.getExit(Exit.DONE).getDoneBus());
	}

}// RegWriteVar
