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

import net.sf.openforge.lim.primitive.SRL16;

/**
 * SRL16Var maintains the stateful variables for an SRL16. This includes an
 * array for maintaining the stages of the SRL16 as well as a variable to
 * emulate the state of the enable signal.
 * 
 * 
 * <p>
 * Created: Wed Mar 2 21:21:30 2005
 * 
 * @author imiller, last modified by $Author: imiller $
 * @version $Id: SRL16Var.java 2 2005-06-09 20:00:48Z imiller $
 */
public class SRL16Var extends IndexedVar implements StateVar {
	private final SRL16 srl;

	/**
	 * Constructs a new SRL16Var based on the specified {@link SRL16}
	 * 
	 * @param reg
	 *            a non-null SRL16
	 * @param cache
	 *            a CNameCache for name uniquifying
	 */
	public SRL16Var(SRL16 reg, CNameCache cache) {
		super(reg, reg.getResultBus(), cache);
		this.srl = reg;
	}

	/**
	 * Writes declarations for any stateful variables necessary for this
	 * {@link StateVar} to the specified stream, including the array
	 * initialization data for the SRL stages and the enable variable. The SRL16
	 * is assumed to initialize to 0 for each stage. If the SRL16 is NOT an
	 * enabled SRL16, then the enable variable is set to 1.
	 * 
	 * @param ps
	 *            a non-null PrintStream
	 * @throws NullPointerException
	 *             if ps is null
	 */
	@Override
	public void declareGlobal(PrintStream stream) {
		assert this.srl.getResultBus().getValue() != null;
		String declType = StateVar.STORAGE_CLASS
				+ getTypeDeclaration(this.srl.getResultBus().getValue());
		int stages = getDepth();
		stream.println(declType + " " + getDataIn() + " = 0;");
		if (this.srl.getEnablePort().isUsed()) {
			stream.println(StateVar.STORAGE_CLASS + "char " + getEnable()
					+ " = 0;");
		} else {
			stream.println(StateVar.STORAGE_CLASS + "char " + getEnable()
					+ " = 1;");
		}
		String arrname = getDefaultBusName(this.srl.getResultBus());
		String arrDecl = declType + " " + arrname + "[" + stages + "] = {";
		for (int i = 0; i < stages; i++) {
			arrDecl += "0";
			if (i < (stages - 1))
				arrDecl += ", ";
		}
		arrDecl += "};";
		stream.println(arrDecl);
	}

	/**
	 * Writes update statements for the stateful variables of this
	 * {@link StateVar} to the specified stream. The state array is shifted by
	 * one stage if the enable signal is true.
	 * 
	 * @param ps
	 *            a non-null PrintStream
	 * @throws NullPointerException
	 *             if ps is null
	 */
	@Override
	public void writeTick(PrintStream ps) {
		/*
		 * if (enable) { int i; for (i=length-1; i > 0; i--) srl16[i] =
		 * srl16[i-1]; srl16[0] = next; }
		 */
		ps.println("\tif (" + getEnable() + ") {");
		ps.println("\t\tint i;");
		ps.println("\t\tfor (i=(" + (getDepth() - 1) + "); i > 0; i--) {");
		ps.println("\t\t\t" + getMemoryName() + "[i] = " + getMemoryName()
				+ "[i-1];");
		ps.println("\t\t}");
		ps.println("\t\t" + getMemoryName() + "[0] = " + getDataIn() + ";");
		ps.println("\t}");
	}

	public String getDataIn() {
		return getDefaultBusName(this.srl.getResultBus()) + "_next";
	}

	public String getEnable() {
		return getDefaultBusName(this.srl.getResultBus()) + "_enable";
	}

	public String getMemoryName() {
		return getDefaultBusName(this.srl.getResultBus());
	}

	@Override
	protected String getIndex() {
		return Integer.toString(getDepth() - 1);
	}

	public int getDepth() {
		int stages = this.srl.getStages();
		return stages;
	}

}// SRL16Var
