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

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import net.sf.orcc.OrccException;
import net.sf.orcc.backends.AbstractBackend;
import net.sf.orcc.backends.transformations.CastAdder;
import net.sf.orcc.backends.transformations.DivisionSubstitution;
import net.sf.orcc.backends.transformations.Inliner;
import net.sf.orcc.backends.transformations.Multi2MonoToken;
import net.sf.orcc.backends.transformations.tac.ExpressionSplitter;
import net.sf.orcc.backends.xlim.XlimActorTemplateData;
import net.sf.orcc.backends.xlim.XlimExprPrinter;
import net.sf.orcc.backends.xlim.XlimTypePrinter;
import net.sf.orcc.backends.xlim.transformations.CustomPeekAdder;
import net.sf.orcc.backends.xlim.transformations.GlobalArrayInitializer;
import net.sf.orcc.backends.xlim.transformations.InstPhiTransformation;
import net.sf.orcc.backends.xlim.transformations.InstTernaryAdder;
import net.sf.orcc.backends.xlim.transformations.ListFlattener;
import net.sf.orcc.backends.xlim.transformations.LiteralIntegersAdder;
import net.sf.orcc.backends.xlim.transformations.LocalArrayRemoval;
import net.sf.orcc.backends.xlim.transformations.UnaryListRemoval;
import net.sf.orcc.backends.xlim.transformations.XlimDeadVariableRemoval;
import net.sf.orcc.backends.xlim.transformations.XlimVariableRenamer;
import net.sf.orcc.ir.Actor;
import net.sf.orcc.ir.transformations.BlockCombine;
import net.sf.orcc.ir.transformations.BuildCFG;
import net.sf.orcc.ir.transformations.DeadCodeElimination;
import net.sf.orcc.ir.transformations.DeadGlobalElimination;
import net.sf.orcc.ir.transformations.SSATransformation;
import net.sf.orcc.ir.util.ActorVisitor;
import net.sf.orcc.ir.util.IrUtil;
import net.sf.orcc.network.Network;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileInfo;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.filesystem.IFileSystem;
import org.eclipse.core.filesystem.provider.FileStore;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Platform;

/**
 * This class defines an XLIM and OpenForge based back-end
 * 
 * @author Endri Bezati
 * @author Herve Yviquel
 * 
 */
public class Orc2HDL extends AbstractBackend {

	private boolean debugMode;

	private String FpgaType;

	private Boolean Verbose;

	private Boolean Pipeline;

	private Boolean NoBlockIO;

	private Boolean NoBlockBasedScheduling;

	private Boolean SimpleSharedMemoryArbitration;

	private Boolean NoEDKGeneration;

	private Boolean BalanceLoopLatency;

	private Boolean MultiplierDecomposition;

	private Boolean CombinationallyLUTReads;

	private Boolean AllowDualPortLUT;

	private Boolean NoLog;

	private Boolean NoInclude;

	private List<String> forgeFlags;

	@Override
	protected void doInitializeOptions() {

		FpgaType = getAttribute("net.sf.orc2hdl.FpgaType", "Virtex 2");
		Verbose = getAttribute("net.sf.orc2hdl.Verbose", false);
		Pipeline = getAttribute("net.sf.orc2hdl.Pipelining", true);
		NoBlockIO = getAttribute("net.sf.orc2hdl.NoBlockIO", true);
		NoBlockBasedScheduling = getAttribute(
				"net.sf.orc2hdl.NoBlockBasedScheduling", true);
		SimpleSharedMemoryArbitration = getAttribute(
				"net.sf.orc2hdl.SimpleSharedMemoryArbitration", true);
		NoEDKGeneration = getAttribute("net.sf.orc2hdl.NoEDKGeneration", true);
		BalanceLoopLatency = getAttribute("net.sf.orc2hdl.BalanceLoopLatency",
				true);
		MultiplierDecomposition = getAttribute(
				"net.sf.orc2hdl.MultiplierDecomposition", true);
		CombinationallyLUTReads = getAttribute(
				"net.sf.orc2hdl.CombinationallyLUTReads", true);
		AllowDualPortLUT = getAttribute("net.sf.orc2hdl.AllowDualPortLUT", true);
		NoLog = getAttribute("net.sf.orc2hdl.NoLog", true);
		NoInclude = getAttribute("net.sf.orc2hdl.NoInclude", true);
		debugMode = getAttribute(DEBUG_MODE, true);
	}

