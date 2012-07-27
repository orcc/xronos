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

/*
 * File modified by Endri Bezati
 * Adding the XmlResourcePrinter
 */

package net.sf.openforge.app;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sf.openforge.backend.OutputEngine;
import net.sf.openforge.backend.edk.ForgeCoreDescriptor;
import net.sf.openforge.backend.hdl.TestBenchEngine;
import net.sf.openforge.backend.hdl.VerilogTranslateEngine;
import net.sf.openforge.backend.sysgen.SysgenSimApi;
import net.sf.openforge.backend.timedc.CycleCTranslateEngine;
import net.sf.openforge.forge.api.internal.Core;
import net.sf.openforge.forge.api.runtime.RunTime;
import net.sf.openforge.forge.api.sim.pin.PinSimData;
import net.sf.openforge.lim.Call;
import net.sf.openforge.lim.CodeLabel;
import net.sf.openforge.lim.Design;
import net.sf.openforge.lim.IPCoreCall;
import net.sf.openforge.lim.Latency;
import net.sf.openforge.lim.LimDRC;
import net.sf.openforge.lim.Pin;
import net.sf.openforge.lim.Task;
import net.sf.openforge.optimize.Optimizer;
import net.sf.openforge.report.DesignResource;
import net.sf.openforge.report.HardwareResourceUtilizer;
import net.sf.openforge.report.InterfaceReporter;
import net.sf.openforge.report.ResourcePrinter;
import net.sf.openforge.report.SimpleResourceReporter;
import net.sf.openforge.report.XmlResourcePrinter;
import net.sf.openforge.report.throughput.ThroughputAnalyzer;
import net.sf.openforge.schedule.Scheduler;
import net.sf.openforge.verilog.testbench.CycleSimTestBench;
import net.sf.openforge.verilog.testbench.GenericTestbenchWriter;
import net.sf.openforge.verilog.translate.PassThroughComponentRemover;

/**
 * LIMCompiler is responsible for handling optimization, scheduling and
 * translation of a JCompilationUnit into a JDesign and LIM Design based on the
 * preferences
 * 
 * @author CSchanck
 * @version $Id: LIMCompiler.java 424 2007-02-26 22:36:09Z imiller $
 */
public class LIMCompiler {

	private File reportDirectory;

	public LIMCompiler() {
	}

	/**
	 * Compiles/Translates/etc a LIM design
	 * 
	 * @param design
	 *            a value of type 'Design'
	 */
	public Design processLim(Design design) {
		// Option op;
		GenericJob gj = EngineThread.getGenericJob();
		gj.getOption(OptionRegistry.PE_NAME).setValue(CodeLabel.UNSCOPED,
				design.showIDLogical());

		final List<OutputEngine> outputEngines = generateOutputEngines();
		final OutputEngine cycleCEngine;
		if (gj.getUnscopedBooleanOptionValue(OptionRegistry.WRITE_CYCLE_C)) {
			cycleCEngine = new CycleCTranslateEngine();
		} else {
			cycleCEngine = null;
		}

		if (cycleCEngine != null)
			cycleCEngine.initEnvironment();
		for (OutputEngine engine : outputEngines) {
			engine.initEnvironment();
		}

		if (!gj.getUnscopedBooleanOptionValue(OptionRegistry.NO_EDK))
			this.reportDirectory = gj.getFileHandler().getFile(
					ForgeCoreDescriptor.EDK_REPORT_DIR);
		else
			this.reportDirectory = gj.getFileHandler().registerFile(
					new ForgeFileKey("report dir"), "report");

		Engine.breathe();

		/*
		 * Optimize.
		 */
		Optimizer optimizer = new Optimizer();
		design = (Design) optimizer.optimize(design);

		Engine.breathe();

		/*
		 * Schedule.
		 */
		design = Scheduler.schedule(design);

		Engine.breathe();

		//
		// Calculate the design go spacing. Annotates each Task.
		//
		final ThroughputAnalyzer throughputAnalyzer = new ThroughputAnalyzer();
		design.accept(throughputAnalyzer);

		Engine.breathe();

		/*
		 * FIXME: CWU Do we need to normalize size of value on each data port? I
		 * asumed that the size of port will never change once it has been
		 * sized.
		 */

		Engine.breathe();

		// DRC
		LimDRC ldrc = new LimDRC();
		design.accept(ldrc);
		ldrc.dumpFailures();

		Engine.breathe();

		/*
		 * Apply naming to all LIM components. Must Come after LimDRC because it
		 * relies on a well-connected LIM graph.
		 */
		net.sf.openforge.lim.naming.LIMLogicalNamer.setNames(design, false);

		Engine.breathe();

		try {
			if (cycleCEngine != null)
				cycleCEngine.translate(design);
		} catch (IOException ioe) {
			gj.fatalError("Error generating files during "
					+ cycleCEngine.getOutputPhaseId() + "\n" + ioe);
		}

		Engine.breathe();

		if (gj.getUnscopedBooleanOptionValue(OptionRegistry.SHOULD_SIMULATE)) {
			simulate(design);
			Engine.breathe();
		}

		// CWU - Removes the pass through components to avoid
		// generating unnecessary wire declarations and wire
		// assignment statements.
		// Moved here from the translate method so that reporting
		// correctly ignores these pass through.
		design.accept(new PassThroughComponentRemover());

		if (gj.getUnscopedBooleanOptionValue(OptionRegistry.REPORT)) {
			report(design, throughputAnalyzer);
			Engine.breathe();
		}

		if (!Core.isEmpty()) {
			IPCoreInstantiator ip = new IPCoreInstantiator(design);
			ip.makeIPCore();
		}

		for (OutputEngine engine : outputEngines) {
			try {
				engine.translate(design);
			} catch (IOException ioe) {
				gj.fatalError("Error generating files during "
						+ engine.getOutputPhaseId());
			}
		}

		reportDesignCharacteristics(design);

		if (gj.getUnscopedBooleanOptionValue(OptionRegistry.ENABLE_XFLOW)) {
			xflow(design);
			Engine.breathe();
		}
		
		//LXGraph.graphTo(design, "/tmp/" +design.showIDGlobal()+"_graph.dot");
		GenericTestbenchWriter test = new GenericTestbenchWriter(design);
		test.genTestbench();
		clearAPIRuntime();

		return design;
	}

