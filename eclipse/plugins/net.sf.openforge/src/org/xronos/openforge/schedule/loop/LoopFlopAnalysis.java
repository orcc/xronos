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

package org.xronos.openforge.schedule.loop;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import org.xronos.openforge.lim.ArrayRead;
import org.xronos.openforge.lim.ArrayWrite;
import org.xronos.openforge.lim.Block;
import org.xronos.openforge.lim.Branch;
import org.xronos.openforge.lim.Bus;
import org.xronos.openforge.lim.Call;
import org.xronos.openforge.lim.Component;
import org.xronos.openforge.lim.Decision;
import org.xronos.openforge.lim.Exit;
import org.xronos.openforge.lim.FailVisitor;
import org.xronos.openforge.lim.ForBody;
import org.xronos.openforge.lim.HeapRead;
import org.xronos.openforge.lim.HeapWrite;
import org.xronos.openforge.lim.InBuf;
import org.xronos.openforge.lim.Latch;
import org.xronos.openforge.lim.Latency;
import org.xronos.openforge.lim.Loop;
import org.xronos.openforge.lim.LoopBody;
import org.xronos.openforge.lim.MemoryAccessBlock;
import org.xronos.openforge.lim.Module;
import org.xronos.openforge.lim.OutBuf;
import org.xronos.openforge.lim.PriorityMux;
import org.xronos.openforge.lim.Procedure;
import org.xronos.openforge.lim.RegisterRead;
import org.xronos.openforge.lim.RegisterWrite;
import org.xronos.openforge.lim.Scoreboard;
import org.xronos.openforge.lim.Switch;
import org.xronos.openforge.lim.TaskCall;
import org.xronos.openforge.lim.UntilBody;
import org.xronos.openforge.lim.WhileBody;
import org.xronos.openforge.lim.io.FifoAccess;
import org.xronos.openforge.lim.io.FifoRead;
import org.xronos.openforge.lim.io.FifoWrite;
import org.xronos.openforge.lim.io.SimplePinAccess;
import org.xronos.openforge.lim.io.SimplePinRead;
import org.xronos.openforge.lim.io.SimplePinWrite;
import org.xronos.openforge.lim.memory.AbsoluteMemoryRead;
import org.xronos.openforge.lim.memory.AbsoluteMemoryWrite;
import org.xronos.openforge.lim.memory.LocationConstant;
import org.xronos.openforge.lim.op.AddOp;
import org.xronos.openforge.lim.op.AndOp;
import org.xronos.openforge.lim.op.CastOp;
import org.xronos.openforge.lim.op.ComplementOp;
import org.xronos.openforge.lim.op.ConditionalAndOp;
import org.xronos.openforge.lim.op.ConditionalOrOp;
import org.xronos.openforge.lim.op.Constant;
import org.xronos.openforge.lim.op.DivideOp;
import org.xronos.openforge.lim.op.EqualsOp;
import org.xronos.openforge.lim.op.GreaterThanEqualToOp;
import org.xronos.openforge.lim.op.GreaterThanOp;
import org.xronos.openforge.lim.op.LeftShiftOp;
import org.xronos.openforge.lim.op.LessThanEqualToOp;
import org.xronos.openforge.lim.op.LessThanOp;
import org.xronos.openforge.lim.op.MinusOp;
import org.xronos.openforge.lim.op.ModuloOp;
import org.xronos.openforge.lim.op.MultiplyOp;
import org.xronos.openforge.lim.op.NoOp;
import org.xronos.openforge.lim.op.NotEqualsOp;
import org.xronos.openforge.lim.op.NotOp;
import org.xronos.openforge.lim.op.NumericPromotionOp;
import org.xronos.openforge.lim.op.OrOp;
import org.xronos.openforge.lim.op.PlusOp;
import org.xronos.openforge.lim.op.RightShiftOp;
import org.xronos.openforge.lim.op.RightShiftUnsignedOp;
import org.xronos.openforge.lim.op.ShortcutIfElseOp;
import org.xronos.openforge.lim.op.SubtractOp;
import org.xronos.openforge.lim.op.XorOp;
import org.xronos.openforge.lim.primitive.And;
import org.xronos.openforge.lim.primitive.EncodedMux;
import org.xronos.openforge.lim.primitive.Mux;
import org.xronos.openforge.lim.primitive.Not;
import org.xronos.openforge.lim.primitive.Or;
import org.xronos.openforge.lim.primitive.Reg;
import org.xronos.openforge.lim.primitive.SRL16;
import org.xronos.openforge.schedule.LatencyCache;


