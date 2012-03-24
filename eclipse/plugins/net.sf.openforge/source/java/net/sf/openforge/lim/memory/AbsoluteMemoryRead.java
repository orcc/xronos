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

import java.util.Map;

import net.sf.openforge.lim.Bus;
import net.sf.openforge.lim.Component;
import net.sf.openforge.lim.DataDependency;
import net.sf.openforge.lim.Entry;
import net.sf.openforge.lim.Exit;
import net.sf.openforge.lim.MemoryAccessBlock;
import net.sf.openforge.lim.Module;
import net.sf.openforge.lim.OutBuf;
import net.sf.openforge.lim.Visitor;
import net.sf.openforge.lim.op.Constant;

/**
 * AbsoluteMemoryRead is a fixed access to a {@link LogicalMemory}, in which the
 * {@link Location} being accessed is fully specified at compile time and does
 * not depend on a base or offset address. This module is populated with a
 * {@link MemoryRead} and 2 constants. The first identifies the address being
 * read and is a DeferredConstant based on the particular {@link Allocation}
 * being accessed. The second is a fixed constant, indicating the number of
 * bytes being accessed.
 * 
 * <p>
 * Created: Fri Feb 28 13:34:42 2003
 * 
 * @author imiller, last modified by $Author: imiller $
 * @version $Id: AbsoluteMemoryRead.java 556 2008-03-14 02:27:40Z imiller $
 */
public class AbsoluteMemoryRead extends MemoryAccessBlock {

	/** Data bus for result of read */
	private Bus resultBus;

	/** Constant supplying address to memory read. */
	private LocationConstant addrConst;

	/**
	 * Constructs a read access which retrieves the entire Allocation
	 * 
	 * @param location
	 *            a value of type 'Allocation'
	 * @param maxAddressWidth
	 *            the pre-optimized number of bits in the address bus
	 */
	public AbsoluteMemoryRead(Location location, int maxAddressWidth,
			boolean isSigned) {
		this(location, location.getAddressableSize(), maxAddressWidth, isSigned);
	}

	/**
	 * Constructs a read access which retrieves the specified number of bytes
	 * from the given Allocation.
	 * 
	 * @param target
	 *            a value of type 'Allocation'
	 * @param addressableLocations
	 *            a value of type 'int'
	 * @param maxAddressWidth
	 *            the pre-optimized number of bits in the address bus
	 */
	public AbsoluteMemoryRead(Location location, int addressableLocations,
			int maxAddressWidth, boolean isSigned) {
		// Use the absolute base location because the access may be
		// based on an index or other Location for which the direct
		// initial value may not be available. There is always a
		// valid initial value for the absolute base.
		this(new MemoryRead(false, (addressableLocations * location
				.getAbsoluteBase().getInitialValue().getAddressStridePolicy()
				.getStride()), isSigned), location, addressableLocations,
				maxAddressWidth);
	}

	/**
	 * Constructs a read access which retrieves the specified number of bytes
	 * from the given Allocation.
	 * 
	 * @param memoryRead
	 *            a value of type 'MemoryRead'
	 * @param location
	 *            a value of type 'Allocation'
	 * @param addressableLocations
	 *            a value of type 'int'
	 * @param maxAddressWidth
	 *            the pre-optimized number of bits in the address bus
	 */
	private AbsoluteMemoryRead(MemoryRead memoryRead, Location location,
			int addressableLocations, int maxAddressWidth) {
		super(memoryRead, addressableLocations);

		// Create the size constant
		Constant sizeConst = StructuralMemory
				.encodeAccessCount(addressableLocations);
		insertComponent(sizeConst, 0);

		// Create the deferred (address) constant.
		addrConst = new LocationConstant(location, maxAddressWidth, location
				.getAbsoluteBase().getLogicalMemory().getAddressStridePolicy());
		insertComponent(addrConst, 0);

		setControlDependencies(false);

		Exit exit = getExit(Exit.DONE);
		exit.getDoneBus().setUsed(true);
		resultBus = exit.makeDataBus();

		/*
		 * Connect the MemoryRead's data output to the output of this module.
		 */
		final OutBuf ob = exit.getPeer();
		final Entry obEntry = ob.getEntries().get(0);
		resultBus.setSize(memoryRead.getWidth(), memoryRead.isSigned());
		obEntry.addDependency(resultBus.getPeer(), new DataDependency(
				memoryRead.getResultBus()));

		final Entry memReadEntry = memoryRead.getEntries().get(0);
		// Connect up the constants.
		memReadEntry.addDependency(memoryRead.getSizePort(),
				new DataDependency(sizeConst.getValueBus()));
		memoryRead.getSizePort().setBus(sizeConst.getValueBus());
		memReadEntry.addDependency(memoryRead.getAddressPort(),
				new DataDependency(addrConst.getValueBus()));
		memoryRead.getAddressPort().setBus(addrConst.getValueBus());
	}

