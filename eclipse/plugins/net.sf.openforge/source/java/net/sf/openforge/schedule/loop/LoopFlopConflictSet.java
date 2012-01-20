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

package net.sf.openforge.schedule.loop;


import java.util.*;

import net.sf.openforge.app.*;
import net.sf.openforge.app.project.*;
import net.sf.openforge.lim.*;
import net.sf.openforge.schedule.*;

/**
 * LoopFlopConflictSet provides specific behavior for resolving the
 * conflicts contained in the set based on a particular resolution
 * policy.  Specifically, private subclasses manage the details of
 * resolving either first or last accesses to the conflicted resource
 * based on the specified resolution policy.  Additinally, this class
 * allows for querying of the collection of conflicts for the set of
 * components that make up the logical path to the accesses which are
 * to be fixed during resolution.  This is of use when determining
 * whether the resolving should be done.
 *
 * @author imiller
 * @version $Id: LoopFlopConflictSet.java 427 2007-04-16 17:38:32Z imiller $
 */
public abstract class LoopFlopConflictSet
{
    private static final String _RCS_ = "$Rev: 427 $";

    /** These values determine how the resolution of conflicts in
     * removing a loop flop are resolved. */
    public static final int FIX_NONE     = 0; // Performs no fixes
    public static final int FIX_FEWEST   = 1; // Inserts fewest number of stalls possible
    public static final int FIX_MOST     = 2; // Inserts most number of stalls
    public static final int FIRST_ALWAYS = 3; // Always stalls the 1st accesses to conflicted resources
    public static final int LAST_ALWAYS  = 4; // Always stalls the last accesses to conflicted resources

    /** The Set of LoopFlopConflict objects for a give loop body. */
    private Set<LoopFlopConflict> conflicts;
    /** A correlation map which is used to correlate a given component
     * to the component which should be fixed during resolving.  This
     * is needed when the conflict access was determined from a cloned
     * tree and the fix is to be applied to the orignal tree. */
    private Map<Component, Component> correlation;

    /**
     * Returns a LoopFlopConflictSet object based on the collection of
     * conflicts and specified correlation using the current
     * resolution policy as specified by the value in the preference
     * {@link OptionRegistry#LOOP_RESOURCE_FIX_POLICY}
     *
     * @param conflicts a Collection of {@link LoopFlopConflict}
     * objects.
     * @param correlation a Map of Component to Component, used in
     * transferring the conflict access to the accessin the LIM tree
     * that is to be fixed.
     */
    public static LoopFlopConflictSet getConflictSet (Collection<LoopFlopConflict> conflicts, Map correlation)
    {
        final GenericJob gj = EngineThread.getEngine().getGenericJob();
        final int fixPolicy = ((OptionInt)gj.getOption(OptionRegistry.LOOP_RESOURCE_FIX_POLICY)).getValueAsInt(CodeLabel.UNSCOPED);
        //final int fixPolicy = FIX_NONE;
        return getConflictSet(conflicts, correlation, fixPolicy);
    }
    
    /**
     * Returns a LoopFlopConflictSet object based on the collection of
     * conflicts and specified correlation using the specified
     * resolution policy
     *
     * @param conflicts a Collection of {@link LoopFlopConflict}
     * objects.
     * @param correlation a Map of Component to Component, used in
     * transferring the conflict access to the accessin the LIM tree
     * that is to be fixed.
     * @param policy the resolution policy.
     */
    public static LoopFlopConflictSet getConflictSet (Collection<LoopFlopConflict> conflicts, Map correlation, int policy)
    {
        // Just in case we need the counts
        final Set firsts = new HashSet();
        final Set lasts = new HashSet();
        for (LoopFlopConflict conflict : conflicts)
        {
            firsts.add(conflict.getHeadAccess());
            lasts.add(conflict.getTailAccess());
        }
        
        switch (policy)
        {
            case FIX_NONE : return new FixNone();
            case FIRST_ALWAYS : return new FirstAlways(conflicts, correlation);
            case LAST_ALWAYS : return new LastAlways(conflicts, correlation);
            case FIX_FEWEST :
                if (firsts.size() <= lasts.size())
                    return new FirstAlways(conflicts, correlation);
                else
                    return new LastAlways(conflicts, correlation);
            case FIX_MOST : 
                if (firsts.size() >= lasts.size())
                    return new FirstAlways(conflicts, correlation);
                else
                    return new LastAlways(conflicts, correlation);
            default : throw new IllegalArgumentException("Illegal loop resource fix policy. " + policy);
        }
    }
    
