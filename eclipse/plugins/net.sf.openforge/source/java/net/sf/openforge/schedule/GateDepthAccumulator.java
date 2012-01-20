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
import net.sf.openforge.lim.io.*;
import net.sf.openforge.lim.memory.*;
import net.sf.openforge.lim.op.*;

/**
 * GateDepthAccumulator calculates the maximum gate depth for each task in a
 * {@link Design} by adding up the gate depth of each LIM {@link
 * Component} in the date path on the task level. The largest number is
 * chosen to be the maximum gate depth for that particular task.
 *
 * Created: Tue Dec 17 14:22:05 2002
 *
 * @author cwu
 * @version $Id: GateDepthAccumulator.java 88 2006-01-11 22:39:52Z imiller $
 */
class GateDepthAccumulator extends DataFlowVisitor
{
    private static final String _RCS_ = "$Rev: 88 $";
    
    /** Map of Exit to Integer object of cumulative gate depth */
    private Map exitToGateDepthMap;
    
    private Stack unresolvedGateDepthComponents;

    private int taskMaxGateDepth;
    
    private int designMaxGateDepth;
    
    private int unbreakableGateDepth;
    
    public static int getUnbreakableGateDepth(Design design)
    {
        design.accept(new GateDepthAccumulator());
        return design.getUnbreakableGateDepth();
    }
    
    /**
     * Constructor
     */
    GateDepthAccumulator ()
    {
        super();
        // set the data connection flow to forward mode.
        setRunForward(true);
    }

    protected void preFilter (Design node)
    {
        if (_schedule.db) _schedule.ln(_schedule.GDA, "\tDesign PreFilter " + node);
        preFilterAny(node);
    }
    
    protected void preFilter (Task node)
    {
        if (_schedule.db) _schedule.ln(_schedule.GDA, "\tTask PreFilter " + node);
        preFilterAny(node);
    }
    
    protected void preFilter (Procedure node)
    {
        if (_schedule.db) _schedule.ln(_schedule.GDA, "\tProcedure PreFilter " + node);
        preFilterAny(node);
    }
    
    protected void preFilter (Component node)
    {
        if (_schedule.db) _schedule.ln(_schedule.GDA, "\tComponent PreFilter " + node);
        preFilterAny(node);
    }
    
    protected void preFilter (Visitable node)
    {
        if (_schedule.db) _schedule.ln(_schedule.GDA, "\tVisitable PreFilter " + node);
        preFilterAny(node);
    }

    protected void preFilterAny (Component node)
    {
        if (_schedule.db) _schedule.ln(_schedule.GDA, "\tComponent PreFilterAny " + node);
    }
    
    protected void preFilterAny (Visitable node)
    {
        if (_schedule.db) _schedule.ln(_schedule.GDA, "\tNON-Component PreFilterAny " + node);
    }

    protected void postFilter (Design node)
    {
        if (_schedule.db) _schedule.ln(_schedule.GDA, "\tDesign PostFilter " + node);
        postFilterAny(node);
    }
    
    protected void postFilter (Task node)
    {
        if (_schedule.db) _schedule.ln(_schedule.GDA, "\tTask PostFilter " + node);
        postFilterAny(node);
    }
    
    protected void postFilter (Procedure node)
    {
        if (_schedule.db) _schedule.ln(_schedule.GDA, "\tProcedure PostFilter " + node);
        postFilterAny(node);
    }
    
    protected void postFilter (Component node)
    {
        if (_schedule.db) _schedule.ln(_schedule.GDA, "\tComponent PostFilter " + node);
        postFilterAny(node);
    }
    
    protected void postFilter (Visitable node)
    {
        if (_schedule.db) _schedule.ln(_schedule.GDA, "\tVisitable PostFilter " + node);
        postFilterAny(node);
    }

    protected void postFilterAny (Component node)
    {
        if (_schedule.db) _schedule.ln(_schedule.GDA, "\tComponent PostFilterAny " + node);
    }

    protected void postFilterAny (Visitable node)
    {
        if (_schedule.db) _schedule.ln(_schedule.GDA, "\tNON-Component PostFilterAny " + node);
    }

