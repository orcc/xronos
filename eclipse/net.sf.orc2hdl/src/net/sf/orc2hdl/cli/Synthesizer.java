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

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.sf.openforge.app.Forge;
import net.sf.orcc.OrccException;
import net.sf.orcc.backends.xlim.XlimBackendImpl;
import net.sf.orcc.network.Instance;
import net.sf.orcc.network.Network;
import net.sf.orcc.network.serialize.XDFParser;

import org.eclipse.core.runtime.FileLocator;
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
				flags.addAll(Arrays.asList("-d", outputFolder, "-o", id, xlim));
				Forge.runForge((String[]) flags.toArray(new String[0]));
			}
		}
	}

	private Network network;

	private String inputXDF;

	private List<String> vtlFolders;

	private String outputFolder;

	private List<String> forgeFlags;

	private String fpgaType;

	private Boolean syncFifo;

	public Synthesizer(String inputXDF, List<String> vtlFolders,
			String outputFolder, List<String> forgeFlags, String fpgaType,
			Boolean syncFifo) {
		this.inputXDF = inputXDF;
		this.vtlFolders = vtlFolders;
		this.outputFolder = outputFolder;
		this.forgeFlags = forgeFlags;
		this.fpgaType = fpgaType;
		this.syncFifo = syncFifo;

		init();
	}

	private void init() {
		try {

			network = new XDFParser(inputXDF).parseNetwork();
			network.updateIdentifiers();
			network.instantiate(vtlFolders);
			network.flatten();

		} catch (Exception e) {
			e.printStackTrace();
		}
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

	public void synthesize() throws OrccException, IOException {

		XlimBackendImpl xlim = new XlimBackendImpl();
		xlim.compileVTL(null, vtlFolders);

		// Create Output folder for the XLIM cache files and the destination
		// folder for openForge
		String openForgeFolder = outputFolder + File.separator + "src";
		new File(openForgeFolder).mkdir();

		String xlimFolder = outputFolder + File.separator + "xlim";
		new File(xlimFolder).mkdir();

		// Set the XLIM Back-end to a HW XLIM code generation
		xlim.setHardwareGen(true);

		// Set the FPGA type
		xlim.setFpgaType(fpgaType);

		// Set the Output folder for the XLIM Back-end
		xlim.setOutputFolder(xlimFolder);

		// Write the flatten XDF to the XLIM generated output folder
		// new XDFWriter(new File(XDFfilename), network);
		xlim.compileXDF(null, inputXDF);

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
		URL hdlLibrariesURL = Platform.getBundle("net.sf.orc2hdl")
				.getEntry("/HdlLibraries");

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

	public static void main(String[] args) throws OrccException, IOException {

		final String[] defaultForgeFlags = { "-vv", "-pipeline", "-noblockio",
				"-no_block_sched", "-simple_arbitration", "-noedk", "-loopbal",
				"-multdecomplimit", "2", "-comb_lut_mem_read", "-dplut",
				"-nolog", "-noinclude" };

		if (args.length == 3) {
			String inputXDF = args[0];
			List<String> vtlFolders = Arrays.asList(args[1]
					.split(File.pathSeparator));
			String outputFolder = args[2];
			// The default execution should not contain syncFIFOs and should
			// generate HW XLIM for a Virtex 2
			new Synthesizer(inputXDF, vtlFolders, outputFolder,
					new ArrayList<String>(Arrays.asList(defaultForgeFlags)),
					"xc2vp30-7-ff1152", false).synthesize();

		} else {
			System.err.println("ORCC OpenForge Backend, usage:"
					+ Synthesizer.class.getSimpleName()
					+ " <input XDF network> <VTL folder> <output folder>");
		}

	}
}
