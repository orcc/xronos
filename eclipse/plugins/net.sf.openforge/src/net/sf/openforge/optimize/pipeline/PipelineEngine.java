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

package net.sf.openforge.optimize.pipeline;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import net.sf.openforge.app.EngineThread;
import net.sf.openforge.app.OptionRegistry;
import net.sf.openforge.app.project.OptionInt;
import net.sf.openforge.lim.And;
import net.sf.openforge.lim.ArrayRead;
import net.sf.openforge.lim.ArrayWrite;
import net.sf.openforge.lim.Block;
import net.sf.openforge.lim.Bus;
import net.sf.openforge.lim.Call;
import net.sf.openforge.lim.ClockDependency;
import net.sf.openforge.lim.Component;
import net.sf.openforge.lim.ControlDependency;
import net.sf.openforge.lim.DataDependency;
import net.sf.openforge.lim.DataFlowVisitor;
import net.sf.openforge.lim.Dependency;
import net.sf.openforge.lim.Design;
import net.sf.openforge.lim.EncodedMux;
import net.sf.openforge.lim.Entry;
import net.sf.openforge.lim.Exit;
import net.sf.openforge.lim.HeapRead;
import net.sf.openforge.lim.HeapWrite;
import net.sf.openforge.lim.InBuf;
import net.sf.openforge.lim.Latch;
import net.sf.openforge.lim.Loop;
import net.sf.openforge.lim.Module;
import net.sf.openforge.lim.Mux;
import net.sf.openforge.lim.Not;
import net.sf.openforge.lim.Operation;
import net.sf.openforge.lim.Or;
import net.sf.openforge.lim.OutBuf;
import net.sf.openforge.lim.PinRead;
import net.sf.openforge.lim.PinStateChange;
import net.sf.openforge.lim.PinWrite;
import net.sf.openforge.lim.Port;
import net.sf.openforge.lim.Reference;
import net.sf.openforge.lim.Reg;
import net.sf.openforge.lim.RegisterRead;
import net.sf.openforge.lim.RegisterWrite;
import net.sf.openforge.lim.ResetDependency;
import net.sf.openforge.lim.ResourceDependency;
import net.sf.openforge.lim.SRL16;
import net.sf.openforge.lim.Scoreboard;
import net.sf.openforge.lim.Task;
import net.sf.openforge.lim.TaskCall;
import net.sf.openforge.lim.TriBuf;
import net.sf.openforge.lim.io.FifoAccess;
import net.sf.openforge.lim.io.FifoRead;
import net.sf.openforge.lim.io.FifoWrite;
import net.sf.openforge.lim.io.SimplePinAccess;
import net.sf.openforge.lim.io.SimplePinRead;
import net.sf.openforge.lim.io.SimplePinWrite;
import net.sf.openforge.lim.memory.MemoryRead;
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
import net.sf.openforge.util.naming.ID;

/**
 * PipelineEngine is the visitor for pipelining a design. PipelineEngine only
 * attempts to pipeline within a Module.
 * <p>
 * This class must visit components in the order that they execute and preserve
 * the schedule. In order to assure this, the components are visited in Data
 * Flow order (as defined by the {@link DataFlowVisitor}). In addition to data
 * flow order, the sequencing of {@link Reference}s must be preserved. This is
 * accomplished by ensuring that each component is delayed by at least as many
 * cycles as any other component upon which it has a {@link ResourceDependency}.
 * <p>
 * By ensuring that resource dependencies are correctly handled, and by
 * modifying DataFlowVisitor to ensure that it visits only after all data flows
 * <b>and</b> all resource dependencies have been satisfied, the protections in
 * this class against components that have not been fully resolved could be
 * removed. Check out version 1.18 of this class to see the protections in
 * place.
 * <p>
 * July 05, 2006. The pipelining engine has been modified to allow different
 * target gate depth at each Module scope. If the gate depth is specified for a
 * particular scope, then it will apply to that scope and ALL logic within that
 * scope unless re-set at one or more of the contained scopes.
 * 
 * @author cschanck, from ysyu &amp; cwu, imiller
 * @version $Id: PipelineEngine.java 344 2006-09-25 14:27:18Z imiller $
 */
class PipelineEngine extends DataFlowVisitor implements _pipeline.Debug {

