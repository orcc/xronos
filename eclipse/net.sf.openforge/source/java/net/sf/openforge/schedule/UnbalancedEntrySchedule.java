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

import net.sf.openforge.app.EngineThread;
import net.sf.openforge.lim.*;
import net.sf.openforge.schedule.block.ProcessTracker;
import net.sf.openforge.schedule.block._block;


/**
 * UnbalancedEntrySchedule derives the scheduled {@link Bus} for each
 * {@link Port} according to the {@link Dependency Dependencies} of a
 * single {@link Entry}.  This is accomplished in two steps.  First
 * the GO dependencies and the control bus for each data dependency
 * are analysed to determine which bus arrives 'latest'.  If one or
 * more of these buses has a {@link Latency} which is not
 * unambiguously comparable to the other buses then there may be
 * multiple potential 'latest' buses.  The collection of latest buses
 * is used to generate a control signal for the component.
 * Subsequently, all the data inputs are scheduled relative to this
 * control signal, inserting latches or enabled registers as needed to
 * preserve data validity.
 *
 * @version $Id: UnbalancedEntrySchedule.java 538 2007-11-21 06:22:39Z imiller $
 */
class UnbalancedEntrySchedule extends EntrySchedule
{
    private static final String _RCS_ = "$Rev: 538 $";

    private boolean isStallPoint = false;
    /*
     * The Stallboard created to stall this component.  The stall
     * inputs will be generated after the design is scheduled.
     */
    private Stallboard stallboard = null;

    private boolean doDebug = false;
    
    /**
     * Constructs an UnbalancedEntrySchedule for a given {@link Entry}.
     *
     * @param entry the entry to be scheduled
     * @param tracker the current {@link LatencyTracker}
     * @param isBalancing a value of type 'boolean'
     * @param stall set to true if a scheduling stall point should be
     * inserted into the control path enabling this node.
     */
    UnbalancedEntrySchedule (Entry entry, LatencyTracker tracker, boolean isBalancing, boolean stall)
    {
        super(entry, tracker, isBalancing);
//         if (entry.getOwner().debug || (entry.getOwner().getOwner() != null && entry.getOwner().getOwner().debug == true))
//             this.doDebug = true;
        
        if (this.doDebug) System.out.println("Scheduling: " + entry.getOwner() + " " + entry.getOwner().showOwners());
        this.isStallPoint = stall;
        
        if (this.doDebug) System.out.println("Generating Control");
        generateControl();
        assert getControlBus() != null : "failed to init Entry's ControlState";
        if (stall)
        {
            assert this.stallboard != null : "did not create stall hardware";
            if (_block.db) _block.ln("Inserted stallboard " + this.stallboard + " to control " + entry.getOwner());
        }
        if (this.doDebug) System.out.println("Syncing Data");
        syncDataToControl();
    }


    /**
     * Constructor for unit testing.
     */
    UnbalancedEntrySchedule ()
    {
        super();
    }

    /**
     * Returns the {@link Stallboard} created to stall this entry, or
     * null if this is not a stalled entry.  It is an error to call
     * this method on a non-stalled process.
     *
     * @return a Stallboard, may be null
     */
    public Stallboard getStallboard ()
    {
        assert this.isStallPoint : "Attempt to retrieve stall hardware from non-stalled entry";
        return this.stallboard;
    }
    
