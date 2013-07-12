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

package org.xronos.orcc.design.visitors;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import net.sf.orcc.backends.ir.InstCast;
import net.sf.orcc.df.Action;
import net.sf.orcc.ir.BlockBasic;
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
import net.sf.orcc.ir.InstPhi;
import net.sf.orcc.ir.InstReturn;
import net.sf.orcc.ir.InstStore;
import net.sf.orcc.ir.IrFactory;
import net.sf.orcc.ir.OpBinary;
import net.sf.orcc.ir.OpUnary;
import net.sf.orcc.ir.Procedure;
import net.sf.orcc.ir.Type;
import net.sf.orcc.ir.TypeList;
import net.sf.orcc.ir.Var;
import net.sf.orcc.ir.util.AbstractIrVisitor;

import org.eclipse.emf.ecore.EObject;
import org.xronos.openforge.frontend.slim.builder.ActionIOHandler;
import org.xronos.openforge.lim.Block;
import org.xronos.openforge.lim.Branch;
import org.xronos.openforge.lim.Bus;
import org.xronos.openforge.lim.Component;
import org.xronos.openforge.lim.DataDependency;
import org.xronos.openforge.lim.Decision;
import org.xronos.openforge.lim.Exit;
import org.xronos.openforge.lim.HeapRead;
import org.xronos.openforge.lim.HeapWrite;
import org.xronos.openforge.lim.Loop;
import org.xronos.openforge.lim.Module;
import org.xronos.openforge.lim.Port;
import org.xronos.openforge.lim.TaskCall;
import org.xronos.openforge.lim.memory.AddressStridePolicy;
import org.xronos.openforge.lim.memory.Location;
import org.xronos.openforge.lim.memory.LogicalMemoryPort;
import org.xronos.openforge.lim.memory.LogicalValue;
import org.xronos.openforge.lim.op.AddOp;
import org.xronos.openforge.lim.op.AndOp;
import org.xronos.openforge.lim.op.CastOp;
import org.xronos.openforge.lim.op.ComplementOp;
import org.xronos.openforge.lim.op.DivideOp;
import org.xronos.openforge.lim.op.EqualsOp;
import org.xronos.openforge.lim.op.GreaterThanEqualToOp;
import org.xronos.openforge.lim.op.GreaterThanOp;
import org.xronos.openforge.lim.op.LeftShiftOp;
import org.xronos.openforge.lim.op.LessThanEqualToOp;
import org.xronos.openforge.lim.op.LessThanOp;
import org.xronos.openforge.lim.op.MinusOp;
import org.xronos.openforge.lim.op.ModuloOp;
import org.xronos.openforge.lim.op.MultiplyOp;
import org.xronos.openforge.lim.op.NoOp;
import org.xronos.openforge.lim.op.NotEqualsOp;
import org.xronos.openforge.lim.op.NotOp;
import org.xronos.openforge.lim.op.OrOp;
import org.xronos.openforge.lim.op.RightShiftOp;
import org.xronos.openforge.lim.op.SimpleConstant;
import org.xronos.openforge.lim.op.SubtractOp;
import org.xronos.openforge.lim.op.XorOp;
import org.xronos.openforge.lim.primitive.And;
import org.xronos.openforge.lim.primitive.Or;
import org.xronos.openforge.util.MathStuff;
import org.xronos.openforge.util.naming.IDSourceInfo;
import org.xronos.orcc.design.ResourceCache;
import org.xronos.orcc.design.ResourceDependecies;
import org.xronos.orcc.design.util.DesignUtil;
import org.xronos.orcc.design.util.ModuleUtil;
import org.xronos.orcc.design.util.PortUtil;
import org.xronos.orcc.design.visitors.stmIO.BlockBasicIO;
import org.xronos.orcc.design.visitors.stmIO.BranchIO;
import org.xronos.orcc.design.visitors.stmIO.LoopIO;
import org.xronos.orcc.design.visitors.stmIO.MutexIO;
import org.xronos.orcc.ir.BlockMutex;
import org.xronos.orcc.ir.InstPortPeek;
import org.xronos.orcc.ir.InstPortRead;
import org.xronos.orcc.ir.InstPortStatus;
import org.xronos.orcc.ir.InstPortWrite;

