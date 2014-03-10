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

package org.xronos.openforge.lim;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

import org.xronos.openforge.util.naming.ID;
import org.xronos.openforge.util.naming.IDSourceInfo;


/**
 * A Bus represents the source side of a data connection between
 * {@link Component Component}s. One or more destination {@link Port Port}s may
 * be connected to the Bus; these represent the receivers of the Bus's data.
 * <P>
 * Each Bus is owned by a single {@link Component Component}. In addition to
 * Port connections, a Bus may have multiple logical dependent Ports, repesented
 * by {@link Dependency Dependencies}. It is from these that the final Port
 * connections are ultimately derived.
 * <P>
 * Each Bus has an underlying vector of {@link Bit Bits} which comprise it. The
 * number of bits in the vector is determined when the width of the Bus is
 * established via the method {@link Bus#setBits(int)}). Each of these
 * {@link Bit Bits} also designates the Bus as its owner; they can also be
 * referenced in any {@link Value}, but their number and identity within the Bus
 * can never be changed once they are created.
 * <P>
 * The Bus also has a {@link Value}. An initial {@link Value} is created along
 * with the {@link Bit} vector; this initial value simply contains the bits of
 * the vector. Other value may be set on the Bus containing Bits that are owne
 * by other Buses.
 * 
 * @version $Id: Bus.java 202 2006-07-06 18:40:37Z imiller $
 */
public class Bus extends ID {

	/** The owner of this bus */
	private Exit owner;

	/**
	 * The peer port, if any, that continues this bus across a module boundary.
	 */
	private Port peer = null;

	/** Collection of Ports; accessed directly by Port */
	Collection<Port> ports;// = new HashSet<Port>(3);

	/** Collection of Dependencys; accessed directly by Dependency */
	Collection<Dependency> logicalDependents;// = new HashSet<Dependency>(3);

	/** True if significant, false if ignorable */
	private boolean isUsed = false;

	/** type of bus - normal, sideband, etc */
	private Component.Type type = Component.NORMAL;

	private boolean isFloatBus = false;

	/*
	 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ Begin new
	 * constant prop implementation.
	 */

	/** The current Value of this Bus */
	private Value value = null;

	/**
	 * has the value been forced?
	 */
	private boolean valueForced = false;

	/**
	 * Sets the size in bits of this bus.
	 * 
	 * @param size
	 *            the number of bits that comprise this bus, > 0
	 * @param isSigned
	 *            true if this bus represents a signed value, false if unsigned
	 * @throws IllegalStateException
	 *             if this method has already been called on this bus
	 * @throws IllegalArgumentException
	 *             if 'size' is less than 1
	 */
	public void setSize(int size, boolean isSigned) {
		// is it allocated?
		if (value != null) {
			throw new IllegalStateException("Value already set");
		} else if (size < 1) {
			throw new IllegalArgumentException("invalid size: " + size);
		} else {
			// allocate ...
			value = new Value(this, size, isSigned);
		}
	}

	/**
	 * Get the original bit for this Bus at the designated position
	 * 
	 * @param position
	 *            a value of type 'int'
	 * @return a value of type 'Bit'
	 */
	public final Bit getLocalBit(int position) {
		return new Bit(this, position);
	}

	/**
	 * Gets the size of this bus.
	 * 
	 * @return the number of bits in this bus
	 */
	public int getSize() {
		return value.getSize();
	}

	/**
	 * Gets this bus's value.
	 * 
	 * @return the value object that was created with
	 *         {@link #setSize(int,boolean)}
	 */
	public Value getValue() {
		return value;
	}

	/**
	 * Updates this bus's {@link Value} from within its {@link Component} during
	 * forward constant propagation.
	 * <P>
	 * Firstly, if this bus does not yet have a value, then
	 * {@link #setSize(int,boolean)} is called using the size and sign-edness of
	 * the input value. Then the update occurs as described below.
	 * <P>
	 * For each {@link Bit} of the incoming value, this bus's value bit at the
	 * same position is updated as follows:
	 * <ol>
	 * <li>If either bit is {@link Bit#DONT_CARE}, no update occurs; otherwise:
	 * <li>If the incoming bit is a constant, it becomes the current bit;
	 * otherwise:
	 * <li>If the incoming bit is {@link Bit#CARE}, the current bit becomes this
	 * bus's bit from the same position; otherwise:
	 * <li>The incoming bit (another bus's bit) becomes the current bit.
	 * </ol>
	 * 
	 * @param value
	 *            the incoming value to be pushed onto this bus
	 * 
	 * @return true if this bus's value was modified, false otherwise
	 */
	public boolean pushValueForward(Value v) {
		if (valueForced) {
			throw new IllegalStateException("Can't push value after forcing");
		}

		boolean modified = false;

		/*
		 * Firstly, if this bus does not yet have a value, then then initialize
		 * it with a "generic" care value that has the same size and sign as the
		 * incoming value.
		 */
		if (value == null) {
			setSize(v.getSize(), v.isSigned());
		}

		assert (v.getSize() == getSize()) : ("size mismatch: " + v.getSize()
				+ " vs. " + getSize());
		assert (v.isSigned() == value.isSigned()) : "sign mismatch";

		/*
		 * Compare each incoming Bit with the current Bit at the same index.
		 */
		for (int i = 0; i < value.getSize(); i++) {
			boolean mod = false;

			/*
			 * Skip Bits that are already the same.
			 */
			if (!v.bitEquals(i, value, i)) {
				if (!v.isCare(i) || !value.isCare(i)) {
					/*
					 * If either bit is DONT_CARE, no update occurs...
					 */
				} else if (v.getConstant(i) < Value.NOT_CONSTANT) {
					/*
					 * Otherwise, if the incoming bit is a constant, it becomes
					 * the current bit...
					 */
					value.setBit(i, v, i);
					mod = true;
				} else if (v.isCare(i) && v.isGlobal(i)) {
					/*
					 * Otherwise, if the incoming bit is global CARE (no owner),
					 * the current bit is set to this bus's bit from the same
					 * position (if not already)...
					 */
					Bit localBit = new Bit(this, i);
					if (!value.bitEquals(i, localBit)) {
						value.setBit(i, localBit);
						mod = true;
					}
				} else {
					/*
					 * Otherwise, the incoming bit (another bus's bit) becomes
					 * the current bit.
					 */
					value.setBit(i, v, i);
					mod = true;
				}

				modified |= mod;
			}
		}

		return modified;
	}

