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

package net.sf.openforge.lim.io;

import net.sf.openforge.lim.*;

/**
 * SimplePinWrite is a very lightweight component used to indicate a
 * data value sent to a specific {@link SimplePin}.  This node does
 * not contain any specific functionality other than to take the Bus
 * attached to its data port and wire it through to the targetted pin
 * after being qualified with a logical AND with its GO.  The logical
 * AND allows us to use a simple OR function to merge multiple writers
 * to the same pin.
 * <p>Functionally this node represents the following verilog
 * structure:
 * <code>SIDE_BUS = {N{GO}} & DATA_PORT;</code> where N is the width
 * of the data port.
 * <p>Alternatively, if there is only a single writer to the pin then
 * the node will return false for {@link #consumesGo} and represents
 * the following verilog structure:
 * <code>SIDE_BUS = DATA_PORT;</code>
 * 
 * <p>Note that pin reads and writes are always unsigned and you must
 * cast to signed if you need signed data.
 *
 * <p>Created: Fri Jan 16 04:23:34 2004
 *
 * @author imiller, last modified by $Author: imiller $
 * @version $Id: SimplePinWrite.java 425 2007-03-07 19:17:39Z imiller $
 */
public class SimplePinWrite extends Component implements Visitable, Referencer
{
    private static final String _RCS_ = "$Rev: 425 $";

    /** The pin being targetted by this write operation. */
    private SimplePin targetPin;

    /** True iff this component uses the GO signal to qualify the data
     * being sent to the pin. */
    private boolean needsGo = true;
    
    /**
     * Construct a new SimplePinWrite targetting the given, non null,
     * pin. 
     *
     * @param targetPin a {@link SimplePin}
     * @throws IllegalArgumentException if targetPin is null.
     */
    public SimplePinWrite (SimplePin targetPin)
    {
        // Create with one port (the input data port) and one bus
        // (the sideband data  bus).  Mark the sideband bus as not
        // used until it gets connected to the pin.
        super(1);
        
        if (targetPin == null)
        {
            throw new IllegalArgumentException("Cannot have an access to null pin");
        }
        
        this.targetPin = targetPin;

        // The GO port is used by this component because we must
        // qualify the data with it in order to do a 'wired or' with
        // other writes to the same pin.
        //getGoPort().setUsed(true);
        setGoNeeded(true);
        
        //makeExit(1); // Create one exit with one bus, the read value.
        makeExit(0); // Defer the sideband bus until pin connection
        // ((Bus)getExit(Exit.DONE).getDataBuses().get(0)).setUsed(false);
    }

    /**
     * Override from Component to assert that we do in fact need a
     * valid GO supplied (so that this node can logicall AND that GO
     * with the data port).
     *
     * @return true
     */
    public boolean consumesGo ()
    {
        return this.needsGo;
    }

    /**
     * Allows the use of the GO signal to be disabled in the case
     * where this is the only write to the target pin and no
     * qualification of the data is needed.
     */
    public void setGoNeeded (boolean value)
    {
        this.needsGo = value;
        this.getGoPort().setUsed(value);
    }
    
    /**
     * Returns the data port which is the port whose input value is
     * sent to the pin.
     *
     * @return a value of type 'Port'
     */
    public Port getDataPort ()
    {
        return (Port)getDataPorts().get(0);
    }

    public void accept (Visitor vis)
    {
        vis.visit(this);
    }
    
    /**
     * Returns the {@link Referenceable} {@link SimplePin} which this
     * node targets.
     */
    public Referenceable getReferenceable ()
    {
        return this.targetPin;
    }
    
    /**
     * This accessor modifies the {@link Referenceable} target state
     * so it may not execute in parallel with other accesses.
     */
    public boolean isSequencingPoint ()
    {
        return true;
    }
    
    public boolean pushValuesForward ()
    {
        // Do nothing except ensure that the sideband write bus has a
        // value as there is no push-forward to the pin.
        // It may be tempting to push the sideband port value forward
        // to the output bus.  This would be WRONG because we mask the
        // data port with the GO.  Thus this is NOT a passthrough like
        // that.
        if (!getExit(Exit.DONE).getDataBuses().isEmpty())
        {
            ((Bus)getExit(Exit.DONE).getDataBuses().get(0)).pushValueForward(new Value(this.targetPin.getWidth(), false));
        }
        return false;
    }

    public boolean pushValuesBackward ()
    {
        Exit exit = getExit(Exit.DONE);
        Bus sideband = exit.getDataBuses().isEmpty() ? null : (Bus)exit.getDataBuses().get(0);
        Value newValue;
        if (sideband != null && sideband.isConnected())
        {
            newValue = sideband.getValue();
        }
        else
        {
            newValue = new Value(this.targetPin.getWidth(), false);
        }
        
        return getDataPort().pushValueBackward(newValue);
    }
    
}// SimplePinWrite
