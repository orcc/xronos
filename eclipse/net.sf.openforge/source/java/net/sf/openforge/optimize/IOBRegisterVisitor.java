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

import net.sf.openforge.lim.*;
import net.sf.openforge.lim.memory.*;

/**
 * IOBRegisterVisitor visits a design by finding all Reg and SRL16's
 * that are connected directly to a pin (via any number of module
 * boundries).  When a Reg is found it is annotated (via
 * Component.setAttribute()) with an {@link Attribute} to force it to
 * be pushed into the IOBs.  When an SRL16 is found, the first (for
 * input pin connection) or last (for output pin connection) register
 * is split out from the SRL16 (reducing its stages by 1) in order to
 * annotate just the regiter.
 *
 * <p>Created: Tue Jan 28 12:25:46 2003
 *
 * @author imiller, last modified by $Author: imiller $
 * @version $Id: IOBRegisterVisitor.java 14 2005-08-11 19:49:42Z imiller $
 */
public class IOBRegisterVisitor extends DefaultVisitor
{
    private static final String _RCS_ = "$Rev: 14 $";

    private Set iobPortBuses = new HashSet();
    
    public IOBRegisterVisitor ()
    {}

    /**
     * Just for testing please...
     */
    IOBRegisterVisitor (Set iobPortBus)
    {
        this();
        this.iobPortBuses.addAll(iobPortBus);
    }
    
    
    public void visit (Design design)
    {
        for (Iterator iter = design.getInputPins().iterator(); iter.hasNext();)
        {
            iobPortBuses.add(((InputPin)iter.next()).getBus());
        }
        for (Iterator iter = design.getOutputPins().iterator(); iter.hasNext();)
        {
            iobPortBuses.add(((OutputPin)iter.next()).getPort());
        }
        for (Iterator iter = design.getBidirectionalPins().iterator(); iter.hasNext();)
        {
            BidirectionalPin bp = (BidirectionalPin)iter.next();
            iobPortBuses.add(bp.getPort());
            iobPortBuses.add(bp.getBus());
        }
        
        super.visit(design);
    }
    
    //public void visit (Task task){}
    
    public void visit (Call call)
    {
        Procedure proc = call.getProcedure();
        for (Iterator portIter = call.getPorts().iterator(); portIter.hasNext();)
        {
            Port port = (Port)portIter.next();
            Bus bus = port.getBus();
            if (bus != null && iobPortBuses.contains(bus))
            {
                Port procPort = call.getProcedurePort(port);
                iobPortBuses.add(procPort.getPeer());
            }
        }

        for (Iterator busIter = call.getBuses().iterator(); busIter.hasNext();)
        {
            Bus bus = (Bus)busIter.next();
            boolean allPorts = bus.getPorts().size() > 0;
            for (Iterator portIter = bus.getPorts().iterator(); portIter.hasNext();)
            {
                allPorts &= iobPortBuses.contains((Port)portIter.next());
            }
            if (allPorts)
            {
                Bus procBus = call.getProcedureBus(bus);
                iobPortBuses.add(procBus.getPeer());
            }
        }
        super.visit(call);        
    }
    
    //public void visit (Procedure procedure){}
    
    public void visit (Block mod)
    {
        processModule(mod);
        super.visit(mod);
    }
    public void visit (Loop mod)
    {
        processModule(mod);
        super.visit(mod);
    }
    public void visit (WhileBody mod)
    {
        processModule(mod);
        super.visit(mod);
    }
    public void visit (UntilBody mod)
    {
        processModule(mod);
        super.visit(mod);
    }
    public void visit (ForBody mod)
    {
        processModule(mod);
        super.visit(mod);
    }
    public void visit (Branch mod)
    {
        processModule(mod);
        super.visit(mod);
    }
    public void visit (Decision mod)
    {
        processModule(mod);
        super.visit(mod);
    }
    public void visit (Switch mod)
    {
        processModule(mod);
        super.visit(mod);
    }
    public void visit (AbsoluteMemoryRead mod)
    {
        processModule(mod);
        super.visit(mod);
    }
    public void visit (AbsoluteMemoryWrite mod)
    {
        processModule(mod);
        super.visit(mod);
    }
    
    
    public void visit (Reg reg)
    {
        Bus driver = reg.getDataPort().getBus();
        if (driver != null && iobPortBuses.contains(driver))
        {
            reg.addAttribute(new Attribute.IOB_True());
        }
        
        if (isIOBOutput(reg))
        {
            reg.addAttribute(new Attribute.IOB_True());
        }
    }

