/*
 * Copyright (c) 2011, Ecole Polytechnique Fédérale de Lausanne
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

package net.sf.orc2hdl.backend;

import static net.sf.orc2hdl.preference.Constants.P_MODELSIM;
import static net.sf.orcc.OrccLaunchConstants.DEBUG_MODE;
import static net.sf.orcc.OrccLaunchConstants.MAPPING;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sf.openforge.app.Engine;
import net.sf.openforge.app.Forge;
import net.sf.openforge.app.ForgeFatalException;
import net.sf.openforge.app.GenericJob;
import net.sf.openforge.app.NewJob;
import net.sf.openforge.app.OptionRegistry;
import net.sf.orc2hdl.Activator;
import net.sf.orc2hdl.analysis.ExecutionChart;
import net.sf.orc2hdl.analysis.SimParser;
import net.sf.orc2hdl.analysis.TimeGoDone;
import net.sf.orc2hdl.analysis.WeightWriter;
import net.sf.orc2hdl.backend.transform.DeadPhiRemover;
import net.sf.orc2hdl.backend.transform.IndexFlattener;
import net.sf.orc2hdl.backend.transform.RepeatPattern;
import net.sf.orc2hdl.design.DesignEngine;
import net.sf.orc2hdl.printer.Orc2HDLPrinter;
import net.sf.orcc.OrccException;
import net.sf.orcc.backends.AbstractBackend;
import net.sf.orcc.backends.StandardPrinter;
import net.sf.orcc.backends.transform.CastAdder;
import net.sf.orcc.backends.transform.DivisionSubstitution;
import net.sf.orcc.backends.transform.EmptyBlockRemover;
import net.sf.orcc.backends.transform.Inliner;
import net.sf.orcc.backends.transform.InstPhiTransformation;
import net.sf.orcc.backends.transform.Multi2MonoToken;
import net.sf.orcc.backends.transform.StoreOnceTransformation;
import net.sf.orcc.backends.xlim.XlimActorTemplateData;
import net.sf.orcc.backends.xlim.XlimExprPrinter;
import net.sf.orcc.backends.xlim.XlimTypePrinter;
import net.sf.orcc.backends.xlim.transform.CustomPeekAdder;
import net.sf.orcc.backends.xlim.transform.GlobalArrayInitializer;
import net.sf.orcc.backends.xlim.transform.InstTernaryAdder;
import net.sf.orcc.backends.xlim.transform.ListFlattener;
import net.sf.orcc.backends.xlim.transform.LiteralIntegersAdder;
import net.sf.orcc.backends.xlim.transform.LocalArrayRemoval;
import net.sf.orcc.backends.xlim.transform.UnaryListRemoval;
import net.sf.orcc.backends.xlim.transform.XlimDeadVariableRemoval;
import net.sf.orcc.backends.xlim.transform.XlimVariableRenamer;
import net.sf.orcc.df.Action;
import net.sf.orcc.df.Actor;
import net.sf.orcc.df.Instance;
import net.sf.orcc.df.Network;
import net.sf.orcc.df.transform.Instantiator;
import net.sf.orcc.df.transform.NetworkFlattener;
import net.sf.orcc.df.transform.UnitImporter;
import net.sf.orcc.df.util.DfSwitch;
import net.sf.orcc.df.util.DfVisitor;
import net.sf.orcc.ir.CfgNode;
import net.sf.orcc.ir.Expression;
import net.sf.orcc.ir.transform.BlockCombine;
import net.sf.orcc.ir.transform.ControlFlowAnalyzer;
import net.sf.orcc.ir.transform.DeadCodeElimination;
import net.sf.orcc.ir.transform.DeadGlobalElimination;
import net.sf.orcc.ir.transform.SSATransformation;
import net.sf.orcc.ir.transform.TacTransformation;
import net.sf.orcc.ir.util.IrUtil;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.filesystem.IFileSystem;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Platform;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;

/**
 * This class defines an XLIM and OpenForge based back-end
 * 
 * @author Endri Bezati
 * @author Herve Yviquel
 * 
 */
public class Orc2HDL extends AbstractBackend {

	private Map<String, String> clkDomains;

	private boolean debugMode;

	private List<String> forgeFlags;

	private String fpgaName;

	private boolean goDoneSignal;

	// private boolean haveSystemActors;

	private boolean instanceToDesign;

	private boolean modelsimAnalysis;

