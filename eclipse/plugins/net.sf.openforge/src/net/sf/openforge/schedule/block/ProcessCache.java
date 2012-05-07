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

package net.sf.openforge.schedule.block;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import net.sf.openforge.app.GenericJob;
import net.sf.openforge.app.OptionRegistry;
import net.sf.openforge.lim.Component;
import net.sf.openforge.lim.Module;
import net.sf.openforge.lim.Stallboard;
import net.sf.openforge.lim.Visitable;

/**
 * ProcessCache provides utilities to the scheduler for determining the
 * relationship of components to the identified processes.
 * 
 * <p>
 * Created: Tue Sep 21 08:37:56 2004
 * 
 * @author imiller, last modified by $Author: imiller $
 * @version $Id: ProcessCache.java 2 2005-06-09 20:00:48Z imiller $
 */
public class ProcessCache {

	/** A set of MemProcess objects that have been identified */
	private Set<MemProcess> processes;

	/**
	 * A Map of component owner (Module) to ProcessTracker object for that
	 * module.
	 */
	private final Map<Module, ProcessTracker> trackers = new HashMap<Module, ProcessTracker>();

	public ProcessCache(Visitable vis, GenericJob job) {
		// If we are not doing a block IO based design, then generate
		// no processes and (consequently) dont do any new scheduling
		if (job.getUnscopedBooleanOptionValue(OptionRegistry.SCHEDULE_NO_BLOCK_SCHEDULING)) {
			processes = Collections.emptySet();
		} else {
			processes = Collections.unmodifiableSet(new HashSet<MemProcess>(
					ProcessIdentifier.generateProcesses(vis).values()));
		}
	}

	/**
	 * Returns a Set of all {@link MemProcess} objects identified in this cache.
	 * 
	 * @return a Set of MemProcess objects.
	 */
	public Set<MemProcess> getProcesses() {
		return processes;
	}

	/**
	 * Returns true if the given Component is a critical start point for one or
	 * more processes.
	 * 
	 * @param comp
	 *            a value of type 'Component'
	 * @return a value of type 'boolean'
	 */
	public boolean isCriticalStartPoint(Component comp) {
		if (comp == null)
			throw new IllegalArgumentException(
					"Cannot determine if null is critical start point");

		for (MemProcess memProc : processes) {
			if (memProc.isStartPoint(comp)) {
				return true;
			}
		}

		return false;
	}

	/**
	 * Annotates the cached processes for the given Component with the
	 * stallboard which is the stall point for that process.
	 * 
	 * @param comp
	 *            a value of type 'Component'
	 * @param scbd
	 *            a value of type 'Stallboard'
	 */
	public void registerStartPoint(Component comp, Stallboard stbd) {
		for (MemProcess memProc : processes) {
			if (memProc.isStartPoint(comp)) {
				memProc.setStallPoint(comp, stbd);
			}
		}
	}

	/**
	 * Retrieves the ProcessTracker object that has been allocated to track
	 * process scheduling for the given module. It is an error to request a
	 * tracker for a module after having deleted the tracker via the
	 * {@link deleteTracker} method.
	 * 
	 * @param module
	 *            a non-null Module
	 * @return a non-null ProcessTracker. One will be created if this is the
	 *         first call to getTracker with the specified module.
	 * @throws IllegalArgumentException
	 *             if module is null or tracker has been deleted.
	 */
	public ProcessTracker getTracker(Module module) {
		if (module == null) {
			throw new IllegalArgumentException(
					"Cannot request tracker for null module");
		}
		if (deleted.contains(module)) {
			throw new IllegalArgumentException(
					"Cannot request tracker after deleting module tracker");
		}

		// If it already has been requested, then return the cached version
		if (trackers.containsKey(module)) {
			return trackers.get(module);
		}

		final ProcessTracker tracker = new ProcessTracker(processes);
		trackers.put(module, tracker);
		return tracker;
	}

	/*
	 * A Set of modules which have had a tracker allocated and which is now
	 * deleted
	 */
	private final Set<Module> deleted = new HashSet<Module>();

	/**
	 * Deletes the ProcessTracker which was created for the given module. It is
	 * an error to delete a tracker multiple times, or to delete a tracker for a
	 * module which has not had a tracker allocated via the {@link getTracker}
	 * method.
	 * 
	 * @param module
	 *            a value of type 'Module'
	 */
	public void deleteTracker(Module module) {
		assert trackers.containsKey(module);
		trackers.remove(module);
	}

}// ProcessCache
