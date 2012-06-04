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
import net.sf.openforge.lim.Entry;
import net.sf.openforge.lim.Exit;
import net.sf.openforge.lim.InBuf;
import net.sf.openforge.lim.Module;
import net.sf.openforge.lim.OutBuf;
import net.sf.openforge.lim.Port;
import net.sf.openforge.lim.ResetDependency;
import net.sf.openforge.lim.op.NoOp;
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
	 * This method creates an NoOp component used for assign that are not in the
	 * Orcc Ir
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
			Map<Component, Map<Port, Var>> portDependency,
			Map<Component, Map<Bus, List<Var>>> busDependency,
			Map<Component, Map<Bus, Integer>> doneBusDependency) {
		Component component = new NoOp(1, Exit.DONE);
		PortUtil.mapInDataPorts(Arrays.asList(source), component,
				portDependency);
		PortUtil.mapOutDataPorts(component, target, 0, busDependency,
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
			Block elseBlock, List<Var> inVars, Map<Var, List<Var>> outVars,
			String searchScope, Exit.Type exitType,
			Map<Component, Map<Port, Var>> portDependency,
			Map<Component, Map<Bus, List<Var>>> busDependency,
			Map<Component, Map<Bus, Integer>> doneBusDependency) {
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
				portDependency, busDependency);

		// Map In/Out port of branch, Branch done Group 0
		PortUtil.mapInDataPorts(inVars, branch, portDependency);
		PortUtil.mapOutControlPort(branch, 0, doneBusDependency);

		moduleDependencies(branch, branchComponents, portDependency,
				branch.getExit(Exit.DONE), portDependency, busDependency,
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
			Map<Component, Map<Port, Var>> portDependency,
			Map<Component, Map<Bus, List<Var>>> busDependency,
			Map<Component, Map<Bus, Integer>> doneBusDependency) {
		Decision decision = null;
		// Create the decision variable and assign the inputDecision to it
		Type type = IrFactory.eINSTANCE.createTypeBool();
		Var decisionVar = IrFactory.eINSTANCE.createVar(0, type, resultName,
				false, 0);

		Component assignComp = assignComponent(decisionVar, inputDecision,
				portDependency, busDependency, doneBusDependency);
		Module decisionModule = (Module) createModule(
				Arrays.asList(assignComp), Arrays.asList(inputDecision),
				Collections.<Var, List<Var>> emptyMap(), "decisionBlock",
				Exit.DONE, 0, portDependency, busDependency, doneBusDependency);

		// Add done dependency, Decision group 0
		PortUtil.mapOutControlPort(decisionModule, 0, doneBusDependency);

		// Create the decision
		decision = new Decision((Block) decisionModule, assignComp);

		// Propagate Inputs
		decisionPropagateInputs(decision, (Block) decisionModule);

		// Add to dependency, A Decision has only one Input
		Map<Port, Var> portDep = new HashMap<Port, Var>();
		Port port = decision.getDataPorts().get(0);
		portDep.put(port, inputDecision);
		portDependency.put(decision, portDep);

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
			List<Var> inVars, Map<Var, List<Var>> outVars, String searchScope,
			Exit.Type exitType, Integer group,
			Map<Component, Map<Port, Var>> portDependency,
			Map<Component, Map<Bus, List<Var>>> busDependency,
			Map<Component, Map<Bus, Integer>> doneBusDependency) {

		// Create an Empty Block
		Module module = new Block(false);

		// Create the modules IO interface
		createModuleInterface(module, inVars, outVars, exitType,
				portDependency, busDependency);
		// Set all dependencies

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
	public static void createModuleInterface(Module module, List<Var> inVars,
			Map<Var, List<Var>> outVars, Exit.Type exitType,
			Map<Component, Map<Port, Var>> portDependency,
			Map<Component, Map<Bus, List<Var>>> busDependency) {
		// Create Module Input(s) if any
		if (!inVars.isEmpty()) {
			Map<Port, Var> portDep = new HashMap<Port, Var>();
			for (Var var : inVars) {
				Port port = module.makeDataPort();
				port.setIDLogical(var.getIndexedName());
				portDep.put(port, var);
			}
			portDependency.put(module, portDep);
		}
		// Create module Exit
		moduleExit(module, exitType);

		// Create module Output(s) if any
		if (!outVars.isEmpty()) {
			Exit exit = module.getExit(Exit.DONE);
			Map<Bus, List<Var>> busDep = new HashMap<Bus, List<Var>>();
			for (Var var : outVars.keySet()) {
				Bus dataBus = exit.makeDataBus();
				Integer busSize = var.getType().getSizeInBits();
				boolean isSigned = var.getType().isInt()
						|| var.getType().isBool();
				dataBus.setSize(busSize, isSigned);
				dataBus.setIDLogical(var.getIndexedName());
				// Condition if the output of the module is a phi resolution or
				// a simple output
				if (outVars.get(var).isEmpty()) {
					busDep.put(dataBus, Arrays.asList(var));
				} else {
					busDep.put(dataBus, outVars.get(var));
				}
			}
			busDependency.put(module, busDep);
		}

	}

	public static void moduleDependencies(Module module,
			List<Component> components,
			Map<Component, Map<Port, Var>> dependecies, Exit exit,
			Map<Component, Map<Port, Var>> portDependency,
			Map<Component, Map<Bus, List<Var>>> busDependency,
			Map<Component, Map<Bus, Integer>> doneBusDependency) {

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
}
