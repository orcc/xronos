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

package net.sf.openforge.schedule.loop;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import net.sf.openforge.app.EngineThread;
import net.sf.openforge.lim.ArrayRead;
import net.sf.openforge.lim.ArrayWrite;
import net.sf.openforge.lim.Block;
import net.sf.openforge.lim.Branch;
import net.sf.openforge.lim.Call;
import net.sf.openforge.lim.Component;
import net.sf.openforge.lim.Decision;
import net.sf.openforge.lim.Dependency;
import net.sf.openforge.lim.Design;
import net.sf.openforge.lim.Entry;
import net.sf.openforge.lim.Exit;
import net.sf.openforge.lim.ForBody;
import net.sf.openforge.lim.HeapRead;
import net.sf.openforge.lim.HeapWrite;
import net.sf.openforge.lim.IPCoreCall;
import net.sf.openforge.lim.InBuf;
import net.sf.openforge.lim.Kicker;
import net.sf.openforge.lim.Latch;
import net.sf.openforge.lim.Latency;
import net.sf.openforge.lim.Loop;
import net.sf.openforge.lim.Module;
import net.sf.openforge.lim.OutBuf;
import net.sf.openforge.lim.PinRead;
import net.sf.openforge.lim.PinReferee;
import net.sf.openforge.lim.PinStateChange;
import net.sf.openforge.lim.PinWrite;
import net.sf.openforge.lim.PriorityMux;
import net.sf.openforge.lim.Procedure;
import net.sf.openforge.lim.RegisterGateway;
import net.sf.openforge.lim.RegisterRead;
import net.sf.openforge.lim.RegisterReferee;
import net.sf.openforge.lim.RegisterWrite;
import net.sf.openforge.lim.ResourceDependency;
import net.sf.openforge.lim.Scoreboard;
import net.sf.openforge.lim.Switch;
import net.sf.openforge.lim.Task;
import net.sf.openforge.lim.TaskCall;
import net.sf.openforge.lim.TriBuf;
import net.sf.openforge.lim.UntilBody;
import net.sf.openforge.lim.Visitable;
import net.sf.openforge.lim.Visitor;
import net.sf.openforge.lim.WhileBody;
import net.sf.openforge.lim.io.FifoAccess;
import net.sf.openforge.lim.io.FifoRead;
import net.sf.openforge.lim.io.FifoWrite;
import net.sf.openforge.lim.io.SimplePin;
import net.sf.openforge.lim.io.SimplePinAccess;
import net.sf.openforge.lim.io.SimplePinRead;
import net.sf.openforge.lim.io.SimplePinWrite;
import net.sf.openforge.lim.memory.AbsoluteMemoryRead;
import net.sf.openforge.lim.memory.AbsoluteMemoryWrite;
import net.sf.openforge.lim.memory.EndianSwapper;
import net.sf.openforge.lim.memory.LocationConstant;
import net.sf.openforge.lim.memory.MemoryBank;
import net.sf.openforge.lim.memory.MemoryGateway;
import net.sf.openforge.lim.memory.MemoryRead;
import net.sf.openforge.lim.memory.MemoryReferee;
import net.sf.openforge.lim.memory.MemoryWrite;
import net.sf.openforge.lim.op.AddOp;
import net.sf.openforge.lim.op.AndOp;
import net.sf.openforge.lim.op.CastOp;
import net.sf.openforge.lim.op.ComplementOp;
import net.sf.openforge.lim.op.ConditionalAndOp;
import net.sf.openforge.lim.op.ConditionalOrOp;
import net.sf.openforge.lim.op.Constant;
import net.sf.openforge.lim.op.DivideOp;
import net.sf.openforge.lim.op.EqualsOp;
import net.sf.openforge.lim.op.GreaterThanEqualToOp;
import net.sf.openforge.lim.op.GreaterThanOp;
import net.sf.openforge.lim.op.LeftShiftOp;
import net.sf.openforge.lim.op.LessThanEqualToOp;
import net.sf.openforge.lim.op.LessThanOp;
import net.sf.openforge.lim.op.MinusOp;
import net.sf.openforge.lim.op.ModuloOp;
import net.sf.openforge.lim.op.MultiplyOp;
import net.sf.openforge.lim.op.NoOp;
import net.sf.openforge.lim.op.NotEqualsOp;
import net.sf.openforge.lim.op.NotOp;
import net.sf.openforge.lim.op.NumericPromotionOp;
import net.sf.openforge.lim.op.OrOp;
import net.sf.openforge.lim.op.PlusOp;
import net.sf.openforge.lim.op.ReductionOrOp;
import net.sf.openforge.lim.op.RightShiftOp;
import net.sf.openforge.lim.op.RightShiftUnsignedOp;
import net.sf.openforge.lim.op.ShortcutIfElseOp;
import net.sf.openforge.lim.op.SubtractOp;
import net.sf.openforge.lim.op.TimingOp;
import net.sf.openforge.lim.op.XorOp;
import net.sf.openforge.lim.primitive.And;
import net.sf.openforge.lim.primitive.EncodedMux;
import net.sf.openforge.lim.primitive.Mux;
import net.sf.openforge.lim.primitive.Not;
import net.sf.openforge.lim.primitive.Or;
import net.sf.openforge.lim.primitive.Reg;
import net.sf.openforge.lim.primitive.SRL16;

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
