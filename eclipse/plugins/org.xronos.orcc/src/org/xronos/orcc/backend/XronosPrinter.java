/*
 * Copyright (c) 2012, Ecole Polytechnique Fédérale de Lausanne
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
 *   * Neither the name of the Ecole Polytechnique Fédérale de Lausanne nor the names of its
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

package org.xronos.orcc.backend;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

import net.sf.orcc.df.Actor;
import net.sf.orcc.df.Instance;
import net.sf.orcc.df.Network;
import net.sf.orcc.graph.Vertex;
import net.sf.orcc.util.OrccLogger;

import org.eclipse.core.resources.IFile;
import org.eclipse.emf.ecore.EObject;
import org.xronos.openforge.app.Engine;
import org.xronos.openforge.app.Forge;
import org.xronos.openforge.app.ForgeFatalException;
import org.xronos.openforge.app.GenericJob;
import org.xronos.openforge.app.NewJob;
import org.xronos.openforge.app.OptionRegistry;
import org.xronos.openforge.verilog.model.Assign.UnbalancedAssignmentException;
import org.xronos.orcc.design.DesignEngine;
import org.xronos.orcc.design.ResourceCache;

/**
 * This class defines a printer for the Xronos Orcc frontend. This class
 * supports caching in order not to regenerate the Verilog files.
 * 
 * @author Endri Bezati
 * 
 */

public class XronosPrinter {
	/** Keep the unchanged files flag **/
	private boolean keepUnchangedFiles;

	/** The options given to the printer **/
	protected Map<String, Object> options;

	public XronosPrinter() {
		options = new HashMap<String, Object>();
	}

	public XronosPrinter(Boolean keepUnchangedFiles) {
		this();
		this.keepUnchangedFiles = keepUnchangedFiles;
	}

	/**
	 * Returns the time of the most recently modified file in the hierarchy.
	 * 
	 * @param instance
	 *            an instance
	 * @return the time of the most recently modified file in the hierarchy
	 */
	private long getLastModifiedHierarchy(Instance instance) {
		long instanceModified = 0;
		if (instance.isActor()) {
			Actor actor = instance.getActor();
			if (actor.getFileName() == null) {
				// if source file does not exist, force to generate
				instanceModified = Long.MAX_VALUE;
			} else {
				IFile file;
				if (instance.isActor()) {
					file = instance.getActor().getFile();
				} else if (instance.isNetwork()) {
					file = instance.getNetwork().getFile();
				} else {
					return Long.MAX_VALUE;
				}
				instanceModified = file.getLocalTimeStamp();
			}
		} else if (instance.isNetwork()) {
			Network network = instance.getNetwork();
			instanceModified = network.getFile().getLocalTimeStamp();
		}

		EObject cter = instance.eContainer();
		if (cter instanceof Network) {
			Network network = (Network) cter;
			long parentModif;
			if (network.getFile() != null) {
				parentModif = network.getFile().getLocalTimeStamp();
			} else {
				parentModif = Long.MAX_VALUE;
			}
			return Math.max(parentModif, instanceModified);
		} else {
			return instanceModified;
		}
	}

	/**
	 * Get the map of options
	 * 
	 * @return
	 */
	public Map<String, Object> getOptions() {
		return options;
	}

