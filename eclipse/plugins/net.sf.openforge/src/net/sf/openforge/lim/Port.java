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
import java.util.HashSet;
import java.util.Set;

import net.sf.openforge.util.naming.ID;
import net.sf.openforge.util.naming.IDSourceInfo;

/**
 * A Port represents the receiving side of a data connection between
 * {@link Component Component}s. At most one source {@link Bus Bus}es may be
 * connected to a Port.
 * <P>
 * Each Port is owned by a single {@link Component Component}.
 * 
 * @author Stephen Edwards
 * @version $Id: Port.java 150 2006-06-28 18:43:12Z imiller $
 */
public class Port extends ID {

	/** The owner of this port */
	private Component owner;

	/**
	 * The peer bus of this port, if any, that continues it across a Module
	 * boundary
	 */
	private Bus peer = null;

	/** The bus physically connected to this port; null if not connected */
	private Bus bus = null;

	/** True if significant, false if ignorable */
	private boolean isUsed = false;

	/** type of bus - normal, sideband, etc */
	private Component.Type tag = Component.NORMAL;

	/*
	 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ Begin new
	 * constant prop implementation.
	 */

	/** The current Value of this Port */
	private Value value = null;

	/** has the value been forced? */
	private boolean valueForced = false;

	/**
	 * Sets the size in bits of this port.
	 * 
	 * @param size
	 *            the number of bits that comprise this bus, > 0
	 * @param isSigned
	 *            true if this port represents a signed value, false if unsigned
	 * @throws IllegalStateException
	 *             if this method has already been called on this port
	 * @throws IllegalArgumentException
	 *             if 'size' is less than 1
	 */
	public void setSize(int size, boolean isSigned)
			throws IllegalStateException, IllegalArgumentException {
		if (value != null) {
			throw new IllegalStateException("port already sized");
		}

		if (size < 1) {
			throw new IllegalArgumentException("invalid size: " + size);
		}

		value = new Value(size, isSigned);
	}

	/**
	 * Gets the size of this port.
	 * 
	 * @return the number of bits in this port's {@link Value value}
	 * @throws NullPointerException
	 *             if {@link #setSize(int,boolean)} has not yet been called
	 */
	public int getSize() throws NullPointerException {
		return value.getSize();
	}

	/**
	 * Gets this port's value.
	 * 
	 * @return the value object that was created with
	 *         {@link #setSize(int,boolean)}, or null if
	 *         {@link #setSize(int,boolean)} has not yet been called
	 */
	public Value getValue() {
		return value;
	}

