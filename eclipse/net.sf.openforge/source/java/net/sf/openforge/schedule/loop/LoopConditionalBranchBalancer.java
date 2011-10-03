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

package net.sf.openforge.schedule.loop;


import java.util.*;

import net.sf.openforge.app.EngineThread;
import net.sf.openforge.lim.*;
import net.sf.openforge.lim.io.*;
import net.sf.openforge.lim.memory.*;
import net.sf.openforge.lim.op.*;

/**
 * The LoopConditionalBranchBalancer finds combinational paths
 * through a loop body and attempts to balance the delay through those
 * paths according to this logic:
 * <ol>
 * <li>If all paths are combinational, nothing is done
 * <li>If all paths are definitively non-combinational, nothing is
 * done.
 * <li>If one side of a branch statement is definitively
 * non-combinational, and the other side is definitively
 * combinational, the combinational path will have a new
 * {@link ResourceDependency} added from the GO to the DONE with a
 * minimum offset clocks of 1.
 * <li>If the latency of either side is non definitive then a new
 * {@link ResourceDependency} with minimum clocks of 1 will be placed
 * around the entire Branch, causing a scoreboard to be inserted to
 * merge the done of the branch with a 1 cycle latency delay of its
 * GO. 
 * </ol>
 *
 * <p>This optimization works by visiting the entire LIM depth first.
 * At each modular level the latency is determined by examining the
 * contents of that module.  These latencies are propagated up
 * through the levels.  At each branch, the latencies of the true vs
 * the false branches are compared (actually the greatest latency(ies)
 * from each is(are) compared.  
 * 
 * @version $Id: LoopConditionalBranchBalancer.java 558 2008-03-14 14:14:48Z imiller $
 */
public class LoopConditionalBranchBalancer implements Visitor
{
    private static final String _RCS_ = "$Rev: 558 $";

    private Set<Latency> currentLatencies = new HashSet();
    private Stack<Set<Latency>> context = new Stack();
    private boolean isInLoopContext = false;
    
    public void visit (Branch branch)
    {
        // Only do the analysis in the context of a loop, otherwise we
        // are slowing down the processing needlessly.
        if (this.isInLoopContext)
        {
            enterModule();
            
            // Handle the true and false seperately so that we can see
            // what the latencies in those blocks are.
            enterModule();
            branch.getTrueBranch().accept(this);
            Set trueLatencies = exitModule(branch.getTrueBranch());
            
            enterModule();
            branch.getFalseBranch().accept(this);
            Set falseLatencies = exitModule(branch.getFalseBranch());
            
            final boolean trueComb = isCombinational(trueLatencies);
            final boolean falseComb = isCombinational(falseLatencies);
            final boolean trueSeq = isSequential(trueLatencies);
            final boolean falseSeq = isSequential(falseLatencies);

            if (_loop.db)
            {
                _loop.ln("Analyzing " + branch + " of " + branch.showOwners());
                _loop.ln("\ttrueComb " + trueComb + " trueSeq " + trueSeq);
                _loop.ln("\ttrue latencies " + trueLatencies);
                _loop.ln("\tfalseComb " + falseComb + " falseSeq " + falseSeq);
                _loop.ln("\tfalse latencies " + falseLatencies);
            }
            
            if (((trueComb && falseComb) && (!trueSeq && !falseSeq)) ||
                ((!trueComb && !falseComb) && (trueSeq && falseSeq)))
            {
                // do nothing
            }
            else if (trueComb && falseSeq)
            {
                if (_loop.db) _loop.ln("Delaying true branch of " + branch + " " + branch.showOwners());
                // delay the true path
                delayModule(branch.getTrueBranch());
                // Ensure that we mark the fact that this branch now takes
                // at least 1 cycle.
                this.currentLatencies.add(Latency.ONE);
            }
            else if (trueSeq && falseComb)
            {
                if (_loop.db) _loop.ln("Delaying false branch of " + branch + " " + branch.showOwners());
                // delay the false path
                delayModule(branch.getFalseBranch());
                // Ensure that we mark the fact that this branch now takes
                // at least 1 cycle.
                this.currentLatencies.add(Latency.ONE);
            }
            else
            {
                if (_loop.db) _loop.ln("Delaying entire branch " + branch + " " + branch.showOwners());
                // delay the whole branch
                delayModule(branch);
                // Ensure that we mark the fact that this branch now takes
                // at least 1 cycle.
                this.currentLatencies.add(Latency.ONE);
            }
            
            enterModule();
            branch.getDecision().accept(this);
            exitModule(branch.getDecision());
            
            exitModule(branch);
        }
        else
        {
            handleModule(branch);
        }
    }

