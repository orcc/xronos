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

import net.sf.openforge.lim.And;
import net.sf.openforge.lim.ArrayRead;
import net.sf.openforge.lim.ArrayWrite;
import net.sf.openforge.lim.Block;
import net.sf.openforge.lim.Branch;
import net.sf.openforge.lim.Bus;
import net.sf.openforge.lim.Call;
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

	public void visit(AbsoluteMemoryRead read) {
	}

	public void visit(AbsoluteMemoryWrite write) {
	}

	public void visit(AddOp add) {
	}

	public void visit(And a) {
	}

	public void visit(AndOp and) {
	}

	public void visit(ArrayRead arrayRead) {
	}

	public void visit(ArrayWrite arrayWrite) {
	}

	public void visit(Block block) {
	}

	public void visit(Branch branch) {
	}

	public void visit(Call call) {
	}

	public void visit(CastOp cast) {
	}

	public void visit(ComplementOp complement) {
	}

	public void visit(ConditionalAndOp conditionalAnd) {
	}

	public void visit(ConditionalOrOp conditionalOr) {
	}

	public void visit(net.sf.openforge.lim.op.Constant constant) {
	}

	public void visit(Decision decision) {
	}

	public void visit(Design design) {
	}

	public void visit(DivideOp divide) {
	}

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

	public void visit(EndianSwapper mod) {
	}

	public void visit(EqualsOp equals) {
	}

	public void visit(FifoAccess mod) {
	}

	public void visit(FifoRead mod) {
	}

	public void visit(FifoWrite mod) {
	}

	public void visit(ForBody forBody) {
	}

	public void visit(GreaterThanEqualToOp greaterThanEqualTo) {
	}

	public void visit(GreaterThanOp greaterThan) {
	}

	public void visit(HeapRead heapRead) {
	}

	public void visit(HeapWrite heapWrite) {
	}

	public void visit(InBuf ib) {
	}

	public void visit(IPCoreCall call) {
	}

	public void visit(Kicker kicker) {
	}

	public void visit(Latch latch) {
	}

	public void visit(LeftShiftOp leftShift) {
	}

	public void visit(LessThanEqualToOp lessThanEqualTo) {
	}

	public void visit(LessThanOp lessThan) {
	}

	public void visit(LocationConstant loc) {
	}

	public void visit(Loop loop) {
	}

	public void visit(MemoryBank memBank) {
	}

	public void visit(MemoryGateway mg) {
	}

	public void visit(MemoryRead memRead) {
	}

	public void visit(MemoryReferee memReferee) {
	}

	public void visit(MemoryWrite memWrite) {
	}

	public void visit(MinusOp minus) {
	}

	public void visit(ModuloOp modulo) {
	}

	public void visit(MultiplyOp multiply) {
	}

	public void visit(Mux m) {
	}

	public void visit(NoOp nop) {
	}

	public void visit(Not n) {
	}

	public void visit(NotEqualsOp notEquals) {
	}

	public void visit(NotOp not) {
	}

	public void visit(NumericPromotionOp numericPromotion) {
	}

	public void visit(Or o) {
	}

	public void visit(OrOp or) {
	}

	public void visit(OutBuf ob) {
	}

	public void visit(PinRead pinRead) {
	}

	public void visit(PinReferee pinReferee) {
	}

	public void visit(PinStateChange pinChange) {
	}

	public void visit(PinWrite pinWrite) {
	}

	public void visit(PlusOp plus) {
	}

	public void visit(PriorityMux pmux) {
	}

	public void visit(Procedure procedure) {
	}

	public void visit(ReductionOrOp reductionOr) {
	}

	public void visit(Reg reg) {
		net = new BusRegister(bus, reg.getInitialValue());
	}

	public void visit(RegisterGateway gw) {
	}

	public void visit(RegisterRead regRead) {
	}

	public void visit(RegisterReferee regReferee) {
	}

	public void visit(RegisterWrite regWrite) {
	}

	public void visit(RightShiftOp rightShift) {
	}

	public void visit(RightShiftUnsignedOp rightShiftUnsigned) {
	}

	public void visit(Scoreboard scoreboard) {
	}

	public void visit(ShortcutIfElseOp shortcutIfElse) {
	}

	public void visit(SimplePinAccess mod) {
	}

	public void visit(SimplePin notAcomp) {
	}

	public void visit(SimplePinRead comp) {
	}

	public void visit(SimplePinWrite comp) {
	}

	public void visit(SRL16 srl16) {
	}

	public void visit(SubtractOp subtract) {
	}

	public void visit(Switch swich) {
	}

	public void visit(Task task) {
	}

	public void visit(TaskCall mod) {
	}

	public void visit(TimingOp timingOp) {
	}

	public void visit(TriBuf tbuf) {
	}

	public void visit(UntilBody untilBody) {
	}

	public void visit(WhileBody whileBody) {
	}

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
