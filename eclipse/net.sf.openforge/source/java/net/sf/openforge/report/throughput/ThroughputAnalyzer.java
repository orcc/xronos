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

package net.sf.openforge.report.throughput;

import java.util.*;
import java.io.*;

import net.sf.openforge.lim.*;
import net.sf.openforge.lim.memory.*;
import net.sf.openforge.util.naming.*;

/**
 * ThroughputAnalyzer determines the minimum spacing between
 * assertions of GO/data to each task in order to prevent overlap of
 * data processing within the task.  There are 3 considerations which
 * factor into this spacing.  These are:
 * <ul>
 * <li>The task was balanced during scheduling
 * <li>The task contains loops
 * <li>The task contains multiple accesses to the same global
 * resource.
 * </ul>
 * If the design is not balanced during scheduling, OR contains
 * unbounded loops, then the minimum spacing is to wait until the
 * previous DONE.  If the design contains bounded loops and is
 * balanced, the spacing is the number of loop iterations times the
 * number of cycles per iteration.  If the design contains multiple
 * accesses to the same global resource the spacing is the number of
 * cycles between the first and last access to that resource.
 *
 * <p><b>NOTE, to decrease the memory requirements for this analysis,
 * it is possible that the ThroughputAnalyzer visitor and the
 * GlobalLatencyVisitor could be combined into a single visitor, in
 * which the latency of each bus is calculated, then stored in a map
 * based on which ports it connects to (ie port->latency map).  Then
 * as each component is processed, remove the entry from the map and
 * throw it away.  As each component is processed, we mark the ones we
 * are interested in.  </b>
 *
 * <p><b>NOTE.  This will not currently work with dual ported
 * memories, in which both ports of the memory are accessed from
 * within the same task, because they will look like 2 different
 * resources.  This isn't an issue for now since the only dual ported
 * memories are ROM</b>
 *
 * <p>Created: Tue Jan 21 10:23:27 2003
 *
 * @author imiller, last modified by $Author: imiller $
 * @version $Id: ThroughputAnalyzer.java 538 2007-11-21 06:22:39Z imiller $
 */
public class ThroughputAnalyzer extends DataFlowVisitor implements Visitor
{
    private static final String _RCS_ = "$Rev: 538 $";

    /** These 4 maps are all non persistent.  They are created and
     * destroyed during processing of each Task. */
    private Map latencyMap = Collections.EMPTY_MAP;
    private Map inputLatencyMap = Collections.EMPTY_MAP;
    
    /** Loop/Resource -> ThroughputLimit */
    private Map nodeToThroughputLimit = new HashMap();
    /** Resource->ThroughputLimit for tracking limits imposed by the
        address port of a memory */
    private Map addrLimit = new HashMap();

    /** Stack of Calls as we descend through the method call hierarchy
     * we push onto the stack and pop off as we come back up.  Used to
     * identify where accesses occur. */
    private Stack methodStack = new Stack();

    /** This is a Stack of loops as they are traversed, used to
     * annotate resource accesses that occur in a loop so that, if the
     * access is the first to a resource, then the loops entry latency
     * is the latency of the access, otherwise the loops exit latency
     * is the latency of the access. */
    private Stack loopStack = new Stack();
    
    /** A persitent map of Task->Set where the Set is a collection of
     * ThroughputLimit objects for that task. Used for reporting. */
    private Map taskToThroughputLimit = new HashMap();

    private static final Resource PIN_RESOURCE = new DummyPinResource();
    private static class DummyPinResource extends Resource
    {
        public int getSpacing (Referencer from, Referencer to) {return 0;}
        public int getGoSpacing (Referencer from, Referencer to) {return -1;}
        public Latency getLatency (Exit exit) {assert false;return null;}
        public String toString () {return "DUMMY_PIN_RESOURCE";}
    }
    
    public ThroughputAnalyzer ()
    {
        super();
        setRunForward(true);
    }

