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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import org.xronos.openforge.app.EngineThread;
import org.xronos.openforge.lim.ArrayRead;
import org.xronos.openforge.lim.ArrayWrite;
import org.xronos.openforge.lim.Block;
import org.xronos.openforge.lim.Branch;
import org.xronos.openforge.lim.Call;
import org.xronos.openforge.lim.Component;
import org.xronos.openforge.lim.Decision;
import org.xronos.openforge.lim.Dependency;
import org.xronos.openforge.lim.Design;
import org.xronos.openforge.lim.Entry;
import org.xronos.openforge.lim.Exit;
import org.xronos.openforge.lim.ForBody;
import org.xronos.openforge.lim.HeapRead;
import org.xronos.openforge.lim.HeapWrite;
import org.xronos.openforge.lim.IPCoreCall;
import org.xronos.openforge.lim.InBuf;
import org.xronos.openforge.lim.Kicker;
import org.xronos.openforge.lim.Latch;
import org.xronos.openforge.lim.Latency;
import org.xronos.openforge.lim.Loop;
import org.xronos.openforge.lim.Module;
import org.xronos.openforge.lim.OutBuf;
import org.xronos.openforge.lim.PinRead;
import org.xronos.openforge.lim.PinReferee;
import org.xronos.openforge.lim.PinStateChange;
import org.xronos.openforge.lim.PinWrite;
import org.xronos.openforge.lim.PriorityMux;
import org.xronos.openforge.lim.Procedure;
import org.xronos.openforge.lim.RegisterGateway;
import org.xronos.openforge.lim.RegisterRead;
import org.xronos.openforge.lim.RegisterReferee;
import org.xronos.openforge.lim.RegisterWrite;
import org.xronos.openforge.lim.ResourceDependency;
import org.xronos.openforge.lim.Scoreboard;
import org.xronos.openforge.lim.Switch;
import org.xronos.openforge.lim.Task;
import org.xronos.openforge.lim.TaskCall;
import org.xronos.openforge.lim.TriBuf;
import org.xronos.openforge.lim.UntilBody;
import org.xronos.openforge.lim.Visitable;
import org.xronos.openforge.lim.Visitor;
import org.xronos.openforge.lim.WhileBody;
import org.xronos.openforge.lim.io.FifoAccess;
import org.xronos.openforge.lim.io.FifoRead;
import org.xronos.openforge.lim.io.FifoWrite;
import org.xronos.openforge.lim.io.SimplePin;
import org.xronos.openforge.lim.io.SimplePinAccess;
import org.xronos.openforge.lim.io.SimplePinRead;
import org.xronos.openforge.lim.io.SimplePinWrite;
import org.xronos.openforge.lim.memory.AbsoluteMemoryRead;
import org.xronos.openforge.lim.memory.AbsoluteMemoryWrite;
import org.xronos.openforge.lim.memory.EndianSwapper;
import org.xronos.openforge.lim.memory.LocationConstant;
import org.xronos.openforge.lim.memory.MemoryBank;
import org.xronos.openforge.lim.memory.MemoryGateway;
import org.xronos.openforge.lim.memory.MemoryRead;
import org.xronos.openforge.lim.memory.MemoryReferee;
import org.xronos.openforge.lim.memory.MemoryWrite;
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
import org.xronos.openforge.lim.op.ReductionOrOp;
import org.xronos.openforge.lim.op.RightShiftOp;
import org.xronos.openforge.lim.op.RightShiftUnsignedOp;
import org.xronos.openforge.lim.op.ShortcutIfElseOp;
import org.xronos.openforge.lim.op.SubtractOp;
import org.xronos.openforge.lim.op.TimingOp;
import org.xronos.openforge.lim.op.XorOp;
import org.xronos.openforge.lim.primitive.And;
import org.xronos.openforge.lim.primitive.EncodedMux;
import org.xronos.openforge.lim.primitive.Mux;
import org.xronos.openforge.lim.primitive.Not;
import org.xronos.openforge.lim.primitive.Or;
import org.xronos.openforge.lim.primitive.Reg;
import org.xronos.openforge.lim.primitive.SRL16;


