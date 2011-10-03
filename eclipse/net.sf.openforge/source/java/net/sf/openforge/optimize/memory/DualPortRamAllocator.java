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

package net.sf.openforge.optimize.memory;


import java.util.*;

import net.sf.openforge.app.*;
import net.sf.openforge.lim.*;
import net.sf.openforge.lim.memory.*;

/**
 * An optimization to create dual port memories for any memory with
 * two accesses in an action (task).  This optimization works in close
 * conjunction with the insertion of resoure dependencies (see
 * GlobalResourceSequencer and LogicalMemory.getSpacing).
 *
 * In order to ensure correct functionality the appearance of order
 * must be preserved.  This is done through the following rules
 * 1) GlobalResourceSequencer will always put a resource dependency
 * between two accesses to the same memory even if it is a dual port
 * and the accesses are on seperate ports.
 * 2) The resource dependency will be 0 cycle (allowing concurrent
 * access but not out of order access) unless the 2 accesses are R-R,
 * W-R or out of phase with the memory type.
 *  2a) LUT memories are always read first.  BRAMs may be selected to
 *      be read first or write first.
 *  2b) out of phase for read first is W-R.  out of phase for write
 *      first is R-W
 *
 * Created: Thu Mar 21 12:22:58 2002
 *
 * @author imiller
 * @version $Id: Scheduler.java 425 2007-03-07 19:17:39Z imiller $
 */
public class DualPortRamAllocator extends DefaultVisitor
{
    private static final String _RCS_ = "$Rev: 425 $";

    private Map<LogicalMemory, Set<MemoryAccessBlock>> memToAcc = new HashMap();
    private Map<LogicalMemory, Map<Task, Set<MemoryAccessBlock>> > taskToAcc = new HashMap();
    private Task currentTask = null;
    
    public void visit (Design des)
    {
        final GenericJob gj = EngineThread.getGenericJob();
        final boolean allowDPLutMem = gj.getUnscopedBooleanOptionValue(OptionRegistry.ALLOW_DUAL_PORT_LUT);
        
        this.memToAcc = new HashMap();
        super.visit(des);
        for (LogicalMemory mem : memToAcc.keySet())
        {
            boolean readOnlyPort = false;
            if (!allowDPLutMem && mem.getImplementation().isLUT())
                continue;
            if (allowDPLutMem && mem.getImplementation().isLUT())
                readOnlyPort = true;
            
            // Find one task with more than one access.  If the memory implementation
            // has one port as read-only, then ensure that the found task has at least 
            // one read.
            boolean validForDualPorting = false;
            for (Set<MemoryAccessBlock> mabs : taskToAcc.get(mem).values())
            {
                if (mabs.size() < 2)
                    continue;
                
                if (readOnlyPort)
                {
                    for (MemoryAccessBlock mab : mabs)
                    {
                        if (mab.getMemoryAccess().isReadAccess())
                        {
                            validForDualPorting = true;
                            break;
                        }
                    }
                } else {
                    validForDualPorting = true;
                    break;
                }
            }
            
            if (validForDualPorting)
            {
                if (mem.getLogicalMemoryPorts().size() < 2)
                {
                    mem.createLogicalMemoryPort();
                    String id = mem.getSourceName() == null ? mem.toString():mem.getSourceName();
                    gj.info("Created a second memory port for " + id);
                }
                gj.info("\tReallocating accesses to memory");

                // Do the re-allocation by task so that multiple accesses in a task are split across the dual ports
                Map<Task, Set<MemoryAccessBlock>> accessMap = this.taskToAcc.get(mem);
                for (Set<MemoryAccessBlock> accesses : accessMap.values())
                {
                    for (MemoryAccessBlock mab : accesses)
                    {
                        mem.removeAccessor(mab);
                    }
                    Iterator portIter = mem.getLogicalMemoryPorts().iterator();
                    LogicalMemoryPort port1 = (LogicalMemoryPort)portIter.next();
                    LogicalMemoryPort port2 = (LogicalMemoryPort)portIter.next();

                    boolean evenReadOnly = false;
                    if (mem.getImplementation().isLUT())
                    {
                        final boolean port1readOnly = port1.isReadOnly();
                        final boolean port2readOnly = port2.isReadOnly();
                        assert port1readOnly || port2readOnly : "LUT memories must have at least 1 read only port";

                        evenReadOnly = true;
                        if (!port1readOnly)
                        {
                            // Port 2 must be read only, thus switch them.
                            LogicalMemoryPort p = port2;
                            port2 = port1;
                            port1 = p;
                        }
                    }

                    boolean even = true;
                    for (MemoryAccessBlock mab : accesses)
                    {
                        if (even)
                        {
                            if (evenReadOnly && !mab.getMemoryAccess().isReadAccess())
                            {
                                port2.addAccess(mab);
                                // dont switch 'even' since we've not yet added to port 1
                            }
                            else
                            {
                                port1.addAccess(mab);
                                even = !even;
                            }
                        }
                        else
                        {
                            port2.addAccess(mab);
                            even = !even;
                        }
                    }
                }
            }
        }
    }

    public void visit (Task task)
    {
        this.currentTask = task;
        super.visit(task);
    }

    public void visit (AbsoluteMemoryRead read){addAcc(read);}
    public void visit (AbsoluteMemoryWrite write){addAcc(write);}
    public void visit (HeapRead read){addAcc(read);}
    public void visit (HeapWrite write){addAcc(write);}

    private void addAcc (MemoryAccessBlock acc)
    {
        LogicalMemory mem = acc.getLogicalMemoryPort().getLogicalMemory();
        assert mem != null;
        Set<MemoryAccessBlock> accesses = memToAcc.get(mem);
        if (accesses == null)
        {
            accesses = new LinkedHashSet();
            memToAcc.put(mem, accesses);
        }
        accesses.add(acc);

        // Register the access by task and memory
        Map<Task, Set<MemoryAccessBlock>> accessMap = this.taskToAcc.get(mem);
        if (accessMap == null)
        {
            accessMap = new HashMap();
            this.taskToAcc.put(mem, accessMap);
        }
        accesses = accessMap.get(this.currentTask);
        if (accesses == null)
        {
            accesses = new LinkedHashSet();
            accessMap.put(this.currentTask, accesses);
        }
        accesses.add(acc);
        
    }

}
