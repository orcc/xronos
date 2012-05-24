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

import net.sf.openforge.util.naming.ID;

/**
 * The Kicker uses the Reset port as input, and the done bus as output. the
 * equation is: reg1=(!RESET) reg2=(reg1)&(!RESET)
 * done<=(reg1)&&(!reg2)&&(!RESET)
 * 
 * @version $Id: Kicker.java 562 2008-03-17 21:33:12Z imiller $
 */
public class Kicker extends Module implements Composable {

	private static final int GATE_DEPTH = 0;

	/** Keep all instances of Kickers uniquely named */
	private static int index = 0;

	/**
	 * Build the basic Kicker
	 * 
	 * @param size
	 *            the initial size in bits of the Latch.
	 */
	public Kicker() {
		this.setIDLogical("Kicker_" + index++);

		// make an exit
		Exit exit = makeExit(0);
		// Assuming it has a latency of 2 -- I believe this is irrelevent
		exit.setLatency(Latency.get(2));

		// create the objects
		// No need to hook to reset... their data is based on reset
		Reg reg1 = new Reg(Reg.REG, "kicker_1");
		Reg reg2 = new Reg(Reg.REG, "kicker_2");
		Not not1 = new Not();
		Not not2 = new Not();
		And and1 = new And(2);
		And and2 = new And(3);
		Reg regFinal = new Reg(Reg.REG, "kicker_res");

		// add them to the module
		addComponent(reg1);
		addComponent(reg2);
		addComponent(not1);
		addComponent(not2);
		addComponent(and1);
		addComponent(and2);
		addComponent(regFinal);

		// name them
		reg1.setIDLogical(ID.showLogical(this) + "_reg1");
		reg2.setIDLogical(ID.showLogical(this) + "_reg2");
		not1.setIDLogical(ID.showLogical(this) + "_not1");
		not2.setIDLogical(ID.showLogical(this) + "_not2");
		and1.setIDLogical(ID.showLogical(this) + "_and1");
		and2.setIDLogical(ID.showLogical(this) + "_and2");
		regFinal.setIDLogical(ID.showLogical(this) + "_res");

		reg1.getResultBus().setSize(1, false);
		reg2.getResultBus().setSize(1, false);
		not1.getResultBus().setSize(1, false);
		not2.getResultBus().setSize(1, false);
		and1.getResultBus().setSize(1, false);
		and2.getResultBus().setSize(1, false);
		regFinal.getResultBus().setSize(1, false);

		// clock and reset are used ...
		getClockPort().setUsed(true);
		getResetPort().setUsed(true);

		// done bus is used
		getDoneBus().setUsed(true);
		// Needed for translation in its own module.
		getDoneBus().getPeer().setUsed(true);

		// go port is ignored...
		getGoPort().setUsed(false);

		// useful
		// Bus resetBus =
		getInBuf().getResetBus();

		// now, construct the guts of this.

		// the not1 connected to the ... reset port
		not1.getDataPort().setBus(getResetPort().getPeer());

		// the reg1 connected to the .. not1
		reg1.getDataPort().setBus(not1.getResultBus());
		reg1.getClockPort().setBus(getClockPort().getPeer()); // connect clock

		// the and1 connected to the ... not1 & reg1
		and1.getDataPorts().get(0).setBus(not1.getResultBus());
		and1.getDataPorts().get(1).setBus(reg1.getResultBus());

		// the reg2 connected to the and1
		reg2.getDataPort().setBus(and1.getResultBus());
		reg2.getClockPort().setBus(getClockPort().getPeer()); // connect clock

		// the not2 connected to the reg2
		not2.getDataPort().setBus(reg2.getResultBus());

		// the and21 connected to the reg1, not1, not2
		and2.getDataPorts().get(0).setBus(reg1.getResultBus());
		and2.getDataPorts().get(1).setBus(not1.getResultBus());
		and2.getDataPorts().get(2).setBus(not2.getResultBus());

		regFinal.getDataPort().setBus(and2.getResultBus());
		regFinal.getClockPort().setBus(getClockPort().getPeer()); // connect
																	// clock

		// the done bus connected to the regFinal
		getDoneBus().getPeer().setBus(regFinal.getResultBus());
	}

	/**
	 * Throws an exception, replacement in this class not supported.
	 */
	@Override
	public boolean replaceComponent(Component removed, Component inserted) {
		throw new UnsupportedOperationException("Cannot replace components in "
				+ getClass());
	}

	/**
	 * <code>getRepeatPort</code> will return a non-null port if this kicker has
	 * an input port for causing the kicker to repeat, or null if no such port
	 * exists.
	 * 
	 * @return a <code>Port</code> value, can be null
	 */
	public Port getRepeatPort() {
		return null;
	}

	public Bus getDoneBus() {
		return getExit(Exit.DONE).getDoneBus();
	}

	@Override
	public void accept(Visitor v) {
		v.visit(this);
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
		return GATE_DEPTH;
	}

	/**
	 * Tests whether this component requires a connection to its clock
	 * {@link Port}.
	 */
	@Override
	public boolean consumesClock() {
		return true;
	}

	/**
	 * Tests whether this component requires a connection to its reset
	 * {@link Port}. By default, returns the value of
	 * {@link Component#consumesClock()}.
	 */
	@Override
	public boolean consumesReset() {
		return true;
	}

