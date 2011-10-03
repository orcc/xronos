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
 * A SharedProcedureCall invokes a {@link SharedProcedure}.
 *
 * @author  Stephen Edwards
 * @version $Id: SharedProcedureCall.java 88 2006-01-11 22:39:52Z imiller $
 */
public class SharedProcedureCall extends Access
{
    private static final String rcs_id = "RCS_REVISION: $Rev: 88 $";

    /** Map of call's Port to referent's body Port */
    private Map portMap;

    /** Map of call's Exit to referent's body Exit */
    private Map exitMap;

    /** Map of call's Bus to referent's body Bus */
    private Map busMap;

    /**
     * Constructs a new SharedProcedureCall.
     */
    SharedProcedureCall (SharedProcedure procedure)
    {
        super(procedure, procedure.getProcedure().getBody().getDataPorts().size(), false);
        Component body = procedure.getProcedure().getBody();

        generateMaps(body);        
    }

    public boolean isSequencingPoint ()
    {
        throw new UnsupportedOperationException("Shared procedure unsupported.  Call to unsupported method");
    }
    
    /**
     * Calls the super then removes the port from the port mapping.
     */
    public boolean removeDataPort (Port port)
    {
        boolean ret = super.removeDataPort(port);
        portMap.remove(port);
        return ret;
    }

    /**
     * Calls the super then removes the bus from the bus mapping.
     */
    public boolean removeDataBus (Bus bus)
    {
        boolean ret = super.removeDataBus(bus);
        busMap.remove(bus);
        return ret;
    }
    
    private void generateMaps(Component body)
    {
        /*
         * Map ports to procedure body's ports.
         */
        portMap = new HashMap(body.getPorts().size());
        portMap.put(getClockPort(), body.getClockPort());
        portMap.put(getResetPort(), body.getResetPort());
        portMap.put(getGoPort(), body.getGoPort());
        for (Iterator iter = getDataPorts().iterator(),
             bodyIter = body.getDataPorts().iterator();
             iter.hasNext();)
        {
            portMap.put(iter.next(), bodyIter.next());
        }
        
        /*
         * Map exits to procedure body's exits.
         */
        exitMap = new HashMap(body.getExits().size());
        busMap = new HashMap(body.getBuses().size());
        for (Iterator exitIter = body.getExits().iterator();
             exitIter.hasNext();)
        {
            Exit bodyExit = (Exit)exitIter.next();
            Exit exit = getExit(Exit.DONE);
            if (exit == null)
                exit = makeExit(bodyExit.getDataBuses().size());
            exitMap.put(exit, bodyExit);

            /*
             * Map buses to procedure body's buses.
             */
            for (Iterator bodyIter = bodyExit.getDataBuses().iterator(),
                 iter = exit.getDataBuses().iterator();
                 bodyIter.hasNext();)
            {
                busMap.put(iter.next(), bodyIter.next());
            }

//             if (bodyExit.isMain())
//             {
//                 setMainExit(exit);
//             }
        }
    }

    /**
     * Resets the target {@link Referent} and regenerates the port,
     * exit, and bus correlation maps.
     *
     * @param ref the new target Referent
     */
    public void setReferent(Referent ref)
    {
        super.setRef(ref);
        assert ref instanceof Procedure : "Referent of shared procedure call is NOT a procedure!";
        generateMaps(((Procedure)ref).getBody());
    }
    

    /**
     * Gets the SharedProcedure invoked by this Call.
     */
    public SharedProcedure getSharedProcedure ()
    {
        return (SharedProcedure)getResource();
    }

    /**
     * Gets the procedure body port that corresponds to a given
     * call port.
     *
     * @param port a port of this call
     * @return the corresponding procedure's port
     */
    public Port getProcedurePort (Port port)
    {
        if (port.getOwner() != this)
        {
            throw new IllegalArgumentException("unknown port");
        }
        return (Port)portMap.get(port);
    }

