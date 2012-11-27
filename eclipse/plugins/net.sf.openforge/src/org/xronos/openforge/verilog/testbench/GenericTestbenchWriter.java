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

package org.xronos.openforge.verilog.testbench;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.xronos.openforge.app.EngineThread;
import org.xronos.openforge.app.ForgeFileHandler;
import org.xronos.openforge.app.GenericJob;
import org.xronos.openforge.app.OptionRegistry;
import org.xronos.openforge.backend.hdl.TestBenchEngine;
import org.xronos.openforge.backend.hdl.VerilogTranslateEngine;
import org.xronos.openforge.lim.Bus;
import org.xronos.openforge.lim.Call;
import org.xronos.openforge.lim.CodeLabel;
import org.xronos.openforge.lim.Component;
import org.xronos.openforge.lim.Design;
import org.xronos.openforge.lim.Exit;
import org.xronos.openforge.lim.Kicker;
import org.xronos.openforge.lim.Port;
import org.xronos.openforge.lim.Register;
import org.xronos.openforge.lim.io.FifoIF;
import org.xronos.openforge.lim.io.FifoInput;
import org.xronos.openforge.lim.io.FifoOutput;
import org.xronos.openforge.lim.io.SimplePin;
import org.xronos.openforge.util.IndentWriter;
import org.xronos.openforge.util.naming.ID;
import org.xronos.openforge.verilog.pattern.BusWire;
import org.xronos.openforge.verilog.pattern.GenericModule;
import org.xronos.openforge.verilog.pattern.PortWire;


/**
 * This class generates a self-verifying testbench for the given {@link Design}
 * where input and output vectors are supplied in files named
 * <i>actorName</i>_<i>portName</i>.vec. The testbench uses PLI calls that are
 * available through the PLI code in konaAtest/bin/autoTestPli.c
 * 
 * @author imiller
 * @version $Id: GenericTestbenchWriter.java 284 2006-08-15 15:43:34Z imiller $
 */
public class GenericTestbenchWriter {
	private final String actorName;
	private final Design design;

	public GenericTestbenchWriter(Design design) {
		final GenericJob gj = EngineThread.getGenericJob();
		actorName = gj.getOutputBaseName();
		this.design = design;
	}

	private String getActorName() {
		return actorName;
	}

	private Design getDesign() {
		return design;
	}

	/*
	 * Returns the name to be used for the instance of the design being tested
	 */
	private String getInstanceName() {
		return "dut";
	}

