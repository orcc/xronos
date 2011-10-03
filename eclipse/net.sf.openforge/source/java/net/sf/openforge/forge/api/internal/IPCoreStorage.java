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

package net.sf.openforge.forge.api.internal;

import java.util.*;

import net.sf.openforge.forge.api.*;
import net.sf.openforge.forge.api.ipcore.*;
import net.sf.openforge.forge.api.pin.*;

/**
 * An IPCoreStorage is a collection of various IPCore pins.
 * It consists of an IPCore's name and its clock and reset name.
 * In addition, an IPCoreStorage has lists to store all the
 * pins which associates with this particular IPCore. IPCoreStorage
 * provides accessors to get IPCore pin infomation.
 *
 */
public class IPCoreStorage
{
    private static final String rcs_id = "RCS_REVISION: $Rev: 2 $";

    /** IPCore's name */
    private String module_name = null;

    // map ipcore port name to clock pin.  port name is key
    private HashMap portToClockMap=new HashMap();
    // map ipcore port name to reset pin.  port name is key
    private HashMap portToResetMap=new HashMap();
    
    /** List of IPCore pins used and unused */
    private ArrayList allPins = new ArrayList();
    private ArrayList publishPins = new ArrayList();
    private ArrayList noConnectPins = new ArrayList();

    /** List of published names for published pins */
    private ArrayList publishNames = new ArrayList();    

    private String instanceName = null;
    
    private String hdl_source = null;

    /** The users HDL Writer if they have registered one for this
     * IPCore instance. */
    private HDLWriter hdlWriter = null;

    /**
     * Constructs with a IPCore's name
     */
    public IPCoreStorage(String name)
    {
        this.module_name = name;
    }

    /**
     * Adds a Pin to this ip core
     *
     * @param pin a Buffer
     */
    public void addPin(Object pin)
    {
        allPins.add(pin);
    }

    /**
     * @return all Buffers associate with this ip core
     */
    public ArrayList getAllPins()
    {
        return allPins;
    }
    
    /**
     * Put published pin to list
     *
     * @param pin a pin to be published
     */
    public void addPublishPin (Buffer pin, String publishName)
    {
        publishPins.add(pin);
        publishNames.add(publishName);
    }

    /**
     * Checks whether a pin has been published.
     */
    public boolean hasPublished (Buffer pin)
    {
        return publishPins.contains(pin);
    }
    
    /**
     * @return a List of published pins
     */
    public ArrayList getPublishedPins()
    {
        return publishPins;
    }
    
    /** 
     * @return a List of published pin names
     */
    public ArrayList getPublishedNames()
    {
        return publishNames;
    }
    
    /**
     * Put no connect pin to list
     *
     * @param pin a no connect pin
     */
    public void addNoConnectPin(Buffer pin)
    {
        noConnectPins.add(pin);
    }

    /**
     * @return a List of no connect pins
     */
    public ArrayList getNoConnectPins()
    {
        return noConnectPins;
    }

    /**
     * @return this ipcore's name
     */
    public String getModuleName()
    {
        return module_name;
    }

    /**
     * Sets the HDL instance name to use
     *
     * @param instanceName the name of the instance in HDL.
     */
    public void setHDLInstanceName(String instanceName)
    {
        this.instanceName = instanceName;
    }

    public String getHDLInstanceName()
    {
        return this.instanceName;
    }
    
    /**
     * Include the source file
     *
     * @param source HDL source file in the form of
     * /home/john/library/HDL_CORE.v
     */
    public void setHDLSource(String source)
    {
        this.hdl_source = source;
    }

    /**
     * @return the HDL source file 
     */
    public String getHDLSource()
    {
        return this.hdl_source;
    }

    /**
     * Sets the HDLWriter that is to be used in writing out the
     * definition of this <code>IPCore</code> instance.  Optional
     * Parameter.
     */
    public void setHDLWriter (HDLWriter writer)
    {
        this.hdlWriter = writer;
    }

    /**
     * Retrieves the HDLWriter that is to be used to inline HDL code
     * when translating the <code>IPCore</code> instance that this
     * storage maintains state for, or <code>null</code> if none has
     * been registered.
     */
    public HDLWriter getHDLWriter ()
    {
        return this.hdlWriter;
    }
    
    /**
     * connect clock pin to IPCore port
     * @param clockPin clock pin to connect
     * @param portName 
     */
    public void connect(ClockPin clockPin, String portName)
    {
        Object x=portToResetMap.get(portName);
        
        if (x != null) // portName already driven by resetpin: x
        {
            throw new ForgeApiException("IPCore port "+portName+" can not be driven by both clock "+clockPin
                                        +" and reset "+x);
        }
        // now add to clock map
        x=portToClockMap.put(portName, clockPin);
        if (x != null) // if x != null then another clock is driving portName
        {
            throw new ForgeApiException("Can't have two clocks ("+x+" and "+clockPin+") driving port "+portName);
        }
    }
    /**
     * return an hashmap of clocks for this ip core
     * keyset is the list of String portName
     * each Value is the ClockPin that drives that port
     */
    public HashMap getClockMap ()
    {
        return portToClockMap;
    }
    
    /**
     * connect reset pin to IPCore port
     * note that active high or low is defined by the reset pin
     * @param resetPin reset pin to connect
     * @param portName
     */
    public void connect (ResetPin resetPin, String portName)
    {
        Object x=portToClockMap.get(portName);
        if (x!=null) // portName already driven by a clockpin: x
        {
            throw new ForgeApiException("IPCore port "+portName+" can not be driven by both clock "+x
                                        +" and reset "+resetPin);
        }
        // now add to reset map
        x=portToResetMap.put(portName, resetPin);
        if (x != null) // if x != null then another reset is driving portName
        {
            throw new ForgeApiException("Can't have two resets ("+x+" and "+resetPin+") driving port "+portName);
        }
    }
    /**
     * return an hashmap of resets for this ip core
     * keyset is the list of String portNames
     * each Value is the ResetPin that drives that port
     */
    public HashMap getResetMap ()
    {
        return portToResetMap;
    }
    
}
