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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.sf.openforge.app.GenericJob;
import net.sf.openforge.app.OptionRegistry;
import net.sf.openforge.app.project.Option;
import net.sf.openforge.app.project.OptionBoolean;
import net.sf.openforge.app.project.OptionInt;
import net.sf.openforge.lim.memory.AbsoluteMemoryRead;
import net.sf.openforge.lim.memory.AbsoluteMemoryWrite;
import net.sf.openforge.lim.memory.LocationConstant;
import net.sf.openforge.util.naming.IDSourceInfo;

/**
 * Loop is a generic representation of a loop control structure. Its purpose is
 * to iteratively execute the contents of a {@link LoopBody}. Internally, the
 * Loop also creates data {@link Reg Registers} and {@link Latch Latches} to
 * manage feedback data.
 * 
 * @author Stephen Edwards
 * @version $Id: Loop.java 280 2006-08-11 17:00:32Z imiller $
 */
public class Loop extends Module {

	/** An unknown number of iterations */
	public static final int ITERATIONS_UNKNOWN = -1;

	/** Initialization of the loop */
	private Block initBlock;

	/** The body of the loop */
	private LoopBody body;

	/** Initial entry into the body */
	private Entry bodyInitEntry = null;

	/** Feedback entry into the body */
	private Entry bodyFeedbackEntry = null;

	/**
	 * Collection of Reg objects used to delay feedback data values from the
	 * LoopBody
	 */
	private List dataRegisters = new LinkedList();

	/**
	 * Collection of Latch objects used to hold data values used in the LoopBody
	 */
	private List dataLatches = new LinkedList();

	/** Flop for control feedback */
	private Reg controlRegister = null;

	/** set to false if the loop is completely unrolled */
	private final boolean isIterative = true;

	/** The number of iterations, if known */
	private int iterations = ITERATIONS_UNKNOWN;

	/**
	 * When set to true, this forces the isLoopUnrollingEnabled method to return
	 * true regardless of the preference setting.
	 */
	private boolean forceLoopUnrolling = false;

	/**
	 * Constructs a Loop and establishes the control dependencies between the
	 * various components of the loop.
	 * 
	 * @param body
	 *            the body of the loop
	 */
	public Loop(Block initBlock, LoopBody body) {
		super();

		/*
		 * By default, the Loop Exit has no data buses.
		 */
		if (_lim.db) {
			_lim.ln(_lim.LOOPS, "Making an exit on Loop");
		}
		makeExit(0);
		addComponent(this.initBlock = initBlock);
		addComponent(this.body = body);
		// Needs RESET b/c it is in the control path
		addComponent(controlRegister = Reg.getConfigurableReg(Reg.REGR,
				"loopControl"));
		setControlDependencies();
	}

	/**
	 * Constructs a new Loop that has no initialization block.
	 * 
	 * @param body
	 *            the body of the loop
	 */
	public Loop(LoopBody body) {
		this(new Block(Collections.<Component> emptyList()), body);
	}

	@Override
	public void accept(Visitor vis) {
		vis.visit(this);
	}

	/**
	 * Gets the initializer block.
	 */
	public Block getInitBlock() {
		return initBlock;
	}

	/**
	 * Gets the loop body. May be null if this loop has been unrooled.
	 */
	public LoopBody getBody() {
		return body;
	}

	@Override
	public boolean replaceComponent(Component removed, Component inserted) {
		assert removed != null;

		if (removed == getInitBlock()) {
			initBlock = (Block) inserted;
		} else if (removed == getBody()) {
			body = (LoopBody) inserted;
		} else if (removed == getControlRegister()) {
			controlRegister = (Reg) inserted;
		} else if (getDataLatches().contains(removed)) {
			int index = dataLatches.indexOf(removed);
			dataLatches.add(index, inserted);
			dataLatches.remove(removed);
		} else if (getDataRegisters().contains(removed)) {
			int index = dataRegisters.indexOf(removed);
			dataRegisters.add(index, inserted);
			dataRegisters.remove(removed);
		} else {
			throw new IllegalArgumentException(
					"Cannot replace unknown component in " + getClass());
		}

		boolean mod = removeComponent(removed);
		addComponent(inserted);

		return mod;
	}

	/**
	 * Gets the {@link Entry} by which the {@link LoopBody} is entered for to
	 * the first iteration.
	 */
	public Entry getBodyInitEntry() {
		return bodyInitEntry;
	}

	/**
	 * Gets the {@link Entry} by which the {@link LoopBody} is entered for all
	 * feedback iterations.
	 */
	public Entry getBodyFeedbackEntry() {
		return bodyFeedbackEntry;
	}

	/**
	 * Gets the register that delays the control feedback signal.
	 */
	public Reg getControlRegister() {
		return controlRegister;
	}

