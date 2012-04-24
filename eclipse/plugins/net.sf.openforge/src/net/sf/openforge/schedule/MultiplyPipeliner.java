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
import net.sf.openforge.lim.op.*;
import net.sf.openforge.util.naming.*;

/**
 * This visitor traverses the entire LIM and finds all {@link
 * MultiplyOp} operations and depending on the value returned by 
 * {@link MultiplyOp#getPipeStages} will append 'n' registers after
 * the multiplier.  The intent is that XST will then subsume those
 * registers into the multiplier itself.  It is important to note that
 * this visitor must be run only 1 time, and before scheduling,
 * otherwise we will end up with multiple sets of registers added
 * after the multiplier.
 *
 * <p>Created: Thu Jul 15 10:40:14 2004
 *
 * @author imiller, last modified by $Author: imiller $
 * @version $Id: MultiplyPipeliner.java 280 2006-08-11 17:00:32Z imiller $
 */
public class MultiplyPipeliner extends DefaultVisitor
{
    private List multipliers = new ArrayList();
    
    /**
     * Only allowed accesses are through static methods
     */
    private MultiplyPipeliner ()
    {}
    
    /**
     * Traverse the LIM, find multipliers, and insert registers as
     * directed. 
     *
     * @param design a non-null 'Design'
     * @throws NullPointerException if design is null.
     */
    public static void pipelineMultipliers (Design design)
    {
        MultiplyPipeliner finder = new MultiplyPipeliner();
        design.accept(finder);
        for (Iterator iter = finder.multipliers.iterator(); iter.hasNext();)
        {
            MultiplyOp multiply = (MultiplyOp)iter.next();
            pipeline(multiply);
        }
    }
    

    /**
     * Cache up the multiplies found in the LIM for modification.
     *
     * @param multiply a value of type 'MultiplyOp'
     */
    public void visit (MultiplyOp multiply)
    {
        super.visit(multiply);
        this.multipliers.add(multiply);
    }
    
    /**
     * For each stage required by the getPipeStages method on a
     * multiplier we create a new Register and add it to the output of
     * the multiplier.  Once the stages are in place then all the
     * dependencies from the multiplier output are moved over to the
     * last register in the chain.
     *
     * @param multiply a value of type 'MultiplyOp'
     */
    private static void pipeline (MultiplyOp multiply)
    {
        // Add 'n' registers (non enabled) to the output of the
        // multiplier
        if (multiply.getPipeStages() > 0)
        {
            // Track the current result and done buses for connection
            // to the next register or ultimately to the consumers of
            // the multipliers value.
            Bus output = multiply.getResultBus();
            Bus done = multiply.getExit(Exit.DONE).getDoneBus();
            // Cache up the dependents so that we can reconnect them
            List dependents = new ArrayList(output.getLogicalDependents());
            // We need the clock and reset bus for each reg.
            Bus clockBus = multiply.getOwner().getClockPort().getPeer();
            Bus resetBus = multiply.getOwner().getResetPort().getPeer();
            for (int i=0; i < multiply.getPipeStages(); i++)
            {
                // Create the register.  No reset needed b/c it is
                // solely data path
                Reg nextReg = Reg.getConfigurableReg(Reg.REG, ID.showLogical(multiply) + "_pipe_" + i);
                // Dont allow it to be turned into an SRL16
                nextReg.setGroupableFlag(false);
                // Size the reg so that it has a Value on its bus
                nextReg.getResultBus().setSize(output.getValue().getSize(), output.getValue().isSigned());
                // Create an entry to attach the dependencies to
                Entry nextEntry = nextReg.makeEntry(output.getOwner());
                nextEntry.addDependency(nextReg.getDataPort(), new DataDependency(output));
                nextEntry.addDependency(nextReg.getClockPort(), new ClockDependency(clockBus));
                nextEntry.addDependency(nextReg.getResetPort(), new ResetDependency(resetBus));
                nextEntry.addDependency(nextReg.getGoPort(), new ControlDependency(done));

                moduleInsert(output.getOwner().getOwner(), nextReg);
                
                output = nextReg.getResultBus();
                done = nextReg.getExit(Exit.DONE).getDoneBus();
            }

            // Reconnect the dependencies.
            for (Iterator iter = dependents.iterator(); iter.hasNext();)
            {
                Dependency originalDep = (Dependency)iter.next();
                originalDep.getEntry().addDependency(originalDep.getPort(), new DataDependency(output));
                // Disconnect the old dependency
                originalDep.zap();
            }
        }
    }

    /**
     * Stolen from ComponentSwapVisitor...
     *
     * @param location a value of type 'Component'
     * @param toInsert a value of type 'Component'
     */
    private static void moduleInsert (Component location, Component toInsert)
    {
        // If the owner is a block use the much friendlier insert method.
        if (location.getOwner() instanceof Block)
        {
            Block block = (Block)location.getOwner();
            int position = block.getSequence().indexOf(location);
            // Put it _after_ the component
            block.insertComponent(toInsert, position+1);
        }
        else
        {
            location.getOwner().addComponent(toInsert);
        }
    }
    
}
