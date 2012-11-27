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

package org.xronos.openforge.optimize.io;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


import org.eclipse.core.runtime.jobs.Job;
import org.xronos.openforge.app.EngineThread;
import org.xronos.openforge.app.OptionRegistry;
import org.xronos.openforge.lim.Dependency;
import org.xronos.openforge.lim.Design;
import org.xronos.openforge.lim.Exit;
import org.xronos.openforge.lim.Loop;
import org.xronos.openforge.lim.MemoryAccessBlock;
import org.xronos.openforge.lim.Visitable;
import org.xronos.openforge.lim.io.BlockElement;
import org.xronos.openforge.lim.memory.Allocation;
import org.xronos.openforge.lim.memory.LValue;
import org.xronos.openforge.lim.memory.LogicalMemory;
import org.xronos.openforge.optimize.ComponentSwapVisitor;
import org.xronos.openforge.optimize.Optimization;
import org.xronos.openforge.optimize.memory.ObjectResolver;

/**
 * BlockElementRemover analyzes each Allocation in all memories of a Design, and
 * determines from their access characteristics and their association with a
 * given {@link BlockElement} whether that BlockElement can be eliminated from
 * either the input block IO interface or the output block IO interface. If the
 * BlockElement can be eliminated from the interface, then the LValue access(es)
 * in the block IO wrapper functionality (around the entry method) associated
 * with either loading or unloading the Allocation(s) are deleted, effectively
 * removing the element from the interface (removal of the LValue will leave the
 * fifo read/write dangling and dead component removal will remove it). Further,
 * the BlockElement will be modified so that its stream format is empty,
 * effectively removing it from the stream while still maintaining the
 * information needed for generation of an ATB.
 * 
 * <p>
 * <b>Note</b> in the case of input parameters which are write-only in the
 * algorithmic portion of the design, we do not currently do enough analysis to
 * determine that the parameter can be eliminated from the transfer. Take for
 * example a function which is modifying only half of an array. If we do not
 * accept that array as input data, then the returned array will only be 1/2
 * correct. ie, it is necessary for us to preserve the data in any non-written
 * elements. Consequently the elimination of write-only parameters is now
 * qualified by a user preference (command line arg woparamopt) which defaults
 * to not removing these parameters. Forge should be modified so that we do a
 * more comprehensive analysis, and if we can determine that the param is
 * write-first (or write-only) to EVERY byte of the parameter, then we can
 * automatically eliminate it from the stream. Until then, the command line arg
 * defaults to false and we will leave all write only params in the input stream
 * to ensure correctness in all cases.
 * 
 * <p>
 * Created: Thu Feb 26 07:28:05 2004
 * 
 * @author imiller, last modified by $Author: imiller $
 * @version $Id: BlockElementRemover.java 2 2005-06-09 20:00:48Z imiller $
 */
public class BlockElementRemover implements Optimization {

	/**
	 * A List of BlockElements that have been deleted ON THE CURRENT RUN, this
	 * list will be cleared out by the 'clear' method.
	 */
	private List<BlockElement> deletedBlockElements = new ArrayList<BlockElement>();

	/**
	 * Create a new instance of this Optimization.
	 */
	public BlockElementRemover() {
	}

