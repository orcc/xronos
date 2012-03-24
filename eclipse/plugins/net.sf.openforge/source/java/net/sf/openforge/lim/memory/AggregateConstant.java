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

import net.sf.openforge.lim.Value;
import net.sf.openforge.lim.op.Constant;

/**
 * AggregateConstant is a {@link Constant} that represents a concatentation of
 * other Constants, typically derived from a {@link Record}.
 * 
 * @author abk, last modified by $Author: imiller $
 * @version $Id: AggregateConstant.java 568 2008-03-31 17:23:31Z imiller $
 */
public class AggregateConstant extends MemoryConstant {

	/** The constituent constants. */
	private List<Constant> aggregatedConstants;

	/**
	 * Creates a new AggregateConstant from a List of Constant objects.
	 * 
	 * @param constants
	 *            the list of Constants
	 * @param width
	 *            the total bitwidth of the constants
	 */
	public AggregateConstant(List<Constant> constants, int width) {
		super(width, false);

		aggregatedConstants = new ArrayList<Constant>(constants);

		pushValuesForward();
	}

	/**
	 * Gets the list of Constants which comprise this AggregateConstant.
	 * 
	 * @return a List of Constants
	 */
	public List<Constant> getAggregatedConstants() {
		return aggregatedConstants;
	}

	/**
	 * Returns the ordered and flattened list of constants that make up the
	 * value generating constants for this aggregate.
	 * 
	 * @return a List of Constants
	 */
	@Override
	public List<Constant> getConstituents() {
		List<Constant> flat = new ArrayList<Constant>();
		for (Constant c : getAggregatedConstants()) {
			flat.addAll(c.getConstituents());
		}
		return Collections.unmodifiableList(flat);
	}

	/**
	 * Returns a non modifiable set containing this object as well as the
	 * contents of every constant being aggregated.
	 * 
	 * @return a Set of Constant objects.
	 */
	@Override
	public Set<Constant> getContents() {
		Set<Constant> contents = new HashSet<Constant>();
		contents.add(this);
		for (Constant c : getAggregatedConstants()) {
			contents.addAll(c.getContents());
		}
		return Collections.unmodifiableSet(contents);
	}

	/**
	 * Returns a bundle of AURep objects that define the numerical value (and
	 * locked state) of each unit in this constant as those units were laid out
	 * in memory. Thus this representation follows the endianness of the
	 * compilation.
	 * 
	 * @return a non null AURepBundle
	 */
	@Override
	public AURepBundle getRepBundle() {
		return getRepBundle(false);
	}

	public AURepBundle getRepBundle(boolean swapEndian) {
		List<AddressableUnit> rep = new ArrayList<AddressableUnit>();
		int size = -1;
		for (Constant memConst : getConstituents()) {
			AURepBundle repBundle = memConst.getRepBundle();

			if (size < 0)
				size = repBundle.getBitsPerUnit();
			if (repBundle.getBitsPerUnit() != size)
				throw new UnsupportedOperationException(
						"Unexpected aggregated constant structure.  Mismatched addressable sizes.  Expected "
								+ size + " found " + repBundle.getBitsPerUnit());

			AddressableUnit cRep[] = repBundle.getRep();
			if (swapEndian) {
				cRep = swapEndian(cRep);
			}
			for (int i = 0; i < cRep.length; i++) {
				rep.add(cRep[i]);
			}
		}
		final AddressableUnit flatRep[] = new AddressableUnit[rep.size()];
		for (int i = 0; i < rep.size(); i++) {
			flatRep[i] = rep.get(i);
		}
		AURepBundle flat = new AURepBundle(flatRep, size);
		return flat;
	}

	@Override
	public boolean pushValuesForward() {
		boolean mod = false;

		// Causes all the LEAF constants to be endian swapped back to
		// little endian representation for pushing out as a
		// constant. Remember that all values as data flows are
		// little endian (because of how the pushValue works).
		AURepBundle thisRep = getRepBundle(isBigEndian());

		Value oldValue = getValueBus().getValue();

		Value newValue = pushValue(oldValue.getSize(), oldValue.isSigned(),
				thisRep);

		mod |= getValueBus().pushValueForward(newValue);
		return mod;
	}

}// AggregateConstant
