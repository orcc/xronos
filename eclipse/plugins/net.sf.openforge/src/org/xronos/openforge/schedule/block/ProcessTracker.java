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

package org.xronos.openforge.schedule.block;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.xronos.openforge.lim.Bus;
import org.xronos.openforge.lim.Component;
import org.xronos.openforge.lim.DataFlowVisitor;
import org.xronos.openforge.lim.Dependency;
import org.xronos.openforge.lim.Entry;
import org.xronos.openforge.lim.Exit;
import org.xronos.openforge.lim.Module;
import org.xronos.openforge.lim.Port;


/**
 * ProcessTracker provides facilities to the scheduler for updating the various
 * {@link MemProcess} objects identified in a design based on the scheduling of
 * various components. As each component is scheduled, it is registered with
 * this tracker. The registration process allows us to identify what processes
 * the component is subject to the control of and which processes it opens and
 * closes. Using this information, the control bus for that component may be
 * used to stall various processes.
 * <p>
 * <b>Technically, because we do all this analysis based on dependencies, it
 * would be possible to do this tracking/analysis prior to scheduling and only
 * resolve component done buses to correct control buses during scheduling.</b>
 * 
 * <p>
 * Created: Wed Sep 15 09:50:01 2004
 * 
 * @author imiller, last modified by $Author: imiller $
 * @version $Id: ProcessTracker.java 88 2006-01-11 22:39:52Z imiller $
 */
public class ProcessTracker extends DataFlowVisitor {

	/**
	 * A Map of Component to Set of MemProcess objects to which that component
	 * is subject. Protected for testing.
	 */
	protected Map<Port, Set<MemProcess>> portToProcs = new HashMap<Port, Set<MemProcess>>();

	/** The Set of MemProcess objects which are tracked. */
	private final Set<MemProcess> processes;

	/**
	 * A Map of Module to Set of ModuleStallSource objects which identify
	 * stallers of the GO for the Module
	 */
	private final Map<Module, Set<ModuleStallSource>> stalledModules = new HashMap<Module, Set<ModuleStallSource>>();

	/**
	 * Create a new ProcessTracker for the given set of MemProcess objects.
	 * 
	 * @param processes
	 *            a Collection of MemProcess objects.
	 */
	public ProcessTracker(Collection<MemProcess> processes) {
		this.processes = new HashSet<MemProcess>(processes);
	}

	/**
	 * Registers the component with this tracker, causing the component to
	 * become a stall signal to various processes (or the containing module)
	 * based on the processes that are opened or closed by the component and the
	 * processes which produce data consumed by this component.
	 * 
	 * @param comp
	 *            a non-null Component
	 */
	public void register(Component comp) {
		if (comp == null) {
			throw new IllegalArgumentException(
					"component to track cannot be null");
		}
		if (_block.db)
			_block.ln("Registering " + comp);
		// Use the ComponentProcessDeriver to determine how this
		// component relates to the identified processes
		final ComponentProcessDeriver compProcs = new ComponentProcessDeriver(
				comp, this);

		// Push the set of 'in' processes identified for this
		// component to each port that has a dependency on one of the
		// buses of this component. By pushing the processes forward,
		// instead of caching the state of each compnent we can much
		// better manage the memory consumption of this class. The
		// data is only preserved as long as needed.
		markPorts(comp, compProcs.getInProcesses());
		// Because we have fully characterized
		// (via the ComponentProcessDeriver) the processes for this
		// component, we can eliminate from the map the annotations
		// for this components ports.
		for (Port port : comp.getPorts()) {
			portToProcs.remove(port);
		}

		if (_block.db)
			_block.ln(compProcs.debug());

		// If the component is an endpoint of the process, then use it
		// as the stall (release) signal to the process.
		for (MemProcess closedProc : compProcs.getClosedProcesses()) {
			if (_block.db)
				_block.ln("Stalling(close) " + closedProc + " by " + comp);
			closedProc.addStallSignal(comp);
		}

		// In addition to closing the process, a component may have to
		// provide reverse flow control to other processes or the
		// containing module. Two criteria must be met.
		// First, every process must stall something that comes before
		// it in order to ensure full reverse flow control.
		// Second, every component must stall whatever is generating
		// data that is consumed by it iff it comes from 'outside' the
		// process.

		// Stall anything that produces data consumed in this process
		boolean dataStalled = false;
		for (MemProcess dataProc : compProcs.getDataProcsToStall()) {
			dataStalled = true;
			if (_block.db)
				_block.ln("Stalling(data): " + dataProc + " by " + comp);
			dataProc.addStallSignal(comp);
		}

		// If there were no initial processes, but there are control
		// processes then we need to stall something before the
		// control process since there is not explicit reverse flow
		// control (the control proc is 'outside' any other process).
		// So, if there was a data process, assume it (they) were
		// stalled. Otherwise default back to simply stalling the
		// containing module.
		final Set<MemProcess> uncontrolledOpenedProcs = compProcs
				.getUncontrolledOpenProcs();
		if (!uncontrolledOpenedProcs.isEmpty() && !dataStalled) {
			if (_block.db)
				_block.ln("Stalling(module by process) " + comp.getOwner()
						+ " at " + comp.showOwners() + " by "
						+ uncontrolledOpenedProcs);
			stallModuleByProcs(uncontrolledOpenedProcs, comp.getOwner());
		}

		// An uncontrolled data port is one which consumes data which
		// is not generated in another process. That means that this
		// data does not depend in any way on the stallable processes
		// that start from the input of FIFO data. The data generator
		// logic could potentially 'freewheel' uncontrollably while
		// the process that this component is in is stalled, thus
		// 'getting ahead' of the current correct state. In order to
		// stall these uncontrolled data generators we need to stall
		// the containing module by this component.
		final Set<Port> uncontrolledData = compProcs.getUncontrolledDataPorts();
		if (!compProcs.getInProcesses().isEmpty()
				&& !uncontrolledData.isEmpty()) {
			if (_block.db)
				_block.ln("Stalling(module by uncontrolled data) "
						+ comp.getOwner() + " by " + comp.show() + " "
						+ uncontrolledData + " in " + comp.showOwners());
			stallModuleByComp(comp, comp.getOwner());
		}
	}

