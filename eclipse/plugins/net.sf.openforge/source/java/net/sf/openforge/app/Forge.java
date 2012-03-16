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

import java.io.File;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import net.sf.openforge.app.project.ForgeProjectWriter;
import net.sf.openforge.app.project.Option;
import net.sf.openforge.app.project.OptionBoolean;
import net.sf.openforge.app.project.OptionList;
import net.sf.openforge.frontend.xlim.app.XLIMEngine;
import net.sf.openforge.lim.CodeLabel;
import net.sf.openforge.util.ForgeDebug;
import net.sf.openforge.util.Tee;
import net.sf.openforge.util.license.License;

/**
 * Forge is the main command-line entry point for the Forge compiler.
 * 
 * 
 * Created: Mon Mar 18 16:14:04 2002
 * 
 * @author <a href="mailto:abk@ladd">Andy Kollegger</a>
 * @version $Id: Forge.java 557 2008-03-14 03:29:48Z imiller $
 */

public class Forge implements ForgeDebug {

	// //////////////////////////////////////
	// instance fields

	PrintWriter out = new Tee(System.out, true);
	PrintWriter err = new Tee(System.err, true);

	// //////////////////////////////////////
	// constructors

	/**
	 * The no-arg constructor
	 */
	public Forge() {
	} // Forge()

	public void preprocess(GenericJob gj) {
		// Check if the user requested license info before checking
		// if the license is valid. This will allow them to debug
		// their license install without having to have a valid license.
		// The info returned will be if the license file was found, what
		// is in it, and what the expiration data is for the license.

		// Option option;

		if (gj.getUnscopedBooleanOptionValue(OptionRegistry.LICENSE)) {
			msgln(License.getLicenseInfo());
		}

		// //// This is where the license check goes...
		// boolean validLic = true;

		if (gj.getUnscopedBooleanOptionValue(OptionRegistry.VERSION)) {
			msgln("Version: " + Version.versionNumber());
		}

		if (gj.getUnscopedBooleanOptionValue(OptionRegistry.NOTES)) {
			msgln("Release Notes:\n" + Release.releaseNotes());
		}

		if (gj.getUnscopedBooleanOptionValue(OptionRegistry.HELP)) {
			msgln(OptionRegistry.usage(false));
			throw new ForgeFatalException("Help message generated");
			// System.exit(0);
		}

		if (gj.getUnscopedBooleanOptionValue(OptionRegistry.HELP_DETAIL)) {
			msgln(OptionRegistry.usage(true));
			throw new ForgeFatalException("Detailed help message generated");
			// System.exit(0);
		}

		if (gj.getUnscopedBooleanOptionValue(OptionRegistry.X_WRITE_CYCLE_C_VPGEN)) {
			if (!gj.getUnscopedBooleanOptionValue(OptionRegistry.WRITE_CYCLE_C)) {
				msgln(OptionRegistry.X_WRITE_CYCLE_C_VPGEN
						.getHelpFormattedKeyList()
						+ " requires option "
						+ OptionRegistry.WRITE_CYCLE_C
								.getHelpFormattedKeyList() + " to be set true");
				// System.exit(0);
				throw new ForgeFatalException(
						"Cycle accurate C VPGen requires generation of cycle accurate C");
			}
		}

		updateOptions(gj);

		checkOptions(gj);

	} // preprocess()

	/**
	 * Certain options may need to modify other options based on the way that
	 * they are set, this method accomplishes that task.
	 */
	private void updateOptions(NewJob job) {
		// Turn off the forge.log output if NOLOG was specified.
		if (job.getUnscopedBooleanOptionValue(OptionRegistry.NOLOG)) {
			// get the LOG option and re-set its value after having
			// cleared the forge.log entry.
			final OptionList optionList = (OptionList) job
					.getOption(OptionRegistry.LOG);
			final List<String> modified = new ArrayList<String>();
			for (Iterator<?> iter = optionList.getValueAsList(
					CodeLabel.UNSCOPED).iterator(); iter.hasNext();) {
				final String nextValue = (String) iter.next();
				if (!nextValue.startsWith("forge.log")) {
					modified.add(nextValue);
				}
			}
			optionList.replaceValue(CodeLabel.UNSCOPED,
					OptionList.toString(modified));
		}

		SimpleDateFormat df = new SimpleDateFormat("EEE d MMM yyyy HH:mm:ss Z");
		String runDate = df.format(new Date());
		job.getOption(OptionRegistry.RUN_DATE).replaceValue(CodeLabel.UNSCOPED,
				runDate);
	}