	/**
	 * Runs this Optimization on the specified {@link Visitable} target, which
	 * must be a design. Does nothing if it is not a design.
	 * 
	 * @param visitable
	 *            a <code>Visitable</code> value
	 */
	@Override
	public void run(Visitable visitable) {
		if (!(visitable instanceof Design)) {
			return;
		}

		final Design design = (Design) visitable;

		// I really hate to insert yet another call to object
		// resolver, but we must be sure that the memory information
		// is correct or this will fail.
		ObjectResolver.resolve(design);

		// REMEMBER: A BlockElement is unique to a particular
		// parameter on a particular interface!
		// 1. Capture all the Allocations in the design into
		// BlockAllocationSets based on their BlockElements. It is
		// correct for one Allocation to be in multiple sets because
		// the end result is to clear BlockElement's stream formats
		// and remove the LValues associated with BlockElements
		// (ie no change to an Allocation itself)
		// 2. For each Set query if isInternalReadOnly()
		// 2.1 If read only, AND the BlockElement is owned by a
		// BlockDescriptor which is !isSlave(), then get all
		// isRead LValues for all Allocations in the Set. For
		// any of these read LValues where
		// <lvalue>.getBlockElement != the block element for the
		// BlockAllocationSet and is not null, delete that read
		// LValue from the LIM. If that LValue is owned by a
		// loop body, then force loop unrolling to run on that
		// loop (ie, set a local preference for that loop to turn
		// on loop unrolling)
		// 2.2 Also, remove the blockElement from the Allocation
		// 2.3 Also, clear the stream format of that block element
		// 3. For each Set query if isInternalWriteOnly() ||
		// isInternalWriteFirst()
		// 3.1 If write only(either condition), AND the BlockElement
		// is owned by a BlockDescriptor which isSlave(), then
		// get all isWrite LValues for all Allocations in the
		// Set. For any of these write LValues where
		// <lvalue>.getBlockElement != the block element for the
		// BlockAllocationSet, and is not null, delete
		// that write LValue from the LIM. If that LValue is
		// owned by a loop body, then force loop unrolling to run
		// on that loop (ie, set a local preference for that loop
		// to turn on loop unrolling)
		// 3.2 Also remove the blockElement from the Allocation
		// 3.3 Also, clear the stream format of that block element

		final Set<BlockAllocationSet> blockElements = buildSets(design);
		// Track ALL LValues. We want to look at them later to see
		// which BlockElements are still being used.
		final Set<LValue> allLValues = new HashSet<LValue>();
		// Track deleted LValues. We will use these to ignore any
		// lvalues in the ALL list when determining which
		// BlockElements are still being used.
		final Set<LValue> deletedLValues = new HashSet<LValue>();

		for (BlockAllocationSet set : blockElements) {
			final BlockElement element = set.getBlockElement();
			final boolean isInputIF = element.getBlockDescriptor().isSlave();

			final Set<LValue> accessingLValues = set.getAccessingLValues();
			allLValues.addAll(accessingLValues);

			if (isInputIF) {
				// Then we can look to delete any read LValues that
				// are part of a block interface.
				if (set.isInternalReadOnly()) {
					for (LValue lvalue : accessingLValues) {
						if (lvalue.isWrite()) {
							continue;
						}
						// By not testing that that the lvalue block
						// element is != current element we are
						// assuming that for a given block element
						// there is only 1 direction of transfer.
						if (lvalue.getBlockElement() != null) {
							assert lvalue.getBlockElement() != element;
							delete(lvalue);
							deletedLValues.add(lvalue);
						}
					}
				}
			} else {
				// Then we can look to delete any write LValues that
				// are part of a block interface.
				if (EngineThread.getGenericJob().getUnscopedBooleanOptionValue(
						OptionRegistry.WRITE_ONLY_INPUT_PARAM_OPT)) {
					if (set.isInternalWriteOnly() || set.isInternalWriteFirst()) {
						for (LValue lvalue : accessingLValues) {
							if (!lvalue.isWrite()) {
								continue;
							}
							if (lvalue.getBlockElement() != null) {
								assert lvalue.getBlockElement() != element;
								delete(lvalue);
								deletedLValues.add(lvalue);
							}
						}
					}
				}
			}
		}

		// Now figure out which BlockElements we deleted and clear
		// them out.
		allLValues.removeAll(deletedLValues);
		final Set<BlockElement> blockElementsWithLValues = new HashSet<BlockElement>();
		for (LValue lvalue : allLValues) {
			if (lvalue.getBlockElement() != null) {
				blockElementsWithLValues.add(lvalue.getBlockElement());
			}
		}
		// Now that we know which blockElements still have lvalue
		// accesses (in the IO interface sections) we can clear out
		// the others.
		for (BlockAllocationSet baSet : blockElements) {
			if (!blockElementsWithLValues.contains(baSet.getBlockElement())) {
				baSet.deleteElement();
				deletedBlockElements.add(baSet.getBlockElement());
			}
		}
	}

