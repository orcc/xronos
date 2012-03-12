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

package net.sf.openforge.backend.sysgen;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import net.sf.openforge.app.EngineThread;
import net.sf.openforge.app.ForgeFileHandler;
import net.sf.openforge.app.ForgeFileKey;
import net.sf.openforge.app.OptionRegistry;
import net.sf.openforge.backend.OutputEngine;
import net.sf.openforge.backend.hdl.VerilogTranslateEngine;
import net.sf.openforge.backend.timedc.CycleCTranslateEngine;
import net.sf.openforge.lim.CodeLabel;
import net.sf.openforge.lim.Design;
import net.sf.openforge.lim.io.FifoIF;
import net.sf.openforge.util.naming.ID;

/**
 * The SysgenSimApi is an output engine capable of creating the necessary files
 * for dropping a generated core into a SysGen simulation. The generated files
 * include:
 * <ul>
 * <li><i>design</i>.cxx - this file is the configuration API C++ class for the
 * design. It contains all the functionality to correctly configure the number
 * and types (rate, data type, etc) of ports on the core. Additionally this file
 * configures the relevant simulation characteristics and synthesis file
 * inclusion.
 * <li><i>design</i>_Model.cxx - this file is the simulation API C++ class for
 * the design. This file translates the System Generator &tm; simulation API to
 * the API used by our cycle accurate C models.
 * <li><i>design</i>_createBlock.m - this file is an MCode function/script which
 * generates the library block in System Generator &tm;
 * <li><i>design</i>_gui.xml - specifies the look and functionality of the block
 * configuration GUI.
 * <li><i>design</i>_declarations.xml - specifies any non-standard variables
 * that are passed between the System Generator &tm; environment and the
 * configuration API file <i>design</i>.cxx via the 'mask'.
 * </ul>
 * 
 * <p>
 * Created: Wed Mar 22 13:05:00 2006
 * 
 * @author imiller, last modified by $Author: imiller $
 * @version $Id: SysgenSimApi.java 128 2006-04-04 15:05:31Z imiller $
 */
public class SysgenSimApi implements OutputEngine {
	private static final ForgeFileKey SYSGEN_DIR = new ForgeFileKey(
			"SysGen directory");
	public static final ForgeFileKey API_CXX = new ForgeFileKey(
			"SysGen C++ class for interfaceing to SysGen");
	public static final ForgeFileKey MODEL_CXX = new ForgeFileKey(
			"SysGen C++ class for tying our model to SysGen");
	public static final ForgeFileKey MCODE_BLOCK = new ForgeFileKey(
			"SysGen createBlock mcode script");
	public static final ForgeFileKey GUI_XML = new ForgeFileKey(
			"SysGen XML file for definition of the config panel GUI");
	public static final ForgeFileKey DECL_XML = new ForgeFileKey(
			"SysGen XML file for declaration of vars");

	private static final String CORE_OUTPUT_RATE = "Sysgen::SysgenRate(1,0)";
	private String baseName = "unknown";
	private String configName = "unknown";
	private String libName = "unknown";
	private String modelName = "unknown";

	private ForgeFileHandler fileHandler = null;

	/**
	 * Initialize the target or destination for the output of this engine. This
	 * may include registering files with the ForgeFileHandler.
	 */
	public void initEnvironment() {
		this.fileHandler = EngineThread.getGenericJob().getFileHandler();
		// File base = this.fileHandler.registerFile(SYSGEN_DIR, "sysgen");
		File base = this.fileHandler.registerFile(SYSGEN_DIR, "");
		// this.fileHandler.registerFile(MCODE_BLOCK, base, "createBlock.m");
		this.fileHandler.registerFile(MCODE_BLOCK, base,
				this.fileHandler.buildName("_createBlock", "m"));
		this.fileHandler.registerFile(GUI_XML, base,
				this.fileHandler.buildName("_gui", "xml"));
		this.fileHandler.registerFile(DECL_XML, base,
				this.fileHandler.buildName("_declarations", "xml"));
		this.fileHandler.registerFile(API_CXX, base,
				this.fileHandler.buildName("", "cxx"));
		this.fileHandler.registerFile(MODEL_CXX, base,
				this.fileHandler.buildName("_Model", "cxx"));

		if (!this.fileHandler.isRegistered(VerilogTranslateEngine.SYNPRIMINCL)) {
			VerilogTranslateEngine.registerSynPrimIncl();
		}

		this.baseName = this.fileHandler.buildName("", null);
		this.configName = this.baseName + "_config";
		this.libName = this.baseName + "_lib";
		this.modelName = this.baseName + "_model";
	}

