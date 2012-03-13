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

import net.sf.openforge.lim.And;
import net.sf.openforge.lim.Exit;
import net.sf.openforge.lim.Latency;
import net.sf.openforge.lim.Not;
import net.sf.openforge.lim.Or;
import net.sf.openforge.lim.Port;
import net.sf.openforge.lim.Referenceable;
import net.sf.openforge.lim.Reg;

/**
 * SimplePinStall is a LIM module that provides the ability to stall the LIM
 * wavefront (GO/DONE wavefront) until a pin is non-zero.
 * 
 * <p>
 * Created: Thu Jan 15 11:19:38 2004
 * 
 * @author imiller, last modified by $Author: imiller $
 * @version $Id: SimplePinStall.java 280 2006-08-11 17:00:32Z imiller $
 */
public class SimplePinStall extends SimplePinAccess {
	private static final boolean WIRE_THROUGH = true;

	public SimplePinStall(SimplePin pin) {
		super(pin);

		// The alternative is to OR all the bits of the pin read

		assert pin.getWidth() == 1 : "Pin stalling only supported for 1 bit pins";

		Exit exit = makeExit(0);
		// Can be combinational or take 1+ cycles.
		exit.setLatency(Latency.ZERO.open(exit));

		setConsumesGo(true);
		setProducesDone(true);
		setDoneSynchronous(true);

		if (WIRE_THROUGH) {
			System.out.println("PIN STALL IS SIMPLY A WIRE-THROUGH");

			SimplePinRead read = new SimplePinRead(pin);
			addComponent(read);
			read.getGoPort().setBus(getGoPort().getPeer());

			exit.getDoneBus().getPeer().setBus(read.getResultBus());
		} else {
			// consume a GO and produce a DONE only when the specified pin
			// is non-zero
			// DONE = (GO | GO') & pin
			// GO' = (GO' | GO) & !pin // IDM modified. This does not handle
			// back to back GO assertions where the DONE is not combinational
			// GO' = GO | (GO' & !pin)
			Or goOr = new Or(2);
			Or goPrimeOr = new Or(2);
			And goPrimeAnd = new And(2);
			And doneAnd = new And(2);
			Not not = new Not();
			// And captureAnd = new And(2);
			// Needs RESET b/c it is in the control path
			Reg goReg = Reg.getConfigurableReg(Reg.REGR, "stall_gocapture");

			SimplePinRead read = new SimplePinRead(pin);

			addComponent(read);
			addComponent(goOr);
			addComponent(goPrimeOr);
			addComponent(goPrimeAnd);
			addComponent(doneAnd);
			addComponent(not);
			// addComponent(captureAnd);
			addComponent(goReg);
			addFeedbackPoint(goReg);

			read.getGoPort().setBus(getGoPort().getPeer());

			((Port) goOr.getDataPorts().get(0)).setBus(getGoPort().getPeer());
			((Port) goOr.getDataPorts().get(1)).setBus(goReg.getResultBus());

			not.getDataPort().setBus(read.getResultBus());

			((Port) doneAnd.getDataPorts().get(0)).setBus(goOr.getResultBus());
			((Port) doneAnd.getDataPorts().get(1)).setBus(read.getResultBus());

			// ((Port)captureAnd.getDataPorts().get(0)).setBus(goOr.getResultBus());
			// ((Port)captureAnd.getDataPorts().get(1)).setBus(not.getResultBus());

			((Port) goPrimeAnd.getDataPorts().get(0)).setBus(goReg
					.getResultBus());
			((Port) goPrimeAnd.getDataPorts().get(1))
					.setBus(not.getResultBus());

			((Port) goPrimeOr.getDataPorts().get(0)).setBus(getGoPort()
					.getPeer());
			((Port) goPrimeOr.getDataPorts().get(1)).setBus(goPrimeAnd
					.getResultBus());

			goReg.getClockPort().setBus(getClockPort().getPeer());
			goReg.getResetPort().setBus(getResetPort().getPeer());
			goReg.getInternalResetPort().setBus(getResetPort().getPeer());
			goReg.getDataPort().setBus(goPrimeOr.getResultBus());
			goReg.getResultBus().setSize(1, false);

			exit.getDoneBus().getPeer().setBus(doneAnd.getResultBus());
		}
	}

	/**
	 * This accessor modifies the {@link Referenceable} target state so it may
	 * not execute in parallel with other accesses.
	 */
	public boolean isSequencingPoint() {
		return true;
	}

}
