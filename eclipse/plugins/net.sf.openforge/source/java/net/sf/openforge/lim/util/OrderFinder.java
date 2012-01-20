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

package net.sf.openforge.lim.util;

import java.util.LinkedList;
import java.util.List;

import net.sf.openforge.lim.*;
import net.sf.openforge.lim.io.*;
import net.sf.openforge.lim.memory.*;
import net.sf.openforge.lim.op.*;



/**
 *
 *
 * Created: Tue Jul 13 2004
 *
 * @author madhupc
 * @version $Id: OrderFinder.java 88 2006-01-11 22:39:52Z imiller $
 */
public class OrderFinder extends DataFlowVisitor
{
    private static final String _RCS_ = "$Rev: 88 $";

   private List order = new LinkedList();

   public OrderFinder ()
    {
        // Call the constructor of the super class
        super();
        this.setRunForward(true);
    }

    public static List getOrder (Module module)
    {
         OrderFinder finder = new OrderFinder();
         finder.traverse(module);
         return finder.getOrder();
    }

    private List getOrder()
    {
        return this.order;
    }

    
    public void visit(Design param1) { this.order.add(param1); }
    public void visit (TaskCall param1) { this.order.add(param1); }
    public void visit(Task param1) { this.order.add(param1); }
    public void visit(Call param1) { this.order.add(param1); }
    public void visit(IPCoreCall param1) {}
    public void visit(Procedure param1) { this.order.add(param1); }
    public void visit(Block param1) { this.order.add(param1); }
    public void visit(Loop param1) { this.order.add(param1); }
    public void visit(WhileBody param1) { this.order.add(param1); }
    public void visit(UntilBody param1) { this.order.add(param1); }
    public void visit(ForBody param1) { this.order.add(param1); }
    public void visit(AddOp param1) { this.order.add(param1); }
    public void visit(AndOp param1) { this.order.add(param1); }
    public void visit(CastOp param1) { this.order.add(param1); }
    public void visit(ComplementOp param1) { this.order.add(param1); }
    public void visit(ConditionalAndOp param1) { this.order.add(param1); }
    public void visit(ConditionalOrOp param1) { this.order.add(param1); }
    public void visit(Constant param1) { this.order.add(param1); }
    public void visit (LocationConstant loc) { this.order.add(loc); }
    public void visit(DivideOp param1) { this.order.add(param1); }
    public void visit(EqualsOp param1) { this.order.add(param1); }
    public void visit(GreaterThanEqualToOp param1) { this.order.add(param1); }
    public void visit(GreaterThanOp param1) { this.order.add(param1); }
    public void visit(LeftShiftOp param1) { this.order.add(param1); }
    public void visit(LessThanEqualToOp param1) { this.order.add(param1); }
    public void visit(LessThanOp param1) { this.order.add(param1); }
    public void visit(MinusOp param1) { this.order.add(param1); }
    public void visit(ModuloOp param1) { this.order.add(param1); }
    public void visit(MultiplyOp param1) { this.order.add(param1); }
    public void visit(NotEqualsOp param1) { this.order.add(param1); }
    public void visit(NotOp param1) { this.order.add(param1); }
    public void visit(OrOp param1) { this.order.add(param1); }
    public void visit(PlusOp param1) { this.order.add(param1); }
    public void visit(ReductionOrOp param1) { this.order.add(param1); }
    public void visit(RightShiftOp param1) { this.order.add(param1); }
    public void visit(RightShiftUnsignedOp param1) { this.order.add(param1); }
    public void visit(ShortcutIfElseOp param1) { this.order.add(param1); }
    public void visit(SubtractOp param1) { this.order.add(param1); }
    public void visit(NumericPromotionOp param1) { this.order.add(param1); }
    public void visit(XorOp param1) { this.order.add(param1); }
    public void visit(Branch param1) { this.order.add(param1); }
    public void visit(Decision param1) { this.order.add(param1); }
    public void visit(Switch param1) { this.order.add(param1); }
    public void visit(InBuf param1) { this.order.add(param1); }
    public void visit(OutBuf param1) { this.order.add(param1); }
    public void visit(Reg param1) { this.order.add(param1); }
    public void visit(Mux param1) { this.order.add(param1); }
    public void visit(EncodedMux param1) { this.order.add(param1); }
    public void visit(PriorityMux param1) { this.order.add(param1); }
    public void visit(And param1) { this.order.add(param1); }
    public void visit(Not param1) { this.order.add(param1); }
    public void visit(Or or) { this.order.add(or); }
    public void visit(Scoreboard param1) { this.order.add(param1); }
    public void visit(Latch param1) { this.order.add(param1); }
    public void visit(NoOp param1) { this.order.add(param1); }
    public void visit(TimingOp param1) { this.order.add(param1); }
    public void visit(RegisterRead param1) { this.order.add(param1); }
    public void visit(RegisterWrite param1) { this.order.add(param1); }
    public void visit(RegisterGateway param1) { this.order.add(param1); }
    public void visit(RegisterReferee param1) { this.order.add(param1); }
    public void visit(MemoryBank param1) { this.order.add(param1); }
    public void visit(MemoryRead memoryRead) { this.order.add(memoryRead); }
    public void visit(MemoryWrite memoryWrite) { this.order.add(memoryWrite); }
    public void visit(MemoryReferee param1) { this.order.add(param1); }
    public void visit(MemoryGateway param1) { this.order.add(param1); }
    public void visit(HeapRead param1) { this.order.add(param1); }
    public void visit(ArrayRead param1) { this.order.add(param1); }
    public void visit(HeapWrite param1) { this.order.add(param1); }
    public void visit(ArrayWrite param1) { this.order.add(param1); }
    public void visit (AbsoluteMemoryRead param1) { this.order.add(param1); }
    public void visit (AbsoluteMemoryWrite param1) { this.order.add(param1); }
    public void visit(Kicker param1) { this.order.add(param1); }
    public void visit(PinRead param1) { this.order.add(param1); }
    public void visit(PinWrite param1) { this.order.add(param1); }
    public void visit(PinStateChange param1) { this.order.add(param1); }
    public void visit(SRL16 param1) { this.order.add(param1); }
    public void visit(PinReferee param1) { this.order.add(param1); }
    public void visit(TriBuf param1) { this.order.add(param1); }
    public void visit (SimplePinAccess param1) { this.order.add(param1); }
    public void visit (SimplePinRead param1) { this.order.add(param1); }
    public void visit (SimplePinWrite param1) { this.order.add(param1); }
    public void visit (FifoAccess param1) { this.order.add(param1); }
    public void visit (FifoRead param1) { this.order.add(param1); }
    public void visit (FifoWrite param1) { this.order.add(param1); }
    public void visit (EndianSwapper param1) { this.order.add(param1); }
}