    /**
     * Responsible for traversing within a design
     */
    protected void traverse (Design node)
    {
        if (_schedule.db) _schedule.ln(_schedule.GDA, "Traversing Design " + node);
        for (Iterator iter = node.getTasks().iterator(); iter.hasNext();)
        {
            ((Visitable)iter.next()).accept(this);
        }
    }
    
    /**
     * Responsible for traversing within a Task
     */
    protected void traverse (Task node)
    {
        if (_schedule.db) _schedule.ln(_schedule.GDA, "Traversing task " + node);
        if (node.getCall() != null)
        {
            node.getCall().accept(this);
        }
    }
    
    /**
     * Responsible for traversing within a Call
     */
    protected void traverse (Call node)
    {
        if (_schedule.db) _schedule.ln(_schedule.GDA, "Traversing Call " + node);
        if (node.getProcedure() != null)
        {
            node.getProcedure().accept(this);
        }
    }
    
    /**
     * Responsible for traversing within a Procedure
     */
    protected void traverse (Procedure node)
    {
        if (_schedule.db) _schedule.ln(_schedule.GDA, "Traversing Procedure " + node);
        if (node.getBody() != null)
        {
            node.getBody().accept(this);
        }
    }
    
    /**
     * Responsible for traversing within a Module
     */
    protected void traverse (Module module)
    {
        // Define all feedback points as having 0 depth on their exits
        // in order to break the iterative computation.
        for (Iterator iter = module.getFeedbackPoints().iterator(); iter.hasNext();)
        {
            Component comp = (Component)iter.next();
            for (Iterator exitIter = comp.getExits().iterator(); exitIter.hasNext();)
            {
                Exit exit = (Exit)exitIter.next();
                this.exitToGateDepthMap.put(exit, new Integer(0));
            }
        }
        
        if (!findUnknownGateDepthOnInputs(module))
        {
            if (_schedule.db) _schedule.ln(_schedule.GDA, "Module Traversal " + module);
            if (isForward())
            {
                traverseModuleForward(module, module.getFeedbackPoints());
            }
            else
            {
                traverseModuleReverse(module, module.getFeedbackPoints());
            }

            LinkedList revisitComponents = new LinkedList();
            while (true)
            {
                while(!this.unresolvedGateDepthComponents.isEmpty())
                {
                    if (((Component)this.unresolvedGateDepthComponents.peek()).getOwner() == module)
                    {
                        revisitComponents.add(this.unresolvedGateDepthComponents.pop());
                    }
                    else
                    {
                        break;
                    }
                }
                if (revisitComponents.isEmpty())
                {
                    break;
                }
                revisitUnknownGateDepthComponents(revisitComponents);
            }
        }
    }

    /**
     * Responsible for traversing in a Block
     */
    protected void traverse (Block module)
    {
        traverse((Module)module);
    }
    
    /**
     * Traverses the {@link PinAccess#getPhysicalComponent} if it
     * exists.
     */
    protected void traverse (PinAccess node)
    {
        if (_schedule.db) _schedule.ln(_schedule.GDA, "PinAccess Traversal " + node);
        if (node.hasPhysicalComponent())
        {
            traverse(node.getPhysicalComponent());
        }
    }
    
    /**
     * Traverses the {@link MemoryAccess#getPhysicalComponent} if it
     * exists.
     */
    protected void traverse (MemoryAccess node)
    {
        if (_schedule.db) _schedule.ln(_schedule.GDA, "MemoryAccess Traversal " + node);
        if (node.hasPhysicalComponent())
        {
            traverse(node.getPhysicalComponent());
        }
    }
    
    protected void traverse (Component node)
    {
        if (_schedule.db) _schedule.ln(_schedule.GDA, "Component Traversal " + node);
    }
    
    protected void traverse (Visitable node)
    {
        if (_schedule.db) _schedule.ln(_schedule.GDA, "NON-Component Traversal " + node);
    }

    public void visit (Design design)
    {
        preFilter(design);
        // initialize a design's gate depth
        this.designMaxGateDepth = 0;
        this.unbreakableGateDepth = 0;
        traverse(design);
        // set a design's max gate depth
        design.setMaxGateDepth(this.designMaxGateDepth);
        if (_schedule.db) _schedule.ln(_schedule.GDA, design + ", maximum gate depth: " + design.getMaxGateDepth());
        design.setUnbreakableGateDepth(this.unbreakableGateDepth);
        if (_schedule.db) _schedule.ln(_schedule.GDA, design + ", maximum unbreakable gate depth: " + design.getUnbreakableGateDepth());
        postFilter(design);
        
    }
    
