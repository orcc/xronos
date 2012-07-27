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

package net.sf.openforge.schedule;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.sf.openforge.app.EngineThread;
import net.sf.openforge.app.GenericJob;
import net.sf.openforge.app.OptionRegistry;
import net.sf.openforge.lim.ArrayRead;
import net.sf.openforge.lim.ArrayWrite;
import net.sf.openforge.lim.Block;
import net.sf.openforge.lim.Branch;
import net.sf.openforge.lim.Bus;
import net.sf.openforge.lim.Call;
import net.sf.openforge.lim.Component;
import net.sf.openforge.lim.ControlDependency;
import net.sf.openforge.lim.Decision;
import net.sf.openforge.lim.DefaultVisitor;
import net.sf.openforge.lim.Dependency;
import net.sf.openforge.lim.Entry;
import net.sf.openforge.lim.Exit;
import net.sf.openforge.lim.ForBody;
import net.sf.openforge.lim.HeapRead;
import net.sf.openforge.lim.HeapWrite;
import net.sf.openforge.lim.InBuf;
import net.sf.openforge.lim.Latch;
import net.sf.openforge.lim.Latency;
import net.sf.openforge.lim.LatencyKey;
import net.sf.openforge.lim.Loop;
import net.sf.openforge.lim.LoopBody;
import net.sf.openforge.lim.Module;
import net.sf.openforge.lim.OutBuf;
import net.sf.openforge.lim.Port;
import net.sf.openforge.lim.Stallboard;
import net.sf.openforge.lim.Switch;
import net.sf.openforge.lim.Task;
import net.sf.openforge.lim.TaskCall;
import net.sf.openforge.lim.UntilBody;
import net.sf.openforge.lim.Visitable;
import net.sf.openforge.lim.WhileBody;
import net.sf.openforge.lim.io.FifoAccess;
import net.sf.openforge.lim.io.FifoRead;
import net.sf.openforge.lim.io.FifoWrite;
import net.sf.openforge.lim.io.SimplePinAccess;
import net.sf.openforge.lim.memory.AbsoluteMemoryRead;
import net.sf.openforge.lim.memory.AbsoluteMemoryWrite;
import net.sf.openforge.lim.op.Constant;
import net.sf.openforge.lim.op.OrOp;
import net.sf.openforge.lim.op.SimpleConstant;
import net.sf.openforge.lim.op.TimingOp;
import net.sf.openforge.lim.primitive.And;
import net.sf.openforge.lim.primitive.Reg;
import net.sf.openforge.lim.util.OrderFinder;
import net.sf.openforge.schedule.block.MemProcess;
import net.sf.openforge.schedule.block.ModuleStallSource;
import net.sf.openforge.schedule.block.ProcessCache;
import net.sf.openforge.schedule.block.ProcessStartPoint;
import net.sf.openforge.schedule.loop.LoopFlopAnalysis;
import net.sf.openforge.util.naming.ID;

/**
 * ScheduleVisitor performs the actual work of scheduling the internal LIM of
 * each {@link Module} based upon the properties of its {@link Component
 * Components}.
 * 
 * @version $Id: ScheduleVisitor.java 562 2008-03-17 21:33:12Z imiller $
 */
public class ScheduleVisitor extends DefaultVisitor {

	/** True if balanced scheduling should be attempted */
	private final boolean isBalancing;

	/**
	 * True if go and done signals should be generated regardless of the
	 * attributes of the design.
	 */
	private final boolean isForcingDone;

	private final TaskCache taskCache = new TaskCache();

	/** True if the design has been found capable of being balanced */
	private boolean isBalanceable = false;

	private final LatencyTracker tracker = new LatencyTracker();

	/**
	 * The process cache, used to store the current
	 */
	private final ProcessCache processCache;

	public static ScheduleVisitor schedule(Visitable vis) {
		final GenericJob gj = EngineThread.getEngine().getGenericJob();
		final ProcessCache processCache = new ProcessCache(vis, gj);
		final boolean isForcingDone = gj
				.getUnscopedBooleanOptionValue(OptionRegistry.FORCE_GO_DONE);
		boolean isBalancing = gj
				.getUnscopedBooleanOptionValue(OptionRegistry.SCHEDULE_BALANCE);
		final boolean noBlockSched = gj
				.getUnscopedBooleanOptionValue(OptionRegistry.SCHEDULE_NO_BLOCK_SCHEDULING);
		if (isBalancing && !noBlockSched) {
			isBalancing = false;
			gj.warn("Balanced Scheduling not possible: Block based scheduling infers feedback points in implementation.");
			gj.warn("\t... Reverting to non-balanced (block based) scheduling");
		}
		final ScheduleVisitor scheduler = new ScheduleVisitor(isBalancing,
				isForcingDone, processCache);
		vis.accept(scheduler);
		return scheduler;
	}

	private ScheduleVisitor(boolean isBalancing, boolean isForcingDone,
			ProcessCache procCache) {
		super();
		this.isBalancing = isBalancing;
		this.isForcingDone = isForcingDone;
		processCache = procCache;
	}

