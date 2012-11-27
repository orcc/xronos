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

package org.xronos.openforge.util.exec;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;

/**
 * GCC is a class to interface with the GNU C Compiler. It executes the GNU C
 * compiler, {@link GCC}, on a given stream of input. The compiler is executed
 * in a separate process, and its standard output and error are redirected to
 * specified output streams.
 * <P>
 * direct execution, compile only and preprocessor-only methods are available.
 * <P>
 * formerly, it was expected that the compiler can be found in the user's path.
 * NOW gcc is expected to be in ForgeHome/gcc/bin/gcc This can be overriden by
 * defining the FORGE_GCC environment var.
 * 
 * @version $Id: GCC.java 2 2005-06-09 20:00:48Z imiller $
 */
public class GCC {
	/**
	 * The path of the GNU C Compiler - can't initialize statically because
	 * failure throws an exception, which screws up under obfuscation
	 */
	private static String gccExe;

	private String[] commandArgs = null;

	private int status = 1;

	/**
	 * Describe constructor here.
	 */
	public GCC() {
		gccExe = findExecutable();
	}

	/**
	 * Raw form. executes "gcc <command line>"
	 * 
	 * @param commandLine
	 *            a value of type 'String'
	 */
	public int exec(String[] args, OutputStream out, OutputStream err)
			throws java.io.IOException, ExecutionException {
		commandArgs = new String[args.length + 1];
		commandArgs[0] = gccExe;
		System.arraycopy(args, 0, commandArgs, 1, args.length);

		run(out, err);
		return exitValue();
	}

	/**
	 * Compile only. executes gcc -c <command line>
	 * 
	 * @param commandLine
	 *            a value of type 'String'
	 */
	public int compileOnly(String[] args, OutputStream out, OutputStream err)
			throws java.io.IOException, ExecutionException {
		/*
		 * Yeah, that's right, I did it, and I'm not sorry. --SGE
		 */
		if (System.getProperty("os.name").equals("Mac OS X")) {
			String[] newArgs = new String[args.length + 2];
			System.arraycopy(args, 0, newArgs, 0, args.length);
			newArgs[args.length] = "-no-cpp-precomp";
			newArgs[args.length + 1] = "-Wno-long-double";
			args = newArgs;
		}

		String[] newArgs = new String[args.length + 1];
		newArgs[0] = "-c";
		System.arraycopy(args, 0, newArgs, 1, args.length);
		args = newArgs;

		return exec(args, out, err);
	}

	public int compileAndLink(String[] args, OutputStream out, OutputStream err)
			throws java.io.IOException, ExecutionException {
		/*
		 * Yeah, that's right, I did it, and I'm not sorry. --SGE
		 */
		if (System.getProperty("os.name").equals("Mac OS X")) {
			String[] newArgs = new String[args.length + 2];
			System.arraycopy(args, 0, newArgs, 0, args.length);
			newArgs[args.length] = "-no-cpp-precomp";
			newArgs[args.length + 1] = "-Wno-long-double";
			args = newArgs;
		}
		return exec(args, out, err);
	}

	/**
	 * Preprocesser only. gcc -E <command line>
	 * 
	 * @param commandLine
	 *            a value of type 'String'
	 */
	public int preprocessOnly(String[] args, OutputStream out, OutputStream err)
			throws java.io.IOException, ExecutionException {
		/*
		 * Yeah, that's right, I did it, and I'm not sorry. --SGE
		 */
		if (System.getProperty("os.name").equals("Mac OS X")) {
			String[] newArgs = new String[args.length + 1];
			System.arraycopy(args, 0, newArgs, 0, args.length);
			newArgs[args.length] = "-no-cpp-precomp";
			args = newArgs;
		}

		String[] newArgs = new String[args.length + 1];
		newArgs[0] = "-E";
		System.arraycopy(args, 0, newArgs, 1, args.length);
		args = newArgs;

		return exec(args, out, err);
	}

	public int exitValue() {
		return status;
	}

