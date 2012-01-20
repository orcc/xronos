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
 * BitBuffer represents a series of values with specified width
 *
 * @version $Id: BitBuffer.java 2 2005-06-09 20:00:48Z imiller $
 */
public class BitBuffer
{
    /** Revision */
    public static final String _RCS_ = "$Rev: 2 $";

    private byte[] bits = new byte[0];

    private int bitPtr = 0;

    public BitBuffer ()
    {
    }

    /**
     * Puts the specified value with desired width into this BitBuffer
     *
     * @param value 
     * @param width 
     */
    public void put (long value, int width)
    {
        final int skip = 64 - width;
        value <<= skip;

        for (int i = 0; i < width; i++)
        {
            addBit((value & 0x8000000000000000L) != 0);
            value <<= 1;
        }
    }

    /**
     * @return number of bits needed to represent this BitBuffer
     */
    public int getSize ()
    {
        return (bits.length * 8) - bitPtr;
    }

    /**
     * @return a byte array representation of this BitBuffer
     */
    public byte[] getBits ()
    {
        return bits;
    }

    /**
     * @return a String representation of this BitBuffer
     */
    public String toString ()
    {
        final StringBuffer buf = new StringBuffer();
        final int size = getSize();
        for (int i = 0; i < size; i++)
        {
            final byte byteVal = bits[i / 8];
            final int bitShift = 8 - (i % 8);
            final boolean bit = (byteVal & (1 << (bitShift - 1))) != 0;
            buf.append(bit ? "1" : "0");
        }
        return buf.toString();
    }

    /**
     * Adds a bit to this buffer
     *
     * @param bit a bit to be added
     */
    private void addBit (boolean bit)
    {
        if (bitPtr == 0)
        {
            final byte[] newBits = new byte[bits.length + 1];
            System.arraycopy(bits, 0, newBits, 0, bits.length);
            bits = newBits;
            bitPtr = 8;
        }

        bitPtr--;

        if (bit)
        {
            final int bytePtr = bits.length - 1;
            bits[bytePtr] |= (1 << bitPtr);
        }
    }
}
