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

package net.sf.openforge.lim.memory;

import net.sf.openforge.lim.*;

/**
 * ArrayLengthRead is a specific type of heap read that targets the
 * 'length' field of an accessed array.
 *
 * <p>Created: Wed Nov  6 13:23:52 2002
 *
 * @author imiller, last modified by $Author: imiller $
 * @version $Id: ArrayLengthRead.java 70 2005-12-01 17:43:11Z imiller $
 */
public class ArrayLengthRead extends HeapRead 
{
    private static final String _RCS_ = "$Rev: 70 $";

    /**
     * Constructs a new node which retrieves the length fied from the
     * array identified by 'className' (which is the arrays type)
     *
     * @param memRead the actual {@link MemoryRead} contained in this
     * module that is used to access the memory.
     * @param className a value of type 'String'
     * @param byteCount, the number of bytes read.
     * @param maxAddressWidth the pre-optimized number of bits in the address bus
     */
    public ArrayLengthRead (int byteCount, int maxAddressWidth)
    {
        super(byteCount, maxAddressWidth, 0, false, AddressStridePolicy.BYTE_ADDRESSING);
        throw new UnsupportedOperationException("reimplement me");
    }

    /**
     * returns true
     */
    public boolean isArrayLengthRead ()
    {
        return true;
    }
    
}// ArrayLengthRead
