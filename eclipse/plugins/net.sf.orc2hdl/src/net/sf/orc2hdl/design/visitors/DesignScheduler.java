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
package net.sf.orc2hdl.design.visitors;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sf.openforge.lim.Bus;
import net.sf.openforge.lim.Component;
import net.sf.openforge.lim.Decision;
import net.sf.openforge.lim.Port;
import net.sf.openforge.lim.Task;
import net.sf.openforge.lim.memory.LogicalValue;
import net.sf.orc2hdl.design.ResourceCache;
import net.sf.orc2hdl.design.util.ModuleUtil;
import net.sf.orc2hdl.design.util.PortUtil;
import net.sf.orcc.df.Action;
import net.sf.orcc.df.Actor;
import net.sf.orcc.df.util.DfVisitor;
import net.sf.orcc.ir.ExprVar;
import net.sf.orcc.ir.InstAssign;
import net.sf.orcc.ir.InstReturn;
import net.sf.orcc.ir.IrFactory;
import net.sf.orcc.ir.Type;
import net.sf.orcc.ir.Var;

/**
 * 
 * DesignScheduler class constructs the actors scheduler and it transforms it to
 * a task.
 * 
 * @author Endri Bezati
 * 
 */
public class DesignScheduler extends DfVisitor<Task> {

	private class InnerComponentCreator extends ComponentCreator {

		public InnerComponentCreator(ResourceCache resources,
				List<Component> component, Map<Port, Var> portDependency,
				Map<Bus, Var> busDependency,
				Map<Port, Integer> portGroupDependency,
				Map<Bus, Integer> doneBusDependency) {
			super(resources, component, portDependency, busDependency,
					portGroupDependency, doneBusDependency);
		}

		@Override
		public List<Component> caseInstReturn(InstReturn returnInstr) {
			if (returnInstr.getValue().isExprVar()) {
				// Get Return Var of the procedure
				Var returnVar = ((ExprVar) returnInstr.getValue()).getUse()
						.getVariable();
				Type type = IrFactory.eINSTANCE.createTypeBool();
				Var schedulerReturn = IrFactory.eINSTANCE.createVar(0, type,
						"isSchedulable_" + currentAction.getName() + "_go",
						false, 0);
				InstAssign noop = IrFactory.eINSTANCE.createInstAssign(
						schedulerReturn,
						IrFactory.eINSTANCE.createExprVar(returnVar));
				doSwitch(noop);
				schedulerReturnVar.put(currentAction, schedulerReturn);
			}
			return null;
		}

	}

	/** Map of action associated to its Task **/
	private final Map<Action, Task> actorsTasks;

	/** Dependency between Components and Bus-Var **/
	private final Map<Bus, Var> busDependency;

	/** All components of the scheduler **/
	private List<Component> componentsList;

	/** The current visited Action **/
	private Action currentAction;

	/** Dependency between Components and Done Bus **/
	private final Map<Bus, Integer> doneBusDependency;

	/** Component Creator (Instruction Visitor) **/
	private final InnerComponentCreator innerComponentCreator;

	/** The inputPattern Test components **/
	private Map<Action, List<Component>> inputPatternComponents;

	/** The isSchedulable Test components **/
	private Map<Action, List<Component>> isSchedulableComponents;

	/** The outputPattern Test components **/
	private Map<Action, List<Component>> outputPatternComponents;

	/** The actors ports association with pinStatus Var **/
	private final Map<net.sf.orcc.df.Port, Var> pinStatus;

	/** Dependency between Components and Port-Var **/
	private final Map<Port, Var> portDependency;

	/** Dependency between Components and Done Bus **/
	private final Map<Port, Integer> portGroupDependency;

	/** The resource Cache **/
	private final ResourceCache resources;

	/** All Return Vars given by the action scheduler **/
	private final Map<Action, Var> schedulerReturnVar;

	/** Design stateVars **/
	private final Map<LogicalValue, Var> stateVars;

	public DesignScheduler(ResourceCache resources,
			Map<Action, Task> actorsTasks, List<Component> componentsList,
			Map<LogicalValue, Var> stateVars) {
		this.resources = resources;
		this.actorsTasks = actorsTasks;
		this.componentsList = componentsList;
		this.stateVars = stateVars;
		busDependency = new HashMap<Bus, Var>();
		doneBusDependency = new HashMap<Bus, Integer>();
		portDependency = new HashMap<Port, Var>();
		pinStatus = new HashMap<net.sf.orcc.df.Port, Var>();
		portGroupDependency = new HashMap<Port, Integer>();
		schedulerReturnVar = new HashMap<Action, Var>();
		innerComponentCreator = new InnerComponentCreator(resources,
				componentsList, portDependency, busDependency,
				portGroupDependency, doneBusDependency);
	}

	@Override
	public Task caseAction(Action action) {
		currentAction = action;
		componentsList = new ArrayList<Component>();
		return null;
	}

	@Override
	public Task caseActor(Actor actor) {
		Task task = null;
		/** Initialize fields **/
		componentsList = new ArrayList<Component>();
		isSchedulableComponents = new HashMap<Action, List<Component>>();
		inputPatternComponents = new HashMap<Action, List<Component>>();
		outputPatternComponents = new HashMap<Action, List<Component>>();

		/** Build the Loop Decision **/

		Decision loopDecision = ModuleUtil.createTrueDecision(
				"var_" + actor.getName() + "_loop", portDependency,
				busDependency, portGroupDependency, doneBusDependency);

		/** Get the actor Pin Status from actors Ports **/
		for (net.sf.orcc.df.Port port : actor.getInputs()) {
			if (!port.isNative()) {
				Component pinStatusComponent = PortUtil
						.createPinStatusComponent(port, resources, pinStatus,
								busDependency, doneBusDependency);
				componentsList.add(pinStatusComponent);
			}
		}

		for (net.sf.orcc.df.Port port : actor.getOutputs()) {
			if (!port.isNative()) {
				Component pinStatusComponent = PortUtil
						.createPinStatusComponent(port, resources, pinStatus,
								busDependency, doneBusDependency);
				componentsList.add(pinStatusComponent);
			}
		}

		/** Visit actions and get all the components from the action scheduler **/
		for (Action action : actor.getActions()) {
			doSwitch(action);
			// Add all the components to componentsList
			componentsList.addAll(isSchedulableComponents.get(action));
			componentsList.addAll(inputPatternComponents.get(action));
			componentsList.addAll(outputPatternComponents.get(action));
		}

		return task;
	}
}
