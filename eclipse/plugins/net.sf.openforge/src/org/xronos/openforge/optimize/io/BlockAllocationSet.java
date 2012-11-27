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

package org.xronos.openforge.optimize.io;

import java.util.HashSet;
import java.util.Set;

import org.xronos.openforge.lim.io.BlockElement;
import org.xronos.openforge.lim.memory.Allocation;
import org.xronos.openforge.lim.memory.LValue;
import org.xronos.openforge.lim.memory.Location;
import org.xronos.openforge.lim.memory.LogicalMemory;


/**
 * BlockAllocationSet groups together all the Allocations which share a common
 * BlockElement. By grouping the Allocations related to a single
 * {@link BlockElement} together prior to analysis, we can ensure that we are
 * analyzing an entire parameter or return element and not just a sub-section of
 * that element as factored out into a seperate memory. This allows us to (for
 * example) remove an entire block element from an interface without fear that
 * we are removing simply 'n' bytes from the middle of that element.
 * <p>
 * This class is really just a glorified Set in which all contained elements are
 * {@link Allocation} objects which share a common object contained in the set
 * returned by {@link Allocation#getBlockElements}
 * 
 * <p>
 * Created: Thu Feb 26 06:32:45 2004
 * 
 * @author imiller, last modified by $Author: imiller $
 * @version $Id: BlockAllocationSet.java 2 2005-06-09 20:00:48Z imiller $
 */
public class BlockAllocationSet {

	/**
	 * The BlockElement that is common to all Allocations in this set.
	 */
	private BlockElement baseBlockElement;

	/**
	 * The set of Allocation objects, all of which contain the base BlockElement
	 * for this class. Protected for testing.
	 */
	protected Set<Allocation> allocations = new HashSet<Allocation>();

	/**
	 * Create a new BlockAllocationSet with a specific {@link BlockElement} that
	 * is shared by all Allocations in this Set.
	 * 
	 * @param element
	 *            a value of type {@link BlockElement}
	 */
	public BlockAllocationSet(BlockElement element) {
		assert element != null;
		baseBlockElement = element;
	}

	/**
	 * Returns the {@link BlockElement} that this set is based upon.
	 * 
	 * @return a non null BlockElement
	 */
	public BlockElement getBlockElement() {
		return baseBlockElement;
	}

	/**
	 * Adds the specified Allocation to this Set.
	 * 
	 * @param alloc
	 *            a non-null 'Allocation'
	 * @throws IllegalArgumentException
	 *             if alloc is null
	 * @throws IllegalArgumentException
	 *             if alloc has a different block element than the others in
	 *             this Set.
	 */
	public void add(Allocation alloc) {
		if (alloc == null) {
			throw new IllegalArgumentException("Null allocation");
		}
		if (!alloc.getBlockElements().contains(baseBlockElement)) {
			throw new IllegalArgumentException(
					"Allocation does not target base block element");
		}
		allocations.add(alloc);
	}

	/**
	 * Returns true if there are no LValue write accesses to any Allocation in
	 * this set, unless that LValue write access returns non null for
	 * {@link LValue#getBlockElement}. This means that the only write access to
	 * any {@link Allocation} in this Set comes from the 'load' functionality of
	 * the block IO interface to this design. (ie the algorithmic portion of the
	 * design contains only read accesses to all Allocations associated with a
	 * specific parameter).
	 * 
	 * @return true if the {@link BlockElement} for all Allocations in this Set
	 *         is an input parameter which is not modified.
	 */
	public boolean isInternalReadOnly() {
		return testReadWriteInternal(true);
	}

	/**
	 * Returns true if there are no LValue read accesses to any Allocation in
	 * this set, unless that LValue read access returns non null for
	 * {@link LValue#getBlockElement}. This means that the only read access to
	 * any {@link Allocation} in this Set comes from the 'unload' functionality
	 * of the block IO interface to this design. (ie the algorithmic portion of
	 * the design contains only write accesses to all Allocations associated
	 * with a specific parameter).
	 * 
	 * @return true if the {@link BlockElement} for all Allocations in this Set
	 *         is an output parameter which is never read.
	 */
	public boolean isInternalWriteOnly() {
		return testReadWriteInternal(false);
	}

	/**
	 * Tests whether EVERY byte in each Allocation is written before it is read
	 * (or is non-accessed) exclusive of any LValue access that reports
	 * {@link LValue#getBlockElement} != null.
	 * 
	 * @return true if there is no byte in any Allocation in this set that is
	 *         potentially read before it is written to by the algorithmic
	 *         portion of the design.
	 */
	public boolean isInternalWriteFirst() {
		// TBD block elimination
		return false;
	}

	/**
	 * This method builds up a Set of all {@link LValue} accesses that the
	 * target memory identifies as targetting any Allocation in this set.
	 * 
	 * @return a Set of LValue objects.
	 */
	public Set<LValue> getAccessingLValues() {
		final Set<LValue> lvalues = new HashSet<LValue>();
		for (Allocation alloc : allocations) {
			final LogicalMemory mem = alloc.getLogicalMemory();
			for (LValue lvalue : mem.getLValues()) {
				for (Location loc : mem.getAccesses(lvalue)) {
					if (allocations.contains(loc.getAbsoluteBase())) {
						lvalues.add(lvalue);
					}
				}
			}
		}

		return lvalues;
	}

	/**
	 * This method performs two services to <i>remove</i> the
	 * {@link BlockElement} upon which this set was built. It clears the
	 * BlockElement's streamFormat array by deleting all bytes from it and also
	 * removes the BlockElement from each Allocation in this set.
	 */
	public void deleteElement() {
		final BlockElement element = getBlockElement();
		element.deleteStreamBytes(0, element.getStreamFormat().length);
		for (Allocation alloc : allocations) {
			alloc.removeBlockElement(element);
		}
	}

	/**
	 * This method determines whether the algorithmic portion of the design is
	 * either read only or write only to the allocations in this set. It
	 * accomplishes this by looking at each Allocation in this set, then finding
	 * all LValues to that Allocation. Finally it throws out any LValues that
	 * are part of the block IO or that are reads (if checking read only) or
	 * writes (if checking write only). If any LValues remain, then that
	 * Allocation is not read only or write only and we return false.
	 * 
	 * @param testInternalReads
	 *            set to true to test is all the Allocations in this set are
	 *            read only.
	 * @return true if testInternalReads is set and all LValues to the
	 *         allocations in this set are reads (excluding those LValues with a
	 *         block element set). Also returns true if testInternalReads is
	 *         false and all LValues to the allocations in this set are writes
	 *         (excluding those LValues with a block element set).
	 */
	private boolean testReadWriteInternal(boolean testInternalReads) {
		for (LValue lvalue : getAccessingLValues()) {

			if (lvalue.getBlockElement() != null) {
				// Ignore any lvalue that is a part of the IO
				// Interface logic. We're interested in the
				// behavior of just the core algorithm
				continue;
			}
			if ((!lvalue.isWrite() && testInternalReads)
					|| (lvalue.isWrite() && !testInternalReads)) {
				// Ignore any read accesses if testInternalReads
				// is set, otherwise ignore any write accesses.
				continue;
			}

			return false;
		}

		return true;
	}

}// BlockAllocationSet