/**
 * LoopFlopAnalysis is a visitor that is used to traverse the hierarchy of a
 * loop body and find all accesses to any shared resource. These include
 * memories, registers, and even a loop itself (a loop is a shared resource with
 * only 1 access, itself). For each access, the absolute GO and DONE latencies
 * (with respect to the starting loop body) are captured. Once the traversal is
 * completed we can then group the accesses according to the resource and find
 * any pair of accesses where one access is in the 0th cycle of the loop body
 * and the other access is in the last cycle of the loop body (note that a loops
 * GO may be in the 0th and DONE in the nth which meets this criteria). If this
 * case is found then the loop flop MUST exist in the containing loop.
 * <p>
 * This analysis suffers from one limitation however in that any 'open' latency
 * must be regarded as completing in the last cycle because we do not know
 * whether there will be any cycles beyond that open latency before the end of
 * the loop body. Thus an open latency in the first cycle by (eg) a loop will
 * cause the loop flop to be non-removable. The way to get around this would be
 * to do a traversal and see if there are any latent operations between the done
 * of the open latency component and the done of the loop body.
 * 
 * <p>
 * Created: Fri Mar 19 10:54:35 2004
 * 
 * @author imiller, last modified by $Author: imiller $
 * @version $Id: LoopFlopAnalysis.java 562 2008-03-17 21:33:12Z imiller $
 */
public class LoopFlopAnalysis extends FailVisitor {

	/** The latency of the loop body which is being analyzed. */
	private Latency bodyLatency;
	private boolean flopIsRemovable = false;
	private boolean failFast = true;

	/**
	 * The absolute latency of the GO of the current module relative to the loop
	 * body which is being analyzed.
	 */
	private Latency currentLatency = Latency.ZERO;

	/**
	 * The latency cache that is used to determine the latency of the bus
	 * driving the GO port of components.
	 */
	private LatencyCache latCache;

	/** Map of resource (object) to Set of FeedbackTuples */
	private Map<Object, Set<FeedbackTuple>> resourceTuples = new HashMap<Object, Set<FeedbackTuple>>();
	private Map<Object, Set<LoopFlopConflict>> resourceConflicts = new HashMap<Object, Set<LoopFlopConflict>>();

	private Stack<Component> pathToComponent = new Stack<Component>();

	/**
	 * Sets the state of the {@link LoopBody#isLoopFlopNeeded} method by
	 * analyzing the loop body for dependencies which would require the
	 * iterations to be seperated by a cycle. This method requires that the loop
	 * body be fully scheduled prior to analysis.
	 * 
	 * @param body
	 *            a non null LoopBody
	 * @param cache
	 *            a non null LatencyCache
	 * @throws IllegalArgumentException
	 *             if either body or cache are null.
	 */
	public static void setLoopFlopStatus(LoopBody body, LatencyCache cache) {
		if (body == null || cache == null) {
			throw new IllegalArgumentException("Null argument");
		}

		// If the body of the loop can be combinational, then the loop
		// flop is absolutely necessary.
		if (body.getBody().getLatency().getMinClocks() <= 0 || false) {
			if (_loop.db)
				_loop.ln("LFA: Loop flop needed due to body latency "
						+ body.getBody().getLatency());
			body.setLoopFlopNeeded(true);
			return;
		}

		if (_loop.db)
			_loop.ln("Tracking " + body);
		LoopFlopAnalysis analyzer = new LoopFlopAnalysis(body.getBody(), cache,
				true);
		if (analyzer.isRemovable()) {
			if (_loop.db)
				_loop.ln("LFA: Set loop flop status to removable for loopbody "
						+ body + " of " + body.showOwners());
			body.setLoopFlopNeeded(false);
		}
	}

	public static Map<Object, Set<LoopFlopConflict>> analyzeLoopFlopStatus(
			LoopBody body, LatencyCache cache) {
		if (body == null || cache == null) {
			throw new IllegalArgumentException("Null argument");
		}

		// If the body of the loop can be combinational, then the loop
		// flop is absolutely necessary.
		if (body.getBody().getLatency().getMinClocks() <= 0 || false) {
			if (_loop.db)
				_loop.ln("LFA: Loop flop needed due to body latency "
						+ body.getBody().getLatency());
			return Collections.emptyMap();
		}

		LoopFlopAnalysis analyzer = new LoopFlopAnalysis(body.getBody(), cache,
				false);
		return analyzer.resourceConflicts;
	}