	/**
	 * Updates this port's {@link Value} with its incoming values during forward
	 * constant propagation.
	 * <P>
	 * If this port is connected, then there is only a single incoming value,
	 * that of the connected {@link Bus}. This value us used as the incoming
	 * value.
	 * <P>
	 * Otherwise, if this port is not connected, then the incoming value must be
	 * determined by looking at the {@link Bus Buses} on which this port
	 * depends. A copy of each bus's value is made and any bus bits are replaced
	 * with {@link Bit#CARE} bits (because scheduling has not yet run, so the
	 * bus bits are not yet valid). If there is only one dependency value, that
	 * is used as the incoming value. Multiple values must instead be combined.
	 * This combining process is performed on a {@link Bit}-wise basis. For each
	 * bit position, the union is defined as follows for the set of bits at that
	 * position:
	 * <P>
	 * <ol>
	 * <li>If all of the bits are {@link Bit#DONT_CARE}, then the union of those
	 * bits is {@link Bit#DONT_CARE}; otherwise, discard the
	 * {@link Bit#DONT_CARE} bits and compute the union on those that remain
	 * <li>If all the remaining bits are the same bit object, then the union is
	 * that bit; otherwise, the union is {@link Bit#CARE}
	 * </ol>
	 * <P>
	 * Whether derived from a bus connection or dependencies, the single
	 * incoming value is then made the current value of this port by copying its
	 * {@link Bit Bits} to this port's {@link Value}, BUT ONLY IF THE CURRENT
	 * {@link Bit} IS NOT {@link Bit#DONT_CARE}. If this port does not yet have
	 * a value, one is first created by calling {@link #setSize(int,boolean}
	 * with the incoming value's size and sign-edness.
	 * 
	 * @return true if this port's value has changed as a result of this method,
	 *         false if it remains unchanged
	 */
	public boolean pushValueForward() {
		if (valueForced) {
			throw new IllegalStateException("Can't push value after forcing");
		}

		boolean isInputComplete = true;
		// boolean modified = false;
		Value derived;

		// two cases, connected or not
		if (isConnected()) {
			// easy case ...
			derived = getBus().getValue();
		} else {
			ArrayList<Value> vlist = new ArrayList<Value>(11);

			// cribbed from getdriven value below ...
			for (Entry entry : getOwner().getEntries()) {
				for (Dependency dep : entry.getDependencies(this)) {
					Value v = dep.getLogicalBus().getValue();

					if (v != null) {
						/*
						 * We have to allow for inputs that might not have
						 * values yet, i.e., those providing feedback signals.
						 */
						vlist.add(v);
					} else {
						isInputComplete = false;
					}
				}
			}

			// if we found none, we are done. TBD CHECKME
			if (vlist.size() == 0) {
				return false;
			}

			// now to combine. use the size/signage of the first one
			Value firstVal = vlist.get(0);
			derived = new Value(firstVal.getSize(), firstVal.isSigned());

			// for each bit
			Set<Bit> testBucket = new HashSet<Bit>();
			for (int i = 0; i < derived.getSize(); i++) {
				// for each one in the list, throw it in the bucket
				for (Value nextVal : vlist) {
					testBucket.add(nextVal.getBit(i));
				}

				// try an remove a dont care; if empty it had only dc's
				if ((testBucket.remove(Bit.DONT_CARE))
						&& (testBucket.size() == 0)) {
					// only dont cares, make it dont care
					derived.setBit(i, Bit.DONT_CARE);
				}

				// here either there were no dc's, or something else
				else {
					// now, do we have all the same bits?
					if (testBucket.size() == 1) {
						// could be care, const, or owned bit, but it is unique!
						derived.setBit(i, testBucket.iterator().next());
					} else {
						// multiples, you make this a care
						derived.setBit(i, Bit.CARE);
					}
				}

				// clear the bucket
				testBucket.clear();
			}
		}

		boolean isModified = false;

		// The derived value may be null in the case where we have a
		// collection of uninitialized components that are not visited
		// in dataflow order (or where dataflow order cant be
		// determined). This happens in our design module. So, to
		// handle the case we will terminate the propagate here and
		// allow the next iteration to finish the setting. This only
		// works if everything at the module level WILL resolve.
		if (derived == null) {
			return false;
		}
		if (getValue() == null) {
			/*
			 * Allocate a value even if not all the inputs are available yet. At
			 * least we know the size.
			 */
			setSize(derived.getSize(), derived.isSigned());
			isModified = true;
		}
		if (derived.getSize() != getValue().getSize())
			throw new SizeMismatchException("derived: " + derived.getSize()
					+ " current: " + getValue().getSize());
		// assert derived.getSize()==getValue().getSize():
		// "derived: "+derived.getSize()+" current: "+getValue().getSize();

		/*
		 * If not all inputs have values yet, as can be the case when there is
		 * feedback, exit now.
		 */
		if (!isInputComplete) {
			return isModified;
		}

		/*
		 * Push the incoming Bits into the current Value.
		 */
		for (int i = 0; i < getSize(); i++) {
			Bit incomingBit = derived.getBit(i);
			Bit currentBit = getValue().getBit(i);

			/*
			 * If the current Bit is not a don't-care, but it does differ from
			 * the incoming Bit, then replace the current Bit with the incoming
			 * Bit.
			 */
			if (!getValue().bitEquals(i, derived, i)
					&& currentBit != Bit.DONT_CARE && !currentBit.isConstant()) {
				isModified = true;
				getValue().setBit(i, incomingBit);
			}
		}

		return isModified;
	}

	/**
	 * Updates this port's {@link Value} from within its {@link Component}
	 * during reverse constant propagation. Only don't-care bits are propagated
	 * backwards. Therefore, for each {@link Bit} of a given update value, if
	 * the bit is {@link Bit#DONT_CARE}, then the bit at the same position of
	 * this port's value is also set to {@link Bit#DONT_CARE}. Otherwise no
	 * modifications are made to this port's value.
	 * 
	 * @param value
	 *            the value whose don't-care bits are copied to this port's
	 *            value
	 * 
	 * @return true if this port's value was modified, false otherwise
	 * @throws IllegalStateException
	 *             if {@link #forceValue(Value)} has been previously called on
	 *             this port
	 * @throws NullPointerException
	 *             if <code>v</code> is null
	 */
	public boolean pushValueBackward(Value v) {
		if (valueForced) {
			throw new IllegalStateException("Can't push value after forcing");
		}

		final Value currentValue = getValue();

		boolean modified = false;

		// verify we have not run out of bits in the local size
		for (int i = 0; i < v.getSize(); i++) {
			// get incoming bit
			Bit incomingBit = v.getBit(i);

			// if not a care bit, set to don't care
			if (!incomingBit.isCare() && currentValue.getBit(i).isCare()) {
				currentValue.setBit(i, Bit.DONT_CARE);
				modified = true;
			}
		}

		return modified;
	}

