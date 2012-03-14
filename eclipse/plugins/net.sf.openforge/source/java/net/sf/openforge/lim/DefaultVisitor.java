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

import java.io.OutputStream;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

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
import net.sf.openforge.util.IndentWriter;

/**
 * DefaultVisitor provides an implementation of the Visitor interface that just
 * traverses a LIM containment hierarchy. Subclasses that implement
 * {@link Visitor} may extend from this class and override just those methods
 * for the component types of interest; in those methods, a call to
 * <code>super.visit()</code> will continue the traversal.
 * <P>
 * If <code> _lim.db</code> is set, then a simply formatted report report of the
 * traversal is printed to the debug output stream.
 * 
 * @version $Id: DefaultVisitor.java 88 2006-01-11 22:39:52Z imiller $
 */
public class DefaultVisitor implements Visitor {
	private static final String _RCS_ = "$Rev: 88 $";

	/** The writer to use for debugging */
	private IndentWriter writer = null;

	/** The stream to use for debugging */
	private OutputStream stream = null;

	/**
	 * Set to true to allow the traversal to enter {@link Composable} components
	 */
	private boolean traverseComposable = false;

	public DefaultVisitor() {
		if (_lim.db) {
			this.stream = _lim.getPrintStream();
			this.writer = new IndentWriter(stream);
			writer.setIndentString("   ");
		}
	}

	/**
	 * Used to set whether this Visitor is to traverse into {@link Composable}
	 * Components.
	 */
	public void setTraverseComposable(boolean value) {
		this.traverseComposable = value;
	}

	public void visit(Design design) {
		if (_lim.db)
			writeln("Design: " + design.toString());

		// Cannot just visit the internals of the design module... we
		// will miss the visit (Task).
		// Actually, very few visitors care about the design level
		// components. For those that do, they can override the super
		// visitGenericModule(design.getDesignModule());

		// for (Iterator it = design.getTasks().iterator(); it.hasNext();)
		for (Iterator it = getIterator(design.getTasks()); it.hasNext();) {
			if (_lim.db)
				writer.inc();
			Task task = (Task) it.next();
			task.accept(this);
			if (_lim.db)
				writer.decrease();
		}
	}

	public void visit(Task task) {
		if (_lim.db)
			writeln("Task: " + task.toString());

		if (task.getCall() != null) {
			if (_lim.db)
				writer.inc();
			task.getCall().accept(this);
			if (_lim.db)
				writer.decrease();
		}
	}

	public void visit(Call call) {
		if (_lim.db)
			writeln("Call: " + call.toString());

		if (call.getProcedure() != null) {
			if (_lim.db)
				writer.inc();

			call.getProcedure().accept(this);

			if (_lim.db)
				writer.decrease();
		}
	}

	public void visit(IPCoreCall call) {
	}

	public void visit(Procedure procedure) {
		if (_lim.db)
			writeln("Procedure: " + procedure.toString());
		if (_lim.db)
			writer.inc();

		procedure.getBody().accept(this);

		if (_lim.db)
			writer.decrease();
	}

	public void visit(Block block) {
		if (_lim.db)
			writeln("Block: " + block.toString());
		genericBlockVisit(block);
	}

	public void visit(HeapRead heapRead) {
		if (_lim.db)
			writeln("HeapRead: " + heapRead.toString());
		genericBlockVisit(heapRead);
	}

	public void visit(HeapWrite heapWrite) {
		if (_lim.db)
			writeln("HeapWrite: " + heapWrite.toString());
		genericBlockVisit(heapWrite);
	}

	public void visit(ArrayRead arrayRead) {
		if (_lim.db)
			writeln("Arrayread: " + arrayRead.toString());
		genericBlockVisit(arrayRead);
	}

	public void visit(ArrayWrite arrayWrite) {
		if (_lim.db)
			writeln("ArrayWrite: " + arrayWrite.toString());
		genericBlockVisit(arrayWrite);
	}