    public void visit (Task task)
    {
        preFilter(task);
        // clear the field variables which store information within a task scope.
        this.exitToGateDepthMap = new HashMap();
        this.unresolvedGateDepthComponents = new Stack();
        this.taskMaxGateDepth = 0;
        traverse(task);
        // set a task's max gate depth
        task.setMaxGateDepth(this.taskMaxGateDepth);
        if (_schedule.db) _schedule.ln(_schedule.GDA, task + ", maximum gate depth: " + task.getMaxGateDepth());
        // set design's gate depth to be the largest gate depth among it's tasks.
        if (this.taskMaxGateDepth >= this.designMaxGateDepth)
        {
            this.designMaxGateDepth = this.taskMaxGateDepth;
        }
        postFilter(task);
    }
    
    public void visit (Call call)
    {
        // Do not checck the call's port connections and gate depth on it's
        // inputs for Entry level call, for other type of calls which
        // should be inside a block have to pass the input gate depth
        // verification before traversing the corresponding procedures.
        if (call.getOwner() == null || call.getOwner() instanceof Design.DesignModule || !findUnknownGateDepthOnInputs(call))
        {
            preFilter(call);
            // propagate the gate depth to the procedure ports peer
            // buses which belong to a procedure body's inBuf. 
            Integer currentInputGateDepth = (call.getOwner() != null) ? new Integer(getMaxInputGateDepth(call)) : new Integer(0);
            for (Iterator portIter = call.getPorts().iterator(); portIter.hasNext();)
            {
                Port callPort = (Port)portIter.next();
                Bus peerBus = call.getProcedurePort(callPort).getPeer();
                this.exitToGateDepthMap.put(peerBus.getOwner(), currentInputGateDepth);
            }
            traverse(call);
            for (Iterator callExitIter = call.getExits().iterator(); callExitIter.hasNext();)
            {
                Exit callExit = (Exit)callExitIter.next();
                Integer currentOutputGateDepth = (Integer)this.exitToGateDepthMap.get(call.getProcedureExit(callExit));
                this.exitToGateDepthMap.put(callExit, currentOutputGateDepth);
            }
            postFilter(call);
        }
    }
    
    public void visit (Procedure procedure)
    {
        preFilter(procedure);
        traverse(procedure);
        postFilter(procedure);
    }
    
    public void visit (InBuf inBuf)
    {
        // The gate depth of an inbuf inside a procedure's body has
        // been propagated on visiting it's call, so we only need to
        // determine the gate depth for other kinds of inBuf.
        if (!(inBuf.getOwner() instanceof Block) || !((Block)inBuf.getOwner()).isProcedureBody())
        {
            if (!findUnknownGateDepthOnInputs(inBuf.getOwner()))
            {
                preFilter(inBuf);
                traverse(inBuf);
                final Integer currentGateDepth = new Integer(getMaxInputGateDepth(inBuf)+inBuf.getExitGateDepth());
                for (Iterator busIter = inBuf.getBuses().iterator(); busIter.hasNext();)
                {
                    final Bus bus = (Bus)busIter.next();
                    this.exitToGateDepthMap.put(bus.getOwner(), currentGateDepth);
                }
                if (currentGateDepth.intValue() >= this.taskMaxGateDepth)
                {
                    this.taskMaxGateDepth = currentGateDepth.intValue();
                }
                if (_schedule.db) _schedule.ln(_schedule.GDA, inBuf + ", current gate depth: " + currentGateDepth.intValue());
                postFilter(inBuf);
            }
        }
        else
        {
            // Gate depth has been propagated, just put some message
            // here for debugging purpose only.
            preFilter(inBuf);
            traverse(inBuf);
            if (_schedule.db) _schedule.ln(_schedule.GDA,
                inBuf + ", current gate depth: " +
                ((Integer)this.exitToGateDepthMap.get(inBuf.getExit(Exit.DONE))).intValue());
            postFilter(inBuf);
        }
    }
    
