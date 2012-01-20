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

import java.util.*;

import net.sf.openforge.lim.*;
import net.sf.openforge.report.*;
import net.sf.openforge.util.SizedInteger;


/**
 * OrOpMulti is a multi-bit-wide logical OR which may have any number
 * of inputs.
 *
 * <p>Created: Wed Feb 12 16:40:57 2003
 *
 * @author imiller, last modified by $Author: imiller $
 * @version $Id: OrOpMulti.java 2 2005-06-09 20:00:48Z imiller $
 */
public class OrOpMulti extends OrOp
{
    private static final String _RCS_ = "$Rev: 2 $";

    public OrOpMulti ()
    {
        super();
        // Remove the 2 created ports, they'll get added back later 
        removeDataPort(getRightDataPort());
        removeDataPort(getLeftDataPort());
    }

    /**
     * Gets the left-hand side input Port.
     */
    public Port getLeftDataPort ()
    {
        if (getDataPorts().size() > 0)
            return super.getLeftDataPort();
        return null;
    }

    /**
     * Gets the right-hand side input Port.
     */
    public Port getRightDataPort ()
    {
        if (getDataPorts().size() > 1)
            return super.getRightDataPort();
        return null;
    }

    /**
     * Performes a high level numerical emulation of this component.
     *
     * @param portValues a map of owner {@link Port} to {@link SizedInteger}
     *          input value
     * @return a map of {@link Bus} to {@link SizedInteger} result value
     */
    public Map emulate (Map portValues)
    {
        final Value resultValue = getResultBus().getValue();
        SizedInteger result = SizedInteger.valueOf(0, resultValue.getSize(), resultValue.isSigned());
        for (Iterator iter = getDataPorts().iterator(); iter.hasNext();)
        {
            final SizedInteger arg = (SizedInteger)portValues.get(iter.next());
            result = result.or(arg);
        }
        return Collections.singletonMap(getResultBus(), result);
    }

    
    /**
     * Gets the FPGA hardware resource usage of this component.
     *
     * @return a FPGAResource object
     */
    public FPGAResource getHardwareResourceUsage ()
    {
        int lutCount = 0;

        // An array of whether each bit is significant or not (any
        // constant bit in any value makes a position not significant)
        boolean[] bits = new boolean[getResultBus().getValue().getSize()];
        for (int i=0; i < bits.length; i++)
        {
            bits[i] = true;
        }
        
        for (Iterator iter = getDataPorts().iterator(); iter.hasNext();)
        {
            Port port = (Port)iter.next();
            Value value = port.getValue();
            assert value.getSize() <= bits.length;
            for (int i=0; i < value.getSize(); i++)
            {
                Bit bit = value.getBit(i);
                bits[i] &= (bit.isCare() && bit.isConstant());
            }
        }

        for (int i=0; i < bits.length; i++)
        {
            if (bits[i])
                lutCount++;
        }

        FPGAResource hwResource = new FPGAResource();
        hwResource.addLUT(lutCount);
        
        return hwResource;
    }

    protected boolean isBitwisePassthrough ()
    {
        // An array of whether each bit is significant or not (any
        // constant ONE bit in any value makes a position not significant)
        boolean[] oneBits = new boolean[getResultBus().getValue().getSize()];
        int[] careBits = new int[getResultBus().getValue().getSize()];
        for (int i=0; i < careBits.length; i++)
        {
            oneBits[i] = false;
            careBits[i] = 0;
        }
        
        for (Iterator iter = getDataPorts().iterator(); iter.hasNext();)
        {
            Port port = (Port)iter.next();
            Value value = port.getValue();
            assert value.getSize() <= careBits.length;
            for (int i=0; i < value.getSize(); i++)
            {
                Bit bit = value.getBit(i);
                if (bit.isCare() && !bit.isConstant())
                    careBits[i]++;
                if (bit == Bit.ONE)
                    oneBits[i] = true;
            }
        }
        
        for (int i=0; i < careBits.length; i++)
        {
            // If a bit doesn't have a 1 and has multiple care bits
            // its not a pass through.
            if (!oneBits[i] && careBits[i] > 1)
                return false;
        }
        return true;
    }
    
}// OrOpMulti
