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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

/**
 * An "until" style {@link LoopBody}. That is, the test is performed at the end
 * of each iteration. UntilBody is composed of a {@link Component} which is
 * executed at the start of each iteration and a {@link Decision} which performs
 * a test to determine whether another iteration should occur.
 * 
 * @author Stephen Edwards
 * @version $Id: UntilBody.java 2 2005-06-09 20:00:48Z imiller $
 */
public class UntilBody extends LoopBody implements Cloneable {

	/* The content of the loop body */
	private Module body;

	/** The iteration test */
	private Decision decision;

	/**
	 * Constructs an UntilBody and establishes control dependencies between the
	 * decision, body and perimeter of the UntilBody..
	 * 
	 * @param body
	 *            the content of the loop body
	 * @param decision
	 *            the iteration test
	 */
	public UntilBody(Decision decision, Module body) {
		super();
		addComponent(this.body = body);
		addComponent(this.decision = decision);
		setControlDependencies();
	}

	protected void setControlDependencies() {
		final Bus clockBus = getInBuf().getClockBus();
		final Bus resetBus = getInBuf().getResetBus();
		final Bus goBus = getInBuf().getGoBus();
		final Exit inbufExit = getInBuf().getExit(Exit.DONE);

		/*
		 * InBuf to body Component.
		 */
		final Module body = getBody();
		assert body.getEntries().isEmpty() : "Unexpected body Entry(s)";
		final Entry bodyEntry = body.makeEntry(inbufExit);
		addDependencies(bodyEntry, clockBus, resetBus, goBus);

		/*
		 * Body Component to Decision.
		 */
		final Map exitMap = new HashMap();
		collectExits(getBody(), exitMap);

		Exit bodyDoneExit = body.getExit(Exit.DONE);
		Exit bodyContinueExit = body.getExit(Exit.CONTINUE);

		if ((bodyDoneExit == null) && (bodyContinueExit == null)) {
			/*
			 * The Decision can only be reached when the body completes or exits
			 * with a 'continue'. If neither Exit exits, remove the Decision.
			 */
			removeComponent(decision);
			decision = null;
		} else {
			/*
			 * If both DONE and CONTINUE exist, they have to be merged into one
			 * OutBuf/Exit before connecting to the Decision; otherwise there
			 * would be two parallel control paths between the components, which
			 * is not good form.
			 */
			if ((bodyDoneExit != null) && (bodyContinueExit != null)) {
				/*
				 * Merge the CONTINUE OutBuf Entries onto the DONE OutBuf.
				 */
				final OutBuf doneOutBuf = bodyDoneExit.getPeer();
				final OutBuf continueOutBuf = bodyContinueExit.getPeer();
				for (Iterator iter = continueOutBuf.getEntries().iterator(); iter
						.hasNext();) {
					final Entry continueOutBufEntry = (Entry) iter.next();
					final Entry doneOutBufEntry = doneOutBuf
							.makeEntry(continueOutBufEntry.getDrivingExit());
					final Port continueGoPort = continueOutBuf.getGoPort();
					final Collection continueDependencies = new ArrayList(
							continueOutBufEntry.getDependencies(continueGoPort));
					for (Iterator diter = continueDependencies.iterator(); diter
							.hasNext();) {
						final Dependency dependency = (Dependency) diter.next();
						continueOutBufEntry.removeDependency(continueGoPort,
								dependency);
						final Dependency doneDependency = (Dependency) dependency
								.clone();
						doneDependency
								.setLogicalBus(dependency.getLogicalBus());
						doneOutBufEntry.addDependency(doneOutBuf.getGoPort(),
								doneDependency);
					}
				}

				body.removeExit(continueOutBuf.getPeer());
				exitMap.remove(bodyContinueExit.getTag());
				bodyContinueExit = null;
			}

			/*
			 * Otherwise, connect the remaining exit, if any, to a new Entry on
			 * the Decision.
			 */
			if (bodyDoneExit != null) {
				exitMap.remove(bodyDoneExit.getTag());
				final Entry decisionEntry = getDecision().makeEntry(
						bodyDoneExit);
				addDependencies(decisionEntry, clockBus, resetBus, goBus);
			} else if (bodyContinueExit != null) {
				exitMap.remove(bodyContinueExit.getTag());
				final Entry decisionEntry = getDecision().makeEntry(
						bodyContinueExit);
				addDependencies(decisionEntry, clockBus, resetBus, goBus);
			}

			collectExits(getDecision(), exitMap);
		}

		/*
		 * Feedback Exit is comprised of Decision true Exit.
		 */
		Collection feedbackExits = new HashSet();
		if (decision != null) {
			final Exit.Tag trueTag = getDecision().getTrueExit().getTag();
			feedbackExits = (Collection) exitMap.remove(trueTag);
		}

		if (!feedbackExits.isEmpty()) {
			exitMap.put(FEEDBACK_TAG, feedbackExits);
		}

		/*
		 * Completion Exit is comprised of Decision false Exit and body's BREAK
		 * Exit, if any.
		 */
		Collection completeExits = new HashSet();
		if (decision != null) {
			final Exit.Tag falseTag = getDecision().getFalseExit().getTag();
			completeExits = (Collection) exitMap.remove(falseTag);
		}

		final Collection breakExits = (Collection) exitMap.remove(Exit
				.getTag(Exit.BREAK));
		if (breakExits != null) {
			completeExits.addAll(breakExits);
		}

		if (!completeExits.isEmpty()) {
			exitMap.put(Exit.getTag(Exit.DONE), completeExits);
		}

		mergeExits(exitMap, clockBus, resetBus);
	}

	@Override
	public void accept(Visitor vis) {
		vis.visit(this);
	}

	/**
	 * Gets the iteration test.
	 */
	@Override
	public Decision getDecision() {
		return decision;
	}

	/**
	 * Gets the iteration contents.
	 */
	@Override
	public Module getBody() {
		return body;
	}

	/**
	 * returns true of the decision comes before the body of the loop, or false
	 * if after. This is needed to compute the number of iterations for loop
	 * unrolling
	 */
	@Override
	public boolean isDecisionFirst() {
		return false;
	}

	@Override
	public boolean removeComponent(Component component) {
		component.disconnect();
		if (component == decision) {
			decision = null;
		} else if (component == body) {
			body = null;
		}
		return super.removeComponent(component);
	}

	@Override
	public boolean replaceComponent(Component removed, Component inserted) {
		assert removed != null;
		if (removed == getDecision()) {
			decision = (Decision) inserted;
		} else if (removed == getBody()) {
			body = (Module) inserted;
		} else
			throw new IllegalArgumentException(
					"Cannot replace unknown component in " + getClass());

		boolean mod = removeComponent(removed);
		addComponent(inserted);
		return mod;
	}

	/**
	 * For a given Bus from the feedback Exit, gets the corresponding Port that
	 * represents the initial value.
	 * 
	 * @param feedbackBus
	 *            a bus from the feedback Exit
	 * @return the port which corresponds to the initial value of the feedback
	 *         data flow
	 */
	@Override
	public Port getInitalValuePort(Bus feedbackBus) {
		/*
		 * tbd
		 */
		return null;
	}

	@Override
	protected void cloneNotify(Module moduleClone, Map cloneMap) {
		super.cloneNotify(moduleClone, cloneMap);
		final UntilBody clone = (UntilBody) moduleClone;
		clone.body = (Module) cloneMap.get(body);
		clone.decision = (Decision) cloneMap.get(decision);
	}
}