	/**
	 * Generate the output for this engine, including creation of the output
	 * files and/or directories.
	 */
	public void translate(Design design) throws IOException {
		final File baseDir = this.fileHandler.getFile(SYSGEN_DIR);
		final String funcName = net.sf.openforge.backend.timedc.CNameCache
				.getLegalIdentifier(ID.showLogical(design));
		if (!baseDir.exists()) {
			baseDir.mkdirs();
		}

		List<IOHandler> ioHandlers = new ArrayList<IOHandler>();
		for (Object fifoIF : design.getFifoInterfaces()) {
			ioHandlers.add(new IOHandler((FifoIF) fifoIF));
		}

		PrintStream mcodePS = new PrintStream(new FileOutputStream(
				this.fileHandler.getFile(MCODE_BLOCK)));
		writeCreateBlock(mcodePS, ioHandlers.size());
		mcodePS.close();

		PrintStream guiPS = new PrintStream(new FileOutputStream(
				this.fileHandler.getFile(GUI_XML)));
		writeGUI(guiPS);
		guiPS.close();

		PrintStream declPS = new PrintStream(new FileOutputStream(
				this.fileHandler.getFile(DECL_XML)));
		writeDeclarations(declPS);
		declPS.close();

		PrintStream apiPS = new PrintStream(new FileOutputStream(
				this.fileHandler.getFile(API_CXX)));
		writeAPI(apiPS, ioHandlers);
		apiPS.close();

		PrintStream modelPS = new PrintStream(new FileOutputStream(
				this.fileHandler.getFile(MODEL_CXX)));
		writeModelAPI(modelPS, ioHandlers, funcName);
		modelPS.close();
	}

	/**
	 * Returns a unique string which identifies this engine.
	 */
	public String getOutputPhaseId() {
		return "Generation of System Generator core files";
	}

	/**
	 * Generate the creatBlock file
	 * 
	 * @param ps
	 *            a <code>PrintStream</code> value
	 * @param numPorts
	 *            an <code>int</code> value, the number of ports on the block
	 *            (ports NOT pins)
	 */
	private void writeCreateBlock(PrintStream ps, int numPorts) {
		int blockHeight = 25 * numPorts;

		ps.println("function createBlock()");
		ps.println("");
		ps.println("  Blk = xlGetDefaultBlockSettings;");
		ps.println("");
		ps.println("  Blk.name = '" + this.baseName + "';");
		ps.println("");
		ps.println("  Blk.config_dll_name = '" + this.baseName + "';");
		ps.println("  Blk.config_dll_entry_point = '" + this.configName + "';");
		ps.println("");
		ps.println("  Blk.gui_xml = '"
				+ this.fileHandler.getFile(GUI_XML).getName() + "';");
		ps.println("  Blk.declarations_xml = '"
				+ this.fileHandler.getFile(DECL_XML).getName() + "';");
		ps.println("");
		ps.println("  Blk.height = " + blockHeight + ";");
		ps.println("  ");
		ps.println("  Blk");
		ps.println("");
		ps.println("  xlCreateBlock('" + this.libName + "', [200 100], Blk);");
		ps.println("");
		ps.println("  return;");
	}

