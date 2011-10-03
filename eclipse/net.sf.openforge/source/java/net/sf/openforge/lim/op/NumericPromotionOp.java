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

import net.sf.openforge.app.EngineThread;
import net.sf.openforge.lim.*;
import net.sf.openforge.util.SizedInteger;


/**
 * A numeric promotion (sign-extending res-size) operation of a value.
 *
 * Created: Thu Mar 08 16:39:34 2002
 *
 * @author  Conor Wu
 * @version $Id: NumericPromotionOp.java 2 2005-06-09 20:00:48Z imiller $
 */
public class NumericPromotionOp extends UnaryOp implements Emulatable
{
    private static final String _RCS_ = "$Rev: 2 $";
    
    private int promoteSize;
    private boolean promoteSigned;
    
    /**
     * Constructs a numeric promotion operation.
     *
     * @param size, the number of bits of the output (the promoted size).
     * @param signed a boolean, true if the promoted value (output) is
     * a signed value.
     */
    public NumericPromotionOp (int size, boolean signed)
    {
        super();
        promoteSize=size;
        promoteSigned=signed;
    }

    /**
     * Accept method for the Visitor interface
     */ 
    public void accept (Visitor visitor)
    {
        visitor.visit(this);
    }

    /**
     * Used for cloning
     *
     * @param size a value of type 'int'
     */
    private void setSize(int size)
    {
        promoteSize=size;
    }

    /**
     * Used for cloning
     *
     * @param isSigned a value of type 'boolean'
     */
    private void setSigned(boolean isSigned)
    {
        promoteSigned=isSigned;
    }
    
    /**
     * Returns the number of bits in the promoted type.
     *
     * @return an int, the number of bits of the type being promoted
     * to.
     */
    public int getPromoteSize()
    {
        return promoteSize;
    }

    /**
     * Returns true if the type being promoted to is signed.
     */
    public boolean isPromoteSigned()
    {
        return promoteSigned;
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
        final SizedInteger inVal = (SizedInteger)portValues.get(getDataPort());
        final Value resultValue = getResultBus().getValue();
        final SizedInteger outVal = SizedInteger.valueOf(inVal.numberValue(),
            resultValue.getSize(), resultValue.isSigned());
        return Collections.singletonMap(getResultBus(), outVal);
    }

    /*
     * ===================================================
     *    Begin new constant prop rules implementation.
     */
    
    /**
     * The value on the data input is propagated to the output and
     * either sign extended or truncated to the result size.
     */
    public boolean pushValuesForward ()
    {
        boolean mod = false;
        
        Value newValue = new Value(getPromoteSize(), isPromoteSigned());

        for (int i = 0; i < newValue.getSize(); i++)
        {
            if (i < getDataPort().getSize())
            {
                Bit incoming = getDataPort().getValue().getBit(i);
                newValue.setBit(i, incoming);
            }
            else
            {
                if (isPromoteSigned())
                {
                    Bit signBit = getDataPort().getValue().getBit(getDataPort().getSize()-1);
                    newValue.setBit(i, signBit);
                }
                else
                {
                    newValue.setBit(i, Bit.ZERO);
                }
            }
        }
        
        mod |= getResultBus().pushValueForward(newValue);
        
        return mod;
    }
    
    /**
     * The value on the result bus is propagated to the input port,
     * truncated to the input size if needed.
     */
    public boolean pushValuesBackward ()
    {
        boolean mod = false;
        
        Value newPushBackValue = new Value(Math.min(getResultBus().getSize(), getDataPort().getSize()), getDataPort().getValue().isSigned());
        
        for (int i = 0; i < newPushBackValue.getSize(); i++)
        {
            newPushBackValue.setBit(i, getResultBus().getValue().getBit(i));
        }
        
        mod |= getDataPort().pushValueBackward(newPushBackValue);
        
        return mod;
    }

    /*
     *    End new constant prop rules implementation.
     * =================================================
     */

