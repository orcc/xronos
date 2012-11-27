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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import org.xronos.openforge.lim.Block;
import org.xronos.openforge.lim.Component;
import org.xronos.openforge.lim.Stallboard;


/**
 * MemProcess identifies and represents one virtual process in the users design.
 * A process is a set of logic that logically operates as a contiguous whole and
 * may run (relatively) independently of other processes in the same design. A
 * MemProcess is a process which collects all logic existing between the two
 * endpoints which are the first and last access(es) to a given memory resource.
 * 
 * 
 * <p>
 * Created: Tue Sep 7 09:05:11 2004
 * 
 * @author imiller, last modified by $Author: imiller $
 * @version $Id: MemProcess.java 122 2006-03-30 18:05:17Z imiller $
 */
public class MemProcess {

	private Set<ProcessStartPoint> startPoints = null;

	private ProcessPoint endPoint = null;

	/**
	 * The Module which contains all the logic for a given process. This is the
	 * module which contains both the start and end points.
	 */
	private Component processContext = null;

	/**
	 * Generates a new MemProcess which is an abstraction of a collection of
	 * logic, denoted by start and end points, which represents the entirety of
	 * accesses to some resource (such as memory).
	 * <p>
	 * The start and end of a process must be a single component existing at the
	 * same level of hierarchy in the LIM. Because of the restrictions imposed
	 * on the LIM by the ProcessPoint (finding the critical context) and the
	 * flattening of the LIM, it should always be true that the critical context
	 * for the start and end points are resident in the same module. This is
	 * true because the relevent context is the outermost loop or branch and
	 * there should be no other containment hierarchy in a flattened lim.
	 * <p>
	 * All components in either the start or end collection must reside in a
	 * Module hierarchy in the <b>same</b> procedure. This is true becuase we
	 * must be able to identify a single Module from which all endpoints are
	 * reachable through a depth only traversal (ie not going 'up' any levels
	 * and not traversing through any calls).
	 * 
	 * @param start
	 *            a Collection of Components representing all potential 'first
	 *            accesses' to the common resource.
	 * @param end
	 *            a Collection of Components representing all potential 'last
	 *            accesses' to the common resource.
	 */
	public MemProcess(Collection<Component> start, Collection<Component> end) {
		// Find the containing context
		final Set<Stack<Component>> hierarchy = new LinkedHashSet<Stack<Component>>();
		hierarchy.addAll(buildHierarchy(start));
		hierarchy.addAll(buildHierarchy(end));

		// Keep popping off the context (top down) so long as all the
		// critical points have the same context.
		boolean matched = true;
		processContext = null;
		while (matched) {
			matched = true;
			// Stack firstStack = (Stack)hierarchy.iterator().next();
			final Component testContext = hierarchy.iterator().next().peek();
			for (Stack<Component> stack : hierarchy) {
				if (stack.peek() != testContext) {
					matched = false;
				}
			}
			if (matched) {
				// Save off the last known Block. This protects
				// agains the possibility of a first access in a
				// decision and a last access in a branch of an if.
				// Instead it pushes the context back up to the
				// containing block of the branch (example)
				// *** FIXME: I hate instanceof....
				if (testContext instanceof Block) {
					processContext = testContext;
				}
				for (Stack<Component> stack : hierarchy) {
					stack.pop();
				}
			}
		}
		assert processContext != null : "No context for process";
		startPoints = buildStartPoints(start, processContext);
		endPoint = new ProcessPoint(end, processContext);
		// One of the things we depend on in scheduling is that the
		// start and end point exist in the same Block. This ensures
		// that we do not have to 'tunnel' through hierarchy to get
		// the stall signal back to the stall point. It also ensures
		// that we will always recieve the stall signal.
		assert endPoint.getCriticalContext().getOwner() == processContext;
	}

	/**
	 * Creates a List of Stack objects representing the 'owner' hierarchy of
	 * each Component in the Collection of points
	 * 
	 * @param points
	 *            a Collection of Component objects
	 * @return a List of Stack objects, where those Stack objects contain
	 *         Components.
	 */
	private static List<Stack<Component>> buildHierarchy(
			Collection<Component> points) {
		List<Stack<Component>> hier = new ArrayList<Stack<Component>>();
		for (Component comp : points) {
			Stack<Component> owners = new Stack<Component>();
			do {
				owners.push(comp);
				comp = comp.getOwner();
			} while (comp != null);
			hier.add(owners);
		}
		return hier;
	}

