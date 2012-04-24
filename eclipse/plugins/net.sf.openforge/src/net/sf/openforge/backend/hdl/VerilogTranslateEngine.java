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

package net.sf.openforge.backend.hdl;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import net.sf.openforge.app.EngineThread;
import net.sf.openforge.app.ForgeFileHandler;
import net.sf.openforge.app.ForgeFileKey;
import net.sf.openforge.app.GenericJob;
import net.sf.openforge.app.OptionRegistry;
import net.sf.openforge.backend.OutputEngine;
import net.sf.openforge.backend.edk.ForgeCoreDescriptor;
import net.sf.openforge.lim.Design;
import net.sf.openforge.verilog.translate.VerilogNaming;
import net.sf.openforge.verilog.translate.VerilogTranslator;

/**
 * VerilogTranslateEngine is the {@link OutputEngine} for translation of the
 * design to Verilog HDL.
 * 
 * <p>
 * Created: Fri Mar 17 20:50:55 2006
 * 
 * @author imiller, last modified by $Author: imiller $
 * @version $Id: VerilogTranslateEngine.java 424 2007-02-26 22:36:09Z imiller $
 */
public class VerilogTranslateEngine implements OutputEngine {

	/** The Verilog HDL output */
	public static final ForgeFileKey VERILOG = new ForgeFileKey("Verilog HDL");
	/**
	 * An additional Verilog HDL output which simply includes the main design
	 * and the simulation primitives
	 */
	public static final ForgeFileKey SIMINCL = new ForgeFileKey(
			"Verilog HDL simulation includes");
	/**
	 * An additional Verilog HDL output which simply includes the main design
	 * and the synthesis primitives
	 */
	public static final ForgeFileKey SYNINCL = new ForgeFileKey(
			"Verilog HDL synthesis includes");
	/**
	 * An HDL output file which contains ONLY the synthesis primitives and does
	 * NOT contain an inclusion of the generated HDL file.
	 */
	public static final ForgeFileKey SYNPRIMINCL = new ForgeFileKey(
			"Verilog HDL synthesis includes");

	@Override
	public void initEnvironment() {
		ForgeFileHandler fileHandler = EngineThread.getGenericJob()
				.getFileHandler();
		final boolean suppressIncludes = EngineThread.getGenericJob()
				.getUnscopedBooleanOptionValue(OptionRegistry.NO_INCLUDE_FILES);
		if (fileHandler.isRegistered(ForgeCoreDescriptor.EDK_HDL_VER_DIR)) {
			File base = fileHandler
					.getFile(ForgeCoreDescriptor.EDK_HDL_VER_DIR);
			fileHandler.registerFile(VERILOG, base,
					fileHandler.buildName("", "v"));
			if (!suppressIncludes) {
				fileHandler.registerFile(SIMINCL, base,
						fileHandler.buildName("_sim", "v"));
				fileHandler.registerFile(SYNINCL, base,
						fileHandler.buildName("_synth", "v"));
			}
		} else {
			fileHandler.registerFile(VERILOG, fileHandler.buildName("", "v"));
			if (!suppressIncludes) {
				fileHandler.registerFile(SIMINCL,
						fileHandler.buildName("_sim", "v"));
				fileHandler.registerFile(SYNINCL,
						fileHandler.buildName("_synth", "v"));
			}
			// The synthesis primitives include file will be
			// registered by some other process(es) if it is needed.
			// fileHandler.registerFile(SYNPRIMINCL,
			// fileHandler.buildName("_synincl","v"));
		}

	}

	/**
	 * Causes the synthesis primitive (only) include file to be registered, and
	 * consequently, to be generated during the translate phase. This method is
	 * safe to be called multiple times.
	 */
	public static void registerSynPrimIncl() {
		final ForgeFileHandler fileHandler = EngineThread.getGenericJob()
				.getFileHandler();
		if (!fileHandler.isRegistered(SYNPRIMINCL)) {
			fileHandler.registerFile(SYNPRIMINCL,
					fileHandler.buildName("_synincl", "v"));
		}
	}

	@Override
	public void translate(Design design) throws IOException {
		GenericJob gj = EngineThread.getGenericJob();
		ForgeFileHandler fileHandler = gj.getFileHandler();

		final File vFile = fileHandler.getFile(VERILOG);

		// ABK - first, run the VerilogNaming visitor to correct any naming
		// problems
		// which may have cropped up
		VerilogNaming naming = new VerilogNaming();
		naming.visit(design);

		gj.info("writing " + vFile.getAbsolutePath());
		gj.inc();

		// Don't open the outputstreams until just before we are going
		// to use them. That way we don't create the file in the case
		// where the translator throws an exception.
		final boolean suppressAppModule = gj
				.getUnscopedBooleanOptionValue(OptionRegistry.SUPPRESS_APP_MODULE);
		VerilogTranslator vt = new VerilogTranslator(design, suppressAppModule);

		if (!vFile.getParentFile().exists()) {
			vFile.getParentFile().mkdirs();
		}
		FileOutputStream vFos = new FileOutputStream(vFile);

		vt.writeDocument(vFos);
		vFos.close();

		if (fileHandler.isRegistered(SIMINCL)) {
			FileOutputStream simFos = new FileOutputStream(
					fileHandler.getFile(SIMINCL));
			vt.outputSimInclude(vFile, simFos);
			simFos.close();
		}

		if (fileHandler.isRegistered(SYNINCL)) {
			FileOutputStream synthFos = new FileOutputStream(
					fileHandler.getFile(SYNINCL));
			vt.outputSynthInclude(vFile, synthFos);
			synthFos.close();
		}

		if (fileHandler.isRegistered(SYNPRIMINCL)) {
			FileOutputStream synPrimFos = new FileOutputStream(
					fileHandler.getFile(SYNPRIMINCL));
			vt.outputSynthInclude(null, synPrimFos);
			synPrimFos.close();
		}

		gj.dec();

	}

	/**
	 * Returns a string which uniquely identifies this phase of the compiler
	 * output.
	 * 
	 * @return a non-empty, non-null String
	 */
	@Override
	public String getOutputPhaseId() {
		return "Verilog HDL";
	}
}
