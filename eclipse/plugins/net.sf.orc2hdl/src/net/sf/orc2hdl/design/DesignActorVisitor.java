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

import java.lang.reflect.Array;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sf.openforge.frontend.slim.builder.ActionIOHandler;
import net.sf.openforge.frontend.slim.builder.ActionIOHandler.FifoIOHandler;
import net.sf.openforge.frontend.slim.builder.ActionIOHandler.NativeIOHandler;
import net.sf.openforge.lim.And;
import net.sf.openforge.lim.Block;
import net.sf.openforge.lim.Bus;
import net.sf.openforge.lim.Call;
import net.sf.openforge.lim.ClockDependency;
import net.sf.openforge.lim.Component;
import net.sf.openforge.lim.ControlDependency;
import net.sf.openforge.lim.DataDependency;
import net.sf.openforge.lim.Dependency;
import net.sf.openforge.lim.Design;
import net.sf.openforge.lim.Entry;
import net.sf.openforge.lim.Exit;
import net.sf.openforge.lim.InBuf;
import net.sf.openforge.lim.Module;
import net.sf.openforge.lim.Or;
import net.sf.openforge.lim.OutBuf;
import net.sf.openforge.lim.Port;
import net.sf.openforge.lim.ResetDependency;
import net.sf.openforge.lim.Task;
import net.sf.openforge.lim.TaskCall;
import net.sf.openforge.lim.memory.AddressStridePolicy;
import net.sf.openforge.lim.memory.AddressableUnit;
import net.sf.openforge.lim.memory.Allocation;
import net.sf.openforge.lim.memory.LogicalMemory;
import net.sf.openforge.lim.memory.LogicalValue;
import net.sf.openforge.lim.memory.Record;
import net.sf.openforge.lim.memory.Scalar;
import net.sf.openforge.lim.op.AddOp;
import net.sf.openforge.lim.op.AndOp;
import net.sf.openforge.lim.op.ComplementOp;
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
import net.sf.openforge.lim.op.OrOp;
import net.sf.openforge.lim.op.RightShiftOp;
import net.sf.openforge.lim.op.SimpleConstant;
import net.sf.openforge.lim.op.SubtractOp;
import net.sf.openforge.lim.op.XorOp;
import net.sf.openforge.util.MathStuff;
import net.sf.openforge.util.naming.ID;
import net.sf.openforge.util.naming.IDSourceInfo;
import net.sf.orcc.df.Action;
import net.sf.orcc.df.Actor;
import net.sf.orcc.ir.BlockIf;
import net.sf.orcc.ir.BlockWhile;
import net.sf.orcc.ir.ExprBinary;
import net.sf.orcc.ir.ExprBool;
import net.sf.orcc.ir.ExprInt;
import net.sf.orcc.ir.ExprUnary;
import net.sf.orcc.ir.ExprVar;
import net.sf.orcc.ir.InstAssign;
import net.sf.orcc.ir.InstCall;
import net.sf.orcc.ir.InstLoad;
import net.sf.orcc.ir.OpBinary;
import net.sf.orcc.ir.OpUnary;
import net.sf.orcc.ir.Procedure;
import net.sf.orcc.ir.Type;
import net.sf.orcc.ir.TypeList;
import net.sf.orcc.ir.Var;
import net.sf.orcc.ir.util.AbstractActorVisitor;
import net.sf.orcc.ir.util.ValueUtil;

import org.eclipse.emf.common.util.EList;

/**
 * The DesignActorVisitor class transforms an Orcc Actor to an OpenForge Design
 * 
 * @author Endri Bezati
 * 
 */
public class DesignActorVisitor extends AbstractActorVisitor<Object> {

	/** List which associates each action with its components **/
	private Map<Action, List<Component>> actionComponents = new HashMap<Action, List<Component>>();

	/** Action component Counter **/
	protected Integer componentCounter;

	/** Dependency between Components and Vars **/
	protected Map<Component, Var> componentDependency = new HashMap<Component, Var>();

	/** Current visited action **/
	private Action currentAction = null;

