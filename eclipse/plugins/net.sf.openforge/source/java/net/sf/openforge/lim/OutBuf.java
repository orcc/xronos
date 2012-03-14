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

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import net.sf.openforge.util.SizedInteger;

/**
 * An OutBuf is used to bring a structural flow to the outside of a
 * {@link Module} from its inside. An OutBuf is created automatically for each
 * {@link Exit} of the {@link Module}, and adding a data {@link Bus} to the
 * {@link Exit} also adds a corresponding {@link Port} to the OutBuf. The OutBuf
 * itself has no {@link Exit} and no {@link Bus Buses}. Logically, the
 * {@link Bus Buses} of the {@link Exit} are the continuation of the OutBuf's
 * input {@link Port Ports}.
 * 
 * @version $Id: OutBuf.java 2 2005-06-09 20:00:48Z imiller $
 */
public class OutBuf extends Component implements Emulatable {
	private static final String rcs_id = "RCS_REVISION: $Rev: 2 $";

	/** The estimated 'gate depth' of the default IOB's */
	public static final int IOB_DEFAULT = 74;

	/**
	 * The gate depth of this outbuf. Always 0 except at the periphery of the
	 * task to help register IOB's of a task.
	 */
	private int gateDepth = 0;

	/** The peer Exit of this OutBuf */
	private Exit exit;

	/** True if the peer Exit produces a done */
	private boolean consumesGo = false;

	/**
	 * Gets the peer {@link Exit}.
	 * 
	 * @return the {@link Exit} whose {@link Bus Buses} represent the
	 *         continutation of this OutBuf's input {@link Port Ports}.
	 */
	// public Exit getExit ()
	public Exit getPeer() {
		return exit;
	}

	/**
	 * Constructs a new OutBuf. It is package private since only {@link Exit} is
	 * meant to call it.
	 * 
	 * @param module
	 *            the owner of this OutBuf
	 * @param exit
	 *            the peer {@link Exit} of this OutBuf
	 */
	OutBuf(Module owner, Exit exit) {
		super(exit.getDataBuses().size());
		this.exit = exit;
	}

	public Collection getExits() {
		return Collections.EMPTY_LIST;
	}

	public void accept(Visitor v) {
		v.visit(this);
	}

	public boolean hasWait() {
		return false;
	}

	/**
	 * Tests whether this component requires a connection to its <em>go</em>
	 * {@link Port} in order to commence processing.
	 */
	public boolean consumesGo() {
		return consumesGo;
	}

	/**
	 * Called by the containing {@link Module} to agree with its value for
	 * {@link Component#producesDone()}.
	 */
	void setConsumesGo(boolean consumesGo) {
		this.consumesGo = consumesGo;
	}

	/**
	 * Sets the gate depth for this OutBuf, called by the Pipeliner to set a
	 * depth on the inputs of a task to account for IOBs
	 */
	public void setGateDepth(int value) {
		this.gateDepth = value;
	}

	/**
	 * Overrides method in Component to provide the OutBuf depth when it exists
	 * at the boundry of a task to account for IOBs
	 */
	public int getGateDepth() {
		return this.gateDepth;
	}

	/**
	 * Performes a high level numerical emulation of this component. For each
	 * {@link Port} for which a value is provided, that value is returned as the
	 * value of the corresponding peer {@link Bus}.
	 * 
	 * @param portValues
	 *            a map of {@link Port} to {@link SizedInteger} input value
	 * @return a map of owner {@link Bus} to {@link SizedInteger} result value
	 */
	public Map emulate(Map portValues) {
		final Map outputValues = new HashMap();
		for (Iterator iter = getDataPorts().iterator(); iter.hasNext();) {
			final Port port = (Port) iter.next();
			final SizedInteger portValue = (SizedInteger) portValues.get(port);
			if (portValue != null) {
				outputValues.put(port.getPeer(), portValue);
			}
		}
		return outputValues;
	}

	/*
	 * =================================================== Begin new constant
	 * prop rules implementation.
	 */

	/**
	 * Pushes Value information across the Module boundry and returns true if
	 * the pushed Value contains any new information.
	 * 
	 * @return true if the Value pushed across the module boundry contained any
	 *         new information.
	 */
	public boolean pushValuesForward() {
		boolean isModified = false;
		for (Iterator iter = getPorts().iterator(); iter.hasNext();) {

			final Port port = (Port) iter.next();
			if ((port == getClockPort()) || (port == getResetPort())
					|| ((port == getGoPort()) && (port.getValue() == null))) {
				/*
				 * Do nothing for clock or reset, since they are not passed out
				 * of the OutBuf's module. Also do nothing if the go is not
				 * being fed with a value.
				 */
			} else {
				final Value pushedValue = port.getValue();

				/*
				 * If the Bus doesn't have a Value yet, create one.
				 */
				Value peerValue = port.getPeer().getValue();
				if (peerValue == null) {
					port.getPeer().setSize(pushedValue.getSize(),
							pushedValue.isSigned());
					peerValue = port.getPeer().getValue();
					isModified = true;
				}

				/*
				 * Rather than depending on the behavior of
				 * Bus.pushValueForward(Value), we need to be more precise here,
				 * since an OutBuf is part Module and part Component. Compare
				 * each pair of Bits.
				 */
				for (int i = 0; i < pushedValue.getSize(); i++) {
					final Bit pushedBit = pushedValue.getBit(i);
					final Bit peerBit = peerValue.getBit(i);

					if (!pushedValue.bitEquals(i, peerValue, i)
							&& peerBit.isCare()) {
						if (getOwner().isOpaque()) {
							/*
							 * If this OutBuf lives in an opaque Module, then we
							 * have to avoid pushing through internal non-global
							 * Bits. Since we don't push don't-cares, that only
							 * leaves constants as viable propagatees.
							 */
							if (pushedBit.isConstant()) {
								peerValue.setBit(i, pushedBit);
								isModified = true;
							}
						} else if (pushedBit.isConstant()
								|| !pushedBit.isGlobal()) {
							/*
							 * Otherwise, this OutBuf lives in a transparent
							 * Module, and we can push through both constant and
							 * internal Bus Bits.
							 */
							peerValue.setBit(i, pushedBit);
							isModified = true;
						}
					}
				}

				if (pushedValue.getCompactedSize() != peerValue
						.getCompactedSize()) {
					final Bit msb = peerValue.getBit(pushedValue
							.getCompactedSize() - 1);
					for (int i = pushedValue.getCompactedSize(); i < peerValue
							.getSize(); i++) {
						peerValue.setBit(i, msb);
					}
				}
			}
		}
		return isModified;
	}

	/**
	 * Determines the consumed {@link Value} of each outbuf Port's peer Bus and
	 * updates the Port's value with that information. Returns true if any new
	 * information was pushed across the module boundry.
	 */
	public boolean pushValuesBackward() {
		boolean mod = false;
		for (Iterator iter = getPorts().iterator(); iter.hasNext();) {
			final Port port = (Port) iter.next();
			if (port.getPeer() != null) {
				final Value pushedValue = port.getPeer().getValue();
				if (pushedValue != null) {
					mod |= port.pushValueBackward(pushedValue);
				}
			}
		}

		return mod;
	}

	/*
	 * End new constant prop rules implementation.
	 * =================================================
	 */

	/**
	 * Cloning of OutBufs is not allowed. Use
	 * {@link Component#makeExit(int,Exit.Type,String)} instead.
	 * 
	 * @throws CloneNotSupportedException
	 *             always
	 */
	public Object clone() throws CloneNotSupportedException {
		throw new CloneNotSupportedException("attempt to clone OutBuf");
	}

}
