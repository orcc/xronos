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

import org.xronos.openforge.app.EngineThread;
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
 * FailVisitor implements each of the 'visit' methods defined in the
 * {@link Visitor} interface and throws a fatal exception for each one of them.
 * Using this class a lightweight Visitor can be easily created in which only
 * the classes of interest must be implemented.
 * 
 * 
 * <p>
 * Created: Wed Oct 22 16:44:35 2003
 * 
 * @author imiller, last modified by $Author: imiller $
 * @version $Id: FailVisitor.java 88 2006-01-11 22:39:52Z imiller $
 */
public class FailVisitor implements Visitor {

	private String messageLocator;

	protected FailVisitor(String messageLocation) {
		messageLocator = messageLocation;
	}

	protected void fail(Visitable vis) {
		EngineThread.getEngine().fatalError(
				"Internal error at: " + messageLocator
						+ ".  Unexpected traversal of " + vis + " encountered");
	}

	@Override
	public void visit(AbsoluteMemoryRead vis) {
		fail(vis);
	}

	@Override
	public void visit(AbsoluteMemoryWrite vis) {
		fail(vis);
	}

	@Override
	public void visit(AddOp vis) {
		fail(vis);
	}

	@Override
	public void visit(And vis) {
		fail(vis);
	}

	@Override
	public void visit(AndOp vis) {
		fail(vis);
	}

	@Override
	public void visit(ArrayRead vis) {
		fail(vis);
	}

	@Override
	public void visit(ArrayWrite vis) {
		fail(vis);
	}

	@Override
	public void visit(Block vis) {
		fail(vis);
	}

	@Override
	public void visit(Branch vis) {
		fail(vis);
	}

	@Override
	public void visit(Call vis) {
		fail(vis);
	}

	@Override
	public void visit(CastOp vis) {
		fail(vis);
	}

	@Override
	public void visit(ComplementOp vis) {
		fail(vis);
	}

	@Override
	public void visit(ConditionalAndOp vis) {
		fail(vis);
	}

	@Override
	public void visit(ConditionalOrOp vis) {
		fail(vis);
	}

	@Override
	public void visit(Constant vis) {
		fail(vis);
	}

	@Override
	public void visit(Decision vis) {
		fail(vis);
	}

	@Override
	public void visit(Design vis) {
		fail(vis);
	}

	@Override
	public void visit(DivideOp vis) {
		fail(vis);
	}

	@Override
	public void visit(EncodedMux vis) {
		fail(vis);
	}

	@Override
	public void visit(EndianSwapper vis) {
		fail(vis);
	}

	@Override
	public void visit(EqualsOp vis) {
		fail(vis);
	}

	@Override
	public void visit(FifoAccess vis) {
		fail(vis);
	}

	@Override
	public void visit(FifoRead vis) {
		fail(vis);
	}

	@Override
	public void visit(FifoWrite vis) {
		fail(vis);
	}

	@Override
	public void visit(ForBody vis) {
		fail(vis);
	}

	@Override
	public void visit(GreaterThanEqualToOp vis) {
		fail(vis);
	}

	@Override
	public void visit(GreaterThanOp vis) {
		fail(vis);
	}

	@Override
	public void visit(HeapWrite vis) {
		fail(vis);
	}

	@Override
	public void visit(HeapRead vis) {
		fail(vis);
	}

	@Override
	public void visit(InBuf vis) {
		fail(vis);
	}

	@Override
	public void visit(IPCoreCall vis) {
		fail(vis);
	}

	@Override
	public void visit(Kicker vis) {
		fail(vis);
	}

	@Override
	public void visit(Latch vis) {
		fail(vis);
	}

	@Override
	public void visit(LeftShiftOp vis) {
		fail(vis);
	}

	@Override
	public void visit(LessThanEqualToOp vis) {
		fail(vis);
	}

	@Override
	public void visit(LessThanOp vis) {
		fail(vis);
	}

	@Override
	public void visit(LocationConstant vis) {
		fail(vis);
	}