/**
 * This visitor visit the procedural blocks and it creates a Design component
 * 
 * @author Endri Bezati
 * 
 */
public class ComponentCreator extends AbstractIrVisitor<List<Component>> {

	private static boolean TEST_BLOCKBASIC = false;
	private Def assignTarget;

	/** Dependency between Components and Bus-Var **/
	protected Map<Bus, Var> busDependency;

	private Integer castIndex = 0;

	/** Action component Counter **/
	protected Integer componentCounter;

	/** Current List Component **/
	private List<Component> componentList;

	/** Current Component **/
	private Component currentComponent;

	/** Dependency between Components and Done Bus **/
	protected Map<Bus, Integer> doneBusDependency;

	/** Dependency between Components and Port-Var **/
	protected Map<Port, Var> portDependency;

	/** Dependency between Components and Port-Var **/
	protected Map<Port, Integer> portGroupDependency;

	/** Design Resources **/
	protected ResourceCache resources;

	/** Design stateVars **/
	protected Map<LogicalValue, Var> stateVars;

	private final boolean STM_DEBUG = false;

	public ComponentCreator(ResourceCache resources,
			Map<Port, Var> portDependency, Map<Bus, Var> busDependency,
			Map<Port, Integer> portGroupDependency,
			Map<Bus, Integer> doneBusDependency) {
		super(true);
		this.portDependency = portDependency;
		this.busDependency = busDependency;
		this.portGroupDependency = portGroupDependency;
		this.doneBusDependency = doneBusDependency;
		this.resources = resources;
		componentList = new ArrayList<Component>();
	}

	public ComponentCreator(ResourceCache resources,
			ResourceDependecies resourceDependecies) {
		super(true);
		this.portDependency = resourceDependecies.getPortDependency();
		this.busDependency = resourceDependecies.getBusDependency();
		this.portGroupDependency = resourceDependecies.getPortGroupDependency();
		this.doneBusDependency = resourceDependecies.getDoneBusDependency();
		this.resources = resources;
		componentList = new ArrayList<Component>();
	}

	@Override
	public List<Component> caseBlockBasic(BlockBasic block) {
		if (TEST_BLOCKBASIC) {
			List<Component> oldComponents = new ArrayList<Component>(
					componentList);

			// Visit the blockBasic
			componentList = new ArrayList<Component>();
			visitInstructions(block.getInstructions());

			BlockBasicIO blockBasicIO = new BlockBasicIO(block);
			List<Var> blockInputs = blockBasicIO.getInputs();
			List<Var> blockOutputs = blockBasicIO.getOutputs();

			Block blk = (Block) ModuleUtil.createModule(componentList,
					blockInputs, blockOutputs, "blockBasic", false, Exit.DONE,
					0, portDependency, busDependency, portGroupDependency,
					doneBusDependency);
			blk.setNonRemovable();
			blk.setIDLogical("blockBasic");
			componentList = new ArrayList<Component>();
			componentList.addAll(oldComponents);
			componentList.add(blk);

		} else {
			return super.caseBlockBasic(block);
		}
		return null;
	}

