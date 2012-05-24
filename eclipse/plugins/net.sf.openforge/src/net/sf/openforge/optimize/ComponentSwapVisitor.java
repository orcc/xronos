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

package net.sf.openforge.optimize;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.sf.openforge.lim.Block;
import net.sf.openforge.lim.Bus;
import net.sf.openforge.lim.Component;
import net.sf.openforge.lim.Dependency;
import net.sf.openforge.lim.Entry;
import net.sf.openforge.lim.Exit;
import net.sf.openforge.lim.Module;
import net.sf.openforge.lim.Operation;
import net.sf.openforge.lim.Port;
import net.sf.openforge.lim.SafeFilteredVisitor;
import net.sf.openforge.lim.Value;
import net.sf.openforge.lim.op.Constant;
import net.sf.openforge.util.naming.ID;

/**
 * ComponentSwapVisitor is an extention of the SafeFilteredVisitor which
 * provides some utility methods for performing constant prop.
 * 
 * 
 * <p>
 * Created: Fri Jul 12 10:41:32 2002
 * 
 * @author imiller
 * @version $Id: ComponentSwapVisitor.java 61 2005-11-17 18:07:21Z imiller $
 */
public class ComponentSwapVisitor extends SafeFilteredVisitor {

	/** Set to true if the visitor has modified the LIM */
	protected boolean modified = false;
	protected int replacedNodeCount = 0;
	protected int removedNodeCount = 0;

	/** Set to true if the visitor did modify the LIM */
	protected boolean did_modify = false;
	protected int replacedNodeCountTotal = 0;
	protected int removedNodeCountTotal = 0;

	/**
	 * Implemented solely to provide debug output of which components are being
	 * traversed.
	 * 
	 * @param c
	 *            a value of type 'Component'
	 */
	@Override
	public void preFilterAny(Component c) {
		if (_optimize.db)
			_optimize.ln("Analyzing component: " + c + "/" + ID.glob(c) + "/"
					+ ID.log(c));
	}

	public void swapComponents(Component orig, Component replacement) {
		mapExact(orig, replacement);

		// If the owner is a block use the much friendlier replace method.
		if (orig.getOwner() instanceof Block) {
			if (((Block) orig.getOwner()).replaceComponent(orig, replacement)) {
				setModified(true);
			}
		} else {
			if (orig.getOwner().removeComponent(orig)) {
				setModified(true);
			}
			orig.getOwner().addComponent(replacement);
		}

		orig.disconnect();

		replacement.setOptionLabel(orig.getOptionLabel());

		replacedNodeCount++;
		replacedNodeCountTotal++;
	}

	/**
	 * Used to set the 'isModified' flag.
	 * 
	 * @param value
	 *            a value of type 'boolean'
	 */
	protected void setModified(boolean value) {
		modified = value;
		did_modify = value;
	}

	/**
	 * Returns true if the last iteration of this Visitor modified the LIM.
	 * 
	 * @return true if this Visitor has modified the LIM since it was last
	 *         {@link ComponentSwapVisitor#reset}
	 */
	protected boolean isModified() {
		return modified;
	}

	/**
	 * Return true if any of the iteration of this Visitor modified the LIM.
	 * 
	 * @return true if the Visitor did modify the LIM during the run.
	 */
	public boolean didModify() {
		return did_modify;
	}

	/**
	 * Reset's the necessary flags to begin another iteration of this visitor.
	 * 
	 */
	protected void reset() {
		modified = false;
	}

	public void clear() {
		did_modify = false;
		clearCount();
	}

	protected void clearCount() {
		replacedNodeCount = 0;
		removedNodeCount = 0;
	}

	/**
	 * used, ie in DivideOpRule, to indicate that a component was replaced
	 * without calling SwapComponents or replaceComponent
	 */
	public void replacedNodeCountIncrement() {
		replacedNodeCount++;
		replacedNodeCountTotal++;
	}

	/**
	 * used, ie in DivideOpRule, to indicate that a component was removed
	 */
	public void removedNodeCountIncrement() {
		removedNodeCount++;
		removedNodeCountTotal++;
	}

	public int getReplacedNodeCount() {
		return replacedNodeCount;
	}

	public int getRemovedNodeCount() {
		return removedNodeCount;
	}

	public int getReplacedNodeCountTotal() {
		return replacedNodeCountTotal;
	}

	public int getRemovedNodeCountTotal() {
		return removedNodeCountTotal;
	}