	/**
	 * Certain options expect that other options are set in certain ways. This
	 * method accomplishes that task.
	 */
	public static void checkOptions(GenericJob job) {
		Option option;

		option = job.getOption(OptionRegistry.NO_BLOCK_IO);
		boolean noblockio = ((OptionBoolean) option)
				.getValueAsBoolean(CodeLabel.UNSCOPED);
		option = job.getOption(OptionRegistry.MODULE_BUILDER);
		boolean module_builder = ((OptionBoolean) option)
				.getValueAsBoolean(CodeLabel.UNSCOPED);
		option = job.getOption(OptionRegistry.CHANNEL_DESCRIPTOR);
		boolean channel_descriptor = !option.getValue(CodeLabel.UNSCOPED)
				.equals("null");

		// Check to see that -noblockio is specified with -Xmodulebuilder
		if (module_builder && !noblockio) {
			job.fatalError("-Xmodule_builder requires -noblockio flag");
		}

		// check to see that -noblockio has either a -channel_descriptor or a
		// -Xmodule_builder
		File[] inputFiles = job.getTargetFiles();
		if (noblockio && !ForgeFileTyper.isXLIMSource(inputFiles[0].getName())) {
			if (channel_descriptor && module_builder) {
				job.fatalError("-channel_descriptor and -Xmodule_builder are mutually exclusive");
			}

			if (!(channel_descriptor || module_builder)) {
				job.fatalError("-noblockio requires one of -channel_descriptor or -Xmodule_builder");
			}
		}

		if (job.getUnscopedBooleanOptionValue(OptionRegistry.SYSGEN)) {
			if (!job.getUnscopedBooleanOptionValue(OptionRegistry.WRITE_CYCLE_C)) {
				job.fatalError("Option '"
						+ OptionRegistry.SYSGEN.getHelpFormattedKeyList()
						+ "' requires that the option '"
						+ OptionRegistry.WRITE_CYCLE_C
								.getHelpFormattedKeyList() + "' be specified");
			}
		}
	}

	/**
	 * <code>compile</code> starts the compilation process.
	 * 
	 * @return <code>true</code> on fatal error during compilation,
	 *         <code>false</code> otherwise.
	 */
	public boolean compile(GenericJob gj) {
		// if(_gapp) gapp.ln("Compile() called: "+projects.size());
		if (_gapp)
			gapp.ln("Compile() called: ");

		boolean exitError = false;
		Option option;
		// The logger update must occur after the updateOptions method
		// so that the NOLOG option can take effect
		gj.updateLoggers();
		Engine engine = null;
		File[] srcFiles = gj.getTargetFiles();

		// Proceed further with creating a CEngine only if there is atleast one
		// source file.
		if (srcFiles.length > 0) {
			File targetFile = gj.getTargetFiles()[0];

			if (ForgeFileTyper.isJavaSource(targetFile.getName())) {
				gj.fatalError("Java is not supported as an input source code language");
			} else if (ForgeFileTyper.isXLIMSource(targetFile.getName())) {
				engine = new XLIMEngine(gj);
			} else {
				assert false;
			}

			gj.info("OpenForge " + Version.versionNumber());
			gj.info("");
			gj.info("Starting compilation for target: "
					+ targetFile.getAbsolutePath());

			option = gj.getOption(OptionRegistry.CWD);
			String s = option.getValue(CodeLabel.UNSCOPED).toString();
			gj.info("      Current Working Directory: " + s);

			// File dest = gj.getDestination();
			// gj.info("          Destination Directory: " +
			// dest.getAbsolutePath());

			List<String> incList = gj.getIncludeDirList();
			gj.info("        Include files Directory: ");
			for (int i = 0; i < incList.size(); i++) {
				gj.info("            " + incList.get(i).toString());
			}

			gj.info("");

			Monitor engineMonitor = new Monitor(engine);
			engine.addJobListener(engineMonitor);

			try {
				engine.begin();
				gj.decAll();
				gj.info("");
				gj.info("Forge compilation complete");
			} catch (ForgeFatalException e) {
				exitError = true;
				gj.decAll();
				gj.error("");
				gj.error("Forge compilation ended with fatal error:");
				gj.error(e.getMessage());
			} catch (RecursiveMethodException e) {
				exitError = true;
				gj.decAll();
				gj.error("");
				gj.error("Forge did not compile target: "
						+ targetFile.getAbsolutePath());
			} catch (XflowException e) {
				exitError = true;
				gj.decAll();
				gj.error("");
				gj.error("");
				gj.error(e.getMessage());
				gj.error("");
				gj.error("Forge did not finish xflow for target: "
						+ targetFile.getAbsolutePath());
			} catch (Throwable t) {
				exitError = true;

				StringBuffer traceBuf = new StringBuffer();
				traceBuf.append("Stack trace:");
				StackTraceElement[] trace = t.getStackTrace();
				for (int i = 0; i < trace.length; i++) {
					traceBuf.append("\n");
					traceBuf.append(trace[i]);
				}

				gj.decAll();
				gj.inc();

				gj.error("Forge compilation ended with fatal internal error: "
						+ t.toString() + "\n" + traceBuf.toString());

				gj.decAll();
			}
		}

		boolean proj_file = gj
				.getUnscopedBooleanOptionValue(OptionRegistry.PROJ_FILE);
		boolean proj_file_desc = gj
				.getUnscopedBooleanOptionValue(OptionRegistry.PROJ_FILE_DESC);

		// check for creation of custom project, then make and output one
		if (proj_file || proj_file_desc) {
			try {
				String projectName = GenericJob.DEFAULT_FILENAME;
				File[] compileTargets = gj.getTargetFiles();
				if (compileTargets.length > 0) {
					if (compileTargets[0].exists()) {
						projectName = compileTargets[0].toString();
					}
				}
				projectName = projectName.substring(0,
						projectName.lastIndexOf('.'));
				projectName += ".forge";
				File proj = new File(projectName);
				if (!proj.exists()) {
					// proj = new File(System.getProperty("user.dir") +
					// File.separator + projectName);
					proj = new File(projectName);
				}
				ForgeProjectWriter fpw = new ForgeProjectWriter(proj, gj,
						engine, proj_file_desc);
				fpw.write();
			} catch (Exception e) {
				errorln("Failed to create custom project because -- "
						+ e.toString());
			}
		}

		if (engine != null)
			engine.kill(); // notify this engine it is dead...
		System.gc(); // do gc in an orderly fashion -- between forging jobs

		return exitError;
	} // compile()

