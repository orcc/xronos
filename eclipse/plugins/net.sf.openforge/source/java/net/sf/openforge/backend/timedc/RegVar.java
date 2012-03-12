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

import net.sf.openforge.lim.Reg;

/**
 * RegVar maintains all the stateful variables for a simple flip-flop. The
 * stateful variables are only for current and next value. All the management of
 * enables, set, reset, etc are handled during the combinational phase such that
 * the 'next' value is always correct and will always be clocked through. In the
 * case of an enabled register whose enable is not correct, the next value is
 * simply set to the current value.
 * 
 * 
 * <p>
 * Created: Wed Mar 2 21:21:30 2005
 * 
 * @author imiller, last modified by $Author: imiller $
 * @version $Id: RegVar.java 2 2005-06-09 20:00:48Z imiller $
 */
public class RegVar extends OpHandle implements StateVar {

	private Reg reg;

	/**
	 * Constructs a new RegVar based on the specified Reg object.
	 * 
	 * @param reg
	 *            a non-null {@link Reg}
	 */
	public RegVar(Reg reg, CNameCache cache) {
		super(reg, cache);
		this.reg = reg;
	}

	/**
	 * Writes declarations for any stateful variables necessary for this
	 * {@link StateVar} to the specified stream, including the correct initial
	 * value for the flop.
	 * 
	 * @param ps
	 *            a non-null PrintStream
	 * @throws NullPointerException
	 *             if ps is null
	 */
	public void declareGlobal(PrintStream stream) {
		long initNum = 0;
		if (this.reg.getInitialValue() != null) {
			assert this.reg.getInitialValue().isConstant() : "Non constant initial value for reg";
			initNum = this.reg.getInitialValue().toNumber().numberValue()
					.longValue();
		}
		assert this.reg.getResultBus().getValue() != null;
		String declType = StateVar.STORAGE_CLASS
				+ getTypeDeclaration(this.reg.getResultBus().getValue());
		stream.println(declType + " " + getDataIn() + " = 0;");
		stream.println(declType + " " + getDataOut() + " = " + initNum + ";");
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
		ps.println("\t" + getDataOut() + " = " + getDataIn() + ";");
	}

	public String getDataIn() {
		return getDataOut() + "_next";
	}

	public String getDataOut() {
		return getBusName(this.reg.getResultBus());
	}

}// RegVar