	private String simPath;

	private String simTime;

	private String srcGoDonePath;

	private String srcPath;

	private String tbPath;

	private String tracePath;

	@Override
	protected void doInitializeOptions() {
		clkDomains = getAttribute(MAPPING, new HashMap<String, String>());
		instanceToDesign = getAttribute("net.sf.orc2hdl.instanceDesign", false);
		goDoneSignal = getAttribute("net.sf.orc2hdl.goDoneSignal", false);
		modelsimAnalysis = getAttribute("net.sf.orc2hdl.modelSimAnalysis",
				false);
		simTime = getAttribute("net.sf.orc2hdl.simTime", "5000");

		// The source Path
		srcPath = path + File.separator + "src";
		new File(srcPath).mkdir();

		// The source Path with the Go and Done signal
		srcGoDonePath = path + File.separator + "srcGoDone";
		if (goDoneSignal) {
			new File(srcGoDonePath).mkdir();
		}

		// The simualtion path
		simPath = path + File.separator + "sim";
		new File(simPath).mkdir();

		// The Testbench Path and Folder
		tbPath = simPath + File.separator + "Testbench";
		File tbFolder = new File(tbPath);
		if (!tbFolder.exists()) {
			tbFolder.mkdir();
		}

		// Testbench VHDs and Traces path
		tracePath = tbPath + File.separator + "traces";
		new File(tracePath).mkdir();
		new File(tbPath + File.separator + "vhd").mkdir();

		if (modelsimAnalysis) {
			goDoneSignal = true;
		}

		String fpgaType = getAttribute("net.sf.orc2hdl.FpgaType", "Virtex 2");

		if (fpgaType.equals("Spartan 3")) {
			// fpgaName = "xc3s5000-5-fg1156";
			fpgaName = "xc3s200-4-tq144C";
		} else if (fpgaType.equals("Virtex 2")) {
			fpgaName = "xc2vp30-7-ff1152";
		} else if (fpgaType.equals("Virtex 4")) {
			fpgaName = "xc4vlx100-10-ff1513";
		}

		debugMode = getAttribute(DEBUG_MODE, true);

		// Populating ForgeFlags
		forgeFlags = new ArrayList<String>();

		if (getAttribute("net.sf.orc2hdl.Verbose", false)) {
			forgeFlags.add("-vv");
		}

		if (getAttribute("net.sf.orc2hdl.Pipelining", true)) {
			forgeFlags.add("-pipeline");
		}

		if (getAttribute("net.sf.orc2hdl.NoBlockIO", true)) {
			forgeFlags.add("-noblockio");
		}

		if (getAttribute("net.sf.orc2hdl.NoBlockBasedScheduling", true)) {
			forgeFlags.add("-no_block_sched");
		}

		if (getAttribute("net.sf.orc2hdl.SimpleSharedMemoryArbitration", true)) {
			forgeFlags.add("-simple_arbitration");
		}

		if (getAttribute("net.sf.orc2hdl.NoEDKGeneration", true)) {
			forgeFlags.add("-noedk");
		}

		if (getAttribute("net.sf.orc2hdl.BalanceLoopLatency", true)) {
			forgeFlags.add("-loopbal");
		}

		if (getAttribute("net.sf.orc2hdl.MultiplierDecomposition", true)) {
			forgeFlags.add("-multdecomplimit");
			forgeFlags.add("2");
		}

		if (getAttribute("net.sf.orc2hdl.CombinationallyLUTReads", true)) {
			forgeFlags.add("-comb_lut_mem_read");
		}

		if (getAttribute("net.sf.orc2hdl.AllowDualPortLUT", true)) {
			forgeFlags.add("-dplut");
		}

		if (getAttribute("net.sf.orc2hdl.NoLog", true)) {
			forgeFlags.add("-nolog");
		}

		if (getAttribute("net.sf.orc2hdl.NoInclude", true)) {
			forgeFlags.add("-noinclude");
		}

		if (getAttribute("net.sf.orc2hdl.NoInclude", true)) {
			forgeFlags.add("-report");
			forgeFlags.add("-Xdetailed_report");
		}

	}