	/**
	 * Pushes the collection of MemProcess objects out to each port which
	 * maintains a dependency to any bus of this component. The state for each
	 * of these ports is maintained in the portToProcs map. (protected instead
	 * of private for testing purposes)
	 * 
	 * @param comp
	 *            a non-null Component
	 * @param procs
	 *            a Collection of MemProcess objects.
	 */
	protected void markPorts(Component comp, Collection<MemProcess> procs) {
		// Push the collection of processes onto the dependent ports
		for (Exit exit : comp.getExits()) {
			for (Bus bus : exit.getBuses()) {
				for (Dependency dep : bus.getLogicalDependents()) {
					Set<MemProcess> procSet = portToProcs.get(dep.getPort());
					if (procSet == null) {
						procSet = new HashSet<MemProcess>();
						portToProcs.put(dep.getPort(), procSet);
					}
					procSet.addAll(procs);
				}
			}
		}
	}

	/**
	 * Takes the collection of MemProcess objects and identifies them as needing
	 * to stall the given context Module. Module stalls (by process) are tracked
	 * by the MemProcess that is to stall the module because of the possibility
	 * that when we realize that a process needs to stall the module that all
	 * the start points may not have yet been processed. In this case the
	 * stallboard in the unprocessed start point is null and thus we do not have
	 * access to it to capture its bus. This can happen when there are multiple
	 * parallel register reads prior to a register write.
	 */
	private void stallModuleByProcs(Collection<MemProcess> stalls,
			Module context) {
		Set<ModuleStallSource> stallProcs = new HashSet<ModuleStallSource>();

		for (MemProcess memProc : stalls) {
			class MemProcModuleStall implements ModuleStallSource {
				private final MemProcess process;

				MemProcModuleStall(MemProcess proc) {
					process = proc;
				}

				@Override
				public Set<Component> getStallingComponents() {
					Set<Component> comps = new HashSet<Component>();
					for (ProcessStartPoint processStartPoint : process
							.getStartPoints()) {
						comps.add(processStartPoint.getStallPoint());
					}
					return comps;
				}
			}
			stallProcs.add(new MemProcModuleStall(memProc));
		}

		stallModule(stallProcs, context);
	}

	/**
	 * Adds the specified component as a staller of the module by wrapping it in
	 * a lightweight implementation of the {@link ModuleStallSource} interface.
	 * 
	 * @param stallComp
	 *            a non-null Component, used to stall the context module
	 * @param context
	 *            a non-null Module
	 */
	private void stallModuleByComp(Component stallComp, Module context) {
		if (stallComp == null) {
			throw new IllegalArgumentException(
					"Cannot stall a context by null component");
		}

		class ComponentModuleStall implements ModuleStallSource {
			private final Component component;

			ComponentModuleStall(Component comp) {
				component = comp;
			}

			@Override
			public Set<Component> getStallingComponents() {
				return Collections.singleton(component);
			}
		}

		stallModule(
				Collections.<ModuleStallSource> singleton(new ComponentModuleStall(
						stallComp)), context);
	}

	/**
	 * Adds the collection of ModuleStallSource objects as stallers of the
	 * specfied module. Because the decision to stall a module is based upon the
	 * fact that a process consumes something that needs to thus stall the
	 * module, we are guaranteed that the context module will consume a GO.
	 * 
	 * @param modStallSources
	 *            a Collection of {@link ModuleStallSource} objects
	 * @param context
	 *            a non-null Module
	 */
	private void stallModule(Collection<ModuleStallSource> modStallSources,
			Module context) {
		if (context == null) {
			throw new IllegalArgumentException("Cannot annotated null context");
		}

		Set<ModuleStallSource> stallProcs = stalledModules.get(context);
		if (stallProcs == null) {
			stallProcs = new HashSet<ModuleStallSource>();
			stalledModules.put(context, stallProcs);
		}
		stallProcs.addAll(modStallSources);
	}

