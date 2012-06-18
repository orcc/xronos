/*
 * Copyright (c) 2012, Ecole Polytechnique Fédérale de Lausanne
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 *   * Redistributions of source code must retain the above copyright notice,
 *     this list of conditions and the following disclaimer.
 *   * Redistributions in binary form must reproduce the above copyright notice,
 *     this list of conditions and the following disclaimer in the documentation
 *     and/or other materials provided with the distribution.
 *   * Neither the name of the Ecole Polytechnique Fédérale de Lausanne nor the names of its
 *     contributors may be used to endorse or promote products derived from this
 *     software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY
 * WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 */

package net.sf.orc2hdl.design.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sf.openforge.lim.Block;
import net.sf.openforge.lim.Branch;
import net.sf.openforge.lim.Bus;
import net.sf.openforge.lim.ClockDependency;
import net.sf.openforge.lim.Component;
import net.sf.openforge.lim.ControlDependency;
import net.sf.openforge.lim.DataDependency;
import net.sf.openforge.lim.Decision;
import net.sf.openforge.lim.Dependency;
import net.sf.openforge.lim.Entry;
import net.sf.openforge.lim.Exit;
import net.sf.openforge.lim.InBuf;
import net.sf.openforge.lim.Loop;
import net.sf.openforge.lim.LoopBody;
import net.sf.openforge.lim.Module;
import net.sf.openforge.lim.OutBuf;
import net.sf.openforge.lim.Port;
import net.sf.openforge.lim.ResetDependency;
import net.sf.openforge.lim.Task;
import net.sf.openforge.lim.TaskCall;
import net.sf.openforge.lim.op.NoOp;
import net.sf.openforge.lim.op.SimpleConstant;
import net.sf.orcc.ir.IrFactory;
import net.sf.orcc.ir.Type;
import net.sf.orcc.ir.Var;

/**
 * This class contains several methods for building Design Modules
 * 
 * @author Endri Bezati
 * 
 */

public class ModuleUtil {
	/**
	 * This method creates an NoOp component used for assign that are not
	 * include in a procedure
	 * 
	 * @param target
	 *            the target variable
	 * @param source
	 *            the source variable
	 * @param portDependency
	 *            the port dependency map
	 * @param busDependency
	 *            the bus dependency map
	 * @param doneBusDependency
	 *            the bus done dependency map
	 * @return
	 */
	public static Component assignComponent(Var target, Var source,
			Map<Port, Var> portDependency, Map<Bus, Var> busDependency,
			Map<Port, Integer> portGroupDependency,
			Map<Bus, Integer> doneBusDependency) {
		Component component = new NoOp(1, Exit.DONE);

		GroupedVar inVar = new GroupedVar(source, 0);
		PortUtil.mapInDataPorts(component, inVar.getAsList(), portDependency,
				portGroupDependency);

		GroupedVar outVar = new GroupedVar(target, 0);
		PortUtil.mapOutDataPorts(component, outVar.getAsList(), busDependency,
				doneBusDependency);
		return component;
	}

	/**
	 * This method adds an entry to a component
	 * 
	 * @param component
	 *            the component
	 * @param drivingExit
	 *            the driveing Exit
	 * @param clockBus
	 *            the clock bus attached to the component
	 * @param resetBus
	 *            the reset bus attached to the component
	 * @param goBus
	 *            the go bus control of the component
	 */
	public static void componentAddEntry(Component component, Exit drivingExit,
			Bus clockBus, Bus resetBus, Bus goBus) {

		Entry entry = component.makeEntry(drivingExit);
		// Even though most components do not use the clock, reset and
		// go ports we set up the dependencies for consistency.
		entry.addDependency(component.getClockPort(), new ClockDependency(
				clockBus));
		entry.addDependency(component.getResetPort(), new ResetDependency(
				resetBus));
		entry.addDependency(component.getGoPort(), new ControlDependency(goBus));
	}

