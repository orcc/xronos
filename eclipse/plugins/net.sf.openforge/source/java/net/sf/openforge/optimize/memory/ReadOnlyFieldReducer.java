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
import net.sf.openforge.lim.op.*;
import net.sf.openforge.optimize.*;

/**
 * ReadOnlyFieldReducer is responsible for converting any memory
 * access (LValue) whose target(s) is/are read only Location(s) to
 * Constants in the LIM.
 * 
 * <p>Created: Thu Oct 17 14:28:39 2002
 *
 * @author imiller, last modified by $Author: imiller $
 * @version $Id: ReadOnlyFieldReducer.java 2 2005-06-09 20:00:48Z imiller $
 */
public class ReadOnlyFieldReducer implements Optimization
{
    private static final String _RCS_ = "$Rev: 2 $";

    /** A Map of Location to Set of LValues that access that
     * Location. */
    private Map locationToLValues = new HashMap();

    /** A Map of Location to Set of Locations that overlap that
     * Location (as determined by Location.overlaps(). */
    private Map locationOverlaps = new HashMap();
    
    public ReadOnlyFieldReducer ()
    {}

    /**
     * Tests the given Location for being 'read only'.  There are 2
     * criteria for being a read only field.
     * 1. All accesses to this Location must be reads.
     * 2. All accesses to any Location that overlaps this Location
     * must also be reads.
     *
     * <p>requires: non-null Location
     * <p>modifies: none
     * <p>effects: returns true if all accesses to the Location are
     * read and all accesses to all overlapping Locations are reads.
     *
     * @param loc a 'Location'
     * @return true if all accesses to the Location are read and all
     * accesses to all overlapping Locations are reads.
     */
    protected boolean isReadOnly (Location loc)
    {
        boolean isReadOnly = true;

        for (Iterator accesses = ((Collection)locationToLValues.get(loc)).iterator(); accesses.hasNext();)
        {
            LValue lv = (LValue)accesses.next();
            if (lv.isWrite()) 
            {
                isReadOnly = false;
                break; 
            }
        }
        if (isReadOnly)
        {
            for (Iterator overlaps = ((Collection)locationOverlaps.get(loc)).iterator(); overlaps.hasNext();)
            {
                Location overlap = (Location)overlaps.next();
                for (Iterator accesses = ((Collection)locationToLValues.get(overlap)).iterator(); accesses.hasNext();)
                {
                    LValue lv = (LValue)accesses.next();
                    if (lv.isWrite()) 
                    {
                        isReadOnly = false;
                        break; 
                    }
                }
            }
        }
        return isReadOnly;
    }

    /**
     * Tests whether the given location was found to have any overlaps
     * in the most recently analyzed Design. (Mostly for testing
     * purposes)
     * 
     * @param loc the location to test
     * @return true if any overlapping Locations were found, false otherwise
     */
    boolean hasOverlap(Location loc)
    {
        Collection overlaps = (Collection)locationOverlaps.get(loc);
        return ((overlaps != null) && (overlaps.size() > 0));
    }
    
    /**
     * The MemoryAnalyzer populates the various analysis maps
     * in preparation for field reduction. 
     * 
     * @author <a href="mailto:andreas.kollegger@xilinx.com">Andreas Kollegger</a>
     * @version $Id: ReadOnlyFieldReducer.java 2 2005-06-09 20:00:48Z imiller $
     */
    private class MemoryAnalyzer extends DefaultVisitor
    {
        /** Indicates whether the MemoryAnalyzer found a Design to analyze. */
        private boolean foundDesign = false;
        
