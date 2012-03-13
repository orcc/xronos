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
import net.sf.openforge.lim.Latch;
import net.sf.openforge.lim.Latency;
import net.sf.openforge.lim.Module;
import net.sf.openforge.lim.Not;
import net.sf.openforge.lim.Or;
import net.sf.openforge.lim.Port;
import net.sf.openforge.lim.Referenceable;
import net.sf.openforge.lim.Reg;
import net.sf.openforge.lim.Value;
import net.sf.openforge.lim.Visitable;
import net.sf.openforge.lim.Visitor;
import net.sf.openforge.lim.op.CastOp;

/**
 * FifoWrite is an atomic access to a given {@link FifoIF} which sends a single
 * element of data to that interface. It is, however, a subclass of Module so
 * that this functionality can be further broken down into a sequence of pin
 * accesses. This functionality is hand-constructed.
 * <P>
 * FifoWrite components are inherently unsigned due to the fact that the
 * {@link SimplePinRead} and {@link SimplePinWrite} on which they are based are
 * inherently unsigned. If you need a FifoWrite to consume a signed value you
 * must cast the data port to the proper signedness.
 * 
 * <p>
 * Created: Tue Dec 16 11:44:39 2003
 * 
 * @author imiller, last modified by $Author: imiller $
 * @version $Id: FifoWrite.java 280 2006-08-11 17:00:32Z imiller $
 */
public class FifoWrite extends FifoAccess implements Visitable {

	/** A set containing only the flop if applicable */
	private Set feedbackPoints = Collections.EMPTY_SET;

	protected FifoWrite(FifoOutput targetInterface, Latency operationalLatency) {
		super(targetInterface);
		// Excluding 'sideband' ports/buses (those connecting to pins)
		// there is a single data port on this module, and a GO port
		// and DONE bus.
		Exit exit = makeExit(0);
		Bus done = exit.getDoneBus();
		done.setUsed(true);
		// Because we register the DONE signal (and no data is
		// produced by this node) the component is guaranteed to take
		// at least one clock cycle, but could take more if the write
		// is blocked by the full status flag.
		exit.setLatency(operationalLatency);

		Port data = makeDataPort();
		data.setUsed(true);
	}