	public static Component createBranch(Decision decision, Block thenBlock,
			Block elseBlock, List<GroupedVar> inVars, List<GroupedVar> outVars,
			Map<Var, List<Var>> phiOuts, String searchScope,
			Exit.Type exitType, Map<Port, Var> portDependency,
			Map<Bus, Var> busDependency,
			Map<Port, Integer> portGroupDependency,
			Map<Bus, Integer> doneBusDependency) {
		Branch branch = null;
		List<Component> branchComponents = new ArrayList<Component>();
		if (elseBlock == null) {
			branch = new Branch(decision, thenBlock);
			branchComponents.add(decision);
			branchComponents.add(thenBlock);
		} else {
			branch = new Branch(decision, thenBlock, elseBlock);
			branchComponents.add(decision);
			branchComponents.add(thenBlock);
			branchComponents.add(elseBlock);
		}

		createModuleInterface(branch, inVars, outVars, exitType,
				portDependency, portGroupDependency, busDependency);

		// Map In/Out port of branch, Branch done Group 0
		PortUtil.mapInDataPorts(branch, inVars, portDependency,
				portGroupDependency);
		PortUtil.mapOutControlPort(branch, 0, doneBusDependency);

		// moduleDependencies(branch, branchComponents,
		// branch.getExit(Exit.DONE),
		// portDependency, busDependency, portGroupDependency,
		// doneBusDependency);

		branchDependencies(branch, decision, thenBlock, elseBlock,
				portDependency, busDependency, phiOuts, portGroupDependency,
				doneBusDependency);

		// Give the name of the searchScope
		branch.specifySearchScope(searchScope);
		return branch;
	}

	/**
	 * This method creates a Design decision
	 * 
	 * @param inputDecision
	 *            the input decision variable
	 * @param resultName
	 *            the final name of the inputDecision for the testBlock
	 * @param portDependency
	 *            the port dependency map
	 * @param busDependency
	 *            the bus dependency map
	 * @param doneBusDependency
	 *            the done bus dependency map
	 * @return
	 */
	public static Decision createDecision(Var inputDecision, String resultName,
			Map<Port, Var> portDependency, Map<Bus, Var> busDependency,
			Map<Port, Integer> portGroupDependency,
			Map<Bus, Integer> doneBusDependency) {
		Decision decision = null;
		// Create the decision variable and assign the inputDecision to it
		Type type = IrFactory.eINSTANCE.createTypeBool();
		Var decisionVar = IrFactory.eINSTANCE.createVar(0, type, resultName,
				false, 0);

		Component assignComp = assignComponent(decisionVar, inputDecision,
				portDependency, busDependency, portGroupDependency,
				doneBusDependency);

		GroupedVar inVars = new GroupedVar(inputDecision, 0);
		Module decisionModule = (Module) createModule(
				Arrays.asList(assignComp), inVars.getAsList(),
				Collections.<GroupedVar> emptyList(), "decisionBlock",
				Exit.DONE, 0, portDependency, busDependency,
				portGroupDependency, doneBusDependency);

		// Add done dependency, Decision group 0
		PortUtil.mapOutControlPort(decisionModule, 0, doneBusDependency);

		// Create the decision
		decision = new Decision((Block) decisionModule, assignComp);

		// Propagate Inputs
		decisionPropagateInputs(decision, (Block) decisionModule);

		// Add to dependency, A Decision has only one Input at group 0
		Port port = decision.getDataPorts().get(0);
		port.setIDLogical(inputDecision.getIndexedName());
		portDependency.put(port, inputDecision);
		portGroupDependency.put(port, 0);

		return decision;
	}

