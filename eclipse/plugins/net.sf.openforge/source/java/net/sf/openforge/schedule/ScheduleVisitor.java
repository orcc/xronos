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
import net.sf.openforge.app.GenericJob;
import net.sf.openforge.app.OptionRegistry;
import net.sf.openforge.lim.*;
import net.sf.openforge.lim.io.*;
import net.sf.openforge.lim.memory.*;
import net.sf.openforge.lim.op.*;
import net.sf.openforge.lim.util.OrderFinder;
import net.sf.openforge.schedule.block.*;
import net.sf.openforge.schedule.loop.LoopFlopAnalysis;


/**
 * ScheduleVisitor performs the actual work of scheduling the internal LIM
 * of each {@link Module} based upon the properties of its {@link Component Components}.
 *
 * @version $Id: ScheduleVisitor.java 562 2008-03-17 21:33:12Z imiller $
 */
public class ScheduleVisitor extends DefaultVisitor 
{
    private static final String _RCS_ = "$Rev: 562 $";

    /** True if balanced scheduling should be attempted */
    private final boolean isBalancing;

    /**
     * True if go and done signals should be generated regardless of the attributes
     * of the design.
     */
    private final boolean isForcingDone;

    private final TaskCache taskCache = new TaskCache();
    
    /** True if the design has been found capable of being balanced */
    private boolean isBalanceable = false;

    private final LatencyTracker tracker = new LatencyTracker();
    
    /**
     * The process cache, used to store the current 
     */
    private ProcessCache processCache;

    public static ScheduleVisitor schedule (Visitable vis)
    {
        final GenericJob gj = EngineThread.getEngine().getGenericJob();
        final ProcessCache processCache = new ProcessCache(vis, gj);
        final boolean isForcingDone = gj.getUnscopedBooleanOptionValue(OptionRegistry.FORCE_GO_DONE);
        boolean isBalancing = gj.getUnscopedBooleanOptionValue(OptionRegistry.SCHEDULE_BALANCE);
        final boolean noBlockSched = gj.getUnscopedBooleanOptionValue(OptionRegistry.SCHEDULE_NO_BLOCK_SCHEDULING);
        if (isBalancing && !noBlockSched )
        {
            isBalancing = false;
            gj.warn("Balanced Scheduling not possible: Block based scheduling infers feedback points in implementation.");
            gj.warn("\t... Reverting to non-balanced (block based) scheduling");
        }
        final ScheduleVisitor scheduler = new ScheduleVisitor(isBalancing, isForcingDone, processCache);
        vis.accept(scheduler);
        return scheduler;
    }
    
    private ScheduleVisitor (boolean isBalancing, boolean isForcingDone, ProcessCache procCache)
    {
        super();
        this.isBalancing = isBalancing;
        this.isForcingDone = isForcingDone;
        this.processCache = procCache;
    }

    public LatencyCache getLatencyCache () 
    {
        return this.tracker;
    }

    public void visit (Task task)
    {
        taskCache.startTask(task);

        final boolean oldBalanceable = this.isBalanceable;
        
        this.isBalanceable = task.getCall().isBalanceable();
        super.visit(task);

        final Call call = task.getCall();
        call.getClockPort().setUsed(call.consumesClock());
        call.getResetPort().setUsed(call.consumesReset());
        call.getGoPort().setUsed(call.consumesGo());
        for (Iterator iter = call.getExits().iterator(); iter.hasNext();)
        {
            ((Exit)iter.next()).getDoneBus().setUsed(call.producesDone());
        }
        
        task.setBalanced(this.isBalancing && this.isBalanceable);

        taskCache.completeTask(task);

        this.isBalanceable = oldBalanceable;
    }

    public void visit (Block block)
    {
        visitComponents(block);

        scheduleInBuf(block);
        final List components = new LinkedList(block.getSequence());
        int size = components.size();
        for (Iterator iter = components.iterator(); iter.hasNext();)
        {
            final Component component = (Component)iter.next();
            schedule(component);
        }
        scheduleOutBufs(block);

        closeModule(block);
    }

    public void visit (SimplePinAccess mod)
    {
        // Doing nothing means that the SimplePinAccess will get scheduled
        // as a component due to the visit of the block that contains
        // it.  By not calling the super.visit() we will not descend
        // into the Module.  This is what we want because the Module
        // has been hand-constructed.
        
        propagateSchedulingAttributes(mod);
    }
    
    public void visit (TaskCall mod)
    {
        // If the target task has not been scheduled then jump out
        // here and schedule it so that the scheduling attributes of
        // the task call can be correctly set.
        if (!this.taskCache.isScheduled(mod.getTarget()))
        {
            mod.getTarget().accept(this);
        }
        
        // Doing nothing means that the TaskCall will get scheduled
        // as a component due to the visit of the block that contains
        // it.  By not calling the super.visit() we will not descend
        // into the Module.  This is what we want because the Module
        // has been hand-constructed.
        propagateSchedulingAttributes(mod);
    }
    
