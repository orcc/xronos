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

import net.sf.openforge.lim.op.OrOp;
import net.sf.openforge.report.*;


/**
 * An Or accepts multiple 1-bit signals, or'ing them together to generate a 1-bit result.
 * The go port and done bus of an Or are unused.
 *
 * Created:   April 25, 2002
 *
 * @author    <a href="mailto:andreas.kollegger@xilinx.com">Andreas Kollegger</a>
 * @version   $Id: Or.java 2 2005-06-09 20:00:48Z imiller $
 */
public class Or extends Primitive
{
    private static final String rcs_id = "RCS_REVISION: $Rev: 2 $";

    /**
     * Constructor for the Or object
     *
     * @param goCount  The number of data inputs to be or'd
     */
    public Or(int goCount)
    {
        super(goCount);
        assert (goCount > 1) : "A one-input Or? What for?";
    }

    public void accept(Visitor v)
    {
        v.visit(this);
    }

    /**
     * Gets the gate depth of this component.  This is the maximum number of gates
     * that any input signal must traverse before reaching an {@link Exit}.
     *
     * @return a non-negative integer
     */
    public int getGateDepth ()
    {
        return log2(getMaxCareBits());
    }

    /**
     * Gets the FPGA hardware resource usage of this component.
     *
     * @return a FPGAResource objec
     */
    public FPGAResource getHardwareResourceUsage ()
    {
        int terms = getDataPorts().size();
        int groupedCount = 0;
        
        while (terms > 1)
        {
            groupedCount += terms >> 2;
            terms = (terms >> 2) + (terms % 4);
            if (terms < 4)
            {
                groupedCount += 1;
                break;
            }
        }
        
        final int maxCareBits = getMaxCareBits();
        final int lutCount = maxCareBits * groupedCount;
        
        FPGAResource hwResource = new FPGAResource();
        hwResource.addLUT(lutCount);
        
        return hwResource;
    }

    /*
     * ===================================================
     *    Begin new constant prop rules implementation.
     */

    /**
     * Pushes size, care, and constant information forward through
     * this OrOp according to these rules:
     * <pre>
     * x | * = x
     * 0 | c = c
     * * | 1 = 1
     * 0 | 0 = 0
     * Result size is input size
     * </pre>
     * @return a value of type 'boolean'
     */    
    public boolean pushValuesForward ()
    {
        boolean mod = false;
        
        assert getDataPorts().size() > 0;
        
        Iterator dpIter = getDataPorts().iterator();
        Value orValue = ((Port)dpIter.next()).getValue();
        while (dpIter.hasNext())
        {
            Value nextValue = ((Port)dpIter.next()).getValue();
            orValue = OrOp.pushValuesForwardOr(orValue, nextValue);
        }
        
        mod |= getResultBus().pushValueForward(orValue);

        return mod;
    }

    /**
     * Reverse constant prop on an Or simply propagates the
     * consumed value back to the Ports. Any constant/dc bits in the
     * output produce don't care bits on the inputs.
     *
     * @return a value of type 'boolean'
     */
    public boolean pushValuesBackward ()
    {
        boolean mod = false;
        
        Value resultBusValue = getResultBus().getValue();

        Value newValue = new Value(resultBusValue.getSize(), resultBusValue.isSigned());
        
        for (int i=0; i < resultBusValue.getSize(); i++)
        {
            if (resultBusValue.getBit(i).isConstant() || !resultBusValue.getBit(i).isCare())
            {
                newValue.setBit(i, Bit.DONT_CARE);
            }
        }

        for (Iterator iter = getDataPorts().iterator(); iter.hasNext();)
        {
            mod |= ((Port)iter.next()).pushValueBackward(newValue);
        }
        
        return mod;
    }

    /*
     *    End new constant prop rules implementation.
     * =================================================
     */
    
} // class Or