	/**
	 * Build up the set of ProcessStartPoints which represent unique components
	 * in the processContext module which are the independent entry points into
	 * the process.
	 * <p>
	 * Group the start points by their owner tree. Each ProcessStartPoint must
	 * represent a unique Component or Module in the processContext and all
	 * Components in start that descent from that unique point must be in that
	 * ProcessStartPoint.
	 * 
	 * @param start
	 *            a value of type 'Collection'
	 * @param processContext
	 *            a value of type 'Component'
	 * @return a value of type 'Set'
	 */
	private static Set<ProcessStartPoint> buildStartPoints(
			Collection<Component> start, Component processContext) {
		final Map<Component, Set<Component>> contextToPoints = new HashMap<Component, Set<Component>>();
		for (Component comp : start) {
			// Supports case where component is directly in the
			// process context
			Component owner = comp;
			do {
				if (owner.getOwner() == processContext) {
					Set<Component> points = contextToPoints.get(owner);
					if (points == null) {
						points = new HashSet<Component>();
						contextToPoints.put(owner, points);
					}
					points.add(comp);
					break;
				}
				owner = owner.getOwner();
				assert owner != null : "Identified start point does not exist in process context";
			} while (owner != null);
		}

		final Set<ProcessStartPoint> startPoints = new LinkedHashSet<ProcessStartPoint>(
				contextToPoints.keySet().size());
		for (Map.Entry<Component, Set<Component>> entry : contextToPoints
				.entrySet()) {
			ProcessStartPoint startPoint = new ProcessStartPoint(
					entry.getValue(), processContext);
			assert startPoint.getCriticalContext() == entry.getKey();
			assert startPoint.getCriticalContext().getOwner() == processContext;
			startPoints.add(startPoint);
		}
		return startPoints;
	}

	/**
	 * Returns a Collection of the {@link ProcessStartPoint} representing the
	 * complete set of entries into this process.
	 * 
	 * @return a Collection of ProcessStartPoint objects
	 */
	public Collection<ProcessStartPoint> getStartPoints() {
		return Collections.unmodifiableSet(startPoints);
	}

	/**
	 * Returns true if the given {@link Component} is the critical context for
	 * one of the process start points.
	 * 
	 * @param test
	 *            a non-null Component
	 * @return true if the Component is the critical context for a start point
	 *         of this process.
	 * @throws IllegalArgumentException
	 *             if test is null
	 */
	public boolean isStartPoint(Component test) {
		if (test == null) {
			throw new IllegalArgumentException(
					"Cannot test null component for start point");
		}

		for (ProcessPoint point : startPoints) {
			if (point.getCriticalContext() == test) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Returns the ProcessPoint representing the termination of this process.
	 * 
	 * @return a value of type 'ProcessPoint'
	 */
	public ProcessPoint getEndPoint() {
		return endPoint;
	}

	/**
	 * Returns true if the given {@link Component} is the critical context for
	 * the process end point.
	 * 
	 * @param test
	 *            a non-null Component
	 * @return true if the Component is the critical context for the end point
	 *         of this process.
	 * @throws IllegalArgumentException
	 *             if test is null
	 */
	public boolean isEndPoint(Component test) {
		if (test == null) {
			throw new IllegalArgumentException(
					"Cannot test null component for start point");
		}

		return (getEndPoint().getCriticalContext() == test);
	}

	/**
	 * Returns true if the the Component (probably a Module) is the context for
	 * this process, that is, the module which contains all the components
	 * (start, end, and all others) for the process.
	 * 
	 * @return true if the given Component is the process context
	 */
	public boolean isProcessContext(Component test) {
		return (test == processContext);
	}

	/**
	 * Sets the given Stallboard as the stalling entity for the starting entry
	 * point identified by the component. The component must correspond to one
	 * of the ProcessStartPoints for this process or an exception will be
	 * thrown. If {@link #isStartPoint} returns true then this method will
	 * successfully complete
	 * 
	 * @param comp
	 *            a non-null Component which is the critical context of a
	 *            ProcessStartPoint of this process.
	 * @param stbd
	 *            a non-null Stallboard
	 */
	public void setStallPoint(Component comp, Stallboard stbd) {
		for (ProcessStartPoint point : startPoints) {
			if (point.getCriticalContext() == comp) {
				point.setStallPoint(stbd);
				return;
			}
		}
		throw new IllegalArgumentException(
				"Component is not an entry point of this process");
	}

	public void addStallSignal(Component comp) {
		for (ProcessStartPoint startPoint : getStartPoints()) {
			startPoint.addStallSignal(comp);
		}
	}

	public void addStallSignals(Collection<Component> comps) {
		for (Component component : comps) {
			addStallSignal(component);
		}
	}

	@Override
	public String toString() {
		String ret = super.toString();
		ret = ret.substring(ret.lastIndexOf(".") + 1);
		return ret;
	}

	public String debug() {
		String ret = "\n" + toString();
		for (ProcessStartPoint processStartPoint : getStartPoints()) {
			ret += "\n";
			ret += "\tstart point: " + processStartPoint.toString();
		}
		ret += "\n";
		ret += "\tend point " + endPoint.toString();
		ret += "\n";
		return ret;
	}

	/** FOR TESTING ONLY!!! */
	protected MemProcess() {
	}

}// MemProcess
