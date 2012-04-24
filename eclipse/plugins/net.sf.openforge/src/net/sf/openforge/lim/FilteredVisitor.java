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
 * * ContainmentVisitor provides an implementation of the Visitor interface
 * which traverses an entire LIM model.
 * <P>
 * Visited objects are filtered according to their generalized higher-level
 * classes, with a common method called for each group. This allows sub-classes
 * to focus on general processing, adding special cases only when needed.
 * <P>
 * The higher-level classes currently supported are:
 * <UL>
 * <LI>{@link Operation} -- calls filter(Operation o)
 * <LI>{@link Module} -- calls filter(Module m)
 * <LI>{@link Primitive} -- calls filter(Primitive p)
 * </UL>
 * Each of the filter() methods call filterAny(Component c) to allow general
 * handling of all Components.
 * <P>
 * <b>Note</b>: In each visit(), the LIM class is visited first, which may
 * involve traversing its children, <i>before</i> the appropriate filter() is
 * called.
 * <P>
 * There is a set of traverse() methods which are used to implement a traversal
 * of the contents of any container object. By default, the traverse() methods
 * call traverseAny(Module), which performs the content traversal by following
 * the container-component relationships of Modules. The ordering of Component
 * visits within a Module is unpredictable, but all Components are guaranteed to
 * be visited.
 * 
 * FilteredVisitor traverses a LIM model by using a Scanner to enter all
 * containers. This follows the ordering of the {@link DefaultVisitor} for
 * Components, though it is not guaranteed that all Components will be visited.
 * 
 * @see Visitable
 */
public class FilteredVisitor implements Visitor {

	protected Scanner scanner;

	public FilteredVisitor() {
		this(false);
	}

	public FilteredVisitor(boolean traversesComposable) {
		scanner = new Scanner(this);
		scanner.setTraverseComposable(traversesComposable);
	}

	@Override
	public void visit(Design design) {
		traverse(design);
	}

	@Override
	public void visit(Task task) {
		traverse(task);
	}

	@Override
	public void visit(Call call) {
		preFilter(call);
		traverse(call);
		filter(call);
	}

	@Override
	public void visit(IPCoreCall call) {
	}

	@Override
	public void visit(Procedure procedure) {
		traverse(procedure);
	}

	@Override
	public void visit(Block block) {
		preFilter(block);
		traverse(block);
		filter(block);
	}

	@Override
	public void visit(HeapRead heapRead) {
		preFilter(heapRead);
		traverse(heapRead);
		filter(heapRead);
	}

	@Override
	public void visit(ArrayRead arrayRead) {
		preFilter(arrayRead);
		traverse(arrayRead);
		filter(arrayRead);
	}

	@Override
	public void visit(AbsoluteMemoryRead read) {
		preFilter(read);
		traverse(read);
		filter(read);
	}

	@Override
	public void visit(HeapWrite heapWrite) {
		preFilter(heapWrite);
		traverse(heapWrite);
		filter(heapWrite);
	}

	@Override
	public void visit(ArrayWrite arrayWrite) {
		preFilter(arrayWrite);
		traverse(arrayWrite);
		filter(arrayWrite);
	}

	@Override
	public void visit(AbsoluteMemoryWrite write) {
		preFilter(write);
		traverse(write);
		filter(write);
	}

	@Override
	public void visit(Loop loop) {
		preFilter(loop);
		traverse(loop);
		filter(loop);
	}

	@Override
	public void visit(WhileBody whileBody) {
		preFilter(whileBody);
		traverse(whileBody);
		filter(whileBody);
	}

	@Override
	public void visit(UntilBody untilBody) {
		preFilter(untilBody);
		traverse(untilBody);
		filter(untilBody);
	}

	@Override
	public void visit(ForBody forBody) {
		preFilter(forBody);
		traverse(forBody);
		filter(forBody);
	}

