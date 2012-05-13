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

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import net.sf.openforge.lim.ArrayRead;
import net.sf.openforge.lim.ArrayWrite;
import net.sf.openforge.lim.Block;
import net.sf.openforge.lim.Branch;
import net.sf.openforge.lim.Bus;
import net.sf.openforge.lim.Call;
import net.sf.openforge.lim.Component;
import net.sf.openforge.lim.Decision;
import net.sf.openforge.lim.DefaultVisitor;
import net.sf.openforge.lim.Entry;
import net.sf.openforge.lim.Exit;
import net.sf.openforge.lim.ForBody;
import net.sf.openforge.lim.HeapRead;
import net.sf.openforge.lim.HeapWrite;
import net.sf.openforge.lim.Loop;
import net.sf.openforge.lim.Module;
import net.sf.openforge.lim.PinRead;
import net.sf.openforge.lim.PinStateChange;
import net.sf.openforge.lim.PinWrite;
import net.sf.openforge.lim.Referenceable;
import net.sf.openforge.lim.Referencer;
import net.sf.openforge.lim.RegisterRead;
import net.sf.openforge.lim.RegisterWrite;
import net.sf.openforge.lim.Resource;
import net.sf.openforge.lim.ResourceDependency;
import net.sf.openforge.lim.Switch;
import net.sf.openforge.lim.TaskCall;
import net.sf.openforge.lim.UntilBody;
import net.sf.openforge.lim.WhileBody;
import net.sf.openforge.lim.io.FifoAccess;
import net.sf.openforge.lim.io.FifoRead;
import net.sf.openforge.lim.io.FifoWrite;
import net.sf.openforge.lim.io.SimplePinAccess;
import net.sf.openforge.lim.io.SimplePinRead;
import net.sf.openforge.lim.io.SimplePinWrite;
import net.sf.openforge.lim.memory.AbsoluteMemoryRead;
import net.sf.openforge.lim.memory.AbsoluteMemoryWrite;
import net.sf.openforge.lim.memory.MemoryRead;
import net.sf.openforge.lim.memory.MemoryWrite;

/**
 * The GlobalResourceSequencer inserts {@link ResourceDependency
 * ResourceDependencies} between {@link Component Components} that access the
 * same global {@link Resource} where those components might otherwise execute
 * in parallel, i.e., when they appear in a {@link Block}. These components
 * could be low level operations or {@link Module Modules} that contain those
 * operations.
 * 
 * GlobalResourceSequencer works with {@link Referenceable} and
 * {@link Referencer} objects to insert the necessary {@link ResourceDependency}
 * between {@link Components} (implementers of {@link Referencer}) which share a
 * common {@link Referenceable} target. This ensures that the scheduling
 * attributes dictated by the {@link Referenceable} target are upheld where the
 * components may otherwise execute in parallel or lose the intended sequence of
 * accesses due to a lack of either data or control dependencies.
 * 
 * @version $Id: GlobalResourceSequencer.java 538 2007-11-21 06:22:39Z imiller $
 */
public class GlobalResourceSequencer extends DefaultVisitor {

	private static final boolean debug = false;

	/**
	 * An association between one {@link Component} that is the source of a
	 * control signal and another {@link Component} that is the target of the
	 * control signal. Will be used as the key in the <code>controlExits</code>
	 * map to record the {@link Exit} that should be used to implement the
	 * control connection between these two.
	 */
	private static class ControlPair {
		/** The source of a DONE signal */
		private Component source;

		/** The target of the DONE signal */
		private Component target;

		/**
		 * Creates a new <code>ControlPair</code> instance.
		 * 
		 * @param source
		 *            the source of a DONE signal
		 * @param target
		 *            the target of the DONE signal
		 */
		ControlPair(Component source, Component target) {
			this.source = source;
			this.target = target;
		}

		@Override
		public int hashCode() {
			return source.hashCode() + target.hashCode();
		}

		@Override
		public boolean equals(Object object) {
			if (object instanceof ControlPair) {
				final ControlPair pair = (ControlPair) object;
				return (pair.source == source) && (pair.target == target);
			}
			return false;
		}
	}