	public LatencyCache getLatencyCache() {
		return tracker;
	}

	@Override
	public void visit(Task task) {
		taskCache.startTask(task);

		final boolean oldBalanceable = isBalanceable;

		isBalanceable = task.getCall().isBalanceable();
		super.visit(task);

		final Call call = task.getCall();
		call.getClockPort().setUsed(call.consumesClock());
		call.getResetPort().setUsed(call.consumesReset());
		call.getGoPort().setUsed(call.consumesGo());
		for (Exit exit : call.getExits()) {
			exit.getDoneBus().setUsed(call.producesDone());
		}

		task.setBalanced(isBalancing && isBalanceable);

		taskCache.completeTask(task);

		isBalanceable = oldBalanceable;
	}

	@Override
	public void visit(Block block) {
		visitComponents(block);

		scheduleInBuf(block);
		final List<Component> components = new LinkedList<Component>(
				block.getSequence());
		// int size = components.size();
		for (Component component : components) {
			schedule(component);
		}
		scheduleOutBufs(block);

		closeModule(block);
	}

	@Override
	public void visit(SimplePinAccess mod) {
		// Doing nothing means that the SimplePinAccess will get scheduled
		// as a component due to the visit of the block that contains
		// it. By not calling the super.visit() we will not descend
		// into the Module. This is what we want because the Module
		// has been hand-constructed.

		propagateSchedulingAttributes(mod);
	}

	@Override
	public void visit(TaskCall mod) {
		// If the target task has not been scheduled then jump out
		// here and schedule it so that the scheduling attributes of
		// the task call can be correctly set.
		if (!taskCache.isScheduled(mod.getTarget())) {
			mod.getTarget().accept(this);
		}

		// Doing nothing means that the TaskCall will get scheduled
		// as a component due to the visit of the block that contains
		// it. By not calling the super.visit() we will not descend
		// into the Module. This is what we want because the Module
		// has been hand-constructed.
		propagateSchedulingAttributes(mod);
	}

	@Override
	public void visit(FifoAccess mod) {
		// Doing nothing means that the Fifo access will get scheduled
		// as a component due to the visit of the block that contains
		// it. By not calling the super.visit() we will not descend
		// into the Module. This is what we want because the Module
		// has been hand-constructed.

		propagateSchedulingAttributes(mod);
	}

	@Override
	public void visit(FifoRead mod) {
		// Doing nothing means that the Fifo access will get scheduled
		// as a component due to the visit of the block that contains
		// it. By not calling the super.visit() we will not descend
		// into the Module. This is what we want because the Module
		// has been hand-constructed.
		propagateSchedulingAttributes(mod);
	}

	@Override
	public void visit(FifoWrite mod) {
		// Doing nothing means that the Fifo access will get scheduled
		// as a component due to the visit of the block that contains
		// it. By not calling the super.visit() we will not descend
		// into the Module. This is what we want because the Module
		// has been hand-constructed.
		propagateSchedulingAttributes(mod);
	}

	@Override
	public void visit(Branch branch) {
		visitComponents(branch);

		scheduleInBuf(branch);
		schedule(branch.getDecision());
		schedule(branch.getTrueBranch());
		schedule(branch.getFalseBranch());
		scheduleOutBufs(branch);

		closeModule(branch);
	}

	@Override
	public void visit(Decision decision) {
		decision.setConsumesGo(true);

		visitComponents(decision);

		final List<ID> decisionComponents = OrderFinder.getOrder(decision);
		decisionComponents.remove(decision.getInBuf());
		decisionComponents.remove(decision.getOutBufs());

		scheduleInBuf(decision);
		scheduleComponents(decisionComponents);

		final And trueAnd = decision.getTrueAnd();
		trueAnd.getDataPorts().get(0)
				.setBus(tracker.getControlBus(trueAnd.getExit(Exit.DONE)));
		tracker.defineControlBus(trueAnd.getResultBus(),
				tracker.getLatency(trueAnd.getExit(Exit.DONE)));

		final And falseAnd = decision.getFalseAnd();
		falseAnd.getDataPorts().get(0)
				.setBus(tracker.getControlBus(falseAnd.getExit(Exit.DONE)));
		tracker.defineControlBus(falseAnd.getResultBus(),
				tracker.getLatency(falseAnd.getExit(Exit.DONE)));

		/*
		 * Schedule the OutBufs normally using their Dependencies.
		 */
		scheduleOutBufs(decision);

		/*
		 * Regardless of what else scheduling did, force a connection from the
		 * true/false OutBuf go Ports to the boolean data Buses that supply
		 * their signals.
		 */
		final Bus trueBus = trueAnd.getResultBus();
		final Component trueOutbuf = decision.getTrueExit().getPeer();
		trueOutbuf.getGoPort().setBus(trueBus);
		tracker.setControlBus(trueOutbuf, trueBus);

		final Bus falseBus = falseAnd.getResultBus();
		final Component falseOutbuf = decision.getFalseExit().getPeer();
		falseOutbuf.getGoPort().setBus(falseBus);
		tracker.setControlBus(falseOutbuf, falseBus);

		closeModule(decision);
	}

