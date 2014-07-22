/* 
 * XRONOS, High Level Synthesis of Streaming Applications
 * 
 * Copyright (C) 2014 EPFL SCI STI MM
 *
 * This file is part of XRONOS.
 *
 * XRONOS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * XRONOS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with XRONOS.  If not, see <http://www.gnu.org/licenses/>.
 * 
 * Additional permission under GNU GPL version 3 section 7
 * 
 * If you modify this Program, or any covered work, by linking or combining it
 * with Eclipse (or a modified version of Eclipse or an Eclipse plugin or 
 * an Eclipse library), containing parts covered by the terms of the 
 * Eclipse Public License (EPL), the licensors of this Program grant you 
 * additional permission to convey the resulting work.  Corresponding Source 
 * for a non-source form of such a combination shall include the source code 
 * for the parts of Eclipse libraries used as well as that of the  covered work.
 * 
 */

package org.xronos.orcc.backend;

import static net.sf.orcc.OrccLaunchConstants.DEBUG_MODE;
import static net.sf.orcc.OrccLaunchConstants.MAPPING;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sf.orcc.backends.AbstractBackend;
import net.sf.orcc.backends.util.Validator;
import net.sf.orcc.df.Actor;
import net.sf.orcc.df.Network;
import net.sf.orcc.df.transform.Instantiator;
import net.sf.orcc.df.transform.NetworkFlattener;
import net.sf.orcc.df.transform.TypeResizer;
import net.sf.orcc.graph.Vertex;
import net.sf.orcc.util.FilesManager;
import net.sf.orcc.util.OrccLogger;
import net.sf.orcc.util.Result;

import org.xronos.orcc.analysis.XronosDynamicWeights;
import org.xronos.orcc.analysis.XronosStaticWeight;
import org.xronos.orcc.backend.transform.NetworkBufferSizeImporter;
import org.xronos.orcc.design.ResourceCache;

/**
 * The Xronos Orcc Front-End.
 * 
 * @author Endri Bezati
 * 
 */

public class Xronos extends AbstractBackend {

	/** The clock Domains Map **/
	private Map<String, String> clkDomains;

	/** Debug Mode, no caching, generating always **/
	private boolean debugMode;

	/** The used Xilinx FPGA Name **/
	private String fpgaName;

	/** Import buffer size file **/
	private boolean importBufferSize;

	/** Generate Verilog files with Go And Done signal on Top Module **/
	private boolean generateGoDone;

	private boolean generateWeights;

	/** The path used for the RTL Go Done generation **/
	private String rtlGoDonePath;

	/** The path used for the RTL generation **/
	private String rtlPath;

	/** The path used for the simulation generation **/
	private String simPath;

	/** One verilog contains all the design **/
	private boolean singleFileGeneration;

	/** The path used for the testBench generation **/
	private String testBenchPath;

	/** The path where the fifo traces should be placed **/
	private String tracePath;

	/** The path where the VHDL tesbenches are placed */
	private String tbVhdPath;

	/** Copy the Xilinx RAM/registers primitives **/
	private boolean xilinxPrimitives;

	private boolean outputClockGating;

	private boolean inputClockGating;

	boolean schedulerInformation;

	boolean newLimGen;

	@Override
	protected void doInitializeOptions() {
		clkDomains = getOption(MAPPING, new HashMap<String, String>());
		debugMode = getOption(DEBUG_MODE, true);
		generateGoDone = getOption("org.xronos.orcc.generateGoDone", false);
		generateWeights = getOption("org.xronos.orcc.generateWeights", false);
		xilinxPrimitives = getOption("org.xronos.orcc.xilinxPrimitives",
				false);
		singleFileGeneration = getOption(
				"org.xronos.orcc.singleFileGeneration", false);
		importBufferSize = getOption("org.xronos.orcc.importBufferSize",
				false);
		fifoSize = getOption("net.sf.orcc.fifoSize", 1);
		outputClockGating = getOption("org.xronos.orcc.outputClockGating",
				false);
		inputClockGating = getOption("org.xronos.orcc.inputClockGating",
				false);
		schedulerInformation = getOption(
				"org.xronos.orcc.schedulingInformation", false);
		newLimGen = getOption("org.xronos.orcc.newLimGen", false);

		// Set Paths for RTL
		rtlPath = path + File.separator + "rtl";
		File rtlDir = new File(rtlPath);
		if (!rtlDir.exists()) {
			rtlDir.mkdir();
		}

		if (generateGoDone) {
			rtlGoDonePath = rtlPath + File.separator + "rtlGoDone";
			File rtlGoDoneDir = new File(rtlGoDonePath);
			if (!rtlGoDoneDir.exists()) {
				rtlGoDoneDir.mkdir();
			}
		}

		// Set Paths for simulation
		simPath = path + File.separator + "sim";
		File simDir = new File(simPath);
		if (!simDir.exists()) {
			simDir.mkdir();
		}

		// Set Paths for testBenches
		testBenchPath = path + File.separator + "testbench";
		File testBenchDir = new File(testBenchPath);
		if (!testBenchDir.exists()) {
			testBenchDir.mkdir();
		}

		// Create the fifoTraces folder
		tracePath = testBenchPath + File.separator + "fifoTraces";
		File fifoTracesDir = new File(tracePath);
		if (!fifoTracesDir.exists()) {
			fifoTracesDir.mkdir();
		}

		// Create the VHD directory on the testbench folder
		tbVhdPath = testBenchPath + File.separator + "vhd";
		File tbVhdDir = new File(tbVhdPath);
		if (!tbVhdDir.exists()) {
			tbVhdDir.mkdir();
		}

		// Set FPGA name and forge flags
		fpgaName = "xc2vp30-7-ff1152";
	}

