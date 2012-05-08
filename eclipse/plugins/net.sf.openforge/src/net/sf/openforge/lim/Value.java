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

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.sf.openforge.util.HexBits;
import net.sf.openforge.util.SizedInteger;

/**
 * The current value of an Bus which contains any information known about the
 * bits that comprise that bus: constant, global, care, number of bits,
 * originating bus, number of bits in required in the bus, is the bus signed.
 * 
 * This is a rewrite of the original Value/Bit classes to reduce the number of
 * Bit objects created. Originally each value was queried for each Bit, then
 * each Bit was queried for its attributes. Now the Value is queried for the
 * attributes of each bit.
 * 
 * 
 * @version $Id: Value.java 41 2005-10-17 15:10:57Z imiller $
 */
public class Value {

	/** True if this is a signed value, false otherwise */
	private boolean isSigned;

	/**
	 * the size of the Value. this is reduced when the top bit is set to dont
	 * care
	 */
	private int size;

	/**
	 * isCare is bit 0 of state
	 * 
	 * isCare is TRUE if all bits are care, FALSE if all bits are not care, and
	 * ARRAY_VALID if isCareArray is valid. isCareArray (if valid) is an array
	 * of 0..(size-1) where the index corresponds to bit position, and the value
	 * is true if the bit is constant, else false.
	 * 
	 * current state next state setCare(i,careState) const[i]=any
	 * const[i]=NOT_CONSTANT care[i]=any care[i]=careState owner[i]=any owner[i]
	 * unchanged
	 */

	/**
	 * bits 1-2 of state
	 * 
	 * similar to isCare, but describes constant bits. if
	 * isConstant=NOT_CONSTANT there are no constant bits, if ONE then all
	 * constant bits, if 2 then some constant bits. isConstantArray is defined
	 * for isConstant > 0, isConstantArray contains ZERO or ONE or NOT_CONSTANT
	 * for non-constant bits
	 * 
	 * current state next state setConstant(i,constState) care[i]=any
	 * care[i]=true const[i]=any const[i]=constState owner[i]=any owner[i]=null
	 */

	/**
	 * bit 3 of state
	 * 
	 * similar to isCare, but describes global bits - derived from
	 * owner/ownerArray
	 */

	/**
	 * holds the bus that "owns" this value, if null then consult ownerArray. if
	 * ownerArray is null then there is no owner for any bit; if owner array is
	 * not null, then each element corresponds to the owner of a particular
	 * position. ownerArray is only defined if owner==null
	 * 
	 * current state next state setOwner(i,bus) const[i]=any
	 * const[i]=NOT_CONSTANT care[i]=any care[i] unchanged owner[i]=any
	 * owner[i]=bus
	 * 
	 * ownerOffset, if set, is the offset of the bit in the driving bus.if not
	 * set, the offset is the same this value
	 * 
	 * ownerOffset is bits 4-9 of state
	 */

	private int[] state;

	private Bus owner = null;
	private Bus[] ownerArray = null;

	/**
	 * holds the bus that "owns" the inverse of this value, if null then consult
	 * ownerInvArray. if ownerInvArray is null then there is no inverted owner
	 * for any bit; if owner invert array is not null, then each element
	 * corresponds to the owner of a particular position
	 * 
	 * ownerInvOffset is bits 10-15 of state
	 */
	private Bus ownerInv = null;
	private Bus[] ownerInvArray = null;

	/**
	 * The array of bits that comprise this value - used only if bits need to be
	 * generated - it is expected that this will be deprecated
	 */

	/** constant symbols */
	public static final int ONE = 1;
	public static final int ZERO = 0;
	public static final int NOT_CONSTANT = 3;
	public static final int ARRAY_VALID = 2;
	public static final int CARE = 1;
	public static final int DONT_CARE = 0;
	public static final int TRUE = 1;
	public static final int FALSE = 0;

	private static final int CARE_MASK = 0x1; // bit 0
	private static final int CONSTANT_MASK = 0x6; // bits 1-2
	private static final int CONSTANT_NOT = 0x6;
	private static final int CONSTANT_ONE = 0x2;

	private static final int GLOBAL_MASK = 0x8; // bit 3
	private static final int OWNER_MASK = 0x3f0; // bits 4-9
	private static final int INV_OWNER_MASK = 0xfc00; // bits 10-15