	/**
	 * Updates this bus's {@link Value} with the values of its target
	 * {@link Port Ports} during reverse constant propagation by propagating
	 * don't care bits.
	 * <P>
	 * If this bus is connected, then the target ports are those connected to
	 * this bus. Otherwise, the target ports are those which have a dependency
	 * on this bus.
	 * <P>
	 * For each {@link Bit} of this bus's value, if all of the bits at the same
	 * position in the target values are {@link Bit#DONT_CARE}, then this bus's
	 * bit is set to {@link Bit#DONT_CARE}. Otherwise no modification to this
	 * bus's value is made.
	 * 
	 * @return true if this bus's value was modified, false otherwise
	 */
	public boolean pushValueBackward() {
		if (valueForced) {
			throw new IllegalStateException("Can't push value after forcing");
		}

		// hard thing here is to get the list of other ports to reflect.
		// is connected, use those.
		// if not, we need a list based on dependencies
		Collection<Port> plist;
		if (isConnected()) {
			plist = getPorts();
		} else {
			//
			// CRSS
			//
			// CHECKME - do we need to do logical and structural?
			//
			//
			// make sure we get no dups to process
			HashSet<Port> hs = new HashSet<Port>();

			// for all logicals
			for (Dependency d : getLogicalDependents()) {
				hs.add(d.getPort());
			}

			plist = hs;
		}

		if (plist.size() == 0) {
			return false;
		}

		// now plist has all the ports in it we need
		// boolean modified = false;

		final Value currentValue = value;
		if (currentValue == null) {
			return false;
		}

		final Value newValue = new Value(currentValue, currentValue.isSigned());

		// for each bit we have ...
		for (int i = 0; i < getSize(); i++) {
			// Bit currentBit = currentValue.getBit(i);
			// for each port, as long as we haven't found a care
			boolean foundDontCare = false;
			boolean foundCare = false;
			for (Port p : plist) {

				// first check if it has a value and size is ok
				if ((p.getValue() != null) && (i < p.getSize())) {
					if (p.getValue().getBit(i).isCare()) {
						foundCare = true;
					} else {
						foundDontCare = true;
					}
				}
			}

			// if we found dont cares but no cares ....
			if ((foundDontCare) && (!foundCare)) {
				// change the value
				newValue.setBit(i, Bit.DONT_CARE);
			}
		}

		if (value == null) {
			setSize(newValue.getSize(), newValue.isSigned());
		}

		boolean isModified = false;
		for (int i = 0; i < value.getSize(); i++) {
			if (!newValue.getBit(i).isCare() && value.getBit(i).isCare()) {
				value.setBit(i, Bit.DONT_CARE);
				isModified = true;
			}
		}
		return isModified;
	}

	/**
	 * Forces the current value of this bus to a given value. This method should
	 * only be used after all constant propagation has completed in order to
	 * shave don't-care bits from the upper end of the value. It is intended to
	 * be called directly before translation.
	 * 
	 * @param value
	 *            the value to be forced into this bus
	 * @throws NullPointerException
	 *             if the given value is null
	 * @throws IllegalStateException
	 *             if this bus does not currently have a value
	 */
	public void forceValue(Value v) throws IllegalStateException,
			NullPointerException {
		if (v == null) {
			throw new NullPointerException("null input value");
		}

		if (getValue() == null) {
			throw new IllegalStateException("no current value");
		}

		assert (v.getSize() <= getSize());
		valueForced = true;
		value = v;
	}

	/*
	 * End new constant prop implementation.
	 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	 */

	/**
	 * Constructs a new Bus, but defers the creation of the vector of
	 * {@link Bit Bits} that comprise this Bus.
	 * 
	 * @param owner
	 *            the exit to which this bus belongs
	 */
	public Bus(Exit owner) {
		this.owner = owner;
		this.ports = new HashSet<Port>(3);
		this.logicalDependents = new HashSet<Dependency>(3);
	}

