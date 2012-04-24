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

import java.util.*;

import net.sf.openforge.lim.*;
import net.sf.openforge.lim.io.*;
import net.sf.openforge.lim.memory.*;

/**
 * The GlobalResourceSequencer inserts {@link ResourceDependency
 * ResourceDependencies} between {@link Component Components} that
 * access the same global {@link Resource} where those components
 * might otherwise execute in parallel, i.e., when they appear in a
 * {@link Block}.  These components could be low level operations or
 * {@link Module Modules} that contain those operations.
 *
 * GlobalResourceSequencer works with {@link Referenceable} and {@link
 * Referencer} objects to insert the necessary {@link
 * ResourceDependency} between {@link Components} (implementers of
 * {@link Referencer}) which share a common {@link Referenceable}
 * target.  This ensures that the scheduling attributes dictated by
 * the {@link Referenceable} target are upheld where the components
 * may otherwise execute in parallel or lose the intended sequence of
 * accesses due to a lack of either data or control dependencies.
 *
 * @version $Id: GlobalResourceSequencer.java 538 2007-11-21 06:22:39Z imiller $
 */
public class GlobalResourceSequencer extends DefaultVisitor
{
    private static final String _RCS_ = "$Rev: 538 $";

    private static final boolean debug = false;
    
    /**
     * An association between one {@link Component} that is the
     * source of a control signal and another {@link Component} that
     * is the target of the control signal.  Will be used as the key
     * in the <code>controlExits</code> map to record the {@link Exit}
     * that should be used to implement the control connection between
     * these two.
     */
    private static class ControlPair
    {
        /** The source of a DONE signal */
        private Component source;

        /** The target of the DONE signal */
        private Component target;

        /**
         * Creates a new <code>ControlPair</code> instance.
         *
         * @param source the source of a DONE signal
         * @param target the target of the DONE signal
         */
        ControlPair (Component source, Component target)
        {
            this.source = source;
            this.target = target;
        }

        public int hashCode ()
        {
            return source.hashCode() + target.hashCode();
        }

        public boolean equals (Object object)
        {
            if (object instanceof ControlPair)
            {
                final ControlPair pair = (ControlPair)object;
                return (pair.source == source) && (pair.target == target);
            }
            return false;
        }
    }

    /**
     * Define a Referenceable to to which all volatile accesses are
     * mapped.
     */
    private static final Referenceable VOLATILE = new Referenceable()
        {
            public int getSpacing (Referencer from, Referencer to) { return 0;}
            public int getGoSpacing (Referencer from, Referencer to) { return -1;}
        };
    
    /** Top level access tracker */
    private AccessTracker tracker = new AccessTracker();

    /**
     * Map of ControlPair to Exit, which shows which Exit of a source
     * Component should be used to derive the control signal for a
     * target Component
     */
    private Map controlExits = new HashMap();


    public GlobalResourceSequencer ()
    {
        super();
    }

//     public void visit (Task task)
//     {
//         super.visit(task);
//         _schedule.d.launchGraph(task);
//         try{System.out.println("SLEEP");Thread.sleep(3000);}catch(Exception e){}
//     }
    
    public void visit (Call call)
    {
        if (debug) System.out.println("CALL " + call);
        AccessTracker outerTracker = this.tracker;
        this.tracker = new AccessTracker();
        call.getProcedure().getBody().accept(this);
        outerTracker.propagateUp(this.tracker, call);
        this.tracker = outerTracker;
    }

    public void visit (Block block) { processModule(block); }
    public void visit (Decision decision) { processModule(decision); }
    public void visit (Switch swich) { visit((Block)swich); }
    public void visit (HeapRead heapRead) { visit((Block)heapRead); }
    public void visit (ArrayRead arrayRead) { visit((Block)arrayRead); }
    public void visit (AbsoluteMemoryRead mod) { visit((Block)mod); }
    public void visit (HeapWrite heapWrite) { visit((Block)heapWrite); }
    public void visit (ArrayWrite arrayWrite) { visit((Block)arrayWrite); }
    public void visit (AbsoluteMemoryWrite mod) { visit((Block)mod); }
    public void visit (SimplePinAccess mod) { processModule(mod); }
    public void visit (TaskCall mod) { processModule(mod); }
    

