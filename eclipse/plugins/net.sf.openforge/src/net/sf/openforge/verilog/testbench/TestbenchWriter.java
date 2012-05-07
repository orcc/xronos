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
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;

import net.sf.openforge.app.EngineThread;
import net.sf.openforge.app.ForgeFileHandler;
import net.sf.openforge.app.OptionRegistry;
import net.sf.openforge.backend.hdl.TestBenchEngine;
import net.sf.openforge.backend.hdl.VerilogTranslateEngine;
import net.sf.openforge.lim.Design;
import net.sf.openforge.lim.Task;
import net.sf.openforge.lim.Tester;
import net.sf.openforge.verilog.model.Comment;
import net.sf.openforge.verilog.model.Directive;
import net.sf.openforge.verilog.model.InitialBlock;
import net.sf.openforge.verilog.model.InlineComment;
import net.sf.openforge.verilog.model.Keyword;
import net.sf.openforge.verilog.model.Module;
import net.sf.openforge.verilog.model.VerilogDocument;
import net.sf.openforge.verilog.pattern.IncludeStatement;
import net.sf.openforge.verilog.translate.PrettyPrinter;

/**
 * TestbenchWriter is the second generation of the test bench writer (as of
 * version 1.33) and builds a Verilog testbench for a given design and
 * collection of TestVectors as supplied by the {@link Tester} attached to the
 * design. This testbench is a self-verifying testbench which performs the
 * following functions:
 * <ul>
 * <li>Generates data for each input argument of each task.
 * <li>Generates a unique GO signal for each task.
 * <li>Generates a DONE signal for any task with fixed timing and no DONE pin.
 * <li>Samples the DONE and RESULT outputs from each task.
 * <li>Compares the RESULT of each task to the expected value on assertion of
 * DONE.
 * <li>Counts the number of clock cycles between each assertion of GO to a task
 * and the associated DONE.
 * <li>Asserts GO (and next data set) to each task every 'n' cycles as specified
 * by the -atb_gs command line, or upon successful receipt of the previous DONE.
 * </ul>
 * <b>NOTE:</b> that all tasks share the same argument data memories. There are
 * enough memories to cover the task with the most number of arguments. Any task
 * with fewer arguments simply ignores the extra argument vectors. Ditto the
 * result memory (it's shared and if a method doesn't have a result it will be
 * ignored).
 * 
 * <p>
 * Created: Wed Jan 8 15:44:34 2003
 * 
 * @author imiller, last modified by $Author: imiller $
 * @version $Id: TestbenchWriter.java 490 2007-06-15 16:37:00Z imiller $
 */
public class TestbenchWriter {

	/**
	 * The verilog document being populated to generate the testbench.
	 */
	private VerilogDocument document;

	/** A unique Identifier for each test fixture. */
	private static int uniqueID = 0;

	public TestbenchWriter(Design design) {
		// Create a File to use for the RESULTS file
		/*
		 * String name = source.getName(); name = name.substring(0,
		 * name.length()-2); File resFile = new File(source.getParent(), name +
		 * ".results"); File clkFile = new File(source.getParent(), name
		 * +".results.clocks"); File benchmarkFile = new
		 * File(source.getParent(), name+".benchmarks.report");
		 */
		final ForgeFileHandler fileHandler = EngineThread.getGenericJob()
				.getFileHandler();
		final File resFile = fileHandler.getFile(TestBenchEngine.RESULTS);
		final File clkFile = fileHandler
				.getFile(TestBenchEngine.DefaultTestBenchEngine.CLKS);
		final File benchmarkFile = fileHandler
				.getFile(TestBenchEngine.DefaultTestBenchEngine.REPORT);
		final File source = fileHandler.getFile(VerilogTranslateEngine.SIMINCL);

		// A List of TestVector objects.
		List<TestVector> vectors = getVectors(design);

		if (vectors.size() == 0) {
			document = getDefaultDocument(design, resFile);
			return;
		}

		// Create the document and give it a header, timescale, etc.
		document = getInitializedDocument(source);

		Module testModule = new Module("fixture_" + uniqueID++);

		// Generate a TaskHandle for each task
		LinkedHashMap<Task, TaskHandle> taskHandles = new LinkedHashMap<Task, TaskHandle>();
		for (TestVector tv : vectors) {
			if (!taskHandles.containsKey(tv.getTask())) {
				TaskHandle th = new TaskHandle(design, tv.getTask());
				taskHandles.put(tv.getTask(), th);
			}
		}

		// Create the instantiation
		TestInstantiation instance = new TestInstantiation(design,
				new HashSet<TaskHandle>(taskHandles.values()));

		// Create all the memories
		Memories mems = new Memories(vectors, taskHandles);

		// Create the state machine (main controller of test)
		StateMachine machine = new StateMachine(new ArrayList<TaskHandle>(
				taskHandles.values()), vectors.size(), mems);

		// Create the hang timer
		// HangTimer timer = new HangTimer(new HashSet(taskHandles.values()));
		HangTimer timer = new HangTimer();

		// Create a handle to refer to the results file
		SimFileHandle resFileHandle = new SimFileHandle(resFile, "resultFile");

		// Create a cycle checker
		ClockChecker clockChecker = new ClockChecker(clkFile);

		// Create a benchmarker
		boolean atbGenerateBMReport = EngineThread.getGenericJob()
				.getUnscopedBooleanOptionValue(
						OptionRegistry.ATB_BENCHMARK_REPORT);

		BenchMarker benchMarker = null;
		if (atbGenerateBMReport) {
			benchMarker = new BenchMarker(benchmarkFile);
		}

		//
		// Initialize everything except the memories (theyre too long)
		//
		InitialBlock ib = new InitialBlock();
		resFileHandle.stateInits(ib);
		timer.stateInits(ib);
		for (TaskHandle taskHandle : taskHandles.values()) {
			taskHandle.stateInits(ib);
		}
		clockChecker.stateInits(ib);
		if (atbGenerateBMReport) {
			benchMarker.stateInits(ib);
		}

		// State machine must be last since it does #delays
		machine.stateInits(ib);
		testModule.state(ib);

		//
		// CHEAT!!! Add a commented out waves.vcd line
		//
		InlineComment comment = new InlineComment(
				"initial begin  $dumpfile(\"waves.vcd\");  $dumpvars;end",
				Comment.SHORT);
		testModule.state(comment);

		//
		// State the logic for each function of the testbench.
		//
		instance.stateLogic(testModule, machine);
		machine.stateLogic(testModule, mems, resFileHandle);
		timer.stateLogic(testModule, resFileHandle, machine);
		// Make connections for each argument and result of each task
		for (TaskHandle th : taskHandles.values()) {
			th.stateLogic(testModule, machine, mems, resFileHandle);
		}
		mems.stateLogic(); // does nothing.

		if (atbGenerateBMReport) {
			benchMarker.stateLogic(testModule, machine, design);
		}
		clockChecker.stateLogic(testModule, machine);

		//
		// Ok.. everything else is done, so go ahead and initialize
		// those long memories.
		//
		InitialBlock memsInit = new InitialBlock();
		mems.stateInits(memsInit);
		testModule.state(memsInit);

		//
		// Write all this stuff to the document.
		//
		document.append(testModule);
	}

