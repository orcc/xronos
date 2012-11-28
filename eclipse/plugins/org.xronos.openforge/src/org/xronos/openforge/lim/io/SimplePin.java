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
import org.xronos.openforge.lim.Component;
import org.xronos.openforge.lim.Exit;
import org.xronos.openforge.lim.Port;
import org.xronos.openforge.lim.Referenceable;
import org.xronos.openforge.lim.Referencer;
import org.xronos.openforge.lim.StateHolder;
import org.xronos.openforge.lim.Visitor;


/**
 * SimplePin represents nothing more than a sized and named port on the design.
 * It implements the {@link Referenceable} interface so it can be used in
 * asserting scheduling dependencies on any access to it. The directionality of
 * a pin is dependent solely on the types of accesses to it.
 * <p>
 * The SimplePin has a single Port and a single Bus (even though it is not a
 * component) to facilitate connections at the global level. An exception will
 * be thrown if an attempt is made to connect both the Port and the Bus of the
 * same SimplePin object.
 * <p>
 * SimplePins are <b>always</b> unsigned. Signedness can be changed by casting
 * before/after the {@link SimplePinRead} and {@link SimplePinWrite}
 * 
 * <p>
 * Created: Thu Jan 15 10:22:49 2004
 * 
 * @author imiller, last modified by $Author: imiller $
 * @version $Id: SimplePin.java 538 2007-11-21 06:22:39Z imiller $
 */