	/**
	 * Indicates that this module will be instantiated as its own module in HDL,
	 * ie signal names cannot propagate across its boundry.
	 * 
	 * @return a value of type 'boolean'
	 */
	@Override
	public boolean isOpaque() {
		return true;
	}

	/**
	 * @return false
	 */
	@Override
	public boolean isBalanceable() {
		return false;
	}

	/**
	 * A KickerPerpetual is a Module which takes in Clock and generates a single
	 * cycle pulse on the done output shortly after device power-up
	 * (de-assertion of GSR). Additionally, there is a 'repeat' port which is
	 * delayed by one cycle and then logically ORed with this post-powerup
	 * signal. When the repeat port is connected to the DONE of an entry method
	 * and the output of the kicker to the GO, this will cause the circuit
	 * (entry method) to run forever as it will be continuously restarted
	 * immediately after completing.
	 */
	public static class KickerPerpetual extends Kicker {
		private Port repeatPort;

		public KickerPerpetual() {
			this(true);
		}

		public KickerPerpetual(boolean fbFlopRequired) {
			super();

			repeatPort = this.makeDataPort();
			repeatPort.setUsed(true);
			repeatPort.setSize(1, false);
			final Bus repeatBus = repeatPort.getPeer();
			repeatBus.setSize(1, false);

			// Now grab the done bus and put the logical OR in front
			// of it.
			Bus doneDriver = getDoneBus().getPeer().getBus();
			assert doneDriver != null;

			Or or = new Or(2);
			addComponent(or);
			or.setIDLogical(ID.showLogical(this) + "_or1");

			final Bus rptBus;
			if (fbFlopRequired) {
				//
				// The delay register is needed because of a corner
				// condition case in which the function being wrapped in
				// the block IO code generates a constant return value
				// (and no params are modified). In this case, the 'read'
				// loops and the 'write' loops can execute simultaneously
				// and when both are complete there will exist through the
				// design a combinational path from GO to DONE that is
				// being taken (from GO through the 'if' statements of the
				// loops, through the AND's in the scoreboards, to the
				// DONE of the block IO wrapper). If the kicker does not
				// insert a delay, then this loop hangs simulations.
				// Basically it gives the loops a 'finishing' cycle to
				// reset for the next execution
				//
				// Needs RESET b/c it is in the control path
				Reg delay = Reg.getConfigurableReg(Reg.REGR,
						ID.showLogical(this) + "_fb_delay");
				addComponent(delay);

				delay.getDataPort().setBus(repeatBus);
				delay.getClockPort().setBus(getClockPort().getPeer()); // connect
																		// clock
				delay.getResetPort().setBus(getResetPort().getPeer()); // connect
																		// reset
				delay.getInternalResetPort().setBus(getResetPort().getPeer()); // connect
																				// reset
				rptBus = delay.getResultBus();
			} else {
				rptBus = repeatBus;
			}

			// ((Port)or.getDataPorts().get(1)).setBus(delay.getResultBus());
			or.getDataPorts().get(1).setBus(rptBus);
			or.getDataPorts().get(0).setBus(doneDriver);

			getDoneBus().getPeer().setBus(or.getResultBus());
			getDoneBus().setUsed(true);
		}

		/**
		 * Returns the port which when set high will cause the output (done bus)
		 * of this KickerPerpetual to go high, ie it causes the kicker to fire
		 * again.
		 * 
		 * @return a non null Port
		 */
		@Override
		public Port getRepeatPort() {
			return repeatPort;
		}

	}

	/**
	 * A KickerContinuous is a Module which takes in Clock and generates a
	 * single cycle pulse on the done output shortly after device power-up
	 * (de-assertion of GSR) and once asserted, maintains that done signal in
	 * the true state until the device Reset signal is asserted.
	 */
	public static class KickerContinuous extends Kicker {
		private Port repeatPort;

		public KickerContinuous() {
			super();

			// Grab the done and put the logical OR in front of it.
			Bus doneDriver = getDoneBus().getPeer().getBus();
			assert doneDriver != null;

			Or or = new Or(2);
			addComponent(or);
			or.setIDLogical(ID.showLogical(this) + "_or1");

			//
			// The delay register is needed to ensure that we do not
			// have a combinational feedback path here.
			//
			// Needs RESET b/c it is in the control path
			Reg delay = Reg.getConfigurableReg(Reg.REGR, ID.showLogical(this)
					+ "_fb_delay");
			delay.getClockPort().setBus(getClockPort().getPeer()); // connect
																	// clock
			delay.getResetPort().setBus(getResetPort().getPeer()); // connect
																	// reset
			delay.getInternalResetPort().setBus(getResetPort().getPeer()); // connect
																			// reset
			delay.getResultBus().setSize(1, false);
			addComponent(delay);
			addFeedbackPoint(delay);

			delay.getDataPort().setBus(or.getResultBus());
			or.getDataPorts().get(0).setBus(doneDriver);
			or.getDataPorts().get(1).setBus(delay.getResultBus());

			getDoneBus().getPeer().setBus(or.getResultBus());
			getDoneBus().setUsed(true);
		}

		/**
		 * Returns the port which when set high will cause the output (done bus)
		 * of this KickerContinuous to go high, ie it causes the kicker to fire
		 * again.
		 * 
		 * @return a non null Port
		 */
		@Override
		public Port getRepeatPort() {
			return repeatPort;
		}

	}

}
