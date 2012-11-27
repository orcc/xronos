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

import java.util.Map;

import org.xronos.openforge.lim.memory.AddressStridePolicy;
import org.xronos.openforge.lim.memory.Location;
import org.xronos.openforge.lim.memory.LogicalMemoryPort;
import org.xronos.openforge.lim.memory.MemoryRead;
import org.xronos.openforge.lim.op.AddMultiOp;
import org.xronos.openforge.lim.op.Constant;
import org.xronos.openforge.lim.op.MultiplyOp;
import org.xronos.openforge.lim.op.SimpleConstant;


/**
 * ArrayRead is an {@link OffsetMemoryRead} which gets its offset value from a
 * data {@link Port} and may add an additional offset to the runtime offset to
 * skip over the length field of the array. The MemoryReducer optimization may
 * remove this additional +1 if the length field is pruned from the memory.
 * 
 * @version $Id: ArrayRead.java 88 2006-01-11 22:39:52Z imiller $
 */
public class ArrayRead extends OffsetMemoryRead implements ArrayAccess {

	/** The port that recieves the offset address */
	private Port offsetPort;

	/**
	 * The number of housekeeping addressable locations between the base address
	 * and the first data element.
	 */
	// private final int skip;

	/** A constant representing the <code>skip</code> value */
	private Constant skipConstant;

	/**
	 * Constructs an ArrayRead with the specified memory read access.
	 * 
	 * @param addressableLocations
	 *            , the number of addressable locations read.
	 * @param maxAddressWidth
	 *            the pre-optimized number of bits in the address bus
	 * @param skip
	 *            the number of housekeeping addressable locations between the
	 *            array base address and the first data element
	 * @param policy
	 *            , the address stride policy for this access
	 */
	public ArrayRead(int addressableLocations, int maxAddressWidth,
			boolean isSigned, AddressStridePolicy policy) {
		this(addressableLocations, maxAddressWidth, isSigned, 0, policy);
	}

	/**
	 * Constructs an ArrayRead with the specified memory read access.
	 * 
	 * @param addressableLocations
	 *            , the number of addressable locations read.
	 * @param maxAddressWidth
	 *            the pre-optimized number of bits in the address bus
	 * @param skip
	 *            the number of housekeeping addressable locations between the
	 *            array base address and the first data element
	 * @param policy
	 *            , the address stride policy for this access
	 */
	public ArrayRead(int addressableLocations, int maxAddressWidth,
			boolean isSigned, int skip, AddressStridePolicy policy) {
		this(new MemoryRead(false, (addressableLocations * policy.getStride()),
				isSigned), addressableLocations, maxAddressWidth, skip);
	}

	/**
	 * Constructs an ArrayRead with the specified memory read access.
	 * 
	 * @param memoryRead
	 *            the actual {@link MemoryRead} used to read the memory,
	 *            contained in this Module.
	 * @param addressableLocations
	 *            , the number of addressable locations read.
	 * @param maxAddressWidth
	 *            the pre-optimized number of bits in the address bus
	 * @param skip
	 *            the number of housekeeping locations between the array base
	 *            address and the first data element
	 */
	private ArrayRead(MemoryRead memoryRead, int addressableLocations,
			int maxAddressWidth, int skip) {
		super(memoryRead, addressableLocations, maxAddressWidth);

		offsetPort = makeDataPort();
		final Bus offsetBus = offsetPort.getPeer();

		/*
		 * Multiply the offset (which is in number of array elements) by the
		 * addressable location count to obtain the number of offset locations.
		 */
		final Constant addressableLocationsConstant = new SimpleConstant(
				addressableLocations, maxAddressWidth, true);
		insertComponent(addressableLocationsConstant, 0);
		final Entry constEntry = addressableLocationsConstant
				.makeEntry(getInBuf().getExit(Exit.DONE));
		constEntry
				.addDependency(addressableLocationsConstant.getGoPort(),
						new ControlDependency(constEntry.getDrivingExit()
								.getDoneBus()));

		final MultiplyOp multiplier = new MultiplyOp(maxAddressWidth);
		insertComponent(multiplier, 1);
		final Entry multEntry = multiplier.makeEntry(getInBuf().getExit(
				Exit.DONE));
		multEntry.addDependency(multiplier.getLeftDataPort(),
				new DataDependency(offsetBus));
		multEntry.addDependency(multiplier.getRightDataPort(),
				new DataDependency(addressableLocationsConstant.getValueBus()));
		multEntry.addDependency(multiplier.getGoPort(), new ControlDependency(
				multEntry.getDrivingExit().getDoneBus()));

		// this.skip = skip;
		skipConstant = new SimpleConstant(skip, maxAddressWidth, false);
		insertComponent(skipConstant, 2);
		final Entry skipEntry = skipConstant.makeEntry(getInBuf().getExit(
				Exit.DONE));
		skipEntry.addDependency(skipConstant.getGoPort(),
				new ControlDependency(skipEntry.getDrivingExit().getDoneBus()));

		final AddMultiOp addOp = (AddMultiOp) getAddOp();
		final Entry addEntry = addOp.getEntries().get(0);
		addEntry.addDependency(addOp.getRightDataPort(), new DataDependency(
				multiplier.getResultBus()));
		final Port skipPort = addOp.makeDataPort();
		addEntry.addDependency(skipPort,
				new DataDependency(skipConstant.getValueBus()));
	}

	/**
	 * Gets the port which receives the address offset value.
	 */
	@Override
	public Port getOffsetPort() {
		return offsetPort;
	}

	/**
	 * Returns true if this memory access is an array access. returns true.
	 */
	@Override
	public boolean isArrayAccess() {
		return true;
	}

	@Override
	public void accept(Visitor visitor) {
		visitor.visit(this);
	}

	@Override
	public boolean removeComponent(Component component) {
		boolean ret = super.removeComponent(component);
		if (component == skipConstant) {
			skipConstant = null;
		}
		return ret;
	}

	/**
	 * Clones this object. A new ArrayRead is created using a copy of the
	 * {@link MemoryRead}, including its {@link LogicalMemoryPort}, and the
	 * {@link Location} from the {@link AllocationConstant}. The clone is added
	 * as an access to the {@link LogicalMemoryPort}.
	 * 
	 * @return the clone
	 */
	@Override
	public Object clone() throws CloneNotSupportedException {
		/*
		 * final MemoryRead cloneRead = new MemoryRead(false,
		 * getMemoryRead().getWidth(), getMemoryRead().isSigned());
		 * 
		 * final ArrayRead clone = new ArrayRead(cloneRead,
		 * getAccessLocationCount(),
		 * skipConstant.getValueBus().getValue().getSize(),
		 * (int)skipConstant.getValueBus().getValue().getValueMask());
		 * 
		 * clone.setBlockElement(this.getBlockElement()); if
		 * (getLogicalMemoryPort() != null) {
		 * getLogicalMemoryPort().addAccess(clone); }
		 * this.copyComponentAttributes(clone); return clone;
		 */
		ArrayRead clone = (ArrayRead) super.clone();

		if (getLogicalMemoryPort() != null) {
			getLogicalMemoryPort().addAccess(clone);
		}

		return clone;
	}

	@Override
	protected void cloneNotify(Module moduleClone,
			Map<Component, Component> cloneMap) {
		super.cloneNotify(moduleClone, cloneMap);
		((ArrayRead) moduleClone).skipConstant = (Constant) cloneMap
				.get(skipConstant);
		((ArrayRead) moduleClone).offsetPort = getPortClone(offsetPort,
				cloneMap);
	}
}
