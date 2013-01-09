package org.xronos.orcc.backend;

import static net.sf.orcc.OrccLaunchConstants.DEBUG_MODE;
import static net.sf.orcc.OrccLaunchConstants.MAPPING;
import static net.sf.orcc.OrccLaunchConstants.NO_LIBRARY_EXPORT;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sf.orcc.backends.AbstractBackend;
import net.sf.orcc.df.Actor;
import net.sf.orcc.df.Instance;
import net.sf.orcc.df.Network;
import net.sf.orcc.df.transform.Instantiator;
import net.sf.orcc.df.transform.NetworkFlattener;
import net.sf.orcc.graph.Vertex;
import net.sf.orcc.util.OrccLogger;

import org.eclipse.core.resources.IFile;
import org.xronos.orcc.backend.transform.XronosTransform;
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

	/** A list which contains the given xronosFlags **/
	private List<String> xronosFlags;

	/** The used Xilinx FPGA Name **/
	private String fpgaName;

	/** Generate Verilog files with Go And Done signal on Top Module **/
	private boolean generateGoDone;

	private boolean generateWeights;

	/** Use Orcc as a fronted for OpenForge, No XLIM code generation **/

	/** The path used for the RTL Go Done generation **/
	private String rtlGoDonePath;

	/** The path used for the RTL generation **/
	private String rtlPath;

	/** The path used for the simulation generation **/
	private String simPath;

	/** The path used for the testBench generation **/
	private String testBenchPath;

	private boolean xilinxPrimitives;

	@Override
	protected void doInitializeOptions() {
		clkDomains = getAttribute(MAPPING, new HashMap<String, String>());
		debugMode = getAttribute(DEBUG_MODE, true);
		generateGoDone = getAttribute("net.sf.orc2hdl.generateGoDone", false);
		generateWeights = getAttribute("net.sf.orc2hdl.generateWeights", false);
		xilinxPrimitives = getAttribute("net.sf.orc2hdl.xilinxPrimitives",
				false);

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

		// Set FPGA name and forge flags
		fpgaName = "xc2vp30-7-ff1152";

		// Set Forge Flags
		xronosFlags = new ArrayList<String>();
		xronosFlags.add("-vv");
		xronosFlags.add("-pipeline");
		xronosFlags.add("-noblockio");
		xronosFlags.add("-no_block_sched");
		xronosFlags.add("-simple_arbitration");
		xronosFlags.add("-noedk");
		xronosFlags.add("-loopbal");
		// xronosFlags.add("-unroll");
		xronosFlags.add("-multdecomplimit");
		xronosFlags.add("2");
		xronosFlags.add("-comb_lut_mem_read");
		xronosFlags.add("-dplut");
		xronosFlags.add("-nolog");
		xronosFlags.add("-noinclude");
		xronosFlags.add("-report");
		xronosFlags.add("-Xdetailed_report");
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
		new Instantiator(false, 1).doSwitch(network);
		new NetworkFlattener().doSwitch(network);

		// Compute the Network Template
		network.computeTemplateMaps();

		// Print Network
		printNetwork(network);

		// Print Testbenches
		printTestbenches(network);

		// Print Instances
		generateInstances(network);

		Runtime r = Runtime.getRuntime();
		r.gc();
	}

	@Override
	public boolean exportRuntimeLibrary() {
		boolean exportLibrary = !getAttribute(NO_LIBRARY_EXPORT, false);

		String libPath = path + File.separator + "lib";

		if (exportLibrary) {
			copyFileToFilesystem("/bundle/README.txt", path + File.separator
					+ "README.txt");

			OrccLogger.trace("Export libraries sources into " + libPath
					+ "... ");
			if (copyFolderToFileSystem("/bundle/lib", libPath)) {
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
		int numCached = 0;

		long t0 = System.currentTimeMillis();
		List<Actor> visitedActrors = new ArrayList<Actor>();
		for (Vertex vertex : network.getChildren()) {
			final Instance instance = vertex.getAdapter(Instance.class);
			if (instance != null) {
				if (!instance.getActor().isNative()) {
					ResourceCache resourceCache = new ResourceCache();
					Actor actor = instance.getActor();
					if (!visitedActrors.contains(actor)) {
						XronosTransform.transformActor(actor, resourceCache,
								debugMode);
						visitedActrors.add(actor);
					}
					XronosPrinter printer = new XronosPrinter(!debugMode);
					printer.getOptions().put("generateGoDone", generateGoDone);
					printer.getOptions().put("fpgaType", fpgaName);
					List<String> flags = new ArrayList<String>(xronosFlags);
					flags.addAll(Arrays.asList("-d", rtlPath, "-o",
							instance.getSimpleName()));
					Boolean cached = printer.printInstance(
							flags.toArray(new String[0]), rtlPath, instance,
							resourceCache);
					if (cached) {
						numCached++;
					}
				}
			}
		}
		long t1 = System.currentTimeMillis();
		OrccLogger.traceln("Done in " + (float) (t1 - t0) / (float) 1000 + "s");
		if (numCached > 0) {
			OrccLogger
					.traceln("*******************************************************************************");
			OrccLogger.traceln("* NOTE: " + numCached
					+ " instances were not regenerated "
					+ "because they were not modyified *");
			OrccLogger
					.traceln("*******************************************************************************");
		}
	}

	private void printNetwork(Network network) {
		OrccLogger.traceln("Generating Network...");

		XronosPrinter xronosPrinter = new XronosPrinter();
		xronosPrinter.getOptions().put("clkDomains", clkDomains);
		xronosPrinter.printNetwork(rtlPath, network);

		if (generateGoDone) {
			xronosPrinter.getOptions().put("generateGoDone", generateGoDone);
			xronosPrinter.printNetwork(rtlGoDonePath, network);
		}

	}

	private void printTestbenches(Network network) {
		OrccLogger.traceln("Generating Testbenches...");

		// Create the fifoTraces folder
		String tracePath = testBenchPath + File.separator + "fifoTraces";
		File fifoTracesDir = new File(tracePath);
		if (!fifoTracesDir.exists()) {
			fifoTracesDir.mkdir();
		}

		// Create the VHD directory on the testbench folder
		String tbVhdPath = testBenchPath + File.separator + "vhd";
		File tbVhdDir = new File(tbVhdPath);
		if (!tbVhdDir.exists()) {
			tbVhdDir.mkdir();
		}

		// Create the Xronos Printer
		XronosPrinter xronosPrinter = new XronosPrinter();
		xronosPrinter.getOptions().put("xilinxPrimitives", xilinxPrimitives);

		// Print the network TCL ModelSim simulation script
		xronosPrinter.printSimTclScript(simPath, false, network);
		if (generateGoDone) {
			xronosPrinter.getOptions().put("generateGoDone", generateGoDone);
			xronosPrinter.getOptions().put("generateWeights", generateWeights);
			// Create the weights path
			File weightsPath = new File(simPath + File.separator + "weights");
			if (!weightsPath.exists()) {
				weightsPath.mkdir();
			}
			xronosPrinter.printWeightTclScript(simPath, network);
			xronosPrinter.printSimTclScript(simPath, true, network);
		}

		// print the network VHDL Testbech sourcefile
		xronosPrinter.printTestbench(tbVhdPath, network);

		// Print the network testbench TCL ModelSim simulation script
		xronosPrinter.printTclScript(testBenchPath, true, network);

		for (Vertex vertex : network.getChildren()) {
			final Instance instance = vertex.getAdapter(Instance.class);
			if (instance != null) {
				xronosPrinter.printTestbench(tbVhdPath, instance);
				xronosPrinter.printTclScript(testBenchPath, true, instance);
			}
		}
	}

}