	@Override
	protected void doTransformActor(Actor actor) throws OrccException {
		XlimActorTemplateData data = new XlimActorTemplateData();
		actor.setTemplateData(data);
		if (instanceToDesign) {
			List<DfSwitch<?>> transformations = new ArrayList<DfSwitch<?>>();
			// transformations.add(new DfVisitor<Void>(new
			// LocalVarInitializer()));
			transformations.add(new StoreOnceTransformation());
			transformations.add(new DfVisitor<Void>(new LocalArrayRemoval()));
			transformations.add(new UnitImporter());
			transformations.add(new UnaryListRemoval());
			transformations.add(new DfVisitor<Void>(new SSATransformation()));
			transformations.add(new RepeatPattern());
			transformations.add(new GlobalArrayInitializer(true));
			transformations.add(new DfVisitor<Void>(new Inliner(true, true)));
			transformations.add(new DfVisitor<Void>(new DeadCodeElimination()));
			transformations.add(new DfVisitor<Expression>(
					new LiteralIntegersAdder()));
			transformations.add(new DfVisitor<Void>(new IndexFlattener()));
			transformations.add(new DfVisitor<Expression>(
					new TacTransformation()));
			transformations.add(new DfVisitor<CfgNode>(
					new ControlFlowAnalyzer()));
			transformations.add(new DfVisitor<Expression>(
					new LiteralIntegersAdder()));
			transformations
					.add(new DfVisitor<Expression>(new CastAdder(false)));
			transformations.add(new DfVisitor<Void>(new DeadPhiRemover()));

			for (DfSwitch<?> transformation : transformations) {
				transformation.doSwitch(actor);
				ResourceSet set = new ResourceSetImpl();
				if (debugMode && !IrUtil.serializeActor(set, path, actor)) {
					System.out.println("oops " + transformation + " "
							+ actor.getName());
				}
			}

		} else {
			DfSwitch<?>[] transformations = { new StoreOnceTransformation(),
					new DfVisitor<Void>(new LocalArrayRemoval()),
					new Multi2MonoToken(), new DivisionSubstitution(),
					new UnitImporter(),
					new DfVisitor<Void>(new SSATransformation()),
					/* new TypeResizer(false, true, true, true), */
					new GlobalArrayInitializer(true),
					new DfVisitor<Void>(new Inliner(true, true)),
					new DfVisitor<Void>(new InstTernaryAdder()),
					new UnaryListRemoval(), new CustomPeekAdder(),
					new DeadGlobalElimination(),
					new DfVisitor<Void>(new DeadCodeElimination()),
					new DfVisitor<Void>(new XlimDeadVariableRemoval()),
					new DfVisitor<Void>(new ListFlattener()),
					new DfVisitor<Expression>(new TacTransformation()),
					new DfVisitor<CfgNode>(new ControlFlowAnalyzer()),
					new DfVisitor<Void>(new InstPhiTransformation()),
					new DfVisitor<Expression>(new LiteralIntegersAdder()),
					new DfVisitor<Expression>(new CastAdder(true)),
					new XlimVariableRenamer(),
					new DfVisitor<Void>(new EmptyBlockRemover()),
					new DfVisitor<Void>(new BlockCombine()) };

			for (DfSwitch<?> transformation : transformations) {
				transformation.doSwitch(actor);
				ResourceSet set = new ResourceSetImpl();
				if (debugMode && !IrUtil.serializeActor(set, path, actor)) {
					System.out.println("oops " + transformation + " "
							+ actor.getName());
				}
			}
		}

		data.computeTemplateMaps(actor);
	}

	@Override
	protected void doVtlCodeGeneration(List<IFile> files) throws OrccException {
		// do not generate an XLIM VTL
	}

	@Override
	protected void doXdfCodeGeneration(Network network) throws OrccException {
		// instantiate and flattens network
		new Instantiator(false, 1).doSwitch(network);
		new NetworkFlattener().doSwitch(network);

		transformActors(network.getAllActors());

		network.computeTemplateMaps();

		TopNetworkTemplateData data = new TopNetworkTemplateData();

		data.computeTemplateMaps(network, clkDomains);
		network.setTemplateData(data);

		printNetwork(network);
		// Create the sim directory copy the glbl.v file in it and then print
		// the "do file"

		printSimDoFile(network);

		// Print the xlim files
		printInstances(network);

		if (modelsimAnalysis) {
			String exe = Activator.getDefault().getPreference(P_MODELSIM, "");
			if (exe == null || exe.isEmpty()) {
				write("Warning: The path to ModelSim executable is not set!\n"
						+ "Go to Window > Preferences > Orc2HDL to edit them.\n");
			} else {
				// Run ModelSim

				runModelSim(exe, network);

				// Parse Files
				SimParser simParser = new SimParser(network, path
						+ File.separator + "analysis");
				simParser.createMaps();
				Map<Instance, Map<Action, TimeGoDone>> execution = simParser
						.getExecutionMap();

				WeightWriter weightWriter = new WeightWriter(execution,
						network, path + File.separator + "analysis"
								+ File.separator + network.getName() + ".ew");
				weightWriter.writeProtobuf();
				ExecutionChart chart = new ExecutionChart(execution, network,
						path);
				chart.saveChart();

			}
		}
	}

