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

package net.sf.openforge.lim;

import java.util.*;

import net.sf.openforge.lim.memory.*;
import net.sf.openforge.lim.op.*;

/**
 * HeapWrite is an {@link OffsetMemoryWrite} whose offset value is a
 * constant that is assigned according to a symbol that is supplied
 * with the constructor. 
 *
 * @version $Id: HeapWrite.java 88 2006-01-11 22:39:52Z imiller $
 */
public class HeapWrite extends OffsetMemoryWrite
{
    private static final String _RCS_ = "$Rev: 88 $";

    private int offset;
    
    private Constant offsetConstant = null;

    /**
     * Constructs a <code>HeapWrite</code>.
     *
     * @param addressableLocations the number of locations to be read
     * @param maxAddressWidth the pre-optimized number of bits in the
     * address bus 
     * @param offset the number of locations between the base address
     * and the first location to be read
     * @param isSigned, true if this is a signed access
     * @param policy the {@link AddressStridePolicy} which governs
     * this memory access.
     */
    public HeapWrite (int addressableLocations, int maxAddressWidth, int offset, boolean isSigned, AddressStridePolicy policy)
    {
        this(addressableLocations, maxAddressWidth, offset, isSigned, (addressableLocations * policy.getStride()));
    }

    /**
     * Constructs a <code>HeapWrite</code>.
     *
     * @param addressableLocations the number of locations to be read
     * @param maxAddressWidth the pre-optimized number of bits in the
     * address bus 
     * @param offset the number of locations between the base address
     * and the first location to be read
     * @param isSigned, true if this is a signed access
     * @param accessBitWidth, the number of unoptimizedd bits in this
     * access 
     */
    public HeapWrite (int addressableLocations, int maxAddressWidth,
        int offset, boolean isSigned, int accessBitWidth)
    {
        this(new MemoryWrite(false, accessBitWidth, isSigned),
            addressableLocations, maxAddressWidth, offset);
    }
    

    /**
     * Constructs a <code>HeapWrite</code>.
     *
     * @param memoryWrite the actual {@link MemoryWrite} used to write
     *          the memory, contained in this Module
     * @param addressableLocations the number of locations to be written
     * @param maxAddressWidth the pre-optimized number of bits in the
     * address bus 
     * @param offset the number of locations between the base address
     * and the first location to be read
     */
    private HeapWrite (MemoryWrite memoryWrite, int addressableLocations, int maxAddressWidth, int offset)
    {
        super(memoryWrite, addressableLocations, maxAddressWidth);

        /*
         * Create a Constant to drive the offset value.
         */
        this.offset = offset;
        this.offsetConstant = new SimpleConstant(offset, maxAddressWidth, true);
        insertComponent(offsetConstant, 0);
        final Entry offsetEntry = offsetConstant.makeEntry(getInBuf().getExit(Exit.DONE));
        offsetEntry.addDependency(offsetConstant.getGoPort(),
            new ControlDependency(offsetEntry.getDrivingExit().getDoneBus()));

        /*
         * Connect the offset Constant as the righthand input to the add.
         */
        final AddOp addOp = getAddOp();
        final Entry addEntry = (Entry)addOp.getEntries().get(0);
        addEntry.addDependency(addOp.getRightDataPort(), new DataDependency(offsetConstant.getValueBus()));
    }

    /**
     * Gets the offset of the write.
     *
     * @return the number of locations between the base address and the first
     *          location to be written
     */
    public int getOffset ()
    {
        return offset;
    }

    /**
     * Modifies the offset of this heap access, rebuilding the
     * necessary internal structure if necessary.
     *
     * @param offset a value of type 'int'
     */
    public void setOffset (int offset)
    {
        if ((this.offset != offset) || (getAddOp().getOwner() == null))
        {
            this.offset = offset;
            this.offsetConstant = new SimpleConstant(this.offset, getMaxAddressWidth(), true);
            this.rebuildAdder();
        }
    }
    
    /**
     * Gets the offset Constant of the write
     *
     * @return the Constant representation of the offset value
     */
    Constant getOffsetConstant()
    {
        return offsetConstant;
    }

    public void accept(Visitor vis)
    {
        vis.visit(this);
    }

    protected void rebuildAdder ()
    {
        super.rebuildAdder();

        Constant offsetConst = getOffsetConstant();
        
        insertComponent(offsetConst, 0);
        final Entry offsetEntry = offsetConst.makeEntry(getInBuf().getExit(Exit.DONE));
        offsetEntry.addDependency(offsetConst.getGoPort(),
            new ControlDependency(offsetEntry.getDrivingExit().getDoneBus()));
        
        /*
         * Connect the offset Constant as the righthand input to the add.
         */
        final AddOp addOp = getAddOp();
        final Entry addEntry = (Entry)addOp.getEntries().get(0);
        addEntry.addDependency(addOp.getRightDataPort(), new DataDependency(offsetConst.getValueBus()));
    }
    
    public boolean removeComponent (Component component)
    {
        boolean ret = super.removeComponent(component);
        if (component == this.offsetConstant)
            this.offsetConstant = null;
        return ret;
    }
    
    /**
     * Clones this object.  A new HeapWrite is created using a copy of
     * the {@link MemoryWrite}, including its
     * {@link LogicalMemoryPort}, and the {@link Location} from the
     * {@link AllocationConstant}.  The clone is added as an access to
     * the {@link LogicalMemoryPort}.
     *
     * @return the clone
     */
    public Object clone () throws CloneNotSupportedException
    {
//         final HeapWrite clone = new HeapWrite(getAccessLocationCount(),
//             offsetConstant.getValueBus().getValue().getSize(),
//             offset, getMemoryWrite().isSigned(), getMemoryWrite().getWidth());
        
//         clone.setBlockElement(this.getBlockElement());
//         if (getLogicalMemoryPort() != null)
//         {
//             getLogicalMemoryPort().addAccess(clone);
//         }
        
//         this.copyComponentAttributes(clone);
        HeapWrite clone = (HeapWrite)super.clone();
        if (getLogicalMemoryPort() != null)
        {
            getLogicalMemoryPort().addAccess(clone);
        }
        return clone;
    }

    protected void cloneNotify (Module moduleClone, Map cloneMap)
    {
        super.cloneNotify(moduleClone, cloneMap);
        ((HeapWrite)moduleClone).offsetConstant = (Constant)cloneMap.get(this.offsetConstant);
    }
}