	private void writeGUI(PrintStream ps) {
		final String label = "Actor Model -- " + this.baseName;
		final String runDate = EngineThread.getGenericJob()
				.getOption(OptionRegistry.RUN_DATE)
				.getValue(CodeLabel.UNSCOPED).toString();
		ps.println("<!--  *            -->");
		ps.println("<!DOCTYPE blockgui SYSTEM \"blockgui.dtd\">");
		ps.println("<blockgui label=\"" + label + "\" empty=\"true\">");
		ps.println("    <editbox name=\"infoedit\" read_only=\"true\" multi_line=\"true\" default=\"This block is the System Generator&tm; model of a core designed in the Actor/Dataflow network  methodology. &lt;P&gt;Design "
				+ label
				+ "&lt;P&gt;Generated on: "
				+ runDate
				+ " .\" evaluate=\"false\"/>");
		ps.println("  <hiddenvar name=\"sim_language\" default=\"1\" evaluate=\"true\"/>");
		ps.println("  <hiddenvar name=\"has_advanced_control\" default=\"0\" evaluate=\"true\"/>");
		ps.println("  <hiddenvar name=\"block_config\" default=\"myDLL:"
				+ this.configName + "\" evaluate=\"false\"/>");
		ps.println("  <hiddenvar name=\"serialized_declarations\" default=\"DECLARATIONS_XTABLE_GOES_HERE\" evaluate=\"false\"/>");
		ps.println("  <hiddenvar name=\"sggui_pos\" default=\"-1,-1,-1,-1\" evaluate=\"false\"/>");
		ps.println("  <hiddenvar name=\"block_type\" default=\""
				+ this.baseName + "\" evaluate=\"false\"/>");
		ps.println("  <hiddenvar name=\"sg_icon_stat\" default=\"ICON_STAT_GOES_HERE\" evaluate=\"false\"/>");
		ps.println("  <hiddenvar name=\"sg_mask_display\" default=\"\" evaluate=\"false\"/>");
		ps.println("  <hiddenvar name=\"sg_list_contents\" default=\"\" evaluate=\"false\"/>");
		ps.println("</blockgui>");
	}

	private void writeDeclarations(PrintStream ps) {
		ps.println("<dictionary>");
		ps.println("  <entry key=\"sim_language\" value=\"Int\"/>");
		ps.println("  <entry key=\"block_config\" value=\"String\"/>");
		ps.println("</dictionary>");
	}

