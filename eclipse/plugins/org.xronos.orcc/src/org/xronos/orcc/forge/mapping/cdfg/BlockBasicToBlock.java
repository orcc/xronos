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
 * If you modify this Program, or any covered work, by linking or combining it
 * with Eclipse (or a modified version of Eclipse or an Eclipse plugin or 
 * an Eclipse library), containing parts covered by the terms of the 
 * Eclipse Public License (EPL), the licensors of this Program grant you 
 * additional permission to convey the resulting work.  Corresponding Source 
 * for a non-source form of such a combination shall include the source code 
 * for the parts of Eclipse libraries used as well as that of the  covered work.
 * 
 */

package org.xronos.orcc.forge.mapping.cdfg;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import net.sf.orcc.backends.ir.InstCast;
import net.sf.orcc.df.Action;
import net.sf.orcc.df.impl.PatternImpl;
import net.sf.orcc.ir.Arg;
import net.sf.orcc.ir.ArgByRef;
import net.sf.orcc.ir.ArgByVal;
import net.sf.orcc.ir.BlockBasic;
import net.sf.orcc.ir.Def;
import net.sf.orcc.ir.ExprVar;
import net.sf.orcc.ir.Expression;
import net.sf.orcc.ir.InstAssign;
import net.sf.orcc.ir.InstCall;
import net.sf.orcc.ir.InstLoad;
import net.sf.orcc.ir.InstReturn;
import net.sf.orcc.ir.InstStore;
import net.sf.orcc.ir.Instruction;
import net.sf.orcc.ir.IrFactory;
import net.sf.orcc.ir.Param;
import net.sf.orcc.ir.Procedure;
import net.sf.orcc.ir.Type;
import net.sf.orcc.ir.TypeList;
import net.sf.orcc.ir.Use;
import net.sf.orcc.ir.Var;
import net.sf.orcc.ir.util.AbstractIrVisitor;
import net.sf.orcc.ir.util.IrUtil;
import net.sf.orcc.util.util.EcoreHelper;

import org.eclipse.emf.ecore.EObject;
import org.xronos.openforge.frontend.slim.builder.ActionIOHandler;
import org.xronos.openforge.lim.Block;
import org.xronos.openforge.lim.Bus;
import org.xronos.openforge.lim.Component;
import org.xronos.openforge.lim.ControlDependency;
import org.xronos.openforge.lim.DataDependency;
import org.xronos.openforge.lim.Exit;
import org.xronos.openforge.lim.HeapRead;
import org.xronos.openforge.lim.HeapWrite;
import org.xronos.openforge.lim.Port;
import org.xronos.openforge.lim.Task;
import org.xronos.openforge.lim.TaskCall;
import org.xronos.openforge.lim.memory.AbsoluteMemoryRead;
import org.xronos.openforge.lim.memory.AbsoluteMemoryWrite;
import org.xronos.openforge.lim.memory.AddressStridePolicy;
import org.xronos.openforge.lim.memory.LValue;
import org.xronos.openforge.lim.memory.Location;
import org.xronos.openforge.lim.memory.LocationConstant;
import org.xronos.openforge.lim.memory.LogicalMemoryPort;
import org.xronos.openforge.lim.op.AddOp;
import org.xronos.openforge.lim.op.CastOp;
import org.xronos.openforge.lim.op.NoOp;
import org.xronos.openforge.util.Debug;
import org.xronos.openforge.util.naming.IDSourceInfo;
import org.xronos.orcc.ir.InstPortRead;
import org.xronos.orcc.ir.InstPortStatus;
import org.xronos.orcc.ir.InstPortWrite;
import org.xronos.orcc.preference.Constants;

/**
 * This Visitor transforms a BlockBasic to a LIM Block, IndexFlattener should be
 * applied before creating the LIM Block
 * 
 * @author Endri Bezati
 * 
 */
public class BlockBasicToBlock extends AbstractIrVisitor<Component> {

	/**
	 * The current Block (Module) being created
	 */
	Block currentBlock;

	/**
	 * Set of Block inputs
	 */
	Map<Var, Port> inputs;

	/**
	 * The port variable for each dependency
	 */
	Map<Port, Var> portDependecies;

	/**
	 * The Bus variable for each dependency
	 */
	Map<Bus, Var> busDependecies;

	/**
	 * Set og Block outputs
	 */
	Map<Var, Bus> outputs;

	Component returnComponent;

	/**
	 * This inner class replaces the local list variables with a referenced one
	 * 
	 * @author Endri Bezati
	 * 
	 */
	public class PropagateReferences extends AbstractIrVisitor<Void> {
		Map<Var, Var> paramVarToRefVar;

		public PropagateReferences(Map<Var, Var> paramVarToRefVar) {
			super(true);
			this.paramVarToRefVar = paramVarToRefVar;
		}

