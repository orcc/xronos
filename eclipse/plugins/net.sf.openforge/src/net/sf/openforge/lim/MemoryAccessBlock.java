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

import java.util.Map;

import net.sf.openforge.lim.io.BlockElement;
import net.sf.openforge.lim.memory.LValue;
import net.sf.openforge.lim.memory.LogicalMemoryPort;
import net.sf.openforge.lim.memory.MemoryAccess;

/**
 * MemoryAccessBlock is an abstract class which contains a {@link MemoryAccess}
 * and represents an access to memory as characterized by the memory access and
 * a fixed number of addressable locations accessed.
 * 
 * <p>
 * Created: Thu Feb 27 16:20:44 2003
 * 
 * @author imiller, last modified by $Author: imiller $
 * @version $Id: MemoryAccessBlock.java 70 2005-12-01 17:43:11Z imiller $
 */
public abstract class MemoryAccessBlock extends Block implements LValue {

	/** Memory access operation */
	private MemoryAccess memoryAccess;

	/** The number of addressable locations being accessed. */
	private int addressableCount;

	/**
	 * This BlockElement identifies the specific block of data on the IO
	 * interface that this LValue is associated with. This field will be null
	 * for the majority of LValues.
	 */
	private BlockElement ioBlockElement = null;

	public MemoryAccessBlock(MemoryAccess access, int addressableLocationCount) {
		super(false);
		memoryAccess = access;
		addressableCount = addressableLocationCount;
		insertComponent(memoryAccess, 0);
	}

	@Override
	public void accept(Visitor visitor) {
		visitor.visit(this);
	}

	/**
	 * Gets the low level operation that actually performs the memory access.
	 */
	public MemoryAccess getMemoryAccess() {
		return memoryAccess;
	}

	/**
	 * Gets the identity of the {@link LogicalMemoryPort} which is accessed by
	 * this component.
	 * 
	 * @return the logical memory port accessed by this component, or null if
	 *         there is none
	 */
	@Override
	public LogicalMemoryPort getLogicalMemoryPort() {
		return getMemoryAccess().getMemoryPort();
	}

	/**
	 * Sets the identity of the {@link LogicalMemoryPort} which is accessed by
	 * this component. Also calls
	 * {@link MemoryAccess#setMemoryPort(LogicalMemoryPort)} on the contained
	 * {@link MemoryAccess}.
	 * 
	 * @param logicalMemoryPort
	 *            the logical memory port accessed by this component, or null if
	 *            there is none
	 */
	@Override
	public void setLogicalMemoryPort(LogicalMemoryPort logicalMemoryPort) {
		getMemoryAccess().setMemoryPort(logicalMemoryPort);
	}

	/**
	 * Remove the underlying {@link MemoryAccess} as a reference of the
	 * targetted memory.
	 */
	public void removeFromMemory() {
		final LogicalMemoryPort memoryPort = getLogicalMemoryPort();
		memoryPort.getLogicalMemory().removeAccessor(this);
	}

	/**
	 * Returns the number of addressable locations accessed by this memory
	 * access.
	 */
	@Override
	public int getAccessLocationCount() {
		return addressableCount;
	}

	/**
	 * Returns true if this access is a write.
	 */
	@Override
	public abstract boolean isWrite();

	/**
	 * Sets the specific {@link BlockElement} that this LValue is associated
	 * with, and further indicates that this LValue is part of the interface
	 * wrapper code.
	 * 
	 * @param element
	 *            a BlockElement, may be null
	 */
	@Override
	public void setBlockElement(BlockElement element) {
		ioBlockElement = element;
	}

	/**
	 * Retrieves the {@link BlockElement} associated with this LValue or null if
	 * none has been defined. A non null value means that this LValue is part of
	 * the interface wrapper logic for the design.
	 * 
	 * @return a BlockElement, may be null
	 */
	@Override
	public BlockElement getBlockElement() {
		return ioBlockElement;
	}

	/**
	 * Returns true if this memory access is an array access. returns false.
	 */
	public boolean isArrayAccess() {
		return false;
	}

	@Override
	protected void cloneNotify(Module moduleClone,
			Map<Component, Component> cloneMap) {
		super.cloneNotify(moduleClone, cloneMap);
		MemoryAccessBlock clone = (MemoryAccessBlock) moduleClone;
		clone.memoryAccess = (MemoryAccess) cloneMap.get(memoryAccess);
		clone.memoryAccess.setMemoryPort(getLogicalMemoryPort());
		// Nothing to do for the ioBlockElement since we want the
		// clone to be associated with the same element.
	}

}// MemoryAccessBlock