	/**
	 * Returns a Number representation of the constant value applied to the port
	 * via it's logical dependency, or null if the port is not driven by a
	 * constant or is not analyzable. This method only analyzes ports which are
	 * driven by a single logical bus whose Value is constant (
	 * {@link Value#isConstant}).
	 * 
	 * @param port
	 *            the {@link Port} to analyze.
	 * @return the Number representation of the constant value or null if the
	 *         port could not be fully analyzed or is driven by a non-constant
	 *         bus.
	 */
	public static Number getPortConstantValue(Port port) {
		return getPortConstantValue(port, new ArrayList<Entry>(port.getOwner()
				.getEntries()));
	}

	/**
	 * Returns a Number representation of the constant value applied to the port
	 * via it's logical dependency, or null if the port is not driven by a
	 * constant or is not analyzable. This method only analyzes ports which are
	 * driven by a single logical bus whose Value is constant (
	 * {@link Value#isConstant}).
	 * 
	 * @param port
	 *            the {@link Port} to analyze.
	 * @param the
	 *            entry to use
	 * @return the Number representation of the constant value or null if the
	 *         port could not be fully analyzed or is driven by a non-constant
	 *         bus.
	 */
	public static Number getPortConstantValue(Port port, Entry entry) {
		return getPortConstantValue(port, Collections.singletonList(entry));
	}

	private static Number getPortConstantValue(Port port, List<Entry> entries) {
		Component owner = port.getOwner();
		assert owner != null;
		Set<Bus> logicalBuses;
		if (port.getBus() == null) {
			logicalBuses = new HashSet<Bus>();
			for (Entry entry : entries) {
				for (Dependency dep : entry.getDependencies(port)) {
					logicalBuses.add(dep.getLogicalBus());
				}
			}
		} else {
			logicalBuses = Collections.singleton(port.getBus());
		}

		if (logicalBuses.size() != 1) {
			if (_optimize.db)
				_optimize.ln(_optimize.FULL_CONST, "\t" + owner + " has "
						+ logicalBuses.size()
						+ " logical dependencies on port " + port);
			return null;
		}

		Bus logicalBus = logicalBuses.iterator().next();

		Value busValue = logicalBus.getValue();

		if (busValue != null && busValue.isConstant() && !busValue.isDontCare()) {
			// check for bus that comes from a floating point op
			if (isFPOpBus(logicalBus)) {
				return makeFPNumber(logicalBus, busValue);
			}

			Value value = busValue;
			long lvalue = value.getValueMask();
			if (_optimize.db)
				_optimize.ln(_optimize.FULL_CONST, "\tConstant Valued: "
						+ lvalue);
			return new Long(lvalue);
		} else {
			if (_optimize.db)
				_optimize.ln(_optimize.FULL_CONST, "\tNot constant");
			return null;
		}
	}

	/**
	 * Maps the ports, buses and exits from the source component to the target
	 * component _exactly_ 1 to 1. This means that the source and target must
	 * have exactly the same number of ports, buses, and exits.
	 * 
	 * @param source
	 *            the source Component from which characteristics are taken
	 * @param target
	 *            the target Component to which the characteristics are copied.
	 */
	public static void mapExact(Component source, Component target) {
		Map<Port, Port> portCorrelation = new HashMap<Port, Port>();
		Map<Bus, Bus> busCorrelation = new HashMap<Bus, Bus>();
		Map<Exit, Exit> exitCorrelation = new HashMap<Exit, Exit>();

		for (Iterator<Port> sourceIter = source.getPorts().iterator(), targetIter = target
				.getPorts().iterator(); sourceIter.hasNext();) {
			portCorrelation.put(sourceIter.next(), targetIter.next());
		}

		for (Iterator<Exit> sourceExitIter = source.getExits().iterator(), targetExitIter = target
				.getExits().iterator(); sourceExitIter.hasNext();) {
			Exit sourceExit = sourceExitIter.next();
			Exit targetExit = targetExitIter.next();
			exitCorrelation.put(sourceExit, targetExit);

			for (Iterator<Bus> sourceIter = sourceExit.getBuses().iterator(), targetIter = targetExit
					.getBuses().iterator(); sourceIter.hasNext();) {
				busCorrelation.put(sourceIter.next(), targetIter.next());
			}
		}

		replaceConnections(portCorrelation, busCorrelation, exitCorrelation);
	}

