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
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.xronos.openforge.app.EngineThread;
import org.xronos.openforge.lim.op.Constant;

/**
 * ScalarConstant is a MemoryConstant whose value is a simple scalar value,
 * created as an ordered sequence of bytes. The value of this constant is
 * immutable and fixed at creation.
 * 
 * <p>
 * Created: Wed Apr 21 09:33:21 2004
 * 
 * @author imiller, last modified by $Author: imiller $
 * @version $Id: ScalarConstant.java 568 2008-03-31 17:23:31Z imiller $
 */
public class ScalarConstant extends MemoryConstant {

	/** The bundle of immutable AddressableUnit objects for this constant. */
	final AURepBundle rep;

	/**
	 * Creates a new ScalarConstant whose value is fixed at creation.
	 * 
	 * @param bytes
	 *            an array of bytes ordered from least significant address to
	 *            most significant that defines the numerical value of this
	 *            constant.
	 * @param width
	 *            the width, in bits, of this constant
	 * @param isSigned
	 *            true if this is a signed constant, false otherwise.
	 */
	// public ScalarConstant (byte[] bytes, int width, boolean isSigned)
	public ScalarConstant(AddressableUnit[] bytes, int width, boolean isSigned,
			AddressStridePolicy policy) {
		super(width, isSigned);

		final int units = (int) Math.ceil(width * 1.0 / policy.getStride());
		if (units != bytes.length) {
			EngineThread
					.getGenericJob()
					.warn("Constant specified with incorrect number of units.  Endianness of platform may cause discrepancies. Required: "
							+ units + " actual: " + bytes.length);
		}

		final AddressableUnit aurep[] = new AddressableUnit[units];
		for (int i = 0; i < units; i++) {
			aurep[i] = new AddressableUnit(bytes[i].getValue().intValue(), true);
		}

		// Mask the top value to the specified number of bits
		int topBits = width % policy.getStride();
		if (topBits == 0) {
			topBits = policy.getStride();
		}
		BigInteger mask = BigInteger.ZERO;
		for (int i = 0; i < topBits; i++) {
			mask = mask.shiftLeft(1).or(BigInteger.ONE);
		}
		aurep[aurep.length - 1] = new AddressableUnit(bytes[aurep.length - 1]
				.getValue().and(mask).intValue(), true);

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
	public List<Constant> getConstituents() {
		return Collections.unmodifiableList(Collections
				.singletonList((Constant) this));
	}

	/**
	 * Returns a single element non modifiable set containing only this object.
	 * 
	 * @return a singleton Set containing this object.
	 */
	@Override
	public Set<Constant> getContents() {
		return Collections.unmodifiableSet(Collections
				.singleton((Constant) this));
	}

	/**
	 * Returns an array of ByteRep objects which specifies the exact byte layout
	 * from memory. All bytes are specified as fully locked.
	 */
	@Override
	public AURepBundle getRepBundle() {
		return rep;
	}

}// ScalarConstant