	@Override
	protected void doTransformActor(Actor actor) {
		// Do not transform at this moment
	}

	@Override
	protected void doXdfCodeGeneration(Network network) {
		Validator.checkMinimalFifoSize(network, fifoSize);
		// instantiate and flattens network
		new Instantiator(true).doSwitch(network);
		new NetworkFlattener().doSwitch(network);

		if (importBufferSize) {
			String bufferSzeFile = getOption(
					"org.xronos.orcc.bufferSizeFile", "");
			new NetworkBufferSizeImporter(bufferSzeFile).doSwitch(network);
		}

		new TypeResizer(false, true, false, false).doSwitch(network);
		// Compute the Network Template
		network.computeTemplateMaps();

		if (singleFileGeneration) {
			// Generate Network Design
			generateNetwork(network);
		} else {
			// Print Instances
			generateInstances(network);

			// Print Network
			printNetwork(network);
		}
		// Print Testbenches
		printTestbenches(network);

		// Weight Static Analysis
		XronosStaticWeight staticWeight = new XronosStaticWeight("weights_"
				+ network.getSimpleName(), rtlPath + File.separator + "report");
		staticWeight.createStaticWeight();

		if (generateWeights) {
			XronosDynamicWeights xronosDynamicWeights = new XronosDynamicWeights(
					network, testBenchPath);
			xronosDynamicWeights.getMeanWeights(rtlPath + File.separator
					+ "report");
		}

	}

	@Override
	protected Result doLibrariesExtraction() {
		Result result = FilesManager.extract("/bundle/README.txt", path);
		String libPath = path + File.separator + "lib";
		OrccLogger.traceln("Export libraries sources into " + libPath + "... ");
		result.merge(FilesManager.extract("/bundle/lib", path));
		return result;
	}

	public void generateInstances(Network network) {
		OrccLogger.traceln("Generating Instances...");
		OrccLogger
				.traceln("-------------------------------------------------------------------------------");
		long t0 = System.currentTimeMillis();

		List<Actor> instanceToBeCompiled = new ArrayList<Actor>();

		int cachedInstances = 0;
		// Figure out how many instances need to be compiled/Recompiled
		for (Vertex vertex : network.getChildren()) {
			final Actor actor = vertex.getAdapter(Actor.class);
			// //// TEST
			// StateVarAnalysisWriter analysisWriter = new
			// StateVarAnalysisWriter();
			// analysisWriter.print(actor, rtlPath);// + File.separator +
			// "report");
			// //// END TEST
			if (actor != null) {
				if (!actor.isNative()) {
					if (!debugMode) {
						long sourceLastModified = XronosPrinter
								.getLastModifiedHierarchy(actor);
						String file = rtlPath + File.separator
								+ actor.getSimpleName() + ".v";
						File targetFile = new File(file);
						long targetLastModified = targetFile.lastModified();
						if (sourceLastModified > targetLastModified) {
							if (!actor.hasAttribute("no_generation")) {
								instanceToBeCompiled.add(actor);
							} else {
								OrccLogger
										.warnln("Instance: "
												+ actor.getSimpleName()
												+ " contains @no_generation tag, it will not be generated!");
							}
						} else {
							cachedInstances++;
						}
					} else {
						if (!actor.hasAttribute("no_generation")) {
							instanceToBeCompiled.add(actor);
						} else {
							OrccLogger
									.warnln("Actor: "
											+ actor.getSimpleName()
											+ " contains @no_generation tag, it will not be generated!");
						}
					}

				}

			}
		}

		int toBeCompiled = instanceToBeCompiled.size();

		if (cachedInstances > 0) {
			OrccLogger.traceln("NOTE: Cached instances: " + cachedInstances);
		}
		if (toBeCompiled > 0) {
			OrccLogger.traceln("NOTE: Actors to be generated: " + toBeCompiled);
		}
		OrccLogger
				.traceln("-------------------------------------------------------------------------------");

		int numInstance = 1;
		int failedToCompile = 0;
		for (Actor actor : instanceToBeCompiled) {
			ResourceCache resourceCache = new ResourceCache();
			XronosPrinter printer = new XronosPrinter(!debugMode);
			printer.getOptions().put("generateGoDone", generateGoDone);
			printer.getOptions().put("xilinxPrimitives", xilinxPrimitives);
			printer.getOptions().put("fpgaType", fpgaName);
			XronosFlags flags = new XronosFlags(rtlPath, actor.getSimpleName());
			if (actor.hasAttribute("xronos_pipeline")) {
				if (actor.getAttribute("xronos_pipeline").hasAttribute("gd")) {
					Integer gateDepth = Integer.parseInt(actor
							.getAttribute("xronos_pipeline").getAttribute("gd")
							.getStringValue());
					flags.activatePipelining(gateDepth);
				} else {
					OrccLogger
							.warnln("PIPELINING: gd attribute missing, example: @xronos_pipeline(gd=\"100\")");
				}
			}

			boolean failed = printer.printInstance(flags.getStringFlag(),
					rtlPath, testBenchPath, tbVhdPath, actor, getOptions(),
					resourceCache, numInstance, toBeCompiled,
					schedulerInformation, newLimGen, debugMode);
			if (failed) {
				failedToCompile++;
			}
			numInstance++;
		}

		if (failedToCompile > 0) {
			OrccLogger
					.severeln("-------------------------------------------------------------------------------");
			OrccLogger.severeln("NOTE: " + failedToCompile + " actor"
					+ (failedToCompile > 1 ? "s" : "") + " failed to compile");
			OrccLogger
					.severeln("-------------------------------------------------------------------------------");
		}

		OrccLogger
				.traceln("*******************************************************************************");
		long t1 = System.currentTimeMillis();
		OrccLogger.traceln("Xronos done in " + (float) (t1 - t0) / (float) 1000
				+ "s");
	}

