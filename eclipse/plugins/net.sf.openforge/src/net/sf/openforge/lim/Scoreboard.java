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
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import net.sf.openforge.lim.primitive.And;
import net.sf.openforge.lim.primitive.Not;
import net.sf.openforge.lim.primitive.Or;
import net.sf.openforge.lim.primitive.Reg;
import net.sf.openforge.util.naming.ID;

/**
 * Scoreboard represents a logical AND in which the inputs may arrive during
 * different clock cycles. In order to track which inputs have recieved a
 * logical true, the inputs must be latched when activated until all inputs have
 * been activated one time, at which point the single output bus is set true for
 * one cycle and the input latches are all cleared.
 * 
 * Created: Thu May 9 11:51:59 2002
 * 
 * @author imiller
 * @version $Id: Scoreboard.java 280 2006-08-11 17:00:32Z imiller $
 */
public class Scoreboard extends Module implements Composable {

	private Set<Reg> feedbackPoints = new HashSet<Reg>(7);

	/* The logical AND that generates the scoreboard result */
	private And scbdAnd;
	/*
	 * The result of this scoreboard... may be logically ORed with the reset
	 * signal if any.
	 */
	private Bus resultBus;

	/**
	 * Create a Scoreboard for the given collection of buses.
	 * 
	 * @param buses
	 *            a value of type 'Collection'
	 */
	public Scoreboard(Collection<Bus> buses) {
		this(buses.size());

		final Iterator<Port> portIter = getDataPorts().iterator();
		final Iterator<Bus> busIter = buses.iterator();
		while (busIter.hasNext()) {
			final Port port = portIter.next();
			port.setBus(busIter.next());
			port.setUsed(true);
		}
	}

	public Scoreboard(int count) {
		final Exit exit = makeExit(0);

		exit.getDoneBus().setUsed(true);
		getGoPort().setUsed(false);

		exit.setLatency(Latency.ZERO);

		// clock and reset are used ...
		getClockPort().setUsed(true);
		getResetPort().setUsed(true);

		for (int i = 0; i < count; i++) {
			makeDataPort();
		}

		// make it...
		makePhysical();

		setProducesDone(true);
		setConsumesClock(true);
		setConsumesReset(true);
	}

	/**
	 * Gets the gate depth of this component. This is the maximum number of
	 * gates that any input signal must traverse before reaching an {@link Exit}
	 * .
	 * 
	 * @return a non-negative integer
	 */
	@Override
	public int getGateDepth() {
		return log2(getDataPorts().size()) + 1;
	}

	/**
	 * Returns a Set of {@link Component Components} that represent the feedback
	 * points in this Scoreboard. This set is populated from the Reg's created
	 * for this scoreboard since their Q is directly connected to D and the
	 * 'done' from the scoreboard feeds back around to the Reg.
	 * 
	 * @return a 'Set' of {@link Reg Reg's}
	 */
	@Override
	public Set<Component> getFeedbackPoints() {
		Set<Component> feedback = new HashSet<Component>();
		feedback.addAll(super.getFeedbackPoints());
		feedback.addAll(feedbackPoints);

		return Collections.unmodifiableSet(feedback);
	}

	/**
	 * Throws an exception, replacement in this class not supported.
	 */
	@Override
	public boolean replaceComponent(Component removed, Component inserted) {
		throw new UnsupportedOperationException("Cannot replace components in "
				+ getClass());
	}

	protected Bus getScbdResult() {
		return resultBus;
	}

	protected And getScbdAnd() {
		return scbdAnd;
	}

	protected boolean isStallable() {
		return false;
	}

	private void makePhysical() {
		// create the AND, or the ADN & OR pair ...
		// connect to the donebus...

		int size = getDataPorts().size();
		And and = new And(size);
		and.getResultBus().setIDLogical(ID.showLogical(this) + "_and");
		scbdAnd = and;
		Bus commonResultBus;
		// add the component
		addComponent(and);
		// If this is a stallboard then we need to NOT reset the stall
		// inputs to 0, but to 1. So we need for each slice to have
		// control over its own reset behavior.
		if (isStallable()) {
			// if we are a stall board then send the scoreboard done
			// signal to each slice where it will be combined with the
			// reset signal as needed.
			commonResultBus = and.getResultBus();
			getResultBus().getPeer().setBus(and.getResultBus());
		} else {
			// in this case we also need an or with the reset signal
			Or or = new Or(2);
			addComponent(or);
			List<Port> l = or.getDataPorts();

			// or together the result of the and
			Port port = l.get(0);
			port.setBus(and.getResultBus());

			// and the global reset
			port = l.get(1);
			port.setBus(getResetPort().getPeer());

			// connect the output of the OR to the donebus...
			commonResultBus = or.getResultBus();
			getResultBus().getPeer().setBus(and.getResultBus());
		}

		resultBus = commonResultBus;

		// iterator for the and
		Iterator<Port> it = and.getDataPorts().iterator();
		// iterator for the Scoreboard
		Iterator<Port> it2 = getDataPorts().iterator();
		int i = 0;
		while (it.hasNext()) {
			createSlice(i++, it.next(), it2.next(), false);
		}
	}