	/**
	 * Define a Referenceable to to which all volatile accesses are mapped.
	 */
	private static final Referenceable VOLATILE = new Referenceable() {
		@Override
		public int getSpacing(Referencer from, Referencer to) {
			return 0;
		}

		@Override
		public int getGoSpacing(Referencer from, Referencer to) {
			return -1;
		}
	};

	/** Top level access tracker */
	private AccessTracker tracker = new AccessTracker();

	/**
	 * Map of ControlPair to Exit, which shows which Exit of a source Component
	 * should be used to derive the control signal for a target Component
	 */
	private Map<ControlPair, Exit> controlExits = new HashMap<ControlPair, Exit>();

	public GlobalResourceSequencer() {
		super();
	}

	// public void visit (Task task)
	// {
	// super.visit(task);
	// _schedule.d.launchGraph(task);
	// try{System.out.println("SLEEP");Thread.sleep(3000);}catch(Exception e){}
	// }

	@Override
	public void visit(Call call) {
		if (debug)
			System.out.println("CALL " + call);
		AccessTracker outerTracker = tracker;
		tracker = new AccessTracker();
		call.getProcedure().getBody().accept(this);
		outerTracker.propagateUp(tracker, call);
		tracker = outerTracker;
	}

	@Override
	public void visit(Block block) {
		processModule(block);
	}

	@Override
	public void visit(Decision decision) {
		processModule(decision);
	}

	@Override
	public void visit(Switch swich) {
		visit((Block) swich);
	}

	@Override
	public void visit(HeapRead heapRead) {
		visit((Block) heapRead);
	}

	@Override
	public void visit(ArrayRead arrayRead) {
		visit((Block) arrayRead);
	}

	@Override
	public void visit(AbsoluteMemoryRead mod) {
		visit((Block) mod);
	}

	@Override
	public void visit(HeapWrite heapWrite) {
		visit((Block) heapWrite);
	}

	@Override
	public void visit(ArrayWrite arrayWrite) {
		visit((Block) arrayWrite);
	}

	@Override
	public void visit(AbsoluteMemoryWrite mod) {
		visit((Block) mod);
	}

	@Override
	public void visit(SimplePinAccess mod) {
		processModule(mod);
	}

	@Override
	public void visit(TaskCall mod) {
		processModule(mod);
	}

	private void processModule(Module module) {
		AccessTracker outerTracker = tracker;
		tracker = new AccessTracker();

		/*
		 * Travese the sequence of Components.
		 */
		Collection<Component> comps = (module instanceof Block) ? ((Block) module)
				.getSequence() : module.getComponents();
		for (Component component : comps) {
			component.accept(this);
			addDependencies(component, tracker);
		}

		outerTracker.propagateUp(tracker, module);
		tracker = outerTracker;
	}

	@Override
	public void visit(Branch branch) {
		AccessTracker outerTracker = tracker;

		tracker = new AccessTracker();

		/*
		 * Record which Exit drives which branch.
		 */
		final Decision decision = branch.getDecision();
		addControlExit(decision, branch.getTrueBranch(), decision.getTrueExit());
		addControlExit(decision, branch.getFalseBranch(),
				decision.getFalseExit());

		decision.accept(this);

		AccessTracker save = tracker;
		final AccessTracker trueTracker = new AccessTracker();
		tracker = trueTracker;
		branch.getTrueBranch().accept(this);
		tracker = save;

		branch.getFalseBranch().accept(this);
		save = tracker;
		final AccessTracker falseTracker = new AccessTracker();
		tracker = falseTracker;
		branch.getFalseBranch().accept(this);
		tracker = save;

		// Add the dependencies before propagating either the true or
		// false blocks up. This way they do not depend on each
		// other, but rather on the decision block.
		addDependencies(branch.getTrueBranch(), tracker, trueTracker);
		addDependencies(branch.getFalseBranch(), tracker, falseTracker);

		// This makes it look like all the accesses happen in the true
		// branch. But that is OK because all we are concerned with
		// is the existance of accesses and what their type is.
		// this.tracker.propagateUp(trueTracker, branch.getTrueBranch());
		// this.tracker.propagateUp(trueTracker, branch.getFalseBranch());
		AccessTracker merged = trueTracker.mergeTracker(falseTracker);
		tracker.propagateUp(merged, branch.getTrueBranch());
		outerTracker.propagateUp(tracker, branch);

		tracker = outerTracker;
	}

