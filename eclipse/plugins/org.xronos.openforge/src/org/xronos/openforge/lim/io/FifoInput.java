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

/**
 * FifoInput is an abstract class which contains the common behavior for all
 * types of fifo input
 * 
 * 
 * <p>
 * Created: Tue Dec 16 12:10:31 2003
 * 
 * @author imiller, last modified by $Author: imiller $
 * @version $Id: FifoInput.java 95 2006-01-19 22:12:38Z imiller $
 */
public abstract class FifoInput extends FifoIF {

	/**
	 * Constructs a new FifoInput instance
	 * 
	 * @param width
	 *            an int, the byte bit of the Fifo data path.
	 */
	public FifoInput(int width) {
		super(width);
	}

	/**
	 * Returns a {@link FifoRead} object that is used to obtain data from this
	 * FifoIF.
	 * 
	 * @return a {@link FifoAccess}, specifically of type {@link FifoRead}
	 */
	@Override
	public FifoAccess getAccess() {
		return new FifoRead(this);
	}

	/**
	 * Returns true due to the fact that the data pin for this interface is an
	 * input to the design.
	 * 
	 * @return true
	 */
	@Override
	public boolean isInput() {
		return true;
	}

	/** Returns the input data pin */
	public abstract SimplePin getDataPin();

	/**
	 * Returns the input send pin which indicates that a value is being sent TO
	 * this input interface.
	 */
	public abstract SimplePin getSendPin();

	/**
	 * Returns the output ack pin which indicates that this interface is
	 * acknowledging reciept of the current send value
	 */
	public abstract SimplePin getAckPin();

	@Override
	public String toString() {
		return super.toString().replaceAll("net.sf.openforge.lim.io.", "")
				+ "{" + getDataPin().getName() + "}";
	}

}// FifoInput
