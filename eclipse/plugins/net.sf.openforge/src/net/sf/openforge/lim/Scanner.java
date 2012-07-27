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

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;

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
import net.sf.openforge.lim.primitive.And;
import net.sf.openforge.lim.primitive.EncodedMux;
import net.sf.openforge.lim.primitive.Mux;
import net.sf.openforge.lim.primitive.Not;
import net.sf.openforge.lim.primitive.Or;
import net.sf.openforge.lim.primitive.Reg;
import net.sf.openforge.lim.primitive.SRL16;

/**
 * One of the potential problems with defining a {@link Visitor} implementation
 * to be a subclass of {@link DefaultVisitor} is that it may hide a missing
 * method in the implementation, whereas a direct implementer of {@link Visitor}
 * would fail in compilation. Scanner mitigates that problem by providing
 * generic {@link Module} hierarchy traversal without the need to subclass
 * {@link DefaultVisitor}.
 * <P>
 * A {@link Visitor} implementer constructs a Scanner using itself as the
 * argument. Visiting then begins as usual. When a {@link Module} object is
 * visited, the visitor may continue the traversal into the module by calling,
 * for example, {@link Scanner#enter(Block)} (as opposed to a
 * {@link DefaultVisitor} subclass, which would call
 * <code>super.visit(block)</code>).
 * 
 * @version $Id: Scanner.java 88 2006-01-11 22:39:52Z imiller $
 */
public final class Scanner extends DefaultVisitor {

	/** The real visitor */
	private Visitor visitor;
	/**
	 * Set true if all iterators should protect against
	 * ConcurrentModificationExceptions
	 */
	private boolean safeMode;

	/**
	 * Constructs a new Scanner.
	 * 
	 * @param visitor
	 *            the visitor for which traversal is being performed
	 */
	public Scanner(Visitor visitor) {
		this(visitor, false);
	}

	/**
	 * Constructs a new scanner which may be put into safe mode, in which the
	 * iterators used to traverse module components operate on newly created
	 * lists to avoid concurrent modification exceptions.
	 * 
	 * @param visitor
	 *            the visitor for which traversal is being performed
	 * @param safeMode
	 *            true to create new lists to iterate over for all iterators.
	 */
	public Scanner(Visitor visitor, boolean safeMode) {
		this.visitor = visitor;
		this.safeMode = safeMode;
	}

	/*
	 * Modules.
	 */

	@Override
	public void visit(Design design) {
		visitor.visit(design);
	}

	/**
	 * Continue visiting inside a Design.
	 */
	public void enter(Design design) {
		super.visit(design);
	}

	@Override
	public void visit(Task task) {
		visitor.visit(task);
	}

	/**
	 * Continue visiting inside a Task.
	 */
	public void enter(Task task) {
		super.visit(task);
	}

	@Override
	public void visit(Call call) {
		visitor.visit(call);
	}

	/**
	 * Continue visiting inside a Call's Procedure.
	 */
	public void enter(Call call) {
		super.visit(call);
	}

	@Override
	public void visit(Procedure procedure) {
		visitor.visit(procedure);
	}

	/**
	 * Continue visiting inside a Procedure.
	 */
	public void enter(Procedure procedure) {
		super.visit(procedure);
	}

	@Override
	public void visit(Block block) {
		visitor.visit(block);
	}

	/**
	 * Continue visiting inside a Block.
	 */
	public void enter(Block block) {
		super.visit(block);
	}

	@Override
	public void visit(Loop loop) {
		visitor.visit(loop);
	}

	/**
	 * Continue visiting inside a Loop.
	 */
	public void enter(Loop loop) {
		super.visit(loop);
	}

	@Override
	public void visit(WhileBody whileBody) {
		visitor.visit(whileBody);
	}

	/**
	 * Continue visiting inside a WhileBody.
	 */
	public void enter(WhileBody whileBody) {
		super.visit(whileBody);
	}

	@Override
	public void visit(UntilBody untilBody) {
		visitor.visit(untilBody);
	}

	/**
	 * Continue visiting inside an UntilBody.
	 */
	public void enter(UntilBody untilBody) {
		super.visit(untilBody);
	}

	@Override
	public void visit(ForBody forBody) {
		visitor.visit(forBody);
	}

	/**
	 * Continue visiting inside a ForBody.
	 */
	public void enter(ForBody forBody) {
		super.visit(forBody);
	}

	@Override
	public void visit(Branch branch) {
		visitor.visit(branch);
	}

	/**
	 * Continue visiting inside a Branch.
	 */
	public void enter(Branch branch) {
		super.visit(branch);
	}

	@Override
	public void visit(Decision decision) {
		visitor.visit(decision);
	}

	/**
	 * Continue visiting inside a Decision.
	 */
	public void enter(Decision decision) {
		super.visit(decision);
	}