	/**
	 * Forces the current value of this port to a given value. This method
	 * should only be used after all constant propagation has completed in order
	 * to shave don't-care bits from the upper end of the value. It is intended
	 * to be called directly before translation.
	 * 
	 * @param value
	 *            the value to be forced into this port
	 * @throws NullPointerException
	 *             if the given value is null
	 * @throws IllegalStateException
	 *             if this port does not currently have a value
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
	 * Constructs a new Port.
	 * 
	 * @param owner
	 *            the component to which this port belongs
	 */
	public Port(Component owner) {
		this.owner = owner;
	}

	/**
	 * Gets the peer Bus, if any, that connects this Port across a module
	 * boundary.
	 * 
	 * @return the peer bus, or null if there is none
	 */
	public Bus getPeer() {
		return peer;
	}

	/**
	 * Gets the Component to which this Port belongs.
	 */
	public Component getOwner() {
		return owner;
	}

	/**
	 * Tests whether this port is connected to a bus or not.
	 */
	public boolean isConnected() {
		return getBus() != null;
	}

	/**
	 * Gets the Bus to which this Port is connected.
	 * 
	 * @return the connected Bus, or null if not connected
	 */
	public Bus getBus() {
		return bus;
	}

	/**
	 * Tests whether this Port is used.
	 * 
	 * @return true if used, false if ignorable
	 */
	public boolean isUsed() {
		return isUsed;
	}

	/**
	 * Sets whether this Port is used.
	 * 
	 * @param isUsed
	 *            true if used, false if ignorable
	 */
	public void setUsed(boolean isUsed) {
		this.isUsed = isUsed;
	}

	/**
	 * Disconnects the current physical bus connection.
	 */
	public void disconnect() {
		setBus(null);
	}

	/**
	 * Sets the Bus to which this Port is connected and reconciles the Value on
	 * the Bus with the Value on the Port. If this port has a peer, set the peer
	 * to contain the bus's information.
	 * 
	 * @param bus
	 *            the Bus to connect, or null if not connected
	 */
	public void setBus(Bus bus) {
		if (this.bus != null) {
			this.bus.ports.remove(this);
		}

		this.bus = bus;
		if (bus != null) {
			bus.ports.add(this);
		}
	}

	/**
	 * Copies the primitive attributes of a given Port to this one. These
	 * attributes include {@link Port#isUsed()}, {@link Port#getTag()}, and
	 * {@link Port#getValue()}. Used during cloning.
	 */
	public void copyAttributes(Port port) {
		setUsed(port.isUsed());
		tag(port.getTag());
		ID.copy(port, this);

		/*
		 * Copy the Value if it exists and is constant.
		 */
		final Value value = port.getValue();
		if (value != null) {
			this.value = new Value(value.getSize(), value.isSigned());
		}
	}

	/**
	 * Sets the peer Bus, if any, that connects this Port across a module
	 * boundary.
	 * 
	 * @param peer
	 *            the peer bus, or null if there is none
	 */
	void setPeer(Bus peer) {
		this.peer = peer;
	}

	/**
	 * Overrides IDNameAdaptor.getIDSourceInfo()
	 * <P>
	 * ABK: why can't ports have there very own id source info? It's
	 * particularly useful when they are attached to the body of a procedure and
	 * represent the input args.
	 * 
	 * @return this port owner id source info
	 */
	@Override
	public IDSourceInfo getIDSourceInfo() {
		return super.getIDSourceInfo();
		// return getOwner().getIDSourceInfo();
	}

	/**
	 * DOCUMENT ME!
	 * 
	 * @return DOCUMENT ME!
	 */
	@Override
	public String toString() {
		return ID.showLogical(this);
	}

	/** set the type (used to denote sideband ports/buses) */
	public void tag(Component.Type tag) {
		this.tag = tag;
	}

	/**
	 * DOCUMENT ME!
	 * 
	 * @return DOCUMENT ME!
	 */
	public Component.Type getTag() {
		return tag;
	}

	@SuppressWarnings("serial")
	static class SizeMismatchException extends RuntimeException {
		public SizeMismatchException(String msg) {
			super(msg);
		}
	}

}
