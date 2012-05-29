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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import net.sf.openforge.frontend.slim.builder.ActionIOHandler;
import net.sf.openforge.frontend.slim.builder.ActionIOHandler.FifoIOHandler;
import net.sf.openforge.frontend.slim.builder.ActionIOHandler.NativeIOHandler;
import net.sf.openforge.frontend.slim.builder.SLIMConstants;
import net.sf.openforge.frontend.slim.builder.XModuleFactory;
import net.sf.openforge.lim.And;
import net.sf.openforge.lim.Block;
import net.sf.openforge.lim.Branch;
import net.sf.openforge.lim.Bus;
import net.sf.openforge.lim.Call;
import net.sf.openforge.lim.ClockDependency;
import net.sf.openforge.lim.Component;
import net.sf.openforge.lim.ControlDependency;
import net.sf.openforge.lim.DataDependency;
import net.sf.openforge.lim.Decision;
import net.sf.openforge.lim.Dependency;
import net.sf.openforge.lim.Design;
import net.sf.openforge.lim.Entry;
import net.sf.openforge.lim.Exit;
import net.sf.openforge.lim.HeapRead;
import net.sf.openforge.lim.HeapWrite;
import net.sf.openforge.lim.InBuf;
import net.sf.openforge.lim.Loop;
import net.sf.openforge.lim.LoopBody;
import net.sf.openforge.lim.Module;
import net.sf.openforge.lim.OffsetMemoryAccess;
import net.sf.openforge.lim.Or;
import net.sf.openforge.lim.OutBuf;
import net.sf.openforge.lim.Port;
import net.sf.openforge.lim.ResetDependency;
import net.sf.openforge.lim.Task;
import net.sf.openforge.lim.TaskCall;
import net.sf.openforge.lim.memory.AddressStridePolicy;
import net.sf.openforge.lim.memory.AddressableUnit;
import net.sf.openforge.lim.memory.Allocation;
import net.sf.openforge.lim.memory.Location;
import net.sf.openforge.lim.memory.LocationConstant;
import net.sf.openforge.lim.memory.LogicalMemory;
import net.sf.openforge.lim.memory.LogicalMemoryPort;
import net.sf.openforge.lim.memory.LogicalValue;
import net.sf.openforge.lim.memory.Record;
import net.sf.openforge.lim.memory.Scalar;
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
import net.sf.openforge.util.naming.ID;
import net.sf.openforge.util.naming.IDSourceInfo;
import net.sf.orcc.df.Action;
import net.sf.orcc.df.Actor;
import net.sf.orcc.df.Instance;
import net.sf.orcc.df.util.DfVisitor;
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
import net.sf.orcc.ir.util.ValueUtil;

import org.eclipse.emf.common.util.EList;

/**
 * The DesignActorVisitor class transforms an Orcc Actor to an OpenForge Design
 * 
 * @author Endri Bezati
 * 
 */
public class DesignActorVisitor extends DfVisitor<Object> {

	protected class InnerIrVisitor extends AbstractIrVisitor<Object> {

		private Def assignTarget;
		private Integer castIndex = 0;
		private Integer listIndexes = 0;

		public InnerIrVisitor() {
			super(true);
		}

		@Override
		public Object caseBlockIf(BlockIf blockIf) {
			// Create the decision
			Var condVar = resources.getBranchDecision(blockIf);
			String condName = "decision_" + currentAction.getName() + "_"
					+ condVar.getIndexedName();
			Decision decision = buildDecision(condVar, condName);

			// Visit the then block
			currentListComponent = new ArrayList<Component>();
			doSwitch(blockIf.getThenBlocks());
			// Get the then Input Vars
			List<Var> thenInputs = resources.getBranchThenVars(blockIf);
			Map<Var, List<Var>> thenOutputs = groupZeroOutput(resources
					.getBranchThenOutputVars(blockIf));
			Block thenBlock = (Block) buildModule(currentListComponent,
					thenInputs, thenOutputs, "thenBlock", Exit.DONE, 0);

			// Visit the else block
			Block elseBlock = null;
			List<Var> elseInputs = new ArrayList<Var>();
			if (!blockIf.getElseBlocks().isEmpty()) {
				currentListComponent = new ArrayList<Component>();
				doSwitch(blockIf.getElseBlocks());
			} else {
				elseBlock = (Block) buildModule(
						Collections.<Component> emptyList(),
						Collections.<Var> emptyList(),
						Collections.<Var, List<Var>> emptyMap(), "elseBlock",
						Exit.DONE, 1);
			}
			// Get All input Vars
			List<Var> ifInputVars = new ArrayList<Var>();
			ifInputVars.add(condVar);
			ifInputVars.addAll(thenInputs);
			ifInputVars.addAll(elseInputs);

			// Get Phi target Vars, aka branchIf Outputs
			Map<Var, List<Var>> phiOuts = resources.getBranchPhiVars(blockIf);
			currentComponent = buildBranch(decision, thenBlock, elseBlock,
					ifInputVars, phiOuts, "ifBLOCK", Exit.DONE);
			return null;
		}