    public void visit (FifoAccess mod)
    {
        // Doing nothing means that the Fifo access will get scheduled
        // as a component due to the visit of the block that contains
        // it.  By not calling the super.visit() we will not descend
        // into the Module.  This is what we want because the Module
        // has been hand-constructed.
        
        propagateSchedulingAttributes(mod);
    }
    
    public void visit (FifoRead mod)
    {
        // Doing nothing means that the Fifo access will get scheduled
        // as a component due to the visit of the block that contains
        // it.  By not calling the super.visit() we will not descend
        // into the Module.  This is what we want because the Module
        // has been hand-constructed.
        propagateSchedulingAttributes(mod);
    }
    
    public void visit (FifoWrite mod)
    {
        // Doing nothing means that the Fifo access will get scheduled
        // as a component due to the visit of the block that contains
        // it.  By not calling the super.visit() we will not descend
        // into the Module.  This is what we want because the Module
        // has been hand-constructed.
        propagateSchedulingAttributes(mod);
    }
    

    public void visit (Branch branch)
    {
        visitComponents(branch);

        scheduleInBuf(branch);
        schedule(branch.getDecision());
        schedule(branch.getTrueBranch());
        schedule(branch.getFalseBranch());
        scheduleOutBufs(branch);

        closeModule(branch);
    }

    public void visit (Decision decision)
    {
        decision.setConsumesGo(true);
        
        visitComponents(decision);
        
        final List decisionComponents = OrderFinder.getOrder(decision);
        decisionComponents.remove(decision.getInBuf());
        decisionComponents.remove(decision.getOutBufs());
        
        scheduleInBuf(decision);
        scheduleComponents(decisionComponents);
        
        final And trueAnd = decision.getTrueAnd();
        ((Port)trueAnd.getDataPorts().get(0)).setBus(tracker.getControlBus(trueAnd.getExit(Exit.DONE)));
        tracker.defineControlBus(trueAnd.getResultBus(), tracker.getLatency(trueAnd.getExit(Exit.DONE)));
        
        final And falseAnd = decision.getFalseAnd();
        ((Port)falseAnd.getDataPorts().get(0)).setBus(tracker.getControlBus(falseAnd.getExit(Exit.DONE)));
        tracker.defineControlBus(falseAnd.getResultBus(), tracker.getLatency(falseAnd.getExit(Exit.DONE)));
        
        /*
         * Schedule the OutBufs normally using their Dependencies.
         */
        scheduleOutBufs(decision);
        
        /*
         * Regardless of what else scheduling did, force a connection from the
         * true/false OutBuf go Ports to the boolean data Buses that supply
         * their signals.
         */
        final Bus trueBus = trueAnd.getResultBus();
        final Component trueOutbuf = decision.getTrueExit().getPeer();
        trueOutbuf.getGoPort().setBus(trueBus);
        tracker.setControlBus(trueOutbuf, trueBus);
        
        final Bus falseBus = falseAnd.getResultBus();
        final Component falseOutbuf = decision.getFalseExit().getPeer();
        falseOutbuf.getGoPort().setBus(falseBus);
        tracker.setControlBus(falseOutbuf, falseBus);
        
        closeModule(decision);
    }

    public void visit (Loop loop)
    {
        fixLoopDataRegisters(loop);
        
        final List loopComponents = OrderFinder.getOrder(loop);
        loopComponents.remove(loop.getInBuf());
        loopComponents.removeAll(loop.getOutBufs());
        
        visitComponents(loop); // Sub-schedule
        
        // Initialize the latency of the feedback flop. The
        // feedback flop is the control signal for all feedback
        // data.  Because its latency actually depends on the
        // running of the loop we must pre-initialize its latency.
        if (loop.getControlRegister() != null)
        {
            tracker.defineControlBus(loop.getControlRegister().getResultBus(), Latency.ZERO);
        }
        
        scheduleInBuf(loop);
        
        // Schedule the components of the loop
        for (Iterator iter = loopComponents.iterator(); iter.hasNext();)
        {
            final Component nextComp = (Component)iter.next();
            
            // Set the control register latency to be the latest of
            // any input latency to the loop body. This is necessary
            // b/c (eg) a latched input may have pipeline register(s)
            // inserted before it. Thus they will be 'later'.  Unless
            // the control reg is at least as late as these then it
            // will NOT be the controlling input of the feedback entry
            // and the loop will not work
            if (nextComp == loop.getBody() && loop.getControlRegister() != null)
            {
                final Map latencyMap = new HashMap();
                for (Iterator entryIter = loop.getBody().getEntries().iterator(); entryIter.hasNext();)
                {
                    final Entry entry = (Entry)entryIter.next();
                    for (Iterator portIter = nextComp.getPorts().iterator(); portIter.hasNext();)
                    {
                        final Port port = (Port)portIter.next();
                        for (Iterator depIter = entry.getDependencies(port).iterator(); depIter.hasNext();)
                        {
                            final Latency lat = tracker.getLatency(((Dependency)depIter.next()).getLogicalBus());
                            // may be null if a data register input
                            if (lat != null)
                            {
                                latencyMap.put(port, lat);
                            }
                        }
                    }
                }
                assert latencyMap.size() > 0 : "No latencies to initialize loop feedback control";
                final Map latest = Latency.getLatest(latencyMap);
                assert latest.size() == 1 : "Loop has unknown latencies leading to body";
                tracker.defineControlBus(loop.getControlRegister().getResultBus(), (Latency)latest.values().iterator().next());
                // Now that the latency of the control register is
                // set, set the control register to be the control bus
                // for the data register exits.
                for (Iterator dataRegIter = loop.getDataRegisters().iterator(); dataRegIter.hasNext();)
                {
                    final Reg register = (Reg)dataRegIter.next();
                    tracker.setControlBus(register.getExit(Exit.DONE), loop.getControlRegister().getResultBus());
                }
            }
            
            schedule(nextComp);
        }
        
        if (loop.getBody() != null && loop.getBody().getFeedbackExit() != null && loop.getControlRegister() != null && !loop.getBody().isLoopFlopNeeded())
        {
            optimizeLoopFlop(loop);
        }
        
        scheduleOutBufs(loop);
        
        fixLoopLatency(loop);
        
        closeModule(loop);
    }
    
