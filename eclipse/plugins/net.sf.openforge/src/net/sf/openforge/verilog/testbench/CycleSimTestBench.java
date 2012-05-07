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

package net.sf.openforge.verilog.testbench;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.sf.openforge.app.EngineThread;
import net.sf.openforge.forge.api.internal.Core;
import net.sf.openforge.forge.api.pin.Buffer;
import net.sf.openforge.forge.api.pin.ClockDomain;
import net.sf.openforge.forge.api.pin.ClockPin;
import net.sf.openforge.forge.api.sim.pin.PinSimData;
import net.sf.openforge.forge.api.sim.pin.SignalValue;
import net.sf.openforge.lim.BidirectionalPin;
import net.sf.openforge.lim.Bus;
import net.sf.openforge.lim.Call;
import net.sf.openforge.lim.Component;
import net.sf.openforge.lim.Design;
import net.sf.openforge.lim.Exit;
import net.sf.openforge.lim.InputPin;
import net.sf.openforge.lim.OutputPin;
import net.sf.openforge.lim.Pin;
import net.sf.openforge.lim.Port;
import net.sf.openforge.lim.Task;
import net.sf.openforge.util.naming.ID;
import net.sf.openforge.verilog.model.Always;
import net.sf.openforge.verilog.model.Assign;
import net.sf.openforge.verilog.model.BinaryConstant;
import net.sf.openforge.verilog.model.Comment;
import net.sf.openforge.verilog.model.Compare;
import net.sf.openforge.verilog.model.ConditionalStatement;
import net.sf.openforge.verilog.model.Constant;
import net.sf.openforge.verilog.model.Decimal;
import net.sf.openforge.verilog.model.DelayStatement;
import net.sf.openforge.verilog.model.Directive;
import net.sf.openforge.verilog.model.EventControl;
import net.sf.openforge.verilog.model.EventExpression;
import net.sf.openforge.verilog.model.Expression;
import net.sf.openforge.verilog.model.FStatement;
import net.sf.openforge.verilog.model.HexConstant;
import net.sf.openforge.verilog.model.HexNumber;
import net.sf.openforge.verilog.model.InitialBlock;
import net.sf.openforge.verilog.model.InitializedMemory;
import net.sf.openforge.verilog.model.InlineComment;
import net.sf.openforge.verilog.model.Inout;
import net.sf.openforge.verilog.model.Input;
import net.sf.openforge.verilog.model.Keyword;
import net.sf.openforge.verilog.model.MemoryElement;
import net.sf.openforge.verilog.model.Module;
import net.sf.openforge.verilog.model.ModuleInstance;
import net.sf.openforge.verilog.model.Net;
import net.sf.openforge.verilog.model.Output;
import net.sf.openforge.verilog.model.PortConnection;
import net.sf.openforge.verilog.model.ProceduralTimingBlock;
import net.sf.openforge.verilog.model.QualifiedNet;
import net.sf.openforge.verilog.model.Register;
import net.sf.openforge.verilog.model.SequentialBlock;
import net.sf.openforge.verilog.model.Statement;
import net.sf.openforge.verilog.model.StringStatement;
import net.sf.openforge.verilog.model.Unary;
import net.sf.openforge.verilog.model.VerilogDocument;
import net.sf.openforge.verilog.model.Wire;
import net.sf.openforge.verilog.pattern.CommaDelimitedStatement;
import net.sf.openforge.verilog.pattern.IncludeStatement;
import net.sf.openforge.verilog.pattern.StatementBlock;
import net.sf.openforge.verilog.translate.PrettyPrinter;

/**
 * CycleSimTestBench creates a testbench framework in which the user can
 * populate exactly what values are applied to each input each clock cycle
 * (including reset) and what values are to be expected from each output on each
 * clock cycle.
 * 
 * The basic structures created are:
 * <ul>
 * <li>1 memory for each pin
 * <li>code to apply the value from the memory to each input pin every clock
 * cycle.
 * <li>code to verify the value on each output pin against the value in the
 * memory every clock cycle.
 * </ul>
 * 
 * 
 * <p>
 * Created: Tue Oct 29 14:10:48 2002
 * 
 * @author imiller, last modified by $Author: imiller $
 * @version $Id: CycleSimTestBench.java 282 2006-08-14 21:25:33Z imiller $
 */
public class CycleSimTestBench {

	// This static is used to uniquely name the test bench modules
	// produced within the same run of forge so they can all be run together
	private static int uniqueID = 1;

	private int cycleCount;
	private ClockPin cycleCountClockPin;
	private VerilogDocument simulationDocument;

