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

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Stack;

import net.sf.openforge.lim.ArrayRead;
import net.sf.openforge.lim.ArrayWrite;
import net.sf.openforge.lim.Block;
import net.sf.openforge.lim.Branch;
import net.sf.openforge.lim.Bus;
import net.sf.openforge.lim.Call;
import net.sf.openforge.lim.Component;
import net.sf.openforge.lim.DataFlowVisitor;
import net.sf.openforge.lim.Decision;
import net.sf.openforge.lim.Design;
import net.sf.openforge.lim.Exit;
import net.sf.openforge.lim.ForBody;
import net.sf.openforge.lim.HeapRead;
import net.sf.openforge.lim.HeapWrite;
import net.sf.openforge.lim.InBuf;
import net.sf.openforge.lim.Kicker;
import net.sf.openforge.lim.Latch;
import net.sf.openforge.lim.Loop;
import net.sf.openforge.lim.Module;
import net.sf.openforge.lim.OutBuf;
import net.sf.openforge.lim.PinAccess;
import net.sf.openforge.lim.PinRead;
import net.sf.openforge.lim.PinReferee;
import net.sf.openforge.lim.PinStateChange;
import net.sf.openforge.lim.PinWrite;
import net.sf.openforge.lim.Port;
import net.sf.openforge.lim.PriorityMux;
import net.sf.openforge.lim.Procedure;
import net.sf.openforge.lim.RegisterGateway;
import net.sf.openforge.lim.RegisterRead;
import net.sf.openforge.lim.RegisterReferee;
import net.sf.openforge.lim.RegisterWrite;
import net.sf.openforge.lim.Scoreboard;
import net.sf.openforge.lim.Switch;
import net.sf.openforge.lim.Task;
import net.sf.openforge.lim.TaskCall;
import net.sf.openforge.lim.TriBuf;
import net.sf.openforge.lim.UntilBody;
import net.sf.openforge.lim.Visitable;
import net.sf.openforge.lim.WhileBody;
import net.sf.openforge.lim.io.FifoAccess;
import net.sf.openforge.lim.io.FifoRead;
import net.sf.openforge.lim.io.FifoWrite;
import net.sf.openforge.lim.io.SimplePinAccess;
import net.sf.openforge.lim.io.SimplePinRead;
import net.sf.openforge.lim.io.SimplePinWrite;
import net.sf.openforge.lim.memory.AbsoluteMemoryRead;
import net.sf.openforge.lim.memory.AbsoluteMemoryWrite;
import net.sf.openforge.lim.memory.MemoryAccess;
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
 * GateDepthAccumulator calculates the maximum gate depth for each task in a
 * {@link Design} by adding up the gate depth of each LIM {@link Component} in
 * the date path on the task level. The largest number is chosen to be the
 * maximum gate depth for that particular task.
 * 
 * Created: Tue Dec 17 14:22:05 2002
 * 
 * @author cwu
 * @version $Id: GateDepthAccumulator.java 88 2006-01-11 22:39:52Z imiller $
 */
class GateDepthAccumulator extends DataFlowVisitor {

	/** Map of Exit to Integer object of cumulative gate depth */
	private Map<Exit, Integer> exitToGateDepthMap;

	private Stack<Component> unresolvedGateDepthComponents;

	private int taskMaxGateDepth;

	private int designMaxGateDepth;

	private int unbreakableGateDepth;

	public static int getUnbreakableGateDepth(Design design) {
		design.accept(new GateDepthAccumulator());
		return design.getUnbreakableGateDepth();
	}

	/**
	 * Constructor
	 */
	GateDepthAccumulator() {
		super();
		// set the data connection flow to forward mode.
		setRunForward(true);
	}

	@Override
	protected void preFilter(Design node) {
		if (_schedule.db)
			_schedule.ln(_schedule.GDA, "\tDesign PreFilter " + node);
		preFilterAny(node);
	}

	@Override
	protected void preFilter(Task node) {
		if (_schedule.db)
			_schedule.ln(_schedule.GDA, "\tTask PreFilter " + node);
		preFilterAny(node);
	}

	@Override
	protected void preFilter(Procedure node) {
		if (_schedule.db)
			_schedule.ln(_schedule.GDA, "\tProcedure PreFilter " + node);
		preFilterAny(node);
	}

	@Override
	protected void preFilter(Component node) {
		if (_schedule.db)
			_schedule.ln(_schedule.GDA, "\tComponent PreFilter " + node);
		preFilterAny(node);
	}

	@Override
	protected void preFilter(Visitable node) {
		if (_schedule.db)
			_schedule.ln(_schedule.GDA, "\tVisitable PreFilter " + node);
		preFilterAny(node);
	}