    private void fixLoopDataRegisters (Loop loop)
    {
        // FIXME! This should not have to be here...
        // The enable port of the feedback registers have no
        // dependency.  Create a control dependency based on the data
        // port.  There is a danger if this is done any earlier b/c a
        // pipeline register may have been inserted on the data path.
        // b/c the enable port is NOT the go port we could end up with
        // synchronization problems.  By doing it here, we will get
        // the enable dependency correctly reflecting the data path
        // regardless of what optimizations have been done.
        for (Iterator regIter = loop.getDataRegisters().iterator(); regIter.hasNext();)
        {
            final Reg reg = (Reg)regIter.next();
            assert reg.getEntries().size() == 1;
            final Entry entry = (Entry)reg.getEntries().get(0);
            assert entry.getDependencies(reg.getDataPort()).size() == 1;
            final Dependency dataDep = (Dependency)entry.getDependencies(reg.getDataPort()).iterator().next();
            assert entry.getDependencies(reg.getEnablePort()).size() == 0;
            entry.addDependency(reg.getEnablePort(), new ControlDependency(dataDep.getLogicalBus()));
        }
    }
    
    /**
     * Special handling of setting the latency of the Loop outbuf
     * latency.  This method sets the output latency to be a bounded
     * latency where the bounds are min clocks to max clocks.  This is
     * determined by multiplying the number of iterations (if known)
     * by the min and max clocks that the loop body takes.
     *
     * @param loop a value of type 'Loop'
     */
    private void fixLoopLatency (Loop loop)
    {
        final LoopBody body = loop.getBody();
        for (Iterator iter = loop.getOutBufs().iterator(); iter.hasNext();)
        {
            final OutBuf outbuf = (OutBuf)iter.next();

            /*
             * If this is an iterative loop, redefine the Latency of the
             * OutBufs' control Buses to be open.
             */
            if (body != null && body.getFeedbackExit() != null)
            {
                final Bus controlBus = tracker.getControlBus(outbuf);
                Latency controlLatency = tracker.getLatency(controlBus);
                final Latency old = controlLatency;

                //
                // Calculate the minimum and maximum number of clock
                // cycles that the loop may take to complete.  This is
                // accomplished by figuing out the maximum and minimum
                // number of cycles that the loop body, update
                // expression, and decision take to complete (decision
                // may take more than 0 cycles if it contains a mul,
                // div, or rem that is replaced with an iterative
                // version).  These min/max cycles are multiplied by
                // the number of iterations of the loop to give the
                // range of latencies the loop may take.  If there is
                // a break or return, then the latency of those exits
                // is 'ored' into the calculated latency as an
                // alternative 'exit' of the loop.  This gives us the
                // final bounds for the Latency of the loop.  If the
                // loop does not iterate a known number of times, then
                // we default back to how we've always calculated the
                // loops latency (controlLatency.open()).
                //
                //if (debug) System.out.println("\tdecision on how to handle: iter " + loop.isIterative() + " bounded " + loop.isBounded() + " body " + body.getBody() + " body latency " + ((body.getBody() != null) ? body.getBody().getLatency().toString():"null") + " control latency " + controlLatency);
                if (loop.isIterative()
                    && loop.isBounded()
                    && body.getBody() != null
                    && !body.getBody().getLatency().isOpen()
                    && !controlLatency.isOpen())
                {
                    final int iterations = loop.getIterations();

                    Latency fbLatency = body.getFeedbackExit().getLatency();
                    // Add one for the loop flop if loop flop is still here.
                    if (loop.getControlRegister() != null)
                    {
                        fbLatency = fbLatency.addTo(Latency.ONE);
                    }
                    
                    final Set allLatencies = new HashSet();
                    allLatencies.add(fbLatency);
                    for (Iterator exitIter = body.getBody().getExits().iterator(); exitIter.hasNext();)
                    {
                        Exit exit = (Exit)exitIter.next();
                        if (exit == body.getBody().getExit(Exit.DONE))
                        {
                            // The fb exit has the done factored in,
                            // along with the decision latency.
                            continue;
                        }
                        allLatencies.add(exit.getLatency());
                    }
                    // The key is irrelevant due to the short lifetime
                    // and limited use of this latency.
                    Latency bodyLatency = Latency.or(allLatencies, LatencyKey.BASE);

                    final int minClocks = bodyLatency.getMinClocks() * iterations;
                    final int maxClocks = bodyLatency.getMaxClocks() * iterations;
                    final Latency boundedLatency = Latency.get(minClocks, maxClocks);
                    controlLatency = boundedLatency.addTo(controlLatency);
                }
                else
                {
                    controlLatency = Latency.get(controlLatency.getMinClocks()).open(controlBus.getOwner());
                }
                tracker.defineControlBus(controlBus, controlLatency);
            }
        }
    }
    
