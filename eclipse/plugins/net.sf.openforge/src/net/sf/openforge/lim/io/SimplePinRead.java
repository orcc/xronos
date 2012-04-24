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

import net.sf.openforge.lim.Bus;
import net.sf.openforge.lim.Component;
import net.sf.openforge.lim.Exit;
import net.sf.openforge.lim.Port;
import net.sf.openforge.lim.Referenceable;
import net.sf.openforge.lim.Referencer;
import net.sf.openforge.lim.Value;
import net.sf.openforge.lim.Visitable;
import net.sf.openforge.lim.Visitor;

/**
 * SimplePinRead is a lightweight LIM node that is used to simply wire a
 * SimplePin (pin on the design) straight through to a particular point in the
 * LIM. There is no logic associated with this node and it does not consume a GO
 * or produce a DONE.
 * <p>
 * Note that pin reads and writes are always unsigned and you must cast to
 * signed if you need signed data.
 * 
 * <p>
 * Created: Thu Jan 15 11:19:38 2004
 * 
 * @author imiller, last modified by $Author: imiller $
 * @version $Id: SimplePinRead.java 122 2006-03-30 18:05:17Z imiller $
 */
public class SimplePinRead extends Component implements Visitable, Referencer {

	/** The targetted pin. Never null */
	private SimplePin targetPin;

	/**
	 * Constructs a new SimplePinRead, which reads the given target pin.
	 * 
	 * @param targetPin
	 *            a {@link SimplePin}
	 * @throws IllegalArgumentException
	 *             if targetPin is null.
	 */
	public SimplePinRead(SimplePin targetPin) {
		super(0); // Defer the making of the port until pin connection

		if (targetPin == null) {
			throw new IllegalArgumentException(
					"Cannot have an access to null pin");
		}

		this.targetPin = targetPin;

		makeExit(1); // Create one exit with one bus, the read value.
	}

	/**
	 * Returns the data bus used to supply the {@link SimplePin SimplePins}
	 * value to the LIM.
	 * 
	 * @return a non-null Bus
	 */
	public Bus getResultBus() {
		return getExit(Exit.DONE).getDataBuses().get(0);
	}

	@Override
	public void accept(Visitor vis) {
		vis.visit(this);
	}

	/**
	 * Returns the {@link Referenceable} {@link SimplePin} which this node
	 * targets.
	 */
	@Override
	public Referenceable getReferenceable() {
		return targetPin;
	}

	/**
	 * This accessor may execute in parallel with other similar (non state
	 * modifying) accesses.
	 */
	@Override
	public boolean isSequencingPoint() {
		return false;
	}

	@Override
	public boolean pushValuesForward() {
		Port sideband = getDataPorts().isEmpty() ? null : (Port) getDataPorts()
				.get(0);
		Value newValue;
		if (sideband != null && sideband.isConnected()) {
			newValue = sideband.getValue();
		} else {
			// The input pin always supplies 'n' bits of valid data
			newValue = new Value(targetPin.getWidth(), false);
		}

		return getResultBus().pushValueForward(newValue);
	}

	@Override
	public boolean pushValuesBackward() {
		// Do nothing except ensure that the sideband port has a
		// value as there is no push-back to the pin.
		if (!getDataPorts().isEmpty()) {
			getDataPorts().get(0).pushValueBackward(
					new Value(targetPin.getWidth(), false));
		}
		return false;
	}

}// SimplePinRead