    /**
     * Writes out a report of all identified paths for each resource
     * encounted, on a per-task basis to the given PrintStream.
     */
    public void writeReport (PrintStream ps)
    {
        for (Iterator iter = this.taskToThroughputLimit.keySet().iterator(); iter.hasNext();)
        {
            final Task task = (Task)iter.next();
            final Set limits = (Set)this.taskToThroughputLimit.get(task);

            //String name = task.showIDLogical();
            String name = task.getCall().showIDLogical();
            
            ps.println("Throughput analysis results for task: " + name);
            
            int pathCount = 0;
            for (Iterator mapIter = limits.iterator(); mapIter.hasNext();)
            {
                ThroughputLimit limiter = (ThroughputLimit)mapIter.next();
                limiter.writeReport(ps, 0);
            }
            ps.println();
        }
    }

    public void visit (Design design)
    {
        if (_throughput.db) _throughput.d.launchXGraph(design, false);
        super.visit(design);
    }
    
    public void visit (Task task)
    {
        if (!task.isBalanced())
        {
            Latency callLatency = task.getCall().getLatency();
            if (callLatency.getMaxClocks() == Latency.UNKNOWN)
                task.setGoSpacing(Task.INDETERMINATE_GO_SPACING);
            else
                task.setGoSpacing(callLatency.getMaxClocks());
            return;
        }

        //
        // Identify the latency of each bus in the LIM as an absolute
        // value from the initial GO so we can calculate the
        // difference between the first and last access to resources.
        //
        final GlobalLatencyVisitor glv = new GlobalLatencyVisitor();
        task.accept(glv);
        this.latencyMap = glv.getLatencyMap();
        this.inputLatencyMap = glv.getInputLatencyMap();
        this.methodStack = new Stack();
        this.nodeToThroughputLimit = new HashMap();
        this.addrLimit = new HashMap();
        
        super.visit(task);

        //
        // Clear out the maps to save memory.
        //
        this.latencyMap = Collections.EMPTY_MAP;
        this.inputLatencyMap = Collections.EMPTY_MAP;
        this.methodStack = null;
        final HashSet limitSet = new HashSet(this.nodeToThroughputLimit.values());
        limitSet.addAll(addrLimit.values());
        this.taskToThroughputLimit.put(task, limitSet);
        this.nodeToThroughputLimit = Collections.EMPTY_MAP;
        this.addrLimit = Collections.EMPTY_MAP;

        //
        // Start at 0 since the 'no restrictions' case means we can
        // assert data every clock cycle.
        //
        if (_throughput.db) _throughput.d.ln(_throughput.TA, "Analyzing paths:");
        int longestPath = 0;
        for (Iterator iter = limitSet.iterator(); iter.hasNext();)
        {
            ThroughputLimit paths = (ThroughputLimit)iter.next();
            if (_throughput.db) _throughput.d.ln(_throughput.TA, "\t" + paths + " limit: " + paths.getLimit());
            final int limit = paths.getLimit();
            if (limit < 0)
            {
                longestPath = Task.INDETERMINATE_GO_SPACING;
                break;
            }
            longestPath = Math.max(longestPath, paths.getLimit());
        }
        task.setGoSpacing(longestPath);
    }

    public void visit (Call call)
    {
        methodStack.push(call);
        markCall(call);
        super.visit(call);
        methodStack.pop();
    }

    public void visit (MemoryRead comp)
    {
        markMemAccess(comp);

        // Since the array length cannot change then it is not a
        // limitation on the throughput because no one can change it.
        Module owner = comp.getOwner();
        if (owner instanceof HeapRead &&
            ((HeapRead)owner).isArrayLengthRead())
        {;}
        else
        {
            markResource(comp);
        }
        super.visit(comp);
    }

    public void visit (MemoryWrite comp)
    {
        markMemAccess(comp);
        markResource(comp);
        super.visit(comp);
    }

    public void visit (PinRead comp)
    {
        markResource(comp);
        super.visit(comp);
    }

