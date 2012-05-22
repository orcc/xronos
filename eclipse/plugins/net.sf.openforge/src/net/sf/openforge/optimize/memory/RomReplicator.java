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

package net.sf.openforge.optimize.memory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.sf.openforge.app.EngineThread;
import net.sf.openforge.app.GenericJob;
import net.sf.openforge.app.OptionRegistry;
import net.sf.openforge.app.project.Option;
import net.sf.openforge.app.project.OptionInt;
import net.sf.openforge.lim.CodeLabel;
import net.sf.openforge.lim.Design;
import net.sf.openforge.lim.Visitable;
import net.sf.openforge.lim.memory.LValue;
import net.sf.openforge.lim.memory.Location;
import net.sf.openforge.lim.memory.LocationValueSource;
import net.sf.openforge.lim.memory.LogicalMemory;
import net.sf.openforge.lim.memory.LogicalMemoryPort;
import net.sf.openforge.lim.memory.Variable;
import net.sf.openforge.optimize.Optimization;
import net.sf.openforge.util.naming.ID;

/**
 * <code>RomReplicator</code> reduces the number of accesses on each read-only
 * {@link LogicalMemory} (ROM) in a {@link Design} by replicating the ROM once
 * for every access ({@link LValue}) to that ROM. Once a maximum number of
 * memory bits for the design has been reached, no new memories will be
 * allocated and all accesses will be distributed among the existing copies.
 * <P>
 * Additionally, the number of copies may be reduced by the use of dual-ported
 * memories.
 * <p>
 * This class uses the preferences {@link OptimizeDefiner#ROM_REPLICATION_LIMIT}
 * and {@link OptimizeDefiner#FORCE_SINGLE_PORT_ROMS}
 * <p>
 * Created: Mon Oct 21 16:02:39 2002
 * 
 * @author imiller, last modified by $Author: imiller $
 * @version $Id: RomReplicator.java 490 2007-06-15 16:37:00Z imiller $
 */
public class RomReplicator implements Optimization {

	private static final int memLimitDivisor = 1024;

	private int repCount = 0; // Count of how many memories were replicated

	private int accessesPerPort = 1; // Count of accesses per port on replicated
										// memories