		@Override
		public Void caseInstLoad(InstLoad load) {
			Var source = load.getSource().getVariable();
			if (paramVarToRefVar.containsKey(source)) {
				Use newUse = IrFactory.eINSTANCE.createUse(paramVarToRefVar
						.get(source));
				load.setSource(newUse);
			}
			return null;
		}

		@Override
		public Void caseInstStore(InstStore store) {
			Var target = store.getTarget().getVariable();
			if (paramVarToRefVar.containsKey(target)) {
				Def newDef = IrFactory.eINSTANCE.createDef(paramVarToRefVar
						.get(target));
				store.setTarget(newDef);
			}
			return null;
		}

	}

	public class PropagateReturnTarget extends AbstractIrVisitor<Void> {

		Var target;

		public PropagateReturnTarget(Var target) {
			this.target = target;
		}

		@Override
		public Void caseInstReturn(InstReturn returnInstr) {
			returnInstr.setAttribute("returnTarget", target);
			return null;
		}

	}

	public BlockBasicToBlock() {
		super(true);
		// Initialize members
		inputs = new HashMap<Var, Port>();
		outputs = new HashMap<Var, Bus>();
		portDependecies = new HashMap<Port, Var>();
		busDependecies = new HashMap<Bus, Var>();
	}

	@Override
	public Component caseBlockBasic(BlockBasic block) {
		Component component = super.caseBlockBasic(block);
		block.setAttribute("inputs", inputs);
		block.setAttribute("outputs", outputs);
		return component;
	}

	@Override
	public Component caseInstAssign(InstAssign assign) {
		Var target = assign.getTarget().getVariable();
		Type targetType = target.getType();
		Expression expr = assign.getValue();

		if (!expr.isExprVar()) {
			Type exprType = expr.getType();
			Component comp = null;

			if (exprType.getSizeInBits() != targetType.getSizeInBits()) {
				Component exprComp = new ExprToComponent().doSwitch(expr);
				CastOp castOp = new CastOp(targetType.getSizeInBits(),
						targetType.isInt());

				comp = new Block(Arrays.asList(exprComp, castOp));

				// Now deal with dependencies
				Bus compResultBus = exprComp.getExit(Exit.DONE).getDataBuses()
						.get(0);
				Port castDataPort = castOp.getDataPort();
				ComponentUtil.connectDataDependency(compResultBus,
						castDataPort, 0);

				// Create a block data bus
				Bus resultBus = comp.getExit(Exit.DONE).makeDataBus(
						targetType.getSizeInBits(), targetType.isInt());
				Port reslutPort = resultBus.getPeer();
				Bus castResultBus = castOp.getResultBus();
				ComponentUtil.connectDataDependency(castResultBus, reslutPort,
						0);

				@SuppressWarnings("unchecked")
				Map<Var, Port> exprInput = (Map<Var, Port>) expr.getAttribute(
						"inputs").getObjectValue();
				for (Var var : exprInput.keySet()) {
					Type type = var.getType();
					Port blkDataPort = comp.makeDataPort(var.getName(),
							type.getSizeInBits(), type.isInt());
					Bus blkDataPortPeer = blkDataPort.getPeer();

					// Connect it to this port
					Port exprDataPort = exprInput.get(var);
					ComponentUtil.connectDataDependency(blkDataPortPeer,
							exprDataPort, 0);

					// Add to port dependencies
					portDependecies.put(blkDataPort, var);
				}

			} else {
				comp = new ExprToComponent().doSwitch(expr);
				@SuppressWarnings("unchecked")
				Map<Var, Port> exprInput = (Map<Var, Port>) expr.getAttribute(
						"inputs").getObjectValue();
				for (Var var : exprInput.keySet()) {
					Port dataPort = exprInput.get(var);
					portDependecies.put(dataPort, var);
				}
			}

			Exit compExit = comp.getExit(Exit.DONE);
			Bus resultBus = compExit.getDataBuses().get(0);
			resultBus.setIDLogical(target.getName());

			// Bus dependencies
			busDependecies.put(resultBus, target);
			return comp;
		} else {
			Var source = ((ExprVar) expr).getUse().getVariable();
			Type sourceType = source.getType();
			Component comp = null;
			if (targetType.getSizeInBits() != sourceType.getSizeInBits()) {
				List<Component> sequence = new ArrayList<Component>();
				NoOp noop = new NoOp(1, Exit.DONE);
				CastOp castOp = new CastOp(targetType.getSizeInBits(),
						targetType.isInt());
				sequence.add(noop);
				sequence.add(castOp);
				Block block = new Block(sequence);

				// Resolve Dependencies
				// -- Input

				Type type = source.getType();
				Port dataPort = block.makeDataPort(source.getName(),
						type.getSizeInBits(), type.isInt());

				// Port Dependencies
				portDependecies.put(dataPort, source);

				Bus dataBus = dataPort.getPeer();

				// Between Block input data port and noop data Port
				Port noopDataPort = noop.getDataPorts().get(0);
				ComponentUtil.connectDataDependency(dataBus, noopDataPort, 0);
				addToInputs(source, dataPort);

				// -- Between NoOp and CastOp
				Bus noopDataBus = noop.getResultBus();
				Port castDataPort = castOp.getDataPort();
				ComponentUtil.connectDataDependency(noopDataBus, castDataPort,
						0);

				// -- Between CastOp and Output DataBus
				Bus castDatBus = castOp.getResultBus();
				Bus blockDataBus = block.getExit(Exit.DONE).makeDataBus(
						target.getName(), targetType.getSizeInBits(),
						targetType.isInt());
				Port blockDataBusPeer = blockDataBus.getPeer();
				ComponentUtil.connectDataDependency(castDatBus,
						blockDataBusPeer, 0);
				// addToLastDefined(target, blockDataBus);
				busDependecies.put(blockDataBus, target);
				comp = block;
			} else {
				// Dependencies, put port and bus dependencies on source and
				// target
				NoOp noop = new NoOp(1, Exit.DONE);
				Port noopDatPort = noop.getDataPorts().get(0);
				// Port Dependencies
				portDependecies.put(noopDatPort, source);

				// -- Output
				Bus noopResultBus = noop.getResultBus();
				noopResultBus.setIDLogical(target.getName());
				busDependecies.put(noopResultBus, target);

				comp = noop;
			}

			return comp;
		}
	}

