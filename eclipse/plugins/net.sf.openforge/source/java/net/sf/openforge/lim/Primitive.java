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


/**
 * A Primitive is a low level component that performs a simple
 * logic function.  It has a single Exit, and the clock, reset,
 * go, and done Buses are unused.
 *
 * @author  Stephen Edwards
 * @version $Id: Primitive.java 88 2006-01-11 22:39:52Z imiller $
 */
public abstract class Primitive extends Component
{
    private static final String _RCS_ = "$Rev: 88 $";

    private Bus resultBus = null;
    
    /**
     * Constructs a Primitive.
     */
    public Primitive (int dataCount, boolean createResultBus)
    {
        super(dataCount);

        Exit exit = super.makeExit(0);
        exit = getExit(Exit.DONE);
        if (createResultBus)
        {
            resultBus = exit.makeDataBus();
        }
    }

    public Primitive(int dataCount)
    {
        this(dataCount, true);
    }

    public Exit makeExit (int dataCount)
    {
        throw new UnsupportedOperationException("Primitive has one Exit");
    }

    /**
     * Gets the Bus (attached to the DONE Exit) which is the result of
     * this primitive operation.
     */
    public Bus getResultBus()
    {
        return this.resultBus;
    }
    

    /**
     * Calls the super, then removes any reference to the given bus in
     * this class.
     */
    public boolean removeDataBus (Bus bus)
    {
        if (super.removeDataBus(bus))
        {
            if (bus == this.resultBus)
                this.resultBus = null;
            return true;
        }
        return false;
    }
    
    public boolean hasWait ()
    {
        return false;
    }

    /*
     * ===================================================
     *    Begin new constant prop rules implementation.
     */
    
    /**
     * Asserts false until rule is supported.
     */
    public boolean pushValuesForward ()
    {
        assert false : "new pushValuesForward propagation of constants through " + this.getClass() + " not yet supported";
        return false;
    }
    
    /**
     * Asserts false until rule is supported.
     */
    public boolean pushValuesBackward ()
    {
        assert false : "new pushValuesBackward propagation of constants through " + this.getClass() + " not yet supported";
        return false;
    }
    
    /*
     *    End new constant prop rules implementation.
     * =================================================
     */
 
    /**
     * Clones this primitive and correctly resets the result bus
     *
     * @return a Primitive Object.
     * @exception CloneNotSupportedException if an error occurs
     */
    public Object clone () throws CloneNotSupportedException
    {
        Primitive clone = (Primitive)super.clone();
        if (this.resultBus != null)
        {
            clone.resultBus = (Bus)clone.getExit(Exit.DONE).getDataBuses().get(0);
        }
        return clone;
    }

    /**
     * Gets the maximum number of significant inputs at any bit position.  This
     * can be used by bitwise operators to calculate their gate depth.
     *
     * @return the maximum number of significant inputs at any bit position
     */
    protected int getMaxCareBits ()
    {
        int maxCareBits = 0;
        final int maxLength = maxPortSize();
        for (int bitPosition = 0; bitPosition < maxLength; bitPosition++)
        {
            maxCareBits = Math.max(maxCareBits, getCareBitsAt(bitPosition));
        }
        return maxCareBits;
    }

    /**
     * Gets the number of significant input bits at a given position.
     *
     * @param position an input bit position
     * @return the number of inputs providing a care bit at that position
     */
    protected int getCareBitsAt (int position)
    {
        int careBits = 0;
        for (Iterator iter = getDataPorts().iterator(); iter.hasNext();)
        {
            final Port port = (Port)iter.next();
            final Value value = port.getValue();
            //if ((position < value.size()) && (value.getBit(position) == Bit.CARE))
            if (position < value.getSize())
            {
                final Bit bit = value.getBit(position);
                if (bit.isCare() && !bit.isConstant())
                {
                    careBits++;
                }
            }
        }
        return careBits;
    }
    
    /**
     * Gets the maximum size of any input data port.
     *
     * @return maximum {@link Value} size at any data port
     */
    protected int maxPortSize ()
    {
        int size = 0;
        for (Iterator iter = getDataPorts().iterator(); iter.hasNext();)
        {
            final Port port = (Port)iter.next();
            size = Math.max(size, port.getValue().getSize());
        }
        return size;
    }

}
