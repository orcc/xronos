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

import net.sf.openforge.lim.Register;
import net.sf.openforge.lim.memory.AddressStridePolicy;
import net.sf.openforge.util.naming.ID;

/**
 * RegisterVar maintains all the stateful variables for a design level register.
 * 
 * <p>
 * Created: Wed Mar 2 21:21:30 2005
 * 
 * @author imiller, last modified by $Author: imiller $
 * @version $Id: RegisterVar.java 116 2006-03-22 21:17:39Z imiller $
 */
class RegisterVar implements StateVar {
	public static final AddressStridePolicy ADDRESSING_POLICY = AddressStridePolicy.BYTE_ADDRESSING;

	private final Register reg;
	private final String regName;
	private boolean tickWritten = false;

	public RegisterVar(Register reg) {
		this.reg = reg;
		regName = CNameCache.getLegalIdentifier(ID.showLogical(reg)
				+ System.identityHashCode(reg));
	}

	@Override
	public void declareGlobal(PrintStream ps) {
		String type = StateVar.STORAGE_CLASS
				+ OpHandle.getTypeDeclaration(reg.getInitWidth(),
						Register.isSigned());
		long initValue = MemoryWriter.constantValue(reg.getInitialValue()
				.getRep(), ADDRESSING_POLICY);
		ps.println(type + " " + getDataOut() + " = " + initValue + ";");
		ps.println(type + " " + getDataIn() + " = 0;");
		ps.println(StateVar.STORAGE_CLASS + "char " + getEnable() + " = 0;");
	}

	@Override
	public void writeTick(PrintStream ps) {
		if (!tickWritten) {
			tickWritten = true;
			/*
			 * if (enable) dataout = dataIn; enable = 0;
			 */
			ps.println("\tif (" + getEnable() + ") {");
			ps.println("\t\t" + getDataOut() + " = " + getDataIn() + ";");
			ps.println("\t}");
			ps.println("\t" + getEnable() + " = 0;");
		}
	}

	String getDataIn() {
		return getBaseName() + "_next";
	}

	String getEnable() {
		return getBaseName() + "_en";
	}

	String getDataOut() {
		return getBaseName();
	}

	private String getBaseName() {
		return regName;
	}
}