	@Override
	public void visit(Loop loop) {
		AccessTracker outerTracker = tracker;

		// Tracker for the interior of the loop
		tracker = new AccessTracker();
		AccessTracker loopTracker = tracker;

		final AccessTracker initTracker = tracker = new AccessTracker();
		loop.getInitBlock().accept(this);

		AccessTracker bodyTracker = null;
		if (loop.getBody() != null) {
			bodyTracker = tracker = new AccessTracker();
			loop.getBody().accept(this);
		}

		tracker = loopTracker;

		tracker.propagateUp(initTracker, loop.getInitBlock());
		if (bodyTracker != null) {
			tracker.propagateUp(bodyTracker, loop.getBody());
		}

		outerTracker.propagateUp(tracker, loop);
		tracker = outerTracker;
	}

	@Override
	public void visit(WhileBody whileBody) {
		AccessTracker outerTracker = tracker;

		tracker = new AccessTracker();

		processModule(whileBody.getDecision());

		AccessTracker save;
		/*
		 * Body could be null if unrolled.
		 */
		AccessTracker bodyTracker = null;
		if (whileBody.getBody() != null) {
			save = tracker;
			bodyTracker = tracker = new AccessTracker();
			whileBody.getBody().accept(this);
			tracker = save;
		}

		if (bodyTracker != null) {
			tracker.propagateUp(bodyTracker, whileBody.getBody());
		}
		outerTracker.propagateUp(tracker, whileBody);

		tracker = outerTracker;
	}

	@Override
	public void visit(UntilBody untilBody) {
		AccessTracker outerTracker = tracker;

		tracker = new AccessTracker();
		AccessTracker save;

		AccessTracker bodyTracker = null;
		if (untilBody.getBody() != null) {
			save = tracker;
			bodyTracker = tracker = new AccessTracker();
			untilBody.getBody().accept(this);
			tracker = save;
		}

		AccessTracker decisionTracker = null;
		if (untilBody.getDecision() != null) {
			save = tracker;
			decisionTracker = tracker = new AccessTracker();
			untilBody.getDecision().accept(this);
			tracker = save;
		}

		if (bodyTracker != null) {
			tracker.propagateUp(bodyTracker, untilBody.getBody());
		}

		if (decisionTracker != null) {
			tracker.propagateUp(decisionTracker, untilBody.getDecision());
		}

		outerTracker.propagateUp(tracker, untilBody);

		tracker = outerTracker;
	}

	@Override
	public void visit(ForBody forBody) {
		AccessTracker outerTracker = tracker;

		// Tracker for the LoopBody itself
		tracker = new AccessTracker();

		AccessTracker save;

		processModule(forBody.getDecision());

		/*
		 * Body could be null if unrolled.
		 */
		AccessTracker bodyTracker = null;
		if (forBody.getBody() != null) {
			save = tracker;
			bodyTracker = tracker = new AccessTracker();
			forBody.getBody().accept(this);
			tracker = save;
		}

		/*
		 * Update could be null if there's no increment
		 */
		AccessTracker updateTracker = null;
		if (forBody.getUpdate() != null) {
			save = tracker;
			updateTracker = tracker = new AccessTracker();
			forBody.getUpdate().accept(this);
			tracker = save;
		}

		if (bodyTracker != null) {
			tracker.propagateUp(bodyTracker, forBody.getBody());
		}

		if (updateTracker != null) {
			tracker.propagateUp(updateTracker, forBody.getUpdate());
		}

		outerTracker.propagateUp(tracker, forBody);
		tracker = outerTracker;
	}

	@Override
	public void visit(RegisterRead read) {
		tracker.addAccess(read,
				read.isVolatile() ? VOLATILE : read.getReferenceable());
	}

	@Override
	public void visit(RegisterWrite write) {
		tracker.addAccess(write,
				write.isVolatile() ? VOLATILE : write.getReferenceable());
	}

