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
package net.sf.openforge.lim;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.sf.openforge.report.FPGAResource;

/**
 * A Mux accepts paired GO/Data signals, using each GO to select its related
 * Data to be provided on the Result Bus.
 * 
 * Created: April 25, 2002
 * 
 * @author <a href="mailto:andreas.kollegger@xilinx.com">Andreas Kollegger</a>
 * @version $Id: Mux.java 2 2005-06-09 20:00:48Z imiller $
 */
public class Mux extends Primitive {

	/** List of Go signal ports */
	List<Port> goPorts = Collections.emptyList();

	/** Map of Go-Data pairs */
	Map<Port, Port> muxMap = new LinkedHashMap<Port, Port>();

	/**
	 * Constructor for the Mux object
	 * 
	 * @param goCount
	 *            The number of mux entry pairs
	 */
	public Mux(int pairs) {
		super(0);

		if (pairs < 0) {
			throw new IllegalArgumentException("negative pairs");
		}

		if (pairs > 0) {
			for (int i = 0; i < pairs; i++) {
				makeMuxEntry();
			}
		}
	} // Mux()

	/**
	 * Gets the gate depth of this component. This is the maximum number of
	 * gates that any input signal must traverse before reaching an {@link Exit}
	 * .
	 * 
	 * @return a non-negative integer
	 */
	public int getGateDepth() {
		final int maxCareInputs = getMaxCareInputs();
		return (maxCareInputs < 2) ? 0 : (log2(maxCareInputs) + 1);
	}

	/**
	 * Gets the FPGA hardware resource usage of this component.
	 * 
	 * @return a FPGAResource objec
	 */
	public FPGAResource getHardwareResourceUsage() {
		int terms = 2 * goPorts.size();
		int groupedCount = 0;

		while (terms > 1) {
			groupedCount += terms >> 2;
			terms = (terms >> 2) + (terms % 4);
			if (terms < 4) {
				groupedCount += 1;
				break;
			}
		}

		final int maxCareInputs = getMaxCareInputs();
		final int lutCount = maxCareInputs * groupedCount;

		FPGAResource hwResource = new FPGAResource();
		hwResource.addLUT(lutCount);

		return hwResource;
	}

	/**
	 * Returns the collection of GO ports (which are stored as data ports).
	 * 
	 * @return The GoPorts value
	 */
	public List<Port> getGoPorts() {
		return goPorts;
	}

	/**
	 * Gets the Data Port which is paird with the given Go port.
	 */
	public Port getDataPort(Port go) {
		return (Port) muxMap.get(go);
	}

	public boolean removeDataPort(Port port) {
		assert false : "remove data port not supported for " + this;
		return false;
	}

	/**
	 * Gets a Set view of all the mux entries, where each element is a Map.Entry
	 * pairing of the Go/Data. The Set reflects the same ordering as the Go and
	 * Data Lists.
	 */
	public Set<java.util.Map.Entry<Port, Port>> getMuxEntries() {
		return muxMap.entrySet();
	}

	/**
	 * Makes a new mux entry. The Go/Data pair are stored in a Map with the Go
	 * as the key and the Data as the value.
	 * 
	 * @return the Go port
	 */
	public Port makeMuxEntry() {
		Port goPort = makeDataPort();
		Port dataPort = makeDataPort();

		if (this.goPorts == Collections.EMPTY_LIST) {
			this.goPorts = new LinkedList<Port>();
		}
		this.goPorts.add(goPort);
		muxMap.put(goPort, dataPort);
		return goPort;
	}

	public List<Port> getPorts() {
		List<Port> ports = new ArrayList<Port>();
		ports.add(getClockPort());
		ports.add(getResetPort());
		ports.add(getGoPort());
		ports.addAll(getDataPorts());
		return ports;
	}

	/**
	 * An Mux has no wait.
	 * 
	 * @return false
	 */
	public boolean hasWait() {
		return false;
	}

	public void accept(Visitor v) {
		v.visit(this);
	}

	/**
	 * Creates a copy of this Mux object with the same number of inputs/outputs.
	 * All ports and buses have the same attributes as the original Mux.
	 * 
	 * @return a Mux.
	 * @exception CloneNotSupportedException
	 *                if an error occurs
	 */
	public Object clone() throws CloneNotSupportedException {
		Mux clone = (Mux) super.clone();

		clone.goPorts = new LinkedList<Port>();
		clone.muxMap = new LinkedHashMap<Port, Port>();

		for (Iterator<Port> iter = clone.getDataPorts().iterator(); iter
				.hasNext();) {
			Port goPort = (Port) iter.next();
			Port dataPort = (Port) iter.next();
			clone.goPorts.add(goPort);
			clone.muxMap.put(goPort, dataPort);
		}

		return clone;
	}

	/*
	 * =================================================== Begin new constant
	 * prop rules implementation.
	 */