	@Override
	public void visit(Loop loop) {
		fixLoopDataRegisters(loop);

		final List<ID> loopComponents = OrderFinder.getOrder(loop);
		loopComponents.remove(loop.getInBuf());
		loopComponents.removeAll(loop.getOutBufs());

		visitComponents(loop); // Sub-schedule

		// Initialize the latency of the feedback flop. The
		// feedback flop is the control signal for all feedback
		// data. Because its latency actually depends on the
		// running of the loop we must pre-initialize its latency.
		if (loop.getControlRegister() != null) {
			tracker.defineControlBus(loop.getControlRegister().getResultBus(),
					Latency.ZERO);
		}

		scheduleInBuf(loop);

		// Schedule the components of the loop
		for (Iterator<ID> iter = loopComponents.iterator(); iter.hasNext();) {
			final Component nextComp = (Component) iter.next();

			// Set the control register latency to be the latest of
			// any input latency to the loop body. This is necessary
			// b/c (eg) a latched input may have pipeline register(s)
			// inserted before it. Thus they will be 'later'. Unless
			// the control reg is at least as late as these then it
			// will NOT be the controlling input of the feedback entry
			// and the loop will not work
			if (nextComp == loop.getBody() && loop.getControlRegister() != null) {
				final Map<ID, Latency> latencyMap = new HashMap<ID, Latency>();
				for (Entry entry : loop.getBody().getEntries()) {
					for (Port port : nextComp.getPorts()) {
						for (Iterator<Dependency> depIter = entry
								.getDependencies(port).iterator(); depIter
								.hasNext();) {
							final Latency lat = tracker.getLatency(depIter
									.next().getLogicalBus());
							// may be null if a data register input
							if (lat != null) {
								latencyMap.put(port, lat);
							}
						}
					}
				}
				assert latencyMap.size() > 0 : "No latencies to initialize loop feedback control";
				final Map<Object, Latency> latest = Latency
						.getLatest(latencyMap);
				assert latest.size() == 1 : "Loop has unknown latencies leading to body";
				tracker.defineControlBus(loop.getControlRegister()
						.getResultBus(), latest.values().iterator().next());
				// Now that the latency of the control register is
				// set, set the control register to be the control bus
				// for the data register exits.
				for (Reg register : loop.getDataRegisters()) {
					tracker.setControlBus(register.getExit(Exit.DONE), loop
							.getControlRegister().getResultBus());
				}
			}

			schedule(nextComp);
		}

		if (loop.getBody() != null && loop.getBody().getFeedbackExit() != null
				&& loop.getControlRegister() != null
				&& !loop.getBody().isLoopFlopNeeded()) {
			optimizeLoopFlop(loop);
		}

		scheduleOutBufs(loop);

		fixLoopLatency(loop);

		closeModule(loop);
	}

	private void fixLoopDataRegisters(Loop loop) {
		// FIXME! This should not have to be here...
		// The enable port of the feedback registers have no
		// dependency. Create a control dependency based on the data
		// port. There is a danger if this is done any earlier b/c a
		// pipeline register may have been inserted on the data path.
		// b/c the enable port is NOT the go port we could end up with
		// synchronization problems. By doing it here, we will get
		// the enable dependency correctly reflecting the data path
		// regardless of what optimizations have been done.
		for (Reg reg : loop.getDataRegisters()) {
			assert reg.getEntries().size() == 1;
			final Entry entry = reg.getEntries().get(0);
			assert entry.getDependencies(reg.getDataPort()).size() == 1;
			final Dependency dataDep = entry.getDependencies(reg.getDataPort())
					.iterator().next();
			assert entry.getDependencies(reg.getEnablePort()).size() == 0;
			entry.addDependency(reg.getEnablePort(), new ControlDependency(
					dataDep.getLogicalBus()));
		}
	}