	public CycleSimTestBench(Design design, File source) {
		String name = source.getName();
		name = name.substring(0, name.length() - 2);
		File resFile = new File(source.getParent(), name + ".results");
		File clkFile = new File(source.getParent(), name + ".results.clocks");

		ResultFile resultFile = new ResultFile(resFile);
		ResultFile clkCntFile = new ResultFile(clkFile);

		Map<Pin, ClockPin> pinToClockPinMap = getPinToClockPinMap(design);

		// get the cycle count lenght and clock pin this sim should run for
		CycleCount cc = findCycleCount(design, pinToClockPinMap);
		cycleCount = cc.getCycleCount();
		// cycleCountClockPin = cc.getClockPin();

		// this set is all the clocks the user declared in their test
		// bench (pin sim), they might not actually be used by the
		// design, and if that is the case, then we need to create pin
		// logic for them.
		Set<ClockPin> clocksUsed = cc.getClocksUsed();

		if (cycleCount < 0) {
			design.getEngine()
					.getGenericJob()
					.warn("No pin found with cycle simulation clock limit.  No testbench written");
			simulationDocument = new DefaultSimDocument(
					"No Cycle Count Limit Specified On Any Pin",
					Collections.singletonList(resFile));
			return;
		}

		int indexSize = (int) java.lang.Math.ceil(java.lang.Math
				.log(cycleCount + 1) / java.lang.Math.log(2));
		Net index = new Register("testbench_index", indexSize);

		simulationDocument = new VerilogDocument();
		addHeader(simulationDocument);

		// add the timescale directive
		simulationDocument.append(new Directive.TimeScale(1, Keyword.NS, 100,
				Keyword.PS));
		simulationDocument.append(new Comment(Comment.BLANK));

		addInclude(simulationDocument, source);

		// add a second timescale directive after the includes to
		// prevent any of them from changing our timescale
		simulationDocument.append(new Directive.TimeScale(1, Keyword.NS, 100,
				Keyword.PS));
		simulationDocument.append(new Comment(Comment.BLANK));

		// get a module
		Module testModule = new Module("fixture_" + uniqueID++);

		// Generate a PinLogic instance for each pin.
		Map<Pin, PinLogic> pinToLogic = buildPinLogic(design, resultFile,
				clocksUsed);

		// Instantiate the design. Hook up each input to a register
		// and each output to a wire. Pass the clock logic explicitly
		instantiate(design, testModule, pinToLogic);

		// CHEAT!!! Install a commented out VCD generation command.
		testModule.state(new InlineComment(
				"initial begin  $dumpfile(\"waves.vcd\");  $dumpvars;end",
				Comment.SHORT));

		// Remove the clocks so we don't create logic for it. Need to
		// copy the map to prevent concurrent modification errors.
		Map<Pin, PinLogic> cpPinToLogic = new HashMap<Pin, PinLogic>(pinToLogic);
		Map<Object, PinLogic> clkPinToLogic = new HashMap<Object, PinLogic>();

		// while looping, create clock specific indexes and store in
		// map
		Map<PinLogic, Net> clkPinLogicToIndex = new HashMap<PinLogic, Net>();

		for (Pin pin : cpPinToLogic.keySet()) {
			Object o = pin;

			PinLogic pl = pinToLogic.get(o);

			if (pl instanceof ClkPinLogic) {
				pinToLogic.remove(o);
				clkPinToLogic.put(o, pl);

				Net index_clock = new Register("testbench_index_"
						+ pl.getName(), 32);
				clkPinLogicToIndex.put(pl, index_clock);
			}
		}

		for (Object o : clkPinToLogic.keySet()) {
			Pin clock = (Pin) o;
			ClkPinLogic cpl = (ClkPinLogic) clkPinToLogic.get(clock);

			// Create an always @(posedge clock) block for all the 'stuff'
			SequentialBlock block = new SequentialBlock();

			// only add the cycle count finish stuff to the correct
			// clock block
			if (clock.getApiPin().equals(cycleCountClockPin)) {
				block.add(new Assign.NonBlocking(index,
						new net.sf.openforge.verilog.model.Math.Add(index,
								new Constant(1, index.getWidth()))));

				// Create the ending condition for the 'simulation'
				block.add(getIndexTerminalStatement(resultFile, index,
						cycleCount, clkCntFile));
			}

			// add clock specific index increment
			Net tmpindex = clkPinLogicToIndex.get(cpl);
			block.add(new Assign.NonBlocking(tmpindex,
					new net.sf.openforge.verilog.model.Math.Add(tmpindex,
							new Constant(1, tmpindex.getWidth()))));

			// Instantiate the logic for asserting inputs/io's and testing
			// outputs/io's
			for (Pin p : pinToLogic.keySet()) {
				PinLogic logic = pinToLogic.get(p);

				_testbench.d.ln("Cons: pin: " + p.getApiPin() + " is " + logic);
				// only add if in this clock

				if (pinToClockPinMap.get(p) == null) {
					_testbench.d.ln("\t==null");
					// catches reset and other stuff that is going
					// away
				} else if (pinToClockPinMap.get(p).equals(clock.getApiPin())) {
					_testbench.d.ln("\t==ok");
					block.add(logic.stateSequential(index,
							clkPinLogicToIndex.get(cpl)));

					// for continuous we need an index that correlates
					// to the current clock, not the master index used
					// to know when simulation is active which the
					// sequential states use to know when to verify
					// the outputs or not if the simulation
					// verification is finished and the simulation is
					// coasting to completion ($finish).
					testModule.state(logic.stateContinuous(clkPinLogicToIndex
							.get(cpl)));
				} else {
					_testbench.d.ln("plogmap: " + pinToClockPinMap.get(p)
							+ " and apipin: " + clock.getApiPin());
				}
			}

			testModule
					.state(new Always(new ProceduralTimingBlock(
							new EventControl(new EventExpression.PosEdge(cpl
									.getNet())), block)));
		}

		/**
		 * <pre>
		 * 
		 * assign glbl.GSR = GSR;
		 * assign glbl.GTS = 1'b0;
		 * assign glbl.PRLD = 1'b0;
		 * 
		 * initial begin
		 *   GSR <= 1;
		 *   #1 GSR <= 0;
		 * end
		 * </pre>
		 */
		Net gsr = new Register("LGSR", 1);
		Module m = new Module("glbl");
		Net w = new Wire("GSR", 1);
		QualifiedNet qn = new QualifiedNet(m, w);
		testModule.state(new Assign.Continuous(qn, gsr));

		m = new Module("glbl");
		w = new Wire("GTS", 1);
		qn = new QualifiedNet(m, w);
		testModule.state(new Assign.Continuous(qn, new Constant(0, 1)));

		m = new Module("glbl");
		w = new Wire("PRLD", 1);
		qn = new QualifiedNet(m, w);
		testModule.state(new Assign.Continuous(qn, new Constant(0, 1)));

		InitialBlock ib = new InitialBlock();

		ib.add(new Assign.NonBlocking(gsr, new Constant(1, 1)));

		ib.add(new DelayStatement(new Assign.NonBlocking(gsr,
				new Constant(0, 1)), 1));

		testModule.state(ib);

		ib = new InitialBlock();

		for (Object o : clkPinToLogic.keySet()) {
			Pin clock = (Pin) o;
			ClkPinLogic cpl = (ClkPinLogic) clkPinToLogic.get(clock);

			ib.add(new Assign.NonBlocking(cpl.getNet(), new HexNumber(
					new HexConstant(0, cpl.getNet().getWidth()))));

			// zero out clock specific indexes
			Net tmpindex = clkPinLogicToIndex.get(cpl);
			ib.add(new Assign.NonBlocking(tmpindex, new HexNumber(
					new HexConstant(0, tmpindex.getWidth()))));

		}

		ib.add(new Assign.NonBlocking(index, new HexNumber(new HexConstant(0,
				index.getWidth()))));

		ib.add(resultFile.init());
		ib.add(clkCntFile.init());

		for (PinLogic logic : pinToLogic.values()) {
			logic.initMemory(ib);
		}
		testModule.state(ib);

		simulationDocument.append(testModule);
	}

