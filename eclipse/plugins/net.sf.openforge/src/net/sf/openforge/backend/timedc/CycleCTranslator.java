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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.sf.openforge.app.EngineThread;
import net.sf.openforge.app.ForgeFileTyper;
import net.sf.openforge.app.OptionRegistry;
import net.sf.openforge.lim.And;
import net.sf.openforge.lim.ArrayRead;
import net.sf.openforge.lim.ArrayWrite;
import net.sf.openforge.lim.Block;
import net.sf.openforge.lim.Branch;
import net.sf.openforge.lim.Bus;
import net.sf.openforge.lim.Call;
import net.sf.openforge.lim.Component;
import net.sf.openforge.lim.DataFlowVisitor;
import net.sf.openforge.lim.Decision;
import net.sf.openforge.lim.Design;
import net.sf.openforge.lim.EncodedMux;
import net.sf.openforge.lim.Exit;
import net.sf.openforge.lim.FilteredVisitor;
import net.sf.openforge.lim.ForBody;
import net.sf.openforge.lim.HeapRead;
import net.sf.openforge.lim.HeapWrite;
import net.sf.openforge.lim.IPCoreCall;
import net.sf.openforge.lim.InBuf;
import net.sf.openforge.lim.Kicker;
import net.sf.openforge.lim.Latch;
import net.sf.openforge.lim.Loop;
import net.sf.openforge.lim.Module;
import net.sf.openforge.lim.Mux;
import net.sf.openforge.lim.Not;
import net.sf.openforge.lim.Or;
import net.sf.openforge.lim.OutBuf;
import net.sf.openforge.lim.PinRead;
import net.sf.openforge.lim.PinReferee;
import net.sf.openforge.lim.PinStateChange;
import net.sf.openforge.lim.PinWrite;
import net.sf.openforge.lim.Port;
import net.sf.openforge.lim.PriorityMux;
import net.sf.openforge.lim.Referenceable;
import net.sf.openforge.lim.Reg;
import net.sf.openforge.lim.Register;
import net.sf.openforge.lim.RegisterGateway;
import net.sf.openforge.lim.RegisterRead;
import net.sf.openforge.lim.RegisterReferee;
import net.sf.openforge.lim.RegisterWrite;
import net.sf.openforge.lim.SRL16;
import net.sf.openforge.lim.Scoreboard;
import net.sf.openforge.lim.Switch;
import net.sf.openforge.lim.Task;
import net.sf.openforge.lim.TaskCall;
import net.sf.openforge.lim.TriBuf;
import net.sf.openforge.lim.UnexpectedVisitationException;
import net.sf.openforge.lim.UntilBody;
import net.sf.openforge.lim.WhileBody;
import net.sf.openforge.lim.io.FifoAccess;
import net.sf.openforge.lim.io.FifoRead;
import net.sf.openforge.lim.io.FifoWrite;
import net.sf.openforge.lim.io.SimpleInternalPin;
import net.sf.openforge.lim.io.SimplePin;
import net.sf.openforge.lim.io.SimplePinAccess;
import net.sf.openforge.lim.io.SimplePinRead;
import net.sf.openforge.lim.io.SimplePinWrite;
import net.sf.openforge.lim.memory.AbsoluteMemoryRead;
import net.sf.openforge.lim.memory.AbsoluteMemoryWrite;
import net.sf.openforge.lim.memory.EndianSwapper;
import net.sf.openforge.lim.memory.MemoryBank;
import net.sf.openforge.lim.memory.MemoryGateway;
import net.sf.openforge.lim.memory.MemoryRead;
import net.sf.openforge.lim.memory.MemoryReferee;
import net.sf.openforge.lim.memory.MemoryWrite;
import net.sf.openforge.lim.op.AddOp;
import net.sf.openforge.lim.op.AndOp;
import net.sf.openforge.lim.op.CastOp;
import net.sf.openforge.lim.op.ComplementOp;
import net.sf.openforge.lim.op.ConditionalAndOp;
import net.sf.openforge.lim.op.ConditionalOrOp;
import net.sf.openforge.lim.op.DivideOp;
import net.sf.openforge.lim.op.EqualsOp;
import net.sf.openforge.lim.op.GreaterThanEqualToOp;
import net.sf.openforge.lim.op.GreaterThanOp;
import net.sf.openforge.lim.op.LeftShiftOp;
import net.sf.openforge.lim.op.LessThanEqualToOp;
import net.sf.openforge.lim.op.LessThanOp;
import net.sf.openforge.lim.op.MinusOp;
import net.sf.openforge.lim.op.ModuloOp;
import net.sf.openforge.lim.op.MultiplyOp;
import net.sf.openforge.lim.op.NoOp;
import net.sf.openforge.lim.op.NotEqualsOp;
import net.sf.openforge.lim.op.NotOp;
import net.sf.openforge.lim.op.NumericPromotionOp;
import net.sf.openforge.lim.op.OrOp;
import net.sf.openforge.lim.op.OrOpMulti;
import net.sf.openforge.lim.op.PlusOp;
import net.sf.openforge.lim.op.ReductionOrOp;
import net.sf.openforge.lim.op.RightShiftOp;
import net.sf.openforge.lim.op.RightShiftUnsignedOp;
import net.sf.openforge.lim.op.ShortcutIfElseOp;
import net.sf.openforge.lim.op.SubtractOp;
import net.sf.openforge.lim.op.TimingOp;
import net.sf.openforge.lim.op.XorOp;
import net.sf.openforge.util.naming.ID;

/**
 * The CycleCTranslator converts our LIM data structure to a C model of the
 * functionality described by the LIM. This C Model is a cycle accurate model
 * which is designed to integrate into a cycle-by-cycle simulation environment.
 * <p>
 * The generated C code is divided into two functions. The first function (
 * <code>update</code>) is responsible for propagating all combinational data
 * paths. The values of all sequential elements are known and thus provide the
 * 'inputs' to this function (via global variables). The update function
 * contains logic for every node in the LIM in order to calculate the 'next'
 * value for each sequential element and/or memory access. This
 * <code>update</code> function contains a loop which iterates over all the
 * logic 3 times. This handles the fact that not all LIM feedback points occur
 * at a sequential element. Specifically, when we do loop flop removal, the
 * feedback point is an or gate. To correctly propagate these feedback signals
 * we need to iterate over the logic 3 times.
 * <p>
 * The second function (<code>clockEdge</code>) is responsible for moving all
 * the calculated 'next state' for each sequential element into the 'current
 * state' postion. This is equivalent to a flop clocking in its data input port.
 * The completion of this function leaves the simulation model in a state where
 * the sequential elements are all correct for the given clock cycle, and the
 * <code>update</code> function needs to be called to generate the 'next'
 * values.
 * <p>
 * Each sequential element (flops, SRL16, memory, register, etc) is modelled by
 * a combination of a state preserving variable (primitive type or array
 * depending on the type of element) and variable(s) which model the various
 * ways of interacting with that element. All these variables are global
 * variables in order to maintain the state. The extra (non state bearing)
 * variables model the behavior of the read/write strobes, address, and size
 * ports of the stateful resource so that during the clockEdge function the
 * appropriate action is taken.
 * 
 * 
 * <p>
 * Created: Wed Mar 2 20:50:55 2005
 * 
 * @author imiller, last modified by $Author: imiller $
 * @version $Id: CycleCTranslator.java 424 2007-02-26 22:36:09Z imiller $
 */
