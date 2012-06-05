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

import net.sf.openforge.lim.And;
import net.sf.openforge.lim.Block;
import net.sf.openforge.lim.Bus;
import net.sf.openforge.lim.Component;
import net.sf.openforge.lim.DataDependency;
import net.sf.openforge.lim.Decision;
import net.sf.openforge.lim.Exit;
import net.sf.openforge.lim.HeapRead;
import net.sf.openforge.lim.HeapWrite;
import net.sf.openforge.lim.Or;
import net.sf.openforge.lim.Port;
import net.sf.openforge.lim.TaskCall;
import net.sf.openforge.lim.memory.AddressStridePolicy;
import net.sf.openforge.lim.memory.Location;
import net.sf.openforge.lim.memory.LogicalMemoryPort;
import net.sf.openforge.lim.memory.LogicalValue;
import net.sf.openforge.lim.op.AddOp;
import net.sf.openforge.lim.op.AndOp;
import net.sf.openforge.lim.op.CastOp;
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
import net.sf.orc2hdl.design.ResourceCache;
import net.sf.orc2hdl.design.util.DesignUtil;
import net.sf.orc2hdl.design.util.ModuleUtil;
import net.sf.orc2hdl.design.util.PortUtil;
import net.sf.orcc.ir.BlockIf;
import net.sf.orcc.ir.BlockWhile;
import net.sf.orcc.ir.Def;
import net.sf.orcc.ir.ExprBinary;
import net.sf.orcc.ir.ExprBool;
import net.sf.orcc.ir.ExprInt;
import net.sf.orcc.ir.ExprUnary;
import net.sf.orcc.ir.ExprVar;
import net.sf.orcc.ir.Expression;
import net.sf.orcc.ir.InstAssign;
import net.sf.orcc.ir.InstCall;
import net.sf.orcc.ir.InstLoad;
import net.sf.orcc.ir.InstStore;
import net.sf.orcc.ir.IrFactory;
import net.sf.orcc.ir.OpBinary;
import net.sf.orcc.ir.OpUnary;
import net.sf.orcc.ir.Procedure;
import net.sf.orcc.ir.Type;
import net.sf.orcc.ir.TypeList;
import net.sf.orcc.ir.Var;
import net.sf.orcc.ir.util.AbstractIrVisitor;

/**
 * This visitor visit the procedural blocks and it creates a Design component
 * 
 * @author Endri Bezati
 * 
 */
public class InstructionVisitor extends AbstractIrVisitor<List<Component>> {

	private Def assignTarget;
	private Integer castIndex = 0;
	private Integer listIndexes = 0;

	/** Current List Component **/
	private List<Component> componentList;
	/** Current Component **/
	private Component currentComponent;

	/** Design Resources **/
	protected ResourceCache resources;

	/** Dependency between Components and Bus-Var **/
	protected Map<Bus, Var> busDependency;

	/** Action component Counter **/
	protected Integer componentCounter;

	/** Dependency between Components and Done Bus **/
	protected Map<Bus, Integer> doneBusDependency;

	/** Dependency between Components and Port-Var **/
	protected Map<Port, Var> portDependency;

	/** Dependency between Components and Port-Var **/
	protected Map<Port, Integer> portGroupDependency;

	/** Design stateVars **/
	protected Map<LogicalValue, Var> stateVars;

	public InstructionVisitor(ResourceCache resources, List<Component> component) {
		super(true);
		this.resources = resources;
		componentList = component;
	}

	@Override
	public List<Component> caseProcedure(Procedure procedure) {
		super.caseProcedure(procedure);
		return componentList;
	}

