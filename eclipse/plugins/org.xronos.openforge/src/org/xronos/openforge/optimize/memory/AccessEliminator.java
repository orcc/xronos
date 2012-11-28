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

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;


import org.eclipse.core.runtime.jobs.Job;
import org.xronos.openforge.lim.Access;
import org.xronos.openforge.lim.DefaultVisitor;
import org.xronos.openforge.lim.Design;
import org.xronos.openforge.lim.Visitable;
import org.xronos.openforge.lim.memory.LogicalMemory;
import org.xronos.openforge.lim.memory.MemoryAccess;
import org.xronos.openforge.lim.memory.MemoryRead;
import org.xronos.openforge.lim.memory.MemoryWrite;
import org.xronos.openforge.optimize.Optimization;

/**
 * AccessEliminator deletes from all memories, in any visited Design, any
 * {@link Access} which is not found in the LIM contained within that
 * {@link Design}. This is used to clean up after merging a library design with
 * the main design which may leave accesses to the memory which exist only in
 * library methods that were not brought over to the design.
 * 
 * <p>
 * Created: Wed Apr 9 17:01:47 2003
 * 
 * @author imiller, last modified by $Author: imiller $
 * @version $Id: AccessEliminator.java 2 2005-06-09 20:00:48Z imiller $
 */
public class AccessEliminator extends DefaultVisitor implements Optimization {

	/**
	 * A Set of Access objects that were found when traversing the LIM
	 */
	private Set<MemoryAccess> accessesInLIM = new HashSet<MemoryAccess>();

	public AccessEliminator() {
	}

	/**
	 * Applies this optimization to a given target.
	 * 
	 * @param target
	 *            the target on which to run this optimization
	 */
	@Override
	public void run(Visitable target) {
		target.accept(this);
	}

	/**
	 * Traverses the LIM recording any memory access, then remove any accesses
	 * that the memory knows about that was not found in the LIM.
	 */
	@Override
	public void visit(Design design) {
		for (Iterator<LogicalMemory> iter = design.getLogicalMemories()
				.iterator(); iter.hasNext();) {
			// TBD
			iter.next();
		}
	}

	@Override
	public void visit(MemoryRead acc) {
		super.visit(acc);
		accessesInLIM.add(acc);
	}

	@Override
	public void visit(MemoryWrite acc) {
		super.visit(acc);
		accessesInLIM.add(acc);
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
	 * Clears out the set of Access objects found in the LIM.
	 */
	@Override
	public void clear() {
		accessesInLIM.clear();
	}

	/**
	 * Reports, via {@link Job#info}, what optimization is being performed, in
	 * this case, nothing is reported to the user.
	 */
	@Override
	public void preStatus() {
		// Job.info("reducing expressions with constants...");
	}

	/**
	 * Reports, via {@link Job#verbose}, the results of <b>this</b> pass of the
	 * optimization.
	 */
	@Override
	public void postStatus() {
		// Job.verbose("reduced " + getReplacedNodeCount() + " expressions");
	}

}// AccessEliminator
