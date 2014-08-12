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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sf.orcc.ir.Arg;
import net.sf.orcc.ir.ArgByRef;
import net.sf.orcc.ir.ArgByVal;
import net.sf.orcc.ir.Def;
import net.sf.orcc.ir.ExprVar;
import net.sf.orcc.ir.Expression;
import net.sf.orcc.ir.InstCall;
import net.sf.orcc.ir.InstLoad;
import net.sf.orcc.ir.InstReturn;
import net.sf.orcc.ir.InstStore;
import net.sf.orcc.ir.IrFactory;
import net.sf.orcc.ir.Param;
import net.sf.orcc.ir.Procedure;
import net.sf.orcc.ir.Type;
import net.sf.orcc.ir.Use;
import net.sf.orcc.ir.Var;
import net.sf.orcc.ir.util.AbstractIrVisitor;
import net.sf.orcc.ir.util.IrUtil;

import org.xronos.openforge.app.project.SearchLabel;
import org.xronos.openforge.lim.Block;
import org.xronos.openforge.lim.Bus;
import org.xronos.openforge.lim.Call;
import org.xronos.openforge.lim.CodeLabel;
import org.xronos.openforge.lim.Component;
import org.xronos.openforge.lim.Exit;
import org.xronos.openforge.lim.IPCoreCall;
import org.xronos.openforge.lim.Port;
import org.xronos.openforge.lim.op.CastOp;
import org.xronos.openforge.util.Debug;
import org.xronos.orcc.forge.transform.analysis.Liveness;
import org.xronos.orcc.forge.transform.analysis.XronosCFG;

public class CallToCall extends AbstractIrVisitor<Component> {

	/**
	 * This inner class replaces the local list variables with a referenced one
	 * 
	 * @author Endri Bezati
	 * 
	 */
	public class PropagateReferences extends AbstractIrVisitor<Void> {

		Map<Var, Expression> paramVarToExpr;

		Map<Var, Var> paramVarToRefVar;

		public PropagateReferences(Map<Var, Var> paramVarToRefVar,
				Map<Var, Expression> paramVarToExpr) {
			super(true);
			this.paramVarToRefVar = paramVarToRefVar;
			this.paramVarToExpr = paramVarToExpr;
		}