	@Override
	public List<Component> caseBlockIf(BlockIf blockIf) {
		// test
		BranchIO branchIO = new BranchIO(blockIf);
		List<Component> oldComponents = new ArrayList<Component>(componentList);
		// Create the decision
		// Var decisionVar = resources.getBlockDecisionInput(blockIf).get(0);
		Var decisionVar = branchIO.getDecision();
		Decision decision = null;
		String condName = "decision_" + procedure.getName() + "_"
				+ decisionVar.getIndexedName();

		decision = ModuleUtil.createDecision(decisionVar, condName,
				portDependency, busDependency, portGroupDependency,
				doneBusDependency);

		// Visit the then block
		componentList = new ArrayList<Component>();
		doSwitch(blockIf.getThenBlocks());

		// Get the then Input Vars
		// List<Var> thenInputs = resources.getBranchThenInput(blockIf);
		// List<Var> thenOutputs = resources.getBranchThenOutput(blockIf);

		List<Var> thenInputs = branchIO.getThenInputs();
		List<Var> thenOutputs = branchIO.getThenOutputs();

		Block thenBlock = (Block) ModuleUtil.createModule(componentList,
				thenInputs, thenOutputs, "thenBlock", false, Exit.DONE, 0,
				portDependency, busDependency, portGroupDependency,
				doneBusDependency);
		thenBlock.setNonRemovable();
		thenBlock.setIDLogical("thenBlock");
		// Visit the else block
		Block elseBlock = null;

		if (!blockIf.getElseBlocks().isEmpty()) {
			componentList = new ArrayList<Component>();
			doSwitch(blockIf.getElseBlocks());

			List<Var> elseInputs = branchIO.getElseInputs();
			List<Var> elseOutputs = branchIO.getElseOutputs();

			elseBlock = (Block) ModuleUtil.createModule(componentList,
					elseInputs, elseOutputs, "elseBlock", false, Exit.DONE, 1,
					portDependency, busDependency, portGroupDependency,
					doneBusDependency);
			thenBlock.setIDLogical("elseBlock");
		} else {
			elseBlock = (Block) ModuleUtil.createModule(
					Collections.<Component> emptyList(),
					Collections.<Var> emptyList(),
					Collections.<Var> emptyList(), "elseBlock", false,
					Exit.DONE, 1, portDependency, busDependency,
					portGroupDependency, doneBusDependency);
			elseBlock.setIDLogical("elseBlock");
			elseBlock.setNonRemovable();
		}
		// Get All input Vars
		List<Var> ifInputVars = branchIO.getInputs();
		List<Var> ifOutputVars = branchIO.getOutputs();

		// Get Phi target Vars, aka branchIf Outputs
		Map<Var, List<Var>> phiOuts = branchIO.getPhi();
		Branch branch = (Branch) ModuleUtil.createBranch(decision, thenBlock,
				elseBlock, ifInputVars, ifOutputVars, phiOuts, "ifBLOCK",
				Exit.DONE, portDependency, busDependency, portGroupDependency,
				doneBusDependency);
		branch.setNonRemovable();
		if (STM_DEBUG) {
			System.out.println("Branch: line :" + blockIf.getLineNumber()
					+ " Name: " + procedure.getName());
			System.out.println("Branch: thenBlock :" + thenBlock.toString());
			System.out.println("Branch: elseBlock :" + elseBlock.toString());
			System.out.println("Branch: Branch :" + branch.toString() + "\n");
		}
		IDSourceInfo sinfo = new IDSourceInfo(procedure.getName(),
				blockIf.getLineNumber());

		branch.setIDSourceInfo(sinfo);

		currentComponent = branch;
		componentList = new ArrayList<Component>();
		componentList.addAll(oldComponents);
		if (blockIf.getAttribute("isMutex") != null) {
			Module mutexFsmBlock = (Module) ModuleUtil.createModule(
					Arrays.asList((Component) branch), ifInputVars,
					ifOutputVars, "mutex_BranchBlock", true, Exit.DONE, 0,
					portDependency, busDependency, portGroupDependency,
					doneBusDependency);
			componentList.add(mutexFsmBlock);
		} else {
			componentList.add(currentComponent);
		}
		return null;
	}

	public List<Component> caseBlockMutex(BlockMutex blockMutex) {
		MutexIO mutexIO = new MutexIO(blockMutex);

		List<Component> oldComponents = new ArrayList<Component>(componentList);
		componentList = new ArrayList<Component>();

		doSwitch(blockMutex.getBlocks());

		List<Var> inputs = mutexIO.getInputs();

		List<Var> outputs = mutexIO.getOutputs();

		Module mutexModule = (Module) ModuleUtil.createModule(componentList,
				inputs, outputs, "mutex_BranchBlock", true, Exit.DONE, 0,
				portDependency, busDependency, portGroupDependency,
				doneBusDependency);

		componentList = new ArrayList<Component>();
		/** Put back all previous components and add the loop **/
		componentList.addAll(oldComponents);
		componentList.add(mutexModule);
		return null;
	}

