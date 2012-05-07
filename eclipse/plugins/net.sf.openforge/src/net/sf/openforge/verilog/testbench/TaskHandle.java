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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sf.openforge.app.EngineThread;
import net.sf.openforge.app.OptionRegistry;
import net.sf.openforge.app.project.Option;
import net.sf.openforge.app.project.OptionInt;
import net.sf.openforge.lim.Bus;
import net.sf.openforge.lim.CodeLabel;
import net.sf.openforge.lim.Component;
import net.sf.openforge.lim.Design;
import net.sf.openforge.lim.Exit;
import net.sf.openforge.lim.InputPin;
import net.sf.openforge.lim.Pin;
import net.sf.openforge.lim.Port;
import net.sf.openforge.lim.Task;
import net.sf.openforge.util.naming.ID;
import net.sf.openforge.verilog.model.Always;
import net.sf.openforge.verilog.model.Assign;
import net.sf.openforge.verilog.model.Bitwise;
import net.sf.openforge.verilog.model.Comment;
import net.sf.openforge.verilog.model.Concatenation;
import net.sf.openforge.verilog.model.ConditionalStatement;
import net.sf.openforge.verilog.model.Constant;
import net.sf.openforge.verilog.model.Display;
import net.sf.openforge.verilog.model.EventControl;
import net.sf.openforge.verilog.model.EventExpression;
import net.sf.openforge.verilog.model.Expression;
import net.sf.openforge.verilog.model.InitialBlock;
import net.sf.openforge.verilog.model.InitializedMemory;
import net.sf.openforge.verilog.model.InlineComment;
import net.sf.openforge.verilog.model.Logical;
import net.sf.openforge.verilog.model.MemoryElement;
import net.sf.openforge.verilog.model.Module;
import net.sf.openforge.verilog.model.ProceduralTimingBlock;
import net.sf.openforge.verilog.model.Register;
import net.sf.openforge.verilog.model.Replication;
import net.sf.openforge.verilog.model.SequentialBlock;
import net.sf.openforge.verilog.model.StringStatement;
import net.sf.openforge.verilog.model.Unary;
import net.sf.openforge.verilog.model.Wire;

/**
 * TaskHandle is the class which ties together all the wires and logic
 * associated with a given Task in the design being tested. This includes
 * generation of the GO and argument inputs as well as wires for the expected
 * result, actual result, and done. If there is no go-done provided by the task
 * (or just a GO), this class will generate a delay chain of flops equal to the
 * latency of the task (reported). This class also instantiates the expected
 * value checker ({@link ExpectedChecker}).
 * 
 * <p>
 * Created: Wed Jan 8 15:46:36 2003
 * 
 * @author imiller, last modified by $Author: imiller $
 * @version $Id: TaskHandle.java 2 2005-06-09 20:00:48Z imiller $
 */
public class TaskHandle {

	/** The task for which this class contains logic. */
	private final Task task;
	/** The design that contains the task */
	private final Design design;
	/** The verilog compliant name of this task. */
	private final String baseName;

	/**
	 * The class containing the logic needed to validate each returned result.
	 */
	private final ExpectedChecker checker;

	// Wires used to drive inputs and capture outputs
	private final Wire go;
	private final Wire done;
	/**
	 * A List of Wire objects, in order of the data ports of the task's call.
	 */
	private final List<Wire> arguments;
	/**
	 * A List of Wire objects for the data buses of the task's call (should only
	 * be 1 for the result!)
	 */
	private final List<Wire> buses;
	private Wire expectedResult = null;

	/**
	 * The shift register used as a GO pulse generator for this task when the go
	 * spacing is determinate
	 */
	private final Register taskGoShift;
	/**
	 * A signal which indicates that we should fire a next GO (the nextGo memory
	 * points to us, and we have met the required condition imposed by the go
	 * pulse generator or previous done.
	 */
	private final Register taskGoValid;
	private final Wire taskNextGo;

