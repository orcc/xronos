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

package net.sf.openforge.report.throughput;

import java.io.PrintStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import net.sf.openforge.lim.Access;
import net.sf.openforge.lim.Bus;
import net.sf.openforge.lim.Call;
import net.sf.openforge.lim.Component;
import net.sf.openforge.lim.DataFlowVisitor;
import net.sf.openforge.lim.Design;
import net.sf.openforge.lim.Exit;
import net.sf.openforge.lim.HeapRead;
import net.sf.openforge.lim.Latency;
import net.sf.openforge.lim.Loop;
import net.sf.openforge.lim.Module;
import net.sf.openforge.lim.PinAccess;
import net.sf.openforge.lim.PinRead;
import net.sf.openforge.lim.PinStateChange;
import net.sf.openforge.lim.PinWrite;
import net.sf.openforge.lim.Referencer;
import net.sf.openforge.lim.RegisterRead;
import net.sf.openforge.lim.RegisterWrite;
import net.sf.openforge.lim.Resource;
import net.sf.openforge.lim.Task;
import net.sf.openforge.lim.Visitor;
import net.sf.openforge.lim.memory.LogicalMemoryPort;
import net.sf.openforge.lim.memory.MemoryRead;
import net.sf.openforge.lim.memory.MemoryWrite;
import net.sf.openforge.util.naming.ID;
import net.sf.openforge.util.naming.IDSourceInfo;

/**
 * ThroughputAnalyzer determines the minimum spacing between assertions of
 * GO/data to each task in order to prevent overlap of data processing within
 * the task. There are 3 considerations which factor into this spacing. These
 * are:
 * <ul>
 * <li>The task was balanced during scheduling
 * <li>The task contains loops
 * <li>The task contains multiple accesses to the same global resource.
 * </ul>
 * If the design is not balanced during scheduling, OR contains unbounded loops,
 * then the minimum spacing is to wait until the previous DONE. If the design
 * contains bounded loops and is balanced, the spacing is the number of loop
 * iterations times the number of cycles per iteration. If the design contains
 * multiple accesses to the same global resource the spacing is the number of
 * cycles between the first and last access to that resource.
 * 
 * <p>
 * <b>NOTE, to decrease the memory requirements for this analysis, it is
 * possible that the ThroughputAnalyzer visitor and the GlobalLatencyVisitor
 * could be combined into a single visitor, in which the latency of each bus is
 * calculated, then stored in a map based on which ports it connects to (ie
 * port->latency map). Then as each component is processed, remove the entry
 * from the map and throw it away. As each component is processed, we mark the
 * ones we are interested in. </b>
 * 
 * <p>
 * <b>NOTE. This will not currently work with dual ported memories, in which
 * both ports of the memory are accessed from within the same task, because they
 * will look like 2 different resources. This isn't an issue for now since the
 * only dual ported memories are ROM</b>
 * 
 * <p>
 * Created: Tue Jan 21 10:23:27 2003
 * 
 * @author imiller, last modified by $Author: imiller $
 * @version $Id: ThroughputAnalyzer.java 538 2007-11-21 06:22:39Z imiller $
 */
public class ThroughputAnalyzer extends DataFlowVisitor implements Visitor {

	/**
	 * These 4 maps are all non persistent. They are created and destroyed
	 * during processing of each Task.
	 */
	private Map<Bus, Latency> latencyMap = Collections.emptyMap();
	private Map<Component, Latency> inputLatencyMap = Collections.emptyMap();

	/** Loop/Resource -> ThroughputLimit */
	private Map<ID, ThroughputLimit> nodeToThroughputLimit = new HashMap<ID, ThroughputLimit>();
	/**
	 * Resource->ThroughputLimit for tracking limits imposed by the address port
	 * of a memory
	 */
	private Map<ID, ThroughputLimit> addrLimit = new HashMap<ID, ThroughputLimit>();

	/**
	 * Stack of Calls as we descend through the method call hierarchy we push
	 * onto the stack and pop off as we come back up. Used to identify where
	 * accesses occur.
	 */
	private Stack<Call> methodStack = new Stack<Call>();

