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

import java.io.*;
import java.util.*;

import net.sf.openforge.app.EngineThread;
import net.sf.openforge.lim.*;
import net.sf.openforge.lim.memory.*;
import net.sf.openforge.verilog.mapping.*;
import net.sf.openforge.verilog.mapping.memory.*;


/**
 * A visitor that travserses through a Design and collect the resource
 * consumsion information for each individual procedure, task, and design.
 *
 * @author    cwu
 * @version   $Id: HardwareResourceUtilizer.java 2 2005-06-09 20:00:48Z imiller $
 */
public class HardwareResourceUtilizer extends FilteredVisitor
{
    private final static String _RCS_ = "RCS_REVISION: $Rev: 2 $";

    private FPGAResource designResourceUsage;
    private FPGAResource currentResourceUsage;
    private Map procedureToResourceUsageMap;
    private Stack procedureResourceUsageStack;
    
    private ResourceUtilizationReporter rerourceUtilizationWriter;
    
    public HardwareResourceUtilizer (Design design, FileOutputStream fos)
    {
        super();
        this.procedureToResourceUsageMap = new HashMap();
        visit(design);
        this.rerourceUtilizationWriter = new ResourceUtilizationReporter(design, designResourceUsage, procedureToResourceUsageMap, fos);
    }
    
    /**
     * Visit the Task(s) according to TaskResources in a DesignResource 
     *
     * @param design a LIM design
     */
    public void visit(Design design)
    {
        this.designResourceUsage = new FPGAResource();
        this.currentResourceUsage = new FPGAResource();
        
        // Identify whether a design needs clock or not  
        if (design.consumesClock())
        {
            // FIXME ABK: unclear usage, addClock(number of clocks to add?)
            //this.designResourceUsage.addClock(1);
            this.designResourceUsage.addClock(design.getClockPins().size());
        }

        // Add IBUF resource usage
        for (Iterator inPinIter = design.getInputPins().iterator(); inPinIter.hasNext();)
        {
            InputPin inPin = (InputPin)inPinIter.next();
            this.designResourceUsage.addIB(inPin.getWidth());
        }
        // Add OBUF resource usage
        for (Iterator outPinIter = design.getOutputPins().iterator(); outPinIter.hasNext();)
        {
            OutputPin outPin = (OutputPin)outPinIter.next();
            this.designResourceUsage.addOB(outPin.getWidth());
        }
        // Add IOBUF resource usage
        for (Iterator inoutPinIter = design.getBidirectionalPins().iterator(); inoutPinIter.hasNext();)
        {
            BidirectionalPin inoutPin = (BidirectionalPin)inoutPinIter.next();
            this.designResourceUsage.addIOB(inoutPin.getWidth());
        }
        
        // Add design's memories LUT resource usage
        for (Iterator memoryIter = design.getLogicalMemories().iterator(); memoryIter.hasNext();)
        {
            LogicalMemory memory = (LogicalMemory)memoryIter.next();
            for (Iterator memports = memory.getLogicalMemoryPorts().iterator(); memports.hasNext();)
            {
                LogicalMemoryPort memport = (LogicalMemoryPort)memports.next();
                MemoryReferee referee = memport.getReferee();
                Collection components = new HashSet(referee.getComponents());
                components.remove(referee.getInBuf());
                components.removeAll(referee.getOutBufs());
                for (Iterator it = components.iterator(); it.hasNext();)
                {
                    Visitable vis = (Visitable)it.next();
                    vis.accept(this);
                }
            }
            StructuralMemory structMem = memory.getStructuralMemory();
            if (structMem != null)
            {
                Collection comps = new HashSet(structMem.getComponents());
                comps.remove(structMem.getInBuf());
                comps.removeAll(structMem.getOutBufs());
                for (Iterator iter = comps.iterator(); iter.hasNext();)
                {
                    ((Visitable)iter.next()).accept(this);
                }
            }
        }
        
        // Add design's global registers FlipFlop resource usage
        for (Iterator registerIter = design.getRegisters().iterator(); registerIter.hasNext();)
        {
            Register reg = (Register)registerIter.next();
            // XXX FIXME.  Register.Physical has no visitor support.
            Module connector =  (Module)reg.getPhysicalComponent();
            if (connector != null)
            {
                if (!(connector instanceof Register.Physical))
                {
                    connector.accept(this);
                }
                else
                {
                    int careBitCount = reg.getInitWidth();
                    this.currentResourceUsage.addFlipFlop(careBitCount);
                }
            }
        }
        
        this.designResourceUsage.addResourceUsage(this.currentResourceUsage);
        
        super.visit(design);
        
        // Sum up the resource usage of each task as a design's
        // resource usage
        for (Iterator taskIter = design.getTasks().iterator(); taskIter.hasNext();)
        {
            Task task = (Task)taskIter.next();
            Call taskCall = task.getCall();
            Procedure topProcedure = taskCall.getProcedure();
            this.designResourceUsage.addResourceUsage((FPGAResource)this.procedureToResourceUsageMap.get(topProcedure));
        }
    }

