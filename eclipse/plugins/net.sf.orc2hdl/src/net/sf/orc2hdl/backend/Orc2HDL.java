package net.sf.orc2hdl.backend;

import static net.sf.orcc.OrccLaunchConstants.DEBUG_MODE;
import static net.sf.orcc.OrccLaunchConstants.MAPPING;
import static net.sf.orcc.OrccLaunchConstants.NO_LIBRARY_EXPORT;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import net.sf.openforge.app.Engine;
import net.sf.openforge.app.Forge;
import net.sf.openforge.app.ForgeFatalException;
import net.sf.openforge.app.GenericJob;
import net.sf.openforge.app.NewJob;
import net.sf.openforge.app.OptionRegistry;
import net.sf.openforge.verilog.model.Assign.UnbalancedAssignmentException;
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
import net.sf.orcc.util.OrccLogger;

import org.eclipse.core.resources.IFile;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;

/**
 * The OpenForge's Orcc Frontend, it uses the AbstractBackend class for the XLIM
 * Backend and for Orcc user Interface
 * 
 * @author Endri Bezati
 * 
 */

public class Orc2HDL extends AbstractBackend {

	/** The clock Domains Map **/
	private Map<String, String> clkDomains;

	/** Debug Mode, no caching, generating always **/
	private boolean debugMode;

	/** A list which contains the given forgeFlags **/
	private List<String> forgeFlags;

	/** The used Xilinx FPGA Name **/
	private String fpgaName;

	/** Generate Verilog files with Go And Done signal on Top Module **/
	private boolean generateGoDone;

	/** Use Orcc as a fronted for OpenForge, No XLIM code generation **/
	private boolean instanceToDesign;

	/** The path used for the RTL Go Done generation **/
	private String rtlGoDonePath;

	/** The path used for the RTL generation **/
	private String rtlPath;

	/** The path used for the simulation generation **/
	private String simPath;

	/** The path used for the testBench generation **/
	private String testBenchPath;

	@Override
	protected void doInitializeOptions() {
		clkDomains = getAttribute(MAPPING, new HashMap<String, String>());
		debugMode = getAttribute(DEBUG_MODE, true);
		instanceToDesign = getAttribute("net.sf.orc2hdl.instanceDesign", false);
		generateGoDone = getAttribute("net.sf.orc2hdl.generateGoDone", false);

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
		forgeFlags = new ArrayList<String>();
		forgeFlags.add("-vv");
		forgeFlags.add("-pipeline");
		forgeFlags.add("-noblockio");
		forgeFlags.add("-no_block_sched");
		forgeFlags.add("-simple_arbitration");
		forgeFlags.add("-noedk");
		forgeFlags.add("-loopbal");
		forgeFlags.add("-multdecomplimit");
		forgeFlags.add("2");
		forgeFlags.add("-comb_lut_mem_read");
		forgeFlags.add("-dplut");
		forgeFlags.add("-nolog");
		forgeFlags.add("-noinclude");
		forgeFlags.add("-report");
		forgeFlags.add("-Xdetailed_report");
	}

