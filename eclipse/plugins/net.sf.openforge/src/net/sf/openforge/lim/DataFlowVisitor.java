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
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Set;

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

/**
 * <p>
 * DataFlowVisitor traverses a design in either forward or reverse data flow
 * order depending on what isForward() returns. The traversal of any
 * {@link Module} visits each {@link Component} returned by
 * {@link Module#getComponents()} only when all components that feed data to the
 * given Component have been traversed. Since {@link InBuf}s and
 * {@link Constants} have no data inputs, they are considered ready to be
 * visited which primes the visitation. For each data {@link Port} on a
 * Component to be resolved it must meet one of these criteria:
 * <ul>
 * <li>The port has an attached bus and the source of that bus has been visited.
 * <b>AND</b> any ResourceDependencys have been resolved.
 * <li>The source of each bus captured in a dependency for that port has been
 * visited. Note that all entries are looked at.
 * </ul>
 * <p>
 * Reverse data flow order ensures that all components which consume a data bus
 * from the component have been traversed. Similar to the way in which the
 * forward propagate works, the component is traversed only when all data buses
 * meet one of the following criteria:
 * <ul>
 * <li>The bus has at least 1 port attached to it ({@link Bus#getPorts()}.size >
 * 0) and the owner of each of those ports has been visited. <b>AND</b> any
 * ResourceDependencys have been resolved.
 * <li>All ports which depend on the bus are owned by a component that has been
 * traversed.
 * </ul>
 * <p>
 * <b>NOTE:</b>If you need to over-ride any traverse, preFilter, or postFilter
 * method for a specific class, be sure that the method exists in this class
 * first. Otherwise because of overloading rules your implementation in the
 * sub-class won't be seen by this class.
 * 
 * <p>
 * Created: Thu Nov 14 12:20:41 2002
 * 
 * @author imiller, last modified by $Author: imiller $
 * @version $Id: DataFlowVisitor.java 88 2006-01-11 22:39:52Z imiller $
 */
public abstract class DataFlowVisitor implements Visitor {

	private boolean forward = false;

	public DataFlowVisitor() {
	}

	/**
	 * Set true to run through in input -> output data flow order, or set to
	 * false to run through modules in output -> input (reverse) data flow
	 * order.
	 */
	public void setRunForward(boolean fwd) {
		forward = fwd;
	}

	/**
	 * Returns true if this visitor will traverse from input to output in data
	 * flow order.
	 */
	public boolean isForward() {
		return forward;
	}

	protected void preFilter(Design node) {
		if (_lim.db)
			_lim.ln(_lim.DFV, "\tDesign PreFilter " + node);
		preFilterAny(node);
	}

	protected void preFilter(Task node) {
		if (_lim.db)
			_lim.ln(_lim.DFV, "\tTask PreFilter " + node);
		preFilterAny(node);
	}

	protected void preFilter(Procedure node) {
		if (_lim.db)
			_lim.ln(_lim.DFV, "\tProcedure PreFilter " + node);
		preFilterAny(node);
	}

	protected void preFilter(Component node) {
		if (_lim.db)
			_lim.ln(_lim.DFV, "\tComponent PreFilter " + node);
		preFilterAny(node);
	}

	protected void preFilter(Visitable node) {
		if (_lim.db)
			_lim.ln(_lim.DFV, "\tVisitable PreFilter " + node);
		preFilterAny(node);
	}

	/**
	 * Called by every visit method before traversing the contents of the given
	 * component.
	 */
	protected void preFilterAny(Component node) {
		if (_lim.db)
			_lim.ln(_lim.DFV, "\tComponent PreFilterAny " + node);
	}

	protected void preFilterAny(Visitable node) {
		if (_lim.db)
			_lim.ln(_lim.DFV, "\tNON-Component PreFilterAny " + node);
	}

	protected void postFilter(Design node) {
		if (_lim.db)
			_lim.ln(_lim.DFV, "\tDesign PostFilter " + node);
		postFilterAny(node);
	}

