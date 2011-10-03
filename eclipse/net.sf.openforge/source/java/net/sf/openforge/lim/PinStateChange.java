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

import net.sf.openforge.util.naming.ID;

/**
 * An {@link OutPinBuf} state change access; the pin can be
 * set to either drive its output or enter high impedence state.
 * It can also be synchronous, in which case it takes effect at
 * the next clock edge, or immediate.
 *
 * @author  Stephen Edwards
 * @version $Id: PinStateChange.java 88 2006-01-11 22:39:52Z imiller $
 */
public class PinStateChange extends PinAccess
{
    private static final String rcs_id = "RCS_REVISION: $Rev: 88 $";

    /** True if syncrhonous with the next clock edge, false if immediate */
    private boolean isSynchronous;
    private Physical physical = null;

    public boolean isSynchronous ()
    {
        return isSynchronous;
    }

    public PinStateChange (boolean isSynchronous)
    {
        // two ports -- address and drive state (a boolean)
        super(2);
        this.isSynchronous = isSynchronous;
        //setMainExit(makeExit(0));
        makeExit(0);
    }
    
    /**
     * Gets the port which determines whether or not
     * to drive the Pin.
     */
    public Port getDrivingPort()
    {
        return (Port)getDataPorts().get(1);
    }

    /**
     * Returns a copy of this PinStateChange by creating a new access
     * off of the {@link OutPinBuf} associated with this node.  We
     * create a new access instead of cloning because of the way that
     * the OutPinBuf stores references (not in Referent).  Creating a
     * new access correctly sets up the Referent/Reference
     * relationship.
     *
     * @return a PinStateChange object.
     * @exception CloneNotSupportedException if an error occurs
     */
    public Object clone () throws CloneNotSupportedException
    {
        PinStateChange clone = (PinStateChange)super.clone();
        this.copyComponentAttributes(clone);
        return clone;
    }

    /**
     * @return true
     */
    public boolean consumesGo ()
    {
        return true;
    }
    
    /*
     * ===================================================
     *    Begin new constant prop rules implementation.
     */
    
    public boolean pushValuesForward ()
    {
        return false;
    }
    
    public boolean pushValuesBackward ()
    {
        return false;
    }
    
    /*
     *    End new constant prop rules implementation.
     * =================================================
     */

    public void accept (Visitor v)
    {
        v.visit(this);
    }

    public Physical makePhysicalComponent()
    {
        assert (physical == null) : "Physical component of PinStateChange can only be made once.";
        physical = new Physical();
        return physical;
    }
    
    public Module getPhysicalComponent()
    {
        return physical;
    }
    
    /**
     * The full physical implementation of a PinStateChange. Physical provides
     * explicit internal connections for ports and buses, and extra logic
     * to trap the GO signal so that it can be paired with the returning
     * DONE signal. 
     * <P>
     */
    public class Physical extends PhysicalImplementationModule
    {
        private Bus sideAddress;
        private Bus sideEnable;
        private Bus sideData;
        
        /**
         * Constructs a new Physical which appropriates all the
         * port-bus connections of the PinStateChange.
         */
        public Physical()
        {
            super(0); 
            
            // one normal port for the address
            Port addressIn = makeDataPort();
            Port pinStateChangeAddress = (Port)PinStateChange.this.getDataPorts().get(0);
            assert (pinStateChangeAddress.getBus() != null) : "PinStateChange's address port not attached to a bus.";
            
            {
                Bus changeAddrBus = pinStateChangeAddress.getBus();
                // addressIn.getPeer().setSize(changeAddrBus.getSize(), changeAddrBus.getValue().isSigned());
            }
            addressIn.setUsed(pinStateChangeAddress.isUsed());
            addressIn.setBus(pinStateChangeAddress.getBus());
            
            // and one normal port for the data
            Port dataIn = makeDataPort();
            Port pinStateChangeData = (Port)PinStateChange.this.getDataPorts().get(1);
            assert (pinStateChangeData.getBus() != null) : "PinStateChange's address port not attached to a bus.";
            
            {
                Bus changeDataBus = pinStateChangeData.getBus();
                // dataIn.getPeer().setSize(changeDataBus.getSize(), changeDataBus.getValue().isSigned());
            }
            dataIn.setUsed(pinStateChangeData.isUsed());
            dataIn.setBus(pinStateChangeData.getBus());
            
            // appropriate the go signal
            Port pinStateChangeGo = PinStateChange.this.getGoPort();
            Port go = getGoPort();
            assert (pinStateChangeGo.getBus() != null) : "PinStateChange's go port not attached to a bus.";
            go.setUsed(pinStateChangeGo.isUsed());
            go.setBus(pinStateChangeGo.getBus());

            Exit physicalExit = makeExit(0);

            sideAddress = physicalExit.makeDataBus(Component.SIDEBAND);
            sideAddress.setIDLogical(ID.showLogical(PinStateChange.this) + "_WA");
            {
                Bus changeAddrBus = pinStateChangeAddress.getBus();
                // dataIn.getPeer().setSize(changeAddrBus.getSize(), changeAddrBus.getValue().isSigned());
            }
            
            sideData = physicalExit.makeDataBus(Component.SIDEBAND);
            sideData.setIDLogical(ID.showLogical(PinStateChange.this) + "_WD");
            {
                Bus changeDataBus = pinStateChangeData.getBus();
                // dataIn.getPeer().setSize(changeDataBus.getSize(), changeDataBus.getValue().isSigned());
            }
            
            sideEnable = physicalExit.makeDataBus(Component.SIDEBAND);
            sideEnable.setIDLogical(ID.showLogical(PinStateChange.this) + "_WE");
            // sideEnable.setSize(1, true);
            
            // now wire everything up
            sideEnable.getPeer().setBus(go.getPeer());
            sideAddress.getPeer().setBus(addressIn.getPeer());
            sideData.getPeer().setBus(dataIn.getPeer());            

            getClockPort().setUsed(false);
            getResetPort().setUsed(false);
            physicalExit.getDoneBus().setUsed(false);
        }
        
        public Bus getSideDataBus()
        {
            return sideData;
        }
        
        public Bus getSideAddressBus()
        {
            return sideAddress;
        }
        
        public Bus getSideEnableBus()
        {
            return sideEnable;
        }
        
        public void accept (Visitor v)
        {
            ; // nobody should be visiting this component directly
        }
        
        public boolean removeDataBus (Bus bus)
        {
            assert false : "remove data port not supported on " + this;
            return false;
        }
        
        public boolean removeDataPort (Port port)
        {
            assert false : "remove data port not supported on " + this;
            return false;
        }
        
    } // class Physical
}