	private boolean doPipeline = false;
	private int targetGateDepth;

	/** Map of Exit to cumulative gate depth Integer */
	private Map<Exit, Integer> exitToGateDepthMap = new HashMap<Exit, Integer>();

	/** A set of modules which do NOT get traversed internally */
	private Set<Module> atomicModules = new HashSet<Module>();

	private int taskMaxGateDepth;

	private int designMaxGateDepth;

	private int unbreakableGateDepth;

	private int pipelineCount;

	PipelineEngine() {
		super();
		setRunForward(true);
		doPipeline = false;
	}

	public void pipeline(Design d) {
		clear();
		doPipeline = false;
		if (_d) {
			_dbg.pushPreface(" P1");
		}
		d.accept(this);
	}

	public void pipeline(Design d, int targetGateDepth) {
		clear();
		doPipeline = true;
		this.targetGateDepth = targetGateDepth;
		if (_d) {
			_dbg.pushPreface(" P2");
		}
		d.accept(this);
	}

	private void clear() {
		exitToGateDepthMap.clear();
		atomicModules.clear();
		taskMaxGateDepth = 0;
		designMaxGateDepth = 0;
		unbreakableGateDepth = 0;
		pipelineCount = 0;
	}

	public int getPipelineCount() {
		return pipelineCount;
	}

	/**
	 * Responsible for traversing within a Module
	 */
	@Override
	protected void traverse(Module module) {
		if (_d) {
			_dbg.ln("Module Traversal " + module);
		}
		zeroFeedbackExits(module);

		// Allow the target gate depth to be specified on a module by
		// module basis
		final int oldTargetGateDepth = targetGateDepth;
		final int spec_level = ((OptionInt) EngineThread.getGenericJob()
				.getOption(OptionRegistry.SCHEDULE_PIPELINE_GATE_DEPTH))
				.getValueAsInt(module.getSearchLabel());
		targetGateDepth = spec_level;
		if (_d) {
			_dbg.ln("Module " + module + " TGD set to " + targetGateDepth);
		}
		super.traverse(module);
		targetGateDepth = oldTargetGateDepth;
	}

	@Override
	public void visit(Design design) {
		preFilter(design);

		// initialize a design's gate depth
		designMaxGateDepth = 0;
		unbreakableGateDepth = 0;

		traverse(design);

		// set a design's max gate depth
		design.setMaxGateDepth(designMaxGateDepth);
		design.setUnbreakableGateDepth(unbreakableGateDepth);

		postFilter(design);

		if (_d) {
			_dbg.ln(design + ", maximum gate depth: "
					+ design.getMaxGateDepth());
		}
		if (_d) {
			_dbg.ln(design + ", unbreakable gate depth: "
					+ design.getUnbreakableGateDepth());
		}
	}

	@Override
	public void visit(Task task) {
		preFilter(task);

		// clear the field variables which store information within a task
		// scope.
		exitToGateDepthMap.clear();
		atomicModules.clear();
		taskMaxGateDepth = 0;

		traverse(task);

		// set a task's max gate depth
		task.setMaxGateDepth(taskMaxGateDepth);

		// set design's gate depth to be the largest gate depth among it's
		// tasks.
		if (taskMaxGateDepth >= designMaxGateDepth) {
			designMaxGateDepth = taskMaxGateDepth;
		}

		postFilter(task);

		if (_d) {
			_dbg.ln(task + ", maximum gate depth: " + task.getMaxGateDepth());
		}
	}

