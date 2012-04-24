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

package net.sf.openforge.lim.memory;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import net.sf.openforge.lim.Visitor;
import net.sf.openforge.lim.op.Constant;

/**
 * LocationConstant is a {@link MemoryConstant} whose value is deferred (
 * {@link #isLocked} is false) which is based on accessing a particular
 * {@link Location}. The actual value of this constant will be set once the
 * memory map is fixed and the {@link #lock} method is called.
 * 
 * <p>
 * Created: Fri Feb 28 15:49:00 2003
 * 
 * @author imiller, last modified by $Author: imiller $
 * @version $Id: LocationConstant.java 568 2008-03-31 17:23:31Z imiller $
 */
public class LocationConstant extends MemoryConstant implements
		LocationValueSource {

	/** The location that this constant is based upon. */
	private Location location;

	/**
	 * The state of this constant, when set to true the ByteRep array will be
	 * populated with fixed numerical values corresponding to the address of the
	 * location referenced.
	 */
	private boolean isLocked = false;

	/**
	 * The addressable unit representation of this constant, whose endianness
	 * corresponds to the endianness of the compilation. Initially this bundle
	 * is populated with AURep objects whose value is 0 and isLocked is false.
	 * After this constant is locked this bundle is repopulated with locked and
	 * numerically correct values.
	 */
	private AURepBundle rep;

	/*
	 * public LocationConstant (Location loc, int width) { this(loc, width,
	 * loc.getAbsoluteBase().getLogicalMemory().getAddressStridePolicy()); }
	 */
	/**
	 * Creates a new LocationConstant which represents the numerical address of
	 * the given Location in memory and has the specified width in bits.
	 * 
	 * @param loc
	 *            the target Location.
	 * @param width
	 *            the int bitwidth.
	 */
	public LocationConstant(Location loc, int width, AddressStridePolicy policy) {
		super(width, false); // unsigned. Addresses are always unsigned
		setTarget(loc);
		// int repLength = (int)Math.ceil(((double)width) / 8.0);
		int repLength = (int) Math.ceil(((double) width) / policy.getStride());
		assert repLength > 0;
		AddressableUnit[] aurep = new AddressableUnit[repLength];
		for (int i = 0; i < repLength; i++) {
			// Set each byte to 'indeterminate' for now.
			aurep[i] = new AddressableUnit(0, false);
		}
		rep = new AURepBundle(aurep, policy.getStride());

		pushValuesForward();
	}

	/**
	 * Returns a single element, non modifiable list containing this constant as
	 * its only constituent is itself.
	 * 
	 * @return a List containing only this constant
	 */
	@Override
	public List getConstituents() {
		return Collections.unmodifiableList(Collections.singletonList(this));
	}

	/**
	 * Returns a single element non modifiable set containing only this object.
	 * 
	 * @return a singleton Set containing this object.
	 */
	@Override
	public Set getContents() {
		return Collections.unmodifiableSet(Collections.singleton(this));
	}

	/**
	 * Returns a bundle of AURep objects which define the address of the
	 * location which this constant represents. The values are marked
	 * indeterminate until this constant is locked. The value ordering returned
	 * depends on the endianness of the compilation.
	 */
	@Override
	public AURepBundle getRepBundle() {
		return rep;
	}

	/**
	 * Retrieves the {@link Location} that this constant is based upon.
	 */
	@Override
	public Location getTarget() {
		return location;
	}

	/**
	 * Modifies the location to which this constant points and removes this
	 * LocationConstant from the old logical memory and adds it to the new
	 * logical memory.
	 * 
	 * @param newLoc
	 *            a non-null 'Location'
	 * @throws IllegalArgumentException
	 *             if newLoc is null
	 * @throws UnsupportedOperationException
	 *             if this constant is locked.
	 */
	@Override
	public void setTarget(Location newLoc) {
		if (newLoc == null)
			throw new IllegalArgumentException(
					"Cannot change target location to null");
		if (isLocked())
			throw new UnsupportedOperationException(
					"Cannot change target location of a locked constant");

		removeFromMemory();
		location = newLoc;
		newLoc.getLogicalMemory().addLocationConstant(this);
	}

	/**
	 * Derives, if necessary, the numeric value represented by this constant.
	 */
	@Override
	public void lock() {
		isLocked = true;
		final Location location = getTarget();

		final long addr = location.getLogicalMemory().getAddress(location);

		// The address is in 'little endian' format, so if this is a
		// big endian compilation, byte swap.
		AddressableUnit[] fixedRep = new AddressableUnit[rep.getLength()];
		int bitsPerUnit = rep.getBitsPerUnit();
		long mask = 0;
		for (int i = 0; i < bitsPerUnit; i++)
			mask = (mask << 1) | 1L;
		// first, populate in little endian order
		for (int i = 0; i < fixedRep.length; i++) {
			// fixedRep[i] = new AURep((byte)((addr >>> (8 * i)) & 0xFF));
			fixedRep[i] = new AddressableUnit(
					(int) ((addr >>> (bitsPerUnit * i)) & mask), true);
		}
		if (isBigEndian()) {
			fixedRep = swapEndian(fixedRep);
		}

		rep = new AURepBundle(fixedRep, rep.getBitsPerUnit());

		pushValuesForward();
	}

	/**
	 * Returns true if this symbolic constant has been resolved to a true
	 * constant.
	 */
	@Override
	public boolean isLocked() {
		return isLocked;
	}

	/**
	 * Remove the underlying {@link LocationConstant} as a reference of the
	 * targetted memory.
	 */
	public void removeFromMemory() {
		if (getTarget() != null) {
			getTarget().getLogicalMemory().removeLocationConstant(this);
		}
	}

	/**
	 * returns true
	 */
	@Override
	public boolean isPointerValue() {
		return true;
	}

	/**
	 * Tests whether the given constant has the same numerical value, or will
	 * have the same value if it is a deferred constant. Here we return false is
	 * it isn't locked, otherwise call Constant's isSameValue()
	 * 
	 * <p>
	 * requires: none
	 * <p>
	 * modifies: none
	 * <p>
	 * effects: returns true if the constant will ultimately resolve to the same
	 * numerical value as this Constant.
	 * 
	 * @param constant
	 *            a Constant, may be null
	 * @return true if the given constant is not null and has the same numerical
	 *         value.
	 */
	@Override
	public boolean isSameValue(Constant constant) {
		// if we are locked, abdicate responsibility up the line
		if (isLocked()) {
			return super.isSameValue(constant);
		}

		// if our to-be-compared-to constant is not a pointer, fail
		if (!constant.isPointerValue()) {
			return false;
		}

		// compare-to-be is locked? we are not
		if (constant.isLocked()) {
			return false;
		}

		// get starting locations
		final Location thisLoc = getTarget();
		final Location thatLoc = ((LocationConstant) constant).getTarget();

		if (thisLoc.getAbsoluteBase() != thatLoc.getAbsoluteBase()) {
			return false;
		}

		// get this min/max/size
		final int thisAbsMinDelta = thisLoc.getAbsoluteMinDelta();
		final int thisAbsMaxDelta = thisLoc.getAbsoluteMaxDelta();
		final int thisSize = getValueBus().getSize();

		// get that min/max/size
		final int thatAbsMinDelta = thatLoc.getAbsoluteMinDelta();
		final int thatAbsMaxDelta = thatLoc.getAbsoluteMaxDelta();
		final int thatSize = constant.getValueBus().getSize();

		// all must match
		return ((thisAbsMinDelta == thatAbsMinDelta)
				&& (thisAbsMaxDelta == thatAbsMaxDelta) && (thisSize == thatSize));

	}

	/**
	 * Accept method for the Visitor interface
	 */
	@Override
	public void accept(Visitor visitor) {
		visitor.visit(this);
	}

	@Override
	public Object clone() throws CloneNotSupportedException {
		LocationConstant clone = (LocationConstant) super.clone();
		if (clone.getTarget() != null) {
			LogicalMemory mem = clone.getTarget().getLogicalMemory();
			mem.addLocationConstant(clone);
		}
		return clone;
	}

}// LocationConstant