	public void genTestbench() {
		// use a function for getting the current asserted value and
		// the current testing value. Return errors if there is no
		// such data available and/or the comparisons dont match.
		// Also run the hang timer and the cycle count file.

		final GenericJob gj = EngineThread.getGenericJob();
		final ForgeFileHandler fileHandler = EngineThread.getGenericJob()
				.getFileHandler();

		int hangTimerExpire = 1500;
		try {
			hangTimerExpire = Integer.parseInt(
					gj.getOption(OptionRegistry.HANG_TIMER)
							.getValue(CodeLabel.UNSCOPED).toString(), 10);
		} catch (Exception e) {
			hangTimerExpire = 1500;
		}

		final String designVerilogIdentifier = ID.toVerilogIdentifier(ID
				.showLogical(getDesign()));

		final IndentWriter pw;
		try {
			 pw = new IndentWriter(new PrintWriter(new FileWriter("/tmp/"+designVerilogIdentifier +
			 "_atb.v")));
			//pw = new IndentWriter(new PrintWriter(new FileWriter(
			//		fileHandler.getFile(TestBenchEngine.ATB))));
			 
		} catch (IOException ioe) {
			gj.warn(" Could not create atb file " + ioe);
			return;
		}

		List<TBIOHandler> handlers = new ArrayList<TBIOHandler>();
		for (FifoIF fifoIF : getDesign().getFifoInterfaces()) {
			if (fifoIF instanceof FifoInput) {
				handlers.add(new InputHandler((FifoInput) fifoIF));
			} else if (fifoIF instanceof FifoOutput) {
				handlers.add(new OutputHandler((FifoOutput) fifoIF));
			} else {
				throw new IllegalArgumentException(
						"Unknown type of fifo interface");
			}
		}

		StateHandler stateh = new StateHandler(getDesign());

		pw.println("`timescale 1ns/1ps");
		pw.println("`define legacy_model // Some simulators cannot handle the syntax of the new memory models.  This define uses a simpler syntax for the memory models in the unisims library");
		//final String simFile = fileHandler.getFile(
		//		VerilogTranslateEngine.SIMINCL).getAbsolutePath();
		//pw.println("`include \"" + simFile + "\"");
		pw.println("`timescale 1ns/1ps");
		pw.println("");
		pw.println("module fixture();");
		pw.inc();

		pw.println("reg            clk;");
		pw.println("reg            LGSR;");
		pw.println("reg            startSimulation;");
		pw.println("reg            run;");
		pw.println("wire           fire;");
		pw.println("wire           fireDone;");
		pw.println("reg [31:0]     hangTimer;");
		pw.println("reg [31:0]     clockCount;");
		pw.println("reg [31:0]     actionFiringCount;");
		pw.println("integer        resultFile;");
		pw.println("integer        stateDumpFile;");

		for (TBIOHandler ioh : handlers) {
			ioh.writeDeclarations(pw);
		}

		pw.println("always #25 clk = ~clk;");
		pw.println("//initial begin  $dumpfile(\"waves.vcd\");  $dumpvars;end");
		pw.println("assign glbl.GSR=LGSR;");

		// /////////////////
		// Instantiate the design to be tested: <name> dut (.x(y), .z(a), ....);
		// /////////////////
		pw.println(designVerilogIdentifier);
		pw.inc();
		pw.print(getInstanceName() + " (");
		pw.inc();
		for (TBIOHandler ioh : handlers) {
			ioh.writeInstantiation(pw);
		}
		for (Iterator<Design.ClockDomain> domainIter = design
				.getAllocatedClockDomains().iterator(); domainIter.hasNext();) {
			Design.ClockDomain domain = domainIter.next();
			pw.print("." + domain.getClockPin().getName() + "(clk)");
			if (domain.getResetPin() != null) {
				pw.print(", ." + domain.getResetPin().getName() + "(1'b0)");
			}
			if (domainIter.hasNext()) {
				pw.print(",");
				// pw.print(".CLK(clk)");
			}
		}
		pw.dec();
		pw.dec();
		pw.println(");");

		// /////////////////
		// Initialze the sequential elements controlling the simulation
		// /////////////////
		pw.println("initial begin");
		pw.inc();
		pw.println("clk <= 0;");
		pw.println("hangTimer <= 0;");
		pw.println("startSimulation <= 0;");
		pw.println("run <= 0;");
		final String simResults = "simResult"; 
				//fileHandler.getFile(TestBenchEngine.RESULTS).getAbsolutePath();
		final String simState = "simState";
				//fileHandler.getFile(TestBenchEngine.GenericTBEngine.STATE).getAbsolutePath();
		pw.println("resultFile <= $fopen(\"" + simResults + "\");");
		pw.println("stateDumpFile <= $fopen(\"" + simState + "\");");
		pw.println("clockCount <= 0;");
		pw.println("actionFiringCount <= 0;");

		for (TBIOHandler ioh : handlers) {
			ioh.writeInitial(pw);
		}

		pw.println("LGSR <= 1;");
		pw.println("#1 LGSR <= 0;");

		pw.println("#500 startSimulation <= 1;");
		pw.dec();
		pw.println("end");

		// /////////////////
		// Generate the logic to test the validity of I/O behavior
		// /////////////////
		pw.println("assign fire = " + getActionFiringString() + ";");
		pw.println("assign fireDone = " + getActionFiringDoneString() + ";");
		pw.println("always @(negedge clk) begin");
		pw.inc();
		pw.println("if (fireDone) begin");
		pw.inc();
		for (String capture : stateh.getCaptureStrings()) {
			pw.println(capture);
		}
		pw.println("$fwrite(stateDumpFile, \"MARK\\n\");");
		pw.dec();
		pw.println("end");
		pw.dec();
		pw.println("end");

		pw.println();
		pw.println("always @(posedge clk) begin");
		pw.inc();
		pw.println("run <= startSimulation; // ensure that we start handling on a rising edge");
		pw.println("if (run) begin");
		pw.inc();

		// Generate the logic for how to test for valid state of each
		// port
		for (TBIOHandler ioh : handlers) {
			ioh.writeTest(pw);
		}

		pw.dec();
		pw.println("end // else");
		pw.dec();
		pw.println("end // always");

		// The end of simulation code
		pw.println();
		pw.println("always @(posedge clk) begin");
		pw.inc();
		// Generate the condition on which the simulation completes.
		pw.println("if (!$actionFiringsRemain()) begin");
		pw.inc();
		pw.println("$fwrite(resultFile, \"PASSED in %d action firings (%d cycles)\\n\", actionFiringCount, clockCount);");
		pw.println("$display(\"PASSED\");");
		pw.println("$finish;");
		pw.dec();
		pw.println("end");
		pw.dec();
		pw.println("end");

		// The action firing mark code
		pw.println();
		pw.println("// negedge so that we know the action update happens before the queue update");
		pw.println("always @(negedge clk) begin");
		pw.inc();
		pw.println("if (fire) begin");
		pw.inc();
		pw.println("$markActionFiring();");
		pw.println("actionFiringCount <= actionFiringCount + 1;");
		pw.println("if (actionFiringCount[4:0] === 0) begin $display(\"Fire %d at cycle %d\", actionFiringCount, clockCount); end");
		pw.dec();
		pw.println("end");
		pw.dec();
		pw.println("end");

		// The hangtimer code
		pw.println();
		pw.println("always @(posedge clk) begin");
		pw.inc();
		pw.println("clockCount <= clockCount + 1;");
		pw.print("if (");
		for (Iterator<TBIOHandler> iter = handlers.iterator(); iter.hasNext();) {
			TBIOHandler ioh = iter.next();
			pw.print(ioh.getActivityName());
			if (iter.hasNext()) {
				pw.print(" || ");
			}
		}
		pw.println(") hangTimer <= 0;");
		pw.println("else begin");
		pw.inc();
		pw.println("hangTimer <= hangTimer + 1;");
		pw.println("if (hangTimer > " + hangTimerExpire + " ) begin");
		pw.inc();
		pw.println("$fwrite (resultFile, \"FAIL: Hang Timer expired after %d action firings (%d - "
				+ hangTimerExpire
				+ "cycles)\\n\", actionFiringCount, clockCount);");
		pw.println("$fwrite (resultFile, \"\\tPortName : TokenCount\\n\");");
		for (TBIOHandler ioh : handlers) {
			pw.println("$fwrite (resultFile, \"\\t" + ioh.getName()
					+ " : %d\\n\", " + ioh.getCountName() + ");");
		}
		pw.println("$finish;");
		pw.dec();
		pw.println("end");
		pw.dec();
		pw.println("end");
		pw.dec();
		pw.println("end");

		pw.dec();
		pw.println("endmodule // fixture");
	}