	/**
	 * This is a Stack of loops as they are traversed, used to annotate resource
	 * accesses that occur in a loop so that, if the access is the first to a
	 * resource, then the loops entry latency is the latency of the access,
	 * otherwise the loops exit latency is the latency of the access.
	 */
	private Stack<Loop> loopStack = new Stack<Loop>();

	/**
	 * A persitent map of Task->Set where the Set is a collection of
	 * ThroughputLimit objects for that task. Used for reporting.
	 */
	private Map<Task, Set<ThroughputLimit>> taskToThroughputLimit = new HashMap<Task, Set<ThroughputLimit>>();

	private static final Resource PIN_RESOURCE = new DummyPinResource();

	private static class DummyPinResource extends Resource {
		@Override
		public int getSpacing(Referencer from, Referencer to) {
			return 0;
		}

		@Override
		public int getGoSpacing(Referencer from, Referencer to) {
			return -1;
		}

		@Override
		public Latency getLatency(Exit exit) {
			assert false;
			return null;
		}

		@Override
		public String toString() {
			return "DUMMY_PIN_RESOURCE";
		}
	}

	public ThroughputAnalyzer() {
		super();
		setRunForward(true);
	}

	/**
	 * Writes out a report of all identified paths for each resource encounted,
	 * on a per-task basis to the given PrintStream.
	 */
	public void writeReport(PrintStream ps) {
		for (Task task : taskToThroughputLimit.keySet()) {
			final Set<ThroughputLimit> limits = taskToThroughputLimit.get(task);

			// String name = task.showIDLogical();
			String name = task.getCall().showIDLogical();

			ps.println("Throughput analysis results for task: " + name);

			// int pathCount = 0;
			for (ThroughputLimit limiter : limits) {
				limiter.writeReport(ps, 0);
			}
			ps.println();
		}
	}

	@Override
	public void visit(Design design) {
		if (_throughput.db)
			_throughput.d.launchXGraph(design, false);
		super.visit(design);
	}

	@Override
	public void visit(Task task) {
		if (!task.isBalanced()) {
			Latency callLatency = task.getCall().getLatency();
			if (callLatency.getMaxClocks() == Latency.UNKNOWN)
				task.setGoSpacing(Task.INDETERMINATE_GO_SPACING);
			else
				task.setGoSpacing(callLatency.getMaxClocks());
			return;
		}

		//
		// Identify the latency of each bus in the LIM as an absolute
		// value from the initial GO so we can calculate the
		// difference between the first and last access to resources.
		//
		final GlobalLatencyVisitor glv = new GlobalLatencyVisitor();
		task.accept(glv);
		latencyMap = glv.getLatencyMap();
		inputLatencyMap = glv.getInputLatencyMap();
		methodStack = new Stack<Call>();
		nodeToThroughputLimit = new HashMap<ID, ThroughputLimit>();
		addrLimit = new HashMap<ID, ThroughputLimit>();

		super.visit(task);

		//
		// Clear out the maps to save memory.
		//
		latencyMap = Collections.emptyMap();
		inputLatencyMap = Collections.emptyMap();
		methodStack = null;
		Set<ThroughputLimit> limitSet = new HashSet<ThroughputLimit>(
				nodeToThroughputLimit.values());
		limitSet.addAll(addrLimit.values());
		taskToThroughputLimit.put(task, limitSet);
		nodeToThroughputLimit = Collections.emptyMap();
		addrLimit = Collections.emptyMap();

		//
		// Start at 0 since the 'no restrictions' case means we can
		// assert data every clock cycle.
		//
		if (_throughput.db)
			_throughput.d.ln(_throughput.TA, "Analyzing paths:");
		int longestPath = 0;
		for (ThroughputLimit paths : limitSet) {
			if (_throughput.db)
				_throughput.d.ln(_throughput.TA, "\t" + paths + " limit: "
						+ paths.getLimit());
			final int limit = paths.getLimit();
			if (limit < 0) {
				longestPath = Task.INDETERMINATE_GO_SPACING;
				break;
			}
			longestPath = Math.max(longestPath, paths.getLimit());
		}
		task.setGoSpacing(longestPath);
	}

	@Override
	public void visit(Call call) {
		methodStack.push(call);
		markCall(call);
		super.visit(call);
		methodStack.pop();
	}