public class CycleCTranslator {

	/**
	 * Map of Component to the OpHandle (StateVar) for that component.
	 * Containing all the sequential elements in the design.
	 */
	private final Map sequentialElements = new LinkedHashMap();

	private final File headerFile;

	/*
	 * A Map of Referenceable to object, may be a MemoryVar or a RegisterVar.
	 */
	private final Map<Referenceable, StateVar> referenceableMap = new LinkedHashMap<Referenceable, StateVar>();

	/**
	 * A class which ensures unique naming for all lim objects.
	 * System.identityHashCode wont do it for us!
	 */
	private final CNameCache nameCache = new CNameCache();

	private final IOHandler ioHandle;

	/**
	 * A set of var names that define the 'go' to each task EXCLUSIVE of those
	 * that are derived from kickers. ie, this is the set of vars that enable
	 * tasks which are called from other tasks. The set up of this set is really
	 * kludgy right now, depending on the var names being associated with
	 * SimpleInternalPins and containing the string '_go_'
	 */
	private Set<String> taskGoVars = null;

	/**
	 * Access via the static translate method.
	 */
	private CycleCTranslator(Design design, File headerFile) {
		ioHandle = IOHandler.makeIOHandler(design);

		this.headerFile = headerFile;
	}

	public static void translate(Design design, File headerFile, File sourceFile)
			throws CycleCTranslatorException {
		// _timedc.d.launchXGraph(design, false);
		// _timedc.d.modGraph(design, "graph");

		String baseName = EngineThread.getGenericJob().getOutputBaseName();
		final boolean doVPCompliant = EngineThread.getGenericJob()
				.getUnscopedBooleanOptionValue(
						OptionRegistry.X_WRITE_CYCLE_C_VPGEN);

		final String funcName;
		if (doVPCompliant) {
			// If we are doing a VP compatible compile, make the name
			// of the generated function unique. Use the same name as
			// from the Verilog module.
			funcName = CNameCache.getLegalIdentifier(ID.showLogical(design));
		} else {
			//
			// For integration with System Generator it is easier if
			// the function name is predictable. So explicitly set
			// its name here to 'update'
			//
			funcName = "update";
		}

		PrintStream ps = null;
		PrintStream headerPS = null;
		try {
			ps = new PrintStream(new FileOutputStream(sourceFile), true);
			headerPS = new PrintStream(new FileOutputStream(headerFile), true);
		} catch (IOException ioe) {
			throw new FileHandlingException("Could not create output stream "
					+ ioe.getMessage());
		}

		CycleCTranslator xlat = new CycleCTranslator(design, headerFile);
		xlat.writeHeaderFile(ps, headerPS, funcName);
		xlat.writeFile(design, funcName, ps);

		if (doVPCompliant) {
			try {
				VPGenWrapper vpWrapper = new VPGenWrapper(xlat.ioHandle,
						funcName, xlat.headerFile);
				vpWrapper.generateVPWrapper(sourceFile);
			} catch (IOException ioe) {
				throw new FileHandlingException(
						"Could not create output stream " + ioe.getMessage());
			}
		} else {
			xlat.writeDebugMain(ps, funcName, baseName);
		}

		ps.flush();
		headerPS.flush();
		ps.close();
		headerPS.close();
	}

	private void writeFile(Design design, String funcName, PrintStream ps) {
		createSequentialVars(design, ps);
		buildLogic(design, funcName, ps);
		// Write the logic for updating at the clock edge
		writeTick(ps, funcName);
		writeAPI(ps, false);
	}

	private void writeDebugMain(PrintStream ps, String funcName, String baseName) {
		if (EngineThread.getGenericJob().getUnscopedBooleanOptionValue(
				OptionRegistry.AUTO_TEST_BENCH)) {
			final File[] inputFiles = EngineThread.getGenericJob()
					.getTargetFiles();
			ps.println("#ifdef FORGE_ATB");
			if (ForgeFileTyper.isXLIMSource(inputFiles[0].getName()))
				CycleCTestbench.writeQueueTestingMain(ps, funcName, baseName,
						ioHandle, taskGoVars);
			else if (ForgeFileTyper.isCSource(inputFiles[0].getName()))
				CycleCTestbench.writeVECTestingMain(ps, funcName, baseName,
						ioHandle);
			ps.println("#endif");
		}

		// ps.println();
		// ps.println("#ifdef FORGE_DEBUG");
		// ps.println("/* This function is intended only for testing. */");
		// ps.println("int main () {");
		// ps.println("\tstruct fsl_input in = {0x11223346, 1, 0};");
		// ps.println("\tstruct fsl_output out = {0,0,0};");
		// ps.println("\tint i;");
		// ps.println("\tint j=0;");
		// ps.println("\tfor(i=0; i < 40; i++) {");
		// ps.println("\t\t"+funcName+"(&in, &out);");
		// ps.println("\t\tclockEdge();");
		// ps.println("\t\tprintf(\"i: %d inData: %x rd: %d outData: %x outWr: %d\\n\",i, in.data, in.read, out.data, out.write);");
		// ps.println("\t\tprintf(\"-------\\n\");");
		// ps.println("\t\tif (in.read) { in.data += j++; }");
		// ps.println("\t}");
		// ps.println("}");
		// ps.println("#endif");
		// ps.println();
		// ps.println();
	}

	private void writeHeaderFile(PrintStream ps, PrintStream headerPS,
			String baseName) {
		// final String protectDefine = "__" + baseName.toUpperCase() + "_H_";
		// Because these types are going to be the same across all
		// generated cores, we need to use a common name that is not
		// based on the compiled function name
		final String protectDefine = "__FORGE_C_MODEL_TYPES_H_";
		headerPS.println("#ifndef " + protectDefine);
		headerPS.println("#define " + protectDefine);
		ioHandle.writeTypeDecls(headerPS);
		MemoryVar.declareType(headerPS);
		headerPS.println(getUpdateFunctionDecl(baseName) + ";");
		headerPS.println(getClockEdgeFunctionDecl(baseName) + ";");
		writeAPI(headerPS, true);
		headerPS.println("#endif");

		ps.println("#include \"" + headerFile.getName() + "\"");
		// The string class is needed for the getID API function in the
		// IOHandler API.
		ps.println("#include <string.h>");
	}

