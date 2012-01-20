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
 * A unary bitwise and logical operation in a form of ~.
 *
 * Created: Thu Mar 08 16:39:34 2002
 *
 * @author  Conor Wu
 * @version $Id: ComplementOp.java 2 2005-06-09 20:00:48Z imiller $
 */
public class ComplementOp extends UnaryOp implements Emulatable
{
    private static final String _RCS_ = "$Rev: 2 $";

    /**
     * Constructs a bitwise complement operation.
     *
     */
    public ComplementOp ()
    {
        super();
    }

    /**
     * Accept method for the Visitor interface
     */
    public void accept (Visitor visitor)
    {
        visitor.visit(this);
    }

    /**
     * Gets the gate depth of this component.  This is the maximum number of gates
     * that any input signal must traverse before reaching an {@link Exit}.
     *
     * @return a non-negative integer
     */
    public int getGateDepth ()
    {
        return 1;
    }

    /**
     * Gets the FPGA hardware resource usage of this component.
     *
     * @return a FPGAResource objec
     */
    public FPGAResource getHardwareResourceUsage ()
    {
        int lutCount = 0;

        Value inputValue = getDataPort().getValue();
        for (int i = 0; i < inputValue.getSize(); i++)
        {
            Bit inputBit = inputValue.getBit(i);
            if (!inputBit.isConstant() && inputBit.isCare())
            {
                lutCount ++;
            }
        }

        FPGAResource hwResource = new FPGAResource();
        hwResource.addLUT(lutCount);

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
        final SizedInteger inval = (SizedInteger)portValues.get(getDataPort());
        return Collections.singletonMap(getResultBus(), inval.not());
    }

    /*
     * ===================================================
     *    Begin new constant prop rules implementation.
     */

    /**
     * Pushes size, care, and constant information forward through
     * this ComplementOp according to these rules:
     * <pre>
     * ~x = x
     * ~c = c
     * ~1 = 0
     * ~0 = 1
     * Result size is input size
     * </pre>
     * @return a value of type 'boolean'
     */
    public boolean pushValuesForward ()
    {
        boolean mod = false;

        Value in0 = ((Port)getDataPorts().get(0)).getValue();

        int newSize = in0.getSize();
        boolean isSigned = in0.isSigned();

        Value newValue = new Value(newSize, isSigned);

        for(int i = 0; i < newSize; i++)
        {
            Bit bit = in0.getBit(i);

            if(!bit.isCare())
            {
                /*
                 * Don't-cares will be ignored going forward.
                 */
                newValue.setBit(i, Bit.DONT_CARE);
            }
            else if(bit.isConstant())
            {
                /*
                 * Push the inversion of the constant.
                 */
                newValue.setBit(i, bit.isOn() ? Bit.ZERO:Bit.ONE);
            }
            else
            {
                /*
                 * Otherwise just push a generic CARE until we're sure that
                 * there's a Value on the result Bus.
                 */
                newValue.setBit(0, Bit.CARE);
            }
        }

        // update all bits above the carry out bit to be signed
        // extended of carry out bit
        if (getResultBus().getValue() != null)
        {
            if (!in0.isConstant())
            {
                int compactedSize = Math.min(newSize, in0.getCompactedSize());
                Bit carryoutBit = getResultBus().getValue().getBit(compactedSize -1);

                for (int i = compactedSize; i < newSize; i++)
                {
                    if(newValue.getBit(i) != Bit.DONT_CARE)
                        newValue.setBit(i, carryoutBit);
                }
            }
        }

        mod |= getResultBus().pushValueForward(newValue);

        for(int i = 0; i < newSize; i++)
        {
            Bit bit = in0.getBit(i);
            if (!bit.isGlobal())
            {
                /*
                 * Set the inversion shortcut if appropriate.
                 */
                getResultBus().getValue().getBit(i).setInvertedBit(bit);
            }
        }

        return mod;
    }

    /**
     * Reverse constant prop on a ComplementOp simply propagates the
     * consumed value back to the Port. Any constant/dc bits in the output
     * produce don't care bits on the input.
     *
     * @return a value of type 'boolean'
     */
    public boolean pushValuesBackward ()
    {
        boolean mod = false;

        Value resultBusValue = getResultBus().getValue();

        Value newValue = new Value(resultBusValue.getSize(), resultBusValue.isSigned());

        for (int i = 0; i < resultBusValue.getSize(); i++)
        {
            Bit bit = resultBusValue.getBit(i);
            if (!bit.isCare())
            {
                newValue.setBit(i, Bit.DONT_CARE);
            }
        }

        mod |= ((Port)getDataPorts().get(0)).pushValueBackward(newValue);

        return mod;
    }

    /*
     *    End new constant prop rules implementation.
     * =================================================
     */
}
