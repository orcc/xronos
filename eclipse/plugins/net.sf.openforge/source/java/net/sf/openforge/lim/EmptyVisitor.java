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

import net.sf.openforge.lim.io.FifoAccess;
import net.sf.openforge.lim.io.FifoRead;
import net.sf.openforge.lim.io.FifoWrite;
import net.sf.openforge.lim.io.SimplePin;
import net.sf.openforge.lim.io.SimplePinAccess;
import net.sf.openforge.lim.io.SimplePinRead;
import net.sf.openforge.lim.io.SimplePinWrite;
import net.sf.openforge.lim.memory.AbsoluteMemoryRead;
import net.sf.openforge.lim.memory.AbsoluteMemoryWrite;
import net.sf.openforge.lim.memory.EndianSwapper;
import net.sf.openforge.lim.memory.LocationConstant;
import net.sf.openforge.lim.memory.MemoryBank;
import net.sf.openforge.lim.memory.MemoryGateway;
import net.sf.openforge.lim.memory.MemoryRead;
import net.sf.openforge.lim.memory.MemoryReferee;
import net.sf.openforge.lim.memory.MemoryWrite;
import net.sf.openforge.lim.op.AddOp;
import net.sf.openforge.lim.op.AndOp;
import net.sf.openforge.lim.op.CastOp;
import net.sf.openforge.lim.op.ComplementOp;
import net.sf.openforge.lim.op.ConditionalAndOp;
import net.sf.openforge.lim.op.ConditionalOrOp;
import net.sf.openforge.lim.op.Constant;
import net.sf.openforge.lim.op.DivideOp;
import net.sf.openforge.lim.op.EqualsOp;
import net.sf.openforge.lim.op.GreaterThanEqualToOp;
import net.sf.openforge.lim.op.GreaterThanOp;
import net.sf.openforge.lim.op.LeftShiftOp;
import net.sf.openforge.lim.op.LessThanEqualToOp;
import net.sf.openforge.lim.op.LessThanOp;
import net.sf.openforge.lim.op.MinusOp;
import net.sf.openforge.lim.op.ModuloOp;
import net.sf.openforge.lim.op.MultiplyOp;
import net.sf.openforge.lim.op.NoOp;
import net.sf.openforge.lim.op.NotEqualsOp;
import net.sf.openforge.lim.op.NotOp;
import net.sf.openforge.lim.op.NumericPromotionOp;
import net.sf.openforge.lim.op.OrOp;
import net.sf.openforge.lim.op.PlusOp;
import net.sf.openforge.lim.op.ReductionOrOp;
import net.sf.openforge.lim.op.RightShiftOp;
import net.sf.openforge.lim.op.RightShiftUnsignedOp;
import net.sf.openforge.lim.op.ShortcutIfElseOp;
import net.sf.openforge.lim.op.SubtractOp;
import net.sf.openforge.lim.op.TimingOp;
import net.sf.openforge.lim.op.XorOp;

/**
 * This class is a visitor class which does nothing for every vistable method.
 * It is useful for everriding certain elements for double-dispatch analysis.
 * {@link Visitable Visitable}
 * 
 * 
 * 
 * @author cschanck
 * @version $Id: EmptyVisitor.java 88 2006-01-11 22:39:52Z imiller $
 * @see Visitable
 */
public class EmptyVisitor implements Visitor {
	static final String _RCS_ = "$Rev: 88 $";

	public void visit(Design design) {
	}

	public void visit(Task task) {
	}

	public void visit(Call call) {
	}

	public void visit(IPCoreCall call) {
	}

	public void visit(Procedure procedure) {
	}

	public void visit(Block block) {
	}

	public void visit(Loop loop) {
	}

	public void visit(WhileBody whileBody) {
	}

	public void visit(UntilBody untilBody) {
	}

	public void visit(ForBody forBody) {
	}

	public void visit(AddOp add) {
	}

	public void visit(AndOp andOp) {
	}

	public void visit(CastOp cast) {
	}