    private void processModule (Module module)
    {
        AccessTracker outerTracker = this.tracker;
        this.tracker = new AccessTracker();

        /*
         * Travese the sequence of Components.
         */
        Collection comps = (module instanceof Block) ? ((Block)module).getSequence() : module.getComponents();
        for (Iterator iter = comps.iterator(); iter.hasNext();)
        {
            final Component component = (Component)iter.next();
            component.accept(this);
            addDependencies(component, this.tracker);
        }

        outerTracker.propagateUp(this.tracker, module);
        this.tracker = outerTracker;
    }

    public void visit (Branch branch)
    {
        AccessTracker outerTracker = this.tracker;

        this.tracker = new AccessTracker();
        
        /*
         * Record which Exit drives which branch.
         */
        final Decision decision = branch.getDecision();
        addControlExit(decision, branch.getTrueBranch(), decision.getTrueExit());
        addControlExit(decision, branch.getFalseBranch(), decision.getFalseExit());

        decision.accept(this);

        AccessTracker save = this.tracker;
        final AccessTracker trueTracker = new AccessTracker();
        this.tracker = trueTracker;
        branch.getTrueBranch().accept(this);
        this.tracker = save;

        branch.getFalseBranch().accept(this);
        save = this.tracker;
        final AccessTracker falseTracker = new AccessTracker();
        this.tracker = falseTracker;
        branch.getFalseBranch().accept(this);
        this.tracker = save;

        // Add the dependencies before propagating either the true or
        // false blocks up.  This way they do not depend on each
        // other, but rather on the decision block.
        addDependencies(branch.getTrueBranch(), this.tracker, trueTracker);
        addDependencies(branch.getFalseBranch(), this.tracker, falseTracker);
        
        // This makes it look like all the accesses happen in the true
        // branch.  But that is OK because all we are concerned with
        // is the existance of accesses and what their type is.
//         this.tracker.propagateUp(trueTracker, branch.getTrueBranch());
//         this.tracker.propagateUp(trueTracker, branch.getFalseBranch());
        AccessTracker merged = trueTracker.mergeTracker(falseTracker);
        this.tracker.propagateUp(merged, branch.getTrueBranch());
        outerTracker.propagateUp(this.tracker, branch);

        this.tracker = outerTracker;
    }

    public void visit (Loop loop)
    {
        AccessTracker outerTracker = this.tracker;

        // Tracker for the interior of the loop
        this.tracker = new AccessTracker();
        AccessTracker loopTracker = this.tracker;
        
        final AccessTracker initTracker = this.tracker = new AccessTracker();
        loop.getInitBlock().accept(this);

        AccessTracker bodyTracker = null;
        if (loop.getBody() != null)
        {
            bodyTracker = this.tracker = new AccessTracker();
            loop.getBody().accept(this);
        }
        
        this.tracker = loopTracker;
        
        this.tracker.propagateUp(initTracker, loop.getInitBlock());
        if (bodyTracker != null)
        {
            this.tracker.propagateUp(bodyTracker, loop.getBody());
        }

        outerTracker.propagateUp(this.tracker, loop);
        this.tracker = outerTracker;
    }
    
    public void visit (WhileBody whileBody)
    {
        AccessTracker outerTracker = this.tracker;

        this.tracker = new AccessTracker();
        
        processModule(whileBody.getDecision());

        AccessTracker save;
        /*
         * Body could be null if unrolled.
         */
        AccessTracker bodyTracker = null;
        if (whileBody.getBody() != null)
        {
            save = this.tracker;
            bodyTracker = this.tracker = new AccessTracker();
            whileBody.getBody().accept(this);
            this.tracker = save;
        }

        if (bodyTracker != null)
        {
            this.tracker.propagateUp(bodyTracker, whileBody.getBody());
        }
        outerTracker.propagateUp(this.tracker, whileBody);
        
        this.tracker = outerTracker;
    }
    
