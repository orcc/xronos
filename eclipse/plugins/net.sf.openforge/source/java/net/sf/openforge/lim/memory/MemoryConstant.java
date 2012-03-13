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

import net.sf.openforge.app.OptionRegistry;
import net.sf.openforge.lim.Bit;
import net.sf.openforge.lim.Value;
import net.sf.openforge.lim.op.Constant;

/**
 * MemoryConstant is the parent class of all Constants inserted into the lim
 * which represent a value or region of memory. This class maintains knowledge
 * of the exact addressing order of the bytes in the constant as well as the
 * endianness of the memory from which the bytes were pulled.
 * 
 * <p>
 * Created: Wed Apr 21 05:10:35 2004
 * 
 * @author imiller, last modified by $Author: imiller $
 * @version $Id: MemoryConstant.java 568 2008-03-31 17:23:31Z imiller $
 */
public abstract class MemoryConstant extends Constant {

	/** Tracks the state of this compilation. */
	private boolean bigEndian;

	/**
	 * Constructs a new MemoryConstant object with the specified bit width for
	 * the value bus and the specified signedness.
	 * 
	 * @param bitWidth
	 *            the width in bits of the value bus.
	 * @param isSigned
	 *            the signedness of the value bus.
	 */
	public MemoryConstant(int bitWidth, boolean isSigned) {
		super(bitWidth, isSigned);
		this.bigEndian = !getGenericJob().getUnscopedBooleanOptionValue(
				OptionRegistry.LITTLE_ENDIAN);

	}

	/**
	 * Returns true if the bytes represented by the getRep method are in big
	 * endian order.
	 * 
	 * @return true if this constants internal byte representation is in big
	 *         endian order. False if internally it is in little endian order.
	 */
	protected boolean isBigEndian() {
		return this.bigEndian;
	}

	/**
	 * Get the rep. If the compilation is big endian, then switch the rep so
	 * that it is now little endian. Then, simply propagate each bit out of the
	 * rep and onto the value bus. However any deferred ByteRep must be
	 * propagated as simply a 'care' bit until it is locked down.
	 * 
	 * @return a value of type 'boolean'
	 */
	public boolean pushValuesForward() {
		boolean mod = false;
		AURepBundle thisRep = getRepBundle();

		Value oldValue = getValueBus().getValue();

		// A sanity check here. I would not expect the rep to be
		// larger than we need.
		int unitsNeeded = (int) Math.ceil(((double) oldValue.getSize())
				/ thisRep.getBitsPerUnit());
		if (thisRep.getLength() > unitsNeeded) {
			getGenericJob()
					.warn("Constant contains more data than needed.  Trimming to needed size");
			AddressableUnit[] newRep = new AddressableUnit[unitsNeeded];
			System.arraycopy(thisRep, 0, newRep, 0, unitsNeeded);
			thisRep = new AURepBundle(newRep, thisRep.getBitsPerUnit());
		}

		if (this.isBigEndian()) {
			AddressableUnit[] swapped = swapEndian(thisRep.getRep());
			thisRep = new AURepBundle(swapped, thisRep.getBitsPerUnit());
		}

		Value newValue = pushValue(oldValue.getSize(), oldValue.isSigned(),
				thisRep);

		mod |= getValueBus().pushValueForward(newValue);
		return mod;
	}

	protected Value pushValue(int width, boolean isSigned, AURepBundle thisRep) {
		Value newValue = new Value(width, isSigned);

		for (int i = 0; i < width; i++) {
			// int byteIndex = i / 8;
			// int bitIndex = i % 8;
			int unitIndex = i / thisRep.getBitsPerUnit();
			int bitIndex = i % thisRep.getBitsPerUnit();
			AddressableUnit unitRep = thisRep.getRep()[unitIndex];
			Bit bit;
			if (!unitRep.isLocked()) {
				bit = Bit.CARE;
			} else {
				// if (((unitRep.value() >>> bitIndex) & 0x1) != 0)
				if (unitRep.getBit(bitIndex) != 0)
					bit = Bit.ONE;
				else
					bit = Bit.ZERO;
			}
			newValue.setBit(i, bit);
		}
		return newValue;
	}

	/**
	 * Does nothing.
	 * 
	 * @return false
	 */
	public boolean pushValuesBackward() {
		return false;
	}

	/**
	 * Reverses the endianness of the array of bytes, returning a new array
	 * populated with the endian swapped contents of the input array.
	 * 
	 * @param rep
	 *            a non null, non zero length array of ByteRep objects
	 * @return a non null, non zero length array of the ByteRep objects from the
	 *         input array in the opposite endian order
	 * @throws NullPointerException
	 *             if rep is null
	 * @throws IllegalArgumentException
	 *             if rep legnth is zero
	 */
	protected static AddressableUnit[] swapEndian(AddressableUnit[] rep) {
		if (rep == null)
			throw new NullPointerException(
					"Null byte representation in constant");
		if (rep.length == 0)
			throw new IllegalArgumentException(
					"Cannot swap endianness of zero length constant");

		AddressableUnit result[] = new AddressableUnit[rep.length];
		for (int i = 0; i < rep.length; i++) {
			result[i] = rep[rep.length - i - 1];
		}
		return result;
	}

}// MemoryConstant