	@Override
	public List<Component> caseBlockIf(BlockIf blockIf) {
		List<Component> oldComponents = new ArrayList<Component>(componentList);
		// Create the decision
		Map<Var, Integer> decisionVar = resources.getBranchDecision(blockIf);
		Decision decision = null;
		for (Var condVar : decisionVar.keySet()) {
			String condName = "decision_" + procedure.getName() + "_"
					+ condVar.getIndexedName();

			decision = ModuleUtil.createDecision(condVar, condName,
					portDependency, busDependency, portGroupDependency,
					doneBusDependency);
		}
		// Visit the then block
		componentList = new ArrayList<Component>();
		doSwitch(blockIf.getThenBlocks());
		// Get the then Input Vars
		Map<Var, Integer> thenInputs = resources.getBranchThenVars(blockIf);
		Map<Var, Integer> thenOutputs = resources
				.getBranchThenOutputVars(blockIf);
		Block thenBlock = (Block) ModuleUtil.createModule(componentList,
				thenInputs, thenOutputs, "thenBlock", Exit.DONE, 0,
				portDependency, busDependency, portGroupDependency,
				doneBusDependency);
		thenBlock.setIDLogical("thenBlock");
		// Visit the else block
		Block elseBlock = null;

		if (!blockIf.getElseBlocks().isEmpty()) {
			componentList = new ArrayList<Component>();
			doSwitch(blockIf.getElseBlocks());

			Map<Var, Integer> elseInputs = resources.getBranchElseVars(blockIf);
			Map<Var, Integer> elseOutputs = resources
					.getBranchElseOutputVars(blockIf);
			elseBlock = (Block) ModuleUtil.createModule(componentList,
					elseInputs, elseOutputs, "elseBlock", Exit.DONE, 1,
					portDependency, busDependency, portGroupDependency,
					doneBusDependency);
			thenBlock.setIDLogical("elseBlock");
		} else {
			elseBlock = (Block) ModuleUtil.createModule(
					Collections.<Component> emptyList(),
					Collections.<Var, Integer> emptyMap(),
					Collections.<Var, Integer> emptyMap(), "elseBlock",
					Exit.DONE, 1, portDependency, busDependency,
					portGroupDependency, doneBusDependency);
			elseBlock.setIDLogical("elseBlock");
		}
		// Get All input Vars
		Map<Var, Integer> ifInputVars = resources.getBranchInputs(blockIf);
		Map<Var, Integer> ifOutputVars = Collections.emptyMap();
		// Get Phi target Vars, aka branchIf Outputs
		Map<Var, List<Var>> phiOuts = resources.getBranchPhiVars(blockIf);
		currentComponent = ModuleUtil.createBranch(decision, thenBlock,
				elseBlock, ifInputVars, ifOutputVars, phiOuts, "ifBLOCK",
				Exit.DONE, portDependency, busDependency, portGroupDependency,
				doneBusDependency);
		componentList = new ArrayList<Component>();
		componentList.addAll(oldComponents);
		componentList.add(currentComponent);
		return null;
	}

	@Override
	public List<Component> caseBlockWhile(BlockWhile blockWhile) {
		return null;
	}

