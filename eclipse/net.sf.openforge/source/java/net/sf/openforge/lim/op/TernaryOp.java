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

import net.sf.openforge.lim.Bus;
import net.sf.openforge.lim.Exit;
import net.sf.openforge.lim.Operation;

/**
 * Base class of all operations, which require three operands and
 * generate two results for true and false case. The Java language has
 * only one ternary operation, which is the short-hand if-else
 * operation.
 * 
 * Created: Thu Mar 08 16:39:34 2002
 *
 * @author  Conor Wu
 * @version $Id: TernaryOp.java 2 2005-06-09 20:00:48Z imiller $
 */
public abstract class TernaryOp extends Operation
{
    private static final String _RCS_ = "$Rev: 2 $";

//     private Bus true_bus;

//     private Bus false_bus;
    
    /**
     * Constructs a ternary opeation.
     * By default the done Bus of a TernaryOp is set to unused.
     */
    public TernaryOp ()
    {
        super(3);
        Exit exit = makeExit(1);
        //setMainExit(exit);
//         exit.getDoneBus().setUsed(false);
//         Iterator exitBusIter = exit.getDataBuses().iterator();
//         true_bus = (Bus)exitBusIter.next();
//         false_bus = (Bus)exitBusIter.next();
    }

//     public Bus getTrueBus ()
//     {
//         return true_bus;
//     }
    
//     public Bus getFalseBus ()
//     {
//         return false_bus;
//     }

    public Bus getResultBus ()
    {
        return (Bus)getExit(Exit.DONE).getDataBuses().iterator().next();
    }
    
    /**
     * Clones this TernaryOp and correctly set's the 'true_bus' and 'false_bus'
     *
     * @return a TernaryOp clone of this operations.
     * @exception CloneNotSupportedException if an error occurs
     */
    public Object clone () throws CloneNotSupportedException
    {
        TernaryOp clone = (TernaryOp)super.clone();
        
//         Iterator exitBusIter =  clone.getExit(Exit.DONE).getDataBuses().iterator();
//         clone.true_bus = (Bus)exitBusIter.next();
//         clone.false_bus = (Bus)exitBusIter.next();
        
        return clone;
    }

}