	protected void postFilter(Task node) {
		if (_lim.db)
			_lim.ln(_lim.DFV, "\tTask PostFilter " + node);
		postFilterAny(node);
	}

	protected void postFilter(Procedure node) {
		if (_lim.db)
			_lim.ln(_lim.DFV, "\tProcedure PostFilter " + node);
		postFilterAny(node);
	}

	protected void postFilter(Component node) {
		if (_lim.db)
			_lim.ln(_lim.DFV, "\tComponent PostFilter " + node);
		postFilterAny(node);
	}

	protected void postFilter(Visitable node) {
		if (_lim.db)
			_lim.ln(_lim.DFV, "\tVisitable PostFilter " + node);
		postFilterAny(node);
	}

	/**
	 * Called by every visit method after traversing the contents of the given
	 * component.
	 */
	protected void postFilterAny(Component node) {
		if (_lim.db)
			_lim.ln(_lim.DFV, "\tComponent PostFilterAny " + node);
	}

	protected void postFilterAny(Visitable node) {
		if (_lim.db)
			_lim.ln(_lim.DFV, "\tNON-Component PostFilterAny " + node);
	}

	/**
	 * Responsible for traversing within a design
	 */
	protected void traverse(Design node) {
		if (_lim.db)
			_lim.ln(_lim.DFV, "Traversing Design " + node);
		for (Task task : node.getTasks()) {
			task.accept(this);
		}
	}

	/**
	 * Responsible for traversing within a Task
	 */
	protected void traverse(Task node) {
		if (_lim.db)
			_lim.ln(_lim.DFV, "Traversing task " + node);
		if (node.getCall() != null) {
			node.getCall().accept(this);
		}
	}

	/**
	 * Responsible for traversing within a Call
	 */
	protected void traverse(Call node) {
		if (_lim.db)
			_lim.ln(_lim.DFV, "Traversing Call " + node);
		if (node.getProcedure() != null) {
			node.getProcedure().accept(this);
		}
	}

	protected void traverse(IPCoreCall node) {
		if (_lim.db)
			_lim.ln(_lim.DFV, "Traversing IPCoreCall " + node);
	}

	/**
	 * Responsible for traversing within a Procedure
	 */
	protected void traverse(Procedure node) {
		if (_lim.db)
			_lim.ln(_lim.DFV, "Traversing Procedure " + node);
		if (node.getBody() != null) {
			node.getBody().accept(this);
		}
	}

	/**
	 * Responsible for traversing within a Module
	 */
	protected void traverse(Module module) {
		if (_lim.db)
			_lim.ln(_lim.DFV, "Module Traversal " + module);

		if (isForward())
			traverseModuleForward(module, module.getFeedbackPoints());
		else
			traverseModuleReverse(module, module.getFeedbackPoints());
	}

	/**
	 * Responsible for traversing in a Block
	 */
	protected void traverse(Block module) {
		traverse((Module) module);
	}

	/**
	 * Traverses the {@link PinAccess#getPhysicalComponent} if it exists.
	 */
	protected void traverse(PinAccess node) {
		if (_lim.db)
			_lim.ln(_lim.DFV, "PinAccess Traversal " + node);
		if (node.hasPhysicalComponent()) {
			traverse(node.getPhysicalComponent());
		}
	}

	/**
	 * Traverses the {@link MemoryAccess#getPhysicalComponent} if it exists.
	 */
	protected void traverse(MemoryAccess node) {
		if (_lim.db)
			_lim.ln(_lim.DFV, "MemoryAccess Traversal " + node);
		if (node.hasPhysicalComponent()) {
			traverse(node.getPhysicalComponent());
		}
	}

	protected void traverse(Component node) {
		if (_lim.db)
			_lim.ln(_lim.DFV, "Component Traversal " + node);
	}

	protected void traverse(Visitable node) {
		if (_lim.db)
			_lim.ln(_lim.DFV, "NON-Component Traversal " + node);
	}

