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
package net.sf.openforge.report;

import java.io.FileOutputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Stack;

import net.sf.openforge.app.EngineThread;
import net.sf.openforge.lim.BidirectionalPin;
import net.sf.openforge.lim.Call;
import net.sf.openforge.lim.CodeLabel;
import net.sf.openforge.lim.Component;
import net.sf.openforge.lim.Design;
import net.sf.openforge.lim.FilteredVisitor;
import net.sf.openforge.lim.InputPin;
import net.sf.openforge.lim.Kicker;
import net.sf.openforge.lim.Latch;
import net.sf.openforge.lim.Module;
import net.sf.openforge.lim.Operation;
import net.sf.openforge.lim.OutputPin;
import net.sf.openforge.lim.Pin;
import net.sf.openforge.lim.PinReferee;
import net.sf.openforge.lim.Procedure;
import net.sf.openforge.lim.Register;
import net.sf.openforge.lim.Scoreboard;
import net.sf.openforge.lim.Task;
import net.sf.openforge.lim.Visitable;
import net.sf.openforge.lim._lim;
import net.sf.openforge.lim.memory.LogicalMemory;
import net.sf.openforge.lim.memory.LogicalMemoryPort;
import net.sf.openforge.lim.memory.MemoryBank;
import net.sf.openforge.lim.memory.MemoryRead;
import net.sf.openforge.lim.memory.MemoryReferee;
import net.sf.openforge.lim.memory.MemoryWrite;
import net.sf.openforge.lim.memory.StructuralMemory;
import net.sf.openforge.lim.primitive.Primitive;
import net.sf.openforge.verilog.mapping.MemoryMapper;
import net.sf.openforge.verilog.mapping.memory.BlockRam;
import net.sf.openforge.verilog.mapping.memory.DualPortBlockRam;
import net.sf.openforge.verilog.mapping.memory.DualPortLutRam;
import net.sf.openforge.verilog.mapping.memory.LutRam;
import net.sf.openforge.verilog.mapping.memory.Ram;
import net.sf.openforge.verilog.mapping.memory.VerilogMemory;

/**
 * A visitor that travserses through a Design and collect the resource
 * consumsion information for each individual procedure, task, and design.
 * 
 * @author cwu
 * @version $Id: HardwareResourceUtilizer.java 2 2005-06-09 20:00:48Z imiller $
 */
public class HardwareResourceUtilizer extends FilteredVisitor {

	private FPGAResource designResourceUsage;
	private FPGAResource currentResourceUsage;
	private Map<Procedure, FPGAResource> procedureToResourceUsageMap;
	private Stack<FPGAResource> procedureResourceUsageStack;

	@SuppressWarnings("unused")
	private ResourceUtilizationReporter rerourceUtilizationWriter;

	public HardwareResourceUtilizer(Design design, FileOutputStream fos) {
		super();
		procedureToResourceUsageMap = new HashMap<Procedure, FPGAResource>();
		visit(design);
		rerourceUtilizationWriter = new ResourceUtilizationReporter(design,
				designResourceUsage, procedureToResourceUsageMap, fos);
	}