	@Override
	public void visit(MemoryRead mr) {
		// tracker.addAccess(mr, mr.isVolatile() ?
		// VOLATILE:mr.getReferenceable());
		// Relative to the LogicalMemoryPort a read IS a sequencing
		// point (only 1 can occur at a time), however, relative to
		// the memory a read is NOT a sequencing point as there may be
		// multiple ports.
		tracker.addAccess(mr, mr.isVolatile() ? VOLATILE : mr.getMemoryPort());
		tracker.addAccess(mr, mr.getMemoryPort().getLogicalMemory(), false);
	}

	@Override
	public void visit(MemoryWrite mw) {
		// A memory write, because it alters the state is always a
		// sequencing point for both the ports (one access at a time)
		// and the memory (ensures we get the right value)
		tracker.addAccess(mw, mw.isVolatile() ? VOLATILE : mw.getMemoryPort());
		tracker.addAccess(mw, mw.getMemoryPort().getLogicalMemory(), true);
	}

	@Override
	public void visit(SimplePinRead pinRead) {
		tracker.addAccess(pinRead);
	}

	@Override
	public void visit(SimplePinWrite pinWrite) {
		tracker.addAccess(pinWrite);
	}

	@Override
	public void visit(FifoAccess access) {
		tracker.addAccess(access);
		processModule(access);
	}

	@Override
	public void visit(FifoRead fifoRead) {
		tracker.addAccess(fifoRead);
		processModule(fifoRead);
	}

	@Override
	public void visit(FifoWrite fifoWrite) {
		tracker.addAccess(fifoWrite);
		processModule(fifoWrite);
	}

	@Override
	public void visit(PinRead pinRead) {
		throw new UnsupportedOperationException("Obsolete method in GRS_PR");
	}

	@Override
	public void visit(PinWrite pinWrite) {
		throw new UnsupportedOperationException("Obsolete method in GRS_PW");
	}

	@Override
	public void visit(PinStateChange pinStateChange) {
		throw new UnsupportedOperationException("Obsolete method in GRS_PSC");
	}

	private void addDependencies(Component comp, AccessTracker tracker) {
		addDependencies(comp, tracker, tracker);
	}

	/**
	 * The 'tracker' is used to determine what things are depended on and the
	 * nature of those things. The subTracker is used to determine the
	 * characteristics of the component being looked at. In the case of branches
	 * these are different because we must ensure that the true block does not
	 * depend on the false block and vice-versa, thus we cannot propagate those
	 * modules into the tracker until dependencies have been added.
	 */
	private void addDependencies(Component comp, AccessTracker tracker,
			AccessTracker subTracker) {
		if (comp.getOwner().isMutexModule()) {
			if (debug)
				System.out.println("NOT Adding dependencies for " + comp
						+ " in " + comp.getOwner() + " due to mutex");
			return;
		}

		if (debug)
			System.out.println("Adding dependencies for " + comp + " from "
					+ tracker + " specific " + subTracker);

		// Check all referenceables found in the component which also
		// exist in our context.
		Collection<Referenceable> componentRelevant = new HashSet<Referenceable>(
				subTracker.getRelevantReferenceables(comp));
		componentRelevant.retainAll(tracker.getAllReferenceables());
		// for (Referenceable refable :
		// subTracker.getRelevantReferenceables(comp))
		for (Referenceable refable : componentRelevant) {
			// For each dependency source, create a dependency
			if (debug)
				System.out.println("Getting dep sources for " + refable);
			for (Component source : tracker.getDepSources(refable)) {
				// To create the dependency we need the min delay
				// clocks. This is specified by the resource based on
				// the types of the source and target.
				Set<Component> trueSources = tracker.getTrueSources(source,
						refable);
				Set<Component> trueSinks = subTracker.getTrueSinks(comp,
						refable);
				int delayClocks = findDelayClocks(trueSources, trueSinks,
						refable);
				int goDelayClocks = findGoDelayClocks(trueSources, trueSinks,
						refable);
				boolean isGoSpacingAllowable = checkForGoSpacing(trueSources,
						comp);

				final Bus controlBus = getControlExit(source, comp)
						.getDoneBus();

				final ResourceDependency dep;
				if (isGoSpacingAllowable && (goDelayClocks >= 0)) {
					// The GO to GO dep still gets the source done bus
					// for consistency in dependency structure.
					// Special handling is applied during scheduling.
					ResourceDependency.GoToGoDep depGTG = new ResourceDependency.GoToGoDep(
							controlBus, goDelayClocks);
					dep = depGTG;
					source.addPostScheduleCallback(depGTG);
				} else {
					dep = new ResourceDependency(controlBus, delayClocks);
				}

				if (debug) {
					String refName = refable.toString();
					refName = refName.lastIndexOf(".") > 0 ? refName.substring(
							refName.lastIndexOf("."), refName.length())
							: refable.toString();
					System.out
							.println("new resource dep(X) based on " + refName
									+ " delay clocks " + dep.getDelayClocks());
					System.out.println("\tfrom " + source + " "
							+ source.showOwners());
					System.out
							.println("\tto " + comp + " " + comp.showOwners());
					System.out.println("\tType " + dep);
				}

				assert comp.getEntries().size() == 1 : "Entry count "
						+ comp.getEntries().size() + " (expecting 1)";
				assert controlBus.getOwner().getOwner().getOwner() == comp
						.getOwner() : "Dependency (X) spans module boundry";

				Entry entry = comp.getEntries().iterator().next();
				entry.addDependency(comp.getGoPort(), dep);
			}
		}
	}

