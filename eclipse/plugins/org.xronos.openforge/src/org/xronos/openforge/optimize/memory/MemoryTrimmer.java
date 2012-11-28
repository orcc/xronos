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

import java.util.*;

import org.xronos.openforge.app.EngineThread;
import org.xronos.openforge.lim.*;
import org.xronos.openforge.lim.memory.*;
import org.xronos.openforge.optimize.Optimization;
import org.xronos.openforge.util.naming.ID;


/**
 * MemoryTrimmer works in tandem with MemoryReducer to eliminate
 * unused bytes from memory.  This class does the simple job of
 * eliminating bytes from Allocations when the whole Allocation is not
 * accessed.  Note that does not mean that we can always delete the
 * Allocation.  The Allocation may still be the target of an
 * address-of operator, in which case we still need the allocation in
 * order to get an address for it.  To accomodate that case, we simply
 * create a 0 length Allocation and replace the non-accessed
 * allocation with it if there are any LocationConstants that refer to
 * it.  In the case where there is neither a LocationConstant or an
 * LValue access to the Allocation, it is simply removed.
 *
 *
 * <p>Created: Thu Nov 13 10:15:01 2003
 *
 * @author imiller, last modified by $Author: imiller $
 * @version $Id: MemoryTrimmer.java 107 2006-02-23 15:46:07Z imiller $
 */
public class MemoryTrimmer implements Optimization
{
    private static final String _RCS_ = "$Rev: 107 $";

    /** Tracks the number of removed bytes, for user status */
    private int bytesRemoved = 0;
    
    public MemoryTrimmer ()
    {}

    /**
     * Reduces the number of bytes allocated in the memory by
     * converting any Allocation with no LValue accessors to a 0 byte
     * length Allocation.  All Pointers and LocationConstants to that
     * Allocation are retargetted to the 0 length Allocation.
     *
     * @param mem a non-null 'LogicalMemory'
     * least one pointer reference somewhere in a memory in the design.
     * @return a 'Map' of old Allocation to new Allocation.
     */
    private Map trim (LogicalMemory mem)
    {
        if (mem == null)
        {
            throw new IllegalArgumentException("Null memory not allowed");
        }
        
        Collection baseLocationMaps = BaseLocationMap.buildMaps(mem);
        Map prunedCorrelation = prune(baseLocationMaps, mem);

        // Now that we have the new allocation for each old
        // allocation, retarget any LocationConstants
        // (by definition there are no LValues to retarget)
        RetargetVisitor vis = new RetargetVisitor(prunedCorrelation);
        for (Iterator iter = (new HashSet(mem.getLocationConstants())).iterator(); iter.hasNext();)
        {
            ((Visitable)iter.next()).accept(vis);
        }

        return prunedCorrelation;
    }

    /**
     * A simple visitor used to fix the LocationConstants in the LIM
     * that point to any Allocation that we have changed to a 0 length
     * Allocation. 
     */
    private class RetargetVisitor extends FailVisitor
    {
        private final Map correlation;
        public RetargetVisitor (Map corrMap)
        {
            super("Retarget after memory trimming");
            this.correlation = corrMap;
        }
        
        // Fix the location constant by first changing its offset (via
        // chopStart) then correlating it over to the new base
        // allocation.
        public void visit (LocationConstant comp)
        {
            try
            {
                Location newLoc = Variable.getCorrelatedLocation(correlation, comp.getTarget());
                comp.setTarget(newLoc);
            } catch (IllegalArgumentException e)
            {
                // This exception signifies that the given correlation
                // does not contain a correlation for any base of the
                // target.  Thats OK in this situation where we may
                // only prune a few Allocations in the whole design.
            }
        }
    }
    
