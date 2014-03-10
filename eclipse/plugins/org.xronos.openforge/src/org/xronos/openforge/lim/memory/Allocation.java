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

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.xronos.openforge.lim.io.BlockElement;

/**
 * An Allocation is a region of memory that has been allocated by the
 * declaration of a variable in the user application. It is defined by the
 * initial {@link LogicalValue value} of the defined variable.
 * 
 * @version $Id: Allocation.java 70 2005-12-01 17:43:11Z imiller $
 */
public class Allocation extends Variable implements MemoryVisitable {

	/** The initialization value of this memory allocation */
	private LogicalValue initialValue;

	/**
	 * The set of BlockElement objects which identify io streams that make use
	 * of this Allocation. Note that there may be multiple because an allocation
	 * may be filled by an input stream and drained by an output stream
	 */
	private Set<BlockElement> blockElements;

	/**
	 * Constructs a new Allocation.
	 * 
	 * @param LogicalMemory
	 *            the memory to which this Allocation belongs
	 * @param initialValue
	 *            the initialization value for the allocated region of memory;
	 *            the size of the region will be the the size of this value
	 * @throws NullPointerException
	 *             if <code>initialValue</code> or <code>logicalMemory</code> is
	 *             null
	 */
	Allocation(LogicalMemory logicalMemory, LogicalValue initialValue) {
		super(logicalMemory, initialValue.getSize());
		this.initialValue = initialValue;
		this.blockElements = Collections.emptySet();
	}

	/**
	 * Implementation of the MemoryVisitable interface.
	 * 
	 * @param memVis
	 *            a non null 'MemoryVisitor'
	 * @throws NullPointerException
	 *             if memVis is null
	 */
	@Override
	public void accept(MemoryVisitor memVis) {
		memVis.visit(this);
	}

	/**
	 * Adds the given {@link BlockElement} to this Allocation, indicating that
	 * this Allocation is part of the backing memory for the interface transfer
	 * denoted by that BlockElement.
	 * 
	 * @param element
	 *            a {@link BlockElement}, non-null
	 * @throws IllegalArgumentException
	 *             if element is null
	 */
	public void addBlockElement(BlockElement element) {
		if (element == null) {
			throw new IllegalArgumentException(
					"Cannot add a null io element to a memory allocation");
		}

		if (blockElements == Collections.EMPTY_SET) {
			blockElements = new HashSet<BlockElement>();
		}

		blockElements.add(element);
	}

	/**
	 * Copies the Set of {@link BlockElements} from the given Location, which
	 * must be an Allocation (protected with an assert).
	 * 
	 * @param loc
	 *            a 'Location' which must be an Allocation
	 */
	public void copyBlockElements(Location loc) {
		assert loc instanceof Allocation;
		if (!(loc instanceof Allocation)) {
			return;
		}

		Allocation template = (Allocation) loc;
		for (BlockElement blockElement : template.getBlockElements()) {
			addBlockElement(blockElement);
		}
	}

	@Override
	public String debug() {
		return toString().replaceAll("net.sf.openforge.", "") + " <"
				+ getInitialValue().toString() + ">";
	}

	/**
	 * Creates and returns a <code>Location</code> that is a duplicate of this
	 * one using a given <code>Location</code> as a base.
	 * 
	 * @param baseLocation
	 *            the base for the duplicate location
	 * @return the duplicated location
	 */
	@Override
	public Location duplicateForBaseLocation(Location baseLocation) {
		return baseLocation;
	}

	/**
	 * Returns the set of {@link BlockElement} objects that have been associated
	 * with this Allocation, or the empty set if this Allocation is not related
	 * to the block IO interface of the design.
	 * 
	 * @return an unmodifiable Set of {@link BlockElement} objects, which may be
	 *         the empty set.
	 */
	public Set<BlockElement> getBlockElements() {
		return Collections.unmodifiableSet(blockElements);
	}

	/**
	 * Gets the initial value.
	 * 
	 * @return the value that was used to initialize this memory location
	 */
	@Override
	public LogicalValue getInitialValue() {
		return initialValue;
	}

	/**
	 * Removes the specified {@link BlockElement} from this Allocation,
	 * indicating that this Allocation is no longer the backing memory for that
	 * BlockElement (or that the element has been removed from all interfaces
	 * and is no longer needed)
	 * 
	 * @param element
	 *            a non null BlockElement
	 * @throws IllegalArgumentException
	 *             if element is null or not contained by this Allocation.
	 */
	public void removeBlockElement(BlockElement element) {
		if (element == null) {
			throw new IllegalArgumentException("Null element cannot be removed");
		}
		// Do the remove and test that it worked.
		if (!blockElements.remove(element)) {
			throw new IllegalArgumentException(
					"Unknown element was not removed from memory allocation");
		}
	}

	@Override
	public String toString() {
		return "[Allocation@" + Integer.toHexString(hashCode()) + "="
				+ initialValue + "]";
	}
}
