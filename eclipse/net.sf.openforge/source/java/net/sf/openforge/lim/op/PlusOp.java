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

/**
 * A unary bitwise promotion operation in a form of +.
 *
 * Created: Thu Mar 08 16:39:34 2002
 *
 * @author  Conor Wu
 * @version $Id: PlusOp.java 2 2005-06-09 20:00:48Z imiller $
 */
public class PlusOp extends UnaryOp implements Emulatable
{
    private static final String _RCS_ = "$Rev: 2 $";
    
    /**
     * Constructs a promotion plus operation.
     *
     */
    public PlusOp ()
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
     * Performes a high level numerical emulation of this component.
     *
     * @param portValues a map of owner {@link Port} to {@link SizedInteger}
     *          input value
     * @return a map of {@link Bus} to {@link SizedInteger} result value
     */
    public Map emulate (Map portValues)
    {
        return Collections.singletonMap(getResultBus(), portValues.get(getDataPort()));
    }

    /*
     * ===================================================
     *    Begin new constant prop rules implementation.
     */

    /**
     * Pushes size, care, and constant information forward through
     * this PlusOp according to these rules:
     *
     * Result value is all pass throughs of input value
     *
     * @return true if new information was generated on the result
     * bus.
     */
    public boolean pushValuesForward ()
    {
        boolean mod = false;
        
        Value in = getDataPort().getValue();
        
        Value newValue = new Value(32, in.isSigned());
        
        for (int i = 0; i < 32; i++)
        {
            if (i < in.getSize())
            {
                newValue.setBit(i, in.getBit(i));
            }
            else
            {
                newValue.setBit(i, in.getBit(in.getSize()-1));
            }
        }
        
        mod |= getResultBus().pushValueForward(newValue);

        return mod;
    }
    
    /**
     * Reverse constant prop on a PlusOp simply propagates the
     * consumed value back to the Port. The port has the same size
     * with the consumed value.
     *
     * @return true if new information was generated on the input port
     */
    public boolean pushValuesBackward ()
    {
        boolean mod = false;
        
        Value resultBusValue = getResultBus().getValue();
        
        Value newValue = new Value(getDataPort().getValue().getSize(), getDataPort().getValue().isSigned());
        
        for (int i = 0; i < getDataPort().getValue().getSize(); i++)
        {
            newValue.setBit(i, resultBusValue.getBit(i));
        }
        
        mod |= getDataPort().pushValueBackward(newValue);
        
        return mod;
    }
    
    /*
     *    End new constant prop rules implementation.
     * =================================================
     */
}