	@Override
	public void visit(Call call) {
		preFilter(call);

		// propagate the gate depth to the procedure ports peer
		// buses which belong to a procedure body's inBuf.
		// int maxInputDepth = (call.getOwner() != null) ?
		// getMaxInputGateDepth(call) : 0;
		// IDM. Ugly hack, but there is no way to be sure we will have
		// traversed the top level design.
		int maxInputDepth = (call.getOwner() instanceof Design.DesignModule) ? 0
				: getMaxInputGateDepth(call);
		if (call.getProcedure() != null
				&& call.getProcedure().getBody() != null) {
			// Add in the inbuf exit depth. Always 0 except at top
			// level (task) call to account for input buffer depth.
			maxInputDepth += call.getProcedure().getBody().getInBuf()
					.getExitGateDepth();
		}
		final Integer currentInputGateDepth = new Integer(maxInputDepth);
		for (Port callPort : call.getDataPorts()) {
			Bus peerBus = call.getProcedurePort(callPort).getPeer();
			exitToGateDepthMap.put(peerBus.getOwner(), currentInputGateDepth);
		}

		if (_d) {
			_dbg.ln("\t*** " + call + ", current input gate depth: "
					+ currentInputGateDepth);
		}

		final int oldTargetGateDepth = targetGateDepth;
		final int spec_level = ((OptionInt) EngineThread.getGenericJob()
				.getOption(OptionRegistry.SCHEDULE_PIPELINE_GATE_DEPTH))
				.getValueAsInt(call.getProcedure().getSearchLabel());
		targetGateDepth = spec_level;
		if (_d) {
			_dbg.ln("For Call " + call + " the gate depth level is "
					+ targetGateDepth);
		}
		traverse(call);
		targetGateDepth = oldTargetGateDepth;

		for (Exit callExit : call.getExits()) {
			Integer currentOutputGateDepth = exitToGateDepthMap.get(call
					.getProcedureExit(callExit));
			exitToGateDepthMap.put(callExit, currentOutputGateDepth);
		}

		postFilter(call);

		if (_d) {
			_dbg.ln("\t*** " + call + ", current output gate depth: "
					+ exitToGateDepthMap.get(call.getAnyExit()).intValue());
		}
	}

	@Override
	public void visit(Loop loop) {
		visit((Component) loop);
		super.visit(loop);
	}

	@Override
	public void visit(InBuf inBuf) {
		preFilter(inBuf);

		if (_d) {
			_dbg.ln("\tinbuf " + inBuf + " gateDepth: "
					+ inBuf.getExitGateDepth() + "");
		}
		// final Integer currentGateDepth = new
		// Integer(getMaxInputGateDepth(inBuf)+inBuf.getExitGateDepth());
		final Integer currentGateDepth = new Integer(inBuf.getExitGateDepth());
		for (Exit exit : inBuf.getExits()) {
			exitToGateDepthMap.put(exit, currentGateDepth);
		}
		if (currentGateDepth.intValue() >= taskMaxGateDepth) {
			taskMaxGateDepth = currentGateDepth.intValue();
		}

		postFilter(inBuf);

		if (_d) {
			_dbg.ln("\t*** " + inBuf + ", current gate depth: "
					+ currentGateDepth.intValue());
		}
	}

	@Override
	public void visit(OutBuf outBuf) {
		visit((Component) outBuf);

		final Integer currentGateDepth = new Integer(
				getMaxInputGateDepth(outBuf) + outBuf.getExitGateDepth());
		for (Port port : outBuf.getPorts()) {
			final Bus moduleBus = port.getPeer();
			if (moduleBus != null) {
				exitToGateDepthMap.put(moduleBus.getOwner(), currentGateDepth);
			}
		}

		postFilter(outBuf);

		if (_d) {
			_dbg.ln("\t*** " + outBuf + ", current gate depth: "
					+ currentGateDepth.intValue());
		}
	}

	@Override
	public void visit(Reg reg) {
		// For any enabled reg make sure that the enable signal and
		// the data port have been delayed by the same amount. I have
		// no test case for this as of yet, but I found a bug in the
		// old java version that was fixed by this visit line.
		// IDM 06/10/2004
		visit((Component) reg);

		preFilter(reg);
		traverse(reg);
		// this reset the gate depth to 0 for the result bus -- makes sense;-)
		exitToGateDepthMap.put(reg.getBuses().iterator().next().getOwner(),
				new Integer(0));

		postFilter(reg);
	}

	@Override
	public void visit(Latch latch) {
		visitAtomic(latch);
	}

	@Override
	public void visit(Constant constant) {
		visit((Component) constant);
	}

	@Override
	public void visit(EncodedMux encodedMux) {
		visit((Component) encodedMux);
	}

	@Override
	public void visit(And and) {
		visit((Component) and);
	}

	@Override
	public void visit(Not not) {
		visit((Component) not);
	}

	@Override
	public void visit(Or or) {
		visit((Component) or);
	}

