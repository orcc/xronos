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
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Decision is a refinement of {@link Module} for components that produce a
 * truth value. It defines two {@link Exit Exits}, one whose done {@link Bus}
 * signals <i>true</i> and one whose done {@link Bus} signals <i>false</i>. On a
 * true condition, the true bus is asserted and the false bus is deasserted; on
 * a false condition, the true bus is deasserted and the false bus is asserted.
 * <P>
 * The Decision is constructed with a {@link Component} representing the boolean
 * value to be tested. This argument must produce a single data value.
 * <P>
 * <b>NOTE: it is the responsibility of the caller (i.e., the scheduler) to AND
 * the the {@link Bus} representing the boolean values the ready {@link Bus}
 * before they exit the Decision if necessary; this will be necessary if the
 * test component requires a control signal.</b>
 * 
 * @version $Id: Decision.java 558 2008-03-14 14:14:48Z imiller $
 */
public class Decision extends Module {

	/**
	 * The block containing the logic that is executed to produce the boolean
	 * value
	 */
	private Block testBlock;

	/** The component of the testBlock whose data Bus provides the boolean value */
	private Component testComponent;

	/** Used to invert the boolean value to obtain the false output */
	private Not not;

	/** Used to merge the control signal for exit of true block. */
	private And trueAnd;

	/** Used to merge the control signal for exit of true block. */
	private And falseAnd;

	/** Exit whose done Bus signals a true decision */
	private Exit trueExit;

	/** Exit whose done Bus signals a false decision */
	private Exit falseExit;

	/**
	 * Constructs a new Decision. It is the responsibility of the caller to add
	 * an appropriate {@link DataDependency} to the input of the {@link Not}
	 * component; in this case, the dependency should be made on the data output
	 * of the given {@link Component}.
	 * 
	 * @param testBlock
	 *            a component (possibly a module) which is executed within the
	 *            Decision to obtain a boolean value
	 * @param testComponent
	 *            a {@link Component} within the testBlock which is the
	 *            component which provides the boolean true/false for this
	 *            decision.
	 */
	public Decision(Block testBlock, Component testComponent) {
		this.testBlock = testBlock;
		this.testComponent = testComponent;
		this.testComponent.setNonRemovable();
		assert testComponent.getOwner() == testBlock;
		addComponent(testBlock);
		addComponent(not = new Not());
		addComponent(trueAnd = new And(2));
		addComponent(falseAnd = new And(2));
		this.trueExit = makeExit(0, Exit.DONE, "true");
		this.falseExit = makeExit(0, Exit.DONE, "false");
		setControlDependencies();
		setDataDependencies();
		setProducesDone(true);
	}

	public void accept(Visitor vis) {
		vis.visit(this);
	}

	/**
	 * Gets the {@link Block} that is executed within this Decision in order to
	 * obtain a boolean value.
	 * 
	 * @return the component
	 */
	public Block getTestBlock() {
		return testBlock;
	}

	/**
	 * The low level {@link Component} of the test {@link Block} that drives the
	 * boolean test value with its output {@link Bus}.
	 */
	public Component getTestComponent() {
		return testComponent;
	}

	/**
	 * Gets the Not that is used to invert the output of test component.
	 */
	public Not getNot() {
		return not;
	}

	/**
	 * Gets the And of true exit that is used to produce the "true" output.
	 */
	public And getTrueAnd() {
		return trueAnd;
	}

	/**
	 * Gets the And of false exit that is used to produce the "false" output.
	 */
	public And getFalseAnd() {
		return falseAnd;
	}

	/**
	 * Gets the <i>true</i> {@link Exit}.
	 */
	public Exit getTrueExit() {
		return trueExit;
	}

	/**
	 * Gets the <i>false</i> {@link Exit}.
	 */
	public Exit getFalseExit() {
		return falseExit;
	}

	/**
	 * Gets the true bus.
	 * 
	 * @return the bus that is asserted when the decision is true
	 */
	public Bus getTrueBus() {
		return getTrueExit().getDoneBus();
	}

	/**
	 * Gets the false bus.
	 * 
	 * @return the bus that is asserted when the decision is false
	 */
	public Bus getFalseBus() {
		return getFalseExit().getDoneBus();
	}

	/**
	 * Gets the data bus coming out of the testBlock that is used to create the
	 * true and false buses.
	 * 
	 * @return a value of type 'Bus'
	 */
	public Bus getDecisionDataBus() {
		assert this.not.getEntries().size() == 1;
		Entry notEntry = (Entry) this.not.getEntries().get(0);
		Collection notDeps = notEntry.getDependencies(this.not.getDataPort());
		assert notDeps.size() == 1;
		Bus decisionData = ((Dependency) notDeps.iterator().next())
				.getLogicalBus();
		return decisionData;
	}