    /**
     * Create a 1 cycle delay resource dependency between the GO and
     * DONE of the specified module.
     */
    private static void delayModule (Module module)
    {
        assert module.getExits().size() == 1 : "Can only delay modules with one (done) exit";
        final Exit exit = module.getExit(Exit.DONE);
        assert exit != null : "Module did not have done exit in delayModule";

        final OutBuf outbuf = exit.getPeer();
        final Entry outBufEntry = (Entry)outbuf.getEntries().get(0);
        outBufEntry.addDependency(outbuf.getGoPort(), new ResourceDependency(module.getGoPort().getPeer(), 1));
    }

    /**
     * Return true if ALL latencies in the set have fixed latency of 0.
     */
    private static boolean isCombinational (Set latencies)
    {
        for (Iterator iter = latencies.iterator(); iter.hasNext();)
        {
            Latency lat = (Latency)iter.next();
            if (!lat.equals(Latency.ZERO))
                return false;
        }
        return true;
    }

    /**
     * Returns true if there is ANY sequential latency in the set.
     */
    private static boolean isSequential (Set latencies)
    {
        for (Iterator iter = latencies.iterator(); iter.hasNext();)
        {
            Latency lat = (Latency)iter.next();
            if (lat.getMinClocks() > 0)
                return true;
        }
        return false;
    }

    /**
     * Capture the latency of this component
     */
    private void mark (Component comp)
    {
        // Add the latency of all exits
        for (Iterator iter = comp.getExits().iterator(); iter.hasNext();)
        {
            Exit exit = (Exit)iter.next();
            if (exit.getLatency() != null)
                currentLatencies.add(exit.getLatency());
            else
                System.out.println("WARNING!!! The latency of exit " + exit+ " of " + comp + " is null");
        }
    }

    private void enterModule ()
    {
        this.context.push(this.currentLatencies);
        this.currentLatencies = new HashSet();
    }

    private Set exitModule (Module module)
    {
        // All we care about is the difference between sequential and
        // non sequential.
        final Map allLatencies = new HashMap();
        for (Latency lat : this.currentLatencies)
        {
            allLatencies.put(lat, lat);
        }
        final Map maxLatencies = Latency.getLatest(allLatencies);
        final Set max = new HashSet(maxLatencies.values());
        
        // Also pick up any dependencies
        for (Component comp : module.getComponents())
        {
            for (Entry entry : comp.getEntries())
            {
                for (Dependency dep : entry.getDependencies(comp.getGoPort()))
                {
                    if (dep.getDelayClocks() > 0)
                        max.add(Latency.ONE);
                }
            }
        }
        
        this.currentLatencies = context.pop();
        this.currentLatencies.addAll(max);

        return new HashSet(max);
    }
    
    private void handleModule (Module mod)
    {
        enterModule();
        for (Component component : mod.getComponents())
        {
            component.accept(this);
        }
        exitModule(mod);
    }
    
    
    
    public void visit (Design design)
    {
        for (Task task : design.getTasks()) { task.accept(this); }
    }
    
    public void visit (Task task)
    {
        this.isInLoopContext = false;
        if (task.getCall() != null)
            task.getCall().accept(this);
    }
    
    public void visit (Call call)
    {
        mark(call);
        if (call.getProcedure() != null)
            call.getProcedure().accept(this);
    }
    public void visit (Procedure procedure)
    {
        if (procedure.getBody() != null)
            procedure.getBody().accept(this);
    }

    public void visit (Block block) { handleModule(block); }
    public void visit (Loop loop)
    {
        boolean oldContext = this.isInLoopContext;
        this.isInLoopContext = true;
        handleModule(loop);
        this.isInLoopContext = oldContext;
    }
    public void visit (WhileBody mod)  { handleModule(mod); }
    public void visit (UntilBody mod)  { handleModule(mod); }
    public void visit (ForBody mod)    { handleModule(mod); }
    public void visit (Decision mod)   { handleModule(mod); }
    public void visit (Switch mod)     { handleModule(mod); }
    public void visit (HeapRead mod)   { handleModule(mod); }
    public void visit (HeapWrite mod)  { handleModule(mod); }
    public void visit (ArrayRead mod)  { handleModule(mod); }
    public void visit (ArrayWrite mod) { handleModule(mod); }
    public void visit (TaskCall comp)
    {
        handleModule(comp.getTarget().getCall().getProcedure().getBody());
    }