	/**
	 * Gets the data registers that were created to delay data feedback values.
	 * 
	 * @return a collection of enabled Reg objects
	 */
	public Collection<Reg> getDataRegisters() {
		return dataRegisters;
	}

	/**
	 * Returns a Set of {@link Component Components} that represent the points
	 * in this Loop that are feedback points which are the data and control
	 * registers.
	 * 
	 * @return a Set containing the data registers and control register if
	 *         non-null
	 */
	@Override
	public Set<Component> getFeedbackPoints() {
		Set<Component> feedback = new HashSet<Component>();
		feedback.addAll(super.getFeedbackPoints());
		feedback.addAll(getDataRegisters());
		if (getControlRegister() != null) {
			feedback.add(getControlRegister());
		}

		return feedback;
	}

	public Collection getDataLatches() {
		return dataLatches;
	}

	/**
	 * Creates and returns a new data Reg, which is also added to this Loop as a
	 * component. It is assumed this Reg will be used to delay a feedback value
	 * from the LoopBody.
	 * 
	 * @return the new Reg
	 */
	public Reg createDataRegister() {
		// No reset needed b/c it is only in the data path and has no
		// reset value
		final Reg reg = Reg.getConfigurableReg(Reg.REGE, "loopData");
		dataRegisters.add(reg);
		addComponent(reg);
		return reg;
	}

	/**
	 * Creates and returns a new data Latch, which is also added to this Loop as
	 * a component. It is assumed this Latch will be used to hold a value that
	 * is used within the LoopBody.
	 * 
	 * @return the new latch
	 */
	public Latch createDataLatch() {
		final Latch latch = new Latch();
		dataLatches.add(latch);
		addComponent(latch);
		return latch;
	}

	@Override
	public boolean removeComponent(Component component) {
		// disconnect exits
		for (Exit exit : component.getExits()) {
			for (Entry entry : new ArrayList<Entry>(exit.getDrivenEntries())) {
				entry.setDrivingExit(null);
			}
		}

		component.disconnect();
		if (component == initBlock) {
			initBlock = null;
		} else if (component == body) {
			body = null;
		} else if (component == controlRegister) {
			controlRegister = null;
		} else if (dataRegisters.contains(component)) {
			dataRegisters.remove(component);
		} else if (dataLatches.contains(component)) {
			dataLatches.remove(component);
		}
		return super.removeComponent(component);
	}

	/**
	 * need to set up dependencies - setControlDependencies() is not re-entrant
	 */
	public boolean replaceInitBlock(Block init) {
		if (!removeComponent(initBlock)) {
			return false;
		}
		initBlock = init;
		addComponent(init);
		return true;
	}

	/**
	 * Tests whether or not the timing of this component can be balanced during
	 * scheduling. That is, can all of the execution paths through the component
	 * be made to complete in the same number of clocks. Note that this property
	 * is based only upon the type of this component and any components that it
	 * may contain.
	 * 
	 * @return true if the loop is bounded and will iterate for a fixed number
	 *         of clocks. False if not bounded, contains a non-balanceable
	 *         component, orcontains a break, continue, or return.
	 */
	@Override
	public boolean isBalanceable() {
		boolean containsContinue = false;
		HashSet exits = new HashSet();
		if (getBody() != null && getBody().getBody() != null) {
			exits.addAll(getBody().getBody().getExits());
			exits.remove(getBody().getBody().getExit(Exit.DONE));

			for (Iterator iter = getBody().getBody().getComponents().iterator(); iter
					.hasNext();) {
				Component comp = (Component) iter.next();
				containsContinue |= (comp.getExit(Exit.CONTINUE) != null);
			}
		}
		// Loop is not balanceable if its body has more than 1 exit
		// point (ie contains a return or break). Also not
		// balanceable if it contiains a continue since that will/may
		// lead to bounded latencies on data produced in the loop.
		if (exits.size() != 0) {
			balanceInfo("Loop contains break or return.  Task not balanceable");
			return false;
		}

		if (containsContinue) {
			balanceInfo("Loop contains continue.  Task not balanceable");
			return false;
		}

		// The data latches should not be considered as precluding
		// balancing since they are 'inside' the loop structure and we
		// aren't balancing inside the loop structure. Actually any
		// latches shouldn't preclude balancing since they are all
		// added by scheduling when we are not balancing, but the only
		// place they exist, pre-scheduling is in a loop, so we are
		// covered.
		Set<Component> components = new HashSet<Component>(getComponents());
		components.removeAll(getDataLatches());
		for (Component comp : components) {
			if (!comp.isBalanceable()) {
				return false;
			}
		}

		if (isIterative() && (getIterations() != ITERATIONS_UNKNOWN)) {
			balanceInfo("Loop is not bounded.  Task not balanceable");
			return false;
		}

		return true;
	}