	/**
	 * Result size is max data port width. Bit states are dont care if ALL data
	 * ports are dont care, constant if all data ports are same constant and
	 * pass through if all data ports are same pass through bit. Also ensures
	 * that the result bus has had it's 'bits' set via the {@link Bus#setBits}
	 * method since Muxes get added during scheduling.
	 */
	public boolean pushValuesForward() {
		boolean mod = false;

		Value newValue = createForwardPushedValue();

		for (int i = 0; i < newValue.getSize(); i++) {
			Value samePositionBits = new Value(getGoPorts().size(), false);

			int index = 0;

			for (Iterator<Port> goPortIter = getGoPorts().iterator(); goPortIter
					.hasNext();) {
				Port goPort = (Port) goPortIter.next();
				Port dataPort = getDataPort(goPort);
				Value drivingValue = dataPort.getValue();
				samePositionBits.setBit(index++,
						(i < drivingValue.getSize() ? drivingValue.getBit(i)
								: Bit.ZERO));
			}

			Bit positionBit = Bit.CARE;
			for (int j = 1; j < samePositionBits.getSize(); j++) {
				if (!samePositionBits.bitEquals(0, samePositionBits, j)) {
					positionBit = Bit.CARE;
					break;
				} else {
					// if both are same constant, is that constant, or if
					// both are the same pass through.
					positionBit = samePositionBits.getBit(0);
				}
			}
			newValue.setBit(i, positionBit);
		}

		// update all bits above the carry out bit to be signed
		// extended of carry out bit
		if (getResultBus().getValue() != null) {
			int compactedSize = 1;
			for (Iterator<Port> goPortIter = getGoPorts().iterator(); goPortIter
					.hasNext();) {
				Port goPort = (Port) goPortIter.next();
				Port dataPort = getDataPort(goPort);
				compactedSize = Math.min(newValue.getSize(), Math.max(
						compactedSize, dataPort.getValue().getCompactedSize()));
			}

			Bit carryoutBit = getResultBus().getValue().getBit(
					compactedSize - 1);
			for (int i = compactedSize; i < newValue.getSize(); i++) {
				if (newValue.getBit(i) != Bit.DONT_CARE)
					newValue.setBit(i, carryoutBit);
			}
		}

		mod |= getResultBus().pushValueForward(newValue);

		return mod;
	}

	/**
	 * Any constant or dont care bits on the result propagate backwards to be
	 * dont care bits on all data inputs.
	 */
	public boolean pushValuesBackward() {
		boolean mod = false;

		Value resultBusValue = getResultBus().getValue();

		for (Port goPort : getGoPorts()) {
			Port dataPort = getDataPort(goPort);
			Value dataPortValue = dataPort.getValue();
			Value newValue = new Value(dataPortValue.getSize(),
					dataPortValue.isSigned());

			for (int i = 0; i < dataPortValue.getSize(); i++) {
				if (!resultBusValue.getBit(i).isCare()) {
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
	 * Creates a new value to be pushed forward during partial const
	 * propagation.
	 * 
	 * @return a value to be pushed forward.
	 */
	private Value createForwardPushedValue() {
		int maxSize = 0;
		boolean isSigned = true;

		for (Port goPort : getGoPorts()) {
			Port dataPort = getDataPort(goPort);
			maxSize = Math.max(maxSize, dataPort.getValue().getSize());
			isSigned = isSigned && dataPort.getValue().isSigned();
		}

		return new Value(maxSize, isSigned);
	}

	/**
	 * Gets the maximum number of significant inputs at any bit position.
	 * 
	 * @return the maximum number of inputs at any bit position which have
	 *         either a non-constant select or a CARE data input
	 */
	protected int getMaxCareInputs() {
		int maxCareInputs = 0;
		final int maxLength = maxPortSize();
		for (int bitPosition = 0; bitPosition < maxLength; bitPosition++) {
			maxCareInputs = Math.max(maxCareInputs, getCareInputs(bitPosition));
		}
		return maxCareInputs;
	}

	/**
	 * Gets the number of significant inputs at any bit position.
	 * 
	 * @return the number of inputs at the bit position which have either a
	 *         non-constant select or a CARE data input
	 */
	protected int getCareInputs(int bitPosition) {
		int careBits = 0;
		int constBits = 0;
		for (Port goPort : getGoPorts()) {
			final Port dataPort = getDataPort(goPort);

			if (dataPort.getValue().getSize() > bitPosition) {
				final Bit bit = dataPort.getValue().getBit(bitPosition);
				final Value goValue = goPort.getValue();

				if (goValue.isConstant()) {
					if (goValue.getValueMask() == 0L) {
						continue;
					} else {
						// if (bit == Bit.CARE)
						if (bit.isCare() && !bit.isConstant()) {
							careBits++;
						} else if ((bit == Bit.ONE) || (bit == Bit.ZERO)) {
							constBits++;
						}
					}
				} else {
					careBits++;
				}
			}
		}

		return (careBits == 0) ? 0 : (careBits + (constBits > 0 ? 1 : 0));
	}

} // class Mux