		@Override
		public Object caseBlockWhile(BlockWhile blockWhile) {
			return null;
		}

		@Override
		public Object caseExprBinary(ExprBinary expr) {
			// Get the size of the target and give it to the component
			int sizeInBits = assignTarget.getVariable().getType()
					.getSizeInBits();
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
			mapInPorts(binaryCastOp(e1, e2, newMaxSize), component);
			currentComponent = component;
			return null;
		}

		protected List<Var> binaryCastOp(Var e1, Var e2, Integer newMaxSize) {
			Boolean isSigned = e1.getType().isInt() || e2.getType().isInt();
			List<Var> newVars = new ArrayList<Var>();
			// Add the new Casted variables
			newVars.add(unaryCastOp(e1, newMaxSize, isSigned));
			newVars.add(unaryCastOp(e2, newMaxSize, isSigned));
			return newVars;
		}

		protected Var unaryCastOp(Var var, Integer newMaxSize, Boolean isSigned) {
			Var newVar = var;
			Integer sizeVar = var.getType().getSizeInBits();

			if (sizeVar != newMaxSize) {
				currentComponent = new CastOp(newMaxSize, isSigned);
				mapInPorts(new ArrayList<Var>(Arrays.asList(var)),
						currentComponent);
				Var castedVar = procedure.newTempLocalVariable(
						IrFactory.eINSTANCE.createTypeInt(newMaxSize),
						"casted_" + castIndex + "_" + var.getIndexedName());
				mapOutPorts(castedVar, 0);
				currentListComponent.add(currentComponent);
				newVar = castedVar;
				castIndex++;
			}

			return newVar;
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
			int sizeInBits = 32;

			if (assignTarget != null) {
				sizeInBits = assignTarget.getVariable().getType()
						.getSizeInBits();
			} else {
				sizeInBits = expr.getType().getSizeInBits();
			}

			currentComponent = new SimpleConstant(value, sizeInBits, expr
					.getType().isInt());
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
			currentComponent = new NoOp(1, Exit.DONE);
			mapInPorts(
					new ArrayList<Var>(
							Arrays.asList(var.getUse().getVariable())),
					currentComponent);
			return null;
		}