	/**
	 * Special handling of setting the latency of the Loop outbuf latency. This
	 * method sets the output latency to be a bounded latency where the bounds
	 * are min clocks to max clocks. This is determined by multiplying the
	 * number of iterations (if known) by the min and max clocks that the loop
	 * body takes.
	 * 
	 * @param loop
	 *            a value of type 'Loop'
	 */
	private void fixLoopLatency(Loop loop) {
		final LoopBody body = loop.getBody();
		for (OutBuf outbuf : loop.getOutBufs()) {

			/*
			 * If this is an iterative loop, redefine the Latency of the
			 * OutBufs' control Buses to be open.
			 */
			if (body != null && body.getFeedbackExit() != null) {
				final Bus controlBus = tracker.getControlBus(outbuf);
				Latency controlLatency = tracker.getLatency(controlBus);
				// final Latency old = controlLatency;

				//
				// Calculate the minimum and maximum number of clock
				// cycles that the loop may take to complete. This is
				// accomplished by figuing out the maximum and minimum
				// number of cycles that the loop body, update
				// expression, and decision take to complete (decision
				// may take more than 0 cycles if it contains a mul,
				// div, or rem that is replaced with an iterative
				// version). These min/max cycles are multiplied by
				// the number of iterations of the loop to give the
				// range of latencies the loop may take. If there is
				// a break or return, then the latency of those exits
				// is 'ored' into the calculated latency as an
				// alternative 'exit' of the loop. This gives us the
				// final bounds for the Latency of the loop. If the
				// loop does not iterate a known number of times, then
				// we default back to how we've always calculated the
				// loops latency (controlLatency.open()).
				//
				// if (debug)
				// System.out.println("\tdecision on how to handle: iter " +
				// loop.isIterative() + " bounded " + loop.isBounded() +
				// " body " + body.getBody() + " body latency " +
				// ((body.getBody() != null) ?
				// body.getBody().getLatency().toString():"null") +
				// " control latency " + controlLatency);
				if (loop.isIterative() && loop.isBounded()
						&& body.getBody() != null
						&& !body.getBody().getLatency().isOpen()
						&& !controlLatency.isOpen()) {
					final int iterations = loop.getIterations();

					Latency fbLatency = body.getFeedbackExit().getLatency();
					// Add one for the loop flop if loop flop is still here.
					if (loop.getControlRegister() != null) {
						fbLatency = fbLatency.addTo(Latency.ONE);
					}

					final Set<Latency> allLatencies = new HashSet<Latency>();
					allLatencies.add(fbLatency);
					for (Exit exit : body.getBody().getExits()) {
						if (exit == body.getBody().getExit(Exit.DONE)) {
							// The fb exit has the done factored in,
							// along with the decision latency.
							continue;
						}
						allLatencies.add(exit.getLatency());
					}
					// The key is irrelevant due to the short lifetime
					// and limited use of this latency.
					Latency bodyLatency = Latency.or(allLatencies,
							LatencyKey.BASE);

					final int minClocks = bodyLatency.getMinClocks()
							* iterations;
					final int maxClocks = bodyLatency.getMaxClocks()
							* iterations;
					final Latency boundedLatency = Latency.get(minClocks,
							maxClocks);
					controlLatency = boundedLatency.addTo(controlLatency);
				} else {
					controlLatency = Latency.get(controlLatency.getMinClocks())
							.open(controlBus.getOwner());
				}
				tracker.defineControlBus(controlBus, controlLatency);
			}
		}
	}

	/*
	 * Schedule a List of components
	 * 
	 * @param componentList list of components to be scheduled
	 * 
	 * - gandhij
	 */
	private void scheduleComponents(List<ID> componentList) {
		Iterator<ID> iter = componentList.iterator();
		while (iter.hasNext()) {
			Component comp = (Component) iter.next();
			schedule(comp);
		}
	}

	@Override
	public void visit(WhileBody whileBody) {
		visitComponents(whileBody);

		/* get all the components in the forBody in dataFlow order */
		List<ID> whileBodyComponents = OrderFinder.getOrder(whileBody);

		/* remove inbufs and outbufs from the list */
		whileBodyComponents.remove(whileBody.getInBuf());
		whileBodyComponents.removeAll(whileBody.getOutBufs());

		scheduleInBuf(whileBody);
		scheduleComponents(whileBodyComponents);
		scheduleOutBufs(whileBody);

		closeModule(whileBody);
		LoopFlopAnalysis.setLoopFlopStatus(whileBody, tracker);
	}

	@Override
	public void visit(UntilBody untilBody) {
		visitComponents(untilBody);

		/* get all the components in the forBody in dataFlow order */
		List<ID> untilBodyComponents = OrderFinder.getOrder(untilBody);

		/* remove inbufs and outbufs from the list */
		untilBodyComponents.remove(untilBody.getInBuf());
		untilBodyComponents.removeAll(untilBody.getOutBufs());

		scheduleInBuf(untilBody);
		scheduleComponents(untilBodyComponents);
		if ((untilBody.getDecision() != null) && (untilBody.getBody() != null)) {
			untilBody
					.getDecision()
					.getGoPort()
					.setBus(tracker.getControlBus(untilBody.getBody().getExit(
							Exit.DONE)));
		}
		scheduleOutBufs(untilBody);

		closeModule(untilBody);

		LoopFlopAnalysis.setLoopFlopStatus(untilBody, tracker);
	}

	@Override
	public void visit(ForBody forBody) {

		visitComponents(forBody);

		/* get all the components in the forBody in dataFlow order */
		List<ID> forBodyComponents = OrderFinder.getOrder(forBody);

		/* remove inbufs and outbufs from the list */
		forBodyComponents.remove(forBody.getInBuf());
		forBodyComponents.removeAll(forBody.getOutBufs());

		scheduleInBuf(forBody);
		scheduleComponents(forBodyComponents);
		scheduleOutBufs(forBody);
		closeModule(forBody);

		LoopFlopAnalysis.setLoopFlopStatus(forBody, tracker);
	}