	/**
	 * Set the PrintWriter to use for messages directed to stdout.
	 * 
	 * @param writer
	 *            the non-null stdout PrintWriter
	 */
	public void setStdOut(PrintWriter writer) {
		if (writer != null)
			out = writer;
	} // setStdOut()

	/**
	 * Set the PrintWriter to use for messages directed to stderr.
	 * 
	 * @param writer
	 *            the non-null stderr PrintWriter
	 */
	public void setStdErr(PrintWriter writer) {
		if (writer != null)
			err = writer;
	} // setStdErr()

	private void errorln(String text) {
		err.println(text);
	}

	private void msgln(String text) {
		out.println(text);
	}

	// /////////////////////////////////////////
	// utility inner class to handle JobListner

	public class Monitor implements JobListener {
		Engine eng;

		public Monitor(Engine e) {
			this.eng = e;
		}

		public void jobNotice(JobEvent je) {
			eng.getGenericJob().info(
					eng.getCurrentHandler().getStageName() + " "
							+ je.getMessage());
		}

	} // inner class Monitor()

	@SuppressWarnings("unused")
	public static void main(String[] args) {
		String[] forgeFlags = { "-vv", // be verbose
				"-pipeline", // Allow auto-insertion of registers based on
								// max-gate-depth spec in the XLIM (simply turns
								// feature on)
				"-noblockio", // Do not automatically infer fifo interfaces
								// from top level function signature (legacy C
								// interface feature)
				"-no_block_sched", // Do not perform block-based scheduling
									// (auto s/w pipelining of top level tasks)
				"-simple_arbitration", // Use simple arbitration of shared
										// memories (assumes logic handles
										// contention)
				"-noedk", // Do not generate EDK pcore compliant directory
							// output structure.
				"-loopbal", // Balance loop latency. Ensures that all paths
							// take at least 1 cycle if any path does so that
							// loop iteration flop can be removed.
				"-multdecomplimit", "2", // Any multiplier which can be
											// decomposed into 2 or fewer
											// add/subtract + shift stages is.
				"-comb_lut_mem_read", // Reads of LUT based memories are
										// performed combinationally.
				"-nolog", // No log file generation
				"-dplut", // Allow generation of dual ported LUT memories
							// (default is to only use dual port BRAMs)
				"-noinclude", // Suppress generation of _sim and _synth files
		};

		List<String> finalArgs = new ArrayList<String>();

		finalArgs.addAll(Arrays.asList(forgeFlags));
		for (String argument : args) {
			finalArgs.add(argument);
		}
		Forge f = new Forge();
		GenericJob forgeMainJob = new GenericJob();
		boolean exitError = true;

		try {
			forgeMainJob.setOptionValues((String[]) finalArgs
					.toArray(new String[0]));
			f.preprocess(forgeMainJob);
			exitError = f.compile(forgeMainJob);
		} catch (NewJob.ForgeOptionException foe) {
			f.msgln("Command line option error: " + foe.getMessage());
			f.msgln("");
			f.msgln(OptionRegistry.usage(false));
			System.exit(-1);
		} catch (ForgeFatalException ffe) {
			f.msgln("Forge compilation ended with fatal error:");
			f.msgln(ffe.getMessage());
			System.exit(-1);
		}

//		if (!runForge(args)) {
//			System.exit(-1);
//		} else {
//			System.exit(0);
//		}

	} // main()

	public static boolean runForge(String[] args) {

		Forge f = new Forge();
		GenericJob forgeMainJob = new GenericJob();
		boolean error = true;
		System.out.println("OpenForge Synthesizer");
		try {
			forgeMainJob.setOptionValues(args);
			f.preprocess(forgeMainJob);
			error = f.compile(forgeMainJob);
		} catch (NewJob.ForgeOptionException foe) {
			f.msgln("Command line option error: " + foe.getMessage());
			f.msgln("");
			f.msgln(OptionRegistry.usage(false));
			error = true;
		} catch (ForgeFatalException ffe) {
			f.msgln("Forge compilation ended with fatal error:");
			f.msgln(ffe.getMessage());
			error = true;
		}

		return !error;
	}

}// Forge