	private static int findDelayClocks(Set<Component> fromSet,
			Set<Component> toSet, Referenceable ref) {
		if (debug) {
			System.out.println("Refable: " + ref);
			System.out.println("\tFromSet: " + fromSet);
			System.out.println("\tToSet: " + fromSet);
		}

		int delay = 0;
		for (Component from : fromSet) {
			for (Component to : toSet) {
				if (debug)
					System.out.println("delay clocks from " + from + " to "
							+ to);
				delay = Math.max(delay,
						ref.getSpacing((Referencer) from, (Referencer) to));
			}
		}
		return delay;
	}

	private static int findGoDelayClocks(Set<Component> fromSet,
			Set<Component> toSet, Referenceable ref) {
		if (debug) {
			System.out.println("Go spacing...");
		}

		int delay = 0;
		for (Component from : fromSet) {
			for (Component to : toSet) {
				int space = ref
						.getGoSpacing((Referencer) from, (Referencer) to);
				if (debug)
					System.out.println("go delay clocks from " + from + " to "
							+ to + " = " + space);

				if (space < 0)
					return space; // fail fast

				delay = Math.max(delay, space);
			}
		}
		return delay;
	}

	private static boolean checkForGoSpacing(Set<Component> fromSet,
			Component to) {
		// In order to guarantee that the delta between GO's is at
		// least the specified amount all entries in the fromSet (ie
		// the true sources of the dep) must be in the same context as
		// the target of the dependency. Without this condition it is
		// possible that the from could be scheduled later than
		// intended (as part of a sub module). **NOTE** using a
		// scheduling callback the source node may now be one level of
		// hierarchy below the target node.
		for (Component comp : fromSet) {
			if ((comp.getOwner() != to.getOwner())
					&& (comp.getOwner().getOwner() != to.getOwner()))
				return false;
		}
		return true;
	}

	/**
	 * Gets the {@link Exit} of a source {@link Component} that should be used
	 * to produce the GO signal for a target {@link Component}.
	 * 
	 * @param source
	 *            the control source
	 * @param target
	 *            the control target
	 * @return the exit of the source whose done bus will provide the control
	 *         signal for the target
	 */
	private Exit getControlExit(Component source, Component target) {
		final Exit exit = controlExits.get(new ControlPair(source, target));
		return exit != null ? exit : source.getExit(Exit.DONE);
	}

	/**
	 * Records the {@link Exit} of a source {@link Component} that should be
	 * used to produce the GO signal for a target {@link Component}.
	 * 
	 * @param source
	 *            the control source
	 * @param target
	 *            the control target
	 * @param exit
	 *            the exit of the source whose done bus will provide the control
	 *            signal for the target
	 */
	private void addControlExit(Component source, Component target, Exit exit) {
		assert exit.getOwner() == source : "invalid exit";
		controlExits.put(new ControlPair(source, target), exit);
	}

