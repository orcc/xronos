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
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import net.sf.openforge.lim.Exit.Tag;

/**
 * A Branch represents a choice of execution between two {@link Component
 * Components}, one representing a path for a true condition, the other a path
 * for a false condition. The choice is based upon the output value of a
 * {@link Decision}.
 * 
 * @version $Id: Branch.java 78 2005-12-14 21:24:03Z imiller $
 */
public class Branch extends Module {

	/** Boolean expression */
	private Decision decision;

	/** True path */
	private Module trueBranch;

	/** False path -- might be an empty Block */
	private Module falseBranch;

	/**
	 * Constructs a new Branch.
	 * 
	 * @param decision
	 *            the decision which chooses the execution path
	 * @param trueBranch
	 *            the true path component
	 * @param falseBranch
	 *            the false path component, null if there is none
	 */
	public Branch(Decision decision, Module trueBranch, Module falseBranch) {
		super();
		assert trueBranch != null : "No 'true' component specified";
		addComponent(this.decision = decision);
		addComponent(this.trueBranch = trueBranch);
		this.falseBranch = falseBranch == null ? new Block(
				Collections.EMPTY_LIST) : falseBranch;
		addComponent(this.falseBranch);
		setControlDependencies();
	}

	/**
	 * Constructs a Branch with no else clause.
	 * 
	 * @param decision
	 *            the decision which chooses the execution path
	 * @param trueBranch
	 *            the true path component
	 */
	public Branch(Decision decision, Module trueBranch) {
		this(decision, trueBranch, new Block(Collections.EMPTY_LIST));
	}

	@Override
	public void accept(Visitor vis) {
		vis.visit(this);
	}

	/**
	 * Gets the decision.
	 * 
	 * @return the decision which chooses the execution path
	 */
	public Decision getDecision() {
		return decision;
	}

	/**
	 * Gets the {@link Component} that is executed if the {@link Decision} is
	 * true.
	 */
	public Module getTrueBranch() {
		return trueBranch;
	}

	/**
	 * Gets the {@link Component} that is executed if the {@link Decision} is
	 * false.
	 */
	public Module getFalseBranch() {
		return falseBranch;
	}

	/**
	 * Removes the specified component from this branch
	 * 
	 * @param component
	 *            a value of type 'Component'
	 * @return a value of type 'boolean'
	 */
	@Override
	public boolean removeComponent(Component component) {
		component.disconnect();
		if (component == decision) {
			decision = null;
		} else if (component == trueBranch) {
			trueBranch = null;
		} else if (component == falseBranch) {
			falseBranch = null;
		}
		return super.removeComponent(component);
	}

	@Override
	public boolean replaceComponent(Component removed, Component inserted) {
		assert removed != null;
		if (removed == getDecision()) {
			decision = (Decision) inserted;
		} else if (removed == getTrueBranch()) {
			assert (inserted instanceof Module) : "Replacement for true must be a module";
			trueBranch = (Module) inserted;
		} else if (removed == getFalseBranch()) {
			assert (inserted instanceof Module) : "Replacement for false must be a module";
			falseBranch = (Module) inserted;
		} else
			throw new IllegalArgumentException(
					"Cannot replace unknown component in " + getClass());

		boolean mod = removeComponent(removed);
		addComponent(inserted);
		return mod;
	}

	/**
	 * Sets the internal dependencies for all clock, reset, and go {@link Port
	 * Ports} of the children of this Branch. Also creates the {@link Exit
	 * Exits} for this Branch based upon the {@link Exit Exits} of its
	 * components.
	 */
	private void setControlDependencies() {
		final InBuf inBuf = getInBuf();
		final Bus clockBus = inBuf.getClockBus();
		final Bus resetBus = inBuf.getResetBus();
		final Bus goBus = inBuf.getGoBus();

		final Decision decision = getDecision();
		assert decision.getEntries().isEmpty() : "Not yet re-entrant";
		addDependencies(decision.makeEntry(inBuf.getExit(Exit.DONE)), clockBus,
				resetBus, goBus);

		final Component trueComponent = getTrueBranch();
		addDependencies(
				trueComponent.makeEntry(decision.getTrueBus().getOwner()),
				clockBus, resetBus, decision.getTrueBus());

		final Component falseComponent = getFalseBranch();
		addDependencies(
				falseComponent.makeEntry(decision.getFalseBus().getOwner()),
				clockBus, resetBus, decision.getFalseBus());

		final Map<Tag, Collection<Exit>> exitMap = new HashMap<Tag, Collection<Exit>>(
				11);
		collectExits(trueComponent, exitMap);
		collectExits(falseComponent, exitMap);
		mergeExits(exitMap, clockBus, resetBus);
	}

	@Override
	protected void cloneNotify(Module moduleClone, Map cloneMap) {
		super.cloneNotify(moduleClone, cloneMap);
		final Branch clone = (Branch) moduleClone;
		clone.decision = (Decision) cloneMap.get(decision);
		clone.trueBranch = (Module) cloneMap.get(trueBranch);
		clone.falseBranch = (Module) cloneMap.get(falseBranch);
	}

	// /**
	// * Returns the clone of this Branch with the decision, true and
	// * false branches all set to the correct cloned components.
	// *
	// * @return a cloned copy of this Branch
	// * @exception CloneNotSupportedException if an error occurs
	// */
	// public Object clone () throws CloneNotSupportedException
	// {
	// Branch clone = (Branch)super.clone();

	// Map correlation = getCloneCorrelationMap();

	// // The components are already cloned and hooked up, we just
	// // need to grab them from the correlation map.
	// assert correlation.containsKey(this.decision) :
	// "No decision in correlation map";
	// assert correlation.containsKey(this.trueBranch) :
	// "No truebranch in correlation map";
	// assert correlation.containsKey(this.falseBranch) :
	// "No falsebranch in correlation map";

	// clone.decision = (Decision)correlation.get(this.decision);
	// clone.trueBranch = (Component)correlation.get(this.trueBranch);
	// clone.falseBranch = (Component)correlation.get(this.falseBranch);

	// // DONT clear the maps here, Decision and perhaps resources
	// // need the correlation information from sub-blocks.
	// // clearCloneCorrelationMap();
	// // clone.clearCloneCorrelationMap();

	// return clone;
	// }

} // class Branch
