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
package org.xronos.orcc.systemc;

import static net.sf.orcc.backends.BackendsConstants.BXDF_FILE;
import static net.sf.orcc.backends.BackendsConstants.IMPORT_BXDF;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import net.sf.orcc.backends.AbstractBackend;
import net.sf.orcc.backends.transform.BlockForAdder;
import net.sf.orcc.backends.transform.DisconnectedOutputPortRemoval;
import net.sf.orcc.backends.util.Validator;
import net.sf.orcc.df.Actor;
import net.sf.orcc.df.Instance;
import net.sf.orcc.df.Network;
import net.sf.orcc.df.transform.Instantiator;
import net.sf.orcc.df.transform.NetworkFlattener;
import net.sf.orcc.df.transform.UnitImporter;
import net.sf.orcc.df.util.DfVisitor;
import net.sf.orcc.df.util.NetworkValidator;
import net.sf.orcc.ir.CfgNode;
import net.sf.orcc.ir.transform.ControlFlowAnalyzer;
import net.sf.orcc.ir.transform.RenameTransformation;
import net.sf.orcc.tools.mapping.XmlBufferSizeConfiguration;
import net.sf.orcc.util.FilesManager;
import net.sf.orcc.util.OrccLogger;
import net.sf.orcc.util.Result;

import org.xronos.orcc.backend.transform.CheckVarSize;
import org.xronos.orcc.forge.scheduler.ActorAddFSM;
import org.xronos.orcc.forge.transform.memory.VarInitializer;
import org.xronos.orcc.systemc.transform.LoopLabeler;
import org.xronos.orcc.systemc.transform.UniquePortMemory;

/**
 * A synthesizable SystemC backend based on Xronos principles
 * 
 * @author Endri Bezati
 *
 */
public class XronosSystemC extends AbstractBackend {

	/** Printers **/
	private NetworkPrinter nPrinter;
	private InstancePrinter iPrinter;
	private TestbenchPrinter tbPrinter;
	private TestBenchUtilityPrinter tbutilityPrinter;
	private VHDLTestbenchPrinter tbVHDLPrinter;
	private TclPrinter tclPrinter;
	private ReadMePrinter readMePrinter;

	/** Path for generated SystemC Actor and Network source file **/
	private String srcPath;
	private String srcHeaderPath;

	/** Path for the RTL to be populated by HLS tools **/
	private String rtlPath;

	/** Path that contains the SystemC TestBench files **/
	private String tbPath;
	private String tbSrcPath;
	private String tbHeaderPath;
	private String tbVHDLPath;

	/** Path for TCL scripts **/
	private String scriptsPath;
	private String scriptsSynthesisPath;
	private String scriptsSimulationPath;

	public XronosSystemC() {
		nPrinter = new NetworkPrinter();
		iPrinter = new InstancePrinter();
		tbPrinter = new TestbenchPrinter();
		tbutilityPrinter = new TestBenchUtilityPrinter();
		tbVHDLPrinter = new VHDLTestbenchPrinter();
		tclPrinter = new TclPrinter();
		readMePrinter = new ReadMePrinter();
	}

