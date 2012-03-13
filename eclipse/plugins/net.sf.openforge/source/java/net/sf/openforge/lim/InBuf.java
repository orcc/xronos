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

import net.sf.openforge.util.SizedInteger;

/**
 * An InBuf is used to bring a structural flow to the inside
 * of a {@link Module} from its outside.  A single InBuf is created
 * automatically for each {@link Module},
 * and adding a data {@link Port} to the {@link Module} also adds
 * a corresponding {@link Bus} to the InBuf.  The InBuf itself
 * has no {@link Port Ports} and no {@link Entry Entries}.  Logically, the
 * {@link Port Ports} of the {@link Module} are the continued internally
 * by the InBuf's {@link Bus Buses}.
 * <P>
 * The InBuf's single {@link Exit} is created with the usual done
 * {@link Bus} and at least two data {@link Bus Buses}.  The done
 * {@link Bus} is the continuation of the {@link Module Module's}
 * go {@link Port}; that is why it is also known as the "go
 * {@link Bus}" and can be retrieved with the method
 * {@link InBuf#getGoBus()}.  The {@link Module Module's} clock
 * and reset {@link Port} signals are represented by the first
 * two data {@link Bus Buses}; this is because this is the only
 * case in which a clock or a reset will exit a {@link Component}.
 * In addition to being the first two buses in the list returned
 * by {@link Exit#getDataBuses()}, these buses can also be obtained
 * with the methods {@link InBuf#getClockBus()} and {@link InBuf#getResetBus()},
 * respectively.  The remaining data buses carry regular data values;
 * these buses are the only ones returned by the method
 * {@link InBuf#getDataBuses()}.
 *
 * @version $Id: InBuf.java 2 2005-06-09 20:00:48Z imiller $
 */
public class InBuf extends Component implements Emulatable
{
    private static final String rcs_id = "RCS_REVISION: $Rev: 2 $";

    /** The estimated 'gate depth' of the default IOB's */
    public static final int IOB_DEFAULT = 10;

    /** The gate depth of this inbuf.  Always 0 except at the
     * periphery of the task to help register IOB's of a task. */
    private int gateDepth = 0;
    
    /** A bus which maps to the module's clock port */
    private Bus clockBus;

    /** A bus which maps to the module's reset port */
    private Bus resetBus;

    /** A bus which maps to the module's go port */
    private Bus goBus;

    /** A bus which maps to the module's 'this' port if any */
    private Bus thisBus;

    /** True if the containing Module consumes a go signal */
    private boolean producesDone = false;
    
    /**
     * Constructs a new InBuf.     */
    InBuf (int dataBusCount)
    {
        super(0);
        Exit exit = super.makeExit(0);
        this.clockBus = exit.makeOneBitBus();
        this.resetBus = exit.makeOneBitBus();
        this.goBus = exit.getDoneBus();
        clockBus.setUsed(false);
        resetBus.setUsed(false);
        goBus.setUsed(false);
        
        for (int i = 0; i < dataBusCount; i++)
        {
            exit.makeDataBus();
        }
    }

    /**
     * Gets the clock bus.
     *
     * @return the bus which represents the module's clock port
     */
    public Bus getClockBus ()
    {
        return clockBus;
    }

    /**
     * Gets the reset bus.
     *
     * @return the bus which represents the module's reset port
     */
    public Bus getResetBus ()
    {
        return resetBus;
    }

    /**
     * Gets the go bus.
     *
     * @return the bus which represents the module's go port
     */
    public Bus getGoBus ()
    {
        return goBus;
    }

    public Bus getThisBus ()
    {
        return this.thisBus;
    }

    public Bus makeThisBus ()
    {
        assert this.thisBus == null : "Can only make the this bus once";
        Exit exit = getExit(Exit.DONE);
        Bus bus = exit.makeDataBus();
        this.thisBus = bus;
        // Put it just after the clock and reset
        exit.moveDataBusLocation(bus, 2);
        return bus;
    }

    /**
     * Gets the data buses.
     *
     * @return a collection of Bus, each of which represents one
     *         of the module's data ports; note that this list is
     *         not the same as that returned by {@link Exit#getDataBuses()}
     *         in that it does not contain the clock or reset bus
     */
    public Collection<Bus> getDataBuses ()
    {
        List<Bus> list = new LinkedList<Bus>(getExit(Exit.DONE).getDataBuses());
        list.remove(getClockBus());
        list.remove(getResetBus());
        return list;
    }

    /**
     * Calls the super, then removes any reference to the given bus in
     * this class.
     */
    public boolean removeDataBus (Bus bus)
    {
        if (super.removeDataBus(bus))
        {
            if (bus == this.clockBus)
                this.clockBus = null;
            else if (bus == this.resetBus)
                this.resetBus = null;
            else if (bus == this.goBus)
                this.goBus = null;
            else if (bus == this.thisBus)
                this.thisBus = null;
                
            return true;
        }
        return false;
    }
    
    /**
     * Updates the physical Port-Bus connections, based on
     * current Structural Dependencies. InBufs have no Entries
     * of their own, so this is a no-op.
     */
    public void updatePhysical()
    {
    }
    
    public void accept(Visitor v)
    {
        v.visit(this);
    }
    
    public boolean hasWait ()
    {
        return false;
    }

