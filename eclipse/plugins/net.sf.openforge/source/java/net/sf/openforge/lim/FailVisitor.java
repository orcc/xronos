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
import net.sf.openforge.app.EngineThread;
import net.sf.openforge.lim.io.*;
import net.sf.openforge.lim.memory.*;
import net.sf.openforge.lim.op.*;


/**
 * FailVisitor implements each of the 'visit' methods defined in the
 * {@link Visitor} interface and throws a fatal exception for each one
 * of them.  Using this class a lightweight Visitor can be easily
 * created in which only the classes of interest must be implemented.
 *
 *
 * <p>Created: Wed Oct 22 16:44:35 2003
 *
 * @author imiller, last modified by $Author: imiller $
 * @version $Id: FailVisitor.java 88 2006-01-11 22:39:52Z imiller $
 */
public class FailVisitor implements Visitor
{
    private static final String _RCS_ = "$Rev: 88 $";

    private String messageLocator;
    
    protected FailVisitor (String messageLocation)
    {
        this.messageLocator = messageLocation;
    }

    protected void fail (Visitable vis)
    {
    	EngineThread.getEngine().fatalError("Internal error at: " + this.messageLocator + ".  Unexpected traversal of " + vis + " encountered");
    }
    
    public void visit (AbsoluteMemoryRead vis){ fail(vis); }
    public void visit (AbsoluteMemoryWrite vis){ fail(vis); }
    public void visit (AddOp vis){ fail(vis); }
    public void visit (And vis){ fail(vis); }
    public void visit (AndOp vis){ fail(vis); }
    public void visit (ArrayRead vis){ fail(vis); }
    public void visit (ArrayWrite vis){ fail(vis); }
    public void visit (Block vis){ fail(vis); }
    public void visit (Branch vis){ fail(vis); }
    public void visit (Call vis){ fail(vis); }
    public void visit (CastOp vis){ fail(vis); }
    public void visit (ComplementOp vis){ fail(vis); }
    public void visit (ConditionalAndOp vis){ fail(vis); }
    public void visit (ConditionalOrOp vis){ fail(vis); }
    public void visit (Constant vis){ fail(vis); }
    public void visit (Decision vis){ fail(vis); }
    public void visit (Design vis){ fail(vis); }
    public void visit (DivideOp vis){ fail(vis); }
    public void visit (EncodedMux vis){ fail(vis); }
    public void visit (EndianSwapper vis){ fail(vis); }
    public void visit (EqualsOp vis){ fail(vis); }
    public void visit (FifoAccess vis) { fail(vis); }
    public void visit (FifoRead vis) { fail(vis); }
    public void visit (FifoWrite vis) { fail(vis); }
    public void visit (ForBody vis){ fail(vis); }
    public void visit (GreaterThanEqualToOp vis){ fail(vis); }
    public void visit (GreaterThanOp vis){ fail(vis); }
    public void visit (HeapWrite vis){ fail(vis); }
    public void visit (HeapRead vis){ fail(vis); }
    public void visit (InBuf vis){ fail(vis); }
    public void visit (IPCoreCall vis){ fail(vis); }
    public void visit (Kicker vis){ fail(vis); }
    public void visit (Latch vis){ fail(vis); }
    public void visit (LeftShiftOp vis){ fail(vis); }
    public void visit (LessThanEqualToOp vis){ fail(vis); }
    public void visit (LessThanOp vis){ fail(vis); }
    public void visit (LocationConstant vis){ fail(vis); }
    public void visit (Loop vis){ fail(vis); }
    public void visit (MemoryBank vis){ fail(vis); }
    public void visit (MemoryGateway vis){ fail(vis); }
    public void visit (MemoryRead vis){ fail(vis); }
    public void visit (MemoryReferee vis){ fail(vis); }
    public void visit (MemoryWrite vis){ fail(vis); }
    public void visit (MinusOp vis){ fail(vis); }
    public void visit (ModuloOp vis){ fail(vis); }
    public void visit (MultiplyOp vis){ fail(vis); }
    public void visit (Mux vis){ fail(vis); }
    public void visit (NoOp vis){ fail(vis); }
    public void visit (Not vis){ fail(vis); }
    public void visit (NotEqualsOp vis){ fail(vis); }
    public void visit (NotOp vis){ fail(vis); }
    public void visit (NumericPromotionOp vis){ fail(vis); }
    public void visit (Or vis){ fail(vis); }
    public void visit (OrOp vis){ fail(vis); }
    public void visit (OutBuf vis){ fail(vis); }
    public void visit (PinRead vis){ fail(vis); }
    public void visit (PinReferee vis){ fail(vis); }
    public void visit (PinStateChange vis){ fail(vis); }
    public void visit (PinWrite vis){ fail(vis); }
    public void visit (PlusOp vis){ fail(vis); }
    public void visit (PriorityMux vis){ fail(vis); }
    public void visit (Procedure vis){ fail(vis); }
    public void visit (ReductionOrOp vis){ fail(vis); }
    public void visit (Reg vis){ fail(vis); }
    public void visit (RegisterGateway vis){ fail(vis); }
    public void visit (RegisterRead vis){ fail(vis); }
    public void visit (RegisterReferee vis){ fail(vis); }
    public void visit (RegisterWrite vis){ fail(vis); }
    public void visit (RightShiftOp vis){ fail(vis); }
    public void visit (RightShiftUnsignedOp vis){ fail(vis); }
    public void visit (Scoreboard vis){ fail(vis); }
    public void visit (ShortcutIfElseOp vis){ fail(vis); }
    public void visit (SimplePin vis) { fail(vis); }
    public void visit (SimplePinAccess vis) { fail(vis); }
    public void visit (SimplePinRead vis) { fail(vis); }
    public void visit (SimplePinWrite vis) { fail(vis); }
    public void visit (SRL16 vis){ fail(vis); }
    public void visit (SubtractOp vis){ fail(vis); }
    public void visit (Switch vis){ fail(vis); }
    public void visit (Task vis){ fail(vis); }
    public void visit (TaskCall vis) { fail(vis); }
    public void visit (TimingOp vis){ fail(vis); }
    public void visit (TriBuf vis){ fail(vis); }
    public void visit (UntilBody vis){ fail(vis); }
    public void visit (WhileBody vis){ fail(vis); }
    public void visit (XorOp vis){ fail(vis); }

    
}// FailVisitor