	/**
	 * Gets the data {@link Bus}.
	 */
	public Bus getResultBus() {
		return resultBus;
	}

	/**
	 * Returns the {@link LocationConstant} used to source the actual address to
	 * the memory.
	 */
	public LocationConstant getAddressConstant() {
		return addrConst;
	}

	/**
	 * Attempts to remove the given {@link Bus} from this component.
	 * 
	 * @param bus
	 *            the bus to remove
	 * @return true if the bus was removed.
	 */
	@Override
	public boolean removeDataBus(Bus bus) {
		final boolean isRemoved = super.removeDataBus(bus);
		if (isRemoved && (bus == resultBus)) {
			resultBus = null;
		}
		return isRemoved;
	}

	/**
	 * Returns false
	 */
	@Override
	public boolean isWrite() {
		return false;
	}

	/**
	 * Gets the low level read operation contined in this module.
	 */
	public MemoryRead getMemoryRead() {
		return (MemoryRead) getMemoryAccess();
	}

	/**
	 * Accepts a {@link Visitor}.
	 * 
	 * @param visitor
	 *            the instance to be visited with
	 *            {@link Visitor#visit(AbsoluteMemoryRead)}
	 */
	@Override
	public void accept(Visitor visitor) {
		visitor.visit(this);
	}

	/**
	 * Remove the underlying {@link MemoryAccess} as a reference of the
	 * targetted memory.
	 */
	@Override
	public void removeFromMemory() {
		super.removeFromMemory();
		getLogicalMemoryPort().getLogicalMemory().removeLocationConstant(
				addrConst);
	}

	@Override
	public boolean removeComponent(Component component) {
		boolean ret = super.removeComponent(component);
		if (component == addrConst)
			addrConst = null;
		return ret;
	}

	/**
	 * Clones this object. A new AbsoluteMemoryRead is created using a copy of
	 * the {@link MemoryRead}, including its {@link LogicalMemoryPort}, and the
	 * {@link Location} from the {@link AllocationConstant}. The clone is added
	 * as an access to the {@link LogicalMemoryPort}.
	 * 
	 * @return the clone
	 */
	@Override
	public Object clone() throws CloneNotSupportedException {
		/*
		 * final LocationConstant addrConst = getAddressConstant(); final
		 * Location location = addrConst.getTarget(); final AbsoluteMemoryRead
		 * clone = new AbsoluteMemoryRead((MemoryRead)getMemoryRead().clone(),
		 * location, getAccessLocationCount(),
		 * getAddressConstant().getValueBus().getValue().getSize()); final
		 * LogicalMemoryPort memPort = getLogicalMemoryPort();
		 * clone.setBlockElement(this.getBlockElement()); if (memPort != null) {
		 * memPort.addAccess(clone, location); // Cloning should not delete the
		 * location constant of this // access from the memory! // if
		 * (memPort.getLogicalMemory
		 * ().getLocationConstants().contains(addrConst)) // { //
		 * memPort.getLogicalMemory().removeLocationConstant(addrConst); // } }
		 * this.copyComponentAttributes(clone); return clone;
		 */
		AbsoluteMemoryRead clone = (AbsoluteMemoryRead) super.clone();
		final LocationConstant addrConst = getAddressConstant();
		final Location location = addrConst.getTarget();
		final LogicalMemoryPort memPort = getLogicalMemoryPort();
		if (memPort != null) {
			memPort.addAccess(clone, location);
		}

		return clone;
	}

	@Override
	protected void cloneNotify(Module moduleClone, Map cloneMap) {
		super.cloneNotify(moduleClone, cloneMap);
		AbsoluteMemoryRead clone = (AbsoluteMemoryRead) moduleClone;
		clone.resultBus = getBusClone(resultBus, cloneMap);
		clone.addrConst = (LocationConstant) cloneMap.get(addrConst);
	}

}// AbsoluteMemoryRead