	private void balanceInfo(String msg) {
		GenericJob gj = getGenericJob();
		if (gj.getUnscopedBooleanOptionValue(OptionRegistry.SCHEDULE_BALANCE)) {
			gj.inc();
			gj.info(msg);
			gj.inc();
			IDSourceInfo info = getIDSourceInfo();
			gj.verbose("Loop location:" + " line: " + info.getSourceLine()
					+ " of " + info.getSourceFileName());
			gj.dec();
			gj.dec();
		}
	}

	protected void setControlDependencies() {
		final Bus clockBus = getInBuf().getClockBus();
		final Bus resetBus = getInBuf().getResetBus();
		final Bus goBus = getInBuf().getGoBus();
		final Exit startExit = getInBuf().getExit(Exit.DONE);

		/*
		 * InBuf to init Block.
		 */
		final Block initBlock = getInitBlock();
		final Entry initBlockEntry = initBlock.makeEntry(startExit);
		addDependencies(initBlockEntry, clockBus, resetBus, goBus);
		final Exit initBlockExit = initBlock.getExit(Exit.DONE);

		/*
		 * Init Block to LoopBody.
		 */
		final LoopBody body = getBody();
		bodyInitEntry = body.makeEntry(initBlockExit);
		addDependencies(bodyInitEntry, clockBus, resetBus,
				initBlockExit.getDoneBus());

		/*
		 * If there's a feedback exit, create a path from it through the control
		 * register back around to the body input.
		 */
		if (body.getFeedbackExit() != null) {
			final Reg controlReg = getControlRegister();

			/*
			 * LoopBody feedback to control Reg.
			 */
			final Entry controlRegEntry = controlReg.makeEntry(body
					.getFeedbackExit());
			controlRegEntry.addDependency(controlReg.getClockPort(),
					new ClockDependency(clockBus));
			controlRegEntry.addDependency(controlReg.getResetPort(),
					new ResetDependency(resetBus));
			controlRegEntry.addDependency(controlReg.getInternalResetPort(),
					new ResetDependency(resetBus));
			controlRegEntry.addDependency(controlReg.getDataPort(),
					new DataDependency(body.getFeedbackExit().getDoneBus()));

			/*
			 * Feedback control Reg to LoopBody.
			 */
			bodyFeedbackEntry = body.makeEntry(controlReg.getExit(Exit.DONE));
			addDependencies(bodyFeedbackEntry, clockBus, resetBus,
					controlReg.getResultBus());
		} else {
			removeComponent(getControlRegister());
		}

		/*
		 * LoopBody exit to Loop exit.
		 */
		final Exit loopExit = getExit(Exit.DONE);
		final OutBuf outBuf = loopExit.getPeer();
		final Entry outbufEntry = outBuf.makeEntry(body.getLoopCompleteExit());
		addDependencies(outbufEntry, clockBus, resetBus, body
				.getLoopCompleteExit().getDoneBus());

		/*
		 * Abnormal Exits. Assume that unlabeled break/continue Exits are merged
		 * inside the LoopBody with its regular Exits, and there are no labeled
		 * breaks/continues.
		 */
		final Map exitMap = new LinkedHashMap(11);
		collectExits(initBlock, exitMap);
		collectExits(body, exitMap);
		exitMap.remove(Exit.getTag(Exit.DONE));
		exitMap.remove(LoopBody.FEEDBACK_TAG);
		mergeExits(exitMap, clockBus, resetBus);
	}

	@Override
	protected void cloneNotify(Module moduleClone, Map cloneMap) {
		super.cloneNotify(moduleClone, cloneMap);
		final Loop clone = (Loop) moduleClone;
		clone.initBlock = (Block) cloneMap.get(initBlock);
		clone.body = (LoopBody) cloneMap.get(body);
		clone.bodyInitEntry = getEntryClone(bodyInitEntry, cloneMap);
		clone.bodyFeedbackEntry = getEntryClone(bodyFeedbackEntry, cloneMap);
		clone.dataRegisters = new LinkedList();
		for (Iterator iter = dataRegisters.iterator(); iter.hasNext();) {
			clone.dataRegisters.add(cloneMap.get(iter.next()));
		}
		clone.dataLatches = new LinkedList();
		for (Iterator iter = dataLatches.iterator(); iter.hasNext();) {
			clone.dataLatches.add(cloneMap.get(iter.next()));
		}
		clone.controlRegister = (Reg) cloneMap.get(controlRegister);
	}

