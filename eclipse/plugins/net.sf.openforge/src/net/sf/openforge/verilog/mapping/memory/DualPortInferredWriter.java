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

import net.sf.openforge.lim.memory.LogicalMemoryPort;
import net.sf.openforge.lim.memory.MemoryBank;
import net.sf.openforge.verilog.mapping.MappedModule;
import net.sf.openforge.verilog.model.Always;
import net.sf.openforge.verilog.model.Assign;
import net.sf.openforge.verilog.model.ConditionalStatement;
import net.sf.openforge.verilog.model.EventControl;
import net.sf.openforge.verilog.model.EventExpression;
import net.sf.openforge.verilog.model.Expression;
import net.sf.openforge.verilog.model.HexConstant;
import net.sf.openforge.verilog.model.HexNumber;
import net.sf.openforge.verilog.model.Logical;
import net.sf.openforge.verilog.model.MemoryDeclaration;
import net.sf.openforge.verilog.model.MemoryElement;
import net.sf.openforge.verilog.model.Module;
import net.sf.openforge.verilog.model.Net;
import net.sf.openforge.verilog.model.ProceduralTimingBlock;
import net.sf.openforge.verilog.model.Register;
import net.sf.openforge.verilog.model.SequentialBlock;
import net.sf.openforge.verilog.model.Statement;
import net.sf.openforge.verilog.pattern.MemoryModule;
import net.sf.openforge.verilog.pattern.SynopsysBlock;

/**
 * DualPortInferredWriter implements the defineModule method necessary for
 * creating an inferred (entirely verilog, no instantiated primitives) dual port
 * RAM or ROM.
 * 
 * <p>
 * Created: Tue Dec 3 15:20:03 2002
 * 
 * @author imiller, last modified by $Author: imiller $
 * @version $Id: DualPortInferredWriter.java 490 2007-06-15 16:37:00Z imiller $
 */
public class DualPortInferredWriter extends DualPortWriter {

	private static final boolean SEQUENTIAL_READ = false;

	public DualPortInferredWriter(MemoryBank memory) {
		super(memory);
	}

