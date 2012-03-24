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

package net.sf.openforge.lim;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.sf.openforge.app.EngineThread;
import net.sf.openforge.lim.memory.LogicalMemory;
import net.sf.openforge.util.naming.ID;

/**
 * LimDRC does Design-Rule-Checking for a lim prior to Scheduling...
 * 
 * @author cschanck
 * @version $Id: LimDRC.java 2 2005-06-09 20:00:48Z imiller $
 * 
 * @see Visitable
 */
public class LimDRC extends FilteredVisitor {

	private Map failures = new LinkedHashMap();
	private Set procedureBodies = new HashSet();

	/**
	 * DOCUMENT ME!
	 * 
	 * @return DOCUMENT ME!
	 */
	public Map getFailures() {
		return failures;
	}

	/**
	 * DOCUMENT ME!
	 */
	public void dumpFailures() {
		if (!failures.isEmpty()) {
			StringBuffer buf = new StringBuffer();

			EngineThread.getGenericJob().warn(
					"internal integrity check failure:");
			EngineThread.getGenericJob().inc();
			for (Iterator it = failures.values().iterator(); it.hasNext();) {
				EngineThread.getGenericJob().info((String) it.next());
			}

			EngineThread.getGenericJob().dec();
		}
	}

	/**
	 * DOCUMENT ME!
	 * 
	 * @param design
	 *            DOCUMENT ME!
	 */
	@Override
	public void visit(Design design) {
		for (Iterator iter = design.getLogicalMemories().iterator(); iter
				.hasNext();) {
			LogicalMemory logicalMem = (LogicalMemory) iter.next();
			filterAny(logicalMem.getStructuralMemory());
			traverseExtraModule(logicalMem.getStructuralMemory());
		}

		for (Iterator iter = design.getPins().iterator(); iter.hasNext();) {
			filterAny((Pin) iter.next());
		}

		super.visit(design);
	}

	/**
	 * DOCUMENT ME!
	 * 
	 * @param proc
	 *            DOCUMENT ME!
	 */
	@Override
	public void visit(Procedure proc) {
		procedureBodies.add(proc.getBody());
		super.visit(proc);
	}

	/**
	 * DOCUMENT ME!
	 * 
	 * @param call
	 *            DOCUMENT ME!
	 */
	@Override
	public void visit(Call call) {
		super.visit(call);

		// Ensure that all call/procedure buses and ports agree on
		// their 'usedness'
		for (Iterator iter = call.getPorts().iterator(); iter.hasNext();) {
			Port port = (Port) iter.next();
			Port procPort = call.getProcedurePort(port);
			if ((port != null) && (procPort != null)
					&& (port.isUsed() != procPort.isUsed())) {
				failures.put(port,
						"Call port and procedure port disagree on used-ness.  Auto Fixing.");
				port.setUsed(true);
				procPort.setUsed(true);
			}

			// XXX FIXME. Why is there disagreement???? I commented
			// this out for now, but it should probably be fixed anyway!!!
			// assert (port != null && procPort != null) :
			// "DISAGREEMENT between call interface and procedure interface";
		}

		for (Iterator iter = call.getBuses().iterator(); iter.hasNext();) {
			Bus bus = (Bus) iter.next();
			Bus procBus = call.getProcedureBus(bus);
			if ((bus != null) && (procBus != null)
					&& (bus.isUsed() != procBus.isUsed())) {
				failures.put(bus,
						"Call bus and procedure bus disagree on used-ness.  Auto Fixing.");
				bus.setUsed(true);
				procBus.setUsed(true);
			}

			// XXX FIXME. Why is there disagreement???? I commented
			// this out for now, but it should probably be fixed anyway!!!
			// assert (bus != null && procBus != null) :
			// "DISAGREEMENT between call interface and procedure interface";
		}
	}

	/**
	 * Recursively calls filterAny on all components of the given Module. Meant
	 * to be used for Modules contained in the design which are not defined in
	 * the Visitor interface.
	 */
	public void traverseExtraModule(Module mod) {
		for (Iterator iter = mod.getComponents().iterator(); iter.hasNext();) {
			Component c = (Component) iter.next();
			filterAny(c);
			if (c instanceof Module) {
				traverseExtraModule((Module) c);
			}
		}
	}

	/**
	 * DOCUMENT ME!
	 * 
	 * @param c
	 *            DOCUMENT ME!
	 */
	@Override
	public void filterAny(Component c) {
		// Job.info("DRC Checking: "+ID.showLogical(c));
		if (_lim.db) {
			_lim.ln(_lim.DRC, "DRC Checking: " + ID.showLogical(c));
		}

		drcBuses(c);
		drcPorts(c);
		super.filterAny(c);
	}

	/**
	 * Apply design rule checks to the Bus' of the given {@link Component}.
	 * Current checks include:
	 * 
	 * <p>
	 * 
	 * <ul>
	 * <li>
	 * All buses have a {@link Value}</li>
	 * </ul>
	 * </p>
	 * 
	 * @param c
	 *            the {@link Component} to check.
	 */
	private void drcBuses(Component c) {
		Collection buses = c.getBuses();
		for (Iterator it = buses.iterator(); it.hasNext();) {
			Bus b = (Bus) it.next();
			if (b.isUsed()) {
				// does it have a value
				if (b.getValue() == null) {
					failures.put(b, "Bus " + ID.showLogical(b)
							+ " belonging to " + ID.showLogical(c)
							+ " Lacks Value");
					if (_lim.db) {
						_lim.ln(_lim.DRC,
								"Bus has no value: " + b + " " + c.show());
					}
				}
			}
		}
	}