	@Override
	public Component caseInstCall(InstCall call) {
		Component component = null;
		// Test if the call is coming from an action or the XronosScheduler
		Action action = EcoreHelper.getContainerOfType(this.procedure,
				Action.class);
		if (action == null) {
			List<Component> sequence = new ArrayList<Component>();
			// Construct the Block of the Procedure

			Map<Var, Var> byRefVar = new HashMap<Var, Var>();
			Map<Var, Component> byValComponent = new HashMap<Var, Component>();
			Map<Component, CastOp> byValComponentCast = new HashMap<Component, CastOp>();
			Map<Var, Expression> byValExpression = new HashMap<Var, Expression>();
			Map<Bus, Var> byValBusVar = new HashMap<Bus, Var>();

			// Clone Procedure
			Procedure proc = IrUtil.copy(call.getProcedure());

			int nbArg = 0;
			// Propagate References or create the necessary argument components
			for (Arg arg : call.getArguments()) {
				Param param = proc.getParameters().get(nbArg);
				if (arg.isByRef() || param.getVariable().getType().isList()) {
					Var refVar = null;
					// ORCC Workaround to support by reference
					if (arg.isByRef()) {
						refVar = ((ArgByRef) arg).getUse().getVariable();
					} else {
						Expression exprArg = ((ArgByVal) arg).getValue();
						if (exprArg.isExprVar()) {
							refVar = ((ExprVar) exprArg).getUse().getVariable();
						} else if (exprArg.isExprList()) {
							// Already resolved from Frontend
							throw (new NullPointerException(
									"ExprArg is ExprList"));
						}
					}
					byRefVar.put(param.getVariable(), refVar);
				} else {
					Expression exprArg = ((ArgByVal) arg).getValue();
					Component argComponent = new ExprToComponent()
							.doSwitch(exprArg);
					Var paramVar = param.getVariable();
					Type type = paramVar.getType();
					Bus resultBus = argComponent.getExit(Exit.DONE)
							.getDataBuses().get(0);

					Type typeExpr = exprArg.getType();
					// Add casting if necessary
					if (type.getSizeInBits() != typeExpr.getSizeInBits()) {
						CastOp cast = new CastOp(type.getSizeInBits(),
								type.isInt());
						sequence.add(cast);
						byValComponentCast.put(argComponent, cast);
					}

					byValBusVar.put(resultBus, paramVar);
					// For the components to be included on a new block
					sequence.add(argComponent);
					byValComponent.put(paramVar, argComponent);
					byValExpression.put(paramVar, exprArg);
				}
			}

			// Create call Block
			Block callBlock = null;

			// Propagate all reference if any
			if (!byRefVar.isEmpty()) {
				new PropagateReferences(byRefVar).doSwitch(proc);
			}

			if (call.getTarget() != null) {
				// Call is a CAL function
				Var target = call.getTarget().getVariable();
				new PropagateReturnTarget(target).doSwitch(proc);
				callBlock = new ProcedureToBlock(target).doSwitch(proc);
				sequence.add(callBlock);
			} else {
				// Call is a procedure
				callBlock = new ProcedureToBlock(false).doSwitch(proc);
				sequence.add(callBlock);
			}

			// -- Single Data Bus on component Exit
			CastOp castOp = null;
			if (!proc.getReturnType().isVoid()) {
				Var target = call.getTarget().getVariable();
				Type type = target.getType();
				if (type.getSizeInBits() != proc.getReturnType()
						.getSizeInBits()) {
					castOp = new CastOp(type.getSizeInBits(), type.isInt());
					sequence.add(castOp);
				}
			}

			component = new Block(sequence);
			Map<Var, Port> blkDataports = new HashMap<Var, Port>();

			// Dependencies
			nbArg = 0;
			for (Param param : proc.getParameters()) {
				Var paramVar = param.getVariable();

				if (byValComponent.containsKey(paramVar)) {
					// --Data Port dependency from arguments
					Expression expr = byValExpression.get(paramVar);
					@SuppressWarnings("unchecked")
					Map<Var, Port> exprInputs = (Map<Var, Port>) expr
							.getAttribute("inputs").getObjectValue();
					for (Var var : exprInputs.keySet()) {
						Type type = var.getType();
						if (blkDataports.containsKey(var)) {
							Port dataPort = blkDataports.get(var);

							Bus dataPortPeer = dataPort.getPeer();
							ComponentUtil.connectDataDependency(dataPortPeer,
									exprInputs.get(var), 0);
						} else {
							Port dataPort = component.makeDataPort(
									var.getName(), type.getSizeInBits(),
									type.isInt());
							Bus dataPortPeer = dataPort.getPeer();
							ComponentUtil.connectDataDependency(dataPortPeer,
									exprInputs.get(var), 0);
							// Save DataPort
							blkDataports.put(var, dataPort);
							portDependecies.put(dataPort, var);
						}
					}

					// -- Argument resultBus --> call Block data port
					Component valComponent = byValComponent.get(paramVar);
					Bus resultBus = valComponent.getExit(Exit.DONE)
							.getDataBuses().get(0);
					if (byValComponentCast.containsKey(valComponent)) {
						CastOp cast = byValComponentCast.get(valComponent);
						// Dependency
						ComponentUtil.connectDataDependency(resultBus,
								cast.getDataPort(), 0);
						Var var = byValBusVar.get(resultBus);
						resultBus = cast.getResultBus();
						// Update the resultBus with the one of the cast
						byValBusVar.put(resultBus, var);
					}

					Var resultVar = byValBusVar.get(resultBus);
					@SuppressWarnings("unchecked")
					Map<Var, Port> procInputs = (Map<Var, Port>) proc
							.getAttribute("inputs").getObjectValue();
					// Connect it if same resultVar
					if (procInputs.containsKey(resultVar)) {
						Port dataPort = procInputs.get(resultVar);
						ComponentUtil.connectDataDependency(resultBus,
								dataPort, 0);
					}
				}

			}

			// -- Single Data Bus on component Exit
			if (!proc.getReturnType().isVoid()) {
				Var target = call.getTarget().getVariable();
				Type type = target.getType();
				Bus blockResultBus = component.getExit(Exit.DONE).makeDataBus(
						target.getName(), type.getSizeInBits(), type.isInt());
				Port blockResultBusPeer = blockResultBus.getPeer();

				Bus resultBus = null;
				// -- Connect with Cast if any
				if (castOp != null) {
					Bus callBlockResultBus = callBlock.getExit(Exit.DONE)
							.getDataBuses().get(0);
					ComponentUtil.connectDataDependency(callBlockResultBus,
							castOp.getDataPort(), 0);
					resultBus = castOp.getResultBus();
				} else {
					resultBus = callBlock.getExit(Exit.DONE).getDataBuses()
							.get(0);
				}

				ComponentUtil.connectDataDependency(resultBus,
						blockResultBusPeer, 0);
				busDependecies.put(blockResultBus, target);
			}

		} else {
			// Construct a LIM Call
			TaskCall taskCall = new TaskCall();
			Task task = (Task) action.getAttribute("task").getObjectValue();
			taskCall.setTarget(task);
			taskCall.setIDSourceInfo(new IDSourceInfo(action.getName(), call
					.getLineNumber()));
			component = taskCall;
		}
		Debug.wireGraphTo(component, "call", "/tmp/call.dot");
		return component;
	}

