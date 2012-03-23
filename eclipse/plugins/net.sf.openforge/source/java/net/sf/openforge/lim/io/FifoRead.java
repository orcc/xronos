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

package net.sf.openforge.lim.io;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import net.sf.openforge.lim.And;
import net.sf.openforge.lim.Bus;
import net.sf.openforge.lim.Component;
import net.sf.openforge.lim.Exit;
import net.sf.openforge.lim.Latency;
import net.sf.openforge.lim.Module;
import net.sf.openforge.lim.Not;
import net.sf.openforge.lim.Or;
import net.sf.openforge.lim.Referenceable;
import net.sf.openforge.lim.Reg;
import net.sf.openforge.lim.Value;
import net.sf.openforge.lim.Visitable;
import net.sf.openforge.lim.Visitor;

/**
 * FifoRead is an atomic access to a given {@link FifoIF} which returns a single
 * element of data from that interface. It is, however, a subclass of Module so
 * that this functionality can be further broken down into a sequence of pin
 * accesses. This functionality is hand-constructed.
 * <P>
 * FifoRead components are inherently unsigned due to the fact that the
 * {@link SimplePinRead} and {@link SimplePinWrite} on which they are based are
 * inherently unsigned. If you need a signed value from a FIFO read you must
 * cast the result bus to the proper signeness.
 * 
 * <p>
 * Created: Tue Dec 16 11:44:39 2003
 * 
 * @author imiller, last modified by $Author: imiller $
 * @version $Id: FifoRead.java 280 2006-08-11 17:00:32Z imiller $
 */
public class FifoRead extends FifoAccess implements Visitable {

	/** A set containing only the flop */
	private Set<Reg> feedbackPoints = new HashSet<Reg>();

	protected FifoRead(FifoInput targetInterface, Latency operationalLatency) {
		super(targetInterface);

		// Excluding 'sideband' ports/buses (those connecting to pins)
		// there is a single result bus on this module, and a GO port
		// and DONE bus.
		Exit exit = makeExit(1);
		Bus result = exit.getDataBuses().get(0);
		Bus done = exit.getDoneBus();
		done.setUsed(true);
		result.setUsed(true);

		exit.setLatency(operationalLatency);
	}

	/**
	 * Constructs a new FifoRead targeting the given FifoIF.
	 * 
	 * @param targetInterface
	 *            a non null 'FifoIF'
	 */
	public FifoRead(FifoInput targetInterface) {
		// Could be combinational or longer if the access is blocked
		// by status flags.
		// this(targetInterface, Latency.ZERO.open(new Object()));
		this(targetInterface, null);
		Exit exit = getExit(Exit.DONE);
		exit.setLatency(Latency.ZERO.open(exit));

		this.setProducesDone(true);
		this.setDoneSynchronous(true);

		/*
		 * Build up the correct logic in this module to implement the
		 * functionality: read_data = fifo_DIN; pending = (GO || GO'); read_done
		 * = fifo_ACK = pending && fifo_DE; GO' <= pending && !fifo_DE;
		 */
		// // Excluding 'sideband' ports/buses (those connecting to pins)
		// // there is a single result bus on this module, and a GO port
		// // and DONE bus.
		// Exit exit = makeExit(1);
		// Bus result = (Bus)exit.getDataBuses().get(0);
		// Bus done = exit.getDoneBus();
		// done.setUsed(true);
		// result.setUsed(true);

		// // Could be combinational or longer if the access is blocked
		// // by status flags.
		// exit.setLatency(Latency.ZERO.open(this));
		Bus done = exit.getDoneBus();
		Bus result = exit.getDataBuses().get(0);

		// To implement the functionality we have:
		// 1 FD flop (resets to 0)
		// 1 logical OR (2 input)
		// 2 logical ANDs (2 input)
		// 1 logical NOT
		// 1 fifo data read
		// 1 fifo DE read
		// 1 fifo ACK write
		// Needs RESET b/c it is in the control path
		final Reg flop = Reg.getConfigurableReg(Reg.REGR, "fifoReadFlop");
		flop.getClockPort().setBus(this.getClockPort().getPeer());
		flop.getResetPort().setBus(this.getResetPort().getPeer());
		flop.getInternalResetPort().setBus(this.getResetPort().getPeer());
		// Because the flop is a feedback point it needs to be
		// pre-initialized with its value
		flop.getResultBus().pushValueForward(new Value(1, false));
		final Or pending = new Or(2);
		final And done_and = new And(2);
		final And flop_and = new And(2);
		final Not not = new Not();
		final SimplePinRead din = new SimplePinRead(
				targetInterface.getDataPin());
		final SimplePinRead exists = new SimplePinRead(
				targetInterface.getSendPin());
		final SimplePinWrite ack = new SimplePinWrite(
				targetInterface.getAckPin());
		this.addComponent(flop);
		this.addComponent(pending);
		this.addComponent(done_and);
		this.addComponent(flop_and);
		this.addComponent(not);
		this.addComponent(din);
		this.addComponent(exists);
		this.addComponent(ack);

		// Hook fifo DIN pin to the result of the module. Easy
		// straight wire through.
		result.getPeer().setBus(din.getResultBus());

		// Calculate 'pending'
		pending.getDataPorts().get(0).setBus(flop.getResultBus());
		pending.getDataPorts().get(1).setBus(getGoPort().getPeer());

		// calculate the 'read done'
		done_and.getDataPorts().get(0).setBus(pending.getResultBus());
		done_and.getDataPorts().get(1).setBus(exists.getResultBus());

		// hook 'read done' to done bus and fifo ack
		done.getPeer().setBus(done_and.getResultBus());
		ack.getDataPort().setBus(done_and.getResultBus());
		ack.getGoPort().setBus(done_and.getResultBus());

		// calculate the and for the flop
		not.getDataPort().setBus(exists.getResultBus());
		flop_and.getDataPorts().get(0).setBus(not.getResultBus());
		flop_and.getDataPorts().get(1).setBus(pending.getResultBus());

		// Connect the flop port
		flop.getDataPort().setBus(flop_and.getResultBus());

		// Define the feedback point
		this.feedbackPoints = Collections.singleton(flop);
	}

