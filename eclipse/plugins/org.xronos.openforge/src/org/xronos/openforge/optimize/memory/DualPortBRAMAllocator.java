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

package org.xronos.openforge.optimize.memory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.xronos.openforge.app.EngineThread;
import org.xronos.openforge.app.GenericJob;
import org.xronos.openforge.app.OptionRegistry;
import org.xronos.openforge.app.project.OptionInt;
import org.xronos.openforge.lim.ArrayRead;
import org.xronos.openforge.lim.CodeLabel;
import org.xronos.openforge.lim.DataFlowVisitor;
import org.xronos.openforge.lim.Design;
import org.xronos.openforge.lim.HeapRead;
import org.xronos.openforge.lim.MemoryAccessBlock;
import org.xronos.openforge.lim.Task;
import org.xronos.openforge.lim.Visitable;
import org.xronos.openforge.lim.memory.AbsoluteMemoryRead;
import org.xronos.openforge.lim.memory.LogicalMemory;
import org.xronos.openforge.lim.memory.LogicalMemoryPort;
import org.xronos.openforge.optimize.Optimization;
import org.xronos.openforge.optimize._optimize;


/**
 * <code>DualPortBRAMAllocator</code> creates a second memory port on any memory
 * meeting the following criteria:
 * <ul>
 * <li>The memory is a block ram OR the allow_dual_port_lut_rams preference is
 * set
 * <li>There exists at least one action with multiple read accesses.
 * </ul>
 * <p>
 * Currently the write accesses are all left on the original port and the read
 * accesses are allocated to both ports alternately according to dataflow order.
 * This is not an ideal allocation as it would be better to have the reads in
 * parallel branches scheduled on the same port since they cannot ever execute
 * in the same cycle anyway.
 * <p>
 * Created: Tue Jun 12 23:29:05 2007
 * 
 * @author imiller, last modified by $Author: imiller $
 * @version $Id: RomReplicator.java 70 2005-12-01 17:43:11Z imiller $
 */
public class DualPortBRAMAllocator implements Optimization {

	/**
	 * Counter used in reporting to user.
	 */
	private int repCount = 0;

	/**
	 * Applies this optimization to the given visitable target (must be a Design
	 * object)
	 * 
	 * @param target
	 *            the Design on which to run this optimization
	 */
	@Override
	public void run(Visitable target) {
		if (!(target instanceof Design)) {
			return;
		}

		final boolean suppress = EngineThread.getGenericJob()
				.getUnscopedBooleanOptionValue(
						OptionRegistry.SUPPRESS_DUAL_PORT_RAM);
		if (suppress) {
			return;
		}

		final Design design = (Design) target;
		_optimize.ln(_optimize.DP_MEM, "DP Analyzing desing " + design);

		Map<Task, Map<LogicalMemory, Set<MemoryAccessBlock>>> taskReads = new HashMap<Task, Map<LogicalMemory, Set<MemoryAccessBlock>>>();
		Set<LogicalMemory> dualPortCandidates = new HashSet<LogicalMemory>();
		for (Task task : design.getTasks()) {
			ReadFinder finder = new ReadFinder();
			task.accept(finder);
			_optimize.ln(_optimize.DP_MEM, "DP Found reads " + finder.readMap);
			_optimize.ln(_optimize.DP_MEM, "DP Found multireads "
					+ finder.multiReadMemories);
			taskReads.put(task, finder.readMap);
			dualPortCandidates.addAll(finder.multiReadMemories);
		}

		final boolean allowDPLutMem = EngineThread.getGenericJob()
				.getUnscopedBooleanOptionValue(
						OptionRegistry.ALLOW_DUAL_PORT_LUT);

		for (LogicalMemory mem : dualPortCandidates) {
			_optimize.ln(_optimize.DP_MEM, "Candidate memory " + mem + " "
					+ mem.getLogicalMemoryPorts().size());
			if (mem.getLogicalMemoryPorts().size() > 1) {
				continue;
			}

			// Implementation is not set until very late in the process
			// if (mem.getImplementation() == null)
			// continue;
			// if (mem.getImplementation().isLUT() && !allowDPLutMem) continue;
			_optimize.ln(_optimize.DP_MEM, "\tallowing DP LUT memories "
					+ allowDPLutMem);
			if (!allowDPLutMem) {
				final int bytes = ((OptionInt) EngineThread.getGenericJob()
						.getOption(OptionRegistry.MAX_LUT_BYTES))
						.getValueAsInt(CodeLabel.UNSCOPED);
				_optimize.ln(_optimize.DP_MEM,
						"\t\tmemSize " + mem.getSizeInBytes() + " max size "
								+ bytes);
				if (mem.getSizeInBytes() <= bytes) {
					continue;
				}
			}

			mem.createLogicalMemoryPort();

			repCount++;

			assert mem.getLogicalMemoryPorts().size() == 2;

			// Build a list of all accesses
			List<MemoryAccessBlock> reads = new ArrayList<MemoryAccessBlock>();
			for (Map<LogicalMemory, Set<MemoryAccessBlock>> memMap : taskReads
					.values()) {
				if (memMap.containsKey(mem)) {
					reads.addAll(memMap.get(mem));
				}
			}
			_optimize.ln(_optimize.DP_MEM, "All reads " + reads);

			// Remove all the read accesses from the memory, then
			// re-attach them, balanced across all ports. By only
			// moving the reads we are assured that the writes will
			// all be on one port which is a requirement for any LUT
			// based memory.
			for (MemoryAccessBlock access : reads) {
				access.removeFromMemory();
			}

			List<LogicalMemoryPort> ports = new LinkedList<LogicalMemoryPort>(
					mem.getLogicalMemoryPorts());
			for (MemoryAccessBlock access : reads) {
				LogicalMemoryPort port = ports.remove(0);
				port.addAccess(access);
				_optimize.ln(_optimize.DP_MEM, "\t" + access + " to port "
						+ port);
				ports.add(port); // put it back on the end
			}
		}
	}

