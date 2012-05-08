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

package net.sf.openforge.optimize.loop;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.sf.openforge.app.EngineThread;
import net.sf.openforge.lim.Bus;
import net.sf.openforge.lim.Component;
import net.sf.openforge.lim.Dependency;
import net.sf.openforge.lim.Emulatable;
import net.sf.openforge.lim.Entry;
import net.sf.openforge.lim.Exit;
import net.sf.openforge.lim.InBuf;
import net.sf.openforge.lim.Loop;
import net.sf.openforge.lim.LoopBody;
import net.sf.openforge.lim.Module;
import net.sf.openforge.lim.OutBuf;
import net.sf.openforge.lim.Port;
import net.sf.openforge.lim.Reg;
import net.sf.openforge.lim.Value;
import net.sf.openforge.util.SizedInteger;

/**
 * A <code>DecisionCircuit</code> characterizes the iterating of a {@link Loop}
 * by constructing a representation of the logic circuit that drives the loop's
 * decision value. A static simulation is then performed (see {@link Emulatable}
 * to determine how many times up to a specified maximum the loop will iterate.
 * This information is needed when unrolling loops.
 * <P>
 * To use the class, simply create an instance and call
 * {@link DecisionCircuit#getIterationCount()}.
 * 
 * @version $Id: DecisionCircuit.java 60 2005-11-17 17:56:28Z imiller $
 */
class DecisionCircuit {
	/** Revision */

	/**
	 * If the number of iterations exceeds this, consider the loop unbounded.
	 * This number was determined empirically. It takes only a few seconds to
	 * emulate a tight increment loop this many times, and it's doubtful you'd
	 * want to unroll anything even close to this big anyway.
	 */
	private static final int MAX_ITERATIONS = 0xffff;

	/** The Loop to be characterized */
	private Loop loop;

	/** Ordered list of Components in the circuit */
	private LinkedList<Component> componentSequence = new LinkedList<Component>();

	/** Map of each traversed Port to its input Bus */
	private Map<Port, Bus> inputMap = new HashMap<Port, Bus>();

	/** Map of LoopBody Port to initial value SizedInteger */
	private Map<Port, SizedInteger> startPorts = new HashMap<Port, SizedInteger>();

	/** Set of Buses that provide the feedback values to the next iteration */
	private Set<Bus> endBuses = new HashSet<Bus>();

	/**
	 * Implementation of a simple queue of {@link Component} with a fast
	 * {@link #contains(Component)} method.
	 */
	private static class ComponentQueue {
		/** Queue-like behavior */
		private LinkedList<Component> queue = new LinkedList<Component>();

		/** Fast contains() behavior */
		private Set<Component> set = new HashSet<Component>();

		/**
		 * Adds a component to the end of this queue, if it is not already in
		 * the queue.
		 * 
		 * @param component
		 *            a component to add
		 */
		void add(Component component) {
			if (!contains(component)) {
				queue.add(component);
				set.add(component);
			}
		}

		/**
		 * Adds a collection of components to this queue. Only those that are
		 * not already in the queue are added.
		 * 
		 * @param collection
		 *            a collection of {@link Component}
		 */
		void addAll(Collection<Component> collection) {
			for (Component component : collection) {
				add(component);
			}
		}

		/**
		 * Removes and returns the first component in this queue.
		 * 
		 * @return the first component
		 */
		Component removeFirst() {
			final Component component = queue.removeFirst();
			set.remove(component);
			return component;
		}

		/**
		 * Tests this queue for emptiness.
		 * 
		 * @return a <code>true</code> if this queue is empty, false otherwise
		 */
		boolean isEmpty() {
			return queue.isEmpty();
		}

		/**
		 * Tests whether this queue contains a given component.
		 * 
		 * @param component
		 *            a component to test for
		 * @return true if this queue contains the component, false otherwise
		 */
		boolean contains(Component component) {
			return set.contains(component);
		}

		@Override
		public String toString() {
			return "DC_CQ: " + queue.toString();
		}
	}

