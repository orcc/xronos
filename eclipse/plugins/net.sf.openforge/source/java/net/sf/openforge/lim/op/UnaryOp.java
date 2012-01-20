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
 * Base class of all operations, which require only one operand and
 * generate one result.
 *
 * Created: Thu Mar 08 16:39:34 2002
 *
 * @author  Conor Wu
 * @version $Id: UnaryOp.java 2 2005-06-09 20:00:48Z imiller $
 */
public abstract class UnaryOp extends Operation
{
    private static final String _RCS_ = "$Rev: 2 $";

    private Bus result_bus;
    
    /**
     * Constructs a unary opeation.
     * By default, the done Bus of a UnaryOp is set to unused.
     */
    public UnaryOp ()
    {
        super(1);
        Exit exit = makeExit(1);
        result_bus = (Bus)exit.getDataBuses().iterator().next();
    }
    
    /**
     * Gets the single data input Port.
     */
    public Port getDataPort ()
    {
        return (Port)getDataPorts().get(0);
    }

    /**
     * Gets the single data bus available on a UnaryOp, which
     * is the result bus.
     */
    public Bus getResultBus()
    {
        return result_bus;
    }

    /**
     * Returns true if this UnaryOp returns a floating point value.
     */
    public boolean isFloat ()
    {
        return getResultBus().isFloat();
    }
    
    /**
     * Calls the super, then removes any reference to the given bus in
     * this class.
     */
    public boolean removeDataBus (Bus bus)
    {
        if (super.removeDataBus(bus))
        {
            if (bus == result_bus)
                this.result_bus = null;
            return true;
        }
        return false;
    }
    
    /**
     * Clones this UnaryOp and correctly set's the 'resultBus'
     *
     * @return a UnaryOp clone of this operations.
     * @exception CloneNotSupportedException if an error occurs
     */
    public Object clone () throws CloneNotSupportedException
    {
        UnaryOp clone = (UnaryOp)super.clone();
        clone.result_bus = (Bus)clone.getExit(Exit.DONE).getDataBuses().iterator().next();
        return clone;
    }

}