	public void visit(AbsoluteMemoryRead read) {
		if (_lim.db)
			writeln("AbsoluteMemoryRead: " + read.toString());
		genericBlockVisit(read);
	}

	public void visit(AbsoluteMemoryWrite write) {
		if (_lim.db)
			writeln("AbsoluteMemoryWrite: " + write.toString());
		genericBlockVisit(write);
	}

	private void genericBlockVisit(Block block) {
		Set otherComponents = new LinkedHashSet(block.getComponents());
		otherComponents.removeAll(block.getSequence());
		otherComponents.remove(block.getInBuf());
		otherComponents.removeAll(block.getOutBufs());

		block.getInBuf().accept(this);

		for (Iterator it = getIterator(block.getSequence()); it.hasNext();) {
			if (_lim.db)
				writer.inc();

			((Visitable) it.next()).accept(this);

			if (_lim.db)
				writer.decrease();
		}

		visit(otherComponents);
		visit(block.getOutBufs());
	}

	public void visit(Loop loop) {
		if (_lim.db)
			writeln("Loop: " + loop.toString());
		if (_lim.db)
			writer.inc();

		loop.getInBuf().accept(this);
		loop.getInitBlock().accept(this);
		if (loop.isIterative()) {
			loop.getBody().accept(this);
		}

		Collection components = new LinkedHashSet(loop.getComponents());

		components.remove(loop.getInBuf());
		components.removeAll(loop.getOutBufs());
		components.remove(loop.getInitBlock());
		if (loop.isIterative()) {
			components.remove(loop.getBody());
		}

		visit(components);
		visit(loop.getOutBufs());

		if (_lim.db)
			writer.dec();
	}

	public void visit(WhileBody whileBody) {
		if (_lim.db)
			writeln("While Body");

		whileBody.getInBuf().accept(this);
		Collection components = new LinkedHashSet(whileBody.getComponents());
		components.remove(whileBody.getInBuf());
		components.removeAll(whileBody.getOutBufs());
		visit(components);
		visit(whileBody.getOutBufs());
	}

	public void visit(UntilBody untilBody) {
		if (_lim.db)
			writeln("Until Body");

		untilBody.getInBuf().accept(this);
		Collection components = new LinkedHashSet(untilBody.getComponents());
		components.remove(untilBody.getInBuf());
		components.removeAll(untilBody.getOutBufs());
		visit(components);
		visit(untilBody.getOutBufs());
	}

	public void visit(ForBody forBody) {
		if (_lim.db)
			writeln("For Body");

		forBody.getInBuf().accept(this);
		Collection components = new LinkedHashSet(forBody.getComponents());
		components.remove(forBody.getInBuf());
		components.removeAll(forBody.getOutBufs());
		visit(components);
		visit(forBody.getOutBufs());
	}

	public void visit(Decision decision) {
		if (_lim.db)
			writeln("Decision: " + decision.toString());

		Set otherComponents = new LinkedHashSet(decision.getComponents());
		otherComponents.remove(decision.getTestBlock());
		otherComponents.remove(decision.getInBuf());
		otherComponents.removeAll(decision.getOutBufs());

		decision.getInBuf().accept(this);
		decision.getTestBlock().accept(this);

		visit(otherComponents);
		visit(decision.getOutBufs());
	}

	public void visit(Switch swich) {
		visit((Block) swich);
	}

