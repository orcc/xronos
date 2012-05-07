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

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import net.sf.openforge.lim.Branch;
import net.sf.openforge.lim.Component;
import net.sf.openforge.lim.DataFlowVisitor;
import net.sf.openforge.lim.RegisterRead;
import net.sf.openforge.lim.RegisterWrite;
import net.sf.openforge.lim.StateAccessor;
import net.sf.openforge.lim.StateHolder;
import net.sf.openforge.lim.TaskCall;
import net.sf.openforge.lim.Visitable;
import net.sf.openforge.lim.io.FifoAccess;
import net.sf.openforge.lim.io.FifoRead;
import net.sf.openforge.lim.io.FifoWrite;
import net.sf.openforge.lim.io.SimplePinAccess;
import net.sf.openforge.lim.memory.MemoryRead;
import net.sf.openforge.lim.memory.MemoryWrite;

/**
 * ProcessIdentifier is a helper class used to generate a collection of
 * processes that need to be scheduled against. This class identifies processes
 * by analyzing the sequence of accesses to state bearing resources such as
 * memories and registers. The process endpoints are defined to be the first and
 * last access to each resource.
 * 
 * <p>
 * Created: Wed Sep 1 08:25:22 2004
 * 
 * @author imiller, last modified by $Author: imiller $
 * @version $Id: ProcessIdentifier.java 122 2006-03-30 18:05:17Z imiller $
 */
public class ProcessIdentifier extends DataFlowVisitor {

	private ProcessIdentifier() {
		super();
		setRunForward(true);
	}

	/**
	 * Generates a map of StateHolder key to MemProcess value. The StateHolder
	 * is the 'reason' for the process and the MemProcess identifies the
	 * endpoints of the process (first and last accesses) and the critical
	 * context for these endpoints.
	 * 
	 * @param vis
	 *            a value of type 'Visitable'
	 * @return a Map of {@link StateHolder} to {@link MemProcess}
	 */
	public static Map<StateHolder, MemProcess> generateProcesses(Visitable vis) {
		ProcessIdentifier pid = new ProcessIdentifier();
		vis.accept(pid);

		Map<StateHolder, MemProcess> processMap = new HashMap<StateHolder, MemProcess>();
		Set<StateHolder> keys = pid.getProcessKeys();
		for (Iterator<StateHolder> iter = keys.iterator(); iter.hasNext();) {
			Object key = iter.next();

			Set start = (Set) pid.stateToFirst.get(key);
			Set end = (Set) pid.stateToLast.get(key);
			assert start != null && start.size() > 0 : "No valid start point found for process";
			assert end != null && end.size() > 0 : "No valid end point found for process";

			// If the start and end point are the same, then there is
			// no process, it is just a single access to memory that
			// needs no stalling.
			if (!start.equals(end)) {
				MemProcess process = new MemProcess(start, end);
				if (_block.db)
					_block.ln("Process " + process.debug());
				processMap.put((StateHolder) key, process);
			}
		}

		return processMap;
		// return Collections.EMPTY_MAP;
	}

	private Map<StateHolder, Collection<StateAccessor>> stateToFirst = new HashMap<StateHolder, Collection<StateAccessor>>();
	private Map<StateHolder, Collection<StateAccessor>> stateToLast = new HashMap<StateHolder, Collection<StateAccessor>>();
	/**
	 * A set of StateHolder objects that have been identified such that as we
	 * reach new accesses the 'firsts' should not be aggregated. That is, any
	 * StateHolder identified here has only 1 call to the 'register' method to
	 * register first accesses. If the stateID is not in here, then each call to
	 * register simply adds more potential first accesses to the set of
	 * possibles. This is to handle register reads which have no inter
	 * dependencies.
	 */
	private Set<StateHolder> nonAggregating = new HashSet<StateHolder>();

	/**
	 * Registers the access as the first access iff no other access to the given
	 * target has been registered, and registers the access as the last access,
	 * always overwriting any prior last access.
	 * 
	 * @param acc
	 *            a non-null StateAccessor
	 */
	private void register(StateAccessor acc) {
		StateHolder stateID = acc.getStateHolder();
		register(stateID, Collections.singleton(acc));
	}

	private void register(StateHolder stateID,
			Collection<StateAccessor> accessors) {
		if (_block.db)
			_block.ln("Identified " + stateID + " <= " + accessors);
		final boolean aggregateFirsts = !nonAggregating.contains(stateID);
		if (!aggregateFirsts) {
			if (!stateToFirst.containsKey(stateID)) {
				stateToFirst.put(stateID, accessors);
			}
		} else {
			Collection<StateAccessor> firsts = stateToFirst.get(stateID);
			if (firsts == null) {
				firsts = new HashSet<StateAccessor>();
			}
			firsts.addAll(accessors);
			stateToFirst.put(stateID, firsts);
		}

		stateToLast.put(stateID, accessors);
	}

	private Set<StateHolder> getProcessKeys() {
		Set<StateHolder> keys = new HashSet<StateHolder>();
		keys.addAll(stateToFirst.keySet());
		keys.addAll(stateToLast.keySet());
		return keys;
	}