	protected void traverseModuleForward(Module module,
			Set<Component> feedbackComps) {
		Set<Component> processed = new HashSet<Component>();
		Set<Component> feedback = new LinkedHashSet<Component>(feedbackComps);
		processed.addAll(feedback);
		LinkedList<Component> queue = new LinkedList<Component>(
				module.getComponents());

		while (!queue.isEmpty()) {
			Component comp = queue.removeFirst();
			if (processed.contains(comp)) {
				continue;
			}

			// Gateways represent implicit feedback points if the
			// accessed module has both read and write accesses to the
			// given resource.
			if (comp.isGateway()) {
				processed.add(comp);
				feedback.add(comp);
				continue;
			}

			// Ensure that all inputs have been processed
			Set<Component> depenComponentsSet = new HashSet<Component>();
			for (Port port : comp.getPorts()) {
				if (port.getBus() != null) {
					Component depenComp = port.getBus().getOwner().getOwner();
					if (!processed.contains(depenComp)) {
						depenComponentsSet.add(depenComp);
						queue.remove(depenComp);
						queue.addFirst(depenComp);
					}
					for (Entry entry : comp.getEntries()) {
						for (Dependency dep : entry.getDependencies(port)) {
							depenComp = dep.getLogicalBus().getOwner()
									.getOwner();
							if (dep instanceof ResourceDependency
									&& !processed.contains(depenComp)) {
								depenComponentsSet.add(depenComp);
								queue.remove(depenComp);
								queue.addFirst(depenComp);
							}
						}
					}
				} else {
					// Iterate through the dependencies...
					for (Entry entry : comp.getEntries()) {
						for (Dependency dep : entry.getDependencies(port)) {
							Component depenComp = dep.getLogicalBus()
									.getOwner().getOwner();
							if (!processed.contains(depenComp)) {
								depenComponentsSet.add(depenComp);
								queue.remove(depenComp);
								queue.addFirst(depenComp);
							}
						}
					}
				}
			}

			if (depenComponentsSet.isEmpty()) {
				processed.add(comp);
				comp.accept(this);
			} else {
				// if the component is not ready, put it back in the queue at
				// the appropriate position.
				queue.add(depenComponentsSet.size(), comp);
			}
		}

		for (Component component : feedback) {
			final Visitable visitable = component;
			visitable.accept(this);
		}
	}

	protected void traverseModuleReverse(Module module,
			Set<Component> feedbackComps) {
		HashSet<Component> processed = new HashSet<Component>();
		Set<Component> feedback = new LinkedHashSet<Component>(feedbackComps);
		processed.addAll(feedback);
		LinkedList<Component> queue = new LinkedList<Component>(
				module.getComponents());
		while (!queue.isEmpty()) {
			Component comp = queue.removeFirst();
			if (processed.contains(comp)) {
				continue;
			}
			// Gateways represent implicit feedback points if the
			// accessed module has both read and write accesses to the
			// given resource.
			if (comp.isGateway()) {
				processed.add(comp);
				feedback.add(comp);
				comp.accept(this);
				continue;
			}

			// Ensure that all consumers have been processed.
			boolean isReady = true;
			for (Bus bus : comp.getBuses()) {
				Collection<Port> ports;
				if (bus.getPorts().size() > 0) {
					ports = new HashSet<Port>(bus.getPorts());
					// Pick up any resource dependencies as well to
					// guarantee correct data flow ordering through
					// global resources.
					for (Dependency dep : bus.getLogicalDependents()) {
						if (dep instanceof ResourceDependency) {
							ports.add(dep.getPort());
						}
					}
				} else {
					ports = new LinkedList<Port>();
					for (Dependency dependency : bus.getLogicalDependents()) {
						ports.add(dependency.getPort());
					}
				}
				for (Port port : ports) {
					if (!processed.contains(port.getOwner())) {
						isReady = false;
						queue.remove(port.getOwner());
						queue.addFirst(port.getOwner());
					}
				}
				if (!isReady) {
					queue.add(comp); // Put it back on the list.
					break;
				}
			}

			if (isReady) {
				processed.add(comp);
				comp.accept(this);
			}
		}
		for (Component component : feedback) {
			component.accept(this);
		}
	}

