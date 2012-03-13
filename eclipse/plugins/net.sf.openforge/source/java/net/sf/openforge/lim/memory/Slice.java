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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.sf.openforge.lim.op.Constant;

/**
 * <code>Slice</code> is a {@link LogicalValue} that denotes a contiguous
 * portion of another source {@link LogicalValue}.
 * 
 * @version $Id: Slice.java 568 2008-03-31 17:23:31Z imiller $
 */
public class Slice implements LogicalValue {

	/** The value containing this slice */
	private LogicalValue targetValue;

	/** The offset from the start of the targetValue */
	private int delta;

	/** The size, in addressable units, of this slice */
	private int size;

	/** The size, in bits, of this slice */
	private int bitSize;

	/**
	 * Constructs a new <code>Slice</code> instance. Note that part or all of
	 * the slice may lie outside the bounds of the target value. The value of
	 * such bits will default to 0.
	 * 
	 * @param targetValue
	 *            the value from which this slice is taken
	 * @param delta
	 *            the offset in addressable units from the start of
	 *            <code>targetValue</code> at which the slice begins
	 * @param size
	 *            the size in addressable units of the slice
	 */
	public Slice(LogicalValue targetValue, int delta, int size) {
		if (targetValue == null) {
			throw new NullPointerException("null targetValue");
		}

		this.targetValue = targetValue;
		this.delta = delta;
		this.size = size;
		this.bitSize = size * targetValue.getAddressStridePolicy().getStride();
	}

	/**
	 * Implementation of the MemoryVisitable interface.
	 * 
	 * @param memVis
	 *            a non null 'MemoryVisitor'
	 * @throws NullPointerException
	 *             if memVis is null
	 */
	public void accept(MemoryVisitor memVis) {
		memVis.visit(this);
	}

	/** @inheritDoc */
	public int getSize() {
		return size;
	}

	public int getBitSize() {
		return this.bitSize;
	}

	/**
	 * Returns the address stride policy governing this slice, deferring to the
	 * policy of the base logical value being sliced.
	 */
	public AddressStridePolicy getAddressStridePolicy() {
		return this.targetValue.getAddressStridePolicy();
	}

	/**
	 * Returns the number of addressable units offset from the source logical
	 * value that this slice selects.
	 * 
	 * @return the addressable unit offset.
	 */
	public int getDelta() {
		return this.delta;
	}

	/**
	 * Returns the alignment size of the backing logical value.
	 * 
	 * @return a value of type 'int'
	 */
	public int getAlignmentSize() {
		// defer to the alignment size of the backing logical value
		return getSourceValue().getAlignmentSize();
	}

	public LogicalValue getSourceValue() {
		return targetValue;
	}

	/** @inheritDoc */
	public AddressableUnit[] getRep() {
		final AddressableUnit[] rep = new AddressableUnit[getSize()];
		System.arraycopy(targetValue.getRep(), delta, rep, 0, getSize());
		return rep;
	}

	/** @inheritDoc */
	public LogicalValue copy() {
		return new Slice(targetValue.copy(), delta, size);
	}

	/** @inheritDoc */
	public LogicalValue removeRange(int min, int max)
			throws NonRemovableRangeException {
		if ((min < 0) || (min >= size) || (max >= size) || (max <= min)) {
			throw new NonRemovableRangeException("invalid range");
		}

		if (min == 0) {
			/*
			 * Remove from the lower end.
			 */
			return targetValue.getValueAtOffset((max + 1),
					(getSize() - max - 1));
		} else if (max == (getSize() - 1)) {
			/*
			 * Remove from the upper end.
			 */
			return targetValue.getValueAtOffset(delta, min);
		} else {
			/*
			 * Remove from the middle.
			 */
			final List<LogicalValue> elements = new ArrayList<LogicalValue>(2);
			elements.add(targetValue.getValueAtOffset(delta, min));
			elements.add(targetValue.getValueAtOffset((delta + max + 1),
					(getSize() - max - 1)));
			return new Record(elements);
		}
	}

	/** @inheritDoc */
	public MemoryConstant toConstant() {
		final MemoryConstant targetConstant = targetValue.toConstant();
		return new SliceConstant(targetConstant, delta, getSize());
	}

	/** @inheritDoc */
	public Location toLocation() {
		return Location.INVALID;
	}

	/** @inheritDoc */
	public LogicalValue getValueAtOffset(int delta, int size) {
		return targetValue.getValueAtOffset(delta + this.delta, size);
	}

	public String toString() {
		return "[Slice(" + targetValue + ", " + delta + ", " + getSize() + ")]";
	}
}

/**
 * <code>SliceConstant</code> here is a constant that derives its value from a
 * slice of another target {@link MemoryConstant}.
 * 
 * @version $Id: Slice.java 568 2008-03-31 17:23:31Z imiller $
 */
class SliceConstant extends MemoryConstant {
	/** Constant from which slice is taken */
	private MemoryConstant sourceConstant;

	/** Bit offset from MSB of sourceConstant */
	private int delta;

	/** The number of units wide that this slice constants value is. */
	private int sliceUnits;

	/**
	 * Creates a new <code>SliceConstant</code> instance. Since the
	 * <code>sourceConstant</code> or a component of it may be deferred, the
	 * value representation is re-built each time that the getRepBundle method
	 * is called, and consequently, only known, fixed constant bits are
	 * propagated out to the bus on each call to pushValuesForward.
	 * 
	 * @param sourceConstant
	 *            the constant from which the slice is taken
	 * @param delta
	 *            the offset in units from the start of the source constant
	 * @param size
	 *            the size in units of the slice
	 */
	SliceConstant(MemoryConstant sourceConstant, int delta, int size) {
		// super((size * 8), false);
		super((size * sourceConstant.getRepBundle().getBitsPerUnit()), false);
		this.sourceConstant = sourceConstant;
		this.sliceUnits = size;
		this.delta = delta;

		pushValuesForward();
	}

	/**
	 * Returns a single element, non modifiable list containing this constant as
	 * its only constituent is itself.
	 * 
	 * @return a List containing only this constant
	 */
	public List<Constant> getConstituents() {
		return Collections.unmodifiableList(Collections
				.singletonList((Constant) this));
	}

	/**
	 * Returns a non modifiable set containing this object as well as the
	 * backing Constant.
	 * 
	 * @return a Set containing this object and its backing constant.
	 */
	public Set<Constant> getContents() {
		Set<Constant> contents = new HashSet<Constant>();
		contents.add(this);
		contents.add(this.sourceConstant);
		return Collections.unmodifiableSet(contents);
	}

	/**
	 * Returns the bundle defining the state of this constant. Because the
	 * backing constants may or may not be fixed at any point in the
	 * compilation, the AURep array is re-built on each call to this method.
	 * Thus any changes (locking) of any backing constant will be propagated to
	 * this constants representation on the next call.
	 * 
	 * @return a value of type 'AURepBundle'
	 */
	@Override
	public AURepBundle getRepBundle() {
		AURepBundle backing = this.sourceConstant.getRepBundle();
		AddressableUnit[] ourRep = new AddressableUnit[this.sliceUnits];
		for (int i = 0; i < ourRep.length; i++) {
			// If the slice falls off the end of the backing constant,
			// just fill with zeros.
			int index = i + delta;
			if (index < 0 || index >= backing.getRep().length)
				ourRep[i] = new AddressableUnit(0, true);
			else
				ourRep[i] = backing.getRep()[index];
		}
		return new AURepBundle(ourRep, backing.getBitsPerUnit());
	}

	public String toString() {
		return super.toString() + " delta " + delta;
	}

}