	@Override
	public void visit(TaskCall mod) {
		// Handle the task call by registering the call as an accessor
		// of all resources in the called task.
		ProcessIdentifier pid = new ProcessIdentifier();
		mod.getTarget().accept(pid);
		for (Iterator<StateHolder> iter = pid.getProcessKeys().iterator(); iter
				.hasNext();) {
			Object key = iter.next();
			Set<StateAccessor> targets = new HashSet<StateAccessor>();
			if (pid.stateToFirst.containsKey(key))
				targets.addAll(pid.stateToFirst.get(key));
			if (pid.stateToLast.containsKey(key))
				targets.addAll(pid.stateToLast.get(key));
			register((StateHolder) key, Collections.unmodifiableSet(targets));
		}
		nonAggregating.addAll(pid.nonAggregating);
	}

	@Override
	public void visit(SimplePinAccess point) {
		nonAggregating.add(point.getStateHolder());
		register(point);
	}

	@Override
	public void visit(FifoAccess point) {
		nonAggregating.add(point.getStateHolder());
		register(point);
	}

	@Override
	public void visit(FifoRead point) {
		nonAggregating.add(point.getStateHolder());
		register(point);
	}

	@Override
	public void visit(FifoWrite point) {
		nonAggregating.add(point.getStateHolder());
		register(point);
	}

	@Override
	public void visit(MemoryRead point) {
		nonAggregating.add(point.getStateHolder());
		register(point);
	}

	@Override
	public void visit(MemoryWrite point) {
		nonAggregating.add(point.getStateHolder());
		register(point);
	}

	/**
	 * Any register read which comes before a register write is potentially a
	 * 'first' access because they do not depend on one another. So, we will
	 * continue to 'aggregate' them until we hit a register write (to the same
	 * register).
	 * <p>
	 * NOTE: This will work so long as we hit all the register reads before
	 * descending into a sub-module which contains a register write. I fear that
	 * our data flow visitor may not uphold this contract for us. If it does not
	 * (ie if it descends into a sub-module and then visits another register
	 * read afterwards where that next register read does not depend on the
	 * traversed module) then we need to modify the data flow visitor to ensure
	 * that it traverses ALL components with satisfied dependencies before it
	 * traverses any contained modules.
	 * 
	 * @param point
	 *            a value of type 'RegisterRead'
	 */
	@Override
	public void visit(RegisterRead point) {
		// Register reads do not add the register to the
		// nonAggregating set because they do not depend on each
		// other. Thus we need to 'aggregate' them all as 'first'
		// accesses until we get to a register write at which point we
		// have found all the possible firsts.
		register(point);
	}

	@Override
	public void visit(RegisterWrite point) {
		nonAggregating.add(point.getStateHolder());
		register(point);
	}

	/**
	 * Visit the true and false branches. Register any targetted resource found
	 * in either branch as joined. Thus the last found will be a set of the
	 * lasts from each branch.
	 * 
	 * @param branch
	 *            a value of type 'Branch'
	 */
	@Override
	public void visit(Branch branch) {
		Set<Component> components = new HashSet<Component>(
				branch.getComponents());
		components.remove(branch.getDecision());
		components.remove(branch.getTrueBranch());
		components.remove(branch.getFalseBranch());
		components.remove(branch.getInBuf());
		branch.getInBuf().accept(this);
		branch.getDecision().accept(this);
		ProcessIdentifier trueIdentifier = new ProcessIdentifier();
		branch.getTrueBranch().accept(trueIdentifier);
		ProcessIdentifier falseIdentifier = new ProcessIdentifier();
		branch.getFalseBranch().accept(falseIdentifier);

		Set<StateHolder> keys = new HashSet<StateHolder>();
		keys.addAll(trueIdentifier.getProcessKeys());
		keys.addAll(falseIdentifier.getProcessKeys());
		for (Iterator<StateHolder> iter = keys.iterator(); iter.hasNext();) {
			Object key = iter.next();
			Set<StateAccessor> targets = new HashSet<StateAccessor>();
			if (trueIdentifier.stateToFirst.containsKey(key))
				targets.addAll(trueIdentifier.stateToFirst.get(key));
			if (falseIdentifier.stateToFirst.containsKey(key))
				targets.addAll(falseIdentifier.stateToFirst.get(key));

			if (trueIdentifier.stateToLast.containsKey(key))
				targets.addAll(trueIdentifier.stateToLast.get(key));
			if (falseIdentifier.stateToLast.containsKey(key))
				targets.addAll(falseIdentifier.stateToLast.get(key));

			register((StateHolder) key, Collections.unmodifiableSet(targets));
		}
		nonAggregating.addAll(trueIdentifier.nonAggregating);
		nonAggregating.addAll(falseIdentifier.nonAggregating);
	}

	@Override
	public String toString() {
		String ret = super.toString();
		return ret.substring(ret.lastIndexOf(".") + 1);
	}

}// ProcessIdentifier