	@Override
	public void visit(Decision decision) {
		preFilter(decision);
		traverse(decision);
		filter(decision);
	}

	@Override
	public void visit(Branch branch) {
		preFilter(branch);
		traverse(branch);
		filter(branch);
	}

	@Override
	public void visit(Switch sw) {
		preFilter(sw);
		traverse(sw);
		filter(sw);
	}

	@Override
	public void visit(FifoAccess mod) {
		preFilter(mod);
		traverse(mod);
		filter(mod);
	}

	@Override
	public void visit(FifoRead mod) {
		preFilter(mod);
		traverse(mod);
		filter(mod);
	}

	@Override
	public void visit(FifoWrite mod) {
		preFilter(mod);
		traverse(mod);
		filter(mod);
	}

	@Override
	public void visit(EndianSwapper mod) {
		preFilter(mod);
		traverse(mod);
		filter(mod);
	}

	@Override
	public void visit(AddOp add) {
		preFilter(add);
		filter(add);
	}

	@Override
	public void visit(AndOp and) {
		preFilter(and);
		filter(and);
	}

	@Override
	public void visit(NumericPromotionOp numericPromotion) {
		preFilter(numericPromotion);
		filter(numericPromotion);
	}

	@Override
	public void visit(CastOp cast) {
		preFilter(cast);
		filter(cast);
	}

	@Override
	public void visit(ComplementOp complement) {
		preFilter(complement);
		filter(complement);
	}

	@Override
	public void visit(ConditionalAndOp conditionalAnd) {
		preFilter(conditionalAnd);
		filter(conditionalAnd);
	}

	@Override
	public void visit(ConditionalOrOp conditionalOr) {
		preFilter(conditionalOr);
		filter(conditionalOr);
	}

	@Override
	public void visit(Constant constant) {
		preFilter(constant);
		filter(constant);
	}

	@Override
	public void visit(LocationConstant loc) {
		visit((Constant) loc);
	}

	@Override
	public void visit(DivideOp divide) {
		preFilter(divide);
		filter(divide);
	}

	@Override
	public void visit(EqualsOp equals) {
		preFilter(equals);
		filter(equals);
	}

	@Override
	public void visit(GreaterThanEqualToOp greaterThanEqualTo) {
		preFilter(greaterThanEqualTo);
		filter(greaterThanEqualTo);
	}

	@Override
	public void visit(GreaterThanOp greaterThan) {
		preFilter(greaterThan);
		filter(greaterThan);
	}

	@Override
	public void visit(LeftShiftOp leftShift) {
		preFilter(leftShift);
		filter(leftShift);
	}

	@Override
	public void visit(LessThanEqualToOp lessThanEqualTo) {
		preFilter(lessThanEqualTo);
		filter(lessThanEqualTo);
	}

	@Override
	public void visit(LessThanOp lessThan) {
		preFilter(lessThan);
		filter(lessThan);
	}

	@Override
	public void visit(MinusOp minus) {
		preFilter(minus);
		filter(minus);
	}

	@Override
	public void visit(ModuloOp modulo) {
		preFilter(modulo);
		filter(modulo);
	}

	@Override
	public void visit(MultiplyOp multiply) {
		preFilter(multiply);
		filter(multiply);
	}

	@Override
	public void visit(NotEqualsOp notEquals) {
		preFilter(notEquals);
		filter(notEquals);
	}

	@Override
	public void visit(NoOp nop) {
		preFilter(nop);
		filter(nop);
	}

	@Override
	public void visit(TimingOp top) {
		preFilter(top);
		filter(top);
	}

	@Override
	public void visit(NotOp not) {
		preFilter(not);
		filter(not);
	}

	@Override
	public void visit(OrOp or) {
		preFilter(or);
		filter(or);
	}

	@Override
	public void visit(PlusOp plus) {
		preFilter(plus);
		filter(plus);
	}