	/**
	 * Constructs a new Value of a specified size, all bits care and global.
	 * 
	 * @param size
	 *            the number of bits in this value
	 * @param isSigned
	 *            true if this is a signed value, false if unsigned
	 */
	public Value(int size, boolean isSigned) {
		state = new int[size];
		for (int i = 0; i < size; i++) {
			state[i] = CARE_MASK | GLOBAL_MASK | CONSTANT_NOT | (i << 4)
					| (i << 10);
		}
		this.size = size;
		this.isSigned = isSigned;
	}

	/**
	 * Constructs a new Value with with all bits owned by a specific bus
	 * 
	 * @param bus
	 *            the bus that owns all bits
	 * @param size
	 *            the size of the bus
	 * @param isSigned
	 *            true if this is a signed value, false if unsigned
	 */
	public Value(Bus bus, int size, boolean isSigned) {
		this.size = size;
		state = new int[size];
		for (int i = 0; i < size; i++) {
			state[i] = CARE_MASK | CONSTANT_NOT | (i << 4) | (i << 10);
		}
		owner = bus;
		this.isSigned = isSigned;
	}

	/**
	 * Constructs a new Value based on the bits of an existing Value
	 * 
	 * @param value
	 *            value to copy
	 * @param isSigned
	 *            true if this is a signed value, false if unsigned
	 */
	public Value(Value value, boolean isSigned) {

		size = value.size;
		state = new int[size];
		System.arraycopy(value.state, 0, state, 0, size);

		owner = value.owner;
		if (value.ownerArray != null) {
			assert size == value.ownerArray.length;

			ownerArray = new Bus[size];
			System.arraycopy(value.ownerArray, 0, ownerArray, 0, size);
		}

		ownerInv = value.ownerInv;
		if (value.ownerInvArray != null) {
			assert size == value.ownerInvArray.length;
			ownerInvArray = new Bus[size];
			System.arraycopy(value.ownerInvArray, 0, ownerInvArray, 0, size);
		}
		this.isSigned = isSigned;
	}

	/**
	 * Gets the size of this value.
	 * 
	 * @return the number of bits in this value
	 */
	public int getSize() {
		return size;
	}

	/**
	 * Is this a signed value?
	 * 
	 * @return true if signed
	 */
	public boolean isSigned() {
		return isSigned;
	}

	/**
	 * Sets the bit at a specific position to be a care bit.
	 * 
	 * TBD: add method to set entire value in one call if useful
	 * 
	 * @param position
	 *            the index of the bit, 0 <= position < {@link #getSize()}
	 * @param care
	 *            true if care, false if don't care
	 */
	public void setCare(int position, boolean care)
			throws IndexOutOfBoundsException {
		/*
		 * RT-72 setConstant(position, NOT_CONSTANT);//so a constant to care
		 * works
		 */

		if (care) {
			state[position] |= CARE_MASK;
		} else {
			state[position] &= (~CARE_MASK);
			setOwner(position, null);
		}

		/*
		 * RT-72 state[position] |= CONSTANT_NOT;
		 */
	}

	/**
	 * Sets the bit at a specific position to be a constant bit.
	 * 
	 * TBD: add method to set entire value in one call if useful
	 * 
	 * @param position
	 *            the index of the bit, 0 <= position < {@link #getSize()}
	 * @param constant
	 *            ZERO or 1 if constant, NOT_CONSTANT if not constant
	 */
	public void setConstant(int position, int constant)
			throws IndexOutOfBoundsException, IllegalArgumentException {
		assert constant >= ZERO && constant <= NOT_CONSTANT : "Illegal argument "
				+ constant + " to setConstant";

		state[position] |= CARE_MASK;

		// first reset the constant state to 0, then or in the correct state
		state[position] &= (~CONSTANT_MASK);

		switch (constant) {
		case ONE:
			state[position] |= CONSTANT_ONE;
			break;
		case NOT_CONSTANT:
			state[position] |= CONSTANT_NOT;
			break;
		}

		// owner to be set to null
		state[position] |= GLOBAL_MASK;
		if (owner != null) // if owner not null then all bits owned by the same
							// bus
		{
			ownerArray = new Bus[size];

			for (int i = 0; i < size; i++) {
				ownerArray[i] = owner;
			}
			ownerArray[position] = null;
			owner = null;
		} else if (ownerArray != null) {
			ownerArray[position] = null;
			updateOwner();
		} // else both owner and ownerArray are null and the bit was already
			// global
	}