    public void visit (OutBuf outBuf)
    {
        if (!findUnknownGateDepthOnInputs(outBuf))
        {
            preFilter(outBuf);
            traverse(outBuf);
            final Integer currentGateDepth = new Integer(getMaxInputGateDepth(outBuf)+outBuf.getExitGateDepth());
            for (Iterator portIter = outBuf.getPorts().iterator(); portIter.hasNext();)
            {
                final Bus moduleBus = ((Port)portIter.next()).getPeer();
                if (moduleBus != null)
                {
                    this.exitToGateDepthMap.put(moduleBus.getOwner(), currentGateDepth);
                }
            }
            if (currentGateDepth.intValue() >= this.taskMaxGateDepth)
            {
                this.taskMaxGateDepth = currentGateDepth.intValue();
            }
            if (_schedule.db) _schedule.ln(_schedule.GDA, outBuf + ", current gate depth: " + currentGateDepth.intValue());
            postFilter(outBuf);
        }
    }
    
    public void visit (Decision decision)
    {
        if (!findUnknownGateDepthOnInputs(decision))
        {
            super.visit(decision);
        }
    }
    
    public void visit (Loop loop)
    {
        /* 
         * artificially assign feedback exit to gate depth to 0 if the 
         * loop flop has been removed by loop flop optimization
         */
        if (loop.getControlRegister() == null)
        {
            this.exitToGateDepthMap.put(loop.getBody().getFeedbackExit(), new Integer(0));
        }

        if (!findUnknownGateDepthOnInputs(loop))
        {
            super.visit(loop);
        }
    }
    
    public void visit (WhileBody whileBody)
    {
        if (!findUnknownGateDepthOnInputs(whileBody))
        {
            super.visit(whileBody);
        }
    }

    public void visit (UntilBody untilBody)
    {
        if (!findUnknownGateDepthOnInputs(untilBody))
        {
            super.visit(untilBody);
        }
    }

    public void visit (ForBody forBody)
    {
        if (!findUnknownGateDepthOnInputs(forBody))
        {
            super.visit(forBody);
        }
    }
    
    public void visit (Block block)
    {
        if (!findUnknownGateDepthOnInputs(block))
        {
            super.visit(block);
        }
    }
    
    public void visit (Branch branch)
    {
        if (!findUnknownGateDepthOnInputs(branch))
        {
            super.visit(branch);
        }
    }
    
    public void visit (Latch latch)
    {
        if (!findUnknownGateDepthOnInputs(latch))
        {
            super.visit(latch);
        }
    }
    
    public void visit (Switch swith)
    {
        if (!findUnknownGateDepthOnInputs(swith))
        {
            super.visit(swith);
        }
    }
    
    public void visit (PriorityMux priorityMux)
    {
        if (!findUnknownGateDepthOnInputs(priorityMux))
        {
            super.visit(priorityMux);
        }
    }

    public void visit(Scoreboard scoreBoard)
    {
        if (!findUnknownGateDepthOnInputs(scoreBoard))
        {
            super.visit(scoreBoard);
        }
    }

    public void visit(RegisterGateway regGateway)
    {
        if (!findUnknownGateDepthOnInputs(regGateway))
        {
            super.visit(regGateway);
        }
    }
    
    public void visit(RegisterReferee regReferee)
    {
        if (!findUnknownGateDepthOnInputs(regReferee))
        {
            super.visit(regReferee);
        }
    }
    
    public void visit(MemoryReferee memReferee)
    {
        if (!findUnknownGateDepthOnInputs(memReferee))
        {
            super.visit(memReferee);
        }
    }
    
    public void visit(MemoryGateway memGateway)
    {
        if (!findUnknownGateDepthOnInputs(memGateway))
        {
            super.visit(memGateway);
        }
    }
    
    public void visit(MemoryBank comp)
    {
        if (!findUnknownGateDepthOnInputs(comp))
        {
            super.visit(comp);
        }
    }
    
    public void visit(HeapRead heapRead)
    {
        if (!findUnknownGateDepthOnInputs(heapRead))
        {
            super.visit(heapRead);
        }
    }
    
    public void visit(ArrayRead arrayRead)
    {
        if (!findUnknownGateDepthOnInputs(arrayRead))
        {
            super.visit(arrayRead);
        }
    }
    
    public void visit(HeapWrite heapWrite)
    {
        if (!findUnknownGateDepthOnInputs(heapWrite))
        {
            super.visit(heapWrite);
        }
    }
    
