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

import net.sf.openforge.lim.op.CastOp;

/**
 * RegisterAccessBlock is a Module which contains the Register Access
 * component ({@link RegisterRead} or {@link RegisterWrite}) as well
 * as any necessary conversion logic such as casts to preserve correct
 * signedness.  This block is needed because Registers always consider
 * their stored value to be unsigned, and thus the accesses must
 * convert their data values to/from unsigned to preserve correct
 * functionality. 
 *
 *
 * <p>Created: Fri Oct 24 10:55:49 2003
 *
 * @author imiller, last modified by $Author: imiller $
 * @version $Id: RegisterAccessBlock.java 2 2005-06-09 20:00:48Z imiller $
 */
public class RegisterAccessBlock extends Block 
{
    private static final String _RCS_ = "$Rev: 2 $";

    private Access acc;
    
    protected RegisterAccessBlock (Access acc)
    {
        super(false);
        this.acc = acc;
    }

    /**
     * Returns the contained RegisterRead or RegisterWrite Access,
     * used primarily for unit testing.
     *
     * @return a value of type 'Access'
     */
    public Access getAccessComponent ()
    {
        return this.acc;
    }
    
    /**
     * A RegisterAccessBlock that contains a RegisterRead and a cast
     * to convert the value retrieved from the register to a
     * signed/unsigned value as needed.
     */
    public static class RegisterReadBlock extends RegisterAccessBlock
    {
        public RegisterReadBlock (RegisterRead acc)
        {
            super(acc);

            CastOp convert = new CastOp(((Register)acc.getResource()).getInitWidth(), acc.isSigned());
            insertComponent(acc, 0);
            insertComponent(convert, 1);
            
            setControlDependencies(false);
            
            assert convert.getEntries().size() > 0;
            Entry castEntry = (Entry)convert.getEntries().iterator().next();
            castEntry.addDependency(convert.getDataPort(), new DataDependency(acc.getResultBus()));
            
            /*
             * Connect the RegisterRead's data output to the output of this module.
             */
            Exit exit = getExit(Exit.DONE);
            exit.getDoneBus().setUsed(true);
            final OutBuf ob = (OutBuf)exit.getPeer();
            final Entry obEntry = (Entry)ob.getEntries().get(0);
            Bus resultBus = exit.makeDataBus();
            obEntry.addDependency(resultBus.getPeer(), new DataDependency(convert.getResultBus()));
        }
    }

    /**
     * A RegisterAccessBlock that contains a RegisterWrite and a cast
     * to convert the value written to the register to an
     * unsigned value as required by the target Register.
     */
    public static class RegisterWriteBlock extends RegisterAccessBlock
    {
        public RegisterWriteBlock (RegisterWrite acc)
        {
            super(acc);

            Register reg = (Register)acc.getResource();
            CastOp convert = new CastOp(reg.getInitWidth(), reg.isSigned());
            insertComponent(convert, 0);
            insertComponent(acc, 1);
            
            setControlDependencies(false);
            
            Port din = makeDataPort();
            
            assert convert.getEntries().size() > 0;
            Entry castEntry = (Entry)convert.getEntries().iterator().next();
            castEntry.addDependency(convert.getDataPort(), new DataDependency(din.getPeer()));
            
            assert acc.getEntries().size() > 0;
            Entry accEntry = (Entry)acc.getEntries().iterator().next();
            accEntry.addDependency(acc.getDataPort(), new DataDependency(convert.getResultBus()));
            
            /*
             * Turn on the done bus.
             */
            Exit exit = getExit(Exit.DONE);
            exit.getDoneBus().setUsed(true);
        }
    }
    
}// RegisterAccessBlock