	private void writeAPI(PrintStream ps, List<IOHandler> ioHandlers) {
		ps.println("/*");
		ps.println(" *  " + this.baseName + ".cxx");
		ps.println(" *");
		ps.println(" *  Copyright (c) 2006, Xilinx, Inc. All rights reserved.");
		ps.println(" *");
		ps.println(" *  Description: Configuration API for an Actor/Dataflow model.");
		ps.println(" */");
		ps.println("");
		ps.println("#pragma warning(disable : 4251)");
		ps.println("");
		ps.println("// Xilinx inclusions:");
		ps.println("#include \"sysgen/BlockDescriptor.h\"");
		ps.println("#include \"sysgen/BlockUtil.h\"");
		ps.println("//#include \"sysgen/MCodeUtil.h\"");
		ps.println("");
		// ps.println("// Standard Library inclusions:");
		// ps.println("#include <string>");
		// ps.println("#include <sstream>");
		// ps.println("");
		ps.println("");
		ps.println("namespace Sysgen");
		ps.println("{");
		ps.println("  /// \\ingroup public_example");
		ps.println("  /// \\brief Block Descriptor for a basic block");
		ps.println("  class " + this.baseName + ": public BlockDescriptor");
		ps.println("  {");
		ps.println("  public:");
		ps.println("    " + this.baseName + "();");
		ps.println("    virtual ~" + this.baseName + "();");
		ps.println("    ");
		ps.println("    virtual void configureInterface();");
		ps.println("    virtual void configureRateAndType();");
		ps.println("    virtual void configurePostRateAndType();");
		ps.println("    virtual void configureSimulation();");
		ps.println("    virtual void configureNetlist();");
		ps.println("  private:");

		// Write the PortDescriptor handles, eg:
		// ps.println("    PortDescriptor *x, *y, *o;");
		// ps.println("	PortDescriptor *xack, *xsnd, *yack, *ysnd, *oack, *osnd;");
		for (IOHandler ioh : ioHandlers) {
			ioh.writeHandleDecl(ps, "PortDescriptor", "PortDescriptor");
		}

		ps.println("  };");
		ps.println("};");
		ps.println("");
		ps.println("Sysgen::" + this.baseName + "::" + this.baseName + "()");
		ps.println("{");
		ps.println("}");
		ps.println("Sysgen::" + this.baseName + "::~" + this.baseName + "()");
		ps.println("{");
		ps.println("}");
		ps.println("");
		ps.println("void");
		ps.println("Sysgen::" + this.baseName + "::configureInterface()");
		ps.println("{");
		ps.println("    addClk(\"CLK\", 1); // 1 sample period");
		ps.println("");

		for (IOHandler ioh : ioHandlers) {
			ioh.writePortDecl(ps);
		}

		ps.println("");
		ps.println("    /* Unknown");
		ps.println("     * IDM FIXME.  is setMinLatency needed?");
		ps.println("    setMinLatency(\"din\", \"dout\", 1);");
		ps.println("    setMinLatency(\"addr\", \"dout\", 0);");
		ps.println("    */");
		ps.println("");
		ps.println("    setIconText(\"\\bf{ADF: " + this.baseName + "}\");");
		ps.println("    setIconBackgroundColor(\"#C8C8A8\");");
		ps.println("    setIconWatermarkColor(\"#BA9\");");
		ps.println("}");
		ps.println("");
		ps.println("void");
		ps.println("Sysgen::" + this.baseName + "::configureRateAndType()");
		ps.println("{");

		for (IOHandler ioh : ioHandlers) {
			ioh.writeRateAndType(ps);
		}

		// ps.println("	// IDM set rate to unknown.");
		// ps.println("	o->setRate(x->getRate());");
		// ps.println("	osnd->setRate(x->getRate());");
		// ps.println("	xack->setRate(x->getRate());");
		// ps.println("	yack->setRate(y->getRate());");
		ps.println("}");
		ps.println("");
		ps.println("void");
		ps.println("Sysgen::" + this.baseName + "::configurePostRateAndType()");
		ps.println("{");
		ps.println("    /* Type checking needed? */");
		ps.println("}");
		ps.println("");
		ps.println("void");
		ps.println("Sysgen::" + this.baseName + "::configureSimulation()");
		ps.println("{");
		ps.println("    PTable & mask = getPTable();");
		ps.println("");
		ps.println("    if (mask.getInt(\"sim_language\") == 1) {");
		ps.println("        setSimulationType(EXTERNAL_DOUBLE);");
		ps.println("        mask.set(\"simulation_model\", \"" + this.baseName
				+ ":" + this.modelName + "\");");
		ps.println("    }");
		ps.println("    else {");
		ps.println("      addError(\"MCode simulation not supported for this model\");");
		ps.println("    }");
		ps.println("}");
		ps.println("");
		ps.println("void");
		ps.println("Sysgen::" + this.baseName + "::configureNetlist()");
		ps.println("{");
		final File synFile = this.fileHandler
				.getFile(VerilogTranslateEngine.SYNPRIMINCL);
		final File hdlFile = this.fileHandler
				.getFile(VerilogTranslateEngine.VERILOG);
		ps.println("    addFile(\""
				+ synFile.getName()
				+ "\"); // copies the specified file INTO the generated HDL 'in place'");
		ps.println("    addFile(\""
				+ hdlFile.getName()
				+ "\"); // copies the specified file INTO the generated HDL 'in place'");
		ps.println("    setHDLName(\"" + this.baseName + "\");");
		ps.println("    suggestEntityName(\"" + this.baseName.toLowerCase()
				+ "\");");
		ps.println("    setTopLevelLanguage(Sysgen::VERILOG);");
		ps.println("}");
		ps.println("");
		ps.println("");
		ps.println("// The " + this.baseName
				+ "_config symbol comes from the created block, via ");
		ps.println("// the .mdl file (which is created with the "
				+ this.fileHandler.getFile(MCODE_BLOCK).getName() + " mcode");
		ps.println("EXPORT_SYSGEN_FACTORY (" + this.configName + ", Sysgen::"
				+ this.baseName + ");");
	}