	@Override
	public List<Component> caseBlockWhile(BlockWhile blockWhile) {
		LoopIO loopIO = new LoopIO(blockWhile);
		List<Component> oldComponents = new ArrayList<Component>(componentList);
		componentList = new ArrayList<Component>();

		/** Get Decision Components **/
		doSwitch(blockWhile.getJoinBlock());
		Var decisionVar = loopIO.getDecision();

		Component decisionComponent = ModuleUtil.findDecisionComponent(
				componentList, decisionVar, busDependency);

		if (decisionComponent == null) {
			Var target = IrFactory.eINSTANCE.createVar(decisionVar.getType(),
					"decisionVar_" + decisionVar.getIndexedName(), true,
					decisionVar.getIndex());
			decisionComponent = ModuleUtil.assignComponent(target, decisionVar,
					portDependency, busDependency, portGroupDependency,
					doneBusDependency);
			componentList.add(decisionComponent);
		}

		List<Component> decisionBodyComponents = new ArrayList<Component>(
				componentList);

		componentList = new ArrayList<Component>();

		/** Get Loop Body Components **/
		doSwitch(blockWhile.getBlocks());
		List<Component> bodyComponents = new ArrayList<Component>(componentList);

		/** Create the Loop **/
		List<Var> decisionInVars = loopIO.getDecisionInputs();
		List<Var> loopInVars = loopIO.getInputs();
		List<Var> loopOutVars = loopIO.getOutputs();
		List<Var> loopBodyInVars = loopIO.getBodyInputs();
		List<Var> loopBodyOutVars = loopIO.getBodyOutputs();

		Map<Var, List<Var>> loopPhi = loopIO.getPhi();

		Loop loop = (Loop) ModuleUtil.createLoop(decisionComponent,
				decisionBodyComponents, decisionInVars, bodyComponents,
				loopPhi, loopInVars, loopOutVars, loopBodyInVars,
				loopBodyOutVars, portDependency, busDependency,
				portGroupDependency, doneBusDependency);
		loop.setNonRemovable();
		if (STM_DEBUG) {
			System.out.println("Loop: line :" + blockWhile.getLineNumber()
					+ " Name: " + procedure.getName());
			System.out.println("Loop: LoopBody: " + loop.getBody().toString());
			System.out.println("Loop: " + loop.toString() + "\n");
		}

		IDSourceInfo sinfo = new IDSourceInfo(procedure.getName(),
				blockWhile.getLineNumber());
		loop.setIDSourceInfo(sinfo);

		/** Clean componentList **/
		componentList = new ArrayList<Component>();
		/** Put back all previous components and add the loop **/
		componentList.addAll(oldComponents);
		componentList.add(loop);
		return null;
	}

	@Override
	public List<Component> caseExprBinary(ExprBinary expr) {
		// Get the size of the target and give it to the component
		int sizeInBits = assignTarget.getVariable().getType().getSizeInBits();
		// Get the Variables
		Var e1 = ((ExprVar) expr.getE1()).getUse().getVariable();
		Var e2 = ((ExprVar) expr.getE2()).getUse().getVariable();
		List<Var> inVars = new ArrayList<Var>();
		inVars.add(e1);
		inVars.add(e2);
		// Component component = null;
		if (expr.getOp() == OpBinary.BITAND) {
			currentComponent = new AndOp();
		} else if (expr.getOp() == OpBinary.BITOR) {
			currentComponent = new OrOp();
		} else if (expr.getOp() == OpBinary.BITXOR) {
			currentComponent = new XorOp();
		} else if (expr.getOp() == OpBinary.DIV) {
			currentComponent = new DivideOp(sizeInBits);
		} else if (expr.getOp() == OpBinary.DIV_INT) {
			currentComponent = new DivideOp(sizeInBits);
		} else if (expr.getOp() == OpBinary.EQ) {
			currentComponent = new EqualsOp();
		} else if (expr.getOp() == OpBinary.GE) {
			currentComponent = new GreaterThanEqualToOp();
		} else if (expr.getOp() == OpBinary.GT) {
			currentComponent = new GreaterThanOp();
		} else if (expr.getOp() == OpBinary.LE) {
			currentComponent = new LessThanEqualToOp();
		} else if (expr.getOp() == OpBinary.LOGIC_AND) {
			currentComponent = new And(2);
		} else if (expr.getOp() == OpBinary.LOGIC_OR) {
			currentComponent = new Or(2);
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
			int log2N = MathStuff.log2(sizeInBits);
			currentComponent = new LeftShiftOp(log2N);
		} else if (expr.getOp() == OpBinary.SHIFT_RIGHT) {
			int log2N = MathStuff.log2(sizeInBits);
			currentComponent = new RightShiftOp(log2N);
		} else if (expr.getOp() == OpBinary.TIMES) {
			currentComponent = new MultiplyOp(expr.getType().getSizeInBits());
		}
		currentComponent.setNonRemovable();
		PortUtil.mapInDataPorts(currentComponent, inVars, portDependency,
				portGroupDependency);
		return null;
	}