		@Override
		public Void caseInstCall(InstCall call) {
			int nbArg = 0;
			Procedure proc = call.getProcedure();
			// Propagate References on arguments
			Map<Arg, Arg> oldNewArguments = new HashMap<Arg, Arg>();
			for (Arg arg : call.getArguments()) {
				Param param = proc.getParameters().get(nbArg);
				if (param.getVariable().getType().isList()) {
					Expression exprArg = ((ArgByVal) arg).getValue();
					Var var = ((ExprVar) exprArg).getUse().getVariable();
					if (paramVarToRefVar.containsKey(var)) {
						Var replaceVar = paramVarToRefVar.get(var);
						Expression expr = IrFactory.eINSTANCE
								.createExprVar(replaceVar);
						Arg newArg = IrFactory.eINSTANCE.createArgByVal(expr);
						oldNewArguments.put(arg, newArg);
					}
				}

				nbArg++;
			}

			// Replace with new arguments
			for (Arg arg : oldNewArguments.keySet()) {
				Integer index = call.getArguments().indexOf(arg);
				call.getArguments().remove(arg);
				call.getArguments().add(index, oldNewArguments.get(arg));
			}

			return null;
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

	@Override
	public Component caseInstCall(InstCall call) {
		// -- Can be a Call or IPCoreCall
		Call limCall = null;
		// -- Procedure has return value
		boolean hasReturnValue = !call.getProcedure().getReturnType().isVoid();
		// -- Block inputs
		Map<Var, Port> inputs = new HashMap<Var, Port>();
		// -- Block Outputs
		Map<Var, Bus> outputs = new HashMap<Var, Bus>();

		// -- Sequence of components
		List<Component> sequence = new ArrayList<Component>();

		Map<Var, Var> byRefVar = new HashMap<Var, Var>();
		Map<Var, Component> byValComponent = new HashMap<Var, Component>();
		Map<Component, CastOp> byValComponentCast = new HashMap<Component, CastOp>();
		Map<Var, Expression> byValExpression = new HashMap<Var, Expression>();
		Map<Bus, Var> byValBusVar = new HashMap<Bus, Var>();

		// Clone Procedure
		Procedure proc = IrUtil.copy(call.getProcedure());
		new XronosCFG().doSwitch(proc);
		new Liveness().doSwitch(proc);
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
						// Already resolved from Front-end
						throw (new NullPointerException("ExprArg is ExprList"));
					}
				}
				byRefVar.put(param.getVariable(), refVar);
			} else {
				Expression exprArg = ((ArgByVal) arg).getValue();
				Component argComponent = new ExprToComponent()
						.doSwitch(exprArg);
				sequence.add(argComponent);
				Var paramVar = param.getVariable();
				Type type = paramVar.getType();
				Bus resultBus = argComponent.getExit(Exit.DONE).getDataBuses()
						.get(0);

				Type typeExpr = exprArg.getType();
				// Add casting if necessary
				if (type.getSizeInBits() != typeExpr.getSizeInBits()) {
					CastOp cast = new CastOp(type.getSizeInBits(), type.isInt());
					sequence.add(cast);
					byValComponentCast.put(argComponent, cast);
				}

				byValBusVar.put(resultBus, paramVar);
				// For the components to be included on a new block
				byValComponent.put(paramVar, argComponent);
				byValExpression.put(paramVar, exprArg);
			}
			nbArg++;
		}

		// Propagate all reference if any
		if (!byRefVar.isEmpty() || !byValExpression.isEmpty()) {
			new PropagateReferences(byRefVar, byValExpression).doSwitch(proc);
		}

		if (call.getProcedure().isNative()) {
			limCall = new IPCoreCall(null);
			Block block = new Block(Collections.<Component> emptyList(), true);
			final org.xronos.openforge.lim.Procedure procedure = new org.xronos.openforge.lim.Procedure(
					block);
			limCall.setProcedure(procedure);
			sequence.add(limCall);
			SearchLabel sl = new CodeLabel(procedure, call.getProcedure()
					.getName());
			procedure.setSearchLabel(sl);
			limCall.setSourceName(call.getProcedure().getName());
			limCall.setIDLogical(call.getProcedure().getName());
		} else {
			// Create call
			Block callBody = null;
			if (hasReturnValue) {
				Var target = call.getTarget().getVariable();
				new PropagateReturnTarget(target).doSwitch(proc);
				callBody = new ProcedureToBlock(target).doSwitch(proc);
			} else {
				callBody = new ProcedureToBlock(true).doSwitch(proc);
			}
			org.xronos.openforge.lim.Procedure procedure = new org.xronos.openforge.lim.Procedure(
					callBody, hasReturnValue);
			limCall = procedure.makeCall();
			SearchLabel sl = new CodeLabel(procedure, call.getProcedure()
					.getName());
			procedure.setSearchLabel(sl);
			limCall.setSourceName(call.getProcedure().getName());
			limCall.setIDLogical(call.getProcedure().getName());
			// -- Port Dependencies

			// -- Data Dependency
			if (hasReturnValue) {

			}

			sequence.add(limCall);
		}

		// -- Single Data Bus on component Exit
		CastOp castOp = null;
		if (!proc.getReturnType().isVoid()) {
			Var target = call.getTarget().getVariable();
			Type type = target.getType();
			if (type.getSizeInBits() != proc.getReturnType().getSizeInBits()) {
				castOp = new CastOp(type.getSizeInBits(), type.isInt());
				sequence.add(castOp);
			}
		}

		Component component = new Block(sequence);
		Map<Var, Port> blkDataports = new HashMap<Var, Port>();

		// Dependencies
		nbArg = 0;
		for (Param param : proc.getParameters()) {
			Var paramVar = param.getVariable();

			if (byValComponent.containsKey(paramVar)) {
				// --Data Port dependency from arguments
				Expression expr = byValExpression.get(paramVar);
				@SuppressWarnings("unchecked")
				Map<Var, Port> exprInputs = (Map<Var, Port>) expr.getAttribute(
						"inputs").getObjectValue();
				for (Var var : exprInputs.keySet()) {
					Type type = var.getType();
					if (blkDataports.containsKey(var)) {
						Port dataPort = blkDataports.get(var);

						Bus dataPortPeer = dataPort.getPeer();
						ComponentUtil.connectDataDependency(dataPortPeer,
								exprInputs.get(var), 0);
					} else {
						Port dataPort = component.makeDataPort(var.getName(),
								type.getSizeInBits(), type.isInt());
						Bus dataPortPeer = dataPort.getPeer();
						ComponentUtil.connectDataDependency(dataPortPeer,
								exprInputs.get(var), 0);
						// Save DataPort
						blkDataports.put(var, dataPort);
						inputs.put(var, dataPort);
					}
				}

				// -- Argument resultBus --> call Block data port
				Component valComponent = byValComponent.get(paramVar);
				Bus resultBus = valComponent.getExit(Exit.DONE).getDataBuses()
						.get(0);
				if (byValComponentCast.containsKey(valComponent)) {
					CastOp cast = byValComponentCast.get(valComponent);
					// Dependency
					ComponentUtil.connectDataDependency(resultBus,
							cast.getDataPort(), 0);
					Var var = byValBusVar.get(resultBus);
					resultBus = cast.getResultBus();
					// Update the resultBus with the one of the cast
					byValBusVar.put(resultBus, var);

					Var resultVar = byValBusVar.get(resultBus);
					@SuppressWarnings("unchecked")
					Map<Var, Port> procInputs = (Map<Var, Port>) proc
							.getAttribute("inputs").getObjectValue();
					// Connect it if same resultVar
					if (procInputs.containsKey(resultVar)) {
						Port dataPort = limCall.getPortFromProcedurePort(procInputs.get(resultVar));
						ComponentUtil.connectDataDependency(
								cast.getResultBus(), dataPort, 0);
					}
				} else {

					Var resultVar = byValBusVar.get(resultBus);
					@SuppressWarnings("unchecked")
					Map<Var, Port> procInputs = (Map<Var, Port>) proc
							.getAttribute("inputs").getObjectValue();
					// Connect it if same resultVar
					if (procInputs.containsKey(resultVar)) {
						Port dataPort = limCall.getPortFromProcedurePort(procInputs.get(resultVar));
						ComponentUtil.connectDataDependency(resultBus,
								dataPort, 0);
					}
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
			@SuppressWarnings("unchecked")
			Map<Var, Bus> procOutpus = (Map<Var, Bus>) proc
					.getAttribute("outputs").getObjectValue();
			// -- Connect with Cast if any
			if (castOp != null) {
				Bus callResultBus = limCall.getBusFromProcedureBus(procOutpus.get(target));
				ComponentUtil.connectDataDependency(callResultBus,
						castOp.getDataPort(), 0);
				resultBus = castOp.getResultBus();
			} else {
				resultBus = limCall.getBusFromProcedureBus(procOutpus.get(target));
			}

			ComponentUtil.connectDataDependency(resultBus, blockResultBusPeer,
					0);
			outputs.put(target, blockResultBus);
		}

		// -- Set inputs and outputs attribute
		call.setAttribute("inputs", inputs);
		call.setAttribute("outputs", outputs);
		Debug.depGraphTo(component, "comp", "/tmp/comp.dot", 1);
		return component;
	}

}
