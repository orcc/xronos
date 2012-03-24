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

import net.sf.openforge.lim.And;
import net.sf.openforge.lim.ArrayRead;
import net.sf.openforge.lim.ArrayWrite;
import net.sf.openforge.lim.Block;
import net.sf.openforge.lim.Branch;
import net.sf.openforge.lim.Call;
import net.sf.openforge.lim.DataFlowVisitor;
import net.sf.openforge.lim.Decision;
import net.sf.openforge.lim.Design;
import net.sf.openforge.lim.EncodedMux;
import net.sf.openforge.lim.ForBody;
import net.sf.openforge.lim.HeapRead;
import net.sf.openforge.lim.HeapWrite;
import net.sf.openforge.lim.IPCoreCall;
import net.sf.openforge.lim.InBuf;
import net.sf.openforge.lim.Kicker;
import net.sf.openforge.lim.Latch;
import net.sf.openforge.lim.Loop;
import net.sf.openforge.lim.Module;
import net.sf.openforge.lim.Mux;
import net.sf.openforge.lim.Not;
import net.sf.openforge.lim.Or;
import net.sf.openforge.lim.OutBuf;
import net.sf.openforge.lim.PinRead;
import net.sf.openforge.lim.PinReferee;
import net.sf.openforge.lim.PinStateChange;
import net.sf.openforge.lim.PinWrite;
import net.sf.openforge.lim.PriorityMux;
import net.sf.openforge.lim.Procedure;
import net.sf.openforge.lim.Reg;
import net.sf.openforge.lim.RegisterGateway;
import net.sf.openforge.lim.RegisterRead;
import net.sf.openforge.lim.RegisterReferee;
import net.sf.openforge.lim.RegisterWrite;
import net.sf.openforge.lim.SRL16;
import net.sf.openforge.lim.Scoreboard;
import net.sf.openforge.lim.Switch;
import net.sf.openforge.lim.Task;
import net.sf.openforge.lim.TaskCall;
import net.sf.openforge.lim.TriBuf;
import net.sf.openforge.lim.UntilBody;
import net.sf.openforge.lim.WhileBody;
import net.sf.openforge.lim.io.FifoAccess;
import net.sf.openforge.lim.io.FifoRead;
import net.sf.openforge.lim.io.FifoWrite;
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
import net.sf.openforge.util.naming.ID;

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
