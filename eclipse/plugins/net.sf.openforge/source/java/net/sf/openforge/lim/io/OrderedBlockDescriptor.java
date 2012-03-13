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

package net.sf.openforge.lim.io;

import java.util.ArrayList;
import java.util.List;

/**
 * OrderedBlockDescriptor is an abstract class which specifies specific
 * functionality for the {@link BlockDescriptor#getBlockOrganization} method.
 * This class specifies that each {@link BlockElement} contained within this
 * BlockDescriptor will be transferred on the interface to its completion before
 * the start of the next block element. Thus, if there are 4 block elements in
 * this descriptor, returned by {@link BlockDescriptor#getBlockElements} as
 * [A,B,C,D], then all elements of A will be transmitted on the interface,
 * followed by all elements of B, then all of C and finally all of D.
 * 
 * <p>
 * Created: Thu Feb 26 07:55:55 2004
 * 
 * @author imiller, last modified by $Author: imiller $
 * @version $Id: OrderedBlockDescriptor.java 2 2005-06-09 20:00:48Z imiller $
 */
public abstract class OrderedBlockDescriptor extends BlockDescriptor {

	public OrderedBlockDescriptor() {
	}

	/**
	 * Returns the organization of the elements described in this descriptor.
	 * The ordering will be [0,...,1,...,2,...] where the numerical value is the
	 * index into the array returned by {@link BlockDescriptor#getBlockElements}
	 * .
	 * 
	 * @return an int[], defining what BlockElement has control of the interface
	 *         on each cycle. The index of this array denotes a specific cycle
	 *         of one complete transfer, the value denotes which BlockElement is
	 *         transferred on that cycle.
	 */
	public int[] getBlockOrganization() {
		// Derive from the list of elements. This code assumes that
		// we transfer all of each block contiguously. ie, all of
		// block 0 then all of block 1, etc.
		List<Integer> blocks = new ArrayList<Integer> ();
		BlockElement elements[] = getBlockElements();
		for (int count = 0; count < elements.length; count++) {
			BlockElement element = elements[count];
			// Cache up the number of cycles because the
			// 'getNumberOfCycles' method regenerates a lot of data to
			// determine the number and is thus very slow.
			final int cycles = element.getNumberOfCycles();
			for (int i = 0; i < cycles; i++) {
				blocks.add(new Integer(count));
			}
		}
		int org[] = new int[blocks.size()];
		for (int i = 0; i < blocks.size(); i++) {
			org[i] = ((Integer) blocks.get(i)).intValue();
		}
		return org;
	}

}// OrderedBlockDescriptor
