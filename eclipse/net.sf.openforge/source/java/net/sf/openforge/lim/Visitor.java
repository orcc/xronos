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

package net.sf.openforge.lim;

import net.sf.openforge.lim.io.*;
import net.sf.openforge.lim.memory.*;
import net.sf.openforge.lim.op.*;

/**
 * Visitor this interface is used by any engine wishing to traverse
 * the LIM.  Any LIM node which has a specific behavior to an engine
 * traversing the LIM must have a corresponding visit method in this
 * class.  This allows the implementors of this class to provide code
 * to process each possible structure.  This class works closely with
 * {@link Visitable Visitable}
 * 
 * @see Visitable
 */
public interface Visitor
{
    static final String _RCS_ = "$Rev: 98 $";

    void visit (Design vis);
    void visit (Task vis);
    void visit (Call vis);
    void visit (IPCoreCall vis);
    void visit (Procedure vis);
    void visit (Block vis);
    void visit (Loop vis);
    void visit (WhileBody vis);
    void visit (UntilBody vis);
    void visit (ForBody vis);
    void visit (AddOp vis);
    void visit (AndOp vis);
    void visit (CastOp vis);
    void visit (ComplementOp vis);
    void visit (ConditionalAndOp vis);
    void visit (ConditionalOrOp vis);
    void visit (Constant vis);
    void visit (DivideOp vis);
    void visit (EqualsOp vis);
    void visit (GreaterThanEqualToOp vis);
    void visit (GreaterThanOp vis);
    void visit (LeftShiftOp vis);
    void visit (LessThanEqualToOp vis);
    void visit (LessThanOp vis);
    void visit (LocationConstant vis);
    void visit (MinusOp vis);
    void visit (ModuloOp vis);
    void visit (MultiplyOp vis);
    void visit (NotEqualsOp vis);
    void visit (NotOp vis);
    void visit (OrOp vis);
    void visit (PlusOp vis);
    void visit (ReductionOrOp vis);
    void visit (RightShiftOp vis);
    void visit (RightShiftUnsignedOp vis);
    void visit (ShortcutIfElseOp vis);
    void visit (SubtractOp vis);
    void visit (NumericPromotionOp vis);
    void visit (XorOp vis);
    void visit (Branch vis);
    void visit (Decision vis);
    void visit (Switch vis);
    void visit (InBuf vis);
    void visit (OutBuf vis);
    void visit (Reg vis);
    void visit (Mux vis);
    void visit (EncodedMux vis);
    void visit (PriorityMux vis);
    void visit (And vis);
    void visit (Not vis);
    void visit (Or vis);
    void visit (Scoreboard vis);
    void visit (Latch vis);
    void visit (NoOp vis);
    void visit (TimingOp vis);
    void visit (RegisterRead vis);
    void visit (RegisterWrite vis);
    void visit (RegisterGateway vis);
    void visit (RegisterReferee vis);
    void visit (MemoryRead vis);
    void visit (MemoryWrite vis);
    void visit (MemoryReferee vis);
    void visit (MemoryGateway vis);
    void visit (MemoryBank vis);
    void visit (HeapRead vis);
    void visit (ArrayRead vis);
    void visit (HeapWrite vis);
    void visit (ArrayWrite vis);
    void visit (AbsoluteMemoryRead vis);
    void visit (AbsoluteMemoryWrite vis);
    void visit (Kicker vis);
    void visit (PinRead vis);
    void visit (PinWrite vis);
    void visit (PinStateChange vis);
    void visit (SRL16 vis);
    void visit (PinReferee vis);
    void visit (TriBuf vis);

    void visit (TaskCall vis);
    void visit (SimplePinAccess vis);
    void visit (SimplePin vis);
    void visit (SimplePinRead vis);
    void visit (SimplePinWrite vis);
    void visit (FifoAccess vis);
    void visit (FifoRead vis);
    void visit (FifoWrite vis);
    void visit (EndianSwapper vis);
    
} // Visitor
