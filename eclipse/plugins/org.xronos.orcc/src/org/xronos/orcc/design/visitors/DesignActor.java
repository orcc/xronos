/* 
 * XRONOS, High Level Synthesis of Streaming Applications
 * 
 * Copyright (C) 2014 EPFL SCI STI MM
 *
 * This file is part of XRONOS.
 *
 * XRONOS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * XRONOS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with XRONOS.  If not, see <http://www.gnu.org/licenses/>.
 * 
 * Additional permission under GNU GPL version 3 section 7
 * 
 * If you modify this Program, or any covered work, by linking or 
 * combining it with Eclipse libraries (or a modified version of that 
 * library), containing parts covered by the terms of EPL,
 * the licensors of this Program grant you additional permission to convey 
 * the resulting work. {Corresponding Source for a non-source form of such 
 * a combination shall include the source code for the parts of Eclipse 
 * libraries used as well as that of the  covered work.}
 */

package org.xronos.orcc.design.visitors;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sf.orcc.df.Action;
import net.sf.orcc.df.Actor;
import net.sf.orcc.df.util.DfVisitor;
import net.sf.orcc.ir.Procedure;
import net.sf.orcc.ir.Var;

import org.xronos.openforge.lim.Block;
import org.xronos.openforge.lim.Bus;
import org.xronos.openforge.lim.Call;
import org.xronos.openforge.lim.Component;
import org.xronos.openforge.lim.Design;
import org.xronos.openforge.lim.Exit;
import org.xronos.openforge.lim.Module;
import org.xronos.openforge.lim.Port;
import org.xronos.openforge.lim.Task;
import org.xronos.openforge.lim.io.SimplePin;
import org.xronos.openforge.lim.memory.LogicalValue;
import org.xronos.orcc.backend.debug.DebugPrinter;
import org.xronos.orcc.design.ResourceCache;
import org.xronos.orcc.design.util.DesignUtil;
import org.xronos.orcc.design.util.ModuleUtil;
import org.xronos.orcc.design.util.PortUtil;
import org.xronos.orcc.design.visitors.io.SchedulerSimplePin;
import org.xronos.orcc.preference.Constants;

/**
 * The DesignActor class converts an Orcc Actor to an OpenForge Design
 * 
 * @author Endri Bezati
 * 
 */
public class DesignActor extends DfVisitor<Object> {

	/** The list of components **/
	private List<Component> componentsList;

	/** The design that is being populated **/
	private Design design;

	/** Instruction Visitor **/
	private StateVarVisitor stateVarVisitor;

	/** The resource Cache **/
	private ResourceCache resources;

	/** Design stateVars **/
	private Map<LogicalValue, Var> stateVars;

	/** Dependency between Components and Bus-Var **/
	private Map<Bus, Var> busDependency;

	/** Dependency between Components and Done Bus **/
	private Map<Bus, Integer> doneBusDependency;

	/** Dependency between Components and Done Bus **/
	private Map<Port, Integer> portGroupDependency;

	/** Dependency between Components and Port-Var **/
	private Map<Port, Var> portDependency;

	/** Output the scheduling signals, token availability and Idle state **/
	private boolean schedulerInformation;

	/** Scheduling information pins **/
	Map<String, SimplePin> schedulingInfoPins;

	public DesignActor(Design design, ResourceCache resources,
			boolean schedulerInformation) {
		this.design = design;
		this.resources = resources;
		this.schedulerInformation = schedulerInformation;
		busDependency = new HashMap<Bus, Var>();
		doneBusDependency = new HashMap<Bus, Integer>();
		portDependency = new HashMap<Port, Var>();
		portGroupDependency = new HashMap<Port, Integer>();
		stateVars = new HashMap<LogicalValue, Var>();
		stateVarVisitor = new StateVarVisitor(stateVars);
		componentsList = new ArrayList<Component>();
		schedulingInfoPins = new HashMap<String, SimplePin>();
	}

	@Override
	public Object caseAction(Action action) {
		/** Initialize the component List Field **/
		componentsList = new ArrayList<Component>();

		/** Visit the action body and take all the generated components **/
		ComponentCreator actionComponentCreator = new ComponentCreator(
				resources, schedulingInfoPins, portDependency, busDependency,
				portGroupDependency, doneBusDependency);
		List<Component> bodyComponents = actionComponentCreator.doSwitch(action
				.getBody());
		componentsList.addAll(bodyComponents);

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

		/** Add the Scheduler information ports **/
		if (schedulerInformation) {
			for (Action action : actor.getActions()) {
				if (!action.getInputPattern().getPorts().isEmpty()) {
					String pinName = "ta_" + action.getName();
					SchedulerSimplePin schedulerPin = new SchedulerSimplePin(1,
							pinName);
					schedulingInfoPins.put(pinName, schedulerPin);
					design.addComponentToDesign(schedulerPin);
				}
			}
			// Add idle Pin
			SchedulerSimplePin idlePin = new SchedulerSimplePin(1, "idle_"
					+ actor.getName());
			schedulingInfoPins.put("idle_" + actor.getName(), idlePin);
			design.addComponentToDesign(idlePin);

			// Add current Action Pin
			SchedulerSimplePin currentActionPin = new SchedulerSimplePin(actor
					.getActions().size(), "idle_action_" + actor.getName());
			schedulingInfoPins.put("idle_action_" + actor.getName(),
					currentActionPin);
			design.addComponentToDesign(currentActionPin);
		}

		/** Visit the State Variables **/
		for (Var stateVar : actor.getStateVars()) {
			stateVarVisitor.doSwitch(stateVar);
		}

		/** Allocate Memory for the state variables **/
		DesignUtil.designAllocateMemory(design, stateVars,
				Constants.MAX_ADDR_WIDTH, resources);

		Map<Action, Task> actorsTasks = new HashMap<Action, Task>();
		/** Create a Task for each action in the actor **/
		for (Action action : actor.getActions()) {
			Task task = (Task) doSwitch(action);
			actorsTasks.put(action, task);
			design.addTask(task);
		}
		resources.setActionToTask(actorsTasks);

		// Get scheduler
		Procedure procedure = actor.getProcedure("scheduler");

		// Print Debug

		DebugPrinter debugPrinter = new DebugPrinter();
		debugPrinter.printActor("/tmp", actor, procedure, actor.getName(),
				resources);

		List<Component> schedulerComponents = new ArrayList<Component>();
		ComponentCreator schedulerComponentCreator = new ComponentCreator(
				resources, schedulingInfoPins, portDependency, busDependency,
				portGroupDependency, doneBusDependency);
		schedulerComponents = schedulerComponentCreator.doSwitch(procedure);

		/** Module of the scheduler **/
		Module schedulerModule = (Block) ModuleUtil.createModule(
				schedulerComponents, Collections.<Var> emptyList(),
				Collections.<Var> emptyList(), "schedulerBody", false,
				Exit.RETURN, 0, portDependency, busDependency,
				portGroupDependency, doneBusDependency);

		/** Create scheduler Task **/
		Task scheduler = DesignUtil.createTask("scheduler", schedulerModule,
				true);

		/** Add scheduler task to the design **/
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