	/**
	 * Constructs a new FifoWrite targetting the given FifoIF.
	 * 
	 * @param targetInterface
	 *            a non null 'FifoIF'
	 */
	public FifoWrite(FifoOutput targetInterface) {
		// super(targetInterface);
		// this(targetInterface, Latency.ONE.open(new Object()));
		// Because there is a combinational path from GO to DONE the
		// latency is zero. Because the output write may stall the
		// latency must be open.
		// this(targetInterface, Latency.ZERO.open(new Object()));
		this(targetInterface, null);
		Exit exit = getExit(Exit.DONE);
		exit.setLatency(Latency.ZERO.open(exit));

		this.setProducesDone(true);
		this.setDoneSynchronous(true);

		// // Excluding 'sideband' ports/buses (those connecting to pins)
		// // there is a single data port on this module, and a GO port
		// // and DONE bus.
		// Exit exit = makeExit(0);
		// Bus done = exit.getDoneBus();
		// done.setUsed(true);
		// // Because we register the DONE signal (and no data is
		// // produced by this node) the component is guaranteed to take
		// // at least one clock cycle, but could take more if the write
		// // is blocked by the full status flag.
		// exit.setLatency(Latency.ONE.open(this));

		// Port data = makeDataPort();
		// data.setUsed(true);
		Port data = (Port) getDataPorts().get(0);
		Bus done = exit.getDoneBus();

		/*
		 * Build the following code: dataLatch = writeData(port) : en GO
		 * fifo_data = writeData (port); pending = (GO || GO'); fifo_WR =
		 * pending & !fifo_FF write_done (bus) <= fifo_WR; // NOTE THE REGISTER!
		 * GO' <= pending & fifo_ff
		 */
		// 2 FD flops (reset to 0)
		// 1 OR
		// 2 AND
		// 1 NOT
		// 1 SimplePinWrite (fifo data)
		// 1 SimplePinWrite (fifo write)
		// 1 SimplePinRread (FF)
		// 1 latch (data capture in case we are blocked)
		// Needs RESET b/c it is in the control path
		final Reg flop = Reg.getConfigurableReg(Reg.REGR, "fifoWriteFlop");
		final Or pending = new Or(2);
		final And done_and = new And(2);
		final And flop_and = new And(2);
		final Not not = new Not();
		final CastOp doutCast = new CastOp(targetInterface.getDataPin()
				.getWidth(), false);
		final SimplePinWrite dout = new SimplePinWrite(
				targetInterface.getDataPin());
		final SimplePinWrite write = new SimplePinWrite(
				targetInterface.getSendPin());
		final SimplePinRead full = new SimplePinRead(
				targetInterface.getAckPin());
		final Latch capture = new Latch();

		// Give the flops an initial size.
		flop.getResultBus().pushValueForward(new Value(1, false));

		// Connect the clock ports
		flop.getClockPort().setBus(this.getClockPort().getPeer());
		flop.getResetPort().setBus(this.getResetPort().getPeer());
		flop.getInternalResetPort().setBus(this.getResetPort().getPeer());
		capture.getClockPort().setBus(this.getClockPort().getPeer());

		// add all the components
		this.addComponent(flop);
		this.addComponent(pending);
		this.addComponent(done_and);
		this.addComponent(flop_and);
		this.addComponent(not);
		this.addComponent(doutCast);
		this.addComponent(dout);
		this.addComponent(write);
		this.addComponent(full);
		this.addComponent(capture);

		// Hook up data capture latch
		capture.getDataPort().setBus(data.getPeer());
		capture.getEnablePort().setBus(getGoPort().getPeer());

		// Hook fifo data through
		doutCast.getDataPort().setBus(capture.getResultBus());
		dout.getDataPort().setBus(doutCast.getResultBus());
		dout.getGoPort().setBus(done_and.getResultBus());

		// Calculate pending
		((Port) pending.getDataPorts().get(0)).setBus(flop.getResultBus());
		((Port) pending.getDataPorts().get(1)).setBus(getGoPort().getPeer());

		// calculate the done/fifoWR
		not.getDataPort().setBus(full.getResultBus());
		((Port) done_and.getDataPorts().get(0)).setBus(pending.getResultBus());
		((Port) done_and.getDataPorts().get(1)).setBus(not.getResultBus());

		// Connect the fifoWR
		write.getDataPort().setBus(done_and.getResultBus());
		write.getGoPort().setBus(done_and.getResultBus());

		// Connect the done
		done.getPeer().setBus(done_and.getResultBus());

		// calculate the flop expression
		((Port) flop_and.getDataPorts().get(0)).setBus(pending.getResultBus());
		((Port) flop_and.getDataPorts().get(1)).setBus(full.getResultBus());

		// Connect the flop input
		flop.getDataPort().setBus(flop_and.getResultBus());

		// Define the feedback point
		this.feedbackPoints = Collections.singleton(flop);
	}

	/**
	 * Accept the specified visitor
	 * 
	 * @param visitor
	 *            a Visitor
	 */
	public void accept(Visitor visitor) {
		visitor.visit(this);
	}

	/**
	 * Needs a clock for the embedded flop
	 * 
	 * @return true
	 */
	public boolean consumesClock() {
		return true;
	}

	/**
	 * Needs a GO to generate the write signal, overrides Module because this
	 * module is not traversed during scheduling. That means that we will not
	 * automagically detect that it needs a go and thus won't calculate one for
	 * it unless we return true from this method.
	 * 
	 * @return true
	 */
	public boolean consumesGo() {
		return true;
	}

	public boolean replaceComponent(Component removed, Component inserted) {
		// TBD
		assert false;
		return false;
	}

	public Set getFeedbackPoints() {
		Set feedback = new HashSet();
		feedback.addAll(super.getFeedbackPoints());
		feedback.addAll(this.feedbackPoints);

		return feedback;
	}

	/**
	 * This accessor modifies the {@link Referenceable} target state so it may
	 * not execute in parallel with other accesses.
	 */
	public boolean isSequencingPoint() {
		return true;
	}

	protected void cloneNotify(Module clone, Map cloneMap) {
		super.cloneNotify(clone, cloneMap);
		// Re-set the feedback points to point to the correct register
		// in the clone instead of the register in the original IFF it
		// exists... subclasses may have alternate structure
		if (this.feedbackPoints.isEmpty()) {
			((FifoWrite) clone).feedbackPoints = Collections.EMPTY_SET;
		} else {
			Set cloneSet = new HashSet();
			((FifoWrite) clone).feedbackPoints = cloneSet;
			for (Iterator iter = this.feedbackPoints.iterator(); iter.hasNext();)
				cloneSet.add(cloneMap.get(iter.next()));
			assert !cloneSet.contains(null);
		}
		// ((FifoWrite)clone).feedbackPoints =
		// Collections.singleton(cloneMap.get(this.feedbackPoints.iterator().next()));
	}

}// FifoWrite