	public void visit(Branch branch) {
		if (_lim.db)
			writeln("Branch: " + branch.toString());
		if (_lim.db)
			writer.inc();
		if (_lim.db)
			writer.inc();

		Set otherComponents = new LinkedHashSet(branch.getComponents());
		otherComponents.remove(branch.getInBuf());
		otherComponents.remove(branch.getDecision());
		otherComponents.remove(branch.getTrueBranch());
		otherComponents.remove(branch.getFalseBranch());
		otherComponents.removeAll(branch.getOutBufs());

		branch.getInBuf().accept(this);
		branch.getDecision().accept(this);

		if (_lim.db)
			writer.decrease();
		if (_lim.db)
			writeln("True: ");
		if (_lim.db)
			writer.inc();

		branch.getTrueBranch().accept(this);

		if (_lim.db)
			writer.decrease();
		if (_lim.db)
			writeln("False: ");
		if (_lim.db)
			writer.inc();

		if (branch.getFalseBranch() != null) {
			branch.getFalseBranch().accept(this);
		} else {
			if (_lim.db)
				writeln("null");
		}

		if (_lim.db)
			writer.decrease();
		if (_lim.db)
			writeln("Selector: ");
		if (_lim.db)
			writer.inc();

		if (_lim.db)
			writer.decrease();
		if (_lim.db)
			writeln("Others: ");
		if (_lim.db)
			writer.inc();

		visit(otherComponents);
		visit(branch.getOutBufs());

		if (_lim.db)
			writer.decrease();
		if (_lim.db)
			writer.decrease();
	}

	public void visit(AddOp add) {
		if (_lim.db)
			writeln("Operation: " + add.toString());
	}

	public void visit(AndOp and) {
		if (_lim.db)
			writeln("Operation: " + and.toString());
	}

	public void visit(NumericPromotionOp numericPromotion) {
		if (_lim.db)
			writeln("Operation: " + numericPromotion.toString());
	}

	public void visit(CastOp cast) {
		if (_lim.db)
			writeln("Operation: " + cast.toString());
	}

	public void visit(ComplementOp complement) {
		if (_lim.db)
			writeln("Operation: " + complement.toString());
	}

	public void visit(ConditionalAndOp conditionalAnd) {
		if (_lim.db)
			writeln("Operation: " + conditionalAnd.toString());
	}

	public void visit(ConditionalOrOp conditionalOr) {
		if (_lim.db)
			writeln("Operation: " + conditionalOr.toString());
	}

	public void visit(Constant constant) {
		if (_lim.db)
			writeln("Operation: " + constant.toString());
	}

	public void visit(DivideOp divide) {
		if (_lim.db)
			writeln("Operation: " + divide.toString());
	}

	public void visit(EqualsOp equals) {
		if (_lim.db)
			writeln("Operation: " + equals.toString());
	}

	public void visit(GreaterThanEqualToOp greaterThanEqualTo) {
		if (_lim.db)
			writeln("Operation: " + greaterThanEqualTo.toString());
	}

	public void visit(GreaterThanOp greaterThan) {
		if (_lim.db)
			writeln("Operation: " + greaterThan.toString());
	}

	public void visit(LeftShiftOp leftShift) {
		if (_lim.db)
			writeln("Operation: " + leftShift.toString());
	}

	public void visit(LessThanEqualToOp lessThanEqualTo) {
		if (_lim.db)
			writeln("Operation: " + lessThanEqualTo.toString());
	}

	public void visit(LessThanOp lessThan) {
		if (_lim.db)
			writeln("Operation: " + lessThan.toString());
	}

	public void visit(LocationConstant loc) {
		if (_lim.db)
			writeln("Location Constant Operation: " + loc.toString());
		visit((Constant) loc);
	}

	public void visit(MinusOp minus) {
		if (_lim.db)
			writeln("Operation: " + minus.toString());
	}

	public void visit(ModuloOp modulo) {
		if (_lim.db)
			writeln("Operation: " + modulo.toString());
	}

	public void visit(MultiplyOp multiply) {
		if (_lim.db)
			writeln("Operation: " + multiply.toString());
	}

	public void visit(NotEqualsOp notEquals) {
		if (_lim.db)
			writeln("Operation: " + notEquals.toString());
	}

	public void visit(NotOp not) {
		if (_lim.db)
			writeln("Operation: " + not.toString());
	}

	public void visit(OrOp or) {
		if (_lim.db)
			writeln("Operation: " + or.toString());
	}