    public void visit (PinWrite comp)
    {
        markResource(comp);
        super.visit(comp);
    }
    
    public void visit (PinStateChange comp)
    {
        markResource(comp);
        super.visit(comp);
    }

    public void visit (RegisterRead comp)
    {
        markResource(comp);
        super.visit(comp);
    }

    public void visit (RegisterWrite comp)
    {
        markResource(comp);
        super.visit(comp);
    }

    /**
     * The loop only represents a limitation if it is iterative.
     */
    public void visit (Loop loop)
    {
        if (loop.isIterative())
        {
            if (_throughput.db) _throughput.d.ln(_throughput.TA, "Pushing loop " + loop);
            this.loopStack.push(loop);
            markLoop(loop);
            
            super.visit(loop);
            
            Object o = this.loopStack.pop();
            if (_throughput.db) _throughput.d.ln(_throughput.TA, "Popped loop " + o);
            assert loop == o;
        }
        else
        {
            super.visit(loop);
        }
    }

    protected void preFilterAny (Component comp)
    {
        super.preFilterAny(comp);
//         System.out.println("TA: " + comp);
    }
    

    //
    // A shared procedure call does not impose the same type of access
    // restrictions on throughput as does any of the other globally
    // accessing {@link Access Accesses}, however we do not currently
    // use the shared procedure call, so this visit throws an
    // assertion error.
    //
    //     public void visit (SharedProcedureCall comp)
    //     {
    //         assert false : "SharedProcedureCall not supported in Throughput Analysis";
    //         markResource(comp);
    //         super.visit(comp);
    //     }

    /**
     * Method calls have throughput limit set by an user via api method.
     *
     * @param call a value of type 'Call'
     */
    private void markCall (Call call)
    {
        nodeToThroughputLimit.put(call, new ThroughputLocalLimit(call));
    }

    /**
     * Loops impose a limitation on the throughput of a task because
     * they must iterate to completion before new data can be
     * applied.  Thus their limitation is the max clocks of their done
     * exit.
     *
     * @param loop a value of type 'Loop'
     */
    private void markLoop (Loop loop)
    {
        assert !nodeToThroughputLimit.containsKey(loop) : "Duplicate traversal of loop " + loop;
        nodeToThroughputLimit.put(loop, new LoopLimit(loop, (ID)this.methodStack.peek()));
    }

    /**
     * Memory accesses need to be marked in addition to the standard
     * resource marking because their use of the memory address port
     * imposes an additional limitation on the throughput.  The
     * standard markResource is only concerned with complementary
     * accesses, ie the first read to last write or the first write to
     * the last read.  However for the address port of memories we
     * need to track the first access to the last access, which may be
     * two readers or two writers.  See note in {@link MemAddrLimit}.
     *
     * @param access an {@link Access} whose resource is a
     * {@link MemoryPort}
     */
    private void markMemAccess (Access access)
    {
        LogicalMemoryPort resource = (LogicalMemoryPort)access.getResource();
        MemAddrLimit memLimit = (MemAddrLimit)addrLimit.get(resource);
        if (memLimit == null)
        {
            memLimit = new MemAddrLimit(resource, access,
                getHeadLatency(access, resource), (ID)methodStack.peek());
            addrLimit.put(resource, memLimit);
        }
        else
        {
            memLimit.mark(access, getTailLatency(access), (ID)methodStack.peek());
        }
    }
    
