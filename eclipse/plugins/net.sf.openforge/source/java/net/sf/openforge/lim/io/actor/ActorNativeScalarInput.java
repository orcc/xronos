/*
 * Copyright (c) 2012, Ecole Polytechnique Fédérale de Lausanne
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 *   * Redistributions of source code must retain the above copyright notice,
 *     this list of conditions and the following disclaimer.
 *   * Redistributions in binary form must reproduce the above copyright notice,
 *     this list of conditions and the following disclaimer in the documentation
 *     and/or other materials provided with the distribution.
 *   * Neither the name of the Ecole Polytechnique Fédérale de Lausanne nor the names of its
 *     contributors may be used to endorse or promote products derived from this
 *     software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY
 * WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 */
package net.sf.openforge.lim.io.actor;

import java.util.Collection;

import net.sf.openforge.lim.Bus;
import net.sf.openforge.lim.Component;
import net.sf.openforge.lim.Exit;
import net.sf.openforge.lim.Latency;
import net.sf.openforge.lim.io.FifoAccess;
import net.sf.openforge.lim.io.FifoID;
import net.sf.openforge.lim.io.FifoIF;
import net.sf.openforge.lim.io.FifoRead;
import net.sf.openforge.lim.io.NativeInput;
import net.sf.openforge.lim.io.SimpleFifoPin;
import net.sf.openforge.lim.io.SimplePin;
import net.sf.openforge.lim.io.SimplePinRead;

/**
 * @author Endri Bezati
 * 
 */
public class ActorNativeScalarInput extends NativeInput implements ActorPort {

	private final String baseName;
	private final SimplePin data;

	/**
	 * @param width
	 */
	public ActorNativeScalarInput(FifoID fifoID) {
		super(fifoID.getBitWidth());
		baseName = fifoID.getName();
		final String pinBaseName = buildPortBaseName(baseName);

		data = new SimpleFifoPin(this, getWidth(), pinBaseName + "_DATA");
		addPin(data);
	}

	/**
	 * 
	 * Asserts false
	 */
	@Override
	public void setAttribute(int type, String value) {
		assert false : "No supported attributes";

	}

	@Override
	public FifoAccess getAccess(boolean blocking) {
		if (blocking) {
			return new FifoRead(this);
		} else {
			return new ActorNativeInputRead(this);
		}
	}

	/**
	 * 
	 * A Native Port does not contain a COUNT Pin
	 */
	@Override
	public Component getCountAccess() {
		throw new UnsupportedOperationException(
				"Cannot get the Count from an native port");
	}

	@Override
	public Component getPeekAccess() {
		return new ActionTokenPeek(this);
	}

	/**
	 * 
	 * A Native port does not contain a SEND Pin
	 */
	@Override
	public Component getStatusAccess() {
		throw new UnsupportedOperationException(
				"Cannot get the status from an native port");
	}

	/**
	 * 
	 * Return the DATA Pin
	 */
	@Override
	public SimplePin getDataPin() {
		return data;
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
	 * ActorNativeScalarInput ports have no special naming requirements, this
	 * method returns portName
	 */
	@Override
	protected String buildPortBaseName(String portName) {
		return portName;
	}

	/**
	 * Returns the Ports base name
	 */
	@Override
	public String getPortBaseName() {
		return baseName;
	}

	/**
	 * Returns a null, a Native Input port has only the DATA pin.
	 */
	@Override
	public Collection<SimplePin> getOutputPins() {
		return null;
	}

	private class ActorNativeInputRead extends FifoRead {
		private ActorNativeInputRead(ActorNativeScalarInput asi) {
			super(asi, Latency.ZERO);

			Bus done = getExit(Exit.DONE).getDoneBus();
			Bus result = getExit(Exit.DONE).getDataBuses().get(0);

			final SimplePinRead din = new SimplePinRead(asi.getDataPin());
			addComponent(din);

			result.getPeer().setBus(din.getResultBus());
			done.getPeer().setBus(getGoPort().getPeer());
		}

		@Override
		public boolean consumesClock() {
			return false;
		}
	}

}