    /**
     * The prune method looks at each {@link BaseLocationMap} in the
     * collection 'maps' and for each one which has a max accessed
     * size of 0 a new Allocation with 0 bytes length
     * will be created.  This method is destructive and removes the
     * non accessed Allocation from the memory after creating a 0
     * length Allocation to replace it.  It then returns a mapping of
     * the old Allocation to new 0 length Allocation for any
     * BaseLocationMap which has no accessed locations.  This
     * information can then be used to prune the Allocations from
     * memory.  Any allocations which have no LValue accesses AND
     * which have no LocationConstants will be completely removed and
     * no 'empty' allocation created for it.
     *
     * @param maps a 'Collection' of {@link BaseLocationMap} objects.
     * one reference from a pointer somewhere in memory, and thus
     * cannot be removed.
     * @return a Map of old Allocation to new Allocation.
     */
    private Map prune (Collection maps, LogicalMemory mem)
    {
        // First find all the Allocations that have a location
        // constant that refers to them.
        final Set nonRemovableAllocs = new HashSet();
        for (Iterator iter = mem.getLocationConstants().iterator(); iter.hasNext();)
        {
            final LocationConstant locConst = (LocationConstant)iter.next();
            nonRemovableAllocs.add(locConst.getTarget().getAbsoluteBase());
        }
        for (Iterator iter = mem.getAccessingPointers().iterator(); iter.hasNext();)
        {
            final Pointer ptr = (Pointer)iter.next();
            nonRemovableAllocs.add(ptr.getTarget().getAbsoluteBase());
        }
        // Now find all Allocations that have no LValue accesses and
        // 'prune' them, unless they have no location constant too, in
        // which case we can simply delete it.
        final Map pruned = new HashMap();
        for (Iterator iter = maps.iterator(); iter.hasNext();)
        {
            final BaseLocationMap map = (BaseLocationMap)iter.next();
            if (map.maxSize() == 0)
            {
                Allocation alloc = (Allocation)map.getAbsoluteBase();
                if (!nonRemovableAllocs.contains(alloc))
                {
                    mem.delete(alloc);
                    bytesRemoved += alloc.getAddressableSize();
                }
                else if (alloc.getAddressableSize() > 0) // dont prune an empty one
                {
                    Allocation zeroAlloc = mem.allocate(
                        Scalar.buildScalar(new byte[0], alloc.getInitialValue().getAddressStridePolicy()));
                    pruned.put(alloc,zeroAlloc);
                    // Not strictly necessary to copy the block
                    // elements, but for debug it will be nice to see
                    // the lineage.
                    zeroAlloc.copyBlockElements(alloc);
                    mem.delete(alloc);
                    bytesRemoved += alloc.getAddressableSize();
                }
            }
        }
        return pruned;
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
        if (!(target instanceof Design))
        {
            return;
        }
        
        Design design = (Design)target;
        
        // Only applicable to LogicalMemories.
        final ObjectResolver resolver = ObjectResolver.resolve(design);

        Map baseMap = new HashMap();
        for (Iterator iter = design.getLogicalMemories().iterator(); iter.hasNext();)
        {
            LogicalMemory mem = (LogicalMemory)iter.next();
            
            Map correlation = trim(mem);
            baseMap.putAll(correlation);
        }

        /*
         * Now that all memories have been trimmed, we need to fix any
         * Pointers that are stored in ANY memory.  This is because
         * the Pointer may be initialized to a Location that has been
         * 'redefined' by the trimming process.  This visitor uses
         * the correlation data generated when each memory was moved
         * to figure out the new target for the Pointer.
         */
        PointerRetargetVis retarget = new PointerRetargetVis(baseMap);
        for (Iterator iter = design.getLogicalMemories().iterator(); iter.hasNext();)
        {
            LogicalMemory mem = (LogicalMemory)iter.next();
            mem.accept(retarget);
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
     * Clears the count of removed bytes
     */
    public void clear ()
    {
        this.bytesRemoved = 0;
    }
    
    /**
     * Reports, via {@link Job#info}, what optimization is being
     * performed
     */
    public void preStatus ()
    {
    	EngineThread.getGenericJob().info("eliminating unused variable allocations...");
    }
    
    /**
     * Reports, via {@link Job#verbose}, the results of <b>this</b>
     * pass of the optimization.
     */
    public void postStatus ()
    {
    	EngineThread.getGenericJob().verbose("removed " + this.bytesRemoved + " bytes of unused variables");
    }
    
}// MemoryTrimmer