	/**
	 * sets the owner of a specific bit
	 * 
	 * @param position
	 *            bit to modify
	 * @param owner
	 *            of the bit
	 */
	public void setOwner(int position, Bus owner)
			throws IndexOutOfBoundsException {

		// nothing for care bits

		/*
		 * RT-72 // set constant to NOT_CONSTANT if owner != null if (owner !=
		 * null) { state[position] |= NOT_CONSTANT; }
		 */

		// update owner
		if (this.owner != null) {
			ownerArray = new Bus[size];
			for (int i = 0; i < size; i++) {
				ownerArray[i] = this.owner;
				if (this.owner == null) {
					state[i] |= GLOBAL_MASK;
				} else {
					state[i] &= (~GLOBAL_MASK);
				}
			}
			ownerArray[position] = owner;
			if (owner == null) {
				state[position] |= GLOBAL_MASK;
			} else {
				state[position] &= (~GLOBAL_MASK);
			}
			this.owner = null;
		} else if (ownerArray != null) {
			ownerArray[position] = owner;
			if (owner == null) {
				state[position] |= GLOBAL_MASK;
			} else {
				state[position] &= (~GLOBAL_MASK);
			}
			updateOwner();
		} else // create an ownerArray if both owner and ownerArray were null
		{
			ownerArray = new Bus[size];
			for (int i = 0; i < size; i++) {
				ownerArray[i] = null;
				state[i] |= GLOBAL_MASK;
			}
			ownerArray[position] = owner;
			if (owner == null) {
				state[position] |= GLOBAL_MASK;
			} else {
				state[position] &= (~GLOBAL_MASK);
			}
		}
	}

	/**
	 * set the owner of a specific bit, used when the source bit position is
	 * different than the position in this Value
	 * 
	 * @param position
	 *            in this Value
	 * @param owner
	 *            source of the bit
	 * @param ownerPosition
	 *            offset in the owner
	 */
	public void setOwner(int position, Bus owner, int ownerPosition) {
		setOwner(position, owner);
		// zero the owner offset bits, then mask in the correct value
		state[position] &= ~OWNER_MASK;
		state[position] |= (ownerPosition << 4);
	}

	/**
	 * get the position of the bit in the driving bus
	 * 
	 * @param i
	 *            index of the bit in this Value
	 */
	public int getPosition(int i) {
		return (state[i] & OWNER_MASK) >> 4;
	}

	/**
	 * sets the inverted owner of a specific bit
	 * 
	 * @param position
	 *            bit to modify
	 * @param owner
	 *            of the bit
	 * @param ownerPosition
	 *            position in owner of the bit
	 */
	public void setInvertedOwner(int position, Bus owner, int ownerPosition)
			throws IndexOutOfBoundsException {
		if (getInvertedOwner(position) == owner
				&& getInvertedOffset(position) == ownerPosition) {
			return;
		}

		// TBD: do we need to maintain care/constant/global status for inverted
		// bits?

		// update owner
		if (ownerInv != null) {
			ownerInvArray = new Bus[size];
			for (int i = 0; i < size; i++) {
				ownerInvArray[i] = ownerInv;
			}
			ownerInvArray[position] = owner;

			ownerInv = null;
		} else if (ownerInvArray != null) {
			ownerInvArray[position] = owner;
			updateOwnerInv();
		} else // create an ownerArray if both owner and ownerArray were null
		{
			ownerInvArray = new Bus[size];
			for (int i = 0; i < size; i++) {
				ownerInvArray[i] = null;
			}
			ownerInvArray[position] = owner;
		}

		// and update the ownerInvOffset
		state[position] &= ~INV_OWNER_MASK;
		state[position] |= ownerPosition;
	}

