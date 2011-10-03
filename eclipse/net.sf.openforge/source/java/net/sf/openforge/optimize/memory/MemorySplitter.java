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

package net.sf.openforge.optimize.memory;


import java.util.*;

import net.sf.openforge.app.EngineThread;
import net.sf.openforge.lim.*;
import net.sf.openforge.lim.memory.*;
import net.sf.openforge.optimize.*;
import net.sf.openforge.util.Debug;

/**
 * <p>The goal is to reduce contention at LogicalMemories by grouping
 * Locations according to their access characteristics and creating
 * new LogicalMemories for each of those groupings.
 * <p>Locations are grouped such that no MemoryAccess targets a
 * Location (or the underlying byte range it represents) in more than
 * 1 memory.  This is further constrained that for any IndexLocation
 * the IndexLocation.getBaseLocation() must be contained entirely in
 * one memory.
 * <p>Note that it is NOT a requirement of this class that variables
 * in the source code be fully contained within the same memory.  For
 * example, a C structure may have component fields in seperate
 * memories if there are no common accesses of those fields.
 * Similarly, if an integer is only ever accessed as bytes it is
 * possible for each of those bytes to be contained in seperate
 * memories.
 * <p>The new memories are created from the MemoryContents by doing a
 * deep copy of the logical value which backs each Allocation
 * represented, then allocating that copy in the new memory.  This
 * simplifies the issues related to Pointers which may be allocated in
 * one memory, while pointing to another memory.  It is far easier to
 * have a unique Pointer object in each memory, instead of having a
 * single Pointer object allocated in multiple memories.  In cases
 * where the pointer is in a struct and we split the struct into
 * multiple memories, the same Pointer object was the init value in
 * several memories even though it was used in only one.
 * <P>Once the copied memories are created for ALL split memories, we
 * go back through and use correlation data combined from each memory
 * to retarget the address sources for each MemoryContents.
 *
 * <p>Created: Wed Oct 16 15:30:18 2002
 *
 * @author imiller, last modified by $Author: imiller $
 * @version $Id: MemorySplitter.java 70 2005-12-01 17:43:11Z imiller $
 */
public class MemorySplitter implements Optimization 
{
    private static final String _RCS_ = "$Rev: 70 $";

    private int preSplitMemCount = 0;
    private int postSplitMemCount = 0;

    /**
     * The complete set of MemoryContents objects for all split
     * memories.  This set is regenerated on each run of this
     * optimization 
     */
    private Set memoryContents = new HashSet();
    /**
     * The Mapping of old Pointer (in unsplit memory) to a Set of new
     * pointers.  The set generally contains only one Pointer, but in
     * the case of a struct being split across multiple memories, it
     * is possible that the pointer will have multiple copies, even
     * though only one of them is 'usefull'
     */
    private Map pointerCorrelations = new HashMap();
    