/**
 * The LoopConditionalBranchBalancer finds combinational paths through a loop
 * body and attempts to balance the delay through those paths according to this
 * logic:
 * <ol>
 * <li>If all paths are combinational, nothing is done
 * <li>If all paths are definitively non-combinational, nothing is done.
 * <li>If one side of a branch statement is definitively non-combinational, and
 * the other side is definitively combinational, the combinational path will
 * have a new {@link ResourceDependency} added from the GO to the DONE with a
 * minimum offset clocks of 1.
 * <li>If the latency of either side is non definitive then a new
 * {@link ResourceDependency} with minimum clocks of 1 will be placed around the
 * entire Branch, causing a scoreboard to be inserted to merge the done of the
 * branch with a 1 cycle latency delay of its GO.
 * </ol>
 * 
 * <p>
 * This optimization works by visiting the entire LIM depth first. At each
 * modular level the latency is determined by examining the contents of that
 * module. These latencies are propagated up through the levels. At each branch,
 * the latencies of the true vs the false branches are compared (actually the
 * greatest latency(ies) from each is(are) compared.
 * 
 * @version $Id: LoopConditionalBranchBalancer.java 558 2008-03-14 14:14:48Z
 *          imiller $
 */
public class LoopConditionalBranchBalancer implements Visitor {

	private Set<Latency> currentLatencies = new HashSet<Latency>();
	private Stack<Set<Latency>> context = new Stack<Set<Latency>>();
	private boolean isInLoopContext = false;

	@Override
	public void visit(Branch branch) {
		// Only do the analysis in the context of a loop, otherwise we
		// are slowing down the processing needlessly.
		if (isInLoopContext) {
			enterModule();

			// Handle the true and false seperately so that we can see
			// what the latencies in those blocks are.
			enterModule();
			branch.getTrueBranch().accept(this);
			Set<Latency> trueLatencies = exitModule(branch.getTrueBranch());

			enterModule();
			branch.getFalseBranch().accept(this);
			Set<Latency> falseLatencies = exitModule(branch.getFalseBranch());

			final boolean trueComb = isCombinational(trueLatencies);
			final boolean falseComb = isCombinational(falseLatencies);
			final boolean trueSeq = isSequential(trueLatencies);
			final boolean falseSeq = isSequential(falseLatencies);

			if (_loop.db) {
				_loop.ln("Analyzing " + branch + " of " + branch.showOwners());
				_loop.ln("\ttrueComb " + trueComb + " trueSeq " + trueSeq);
				_loop.ln("\ttrue latencies " + trueLatencies);
				_loop.ln("\tfalseComb " + falseComb + " falseSeq " + falseSeq);
				_loop.ln("\tfalse latencies " + falseLatencies);
			}

			if (((trueComb && falseComb) && (!trueSeq && !falseSeq))
					|| ((!trueComb && !falseComb) && (trueSeq && falseSeq))) {
				// do nothing
			} else if (trueComb && falseSeq) {
				if (_loop.db)
					_loop.ln("Delaying true branch of " + branch + " "
							+ branch.showOwners());
				// delay the true path
				delayModule(branch.getTrueBranch());
				// Ensure that we mark the fact that this branch now takes
				// at least 1 cycle.
				currentLatencies.add(Latency.ONE);
			} else if (trueSeq && falseComb) {
				if (_loop.db)
					_loop.ln("Delaying false branch of " + branch + " "
							+ branch.showOwners());
				// delay the false path
				delayModule(branch.getFalseBranch());
				// Ensure that we mark the fact that this branch now takes
				// at least 1 cycle.
				currentLatencies.add(Latency.ONE);
			} else {
				if (_loop.db)
					_loop.ln("Delaying entire branch " + branch + " "
							+ branch.showOwners());
				// delay the whole branch
				delayModule(branch);
				// Ensure that we mark the fact that this branch now takes
				// at least 1 cycle.
				currentLatencies.add(Latency.ONE);
			}

			enterModule();
			branch.getDecision().accept(this);
			exitModule(branch.getDecision());

			exitModule(branch);
		} else {
			handleModule(branch);
		}
	}

	/**
	 * Create a 1 cycle delay resource dependency between the GO and DONE of the
	 * specified module.
	 */
	private static void delayModule(Module module) {
		assert module.getExits().size() == 1 : "Can only delay modules with one (done) exit";
		final Exit exit = module.getExit(Exit.DONE);
		assert exit != null : "Module did not have done exit in delayModule";

		final OutBuf outbuf = exit.getPeer();
		final Entry outBufEntry = outbuf.getEntries().get(0);
		outBufEntry.addDependency(outbuf.getGoPort(), new ResourceDependency(
				module.getGoPort().getPeer(), 1));
	}

	/**
	 * Return true if ALL latencies in the set have fixed latency of 0.
	 */
	private static boolean isCombinational(Set<Latency> latencies) {
		for (Latency latency : latencies) {
			if (!latency.equals(Latency.ZERO))
				return false;
		}
		return true;
	}

	/**
	 * Returns true if there is ANY sequential latency in the set.
	 */
	private static boolean isSequential(Set<Latency> latencies) {
		for (Latency latency : latencies) {
			if (latency.getMinClocks() > 0)
				return true;
		}
		return false;
	}

