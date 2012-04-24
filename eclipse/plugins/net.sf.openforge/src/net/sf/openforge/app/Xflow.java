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
package net.sf.openforge.app;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;

import net.sf.openforge.app.logging.ForgeLogger;
import net.sf.openforge.app.project.Option;
import net.sf.openforge.app.project.OptionBoolean;
import net.sf.openforge.app.project.OptionMultiFile;
import net.sf.openforge.lim.CodeLabel;
import net.sf.openforge.lim.Design;
import net.sf.openforge.util.Environment;
import net.sf.openforge.util.XilinxDevice;

/**
 * Xflow
 * 
 * 
 * Created: Fri Sep 27 11:46:04 2002
 * 
 * @author Conor Wu
 * @version $Id: Xflow.java 112 2006-03-21 15:41:57Z imiller $
 */

public class Xflow {

	File forge_synth_file;
	File destination_dir;
	String xilinx_env;
	boolean env_valid;
	boolean linuxWine;
	String partstring;
	String synthtypestring;
	String targetspeedstring;
	String implementstring;
	String tsimstring;
	Map<String, Object> pin_map;
	Option op;

	boolean run_synth = true;
	boolean run_implement = true;
	boolean run_tsim = true;
	boolean run_config = true;
	boolean run_script = false;

	boolean run_ok = true;

	ForgeLogger logger;

	/**
	 * this is the prefix passed when running with wine (XIL_LINUX_WINE env is
	 * 1)
	 */
	static String[] wineCommandPrefix = { "--debugmsg", "fixme-all", "--" };

	/** save the most recently exec'ed command line for getCommandLine() */
	String commandLine;

	/** The LIM design. */
	Design design;