	// implementation of net.sf.openforge.lim.Visitor interface

	@Override
	public void visit(Design param1) {
		preFilter(param1);
		traverse(param1);
		postFilter(param1);
	}

	@Override
	public void visit(Task param1) {
		preFilter(param1);
		traverse(param1);
		postFilter(param1);
	}

	@Override
	public void visit(Call param1) {
		preFilter(param1);
		traverse(param1);
		postFilter(param1);
	}

	@Override
	public void visit(IPCoreCall param1) {
	}

	@Override
	public void visit(Procedure param1) {
		preFilter(param1);
		traverse(param1);
		postFilter(param1);
	}

	@Override
	public void visit(Block param1) {
		preFilter(param1);
		traverse(param1);
		postFilter(param1);
	}

	@Override
	public void visit(Loop param1) {
		preFilter(param1);
		traverse(param1);
		postFilter(param1);
	}

	@Override
	public void visit(WhileBody param1) {
		preFilter(param1);
		traverse(param1);
		postFilter(param1);
	}

	@Override
	public void visit(UntilBody param1) {
		preFilter(param1);
		traverse(param1);
		postFilter(param1);
	}

	@Override
	public void visit(ForBody param1) {
		preFilter(param1);
		traverse(param1);
		postFilter(param1);
	}

	@Override
	public void visit(AddOp param1) {
		preFilter(param1);
		traverse(param1);
		postFilter(param1);
	}

	@Override
	public void visit(AndOp param1) {
		preFilter(param1);
		traverse(param1);
		postFilter(param1);
	}

	@Override
	public void visit(CastOp param1) {
		preFilter(param1);
		traverse(param1);
		postFilter(param1);
	}

	@Override
	public void visit(ComplementOp param1) {
		preFilter(param1);
		traverse(param1);
		postFilter(param1);
	}

	@Override
	public void visit(ConditionalAndOp param1) {
		preFilter(param1);
		traverse(param1);
		postFilter(param1);
	}

	@Override
	public void visit(ConditionalOrOp param1) {
		preFilter(param1);
		traverse(param1);
		postFilter(param1);
	}

	@Override
	public void visit(Constant param1) {
		preFilter(param1);
		traverse(param1);
		postFilter(param1);
	}

	@Override
	public void visit(LocationConstant loc) {
		visit((Constant) loc);
	}

	@Override
	public void visit(DivideOp param1) {
		preFilter(param1);
		traverse(param1);
		postFilter(param1);
	}

	@Override
	public void visit(EqualsOp param1) {
		preFilter(param1);
		traverse(param1);
		postFilter(param1);
	}

	@Override
	public void visit(GreaterThanEqualToOp param1) {
		preFilter(param1);
		traverse(param1);
		postFilter(param1);
	}

	@Override
	public void visit(GreaterThanOp param1) {
		preFilter(param1);
		traverse(param1);
		postFilter(param1);
	}

	@Override
	public void visit(LeftShiftOp param1) {
		preFilter(param1);
		traverse(param1);
		postFilter(param1);
	}

	@Override
	public void visit(LessThanEqualToOp param1) {
		preFilter(param1);
		traverse(param1);
		postFilter(param1);
	}

	@Override
	public void visit(LessThanOp param1) {
		preFilter(param1);
		traverse(param1);
		postFilter(param1);
	}

	@Override
	public void visit(MinusOp param1) {
		preFilter(param1);
		traverse(param1);
		postFilter(param1);
	}

	@Override
	public void visit(ModuloOp param1) {
		preFilter(param1);
		traverse(param1);
		postFilter(param1);
	}

	@Override
	public void visit(MultiplyOp param1) {
		preFilter(param1);
		traverse(param1);
		postFilter(param1);
	}

	@Override
	public void visit(NotEqualsOp param1) {
		preFilter(param1);
		traverse(param1);
		postFilter(param1);
	}

	@Override
	public void visit(NotOp param1) {
		preFilter(param1);
		traverse(param1);
		postFilter(param1);
	}