	private void addHeader(VerilogDocument doc) {
		Comment header = new Comment("OpenForge Test", Comment.SHORT);
		doc.append(header);
		SimpleDateFormat df = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss Z");
		header = new Comment("Pin Automatic Test Bench.  Generated: "
				+ df.format(new Date()), Comment.SHORT);
		doc.append(header);

		header = new Comment("");
		doc.append(header);
	}

	private void addInclude(VerilogDocument doc, File source) {
		if (source != null) {
			doc.append(new IncludeStatement(source));
		}
	}

	public void write(FileOutputStream fos) {
		PrettyPrinter pp = new PrettyPrinter(fos);
		pp.print(simulationDocument);
	}

	@SuppressWarnings("deprecation")
	private Map<Pin, PinLogic> buildPinLogic(Design design,
			ResultFile resultFile, Set<ClockPin> clocksUsed) {
		Map<Pin, PinLogic> pinToLogic = new HashMap<Pin, PinLogic>();
		Set<ClockPin> unInitedClocks = new HashSet<ClockPin>(clocksUsed);

		for (InputPin pin : design.getClockPins()) {
			unInitedClocks.remove(pin.getApiPin());
			pinToLogic.put(pin, new ClkPinLogic(pin));
		}

		for (Pin pin : design.getInputPins()) {

			if (pinToLogic.get(pin) != null) {
				// skip it, it must be a clock and is already in the map
			} else {
				if ((!Core.hasThisPin(pin.getApiPin()))
						|| (Core.hasPublished(pin.getApiPin()))) {
					pinToLogic.put(pin, new InPinLogic((InputPin) pin));
				}
			}
		}

		for (Pin p : design.getOutputPins()) {
			OutputPin pin = (OutputPin) p;

			if ((!Core.hasThisPin(pin.getApiPin()))
					|| (Core.hasPublished(pin.getApiPin()))) {
				pinToLogic.put(pin, new OutPinLogic(pin, resultFile));
			}
		}

		for (Pin p : design.getBidirectionalPins()) {
			BidirectionalPin pin = (BidirectionalPin) p;

			if ((!Core.hasThisPin(pin.getApiPin()))
					|| (Core.hasPublished(pin.getApiPin()))) {
				pinToLogic.put(pin, new InOutPinLogic(pin, resultFile));
			}
		}

		// finish all uninited clocks
		for (ClockPin cp : unInitedClocks) {

			InputPin ip = new InputPin(1, false);
			ip.setApiPin(cp);
			ip.setIDLogical(cp.getName());

			pinToLogic.put(ip, new ClkPinLogic(ip));
		}

		return pinToLogic;
	}