	public Xflow(Design design, File forge_synth_file, File destination_dir,
			String speed_target_override) {
		GenericJob gj = EngineThread.getGenericJob();
		this.design = design;

		logger = gj.getLogger();

		this.forge_synth_file = forge_synth_file;

		if (destination_dir == null) {
			// derive the destination directory from the synth file
			this.destination_dir = forge_synth_file.getParentFile();
		} else {
			this.destination_dir = destination_dir;
		}

		// Assume everything is OK, until we detect otherwise
		env_valid = true;

		// First thing to check is the filenames, they
		// cannot contain spaces; if they do xflow will fail in
		// synthesis
		if ((this.forge_synth_file.getAbsolutePath().indexOf(' ') >= 0)
				|| (this.destination_dir.getAbsolutePath().indexOf(' ') >= 0)) {
			logger.error("");
			logger.error("    Cannot run xflow with file paths that contain spaces.");
			logger.error("    The Verilog file and working directory paths are:");
			logger.error("       "
					+ quote(this.forge_synth_file.getAbsolutePath()));
			logger.error("       "
					+ quote(this.destination_dir.getAbsolutePath()));
			logger.error("    Please relocate the files such that the paths don't contain");
			logger.error("    spaces and rerun Forge.");

			env_valid = false;
		}

		// Prior to doing anything, verify the user's environment
		// variable setting and the existance of the xflow command and
		// the verilog include files. Print warning if anything isn't
		// valid, along with what we were looking for.
		// Fetch the XILINX environment variable if set,
		// default to $XILINX otherwise

		Properties environmentProperties = Environment.getProperties();

		String xilinx_env_raw = environmentProperties.getProperty("XILINX", "")
				.trim();
		String xil_linux_wine_raw = environmentProperties.getProperty(
				"XIL_LINUX_WINE", "").trim();

		if (env_valid) {
			env_valid = (xilinx_env_raw.length() > 0);

			if (!env_valid) {
				// print error message
				logger.error("");
				logger.error("    Cannot run xflow without XILINX environment variable set.");
				logger.error("    Please set this variable to point to the directory that");
				logger.error("    contains your installation of the Xilinx implementation");
				logger.error("    tools such that the following programs exist:");

				String path;
				String extension;

				if (File.separatorChar == '/') {
					path = "$XILINX/bin/{platform}/";
					extension = "";
				} else {
					path = "$XILINX\\bin\\{platform}\\";
					extension = ".exe";
				}

				logger.info("       " + path + "xflow" + extension);
				logger.info("       " + path + "ngdbuild" + extension);
				logger.info("       " + path + "map" + extension);
				logger.info("       " + path + "par" + extension);
				logger.info("       " + path + "trce" + extension);
				logger.info("       " + path + "bitgen" + extension);
			}
		}

		xilinx_env = xilinx_env_raw;

		// trim any trailing file separators
		while ((xilinx_env.length() > 0)
				&& xilinx_env.endsWith(new String(File.separator))) {
			if (xilinx_env.length() > 1)
				xilinx_env = xilinx_env.substring(0, (xilinx_env.length() - 1));
			else
				xilinx_env = "";
		}

		// can only do the following checks when _not_ running under linux &
		// wine
		linuxWine = false;
		if (xil_linux_wine_raw != null && xil_linux_wine_raw.equals("1")) {
			linuxWine = true;
			logger.info("  You are running the Xilinx back end tools under wine.  We ");
			logger.info("  can not verify your XILINX env variable.  It should be set ");
			logger.info("  in the windows domain, ie: C:\\Xilinx");
		}

		if (!linuxWine) {
			// Check if the directory that XILINX points to looks like
			// what we expect. Specifically check for
			// bin,verilog,verilog/src/iSE/unisim_comp.v
			File x_bin = new File(xilinx_env, "bin");

			if (env_valid && !x_bin.isDirectory()) {
				// $XILINX/bin doesn't exist or isn't a directory!
				env_valid = false;

				logger.error("");
				logger.error("    Cannot run xflow, your XILINX environment variable");
				logger.error("    doesn't point to a valid installation of the Xilinx");
				logger.error("    implementation tools");
				logger.error("    $XILINX = " + xilinx_env_raw);

				if (!x_bin.exists())
					logger.error("       Couldn't find: "
							+ x_bin.getAbsolutePath());
				else
					logger.error("       " + x_bin.getAbsolutePath()
							+ " wasn't a directory");
			}

			File x_verilog = new File(xilinx_env, "verilog");
			File x_unisim = new File(xilinx_env,
					("verilog" + File.separator + "src" + File.separator
							+ "iSE" + File.separator + "unisim_comp.v"));

			if (env_valid && (!x_verilog.isDirectory() || !x_unisim.exists())) {
				// $XILINX/verilog doesn't exist or isn't a directory!
				env_valid = false;

				logger.error("");
				logger.error("    Cannot run xflow, your XILINX environment variable appears");
				logger.error("    to point to a valid installation of the Xilinx implementation");
				logger.error("    tools, but the verilog directory and its contents do not exist.");
				logger.error("    Please verify that you installed the verilog flows and rerun Forge");
			}
		}

		// OK, it looks like $XILINX is valid, is xflow in their path?
		// Try execing xflow with -help and see if the command is
		// found. If we get an IOException, then

		Process p;
		int exitValue = 0;
		List<String> command = new ArrayList<String>();

		if (env_valid) {
			try {
				if (linuxWine) {
					command.add("wine");
					for (int i = 0; i < wineCommandPrefix.length; i++) {
						command.add(wineCommandPrefix[i]);
					}
					command.add(xilinx_env + "\\bin\\nt\\xflow.exe");
				} else {
					command.add("xflow");
				}

				command.add("-help");

				p = (Runtime.getRuntime().exec(getCommandArray(command)));

				// create threads to drain the stderror and stdout of the
				// process
				Drain d1 = new Drain(p.getInputStream(), null);
				Drain d2 = new Drain(p.getErrorStream(), null);

				// Wait for everything to finish
				boolean done = false;

				while (!done) {
					try {
						exitValue = p.waitFor();

						// Yield so the two drain threads can die!
						while (d1.isAlive() && d2.isAlive()) {
							try {
								Thread.sleep(10);
							} catch (InterruptedException ie) {
							}
						}

						done = true;
						p.destroy();
					} catch (InterruptedException ie) {
					}
				}

			} catch (IOException ioe) {
				String execedString = " ";

				for (String string : command) {
					execedString += string + " ";
				}
				// Couldn't find xflow!
				logger.error("");
				logger.error("    Tried to exec " + execedString);
				logger.error("    error was: " + ioe.getMessage());
				logger.error("    Please verify that xflow is in your path and rerun Forge");

				env_valid = false;
			}
		}

		if (env_valid && (exitValue != 0)) {
			env_valid = false;
			logger.error("");
			String execedString = " ";

			for (String string : command) {
				execedString += string + " ";
			}
			logger.error("    " + execedString
					+ "completed with error status: " + exitValue
					+ ", your environment");
			logger.error("    appears to be configured incorrectly.  Please check that your path");
			logger.error("    points to the correct $XILINX/bin/{platform} directory of your Xilinx");
			logger.error("    implementation tools and that your XILINX environment variable is pointing");
			logger.error("    to the install directory, and on unix systems that your LD_LIBRARY_PATH environment");
			logger.error("    points to the $XILINX/bin/{platform} directory.  Linux and Wine environments should");
			logger.error("    set XIL_LINUX_WINE to 1 and set XILINX env variable as a dos string, ie: g:\\ise5.1i");
			logger.error("    instead of /export/path/to/ise5.1i.");
		}

		partstring = gj.getPart(CodeLabel.UNSCOPED).getFullDeviceName();

		op = gj.getOption(OptionRegistry.SYNTH_OPTION);
		synthtypestring = op.getValue(CodeLabel.UNSCOPED).toString();

		targetspeedstring = gj.getTargetSpeed();

		op = gj.getOption(OptionRegistry.IMPLEMENT_OPTION);
		implementstring = op.getValue(CodeLabel.UNSCOPED).toString();

		op = gj.getOption(OptionRegistry.TSIM_OPTION);
		tsimstring = op.getValue(CodeLabel.UNSCOPED).toString();

		if (speed_target_override != null)
			targetspeedstring = speed_target_override;

		if (partstring == null)
			partstring = "";

		if (synthtypestring == null)
			synthtypestring = "";

		if (targetspeedstring == null)
			targetspeedstring = "";

		if (implementstring == null)
			implementstring = "";

		if (tsimstring == null)
			tsimstring = "";

		if (env_valid && !(new XilinxDevice(partstring)).isFullySpecified()) {
			// The part number preference doesn't parse to family,
			// package, speed, and device.

			logger.warn("");
			logger.warn("    The Forge preference:");
			logger.warn("    xflow.part_number = " + partstring);
			logger.warn("    doesn't represent a valid family, device, speed, or package.");
			logger.warn("");
			logger.warn("    Defaulting to: xc2v8000-5-ff1152C");
			partstring = "xc2v8000-5-ff1152C";

			logger.warn("");
			logger.warn("    You can use the partgen program that comes with your ISE tools");
			logger.warn("    to get a list of legal part numbers for your installation.");
			logger.warn("");
			logger.warn("    Please fix for your next run of Forge");

			XilinxDevice[] xds = XilinxDevice.getKnownParts();
			logger.warn("");
			logger.warn("    An abbreviated list of known parts:");
			for (int i = 0; i < xds.length; i++) {
				logger.warn("           " + xds[i]);
			}

			logger.warn("");
		}

		if (partstring.length() > 0)
			partstring = (new XilinxDevice(partstring))
					.getFullDeviceNameNoTemp();

		if (env_valid && synthtypestring.length() == 0) {
			logger.warn("");
			logger.warn("    Forge preference project.synth_opt is blank,");
			logger.warn("    setting to: xst_verilog.opt");
			synthtypestring = "xst_veriog.opt";
		}

		if (env_valid && implementstring.length() == 0) {
			logger.warn("");
			logger.warn("    Forge preference project.implement_opt is blank,");
			logger.warn("    setting to: balanced.opt");
			implementstring = "balanced.opt";
		}

		if (env_valid && tsimstring.length() == 0) {
			logger.warn("");
			logger.warn("    Forge preference project.tsim_opt is blank,");
			logger.warn("    setting to: modelsim_verilog.opt");
			tsimstring = "modelsim_verilog.opt";
		}

		// lastly, check the speed target override supplied, if its
		// null and the speed target string is empty from the
		// preference, warn the user that we are running
		// unconstrained, and suggest using xflowbest or supplying a
		// speed preference.
		if (env_valid && (speed_target_override == null)
				&& (targetspeedstring.length() == 0)) {
			logger.warn("");
			logger.warn("    Forge preference project.target_speed is blank.");
			logger.warn("    This forces xflow to run unconstrained which can");
			logger.warn("    produce poor timing results.  Consider requesting");
			logger.warn("    a target speed using the preference or command line");
			logger.warn("    equivalent -speed.");
			logger.warn("    An example of the preference syntax is:");
			logger.warn("    project.target_speed 100 Mhz");
			logger.warn("");
		}

	}

