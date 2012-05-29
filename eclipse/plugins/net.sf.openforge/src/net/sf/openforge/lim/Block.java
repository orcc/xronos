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
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import net.sf.openforge.app.EngineThread;
import net.sf.openforge.app.project.Configurable;
import net.sf.openforge.app.project.SearchLabel;
import net.sf.openforge.lim.Exit.Tag;

/**
 * A Block is a {@link Module Module} that contains a sequence of
 * {@link Component Components} that are logically executed one after the other.
 * 
 * @author Stephen Edwards
 * @version $Id: Block.java 100 2006-02-03 22:49:08Z imiller $
 */
public class Block extends Module {

	/** Ordered sequence of Components to be executed */
	protected List<Component> sequence = null;

	/** The Procedure (if any) for which this is the body. */
	private Procedure procedure;

	/**
	 * Use this for deferred intialization
	 * 
	 */
	public Block(boolean isProcedureBody) {
		sequence = new LinkedList<Component>();
	}

	/**
	 * Constructs a Block.
	 * 
	 * @param sequence
	 *            a list of Components in logical order of execution
	 * @param isProcedureBody
	 *            true if this is a {@link Procedure} body, false otherwise; the
	 *            {@link Exit Exits} of a body are merged with some small
	 *            differences
	 */
	public Block(List<Component> sequence, boolean isProcedureBody) {
		super();
		this.sequence = new LinkedList<Component>(sequence);
		for (Component component : sequence) {
			addComponent(component);
		}
		setControlDependencies(isProcedureBody);
	}

	/**
	 * Constructs a Block that is not a {@link Procedure} body.
	 * 
	 * @param sequence
	 *            a list of Components in logical order of execution
	 */
	public Block(List<Component> sequence) {
		this(sequence, false);
	}

	@Override
	public void accept(Visitor vis) {
		vis.visit(this);
	}

	/**
	 * Gets the execution sequence.
	 * 
	 * @return the list of Components in order of execution
	 */
	public List<Component> getSequence() {
		return Collections.unmodifiableList(sequence);
	}

	/**
	 * Checks whether a Block is a Procedure body or not
	 * 
	 * @return True if a Block is a procedure body, false otherwise
	 */
	public boolean isProcedureBody() {
		return getProcedure() != null;
	}

	@Override
	public SearchLabel getSearchLabel() {
		SearchLabel sl = null;
		if (getOwner() != null) {
			sl = super.getSearchLabel();
		} else {
			if (isProcedureBody()) {
				sl = getProcedure().getSearchLabel();
			} else {
				EngineThread.getEngine().fatalError(
						"Internal Error. Invariant on Block not met.");
			}
		}
		assert sl != null;
		return sl;
	}

	/**
	 * Sets the Procedure (if any) for which this block is the body.
	 * 
	 * @param procedure
	 *            the Procedure owner of this block
	 */
	public void setProcedure(Procedure procedure) {
		this.procedure = procedure;
	}

	/**
	 * Gets the Procedure (if any) for which this block is the body.
	 * 
	 * @return the Procedure, or null if this is not a procedure's body
	 */
	public Procedure getProcedure() {
		return procedure;
	}

	/**
	 * Gets the Configurable parent (according to scope rules) of this
	 * Component.
	 * 
	 * @return the Configurable parent
	 */
	@Override
	public Configurable getConfigurableParent() {
		return ((isProcedureBody()) ? (Configurable) getProcedure()
				: (Configurable) getOwner());
	}