    public void visit(ArrayWrite arrayWrite)
    {
        if (!findUnknownGateDepthOnInputs(arrayWrite))
        {
            super.visit(arrayWrite);
        }
    }

    public void visit (AbsoluteMemoryRead absRead)
    {
        if (!findUnknownGateDepthOnInputs(absRead))
        {
            super.visit(absRead);
        }
    }
    
    public void visit (AbsoluteMemoryWrite absWrite)
    {
        if (!findUnknownGateDepthOnInputs(absWrite))
        {
            super.visit(absWrite);
        }
    }
    
    public void visit(Kicker kicker)
    {
        if (!findUnknownGateDepthOnInputs(kicker))
        {
            super.visit(kicker);
        }
    }
    
    public void visit(PinReferee pinReferee)
    {
        if (!findUnknownGateDepthOnInputs(pinReferee))
        {
            super.visit(pinReferee);
        }
    }

    public void visit (FifoAccess param1)
    {
        if (!findUnknownGateDepthOnInputs(param1))
        {
            super.visit(param1);
        }
    }
    
    public void visit (FifoRead param1)
    {
        if (!findUnknownGateDepthOnInputs(param1))
        {
            super.visit(param1);
        }
    }
    
    public void visit (FifoWrite param1)
    {
        if (!findUnknownGateDepthOnInputs(param1))
        {
            super.visit(param1);
        }
    }
    
        
    public void visit (Reg reg)
    {
        preFilter(reg);
        traverse(reg);
        final Integer currentGateDepth = new Integer(0);
        this.exitToGateDepthMap.put(((Bus)reg.getBuses().iterator().next()).getOwner(), currentGateDepth);
        postFilter(reg);
    }

    public void visit (Constant constant)
    {
        preFilter(constant);
        traverse(constant);
        final Integer currentGateDepth = new Integer(0);
        this.exitToGateDepthMap.put(((Bus)constant.getBuses().iterator().next()).getOwner(), currentGateDepth);
        postFilter(constant);
    }

    public void visit (EncodedMux encodedMux)
    {
        visit((Component)encodedMux);
    }

    public void visit (And and)
    {
        visit((Component)and);
    }

    public void visit (Not not)
    {
        visit((Component)not);
    }
    
    public void visit (Or or)
    {
        visit((Component)or);
    }

    public void visit (AddOp op)
    {
        visit((Component)op);
    }
    
    public void visit (AndOp op)
    {
        visit((Component)op);
    }

    public void visit (CastOp op)
    {
        visit((Component)op);
    }
    
    public void visit (ComplementOp op)
    {
        visit((Component)op);
    }
    
    public void visit (ConditionalAndOp op)
    {
        visit((Component)op);
    }
    
    public void visit (ConditionalOrOp op)
    {
        visit((Component)op);
    }
    
    public void visit (DivideOp op)
    {
        visit((Component)op);
    }

    public void visit (EqualsOp op)
    {
        visit((Component)op);
    }
    
    public void visit (GreaterThanEqualToOp op)
    {
        visit((Component)op);
    }
    
    public void visit (GreaterThanOp op)
    {
        visit((Component)op);
    }
    
    public void visit (LeftShiftOp op)
    {
        visit((Component)op);
    }
    
    public void visit (LessThanEqualToOp op)
    {
        visit((Component)op);
    }
    
    public void visit (LessThanOp op)
    {
        visit((Component)op);
    }

    public void visit (MinusOp op)
    {
        visit((Component)op);
    }
    
    public void visit (ModuloOp op)
    {
        visit((Component)op);
    }
    
    public void visit (MultiplyOp op)
    {
        visit((Component)op);
    }
    
    public void visit (NotEqualsOp op)
    {
        visit((Component)op);
    }
    
    public void visit (NoOp op)
    {
        visit((Component)op);
    }
    
    public void visit (TimingOp op)
    {
        visit((Component)op);
    }
    
    public void visit (NotOp op)
    {
        visit((Component)op);
    }
    
    public void visit (OrOp op)
    {
        visit((Component)op);
    }
    
    public void visit (PlusOp op)
    {
        visit((Component)op);
    }

    public void visit (ReductionOrOp op)
    {
        visit((Component)op);
    }
    
    public void visit (RightShiftOp op)
    {
        visit((Component)op);
    }
    