	@Override
	protected void preFilterAny(Component node) {
		if (_schedule.db)
			_schedule.ln(_schedule.GDA, "\tComponent PreFilterAny " + node);
	}

	@Override
	protected void preFilterAny(Visitable node) {
		if (_schedule.db)
			_schedule.ln(_schedule.GDA, "\tNON-Component PreFilterAny " + node);
	}

	@Override
	protected void postFilter(Design node) {
		if (_schedule.db)
			_schedule.ln(_schedule.GDA, "\tDesign PostFilter " + node);
		postFilterAny(node);
	}

	@Override
	protected void postFilter(Task node) {
		if (_schedule.db)
			_schedule.ln(_schedule.GDA, "\tTask PostFilter " + node);
		postFilterAny(node);
	}

	@Override
	protected void postFilter(Procedure node) {
		if (_schedule.db)
			_schedule.ln(_schedule.GDA, "\tProcedure PostFilter " + node);
		postFilterAny(node);
	}

	@Override
	protected void postFilter(Component node) {
		if (_schedule.db)
			_schedule.ln(_schedule.GDA, "\tComponent PostFilter " + node);
		postFilterAny(node);
	}

	@Override
	protected void postFilter(Visitable node) {
		if (_schedule.db)
			_schedule.ln(_schedule.GDA, "\tVisitable PostFilter " + node);
		postFilterAny(node);
	}

	@Override
	protected void postFilterAny(Component node) {
		if (_schedule.db)
			_schedule.ln(_schedule.GDA, "\tComponent PostFilterAny " + node);
	}

	@Override
	protected void postFilterAny(Visitable node) {
		if (_schedule.db)
			_schedule
					.ln(_schedule.GDA, "\tNON-Component PostFilterAny " + node);
	}

	/**
	 * Responsible for traversing within a design
	 */
	@Override
	protected void traverse(Design node) {
		if (_schedule.db)
			_schedule.ln(_schedule.GDA, "Traversing Design " + node);
		for (Task task : node.getTasks()) {
			((Visitable) task).accept(this);
		}
	}

	/**
	 * Responsible for traversing within a Task
	 */
	@Override
	protected void traverse(Task node) {
		if (_schedule.db)
			_schedule.ln(_schedule.GDA, "Traversing task " + node);
		if (node.getCall() != null) {
			node.getCall().accept(this);
		}
	}

	/**
	 * Responsible for traversing within a Call
	 */
	@Override
	protected void traverse(Call node) {
		if (_schedule.db)
			_schedule.ln(_schedule.GDA, "Traversing Call " + node);
		if (node.getProcedure() != null) {
			node.getProcedure().accept(this);
		}
	}

	/**
	 * Responsible for traversing within a Procedure
	 */
	@Override
	protected void traverse(Procedure node) {
		if (_schedule.db)
			_schedule.ln(_schedule.GDA, "Traversing Procedure " + node);
		if (node.getBody() != null) {
			node.getBody().accept(this);
		}
	}

	/**
	 * Responsible for traversing within a Module
	 */
	@Override
	protected void traverse(Module module) {
		// Define all feedback points as having 0 depth on their exits
		// in order to break the iterative computation.
		for (Component comp : module.getFeedbackPoints()) {
			for (Exit exit : comp.getExits()) {
				exitToGateDepthMap.put(exit, new Integer(0));
			}
		}

		if (!findUnknownGateDepthOnInputs(module)) {
			if (_schedule.db)
				_schedule.ln(_schedule.GDA, "Module Traversal " + module);
			if (isForward()) {
				traverseModuleForward(module, module.getFeedbackPoints());
			} else {
				traverseModuleReverse(module, module.getFeedbackPoints());
			}

			LinkedList<Component> revisitComponents = new LinkedList<Component>();
			while (true) {
				while (!unresolvedGateDepthComponents.isEmpty()) {
					if (unresolvedGateDepthComponents.peek().getOwner() == module) {
						revisitComponents.add(unresolvedGateDepthComponents
								.pop());
					} else {
						break;
					}
				}
				if (revisitComponents.isEmpty()) {
					break;
				}
				revisitUnknownGateDepthComponents(revisitComponents);
			}
		}
	}

	/**
	 * Responsible for traversing in a Block
	 */
	@Override
	protected void traverse(Block module) {
		traverse((Module) module);
	}

	/**
	 * Traverses the {@link PinAccess#getPhysicalComponent} if it exists.
	 */
	@Override
	protected void traverse(PinAccess node) {
		if (_schedule.db)
			_schedule.ln(_schedule.GDA, "PinAccess Traversal " + node);
		if (node.hasPhysicalComponent()) {
			traverse(node.getPhysicalComponent());
		}
	}

