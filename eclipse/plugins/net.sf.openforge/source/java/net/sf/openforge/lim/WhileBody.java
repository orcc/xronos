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
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;

/**
 * A "while" style {@link LoopBody}. That is, the test is performed before each
 * iteration. WhileBody is composed of a {@link Decision}, which performs the
 * test, and a {@link Component} which is executed if the test is true.
 * <P>
 * 
 * @author Stephen Edwards
 * @version $Id: WhileBody.java 2 2005-06-09 20:00:48Z imiller $
 */
public class WhileBody extends LoopBody implements Cloneable {

	/** The iteration test */
	private Decision decision;

	/* The content of the loop body */
	private Module body;

	/**
	 * 
	 * Constructs a WhileBody and establishes control dependencies between the
	 * decision, body and perimeter of the WhileBody.
	 * 
	 * @param decision
	 *            the iteration test
	 * @param body
	 *            the content of the loop body
	 */
	public WhileBody(Decision decision, Module body) {
		super();
		addComponent(this.decision = decision);
		addComponent(this.body = body);
		setControlDependencies();
	}

	protected void setControlDependencies() {
		final Bus clockBus = getInBuf().getClockBus();
		final Bus resetBus = getInBuf().getResetBus();
		final Bus goBus = getInBuf().getGoBus();
		final Exit inbufExit = getInBuf().getExit(Exit.DONE);

		final Map exitMap = new LinkedHashMap(11);

		/*
		 * InBuf to Decision.
		 */
		assert getDecision().getEntries().isEmpty() : "Unexpected Decision Entry(s)";
		final Entry decisionEntry = getDecision().makeEntry(inbufExit);
		addDependencies(decisionEntry, clockBus, resetBus, goBus);
		collectExits(getDecision(), exitMap);
		exitMap.remove(getDecision().getTrueExit().getTag());

		/*
		 * Decision true to body Component.
		 */
		assert getBody().getEntries().isEmpty() : "Unexpected body Entry(s)";
		final Entry bodyEntry = getBody()
				.makeEntry(getDecision().getTrueExit());
		addDependencies(bodyEntry, clockBus, resetBus, getDecision()
				.getTrueBus());
		collectExits(getBody(), exitMap);

		/*
		 * Feedback Exit is comprised of body Component done and CONTINUE Exit,
		 * if any. There may be no feedback exits if the body ends with a break.
		 */
		Collection feedbackExits = (Collection) exitMap.remove(Exit
				.getTag(Exit.DONE));
		if (feedbackExits == null) {
			feedbackExits = new LinkedList();
		}
		final Collection continueExits = (Collection) exitMap.remove(Exit
				.getTag(Exit.CONTINUE));
		if (continueExits != null) {
			feedbackExits.addAll(continueExits);
		}
		if (!feedbackExits.isEmpty()) {
			exitMap.put(FEEDBACK_TAG, feedbackExits);
		}

		/*
		 * Completion Exit is comprised of Decision false and body's BREAK Exit,
		 * if any.
		 */
		Collection completeExits = (Collection) exitMap.remove(getDecision()
				.getFalseExit().getTag());
		if (completeExits == null) {
			completeExits = new LinkedList();
		}
		final Collection breakExits = (Collection) exitMap.remove(Exit
				.getTag(Exit.BREAK));
		if (breakExits != null) {
			completeExits.addAll(breakExits);
		}
		assert !completeExits.isEmpty();
		exitMap.put(Exit.getTag(Exit.DONE), completeExits);

		mergeExits(exitMap, clockBus, resetBus);
	}

	public void accept(Visitor vis) {
		vis.visit(this);
	}

	/**
	 * Gets the iteration test.
	 */
	public Decision getDecision() {
		return decision;
	}

	/**
	 * Gets the iteration contents.
	 */
	public Module getBody() {
		return body;
	}

	/**
	 * returns true of the decision comes before the body of the loop, or false
	 * if after. This is needed to compute the number of iterations for loop
	 * unrolling
	 */
	public boolean isDecisionFirst() {
		return true;
	}

	public boolean removeComponent(Component component) {
		component.disconnect();
		if (component == decision) {
			decision = null;
		} else if (component == body) {
			body = null;
		}
		return super.removeComponent(component);
	}

	public boolean replaceComponent(Component removed, Component inserted) {
		assert removed != null;
		if (removed == getDecision()) {
			this.decision = (Decision) inserted;
		} else if (removed == getBody()) {
			this.body = (Module) inserted;
		} else
			throw new IllegalArgumentException(
					"Cannot replace unknown component in " + getClass());

		boolean mod = removeComponent(removed);
		addComponent(inserted);
		return mod;
	}

	/**
	 * replace the body of the loop NOTE: the control dependencies must be set
	 * up again
	 */
	// public boolean replaceBody (Component body)
	// {
	// if (!removeComponent(this.body))
	// {
	// return false;
	// }
	// this.body=body;
	// addComponent(body);
	// //setControlDependencies(); // not reentrant
	// return true;
	// }
	/**
	 * For a given Bus from the feedback Exit, gets the corresponding Port that
	 * represents the initial value.
	 * 
	 * @param feedbackBus
	 *            a bus from the feedback Exit
	 * @return the port which corresponds to the initial value of the feedback
	 *         data flow
	 */
	public Port getInitalValuePort(Bus feedbackBus) {
		/*
		 * tbd
		 */
		return null;
	}

	protected void cloneNotify(Module moduleClone, Map cloneMap) {
		super.cloneNotify(moduleClone, cloneMap);
		final WhileBody clone = (WhileBody) moduleClone;
		clone.decision = (Decision) cloneMap.get(decision);
		clone.body = (Module) cloneMap.get(body);
	}

}
