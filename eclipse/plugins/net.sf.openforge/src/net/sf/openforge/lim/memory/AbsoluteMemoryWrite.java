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
import net.sf.openforge.lim.Port;
import net.sf.openforge.lim.Visitor;
import net.sf.openforge.lim.op.Constant;

/**
 * AbsoluteMemoryWrite is a fixed access to a {@link LogicalMemory}, in which
 * the {@link Location} being accessed is fully specified at compile time and
 * does not depend on a base or offset address. This module is populated with a
 * {@link MemoryWrite} and 2 constants. The first identifies the address being
 * writen and is a DeferredConstant based on the particular {@link Allocation}
 * being accessed. The second is a fixed constant, indicating the number of
 * bytes being accessed.
 * 
 * <p>
 * Created: Fri Feb 28 13:34:42 2003
 * 
 * @author imiller, last modified by $Author: imiller $
 * @version $Id: AbsoluteMemoryWrite.java 556 2008-03-14 02:27:40Z imiller $
 */
public class AbsoluteMemoryWrite extends MemoryAccessBlock {

	/** Data port for write data */
	private Port valuePort;

	/** Constant supplying address to memory write. */
	private LocationConstant addrConst;

	/**
	 * Constructs a write access which targets the entire Allocation
	 * 
	 * @param access
	 *            a value of type 'MemoryWrite'
	 * @param target
	 *            a value of type 'Allocation'
	 * @param maxAddressWidth
	 *            the pre-optimized number of bits in the address bus
	 */
	public AbsoluteMemoryWrite(Location target, int maxAddressWidth,
			boolean isSigned) {
		this(target, target.getAddressableSize(), maxAddressWidth, isSigned);
	}

	/**
	 * Constructs a write access which retrieves the specified number of bytes
	 * from the given Allocation.
	 * 
	 * @param memoryWrite
	 *            a value of type 'MemoryWrite'
	 * @param target
	 *            a value of type 'Allocation'
	 * @param addressableLocations
	 *            a value of type 'int', the number of addresses accessed
	 *            atomically by this operation.
	 * @param maxAddressWidth
	 *            the pre-optimized number of bits in the address bus
	 */
	public AbsoluteMemoryWrite(Location target, int addressableLocations,
			int maxAddressWidth, boolean isSigned) {
		// Use the absolute base location because the access may be
		// based on an index or other Location for which the direct
		// initial value may not be available. There is always a
		// valid initial value for the absolute base.
		this(new MemoryWrite(false, (addressableLocations * target
				.getAbsoluteBase().getInitialValue().getAddressStridePolicy()
				.getStride()), isSigned), target, addressableLocations,
				maxAddressWidth);
	}

	/**
	 * Constructs a write access which retrieves the specified number of bytes
	 * from the given Allocation.
	 * 
	 * @param memoryWrite
	 *            a value of type 'MemoryWrite'
	 * @param target
	 *            a value of type 'Allocation'
	 * @param addressableLocations
	 *            a value of type 'int', the number of addresses accessed
	 *            atomically by this operation.
	 * @param maxAddressWidth
	 *            the pre-optimized number of bits in the address bus
	 */
	private AbsoluteMemoryWrite(MemoryWrite memoryWrite, Location target,
			int addressableLocations, int maxAddressWidth) {
		super(memoryWrite, addressableLocations);

		// Create the size constant
		Constant sizeConst = StructuralMemory
				.encodeAccessCount(addressableLocations);
		insertComponent(sizeConst, 0);

		// Create the deferred (address) constant.
		addrConst = new LocationConstant(target, maxAddressWidth, target
				.getAbsoluteBase().getLogicalMemory().getAddressStridePolicy());
		insertComponent(addrConst, 0);

		setControlDependencies(false);

		getExit(Exit.DONE).getDoneBus().setUsed(true);

		/*
		 * Connect the MemoryWrite's data input to the value port's peer.
		 */
		valuePort = makeDataPort();
		final Bus valueBus = valuePort.getPeer();
		// valueBus.setBits(getDataWidth());

		// valueBus.setSize(addressableLocations * 8, false);

		final Entry writeEntry = memoryWrite.getEntries().get(0);
		writeEntry.addDependency(memoryWrite.getDataPort(), new DataDependency(
				valueBus));

		// Connect the constants.
		writeEntry.addDependency(memoryWrite.getSizePort(), new DataDependency(
				sizeConst.getValueBus()));
		writeEntry.addDependency(memoryWrite.getAddressPort(),
				new DataDependency(addrConst.getValueBus()));
	}

	/**
	 * Gets the input data {@link Port}.
	 */
	public Port getValuePort() {
		return valuePort;
	}

	/**
	 * Returns true
	 */
	@Override
	public boolean isWrite() {
		return true;
	}

	/**
	 * Returns the {@link LocationConstant} used to source the actual address to
	 * the memory.
	 */
	public LocationConstant getAddressConstant() {
		return addrConst;
	}

	/**
	 * Attempts to remove the given {@link Port} from this component.
	 * 
	 * @param port
	 *            the port to remove
	 * @return true if the port was removed.
	 */
	@Override
	public boolean removeDataPort(Port port) {
		final boolean isRemoved = super.removeDataPort(port);
		if (isRemoved && (port == getValuePort())) {
			valuePort = null;
		}
		return isRemoved;
	}

	/**
	 * Gets the low level write operation contined in this module.
	 */
	public MemoryWrite getMemoryWrite() {
		return (MemoryWrite) getMemoryAccess();
	}

	/**
	 * Accepts a {@link Visitor}.
	 * 
	 * @param visitor
	 *            the instance to be visited with
	 *            {@link Visitor#visit(AbsoluteMemoryWrite)}
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
	 * Clones this object. A new AbsoluteMemoryWrite is created using a copy of
	 * the {@link MemoryWrite}, including its {@link LogicalMemoryPort}, and the
	 * {@link Location} from the {@link AllocationConstant}. The clone is added
	 * as an access to the {@link LogicalMemoryPort}.
	 * 
	 * @return the clone
	 */
	@Override
	public Object clone() throws CloneNotSupportedException {
		/*
		 * final LocationConstant addrConst = getAddressConstant(); final
		 * Location location = addrConst.getTarget(); final AbsoluteMemoryWrite
		 * clone = new
		 * AbsoluteMemoryWrite((MemoryWrite)getMemoryWrite().clone(), location,
		 * getAccessLocationCount(),
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
		AbsoluteMemoryWrite clone = (AbsoluteMemoryWrite) super.clone();
		final LocationConstant addrConst = getAddressConstant();
		final Location location = addrConst.getTarget();
		final LogicalMemoryPort memPort = getLogicalMemoryPort();
		if (memPort != null) {
			memPort.addAccess(clone, location);
		}

		return clone;
	}

	@Override
	protected void cloneNotify(Module moduleClone,
			Map<Component, Component> cloneMap) {
		super.cloneNotify(moduleClone, cloneMap);
		AbsoluteMemoryWrite clone = (AbsoluteMemoryWrite) moduleClone;
		clone.valuePort = getPortClone(valuePort, cloneMap);
		clone.addrConst = (LocationConstant) cloneMap.get(addrConst);
	}

}// AbsoluteMemoryWrite