	public LoopFlopAnalysis(Module body, LatencyCache cache, boolean failFast) {
		super("Loop Feedback Dependency Analysis");

		this.failFast = failFast;
		flopIsRemovable = false;
		bodyLatency = body.getLatency();
		latCache = cache;
		currentLatency = Latency.ZERO;

		if (_loop.db)
			_loop.ln("Module " + body + " the latency is " + bodyLatency);
		try {
			// Do not simply visit the loop body because its GO is not
			// yet hooked up, thus we would end up not diving into it,
			// instead, we care about what is inside the loop body.

			pathToComponent.push(body);
			currentLatency = Latency.ZERO;

			visitModuleComponents(body);

			Component popped = pathToComponent.pop();
			assert popped == body; // sanity check
		} catch (NotAnalyzableException e) {
			// body.setLoopFlopNeeded(true); // Dont override a previous
			// indication
			if (_loop.db)
				_loop.ln("LFA: Feedback must be delayed (flop not removable) for "
						+ body + " " + e);
			return;
		} catch (FlopNotRemovableException e) {
			// body.setLoopFlopNeeded(true); // Dont override a previous
			// indication
			if (_loop.db)
				_loop.ln("LFA: Feedback must be delayed (Flop not removable) for "
						+ body + " " + e);
			return;
		}

		// If we make it this far, then the flop is removable.
		flopIsRemovable = true;
	}

	public boolean isRemovable() {
		return flopIsRemovable;
	}

	@Override
	public void visit(AbsoluteMemoryRead vis) {
		markMemAcc(vis);
	}

	@Override
	public void visit(AbsoluteMemoryWrite vis) {
		markMemAcc(vis);
	}

	@Override
	public void visit(ArrayRead vis) {
		markMemAcc(vis);
	}

	@Override
	public void visit(ArrayWrite vis) {
		markMemAcc(vis);
	}

	@Override
	public void visit(HeapWrite vis) {
		markMemAcc(vis);
	}

	@Override
	public void visit(HeapRead vis) {
		markMemAcc(vis);
	}

	// SimplePinAccess is generic pin behavior. Mark it to be sure.
	// public void visit (SimplePinAccess vis){mark(vis,
	// vis.getGoPort().getBus(), vis.getGoPort().getBus(), vis.getTargetPin());}
	@Override
	public void visit(SimplePinAccess vis) {
		mark(vis, vis.getGoPort().getBus(),
				vis.getExit(Exit.DONE).getDoneBus(), vis.getTargetPin());
	}

	@Override
	public void visit(FifoAccess vis) {
		Bus controlBus = vis.getGoPort().getBus();
		if (controlBus == null)
			controlBus = latCache.getControlBus(vis);
		Exit exit = vis.getExit(Exit.DONE);
		// if (exit == null) // just to be safe
		// exit = (Exit)vis.getExits().iterator().next();
		mark(vis, controlBus, exit.getDoneBus(), vis.getFifoIF());
	}

	@Override
	public void visit(FifoRead vis) {
		mark(vis, vis.getGoPort().getBus(),
				vis.getExit(Exit.DONE).getDoneBus(), vis.getFifoIF());
	}

	@Override
	public void visit(FifoWrite vis) {
		mark(vis, vis.getGoPort().getBus(),
				vis.getExit(Exit.DONE).getDoneBus(), vis.getFifoIF());
	}

	/*
	 * All memory accesses are buried in one of the more stylized modules. We
	 * should thus not ever see one of these. public void visit (MemoryRead
	 * vis){mark(vis, vis.getMemoryPort().getLogicalMemory());} public void
	 * visit (MemoryWrite vis){mark(vis,
	 * vis.getMemoryPort().getLogicalMemory());}
	 */

	/*
	 * We dont care about register reads in the last cycle, thus set its bus to
	 * null. Actually, since register writes are always 1 cycle, it is
	 * impossible for the start of the accesses to overlap, and register reads
	 * can safely occur in the same cycle. Thus, we can safely not track the
	 * register accesses.
	 */
	@Override
	public void visit(RegisterRead vis) {
	}

	@Override
	public void visit(RegisterWrite vis) {
	}