	/**
	 * This is a really kludgy way to find the GO signal for each internally
	 * fired task.
	 */
	private String getActionFiringString() {
		final List<String> terms = new ArrayList<String>();
		for (Component component : design.getDesignModule().getComponents()) {
			if (component instanceof Call) {
				Bus goBus = ((Call) component).getGoPort().getBus();
				if (goBus == null
						|| !(goBus.getOwner().getOwner() instanceof Kicker)) {
					Port goPort = ((Call) component).getGoPort();
					if (goPort.getValue().isConstant()) {
						continue;
					}
					PortWire cport = new PortWire(goPort);
					terms.add(cport.lexicalify().toString());
				}
			}
		}

		String result = "";
		for (Iterator<String> iter = terms.iterator(); iter.hasNext();) {
			// Prepend 'dut' which is the name of the instance we are
			// testing, of which all these terms will be signals.
			result += getInstanceName() + "." + iter.next();
			if (iter.hasNext()) {
				result += " || ";
			}
		}
		return result;
	}

	private String getActionFiringDoneString() {
		final List<String> terms = new ArrayList<String>();
		for (Component component : design.getDesignModule().getComponents()) {
			if (component instanceof Call) {
				// Bus goBus = ((Call)o).getGoPort().getBus();
				Bus doneBus = ((Call) component).getExit(Exit.DONE)
						.getDoneBus();
				if (doneBus != null) {
					BusWire bwire = new BusWire(doneBus);
					terms.add(bwire.lexicalify().toString());
				}
			}
		}

		String result = "";
		for (Iterator<String> iter = terms.iterator(); iter.hasNext();) {
			// Prepend 'dut' which is the name of the instance we are
			// testing, of which all these terms will be signals.
			result += getInstanceName() + "." + iter.next();
			if (iter.hasNext()) {
				result += " || ";
			}
		}
		return result;
	}