    public void visit (AddOp comp) { mark(comp); }
    public void visit (AndOp comp) { mark(comp); }
    public void visit (CastOp comp) { mark(comp); }
    public void visit (ComplementOp comp) { mark(comp); }
    public void visit (ConditionalAndOp comp) { mark(comp); }
    public void visit (ConditionalOrOp comp) { mark(comp); }
    public void visit (Constant comp) { mark(comp); }
    public void visit (DivideOp comp) { mark(comp); }
    public void visit (EqualsOp comp) { mark(comp); }
    public void visit (GreaterThanEqualToOp comp) { mark(comp); }
    public void visit (GreaterThanOp comp) { mark(comp); }
    public void visit (Latch vis) { mark(vis); }
    public void visit (LeftShiftOp comp) { mark(comp); }
    public void visit (LessThanEqualToOp comp) { mark(comp); }
    public void visit (LessThanOp comp) { mark(comp); }
    public void visit (LocationConstant comp) { mark(comp); }
    public void visit (MinusOp comp) { mark(comp); }
    public void visit (ModuloOp comp) { mark(comp); }
    public void visit (MultiplyOp comp) { mark(comp); }
    public void visit (NotEqualsOp comp) { mark(comp); }
    public void visit (NotOp comp) { mark(comp); }
    public void visit (OrOp comp) { mark(comp); }
    public void visit (PlusOp comp) { mark(comp); }
    public void visit (ReductionOrOp comp) { mark(comp); }
    public void visit (RightShiftOp comp) { mark(comp); }
    public void visit (RightShiftUnsignedOp comp) { mark(comp); }
    public void visit (ShortcutIfElseOp comp) { mark(comp); }
    public void visit (SubtractOp comp) { mark(comp); }
    public void visit (NumericPromotionOp comp) { mark(comp); }
    public void visit (XorOp comp) { mark(comp); }
    public void visit (InBuf comp) { mark(comp); }
    public void visit (OutBuf comp) { mark(comp); }
    public void visit (Reg comp) { mark(comp); }
    public void visit (Mux comp) { mark(comp); }
    public void visit (EncodedMux comp) { mark(comp); }
    public void visit (PriorityMux comp) { mark(comp); }
    public void visit (And comp) { mark(comp); }
    public void visit (Not comp) { mark(comp); }
    public void visit (Or comp) { mark(comp); }
    public void visit (NoOp comp) { mark(comp); }
    public void visit (TimingOp comp) { mark(comp); }
    public void visit (RegisterRead comp) { mark(comp); }
    public void visit (RegisterWrite comp) { mark(comp); }
    public void visit (MemoryRead comp) { mark(comp); }
    public void visit (MemoryWrite comp) { mark(comp); }
    public void visit (AbsoluteMemoryRead comp) { mark(comp); }
    public void visit (AbsoluteMemoryWrite comp) { mark(comp); }
    public void visit (SRL16 comp) { mark(comp); }

    public void visit (SimplePinAccess comp) { mark(comp); }
    public void visit (SimplePinRead comp) { mark(comp); }
    public void visit (SimplePinWrite comp) { mark(comp); }
    public void visit (FifoAccess comp) { mark(comp); }
    public void visit (FifoRead comp) { mark(comp); }
    public void visit (FifoWrite comp) { mark(comp); }


    /**
     * Unexpected elements
     */
    public void visit (IPCoreCall vis) { fail(vis); }
    public void visit (Scoreboard vis) { fail(vis); }
    public void visit (RegisterGateway vis) { fail(vis); }
    public void visit (RegisterReferee vis) { fail(vis); }
    public void visit (MemoryReferee vis) { fail(vis); }
    public void visit (MemoryGateway vis) { fail(vis); }
    public void visit (MemoryBank vis) { fail(vis); }
    public void visit (Kicker vis) { fail(vis); }
    public void visit (PinRead vis) { fail(vis); }
    public void visit (PinWrite vis) { fail(vis); }
    public void visit (PinStateChange vis) { fail(vis); }
    public void visit (PinReferee vis) { fail(vis); }
    public void visit (TriBuf vis) { fail(vis); }
    public void visit (SimplePin vis) { fail(vis); }
    public void visit (EndianSwapper vis) { fail(vis); }

    protected void fail (Visitable vis)
    {
        if (vis instanceof Component)
            System.out.println(((Component)vis).showOwners());
    	EngineThread.getEngine().fatalError("Internal error at: LoopConditionalBranchBalancer.  Unexpected traversal of " + vis + " encountered");
    }
    
}
