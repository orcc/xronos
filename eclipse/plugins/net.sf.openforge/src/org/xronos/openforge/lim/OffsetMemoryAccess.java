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

import org.xronos.openforge.lim.memory.MemoryAccess;
import org.xronos.openforge.lim.memory.StructuralMemory;
import org.xronos.openforge.lim.op.AddMultiOp;
import org.xronos.openforge.lim.op.AddOp;
import org.xronos.openforge.lim.op.CastOp;
import org.xronos.openforge.lim.op.Constant;
import org.xronos.openforge.util.naming.ID;
import org.xronos.openforge.util.naming.IDSourceInfo;


/**
 * OffsetMemoryAccess is an abstract class for memory accesses that determine
 * the memory location using a base address and an offset which are added
 * together. This class creates the {@link AddOp} which adds the offset to the
 * base. The base address is taken from the data {@link Port} created by this
 * class, but it is the responsibility of the subclass to add a
 * {@link Dependency} from the right data {@link Port} of the {@link AddOp
 * AddOp} to a {@link Bus} that can provide the offset.
 * 
 * @version $Id: OffsetMemoryAccess.java 88 2006-01-11 22:39:52Z imiller $
 */
public abstract class OffsetMemoryAccess extends MemoryAccessBlock {

	/**
	 * Adder to add the offset to the base address, implemented as an AddMulti
	 * so that ArrayReads/Writes can add an additional 1 to skip the length
	 * field. Note that if only 2 ports are allocated it will look and act like
	 * a regular 2-input adder.
	 */
	private AddOp add = new AddMultiOp();

	private CastOp castOp;

	/** Base address */
	private Port baseAddressPort;

	private int maxAddressWidth;

	/**
	 * Constructs an OffsetMemoryAccess with the specified memory read access.
	 * An internal {@link AddOp} is created to provide the read address to the
	 * {@link MemoryAccess}. The left operand of the addition is tied to the
	 * base address {@link Port} of this module. The right operand of the
	 * addition, representing the offset, is left without a data dependency. The
	 * subclass should provide this, preferably in its constructor.
	 * 
	 * @param memoryAccess
	 *            the underlying {@link MemoryAccess} for this access.
	 * @param addressableLocations
	 *            the number of addressable locations this access is to
	 *            send/retrieve from the memory.
	 * @param maxAddressWidth
	 *            the pre-optimized number of bits in the address bus
	 */
	protected OffsetMemoryAccess(MemoryAccess memoryAccess,
			int addressableLocations, int maxAddressWidth) {
		super(memoryAccess, addressableLocations);
		baseAddressPort = makeDataPort();
		this.maxAddressWidth = maxAddressWidth;

		/*
		 * Cast the base address to the max number of address bits, so that all
		 * inputs to the adder will be the same width.
		 */
		castOp = new CastOp(maxAddressWidth, false);
		insertComponent(castOp, 0);
		insertComponent(add, 1);

		Constant encodedSize = StructuralMemory
				.encodeAccessCount(getAccessLocationCount());
		insertComponent(encodedSize, 0);

		setControlDependencies(false);

		/*
		 * Create the data dependency from the cast's input to the base address
		 * input.
		 */
		final Entry castEntry = castOp.getEntries().iterator().next();
		castEntry.addDependency(castOp.getDataPort(), new DataDependency(
				getBaseAddressPort().getPeer()));

		/*
		 * Create the data dependency from the add's left input to the cast.
		 */
		final Entry addEntry = add.getEntries().iterator().next();
		addEntry.addDependency(add.getLeftDataPort(),
				new DataDependency(castOp.getResultBus()));

		/*
		 * Create the data dependency from the MemoryAccess address input to the
		 * add's result Bus.
		 */
		final Entry memoryAccessEntry = getMemoryAccess().getEntries().get(0);
		Port addrPort = getMemoryAccess().getAddressPort();
		memoryAccessEntry.addDependency(addrPort,
				new DataDependency(add.getResultBus()));

		/*
		 * Create the data dependency from the MemoryAccess' size port to a
		 * constant (encoded) value of the size.
		 */
		memoryAccessEntry.addDependency(getMemoryAccess().getSizePort(),
				new DataDependency(encodedSize.getValueBus()));

		/*
		 * Give the add a reasonable name so that it doesn't show up as 'null'
		 * after translation.
		 */
		add.setIDLogical("addrCalc");
		add.getResultBus().setIDLogical("addrCalc");
	}