	/**
	 * This method creates a module
	 * 
	 * @param components
	 *            the list of component
	 * @param inVars
	 *            the module inputs
	 * @param outVars
	 *            the module outputs
	 * @param searchScope
	 *            A string which indicates the search scope
	 * @param exitType
	 *            the Exit Type Tag
	 * @param group
	 *            the group of the module (0 or 1)
	 * @param portDependency
	 *            the port dependency map
	 * @param busDependency
	 *            the bus dependency map
	 * @param doneBusDependency
	 *            the done bus dependency map
	 * @return
	 */
	public static Component createModule(List<Component> components,
			List<GroupedVar> inVars, List<GroupedVar> outVars,
			String searchScope, Exit.Type exitType, Integer group,
			Map<Port, Var> portDependency, Map<Bus, Var> busDependency,
			Map<Port, Integer> portGroupDependency,
			Map<Bus, Integer> doneBusDependency) {

		// Create an Empty Block
		Module module = new Block(false);

		// Create the modules IO interface
		createModuleInterface(module, inVars, outVars, exitType,
				portDependency, portGroupDependency, busDependency);

		// Populate Module
		modulePopulate(module, components);

		// Resolve all dependencies
		moduleDependencies(module, components, module.getExit(exitType),
				portDependency, busDependency, portGroupDependency,
				doneBusDependency);
		// Give the name of the searchScope
		module.specifySearchScope(searchScope);

		// The condition of emptiness on components means that the DONE exit
		// signal should not be connected with the other dependencies of its
		// container
		// if (!components.isEmpty()) {
		if (exitType != Exit.RETURN) {
			// Add done dependency on module, Exit group 0 of a "normal"
			// module
			PortUtil.mapOutControlPort(module, group, doneBusDependency);
		}

		return module;
	}

	/**
	 * This method creates the Input DataPorts and the Output DataBuses of a
	 * module
	 * 
	 * @param module
	 *            the module to add the I/O
	 * @param inVars
	 *            the module inputs
	 * @param outVars
	 *            the module outputs
	 * @param exitType
	 *            the Exit Type tag
	 * @param portDependency
	 *            the port dependency map
	 * @param busDependency
	 *            the bus dependency map
	 */
	public static void createModuleInterface(Module module,
			List<GroupedVar> inVars, List<GroupedVar> outVars,
			Exit.Type exitType, Map<Port, Var> portDependency,
			Map<Port, Integer> portGroupDependency, Map<Bus, Var> busDependency) {
		// Create Module Input(s) if any
		if (!inVars.isEmpty()) {
			for (GroupedVar groupedVar : inVars) {
				Var var = groupedVar.getVar();
				Port port = module.makeDataPort();
				port.setIDLogical(var.getIndexedName());
				portDependency.put(port, var);
				portGroupDependency.put(port, groupedVar.getGroup());
				busDependency.put(port.getPeer(), var);
			}
		}
		// Create module Exit
		moduleExit(module, exitType);

		// Create module Output(s) if any
		if (!outVars.isEmpty()) {
			Exit exit = module.getExit(Exit.DONE);
			for (GroupedVar groupedVar : outVars) {
				Var var = groupedVar.getVar();
				Bus dataBus = exit.makeDataBus();
				Integer busSize = var.getType().getSizeInBits();
				boolean isSigned = var.getType().isInt()
						|| var.getType().isBool();
				dataBus.setSize(busSize, isSigned);
				dataBus.setIDLogical(var.getIndexedName());

				portDependency.put(dataBus.getPeer(), var);
				portGroupDependency.put(dataBus.getPeer(),
						groupedVar.getGroup());
				busDependency.put(dataBus, var);
			}
		}

	}

	public static Component createTaskCall(Task task, Var outDecision,
			Map<Port, Var> portDependency, Map<Bus, Var> busDependency,
			Map<Port, Integer> portGroupDependency,
			Map<Bus, Integer> doneBusDependency) {

		String varName = "decision_outputPattern_" + task.showIDGlobal();
		Decision decision = createDecision(outDecision, varName,
				portDependency, busDependency, portGroupDependency,
				doneBusDependency);
		// Create the the TaskCall component and add the Task
		TaskCall taskCall = new TaskCall();
		taskCall.setTarget(task);

		// Map out TaskCall Done port
		PortUtil.mapOutControlPort(taskCall, 0, doneBusDependency);
		Block thenBlock = (Block) createModule(
				Arrays.asList((Component) taskCall),
				Collections.<GroupedVar> emptyList(),
				Collections.<GroupedVar> emptyList(), "taskCallThenBlock",
				Exit.DONE, 0, portDependency, busDependency,
				portGroupDependency, doneBusDependency);
		GroupedVar inVars = new GroupedVar(outDecision, 0);
		Component branch = createBranch(decision, thenBlock, null,
				inVars.getAsList(), Collections.<GroupedVar> emptyList(),
				Collections.<Var, List<Var>> emptyMap(),
				"callTask_" + task.getIDGlobalType(), Exit.DONE,
				portDependency, busDependency, portGroupDependency,
				doneBusDependency);

		Module module = (Module) createModule(Arrays.asList(branch),
				inVars.getAsList(), Collections.<GroupedVar> emptyList(),
				"taskCallBlock", Exit.DONE, 0, portDependency, busDependency,
				portGroupDependency, doneBusDependency);

		return module;
	}