	/**
	 * This method calls Xronos synthesizer and generates a Verilog file for a
	 * given Instance
	 * 
	 * @param xronosArgs
	 *            Xronos input options
	 * @param rtlPath
	 *            the RTL path
	 * @param instance
	 *            an Instance
	 * @return
	 */
	public boolean printInstance(String[] xronosArgs, String rtlPath,
			Instance instance, ResourceCache resourceCache) {
		Forge f = new Forge();
		GenericJob xronosMainJob = new GenericJob();
		boolean error = false;

		String file = rtlPath + File.separator + instance.getSimpleName()
				+ ".v";
		long t0 = System.currentTimeMillis();
		if (instance.isActor()) {
			if (!instance.getActor().hasAttribute("no_generation")) {
				if (keepUnchangedFiles) {
					long sourceLastModified = getLastModifiedHierarchy(instance);
					File targetFile = new File(file);
					long targetLastModified = targetFile.lastModified();
					if (sourceLastModified < targetLastModified) {
						return true;
					}
				}
				try {
					xronosMainJob.setOptionValues(xronosArgs);
					f.preprocess(xronosMainJob);
					Engine engine = new DesignEngine(xronosMainJob, instance,
							resourceCache);
					engine.begin();
				} catch (NewJob.ForgeOptionException foe) {
					OrccLogger.severeln("Command line option error: "
							+ foe.getMessage());
					OrccLogger.severeln("");
					OrccLogger.severeln(OptionRegistry.usage(false));
					error = true;
				} catch (ForgeFatalException ffe) {
					OrccLogger
					.severeln("Forge compilation ended with fatal error:");
					OrccLogger.severeln(ffe.getMessage());
					error = true;
				} catch (NullPointerException ex) {
					OrccLogger.severeln("Instance: " + instance.getSimpleName()
							+ ", failed to compile: NullPointerException, "
							+ ex.getMessage());
					error = true;
				} catch (NoSuchElementException ex) {
					OrccLogger.severeln("Instance: " + instance.getSimpleName()
							+ ", failed to compile: NoSuchElementException, "
							+ ex.getMessage());
					error = true;
				} catch (UnbalancedAssignmentException ex) {
					OrccLogger
					.severeln("Instance: "
							+ instance.getSimpleName()
							+ ", failed to compile: UnbalancedAssignmentException, "
							+ ex.getMessage());
					error = true;
				} catch (ArrayIndexOutOfBoundsException ex) {
					OrccLogger
					.severeln("Instance: "
							+ instance.getSimpleName()
							+ ", failed to compile: ArrayIndexOutOfBoundsException, "
							+ ex.getMessage());
					error = true;
				} catch (Throwable t) {
					OrccLogger.severeln("Instance: " + instance.getSimpleName()
							+ ", failed to compile: " + t.getMessage());
					error = true;
				}
				if (!error) {
					long t1 = System.currentTimeMillis();
					OrccLogger.traceln("Compiling instance: "
							+ instance.getSimpleName() + ": Compiled in: "
							+ (float) (t1 - t0) / 1000 + "s");
				}
				if (options.containsKey("generateGoDone")) {
					Boolean generateGoDone = (Boolean) options
							.get("generateGoDone");

					if (generateGoDone) {
						if (!error) {
							String rtlGoDonePath = rtlPath + File.separator
									+ "rtlGoDone";
							VerilogAddGoDone verilogFile = new VerilogAddGoDone(
									instance, rtlPath, rtlGoDonePath);
							verilogFile.addGoDone();
						}
					}
				}
				Runtime r = Runtime.getRuntime();
				r.gc();
				return error;
			} else {
				OrccLogger.warnln("Instance: " + instance.getSimpleName()
						+ " will not be generated!");
			}
		}
		Runtime r = Runtime.getRuntime();
		r.gc();
		return false;
	}

	/**
	 * This method prints a VHDL representation of a network
	 * 
	 * @param rtlPath
	 *            the RTL path
	 * @param network
	 *            a Network
	 * @return
	 */
	public boolean printNetwork(String rtlPath, Network network) {
		String file = rtlPath + File.separator + network.getSimpleName()
				+ ".vhd";
		CharSequence sequence = new NetworkPrinter().printNetwork(network,
				options);
		try {
			PrintStream ps = new PrintStream(new FileOutputStream(file));
			ps.print(sequence.toString());
			ps.close();
		} catch (FileNotFoundException e) {
			OrccLogger.severeln("File Not Found Exception: " + e.getMessage());
		}
		return false;
	}

