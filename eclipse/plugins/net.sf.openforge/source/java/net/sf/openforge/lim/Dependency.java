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

/**
 * A Dependency describes the relationship between a dependent
 * {@link Port Port} and the {@link Bus Bus} on which it depends.
 * There are multiple types of dependency and the type of dependency
 * affects how it is handled during scheduling.  DataDependencies
 * indicate that the port will recieve a connection to the logical bus
 * that is depended on, though it may be delayed, latched, etc.
 * ControlDependencies will cause the scheduler to find the control
 * signal which qualifies the dependent bus and will use that
 * controlling signal to resolve the dependency.
 *
 * @author  Stephen Edwards
 * @version $Id: Dependency.java 556 2008-03-14 02:27:40Z imiller $
 */
public abstract class Dependency implements Cloneable
{
    private static final String rcs_id = "RCS_REVISION: $Rev:2 $";

    /** The port which this Dependency annotates */
    private Port port = null;

    /** The logical port connection; must be owned by an Executable */
    private Bus logicalBus = null;

    /** The entry with which this dependency is associated. */
    private Entry entry = null;

    /**
     * Constructs a new Dependency.
     *
     * @param logicalBus the bus on which the port will logically depend;
     *          this bus must be owned by an Executable
     */
    public Dependency (Bus logicalBus)
    {
        setLogicalBus(logicalBus);
    }

    /**
     * Returns a new dependency that is the same type as this dependency.
     */
    public abstract Dependency createSameType(Bus logcalBus);

    /**
     * Gets the number of clock edges that must exist between the
     * logical bus contained in this dependency and the enabling of
     * the entry to which this dependency is attached.
     *
     * @return the number of clocks that the port input should be
     *         delayed following the fulfillment of this dependency
     */
    public int getDelayClocks ()
    {
        return 0;
    }

    /**
     * Gets the port to which this dependency belongs.
     */
    public Port getPort ()
    {
        return port;
    }

    /**
     * Gets the entry with which this Dependency is associated.
     *
     * @return the associated entry
     */
    public Entry getEntry ()
    {
        return entry;
    }
    
    /**
     * Sets the entry with which this Dependency is associated.
     *
     * @param entry the associated entry
     */
    public void setEntry(Entry entry)
    {
        this.entry = entry;
    }
    
    /**
     * Gets the logical bus connection implied by this dependency.
     */
    public Bus getLogicalBus ()
    {
        return logicalBus;
    }

    /**
     * Sets the logical bus connection implied by this dependency.
     */
    public void setLogicalBus (Bus bus)
    {
        if (this.logicalBus != null)
        {
//             this.logicalBus.logicalDependents.remove(this);
            assert false : "Logical Bus in Dependency is immutable";
        }
        
        this.logicalBus = bus;
        
        if (bus != null && getPort() != null)
        {
            bus.logicalDependents.add(this);
        }
    }

    /**
     * Removes this dependency from it's containing entry and sets the
     * logical bus to null (which in turn causes the bus to lose any
     * references back to this dependency).
     *
     */
    public void zap()
    {
        if (getEntry() != null)
        {
            getEntry().removeDependency(getPort(), this);
        }

        removeFromBuses();
        
        //setLogicalBus(null);
        this.logicalBus = null;
    }
    

    /**
     * Tests the given Object for equality with this Dependency by
     * checking <b>only</b> the port and logical bus.
     *
     * @param obj a value of type 'Object'
     * @return true if the port and logical bus match.
     */
    public boolean equals (Object obj)
    {
        if ((obj != null) && (obj instanceof Dependency))
        {
            Dependency dep = (Dependency)obj;
            return (dep.getPort().equals(getPort())
                && dep.getLogicalBus() == getLogicalBus());
                
        }
        return false;
    }

    /**
     * Generates a hashcode based <b>only</b> on the port, logical
     * bus, and class of this object.
     *
     * @return a value of type 'int'
     */
    public int hashCode ()
    {
        int code = getClass().hashCode();
        code += port == null ? 0 : port.hashCode();
        code += logicalBus == null ? 0 : logicalBus.hashCode();
        return code;
    }

    /**
     * Clones this Dependency.
     *
     * @return a copy of this dependency with a null port, logical
     * bus, and entry.
     */
    public Object clone ()
    {
        try
        {
            Dependency clone = (Dependency)super.clone();
            clone.port = null;
            clone.logicalBus = null;
            clone.entry = null;
            return clone;
        }
        catch (CloneNotSupportedException eClone)
        {
            assert false : eClone;
            return null;
        }
    }

    /**
     * Sets the port to which this dependency belongs.
     */
    public void setPort (Port depPort)
    {
        if (this.port != null)
        {
            assert false : "Port field in dependency is immutable";
        }
        
        this.port = depPort;
        
        if (port != null)
        {
            addToBuses();
        }
        else
        {
            removeFromBuses();
        }
    }

    /**
     * Adds this Dependency to its source buses.
     */
    private void addToBuses ()
    {
        Bus lbus = getLogicalBus();
        if (lbus != null)
        {
            lbus.logicalDependents.add(this);
        }
    }

    /**
     * Removes this Dependency from its source buses.
     */
    private void removeFromBuses ()
    {
        Bus lbus = getLogicalBus();
        if (lbus != null)
        {
            lbus.logicalDependents.remove(this);
        }
    }

    public String toString()
    {
        String ret = super.toString();
        ret = ret.replaceAll("net.sf.openforge.lim.","");
        return ret;
    }
    

    public String debug()
    {
        String ret = "";
        ret += toString() + " logical: " + getLogicalBus();
        return ret;
    }
    
}