	private CycleCount findCycleCount(Design design,
			Map<Pin, ClockPin> pinToClockPinMap) {
		double minTime = Double.MAX_VALUE;
		boolean bounded = false;
		int whichCount = 0;
		ClockPin whichClk = null;
		HashSet<ClockPin> uniqueClocks = new HashSet<ClockPin>();

		for (Pin pin : design.getPins()) {

			Buffer apiPin = pin.getApiPin();

			// do we have a user pin, not part of a core or a published part of
			// a core
			if ((apiPin != null)
					&& ((!Core.hasThisPin(apiPin)) || (Core
							.hasPublished(apiPin)))) {
				int driveLength = PinSimData.getDriveData(apiPin)
						.getCycleCount();
				int testLength = PinSimData.getTestData(apiPin).getCycleCount();

				int length = -1;

				if (driveLength <= 0) // if we have no drive, use test
				{
					length = testLength;
				} else if (testLength <= 0) // if no test, then drive
				{
					length = driveLength;
				} else {
					length = java.lang.Math.min(driveLength, testLength); // take
																			// smallest
																			// of
																			// two
																			// sides
				}

				if (length > 0) {
					// use clock to calculate the actual time required

					ClockPin cp = pinToClockPinMap.get(pin);
					double period;

					if (cp == null) {
						// something happened, default to global clock
						cp = ClockDomain.GLOBAL.getClockPin();
					}

					uniqueClocks.add(cp);

					if (cp.getFrequency() == ClockPin.UNDEFINED_HZ) {
						// assume 10 Mhz by default
						period = 1.0 / 10000000.0;
					} else {
						period = 1.0 / cp.getFrequency();
					}

					double time = period * length;

					if (time < minTime) {
						// we found a smaller one
						minTime = time;
						whichCount = length;
						whichClk = cp;
					}

					bounded = true;
				}
			}
		}

		return (new CycleCount((bounded ? whichCount : -1), whichClk,
				uniqueClocks));
	}

	private Statement getIndexTerminalStatement(ResultFile file, Net index,
			int vectorCount, ResultFile clockCntFile) {
		SequentialBlock block = new SequentialBlock();
		Statement pass = new StringStatement("PASSED\\n");
		block.add(file.write(pass));

		CommaDelimitedStatement cds = new CommaDelimitedStatement();
		cds.append(new StringStatement("%d\\n"));
		cds.append(new net.sf.openforge.verilog.model.Math.Add(index,
				new Constant(1, index.getWidth())));
		block.add(clockCntFile.write(cds));

		block.add(new FStatement.Finish());
		ConditionalStatement cs = new ConditionalStatement(new Compare.GTEQ(
				index, new HexNumber(new HexConstant(vectorCount,
						index.getWidth()))), block);
		return cs;
	}

