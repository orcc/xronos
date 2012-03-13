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

import net.sf.openforge.util.naming.IDSourceInfo;

/**
 * An Entry specifies one possible set of inputs for
 * a Component's Ports.  For each Port, the inputs are
 * specified as a collection of one or more Dependencies.
 * When scheduling, the Dependencies of a given Entry
 * must be scheduled so that they
 * are all fulfilled simultaneously.  However, they may be
 * scheduled without regard to the Dependencies of other Entry
 * objects.  Multiple
 * Entryies for the same Component are logically muxed.
 * <P>
 * It is expected that each Entry will be created by its
 * owner using {@link Component#makeEntry}.
 *
 * @author  Stephen Edwards
 * @version $Id: Entry.java 74 2005-12-14 21:05:47Z imiller $
 */
public class Entry
{
    private static final String rcs_id = "RCS_REVISION: $Rev: 74 $";

    /** Map of Port to Collection of Dependency */
    private Map portMap = new LinkedHashMap();

    /** The owner of this Entry */
    private Component owner;

    /** The Exit whose execution path leads to this Entry */
    private Exit drivingExit;

    /**
     * Constructs a new Entry.
     *
     * @param owner the component to which this entry belongs
     */
    Entry (Component owner)
    {
        this(owner, null);
    }

    Entry (Component owner, Exit drivingExit)
    {
        this.owner = owner;
        //this.drivingExit = drivingExit;
        setDrivingExit(drivingExit);
    }



    /**
     * Get the ports described by this Entry.
     *
     * @return a collection of Ports
     */
    public List<Port> getPorts ()
    {
        return getOwner().getPorts();
    }

    /**
     * Gets the dependencies for a specified port contained in a new
     * hashset (solves problem of changed hases on dependencies).
     *
     * @param port a port described by this Entry
     * @return a collection of Dependency objects
     */
    public Collection<Dependency> getDependencies (Port port)
    {
        Collection<Dependency> deps = (Collection<Dependency>)portMap.get(port);
        return deps == null ? ((Collection<Dependency>)Collections.EMPTY_LIST) : deps;
    }

    /**
     * Adds a Dependency to a given port.
     *
     * @param port a port described by this Entry
     * @param dependency a dependency to be added to the port
     *          for this Entry
     */
    public void addDependency (Port port, Dependency dependency)
    {
        assert getPorts().contains(port) : "unknown port";

        Collection deps = (Collection)portMap.get(port);
        if (deps == null)
        {
            deps = new HashSet(3);
            portMap.put(port, deps);
        }

        // IDM 01:03 moved to after the 'sets' because the hashcode of
        // a Dependency depends on the port and entry values.  By
        // adding it before the 'sets' we were ending up with multiple
        // of the 'same' dependency in the hashset.
        //deps.add(dependency);
        
        dependency.setPort(port);
        dependency.setEntry(this);
        deps.add(dependency);
    }

    /**
     * Removes the mapping of the given dependency to the port in this
     * entry, but does <b>NOT</b> clear out any of the relationships
     * stored in buses or the dependency, use {@link Dependency#zap}
     * instead.
     *
     * @param port the Port which has the mapped dependency
     * @param toRemove the {@link Dependency} to remove
     */
    public void removeDependency(Port port, Dependency toRemove)
    {
        assert portMap.containsKey(port) : "Unknown port";
        
        Collection depsForPort = (Collection)this.portMap.get(port);
        assert depsForPort.contains(toRemove) : "Entry doesn't contain dependency for port. Dep: " + toRemove + " deps " + depsForPort;
        depsForPort.remove(toRemove);
    }
    
    /**
     * Removes all {@link Dependency Dependencies} for a given {@link Port}, including
     * any references to the {@link Dependency} in its {@link Bus Buses}.
     */
    public void clearDependencies (Port port)
    {
        for (Iterator iter = new ArrayList(getDependencies(port)).iterator(); iter.hasNext();)
        {
            ((Dependency)iter.next()).zap();
        }
    }

    /**
     * Removes and destroys all {@link Dependency Dependencies} from
     * this entry.
     */
    public void decimate()
    {
        // First disconnect all the dependencies so that we don't have
        // leftover references to them in the buses that used to be
        // depended upon.
        for (Iterator portIter = portMap.keySet().iterator(); portIter.hasNext();)
        {
            Port port = (Port)portIter.next();
            clearDependencies(port);
        }

        // Then clear the map.
        portMap.clear();

        // Remove this entry from the exit which drives it.
        setDrivingExit(null);
    }

    /**
     * Gets the {@link Exit} whose path of control leads to this Entry.
     */
    public Exit getDrivingExit ()
    {
        return drivingExit;
    }

    /**
     * Sets the {@link Exit} whose path of control leads to this Entry.
     */
    public void setDrivingExit (Exit exit)
    {
        if (this.drivingExit != null)
        {
            this.drivingExit.removeEntry(this);
        }
        
        this.drivingExit = exit;
        
        if (exit != null)
        {
            exit.drives(this);
        }
    }

    /**
     * Gets the component which this entry annotates.
     */
    public Component getOwner ()
    {
        return owner;
    }

    /**
     * Gets the owner's clock port.
     */
    public Port getClockPort ()
    {
        return getOwner().getClockPort();
    }

    /**
     * Gets the owner's reset port.
     */
    public Port getResetPort ()
    {
        return getOwner().getResetPort();
    }

    /**
     * Gets the owner's go port.
     */
    public Port getGoPort ()
    {
        return getOwner().getGoPort();
    }

    /**
     * Gets the owner's data ports.
     *
     * @return a collection of Ports
     */
    public List getDataPorts ()
    {
        return getOwner().getDataPorts();
    }

    /**
     * Overrides IDNameAdaptor.getIDSourceInfo()
     *
     * @return this entry owner id source info
     */
    public IDSourceInfo getIDSourceInfo()
    {
        return getOwner().getIDSourceInfo();
    }

    public String debug()
    {
        String ret = "";
        ret += "Entry " + this + " owner: " + this.getOwner() + " driving exit " + drivingExit + "\n";
        for (Iterator iter = portMap.keySet().iterator(); iter.hasNext();)
        {
            Port port = (Port)iter.next();
            String pname = port.toString();
            if (port == getOwner().getClockPort()) pname = "Clock<" + pname + ">";
            if (port == getOwner().getResetPort()) pname = "Reset<" + pname + ">";
            if (port == getOwner().getGoPort()) pname = "Go<" + pname + ">";
            for (Iterator depIter = ((Collection)portMap.get(port)).iterator(); depIter.hasNext();)
            {
                ret += pname + " => " + ((Dependency)depIter.next()).debug() + "\n";
            }
        }
        return ret;
    }

    public String toString()
    {
        String ret = super.toString();
        ret = ret.replaceAll("net.sf.openforge.","");
        ret += "<" + getOwner().toString() + ">";
        return ret;
    }
    
    
    
}