	/**
	 * Duplicates the connectivity (in terms of entries, dependencies, and
	 * physical connections) of the ports contained in the portCorrelation Map
	 * and replaces the connections of buses in the busCorrelation Map. This
	 * method assumes that the values in the correlation map are
	 * ports/buses/exits on nodes which have not yet been connected (ie are
	 * being inserted into the graph).
	 * <P>
	 * Port connectivity is copied by creating new dependencies for each port
	 * and setting logical and structural buses to be the same as the original
	 * port. The source port must have only 1 entry, however.
	 * <P>
	 * Bus connectivity is replaced by setting the logical and/or structrual
	 * buses in each dependency of the source bus to be the new bus.
	 * <P>
	 * Exit's are correlated so that any entry which used to be 'driven' by the
	 * original exit can be set up to now be driven by the new exit.
	 * 
	 * @param portCorrelation
	 *            a 'Map' of original {@link Port} to new {@link Port}
	 * @param busCorrelation
	 *            a 'Map' of original {@link Bus} to new {@link Bus}
	 * @param exitCorrelation
	 *            a 'Map' of original {@link Exit} to new {@link Exit}
	 */
	public static void replaceConnections(Map<Port, Port> portCorrelation,
			Map<Bus, Bus> busCorrelation, Map<Exit, Exit> exitCorrelation) {
		// Copy port connections/dependencies
		for (Port source : portCorrelation.keySet()) {
			Port target = portCorrelation.get(source);
			mirrorPort(source, target);
		}

		// REPLACE bus connections/dependencies
		for (Bus source : busCorrelation.keySet()) {
			Bus target = busCorrelation.get(source);

			// Logical. Replace the current logical dependencies with
			// a new dependency.
			List<Dependency> logical = new ArrayList<Dependency>(
					source.getLogicalDependents());
			for (Dependency dep : logical) {
				Dependency newDep = (Dependency) dep.clone();
				newDep.setLogicalBus(target);
				dep.getEntry().addDependency(dep.getPort(), newDep);
				dep.zap();
			}

			// Physical
			for (Port port : (new ArrayList<Port>(source.getPorts()))) {
				port.setBus(target);
			}
		}

		// Replace the given exit with the new exit as the
		// 'drivingExit' for anything that it drives.
		for (Exit source : exitCorrelation.keySet()) {
			Exit target = exitCorrelation.get(source);

			// for (Iterator entryIter = source.getDrivenEntries().iterator();
			// entryIter.hasNext();)
			// {
			// ((Entry)entryIter.next()).setDrivingExit(target);
			// }

			// A ConcurrentModificationException will be thrown if we don't
			// obtain the driven entries first.
			Set<Entry> drivenEntries = new HashSet<Entry>(
					source.getDrivenEntries());
			for (Entry entry : drivenEntries) {
				entry.setDrivingExit(target);
			}

		}
	}

	/**
	 * Does a 1:1 copying of the source port's dependencies/entries to the
	 * target port. Entries are matched up by position in their owners list and
	 * new entries are created on the target if none exits.
	 * 
	 * @param source
	 *            The source Port from which characteristics are copied.
	 * @param target
	 *            The target Port to which characteristics are copied.
	 */
	private static void mirrorPort(Port source, Port target) {
		Component sourceOwner = source.getOwner();
		Component targetOwner = target.getOwner();
		assert sourceOwner != null : "Source port must have owner";
		assert targetOwner != null : "Target port must have owner";

		assert (targetOwner.getEntries().size() == 0)
				|| (targetOwner.getEntries().size() == sourceOwner.getEntries()
						.size()) : "Source and target must have same number of entries to 'mirror'";

		for (Iterator<Entry> sourceEntryIter = sourceOwner.getEntries()
				.iterator(), targetEntryIter = targetOwner.getEntries()
				.iterator(); sourceEntryIter.hasNext();) {
			Entry sourceEntry = sourceEntryIter.next();
			Entry targetEntry;
			if (targetEntryIter.hasNext())
				targetEntry = targetEntryIter.next();
			else
				targetEntry = targetOwner.makeEntry(sourceEntry
						.getDrivingExit());

			copyPort(source, sourceEntry, target, targetEntry);
		}

		if (source.getBus() != null) {
			assert target.getBus() == null : "target already has a bus connected";
			target.setBus(source.getBus());
		}
	}