	private void writeModelAPI(PrintStream ps, List<IOHandler> ioHandlers,
			String funcName) {
		// final String simHeader =
		// this.fileHandler.getFile(CycleCTranslateEngine.HEADER).getName();
		final String simHeader = this.fileHandler.getFile(
				CycleCTranslateEngine.SOURCE).getName();
		final String modelClassName = this.baseName + "_Model";

		ps.println("/*");
		ps.println(" *  " + modelClassName + ".cxx");
		ps.println(" *");
		ps.println(" *  Copyright (c) 2006, Xilinx, Inc. All rights reserved.");
		ps.println(" *");
		ps.println(" *  Description: Interface between System Generator(tm) simulation API and an Actor simulation model.");
		ps.println(" */");
		ps.println("");
		ps.println("#pragma warning(disable : 4251)");
		ps.println("");
		ps.println("#include \"sysgen/ExternalSimulationModel.h\"");
		// ps.println("#include <cmath>");
		// ps.println("");
		// ps.println("#include <string>");
		// ps.println("#include <sstream>");
		ps.println("");
		ps.println("");
		ps.println("extern \"C\" {");
		// ps.println("  #include \""+simHeader+"\"");
		ps.println("  #include \"" + simHeader + "\"");
		ps.println("}");
		ps.println("");
		ps.println("/// \\ingroup public_example");
		ps.println("/// \\brief External simulation model integration of an Actor");
		ps.println("class " + modelClassName
				+ " : public Sysgen::ExternalSimulationModel<double> ");
		ps.println("{");
		ps.println("public:");
		ps.println("    " + modelClassName + "(Sysgen::BlockDescriptor &bd);");
		ps.println("    ~" + modelClassName + "();");
		ps.println("    ");
		ps.println("    void updateState();");
		ps.println("    void updateOutputs() const;");
		ps.println("private:");
		// Write the declarations of vars which hold the current
		// values of the ports of the model (for sysgen) eg:
		// ps.println("  const double *x, *y, *xsnd, *ysnd, *oack;");
		// ps.println("  double *o, *xack, *yack, *osnd;");
		// ps.println("  int xHandle, yHandle, oHandle;");

		for (IOHandler ioh : ioHandlers) {
			ioh.writeHandleDecl(ps, "const double", "double");
			ioh.writeCoreHandleDecl(ps);
		}

		ps.println("};");
		ps.println("");
		ps.println("");
		ps.println("" + modelClassName + "::" + modelClassName
				+ "(Sysgen::BlockDescriptor &bd) :");
		ps.println("    Sysgen::ExternalSimulationModel<double>(bd)");
		ps.println("{");
		ps.println("");

		for (IOHandler ioh : ioHandlers) {
			ioh.writeHandleInit(ps);
		}

		ps.println("");
		ps.println("}");
		ps.println("");
		ps.println("" + modelClassName + "::~" + modelClassName + "()");
		ps.println("{");
		ps.println("}");
		ps.println("");
		ps.println("");
		ps.println("void " + modelClassName + "::updateState()");
		ps.println("{");
		ps.println("  " + funcName + "_clockEdge();");
		ps.println("");

		for (IOHandler ioh : ioHandlers) {
			ioh.writeOutputUpdate(ps);
		}

		ps.println("}");
		ps.println("");
		ps.println("void " + modelClassName + "::updateOutputs() const");
		ps.println("{");

		for (IOHandler ioh : ioHandlers) {
			ioh.writeInputUpdate(ps);
		}

		ps.println("");
		ps.println("    " + funcName + "_update();");
		ps.println("  ");

		for (IOHandler ioh : ioHandlers) {
			ioh.writeOutputUpdate(ps);
		}

		ps.println("}");
		ps.println("");
		ps.println("");
		ps.println("extern \"C\" { ");
		ps.println("    __declspec(dllexport)");
		ps.println("    void* (" + this.modelName
				+ ")(Sysgen::BlockDescriptor &bd) ");
		ps.println("    { return (void*)(new " + modelClassName + "(bd)); }");
		ps.println("}");
		ps.println("");
	}

	private static class IOHandler {
		private String base;
		private boolean input;
		private int width;

		IOHandler(FifoIF fifoIF) {
			base = fifoIF.getPortBaseName();
			input = fifoIF.isInput();
			width = fifoIF.getWidth();
		}

		private String getDataName() {
			return base + (this.input ? "_din" : "_dout");
		}

		private String getSendName() {
			return base + "_snd";
		}

		private String getAckName() {
			return base + "_ack";
		}

		private String getHandleName() {
			return base + "Handle";
		}

