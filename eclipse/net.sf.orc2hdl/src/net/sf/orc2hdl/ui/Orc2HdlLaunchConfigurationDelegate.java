/*
 * Copyright (c) 2011, EPFL
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
 *   * Neither the name of the EPFL nor the names of its
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
package net.sf.orc2hdl.ui;

import static net.sf.orc2hdl.LaunchConstants.FORGE_COMB_LUT_MEM_READ;
import static net.sf.orc2hdl.LaunchConstants.FORGE_DP_LUT;
import static net.sf.orc2hdl.LaunchConstants.FORGE_EDK;
import static net.sf.orc2hdl.LaunchConstants.FORGE_LOOP_BAL;
import static net.sf.orc2hdl.LaunchConstants.FORGE_MUL_DECOMP_LIMIT;
import static net.sf.orc2hdl.LaunchConstants.FORGE_NO_BLOCK;
import static net.sf.orc2hdl.LaunchConstants.FORGE_NO_BLOCK_SCHED;
import static net.sf.orc2hdl.LaunchConstants.FORGE_NO_INCLUDE;
import static net.sf.orc2hdl.LaunchConstants.FORGE_NO_LOG;
import static net.sf.orc2hdl.LaunchConstants.FORGE_PIPELINE;
import static net.sf.orc2hdl.LaunchConstants.FORGE_SIM_ARB;
import static net.sf.orc2hdl.LaunchConstants.FORGE_VERBOSE;
import static net.sf.orc2hdl.LaunchConstants.FPGA_TYPE;
import static net.sf.orc2hdl.LaunchConstants.OUTPUT_FOLDER;
import static net.sf.orc2hdl.LaunchConstants.PROJECT;
import static net.sf.orc2hdl.LaunchConstants.SYNC_FIFO;
import static net.sf.orc2hdl.LaunchConstants.XDF_FILE;

import java.util.ArrayList;
import java.util.List;

import net.sf.orc2hdl.Activator;
import net.sf.orc2hdl.cli.Synthesizer;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.model.ILaunchConfigurationDelegate;

/**
 * @author Endri Bezati
 * 
 */
public class Orc2HdlLaunchConfigurationDelegate implements
		ILaunchConfigurationDelegate {

	@Override
	public void launch(ILaunchConfiguration configuration, String mode,
			ILaunch launch, IProgressMonitor monitor) throws CoreException {
		Orc2HdlProcess process = new Orc2HdlProcess(launch, configuration,
				monitor);

		monitor.subTask("Launching Orc2HDL");
		process.writeText("\n");
		process.writeText("*********************************************"
				+ "**********************************\n");
		process.writeText("Launching Orc2HDL...\n");
		process.writeText("*********************************************"
				+ "**********************************\n");
		launch.addProcess(process);
		try {
			String project = configuration.getAttribute(PROJECT, "");
			String xdf = configuration.getAttribute(XDF_FILE, "");

			String outputFolder = configuration.getAttribute(OUTPUT_FOLDER, "");

			int fpgaType = configuration.getAttribute(FPGA_TYPE, 2);
			String fpgaName = new String();

			Boolean openForgeVerbose = configuration.getAttribute(
					FORGE_VERBOSE, true);
			Boolean openForgePipeline = configuration.getAttribute(
					FORGE_PIPELINE, true);
			Boolean openForgeNoBlockIO = configuration.getAttribute(
					FORGE_NO_BLOCK, true);
			Boolean openForgeNoBlockSched = configuration.getAttribute(
					FORGE_NO_BLOCK_SCHED, true);
			Boolean openForgeSimArb = configuration.getAttribute(FORGE_SIM_ARB,
					true);
			Boolean openForgeNoEDK = configuration
					.getAttribute(FORGE_EDK, true);
			Boolean openForgeLoopBal = configuration.getAttribute(
					FORGE_LOOP_BAL, true);
			Boolean openForgeMulDecompLimit = configuration.getAttribute(
					FORGE_MUL_DECOMP_LIMIT, true);
			Boolean openForgeCombLutMemRead = configuration.getAttribute(
					FORGE_COMB_LUT_MEM_READ, true);
			Boolean openForgeDpLUT = configuration.getAttribute(FORGE_DP_LUT,
					true);
			Boolean openForgeNoLog = configuration.getAttribute(FORGE_NO_LOG,
					true);
			Boolean openForgeNoInclude = configuration.getAttribute(
					FORGE_NO_INCLUDE, true);

			// For the moment only three types of FPGA are supported,
			// Future versions of openForge will support more of them
			if (fpgaType == 0) {
				fpgaName = "xc3s5000-5-fg1156";
			} else if (fpgaType == 1) {
				fpgaName = "xc2vp30-7-ff1152";
			} else if (fpgaType == 2) {
				fpgaName = "xc4vlx100-10-ff1513";
			}

			Boolean syncFifo = configuration.getAttribute(SYNC_FIFO, false);

			// Create the forgeFlags String
			List<String> forgeFlags = new ArrayList<String>();

			if (openForgeVerbose) {
				forgeFlags.add("-vv");
			}

			if (openForgePipeline) {
				forgeFlags.add("-pipeline");
			}

			if (openForgeNoBlockIO) {
				forgeFlags.add("-noblockio");
			}

			if (openForgeNoBlockSched) {
				forgeFlags.add("-no_block_sched");
			}

			if (openForgeSimArb) {
				forgeFlags.add("-simple_arbitration");
			}

			if (openForgeNoEDK) {
				forgeFlags.add("-noedk");
			}

			if (openForgeLoopBal) {
				forgeFlags.add("-loopbal");
			}

			if (openForgeMulDecompLimit) {
				forgeFlags.add("-multdecomplimit");
				forgeFlags.add("2");
			}

			if (openForgeCombLutMemRead) {
				forgeFlags.add("-comb_lut_mem_read");
			}

			if (openForgeDpLUT) {
				forgeFlags.add("-dplut");
			}

			if (openForgeNoLog) {
				forgeFlags.add("-nolog");
			}

			if (openForgeNoInclude) {
				forgeFlags.add("-noinclude");
			}

			Synthesizer synthesizer = new Synthesizer(project, xdf,
					outputFolder, forgeFlags, fpgaName, syncFifo);
			synthesizer.setProgressMonitor(monitor);
			synthesizer.setWriteListener(process);
			synthesizer.synthesize();

			process.writeText("\n");
			process.writeText("Orc2HDL generation done.");
		} catch (Exception e) {
			monitor.setCanceled(true);
			IStatus status = new Status(IStatus.ERROR, Activator.PLUGIN_ID,
					"Orc2HDL could not generate code", e);
			throw new CoreException(status);

		} finally {
			process.terminate();
			monitor.done();
		}
	}

}