	@Override
	public void visit(ReductionOrOp reductionOr) {
		preFilter(reductionOr);
		filter(reductionOr);
	}

	@Override
	public void visit(RightShiftOp rightShift) {
		preFilter(rightShift);
		filter(rightShift);
	}

	@Override
	public void visit(RightShiftUnsignedOp rightShiftUnsigned) {
		preFilter(rightShiftUnsigned);
		filter(rightShiftUnsigned);
	}

	@Override
	public void visit(ShortcutIfElseOp shortcutIfElse) {
		preFilter(shortcutIfElse);
		filter(shortcutIfElse);
	}

	@Override
	public void visit(SimplePin comp) {
		preFilterAny(comp);
		filterAny(comp);
	}

	@Override
	public void visit(SimplePinAccess mod) {
		preFilter(mod);
		traverse(mod);
		filter(mod);
	}

	@Override
	public void visit(SimplePinRead comp) {
		preFilterAny(comp);
		filterAny(comp);
	}

	@Override
	public void visit(SimplePinWrite comp) {
		preFilterAny(comp);
		filterAny(comp);
	}

	@Override
	public void visit(TaskCall mod) {
		preFilter(mod);
		traverse(mod);
		filter(mod);
	}

	@Override
	public void visit(SubtractOp subtract) {
		preFilter(subtract);
		filter(subtract);
	}

	@Override
	public void visit(XorOp xor) {
		preFilter(xor);
		filter(xor);
	}

	@Override
	public void visit(InBuf ib) {
		preFilterAny(ib);
		filterAny(ib);
	}

	@Override
	public void visit(OutBuf ob) {
		preFilterAny(ob);
		filterAny(ob);
	}

	@Override
	public void visit(PriorityMux pmux) {
		preFilter(pmux);
		traverse(pmux);
		filter(pmux);
	}

	@Override
	public void visit(EncodedMux m) {
		preFilter(m);
		filter(m);
	}

	@Override
	public void visit(Mux m) {
		preFilter(m);
		filter(m);
	}

	@Override
	public void visit(And a) {
		preFilter(a);
		filter(a);
	}

	@Override
	public void visit(Not n) {
		preFilter(n);
		filter(n);
	}

	@Override
	public void visit(Or o) {
		preFilter(o);
		filter(o);
	}

	@Override
	public void visit(Reg reg) {
		preFilter(reg);
		filter(reg);
	}

	@Override
	public void visit(SRL16 srl16) {
		preFilter(srl16);
		filter(srl16);
	}

	@Override
	public void visit(Scoreboard scoreboard) {
		preFilter(scoreboard);
		traverse(scoreboard);
		filter(scoreboard);
	}

	@Override
	public void visit(RegisterRead rr) {
		preFilter(rr);
		filter(rr);
	}

	@Override
	public void visit(RegisterWrite rw) {
		preFilter(rw);
		filter(rw);
	}

	@Override
	public void visit(RegisterReferee regReferee) {
		preFilter(regReferee);
		traverse(regReferee);
		filter(regReferee);
	}

	@Override
	public void visit(RegisterGateway regGateway) {
		preFilter(regGateway);
		traverse(regGateway);
		filter(regGateway);
	}

	@Override
	public void visit(MemoryReferee memReferee) {
		preFilter(memReferee);
		traverse(memReferee);
		filter(memReferee);
	}

	@Override
	public void visit(MemoryGateway memGateway) {
		preFilter(memGateway);
		traverse(memGateway);
		filter(memGateway);
	}

	/**
	 * Traverse the MemoryRead to be sure that we pick up any Physical
	 * implementation that exists for the component.
	 */
	@Override
	public void visit(MemoryRead memRead) {
		preFilter(memRead);
		traverse(memRead);
		filter(memRead);
	}

	@Override
	public void visit(MemoryBank comp) {
		preFilterAny(comp);
		filterAny(comp);
	}