	/**
	 * Traverses the {@link MemoryAccess#getPhysicalComponent} if it exists.
	 */
	@Override
	protected void traverse(MemoryAccess node) {
		if (_schedule.db)
			_schedule.ln(_schedule.GDA, "MemoryAccess Traversal " + node);
		if (node.hasPhysicalComponent()) {
			traverse(node.getPhysicalComponent());
		}
	}

	@Override
	protected void traverse(Component node) {
		if (_schedule.db)
			_schedule.ln(_schedule.GDA, "Component Traversal " + node);
	}

	@Override
	protected void traverse(Visitable node) {
		if (_schedule.db)
			_schedule.ln(_schedule.GDA, "NON-Component Traversal " + node);
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
		if (_schedule.db)
			_schedule.ln(_schedule.GDA, design + ", maximum gate depth: "
					+ design.getMaxGateDepth());
		design.setUnbreakableGateDepth(unbreakableGateDepth);
		if (_schedule.db)
			_schedule.ln(
					_schedule.GDA,
					design + ", maximum unbreakable gate depth: "
							+ design.getUnbreakableGateDepth());
		postFilter(design);

	}

	@Override
	public void visit(Task task) {
		preFilter(task);
		// clear the field variables which store information within a task
		// scope.
		exitToGateDepthMap = new HashMap<Exit, Integer>();
		unresolvedGateDepthComponents = new Stack<Component>();
		taskMaxGateDepth = 0;
		traverse(task);
		// set a task's max gate depth
		task.setMaxGateDepth(taskMaxGateDepth);
		if (_schedule.db)
			_schedule.ln(_schedule.GDA,
					task + ", maximum gate depth: " + task.getMaxGateDepth());
		// set design's gate depth to be the largest gate depth among it's
		// tasks.
		if (taskMaxGateDepth >= designMaxGateDepth) {
			designMaxGateDepth = taskMaxGateDepth;
		}
		postFilter(task);
	}

	@Override
	public void visit(Call call) {
		// Do not checck the call's port connections and gate depth on it's
		// inputs for Entry level call, for other type of calls which
		// should be inside a block have to pass the input gate depth
		// verification before traversing the corresponding procedures.
		if (call.getOwner() == null
				|| call.getOwner() instanceof Design.DesignModule
				|| !findUnknownGateDepthOnInputs(call)) {
			preFilter(call);
			// propagate the gate depth to the procedure ports peer
			// buses which belong to a procedure body's inBuf.
			Integer currentInputGateDepth = (call.getOwner() != null) ? new Integer(
					getMaxInputGateDepth(call)) : new Integer(0);
			for (Port callPort : call.getPorts()) {
				Bus peerBus = call.getProcedurePort(callPort).getPeer();
				exitToGateDepthMap.put(peerBus.getOwner(),
						currentInputGateDepth);
			}
			traverse(call);
			for (Exit callExit : call.getExits()) {
				Integer currentOutputGateDepth = exitToGateDepthMap.get(call
						.getProcedureExit(callExit));
				exitToGateDepthMap.put(callExit, currentOutputGateDepth);
			}
			postFilter(call);
		}
	}

	@Override
	public void visit(Procedure procedure) {
		preFilter(procedure);
		traverse(procedure);
		postFilter(procedure);
	}

	@Override
	public void visit(InBuf inBuf) {
		// The gate depth of an inbuf inside a procedure's body has
		// been propagated on visiting it's call, so we only need to
		// determine the gate depth for other kinds of inBuf.
		if (!(inBuf.getOwner() instanceof Block)
				|| !((Block) inBuf.getOwner()).isProcedureBody()) {
			if (!findUnknownGateDepthOnInputs(inBuf.getOwner())) {
				preFilter(inBuf);
				traverse(inBuf);
				final Integer currentGateDepth = new Integer(
						getMaxInputGateDepth(inBuf) + inBuf.getExitGateDepth());
				for (Bus bus : inBuf.getBuses()) {
					exitToGateDepthMap.put(bus.getOwner(), currentGateDepth);
				}
				if (currentGateDepth.intValue() >= taskMaxGateDepth) {
					taskMaxGateDepth = currentGateDepth.intValue();
				}
				if (_schedule.db)
					_schedule.ln(
							_schedule.GDA,
							inBuf + ", current gate depth: "
									+ currentGateDepth.intValue());
				postFilter(inBuf);
			}
		} else {
			// Gate depth has been propagated, just put some message
			// here for debugging purpose only.
			preFilter(inBuf);
			traverse(inBuf);
			if (_schedule.db)
				_schedule.ln(_schedule.GDA, inBuf
						+ ", current gate depth: "
						+ exitToGateDepthMap.get(inBuf.getExit(Exit.DONE))
								.intValue());
			postFilter(inBuf);
		}
	}

