/*
 * Copyright (c) 2012, Ecole Polytechnique Fédérale de Lausanne
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 *   * Redistributions of source code must retain the above copyright notice,
 *     this list of conditions and the following disclaimer.
 *   * Redistributions in binary form must reproduce the above copyright notice,
 *     this list of conditions and the following disclaimer in the documentation
 *     and/or other materials provided with the distribution.
 *   * Neither the name of the Ecole Polytechnique Fédérale de Lausanne nor the names of its
 *     contributors may be used to endorse or promote products derived from this
 *     software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY
 * WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 */
package org.xronos.orcc.backend;

import static net.sf.orcc.OrccLaunchConstants.DEBUG_MODE;
import static net.sf.orcc.OrccLaunchConstants.MAPPING;
import static net.sf.orcc.OrccLaunchConstants.NO_LIBRARY_EXPORT;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sf.orcc.backends.AbstractBackend;
import net.sf.orcc.df.Actor;
import net.sf.orcc.df.Network;
import net.sf.orcc.df.transform.Instantiator;
import net.sf.orcc.df.transform.NetworkFlattener;
import net.sf.orcc.df.transform.TypeResizer;
import net.sf.orcc.graph.Vertex;
import net.sf.orcc.util.OrccLogger;

import org.eclipse.core.resources.IFile;
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

	private boolean doubleBuffering = false;

	@Override
	protected void doInitializeOptions() {
		clkDomains = getAttribute(MAPPING, new HashMap<String, String>());
		debugMode = getAttribute(DEBUG_MODE, true);
		generateGoDone = getAttribute("org.xronos.orcc.generateGoDone", false);
		generateWeights = getAttribute("org.xronos.orcc.generateWeights", false);
		xilinxPrimitives = getAttribute("org.xronos.orcc.xilinxPrimitives",
				false);
		singleFileGeneration = getAttribute(
				"org.xronos.orcc.singleFileGeneration", false);
		importBufferSize = getAttribute("org.xronos.orcc.importBufferSize",
				false);
		fifoSize = getAttribute("net.sf.orcc.fifoSize", 1);

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
	protected void doVtlCodeGeneration(List<IFile> files) {
		// do not generate VTL
	}

	@Override
	protected void doXdfCodeGeneration(Network network) {
		// instantiate and flattens network
		new Instantiator(true, fifoSize).doSwitch(network);
		new NetworkFlattener().doSwitch(network);

		if (importBufferSize) {
			String bufferSzeFile = getAttribute(
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
			// Print Network
			printNetwork(network);

			// Print Instances
			generateInstances(network);
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
	public boolean exportRuntimeLibrary() {
		boolean exportLibrary = !getAttribute(NO_LIBRARY_EXPORT, false);

		String libPath = path + File.separator + "lib";

		if (exportLibrary) {
			copyFileToFilesystem("/bundle/README.txt", path + File.separator
					+ "README.txt", debug);

			OrccLogger.trace("Export libraries sources into " + libPath
					+ "... ");
			if (copyFolderToFileSystem("/bundle/lib", libPath, debug)) {
				OrccLogger.traceRaw("OK" + "\n");
				return true;
			} else {
				OrccLogger.warnRaw("Error" + "\n");
				return false;
			}
		}
		return false;
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
					rtlPath, testBenchPath, tbVhdPath, actor, options,
					resourceCache, numInstance, toBeCompiled, debugMode);
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
		printer.getOptions().put("doubleBuffering", doubleBuffering);
		XronosFlags flags = new XronosFlags(rtlPath, network.getSimpleName());
		boolean failed = printer.printNetwork(flags.getStringFlag(), rtlPath,
				network, options, resourceCache);

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
		xronosPrinter.getOptions().put("doubleBuffering", doubleBuffering);
		xronosPrinter.printNetwork(rtlPath, network);
		if (generateGoDone) {
			xronosPrinter.getOptions().put("generateGoDone", generateGoDone);
			xronosPrinter.printNetwork(rtlGoDonePath, network);
		}
		// Print clock controllers if doublebuffering is enabled
		if (doubleBuffering) {
			ClockEnabler clockEnabler = new ClockEnabler(rtlPath);
			clockEnabler.doSwitch(network);
		}

	}

	private void printTestbenches(Network network) {
		OrccLogger.traceln("Generating Testbenches...");

		// Create the Xronos Printer
		XronosPrinter xronosPrinter = new XronosPrinter();
		xronosPrinter.getOptions().put("xilinxPrimitives", xilinxPrimitives);
		xronosPrinter.getOptions().put("doubleBuffering", doubleBuffering);

		// Print the network TCL ModelSim simulation script
		xronosPrinter.printSimTclScript(simPath, false, network);
		if (generateGoDone) {
			xronosPrinter.getOptions().put("generateGoDone", generateGoDone);
			xronosPrinter.getOptions().put("generateWeights", generateWeights);
			xronosPrinter.getOptions().put("doubleBuffering", doubleBuffering);
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
