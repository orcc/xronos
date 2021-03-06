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

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.xronos.openforge.lim.memory.AbsoluteMemoryRead;
import org.xronos.openforge.lim.memory.AbsoluteMemoryWrite;
import org.xronos.openforge.lim.memory.LocationConstant;


/**
 * Uniquifier is a {@link Visitor} which simply resets the {@link Procedure} of
 * each {@link Call} to be the clone of its current {@link Procedure}. This
 * ensures that each {@link Call} has its own copy of the {@link Procedure}.
 * 
 * @version $Id: Uniquifier.java 23 2005-09-09 18:45:32Z imiller $
 */
public class Uniquifier {

	/**
	 * Ensures that each {@link Call} in a given {@link Design} hierarchy
	 * invokes its own cloned copy of the called {@link Procedure}.
	 */
	public static void uniquify(Design design) {
		final Collection<Call> deadCalls = uniquifyTaskCalls(design);
		ReferenceCleaner.clean(deadCalls);
	}

	/**
	 * Clones each {@link Task Task's} top level {@link Call}, which recursively
	 * clones the entire calling hierarchy, making each one unique.
	 * 
	 * @param design
	 *            the design to be uniquified
	 * @return the replaced calls, which are now obsolete
	 */
	private static Collection<Call> uniquifyTaskCalls(Design design) {
		final List<Call> oldCalls = new LinkedList<Call>();
		try {
			for (Task task : design.getTasks()) {
				final Call oldCall = task.getCall();

				// ensures no dependencies that we would need to copy
				if (!oldCall.getEntries().isEmpty()) {
					throw new IllegalStateException(
							"Top call has dependencies too early");
				}
				for (Port port : oldCall.getPorts()) {
					if (port.isConnected()) {
						throw new IllegalStateException(
								"Top call port connected too early");
					}
				}
				for (Bus bus : oldCall.getBuses()) {
					if (!bus.getLogicalDependents().isEmpty()
							|| bus.isConnected()) {
						throw new IllegalStateException(
								"Top call bus connected too early");
					}
				}

				Call newCall = (Call) oldCall.clone();
				task.setCall(newCall);
				design.getDesignModule().replaceComponent(oldCall, newCall);
				oldCalls.add(oldCall);
			}
		} catch (CloneNotSupportedException eClone) {
			assert false : eClone;
		}
		return oldCalls;
	}

	private Uniquifier() {
	}
}

/**
 * Removes dead references to global resources.
 * 
 * @version $Id: Uniquifier.java 23 2005-09-09 18:45:32Z imiller $
 */
class ReferenceCleaner extends DefaultVisitor {

	private Collection<Call> visitedCalls = new HashSet<Call>();

	private Set<Component> removedRefs = new HashSet<Component>();

	/**
	 * Unlinks all {@link Reference References} found in a hierarchy of
	 * {@link Call Calls} by removing the reference from its {@link Referent}.
	 * 
	 * @param calls
	 *            a collection of dead top-level {@link Call Calls}
	 */
	static void clean(Collection<Call> calls) {
		final ReferenceCleaner cleaner = new ReferenceCleaner();
		for (Call call : calls) {
			call.accept(cleaner);
		}
	}

	@Override
	public void visit(Call call) {
		if (!visitedCalls.contains(call)) {
			visitedCalls.add(call);
			super.visit(call);
		}
	}

	@Override
	public void visit(RegisterRead ref) {
		super.visit(ref);
		removeRef(ref);
	}

	@Override
	public void visit(RegisterWrite ref) {
		super.visit(ref);
		removeRef(ref);
	}

	@Override
	public void visit(HeapRead heapRead) {
		removeMemoryAccessBlock(heapRead);
	}

	@Override
	public void visit(HeapWrite heapWrite) {
		removeMemoryAccessBlock(heapWrite);
	}

	@Override
	public void visit(ArrayRead arrayRead) {
		removeMemoryAccessBlock(arrayRead);
	}

	@Override
	public void visit(ArrayWrite arrayWrite) {
		removeMemoryAccessBlock(arrayWrite);
	}

	@Override
	public void visit(AbsoluteMemoryRead read) {
		removeMemoryAccessBlock(read);
	}

	@Override
	public void visit(AbsoluteMemoryWrite write) {
		removeMemoryAccessBlock(write);
	}

	@Override
	public void visit(LocationConstant locConst) {
		locConst.removeFromMemory();
	}

	private void removeMemoryAccessBlock(MemoryAccessBlock memoryAccessBlock) {
		if (!removedRefs.contains(memoryAccessBlock)) {
			memoryAccessBlock.removeFromMemory();
			removedRefs.add(memoryAccessBlock);
		}
	}

	/*
	 * Pin accesses are dynamic, so they never refer directly to their
	 * referents.
	 */

	private void removeRef(Reference ref) {
		if (!removedRefs.contains(ref)) {
			ref.getReferent().removeReference(ref);
			removedRefs.add(ref);
		}
	}
}