	@Override
	public List<Component> caseExprBinary(ExprBinary expr) {
		// Get the size of the target and give it to the component
		int sizeInBits = assignTarget.getVariable().getType().getSizeInBits();
		Component component = null;
		if (expr.getOp() == OpBinary.BITAND) {
			component = new AndOp();
		} else if (expr.getOp() == OpBinary.BITOR) {
			component = new OrOp();
		} else if (expr.getOp() == OpBinary.BITXOR) {
			component = new XorOp();
		} else if (expr.getOp() == OpBinary.DIV) {
			component = new DivideOp(sizeInBits);
		} else if (expr.getOp() == OpBinary.DIV_INT) {
			component = new DivideOp(sizeInBits);
		} else if (expr.getOp() == OpBinary.EQ) {
			component = new EqualsOp();
		} else if (expr.getOp() == OpBinary.GE) {
			component = new GreaterThanEqualToOp();
		} else if (expr.getOp() == OpBinary.GT) {
			component = new GreaterThanOp();
		} else if (expr.getOp() == OpBinary.LE) {
			component = new LessThanEqualToOp();
		} else if (expr.getOp() == OpBinary.LOGIC_AND) {
			component = new And(2);
		} else if (expr.getOp() == OpBinary.LOGIC_OR) {
			component = new Or(2);
		} else if (expr.getOp() == OpBinary.LT) {
			component = new LessThanOp();
		} else if (expr.getOp() == OpBinary.MINUS) {
			component = new SubtractOp();
		} else if (expr.getOp() == OpBinary.MOD) {
			component = new ModuloOp();
		} else if (expr.getOp() == OpBinary.NE) {
			component = new NotEqualsOp();
		} else if (expr.getOp() == OpBinary.PLUS) {
			component = new AddOp();
		} else if (expr.getOp() == OpBinary.SHIFT_LEFT) {
			int log2N = MathStuff.log2(sizeInBits);
			component = new LeftShiftOp(log2N);
		} else if (expr.getOp() == OpBinary.SHIFT_RIGHT) {
			int log2N = MathStuff.log2(sizeInBits);
			component = new RightShiftOp(log2N);
		} else if (expr.getOp() == OpBinary.TIMES) {
			component = new MultiplyOp(expr.getType().getSizeInBits());
		}
		// Three address code obligated, a binary expression
		// can not contain another binary expression
		// Get variables for E1 and E2
		Var e1 = ((ExprVar) expr.getE1()).getUse().getVariable();
		Var e2 = ((ExprVar) expr.getE2()).getUse().getVariable();
		Integer newMaxSize = assignTarget.getVariable().getType()
				.getSizeInBits();
		PortUtil.mapInDataPorts(component, binaryCastOp(e1, e2, newMaxSize),
				portDependency, portGroupDependency);
		currentComponent = component;
		return null;
	}

	protected Map<Var, Integer> binaryCastOp(Var e1, Var e2, Integer newMaxSize) {
		Boolean isSigned = e1.getType().isInt() || e2.getType().isInt();
		Map<Var, Integer> newVars = new HashMap<Var, Integer>();
		// Add the new Casted variables, group 0 by default
		newVars.put(unaryCastOp(e1, newMaxSize, isSigned), 0);
		newVars.put(unaryCastOp(e2, newMaxSize, isSigned), 0);
		return newVars;
	}

	protected Var unaryCastOp(Var var, Integer newMaxSize, Boolean isSigned) {
		Var newVar = var;
		Integer sizeVar = var.getType().getSizeInBits();

		if (sizeVar != newMaxSize) {
			currentComponent = new CastOp(newMaxSize, isSigned);
			Map<Var, Integer> ioVars = new HashMap<Var, Integer>();
			ioVars.put(var, 0);

			PortUtil.mapInDataPorts(currentComponent, ioVars, portDependency,
					portGroupDependency);
			Var castedVar = procedure.newTempLocalVariable(
					IrFactory.eINSTANCE.createTypeInt(newMaxSize), "casted_"
							+ castIndex + "_" + var.getIndexedName());
			ioVars = new HashMap<Var, Integer>();
			ioVars.put(castedVar, 0);

			PortUtil.mapOutDataPorts(currentComponent, ioVars, busDependency,
					doneBusDependency);
			componentList.add(currentComponent);
			newVar = castedVar;
			castIndex++;
		}

		return newVar;
	}

	@Override
	public List<Component> caseExprBool(ExprBool expr) {
		final long value = expr.isValue() ? 1 : 0;
		currentComponent = new SimpleConstant(value, 1, true);
		return null;
	}

	@Override
	public List<Component> caseExprInt(ExprInt expr) {
		final long value = expr.getIntValue();
		int sizeInBits = 32;

		if (assignTarget != null) {
			sizeInBits = assignTarget.getVariable().getType().getSizeInBits();
		} else {
			sizeInBits = expr.getType().getSizeInBits();
		}

		currentComponent = new SimpleConstant(value, sizeInBits, expr.getType()
				.isInt());
		return null;
	}

