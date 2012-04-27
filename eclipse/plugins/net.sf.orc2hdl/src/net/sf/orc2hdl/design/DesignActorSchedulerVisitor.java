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

package net.sf.orc2hdl.design;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import net.sf.dftools.util.util.EcoreHelper;
import net.sf.openforge.frontend.slim.builder.ActionIOHandler;
import net.sf.openforge.lim.And;
import net.sf.openforge.lim.Block;
import net.sf.openforge.lim.Branch;
import net.sf.openforge.lim.Bus;
import net.sf.openforge.lim.Component;
import net.sf.openforge.lim.Decision;
import net.sf.openforge.lim.Design;
import net.sf.openforge.lim.Exit;
import net.sf.openforge.lim.LoopBody;
import net.sf.openforge.lim.Task;
import net.sf.openforge.lim.TaskCall;
import net.sf.openforge.lim.WhileBody;
import net.sf.orcc.df.Action;
import net.sf.orcc.df.Actor;
import net.sf.orcc.df.Instance;
import net.sf.orcc.df.Pattern;
import net.sf.orcc.ir.Def;
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

import org.eclipse.emf.ecore.util.EcoreUtil;

/**
 * DesignActorSchedulerVisitor class constructs the actors scheduler and it
 * transforms it to a task.
 * 
 * @author Endri Bezati
 * 
 */
public class DesignActorSchedulerVisitor extends DesignActorVisitor {

	/** The actors ports association with pinStatus Var **/
	private final Map<net.sf.orcc.df.Port, Var> pinStatusPort = new HashMap<net.sf.orcc.df.Port, Var>();

	/** All Return Vars given by the action scheduler **/
	private final Map<Action, Var> actionSchedulerReturnVar = new HashMap<Action, Var>();

	/** All inputPattern Var desicions **/
	private final Map<Action, Var> actionSchedulerInDecisions = new HashMap<Action, Var>();

	/** All outputPattern Var desicions **/
	private final Map<Action, Var> actionSchedulerOutDecisions = new HashMap<Action, Var>();

	/** The isSchedulable Test components **/
	private final Map<Action, List<Component>> isSchedulableComponents = new HashMap<Action, List<Component>>();

	/** The inputPattern Test components **/
	private final Map<Action, List<Component>> inputPatternComponents = new HashMap<Action, List<Component>>();

	/** The outputPattern Test components **/
	private final Map<Action, List<Component>> outputPatternComponents = new HashMap<Action, List<Component>>();

	/** All components of the scheduler **/
	private final List<Component> schedulerComponents = new ArrayList<Component>();

	/** Map of action associated to its Task **/
	private Map<Action, Task> actorsTasks = new HashMap<Action, Task>();

	public DesignActorSchedulerVisitor(Instance instance, Design design,
			Map<Action, Task> actorsTasks, ResourceCache resources) {
		super(instance, design, resources);
		this.actorsTasks = actorsTasks;
	}

	private Component createActionTest(List<Action> actions) {
		List<Action> newActionList = actions;
		Branch branch = null;
		for (Iterator<Action> it = actions.iterator(); it.hasNext();) {
			Action action = it.next();
			if (actionSchedulerInDecisions.containsKey(action)) {
				Var decisionVar = actionSchedulerInDecisions.get(action);
				String varName = "decision_isSchedulable_" + action.getName();
				Decision branchDecision = buildDecision(decisionVar, varName);

				// Create the "then" body
				Task actionToBeExecuted = actorsTasks.get(action);
				Var outDecision = actionSchedulerOutDecisions.get(action);
				Block thenBlock = (Block) buildTaskCall(actionToBeExecuted,
						outDecision);

				// Create the "else" body
				Block elseBlock = new Block(false);
				if (it.hasNext()) {
					newActionList.remove(action);
					createActionTest(newActionList);
				}

				// Create the branch
				if (it.hasNext()) {
					branch = new Branch(branchDecision, thenBlock, elseBlock);
				} else {
					branch = new Branch(branchDecision, thenBlock);
				}

			}
		}
		return branch;
	}

