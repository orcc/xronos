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
 * AddMultiOp is an 'n' input add operation.  An add of the form
 * A + B + C + D ....
 * 
 * <p>Created: Wed Nov  6 10:25:34 2002
 *
 * @author imiller, last modified by $Author: imiller $
 * @version $Id: AddMultiOp.java 2 2005-06-09 20:00:48Z imiller $
 */
public class AddMultiOp extends AddOp 
{
    private static final String _RCS_ = "$Rev: 2 $";

    public AddMultiOp ()
    {
        super();
    }

    /**
     * returns true
     */
    public boolean hasMulti ()
    {
        return getDataPorts().size() > 2;
    }

    /**
     * Gets the FPGA hardware resource usage of this component.
     *
     * @return a FPGAResource objec
     */
    public FPGAResource getHardwareResourceUsage ()
    {
        FPGAResource hwResource = new FPGAResource();

        for (int i = 1; i < getDataPorts().size(); i++)
        {
            hwResource.addResourceUsage(super.getHardwareResourceUsage());
        }
        
        return hwResource;
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
        final Iterator iter = getDataPorts().iterator();
        SizedInteger result = (SizedInteger)portValues.get(iter.next());
        while (iter.hasNext())
        {
            result = result.add((SizedInteger)portValues.get(iter.next()));
        }
        return Collections.singletonMap(getResultBus(), result);
    }

    /*
     * ===================================================
     *    Begin new constant prop rules implementation.
     */
     
    /**
     * Overrides the super to take into account all data ports
     *
     * @return true if a change was made to the result bus Value.
     */
    public boolean pushValuesForward ()
    {
        boolean mod = false;
        
        int newSize = 0;
        boolean isSigned = false;
        
        for (Iterator portIter = getDataPorts().iterator(); portIter.hasNext();)
        {
            final Port port = (Port)portIter.next();
            Value portValue = port.getValue();
            newSize = Math.max(newSize, portValue.getSize());
            isSigned &= portValue.isSigned();
        }
        
        Value newValue = new Value(newSize, isSigned);
        
        for(int i = 0; i < newSize; i++)
        {
            boolean hasCare = false;
            for (Iterator iter = getDataPorts().iterator(); iter.hasNext();)
            {
                Value portValue = ((Port)iter.next()).getValue();
                if (i < portValue.getSize())
                {
                    Bit bit = portValue.getBit(i);
                    if (bit.isCare())
                    {
                        hasCare = true;
                        break;
                    }
                }
            }
            
            if (!hasCare)
            {
                newValue.setBit(i, Bit.DONT_CARE);
            }
        }
        
        mod |= getResultBus().pushValueForward(newValue);

        return mod;
    }
    
    /*
     *    End new constant prop rules implementation.
     * =================================================
     */
}// AddMultiOp

