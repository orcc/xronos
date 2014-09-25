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

package org.xronos.openforge.lim.memory;

import java.math.BigInteger;

/**
 * AddressableUnit defines a contiguous chunk of memory that is accessable via
 * exactly one address. It is a non-divisible memory allocation unit whose size
 * is defined by the current AddressStridePolicy and whose value is maintained
 * by this class.
 * 
 * <p>
 * Created: Tue Nov 22 11:40:18 2005
 * 
 * @author imiller, last modified by $Author: imiller $
 * @version $Id: AddressableUnit.java 568 2008-03-31 17:23:31Z imiller $
 */
public class AddressableUnit {

	public static final AddressableUnit ZERO_UNIT = new AddressableUnit(
			BigInteger.ZERO);

	/**
	 * Builds a composite BigInteger from the array of addressable units where
	 * the address stride policy is used to determine how many bits are used
	 * from each AddressableUnit in the array.
	 */
	public static BigInteger getCompositeValue(AddressableUnit[] units,
			AddressStridePolicy policy) {
	
		if (units.length == 1) {
			if (units[0].initValue.equals(BigInteger.ZERO)) {
				return BigInteger.ZERO;
			}
		}
		
		BigInteger composite = BigInteger.ZERO;
		BigInteger mask = BigInteger.ONE.shiftLeft(policy.getStride() + 1)
				.subtract(BigInteger.ONE);

		for (int i = 0; i < units.length; i++) {
			BigInteger value = units[i].getValue();
			value = value.and(mask);
			composite = composite.or(value.shiftLeft(policy.getStride() * i));
		}

		// Now ensure the sign is correct. It is the most significant
		// bit we have set.
		int numBits = units.length * policy.getStride();
		if (composite.testBit(numBits - 1)) {
			mask = BigInteger.ZERO;
			for (int i = 0; i < numBits; i++) {
				mask = mask.shiftLeft(1).or(BigInteger.ONE);
			}
			BigInteger fixed = BigInteger.ZERO.subtract(BigInteger.ONE);
			fixed = fixed.andNot(mask); // clear the lowest bits
			fixed = fixed.or(composite);
			composite = fixed;
		}

		return composite;
	}

	private BigInteger initValue;

	private boolean locked = true;

	public AddressableUnit(BigInteger initValue) {
		this(initValue, true);
	}

	/**
	 * Constructs a new AddressableUnit whose value is the specified BigInteger
	 * and whose size is determined by the specified policy.
	 */
	public AddressableUnit(BigInteger initValue, boolean locked) {
		this.initValue = initValue;
		this.locked = locked;
	}

	public AddressableUnit(long initValue) {
		this(initValue, true);
	}

	/**
	 * Constructs a new AddressableUnit whose value is the specified long
	 */
	public AddressableUnit(long initValue, boolean locked) {
		this(BigInteger.valueOf(initValue), locked);
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof AddressableUnit)) {
			return false;
		}
		AddressableUnit target = (AddressableUnit) o;

		return initValue.equals(target.initValue)
				&& isLocked() == target.isLocked();
	}

	/**
	 * Returns 1 if the specified bit is non-zero, 0 otherwise.
	 */
	public int getBit(int position) {
		return getValue().testBit(position) ? 1 : 0;
	}

	/**
	 * Returns a BigInteger representation of the value of this AddressableUnit.
	 */
	public BigInteger getValue() {
		if (!isLocked()) {
			throw new UnsupportedOperationException(
					"Cannot access the value of an unlocked addressable unit");
		}
		return initValue;
	}

	@Override
	public int hashCode() {
		return initValue.hashCode() + (isLocked() ? 0 : 1);
	}

	public boolean isLocked() {
		return locked;
	}

	@Override
	public String toString() {
		return "AU:" + getValue();
	}

} // AddressableUnit