	@Override
	public void visit(Switch sw) {
		visitor.visit(sw);
	}

	/**
	 * Continue visiting inside a Switch.
	 */
	public void enter(Switch sw) {
		super.visit(sw);
	}

	public void enter(Scoreboard scbd) {
		super.visit(scbd);
	}

	@Override
	public void visit(Scoreboard scbd) {
		visitor.visit(scbd);
	}

	public void enter(PriorityMux pmux) {
		super.visit(pmux);
	}

	@Override
	public void visit(PriorityMux pmux) {
		visitor.visit(pmux);
	}

	public void enter(RegisterReferee regReferee) {
		super.visit(regReferee);
	}

	@Override
	public void visit(RegisterReferee regReferee) {
		visitor.visit(regReferee);
	}

	public void enter(RegisterGateway regGateway) {
		super.visit(regGateway);
	}

	@Override
	public void visit(RegisterGateway regGateway) {
		visitor.visit(regGateway);
	}

	public void enter(MemoryReferee memReferee) {
		super.visit(memReferee);
	}

	@Override
	public void visit(MemoryReferee memReferee) {
		visitor.visit(memReferee);
	}

	public void enter(MemoryGateway memGateway) {
		super.visit(memGateway);
	}

	@Override
	public void visit(MemoryGateway memGateway) {
		visitor.visit(memGateway);
	}

	public void enter(Latch latch) {
		super.visit(latch);
	}

	@Override
	public void visit(Latch latch) {
		visitor.visit(latch);
	}

	public void enter(Kicker kicker) {
		super.visit(kicker);
	}

	@Override
	public void visit(Kicker kicker) {
		visitor.visit(kicker);
	}

	public void enter(ArrayWrite comp) {
		super.visit(comp);
	}

	@Override
	public void visit(ArrayWrite comp) {
		visitor.visit(comp);
	}

	public void enter(ArrayRead comp) {
		super.visit(comp);
	}

	@Override
	public void visit(ArrayRead comp) {
		visitor.visit(comp);
	}

	public void enter(HeapWrite comp) {
		super.visit(comp);
	}

	@Override
	public void visit(HeapWrite comp) {
		visitor.visit(comp);
	}

	public void enter(HeapRead comp) {
		super.visit(comp);
	}

	@Override
	public void visit(HeapRead comp) {
		visitor.visit(comp);
	}

	public void enter(AbsoluteMemoryRead comp) {
		super.visit(comp);
	}

	@Override
	public void visit(AbsoluteMemoryRead comp) {
		visitor.visit(comp);
	}

	public void enter(AbsoluteMemoryWrite comp) {
		super.visit(comp);
	}

	@Override
	public void visit(AbsoluteMemoryWrite comp) {
		visitor.visit(comp);
	}

	public void enter(MemoryRead comp) {
		super.visit(comp);
	}

	@Override
	public void visit(MemoryRead comp) {
		visitor.visit(comp);
	}

	public void enter(MemoryWrite comp) {
		super.visit(comp);
	}

	@Override
	public void visit(MemoryWrite comp) {
		visitor.visit(comp);
	}

	public void enter(PinRead comp) {
		super.visit(comp);
	}

	@Override
	public void visit(PinRead comp) {
		visitor.visit(comp);
	}

	public void enter(PinWrite comp) {
		super.visit(comp);
	}

	@Override
	public void visit(PinWrite comp) {
		visitor.visit(comp);
	}

	public void enter(PinStateChange comp) {
		super.visit(comp);
	}

	@Override
	public void visit(PinStateChange comp) {
		visitor.visit(comp);
	}

	public void enter(PinReferee pinReferee) {
		super.visit(pinReferee);
	}

	@Override
	public void visit(PinReferee pinReferee) {
		visitor.visit(pinReferee);
	}

	public void enter(TaskCall mod) {
		super.visit(mod);
	}

	@Override
	public void visit(TaskCall mod) {
		visitor.visit(mod);
	}

	public void enter(SimplePinAccess mod) {
		super.visit(mod);
	}

	@Override
	public void visit(SimplePinAccess mod) {
		visitor.visit(mod);
	}

	public void enter(FifoAccess mod) {
		super.visit(mod);
	}

	@Override
	public void visit(FifoAccess mod) {
		visitor.visit(mod);
	}

	public void enter(FifoRead mod) {
		super.visit(mod);
	}

	@Override
	public void visit(FifoRead mod) {
		visitor.visit(mod);
	}

	public void enter(FifoWrite mod) {
		super.visit(mod);
	}

	@Override
	public void visit(FifoWrite mod) {
		visitor.visit(mod);
	}

	public void enter(EndianSwapper mod) {
		super.visit(mod);
	}

	@Override
	public void visit(EndianSwapper mod) {
		visitor.visit(mod);
	}

	/*
	 * Primitive Components.
	 */
	@Override
	public void visit(AddOp add) {
		visitor.visit(add);
	}