	/**
	 * Build the infinite loop decision for the actor scheduler
	 * 
	 * @return a decision component
	 */
	private Decision buildInfiniteLoopDecision() {
		Decision decision = null;
		// Create an ExprInt of "1" and assign it to the varActorSched
		Type type = IrFactory.eINSTANCE.createTypeInt(1);
		Var varActorSched = IrFactory.eINSTANCE.createVar(0, type,
				"var_actor_sched", false, 0);
		InstAssign assignSched = IrFactory.eINSTANCE.createInstAssign(
				varActorSched, IrFactory.eINSTANCE.createExprInt(1));
		// Visit assignSched
		doSwitch(assignSched);
		// Create the var_actor_loop variable and assign the var_actor_sched
		// into it
		Var varActorLoop = IrFactory.eINSTANCE.createVar(1, type,
				"var_actor_loop", false, 0);

		InstAssign assignLoop = IrFactory.eINSTANCE.createInstAssign(
				varActorLoop, IrFactory.eINSTANCE.createExprVar(varActorSched));
		doSwitch(assignLoop);

		currentModule = new Block(false);

		// Make an Exit with zero data buses at the output
		currentExit = currentModule.makeExit(0);

		// Add all components to the module
		populateModule(currentModule, currentListComponent);

		// Build Dependencies
		operationDependencies(currentListComponent, componentDependency,
				currentExit);

		// Create the decision
		decision = new Decision((Block) currentModule, currentComponent);
		// Any data inputs to the decision need to be propagated from
		// the block to the decision. There should be no output ports
		// to propagate. They are inferred true/false.
		propagateInputs(decision, (Block) currentModule);

		// Build option scope
		currentModule.specifySearchScope("schedulerLoopDecision");
		return decision;

	}

	private Block buildLoopBody() {
		Block bodyBlock = new Block(false);
		return bodyBlock;
	}

	private Component buildTaskCall(Task task, Var outDecision) {
		Branch branch = null;
		String varName = "decision_outputPattern_" + task.showIDLogical();
		Decision branchDecision = buildDecision(outDecision, varName);

		// Create the the TaskCall and add the Task
		TaskCall taskCall = new TaskCall();
		taskCall.setTarget(task);
		// mapOut Done Port
		Bus doneBus = taskCall.getExit(Exit.DONE).getDoneBus();
		portCache.putDoneBus(taskCall, doneBus);

		// Build then Block
		Block thenBlock = (Block) buildModule(
				Arrays.asList((Component) taskCall),
				Collections.<Var> emptyList(), Collections.<Var> emptyList(),
				"block");
		branch = new Branch(branchDecision, thenBlock);
		return branch;
	}

	@Override
	public Object caseAction(Action action) {
		currentAction = action;

		// Clean the current components
		currentListComponent = new ArrayList<Component>();

		// Build pinPeek
		doSwitch(action.getPeekPattern());

		// Get the action scheduler components
		doSwitch(action.getScheduler());

		// Save the isSchedulable expressions
		isSchedulableComponents.put(action, currentListComponent);

		// Build the inputPattern
		currentListComponent = new ArrayList<Component>();
		patternTest(action.getInputPattern(), "in");
		inputPatternComponents.put(action, currentListComponent);

		// Build the outputPattern
		currentListComponent = new ArrayList<Component>();
		patternTest(action.getOutputPattern(), "out");
		outputPatternComponents.put(action, currentListComponent);

		return null;
	}

	@Override
	public Object caseActor(Actor actor) {
		componentCounter = 0;
		currentListComponent = new ArrayList<Component>();

		// Build infinite loop decision
		Decision loopDecision = buildInfiniteLoopDecision();

		// Get the PinStatus
		currentListComponent = new ArrayList<Component>();
		getActorPinStatus(actor);
		// Visit only the actions
		schedulerComponents.addAll(currentListComponent);
		for (Action action : actor.getActions()) {
			doSwitch(action);
		}

		// Build loop body
		Block bodyBlock = buildLoopBody();
		LoopBody loopBody = new WhileBody(loopDecision, bodyBlock);

		// Add isSchedulableTest components
		for (Action action : actor.getActions()) {
			schedulerComponents.addAll(isSchedulableComponents.get(action));
		}

		// Add inputPatternTest components
		for (Action action : actor.getActions()) {
			schedulerComponents.addAll(inputPatternComponents.get(action));
		}

		// Add outputPatternTest components
		for (Action action : actor.getActions()) {
			schedulerComponents.addAll(outputPatternComponents.get(action));
		}

		// Create TestAction
		createActionTest(actor.getActions());

		return null;
	}

	@Override
	public Object caseInstReturn(InstReturn returnInstr) {
		if (returnInstr.getValue().isExprVar()) {
			Var returnVar = ((ExprVar) returnInstr.getValue()).getUse()
					.getVariable();
			actionSchedulerReturnVar.put(currentAction, returnVar);
		}
		return null;
	}