	public Xflow(Design design, File forge_synth_file) {
		this(design, forge_synth_file, null, null);
	}

	public Xflow(Design design, File forge_synth_file, File destination_dir) {
		this(design, forge_synth_file, destination_dir, null);
	}

	public void runScript() {
		if (isEnvironmentValid()) {
			cleanBeforeRun();

			boolean run_script_save = run_script;

			// Force xflow to run in script mode, return to original
			// value when finished
			run_script = true;

			run();

			// Rename the script file so it doesn't get overriden by
			// subsequent runs of xflow
			File scrfile = new File(destination_dir, "xflow.bat");
			File outfile = null;

			if (scrfile.exists()) {
				outfile = new File(destination_dir, "xflow_full.bat");
				scrfile.renameTo(outfile);
			}

			scrfile = new File(destination_dir, "xflow.scr");
			if (scrfile.exists()) {
				outfile = new File(destination_dir, "xflow_full.scr");
				scrfile.renameTo(outfile);
			}

			if (outfile != null) {
				// tack on bitgen command to the full script.

				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				PrintWriter pw = new PrintWriter(baos);

				pw.println();
				pw.println(getBitgenCommandLineAsString());
				pw.println();
				pw.flush();
				pw.close();

				try {
					RandomAccessFile raf = new RandomAccessFile(outfile, "rw");

					raf.seek(raf.length());
					raf.write(baos.toByteArray());
					raf.close();
				} catch (Throwable t) {
					// oh well, couldn't append the info to the script
					logger.error("Couldn't append BitGen command line to file: "
							+ outfile.getAbsolutePath()
							+ " because: "
							+ t.getMessage());
				}
			}

			run_script = run_script_save;

			cleanAfterRun();
		}
	}

