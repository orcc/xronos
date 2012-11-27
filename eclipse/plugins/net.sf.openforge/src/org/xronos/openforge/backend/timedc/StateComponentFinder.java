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

package org.xronos.openforge.backend.timedc;

import java.io.File;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.xronos.openforge.app.EngineThread;
import org.xronos.openforge.app.ForgeFileTyper;
import org.xronos.openforge.lim.Component;
import org.xronos.openforge.lim.DefaultVisitor;
import org.xronos.openforge.lim.Design;
import org.xronos.openforge.lim.GlobalReset;
import org.xronos.openforge.lim.Referenceable;
import org.xronos.openforge.lim.Register;
import org.xronos.openforge.lim.RegisterWrite;
import org.xronos.openforge.lim.UnexpectedVisitationException;
import org.xronos.openforge.lim.Visitable;
import org.xronos.openforge.lim.memory.MemoryRead;
import org.xronos.openforge.lim.memory.MemoryWrite;
import org.xronos.openforge.lim.primitive.Reg;
import org.xronos.openforge.lim.primitive.SRL16;


/**
 * This class is responsible for finding all the nodes in the LIM which maintain
 * any sort of state. Effectively this is all nodes whose latency is greater
 * than 0. A unique OpHandle subclass is created for each of these nodes which
 * knows how to handle declaration and updating of state.
 */
class StateComponentFinder extends DefaultVisitor {
	private CNameCache nameCache;
	private Map<Referenceable, StateVar> referenceableMap;

	/**
	 * A Map of component to StateVar.
	 */
	private Map<Component, StateVar> seqElements = new LinkedHashMap<Component, StateVar>();

	StateComponentFinder(CNameCache cache, Map<Referenceable, StateVar> refMap) {
		super();
		setTraverseComposable(true);
		nameCache = cache;
		referenceableMap = refMap;
	}

	Map<Component, StateVar> getSeqElements() {
		return Collections.unmodifiableMap(seqElements);
	}

	@Override
	public void visit(Design design) {
		// The design module will contain the kickers. No need to
		// visit them individually
		for (GlobalReset grst : design.getResetPins()) {
			final File[] inputFiles = EngineThread.getGenericJob()
					.getTargetFiles();
			int delay = 5;
			if (ForgeFileTyper.isXLIMSource(inputFiles[0].getName()))
				delay = 10;
			seqElements.put(grst, new ResetVar(grst, nameCache, delay));
		}
		for (Component comp : design.getDesignModule().getComponents()) {
			try {
				((Visitable) comp).accept(this);
			}
			// Anything that throws a UVE is not going to factor
			// into the c translation anyway.
			catch (UnexpectedVisitationException uve) {
			}
		}
		// No need to call super if we visit everything in
		// designModule and we do not need to process the Task objects.
		// super.visit(design);
	}

	@Override
	public void visit(Reg comp) {
		super.visit(comp);
		seqElements.put(comp, new RegVar(comp, nameCache));
	}

	@Override
	public void visit(MemoryRead comp) {
		super.visit(comp);
		MemoryVar memVar = (MemoryVar) referenceableMap.get(comp
				.getMemoryPort().getLogicalMemory());
		seqElements.put(comp, new MemAccessVar(comp, memVar, nameCache));
	}

	@Override
	public void visit(MemoryWrite comp) {
		super.visit(comp);
		MemoryVar memVar = (MemoryVar) referenceableMap.get(comp
				.getMemoryPort().getLogicalMemory());
		seqElements.put(comp, new MemAccessVar(comp, memVar, nameCache));
	}

	// No need for a stateful var for register reads.... they are
	// just a direct wire from the register.
	// public void visit (RegisterRead comp){}

	@Override
	public void visit(RegisterWrite comp) {
		Register target = (Register) comp.getReferenceable();
		assert target != null : "null target of " + comp;
		RegisterVar registerVar = (RegisterVar) referenceableMap.get(target);
		if (registerVar == null) {
			registerVar = new RegisterVar(target);
			referenceableMap.put(target, registerVar);
		}
		RegWriteVar writeVar = new RegWriteVar(comp, registerVar, nameCache);
		seqElements.put(comp, writeVar);
	}