	/**
	 * Builds a Set of BlockAllocationSet objects for the memories in the
	 * design.
	 * 
	 * @param design
	 *            a value of type 'Design'
	 * @return a Set of BlockAllocationSet objects.
	 */
	private Set<BlockAllocationSet> buildSets(Design design) {
		final Map<BlockElement, BlockAllocationSet> blockElements = new HashMap<BlockElement, BlockAllocationSet>();
		for (LogicalMemory mem : design.getLogicalMemories()) {
			for (Allocation alloc : mem.getAllocations()) {
				for (BlockElement element : alloc.getBlockElements()) {
					BlockAllocationSet set = blockElements.get(element);
					if (set == null) {
						set = new BlockAllocationSet(element);
						blockElements.put(element, set);
					}
					set.add(alloc);
				}
			}
		}

		return new HashSet<BlockAllocationSet>(blockElements.values());
	}

	/**
	 * Utility method for deleting an LValue and taking care of all the details.
	 * 
	 * @param lvalue
	 *            a value of type 'LValue'
	 */
	private void delete(LValue lvalue) {
		assert lvalue instanceof MemoryAccessBlock;
		assert lvalue.getBlockElement() != null;
		MemoryAccessBlock mab = (MemoryAccessBlock) lvalue;

		// The IO lvalues should be contained in a block, in a loop
		// body, which is in a loop. If that is the case, set that
		// loop to be forced to unroll. Then we will realize that the
		// entire contents of the loop (including an incrementing
		// pointer) are useless and we should throw it all away.
		try {
			if (mab.getOwner().getOwner().getOwner() instanceof Loop) {
				((Loop) mab.getOwner().getOwner().getOwner())
						.setForceUnroll(true);
			}
		} catch (NullPointerException e) {
			EngineThread
					.getGenericJob()
					.warn("Unexpected internal state while deleting unused memory access.  Recovered normally.  ");
		}

		// Delete any dependencies on the exit's done bus.
		for (Dependency dependency : mab.getExit(Exit.DONE).getDoneBus()
				.getLogicalDependents()) {
			dependency.zap();
		}
		ComponentSwapVisitor.wireControlThrough(mab);
		ComponentSwapVisitor.removeComp(mab);
		// Remove the lvalue from its target memory.
		mab.removeFromMemory();
	}

	/**
	 * Clears the status and internal state of this Optimization.
	 */
	@Override
	public void clear() {
		deletedBlockElements.clear();
	}

	/**
	 * Reports, via the {@link Job} reporting mechanisms any relevent
	 * information prior to running this optimization.
	 */
	@Override
	public void preStatus() {
		EngineThread.getGenericJob().info("Optimizing interface streams...");
	}

	/**
	 * Reports, via the {@link Job} reporting mechanisms any relevent
	 * information subsequent to running this optimization.
	 */
	@Override
	public void postStatus() {
		for (BlockElement element : deletedBlockElements) {
			EngineThread.getGenericJob().verbose(
					"Deleted '"
							+ element.getFormalName()
							+ "' from "
							+ (element.getBlockDescriptor().isSlave() ? "input"
									: "output") + " stream");
		}
	}

	/**
	 * Returns true if the current run of this optimization made any change to
	 * the LIM. That is, returns true if any LValue accesses were deleted.
	 * 
	 * @return true if the LIM structure was modified by this optimization
	 */
	@Override
	public boolean didModify() {
		return deletedBlockElements.size() > 0;
	}

}// BlockElementRemover