	public Component caseInstCast(InstCast cast) {
		Var target = cast.getTarget().getVariable();
		Var source = cast.getSource().getVariable();
		Integer castedSize = target.getType().getSizeInBits();

		CastOp castOp = new CastOp(castedSize, target.getType().isInt());
		Port dataPort = castOp.getDataPort();
		dataPort.setIDLogical(source.getName());

		portDependecies.put(dataPort, source);

		// Name CastOp I/O
		Bus resultBus = castOp.getResultBus();
		resultBus.setIDLogical(target.getName());

		busDependecies.put(resultBus, target);
		return castOp;
	}

	@Override
	public Component caseInstLoad(InstLoad load) {
		Var target = load.getTarget().getVariable();
		Var source = load.getSource().getVariable();

		// Check if it is scaler
		if (!source.getType().isList()) {
			boolean isSigned = source.getType().isInt();
			Location location = null;
			PatternImpl pattern = EcoreHelper.getContainerOfType(source,
					PatternImpl.class);
			if (pattern != null) {
				net.sf.orcc.df.Port dfPort = pattern.getVarToPortMap().get(
						source);
				location = (Location) dfPort.getAttribute("location")
						.getObjectValue();
			} else {
				location = (Location) source.getAttribute("location")
						.getObjectValue();
			}
			LogicalMemoryPort memPort = location.getLogicalMemory()
					.getLogicalMemoryPorts().iterator().next();
			Component absMemRead = new AbsoluteMemoryRead(location,
					Constants.MAX_ADDR_WIDTH, isSigned);
			memPort.addAccess((LValue) absMemRead, location);

			Exit exit = absMemRead.getExit(Exit.DONE);
			Bus resultBus = exit.getDataBuses().get(0);
			busDependecies.put(resultBus, target);
			return absMemRead;
		} else {
			// Index Flattener has flatten all indexes only one expression
			// possible
			List<Component> sequence = new ArrayList<Component>();

			Expression index = load.getIndexes().get(0);
			Component indexComp = new ExprToComponent().doSwitch(index);
			sequence.add(indexComp);

			// Source is a List
			// Get Type
			TypeList typeList = (TypeList) source.getType();
			Type innerType = typeList.getInnermostType();
			int dataSize = innerType.getSizeInBits();
			boolean isSigned = innerType.isInt();

			// Target Type
			Type targetType = target.getType();

			Location location = null;
			PatternImpl pattern = EcoreHelper.getContainerOfType(source,
					PatternImpl.class);
			if (pattern != null) {
				net.sf.orcc.df.Port dfPort = pattern.getVarToPortMap().get(
						source);
				location = (Location) dfPort.getAttribute("location")
						.getObjectValue();
			} else {
				location = (Location) source.getAttribute("location")
						.getObjectValue();
			}
			LogicalMemoryPort memPort = location.getLogicalMemory()
					.getLogicalMemoryPorts().iterator().next();

			AddressStridePolicy addrPolicy = location.getAbsoluteBase()
					.getInitialValue().getAddressStridePolicy();

			HeapRead read = new HeapRead(dataSize / addrPolicy.getStride(), 32,
					0, isSigned, addrPolicy);
			sequence.add(read);
			LocationConstant locationConst = new LocationConstant(location, 32,
					location.getAbsoluteBase().getLogicalMemory()
							.getAddressStridePolicy());
			sequence.add(locationConst);

			CastOp cast = new CastOp(32, false);
			sequence.add(cast);

			AddOp adder = new AddOp();
			sequence.add(adder);

			CastOp castResult = new CastOp(targetType.getSizeInBits(),
					targetType.isInt());
			sequence.add(castResult);

			// Build read Block
			Block loadBlock = new Block(sequence);

			// build loadBlock dependencies

			Bus indexCompDataBus = indexComp.getExit(Exit.DONE).getDataBuses()
					.get(0);

			cast.getEntries()
					.get(0)
					.addDependency(cast.getDataPort(),
							new DataDependency(indexCompDataBus));

			// TODO: use connectDataDependency
			// ComponentUtil.connectDataDependency(indexDataPort.getPeer(),cast.getDataPort(),0);

			adder.getEntries()
					.get(0)
					.addDependency(adder.getLeftDataPort(),
							new DataDependency(locationConst.getValueBus()));
			adder.getEntries()
					.get(0)
					.addDependency(adder.getRightDataPort(),
							new DataDependency(cast.getResultBus()));

			read.getEntries()
					.get(0)
					.addDependency(read.getBaseAddressPort(),
							new DataDependency(adder.getResultBus()));

			Exit done = loadBlock.getExit(Exit.DONE);

			done.getPeer()
					.getEntries()
					.get(0)
					.addDependency(
							done.getDoneBus().getPeer(),
							new ControlDependency(read.getExit(Exit.DONE)
									.getDoneBus()));

			// Build loadBlock input dependencies
			@SuppressWarnings("unchecked")
			Map<Var, Port> exprInput = (Map<Var, Port>) index.getAttribute(
					"inputs").getObjectValue();

			Map<Var, Port> blkDataPorts = new HashMap<Var, Port>();
			for (Var var : exprInput.keySet()) {
				Port dataPort = exprInput.get(var);
				Bus dataPortpeer = dataPort.getPeer();

				if (blkDataPorts.containsKey(var)) {
					Port blkDataPort = blkDataPorts.get(var);
					ComponentUtil.connectDataDependency(dataPortpeer,
							blkDataPort, 0);
				} else {
					Type type = var.getType();
					Port blkDataPort = loadBlock
							.makeDataPort(var.getName(), type.getSizeInBits(),
									type.isInt() || type.isBool());
					ComponentUtil.connectDataDependency(dataPortpeer,
							blkDataPort, 0);
					blkDataPorts.put(var, blkDataPort);
					portDependecies.put(blkDataPort, var);
				}
			}

			// Build loadBlock output dependencies
			Bus resultBus = loadBlock.getExit(Exit.DONE).makeDataBus(
					target.getName(), targetType.getSizeInBits(),
					targetType.isInt());
			resultBus.setIDLogical(target.getName());
			castResult
					.getEntries()
					.get(0)
					.addDependency(castResult.getDataPort(),
							new DataDependency(read.getResultBus()));
			resultBus
					.getPeer()
					.getOwner()
					.getEntries()
					.get(0)
					.addDependency(resultBus.getPeer(),
							new DataDependency(castResult.getResultBus()));

			memPort.addAccess(read, location);

			// Add to bus dependencies
			busDependecies.put(resultBus, target);

			return loadBlock;
		}
	}