	@Override
	public List<Component> caseExprUnary(ExprUnary expr) {
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
	public List<Component> caseExprVar(ExprVar var) {
		currentComponent = new NoOp(1, Exit.DONE);
		Var useVar = var.getUse().getVariable();
		Map<Var, Integer> inVar = new HashMap<Var, Integer>();
		inVar.put(useVar, 0);

		PortUtil.mapInDataPorts(currentComponent, inVar, portDependency,
				portGroupDependency);
		return null;
	}

	@Override
	public List<Component> caseInstAssign(InstAssign assign) {
		// Get the size in bits of the target
		assignTarget = assign.getTarget();
		super.caseInstAssign(assign);
		if (currentComponent != null) {
			componentList.add(currentComponent);
			Map<Var, Integer> outVar = new HashMap<Var, Integer>();
			outVar.put(assign.getTarget().getVariable(), 0);
			PortUtil.mapOutDataPorts(currentComponent, outVar, busDependency,
					doneBusDependency);
		}
		// Put target to null
		assignTarget = null;
		return null;
	}

	@Override
	public List<Component> caseInstCall(InstCall call) {
		currentComponent = new TaskCall();
		resources.addTaskCall(call, (TaskCall) currentComponent);
		return null;
	}

	@Override
	public List<Component> caseInstLoad(InstLoad load) {
		Var sourceVar = load.getSource().getVariable();

		// At this moment the load should have only one index
		Var loadIndexVar = null;
		List<Expression> indexes = load.getIndexes();
		for (Expression expr : new ArrayList<Expression>(indexes)) {
			loadIndexVar = ((ExprVar) expr).getUse().getVariable();
		}

		TypeList typeList = (TypeList) sourceVar.getType();
		Type type = typeList.getInnermostType();

		Boolean isSigned = type.isInt();
		Location targetLocation = resources.getLocation(sourceVar);

		LogicalMemoryPort memPort = targetLocation.getLogicalMemory()
				.getLogicalMemoryPorts().iterator().next();

		AddressStridePolicy addrPolicy = targetLocation.getAbsoluteBase()
				.getInitialValue().getAddressStridePolicy();

		int dataSize = type.getSizeInBits();
		HeapRead read = new HeapRead(dataSize / addrPolicy.getStride(), 32, 0,
				isSigned, addrPolicy);
		CastOp castOp = new CastOp(dataSize, isSigned);
		Block block = DesignUtil.buildAddressedBlock(read, targetLocation,
				Collections.singletonList((Component) castOp));
		Bus result = block.getExit(Exit.DONE).makeDataBus();
		castOp.getEntries()
				.get(0)
				.addDependency(castOp.getDataPort(),
						new DataDependency(read.getResultBus()));
		result.getPeer()
				.getOwner()
				.getEntries()
				.get(0)
				.addDependency(result.getPeer(),
						new DataDependency(castOp.getResultBus()));

		memPort.addAccess(read, targetLocation);

		Var indexVar = procedure.newTempLocalVariable(
				IrFactory.eINSTANCE.createTypeInt(), "index" + listIndexes);
		listIndexes++;
		currentComponent = new CastOp(dataSize, isSigned);

		Map<Var, Integer> ioVars = new HashMap<Var, Integer>();
		ioVars.put(loadIndexVar, 0);
		PortUtil.mapInDataPorts(currentComponent, ioVars, portDependency,
				portGroupDependency);
		// For the moment all indexes are considered 32bits
		Var castedIndexVar = procedure.newTempLocalVariable(
				IrFactory.eINSTANCE.createTypeInt(32), "casted_" + castIndex
						+ "_" + loadIndexVar.getIndexedName());

		ioVars = new HashMap<Var, Integer>();
		ioVars.put(castedIndexVar, 0);

		PortUtil.mapOutDataPorts(currentComponent, ioVars, busDependency,
				doneBusDependency);
		componentList.add(currentComponent);
		castIndex++;
		// add the assign instruction for each index
		InstAssign assign = IrFactory.eINSTANCE.createInstAssign(indexVar,
				castedIndexVar);
		doSwitch(assign);

		currentComponent = block;
		ioVars = new HashMap<Var, Integer>();
		ioVars.put(indexVar, 0);

		PortUtil.mapInDataPorts(currentComponent, ioVars, portDependency,
				portGroupDependency);

		ioVars = new HashMap<Var, Integer>();
		ioVars.put(load.getTarget().getVariable(), 0);
		PortUtil.mapOutDataPorts(currentComponent, ioVars, busDependency,
				doneBusDependency);

		componentList.add(currentComponent);

		return null;
	}

	@Override
	public List<Component> caseInstStore(InstStore store) {
		Var targetVar = store.getTarget().getVariable();

		// At this moment the load should have only one index
		Var storeIndexVar = null;
		List<Expression> indexes = store.getIndexes();
		for (Expression expr : new ArrayList<Expression>(indexes)) {
			storeIndexVar = ((ExprVar) expr).getUse().getVariable();
		}

		// Get store Value Var, after TAC the expression is a ExprVar
		ExprVar value = (ExprVar) store.getValue();
		Var valueVar = value.getUse().getVariable();

		// Get the inner type of the list
		TypeList typeList = (TypeList) targetVar.getType();
		Type type = typeList.getInnermostType();

		// Get Location form resources
		Location targetLocation = resources.getLocation(targetVar);
		LogicalMemoryPort memPort = targetLocation.getLogicalMemory()
				.getLogicalMemoryPorts().iterator().next();
		AddressStridePolicy addrPolicy = targetLocation.getAbsoluteBase()
				.getInitialValue().getAddressStridePolicy();

		int dataSize = type.getSizeInBits();
		Boolean isSigned = type.isInt();

		HeapWrite heapWrite = new HeapWrite(dataSize / addrPolicy.getStride(),
				32, // max address width
				0, // fixed offset
				isSigned, // is signed?
				addrPolicy); // addressing policy

		Block block = DesignUtil.buildAddressedBlock(heapWrite, targetLocation,
				Collections.<Component> emptyList());
		Port data = block.makeDataPort();
		heapWrite
				.getEntries()
				.get(0)
				.addDependency(heapWrite.getValuePort(),
						new DataDependency(data.getPeer()));

		memPort.addAccess(heapWrite, targetLocation);

		Var indexVar = procedure.newTempLocalVariable(
				IrFactory.eINSTANCE.createTypeInt(32), "index" + listIndexes);
		listIndexes++;
		currentComponent = new CastOp(32, isSigned);

		Map<Var, Integer> ioVars = new HashMap<Var, Integer>();
		ioVars.put(storeIndexVar, 0);

		PortUtil.mapInDataPorts(currentComponent, ioVars, portDependency,
				portGroupDependency);
		Var castedIndexVar = procedure.newTempLocalVariable(
				IrFactory.eINSTANCE.createTypeInt(32), "casted_" + castIndex
						+ "_" + storeIndexVar.getIndexedName());

		ioVars = new HashMap<Var, Integer>();
		ioVars.put(castedIndexVar, 0);

		PortUtil.mapOutDataPorts(currentComponent, ioVars, busDependency,
				doneBusDependency);
		componentList.add(currentComponent);
		castIndex++;
		// add the assign instruction for each index
		InstAssign assign = IrFactory.eINSTANCE.createInstAssign(indexVar,
				castedIndexVar);
		doSwitch(assign);

		currentComponent = block;
		ioVars = new HashMap<Var, Integer>();
		ioVars.put(indexVar, 0);
		ioVars.put(valueVar, 0);

		PortUtil.mapInDataPorts(currentComponent, ioVars, portDependency,
				portGroupDependency);
		PortUtil.mapOutControlPort(currentComponent, 0, doneBusDependency);
		componentList.add(currentComponent);

		return null;
	}

}
