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

package net.sf.openforge.schedule;

import java.util.*;

import net.sf.openforge.lim.*;
import net.sf.openforge.lim.op.Constant;
import net.sf.openforge.lim.op.SimpleConstant;

/**
 * EntrySchedule derives the scheduled {@link Bus} for each {@link Port}
 * according to the {@link Dependency Dependencies} of a single {@link Entry}.
 *
 * @version $Id: EntrySchedule.java 2 2005-06-09 20:00:48Z imiller $
 */
class EntrySchedule
{
    private static final String _RCS_ = "$Rev: 2 $";

    /** The entry whose dependencies are being scheduled */
    protected Entry entry;

    /** The LatencyTracker to use for scheduling this entry */
    protected LatencyTracker tracker;

    /** The control bus calculated for this entry */
    protected Bus controlBus = null;

    /** Map of Port to the Bus that was scheduled for it, if any */
    private Map busMap;

    /** True if a balanced schedule is being produced */
    private boolean isBalancing;

    /**
     * Constructs an EntrySchedule for a given {@link Entry}.
     *
     * @param entry the entry to be scheduled
     * @param tracker the current {@link LatencyTracker}
     */
    EntrySchedule (Entry entry, LatencyTracker tracker, boolean isBalancing)
    {
        this.entry = entry;
        this.tracker = tracker;
        this.busMap = new HashMap(entry.getOwner().getPorts().size());
        this.isBalancing = isBalancing;
    }


    /**
     * Constructor for unit testing.
     */
    EntrySchedule ()
    {
        tracker = new LatencyTracker();
        busMap = new LinkedHashMap();
        this.isBalancing = false;
    }

    /**
     * Gets the {@link Entry} being schedule.
     */
    Entry getEntry ()
    {
        return entry;
    }

    Bus getControlBus ()
    {
        return controlBus;
    }

