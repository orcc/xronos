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

package org.xronos.openforge.backend.hdl;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import org.xronos.openforge.app.EngineThread;
import org.xronos.openforge.app.ForgeFileHandler;
import org.xronos.openforge.app.ForgeFileKey;
import org.xronos.openforge.app.GenericJob;
import org.xronos.openforge.backend.OutputEngine;
import org.xronos.openforge.lim.Design;
import org.xronos.openforge.verilog.testbench.BlockIOTestBench;
import org.xronos.openforge.verilog.testbench.GenericTestbenchWriter;
import org.xronos.openforge.verilog.testbench.TestbenchWriter;


/**
 * The TestBenchEngine is the common superclass for all HDL automated testbench
 * styles. All testbenches create an _atb.v file and produce a results files. In
 * addition certain types of testbenches will generate additional data files
 * and/or consume particular input vector files. There are a number of static
 * classes contained within this class which implement the OutputEngine for
 * these testbenches.
 * 
 * <p>
 * Created: Mon Mar 20 12:39:18 2006
 * 
 * @author imiller, last modified by $Author: imiller $
 * @version $Id: TestBenchEngine.java 112 2006-03-21 15:41:57Z imiller $
 */
public abstract class TestBenchEngine implements OutputEngine {

	public static final ForgeFileKey ATB = new ForgeFileKey(
			"Verilog HDL auto testbench");
	public static final ForgeFileKey RESULTS = new ForgeFileKey(
			"Verilog HDL auto testbench results");
	protected File destDir;

	@Override
	public void initEnvironment() {
		ForgeFileHandler fileHandler = EngineThread.getGenericJob()
				.getFileHandler();
		this.destDir = fileHandler.getFile(VerilogTranslateEngine.VERILOG)
				.getParentFile();
		fileHandler.registerFile(ATB, destDir,
				fileHandler.buildName("_atb", "v"));
		fileHandler.registerFile(RESULTS, destDir,
				fileHandler.buildName("_sim", "results"));
	}

	@Override
	public abstract void translate(Design design) throws IOException;

	@Override
	public abstract String getOutputPhaseId();

	public abstract static class DefaultTestBenchEngine extends TestBenchEngine {
		public static final ForgeFileKey CLKS = new ForgeFileKey(
				"Verilog HDL auto testbench clock counts");
		public static final ForgeFileKey REPORT = new ForgeFileKey(
				"Verilog HDL auto testbench benchmark report");

		@Override
		public void initEnvironment() {
			super.initEnvironment();
			ForgeFileHandler fileHandler = EngineThread.getGenericJob()
					.getFileHandler();
			fileHandler.registerFile(CLKS, this.destDir,
					fileHandler.buildName("_sim.results", "clocks"));
			fileHandler.registerFile(REPORT, this.destDir,
					fileHandler.buildName("_sim.benchmarks", "report"));
		}
	}

	/**
	 * The testbench engine for Block IO (C compilation with FSL ports for I/O )
	 * based designs.
	 */
	public static class BlockIOTestBenchEngine extends DefaultTestBenchEngine {
		public static final ForgeFileKey CYCLES = new ForgeFileKey(
				"Verilog HDL auto testbench cycle results");
		public static final ForgeFileKey INVECS = new ForgeFileKey(
				"Verilog HDL auto testbench input vectors");
		public static final ForgeFileKey OUTVECS = new ForgeFileKey(
				"Verilog HDL auto testbench output vectors");
		public static final ForgeFileKey REFLECT = new ForgeFileKey(
				"Verilog HDL auto testbench C reflect file");

		@Override
		public void initEnvironment() {
			super.initEnvironment();
			ForgeFileHandler fileHandler = EngineThread.getGenericJob()
					.getFileHandler();
			fileHandler.registerFile(CYCLES, this.destDir,
					fileHandler.buildName("_sim.cycles", "results"));
			fileHandler.registerFile(INVECS, this.destDir,
					fileHandler.buildName("_input_blocks", "vec"));
			fileHandler.registerFile(OUTVECS, this.destDir,
					fileHandler.buildName("_output_expected", "vec"));
			fileHandler.registerFile(REFLECT, this.destDir,
					fileHandler.buildName("_reflect", "c"));
		}

		@Override
		public void translate(Design design) throws IOException {
			final GenericJob gj = EngineThread.getGenericJob();
			gj.info("Generating Block IO based Auto Test Bench ...");
			gj.inc();
			BlockIOTestBench biotb = new BlockIOTestBench(design);
			biotb.runTests();
			gj.dec();
		}

		/**
		 * Returns a string which uniquely identifies this phase of the compiler
		 * output.
		 * 
		 * @return a non-empty, non-null String
		 */
		@Override
		public String getOutputPhaseId() {
			return "Block IO based testbench";
		}

	}

	/**
	 * Testbench engine for simple (C based) designs which use the Go/done
	 * protocol and simple scalar I/O
	 */
	public static class SimpleTestBenchEngine extends DefaultTestBenchEngine {
		@Override
		public void translate(Design design) throws IOException {
			final GenericJob gj = EngineThread.getGenericJob();
			gj.info("Generating No Block IO Auto Test Bench ...");
			gj.inc();

			// Verify the design has a Tester
			if (design.getTester() != null) {
				// Do the work here, use vtbFos to send the output to
				// BlackBoxTester bbt = new BlackBoxTester(design);

				design.getTester().setDesign(design);

				// Run the tester
				design.getTester().runTests();

				// Write out the test bench
				TestbenchWriter tbw = new TestbenchWriter(design);

				FileOutputStream vtbFos = new FileOutputStream(gj
						.getFileHandler().getFile(ATB));

				tbw.write(vtbFos);

				vtbFos.flush();
				vtbFos.close();
			} else {
				gj.error("No Tester supplied for design, can't create test bench");
			}

			gj.dec();
		}

		/**
		 * Returns a string which uniquely identifies this phase of the compiler
		 * output.
		 * 
		 * @return a non-empty, non-null String
		 */
		@Override
		public String getOutputPhaseId() {
			return "Simple testbench";
		}

	}

	/**
	 * The testbench engine for 'generic' designs (xlim based), using .vec files
	 * for each input.
	 */
	public static class GenericTBEngine extends TestBenchEngine {
		public static final ForgeFileKey STATE = new ForgeFileKey(
				"Verilog HDL auto testbench benchmark report");

		@Override
		public void initEnvironment() {
			super.initEnvironment();
			ForgeFileHandler fileHandler = EngineThread.getGenericJob()
					.getFileHandler();
			fileHandler.registerFile(STATE, this.destDir,
					fileHandler.buildName("_sim", "stateDump"));
		}

		@Override
		public void translate(Design design) throws IOException {
			final GenericJob gj = EngineThread.getGenericJob();
			gj.info("Generating Generic Auto Test Bench ...");
			gj.inc();
			GenericTestbenchWriter gtbw = new GenericTestbenchWriter(design);
			gtbw.genTestbench();
			gj.dec();
		}

		/**
		 * Returns a string which uniquely identifies this phase of the compiler
		 * output.
		 * 
		 * @return a non-empty, non-null String
		 */
		@Override
		public String getOutputPhaseId() {
			return "Generic testbench";
		}

	}

}