	/**
	 * Returns the 'done' bus from this decision which asserts when the test
	 * block has completed (regardless of the true/false value of the decision)
	 * and creates the 'no decision' exit on this Decision if needed.
	 * 
	 * @return the done bus for this decision.
	 */
	public Bus createAndGetNoDecisionBus() {
		Exit noDecision = getExit(Exit.DONE, "NoDecision");
		if (noDecision == null) {
			noDecision = this.makeExit(0, Exit.DONE, "NoDecision");
			Port port = noDecision.getPeer().getGoPort();
			Exit blockExit = getTestBlock().getExit(Exit.DONE);
			InBuf inbuf = getInBuf();
			addDependencies(noDecision.getPeer().makeEntry(blockExit),
					getInBuf().getClockBus(), getInBuf().getResetBus(),
					blockExit.getDoneBus());
		}
		return noDecision.getDoneBus();
	}

	public boolean replaceComponent(Component removed, Component inserted) {
		assert removed != null;
		if (removed == getTestBlock()) {
			this.testBlock = (Block) inserted;
		} else if (removed == getNot()) {
			this.not = (Not) inserted;
		} else if (removed == getTrueAnd()) {
			this.trueAnd = (And) inserted;
		} else if (removed == getFalseAnd()) {
			this.trueAnd = (And) inserted;
		} else
			throw new IllegalArgumentException(
					"Cannot replace unknown component in " + getClass());

		boolean mod = removeComponent(removed);
		addComponent(inserted);
		return mod;
	}

	public boolean removeComponent(Component component) {
		component.disconnect();
		boolean removed = super.removeComponent(component);

		if (component == testBlock) {
			testBlock = null;
		} else if (component == testComponent) {
			testComponent = null;
			if (testBlock != null) {
				testBlock.removeComponent(component);
			}
		} else if (component == not) {
			not = null;
		} else if (component == trueAnd) {
			trueAnd = null;
		} else if (component == falseAnd) {
			falseAnd = null;
		}

		return removed;
	}

	/**
	 * Sets the internal dependencies for all clock, reset, and go {@link Port
	 * Ports} of the children of this Decision. Also creates the {@link Exit
	 * Exits} for this Decision based upon the {@link Exit Exits} of its
	 * components.
	 */
	private void setControlDependencies() {
		/*
		 * Set the dependencies so that everything is initially assumed to be
		 * able to execute in parallel.
		 */
		final InBuf inBuf = getInBuf();
		final Bus clockBus = inBuf.getClockBus();
		final Bus resetBus = inBuf.getResetBus();
		final Bus goBus = inBuf.getGoBus();

		/*
		 * Connect the component that produces the boolean value and make its
		 * done the new go.
		 */
		final Component bool = getTestBlock();
		Exit doneExit = inBuf.getExit(Exit.DONE);
		addDependencies(bool.makeEntry(doneExit), clockBus, resetBus, goBus);

		/*
		 * Connect the true And.
		 */
		final And trueAnd = getTrueAnd();
		doneExit = bool.getExit(Exit.DONE);
		addDependencies(trueAnd.makeEntry(doneExit), clockBus, resetBus, goBus);

		/*
		 * Connect the true OutBuf.
		 */
		final Component trueOutBuf = getTrueExit().getPeer();
		doneExit = trueAnd.getExit(Exit.DONE);
		addDependencies(trueOutBuf.makeEntry(doneExit), clockBus, resetBus,
				trueAnd.getResultBus());

		/*
		 * Connect the Not.
		 */
		final Not not = getNot();
		doneExit = bool.getExit(Exit.DONE);
		addDependencies(not.makeEntry(doneExit), clockBus, resetBus, goBus);

		/*
		 * Connect the false And.
		 */
		final And falseAnd = getFalseAnd();
		doneExit = not.getExit(Exit.DONE);
		addDependencies(falseAnd.makeEntry(doneExit), clockBus, resetBus, goBus);

		/*
		 * Connect the false OutBuf.
		 */
		final Component falseOutBuf = getFalseExit().getPeer();
		doneExit = falseAnd.getExit(Exit.DONE);
		addDependencies(falseOutBuf.makeEntry(doneExit), clockBus, resetBus,
				falseAnd.getResultBus());

		/*
		 * Merge and create non-DONE Exits.
		 */
		mergeExits();
	}

