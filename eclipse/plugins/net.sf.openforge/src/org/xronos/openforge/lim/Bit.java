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

/**
 * Bit represents the value of a single bit in a LIM graph. There are two kinds
 * of Bits. The first kind are global, and are defined to be the static
 * variables {@link ZERO} (a constant 0 bit), {@link ONE} (a constant 1 bit),
 * {@link CARE} (a care bit of unknown value), and {@link DONT_CARE} (a
 * don't-care bit of unknown value).
 * <P>
 * There are also non-global bus bits. A sequence of these Bits is used to
 * represent the components of a {@link Bus}. Each bus Bit is said to be owned
 * by its {@link Bus}.
 * <P>
 * Bits are immutable. In particular, a bus Bit will always belong to the same
 * {@link Bus}. Bits from any source, global or bus, may be arbitrarily combined
 * in a {@link Value} to represent a logical or numerical datum.
 * 
 * @version $Id: Bit.java 2 2005-06-09 20:00:48Z imiller $
 */
public class Bit {

	/** Global constant zero bit */
	public static final Bit ZERO = new Bit();

	/** Global constant one bit */
	public static final Bit ONE = new Bit();

	/** Global care bit */
	public static final Bit CARE = new Bit();

	/** Global don't-care bit */
	public static final Bit DONT_CARE = new Bit();

	/** Owner Bus, if not a global bit */
	private Bus owner = null;

	/** The bit that represents the inversion of this bit; may be null */
	private Bit invertedBit = null;

	/**
	 * Placeholder position for global bits.
	 */
	private static final int ILLEGAL_POSITION = -1;

	/**
	 * This is the position of this bit in the list of its owner Bus. If there
	 * is no owner, it is set to ILLEGAL_POSITION.
	 */
	private final int position;

	/**
	 * Constructs a new non-global Bit. This Bit will be a component of a given
	 * {@link Bus}.
	 * 
	 * @param owner
	 *            the bus to which this bit will belong
	 * @throws IllegalArgumentException
	 *             if 'owner' is null
	 */
	public Bit(Bus owner, int position) throws IllegalArgumentException {
		if (owner == null) {
			throw new IllegalArgumentException("null owner");
		}
		this.owner = owner;
		this.position = position;
	}

	/**
	 * Used only to create global, owner-less bits.
	 */
	private Bit() {
		owner = null;
		position = ILLEGAL_POSITION;
	}

	/**
	 * Tests whether this is a care bit.
	 * 
	 * @return true if this bit is anything other than {@link DONT_CARE}
	 */
	public boolean isCare() {
		return (this != DONT_CARE);
	}

	/**
	 * Tests whether this is a constant bit.
	 * 
	 * @return true if this bit is {@link ZERO} or {@link ONE}, false otherwise
	 */
	public boolean isConstant() {
		return (this == ZERO) || (this == ONE);
	}

	/**
	 * Tests whether this is a constant on bit.
	 * 
	 * @return true if this bit is {@link ONE}, false othwerwise
	 */
	public boolean isOn() {
		return (this == ONE);
	}

	/**
	 * Tests whether this is a constant off bit.
	 * 
	 * @return true if this bit is {@link ZERO}, false othwerwise
	 */
	public boolean isOff() {
		return (this == ZERO);
	}

	/**
	 * Tests whether this is a generic care bit.
	 * 
	 * @return true if this bit is {@link CARE}, false othwerwise
	 */
	public boolean isGenericCare() {
		return (this == CARE);
	}

	/**
	 * Test if this is a global bit
	 * 
	 * @return tru of this bit has no owner
	 */
	public boolean isGlobal() {
		return owner == null;
	}

	/**
	 * Gets the owner of this bit.
	 * 
	 * @return the owner bus if this is a non-global bit, otherwise a null will
	 *         be returned
	 */
	public Bus getOwner() {
		return owner;
	}

	/**
	 * Gets the position of this bit in its owner {@link Bus Bus's} ordered list
	 * of component bits.
	 * 
	 * @return the position of this bit
	 * @throws NullPointerException
	 *             if this is a global bit
	 */
	public int getPosition() throws NullPointerException {
		try {
			if (isGlobal()) {
				throw new NullPointerException();
			} else if (position == -1) {
				Bit[] bitOwnerBits = getOwner().getValue().getBits();
				for (int i = 0; i < bitOwnerBits.length; i++) {
					if (bitOwnerBits[i] == this) {
						return i;
					}
				}
				return -1;
			} else {
				return position;
			}
		} catch (NullPointerException eNull) {
			throw (NullPointerException) new NullPointerException(
					"not a bus bit: " + this).initCause(eNull);
		}
	}

	/**
	 * Gets the bit whose value is the inversion of this bit.
	 * 
	 * @return the inverted bit, or null if not yet set
	 */
	public Bit getInvertedBit() {
		return invertedBit;
	}

	/**
	 * Sets the bit whose value is the inversion of this bit.
	 * 
	 * @param bit
	 *            the inverted bit
	 */
	public void setInvertedBit(Bit bit) {
		if (isGlobal()) {
			throw new UnsupportedOperationException(
					"setInvertedBit of a global");
		}

		if (bit.invertedBit == null) {
			invertedBit = bit;
		}
		owner = bit.owner;
	}

	/**
	 * Gets the string value of this bit ("C", "c", "0", "1", or "x").
	 */
	@Override
	public String toString() {
		if (isCare()) {
			if (isConstant()) {
				return isOn() ? "1" : "0";
			} else {
				return this == Bit.CARE ? "C" : "c";
			}
		} else {
			return "x";
		}
	}
}
