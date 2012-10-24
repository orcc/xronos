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

import net.sf.openforge.lim.Component;
import net.sf.openforge.lim.io.FifoID;
import net.sf.openforge.lim.io.FifoInput;
import net.sf.openforge.lim.io.SimpleFifoPin;
import net.sf.openforge.lim.io.SimplePin;

/**
 * ActorCircularBufferInput is a specialized fifo input which contains the
 * necessary infrastructure to support vector data types.
 * 
 * @author Endri Bezati
 * 
 */
public class ActorCircularBufferInput extends FifoInput implements ActorPort {

	private final String baseName;
	private final SimplePin data;
	private final SimplePin send;
	private final SimplePin ack;
	
	public ActorCircularBufferInput(FifoID fifoID) {
		super(fifoID.getBitWidth());
		
		baseName = fifoID.getName();
		final String pinBaseName = buildPortBaseName(baseName);

		data = new SimpleFifoPin(this, getWidth(), pinBaseName + "_DATA");
		send = new SimpleFifoPin(this, 1, pinBaseName + "_SEND");
		ack = new SimpleFifoPin(this, 1, pinBaseName + "_ACK");
		
		addPin(data);
		addPin(send);
		addPin(ack);
		
	}

	
	@Override
	public void setAttribute(int type, String value) {
		// TODO Auto-generated method stub

	}

	
	@Override
	public Component getAccess(boolean blocking) {
		// TODO Auto-generated method stub
		return null;
	}

	
	@Override
	public Component getCountAccess() {
		// TODO Auto-generated method stub
		return null;
	}

	
	@Override
	public Component getPeekAccess() {
		// TODO Auto-generated method stub
		return null;
	}

	
	@Override
	public Component getStatusAccess() {
		// TODO Auto-generated method stub
		return null;
	}

	
	@Override
	public SimplePin getDataPin() {
		// TODO Auto-generated method stub
		return null;
	}

	
	@Override
	public SimplePin getSendPin() {
		// TODO Auto-generated method stub
		return null;
	}

	
	@Override
	public SimplePin getAckPin() {
		// TODO Auto-generated method stub
		return null;
	}

	
	@Override
	public int getType() {
		// TODO Auto-generated method stub
		return 0;
	}

	
	@Override
	public Collection<SimplePin> getOutputPins() {
		// TODO Auto-generated method stub
		return null;
	}

	
	@Override
	protected String buildPortBaseName(String portName) {
		// TODO Auto-generated method stub
		return null;
	}

	
	@Override
	public String getPortBaseName() {
		// TODO Auto-generated method stub
		return null;
	}

}
