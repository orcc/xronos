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

package org.xronos.openforge.lim.io;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.xronos.openforge.lim.Bus;
import org.xronos.openforge.lim.Port;


/**
 * SimpleInternalPin is a type of SimplePin which is only used for communication
 * between 2 concurrent tasks. This pin is never published on the top level
 * design.
 * 
 * 
 * <p>
 * Created: Mon Aug 29 13:09:09 2005
 * 
 * @author imiller, last modified by $Author: imiller $
 * @version $Id: SimpleInternalPin.java 23 2005-09-09 18:45:32Z imiller $
 */
public class SimpleInternalPin extends SimplePin {

	public SimpleInternalPin(int width, String name) {
		super(width, name);
	}

	/**
	 * Overrides the super to allow for both the source and sink to be
	 * connected.
	 * 
	 * @param valueBus
	 *            a value of type 'Bus'
	 * @throws IllegalArgumentException
	 *             if valueBus is null
	 */
	@Override
	public void connectPort(Bus valueBus) {
		buildPort();
		if (valueBus == null)
			throw new IllegalArgumentException("Cannot connect null to port");

		if (getSink().isConnected()) {
			throw new UnsupportedOperationException(
					"Sink port is already connected");
		}
		getSink().setBus(valueBus);
	}

	/**
	 * Overrides the super in order to allow both source and sink to be
	 * connected
	 * 
	 * @param ports
	 *            a Collection of {@link Port} objects.
	 * @throws NullPointerException
	 *             if ports is null
	 * @throws UnsupportedOperationException
	 *             if connectPort has already been called on this pin.
	 */
	@Override
	public void connectBus(Collection<Port> ports) {
		buildBus();
		// The idea here is to have a 'dangling' Bus that can be
		// connected to and used simply to enforce constant prop
		// rules. No need to have this be a component with all that
		// overhead

		final List<Port> toConnect = new ArrayList<Port>(ports);
		toConnect.removeAll(getSource().getPorts());
		// connect up what's left.
		for (Port port : toConnect) {
			(port).setBus(getSource());
		}
	}

	/**
	 * Returns true if this SimplePin is to be published at the top level of the
	 * implementation. A return of false means that this SimplePin is a top
	 * level entity for routing signals in the design.
	 * 
	 * @return false
	 */
	@Override
	public boolean isPublished() {
		return false;
	}

}// SimpleInternalPin