	@Override
	public void visit(MemoryRead comp) {
		markMemAccess(comp);

		// Since the array length cannot change then it is not a
		// limitation on the throughput because no one can change it.
		Module owner = comp.getOwner();
		if (owner instanceof HeapRead && ((HeapRead) owner).isArrayLengthRead()) {
		} else {
			markResource(comp);
		}
		super.visit(comp);
	}

	@Override
	public void visit(MemoryWrite comp) {
		markMemAccess(comp);
		markResource(comp);
		super.visit(comp);
	}

	@Override
	public void visit(PinRead comp) {
		markResource(comp);
		super.visit(comp);
	}

	@Override
	public void visit(PinWrite comp) {
		markResource(comp);
		super.visit(comp);
	}

	@Override
	public void visit(PinStateChange comp) {
		markResource(comp);
		super.visit(comp);
	}

	@Override
	public void visit(RegisterRead comp) {
		markResource(comp);
		super.visit(comp);
	}

	@Override
	public void visit(RegisterWrite comp) {
		markResource(comp);
		super.visit(comp);
	}

	/**
	 * The loop only represents a limitation if it is iterative.
	 */
	@Override
	public void visit(Loop loop) {
		if (loop.isIterative()) {
			if (_throughput.db)
				_throughput.d.ln(_throughput.TA, "Pushing loop " + loop);
			loopStack.push(loop);
			markLoop(loop);

			super.visit(loop);

			Object o = loopStack.pop();
			if (_throughput.db)
				_throughput.d.ln(_throughput.TA, "Popped loop " + o);
			assert loop == o;
		} else {
			super.visit(loop);
		}
	}

	@Override
	protected void preFilterAny(Component comp) {
		super.preFilterAny(comp);
		// System.out.println("TA: " + comp);
	}

	//
	// A shared procedure call does not impose the same type of access
	// restrictions on throughput as does any of the other globally
	// accessing {@link Access Accesses}, however we do not currently
	// use the shared procedure call, so this visit throws an
	// assertion error.
	//
	// public void visit (SharedProcedureCall comp)
	// {
	// assert false :
	// "SharedProcedureCall not supported in Throughput Analysis";
	// markResource(comp);
	// super.visit(comp);
	// }

	/**
	 * Method calls have throughput limit set by an user via api method.
	 * 
	 * @param call
	 *            a value of type 'Call'
	 */
	private void markCall(Call call) {
		nodeToThroughputLimit.put(call, new ThroughputLocalLimit(call));
	}

	/**
	 * Loops impose a limitation on the throughput of a task because they must
	 * iterate to completion before new data can be applied. Thus their
	 * limitation is the max clocks of their done exit.
	 * 
	 * @param loop
	 *            a value of type 'Loop'
	 */
	private void markLoop(Loop loop) {
		assert !nodeToThroughputLimit.containsKey(loop) : "Duplicate traversal of loop "
				+ loop;
		nodeToThroughputLimit
				.put(loop, new LoopLimit(loop, methodStack.peek()));
	}

	/**
	 * Memory accesses need to be marked in addition to the standard resource
	 * marking because their use of the memory address port imposes an
	 * additional limitation on the throughput. The standard markResource is
	 * only concerned with complementary accesses, ie the first read to last
	 * write or the first write to the last read. However for the address port
	 * of memories we need to track the first access to the last access, which
	 * may be two readers or two writers. See note in {@link MemAddrLimit}.
	 * 
	 * @param access
	 *            an {@link Access} whose resource is a {@link MemoryPort}
	 */
	private void markMemAccess(Access access) {
		LogicalMemoryPort resource = (LogicalMemoryPort) access.getResource();
		MemAddrLimit memLimit = (MemAddrLimit) addrLimit.get(resource);
		if (memLimit == null) {
			memLimit = new MemAddrLimit(resource, access, getHeadLatency(
					access, resource), methodStack.peek());
			addrLimit.put(resource, memLimit);
		} else {
			memLimit.mark(access, getTailLatency(access), methodStack.peek());
		}
	}

