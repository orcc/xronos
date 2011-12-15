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

import static net.sf.orcc.OrccLaunchConstants.DEBUG_MODE;
import static net.sf.orcc.OrccLaunchConstants.MAPPING;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import net.sf.openforge.app.Forge;
import net.sf.orc2hdl.printer.Orc2HDLPrinter;
import net.sf.orcc.OrccException;
import net.sf.orcc.backends.AbstractBackend;
import net.sf.orcc.backends.CustomPrinter;
import net.sf.orcc.backends.StandardPrinter;
import net.sf.orcc.backends.transformations.CastAdder;
import net.sf.orcc.backends.transformations.DivisionSubstitution;
import net.sf.orcc.backends.transformations.EmptyThenElseNodeAdder;
import net.sf.orcc.backends.transformations.Inliner;
import net.sf.orcc.backends.transformations.InstPhiTransformation;
import net.sf.orcc.backends.transformations.Multi2MonoToken;
import net.sf.orcc.backends.transformations.StoreOnceTransformation;
import net.sf.orcc.backends.transformations.TypeResizer;
import net.sf.orcc.backends.transformations.UnitImporter;
import net.sf.orcc.backends.xlim.XlimActorTemplateData;
import net.sf.orcc.backends.xlim.XlimExprPrinter;
import net.sf.orcc.backends.xlim.XlimTypePrinter;
import net.sf.orcc.backends.xlim.transformations.CustomPeekAdder;
import net.sf.orcc.backends.xlim.transformations.GlobalArrayInitializer;
import net.sf.orcc.backends.xlim.transformations.InstTernaryAdder;
import net.sf.orcc.backends.xlim.transformations.ListFlattener;
import net.sf.orcc.backends.xlim.transformations.LiteralIntegersAdder;
import net.sf.orcc.backends.xlim.transformations.LocalArrayRemoval;
import net.sf.orcc.backends.xlim.transformations.UnaryListRemoval;
import net.sf.orcc.backends.xlim.transformations.XlimDeadVariableRemoval;
import net.sf.orcc.backends.xlim.transformations.XlimVariableRenamer;
import net.sf.orcc.df.Actor;
import net.sf.orcc.df.DfFactory;
import net.sf.orcc.df.Instance;
import net.sf.orcc.df.Network;
import net.sf.orcc.df.transformations.Instantiator;
import net.sf.orcc.df.transformations.NetworkFlattener;
import net.sf.orcc.df.util.DfSwitch;
import net.sf.orcc.ir.transformations.BlockCombine;
import net.sf.orcc.ir.transformations.BuildCFG;
import net.sf.orcc.ir.transformations.DeadCodeElimination;
import net.sf.orcc.ir.transformations.DeadGlobalElimination;
import net.sf.orcc.ir.transformations.SSATransformation;
import net.sf.orcc.ir.transformations.TacTransformation;
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

	private boolean debugMode;

	private boolean goDoneSignal;

	private String fpgaName;

	private List<String> forgeFlags;

	// private boolean haveSystemActors;

	private List<String> entities;

	private HashSet<String> entitySet;

	private Map<String, String> clkDomains;

	private void computeEntityList(Instance instance) {
		if (instance.isActor()) {
			String name = instance.getName();
			if (!entitySet.contains(name)) {
				entitySet.add(name);
				entities.add(name);
			}
		} else if (instance.isNetwork()) {
			String name = instance.getName();
			Network network = instance.getNetwork();
			if (!entitySet.contains(name)) {
				for (Instance subInstance : network.getInstances()) {
					computeEntityList(subInstance);
				}

				entitySet.add(name);
			}
		}
	}

	private void copySystemActorsLib() {
		// Copy systemBuilder library to the output folder
		String systemBuilderPath = path + File.separator + "lib"
				+ File.separator + "systemActors";
		new File(systemBuilderPath).mkdir();
		new File(systemBuilderPath + File.separator + "io").mkdir();
		new File(systemBuilderPath + File.separator + "types").mkdir();
		// Get the current folder
		URL hdlLibrariesURL = Platform.getBundle("net.sf.orc2hdl").getEntry(
				"/HdlLibraries/systemActors");

		try {
			List<String> systemActorsFileList = Arrays.asList(
					"types/sa_types.vhd", "io/Source.vhd");
			String hdlLibrariesPath = new File(FileLocator.resolve(
					hdlLibrariesURL).getFile()).getAbsolutePath();
			IFileSystem fileSystem = EFS.getLocalFileSystem();

			for (String files : systemActorsFileList) {
				String path = hdlLibrariesPath + File.separator + files;
				URI uri = new File(path).toURI();
				IFileStore pluginDir = fileSystem.getStore(uri);

				path = systemBuilderPath + File.separator + files;
				uri = new File(path).toURI();
				IFileStore copyDir = fileSystem.getStore(uri);

				pluginDir.copy(copyDir, EFS.OVERWRITE, null);
			}

		} catch (IOException e) {
			e.printStackTrace();
		} catch (CoreException e) {
			e.printStackTrace();
		}

	}

	private void copySystemBuilderLib() {
		// Copy systemBuilder library to the output folder
		String systemBuilderPath = path + File.separator + "lib"
				+ File.separator + "systemBuilder";
		new File(systemBuilderPath).mkdir();

		// Get the current folder
		URL hdlLibrariesURL = Platform.getBundle("net.sf.orc2hdl").getEntry(
				"/HdlLibraries/systemBuilder/vhdl");

		try {
			List<String> systemBuilderFifo = Arrays.asList("sbtypes.vhdl",
					"sbfifo_behavioral.vhdl", "sbfifo.vhdl");
			String hdlLibrariesPath = new File(FileLocator.resolve(
					hdlLibrariesURL).getFile()).getAbsolutePath();
			IFileSystem fileSystem = EFS.getLocalFileSystem();

			for (String files : systemBuilderFifo) {

				String path = hdlLibrariesPath + File.separator + files;
				URI uri = new File(path).toURI();
				IFileStore pluginDir = fileSystem.getStore(uri);

				path = systemBuilderPath + File.separator + files;
				uri = new File(path).toURI();
				IFileStore copyDir = fileSystem.getStore(uri);

				pluginDir.copy(copyDir, EFS.OVERWRITE, null);
			}

		} catch (IOException e) {
			e.printStackTrace();
		} catch (CoreException e) {
			e.printStackTrace();
		}

	}

	@Override
	protected void doInitializeOptions() {
		clkDomains = getAttribute(MAPPING, new HashMap<String, String>());
		goDoneSignal = getAttribute("net.sf.orc2hdl.goDoneSignal", false);
		String fpgaType = getAttribute("net.sf.orc2hdl.FpgaType", "Virtex 2");

		if (fpgaType.equals("Spartan 3")) {
			fpgaName = "xc3s5000-5-fg1156";
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

		DfSwitch<?>[] transformations = { new StoreOnceTransformation(),
				new Multi2MonoToken(), new LocalArrayRemoval(),
				new DivisionSubstitution(), new UnitImporter(),
				new SSATransformation(), new TypeResizer(false, true, true),
				new GlobalArrayInitializer(true), new InstTernaryAdder(),
				new Inliner(true, true), new UnaryListRemoval(),
				new CustomPeekAdder(), new DeadGlobalElimination(),
				new DeadCodeElimination(), new XlimDeadVariableRemoval(),
				new ListFlattener(), new TacTransformation(), new BuildCFG(),
				new InstPhiTransformation(), new LiteralIntegersAdder(),
				new CastAdder(true), new XlimVariableRenamer(),
				new EmptyThenElseNodeAdder(), new BlockCombine() };

		for (DfSwitch<?> transformation : transformations) {
			transformation.doSwitch(actor);
			ResourceSet set = new ResourceSetImpl();
			if (debugMode && !IrUtil.serializeActor(set, path, actor)) {
				System.out.println("oops " + transformation + " "
						+ actor.getName());
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
		network = new Instantiator().doSwitch(network);
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
		// Create the lib folder
		new File(path + File.separator + "lib").mkdir();
		// Copy the systemBuilder and the systemActor libs into lib
		copySystemBuilderLib();
		copySystemActorsLib();

		// Print the xlim files
		printInstances(network);
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

		String srcPath = path + File.separator + "src";
		String goDonePath = path + File.separator + "srcGoDone";
		if (goDoneSignal) {
			new File(goDonePath).mkdir();
		}
		Boolean printOK = true;
		// Test if instance is Native
		if (!instance.getActor().isNative()) {
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
					Boolean okForge = Forge.runForge((String[]) flags
							.toArray(new String[0]));
					long t1 = System.currentTimeMillis();
					if (okForge) {
						if (goDoneSignal) {
							VerilogAddGoDone instanceWithGoDone = new VerilogAddGoDone(
									instance, srcPath, goDonePath);
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
		// generate instances test bench
		File folder = new File(path + File.separator + "Testbench");
		if (!folder.exists()) {
			folder.mkdir();
		}
		StandardPrinter instancePrinter = new StandardPrinter(
				"net/sf/orcc/backends/xlim/hw/ModelSim_Testbench.stg");
		Instance instance = DfFactory.eINSTANCE.createInstance(
				network.getName(), network);
		printTestbench(instancePrinter, instance);
		printTCL(instance);

		Orc2HDLPrinter printer;
		String file = network.getSimpleName();

		file += ".vhd";
		printer = new Orc2HDLPrinter("net/sf/orc2hdl/templates/Network.stg");

		printer.setExpressionPrinter(new XlimExprPrinter());
		printer.setTypePrinter(new XlimTypePrinter());
		printer.getOptions().put("fifoSize", fifoSize);
		printer.getOptions().put("currentTime", currentTime);

		// Create the src directory and print the network inside
		String SrcPath = path + File.separator + "src";
		String goDonePath = path + File.separator + "srcGoDone";

		new File(SrcPath).mkdir();
		printer.print(file, SrcPath, network);

		if (goDoneSignal) {
			new File(goDonePath).mkdir();
			printer.getOptions().put("goDoneSignal", goDoneSignal);
			printer.print(file, goDonePath, network);
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
		String SimPath = path + File.separator + "sim";
		new File(SimPath).mkdir();
		printer.print(file, SimPath, network);

		if (goDoneSignal) {
			printer.getOptions().put("goDoneSignal", goDoneSignal);
			file = "sim_" + network.getSimpleName() + "_goDone" + ".do";
			new File(SimPath).mkdir();
			printer.print(file, SimPath, network);
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

			path = SimPath + "/glbl.v";
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
		CustomPrinter printer = new CustomPrinter(
				"net/sf/orcc/backends/xlim/hw/ModelSim_Script.stg");

		entities = new ArrayList<String>();
		entitySet = new HashSet<String>();
		computeEntityList(instance);

		printer.print("TCLLists.tcl", path, "TCLLists", "name", instance
				.getNetwork().getName(), "entities", entities);
	}

	private void printTestbench(StandardPrinter printer, Instance instance) {
		printer.print(instance.getName() + "_tb.vhd", path + File.separator
				+ "Testbench", instance);

		if (instance.isNetwork()) {
			Network network = instance.getNetwork();
			for (Instance subInstance : network.getInstances()) {
				printTestbench(printer, subInstance);
			}
		}
	}
}