    protected LoopFlopConflictSet (Collection conflicts, Map correlation)
    {
        this.conflicts = new HashSet(conflicts);
        this.correlation = correlation;
    }

    /**
     * Processes each {@link LoopFlopConflict} in this set and inserts
     * {@link ResourceDependency} objects to resolve that conflict
     * based on the currently active policy.
     */
    public void resolveConflicts ()
    {
        if (_loop.db) _loop.ln("Resolving " + getConflicts().size() + " conflicts");
        
        for (LoopFlopConflict conflict : getConflicts())
        {
            resolve(conflict);
        }
    }

    /**
     * Returns an unordered collection of all relevant components in
     * the path to each resolvable access in each conflict contained
     * in this set.  Specifically this method uses the
     * {@link LoopFlopConflict#getHeadPath} or
     * {@link LoopFlopConflict#getTailPath} methods to determine the
     * path components.
     */
    public abstract Collection<Component> getFixableAccessPathComponents ();
    
    protected abstract void resolve (LoopFlopConflict conflict);

    protected Set<LoopFlopConflict> getConflicts ()
    {
        return this.conflicts;
    }

    /**
     * Inserts a {@link ResourceDependency} from the component owners
     * inbuf to the component GO with a min clocks specification of 1.
     */
    protected void delayFirst (Component comp)
    {
        if (_loop.db) _loop.ln("\tDelaying first component " + comp);
        
        Component access = correlation.get(comp);
        // Insert a 1 cycle resource dependency from the component
        // to the outbuf (if !first) or from inbuf to access (if
        // first)
        final Entry accessEntry = (Entry)access.getEntries().get(0);
        accessEntry.addDependency(access.getGoPort(), new ResourceDependency(access.getOwner().getGoPort().getPeer(), 1));
    }
    
    /**
     * Inserts a {@link ResourceDependency} from the component exit
     * done bus to the component owners outbuf with a min clocks
     * specification of 1. 
     */
    protected void delayLast (Component comp)
    {
        if (_loop.db) _loop.ln("\tDelaying last component " + comp);
        
        Component access = correlation.get(comp);
        Exit moduleExit = access.getOwner().getExit(Exit.DONE);
        if (moduleExit == null)
        {
            assert access.getOwner().getExits().size() == 1 : "Module must have a DONE exit, or only 1 exit";
            moduleExit = (Exit)access.getOwner().getExits().iterator().next();
        }
        Exit accessExit = access.getExit(Exit.DONE);
        if (accessExit == null)
        {
            assert access.getExits().size() == 1 : "Access must have a DONE exit, or only 1 exit";
            accessExit = (Exit)access.getExits().iterator().next();
        }
        
        Entry obEntry = (Entry)moduleExit.getPeer().getEntries().get(0);
        obEntry.addDependency(moduleExit.getPeer().getGoPort(), new ResourceDependency(accessExit.getDoneBus(), 1));
    }

    private static class FixNone extends LoopFlopConflictSet
    {
        private FixNone () { super(Collections.EMPTY_SET, Collections.EMPTY_MAP); }

        protected void resolve (LoopFlopConflict conflict)
        {
            ;
        }
        
        public Collection<Component> getFixableAccessPathComponents ()
        {
            return (Collection<Component>)Collections.EMPTY_SET;
        }
    }
    
    private static class FirstAlways extends LoopFlopConflictSet
    {
        private FirstAlways (Collection conflicts, Map corr) { super(conflicts, corr); }

        protected void resolve (LoopFlopConflict conflict)
        {
            delayFirst(conflict.getHeadAccess());
        }
        
        public Collection<Component> getFixableAccessPathComponents ()
        {
            Set<Component> pathComps = new HashSet();
            for (LoopFlopConflict conflict : getConflicts())
            {
                pathComps.addAll(conflict.getHeadPath());
            }
            return pathComps;
        }
    }
    
    private static class LastAlways extends LoopFlopConflictSet
    {
        private LastAlways (Collection conflicts, Map corr) { super(conflicts, corr); }

        protected void resolve (LoopFlopConflict conflict)
        {
            delayLast(conflict.getTailAccess());
        }
        
        public Collection<Component> getFixableAccessPathComponents ()
        {
            Set<Component> pathComps = new HashSet();
            for (LoopFlopConflict conflict : getConflicts())
            {
                pathComps.addAll(conflict.getTailPath());
            }
            return pathComps;
        }
    }
    
}