	/**
	 * Retrieves a set of {@link ModuleStallSource} objects whose context is the
	 * specified Module and which are needed to stall the GO of that module
	 * based on process scheduling constraints. These stall buses should be
	 * derived from the Set of stalling components contained in each
	 * ModuleStallSource. These stall signals are needed to stall the GO signal
	 * because of reverse flow control restrictions on the module. Each process
	 * must be able to stall something 'before' it in order to preserve the
	 * chain of full reverse flow control. In the degenrate case this means
	 * stalling of the containing module for the process. Because this only
	 * happens to modules which contain a process we are guaranteed that the GO
	 * of the module is already marked as needed.
	 * 
	 * @param module
	 *            a Module
	 * @return a Set of {@link ModuleStallSource} objects
	 */
	public Set<ModuleStallSource> getModuleStallProcs(Module module) {
		if (!stalledModules.containsKey(module)) {
			return Collections.emptySet();
		}
		return Collections.unmodifiableSet(stalledModules.get(module));
	}

	/**
	 * Returns the Set of MemProcess objects tracked here.
	 */
	Set<MemProcess> getProcesses() {
		return Collections.unmodifiableSet(processes);
	}

	/**
	 * Uses dependencies to find the processes that 'control' the given ports.
	 * We use dependencies instead of real port/bus connections because
	 * dependencies explicitly relate two components whereas port/bus
	 * connections only have to satisfy the dependency, meaning that there may
	 * only be an implicit relationship between a component and the control
	 * logic for that component.
	 * <p>
	 * Generally an end point of a process is not marked as being 'in' that
	 * process (because it has 'closed' it). However, when looking for what
	 * process generates a data value that is consumed by some other component
	 * we do want the end point to report the process that it closes as the
	 * generating context. Thus we have a boolean flag to make the distinction
	 * between data and control.
	 * 
	 * @param ports
	 *            a Collection of {@link Port} objects
	 * @param isDataSearch
	 *            a boolean, true if the component should consider data from the
	 *            end point of another process as 'generated' by that process.
	 * @return a Set of MemProcess objects.
	 */
	Set<MemProcess> getProcs(Collection<Port> ports, boolean isDataSearch) {
		final Set<MemProcess> portProcs = new HashSet<MemProcess>();
		for (Port port : ports) {
			final Set<MemProcess> procsForPort = new HashSet<MemProcess>();
			// Skip ports which have no dependencies, otherwise they
			// will trigger the assert.
			if (validPort(port)) {
				assert portToProcs.containsKey(port) : "Missing port " + port
						+ " " + port.getOwner().show();
				procsForPort.addAll(portToProcs.get(port));
			}

			final Component comp = port.getOwner();
			for (Entry entry : comp.getEntries()) {
				for (Dependency dep : entry.getDependencies(port)) {
					final Bus bus = dep.getLogicalBus();
					final Component source = bus.getOwner().getOwner();
					if (isDataSearch) {
						// Look to see if the source component is the
						// end point of a process, if so, then the
						// tested component needs to mark that as a
						// consumed process
						for (MemProcess testProc : getProcesses()) {
							if (testProc.isEndPoint(source)) {
								procsForPort.add(testProc);
							}
						}
					}
				}
			}
			portProcs.addAll(procsForPort);
		}
		return portProcs;
	}

	/**
	 * Any port on a component with 0 entries, or a port with no dependencies
	 * does not factor into the scheduling and thus should be skipped. This
	 * happens at the ports of InBuf objects.
	 * 
	 * @param port
	 *            a non-null Port
	 * @return true if there is at least 1 entry on the owning component and
	 *         there is at least 1 dependency for the specified port.
	 * @throws NullPointerException
	 *             if port is null
	 */
	private boolean validPort(Port port) {
		for (Entry entry : port.getOwner().getEntries()) {
			if (entry.getDependencies(port).size() > 0) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Yes, this is a hack, this method is used by a various scheduling pieces
	 * to avoid inserting logic and control structures based on deferred
	 * constants. Any constant is inherently untimed and can thus be ignored
	 * when inserting latches, registers, control logic, etc. Eventually we
	 * would love for constant prop to be able to annotate this information, but
	 * for now, it is handled here.
	 * 
	 * @param bus
	 *            a value of type 'Bus'
	 * @return true if the bus is a constant (real or deferred) or a casted
	 *         constant
	 */
	public static boolean isUntimed(Bus bus) {
		// Unravel the cast ops. This is covering for the fact we
		// dont do symbolic constant prop!
		while (bus.getOwner().getOwner() instanceof org.xronos.openforge.lim.op.CastOp) {
			bus = ((org.xronos.openforge.lim.op.CastOp) bus.getOwner().getOwner())
					.getDataPort().getBus();
			if (bus == null)
				return false;
		}

		if (bus.getOwner().getOwner().isConstant())
			return true;
		if (bus.getValue() == null)
			System.out.println("NULL BUS " + bus + " of "
					+ bus.getOwner().getOwner().show() + " in "
					+ bus.getOwner().getOwner().showOwners());
		if (bus.getValue().isConstant())
			return true;

		return false;
	}

}// ProcessTracker