        /**
         * Reviews all the memories in a design. For each memory, retrieves all
         * the LValues, then locations accessed by the LValue, building the
         * locationToLValue mapping. Then iterate over all Locations which have
         * any access, and find overlapping Locations.
         * 
         * @see net.sf.openforge.lim.Visitor#visit(net.sf.openforge.lim.Design)
         */
        public void visit(Design design)
        {
            clear();

            for (Iterator memories = design.getLogicalMemories().iterator(); memories.hasNext();)
            {
                LogicalMemory memory = (LogicalMemory)memories.next();
                for (Iterator lvalues = memory.getLValues().iterator(); lvalues.hasNext();)
                {
                    LValue lv = (LValue)lvalues.next();
                    for (Iterator locations = memory.getAccesses(lv).iterator(); locations.hasNext();)
                    {
                        Location loc = (Location)locations.next();
                        Collection lvSet = (Collection)locationToLValues.get(loc);
                        if (lvSet == null)
                        {
                            lvSet = new HashSet();
                            locationToLValues.put(loc, lvSet);
                        }
                        lvSet.add(lv);
                    }
                }
            }
            for (Iterator locations = locationToLValues.keySet().iterator(); locations.hasNext();)
            {
                Location loc = (Location)locations.next();
                HashSet overlaps = new HashSet();
                for (Iterator otherLocations = locationToLValues.keySet().iterator(); otherLocations.hasNext();)
                {
                    Location otherLoc = (Location)otherLocations.next();
                    if ((otherLoc != loc) && (otherLoc.overlaps(loc)))
                    {
                        overlaps.add(otherLoc);
                    }
                }
                locationOverlaps.put(loc, overlaps);
            }
            foundDesign = true; // sanity check that the MemoryAnalyzer did something
        }
        
        /**
         * Indicates whether the MemoryAnalyzer visited a design.
         * 
         * @return true if a Design was visited and analyzed.
         */
        public boolean foundDesign()
        {
            return foundDesign;
        }
    }
    
    /**
     * Overrides the read LValue visit methods to determine if those
     * accesses are to read-only Locations.  If the target Locations
     * are read-only and all are the same Constant value then the
     * access is replaced with a Constant of that value.  Makes use of
     * the isReadOnly method to determine if target Location(s) is/are
     * read only.
     */
    private class FieldRemover extends DefaultVisitor
    {
        
        final HashMap readToConstant = new HashMap();
        
        /**
         * Creates a new FieldRemover, ready for action.
         */
        public FieldRemover()
        {
        }
        
        /**
         * Visits a visitable, identifying all read-only accesses,
         * and then replacing them with the related Constants.
         * 
         * @param v the visitable from which to remove read-only accesses
         */
        public void removeFrom(Visitable v)
        {
            // visiting populates the readToConstant mapping
            // with removable read-only accesses and the related constant
            v.accept(this);

            final net.sf.openforge.optimize.ComponentSwapVisitor swapper = 
                new net.sf.openforge.optimize.ComponentSwapVisitor();

            RemoveVisitor removeVisitor = new RemoveVisitor();
            for (Iterator readConstantPairs = readToConstant.entrySet().iterator(); readConstantPairs.hasNext();)
            {
                Map.Entry pair = (Map.Entry)readConstantPairs.next();
                Component lv = (Component)pair.getKey();
                swapper.replaceComponent(lv, (Constant)pair.getValue());
                lv.accept(removeVisitor);
            }
        }

        private class RemoveVisitor extends FailVisitor implements Visitor
        {
            public RemoveVisitor ()
            {
                super("Removal of read only field accesses");
            }
            public void visit (RegisterRead comp)
            {
                comp.removeFromResource();
            }
            public void visit (HeapRead comp)
            {
                comp.getLogicalMemoryPort().removeAccess(comp);
            }
            public void visit (AbsoluteMemoryRead comp)
            {
                comp.getLogicalMemoryPort().removeAccess(comp);
            }
        }
        
        /**
         * Evaluate and replace HeapRead LValues if all target
         * Location(s) are read only and the same constant value.
         *
         * <p>requires: none
         * <p>modifies: LIM, target LogicalMemory
         * <p>effects: none
         *
         * @param read an HeapRead LValue
         */
        public void visit (HeapRead read)
        {
            LogicalMemoryPort port = read.getLogicalMemoryPort();
            boolean isUniqueReadOnly = true;
            Constant commonConstant = null;
            
            for (Iterator locations = port.getLogicalMemory().getAccesses(read).iterator(); locations.hasNext();)
            {
                Location loc = (Location)locations.next();
                if (isReadOnly(loc))
                {
                    //Constant c = loc.getInitialValue().toConstant();
                    Constant c = null;
                    try { c = loc.getInitialValue().toConstant(); }
                    catch (Location.IllegalInitialValueContextException e)
                    {
                        // Can not determine a constant for the
                        // Location, so simply return and dont
                        // propagate the access.
                        return;
                    }
                    if (commonConstant == null)
                    {
                        commonConstant = c;
                    } 
                    else if (!commonConstant.isSameValue(c))
                    {
                        isUniqueReadOnly = false;
                        break;
                    }
                }
                else 
                {
                    isUniqueReadOnly = false;
                    break;
                }
            }
            
            if (isUniqueReadOnly && (commonConstant != null))
            {
                readToConstant.put(read, commonConstant);
            }
        }