	/**
	 * Marks the resource based on complementary accesses which will account for
	 * the first and last accesses that modify/consume the data value
	 * represented by the resource. Note that memories constitute a single
	 * resource, thus any access may modify any location.
	 * <p>
	 * <b>NOTE: We could be more intelligent in the case of head read/write
	 * accesses and try to figure out which locations may be accessed which
	 * could potentially improve out throughput calculation</b>
	 */
	private void markResource(Access access) {
		Resource resource = access.getResource();
		if (resource == null && access instanceof PinAccess) {
			resource = PIN_RESOURCE;
		}

		ResourcePaths paths = (ResourcePaths) nodeToThroughputLimit
				.get(resource);
		if (paths == null) {
			paths = new ResourcePaths(resource);
			nodeToThroughputLimit.put(resource, paths);
		}

		// The latency to use if this access is the start of a new chain.
		Latency headLatency = getHeadLatency(access, resource);
		// The latency to use if this access it the tail of a chain.
		Latency tailLatency = getTailLatency(access);

		paths.mark(access, headLatency, tailLatency, methodStack.peek());

		if (_throughput.db)
			_throughput.d
					.ln(_throughput.TA, "Marked resource " + resource + "("
							+ resource.showIDLogical() + ") access " + access
							+ " headlatency " + headLatency + " taillatency "
							+ tailLatency + " in "
							+ methodStack.peek().showIDLogical());
		// IDSourceInfo info = access.getIDSourceInfo();
		IDSourceInfo info = resource.getIDSourceInfo();
		if (_throughput.db)
			_throughput.d.ln(
					_throughput.TA,
					"\taccess:: pkg: " + info.getSourcePackageName()
							+ " file: " + info.getSourceFileName() + " line "
							+ info.getSourceLine() + " class "
							+ info.getSourceClassName());
	}

	/**
	 * Gets the latency to use if this access is the head of a chain of
	 * accesses.
	 */
	private Latency getHeadLatency(Access access, Resource resource) {
		final Latency headLatency;
		if (_throughput.db)
			_throughput.d.ln(_throughput.TA, "Finding head latency for "
					+ access + " to " + resource);
		if (_throughput.db)
			_throughput.d.ln(_throughput.TA, "\tLoopStack.isEmpty(): "
					+ loopStack.isEmpty());
		if (loopStack.isEmpty()) {
			//
			// If the accessed resource does not allow parallel reads,
			// then that indicates there is a contention on its ports
			// such that 2 things cannot access it simultaneously
			// (like the memory address port). In that case we need
			// to use the INPUT latency of the first access to ensure
			// that the 2 accesses do not occur simultaneously. If
			// there is no contention, then it is OK for the first and
			// last access to occur in the same cycle and so we use
			// the DONE of the first access
			//
			// if (resource.allowsParallelReads())
			// {
			// Bus doneBus = access.getExit(Exit.DONE).getDoneBus();
			// headLatency = (Latency)this.latencyMap.get(doneBus);
			// }
			// else
			// {
			headLatency = inputLatencyMap.get(access);
			// }
		} else {
			Object key = loopStack.firstElement();
			if (_throughput.db)
				_throughput.d.ln(_throughput.TA, "\tUsing input latency of: "
						+ key);
			headLatency = inputLatencyMap.get(key);
		}
		if (_throughput.db)
			_throughput.d.ln(_throughput.TA, "\thead latency: " + headLatency);
		assert headLatency != null;
		return headLatency;
	}

	/**
	 * Gets the latency to use if this access is the tail of a chain of
	 * accesses.
	 */
	private Latency getTailLatency(Access access) {
		final Latency tailLatency;
		if (_throughput.db)
			_throughput.d.ln(_throughput.TA, "Finding tail latency for "
					+ access);
		if (_throughput.db)
			_throughput.d.ln(_throughput.TA, "\tLoopStack.isEmpty(): "
					+ loopStack.isEmpty());
		if (loopStack.isEmpty()) {
			tailLatency = inputLatencyMap.get(access);
		} else {
			Loop loop = loopStack.firstElement();
			if (_throughput.db)
				_throughput.d.ln(_throughput.TA, "\tUsing output latency of: "
						+ loop);
			tailLatency = latencyMap.get(loop.getExit(Exit.DONE).getDoneBus());
		}
		assert tailLatency != null;
		if (_throughput.db)
			_throughput.d.ln(_throughput.TA, "\ttail latency: " + tailLatency);
		return tailLatency;
	}

}// ThroughputAnalyzer

