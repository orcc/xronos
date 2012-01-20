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

import net.sf.openforge.app.GenericJob;
import net.sf.openforge.app.OptionRegistry;
import net.sf.openforge.lim.*;
import net.sf.openforge.lim.memory.*;
import net.sf.openforge.optimize.*;
import net.sf.openforge.optimize.constant.*;
import net.sf.openforge.optimize.io.*;
import net.sf.openforge.optimize.memory.*;
import net.sf.openforge.optimize.pipeline.*;
import net.sf.openforge.schedule.block.ProcessCache;
import net.sf.openforge.schedule.loop.LoopCycleOptimizer;

/**
 * Scheduler is the entry point for scheduling a LIM design.  The
 * intention is that a new Scheduler will be created for each
 * scheduling trial over a given design.  When created the Scheduler
 * should contain all the necessary characteristics for the scheduling
 * pass (including pipeline metric, balanced, resource sharing, etc).
 *
 *
 * Created: Thu Mar 21 12:22:58 2002
 *
 * @author imiller
 * @version $Id: Scheduler.java 563 2008-03-17 21:39:34Z imiller $
 */
public class Scheduler
{
    private static final String _RCS_ = "$Rev: 563 $";

    /**
     * Schedules the given {@link Design}.
     *
     * @param design the design to be scheduled
     * @return the same design after scheduling
     */
    public static Design schedule (Design design)
    {
        //_schedule.d.launchXGraph(design, false);
        //try{System.out.println("SLEEPING");Thread.sleep(2000);}catch(Exception e){}
        GenericJob gj = design.getEngine().getGenericJob();
        gj.info("scheduling...");

        /*
         * Make sure all components except global things have port and bus
         * widths.  This will allow GateDepthAccumulator to run (some gate
         * depths depend upon the widths of the inputs).
         */
        TwoPassPartialConstant.propagate(design, false);

        Optimizer.runOpt(design,new PipelineOptimization());

        
        /*
         * Make sure all components except global things have port and bus
         * widths.  This will allow GateDepthAccumulator to run (some gate
         * depths depend upon the widths of the inputs).
         */
        TwoPassPartialConstant.propagate(design, false);

        /*
         * Resolve memory accesses so we can see which accesses
         * refer to which memories.
         */
        net.sf.openforge.optimize.memory.ObjectResolver.resolve(design);

        /*
         * Remove unused memories.
         */
        net.sf.openforge.optimize.DeadComponentVisitor.pruneMemories(design);

        /**
         * Establish the particular memory implementations
         */
        for (LogicalMemory mem : design.getLogicalMemories())
        {
            MemoryImplementation.setMemoryImplementation(mem);
        }
        
        // Convert 2 access tasks to dual port memory
        design.accept(new DualPortRamAllocator());

        /*
         * Build implementations for the memories that remain.
         * MemoryBuilder must come before global resource sequencer.
         */
        MemoryBuilder.buildMemories(design);

        /*
         * create global resource dependencies
         */
        if(_schedule.db) _schedule.ln("\n----------------------------\nCreating Resource Dependencies\n");
        design.accept(new GlobalResourceSequencer());

//         if (gj.getUnscopedBooleanOptionValue(OptionRegistry.LOOP_BRANCH_BALANCE))
//         {
//             design.accept(new LoopConditionalBranchBalancer());
//         }

        /*
         * Updates the 'consumesGo' and isUsed (on the go port) of
         * SimplePinWrites to fifo output data pins when there is
         * only a single writer to that pin, allowing the pin data
         * value to track intermediate values when not qualified.
         */
        gj.info("Running fifo data output optimizer");
        design.accept(new FifoDataOutputOptimizer());
        
        // 03.02.2007.  This should be obsolete unless we bring back
        // timing operations.
        design.accept(new TimingSequencer());
        
        /*
         * Characterize the types and numbers of accesses in each task
         * of the design and set the characteristics of global
         * entities accordingly.
         */
        if(_schedule.db) _schedule.ln("\n----------------------------\nCharacterizing Global Resources\n");
        design.accept(new AccessCounter());

        /*
         * Traverse the 'wrapper' function (top level function) of the
         * design and find any loops whose signatures match the block
         * IO handling loops and mark those loop flops as removable.
         FIXME.  IDM 03.02.2007  Check if this optimization is needed.
         */
//         System.out.println("Check block io loop flop remover needed");
//         if(_schedule.db) _schedule.ln("\n----------------------------\nMarking Block IO Loop Flops as Removable\n");
//         BlockIOLoopFlopRemover.analyze(design);

        /*
         * Find any/all multipliers in the design and add pipeline
         * registers to their outputs as defined by the user.
         */
        MultiplyPipeliner.pipelineMultipliers(design);
        
        LoopCycleOptimizer.optimize(design);

        /*
         * Perform schedule pass which connects inputs of all
         * components and generates control structures.
         */
        if (_schedule.db) _schedule.ln("\n----------------------------\nScheduling\n");
        ScheduleVisitor schedVis = ScheduleVisitor.schedule(design);

        // Generate result XML if/as necessary.  Should move to
        // LIMCompiler and output with other output engines... tbd
        //System.out.println("Move to LIMCompiler");
        //XLIMOutputEngine.generateXLIMTo(design, "test.xml");
        
        // Add input and output gatedepth as needed
        design.accept(new IODepthVisitor());

        /*
         * Compact FDs to SRL16s
         * 
         * Attention XXX: This visitor should not be moved other place
         * because the Regs chain got removed and replaced by
         * SRL16. In order to resolve the correct ownership of port's
         * Value bits, this visitor needs to placed between Pipeliner
         * and PostScheduleOptimize.  
         */
        if (gj.getPart(CodeLabel.UNSCOPED).doSupportSRL16())
        {
            if (_schedule.db) _schedule.ln("----------------------------\nCompacting SRL16\n");
            design.accept(new CompactSRL16Visitor());
        }

        /*
         * connect global resources with their local accesses
         */
        if (_schedule.db) _schedule.ln("\n----------------------------\nConnecting global resources\n");

        /*
         * We need to do this here so that constant prop runs _after_
         * we are done, because we may decompose an SRL16 (remove it's
         * last register) in order to attach an IOB attribute.  The
         * Bits from the Reg need to be propagated correctly.  But we
         * also need to be after the GlobalConnector because the pins
         * must already be hooked up.
         */
        if (!gj.getUnscopedBooleanOptionValue(OptionRegistry.NO_IOB_OPTS))
        {
            design.accept(new IOBRegisterVisitor());
        }

        // Put here because const prop fails if global things are not
        // properly connected
        design.accept(new GlobalConnector(schedVis.getLatencyCache()));

        /*
         * Make sure any new hardware has appropriate connection
         * Values.  This must be run AFTER all new hardware has been
         * inserted by scheduling, pipelining, and any other
         * optimizations.
         */
//         _schedule.d.launchXGraph(design, true);
//         try{System.out.println("SLEEPING");Thread.sleep(2000);}catch(Exception e){}
        new PostScheduleOptimize(design.getEngine()).optimize(design);

        return design;
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
    
}// Scheduler