	/**
	 * Capture the latency of this component
	 */
	private void mark(Component comp) {
		// Add the latency of all exits
		for (Exit exit : comp.getExits()) {
			if (exit.getLatency() != null)
				currentLatencies.add(exit.getLatency());
			else
				System.out.println("WARNING!!! The latency of exit " + exit
						+ " of " + comp + " is null");
		}
	}

	private void enterModule() {
		context.push(currentLatencies);
		currentLatencies = new HashSet<Latency>();
	}

	private Set<Latency> exitModule(Module module) {
		// All we care about is the difference between sequential and
		// non sequential.
		final Map<Latency, Latency> allLatencies = new HashMap<Latency, Latency>();
		for (Latency lat : currentLatencies) {
			allLatencies.put(lat, lat);
		}
		final Map<?, Latency> maxLatencies = Latency.getLatest(allLatencies);
		final Set<Latency> max = new HashSet<Latency>(maxLatencies.values());

		// Also pick up any dependencies
		for (Component comp : module.getComponents()) {
			for (Entry entry : comp.getEntries()) {
				for (Dependency dep : entry.getDependencies(comp.getGoPort())) {
					if (dep.getDelayClocks() > 0)
						max.add(Latency.ONE);
				}
			}
		}

		currentLatencies = context.pop();
		currentLatencies.addAll(max);

		return new HashSet<Latency>(max);
	}

	private void handleModule(Module mod) {
		enterModule();
		for (Component component : mod.getComponents()) {
			component.accept(this);
		}
		exitModule(mod);
	}

	@Override
	public void visit(Design design) {
		for (Task task : design.getTasks()) {
			task.accept(this);
		}
	}

	@Override
	public void visit(Task task) {
		isInLoopContext = false;
		if (task.getCall() != null)
			task.getCall().accept(this);
	}

	@Override
	public void visit(Call call) {
		mark(call);
		if (call.getProcedure() != null)
			call.getProcedure().accept(this);
	}

	@Override
	public void visit(Procedure procedure) {
		if (procedure.getBody() != null)
			procedure.getBody().accept(this);
	}

	@Override
	public void visit(Block block) {
		handleModule(block);
	}

	@Override
	public void visit(Loop loop) {
		boolean oldContext = isInLoopContext;
		isInLoopContext = true;
		handleModule(loop);
		isInLoopContext = oldContext;
	}

	@Override
	public void visit(WhileBody mod) {
		handleModule(mod);
	}

	@Override
	public void visit(UntilBody mod) {
		handleModule(mod);
	}

	@Override
	public void visit(ForBody mod) {
		handleModule(mod);
	}

	@Override
	public void visit(Decision mod) {
		handleModule(mod);
	}

	@Override
	public void visit(Switch mod) {
		handleModule(mod);
	}

	@Override
	public void visit(HeapRead mod) {
		handleModule(mod);
	}

	@Override
	public void visit(HeapWrite mod) {
		handleModule(mod);
	}

	@Override
	public void visit(ArrayRead mod) {
		handleModule(mod);
	}

	@Override
	public void visit(ArrayWrite mod) {
		handleModule(mod);
	}

	@Override
	public void visit(TaskCall comp) {
		handleModule(comp.getTarget().getCall().getProcedure().getBody());
	}

	@Override
	public void visit(AddOp comp) {
		mark(comp);
	}

	@Override
	public void visit(AndOp comp) {
		mark(comp);
	}

	@Override
	public void visit(CastOp comp) {
		mark(comp);
	}

	@Override
	public void visit(ComplementOp comp) {
		mark(comp);
	}

	@Override
	public void visit(ConditionalAndOp comp) {
		mark(comp);
	}

	@Override
	public void visit(ConditionalOrOp comp) {
		mark(comp);
	}

	@Override
	public void visit(Constant comp) {
		mark(comp);
	}

	@Override
	public void visit(DivideOp comp) {
		mark(comp);
	}

	@Override
	public void visit(EqualsOp comp) {
		mark(comp);
	}

	@Override
	public void visit(GreaterThanEqualToOp comp) {
		mark(comp);
	}

	@Override
	public void visit(GreaterThanOp comp) {
		mark(comp);
	}

	@Override
	public void visit(Latch vis) {
		mark(vis);
	}

	@Override
	public void visit(LeftShiftOp comp) {
		mark(comp);
	}

	@Override
	public void visit(LessThanEqualToOp comp) {
		mark(comp);
	}

	@Override
	public void visit(LessThanOp comp) {
		mark(comp);
	}

	@Override
	public void visit(LocationConstant comp) {
		mark(comp);
	}

	@Override
	public void visit(MinusOp comp) {
		mark(comp);
	}

