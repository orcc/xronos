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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sf.openforge.frontend.slim.builder.ActionIOHandler;
import net.sf.openforge.lim.And;
import net.sf.openforge.lim.Block;
import net.sf.openforge.lim.Branch;
import net.sf.openforge.lim.Bus;
import net.sf.openforge.lim.Component;
import net.sf.openforge.lim.Decision;
import net.sf.openforge.lim.Exit;
import net.sf.openforge.lim.Loop;
import net.sf.openforge.lim.Module;
import net.sf.openforge.lim.Port;
import net.sf.openforge.lim.Task;
import net.sf.openforge.lim.op.SimpleConstant;
import net.sf.orc2hdl.design.ResourceCache;
import net.sf.orc2hdl.design.util.DesignUtil;
import net.sf.orc2hdl.design.util.GroupedVar;
import net.sf.orc2hdl.design.util.ModuleUtil;
import net.sf.orc2hdl.design.util.PortUtil;
import net.sf.orcc.df.Action;
import net.sf.orcc.df.Actor;
import net.sf.orcc.df.FSM;
import net.sf.orcc.df.Pattern;
import net.sf.orcc.df.State;
import net.sf.orcc.df.util.DfVisitor;
import net.sf.orcc.ir.Def;
import net.sf.orcc.ir.ExprInt;
import net.sf.orcc.ir.ExprVar;
import net.sf.orcc.ir.Expression;
import net.sf.orcc.ir.InstAssign;
import net.sf.orcc.ir.InstLoad;
import net.sf.orcc.ir.InstReturn;
import net.sf.orcc.ir.IrFactory;
import net.sf.orcc.ir.Type;
import net.sf.orcc.ir.Use;
import net.sf.orcc.ir.Var;
import net.sf.orcc.ir.util.IrUtil;
import net.sf.orcc.util.util.EcoreHelper;

