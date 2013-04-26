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

package org.xronos.openforge.lim.io.actor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.xronos.openforge.lim.Component;
import org.xronos.openforge.lim.Latency;
import org.xronos.openforge.lim.io.FifoAccess;
import org.xronos.openforge.lim.io.FifoID;
import org.xronos.openforge.lim.io.FifoIF;
import org.xronos.openforge.lim.io.FifoWrite;
import org.xronos.openforge.lim.io.NativeOutput;
import org.xronos.openforge.lim.io.SimpleFifoPin;
import org.xronos.openforge.lim.io.SimplePin;
import org.xronos.openforge.lim.io.SimplePinWrite;

public class ActorNativeScalarOutput extends NativeOutput implements ActorPort {

	private class ActorScalarSimpleOutputWrite extends FifoWrite {
		// This class assumes that the output write will always
		// succeed. So, set the data, strobe the send high and
		// finish.
		private ActorScalarSimpleOutputWrite(
				ActorNativeScalarOutput actorNativeScalarOutput) {
			// super(aso, Latency.ONE);
			// The spacing (handled by the getSpacing method) ensures
			// that we do not have a conflict on the resource.
			super(actorNativeScalarOutput, Latency.ZERO);

			final SimplePinWrite dout = new SimplePinWrite(
					actorNativeScalarOutput.getDataPin());

			addComponent(dout);

			dout.getDataPort().setBus(getDataPorts().get(0).getPeer());
			dout.getGoPort().setBus(getGoPort().getPeer());

		}
	}

	private final String baseName;

	private final SimplePin data;

	public ActorNativeScalarOutput(FifoID fifoID) {
		super(fifoID.getBitWidth());
		baseName = fifoID.getName();
		final String pinBaseName = buildPortBaseName(baseName);

		data = new SimpleFifoPin(this, getWidth(), pinBaseName + "_DATA");
		addPin(data);
	}

	@Override
	protected String buildPortBaseName(String portName) {
		return portName;
	}

	/**
	 * Returns a {@link FifoWrite} object that is used to obtain data from this
	 * FifoIF.
	 * 
	 * @return a blocking {@link FifoAccess}
	 */
	@Override
	public FifoAccess getAccess(boolean blocking) {
		return new ActorScalarSimpleOutputWrite(this);
	}

	@Override
	public Component getCountAccess() {
		throw new UnsupportedOperationException(
				"NativeOutput channels do not have token count facility");
	}

	@Override
	public SimplePin getDataPin() {
		return data;
	}

	/**
	 * Returns a subset of {@link #getPins} that are the output pins of the
	 * interface, containing only the data SimplePin.
	 */
	@Override
	public Collection<SimplePin> getOutputPins() {
		List<SimplePin> list = new ArrayList<SimplePin>();
		list.add(data);
		return Collections.unmodifiableList(list);
	}

	@Override
	public Component getPeekAccess() {
		throw new UnsupportedOperationException(
				"Peeking at output interface not supported");
	}

	@Override
	public String getPortBaseName() {
		return baseName;
	}

	@Override
	public Component getStatusAccess() {
		throw new UnsupportedOperationException(
				"Status of NativeOutput interface not supported");
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
}
