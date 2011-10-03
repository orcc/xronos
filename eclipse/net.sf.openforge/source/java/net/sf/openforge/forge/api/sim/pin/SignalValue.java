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
/* $Rev: 2 $ */
package net.sf.openforge.forge.api.sim.pin;
import java.util.*;

/**
 * SignalValue is used to describe signal, which may be Z, X, or an
 * integer value. For the test side, X equates to a don't care.
 */
public class SignalValue
{
    /**
     * A SignalValue of ONE (1)
     */
    public static final SignalValue ONE=new SignalValue(1L);

    /**
     * A SignalValue of ZERO (0)
     */
    public static final SignalValue ZERO=new SignalValue(0L);

    /**
     * A SignalValue representing Unknown (X)
     */
    public static final SignalValue X=new SignalValue(true,false,false);

    /**
     * A SignalValue representing a tri-stated value (Z)
     */
    public static final SignalValue Z=new SignalValue(false,true,false);

    private long value=-1L;
    private boolean isX=false;
    private boolean isZ=false;
    
    /**
     * Construct a SignalValue using a long as it's value
     *
     * @param value value to use
     */
    public SignalValue(long value)
    {
        isX=isZ=false;
        this.value=value;
    }

    /**
     * Construct a SignalValue using a double as it's value
     *
     * @param value a value of type 'double'
     */
    public SignalValue(double value)
    {
        isX=isZ=false;
        this.value=Double.doubleToRawLongBits(value);
    }

    /**
     * Construct a SignalValue using a float as it's value
     *
     * @param value a value of type 'float'
     */
    public SignalValue(float value)
    {
        isX=isZ=false;
        this.value=(long)Float.floatToRawIntBits(value);
    }

    private SignalValue(boolean isX,boolean isZ,boolean isDC)
    {
        this.isX=isX;
        this.isZ=isZ;
        if((isX)&&(isZ))
        {
            // probably never hit this ...
            throw new IllegalArgumentException("SignalValue can't be X, & Z at the same time!");
        }
    }

    /**
     * Is this an unknown value?
     *
     * @return true if unknown
     */
    public final boolean isX() { return isX; }

    /**
     * Is this a tri-stated value?
     *
     * @return true if tri-stated
     */
    public final boolean isZ() { return isZ; }

    /**
     * Get the long value contained by this SignalValue. Undefined if
     * X or Z
     *
     * @return value
     */
    public final long getValue()
    {
        return value;
    }

    /**
     * Test equality with another SignalValue
     *
     * @param v other SignalValue to test against
     * @return true if equal
     */
    public boolean equals(SignalValue v)
    {
        // any one a shortcut?
        if((isX())||(isZ())||(v.isX())||(v.isZ()))
        {
            // shortcut case .. all the is's must be the same
            return ((v.isX()==isX())&&(v.isZ()==isZ()));
        }
        return v.getValue()==getValue();
    }

    /**
     * Parse a String representation of a SignalValue. Legal values
     * are x, z, 0xNNNN (hex), 0bNNNN (binary), NNNN (decimal); MMM.NNN (Double),
     * MMM.NNNd (Double), MMM.NNNf (Float). Case is irrelevent.
     *
     * @param s a value of type 'String'
     * @return a value of type 'SignalValue[]'
     */
    public static SignalValue[] parse(String s)
    {
        ArrayList al=new ArrayList(11);
        StringTokenizer st = new StringTokenizer(s);
        while (st.hasMoreTokens())
        {
            String tok=(String)(st.nextToken());
            tok=tok.toLowerCase();
            if(tok.equals("x"))
            {
                al.add(SignalValue.X);
            }
            else if(tok.equals("z"))
            {
                al.add(SignalValue.X);
            }
            else if(tok.indexOf('.')>=0) // does it have a decimal point?
            {
                if(tok.charAt(tok.length()-1)=='f') // a float?
                {
                    al.add(new SignalValue(Float.parseFloat(tok)));
                }
                else // a double then
                {
                    if(tok.charAt(tok.length()-1)=='d') // explicit?
                    {
                        tok=tok.substring(0,tok.length()-1); // remove it...
                    }
                    al.add(new SignalValue(Double.parseDouble(tok)));
                    
                }
            }
            else
            {
                int radix=10;
                if(tok.startsWith("0x"))
                {
                    radix=16;
                    tok=tok.substring(2);
                }
                else if(tok.startsWith("0b"))
                {
                    radix=2;
                    tok=tok.substring(2);
                }
                al.add(new SignalValue(Long.parseLong(tok,radix)));
            }
        }

        SignalValue[] svArray=new SignalValue[al.size()];
        al.toArray(svArray);
        return svArray;        
    }
    
    public String toString()
    {
        if(isX())
            return "x";
        if(isZ())
            return "Z";
        return value+"";
    }
}


