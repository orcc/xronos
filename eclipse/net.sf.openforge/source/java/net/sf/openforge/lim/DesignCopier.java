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
import net.sf.openforge.app.project.SearchLabel;
import net.sf.openforge.forge.api.internal.*;
import net.sf.openforge.forge.api.pin.ClockPin;
import net.sf.openforge.forge.api.pin.ResetPin;
import net.sf.openforge.forge.api.sim.pin.*;
import net.sf.openforge.lim.io.*;
import net.sf.openforge.lim.io.actor.*;
import net.sf.openforge.lim.memory.*;
import net.sf.openforge.util.naming.*;



/**
 * DesignCopier is used to create parallel, independent versions of
 * portions of a Design.
 *
 * @author imiller
 * @version $Id: DesignCopier.java 121 2006-03-27 19:58:31Z imiller $
 */
public class DesignCopier
{
    private static final String _RCS_ = "RCS_REVISION: $Rev: 121 $";

    private final CloneCorrelationMap cloneMap = new CloneCorrelationMap();

    public DesignCopier ()
    {
    }

    /**
     * Returns the Map of original component to new (cloned)
     * component. 
     */
    public Map getCorrelation ()
    {
        return Collections.unmodifiableMap(this.cloneMap.cloneMap);
    }
    
    /**
     * Creates a new Design object with an exact copy of all the logic
     * in the Tasks from the source design.  This method does NOT copy
     * referenceable objects such as memories, registers, and pins.
     * This means that there remains linkages between the source and
     * copied design, concentrated at all the reference type nodes in
     * the tasks (memory accesses, pin read/writes, etc).  The copied
     * design contains no memories, registers or pins in its
     * structure. 
     */
    public Design copyLogicOnly (Design design)
    {
        Design newDesign = new Design();
        
        Map taskCorrelation = new HashMap();
        for (Task task : design.getTasks())
        {
            try
            {
                Call originalCall = task.getCall();
                this.cloneMap.addSelf(originalCall);
                Call clonedCall = (Call)originalCall.clone();
                this.cloneMap.removeSelf(originalCall);
                Task cloneTask = new Task(clonedCall);
                cloneTask.setKickerRequired(task.isKickerRequired());
                newDesign.addTask(cloneTask);
                taskCorrelation.put(task, cloneTask);
                correlateTopLevel(task, cloneTask);
            }
            catch (CloneNotSupportedException cnse)
            {
                throw new IllegalStateException("Cloning of design logic failed " + cnse);
            }
        }
        taskCorrelation.putAll(this.cloneMap.cloneMap);
        design.accept(new TaskCallRetarget(taskCorrelation));

        return newDesign;
    }

    private void correlateTopLevel (Task orig, Task clone)
    {
        this.cloneMap.cloneMap.put(orig, clone);
        this.cloneMap.cloneMap.put(orig.getCall(), clone.getCall());
        this.cloneMap.cloneMap.put(orig.getCall().getProcedure(), clone.getCall().getProcedure());
        this.cloneMap.cloneMap.put(orig.getCall().getProcedure().getBody(), clone.getCall().getProcedure().getBody());
    }
    

    private static class TaskCallRetarget extends DefaultVisitor
    {
        private Map corr;
        private TaskCallRetarget (Map corrMap) {this.corr = corrMap;}
        public void visit (TaskCall taskCall)
        {
            Task origTask = taskCall.getTarget();
            Task cloneTask = (Task)corr.get(origTask);
            TaskCall cloneCall = (TaskCall)corr.get(taskCall);
            cloneCall.setTarget(cloneTask);
        }
    }
    
    private static class CloneCorrelationMap extends FilteredVisitor implements CloneListener
    {
        Map cloneMap = new HashMap();
        boolean add = true;

        public void addSelf (Visitable vis)
        {
            this.add = true;
            vis.accept(this);
        }
        
        public void removeSelf (Visitable vis)
        {
            this.add = false;
            vis.accept(this);
        }
        
        public void setCloneMap (Map cloneMap)
        {
            this.cloneMap.putAll(cloneMap);
        }

        public void filterAny (Component comp)
        {
            if (add)
                comp.addCloneListener(this);
            else
                comp.removeCloneListener(this);
        }
    }
    
}