	private abstract class TBIOHandler {
		private String name;

		protected TBIOHandler(String name) {
			this.name = name;
			if (this.name.indexOf("_") > 0) {
				this.name = this.name.substring(0, this.name.lastIndexOf("_"));
			}
		}

		protected String getName() {
			return name;
		}

		public String getDataName() {
			return name + "_din";
		}

		public String getExistsName() {
			return name + "_exists";
		}

		public String getCountName() {
			return getDataName() + "_count";
		}

		public String getHandleName() {
			return name + "_id";
		}

		public String getVecFileName() {
			return GenericTestbenchWriter.this.getActorName() + "_" + getName()
					+ ".vec";
		}

		public abstract String getActivityName();

		public abstract void writeDeclarations(IndentWriter pw);

		public abstract void writeInstantiation(IndentWriter pw);

		public abstract void writeInitial(IndentWriter pw);

		public abstract void writeStartSim(IndentWriter pw);

		public abstract void writeTest(IndentWriter pw);

		@SuppressWarnings("unused")
		public void writeTermCondition(IndentWriter pw) {
			/* $isQueueEmpty(A_id) */
			// pw.print("$isQueueEmpty("+getHandleName()+")");
		}
	}

	private class OutputHandler extends TBIOHandler {
		private final FifoOutput output;

		public OutputHandler(FifoOutput out) {
			super(out.getDataPin().getName());
			output = out;
		}

		public int getDataWidth() {
			return output.getDataPin().getWidth();
		}

		@Override
		public String getDataName() {
			return getName() + "_dout";
		} // override super

		public String getExpectedName() {
			return this.getDataName() + "_expected";
		}

		public String getAckName() {
			return getName() + "_ack";
		}

		private String getSendName() {
			return getName() + "_send";
		}

		@Override
		public String getActivityName() {
			return getSendName();
		}

		@Override
		public void writeDeclarations(IndentWriter pw) {
			/*
			 * wire [31:0] O_dout; wire O_send; reg O_ack; reg O_exists; reg
			 * [31:0] O_expected; reg [31:0] O_dout_count; integer O_id;
			 */
			pw.println("wire [" + (getDataWidth() - 1) + ":0] " + getDataName()
					+ ";");
			pw.println("wire        " + getSendName() + ";");
			pw.println("reg         " + getAckName() + ";");
			pw.println("reg         " + getExistsName() + ";");
			pw.println("reg [" + (getDataWidth() - 1) + ":0]  "
					+ getExpectedName() + ";");
			pw.println("reg [31:0]  " + getCountName() + ";");
			pw.println("integer     " + getHandleName() + ";");
		}

		@Override
		public void writeInstantiation(IndentWriter pw) {
			Set<SimplePin> pins = new HashSet<SimplePin>(output.getPins());
			pins.remove(output.getDataPin());
			pins.remove(output.getSendPin());
			pins.remove(output.getAckPin());
			pw.print("." + output.getDataPin().getName() + "(" + getDataName()
					+ "), ");
			pw.print("." + output.getSendPin().getName() + "(" + getSendName()
					+ "), ");
			pw.print("." + output.getAckPin().getName() + "(" + getAckName()
					+ "), ");
			if (output.getReadyPin() != null) {
				pw.print("." + output.getReadyPin().getName() + "(1'b1), ");
				pins.remove(output.getReadyPin());
			}

			for (SimplePin pin : pins) {
				if (pin.getName().toUpperCase().contains("CLK")) {
					pw.print("." + pin.getName() + "(clk), ");
				}
				if (pin.getName().toUpperCase().contains("RESET")) {
					pw.print("." + pin.getName() + "(1'b0), ");
				} else if (pin.getName().toUpperCase().contains("CONTROL")) {
					pw.print("." + pin.getName() + "(), ");
				} else {
					pw.print("/* ." + pin.getName() + "(),*/ ");
				}
			}
		}

