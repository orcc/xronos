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
 * A binary bitwise and logical operation in a form of |.
 *
 * Created: Thu Mar 08 16:39:34 2002
 *
 * @author  Conor Wu
 * @version $Id: OrOp.java 2 2005-06-09 20:00:48Z imiller $
 */
public class OrOp extends BinaryOp implements Emulatable
{
    private static final String _RCS_ = "$Rev: 2 $";

    /**
     * Constructs a bitwise or operation.
     *
     */
    public OrOp ()
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
        return isBitwisePassthrough() ? 0 : 1;
    }

    /**
     * Gets the FPGA hardware resource usage of this component.
     *
     * @return a FPGAResource object
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

            if ((leftBit != null) && (rightBit != null))
            {
                if (leftBit.isCare() && rightBit.isCare() && (!leftBit.isConstant() || !rightBit.isConstant()))
                {
                    lutCount ++;
                }
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
        final SizedInteger lval = (SizedInteger)portValues.get(getLeftDataPort());
        final SizedInteger rval = (SizedInteger)portValues.get(getRightDataPort());
        return Collections.singletonMap(getResultBus(), lval.or(rval));
    }

    /*
     * ===================================================
     *    Begin new constant prop rules implementation.
     */

    /**
     * Pushes size, care, and constant information forward through
     * this OrOp's result bus.
     *
     * @return a value of type 'boolean'
     */
    public boolean pushValuesForward ()
    {
        Value newValue;
        if (getRightDataPort() == null)
        {
            newValue = getLeftDataPort().getValue();
        }
        else
        {
            /*
             * We need to account for an arbitrary number of inputs,
             * not just two, as in OrOpMulti.
             */
            // Iterate through each port, merging the values of each
            // port as we go.  The compacted size is the minimum of
            // any ports size or the compacted size of the merged
            // value.
            final Iterator portIter = getDataPorts().iterator();
            newValue = ((Port)portIter.next()).getValue();
            int minPortSize = newValue.getSize();
            while (portIter.hasNext())
            {
                final Port nextPort = (Port)portIter.next();
                final Value nextPortValue = nextPort.getValue();
                minPortSize = Math.min(nextPortValue.getSize(), minPortSize);
                newValue = pushValuesForwardOr(newValue, nextPortValue);
            }
            final int compactedSize = Math.min(newValue.getCompactedSize(), minPortSize);
            // update all bits above the carry out bit to be signed
            // extended of carry out bit
            if (getResultBus().getValue() != null)
            {
                if (!newValue.isConstant())
                {
                    Bit carryoutBit = getResultBus().getValue().getBit(compactedSize -1);

                    for (int i = compactedSize; i < newValue.getSize(); i++)
                    {
                        if(newValue.getBit(i) != Bit.DONT_CARE)
                            newValue.setBit(i, carryoutBit);
                    }
                }
            }
        }

        return getResultBus().pushValueForward(newValue);
    }

    /**
     * Reverse constant prop on an OrOp simply propagates the
     * result bus value back to the Ports. Any constant/dc bits in the output
     * produce don't care bits on the inputs.
     *
     * @return a value of type 'boolean'
     */
    public boolean pushValuesBackward ()
    {
        boolean mod = false;

        Value resultBusValue = getResultBus().getValue();
        for (Iterator iter = getDataPorts().iterator(); iter.hasNext();)
        {
            final Port dataPort = (Port)iter.next();
            if (!dataPort.getValue().isConstant())
            {
                Value newPushBackValue = new Value(dataPort.getValue().getSize(), dataPort.getValue().isSigned());
                for (int i = 0; i < newPushBackValue.getSize(); i++)
                {
                    Bit bit = resultBusValue.getBit(i);
                    if (!bit.isCare() || bit.isConstant())
                    {
                        newPushBackValue.setBit(i, Bit.DONT_CARE);
                    }
                }
                mod |= dataPort.pushValueBackward(newPushBackValue);
            }
        }

        return mod;
    }

    /**
     * Produces a Value representing the state after ORing the two
     * given values according to these rules:
     * <pre>
     * x = Dont Care     c = care (non constant)   0 = zero   1 = one
     *    x x : x        c x : x ???    0 x : x    1 x : x
     *    x c : x ???    c c : c        0 c : c    1 c : 1
     *    x 0 : x        c 0 : c        0 0 : 0    1 0 : 1
     *    x 1 : x        c 1 : 1        0 1 : 1    1 1 : 1
     * </pre>
     *
     * @return a {@link Value} representing the And'ing of the two
     * specified input Values.
     */
    public static Value pushValuesForwardOr (Value in0, Value in1)
    {
        int newSize = Math.max(in0.getSize(), in1.getSize());
        boolean isSigned = in0.isSigned() && in1.isSigned();

        Value newValue = new Value(newSize, isSigned);

        for(int i = 0; i < newSize; i++)
        {
            Bit bit0 = in0.getBit(i);
            Bit bit1 = in1.getBit(i);

            /** don't care | don't care **/
            if(!bit0.isCare() || !bit1.isCare())
            {
                newValue.setBit(i, Bit.DONT_CARE);
            }
            else
            {
                if (bit0.isConstant() && bit1.isConstant())
                {
                    newValue.setBit(i, bit0.isOn() || bit1.isOn() ? Bit.ONE : Bit.ZERO);
                }
                else if (bit0.isConstant() && bit0.isOn())
                {
                    newValue.setBit(i, Bit.ONE);
                }
                else if (bit0.isConstant() && !bit0.isOn())
                {
                    newValue.setBit(i, bit1);
                }
                else if (bit1.isConstant() && bit1.isOn())
                {
                    newValue.setBit(i, Bit.ONE);
                }
                else if (bit1.isConstant() && !bit1.isOn())
                {
                    newValue.setBit(i, bit0);
                }
                else
                {
                    newValue.setBit(i, Bit.CARE);
                }
            }
        }

        return newValue;
    }

    /*
     *    End new constant prop rules implementation.
     * =================================================
     */
}