    /**
     * Divides the supplied LogicalMemory into a Set of 
     * LogicalMemories in which there is no overlap between the set of
     * MemoryAccess and Locations in any 2 LogicalMemories.
     * 
     * <p>requires: non-null memory
     * <p>modifies: Accesses will be removed from the supplied
     *              LogicalMemory, thus affecting its state.
     * <p>effects: returns a Set of LogicalMemory objects that were
     *             derived from the given LogicalMemory.  The supplied
     *             LogicalMemory may be contained in the returned Set
     *             if no modifications to the memory are made.
     *
     * @param memory the memory to be split
     * @param resolver the {@link ObjectResolver} used to analyze the {@link Design}
     *          containing the <code>memory</code>
     *
     * @return a non-null 'Set' of derived LogicalMemory objects, may
     *         contain the parameter LogicalMemory.
     *
     * @throws IllegalArgumentException if 'memory' is null.
     */
    public Set splitMemory (LogicalMemory memory, ObjectResolver resolver)
    {
        // hold all the memory contents
        Set memoryContentsSet = new HashSet();

        /*
         * Examine each LValue from the LogicalMemory and derive a MemoryContents
         * to which it can be added.
         */
        for (Iterator iter = memory.getLValues().iterator(); iter.hasNext();)
        {
            final LValue lvalue = (LValue)iter.next();


            /*
             * Find all MemoryContents that overlap the LValue.
             */
            final Set overlappingMemoryContents = new HashSet();
            final Collection accessedLocations = memory.getAccesses(lvalue);

            final Set addressSources = resolver.getAddressSources(lvalue);
            for (Iterator jter = accessedLocations.iterator(); jter.hasNext();)
            {
                final Location accessedLocation = (Location)jter.next();
                for (Iterator kter = memoryContentsSet.iterator(); kter.hasNext();)
                {
                    final MemoryContents memoryContents = (MemoryContents)kter.next();
                    if (memoryContents.overlaps(accessedLocation, addressSources))
                    {
                        overlappingMemoryContents.add(memoryContents);
                    }
                }
            }

            /*
             * From the Set of overlapping MemoryContents, derive a single
             * MemoryContents to which the LValue can be added.
             */
            MemoryContents lvalueMemoryContents = null;
            if (overlappingMemoryContents.size() > 1)
            {
                /*
                 * If there is more than one overlapping MemoryContents, merge
                 * them all to produce a single MemoryContents.
                 */
                lvalueMemoryContents = new MemoryContents();
                for (Iterator miter = overlappingMemoryContents.iterator(); miter.hasNext();)
                {
                    lvalueMemoryContents.merge((MemoryContents)miter.next());
                }
                memoryContentsSet.removeAll(overlappingMemoryContents);
                memoryContentsSet.add(lvalueMemoryContents);
            }
            else if (overlappingMemoryContents.size() == 1)
            {
                /*
                 * If there was only one overlapping MemoryContents, then
                 * add the access to that one.
                 */
                lvalueMemoryContents = (MemoryContents)overlappingMemoryContents.iterator().next();
            }
            else
            {
                /*
                 * Otherwise create a new MemoryContents for the LValue.
                 */
                lvalueMemoryContents = new MemoryContents();
                memoryContentsSet.add(lvalueMemoryContents);
            }

            lvalueMemoryContents.addContents(lvalue, new HashSet(accessedLocations), addressSources);
        }

        /*
         * If there is only one MemoryContents, then the LValues must all refer
         * to the same LogicalMemory.  No splitting can be done, so just return
         * the original LogicalMemory.
         */
        if (memoryContentsSet.size() == 1)
        {
            return Collections.singleton(memory);
        }

        if (_optimize.db) _optimize.d.ln(" MemorySplitter generates "+memoryContentsSet.size()+" logical memories");
        
        this.memoryContents.addAll(memoryContentsSet);
        /*
         * Otherwise, create a new LogicalMemory for each MemoryContents.
         */
        HashSet result = new HashSet();
        for (Iterator iter = memoryContentsSet.iterator(); iter.hasNext();)
        {
            MemoryContents memoryContents = (MemoryContents)iter.next();
            MemoryContents.MemCopyTuple tuple = memoryContents.buildMemory();
            LogicalMemory lm = tuple.getCopiedMemory();
            merge(this.pointerCorrelations, tuple.getPointerCorrelation());
            result.add(lm);
        }

        return result;
    }
    
    /**
     * Merges the contents of the source one->one mapping (Pointer to
     * Pointer) into the target one->many mapping (Pointer to Set of
     * pointers) 
     *
     * @param target a one to many Map of Pointer to Set of Pointers.
     * @param source a one to one Map of Pointer to Pointer.
     */
    private static void merge (Map target, Map source)
    {
        for (Iterator iter = source.keySet().iterator(); iter.hasNext();)
        {
            Object key = iter.next();
            Set targets = (Set)target.get(key);
            if (targets == null)
            {
                targets = new HashSet();
                target.put(key, targets);
            }
            targets.add(source.get(key));
        }
    }
    