	/**
	 * Schedules the switch statment, scheduling the inbuf, switch controller,
	 * all other components and lastly the outbufs.
	 */
	@Override
	public void visit(Switch swich) {
		visit((Block) swich);
	}

	@Override
	public void visit(Call call) {
		/*
		 * Schedule the called Procedure with its own LatencyTracker.
		 */
		// final LatencyTracker outerTracker = tracker;
		// tracker = new LatencyTracker();
		call.getProcedure().accept(this);
		// tracker = outerTracker;

		/*
		 * Propagate the Procedure's Exit Latencies to the Call's Exits.
		 */
		for (Exit callExit : call.getExits()) {
			final Exit procExit = call.getProcedureExit(callExit);
			Latency latency = procExit.getLatency();
			if (latency.isOpen()) {
				/*
				 * If the Procedure latency is open, create a new latency for
				 * the Call that does not have a reference to a Procedure Bus.
				 */
				latency = Latency.get(latency.getMinClocks()).open(callExit);
			}
			callExit.setLatency(latency);
		}

		final Block procBody = call.getProcedure().getBody();
		if (procBody.consumesClock()) {
			procBody.getClockPort().setUsed(true);
		}
		if (procBody.consumesReset()) {
			procBody.getResetPort().setUsed(true);
		}
		if (procBody.consumesGo()) {
			procBody.getGoPort().setUsed(true);
		}
		if (procBody.producesDone()) {
			for (Exit exit : procBody.getExits()) {
				exit.getDoneBus().setUsed(true);
			}
		}
	}

	@Override
	public void visit(HeapWrite comp) {
		visit((Block) comp);
	}

	@Override
	public void visit(HeapRead comp) {
		visit((Block) comp);
	}

	@Override
	public void visit(ArrayWrite comp) {
		visit((Block) comp);
	}

	@Override
	public void visit(ArrayRead comp) {
		visit((Block) comp);
	}

	@Override
	public void visit(AbsoluteMemoryWrite comp) {
		visit((Block) comp);
	}

	@Override
	public void visit(AbsoluteMemoryRead comp) {
		visit((Block) comp);
	}

	@Override
	public void visit(InBuf inbuf) {
	}

	@Override
	public void visit(OutBuf outbuf) {
	}

	/**
	 * Schedules the inputs to a single {@link Component} and derives the
	 * {@link ControlState ControlStates} of its {@link Exit Exits}.
	 * 
	 * @param component
	 *            the component to be scheduled
	 * @param tracker
	 *            the tracker for the current context
	 */
	private void schedule(Component component) {
		final boolean balance = isBalancing && isBalanceable;
		final boolean stall = processCache.isCriticalStartPoint(component);

		/*
		 * Schedule the Entries.
		 */
		final List<EntrySchedule> entrySchedules = new ArrayList<EntrySchedule>(
				component.getEntries().size());
		for (Entry entry : component.getEntries()) {
			entrySchedules.add(new UnbalancedEntrySchedule(entry, tracker,
					balance, stall));
		}

		UnbalancedEntrySchedule.merge(component, entrySchedules, tracker,
				balance);

		if (stall) {
			assert entrySchedules.size() < 2 : "Cannot stall at component with multiple entries";
			final Stallboard stallboard = ((UnbalancedEntrySchedule) entrySchedules
					.get(0)).getStallboard();
			processCache.registerStartPoint(component, stallboard);
		}

		/*
		 * Record the ControlStates for the Component's Exits.
		 */
		tracker.updateExitStates(component);

		// We register based on Dependencies so there is no need
		// to register added scheduling components.
		if (component.getOwner() instanceof Block) {
			processCache.getTracker(component.getOwner()).register(component);
		}

		component.postScheduleCallback(tracker);
	}

	/**
	 * Initializes the tracker with a 0-latency control state for InBuf of a
	 * given Module.
	 */
	private void scheduleInBuf(Module module) {
		/*
		 * Initialize the input latency and ready bus.
		 */
		tracker.defineControlBus(module.getInBuf().getGoBus(), Latency.ZERO);
		processCache.getTracker(module).register(module.getInBuf());
	}

	/**
	 * Schedules all the OutBufs of a Module.
	 */
	private void scheduleOutBufs(Module module) {
		for (OutBuf outBuf : module.getOutBufs()) {
			schedule(outBuf);
		}
	}

