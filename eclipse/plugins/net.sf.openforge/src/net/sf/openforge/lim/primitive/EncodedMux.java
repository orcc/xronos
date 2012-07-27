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
package net.sf.openforge.lim.primitive;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import net.sf.openforge.lim.Bit;
import net.sf.openforge.lim.Exit;
import net.sf.openforge.lim.Port;
import net.sf.openforge.lim.Value;
import net.sf.openforge.lim.Visitor;

/**
 * An Encoded Mux accepts n data signals and a select signal which is m bits
 * wide such that 2^m >= n. The result bus is set equal to the input of the
 * input port selected by the select signal
 * 
 * @version $Id: EncodedMux.java 12 2005-08-11 19:46:07Z imiller $
 */
public class EncodedMux extends Primitive {

	/** The encoded select value */
	private Port selectPort;

	private List<Port> inputPorts = Collections.emptyList();

	/**
	 * Constructor for the Mux object
	 * 
	 * @param size
	 *            The number inputs
	 */
	public EncodedMux(int size) {
		super(0);
		selectPort = makeDataPort();

		if (size < 1) {
			throw new IllegalArgumentException("size too small");
		}

		if (size > 0) {
			for (int i = 0; i < size; i++) {
				makeMuxEntry();
			}
		}
	} // Mux()

	/**
	 * Returns the select ports (which is stored as a data).
	 * 
	 * @return The selectPort value
	 */
	public Port getSelectPort() {
		return selectPort;
	}

	/**
	 * Gets the Data Port which is paird with the given Go port.
	 */
	public Port getDataPort(int num) {
		return inputPorts.get(num);
	}

	@Override
	public List<Port> getDataPorts() {
		return Collections.unmodifiableList(inputPorts);
	}

	@Override
	public boolean removeDataPort(Port port) {
		assert false : "remove data port not supported for " + this;
		return false;
	}

	/**
	 * Gets the gate depth of this component. This is the maximum number of
	 * gates that any input signal must traverse before reaching an {@link Exit}
	 * .
	 * 
	 * @return a non-negative integer
	 */
	@Override
	public int getGateDepth() {
		if (isPassThrough()) {
			return 0;
		}

		final int inputCount = getDataPorts().size();
		final int selectWidth = log2(inputCount);
		return log2(inputCount) + log2(selectWidth);
	}

	/**
	 * Makes a new mux entry. The Go/Data pair are stored in a Map with the Go
	 * as the key and the Data as the value.
	 * 
	 * @return the Go port
	 */
	public Port makeMuxEntry() {
		Port dataPort = makeDataPort();

		if (inputPorts == Collections.<Port> emptyList()) {
			inputPorts = new ArrayList<Port>(7);
		}
		inputPorts.add(dataPort);

		return dataPort;
	}

	@Override
	public List<Port> getPorts() {
		List<Port> ports = new ArrayList<Port>();
		ports.add(getClockPort());
		ports.add(getResetPort());
		ports.add(getGoPort());
		ports.add(getSelectPort());
		ports.addAll(getDataPorts());
		return ports;
	}

	public int getSize() {
		return dataPorts.size();
	}

	/**
	 * An Mux has no wait.
	 * 
	 * @return false
	 */
	@Override
	public boolean hasWait() {
		return false;
	}

	@Override
	public void accept(Visitor v) {
		v.visit(this);
	}

	/*
	 * =================================================== Begin new constant
	 * prop rules implementation.
	 */