    public void visit (UntilBody untilBody)
    {
        AccessTracker outerTracker = this.tracker;

        this.tracker = new AccessTracker();
        AccessTracker save;
        
        AccessTracker bodyTracker = null;
        if (untilBody.getBody() != null)
        {
            save = this.tracker;
            bodyTracker = this.tracker = new AccessTracker();
            untilBody.getBody().accept(this);
            this.tracker = save;
        }
        
        AccessTracker decisionTracker = null;
        if (untilBody.getDecision() != null)
        {
            save = this.tracker;
            decisionTracker = this.tracker = new AccessTracker();
            untilBody.getDecision().accept(this);
            this.tracker = save;
        }

        if (bodyTracker != null)
        {
            this.tracker.propagateUp(bodyTracker, untilBody.getBody());
        }

        if (decisionTracker != null)
        {
            this.tracker.propagateUp(decisionTracker, untilBody.getDecision());
        }

        outerTracker.propagateUp(this.tracker, untilBody);
        
        this.tracker = outerTracker;
    }
    
    public void visit (ForBody forBody)
    {
        AccessTracker outerTracker = this.tracker;

        // Tracker for the LoopBody itself
        this.tracker = new AccessTracker();

        AccessTracker save;

        processModule(forBody.getDecision());

        /*
         * Body could be null if unrolled.
         */
        AccessTracker bodyTracker = null;
        if (forBody.getBody() != null)
        {
            save = this.tracker;
            bodyTracker = this.tracker = new AccessTracker();
            forBody.getBody().accept(this);
            this.tracker = save;
        }

        /*
         * Update could be null if there's no increment
         */
        AccessTracker updateTracker = null;
        if (forBody.getUpdate() != null)
        {
            save = this.tracker;
            updateTracker = this.tracker = new AccessTracker();
            forBody.getUpdate().accept(this);
            this.tracker = save;
        }

        if (bodyTracker != null)
        {
            this.tracker.propagateUp(bodyTracker, forBody.getBody());
        }

        if (updateTracker != null)
        {
            this.tracker.propagateUp(updateTracker, forBody.getUpdate());
        }

        outerTracker.propagateUp(this.tracker, forBody);
        this.tracker = outerTracker;
    }
    
    public void visit (RegisterRead read)
    {
        tracker.addAccess(read, read.isVolatile() ? VOLATILE:read.getReferenceable());
    }
    
    public void visit (RegisterWrite write)
    {
        tracker.addAccess(write, write.isVolatile() ? VOLATILE:write.getReferenceable());
    }

    public void visit (MemoryRead mr)
    {
        //tracker.addAccess(mr, mr.isVolatile() ? VOLATILE:mr.getReferenceable());
        // Relative to the LogicalMemoryPort a read IS a sequencing
        // point (only 1 can occur at a time), however, relative to
        // the memory a read is NOT a sequencing point as there may be
        // multiple ports.
        tracker.addAccess(mr, mr.isVolatile() ? VOLATILE:mr.getMemoryPort());
        tracker.addAccess(mr, mr.getMemoryPort().getLogicalMemory(), false);
    }
    
    public void visit (MemoryWrite mw)
    {
        // A memory write, because it alters the state is always a
        // sequencing point for both the ports (one access at a time)
        // and the memory (ensures we get the right value)
        tracker.addAccess(mw, mw.isVolatile() ? VOLATILE:mw.getMemoryPort());
        tracker.addAccess(mw, mw.getMemoryPort().getLogicalMemory(), true);
    }
    