	/**
	 * Traverse the MemoryWrite to be sure that we pick up any Physical
	 * implementation that exists for the component.
	 */
	@Override
	public void visit(MemoryWrite memWrite) {
		preFilter(memWrite);
		traverse(memWrite);
		filter(memWrite);
	}

	/**
	 * Traverse the PinRead to be sure that we pick up any Physical
	 * implementation that exists for the component.
	 */
	@Override
	public void visit(PinRead pinRead) {
		preFilter(pinRead);
		traverse(pinRead);
		filter(pinRead);
	}

	/**
	 * Traverse the PinWrite to be sure that we pick up any Physical
	 * implementation that exists for the component.
	 */
	@Override
	public void visit(PinWrite pinWrite) {
		preFilter(pinWrite);
		traverse(pinWrite);
		filter(pinWrite);
	}

	/**
	 * Traverse the PinStageChange to be sure that we pick up any Physical
	 * implementation that exists for the component.
	 */
	@Override
	public void visit(PinStateChange pinChange) {
		preFilter(pinChange);
		traverse(pinChange);
		filter(pinChange);
	}

	@Override
	public void visit(PinReferee pinReferee) {
		preFilter(pinReferee);
		traverse(pinReferee);
		filter(pinReferee);
	}

	@Override
	public void visit(Latch latch) {
		preFilter(latch);
		traverse(latch);
		filter(latch);
	}

	@Override
	public void visit(Kicker kicker) {
		preFilter(kicker);
		traverse(kicker);
		filter(kicker);
	}

	@Override
	public void visit(TriBuf tbuf) {
		preFilterAny(tbuf);
		filterAny(tbuf);
	}

	/**
	 * Handle any kind of {@link Module}. All visit(Module subclass) methods
	 * should call this as part of the visit. The default behavior of this
	 * method is to call filterAny(Component).
	 */
	public void filter(Module m) {
		if (_lim.db)
			_lim.ln("filter(Module): " + m.toString());
		filterAny(m);
	}

	public void preFilter(Module m) {
		if (_lim.db)
			_lim.ln("pre-filter(Module): " + m.toString());
		preFilterAny(m);
	}

	/**
	 * Handle any kind of {@link Operation}. All visit(Operation subclass)
	 * methods should call this as part of the visit.
	 */
	public void filter(Operation o) {
		if (_lim.db)
			_lim.ln("filter(Operation): " + o.toString());
		filterAny(o);
	}

	public void preFilter(Operation o) {
		if (_lim.db)
			_lim.ln("pre-filter(Operation): " + o.toString());
		preFilterAny(o);
	}

	/**
	 * Handle any kind of {@link Primitive}. All visit(Primitive subclass)
	 * methods should call this as part of the visit.
	 */
	public void filter(Primitive p) {
		if (_lim.db)
			_lim.ln("filter(Primitive): " + p.toString());
		filterAny(p);
	}

	public void preFilter(Primitive p) {
		if (_lim.db)
			_lim.ln("pre-filter(Primitive): " + p.toString());
		preFilterAny(p);
	}

	/**
	 * Handles {@link Call}. The visit(Call call) calls this as part of the
	 * visit
	 */
	public void filter(Call c) {
		if (_lim.db)
			_lim.ln("filter(Call): " + c.toString());
		filterAny(c);
	}

	public void preFilter(Call c) {
		if (_lim.db)
			_lim.ln("pre-filter(Call): " + c.toString());
		preFilterAny(c);
	}

	/**
	 * The default behavior of each filter() method is to call this, allowing
	 * any component to be handled generically.
	 * 
	 * @param c
	 *            the component that was visited
	 */
	public void filterAny(Component c) {
		if (_lim.db)
			_lim.ln("filterAny(Component): " + c.toString());
	}

	public void preFilterAny(Component c) {
		if (_lim.db)
			_lim.ln("pre-filterAny(Component): " + c.toString());
	}