	public static Decision createTrueDecision(String resultName,
			Map<Port, Var> portDependency, Map<Bus, Var> busDependency,
			Map<Port, Integer> portGroupDependency,
			Map<Bus, Integer> doneBusDependency) {
		Decision decision = null;

		// Create a constant of "1" and assign it to the varTrue
		Type typeBool = IrFactory.eINSTANCE.createTypeBool();
		Var trueVar = IrFactory.eINSTANCE.createVar(0, typeBool, "trueVar",
				false, 0);

		Component constant = new SimpleConstant(1, 1, false);

		GroupedVar vars = new GroupedVar(trueVar, 0);
		PortUtil.mapOutDataPorts(constant, vars.getAsList(), busDependency,
				doneBusDependency);

		// Create the decision variable and assign the inputDecision to it
		Type type = IrFactory.eINSTANCE.createTypeBool();
		Var decisionVar = IrFactory.eINSTANCE.createVar(0, type, resultName,
				false, 0);

		Component assignComp = assignComponent(decisionVar, trueVar,
				portDependency, busDependency, portGroupDependency,
				doneBusDependency);

		Module decisionModule = (Module) createModule(
				Arrays.asList(constant, assignComp),
				Collections.<GroupedVar> emptyList(),
				Collections.<GroupedVar> emptyList(), "decisionBlock",
				Exit.DONE, 0, portDependency, busDependency,
				portGroupDependency, doneBusDependency);

		// Add done dependency, Decision group 0
		PortUtil.mapOutControlPort(decisionModule, 0, doneBusDependency);

		// Create the decision
		decision = new Decision((Block) decisionModule, assignComp);

		// Propagate Inputs
		decisionPropagateInputs(decision, (Block) decisionModule);

		return decision;
	}

	/**
	 * This method propagates the input of the testBlock of the decision to its
	 * container. Any data inputs to the decision need to be propagated from the
	 * block to the decision. There should be no output ports to propagate. They
	 * are inferred true/false.
	 * 
	 * @param decision
	 * @param testBlock
	 */
	public static void decisionPropagateInputs(Decision decision,
			Block testBlock) {
		for (Port port : testBlock.getDataPorts()) {
			Port decisionPort = decision.makeDataPort();
			Entry entry = port.getOwner().getEntries().get(0);
			entry.addDependency(port,
					new DataDependency(decisionPort.getPeer()));
		}
	}

	public static List<Port> getDependencyTargetPorts(
			Collection<Component> components, Var var,
			Map<Port, Var> portDependency) {
		List<Port> targetPorts = new ArrayList<Port>();

		for (Component component : components) {
			for (Port port : component.getDataPorts()) {
				if (portDependency.get(port) == var) {
					targetPorts.add(port);
				}
			}
		}

		return targetPorts;
	}

	public static void moduleDependencies(Module module,
			List<Component> components, Exit exit,
			Map<Port, Var> portDependency, Map<Bus, Var> busDependency,
			Map<Port, Integer> portGroupDependency,
			Map<Bus, Integer> doneBusDependency) {

		for (Component component : module.getComponents()) {
			// Build Data Dependencies
			if (component instanceof Loop) {
				loopDependecies(component);
			} else {
				for (Bus bus : component.getDataBuses()) {
					Var busVar = busDependency.get(bus);
					List<Port> targetPorts = getDependencyTargetPorts(
							module.getComponents(), busVar, portDependency);
					for (Port port : targetPorts) {
						int group = portGroupDependency.get(port);
						List<Entry> entries = port.getOwner().getEntries();
						Entry entry = entries.get(group);
						Dependency dep = new DataDependency(bus);
						entry.addDependency(port, dep);
					}
				}
			}
			// Build control Dependencies
			if (!(component instanceof InBuf) && !(component instanceof OutBuf)
					&& !(component instanceof Decision)
					&& !(module instanceof Branch)
					&& !(component instanceof Loop)) {
				Bus doneBus = component.getExit(Exit.DONE).getDoneBus();
				Port donePort = exit.getDoneBus().getPeer();
				List<Entry> entries = donePort.getOwner().getEntries();
				Entry entry = entries.get(doneBusDependency.get(doneBus));
				Dependency dep = new ControlDependency(doneBus);
				entry.addDependency(donePort, dep);
			}
		}

	}