		@Override
		public Object caseInstAssign(InstAssign assign) {
			// Get the size in bits of the target
			assignTarget = assign.getTarget();
			super.caseInstAssign(assign);
			if (currentComponent != null) {
				currentListComponent.add(currentComponent);
				mapOutPorts(assign.getTarget().getVariable(), 0);
			}
			// Put target to null
			assignTarget = null;
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
			HeapRead read = new HeapRead(dataSize / addrPolicy.getStride(), 32,
					0, isSigned, addrPolicy);
			CastOp castOp = new CastOp(dataSize, isSigned);
			Block block = buildAddressedBlock(read, targetLocation,
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
			mapInPorts(new ArrayList<Var>(Arrays.asList(loadIndexVar)),
					currentComponent);
			// For the moment all indexes are considered 32bits
			Var castedIndexVar = procedure.newTempLocalVariable(
					IrFactory.eINSTANCE.createTypeInt(32), "casted_"
							+ castIndex + "_" + loadIndexVar.getIndexedName());
			mapOutPorts(castedIndexVar, 0);
			currentListComponent.add(currentComponent);
			castIndex++;
			// add the assign instruction for each index
			InstAssign assign = IrFactory.eINSTANCE.createInstAssign(indexVar,
					castedIndexVar);
			doSwitch(assign);

			currentComponent = block;
			mapInPorts(new ArrayList<Var>(Arrays.asList(indexVar)),
					currentComponent);
			mapOutPorts(load.getTarget().getVariable(), 0);

			currentListComponent.add(currentComponent);

			return null;
		}

		@Override
		public Object caseInstStore(InstStore store) {
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

			HeapWrite heapWrite = new HeapWrite(dataSize
					/ addrPolicy.getStride(), 32, // max address width
					0, // fixed offset
					isSigned, // is signed?
					addrPolicy); // addressing policy

			Block block = buildAddressedBlock(heapWrite, targetLocation,
					Collections.<Component> emptyList());
			Port data = block.makeDataPort();
			heapWrite
					.getEntries()
					.get(0)
					.addDependency(heapWrite.getValuePort(),
							new DataDependency(data.getPeer()));

			memPort.addAccess(heapWrite, targetLocation);

			Var indexVar = procedure.newTempLocalVariable(
					IrFactory.eINSTANCE.createTypeInt(32), "index"
							+ listIndexes);
			listIndexes++;
			currentComponent = new CastOp(32, isSigned);
			mapInPorts(new ArrayList<Var>(Arrays.asList(storeIndexVar)),
					currentComponent);
			Var castedIndexVar = procedure.newTempLocalVariable(
					IrFactory.eINSTANCE.createTypeInt(32), "casted_"
							+ castIndex + "_" + storeIndexVar.getIndexedName());
			mapOutPorts(castedIndexVar, 0);
			currentListComponent.add(currentComponent);
			castIndex++;
			// add the assign instruction for each index
			InstAssign assign = IrFactory.eINSTANCE.createInstAssign(indexVar,
					castedIndexVar);
			doSwitch(assign);

			currentComponent = block;
			mapInPorts(new ArrayList<Var>(Arrays.asList(indexVar, valueVar)),
					currentComponent);
			mapOutControlPort(currentComponent, 0);
			currentListComponent.add(currentComponent);

			return null;
		}

		@Override
		public Object caseVar(Var var) {
			if (var.isGlobal()) {
				stateVars.put(makeLogicalValue(var), var);
			}
			return null;
		}

		private Block buildAddressedBlock(OffsetMemoryAccess memAccess,
				Location targetLocation, List<Component> otherComps) {
			final LocationConstant locationConst = new LocationConstant(
					targetLocation, SLIMConstants.MAX_ADDR_WIDTH,
					targetLocation.getAbsoluteBase().getLogicalMemory()
							.getAddressStridePolicy());
			final AddOp adder = new AddOp();
			final CastOp cast = new CastOp(SLIMConstants.MAX_ADDR_WIDTH, false);

			final Block block = new Block(false);
			final Exit done = block.makeExit(0, Exit.DONE);
			final List<Component> comps = new ArrayList<Component>();
			comps.add(locationConst);
			comps.add(cast);
			comps.add(adder);
			comps.add(memAccess);
			comps.addAll(otherComps);
			XModuleFactory.populateModule(block, comps);
			final Port index = block.makeDataPort();

			// Now build the dependencies
			cast.getEntries()
					.get(0)
					.addDependency(cast.getDataPort(),
							new DataDependency(index.getPeer()));
			adder.getEntries()
					.get(0)
					.addDependency(adder.getLeftDataPort(),
							new DataDependency(locationConst.getValueBus()));
			adder.getEntries()
					.get(0)
					.addDependency(adder.getRightDataPort(),
							new DataDependency(cast.getResultBus()));

			memAccess
					.getEntries()
					.get(0)
					.addDependency(memAccess.getBaseAddressPort(),
							new DataDependency(adder.getResultBus()));

			done.getPeer()
					.getEntries()
					.get(0)
					.addDependency(
							done.getDoneBus().getPeer(),
							new ControlDependency(memAccess.getExit(Exit.DONE)
									.getDoneBus()));

			return block;
		}

		private Map<Var, List<Var>> groupZeroOutput(List<Var> vars) {
			Map<Var, List<Var>> outputs = new HashMap<Var, List<Var>>();
			for (Var var : vars) {
				List<Var> emptyList = Collections.<Var> emptyList();
				outputs.put(var, emptyList);
			}
			return outputs;
		}

	}

	/** Map of action associated to its Task **/
	private final Map<Action, Task> actorsTasks = new HashMap<Action, Task>();

