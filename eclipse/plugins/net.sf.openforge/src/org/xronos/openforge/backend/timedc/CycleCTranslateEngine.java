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

package org.xronos.openforge.backend.timedc;

import org.xronos.openforge.app.EngineThread;
import org.xronos.openforge.app.ForgeFileHandler;
import org.xronos.openforge.app.ForgeFileKey;
import org.xronos.openforge.backend.OutputEngine;
import org.xronos.openforge.lim.Design;

/**
 * CycleCTranslateEngine is the implementation of {@link OutputEngine} that
 * controls the location and generation of the Cycle accurate c simulation model
 * of the design.
 * 
 * <p>
 * Created: Fri Mar 17 20:50:55 2006
 * 
 * @author imiller, last modified by $Author: imiller $
 * @version $Id: CycleCTranslateEngine.java 112 2006-03-21 15:41:57Z imiller $
 */
public class CycleCTranslateEngine implements OutputEngine {

	public static final ForgeFileKey HEADER = new ForgeFileKey(
			"C sim model header");
	public static final ForgeFileKey SOURCE = new ForgeFileKey(
			"C sim model source");
	public static final ForgeFileKey VPGEN_HEADER = new ForgeFileKey(
			"C sim model vp compliant header");
	public static final ForgeFileKey VPGEN_WRAPPER = new ForgeFileKey(
			"C sim model vp compliant wrapper");

	@Override
	public void initEnvironment() {
		ForgeFileHandler fileHandler = EngineThread.getGenericJob()
				.getFileHandler();
		fileHandler.registerFile(HEADER, fileHandler.buildName("_sim", "h"));
		fileHandler.registerFile(SOURCE, fileHandler.buildName("_sim", "c"));
		fileHandler.registerFile(VPGEN_HEADER,
				fileHandler.buildName("_vp", "h"));
		fileHandler.registerFile(VPGEN_WRAPPER,
				fileHandler.buildName("_vp", "c"));
	}

	@Override
	public void translate(Design design) {
		ForgeFileHandler fileHandler = EngineThread.getGenericJob()
				.getFileHandler();

		try {
			CycleCTranslator.translate(design, fileHandler.getFile(HEADER),
					fileHandler.getFile(SOURCE));
		} catch (CycleCTranslator.CycleCTranslatorException ccte) {
			EngineThread.getGenericJob().error(
					"Could not generate a cycle-c model due to internal error: "
							+ ccte.getMessage());
		}

	}

	/**
	 * Returns a string which uniquely identifies this phase of the compiler
	 * output.
	 * 
	 * @return a non-empty, non-null String
	 */
	@Override
	public String getOutputPhaseId() {
		return "Cycle-accurate C model";
	}

}