    /*
     * Schedule a List of components
     *
     * @param componentList list of components to be scheduled
     *
     * - gandhij
     */
    private void scheduleComponents(List componentList)
    {
        Iterator iter = componentList.iterator();
        while(iter.hasNext())
        {
            Component comp = (Component)iter.next();
            schedule(comp);
        }
    }

    public void visit (WhileBody whileBody)
    {
        visitComponents(whileBody);
        
        /* get all the components in the forBody in dataFlow order */
        List whileBodyComponents = OrderFinder.getOrder(whileBody);
        
        /* remove inbufs and outbufs from the list */
        whileBodyComponents.remove(whileBody.getInBuf());
        whileBodyComponents.removeAll(whileBody.getOutBufs());
        
        scheduleInBuf(whileBody);
        scheduleComponents(whileBodyComponents);
        scheduleOutBufs(whileBody);
        
        closeModule(whileBody);
        LoopFlopAnalysis.setLoopFlopStatus(whileBody, this.tracker);
    }
    
    public void visit (UntilBody untilBody)
    {
        visitComponents(untilBody);
        
        /* get all the components in the forBody in dataFlow order */
        List untilBodyComponents = OrderFinder.getOrder(untilBody);
        
        /* remove inbufs and outbufs from the list */
        untilBodyComponents.remove(untilBody.getInBuf());
        untilBodyComponents.removeAll(untilBody.getOutBufs());
        
        scheduleInBuf(untilBody);
        scheduleComponents(untilBodyComponents);
        if ((untilBody.getDecision() != null) && (untilBody.getBody() != null))
        {
            untilBody.getDecision().getGoPort().setBus(tracker.getControlBus(untilBody.getBody().getExit(Exit.DONE)));
        }
        scheduleOutBufs(untilBody);
        
        closeModule(untilBody);
        
        LoopFlopAnalysis.setLoopFlopStatus(untilBody, this.tracker);
    }
    
    public void visit (ForBody forBody)
    {
        
        visitComponents(forBody);
        
        /* get all the components in the forBody in dataFlow order */
        List forBodyComponents = OrderFinder.getOrder(forBody);
        
        /* remove inbufs and outbufs from the list */
        forBodyComponents.remove(forBody.getInBuf());
        forBodyComponents.removeAll(forBody.getOutBufs());
        
        scheduleInBuf(forBody);
        scheduleComponents(forBodyComponents);
        scheduleOutBufs(forBody);
        closeModule(forBody);
        
        LoopFlopAnalysis.setLoopFlopStatus(forBody, this.tracker);
    }


    /**
     * Schedules the switch statment, scheduling the inbuf, switch
     * controller, all other components and lastly the outbufs.
     */
    public void visit (Switch swich)
    {
        visit((Block)swich);
    }    
    
    public void visit (Call call)
    {
        /*
         * Schedule the called Procedure with its own LatencyTracker.
         */
        final LatencyTracker outerTracker = tracker;
        //tracker = new LatencyTracker();
        call.getProcedure().accept(this);
        //tracker = outerTracker;

        /*
         * Propagate the Procedure's Exit Latencies to the Call's Exits.
         */
        for (Iterator iter = call.getExits().iterator(); iter.hasNext();)
        {
            final Exit callExit = (Exit)iter.next();
            final Exit procExit = call.getProcedureExit(callExit);
            Latency latency = procExit.getLatency();
            if (latency.isOpen())
            {
                /*
                 * If the Procedure latency is open, create a new latency for
                 * the Call that does not have a reference to a Procedure Bus.
                 */
                latency = Latency.get(latency.getMinClocks()).open(callExit);
            }
            callExit.setLatency(latency);
        }

        final Block procBody = call.getProcedure().getBody();
        if (procBody.consumesClock())
        {
            procBody.getClockPort().setUsed(true);
        }
        if (procBody.consumesReset())
        {
            procBody.getResetPort().setUsed(true);
        }
        if (procBody.consumesGo())
        {
            procBody.getGoPort().setUsed(true);
        }
        if (procBody.producesDone())
        {
            for (Iterator iter = procBody.getExits().iterator(); iter.hasNext();)
            {
                ((Exit)iter.next()).getDoneBus().setUsed(true);
            }
        }
    }