	/** Dependency between Components and Bus-Var **/
	protected Map<Component, Map<Bus, List<Var>>> componentBusDependency = new HashMap<Component, Map<Bus, List<Var>>>();

	/** Action component Counter **/
	protected Integer componentCounter;

	/** Dependency between Components and Done Bus **/
	protected Map<Component, Map<Bus, Integer>> componentDoneBusDependency = new HashMap<Component, Map<Bus, Integer>>();

	/** Dependency between Components and Port-Var **/
	protected Map<Component, Map<Port, Var>> componentPortDependency = new HashMap<Component, Map<Port, Var>>();

	/** Current visited action **/
	protected Action currentAction = null;

	/** Current Component **/
	protected Component currentComponent = null;

	/** Current Exit **/
	protected Exit.Type currentExit;

	/** Current List Component **/
	protected List<Component> currentListComponent;

	/** The current module which represents the Action **/
	protected Module currentModule;

	/** Design to be build **/
	protected Design design;

	/** Instance **/
	Instance instance;

	/** Dependency between Module Ports and its associated Var **/
	protected Map<Module, Map<Port, Var>> modulePortDependency = new HashMap<Module, Map<Port, Var>>();

	/** Port Cache **/
	protected PortCache portCache = new PortCache();

	/** Design Resources **/
	protected ResourceCache resources;

	/** Design stateVars **/
	protected Map<LogicalValue, Var> stateVars;

	public DesignActorVisitor(Instance instance, Design design,
			ResourceCache resources) {
		this.instance = instance;
		this.design = design;
		this.resources = resources;
		irVisitor = new InnerIrVisitor();
	}

	protected Component buildBranch(Decision decision, Block thenBlock,
			Block elseBlock, List<Var> inVars, Map<Var, List<Var>> outVars,
			String searchScope, Exit.Type exitType) {
		Branch branch = null;
		portCache.publish(decision);
		portCache.publish(thenBlock);
		List<Component> branchComponents = new ArrayList<Component>();
		if (elseBlock == null) {
			branch = new Branch(decision, thenBlock);
			branchComponents.add(decision);
			branchComponents.add(thenBlock);
		} else {
			branch = new Branch(decision, thenBlock, elseBlock);
			branchComponents.add(decision);
			branchComponents.add(thenBlock);
			branchComponents.add(elseBlock);
			portCache.publish(elseBlock);
		}

		createModuleInterface(branch, inVars, outVars, exitType);

		// Map In/Out port of branch, Branch done Group 0
		mapInPorts(inVars, branch);
		mapOutControlPort(branch, 0);

		operationDependencies(branch, branchComponents,
				componentPortDependency, branch.getExit(Exit.DONE));

		// Give the name of the searchScope
		branch.specifySearchScope(searchScope);
		return branch;
	}

	protected Decision buildDecision(Var inputDecision, String resultName) {
		Decision decision = null;
		// Create the decision variable and assign the inputDecision to it
		Type type = IrFactory.eINSTANCE.createTypeBool();
		Var decisionVar = IrFactory.eINSTANCE.createVar(0, type, resultName,
				false, 0);
		InstAssign assign = IrFactory.eINSTANCE.createInstAssign(decisionVar,
				IrFactory.eINSTANCE.createExprVar(inputDecision));
		// Visit assignSched
		doSwitch(assign);

		currentModule = (Module) buildModule(Arrays.asList(currentComponent),
				Arrays.asList(inputDecision),
				Collections.<Var, List<Var>> emptyMap(), "decisionBlock",
				Exit.DONE, 0);

		// Add done dependency, Decision group 0
		mapOutControlPort(currentModule, 0);

		// Create the decision
		decision = new Decision((Block) currentModule, currentComponent);
		// Any data inputs to the decision need to be propagated from
		// the block to the decision. There should be no output ports
		// to propagate. They are inferred true/false.
		propagateInputs(decision, (Block) currentModule);

		// Add to dependency, A Decision has only one Input
		Map<Port, Var> portDep = new HashMap<Port, Var>();
		Port port = decision.getDataPorts().get(0);
		portDep.put(port, inputDecision);
		componentPortDependency.put(decision, portDep);

		// Build option scope
		currentModule.specifySearchScope("moduleDecision");

		// Add done dependency on decision
		// mapOutControlPort(decision);
		return decision;
	}