	/**
	 * Creates a new <code>DecisionCircuit</code> for a given {@link Loop}.
	 * 
	 * @param loop
	 *            a <code>Loop</code> value
	 * @exception LoopUnrollingException
	 *                if the given <code>loop</code> is found not to be
	 *                unrollable
	 */
	DecisionCircuit(Loop loop) throws LoopUnrollingException {
		this.loop = loop;

		/*
		 * Cache the Loop's data feedback registers for quick comparison.
		 */
		final Set<Reg> dataRegisters = new HashSet<Reg>(loop.getDataRegisters());

		/*
		 * Record those data feedback registers that are encountered during
		 * traversal.
		 */
		final Set<Component> feedbackRegisters = new HashSet<Component>();

		/*
		 * Only traverse those InBuf/OutBuf Ports whose Buses are actually used.
		 */
		final Set<Port> peerPorts = new HashSet<Port>();

		/*
		 * Record the traversed components in two parts: those traversed from
		 * the decision test to the feedback registers, and then those from the
		 * feedback registers back to the decision test.
		 */
		final List<Component> preFeedbackSequence = new LinkedList<Component>();
		final List<Component> postFeedbackSequence = new LinkedList<Component>();
		boolean isPreFeedback = true;

		/*
		 * Use a queue to traverse backwards, starting with the decision test
		 * component.
		 */
		final ComponentQueue queue = new ComponentQueue();
		final Set<Component> completeSet = new HashSet<Component>();
		final Component testComponent = loop.getBody().getDecision()
				.getTestComponent();
		queue.add(testComponent);

		while (!queue.isEmpty() || isPreFeedback) {
			if (queue.isEmpty()) {
				/*
				 * After the queue is exhausted in the pre-feedback sequence,
				 * start again with the feedback data registers that were
				 * traversed.
				 */
				queue.addAll(feedbackRegisters);
				isPreFeedback = false;

				/*
				 * Traverse to the edge of the LoopBody again to catch any
				 * inputs that may not go through the Decision.
				 */
				completeSet.remove(loop.getBody().getInBuf());

				// If the loop is an infinite loop, then the feedback
				// regs may be empty, in which case we can fall
				// through here and still have an empty queue.
				if (queue.isEmpty()) {
					continue;
				}
			}

			final Component component = queue.removeFirst();

			if ((component != testComponent)
					&& !isReady(component, completeSet, queue)) {
				queue.add(component);
			} else if (!completeSet.contains(component)) {
				List<Port> dataPortList = null;
				if (dataRegisters.contains(component)) {
					if (isPreFeedback) {
						/*
						 * If we're still looking at components in the
						 * pre-feedback segment, just record the register.
						 */
						feedbackRegisters.add(component);
						continue;
					} else {
						/*
						 * Otherwise a Reg has only one data port to traverse.
						 * Also record its result Bus as an end point.
						 */
						final Reg reg = (Reg) component;
						dataPortList = Collections.singletonList(reg
								.getDataPort());
						endBuses.add(reg.getResultBus());
					}
				} else if (isInBuf(component)) {
					dataPortList = new ArrayList<Port>(component.getOwner()
							.getDataPorts());
					dataPortList.retainAll(peerPorts);
				} else if (isOutBuf(component)) {
					dataPortList = new ArrayList<Port>(component.getDataPorts());
					dataPortList.retainAll(peerPorts);
				} else {
					dataPortList = component.getDataPorts();
				}

				/*
				 * For each Port, record the Bus that provides its value and
				 * queue the Bus's owner for traversal.
				 */
				for (Port dataPort : dataPortList) {

					if (dataPort.isUsed()) {
						final Bus inputBus = getInputBus(dataPort, loop);
						inputMap.put(dataPort, inputBus);

						Component nextComponent = inputBus.getOwner()
								.getOwner();

						/*
						 * Not all peer Ports are traversed, so keep track of
						 * those that are.
						 */
						final Port peerPort = inputBus.getPeer();
						if (peerPort != null) {
							peerPorts.add(peerPort);
						}

						/*
						 * If the next component is not Emulatable, and we can't
						 * traverse inside it, then give up.
						 */
						if (!(nextComponent instanceof Emulatable)) {
							if (peerPort != null) {
								/*
								 * Make the next component the peer Port's
								 * owner, i.e., the OutBuf.
								 */
								nextComponent = peerPort.getOwner();
							} else {
								throw new LoopUnrollingException(
										"input not simulatable: "
												+ nextComponent + " in "
												+ nextComponent.getOwner());
							}
						}

						queue.add(nextComponent);
					}
				}

				/*
				 * Mark the component as visited and add it to the appropriate
				 * sequence.
				 */
				completeSet.add(component);
				(isPreFeedback ? preFeedbackSequence : postFeedbackSequence)
						.add(component);
			}
		}

		/*
		 * Merge and reverse the two lists so that the sequence starts at the
		 * top of the LoopBody and ends at the feedback Regs.
		 */
		Collections.reverse(preFeedbackSequence);
		componentSequence.addAll(preFeedbackSequence);
		Collections.reverse(postFeedbackSequence);
		componentSequence.addAll(postFeedbackSequence);
	}

