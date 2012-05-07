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

package net.sf.openforge.schedule;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import net.sf.openforge.lim.Access;
import net.sf.openforge.lim.DefaultVisitor;
import net.sf.openforge.lim.Design;
import net.sf.openforge.lim.Referent;
import net.sf.openforge.lim.Resource;
import net.sf.openforge.lim.Task;
import net.sf.openforge.lim.memory.LogicalMemory;
import net.sf.openforge.lim.memory.LogicalMemoryPort;
import net.sf.openforge.lim.memory.MemoryRead;
import net.sf.openforge.lim.memory.MemoryWrite;

/**
 * A small visitor used to traverse a design and count the types and numbers of
 * accesses in each task, then use that information to annotate global resources
 * with information used during scheduling.
 * 
 * <p>
 * Created: Fri Jul 26 12:19:04 2002
 * 
 * @author imiller
 * @version $Id: AccessCounter.java 2 2005-06-09 20:00:48Z imiller $
 */
public class AccessCounter extends DefaultVisitor {

	/** A map of Resource -> Task's that access that resource. */
	private Map<Resource, Set<Task>> resourceToTasks = new HashMap<Resource, Set<Task>>();

	/** The current task being traversed. */
	private Task currentTask = null;

	/**
	 * Traverses the design to characterize which {@link Referent}s are accessed
	 * from each task and updates the Referent's according to that information.
	 * 
	 * @param design
	 *            a value of type 'Design'
	 */
	@Override
	public void visit(Design design) {
		super.visit(design);

		for (LogicalMemory memory : design.getLogicalMemories()) {
			for (LogicalMemoryPort resource : memory.getLogicalMemoryPorts()) {
				Set<Task> accessingTasks = resourceToTasks.get(resource);
				resource.setArbitrated(accessingTasks == null ? false
						: (accessingTasks.size() > 1));
			}
		}
	}

	@Override
	public void visit(Task task) {
		currentTask = task;
		super.visit(task);
	}

	@Override
	public void visit(MemoryRead memRead) {
		super.visit(memRead);
		addAccess(memRead);
	}

	@Override
	public void visit(MemoryWrite memWrite) {
		super.visit(memWrite);
		addAccess(memWrite);
	}

	private void addAccess(Access access) {
		Resource resource = access.getResource();
		Set<Task> tasks = resourceToTasks.get(resource);
		if (tasks == null) {
			tasks = new HashSet<Task>();
			resourceToTasks.put(resource, tasks);
		}
		tasks.add(currentTask);
	}

}// AccessCounter