	@Override
	public void visit(AddOp op) {
		visit((Component) op);
	}

	@Override
	public void visit(AndOp op) {
		visit((Component) op);
	}

	@Override
	public void visit(CastOp op) {
		visit((Component) op);
	}

	@Override
	public void visit(ComplementOp op) {
		visit((Component) op);
	}

	@Override
	public void visit(ConditionalAndOp op) {
		visit((Component) op);
	}

	@Override
	public void visit(ConditionalOrOp op) {
		visit((Component) op);
	}

	@Override
	public void visit(DivideOp op) {
		visit((Component) op);
	}

	@Override
	public void visit(EqualsOp op) {
		visit((Component) op);
	}

	@Override
	public void visit(GreaterThanEqualToOp op) {
		visit((Component) op);
	}

	@Override
	public void visit(GreaterThanOp op) {
		visit((Component) op);
	}

	@Override
	public void visit(LeftShiftOp op) {
		visit((Component) op);
	}

	@Override
	public void visit(LessThanEqualToOp op) {
		visit((Component) op);
	}

	@Override
	public void visit(LessThanOp op) {
		visit((Component) op);
	}

	@Override
	public void visit(MinusOp op) {
		visit((Component) op);
	}

	@Override
	public void visit(ModuloOp op) {
		visit((Component) op);
	}

	@Override
	public void visit(MultiplyOp op) {
		visit((Component) op);
	}

	@Override
	public void visit(NotEqualsOp op) {
		visit((Component) op);
	}

	@Override
	public void visit(NoOp op) {
		visit((Component) op);
	}

	@Override
	public void visit(TimingOp op) {
		visit((Component) op);
	}

	@Override
	public void visit(NotOp op) {
		visit((Component) op);
	}

	@Override
	public void visit(OrOp op) {
		visit((Component) op);
	}

	@Override
	public void visit(PlusOp op) {
		visit((Component) op);
	}

	@Override
	public void visit(ReductionOrOp op) {
		visit((Component) op);
	}

	@Override
	public void visit(RightShiftOp op) {
		visit((Component) op);
	}

	@Override
	public void visit(RightShiftUnsignedOp op) {
		visit((Component) op);
	}

	@Override
	public void visit(ShortcutIfElseOp op) {
		visit((Component) op);
	}

	@Override
	public void visit(SubtractOp op) {
		visit((Component) op);
	}

	@Override
	public void visit(NumericPromotionOp op) {
		visit((Component) op);
	}

	@Override
	public void visit(XorOp op) {
		visit((Component) op);
	}

	@Override
	public void visit(Mux mux) {
		visit((Component) mux);
	}

	@Override
	public void visit(RegisterRead regRead) {
		visit((Component) regRead);
	}

	@Override
	public void visit(RegisterWrite regWrite) {
		visit((Component) regWrite);
	}

	@Override
	public void visit(MemoryRead memoryRead) {
		visit((Component) memoryRead);
	}

	@Override
	public void visit(MemoryWrite memoryWrite) {
		visit((Component) memoryWrite);
	}

	@Override
	public void visit(PinRead pinRead) {
		visit((Component) pinRead);
	}

	@Override
	public void visit(PinWrite pinWrite) {
		visit((Component) pinWrite);
	}

	@Override
	public void visit(PinStateChange pinStateChange) {
		visit((Component) pinStateChange);
	}

	@Override
	public void visit(SRL16 srl16) {
		visit((Component) srl16);
	}

	@Override
	public void visit(TriBuf triBuf) {
		visit((Component) triBuf);
	}

	/**
	 * TaskCall is a module that is pre-constructed internally, and as such we
	 * do not want any registers inserted into it. Visit it as a component and
	 * it will be treated atomically.
	 */
	@Override
	public void visit(TaskCall comp) {
		visitAtomic(comp);
	}

	/**
	 * SimplePinAccess is a module that is pre-constructed internally, and as
	 * such we do not want any registers inserted into it. Visit it as a
	 * component and it will be treated atomically.
	 */
	@Override
	public void visit(SimplePinAccess comp) {
		visitAtomic(comp);
	}

	@Override
	public void visit(SimplePinRead comp) {
		visit((Component) comp);
	}

	@Override
	public void visit(SimplePinWrite comp) {
		visit((Component) comp);
	}