	private void closeModule(Module module) {
		connectStalls(module);

		// If we need to stall the module (because one or more
		// contained process has no mechanism of stalling a prior
		// process), then do so here by inserting a stall board to
		// catch the GO signal and then latch all the data ports. The
		// module is guaranteed to have a GO signal becuase it
		// contains one or more processes which will necessitate a
		// GO.
		final Set<ModuleStallSource> moduleStallProcs = processCache
				.getTracker(module).getModuleStallProcs(module);
		if (!moduleStallProcs.isEmpty()) {
			// First, insert latches on every data port
			Collection<Latch> latches = new HashSet<Latch>();
			for (Port port : module.getDataPorts()) {
				Bus dataBus = port.getPeer();
				Latch latch = tracker.getLatch(dataBus, module);
				latches.add(latch);
				for (Iterator<Port> connIter = (new HashSet<Port>(
						dataBus.getPorts())).iterator(); connIter.hasNext();) {
					Port connectionPort = connIter.next();
					// The latch may have already been inserted into
					// the LIM, thus do not connect the latch output
					// back to its input!
					if (connectionPort.getOwner() != latch) {
						connectionPort.setBus(latch.getResultBus());
					}
				}
			}
			// Now insert the stallboard between the GO and all logic,
			// but be careful to NOT move the latches enable. They
			// still need to be enabled by the GO.
			final Collection<Port> goTargets = new HashSet<Port>(module
					.getGoPort().getPeer().getPorts());
			// The module stallboard has only the GO as a non-stall input
			final Stallboard stbd = tracker
					.getStallboard(
							Collections.singleton(module.getGoPort().getPeer()),
							module);
			for (ModuleStallSource stallSource : moduleStallProcs) {
				final Set<Bus> stallSignals = new HashSet<Bus>();
				for (Component stallComp : stallSource.getStallingComponents()) {
					final Bus stallBus = BlockControlSignalIdentifier
							.getControlSignal(stallComp, tracker);
					// Use a pass through node so that it is used as
					// the feedback point so that constant prop does
					// not get confused about the order to run things
					// in.
					final Bus fbBus = getPassthruFeedbackBus(
							stallComp.getOwner(), stallBus);
					stallSignals.add(fbBus);
				}
				stbd.addStallSignals(stallSignals);
			}
			for (Port goPort : goTargets) {
				if (!latches.contains(goPort.getOwner())) {
					goPort.setBus(stbd.getResultBus());
				}
			}
		}
		processCache.deleteTracker(module);

		updateExits(module);
		updateConnectorAttributes(module);
	}

	/**
	 * During scheduling the stall signals for each process are identified, now
	 * we need to actually connect them to the Stallboard for the process.
	 * 
	 * @param module
	 *            a value of type 'Module'
	 */
	private void connectStalls(Module module) {
		for (MemProcess process : processCache.getProcesses()) {
			if (process.isProcessContext(module)) {
				for (ProcessStartPoint startPoint : process.getStartPoints()) {
					final Stallboard stbd = startPoint.getStallPoint();
					final Map<ID, Latency> stallBuses = new HashMap<ID, Latency>();
					for (Component stallComp : startPoint.getStallSignals()) {
						// Check to ensure that the stall signal is
						// coming from a component in the same module.
						// This MUST be guaranteed!
						assert stallComp.getOwner() == stbd.getOwner();
						final Bus stallBus = BlockControlSignalIdentifier
								.getControlSignal(stallComp, tracker);
						stallBuses.put(stallBus, tracker.getLatency(stallBus));
					}

					// Only connect the 'latest' stall. Because we
					// are in a single block, we are guaranteed that
					// all signals are going to be activated, thus we
					// only need the latest one.
					final Map latestStalls = Latency.getLatest(stallBuses);
					stbd.addStallSignals(latestStalls.keySet());
					stbd.getOwner().addFeedbackPoint(stbd);
				}
			}
		}
	}

	/**
	 * Propagates the internal Latencies of each OutBuf to its peer Exit. Also
	 * removes any unnecessary components that may have been added during
	 * scheduling, and updates the scheduling attributes of the module based
	 * upon its contents.
	 * 
	 * @param module
	 *            the module whose Exits are to be updated
	 * @param tracker
	 *            the tracker for the module context
	 * @param isControlledContext
	 *            true if the OutBufs require a go Port connection due to
	 *            implicit conditions within the Module
	 */
	private void updateExits(Module module) {
		/*
		 * Hardware may have been added that affects the attributes of the
		 * Module (e.g., it may now need a clock).
		 */
		updateSchedulingAttributes(module, true);

		/*
		 * If the module produces a done signal, hook up the done Ports of all
		 * OutBufs.
		 */
		if (module.producesDone()) {
			for (OutBuf outbuf : module.getOutBufs()) {
				outbuf.getGoPort().setBus(tracker.getControlBus(outbuf));
			}
		}

		/*
		 * Propagate the OutBuf Latencies to the Exits. Create new open
		 * Latencies as needed that do not refer to Buses within the Module.
		 */
		for (Exit nextExit : module.getExits()) {
			final Bus outbufControlBus = tracker.getControlBus(nextExit
					.getPeer());
			Latency exitLatency = tracker.getLatency(outbufControlBus);
			if (exitLatency.isOpen()) {
				exitLatency = Latency.get(exitLatency.getMinClocks()).open(
						nextExit);
			}
			// System.out.println("setting exit " + nextExit + " of " +
			// nextExit.getOwner() + " to " + exitLatency);
			// System.out.println("\tmin: " + exitLatency.getMinClocks() +
			// " max: " + exitLatency.getMaxClocks());
			nextExit.setLatency(exitLatency);
		}

		/*
		 * Get rid of logic that wasn't actually connected.
		 */
		removeUnusedComponents(module);
	}