    public void visit (SimplePinRead pinRead)
    {
        tracker.addAccess(pinRead);
    }
    
    public void visit (SimplePinWrite pinWrite)
    {
        tracker.addAccess(pinWrite);
    }

    public void visit (FifoAccess access)
    {
        tracker.addAccess(access);
        processModule(access);
    }
    
    public void visit (FifoRead fifoRead)
    {
        tracker.addAccess(fifoRead);
        processModule(fifoRead);
    }
    
    public void visit (FifoWrite fifoWrite)
    {
        tracker.addAccess(fifoWrite);
        processModule(fifoWrite);
    }
    
    public void visit (PinRead pinRead)
    {
        throw new UnsupportedOperationException("Obsolete method in GRS_PR");
    }
    
    public void visit (PinWrite pinWrite)
    {
        throw new UnsupportedOperationException("Obsolete method in GRS_PW");
    }

    public void visit (PinStateChange pinStateChange)
    {
        throw new UnsupportedOperationException("Obsolete method in GRS_PSC");
    }

    private void addDependencies (Component comp, AccessTracker tracker)
    {
        addDependencies(comp, tracker, tracker);
    }

    /**
     * The 'tracker' is used to determine what things are depended on
     * and the nature of those things.  The subTracker is used to
     * determine the characteristics of the component being looked
     * at.  In the case of branches these are different because we
     * must ensure that the true block does not depend on the false
     * block and vice-versa, thus we cannot propagate those modules
     * into the tracker until dependencies have been added.
     */
    private void addDependencies (Component comp, AccessTracker tracker, AccessTracker subTracker)
    {
        if (comp.getOwner().isMutexModule())
        {
            if (debug) System.out.println("NOT Adding dependencies for " + comp + " in " + comp.getOwner() + " due to mutex");
            return;
        }
        
        if (debug) System.out.println("Adding dependencies for " + comp + " from " + tracker + " specific " + subTracker);

        // Check all referenceables found in the component which also
        // exist in our context.
        Collection<Referenceable> componentRelevant = new HashSet(subTracker.getRelevantReferenceables(comp));
        componentRelevant.retainAll(tracker.getAllReferenceables());
//         for (Referenceable refable : subTracker.getRelevantReferenceables(comp))
        for (Referenceable refable : componentRelevant)
        {
            // For each dependency source, create a dependency
            if (debug) System.out.println("Getting dep sources for " + refable);
            for (Component source : tracker.getDepSources(refable))
            {
                // To create the dependency we need the min delay
                // clocks.  This is specified by the resource based on
                // the types of the source and target.
                Set trueSources = tracker.getTrueSources(source, refable);
                Set trueSinks = subTracker.getTrueSinks(comp, refable);
                int delayClocks = findDelayClocks(trueSources, trueSinks, refable);
                int goDelayClocks = findGoDelayClocks(trueSources, trueSinks, refable);
                boolean isGoSpacingAllowable = checkForGoSpacing(trueSources, comp);

                final Bus controlBus = getControlExit(source, comp).getDoneBus();
                
                final ResourceDependency dep;
                if (isGoSpacingAllowable && (goDelayClocks >= 0))
                {
                    // The GO to GO dep still gets the source done bus
                    // for consistency in dependency structure.
                    // Special handling is applied during scheduling.
                    ResourceDependency.GoToGoDep depGTG = new ResourceDependency.GoToGoDep(controlBus, goDelayClocks);
                    dep = depGTG;
                    source.addPostScheduleCallback(depGTG);
                }
                else
                {
                    dep = new ResourceDependency(controlBus, delayClocks);
                }
                
                if (debug)
                {
                    String refName = refable.toString();
                    refName = refName.lastIndexOf(".") > 0 ? refName.substring(refName.lastIndexOf("."), refName.length()) : refable.toString();
                    System.out.println("new resource dep(X) based on "+refName+" delay clocks " + dep.getDelayClocks());
                    System.out.println("\tfrom " + source + " " + source.showOwners());
                    System.out.println("\tto " + comp + " " + comp.showOwners());
                    System.out.println("\tType " + dep);
                }
                
                
                assert comp.getEntries().size() == 1 : "Entry count " + comp.getEntries().size() + " (expecting 1)";
                assert controlBus.getOwner().getOwner().getOwner() == comp.getOwner() : "Dependency (X) spans module boundry";
                
                Entry entry = (Entry)comp.getEntries().iterator().next();
                entry.addDependency(comp.getGoPort(), dep);
            }
        }
    }