	@Override
	public void visit(Loop vis) {
		mark(vis, vis.getGoPort().getBus(),
				vis.getExit(Exit.DONE).getDoneBus(), vis);
		moduleDive(vis);
	}

	@Override
	public void visit(ForBody vis) {
		moduleDive(vis);
	}

	@Override
	public void visit(UntilBody vis) {
		moduleDive(vis);
	}

	@Override
	public void visit(WhileBody vis) {
		moduleDive(vis);
	}

	@Override
	public void visit(Block vis) {
		moduleDive(vis);
	}

	@Override
	public void visit(Branch vis) {
		moduleDive(vis);
	}

	@Override
	public void visit(Decision vis) {
		moduleDive(vis);
	}

	@Override
	public void visit(TaskCall vis) {
		// Similar to handling the Call but the outerlatency is from
		// the taskcall object in caller
		if (_loop.db)
			_loop.ln("\nEntering taskcall " + vis.showIDLogical());
		moduleDive(vis, vis.getTarget().getCall().getProcedure().getBody());
		if (_loop.db)
			_loop.ln("done with taskcall " + vis.showIDLogical());
	}

	@Override
	public void visit(Call vis) {
		if (vis.getProcedure() != null && vis.getProcedure().getBody() != null) {
			Block b = vis.getProcedure().getBody();
			moduleDive(vis, b);
		}
	}

	private void moduleDive(Module mod) {
		moduleDive(mod, mod);
	}

	/**
	 * Visit the contents of the module after setting the current latency (ie
	 * base latency) to be that of the modules GO port, relative to the loop
	 * body being analyzed.
	 * 
	 * @param ref
	 *            the component which defines the entry latency of the module
	 *            (may be the module itself or it may be a call)
	 * @param body
	 *            a value of type 'Module'
	 */
	private void moduleDive(Component ref, Module body) {
		// Save the current latency so we can restore it when done
		// with this module.
		Latency outerLatency = currentLatency;

		Latency moduleGo;
		if (ref.getGoPort() == null || !ref.getGoPort().isConnected()) {
			// Everything that we consider to be a feedback point
			// (loops, memory accesses, etc) all require a GO to be
			// consumed, therefor, if a module has no GO, it cannot
			// supply the GO to what we are interested in, thus we
			// do not need to dive into the module
			return;
		}

		pathToComponent.push(ref);

		// Calculate the absolute latency of the Module GO
		moduleGo = getLatency(ref.getGoPort().getBus());
		currentLatency = mergeLatencies(moduleGo, currentLatency);

		// Visit all the components of this module
		visitModuleComponents(body);

		// restore the current latency.
		currentLatency = outerLatency;
		Object popped = pathToComponent.pop();
		assert popped == ref; // sanity check
	}

	/**
	 * Simply visit each component of the module according to the order that
	 * they are returned by the getComponents method.
	 * 
	 * @param module
	 *            a non null Module
	 * @throws NullPointerException
	 *             if module is null
	 */
	private void visitModuleComponents(Module module) {
		for (Component comp : module.getComponents()) {
			comp.accept(this);
		}
	}

	/**
	 * Mark the memory access. In general, what we care about is whether or not
	 * 2 memory accesses can START in the same cycle, thus it is intuitive to
	 * use the go as both the early and late latency.
	 */
	private void markMemAcc(MemoryAccessBlock acc) {
		mark(acc, acc.getGoPort().getBus(), acc.getGoPort().getBus(), acc
				.getLogicalMemoryPort().getLogicalMemory());
	}