    public void visit(Task task)
    {
        this.procedureResourceUsageStack = new Stack();
        super.visit(task);
    }

    public void visit (Call call)
    {
        if (call.getOwner() != null)
        {
            this.procedureResourceUsageStack.push(this.currentResourceUsage);
        }

        super.visit(call);
        
        if (call.getOwner() != null)
        {
            FPGAResource callResource = this.currentResourceUsage;
            this.currentResourceUsage = (FPGAResource)procedureResourceUsageStack.pop();
            this.currentResourceUsage.addResourceUsage(callResource);
        }
    }

    public void visit (Procedure procedure)
    {
        this.currentResourceUsage = new FPGAResource();
        
        if (!procedureToResourceUsageMap.containsKey(procedure))
        {
            super.visit(procedure);
            this.procedureToResourceUsageMap.put(procedure, this.currentResourceUsage);
        }
    }
    
    public void visit (Latch latch)
    {
        for(Iterator it = latch.getComponents().iterator(); it.hasNext();)
        {
            ((Component)it.next()).accept(this);
        }
    }
 
    public void visit (PinReferee pref)
    {
        for(Iterator it = pref.getComponents().iterator(); it.hasNext();)
        {
            Component c = (Component)it.next();
            c.accept(this);
        }
    }

    public void visit (Kicker kicker)
    {
        for(Iterator it = kicker.getComponents().iterator(); it.hasNext();)
        {
            ((Component)it.next()).accept(this);
        }
    }
    
    public void visit (Scoreboard scoreboard)
    {
        for(Iterator it = scoreboard.getComponents().iterator(); it.hasNext();)
        {
            ((Component)it.next()).accept(this);
        }
    }

    public void visit (MemoryBank memBank)
    {
        VerilogMemory vm = MemoryMapper.getMemoryType(memBank);
        Ram match = vm.getLowestCost(Ram.getMappers(EngineThread.getGenericJob().getPart(CodeLabel.UNSCOPED), memBank.getImplementation().isLUT()));
        int new_width = (int)java.lang.Math.ceil((double)vm.getDataWidth()/(double)match.getWidth());
        int new_depth = (int)java.lang.Math.ceil((double)vm.getDepth()/(double)match.getDepth());
        if (match instanceof LutRam)
        {
            this.currentResourceUsage.addSpLutRam(match.getCost() * new_width * new_depth);
        }
        else if (match instanceof DualPortLutRam)
        {
            this.currentResourceUsage.addDpLutRam(match.getCost() * new_width * new_depth);
        }
        else if (match instanceof BlockRam)
        {
            this.currentResourceUsage.addSpBlockRam(match.getCost() * new_width * new_depth);
        }
        else if (match instanceof DualPortBlockRam)
        {
            this.currentResourceUsage.addDpBlockRam(match.getCost() * new_width * new_depth);
        }
        else
        {
            this.currentResourceUsage.addRom(match.getCost() * new_width * new_depth);
        }
    }
   
