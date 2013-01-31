/*
 * Copyright (c) 2013, Ecole Polytechnique Fédérale de Lausanne
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
package org.xronos.openforge.lim.io;

import java.util.Collection;
import java.util.Collections;

/**
 * An AXI4-Stream Slave Interface
 * 
 * @author Endri Bezati
 * 
 */
public class AXI4StreamInput extends FifoInput {

	/** AXI Signals **/
	private final SimplePin aclk;

	/** Interface Base Name **/
	private final String baseName;
	private final SimplePin tdata;
	private final SimplePin tlast;
	private final SimplePin tready;
	private final SimplePin tvalid;

	public AXI4StreamInput(String idString, int width) {
		super(width);
		this.baseName = idString;
		final String pinBaseName = buildPortBaseName(idString);

		aclk = new SimpleFifoPin(this, 1, pinBaseName + "_ACLK");
		tvalid = new SimpleFifoPin(this, 1, pinBaseName + "_TVALID");
		tready = new SimpleFifoPin(this, 1, pinBaseName + "_TREADY");
		tdata = new SimpleFifoPin(this, width, pinBaseName + "_TDATA");
		tlast = new SimpleFifoPin(this, 1, pinBaseName + "_TLAST");

		// The order that these are added here determines the order
		// they show up in the translated interface.
		addPin(aclk);
		addPin(tvalid);
		addPin(tready);
		addPin(tdata);
		addPin(tlast);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.xronos.openforge.lim.io.FifoIF#buildPortBaseName(java.lang.String)
	 */
	@Override
	protected String buildPortBaseName(String portName) {
		return "S_AXIS_" + portName;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.xronos.openforge.lim.io.FifoInput#getAckPin()
	 */
	@Override
	public SimplePin getAckPin() {
		return tready;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.xronos.openforge.lim.io.FifoInput#getDataPin()
	 */
	@Override
	public SimplePin getDataPin() {
		return tdata;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.xronos.openforge.lim.io.FifoIF#getOutputPins()
	 */
	@Override
	public Collection<SimplePin> getOutputPins() {
		return Collections.unmodifiableList(Collections.singletonList(tready));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.xronos.openforge.lim.io.FifoIF#getPortBaseName()
	 */
	@Override
	public String getPortBaseName() {
		return baseName;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.xronos.openforge.lim.io.FifoInput#getSendPin()
	 */
	@Override
	public SimplePin getSendPin() {
		return tvalid;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.xronos.openforge.lim.io.FifoIF#getType()
	 */
	@Override
	public int getType() {
		return FifoIF.TYPE_AXI4_STREAM_FIFO;
	}

}
