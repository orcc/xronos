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

import java.util.Collection;
import java.util.Collections;

import org.xronos.openforge.lim.Bus;
import org.xronos.openforge.lim.Exit;
import org.xronos.openforge.lim.Latency;
import org.xronos.openforge.lim.Module;
import org.xronos.openforge.lim.Port;
import org.xronos.openforge.lim.Referenceable;
import org.xronos.openforge.lim.Resource;
import org.xronos.openforge.lim.SizedAccess;
import org.xronos.openforge.lim.StateAccessor;


/**
 * MemoryAccess factors out functionality that is common among all accesses to
 * memory such as address port, done bus and methods that identify whether the
 * node uses go, done, etc.
 * 
 * <p>
 * Created: Mon Sep 23 16:13:23 2002
 * 
 * @author imiller, last modified by $Author: imiller $
 * @version $Id: MemoryAccess.java 490 2007-06-15 16:37:00Z imiller $
 */
public abstract class MemoryAccess extends SizedAccess implements StateAccessor {

	private LogicalMemoryPort logicalMemoryPort;

	/** True if the value being accessed is signed, false if unsigned */
	private boolean isSigned;

	/** The number of (unoptimized) bits in the value being accessed in memory */
	private int width;

	/**
	 * Describe constructor here.
	 * 
	 * @param portCount
	 *            the number of data ports
	 * @param isVolatile
	 *            true if this is an access to a volatile memory location
	 */
	public MemoryAccess(int portCount, boolean isVolatile, boolean isSigned,
			int width) {
		super(null, portCount, isVolatile);
		logicalMemoryPort = null;
		this.isSigned = isSigned;
		this.width = width;
	}

	/**
	 * Gets the {@link LogicalMemoryPort} accessed by this component.
	 * 
	 * @return the logical memory port, or null if there is none
	 */
	public LogicalMemoryPort getMemoryPort() {
		return logicalMemoryPort;
	}

	/**
	 * Sets the {@link LogicalMemoryPort} accessed by this component.
	 * 
	 * @param memoryPort
	 *            the logical memory port, or null if there is none
	 */
	public void setMemoryPort(LogicalMemoryPort memoryPort) {
		logicalMemoryPort = memoryPort;
	}

	/**
	 * Same as {@link #getMemoryPort()}.
	 * 
	 * @return the logical memory port, or null if there is none
	 */
	@Override
	public Resource getResource() {
		return getMemoryPort();
	}

	@Override
	public Referenceable getReferenceable() {
		return getMemoryPort().getLogicalMemory();
	}

	/**
	 * Returns the number of unoptimized bits in the value being accessed in
	 * memory
	 * 
	 * @return a value of type 'int'
	 */
	public int getWidth() {
		return width;
	}

	public Port getAddressPort() {
		return getDataPorts().get(0);
	}

	public Bus getDoneBus() {
		return getExit(Exit.DONE).getDoneBus();
	}

	/**
	 * Returns true if this MemoryAccess is a signed accesses to memory or false
	 * if the access is an unsigned access to memory.
	 * 
	 * @return a 'boolean'
	 */
	public boolean isSigned() {
		return isSigned;
	}

	public abstract Module getPhysicalComponent();

	public abstract boolean isReadAccess();

	public abstract boolean isWriteAccess();

	public boolean hasPhysicalComponent() {
		return getPhysicalComponent() != null;
	}

	/**
	 * Returns true since both {@link MemoryRead} and {@link MemoryWrite} use
	 * the clock in their Physical implementation.
	 */
	@Override
	public boolean consumesClock() {
		return true;
	}

	/**
	 * Returns true since both {@link MemoryRead} and {@link MemoryWrite} use
	 * the reset in their Physical implementation.
	 */
	@Override
	public boolean consumesReset() {
		return true;
	}

	/**
	 * Tests whether this component requires a connection to its <em>go</em>
	 * {@link Port} in order to commence processing.
	 */
	@Override
	public boolean consumesGo() {
		return true;
	}

	/**
	 * Tests whether this component produces a signal on the done {@link Bus} of
	 * each of its {@link Exit Exits}, returns true if the accessed
	 * {@link LogicalMemoryPort} is arbitrated.
	 */
	@Override
	public boolean producesDone() {
		return getMemoryPort().isArbitrated();
	}

	/**
	 * Returns true if this access takes more than one clock cycle, or if the
	 * latency is open.
	 */
	@Override
	public boolean isDoneSynchronous() {
		return getLatency() != Latency.ZERO;
	}

	/**
	 * returns true if the accessed {@link LogicalMemoryPort} is not arbitrated.
	 */
	@Override
	public boolean isBalanceable() {
		return !getMemoryPort().isArbitrated();
	}

	/**
	 * Returns the {@link Latency} reported for this access by the accessed
	 * resource.
	 */
	@Override
	public Latency getLatency() {
		return getMemoryPort().getLatency(getOnlyExit());
	}

	/**
	 * Gets the resources accessed by or within this component.
	 * 
	 * @return a collection of {@link Resource}
	 */
	@Override
	public Collection<Resource> getAccessedResources() {
		return Collections.singletonList((Resource) getMemoryPort());
	}

	@Override
	protected Exit createExit(int dataCount, Exit.Type type, String label) {
		return new VariableLatencyExit(this, dataCount, type, label);
	}

	/**
	 * Overrides {@link Exit#getLatency()} to return the latency as specified by
	 * the owner of the exit.
	 */
	private static class VariableLatencyExit extends Exit {
		VariableLatencyExit(MemoryAccess memoryAccess, int dataCount,
				Exit.Type type, String label) {
			super(memoryAccess, dataCount, type, label);
		}

		@Override
		public Latency getLatency() {
			return ((MemoryAccess) getOwner()).getLatency();
		}
	}

}// MemoryAccess