	private List<OutputEngine> generateOutputEngines() {
		List<OutputEngine> engines = new ArrayList<OutputEngine>();
		// Option op;
		GenericJob gj = EngineThread.getGenericJob();

		// Cycle C handled seperately because of where it has to run
		// if (gj.getUnscopedBooleanOptionValue(OptionRegistry.WRITE_CYCLE_C))
		// engines.add(new CycleCTranslateEngine());

		if (!gj.getUnscopedBooleanOptionValue(OptionRegistry.NO_EDK)) // DO EDK
		{
			// The ForgeCoreDescriptor engine MUST run before the
			// verilog translate engine in order to set up the
			// destinations correctly
			engines.add(new ForgeCoreDescriptor());
		}

		if (!gj.getUnscopedBooleanOptionValue(OptionRegistry.SHOULD_NOT_TRANSLATE)) // SHOULD
																					// translate..
		{
			engines.add(new VerilogTranslateEngine());
		}

		if (gj.getUnscopedBooleanOptionValue(OptionRegistry.AUTO_TEST_BENCH)
				|| PinSimData.exists()) {
			// deprecated
			// if (PinSimData.exists())
			// engines.add(CycleSimTestBench);
			// else
			if (!gj.getUnscopedBooleanOptionValue(OptionRegistry.NO_BLOCK_IO))// if
																				// Do
																				// BlockIO
			{
				engines.add(new TestBenchEngine.BlockIOTestBenchEngine());
			} else if (ForgeFileTyper.isXLIMSource(gj.getTargetFiles()[0]
					.getName())) {
				engines.add(new TestBenchEngine.GenericTBEngine());
			} else {
				engines.add(new TestBenchEngine.SimpleTestBenchEngine());
			}
		}

		if (gj.getUnscopedBooleanOptionValue(OptionRegistry.SYSGEN)) {
			engines.add(new SysgenSimApi());
		}

		if (gj.getOption(OptionRegistry.UCF_FILE).getValue(CodeLabel.UNSCOPED)
				.toString() != null
				&& !gj.getOption(OptionRegistry.UCF_FILE)
						.getValue(CodeLabel.UNSCOPED).toString().equals("")) {
			engines.add(new DesignUCFDocument());
		}

		return engines;
	}