	@Override
	public List<Component> caseExprBool(ExprBool expr) {
		final long value = expr.isValue() ? 1 : 0;
		currentComponent = new SimpleConstant(value, 1, true);
		currentComponent.setNonRemovable();
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
		currentComponent.setNonRemovable();
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
		currentComponent.setNonRemovable();
		return null;
	}

	@Override
	public List<Component> caseExprVar(ExprVar var) {
		Component noop = new NoOp(1, Exit.DONE);
		Var useVar = var.getUse().getVariable();
		Var inVar = useVar;
		/** Cast if necessary **/
		Integer newMaxSize = assignTarget.getVariable().getType()
				.getSizeInBits();
		if (newMaxSize != var.getType().getSizeInBits()) {
			Boolean isSigned = assignTarget.getVariable().getType().isInt();
			Var castedVar = unaryCastOp(var.getUse().getVariable(), newMaxSize,
					isSigned);
			inVar = castedVar;
		}
		PortUtil.mapInDataPorts(noop, inVar, portDependency,
				portGroupDependency);
		currentComponent = noop;
		currentComponent.setNonRemovable();
		return null;
	}

	@Override
	public List<Component> caseInstAssign(InstAssign assign) {
		// Get the size in bits of the target
		assignTarget = assign.getTarget();
		super.caseInstAssign(assign);
		if (currentComponent != null) {
			IDSourceInfo sinfo = new IDSourceInfo(procedure.getName(),
					assign.getLineNumber());
			currentComponent.setIDSourceInfo(sinfo);

			Var outVar = assign.getTarget().getVariable();
			PortUtil.mapOutDataPorts(currentComponent, outVar, busDependency,
					doneBusDependency);

			componentList.add(currentComponent);
		}
		// Put target to null
		assignTarget = null;
		return null;
	}

	@Override
	public List<Component> caseInstCall(InstCall call) {
		Procedure procedure = call.getProcedure();
		// Test if the container is an Action, visit
		if (procedure.eContainer() instanceof Action) {
			Action action = (Action) procedure.eContainer();
			if (procedure == action.getScheduler()) {
				doSwitch(procedure);
			} else if (procedure == action.getBody()) {
				TaskCall taskCall = new TaskCall();
				taskCall.setTarget(resources.getTaskFromAction(action));
				PortUtil.mapOutControlPort(taskCall, 0, doneBusDependency);
				IDSourceInfo sinfo = new IDSourceInfo(procedure.getName(),
						call.getLineNumber());
				taskCall.setIDSourceInfo(sinfo);
				taskCall.setNonRemovable();
				componentList.add(taskCall);
			}
		}

		return null;
	}

	public List<Component> caseInstCast(InstCast cast) {
		Var target = cast.getTarget().getVariable();
		Var source = cast.getSource().getVariable();
		Integer castedSize = target.getType().getSizeInBits();

		Component castOp = new CastOp(castedSize, target.getType().isInt());
		castOp.setNonRemovable();
		PortUtil.mapInDataPorts(castOp, source, portDependency,
				portGroupDependency);
		PortUtil.mapOutDataPorts(castOp, target, busDependency,
				doneBusDependency);
		componentList.add(castOp);
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

		if (sourceVar.getType().isList()) {
			TypeList typeList = (TypeList) sourceVar.getType();
			Type type = typeList.getInnermostType();

			Boolean isSigned = type.isInt();
			Location targetLocation = resources.getLocation(sourceVar);

			LogicalMemoryPort memPort = targetLocation.getLogicalMemory()
					.getLogicalMemoryPorts().iterator().next();

			AddressStridePolicy addrPolicy = targetLocation.getAbsoluteBase()
					.getInitialValue().getAddressStridePolicy();

			int dataSize = type.getSizeInBits();
			HeapRead read = new HeapRead(dataSize / addrPolicy.getStride(), 32,
					0, isSigned, addrPolicy);
			read.setNonRemovable();
			CastOp castOp = new CastOp(dataSize, isSigned);
			castOp.setNonRemovable();
			Block block = DesignUtil.buildAddressedBlock(read, targetLocation,
					Collections.singletonList((Component) castOp));
			block.setNonRemovable();
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

			PortUtil.mapInDataPorts(block, loadIndexVar, portDependency,
					portGroupDependency);

			// Check if the load target should be casted

			Var target = load.getTarget().getVariable();
			int targetSize = target.getType().getSizeInBits();

			if (targetSize > dataSize) {
				// NOTE: OpenForge does not accept a cast here
				if (target.getType().isInt()) {
					target.setType(IrFactory.eINSTANCE.createTypeInt(dataSize));
				} else if (target.getType().isUint()) {
					target.setType(IrFactory.eINSTANCE.createTypeUint(dataSize));
				}
			}

			PortUtil.mapOutDataPorts(block, target, busDependency,
					doneBusDependency);
			IDSourceInfo sinfo = new IDSourceInfo(procedure.getName(),
					load.getLineNumber());
			block.setIDSourceInfo(sinfo);
			componentList.add(block);
		} else {
			Var target = load.getTarget().getVariable();
			Component absoluteMemoryRead = ModuleUtil.absoluteMemoryRead(
					sourceVar, target, resources, busDependency,
					doneBusDependency);
			componentList.add(absoluteMemoryRead);
		}
		return null;
	}