	/**
	 * assumes ownerArray != null, are all entries the same? also updates
	 * isGlobal/isGlobalArray
	 */
	private void updateOwner() {
		boolean allSame = true;
		Bus first = ownerArray[0];

		for (int i = 1; i < size && allSame; i++) {
			if (first != ownerArray[i]) {
				allSame = false;
			}
		}

		if (allSame) {
			owner = first;
			ownerArray = null;
		}
	}

	/**
	 * assumes ownerInvArray != null, are all entries the same?
	 */
	private void updateOwnerInv() {
		boolean allSame = true;
		Bus first = ownerInvArray[0];

		for (int i = 1; i < size && allSame; i++) {
			if (first != ownerInvArray[i]) {
				allSame = false;
			}
		}
		if (allSame) {
			ownerInv = first;
			ownerInvArray = null;
		}
	}

	/**
	 * Gets the bit at a specific position.
	 * 
	 * @param position
	 *            the index of the bit, 0 <= position < {@link #getSize()}
	 * @return the bit at the specified position
	 */
	public Bit getBit(int position) throws IndexOutOfBoundsException {
		assert position < size : "Attempting to get a bit which is too large for this value "
				+ position + " " + size;

		if (isGlobal(position)) {
			/*
			 * RT-72 Since value is not made NOT_CONSTANT by modifying care, a
			 * value can both be ON/OFF and don't care. Hence check for don't
			 * care before checking for ON/OFF
			 */
			if (!isCare(position)) {
				return Bit.DONT_CARE;
			} else if (isOn(position)) {
				return Bit.ONE;
			} else if (isOff(position)) {
				return Bit.ZERO;
			} else if (isCare(position)) {
				return Bit.CARE;
			}
		}
		Bus bitOwner = getOwner(position);

		Bit bit = new Bit(bitOwner, getPosition(position));

		return bit;
	}

	/**
	 * set a bit of the Value
	 */
	public void setBit(int position, Bit bit) {
		assert position < size : "Attempting to set a bit which is too large for this value";

		if (bit.isConstant()) {
			setConstant(position, bit.isOn() ? 1 : 0);
		}

		/*
		 * RT-72 Call setCare() after setConstant() as setConstant always sets
		 * the care bit to true
		 */
		setCare(position, bit.isCare());

		if (bit.isGlobal()) {
			setOwner(position, null);
		} else {
			setOwner(position, bit.getOwner(), bit.getPosition());
		}
		Bit inverted = bit.getInvertedBit();
		if (inverted != null) {
			setInvertedOwner(position, inverted.getOwner(),
					inverted.getPosition());
		}
	}

	/**
	 * set a bit to have the same parameters as another bit from a Value
	 * 
	 * @param position
	 *            bit to set
	 * @param value
	 *            source value
	 * @param valuePosition
	 *            offset in source value
	 */
	public void setBit(int position, Value value, int valuePosition) {
		assert position < size : "Attempting to set a bit which is too large for this value";

		state[position] = value.state[valuePosition];
		setOwner(position, value.getOwner(valuePosition));// ,
															// value.getPosition(valuePosition));
	}

	/**
	 * Gets the bits which comprise this value.
	 * 
	 * @return the array of {@link Bit Bits}
	 */
	public Bit[] getBits() {
		Bit[] bits = new Bit[size];
		for (int i = 0; i < size; i++) {
			bits[i] = getBit(i);
		}

		return bits;
	}

	/** Gets the bits which comprise this Value as a list */
	public List<Bit> getBitsList() {
		return Arrays.asList(getBits());
	}

	/**
	 * return true or false for care status of bit position
	 */
	public boolean isCare(int position) {
		return (state[position] & CARE_MASK) == CARE_MASK;
	}

	/**
	 * return true if isCare == true and bit is global
	 */
	public boolean isGenericCare(int position) {
		return (isGlobal(position) && isCare(position) && (getConstant(position) == NOT_CONSTANT));
	}

	/**
	 * return the value of bit position
	 * 
	 * @return ZERO, ONE or NOT_CONSTANT
	 */
	public int getConstant(int position) {
		return (state[position] & CONSTANT_MASK) >> 1;
	}

	/**
	 * return true if bit position is constant and valued ONE
	 */
	public boolean isOn(int position) {
		return getConstant(position) == ONE;
	}