	@Override
	public void visit(AndOp andOp) {
		visitor.visit(andOp);
	}

	@Override
	public void visit(NumericPromotionOp numericPromotion) {
		visitor.visit(numericPromotion);
	}

	@Override
	public void visit(CastOp cast) {
		visitor.visit(cast);
	}

	@Override
	public void visit(ComplementOp complement) {
		visitor.visit(complement);
	}

	@Override
	public void visit(ConditionalAndOp conditionalAnd) {
		visitor.visit(conditionalAnd);
	}

	@Override
	public void visit(ConditionalOrOp conditionalOr) {
		visitor.visit(conditionalOr);
	}

	@Override
	public void visit(Constant constant) {
		visitor.visit(constant);
	}

	@Override
	public void visit(DivideOp divide) {
		visitor.visit(divide);
	}

	@Override
	public void visit(EqualsOp equals) {
		visitor.visit(equals);
	}

	@Override
	public void visit(GreaterThanEqualToOp greaterThanEqualTo) {
		visitor.visit(greaterThanEqualTo);
	}

	@Override
	public void visit(GreaterThanOp greaterThan) {
		visitor.visit(greaterThan);
	}

	@Override
	public void visit(LeftShiftOp leftShift) {
		visitor.visit(leftShift);
	}

	@Override
	public void visit(LessThanEqualToOp lessThanEqualTo) {
		visitor.visit(lessThanEqualTo);
	}

	@Override
	public void visit(LessThanOp lessThan) {
		visitor.visit(lessThan);
	}

	@Override
	public void visit(MinusOp minus) {
		visitor.visit(minus);
	}

	@Override
	public void visit(ModuloOp modulo) {
		visitor.visit(modulo);
	}

	@Override
	public void visit(MultiplyOp multiply) {
		visitor.visit(multiply);
	}

	@Override
	public void visit(NoOp nop) {
		visitor.visit(nop);
	}

	@Override
	public void visit(TimingOp top) {
		visitor.visit(top);
	}

	@Override
	public void visit(NotEqualsOp notEquals) {
		visitor.visit(notEquals);
	}

	@Override
	public void visit(NotOp not) {
		visitor.visit(not);
	}

	@Override
	public void visit(OrOp or) {
		visitor.visit(or);
	}

	@Override
	public void visit(PlusOp plus) {
		visitor.visit(plus);
	}

	@Override
	public void visit(ReductionOrOp reductionOr) {
		visitor.visit(reductionOr);
	}

	@Override
	public void visit(RegisterRead read) {
		visitor.visit(read);
	}

	@Override
	public void visit(RegisterWrite write) {
		visitor.visit(write);
	}

	@Override
	public void visit(RightShiftOp rightShift) {
		visitor.visit(rightShift);
	}

	@Override
	public void visit(RightShiftUnsignedOp rightShiftUnsigned) {
		visitor.visit(rightShiftUnsigned);
	}

	@Override
	public void visit(ShortcutIfElseOp shortcutIfElse) {
		visitor.visit(shortcutIfElse);
	}

	@Override
	public void visit(SubtractOp subtract) {
		visitor.visit(subtract);
	}

	@Override
	public void visit(XorOp xor) {
		visitor.visit(xor);
	}

	@Override
	public void visit(InBuf ib) {
		visitor.visit(ib);
	}

	@Override
	public void visit(OutBuf ob) {
		visitor.visit(ob);
	}

	@Override
	public void visit(Reg reg) {
		visitor.visit(reg);
	}

	@Override
	public void visit(SRL16 srl_16) {
		visitor.visit(srl_16);
	}

	@Override
	public void visit(Mux m) {
		visitor.visit(m);
	}

	@Override
	public void visit(And a) {
		visitor.visit(a);
	}

	@Override
	public void visit(Not n) {
		visitor.visit(n);
	}

	@Override
	public void visit(Or o) {
		visitor.visit(o);
	}

	@Override
	public void visit(EncodedMux m) {
		visitor.visit(m);
	}

	@Override
	public void visit(MemoryBank m) {
		visitor.visit(m);
	}

	@Override
	public void visit(SimplePin m) {
		visitor.visit(m);
	}

	@Override
	public void visit(SimplePinRead m) {
		visitor.visit(m);
	}

	@Override
	public void visit(SimplePinWrite m) {
		visitor.visit(m);
	}

	/**
	 * Creates a new LinkedList with the contents of the given collection and
	 * returns a new iterator over that linked list if this Scanner is in safe
	 * mode.
	 * 
	 * @param collection
	 *            the 'Collection' to iterate over
	 * @return an 'Iterator' over the given collection
	 */
	@Override
	protected Iterator getIterator(Collection collection) {
		if (safeMode) {
			return (new LinkedList(collection)).iterator();
		}

		return super.getIterator(collection);
	}

}