	public Component caseInstPortRead(InstPortRead instPortRead) {
		// TODO: Add cast if necessary, even thought impossible
		Var target = instPortRead.getTarget().getVariable();
		net.sf.orcc.df.Port port = (net.sf.orcc.df.Port) instPortRead.getPort();

		// Construct ioHandler ReadAccess Component
		ActionIOHandler ioHandler = (ActionIOHandler) port.getAttribute(
				"ioHandler").getObjectValue();
		Component pinRead = ioHandler.getReadAccess(false);
		pinRead.setNonRemovable();

		// Get Exit and ResultBus
		Exit exit = pinRead.getExit(Exit.DONE);
		Bus resultBus = exit.getDataBuses().get(0);

		// Add to bus dependencies
		busDependecies.put(resultBus, target);
		return pinRead;
	}

	public Component caseInstPortWrite(InstPortWrite instPortWrite) {
		List<Component> sequence = new ArrayList<Component>();
		Component value = new ExprToComponent().doSwitch(instPortWrite
				.getValue());
		sequence.add(value);

		net.sf.orcc.df.Port port = (net.sf.orcc.df.Port) instPortWrite
				.getPort();
		ActionIOHandler ioHandler = (ActionIOHandler) port.getAttribute(
				"ioHandler").getObjectValue();
		Component pinWrite = ioHandler.getWriteAccess(false);
		pinWrite.setNonRemovable();
		sequence.add(pinWrite);

		Block block = new Block(sequence);

		// -- Input Dependencies
		@SuppressWarnings("unchecked")
		Map<Var, Port> exprInput = (Map<Var, Port>) instPortWrite.getValue()
				.getAttribute("inputs").getObjectValue();
		for (Var var : exprInput.keySet()) {
			Type type = var.getType();
			Port dataPort = exprInput.get(var);
			
			Port blkDataPort = block.makeDataPort(var.getName(),
					type.getSizeInBits(), type.isInt());
			Bus blkDataPortBus = blkDataPort.getPeer();
			
			ComponentUtil.connectDataDependency(blkDataPortBus, dataPort, 0);
			portDependecies.put(blkDataPort, var);
		}

		// -- Value Component --> pinWrite dependency
		Bus resultBus = value.getExit(Exit.DONE).getDataBuses().get(0);
		Port dataPort = pinWrite.getDataPorts().get(0);
		ComponentUtil.connectDataDependency(resultBus, dataPort, 0);

		return block;
	}