	/**
	 * FifoAccess is a module that is pre-constructed internally, and as such we
	 * do not want any registers inserted into it. Visit it as a component and
	 * it will be treated atomically.
	 */
	@Override
	public void visit(FifoAccess comp) {
		visitAtomic(comp);
	}

	/**
	 * FifoRead is a module that is pre-constructed internally, and as such we
	 * do not want any registers inserted into it. Visit it as a component and
	 * it will be treated atomically.
	 */
	@Override
	public void visit(FifoRead comp) {
		visitAtomic(comp);
	}

	/**
	 * FifoWrite is a module that is pre-constructed internally, and as such we
	 * do not want any registers inserted into it. Visit it as a component and
	 * it will be treated atomically.
	 */
	@Override
	public void visit(FifoWrite comp) {
		visitAtomic(comp);
	}

	/**
	 * Scoreboard is a module that is pre-constructed internally, and as such we
	 * do not want any registers inserted into it. Visit it as a component and
	 * it will be treated atomically.
	 */
	@Override
	public void visit(Scoreboard comp) {
		visitAtomic(comp);
	}

	// CRSS XXX put hese back int -- ask IDM
	@Override
	public void visit(ArrayRead comp) {
		visitAtomic(comp);
	}

	@Override
	public void visit(ArrayWrite comp) {
		visitAtomic(comp);
	}

	@Override
	public void visit(HeapRead comp) {
		visitAtomic(comp);
	}

	@Override
	public void visit(HeapWrite comp) {
		visitAtomic(comp);
	}

	private void visitAtomic(Module module) {
		atomicModules.add(module);
		visit(module);
	}

	public void visit(Component component) {
		preFilter(component);

		insertPipelineRegister(component);
		unbreakableGateDepth = Math.max(unbreakableGateDepth,
				component.getExitGateDepth());

		// reset this...
		int maxInputGateDepth = getMaxInputGateDepth(component);
		final Integer currentGateDepth = new Integer(maxInputGateDepth
				+ component.getExitGateDepth());
		for (Exit exit : component.getExits()) {
			exitToGateDepthMap.put(exit, currentGateDepth);
		}
		if (currentGateDepth.intValue() >= taskMaxGateDepth) {
			taskMaxGateDepth = currentGateDepth.intValue();
		}
		postFilter(component);
		if (_d) {
			_dbg.ln("\t*** " + component + ", current gate depth: "
					+ currentGateDepth.intValue());
		}
	}

	/**
	 * Record all feedback compoenents's exits as gate depth 0. Loop has to stop
	 * somewhere, right?
	 * 
	 * @param m
	 *            a value of type 'Module'
	 */
	private void zeroFeedbackExits(Module m) {
		for (Component c : m.getFeedbackPoints()) {
			for (Exit ex : c.getExits()) {
				exitToGateDepthMap.put(ex, new Integer(0));
			}
		}
	}

	/**
	 * Find the max input gate depth to this compoenent, relying solely on
	 * dependencies. Basically, the worst input gate depth to this component
	 * 
	 * @param component
	 *            a value of type 'Component'
	 * @return a value of type 'int' 0-N for known gate depth
	 */
	private int getMaxInputGateDepth(Component component) {
		int maxGateDepth = 0;
		// component = (component instanceof InBuf) ? component.getOwner() :
		// component;
		if (_d) {
			_dbg.ln("getting MaxInputDepth of " + component);
		}
		// for each input (data) port of the component
		for (Port port : component.getDataPorts()) {
			if (_d) {
				_dbg.ln("\tport " + port);
			}
			maxGateDepth = Math.max(maxGateDepth, getMaxInputGateDepth(port));
		}
		if (_d) {
			_dbg.ln("got " + maxGateDepth);
		}
		return maxGateDepth;
	}