	public void visit(ComplementOp complement) {
	}

	public void visit(ConditionalAndOp conditionalAnd) {
	}

	public void visit(ConditionalOrOp conditionalOr) {
	}

	public void visit(Constant constant) {
	}

	public void visit(DivideOp divide) {
	}

	public void visit(EqualsOp equals) {
	}

	public void visit(GreaterThanEqualToOp greaterThanEqualTo) {
	}

	public void visit(GreaterThanOp greaterThan) {
	}

	public void visit(LeftShiftOp leftShift) {
	}

	public void visit(LessThanEqualToOp lessThanEqualTo) {
	}

	public void visit(LessThanOp lessThan) {
	}

	public void visit(LocationConstant loc) {
	}

	public void visit(MinusOp minus) {
	}

	public void visit(ModuloOp modulo) {
	}

	public void visit(MultiplyOp multiply) {
	}

	public void visit(NotEqualsOp notEquals) {
	}

	public void visit(NotOp not) {
	}

	public void visit(OrOp or) {
	}

	public void visit(PlusOp plus) {
	}

	public void visit(ReductionOrOp reducedOr) {
	}

	public void visit(RightShiftOp rightShift) {
	}

	public void visit(RightShiftUnsignedOp rightShiftUnsigned) {
	}

	public void visit(ShortcutIfElseOp shortcutIfElse) {
	}

	public void visit(SubtractOp subtract) {
	}

	public void visit(NumericPromotionOp numericPromotion) {
	}

	public void visit(XorOp xor) {
	}

	public void visit(Branch branch) {
	}

	public void visit(Decision decision) {
	}

	public void visit(Switch sw) {
	}

	public void visit(InBuf ib) {
	}

	public void visit(OutBuf ob) {
	}

	public void visit(Reg reg) {
	}

	public void visit(Mux m) {
	}

	public void visit(EncodedMux m) {
	}

	public void visit(PriorityMux pmux) {
	}

	public void visit(And a) {
	}

	public void visit(Not n) {
	}

	public void visit(Or o) {
	}

	public void visit(Scoreboard scoreboard) {
	}

	public void visit(Latch latch) {
	}

	public void visit(NoOp nop) {
	}

	public void visit(TimingOp nop) {
	}

	public void visit(RegisterRead regRead) {
	}

	public void visit(RegisterWrite regWrite) {
	}

	public void visit(RegisterGateway regGateway) {
	}

	public void visit(RegisterReferee regReferee) {
	}

	public void visit(MemoryRead memRead) {
	}

	public void visit(MemoryWrite memWrite) {
	}

	public void visit(MemoryReferee memReferee) {
	}

	public void visit(MemoryGateway memGateway) {
	}

	public void visit(MemoryBank memBank) {
	}

	public void visit(HeapRead heapRead) {
	}

	public void visit(ArrayRead arrayRead) {
	}

	public void visit(HeapWrite heapWrite) {
	}

	public void visit(ArrayWrite arrayWrite) {
	}

	public void visit(AbsoluteMemoryRead absRead) {
	}

	public void visit(AbsoluteMemoryWrite absWrite) {
	}

	public void visit(Kicker kicker) {
	}

	public void visit(PinRead pinRead) {
	}

	public void visit(PinWrite pinWrite) {
	}

	public void visit(PinStateChange pinChange) {
	}

	public void visit(SRL16 srl16) {
	}

	public void visit(PinReferee pinReferee) {
	}

	public void visit(TriBuf tbuf) {
	}

	public void visit(SimplePin comp) {
	}

	public void visit(TaskCall mod) {
	}

	public void visit(SimplePinAccess mod) {
	}

	public void visit(SimplePinRead comp) {
	}

	public void visit(SimplePinWrite comp) {
	}

	public void visit(FifoAccess mod) {
	}

	public void visit(FifoRead mod) {
	}

	public void visit(FifoWrite mod) {
	}

	public void visit(EndianSwapper mod) {
	}
}