	public void visit(PlusOp plus) {
		if (_lim.db)
			writeln("Operation: " + plus.toString());
	}

	public void visit(ReductionOrOp reductionOr) {
		if (_lim.db)
			writeln("Operation: " + reductionOr.toString());
	}

	public void visit(RightShiftOp rightShift) {
		if (_lim.db)
			writeln("Operation: " + rightShift.toString());
	}

	public void visit(RightShiftUnsignedOp rightShiftUnsigned) {
		if (_lim.db)
			writeln("Operation: " + rightShiftUnsigned.toString());
	}

	public void visit(ShortcutIfElseOp shortcutIfElse) {
		if (_lim.db)
			writeln("Operation: " + shortcutIfElse.toString());
	}

	public void visit(SubtractOp subtract) {
		if (_lim.db)
			writeln("Operation: " + subtract.toString());
	}

	public void visit(XorOp xor) {
		if (_lim.db)
			writeln("Operation: " + xor.toString());
	}

	public void visit(InBuf ib) {
		if (_lim.db)
			writeln("InBuf: " + ib);
	}

	public void visit(OutBuf ob) {
		if (_lim.db)
			writeln("OutBuf: " + ob);
	}

	public void visit(Reg reg) {
		if (_lim.db)
			writeln("Reg: " + reg);
	}

	public void visit(SRL16 srl16) {
		if (_lim.db)
			writeln("SRL16: " + srl16);
	}

	public void visit(Mux m) {
		if (_lim.db)
			writeln("Mux: " + m);
	}

	public void visit(EncodedMux m) {
		if (_lim.db)
			writeln("EnabledMux: " + m);
	}

	public void visit(PriorityMux pmux) {
		if (_lim.db)
			writeln("PriorityMux: " + pmux);
		if (_lim.db)
			writer.inc();

		pmux.getInBuf().accept(this);

		Collection components = new LinkedList(pmux.getComponents());
		components.remove(pmux.getInBuf());
		components.removeAll(pmux.getOutBufs());

		// for (Iterator it = components.iterator(); it.hasNext();)
		for (Iterator it = getIterator(components); it.hasNext();) {
			if (_lim.db)
				writer.inc();

			((Visitable) it.next()).accept(this);

			if (_lim.db)
				writer.decrease();
		}

		visit(pmux.getOutBufs());

		if (_lim.db)
			writer.dec();
	}

	public void visit(And a) {
		if (_lim.db)
			writeln("And: " + a);
	}

	public void visit(Not n) {
		if (_lim.db)
			writeln("Not: " + n);
	}

	public void visit(Or o) {
		if (_lim.db)
			writeln("Or: " + o);
	}

	public void visit(TriBuf tbuf) {
		if (_lim.db)
			writeln("TriBuf: " + tbuf);
	}

	public void visit(Scoreboard scoreboard) {
		if (_lim.db)
			writeln("Scoreboard: " + scoreboard);
		if (traverseComposable) {
			scoreboard.getInBuf().accept(this);
			List components = new LinkedList(scoreboard.getComponents());
			components.remove(scoreboard.getInBuf());
			components.remove(scoreboard.getOutBufs());
			for (Iterator it = getIterator(components); it.hasNext();) {
				if (_lim.db)
					writer.inc();

				((Visitable) it.next()).accept(this);

				if (_lim.db)
					writer.decrease();
			}

			visit(scoreboard.getOutBufs());
		}
	}

	public void visit(Latch latch) {
		if (_lim.db)
			writeln("Latch: " + latch);
		if (traverseComposable) {
			latch.getInBuf().accept(this);
			List components = new LinkedList(latch.getComponents());
			components.remove(latch.getInBuf());
			components.remove(latch.getOutBufs());
			for (Iterator it = getIterator(components); it.hasNext();) {
				if (_lim.db)
					writer.inc();

				((Visitable) it.next()).accept(this);

				if (_lim.db)
					writer.decrease();
			}

			visit(latch.getOutBufs());
		}
	}

