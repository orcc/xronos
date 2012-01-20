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

package net.sf.openforge.backend.timedc;


import java.util.*;
import java.io.*;

import net.sf.openforge.app.*;
import net.sf.openforge.app.project.*;
import net.sf.openforge.lim.*;
import net.sf.openforge.lim.io.*;
import net.sf.openforge.lim.memory.*;
import net.sf.openforge.lim.op.*;
import net.sf.openforge.util.naming.*;

/**
 * This class is responsible for finding all the nodes in the LIM
 * which maintain any sort of state.  Effectively this is all
 * nodes whose latency is greater than 0.  A unique OpHandle
 * subclass is created for each of these nodes which knows how to
 * handle declaration and updating of state.  
 */
class StateComponentFinder extends DefaultVisitor
{
    private CNameCache nameCache;
    private Map referenceableMap;
    
    /**
     * A Map of component to StateVar.
     */
    private Map seqElements = new LinkedHashMap();
    
    StateComponentFinder (CNameCache cache, Map refMap)
    {
        super();
        setTraverseComposable(true);
        this.nameCache = cache;
        this.referenceableMap = refMap;
    }

    Map getSeqElements () 
    {
        return Collections.unmodifiableMap(this.seqElements);
    }
    
    public void visit (Design design)
    {
        // The design module will contain the kickers.  No need to
        // visit them individually
        for (Iterator iter = design.getResetPins().iterator(); iter.hasNext();)
        {
            GlobalReset grst = (GlobalReset)iter.next();
            final File[] inputFiles = EngineThread.getGenericJob().getTargetFiles();
            int delay=5;
            if (ForgeFileTyper.isXLIMSource(inputFiles[0].getName()))
                delay = 10;
            this.seqElements.put(grst, new ResetVar(grst, this.nameCache, delay));
        }
        for (Iterator iter = design.getDesignModule().getComponents().iterator(); iter.hasNext();)
        {
            try
            {
                ((Visitable)iter.next()).accept(this);
            }
            // Anything that throws a UVE is not going to factor
            // into the c translation anyway.
            catch (UnexpectedVisitationException uve){}
        }
        // No need to call super if we visit everything in
        //designModule and we do not need to process the Task objects.
        //super.visit(design);
    }
    
    public void visit (Reg comp)
    {
        super.visit(comp);
        this.seqElements.put(comp, new RegVar(comp, this.nameCache));
    }
    
    public void visit (MemoryRead comp)
    {
        super.visit(comp);
        MemoryVar memVar = (MemoryVar)this.referenceableMap.get(comp.getMemoryPort().getLogicalMemory());
        this.seqElements.put(comp, new MemAccessVar(comp, memVar, this.nameCache));
    }
    
    public void visit (MemoryWrite comp)
    {
        super.visit(comp);
        MemoryVar memVar = (MemoryVar)this.referenceableMap.get(comp.getMemoryPort().getLogicalMemory());
        this.seqElements.put(comp, new MemAccessVar(comp, memVar, this.nameCache));
    }
    
    // No need for a stateful var for register reads.... they are
    // just a direct wire from the register.
    //public void visit (RegisterRead comp){}
    
    public void visit (RegisterWrite comp)
    {
        Register target = (Register)comp.getReferenceable();
        assert target != null : "null target of " + comp;
        RegisterVar registerVar = (RegisterVar)this.referenceableMap.get(target);
        if (registerVar == null)
        {
            registerVar = new RegisterVar(target);
            this.referenceableMap.put(target, registerVar);
        }
        RegWriteVar writeVar = new RegWriteVar(comp, registerVar, this.nameCache);
        this.seqElements.put(comp, writeVar);
    }
    
    public void visit (SRL16 comp)
    {
        super.visit(comp);
        this.seqElements.put(comp, new SRL16Var(comp, this.nameCache));
    }

