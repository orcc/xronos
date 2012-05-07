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

package net.sf.openforge.verilog.mapping.memory;

import java.math.BigInteger;
import java.util.Collections;
import java.util.Iterator;

import net.sf.openforge.lim.memory.MemoryBank;
import net.sf.openforge.verilog.mapping.MappedModule;
import net.sf.openforge.verilog.model.Always;
import net.sf.openforge.verilog.model.Assign;
import net.sf.openforge.verilog.model.CaseBlock;
import net.sf.openforge.verilog.model.Decimal;
import net.sf.openforge.verilog.model.EventControl;
import net.sf.openforge.verilog.model.EventExpression;
import net.sf.openforge.verilog.model.HexConstant;
import net.sf.openforge.verilog.model.HexNumber;
import net.sf.openforge.verilog.model.Module;
import net.sf.openforge.verilog.model.NetDeclaration;
import net.sf.openforge.verilog.model.ProceduralTimingBlock;
import net.sf.openforge.verilog.model.Register;
import net.sf.openforge.verilog.model.SequentialBlock;
import net.sf.openforge.verilog.model.Statement;
import net.sf.openforge.verilog.model.Wire;
import net.sf.openforge.verilog.pattern.MemoryModule;

/**
 * SinglePortInferredRomWriter.java
 * 
 * 
 * <p>
 * Created: Tue Dec 3 10:40:14 2002
 * 
 * @author imiller, last modified by $Author: imiller $
 * @version $Id: SinglePortInferredRomWriter.java 70 2005-12-01 17:43:11Z
 *          imiller $
 */
public class SinglePortInferredRomWriter extends SinglePortRamWriter {

	public SinglePortInferredRomWriter(MemoryBank memory) {
		super(memory);

		// Set these ports to null so that they don't get written out
		// in the instantiation of the memory module.
		wenPort = null;
		dinPort = null;
		clkPort = null;
	}

	@Override
	public Module defineModule() {
		Module memoryModule = new MemoryModule(getName(),
				Collections.<MappedModule> emptySet());

		memoryModule.addPort(renPort);
		memoryModule.addPort(adrPort);
		memoryModule.addPort(doutPort);
		memoryModule.addPort(donePort);

		Wire dout = new Wire(doutPort.getIdentifier(), getDataWidth());

		memoryModule.declare(new NetDeclaration(dout));
		Register romData = new Register("rom_data", getDataWidth());
		memoryModule.declare(romData);

		EventExpression addrEvent = new EventExpression(adrPort);
		EventControl addrEventControl = new EventControl(addrEvent);

		CaseBlock caseBlock = new CaseBlock(adrPort);

		Iterator<BigInteger> initValueIter = getInitValuesByLine().iterator();
		for (int i = 0; i < getDepth() && initValueIter.hasNext(); i++) {
			// Object initValue = initValueIter.next();
			// String hex = HexString.valueToHex(initValue, getDataWidth());
			BigInteger initValue = initValueIter.next();
			String hexString = initValue.toString(16);
			Decimal caseValue = new Decimal(i, getAddrWidth());
			HexConstant hexInitValue = new HexConstant(hexString,
					getDataWidth());
			Statement caseStatement = new Assign.Blocking(romData,
					new HexNumber(hexInitValue));
			caseBlock.add(caseValue.toString(), caseStatement);
		}
		HexConstant unknown = new HexConstant("0", getDataWidth());
		caseBlock.add("default", new Assign.Blocking(romData, new HexNumber(
				unknown)));

		SequentialBlock sequentialBlock = new SequentialBlock(caseBlock);
		Always always = new Always(new ProceduralTimingBlock(addrEventControl,
				sequentialBlock));
		memoryModule.state(always);

		Statement assignOut = new Assign.Continuous(doutPort, romData);
		memoryModule.state(assignOut);
		memoryModule.state(new Assign.Continuous(donePort, renPort));

		return memoryModule;
	}

}// SinglePortInferredRomWriter