    public void visit (MemoryRead memoryRead)
    {
        MemoryRead.Physical physical = (MemoryRead.Physical)memoryRead.getPhysicalComponent();
        Collection components = physical.getComponents();
        components.remove(physical.getInBuf());
        components.removeAll(physical.getOutBufs());
        for (Iterator it = components.iterator(); it.hasNext();)
        {
            ((Visitable)it.next()).accept(this);   
        }
    }
    
    public void visit (MemoryWrite memoryWrite)
    {
        MemoryWrite.Physical physical = (MemoryWrite.Physical)memoryWrite.getPhysicalComponent();
        Collection components = physical.getComponents();
        components.remove(physical.getInBuf());
        components.removeAll(physical.getOutBufs());
        for (Iterator it = components.iterator(); it.hasNext();)
        {
            ((Visitable)it.next()).accept(this);   
        }
    }

    /**
     * Handle any kind of {@link Module}. All visit(Module subclass) methods
     * should call this as part of the visit. The default behavior of this
     * method is to call filterAny(Component).
     */
    public void filter(Module m)
    {
        if (_report.db) _lim.ln("filter(Module): " + m.toString());
        filterAny(m);
    }

    public void preFilter(Module m)
    {
        if (_report.db) _lim.ln("pre-filter(Module): " + m.toString());
        preFilterAny(m);
    }
    
    
    /**
     * Handle any kind of {@link Operation}. All visit(Operation subclass)
     * methods should call this as part of the visit.
     */
    public void filter(Operation o)
    {
        if (_report.db) _lim.ln("filter(Operation): " + o.toString());
        filterAny(o);
    }
    
    public void preFilter(Operation o)
    {
        if (_report.db) _lim.ln("pre-filter(Operation): " + o.toString());
        preFilterAny(o);
    }
    
    /**
     * Handle any kind of {@link Primitive}. All visit(Primitive subclass)
     * methods should call this as part of the visit.
     */
    public void filter(Primitive p)
    {
        if (_report.db) _lim.ln("filter(Primitive): " + p.toString());
        filterAny(p);
    }

    public void preFilter(Primitive p)
    {
        if (_report.db) _lim.ln("pre-filter(Primitive): " + p.toString());
        preFilterAny(p);
    }
    
    /**
     * Handles {@link Call}. The visit(Call call) calls this as part of the visit
     */
    public void filter(Call c)
    {
        if (_report.db) _lim.ln("filter(Call): " + c.toString());
        filterAny(c);
    }
    
    public void preFilter(Call c)
    {
        if (_report.db) _lim.ln("pre-filter(Call): " + c.toString());
        preFilterAny(c);
    }
    
    /**
     * The default behavior of each filter() method is to call this, allowing
     * any component to be handled generically.
     *
     * @param c the component that was visited
     */
    public void filterAny(Component c)
    {
        if (_report.db) _lim.ln("filterAny(Component): " + c.toString());
        this.currentResourceUsage.addResourceUsage(c.getHardwareResourceUsage());
    }

    public void preFilterAny(Component c)
    {
        if (_report.db) _lim.ln("pre-filterAny(Component): " + c.toString());
    }

    protected void traverse(Design d)
    {
        if (_report.db) _lim.ln("FilteredScan.traverse(Design)");
        scanner.enter(d);
    }
    
    protected void traverse(Task t)
    {
        if (_report.db) _lim.ln("FilteredScan.traverse(Task)");
        scanner.enter(t);
    }
    
    protected void traverse(Call c)
    {
        if (_report.db) _lim.ln("FilteredScan.traverse(Call)");
        scanner.enter(c);
    }
    
    protected void traverse(Procedure p)
    {
        if (_report.db) _lim.ln("FilteredScan.traverse(Procedure)");
        scanner.enter(p);
    }

    public Map getProcedureResourceUsageMap ()
    {
        return this.procedureToResourceUsageMap;
    }
}