    private static int findDelayClocks (Set fromSet, Set toSet, Referenceable ref)
    {
        if (debug)
        {
            System.out.println("Refable: " + ref);
            System.out.println("\tFromSet: " + fromSet);
            System.out.println("\tToSet: " + fromSet);
        }
        
        int delay = 0;
        for (Referencer from : ((Set<Referencer>)fromSet))
        {
            for (Referencer to : ((Set<Referencer>)toSet))
            {
                if (debug) System.out.println("delay clocks from " + from + " to " + to);
                delay = Math.max(delay, ref.getSpacing(from, to));
            }
        }
        return delay;
    }
    
    private static int findGoDelayClocks (Set fromSet, Set toSet, Referenceable ref)
    {
        if (debug)
        {
            System.out.println("Go spacing...");
        }
        
        int delay = 0;
        for (Referencer from : ((Set<Referencer>)fromSet))
        {
            for (Referencer to : ((Set<Referencer>)toSet))
            {
                int space = ref.getGoSpacing(from, to);
                if (debug) System.out.println("go delay clocks from " + from + " to " + to + " = " + space);

                if (space < 0) return space; // fail fast
                
                delay = Math.max(delay, space);
            }
        }
        return delay;
    }

    private static boolean checkForGoSpacing (Set fromSet, Component to)
    {
        // In order to guarantee that the delta between GO's is at
        // least the specified amount all entries in the fromSet (ie
        // the true sources of the dep) must be in the same context as
        // the target of the dependency.  Without this condition it is
        // possible that the from could be scheduled later than
        // intended (as part of a sub module).  **NOTE** using a
        // scheduling callback the source node may now be one level of
        // hierarchy below the target node.
        for (Iterator iter = fromSet.iterator(); iter.hasNext();)
        {
            Component comp = (Component)iter.next();
            
            if (
                (comp.getOwner() != to.getOwner()) &&
                (comp.getOwner().getOwner() != to.getOwner())
                )
                return false;
        }
        return true;
    }
    
    
    /**
     * Gets the {@link Exit} of a source {@link Component} that should
     * be used to produce the GO signal for a target {@link Component}.
     *
     * @param source the control source
     * @param target the control target
     * @return the exit of the source whose done bus will provide the
     *         control signal for the target
     */
    private Exit getControlExit (Component source, Component target)
    {
        final Exit exit = (Exit)controlExits.get(new ControlPair(source, target));
        return exit != null ? exit : source.getExit(Exit.DONE);
    }

    /**
     * Records the {@link Exit} of a source {@link Component} that should
     * be used to produce the GO signal for a target {@link Component}.
     *
     * @param source the control source
     * @param target the control target
     * @param exit the exit of the source whose done bus will provide the
     *         control signal for the target
     */
    private void addControlExit (Component source, Component target, Exit exit)
    {
        assert exit.getOwner() == source : "invalid exit";
        controlExits.put(new ControlPair(source, target), exit);
    }

    private class AccessTracker
    {
        /** A map of the resource being accessed to the ResourceState
         * object that tells us about how that resource is being
         * accessed (ie first accesses, last accesses, etc) */
        private Map<Referenceable, ResourceState> stateMap = new HashMap();
        private Map<Component, AccessTracker> subTrackerMap = new HashMap();