    public void visit (HeapWrite comp)
    {
        visit((Block)comp);
    }

    public void visit (HeapRead comp)
    {
        visit((Block)comp);
    }

    public void visit (ArrayWrite comp)
    {
        visit((Block)comp);
    }

    public void visit (ArrayRead comp)
    {
        visit((Block)comp);
    }
    
    public void visit (AbsoluteMemoryWrite comp)
    {
        visit((Block)comp);
    }

    public void visit (AbsoluteMemoryRead comp)
    {
        visit((Block)comp);
    }
    
    public void visit (InBuf inbuf)
    {
    }

    public void visit (OutBuf outbuf)
    {
    }


    /**
     * Schedules the inputs to a single {@link Component} and derives the
     * {@link ControlState ControlStates} of its {@link Exit Exits}.
     *
     * @param component the component to be scheduled
     * @param tracker the tracker for the current context
     */
    private void schedule (Component component)
    {
        final boolean balance = isBalancing && isBalanceable;
        final boolean stall = processCache.isCriticalStartPoint(component);
        
        /*
         * Schedule the Entries.
         */
        final List entrySchedules = new ArrayList(component.getEntries().size());
        for (Iterator iter = component.getEntries().iterator(); iter.hasNext();)
        {
            final Entry entry = (Entry)iter.next();
            entrySchedules.add(new UnbalancedEntrySchedule(entry, tracker, balance, stall));
        }
        
        UnbalancedEntrySchedule.merge(component, entrySchedules, tracker, balance);

        if (stall)
        {
            assert entrySchedules.size() < 2 : "Cannot stall at component with multiple entries";
            final Stallboard stallboard = ((UnbalancedEntrySchedule)entrySchedules.get(0)).getStallboard();
            processCache.registerStartPoint(component, stallboard);
        }
        
        /*
         * Record the ControlStates for the Component's Exits.
         */
        tracker.updateExitStates(component);

        // We register based on Dependencies so there is no need
        // to register added scheduling components.
        if (component.getOwner() instanceof Block)
        {
            processCache.getTracker(component.getOwner()).register(component);
        }

        component.postScheduleCallback(this.tracker);
    }
        
    /**
     * Initializes the tracker with a 0-latency control state for InBuf of
     * a given Module.
     */
    private void scheduleInBuf (Module module)
    {
        /*
         * Initialize the input latency and ready bus.
         */
        tracker.defineControlBus(module.getInBuf().getGoBus(), Latency.ZERO);
        processCache.getTracker(module).register(module.getInBuf());
    }

    /**
     * Schedules all the OutBufs of a Module.
     */
    private void scheduleOutBufs (Module module)
    {
        for (Iterator iter = module.getOutBufs().iterator(); iter.hasNext();)
        {
            schedule((OutBuf)iter.next());
        }
    }