	/** Current Component **/
	protected Component currentComponent = null;

	/** Current Exit **/
	protected Exit currentExit;

	/** Current List Component **/
	protected List<Component> currentListComponent;

	/** The current module which represents the Action **/
	protected Module currentModule;

	/** Design to be build **/
	protected Design design;

	/** Port Cache **/
	protected PortCache portCache = new PortCache();

	/** Design Resources **/
	protected ResourceCache resources;

	/** Design stateVars **/
	protected Map<LogicalValue, Var> stateVars;

	public DesignActorVisitor(Design design, ResourceCache resources) {
		super(true);
		this.design = design;
		this.resources = resources;
	}

	@Override
	public Object caseAction(Action action) {
		currentAction = action;
		componentCounter = 0;
		currentListComponent = new ArrayList<Component>();
		// Initialize currentModule and its exit Type
		currentModule = new Block(false);

		// Make Action module exit
		currentExit = currentModule.makeExit(0, Exit.RETURN);
		// Get pinRead Operation(s)

		for (net.sf.orcc.df.Port port : action.getInputPattern().getPorts()) {
			makePinReadOperation(port, portCache);
			currentListComponent.add(currentComponent);
		}

		// Visit the rest of the action
		super.doSwitch(action.getBody());

		// Get pinWrite Operation(s)
		for (net.sf.orcc.df.Port port : action.getOutputPattern().getPorts()) {
			makePinWriteOperation(port, portCache);
			currentListComponent.add(currentComponent);
		}
		// Create the task
		Task task = createTask(currentAction.getName());
		// Add it to the design
		design.addTask(task);

		// TODO: To be deleted
		actionComponents.put(currentAction, currentListComponent);
		return null;
	}

	@Override
	public Object caseActor(Actor actor) {
		// Get Actors Input(s) Port
		getActorsPorts(actor.getInputs(), "in", resources);
		// Get Actors Output(s) Port
		getActorsPorts(actor.getOutputs(), "out", resources);

		// TODO: Get the values of the parameters before visiting
		for (Var parameter : actor.getParameters()) {
			doSwitch(parameter);
		}

		stateVars = new HashMap<LogicalValue, Var>();
		// Visit stateVars
		for (Var stateVar : actor.getStateVars()) {
			doSwitch(stateVar);
		}

		// Allocate each LogicalValue (State Variable) in a memory
		// with a matching address stride. This provides consistency
		// in the memories and allows for state vars to be co-located
		// if area is of concern.
		Map<Integer, LogicalMemory> memories = new HashMap<Integer, LogicalMemory>();
		for (LogicalValue lvalue : stateVars.keySet()) {
			int stride = lvalue.getAddressStridePolicy().getStride();
			LogicalMemory mem = memories.get(stride);
			if (mem == null) {
				// 32 should be more than enough for max address
				// width
				mem = new LogicalMemory(32);
				mem.createLogicalMemoryPort();
				design.addMemory(mem);
			}
			// Create a 'location' for the stateVar that is
			// appropriate for its type/size.
			Allocation location = mem.allocate(lvalue);
			Var stateVar = stateVars.get(lvalue);
			setAttributes(stateVar, location);
			resources.addLocation(stateVar, location);
		}
		// TODO: Create Task for procedures
		for (Procedure procedure : actor.getProcs()) {
			doSwitch(procedure);
		}
		// Create a Task for each action in the actor
		for (Action action : actor.getActions()) {
			doSwitch(action);
		}
		// TODO: Do not know what to do for the moment with this one
		for (Action initialize : actor.getInitializes()) {
			doSwitch(initialize);
		}

		// Create a task for the scheduler and add it directly to the design
		DesignActorSchedulerVisitor schedulerVisitor = new DesignActorSchedulerVisitor(
				design, resources);
		schedulerVisitor.doSwitch(actor);

		return null;
	}