	private class AccessTracker {
		/**
		 * A map of the resource being accessed to the ResourceState object that
		 * tells us about how that resource is being accessed (ie first
		 * accesses, last accesses, etc)
		 */
		private Map<Referenceable, ResourceState> stateMap = new HashMap<Referenceable, ResourceState>();
		private Map<Component, AccessTracker> subTrackerMap = new HashMap<Component, AccessTracker>();

		public void addAccess(Referencer ref) {
			addAccess(ref, ref.getReferenceable());
		}

		public void addAccess(Referencer ref, Referenceable target) {
			addAccess(ref, target, ref.isSequencingPoint());
		}

		public void addAccess(Referencer ref, Referenceable target,
				boolean seqPt) {
			ResourceState state = getResourceState(target);
			if (debug)
				System.out.println("addAccess called on " + this
						+ " with statemap "
						+ Integer.toHexString(stateMap.hashCode()));
			state.addAccessor((Component) ref, seqPt);
		}

		public void propagateUp(AccessTracker innerTracker, Component comp) {
			/*
			 * When propagating up the branch we miss whatever is in the false
			 * branch.
			 */
			if (debug)
				System.out.println("Propagating up " + comp + " in " + this
						+ " with statemap "
						+ Integer.toHexString(stateMap.hashCode()) + " from "
						+ innerTracker);
			for (Referenceable ref : innerTracker.stateMap.keySet()) {
				ResourceState innerState = innerTracker.stateMap.get(ref);
				ResourceState currentState = getResourceState(ref);
				currentState.addAccessor(comp, innerState.isSequencing);
			}
			subTrackerMap.put(comp, innerTracker);
		}

		public Set<Referenceable> getAllReferenceables() {
			return stateMap.keySet();
		}

		public Set<Referenceable> getRelevantReferenceables(Component comp) {
			Set<Referenceable> relevant = new HashSet<Referenceable>();
			for (Referenceable ref : stateMap.keySet()) {
				ResourceState state = stateMap.get(ref);
				if (state.contains(comp))
					relevant.add(ref);
			}

			return relevant;
		}

		public Set<Component> getDepSources(Referenceable ref) {
			// Return the prior because we expect that the current
			// node has already been added to the tracker.
			return stateMap.get(ref).priorAccesses;
		}

		private Set<Component> getTrue(Component comp, Referenceable ref,
				boolean getSource) {
			if (debug)
				System.out.println("getTrue " + getSource + "called on " + this
						+ " with statemap "
						+ Integer.toHexString(stateMap.hashCode()));

			if (!subTrackerMap.containsKey(comp)) {
				if (debug)
					System.out.println("Subtracker does not contain key "
							+ comp);
				return Collections.singleton(comp);
			}
			AccessTracker inner = subTrackerMap.get(comp);
			if (!inner.stateMap.containsKey(ref)) {
				if (debug)
					System.out
							.println("Subtracker state map does not contain key "
									+ ref);
				return Collections.singleton(comp);
			}
			ResourceState innerState = inner.stateMap.get(ref);
			Set<Component> trueComps = new HashSet<Component>();
			// Get the components that are the accesses to the
			// Referenceable. Note that if the 'firstAccesses' in the
			// ResourceState has not yet been populated then we need
			// to use the 'currentAccesses' set. This can happen when
			// there is only one access in a nested module. It was
			// first seen in code like:
			// if (__port_read(1) == 0) ... Where __port_read is the
			// API call to access an interface port.
			Collection<Component> comps = getSource ? innerState.currentAccesses
					: (innerState.isFirstAccessesValidYet ? innerState.firstAccesses
							: innerState.currentAccesses);
			for (Component innerComp : comps) {
				trueComps.addAll(inner.getTrueSources(innerComp, ref));
			}
			return trueComps;
		}

		public Set<Component> getTrueSources(Component comp, Referenceable ref) {
			return getTrue(comp, ref, true);
		}

		public Set<Component> getTrueSinks(Component comp, Referenceable ref) {
			return getTrue(comp, ref, false);
		}

