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
package net.sf.openforge.verilog.model;

import net.sf.openforge.lim.ArrayRead;
import net.sf.openforge.lim.ArrayWrite;
import net.sf.openforge.lim.Block;
import net.sf.openforge.lim.Branch;
import net.sf.openforge.lim.Bus;
import net.sf.openforge.lim.Call;
import net.sf.openforge.lim.Decision;
import net.sf.openforge.lim.Design;
import net.sf.openforge.lim.ForBody;
import net.sf.openforge.lim.HeapRead;
import net.sf.openforge.lim.HeapWrite;
import net.sf.openforge.lim.IPCoreCall;
import net.sf.openforge.lim.InBuf;
import net.sf.openforge.lim.Kicker;
import net.sf.openforge.lim.Latch;
import net.sf.openforge.lim.Loop;
import net.sf.openforge.lim.OutBuf;
import net.sf.openforge.lim.PinRead;
import net.sf.openforge.lim.PinReferee;
import net.sf.openforge.lim.PinStateChange;
import net.sf.openforge.lim.PinWrite;
import net.sf.openforge.lim.PriorityMux;
import net.sf.openforge.lim.Procedure;
import net.sf.openforge.lim.RegisterGateway;
import net.sf.openforge.lim.RegisterRead;
import net.sf.openforge.lim.RegisterReferee;
import net.sf.openforge.lim.RegisterWrite;
import net.sf.openforge.lim.Scoreboard;
import net.sf.openforge.lim.Switch;
import net.sf.openforge.lim.Task;
import net.sf.openforge.lim.TaskCall;
import net.sf.openforge.lim.TriBuf;
import net.sf.openforge.lim.UntilBody;
import net.sf.openforge.lim.Visitor;
import net.sf.openforge.lim.WhileBody;
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
import net.sf.openforge.lim.primitive.And;
import net.sf.openforge.lim.primitive.EncodedMux;
import net.sf.openforge.lim.primitive.Mux;
import net.sf.openforge.lim.primitive.Not;
import net.sf.openforge.lim.primitive.Or;
import net.sf.openforge.lim.primitive.Reg;
import net.sf.openforge.lim.primitive.SRL16;
import net.sf.openforge.verilog.pattern.BusRegister;
import net.sf.openforge.verilog.pattern.BusWire;

/**
 * NetFactory is a factory to create different types of Net objects based on the
 * owner of the bus provided. For example if the owner is a Reg , the Factory
 * returns a BusRegister.
 * 
 * Note: Currently the NetFactory is only used to create BusRegisters and
 * BusWires
 * 
 * @author gandhij, last modified by $Author: imiller $
 * @version $Id: NetFactory.java 88 2006-01-11 22:39:52Z imiller $
 */
public class NetFactory implements Visitor {

	Net net = null;
	Bus bus = null;

	/*
	 * Get the newly created Net
	 */
	public Net getNet() {
		return net;
	}

	@Override
	public void visit(AbsoluteMemoryRead read) {
	}

	@Override
	public void visit(AbsoluteMemoryWrite write) {
	}

	@Override
	public void visit(AddOp add) {
	}

	@Override
	public void visit(And a) {
	}

	@Override
	public void visit(AndOp and) {
	}

	@Override
	public void visit(ArrayRead arrayRead) {
	}

	@Override
	public void visit(ArrayWrite arrayWrite) {
	}

	@Override
	public void visit(Block block) {
	}

	@Override
	public void visit(Branch branch) {
	}

	@Override
	public void visit(Call call) {
	}

	@Override
	public void visit(CastOp cast) {
	}

	@Override
	public void visit(ComplementOp complement) {
	}

	@Override
	public void visit(ConditionalAndOp conditionalAnd) {
	}

	@Override
	public void visit(ConditionalOrOp conditionalOr) {
	}

	@Override
	public void visit(net.sf.openforge.lim.op.Constant constant) {
	}

	@Override
	public void visit(Decision decision) {
	}

	@Override
	public void visit(Design design) {
	}

	@Override
	public void visit(DivideOp divide) {
	}

	@Override
	public void visit(EncodedMux m) {
		/*
		 * The encoded mux is handled based on the number of data ports ..
		 */
		if (m.getDataPorts().size() == 2) {
			net = new BusWire(bus);
		} else {
			net = new BusRegister(bus);
		}
	}

	@Override
	public void visit(EndianSwapper mod) {
	}

	@Override
	public void visit(EqualsOp equals) {
	}

	@Override
	public void visit(FifoAccess mod) {
	}

	@Override
	public void visit(FifoRead mod) {
	}

	@Override
	public void visit(FifoWrite mod) {
	}

	@Override
	public void visit(ForBody forBody) {
	}

