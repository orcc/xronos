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

package net.sf.openforge.util;

/**
 * HexBits.java
 *
 *
 * Created: Thu Apr 11 09:05:34 2002
 *
 * @author imiller
 * @version $Id: HexBits.java 2 2005-06-09 20:00:48Z imiller $
 */
public class HexBits 
{
    public HexBits ()
    {
    }

    /**
     * A catch all utility method for converting a generic Number into
     * a collection of binary bits needed to represent that number.
     * For all integer type numbers (Byte, Short, Integer, Long) this
     * is a simple call to 'longValue', for floating point
     * representations we use the 'toRawBits' methods in Float and
     * Double to get a hex representation of the value in floating
     * point format.
     *
     * @param number a value of type 'Number'
     * @return a value of type 'long'
     */
    public static long getBits(Number number)
    {
        long retValue = 0;
        if (number instanceof Float)
        {
            int ibits = Float.floatToRawIntBits(((Float)number).floatValue());
            retValue = ((long)ibits) & 0x00000000FFFFFFFFL;
        }
        else if (number instanceof Double)
        {
            retValue = Double.doubleToRawLongBits(((Double)number).doubleValue());
        }
        else
        {
            retValue = number.longValue();
        }
        return retValue;
    }
    
    /**
     * @return the number of bits needed to represent the input
     */
    public static int getSize(long z)
    {
        int size;
        
        if (z >= 0)
        {
            size=1;
            z=Math.abs(z);
            for (; size < 65; size++)
            {
                if (z >>> size == 0)
                {
                    break;
                }
            }

            // Add one bit on top for a leading zero.
            // XXX Everything is signed now.
            if (z != 0)
                size++;
            return size;
        }
        else // z is negative, size is the  position of the highest 0 + 1
        {
            size=63;
            for (; size > -1; size--)
            {
                long mask=1L<<((long) size);
                
                if ( (mask & z) == 0)
                {
                    size++;//point to the last 1 preceding the top zero
                    break;
                }
            }
            
            size++;//go from 0 base to 1 base

            if (size > 64)
            {
                size=64;
            }

            if (size < 1) //needed when the loop terminates normally, ie z=-1
            {
                size = 1;
            }
            return size;
        }
    }


    /** tests for getSize */
    public static void test(long x)
    {
        System.err.println("for input: "+x+"(0x"+Long.toHexString(x)+") size is: "+getSize(x));
    }    
    public static void main(String[] args)
    {
        test(0);
        test(1);
        test(-1);
        test(-2);
        test((int) -2);
        test(Long.MIN_VALUE);
        test(Long.MIN_VALUE+1);
        test(Long.MAX_VALUE);
        test(Long.MAX_VALUE-1);
        test(32);
        test(-32);
        test(256);
        test(255);
        test(-256);
        test(-255);
        test(0x10);
        test(0x100);
        test(0x1000);
        test(0x100000);
        test(0x1000000);
        test(0x10000000);
        test(0x80000000);
        test(0x80000000L);
        test(0x100000000L);
        test(0x1000000000L);
        test(0x10000000000L);
        test(0x100000000000L);
        test(0x1000000000000L);
        test(0x10000000000000L);
        test(0x100000000000000L);
        test(0x1000000000000000L);
        test(0x2000000000000000L);
        test(0x4000000000000000L);
        test(0x8000000000000000L);
        test(0xc000000000000000L);
        test(0xd000000000000000L);
        test(0xe000000000000000L);
        test(0xf000000000000000L);
        test(0xff00000000000000L);
        test(0xf700000000000000L);
        test(0xfc00000000000000L);
        test(Long.MIN_VALUE+1);
        test(Long.MIN_VALUE);

    }
        
}// HexBits