	public boolean success() {
		return status == 0;
	}

	public String[] getArguments() {
		return commandArgs;
	}

	/**
	 * If the platform is windows, this method will wrap any command line
	 * argument which contains whitespace with double quotes.
	 * 
	 * @return an array of Strings, the formatted command line args
	 */
	private String[] getFormattedCommandArgs() {
		String[] formatted = new String[commandArgs.length];
		boolean doFix = isWindows();
		for (int i = 0; i < commandArgs.length; i++) {
			String nextArg = commandArgs[i];
			if (doFix && nextArg.matches("\\s")) {
				nextArg = "\"" + nextArg + "\"";
			}
			formatted[i] = nextArg;
		}
		return formatted;
	}

	/**
	 * Runs the C Compiler on a given input stream.
	 * 
	 * @param out
	 *            a stream to which the preprocessor's standard output will be
	 *            written
	 * @param err
	 *            a stream to which the preprocessor's standard error will be
	 *            written
	 * 
	 * @return the exit value from the preprocessor
	 * 
	 * @exception java.io.IOException
	 *                if an error occurs on the input or output streams
	 * @exception ExecutionException
	 *                if the preprocessor cannot be executed
	 */
	private int run(OutputStream out, OutputStream err)
			throws java.io.IOException, ExecutionException {
		final Process process;
		try {
			String[] runArgs = getFormattedCommandArgs();
			// process = Runtime.getRuntime().exec(commandArgs);
			process = Runtime.getRuntime().exec(runArgs);
		} catch (java.io.IOException eIO) {
			throw new ExecutionException(
					"C compiler "
							+ gccExe
							+ " not found.  Please check your installation - the Forge expects to find gcc"
							+ File.separator + "bin" + File.separator
							+ "gcc in the Forge install directory.");
		}

		final Drain outConduit = new Drain(process.getInputStream(),
				new PrintWriter(out));
		final Drain errConduit = new Drain(process.getErrorStream(),
				new PrintWriter(err));

		try {
			process.getOutputStream().close();
			process.waitFor();
			outConduit.waitFor();
			errConduit.waitFor();
		} catch (InterruptedException eInterrupted) {
			process.destroy();
			throw new ExecutionException("preprocessor thread interrupted");
		}

		return status = process.exitValue();
	}

	/**
	 * Finds gcc or gcc.exe in forge.home + "/gcc/bin". Returns "gcc" otherwise.
	 */
	private static String findExecutable() {
		/*
		 * Default to a name that can be found in the user's path.
		 */
		final String defaultGcc = isWindows() ? "gcc.exe" : "gcc";
		String gccName = defaultGcc;

		// look in the forge installation first
		final String forgeHome = System.getProperty("forge.home");
		if (forgeHome != null) {
			final String gccHome = forgeHome + File.separator + "gcc"
					+ File.separator + "bin";

			final File gccFile = new File(gccHome);
			if (gccFile.exists()) {
				final FilenameFilter gccFilter = new FilenameFilter() {
					@Override
					public boolean accept(File dir, String name) {
						return name.toLowerCase().equals(defaultGcc);
					}
				};
				final String[] gccNames = gccFile.list(gccFilter);
				for (int i = 0; i < gccNames.length; i++) {
					/*
					 * Take the first matching name that exists.
					 */
					try {
						final String gccPath = gccHome + File.separator
								+ gccNames[i];
						final File canonicalFile = new File(gccPath)
								.getCanonicalFile();
						if (canonicalFile.exists()) {
							gccName = canonicalFile.getCanonicalPath();
							break;
						}
					} catch (IOException eIO) {
					}
				}
			}
		}

		return gccName;
	}

	/**
	 * Tests whether this is a Windows platform, thus requiring ".exe" as the
	 * suffix for an executable. This magic is performed by checking to see
	 * whether the path separator is ":" or ";". True, 'tis ugly, but don't you
	 * know in your heart that it will always work?
	 */
	private static boolean isWindows() {
		return File.pathSeparator.equals(";");
	}

}