	private int goSpacing = -1;

	/**
	 * A Map of the port/bus -> Wire created for that Port/Bus of the task's
	 * call
	 */
	private final Map<ID, Wire> pinWireCorrelation = new HashMap<ID, Wire>();

	/**
	 * The goMask (used in the nextGo memory ({@link Memories})) for this task.
	 */
	private long goMask = -1;

	/**
	 * Creates the TaskHandle and allocates {@link Wire Wires} for each input
	 * and output.
	 * 
	 * @param design
	 *            a value of type 'Design'
	 * @param task
	 *            a value of type 'Task'
	 */
	public TaskHandle(Design design, Task task) {
		this.task = task;
		this.design = design;

		goSpacing = getGoSpacing();

		baseName = ID.toVerilogIdentifier(ID.showLogical(task));

		checker = new ExpectedChecker(this);

		go = new Wire(getBaseName() + "_go", 1);
		done = new Wire(getBaseName() + "_done", 1);
		taskGoShift = new Register(getBaseName() + "_goShift",
				java.lang.Math.max(2, goSpacing + 1));
		taskGoValid = new Register(getBaseName() + "_goValid", 1);
		taskNextGo = new Wire(getBaseName() + "_nextGoCondition", 1);

		arguments = new ArrayList<Wire>();
		for (Port port : task.getCall().getDataPorts()) {
			if (port == port.getOwner().getThisPort()
					|| port.getTag() != Component.NORMAL) {
				continue;
			}
			InputPin pin = (InputPin) design.getPin(port);
			String busName = ID
					.toVerilogIdentifier(ID.showLogical(pin.getBus()));
			String name = busName + "_data";
			Wire wire = new Wire(name, pin.getWidth());
			arguments.add(wire);
			pinWireCorrelation.put(port, wire);
		}

		buses = new ArrayList<Wire>(1);
		for (Bus bus : task.getCall().getExit(Exit.DONE).getDataBuses()) {
			if (bus.getTag() != Component.NORMAL) {
				continue;
			}

			Pin pin = design.getPin(bus);
			String name = ID.toVerilogIdentifier(ID.showLogical(pin));

			Wire wire = new Wire(name, pin.getWidth());
			buses.add(wire);
			pinWireCorrelation.put(bus, wire);

			assert expectedResult == null : "Can only be 1 result wire";
			expectedResult = new Wire(name + "_expected", pin.getWidth());
		}
		if (expectedResult == null) {
			expectedResult = new Wire(
					getBaseName() + "_defaultresult_expected", 1);
		}
	}

	/**
	 * Adds any logic to the InitialBlock needed for the Wires/logic in this
	 * class
	 * 
	 * @param ib
	 *            the {@link InitialBlock} to add the logic to.
	 */
	public void stateInits(InitialBlock ib) {
		if (goSpacing >= 0) {
			ib.add(new Display(new StringStatement("Consecutive Go's for "
					+ getBaseName() + " will be asserted with " + goSpacing
					+ " cycles in between")));
		} else {
			ib.add(new Display(new StringStatement("Go to " + getBaseName()
					+ " will be asserted after previous done")));
		}
		ib.add(new Assign.NonBlocking(taskGoShift, new Constant(0, taskGoShift
				.getWidth())));
		ib.add(new Assign.NonBlocking(taskGoValid, new Constant(0, taskGoValid
				.getWidth())));

		// Try the checker
		checker.stateInits(ib);
	}