	private void clearAPIRuntime() {
		// clear out the api RunTime class in case this is a multi
		// file compilation
		try {
			Class<RunTime> runtime = net.sf.openforge.forge.api.runtime.RunTime.class;

			// init all fields
			// FIXME
			@SuppressWarnings("rawtypes")
			Class[] args = { java.lang.Integer.TYPE, java.io.File.class,
					java.io.File.class, java.io.File.class };

			Method method = runtime.getDeclaredMethod("setValues", args);

			// allow ourselves to call the private method
			method.setAccessible(true);

			Object[] params = new Object[4];

			params[0] = new Integer(0);
			params[1] = null;
			params[2] = null;
			params[3] = null;

			method.invoke(null, params);
		} catch (Throwable t) {
			// Fail silently. We no longer use/maintain the API
			// classes. Message removed 03/24/2005 IDM.
			// EngineThread.getGenericJob().error("internal problem configuring RunTime api class: "
			// + t.getMessage());
		}
	}

	/**
	 * Reports the user the latency and control characteristics of each top
	 * level module.
	 */
	private void reportDesignCharacteristics(Design design) {
		(new InterfaceReporter()).reportStreams();
		GenericJob gj = EngineThread.getGenericJob();
		for (Task task : design.getTasks()) {
			final Call topCall = task.getCall();
			if (topCall instanceof IPCoreCall)
				continue;
			final Latency latency = topCall.getLatency();
			// final StringBuffer stringBuffer = new StringBuffer();

			gj.inc();
			gj.info("entry module \"" + topCall.showIDLogical() + "\":");
			gj.inc();
			gj.info("max gate depth = " + task.getMaxGateDepth());
			gj.info("min latency = " + getClocksString(latency.getMinClocks()));
			gj.info("max latency = " + getClocksString(latency.getMaxClocks()));
			gj.info((topCall.consumesGo() ? "requires " : "does not require ")
					+ "go input");
			gj.info((topCall.producesDone() ? "produces " : "does not produce ")
					+ "done output");
			// Job.info("module is " + (!topCall.isBalanceable() ||
			// !latency.isFixed() ? "not " : "") + "balanced");
			String spacing = "";
			int space = task.getGoSpacing();
			if (space == Task.INDETERMINATE_GO_SPACING)
				spacing = "indeterminate";
			else {
				spacing = Integer.toString(space) + " clock";
				if (space != 1)
					spacing += "s";
			}
			gj.info("min go spacing is " + spacing);
			gj.dec();
			gj.dec();
		}

	}

	/**
	 * Gets a descriptive string for a number of clocks. {@link Latency#UNKNOWN}
	 * is reported as "*".
	 */
	private static String getClocksString(int clocks) {
		if (clocks == Latency.UNKNOWN) {
			return "unknown";
		}

		StringBuffer buf = new StringBuffer();
		buf.append(clocks);
		buf.append(" clock");
		if (clocks != 1) {
			buf.append("s");
		}
		return buf.toString();
	}

	/**
	 * <code>simulate</code> performs a cycle accurate simulation of the LIM
	 * data structure.
	 * 
	 * @see Design
	 * 
	 * @param design
	 *            a <code>Design</code> value
	 */
	private void simulate(Design design) {
		EngineThread
				.getGenericJob()
				.warn("Forge data model simulation not available.  -sim command line argument is deprecated.");
		/*
		 * Simulator simulator = new Simulator(design); simulator.simulate();
		 */
	} // simulate()

	/**
	 * Generates reports for this design.
	 * 
	 * @param design
	 *            a {@link Design}
	 */