	public static void branchDependencies(Branch branch, Decision decision,
			Block thenBlock, Block elseBlock, Map<Port, Var> portDependency,
			Map<Bus, Var> busDependency, Map<Var, List<Var>> phiOuts,
			Map<Port, Integer> portGroupDependency,
			Map<Bus, Integer> doneBusDependency) {

		/** Input Dependencies **/
		InBuf inBuf = branch.getInBuf();
		for (Bus bus : inBuf.getDataBuses()) {
			Var busVar = busDependency.get(bus);
			// Decision
			for (Port port : decision.getDataPorts()) {
				if (portDependency.get(port) == busVar) {
					int group = portGroupDependency.get(port);
					List<Entry> entries = port.getOwner().getEntries();
					Entry entry = entries.get(group);
					Dependency dep = new DataDependency(bus);
					entry.addDependency(port, dep);
				}
			}
			// Then Block
			for (Port port : thenBlock.getDataPorts()) {
				if (portDependency.get(port) == busVar) {
					int group = portGroupDependency.get(port);
					List<Entry> entries = port.getOwner().getEntries();
					Entry entry = entries.get(group);
					Dependency dep = new DataDependency(bus);
					entry.addDependency(port, dep);
				}
			}
			if (elseBlock != null) {
				// Else Block
				for (Port port : elseBlock.getDataPorts()) {
					if (portDependency.get(port) == busVar) {
						int group = portGroupDependency.get(port);
						List<Entry> entries = port.getOwner().getEntries();
						Entry entry = entries.get(group);
						Dependency dep = new DataDependency(bus);
						entry.addDependency(port, dep);
					}
				}
			}

			// Check if there is any dependency from input to output (Group 1)
			for (Bus outBus : branch.getDataBuses()) {
				Port port = outBus.getPeer();
				Var var = portDependency.get(port);
				if (phiOuts.keySet().contains(var)) {
					List<Var> joinVars = phiOuts.get(var);
					if (joinVars.get(1) == busVar) {
						List<Entry> entries = port.getOwner().getEntries();
						Entry entry = entries.get(1);
						Dependency dep = new DataDependency(bus);
						entry.addDependency(port, dep);
					}
				}
			}
		}

		/** Then and Else Output block Dependencies **/
		for (Bus bus : thenBlock.getDataBuses()) {
			Var busVar = busDependency.get(bus);
			for (Bus outBus : branch.getDataBuses()) {
				Port port = outBus.getPeer();
				Var var = portDependency.get(port);
				if (phiOuts.keySet().contains(var)) {
					List<Var> joinVars = phiOuts.get(var);
					if (joinVars.get(0) == busVar) {
						List<Entry> entries = port.getOwner().getEntries();
						Entry entry = entries.get(0);
						Dependency dep = new DataDependency(bus);
						entry.addDependency(port, dep);
					}
				}
			}
		}

		// Control Dependency
		Bus doneBus = thenBlock.getExit(Exit.DONE).getDoneBus();
		Port donePort = branch.getExit(Exit.DONE).getDoneBus().getPeer();
		List<Entry> entries = donePort.getOwner().getEntries();
		Entry entry = entries.get(0);
		Dependency dep = new ControlDependency(doneBus);
		entry.addDependency(donePort, dep);
		
		if (elseBlock != null) {
			for (Bus bus : elseBlock.getDataBuses()) {
				Var busVar = busDependency.get(bus);
				for (Bus outBus : branch.getDataBuses()) {
					Port port = outBus.getPeer();
					Var var = portDependency.get(port);
					if (phiOuts.keySet().contains(var)) {
						List<Var> joinVars = phiOuts.get(var);
						if (joinVars.get(1) == busVar) {
							entries = port.getOwner().getEntries();
							entry = entries.get(1);
							dep = new DataDependency(bus);
							entry.addDependency(port, dep);
						}
					}
				}
			}
			
			doneBus = elseBlock.getExit(Exit.DONE).getDoneBus();
			donePort = branch.getExit(Exit.DONE).getDoneBus().getPeer();
			entries = donePort.getOwner().getEntries();
			entry = entries.get(1);
			dep = new ControlDependency(doneBus);
			entry.addDependency(donePort, dep);
		}

	}