		@Override
		public void writeInitial(IndentWriter pw) {
			/*
			 * O_dout_count <= 0; O_id <= $registerVectorFile("MY_X.vec", X_din,
			 * X_exists); O_exists <= 0; O_expected <= 0; O_ack <= 1; // For now
			 * say we are always asserting ack
			 */
			pw.println("" + getCountName() + " <= 0;");
			pw.println("" + getHandleName() + " <= $registerVectorFile(\""
					+ getVecFileName() + "\", " + getExpectedName() + ", "
					+ getExistsName() + ");");
			pw.println("" + getExistsName() + " <= 0;");
			pw.println("" + getExpectedName() + " <= 0;");
			pw.println("" + getAckName()
					+ " <= 1; // For now say we are always acking");
		}

		@Override
		public void writeStartSim(IndentWriter pw) {
			// $vectorPop(O_id);
		}

		@Override
		public void writeTest(IndentWriter pw) {
			/*
			 * if (O_write) begin if (!O_exists) begin $fwrite(resultFile,
			 * "FAIL: Token output from port O when no output was expected.  Output token %x at count %d\n"
			 * , O_dout, O_dout_count); #100 $finish; end else if (O_dout !==
			 * O_expected) begin $fwrite(resultFile,
			 * "FAIL: Incorrect result on port O.  output token count %d expected %d found %d\n"
			 * , O_dout_count, O_expected, O_dout); #100 $finish; end
			 * O_dout_count <= O_dout_count + 1; $vectorPop(O_id); // May be
			 * invalid if beyond the end of the queue end // if (O_write) else
			 * begin $vectorPeek(O_id); end
			 */
			pw.println("if (" + getSendName() + ") begin");
			pw.inc();
			pw.println("if (!" + getExistsName() + ") begin");
			pw.inc();
			pw.println("$fwrite(resultFile, \"FAIL: Token output from port "
					+ getName()
					+ " when no output was expected.  Output token %x at count %d\\n\", "
					+ getDataName() + ", " + getCountName() + ");");
			pw.println("#100 $finish;");
			pw.dec();
			pw.println("end");
			pw.println("else if (" + getDataName() + " !== "
					+ getExpectedName() + ") begin");
			pw.inc();
			pw.println("$fwrite(resultFile, \"FAIL: Incorrect result on port "
					+ getName()
					+ ".  output token count %d expected %d found %d\\n\", "
					+ getCountName() + ", " + getExpectedName() + ", "
					+ getDataName() + ");");
			pw.println("#100 $finish;");
			pw.dec();
			pw.println("end");
			pw.println("" + getCountName() + " <= " + getCountName() + " + 1;");
			pw.println("$vectorPop(" + getHandleName()
					+ "); // May be invalid if beyond the end of the queue");
			pw.dec();
			pw.println("end // if (" + getSendName() + ")");
			pw.println("else begin");
			pw.inc();
			pw.println("$vectorPeek(" + getHandleName() + ");");
			pw.dec();
			pw.println("end");
		}
	}

	private class InputHandler extends TBIOHandler {
		private final FifoInput input;

		public InputHandler(FifoInput in) {
			super(in.getDataPin().getName());
			input = in;
		}

		public int getDataWidth() {
			return input.getDataPin().getWidth();
		}

		public String getAckName() {
			return getName() + "_read";
		}

		@Override
		public String getActivityName() {
			return getAckName();
		}

		@Override
		public void writeDeclarations(IndentWriter pw) {
			/*
			 * reg [N:0] din; wire read; reg exists; reg [31:0] count;
			 */
			pw.println("reg\t[" + (getDataWidth() - 1) + ":0] " + getDataName()
					+ ";");
			pw.println("wire\t\t" + getAckName() + ";");
			pw.println("reg\t\t" + getExistsName() + ";");
			pw.println("reg\t[31:0] " + getCountName() + ";");
			pw.println("integer\t\t " + getHandleName() + ";");
		}