	/**
	 * Adds all the needed logic for this task to the given module, including
	 * selecting argument values from memories, determining expected values,
	 * assigning a value to the GO, and generating a done.
	 * 
	 * @param module
	 *            a value of type 'Module'
	 * @param mach
	 *            a value of type 'StateMachine'
	 * @param mems
	 *            a value of type 'Memories'
	 * @param resultFile
	 *            a value of type 'SimFileHandle'
	 */
	public void stateLogic(Module module, StateMachine mach, Memories mems,
			SimFileHandle resultFile) {
		module.state(new InlineComment("", Comment.SHORT));
		module.state(new InlineComment("Logic associated with task "
				+ getBaseName(), Comment.SHORT));
		stateArgConnections(module, mach, mems);
		stateExpectedConnections(module, mach, mems);
		stateGoGenerator(module, mach);
		stateGoLogic(module, mach, mems);
		stateGoDoneLogic(module, mach);

		// Only do the check if the task has a result.
		if (this.getResultWire() != null) {
			checker.stateLogic(module, mach, mems, resultFile);
		}
		module.state(new InlineComment("", Comment.SHORT));
	}

	/**
	 * Assign a value to each argument wire created for the task from the
	 * argument memories, qualified with the 'go' for the task so that in any
	 * cycle that the GO is not asserted the argument inputs are 0.
	 * 
	 * @param module
	 *            a value of type 'Module'
	 * @param mach
	 *            a value of type 'StateMachine'
	 * @param mems
	 *            a value of type 'Memories'
	 */
	private void stateArgConnections(Module module, StateMachine mach,
			Memories mems) {
		int i = 0;
		for (Wire argWire : arguments) {
			InitializedMemory im = mems.getArgMemory(i++);

			Wire portTemp = new Wire(argWire.getIdentifier().getToken()
					+ "_TEMP", im.getWidth());

			int width = argWire.getWidth();

			Assign assignTemp = new Assign.Continuous(portTemp,
					new MemoryElement(im, mach.getArgIndex()));
			module.state(assignTemp);

			Assign assign = new Assign.Continuous(argWire, new Bitwise.And(
					portTemp.getRange(width - 1, 0), new Replication(width,
							getGoWire())));
			module.state(assign);
		}
	}

	/**
	 * Assign the value to the 'expected' wire for this task, including
	 * selecting out the valid range from the results memory.
	 * 
	 * @param module
	 *            a value of type 'Module'
	 * @param mach
	 *            a value of type 'StateMachine'
	 * @param mems
	 *            a value of type 'Memories'
	 */
	private void stateExpectedConnections(Module module, StateMachine mach,
			Memories mems) {
		InitializedMemory results = mems.getResultMemory();

		/*
		 * Use a temporary variable to avoid the "[][]" syntax, which chokes
		 * Icarus.
		 */
		Wire expected = getExpectedResultWire();

		Wire expectedTemp = new Wire(expected.getIdentifier().getToken()
				+ "_TEMP", results.getWidth());
		Assign assignTemp = new Assign.Continuous(expectedTemp,
				new MemoryElement(results, mach.getResIndex()));
		module.state(assignTemp);

		Assign exp = new Assign.Continuous(expected, expectedTemp.getRange(
				expected.getWidth() - 1, 0));
		module.state(exp);
	}

	/**
	 * Generates the task_go_valid and the task_next_go signals.
	 */
	private void stateGoGenerator(Module module, StateMachine mach) {
		// task_next_go = start || allDones; IFF GO SPACE INDETERMINATE
		// task_next_go = start || taskGoShift[goSpace];
		Expression nextGoSignal;
		if (goSpacing < 0) {
			nextGoSignal = mach.getAllDoneWire();
		} else {
			nextGoSignal = taskGoShift.getBitSelect(goSpacing);
		}
		module.state(new Assign.Continuous(taskNextGo, new Logical.Or(mach
				.getStart(), nextGoSignal)));

		SequentialBlock block = new SequentialBlock();
		//
		// The valid signal (indicates that we should fire a GO).
		//
		block.add(new Assign.NonBlocking(taskGoValid, new Logical.And(
				taskNextGo, mach.getNotMaxArg())));

		//
		// The shifter
		//
		Concatenation cat = new Concatenation();
		cat.add(taskGoShift.getRange(taskGoShift.getWidth() - 2, 0));
		cat.add(taskNextGo);
		block.add(new Assign.NonBlocking(taskGoShift, cat));

		ProceduralTimingBlock ptb = new ProceduralTimingBlock(new EventControl(
				new EventExpression.PosEdge(mach.getClock())), block);

		module.state(new Always(ptb));
	}