	/**
	 * Gets the Exit to which this Bus belongs.
	 */
	public Exit getOwner() {
		return owner;
	}

	/**
	 * Gets the peer Port.
	 * 
	 * @return the peer port which continues this bus across a module boundary;
	 *         null if there is none
	 */
	public Port getPeer() {
		return peer;
	}

	/**
	 * Gets the logical dependents of this bus.
	 * 
	 * @return a collection of Dependency objects, one for each Port that
	 *         logically depends upon this bus; Dependencies which are not added
	 *         to a Port are not returned
	 */
	public Collection<Dependency> getLogicalDependents() {
		return logicalDependents;
	}

	/**
	 * Tests whether this bus is connected to at least one port or not.
	 */
	public boolean isConnected() {
		return !getPorts().isEmpty();
	}

	/**
	 * Gets the Ports that are connected to this Bus.
	 * 
	 * @return a collection of Ports
	 */
	public Collection<Port> getPorts() {
		return ports;
	}

	/**
	 * Tests whether this Bus is used.
	 * 
	 * @return true if used, false if ignorable
	 */
	public boolean isUsed() {
		return isUsed;
	}

	/**
	 * Sets whether this Bus is used.
	 * 
	 * @param isUsed
	 *            true if used, false if ignorable
	 */
	public void setUsed(boolean isUsed) {
		this.isUsed = isUsed;
	}

	/**
	 * Gets the cumulative latency at this bus. This value is relative to the
	 * start of the scheduling context (usually the current module).
	 */
	public Latency getLatency() {
		return getOwner().getLatency();
	}

	/**
	 * Disconnects all Ports that are physically connected to this bus.
	 */
	public void disconnect() {
		for (Port port : new ArrayList<Port>(getPorts())) {
			port.setBus(null);
		}
	}

	/**
	 * Removes this bus as a dependency from all logical dependents, and deletes
	 * the dependency which stored the relationship on the target entry.
	 */
	public void clearLogicalDependents() {
		for (Dependency logical : new ArrayList<Dependency>(
				getLogicalDependents())) {

			// clears the logical dependency but also removes the
			// dependency from the containing entry.
			logical.zap();
		}
	}

	/**
	 * Removes this bus as a dependency from all dependents, both structural and
	 * logical.
	 */
	public void clearDependents() {
		clearLogicalDependents();
	}

	/**
	 * Sets the peer Port.
	 * 
	 * @param peer
	 *            the peer port which continues this bus across a module
	 *            boundary; null if there is none
	 */
	public void setPeer(Port peer) {
		this.peer = peer;
	}

	/**
	 * Overrides IDNameAdaptor.getIDSourceInfo()
	 * 
	 * @return this bus owner id source info
	 */
	@Override
	public IDSourceInfo getIDSourceInfo() {
		return getOwner().getIDSourceInfo();
	}

	/**
	 * DOCUMENT ME!
	 * 
	 * @return DOCUMENT ME!
	 */
	@Override
	public String toString() {
		return ID.showLogical(this) + ":" + ID.glob(this);
	}

	/** set the type (used to denote sideband ports/buses) */
	public void tag(Component.Type type) {
		this.type = type;
	}

	/**
	 * DOCUMENT ME!
	 * 
	 * @return DOCUMENT ME!
	 */
	public Component.Type getTag() {
		return type;
	}

	/**
	 * Copies the attributes of a given Bus to this one during the cloning
	 * process. This includes the isUsed flag, the tag, and the {@link ID}. In
	 * addition, if the given Bus already has a vector of {@link Bit Bits}, them
	 * same number of {@link Bit Bits} will be created for this Bus.
	 * 
	 * @param bus
	 *            the bus from which to copy the attributes
	 */
	public void copyAttributes(Bus sourceBus) {
		setUsed(sourceBus.isUsed());
		tag(sourceBus.getTag());
		ID.copy(sourceBus, this);
		setFloat(sourceBus.isFloat());

		value = null;
		Value sourceValue = sourceBus.getValue();

		if (sourceValue != null)// !(sourceBus.bits.length == 0))
		{
			/*
			 * If there are Bits, there must be a Value as well.
			 */
			setSize(sourceValue.getSize(), sourceValue.isSigned());
			for (int i = 0; i < sourceBus.getSize(); i++) {
				/*
				 * Copy DONT_CARE bit and CONSTANT bit from source
				 */
				final Bit bit = sourceValue.getBit(i);
				if (bit.isConstant() || !bit.isCare()) {
					getValue().setBit(i, bit);
				}
			}
		}
	}

	/**
	 * DOCUMENT ME!
	 * 
	 * @return DOCUMENT ME!
	 */
	int getWidth() {
		if (value == null) {
			return 0;
		}

		return value.getSize();
	}

	/**
	 * Sets the state of the isFloat method.
	 */
	public void setFloat(boolean value) {
		isFloatBus = value;
	}

	/**
	 * Returns true if the implementation of this bus is floating point.
	 */
	public boolean isFloat() {
		return isFloatBus;
	}
}