public abstract class SimplePin extends Component implements Referenceable,
		StateHolder {

	/** The bit width of this pin. */
	private int bitWidth = -1;
	/** The string name of this pin. */
	private String name = "";

	/**
	 * This is a 'dangling' port that is used only to keep track of any bus that
	 * is used to write data to this pin. It is considered dangling because
	 * there is no accessible Component to which it belongs. It is just here to
	 * connect a bus to and provide correct behavior during constant prop. It is
	 * an error for this port and the 'source' bus to both have connections.
	 */
	private Port sink;
	/**
	 * This is a 'dangling' bus that is used to track any consumers of data from
	 * this pin (readers). It is dangling because there is no accessible
	 * component to which it belongs. It is here just to provide connectivity
	 * between the pin and the consumers of the pin data. It is an error for
	 * this bus and the 'sink' port to both have connections.
	 */
	private Bus source;

	/**
	 * Constructs a new SimplePin with the specified bit width and name.
	 * 
	 * @param width
	 *            an int, the number of BITS wide this pin is.
	 * @param pinName
	 *            a 'String'
	 * @throws IllegalArgumentException
	 *             if width <= 0 or if the pinName is empty.
	 */
	public SimplePin(int width, String pinName) {
		if (width <= 0)
			throw new IllegalArgumentException("Illegal pin width " + width);
		if (pinName.length() <= 0)
			throw new IllegalArgumentException("Illegal pin name '" + pinName
					+ "'");

		bitWidth = width;
		name = pinName;

		// Naming stuff
		this.setIDLogical(pinName);
	}

	@Override
	public void accept(Visitor vis) {
		vis.visit(this);
	}

	/**
	 * Allows sub-classes accesses to the source bus.
	 */
	protected Bus getSource() {
		return source;
	}

	/**
	 * Allows sub-classes accesses to the sink port.
	 */
	protected Port getSink() {
		return sink;
	}

	/**
	 * Retrieves the bitwidth of this simple pin.
	 * 
	 * @return an int, defined as the number of <b>bits</b> of this pin.
	 */
	public int getWidth() {
		return bitWidth;
	}

	/**
	 * Retrieves the name given to this simple pin at contstruction.
	 */
	public String getName() {
		return name;
	}

	/**
	 * Returns true if this SimplePin is to be published at the top level of the
	 * implementation. A return of false means that this SimplePin is a top
	 * level entity for routing signals in the design.
	 * 
	 * @return a value of type 'boolean'
	 */
	public boolean isPublished() {
		return true;
	}

	protected void buildPort() {
		if (getSink() == null) {
			sink = makeDataPort(Component.SIDEBAND);
			sink.setSize(getWidth(), false);
		}
	}

	protected void buildBus() {
		if (getSource() == null) {
			assert getExit(Exit.SIDEBAND) == null;
			source = makeExit(0, Exit.SIDEBAND).makeDataBus(Component.SIDEBAND);
			source.setSize(getWidth(), false);
			source.setIDLogical(getName());
		}
	}

	/**
	 * Sets the given valueBus as the source of write data to this pin. An
	 * exception will be thrown if this pin already has a data source bus
	 * defined or if the pin is already defined as having one or more read
	 * accesses (as specified by a call to the connectBus method).
	 * 
	 * @param valueBus
	 *            a non-null 'Bus'
	 * @throws NullPointerException
	 *             if valueBus is null
	 * @throws UnsupportedOperationException
	 *             if connectBus has already been called on this pin or if
	 *             connectPort has already been called.
	 */
	public void connectPort(Bus valueBus) {
		buildPort();

		// The idea here is to have a 'dangling' Port that can be
		// connected to and used simply to enforce constant prop
		// rules. No need to have this be a component with all that
		// overhead
		if (getSource() != null && getSource().isConnected()) {
			throw new UnsupportedOperationException("Source bus of '" + name
					+ "' is already connected.  Cannot connect the sink port");
		}
		if (getSink().isConnected()) {
			throw new UnsupportedOperationException("Sink port of '" + name
					+ "' is already connected");
		}
		getSink().setBus(valueBus);
	}

	/**
	 * Connects the data source bus of this pin to each {@link Port} in the
	 * Collection of ports. An exception will be thrown if this pin already has
	 * a data source bus defined (as specified by a call to the connectBus
	 * method).
	 * 
	 * @param ports
	 *            a Collection of {@link Port} objects.
	 * @throws NullPointerException
	 *             if ports is null
	 * @throws UnsupportedOperationException
	 *             if connectPort has already been called on this pin.
	 */
	public void connectBus(Collection<Port> ports) {
		buildBus();
		// The idea here is to have a 'dangling' Bus that can be
		// connected to and used simply to enforce constant prop
		// rules. No need to have this be a component with all that
		// overhead
		if (getSink() != null && getSink().isConnected()) {
			throw new UnsupportedOperationException(
					"Sink port is already connected.  Cannot hook up the source bus");
		}

		final List<Port> toConnect = new ArrayList<Port>(ports);
		toConnect.removeAll(getSource().getPorts());
		// connect up whats left.
		for (Port port : toConnect) {
			port.setBus(getSource());
		}
	}

	/**
	 * This method returns a new instance of XLatData which is used by
	 * translation to query for data about this SimplePin. This class is used to
	 * intentionally obfuscate access to the internal structure of this pin in
	 * the hopes of discouraging any accesses other than by the translator
	 * 
	 * @return a new {@link XLatData} instance.
	 */
	public XLatData getXLatData() {
		return new XLatData();
	}

	/**
	 * Simple class used only to pass data to the translation engine.
	 */
	public class XLatData {
		public Port getSink() {
			return SimplePin.this.getSink();
		}

		public Bus getSource() {
			return SimplePin.this.getSource();
		}

		public boolean isInput() {

			return sink == null || !sink.isConnected();
		}
	}

	/**
	 * Simply calls {@link Port#pushValueForward} on the sink port of this pin
	 * so that any bits in the connected bus (if any) will be pushed into the
	 * Value of the port.
	 * 
	 * @return a value of type 'boolean'
	 */
	@Override
	protected boolean pushValuesForward() {
		if (getSink() != null)
			return getSink().pushValueForward();
		else
			return false;
	}

	// ///////////////////////////////////////////////////////
	// ///////////////////////////////////////////////////////
	//
	// Referenceable interface
	//
	// ///////////////////////////////////////////////////////
	// ///////////////////////////////////////////////////////

	/**
	 * Tests the referencer types for compatibility and then returns 1 or 0
	 * depending on the type of accessor.
	 * 
	 * @param from
	 *            the prior accessor in source document order.
	 * @param to
	 *            the latter accessor in source document order.
	 */
	@Override
	public int getSpacing(Referencer from, Referencer to) {
		if (from instanceof SimplePinWrite)
			return 1;
		else if ((from instanceof SimplePinRead)
				|| (from instanceof SimplePinStall))
			return 0;
		else
			throw new IllegalArgumentException("Source access to " + this
					+ " is of unknown type " + from.getClass());
	}

	/**
	 * Returns -1 indicating that the referencers must be scheduled using the
	 * default DONE to GO spacing.
	 */
	@Override
	public int getGoSpacing(Referencer from, Referencer to) {
		return -1;
	}

	/**
	 * For debug.
	 */
	@Override
	public String toString() {
		return "SimplePin<" + Integer.toHexString(hashCode()) + ">="
				+ getName();
	}

}// SimplePin
