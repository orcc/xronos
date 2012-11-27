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
package org.xronos.openforge.lim;

import org.xronos.openforge.lim.util.PostScheduleCallback;
import org.xronos.openforge.schedule.LatencyTracker;

/**
 * A ResourceDependency is a {@link ControlDependency ControlDependency} between
 * two {@link Component Components} that access the same mutually exclusive
 * resource. The {@link Bus Bus} represents the signal that is asserted when the
 * predecessor access has completed or has been skipped; in other words, when it
 * is safe for the downstream accessor to execute.
 * <P>
 * Included is a number of clocks by which the successor should delay before
 * accessing the resource.
 * 
 * @author Stephen Edwards
 * @version $Id: ResourceDependency.java 538 2007-11-21 06:22:39Z imiller $
 */
public class ResourceDependency extends ControlDependency {

	private int delayClocks;

	/**
	 * Constructs a ResourceDependency.
	 * 
	 * @param logicalBus
	 *            the done bus on which the go port logically depends
	 * @param clocks
	 *            the number of clocks by which the dependent component should
	 *            delay its execution
	 */
	public ResourceDependency(Bus logicalBus, int clocks) {
		super(logicalBus);
		delayClocks = clocks;
	}

	/**
	 * Returns a new resourcedependency with the same number of delay clocks as
	 * this one.
	 */
	@Override
	public Dependency createSameType(Bus logicalBus) {
		return new ResourceDependency(logicalBus, getDelayClocks());
	}

	/**
	 * Gets the number of clocks by which the dependent component should delay
	 * before accessing the resource.
	 */
	@Override
	public int getDelayClocks() {
		return delayClocks;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof ResourceDependency && super.equals(obj)) {
			ResourceDependency dep = (ResourceDependency) obj;
			return getDelayClocks() == dep.getDelayClocks();
		}
		return false;
	}

	@Override
	public int hashCode() {
		return super.hashCode() + getDelayClocks();
	}

	/**
	 * A particular resource dependency used to indicate that the delay clocks
	 * is a delta between the GO conditions of the source and sink rather than
	 * the DONE to GO conditions. However, if default handling is applied (ie
	 * DONE to GO scheduling) the circuit will still be correct, but may be sub
	 * optimal.
	 */
	public static class GoToGoDep extends ResourceDependency implements
			PostScheduleCallback {
		private boolean preconditionIsValid = true;

		public GoToGoDep(Bus logicalBus, int clocks) {
			super(logicalBus, clocks);
		}

		public boolean preconditionIsValid() {
			return preconditionIsValid;
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof ResourceDependency.GoToGoDep
					&& super.equals(obj)) {
				ResourceDependency dep = (ResourceDependency) obj;
				return getDelayClocks() == dep.getDelayClocks();
			}
			return false;
		}

		@Override
		public int hashCode() {
			return super.hashCode() - 1;
		}

		@Override
		public void postSchedule(LatencyTracker lt, Component comp) {
			// Once the component has been scheduled determine if the
			// precondition still holds. Specifically, the scheduled
			// component (source of the dep) must have it's GO latency
			// equal to the GO of the module (max latency of 0)
			Latency latency = lt.getLatency(comp);
			if (latency == null)
				throw new IllegalStateException(
						"Could not determine latency of scheduled component (source of resource dependency)");
			if (latency.getMaxClocks() != 0) {
				preconditionIsValid = false;
			}
			comp.removePostScheduleCallback(this);
		}
	}
}