	@Override
	protected void doInitializeOptions() {
		// Create Folders
		// -- Source folder
		srcPath = outputPath + File.separator + "src";
		File srcDir = new File(srcPath);
		if (!srcDir.exists()) {
			srcDir.mkdir();
		}

		// -- Source header folder
		srcHeaderPath = srcPath + File.separator + "header";
		File srcHeaderDir = new File(srcPath);
		if (!srcHeaderDir.exists()) {
			srcHeaderDir.mkdir();
		}

		// -- RTL folder
		rtlPath = outputPath + File.separator + "rtl";
		File rtlDir = new File(rtlPath);
		if (!rtlDir.exists()) {
			rtlDir.mkdir();
		}

		// -- TestBench folder
		tbPath = outputPath + File.separator + "testbench";
		File tbDir = new File(tbPath);
		if (!tbDir.exists()) {
			tbDir.mkdir();
		}

		// -- TestBench source path
		tbSrcPath = tbPath + File.separator + "src";
		File tbSrcDir = new File(tbSrcPath);
		if (!tbSrcDir.exists()) {
			tbSrcDir.mkdir();
		}

		// -- TestBench header path
		tbHeaderPath = tbSrcPath + File.separator + "header";
		File tbHeaderDir = new File(tbHeaderPath);
		if (!tbHeaderDir.exists()) {
			tbHeaderDir.mkdir();
		}

		// -- TestBench vhdl path
		tbVHDLPath = tbPath + File.separator + "vhdl";
		File tbVHDLDir = new File(tbVHDLPath);
		if (!tbVHDLDir.exists()) {
			tbVHDLDir.mkdir();
		}

		// -- Scripts path
		scriptsPath = outputPath + File.separator + "scripts";
		File scriptsDir = new File(scriptsPath);
		if (!scriptsDir.exists()) {
			scriptsDir.mkdir();
		}
		// -- Scripts path Synthesis
		scriptsSynthesisPath = scriptsPath + File.separator + "synthesis";
		File scriptsSynthesisDir = new File(scriptsSynthesisPath);
		if (!scriptsSynthesisDir.exists()) {
			scriptsSynthesisDir.mkdir();
		}

		// -- Scripts path Simualtion
		scriptsSimulationPath = scriptsPath + File.separator + "simulation";
		File scriptsSimulationDir = new File(scriptsSimulationPath);
		if (!scriptsSimulationDir.exists()) {
			scriptsSimulationDir.mkdir();
		}

		// -- TestBench queue traces folder
		String tracesPath = tbPath + File.separator + "traces";
		File tracesDir = new File(tracesPath);
		if (!tracesDir.exists()) {
			tracesDir.mkdir();
		}

		// -- Set Printer Options
		nPrinter.setOptions(getOptions());
		iPrinter.setOptions(getOptions());
		tbPrinter.setOptions(getOptions());
		tbVHDLPrinter.setOptions(getOptions());
		tclPrinter.setOptions(getOptions());

		// -- Network Transformations
		networkTransfos.add(new Instantiator(true));
		networkTransfos.add(new NetworkFlattener());
		networkTransfos.add(new UnitImporter());
		networkTransfos.add(new DisconnectedOutputPortRemoval());
		networkTransfos.add(new RenameTransformation(getRenameMap()));

		// -- Child Transformations
		childrenTransfos.add(new UniquePortMemory(fifoSize));
		// childrenTransfos.add(new DeadGlobalElimination());
		childrenTransfos.add(new ActorAddFSM());
		childrenTransfos.add(new VarInitializer());
		// childrenTransfos.add(new CheckVarSize());
		childrenTransfos.add(new DfVisitor<CfgNode>(new ControlFlowAnalyzer()));
		childrenTransfos.add(new BlockForAdder());
		childrenTransfos.add(new LoopLabeler());
	}

	@Override
	protected Result doLibrariesExtraction() {
		Result result = FilesManager.extract(
				"/bundle/lib/simulation/sim_package.vhd", tbVHDLPath);
		return result;
	}

	@Override
	protected void doValidate(Network network) {
		Validator.checkMinimalFifoSize(network, fifoSize);

		new NetworkValidator().doSwitch(network);
	}

	@Override
	protected Result doGenerateNetwork(Network network) {
		final Result result = Result.newInstance();
		nPrinter.setNetwork(network);
		result.merge(FilesManager.writeFile(nPrinter.getContent(),
				srcHeaderPath, network.getSimpleName() + ".h"));
		result.merge(FilesManager.writeFile(nPrinter.getNetworkContentSource(),
				srcPath, network.getSimpleName() + ".cpp"));
		return result;
	}

	@Override
	protected Result doGenerateInstance(Instance instance) {
		final Result result = Result.newInstance();
		iPrinter.setInstance(instance);
		result.merge(FilesManager.writeFile(iPrinter.getActorHeaderContent(),
				srcHeaderPath, instance.getSimpleName() + ".h"));
		result.merge(FilesManager.writeFile(iPrinter.getActorSourceContent(),
				srcPath, instance.getSimpleName() + ".cpp"));
		return result;
	}

	@Override
	protected void beforeGeneration(Network network) {
		network.computeTemplateMaps();

		// if required, load the buffer size from the mapping file
		if (getOption(IMPORT_BXDF, false)) {
			File f = new File(getOption(BXDF_FILE, ""));
			new XmlBufferSizeConfiguration().load(f, network);
		}
	}

