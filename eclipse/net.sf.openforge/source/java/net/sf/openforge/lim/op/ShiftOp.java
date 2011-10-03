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

package net.sf.openforge.lim.op;

import net.sf.openforge.lim.*;
import net.sf.openforge.report.*;


/**
 * ShiftOp is the base class for all types of Shifts in the LIM and
 * contains functionality common to them all.  A ShiftOp must know the
 * maximum number of stages necessary to implement the shift.
 *
 * <p>Created: Thu Oct  3 13:36:20 2002
 *
 * @author imiller, last modified by $Author: imiller $
 * @version $Id: ShiftOp.java 2 2005-06-09 20:00:48Z imiller $
 */
public abstract class ShiftOp extends BinaryOp 
{
    private static final String _RCS_ = "$Rev: 2 $";

    /** The maximum number of stages which should be needed to 
     * perform this shift operation. Which should be log2 of the
     * natural size of this operation */
    private int max_stages = 64;
    
    /**
     * Constructs a new shift.
     *
     * @param maxStages the maximum number of stages necessary to
     * build a functionally correct shifter, equal to log base2 of the
     * natural size of the data type being shifted (rounded up).
     */
    public ShiftOp (int maxStages)
    {
        super();
        this.max_stages = maxStages;
    }

    /**
     * Gets the gate depth of this component.  This is the maximum number of gates
     * that any input signal must traverse before reaching an {@link Exit}.
     *
     * @return a non-negative integer
     */
    public int getGateDepth ()
    {
        if ((getRightDataPort().getValue() != null) &&
            getRightDataPort().getValue().isConstant())
        {
            return 0;
        }
        return (3 * getStages());
    }

    /**
     * Gets the FPGA hardware resource usage of this component.
     *
     * @return a FPGAResource objec
     */
    public FPGAResource getHardwareResourceUsage ()
    {
        int lutCount = 0;
        
        Value leftValue = getLeftDataPort().getValue();
        Value rightValue = getRightDataPort().getValue();
        
        for (int i = 0; i < Math.max(leftValue.getSize(), rightValue.getSize()); i++)
        {
            Bit leftBit = null;
            Bit rightBit = null;
            if (i < leftValue.getSize())
            {
                leftBit = leftValue.getBit(i);
            }
            if (i < rightValue.getSize())
            {
                rightBit = rightValue.getBit(i);
            }
            
            if ((leftBit != null))
            {
                if (leftBit.isCare())
                {
                    lutCount += 5;
                }
            }
        }

        FPGAResource hwResource = new FPGAResource();
        hwResource.addLUT(lutCount);
        
        return hwResource;
    }
    
    /**
     * Get the maximum number of stages which should be needed to 
     * perform this shift operation.
     */
    public int getMaxStages () 
    {
        return max_stages;
    }

    /**
     * Gets the constant value applied to the shift magnitude input or
     * -1 if not a constant value.
     *
     * @return a value of type 'int'
     */
    public int getShiftMagnitude ()
    {
        if (getRightDataPort().getValue() == null)
        {
            getRightDataPort().pushValueForward();
        }
        
        Value magValue = getRightDataPort().getValue();
        if (magValue.isConstant())
        {
            int shiftMag = (int)magValue.getValueMask();
            int mask = 0;
            for (int i=0; i < getMaxStages(); i++)
            {
                mask = (mask << 1) | 1;
            }
            shiftMag &= mask;
            
            return shiftMag;
        }
        return -1;
    }

    /**
     * Gets the actual number of stages needed to compute the shift, based
     * upon the current {@link Value} of the right data {@link Port}.
     *
     * @return the number equal to the number of non-constant care
     *         bits in the right data port value
     */
    private int getStages ()
    {
        int stages = 0;
        final Value shiftValue = getRightDataPort().getValue();
        for (int bitPosition = 0; bitPosition < shiftValue.getSize(); bitPosition++)
        {
            Bit bit = shiftValue.getBit(bitPosition);
            //if (shiftValue.getBit(bitPosition) == Bit.CARE)
            if (!bit.isConstant() && bit.isCare())
            {
                stages++;
            }
        }
        return stages;
    }
    
}// ShiftOp
