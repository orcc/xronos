/* 
 * XRONOS-EXELIXI
 * 
 * Copyright (C) 2011-2016 EPFL SCI STI MM
 *
 * This file is part of XRONOS-EXELIXI.
 *
 * XRONOS-EXELIXI is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * XRONOS-EXELIXI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with XRONOS-EXELIXI. If not, see <http://www.gnu.org/licenses/>.
 * 
 * Additional permission under GNU GPL version 3 section 7
 * 
 * If you modify this Program, or any covered work, by linking or combining it
 * with Eclipse (or a modified version of Eclipse or an Eclipse plugin or 
 * an Eclipse library), containing parts covered by the terms of the 
 * Eclipse Public License (EPL), the licensors of this Program grant you 
 * additional permission to convey the resulting work.  Corresponding Source 
 * for a non-source form of such a combination shall include the source code 
 * for the parts of Eclipse libraries used as well as that of the covered work.
 * 
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
import org.xronos.openforge.lim.CodeLabel;
import org.xronos.openforge.verilog.model.Assign.UnbalancedAssignmentException;
import org.xronos.orcc.backend.transform.XronosTransform;
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
	static public long getLastModifiedHierarchy(Actor actor) {
		long actorModified = 0;

		if (actor.getFileName() == null) {
			// if source file does not exist, force to generate
			actorModified = Long.MAX_VALUE;
		} else {
			IFile file = actor.getFile();
			actorModified = file.getLocalTimeStamp();
		}
		return actorModified;
	}

	/**
	 * Returns the time of the most recently modified file in the hierarchy.
	 * 
	 * @param instance
	 *            an instance
	 * @return the time of the most recently modified file in the hierarchy
	 */
	static public long getLastModifiedHierarchy(Instance instance) {
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

	/** The options given to the printer **/
	protected Map<String, Object> options;

	public XronosPrinter() {
		options = new HashMap<String, Object>();
	}

	public XronosPrinter(Boolean keepUnchangedFiles) {
		this();
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
			String tbPath, String tbVhdPath, Actor actor,
			Map<String, Object> options, ResourceCache resourceCache,
			int idxInstance, int totalInstances, boolean schedulerInformation,
			boolean newLimGen, Boolean debugMode) {
		Forge f = new Forge();
		GenericJob xronosMainJob = new GenericJob();
		Engine engine = null;
		boolean error = false;

		long t0 = System.currentTimeMillis();

		if (!actor.hasAttribute("xronos_no_generation")) {
			try {
				xronosMainJob.setOptionValues(xronosArgs);
				// Set the Xilinx Part
				xronosMainJob.getOption(OptionRegistry.XILINX_PART).setValue(
						CodeLabel.UNSCOPED, "xc2vp30-7-ff1152");
				f.preprocess(xronosMainJob);
				OrccLogger.traceln("Compiling instance: "
						+ actor.getSimpleName() + " (" + idxInstance + "/"
						+ totalInstances + ")");
				if (newLimGen) {
					XronosTransform.transformActor(actor, options, debugMode);
					engine = new DesignEngine(xronosMainJob, actor, schedulerInformation);
				} else {
					XronosTransform.transformActor(actor, options,
							resourceCache, true, schedulerInformation,
							debugMode);
					engine = new DesignEngine(xronosMainJob, actor,
							resourceCache, schedulerInformation);
				}
				engine.begin();
			} catch (NewJob.ForgeOptionException foe) {
				OrccLogger.severeln("\t command line option error: "
						+ foe.getMessage());
				OrccLogger.severeln("");
				OrccLogger.severeln(OptionRegistry.usage(false));
				error = true;
			} catch (ForgeFatalException ffe) {
				OrccLogger
						.severeln("\t - failed to compile:Forge compilation ended with fatal error: "
								+ ffe.getMessage());
				error = true;
			} catch (NullPointerException ex) {
				OrccLogger
						.severeln("\t - failed to compile: NullPointerException, "
								+ ex.getMessage());
				error = true;
			} catch (NoSuchElementException ex) {
				OrccLogger
						.severeln("\t - failed to compile: NoSuchElementException, "
								+ ex.getMessage());
				error = true;
			} catch (UnbalancedAssignmentException ex) {
				OrccLogger
						.severeln("\t - failed to compile: UnbalancedAssignmentException, "
								+ ex.getMessage());
				error = true;
			} catch (ArrayIndexOutOfBoundsException ex) {
				OrccLogger
						.severeln("\t - failed to compile: ArrayIndexOutOfBoundsException, "
								+ ex.getMessage());
				error = true;
			} catch (Throwable t) {
				OrccLogger
						.severeln("\t - failed to compile: " + t.getMessage());
				error = true;
			}
			if (!error) {
				long t1 = System.currentTimeMillis();
				OrccLogger.traceln("\t - Compiled in: " + (float) (t1 - t0)
						/ 1000 + "s");
				engine.kill();
				engine = null;
				printTestbenches(actor, tbPath, tbVhdPath,
						(Boolean) getOptions().get("xilinxPrimitives"));
			}
			if (options.containsKey("org.xronos.orcc.generateGoDone")) {
				Boolean generateGoDone = (Boolean) options
						.get("org.xronos.orcc.generateGoDone");

				if (generateGoDone) {
					if (!error) {
						String rtlGoDonePath = rtlPath + File.separator
								+ "rtlGoDone";
						VerilogAddGoDone verilogFile = new VerilogAddGoDone(
								actor, rtlPath, rtlGoDonePath);
						verilogFile.addGoDone();
					}
				}
			}
			return error;
		}

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

	public boolean printNetwork(String[] xronosArgs, String rtlPath,
			Network network, Map<String, Object> options,
			ResourceCache resourceCache, boolean schedulerInformation) {
		Forge f = new Forge();
		GenericJob xronosMainJob = new GenericJob();
		boolean error = false;

		long t0 = System.currentTimeMillis();

		xronosMainJob.setOptionValues(xronosArgs);
		// Set the Xilinx Part
		xronosMainJob.getOption(OptionRegistry.XILINX_PART).setValue(
				CodeLabel.UNSCOPED, "xc2vp30-7-ff1152");
		f.preprocess(xronosMainJob);

		XronosTransform.transformNetworkActors(network, options, resourceCache,
				schedulerInformation);
		Engine engine = new DesignEngine(xronosMainJob, network, resourceCache,
				schedulerInformation);
		try {
			engine.begin();
		} catch (NewJob.ForgeOptionException foe) {
			OrccLogger.severeln("\t command line option error: "
					+ foe.getMessage());
			OrccLogger.severeln("");
			OrccLogger.severeln(OptionRegistry.usage(false));
			error = true;
		} catch (ForgeFatalException ffe) {
			OrccLogger
					.severeln("\t - failed to compile:Forge compilation ended with fatal error: "
							+ ffe.getMessage());
			error = true;
		} catch (NullPointerException ex) {
			OrccLogger
					.severeln("\t - failed to compile: NullPointerException, "
							+ ex.getMessage());
			error = true;
		} catch (NoSuchElementException ex) {
			OrccLogger
					.severeln("\t - failed to compile: NoSuchElementException, "
							+ ex.getMessage());
			error = true;
		} catch (UnbalancedAssignmentException ex) {
			OrccLogger
					.severeln("\t - failed to compile: UnbalancedAssignmentException, "
							+ ex.getMessage());
			error = true;
		} catch (ArrayIndexOutOfBoundsException ex) {
			OrccLogger
					.severeln("\t - failed to compile: ArrayIndexOutOfBoundsException, "
							+ ex.getMessage());
			error = true;
		} catch (Throwable t) {
			OrccLogger.severeln("\t - failed to compile: " + t.getMessage());
			error = true;
		}
		if (!error) {
			long t1 = System.currentTimeMillis();
			OrccLogger.traceln("\t - Compiled in: " + (float) (t1 - t0) / 1000
					+ "s");
		}
		engine.kill();
		return error;
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
		if (vertex instanceof Actor) {
			file = path + File.separator + "tcl_"
					+ ((Actor) vertex).getSimpleName() + ".tcl";
			sequence = new TclScriptPrinter().printInstanceTestbenchTclScript(
					(Actor) vertex, options);
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

	/**
	 * This method prints a VHDL testbench for a given Instance
	 * 
	 * @param path
	 *            the testbench path
	 * @param instance
	 *            a Instance
	 * @return
	 */
	public boolean printTestbench(String path, Actor actor) {
		String file = path + File.separator + actor.getSimpleName() + "_tb.vhd";
		CharSequence sequence = new TestbenchPrinter().printInstance(actor,
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
	 * This method prints a VHDL Testbench for a given network
	 * 
	 * @param path
	 *            the testbench path
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

	public void printTestbenches(Actor actor, String tbPath, String tbVhdPath,
			Boolean xilinxPrimitives) {
		printTclScript(tbPath, true, actor);
		printTestbench(tbVhdPath, actor);
	}

	public boolean printWeightTclScript(String path, Network network) {
		try {
			String file = path + File.separator + "tcl_generate_weights_"
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
}
