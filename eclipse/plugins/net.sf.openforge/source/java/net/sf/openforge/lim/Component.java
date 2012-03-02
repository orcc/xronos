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

import net.sf.openforge.app.Engine;
import net.sf.openforge.app.EngineThread;
import net.sf.openforge.app.GenericJob;
import net.sf.openforge.app.project.*;
import net.sf.openforge.lim.util.PostScheduleCallback;
import net.sf.openforge.report.*;
import net.sf.openforge.schedule.LatencyTracker;
import net.sf.openforge.util.naming.ID;



/**
 * Component is the base class of all components.
 *
 * @author  Stephen Edwards
 * @version $Id: Component.java 558 2008-03-14 14:14:48Z imiller $
 */
public abstract class Component extends ID 
    implements Visitable, Cloneable, Configurable
{
    /**  The default port or bus      */
    public static final Type NORMAL      = new Type(Type.ID_NORMAL);

    /** The type of port or bus that is associated with static or field variable accesses */
    public static final Type SIDEBAND     = new Type(Type.ID_SIDEBAND);

    /** The containing Module; null if there is none */
    private Module owner = null;

    /** The model which this component is derived from (java.model or other language) */
    private Object modelObject = null;

    /** Clock signal port */
    private Port clockPort;

    /** Reset signal port */
    private Port resetPort;

    /** Go signal port */
    private Port goPort;

    /** Halt signal port */
    private Port haltPort;
    
    /** 'this' data port, null if not used by this component */
    private Port thisPort;

    /** List of argument Ports */
    protected List<Port> dataPorts = new LinkedList();
    
    /** Entry */
    protected List<Entry> entries = new LinkedList<Entry>();

    /** Map of Exit.Tag to Exit */
    Map<Exit.Tag, Exit> exits = new HashMap();
    
    /** Natural log of 2 */
    private static final double LN_2 = Math.log(2);

    /** Collection of CloneListener */
    private Collection cloneListeners = Collections.EMPTY_LIST;

    /** denotes if this Component is non-removable */
    private boolean nonRemovable = false;

    /** A list of Attribute objects that should be attached to this
     * component when instantiated in the LIM. */
    private List attributes = Collections.EMPTY_LIST;

    private List<PostScheduleCallback> schedCallbacks = Collections.EMPTY_LIST;
    
    /**
     * A type identifier for an Bus or Port.
     *
     * @version $Id: Component.java 558 2008-03-14 14:14:48Z imiller $
     */
    public final static class Type
    {
        private static final String _RCS_ = "$Rev: 558 $";

        public static final int ID_NORMAL = 0;
        public static final int ID_SIDEBAND = 1;

        /** The kind of exit */
        private int id;

        private Type (int id)
        {
            this.id = id;
        }

        private int getId ()
        {
            return id;
        }

        public String toString ()
        {
            switch (getId())
            {
                case ID_NORMAL:
                    return "NORMAL";
                case ID_SIDEBAND:
                    return "SIDEBAND";
                default:
                    assert false : "Unknown id: " + getId();
                    return null;
            }
        }
    }
    
    /**
     * Constructs a new Component.
     *
     * @param dataCount the number of data {@link Port Ports} to create
     */
    public Component (int dataCount)
    {
        clockPort = new Port(this);
        resetPort = new Port(this);
        goPort = new Port(this);
        clockPort.setUsed(false);
        resetPort.setUsed(false);
        goPort.setUsed(false);
        
        if (dataCount < 0)
        {
            throw new IllegalArgumentException("negative dataCount");
        }

        if (dataCount > 0)
        {
            this.dataPorts = new LinkedList();
            for (int i = 0; i < dataCount; i++)
            {
                makeDataPort();
            }
        }
    }

    /**
     * Constructs a Component with no data ports.
     */
    public Component ()
    {
        this(0);
    }

    /**
     * Gets the single data {@link Bus} from the {@link Exit#DONE DONE}
     * {@link Exit} of a given {@link Component}.
     *
     * @param component the bus's component; it is assumed the component
     *          has a DONE exit with a single data bus
     * @return the data bus
     */
    public static Bus getDataBus (Component component)
    {
        final List dataBuses = component.getExit(Exit.DONE).getDataBuses();
        // C pre increment and predecrement ops generate a block with 2 output
        // buses.  the "data bus" should be bus 0.  bus 1 is the pre altered
        // value. 
        if (!(component instanceof Block) && dataBuses.size() != 1)
        {
            throw new IllegalArgumentException("bad data bus count: " + dataBuses.size()
                + " for component " + component);
        }
        return (Bus)dataBuses.get(0);
    }

    /**
     * Tests whether this component is opaque.  If true, then this
     * component is to be treated as a self-contained entity.  This
     * means that its internal definition can make no direct references
     * to external entitities.  In particular, external {@link Bit Bits}
     * are not pushed into this component during constant propagation,
     * nor are any of its internal {@link Bit Bits} propagated to its
     * external {@link Bus Buses}.
     * <P>
     * Typically this implies that the translator will either generate
     * a primitive definition or an instantiatable module for this
     * component.
     *
     * @return true if this component is opaque, false otherwise
     */
    public boolean isOpaque ()
    {
        return true;
    }

    /**
     * Updates the {@link Value} of each {@link Port} then calls
     * {@link #pushValuesForward()}.  This method is called by the
     * various constant propagation visitors.
     *
     * @return the result of {@link #pushValuesForward}
     */
    public boolean propagateValuesForward ()
    {
        for (Iterator iter = getPorts().iterator(); iter.hasNext();)
        {
            Port p=(Port)iter.next();
            try
            {
                p.pushValueForward();
            }
            catch (Port.SizeMismatchException sme)
            {
                System.out.println("Error processing " + this);
                throw sme;
            }
        }
        return pushValuesForward();
    }

    /**
     * Updates the {@link Value} of each {@link Bus} then calls
     * {@link #pushValuesBackward()}.  This method is called by
     * the various constant propagation visitors.
     *
     * @return the result of {@link #pushValuesBackward}
     */
    public boolean propagateValuesBackward ()
    {
        for (Iterator iter = getBuses().iterator(); iter.hasNext();)
        {
            ((Bus)iter.next()).pushValueBackward();
        }
        return pushValuesBackward();
    }

    /**
     * Performs forward constant propagation through this component.  This
     * component will fetch the incoming {@link Value} from each {@link Port}
     * using {@link Port#_getValue()}.  It will then compute a new outgoing
     * {@link Value} for each {@link Bus} and set it with
     * {@link Bus#pushValueForward(Value)}.
     *
     * @return true if any of the bus values was modified, false otherwise
     */
    protected boolean pushValuesForward ()
    {
        return false;
    }

    /**
     * Performs reverse constant propagation inside through component.  This
     * component will fetch the incoming {@link Value} from each {@link Bus}
     * using {@link Bus#_getValue()}.  It will then compute a new outgoing
     * {@link Value} for each {@link Port} and set it with
     * {@link Port#pushValueBackward(Value)}.
     *
     * @return true if any of the port values was modified, false otherwise
     */
    protected boolean pushValuesBackward ()
    {
        return false;
    }



    /**
     * Gets the {@link CloneListener CloneListeners} for this component.
     *
     * @return a collection of {@link CloneListener}
     */
    public Collection getCloneListeners ()
    {
        return cloneListeners;
    }

    /**
     * Adds a {@link CloneListener} to this component.
     *
     * @param cloneListener the listener to receive notification of cloning
     */
    public void addCloneListener (CloneListener cloneListener)
    {
        if (cloneListeners.isEmpty())
        {
            cloneListeners = new LinkedList();
        }
        cloneListeners.add(cloneListener);
    }

    /**
     * Removes a {@link CloneListener} from this component.
     *
     * @param cloneListener a previously added listener
     * @return true if the listener was removed, false if not found
     */
    public boolean removeCloneListener (CloneListener cloneListener)
    {
        return cloneListeners.remove(cloneListener);
    }

    public void addPostScheduleCallback (PostScheduleCallback cb)
    {
        if (schedCallbacks.size() == 0)
            this.schedCallbacks = new ArrayList();
        this.schedCallbacks.add(cb);
    }

    public void removePostScheduleCallback (PostScheduleCallback cb)
    {
        if (!this.schedCallbacks.remove(cb))
        {
            throw new IllegalArgumentException("No such callback " + cb + " in " + this);
        }
        if (this.schedCallbacks.size() == 0)
            this.schedCallbacks = Collections.EMPTY_LIST;
    }

    public void postScheduleCallback (LatencyTracker lt)
    {
        // Make a copy so that the callbacks can delete themselves
        // after being invoked.
        List<PostScheduleCallback> copy = new ArrayList(this.schedCallbacks);
        for (PostScheduleCallback pcb : copy)
        {
            pcb.postSchedule(lt, this);
        }
    }
    
    /**
     * Gets the gate depth of this component.  This is the maximum number of gates
     * in any combinational path within the bounds of this component.
     *
     * @return a non-negative integer
     */
    public int getGateDepth ()
    {
        return 0;
    }

    /**
     * Gets the FPGA hardware resource usage of this component.
     *
     * @return a FPGAResource objec
     */
    public FPGAResource getHardwareResourceUsage ()
    {
        return new FPGAResource();
    }
    
    /**
     * Gets the entry gate depth of this component.  This is the maximum number of
     * gates in any combinational path within the bounds of this component such
     * that the path starts at the entry of this component.
     *
     * @return a non-negative integer
     */
    public int getEntryGateDepth ()
    {
        return getGateDepth();
    }

    /**
     * Gets the exit gate depth of this component.  This is the maximum number of
     * gates in any combinational path within the bounds of this component such
     * that the path ends at an {@link Exit} of this component.
     *
     * @return a non-negative integer
     */
    public int getExitGateDepth ()
    {
        return getGateDepth();
    }

    /**
     * Gets the input ports.
     *
     * @return a Collection of {@link Port Ports}
     */
    public List<Port> getPorts ()
    {
        // The 'this' port is included in the ports returned from the
        // data ports.
        List dataPortsList = getDataPorts();

        int portCount = dataPortsList.size() + 3;
        List<Port> list = new ArrayList(portCount);
        list.add(getClockPort());
        list.add(getResetPort());
        list.add(getGoPort());
        list.addAll(dataPortsList);
        return list;
    }

    /**
     * Gets the clock port.
     */
    public Port getClockPort ()
    {
        return clockPort;
    }

    /**
     * Gets the reset port.
     */
    public Port getResetPort ()
    {
        return resetPort;
    }

    /**
     * Gets the go port.
     */
    public Port getGoPort ()
    {
        return goPort;
    }

    /**
     * Gets the halt port.  Currently returns null since the global
     * halt signal isn't supported.  Once supported, all cloning and
     * operations on ports similar to getGoPort() will need to be
     * added for getHaltPort().
     *
     */
    public Port getHaltPort ()
    {
        return haltPort;
    }
    

    /**
     * Gets the <i>this</i> port or null if port not needed for this
     * component.
     */
    public Port getThisPort ()
    {
        return this.thisPort;
    }
    

    /**
     * Gets the data ports, including the 'this' port.
     *
     * @return a list of {@link Port Ports}
     */
    public List<Port> getDataPorts ()
    {
        if (getThisPort() == null)
        {
            return dataPorts;
        }
        else
        {
            ArrayList<Port> dpList = new ArrayList(dataPorts.size() + 1);
            dpList.add(getThisPort());
            dpList.addAll(this.dataPorts);
            return dpList;
        }
    }

    /**
     * Gets the output data buses of all {@link Exit Exits}
     *
     * @return a collection of {@link Bus buses}
     */
    public Collection getDataBuses ()
    {
        List list = new LinkedList();
        for (Iterator iter = exits.values().iterator(); iter.hasNext();)
        {
            Exit exit = (Exit)iter.next();
            list.addAll(exit.getDataBuses());
        }
        return list;
    }
    
    /**
     * Gets the output buses of all {@link Exit Exits}.
     *
     * @return a list of {@link Bus Buses}
     */
    public Collection getBuses ()
    {
        List list = new LinkedList();
        for (Iterator iter = exits.values().iterator(); iter.hasNext();)
        {
            Exit exit = (Exit)iter.next();
            list.addAll(exit.getBuses());
        }
        return list;
    }

    /**
     * Gets all exits. This collection is in no particular order!!!!
     *
     * @return a collection of Exit objects
     */
    public Collection<Exit> getExits ()
    {
        return new ArrayList(exits.values());
    }

    /**
     * Get the one and only one exit for this comp. Assert's that
     * there can be only 1. The Highlander of getExit calls.
     *
     * @return a value of type 'Exit'
     */
    public Exit getOnlyExit()
    {
        assert getExits().size()==1;
        return (Exit)getExits().iterator().next();
    }

    /**
     * Get's one exit, asserting there is at least one.
     *
     * @return a value of type 'Exit'
     */
    public Exit getAnyExit()
    {
        assert getExits().size()>=1;
        return (Exit)getExits().iterator().next();
    }
        
    /**
     * Gets the {@link Exit} with a given {@link Exit.Tag Tag}.
     *
     * @param tag an exit tag
     * @return the specified exit, or null if not found
     */
    public Exit getExit (Exit.Tag tag)
    {
        return (Exit)exits.get(tag);
    }

    /**
     * Gets the {@link Exit} whose {@link Exit.Tag Tag} has a
     * specified {@link Exit.Type Type} and label.
     *
     * @param type the exit type
     * @param label the exit label
     * @return the exit, or null if not found
     */
    public Exit getExit (Exit.Type type, String label)
    {
        return getExit(Exit.getTag(type, label));
    }

    /**
     * Gets the {@link Exit} whose {@link Exit.Tag Tag} has a
     * specified {@link Exit.Type Type} and no label.
     *
     * @param type a value of type 'Exit.Type'
     * @return a value of type 'Exit'
     */
    public Exit getExit (Exit.Type type)
    {
        return getExit(type, Exit.Tag.NOLABEL);
    }

    /**
     * Makes a new {@link Exit} for this component.  Each {@link Exit} in
     * a Component must have a unique {@link Exit.Type Type} and label
     * String pair.  The {@link Latency} of the new {@link Exit} will
     * be {@link Latency#ZERO}.
     *
     * @param dataCount the number of data {@link Bus Buses} on the exit
     * @param type type type of the exit
     * @param label the lable of the exit
     */
    public Exit makeExit (int dataCount, Exit.Type type, String label)
    {
        Exit exit = createExit(dataCount, type, label);
        exit.setLatency(Latency.ZERO);
        assert !exits.containsKey(exit.getTag()) : "Duplicate Exit tag: " + exit.getTag();
        exits.put(exit.getTag(), exit);
        return exit;
    }

    /**
     * Makes a new unlabeled {@link Exit} for this component.  The {@link Latency}
     * of the new {@link Exit} will be {@link Latency#ZERO}.
     *
     * @param dataCount the number of data {@link Bus Buses} on the exit
     * @param type type type of the exit
     */
    public Exit makeExit (int dataCount, Exit.Type type)
    {
        return makeExit(dataCount, type, Exit.Tag.NOLABEL);
    }

    /**
     * Removes a specified {@link Exit} from this component.
     */
    public void removeExit (Exit exit)
    {
        assert exits.containsKey(exit.getTag());
        exits.remove(exit.getTag());
    } 

    /**
     * change the type of an exit by creating a new exit, copying the buses/dependencies that feed it, and removing the original
     * @param oldExit the exit to change
     * @param newType the type to change to 
     */
    public void changeExit (Exit oldExit, Exit.Type newType)
    {
        final Exit newExit=makeExit(oldExit.getDataBuses().size(), newType);
        final OutBuf newOutBuf=(OutBuf) newExit.getPeer();

        final OutBuf oldOutBuf=(OutBuf) oldExit.getPeer();

        List oldEntryList=oldOutBuf.getEntries();
        // iterate over old outbuf's entries
        for (Iterator entryIter=oldEntryList.iterator(); entryIter.hasNext();)
        {
            final Entry oldEntry=(Entry) entryIter.next();
            final Entry newEntry=newOutBuf.makeEntry(oldEntry.getDrivingExit());
            
            // iterate over the entry's ports & "clone" the dependencies
            Iterator newPortIterator=newEntry.getPorts().iterator();
            for (Iterator oldPortIterator=oldEntry.getPorts().iterator();
                 oldPortIterator.hasNext();)
            {
                final Port oldPort=(Port) oldPortIterator.next();
                final Port newPort=(Port) newPortIterator.next();
                for (Iterator dependencyIterator=new ArrayList(oldEntry.getDependencies(oldPort)).iterator(); 
                     dependencyIterator.hasNext();)
                {
                    Dependency oldDep=(Dependency) dependencyIterator.next();
                    Dependency newDep=null;
                    
                    if (oldDep instanceof ClockDependency)
                    {
                        newDep=new ClockDependency(oldDep.getLogicalBus());
                    }
                    else if (oldDep instanceof ControlDependency)
                    {
                        newDep=new ControlDependency(oldDep.getLogicalBus());
                    }
                    else if (oldDep instanceof DataDependency)
                    {
                        newDep=new DataDependency(oldDep.getLogicalBus());
                    }
                    else
                    {
                        assert oldDep instanceof ResetDependency : "bad dependency class: "+oldDep.getClass().getName();
                        newDep=new ResetDependency(oldDep.getLogicalBus());
                    }

                    newEntry.addDependency(newPort, newDep);
                    
                    oldDep.zap();
                }
            }
        }
        // Copy the attributes from old exit to new exit.
        newExit.copyAttributes(oldExit);
        
        removeExit(oldExit);
    }
    
    /**
     * Adds an unlabeled {@link Exit#DONE DONE} {@link Exit} for this component.
     * The {@link Latency} of the new {@link Exit} will be {@link Latency#ZERO}.
     *
     * @param dataCount the number of data {@link Bus Buses} on the exit
     */
    public Exit makeExit (int dataCount)
    {
        return makeExit(dataCount, Exit.DONE);
    }

    public boolean removeDataPort (Port port)
    {
        if (dataPorts.remove(port) || thisPort == port)
        {
            if (thisPort == port)
            {
                thisPort = null;
            }
            
            if (port.getPeer() != null)
            {
                Bus peer = port.getPeer();
                peer.setPeer(null);
                port.setPeer(null);
                peer.getOwner().getOwner().removeDataBus(peer);
            }

            removeDependencies(port);
            return true;
        }
        return false;
    }

    protected void removeDependencies (Port port)
    {
        for (Iterator iter = getEntries().iterator(); iter.hasNext();)
        {
            final Entry entry = (Entry)iter.next();
            for (Iterator iter2 = new ArrayList(entry.getDependencies(port)).iterator();
                 iter2.hasNext();)
            {
                ((Dependency)iter2.next()).zap();
            }
        }
    }

    /**
     * Attempts to remove the given {@link Bus} from this component.
     *
     * @param bus the bus to remove
     * @return true if the bus was removed.
     */
    public boolean removeDataBus (Bus bus)
    {
        for (Iterator iter = getExits().iterator(); iter.hasNext();)
        {
            Exit exit = (Exit)iter.next();
            if (exit.removeDataBus(bus))
            {
                return true;
            }
        }
        return false;
    }
    

    /**
     * Makes a new data port for this component.
     *
     * @return the new Port
     */
    public Port makeDataPort ()
    {
        return makeDataPort(Component.NORMAL);
    }
    
    /**
     * Makes a new data port for this component, and tags it
     *
     * @param type type of port - usually Component.SIDEBAND
     *
     * @return the new Port
     */
    public Port makeDataPort (Type tag)
    {
        Port port = new Port(this);
        port.tag(tag);
        port.setUsed(true); // ABK -- data ports should always be used.
        if (this.dataPorts == Collections.EMPTY_LIST)
        {
            this.dataPorts = new LinkedList<Port>();
        }
        this.dataPorts.add(port);
        return port;
    }

    public Port makeThisPort ()
    {
        assert getThisPort() == null : "Cannot create 'this' port multiple times";
        Port port = new Port(this);
        this.thisPort = port;
        port.setUsed(true);
        return port;
    }
    

    /**
     * Gets the {@link Module Module} in which this Component
     * resides.
     *
     * @return the module, or null if there is none
     */
    public Module getOwner ()
    {
        return owner;
    }

    /**
     * Gets the Entries for this Component.
     *
     * @return a collection of {@link Entry Entry} objects; each
     * describes a possible set of inputs to this component's Ports
     */
    public List<Entry> getEntries ()
    {
        return entries;
    }

    /**
     * Gets the main {@link Entry} of this component.
     *
     * @return if there is one entry, return it; else return null
     */
    public Entry getMainEntry ()
    {
        return entries.size() == 1 ? (Entry)entries.get(0) : null;
    }

    /**
     * Create an entry with a specified driving exit
     *
     * @param drivingExit driving exit; can be null
     * @return a value of type 'Entry'
     */
    public Entry makeEntry (Exit drivingExit)
    {
        Entry entry = new Entry(this, drivingExit);
        entries.add(entry);
        return(entry);
    }

    /**
     * Convenience method, make an entry wioth no driving exit
     *
     * @return a value of type 'Entry'
     */
    public Entry makeEntry ()
    {
        return makeEntry(null);
    }


    /**
     * Removes an Entry from this component and decimates it.
     *
     * @param entry the Entry to remove
     */
    public void removeEntry (Entry entry)
    {
        if (entries.remove(entry))
        {
            entry.decimate();
        }
    }

    /**
     * Tests whether this component requires a connection to its
     * <em>go</em> {@link Port} in order to commence processing.
     */
    public boolean consumesGo ()
    {
        return false;
    }

    /**
     * Tests whether this component produces a signal on the
     * done {@link Bus} of each of its {@link Exit Exits}.
     *
     * @see Component#isDoneSynchronous()
     */
    public boolean producesDone ()
    {
        return false;
    }

    /**
     * Tests whether the done signal, if any, produced by this
     * component (see {@link Component#producesDone()}) is
     * synchronous or not.  A true value means that the done signal
     * will be produced with the clock and no earlier than the go
     * signal is asserted.
     *
     * @see Component#producesDone()
     */
    public boolean isDoneSynchronous ()
    {
        return false;
    }

    /**
     * Tests whether this component requires a connection to
     * its clock {@link Port}.
     */
    public boolean consumesClock ()
    {
        return false;
    }

    /**
     * Tests whether this component requires a connection to
     * its reset {@link Port}.  By default, returns the
     * value of {@link Component#consumesClock()}.
     */
    public boolean consumesReset ()
    {
        return false;
    }
    
    /**
     * Tests whether this component is a pass through component which
     * means all driven ports of this component's output bus do not
     * use this component's output bus as the source bus.
     * 
     * @return true if none of the driven port's value bit use the output bus
     *         as it's source bus, else return false.
     */
    public boolean isPassThrough ()
    {
        boolean isPassThrough = true;
        
        for (Iterator exitIter = getExits().iterator(); exitIter.hasNext();)
        {
            Exit exit = (Exit)exitIter.next();
            for (Iterator dataBusIter = exit.getDataBuses().iterator(); dataBusIter.hasNext();)
            {
                Bus dataBus = (Bus)dataBusIter.next();
                for (Iterator portIter = dataBus.getPorts().iterator(); portIter.hasNext();)
                {
                    Port port = (Port)portIter.next();
                    int size = port.getValue().getSize();
                    for (int i = 0; i < size; i++)
                    {
                        Bit bit = port.getValue().getBit(i);
                        Bit invertedBit = bit.getInvertedBit();
                        Bus sourceBus = (invertedBit == null) ? bit.getOwner() : invertedBit.getOwner();
                        if ((sourceBus == dataBus))
                        {
                            isPassThrough = false;
                            break;
                        }
                    }
                }
            }
        }
        
        return isPassThrough;
    }
    
    /**
     * Tests whether or not the timing of this component can be balanced
     * during scheduling.  That is, can all of the execution paths through
     * the component be made to complete in the same number of clocks. Note
     * that this property is based only upon the type of this component
     * and any components that it may contain.
     *
     * @return true by default; overridden by subclasses
     */
    public boolean isBalanceable ()
    {
        return true;
    }

    /**
     * Tests whether this component resides in the containment
     * hierarchy of a given Module.
     *
     * @param module the module whose hierarchy is to be searched
     * @return true if this component is contained within the
     *         module's hierarchy, false otherwise
     */
    public boolean isDescendantOf (Module module)
    {
        Module parent = getOwner();
        if (parent == null || module == null || this == module)
        {
            return false;
        }
        else if (parent == module)
        {
            return true;
        }
        else
        {
            return parent.isDescendantOf(module);
        }
    }

    /**
     * Gets the ancestors of this component from top to bottom.
     *
     * @return a list of Modules, ending with the parent of
     *         this component, if any
     */
    public List getLineage ()
    {
        List list = null;
        Module parent = getOwner();
        if (parent == null)
        {
            list = new LinkedList();
        }
        else
        {
            list = parent.getLineage();
            list.add(parent);
        }
        return list;
    }

    /**
     * Gets the ancestors of this component from top to bottom,
     * starting with a specified ancestor.
     *
     * @param ancestor the ancestor from which to trace down to
     *          this component
     * @return a list of Modules, starting with the given ancestor
     *         and ending with the parent of this component, if any;
     *         if the specified ancestor is invalid, the returned
     *         list will be empty
     */
    public List getLineage (Module ancestor)
    {
        List lineage = getLineage();
        int index = lineage.indexOf(ancestor);
        if (index != -1)
        {
            return lineage.subList(index, lineage.size());
        }
        return Collections.EMPTY_LIST;
    }

    /**
     * Gets the lowest level Module that is an ancestor
     * of both this component and a given component.
     *
     * @return the common ancestor, or null if there is none
     */
    public Module getCommonAncestor (Component component)
    {
        Iterator iter = getLineage().iterator();
        Iterator otherIter = component.getLineage().iterator();

        Module commonAncestor = null;
        while (iter.hasNext() && otherIter.hasNext())
        {
            Module ancestor = (Module)iter.next();
            Module otherAncestor = (Module)otherIter.next();
            if (ancestor == otherAncestor)
            {
                commonAncestor = ancestor;
            }
            else
            {
                break;
            }
        }
        return commonAncestor;
    }

    /**
     * Gets the resources accessed by or within this component.
     *
     * @return a collection of {@link Resource}
     */
    public Collection getAccessedResources ()
    {
        return Collections.EMPTY_LIST;
    }

    /**
     * Disconnects this component by removing its dependencies, and disconnects
     * all its ports from their dependencies and buses from their dependents
     */
    public void disconnect()
    {
        for(Iterator eiter = new ArrayList(getEntries()).iterator(); eiter.hasNext();)
        {
            removeEntry((Entry)eiter.next());
        }
        for(Iterator piter = getPorts().iterator(); piter.hasNext();)
        {
            ((Port)piter.next()).disconnect();
        }
        //         for(Iterator biter = getBuses().iterator(); biter.hasNext();)
        //         {
        //             Bus bus = (Bus)biter.next();
        //             bus.clearDependents();
        //             bus.disconnect();
        //         }
        for (Iterator exitIter = getExits().iterator(); exitIter.hasNext();)
        {
            ((Exit)exitIter.next()).disconnect();
        }
    }

    /**
     * Gets the latency of this component.
     *
     * @return the latency of the main exit
     */
    public Latency getLatency ()
    {
        //return getMainExit().getLatency();
        Exit doneExit = getExit(Exit.DONE);
        if (_lim.db && doneExit == null) _lim.ln("NO EXITS ON " + this + " RETURNING LATENCY.ZERO");
        return doneExit != null ? doneExit.getLatency():Latency.ZERO;
        //return getExit(Exit.DONE).getLatency();
    }

    /**
     * Sets the {@link Module Module} in which this Component
     * resides.
     *
     * @param module the module, or null if there is none
     */
    public void setOwner (Module owner)
    {
        this.owner = owner;
    }

    /**
     * Prints out to the 'info' printstream the location of this
     * component in the method/function call hierarchy.
     */
    public void printCallHierarchy ()
    {
        List callHier = this.getCallHierarchy();
        getGenericJob().info("Method/Function call hierarchy:");
        String space = "  ";
        for (Iterator iter = callHier.iterator(); iter.hasNext();)
        {
            getGenericJob().info(space + iter.next());
            space += "  ";
        }
    }
    
    /**
     * Returns a List of String objects that indicate the method call
     * hierarchy used to reach this component starting from the entry
     * method and terminating at the method which actually contains
     * this component. If at any point a procedure has multiple calls
     * to it (or 0 calls to it) or if we end up in an infinite loop
     * the List will contain only "Traceback failed". 
     *
     * @return a 'List' of String objects
     */
    public List getCallHierarchy ()
    {
        // Assemble a List of the method call hierarchy to get to this
        // component. 
        List callHier = new ArrayList();

        // Protect against infinite looping
        int infiniteLoopCount = 0;
        final int MAX_COUNT = 10000;

        Component owner = this.getOwner();
        while (owner != null && infiniteLoopCount < MAX_COUNT)
        {
            infiniteLoopCount++;
            Component nextOwner = owner.getOwner();
            if (owner instanceof Block)
            {
                Block block = (Block)owner;
                Procedure proc = block.getProcedure();
                if (proc != null)
                {
                    callHier.add(0, proc.getName());
                    Collection calls = proc.getCalls();
                    if (calls.size() != 1)
                    {
                        return Collections.singletonList("Traceback failed.");
                    }
                    else
                    {
                        nextOwner = (Component)calls.iterator().next();
                    }
                }
            }
            owner = nextOwner;
        }
        
        if (infiniteLoopCount >= MAX_COUNT)
            return Collections.singletonList("Traceback failed.");
        
        return callHier;
    }

    public String toString ()
    {
        return ID.glob(this);
    }

    public String show ()
    {
        return show(false);
    }
    
    public String show (boolean verbose)
    {
        String ret = toString();
        for (Iterator iter = getPorts().iterator(); iter.hasNext();)
        {
            Port port = (Port)iter.next();
            String value = port.getValue() == null ? "null":port.getValue().debug();
            String val = verbose ? port.toString() + "(" + value + ")":port.toString();
            if (port == getGoPort())
                ret = ret + " go:" + val;
            else if (port == getClockPort())
                ret = ret + " ck:" + val;
            else if (port == getResetPort())
                ret = ret + " rs:" + val;
            else
                ret = ret + " p:" + val;
        }
        for (Iterator iter = getExits().iterator(); iter.hasNext();)
        {
            Exit exit = (Exit)iter.next();
            for (Iterator busIter = exit.getBuses().iterator(); busIter.hasNext();)
            {
                Bus bus = (Bus)busIter.next();
                String value = bus.getValue() == null ? "null":bus.getValue().debug();
                String val = verbose ? bus.toString() + "(" + value + ")":bus.toString();
                if (bus == exit.getDoneBus())
                    ret = ret + " done:" + val;
                else
                    ret = ret + " data:" + val;
            }
        }
        
        return ret;
    }

    public String showOwners ()
    {
        String ret = "";
        Component owner = getOwner();
        while (owner != null)
        {
            ret += owner;
            owner = owner.getOwner();
        }
        return ret;
    }
    
    public String cpDebug (boolean verbose)
    {
        String ret = toString();
        for (Iterator iter = getDataPorts().iterator(); iter.hasNext();)
        {
            Port p = (Port)iter.next();
            if (verbose)
                ret += p.getValue() == null ? " n":" " + p.getValue().bitSourceDebug();
            else
                ret += p.getValue() == null ? " n":" " + p.getValue().getSize();
        }
        for (Iterator iter = getExits().iterator(); iter.hasNext();)
        {
            Exit exit = (Exit)iter.next();
            ret += " :";
            for (Iterator busIter = exit.getDataBuses().iterator(); busIter.hasNext();)
            {
                Bus bus = (Bus)busIter.next();
                if (verbose)
                    ret += bus.getValue() == null ? " n":" " + bus.getValue().bitSourceDebug();
                else
                    ret += bus.getValue() == null ? " n":" " + bus.getValue().getSize();
            }
        }
        return ret;
    }

    /**
     * Returns a deep cloned copy of this Component in which all
     * fields <u>except</u> the owner have been
     * cloned.  Note that the entry list of the clone is left empty.
     * If the {@link CloneListener} has been set, its method
     * {@link CloneListener#setCloneMap} will be called
     * to inform if of the cloning.  The clone's {@link CloneListener} will
     * be null.
     */
    public Object clone () throws CloneNotSupportedException
    {
        Component clone = (Component)super.clone();       
        
        /*
         * Don't assume the clone will reside inside the same
         * Module.  Wait until it's added to something.
         */
        clone.owner = null;
        
        /*
         * It's OK to let the model object go through.
         */
        clone.modelObject = modelObject;

        /*
         * Duplicate the Ports.
         */
        clone.clockPort = clonePort(clone, clockPort);
        clone.resetPort = clonePort(clone, resetPort);
        clone.goPort = clonePort(clone, goPort);
        clone.thisPort = null;
        if (getThisPort() != null)
        {
            clone.makeThisPort().copyAttributes(getThisPort());
        }
        clone.dataPorts = new LinkedList();
        for (Iterator iter = dataPorts.iterator(); iter.hasNext();)
        {
            Port port = (Port)iter.next();
            Port clonePort = clone.makeDataPort();
            clonePort.copyAttributes(port);
        }

        /*
         * Leave the creation of Entrys to the caller.
         */
        clone.entries = new LinkedList<Entry>();

        /*
         * Duplicate the Exits.
         */
        clone.exits = new LinkedHashMap();
        for (Iterator iter = exits.entrySet().iterator(); iter.hasNext();)
        {
            Map.Entry mapEntry = (Map.Entry)iter.next();
            Exit exit = (Exit)mapEntry.getValue();
            cloneExit(clone, exit);
        }

        /*
         * Identifiers.
         */
        ID.copy(this, clone);
        clone.setOptionLabel(getOptionLabel());
        
        /*
         * Notify the listeners.
         */
        notifyCloneListeners(Collections.singletonMap(this, clone));

        return clone;
    }

    /**
     * Finds the clone of an {@link Exit}.
     *
     * @param exit the exit from an original component in the clone map
     * @param cloneMap a map of original components to cloned components
     */
    static Exit getExitClone (Exit exit, Map cloneMap)
    {
        final Component componentClone = (Component)cloneMap.get(exit.getOwner());
        assert componentClone!=null: "Unable to find clone map entry for: "+exit.getOwner();
        return componentClone.getExit(exit.getTag());
    }

    /**
     * Finds the clone of a {@link Bus}.
     *
     * @param bus the bus from an original component in the clone map
     * @param cloneMap a map of original components to cloned components
     */
    protected static Bus getBusClone (Bus bus, Map cloneMap)
    {
        final Exit exit = bus.getOwner();
        final Exit exitClone = getExitClone(exit, cloneMap);
        if (bus == exit.getDoneBus())
        {
            return exitClone.getDoneBus();
        }
        else
        {
            final int index = exit.getDataBuses().indexOf(bus);
            return (Bus)exitClone.getDataBuses().get(index);
        }
    }

    /**
     * Constructs and returns a new {@link Exit} for this component, but does
     * no other modification of this component's state.  Subclasses may override
     * to specialize the behavior of their exits.
     *
     * @param dataCount the number of data {@link Bus Buses} on the exit
     * @param type type type of the exit
     * @param label the lable of the exit
     * @return the new exit, which will be added to the list for this component
     */
    protected Exit createExit (int dataCount, Exit.Type type, String label)
    {
        return new Exit(this, dataCount, type, label);
    }

    /**
     * Calls {@link CloneListener#setCloneMap(Map)} on all listeners.
     */
    protected void notifyCloneListeners (Map cloneMap)
    {
        for (Iterator iter = getCloneListeners().iterator(); iter.hasNext();)
        {
            ((CloneListener)iter.next()).setCloneMap(cloneMap);
        }
    }

    /**
     * Creates a clone {@link Port}.
     *
     * @param clone the component for which the new port is to be created
     * @param port the port of this component whose primitive attributes
     *          are copied to the new port; these include the isUsed flag
     *          and the tag type
     */
    private Port clonePort (Component clone, Port port)
    {
        final Port clonePort = new Port(clone);
        clonePort.copyAttributes(port);
        return clonePort;
    }

    /**
     * Clones an {@link Exit} and copies its primitive attributes.
     * Should be overridden by any subclass that also overrides
     * {@link Component#makeExit(int,Exit.Type,String)}.
     *
     * @param clone the component on which the new exit should be
     *          created
     * @param exit the exit whose attributes are copied to the
     *          new exit
     */
    protected void cloneExit (Component clone, Exit exit)
    {
        final Exit cloneExit = clone.makeExit(
            exit.getDataBuses().size(),
            exit.getTag().getType(),
            exit.getTag().getLabel());
        cloneExit.copyAttributes(exit);
    }

    /**
     * Copies the {@link Port} and {@link Exit} attributes from
     * a given component using {@link Port#copyAttributes(Port)} and
     * copies exit/bus attributes.
     */
    protected void copyComponentAttributes (Component clone)
    {
        clone.getClockPort().copyAttributes(getClockPort());
        clone.getResetPort().copyAttributes(getResetPort());
        clone.getGoPort().copyAttributes(getGoPort());
        List thisPorts = getDataPorts();
        List clonePorts = clone.getDataPorts();
        for (int i=0; i < thisPorts.size(); i++)
        {
            ((Port)clonePorts.get(i)).copyAttributes((Port)thisPorts.get(i));
        }
        
        for (Iterator iter = getExits().iterator(); iter.hasNext();)
        {
            Exit thisExit = (Exit)iter.next();
            Exit cloneExit = clone.getExit(thisExit.getTag());
            cloneExit.copyAttributes(thisExit);
        }

        ID.copy(this, clone);
    }
    
    public Engine getEngine ()
    {
        return EngineThread.getEngine();
    }
    
    public GenericJob getGenericJob ()
    {
        return EngineThread.getGenericJob();
    }
    
    /**
     * Gets the Configurable parent (according to scope rules)
     * of this Component.
     *
     * @return the Configurable parent
     */
    public Configurable getConfigurableParent()
    {
        return getOwner();
    }
     
    public SearchLabel getSearchLabel()
    {               
        //if there is no owner, return the CodeLabel.UNSCOPED label
        if (getOwner() == null)
        {
            return CodeLabel.UNSCOPED;
        }
        //if there is an owner, return its searchlabel.
        return getOwner().getSearchLabel();      
    }
    
    /** The label, if any, with which this component has been tagged. This
     * is used to look-up an approptiate OptionDB.
     */
    String odbLabel;
     
    /**
     * Sets the label to use for OptionDB look-up.
     *
     * @param label the code-label
     */
    public void setOptionLabel(String label)
    {
        //if(label != null)
        //  System.out.println("Component.java : Setting odbLabel to : " + label);
        //else
        //  System.out.println("Component.java : Setting odbLabel to NULL");
        this.odbLabel = label;    
    }
    
    /**
     * Gets the OptionDB label.
     */
    public String getOptionLabel()
    {
        //if(odbLabel != null)
        //  System.out.println("Component.java: CurrentValue for odbLabel is " + odbLabel);
        //else
        //  System.out.println("Component.java: ODBLabel is NULL!!!!");
        return odbLabel;
    }   
    
    /**
     * Gets the log-base-2 of a double as an integer.  Used for determining the number
     * of 2-input gates needed to handle a given number of inputs.
     * 
     * @param d the operand for the log operation
     */
    protected static int log2 (double d)
    {
        return (d == 0.0) ? (int)0 : (int)Math.ceil(Math.log(d)/LN_2);
    }
    
    /**
     * Set this Component to be non-removable
     */
    public void setNonRemovable()
    {
        this.nonRemovable = true;
    }
    
    /**
     * @return true if this Component is non-removable
     */
    public boolean isNonRemovable()
    {
        return this.nonRemovable;
    }

    /**
     * Returns true if this component is a type of {@link Gateway},
     * which is usefull to know since gateways may be points of
     * implicit feedback in a design if the accessed memory is both
     * read and written within a modules hierarchy.
     */
    public boolean isGateway ()
    {
        return false;
    }

    /**
     * Returns true if this Component is a Constant, even if it is a
     * deferred constant that has not yet been locked down.
     */
    public boolean isConstant ()
    {
        return false;
    }

    /**
     * Adds the specified {@link Attribute} to this component.
     *
     * @param attribute a value of type 'Attribute'
     */
    public void addAttribute (Attribute attribute)
    {
        if (this.attributes.size() == 0)
            this.attributes = new ArrayList(5);
        this.attributes.add(attribute);
    }

    /**
     * Retrieves the List of {@link Attribute Attributes} associated
     * with this component that should be written out into the
     * implementation HDL.
     *
     * @return a 'List' of {@link Attribute Attributes}
     */
    public List<Attribute> getAttributes ()
    {
        return this.attributes;
    }

    public void debugPushValuesForward ()
    {
        System.out.print(this + ", FORWARD ==> ");
        for (Iterator iter = getDataPorts().iterator(); iter.hasNext();)
        {
            System.out.print("portValue: ");
            Value portValue = ((Port)iter.next()).getValue();
            for (int i = portValue.getSize()-1; i >= 0; i--)
            {
                System.out.print(portValue.getBit(i));
            }
            System.out.print(", ");
        }
        Value resultValue = getDataBus(this).getValue();
        System.out.print("resultValue: ");
        for (int i = resultValue.getSize()-1; i >= 0; i--)
        {
            System.out.print(resultValue.getBit(i));
        }
        System.out.println();
    }
    
    public void debugPushValuesBackward ()
    {
        Value resultValue = getDataBus(this).getValue();
        System.out.print("    " + this + ", BACKWARD <== resultValue: ");
        for (int i = resultValue.getSize()-1; i >= 0; i--)
        {
            System.out.print(resultValue.getBit(i));
        }
        for (Iterator iter = getDataPorts().iterator(); iter.hasNext();)
        {
            System.out.print(", portValue: ");
            Value portValue = ((Port)iter.next()).getValue();
            for (int i = portValue.getSize()-1; i >= 0; i--)
            {
                System.out.print(portValue.getBit(i));
            }
        }
        System.out.println();
    }

    /**
     * Gets the components which have a logical dependency upon this bus.
     * Located here because current
     *
     * @param bus the bus whose logical dependents are collected
     * @return a list of logically dependency {@link Component Components}
     */
    public static List getDependentComponents (Bus bus)
    {
        final List list = new LinkedList();
        for (Iterator iter = bus.getLogicalDependents().iterator(); iter.hasNext();)
        {
            final Dependency dependency = (Dependency)iter.next();
            list.add(dependency.getPort().getOwner());
        }
        return list;
    }
}