	/**
	 * return true if bit position is constant and valued ZERO
	 */
	public boolean isOff(int position) {
		return getConstant(position) == ZERO;
	}

	/**
	 * return the owner of the given bit, or null if no owner (global)
	 */
	public Bus getOwner(int position) {
		if (owner != null) {
			return owner;
		}
		if (ownerArray != null) {
			return ownerArray[position];
		}
		return null;
	}

	/**
	 * return the owner of the inverted bit if defined, else null
	 * 
	 * @param position
	 * @return Bus or null if not defined
	 */
	public Bus getInvertedOwner(int position) {
		if (ownerInv != null) {
			return ownerInv;
		} else if (ownerInvArray != null) {
			return ownerInvArray[position];
		}
		return null;
	}

	/**
	 * return the bus position of the inverted bit. only valid if
	 * getInvertedOwner() does not return null
	 */
	public int getInvertedOffset(int position) {
		return (state[position] & INV_OWNER_MASK) >> 10;
	}

	/**
	 * return global status of the given bit
	 */
	public boolean isGlobal(int position) {
		return ((state[position] & GLOBAL_MASK) == GLOBAL_MASK);
	}

	/**
	 * Gets the current vector of on/off states as a mask.
	 */
	public long getValueMask() {
		long value = 0L;
		int msb = getConstant(size - 1);
		for (int i = 0; i < 64; i++) {
			if (i < size) {
				int constant = getConstant(i);

				if (constant == ONE) {
					value |= (1L << i);
				}
			} else {
				if (isSigned && msb == ONE) {
					value |= (1L << i);
				}
			}
		}
		return value;
	}

	/**
	 * Gets the current vector of care/don't-care states as a mask.
	 */
	public long getCareMask() {
		long mask = 0L;
		for (int i = 0; i < size; i++) {
			if (isCare(i)) {
				mask |= (1L << i);
			}
		}
		return mask;
	}

	/**
	 * Gets the current vector of constant/non-constant states as a mask.
	 */
	public long getConstantMask() {
		long mask = 0L;
		for (int i = 0; i < size; i++) {
			if (getConstant(i) < NOT_CONSTANT) {
				mask |= (1L << i);
			}
		}
		return mask;
	}