    public void visit (RightShiftUnsignedOp op)
    {
        visit((Component)op);
    }
    
    public void visit (ShortcutIfElseOp op)
    {
        visit((Component)op);
    }
    
    public void visit (SubtractOp op)
    {
        visit((Component)op);
    }

    public void visit (NumericPromotionOp op)
    {
        visit((Component)op);
    }
    
    public void visit (XorOp op)
    {
        visit((Component)op);
    }
    
    public void visit (Mux mux)
    {
        visit((Component)mux);
    } 

    public void visit (RegisterRead regRead)
    {
        visit((Component)regRead);
    }
    
    public void visit (RegisterWrite regWrite)
    {
        visit((Component)regWrite);
    }

    public void visit (MemoryRead memoryRead)
    {
        visit((Component)memoryRead);
    }
    
    public void visit (MemoryWrite memoryWrite)
    {
        visit((Component)memoryWrite);
    }
    
    public void visit(PinRead pinRead)
    {
        visit((Component)pinRead);
    }
    
    public void visit(PinWrite pinWrite)
    {
        visit((Component)pinWrite);
    }

    public void visit(PinStateChange pinStateChange)
    {
        visit((Component)pinStateChange);
    }
    
    public void visit(SRL16 srl16)
    {
        visit((Component)srl16);
    }

    public void visit(TriBuf triBuf)
    {
        visit((Component)triBuf);
    }

    public void visit (TaskCall param1)
    {
        if (!findUnknownGateDepthOnInputs(param1))
        {
            super.visit(param1);
        }
    }
    
    public void visit (SimplePinAccess param1)
    {
        if (!findUnknownGateDepthOnInputs(param1))
        {
            super.visit(param1);
        }
    }
    
    public void visit (SimplePinRead param1)
    {
        visit ((Component)param1);
    }
    
    public void visit (SimplePinWrite param1)
    {
        visit ((Component)param1);
    }
    
    public void visit (Component component)
    {
        this.unbreakableGateDepth = Math.max(this.unbreakableGateDepth, component.getExitGateDepth());

        if (!findUnknownGateDepthOnInputs(component))
        {
            preFilter(component);
            final Integer currentGateDepth = new Integer(getMaxInputGateDepth(component)+component.getExitGateDepth());
            for (Iterator busIter = component.getBuses().iterator(); busIter.hasNext();)
            {
                Bus bus = (Bus)busIter.next();
                this.exitToGateDepthMap.put(bus.getOwner(), currentGateDepth);
            }
            if (currentGateDepth.intValue() >= this.taskMaxGateDepth)
            {
                this.taskMaxGateDepth = currentGateDepth.intValue();
            }
            if (_schedule.db) _schedule.ln(_schedule.GDA, component + ", current gate depth: " + currentGateDepth.intValue());
            postFilter(component);
        }
    }

    private int getMaxInputGateDepth (Component component)
    {
        int maxGateDepth = 0;
        component = (component instanceof InBuf) ? component.getOwner() : component;
        for (Iterator portIter = component.getPorts().iterator(); portIter.hasNext();)
        {
            final Port port = (Port)portIter.next();
            if (port == component.getClockPort() ||
                port == component.getResetPort())
            {
                continue;
            }
            int gateDepth = port.isConnected() ? ((Integer)this.exitToGateDepthMap.get(port.getBus().getOwner())).intValue() : 0;
            maxGateDepth = Math.max(maxGateDepth, gateDepth);
        }
        return maxGateDepth;
    }

    private boolean findUnknownGateDepthOnInputs (Component component)
    {
        for (Iterator portIter = component.getPorts().iterator(); portIter.hasNext();)
        {
            final Port port = (Port)portIter.next();
//             assert (port.isConnected() &&
//                 this.exitToGateDepthMap.containsKey(port.getBus().getOwner()));
            if (port.isConnected() && !this.exitToGateDepthMap.containsKey(port.getBus().getOwner()))
            {
                this.unresolvedGateDepthComponents.push(component);
                return true;
            }
        }
        return false;
    }
    
    private void revisitUnknownGateDepthComponents (LinkedList revisitComponents)
    {
        while (!revisitComponents.isEmpty())
        {
            ((Component)revisitComponents.removeFirst()).accept(this);
        }
    }
}