	@Override
	public void visit(OutBuf outBuf) {
		if (!findUnknownGateDepthOnInputs(outBuf)) {
			preFilter(outBuf);
			traverse(outBuf);
			final Integer currentGateDepth = new Integer(
					getMaxInputGateDepth(outBuf) + outBuf.getExitGateDepth());
			for (Port port : outBuf.getPorts()) {
				final Bus moduleBus = port.getPeer();
				if (moduleBus != null) {
					exitToGateDepthMap.put(moduleBus.getOwner(),
							currentGateDepth);
				}
			}
			if (currentGateDepth.intValue() >= taskMaxGateDepth) {
				taskMaxGateDepth = currentGateDepth.intValue();
			}
			if (_schedule.db)
				_schedule.ln(_schedule.GDA, outBuf + ", current gate depth: "
						+ currentGateDepth.intValue());
			postFilter(outBuf);
		}
	}

	@Override
	public void visit(Decision decision) {
		if (!findUnknownGateDepthOnInputs(decision)) {
			super.visit(decision);
		}
	}

	@Override
	public void visit(Loop loop) {
		/*
		 * artificially assign feedback exit to gate depth to 0 if the loop flop
		 * has been removed by loop flop optimization
		 */
		if (loop.getControlRegister() == null) {
			exitToGateDepthMap.put(loop.getBody().getFeedbackExit(),
					new Integer(0));
		}

		if (!findUnknownGateDepthOnInputs(loop)) {
			super.visit(loop);
		}
	}

	@Override
	public void visit(WhileBody whileBody) {
		if (!findUnknownGateDepthOnInputs(whileBody)) {
			super.visit(whileBody);
		}
	}

	@Override
	public void visit(UntilBody untilBody) {
		if (!findUnknownGateDepthOnInputs(untilBody)) {
			super.visit(untilBody);
		}
	}

	@Override
	public void visit(ForBody forBody) {
		if (!findUnknownGateDepthOnInputs(forBody)) {
			super.visit(forBody);
		}
	}

	@Override
	public void visit(Block block) {
		if (!findUnknownGateDepthOnInputs(block)) {
			super.visit(block);
		}
	}

	@Override
	public void visit(Branch branch) {
		if (!findUnknownGateDepthOnInputs(branch)) {
			super.visit(branch);
		}
	}

	@Override
	public void visit(Latch latch) {
		if (!findUnknownGateDepthOnInputs(latch)) {
			super.visit(latch);
		}
	}

	@Override
	public void visit(Switch swith) {
		if (!findUnknownGateDepthOnInputs(swith)) {
			super.visit(swith);
		}
	}

	@Override
	public void visit(PriorityMux priorityMux) {
		if (!findUnknownGateDepthOnInputs(priorityMux)) {
			super.visit(priorityMux);
		}
	}

	@Override
	public void visit(Scoreboard scoreBoard) {
		if (!findUnknownGateDepthOnInputs(scoreBoard)) {
			super.visit(scoreBoard);
		}
	}

	@Override
	public void visit(RegisterGateway regGateway) {
		if (!findUnknownGateDepthOnInputs(regGateway)) {
			super.visit(regGateway);
		}
	}

	@Override
	public void visit(RegisterReferee regReferee) {
		if (!findUnknownGateDepthOnInputs(regReferee)) {
			super.visit(regReferee);
		}
	}

	@Override
	public void visit(MemoryReferee memReferee) {
		if (!findUnknownGateDepthOnInputs(memReferee)) {
			super.visit(memReferee);
		}
	}

	@Override
	public void visit(MemoryGateway memGateway) {
		if (!findUnknownGateDepthOnInputs(memGateway)) {
			super.visit(memGateway);
		}
	}

	@Override
	public void visit(MemoryBank comp) {
		if (!findUnknownGateDepthOnInputs(comp)) {
			super.visit(comp);
		}
	}

	@Override
	public void visit(HeapRead heapRead) {
		if (!findUnknownGateDepthOnInputs(heapRead)) {
			super.visit(heapRead);
		}
	}

	@Override
	public void visit(ArrayRead arrayRead) {
		if (!findUnknownGateDepthOnInputs(arrayRead)) {
			super.visit(arrayRead);
		}
	}

	@Override
	public void visit(HeapWrite heapWrite) {
		if (!findUnknownGateDepthOnInputs(heapWrite)) {
			super.visit(heapWrite);
		}
	}

