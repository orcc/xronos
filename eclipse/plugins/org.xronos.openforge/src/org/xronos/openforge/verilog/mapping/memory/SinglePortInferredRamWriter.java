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

package org.xronos.openforge.verilog.mapping.memory;

import java.math.BigInteger;
import java.util.Collections;
import java.util.Iterator;

import org.xronos.openforge.lim.memory.MemoryBank;
import org.xronos.openforge.verilog.mapping.MappedModule;
import org.xronos.openforge.verilog.model.Always;
import org.xronos.openforge.verilog.model.Assign;
import org.xronos.openforge.verilog.model.Bitwise;
import org.xronos.openforge.verilog.model.ConditionalStatement;
import org.xronos.openforge.verilog.model.EventControl;
import org.xronos.openforge.verilog.model.EventExpression;
import org.xronos.openforge.verilog.model.Expression;
import org.xronos.openforge.verilog.model.HexConstant;
import org.xronos.openforge.verilog.model.HexNumber;
import org.xronos.openforge.verilog.model.Logical;
import org.xronos.openforge.verilog.model.MemoryDeclaration;
import org.xronos.openforge.verilog.model.MemoryElement;
import org.xronos.openforge.verilog.model.Module;
import org.xronos.openforge.verilog.model.ProceduralTimingBlock;
import org.xronos.openforge.verilog.model.Register;
import org.xronos.openforge.verilog.model.SequentialBlock;
import org.xronos.openforge.verilog.model.Statement;
import org.xronos.openforge.verilog.pattern.MemoryModule;
import org.xronos.openforge.verilog.pattern.SynopsysBlock;

/**
 * SinglePortInferredRamWriter.java
 * 
 * 
 * <p>
 * Created: Tue Dec 3 10:40:14 2002
 * 
 * @author imiller, last modified by $Author: imiller $
 * @version $Id: SinglePortInferredRamWriter.java 70 2005-12-01 17:43:11Z
 *          imiller $
 */
public class SinglePortInferredRamWriter extends SinglePortRamWriter {

	public static final boolean SEQUENTIAL_READ = false;

	public SinglePortInferredRamWriter(MemoryBank memory) {
		super(memory);
	}

	@Override
	public Module defineModule() {
		Module memoryModule = new MemoryModule(getName(),
				Collections.<MappedModule> emptySet());

		memoryModule.addPort(clkPort);
		memoryModule.addPort(renPort);
		memoryModule.addPort(wenPort);
		memoryModule.addPort(adrPort);
		memoryModule.addPort(dinPort);
		memoryModule.addPort(doutPort);
		memoryModule.addPort(donePort);

		Register ramReg = new Register("RAM", getDataWidth());
		MemoryDeclaration ram = new MemoryDeclaration(ramReg, getDepth() - 1, 0);
		memoryModule.declare(ram);

		EventExpression clk_event = new EventExpression.PosEdge(clkPort);
		EventControl ec = new EventControl(clk_event);

		SequentialBlock body = new SequentialBlock();
		Expression addr;
		if (SEQUENTIAL_READ) {
			Register readAddr = new Register("read_address", getAddrWidth());
			memoryModule.declare(readAddr);
			addr = readAddr;
			body.add(new Assign.NonBlocking(readAddr, adrPort));
		} else {
			addr = adrPort;
		}

		Expression condition = new Logical.And(renPort, wenPort);
		Statement trueBranch = new Assign.NonBlocking(new MemoryElement(ramReg,
				adrPort), dinPort);
		ConditionalStatement cs = new ConditionalStatement(condition,
				trueBranch);
		body.add(cs);

		// Done logic
		// Register doneReg = new Register("doneReg", 1);
		// body.add(new Assign.NonBlocking(doneReg, new Bitwise.Or(renPort,
		// wenPort)));
		Expression renDone = renPort;
		Expression wenDone = wenPort;
		if (getReadLatency().getMinClocks() > 0) {
			renDone = new Register("ren_done", 1);
			body.add(new Assign.NonBlocking((Register) renDone, renPort));
		}
		if (getWriteLatency().getMinClocks() > 0) {
			wenDone = new Register("wen_done", 1);
			body.add(new Assign.NonBlocking((Register) wenDone, wenPort));
		}
		memoryModule.state(new Assign.Continuous(donePort, new Bitwise.Or(
				renDone, wenDone)));

		Always always = new Always(new ProceduralTimingBlock(ec, body));
		memoryModule.state(always);

		Statement assignOut = new Assign.Continuous(doutPort,
				new MemoryElement(ramReg, addr));
		memoryModule.state(assignOut);

		// Memory Initialization
		SynopsysBlock initialBlock = new SynopsysBlock();
		initialBlock.append("initial begin");
		Iterator<BigInteger> initValueIter = getInitValuesByLine().iterator();
		for (int i = 0; i < getDepth() && initValueIter.hasNext(); i++) {
			// Number initValue = (Number)initValueIter.next();
			// String hexString = HexString.valueToHex(initValue,
			// getDataWidth());
			BigInteger initValue = initValueIter.next();
			String hexString = initValue.toString(16);
			HexNumber hexInitValue = new HexNumber(new HexConstant(hexString,
					getDataWidth()));
			Statement assignValueStatement = new Assign.NonBlocking(
					new MemoryElement(ramReg, i), hexInitValue);
			initialBlock.append(assignValueStatement.toString());
		}
		initialBlock.append("end");
		memoryModule.state(initialBlock);

		return memoryModule;
	}

}// SinglePortInferredRamWriter
