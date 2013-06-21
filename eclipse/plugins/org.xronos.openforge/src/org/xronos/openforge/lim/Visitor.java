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

package org.xronos.openforge.lim;

import org.xronos.openforge.lim.io.FifoAccess;
import org.xronos.openforge.lim.io.FifoRead;
import org.xronos.openforge.lim.io.FifoWrite;
import org.xronos.openforge.lim.io.SimplePin;
import org.xronos.openforge.lim.io.SimplePinAccess;
import org.xronos.openforge.lim.io.SimplePinRead;
import org.xronos.openforge.lim.io.SimplePinWrite;
import org.xronos.openforge.lim.memory.AbsoluteMemoryRead;
import org.xronos.openforge.lim.memory.AbsoluteMemoryWrite;
import org.xronos.openforge.lim.memory.EndianSwapper;
import org.xronos.openforge.lim.memory.LocationConstant;
import org.xronos.openforge.lim.memory.MemoryBank;
import org.xronos.openforge.lim.memory.MemoryGateway;
import org.xronos.openforge.lim.memory.MemoryRead;
import org.xronos.openforge.lim.memory.MemoryReferee;
import org.xronos.openforge.lim.memory.MemoryWrite;
import org.xronos.openforge.lim.op.AddOp;
import org.xronos.openforge.lim.op.AndOp;
import org.xronos.openforge.lim.op.CastOp;
import org.xronos.openforge.lim.op.ComplementOp;
import org.xronos.openforge.lim.op.ConditionalAndOp;
import org.xronos.openforge.lim.op.ConditionalOrOp;
import org.xronos.openforge.lim.op.Constant;
import org.xronos.openforge.lim.op.DivideOp;
import org.xronos.openforge.lim.op.EqualsOp;
import org.xronos.openforge.lim.op.GreaterThanEqualToOp;
import org.xronos.openforge.lim.op.GreaterThanOp;
import org.xronos.openforge.lim.op.LeftShiftOp;
import org.xronos.openforge.lim.op.LessThanEqualToOp;
import org.xronos.openforge.lim.op.LessThanOp;
import org.xronos.openforge.lim.op.MinusOp;
import org.xronos.openforge.lim.op.ModuloOp;
import org.xronos.openforge.lim.op.MultiplyOp;
import org.xronos.openforge.lim.op.NoOp;
import org.xronos.openforge.lim.op.NotEqualsOp;
import org.xronos.openforge.lim.op.NotOp;
import org.xronos.openforge.lim.op.NumericPromotionOp;
import org.xronos.openforge.lim.op.OrOp;
import org.xronos.openforge.lim.op.PlusOp;
import org.xronos.openforge.lim.op.ReductionOrOp;
import org.xronos.openforge.lim.op.RightShiftOp;
import org.xronos.openforge.lim.op.RightShiftUnsignedOp;
import org.xronos.openforge.lim.op.ShortcutIfElseOp;
import org.xronos.openforge.lim.op.SubtractOp;
import org.xronos.openforge.lim.op.TimingOp;
import org.xronos.openforge.lim.op.XorOp;
import org.xronos.openforge.lim.primitive.And;
import org.xronos.openforge.lim.primitive.EncodedMux;
import org.xronos.openforge.lim.primitive.Mux;
import org.xronos.openforge.lim.primitive.Not;
import org.xronos.openforge.lim.primitive.Or;
import org.xronos.openforge.lim.primitive.Reg;
import org.xronos.openforge.lim.primitive.SRL16;

/**
 * Visitor this interface is used by any engine wishing to traverse the LIM. Any
 * LIM node which has a specific behavior to an engine traversing the LIM must
 * have a corresponding visit method in this class. This allows the implementors
 * of this class to provide code to process each possible structure. This class
 * works closely with {@link Visitable Visitable}
 * 
 * @see Visitable
 */
public interface Visitor {

	void visit(AbsoluteMemoryRead vis);

	void visit(AbsoluteMemoryWrite vis);

	void visit(AddOp vis);

	void visit(And vis);

	void visit(AndOp vis);

	void visit(ArrayRead vis);

	void visit(ArrayWrite vis);

	void visit(Block vis);

	void visit(Branch vis);

	void visit(Call vis);

	void visit(CastOp vis);

	void visit(ComplementOp vis);

	void visit(ConditionalAndOp vis);

	void visit(ConditionalOrOp vis);

	void visit(Constant vis);

	void visit(Decision vis);

	void visit(Design vis);

	void visit(DivideOp vis);

	void visit(EncodedMux vis);

	void visit(EndianSwapper vis);

	void visit(EqualsOp vis);

	void visit(FifoAccess vis);

	void visit(FifoRead vis);

	void visit(FifoWrite vis);

	void visit(ForBody vis);

	void visit(GreaterThanEqualToOp vis);

	void visit(GreaterThanOp vis);

	void visit(HeapRead vis);

	void visit(HeapWrite vis);

	void visit(InBuf vis);

	void visit(IPCoreCall vis);

	void visit(Kicker vis);

	void visit(Latch vis);

	void visit(LeftShiftOp vis);

	void visit(LessThanEqualToOp vis);

	void visit(LessThanOp vis);

	void visit(LocationConstant vis);

	void visit(Loop vis);

	void visit(MemoryBank vis);

	void visit(MemoryGateway vis);

	void visit(MemoryRead vis);

	void visit(MemoryReferee vis);

	void visit(MemoryWrite vis);

	void visit(MinusOp vis);

	void visit(ModuloOp vis);

	void visit(MultiplyOp vis);

	void visit(Mux vis);

	void visit(NoOp vis);

	void visit(Not vis);

	void visit(NotEqualsOp vis);

	void visit(NotOp vis);

	void visit(NumericPromotionOp vis);

	void visit(Or vis);

	void visit(OrOp vis);

	void visit(OutBuf vis);

	void visit(PinRead vis);

	void visit(PinReferee vis);

	void visit(PinStateChange vis);

	void visit(PinWrite vis);

	void visit(PlusOp vis);

	void visit(PriorityMux vis);

	void visit(Procedure vis);

	void visit(ReductionOrOp vis);

	void visit(Reg vis);

	void visit(RegisterGateway vis);

	void visit(RegisterRead vis);

	void visit(RegisterReferee vis);

	void visit(RegisterWrite vis);

	void visit(RightShiftOp vis);

	void visit(RightShiftUnsignedOp vis);

	void visit(Scoreboard vis);

	void visit(ShortcutIfElseOp vis);

	void visit(SimplePin vis);

	void visit(SimplePinAccess vis);

	void visit(SimplePinRead vis);

	void visit(SimplePinWrite vis);

	void visit(SRL16 vis);

	void visit(SubtractOp vis);

	void visit(Switch vis);

	void visit(Task vis);

	void visit(TaskCall vis);

	void visit(TimingOp vis);

	void visit(TriBuf vis);

	void visit(UntilBody vis);

	void visit(WhileBody vis);

	void visit(XorOp vis);

} // Visitor