	@Override
	public void visit(OrOp param1) {
		preFilter(param1);
		traverse(param1);
		postFilter(param1);
	}

	@Override
	public void visit(PlusOp param1) {
		preFilter(param1);
		traverse(param1);
		postFilter(param1);
	}

	@Override
	public void visit(ReductionOrOp param1) {
		preFilter(param1);
		traverse(param1);
		postFilter(param1);
	}

	@Override
	public void visit(RightShiftOp param1) {
		preFilter(param1);
		traverse(param1);
		postFilter(param1);
	}

	@Override
	public void visit(RightShiftUnsignedOp param1) {
		preFilter(param1);
		traverse(param1);
		postFilter(param1);
	}

	@Override
	public void visit(ShortcutIfElseOp param1) {
		preFilter(param1);
		traverse(param1);
		postFilter(param1);
	}

	@Override
	public void visit(SubtractOp param1) {
		preFilter(param1);
		traverse(param1);
		postFilter(param1);
	}

	@Override
	public void visit(NumericPromotionOp param1) {
		preFilter(param1);
		traverse(param1);
		postFilter(param1);
	}

	@Override
	public void visit(XorOp param1) {
		preFilter(param1);
		traverse(param1);
		postFilter(param1);
	}

	@Override
	public void visit(Branch param1) {
		preFilter(param1);
		traverse(param1);
		postFilter(param1);
	}

	@Override
	public void visit(Decision param1) {
		preFilter(param1);
		traverse(param1);
		postFilter(param1);
	}

	@Override
	public void visit(Switch param1) {
		preFilter(param1);
		traverse(param1);
		postFilter(param1);
	}

	@Override
	public void visit(InBuf param1) {
		preFilter(param1);
		traverse(param1);
		postFilter(param1);
	}

	@Override
	public void visit(OutBuf param1) {
		preFilter(param1);
		traverse(param1);
		postFilter(param1);
	}

	@Override
	public void visit(Reg param1) {
		preFilter(param1);
		traverse(param1);
		postFilter(param1);
	}

	@Override
	public void visit(Mux param1) {
		preFilter(param1);
		traverse(param1);
		postFilter(param1);
	}

	@Override
	public void visit(EncodedMux param1) {
		preFilter(param1);
		traverse(param1);
		postFilter(param1);
	}

	@Override
	public void visit(PriorityMux param1) {
		preFilter(param1);
		traverse(param1);
		postFilter(param1);
	}

	@Override
	public void visit(And param1) {
		preFilter(param1);
		traverse(param1);
		postFilter(param1);
	}

	@Override
	public void visit(Not param1) {
		preFilter(param1);
		traverse(param1);
		postFilter(param1);
	}

	@Override
	public void visit(Or or) {
		preFilter(or);
		traverse(or);
		postFilter(or);
	}

	@Override
	public void visit(Scoreboard param1) {
		preFilter(param1);
		traverse(param1);
		postFilter(param1);
	}

	@Override
	public void visit(Latch param1) {
		preFilter(param1);
		traverse(param1);
		postFilter(param1);
	}

	@Override
	public void visit(NoOp param1) {
		preFilter(param1);
		traverse(param1);
		postFilter(param1);
	}

	@Override
	public void visit(TimingOp param1) {
		preFilter(param1);
		traverse(param1);
		postFilter(param1);
	}

	@Override
	public void visit(RegisterRead param1) {
		preFilter(param1);
		traverse(param1);
		postFilter(param1);
	}

	@Override
	public void visit(RegisterWrite param1) {
		preFilter(param1);
		traverse(param1);
		postFilter(param1);
	}

	@Override
	public void visit(RegisterGateway param1) {
		preFilter(param1);
		traverse(param1);
		postFilter(param1);
	}

	@Override
	public void visit(RegisterReferee param1) {
		preFilter(param1);
		traverse(param1);
		postFilter(param1);
	}

	@Override
	public void visit(MemoryBank param1) {
		preFilter(param1);
		traverse(param1);
		postFilter(param1);
	}