	@Override
	protected void doTransformActor(Actor actor) throws OrccException {
		XlimActorTemplateData data = new XlimActorTemplateData();
		actor.setTemplateData(data);

		new Multi2MonoToken().doSwitch(actor);
		new LocalArrayRemoval().doSwitch(actor);
		new DivisionSubstitution().doSwitch(actor);

		ActorVisitor<?>[] transformations = { new SSATransformation(),
				new GlobalArrayInitializer(true), new InstTernaryAdder(),
				new Inliner(true, true), new UnaryListRemoval(),
				new CustomPeekAdder(), new DeadGlobalElimination(),
				new DeadCodeElimination(), new XlimDeadVariableRemoval(),
				new ListFlattener(), new ExpressionSplitter(true), /*
																	 * new
																	 * CopyPropagator
																	 * (),
																	 */
				new BuildCFG(), new CastAdder(true, true),
				new InstPhiTransformation(), new LiteralIntegersAdder(true),
				new XlimVariableRenamer(), new BlockCombine() };

		for (ActorVisitor<?> transformation : transformations) {
			transformation.doSwitch(actor);
			if (debugMode && !IrUtil.serializeActor(path, actor)) {
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
		network.flatten();

		transformActors(network.getActors());

		network.computeTemplateMaps();

		TopNetworkTemplateData data = new TopNetworkTemplateData();

		data.computeTemplateMaps(network);
		network.setTemplateData(data);

		write("Printing Top VHDL network...\n");
		printNetwork(network);

	}

	private void printNetwork(Network network) {
		// Get the current time
		DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
		Date date = new Date();

		String currentTime = dateFormat.format(date);

		Orc2HDLNetworkPrinter printer;
		String file = network.getName();

		file += ".vhd";
		printer = new Orc2HDLNetworkPrinter("Top_VHDL_network");

		printer.setExpressionPrinter(new XlimExprPrinter());
		printer.setTypePrinter(new XlimTypePrinter());
		printer.getOptions().put("fifoSize", fifoSize);
		printer.getOptions().put("currentTime", currentTime);

		// Create the src directory and print the network inside
		String SrcPath = path + File.separator + "src";
		new File(SrcPath).mkdir();
		printer.print(file, SrcPath, network, "network");

		// Create the sim directory copy the glbl.v file in it and then print
		// the "do file"
		printSimDoFile(network);
		// Copy the systemBuilder libraries
		copySystemBuilderLib();
	}

	private void printSimDoFile(Network network) {
		// Get the current time
		DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
		Date date = new Date();

		String currentTime = dateFormat.format(date);

		Orc2HDLNetworkPrinter printer;
		String file = network.getName();
		write("Printing Network Simulation Do file... \n");

		printer = new Orc2HDLNetworkPrinter("Top_Sim_do");
		printer.setExpressionPrinter(new XlimExprPrinter());
		printer.setTypePrinter(new XlimTypePrinter());
		printer.getOptions().put("currentTime", currentTime);
		file = network.getName() + ".do";
		String SimPath = path + File.separator + "sim";
		new File(SimPath).mkdir();
		printer.print(file, SimPath, network, "simulation");

		// Copy the glbl.v file to the simulation "sim" folder

		// Get the current folder
		URL glblFileURL = Platform.getBundle("net.sf.orc2hdl").getEntry(
				"/HdlLibraries/glbl");

		try {

			String glblFilePath = new File(FileLocator.resolve(glblFileURL)
					.getFile()).getAbsolutePath();
			IFileSystem fileSystem = EFS.getLocalFileSystem();

			IFileStore pluginDir = fileSystem.getStore(URI.create(glblFilePath
					+ File.separator + "glbl.v"));

			IFileStore copyDir = fileSystem.getStore(URI.create(SimPath
					+ File.separator + "glbl.v"));

			pluginDir.copy(copyDir, EFS.OVERWRITE, null);

		} catch (IOException e) {
			e.printStackTrace();
		} catch (CoreException e) {
			e.printStackTrace();
		}

	}

	private void copySystemBuilderLib() {
		// Copy systemBuilder library to the output folder
		String systemBuilderPath = path + File.separator + "systemBuilder";
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

				IFileStore pluginDir = fileSystem.getStore(URI
						.create(hdlLibrariesPath + File.separator + files));

				IFileStore copyDir = fileSystem.getStore(URI
						.create(systemBuilderPath + File.separator + files));

				pluginDir.copy(copyDir, EFS.OVERWRITE, null);
			}

		} catch (IOException e) {
			e.printStackTrace();
		} catch (CoreException e) {
			e.printStackTrace();
		}

	}

}
