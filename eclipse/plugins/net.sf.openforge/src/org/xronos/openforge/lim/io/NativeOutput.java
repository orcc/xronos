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
package org.xronos.openforge.lim.io;

/**
 * {@link NativeOutput} is an abstract class which contains only the data part
 * of fifo input
 * 
 * @author Endri Bezati <endri.bezati@epfl.ch>
 * 
 */
public abstract class NativeOutput extends FifoIF {

	protected NativeOutput(int width) {
		super(width);
	}

	/**
	 * Returns a {@link FifoWrite} object that is used to send data to this
	 * FifoIF.
	 * 
	 * @return a {@link FifoAccess}, specifically of type {@link FifoWrite}
	 */
	@Override
	public FifoAccess getAccess() {
		return new FifoWrite(this);
	}

	/**
	 * Returns the output data pin for this interface
	 * 
	 */
	public abstract SimplePin getDataPin();

	/**
	 * Returns false due to the fact that the data pin for this interface is an
	 * output to the design.
	 * 
	 * @return false
	 */
	@Override
	public boolean isInput() {
		return false;
	}

	@Override
	public String toString() {
		return super.toString().replaceAll("net.sf.openforge.lim.io.", "")
				+ "{" + getDataPin().getName() + "}";
	}

}