	protected Component buildModule(List<Component> components,
			List<Var> inVars, Map<Var, List<Var>> outVars, String searchScope,
			Exit.Type exitType, Integer group) {
		// Create an Empty Block
		Module module = new Block(false);

		// Add Input and Output Port for the Module
		createModuleInterface(module, inVars, outVars, exitType);

		// Put all the components to the Module
		populateModule(module, components);

		// Set all the dependencies
		operationDependencies(module, components, componentPortDependency,
				module.getExit(exitType));

		// Give the name of the searchScope
		module.specifySearchScope(searchScope);
		// The condition of emptiness on components means that the DONE exit
		// signal should not be connected with the other dependencies of its
		// container
		if (!components.isEmpty()) {
			if (exitType != Exit.RETURN) {
				// Add done dependency on module, Exit group 0 of a "normal"
				// module
				mapOutControlPort(module, group);
			}
		}
		return module;
	}

	protected Component buildComponentModule(Component component,
			List<Var> inVars, Map<Var, List<Var>> outVars, String searchScope,
			Exit.Type exitType) {
		Module module = (Module) component;
		// Add Input and Output Port for the Module
		createModuleInterface(module, inVars, outVars, exitType);
		// Set all the dependencies
		operationDependencies(module, Arrays.asList(component),
				componentPortDependency, module.getExit(exitType));

		// Give the name of the searchScope
		module.specifySearchScope(searchScope);
		if (exitType != Exit.RETURN) {
			// Add done dependency on module, Exit group 0 of a "normal" module
			mapOutControlPort(module, 0);
		}

		return module;
	}

	@Override
	public Object caseAction(Action action) {
		currentAction = action;
		componentCounter = 0;
		currentListComponent = new ArrayList<Component>();
		// Initialize currentModule and its exit Type

		// Make Action module exit
		currentExit = Exit.RETURN;
		// Get pinRead Operation(s)

		for (net.sf.orcc.df.Port port : action.getInputPattern().getPorts()) {
			makePinReadOperation(port);
			currentListComponent.add(currentComponent);
		}

		// Visit the rest of the action
		super.doSwitch(action.getBody());

		// Get pinWrite Operation(s)
		for (net.sf.orcc.df.Port port : action.getOutputPattern().getPorts()) {
			makePinWriteOperation(port);
			currentListComponent.add(currentComponent);
		}
		// Create the task
		String taskName = currentAction.getName();
		currentModule = (Module) buildModule(currentListComponent,
				Collections.<Var> emptyList(),
				Collections.<Var, List<Var>> emptyMap(), taskName + "Body",
				currentExit, 0);

		Task task = createTask(taskName, currentModule, false);
		actorsTasks.put(action, task);
		// Add it to the design
		design.addTask(task);

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
				instance, design, actorsTasks, resources, stateVars);
		schedulerVisitor.doSwitch(actor);

		for (Task task : design.getTasks()) {
			Call call = task.getCall();
			if (call.getExit(Exit.DONE).getDoneBus().isConnected()) {
				call.getProcedure().getBody().setProducesDone(true);
			}
			if (call.getGoPort().isConnected()) {
				call.getProcedure().getBody().setConsumesGo(true);
			}
		}

		return null;
	}

	protected void componentAddEntry(Component comp, Exit drivingExit,
			Bus clockBus, Bus resetBus, Bus goBus) {

		Entry entry = comp.makeEntry(drivingExit);
		// Even though most components do not use the clock, reset and
		// go ports we set up the dependencies for consistency.
		entry.addDependency(comp.getClockPort(), new ClockDependency(clockBus));
		entry.addDependency(comp.getResetPort(), new ResetDependency(resetBus));
		entry.addDependency(comp.getGoPort(), new ControlDependency(goBus));
	}