	public String getSubFileName() {
		return (forge_synth_file.getName()).substring(0,
				((forge_synth_file.getName()).lastIndexOf(".v")));
	}

	public void run() {
		if (isEnvironmentValid()) {
			cleanBeforeRun();

			GenericJob gj = EngineThread.getGenericJob();
			Option op;

			// write out ucf file in the working directory. Might need to create
			// the directory
			// too! If the design is unconstrained and a ucf file
			// already exists, don't do anything, if running
			// constrained, then move the current ucf to a backup and
			// write out our new one.

			String subFileName = getSubFileName();

			File ucfFile = new File(destination_dir, (subFileName + ".ucf"));
			op = gj.getOption(OptionRegistry.UCF_INCLUDES);
			List<String> ucf_list = ((OptionMultiFile) op)
					.getValueAsList(CodeLabel.UNSCOPED);

			if ((targetspeedstring.length() == 0) && (ucfFile.exists())
					&& (ucf_list.size() == 0)) {
				// Don't do anything, the user hasn't constrained the
				// design and a ucf file already exists and they
				// didn't give us any ucf files to include!
			} else {
				// Either we are constrained or there is no ucf file,
				// therefore create one, or the user gave us ucf files
				// to include in the file which means we always
				// generate the target ucf.

				try {
					DesignUCFDocument ucfDoc = new DesignUCFDocument();
					ucfDoc.initEnvironment();
					ucfDoc.translate(design);
				} catch (IOException ioe) {
					logger.error("");
					logger.error("    During generation of ucf file: "
							+ ioe.getMessage());
					run_ok = false;
				}

				/*
				 * // Create directories if necessary
				 * if(!destination_dir.exists()) destination_dir.mkdirs();
				 * 
				 * // Test if the ucf file already exists, and if so make //
				 * backup if(ucfFile.exists()) ucfFile.renameTo(new
				 * File(ucfFile.getAbsolutePath() + ".old"));
				 * 
				 * // OK, create our new UCF file try { FileOutputStream fos =
				 * new FileOutputStream(ucfFile); PrintWriter pw = new
				 * PrintWriter(fos);
				 * 
				 * DesignUCFDocument ucfDoc = new DesignUCFDocument(gj, design);
				 * 
				 * ucfDoc.write(pw); pw.close();
				 * 
				 * } catch(FileNotFoundException fnfe) { logger.error("");
				 * logger.error("    During generation of ucf file: " +
				 * fnfe.getMessage()); run_ok = false; }
				 */
			}

			// Check if a ucffile was given, and if supplied,
			// copy the one we just created to that name to.
			op = gj.getOption(OptionRegistry.UCF_FILE);
			String ucfFilePref = op.getValue(CodeLabel.UNSCOPED).toString()
					.trim();

			if (ucfFilePref.length() > 0) {
				byte[] buf = new byte[1024];

				BufferedInputStream bis = null;
				FileOutputStream fos = null;

				try {
					fos = new FileOutputStream(ucfFilePref);

					// open the file and copy its contents into
					// our new ucf file.
					bis = new BufferedInputStream(new FileInputStream(ucfFile));

					int bytesRead = bis.read(buf, 0, buf.length);

					while (bytesRead != -1) {
						if (bytesRead > 0)
							fos.write(buf, 0, bytesRead);

						bytesRead = bis.read(buf, 0, buf.length);
					}
				} catch (FileNotFoundException fnfe) {
					logger.error("");
					logger.error("    During generation of ucf file: "
							+ fnfe.getMessage());
					run_ok = false;
				} catch (IOException ioe) {
				}

				if (bis != null) {
					try {
						bis.close();
					} catch (IOException ioed) {
					}
				}

				if (fos != null) {
					try {
						fos.close();
					} catch (IOException ioed) {
					}
				}
			}

			// Delete the .xpi file if it exists
			File xpiFile = new File(destination_dir, (subFileName + ".xpi"));

			if (xpiFile.exists())
				xpiFile.delete();

			// Check if user requested only a ucf file and we are not
			// in script mode, if so we are
			// done.
			op = gj.getOption(OptionRegistry.UCF_ONLY);
			boolean isUcfOnly = ((OptionBoolean) op)
					.getValueAsBoolean(CodeLabel.UNSCOPED);
			if (isUcfOnly && !run_script)
				return;

			boolean xflow_error = true;
			List<String> command = new ArrayList<String>();

			try {
				// Setup and call the execution function, using the commandArray
				// to avoid the Windows issues of filenames with spaces in
				// them, since our commandLine String gets parsed if we use it
				// as the input to execution.

				if (linuxWine) {
					command.add("wine");
					for (int i = 0; i < wineCommandPrefix.length; i++) {
						command.add(wineCommandPrefix[i]);
					}
					command.add(xilinx_env + "\\bin\\nt\\xflow.exe");
				} else {
					command.add("xflow");
				}

				command = getCommandLineForExec(command);
				Process p;
				p = Runtime.getRuntime().exec(getCommandArray(command), null,
						this.destination_dir);

				int exitValue = 0;

				// create threads to drain the stderror and stdout of
				// the process
				Drain d1 = new Drain(p.getInputStream(), logger);
				Drain d2 = new Drain(p.getErrorStream(), logger);

				// Wait for everything to finish
				boolean done = false;

				while (!done) {
					try {
						exitValue = p.waitFor();

						if (exitValue == 0)
							xflow_error = false;

						// Yield so the two drain threads can die!
						while (d1.isAlive() && d2.isAlive()) {
							try {
								Thread.sleep(10);
							} catch (InterruptedException ie) {
							}
						}

						done = true;
						p.destroy();
					} catch (InterruptedException ie) {
					}
				}

			}

			catch (IOException ioe) {
				// Couldn't find xflow!
				String execedString = " ";
				for (String string : command) {
					execedString += string + " ";
				}

				logger.error("");
				logger.error("    Cannot exec " + execedString + ": "
						+ ioe.getMessage());
				logger.error("    Please verify that xflow is in your path,");
				logger.error("    your preferences are correct, and rerun Forge");
			}

			cleanAfterRun();

			if (xflow_error)
				run_ok = false;

			// Delete all the bitgen targets

			String[] extensions = { ".bgn", ".rbt", ".nky", ".ll", ".msk",
					".drc", ".bit" };

			for (int i = 0; i < extensions.length; i++) {
				File todel = new File(destination_dir,
						(subFileName + extensions[i]));
				todel.delete();
			}

			if (!run_script && !xflow_error & shouldRunBitgen()) {
				// Since we are not in script mode, run bitgen with
				// the command line options from the preference.

				// Step 1, parse the command line preference settings
				// for bitgen
				StringTokenizer st = new StringTokenizer(getBitgenOptions());

				command = new ArrayList<String>();

				if (linuxWine) {
					command.add("wine");
					for (int i = 0; i < wineCommandPrefix.length; i++) {
						command.add(wineCommandPrefix[i]);
					}
					command.add(xilinx_env + "\\bin\\nt\\bitgen.exe");
				} else {
					command.add("bitgen");
				}

				// copy in the command options from the bitgen_options
				// preference
				while (st.hasMoreTokens()) {
					command.add(st.nextToken());
				}

				// tag on the file name for bitgen to process, it will
				// assume a .ncd extension
				command.add(subFileName);

				logger.info("> " + getBitgenCommandLineAsString());

				// Make call to bitgen, but force a change directory
				// to the xflow directory, so the file can be found
				try {
					// Settup and call the exec function, using the commandArray
					// to avoid the Windows issues of filenames with spaces in
					// them, since our commandLine String gets parsed if we use
					// it
					// as the input to exec. The null input to exec
					// forces the execed process to inherit our
					// environment and the destination dir make that
					// the current working directory for the process.
					Process p = Runtime.getRuntime().exec(
							getCommandArray(command), null,
							this.destination_dir);
					int exitValue = 0;

					// create threads to drain the stderror and stdout of the
					// process
					Drain d1 = new Drain(p.getInputStream(), logger);
					Drain d2 = new Drain(p.getErrorStream(), logger);

					// Wait for everything to finish
					boolean done = false;

					while (!done) {
						try {
							exitValue = p.waitFor();

							// Yield so the two drain threads can die!
							while (d1.isAlive() && d2.isAlive()) {
								try {
									Thread.sleep(10);
								} catch (InterruptedException ie) {
								}
							}

							done = true;
							p.destroy();
						} catch (InterruptedException ie) {
						}
					}

					if (exitValue != 0)
						run_ok = false;

				} catch (IOException ioe) {
					// Couldn't find bitgen!
					String execedString = "";

					for (String string : command) {
						execedString += " " + string;
					}
					logger.error("");
					logger.error("    Cannot exec" + execedString + ": "
							+ ioe.getMessage());
					logger.error("    Please verify that bitgen is in your path,");
					logger.error("    your preferences are correct, and rerun Forge");
					run_ok = false;
				}

			}
		} else {
			// if environment wasn't valid, the run isn't either
			run_ok = false;
		}
	}