	/**
	 * Visit the Task(s) according to TaskResources in a DesignResource
	 * 
	 * @param design
	 *            a LIM design
	 */
	@SuppressWarnings("deprecation")
	@Override
	public void visit(Design design) {
		designResourceUsage = new FPGAResource();
		currentResourceUsage = new FPGAResource();

		// Identify whether a design needs clock or not
		if (design.consumesClock()) {
			// FIXME ABK: unclear usage, addClock(number of clocks to add?)
			// this.designResourceUsage.addClock(1);
			designResourceUsage.addClock(design.getClockPins().size());
		}

		// Add IBUF resource usage
		for (Pin pin : design.getInputPins()) {
			InputPin inPin = (InputPin) pin;
			designResourceUsage.addIB(inPin.getWidth());
		}
		// Add OBUF resource usage
		for (Pin pin : design.getOutputPins()) {
			OutputPin outPin = (OutputPin) pin;
			designResourceUsage.addOB(outPin.getWidth());
		}
		// Add IOBUF resource usage
		for (Pin pin : design.getBidirectionalPins()) {
			BidirectionalPin inoutPin = (BidirectionalPin) pin;
			designResourceUsage.addIOB(inoutPin.getWidth());
		}

		// Add design's memories LUT resource usage
		for (LogicalMemory memory : design.getLogicalMemories()) {
			for (LogicalMemoryPort memport : memory.getLogicalMemoryPorts()) {
				MemoryReferee referee = memport.getReferee();
				Collection<Component> components = new HashSet<Component>(
						referee.getComponents());
				components.remove(referee.getInBuf());
				components.removeAll(referee.getOutBufs());
				for (Component component : components) {
					Visitable vis = component;
					vis.accept(this);
				}
			}
			StructuralMemory structMem = memory.getStructuralMemory();
			if (structMem != null) {
				Collection<Component> comps = new HashSet<Component>(
						structMem.getComponents());
				comps.remove(structMem.getInBuf());
				comps.removeAll(structMem.getOutBufs());
				for (Component component : comps) {
					((Visitable) component).accept(this);
				}
			}
		}

		// Add design's global registers FlipFlop resource usage
		for (Register reg : design.getRegisters()) {
			// XXX FIXME. Register.Physical has no visitor support.
			Module connector = reg.getPhysicalComponent();
			if (connector != null) {
				if (!(connector instanceof Register.Physical)) {
					connector.accept(this);
				} else {
					int careBitCount = reg.getInitWidth();
					currentResourceUsage.addFlipFlop(careBitCount);
				}
			}
		}

		designResourceUsage.addResourceUsage(currentResourceUsage);

		super.visit(design);

		// Sum up the resource usage of each task as a design's
		// resource usage
		for (Task task : design.getTasks()) {
			Call taskCall = task.getCall();
			Procedure topProcedure = taskCall.getProcedure();
			designResourceUsage.addResourceUsage(procedureToResourceUsageMap
					.get(topProcedure));
		}
	}

	@Override
	public void visit(Task task) {
		procedureResourceUsageStack = new Stack<FPGAResource>();
		super.visit(task);
	}

	@Override
	public void visit(Call call) {
		if (call.getOwner() != null) {
			procedureResourceUsageStack.push(currentResourceUsage);
		}

		super.visit(call);

		if (call.getOwner() != null) {
			FPGAResource callResource = currentResourceUsage;
			currentResourceUsage = procedureResourceUsageStack.pop();
			currentResourceUsage.addResourceUsage(callResource);
		}
	}

	@Override
	public void visit(Procedure procedure) {
		currentResourceUsage = new FPGAResource();

		if (!procedureToResourceUsageMap.containsKey(procedure)) {
			super.visit(procedure);
			procedureToResourceUsageMap.put(procedure, currentResourceUsage);
		}
	}

	@Override
	public void visit(Latch latch) {
		for (Component component : latch.getComponents()) {
			component.accept(this);
		}
	}

	@Override
	public void visit(PinReferee pref) {
		for (Component component : pref.getComponents()) {
			component.accept(this);
		}
	}

	@Override
	public void visit(Kicker kicker) {
		for (Component component : kicker.getComponents()) {
			component.accept(this);
		}
	}

	@Override
	public void visit(Scoreboard scoreboard) {
		for (Component component : scoreboard.getComponents()) {
			component.accept(this);
		}
	}

	@Override
	public void visit(MemoryBank memBank) {
		VerilogMemory vm = MemoryMapper.getMemoryType(memBank);
		Ram match = vm.getLowestCost(Ram.getMappers(EngineThread
				.getGenericJob().getPart(CodeLabel.UNSCOPED), memBank
				.getImplementation().isLUT()));
		int new_width = (int) java.lang.Math.ceil((double) vm.getDataWidth()
				/ (double) match.getWidth());
		int new_depth = (int) java.lang.Math.ceil((double) vm.getDepth()
				/ (double) match.getDepth());
		if (match instanceof LutRam) {
			currentResourceUsage.addSpLutRam(match.getCost() * new_width
					* new_depth);
		} else if (match instanceof DualPortLutRam) {
			currentResourceUsage.addDpLutRam(match.getCost() * new_width
					* new_depth);
		} else if (match instanceof BlockRam) {
			currentResourceUsage.addSpBlockRam(match.getCost() * new_width
					* new_depth);
		} else if (match instanceof DualPortBlockRam) {
			currentResourceUsage.addDpBlockRam(match.getCost() * new_width
					* new_depth);
		} else {
			currentResourceUsage
					.addRom(match.getCost() * new_width * new_depth);
		}
	}

