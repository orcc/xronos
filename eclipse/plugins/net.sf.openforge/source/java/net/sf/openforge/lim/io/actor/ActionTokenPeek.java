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
import net.sf.openforge.lim.Port;
import net.sf.openforge.lim.io.FifoAccess;
import net.sf.openforge.lim.io.FifoIF;
import net.sf.openforge.lim.io.SimplePin;
import net.sf.openforge.lim.io.SimplePinRead;

/**
 * ActionTokenPeek is a {@link Component} which represents an access to the
 * ActorPort in order to peek at the value specified by an offset index into the
 * queue. The process of peeking at a value does not affect the number or value
 * of tokens in the queue. The component has a single output bus and always
 * completes combinationally.
 * 
 * <p>
 * Created: Fri Aug 26 15:28:50 2005
 * 
 * @author imiller, last modified by $Author: imiller $
 * @version $Id: ActionTokenPeek.java 100 2006-02-03 22:49:08Z imiller $
 */
public class ActionTokenPeek extends FifoAccess {

	
	private ActionTokenPeek(FifoIF fifoIF, SimplePin indexPin, SimplePin dataPin) {
		super(fifoIF);

		// Excluding 'sideband' ports/buses (those connecting to pins)
		// there is a single index port and a single result bus on
		// this module. Since the peek is always valid, the done
		// is not needed. The Go is needed to drive the pin write.
		// unused right now
		@SuppressWarnings("unused")
		Port index = makeDataPort(); 
		Exit exit = makeExit(1);
		Bus result = (Bus) exit.getDataBuses().get(0);
		// Bus done = exit.getDoneBus();
		// done.setUsed(true);
		result.setUsed(true);

		// Is always combinational.
		exit.setLatency(Latency.ZERO);

		/*
		 * We only support token peeks to the head of the queue. If we leave
		 * this pin write here then resource sequencing will enforce that 2
		 * peeks are in different clock cyles due to their index port writes.
		 * final SimplePinWrite indexWrite = new SimplePinWrite(indexPin);
		 * indexWrite.getDataPort().setBus(index.getPeer());
		 * indexWrite.getGoPort().setBus(getGoPort().getPeer());
		 * addComponent(indexWrite);
		 */

		final SimplePinRead peek = new SimplePinRead(dataPin);
		peek.getGoPort().setBus(getGoPort().getPeer());

		addComponent(peek);

		// Hook the peek pin read to the result bus.
		result.getPeer().setBus(peek.getResultBus());
	}

	public ActionTokenPeek(ActorScalarInput fifoIF) {
		this(fifoIF, /* fifoIF.getIndexPin() */null, fifoIF.getDataPin());
	}

	// public ActionTokenPeek (ActorScalarOutput fifoIF)
	// {
	// //this(fifoIF, null, null);
	// throw new
	// UnsupportedOperationException("Peeking at output interface not yet supported");
	// }

	// public ActionTokenPeek (ActorObjectInput fifoIF)
	// {
	// }

	/**
	 * This accessor may execute in parallel with other similar (non state
	 * modifying) accesses.
	 */
	public boolean isSequencingPoint() {
		return false;
	}

}// ActionTokenPeek
