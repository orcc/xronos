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

package net.sf.openforge.schedule.block;

import java.util.*;

import net.sf.openforge.lim.*;

/**
 * <p>ComponentProcessDeriver is a convenience class for determining
 * what {@link MemProcess} objects a given component is subject to and
 * what processes need to be stalled based on the given component.
 * There are several relevent conditions to a component that determine
 * what constraints must be imposed on scheduling.  In general a
 * component may be characterized by what processes are derived from
 * its control path and the processes that are derived from its data
 * path. Additionally a component may open or close any given process.
 * A component is defined as 'in' a process if that component has
 * dependencies for its GO port (control path) that derive from other
 * components in the process.  Additionally a component may be 'in' a
 * process if it has NO control processes, but its data flows derive
 * from nodes in a process.
 * <p>Based on the process(es) that a component is identified as being
 * 'in' as well as the processes from which a component derives its
 * input data, stall signals for one or more processes may be
 * derived.  This class simplifies and centralizes the decisions
 * related to what processes are impacted by the state of inputs for
 * the component.
 *
 *
 * <p>Created: Tue Oct 26 11:43:57 2004
 *
 * @author imiller, last modified by $Author: imiller $
 * @version $Id: ComponentProcessDeriver.java 2 2005-06-09 20:00:48Z imiller $
 */
class ComponentProcessDeriver 
{
    private static final String _RCS_ = "$Rev: 2 $";

    /** A Set of MemProcess objects identified from the components
     * from which this component derives its GO signal */
    private Set initialProcs;
    /** A Set of MemProcess objects identified from the components
     * from which this component derives its input data signals */
    private Set dataProcs;
    /** A Set of MemProcess objects that are 'opened' by this
     * component. */
    private Set openedProcs;
    /** A Set of MemProcess objects that are 'closed' by this
     * component */
    private Set closedProcs;
    /** A Set or data ports from the component that consume data that
     * is not produced by another process. */
    private Set uncontrolledDataPorts;
    
    /**
     * Uses the {@link ProcessTracker} in order to characterize the
     * relationship of the specified {@link Component} to the
     * identified {@link MemProcess} objects stored in the
     * ProcessTracker 
     *
     * @param comp a non-null {@link Component}
     * @param tracker a non-null {@link ProcessTracker}
     * @throws NullPointerException if comp or tracker are null
     */
    public ComponentProcessDeriver (Component comp, ProcessTracker tracker)
    {
        this.initialProcs = tracker.getProcs(Collections.singleton(comp.getGoPort()), false);
        this.dataProcs = new HashSet();
        this.uncontrolledDataPorts = new HashSet();
        for (Iterator iter = comp.getDataPorts().iterator(); iter.hasNext();)
        {
            final Port dataPort = (Port)iter.next();
            final Set dataPortProcs = tracker.getProcs(Collections.singleton(dataPort), true);
            this.dataProcs.addAll(dataPortProcs);
            if (dataPortProcs.isEmpty())
            {
                this.uncontrolledDataPorts.add(dataPort);
            }
        }
        this.openedProcs = new HashSet();
        this.closedProcs = new HashSet();

        // Check each process that is being tracked by the
        // ProcessTracker to see if this component is either a start
        // or end point of the process.
        for (Iterator iter = tracker.getProcesses().iterator(); iter.hasNext();)
        {
            MemProcess memProc = (MemProcess)iter.next();
            if (memProc.isStartPoint(comp))
            {
                this.openedProcs.add(memProc);
            }
            if (memProc.isEndPoint(comp))
            {
                this.closedProcs.add(memProc);
            }
        }
    }

    /**
     * Derives the Set of {@link MemProcess} objects that the
     * component is under the purview of.  This means any Process
     * which controls a node upon which the GO signal of this
     * component depends, or the Set of data processes if there are no
     * control processes.
     * <p>A component is in a process if that process is in its
     * control path (initialProcs) or the process is opened by that
     * component (opened procs).  Any processes closed by the
     * component are removed from the set of identified 'in'
     * processes.
     * <p>But, if there are no processses identified on the control
     * path, then the component is in whatever processes are defined
     * on the data ports.
     *
     * @return a non-null Set of {@link MemProcess} objects, may be
     * empty 
     */
    public Set getInProcesses ()
    {
        Set inProcesses = new HashSet();
        inProcesses.addAll(this.initialProcs);
        inProcesses.addAll(this.openedProcs);
        if (inProcesses.isEmpty())
        {
            inProcesses.addAll(this.dataProcs);
        }
        else
        {
            // Removal of closed procs is mutually exclusive from the
            // reverting to data procs.  This way any pure data flow
            // component will preserve the fact that it generated data
            // from the specified processes.
            inProcesses.removeAll(this.closedProcs);
        }
        // Save some memory since this set will probably be saved off
        // somewhere 
        if (inProcesses.isEmpty())
        {
            inProcesses = Collections.EMPTY_SET;
        }
        
        return inProcesses;
    }

