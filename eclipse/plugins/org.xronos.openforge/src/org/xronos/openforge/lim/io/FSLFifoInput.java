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

import java.util.Collection;
import java.util.Collections;

/**
 * FSLFifoInput creates pins necessary for obtaining data from an input FIFO
 * interface that is FSL compliant.
 * 
 * 
 * <p>
 * Created: Tue Dec 16 12:10:31 2003
 * 
 * @author imiller, last modified by $Author: imiller $
 * @version $Id: FSLFifoInput.java 128 2006-04-04 15:05:31Z imiller $
 */
public class FSLFifoInput extends FifoInput {

	private final String baseName;
	private final SimplePin data;
	private final SimplePin exists;
	private final SimplePin ctrl;
	private final SimplePin clk;
	private final SimplePin read;

	/**
	 * Constructs a new FSLFifoInput instance, creating all the necessary pin
	 * objects and assigning them names of the form
	 * <code><i>idString</i>_S_DIN</code>, etc.
	 * 
	 * @param idString
	 *            a uniquifying String that is pre-pended to the signal names
	 *            which must conform to the specification for signal naming (ie
	 *            FSLx where x is the instance number)
	 * @param width
	 *            an int, the byte bit of the Fifo data path.
	 */
	public FSLFifoInput(String idString, int width) {
		super(width);

		// This class creates all of the FSL slave pins according to
		// the specified interface. The idString will be of the form
		// FSLx. To this idString we must append the _S_yyyyy signal
		// names to generate the following pins:
		/*
		 * FSLx_S_DATA <width> bits wide input FSLx_S_EXISTS 1 bit input
		 * FSLx_S_READ 1 bit output FSLx_S_CONTROL 1 bit input, FSLx_S_CLK 1 bit
		 * input
		 */
		baseName = idString;
		final String pinBaseName = buildPortBaseName(idString);
		// The direction of the pin is dependent only on how we access it
		// this.data = new SimpleFifoPin(this, width * 8, pinBaseName +
		// "_DATA");
		data = new SimpleFifoPin(this, width, pinBaseName + "_DATA");
		exists = new SimpleFifoPin(this, 1, pinBaseName + "_EXISTS");
		ctrl = new SimpleFifoPin(this, 1, pinBaseName + "_CONTROL");
		clk = new SimpleFifoPin(this, 1, pinBaseName + "_CLK");
		read = new SimpleFifoPin(this, 1, pinBaseName + "_READ");

		// The order that these are added here determines the order
		// they show up in the translated inteface.
		addPin(data);
		addPin(exists);
		addPin(read);
		addPin(ctrl);
		addPin(clk);

		// Tie off the unused outputs.
		// None.
	}

	/**
	 * <code>getType</code> returns {@link FifoIF#TYPE_FSL_FIFO}
	 * 
	 * @return an <code>int</code> value
	 */
	@Override
	public int getType() {
		return FifoIF.TYPE_FSL_FIFO;
	}

	@Override
	public String getPortBaseName() {
		return baseName;
	}

	/**
	 * Fifo input ports are slave queues, this method returns portname_S
	 */
	@Override
	protected String buildPortBaseName(String portName) {
		return portName + "_S";
	}

	/**
	 * Returns a subset of {@link #getPins} that are the output pins of the
	 * interface, containing only the read pin.
	 */
	@Override
	public Collection<SimplePin> getOutputPins() {
		return Collections.unmodifiableList(Collections.singletonList(read));
	}

	@Override
	public SimplePin getDataPin() {
		return data;
	}

	@Override
	public SimplePin getSendPin() {
		return exists;
	}

	@Override
	public SimplePin getAckPin() {
		return read;
	}

}// FSLFifoInput