        public void addAccess (Referencer ref)
        {
            addAccess(ref, ref.getReferenceable());
        }
        
        public void addAccess (Referencer ref, Referenceable target)
        {
            addAccess(ref, target, ref.isSequencingPoint());
        }
        
        public void addAccess (Referencer ref, Referenceable target, boolean seqPt)
        {
            ResourceState state = getResourceState(target);
            if (debug) System.out.println("addAccess called on " + this + " with statemap " + Integer.toHexString(stateMap.hashCode()));
            state.addAccessor((Component)ref, seqPt);
        }
        public void propagateUp (AccessTracker innerTracker, Component comp)
        {
            /*
             * When propagating up the branch we miss whatever is in
             * the false branch.
             */
            if (debug) System.out.println("Propagating up " + comp + " in " + this + " with statemap " + Integer.toHexString(stateMap.hashCode()) + " from " + innerTracker);
            for (Referenceable ref : innerTracker.stateMap.keySet())
            {
                ResourceState innerState = innerTracker.stateMap.get(ref);
                ResourceState currentState = getResourceState(ref);
                currentState.addAccessor(comp, innerState.isSequencing);
            }
            subTrackerMap.put(comp, innerTracker);
        }

        public Set<Referenceable> getAllReferenceables ()
        {
            return stateMap.keySet();
        }
        
        public Set<Referenceable> getRelevantReferenceables (Component comp)
        {
            Set<Referenceable> relevant = new HashSet();
            for (Referenceable ref : stateMap.keySet())
            {
                ResourceState state = stateMap.get(ref);
                if (state.contains(comp))
                    relevant.add(ref);
            }
            
            return relevant;
        }

        public Set<Component> getDepSources (Referenceable ref)
        {
            // Return the prior because we expect that the current
            // node has already been added to the tracker.
            return stateMap.get(ref).priorAccesses;
        }

        private Set getTrue (Component comp, Referenceable ref, boolean getSource)
        {
            if (debug) System.out.println("getTrue "+getSource+"called on " + this + " with statemap " + Integer.toHexString(stateMap.hashCode()));
            
            if (!subTrackerMap.containsKey(comp))
            {
                if (debug) System.out.println("Subtracker does not contain key " + comp);
                return Collections.singleton(comp);
            }
            AccessTracker inner = subTrackerMap.get(comp);
            if (!inner.stateMap.containsKey(ref))
            {
                if (debug) System.out.println("Subtracker state map does not contain key " + ref);
                return Collections.singleton(comp);
            }
            ResourceState innerState = inner.stateMap.get(ref);
            Set trueComps = new HashSet();
            // Get the components that are the accesses to the
            // Referenceable.  Note that if the 'firstAccesses' in the
            // ResourceState has not yet been populated then we need
            // to use the 'currentAccesses' set.  This can happen when
            // there is only one access in a nested module.  It was
            // first seen in code like:
            // if (__port_read(1) == 0) ... Where __port_read is the
            // API call to access an interface port.
            Collection comps = getSource ? innerState.currentAccesses : (innerState.isFirstAccessesValidYet ? innerState.firstAccesses:innerState.currentAccesses);
            for (Component innerComp : ((Collection<Component>)comps))
            {
                trueComps.addAll(inner.getTrueSources(innerComp, ref));
            }
            return trueComps;
        }
        
        public Set getTrueSources (Component comp, Referenceable ref)
        {
            return getTrue(comp, ref, true);
        }
        
        public Set getTrueSinks (Component comp, Referenceable ref)
        {
            return getTrue(comp, ref, false);
        }

