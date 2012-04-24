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

import java.util.HashSet;
import java.util.Set;

import net.sf.openforge.lim.Task;

/**
 * TaskCache is a simple and convenient way to keep track of scheduled Tasks and
 * those Tasks which are pending scheduling.
 * 
 * Created: Tue May 14 10:23:00 2002
 * 
 * @author imiller
 * @version $Id: TaskCache.java 88 2006-01-11 22:39:52Z imiller $
 */
public class TaskCache {

	private final Set<Task> scheduledTasks = new HashSet<Task>();
	private final Set<Task> pendingTasks = new HashSet<Task>();

	public TaskCache() {
	}

	public boolean isScheduled(Task task) {
		return this.scheduledTasks.contains(task);
	}

	public void startTask(Task task) {
		// This test will catch the case where the LIM is cyclic, with
		// 2 tasks depending on one another.

		// if (this.scheduledTasks.contains(task)
		// || this.pendingTasks.contains(task))
		// throw new IllegalArgumentException(
		// "Marking task as pending after it has been started or completed");

		this.pendingTasks.add(task);
	}

	public void completeTask(Task task) {
		if (!this.pendingTasks.contains(task))
			throw new IllegalArgumentException(
					"Attempt to complete scheduling of task which was not pending");
		this.pendingTasks.remove(task);
		this.scheduledTasks.add(task);
	}
}