	private void buildLogic(Design design, String funcName, PrintStream ps) {
		// Write the I/O handling data structures and their inits.
		ioHandle.declareStructures(ps);

		// The stream for the logic of the design update function
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		PrintStream designFxnPS = new PrintStream(baos);

		// Create the TranslateVisitor and visit the design. Because
		// the TranslateVisitor creates new instances of itself for
		// each task, this tvis will hold handles to only the design
		// module level components.
		final TranslateVisitor tvis = new TranslateVisitor(designFxnPS, ps);
		tvis.opHandles.putAll(sequentialElements);
		final Set<OpHandle> preDefinedOpHandles = new HashSet<OpHandle>(
				tvis.opHandles.values());
		design.accept(tvis);

		// Print the task definitions/implementations
		for (ByteArrayOutputStream taskImpl : tvis.getTaskImplementations()
				.values()) {
			ps.println();
			ps.print(taskImpl.toString());
		}

		// Print the design update definition. The update function is
		// the propagation of all combinational paths in the design.
		// This function takes the form of:
		// initialize all output pins to 0
		// initialize all internal pins to 0
		// iterate until settled:
		// propagate all design module logic
		// call each task function (each iterates until settled)
		// iterate = true if any design level logic changed
		ps.println(getUpdateFunctionDecl(funcName) + "{");

		// Each output value must be initialized to 0 so that the pin
		// writes can use a logical OR to update their value.
		ps.println("/* Initialize each output (produced value) to zero so that a 'wired-or' \n can be used to derive the final value */");
		ioHandle.writeOutputInits(ps);
		// Each internal pin always resets to zero at a clock edge
		// because they are simply wires. Effectively this is
		// allowing us to do a wired or for its behavior.
		taskGoVars = new HashSet<String>();
		for (Map.Entry<Component, OpHandle> entry : tvis.opHandles.entrySet()) {
			// XXX: FIXME -- Kludgy but works...
			if (entry.getKey() instanceof SimpleInternalPin) {
				OpHandle handle = entry.getValue();
				SimpleInternalPin pin = (SimpleInternalPin) entry.getKey();
				ps.println(handle.assign(pin.getXLatData().getSource(), "0"));
				String name = handle.getBusName(pin.getXLatData().getSource());
				if (name.contains("_go_")) {
					taskGoVars.add(name);
				}
			}
		}

		// Write out declarations for 'previous' values of design
		// level components. Because this uses the 'isDeclared'
		// method in OpHandles, this functionality cannot be executed
		// until the visitor has visited all the logic of the design
		// module.
		ArrayList<Bus> fbBuses = new ArrayList<Bus>();
		for (Map.Entry<Component, OpHandle> entry : tvis.opHandles.entrySet()) {
			OpHandle handle = entry.getValue();
			// Any opHandle that was 'pre defined' was not discovered
			// as a feedback point in the design. Therefor we can
			// safely ignore them in the calculation of the iterate
			// variable.
			if (preDefinedOpHandles.contains(handle)) {
				continue;
			}
			for (Exit exit : entry.getKey().getExits()) {
				if (exit.getTag().getType() == Exit.SIDEBAND) {
					continue;
				}
				for (Bus b : exit.getBuses()) {
					// If the bus has not been declared by now then it
					// did not figure into the design level logic and
					// is 'unused'
					if (!handle.isDeclared(b, ""))
						continue;
					fbBuses.add(b);
					ps.println(handle.declare(b, "_prev"));
				}
			}
		}

		// Set up the iterative portion of the design module logic
		final String iterateVar = "iterate";
		ps.println("int " + iterateVar + " = 1;");
		ps.println("while (" + iterateVar + ") {");
		ps.println(iterateVar + " = 0;");

		// Write out the logic for the design module functionality
		ps.println(baos.toString());
		for (String taskCall : tvis.getTaskImplementations().keySet()) {
			ps.println(taskCall + "();");
		}

		// Write out the iterate decision logic
		for (Bus b : fbBuses) {
			OpHandle handle = tvis.opHandles.get(b.getOwner().getOwner());
			ps.println(iterateVar + " |= (" + handle.getBusName(b, "_prev")
					+ " != " + handle.getBusName(b) + ");");
			ps.println(handle.getBusName(b, "_prev") + " = "
					+ handle.getBusName(b) + ";");
		}
		ps.println("} // while(iterate)");

		// //////

		ps.println("}"); // Close the update function decl
	}

	private void writeTick(PrintStream ps, String baseName) {
		ps.println("/* Logic for updating state variables at the clock edge */");
		ps.println(getClockEdgeFunctionDecl(baseName) + " {");

		for (Iterator<OpHandle> iter = sequentialElements.values().iterator(); iter
				.hasNext();) {
			((StateVar) iter.next()).writeTick(ps);
		}

		ps.println("} /* end " + baseName + "_clockEdge */");
	}

	private void writeAPI(PrintStream ps, boolean declarationOnly) {
		ps.println("\n\n// API calls");
		ioHandle.writeGetId(ps, declarationOnly);
		ioHandle.writeIsSending(ps, declarationOnly);
		ioHandle.writeIsAcking(ps, declarationOnly);
		ioHandle.writeGetValue(ps, declarationOnly);
		ioHandle.writeSetValue(ps, declarationOnly);
	}

	// private String getUpdateFunctionDecl (String baseName) { return ("void "
	// + baseName + "_update (" + this.ioHandle.getInputDecl() + ", " +
	// this.ioHandle.getOutputDecl() + ")"); }
	private String getUpdateFunctionDecl(String baseName) {
		return ("void " + baseName + "_update ()");
	}

	private String getClockEdgeFunctionDecl(String baseName) {
		return ("void " + baseName + "_clockEdge()");
	}

	private void createSequentialVars(Design design, PrintStream stream) {
		referenceableMap.putAll(MemoryWriter.generateMemories(design));
		StateComponentFinder finder = new StateComponentFinder(nameCache,
				referenceableMap);
		design.accept(finder);

		for (Iterator<StateVar> iter = referenceableMap.values().iterator(); iter
				.hasNext();) {
			iter.next().declareGlobal(stream);
		}
		for (Iterator<StateVar> iter = finder.getSeqElements().values()
				.iterator(); iter.hasNext();) {
			iter.next().declareGlobal(stream);
		}

		sequentialElements.putAll(finder.getSeqElements());
	}

	/**
	 * A lightweight visitor to pre-identify all of the feedback points
	 */
	private static class FeedbackFinder extends FilteredVisitor {
		/*
		 * A Set of Component objects, as returned by module.getFeedbackPoints
		 */
		private final Set<Component> fbPoints = new LinkedHashSet<Component>();

		/**
		 * Returns a Set of all components which are the feedback points in the
		 * LIM.
		 */
		public Set<Component> getFbPoints() {
			return fbPoints;
		}

		@Override
		public void visit(Task t) {
			// The call is a feedback point because of the fact that
			// the done feeds back to the kicker.
			// this.fbPoints.add(t.getCall());
			super.visit(t);
		}

		@Override
		public void preFilter(Module m) {
			fbPoints.addAll(m.getFeedbackPoints());
		}
	}

