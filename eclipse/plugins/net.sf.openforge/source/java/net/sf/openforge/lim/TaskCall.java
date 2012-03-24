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

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import net.sf.openforge.lim.io.SimpleInternalPin;
import net.sf.openforge.lim.io.SimplePin;
import net.sf.openforge.lim.io.SimplePinRead;
import net.sf.openforge.lim.io.SimplePinWrite;
import net.sf.openforge.util.naming.ID;

/**
 * A TaskCall is an atomic node in the LIM which implements the 'firing' of a
 * top level design {@link Task}. Specifically, the TaskCall has a target Task
 * which is enabled when the Go port of the TaskCall is enabled. The TaskCall
 * stalls until the target task issues a Done signal. The Latency of this
 * TaskCall is the same as the Latency of the called Task.
 * 
 * @author imiller
 * @version $Id: TaskCall.java 153 2006-06-28 19:18:03Z imiller $
 */
public class TaskCall extends Module {

	/** The target Task for this call */
	private Task targetTask;

	/**
	 * Constructs a new TaskCall with an unspecified target.
	 */
	public TaskCall() {
		super();

		setProducesDone(true);
		setDoneSynchronous(true);

		makeExit(0);
	}

	/**
	 * Constructs a new TaskCall with the specified {@link Task} target.
	 */
	public TaskCall(Task target) {
		this();
		setTarget(target);
	}

	/**
	 * Sets the target of this task call, but may only be called once.
	 */
	public void setTarget(Task target) {
		if (target == null)
			throw new IllegalArgumentException(
					"Cannot set Task Call to null target");

		if (targetTask != null)
			throw new IllegalStateException(
					"Cannot change the target of a Task Call");

		targetTask = target;
		build();
	}

	/**
	 * Returns the target of this task.
	 */
	public Task getTarget() {
		return targetTask;
	}

	/**
	 * Returns the latency of this call by deferring to the target task latency.
	 */
	@Override
	public Latency getLatency() {
		return getTarget().getCall().getLatency();
	}

	@Override
	public void accept(Visitor vis) {
		vis.visit(this);
	}

	@Override
	public boolean replaceComponent(Component removed, Component inserted) {
		throw new UnsupportedOperationException(
				"Cannot modify the components internal to a task call");
	}

	private void build() {
		assert getTarget() != null : "Cannot build taskCall until target task has been set";

		final Call theCall = getTarget().getCall();
		final SimplePin goPin;
		final SimplePin donePin;
		if (!theCall.getGoPort().isConnected()) {
			final Module owner = theCall.getOwner();
			goPin = new SimpleInternalPin(1, theCall.showIDLogical() + "_go");
			final SimplePinRead read = new SimplePinRead(goPin);
			theCall.getGoPort().setBus(read.getResultBus());
			owner.addComponent(goPin);
			owner.addComponent(read);
		} else {
			goPin = (SimplePin) ((SimplePinRead) theCall.getGoPort().getBus()
					.getOwner().getOwner()).getReferenceable();
		}

		if (!theCall.getExit(Exit.DONE).getDoneBus().isConnected()) {
			final Module owner = theCall.getOwner();
			donePin = new SimpleInternalPin(1, theCall.showIDLogical()
					+ "_done");
			final SimplePinWrite write = new SimplePinWrite(donePin);
			write.getDataPort().setBus(theCall.getExit(Exit.DONE).getDoneBus());
			// The go port is hooked up to the done bus of the data
			// port driving bus in the SimplePinConnector.
			// write.getGoPort().setBus(theCall.getExit(Exit.DONE).getDoneBus());
			owner.addComponent(donePin);
			owner.addComponent(write);
		} else {
			final Bus doneBus = theCall.getExit(Exit.DONE).getDoneBus();
			final Set owners = new HashSet();
			for (Iterator iter = doneBus.getPorts().iterator(); iter.hasNext();)
				owners.add(((Port) iter.next()).getOwner());
			assert owners.size() == 1 : "Task done must have only one target component "
					+ owners.size();

			donePin = (SimplePin) ((SimplePinWrite) owners.iterator().next())
					.getReferenceable();
		}

		// Create a write to the go pin and a read from the done pin.
		final SimplePinWrite goWrite = new SimplePinWrite(goPin);
		final SimplePinRead doneRead = new SimplePinRead(donePin);
		addComponent(goWrite);
		addComponent(doneRead);
		goWrite.getDataPort().setBus(getGoPort().getPeer());
		goWrite.getGoPort().setBus(getGoPort().getPeer());
		getExit(Exit.DONE).getDoneBus().getPeer()
				.setBus(doneRead.getResultBus());
	}

	@Override
	protected Exit createExit(int dataCount, Exit.Type type, String label) {
		return new VariableLatencyExit(this, dataCount, type, label);
	}

	/**
	 * Overrides {@link Exit#getLatency()} to return the latency as specified by
	 * the owner of the exit.
	 */
	private static class VariableLatencyExit extends Exit {
		VariableLatencyExit(Module owner, int dataCount, Exit.Type type,
				String label) {
			super(owner, dataCount, type, label);
		}

		@Override
		public Latency getLatency() {
			return getOwner().getLatency();
		}
	}

	/**
	 * Performs the clone by returning an empty (untargetted) task call which
	 * can be 'deep' cloned by calling setTarget with the target of the
	 * original.
	 */
	@Override
	public Object clone() {
		TaskCall clone = new TaskCall();
		copyComponentAttributes(clone);

		return clone;
	}

	@Override
	public String toString() {
		String ret = super.toString();
		ret += "<" + ID.showLogical(this) + ">";
		return ret;
	}

}