	/**
	 * Calls {@link ScheduleVisitor#updateConnectorAttributes(Component)} on
	 * each {@link Component} of a given {@link Module}.
	 */
	private void updateConnectorAttributes(Module module) {
		for (Component component : module.getComponents()) {
			updateConnectorAttributes(component);
		}
	}

	/**
	 * Connects clock and reset Buses to a given Component's Ports if necessary.
	 * Also sets the isUsed flag of each Port and Bus according to whether or
	 * not it is connected.
	 */
	private void updateConnectorAttributes(Component component) {
		final Module owner = component.getOwner();
		final InBuf inbuf = owner.getInBuf();

		component.getClockPort().setUsed(component.consumesClock());
		if (component.consumesClock()) {
			component.getClockPort().setBus(inbuf.getClockBus());
		}

		component.getResetPort().setUsed(component.consumesReset());
		if (component.consumesReset()) {
			component.getResetPort().setBus(inbuf.getResetBus());
		}

		for (Port port : component.getPorts()) {
			if (port.isConnected()) {
				port.setUsed(true);
				port.getBus().setUsed(true);
			}
		}

		component.getGoPort().setUsed(component.consumesGo());
		for (Exit exit : component.getExits()) {
			exit.getDoneBus().setUsed(component.producesDone());
		}
	}

	/**
	 * Visits each {@link Component} of a given {@link Module} and schedules
	 * that {@link Component} internally. Also sets the scheduling attributes of
	 * the {@link Module} based upon its contents.
	 * 
	 * @param module
	 *            the modules whose components are to be visited
	 */
	private void visitComponents(Module module) {
		/*
		 * Schedule each component internally. We will then know each
		 * component's latency and other scheduling characteristics.
		 */
		Collection<Component> components = new HashSet<Component>(
				module.getComponents());
		for (Component component : components) {
			component.accept(this);
		}

		/*
		 * Do a preliminary update of the module's scheduling characteristics.
		 */
		updateSchedulingAttributes(module, false);
	}

	/**
	 * Update the scheduling attributes of a {@link Module} based upon its
	 * components.
	 * 
	 * @param module
	 *            the module whose attributes are updated
	 * @param isScheduled
	 *            true if the module has already been internally scheduled, that
	 *            is, the LatencyTracker has an entry latency for each of the
	 *            module's components
	 * 
	 * @see Component#consumesGo()
	 * @see Component#consumesClock()
	 * @see Component#consumesReset()
	 * @see Component#producesDone()
	 * @see Component#isDoneSynchronous()
	 */
	private void updateSchedulingAttributes(Module module, boolean isScheduled) {
		// System.out.println("UPDATING " + module);
		/*
		 * Ignore InBuf and OutBufs, since they derive their attributes from the
		 * Module.
		 */
		Collection<Component> components = new HashSet<Component>(
				module.getComponents());
		components.remove(module.getInBuf());

		/*
		 * If the latency at an OutBuf isn't fixed, then the Module must produce
		 * a synchronous done.
		 */
		for (OutBuf outbuf : module.getOutBufs()) {
			if (isScheduled) {
				/*
				 * If the module has already been scheduled, then additionally
				 * check the latencies of the OutBufs. This is especially useful
				 * in the case of a Branch, in which each of the two paths has a
				 * different fixed latency, which results in a variable latency
				 * overall.
				 */
				if (!tracker.getLatency(outbuf).isFixed()) {
					module.setProducesDone(true);
					module.setDoneSynchronous(true);
				}
			}
			components.remove(outbuf);
		}

		propagateSchedulingAttributes(module);

		/*
		 * If for no other reason, a Module must produce done signals if it has
		 * more than one Exit (else how would we know which Exit was being
		 * taken?).
		 */
		if (!module.producesDone()) {
			module.setProducesDone(isForcingDone
					|| (module.getExits().size() > 1));
		}
	}

	private static void propagateSchedulingAttributes(Module module) {
		Collection<Component> components = new LinkedHashSet<Component>(
				module.getComponents());
		components.remove(module.getInBuf());
		components.removeAll(module.getOutBufs());

		/*
		 * Update the module based upon the rest of its components.
		 */
		for (Component component : components) {

			if (component.consumesGo()) {
				module.setConsumesGo(true);
			}

			// Seperated doneSynch out from the consumesGo test
			// because (eg) combinational memory reads consume a GO to
			// generated the enable to the memory, but the DONE from
			// the component is not used. A modules done is
			// synchronous iff something within that module generates
			// a synchronous done.

			if (component.isDoneSynchronous()) {
				module.setDoneSynchronous(true);
			}
			if (component.consumesClock()) {
				module.setConsumesClock(true);
			}
			if (component.consumesReset()) {
				module.setConsumesReset(true);
			}
			if (hasVariableLatency(component)) {
				module.setProducesDone(true);
				module.setDoneSynchronous(true);
			}
		}

	}