	private class TranslateVisitor extends DataFlowVisitor {
		/* A map of Component to OpHandle */
		Map<Component, OpHandle> opHandles = new HashMap<Component, OpHandle>();
		private final PrintStream ps;
		private final PrintStream alternateDeclarationPS; // may be null
		private final Map<ID, ID> callBoundryMap = new HashMap<ID, ID>();
		private final Map<String, ByteArrayOutputStream> taskImplementationMap = new LinkedHashMap<String, ByteArrayOutputStream>();

		public TranslateVisitor(PrintStream ps) {
			this(ps, null);
		}

		private TranslateVisitor(PrintStream ps, PrintStream declarationStream) {
			setRunForward(true);
			this.ps = ps;
			alternateDeclarationPS = declarationStream;
		}

		public Map<String, ByteArrayOutputStream> getTaskImplementations() {
			return Collections.unmodifiableMap(taskImplementationMap);
		}

		@Override
		public void visit(Design design) {
			// The design module contains the kickers.
			List<Call> taskCalls = new ArrayList<Call>();
			for (Task task : design.getTasks()) {
				taskCalls.add(task.getCall());
			}
			// First, create op handles for ALL the components in the
			// design module
			for (Component comp : design.getDesignModule().getComponents()) {
				makeOpHandle(comp);
			}
			// Now visit the components and connect them, not
			// traversing the calls. These will be handled when we
			// visit tasks later.
			for (Component comp : design.getDesignModule().getComponents()) {
				if (!taskCalls.contains(comp)) {
					try {
						comp.accept(this);
					}
					// Anything that throws a UVE is not going to
					// factor into the c translation anyway.
					catch (UnexpectedVisitationException uve) {
					}
				} else {
					// Simply pre-declare the outputs of the task
					// calls so that they become declared at global
					// scope. This allows them to be connected to
					// other task calls.
					OpHandle handle = makeOpHandle(comp);
					for (Exit exit : comp.getExits()) {
						for (Bus bus : exit.getBuses())
							preDeclare(handle, bus);
					}
				}
			}

			// The iterator above works with the super traversal to
			// ensure that everything in the design module is
			// traversed only 1 time.
			for (Task task : design.getTasks()) {
				final ByteArrayOutputStream baos = new ByteArrayOutputStream();
				final TranslateVisitor tvis = new TranslateVisitor(
						new PrintStream(baos));
				tvis.opHandles.putAll(opHandles);
				task.accept(tvis);
				final String functionName = CNameCache.getLegalIdentifier(ID
						.showLogical(task) + "_logic");
				taskImplementationMap.put(functionName, baos);
			}
		}

		@Override
		public void visit(Task task) {
			final String functionName = CNameCache.getLegalIdentifier(ID
					.showLogical(task) + "_logic");
			outln("void " + functionName + " () {");

			final String iterateVar = "iterate";
			String id = ID.showLogical(task);
			outln("// Beginning of task " + id);
			FeedbackFinder fbFinder = new FeedbackFinder();
			task.accept(fbFinder);
			final Set<Component> fbPoints = new LinkedHashSet<Component>(
					fbFinder.getFbPoints());
			fbPoints.removeAll(sequentialElements.keySet());
			final Set<Bus> fbBuses = new LinkedHashSet<Bus>();
			for (Component comp : fbPoints) {
				OpHandle handle = makeOpHandle(comp);
				for (Exit exit : comp.getExits()) {
					if (exit.getTag().getType() == Exit.SIDEBAND) {
						continue;
					}
					for (Bus b : exit.getBuses()) {
						fbBuses.add(b);
						outln(handle.declare(b));
						outln(handle.declare(b, "_prev"));
					}
				}
			}
			outln("unsigned int " + iterateVar + "=1;");
			outln("while(" + iterateVar + ") {");

			super.visit(task);

			outln(iterateVar + "=0;");
			for (Bus b : fbBuses) {
				OpHandle handle = opHandles.get(b.getOwner().getOwner());
				outln(iterateVar + " |= (" + handle.getBusName(b, "_prev")
						+ " != " + handle.getBusName(b) + ");");
				outln(handle.getBusName(b, "_prev") + " = "
						+ handle.getBusName(b) + ";");
			}

			outln("} /* end while(" + iterateVar + ") loop */");
			outln("// end of task " + id);

			outln("} // end of  " + functionName + " implementation");
		}

		@Override
		public void visit(Call call) {
			if (!opHandles.containsKey(call)) {
				// The top level call, due to feedback from done to
				// kicker, should already be created, but other calls
				// may not be.
				makeOpHandle(call);
			}

			// Create correlations from call port/bus to block port/bus
			for (Port port : call.getPorts()) {
				Port match = call.getProcedurePort(port);
				callBoundryMap.put(match, port);
			}
			for (Bus bus : call.getBuses()) {
				Bus match = call.getProcedureBus(bus);
				callBoundryMap.put(match, bus);
			}
			super.visit(call);
		}

		@Override
		public void visit(InBuf ib) {
			List<Bus> buses = new ArrayList<Bus>();
			buses.add(ib.getGoBus());
			buses.add(ib.getResetBus());
			buses.addAll(ib.getDataBuses());
			OpHandle ibHandler = makeOpHandle(ib);
			for (Bus ibBus : buses) {
				if (ibBus.getTag() == Component.SIDEBAND) {
					continue;
				}
				Bus source = ibBus.getPeer().getBus();
				if (source == null
						&& callBoundryMap.containsKey(ibBus.getPeer())) {
					source = ((Port) callBoundryMap.get(ibBus.getPeer()))
							.getBus();
				}

				if (source != null) {
					preDeclare(ibHandler, ibBus);
					outln(ibHandler.assign(ibBus, getRValue(source)));
				} else {
					// Some ports, eg clk/reset, may not be used
					if (ibBus.isUsed() && ibBus.getPeer().isUsed()) {
						throw new UnexpectedStructureException(
								"null source for " + ib + " " + ibBus);
					}
				}
			}
		}

		@Override
		public void visit(OutBuf ob) {
			OpHandle ownerHandler = opHandles.get(ob.getOwner());
			assert ownerHandler != null : "Null owner handler for outbuf "
					+ ob.getOwner();

			for (Port port : ob.getPorts()) {
				if (port.getTag() == Component.SIDEBAND) {
					continue;
				}
				Bus source = port.getBus();
				Bus target = port.getPeer();
				if (source != null) {
					preDeclare(ownerHandler, target);
					outln(ownerHandler.assign(target, getRValue(source)));
					if (callBoundryMap.containsKey(target)) {
						Bus peer = (Bus) callBoundryMap.get(target);
						OpHandle callHandler = opHandles.get(peer.getOwner()
								.getOwner());
						assert callHandler != null : "Null call handler "
								+ peer.getOwner().getOwner();
						preDeclare(callHandler, peer);
						outln(callHandler.assign(peer, getRValue(target)));
					}
				} else {
					if (port.isUsed() && target.isUsed()) {
						throw new UnexpectedStructureException(
								"null source for " + ob + " " + port);
					}
				}
			}
		}