	/**
	 * Re-adds the {@link AddOp} and {@link CastOp} components and their
	 * {@link Dependency Dependencies}, removing them first if necessary. This
	 * allows this module to be reconstituted after constant propragation
	 * removes the adder.
	 */
	protected void rebuildAdder() {
		final AddOp theAdd = add;
		final CastOp theCast = castOp;

		if (add.getOwner() == this) {
			removeComponent(add);
		}
		add = theAdd;

		if (castOp.getOwner() == this) {
			removeComponent(castOp);
		}
		castOp = theCast;

		final InBuf inBuf = getInBuf();
		final Bus clockBus = inBuf.getClockBus();
		final Bus resetBus = inBuf.getResetBus();
		final Bus goBus = inBuf.getGoBus();

		final Component firstComponent = getSequence().get(0);
		Exit nextExit = getInBuf().getExit(Exit.DONE);
		Entry entry = null;

		insertComponent(castOp, 0);
		entry = castOp.makeEntry(nextExit);
		addDependencies(entry, clockBus, resetBus, goBus);
		entry.addDependency(castOp.getDataPort(), new DataDependency(
				getBaseAddressPort().getPeer()));
		nextExit = castOp.getExit(Exit.DONE);

		insertComponent(add, 1);
		entry = add.makeEntry(nextExit);
		entry.addDependency(add.getLeftDataPort(),
				new DataDependency(castOp.getResultBus()));
		addDependencies(entry, clockBus, resetBus, goBus);
		nextExit = add.getExit(Exit.DONE);

		entry = firstComponent.getEntries().iterator().next();
		entry.setDrivingExit(nextExit);
		nextExit = firstComponent.getExit(Exit.DONE);

		entry = getMemoryAccess().getEntries().iterator().next();
		entry.setDrivingExit(nextExit);
		entry.addDependency(getMemoryAccess().getAddressPort(),
				new DataDependency(add.getResultBus()));
	}

	/**
	 * Gets the {@link Port} that is to receive the base address.
	 */
	public Port getBaseAddressPort() {
		return baseAddressPort;
	}

	public int getMaxAddressWidth() {
		return maxAddressWidth;
	}

	@Override
	public void accept(Visitor visitor) {
		visitor.visit(this);
	}

	/**
	 * Gets the add operation used to add the base address and offset.
	 */
	protected AddOp getAddOp() {
		return add;
	}

	/**
	 * XXX FIXME This method is temporary support that is used because the
	 * Pipeliner treats memory access blocks as atomic components, thus not
	 * pipelining them internally which avoids pipelining the 0 depth adder.
	 * Once we add support for running full/half constant and dead component
	 * removal after scheduling we can remove these methods.
	 */
	@Override
	public int getEntryGateDepth() {
		return getMemoryAccess().getEntryGateDepth();
	}

	/**
	 * XXX FIXME This method is temporary support that is used because the
	 * Pipeliner treats memory access blocks as atomic components, thus not
	 * pipelining them internally which avoids pipelining the 0 depth adder.
	 * Once we add support for running full/half constant and dead component
	 * removal after scheduling we can remove these methods.
	 */
	@Override
	public int getExitGateDepth() {
		return getMemoryAccess().getExitGateDepth();
	}

	/**
	 * Overrides the super in order to set the same id source info on all
	 * components contained in this Module.
	 */
	@Override
	public void setIDSourceInfo(IDSourceInfo sinfo) {
		super.setIDSourceInfo(sinfo);
		for (Component component : getComponents()) {
			((ID) component).setIDSourceInfo(sinfo);
		}
	}

	@Override
	public boolean removeComponent(Component component) {
		boolean ret = super.removeComponent(component);
		if (component == add) {
			add = null;
		}
		if (component == castOp) {
			castOp = null;
		}

		return ret;
	}

	@Override
	protected void cloneNotify(Module moduleClone,
			Map<Component, Component> cloneMap) {
		super.cloneNotify(moduleClone, cloneMap);
		OffsetMemoryAccess clone = (OffsetMemoryAccess) moduleClone;
		clone.add = (AddOp) cloneMap.get(add);
		clone.castOp = (CastOp) cloneMap.get(castOp);
		clone.baseAddressPort = getPortClone(baseAddressPort, cloneMap);
	}
}