	/**
	 * Short-circuits a given {@link Port Port's} dependencies and connections
	 * to the dependents of a {@link Bus} on the same {@link Component}. This
	 * essentially eliminates the path between the port and the bus. Following
	 * this, all connections and dependencies of the port and bus are
	 * eliminated.
	 * 
	 * @param port
	 *            the port whose connections are moved to the dependents of
	 *            <code>bus</code>
	 * @param bus
	 *            the bus whose dependents will receive the <code>port's</code>
	 *            connections
	 */
	public static void shortCircuit(Port port, Bus bus) {
		/*
		 * Make sure the Port and Bus are at opposit ends of the same Component
		 * or Module.
		 */
		if (port.getOwner() != bus.getOwner().getOwner()) {
			throw new IllegalArgumentException("Port and Bus Components differ");
		}

		/*
		 * Copy the Port's connections and dependencies to each of the Bus's
		 * dependents.
		 */
		for (Dependency dependent : bus.getLogicalDependents()) {
			final Entry dependentEntry = dependent.getEntry();
			copyPortConnections(port, dependent.getPort(), dependentEntry);
		}

		/*
		 * Disconnect the Port and Bus.
		 */
		port.disconnect();
		for (Entry entry : port.getOwner().getEntries()) {
			entry.clearDependencies(port);
		}
		bus.disconnect();
		bus.clearLogicalDependents();
	}

	/**
	 * Copies recreates all of the {@link Dependency dependencies} of one given
	 * {@link Port} on another given {@link Port}. The {@link Bus} connection
	 * value is also copied.
	 * 
	 * @param sourcePort
	 *            the port whose connections are copied
	 * @param targetPort
	 *            the port on which the copied connections are created
	 * @param targetEntry
	 *            the entry used to create dependencies for
	 *            <code>targetPort</code>
	 */
	private static void copyPortConnections(Port sourcePort, Port targetPort,
			Entry targetEntry) {
		for (Entry sourceEntry : sourcePort.getOwner().getEntries()) {
			for (Dependency sourceDependency : sourceEntry
					.getDependencies(sourcePort)) {
				final Dependency targetDependency = (Dependency) sourceDependency
						.clone();
				targetDependency
						.setLogicalBus(sourceDependency.getLogicalBus());
				targetEntry.addDependency(targetPort, targetDependency);
			}
		}

		targetPort.setBus(sourcePort.getBus());
	}

	/**
	 * copy dependencies from the source port (as found in the sourceEntry) to
	 * the target port (in the targetEntry).
	 */
	private static void copyPort(Port source, Entry sourceEntry, Port target,
			Entry targetEntry) {
		for (Dependency sourceDep : sourceEntry.getDependencies(source)) {
			Dependency targetDep = (Dependency) sourceDep.clone();
			targetDep.setLogicalBus(sourceDep.getLogicalBus());
			targetEntry.addDependency(target, targetDep);
		}
	}

	/**
	 * Reconnects any dependency attached to the Go port to each logical
	 * dependent of the done bus.
	 * 
	 * @param c
	 *            a value of type 'Component'
	 */
	public static void wireControlThrough(Component c) {
		for (Exit exit : c.getExits()) {
			shortCircuit(c.getGoPort(), exit.getDoneBus());
		}
	}

	/**
	 * Removes the component from it's owner after ensuring that the component's
	 * exit(s) is not listed as the driving exit of any entries.
	 * 
	 * @param c
	 *            a value of type 'Component'
	 * @return true if the component was removed from the owner, false if the
	 *         component has no owner or if it could not be removed from its
	 *         owner.
	 */
	public static boolean removeComp(Component c) {
		if (_optimize.db)
			_optimize.ln("\tRemoving " + c.toString());

		// Before we remove it, make sure it won't report that
		// it's still driving an entry....
		for (Exit exit : c.getExits()) {
			// assert exit.getDrivenEntries().size()==0;
			if (_optimize.db) {
				if (exit.getDrivenEntries().size() > 0) {
					_optimize
							.ln(_optimize.DEAD_CODE,
									"WARNING.  Exit has "
											+ exit.getDrivenEntries().size()
											+ " entries that still report it as their driving exit");
				}
			}

			for (Entry entry : new ArrayList<Entry>(exit.getDrivenEntries())) {
				entry.setDrivingExit(null);
			}
		}

		return c.getOwner() != null && c.getOwner().removeComponent(c);
	}