	/**
	 * Gets the number iterations in which the loop's decision will evaluate to
	 * <code>true</code>. Note that this is not the same as the number of times
	 * that the loop's body will execute. For example, a <code>do</code>
	 * statement body will execute once even if the decision is never
	 * <code>true</code>
	 * 
	 * @return the number of iterations during which the decision test evaluates
	 *         to <code>true</code>; returns {@link Loop#ITERATIONS_UNKNOWN} if
	 *         the circuit cannot be simulated or if the number of iterations
	 *         exceeeds {@link #MAX_ITERATIONS}
	 * 
	 */
	int getIterationCount() {
		long iterations = 0;
		// boolean isDone = false;
		// final int limit = loop.getUnrollLimit();

		final Map<Port, SizedInteger> inputValues = new HashMap<Port, SizedInteger>(
				startPorts);
		try {
			while ((iterations <= MAX_ITERATIONS) && iterate(inputValues)) {
				iterations++;
			}
		} catch (Exception e) {
			EngineThread
					.getGenericJob()
					.warn("Loop decision circuit emulation terminated unexpectedly.  Loop implementation may be sub-optimal");
			return Loop.ITERATIONS_UNKNOWN;
		}

		return (int) (iterations <= MAX_ITERATIONS ? iterations
				: Loop.ITERATIONS_UNKNOWN);
	}

	/**
	 * Simulates a single iteration of the decision circuit logic with a given
	 * set of input values.
	 * 
	 * @param inputValues
	 *            a map of each relevant loop body {@link Port} to its starting
	 *            {@link SizedInteger} value; after execution, each port will be
	 *            mapped to its value for the start of the next iteration so
	 *            that this method should be called repeatedly with the same
	 *            argument
	 * 
	 * @return the boolean value of the loop's test expression during the
	 *         iteration
	 */
	private boolean iterate(Map<Port, SizedInteger> inputValues) {
		/*
		 * Prime the input bus value map with the given port values.
		 */
		boolean isDone = false;
		final Map<Bus, SizedInteger> busValues = new HashMap<Bus, SizedInteger>();
		for (Port port : inputValues.keySet()) {
			final Bus inputBus = inputMap.get(port);
			busValues.put(inputBus, inputValues.get(port));
		}

		/*
		 * Traverse each component in the circuit.
		 */
		for (Iterator<Component> iter = componentSequence.iterator(); iter
				.hasNext() && !isDone;) {
			final Component component = iter.next();

			/*
			 * Collect the input values for the component from its input buses.
			 */
			final HashMap<Port, SizedInteger> portValues = new HashMap<Port, SizedInteger>();
			List<Port> dataPorts = isInBuf(component) ? component.getOwner()
					.getDataPorts() : component.getDataPorts();
			for (Port dataPort : dataPorts) {
				final Bus dataBus = inputMap.get(dataPort);
				if (dataBus != null) {
					portValues.put(dataPort, busValues.get(dataBus));
				}
			}

			/*
			 * Simulate and store the output values for the next component.
			 */
			final Map<Bus, SizedInteger> outputValues = ((Emulatable) component)
					.emulate(portValues);
			busValues.putAll(outputValues);

			/*
			 * If this is the loop's boolean test expression, save its value.
			 */
			if (component == loop.getDecisionOp()) {
				final Bus testBus = component.getExit(Exit.DONE).getDataBuses()
						.get(0);
				final SizedInteger testValue = outputValues.get(testBus);
				isDone = testValue.numberValue().equals(BigInteger.ZERO);
			}
		}

		/*
		 * Save the output values as the input values for the next iteration.
		 */
		for (Port port : inputValues.keySet()) {
			inputValues.put(port, busValues.get(inputMap.get(port)));
		}

		return !isDone;
	}

	/**
	 * Gets the {@link Bus} that drives a given {@link Port}. It is expected
	 * that the port will have only one {@link Dependency} and that its owner
	 * will have only one {@link Entry} to query for dependencies (except in the
	 * case of a {@link LoopBody LoopBody's}, which is expected to have both an
	 * initialization and feedback entry; the feedback entry is traversed).
	 * 
	 * @param port
	 *            the port whose driving bus is to be determined
	 * @param loop
	 *            the loop being traversed
	 * @return the bus that provides the input value to <code>port</code>; if
	 *         the port is already connected, the bus will be the connected one;
	 *         otherwise the port's dependency will be used
	 * @exception LoopUnrollingException
	 *                if <code>port</code> does not have exactly one
	 *                {@link Dependency}, or a single {@link Entry} cannot be
	 *                determind for the port's owner
	 */
	private Bus getInputBus(Port port, Loop loop) throws LoopUnrollingException {
		final Collection<Dependency> deps = getInputEntry(port.getOwner(), loop)
				.getDependencies(port);

		/*
		 * Handle connections that were established manually rather than through
		 * dependency resolution.
		 */
		if (deps.isEmpty() && port.isConnected()) {
			return port.getBus();
		}

		if (deps.size() != 1) {
			throw new LoopUnrollingException("wrong dependency count: "
					+ deps.size() + " for " + port.getOwner() + " in "
					+ port.getOwner().getOwner());
		}
		final Dependency dep = deps.iterator().next();
		Bus inputBus = dep.getLogicalBus();

		recordStartingPort(port, loop);
		return inputBus;
	}