    /**
     * For each {@link Port} of a {@link Component}, merge and connect the
     * scheduled {@link Bus Buses} specified by one or more
     * {@link EntrySchedule EntrySchedules}.  This will also set the entry
     * {@link ControlState} for the {@link Component} in the {@link LatencyTracker}.
     *
     * @param component the component whose ports are to be connected
     * @param entrySchedules a collection of {@link EntrySchedule}, one for each
     *          {@link Entry} of the given component
     * @param tracker the current {@link LatencyTracker}
     * @param isBalancing a value of type 'boolean'
     * @return a Set of the components that were added to the
     * components owner in order to schedule that component.  The
     * collection may contain some of the same components returned by
     * scheduling of other components due to hardware caching.
     */
    static void merge (Component component, Collection entrySchedules, LatencyTracker tracker,
        boolean isBalancing)
    {
        /** Map of component's Port to the Bus that was calculated for it */
        final Map connectionMap = new HashMap();

        if (entrySchedules.isEmpty())
        {
            /*
             * No entry schedules means there were no dependencies for
             * this component.  Tie off GO and DATA ports to 0.
             */
            final Module module = component.getOwner();
            final Bus entryControlBus = tracker.getControlBus(module.getInBuf().getExit(Exit.DONE));
            tracker.setControlBus(component, entryControlBus);

            if (component.consumesGo())
            {
                final Constant goConst = new SimpleConstant(0, 1);
                module.addComponent(goConst);
                connectionMap.put(component.getGoPort(), goConst.getValueBus());
            }

            for (Iterator iter = component.getDataPorts().iterator(); iter.hasNext();)
            {
                final Port port = (Port)iter.next();
                final Constant dataConst = new SimpleConstant(0, port.getValue().getSize());
                module.addComponent(dataConst);
                connectionMap.put(port, dataConst.getValueBus());
            }
        }
        else if (entrySchedules.size() == 1)
        {
            /*
             * If there is only one Entry, then its EntrySchedule Buses can be
             * connected directly to the Component's Ports.
             */
            final EntrySchedule entrySchedule = (EntrySchedule)entrySchedules.iterator().next();
            tracker.setControlBus(component, entrySchedule.getControlBus());
            connectionMap.put(component.getGoPort(), entrySchedule.getBus(component.getGoPort()));
            for (Iterator iter = component.getDataPorts().iterator(); iter.hasNext();)
            {
                final Port port = (Port)iter.next();
                connectionMap.put(port, entrySchedule.getBus(port));
            }
        }
        else
        {
            /*
             * OR the control Buses to produce a single go Bus.
             */
            final Port goPort = component.getGoPort();
            final Map uniqueGoBuses = new LinkedHashMap();
            for (Iterator iter = entrySchedules.iterator(); iter.hasNext();)
            {
                final EntrySchedule entrySchedule = (EntrySchedule)iter.next();
                final Bus controlBus = entrySchedule.getControlBus();
                uniqueGoBuses.put(controlBus, entrySchedule);
            }
            assert uniqueGoBuses.size() == entrySchedules.size() : "non-unique go Buses " + uniqueGoBuses.size() + ":" + entrySchedules.size();
            Collection goBuses = uniqueGoBuses.keySet();

            /*
             * If balancing, delay all the go buses to the same length.
             */
            Map newBusToOldBus = null;
            if (isBalancing)
            {
                newBusToOldBus = synchronizeControlBuses(goBuses, component.getOwner(), tracker);
            }

            /* 
             * Hack: if we are not balancing, newBusToOldBus map is null so
             * map the goBuses to themselves.
             */ 
            if(newBusToOldBus == null)
            {
                newBusToOldBus = new HashMap(goBuses.size());
                for(Iterator chiter = goBuses.iterator(); chiter.hasNext();)
                {
                    Bus goBus = (Bus)chiter.next();
                    newBusToOldBus.put(goBus, goBus);
                }
            }
            final Or or = tracker.getOr(newBusToOldBus.keySet(), component.getOwner());
            final Bus orBus = or.getResultBus();
            connectionMap.put(goPort, orBus);
            tracker.setControlBus(component, orBus);

            /*
             * Merge each set of data Buses using a Mux.
             */
            for (Iterator iter = component.getDataPorts().iterator(); iter.hasNext();)
            {
                final Port dataPort = (Port)iter.next();
                final List selectBuses = new LinkedList();
                final List dataBuses = new LinkedList();
                
                /*
                 * Use the new Buses obtained from synchronizing-control-buses-step to
                 * get the Latency, but use the original Buses to get entrySchedule.
                 */
                for(Iterator newBusIter = newBusToOldBus.keySet().iterator(); newBusIter.hasNext();)
                {
                    Bus newGoBus = (Bus)newBusIter.next();

                    final Latency goLatency = tracker.getLatency(newGoBus);
                    EntrySchedule entrySchedule = (EntrySchedule)uniqueGoBuses.get(newBusToOldBus.get(newGoBus));
    
                    /*
                     * Not every Entry has to supply a dependency for every Port,
                     * although at least one Entry should.
                     */
                    Bus dataBus = entrySchedule.getBus(dataPort);
                    if (dataBus != null)
                    {
                        /*
                         * If balancing, synchronize each data Bus
                         * with its (possibly delayed) select Bus.
                         */
                        if (isBalancing)
                        {
                            final Latency dataLatency = tracker.getLatency(dataBus);
                            assert dataLatency.isFixed() : "data bus with variable latency";
                            dataBus = tracker.delayDataBus(dataBus, component.getOwner(), goLatency.getMaxClocks() - dataLatency.getMaxClocks());
                        }
                        
                        dataBuses.add(dataBus);
                        selectBuses.add(newGoBus);
                    }
                }   
                
                /*
                 * If there are two or more unique data Buses, create a Mux;
                 * else just use the one unique Bus.
                 */
                if (new HashSet(dataBuses).size() > 1)
                {
                    final Mux mux = tracker.getMux(selectBuses, dataBuses, component.getOwner());
                    connectionMap.put(dataPort, mux.getResultBus());
                }
                else
                {
                    connectionMap.put(dataPort, (Bus)dataBuses.iterator().next());
                }
            }
        }

        final Port goPort = component.getGoPort();

        /*
         * Connect the Ports to the Buses that were found for them.
         */
        if (component.consumesGo())
        {
            goPort.setBus((Bus)connectionMap.get(goPort));
        }

        for (Iterator iter = component.getDataPorts().iterator(); iter.hasNext();)
        {
            final Port dataPort = (Port)iter.next();
            dataPort.setBus((Bus)connectionMap.get(dataPort));
        }
    }

    
    /**
     * Gets the {@link Bus} that was scheduled for a given {@link Port}.
     */
    Bus getBus (Port port)
    {
        return (Bus)busMap.get(port);
    }

    /**
     * Records the {@link Bus} that was scheduled for a given {@link Port}.
     */
    void setBus (Port port, Bus bus)
    {
        busMap.put(port, bus);
    }


    protected boolean isBalancing ()
    {
        return isBalancing;
    }

    private static Map synchronizeControlBuses (Collection buses, Module module, LatencyTracker tracker)
    {
        final Map busLatencies = new HashMap();
        for (Iterator iter = buses.iterator(); iter.hasNext();)
        {
            final Bus bus = (Bus)iter.next();
            busLatencies.put(bus, tracker.getLatency(bus));
        }
        final Map maxLatencies = Latency.getLatest(busLatencies);
        assert maxLatencies.size() == 1 : "unable to balance control Buses";

        final Latency maxLatency = (Latency)maxLatencies.values().iterator().next();
        assert maxLatency.isFixed() : "unable to balance control Bus with unfixed Latency";
        final int maxClocks = maxLatency.getMaxClocks();

        Map syncedBuses = new HashMap(buses.size());
        for (Iterator iter = buses.iterator(); iter.hasNext();)
        {
            final Bus bus = (Bus)iter.next();
            final Latency latency = (Latency)busLatencies.get(bus);
            syncedBuses.put(tracker.delayControlBus(bus, module, maxClocks - latency.getMaxClocks()), bus);
        }

        return syncedBuses;
    }
}