	/**
	 * This method prints a ModelSim TCL script for an instance or a Network
	 * 
	 * @param path
	 *            the testbench path
	 * @param testBench
	 *            Test input output of the Network with fifo traces
	 * @param vertex
	 *            a Vertex of Insance or Network
	 * @return
	 */
	public boolean printTclScript(String path, Boolean testBench, Vertex vertex) {
		String file = null;
		CharSequence sequence = null;
		if (vertex instanceof Instance) {
			file = path + File.separator + "tcl_"
					+ ((Instance) vertex).getSimpleName() + ".tcl";
			sequence = new TclScriptPrinter().printInstanceTestbenchTclScript(
					(Instance) vertex, options);
		} else if (vertex instanceof Network) {
			file = path + File.separator + "tcl_"
					+ ((Network) vertex).getSimpleName() + ".tcl";
			sequence = new TclScriptPrinter().printNetworkTestbenchTclScript(
					(Network) vertex, options);
		}

		try {
			PrintStream ps = new PrintStream(new FileOutputStream(file));
			ps.print(sequence.toString());
			ps.close();
		} catch (FileNotFoundException e) {
			OrccLogger.severeln("File Not Found Exception: " + e.getMessage());
		}
		return false;
	}

	public boolean printSimTclScript(String path, Boolean weights,
			Network network) {
		try {
			String prefix = weights ? "sim_goDone_" : "sim_";
			String file = path + File.separator + prefix
					+ network.getSimpleName() + ".tcl";

			CharSequence sequence = new TclScriptPrinter()
			.printNetworkTclScript(network, options);
			PrintStream ps = new PrintStream(new FileOutputStream(file));
			ps.print(sequence.toString());
			ps.close();
		} catch (FileNotFoundException e) {
			OrccLogger.severeln("File Not Found Exception: " + e.getMessage());
		}
		return false;
	}

	public boolean printWeightTclScript(String path, Network network) {
		try {
			String file = path + File.separator + "generateWeights_"
					+ network.getSimpleName() + ".tcl";

			CharSequence sequence = new TclScriptPrinter()
			.printNetworkTclScript(network, options);
			PrintStream ps = new PrintStream(new FileOutputStream(file));
			ps.print(sequence.toString());
			ps.close();
		} catch (FileNotFoundException e) {
			OrccLogger.severeln("File Not Found Exception: " + e.getMessage());
		}
		return false;
	}

	/**
	 * This method prints a VHDL Testbench for a given network
	 * 
	 * @param path
	 *            the testbench path
	 * @param goDone
	 *            generate a GoDone tracer testbench
	 * @param network
	 *            a Network
	 * @return
	 */
	public boolean printTestbench(String path, Network network) {
		String file = path + File.separator + network.getSimpleName()
				+ "_tb.vhd";
		CharSequence sequence;
		sequence = new TestbenchPrinter().printNetwork(network, options);

		try {
			PrintStream ps = new PrintStream(new FileOutputStream(file));
			ps.print(sequence.toString());
			ps.close();
		} catch (FileNotFoundException e) {
			OrccLogger.severeln("File Not Found Exception: " + e.getMessage());
		}
		return false;
	}

	/**
	 * This method prints a VHDL testbench for a given Instance
	 * 
	 * @param path
	 *            the testbench path
	 * @param instance
	 *            a Instance
	 * @return
	 */
	public boolean printTestbench(String path, Instance instance) {
		String file = path + File.separator + instance.getSimpleName()
				+ "_tb.vhd";
		CharSequence sequence = new TestbenchPrinter().printInstance(instance,
				options);
		try {
			PrintStream ps = new PrintStream(new FileOutputStream(file));
			ps.print(sequence.toString());
			ps.close();
		} catch (FileNotFoundException e) {
			OrccLogger.severeln("File Not Found Exception: " + e.getMessage());
		}
		return false;
	}
}
