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

import java.util.Collection;
import java.util.Collections;

import net.sf.openforge.lim.Bus;
import net.sf.openforge.lim.Component;
import net.sf.openforge.lim.Exit;
import net.sf.openforge.lim.Latency;
import net.sf.openforge.lim.Referencer;
import net.sf.openforge.lim.io.FifoAccess;
import net.sf.openforge.lim.io.FifoID;
import net.sf.openforge.lim.io.FifoIF;
import net.sf.openforge.lim.io.FifoInput;
import net.sf.openforge.lim.io.FifoRead;
import net.sf.openforge.lim.io.SimpleFifoPin;
import net.sf.openforge.lim.io.SimplePin;
import net.sf.openforge.lim.io.SimplePinRead;
import net.sf.openforge.lim.io.SimplePinWrite;

/**
 * ActorScalarInput is a specialized fifo input interface which contains the
 * necessary infrastructure to support scalar data types. This includes:
 * <p>
 * <ul>
 * <li>Data input</li>
 * <li>index output</li>
 * <li>token count input</li>
 * </ul>
 * 
 * <p>
 * Created: Fri Aug 26 14:32:32 2005
 * 
 * @author imiller, last modified by $Author: Endri Bezati $
 * @version $Id: ActorScalarInput.java 236 2006-07-20 16:57:10Z imiller $
 */
public class ActorScalarInput extends FifoInput implements ActorPort {

	private class ActorScalarSimpleInputRead extends FifoRead {
		private ActorScalarSimpleInputRead(ActorScalarInput asi) {
			super(asi, Latency.ZERO);

			Bus done = getExit(Exit.DONE).getDoneBus();
			Bus result = getExit(Exit.DONE).getDataBuses().get(0);

			final SimplePinRead din = new SimplePinRead(asi.getDataPin());
			final SimplePinWrite ack = new SimplePinWrite(asi.getAckPin());
			addComponent(din);
			addComponent(ack);

			result.getPeer().setBus(din.getResultBus());
			done.getPeer().setBus(getGoPort().getPeer());
			ack.getDataPort().setBus(getGoPort().getPeer());
			ack.getGoPort().setBus(getGoPort().getPeer());
		}

		@Override
		public boolean consumesClock() {
			return false;
		}
	}
	private final SimplePin ack;
	private final String baseName;
	private final SimplePin data;

	private final SimplePin send;

	/** The input pin which specifies the number of available tokens */
	private final SimplePin tokenCount;

	// public ActorScalarInput (String idString, int width)
	public ActorScalarInput(FifoID fifoID) {
		super(fifoID.getBitWidth());

		baseName = fifoID.getName();
		final String pinBaseName = buildPortBaseName(baseName);

		data = new SimpleFifoPin(this, getWidth(), pinBaseName + "_DATA");
		send = new SimpleFifoPin(this, 1, pinBaseName + "_SEND");
		ack = new SimpleFifoPin(this, 1, pinBaseName + "_ACK");
		tokenCount = new SimpleFifoPin(this, ActorPort.COUNT_PORT_WIDTH,
				pinBaseName + "_COUNT");

		addPin(data);
		addPin(send);
		addPin(ack);
		addPin(tokenCount);
	}

	/**
	 * ActorScalarInput ports have no special naming requirements, this method
	 * returns portname
	 */
	@Override
	protected String buildPortBaseName(String portName) {
		return portName;
	}

	/**
	 * Returns a {@link FifoRead} object that is used to obtain data from this
	 * FifoIF.
	 * 
	 * @return a blocking {@link FifoAccess}
	 */
	@Override
	public FifoAccess getAccess() {
		return getAccess(true);
	}

	/**
	 * Returns a {@link FifoRead} object that is used to obtain data from this
	 * FifoIF.
	 * 
	 * @param blocking
	 *            if set true causes a blocking read to be returned, otherwise a
	 *            non-blocking read is returned.
	 * @return a {@link FifoAccess}
	 */
	@Override
	public FifoAccess getAccess(boolean blocking) {
		if (blocking)
			return new FifoRead(this);
		else
			return new ActorScalarSimpleInputRead(this);
	}

	/**
	 * Returns the output ack pin which indicates that this interface is
	 * acknowledging reciept of the current send value
	 */
	@Override
	public SimplePin getAckPin() {
		return ack;
	}

	@Override
	public Component getCountAccess() {
		return new ActionTokenCountRead(this);
	}

	SimplePin getCountPin() {
		return tokenCount;
	}

	/** Returns the input data pin */
	@Override
	public SimplePin getDataPin() {
		return data;
	}

	/**
	 * Returns a subset of {@link #getPins} that are the output pins of the
	 * interface, containing only the data, write, and ctrl pins.
	 */
	@Override
	public Collection<SimplePin> getOutputPins() {
		return Collections.singletonList(ack);
	}

	@Override
	public Component getPeekAccess() {
		return new ActionTokenPeek(this);
	}

	@Override
	public String getPortBaseName() {
		return baseName;
	}

	/**
	 * Returns the input send pin which indicates that a value is being sent TO
	 * this input interface.
	 */
	@Override
	public SimplePin getSendPin() {
		return send;
	}

	/**
	 * Tests the referencer types and then returns 1 or 0 depending on the types
	 * of each accessor.
	 * 
	 * @param from
	 *            the prior accessor in source document order.
	 * @param to
	 *            the latter accessor in source document order.
	 */
	@Override
	public int getSpacing(Referencer from, Referencer to) {
		// Options for accesses to an input are
		// FifoRead (ActorScalarSimpleInputRead)
		// ActionTokenCountRead
		// ActionTokenPeek
		// ActionPortStatus

		if (from instanceof FifoRead) {
			return 1;
		} else if ((from instanceof ActionTokenCountRead)
				|| (from instanceof ActionTokenPeek)
				|| (from instanceof ActionPortStatus)) {
			return 0;
		} else {
			throw new IllegalArgumentException("Source access to " + this
					+ " is of unknown type " + from.getClass());
		}
	}

	@Override
	public Component getStatusAccess() {
		return new ActionPortStatus(this);
	}

	/**
	 * <code>getType</code> returns {@link FifoIF#TYPE_ACTOR_QUEUE}
	 * 
	 * @return an <code>int</code> value
	 */
	@Override
	public int getType() {
		return FifoIF.TYPE_ACTOR_QUEUE;
	}

	/**
	 * asserts false
	 */
	@Override
	public void setAttribute(int type, String value) {
		assert false : "No supported attributes";
	}

}// ActorScalarInput