	/**
	 * Remove a list of components
	 * 
	 * @param components
	 *            list of components to remove
	 * @return true if all were removed, else false
	 */
	public boolean removeComponents(List<Component> components) {
		for (Component component : components) {
			if (!removeComponent(component)) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Removes a component from this block sequence
	 * 
	 * @param component
	 *            the component to be removed
	 * @return true if the component was removed, false if not found
	 */
	@Override
	public boolean removeComponent(Component component) {
		// first clear the exits
		for (Exit exit : component.getExits()) {
			for (Entry entry : new ArrayList<Entry>(exit.getDrivenEntries())) {
				entry.setDrivingExit(null);
			}
		}
		component.disconnect();
		boolean removed = super.removeComponent(component);
		if (sequence.remove(component)) {
			component.setOwner(null);
			removed = true;
		}
		return removed;
	}

	/**
	 * Inserts a list of components. the user must take care of dependencies
	 * 
	 * @param component
	 *            the list of components
	 * @param offset
	 *            where in the sequence to insert the first element of
	 *            components
	 * @return true if succesful, else false
	 */
	public boolean insertComponents(List<Component> components, int offset) {
		for (ListIterator<Component> iter = components.listIterator(components
				.size()); iter.hasPrevious();) {
			Component c = iter.previous();
			if (!insertComponent(c, offset)) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Inserts a component. the user must take care of dependencies
	 * 
	 * @param component
	 *            to insert
	 * @param offset
	 *            where in the sequence to insert the component
	 * @return true if succesful, else false
	 */
	public boolean insertComponent(Component component, int offset) {
		try {
			sequence.add(offset, component);
			addComponent(component);
		} catch (IndexOutOfBoundsException exc) {
			return false;
		}
		return true;
	}

	/**
	 * Replaces a list of components with another list. the user must take care
	 * of dependencies assumes that each element in the sequence is unique, and
	 * that elements in the lists are sequential
	 * 
	 * @param remove
	 *            list of components to be removed
	 * @param insert
	 *            list of components to insert instead
	 * @return true if successful, else false
	 */
	public boolean replaceComponents(List<Component> remove,
			List<Component> insert) {
		int offset = sequence.indexOf(remove.get(0));
		if (offset == -1 || !removeComponents(remove)
				|| !insertComponents(insert, offset)) {
			return false;
		}
		return true;
	}

	/**
	 * Replace a component with another. the user must take care of dependencies
	 * 
	 * @param remove
	 *            component to remove
	 * @param insert
	 *            component to insert
	 * @return true if successful, else false
	 */
	@Override
	public boolean replaceComponent(Component remove, Component insert) {
		int offset = sequence.indexOf(remove);
		if (offset == -1 || !removeComponent(remove)
				|| !insertComponent(insert, offset)) {
			return false;
		}
		return true;
	}

	/**
	 * Sets the internal dependencies for all clock, reset, and go {@link Port
	 * Ports} of the children of this Block. Also creates the {@link Exit Exits}
	 * for this Block based upon the {@link Exit Exits} of the components in its
	 * sequence.
	 */
	protected void setControlDependencies(boolean isProcedureBody) {
		/*
		 * Assume the inputs to the Block have already been connected. Start on
		 * the inside with the InBuf. Use its clock, reset, and go Buses as the
		 * starting point.
		 */
		InBuf inBuf = getInBuf();
		Bus clockBus = inBuf.getClockBus();
		Bus resetBus = inBuf.getResetBus();
		Bus goBus = inBuf.getGoBus();

		/*
		 * Exit which drives the next Entry in the sequence of execution.
		 */
		Exit drivingExit = inBuf.getExit(Exit.DONE);

		/*
		 * Map of Exit.Tag to Collection of Exits for all component exits.
		 */
		Map<Exit.Tag, Collection<Exit>> exitMap = new LinkedHashMap<Tag, Collection<Exit>>(
				11);

		/*
		 * Visit each Component in the sequence.
		 */
		List<Component> components = getSequence();
		for (Component component : components) {

			/*
			 * Make a single entry for the Component, connecting it to the
			 * clock, reset, and go Buses.
			 */
			// System.out.println("Comp: "+component);
			assert component.getEntries().size() == 0 : "Not yet re-entrant";
			final Entry entry = component.makeEntry(drivingExit);
			addDependencies(entry, clockBus, resetBus, goBus);

			/*
			 * If the Component has multiple Exits or has no DONE Exit, then it
			 * represents a change in the flow of control. Special action is
			 * required!
			 */
			if ((component.getExits().size() > 1)
					|| (component.getExit(Exit.DONE) == null)) {
				/*
				 * First, make sure all predecessors have completed before
				 * allowing this component to execute.
				 */
				final Collection<Exit> doneExits = exitMap.remove(Exit
						.getTag(Exit.DONE));
				if (doneExits != null) {
					for (Exit doneExit : doneExits) {
						entry.addDependency(component.getGoPort(),
								new ControlDependency(doneExit.getDoneBus()));
					}
				}

				/*
				 * Then make sure that any successors will not execute unless
				 * the normal DONE Exit (if any) of this component is followed.
				 */
				final Exit doneExit = component.getExit(Exit.DONE);
				if (doneExit != null) {
					goBus = doneExit.getDoneBus();
				} else {
					/*
					 * If there is no DONE Exit, then there should be no
					 * successors in the normal flow of control.
					 */
					assert (components.indexOf(component) == components.size() - 1) : "not at the end of a block: "
							+ component;
				}
			}

			/*
			 * Record all the Exits of this Component by Tag.
			 */
			collectExits(component, exitMap);
			drivingExit = component.getExit(Exit.DONE);
		}

		/*
		 * Merge the non-DONE Exits of each Tag into a single Exit at the Block
		 * level.
		 */
		final Collection<Exit> doneExits = exitMap.remove(Exit
				.getTag(Exit.DONE));
		Collection<Exit> returnExits = null;
		Exit.Type mainExitType = Exit.DONE;
		if (isProcedureBody) {
			returnExits = exitMap.remove(Exit.getTag(Exit.RETURN));
			mainExitType = Exit.RETURN;
		}
		mergeExits(exitMap, clockBus, resetBus);

		/*
		 * If there is at least one element of the sequence that has a normal
		 * DONE Exit, then that is a possible normal exit from the Block. Make a
		 * DONE Exit that depends on all of these Exits.
		 */
		if (doneExits != null) {
			final Exit exit = makeExit(0, mainExitType);
			final Component outBuf = exit.getPeer();
			final Entry outBufEntry = outBuf.makeEntry(drivingExit);
			outBufEntry.addDependency(outBuf.getClockPort(),
					new ClockDependency(clockBus));
			outBufEntry.addDependency(outBuf.getResetPort(),
					new ResetDependency(resetBus));
			for (Exit doneExit : doneExits) {
				outBufEntry.addDependency(outBuf.getGoPort(),
						new ControlDependency(doneExit.getDoneBus()));
			}
		} else if (components.isEmpty()) {
			/*
			 * If the block is empty, connect the InBuf to the OutBuf.
			 */
			addDependencies(
					makeExit(0, mainExitType).getPeer().makeEntry(drivingExit),
					clockBus, resetBus, goBus);
		}

		/*
		 * If a procedure body has explicit RETURN Exits, merge them into the
		 * main Exit of the Block.
		 */
		if (isProcedureBody && (returnExits != null)) {
			exitMap = Collections.singletonMap(Exit.getTag(mainExitType),
					returnExits);
			mergeExits(exitMap, clockBus, resetBus);
		}
	}

	@Override
	protected void cloneNotify(Module moduleClone, Map cloneMap) {
		super.cloneNotify(moduleClone, cloneMap);
		final Block clone = (Block) moduleClone;
		clone.sequence = new LinkedList<Component>();
		for (Component component : sequence) {
			clone.sequence.add((Component) cloneMap.get(component));
		}
	}

}