	/**
	 * Apply design rule checks to the Ports of the given component. Current
	 * checks include:
	 * 
	 * <p>
	 * 
	 * <ul>
	 * <li>
	 * All ports have a {@link Value}</li>
	 * <li>
	 * All ports have a {@link Bus} connected unless:
	 * 
	 * <ul>
	 * <li>
	 * The port is unused</li>
	 * <li>
	 * The port is owned by a procedure body</li>
	 * </ul>
	 * </li>
	 * <li>
	 * The size of the Port's Value ({@link Value#size}) is less than or equal
	 * to the size of the Bus' Value.</li>
	 * </ul>
	 * </p>
	 * 
	 * @param component
	 *            the{@link Component} to test
	 */
	private void drcPorts(Component component) {
		List ports = component.getPorts();
		for (Iterator it = ports.iterator(); it.hasNext();) {
			final Port port = (Port) it.next();

			if (port.isUsed()) {
				// does it have a value
				final boolean portHasValue = (port.getValue() != null);
				if (!portHasValue && !isWeirdRegPort(port)) {
					failures.put(port, "Port " + ID.showLogical(port)
							+ " belonging to " + ID.showLogical(component)
							+ " Lacks Value");
					if (_lim.db) {
						_lim.ln(_lim.DRC, "Port has no value: " + port
								+ " is used: " + port.isUsed() + " "
								+ component.show());
					}
				}

				// Don't test anything else on unused ports.
				if (!port.isUsed()) {
					continue;
				}

				// port must have a bus
				if ((port.getBus() == null)
						&& !procedureBodies.contains(port.getOwner())) // Procedure
																		// bodies
																		// have
																		// no
																		// connections
																		// on
																		// their
																		// ports
				{
					if (!isWeirdRegPort(port)) {
						failures.put(port, "Port " + ID.showLogical(port)
								+ " belonging to " + ID.showLogical(component)
								+ " Has No Bus");
						if (_lim.db) {
							_lim.ln(_lim.DRC, "Port has no bus: " + port + " "
									+ component.show());
						}
					}
				}

				/*
				 * The rule is actually that all 'care' bits in the port's value
				 * must come froma Bus and the position in that bus must be less
				 * than that bus' Value size
				 */
				/*
				 * Check that all bits sourcing a port (non constant) come from
				 * a bus and that the Value on that Bus has sufficient bits to
				 * cover the sourced bit. InBufs and procedure blocks are
				 * special since they never get their ports hooked up.
				 */
				if (portHasValue) {
					if (!(component instanceof InBuf)
							&& !procedureBodies.contains(port.getOwner())) {
						for (int i = 0; i < port.getValue().getSize(); i++) {
							Bit bit = port.getValue().getBit(i);

							// Check that none of the bits are the generic CARE
							// bit. All should come from real buses by this
							// point.
							if (bit == Bit.CARE) {
								failures.put(port, "Bit " + i + " of Port "
										+ ID.showLogical(port)
										+ " is the generic care bit");
								if (_lim.db) {
									_lim.ln(_lim.DRC, "Bit " + i + " port "
											+ port + " is generic care bit "
											+ component.show());
								}
							}

							// basic bit test added CRSS, fixed for new Value:
							// SGE
							if (!bit.isGlobal()) {
								int position = bit.getPosition();
								if (position >= bit.getOwner().getValue()
										.getSize()) {
									failures.put(
											port,
											"Bit "
													+ i
													+ " of Port "
													+ ID.showLogical(port)
													+ " has position in it's source bus greater than that bus' size");
									if (_lim.db) {
										_lim.ln(_lim.DRC,
												"Bit "
														+ i
														+ " position: "
														+ position
														+ " is greater than bus "
														+ bit.getOwner()
														+ " value size "
														+ bit.getOwner()
																.getValue()
																.debug());
									}
								}
							}
						}
					}
				}
			}
		}
	}

	/**
	 * This is a big FIXME. Synchronous set/reset Regs are often created with
	 * the internalResetPort or setPort marked "isUsed", but unconnected.
	 * 
	 * @param port
	 *            any port
	 * 
	 * @return true if the port is the set port or internal reset port of a Reg
	 */
	private static boolean isWeirdRegPort(Port port) {
		if (port.getOwner() instanceof Reg) {
			final Reg reg = (Reg) port.getOwner();
			return (port == reg.getInternalResetPort())
					|| (port == reg.getSetPort());
		} else if (port.getOwner() instanceof Latch) {
			final Latch latch = (Latch) port.getOwner();
			// Go port of a latch needs no connection and really is
			// not used, but gets set to 'used' during scheduling
			// because we say it consumes a GO.
			return (port == latch.getGoPort());
		}

		return false;
	}
}