        public AccessTracker mergeTracker (AccessTracker other)
        {
            AccessTracker merged = new AccessTracker();
            Set<Referenceable> allRefs = new HashSet();
            allRefs.addAll(this.stateMap.keySet());
            allRefs.addAll(other.stateMap.keySet());
            for (Referenceable ref : allRefs)
            {
                final ResourceState tState = this.stateMap.containsKey(ref) ? this.stateMap.get(ref):new ResourceState(ref);
                final ResourceState oState = other.stateMap.containsKey(ref) ? other.stateMap.get(ref):new ResourceState(ref);
                merged.stateMap.put(ref, tState.mergeState(oState));
            }
            merged.subTrackerMap.putAll(this.subTrackerMap);
            merged.subTrackerMap.putAll(other.subTrackerMap);
            return merged;
        }

        private ResourceState getResourceState (Referenceable ref)
        {
            ResourceState state = stateMap.get(ref);
            if (state == null)
            {
                state = new ResourceState(ref);
                stateMap.put(ref, state);
            }
            return state;
        }

        public String toString ()
        {
            return super.toString().replaceAll("net.sf.openforge.schedule.GlobalResourceSequencer","");
        }
        
    }
    
    private class ResourceState
    {
        Referenceable ref;
        /** the set of 'first accesses' to the resource in this
            context.  Will be empty if we have not gotten to a 'next'
            sequenc yet */
        Set<Component> firstAccesses = new HashSet();
        boolean isFirstAccessesValidYet = false;
        Set<Component> priorAccesses = new HashSet();
        Set<Component> currentAccesses = new HashSet();
        boolean isSequencing = false;
        boolean currentIsSeqPoint = false;

        public ResourceState (Referenceable ref)
        {
            this.ref = ref;
        }
        
//         public void addAccessor (Referencer comp)
//         {
//             addAccessor((Component)comp, comp.isSequencingPoint());
//         }
        
        public void addAccessor (Component comp, boolean seqPoint)
        {
            if (debug) System.out.println("Adding " + comp + " " + seqPoint + " to " + this.ref);
            // If the are 'unmatched' then we are switching contexts
            // from a seq point to unsequenced, or vice-versa.
            // However EVERY sequencing point is a new context.
            boolean unmatched = currentAccesses.isEmpty() ? false : (currentIsSeqPoint != seqPoint);
            currentIsSeqPoint = seqPoint;
            if (seqPoint || unmatched)
            {
                this.isSequencing = (this.isSequencing | seqPoint);
                if (firstAccesses.isEmpty())
                {
                    firstAccesses = new HashSet(currentAccesses);
                    this.isFirstAccessesValidYet = !firstAccesses.isEmpty();
                }
                priorAccesses = currentAccesses;
                currentAccesses = new HashSet();
            }
            currentAccesses.add((Component)comp);
            if (debug) System.out.println("\t" + this);
        }

        public boolean contains (Component comp)
        {
            // In actuality, the way traversal works it would always
            // be in the currentAccesses set.
            return currentAccesses.contains(comp) || priorAccesses.contains(comp) || firstAccesses.contains(comp);
        }

        public ResourceState mergeState (ResourceState other)
        {
            if (other.ref != this.ref)
                throw new IllegalArgumentException("Must have same referenceable to mergeState Resource States");
            ResourceState merged = new ResourceState(this.ref);
            merged.firstAccesses.addAll(this.firstAccesses);
            merged.firstAccesses.addAll(other.firstAccesses);
            merged.priorAccesses.addAll(this.priorAccesses);
            merged.priorAccesses.addAll(other.priorAccesses);
            merged.currentAccesses.addAll(this.currentAccesses);
            merged.currentAccesses.addAll(other.currentAccesses);
            merged.isSequencing = this.isSequencing | other.isSequencing;
            merged.currentIsSeqPoint = this.currentIsSeqPoint | other.currentIsSeqPoint;
            
            return merged;
        }
        
        public String toString ()
        {
            return "R-S["+Integer.toHexString(hashCode())+"]: first: " + this.firstAccesses + " firstValid? " + this.isFirstAccessesValidYet + " prior: " + this.priorAccesses + " cur: " + this.currentAccesses + " ref: " + this.ref;
        }
    }
    
}