	@Override
	public List<Component> caseInstPhi(InstPhi phi) {
		// Do nothing
		return null;
	}

	public List<Component> caseInstPortPeek(InstPortPeek portPeek) {
		net.sf.orcc.df.Port port = (net.sf.orcc.df.Port) portPeek.getPort();
		Var peekVar = portPeek.getTarget().getVariable();
		ActionIOHandler ioHandler = resources.getIOHandler(port);
		Component pinPeekComponent = ioHandler.getTokenPeekAccess();
		pinPeekComponent.setNonRemovable();
		PortUtil.mapOutDataPorts(pinPeekComponent, peekVar, busDependency,
				doneBusDependency);
		componentList.add(pinPeekComponent);
		return null;
	}

	public List<Component> caseInstPortRead(InstPortRead portRead) {
		net.sf.orcc.df.Port port = (net.sf.orcc.df.Port) portRead.getPort();
		ActionIOHandler ioHandler = resources.getIOHandler(port);
		Component pinRead = ioHandler.getReadAccess(false);
		pinRead.setNonRemovable();

		Var pinReadVar = portRead.getTarget().getVariable();
		PortUtil.mapOutDataPorts(pinRead, pinReadVar, busDependency,
				doneBusDependency);
		componentList.add(pinRead);
		return null;
	}

	public List<Component> caseInstPortStatus(InstPortStatus portStatus) {
		net.sf.orcc.df.Port port = (net.sf.orcc.df.Port) portStatus.getPort();
		Var statusVar = portStatus.getTarget().getVariable();

		ActionIOHandler ioHandler = resources.getIOHandler(port);
		Component pinStatusComponent = ioHandler.getStatusAccess();
		pinStatusComponent.setNonRemovable();
		PortUtil.mapOutDataPorts(pinStatusComponent, statusVar, busDependency,
				doneBusDependency);
		componentList.add(pinStatusComponent);
		return null;
	}

	public List<Component> caseInstPortWrite(InstPortWrite portWrite) {
		net.sf.orcc.df.Port port = (net.sf.orcc.df.Port) portWrite.getPort();
		ActionIOHandler ioHandler = resources.getIOHandler(port);
		// Boolean blocking = portWrite.isBlocking();
		Component pinWrite = ioHandler.getWriteAccess(false);
		pinWrite.setNonRemovable();

		ExprVar value = (ExprVar) portWrite.getValue();
		Var pinWriteVar = value.getUse().getVariable();

		PortUtil.mapInDataPorts(pinWrite, pinWriteVar, portDependency,
				portGroupDependency);
		PortUtil.mapOutControlPort(pinWrite, 0, doneBusDependency);
		componentList.add(pinWrite);
		return null;
	}

	@Override
	public List<Component> caseInstReturn(InstReturn returnInstr) {
		// Do nothing
		return null;
	}