	@Override
	protected void doTransformActor(Actor actor) throws OrccException {
		XlimActorTemplateData data = new XlimActorTemplateData();
		actor.setTemplateData(data);
		if (!actor.isNative()) {
			if (instanceToDesign) {
				List<DfSwitch<?>> transformations = new ArrayList<DfSwitch<?>>();
				// transformations.add(new DfVisitor<Void>(new
				// LocalVarInitializer()));
				transformations.add(new StoreOnceTransformation());
				transformations
						.add(new DfVisitor<Void>(new LocalArrayRemoval()));
				transformations.add(new UnitImporter());
				transformations.add(new UnaryListRemoval());
				transformations
						.add(new DfVisitor<Void>(new SSATransformation()));
				transformations.add(new RepeatPattern());
				transformations.add(new GlobalArrayInitializer(true));
				transformations
						.add(new DfVisitor<Void>(new Inliner(true, true)));
				transformations.add(new DfVisitor<Void>(
						new DeadCodeElimination()));
				transformations.add(new DfVisitor<Expression>(
						new LiteralIntegersAdder()));
				transformations.add(new DfVisitor<Void>(new IndexFlattener()));
				transformations.add(new DfVisitor<Expression>(
						new TacTransformation()));
				transformations.add(new DfVisitor<CfgNode>(
						new ControlFlowAnalyzer()));
				transformations.add(new DfVisitor<Expression>(
						new LiteralIntegersAdder()));
				transformations.add(new DfVisitor<Expression>(new CastAdder(
						false, false)));
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
				DfSwitch<?>[] transformations = {
						new StoreOnceTransformation(),
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
						new DfVisitor<Expression>(new CastAdder(true, true)),
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
	}

	@Override
	protected void doVtlCodeGeneration(List<IFile> files) throws OrccException {
		// do not generate VTL
	}

	@Override
	protected void doXdfCodeGeneration(Network network) throws OrccException {
		// instantiate and flattens network
		new Instantiator(false, 1).doSwitch(network);
		new NetworkFlattener().doSwitch(network);

		// Transform Actors
		transformActors(network.getAllActors());

		// Compute the Network Template
		network.computeTemplateMaps();
		TopNetworkTemplateData data = new TopNetworkTemplateData();
		data.computeTemplateMaps(network, clkDomains);
		network.setTemplateData(data);

		// Print the Network
		printNetwork(network);

		// Print Simulation files
		printSimFiles(network);

		// Print Instance files
		printInstances(network);
	}

	@Override
	public boolean exportRuntimeLibrary() throws OrccException {
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

	@Override
	protected boolean printInstance(Instance instance) {
		StandardPrinter printer = new StandardPrinter(
				"net/sf/orcc/backends/xlim/hw/Actor.stg", !debugMode);

		printer.getOptions().put("fpgaType", fpgaName);

		printer.setExpressionPrinter(new XlimExprPrinter());
		printer.setTypePrinter(new XlimTypePrinter());

		// Create the XLIM Path
		String xlimPath = path + File.separator + "xlim";
		new File(xlimPath).mkdir();
		Boolean printOK = true;

		// Test if instance is Native
		if (!instance.getActor().isNative()) {

			// Print TCL launch script and VHD Testbenches for each Instance
			printTestbench(instance);

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
					flags.addAll(Arrays.asList("-d", rtlPath, "-o", id, xlim));

					long t0 = System.currentTimeMillis();
					Boolean okForge = false;

					// Experimental
					try {
						if (instanceToDesign) {
							okForge = runForge(flags.toArray(new String[0]),
									instance);
						} else {
							okForge = Forge.runForge(flags
									.toArray(new String[0]));
						}
					} catch (NullPointerException ex) {
						file.delete();
						OrccLogger.severeln("Instance: " + id
								+ ", failed to compile: NullPointerException, "
								+ ex.getMessage());
					} catch (NoSuchElementException ex) {
						file.delete();
						OrccLogger
								.severeln("Instance: "
										+ id
										+ ", failed to compile: NoSuchElementException, "
										+ ex.getMessage());
					} catch (UnbalancedAssignmentException ex) {
						file.delete();
						OrccLogger
								.severeln("Instance: "
										+ id
										+ ", failed to compile: UnbalancedAssignmentException, "
										+ ex.getMessage());
					} catch (ArrayIndexOutOfBoundsException ex) {
						file.delete();
						OrccLogger
								.severeln("Instance: "
										+ id
										+ ", failed to compile: ArrayIndexOutOfBoundsException, "
										+ ex.getMessage());
					} catch (Throwable t) {
						file.delete();
						OrccLogger.severeln("Instance: " + id
								+ ", failed to compile: " + t.getMessage());
					}

					long t1 = System.currentTimeMillis();
					if (okForge) {
						if (generateGoDone) {
							VerilogAddGoDone instanceWithGoDone = new VerilogAddGoDone(
									instance, rtlPath, rtlGoDonePath);
							instanceWithGoDone.addGoDone();
						}
						OrccLogger.traceln("Compiling instance: " + id
								+ ": Compiled in: "
								+ ((float) (t1 - t0) / (float) 1000) + "s");
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

		printer.print(file, rtlPath, network);

		if (generateGoDone) {
			printer.getOptions().put("generateGoDone", generateGoDone);
			printer.print(file, rtlGoDonePath, network);
			printer = new Orc2HDLPrinter(
					"net/sf/orc2hdl/templates/GoDoneTestBench.stg");
			printer.setExpressionPrinter(new XlimExprPrinter());
			printer.setTypePrinter(new XlimTypePrinter());
			printer.getOptions().put("currentTime", currentTime);
			file = "tb_" + network.getSimpleName() + ".vhd";
			printer.print(file, rtlGoDonePath, network);
		}

	}

	private void printSimFiles(Network network) {
		// Get the current time
		DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
		Date date = new Date();

		String currentTime = dateFormat.format(date);

		Orc2HDLPrinter printer;

		printer = new Orc2HDLPrinter("net/sf/orc2hdl/templates/Top_Sim_do.stg");
		printer.setExpressionPrinter(new XlimExprPrinter());
		printer.setTypePrinter(new XlimTypePrinter());
		printer.getOptions().put("currentTime", currentTime);

		String file = network.getName();
		file = "sim_" + network.getSimpleName() + ".do";
		printer.print(file, simPath, network);

		if (generateGoDone) {
			// Print Simulation Do file with Go and Done on Top Modules
			printer.getOptions().put("generateGoDone", generateGoDone);
			file = "sim_" + network.getSimpleName() + "_goDone" + ".do";
			new File(simPath).mkdir();
			printer.print(file, simPath, network);

			// Print Go Done Weights Simluation file
			String weightsPath = simPath + File.separator + "weights";
			File weightsDir = new File(weightsPath);
			if (!weightsDir.exists()) {
				weightsDir.mkdir();
			}
			printer.getOptions().put("generateGoDone", generateGoDone);
			printer.getOptions().put("modelsimAnalysis", generateGoDone);
			printer.getOptions().put("simTime", "10000");
			file = "sim_weights_" + network.getSimpleName() + ".do";
			new File(simPath).mkdir();
			printer.print(file, simPath, network);
		}
	}

	private void printTestbench(Instance instance) {
		// Print TCL Script
		Orc2HDLPrinter printer = new Orc2HDLPrinter(
				"net/sf/orc2hdl/templates/ModelSim_Script.stg");
		printer.print("tcl_" + instance.getSimpleName() + ".tcl",
				testBenchPath, instance);
		// Create VHD folder
		String tbVhdPath = testBenchPath + File.separator + "vhd";
		File tbVhdDir = new File(tbVhdPath);
		if (!tbVhdDir.exists()) {
			tbVhdDir.mkdir();
		}

		// Create the fifoTraces folder
		String tracePath = testBenchPath + File.separator + "fifoTraces";
		File fifoTracesDir = new File(tracePath);
		if (!fifoTracesDir.exists()) {
			fifoTracesDir.mkdir();
		}

		// Print the VHD testbenches
		Orc2HDLPrinter tbPrinter = new Orc2HDLPrinter(
				"net/sf/orc2hdl/templates/ModelSim_Testbench.stg");
		tbPrinter.getOptions().put("tracePath", tracePath);
		tbPrinter.print(instance.getSimpleName() + "_tb.vhd", tbVhdPath,
				instance);
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
			OrccLogger.severeln("Command line option error: "
					+ foe.getMessage());
			OrccLogger.severeln("");
			OrccLogger.severeln(OptionRegistry.usage(false));
			error = true;
		} catch (ForgeFatalException ffe) {
			OrccLogger.severeln("Forge compilation ended with fatal error:");
			OrccLogger.severeln(ffe.getMessage());
			error = true;
		}

		return !error;
	}

}