    /**
     * This method calls {@link MemoryContents#retargetAddressSources}
     * on every MemoryContents object from which a new memory was
     * created.  This is done after the splitting of ALL memories so
     * that we can correctly correlate the original (unsplit) pointers
     * to their corresponding Pointer objects in the split memories.
     */
    private void updateAddressSources ()
    {
        for (Iterator iter = this.memoryContents.iterator(); iter.hasNext();)
        {
            ((MemoryContents)iter.next()).retargetAddressSources(this.pointerCorrelations);
        }
    }
    
    //
    // Optimization interface.
    //
    
    /**
     * Applies this optimization to a given target.
     *
     * @param target the target on which to run this optimization
     */
    public void run (Visitable target)
    {
        // TBD, only applicable to a Design, split each memory.
        // Handle adding new memories to design and setting the
        // pre/post split memory numbers.
        assert (target instanceof Design): "MemorySplitter.run() only runs on a Design";
        Design design=(Design) target;

        ObjectResolver resolver=ObjectResolver.resolve(design);

        Set oldMemories=new HashSet();
        Set newMemories=new HashSet();
        for (Iterator iter=design.getLogicalMemories().iterator(); iter.hasNext();)
        {
            LogicalMemory lm=(LogicalMemory) iter.next();

            if (_optimize.db)
            {
                for (Iterator kter=lm.getLValues().iterator(); kter.hasNext();)
                {
                    LValue lv = (LValue)kter.next();
                    for (Iterator jter = lm.getAccesses(lv).iterator(); jter.hasNext();)
                    {
                        Location loc = (Location)jter.next();
                        _optimize.d.ln("  location: "+loc);
                        _optimize.d.ln("    base: "+loc.getBaseLocation()+" min delta: "+loc.getMinDelta()
                            +" max delta: "+loc.getMaxDelta()+" size: "+loc.getAddressableSize());
                    }
                }
            }

            Set result=splitMemory(lm, resolver);

            // only add if the memory was split
            if (result.size() > 1)
            {
                oldMemories.add(lm);
                newMemories.addAll(result);
            }
        }

        /* Retarget pointers.  It must be done here (after ALL
         * memories are split) so that we can correctly figure out
         * which new Pointers were derived from which old Pointers.
         * The correlation of old Allocation to new Allocation (used
         * to map old to new target of pointers and location
         * constants) is maintained in the MemoryContents class
         * because pointers need to follow the
         * copy of the allocation that they are related to.  ie,
         * memory splitting can make several copies of a given
         * allocation, and we need to be sure that they follow the
         * correct one. */
        updateAddressSources();

        /* Let dead componant removal take care of removing the old
         * memory.  Otherwise, we may remove a memory that still has
         * Allocations that are referred to by address of operations.
        for (Iterator iter=oldMemories.iterator(); iter.hasNext();)
        {
            design.removeMemory((LogicalMemory) iter.next());
        }
        */

        /* Add each new memory to the design */
        for (Iterator iter=newMemories.iterator(); iter.hasNext();)
        {
            LogicalMemory mem = (LogicalMemory)iter.next();
            design.addMemory(mem);
        }
    }

    /**
     * Returns false, the didModify is used to determine if this
     * optimization caused a change which necessitates other
     * optimizations to re-run.
     */
    public boolean didModify ()
    {
        return false;
    }
    /**
     * Does nothing.
     */
    public void clear ()
    {
        this.memoryContents.clear();
        this.pointerCorrelations.clear();
    }
    
    /**
     * Reports, via {@link Job#info}, what optimization is being
     * performed
     */
    public void preStatus ()
    {
    	EngineThread.getGenericJob().info("splitting memories...");
    }
    
    /**
     * Reports, via {@link Job#verbose}, the results of <b>this</b>
     * pass of the optimization.
     */
    public void postStatus ()
    {
        if (this.postSplitMemCount > this.preSplitMemCount)
        {
        	EngineThread.getGenericJob().info("split " + this.preSplitMemCount + " " +
                (this.preSplitMemCount == 1 ? "memory":"memories") +
                " into " + this.postSplitMemCount + ".");
        }
    }
    
}// MemorySplitter