    /**
     * Determines the latency of each data dependency and each control
     * dependency and resolves this down to a set of all possible
     * latest buses.  The set of latest buses are used to generate a
     * signal capable of being used as the GO to this component by
     * getting the control bus for each latest bus from the
     * {@link LatencyTracker} and scoreboarding these.
     *
     */
    private void generateControl ()
    {
        final Component component = getEntry().getOwner();

        /*
         * Collect the latencies of the data input Buses
         */
        final List dataPorts = component.getDataPorts();
        final Map dataLatencies = new HashMap();
        for (Iterator iter = dataPorts.iterator(); iter.hasNext();)
        {
            final Port port = (Port)iter.next();
            final Collection dataDeps = getEntry().getDependencies(port);
            assert dataDeps.size() < 2 : "data dependencies: " + dataDeps.size() + " (should be < 2)";
            if (!dataDeps.isEmpty())
            {
                final Dependency dataDep = (Dependency)dataDeps.iterator().next();
                if (this.doDebug) System.out.println("\tdata dep " + dataDep);
                final Bus bus = getSatisfyingBus(dataDep);
                dataLatencies.put(tracker.getControlBus(bus), tracker.getLatency(bus));
            }
        }
        if (this.doDebug) System.out.println("\tGC dataPortLatencies: " + dataLatencies);

        /*
         * Collect the latencies of the GO dependencies
         */
        final Collection goDependencies = getEntry().getDependencies(component.getGoPort());
        final Map controlLatencies = new LinkedHashMap(goDependencies.size());
        final Set placeholderBuses = new HashSet();
        for (Iterator iter = goDependencies.iterator(); iter.hasNext();)
        {
            final Dependency goDependency = (Dependency)iter.next();
            if (this.doDebug) System.out.println("\tcontrol dep " + goDependency);
                
            // The dependency may specify a certain number of 'delay'
            // clocks which means that this entry must not execute
            // until at least the specified number of clocks after the
            // done of the dependencies logical bus, so delay the
            // logical bus by that many cycles before adding to the map.
            Bus controlBus = getSatisfyingBus(goDependency);
            if (this.doDebug) System.out.println("\tGC Controlbus: " + controlBus + " from " + controlBus.getOwner() + " of " + controlBus.getOwner().getOwner() + " lat: " + tracker.getLatency(controlBus));
            if (this.doDebug && goDependency.getDelayClocks() > 0) System.out.println("\tGC delaying: by " + goDependency.getDelayClocks());
            controlBus = tracker.delayControlBus(controlBus, component.getOwner(), goDependency.getDelayClocks());
            if (LatencyTracker.isPlaceholder(controlBus))
            {
                /*
                 * If the go state is just a placeholder, it has no effect
                 * on the start time.
                 */
                placeholderBuses.add(controlBus);
                continue;
            }
            controlLatencies.put(controlBus, tracker.getLatency(controlBus));
        }
        if (this.doDebug) System.out.println("\tGC controlLatencies: " + controlLatencies);
        
        /*
         * Always prefer the control buses over the data buses.
         * Otherwise scheduling of loop bodies will fail when there is
         * a data input from a latch.  The feedback latency is 0 and
         * the data latency is 0 and without preferring the control
         * buses we may pick the data control bus.
         */
        final Set preferred = new HashSet(controlLatencies.keySet());

        /*
         * If a control bus is contained in both maps it is guaranteed
         * to have the same latency in each map since the latencies
         * both came from the latency tracker.
         */
        final Map allLatencies = new HashMap();
        allLatencies.putAll(dataLatencies);
        allLatencies.putAll(controlLatencies);

        final Map latestLatencyMap = Latency.getLatest(allLatencies, preferred);
        if (this.doDebug) System.out.println("\tGC latestLatencies: " + latestLatencyMap);

        /*
         * Calculate the GO control bus.  If there is a latest latency, then
         * the ready bus is that of the latency's bus.  Otherwise it is the
         * scoreboard of the buses with the latest latencies.
         */
        Set latestBuses = latestLatencyMap.keySet();
        if (latestBuses.size() == 0)
        {
            // This means that there were no (non placeholder)
            // dependencies for either the GO or the data ports, so we
            // can go ahead and assume that the controlBus is the
            // non-usefull placeholder
            assert placeholderBuses.size() > 0 : "There were 0 dependencies in an entry";
            latestBuses = placeholderBuses;
        }

        if (this.isStallPoint)
        {
            this.stallboard = tracker.getStallboard(latestBuses, component.getOwner());
            this.controlBus = this.stallboard.getResultBus();
        }
        else
        {
            if (latestBuses.size() == 1)
            {
                this.controlBus = (Bus)latestBuses.iterator().next();
            }
            else
            {
                assert !isBalancing() : "balancing with variable control latencies";
                final Scoreboard scoreboard = tracker.getScoreboard(latestBuses, component.getOwner());
                this.controlBus = scoreboard.getResultBus();
            }
        }
        
        setBus(component.getGoPort(), this.controlBus);
    }
    
    
    private void syncDataToControl ()
    {
        /*
         * The goal here is to ensure that the data will be valid when
         * the GO to the component arrives.
         */

        final Component component = getEntry().getOwner();
        final Bus goBus = getControlBus();
        final Latency goLatency = tracker.getLatency(goBus);
        if (this.doDebug) System.out.println("\tSD goBus: " + goBus + " " + goBus.getOwner().getOwner() + " " + goLatency);
        if (this.doDebug) System.out.println("\tSD component: " + component.show());

        for (Iterator iter = component.getDataPorts().iterator(); iter.hasNext();)
        {
            final Port dataPort = (Port)iter.next();
            final Collection dataDeps = getEntry().getDependencies(dataPort);
            assert dataDeps.size() < 2 : "data dependencies: " + dataDeps.size() + " (should be < 2)";
            if (this.doDebug) System.out.println("\tSD port: " + dataPort);

            /*
             * Because the previous version of this class did
             * (essentially) the same thing (ignore 0 dep ports) I
             * think that this case is precluded.  ie, all ports must
             * have dependencies in all entries.
             */
            if (dataDeps.isEmpty())
                continue;
            
            final Dependency dataDep = (Dependency)dataDeps.iterator().next();
            final Bus dataBus = getSatisfyingBus(dataDep);
            final Latency dataLatency = tracker.getLatency(dataBus);
            if (this.doDebug) System.out.println("\tSD dataBus: " + dataBus + " owner " + dataBus.getOwner().getOwner() + " lat: " + dataLatency);
            
            /*
             * If the go can lag behind the data, then delay the data.
             */
            Bus scheduledDataBus = dataBus;
                
            /*
             * No need to include the test for
             * goLatency.isGE(dataLatency) because by definition that
             * should always be true because the control signal MUST
             * account for all data inputs.  The only time it is not
             * true is if they are not comparable in which case we
             * must latch the data anyway.
             */
            if (this.doDebug) System.out.println("\tSD bal: " + isBalancing() + " go.gt(data): " + goLatency.isGT(dataLatency));
            if (!goLatency.equals(dataLatency) && !ProcessTracker.isUntimed(dataBus))
            {
                if (isBalancing() && !goLatency.isOpen() && !dataLatency.isOpen())
                {
                    /*
                     * If balancing, make up the difference with a
                     * chain of registers.
                     */
                    scheduledDataBus = tracker.delayDataBus(dataBus, component.getOwner(),goLatency.getMaxClocks() - dataLatency.getMaxClocks());
                }
                else
                {
                    if (isBalancing())
                    {
                        // If we get here, that means that either the
                        // go or done latency was NOT determinate and
                        // balanced scheduling could not be
                        // accomplished.  This is the catch all to try
                        // to fix the error by inserting the latch
                        // instead.
                        EngineThread.getEngine().getGenericJob().warn("Internal Error:  Balanced scheduling failed.  Correcting schedule and continuing, but design may not be fully balanced");
                    }
                    //
                    // Otherwise, just latch or insert a reg.
                    //
                    // To prevent a combinational data path int a loop body when we do
                    // loop flop optimization, we have to check if the data latency is less
                    // than go latency. If true we need to use a Enable Register
                    // instead of a Latch.
                    if (goLatency.isGT(dataLatency))
                    {
                        Reg reg = tracker.getEnableReg(dataBus, component.getOwner());
                        scheduledDataBus = reg.getResultBus();
                    }
                    else
                    {   
                        Latch latch = tracker.getLatch(dataBus, component.getOwner());
                        scheduledDataBus = latch.getResultBus();
                    }
                }
            }
            setBus(dataPort, scheduledDataBus);
        }
    }

    private Bus getSatisfyingBus (Dependency dep)
    {
        Bus logicalBus = dep.getLogicalBus();

        if ((dep instanceof ResourceDependency.GoToGoDep) &&
            ((ResourceDependency.GoToGoDep)dep).preconditionIsValid())
        {
            return this.tracker.getControlBus(logicalBus.getOwner().getOwner());
        }
        else if (dep instanceof ControlDependency)
            return this.tracker.getControlBus(logicalBus);
        else
            return logicalBus;
    }
}