	protected Call createCall(String name, Module module) {
		Block procedureBlock = (Block) module;
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

	protected void createModuleInterface(Module module, List<Var> inVars,
			Map<Var, List<Var>> outVars, Exit.Type exitType) {
		if (!inVars.isEmpty()) {
			Map<Port, Var> portDep = new HashMap<Port, Var>();
			for (Var var : inVars) {
				Port port = module.makeDataPort();
				port.setIDLogical(var.getIndexedName());
				portDep.put(port, var);
			}
			modulePortDependency.put(module, portDep);
			componentPortDependency.put(module, portDep);
		}

		// Create modules exit
		if (module.getExit(Exit.DONE) == null) {
			if ((exitType != null) && (exitType != Exit.DONE)) {
				module.makeExit(0, exitType);
			} else {
				module.makeExit(0);
			}
		}

		if (!outVars.isEmpty()) {
			Exit exit = module.getExit(Exit.DONE);
			Map<Bus, List<Var>> busDep = new HashMap<Bus, List<Var>>();
			for (Var var : outVars.keySet()) {
				Bus dataBus = exit.makeDataBus();
				Integer busSize = var.getType().getSizeInBits();
				boolean isSigned = var.getType().isInt()
						|| var.getType().isBool();
				dataBus.setSize(busSize, isSigned);
				dataBus.setIDLogical(var.getIndexedName());
				busDep.put(dataBus, outVars.get(var));
			}
			componentBusDependency.put(module, busDep);
		}
	}

	protected Task createTask(String taskName, Module taskModule,
			Boolean requiresKicker) {
		Task task = null;

		// create Call
		Call call = createCall(taskName, taskModule);
		topLevelInit(call);
		// Create task
		task = new Task(call);
		task.setKickerRequired(requiresKicker);
		task.setSourceName(taskName);
		return task;
	}

	protected Boolean dependencyOnModulePort(Module module, Var var) {
		Map<Port, Var> portVar = modulePortDependency.get(module);
		if (portVar != null) {
			for (Port port : portVar.keySet()) {
				if (portVar.get(port) == var) {
					return true;
				}
			}
		}
		return false;
	}

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

	protected Bus getModuleComponentPortPeer(Module module, Var var, int group) {
		for (Component component : module.getComponents()) {
			Map<Bus, List<Var>> portVar = componentBusDependency.get(component);
			if (portVar != null) {
				for (Bus bus : portVar.keySet()) {
					if (var == portVar.get(bus).get(group)) {
						return bus;
					}
				}
			}
		}
		return null;
	}

	protected Port getModulePort(Component component, Var var) {
		Map<Port, Var> portVar = componentPortDependency.get(component);
		for (Port port : portVar.keySet()) {
			if (var == portVar.get(port)) {
				return port;
			}
		}
		return null;
	}

	protected Bus getModulePortPeer(Component component, Var var) {
		Map<Port, Var> portVar = modulePortDependency.get(component);
		for (Port port : portVar.keySet()) {
			if (var == portVar.get(port)) {
				return port.getPeer();
			}
		}
		return null;
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
			Integer firstDim = dimension.get(0);
			newListDimension.remove(0);
			for (int i = 0; i < firstDim; i++) {
				subElements.add(makeLogicalValueObject(Array.get(obj, i),
						newListDimension, type));
			}

			logicalValue = new Record(subElements);
		} else {
			if (dimension.get(0).equals(1)) {
				BigInteger value = BigInteger.valueOf(0);
				if (type.isBool()) {
					int boolValue = ((Boolean) ValueUtil.get(type, obj, 0)) ? 1
							: 0;
					value = BigInteger.valueOf(boolValue);
				} else {
					value = (BigInteger) ValueUtil.get(type, obj, 0);
				}
				String valueString = value.toString();
				logicalValue = makeLogicalValue(valueString, type);
			} else {
				List<LogicalValue> subElements = new ArrayList<LogicalValue>(
						dimension.get(0));
				for (int i = 0; i < dimension.get(0); i++) {
					BigInteger value = BigInteger.valueOf(0);
					if (type.isBool()) {
						int boolValue = ((Boolean) ValueUtil.get(type, obj, i)) ? 1
								: 0;
						value = BigInteger.valueOf(boolValue);
					} else {
						value = (BigInteger) ValueUtil.get(type, obj, i);
					}

					String valueString = value.toString();
					subElements.add(makeLogicalValue(valueString, type));
				}
				logicalValue = new Record(subElements);
			}

		}

		return logicalValue;
	}

