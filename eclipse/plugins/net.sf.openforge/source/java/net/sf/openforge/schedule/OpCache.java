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

package net.sf.openforge.schedule;

import java.util.*;

import net.sf.openforge.lim.*;
import net.sf.openforge.optimize.constant.TwoPassPartialConstant;

/**
 * OpCache is a simple and convenient way to keep track of structures
 * that have already been created in the LIM and reuse them when
 * applicable.  Any scheduling artifacts (scoreboards, registers, etc)
 * created during scheduling should use the cache to avoid duplicate
 * nodes.
 *
 *
 * Created: Tue May 14 10:23:00 2002
 *
 * @author imiller
 * @version $Id: OpCache.java 280 2006-08-11 17:00:32Z imiller $
 */
public class OpCache 
{
    private static final String _RCS_ = "$Rev: 280 $";

    private Map eregMap = new HashMap();
    private Map scoreMap = new HashMap();
    private Map latchMap = new HashMap();
    private Map regMap = new HashMap();
    private Map orMap=new HashMap();
    private Map andMap=new HashMap();
    private Map muxMap=new HashMap();
    

    public OpCache ()
    {   
    }

    /**
     * Returns an enabled (but NOT reset) registered version of the 
     * specified bus
     *
     * @param dataBus a <code>Bus</code> value
     * @param enableBus a <code>Bus</code> value
     * @return a <code>Reg</code> value
     */
    public Reg getEnableReg (Bus dataBus, Bus enableBus)
    {
        Reg reg = (Reg)eregMap.get(dataBus);
        if (reg == null)
        {
            reg = Reg.getConfigurableReg(Reg.REGE, "syncEnable");
            reg.getDataPort().setBus(dataBus);
            reg.getEnablePort().setBus(enableBus);
            TwoPassPartialConstant.propagateQuiet(reg);
            eregMap.put(dataBus, reg);
        }
        assert reg.getEnablePort().getBus() == enableBus;
        return reg;
    }

    public Latch getLatch (Bus dataBus, Bus enableBus)
    {
        Latch latch = (Latch)latchMap.get(dataBus);
        if (latch == null)
        {
            latch = new Latch();
            latch.getDataPort().setBus(dataBus);
            latch.getEnablePort().setBus(enableBus);
            TwoPassPartialConstant.propagateQuiet(latch);
            latchMap.put(dataBus, latch);
        }
        assert latch.getEnablePort().getBus() == enableBus;
        return latch;
    }

    Scoreboard getScoreboard (Collection controlBuses)
    {
        final Set uniqueControlBuses = new LinkedHashSet(controlBuses);
        Scoreboard scoreboard = (Scoreboard)scoreMap.get(uniqueControlBuses);
        if (scoreboard == null)
        {
            scoreboard = new Scoreboard(uniqueControlBuses);
            TwoPassPartialConstant.propagateQuiet(scoreboard);
            scoreMap.put(uniqueControlBuses, scoreboard);
        }
        return scoreboard;
    }

    /**
     * Stallboards are never cached because their stall inputs are
     * determined post-scheduling.
     *
     * @param controlBuses a Collection of Bus objects to be
     * stallboarded. 
     * @return a non-null, unique Stallboard
     */
    Stallboard getStallboard (Collection controlBuses)
    {
        final Set uniqueControlBuses = new LinkedHashSet(controlBuses);
        Stallboard stallboard = new Stallboard(uniqueControlBuses);
        TwoPassPartialConstant.propagateQuiet(stallboard);
        return stallboard;
    }

    Reg getReg (Bus inputBus)
    {
        return getReg(inputBus, false);
    }
    
    Reg getReg (Bus inputBus, boolean resetable)
    {
        // NOTE: This assumes that the type of register will always be
        // the same for a given input bus
        Reg reg = (Reg)regMap.get(inputBus);
        if (reg == null)
        {
            if (resetable)
            {
                reg = Reg.getConfigurableReg(Reg.REGR, null);
                // We have to pick up the reset to the internal reset
                // port.  So grab it from the bus owners (owner) module
                reg.getInternalResetPort().setBus(inputBus.getOwner().getOwner().getOwner().getResetPort().getPeer());
            }
            else
            {
                reg = Reg.getConfigurableReg(Reg.REG, null);
            }
            reg.getDataPort().setBus(inputBus);
            reg.propagateValuesForward();
            regMap.put(inputBus, reg);
        }
        // Ensure consistency
        if ((reg.getType() & Reg.RESET) != 0)
            assert resetable : "Changed type of register for input bus";
        else
            assert !resetable : "Changed type of register for input bus";
        
        return reg;
    }

    /**
     * returns an or with the set of buses as inputs
     * @param buses collection of buses which will be connected to the input of the Or
     * @return the Or, with buses connected.  
     * NOTE there will be no dependencies created or connected
     */
    public Or getOr (Collection buses)
    {
        final Set uniqueBuses = new LinkedHashSet(buses);
        Or or = (Or) orMap.get(uniqueBuses);
        if (or == null)
        {
            or=new Or(uniqueBuses.size());
            int i=0;
            for (Iterator iter=uniqueBuses.iterator(); iter.hasNext();)
            {
                Port port=(Port) or.getDataPorts().get(i);
                port.setBus((Bus) iter.next());
                i++;
            }
            or.propagateValuesForward();
            orMap.put(uniqueBuses, or);
        }
        return or;
    }

    /**
     * Returns an And with the set of buses as inputs
     * @param buses collection of buses which will be connected to the input of the And
     * @return the And, with its Ports connected to the given Buses
     */
    public And getAnd (Collection buses)
    {
        final Set uniqueBuses = new LinkedHashSet(buses);
        And and = (And)andMap.get(uniqueBuses);
        if (and == null)
        {
            
            //and = new And(buses.size());
            and = new And(uniqueBuses.size());
            int i=0;
            for (Iterator iter=uniqueBuses.iterator(); iter.hasNext();)
            {
                Port port=(Port) and.getDataPorts().get(i);
                port.setBus((Bus) iter.next());
                i++;
            }
            and.propagateValuesForward();
            andMap.put(uniqueBuses, and);
        }
        return and;
    }

    /**
     * returns a Mux with the set of buses as inputs
     * @param goBuses list of go buses which will be connected to the go inputs of the Mux
     * @param dataBuses list of data buses which will be connected to the data inputs of the Mux
     * @return the Mux, with buses connected.  
     * NOTE there will be no dependencies created or connected
     */
    public Mux getMux (List goBuses, List dataBuses)
    {
        if (goBuses.size() != dataBuses.size())
        {
            throw new IllegalArgumentException("mismatched number of go and data buses");
        }
        
        final Set uniqueBuses = new LinkedHashSet(goBuses);
        uniqueBuses.add(dataBuses);
        Mux mux = (Mux) muxMap.get(uniqueBuses);
        if (mux == null)
        {
            mux=new Mux(goBuses.size());
            List goPorts=mux.getGoPorts();
            
            for (int i=0; i<goPorts.size(); i++)
            {
                Port goPort=(Port) goPorts.get(i);
                Bus bus=(Bus) goBuses.get(i);
                goPort.setBus(bus);
                
                Port dataPort=mux.getDataPort(goPort);
                bus=(Bus) dataBuses.get(i);
                dataPort.setBus(bus);
            }
            mux.propagateValuesForward();
            muxMap.put(uniqueBuses, mux);
        }
        return mux;
    }


}