    /*
    public void visit (Block vis) { super.visit(vis); filterAny(vis); }
    public void visit (Loop vis) { super.visit(vis); filterAny(vis); }
    public void visit (AddOp vis) { super.visit(vis); filterAny(vis); }
    public void visit (AndOp vis) { super.visit(vis); filterAny(vis); }
    public void visit (CastOp vis) { super.visit(vis); filterAny(vis); }
    public void visit (ComplementOp vis) { super.visit(vis); filterAny(vis); }
    public void visit (ConditionalAndOp vis) { super.visit(vis); filterAny(vis); }
    public void visit (ConditionalOrOp vis) { super.visit(vis); filterAny(vis); }
    public void visit (Constant vis) { super.visit(vis); filterAny(vis); }
    public void visit (DivideOp vis) { super.visit(vis); filterAny(vis); }
    public void visit (EqualsOp vis) { super.visit(vis); filterAny(vis); }
    public void visit (GreaterThanEqualToOp vis) { super.visit(vis); filterAny(vis); }
    public void visit (GreaterThanOp vis) { super.visit(vis); filterAny(vis); }
    public void visit (LeftShiftOp vis) { super.visit(vis); filterAny(vis); }
    public void visit (LessThanEqualToOp vis) { super.visit(vis); filterAny(vis); }
    public void visit (LessThanOp vis) { super.visit(vis); filterAny(vis); }
    public void visit (LocationConstant vis) { super.visit(vis); filterAny(vis); }
    public void visit (MinusOp vis) { super.visit(vis); filterAny(vis); }
    public void visit (ModuloOp vis) { super.visit(vis); filterAny(vis); }
    public void visit (MultiplyOp vis) { super.visit(vis); filterAny(vis); }
    public void visit (NotEqualsOp vis) { super.visit(vis); filterAny(vis); }
    public void visit (NotOp vis) { super.visit(vis); filterAny(vis); }
    public void visit (OrOp vis) { super.visit(vis); filterAny(vis); }
    public void visit (PlusOp vis) { super.visit(vis); filterAny(vis); }
    public void visit (ReductionOrOp vis) { super.visit(vis); filterAny(vis); }
    public void visit (RightShiftOp vis) { super.visit(vis); filterAny(vis); }
    public void visit (RightShiftUnsignedOp vis) { super.visit(vis); filterAny(vis); }
    public void visit (ShortcutIfElseOp vis) { super.visit(vis); filterAny(vis); }
    public void visit (SubtractOp vis) { super.visit(vis); filterAny(vis); }
    public void visit (NumericPromotionOp vis) { super.visit(vis); filterAny(vis); }
    public void visit (XorOp vis) { super.visit(vis); filterAny(vis); }
    public void visit (Branch vis) { super.visit(vis); filterAny(vis); }
    public void visit (Switch vis) { super.visit(vis); filterAny(vis); }
    public void visit (InBuf vis) { super.visit(vis); filterAny(vis); }
    public void visit (OutBuf vis) { super.visit(vis); filterAny(vis); }
    public void visit (Mux vis) { super.visit(vis); filterAny(vis); }
    public void visit (EncodedMux vis) { super.visit(vis); filterAny(vis); }
    public void visit (PriorityMux vis) { super.visit(vis); filterAny(vis); }
    public void visit (And vis) { super.visit(vis); filterAny(vis); }
    public void visit (Not vis) { super.visit(vis); filterAny(vis); }
    public void visit (Or vis) { super.visit(vis); filterAny(vis); }
    public void visit (Scoreboard vis) { super.visit(vis); filterAny(vis); }
    public void visit (Latch vis) { super.visit(vis); filterAny(vis); }
    public void visit (NoOp vis) { super.visit(vis); filterAny(vis); }
    public void visit (RegisterRead vis) { super.visit(vis); filterAny(vis); }
    public void visit (HeapRead vis) { super.visit(vis); filterAny(vis); }
    public void visit (ArrayRead vis) { super.visit(vis); filterAny(vis); }
    public void visit (HeapWrite vis) { super.visit(vis); filterAny(vis); }
    public void visit (ArrayWrite vis) { super.visit(vis); filterAny(vis); }
    public void visit (AbsoluteMemoryRead vis) { super.visit(vis); filterAny(vis); }
    public void visit (AbsoluteMemoryWrite vis) { super.visit(vis); filterAny(vis); }
    public void visit (Kicker vis) { super.visit(vis); filterAny(vis); }

    public void visit (TaskCall vis) { super.visit(vis); filterAny(vis); }
    public void visit (SimplePinAccess vis) { super.visit(vis); filterAny(vis); }
    public void visit (SimplePin vis) { super.visit(vis); filterAny(vis); }
    public void visit (SimplePinRead vis) { super.visit(vis); filterAny(vis); }
    public void visit (SimplePinWrite vis) { super.visit(vis); filterAny(vis); }
    public void visit (FifoAccess vis) { super.visit(vis); filterAny(vis); }
    public void visit (FifoRead vis) { super.visit(vis); filterAny(vis); }
    public void visit (FifoWrite vis) { super.visit(vis); filterAny(vis); }
    //public void visit (WhileBody vis) { super.visit(vis); filterAny(vis); }
    //public void visit (UntilBody vis) { super.visit(vis); filterAny(vis); }
    //public void visit (ForBody vis) { super.visit(vis); filterAny(vis); }
    //public void visit (Decision vis) { super.visit(vis); filterAny(vis); }
    //public void visit (TimingOp vis) { super.visit(vis); filterAny(vis); }
    //public void visit (RegisterGateway vis) { super.visit(vis); filterAny(vis); }
    //public void visit (RegisterReferee vis) { super.visit(vis); filterAny(vis); }
    //public void visit (MemoryReferee vis) { super.visit(vis); filterAny(vis); }
    //public void visit (MemoryGateway vis) { super.visit(vis); filterAny(vis); }
    //public void visit (MemoryBank vis) { super.visit(vis); filterAny(vis); }
    //public void visit (PinRead vis) { super.visit(vis); filterAny(vis); }
    //public void visit (PinWrite vis) { super.visit(vis); filterAny(vis); }
    //public void visit (PinStateChange vis) { super.visit(vis); filterAny(vis); }
    //public void visit (PinReferee vis) { super.visit(vis); filterAny(vis); }
    //public void visit (TriBuf vis) { super.visit(vis); filterAny(vis); }
    //public void visit (EndianSwapper vis) { super.visit(vis); filterAny(vis); }
    
    
    public void filterAny (Component comp)
    {
        if (!this.seqElements.containsKey(comp))
        {
            this.seqElements.put(comp, new GenericStateVarOpHandle(comp, this.nameCache));
        }
    }
    private class GenericStateVarOpHandle extends OpHandle implements StateVar
    {
        private List<Bus> buses;
        private GenericStateVarOpHandle (Component comp, CNameCache cache)
        {
            super(comp, cache);
            buses = new ArrayList(comp.getBuses());
        }
        
        public void declareGlobal (PrintStream ps)
        {
            for (Bus bus : this.buses)
            {
                ps.println(declare(bus));
            }
        }
        
        public void writeTick (PrintStream ps) { ; }
    }
    */
}