    /**
     * Returns the Set of {@link MemProcess} objects that are 'closed'
     * by this component.  A component closes a process if the
     * component is one of the endpoints (ie a last access, or the
     * container of a last access) to the resource for the process.
     *
     * @return a Set of {@link MemProcess} objects.
     */
    public Set getClosedProcesses ()
    {
        return Collections.unmodifiableSet(this.closedProcs);
    }
    
    /**
     * Returns the Set of {@link MemProcess} objects that need to be
     * stalled based on the consumption of data from them by this
     * component.
     *
     * @return a non-null Set of MemProcess objects, may be null.
     */
    public Set getDataProcsToStall ()
    {
        /* At one point I believed that it was enough to stall only
         * those processes which generated data consumed by this node
         * and which were not already factored into the control of
         * this component.  However, it is possible that control wise
         * this node may exist in a given process, but that the DONE
         * from this component does not factor into the control of the
         * 'last' or closing node for the process.  Thus, this
         * component would not serve to stall the process even though
         * it is 'in' the process.  Thus we need to return all data
         * processes that are consumed here so that this component
         * factors into the stall signal(s) for those processes.
        final Set controlProcs = new HashSet();
        controlProcs.addAll(this.initialProcs);
        controlProcs.addAll(this.openedProcs);
        
        final Set uncontrolledDataProcs = new HashSet(this.dataProcs);
        uncontrolledDataProcs.removeAll(controlProcs);
        if (!uncontrolledDataProcs.isEmpty() && !controlProcs.isEmpty())
        {
            return uncontrolledDataProcs;
        }
        return Collections.EMPTY_SET;
        */
        return Collections.unmodifiableSet(this.dataProcs);
    }

    /**
     * Returns a Set of {@link MemProcess} objects that were opened by
     * this component iff the Set of initial processes (those derived
     * from the GO of the component) is empty.  This set of
     * 'uncontrolled' opened processes represents a break in the
     * reverse flow control of the design and their go signals (the
     * process GO == stallboard done) must be factored into some prior
     * logic to ensure the continuity of reverse flow control.
     *
     * @return a non-null Set of MemProcess objects, may be empty.
     */
    public Set getUncontrolledOpenProcs ()
    {
        if (this.initialProcs.isEmpty() && !openedProcs.isEmpty())
            return Collections.unmodifiableSet(this.openedProcs);

        return Collections.EMPTY_SET;
    }

    /**
     * Returns a Set of {@link Port} objects that consume
     * 'uncontrolled' data values, meaning that their values are
     * generated by logic that is not subject to stalling by another
     * process.
     * <p>As an optimization this method prunes out any ports which
     * consume untimed values as determined by the
     * {@link ProcessTracker#isUntimed} method.
     *
     * @return a Set of Port objects.
     */
    public Set getUncontrolledDataPorts ()
    {
        // Prune out of the uncontrolled ports any untimed ports.
        final Set untimedPorts = new HashSet();
        for (Iterator iter = this.uncontrolledDataPorts.iterator(); iter.hasNext();)
        {
            final Port dataPort = (Port)iter.next();
            boolean isUntimed = true;
            for (Iterator entryIter = dataPort.getOwner().getEntries().iterator(); entryIter.hasNext();)
            {
                final Entry entry = (Entry)entryIter.next();
                for (Iterator depIter = entry.getDependencies(dataPort).iterator(); depIter.hasNext();)
                {
                    final Bus depBus = ((Dependency)depIter.next()).getLogicalBus();
                    if (!ProcessTracker.isUntimed(depBus))
                    {
                        isUntimed = false;
                    }
                }
            }
            if (isUntimed)
            {
                untimedPorts.add(dataPort);
            }
        }
        final Set timedPorts = new HashSet(this.uncontrolledDataPorts);
        timedPorts.removeAll(untimedPorts);
        return timedPorts;
    }
    

    /**
     * Debug string.
     *
     * @return a non-null String
     */
    public String debug ()
    {
        String ret = "";
        ret += "\tinit  " + this.initialProcs + "\n";
        ret += "\topen  " + this.openedProcs + "\n";
        ret += "\tclose " + this.closedProcs + "\n";
        ret += "\tdata " + this.dataProcs + "\n";
        ret += "\tdataP " + this.uncontrolledDataPorts + "\n";
        return ret;
    }
    
}// ComponentProcessDeriver