	@Override
	public Object caseExprBinary(ExprBinary expr) {
		if (expr.getOp() == OpBinary.BITAND) {
			currentComponent = new AndOp();
		} else if (expr.getOp() == OpBinary.BITOR) {
			currentComponent = new OrOp();
		} else if (expr.getOp() == OpBinary.BITXOR) {
			currentComponent = new XorOp();
		} else if (expr.getOp() == OpBinary.DIV) {
			currentComponent = new DivideOp(expr.getType().getSizeInBits());
		} else if (expr.getOp() == OpBinary.DIV_INT) {
			currentComponent = new DivideOp(expr.getType().getSizeInBits());
		} else if (expr.getOp() == OpBinary.EQ) {
			currentComponent = new EqualsOp();
		} else if (expr.getOp() == OpBinary.GE) {
			currentComponent = new GreaterThanEqualToOp();
		} else if (expr.getOp() == OpBinary.GT) {
			currentComponent = new GreaterThanOp();
		} else if (expr.getOp() == OpBinary.LE) {
			currentComponent = new LessThanEqualToOp();
		} else if (expr.getOp() == OpBinary.LOGIC_AND) {
			currentComponent = new And(expr.getType().getSizeInBits());
		} else if (expr.getOp() == OpBinary.LOGIC_OR) {
			currentComponent = new Or(expr.getType().getSizeInBits());
		} else if (expr.getOp() == OpBinary.LT) {
			currentComponent = new LessThanOp();
		} else if (expr.getOp() == OpBinary.MINUS) {
			currentComponent = new SubtractOp();
		} else if (expr.getOp() == OpBinary.MOD) {
			currentComponent = new ModuloOp();
		} else if (expr.getOp() == OpBinary.NE) {
			currentComponent = new NotEqualsOp();
		} else if (expr.getOp() == OpBinary.PLUS) {
			currentComponent = new AddOp();
		} else if (expr.getOp() == OpBinary.SHIFT_LEFT) {
			int log2N = MathStuff.log2(expr.getType().getSizeInBits());
			currentComponent = new LeftShiftOp(log2N);
		} else if (expr.getOp() == OpBinary.SHIFT_RIGHT) {
			int log2N = MathStuff.log2(expr.getType().getSizeInBits());
			currentComponent = new RightShiftOp(log2N);
		} else if (expr.getOp() == OpBinary.TIMES) {
			currentComponent = new MultiplyOp(expr.getType().getSizeInBits());
		}
		return null;
	}

	@Override
	public Object caseExprBool(ExprBool expr) {
		final long value = expr.isValue() ? 1 : 0;
		currentComponent = new SimpleConstant(value, 1, true);
		return null;
	}

	@Override
	public Object caseExprInt(ExprInt expr) {
		final long value = expr.getIntValue();
		currentComponent = new SimpleConstant(value, expr.getType()
				.getSizeInBits(), expr.getType().isInt());
		return null;
	}

	@Override
	public Object caseExprUnary(ExprUnary expr) {
		if (expr.getOp() == OpUnary.BITNOT) {
			currentComponent = new ComplementOp();
		} else if (expr.getOp() == OpUnary.LOGIC_NOT) {
			currentComponent = new NotOp();
		} else if (expr.getOp() == OpUnary.MINUS) {
			currentComponent = new MinusOp();
		}
		return null;
	}

	@Override
	public Object caseExprVar(ExprVar var) {
		// TODO: See if NoOP can have more than one inputs after all actors
		// Transformations
		currentComponent = new NoOp(1, Exit.DONE);
		mapInPorts(var.getUse().getVariable());
		return null;
	}

	@Override
	public Object caseInstAssign(InstAssign assign) {
		super.caseInstAssign(assign);
		if (currentComponent != null) {
			currentListComponent.add(currentComponent);
			mapOutPorts(assign.getTarget().getVariable());
		}
		return null;
	}

	@Override
	public Object caseInstCall(InstCall call) {
		currentComponent = new TaskCall();
		resources.addTaskCall(call, (TaskCall) currentComponent);
		return null;
	}

	@Override
	public Object caseInstLoad(InstLoad load) {
		if (load.getSource().getVariable().getType().isList()) {
			// TODO: Load index of the List
		} else {
			currentComponent = new NoOp(1, Exit.DONE);

		}
		return null;
	}