    private void closeModule (Module module)
    {
        connectStalls(module);

        // If we need to stall the module (because one or more
        // contained process has no mechanism of stalling a prior
        // process), then do so here by inserting a stall board to
        // catch the GO signal and then latch all the data ports.  The
        // module is guaranteed to have a GO signal becuase it
        // contains one or more processes which will necessitate a
        // GO. 
        final Set moduleStallProcs = processCache.getTracker(module).getModuleStallProcs(module);
        if (!moduleStallProcs.isEmpty())
        {
            // First, insert latches on every data port
            Collection latches = new HashSet();
            for (Iterator iter = module.getDataPorts().iterator(); iter.hasNext();)
            {
                Bus dataBus = ((Port)iter.next()).getPeer();
                Latch latch = tracker.getLatch(dataBus, module);
                latches.add(latch);
                for (Iterator connIter = (new HashSet(dataBus.getPorts())).iterator(); connIter.hasNext();)
                {
                    Port connectionPort = (Port)connIter.next();
                    // The latch may have already been inserted into
                    // the LIM, thus do not connect the latch output
                    // back to its input!
                    if (connectionPort.getOwner() != latch)
                    {
                        connectionPort.setBus(latch.getResultBus());
                    }
                }
            }
            // Now insert the stallboard between the GO and all logic,
            // but be careful to NOT move the latches enable.  They
            // still need to be enabled by the GO.
            final Collection goTargets = new HashSet(module.getGoPort().getPeer().getPorts());
            // The module stallboard has only the GO as a non-stall input
            final Stallboard stbd = tracker.getStallboard(Collections.singleton(module.getGoPort().getPeer()), module);
            for (Iterator iter = moduleStallProcs.iterator(); iter.hasNext();)
            {
                final ModuleStallSource stallSource = (ModuleStallSource)iter.next();
                final Set stallSignals = new HashSet();
                for (Iterator stallIter = stallSource.getStallingComponents().iterator(); stallIter.hasNext();)
                {
                    final Component stallComp = (Component)stallIter.next();
                    final Bus stallBus = BlockControlSignalIdentifier.getControlSignal(stallComp, this.tracker);
                    // Use a pass through node so that it is used as
                    // the feedback point so that constant prop does
                    // not get confused about the order to run things
                    // in. 
                    final Bus fbBus = getPassthruFeedbackBus(stallComp.getOwner(), stallBus);
                    stallSignals.add(fbBus);
                }
                stbd.addStallSignals(stallSignals);
            }
            for (Iterator iter = goTargets.iterator(); iter.hasNext();)
            {
                Port goPort = (Port)iter.next();
                if (!latches.contains(goPort.getOwner()))
                {
                    goPort.setBus(stbd.getResultBus());
                }
            }
        }
        this.processCache.deleteTracker(module);
        
        updateExits(module);
        updateConnectorAttributes(module);
    }
    
    
    /**
     * During scheduling the stall signals for each process are
     * identified, now we need to actually connect them to the
     * Stallboard for the process.
     *
     * @param module a value of type 'Module'
     */
    private void connectStalls (Module module)
    {
        for (Iterator iter = processCache.getProcesses().iterator(); iter.hasNext();)
        {
            final MemProcess process = (MemProcess)iter.next();
            if (process.isProcessContext(module))
            {
                for (Iterator startPointIter = process.getStartPoints().iterator(); startPointIter.hasNext();)
                {
                    final ProcessStartPoint startPoint = (ProcessStartPoint)startPointIter.next();
                    final Stallboard stbd = startPoint.getStallPoint();
                    final Map stallBuses = new HashMap();
                    for (Iterator stallCompIter = startPoint.getStallSignals().iterator(); stallCompIter.hasNext();)
                    {
                        final Component stallComp = (Component)stallCompIter.next();
                        // Check to ensure that the stall signal is
                        // coming from a component in the same module.
                        // This MUST be guaranteed!
                        assert stallComp.getOwner() == stbd.getOwner();
                        final Bus stallBus = BlockControlSignalIdentifier.getControlSignal(stallComp, this.tracker);
                        stallBuses.put(stallBus, this.tracker.getLatency(stallBus));
                    }
                    
                    // Only connect the 'latest' stall.  Because we
                    // are in a single block, we are guaranteed that
                    // all signals are going to be activated, thus we
                    // only need the latest one.
                    final Map latestStalls = Latency.getLatest(stallBuses);
                    stbd.addStallSignals(latestStalls.keySet());
                    stbd.getOwner().addFeedbackPoint(stbd);
                }
            }
        }
    }

    /**
     * Propagates the internal Latencies of each OutBuf to its peer Exit.  Also removes
     * any unnecessary components that may have been added during scheduling, and
     * updates the scheduling attributes of the module based upon its contents.
     *
     * @param module the module whose Exits are to be updated
     * @param tracker the tracker for the module context
     * @param isControlledContext true if the OutBufs require a go Port connection
     *          due to implicit conditions within the Module
     */
    private void updateExits (Module module)
    {
        /*
         * Hardware may have been added that affects the attributes of the
         * Module (e.g., it may now need a clock).
         */
        updateSchedulingAttributes(module, true);

        /*
         * If the module produces a done signal, hook up the done Ports of all OutBufs.
         */
        if (module.producesDone())
        {
            for (Iterator iter = module.getOutBufs().iterator(); iter.hasNext();)
            {
                final OutBuf outbuf = (OutBuf)iter.next();
                outbuf.getGoPort().setBus(tracker.getControlBus(outbuf));
            }
        }

        /*
         * Propagate the OutBuf Latencies to the Exits. Create new open Latencies
         * as needed that do not refer to Buses within the Module.
         */
        for (Iterator iter = module.getExits().iterator(); iter.hasNext();)
        {
            final Exit nextExit = (Exit)iter.next();
            final Bus outbufControlBus = tracker.getControlBus(nextExit.getPeer());
            Latency exitLatency = tracker.getLatency(outbufControlBus);
            if (exitLatency.isOpen())
            {
                exitLatency = Latency.get(exitLatency.getMinClocks()).open(nextExit);
            }
//             System.out.println("setting exit " + nextExit + " of " + nextExit.getOwner() + " to " + exitLatency);
//             System.out.println("\tmin: " + exitLatency.getMinClocks() + " max: " + exitLatency.getMaxClocks());
            nextExit.setLatency(exitLatency);
        }


        /*
         * Get rid of logic that wasn't actually connected.
         */
        removeUnusedComponents(module);
    }
    
    /**
     * Calls {@link ScheduleVisitor#updateConnectorAttributes(Component)} on
     * each {@link Component} of a given {@link Module}.
     */
    private void updateConnectorAttributes (Module module)
    {
        for (Iterator iter = module.getComponents().iterator(); iter.hasNext();)
        {
            updateConnectorAttributes((Component)iter.next());
        }
    }

