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

import net.sf.openforge.util.naming.ID;

/**
 * A pin which a {@link Design} may both read and write.
 * This pin may also be tri-stated.
 *
 * @author  Stephen Edwards
 * @version $Id: BidirectionalPin.java 2 2005-06-09 20:00:48Z imiller $
 */
public class BidirectionalPin extends Pin
{
    private static final String rcs_id = "RCS_REVISION: $Rev: 2 $";

    private InPinBuf inBuf;
    private OutPinBuf outBuf;

    private Bus bus;
    private Port port;
    
    /**
     * Constructs a new BidirectionalPin with a 
     * specific width and signedness.
     *
     * @param width the bit-width of the pin
     * @param isSigned the signedness of the pin
     */
    public BidirectionalPin (int width, boolean isSigned)
    {
        this(width, isSigned, "bipin");
    }
    
    /**
     * Constructs a BidirectionalPin with explicit size, signedness and name.
     * 
     * @param width
     * @param isSigned
     * @param name
     */
    public BidirectionalPin(int width, boolean isSigned, String name)
    {
        super(width, isSigned);
        this.inBuf = new InPinBuf(this);
        this.outBuf = new OutPinBuf(this);
        setIDLogical(name);
        makeBus();
        makePort();
    }
    
    private void makeBus()
    {
        Exit exit = makeExit(0);
        this.bus = (Bus)exit.makeDataBus();
        bus.setSize(getWidth(), true);
        bus.setIDLogical(ID.showLogical(this));
    }
    
    private void makePort()
    {
        this.port = makeDataPort();
        port.setIDLogical(ID.showLogical(this));
    }

    public Port getPort()
    {
        return port;
    }
    
    public Bus getBus()
    {
        return bus;
    }
    
    public InPinBuf getInPinBuf ()
    {
        return inBuf;
    }

    public OutPinBuf getOutPinBuf ()
    {
        return outBuf;
    }

    public Collection getPinBufs ()
    {
        Set bufs = new LinkedHashSet();
        bufs.add(getInPinBuf());
        bufs.add(getOutPinBuf());
        return Collections.unmodifiableSet(bufs);
    }

    /*
     * ===================================================
     *    Begin new constant prop rules implementation.
     */
    
    /**
     * Pushes the size of the pin onto the Port as all care bits.
     */
    public boolean pushValuesBackward ()
    {
        boolean mod = false;
        
        Value newValue = new Value(getWidth(), true);
        
        mod |= getPort().pushValueBackward(newValue);
        
        return mod;
    }
    
    /*
     *    End new constant prop rules implementation.
     * =================================================
     */

    /**
     * Returns a full copy of this Pin
     *
     * @return a BidirectionalPin object.
     * @exception CloneNotSupportedException if an error occurs
     */
    public Object clone () throws CloneNotSupportedException
    {
        throw new CloneNotSupportedException();
    }
    
}