    /**
     * Clones this NumericPromotionOp and correctly set's the 'resultBus'
     *
     * @return a UnaryOp clone of this operations.
     * @exception CloneNotSupportedException if an error occurs
     */
    public Object clone () throws CloneNotSupportedException
    {
        NumericPromotionOp clone = (NumericPromotionOp)super.clone();
        clone.setSize(promoteSize);
        clone.setSigned(promoteSigned);
        return clone;
    }

    /**
     * Retrieves the correct numeric promotion op for the given
     * types.  Types may be one of 'byte', 'char', 'short', 'int',
     * 'long', 'float', or 'double'.
     *
     * @param inType a value of type 'String'
     * @param outType a value of type 'String'
     * @return a value of type 'NumericPromotionOp'
     */
    public static NumericPromotionOp getNumericPromotionOp (String inType, String outType)
    {
        final String b = "byte";
        final String c = "char";
        final String s = "short";
        final String i = "int";
        final String l = "long";
        final String f = "float";
        final String d = "double";

        if      (inType.equals(b) && outType.equals(f)) return new NumericPromoteB2F();
        else if (inType.equals(c) && outType.equals(f)) return new NumericPromoteC2F();
        else if (inType.equals(s) && outType.equals(f)) return new NumericPromoteS2F();
        else if (inType.equals(i) && outType.equals(f)) return new NumericPromoteI2F();
        else if (inType.equals(l) && outType.equals(f)) return new NumericPromoteL2F();

        else if (inType.equals(b) && outType.equals(d)) return new NumericPromoteB2D();
        else if (inType.equals(c) && outType.equals(d)) return new NumericPromoteC2D();
        else if (inType.equals(s) && outType.equals(d)) return new NumericPromoteS2D();
        else if (inType.equals(i) && outType.equals(d)) return new NumericPromoteI2D();
        else if (inType.equals(l) && outType.equals(d)) return new NumericPromoteL2D();
        else if (inType.equals(f) && outType.equals(d)) return new NumericPromoteF2D();

        else
        {
            if (inType.equals(f) || inType.equals(d) ||
                outType.equals(f) || outType.equals(d))
            {
            	EngineThread.getEngine().fatalError("Unknown floating point promotion from " + inType + " to " + outType);
            }
            int size = 64;
            boolean sign = true;
            if (outType.equals(b)) size = 8;
            else if (outType.equals(c)) { size = 8; sign = false;}
            else if (outType.equals(s)) size = 16;
            else if (outType.equals(i)) size = 32;
            else if (outType.equals(l)) size = 64;
            
            return new NumericPromotionOp(size, sign);
        }
        
    }
    
    private static class FloatTypeNumericPromotionOp extends NumericPromotionOp
    {
        public FloatTypeNumericPromotionOp (int promotedBits)
        {
            super(promotedBits,true);
        }
        
        public boolean isFloat ()
        {
            return true;
        }
    }

    private static class FloatNumericPromotionOp extends FloatTypeNumericPromotionOp
    {
        public FloatNumericPromotionOp () { super (32); }
    }
    
    private static class DoubleNumericPromotionOp extends FloatTypeNumericPromotionOp
    {
        public DoubleNumericPromotionOp () { super (64); }
    }
    
    private static final class NumericPromoteB2F extends FloatNumericPromotionOp {}
    private static final class NumericPromoteC2F extends FloatNumericPromotionOp {}
    private static final class NumericPromoteS2F extends FloatNumericPromotionOp {}
    private static final class NumericPromoteI2F extends FloatNumericPromotionOp {}
    private static final class NumericPromoteL2F extends FloatNumericPromotionOp {}
    
    private static final class NumericPromoteB2D extends DoubleNumericPromotionOp {}
    private static final class NumericPromoteC2D extends DoubleNumericPromotionOp {}
    private static final class NumericPromoteS2D extends DoubleNumericPromotionOp {}
    private static final class NumericPromoteI2D extends DoubleNumericPromotionOp {}
    private static final class NumericPromoteL2D extends DoubleNumericPromotionOp {}
    
    private static final class NumericPromoteF2D extends DoubleNumericPromotionOp {}
}