    /**
     * Connects clock and reset Buses to a given Component's Ports if necessary.
     * Also sets the isUsed flag of each Port and Bus according to whether or not
     * it is connected.
     */
    private void updateConnectorAttributes (Component component)
    {
        final Module owner = component.getOwner();
        final InBuf inbuf = owner.getInBuf();

        component.getClockPort().setUsed(component.consumesClock());
        if (component.consumesClock())
        {
            component.getClockPort().setBus(inbuf.getClockBus());
        }

        component.getResetPort().setUsed(component.consumesReset());
        if (component.consumesReset())
        {
            component.getResetPort().setBus(inbuf.getResetBus());
        }

        for (Iterator iter = component.getPorts().iterator(); iter.hasNext();)
        {
            final Port port = (Port)iter.next();
            if (port.isConnected())
            {
                port.setUsed(true);
                port.getBus().setUsed(true);
            }
        }

        component.getGoPort().setUsed(component.consumesGo());
        for (Iterator iter = component.getExits().iterator(); iter.hasNext();)
        {
            ((Exit)iter.next()).getDoneBus().setUsed(component.producesDone());
        }
    }

    /**
     * Visits each {@link Component} of a given {@link Module} and schedules
     * that {@link Component} internally.  Also sets the scheduling attributes
     * of the {@link Module} based upon its contents.
     *
     * @param module the modules whose components are to be visited
     */
    private void visitComponents (Module module)
    {
        /*
         * Schedule each component internally.  We will then know each
         * component's latency and other scheduling characteristics.
         */
        Collection components = new HashSet(module.getComponents());
        int size = components.size();
        for (Iterator iter = components.iterator(); iter.hasNext();)
        {
            final Component component = (Component)iter.next();
            component.accept(this);
        }

        /*
         * Do a preliminary update of the module's scheduling characteristics.
         */
        updateSchedulingAttributes(module, false);
    }


    /**
     * Update the scheduling attributes of a {@link Module} based upon its
     * components.
     *
     * @param module the module whose attributes are updated
     * @param isScheduled true if the module has already been internally
     *          scheduled, that is, the LatencyTracker has an entry latency
     *          for each of the module's components
     *
     * @see Component#consumesGo()
     * @see Component#consumesClock()
     * @see Component#consumesReset()
     * @see Component#producesDone()
     * @see Component#isDoneSynchronous()
     */
    private void updateSchedulingAttributes (Module module, boolean isScheduled)
    {
        //System.out.println("UPDATING " + module);
        /*
         * Ignore InBuf and OutBufs, since they derive their attributes from
         * the Module.
         */
        Collection components = new HashSet(module.getComponents());
        components.remove(module.getInBuf());

        /*
         * If the latency at an OutBuf isn't fixed, then the Module must
         * produce a synchronous done.
         */
        for (Iterator iter = module.getOutBufs().iterator(); iter.hasNext();)
        {
            final OutBuf outbuf = (OutBuf)iter.next();
            if (isScheduled)
            {
                /*
                 * If the module has already been scheduled, then additionally
                 * check the latencies of the OutBufs.  This is especially useful
                 * in the case of a Branch, in which each of the two paths has
                 * a different fixed latency, which results in a variable latency
                 * overall.
                 */
                if (!tracker.getLatency(outbuf).isFixed())
                {
                    module.setProducesDone(true);
                    module.setDoneSynchronous(true);
                }
            }
            components.remove(outbuf);
        }

        propagateSchedulingAttributes(module);
        
        /*
         * If for no other reason, a Module must produce done signals
         * if it has more than one Exit (else how would we know which
         * Exit was being taken?).
         */
        if (!module.producesDone())
        {
            module.setProducesDone(isForcingDone || (module.getExits().size() > 1));
        }
    }

    private static void propagateSchedulingAttributes (Module module)
    {
        Collection components = new LinkedHashSet(module.getComponents());
        components.remove(module.getInBuf());
        components.removeAll(module.getOutBufs());
        
        /*
         * Update the module based upon the rest of its components.
         */
        for (Iterator iter = components.iterator(); iter.hasNext();)
        {
            final Component component = (Component)iter.next();

            if (component.consumesGo())
            {
                module.setConsumesGo(true);
            }

            // Seperated doneSynch out from the consumesGo test
            // because (eg) combinational memory reads consume a GO to
            // generated the enable to the memory, but the DONE from
            // the component is not used.  A modules done is
            // synchronous iff something within that module generates
            // a synchronous done.  

            if (component.isDoneSynchronous())
            {
                module.setDoneSynchronous(true);
            }
            if (component.consumesClock())
            {
                module.setConsumesClock(true);
            }
            if (component.consumesReset())
            {
                module.setConsumesReset(true);
            }
            if (hasVariableLatency(component))
            {
                module.setProducesDone(true);
                module.setDoneSynchronous(true);
            }
        }

    }
    

