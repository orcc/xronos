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
package net.sf.orc2hdl.cli;

import static net.sf.orcc.OrccLaunchConstants.OUTPUT_FOLDER;
import static net.sf.orcc.OrccLaunchConstants.PROJECT;
import static net.sf.orcc.OrccLaunchConstants.XDF_FILE;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sf.openforge.app.Forge;
import net.sf.orcc.OrccException;
import net.sf.orcc.backends.Backend;
import net.sf.orcc.backends.xlim.XlimBackendImpl;
import net.sf.orcc.network.Instance;
import net.sf.orcc.network.Network;
import net.sf.orcc.network.serialize.XDFParser;
import net.sf.orcc.util.OrccUtil;
import net.sf.orcc.util.WriteListener;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Platform;

/**
 * @author Endri Bezati
 * 
 */
public class Synthesizer {

	private class OpenForgeBackend {

		private List<String> xlimFolders;

		private String outputFolder;

		private Network network;

		private List<String> forgeFlags;

		public OpenForgeBackend(List<String> xlimFolders, String outputFolder,
				Network network, List<String> forgeFlags) {
			this.xlimFolders = xlimFolders;
			this.outputFolder = outputFolder;
			this.network = network;
			this.forgeFlags = forgeFlags;
		}

		public void synthesize() {
			listener.writeText("\n");
			listener.writeText("*********************************************"
					+ "**********************************\n");

			listener.writeText("Orc2HDL: Launching OpenForge");
			listener.writeText("\n");
			listener.writeText("*********************************************"
					+ "**********************************\n");

			int totalInstances = network.getInstances().size();

			listener.writeText("Instances to compile: " + totalInstances);
			listener.writeText("\n");

			int countInstance = 0;
			// Start Timer
			long t0 = System.currentTimeMillis();
			
			for (Instance instance : network.getInstances()) {
				List<String> flags = new ArrayList<String>(forgeFlags);
				String id = instance.getId();
				String xlim = null;
				try {
					for (String folder : xlimFolders) {
						File file = new File(folder + File.separator + id
								+ ".xlim");
						if (file.exists()) {
							xlim = file.getCanonicalPath();
							break;
						}
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
				countInstance++;
				listener.writeText("\n");
				listener.writeText("Compiling instance " + countInstance + "/"
						+ totalInstances + " \t: " + id);
				flags.addAll(Arrays.asList("-d", outputFolder, "-o", id, xlim));
				Forge.runForge((String[]) flags.toArray(new String[0]));
			}
			//Stop Timer
			long t1 = System.currentTimeMillis();
			listener.writeText("\n\n");
			listener.writeText("Done in " + ((float) (t1 - t0) / (float) 1000) + "s\n");
		}

	}

	private Network network;

	private String inputXDF;

	private String outputFolder;

	private List<String> forgeFlags;

	private String fpgaType;

	private Boolean syncFifo;

	private IProject project;

	private String projectName;

	private WriteListener listener;

	private IProgressMonitor monitor;

	private IFile xdfFile;

	public Synthesizer(String project, String inputXDF, String outputFolder,
			List<String> forgeFlags, String fpgaType, Boolean syncFifo) {
		this.projectName = project;
		this.inputXDF = inputXDF;
		this.outputFolder = outputFolder;
		this.forgeFlags = forgeFlags;
		this.fpgaType = fpgaType;
		this.syncFifo = syncFifo;
		init();
	}

	private void init() {
		try {
			// Get Project
			IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
			project = root.getProject(projectName);

			// Parse the network
			xdfFile = OrccUtil.getFile(project, inputXDF, "xdf");
			network = new XDFParser(xdfFile).parseNetwork();
			network.updateIdentifiers();
			network.instantiate(OrccUtil.getOutputFolders(project));
			Network.clearActorPool();
			network.flatten();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void createOutputFolder() {
		try {
			outputFolder = outputFolder + File.separator + network.getName();
			new File(outputFolder).mkdir();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private String createFolderInOutputFolder(String folderName) {
		try {
			String folder = outputFolder + File.separator + folderName;
			new File(folder).mkdir();
			return folder;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public void CopyFile(String InputFile, String OutputFile)
			throws IOException {
		File iFile = new File(InputFile);
		File oFile = new File(OutputFile);

		FileReader in = new FileReader(iFile);
		FileWriter out = new FileWriter(oFile);
		int c;

		while ((c = in.read()) != -1)
			out.write(c);

		in.close();
		out.close();
	}

	public void setProgressMonitor(IProgressMonitor monitor) {
		this.monitor = monitor;
	}

	/**
	 * 
	 * @param listener
	 */
	public void setWriteListener(WriteListener listener) {
		this.listener = listener;
	}

	public void synthesize() throws OrccException, IOException {

		// Create output Folder
		createOutputFolder();

		// Get Project Information
		Map<String, Object> options = new HashMap<String, Object>();
		options.put(PROJECT, projectName);
		options.put(XDF_FILE, inputXDF);
		options.put(OUTPUT_FOLDER, createFolderInOutputFolder("xlim"));
		options.put("net.sf.orcc.backends.xlimFpgaType", fpgaType);
		options.put("net.sf.orcc.backends.multi2mono", true);
		// Institiate XLIM HW Backend
		Backend backend = new XlimBackendImpl();
		backend.setProgressMonitor(monitor);
		backend.setWriteListener(listener);
		backend.setOptions(options);
		// backend.compileVTL();

		listener.writeText("*********************************************"
				+ "**********************************\n");
		listener.writeText("Orc2HDL: Launching ORCC");
		listener.writeText("\n");
		listener.writeText("*********************************************"
				+ "**********************************\n");
		backend.compileXDF();

		// Create Output folder for the XLIM cache files and the destination
		// folder for openForge
		String openForgeFolder = outputFolder + File.separator + "src";
		new File(openForgeFolder).mkdir();

		String xlimFolder = outputFolder + File.separator + "xlim";
		new File(xlimFolder).mkdir();

		// Copy the Generated Top VHDL from xlim folder

		CopyFile(xlimFolder + File.separator + network.getName() + ".vhd",
				openForgeFolder + File.separator + network.getName() + ".vhd");

		// Call OpenForgeBackend
		new OpenForgeBackend(Arrays.asList(xlimFolder), openForgeFolder,
				network, forgeFlags).synthesize();

		// Copy systemBuilder to the Generated folder
		String systemBuilderFolder = outputFolder + File.separator
				+ "systemBuilder";
		new File(systemBuilderFolder).mkdir();

		List<String> systemBuilderFifo = new ArrayList<String>();

		if (syncFifo) {
			systemBuilderFifo = Arrays.asList("sbtypes.vhdl",
					"sbsyncfifo_behavioral.vhdl", "sbsyncfifo.vhdl");
		} else {
			systemBuilderFifo = Arrays.asList("sbtypes.vhdl",
					"sbfifo_behavioral.vhdl", "sbfifo.vhdl");
		}

		// Get the current folder
		URL hdlLibrariesURL = Platform.getBundle("net.sf.orc2hdl").getEntry(
				"/HdlLibraries");

		String hdlLibrariesPath = new File(FileLocator.resolve(hdlLibrariesURL)
				.getFile()).getAbsolutePath();

		// Copy files to the systemBuilder Generated folder
		for (String files : systemBuilderFifo) {
			String inputFile = new String(hdlLibrariesPath + File.separator
					+ "systemBuilder" + File.separator + "vhdl"
					+ File.separator + files);
			String outputFile = new String(systemBuilderFolder + File.separator
					+ files);

			CopyFile(inputFile, outputFile);
		}
	}
}
