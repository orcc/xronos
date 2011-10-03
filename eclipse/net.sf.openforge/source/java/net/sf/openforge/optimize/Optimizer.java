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

package net.sf.openforge.optimize;

import java.util.*;

import net.sf.openforge.app.EngineThread;
import net.sf.openforge.app.GenericJob;
import net.sf.openforge.lim.*;
import net.sf.openforge.optimize.constant.*;
import net.sf.openforge.optimize.io.*;
import net.sf.openforge.optimize.loop.LoopUnrollingVisitor;
import net.sf.openforge.optimize.loop.LoopVariableResizer;
import net.sf.openforge.optimize.memory.*;
import net.sf.openforge.optimize.nesting.*;
import net.sf.openforge.optimize.replace.OperationReplacementVisitor;
import net.sf.openforge.util.Debug;


/**
 * Optimizer supplies the entry point for performing optimizations
 * on a {@link Design}.
 *
 * Created: Tue Mar 12 13:12:02 2002
 *
 * @author imiller
 * @version $Id: Optimizer.java 538 2007-11-21 06:22:39Z imiller $
 */
public class Optimizer
{
    private static final String _RCS_ = "$Rev: 538 $";
    
    /**
     * Perform all possible optimizations on a given {@link Design},
     * including constant propagation, bus sizing, and loop
     * unrolling, as specified by the user preferences.
     *
     * @param target the {@link Design} object to be optimized.
     *               Anything below the target in the LIM hierarchy
     *               will also get optimized.
     * @return the optimized target; currently, no copy is made
     *         of the given target so the original target is simply
     *         returned.
     */
    public Visitable optimize (Design target)
    {
        if (_optimize.db) _optimize.d.graph(target, "pre", "/tmp/pre.dot", Debug.GR_DEFAULT);
        
        GenericJob gj = EngineThread.getGenericJob();
        FullConstantVisitor fullConstVisitor = new FullConstantVisitor();
        HalfConstantVisitor halfConstantVisitor = new HalfConstantVisitor();
        LoopUnrollingVisitor loopUnrollingVisitor = new LoopUnrollingVisitor();
        DeadComponentVisitor deadComponentVisitor = new DeadComponentVisitor();
        BaseAddressUniquifier baseAddrUniquifier = new BaseAddressUniquifier();
        // Functionality of DualPortBRAMAllocator now handled just
        // prior to scheduling in the DualPortRamAllocator
//         DualPortBRAMAllocator dualPortAlloc = new DualPortBRAMAllocator();
        MemorySplitter memSplitter = new MemorySplitter();
        MemoryReducer memReducer = new MemoryReducer();
        MemoryTrimmer memTrimmer = new MemoryTrimmer();
        ReadOnlyFieldReducer readOnlyFieldReducer = new ReadOnlyFieldReducer();
        RomReplicator romRep = new RomReplicator();
        MemoryToRegister memToReg = new MemoryToRegister();
        NestedBlockOptimization nestedBlock=new NestedBlockOptimization();

        BlockElementRemover blockElementRemover = new BlockElementRemover();
        LoopVariableResizer loopVariableResizer = new LoopVariableResizer();
        
        /*
         * This will ensure that all memory accesses are made known to
         * their respective LogicalMemories.
         */
        net.sf.openforge.optimize.memory.ObjectResolver.resolve(target);

        // For now run the block element eliminator just one time. It
        // is possible that running loop unrolling and/or dead code
        // will eliminate an entire writer to an input arg (or reader)
        // but those cases should be minimal and not worth the
        // additional overhead.
        runOpt(target, blockElementRemover);
        
        // block unnesting - XXX
        //runOpt(target,nestedBlock);
        //runOpt(target,loopUnrollingVisitor);
        
        /*
         * Ensure that all NoOps are removed using the DeadComponentVisitor.
         */
        runOpt(target, deadComponentVisitor);
//         target.accept(deadComponentVisitor);

        (new net.sf.openforge.schedule.block.CallUnnesting(false)).flattenCalls(target);

        /*
         * Use partial constant propagation to ensure that every Port and
         * Bus has a Value.
         */
        TwoPassPartialConstant.propagate(target, Collections.EMPTY_LIST);

        List<Optimization> memOpts = new ArrayList();
        memOpts.add(baseAddrUniquifier);
        memOpts.add(memSplitter);
        memOpts.add(readOnlyFieldReducer);
        memOpts.add(memTrimmer);
        memOpts.add(memReducer);
//         memOpts.add(dualPortAlloc);
        
        boolean isModified = true;
        while (isModified)
        {
            isModified = false;
            
            fullConstVisitor.clear();
            halfConstantVisitor.clear();
            loopUnrollingVisitor.clear();
            deadComponentVisitor.clear();
            
            gj.info("replacing constant expressions...");
            gj.inc();
            isModified = isModified | runOpt(target, fullConstVisitor);
//             target.accept(fullConstVisitor);
//             isModified |= fullConstVisitor.didModify(); 
            gj.verbose("replaced " + fullConstVisitor.getReplacedNodeCount() + " expressions");
            gj.dec();
            
            gj.info("pruning dead code...");
            gj.inc();
            isModified = isModified | runOpt(target, deadComponentVisitor);
//             target.accept(deadComponentVisitor);
//             isModified |= deadComponentVisitor.didModify();
            gj.verbose("pruned " + deadComponentVisitor.getRemovedNodeCount() + " expressions");
            gj.dec();
            deadComponentVisitor.clearCount();
            
            isModified |= runOpts(target, memOpts);

            // XXX Run Loop unrolling before half constant prop so that we
            // can recognize divide by power of 2 as a index variable
            // increment.  Same for multiplies that get replaced.
            // Loop unrolling can only detect single operations
            // modifying the loop index and these replacements are
            // multiple nodes.  But, doing so causes Value problems
            // (something in the unrolled loop has no value?)
            isModified=isModified|runOpt(target,loopUnrollingVisitor);

            // block unnesting
            isModified=isModified|runOpt(target,nestedBlock);
        
            gj.info("reducing expressions with constants...");
            isModified = isModified | runOpt(target, halfConstantVisitor);
//             target.accept(halfConstantVisitor);
//             isModified |= halfConstantVisitor.didModify();
            gj.verbose("   reduced - removed " + halfConstantVisitor.getRemovedNodeCount() + " expressions");
            gj.verbose("   reduced - replaced " + halfConstantVisitor.getReplacedNodeCount() + " expressions");
            gj.dec();
            
            gj.info("pruning dead code...");
            gj.inc();
            isModified = isModified | runOpt(target, deadComponentVisitor);
//             target.accept(deadComponentVisitor);
//             isModified |= deadComponentVisitor.didModify();
            gj.verbose("pruned " + deadComponentVisitor.getRemovedNodeCount() + " expressions");
            gj.dec();
         
            //
            // Convert single element memories to registers.  This
            // needs to have partial constant prop run after it but
            // before half or full constant runs again.  That is
            // because the optimization creates RegisterAccessBlock
            // modules which need the partial constant prop rules run
            // in order to initialize the inbuf/outbufs before
            // half/full run.
            //

            isModified= isModified | runOpt(target,memToReg);
            
            gj.info("propagating constant bits...");
            TwoPassPartialConstant.propagate(target, Collections.EMPTY_LIST);

        }
        
        /* resize loop variables before operation substitution */
        runOpt(target, loopVariableResizer);

        gj.info("performing operation substitution...");
        gj.inc();
        OperationReplacementVisitor orv = new OperationReplacementVisitor();
        runOpt(target, orv);
//         target.accept(orv);
        gj.verbose("replaced " + orv.getReplacedNodeCount() + " operations");
        gj.dec();
        
        gj.verbose("Total expressions replaced with constants: " + fullConstVisitor.getReplacedNodeCountTotal());
        gj.verbose("Total pruned expressions: " + deadComponentVisitor.getRemovedNodeCountTotal());
        gj.dec();
        

        // Run these constant prop again after each iteration so that
        // we remove left/unsignedright shifts by magnitude larger
        // than value size.
        List additionalPasses = new ArrayList();
        additionalPasses.add(halfConstantVisitor);
        additionalPasses.add(fullConstVisitor);
        additionalPasses.add(deadComponentVisitor);

        // Always run partial constant propagation.
        gj.info("propagating constant bits...");
        //target.accept(new PartialConstant(additionalPasses));
        TwoPassPartialConstant.propagate(target, additionalPasses);

        // Rom replication must run after partial constant prop to
        // accurately determine the number of bits allocated.
        runOpt(target, romRep);
//         gj.info("*** RomReplicator...");
//         romRep.run(target);

        runOpt(target, memSplitter);
//         gj.info("*** MemorySplitter...");
//         memSplitter.run(target);

        if (_optimize.db) _optimize.d.graph(target, "POST OPTIMIZE", "/tmp/post.dot", Debug.GR_DEFAULT);
        return target;
    }

    private boolean runOpts (Visitable target, List<Optimization> opts)
    {
        boolean modifiedThisPass=false;
        boolean modifiedAtAll=false;
        do
        {
            modifiedThisPass=false;
            // run all these opts as long as any make changes ...
            for (Optimization opt : opts)
            {
                modifiedThisPass=modifiedThisPass|runOpt(target,opt);
                modifiedAtAll=modifiedAtAll|modifiedThisPass;
            }
        }while (modifiedThisPass);

        return modifiedAtAll;
    }

    public static boolean runOpt(Visitable target, Optimization opt)
    {
//         System.out.println("******************");
//         System.out.println("Started " + opt);
        opt.clear(); // Clear out prior
        opt.preStatus();        
        opt.run(target);
        boolean modify=opt.didModify();
        EngineThread.getGenericJob().inc();
        opt.postStatus();
        EngineThread.getGenericJob().dec();
        opt.clear();

//         System.out.println("Finished " + opt);
//         try{Thread.sleep(500);}catch (Exception e){}
        
        return modify;
    }
    
}// Optimizer