	public FifoRead(NativeInput targetInterface) {
		super(targetInterface);
		Exit exit = makeExit(1);
		exit.setLatency(Latency.ZERO.open(exit));

		this.setProducesDone(true);
		this.setDoneSynchronous(true);

		// Build up the correct logic in this module to implement the
		// functionality:
		// read_data = fifo_DIN;
		// pending = (GO || GO');
		// done = pending;
		// GO' <= pending;

		// // Excluding 'sideband' ports/buses (those connecting to pins)
		// // there is a single result bus on this module, and a GO port
		// // and DONE bus.
		// Exit exit = makeExit(1);
		// Bus result = (Bus)exit.getDataBuses().get(0);
		// Bus done = exit.getDoneBus();
		// done.setUsed(true);
		// result.setUsed(true);

		// // Could be combinational or longer if the access is blocked
		// // by status flags.
		// exit.setLatency(Latency.ZERO.open(this));
		Bus done = exit.getDoneBus();
		Bus result = exit.getDataBuses().get(0);

		// To implement the functionality we have:
		// 1 FD flop (resets to 0)
		// 1 fifo data read

		// Needs RESET b/c it is in the control path
		final Reg flop = Reg.getConfigurableReg(Reg.REGR, "fifoReadFlop");
		flop.getClockPort().setBus(this.getClockPort().getPeer());
		flop.getResetPort().setBus(this.getResetPort().getPeer());
		flop.getInternalResetPort().setBus(this.getResetPort().getPeer());
		// Because the flop is a feedback point it needs to be
		// pre-initialized with its value
		flop.getResultBus().pushValueForward(new Value(1, false));
		final Or pending = new Or(2);

		final SimplePinRead din = new SimplePinRead(
				targetInterface.getDataPin());
		this.addComponent(flop);
		this.addComponent(pending);
		this.addComponent(din);

		// Hook fifo DIN pin to the result of the module. Easy
		// straight wire through.
		result.getPeer().setBus(din.getResultBus());

		// Calculate 'pending'
		pending.getDataPorts().get(0).setBus(flop.getResultBus());
		pending.getDataPorts().get(1).setBus(getGoPort().getPeer());

		// hook 'pending' to done bus
		done.getPeer().setBus(pending.getResultBus());

		// Connect the flop port
		flop.getDataPort().setBus(pending.getResultBus());

		// Define the feedback point
		this.feedbackPoints = Collections.singleton(flop);
	}

	/**
	 * Accept the specified visitor
	 * 
	 * @param visitor
	 *            a Visitor
	 */
	@Override
	public void accept(Visitor visitor) {
		visitor.visit(this);
	}

	/**
	 * Needs a clock for the embedded flop
	 * 
	 * @return true
	 */
	@Override
	public boolean consumesClock() {
		return true;
	}

	/**
	 * Needs a GO to generate the done signal, overrides Module because this
	 * module is not traversed during scheduling. That means that we will not
	 * automagically detect that it needs a go and thus won't calculate one for
	 * it unless we return true from this method.
	 * 
	 * @return true
	 */
	@Override
	public boolean consumesGo() {
		return true;
	}

	@Override
	public boolean replaceComponent(Component removed, Component inserted) {
		// TBD
		assert false;
		return false;
	}

	@Override
	public Set<Component> getFeedbackPoints() {
		Set<Component> feedback = new HashSet<Component>();
		feedback.addAll(super.getFeedbackPoints());
		feedback.addAll(this.feedbackPoints);

		return feedback;
	}

	/**
	 * This accessor modifies the {@link Referenceable} target state so it may
	 * not execute in parallel with other accesses.
	 */
	@Override
	public boolean isSequencingPoint() {
		return true;
	}

	@Override
	protected void cloneNotify(Module clone, Map cloneMap) {
		super.cloneNotify(clone, cloneMap);
		// Re-set the feedback points to point to the correct register
		// in the clone instead of the register in the original IFF it
		// exists... subclasses may have alternate structure
		if (this.feedbackPoints.isEmpty()) {
			((FifoRead) clone).feedbackPoints = Collections.emptySet();
		} else {
			Set cloneSet = new HashSet();
			((FifoRead) clone).feedbackPoints = cloneSet;
			for (Iterator iter = this.feedbackPoints.iterator(); iter.hasNext();)
				cloneSet.add(cloneMap.get(iter.next()));
			assert !cloneSet.contains(null);
		}

		// ((FifoRead)clone).feedbackPoints =
		// Collections.singleton(cloneMap.get(this.feedbackPoints.iterator().next()));
	}

}// FifoRead