    /**
     * Tests whether a given {@link Component} has variable {@link Latency} or
     * not.
     *
     * @param component the component whose latency is to be tested
     * @return true if all of the component's exits have the same fixed latency,
     *         false otherwise
     */
    private static boolean hasVariableLatency (Component component)
    {
        final Set exitLatencies = new HashSet();
        for (Iterator iter = component.getExits().iterator(); iter.hasNext();)
        {
            final Latency latency = ((Exit)iter.next()).getLatency();
            if (!latency.isFixed())
            {
                return true;
            }
            exitLatencies.add(new Integer(latency.getMaxClocks()));
        }
        return exitLatencies.size() > 1;
    }

    private static void removeUnusedComponents (Module module)
    {
        final LinkedList unusedQueue = new LinkedList();
        for (Iterator iter = module.getComponents().iterator(); iter.hasNext();)
        {
            final Component component = (Component)iter.next();
            if (!isUsed(component))
            {
                unusedQueue.add(component);
            }
        }

        final Collection removedComponents = new HashSet();
        while (!unusedQueue.isEmpty())
        {
            final Component component = (Component)unusedQueue.removeFirst();
            final Collection inputComponents = getInputComponents(component);
            component.disconnect();
            module.removeComponent(component);
            removedComponents.add(component);

            for (Iterator iter = inputComponents.iterator(); iter.hasNext();)
            {
                final Component inputComponent = (Component)iter.next();
                if (!isUsed(inputComponent) && !removedComponents.contains(inputComponent))
                {
                    unusedQueue.add(inputComponent);
                }
            }
        }
    }

    private static Collection getInputComponents (Component component)
    {
        final Collection inputComponents = new HashSet();
        for (Iterator iter = component.getPorts().iterator(); iter.hasNext();)
        {
            final Port port = (Port)iter.next();
            if (port.isConnected())
            {
                inputComponents.add(port.getBus().getOwner().getOwner());
            }
        }
        return inputComponents;
    }

    private static boolean isUsed (Component component)
    {
        /*
         * Don't throw away InBufs.
         */
        if (component == component.getOwner().getInBuf())
        {
            return true;
        }

        if (component instanceof TimingOp)
        {
            return true;
        }

        if (component.isNonRemovable())
        {
            return true;
        }
        
        /*
         * Dont trounce on the expected loop structure...
         */
        if (component.getOwner() instanceof Loop &&
            component == ((Loop)component.getOwner()).getInitBlock())
        {
            return true;
        }
        
        /*
         * If there is at least one connected Bus, then the component is used.
         */
        for (Iterator iter = component.getBuses().iterator(); iter.hasNext();)
        {
            final Bus bus = (Bus)iter.next();
            if (bus.isConnected())
            {
                return true;
            }
        }

        /*
         * Otherwise, if the component has a data bus that should be
         * used but isn't, then it must be unused.
         */
        for (Iterator iter = component.getExits().iterator(); iter.hasNext();)
        {
            final Exit exit = (Exit)iter.next();
            if (!exit.getDataBuses().isEmpty())
            {
                return false;
            }
        }

        /*
         * Otherwise the component must be fulfulling its purpose without
         * any output Buses.
         */
        return true;
    }

    /**
     * Removes feedback control register in loops and replaces feedback
     * data registers with Latches. This will save a clock count for each 
     * iteration.
     * Only remove the loop flop when the feedback exit has clock
     * counts greater than 0 
     *
     * @param loop a loop
     */
    private void optimizeLoopFlop (Loop loop)
    {
        final Reg controlReg = loop.getControlRegister();
        final LoopBody body = loop.getBody();

        final Bus controlBus = getPassthruFeedbackBus(loop, tracker.getControlBus(body.getFeedbackExit().getDoneBus()));
        List controlPorts = new ArrayList(controlReg.getResultBus().getPorts());
        for (Iterator iter = controlPorts.iterator(); iter.hasNext();)
        {
            final Port port = (Port)iter.next();
            port.disconnect();
            port.setBus(controlBus);
        }
        loop.removeComponent(controlReg);
        
        /* 
         * Just wire through the fb data registers since we are already 
         * replacing the data latch in a loop body with a reg.
         */
        Collection dataRegs = new ArrayList(loop.getDataRegisters());
        for (Iterator iter = dataRegs.iterator(); iter.hasNext();)
        {
            final Reg dataReg = (Reg)iter.next();
            
            for (Iterator it = new ArrayList(dataReg.getResultBus().getPorts()).iterator(); it.hasNext();)
            {
                final Port p = (Port)it.next();
                Bus fb = getPassthruFeedbackBus(loop, dataReg.getDataPort().getBus());
                p.setBus(fb);
            }
            loop.removeComponent(dataReg);
        }
    }

    private static Bus getPassthruFeedbackBus (Module context, Bus bus)
    {
        Constant zero = new SimpleConstant(0, bus.getValue().getSize(), bus.getValue().isSigned());
        OrOp or = new OrOp();
        or.getLeftDataPort().setBus(bus);
        or.getRightDataPort().setBus(zero.getValueBus());
        context.addComponent(zero);
        context.addComponent(or);
        context.addFeedbackPoint(or);
        zero.propagateValuesForward();
        or.propagateValuesForward();
        return or.getResultBus();
    }
    
}
