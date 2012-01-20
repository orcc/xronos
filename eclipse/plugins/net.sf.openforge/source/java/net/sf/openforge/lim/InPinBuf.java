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

import net.sf.openforge.util.naming.*;


/**
 * An InPinBuf allows reading from an input {@link Pin}.
 *
 * @author  Stephen Edwards
 * @version $Id: InPinBuf.java 280 2006-08-11 17:00:32Z imiller $
 */
public class InPinBuf extends PinBuf
{
    private static final String rcs_id = "RCS_REVISION: $Rev: 280 $";

    private Physical phys=null;
    
    InPinBuf (Pin pin)
    {
        /*
         * tbd.
         */
        super(pin);
    }

    /**
     * Gets the latency of a reference's exit.
     */
    public Latency getLatency (Exit exit)
    {
        /*
         * tbd.
         */
        return Latency.ZERO;
    }

    public boolean consumesReset()
    {
        return false;
    }
    
    public boolean consumesClock()
    {
        net.sf.openforge.forge.api.pin.Buffer b=getPin().getApiPin();
        if(b!=null)
        {
            return b.getInputPipelineDepth()>0;
        }
        return false;
    }
    
    public Physical getPhysicalComponent()
    {
        //assert phys!=null;
        return phys;
    }

    /**
     * Makes the Physical implementation of this InPinBuf, and
     * connects it to the InputPin.
     */
    public Physical makePhysicalComponent()
    {
        int pDepth=0;
        net.sf.openforge.forge.api.pin.Buffer b=getPin().getApiPin();
        if(b!=null)
        {
            pDepth=b.getInputPipelineDepth();
        }

        phys=new Physical(getPin().getWidth(),pDepth); // TBD CRSS -- no piplined depth
        
        return phys;
    }

    /**
     * Physical implementation of the InPinBuf logic. 1 input port,
     * the value coming from outsidfe the design. 1 output port
     * the value passing through
     *
     * @author "C. Schanck" <cschanck@cschanck>
     * @version 1.0
     * @since 1.0
     * @see Module
     */
    public class Physical extends PhysicalImplementationModule
    {
        private Bus resultBus;
        private int size;
        private int pDepth;
        
        Physical(int size,int piplinedDepth)
        {
            this.size=size;
            this.pDepth=piplinedDepth;

            Exit exit = makeExit(0);

            // just one data port
            makeDataPort();
//             getInputPort().getPeer().setSize(size, true);

            // make a result bus
            resultBus = (Bus)exit.makeDataBus();
            resultBus.setUsed(true);
            resultBus.setSize(size, true);
        }

        public void connect()
        {
            // the result so far ...
            Bus lastBus=getInputPort().getPeer();

            // now the piplined case
            // for each one
            for(int i=0;i<pDepth;i++)
            {
                // clock used
                getClockPort().setUsed(true);

                // make reg
                Reg reg = Reg.getConfigurableReg(Reg.REG, "inpinbuf_pipe"+i);
                reg.setIDLogical(ID.showLogical(this)+"_pipe"+i);
                addComponent(reg);

                // set up the clock
                reg.getClockPort().setBus(getClockPort().getPeer()); // connect clock

                // set the data
                reg.getDataPort().setBus(lastBus);

                // size bus of reg
//                 reg.getResultBus().setSize(size, true);

                // bump the result
                lastBus=reg.getResultBus();
            }

            // finally connect our resultbus to the last bus
            resultBus.getPeer().setBus(lastBus);        
        }

        /**
         * Data value, properly piplined
         *
         * @return a value of type 'Bus'
         */
        public Bus getResultBus()
        {
            return resultBus;
        }

        /**
         * Input port, hook up at your leisure
         *
         * @return a value of type 'Port'
         */
        public Port getInputPort()
        {
            return (Port)getDataPorts().get(0);
        }

        public void accept (Visitor v)
        {
            throw new UnexpectedVisitationException("InPinBuf's should not be visited!");
        }
    }

}