    /**
     * Gets the procedure body exit that corresponds to a given
     * call exit.
     *
     * @param exit an exit of this call
     * @return the corresponding procedure's exit
     */
    public Exit getProcedureExit (Exit exit)
    {
        if (exit.getOwner() != this)
        {
            throw new IllegalArgumentException("unknown exit");
        }
        return (Exit)exitMap.get(exit);
    }

    /**
     * Gets the procedure body bus that corresponds to a given
     * call bus.
     *
     * @param bus a bus of this call
     * @return the corresponding procedure's bus
     */
    public Bus getProcedureBus (Bus bus)
    {
        if (bus.getOwner().getOwner() != this)
        {
            throw new IllegalArgumentException("unknown bus");
        }
        return (Bus)busMap.get(bus);
    }

    /**
     * Gets the end-to-end Latency of the referent.
     */
    public Latency getLatency ()
    {
        /*
         * tbd.
         */
        return Latency.ZERO;
    }

    /**
     * Gets the pipelined input Latency of the referent.
     */
    public Latency getPipelineLatency ()
    {
        /*
         * tbd.
         */
        return Latency.ONE;
    }

    /**
     * Creates a copy of this SharedProcedureCall which points to the
     * <b>same</b> procedure as this node.
     *
     * @return a SharedProcedureCall
     * @exception CloneNotSupportedException if an error occurs
     */
    public Object clone () throws CloneNotSupportedException
    {
        SharedProcedureCall clone = (SharedProcedureCall)super.clone();

        clone.portMap = new HashMap();
        Iterator origPortIter = this.getPorts().iterator();
        Iterator clonePortIter = clone.getPorts().iterator();
        while(origPortIter.hasNext())
        {
            // Map the clone's ports to the _same_ procedure ports as
            // the original
            Object bodyPort = this.portMap.get(origPortIter.next());
            clone.portMap.put(clonePortIter.next(), bodyPort);
        }

        clone.exitMap = new HashMap();
        clone.busMap = new HashMap();
        Iterator origExitIter = this.getExits().iterator();
        Iterator cloneExitIter = clone.getExits().iterator();
        while (origExitIter.hasNext())
        {
            Exit origExit = (Exit)origExitIter.next();
            Exit cloneExit = (Exit)cloneExitIter.next();
            Object bodyExit = this.exitMap.get(origExit);
            clone.exitMap.put(cloneExit, bodyExit);

            Iterator origBusIter = origExit.getDataBuses().iterator();
            Iterator cloneBusIter = cloneExit.getDataBuses().iterator();
            while(origBusIter.hasNext())
            {
                Object bodyBus = this.busMap.get(origBusIter.next());
                clone.busMap.put(cloneBusIter.next(), bodyBus);
            }
        }

        return clone;
    }

    /*
     * ===================================================
     *    Begin new constant prop rules implementation.
     */

    /**
     * Propagate the Procedures data bus {@link Value} to the
     * corresponding Bus on this Call.
     */
    public boolean pushValuesForward ()
    {
        // All we need to do here is update the output bus(es) with
        // the outputs from the Procedure.
        boolean mod = false;

        for (Iterator exitIter = getExits().iterator(); exitIter.hasNext();)
        {
            Exit exit = (Exit)exitIter.next();
            for (Iterator busIter = exit.getDataBuses().iterator(); busIter.hasNext();)
            {
                Bus callBus = (Bus)busIter.next();
                Bus procBus = getProcedureBus(callBus);
                Value procValue = procBus.getValue();
                mod |= callBus.pushValueForward(procValue);
            }
        }
        
        return mod;
    }
    
    /**
     * Back propagate procedure Port {@link Value Values} to the
     * corresponding Port on the Call.
     */
    public boolean pushValuesBackward ()
    {
        // All we need to do here is back propagate the procedure Port
        // values to the Call's ports.
        boolean mod = false;

        for (Iterator iter = getDataPorts().iterator(); iter.hasNext();)
        {
            Port callPort = (Port)iter.next();
            Port procPort = getProcedurePort(callPort);
            mod |= callPort.pushValueBackward(procPort.getValue());
        }
        
        return mod;
    }

    /*
     *    End new constant prop rules implementation.
     * =================================================
     */
    
}
