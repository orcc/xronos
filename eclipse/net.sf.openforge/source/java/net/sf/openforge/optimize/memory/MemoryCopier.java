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

import net.sf.openforge.lim.memory.*;
import net.sf.openforge.util.naming.ID;

/**
 * MemoryCopier performs two facilities:<br>
 * 1. Creates a complete copy of a LogicalMemory<br>
 * 2. Associates Locations in the original LogicalMemory with the
 * corresponding Location in the new LogicalMemory.
 *
 * <p>Created: Wed Aug 20 17:05:18 2003
 *
 * @author imiller, last modified by $Author: imiller $
 * @version $Id: MemoryCopier.java 556 2008-03-14 02:27:40Z imiller $
 */
public class MemoryCopier 
{
    private static final String _RCS_ = "$Rev: 556 $";

    private LogicalMemory sourceMemory;
    
    private LogicalMemory copiedMemory;

    private Map locationMap = new HashMap();
    
    /**
     * Creates a new MemoryCopier that is used to copy and correlate
     * the specified LogicalMemory
     * 
     * <p>requires: non-null LogicalMemory
     * <p>modifies: this
     * <p>effects: none
     *
     * @param memory a LogicalMemory
     * @throws IllegalArgumentException if 'memory' is null
     */
    public MemoryCopier (LogicalMemory sourceMemory)
    {
        if (sourceMemory == null)
        {
            throw new IllegalArgumentException("null logicalMemory argument");
        }
        else
        {
            this.sourceMemory = sourceMemory;
        }
    }
    
    /**
     * Gets the copied LogicalMemory.
     *
     * <p>requires: none
     * <p>modifies: none
     * <p>effects: retrieves the copied (deep copy) LogicalMemory
     * 
     * @return a LogicalMemory whose contents identically match those
     * of the original LogicalMemory.
     */
    public LogicalMemory getCopy ()
    {
        if (this.copiedMemory == null)
        {
            this.copiedMemory = new LogicalMemory(this.sourceMemory.getMaxAddressWidth());
        
            /*
             * Copy the Allocations and keep association in this.locationMap.
             */
            for (Iterator allocationIter = this.sourceMemory.getAllocations().iterator();
                 allocationIter.hasNext();)
            {
                final Allocation sourceAllocation = (Allocation)allocationIter.next();
                final Allocation copiedAllocation = this.copiedMemory.allocate(sourceAllocation.getInitialValue().copy());
                ID.copy(sourceAllocation, copiedAllocation);
                copiedAllocation.copyBlockElements(sourceAllocation);
                this.locationMap.put(sourceAllocation, copiedAllocation);
            }

            /*
             * Reset the target location for any Pointer initial values.
             */
            for (Iterator allocationIter = this.sourceMemory.getAllocations().iterator();
                 allocationIter.hasNext();)
            {
                final Allocation sourceAllocation = (Allocation)allocationIter.next();
                final Allocation copiedAllocation = (Allocation)this.locationMap.get(sourceAllocation);

                // tbd -- use Ian's visitor
                if (copiedAllocation.getInitialValue() instanceof Pointer)
                {
                    final Location originalTarget = ((Pointer)sourceAllocation.getInitialValue()).getTarget();
                    final Location newTarget = (Location)this.locationMap.get(originalTarget);
                    ((Pointer)copiedAllocation.getInitialValue()).setTarget(newTarget);
                }
            }
            
            /*
             * Copy the LogicalMemoryPorts.
             */ 
            Map logicalMemoryPortMap = new HashMap();
            for (Iterator memoryPortIter = this.sourceMemory.getLogicalMemoryPorts().iterator(); memoryPortIter.hasNext();)
            {
                final LogicalMemoryPort sourceMemoryPort = (LogicalMemoryPort)memoryPortIter.next();
                final LogicalMemoryPort copiedMemoryPort = this.copiedMemory.createLogicalMemoryPort();
                logicalMemoryPortMap.put(sourceMemoryPort, copiedMemoryPort);
            }
            
            /*
             * Copy the read/write accesses on each LogicalmemoryPort.
             */
            for (Iterator lValueIter = this.sourceMemory.getLValues().iterator(); lValueIter.hasNext();)
            {
                final LValue lValue = (LValue)lValueIter.next();
                final LogicalMemoryPort sourceMemPort = this.sourceMemory.getLogicalMemoryPort(lValue);
                final LogicalMemoryPort copiedMemPort = (LogicalMemoryPort)logicalMemoryPortMap.get(sourceMemPort);
                
                final Collection sourceLocations = this.sourceMemory.getAccesses(lValue);
                for (Iterator sourceLocationIter = sourceLocations.iterator(); sourceLocationIter.hasNext();)
                {
                    final Location sourceLocation = (Location)sourceLocationIter.next();
                    final Location copiedLocation = Variable.getCorrelatedLocation(this.locationMap,
                        sourceLocation);
                    this.locationMap.put(sourceLocation, copiedLocation);
                    copiedMemPort.addAccess(lValue, copiedLocation);
                }
            }

            /*
             * Copy and add each LocationConstant.
             */
            for (Iterator constIter = this.sourceMemory.getLocationConstants().iterator();
                 constIter.hasNext();)
            {
                final LocationConstant locationConstant = (LocationConstant)constIter.next();
                final Location newLocation = Variable.getCorrelatedLocation(this.locationMap,
                    locationConstant.getTarget());
                this.locationMap.put(locationConstant.getTarget(), newLocation);
                this.copiedMemory.addLocationConstant(new LocationConstant(newLocation,
                                                          copiedMemory.getMaxAddressWidth(), this.sourceMemory.getAddressStridePolicy()));
            }
        
            /*
             * If the original has a StructuralMemory, build one for the copy.
             */
            assert this.sourceMemory.getStructuralMemory() == null : "Unable to copy a physically implemented memory.";
        }
        
        return this.copiedMemory;
    }

    /**
     * Returns the Map of old Location to new Location as generated
     * during the memory copy.
     *
     * @return a 'Map' of Location : Location
     */
    public Map getLocationMap ()
    {
        return Collections.unmodifiableMap(this.locationMap);
    }
    

    /**
     * Retrieves the source {@link LogicalMemory} which is used for
     * making a copy. 
     */
    public LogicalMemory getOriginalMemory ()
    {
        return this.sourceMemory;
    }
    
    
}// MemoryCopier
