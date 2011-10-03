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
import net.sf.openforge.lim.memory.*;

/**
 * A small visitor used to traverse a design and count the types and
 * numbers of accesses in each task, then use that information to
 * annotate global resources with information used during scheduling.
 *
 * <p>Created: Fri Jul 26 12:19:04 2002
 *
 * @author imiller
 * @version $Id: AccessCounter.java 2 2005-06-09 20:00:48Z imiller $
 */
public class AccessCounter extends DefaultVisitor 
{
    private static final String _RCS_ = "$Rev: 2 $";

    /** A map of Resource -> Task's that access that resource. */
    private Map resourceToTasks = new HashMap();

    /** The current task being traversed. */
    private Task currentTask = null;


    /**
     * Traverses the design to characterize which {@link Referent}s
     * are accessed from each task and updates the Referent's
     * according to that information.
     *
     * @param design a value of type 'Design'
     */
    public void visit (Design design)
    {
        super.visit(design);

        for (Iterator iter = design.getLogicalMemories().iterator(); iter.hasNext();)
        {
            LogicalMemory memory = (LogicalMemory)iter.next();
            for (Iterator portIter = memory.getLogicalMemoryPorts().iterator(); portIter.hasNext();)
            {
                LogicalMemoryPort resource = (LogicalMemoryPort)portIter.next();
                Set accessingTasks = (Set)resourceToTasks.get(resource);
                resource.setArbitrated(accessingTasks == null ? false : (accessingTasks.size() > 1));
            }
        }
    }

    public void visit (Task task)
    {
        this.currentTask = task;
        super.visit(task);
    }

    public void visit (MemoryRead memRead)
    {
        super.visit(memRead);
        addAccess(memRead);
    }
    
    public void visit (MemoryWrite memWrite)
    {
        super.visit(memWrite);
        addAccess(memWrite);
    }

    private void addAccess (Access access)
    {
        Resource resource = access.getResource();
        Set tasks = (Set)resourceToTasks.get(resource);
        if (tasks == null)
        {
            tasks = new HashSet();
            resourceToTasks.put(resource, tasks);
        }
        tasks.add(this.currentTask);
    }
    
}// AccessCounter