	@SuppressWarnings("deprecation")
	private Map<Pin, PinLogic> instantiate(Design design, Module module,
			Map<Pin, PinLogic> pinToLogic) {
		Map<Pin, PinLogic> pinToExpr = new HashMap<Pin, PinLogic>();
		ModuleInstance instance = new ModuleInstance(getVerilogName(design),
				"test");

		List<Pin> inPins = new ArrayList<Pin>(design.getInputPins());

		Set<Pin> configuredClockPins = new HashSet<Pin>();

		// special handling for clock pins prior to the other input pins
		for (Pin p : inPins) {
			InputPin pin = (InputPin) p;

			PinLogic logic = pinToLogic.get(pin);

			if ((logic != null) && (logic instanceof ClkPinLogic)) {
				ClkPinLogic cpl = (ClkPinLogic) logic;

				configuredClockPins.add(pin);

				// handle as a clock pin
				module.state(new Always(cpl.stateSequential(null, null)));

				if (design.consumesClock()) {
					instance.add(new PortConnection(new Input(
							getVerilogName(pin.getBus()), 1), cpl.getNet()));
				}
			}
		}

		// finish all pins in logic to clock map that were not
		// connected to the design
		for (Pin pin : pinToLogic.keySet()) {

			PinLogic logic = pinToLogic.get(pin);

			if (logic instanceof ClkPinLogic) {
				if (!configuredClockPins.contains(pin)) {
					module.state(new Always(logic.stateSequential(null, null)));
					configuredClockPins.add(pin);
				}
			}
		}

		for (Pin p : inPins) {
			InputPin pin = (InputPin) p;
			_testbench.d.ln("Instantiate: Input Pin: " + pin + " Api: "
					+ pin.getApiPin());
			if ((!Core.hasThisPin(pin.getApiPin()))
					|| (Core.hasPublished(pin.getApiPin()))) {
				PinLogic logic = pinToLogic.get(pin);

				_testbench.d.ln("Instantiate: Logic Input Pin: " + logic);
				if (logic instanceof ClkPinLogic) {
					// we already did the clock, skip
				} else {
					// normal old input pin
					String name = getVerilogName(pin.getBus());
					instance.add(new PortConnection(new Input(name, pin
							.getWidth()), logic.getNet()));
				}
			}
		}

		for (Pin p : design.getOutputPins()) {
			OutputPin pin = (OutputPin) p;
			_testbench.d.ln("Instantiate: Output Pin: " + pin + " Api: "
					+ pin.getApiPin());

			if ((!Core.hasThisPin(pin.getApiPin()))
					|| (Core.hasPublished(pin.getApiPin()))) {
				PinLogic logic = pinToLogic.get(pin);
				_testbench.d.ln("Instantiate: Logic Output Pin: " + logic);
				String name = getVerilogName(pin);
				instance.add(new PortConnection(
						new Output(name, pin.getWidth()), logic.getNet()));
			}
		}

		for (Pin p : design.getBidirectionalPins()) {
			BidirectionalPin pin = (BidirectionalPin) p;

			_testbench.d.ln("Instantiate: Bidir Pin: " + pin + " Api: "
					+ pin.getApiPin());
			if ((!Core.hasThisPin(pin.getApiPin()))
					|| (Core.hasPublished(pin.getApiPin()))) {
				PinLogic logic = pinToLogic.get(pin);
				_testbench.d.ln("Instantiate: Logic Bidir Pin: " + logic);
				String name = getVerilogName(pin);
				instance.add(new PortConnection(
						new Inout(name, pin.getWidth()), logic.getNet()));
			}
		}

		module.state(instance);

		return pinToExpr;
	}

	private static String getVerilogName(Object obj) {
		return ID.toVerilogIdentifier(ID.showLogical(obj));
	}