	public Component caseInstPortStatus(InstPortStatus instPortStatus) {
		Var target = instPortStatus.getTarget().getVariable();
		net.sf.orcc.df.Port port = (net.sf.orcc.df.Port) instPortStatus
				.getPort();

		// Construct ioHandler ReadAccess Component
		ActionIOHandler ioHandler = (ActionIOHandler) port.getAttribute(
				"ioHandler").getObjectValue();
		Component pinStatus = ioHandler.getStatusAccess();
		pinStatus.setNonRemovable();

		// Get Exit and ResultBus
		Exit exit = pinStatus.getExit(Exit.DONE);
		Bus resultBus = exit.getDataBuses().get(0);

		// Add to bus dependencies
		busDependecies.put(resultBus, target);
		return pinStatus;
	}

	@Override
	public Component caseInstStore(InstStore store) {
		Var target = store.getTarget().getVariable();

		List<Component> sequence = new ArrayList<Component>();

		Expression value = store.getValue();
		// Grab component from the expression
		Component comp = null;
		Bus compResultBus = null;
		if (!value.isExprVar()) {
			comp = new ExprToComponent().doSwitch(value);
			compResultBus = comp.getExit(Exit.DONE).getDataBuses().get(0);
		}

		// Add component if any
		if (comp != null) {
			sequence.add(comp);
		}

		if (!target.getType().isList()) {
			boolean isSigned = target.getType().isInt();
			Location location = null;
			PatternImpl pattern = EcoreHelper.getContainerOfType(target,
					PatternImpl.class);
			if (pattern != null) {
				net.sf.orcc.df.Port dfPort = pattern.getVarToPortMap().get(
						target);
				location = (Location) dfPort.getAttribute("location")
						.getObjectValue();
			} else {
				location = (Location) target.getAttribute("location")
						.getObjectValue();
			}
			LogicalMemoryPort memPort = location.getLogicalMemory()
					.getLogicalMemoryPorts().iterator().next();
			AbsoluteMemoryWrite absoluteMemWrite = new AbsoluteMemoryWrite(
					location, Constants.MAX_ADDR_WIDTH, isSigned);
			sequence.add(absoluteMemWrite);
			memPort.addAccess((LValue) absoluteMemWrite, location);

			// Create Block
			Block block = new Block(sequence);

			// Resolve inputs
			@SuppressWarnings("unchecked")
			Map<Var, Port> exprInput = (Map<Var, Port>) value.getAttribute(
					"inputs").getObjectValue();
			for (Var var : exprInput.keySet()) {
				Type type = var.getType();

				Port dataPort = block.makeDataPort(type.getSizeInBits(),
						type.isInt());
				Bus dataPortPeer = dataPort.getPeer();
				ComponentUtil.connectDataDependency(dataPortPeer,
						exprInput.get(var), 0);
				portDependecies.put(dataPort, var);
			}

			// Resolve Dependencies
			Port dataPort = absoluteMemWrite.getDataPorts().get(0);
			ComponentUtil.connectDataDependency(compResultBus, dataPort, 0);

			return block;

		} else {
			// Index Flattener has flatten all indexes only one expression
			// possible
			Expression index = store.getIndexes().get(0);
			Component indexComp = new ExprToComponent().doSwitch(index);
			sequence.add(indexComp);

			// Get Type
			TypeList typeList = (TypeList) target.getType();
			Type innerType = typeList.getInnermostType();
			int dataSize = innerType.getSizeInBits();
			boolean isSigned = innerType.isInt();

			// Get Location
			Location location = null;
			PatternImpl pattern = EcoreHelper.getContainerOfType(target,
					PatternImpl.class);
			if (pattern != null) {
				net.sf.orcc.df.Port dfPort = pattern.getVarToPortMap().get(
						target);
				location = (Location) dfPort.getAttribute("location")
						.getObjectValue();
			} else {
				location = (Location) target.getAttribute("location")
						.getObjectValue();
			}

			LogicalMemoryPort memPort = location.getLogicalMemory()
					.getLogicalMemoryPorts().iterator().next();
			AddressStridePolicy addrPolicy = location.getAbsoluteBase()
					.getInitialValue().getAddressStridePolicy();
			HeapWrite write = new HeapWrite(dataSize / addrPolicy.getStride(),
					32, // max address width
					0, // fixed offset
					isSigned, // is signed?
					addrPolicy); // addressing policy
			sequence.add(write);

			memPort.addAccess(write, location);

			LocationConstant locationConst = new LocationConstant(location, 32,
					location.getAbsoluteBase().getLogicalMemory()
							.getAddressStridePolicy());
			sequence.add(locationConst);

			CastOp cast = new CastOp(32, false);
			sequence.add(cast);

			AddOp adder = new AddOp();
			sequence.add(adder);

			// Build read Block
			Block storedBlock = new Block(sequence);

			// Dependencies
			// -- Input form Value

			if (compResultBus == null) {
				// Means that value is an ExprVar
				Var source = ((ExprVar) value).getUse().getVariable();
				Type type = source.getType();
				Port port = storedBlock.makeDataPort(source.getName(),
						type.getSizeInBits(), type.isInt() || type.isBool());
				compResultBus = port.getPeer();

				portDependecies.put(port, source);
			}

			@SuppressWarnings("unchecked")
			Map<Var, Port> exprInput = (Map<Var, Port>) index.getAttribute(
					"inputs").getObjectValue();
			for (Var var : exprInput.keySet()) {
				Type type = var.getType();

				Port dataPort = storedBlock.makeDataPort(type.getSizeInBits(),
						type.isInt());
				Bus dataPortPeer = dataPort.getPeer();
				ComponentUtil.connectDataDependency(dataPortPeer,
						exprInput.get(var), 0);
				portDependecies.put(dataPort, var);
			}

			if (comp != null) {
				@SuppressWarnings("unchecked")
				Map<Var, Port> valueInput = (Map<Var, Port>) value
						.getAttribute("inputs").getObjectValue();
				for (Var var : valueInput.keySet()) {
					Type type = var.getType();
					// Create a Port on the storeBlock
					Port dataPort = storedBlock.makeDataPort(var.getName(),
							type.getSizeInBits(), type.isInt());
					Bus dataPortPeer = dataPort.getPeer();

					ComponentUtil.connectDataDependency(dataPortPeer,
							valueInput.get(var), 0);

					// Add Block port to dependencies
					portDependecies.put(dataPort, var);
				}
			}

			// Build dependencies between block store components
			Bus indexCompDataBus = indexComp.getExit(Exit.DONE).getDataBuses()
					.get(0);

			cast.getEntries()
					.get(0)
					.addDependency(cast.getDataPort(),
							new DataDependency(indexCompDataBus));

			adder.getEntries()
					.get(0)
					.addDependency(adder.getLeftDataPort(),
							new DataDependency(locationConst.getValueBus()));
			adder.getEntries()
					.get(0)
					.addDependency(adder.getRightDataPort(),
							new DataDependency(cast.getResultBus()));

			ComponentUtil.connectDataDependency(compResultBus,
					write.getValuePort(), 0);

			write.getEntries()
					.get(0)
					.addDependency(write.getBaseAddressPort(),
							new DataDependency(adder.getResultBus()));

			Exit done = storedBlock.getExit(Exit.DONE);

			done.getPeer()
					.getEntries()
					.get(0)
					.addDependency(
							done.getDoneBus().getPeer(),
							new ControlDependency(write.getExit(Exit.DONE)
									.getDoneBus()));

			return storedBlock;
		}
	}