	@Override
	public Object caseNodeIf(BlockIf blockIf) {
		return null;
	}

	@Override
	public Object caseNodeWhile(BlockWhile blockWhile) {
		return null;
	}

	@Override
	public Object caseVar(Var var) {
		if (var.isGlobal()) {
			stateVars.put(makeLogicalValue(var), var);
		}
		return null;
	}

	protected void componentAddEntry(Component comp, Exit drivingExit,
			Bus clockBus, Bus resetBus, Bus goBus) {

		Entry entry = comp.makeEntry(drivingExit);
		// Even though most components do not use the clock, reset and
		// go ports we set up the dependencies for consistancy.
		entry.addDependency(comp.getClockPort(), new ClockDependency(clockBus));
		entry.addDependency(comp.getResetPort(), new ResetDependency(resetBus));
		entry.addDependency(comp.getGoPort(), new ControlDependency(goBus));
	}

	protected Call createCall(String name) {
		Block procedureBlock = (Block) currentModule;
		net.sf.openforge.lim.Procedure proc = new net.sf.openforge.lim.Procedure(
				procedureBlock);
		Call call = proc.makeCall();
		proc.setIDSourceInfo(deriveIDSourceInfo(name));

		for (Port blockPort : procedureBlock.getPorts()) {
			Port callPort = call.getPortFromProcedurePort(blockPort);
			portCache.replaceTarget(blockPort, callPort);
		}

		for (Exit exit : procedureBlock.getExits()) {
			for (Bus blockBus : exit.getBuses()) {
				Bus callBus = call.getBusFromProcedureBus(blockBus);
				portCache.replaceSource(blockBus, callBus);
			}
		}
		return call;
	}

	protected Task createTask(String name) {
		Task task = null;
		// Add all components to the module
		populateModule(currentModule, currentListComponent);

		// Build Dependencies
		operationDependencies();
		// Build option scope
		currentModule.specifySearchScope(name);

		// create Call
		Call call = createCall(name);
		topLevelInit(call);
		// Create task
		task = new Task(call);
		task.setKickerRequired(false);
		task.setSourceName(name);
		return task;
	}

	// TODO: set all the necessary information here
	protected IDSourceInfo deriveIDSourceInfo(String name) {
		String fileName = null;
		String packageName = null;
		String className = name;
		String methodName = name;
		String signature = null;
		int line = 0;
		int cpos = 0;
		return new IDSourceInfo(fileName, packageName, className, methodName,
				signature, line, cpos);
	}

	/**
	 * This method get the I/O ports of the actor and it adds in {@link Design}
	 * the actors ports
	 * 
	 * @param ports
	 *            the list of the Ports
	 * @param direction
	 *            the direction of the port, "in" for input / "out" for output
	 * @param resources
	 *            the cache resource
	 */
	private void getActorsPorts(EList<net.sf.orcc.df.Port> ports,
			String direction, ResourceCache resources) {
		for (net.sf.orcc.df.Port port : ports) {
			if (port.isNative()) {
				NativeIOHandler ioHandler = new ActionIOHandler.NativeIOHandler(
						direction, port.getName(), Integer.toString(port
								.getType().getSizeInBits()));
				ioHandler.build(design);
				resources.addIOHandler(port, ioHandler);
			} else {
				FifoIOHandler ioHandler = new ActionIOHandler.FifoIOHandler(
						direction, port.getName(), Integer.toString(port
								.getType().getSizeInBits()));
				ioHandler.build(design);
				resources.addIOHandler(port, ioHandler);
			}
		}
	}

	/**
	 * Constructs a LogicalValue from a String value given its type
	 * 
	 * @param stringValue
	 *            the numerical value
	 * @param type
	 *            the type of the numerical value
	 * @return
	 */
	private LogicalValue makeLogicalValue(String stringValue, Type type) {
		LogicalValue logicalValue = null;
		final BigInteger value;
		Integer bitSize = type.getSizeInBits();
		if (stringValue.trim().toUpperCase().startsWith("0X")) {
			value = new BigInteger(stringValue.trim().substring(2), 16);
		} else {
			value = new BigInteger(stringValue);
		}
		AddressStridePolicy addrPolicy = new AddressStridePolicy(bitSize);
		logicalValue = new Scalar(new AddressableUnit(value), addrPolicy);
		return logicalValue;
	}

