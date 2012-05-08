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

import net.sf.openforge.lim.memory.Allocation;
import net.sf.openforge.lim.op.Constant;
import net.sf.openforge.util.naming.ID;

/**
 * A Task is a thread of execution within a {@link Design}. The executable
 * contents of a Task are expressed as a {@link Call} to a {@link Procedure}.
 * 
 * @author Stephen Edwards
 * @version $Id: Task.java 121 2006-03-27 19:58:31Z imiller $
 */
public class Task extends ID implements Visitable, Cloneable {

	public static final int INDETERMINATE_GO_SPACING = -1;

	private Call call;

	/**
	 * The constant which is hooked to the hidden argument which identifies the
	 * instance on which this entry method is applied.
	 */
	private Constant thisConstant;

	/**
	 * An identifier used to associate this Task with the target object instance
	 * that it is applied to, or null if the call is to a static method.
	 */
	private final Allocation memoryKey;

	/** The max gate depth */
	private int maxGateDepth = 0;

	// Deprecated???
	private final boolean isAutomatic;

	/**
	 * Set to true if a kicker is required to start this task off after reset.
	 */
	private boolean kickerRequired = false;

	/** Indicator of whether this task was balanced during scheduling */
	private boolean isBalanced = false;

	/**
	 * The go spacing of this task, as number of clock cycles necessary between
	 * successive assertions of data/go.
	 */
	private int goSpacing = INDETERMINATE_GO_SPACING;

	/*
	 * tbd. Do we need subclasses for explicit vs. implicit start?
	 */

	public Task(Call call, Allocation memoryKey, boolean isAutomatic) {
		this.call = call;
		this.memoryKey = memoryKey;
		this.isAutomatic = isAutomatic;
	}

	public Task(Call call, Allocation memoryKey) {
		this(call, memoryKey, false);
	}

	public Task(Call call) {
		this(call, null);
	}

	public Task() {
		this(null, null);
	}

	public Allocation getMemoryKey() {
		return memoryKey;
	}

	/**
	 * Adds the given constant to this Task and attaches the constant's value
	 * bus to the Port via dependencies and physical connection.
	 * 
	 * @param constant
	 *            a value of type 'Constant'
	 */
	public void setHiddenConstant(Constant constant) {
		assert thisConstant == null : "Can only set the hidden constant once";
		thisConstant = constant;
		connectConstant(call, thisConstant);
	}

	public Constant getThisConstant() {
		return thisConstant;
	}

	@Override
	public void accept(Visitor vis) {
		vis.visit(this);
	}

	public int getMaxGateDepth() {
		return maxGateDepth;
	}

	public void setMaxGateDepth(int maxGateDepth) {
		this.maxGateDepth = maxGateDepth;
	}

	public Call getCall() {
		return call;
	}

	public void setCall(Call call) {
		this.call = call;
	}

	public void setBalanced(boolean value) {
		isBalanced = value;
	}

	public boolean isBalanced() {
		return isBalanced;
	}

	/**
	 * Sets the identified number of cycles that must exist between assertions
	 * of GO for this task in order to maintain data integrity.
	 */
	public void setGoSpacing(int value) {
		goSpacing = value;
	}

	/**
	 * Returns the minimum number of clock cycles that must occur between
	 * consecutive assertions of GO to this task. Will return
	 * {@link Task#INDETERMINATE_GO_SPACING} if not calculated or if the minimum
	 * spacing could not be determined.
	 */
	public int getGoSpacing() {
		return goSpacing;
	}

	/**
	 * Gets the resources accessed within this task.
	 * 
	 * @return a collection of {@link Resource}
	 */
	public Collection<Resource> getAccessedResources() {
		return getCall().getAccessedResources();
	}

	/**
	 * <code>setKickerRequired</code> is used to indicate whether an
	 * {@link Kicker} is required to initiate this Task after reset.
	 * 
	 * @param value
	 *            a <code>boolean</code> value
	 */
	public void setKickerRequired(boolean value) {
		kickerRequired = value;
	}

	/**
	 * <code>isKickerRequired</code> returns true if a {@link Kicker} is needed
	 * to initiate this Task after reset.
	 * 
	 * @return a <code>boolean</code> value
	 */
	public boolean isKickerRequired() {
		return kickerRequired;
	}

	/**
	 * Returns a copy of this Task which contains a new copy of the contained
	 * Call.
	 * 
	 * @return a Task object.
	 * @exception CloneNotSupportedException
	 *                if an error occurs
	 */
	@Override
	public Object clone() throws CloneNotSupportedException {
		throw new CloneNotSupportedException();
	}

	private static void connectConstant(Call call, Constant constant) {
		assert call.getThisPort() != null : "Cannot set hidden constant on non virtual entry methods";
		Entry entry;
		if (call.getEntries().size() == 0) {
			entry = call.makeEntry(null);
		} else {
			entry = call.getEntries().get(0);
		}

		Dependency dep = new DataDependency(constant.getValueBus());
		entry.addDependency(call.getThisPort(), dep);
		// Reconcile the Values on the Port and bus before connecting.
		// call.getThisPort().updateValue(constant.getValueBus().getValue());
		call.getThisPort().setBus(constant.getValueBus());
	}

	public boolean isAutomatic() {
		return isAutomatic;
	}

	@Override
	public String toString() {
		return sourceName + "[call=" + call + ", thisConstant=" + thisConstant
				+ ", memoryKey=" + memoryKey + ", maxGateDepth=" + maxGateDepth
				+ ", isAutomatic=" + isAutomatic + ", kickerRequired="
				+ kickerRequired + ", isBalanced=" + isBalanced
				+ ", goSpacing=" + goSpacing + "]";
	}
}