	/**
	 * returns a boolean denoting that this loop is iterative (normal) or not
	 * (unrolled)
	 */
	public boolean isIterative() {
		if (getBody() == null) {
			return isIterative;
		} else {
			return isIterative && getBody().isIterative();
		}
	}

	public void setIterations(int iterations) {
		if ((iterations < 0) && (iterations != ITERATIONS_UNKNOWN)) {
			throw new IllegalArgumentException("invalid iterations: "
					+ iterations);
		}
		this.iterations = iterations;
	}

	/**
	 * If this is >0 and != ITERATIONS_UNKNOWN, the this describes NOT the
	 * number of iterations per se, but the number of times the decision in the
	 * body will evaluate to true. For example, getIterations()==0 for a
	 * While/For means just the decision evaluates false, but for an Unitil
	 * means the entire body evaluates then the decision evaluates false.
	 * 
	 * @return a value of type 'int'
	 */
	public int getIterations() {
		return iterations;
	}

	public boolean isBounded() {
		return getIterations() != ITERATIONS_UNKNOWN;
	}

	/**
	 * Finds the single Bus attached to the given port.
	 * 
	 * @param port
	 *            a value of type 'Port'
	 * @return a value of type 'Bus'
	 */
	private Bus getSingleBus(Port port) {
		assert port.getOwner().getEntries().size() == 1;
		Entry entry = port.getOwner().getEntries().get(0);
		Collection deps = entry.getDependencies(port);
		assert deps.size() == 1;
		return ((Dependency) deps.iterator().next()).getLogicalBus();
	}

	// Convenience methods for getting particular compile-time options

	public boolean isLoopUnrollingEnabled() {
		if (forceLoopUnrolling) {
			return true;
		}

		Option op = getGenericJob().getOption(
				OptionRegistry.LOOP_UNROLLING_ENABLE);
		return ((OptionBoolean) op).getValueAsBoolean(getSearchLabel());
	}

	/**
	 * By calling this method with the value set to true, the
	 * {@link Loop#isLoopUnrollingEnabled} method will be forced to return true.
	 * 
	 * @param value
	 *            a value of type 'boolean'
	 */
	public void setForceUnroll(boolean value) {
		forceLoopUnrolling = value;
	}

	public int getUnrollLimit() {
		Option op = getGenericJob().getOption(
				OptionRegistry.LOOP_UNROLLING_LIMIT);
		return ((OptionInt) op).getValueAsInt(getSearchLabel());
	}

	/**
	 * Tests whether this loop requires a go signal.
	 */
	@Override
	public boolean consumesGo() {
		return super.consumesGo() || isIterative();
	}

	/**
	 * Tests whether this loop produces done signals on its {@link Exit Exits}.
	 */
	@Override
	public boolean producesDone() {
		return super.producesDone() || isIterative();
	}

	/**
	 * Tests whether the done signal, if any, produced by this module (see
	 * {@link Module#producesDone()}) is synchronous or not. A true value means
	 * that the done signal will be produced with the clock and no earlier than
	 * the go signal is asserted.
	 * 
	 * @see Module#producesDone()
	 */
	@Override
	public boolean isDoneSynchronous() {
		return super.isDoneSynchronous() || isIterative();
	}

	/**
	 * Tests whether this loop requires a clock signal.
	 */
	@Override
	public boolean consumesClock() {
		return super.consumesClock() || isIterative();
	}

	/**
	 * Tests whether this loop requires a reset signal.
	 */
	@Override
	public boolean consumesReset() {
		return super.consumesReset() || isIterative();
	}

	public Component getDecisionOp() {
		LoopBody lb = getBody();
		if (lb == null) {
			return null;
		}
		Decision decision = lb.getDecision();
		if (decision == null) {
			return null;
		}
		return decision.getTestComponent();
	}

}

class RemoveMemoryAccessVisitor extends DefaultVisitor {
	@Override
	public void visit(ArrayRead ar) {
		ar.removeFromMemory();
	}

	@Override
	public void visit(ArrayWrite aw) {
		aw.removeFromMemory();
	}

	@Override
	public void visit(LocationConstant lc) {
		lc.removeFromMemory();
	}

	@Override
	public void visit(HeapRead hr) {
		hr.removeFromMemory();
	}

	@Override
	public void visit(HeapWrite hw) {
		hw.removeFromMemory();
	}

	@Override
	public void visit(AbsoluteMemoryRead comp) {
		comp.removeFromMemory();
	}

	@Override
	public void visit(AbsoluteMemoryWrite comp) {
		comp.removeFromMemory();
	}

	@Override
	public void visit(RegisterRead rr) {
		rr.getReferent().removeReference(rr);
	}

	@Override
	public void visit(RegisterWrite rw) {
		rw.getReferent().removeReference(rw);
	}
}