	/**
	 * Applies this optimization to a given target. If all LValue accessors of
	 * the given LogicalMemory are read accesses, then copy the memory and
	 * allocate dual ports based on the ROM_REPLICATION_LIMIT and
	 * FORCE_SINGLE_PORT_ROMS preferences in order to reduce the number of
	 * LValue accessors per LogicalMemoryPort. Allocate LValues to the set of
	 * copied memories LogicalMemoryPort(s).
	 * 
	 * @param target
	 *            the target on which to run this optimization
	 */
	@Override
	public void run(Visitable target) {
		final Design design = (Design) target;
		Option op;

		/* Rom replication limit */
		op = EngineThread.getGenericJob().getOption(
				OptionRegistry.ROM_REPLICATION_LIMIT);
		final int romRepLimit = ((OptionInt) op)
				.getValueAsInt(CodeLabel.UNSCOPED) * memLimitDivisor >> 3;
		if (romRepLimit <= 0) {
			return;
		}

		/* Flag of Single/Dual Port replication */
		final boolean isSinglePort = EngineThread.getGenericJob()
				.getUnscopedBooleanOptionValue(
						OptionRegistry.FORCE_SINGLE_PORT_ROMS);

		final ObjectResolver resolver = ObjectResolver.resolve(design);

		int repMemSize = 0;
		Map<LogicalMemory, Set<LogicalMemory>> repMemoriesMap = new HashMap<LogicalMemory, Set<LogicalMemory>>();
		boolean recalculateRepMemSize = true;

		/*
		 * Calculate the repicated Memory size, if the size of memories after
		 * replication exceeds ROM_REPLICATION_LIMIT, the number of accesses per
		 * port increases by 1 until the replicated memory size is less than or
		 * equal to the specified limit.
		 */
		while (recalculateRepMemSize) {
			int maxMemRepCount = 1;

			for (LogicalMemory memory : design.getLogicalMemories()) {
				/*
				 * Count the total number of read accesses to the LogicalMemory.
				 */
				int accessCount = 0;
				for (LogicalMemoryPort memPort : memory.getLogicalMemoryPorts()) {
					accessCount += memPort.getReadAccesses().size();
				}

				int numCopies = (isSinglePort ? (int) Math
						.ceil((float) accessCount / (float) accessesPerPort)
						: (int) Math.ceil(Math.ceil(accessCount / 2.0)
								/ accessesPerPort));

				repMemSize += memory.getSizeInBytes() * numCopies;
				maxMemRepCount = Math.max(maxMemRepCount, numCopies);
			}

			if ((repMemSize > romRepLimit) && (maxMemRepCount > 1)) {
				accessesPerPort++;
				repMemSize = 0;
			} else {
				recalculateRepMemSize = false;
			}
		}

		/*
		 * Replicate each read-only LogicalMemory
		 */
		for (LogicalMemory memory : design.getLogicalMemories()) {
			boolean isReadOnly = true;

			for (LogicalMemoryPort memPort : memory.getLogicalMemoryPorts()) {
				if (!memPort.isReadOnly()) {
					isReadOnly = false;
				}
			}

			// for (Iterator memPortIter =
			// memory.getLogicalMemoryPorts().iterator();
			// memPortIter.hasNext();)
			// {
			// isReadOnly = isReadOnly &&
			// ((LogicalMemoryPort)memPortIter.next()).isReadOnly();
			// }

			if (isReadOnly) {
				repMemoriesMap.put(memory,
						replicate(memory, resolver, isSinglePort));
			}
		}

		/*
		 * If a LogicalMemory has been replicated, remove the original from the
		 * Design and add the replicas.
		 */
		for (Map.Entry<LogicalMemory, Set<LogicalMemory>> entry : repMemoriesMap
				.entrySet()) {
			final LogicalMemory origMem = entry.getKey();
			final Set<LogicalMemory> repMemorySet = entry.getValue();

			if (!repMemorySet.isEmpty()) {
				design.removeMemory(origMem);
				for (Iterator<LogicalMemory> repMemIter = repMemorySet
						.iterator(); repMemIter.hasNext();) {
					design.addMemory(repMemIter.next());
				}
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
	 * Does nothing.
	 */
	@Override
	public void clear() {
	}

	/**
	 * Reports, via {@link GenericJob#info}, what optimization is being
	 * performed
	 */
	@Override
	public void preStatus() {
		EngineThread.getGenericJob().info(
				"optimizing read-only memory locations...");
	}

	/**
	 * Reports, via {@link GenericJob#verbose}, the results of <b>this</b> pass
	 * of the optimization.
	 */
	@Override
	public void postStatus() {
		if (repCount > 0) {
			EngineThread.getGenericJob().info(
					"replicated " + repCount + " read-only memories");
		} else {
			EngineThread.getGenericJob().verbose(
					"replicated " + repCount + " read-only memories");
		}
	}

	/**
	 * Replicate the given LogicalMemory
	 * 
	 * <p>
	 * requires: non-null memory
	 * <p>
	 * modifies: memory (may create second LogicalMemoryPort), removes accesses
	 * (LValues)
	 * <p>
	 * effects: returns a Set of the replicated LogicalMemories.
	 * 
	 * @param memory
	 *            a LogicalMemory
	 * @param locationBaseMap
	 *            a HashMap which keeps the LocationConstant associated with
	 *            each access.
	 * @param isSingle
	 *            a boolean flag of single/dual port(s) replication
	 * @return a non-null Set of newly created LogicalMemory objects. May be
	 *         empty if no copies were made.
	 * @throws IllegalArgumentException
	 *             if 'memory' is null.
	 */
	Set<LogicalMemory> replicate(LogicalMemory memory, ObjectResolver resolver,
			boolean isSinglePort) {
		/* Collect up duplicated rom */
		Set<LogicalMemory> replicatedMemories = new HashSet<LogicalMemory>();

		/*
		 * Make a memory copy and allocate pre-calculated number of accesses per
		 * port
		 */
		for (Iterator<LValue> accessIter = new ArrayList<LValue>(
				memory.getLValues()).iterator(); accessIter.hasNext();) {
			final MemoryCopier copier = new MemoryCopier(memory);
			final LogicalMemory copiedMem = copier.getCopy();
			repCount++;

			assert copiedMem.getLogicalMemoryPorts().size() < 3;
			final Iterator<LogicalMemoryPort> memoryPortIter = copiedMem
					.getLogicalMemoryPorts().iterator();
			final Collection<LValue> retargetedAccesses = new HashSet<LValue>();

			RetargetVisitor retargetVis = new RetargetVisitor(
					copier.getLocationMap());
			LogicalMemoryPort memoryPort = memoryPortIter.next();
			for (int i = 0; i < accessesPerPort && accessIter.hasNext(); i++) {
				final LValue access = accessIter.next();

				access.getLogicalMemoryPort().removeAccess(access);
				memoryPort.addAccess(access);
				access.accept(retargetVis);

				retargetedAccesses.add(access);

				for (LocationValueSource source : resolver
						.getAddressSources(access)) {
					final Location oldLoc = source.getTarget();
					final Location newLoc = Variable.getCorrelatedLocation(
							copier.getLocationMap(), oldLoc);
					assert newLoc != null : "Illegal null correlated location";
					source.setTarget(newLoc);
				}
			}

			/*
			 * If a second memory port is allowed...
			 */
			if (!isSinglePort) {
				/*
				 * If a second memory port is needed, create it if it doesn't
				 * exist.
				 */
				if (accessIter.hasNext()) {
					memoryPort = (memoryPortIter.hasNext() ? (LogicalMemoryPort) memoryPortIter
							.next() : copiedMem.createLogicalMemoryPort());
				}

				for (int i = 0; i < accessesPerPort && accessIter.hasNext(); i++) {
					final LValue access = accessIter.next();

					access.getLogicalMemoryPort().removeAccess(access);
					memoryPort.addAccess(access);
					access.accept(retargetVis);
					retargetedAccesses.add(access);

					for (LocationValueSource source : resolver
							.getAddressSources(access)) {
						// final LocationConstant addressSource =
						// (LocationConstant)sourceIter.next();
						// addressSource.accept(retargetVis);

						final Location oldLoc = source.getTarget();
						final Location newLoc = Variable.getCorrelatedLocation(
								copier.getLocationMap(), oldLoc);
						assert newLoc != null : "Illegal null correlated location";
						source.setTarget(newLoc);
					}
				}
			}

			/*
			 * Only keep the retargeted accesses in the copied memory and remove
			 * the rest, also remove the retargeted accesses in the origical
			 * memory.
			 */
			final List<LValue> lValues = new ArrayList<LValue>(
					memory.getLValues());
			for (LValue lvalue : lValues) {
				if (!retargetedAccesses.contains(lvalue)) {
					copiedMem.removeAccessor(lvalue);
				} else {
					memory.removeAccessor(lvalue);
				}
			}

			replicatedMemories.add(copiedMem);
		}

		return replicatedMemories;
	}

	@SuppressWarnings("unused")
	private static void overLimit(Design design, LogicalMemory mem,
			int romSize, boolean partial) {
		design.getEngine()
				.getGenericJob()
				.warn("could not "
						+ (partial ? "fully " : "")
						+ "replicate "
						+ ID.showLogical(mem)
						+ ";\nuser pref memory limit reached -- increase value for preference: "
						+ OptionRegistry.ROM_REPLICATION_LIMIT.getKey()
						+ " by "
						+ ((int) Math.ceil((float) romSize
								/ (float) memLimitDivisor))
						+ " per desired copy");
	}

}// RomReplicator