	/**
	 * Assigns the value to the 'go' for this task by selecting the correct bit
	 * from the one-hot encoded nextGo memory and qualifying that with the
	 * valid, !reset, and !pause signals from the state machine.
	 * 
	 * @param module
	 *            a value of type 'Module'
	 * @param mach
	 *            a value of type 'StateMachine'
	 * @param mems
	 *            a value of type 'Memories'
	 */
	private void stateGoLogic(Module module, StateMachine mach, Memories mems) {
		assert getGoMask() > 0 || getGoMask() == 0x8000000000000000L : "Error goMask is 0x"
				+ Long.toHexString(getGoMask());

		int bit = 0;
		for (; bit < 64; bit++) {
			if ((getGoMask() >>> bit) == 1L) {
				break;
			}
		}

		InitializedMemory nextGo = mems.getNextGoMemory();

		/*
		 * Use a temporary variable to avoid the "[][]" syntax, which chokes
		 * Icarus.
		 */
		Wire goWireTemp = new Wire(getGoWire().getIdentifier().getToken()
				+ "_TEMP", nextGo.getWidth());
		Assign tempAssign = new Assign.Continuous(goWireTemp,
				new MemoryElement(nextGo, mach.getArgIndex()));
		module.state(tempAssign);

		// final Bitwise.And goAndValid = new Bitwise.And(
		// goWireTemp.getBitSelect(bit), mach.getValid());
		final Bitwise.And goAndValid = new Bitwise.And(
				goWireTemp.getBitSelect(bit), taskGoValid);
		final Bitwise.And pauseAndReset = new Bitwise.And(new Unary.Not(
				mach.getReset()), new Unary.Not(mach.getPause()));

		Assign assign = new Assign.Continuous(getGoWire(), new Bitwise.And(
				goAndValid, pauseAndReset));

		module.state(assign);
	}

	/**
	 * Define the source of the 'done' for this task as the Done pin of the
	 * design if it has one for this task. Otherwise create a chain of flops the
	 * length of the defined latency of this task.
	 * 
	 * @param module
	 *            a value of type 'Module'
	 * @param mach
	 *            a value of type 'StateMachine'
	 */
	private void stateGoDoneLogic(Module module, StateMachine mach) {
		// Only if design contains no go and done
		Pin goPin = design.getPin(task.getCall().getGoPort());
		Pin donePin = design.getPin(task.getCall().getExit(Exit.DONE)
				.getDoneBus());

		if (goPin == null || donePin == null) {
			int latency = task.getCall().getLatency().getMaxClocks();

			if (goPin != null) {
				EngineThread
						.getGenericJob()
						.warn("Task: "
								+ getBaseName()
								+ " has GO but no DONE.  Building external DONE network in test bench for latency: "
								+ latency);
			} else {
				EngineThread
						.getGenericJob()
						.warn("Task: "
								+ getBaseName()
								+ " has no GO or DONE.  Building external DONE network in test bench for latency: "
								+ latency);
			}

			if (latency >= 0) {
				Expression goWire = getGoWire();

				if (latency > 0) {
					Register shifter = new Register(getBaseName() + "go_delay",
							latency);

					Expression rhs = goWire;
					if (latency > 1) {
						Concatenation cat = new Concatenation();
						cat.add(shifter.getRange(shifter.getWidth() - 2, 0));
						cat.add(goWire);
						rhs = cat;
					}

					// if(reset) delay <= 0; else delay <=
					// {delay[n-1:0],goWire};
					SequentialBlock block = new SequentialBlock(
							new ConditionalStatement(
									mach.getReset(),
									new Assign.NonBlocking(shifter,
											new Constant(0, shifter.getWidth())),
									new Assign.NonBlocking(shifter, rhs)));

					EventExpression[] eexpressions = {
							new EventExpression.PosEdge(mach.getClock()),
							new EventExpression.PosEdge(mach.getReset()) };
					EventExpression compoundEE = new EventExpression(
							eexpressions);
					ProceduralTimingBlock ptb = new ProceduralTimingBlock(
							new EventControl(compoundEE), block);
					module.state(new Always(ptb));

					goWire = shifter.getBitSelect(latency - 1);
				}

				module.state(new Assign.Continuous(getDoneWire(), goWire));
			}
		}
	}