	/**
	 * Write out the verilog test bench to the specified file.
	 */
	public void write(FileOutputStream testbenchFOS) {
		PrettyPrinter pp = new PrettyPrinter(testbenchFOS);
		pp.print(document);
	}

	/**
	 * Returns a List of {@link TestVector} objects in the order that they are
	 * generated based on the data supplied by the {@link Tester} supplied in
	 * the Design.
	 * 
	 * @param design
	 *            a value of type 'Design'
	 * @return a List of {@link TestVector} objects.
	 */
	private List<TestVector> getVectors(Design design) {
		Tester bbt = design.getTester();
		List<TestVector> vectors = new ArrayList<TestVector>();
		for (int i = 0; i < bbt.getVectorCount(); i++) {
			List<Object> argValues = Arrays.asList(bbt.getArgsVector(i));
			TestVector tv = new TestVector(bbt.getTaskVector(i), argValues,
					bbt.getResultVector(i), bbt.getResultValidVector(i));
			vectors.add(tv);
		}

		return vectors;
	}

	/**
	 * Creates a VerilogDocument and adds to it the headers we want.
	 */
	private static VerilogDocument getInitializedDocument(File source) {
		VerilogDocument vdoc = new VerilogDocument();

		// Adds a header:
		// // OpenForge Test
		// // Automatic Test Bench. Generated: Mon, 26 Aug 2002 09:21:56 -0400
		// //
		SimpleDateFormat df = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss Z");
		vdoc.append(new Comment("OpenForge Test", Comment.SHORT));
		vdoc.append(new Comment("Automatic Test Bench (MTBW).  Generated: "
				+ df.format(new Date()), Comment.SHORT));
		vdoc.append(new Comment(Comment.BLANK));

		// add the timescale directive to give a default to anyone who
		// doesn't have 1
		vdoc.append(new Directive.TimeScale(1, Keyword.NS, 1, Keyword.PS));
		vdoc.append(new Comment(Comment.BLANK));
		vdoc.append(new Comment(
				" Some simulators cannot handle the syntax of the new memory models.  This define uses a simpler syntax for the memory models in the unisims library"));
		vdoc.append(new Comment(Comment.BLANK));
		vdoc.append(new Directive.Define("legacy_model", ""));

		vdoc.append(new Comment(Comment.BLANK));

		vdoc.append(new IncludeStatement(source));

		// reset the timescale after the includes, just in case a
		// library element changed it to something we don't want
		vdoc.append(new Directive.TimeScale(1, Keyword.NS, 1, Keyword.PS));

		vdoc.append(new Comment(Comment.BLANK));

		return vdoc;
	}

	/**
	 * Creates a default testbench for designs with no vectors which states
	 * either: FAIL -or- Test Bench Generator created no vectors ...
	 * 
	 * @param design
	 *            a value of type 'Design'
	 * @param resFile
	 *            a value of type 'File'
	 * @return a value of type 'VerilogDocument'
	 */
	private VerilogDocument getDefaultDocument(Design design, File resFile) {
		boolean hasExternal = false;

		for (Task t : design.getTasks()) {
			hasExternal |= (!t.isAutomatic());
		}

		String message;
		if (!hasExternal) {
			message = "FAIL: NO FORGEABLE METHODS\\n";
		} else {
			message = "Test Bench Generator created no vectors for design with 1 or more entry methods";
		}

		return new DefaultSimDocument(message,
				Collections.singletonList(resFile));
	}

}// TestbenchWriter