	protected void traverse(Design d) {
		if (_lim.db)
			_lim.ln("FilteredScan.traverse(Design)");
		scanner.enter(d);
	}

	protected void traverse(Task t) {
		if (_lim.db)
			_lim.ln("FilteredScan.traverse(Task)");
		scanner.enter(t);
	}

	protected void traverse(Call c) {
		if (_lim.db)
			_lim.ln("FilteredScan.traverse(Call)");
		scanner.enter(c);
	}

	protected void traverse(Procedure p) {
		if (_lim.db)
			_lim.ln("FilteredScan.traverse(Procedure)");
		scanner.enter(p);
	}

	protected void traverse(Block block) {
		scanner.enter(block);
	}

	protected void traverse(Loop loop) {
		scanner.enter(loop);
	}

	protected void traverse(WhileBody whileBody) {
		scanner.enter(whileBody);
	}

	protected void traverse(UntilBody untilBody) {
		scanner.enter(untilBody);
	}

	protected void traverse(ForBody forBody) {
		scanner.enter(forBody);
	}

	protected void traverse(Decision decision) {
		scanner.enter(decision);
	}

	protected void traverse(Branch branch) {
		scanner.enter(branch);
	}

	protected void traverse(Switch sw) {
		scanner.enter(sw);
	}

	protected void traverse(PriorityMux pmux) {
		scanner.enter(pmux);
	}

	protected void traverse(RegisterReferee regReferee) {
		scanner.enter(regReferee);
	}

	protected void traverse(RegisterGateway regGateway) {
		scanner.enter(regGateway);
	}

	protected void traverse(MemoryReferee memReferee) {
		scanner.enter(memReferee);
	}

	protected void traverse(MemoryGateway memGateway) {
		scanner.enter(memGateway);
	}

	/**
	 * Traverse the MemoryRead to be sure that we pick up any Physical
	 * implementation that exists for the component.
	 */
	protected void traverse(MemoryRead comp) {
		scanner.enter(comp);
	}

	/**
	 * Traverse the MemoryWrite to be sure that we pick up any Physical
	 * implementation that exists for the component.
	 */
	protected void traverse(MemoryWrite comp) {
		scanner.enter(comp);
	}

	/**
	 * Traverse the PinRead to be sure that we pick up any Physical
	 * implementation that exists for the component.
	 */
	protected void traverse(PinRead comp) {
		scanner.enter(comp);
	}

	/**
	 * Traverse the PinWrite to be sure that we pick up any Physical
	 * implementation that exists for the component.
	 */
	protected void traverse(PinWrite comp) {
		scanner.enter(comp);
	}

	/**
	 * Traverse the PinStateChange to be sure that we pick up any Physical
	 * implementation that exists for the component.
	 */
	protected void traverse(PinStateChange comp) {
		scanner.enter(comp);
	}

	protected void traverse(PinReferee pinReferee) {
		scanner.enter(pinReferee);
	}

	protected void traverse(Latch latch) {
		scanner.enter(latch);
	}

	protected void traverse(Kicker kicker) {
		scanner.enter(kicker);
	}

	protected void traverse(Scoreboard scoreboard) {
		scanner.enter(scoreboard);
	}

	protected void traverse(TaskCall mod) {
		scanner.enter(mod);
	}

	protected void traverse(SimplePinAccess mod) {
		scanner.enter(mod);
	}

	protected void traverse(FifoAccess mod) {
		scanner.enter(mod);
	}

	protected void traverse(FifoRead mod) {
		scanner.enter(mod);
	}

	protected void traverse(FifoWrite mod) {
		scanner.enter(mod);
	}

	protected void traverse(EndianSwapper mod) {
		scanner.enter(mod);
	}

	protected void traverseAny(Module m) {
		if (_lim.db)
			_lim.ln("FilteredScan.traverse(Module)");
		assert (false) : "FilteredVisitor can't scanner.enter(Module)";
	}

} // class FilteredVisitor

