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

package net.sf.openforge.lim.io.actor;

import net.sf.openforge.lim.Bus;
import net.sf.openforge.lim.Component;
import net.sf.openforge.lim.Exit;
import net.sf.openforge.lim.Latency;
import net.sf.openforge.lim.io.FifoAccess;
import net.sf.openforge.lim.io.SimplePinRead;

/**
 * ActionTokenCountRead is a {@link Component} which represents an atomic access
 * to the 'token count' associated with an input Actor interface. The component
 * has a single output bus and always completes combinationally.
 * 
 * <p>
 * Created: Fri Aug 26 15:28:50 2005
 * 
 * @author imiller, last modified by $Author: imiller $
 * @version $Id: ActionTokenCountRead.java 62 2005-11-17 18:10:30Z imiller $
 */
public class ActionTokenCountRead extends FifoAccess {

	public ActionTokenCountRead(ActorScalarInput fifoIF) {
		super(fifoIF);

		// Excluding 'sideband' ports/buses (those connecting to pins)
		// there is a single result bus on this module. Since the
		// count is always valid, the go/done are not needed
		Exit exit = makeExit(1);
		Bus result = exit.getDataBuses().get(0);
		// Bus done = exit.getDoneBus();
		// done.setUsed(true);
		result.setUsed(true);

		// Is always combinational.
		exit.setLatency(Latency.ZERO);

		final SimplePinRead count = new SimplePinRead(fifoIF.getCountPin());
		count.getGoPort().setBus(getGoPort().getPeer());
		addComponent(count);

		// Hook the count pin read to the result bus.
		result.getPeer().setBus(count.getResultBus());
	}

	// public ActionTokenCountRead (ActorObjectInput fifoIF)
	// {
	// }

	/**
	 * This accessor may execute in parallel with other similar (non state
	 * modifying) accesses.
	 */
	@Override
	public boolean isSequencingPoint() {
		return false;
	}

}// ActionTokenCountRead
