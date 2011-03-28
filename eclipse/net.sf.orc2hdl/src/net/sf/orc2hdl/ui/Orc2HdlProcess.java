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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import net.sf.orc2hdl.cli.Synthesizer;
import net.sf.orcc.util.OrccUtil;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.IStreamListener;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.core.model.IStreamMonitor;
import org.eclipse.debug.core.model.IStreamsProxy;

/**
 * This class defines the CAL2HDL Process
 * 
 * @author Endri Bezati
 * 
 */
public class Orc2HdlProcess extends PlatformObject implements IProcess {

	/**
	 * This class defines an implementation of stream monitor.
	 * 
	 * @author Matthieu Wipliez
	 * 
	 */
	private class Cal2HdlMonitor implements IStreamMonitor {

		private ListenerList list;

		/**
		 * Creates a new monitor.
		 */
		public Cal2HdlMonitor() {
			list = new ListenerList();
		}

		@Override
		public void addListener(IStreamListener listener) {
			list.add(listener);
		}

		@Override
		public String getContents() {
			synchronized (contents) {
				return contents;
			}
		}

		@Override
		public void removeListener(IStreamListener listener) {
			list.remove(listener);
		}

		/**
		 * Writes the given text to the contents watched by this monitor.
		 * 
		 * @param text
		 *            a string
		 */
		private void write(String text) {
			synchronized (contents) {
				contents += text;
			}

			for (Object listener : list.getListeners()) {
				((IStreamListener) listener).streamAppended(text, this);
			}
		}

	}

	private class Orc2HdlProxy implements IStreamsProxy {

		private IStreamMonitor errorMonitor;

		private IStreamMonitor outputMonitor;

		public Orc2HdlProxy() {
			errorMonitor = new Cal2HdlMonitor();
			outputMonitor = new Cal2HdlMonitor();
		}

		@Override
		public IStreamMonitor getErrorStreamMonitor() {
			return errorMonitor;
		}

		@Override
		public IStreamMonitor getOutputStreamMonitor() {
			return outputMonitor;
		}

		@Override
		public void write(String input) throws IOException {
			// nothing to do
		}

	}

	private ILaunchConfiguration configuration;

	private String contents;

	private ILaunch launch;

	private IProgressMonitor monitor;

	private IStreamsProxy proxy;

	private boolean terminated;

	public Orc2HdlProcess(ILaunch launch, ILaunchConfiguration configuration,
			IProgressMonitor monitor) throws CoreException {
		this.configuration = configuration;
		this.launch = launch;
		this.monitor = monitor;
		contents = "";
		proxy = new Orc2HdlProxy();
	}

	@Override
	public boolean canTerminate() {
		return !terminated;
	}

	@Override
	public String getAttribute(String key) {
		return null;
	}

	@Override
	public int getExitValue() throws DebugException {
		return 0;
	}

	@Override
	public String getLabel() {
		return configuration.getName();
	}

	@Override
	public ILaunch getLaunch() {
		return launch;
	}

	public IProgressMonitor getProgressMonitor() {
		return monitor;
	}

	@Override
	public IStreamsProxy getStreamsProxy() {
		return proxy;
	}

	@Override
	public boolean isTerminated() {
		return terminated;
	}

	@Override
	public void setAttribute(String key, String value) {
	}

	public void start() throws CoreException {
		try {
			write("Launching Open-RVC CAL to HDL ...\n");
			launchSynthesizer();
			write("Orcc backend done.");
		} finally {
			terminated = true;
			DebugEvent event = new DebugEvent(this, DebugEvent.TERMINATE);
			DebugEvent[] events = { event };
			DebugPlugin.getDefault().fireDebugEventSet(events);
		}
	}

	private void launchSynthesizer() throws CoreException {
		try {
			List<String> forgeFlags = new ArrayList<String>();

			String xdf = configuration.getAttribute(XDF_FILE, "");
			String output = configuration.getAttribute(OUTPUT_FOLDER, "");
			String name = configuration.getAttribute(PROJECT, "");

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

			IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
			IProject project = root.getProject(name);

			List<String> vtlFolders = OrccUtil.getOutputPaths(project);

			new Synthesizer(xdf, vtlFolders, output, forgeFlags, fpgaName,
					syncFifo).synthesize();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void terminate() throws DebugException {
	}

	/**
	 * Writes the given text to the normal output of this process.
	 * 
	 * @param text
	 *            a string
	 */
	public void write(String text) {
		((Cal2HdlMonitor) proxy.getOutputStreamMonitor()).write(text);
	}

}