	private String getBitgenOptions() {
		Option op = EngineThread.getGenericJob().getOption(
				OptionRegistry.BITGEN_OPTIONS);
		return (op.getValue(CodeLabel.UNSCOPED).toString().trim());
	}

	private boolean shouldRunBitgen() {
		String bitgen_options = getBitgenOptions();

		return (!bitgen_options.equals("NO_RUN"));
	}

	private String[] getBitgenCommandLine() {
		String bitgen_options = getBitgenOptions();

		// Step 1, parse the command line preference settings
		// for bitgen
		StringTokenizer st = new StringTokenizer(bitgen_options);

		String[] args = new String[st.countTokens() + 2];

		// put in the command name
		args[0] = "bitgen";

		// copy in the command options from the bitgen_options preference
		for (int i = 1; i < (args.length - 1); i++)
			args[i] = st.nextToken();

		// tag on the file name for bitgen to process, it will
		// assume a .ncd extension
		args[args.length - 1] = getSubFileName();

		return args;
	}

	private String getBitgenCommandLineAsString() {
		String[] cl = getBitgenCommandLine();

		String result = "";

		for (int i = 0; i < cl.length; i++) {
			if (i > 0)
				result += " ";

			result += cl[i];
		}

		return result;
	}

	private void cleanBeforeRun() {
		// Xflow is picky if the previous run in a directory failed
		// place and route then the xflow.his file that is left
		// somehow corrupts the next runs of xflow. To prevent
		// problems we will delete the history file
		File histFile = new File(destination_dir, "xflow.his");

		histFile.delete();
	}