	@SuppressWarnings("deprecation")
	private Map<Pin, ClockPin> getPinToClockPinMap(Design design) {
		// the goal of this method is to build a map of lim.Pin
		// objects to api.pin.ClockPin objects so the logic in this
		// class can know what clock domain to put the test logic in

		Map<Pin, ClockPin> pinToClockPinMap = new HashMap<Pin, ClockPin>();

		// step 1, go through each task in the design, fetch all the
		// ports and busses and map them back to their lim.Pin at the
		// top level, then add an association from lim.Pin to the
		// clock for the task.

		for (Task task : design.getTasks()) {

			if (!task.isAutomatic()) {
				Call call = task.getCall();

				Port goPort = null;

				if (call.consumesGo()) {
					goPort = call.getGoPort();
				}

				ArrayList<Port> argumentPorts = new ArrayList<Port>();

				for (Port p : call.getDataPorts()) {

					if (p.getTag() == Component.NORMAL) {
						// this is an argument
						argumentPorts.add(p);
					}
				}

				Bus doneBus = null;
				Bus resultBus = null;

				for (Exit exit : call.getExits()) {

					if (exit.getTag().getType() == Exit.DONE) {
						// this is the result and done bus exit

						List<Bus> l = exit.getDataBuses();
						if (l.size() > 0) {
							resultBus = l.get(0);
						}

						if (call.producesDone()) {
							doneBus = exit.getDoneBus();
						}
					}
				}

				// Now determine the pins representing the ports and buses we
				// identified

				// Procedure procedure = call.getProcedure();
				// ClockPin cp = procedure.getClockPin();
				ClockPin cp = null;

				if (goPort != null) {
					Pin goPin = design.getPin(goPort);
					pinToClockPinMap.put(goPin, cp);
				}

				if (doneBus != null) {
					Pin donePin = design.getPin(doneBus);
					pinToClockPinMap.put(donePin, cp);
				}

				if (resultBus != null) {
					Pin resultPin = design.getPin(resultBus);
					pinToClockPinMap.put(resultPin, cp);
				}

				for (Port port : argumentPorts) {
					Object obj = port;

					// make sure there is a pin for the given argument
					if (design.getPin(obj) != null) {
						pinToClockPinMap.put(design.getPin(obj), cp);
					}
				}
			}
		}

		// Now go through all the design pins and if there is
		// nothing registered in the pinToClockPinMap, then ask
		// the api pin for its clock domain
		Collection<InputPin> clkpins = design.getClockPins();

		for (Pin p : design.getInputPins()) {
			InputPin pin = (InputPin) p;

			// _testbench.d.ln("Map: Input: "+pin.getApiPin()+" Has: "+pin.getClockPin()+" domain: "+pin.getApiPin().getDomain());
			if (clkpins.contains(pin)) {
				// skip it, it is a clock
			} else {
				if ((!Core.hasThisPin(pin.getApiPin()))
						|| (Core.hasPublished(pin.getApiPin()))) {
					if (pinToClockPinMap.get(pin) == null) {
						// here we have to flush in the clock pin from the
						// domain of the api pin
						if (Core.hasPublished(pin.getApiPin())) {
							if (pin.getApiPin().getDomain() == null) {
								pin.getApiPin().setDomain(ClockDomain.GLOBAL);
								EngineThread
										.getGenericJob()
										.warn("IP Core Pin: "
												+ pin.getApiPin()
												+ " using GOBAL clock as default");
							}
							// pinToClockPinMap.put(pin,pin.getApiPin().getDomain().getClockPin());
						}
						// else if(pin.getClockPin() != null)
						// {
						// pinToClockPinMap.put(pin,pin.getClockPin().getApiPin());
						// }
					}
				}
			}
		}

		for (Pin p : design.getOutputPins()) {
			OutputPin pin = (OutputPin) p;
			// _testbench.d.ln("Map: Output: "+pin.getApiPin()+" Has: "+pin.getClockPin()+" domain: "+pin.getApiPin().getDomain());

			if ((!Core.hasThisPin(pin.getApiPin()))
					|| (Core.hasPublished(pin.getApiPin()))) {
				if (pinToClockPinMap.get(pin) == null) {
					// here we have to flush in the clock pin from the domain of
					// the api pin
					if (Core.hasPublished(pin.getApiPin())) {
						if (pin.getApiPin().getDomain() == null) {
							pin.getApiPin().setDomain(ClockDomain.GLOBAL);
							EngineThread.getGenericJob().warn(
									"IP Core Pin: " + pin.getApiPin()
											+ " using GOBAL clock as default");
						}
						// pinToClockPinMap.put(pin,pin.getApiPin().getDomain().getClockPin());
					}
					// else if(pin.getClockPin() != null)
					// {
					// pinToClockPinMap.put(pin,pin.getClockPin().getApiPin());
					// }
				}
			}
		}

		for (Pin p : design.getBidirectionalPins()) {
			BidirectionalPin pin = (BidirectionalPin) p;
			// _testbench.d.ln("Map: Bidir: "+pin.getApiPin()+" Has: "+pin.getClockPin()+" domain: "+pin.getApiPin().getDomain());

			if ((!Core.hasThisPin(pin.getApiPin()))
					|| (Core.hasPublished(pin.getApiPin()))) {
				if (pinToClockPinMap.get(pin) == null) {
					// here we have to flush in the clock pin from the domain of
					// the api pin
					if (Core.hasPublished(pin.getApiPin())) {
						if (pin.getApiPin().getDomain() == null) {
							pin.getApiPin().setDomain(ClockDomain.GLOBAL);
							EngineThread.getGenericJob().warn(
									"IP Core Pin: " + pin.getApiPin()
											+ " using GOBAL clock as default");
						}
						// pinToClockPinMap.put(pin,pin.getApiPin().getDomain().getClockPin());
					}
					// else if(pin.getClockPin() != null)
					// {
					// pinToClockPinMap.put(pin,pin.getClockPin().getApiPin());
					// }
				}
			}
		}

		return (pinToClockPinMap);
	}

	public abstract class PinLogic {
		private final InitializedMemory mem;
		private final String name;
		int width;