	public void visit(Kicker kicker) {
		if (_lim.db)
			writeln("Kicker: " + kicker);
		if (traverseComposable) {
			kicker.getInBuf().accept(this);
			List components = new LinkedList(kicker.getComponents());
			components.remove(kicker.getInBuf());
			components.remove(kicker.getOutBufs());
			for (Iterator it = getIterator(components); it.hasNext();) {
				if (_lim.db)
					writer.inc();

				((Visitable) it.next()).accept(this);

				if (_lim.db)
					writer.decrease();
			}

			visit(kicker.getOutBufs());
		}
	}

	public void visit(NoOp nop) {
		if (_lim.db)
			writeln("NoOp: " + nop);
	}

	public void visit(TimingOp timingOp) {
		if (_lim.db)
			writeln("TimingOp: " + timingOp);
	}

	public void visit(RegisterRead regRead) {
		if (_lim.db)
			writeln("RegisterRead: " + regRead);
	}

	public void visit(RegisterWrite regWrite) {
		if (_lim.db)
			writeln("RegisterWrite: " + regWrite);
	}

	public void visit(RegisterGateway gw) {
		if (_lim.db)
			writeln("RegisterGateway: " + gw.toString());

		Set otherComponents = new LinkedHashSet(gw.getComponents());
		otherComponents.remove(gw.getInBuf());
		otherComponents.removeAll(gw.getOutBufs());

		gw.getInBuf().accept(this);

		visit(otherComponents);
		visit(gw.getOutBufs());
	}

	public void visit(RegisterReferee regReferee) {
		if (_lim.db)
			writeln("RegisterReferee: " + regReferee);

		if (_lim.db)
			writer.inc();
		Collection comps = new LinkedHashSet(regReferee.getComponents());
		comps.remove(regReferee.getInBuf());
		comps.removeAll(regReferee.getOutBufs());

		regReferee.getInBuf().accept(this);

		visit(comps);

		visit(regReferee.getOutBufs());

		// regReferee.getInBuf().accept(this);

		// regReferee.getEnabledReg().accept(this);

		// PriorityMux pmux = regReferee.getPriorityMux();

		// if (pmux != null) pmux.accept(this);

		// visit(regReferee.getOutBufs());

		if (_lim.db)
			writer.dec();
	}

	public void visit(MemoryReferee memReferee) {
		if (_lim.db)
			writeln("MemoryReferee: " + memReferee);

		if (_lim.db)
			writer.inc();

		Collection comps = new LinkedHashSet(memReferee.getComponents());
		comps.remove(memReferee.getInBuf());
		comps.removeAll(memReferee.getOutBufs());

		memReferee.getInBuf().accept(this);

		visit(comps);
		// regReferee.getEnabledReg().accept(this);

		// regReferee.getPriorityMux().accept(this);

		visit(memReferee.getOutBufs());

		if (_lim.db)
			writer.dec();
	}

	public void visit(MemoryBank memBank) {
		if (_lim.db)
			writeln("MemoryBank: " + memBank);
	}

	/**
	 * Extends the visitation by explicitly visiting any
	 * {@link MemoryRead#Physical} if defined for this component.
	 */
	public void visit(MemoryRead memRead) {
		if (_lim.db)
			writeln("MemoryRead: " + memRead);

		if (memRead.getPhysicalComponent() != null) {
			visitGenericModule(memRead.getPhysicalComponent());
		}
	}

	/**
	 * Extends the visitation by explicitly visiting any
	 * {@link MemoryWrite#Physical} if defined for this component.
	 */
	public void visit(MemoryWrite memWrite) {
		if (_lim.db)
			writeln("Memorywrite: " + memWrite);

		if (memWrite.getPhysicalComponent() != null) {
			visitGenericModule(memWrite.getPhysicalComponent());
		}
	}