        /**
         * Evaluate and replace RegisterRead access if the target
         * Register is read only.
         *
         * <p>requires: none
         * <p>modifies: LIM, target Register
         * <p>effects: none
         *
         * @param read a non-null RegisterRead
         */
        public void visit (RegisterRead regRead)
        {
            Register targetReg = (Register)regRead.getResource();
            if (targetReg.getWriteAccesses().size() == 0)
            {
                LogicalValue initValue = targetReg.getInitialValue();
                Constant c = initValue.toConstant();
                readToConstant.put(regRead, c);
            }
        }
        
        /**
         * Does nothing, array reads with constant index are converted
         * by half constant to heap reads.
         * 
         * @param read an ArrayRead LValue
         */
        public void visit (ArrayRead read)
        {
            // Do nothing here.  This is possible because half
            // constant propagation will replace all ArrayRead
            // accesses that have a constant index port with the
            // equivalent HeapRead access.  Due to the fact that this
            // optimization can only apply to the array accesses with
            // constant index, we can thus safely ignore array  reads
            // here.
            super.visit(read);
        }
        
        /**
         * Evaluate and replace AbsolutMemoryRead LValues if the
         * target Location is a read only location.
         *
         * <p>requires: none
         * <p>modifies: LIM, target LogicalMemory
         * <p>effects: none
         *
         * @param read an AbsoluteMemoryRead LValue
         */
        public void visit (AbsoluteMemoryRead read)
        {
            LogicalMemoryPort port = read.getLogicalMemoryPort();
            Collection locations = port.getLogicalMemory().getAccesses(read);

            assert (locations.size() == 1) : "AbsoluteMemoryRead should only access a sinlge location.";
            Location loc = (Location)locations.iterator().next();

            if (isReadOnly(loc))
            {
                Constant c = null;
                try { c = loc.getInitialValue().toConstant(); }
                catch (Location.IllegalInitialValueContextException e)
                {
                    // Can not determine a constant for the
                    // Location, so simply return and dont
                    // propagate the access.
                    return;
                }
                readToConstant.put(read, c);
            }
        }
        
    }
    
    //
    // Optimization interface.
    //
    
    /**
     * Applies this optimization to a given target.
     *
     * @param target the target on which to run this optimization
     * @throws 
     */
    public void run (Visitable target)
    {
        if (!(target instanceof Design))
            return;
        
        ObjectResolver resolver=ObjectResolver.resolve((Design)target);

        final MemoryAnalyzer analyzer = new MemoryAnalyzer();
        target.accept(analyzer);
        assert analyzer.foundDesign() : "optimization only operates on a Design.";
        final FieldRemover fieldRemover = new FieldRemover();
        fieldRemover.removeFrom(target);
    }

    /**
     * Sets the object resolve map to null.  It will be rebuilt as
     * soon as the next 'component' is traversed.
     */
    public void clear ()
    {
        locationOverlaps.clear();
        locationToLValues.clear();
    }

    /**
     * returns true if the current pass of this optimization modified
     * the LIM.
     */
    public boolean didModify ()
    {
        // TBD
        return false;
    }
    
    /**
     * Reports, via {@link Job#info}, what optimization is being
     * performed
     */
    public void preStatus ()
    {
    	EngineThread.getGenericJob().info("optimizing read-only memory locations...");
    }
    
    /**
     * Reports, via {@link Job#verbose}, the results of <b>this</b>
     * pass of the optimization.
     */
    public void postStatus ()
    {
        //Job.verbose("reduced " + getReplacedNodeCount() + " expressions");
    }
    
}// ReadOnlyFieldReducer