	@Override
	public void visit(MemoryRead memoryRead) {
		final Module impl = memoryRead.getPhysicalComponent();
		if (impl != null) {
			preFilter(impl);
			traverse(impl);
			postFilter(impl);
		} else {
			preFilter(memoryRead);
			traverse(memoryRead);
			postFilter(memoryRead);
		}
	}

	@Override
	public void visit(MemoryWrite memoryWrite) {
		final Module impl = memoryWrite.getPhysicalComponent();
		if (impl != null) {
			preFilter(impl);
			traverse(impl);
			postFilter(impl);
		} else {
			preFilter(memoryWrite);
			traverse(memoryWrite);
			postFilter(memoryWrite);
		}
	}

	@Override
	public void visit(MemoryReferee param1) {
		preFilter(param1);
		traverse(param1);
		postFilter(param1);
	}

	@Override
	public void visit(MemoryGateway param1) {
		preFilter(param1);
		traverse(param1);
		postFilter(param1);
	}

	@Override
	public void visit(HeapRead param1) {
		preFilter(param1);
		traverse(param1);
		postFilter(param1);
	}

	@Override
	public void visit(ArrayRead param1) {
		preFilter(param1);
		traverse(param1);
		postFilter(param1);
	}

	@Override
	public void visit(HeapWrite param1) {
		preFilter(param1);
		traverse(param1);
		postFilter(param1);
	}

	@Override
	public void visit(ArrayWrite param1) {
		preFilter(param1);
		traverse(param1);
		postFilter(param1);
	}

	@Override
	public void visit(AbsoluteMemoryRead param1) {
		preFilter(param1);
		traverse(param1);
		postFilter(param1);
	}

	@Override
	public void visit(AbsoluteMemoryWrite param1) {
		preFilter(param1);
		traverse(param1);
		postFilter(param1);
	}

	@Override
	public void visit(Kicker param1) {
		preFilter(param1);
		traverse(param1);
		postFilter(param1);
	}

	@Override
	public void visit(PinRead param1) {
		preFilter(param1);
		traverse(param1);
		postFilter(param1);
	}

	@Override
	public void visit(PinWrite param1) {
		preFilter(param1);
		traverse(param1);
		postFilter(param1);
	}

	@Override
	public void visit(PinStateChange param1) {
		preFilter(param1);
		traverse(param1);
		postFilter(param1);
	}

	@Override
	public void visit(SRL16 param1) {
		preFilter(param1);
		traverse(param1);
		postFilter(param1);
	}

	@Override
	public void visit(PinReferee param1) {
		preFilter(param1);
		traverse(param1);
		postFilter(param1);
	}

	@Override
	public void visit(TriBuf param1) {
		preFilter(param1);
		traverse(param1);
		postFilter(param1);
	}

	@Override
	public void visit(TaskCall param1) {
		preFilter(param1);
		traverse(param1);
		postFilter(param1);
	}

	@Override
	public void visit(SimplePin param1) {
		preFilter(param1);
		traverse(param1);
		postFilter(param1);
	}

	@Override
	public void visit(SimplePinAccess param1) {
		preFilter(param1);
		traverse(param1);
		postFilter(param1);
	}

	@Override
	public void visit(SimplePinRead param1) {
		preFilter(param1);
		traverse(param1);
		postFilter(param1);
	}

	@Override
	public void visit(SimplePinWrite param1) {
		preFilter(param1);
		traverse(param1);
		postFilter(param1);
	}

	@Override
	public void visit(FifoAccess param1) {
		preFilter(param1);
		traverse(param1);
		postFilter(param1);
	}

	@Override
	public void visit(FifoRead param1) {
		preFilter(param1);
		traverse(param1);
		postFilter(param1);
	}

	@Override
	public void visit(FifoWrite param1) {
		preFilter(param1);
		traverse(param1);
		postFilter(param1);
	}

	@Override
	public void visit(EndianSwapper param1) {
		preFilter(param1);
		traverse(param1);
		postFilter(param1);
	}
}