		@Override
		public void writeInstantiation(IndentWriter pw) {
			Set<SimplePin> pins = new HashSet<SimplePin>(input.getPins());
			pins.remove(input.getDataPin());
			pins.remove(input.getAckPin());
			pins.remove(input.getSendPin());

			pw.print("." + input.getDataPin().getName() + "(" + getDataName()
					+ "), ");
			pw.print("." + input.getAckPin().getName() + "(" + getAckName()
					+ "), ");
			pw.print("." + input.getSendPin().getName() + "(" + getExistsName()
					+ "), ");
			for (SimplePin pin : pins) {
				if (pin.getName().toUpperCase().contains("CLK")) {
					pw.print("." + pin.getName() + "(clk), ");
				}
				if (pin.getName().toUpperCase().contains("RESET")) {
					pw.print("." + pin.getName() + "(1'b0), ");
				} else if (pin.getName().toUpperCase().contains("CONTROL")) {
					pw.print("." + pin.getName() + "(1'b0), ");
				} else {
					pw.print("/* ." + pin.getName() + "(),*/");
				}
			}
		}

		@Override
		public void writeInitial(IndentWriter pw) {
			/*
			 * A_din_count <= 0; A_id <= $registerVectorFile("MY_A.vec");
			 * A_exists <= 0; A_din <= 0;
			 */
			pw.println(getCountName() + " <= 0;");
			pw.println(getHandleName() + " <= $registerVectorFile(\""
					+ getVecFileName() + "\", " + getDataName() + ", "
					+ getExistsName() + ");");
			pw.println(getExistsName() + " <= 0;");
			pw.println(getDataName() + " <= 0;");
		}

		@Override
		public void writeStartSim(IndentWriter pw) {
			// $vectorPop(A_id);
		}

		@Override
		public void writeTest(IndentWriter pw) {
			/*
			 * if (A_read) begin if (!A_exists) begin $fwrite(resultFile,
			 * "FAIL: Illegal read from empty queue, port A on %d token\n",
			 * A_din_count); #100 $finish; end $vectorPop(A_id); end else begin
			 * $vectorPeek(A_id); end
			 */
			pw.println("if (" + getAckName() + ") begin");
			pw.inc();
			pw.println("if (!" + getExistsName() + ") begin");
			pw.inc();
			pw.println("$fwrite(resultFile, \"FAIL: Illegal read from empty queue, port "
					+ getName() + " on %d token\\n\", " + getCountName() + ");");
			pw.println("#100 $finish;");
			pw.dec();
			pw.println("end");
			pw.println("" + getCountName() + " <= " + getCountName() + " + 1;");
			pw.println("$vectorPop(" + getHandleName() + ");");
			pw.dec();
			pw.println("end else begin");
			pw.inc();
			pw.println("$vectorPeek(" + getHandleName() + ");");
			pw.dec();
			pw.println("end");
		}

	}

	private class StateHandler {
		Map<String, String> names = new HashMap<String, String>();

		private StateHandler(Design design) {
			for (Register reg : design.getRegisters()) {
				Register.Physical phys = (Register.Physical) reg
						.getPhysicalComponent();
				Bus output = phys.getRegisterOutput();
				BusWire bwire = new BusWire(output);
				GenericModule gmod = new GenericModule(phys);
				String instance = gmod.makeInstance().getIdentifier()
						.toString();
				String netName = "fixture.dut." + instance + "."
						+ bwire.lexicalify().toString();

				// Trim the 'register_xxxx_' off the front and the
				// '_1' off the tail
				if (instance.startsWith("register_")) {
					instance = instance.substring(instance.indexOf("_", 9) + 1);
				}
				if (instance.endsWith("_1")) {
					instance = instance.substring(0, instance.length() - 2);
				}

				names.put(instance, netName);
			}
		}

		public List<String> getCaptureStrings() {
			List<String> keys = new ArrayList<String>(names.keySet());
			if (keys.isEmpty()) {
				return Collections.emptyList();
			}
			List<String> alpha = new LinkedList<String>();
			alpha.add(keys.remove(0));
			// Sort the list alphabetically
			for (String str : keys) {
				boolean inserted = false;
				for (int i = 0; i < alpha.size(); i++) {
					if (str.compareTo(alpha.get(i)) < 0) {
						alpha.add(i, str);
						inserted = true;
						break;
					}
				}
				if (!inserted) {
					alpha.add(str);
				}
			}

			// Use the sorted alpha keys to assemble the strings
			List<String> netDisplay = new ArrayList<String>();
			for (String key : alpha) {
				String netName = names.get(key);
				netDisplay.add("$fwrite(stateDumpFile, \"" + key
						+ " : %d\\n\", " + netName + ");");
			}

			return netDisplay;
		}

	}

}