	private void setDataDependencies() {
		final Bus goBus = getInBuf().getGoBus();
		final Block testBlock = getTestBlock();
		final Bus boolBus = Component.getDataBus(getTestComponent());
		final Bus notBus = Component.getDataBus(getNot());
		final Bus notDoneBus = getNot().getExit(Exit.DONE).getDoneBus();
		final Bus trueAndBus = Component.getDataBus(getTrueAnd());
		final Bus falseAndBus = Component.getDataBus(getFalseAnd());
		final Exit testBlockExit = testBlock.getExit(Exit.DONE);
		final Bus testDoneBus = testBlockExit.getDoneBus();
		final Bus testBlockBus = testBlockExit.makeDataBus();

		/*
		 * Connect the test Block outBuf.
		 */
		final OutBuf testBlockBuf = (OutBuf) testBlockExit.getPeer();
		Entry entry = testBlockBuf.getMainEntry();
		entry.addDependency(testBlockBus.getPeer(), new DataDependency(boolBus));

		/*
		 * Connect the true And; it uses the go bus and boolean data bus as
		 * input data.
		 */
		final And trueAnd = getTrueAnd();
		entry = trueAnd.getMainEntry();
		entry.addDependency((Port) trueAnd.getDataPorts().get(0),
				new DataDependency(testDoneBus));
		entry.addDependency((Port) trueAnd.getDataPorts().get(1),
				new DataDependency(testBlockBus));

		/*
		 * Connect the NOT's data input to the same boolean data bus.
		 */
		final Not not = getNot();
		entry = not.getMainEntry();
		entry.addDependency(not.getDataPort(), new DataDependency(testBlockBus));

		/*
		 * Connect the false And, it uses the go bus and Not data bus as input
		 * data.
		 */
		final And falseAnd = getFalseAnd();
		entry = falseAnd.getMainEntry();
		entry.addDependency((Port) falseAnd.getDataPorts().get(0),
				new DataDependency(notDoneBus));
		entry.addDependency((Port) falseAnd.getDataPorts().get(1),
				new DataDependency(notBus));
	}

	/**
	 * Creates the {@link Exit Exits} of a this Module based upon the
	 * {@link Exit Exits} of its components. For each {@link Exit.Tag Tag}, an
	 * {@link Exit} is created on the {@link Module}; the peer of that
	 * {@link Exit} is then decorated with an {@link Entry} and a
	 * {@link ControlDependency} for each child {@link Exit} with that same
	 * {@link Exit.Tag Tag}.
	 * 
	 * @param exitMap
	 *            a map of exit tag to collection of child exits with that tag
	 *            for all relevant components within the module
	 * @param clockBus
	 *            the clock bus for each new exit
	 * @param resetBus
	 *            the reset bus for each new exit
	 */
	protected void mergeExits(Map exitMap, Bus clockBus, Bus resetBus) {
		exitMap.remove(getTrueExit().getTag());
		exitMap.remove(getFalseExit().getTag());
		final Collection doneExits = (Collection) exitMap.remove(Exit
				.getTag(Exit.DONE));
		super.mergeExits(exitMap, clockBus, resetBus);
	}

	/**
	 * Overrides method in {@link Module Module} to ensure that the arguments to
	 * the decision are scheduled before the {@link Not Not}.
	 * 
	 * @return a value of type 'List'
	 */
	protected List getCompsScheduleOrder() {
		/*
		 * XXX: Traversal should be done by the scheduler in the order that it
		 * wants -- get rid of this.
		 */
		assert false : "Get rid of this";

		List ordered = new LinkedList();
		// Make order inbufs, <most other stuff>, NotOp, outbufs
		Set inbufs = new LinkedHashSet();
		Set outbufs = new LinkedHashSet();
		for (Iterator iter = getComponents().iterator(); iter.hasNext();) {
			Component c = (Component) iter.next();
			if (c instanceof InBuf)
				inbufs.add(c);
			else if (c instanceof OutBuf)
				outbufs.add(c);
			else if (c == getNot())
				;
			else if (c == getTrueAnd())
				;
			else if (c == getFalseAnd())
				;
			else
				ordered.add(c);
		}

		ordered.addAll(0, inbufs);
		ordered.add(getNot());
		ordered.add(getTrueAnd());
		ordered.add(getFalseAnd());
		ordered.addAll(outbufs);

		return ordered;
	}

	public Object clone() throws CloneNotSupportedException {
		/*
		 * Use a CloneListener to obtain the clone of testComponent, which is
		 * hidden inside the testBlock.
		 */
		DefaultCloneListener cloneListener = new DefaultCloneListener();
		testBlock.addCloneListener(cloneListener);
		Decision clone = (Decision) super.clone();
		clone.testComponent = cloneListener.getClone(testComponent);
		testBlock.removeCloneListener(cloneListener);
		return clone;
	}

	protected void cloneNotify(Module moduleClone, Map cloneMap) {
		super.cloneNotify(moduleClone, cloneMap);
		/*
		 * Update the clone fields, except for testComponent which is handled in
		 * clone().
		 */
		final Decision clone = (Decision) moduleClone;
		clone.testBlock = (Block) cloneMap.get(testBlock);
		clone.not = (Not) cloneMap.get(not);
		clone.trueAnd = (And) cloneMap.get(trueAnd);
		clone.falseAnd = (And) cloneMap.get(falseAnd);
		clone.trueExit = getExitClone(trueExit, cloneMap);
		clone.falseExit = getExitClone(falseExit, cloneMap);
	}

}