		void writeHandleDecl(PrintStream ps, String inType, String outType) {
			String in = this.input ? inType : outType;
			String out = this.input ? outType : inType;
			ps.println("    // Port " + base);
			ps.println("    " + in + " *" + getDataName() + ", *"
					+ getSendName() + ";");
			ps.println("    " + out + " *" + getAckName() + ";");
		}

		void writeCoreHandleDecl(PrintStream ps) {
			ps.println("    int " + getHandleName() + ";");
		}

		void writePortDecl(PrintStream ps) {
			if (this.input) {
				ps.println("    " + getDataName() + " = addInport(\""
						+ getDataName() + "\", \"" + getDataName() + "\");");
				ps.println("    " + getSendName() + " = addInport(\""
						+ getSendName() + "\", \"" + getSendName() + "\");");
				ps.println("    " + getAckName() + " = addOutport(\""
						+ getAckName() + "\", \"" + getAckName() + "\");");
			} else {
				ps.println("    " + getDataName() + " = addOutport(\""
						+ getDataName() + "\", \"" + getDataName() + "\");");
				ps.println("    " + getSendName() + " = addOutport(\""
						+ getSendName() + "\", \"" + getSendName() + "\");");
				ps.println("    " + getAckName() + " = addInport(\""
						+ getAckName() + "\", \"" + getAckName() + "\");");
			}
		}

		void writeRateAndType(PrintStream ps) {
			ps.println("  // Set type to be fixed point, precision equal to port width, no binary point.");
			if (this.input) {
				ps.println("    "
						+ getAckName()
						+ "->setType(Sysgen::SysgenType(Sysgen::SysgenType::xlBOOL, 1, 0));");
				// Set the ACK rate to equal the data rate
				ps.println("    " + getAckName() + "->setRate(" + getDataName()
						+ "->getRate());");
			} else {
				ps.println("    "
						+ getDataName()
						+ "->setType(Sysgen::SysgenType(Sysgen::SysgenType::xlFIX, "
						+ this.width + ", 0));");
				ps.println("    " + getDataName() + "->setRate("
						+ SysgenSimApi.CORE_OUTPUT_RATE + ");");
				ps.println("    "
						+ getSendName()
						+ "->setType(Sysgen::SysgenType(Sysgen::SysgenType::xlBOOL, 1, 0));");
				ps.println("    " + getSendName() + "->setRate("
						+ SysgenSimApi.CORE_OUTPUT_RATE + ");");
			}
		}

		void writeHandleInit(PrintStream ps) {
			if (this.input) {
				ps.println("  " + getDataName() + "  = getInputPtr(\""
						+ getDataName() + "\");");
				ps.println("  " + getSendName() + " = getInputPtr(\""
						+ getSendName() + "\");");
				ps.println("  " + getAckName() + " = getOutputPtr(\""
						+ getAckName() + "\");");
				ps.println("  " + getHandleName() + " = getInterfaceID(\""
						+ this.base + "\");");
			} else {
				ps.println("  " + getDataName() + " = getOutputPtr(\""
						+ getDataName() + "\");");
				ps.println("  " + getAckName() + " = getInputPtr(\""
						+ getAckName() + "\");");
				ps.println("  " + getSendName() + " = getOutputPtr(\""
						+ getSendName() + "\");");
				ps.println("  " + getHandleName() + " = getInterfaceID(\""
						+ this.base + "\");");
			}
		}

		void writeOutputUpdate(PrintStream ps) {
			ps.println("    // Drive the output ports based on the calculated values");
			if (this.input) {
				ps.println("    *" + getAckName() + " = (unsigned)isAcking("
						+ getHandleName() + ");");
			} else {
				ps.println("    *" + getDataName()
						+ " = (unsigned)getDataValue(" + getHandleName()
						+ ", (int)*" + getAckName() + ");");
				ps.println("    *" + getSendName() + " = (unsigned)isSending("
						+ getHandleName() + ");");
			}
		}

		void writeInputUpdate(PrintStream ps) {
			ps.println("    // Set the input port states from the System Generator environment");
			if (this.input) {
				ps.println("    setDataValue(" + getHandleName() + ", (int)*"
						+ getDataName() + ", (int)*" + getSendName() + ");");
			} else {
				ps.println("    getDataValue(" + getHandleName() + ", (int)*"
						+ getAckName() + ");  // just to set the ack");
			}
		}

	}

}