    /**
     * Marks the resource based on complementary accesses which will
     * account for the first and last accesses that modify/consume the
     * data value represented by the resource.  Note that memories
     * constitute a single resource, thus any access may modify any
     * location.
     * <p> <b>NOTE: We could be more intelligent in the case of head
     * read/write accesses and try to figure out which locations may
     * be accessed which could potentially improve out throughput
     * calculation</b>
     */
    private void markResource (Access access)
    {
        Resource resource = access.getResource();
        if (resource == null && access instanceof PinAccess)
        {
            resource = PIN_RESOURCE;
        }

        ResourcePaths paths = (ResourcePaths)nodeToThroughputLimit.get(resource);
        if (paths == null)
        {
            paths = new ResourcePaths(resource);
            nodeToThroughputLimit.put(resource, paths);
        }

        // The latency to use if this access is the start of a new chain.
        Latency headLatency = getHeadLatency(access, resource);
        // The latency to use if this access it the tail of a chain.
        Latency tailLatency = getTailLatency(access);
        
        paths.mark(access, headLatency, tailLatency, (ID)methodStack.peek());

        if (_throughput.db) _throughput.d.ln(_throughput.TA,"Marked resource " + resource + "(" + resource.showIDLogical() + ") access " + access + " headlatency " + headLatency + " taillatency " + tailLatency + " in " + ((Call)methodStack.peek()).showIDLogical());
        //IDSourceInfo info = access.getIDSourceInfo();
        IDSourceInfo info = resource.getIDSourceInfo();
        if (_throughput.db) _throughput.d.ln(_throughput.TA,"\taccess:: pkg: " + info.getSourcePackageName() + " file: " + info.getSourceFileName() + " line " + info.getSourceLine() + " class " + info.getSourceClassName());
    }

    /**
     * Gets the latency to use if this access is the head of a chain
     * of accesses.
     */
    private Latency getHeadLatency (Access access, Resource resource)
    {
        final Latency headLatency;
        if (_throughput.db) _throughput.d.ln(_throughput.TA, "Finding head latency for " + access + " to " + resource);
        if (_throughput.db) _throughput.d.ln(_throughput.TA, "\tLoopStack.isEmpty(): " + this.loopStack.isEmpty());
        if (this.loopStack.isEmpty())
        {
            //
            // If the accessed resource does not allow parallel reads,
            // then that indicates there is a contention on its ports
            // such that 2 things cannot access it simultaneously
            // (like the memory address port).  In that case we need
            // to use the INPUT latency of the first access to ensure
            // that the 2 accesses do not occur simultaneously.  If
            // there is no contention, then it is OK for the first and
            // last access to occur in the same cycle and so we use
            // the DONE of the first access
            //
//             if (resource.allowsParallelReads())
//             {
//                 Bus doneBus = access.getExit(Exit.DONE).getDoneBus();
//                 headLatency = (Latency)this.latencyMap.get(doneBus);
//             }
//             else
//             {
                headLatency = (Latency)this.inputLatencyMap.get(access);
//             }
        }
        else
        {
            Object key = loopStack.firstElement();
            if (_throughput.db) _throughput.d.ln(_throughput.TA, "\tUsing input latency of: " + key);
            headLatency = (Latency)this.inputLatencyMap.get(key);
        }
        if (_throughput.db) _throughput.d.ln(_throughput.TA, "\thead latency: " + headLatency);
        assert headLatency != null;
        return headLatency;
    }

    /**
     * Gets the latency to use if this access is the tail of a chain
     * of accesses.
     */
    private Latency getTailLatency (Access access)
    {
        final Latency tailLatency;
        if (_throughput.db) _throughput.d.ln(_throughput.TA, "Finding tail latency for " + access);
        if (_throughput.db) _throughput.d.ln(_throughput.TA, "\tLoopStack.isEmpty(): " + this.loopStack.isEmpty());
        if (this.loopStack.isEmpty())
        {
            tailLatency = (Latency)this.inputLatencyMap.get(access);
        }
        else
        {
            Loop loop = (Loop)loopStack.firstElement();
            if (_throughput.db) _throughput.d.ln(_throughput.TA, "\tUsing output latency of: " + loop);
            tailLatency = (Latency)this.latencyMap.get(loop.getExit(Exit.DONE).getDoneBus());
        }
        assert tailLatency != null;
        if (_throughput.db) _throughput.d.ln(_throughput.TA, "\ttail latency: " + tailLatency);
        return tailLatency;
    }
    
}// ThroughputAnalyzer


