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

import net.sf.openforge.lim.*;

/**
 * An operation that generates a single value.
 *
 * @author  Stephen Edwards
 * @version $Id: ValueOp.java 2 2005-06-09 20:00:48Z imiller $
 */
public abstract class ValueOp extends Operation
{
    private static final String rcs_id = "RCS_REVISION: $Rev: 2 $";

    /** The value data bus */
    private Bus valueBus;

    /**
     * Constructs a long constant.
     * By default, the ValueOp has an unused done Bus.
     * @param lval the long value of the constant
     */
    public ValueOp (int dataPortCount)
    {
        super(dataPortCount);
        Exit exit = makeExit(1);
        //setMainExit(exit);
//         exit.getDoneBus().setUsed(false);
        this.valueBus = (Bus)exit.getDataBuses().iterator().next();
    }

    /**
     * Gets the value data bus.
     */
    public Bus getValueBus ()
    {
        return valueBus;
    }

    /**
     * Returns true if this ValueOp returns a floating point value.
     */
    public boolean isFloat ()
    {
        return getValueBus().isFloat();
    }
    
    /**
     * Calls the super, then removes any reference to the given bus in
     * this class.
     */
    public boolean removeDataBus (Bus bus)
    {
        if (super.removeDataBus(bus))
        {
            if (bus == valueBus)
                this.valueBus = null;
            return true;
        }
        return false;
    }
    
    /**
     * Clones this ValueOp and correctly set's the 'valueBus'
     *
     * @return a ValueOp clone of this operations.
     * @exception CloneNotSupportedException if an error occurs
     */
    public Object clone () throws CloneNotSupportedException
    {
        ValueOp clone = (ValueOp)super.clone();
        clone.valueBus = (Bus)clone.getExit(Exit.DONE).getDataBuses().iterator().next();
        return clone;
    }

}
