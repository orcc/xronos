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

package org.xronos.openforge.lim.util;

import java.util.LinkedList;
import java.util.List;

import org.xronos.openforge.lim.ArrayRead;
import org.xronos.openforge.lim.ArrayWrite;
import org.xronos.openforge.lim.Block;
import org.xronos.openforge.lim.Branch;
import org.xronos.openforge.lim.Call;
import org.xronos.openforge.lim.DataFlowVisitor;
import org.xronos.openforge.lim.Decision;
import org.xronos.openforge.lim.Design;
import org.xronos.openforge.lim.ForBody;
import org.xronos.openforge.lim.HeapRead;
import org.xronos.openforge.lim.HeapWrite;
import org.xronos.openforge.lim.IPCoreCall;
import org.xronos.openforge.lim.InBuf;
import org.xronos.openforge.lim.Kicker;
import org.xronos.openforge.lim.Latch;
import org.xronos.openforge.lim.Loop;
import org.xronos.openforge.lim.Module;
import org.xronos.openforge.lim.OutBuf;
import org.xronos.openforge.lim.PinRead;
import org.xronos.openforge.lim.PinReferee;
import org.xronos.openforge.lim.PinStateChange;
import org.xronos.openforge.lim.PinWrite;
import org.xronos.openforge.lim.PriorityMux;
import org.xronos.openforge.lim.Procedure;
import org.xronos.openforge.lim.RegisterGateway;
import org.xronos.openforge.lim.RegisterRead;
import org.xronos.openforge.lim.RegisterReferee;
import org.xronos.openforge.lim.RegisterWrite;
import org.xronos.openforge.lim.Scoreboard;
import org.xronos.openforge.lim.Switch;
import org.xronos.openforge.lim.Task;
import org.xronos.openforge.lim.TaskCall;
import org.xronos.openforge.lim.TriBuf;
import org.xronos.openforge.lim.UntilBody;
import org.xronos.openforge.lim.WhileBody;
import org.xronos.openforge.lim.io.FifoAccess;
import org.xronos.openforge.lim.io.FifoRead;
import org.xronos.openforge.lim.io.FifoWrite;
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
import org.xronos.openforge.util.naming.ID;


/**
 * 
 * 
 * Created: Tue Jul 13 2004
 * 
 * @author madhupc
 * @version $Id: OrderFinder.java 88 2006-01-11 22:39:52Z imiller $
 */
public class OrderFinder extends DataFlowVisitor {

	private List<ID> order = new LinkedList<ID>();

	public OrderFinder() {
		// Call the constructor of the super class
		super();
		setRunForward(true);
	}

	public static List<ID> getOrder(Module module) {
		OrderFinder finder = new OrderFinder();
		finder.traverse(module);
		return finder.getOrder();
	}

	private List<ID> getOrder() {
		return order;
	}

	@Override
	public void visit(Design param1) {
		order.add(param1);
	}

	@Override
	public void visit(TaskCall param1) {
		order.add(param1);
	}

	@Override
	public void visit(Task param1) {
		order.add(param1);
	}

	@Override
	public void visit(Call param1) {
		order.add(param1);
	}

	@Override
	public void visit(IPCoreCall param1) {
	}

	@Override
	public void visit(Procedure param1) {
		order.add(param1);
	}

	@Override
	public void visit(Block param1) {
		order.add(param1);
	}

	@Override
	public void visit(Loop param1) {
		order.add(param1);
	}

	@Override
	public void visit(WhileBody param1) {
		order.add(param1);
	}

	@Override
	public void visit(UntilBody param1) {
		order.add(param1);
	}

	@Override
	public void visit(ForBody param1) {
		order.add(param1);
	}

	@Override
	public void visit(AddOp param1) {
		order.add(param1);
	}

	@Override
	public void visit(AndOp param1) {
		order.add(param1);
	}

	@Override
	public void visit(CastOp param1) {
		order.add(param1);
	}

	@Override
	public void visit(ComplementOp param1) {
		order.add(param1);
	}

	@Override
	public void visit(ConditionalAndOp param1) {
		order.add(param1);
	}

	@Override
	public void visit(ConditionalOrOp param1) {
		order.add(param1);
	}

	@Override
	public void visit(Constant param1) {
		order.add(param1);
	}

	@Override
	public void visit(LocationConstant loc) {
		order.add(loc);
	}

	@Override
	public void visit(DivideOp param1) {
		order.add(param1);
	}

	@Override
	public void visit(EqualsOp param1) {
		order.add(param1);
	}

	@Override
	public void visit(GreaterThanEqualToOp param1) {
		order.add(param1);
	}

	@Override
	public void visit(GreaterThanOp param1) {
		order.add(param1);
	}

	@Override
	public void visit(LeftShiftOp param1) {
		order.add(param1);
	}

	@Override
	public void visit(LessThanEqualToOp param1) {
		order.add(param1);
	}

	@Override
	public void visit(LessThanOp param1) {
		order.add(param1);
	}

