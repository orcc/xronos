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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sf.openforge.lim.Bus;
import net.sf.openforge.lim.Call;
import net.sf.openforge.lim.Component;
import net.sf.openforge.lim.Design;
import net.sf.openforge.lim.Exit;
import net.sf.openforge.lim.Module;
import net.sf.openforge.lim.Port;
import net.sf.openforge.lim.Task;
import net.sf.openforge.lim.memory.LogicalValue;
import net.sf.orc2hdl.design.ResourceCache;
import net.sf.orc2hdl.design.util.DesignUtil;
import net.sf.orc2hdl.design.util.ModuleUtil;
import net.sf.orc2hdl.design.util.PortUtil;
import net.sf.orc2hdl.preference.Constants;
import net.sf.orcc.df.Action;
import net.sf.orcc.df.Actor;
import net.sf.orcc.df.State;
import net.sf.orcc.df.util.DfVisitor;
import net.sf.orcc.ir.IrFactory;
import net.sf.orcc.ir.Var;

/**
 * The DesignActor class converts an Orcc Actor to an OpenForge Design
 * 
 * @author Endri Bezati
 * 
 */
public class DesignActor extends DfVisitor<Object> {

	/** Map of action associated to its Task **/
	private final Map<Action, Task> actorsTasks = new HashMap<Action, Task>();

	/** The list of components **/
	private List<Component> componentsList;

	/** The design that is being populated **/
	private final Design design;

	/** Component Creator (Instruction Visitor) **/
	private final ComponentCreator componentCreator;

	/** Instruction Visitor **/
	private final StateVarVisitor stateVarVisitor;

	/** The resource Cache **/
	private final ResourceCache resources;

	/** Design stateVars **/
	private final Map<LogicalValue, Var> stateVars;

	/** Dependency between Components and Bus-Var **/
	private final Map<Bus, Var> busDependency;

	/** Dependency between Components and Done Bus **/
	private final Map<Bus, Integer> doneBusDependency;

	/** Dependency between Components and Done Bus **/
	private final Map<Port, Integer> portGroupDependency;

	/** Dependency between Components and Port-Var **/
	private final Map<Port, Var> portDependency;

	public DesignActor(Design design, ResourceCache resources) {
		this.design = design;
		this.resources = resources;
		busDependency = new HashMap<Bus, Var>();
		doneBusDependency = new HashMap<Bus, Integer>();
		portDependency = new HashMap<Port, Var>();
		portGroupDependency = new HashMap<Port, Integer>();
		stateVars = new HashMap<LogicalValue, Var>();
		stateVarVisitor = new StateVarVisitor(stateVars);
		componentsList = new ArrayList<Component>();
		componentCreator = new ComponentCreator(resources, portDependency,
				busDependency, portGroupDependency, doneBusDependency);
	}

	@Override
	public Object caseAction(Action action) {
		/** Initialize the component List Field **/
		componentsList = new ArrayList<Component>();

		/** Get action Input(s) **/
		for (net.sf.orcc.df.Port port : action.getInputPattern().getPorts()) {
			List<Component> pinRead = PortUtil.createPinReadComponent(action,
					port, resources, busDependency, doneBusDependency);
			componentsList.addAll(pinRead);
		}

		/** Visit the action body and take all the generated components **/
		List<Component> bodyComponents = componentCreator.doSwitch(action
				.getBody());
		componentsList.addAll(bodyComponents);

		/** Get action Output(s) **/
		for (net.sf.orcc.df.Port port : action.getOutputPattern().getPorts()) {
			List<Component> pinWrite = PortUtil.createPinWriteComponent(action,
					port, resources, portDependency, portGroupDependency,
					doneBusDependency);
			componentsList.addAll(pinWrite);
		}

		/** Create the Design Task of the Action **/
		String taskName = action.getName();

		/** Build the Task Module which contains all the components **/
		Module taskModule = (Module) ModuleUtil.createModule(componentsList,
				Collections.<Var> emptyList(), Collections.<Var> emptyList(),
				taskName + "Body", false, Exit.RETURN, 0, portDependency,
				busDependency, portGroupDependency, doneBusDependency);
		/** Create the task **/
		Task task = DesignUtil.createTask(taskName, taskModule, false);
		task.setIDLogical(taskName);

		return task;
	}

	@Override
	public Object caseActor(Actor actor) {
		/** Build the Design Ports **/
		PortUtil.createDesignPorts(design, actor.getInputs(), "in", resources);
		PortUtil.createDesignPorts(design, actor.getOutputs(), "out", resources);

		/** Get the values of the parameters before visiting **/
		for (Var parameter : actor.getParameters()) {
			irVisitor.doSwitch(parameter);
		}
		Var currentState = null;
		// Add currentState stateVariable if the actor has an FSM
		if (actor.getFsm() != null) {
			currentState = IrFactory.eINSTANCE.createVarInt("currentState", 32,
					true, 0);
			int i = 0;
			Map<State, Integer> stateIndex = new HashMap<State, Integer>();
			for (State state : actor.getFsm().getStates()) {
				stateIndex.put(state, i);
				i++;
			}
			int index = stateIndex.get(actor.getFsm().getInitialState());
			currentState.setInitialValue(IrFactory.eINSTANCE
					.createExprInt(index));
			currentState.setValue(index);
			actor.getStateVars().add(currentState);
		}

		/** Visit the State Variables **/
		for (Var stateVar : actor.getStateVars()) {
			stateVarVisitor.doSwitch(stateVar);
		}

		/** Allocate Memory for the state variables **/
		DesignUtil.designAllocateMemory(design, stateVars,
				Constants.MAX_ADDR_WIDTH, resources);

		/** Create a Task for each action in the actor **/
		for (Action action : actor.getActions()) {
			Task task = (Task) doSwitch(action);
			actorsTasks.put(action, task);
			design.addTask(task);
		}

		/** Create the design scheduler **/
		DesignScheduler designScheduler = new DesignScheduler(resources,
				actorsTasks, currentState);

		/** Add scheduler task to the design **/
		Task scheduler = designScheduler.doSwitch(actor);
		design.addTask(scheduler);

		for (Task task : design.getTasks()) {
			Call call = task.getCall();
			if (call.getExit(Exit.DONE).getDoneBus().isConnected()) {
				// OutputPin pin = new OutputPin(call.getExit(Exit.DONE)
				// .getDoneBus());
				// pin.setIDLogical(task.showIDLogical() + "_done");
				// design.addOutputPin(pin,
				// call.getExit(Exit.DONE).getDoneBus());
				call.getProcedure().getBody().setProducesDone(true);
			}
			if (call.getGoPort().isConnected()) {
				// Bus goBus = call.getGoPort().getBus();
				// OutputPin pin = new OutputPin(1, false);
				// pin.setIDLogical(task.showIDLogical() + "_go");
				// design.addOutputPin(pin, goBus);
				call.getProcedure().getBody().setConsumesGo(true);
			}
		}

		return null;
	}
}