	@Override
	public boolean exportRuntimeLibrary() throws OrccException {

		String libPath = path + File.separator + "lib";
		boolean result = true;

		result &= copyFolderToFileSystem("/HdlLibraries/systemBuilder/vhdl",
				libPath + File.separator + "systemBuilder");
		result &= copyFolderToFileSystem("/HdlLibraries/systemActors", libPath
				+ File.separator + "systemActors");
		result &= copyFolderToFileSystem("/HdlLibraries/simPackage", libPath
				+ File.separator + "simPackage");

		return result;
	}

	@Override
	protected boolean printInstance(Instance instance) {
		StandardPrinter printer = new StandardPrinter(
				"net/sf/orcc/backends/xlim/hw/Actor.stg", !debugMode);

		printer.getOptions().put("fpgaType", fpgaName);

		printer.setExpressionPrinter(new XlimExprPrinter());
		printer.setTypePrinter(new XlimTypePrinter());

		String xlimPath = path + File.separator + "xlim";
		new File(xlimPath).mkdir();

		Boolean printOK = true;
		// Test if instance is Native
		if (!instance.getActor().isNative()) {

			// Print TestBench
			printTestbench(instance);
			// Print TestBench tcl script
			printTCL(instance);

			printOK = printer.print(instance.getName() + ".xlim", xlimPath,
					instance);
			if (!printOK) {

				try {
					String xlim = null;
					String id = instance.getName();
					File file = new File(xlimPath + File.separator + id
							+ ".xlim");
					if (file.exists()) {
						xlim = file.getCanonicalPath();
					}
					List<String> flags = new ArrayList<String>(forgeFlags);
					flags.addAll(Arrays.asList("-d", srcPath, "-o", id, xlim));

					long t0 = System.currentTimeMillis();
					Boolean okForge = false;

					if (instanceToDesign) {
						// Experimental
						okForge = runForge(flags.toArray(new String[0]),
								instance);
					} else {
						okForge = Forge.runForge(flags.toArray(new String[0]));
					}
					long t1 = System.currentTimeMillis();
					if (okForge) {
						if (goDoneSignal) {
							VerilogAddGoDone instanceWithGoDone = new VerilogAddGoDone(
									instance, srcPath, srcGoDonePath);
							instanceWithGoDone.addGoDone();
						}
						write("Compiling instance: " + id + ": Compiled in: "
								+ ((float) (t1 - t0) / (float) 1000) + "s\n");
					} else {
						write("Compiling instance: " + id
								+ ": OpenForge failed to compile" + "\n");
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

		return printOK;
	}

	private void printNetwork(Network network) {
		// Get the current time
		DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
		Date date = new Date();

		String currentTime = dateFormat.format(date);

		Orc2HDLPrinter printer;
		String file = network.getSimpleName();

		file += ".vhd";
		printer = new Orc2HDLPrinter("net/sf/orc2hdl/templates/Network.stg");

		printer.setExpressionPrinter(new XlimExprPrinter());
		printer.setTypePrinter(new XlimTypePrinter());
		printer.getOptions().put("fifoSize", fifoSize);
		printer.getOptions().put("currentTime", currentTime);

		printer.print(file, srcPath, network);

		if (goDoneSignal || modelsimAnalysis) {
			new File(srcGoDonePath).mkdir();
			printer.getOptions().put("goDoneSignal", goDoneSignal);
			printer.print(file, srcGoDonePath, network);
			if (modelsimAnalysis) {
				String analysis = path + File.separator + "analysis";
				new File(analysis).mkdir();
				printer = new Orc2HDLPrinter(
						"net/sf/orc2hdl/templates/GoDoneTestBench.stg");
				printer.setExpressionPrinter(new XlimExprPrinter());
				printer.setTypePrinter(new XlimTypePrinter());
				printer.getOptions().put("currentTime", currentTime);
				file = "tb_" + network.getSimpleName() + ".vhd";
				printer.print(file, srcGoDonePath, network);
			}
		}
	}

	private void printSimDoFile(Network network) {
		// Get the current time
		DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
		Date date = new Date();

		String currentTime = dateFormat.format(date);

		Orc2HDLPrinter printer;
		String file = network.getName();

		printer = new Orc2HDLPrinter("net/sf/orc2hdl/templates/Top_Sim_do.stg");
		printer.setExpressionPrinter(new XlimExprPrinter());
		printer.setTypePrinter(new XlimTypePrinter());
		printer.getOptions().put("currentTime", currentTime);

		file = "sim_" + network.getSimpleName() + ".do";

		printer.print(file, simPath, network);

		if (goDoneSignal) {
			printer.getOptions().put("goDoneSignal", goDoneSignal);
			file = "sim_" + network.getSimpleName() + "_goDone" + ".do";
			new File(simPath).mkdir();
			printer.print(file, simPath, network);
		}

		if (modelsimAnalysis) {
			printer.getOptions().put("goDoneSignal", goDoneSignal);
			printer.getOptions().put("modelsimAnalysis", modelsimAnalysis);
			printer.getOptions().put("simTime", simTime);
			file = "sim_tb_" + network.getSimpleName() + ".do";
			new File(simPath).mkdir();
			printer.print(file, simPath, network);
		}
		// Copy the glbl.v file to the simulation "sim" folder

		// Get the current folder
		URL glblFileURL = Platform.getBundle("net.sf.orc2hdl").getEntry(
				"/HdlLibraries/glbl");

		try {

			String glblFilePath = new File(FileLocator.resolve(glblFileURL)
					.getFile()).getAbsolutePath();
			IFileSystem fileSystem = EFS.getLocalFileSystem();

			String path = glblFilePath + "/glbl.v";
			URI uri = new File(path).toURI();
			IFileStore pluginDir = fileSystem.getStore(uri);

			path = simPath + "/glbl.v";
			uri = new File(path).toURI();
			IFileStore copyDir = fileSystem.getStore(uri);

			pluginDir.copy(copyDir, EFS.OVERWRITE, null);

		} catch (IOException e) {
			e.printStackTrace();
		} catch (CoreException e) {
			e.printStackTrace();
		}

	}

	private void printTCL(Instance instance) {
		Orc2HDLPrinter printer = new Orc2HDLPrinter(
				"net/sf/orc2hdl/templates/ModelSim_Script.stg");
		printer.print("tcl_" + instance.getSimpleName() + ".tcl", tbPath,
				instance);
	}

	private void printTestbench(Instance instance) {
		Orc2HDLPrinter tbPrinter = new Orc2HDLPrinter(
				"net/sf/orc2hdl/templates/ModelSim_Testbench.stg");
		tbPrinter.getOptions().put("tracePath", tracePath);
		tbPrinter.print(instance.getSimpleName() + "_tb.vhd", tbPath
				+ File.separator + "vhd", instance);
	}

	private boolean runForge(String[] args, Instance instance) {
		Forge f = new Forge();
		GenericJob forgeMainJob = new GenericJob();
		boolean error = false;
		try {
			// Experimental
			forgeMainJob.setOptionValues(args);
			f.preprocess(forgeMainJob);
			Engine engine = new DesignEngine(forgeMainJob, instance);
			engine.begin();
		} catch (NewJob.ForgeOptionException foe) {
			write("Command line option error: " + foe.getMessage());
			write("");
			write(OptionRegistry.usage(false));
			error = true;
		} catch (ForgeFatalException ffe) {
			write("Forge compilation ended with fatal error:");
			write(ffe.getMessage());
			error = true;
		}

		return !error;
	}

	public void runModelSim(String modelSim, Network network) {
		try {
			String line;
			File fPath = new File(simPath);

			String arg = " -c -do sim_tb_" + network.getSimpleName() + ".do";
			String cmd = modelSim + arg;
			write("Launching Modelsim\n");

			Process p = Runtime.getRuntime().exec(cmd, null, fPath);

			BufferedReader bri = new BufferedReader(new InputStreamReader(
					p.getInputStream()));
			while ((line = bri.readLine()) != null) {
				write("Orc2HDL: " + line + "\n");
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