	@Override
	public void visit(MinusOp param1) {
		order.add(param1);
	}

	@Override
	public void visit(ModuloOp param1) {
		order.add(param1);
	}

	@Override
	public void visit(MultiplyOp param1) {
		order.add(param1);
	}

	@Override
	public void visit(NotEqualsOp param1) {
		order.add(param1);
	}

	@Override
	public void visit(NotOp param1) {
		order.add(param1);
	}

	@Override
	public void visit(OrOp param1) {
		order.add(param1);
	}

	@Override
	public void visit(PlusOp param1) {
		order.add(param1);
	}

	@Override
	public void visit(ReductionOrOp param1) {
		order.add(param1);
	}

	@Override
	public void visit(RightShiftOp param1) {
		order.add(param1);
	}

	@Override
	public void visit(RightShiftUnsignedOp param1) {
		order.add(param1);
	}

	@Override
	public void visit(ShortcutIfElseOp param1) {
		order.add(param1);
	}

	@Override
	public void visit(SubtractOp param1) {
		order.add(param1);
	}

	@Override
	public void visit(NumericPromotionOp param1) {
		order.add(param1);
	}

	@Override
	public void visit(XorOp param1) {
		order.add(param1);
	}

	@Override
	public void visit(Branch param1) {
		order.add(param1);
	}

	@Override
	public void visit(Decision param1) {
		order.add(param1);
	}

	@Override
	public void visit(Switch param1) {
		order.add(param1);
	}

	@Override
	public void visit(InBuf param1) {
		order.add(param1);
	}

	@Override
	public void visit(OutBuf param1) {
		order.add(param1);
	}

	@Override
	public void visit(Reg param1) {
		order.add(param1);
	}

	@Override
	public void visit(Mux param1) {
		order.add(param1);
	}

	@Override
	public void visit(EncodedMux param1) {
		order.add(param1);
	}

	@Override
	public void visit(PriorityMux param1) {
		order.add(param1);
	}

	@Override
	public void visit(And param1) {
		order.add(param1);
	}

	@Override
	public void visit(Not param1) {
		order.add(param1);
	}

	@Override
	public void visit(Or or) {
		order.add(or);
	}

	@Override
	public void visit(Scoreboard param1) {
		order.add(param1);
	}

	@Override
	public void visit(Latch param1) {
		order.add(param1);
	}

	@Override
	public void visit(NoOp param1) {
		order.add(param1);
	}

	@Override
	public void visit(TimingOp param1) {
		order.add(param1);
	}

	@Override
	public void visit(RegisterRead param1) {
		order.add(param1);
	}

	@Override
	public void visit(RegisterWrite param1) {
		order.add(param1);
	}

	@Override
	public void visit(RegisterGateway param1) {
		order.add(param1);
	}

	@Override
	public void visit(RegisterReferee param1) {
		order.add(param1);
	}

	@Override
	public void visit(MemoryBank param1) {
		order.add(param1);
	}

	@Override
	public void visit(MemoryRead memoryRead) {
		order.add(memoryRead);
	}

	@Override
	public void visit(MemoryWrite memoryWrite) {
		order.add(memoryWrite);
	}

	@Override
	public void visit(MemoryReferee param1) {
		order.add(param1);
	}

	@Override
	public void visit(MemoryGateway param1) {
		order.add(param1);
	}

	@Override
	public void visit(HeapRead param1) {
		order.add(param1);
	}

	@Override
	public void visit(ArrayRead param1) {
		order.add(param1);
	}

	@Override
	public void visit(HeapWrite param1) {
		order.add(param1);
	}

	@Override
	public void visit(ArrayWrite param1) {
		order.add(param1);
	}

	@Override
	public void visit(AbsoluteMemoryRead param1) {
		order.add(param1);
	}

	@Override
	public void visit(AbsoluteMemoryWrite param1) {
		order.add(param1);
	}

	@Override
	public void visit(Kicker param1) {
		order.add(param1);
	}

	@Override
	public void visit(PinRead param1) {
		order.add(param1);
	}

	@Override
	public void visit(PinWrite param1) {
		order.add(param1);
	}

	@Override
	public void visit(PinStateChange param1) {
		order.add(param1);
	}

	@Override
	public void visit(SRL16 param1) {
		order.add(param1);
	}

	@Override
	public void visit(PinReferee param1) {
		order.add(param1);
	}

	@Override
	public void visit(TriBuf param1) {
		order.add(param1);
	}

	@Override
	public void visit(SimplePinAccess param1) {
		order.add(param1);
	}

	@Override
	public void visit(SimplePinRead param1) {
		order.add(param1);
	}

	@Override
	public void visit(SimplePinWrite param1) {
		order.add(param1);
	}

	@Override
	public void visit(FifoAccess param1) {
		order.add(param1);
	}

	@Override
	public void visit(FifoRead param1) {
		order.add(param1);
	}

	@Override
	public void visit(FifoWrite param1) {
		order.add(param1);
	}

	@Override
	public void visit(EndianSwapper param1) {
		order.add(param1);
	}
}