	private int getMaxInputGateDepth(Port port) {
		int maxGateDepth = 0;
		// for each dependent bus...
		for (Bus b : getDependentBuses(port)) {
			if (_d) {
				_dbg.ln("\t\tbus " + b + " " + b.getOwner() + " "
						+ b.getOwner().getOwner());
			}
			int gateDepth;
			// If the source bus comes from an inbuf or outbuf, then
			// get the gatedepth from the corresponding peer. Unless
			// of course, we did not traverse inside the source module.
			if (b.getPeer() != null
					&& !atomicModules.contains(b.getOwner().getOwner())) {
				gateDepth = getMaxInputGateDepth(b.getPeer());
			} else {
				gateDepth = exitToGateDepthMap.get(b.getOwner()).intValue();
			}
			maxGateDepth = Math.max(maxGateDepth, gateDepth);
		}
		return maxGateDepth;
	}

	/**
	 * Gets the collection of buses upon which the port depends.
	 */
	private static Collection<Bus> getDependentBuses(Port port) {
		final Set<Bus> set = new HashSet<Bus>();
		final Component component = port.getOwner();
		for (Entry entry : component.getEntries()) {
			for (Dependency dep : entry.getDependencies(port)) {
				set.add(dep.getLogicalBus());
			}
		}
		return set;
	}

	private void insertPipelineRegister(Component c) {
		//
		// When comparing against the target depth we need to consider
		// the depth of the path up to this component PLUS the entry
		// depth of the component. If the SUM of those two would take
		// us over the limit, then we need to put a register on that
		// input to break the path. This ensures that NO path in the
		// design exceeds the target, unless caused by a component
		// whose internal depth exceeds the target.
		//

		// get the depth of the entry into this compoenent
		final int entryDepth = c.getEntryGateDepth();
		// Module parent = c.getOwner();
		assert (!(c instanceof InBuf)); // should never happen

		if (_d) {
			_dbg.ln("pipelining " + c);
		}

		// for each input (data) port of the component
		for (Port port : c.getDataPorts()) {
			if (_d) {
				_dbg.ln("\tport " + port);
			}
			// across each entry
			for (Entry entry : c.getEntries()) {
				// for each dependency
				for (Dependency dep : entry.getDependencies(port)) {
					// get the bus
					final Bus bus = dep.getLogicalBus();

					if (_d) {
						_dbg.ln("\t\tbus " + bus + " owner " + bus.getOwner()
								+ " " + bus.getOwner().getOwner());
					}
					// get the gatedepth for this port's bus...
					final int gateDepth = exitToGateDepthMap
							.get(bus.getOwner()).intValue();
					// get the cum depth
					final int cumInputDepth = gateDepth + entryDepth;
					if (_d) {
						_dbg.ln("Comp: " + c + " entryDepth " + entryDepth
								+ " inputDepth " + gateDepth + " cum "
								+ cumInputDepth + " target " + targetGateDepth);
					}

					// if above the targetdepth, and gatedepth and entrydepth
					// are non zero, go for it...
					if ((cumInputDepth > targetGateDepth) && (gateDepth > 0)
							&& (entryDepth > 0) && (targetGateDepth > 0)) {
						// Don't pipeline incoming buses that are constants.
						if (!bus.getValue().isConstant()
								&& !bus.getOwner().getOwner().isConstant()) {
							if (_d) {
								_dbg.ln("\tBingo!");
							}
							if (doPipeline) {
								insertPipelineRegister(entry, dep, port, bus);
							}
							pipelineCount++;// increment number of registers
											// added
						}
					}
				}
			}
		}

	}