	private void generateNetwork(Network network) {
		OrccLogger.traceln("Generating Network...");
		OrccLogger
				.traceln("-------------------------------------------------------------------------------");

		ResourceCache resourceCache = new ResourceCache();
		XronosPrinter printer = new XronosPrinter(!debugMode);
		printer.getOptions().put("generateGoDone", generateGoDone);
		printer.getOptions().put("fpgaType", fpgaName);
		printer.getOptions().put("doubleBuffering", outputClockGating);
		printer.getOptions().put("inputClockGating", inputClockGating);
		XronosFlags flags = new XronosFlags(rtlPath, network.getSimpleName());
		boolean schedulerInformation = getOption(
				"org.xronos.orcc.schedulingInformation", false);
		boolean failed = printer.printNetwork(flags.getStringFlag(), rtlPath,
				network, getOptions(), resourceCache, schedulerInformation);

		if (failed) {
			OrccLogger
					.severeln("-------------------------------------------------------------------------------");
			OrccLogger.severeln("Network:" + network.getName()
					+ " failed to compile");
			OrccLogger
					.severeln("-------------------------------------------------------------------------------");
		}

	}

	private void printNetwork(Network network) {
		OrccLogger.traceln("Generating Network...");

		XronosPrinter xronosPrinter = new XronosPrinter();
		xronosPrinter.getOptions().put("clkDomains", clkDomains);
		xronosPrinter.getOptions().put("doubleBuffering", outputClockGating);
		xronosPrinter.getOptions().put("inputClockGating", inputClockGating);
		xronosPrinter.getOptions().put("fifoSize",fifoSize);
		xronosPrinter.printNetwork(rtlPath, network);
		if (generateGoDone) {
			xronosPrinter.getOptions().put("generateGoDone", generateGoDone);
			xronosPrinter.printNetwork(rtlGoDonePath, network);
		}
		// Print clock controllers if doublebuffering is enabled
		if (outputClockGating) {
			ClockEnabler clockEnabler = new ClockEnabler(rtlPath);
			clockEnabler.doSwitch(network);
		}

	}

	private void printTestbenches(Network network) {
		OrccLogger.traceln("Generating Testbenches...");

		// Create the Xronos Printer
		XronosPrinter xronosPrinter = new XronosPrinter();
		xronosPrinter.getOptions().put("xilinxPrimitives", xilinxPrimitives);
		xronosPrinter.getOptions().put("doubleBuffering", outputClockGating);
		xronosPrinter.getOptions().put("inputClockGating", inputClockGating);
		xronosPrinter.getOptions().put("schedulerInformation",
				schedulerInformation);

		// Print the network TCL ModelSim simulation script
		xronosPrinter.printSimTclScript(simPath, false, network);
		if (generateGoDone) {
			xronosPrinter.getOptions().put("generateGoDone", generateGoDone);
			xronosPrinter.getOptions().put("generateWeights", generateWeights);
			xronosPrinter.getOptions()
					.put("doubleBuffering", outputClockGating);
			xronosPrinter.getOptions()
					.put("inputClockGating", inputClockGating);
			xronosPrinter.getOptions().put("schedulerInformation",
					schedulerInformation);
			// Create the weights path
			File weightsPath = new File(testBenchPath + File.separator
					+ "weights");
			if (!weightsPath.exists()) {
				weightsPath.mkdir();
			}
			xronosPrinter.printWeightTclScript(testBenchPath, network);
			xronosPrinter.printSimTclScript(simPath, true, network);
		}

		// print the network VHDL Testbech sourcefile
		xronosPrinter.getOptions().put("clkDomains", clkDomains);
		xronosPrinter.printTestbench(tbVhdPath, network);

		// Print the network testbench TCL ModelSim simulation script
		xronosPrinter.printTclScript(testBenchPath, true, network);
	}

}