		public AccessTracker mergeTracker(AccessTracker other) {
			AccessTracker merged = new AccessTracker();
			Set<Referenceable> allRefs = new HashSet<Referenceable>();
			allRefs.addAll(stateMap.keySet());
			allRefs.addAll(other.stateMap.keySet());
			for (Referenceable ref : allRefs) {
				final ResourceState tState = stateMap.containsKey(ref) ? stateMap
						.get(ref) : new ResourceState(ref);
				final ResourceState oState = other.stateMap.containsKey(ref) ? other.stateMap
						.get(ref) : new ResourceState(ref);
				merged.stateMap.put(ref, tState.mergeState(oState));
			}
			merged.subTrackerMap.putAll(subTrackerMap);
			merged.subTrackerMap.putAll(other.subTrackerMap);
			return merged;
		}

		private ResourceState getResourceState(Referenceable ref) {
			ResourceState state = stateMap.get(ref);
			if (state == null) {
				state = new ResourceState(ref);
				stateMap.put(ref, state);
			}
			return state;
		}

		@Override
		public String toString() {
			return super.toString().replaceAll(
					"net.sf.openforge.schedule.GlobalResourceSequencer", "");
		}

	}

	private class ResourceState {
		Referenceable ref;
		/**
		 * the set of 'first accesses' to the resource in this context. Will be
		 * empty if we have not gotten to a 'next' sequenc yet
		 */
		Set<Component> firstAccesses = new HashSet<Component>();
		boolean isFirstAccessesValidYet = false;
		Set<Component> priorAccesses = new HashSet<Component>();
		Set<Component> currentAccesses = new HashSet<Component>();
		boolean isSequencing = false;
		boolean currentIsSeqPoint = false;

		public ResourceState(Referenceable ref) {
			this.ref = ref;
		}

		// public void addAccessor (Referencer comp)
		// {
		// addAccessor((Component)comp, comp.isSequencingPoint());
		// }

		public void addAccessor(Component comp, boolean seqPoint) {
			if (debug)
				System.out.println("Adding " + comp + " " + seqPoint + " to "
						+ ref);
			// If the are 'unmatched' then we are switching contexts
			// from a seq point to unsequenced, or vice-versa.
			// However EVERY sequencing point is a new context.
			boolean unmatched = currentAccesses.isEmpty() ? false
					: (currentIsSeqPoint != seqPoint);
			currentIsSeqPoint = seqPoint;
			if (seqPoint || unmatched) {
				isSequencing = (isSequencing | seqPoint);
				if (firstAccesses.isEmpty()) {
					firstAccesses = new HashSet<Component>(currentAccesses);
					isFirstAccessesValidYet = !firstAccesses.isEmpty();
				}
				priorAccesses = currentAccesses;
				currentAccesses = new HashSet<Component>();
			}
			currentAccesses.add(comp);
			if (debug)
				System.out.println("\t" + this);
		}

		public boolean contains(Component comp) {
			// In actuality, the way traversal works it would always
			// be in the currentAccesses set.
			return currentAccesses.contains(comp)
					|| priorAccesses.contains(comp)
					|| firstAccesses.contains(comp);
		}

		public ResourceState mergeState(ResourceState other) {
			if (other.ref != ref)
				throw new IllegalArgumentException(
						"Must have same referenceable to mergeState Resource States");
			ResourceState merged = new ResourceState(ref);
			merged.firstAccesses.addAll(firstAccesses);
			merged.firstAccesses.addAll(other.firstAccesses);
			merged.priorAccesses.addAll(priorAccesses);
			merged.priorAccesses.addAll(other.priorAccesses);
			merged.currentAccesses.addAll(currentAccesses);
			merged.currentAccesses.addAll(other.currentAccesses);
			merged.isSequencing = isSequencing | other.isSequencing;
			merged.currentIsSeqPoint = currentIsSeqPoint
					| other.currentIsSeqPoint;

			return merged;
		}

		@Override
		public String toString() {
			return "R-S[" + Integer.toHexString(hashCode()) + "]: first: "
					+ firstAccesses + " firstValid? " + isFirstAccessesValidYet
					+ " prior: " + priorAccesses + " cur: " + currentAccesses
					+ " ref: " + ref;
		}
	}

}