import org.eclipse.emf.ecore.util.EcoreUtil;

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
				Map<Port, Var> portDependency, Map<Bus, Var> busDependency,
				Map<Port, Integer> portGroupDependency,
				Map<Bus, Integer> doneBusDependency) {
			super(resources, portDependency, busDependency,
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
				actionSchedulerReturnVar.put(currentAction, schedulerReturn);
			}
			return null;
		}

	}

	/** All inputPattern Variable decisions **/
	private Map<Action, Var> actionInDecisions;

	/** All outputPattern Variable decisions **/
	private Map<Action, Var> actionOutDecisions;

	/** Map of action associated to its Task **/
	private Map<Action, Task> actorsTasks;

	/** Dependency between Components and Bus-Variable **/
	private Map<Bus, Var> busDependency;

	/** All components of the scheduler **/
	private List<Component> componentsList;

	/** The current visited Action **/
	private Action currentAction;

	/** Dependency between Components and Done Bus **/
	private Map<Bus, Integer> doneBusDependency;

	/** Component Creator (Instruction Visitor) **/
	private InnerComponentCreator innerComponentCreator;

	/** The inputPattern Test components **/
	private Map<Action, List<Component>> inputPatternComponents;

	/** The isSchedulable Test components **/
	private Map<Action, List<Component>> isSchedulableComponents;

	/** The outputPattern Test components **/
	private Map<Action, List<Component>> outputPatternComponents;

	/** The actors ports association with pinStatus Var **/
	private Map<net.sf.orcc.df.Port, Var> pinStatus;

	/** Dependency between Components and Port-Var **/
	private Map<Port, Var> portDependency;

	/** Dependency between Components and Done Bus **/
	private Map<Port, Integer> portGroupDependency;

	/** The resource Cache **/
	private ResourceCache resources;

	/** All Return Vars given by the action scheduler **/
	private Map<Action, Var> actionSchedulerReturnVar;

	/** Design stateVars **/
	// private Map<LogicalValue, Var> stateVars;

	private Map<Action, List<Component>> actionTests;

	private Integer componentCounter;

	private List<Component> schedulerLoopComponents;

	/** The scheduler current state Variable **/
	private Var currentState;

	private Map<State, Integer> stateToIndexMap;

	private Map<State, Var> stateToVar;

	public DesignScheduler(ResourceCache resources,
			Map<Action, Task> actorsTasks, Var currentState) {
		this.resources = resources;
		this.actorsTasks = actorsTasks;
		componentCounter = 0;
		schedulerLoopComponents = new ArrayList<Component>();
		isSchedulableComponents = new HashMap<Action, List<Component>>();
		inputPatternComponents = new HashMap<Action, List<Component>>();
		outputPatternComponents = new HashMap<Action, List<Component>>();
		actionInDecisions = new HashMap<Action, Var>();
		actionOutDecisions = new HashMap<Action, Var>();

		pinStatus = new HashMap<net.sf.orcc.df.Port, Var>();
		portGroupDependency = new HashMap<Port, Integer>();
		actionSchedulerReturnVar = new HashMap<Action, Var>();
		busDependency = new HashMap<Bus, Var>();
		doneBusDependency = new HashMap<Bus, Integer>();
		portDependency = new HashMap<Port, Var>();
		this.currentState = currentState;
		innerComponentCreator = new InnerComponentCreator(resources,
				portDependency, busDependency, portGroupDependency,
				doneBusDependency);
	}

	@Override
	public Task caseAction(Action action) {
		currentAction = action;
		componentsList = new ArrayList<Component>();

		/** Get the Pin Peek components **/
		doSwitch(action.getPeekPattern());

		/** Get the action scheduler components **/
		List<Component> visitedComponents = innerComponentCreator
				.doSwitch(action.getScheduler());
		componentsList.addAll(visitedComponents);
		/** Save the isSchedulable expressions **/

		isSchedulableComponents.put(action, componentsList);

		/** Build the inputPattern test components **/
		List<Component> inputPatternTestComponents = createPatternTest(action,
				action.getInputPattern(), "in");
		inputPatternComponents.put(action, inputPatternTestComponents);

		/** Build the outputPattern test components **/
		List<Component> outputPatternTestComponents = createPatternTest(action,
				action.getOutputPattern(), "out");
		outputPatternComponents.put(action, outputPatternTestComponents);

		return null;
	}

	@Override
	public Task caseActor(Actor actor) {
		List<Component> schedulerComponents = new ArrayList<Component>();
		/** Get the actor Pin Status from actors Ports **/
		for (net.sf.orcc.df.Port port : actor.getInputs()) {
			if (!port.isNative()) {
				Component pinStatusComponent = PortUtil
						.createPinStatusComponent(port, resources, pinStatus,
								busDependency, doneBusDependency);
				schedulerLoopComponents.add(pinStatusComponent);
			}
		}

		for (net.sf.orcc.df.Port port : actor.getOutputs()) {
			if (!port.isNative()) {
				Component pinStatusComponent = PortUtil
						.createPinStatusComponent(port, resources, pinStatus,
								busDependency, doneBusDependency);
				schedulerLoopComponents.add(pinStatusComponent);
			}
		}

		/** Visit actions and get all the components from the action scheduler **/
		for (Action action : actor.getActions()) {
			doSwitch(action);
			// Add all the components to componentsList
			if (isSchedulableComponents.get(action) != null) {
				schedulerLoopComponents.addAll(isSchedulableComponents
						.get(action));
			}
			if (inputPatternComponents.get(action) != null) {
				schedulerLoopComponents.addAll(inputPatternComponents
						.get(action));
			}
			if (outputPatternComponents.get(action) != null) {
				schedulerLoopComponents.addAll(outputPatternComponents
						.get(action));
			}
		}

		/** Create the action Test for all actions in the actor **/
		createActionTest(actor.getActions());

		/** Construct the scheduler if body **/
		Component branchBlock = null;
		if (actor.getFsm() == null) {
			branchBlock = createOutFsmScheduler(actor.getActionsOutsideFsm());
		} else {
			/** Fill up the state index and var maps **/
			stateToIndexMap = new HashMap<State, Integer>();
			stateToVar = new HashMap<State, Var>();
			int i = 0;
			for (State state : actor.getFsm().getStates()) {
				stateToIndexMap.put(state, i);
				stateToVar.put(
						state,
						IrFactory.eINSTANCE.createVar(
								IrFactory.eINSTANCE.createTypeInt(32), "s_"
										+ state.getName(), true, 0));
				i++;
			}
			/** Create literal components for each state **/
			for (State state : actor.getFsm().getStates()) {
				Component literal = new SimpleConstant(
						stateToIndexMap.get(state), 32, false);
				GroupedVar outVar = new GroupedVar(stateToVar.get(state), 0);
				PortUtil.mapOutDataPorts(literal, outVar.getAsList(),
						busDependency, doneBusDependency);
				schedulerComponents.add(literal);
			}
			/** Create the action transition **/
			createActionTransition(actor.getFsm());

		}

		/** Add the scheduler branch block to the scheduler Components **/
		schedulerLoopComponents.add(branchBlock);

		/** Build the Loop infinite true constant **/
		Var varActorSched = IrFactory.eINSTANCE.createVar(
				IrFactory.eINSTANCE.createTypeBool(),
				"var_" + actor.getSimpleName() + "_sched", true, 0);
		Component trueLoop = new SimpleConstant(1, 1, false);
		GroupedVar gVarActorSched = new GroupedVar(varActorSched, 0);
		PortUtil.mapOutDataPorts(trueLoop, gVarActorSched.getAsList(),
				busDependency, doneBusDependency);

		/** Add the constant to the scheduler components **/
		schedulerComponents.add(trueLoop);
		/** Create the infinite Loop **/
		Decision loopDecision = ModuleUtil.createDecision(varActorSched, "var_"
				+ actor.getSimpleName() + "_loop", portDependency,
				busDependency, portGroupDependency, doneBusDependency);

		/** Create the While body **/
		Module bodyModule = (Module) ModuleUtil.createModule(
				schedulerLoopComponents, Collections.<GroupedVar> emptyList(),
				Collections.<GroupedVar> emptyList(), "loopBody", Exit.DONE, 0,
				portDependency, busDependency, portGroupDependency,
				doneBusDependency);

		/** Create the infinite loop **/
		Loop loop = (Loop) ModuleUtil.createLoop(loopDecision, bodyModule,
				gVarActorSched.getAsList(),
				Collections.<GroupedVar> emptyList(), portDependency,
				busDependency, portGroupDependency, doneBusDependency);

		/** Add the loop to the scheduler components **/
		schedulerComponents.add(loop);

		/** Module of the scheduler **/
		Module scheduler = (Block) ModuleUtil.createModule(schedulerComponents,
				Collections.<GroupedVar> emptyList(),
				Collections.<GroupedVar> emptyList(), "schedulerBody",
				Exit.RETURN, 0, portDependency, busDependency,
				portGroupDependency, doneBusDependency);

		/** Create scheduler Task **/
		Task task = DesignUtil.createTask("scheduler", scheduler, true);

		return task;
	}

	@Override
	public Task casePattern(Pattern pattern) {
		for (net.sf.orcc.df.Port port : pattern.getPorts()) {
			Var oldTarget = pattern.getVariable(port);
			List<Use> uses = new ArrayList<Use>(oldTarget.getUses());
			for (Use use : uses) {
				// Create a custom peek for each load of this variable
				InstLoad load = EcoreHelper.getContainerOfType(use,
						InstLoad.class);

				// Get the index value
				Expression indexExpr = load.getIndexes().get(0);

				if (indexExpr.isExprVar()) {
					ExprVar literalExpr = (ExprVar) indexExpr;
					Var litteralVar = literalExpr.getUse().getVariable();
					Def varDef = litteralVar.getDefs().get(0);
					InstAssign instAssign = EcoreHelper.getContainerOfType(
							varDef, InstAssign.class);
					litteralVar.setType(IrFactory.eINSTANCE.createTypeInt(32));
					Integer value = ((ExprInt) instAssign.getValue())
							.getIntValue();

					Component constant = new SimpleConstant(value, 32,
							((ExprInt) instAssign.getValue()).getType().isInt());
					GroupedVar inOutVars = new GroupedVar(litteralVar, 0);
					constant.setNonRemovable();
					PortUtil.mapOutDataPorts(constant, inOutVars.getAsList(),
							busDependency, doneBusDependency);
					componentsList.add(constant);
					componentCounter++;

					// Get the variable
					Var peekVar = load.getTarget().getVariable();
					peekVar.setName("peek_" + currentAction.getName() + "_"
							+ peekVar.getIndexedName());
					// Create the pinPeek Component
					ActionIOHandler ioHandler = resources.getIOHandler(port);
					Component peekComponent = ioHandler.getTokenPeekAccess();
					peekComponent.setNonRemovable();
					// the peek is supported only in the head of the queue
					// PortUtil.mapInDataPorts(peekComponent,
					// inOutVars.getAsList(), portDependency,
					// portGroupDependency);

					GroupedVar outVars = new GroupedVar(peekVar, 0);
					PortUtil.mapOutDataPorts(peekComponent,
							outVars.getAsList(), busDependency,
							doneBusDependency);

					componentsList.add(peekComponent);
					componentCounter++;
					IrUtil.delete(load);
				}
				EcoreUtil.delete(oldTarget);
			}
		}

		return null;
	}

	private void createActionTest(List<Action> actions) {
		// Component 0: Decision, Component 1:ThenBlock
		actionTests = new HashMap<Action, List<Component>>();

		for (Action action : actions) {
			List<Component> actionTestComponent = new ArrayList<Component>();

			// Create the decision
			Var decisionVar = actionInDecisions.get(action);
			String varName = "decision_isSchedulable_" + action.getName();
			componentsList = new ArrayList<Component>();
			Decision decision = ModuleUtil.createDecision(decisionVar, varName,
					portDependency, busDependency, portGroupDependency,
					doneBusDependency);

			decision.setIDLogical("decision_" + action.getName());
			actionTestComponent.add(0, decision);

			// Create the "then" body
			Task actionToBeExecuted = actorsTasks.get(action);
			Var outDecision = actionOutDecisions.get(action);
			Block thenBlock = (Block) ModuleUtil.createTaskCall(
					actionToBeExecuted, outDecision, portDependency,
					busDependency, portGroupDependency, doneBusDependency);

			thenBlock.setIDLogical("thenBlock_" + action.getName());
			actionTestComponent.add(1, thenBlock);
			actionTests.put(action, actionTestComponent);
		}
	}

	private void createActionTransition(FSM fsm) {

	}

	private Branch createOutFsmScheduler(List<Action> actions) {
		Branch branch = null;

		List<Var> currentInputPorts = new ArrayList<Var>();
		List<Var> previousInputPorts = new ArrayList<Var>();

		int actionListSize = actions.size() - 1;

		for (int i = actionListSize; i >= 0; i--) {
			// Get Action
			Action action = actions.get(i);
			// Get The inputs of the branch
			currentInputPorts.add(actionOutDecisions.get(action));
			currentInputPorts.add(actionInDecisions.get(action));

			if (i == actionListSize) {
				List<Component> comps = actionTests.get(action);
				Decision decision = (Decision) comps.get(0);
				Block thenBlock = (Block) comps.get(1);

				List<GroupedVar> inVars = GroupedVar.ListGroupedVar(
						currentInputPorts, 0);
				branch = (Branch) ModuleUtil.createBranch(decision, thenBlock,
						null, inVars, Collections.<GroupedVar> emptyList(),
						null, "ifBranch_" + action.getName(), null,
						portDependency, busDependency, portGroupDependency,
						doneBusDependency);

				branch.setIDLogical("ifBranch_" + action.getName());
				previousInputPorts.add(currentInputPorts.get(1));
				previousInputPorts.add(currentInputPorts.get(0));

			} else {
				List<Component> comps = actionTests.get(action);
				Decision decision = (Decision) comps.get(0);
				Block thenBlock = (Block) comps.get(1);
				List<Var> inPort = new ArrayList<Var>(currentInputPorts);
				Collections.reverse(inPort);

				List<GroupedVar> inVars = GroupedVar.ListGroupedVar(
						previousInputPorts, 0);
				Block elseBlock = (Block) ModuleUtil.createModule(
						Arrays.asList((Component) branch), inVars,
						Collections.<GroupedVar> emptyList(), "elseBlock",
						Exit.DONE, 0, portDependency, busDependency,
						portGroupDependency, doneBusDependency);

				inVars = GroupedVar.ListGroupedVar(inPort, 0);
				branch = (Branch) ModuleUtil.createBranch(decision, thenBlock,
						elseBlock, inVars,
						Collections.<GroupedVar> emptyList(), null, "ifBranch_"
								+ action.getName(), null, portDependency,
						busDependency, portGroupDependency, doneBusDependency);
				branch.setIDLogical("ifBranch_" + action.getName());
				previousInputPorts = inPort;
			}
		}

		return branch;
	}

	private List<Component> createPatternTest(Action action, Pattern pattern,
			String direction) {
		List<Component> patternComponents = new ArrayList<Component>();
		List<Var> patternVars = new ArrayList<Var>();
		if (!pattern.isEmpty()) {
			for (net.sf.orcc.df.Port port : pattern.getPorts()) {
				if (pinStatus.containsKey(port)) {
					Var pinStatusVar = pinStatus.get(port);
					Type type = IrFactory.eINSTANCE.createTypeBool();
					Var patternPort = IrFactory.eINSTANCE.createVar(0, type,
							direction + "putPattern_" + currentAction.getName()
									+ "_" + port.getName(), false, 0);

					Component assign = ModuleUtil.assignComponent(patternPort,
							pinStatusVar, portDependency, busDependency,
							portGroupDependency, doneBusDependency);
					patternComponents.add(assign);
					patternVars.add(patternPort);
				}
				// Create the Go Decision
				// Put the return Variable to the inputPattern if "in"
				if (direction.equals("in")) {
					patternVars
							.add(actionSchedulerReturnVar.get(currentAction));
				} else {
					Type type = IrFactory.eINSTANCE.createTypeBool();
					Var resVar = IrFactory.eINSTANCE
							.createVar(0, type, "outputPattern_"
									+ currentAction.getName() + "_res", false,
									0);
					GroupedVar outVars = new GroupedVar(resVar, 0);
					Component constant = new SimpleConstant(1, 1, false);
					PortUtil.mapOutDataPorts(constant, outVars.getAsList(),
							busDependency, doneBusDependency);
					patternComponents.add(constant);
					patternVars.add(resVar);
				}

				Component andComponent = new And(patternVars.size());

				List<GroupedVar> inVars = GroupedVar.ListGroupedVar(
						patternVars, 0);
				PortUtil.mapInDataPorts(andComponent, inVars, portDependency,
						portGroupDependency);
				// Create Decision Variable
				Type type = IrFactory.eINSTANCE.createTypeBool();
				Var decisionVar = IrFactory.eINSTANCE.createVar(0, type,
						"isSchedulable_" + currentAction.getName() + "_"
								+ direction + "_decision", false, 0);
				GroupedVar outVars = new GroupedVar(decisionVar, 0);
				PortUtil.mapOutDataPorts(andComponent, outVars.getAsList(),
						busDependency, doneBusDependency);
				patternComponents.add(andComponent);
				// Save the decision, used on transitions
				if (direction.equals("in")) {
					actionInDecisions.put(currentAction, decisionVar);
				} else {
					actionOutDecisions.put(currentAction, decisionVar);
				}
			}
		} else {
			if (direction.equals("in")) {
				/** Create a True Pattern Port **/
				Type type = IrFactory.eINSTANCE.createTypeBool();
				Var patternPort = IrFactory.eINSTANCE.createVar(0, type,
						direction + "putPattern_" + currentAction.getName(),
						false, 0);
				GroupedVar outVars = new GroupedVar(patternPort, 0);
				Component constant = new SimpleConstant(1, 1, false);
				PortUtil.mapOutDataPorts(constant, outVars.getAsList(),
						busDependency, doneBusDependency);
				patternComponents.add(constant);
				patternVars.add(patternPort);

				/** Get the return of the scheduler **/
				patternVars.add(actionSchedulerReturnVar.get(currentAction));

				Component andComponent = new And(patternVars.size());

				List<GroupedVar> inVars = GroupedVar.ListGroupedVar(
						patternVars, 0);
				PortUtil.mapInDataPorts(andComponent, inVars, portDependency,
						portGroupDependency);
				// Create Decision Variable
				Var decisionVar = IrFactory.eINSTANCE.createVar(0, type,
						"isSchedulable_" + currentAction.getName() + "_"
								+ direction + "_decision", false, 0);
				outVars = new GroupedVar(decisionVar, 0);
				PortUtil.mapOutDataPorts(andComponent, outVars.getAsList(),
						busDependency, doneBusDependency);
				patternComponents.add(andComponent);

				actionInDecisions.put(currentAction, decisionVar);
			} else {
				Type type = IrFactory.eINSTANCE.createTypeBool();
				Var decisionVar = IrFactory.eINSTANCE.createVar(0, type,
						"isSchedulable_" + currentAction.getName() + "_"
								+ direction + "_decision", false, 0);

				Component constant = new SimpleConstant(1, 1, false);
				GroupedVar outVars = new GroupedVar(decisionVar, 0);
				PortUtil.mapOutDataPorts(constant, outVars.getAsList(),
						busDependency, doneBusDependency);
				patternComponents.add(constant);
				actionOutDecisions.put(currentAction, decisionVar);
			}
		}
		return patternComponents;
	}

}