	/**
	 * Returns true if all Bits in this Value are ONE, ZERO, or DONT_CARE.
	 * 
	 * @return true if this Value represents a constant value.
	 */
	public boolean isConstant() {
		for (int i = 0; i < getSize(); i++) {
			int constant = getConstant(i);

			if (constant == NOT_CONSTANT && isCare(i)) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Returns true if every Bit in this value is a DONT_CARE.
	 * 
	 * @return true if the whole value is dont care.
	 */
	public boolean isDontCare() {
		for (int i = 0; i < getSize(); i++) {

			if (isCare(i)) {
				return false;
			}

		}
		return true;
	}

	/**
	 * Tests whether all the care bits are of the same type (constants or
	 * non-constants from the same source Bus). Also considers any
	 * non-contiguous series of care bits to be mixed. Also considers any sign
	 * extended value to be mixed.
	 * 
	 * @return false if all the Bits are the same type; otherwise true
	 * 
	 * @throws IllegalStateException
	 *             if the value is not constant, but none of the Bits has an
	 *             owner Bus
	 */
	public boolean isMixed() {
		if (isConstant()) {
			// but - if there are any d/c's in the constant, then its mixed
			for (int i = 0; i < size; i++) {
				if (!isCare(i)) {
					return true;
				}
			}

			return false;
		} else {
			/*
			 * It's mixed if we find any constant bits, or if there are Bits
			 * from more than one owner Bus.
			 */
			Bus bitOwner = null;
			// for (Iterator iter = bits.iterator(); iter.hasNext();)
			for (int i = 0; i < size; i++) {
				Bus iOwner = getOwner(i);

				if (iOwner != null && bitOwner == null) {
					bitOwner = iOwner;
				}
				if (getConstant(i) < NOT_CONSTANT) {
					return true;
				}
			}

			assert bitOwner != null : "care Bits with no owner";

			// If we are sign extended then we are mixed
			// (a non-contiguous segment of Bits exists)

			if (bitOwner.getSize() < size) {
				return true;
			}

			for (int i = 0; i < size; i++) {
				if (getPosition(i) != i) {
					return true;
				}
			}
			return false;
		}
	}

	public String debugState() {
		String result = size + " bits: \n";

		for (int i = 0; i < size; i++) {
			result += "\t" + Integer.toHexString(state[i]) + " "
					+ getInvertedOffset(i) + " " + getPosition(i) + " "
					+ isGlobal(i) + " " + getConstant(i) + " " + isCare(i)
					+ "\n";
		}
		return result;
	}

	/**
	 * Kinda handy way to display this Value's state.
	 */
	public String debug() {
		String ret = "";
		String sizeString;
		String length;

		for (int i = 0; i < size; i++) {
			int constant = getConstant(i);

			/* RT-72 */
			if (!isCare(i)) {
				ret = 'x' + ret;
			} else if (constant == 0 || constant == 1) {
				ret = constant + ret;
			} else if (isCare(i)) {
				if (isGlobal(i)) {
					ret = 'C' + ret;
				} else {
					ret = 'c' + ret;
				}
			} else {
				ret = 'x' + ret;
			}
		}
		sizeString = Integer.toString(size);
		length = (size < 10) ? (" " + sizeString) : sizeString;

		return length + "'[" + ret + "]" + (isSigned() ? "+/-" : "");
	}

	public String debugIsGlobal() {
		String result = "";
		for (int i = 0; i < size; i++) {
			result = ((state[i] & GLOBAL_MASK) == GLOBAL_MASK ? 1 : 0) + result;
		}
		return result;
	}

	public String bitSourceDebug() {
		// System.err.println("bitSourceDebug("+debug()+") ownerOffset: "+ownerOffset);
		// for (int i=0; i<size; i++)

		String ret = null;
		boolean endOfArray = false;

		for (int i = 0; i < size; i++) {
			List<String> group = new ArrayList<String>();
			Bus owner = null;
			int start = -1;
			int stop = -1;
			boolean generic = true;
			if (isGlobal(i)) {
				boolean care = isCare(i);
				int constant = getConstant(i);

				while (isCare(i) == care && getConstant(i) == constant
						&& i < size && isGlobal(i)) {
					if (!care) {
						group.add("x");
					} else if (constant == 0) {
						group.add("0");
					} else if (constant == 1) {
						group.add("1");
					} else if (care) {
						group.add("C");
					}

					if (i == size - 1) {
						endOfArray = true;
						break;
					}
					i++;
				}
			} else {
				generic = false;
				Bus currentSource = getOwner(i);
				int startOffset = getPosition(i);
				int count = 0;
				while (getOwner(i) == currentSource
						&& getPosition(i) == (startOffset + count) && i < size) {
					owner = currentSource;
					if (start == -1) {
						start = startOffset;
					}
					stop = getPosition(i);

					if (isCare(i)) {
						group.add("c");
					} else {
						group.add("x");
					}

					if (i == size - 1) {
						endOfArray = true;
						break;
					}
					i++;
					count++;
				}
			}

			// We have to look at the 'next' bit and thus increment i
			// each time. But, unless we got to the end we need to
			// processing the non-matching bit so decrement i for the
			// 'for' loop.

			String next = "";
			if (generic) {
				next = group.size() + "'" + group.get(0).toString();
			} else if (group.size() == 1) {
				next = owner + "[" + start + "]";
			} else {
				next = owner + "[" + stop + ":" + start + "]";
			}

			if (ret == null) {
				ret = next;
			} else {
				ret = next + "," + ret;
			}
			if (endOfArray) {
				break;
			} else {
				i--;
			}

		}
		String sizeString;
		String length;

		sizeString = Integer.toString(size);
		length = (size < 10) ? (" " + sizeString) : sizeString;

		return length + "'[" + ret + "]" + (isSigned() ? "+/-" : "");
	}

	/**
	 * Returns true if all the bits are equivalent which means that they are the
	 * SAME bit unless one is {@link Bit#CARE} in which case it is equivalent to
	 * any other non-constant non-dontcare bit.
	 * 
	 * @param test
	 *            the Value to test against this one
	 * @return true if they are equivalent Values.
	 */
	public boolean equivalent(Value test) {
		if (test.isSigned() != isSigned()) {
			return false;
		}

		if (test.getSize() != getSize()) {
			return false;
		}

		// default to care
		// Bit localBit = Bit.CARE;
		// Bit testBit = Bit.CARE;
		for (int i = 0; i < size; i++) {
			boolean localIsCare = isCare(i);
			boolean testIsCare = test.isCare(i);
			int localConstant = getConstant(i);
			int testConstant = test.getConstant(i);
			boolean localIsGlobal = isGlobal(i);
			boolean testIsGlobal = test.isGlobal(i);

			// Test that they are exactly the same BIT unless one of
			// them is Bit.CARE, in which case the other must be any
			// non-const/non-dc bit.
			if (localIsGlobal && localIsCare && localConstant == NOT_CONSTANT) {
				if (testConstant == ONE || testConstant == ZERO || !testIsCare) {
					return false;
				}

			} else if (testIsGlobal && testIsCare
					&& testConstant == NOT_CONSTANT) {
				if (localConstant == ONE || localConstant == ZERO
						|| !localIsCare) {
					return false;
				}

			} else if (!bitEquals(i, test, i)) {
				return false;
			}
		}
		return true;
	}

	/**
	 * compare bit position of this value with another value's bit position.
	 * 
	 * @param position
	 *            the bit of this value to compare
	 * @param test
	 *            the value to compare with
	 * @param testPosition
	 *            the bit of the test value
	 */
	public boolean bitEquals(int position, Value test, int testPosition) {
		if (isCare(position) != test.isCare(testPosition)) {
			return false;
		}
		if (isGlobal(position)
				&& getConstant(position) != test.getConstant(testPosition)) {
			return false;
		}
		if (getOwner(position) != test.getOwner(testPosition)) {
			return false;
		}

		// if we have gotten this far, and the bits are global, then they are
		// equivalent.
		if (isGlobal(position)) {
			return true;
		}

		if (getPosition(position) != test.getPosition(testPosition)) {
			return false;
		}
		if (getInvertedOwner(position) != test.getInvertedOwner(testPosition)) {
			return false;
		}
		if (getInvertedOwner(position) != null
				&& getInvertedOffset(position) != test
						.getInvertedOffset(testPosition)) {
			return false;
		}
		return true;
	}

	/**
	 * compare bit position of this value with another Bit
	 * 
	 * @param position
	 *            the bit of this value to compare
	 * @param test
	 *            the Bit to compare with
	 */
	public boolean bitEquals(int position, Bit testBit) {
		Value containerValue = new Value(1, false);
		containerValue.setBit(0, testBit);
		return bitEquals(position, containerValue, 0);
	}

	/**
	 * Joins the values by following these rules for each bit position:
	 * <p>
	 * <ul>
	 * <li>if either bit is a care, result bit is a care
	 * <li>if either bit is non constant, result bit is not constant
	 * <li>if both bits are constant AND same, result bit is that constant
	 * <li>if either value is signed, then the joined value is signed
	 * </ul>
	 * 
	 * @param value
	 *            the Value to union with this Value.
	 * @return a new Value representing the state achieved after merging this
	 *         Value with the specified one.
	 */
	public Value union(Value value) {
		assert value.getSize() == size : "size mismatch: " + value.getSize()
				+ ", " + size;
		assert value.isSigned() == isSigned : "sign mismatch: "
				+ value.isSigned() + ", " + isSigned;

		final Value newValue = new Value(size, isSigned);
		for (int i = 0; i < size; i++) {
			if (bitEquals(i, value, i)) {
				newValue.setBit(i, this, i);
			} else {
				newValue.setCare(i, true);
			}
		}
		return newValue;
	}

	/**
	 * Returns the minimum number of care bits of which is significant for
	 * constant propagation.
	 * 
	 * @return int of minimum care bits to represent this value.
	 */
	public int getCompactedSize() {
		int compactedSize = size;

		int msb = size - 1;

		if (!isSigned() && isCare(msb) && !isOff(msb)) {
			return compactedSize;
		} else {
			for (int i = size - 2; i >= 0; i--) {
				// Compare current bit with msb
				if (isGenericCare(msb)) {
					return (compactedSize == 0) ? 1 : compactedSize;
				} else if (!isGlobal(msb) && (!bitEquals(msb, this, i))) {
					return (compactedSize == 0) ? 1 : compactedSize;
				} else if (isCare(msb) && (!bitEquals(msb, this, i))) {
					return (compactedSize == 0) ? 1 : compactedSize;
				} else {
					compactedSize--;
					msb = i;
				}
			}
		}
		return (compactedSize == 0) ? 1 : compactedSize;
	}

	@SuppressWarnings("unused")
	private void debugBit(int i) {
		System.err.println("bit " + i + " isGlobal " + isGlobal(i) + " isCare "
				+ isCare(i) + " getConstant " + getConstant(i)
				+ " isGenericCare " + isGenericCare(i) + " owner "
				+ getOwner(i) + " offset " + getPosition(i));
	}

	/**
	 * Gets the {@link SizedInteger} representation of this value. Any
	 * <code>DONT_CARE</code> bits will be represented as 0. This value must not
	 * contain non-global bus bits.
	 * 
	 * @return a <code>SizedInteger</code> representation
	 * @throws IllegalStateException
	 *             if this value contains any non-global bits
	 */
	public SizedInteger toNumber() {
		BigInteger bigInteger = BigInteger.ZERO;
		for (int i = 0; i < size; i++) {
			if (!isGlobal(i)) {
				throw new IllegalStateException("non global bit at index " + i);
			}

			/* RT-72 */
			if (isOn(i) && isCare(i)) {
				bigInteger = bigInteger.setBit(i);
			}
		}

		return SizedInteger.valueOf(bigInteger, getSize(), isSigned());
	}

	/**
	 * Parses a string representation into a new {@link Value}.
	 * 
	 * 
	 * @param token
	 *            a string consisting of a sequence of '1', '0', 'c', or 'x',
	 *            one for each bit; the beginning of the string corresponds to
	 *            the most significant bit
	 * @return a new value with the corresponding bits
	 * @exception IllegalArgumentException
	 *                if an invalid character appears in the sequence
	 */
	public static Value parseValue(String token)
			throws IllegalArgumentException {
		final char[] sequence = token.toLowerCase().toCharArray();
		Value value = new Value(sequence.length, false);

		for (int i = sequence.length - 1; i >= 0; i--) {
			int index = sequence.length - 1 - i;

			switch (sequence[i]) {
			case '1':
				value.setConstant(index, 1);
				break;
			case '0':
				value.setConstant(index, 0);
				break;
			case 'c':
				value.setCare(index, true);
				break;
			case 'x':
				value.setCare(index, false);
				break;
			default:
				throw new IllegalArgumentException("invalid bit specifier: "
						+ token.charAt(i));
			}

		}
		return value;
	}

	/**
	 * Utility for obtaining a Value object whose state is all constant
	 * according to the parameter value.
	 */
	public static Value getConstantValue(long constant, int minSize,
			boolean isSigned) {
		int size = HexBits.getSize(constant);

		if (constant > 0) {
			size--;
		}

		Value value = new Value(Math.max(size, minSize), isSigned);

		for (int i = 0; i < size; i++) {
			if (((constant >>> i) & 0x1L) != 0) {
				value.setConstant(i, 1);
			} else {
				value.setConstant(i, 0);
			}
		}

		if (size < minSize) {
			final int padBit = (isSigned && (size > 0)) ? value
					.getConstant(size - 1) : 0;
			for (int i = size; i < minSize; i++) {
				value.setConstant(i, padBit);
			}
		}
		return value;
	}

	public static Value getConstantValue(long constant) {
		return getConstantValue(constant, 0, (constant < 0));
	}

	/**
	 * Returns a Value comprised of only CARE, DONT_CARE, ONE, and ZERO Bits (no
	 * 'pass throughs') based on the supplied value.
	 * 
	 * @param value
	 *            the Value to genericize.
	 * @return a value with no pass through bits.
	 */
	public static Value getGenericValue(Value value) {
		Value newValue = new Value(value.getSize(), value.isSigned());
		for (int i = 0; i < newValue.getSize(); i++) {
			newValue.setConstant(i, value.getConstant(i));
			newValue.setCare(i, value.isCare(i));
		}

		return newValue;
	}

	@Override
	public String toString() {
		return debug();
	}
}