	@Override
	public List<Component> caseInstStore(InstStore store) {
		Var targetVar = store.getTarget().getVariable();

		if (targetVar.isGlobal()) {

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
			Type type = null;
			if (targetVar.getType().isList()) {
				TypeList typeList = (TypeList) targetVar.getType();
				type = typeList.getInnermostType();

				// Get Location form resources
				Location targetLocation = resources.getLocation(targetVar);
				LogicalMemoryPort memPort = targetLocation.getLogicalMemory()
						.getLogicalMemoryPorts().iterator().next();
				AddressStridePolicy addrPolicy = targetLocation
						.getAbsoluteBase().getInitialValue()
						.getAddressStridePolicy();

				int dataSize = type.getSizeInBits();
				Boolean isSigned = type.isInt();

				HeapWrite heapWrite = new HeapWrite(dataSize
						/ addrPolicy.getStride(), 32, // max address width
						0, // fixed offset
						isSigned, // is signed?
						addrPolicy); // addressing policy
				heapWrite.setNonRemovable();
				Block block = DesignUtil.buildAddressedBlock(heapWrite,
						targetLocation, Collections.<Component> emptyList());
				block.setNonRemovable();
				Port data = block.makeDataPort();
				heapWrite
						.getEntries()
						.get(0)
						.addDependency(heapWrite.getValuePort(),
								new DataDependency(data.getPeer()));

				memPort.addAccess(heapWrite, targetLocation);

				IDSourceInfo sinfo = new IDSourceInfo(procedure.getName(),
						store.getLineNumber());
				block.setIDSourceInfo(sinfo);

				currentComponent = block;
				List<Var> inVars = new ArrayList<Var>();
				inVars.add(storeIndexVar);
				inVars.add(valueVar);
				PortUtil.mapInDataPorts(currentComponent, inVars,
						portDependency, portGroupDependency);
				PortUtil.mapOutControlPort(currentComponent, 0,
						doneBusDependency);
				componentList.add(currentComponent);
			} else {
				type = targetVar.getType();
				Component absoluteMemoryWrite = ModuleUtil.absoluteMemoryWrite(
						targetVar, valueVar, resources, portDependency,
						portGroupDependency, doneBusDependency);
				absoluteMemoryWrite.setNonRemovable();
				componentList.add(absoluteMemoryWrite);
			}
		} else {
			if (store.getValue().isExprVar()) {
				Var sourceVar = ((ExprVar) store.getValue()).getUse()
						.getVariable();
				InstAssign assign = IrFactory.eINSTANCE.createInstAssign(
						targetVar, sourceVar);
				doSwitch(assign);
			}
		}
		return null;
	}

	@Override
	public List<Component> caseProcedure(Procedure procedure) {
		// componentList = new ArrayList<Component>();
		super.caseProcedure(procedure);
		return componentList;
	}

	@Override
	public List<Component> defaultCase(EObject object) {
		if (object instanceof BlockMutex) {
			return caseBlockMutex((BlockMutex) object);
		} else if (object instanceof InstPortPeek) {
			return caseInstPortPeek((InstPortPeek) object);
		} else if (object instanceof InstPortRead) {
			return caseInstPortRead((InstPortRead) object);
		} else if (object instanceof InstPortStatus) {
			return caseInstPortStatus((InstPortStatus) object);
		} else if (object instanceof InstPortWrite) {
			return caseInstPortWrite((InstPortWrite) object);
		} else if (object instanceof InstCast) {
			return caseInstCast((InstCast) object);
		}

		return super.defaultCase(object);
	}

	protected Var unaryCastOp(Var var, Integer newMaxSize, Boolean isSigned) {
		Var newVar = var;
		Integer sizeVar = var.getType().getSizeInBits();

		if (sizeVar != newMaxSize) {
			Component castOp = new CastOp(newMaxSize, isSigned);
			castOp.setNonRemovable();
			PortUtil.mapInDataPorts(castOp, var, portDependency,
					portGroupDependency);
			Var castedVar = procedure.newTempLocalVariable(
					IrFactory.eINSTANCE.createTypeInt(newMaxSize), "casted_"
							+ castIndex + "_" + var.getIndexedName());

			PortUtil.mapOutDataPorts(castOp, castedVar, busDependency,
					doneBusDependency);
			componentList.add(castOp);
			newVar = castedVar;
			castIndex++;
		}

		return newVar;
	}

}
