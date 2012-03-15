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

import net.sf.openforge.lim.memory.MemoryRead;

/**
 * OffsetMemoryRead is an {@link OffsetMemoryAccess} for a read of memory. It
 * adds a data {@link Bus} that provides the result of the memory read.
 * 
 * @version $Id: OffsetMemoryRead.java 70 2005-12-01 17:43:11Z imiller $
 */
public abstract class OffsetMemoryRead extends OffsetMemoryAccess {

	/** Data bus for result of read */
	private Bus resultBus;

	/**
	 * Constructs an OffsetMemoryRead with the specified memory read access.
	 * 
	 * @param memoryRead
	 *            the underlying {@link MemoryRead} for this access.
	 * @param addressableLocations
	 *            the number of addressable locations this access is to
	 *            send/retrieve from the memory.
	 * @param maxAddressWidth
	 *            the pre-optimized number of bits in the address bus
	 */
	
	protected OffsetMemoryRead(MemoryRead memoryRead, int addressableLocations,
			int maxAddressWidth) {
		super(memoryRead, addressableLocations, maxAddressWidth);
		@SuppressWarnings("unused")
		final Bus readBus = (Bus) memoryRead.getExit(Exit.DONE).getDataBuses()
				.get(0);

		/*
		 * Connect the MemoryRead's data output to the output of this module.
		 */
		Exit exit = getExit(Exit.DONE);
		exit.getDoneBus().setUsed(true);
		final OutBuf ob = (OutBuf) exit.getPeer();
		final Entry obEntry = (Entry) ob.getEntries().get(0);
		this.resultBus = exit.makeDataBus();
		obEntry.addDependency(resultBus.getPeer(), new DataDependency(
				memoryRead.getResultBus()));
	}

	/**
	 * Gets the data {@link Bus}.
	 */
	public Bus getResultBus() {
		return resultBus;
	}

	/**
	 * Attempts to remove the given {@link Bus} from this component.
	 * 
	 * @param bus
	 *            the bus to remove
	 * @return true if the bus was removed.
	 */
	public boolean removeDataBus(Bus bus) {
		final boolean isRemoved = super.removeDataBus(bus);
		if (isRemoved && (bus == resultBus)) {
			this.resultBus = null;
		}
		return isRemoved;
	}

	/**
	 * Returns false
	 */
	public boolean isWrite() {
		return false;
	}

	/**
	 * Gets the low level read operation contined in this module.
	 */
	public MemoryRead getMemoryRead() {
		return (MemoryRead) getMemoryAccess();
	}

	protected void cloneNotify(Module moduleClone, Map cloneMap) {
		// XXX WARNING! This is probably never called since all the
		// concrete subclasses override cloneNotify and do not call
		// the super!
		super.cloneNotify(moduleClone, cloneMap);
		OffsetMemoryRead clone = (OffsetMemoryRead) moduleClone;
		clone.resultBus = ((resultBus == null) ? null : getBusClone(resultBus,
				cloneMap));
	}
}