	/**
	 * This method marks the early and late latencies of the given component as
	 * an access to the given resource. An early latency is the latency of the
	 * bus that is being considered when determining whether the access is in
	 * the first cycle of the loopbody. The late latency is the latency of the
	 * bus that is being considered when determining whether the access is in
	 * the last cycle of the loop body. This method is fail-fast. Thus as soon
	 * as we find a case where there is an access to the resource in both the
	 * first and last cycle of the loopbody being analyzed, we throw an
	 * exception and stop the travesal.
	 * 
	 * @param comp
	 *            a value of type 'Component'
	 * @param earlyBus
	 *            a value of type 'Bus'
	 * @param lateBus
	 *            a value of type 'Bus'
	 * @param resource
	 *            an Object, used only as a key to the map and for comparisons
	 *            to determine if accesses are to the same resource.
	 */
	private void mark(Component comp, Bus earlyBus, Bus lateBus, Object resource) {
		if (_loop.db)
			_loop.ln("\nMarking comp " + comp + " to resource " + resource
					+ " " + comp.showOwners());
		// Find the latency of the early and late bus
		Latency early = getLatency(earlyBus);
		Latency late = getLatency(lateBus);

		if (_loop.db)
			_loop.ln("\tEarly " + early + " late " + late + " current "
					+ currentLatency);

		// Adjust latencies to be relative to the analyzed loopbody.
		// The currentLatency is the latency of the GO of the current
		// module _relative_ to the loopbody GO (ie it accumulates as
		// we dive into modules)
		Latency adjustedEarly = mergeLatencies(early, currentLatency);
		Latency adjustedLate = mergeLatencies(late, currentLatency);

		if (_loop.db)
			_loop.ln("\tAdjusted: Early " + adjustedEarly + " late "
					+ adjustedLate);

		// Create the tuple, and do the testing to see if the tuple
		// itself is in both the first and last cycle, or if it, along
		// with another access tuple to the same resource spans the
		// first and last cycles.
		FeedbackTuple tuple = new FeedbackTuple(resource, adjustedEarly,
				adjustedLate, comp, pathToComponent);

		if (_loop.db)
			_loop.ln("Created fb tuple " + tuple + " for " + comp + " path "
					+ pathToComponent);
		if (tuple.isFirstCycle() || tuple.isLastCycle()) {
			Set<FeedbackTuple> tuples = resourceTuples.get(tuple
					.getConflictedResource());
			if (tuple.isFirstCycle() && tuple.isLastCycle()) {
				// throw new FlopNotRemovableException("Access " + comp + " to "
				// + tuple.getConflictedResource() +
				// " is in both first and last cycle");
				isNotRemovable(
						"Access " + comp + " to "
								+ tuple.getConflictedResource()
								+ " is in both first and last cycle", tuple,
						tuple);
			}
			// First tuple for this resource. Save it.
			if (tuples == null) {
				tuples = new HashSet<FeedbackTuple>();
				tuples.add(tuple);
				resourceTuples.put(tuple.getConflictedResource(), tuples);
			} else {
				// Test if we already violate the 'removable flop'
				// criteria for fail-fast analysis.
				for (FeedbackTuple test : tuples) {
					if ((test.isFirstCycle() && tuple.isLastCycle())
							|| (tuple.isFirstCycle() && test.isLastCycle())) {
						// throw new
						// FlopNotRemovableException("First/Last access to " +
						// tuple.getConflictedResource());
						FeedbackTuple first = test.isFirstCycle() ? test
								: tuple;
						FeedbackTuple last = test.isFirstCycle() ? tuple : test;
						isNotRemovable(
								"First/Last access to "
										+ tuple.getConflictedResource(), first,
								last);
					}
				}
				tuples.add(tuple);
			}
		}
	}

	private void isNotRemovable(String msg, FeedbackTuple first,
			FeedbackTuple last) {
		if (_loop.db)
			_loop.ln(msg + " First: " + first + " last: " + last);
		if (failFast) {
			throw new FlopNotRemovableException(msg);
		}

		Object resource = first.getConflictedResource();
		assert last.getConflictedResource() == resource;
		Set<LoopFlopConflict> conflicts = resourceConflicts.get(resource);
		if (conflicts == null) {
			conflicts = new HashSet<LoopFlopConflict>();
			resourceConflicts.put(resource, conflicts);
		}
		conflicts.add(new LoopFlopConflict(first.getAccess(), first.getPath(),
				last.getAccess(), last.getPath()));
	}

	/**
	 * Retrieves the Latency of the given bus from the {@link LatencyCache}
	 * contained within this Analysis. It is possible, however, that if the bus
	 * is inside a function call (called from within the loop body) that we do
	 * not have access to the right latency cache. If that is the case, then the
	 * returned latency will be null and we will throw a NotAnalyzableException
	 * 
	 * @param bus
	 *            a non-null Bus
	 * @return a value of type 'Latency'
	 * @throws NotAnalyzableException
	 *             if the latency of the bus could not be determined.
	 */
	private Latency getLatency(Bus bus) {
		Latency lat = latCache.getLatency(bus);
		if (lat == null) {
			// This could be because the component is inside a
			// function call and we do not have access to the right
			// latency cache.
			throw new NotAnalyzableException("Latency of component "
					+ bus.getOwner().getOwner() + " could not be determined.");
		}

		return lat;
	}

