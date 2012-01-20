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

package net.sf.openforge.lim.memory;

import net.sf.openforge.lim.*;
import net.sf.openforge.lim.op.*;
import net.sf.openforge.util.*;
import net.sf.openforge.util.naming.*;

/**
 * <code>EndianSwapper</code> is an extension of {@link Module} and
 * contains one data input and one data output as its only used
 * buses. The purpose of the module is to correctly convert one
 * endianness to the other as needed.
 *
 * <p>Created: Wed Mar 17 12:04:14 2004
 * 
 * @author cwu, last modified by $Author: imiller $
 * @version $Id: EndianSwapper.java 70 2005-12-01 17:43:11Z imiller $
 */
public class EndianSwapper extends Module
{
    public EndianSwapper (int memWidth)
    {
        this(memWidth, AddressStridePolicy.BYTE_ADDRESSING.getStride());
    }
    
    public EndianSwapper (int memWidth, int strideBits)
    {
        // 1 input port and 1 output bus
        super(1);

        // The stride limit is based on the maskValue below (easy to
        // fix) the memory width limit is based on the fact that we
        // cannot create a simple constant for the mask of over 64
        // bits. 
        assert strideBits <= 64 : "Cannot swap in units over 64 bits";
        assert memWidth <= 64 : "Cannot swap a memory of over 64 bits";
        
        Exit exit = makeExit(1);

        getInputPort().setUsed(true);
        getInputPort().getPeer().setIDLogical(ID.showLogical(this)+"_in");
        getOutputBus().setUsed(true);
        getOutputBus().setIDLogical(ID.showLogical(this)+"_out");

        int byteWidth = 1;
        int maxShiftStage = MathStuff.log2(memWidth);
        
        // Width in addressable locations, rounded up to the nearest
        // power of 2.
        while (memWidth > (byteWidth * strideBits))
        {
            byteWidth <<= 1;
        }

        long maskValue = 0;
        for (int i=0; i < strideBits; i++)
        {
            maskValue <<= 1;
            maskValue |= 0x1L;
        }
        
        final OrOpMulti orOpMulti = new OrOpMulti();
        
        for (int i = 0; i < byteWidth; i++)
        {
            long shiftMagnitude = 0;
            Constant shiftConstant = null;
            ShiftOp shiftOp = null;
            
            if (i < byteWidth/2)
            {
                shiftMagnitude = (long)(byteWidth - (2 * i) - 1);
                shiftConstant = new SimpleConstant((8 * shiftMagnitude), maxShiftStage, false);
                shiftOp = new RightShiftUnsignedOp(maxShiftStage);
            }
            else
            {
                shiftMagnitude = (long)(2 * (i + 1) - byteWidth - 1);
                shiftConstant = new SimpleConstant((8 * shiftMagnitude), maxShiftStage, false);
                shiftOp = new LeftShiftOp(maxShiftStage);
            }
            shiftOp.getLeftDataPort().setBus(getInputPort().getPeer());
            shiftOp.getRightDataPort().setBus(shiftConstant.getValueBus());
            addComponent(shiftConstant);
            addComponent(shiftOp);
            
//             long mask = 0xFFL << (8 * i);
            // This will fail if (strideBits * i-1) > 64
            long mask = maskValue << (strideBits * i);
            Constant maskConst = new SimpleConstant(mask, memWidth, false);
            AndOp andOp = new AndOp();
            andOp.getLeftDataPort().setBus(maskConst.getValueBus());
            andOp.getRightDataPort().setBus(shiftOp.getResultBus());
            addComponent(maskConst);
            addComponent(andOp);

            orOpMulti.makeDataPort().setBus(andOp.getResultBus());
        }
        addComponent(orOpMulti);
        getOutputBus().getPeer().setBus(orOpMulti.getResultBus());
    }
    
    public Port getInputPort ()
    {
        return (Port)getDataPorts().iterator().next();
    }
    
    public Bus getOutputBus ()
    {
        return (Bus)getDataBuses().iterator().next();
    }

    /**
     * Replace a component with another.
     * the user must take care of dependencies
     *
     * @param remove component to remove
     * @param insert component to insert
     *
     * @return true if successful, else false
     */
    public boolean replaceComponent (Component removed, Component inserted)
    {
        if (super.removeComponent(removed))
        {
            addComponent(inserted);
            return true;
        }
        return false;
    }

    public void accept (Visitor vis)
    {
        vis.visit(this);
    }

    public boolean isOpaque ()
    {
        return true;
    }
}

    