	@Override
	protected Result doAdditionalGeneration(Network network) {
		final Result result = Result.newInstance();

		// -- README File
		readMePrinter.setNetwork(network);
		result.merge(FilesManager.writeFile(readMePrinter.getContent(),
				outputPath, "README.txt"));

		// -- TestBench Utility Printers
		result.merge(FilesManager.writeFile(tbutilityPrinter.getKickerModule(),
				tbHeaderPath, "tb_kicker.h"));

		result.merge(FilesManager.writeFile(tbutilityPrinter.getDriverModule(),
				tbHeaderPath, "tb_driver.h"));

		result.merge(FilesManager.writeFile(
				tbutilityPrinter.getCompareModule(), tbHeaderPath,
				"tb_compare.h"));

		tbutilityPrinter.setNetwork(network);
		result.merge(FilesManager.writeFile(tbutilityPrinter.getEndSimModule(),
				tbHeaderPath, "tb_endsim_n_" + network.getSimpleName() + ".h"));

		// -- TestBench for Network
		tbPrinter.setNetwork(network);
		result.merge(FilesManager.writeFile(tbPrinter.getContent(), tbSrcPath,
				"tb_" + network.getSimpleName() + ".cpp"));

		// -- VHDL Testbench for Network
		tbVHDLPrinter.setNetwork(network);
		result.merge(FilesManager.writeFile(tbVHDLPrinter.getContent(),
				tbVHDLPath, network.getSimpleName() + "_tb" + ".vhd"));

		// -- TCL scripts for actor
		tclPrinter.setNetwork(network);
		result.merge(FilesManager.writeFile(tclPrinter.getContentForVivado(),
				scriptsSynthesisPath, "tcl_" + network.getSimpleName() + ".tcl"));

		tclPrinter.setNetwork(network);
		result.merge(FilesManager.writeFile(tclPrinter.getContentForModelSim(),
				scriptsSimulationPath, "tcl_" + network.getSimpleName()
						+ ".tcl"));

		return result;
	}

	@Override
	protected Result doAdditionalGeneration(Actor actor) {
		final Result result = Result.newInstance();
		// -- TestBench for actor
		tbPrinter.setActor(actor);
		result.merge(FilesManager.writeFile(tbPrinter.getContent(), tbSrcPath,
				"tb_" + actor.getSimpleName() + ".cpp"));

		// -- End Simulation for Actor
		tbutilityPrinter.setActor(actor);
		result.merge(FilesManager.writeFile(tbutilityPrinter.getEndSimModule(),
				tbHeaderPath, "tb_endsim_a_" + actor.getSimpleName() + ".h"));

		// -- VHDL Testbench for Network
		tbVHDLPrinter.setActor(actor);
		result.merge(FilesManager.writeFile(tbVHDLPrinter.getContent(),
				tbVHDLPath, actor.getSimpleName() + "_tb" + ".vhd"));

		// -- TCL scripts for actor
		tclPrinter.setActor(actor);
		result.merge(FilesManager.writeFile(tclPrinter.getContentForVivado(),
				scriptsSynthesisPath, "tcl_" + actor.getSimpleName() + ".tcl"));

		tclPrinter.setActor(actor);
		result.merge(FilesManager.writeFile(tclPrinter.getContentForModelSim(),
				scriptsSimulationPath, "tcl_" + actor.getSimpleName() + ".tcl"));
		return result;
	}

	@Override
	protected Result doGenerateActor(Actor actor) {
		final Result result = Result.newInstance();

		iPrinter.setActor(actor);
		result.merge(FilesManager.writeFile(iPrinter.getActorHeaderContent(),
				srcHeaderPath, actor.getSimpleName() + ".h"));
		result.merge(FilesManager.writeFile(iPrinter.getActorSourceContent(),
				srcPath, actor.getSimpleName() + ".cpp"));

		return result;
	}

	protected Map<String, String> getRenameMap() {
		Map<String, String> renameMap = new HashMap<String, String>();
		renameMap.put("abs", "abs_replaced");
		renameMap.put("getw", "getw_replaced");
		renameMap.put("exit", "exit_replaced");
		renameMap.put("index", "index_replaced");
		renameMap.put("log2", "log2_replaced");
		renameMap.put("max", "max_replaced");
		renameMap.put("min", "min_replaced");
		renameMap.put("select", "select_replaced");
		renameMap.put("Out", "Out_replaced");
		renameMap.put("OUT", "OUT_REPLACED");
		renameMap.put("In", "In_replaced");
		renameMap.put("IN", "IN_REPLACED");
		renameMap.put("SIZE", "SIZE_REPLACED");
		renameMap.put("done", "done_replaced");
		renameMap.put("start", "start_replaced");
		renameMap.put("round", "round_replaced");

		return renameMap;
	}

}