	/**
	 * Constructs a LogicalValue from a Variable
	 * 
	 * @param var
	 *            the variable
	 * @return
	 */
	private LogicalValue makeLogicalValue(Var var) {
		LogicalValue logicalValue = null;
		if (var.getType().isList()) {

			TypeList typeList = (TypeList) var.getType();
			Type type = typeList.getInnermostType();

			List<Integer> listDimension = typeList.getDimensions();

			Object varValue = var.getValue();
			logicalValue = makeLogicalValueObject(varValue, listDimension, type);
		} else {
			Type type = var.getType();
			if (var.isInitialized()) {
				String valueString = Integer.toString(((ExprInt) var
						.getInitialValue()).getIntValue());
				logicalValue = makeLogicalValue(valueString, type);
			} else {
				logicalValue = makeLogicalValue("0", type);
			}
		}

		return logicalValue;
	}

	/**
	 * Constructs a LogicalValue from a uni or multi-dim Object Value
	 * 
	 * @param obj
	 *            the object value
	 * @param dimension
	 *            the dimension of the object value
	 * @param type
	 *            the type of the object value
	 * @return
	 */
	private LogicalValue makeLogicalValueObject(Object obj,
			List<Integer> dimension, Type type) {
		LogicalValue logicalValue = null;

		if (dimension.size() > 1) {
			List<LogicalValue> subElements = new ArrayList<LogicalValue>(
					dimension.get(0));
			List<Integer> newListDimension = dimension;
			newListDimension.remove(0);
			for (int i = 0; i < dimension.get(0); i++) {
				subElements.add(makeLogicalValueObject(Array.get(obj, i),
						newListDimension, type));
			}

			logicalValue = new Record(subElements);
		} else {
			if (dimension.get(0).equals(1)) {
				BigInteger value = (BigInteger) ValueUtil.get(type, obj, 0);
				String valueString = value.toString();
				logicalValue = makeLogicalValue(valueString, type);
			} else {
				List<LogicalValue> subElements = new ArrayList<LogicalValue>(
						dimension.get(0));
				for (int i = 0; i < dimension.get(0); i++) {
					BigInteger value = (BigInteger) ValueUtil.get(type, obj, i);
					String valueString = value.toString();
					subElements.add(makeLogicalValue(valueString, type));
				}
				logicalValue = new Record(subElements);
			}

		}

		return logicalValue;
	}

	private void makePinReadOperation(net.sf.orcc.df.Port port,
			PortCache portCache) {
		ActionIOHandler ioHandler = resources.getIOHandler(port);
		currentComponent = ioHandler.getReadAccess();
		setAttributes(
				"pinRead_" + port.getName() + "_"
						+ Integer.toString(componentCounter), currentComponent);
		Var pinReadVar = currentAction.getInputPattern().getPortToVarMap()
				.get(port);

		for (Bus dataBus : currentComponent.getExit(Exit.DONE).getDataBuses()) {
			if (dataBus.getValue() == null) {
				dataBus.setSize(port.getType().getSizeInBits(), port.getType()
						.isInt() || port.getType().isBool());
			}
			portCache.putSource(pinReadVar, dataBus);
		}
		mapOutPorts(pinReadVar);
		componentCounter++;
	}

	private void makePinWriteOperation(net.sf.orcc.df.Port port,
			PortCache portCache) {
		ActionIOHandler ioHandler = resources.getIOHandler(port);
		currentComponent = ioHandler.getWriteAccess();
		setAttributes(
				"pinWrite_" + port.getName() + "_"
						+ Integer.toString(componentCounter), currentComponent);
		Var pinWriteVar = currentAction.getOutputPattern().getPortToVarMap()
				.get(port);

		mapInPorts(pinWriteVar);
		// Add done dependency for this operation to the current module exit
		Bus doneBus = currentComponent.getExit(Exit.DONE).getDoneBus();
		portCache.putDoneBus(currentComponent, doneBus);
		componentCounter++;
	}

