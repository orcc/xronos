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

import net.sf.openforge.lim.memory.MemoryWrite;

/**
 * OffsetMemoryRead is an {@link OffsetMemoryAccess} for a write to memory. It
 * adds a data {@link Port} that receives the value to be written.
 * 
 * @version $Id: OffsetMemoryWrite.java 70 2005-12-01 17:43:11Z imiller $
 */
public abstract class OffsetMemoryWrite extends OffsetMemoryAccess {

	/** The port which receives the value to be written */
	private Port valuePort;

	/**
	 * Constructs an OffsetMemoryWrite with the specified memory write access
	 * 
	 * @param memoryWrite
	 *            the underlying {@link MemoryWrite} for this access.
	 * @param addressableLocations
	 *            the number of addressable locations this access is to
	 *            send/retrieve from the memory.
	 * @param maxAddressWidth
	 *            the pre-optimized number of bits in the address bus
	 */
	public OffsetMemoryWrite(MemoryWrite memoryWrite, int addressableLocations,
			int maxAddressWidth) {
		super(memoryWrite, addressableLocations, maxAddressWidth);

		valuePort = makeDataPort();
		final Bus valueBus = valuePort.getPeer();
		final Entry writeEntry = memoryWrite.getEntries().iterator().next();
		writeEntry.addDependency(memoryWrite.getDataPort(), new DataDependency(
				valueBus));

		getExit(Exit.DONE).getDoneBus().setUsed(true);
	}

	/**
	 * Gets the data port that receives the value to be written.
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

	public MemoryWrite getMemoryWrite() {
		return (MemoryWrite) getMemoryAccess();
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

	@Override
	protected void cloneNotify(Module moduleClone,
			Map<Component, Component> cloneMap) {
		// XXX WARNING! This is probably never called since all the
		// concrete subclasses override cloneNotify and do not call
		// the super!
		super.cloneNotify(moduleClone, cloneMap);
		OffsetMemoryWrite clone = (OffsetMemoryWrite) moduleClone;
		clone.valuePort = getPortClone(valuePort, cloneMap);
	}

}