	@Override
	public void visit(SRL16 comp) {
		super.visit(comp);
		seqElements.put(comp, new SRL16Var(comp, nameCache));
	}

	/*
	 * public void visit (Block vis) { super.visit(vis); filterAny(vis); }
	 * public void visit (Loop vis) { super.visit(vis); filterAny(vis); } public
	 * void visit (AddOp vis) { super.visit(vis); filterAny(vis); } public void
	 * visit (AndOp vis) { super.visit(vis); filterAny(vis); } public void visit
	 * (CastOp vis) { super.visit(vis); filterAny(vis); } public void visit
	 * (ComplementOp vis) { super.visit(vis); filterAny(vis); } public void
	 * visit (ConditionalAndOp vis) { super.visit(vis); filterAny(vis); } public
	 * void visit (ConditionalOrOp vis) { super.visit(vis); filterAny(vis); }
	 * public void visit (Constant vis) { super.visit(vis); filterAny(vis); }
	 * public void visit (DivideOp vis) { super.visit(vis); filterAny(vis); }
	 * public void visit (EqualsOp vis) { super.visit(vis); filterAny(vis); }
	 * public void visit (GreaterThanEqualToOp vis) { super.visit(vis);
	 * filterAny(vis); } public void visit (GreaterThanOp vis) {
	 * super.visit(vis); filterAny(vis); } public void visit (LeftShiftOp vis) {
	 * super.visit(vis); filterAny(vis); } public void visit (LessThanEqualToOp
	 * vis) { super.visit(vis); filterAny(vis); } public void visit (LessThanOp
	 * vis) { super.visit(vis); filterAny(vis); } public void visit
	 * (LocationConstant vis) { super.visit(vis); filterAny(vis); } public void
	 * visit (MinusOp vis) { super.visit(vis); filterAny(vis); } public void
	 * visit (ModuloOp vis) { super.visit(vis); filterAny(vis); } public void
	 * visit (MultiplyOp vis) { super.visit(vis); filterAny(vis); } public void
	 * visit (NotEqualsOp vis) { super.visit(vis); filterAny(vis); } public void
	 * visit (NotOp vis) { super.visit(vis); filterAny(vis); } public void visit
	 * (OrOp vis) { super.visit(vis); filterAny(vis); } public void visit
	 * (PlusOp vis) { super.visit(vis); filterAny(vis); } public void visit
	 * (ReductionOrOp vis) { super.visit(vis); filterAny(vis); } public void
	 * visit (RightShiftOp vis) { super.visit(vis); filterAny(vis); } public
	 * void visit (RightShiftUnsignedOp vis) { super.visit(vis); filterAny(vis);
	 * } public void visit (ShortcutIfElseOp vis) { super.visit(vis);
	 * filterAny(vis); } public void visit (SubtractOp vis) { super.visit(vis);
	 * filterAny(vis); } public void visit (NumericPromotionOp vis) {
	 * super.visit(vis); filterAny(vis); } public void visit (XorOp vis) {
	 * super.visit(vis); filterAny(vis); } public void visit (Branch vis) {
	 * super.visit(vis); filterAny(vis); } public void visit (Switch vis) {
	 * super.visit(vis); filterAny(vis); } public void visit (InBuf vis) {
	 * super.visit(vis); filterAny(vis); } public void visit (OutBuf vis) {
	 * super.visit(vis); filterAny(vis); } public void visit (Mux vis) {
	 * super.visit(vis); filterAny(vis); } public void visit (EncodedMux vis) {
	 * super.visit(vis); filterAny(vis); } public void visit (PriorityMux vis) {
	 * super.visit(vis); filterAny(vis); } public void visit (And vis) {
	 * super.visit(vis); filterAny(vis); } public void visit (Not vis) {
	 * super.visit(vis); filterAny(vis); } public void visit (Or vis) {
	 * super.visit(vis); filterAny(vis); } public void visit (Scoreboard vis) {
	 * super.visit(vis); filterAny(vis); } public void visit (Latch vis) {
	 * super.visit(vis); filterAny(vis); } public void visit (NoOp vis) {
	 * super.visit(vis); filterAny(vis); } public void visit (RegisterRead vis)
	 * { super.visit(vis); filterAny(vis); } public void visit (HeapRead vis) {
	 * super.visit(vis); filterAny(vis); } public void visit (ArrayRead vis) {
	 * super.visit(vis); filterAny(vis); } public void visit (HeapWrite vis) {
	 * super.visit(vis); filterAny(vis); } public void visit (ArrayWrite vis) {
	 * super.visit(vis); filterAny(vis); } public void visit (AbsoluteMemoryRead
	 * vis) { super.visit(vis); filterAny(vis); } public void visit
	 * (AbsoluteMemoryWrite vis) { super.visit(vis); filterAny(vis); } public
	 * void visit (Kicker vis) { super.visit(vis); filterAny(vis); }
	 * 
	 * public void visit (TaskCall vis) { super.visit(vis); filterAny(vis); }
	 * public void visit (SimplePinAccess vis) { super.visit(vis);
	 * filterAny(vis); } public void visit (SimplePin vis) { super.visit(vis);
	 * filterAny(vis); } public void visit (SimplePinRead vis) {
	 * super.visit(vis); filterAny(vis); } public void visit (SimplePinWrite
	 * vis) { super.visit(vis); filterAny(vis); } public void visit (FifoAccess
	 * vis) { super.visit(vis); filterAny(vis); } public void visit (FifoRead
	 * vis) { super.visit(vis); filterAny(vis); } public void visit (FifoWrite
	 * vis) { super.visit(vis); filterAny(vis); } //public void visit (WhileBody
	 * vis) { super.visit(vis); filterAny(vis); } //public void visit (UntilBody
	 * vis) { super.visit(vis); filterAny(vis); } //public void visit (ForBody
	 * vis) { super.visit(vis); filterAny(vis); } //public void visit (Decision
	 * vis) { super.visit(vis); filterAny(vis); } //public void visit (TimingOp
	 * vis) { super.visit(vis); filterAny(vis); } //public void visit
	 * (RegisterGateway vis) { super.visit(vis); filterAny(vis); } //public void
	 * visit (RegisterReferee vis) { super.visit(vis); filterAny(vis); }
	 * //public void visit (MemoryReferee vis) { super.visit(vis);
	 * filterAny(vis); } //public void visit (MemoryGateway vis) {
	 * super.visit(vis); filterAny(vis); } //public void visit (MemoryBank vis)
	 * { super.visit(vis); filterAny(vis); } //public void visit (PinRead vis) {
	 * super.visit(vis); filterAny(vis); } //public void visit (PinWrite vis) {
	 * super.visit(vis); filterAny(vis); } //public void visit (PinStateChange
	 * vis) { super.visit(vis); filterAny(vis); } //public void visit
	 * (PinReferee vis) { super.visit(vis); filterAny(vis); } //public void
	 * visit (TriBuf vis) { super.visit(vis); filterAny(vis); } //public void
	 * visit (EndianSwapper vis) { super.visit(vis); filterAny(vis); }
	 * 
	 * 
	 * public void filterAny (Component comp) { if
	 * (!this.seqElements.containsKey(comp)) { this.seqElements.put(comp, new
	 * GenericStateVarOpHandle(comp, this.nameCache)); } } private class
	 * GenericStateVarOpHandle extends OpHandle implements StateVar { private
	 * List<Bus> buses; private GenericStateVarOpHandle (Component comp,
	 * CNameCache cache) { super(comp, cache); buses = new
	 * ArrayList(comp.getBuses()); }
	 * 
	 * public void declareGlobal (PrintStream ps) { for (Bus bus : this.buses) {
	 * ps.println(declare(bus)); } }
	 * 
	 * public void writeTick (PrintStream ps) { ; } }
	 */
}