	protected void mapInPorts(Var var) {
		Port dataPort = currentComponent.getDataPorts().get(0);
		dataPort.setSize(var.getType().getSizeInBits(), var.getType().isInt()
				|| var.getType().isBool());
		portCache.putTarget(var, dataPort);
		// Put Input dependency
		componentDependency.put(currentComponent, var);
	}

	protected void mapOutPorts(Var var) {
		Bus dataBus = currentComponent.getExit(Exit.DONE).getDataBuses().get(0);
		if (dataBus.getValue() == null) {
			dataBus.setSize(var.getType().getSizeInBits(), var.getType()
					.isInt() || var.getType().isBool());
		}
		portCache.putSource(var, dataBus);

		Bus doneBus = currentComponent.getExit(Exit.DONE).getDoneBus();
		portCache.putDoneBus(currentComponent, doneBus);
	}

	protected void operationDependencies() {
		// Build Data Dependencies
		for (Component component : currentListComponent) {
			Var depVar = componentDependency.get(component);
			if (depVar != null) {
				Bus sourceBus = portCache.getSource(depVar);
				Port targetPort = portCache.getTarget(depVar);
				List<Entry> entries = targetPort.getOwner().getEntries();
				Entry entry = entries.get(0);
				Dependency dep = new DataDependency(sourceBus);
				entry.addDependency(targetPort, dep);
			}
		}
		// Build control Dependencies
		for (Component component : currentListComponent) {
			Bus busDone = portCache.getDoneBus(component);
			if (busDone != null) {
				Port actionDonePort = currentExit.getDoneBus().getPeer();
				List<Entry> entries = actionDonePort.getOwner().getEntries();
				Entry entry = entries.get(0);
				Dependency dep = new ControlDependency(busDone);
				entry.addDependency(actionDonePort, dep);
			}
		}

	}

	/**
	 * Takes care of the busy work of putting the components into the module (in
	 * order) and ensuring appropriate clock, reset, and go dependencies (which
	 * ALL components must have)
	 * 
	 * @param components
	 *            a List of {@link Component} objects
	 */
	protected void populateModule(Module module, List<Component> components) {
		final InBuf inBuf = module.getInBuf();
		final Bus clockBus = inBuf.getClockBus();
		final Bus resetBus = inBuf.getResetBus();
		final Bus goBus = inBuf.getGoBus();

		// I believe that the drivingExit no longer relevant
		Exit drivingExit = inBuf.getExit(Exit.DONE);

		int index = 0;
		for (Component comp : components) {
			if (module instanceof Block)
				((Block) module).insertComponent(comp, index++);
			else
				module.addComponent(comp);

			componentAddEntry(comp, drivingExit, clockBus, resetBus, goBus);

			drivingExit = comp.getExit(Exit.DONE);
		}

		// Ensure that the outbufs of the module have an entry
		for (OutBuf outbuf : module.getOutBufs()) {
			componentAddEntry(outbuf, drivingExit, clockBus, resetBus, goBus);
		}
	}

	protected void setAttributes(String tag, Component comp) {
		setAttributes(tag, comp, false);
	}

	protected void setAttributes(String tag, Component comp, Boolean Removable) {
		comp.setSourceName(tag);
		if (!Removable)
			comp.setNonRemovable();
	}

	/**
	 * Set the name of an LIM component by the name of an Orcc variable
	 * 
	 * @param var
	 *            a Orcc IR variable element
	 * @param comp
	 *            a LIM ID component
	 */
	protected void setAttributes(Var var, ID comp) {
		comp.setSourceName(var.getName());
	}

	private void topLevelInit(Call call) {
		call.getClockPort().setSize(1, false);
		call.getResetPort().setSize(1, false);
		call.getGoPort().setSize(1, false);
	}

}