	private void makePinReadOperation(net.sf.orcc.df.Port port) {
		ActionIOHandler ioHandler = resources.getIOHandler(port);
		currentComponent = ioHandler.getReadAccess();
		setAttributes(
				"pinRead_" + port.getName() + "_"
						+ Integer.toString(componentCounter), currentComponent);
		Var pinReadVar = currentAction.getInputPattern().getPortToVarMap()
				.get(port);
		Map<Bus, List<Var>> busDep = new HashMap<Bus, List<Var>>();
		for (Bus dataBus : currentComponent.getExit(Exit.DONE).getDataBuses()) {
			if (dataBus.getValue() == null) {
				dataBus.setSize(port.getType().getSizeInBits(), port.getType()
						.isInt() || port.getType().isBool());
				dataBus.setIDLogical(pinReadVar.getIndexedName());
			}
			busDep.put(dataBus, Arrays.asList(pinReadVar));
			componentBusDependency.put(currentComponent, busDep);
		}
		// Map out Bus , pinRead group 0
		mapOutPorts(pinReadVar, 0);
		componentCounter++;
	}

	private void makePinWriteOperation(net.sf.orcc.df.Port port) {
		ActionIOHandler ioHandler = resources.getIOHandler(port);
		currentComponent = ioHandler.getWriteAccess();
		setAttributes(
				"pinWrite_" + port.getName() + "_"
						+ Integer.toString(componentCounter), currentComponent);
		Var pinWriteVar = currentAction.getOutputPattern().getPortToVarMap()
				.get(port);

		mapInPorts(new ArrayList<Var>(Arrays.asList(pinWriteVar)),
				currentComponent);
		// Add done dependency for this operation to the current module exit,
		// pinWrite Group 0
		mapOutControlPort(currentComponent, 0);
		componentCounter++;
	}

	protected void mapInPorts(List<Var> inVars, Component component) {
		Iterator<Port> portIter = component.getDataPorts().iterator();
		Map<Port, Var> portDep = new HashMap<Port, Var>();
		for (Var var : inVars) {
			Port dataPort = portIter.next();
			dataPort.setIDLogical(var.getIndexedName());
			dataPort.setSize(var.getType().getSizeInBits(), var.getType()
					.isInt() || var.getType().isBool());
			portDep.put(dataPort, var);
		}

		// Put Input dependency
		componentPortDependency.put(component, portDep);
	}

	protected void mapInPorts(List<Var> inVars, Component component,
			String prefix) {
		List<Var> changedVar = new ArrayList<Var>();
		Iterator<Port> portIter = component.getDataPorts().iterator();
		Map<Port, Var> portDep = new HashMap<Port, Var>();

		for (Var var : inVars) {
			Var varDep = IrFactory.eINSTANCE.createVar(0, var.getType(),
					var.getIndexedName() + "_" + prefix, false, 0);
			Port dataPort = portIter.next();
			dataPort.setIDLogical(varDep.getIndexedName());
			dataPort.setSize(varDep.getType().getSizeInBits(), varDep.getType()
					.isInt() || varDep.getType().isBool());
			portDep.put(dataPort, varDep);
			changedVar.add(varDep);
		}

		// Put Input dependency
		componentPortDependency.put(component, portDep);
	}

	protected void mapOutControlPort(Component component, Integer group) {
		Bus doneBus = component.getExit(Exit.DONE).getDoneBus();
		Map<Bus, Integer> busGroup = new HashMap<Bus, Integer>();
		busGroup.put(doneBus, group);
		componentDoneBusDependency.put(component, busGroup);
	}

	protected void mapOutPorts(Var var, Integer group) {
		Bus dataBus = currentComponent.getExit(Exit.DONE).getDataBuses()
				.get(group);
		Map<Bus, List<Var>> busDep = new HashMap<Bus, List<Var>>();
		if (dataBus.getValue() == null) {
			dataBus.setSize(var.getType().getSizeInBits(), var.getType()
					.isInt() || var.getType().isBool());
		}
		dataBus.setIDLogical(var.getIndexedName());
		busDep.put(dataBus, Arrays.asList(var));
		componentBusDependency.put(currentComponent, busDep);

		mapOutControlPort(currentComponent, group);
	}