	/**
	 * Replaced the given component with the specified constant, but does NOT do
	 * any reverse traversal to delete 'dangling' nodes. We will rely on the
	 * code cleanup pass to do this for us.
	 * 
	 * @param comp
	 *            a value of type 'Component'
	 * @param constant
	 *            a value of type 'Constant'
	 */
	public void replaceComponent(Component comp, Constant constant) {
		if (_optimize.db)
			_optimize.ln(_optimize.FULL_CONST, "FULL CONST.  " + comp
					+ " replaced with " + constant);

		replacedNodeCount++;
		replacedNodeCountTotal++;

		Module owner = comp.getOwner();
		assert owner != null : "Cannot replace a component which is not contained in a module";

		// map the dependencies/connections.
		Map<Port, Port> portCorrelation = new HashMap<Port, Port>();
		portCorrelation.put(comp.getClockPort(), constant.getClockPort());
		portCorrelation.put(comp.getResetPort(), constant.getResetPort());
		portCorrelation.put(comp.getGoPort(), constant.getGoPort());

		assert comp.getExits().size() == 1 : "Only expecting one exit on node to be replaced";
		Exit exit = comp.getExits().iterator().next();
		Map<Bus, Bus> busCorrelation = new HashMap<Bus, Bus>();
		Map<Exit, Exit> exitCorrelation = new HashMap<Exit, Exit>();
		if (!exit.getDataBuses().isEmpty()) {
			assert exit.getDataBuses().size() == 1 : "Only expecting one data bus on component to be replaced: "
					+ comp + " data bus count=" + exit.getDataBuses().size();

			busCorrelation.put(exit.getDataBuses().get(0),
					constant.getValueBus());
		}

		busCorrelation.put(exit.getDoneBus(), constant.getOnlyExit()
				.getDoneBus());

		exitCorrelation.put(exit, constant.getOnlyExit());

		replaceConnections(portCorrelation, busCorrelation, exitCorrelation);

		moduleReplace(comp, constant);

		comp.disconnect();
		replacedNodeCount++;
		replacedNodeCountTotal++;

	}

	protected void moduleReplace(Component orig, Component replacement) {
		// If the owner is a block use the much friendlier replace method.
		if (orig.getOwner() instanceof Block) {
			if (((Block) orig.getOwner()).replaceComponent(orig, replacement)) {
				setModified(true);
			}
		} else {
			Module owner = orig.getOwner();
			if (orig.getOwner().removeComponent(orig)) {
				setModified(true);
			}
			owner.addComponent(replacement);
		}
		replacement.setOptionLabel(orig.getOptionLabel());
	}

	protected void moduleInsert(Component location, Component toInsert) {
		// If the owner is a block use the much friendlier insert method.
		if (location.getOwner() instanceof Block) {
			Block block = (Block) location.getOwner();
			int position = block.getSequence().indexOf(location);
			block.insertComponent(toInsert, position);
			setModified(true);
		} else {
			location.getOwner().addComponent(toInsert);
		}
	}

	/**
	 * Checks if the Operation whichs owns the bus operates on floating point
	 * constant(s).
	 * 
	 * @param bus
	 *            an operation bus
	 * 
	 * @return true if the bus belongs to a floating point op
	 */
	private static boolean isFPOpBus(Bus bus) {
		if (bus.isFloat()) {
			return true;
		}
		if (bus.getOwner().getOwner() instanceof Operation
				&& ((Operation) bus.getOwner().getOwner()).isFloat()) {
			return true;
		}

		return false;
	}

	/**
	 * Creates a floating point number from a bus value.
	 * 
	 * @param bus
	 *            an operation bus
	 * @param value
	 *            bus value
	 * 
	 * @return A floating point number
	 */
	private static Number makeFPNumber(Bus bus, Value value) {
		if (value.getSize() <= 32) {
			return new Float(Float.intBitsToFloat((int) value.getValueMask()));
		}

		return new Double(Double.longBitsToDouble(value.getValueMask()));
	}

	/**
	 * Converts a Number to the representation of that number in bits, stored in
	 * a long by calling longValue on any integer types and using the
	 * 'toRawBits' methods for floats and doubles.
	 * 
	 * @param number
	 *            a value of type 'Number'
	 * @return a value of type 'long'
	 */
	protected static long numberToLongBits(Number number) {
		long longValue;
		if (number instanceof Double)
			longValue = Double.doubleToRawLongBits(number.doubleValue());
		else if (number instanceof Float)
			longValue = Float.floatToRawIntBits(number.floatValue()) & 0x00000000FFFFFFFFL;
		else
			longValue = number.longValue();
		return longValue;
	}

}// ComponentSwapVisitor