	/**
	 * Returns false, the didModify is used to determine if this optimization
	 * caused a change which necessitates other optimizations to re-run.
	 */
	@Override
	public boolean didModify() {
		return false;
	}

	/**
	 * resets the repcount
	 */
	@Override
	public void clear() {
		repCount = 0;
	}

	/**
	 * Reports, via {@link GenericJob#info}, what optimization is being
	 * performed
	 */
	@Override
	public void preStatus() {
		EngineThread.getGenericJob()
				.info("converting memories to dual port...");
	}

	/**
	 * Reports, via {@link GenericJob#verbose}, the results of <b>this</b> pass
	 * of the optimization.
	 */
	@Override
	public void postStatus() {
		if (repCount > 0) {
			EngineThread.getGenericJob().info(
					"converted " + repCount + " memories to dual port");
		} else {
			EngineThread.getGenericJob().verbose(
					"converted " + repCount + " memories to dual port");
		}
	}

	private static class ReadFinder extends DataFlowVisitor {
		Map<LogicalMemory, Set<MemoryAccessBlock>> readMap = new HashMap<LogicalMemory, Set<MemoryAccessBlock>>();
		// This visitor will add memories to this set iff they have
		// multiple reads in the given traversal scope.
		Set<LogicalMemory> multiReadMemories = new HashSet<LogicalMemory>();

		@Override
		public void visit(AbsoluteMemoryRead read) {
			super.visit(read);
			handleRead(read);
		}

		@Override
		public void visit(HeapRead read) {
			super.visit(read);
			handleRead(read);
		}

		@Override
		public void visit(ArrayRead read) {
			super.visit(read);
			handleRead(read);
		}

		private void handleRead(MemoryAccessBlock read) {
			LogicalMemory target = read.getLogicalMemoryPort()
					.getLogicalMemory();
			Set<MemoryAccessBlock> reads = readMap.get(target);
			if (reads == null) {
				reads = new LinkedHashSet<MemoryAccessBlock>();
				readMap.put(target, reads);
			}
			reads.add(read);
			if (reads.size() > 1) {
				multiReadMemories.add(target);
			}
		}
	}

}// RomReplicator