	private void cleanAfterRun() {
		// Xflow is picky if the previous run in a directory failed
		// place and route then the xflow.his file that is left
		// somehow corrupts the next runs of xflow. To prevent
		// problems we will delete the history file. We do this
		// after a run too since the user might want to go in by hand
		// an run things.
		File histFile = new File(destination_dir, "xflow.his");

		histFile.delete();
	}

	/**
	 * build an String array from the ArrayList Side effect of rebuilding field
	 * commandLine for getCommandLine() method
	 */
	private String[] getCommandArray(List<String> command) {
		String[] cmdLine = new String[command.size()];
		int i = 0;
		commandLine = "";
		for (String next : command) {
			cmdLine[i++] = next;
			commandLine += next + " ";
		}
		return cmdLine;
	}

	/**
	 * build up the command line arguments. Assumes xflow or xflow.exe are
	 * already in list
	 * 
	 * @param command
	 *            of String
	 * @return list with arguments added
	 */
	private List<String> getCommandLineForExec(List<String> command) {
		command.add("-p");
		command.add(partstring);
		if (run_synth) {
			command.add("-synth");
			command.add(synthtypestring);
		}

		if (run_implement) {
			command.add("-implement");
			command.add(implementstring);
		}

		if (run_tsim) {
			command.add("-tsim");
			command.add(tsimstring);
		}

		if (run_config) {
			command.add("-config");
			command.add("bitgen.opt");
		}

		if (run_script) {
			command.add("-norun");
		}

		command.add("-wd");
		command.add(this.destination_dir.getAbsolutePath());
		command.add(forge_synth_file.getAbsolutePath());

		return command;
	}

	public String getCommandLine() {
		return commandLine.trim();
	}

	public boolean isEnvironmentValid() {
		return env_valid;
	}

	public boolean runOK() {
		return run_ok;
	}

	public void setPinMap(Map<String, Object> hm) {
		this.pin_map = hm;
	}

	public void setRunSynth(boolean b) {
		run_synth = b;
	}

	public void setRunImplement(boolean b) {
		run_implement = b;
	}

	public void setRunTsim(boolean b) {
		run_tsim = b;
	}

	public void setRunConfig(boolean b) {
		run_config = b;
	}

	public void setRunScript(boolean b) {
		run_script = b;
	}

	private String quote(String s) {
		return ("\"" + s + "\"");
	}
}