	@SuppressWarnings("unused")
	// Just for the moment suppress the warning, in construction
	protected void operationDependencies(Module module,
			List<Component> components,
			Map<Component, Map<Port, Var>> dependecies, Exit exit) {

		for (Component component : module.getComponents()) {
			if (components.contains(component)) {
				// Build Data dependencies
				for (Port port : component.getDataPorts()) {
					Var var = componentPortDependency.get(component).get(port);
					if (dependencyOnModulePort(module, var)) {
						Bus sourceBus = getModulePortPeer(module, var);
						Port targetPort = getModulePort(component, var);
						List<Entry> entries = targetPort.getOwner()
								.getEntries();
						Entry entry = entries.get(0);
						Dependency dep = new DataDependency(sourceBus);
						entry.addDependency(targetPort, dep);
					} else {
						Bus sourceBus = getModuleComponentPortPeer(module, var,
								0);
						List<Entry> entries = port.getOwner().getEntries();
						Entry entry = entries.get(0);
						Dependency dep = new DataDependency(sourceBus);
						entry.addDependency(port, dep);
					}
				}
				if (component instanceof Loop) {
					Map<Port, Map<Port, Bus>> feedbackDeps = new HashMap<Port, Map<Port, Bus>>();
					Map<Port, Map<Port, Bus>> initialDeps = new HashMap<Port, Map<Port, Bus>>();
					Map<Port, Map<Port, Bus>> outputDeps = new HashMap<Port, Map<Port, Bus>>();

					LoopBody loopBody = ((Loop) component).getBody();
					Exit fbExit = loopBody.getFeedbackExit();
					Exit doneExit = loopBody.getLoopCompleteExit();
					Exit initExit = ((Loop) component).getInBuf().getExit(
							Exit.DONE);

					// Populate outputDeps with Done Exit
					Map<Port, Bus> doneMap = new HashMap<Port, Bus>();
					Bus doneBus = loopBody.getExit(Exit.DONE).getDoneBus();
					Port donePort = component.getExit(Exit.DONE).getDoneBus()
							.getPeer();
					doneMap.put(donePort, doneBus);
					outputDeps.put(donePort, doneMap);

					Entry initEntry = ((Loop) component).getBodyInitEntry();
					Entry fbEntry = ((Loop) component).getBodyFeedbackEntry();
					Collection<Dependency> goInitDeps = initEntry
							.getDependencies(loopBody.getGoPort());
					Bus initDoneBus = goInitDeps.iterator().next()
							.getLogicalBus();
					// Build the output dependencies
					Entry outbufEntry = ((Loop) component).getExit(Exit.DONE)
							.getPeer().getEntries().get(0);
					for (Port port : outputDeps.keySet()) {
						Port targetPort = port;
						Bus sourceBus = outputDeps.get(port).get(port);
						Dependency dep = (targetPort == targetPort.getOwner()
								.getGoPort()) ? new ControlDependency(sourceBus)
								: new DataDependency(sourceBus);
						outbufEntry.addDependency(targetPort, dep);
					}

				} else {
					// Build control Dependencies
					Map<Bus, Integer> busGroup = componentDoneBusDependency
							.get(component);
					if (busGroup != null) {
						for (Bus busDone : busGroup.keySet()) {

							Port donePort = exit.getDoneBus().getPeer();
							List<Entry> entries = donePort.getOwner()
									.getEntries();
							Entry entry = entries.get(busGroup.get(busDone));
							Dependency dep = new ControlDependency(busDone);
							entry.addDependency(donePort, dep);
						}
					}
				}

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
			if (module instanceof Block) {
				((Block) module).insertComponent(comp, index++);
			} else {
				module.addComponent(comp);
			}

			componentAddEntry(comp, drivingExit, clockBus, resetBus, goBus);

			drivingExit = comp.getExit(Exit.DONE);
		}

		// Ensure that the outbufs of the module have an entry
		for (OutBuf outbuf : module.getOutBufs()) {
			componentAddEntry(outbuf, drivingExit, clockBus, resetBus, goBus);
		}
	}

	protected void propagateInputs(Decision decision, Block testBlock) {
		for (Port port : testBlock.getDataPorts()) {
			Port decisionPort = decision.makeDataPort();
			Entry entry = port.getOwner().getEntries().get(0);
			entry.addDependency(port,
					new DataDependency(decisionPort.getPeer()));
		}
	}

	protected void setAttributes(String tag, Component comp) {
		setAttributes(tag, comp, false);
	}

	protected void setAttributes(String tag, Component comp, Boolean Removable) {
		comp.setSourceName(tag);
		if (!Removable) {
			comp.setNonRemovable();
		}
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

	protected void topLevelInit(Call call) {
		call.getClockPort().setSize(1, false);
		call.getResetPort().setSize(1, false);
		call.getGoPort().setSize(1, false);
	}

}
