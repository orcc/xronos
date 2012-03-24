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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * LoopBody characterizes the body of a {@link Loop}.
 * <P>
 * A LoopBody designates one of its {@link Exit Exits} as a "feedback exit."
 * When this exit is asserted, it indicates that another iteration of the loop
 * should begin. The Buses of this exit are connected to the feedback inputs of
 * data and control {@link Reg Registers}. If the body will never iterate (for
 * example, if it ends with a break) then this exit may be null.
 * <P>
 * The {@link Decision} may also be null if it is known that it will never be
 * reached (for instance, a do-loop that ends with a break).
 * <P>
 * In addition, a LoopBody identifies the input Port that provides the initial
 * value for each feedback data flow.
 * 
 * @author Stephen Edwards
 * @version $Id: LoopBody.java 2 2005-06-09 20:00:48Z imiller $
 */
public abstract class LoopBody extends Module {

	public static final Exit.Tag FEEDBACK_TAG = Exit.getTag(Exit.DONE,
			"#feedback#");
	public static final Exit.Tag COMPLETE_TAG = Exit.getTag(Exit.DONE);

	public Map portNameMap = new TwoWayMap();
	public Map busNameMap = new TwoWayMap();

	/**
	 * This boolean tracks whether or not the loop flop related to this loop
	 * body has been deemed necessary or whether it can be removed. The default
	 * behavior is to assume that it is always needed. The LoopFlopAnalysis
	 * visitor in the scheduling package may change the state of this variable
	 */
	public boolean flopNeeded = true;

	/**
	 * Constructs a new LoopBody.
	 */
	public LoopBody() {
		super();
		if (_lim.db)
			_lim.ln(_lim.LOOPS, "Creating 2 exits on Loop Body");
	}

	/**
	 * Gets the exit which signals that this block has finished, and another
	 * iteration should begin.
	 * 
	 * @return the feedback exit, or null if the loop never iterates
	 */
	public Exit getFeedbackExit() {
		return getExit(FEEDBACK_TAG);
	}

	/**
	 * Gets the exit which signals that this block has finisished, and there are
	 * no more iterations to process.
	 */
	public Exit getLoopCompleteExit() {
		return getExit(COMPLETE_TAG);
	}

	/**
	 * Tests whether this loop body will ever iterate. If not, the feedback
	 * {@link Exit} will be null.
	 */
	public boolean isIterative() {
		return (getFeedbackExit() != null && !getFeedbackExit().getPeer()
				.getEntries().isEmpty());
	}

	public void setPortName(Port port, String name) {
		portNameMap.put(name, port);
	}

	public void setBusName(Bus bus, String name) {
		busNameMap.put(name, bus);
	}

	public Port getNamedPort(String name) {
		return (Port) portNameMap.get(name);
	}

	public String getPortName(Port port) {
		return (String) portNameMap.get(port);
	}

	public Bus getNamedBus(String name) {
		return (Bus) busNameMap.get(name);
	}

	public String getBusName(Bus bus) {
		return (String) busNameMap.get(bus);
	}

	/**
	 * Attempts to remove the given data {@link Port} from this component.
	 * 
	 * @param port
	 *            the port to remove
	 * @return true if the port was removed, false if it was not found
	 */
	@Override
	public boolean removeDataPort(Port port) {
		if (super.removeDataPort(port)) {
			portNameMap.remove(port);
			return true;
		}
		return false;
	}

	/**
	 * Attempts to remove the given data {@link Bus} from this component.
	 * 
	 * @param bus
	 *            the bus to remove
	 * @return true if the bus was removed, false if it was not found
	 */
	@Override
	public boolean removeDataBus(Bus bus) {
		if (super.removeDataBus(bus)) {
			busNameMap.remove(bus);
			return true;
		}
		return false;
	}

	/**
	 * For a given Bus from the feedback Exit, gets the corresponding Port that
	 * represents the initial value.
	 * 
	 * @param feedbackBus
	 *            a bus from the feedback Exit
	 * @return the port which corresponds to the initial value of the feedback
	 *         data flow
	 */
	public abstract Port getInitalValuePort(Bus feedbackBus);

	/**
	 * Returns the {@link Decision} which controls whether this loop body will
	 * continue on each iteration.
	 * 
	 * @return the decision, or null if a decision is never reached
	 */
	public abstract Decision getDecision();

	/**
	 * returns the body of the loop
	 */
	public abstract Module getBody();

	/**
	 * Overridden by ForBody.
	 * 
	 * @return the update component, a block which the update expression
	 *         resides, in this loop
	 */
	public Module getUpdate() {
		return null;
	}

	/**
	 * returns true of the decision comes before the body of the loop, or false
	 * if after. This is needed to compute the number of iterations for loop
	 * unrolling
	 */
	public abstract boolean isDecisionFirst();

	/**
	 * Returns true if this loop body must have a feedback register (the control
	 * loop flop) to seperate iterations, if false the loop flop may be removed.
	 * 
	 * @return a boolean
	 */
	public boolean isLoopFlopNeeded() {
		return flopNeeded;
	}

	/**
	 * Used to set whether the loop control feedback register (loop flop) is
	 * necessary for correct functioning of the loopbody.
	 * 
	 * @param value
	 *            a boolean, set to false if the loop flop can be safely
	 *            removed.
	 */
	public void setLoopFlopNeeded(boolean value) {
		flopNeeded = value;
	}

	@Override
	protected void cloneNotify(Module moduleClone, Map cloneMap) {
		super.cloneNotify(moduleClone, cloneMap);
		final LoopBody clone = (LoopBody) moduleClone;

		clone.portNameMap = new TwoWayMap();
		for (Iterator iter = getPorts().iterator(); iter.hasNext();) {
			final Port port = (Port) iter.next();
			final Port clonePort = getPortClone(port, cloneMap);
			clone.portNameMap.put(clonePort, portNameMap.get(port));
		}

		clone.busNameMap = new TwoWayMap();
		for (Iterator iter = getBuses().iterator(); iter.hasNext();) {
			final Bus bus = (Bus) iter.next();
			final Bus cloneBus = getBusClone(bus, cloneMap);
			clone.busNameMap.put(cloneBus, busNameMap.get(bus));
		}
	}

	public List getBusesInOrder(Exit e) {
		List data = e.getDataBuses();
		List l = new ArrayList(data.size() + 2);
		l.add(e.getDoneBus());
		l.addAll(data);
		return l;
	}

	@Override
	public void changeExit(Exit exit, Exit.Type type) {
		super.changeExit(exit, type);

		List oldBuses = getBusesInOrder(exit);
		List newBuses = getBusesInOrder(getExit(type));
		assert oldBuses.size() == newBuses.size();

		for (int i = 0; i < oldBuses.size(); i++) {
			Bus oldBus = (Bus) oldBuses.get(i);
			Bus newBus = (Bus) newBuses.get(i);
			setBusName(newBus, getBusName(oldBus));
		}
	}
}

class TwoWayMap extends HashMap {
	@Override
	public Object put(Object key, Object value) {
		super.put(key, value);
		super.put(value, key);
		return null;
	}

	@Override
	public Object remove(Object key) {
		Object value = get(key);
		super.remove(key);
		super.remove(value);
		return null;
	}
}