	@Override
	public void visit(ArrayWrite arrayWrite) {
		if (!findUnknownGateDepthOnInputs(arrayWrite)) {
			super.visit(arrayWrite);
		}
	}

	@Override
	public void visit(AbsoluteMemoryRead absRead) {
		if (!findUnknownGateDepthOnInputs(absRead)) {
			super.visit(absRead);
		}
	}

	@Override
	public void visit(AbsoluteMemoryWrite absWrite) {
		if (!findUnknownGateDepthOnInputs(absWrite)) {
			super.visit(absWrite);
		}
	}

	@Override
	public void visit(Kicker kicker) {
		if (!findUnknownGateDepthOnInputs(kicker)) {
			super.visit(kicker);
		}
	}

	@Override
	public void visit(PinReferee pinReferee) {
		if (!findUnknownGateDepthOnInputs(pinReferee)) {
			super.visit(pinReferee);
		}
	}

	@Override
	public void visit(FifoAccess param1) {
		if (!findUnknownGateDepthOnInputs(param1)) {
			super.visit(param1);
		}
	}

	@Override
	public void visit(FifoRead param1) {
		if (!findUnknownGateDepthOnInputs(param1)) {
			super.visit(param1);
		}
	}

	@Override
	public void visit(FifoWrite param1) {
		if (!findUnknownGateDepthOnInputs(param1)) {
			super.visit(param1);
		}
	}

	@Override
	public void visit(Reg reg) {
		preFilter(reg);
		traverse(reg);
		final Integer currentGateDepth = new Integer(0);
		exitToGateDepthMap.put(reg.getBuses().iterator().next().getOwner(),
				currentGateDepth);
		postFilter(reg);
	}

	@Override
	public void visit(Constant constant) {
		preFilter(constant);
		traverse(constant);
		final Integer currentGateDepth = new Integer(0);
		exitToGateDepthMap.put(
				constant.getBuses().iterator().next().getOwner(),
				currentGateDepth);
		postFilter(constant);
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

	@Override
	public void visit(TaskCall param1) {
		if (!findUnknownGateDepthOnInputs(param1)) {
			super.visit(param1);
		}
	}

	@Override
	public void visit(SimplePinAccess param1) {
		if (!findUnknownGateDepthOnInputs(param1)) {
			super.visit(param1);
		}
	}

	@Override
	public void visit(SimplePinRead param1) {
		visit((Component) param1);
	}

	@Override
	public void visit(SimplePinWrite param1) {
		visit((Component) param1);
	}

	public void visit(Component component) {
		unbreakableGateDepth = Math.max(unbreakableGateDepth,
				component.getExitGateDepth());

		if (!findUnknownGateDepthOnInputs(component)) {
			preFilter(component);
			final Integer currentGateDepth = new Integer(
					getMaxInputGateDepth(component)
							+ component.getExitGateDepth());
			for (Bus bus : component.getBuses()) {
				exitToGateDepthMap.put(bus.getOwner(), currentGateDepth);
			}
			if (currentGateDepth.intValue() >= taskMaxGateDepth) {
				taskMaxGateDepth = currentGateDepth.intValue();
			}
			if (_schedule.db)
				_schedule.ln(
						_schedule.GDA,
						component + ", current gate depth: "
								+ currentGateDepth.intValue());
			postFilter(component);
		}
	}

	private int getMaxInputGateDepth(Component component) {
		int maxGateDepth = 0;
		component = (component instanceof InBuf) ? component.getOwner()
				: component;
		for (Port port : component.getPorts()) {
			if (port == component.getClockPort()
					|| port == component.getResetPort()) {
				continue;
			}
			int gateDepth = port.isConnected() ? exitToGateDepthMap.get(
					port.getBus().getOwner()).intValue() : 0;
			maxGateDepth = Math.max(maxGateDepth, gateDepth);
		}
		return maxGateDepth;
	}

	private boolean findUnknownGateDepthOnInputs(Component component) {
		for (Port port : component.getPorts()) {
			// assert (port.isConnected() &&
			// this.exitToGateDepthMap.containsKey(port.getBus().getOwner()));
			if (port.isConnected()
					&& !exitToGateDepthMap
							.containsKey(port.getBus().getOwner())) {
				unresolvedGateDepthComponents.push(component);
				return true;
			}
		}
		return false;
	}

	private void revisitUnknownGateDepthComponents(
			LinkedList<Component> revisitComponents) {
		while (!revisitComponents.isEmpty()) {
			revisitComponents.removeFirst().accept(this);
		}
	}
}