    /**
     * Tests whether this component produces a signal on the
     * done {@link Bus} of each of its {@link Exit Exits}.
     */
    public boolean producesDone ()
    {
        return producesDone;
    }

    /**
     * Called by the containing {@link Module} to agree with its
     * value for {@link Component#consumesGo()}.
     */
    void setProducesDone (boolean producesDone)
    {
        this.producesDone = producesDone;
    }

    /**
     * Sets the gate depth for this InBuf, called by the Pipeliner to
     * set a depth on the inputs of a task to account for IOBs
     */
    public void setGateDepth (int value)
    {
        this.gateDepth = value;
    }

    /**
     * Overrides method in Component to provide the InBuf depth when
     * it exists at the boundry of a task to account for IOBs
     */
    public int getGateDepth()
    {
        return this.gateDepth;
    }

    /**
     * Performes a high level numerical emulation of this component.
     * For each {@link Port} for which a value is provided, that value is returned
     * as the value of the corresponding peer {@link Bus}.
     *
     * @param portValues a map of owner {@link Port} to {@link SizedInteger}
     *          input value
     * @return a map of {@link Bus} to {@link SizedInteger} result value
     */
    public Map emulate (Map portValues)
    {
        final Map outputValues = new HashMap();
        for (Iterator iter = getOwner().getDataPorts().iterator(); iter.hasNext();)
        {
            final Port port = (Port)iter.next();
            final SizedInteger portValue = (SizedInteger)portValues.get(port);
            if (portValue != null)
            {
                outputValues.put(port.getPeer(), portValue);
            }
        }
        return outputValues;
    }

    /*
     * ===================================================
     *    Begin new constant prop rules implementation.
     */
    
    /**
     * Propagates {@link Value Values} through this InBuf in the
     * forward direction.
     * <P>
     * Rather than using {@link Bus#pushValueForward(Value)} as
     * usual, this component propagates each {@link Bit} (or not}
     * itself, according to the following rules.
     * <P>
     * If the owner {@link Module} returns true for
     * {@link Module#isOpaque()}, then only constant {@link Bit Bits}
     * are propagated from the {@link Module Module's} {@link Port Ports}
     * to the InBuf's {@link Bus Buses}.
     * <P>
     * Otherwise, both constant and "owned" {@link Bus} {@link Bit Bits}
     * will be propagated.
     *
     * @return true if any bus values were modified
     */
    public boolean pushValuesForward ()
    {
        boolean isModified = false;
        for (Iterator iter = getBuses().iterator(); iter.hasNext();)
        {
            final Bus bus = (Bus)iter.next();
            final Value pushedValue = bus.getPeer().getValue();
            if (pushedValue != null)
            {
                /*
                 * If the Bus doesn't have a Value yet, create one.
                 */
                Value busValue = bus.getValue();
                if (busValue == null)
                {
                    bus.setSize(pushedValue.getSize(), pushedValue.isSigned());
                    busValue = bus.getValue();
                }
                
                /*
                 * Rather than depending on the behavior of
                 * Bus.pushValueForward(Value), we need to be more
                 * precise here, since an InBuf is part Module and
                 * part Component.  Compare each pair of Bits.
                 */
                for (int i = 0; i < pushedValue.getSize(); i++)
                {
                    final Bit pushedBit = pushedValue.getBit(i);
                    final Bit busBit = busValue.getBit(i);
                    if (!pushedValue.bitEquals(i, busValue, i) && busBit.isCare())
                    {
                        if (getOwner().isOpaque())
                        {
                            /*
                             * If this InBuf lives in an opaque Module, then
                             * we have to avoid pushing through external non-global
                             * Bits.  Since we don't push don't-cares, that only
                             * leaves constants as viable propagatees.
                             */
                            if (pushedBit.isConstant())
                            {
                                busValue.setBit(i, pushedBit);
                                isModified = true;
                            }
                        }
                        else if (pushedBit.isConstant() || !pushedBit.isGlobal())
                        {
                            /*
                             * Otherwise, this InBuf lives in a transparent Module, and
                             * we can push through both constant and external Bus Bits.
                             */
                            busValue.setBit(i, pushedBit);
                            isModified = true;
                        }
                    }
                }
            }
        }
        return isModified;
    }
    
    /**
     * Determines the 'consumed' value for each {@link Bus} of the
     * InBuf and pushes that information back across to it's peer.
     * Returns true if any new information was pushed across the
     * boundry.
     */
    public boolean pushValuesBackward ()
    {
        boolean isModified = false;
        for (Iterator iter = getBuses().iterator(); iter.hasNext();)
        {
            final Bus bus = (Bus)iter.next();
            final Value pushedValue = bus.getValue();
            if (pushedValue != null)
            {
                isModified |= bus.getPeer().pushValueBackward(pushedValue);
            }
        }
        return isModified;
    }

    /*
     *    End new constant prop rules implementation.
     * =================================================
     */

    /**
     * Cloning of InBufs is not allowed.  They are created as needed
     * in cloned {@link Module Modules}.
     *
     * @throws CloneNotSupportedException always
     */
    public Object clone () throws CloneNotSupportedException
    {
        throw new CloneNotSupportedException("attempt to clone InBuf");
    }
    
} // class InBuf