	public void visit(MemoryGateway mg) {
		if (_lim.db)
			writeln("MemoryGateway: " + mg.toString());

		Set otherComponents = new LinkedHashSet(mg.getComponents());
		otherComponents.remove(mg.getInBuf());
		otherComponents.removeAll(mg.getOutBufs());

		mg.getInBuf().accept(this);

		visit(otherComponents);
		visit(mg.getOutBufs());
	}

	public void visit(PinRead pinRead) {
		if (_lim.db)
			writeln("PinRead: " + pinRead);

		if (pinRead.getPhysicalComponent() != null) {
			visitGenericModule(pinRead.getPhysicalComponent());
		}
	}

	/**
	 * Extends the visitation by explicitly visiting any
	 * {@link PinWrite.Physical} if defined for this component.
	 */
	public void visit(PinWrite pinWrite) {
		if (_lim.db)
			writeln("PinWrite: " + pinWrite);

		if (pinWrite.getPhysicalComponent() != null) {
			visitGenericModule(pinWrite.getPhysicalComponent());
		}
	}

	/**
	 * Extends the visitation by explicitly visiting any
	 * {@link PinStateChange.Physical} if defined for this component.
	 */
	public void visit(PinStateChange pinChange) {
		if (_lim.db)
			writeln("PinStateChange: " + pinChange);

		if (pinChange.getPhysicalComponent() != null) {
			visitGenericModule(pinChange.getPhysicalComponent());
		}
	}

	public void visit(PinReferee pinReferee) {
		if (_lim.db)
			writeln("PinReferee: " + pinReferee);

		if (_lim.db)
			writer.inc();

		Collection comps = new LinkedHashSet(pinReferee.getComponents());
		comps.remove(pinReferee.getInBuf());
		comps.removeAll(pinReferee.getOutBufs());

		pinReferee.getInBuf().accept(this);

		visit(comps);

		visit(pinReferee.getOutBufs());

		if (_lim.db)
			writer.dec();
	}

	public void visit(SimplePin comp) {
		if (_lim.db)
			writeln("SimplePin: " + comp);
	}

	public void visit(TaskCall mod) {
		if (_lim.db)
			writeln("TaskCall: " + mod);
		visitGenericModule(mod);
	}

	public void visit(SimplePinAccess mod) {
		if (_lim.db)
			writeln("SimplePinAccess: " + mod);
		visitGenericModule(mod);
	}

	public void visit(SimplePinRead comp) {
		if (_lim.db)
			writeln("SimplePinRead: " + comp);
	}

	public void visit(SimplePinWrite comp) {
		if (_lim.db)
			writeln("SimplePinWrite: " + comp);
	}

	public void visit(FifoAccess mod) {
		if (_lim.db)
			writeln("FifoAccess: " + mod);
		visitGenericModule(mod);
	}

	public void visit(FifoRead mod) {
		if (_lim.db)
			writeln("FifoRead: " + mod);
		visitGenericModule(mod);
	}

	public void visit(FifoWrite mod) {
		if (_lim.db)
			writeln("FifoWrite: " + mod);
		visitGenericModule(mod);
	}

	public void visit(EndianSwapper mod) {
		if (_lim.db)
			writeln("EndianSwapper: " + mod);
		visitGenericModule(mod);
	}

	public IndentWriter getWriter() {
		return this.writer;
	}

	public OutputStream getOutputStream() {
		return this.stream;
	}

	public void writeln(String s) {
		writer.write(s);
		writer.println();
		writer.flush();
	}

	protected void visitGenericModule(Module m) {
		Collection comps = new LinkedHashSet(m.getComponents());
		comps.remove(m.getInBuf());
		comps.removeAll(m.getOutBufs());
		m.getInBuf().accept(this);
		visit(comps);
		visit(m.getOutBufs());
	}

	private void visit(Collection components) {
		for (Iterator iter = getIterator(components); iter.hasNext();) {
			((Visitable) iter.next()).accept(this);
		}
	}

	protected Iterator getIterator(Collection collection) {
		return collection.iterator();
	}

}// DefaultVisitor