	@Override
	public void visit(ModuloOp comp) {
		mark(comp);
	}

	@Override
	public void visit(MultiplyOp comp) {
		mark(comp);
	}

	@Override
	public void visit(NotEqualsOp comp) {
		mark(comp);
	}

	@Override
	public void visit(NotOp comp) {
		mark(comp);
	}

	@Override
	public void visit(OrOp comp) {
		mark(comp);
	}

	@Override
	public void visit(PlusOp comp) {
		mark(comp);
	}

	@Override
	public void visit(ReductionOrOp comp) {
		mark(comp);
	}

	@Override
	public void visit(RightShiftOp comp) {
		mark(comp);
	}

	@Override
	public void visit(RightShiftUnsignedOp comp) {
		mark(comp);
	}

	@Override
	public void visit(ShortcutIfElseOp comp) {
		mark(comp);
	}

	@Override
	public void visit(SubtractOp comp) {
		mark(comp);
	}

	@Override
	public void visit(NumericPromotionOp comp) {
		mark(comp);
	}

	@Override
	public void visit(XorOp comp) {
		mark(comp);
	}

	@Override
	public void visit(InBuf comp) {
		mark(comp);
	}

	@Override
	public void visit(OutBuf comp) {
		mark(comp);
	}

	@Override
	public void visit(Reg comp) {
		mark(comp);
	}

	@Override
	public void visit(Mux comp) {
		mark(comp);
	}

	@Override
	public void visit(EncodedMux comp) {
		mark(comp);
	}

	@Override
	public void visit(PriorityMux comp) {
		mark(comp);
	}

	@Override
	public void visit(And comp) {
		mark(comp);
	}

	@Override
	public void visit(Not comp) {
		mark(comp);
	}

	@Override
	public void visit(Or comp) {
		mark(comp);
	}

	@Override
	public void visit(NoOp comp) {
		mark(comp);
	}

	@Override
	public void visit(TimingOp comp) {
		mark(comp);
	}

	@Override
	public void visit(RegisterRead comp) {
		mark(comp);
	}

	@Override
	public void visit(RegisterWrite comp) {
		mark(comp);
	}

	@Override
	public void visit(MemoryRead comp) {
		mark(comp);
	}

	@Override
	public void visit(MemoryWrite comp) {
		mark(comp);
	}

	@Override
	public void visit(AbsoluteMemoryRead comp) {
		mark(comp);
	}

	@Override
	public void visit(AbsoluteMemoryWrite comp) {
		mark(comp);
	}

	@Override
	public void visit(SRL16 comp) {
		mark(comp);
	}

	@Override
	public void visit(SimplePinAccess comp) {
		mark(comp);
	}

	@Override
	public void visit(SimplePinRead comp) {
		mark(comp);
	}

	@Override
	public void visit(SimplePinWrite comp) {
		mark(comp);
	}

	@Override
	public void visit(FifoAccess comp) {
		mark(comp);
	}

	@Override
	public void visit(FifoRead comp) {
		mark(comp);
	}

	@Override
	public void visit(FifoWrite comp) {
		mark(comp);
	}

	/**
	 * Unexpected elements
	 */
	@Override
	public void visit(IPCoreCall vis) {
		fail(vis);
	}

	@Override
	public void visit(Scoreboard vis) {
		fail(vis);
	}

	@Override
	public void visit(RegisterGateway vis) {
		fail(vis);
	}

	@Override
	public void visit(RegisterReferee vis) {
		fail(vis);
	}

	@Override
	public void visit(MemoryReferee vis) {
		fail(vis);
	}

	@Override
	public void visit(MemoryGateway vis) {
		fail(vis);
	}

	@Override
	public void visit(MemoryBank vis) {
		fail(vis);
	}

	@Override
	public void visit(Kicker vis) {
		fail(vis);
	}

	@Override
	public void visit(PinRead vis) {
		fail(vis);
	}

	@Override
	public void visit(PinWrite vis) {
		fail(vis);
	}

	@Override
	public void visit(PinStateChange vis) {
		fail(vis);
	}

	@Override
	public void visit(PinReferee vis) {
		fail(vis);
	}

	@Override
	public void visit(TriBuf vis) {
		fail(vis);
	}

	@Override
	public void visit(SimplePin vis) {
		fail(vis);
	}

	@Override
	public void visit(EndianSwapper vis) {
		fail(vis);
	}

	protected void fail(Visitable vis) {
		if (vis instanceof Component)
			System.out.println(((Component) vis).showOwners());
		EngineThread.getEngine().fatalError(
				"Internal error at: LoopConditionalBranchBalancer.  Unexpected traversal of "
						+ vis + " encountered");
	}

}
