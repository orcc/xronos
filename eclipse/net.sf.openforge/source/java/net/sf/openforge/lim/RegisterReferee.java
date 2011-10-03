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

import net.sf.openforge.lim.memory.*;
import net.sf.openforge.lim.op.*;
import net.sf.openforge.util.naming.ID;

/**
 * RegisterReferee is a {@link Referee} which controls access to a {@link Register}.
 *
 * @version $Id: RegisterReferee.java 18 2005-08-12 20:32:51Z imiller $
 */
public class RegisterReferee extends MemoryReferee implements Cloneable
{
    private static final String rcs_id = "RCS_REVISION: $Rev: 18 $";

    /**
     * Constructs a RegisterReferee.
     *
     * @param register the Register which this RegisterReferee will manage
     * @param writers the number of write accesses to the register
     */
    public RegisterReferee (Register register, List readList, List writeList)
    {
        super(register, readList, writeList);
    }
    
    public void accept(Visitor v)
    {
        v.visit(this);
    }

    public boolean removeDataBus (Bus bus)
    {
        assert false : "remove data bus not supported on " + this;
        return false;
    }
    
    public boolean removeDataPort (Port port)
    {
        assert false : "remove data port not supported on " + this;
        return false;
    }
    
    /**
     * Returns a complete copy of this RegisterReferee including the
     * same number of 'write slots' or data/enable port pairs as this
     * registerreferee.
     *
     * @return a RegisterReferee Object.
     * @exception CloneNotSupportedException if an error occurs
     */
    public Object clone () throws CloneNotSupportedException
    {
        throw new CloneNotSupportedException();
    }

    public void connectImplementation (Reg enabledReg, List dataPorts)
    {
        assert enabledReg.getType() == Reg.REGE;

        final GlobalSlot globalSlot = getGlobalSlot();

        // Hook the reg output to the global slot read data port
        // Hook the global slot data bus to the reg input
        // Hook the global slot wen bus to the reg enable port
        // Hook the global slot done port to the global slot go bus.

        globalSlot.getReadDataPort().setBus(enabledReg.getResultBus());
        
        enabledReg.getDataPort().setBus(globalSlot.getWriteDataBus());
        Value value = enabledReg.getResultBus().getValue();
        globalSlot.getWriteDataBus().setSize(value.getSize(), value.isSigned());
        enabledReg.getEnablePort().setBus(globalSlot.getWriteEnableBus());
        globalSlot.getDonePort().setBus(globalSlot.getGoBus());

        Iterator portIter = dataPorts.iterator();
        System.out.println("There are " + getTaskSlots());
        System.out.println("There are " + dataPorts);
        Constant zero=new SimpleConstant(0, 1);
        for (Iterator iter = getTaskSlots().iterator(); iter.hasNext();)
        {
            TaskSlot slot = (TaskSlot)iter.next();
            if (slot.getDataInPort() != null)
            {
                assert portIter.hasNext() : "Too few write port pairs on register referee";
                Port enablePort = (Port)portIter.next();
                assert portIter.hasNext() : "Missing data port in pair on register referee";
                Port dataPort = (Port)portIter.next();
                
                slot.getGoWPort().setBus(enablePort.getPeer());
                slot.getDataInPort().setBus(dataPort.getPeer());
            }
            slot.getAddressPort().setBus(zero.getValueBus());
            slot.getSizePort().setBus(zero.getValueBus());
            if (slot.getGoRPort() != null)
                slot.getGoRPort().setBus(zero.getValueBus());
        }
        assert !portIter.hasNext();
    }
    
} // class RegisterReferee