	public Module defineModule() {
		MemoryModule memoryModule = new MemoryModule(getName(),
				Collections.<MappedModule> emptySet());

		memoryModule.addPort(clkPort);
		memoryModule.addPort(mpA.ren);
		if (mpA.writes()) {
			memoryModule.addPort(mpA.wen);
			memoryModule.addPort(mpA.din);
		}
		memoryModule.addPort(mpB.ren);
		if (mpB.writes()) {
			memoryModule.addPort(mpB.wen);
			memoryModule.addPort(mpB.din);
		}
		memoryModule.addPort(mpA.adr);
		memoryModule.addPort(mpB.adr);
		if (mpA.reads()) {
			memoryModule.addPort(mpA.dout);
		}
		if (mpB.reads()) {
			memoryModule.addPort(mpB.dout);
		}
		memoryModule.addPort(mpA.done);
		memoryModule.addPort(mpB.done);

		// declares registers
		Net readAddrA = new Register("read_addressA", getAddrWidth());
		Net readAddrB = new Register("read_addressB", getAddrWidth());
		if (SEQUENTIAL_READ) {
			if (mpA.reads()) {
				memoryModule.declare(readAddrA);
			}
			if (mpB.reads()) {
				memoryModule.declare(readAddrB);
			}
		}
		Register ramReg = new Register("RAM", getDataWidth());
		MemoryDeclaration ram = new MemoryDeclaration(ramReg, getDepth() - 1, 0);
		memoryModule.declare(ram);

		SequentialBlock alwaysClockA = new SequentialBlock();
		Register weaDone = new Register("wea_done", 1);
		if (mpA.writes()) {
			if (!mpA.getWriteMode().equals(LogicalMemoryPort.READ_FIRST))
				throw new IllegalStateException(
						"Inferred dual port memories must be read first");

			Statement weaTrueBranch = new Assign.NonBlocking(new MemoryElement(
					ramReg, mpA.adr), mpA.din);
			Statement weaCS = new ConditionalStatement(mpA.wen, weaTrueBranch);
			SequentialBlock eaTrueBranch = new SequentialBlock(weaCS);
			if (SEQUENTIAL_READ) {
				if (mpA.reads()) {
					eaTrueBranch
							.add(new Assign.NonBlocking(readAddrA, mpA.adr));
				}
				alwaysClockA
						.add(new ConditionalStatement(mpA.ren, eaTrueBranch));
			}
			memoryModule.declare(weaDone);
			alwaysClockA.add(new Assign.NonBlocking(weaDone, mpA.wen));
		} else {
			if (SEQUENTIAL_READ) {
				if (mpA.reads()) {
					Statement trueBranch = new Assign.NonBlocking(readAddrA,
							mpA.adr);
					alwaysClockA.add(new ConditionalStatement(mpA.ren,
							trueBranch));
				}
			}
		}
		if (SEQUENTIAL_READ || mpA.writes()) {
			EventControl ecA = new EventControl(new EventExpression.PosEdge(
					clkPort));
			memoryModule.state(new Always(new ProceduralTimingBlock(ecA,
					alwaysClockA)));
		}

		EventExpression clkB_event = new EventExpression.PosEdge(clkPort);
		SequentialBlock alwaysClockB = new SequentialBlock();
		Register webDone = new Register("web_done", 1);
		if (mpB.writes()) {
			if (!mpB.getWriteMode().equals(LogicalMemoryPort.READ_FIRST))
				throw new IllegalStateException(
						"Inferred dual port memories must be read first");

			Statement webTrueBranch = new Assign.NonBlocking(new MemoryElement(
					ramReg, mpB.adr), mpB.din);
			Statement webCS = new ConditionalStatement(mpB.wen, webTrueBranch);
			SequentialBlock ebTrueBranch = new SequentialBlock(webCS);
			if (SEQUENTIAL_READ) {
				if (mpB.reads()) {
					ebTrueBranch
							.add(new Assign.NonBlocking(readAddrB, mpB.adr));
				}
				alwaysClockB
						.add(new ConditionalStatement(mpB.ren, ebTrueBranch));
			}
			memoryModule.declare(webDone);
			alwaysClockB.add(new Assign.NonBlocking(webDone, mpB.wen));
		} else {
			if (SEQUENTIAL_READ) {
				if (mpB.reads()) {
					Statement trueBranch = new Assign.NonBlocking(readAddrB,
							mpB.adr);
					alwaysClockB.add(new ConditionalStatement(mpB.ren,
							trueBranch));
				}
			}
		}
		if (SEQUENTIAL_READ || mpB.writes()) {
			EventControl ecB = new EventControl(clkB_event);
			memoryModule.state(new Always(new ProceduralTimingBlock(ecB,
					alwaysClockB)));
		}

		Expression addrA = SEQUENTIAL_READ ? readAddrA : mpA.adr;
		Expression addrB = SEQUENTIAL_READ ? readAddrB : mpB.adr;
		if (!mpA.writes()) // read only
		{
			memoryModule.state(new Assign.Continuous(mpA.done, mpA.ren));
			memoryModule.state(new Assign.Continuous(mpA.dout,
					new MemoryElement(ramReg, addrA)));
		} else if (!mpA.reads()) // write only
		{
			memoryModule.state(new Assign.Continuous(mpA.done, weaDone));
		} else {
			memoryModule.state(new Assign.Continuous(mpA.done, new Logical.Or(
					mpA.ren, weaDone)));
			memoryModule.state(new Assign.Continuous(mpA.dout,
					new MemoryElement(ramReg, addrA)));
		}

		if (!mpB.writes()) // read only
		{
			memoryModule.state(new Assign.Continuous(mpB.done, mpB.ren));
			memoryModule.state(new Assign.Continuous(mpB.dout,
					new MemoryElement(ramReg, addrB)));
		} else if (!mpB.reads()) // write only
		{
			memoryModule.state(new Assign.Continuous(mpB.done, weaDone));
		} else {
			memoryModule.state(new Assign.Continuous(mpB.done, new Logical.Or(
					mpB.ren, weaDone)));
			memoryModule.state(new Assign.Continuous(mpB.dout,
					new MemoryElement(ramReg, addrB)));
		}

		// Memory initialization
		SynopsysBlock initialBlock = new SynopsysBlock();
		initialBlock.append("initial begin");
		Iterator<BigInteger> initValueIter = getInitValuesByLine().iterator();
		for (int i = 0; i < getDepth() && initValueIter.hasNext(); i++) {
			// Number initValue = (Number)initValueIter.next();
			// String hexString = HexString.valueToHex(initValue,
			// getDataWidth());
			BigInteger initValue = (BigInteger) initValueIter.next();
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

}// DualPortInferredWriter
