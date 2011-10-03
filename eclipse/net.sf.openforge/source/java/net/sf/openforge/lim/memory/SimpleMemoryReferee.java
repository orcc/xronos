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

import java.util.*;

import net.sf.openforge.lim.*;
import net.sf.openforge.lim.op.*;

/**
 * SimpleMemoryReferee is an implementation of the memory referee in
 * which the multiple memory accessors are simply 'merged' together by
 * muxes and or gates.  The 'done' signal is returned to any active
 * accessor.  This means that there is no arbitration of the accesses,
 * so if this referee is used the arbitration must be guaranteed in
 * some other way.
 *
 * <p>Created: Fri Aug 12 09:38:42 2005
 *
 * @author imiller, last modified by $Author: imiller $
 * @version $Id: SimpleMemoryReferee.java 565 2008-03-21 01:20:02Z imiller $
 */
public class SimpleMemoryReferee extends MemoryReferee
{
    private static final String _RCS_ = "$Rev: 565 $";

    public SimpleMemoryReferee (Arbitratable resource, List readList, List writeList)
    {
        super(resource);
        assert readList.size() == writeList.size() : "readList must match writeList size";
        
        int numTaskSlots=readList.size();

        final int memoryWidth = resource.getDataPathWidth();
        final int addressWidth = resource.getAddrPathWidth();

        getClockPort().setUsed(true);
        getResetPort().setUsed(true);

        // Create the task slots
        int readCount = 0;
        int writeCount = 0;
        for (int i=0; i<numTaskSlots; i++)
        {
            boolean doesRead=readList.get(i) != null;
            boolean doesWrite=writeList.get(i) != null;
            if (doesRead) readCount++;
            if (doesWrite) writeCount++;
            TaskSlot ts = new TaskSlot(this, memoryWidth, addressWidth, doesRead, doesWrite);
            addTaskSlot(ts);
        }

        final GlobalSlot globalSide = getGlobalSlot();
        globalSide.setSizes(memoryWidth, addressWidth, LogicalMemory.SIZE_WIDTH);

        if (numTaskSlots == 1)
        {
            // Wire straight through
            TaskSlot slot = (TaskSlot)getTaskSlots().get(0);
            slot.getDoneBus().getPeer().setBus(globalSide.getDonePort().getPeer());
            if (slot.getDataOutBus() != null)
            {
                // A write only memory port
                slot.getDataOutBus().getPeer().setBus(globalSide.getReadDataPort().getPeer());
            }
            if (slot.getDataInPort() != null)
            {
                globalSide.getWriteDataBus().getPeer().setBus(slot.getDataInPort().getPeer());
                globalSide.getWriteEnableBus().getPeer().setBus(slot.getGoWPort().getPeer());
            }
            else
            {
                Constant zero = new SimpleConstant(0, memoryWidth);
                addComponent(zero);
                globalSide.getWriteDataBus().getPeer().setBus(zero.getValueBus());
                Constant zero1 = new SimpleConstant(0, 1);
                addComponent(zero1);
                globalSide.getWriteEnableBus().getPeer().setBus(zero1.getValueBus());
            }
            
            Bus go = null;
            if (slot.getGoRPort() != null && slot.getGoWPort() != null)
            {
                Or or = new Or(2);
                addComponent(or);
                ((Port)or.getDataPorts().get(0)).setBus(slot.getGoRPort().getPeer());
                ((Port)or.getDataPorts().get(1)).setBus(slot.getGoWPort().getPeer());
                go = or.getResultBus();
            }
            else if (slot.getGoRPort() != null) { go = slot.getGoRPort().getPeer(); }
            else if (slot.getGoWPort() != null) { go = slot.getGoWPort().getPeer(); }
            globalSide.getGoBus().getPeer().setBus(go);
            
            globalSide.getAddressBus().getPeer().setBus(slot.getAddressPort().getPeer());
            globalSide.getSizeBus().getPeer().setBus(slot.getSizePort().getPeer());
        }
        else
        {
            // Now create the muxes and control logic.
            // Mux together the writeData, size and address
            // OR together the writeEnable and go
            Mux writeDataMux = new Mux(writeCount);
            Or wenOr = null;
            Bus wenResult = null; // may be set by wenOr or only found wen
            if (writeCount > 1)
            {
                wenOr = new Or(writeCount);
                wenResult = wenOr.getResultBus();
                addComponent(wenOr);
            }
            
            final Bus writeDataBus;
            if (writeCount > 0)
            {
                addComponent(writeDataMux);
                writeDataBus = writeDataMux.getResultBus();
            }
            else
            {
                Value wbVal = globalSide.getWriteDataBus().getValue();
                Constant zero = new SimpleConstant(0, wbVal.getSize(), wbVal.isSigned());
                writeDataBus = zero.getValueBus();
                addComponent(zero);
                Constant zero1 = new SimpleConstant(0, 1);
                addComponent(zero1);
                wenResult = zero1.getValueBus();
            }
            
            Mux sizeMux = new Mux(numTaskSlots);
            addComponent(sizeMux);
            Mux addrMux = new Mux(numTaskSlots);
            addComponent(addrMux);
            Or enOr = new Or(numTaskSlots);
            addComponent(enOr);
            
            Iterator writeMuxGo = writeDataMux.getGoPorts().iterator();
            Iterator sizeMuxGo = sizeMux.getGoPorts().iterator();
            Iterator addrMuxGo = addrMux.getGoPorts().iterator();
            Iterator enOrPorts = enOr.getDataPorts().iterator();
            Iterator wenOrPorts = wenOr == null ? null:wenOr.getDataPorts().iterator();
            for (Iterator iter = getTaskSlots().iterator(); iter.hasNext();)
            {
                final TaskSlot slot = (TaskSlot)iter.next();
                Bus ren = null;
                Bus wen = null;
                if (slot.getDataOutBus() != null) // is a read slot
                {
                    ren = slot.getGoRPort().getPeer();
                    slot.getDataOutBus().getPeer().setBus(globalSide.getReadDataPort().getPeer());
                }
                if (slot.getDataInPort() != null) // is a write slot
                {
                    wen = slot.getGoWPort().getPeer();
                    if (wenOr != null) { ((Port)wenOrPorts.next()).setBus(wen); }
                    if (wenResult == null) wenResult = wen;// in case of 1 write slot, pick up the wen here
                    Port goPort = (Port)writeMuxGo.next();
                    Port dataPort = writeDataMux.getDataPort(goPort);
                    goPort.setBus(slot.getGoWPort().getPeer());
                    dataPort.setBus(slot.getDataInPort().getPeer());
                }
                
                Bus go = null;
                if (ren != null && wen != null)
                {
                    Or or = new Or(2);
                    ((Port)or.getDataPorts().get(0)).setBus(ren);
                    ((Port)or.getDataPorts().get(1)).setBus(wen);
                    addComponent(or);
                    go = or.getResultBus();
                }
                else if (ren != null) { go = ren; }
                else if (wen != null) { go = wen; }
                else  { assert false : "Slot must either read or write!"; }
                ((Port)enOrPorts.next()).setBus(go);
                
                final Port addrGoPort = (Port)addrMuxGo.next();
                final Port addrPort = addrMux.getDataPort(addrGoPort);
                addrGoPort.setBus(go);
                addrPort.setBus(slot.getAddressPort().getPeer());
                
                final Port sizeGoPort = (Port)sizeMuxGo.next();
                final Port sizePort = (Port)sizeMux.getDataPort(sizeGoPort);
                sizeGoPort.setBus(go);
                sizePort.setBus(slot.getSizePort().getPeer());
                
                // Qualify the done back to each slot and fan out the read
                // data
                
                // The done is the slot go & global done
                // If combinational reads:
                //  done = ren & done | wen_reg & done;
                // else
                //  done = go_reg & done
                // Both cases can be handled by:
                //  slot done = (go | go_reg) & done
                //  go_reg <= (go | go_reg) & !done
                Bus done = globalSide.getDonePort().getPeer();
                // Needs RESET b/c it is in the control path
                Reg reg = new Reg(Reg.REGR, "done_qual");
                reg.getResultBus().setSize(1,false);
                reg.getClockPort().setBus(getClockPort().getPeer());
                reg.getResetPort().setBus(getResetPort().getPeer());
                reg.getInternalResetPort().setBus(getResetPort().getPeer());
                addComponent(reg);
                reg.getDataPort().setBus(go);
                Or goOr = new Or(2);
                addComponent(goOr);
                And regAnd = new And(2);
                addComponent(regAnd);
                And doneAnd = new And(2);
                addComponent(doneAnd);
                Not notDone = new Not();
                addComponent(notDone);
                addFeedbackPoint(reg);
                
                ((Port)goOr.getDataPorts().get(0)).setBus(go);
                ((Port)goOr.getDataPorts().get(1)).setBus(reg.getResultBus());
                notDone.getDataPort().setBus(done);
                ((Port)regAnd.getDataPorts().get(0)).setBus(goOr.getResultBus());
                ((Port)regAnd.getDataPorts().get(1)).setBus(notDone.getResultBus());
                ((Port)doneAnd.getDataPorts().get(0)).setBus(goOr.getResultBus());
                ((Port)doneAnd.getDataPorts().get(1)).setBus(done);
                
                slot.getDoneBus().getPeer().setBus(doneAnd.getResultBus());
            }

            globalSide.getWriteDataBus().getPeer().setBus(writeDataBus);
            globalSide.getAddressBus().getPeer().setBus(addrMux.getResultBus());
            globalSide.getSizeBus().getPeer().setBus(sizeMux.getResultBus());
            globalSide.getWriteEnableBus().getPeer().setBus(wenResult);
            globalSide.getGoBus().getPeer().setBus(enOr.getResultBus());
        }
        
    }
    

    
}// SimpleMemoryReferee