	@Override
	public void visit(MemoryRead memoryRead) {
		MemoryRead.Physical physical = (MemoryRead.Physical) memoryRead
				.getPhysicalComponent();
		Collection<Component> components = physical.getComponents();
		components.remove(physical.getInBuf());
		components.removeAll(physical.getOutBufs());
		for (Component component : components) {
			component.accept(this);
		}
	}

	@Override
	public void visit(MemoryWrite memoryWrite) {
		MemoryWrite.Physical physical = (MemoryWrite.Physical) memoryWrite
				.getPhysicalComponent();
		Collection<Component> components = physical.getComponents();
		components.remove(physical.getInBuf());
		components.removeAll(physical.getOutBufs());
		for (Component component : components) {
			component.accept(this);
		}
	}

	/**
	 * Handle any kind of {@link Module}. All visit(Module subclass) methods
	 * should call this as part of the visit. The default behavior of this
	 * method is to call filterAny(Component).
	 */
	@Override
	public void filter(Module m) {
		if (_report.db)
			_lim.ln("filter(Module): " + m.toString());
		filterAny(m);
	}

	@Override
	public void preFilter(Module m) {
		if (_report.db)
			_lim.ln("pre-filter(Module): " + m.toString());
		preFilterAny(m);
	}

	/**
	 * Handle any kind of {@link Operation}. All visit(Operation subclass)
	 * methods should call this as part of the visit.
	 */
	@Override
	public void filter(Operation o) {
		if (_report.db)
			_lim.ln("filter(Operation): " + o.toString());
		filterAny(o);
	}

	@Override
	public void preFilter(Operation o) {
		if (_report.db)
			_lim.ln("pre-filter(Operation): " + o.toString());
		preFilterAny(o);
	}

	/**
	 * Handle any kind of {@link Primitive}. All visit(Primitive subclass)
	 * methods should call this as part of the visit.
	 */
	@Override
	public void filter(Primitive p) {
		if (_report.db)
			_lim.ln("filter(Primitive): " + p.toString());
		filterAny(p);
	}

	@Override
	public void preFilter(Primitive p) {
		if (_report.db)
			_lim.ln("pre-filter(Primitive): " + p.toString());
		preFilterAny(p);
	}

	/**
	 * Handles {@link Call}. The visit(Call call) calls this as part of the
	 * visit
	 */
	@Override
	public void filter(Call c) {
		if (_report.db)
			_lim.ln("filter(Call): " + c.toString());
		filterAny(c);
	}

	@Override
	public void preFilter(Call c) {
		if (_report.db)
			_lim.ln("pre-filter(Call): " + c.toString());
		preFilterAny(c);
	}

	/**
	 * The default behavior of each filter() method is to call this, allowing
	 * any component to be handled generically.
	 * 
	 * @param c
	 *            the component that was visited
	 */
	@Override
	public void filterAny(Component c) {
		if (_report.db)
			_lim.ln("filterAny(Component): " + c.toString());
		currentResourceUsage.addResourceUsage(c.getHardwareResourceUsage());
	}

	@Override
	public void preFilterAny(Component c) {
		if (_report.db)
			_lim.ln("pre-filterAny(Component): " + c.toString());
	}

	@Override
	protected void traverse(Design d) {
		if (_report.db)
			_lim.ln("FilteredScan.traverse(Design)");
		scanner.enter(d);
	}

	@Override
	protected void traverse(Task t) {
		if (_report.db)
			_lim.ln("FilteredScan.traverse(Task)");
		scanner.enter(t);
	}

	@Override
	protected void traverse(Call c) {
		if (_report.db)
			_lim.ln("FilteredScan.traverse(Call)");
		scanner.enter(c);
	}

	@Override
	protected void traverse(Procedure p) {
		if (_report.db)
			_lim.ln("FilteredScan.traverse(Procedure)");
		scanner.enter(p);
	}

	public Map<Procedure, FPGAResource> getProcedureResourceUsageMap() {
		return procedureToResourceUsageMap;
	}
}
