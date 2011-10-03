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

import net.sf.openforge.app.Engine;
import net.sf.openforge.app.EngineThread;
import net.sf.openforge.lim.*;
import net.sf.openforge.lim.memory.*;
import net.sf.openforge.lim.op.*;
import net.sf.openforge.optimize.constant.*;

/**
 * PostScheduleOptimize is a class which simply calls a sequence of
 * optimizations on a LIM that are used to clean up after scheduling.
 * Currently this includes:
 * <ul>
 * <li>a visitor to turn symbolic {@link MemOffsetConstant}s into
 * actual constant values based on the position of the field in the
 * tagetted memory. </li>
 * <li>a pass of PartialConstant to correctly size everything that was
 * added during scheduling and global connections.</li>
 * </ul>
 *
 * <p>Created: Mon Sep 23 15:49:45 2002
 *
 * @author imiller, last modified by $Author: imiller $
 * @version $Id: PostScheduleOptimize.java 425 2007-03-07 19:17:39Z imiller $
 */
public class PostScheduleOptimize 
{
    private static final String _RCS_ = "$Rev: 425 $";

    Engine controllingEngine;
    
    public PostScheduleOptimize (Engine engine)
    {
        this.controllingEngine = engine;
    }

    public void optimize (Visitable target)
    {
        EngineThread.getGenericJob().info("running post scheduling optimizations...  MemConstantVisitor");
        // Lock down the MemOffsetConstants
        target.accept(new MemConstantVisitor());

        
        // Run the default optimizations, but partial first to ensure
        // everything has values.
        EngineThread.getGenericJob().info("running post scheduling optimizations... TwoPassPartialConstant");
        TwoPassPartialConstant.propagate(target, true);

        /*
         * XXX FIXME. We should really run ALL the optimizations again
         * to remove unneeded adds in the memory accesses (which now
         * have 1 constant input) and reduce any redundant scheduling
         * logic.
         */
        //System.out.println("UPDATE OPTIMIZATIONS TO WORK POST SCHEDULING");
//         Optimizer postOpts = new Optimizer(this.controllingJob);
//         postOpts.optimize(target);
    }
    
}// PostScheduleOptimize

/**
 * This class simply visits anything that is expected to contain a
 * symbolic {@link MemOffsetConstant} and sets the actual constant
 * value of that node.
 */
class MemConstantVisitor extends DefaultVisitor
{
    public void visit (PriorityMux mux)
    {
        /*
         * Visits the constents of the PriorityMux in an order
         * that makes sense for constant prop, i.e. when all the
         * input Buses to a given Component have been visited, then
         * you can visit that Component.
         */
        final Set visitedBuses = new HashSet();
        final LinkedList queue = new LinkedList(mux.getComponents());
        while (!queue.isEmpty())
        {
            final Component component = (Component)queue.removeFirst();
            boolean isReady = true;
            for (Iterator iter = component.getPorts().iterator(); iter.hasNext();)
            {
                final Port port = (Port)iter.next();
                if (port.isConnected() && !visitedBuses.contains(port.getBus()))
                {
                    isReady = false;
                    break;
                }
            }

            if (isReady)
            {
                component.accept(this);
                visitedBuses.addAll(component.getBuses());
            }
            else
            {
                queue.add(component);
            }
        }
    }

    public void visit (HeapRead read)
    {
        /*
         * XXX: In C, the offset is fixed, so a regular Constant
         * is created for it, not a DeferredConstant.
         */
    }

    public void visit (HeapWrite write)
    {
        /*
         * XXX: In C, the offset is fixed, so a regular Constant
         * is created for it, not a DeferredConstant.
         */
    }

    /**
     * Resolves the abstract pointer to a {@link Location} into a
     * concrete address at which to access the backing memory.
     */
    public void visit (AbsoluteMemoryRead read)
    {
        read.getAddressConstant().lock();
    }

    /**
     * Resolves the abstract pointer to a {@link Location} into a
     * concrete address at which to access the backing memory.
     */
    public void visit (AbsoluteMemoryWrite write)
    {
        write.getAddressConstant().lock();
    }

    public void visit (ArrayRead arrayRead)
    {
        super.visit(arrayRead);
    }
    
    public void visit (ArrayWrite arrayWrite)
    {
        super.visit(arrayWrite);
    }

    public void visit (Constant constant)
    {
        super.visit(constant);
        constant.lock();
        // Some constants contain other constants.  These may be
        // 'hidden' or virtual constants as they are not actually in
        // the lim but are simply supporting the constant which is in
        // the LIM.  So, we need to lock down any of those as well.
        for (Iterator iter = constant.getContents().iterator(); iter.hasNext();)
        {
            ((Constant)iter.next()).lock();
        }
    }
}