		public PinLogic(String prefix, Pin pin, List<SignalValue> data) {
			name = ID.toVerilogIdentifier(ID.showLogical(pin));

			width = pin.getWidth();
			mem = initMem(prefix + "_" + name + "_values", pin, data, width);
		}

		protected InitializedMemory initMem(String name, Pin pin,
				List<SignalValue> data, int width) {
			InitializedMemory im = new InitializedMemory(name, width);
			// for (int i=0; i < CycleSimTestBench.this.cycleCount; i++)
			for (int i = 0; i < (data.size() + 2); i++) {
				SignalValue sv = (i < data.size()) ? ((SignalValue) data.get(i))
						: SignalValue.X;
				im.addInitValue(getExpr(sv, width, pin));
			}

			return im;
		}

		private Expression getExpr(SignalValue s, int width, Pin pin) {
			Expression expr = null;
			if (s.isX()) {
				expr = new HexNumber(new BinaryConstant("x", width));
			} else if (s.isZ()) {
				expr = new HexNumber(new BinaryConstant("z", width));
			} else {
				expr = new HexNumber(new HexConstant(s.getValue(), width));
			}
			return expr;
		}

		protected InitializedMemory getMemory() {
			return mem;
		}

		public void initMemory(InitialBlock ib) {
			ib.add(getMemory());
		}

		public String getName() {
			return name;
		}

		public abstract Net getNet();

		public abstract Statement stateSequential(Net index, Net clkIndex);

		public abstract Statement stateContinuous(Net index);
	}

	public class ClkPinLogic extends PinLogic {
		Register reg;
		int periodNs;
		double period = 0.0;

		public ClkPinLogic(InputPin pin) {
			super("arg", pin, pin.getApiPin() == null ? Collections
					.<SignalValue> emptyList() : PinSimData.getDriveData(
					pin.getApiPin()).asList());
			reg = new Register(getVerilogName(pin.getBus()), pin.getWidth());

			ClockPin cp = (ClockPin) pin.getApiPin();

			if (cp.getFrequency() == ClockPin.UNDEFINED_HZ) {
				// assume 10 Mhz by default
				period = 1.0 / 10000000.0;
			} else {
				period = 1.0 / cp.getFrequency();
			}

			// convert to nanoseconds, * 1x10^9
			period *= 1000000000.0;

			periodNs = (int) period;
		}

		@Override
		public Net getNet() {
			return reg;
		}

		@Override
		public Statement stateSequential(Net index, Net clkIndex) {
			Assign assign = new Assign.NonBlocking(getNet(), new Unary.Negate(
					getNet()));
			// delay for 1/2 the period to create the correct clock
			return new DelayStatement(assign, (periodNs / 2));
		}

		@Override
		public Statement stateContinuous(Net index) {
			return new InlineComment("");
		}

		@Override
		public InitializedMemory getMemory() {
			assert false : "Should not use memory for clock";
			return super.getMemory();
		}
	}

	public class InPinLogic extends PinLogic {
		Wire reg;

		public InPinLogic(InputPin pin) {
			super("arg", pin, pin.getApiPin() == null ? Collections
					.<SignalValue> emptyList() : PinSimData.getDriveData(
					pin.getApiPin()).asList());
			reg = new Wire(getVerilogName(pin.getBus()), pin.getWidth());
		}

		@Override
		public Net getNet() {
			return reg;
		}

		@Override
		public Statement stateSequential(Net index, Net clkIndex) {
			return new InlineComment("");
		}

		@Override
		public Statement stateContinuous(Net index) {
			return new Assign.Continuous(reg, new MemoryElement(getMemory(),
					index));
		}
	}

	public class OutPinLogic extends PinLogic {
		Wire wire;
		ResultFile file;
		Wire reswire;

		InitializedMemory driveMem;

		public OutPinLogic(OutputPin pin, ResultFile file) {
			super("res", pin, pin.getApiPin() == null ? Collections
					.<SignalValue> emptyList() : PinSimData.getTestData(
					pin.getApiPin()).asList());

			wire = new Wire(getVerilogName(pin), pin.getWidth());
			reswire = new Wire(getVerilogName(pin) + "_expected",
					pin.getWidth());
			this.file = file;

			List<SignalValue> driveValues = PinSimData.getDriveData(
					pin.getApiPin()).asList();

			if (!driveValues.isEmpty()) {
				driveMem = initMem("drive_" + getName() + "_values", pin,
						driveValues, pin.getWidth());
			} else {
				driveMem = null;
			}
		}

		@Override
		public Net getNet() {
			return wire;
		}

		@Override
		public void initMemory(InitialBlock ib) {
			super.initMemory(ib);

			if (driveMem != null) {
				ib.add(driveMem);
			}
		}