		@Override
		public void visit(Reg reg) {
			RegVar var = (RegVar) opHandles.get(reg);
			assert var != null : "Unknown register found";

			int type = reg.getType();
			switch (type) {
			case Reg.REG:
				/*
				 * dout = din;
				 */
				outln(var.getDataIn() + " = "
						+ getRValue(reg.getDataPort().getBus()) + ";");
				break;
			case Reg.REGE:
				outln("if (" + getRValue(reg.getEnablePort().getBus()) + ") {");
				outln(var.getDataIn() + " = "
						+ getRValue(reg.getDataPort().getBus()) + ";");
				outln("} else {");
				outln(var.getDataIn() + " = " + var.getDataOut() + ";");
				outln("}");
				break;
			case Reg.REGR:
				/*
				 * if (RESET) dout = 0; else dout = din;
				 */
				outln("if (" + getRValue(reg.getInternalResetPort().getBus())
						+ ") {");
				outln("\t" + var.getDataIn() + " = 0;");
				outln("} else {");
				outln("\t" + var.getDataIn() + " = "
						+ getRValue(reg.getDataPort().getBus()) + ";");
				outln("}");
				break;
			case Reg.REGRE:
				/*
				 * if (RESET) dout = 0; else if (ENABLE) dout = din; else dout =
				 * dout;
				 */
				outln("if (" + getRValue(reg.getInternalResetPort().getBus())
						+ ") {");
				outln("\t" + var.getDataIn() + " = 0;");
				outln("} else if  (" + getRValue(reg.getEnablePort().getBus())
						+ ") {");
				outln(var.getDataIn() + " = "
						+ getRValue(reg.getDataPort().getBus()) + ";");
				outln("} else {");
				outln(var.getDataIn() + " = " + var.getDataOut() + ";");
				outln("}");
				break;
			case Reg.REGRS:
				/*
				 * if (RESET) dout = 0; else if (SET) dout = fffffff; else dout
				 * = din;
				 */
				outln("if (" + getRValue(reg.getInternalResetPort().getBus())
						+ ") {");
				outln("\t" + var.getDataIn() + " = 0;");
				outln("} else if (" + getRValue(reg.getSetPort().getBus())
						+ ") {");
				outln("\t" + var.getDataIn() + " = -1;");
				outln("} else {");
				outln("\t" + var.getDataIn() + " = "
						+ getRValue(reg.getDataPort().getBus()) + ";");
				outln("}");
				break;
			default:
				throw new UnexpectedStructureException("Unknown type for "
						+ reg + " " + type);
			}
		}

		@Override
		public void visit(EncodedMux m) {
			/*
			 * switch (m.select) { case 0 : out = port_0; break; case 1 : out =
			 * port_1; break; default : out = 0; break; }
			 */
			OpHandle handle = makeOpHandle(m);
			preDeclare(handle, m.getResultBus());
			outln(handle.declare(m.getResultBus()));
			outln("switch (" + getRValue(m.getSelectPort().getBus()) + ") {");
			int nPorts = m.getDataPorts().size();
			String dout = handle.getBusName(m.getResultBus());
			for (int i = 0; i < nPorts; i++) {
				outln("\tcase " + i + " : " + dout + " = "
						+ getRValue(m.getDataPort(i).getBus()) + "; break;");
			}
			final String dflt = "default";
			outln("\t" + dflt + " : " + dout + " = 0;");
			outln("}");
		}

		@Override
		public void visit(Mux m) {
			assert m.getGoPorts().size() == 2 : "Mux with "
					+ m.getGoPorts().size() + " ports";
			final OpHandle handle = makeOpHandle(m);
			Bus select = m.getGoPorts().get(0).getBus();
			Bus d1 = m.getDataPort(m.getGoPorts().get(0)).getBus();
			Bus d2 = m.getDataPort(m.getGoPorts().get(1)).getBus();
			preDeclare(handle, m.getResultBus());
			outln(handle.assign(m.getResultBus(), "(" + getRValue(select)
					+ ") ? " + getRValue(d1) + ":" + getRValue(d2)));
		}

		@Override
		public void visit(MemoryRead memRead) {
			// if GO, set memory address, enable, and wen, current
			// output is D/C. Set memory read state to pending
			// On next cycle, if read state is pending, set done and
			// data out
			/*
			 * result = ((type *)memory)[address_current/typeByteSize]; done =
			 * memRead.pending; if (go) { memRead.pending = 1;
			 * target.address_next = addr; target.en_next = 1; target.wen_next =
			 * 0; target.size_next = sizePort; } else { memRead.pending = 0; }
			 */
			MemAccessVar accHandle = (MemAccessVar) opHandles.get(memRead);
			preDeclare(accHandle, memRead.getResultBus());
			preDeclare(accHandle, memRead.getExit(Exit.DONE).getDoneBus());
			outln(accHandle.assign(memRead.getResultBus(),
					accHandle.getMemoryRead()));
			outln(accHandle.assign(memRead.getExit(Exit.DONE).getDoneBus(),
					accHandle.getPendingOut()));
			outln("if (" + getRValue(memRead.getGoPort().getBus()) + ") {");
			outln("\t" + accHandle.getPendingIn() + " = 1;");
			outln("\t" + accHandle.getMemStateVar() + ".addr_next = "
					+ getRValue(memRead.getAddressPort().getBus()) + ";");
			outln("\t" + accHandle.getMemStateVar() + ".en_next = 1;");
			outln("\t" + accHandle.getMemStateVar() + ".we_next = 0;");
			outln("\t" + accHandle.getMemStateVar() + ".size_next = "
					+ getRValue(memRead.getSizePort().getBus()) + ";");
			outln("} else {");
			outln("\t" + accHandle.getPendingIn() + " = 0;");
			outln("}");
			// Fix the fact that the containing module will be getting
			// value from the physical output. Do this by defining
			// the name for the physical output
			OpHandle physHandle = makeOpHandle(memRead.getPhysicalComponent());
			physHandle.overrideName(
					memRead.getPhysicalComponent().getExit(Exit.DONE)
							.getDataBuses().get(0),
					accHandle.getBusName(memRead.getResultBus()));
		}

		@Override
		public void visit(MemoryWrite memWrite) {
			/*
			 * done = memwrite.pending; if (go) { memwrite.pending = 1;
			 * target.address_next = addr; target.data_next = data;
			 * target.en_next = 1; target.wen_next = 1; target.size_next =
			 * sizePort; } else { memwrite.pending = 0; }
			 */
			MemAccessVar accHandle = (MemAccessVar) opHandles.get(memWrite);
			outln(accHandle.assign(memWrite.getExit(Exit.DONE).getDoneBus(),
					accHandle.getPendingOut()));
			outln("if (" + getRValue(memWrite.getGoPort().getBus()) + ") {");
			outln("\t" + accHandle.getPendingIn() + " = 1;");
			outln("\t" + accHandle.getMemStateVar() + ".addr_next = "
					+ getRValue(memWrite.getAddressPort().getBus()) + ";");
			outln("\t" + accHandle.getMemStateVar() + ".data_next = "
					+ getRValue(memWrite.getDataPort().getBus()) + ";");
			outln("\t" + accHandle.getMemStateVar() + ".en_next = 1;");
			outln("\t" + accHandle.getMemStateVar() + ".we_next = 1;");
			outln("\t" + accHandle.getMemStateVar() + ".size_next = "
					+ getRValue(memWrite.getSizePort().getBus()) + ";");
			outln("} else {");
			outln("\t" + accHandle.getPendingIn() + " = 0;");
			outln("}");
		}

