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
package net.sf.openforge.report;

import java.util.*;

import net.sf.openforge.lim.*;
import net.sf.openforge.lim.memory.*;

/**
 * A visitor that travserses through a Design and stores resources it discovers
 * to a {@link ResourceBank}.
 *
 * @author    ysyu
 * @version   $Id: ResourceReporter.java 149 2006-06-28 17:34:19Z imiller $
 */
public class ResourceReporter extends FilteredVisitor
{
    private final static String _RCS_ = "$Rev: 149 $";

    private DesignResource designResource = null;

    /*
     * used to keep track of calling methods and submethod calls 
     */
    private ProcedureResource callingProcResource = null;
    private ProcedureResource currentProcResource = null;
    private Map callingToCurrent = new HashMap();
    
    /** used to prevent calling generateTotalReport() more than once */
    private Set unique_proc = new HashSet(11);

    public ResourceReporter()
    {
    }
    
    public DesignResource getResource()
    {
        return designResource;
    }

    /**
     * Creates a new DesignResource and adds a new TaskResource to the
     * newly created DesignResource
     *
     * @param design a LIM design
     */
    public void preFilter(Design design)
    {
        designResource = new DesignResource(design);
        for (Iterator it = design.getTasks().iterator(); it.hasNext();)
        {
            Task task = (Task)it.next();
            designResource.addResource(new TaskResource(task));
        }
    }

    /**
     * Visit the Task(s) according to TaskResources in a DesignResource 
     *
     * @param design a LIM design
     */
    public void visit(Design design)
    {
        preFilter(design);
        for(Iterator iter = designResource.getResources().iterator(); iter.hasNext();)
        {
            TaskResource tr = (TaskResource)iter.next();
            tr.getTask().accept(this);
        }
    }

    /**
     * Resolves the current TaskResource and add a ProcedureResource for 
     * entry method of this Task
     *
     * @param task a LIM task
     */
    public void preFilter(Task task)
    {
        TaskResource res = getCurrentResource(task);
        res.addResource(new ProcedureResource(task.getCall().getProcedure()));
    }

    public void visit(Task task)
    {
        preFilter(task);
        super.visit(task.getCall());
    }

    /**
     * Resolves the current ProcedureResource 
     *
     * @param call a LIM method call
     */
    public void preFilter(Call call)
    {
        ProcedureResource res = getCurrentResource(call.getProcedure());
        if(res == null)
        {
            ProcedureResource newRes = new ProcedureResource(call.getProcedure());
            currentProcResource.addResource(newRes);
            callingToCurrent.put(newRes, currentProcResource);
            callingProcResource = currentProcResource;
            currentProcResource = newRes;
        }
        else
        {
            currentProcResource = res;
        }
    }

    /**
     * Generate a total resource report for a Procedure
     *
     * @param call a LIM method call
     */
    public void filter(Call call)
    {
        if(callingProcResource != null) 
        {    
            if(unique_proc.add(call.getProcedure()))
            {
                currentProcResource.generateTotalReport();
            }
            currentProcResource = callingProcResource;
            callingProcResource = (ProcedureResource)callingToCurrent.get(callingProcResource);
        }
        else
        {
            if(unique_proc.add(call.getProcedure()))
            {
                currentProcResource.generateTotalReport();
            }
        }
    }

    public void visit(Block block)
    {
        traverse(block);
    }

    /**
     * Adds an operation to a ProcedureResource
     *
     * @param op a LIM operation
     */
    public void filter (Operation op)
    {
        currentProcResource.addResource(op);
    }

    /**
     * Adds a primitive to a ProcedureResource
     *
     * @param pm a LIM primitive
     */
    public void filter (Primitive pm)
    {
        currentProcResource.addResource(pm);
    }

    public void filter (MemoryAccess access)
    {
        currentProcResource.addResource(access);
    }

    /**
     * Resolves the TaskResource that contains all the resources
     * from the given Task.
     *
     * @param task a LIM task
     *
     * @return a TaskResource associated with a Task
     */
    private TaskResource getCurrentResource(Task task)
    {
        TaskResource resource = null;
        for(Iterator iter = designResource.getResources().iterator(); iter.hasNext();)
        {
            TaskResource res = (TaskResource)iter.next();
            if(task.equals(res.getTask()))
            {
                resource = res;
            }
        }
        return resource;
    }

    /**
     * Resolves the Procedure that contains all the resources from
     * the given Procedure.
     *
     * @param proc a LIM procedure
     *
     * @return a ProcedureResource associated with a Procedure
     */
    private ProcedureResource getCurrentResource(Procedure proc)
    {
        ProcedureResource resource = null;
        for(Iterator iter = designResource.getResources().iterator(); iter.hasNext();)
        {
            TaskResource tres = (TaskResource)iter.next();
            for(Iterator titer = tres.getResources().iterator(); titer.hasNext();)
            {
                ProcedureResource pres = (ProcedureResource)titer.next();
                if(proc.equals(pres.getProcedure()))
                {
                    return pres;
                }
                else
                {
                    for(Iterator piter = pres.getResources().iterator(); piter.hasNext();)
                    {
                        Object o = piter.next();
                        if(o instanceof ProcedureResource)
                        {
                            if(proc.equals(((ProcedureResource)o).getProcedure()))
                            {   
                                resource = (ProcedureResource)o;
                            }
                        }
                    }
                }
            }
        }
        return resource;
    }
}

        