	@Override
	public Object casePattern(Pattern pattern) {
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
					InstAssign assign = EcoreHelper.getContainerOfType(varDef,
							InstAssign.class);
					doSwitch(assign);

					// Get the variable
					Var peekVar = load.getTarget().getVariable();

					// Create the pinPeek Component
					ActionIOHandler ioHandler = resources.getIOHandler(port);
					currentComponent = ioHandler.getTokenPeekAccess();
					setAttributes(
							"pinPeek" + port.getName() + "_"
									+ Integer.toString(componentCounter),
							currentComponent);
					// mapInPorts(varLitteralPeek);
					mapInPorts(new ArrayList<Var>(Arrays.asList(litteralVar)));
					mapOutPorts(peekVar);
					componentCounter++;
					IrUtil.delete(load);
				}
				EcoreUtil.delete(oldTarget);
			}
		}

		return null;
	}

	private void getActorPinStatus(Actor actor) {
		// Input pinStatus
		for (net.sf.orcc.df.Port port : actor.getInputs()) {
			if (!port.isNative()) {
				getPinStatus(port);
			}
		}
		// Output pinStatus
		for (net.sf.orcc.df.Port port : actor.getOutputs()) {
			if (!port.isNative()) {
				getPinStatus(port);
			}
		}
	}

	private void getPinStatus(net.sf.orcc.df.Port port) {
		ActionIOHandler ioHandler = resources.getIOHandler(port);
		currentComponent = ioHandler.getStatusAccess();
		setAttributes(
				"pinStatus_" + port.getName() + "_"
						+ Integer.toString(componentCounter), currentComponent);
		// Create a new variable for the pinStatus
		Type type = IrFactory.eINSTANCE.createTypeBool();
		Var pinStatusVar = IrFactory.eINSTANCE.createVar(0, type,
				port.getName() + "_pinStatus", false, 0);
		// Add the pinStatusVar to pinStatusPort
		pinStatusPort.put(port, pinStatusVar);

		for (Bus dataBus : currentComponent.getExit(Exit.DONE).getDataBuses()) {
			if (dataBus.getValue() == null) {
				dataBus.setSize(port.getType().getSizeInBits(), port.getType()
						.isInt() || port.getType().isBool());
			}
			portCache.putSource(pinStatusVar, dataBus);
		}
		mapOutPorts(pinStatusVar);
		currentListComponent.add(currentComponent);
		componentCounter++;
	}

	private void patternTest(Pattern pattern, String direction) {
		if (pattern != null) {
			List<Var> inputPattersVars = new ArrayList<Var>();
			for (net.sf.orcc.df.Port port : pattern.getPorts()) {
				if (pinStatusPort.containsKey(port)) {
					Var pinStatus = pinStatusPort.get(port);
					Type type = IrFactory.eINSTANCE.createTypeBool();
					Var patternPort = IrFactory.eINSTANCE.createVar(
							0,
							type,
							"direction" + "putPattern_"
									+ currentAction.getName() + "_"
									+ port.getName(), false, 0);
					InstAssign noop = IrFactory.eINSTANCE.createInstAssign(
							patternPort,
							IrFactory.eINSTANCE.createExprVar(pinStatus));
					doSwitch(noop);
					inputPattersVars.add(patternPort);
				}
				// Create the Go Decision
				// Put the return Var to the inputPattern if "in"
				if (direction.equals("in")) {
					inputPattersVars.add(actionSchedulerReturnVar
							.get(currentAction));
				}
				currentComponent = new And(inputPattersVars.size());
				mapInPorts(inputPattersVars);
				// Create Decision Var
				Type type = IrFactory.eINSTANCE.createTypeBool();
				Var decisionVar = IrFactory.eINSTANCE.createVar(0, type,
						"isSchedulable_" + currentAction.getName() + "_"
								+ direction + "_decision", false, 0);
				mapOutPorts(decisionVar);
				// Save the decision, used on transitions
				if (direction.equals("in")) {
					actionSchedulerInDecisions.put(currentAction, decisionVar);
				} else {
					actionSchedulerOutDecisions.put(currentAction, decisionVar);
				}
				currentListComponent.add(currentComponent);
			}

		} else {
			// Give True to the Decision
			Type type = IrFactory.eINSTANCE.createTypeBool();
			Var decisionVar = IrFactory.eINSTANCE.createVar(0, type,
					"isSchedulable_" + currentAction.getName() + "_decision",
					false, 0);
			InstAssign noop = IrFactory.eINSTANCE.createInstAssign(decisionVar,
					IrFactory.eINSTANCE.createExprBool(true));
			// Visit assignSched
			doSwitch(noop);
			if (direction.equals("in")) {
				actionSchedulerInDecisions.put(currentAction, decisionVar);
			} else {
				actionSchedulerOutDecisions.put(currentAction, decisionVar);
			}
		}
	}

}