	/**
	 * If a given {@link Port} belongs to the {@link LoopBody} of the
	 * {@link Loop} being traversed, then record the {@link Port} and its
	 * initial constant value, if any. The value is obtained by looking at the
	 * {@link Dependency} for the loop body's initialization {@link Entry}.
	 * 
	 * @param port
	 *            a port being traversed
	 * @param loop
	 *            the loop being traversed
	 * 
	 * @exception LoopUnrollingException
	 *                if the port has too many dependencies, or if it does not
	 *                have a constant initial value
	 */
	private void recordStartingPort(Port port, Loop loop)
			throws LoopUnrollingException {
		if (port.getOwner() == loop.getBody()) {
			final Collection<Dependency> initDeps = loop.getBodyInitEntry()
					.getDependencies(port);
			if (initDeps.size() != 1) {
				throw new LoopUnrollingException(
						"wrong init dependency count: " + initDeps.size()
								+ " for " + port.getOwner());
			}

			final Dependency initDep = initDeps.iterator().next();
			final Bus initBus = initDep.getLogicalBus();
			final Value initValue = initBus.getValue();

			if (initValue == null) {
				throw new LoopUnrollingException("no initial value for "
						+ initBus);
			}

			if (!initValue.isConstant()) {
				throw new LoopUnrollingException(
						"non-constant inital value for bus of "
								+ initBus.getOwner().getOwner() + ": "
								+ initValue.debug());
			}

			startPorts.put(port, SizedInteger.valueOf(initValue.getValueMask()
					& initValue.getCareMask(), initValue.getSize(),
					initValue.isSigned()));
		}
	}

	/**
	 * For a given {@link Component}, determine the {@link Entry} which should
	 * be traversed. It is expected that all components other than the
	 * {@link LoopBody} will have exactly one entry, else the loop cannot be
	 * unrolled. For the {@link LoopBody}, the feedback entry will be traversed.
	 * 
	 * @param component
	 *            the component being traversed
	 * @param loop
	 *            the loop being traversed
	 * @return the entry of the <code>component</code> from which the inputs
	 *         should be taken
	 * @exception LoopUnrollingException
	 *                if a single entry cannot be found for the component
	 */
	private static Entry getInputEntry(Component component, Loop loop)
			throws LoopUnrollingException {
		Entry inputEntry = null;
		final Component entryOwner = isInBuf(component) ? component.getOwner()
				: component;
		if (entryOwner == loop.getBody()) {
			inputEntry = loop.getBodyFeedbackEntry();
		} else {
			final Collection<Entry> entries = entryOwner.getEntries();
			if (entries.size() != 1) {
				throw new LoopUnrollingException("wrong entry count: "
						+ entries.size() + " for " + entryOwner + " in "
						+ entryOwner.getLineage());
			}
			inputEntry = entries.iterator().next();
		}
		return inputEntry;
	}

	/**
	 * Tests whether a given component is its owner's {@link InBuf}.
	 */
	private static boolean isInBuf(Component component) {
		final Module owner = component.getOwner();
		return (owner != null) && (owner.getInBuf() == component);
	}

	/**
	 * Tests whether a given component is one of its owner's {@link OutBuf}.
	 */
	private static boolean isOutBuf(Component component) {
		final Module owner = component.getOwner();
		return (owner != null) && (owner.getOutBufs().contains(component));
	}

	/**
	 * Tests whether a given component is ready to be traversed.
	 * 
	 * @param component
	 *            the component to test
	 * @param completeSet
	 *            the set of components that have already been traversed
	 * @param queue
	 *            the queue of components to be traversed
	 * @return true if all dependents of the component that appear in the queue
	 *         also appear in the completed set; false otherwise
	 */
	private static boolean isReady(Component component,
			Set<Component> completeSet, ComponentQueue queue) {
		final List<Bus> dataBuses = (isOutBuf(component) ? ((OutBuf) component)
				.getPeer().getDataBuses() : component.getExit(Exit.DONE)
				.getDataBuses());

		for (Bus dataBus : dataBuses) {
			for (Dependency dependency : dataBus.getLogicalDependents()) {
				final Port dataPort = dependency.getPort();
				final Bus peerBus = dataPort.getPeer();
				final Component dependent = (peerBus == null ? dataPort
						.getOwner() : peerBus.getOwner().getOwner());

				if (queue.contains(dependent)
						&& !completeSet.contains(dependent)) {
					return false;
				}
			}
		}

		return true;
	}

}