	/**
	 * Merges two latencies by adding their min clocks and their max clocks.
	 * However, if either latency is open then the merged latency will be open
	 * too.
	 * 
	 * @param one
	 *            a non-null Latency
	 * @param two
	 *            a non-null Latency
	 * @return a non-null Latency
	 */
	private static Latency mergeLatencies(Latency one, Latency two) {
		Latency adjusted;
		if (one.isOpen() || two.isOpen()) {
			int min = one.getMinClocks() + two.getMinClocks();
			adjusted = Latency.get(min, min).open(null);
		} else {
			adjusted = Latency.get(one.getMinClocks() + two.getMinClocks(),
					one.getMaxClocks() + two.getMaxClocks());
		}

		return adjusted;
	}

	/*
	 * These are all the things that we expect to see but that have no bearing
	 * on this analysis.
	 */
	@Override
	public void visit(AddOp vis) {
	}

	@Override
	public void visit(And vis) {
	}

	@Override
	public void visit(AndOp vis) {
	}

	@Override
	public void visit(CastOp vis) {
	}

	@Override
	public void visit(ComplementOp vis) {
	}

	@Override
	public void visit(ConditionalAndOp vis) {
	}

	@Override
	public void visit(ConditionalOrOp vis) {
	}

	@Override
	public void visit(Constant vis) {
	}

	@Override
	public void visit(DivideOp vis) {
	}

	@Override
	public void visit(EncodedMux vis) {
	}

	@Override
	public void visit(EqualsOp vis) {
	}

	@Override
	public void visit(GreaterThanEqualToOp vis) {
	}

	@Override
	public void visit(GreaterThanOp vis) {
	}

	@Override
	public void visit(InBuf vis) {
	}

	@Override
	public void visit(Latch vis) {
	}

	@Override
	public void visit(LeftShiftOp vis) {
	}

	@Override
	public void visit(LessThanEqualToOp vis) {
	}

	@Override
	public void visit(LessThanOp vis) {
	}

	@Override
	public void visit(LocationConstant vis) {
	}

	@Override
	public void visit(MinusOp vis) {
	}

	@Override
	public void visit(ModuloOp vis) {
	}

	@Override
	public void visit(MultiplyOp vis) {
	}

	@Override
	public void visit(Mux vis) {
	}

	@Override
	public void visit(NoOp vis) {
	}

	@Override
	public void visit(Not vis) {
	}

	@Override
	public void visit(NotEqualsOp vis) {
	}

	@Override
	public void visit(NotOp vis) {
	}

	@Override
	public void visit(NumericPromotionOp vis) {
	}

	@Override
	public void visit(Or vis) {
	}

	@Override
	public void visit(OrOp vis) {
	}

	@Override
	public void visit(OutBuf vis) {
	}

	@Override
	public void visit(PlusOp vis) {
	}

	@Override
	public void visit(PriorityMux vis) {
	}

	@Override
	public void visit(Reg vis) {
	}

	@Override
	public void visit(RightShiftOp vis) {
	}

	@Override
	public void visit(RightShiftUnsignedOp vis) {
	}

	@Override
	public void visit(Scoreboard vis) {
	}

	@Override
	public void visit(ShortcutIfElseOp vis) {
	}

	@Override
	public void visit(SRL16 vis) {
	}

	@Override
	public void visit(SubtractOp vis) {
	}

	@Override
	public void visit(Switch vis) {
	}

	@Override
	public void visit(SimplePinRead vis) {
	}

	@Override
	public void visit(SimplePinWrite vis) {
	}

	@Override
	public void visit(XorOp vis) {
	}

	/**
	 * Becuase of how we visit calls, we should never see a Procedure
	 */
	@Override
	public void visit(Procedure vis) {
		super.visit(vis);
	}