		@Override
		public void visit(RegisterRead regRead) {
			// Tap off the register.
			Register target = (Register) regRead.getReferenceable();
			assert target != null : "Null target reference " + regRead;
			RegisterVar var = (RegisterVar) referenceableMap.get(target);
			OpHandle handle = makeOpHandle(regRead);
			preDeclare(handle, regRead.getResultBus());
			outln(handle.assign(regRead.getResultBus(), var.getDataOut()));
		}

		@Override
		public void visit(RegisterWrite regWrite) {
			// Set the state of the enable port and the data in.
			// Since we do the 'wired OR' approach to sending data,
			// mask the data with the enable locally.
			/*
			 * if (enable) { next = in; pending_next = 1; } else { pending_next
			 * = 0; }
			 */
			final Register target = (Register) regWrite.getReferenceable();
			assert target != null : "Null target reference " + regWrite;
			final RegisterVar var = (RegisterVar) referenceableMap.get(target);
			final RegWriteVar handle = (RegWriteVar) opHandles.get(regWrite);
			assert regWrite.getGoPort().getBus() != null : "No control for "
					+ regWrite;
			outln("if (" + getRValue(regWrite.getGoPort().getBus()) + ") {");
			outln("\t" + var.getDataIn() + " = "
					+ getRValue(regWrite.getDataPort().getBus()) + ";");
			outln("\t" + var.getEnable() + " = 1;");
			outln("\t" + handle.getPendingIn() + " = 1;");
			outln("} else {");
			outln("\t" + handle.getPendingIn() + " = 0;");
			outln("}");
		}

		@Override
		public void visit(OrOp or) {
			if (or instanceof OrOpMulti) {
				// System.out.println("SKIPPING OR OP MULTI");
				/*
				 * The OrOpMulti is only created in gateway objects and in
				 * merging pin accesses. All these situations are not reflected
				 * directly in the C simulation model (we emulate at the LIM
				 * access, not the routed wires). Thus we can safely ignore the
				 * OrOpMulti
				 */
				return;
			}
			// Catch the fact that we may use or ops as feedback
			// points and thus may have already created the handle
			// if (!this.opHandles.containsKey(or))
			// {
			// makeOpHandle(or);
			// }
			// OpHandle orHandle = (OpHandle)this.opHandles.get(or);
			processOr(or);
		}

		private void processOr(Component or) {
			// The Or can have 1-N inputs
			OpHandle handle = makeOpHandle(or);
			String rvalue = "";
			for (Iterator<Port> iter = or.getDataPorts().iterator(); iter
					.hasNext();) {
				Port port = iter.next();
				Bus source = port.getBus();
				rvalue += getRValue(source);
				if (iter.hasNext())
					rvalue += " | ";
			}
			Bus result = or.getExit(Exit.DONE).getDataBuses().get(0);
			preDeclare(handle, result);
			outln(handle.assign(result, rvalue));
		}

		@Override
		public void visit(And a) {
			if (a.getDataPorts().size() == 2) {
				writeBinaryOp(a, "&");
			} else {
				OpHandle handle = makeOpHandle(a);
				String rvalue = "";
				for (Iterator<Port> iter = a.getDataPorts().iterator(); iter
						.hasNext();) {
					Bus source = iter.next().getBus();
					rvalue += getRValue(source);
					if (iter.hasNext())
						rvalue += " & ";
				}
				Bus result = a.getExit(Exit.DONE).getDataBuses().get(0);
				preDeclare(handle, result);
				outln(handle.assign(result, rvalue));
			}
		}

		@Override
		public void visit(ReductionOrOp reducedOr) {
			// A reduction or op is a bitwise oring of all
			// bits... thus a one bit value comes out if any bit is
			// set... ie: (input != 0);
			OpHandle handle = makeOpHandle(reducedOr);
			preDeclare(handle, reducedOr.getResultBus());
			outln(handle.assign(reducedOr.getResultBus(), getRValue(reducedOr
					.getDataPort().getBus()) + " != 0"));
		}

		@Override
		public void visit(SRL16 srl16) {
			// next = data in
			// enable = rotate
			SRL16Var handle = (SRL16Var) opHandles.get(srl16);
			outln(handle.getDataIn() + " = "
					+ getRValue(srl16.getInDataPort().getBus()) + ";");
			if (srl16.getEnablePort().isUsed()) {
				outln(handle.getEnable() + " = "
						+ getRValue(srl16.getEnablePort().getBus()) + ";");
			}
		}

		@Override
		public void visit(SimplePin pin) {
			// Unhandled pins include CLK and RESET which need to be
			// declared. Both CLK and RESET can simply be set to 0
			// (they are handled via the clockEdge function and the
			// kicker circuitry. ALL other 'external' pins should be
			// handled by the IOHandler.
			if (pin instanceof SimpleInternalPin || !ioHandle.isHandled(pin)) {
				OpHandle handle = makeOpHandle(pin);
				preDeclare(handle, pin.getXLatData().getSource());
			}
		}

		@Override
		public void visit(SimplePinRead comp) {
			OpHandle handle = makeOpHandle(comp);
			String rhs;
			if (ioHandle.isHandled((SimplePin) comp.getReferenceable())) {
				// String member =
				// CycleCTranslator.this.ioHandle.getAccessString(comp);
				// Handle External pins via the IOHandler
				rhs = ioHandle.getAccessString(comp);
			} else {
				// Handle Internal pins.
				SimplePin pin = (SimplePin) comp.getReferenceable();
				rhs = opHandles.get(pin).getBusName(
						pin.getXLatData().getSource());
			}

			preDeclare(handle, comp.getResultBus());
			outln(handle.assign(comp.getResultBus(), rhs));
		}

		/**
		 * The simple pin write masks the data that it sends to the pin with the
		 * enable signal so that we can do a simple wired or of all the writes.
		 */
		@Override
		public void visit(SimplePinWrite comp) {
			makeOpHandle(comp);
			String dataValue = getRValue(comp.getDataPort().getBus());
			String enable = getRValue(comp.getGoPort().getBus());
			if (ioHandle.isHandled((SimplePin) comp.getReferenceable())) {
				String member = ioHandle.getAccessString(comp);
				// Instead of having a unique write value for each pin
				// write, assume that the initial value of the pin is 0
				// and do a bitwise OR.
				// out |= (enable) ? rvalue:0;
				outln(member + " |= (" + enable + ") ? " + dataValue + ":0;");
			} else {
				// Handle Internal pins.
				SimplePin pin = (SimplePin) comp.getReferenceable();
				String pinVar = opHandles.get(pin).getBusName(
						pin.getXLatData().getSource());
				outln("if (" + enable + ") { " + pinVar + " = " + dataValue
						+ "; }");
				// outln(pinVar + " = " + dataValue + ";");
			}
		}