	protected void createSlice(int number, Port andPort, Port inputPort,
			boolean isStallPort) {
		// ok, what do we need....
		// first we need a SetReset Reg ...
		// CANNOT be configurable. The scoreboard MUST be synchronous
		// to avoid clearing before the 'done' is consumed.
		Reg r = new Reg(Reg.REGRS, ID.showLogical(this) + "_reg" + number);

		// connect clock
		r.getClockPort().setBus(getClockPort().getPeer());

		// connect output to input
		Port p = r.getDataPort();
		p.setUsed(true);
		p.setBus(r.getResultBus());

		final Bus setBus;
		final Bus resetBus;
		if (isStallable()) {
			final Or resetOr = new Or(2);
			addComponent(resetOr);
			resetOr.getResultBus().setIDLogical(
					ID.showLogical(this) + "_sliceOr" + number);
			final List<Port> l = resetOr.getDataPorts();
			if (isStallPort) {
				// set bus is the or of the input and reset
				l.get(0).setBus(inputPort.getPeer());
				l.get(1).setBus(getResetPort().getPeer());
				setBus = resetOr.getResultBus();
				// reset bus is the scoreboard result && !resetOr
				final And maskAnd = new And(2);
				final Not maskNot = new Not();
				addComponent(maskAnd);
				addComponent(maskNot);
				maskAnd.getResultBus().setIDLogical(
						ID.showLogical(this) + "_sliceMaskAnd" + number);
				maskNot.getResultBus().setIDLogical(
						ID.showLogical(this) + "_sliceMaskNot" + number);
				maskNot.getDataPort().setBus(setBus);
				maskAnd.getDataPorts().get(0).setBus(getScbdResult());
				maskAnd.getDataPorts().get(1).setBus(maskNot.getResultBus());
				// resetBus = getScbdResult();
				resetBus = maskAnd.getResultBus();
			} else {
				// the set bus is the input
				setBus = inputPort.getPeer();
				// reset bus is the or of the common result and the
				// reset port
				l.get(0).setBus(getScbdResult());
				l.get(1).setBus(getResetPort().getPeer());
				resetBus = resetOr.getResultBus();
			}
		} else {
			// set bus is the input
			setBus = inputPort.getPeer();
			// reset bus is the scoreboard result since it already has
			// the reset ored with it.
			resetBus = getScbdResult();
		}

		r.getResetPort().setBus(resetBus); // I dont think that this does
											// anything...
		r.getInternalResetPort().setBus(resetBus);
		r.getSetPort().setBus(setBus);

		// set value and record reg
		r.getResultBus().setSize(1, true);
		addComponent(r);
		feedbackPoints.add(r);

		// If this is a stall port, break any possible combinational
		// path through the process by only having the flop path
		// through the stall board. The combinational path comes
		// about because there is a false combinational path through
		// the decision circuit of loops. UGH!
		Bus sliceDoneBus;
		if (!isStallPort) {
			// create a 2 input OR for the output of this slice
			final Or or = new Or(2);
			or.getResultBus().setIDLogical(
					ID.showLogical(this) + "_resOr" + number);
			addComponent(or);
			List<Port> l = or.getDataPorts();
			// or together the input and the reg result
			l.get(0).setBus(inputPort.getPeer());
			l.get(1).setBus(r.getResultBus());
			sliceDoneBus = or.getResultBus();
		} else {
			sliceDoneBus = r.getResultBus();
		}

		// connect output to the and
		andPort.setBus(sliceDoneBus);
	}

	@Override
	public void accept(Visitor vis) {
		vis.visit(this);
	}

	/**
	 * Gets the {@link Bus} which is asserted to indicate that all inputs have
	 * been asserted.
	 */
	public Bus getDoneBus() {
		return getExit(Exit.DONE).getDoneBus();
	}

	public Bus getResultBus() {
		return getDoneBus();
	}

}// Scoreboard