	@Override
	public Component defaultCase(EObject object) {
		if (object instanceof InstPortRead) {
			return caseInstPortRead((InstPortRead) object);
		} else if (object instanceof InstPortWrite) {
			return caseInstPortWrite((InstPortWrite) object);
		} else if (object instanceof InstPortStatus) {
			return caseInstPortStatus((InstPortStatus) object);
		}
		return null;
	}

	@Override
	public Component visitInstructions(List<Instruction> instructions) {
		int oldIndexInst = indexInst;
		Component result = null;
		List<Component> sequence = new ArrayList<Component>();
		for (indexInst = 0; indexInst < instructions.size(); indexInst++) {
			Instruction inst = instructions.get(indexInst);
			result = doSwitch(inst);
			if (result != null)
				sequence.add(result);
		}

		// restore old index
		indexInst = oldIndexInst;
		Block block = new Block(sequence);

		Map<Var, List<Bus>> lastDefVarBus = new HashMap<Var, List<Bus>>();

		// Build the current block inputs and
		// Set the dependencies for the rest of the components
		for (Component component : sequence) {
			List<Port> dataPorts = component.getDataPorts();
			for (Port port : dataPorts) {
				Var var = portDependecies.get(port);
				if (lastDefVarBus.containsKey(var)) {
					List<Bus> buses = lastDefVarBus.get(var);
					int lastDefIndx = buses.size() - 1;
					// Get last defined bus
					Bus bus = buses.get(lastDefIndx);
					ComponentUtil.connectDataDependency(bus, port, 0);
				} else {
					// It is an input
					Type type = var.getType();
					if (inputs.containsKey(var)) {
						Port blkDataPort = inputs.get(var);
						Bus blkDataBus = blkDataPort.getPeer();
						ComponentUtil
								.connectDataDependency(blkDataBus, port, 0);
					} else {
						// Create a data Port
						Port blkDataPort = block.makeDataPort(var.getName(),
								type.getSizeInBits(),
								type.isInt() || type.isBool());
						inputs.put(var, blkDataPort);
						Bus blkDataBus = blkDataPort.getPeer();
						ComponentUtil
								.connectDataDependency(blkDataBus, port, 0);
					}
				}
			}
			// All dataBuses should be given as lastDefined here
			List<Bus> dataBuses = component.getExit(Exit.DONE).getDataBuses();
			for (Bus bus : dataBuses) {
				Var var = busDependecies.get(bus);
				lastDefVarBus.put(var, Arrays.asList(bus));
			}
		}

		// Build the current block outputs
		ListIterator<Component> iter = sequence.listIterator(sequence.size());

		while (iter.hasPrevious()) {
			Component component = iter.previous();
			List<Bus> dataBuses = component.getExit(Exit.DONE).getDataBuses();
			for (Bus bus : dataBuses) {
				Var var = busDependecies.get(bus);
				if (!outputs.containsKey(var)) {
					Type type = var.getType();
					Bus blkOutputBus = block.getExit(Exit.DONE).makeDataBus(
							var.getName(), type.getSizeInBits(),
							type.isInt() || type.isBool());
					Port blkOutputPort = blkOutputBus.getPeer();
					// Add dependency
					ComponentUtil.connectDataDependency(bus, blkOutputPort, 0);

					outputs.put(var, blkOutputBus);
				}
			}
		}

		return block;
	}

	@Override
	public Component caseInstReturn(InstReturn returnInstr) {
		Expression value = returnInstr.getValue();
		if (value != null) {
			Component comp = new ExprToComponent().doSwitch(value);
			// -- Data Ports dependencies
			@SuppressWarnings("unchecked")
			Map<Var, Port> exprInput = (Map<Var, Port>) value.getAttribute(
					"inputs").getObjectValue();
			for (Var var : exprInput.keySet()) {
				Port port = exprInput.get(var);
				portDependecies.put(port, var);
			}

			Var target = (Var) returnInstr.getAttribute("returnTarget")
					.getReferencedValue();
			// Only one possible output, expression
			Bus resultBus = comp.getExit(Exit.DONE).getDataBuses().get(0);
			busDependecies.put(resultBus, target);
			return comp;
		}
		return null;
	}

	/**
	 * Add to inputs if the variable has not been already added
	 * 
	 * @param var
	 *            the variable
	 * @param port
	 *            the port
	 */
	private void addToInputs(Var var, Port port) {
		if (!inputs.containsKey(var)) {
			inputs.put(var, port);
		}
	}
}