	/**
	 * This method creates the Exit of a module
	 * 
	 * @param module
	 *            a module witch has not an exit
	 * @param exitType
	 *            the Exit Type Tag
	 */
	public static void moduleExit(Module module, Exit.Type exitType) {
		// Add an Exit iff a module has not
		if (module.getExit(Exit.DONE) == null) {
			if ((exitType != null) && (exitType != Exit.DONE)) {
				// Add a latency with an exitType Label
				module.makeExit(0, exitType);
			} else {
				// Add a latency zero Exit
				module.makeExit(0);
			}
		}
	}

	/**
	 * This method populates a module with a given list of components
	 * 
	 * @param module
	 *            an empty module
	 * @param components
	 *            the list of components to add to the module
	 */
	public static void modulePopulate(Module module, List<Component> components) {
		final InBuf inBuf = module.getInBuf();
		final Bus clockBus = inBuf.getClockBus();
		final Bus resetBus = inBuf.getResetBus();
		final Bus goBus = inBuf.getGoBus();

		// I believe that the drivingExit no longer relevant
		Exit drivingExit = inBuf.getExit(Exit.DONE);

		int index = 0;
		for (Component comp : components) {
			if (module instanceof Block) {
				((Block) module).insertComponent(comp, index++);
			} else {
				module.addComponent(comp);
			}

			componentAddEntry(comp, drivingExit, clockBus, resetBus, goBus);

			drivingExit = comp.getExit(Exit.DONE);
		}

		// Ensure that the OutBufs of the module have an entry
		for (OutBuf outbuf : module.getOutBufs()) {
			componentAddEntry(outbuf, drivingExit, clockBus, resetBus, goBus);
		}
	}

	@SuppressWarnings("unused")
	public static void loopDependecies(Component component) {
		Map<Port, Map<Port, Bus>> feedbackDeps = new HashMap<Port, Map<Port, Bus>>();
		Map<Port, Map<Port, Bus>> initialDeps = new HashMap<Port, Map<Port, Bus>>();
		Map<Port, Map<Port, Bus>> outputDeps = new HashMap<Port, Map<Port, Bus>>();

		LoopBody loopBody = ((Loop) component).getBody();
		Exit fbExit = loopBody.getFeedbackExit();
		Exit doneExit = loopBody.getLoopCompleteExit();
		Exit initExit = ((Loop) component).getInBuf().getExit(Exit.DONE);

		// Populate outputDeps with Done Exit
		Map<Port, Bus> doneMap = new HashMap<Port, Bus>();
		Bus doneBus = loopBody.getExit(Exit.DONE).getDoneBus();
		Port donePort = component.getExit(Exit.DONE).getDoneBus().getPeer();
		doneMap.put(donePort, doneBus);
		outputDeps.put(donePort, doneMap);

		Entry initEntry = ((Loop) component).getBodyInitEntry();
		Entry fbEntry = ((Loop) component).getBodyFeedbackEntry();
		Collection<Dependency> goInitDeps = initEntry.getDependencies(loopBody
				.getGoPort());
		Bus initDoneBus = goInitDeps.iterator().next().getLogicalBus();
		// Build the output dependencies
		Entry outbufEntry = ((Loop) component).getExit(Exit.DONE).getPeer()
				.getEntries().get(0);
		for (Port port : outputDeps.keySet()) {
			Port targetPort = port;
			Bus sourceBus = outputDeps.get(port).get(port);
			Dependency dep = (targetPort == targetPort.getOwner().getGoPort()) ? new ControlDependency(
					sourceBus) : new DataDependency(sourceBus);
			outbufEntry.addDependency(targetPort, dep);
		}
	}

}