	@Override
	public void visit(Loop vis) {
		fail(vis);
	}

	@Override
	public void visit(MemoryBank vis) {
		fail(vis);
	}

	@Override
	public void visit(MemoryGateway vis) {
		fail(vis);
	}

	@Override
	public void visit(MemoryRead vis) {
		fail(vis);
	}

	@Override
	public void visit(MemoryReferee vis) {
		fail(vis);
	}

	@Override
	public void visit(MemoryWrite vis) {
		fail(vis);
	}

	@Override
	public void visit(MinusOp vis) {
		fail(vis);
	}

	@Override
	public void visit(ModuloOp vis) {
		fail(vis);
	}

	@Override
	public void visit(MultiplyOp vis) {
		fail(vis);
	}

	@Override
	public void visit(Mux vis) {
		fail(vis);
	}

	@Override
	public void visit(NoOp vis) {
		fail(vis);
	}

	@Override
	public void visit(Not vis) {
		fail(vis);
	}

	@Override
	public void visit(NotEqualsOp vis) {
		fail(vis);
	}

	@Override
	public void visit(NotOp vis) {
		fail(vis);
	}

	@Override
	public void visit(NumericPromotionOp vis) {
		fail(vis);
	}

	@Override
	public void visit(Or vis) {
		fail(vis);
	}

	@Override
	public void visit(OrOp vis) {
		fail(vis);
	}

	@Override
	public void visit(OutBuf vis) {
		fail(vis);
	}

	@Override
	public void visit(PinRead vis) {
		fail(vis);
	}

	@Override
	public void visit(PinReferee vis) {
		fail(vis);
	}

	@Override
	public void visit(PinStateChange vis) {
		fail(vis);
	}

	@Override
	public void visit(PinWrite vis) {
		fail(vis);
	}

	@Override
	public void visit(PlusOp vis) {
		fail(vis);
	}

	@Override
	public void visit(PriorityMux vis) {
		fail(vis);
	}

	@Override
	public void visit(Procedure vis) {
		fail(vis);
	}

	@Override
	public void visit(ReductionOrOp vis) {
		fail(vis);
	}

	@Override
	public void visit(Reg vis) {
		fail(vis);
	}

	@Override
	public void visit(RegisterGateway vis) {
		fail(vis);
	}

	@Override
	public void visit(RegisterRead vis) {
		fail(vis);
	}

	@Override
	public void visit(RegisterReferee vis) {
		fail(vis);
	}

	@Override
	public void visit(RegisterWrite vis) {
		fail(vis);
	}

	@Override
	public void visit(RightShiftOp vis) {
		fail(vis);
	}

	@Override
	public void visit(RightShiftUnsignedOp vis) {
		fail(vis);
	}

	@Override
	public void visit(Scoreboard vis) {
		fail(vis);
	}

	@Override
	public void visit(ShortcutIfElseOp vis) {
		fail(vis);
	}

	@Override
	public void visit(SimplePin vis) {
		fail(vis);
	}

	@Override
	public void visit(SimplePinAccess vis) {
		fail(vis);
	}

	@Override
	public void visit(SimplePinRead vis) {
		fail(vis);
	}

	@Override
	public void visit(SimplePinWrite vis) {
		fail(vis);
	}

	@Override
	public void visit(SRL16 vis) {
		fail(vis);
	}

	@Override
	public void visit(SubtractOp vis) {
		fail(vis);
	}

	@Override
	public void visit(Switch vis) {
		fail(vis);
	}

	@Override
	public void visit(Task vis) {
		fail(vis);
	}

	@Override
	public void visit(TaskCall vis) {
		fail(vis);
	}

	@Override
	public void visit(TimingOp vis) {
		fail(vis);
	}

	@Override
	public void visit(TriBuf vis) {
		fail(vis);
	}

	@Override
	public void visit(UntilBody vis) {
		fail(vis);
	}

	@Override
	public void visit(WhileBody vis) {
		fail(vis);
	}

	@Override
	public void visit(XorOp vis) {
		fail(vis);
	}

}// FailVisitor