	/**
	 * Gets the verilog compliant name of this task
	 */
	public String getBaseName() {
		return baseName;
	}

	/**
	 * Returns the {@link ExpectedChecker} used to validate the results of this
	 * task.
	 */
	public ExpectedChecker getExpectedChecker() {
		return checker;
	}

	/**
	 * Returns the {@link Wire} which supplies the GO signal to this task.
	 */
	public Wire getGoWire() {
		return go;
	}

	/**
	 * Returns the {@link Wire} which indicates that the task has completed
	 * processing a data point.
	 */
	public Wire getDoneWire() {
		return done;
	}

	/**
	 * Returns the {@link Wire} which supplies the exepected result as derived
	 * from the results memory. Never null.
	 */
	public Wire getExpectedResultWire() {
		return expectedResult;
	}

	/**
	 * Returns the {@link Wire} that is connected to the tasks result, or null
	 * if this task has 0 or 2+ result wires.
	 */
	public Wire getResultWire() {
		if (buses.size() == 1) {
			return buses.get(0);
		} else if (buses.size() > 1) {
			EngineThread
					.getGenericJob()
					.warn("Task "
							+ getBaseName()
							+ " has "
							+ buses.size()
							+ " results.  Cannot validate results in testbench.  Task results checking skipped");
		}

		return null;
	}

	/**
	 * Returns the bit width of the argument port in the specified position, or
	 * -1 if the position is higher than the number of argument ports for this
	 * task.
	 */
	public int getWidthOfPort(int position) {
		if (position < arguments.size()) {
			return arguments.get(position).getWidth();
		}

		return -1;
	}

	/**
	 * Returns the bit width of the result of this task.
	 */
	public int getMaxResultWidth() {
		if (buses.size() == 1) {
			return buses.get(0).getWidth();
		}

		return -1;
	}

	/**
	 * Returns the value of the go mask used for this task (value contained in
	 * the next go memory which indicates that this task should be enabled).
	 */
	public long getGoMask() {
		return goMask;
	}

	/**
	 * Sets the go mask for this task. Called by {@link Memories}
	 */
	public void setGoMask(long mask) {
		goMask = mask;
	}

	/**
	 * Returns the wire created for the given Port or Bus.
	 * 
	 * @param o
	 *            an 'Object' of type Port or Bus from the Task's call.
	 * @return a value of type 'Wire'
	 */
	public Wire getWireForConnection(Object o) {
		return pinWireCorrelation.get(o);
	}

	/**
	 * Returns the {@link Task} for which this TaskHandle was created.
	 */
	public Task getTask() {
		return task;
	}

	/**
	 * Returns the GO spacing to use for this task, either from the user
	 * specified value, or the calculated throughput for the task.
	 */
	private int getGoSpacing() {
		Option op = EngineThread.getGenericJob().getOption(
				OptionRegistry.ATB_GO_SPACING);
		int spacing = ((OptionInt) op).getValueAsInt(CodeLabel.UNSCOPED);
		// If spacing is non-negative, then it was specified by the
		// user and we need to respect that.
		// if (spacing >= 0)
		if (spacing != -1) {
			return spacing;
		}

		return getTask().getGoSpacing();
	}

}// TaskHandle