	/**
	 * Put a register at the connected inputs of this component
	 * 
	 * @param component
	 *            a component
	 */
	private void insertPipelineRegister(Entry entry, Dependency dep,
			Port inputPort, Bus drivingBus) {
		if (_d) {
			_dbg.ln("Registering port " + inputPort + " "
					+ inputPort.getOwner());
		}

		Module parent = inputPort.getOwner().getOwner();

		if (_d) {
			_dbg.ln("\tnew reg: ");
		}
		// create it.... Needs a reset if it is in the control path
		// (which could mean that it is not on a go port OR some
		// non-operation type logic)
		final Reg reg;
		final int type;
		if (inputPort == inputPort.getOwner().getGoPort()
				|| !(inputPort.getOwner() instanceof Operation)) {
			type = Reg.REGR;
		} else {
			type = Reg.REG;
		}
		reg = Reg.getConfigurableReg(type, ID.showLogical(drivingBus)
				+ "_pipeline");

		// I think these are needed
		reg.makeEntry();
		reg.getDataPort().setUsed(true);
		// here we have to insert reg into lim
		// add it as a comp
		// be clever, it might be a block
		if (parent instanceof Block) {
			Block bParent = (Block) parent;
			// first find the offset
			int offset = bParent.getSequence().indexOf(inputPort.getOwner());
			assert (offset >= 0);
			// insert the new comp
			bParent.insertComponent(reg, offset);
		} else {
			inputPort.getOwner().getOwner().addComponent(reg);
		}

		// now connect the reg...
		// so:
		// 1) remove current data dependency of inputPort on drivingBus
		// 2) add data dependency of 'inputPort' on resultbus of reg
		// 3) add data dependency of reg.dataPort on driving bus
		// 4) add control dependencies

		// create 2 dependencies, 1 to input to the reg, one to the port
		DataDependency regInDep = new DataDependency(drivingBus);
		Dependency portInDep = dep.createSameType(reg.getResultBus());
		// wax old dep
		dep.zap();

		// connect reg to old driving bus dep
		reg.getMainEntry().addDependency(reg.getDataPort(), regInDep);
		// connect old port to result bus dep
		entry.addDependency(inputPort, portInDep);

		// control dep time
		echoControlDependencies(reg, entry.getOwner());

		// size it
		reg.getResultBus().setSize(inputPort.getValue().getSize(),
				inputPort.getValue().isSigned());

		// visit it
		reg.accept(this);

	}

	/**
	 * Echo control dependencies of the post comp to be the reg
	 * 
	 * @param reg
	 *            a value of type 'Component'
	 * @param postComp
	 *            a value of type 'Component'
	 */
	private void echoControlDependencies(Reg reg, Component postComp) {
		// first the incoming dependencies: clock,reset,go
		//
		// all the dependent buses, as as unique set
		Set<Bus> clockBuses = new HashSet<Bus>(11);
		Set<Bus> resetBuses = new HashSet<Bus>(11);
		Set<Bus> goBuses = new HashSet<Bus>(11);
		for (Entry e : postComp.getEntries()) {
			// for each entry
			// clock first
			for (Dependency dep : e.getDependencies(e.getClockPort())) {
				clockBuses.add(dep.getLogicalBus());
			}
			// reset next
			for (Dependency dep : e.getDependencies(e.getResetPort())) {
				resetBuses.add(dep.getLogicalBus());
			}
			// go last
			for (Dependency dep : e.getDependencies(e.getGoPort())) {
				goBuses.add(dep.getLogicalBus());
			}
		}

		if (_d) {
			_dbg.ln("\tFound " + clockBuses.size() + " dependent clock buses");
		}
		if (_d) {
			_dbg.ln("\tFound " + resetBuses.size() + " dependent reset buses");
		}
		if (_d) {
			_dbg.ln("\tFound " + goBuses.size() + " dependent go buses");
		}
		// add them
		for (Bus bus : clockBuses) {
			reg.getMainEntry().addDependency(reg.getClockPort(),
					new ClockDependency(bus));
		}
		// add them
		for (Bus rstBus : resetBuses) {
			reg.getMainEntry().addDependency(reg.getResetPort(),
					new ResetDependency(rstBus));
			if ((reg.getType() & Reg.RESET) != 0) {
				reg.getMainEntry().addDependency(reg.getInternalResetPort(),
						new ResetDependency(rstBus));
			}
		}
		// add them
		for (Bus bus : goBuses) {
			reg.getMainEntry().addDependency(reg.getGoPort(),
					new ControlDependency(bus));
		}

		// Do we really need to connect up the Reg's DONE control bus
		// for data pipelining register?
		// commented--CWU
		// // now the outgoing : donebus
		// for(Iterator
		// exitIter=postComp.getExits().iterator();exitIter.hasNext();)
		// {
		// Exit ex=(Exit)exitIter.next();
		// if(ex.getTag().getType()==Exit.DONE)
		// {
		// for(Iterator it=ex.getDoneBus().getLogicalDependents().iterator();
		// it.hasNext();)
		// {
		// Dependency dep=(Dependency)it.next();
		// Dependency
		// newDep=dep.createSameType(reg.getExit(Exit.DONE).getDoneBus());
		// dep.getEntry().addDependency(dep.getPort(),newDep);
		// }
		// }
		// }
	}
}