	/*
	 * Defer to super, we should NEVER encounter these nodes in this type of
	 * traveral. public void visit (Design vis){ fail(vis); } public void visit
	 * (IPCoreCall vis){ fail(vis); } public void visit (Kicker vis){ fail(vis);
	 * } public void visit (MemoryBank vis){ fail(vis); } public void visit
	 * (MemoryGateway vis){ fail(vis); } public void visit (MemoryReferee vis){
	 * fail(vis); } public void visit (PinRead vis){ fail(vis); } public void
	 * visit (PinReferee vis){ fail(vis); } public void visit (PinStateChange
	 * vis){ fail(vis); } public void visit (PinWrite vis){ fail(vis); } public
	 * void visit (ReductionOrOp vis){ } public void visit (RegisterGateway
	 * vis){ fail(vis); } public void visit (RegisterReferee vis){ } public void
	 * visit (Task vis){ fail(vis); } public void visit (TimingOp vis){
	 * fail(vis); } public void visit (TriBuf vis){ fail(vis); }
	 */

	/**
	 * Simple utility class for keeping together data about an access to the
	 * specified resource.
	 */
	class FeedbackTuple {
		Object resource;
		Component access;
		private boolean isLast = true;
		private boolean isFirst = true;
		private List<Component> path;

		public FeedbackTuple(Object resource, Latency early, Latency late,
				Component access, Stack<Component> path) {
			this.resource = resource;
			this.access = access;
			isFirst = early.getMinClocks() == Latency.ZERO.getMinClocks();
			this.path = new ArrayList<Component>(path);
			// Test the component and then each component in the path
			// against their respective owners outbuf
			Component comp = access;
			Latency compLatency = late;

			for (int i = path.size() - 1; i >= 0; i--) {
				Module owner = comp.getOwner();
				boolean allGT = true;
				for (Exit exit : owner.getExits()) {
					if (exit.getTag().getType() == Exit.SIDEBAND) {
						// If this is run post-scheduling, sideband
						// exit latency should be ignored.
						continue;
					}

					final Latency outbufLatency = latCache.getLatency(exit
							.getPeer());
					if (_loop.db)
						_loop.ln("LFA: Testing " + comp + " lat " + compLatency
								+ " vs outbuf " + outbufLatency + " of "
								+ comp.showOwners());
					allGT &= outbufLatency.isGT(compLatency);
				}
				if (allGT) {
					isLast = false;
					break;
				}

				comp = path.get(i);
				// compLatency =
				// LoopFlopAnalysis.this.latCache.getLatency(comp.getExit(Exit.DONE).getDoneBus());
				// Since we are comparing the component latency to the
				// outbuf latency to see whether or not the component
				// can be the last thing in the block, we need to
				// assume the worst case. So find the maximum latency
				// of all exits.
				List<Exit> exits = new ArrayList<Exit>();
				for (Exit exit : comp.getExits()) {
					if (exit.getTag().getType() != Exit.SIDEBAND) {
						// If this is run post-scheduling, sideband
						// exit latency should be ignored.
						exits.add(exit);
					}
				}

				assert !exits.isEmpty();
				compLatency = latCache.getLatency(exits.remove(0).getDoneBus());
				for (Exit exit : exits) {
					Latency exitLatency = latCache
							.getLatency(exit.getDoneBus());
					if (exitLatency.isGT(compLatency))
						compLatency = exitLatency;
				}
			}
		}

		public boolean isFirstCycle() {
			return isFirst;
		}

		public boolean isLastCycle() {
			return isLast;
		}

		public Object getConflictedResource() {
			return resource;
		}

		public Component getAccess() {
			return access;
		}

		public List<Component> getPath() {
			return path;
		}

		@Override
		public String toString() {
			return "FeedbackTuple for " + resource + " isFirst: " + isFirst
					+ " isLast: " + isLast;
		}
	}

	@SuppressWarnings("serial")
	class NotAnalyzableException extends RuntimeException {
		public NotAnalyzableException(String msg) {
			super(msg);
		}
	}

	@SuppressWarnings("serial")
	class FlopNotRemovableException extends RuntimeException {
		public FlopNotRemovableException(String msg) {
			super(msg);
		}
	}

	// Testing interface

	/**
	 * <b>FOR TESTING ONLY DO NOT USE</b>
	 */
	// static LoopFlopAnalysis _getTestInstance (Latency bodyLatency)
	// {
	// return new LoopFlopAnalysis(bodyLatency);
	// }

	// FeedbackTuple makeTuple (Object o, Latency one, Latency two)
	// {
	// return new FeedbackTuple(o, one, two, null, new Stack());
	// }

	// private LoopFlopAnalysis (Latency lat)
	// {
	// super("JUST FOR TESTING, LOOP FLOP ANALYSIS");
	// this.bodyLatency = lat;
	// }

}// LoopFlopAnalysis
