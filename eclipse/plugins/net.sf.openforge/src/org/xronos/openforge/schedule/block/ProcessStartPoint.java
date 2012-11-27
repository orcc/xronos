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
import java.util.HashSet;
import java.util.Set;

import org.xronos.openforge.lim.Component;
import org.xronos.openforge.lim.Stallboard;


/**
 * ProcessStartPoint denotes one point of entry (control path) into a scheduling
 * process. In addition to general functionality of a {@link ProcessPoint} it
 * includes methods to set/get the stall point for this entry point. Each
 * ProcessStartPoint has a unique {@link Stallboard} that is used to stall the
 * process.
 * 
 * 
 * <p>
 * Created: Thu Oct 28 15:38:40 2004
 * 
 * @author imiller, last modified by $Author: imiller $
 * @version $Id: ProcessStartPoint.java 2 2005-06-09 20:00:48Z imiller $
 */
public class ProcessStartPoint extends ProcessPoint {

	/**
	 * This field will be set during scheduling when the actual component is
	 * inserted into the LIM for implementing the 'stall'. It is cached here in
	 * the process so that we can add the necessary signals to it once the
	 * process is closed.
	 */
	private Stallboard stallPoint = null;

	/**
	 * The Set of Components which must stall this start point, as determined
	 * during scheduling
	 */
	private final Set<Component> stallComponents = new HashSet<Component>();

	/**
	 * Constructs a new ProcessStartPoint, or identifier for one point of entry
	 * into a process.
	 * 
	 * @param criticalPoints
	 *            a Collection of Component objects. All the Components in
	 *            criticalPoints must be reachable from the processContext (in a
	 *            downward traversal).
	 * @param processContext
	 *            a Component which defines the containing module from which all
	 *            endpoints of the process descend. This is the 'deepest' module
	 *            that contains all endpoints for a given process.
	 */
	public ProcessStartPoint(Collection<Component> criticalPoints,
			Component processContext) {
		super(criticalPoints, processContext);
	}

	/**
	 * Defines the implementation of the stall point for this ProcessStartPoint.
	 * The stall point is immutable once set, therefore this method can only be
	 * called once per ProcessStartPoint.
	 * 
	 * @param scbd
	 *            a non-null Stallboard
	 */
	public void setStallPoint(Stallboard stbd) {
		if (stbd == null)
			throw new IllegalArgumentException(
					"Cannot define stall point as null");
		if (stallPoint != null)
			throw new IllegalStateException(
					"Only one stall point can be defined per process");
		if (_block.db)
			_block.ln("Set " + stbd + " on " + this);
		stallPoint = stbd;
	}

	/**
	 * Returns the stall point ({@link Stallboard}) which is defined for this
	 * process start point, may be null if the stall point has not yet been
	 * specified for this point.
	 * 
	 * @return a Stallboard, may be null
	 */
	public Stallboard getStallPoint() {
		return stallPoint;
	}

	/**
	 * Registers the given component as providing one stall signal to this entry
	 * point.
	 * 
	 * @param comp
	 *            a non-null Component
	 */
	public void addStallSignal(Component comp) {
		stallComponents.add(comp);
	}

	/**
	 * Returns a Collection of Components whose corresponding control signals
	 * must be used to stall this entry point.
	 * 
	 * @return a Set of {@link Component} objects
	 */
	public Set<Component> getStallSignals() {
		return Collections.unmodifiableSet(stallComponents);
	}

}// ProcessStartPoint