	/**
	 * Tests whether a given {@link Component} has variable {@link Latency} or
	 * not.
	 * 
	 * @param component
	 *            the component whose latency is to be tested
	 * @return true if all of the component's exits have the same fixed latency,
	 *         false otherwise
	 */
	private static boolean hasVariableLatency(Component component) {
		final Set<Integer> exitLatencies = new HashSet<Integer>();
		for (Exit exit : component.getExits()) {
			final Latency latency = exit.getLatency();
			if (!latency.isFixed()) {
				return true;
			}
			exitLatencies.add(new Integer(latency.getMaxClocks()));
		}
		return exitLatencies.size() > 1;
	}

	private static void removeUnusedComponents(Module module) {
		final LinkedList<Component> unusedQueue = new LinkedList<Component>();
		for (Component component : module.getComponents()) {
			if (!isUsed(component)) {
				unusedQueue.add(component);
			}
		}

		final Collection<Component> removedComponents = new HashSet<Component>();
		while (!unusedQueue.isEmpty()) {
			final Component component = unusedQueue.removeFirst();
			final Collection<Component> inputComponents = getInputComponents(component);
			component.disconnect();
			module.removeComponent(component);
			removedComponents.add(component);

			for (Component inputComponent : inputComponents) {
				if (!isUsed(inputComponent)
						&& !removedComponents.contains(inputComponent)) {
					unusedQueue.add(inputComponent);
				}
			}
		}
	}

	private static Collection<Component> getInputComponents(Component component) {
		final Collection<Component> inputComponents = new HashSet<Component>();
		for (Port port : component.getPorts()) {
			if (port.isConnected()) {
				inputComponents.add(port.getBus().getOwner().getOwner());
			}
		}
		return inputComponents;
	}

	private static boolean isUsed(Component component) {
		/*
		 * Don't throw away InBufs.
		 */
		if (component == component.getOwner().getInBuf()) {
			return true;
		}

		if (component instanceof TimingOp) {
			return true;
		}

		if (component.isNonRemovable()) {
			return true;
		}

		/*
		 * Dont trounce on the expected loop structure...
		 */
		if (component.getOwner() instanceof Loop
				&& component == ((Loop) component.getOwner()).getInitBlock()) {
			return true;
		}

		/*
		 * If there is at least one connected Bus, then the component is used.
		 */
		for (Bus bus : component.getBuses()) {
			if (bus.isConnected()) {
				return true;
			}
		}

		/*
		 * Otherwise, if the component has a data bus that should be used but
		 * isn't, then it must be unused.
		 */
		for (Exit exit : component.getExits()) {
			if (!exit.getDataBuses().isEmpty()) {
				return false;
			}
		}

		/*
		 * Otherwise the component must be fulfulling its purpose without any
		 * output Buses.
		 */
		return true;
	}

	/**
	 * Removes feedback control register in loops and replaces feedback data
	 * registers with Latches. This will save a clock count for each iteration.
	 * Only remove the loop flop when the feedback exit has clock counts greater
	 * than 0
	 * 
	 * @param loop
	 *            a loop
	 */
	private void optimizeLoopFlop(Loop loop) {
		final Reg controlReg = loop.getControlRegister();
		final LoopBody body = loop.getBody();

		final Bus controlBus = getPassthruFeedbackBus(loop,
				tracker.getControlBus(body.getFeedbackExit().getDoneBus()));
		List<Port> controlPorts = new ArrayList<Port>(controlReg.getResultBus()
				.getPorts());
		for (Port port : controlPorts) {
			port.disconnect();
			port.setBus(controlBus);
		}
		loop.removeComponent(controlReg);

		/*
		 * Just wire through the fb data registers since we are already
		 * replacing the data latch in a loop body with a reg.
		 */
		Collection<Reg> dataRegs = new ArrayList<Reg>(loop.getDataRegisters());
		for (Reg dataReg : dataRegs) {

			for (Port port : new ArrayList<Port>(dataReg.getResultBus()
					.getPorts())) {
				Bus fb = getPassthruFeedbackBus(loop, dataReg.getDataPort()
						.getBus());
				port.setBus(fb);
			}
			loop.removeComponent(dataReg);
		}
	}

	private static Bus getPassthruFeedbackBus(Module context, Bus bus) {
		Constant zero = new SimpleConstant(0, bus.getValue().getSize(), bus
				.getValue().isSigned());
		OrOp or = new OrOp();
		or.getLeftDataPort().setBus(bus);
		or.getRightDataPort().setBus(zero.getValueBus());
		context.addComponent(zero);
		context.addComponent(or);
		context.addFeedbackPoint(or);
		zero.propagateValuesForward();
		or.propagateValuesForward();
		return or.getResultBus();
	}

}