		@Override
		public Statement stateContinuous(Net index) {
			StatementBlock sb = new StatementBlock();

			// drive any drive data if it exists
			if (driveMem != null) {
				sb.add(new Assign.Continuous(getNet(), new MemoryElement(
						driveMem, index)));
			}

			sb.add(new Assign.Continuous(reswire, new MemoryElement(
					getMemory(), index)));

			return sb;
		}

		@Override
		public Statement stateSequential(Net index, Net clkIndex) {
			SequentialBlock trueBlock = new SequentialBlock();
			CommaDelimitedStatement cds = new CommaDelimitedStatement();
			cds.append(new StringStatement(
					"FAIL: Result does not match expected at clock cycle %d for pin "
							+ getName() + " expected %x received %x \\n"));
			cds.append(clkIndex);
			cds.append(reswire);
			cds.append(getNet());
			trueBlock.add(file.write(cds));
			trueBlock.add(new DelayStatement(new FStatement.Finish(), 500));
			ConditionalStatement cs = new ConditionalStatement(
					new Compare.CASE_NEQ(reswire, getNet()), trueBlock);
			ConditionalStatement cs2 = new ConditionalStatement(
					new Compare.CASE_NEQ(reswire, new HexNumber(
							new BinaryConstant("x", width))),
					new SequentialBlock(cs));
			ConditionalStatement cs3 = new ConditionalStatement(new Compare.LT(
					index, new Decimal(new Constant(cycleCount,
							index.getWidth()))), new SequentialBlock(cs2));

			return cs3;
		}
	}

	public class InOutPinLogic extends PinLogic {
		Wire net;
		ResultFile file;
		Wire reswire;

		InitializedMemory driveMem;

		public InOutPinLogic(BidirectionalPin pin, ResultFile file) {
			super("arg", pin, pin.getApiPin() == null ? Collections
					.<SignalValue> emptyList() : PinSimData.getTestData(
					pin.getApiPin()).asList());
			net = new Wire(getVerilogName(pin), pin.getWidth());
			this.file = file;

			reswire = new Wire(getVerilogName(pin) + "_expected",
					pin.getWidth());

			List<SignalValue> driveValues = PinSimData.getDriveData(
					pin.getApiPin()).asList();

			if (!driveValues.isEmpty()) {
				driveMem = initMem("drive_" + getName() + "_values", pin,
						driveValues, pin.getWidth());
			} else {
				driveMem = null;
			}
		}

		@Override
		public void initMemory(InitialBlock ib) {
			super.initMemory(ib);

			if (driveMem != null) {
				ib.add(driveMem);
			}
		}

		@Override
		public Net getNet() {
			return net;
		}

		@Override
		public Statement stateSequential(Net index, Net clkIndex) {
			SequentialBlock trueBlock = new SequentialBlock();
			CommaDelimitedStatement cds = new CommaDelimitedStatement();
			cds.append(new StringStatement(
					"FAIL: Result does not match expected at clock cycle %d for I/O pin "
							+ getName() + " expected %x received %x \\n"));
			cds.append(clkIndex);
			cds.append(reswire);
			cds.append(getNet());
			trueBlock.add(file.write(cds));
			trueBlock.add(new DelayStatement(new FStatement.Finish(), 500));
			ConditionalStatement cs = new ConditionalStatement(
					new Compare.CASE_NEQ(reswire, getNet()), trueBlock);
			ConditionalStatement cs2 = new ConditionalStatement(
					new Compare.CASE_NEQ(reswire, new HexNumber(
							new BinaryConstant("x", width))),
					new SequentialBlock(cs));
			ConditionalStatement cs3 = new ConditionalStatement(new Compare.LT(
					index, new Decimal(new Constant(cycleCount,
							index.getWidth()))), new SequentialBlock(cs2));

			return cs3;
		}

		@Override
		public Statement stateContinuous(Net index) {
			StatementBlock sb = new StatementBlock();

			// drive any drive data if it exists
			if (driveMem != null) {
				sb.add(new Assign.Continuous(getNet(), new MemoryElement(
						driveMem, index)));
			}

			sb.add(new Assign.Continuous(reswire, new MemoryElement(
					getMemory(), index)));

			return sb;
		}
	}

	class CycleCount {
		private final int cycleCount;
		private final ClockPin clk;
		private final Set<ClockPin> clocksUsed;

		CycleCount(int count, ClockPin clk, Set<ClockPin> clocks) {
			cycleCount = count;
			this.clk = clk;
			clocksUsed = clocks;
		}

		public int getCycleCount() {
			return (cycleCount);
		}

		public ClockPin getClockPin() {
			return (clk);
		}

		public Set<ClockPin> getClocksUsed() {
			return clocksUsed;
		}

	}

}// CycleSimTestBench