		@Override
		public void visit(AddOp add) {
			writeBinaryOp(add, "+");
		}

		@Override
		public void visit(AndOp andOp) {
			writeBinaryOp(andOp, "&");
		}

		@Override
		public void visit(CastOp cast) {
			writeUnaryOp(cast, "");
		}

		@Override
		public void visit(ComplementOp comp) {
			writeUnaryOp(comp, "~");
		}

		@Override
		public void visit(ConditionalAndOp cand) {
			writeBinaryOp(cand, "&&");
		}

		@Override
		public void visit(ConditionalOrOp cor) {
			writeBinaryOp(cor, "||");
		}

		@Override
		public void visit(DivideOp divide) {
			writeBinaryOp(divide, "/");
		}

		@Override
		public void visit(EqualsOp equals) {
			writeBinaryOp(equals, "==");
		}

		@Override
		public void visit(GreaterThanEqualToOp gte) {
			writeBinaryOp(gte, ">=");
		}

		@Override
		public void visit(GreaterThanOp gt) {
			writeBinaryOp(gt, ">");
		}

		@Override
		public void visit(LeftShiftOp leftShift) {
			writeBinaryOp(leftShift, "<<");
		}

		@Override
		public void visit(LessThanEqualToOp lte) {
			writeBinaryOp(lte, "<=");
		}

		@Override
		public void visit(LessThanOp lt) {
			writeBinaryOp(lt, "<");
		}

		@Override
		public void visit(MinusOp minus) {
			writeUnaryOp(minus, "-");
		}

		@Override
		public void visit(ModuloOp modulo) {
			writeBinaryOp(modulo, "%");
		}

		@Override
		public void visit(MultiplyOp multiply) {
			writeBinaryOp(multiply, "*");
		}

		@Override
		public void visit(NotEqualsOp notEquals) {
			writeBinaryOp(notEquals, "!=");
		}

		@Override
		public void visit(NotOp not) {
			writeUnaryOp(not, "!");
		}

		@Override
		public void visit(PlusOp plus) {
			writeUnaryOp(plus, "+");
		}

		@Override
		public void visit(SubtractOp subtract) {
			writeBinaryOp(subtract, "-");
		}

		@Override
		public void visit(XorOp xor) {
			writeBinaryOp(xor, "^");
		}

		@Override
		public void visit(Not n) {
			writeUnaryOp(n, "!");
		}

		// public void visit (Or o) { writeBinaryOp(o, "|"); }
		@Override
		public void visit(Or o) {
			processOr(o);
		}

		@Override
		public void visit(RightShiftOp rightShift) {
			// NOTE!!! FIXME!!! The right shift in C is non-portable.
			// The behavior of what bits are shifted in from the left,
			// if the input value is signed and negative, depends on
			// the implementation of the compiler. I verified that we
			// get a 'correct' signed right shift on linux (debian, gcc 3.3.5),
			// solaris (gcc 3.2.1) and windows (minsys gcc 3.4.2)
			writeBinaryOp(rightShift, ">>");
		}

		@Override
		public void visit(RightShiftUnsignedOp rightShiftUnsigned) {
			// writeBinaryOp(rightShiftUnsigned, ">>");
			assert rightShiftUnsigned.getDataPorts().size() == 2;
			OpHandle handle = makeOpHandle(rightShiftUnsigned);

			Bus bus1 = rightShiftUnsigned.getDataPorts().get(0).getBus();
			Bus bus2 = rightShiftUnsigned.getDataPorts().get(1).getBus();
			Bus result = rightShiftUnsigned.getExit(Exit.DONE).getDataBuses()
					.get(0);

			// Cast the left operand to an unsigned type
			String type1 = OpHandle.getTypeDeclaration(bus1.getValue()
					.getSize(), false);
			preDeclare(handle, result);
			outln(handle.assign(result, "((" + type1 + ")" + getRValue(bus1)
					+ ") >> " + getRValue(bus2)));
		}

		@Override
		public void visit(Block block) {
			makeOpHandle(block);
			super.visit(block);
		}

		@Override
		public void visit(Loop loop) {
			makeOpHandle(loop);
			super.visit(loop);
		}

		@Override
		public void visit(WhileBody whileBody) {
			makeOpHandle(whileBody);
			super.visit(whileBody);
		}

		@Override
		public void visit(UntilBody untilBody) {
			makeOpHandle(untilBody);
			super.visit(untilBody);
		}

		@Override
		public void visit(ForBody forBody) {
			makeOpHandle(forBody);
			super.visit(forBody);
		}

		@Override
		public void visit(Branch branch) {
			makeOpHandle(branch);
			super.visit(branch);
		}

		@Override
		public void visit(Decision decision) {
			makeOpHandle(decision);
			super.visit(decision);
		}

		@Override
		public void visit(Switch sw) {
			makeOpHandle(sw);
			super.visit(sw);
		}

		@Override
		public void visit(Scoreboard scoreboard) {
			// A stallboard will be a feedback point in a block, thus
			// we may have already created an OpHandle for it.
			if (!opHandles.containsKey(scoreboard)) {
				makeOpHandle(scoreboard);
			}
			super.visit(scoreboard);
		}

		@Override
		public void visit(Latch latch) {
			makeOpHandle(latch);
			super.visit(latch);
		}

		@Override
		public void visit(HeapRead heapRead) {
			makeOpHandle(heapRead);
			super.visit(heapRead);
		}

		@Override
		public void visit(ArrayRead arrayRead) {
			makeOpHandle(arrayRead);
			super.visit(arrayRead);
		}

		@Override
		public void visit(HeapWrite heapWrite) {
			makeOpHandle(heapWrite);
			super.visit(heapWrite);
		}

		@Override
		public void visit(ArrayWrite arrayWrite) {
			makeOpHandle(arrayWrite);
			super.visit(arrayWrite);
		}

		@Override
		public void visit(AbsoluteMemoryRead absRead) {
			makeOpHandle(absRead);
			super.visit(absRead);
		}

		@Override
		public void visit(AbsoluteMemoryWrite absWrite) {
			makeOpHandle(absWrite);
			super.visit(absWrite);
		}

		@Override
		public void visit(Kicker kicker) {
			makeOpHandle(kicker);
			super.visit(kicker);
		}

		@Override
		public void visit(FifoRead comp) {
			makeOpHandle(comp);
			super.visit(comp);
		}

		@Override
		public void visit(FifoWrite comp) {
			makeOpHandle(comp);
			super.visit(comp);
		}