	/**
	 * Bit states are dont care if ALL data ports are dont care, constant if all
	 * data ports are same constant and pass through if all data ports are same
	 * pass through bit.
	 */
	@Override
	public boolean pushValuesForward() {
		/*
		 * The pushed value must be big enough to accommodate the largest input.
		 */
		int maxSize = 0;
		boolean isSigned = false;
		for (Port port : getDataPorts()) {
			maxSize = Math.max(maxSize, port.getValue().getSize());
			isSigned = isSigned || port.getValue().isSigned();
		}
		Value pushedValue = new Value(maxSize, isSigned);

		final Value selectValue = getSelectPort().getValue();
		if (selectValue.isConstant()) {
			/*
			 * If the select value is constant, that means the same input is
			 * always chosen, so push that one through.
			 */
			final int selection = (int) (selectValue.getValueMask() & selectValue
					.getConstantMask());
			assert (selection >= 0) && (selection < getDataPorts().size()) : "select out of bounds: "
					+ selection + " size: " + getDataPorts().size();

			final Port selectedPort = getDataPorts().get(selection);
			pushedValue = selectedPort.getValue();
		} else {
			/*
			 * Otherwise the input values have to be combined to produce the
			 * value to be pushed.
			 */
			for (int i = 0; i < pushedValue.getSize(); i++) {
				final Iterator<Port> dataIter = getDataPorts().iterator();
				assert dataIter.hasNext() : "Illegal encoded mux with no data ports";
				Bit bit = null;
				do {
					final Value portValue = dataIter.next().getValue();
					final Bit padBit = portValue.isSigned() ? portValue
							.getBit(portValue.getSize() - 1) : Bit.ZERO;
					final Bit nextBit = i < portValue.getSize() ? portValue
							.getBit(i) : padBit;
					if (bit == null) // initial condition
					{
						bit = nextBit;
					}
					if (bit != nextBit) // different bits. Default to CARE and
										// jump out of loop
					{
						bit = Bit.CARE;
					}
				} while (dataIter.hasNext() && bit != Bit.CARE);
				pushedValue.setBit(i, bit);
			}
		}
		@SuppressWarnings("unused")
		Value res = getResultBus().getValue();
		return getResultBus().pushValueForward(pushedValue);
	}

	/**
	 * Any constant or dont care bits on the result propagate backwards to be
	 * dont care bits on all data inputs.
	 */
	@Override
	public boolean pushValuesBackward() {
		boolean mod = false;

		Value resultBusValue = getResultBus().getValue();

		for (Port dataPort : getDataPorts()) {
			Value dataPortValue = dataPort.getValue();
			Value newValue = new Value(dataPortValue.getSize(),
					dataPortValue.isSigned());

			for (int i = 0; i < dataPortValue.getSize(); i++) {
				// if (!resultBusValue.getBit(i).isCare())
				Bit bit = resultBusValue.getBit(i);
				if (!bit.isCare() || bit.isConstant()) {
					newValue.setBit(i, Bit.DONT_CARE);
				}
			}

			mod |= dataPort.pushValueBackward(newValue);
		}

		return mod;
	}

	/*
	 * End new constant prop rules implementation.
	 * =================================================
	 */

	/**
	 * Clones this encoded mux to produce a copy with the same characteristics.
	 * 
	 * @return an EncodedMux
	 * @exception CloneNotSupportedException
	 *                if an error occurs
	 */
	@Override
	public Object clone() throws CloneNotSupportedException {
		EncodedMux clone = (EncodedMux) super.clone();
		// DONT USE getDataPorts because it's overridden to return the
		// 'inputPorts' list contained locally, which when shallow
		// cloned is simply a list of the original nodes ports.
		List<Port> dports = new ArrayList<Port>(clone.dataPorts);

		clone.selectPort = dports.remove(0);
		clone.inputPorts = dports;
		return clone;
	}

	/**
	 * Tests whether this is a passthrough operation.
	 * 
	 * @return true if the output of this component can be reduced to a constant
	 *         or the value of a single input, based upon the current
	 *         {@link Value} of each data {@link Port}
	 */
	@Override
	public boolean isPassThrough() {
		boolean constantSelect = false;
		if (getSelectPort().getValue() != null) {
			constantSelect = getSelectPort().getValue().isConstant();
		}

		return super.isPassThrough() || constantSelect;
	}

} // class Mux