	private void report(Design design, ThroughputAnalyzer throughputAnalyzer) {
		GenericJob gj = EngineThread.getGenericJob();
		gj.info("generating Resource Utilization Report... ");
		// ResourceReporter resReporter = new ResourceReporter();
		SimpleResourceReporter resReporter = new SimpleResourceReporter();
		resReporter.visit(design);

		/*
		 * create a sub forge report directory, then save the report file(s).
		 */
		if (!this.reportDirectory.exists()) {
			this.reportDirectory.mkdirs();
		}

		final File resourceReportFile = new File(this.reportDirectory,
				design.showIDLogical() + "_resource.rpt");
		gj.info("writing " + resourceReportFile.getAbsolutePath());
		gj.inc();
		final FileOutputStream reportFos = openFile(resourceReportFile);
		/** print resource report to file */
		final ResourcePrinter resPrinter = new ResourcePrinter(reportFos);
		// resPrinter.print(resReporter.getResource());
		resPrinter.print((DesignResource) resReporter.getTopResource());
		closeFile(resourceReportFile, reportFos);
		gj.dec();

		final File throughputReportFile = new File(this.reportDirectory,
				design.showIDLogical() + "_throughput.rpt");
		gj.info("writing " + throughputReportFile.getAbsolutePath());
		gj.inc();
		final FileOutputStream throughputFos = openFile(throughputReportFile);
		throughputAnalyzer.writeReport(new PrintStream(throughputFos, true));
		closeFile(throughputReportFile, throughputFos);
		gj.dec();

		// output FPGA resource utilization report
		final File resourceUtilizationReportFile = new File(
				this.reportDirectory, design.showIDLogical()
						+ "_ResourceUtilizationReport.html");
		gj.info("writing " + resourceUtilizationReportFile.getAbsolutePath());
		gj.inc();
		FileOutputStream resourceUtilizationFos = openFile(resourceUtilizationReportFile);
		@SuppressWarnings("unused")
		HardwareResourceUtilizer resourceUtilizer = new HardwareResourceUtilizer(
				design, resourceUtilizationFos);
		closeFile(resourceUtilizationReportFile, resourceUtilizationFos);
		gj.dec();

		// output XML resource report
		final File reportXmlFile = new File(this.reportDirectory,
				design.showIDLogical() + ".xml");
		gj.info("writing " + reportXmlFile.getAbsolutePath());
		gj.inc();
		FileOutputStream reportXmlFos = openFile(reportXmlFile);
		@SuppressWarnings("unused")
		XmlResourcePrinter xmlReport = new XmlResourcePrinter(design,
				reportXmlFos);
		// closeFile(reportXmlFile, reportXmlFos);
		gj.dec();

		// Report on how we distributed the memories of the design
		// 08.26.2004 purging PointerValue left nothing to do in the
		// memory report as it was.
		// File memReportFile = new File(this.reportDirectory,
		// "memoryMap.html");
		// Job.info("writing " + memReportFile.getAbsolutePath());
		// Job.inc();
		// FileOutputStream memReportFos = openFile(memReportFile);
		// design.accept(new MemDispositionReporter(new
		// PrintStream(memReportFos, true), true));
		// closeFile(memReportFile, memReportFos);
		// Job.dec();
	}

	private void xflow(Design design) {
		GenericJob gj = EngineThread.getGenericJob();
		gj.info("Running xflow ...");
		gj.inc();

		// Create the xflow scripting class and request
		// the output go input a directory called
		// xflow_base_file to avoid module builder type
		// builds from overruning the various xflow files
		// that are not uniquely named!
		File synthFile;
		if (gj.getFileHandler().isRegistered(VerilogTranslateEngine.SYNINCL)) {
			synthFile = gj.getFileHandler().getFile(
					VerilogTranslateEngine.SYNINCL);
		} else {
			synthFile = gj.getFileHandler().getFile(
					VerilogTranslateEngine.VERILOG);
		}
		String subFileName = (synthFile.getName()).substring(0,
				((synthFile.getName()).lastIndexOf("_synth.v")));
		File workingDir = new File(synthFile.getParent(),
				(subFileName + "_xflow"));

		if (!workingDir.exists())
			workingDir.mkdirs();

		Xflow xf = new Xflow(design, synthFile, workingDir);

		if (xf.isEnvironmentValid()) {
			Map<String, Object> pin_map = new HashMap<String, Object>();
			for (Object pin : design.getPins()) {
				pin_map.put(((Pin) pin).showIDLogical(), pin);
			}

			xf.setPinMap(pin_map);

			// We want to create the script, but bitgen shouldn't be
			// run (i.e. config(false)).
			xf.setRunScript(true);
			xf.setRunConfig(false);
			gj.info("> " + xf.getCommandLine());
			gj.info("");
			xf.runScript();

			gj.info("");
			gj.info("> renamed xflow.{scr,bat} to xflow_full.{scr,bat}");
			gj.info("");

			xf.setRunScript(false);
			gj.info("> " + xf.getCommandLine());
			gj.info("");

			// Run xflow
			xf.run();

			if (!xf.runOK()) {
				throw new XflowException(
						"xflow completed with non 0 exit value, can't continue");
			}
		}
		gj.dec();
		gj.info("DONE");
		gj.info("");
	}

	private FileOutputStream openFile(File file) {
		FileOutputStream fos = null;
		try {
			fos = new FileOutputStream(file);
		} catch (Exception e) {
			EngineThread.getEngine().fatalError(
					"could not create output file " + file.getAbsolutePath());
		}
		return fos;
	}

	private void closeFile(File file, FileOutputStream fos) {
		String nextFile = null;
		try {
			nextFile = file.getAbsolutePath();
			fos.flush();
			fos.close();
		} catch (Exception e) {
			EngineThread.getGenericJob().warn(
					"could not close output file " + nextFile);
		}
	}

}