    public void visit (SRL16 srl16)
    {
        Bus driver = srl16.getInDataPort().getBus();
        if (driver != null && iobPortBuses.contains(driver))
        {
            Reg first = (Reg)srl16.getCompactedRegs().remove(0);
            first.getDataPort().setBus(driver);
            first.getDataPort().setUsed(true);
            first.getClockPort().setBus(srl16.getClockPort().getBus());
            if (srl16.getEnablePort().isUsed() && first.getEnablePort().isUsed())
            {
                first.getEnablePort().setBus(srl16.getEnablePort().getBus());
            }
            srl16.getInDataPort().setBus(first.getResultBus());
            srl16.getOwner().addComponent(first);
            first.addAttribute(new Attribute.IOB_True());
        }

        if (isIOBOutput(srl16))
        {
            Reg last = (Reg)srl16.getCompactedRegs().remove(srl16.getCompactedRegs().size()-1);
            //last.getDataPort().setBus(srl16.getResultBus());
            last.getDataPort().setUsed(true);
            last.getClockPort().setBus(srl16.getClockPort().getBus());
            if (srl16.getEnablePort().isUsed() && last.getEnablePort().isUsed())
            {
                last.getEnablePort().setBus(srl16.getEnablePort().getBus());
            }
            for (Iterator iter = srl16.getResultBus().getPorts().iterator(); iter.hasNext();)
            {
                Port port = (Port)iter.next();
                port.setBus(last.getResultBus());
            }
            last.getDataPort().setBus(srl16.getResultBus());
            srl16.getOwner().addComponent(last);
            last.addAttribute(new Attribute.IOB_True());
        }

        if (srl16.getStages() == 0)
        {
            // This is the degenerate case where the input feeds the
            // output directly with a 2 stage delay.
            Set ports = new HashSet(srl16.getResultBus().getPorts());
            for (Iterator iter = ports.iterator(); iter.hasNext();)
            {
                Port port = (Port)iter.next();
                port.setBus(srl16.getInDataPort().getBus());
            }
            srl16.getClockPort().setBus(null);
            srl16.getEnablePort().setBus(null);            
            srl16.getOwner().removeComponent(srl16);
        }
        
    }

    private boolean isIOBOutput (Primitive prim)
    {
        boolean allPorts = prim.getResultBus().getPorts().size() > 0;
        for (Iterator iter = prim.getResultBus().getPorts().iterator(); iter.hasNext();)
        {
            Port port = (Port)iter.next();
            allPorts &= iobPortBuses.contains(port);
        }
        return allPorts;
    }
    
    
    // These are modules, but should be left atomic...    
//     public void visit (PriorityMux pmux){}
//     public void visit (Scoreboard scoreboard){}
//     public void visit (Latch latch){}
    
//     public void visit (AddOp add){}
//     public void visit (AndOp andOp){}
//     public void visit (CastOp cast){}
//     public void visit (ComplementOp complement){}
//     public void visit (ConditionalAndOp conditionalAnd){}
//     public void visit (ConditionalOrOp conditionalOr){}
//     public void visit (Constant constant){}
//     public void visit (DivideOp divide){}
//     public void visit (EqualsOp equals){}
//     public void visit (GreaterThanEqualToOp greaterThanEqualTo){}
//     public void visit (GreaterThanOp greaterThan){}
//     public void visit (LeftShiftOp leftShift){}
//     public void visit (LessThanEqualToOp lessThanEqualTo){}
//     public void visit (LessThanOp lessThan){}
//     public void visit (MinusOp minus){}
//     public void visit (ModuloOp modulo){}
//     public void visit (MultiplyOp multiply){}
//     public void visit (NotEqualsOp notEquals){}
//     public void visit (NotOp not){}
//     public void visit (OrOp or){}
//     public void visit (PlusOp plus){}
//     public void visit (RightShiftOp rightShift){}
//     public void visit (RightShiftUnsignedOp rightShiftUnsigned){}
//     public void visit (ShortcutIfElseOp shortcutIfElse){}
//     public void visit (SubtractOp subtract){}
//     public void visit (NumericPromotionOp numericPromotion){}
//     public void visit (XorOp xor){}
//     public void visit (InBuf ib){}
//     public void visit (OutBuf ob){}
//     public void visit (Mux m){}
//     public void visit (EncodedMux m){}
//     public void visit (And a){}
//     public void visit (Not n){}
//     public void visit (Or o){}
//     public void visit (NoOp nop){}
//     public void visit (TimingOp nop){}
//     public void visit (RegisterRead regRead){}
//     public void visit (RegisterWrite regWrite){}
//     public void visit (RegisterGateway regGateway){}
//     public void visit (RegisterReferee regReferee){}
//     public void visit (MemoryRead memRead){}
//     public void visit (MemoryWrite memWrite){}
//     public void visit (MemoryReferee memReferee){}
//     public void visit (MemoryGateway memGateway){}
//     public void visit (HeapRead heapRead){}
//     public void visit (ArrayRead arrayRead){}
//     public void visit (HeapWrite heapWrite){}
//     public void visit (ArrayWrite arrayWrite){}
//     public void visit (Kicker kicker){}
//     public void visit (PinRead pinRead){}
//     public void visit (PinWrite pinWrite){}
//     public void visit (PinStateChange pinChange){}
//     public void visit (PinReferee pinReferee){}
//     public void visit (TriBuf tbuf){}

    /**
     * Translate IOB buses across module boundries.
     */
    private void processModule (Module module)
    {
        for (Iterator portIter = module.getPorts().iterator(); portIter.hasNext();)
        {
            Port port = (Port)portIter.next();
            Bus bus = port.getBus();
            if (bus == null)
            {
                continue;
            }
            if (iobPortBuses.contains(bus))
            {
                iobPortBuses.add(port.getPeer());
            }
        }

        for (Iterator busIter = module.getBuses().iterator(); busIter.hasNext();)
        {
            Bus bus = (Bus)busIter.next();
            boolean allPorts = bus.getPorts().size() > 0;
            for (Iterator portIter = bus.getPorts().iterator(); portIter.hasNext();)
            {
                Port port = (Port)portIter.next();
                allPorts &= iobPortBuses.contains(port);
            }
            if (allPorts)
            {
                iobPortBuses.add(bus.getPeer());
            }
        }
    }
    
}// IOBRegisterVisitor