		@Override
		public void visit(FifoAccess comp) {
			makeOpHandle(comp);
			super.visit(comp);
		}

		@Override
		public void visit(SimplePinAccess comp) {
			makeOpHandle(comp);
			super.visit(comp);
		}

		@Override
		public void visit(TaskCall comp) {
			makeOpHandle(comp);
			super.visit(comp);
		}

		/*
		 * These are all components that we do not need to do anything with.
		 * Note, constant prop will have already pushed constants to all target
		 * ports.
		 */
		// public void visit (Procedure procedure){}
		// public void visit (Constant constant){}
		// public void visit (LocationConstant loc){}
		/*
		 * Skip the gateway and referee objects, they are only related to
		 * sideband data movement which we ignore since our memory read and
		 * memory write objects access memory directly.
		 */
		@Override
		public void visit(RegisterGateway regGateway) {
		}

		@Override
		public void visit(MemoryReferee memReferee) {
		}

		@Override
		public void visit(MemoryGateway memGateway) {
		}

		@Override
		public void visit(RegisterReferee vis) {
		}

		@Override
		public void visit(PinReferee vis) {
		}

		@Override
		public void visit(EndianSwapper vis) {
		}

		/*
		 * These we expect to not traverse
		 */
		@Override
		public void visit(MemoryBank vis) {
			throw new UnexpectedTraversalException(vis.toString());
		}

		@Override
		public void visit(NoOp vis) {
			throw new UnexpectedTraversalException(vis.toString());
		} // THEY SHOULD ALL BE GONE BY NOW

		/*
		 * These are expected to be obsolete
		 */
		@Override
		public void visit(IPCoreCall vis) {
			throw new ObsoleteTraversalException(vis.toString());
		}

		@Override
		public void visit(TimingOp vis) {
			throw new ObsoleteTraversalException(vis.toString());
		}

		@Override
		public void visit(PinRead vis) {
			throw new ObsoleteTraversalException(vis.toString());
		}

		@Override
		public void visit(PinWrite vis) {
			throw new ObsoleteTraversalException(vis.toString());
		}

		@Override
		public void visit(PinStateChange vis) {
			throw new ObsoleteTraversalException(vis.toString());
		}

		@Override
		public void visit(TriBuf vis) {
			throw new ObsoleteTraversalException(vis.toString());
		}

		@Override
		public void visit(ShortcutIfElseOp vis) {
			throw new ObsoleteTraversalException(vis.toString());
		}

		@Override
		public void visit(NumericPromotionOp vis) {
			throw new ObsoleteTraversalException(vis.toString());
		}

		@Override
		public void visit(PriorityMux vis) {
			throw new ObsoleteTraversalException(vis.toString());
		}

		private void preDeclare(OpHandle handle, Bus b) {
			if (alternateDeclarationPS != null) {
				alternateDeclarationPS.println(handle.declare(b));
			}
		}

		private OpHandle makeOpHandle(Component comp) {
			// assert !this.opHandles.containsKey(comp) :
			// "duplicate op handle for " + comp + " of " + comp.getOwner();
			if (opHandles.containsKey(comp)) {
				// System.out.println("WARNING: Repeated call to makeOpHandle for component "
				// + comp + " of " + comp.showOwners());
				return opHandles.get(comp);
			}

			OpHandle handle = new OpHandle(comp, nameCache);
			opHandles.put(comp, handle);

			return handle;
		}

		private void writeUnaryOp(Component op, String operation) {
			OpHandle handle = makeOpHandle(op);

			Bus bus1 = op.getDataPorts().get(0).getBus();
			Bus result = op.getExit(Exit.DONE).getDataBuses().get(0);
			preDeclare(handle, result);
			outln(handle.assign(result, operation + getRValue(bus1)));
		}

		private void writeBinaryOp(Component op, String operation) {
			assert op.getDataPorts().size() == 2 : "Not 2 ports on " + op + " "
					+ op.getDataPorts().size();

			OpHandle handle = makeOpHandle(op);

			Bus bus1 = op.getDataPorts().get(0).getBus();
			Bus bus2 = op.getDataPorts().get(1).getBus();
			Bus result = op.getExit(Exit.DONE).getDataBuses().get(0);
			preDeclare(handle, result);
			outln(handle.assign(result, getRValue(bus1) + " " + operation + " "
					+ getRValue(bus2)));
		}

		private String getRValue(Bus bus) {
			if (bus.getValue().isConstant() && !bus.getValue().isDontCare()) {
				// If the constant value is small (from -126 to 126)
				// then simply use a decimal constant literal.
				// Otheriwse, use an unsigned hex (long long) form
				// that is cast to the right type. This avoids
				// problems with decimal constant literals at the 32
				// bit boundry.
				long valueMask = bus.getValue().getValueMask();
				String cValue;
				if (valueMask > -127 && valueMask < 127) {
					cValue = Long.toString(valueMask);
				} else {
					final String type = OpHandle.getTypeDeclaration(
							bus.getSize(), bus.getValue().isSigned());
					cValue = "((" + type + ")0x" + Long.toHexString(valueMask)
							+ "ULL)";
				}

				return cValue;
			} else if (bus.getValue().isDontCare()) {
				return "0";
			} else {
				Component owner = bus.getOwner().getOwner();
				assert opHandles.containsKey(owner) : "Unknown driver " + owner;
				assert opHandles.containsKey(owner);
				OpHandle handle = opHandles.get(owner);
				return handle.getBusName(bus);
			}
		}

		private void outln(String s) {
			ps.println(s);
		}

	}

	//
	// Exceptions used by this class
	//

	public static class CycleCTranslatorException extends RuntimeException {
		private static final long serialVersionUID = -5542531110540842047L;

		public CycleCTranslatorException(String msg) {
			super(msg);
		}
	}

	@SuppressWarnings("unused")
	private static class UnsupportedCompileTypeException extends
			CycleCTranslatorException {
		private static final long serialVersionUID = 9173443035775914442L;

		public UnsupportedCompileTypeException(String msg) {
			super(msg);
		}
	}

	private static class FileHandlingException extends
			CycleCTranslatorException {
		private static final long serialVersionUID = 6002577187474834056L;

		public FileHandlingException(String msg) {
			super(msg);
		}
	}

	private static class UnexpectedStructureException extends
			CycleCTranslatorException {
		private static final long serialVersionUID = -4054912413950885741L;

		public UnexpectedStructureException(String msg) {
			super(msg);
		}
	}

	private static class UnexpectedTraversalException extends
			CycleCTranslatorException {
		private static final long serialVersionUID = 3244085632475618560L;

		public UnexpectedTraversalException(String msg) {
			super(msg);
		}
	}

	private static class ObsoleteTraversalException extends
			CycleCTranslatorException {
		private static final long serialVersionUID = 6836860953349806325L;

		public ObsoleteTraversalException(String msg) {
			super(msg);
		}
	}

}// CycleCTranslator