	@Override
	public void visit(GreaterThanEqualToOp greaterThanEqualTo) {
	}

	@Override
	public void visit(GreaterThanOp greaterThan) {
	}

	@Override
	public void visit(HeapRead heapRead) {
	}

	@Override
	public void visit(HeapWrite heapWrite) {
	}

	@Override
	public void visit(InBuf ib) {
	}

	@Override
	public void visit(IPCoreCall call) {
	}

	@Override
	public void visit(Kicker kicker) {
	}

	@Override
	public void visit(Latch latch) {
	}

	@Override
	public void visit(LeftShiftOp leftShift) {
	}

	@Override
	public void visit(LessThanEqualToOp lessThanEqualTo) {
	}

	@Override
	public void visit(LessThanOp lessThan) {
	}

	@Override
	public void visit(LocationConstant loc) {
	}

	@Override
	public void visit(Loop loop) {
	}

	@Override
	public void visit(MemoryBank memBank) {
	}

	@Override
	public void visit(MemoryGateway mg) {
	}

	@Override
	public void visit(MemoryRead memRead) {
	}

	@Override
	public void visit(MemoryReferee memReferee) {
	}

	@Override
	public void visit(MemoryWrite memWrite) {
	}

	@Override
	public void visit(MinusOp minus) {
	}

	@Override
	public void visit(ModuloOp modulo) {
	}

	@Override
	public void visit(MultiplyOp multiply) {
	}

	@Override
	public void visit(Mux m) {
	}

	@Override
	public void visit(NoOp nop) {
	}

	@Override
	public void visit(Not n) {
	}

	@Override
	public void visit(NotEqualsOp notEquals) {
	}

	@Override
	public void visit(NotOp not) {
	}

	@Override
	public void visit(NumericPromotionOp numericPromotion) {
	}

	@Override
	public void visit(Or o) {
	}

	@Override
	public void visit(OrOp or) {
	}

	@Override
	public void visit(OutBuf ob) {
	}

	@Override
	public void visit(PinRead pinRead) {
	}

	@Override
	public void visit(PinReferee pinReferee) {
	}

	@Override
	public void visit(PinStateChange pinChange) {
	}

	@Override
	public void visit(PinWrite pinWrite) {
	}

	@Override
	public void visit(PlusOp plus) {
	}

	@Override
	public void visit(PriorityMux pmux) {
	}

	@Override
	public void visit(Procedure procedure) {
	}

	@Override
	public void visit(ReductionOrOp reductionOr) {
	}

	@Override
	public void visit(Reg reg) {
		net = new BusRegister(bus, reg.getInitialValue());
	}

	@Override
	public void visit(RegisterGateway gw) {
	}

	@Override
	public void visit(RegisterRead regRead) {
	}

	@Override
	public void visit(RegisterReferee regReferee) {
	}

	@Override
	public void visit(RegisterWrite regWrite) {
	}

	@Override
	public void visit(RightShiftOp rightShift) {
	}

	@Override
	public void visit(RightShiftUnsignedOp rightShiftUnsigned) {
	}

	@Override
	public void visit(Scoreboard scoreboard) {
	}

	@Override
	public void visit(ShortcutIfElseOp shortcutIfElse) {
	}

	@Override
	public void visit(SimplePinAccess mod) {
	}

	@Override
	public void visit(SimplePin notAcomp) {
	}

	@Override
	public void visit(SimplePinRead comp) {
	}

	@Override
	public void visit(SimplePinWrite comp) {
	}

	@Override
	public void visit(SRL16 srl16) {
	}

	@Override
	public void visit(SubtractOp subtract) {
	}

	@Override
	public void visit(Switch swich) {
	}

	@Override
	public void visit(Task task) {
	}

	@Override
	public void visit(TaskCall mod) {
	}

	@Override
	public void visit(TimingOp timingOp) {
	}

	@Override
	public void visit(TriBuf tbuf) {
	}

	@Override
	public void visit(UntilBody untilBody) {
	}

	@Override
	public void visit(WhileBody whileBody) {
	}

	@Override
	public void visit(XorOp xor) {
	}

	public void visit(Module m) {
	}

	/**
	 * Create a Net based on the owner of the bus Note: Currently used for
	 * creating BusWires and BusRegisters only.
	 * 
	 * @param bus
	 *            bus under consideration
	 * @return Net newly created net based on the owner of the bus.
	 * 
	 */
	public static Net makeNet(Bus bus) {

		NetFactory factory = new NetFactory();
		factory.bus = bus;

		try {
			bus.getOwner().getOwner().accept(factory);
		} catch (Exception ex) {
			// ex.printStackTrace();
		}

		if (factory.net == null) {
			// By default create a BusWire
			factory.net = new BusWire(bus);
		}

		return factory.getNet();

	}
}
