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

import net.sf.openforge.forge.api.ipcore.*;
import net.sf.openforge.forge.api.pin.*;

/**
 * A Core is a collection of user IPCores. This class provides easy
 * access to each IPCore by storing them in a HashMap.
 *
 */
public class Core
{
    private static final String rcs_id = "RCS_REVISION: $Rev: 2 $";

    /** A HashMap of IPCore->IPCoreStorage */
    private static HashMap internalCoreMap = new HashMap();
    
    /**
     * @return A set of IPCore objects
     */
    public static Set getIPCores()
    {
        return internalCoreMap.keySet();
    }

    public static IPCore getIPCore(IPCoreStorage ipcs)
    {
        // we need to find the IPCore that maps to the given storage
        for(Iterator it = internalCoreMap.keySet().iterator();
            it.hasNext(); )
        {
            IPCore ipc = (IPCore)it.next();

            if(ipcs == internalCoreMap.get(ipc))
            {
                return(ipc);
            }
        }

        return null;
    }

    
    /**
     * Add a new IPCore to the internal database of user IPCore objects.
     *
     * @param ipc an IPCore Object
     * @param moduleName a String name by which the IPCore is uniquely
     * identified.
     */
    public static void addIPCore (IPCore ipc, String moduleName)
    {
        IPCoreStorage ipcs = new IPCoreStorage(moduleName);
        internalCoreMap.put(ipc, ipcs);
    }

    /**
     * Adds the specified pin to the data maintained for the given
     * IPCore.
     *
     * @param pin a Buffer Object
     * @param ipc an IPCore Object
     */
    public static void addToPinIPCoreMap(Buffer pin, IPCore ipc)
    {
        getIPCoreStorage(ipc).addPin(pin);
    }

    /**
     * Get an IPCoreStorage object from the internal database using
     * the given IPCore object as the key
     * 
     * @param ipc an IPCore object
     * @return the IPCoreStorage for the specified IPCore
     */
    public static IPCoreStorage getIPCoreStorage (IPCore ipc)
    {
        return (IPCoreStorage)internalCoreMap.get(ipc);
    }

    /**
     * Returns the <code>IPCoreStorage</code> that maintains state
     * information and attributes for the <code>IPCore</code> to which
     * the given <code>Buffer</code> belongs.
     *
     * @param pin a <code>Buffer</code>
     * @return an <code>IPCoreStorage</code> or null if the given
     * <code>Buffer</code> does not belong to an <code>IPCore</code>.
     */
    public static IPCoreStorage getIPCoreStorage (Buffer pin)
    {
        for (Iterator iter = getIPCoreStorages().iterator(); iter.hasNext();)
        {
            IPCoreStorage storage = (IPCoreStorage)iter.next();
            if (storage.getAllPins().contains(pin))
            {
                return storage;
            }
        }

        return null;
    }
    
    /**
     * Returns true of the database contains no IPCores.
     *
     * @return true if no IPCore objects have been registered.
     */
    public static boolean isEmpty()
    {
        return internalCoreMap.isEmpty();
    }
    
    /**
     * Gets all the IPCoreStorage objects that have been registered
     * (one per IPCore).
     *
     * @return a Collection of IPCoreStorage objects
     */
    public static Collection getIPCoreStorages()
    {
        return new HashSet(internalCoreMap.values());
    }

    /** 
     * Determine whether a <code>Buffer</code> belongs to an
     * IPCore. 
     * 
     * @param pin a <code>Buffer</code>
     * @return true if the <code>Buffer</code> belongs to an IPCore.
     */
    public static boolean hasThisPin(Buffer pin)
    {
        return getIPCoreStorage(pin) != null;
    }

    /**
     * Determines whether a <code>Buffer</code> belongs to an
     * IPCore and has been published.
     *
     * @param pin a <code>Buffer</code>
     * @return true if the <code>Buffer</code> belongs to an IPCore and
     * has been published.
     */
    public static boolean hasPublished(Buffer pin)
    {
        IPCoreStorage ipcs = getIPCoreStorage(pin);
        return ((ipcs != null) && ipcs.hasPublished(pin));
    }

    /**
     * Return a shallow clone of the internal clone
     *
     * @return a value of type 'Map'
     */
    public static Map cloneIPCoreMap()
    {
        return (Map)internalCoreMap.clone();
    }

    /**
     * Impose a set of ipcore data on the underlying map
     *
     * @param m a value of type 'Map'
     */
    public static void setIPCoreMap(Map m)
    {
        clearIPCoreMap();
        for(Iterator it=m.entrySet().iterator();it.hasNext();)
        {
            Map.Entry me=(Map.Entry)it.next();
            internalCoreMap.put(me.getKey(),me.getValue());
        }
    }
    
    /**
     * Clears all entries in the internal IPCore->IPCoreStorage
     * map.
     */
    public static void clearIPCoreMap()
    {
        internalCoreMap.clear();
    }
    
}
