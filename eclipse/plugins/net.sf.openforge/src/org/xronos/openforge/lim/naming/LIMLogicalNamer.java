/*******************************************************************************
 * Copyright 2002-2009  Xilinx Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
/*
 * 
 *
 * 
 */
package org.xronos.openforge.lim.naming;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.xronos.openforge.app.EngineThread;
import org.xronos.openforge.app.OptionRegistry;
import org.xronos.openforge.app.project.Option;
import org.xronos.openforge.app.project.OptionBoolean;
import org.xronos.openforge.lim.ArrayRead;
import org.xronos.openforge.lim.ArrayWrite;
import org.xronos.openforge.lim.Block;
import org.xronos.openforge.lim.Branch;
import org.xronos.openforge.lim.Bus;
import org.xronos.openforge.lim.Call;
import org.xronos.openforge.lim.CodeLabel;
import org.xronos.openforge.lim.Component;
import org.xronos.openforge.lim.Decision;
import org.xronos.openforge.lim.Design;
import org.xronos.openforge.lim.Exit;
import org.xronos.openforge.lim.FilteredVisitor;
import org.xronos.openforge.lim.ForBody;
import org.xronos.openforge.lim.HeapRead;
import org.xronos.openforge.lim.HeapWrite;
import org.xronos.openforge.lim.InBuf;
import org.xronos.openforge.lim.Kicker;
import org.xronos.openforge.lim.Latch;
import org.xronos.openforge.lim.Loop;
import org.xronos.openforge.lim.Module;
import org.xronos.openforge.lim.OutBuf;
import org.xronos.openforge.lim.Pin;
import org.xronos.openforge.lim.PinRead;
import org.xronos.openforge.lim.PinReferee;
import org.xronos.openforge.lim.PinStateChange;
import org.xronos.openforge.lim.PinWrite;
import org.xronos.openforge.lim.Port;
import org.xronos.openforge.lim.PriorityMux;
import org.xronos.openforge.lim.Procedure;
import org.xronos.openforge.lim.Register;
import org.xronos.openforge.lim.RegisterGateway;
import org.xronos.openforge.lim.RegisterRead;
import org.xronos.openforge.lim.RegisterReferee;
import org.xronos.openforge.lim.RegisterWrite;
import org.xronos.openforge.lim.Scoreboard;
import org.xronos.openforge.lim.Switch;
import org.xronos.openforge.lim.Task;
import org.xronos.openforge.lim.TaskCall;
import org.xronos.openforge.lim.TriBuf;
import org.xronos.openforge.lim.UnexpectedVisitationException;
import org.xronos.openforge.lim.UntilBody;
import org.xronos.openforge.lim.Visitable;
import org.xronos.openforge.lim.Visitor;
import org.xronos.openforge.lim.WhileBody;
import org.xronos.openforge.lim.io.BlockIOInterface;
import org.xronos.openforge.lim.io.FifoAccess;
import org.xronos.openforge.lim.io.FifoRead;
import org.xronos.openforge.lim.io.FifoWrite;
import org.xronos.openforge.lim.io.SimplePin;
import org.xronos.openforge.lim.io.SimplePinAccess;
import org.xronos.openforge.lim.io.SimplePinRead;
import org.xronos.openforge.lim.io.SimplePinWrite;
import org.xronos.openforge.lim.memory.AbsoluteMemoryRead;
import org.xronos.openforge.lim.memory.AbsoluteMemoryWrite;
import org.xronos.openforge.lim.memory.LocationConstant;
import org.xronos.openforge.lim.memory.MemoryBank;
import org.xronos.openforge.lim.memory.MemoryGateway;
import org.xronos.openforge.lim.memory.MemoryRead;
import org.xronos.openforge.lim.memory.MemoryReferee;
import org.xronos.openforge.lim.memory.MemoryWrite;
import org.xronos.openforge.lim.memory.StructuralMemory;
import org.xronos.openforge.lim.op.AddOp;
import org.xronos.openforge.lim.op.AndOp;
import org.xronos.openforge.lim.op.CastOp;
import org.xronos.openforge.lim.op.ComplementOp;
import org.xronos.openforge.lim.op.ConditionalAndOp;
import org.xronos.openforge.lim.op.ConditionalOrOp;
import org.xronos.openforge.lim.op.Constant;
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
import org.xronos.openforge.lim.op.NumericPromotionOp;
import org.xronos.openforge.lim.op.OrOp;
import org.xronos.openforge.lim.op.PlusOp;
import org.xronos.openforge.lim.op.ReductionOrOp;
import org.xronos.openforge.lim.op.RightShiftOp;
import org.xronos.openforge.lim.op.RightShiftUnsignedOp;
import org.xronos.openforge.lim.op.ShortcutIfElseOp;
import org.xronos.openforge.lim.op.SubtractOp;
import org.xronos.openforge.lim.op.TimingOp;
import org.xronos.openforge.lim.op.XorOp;
import org.xronos.openforge.lim.primitive.And;
import org.xronos.openforge.lim.primitive.EncodedMux;
import org.xronos.openforge.lim.primitive.Mux;
import org.xronos.openforge.lim.primitive.Not;
import org.xronos.openforge.lim.primitive.Or;
import org.xronos.openforge.lim.primitive.Reg;
import org.xronos.openforge.lim.primitive.SRL16;
import org.xronos.openforge.util.naming.ID;
import org.xronos.openforge.util.naming.IDDb;
import org.xronos.openforge.util.naming.IDSourceInfo;


/**
 * Sets unique logical names to every object in a LIM.
 * 
 * @author <a href="mailto:andreas.kollegger@xilinx.com">Andreas Kollegger</a>
 * @version $Id: LIMLogicalNamer.java 286 2006-08-15 17:03:16Z imiller $
 */
public class LIMLogicalNamer extends FilteredVisitor implements Visitor {

	/** Used to turn on debug output. */
	private static final boolean DEBUG = false;

	/** Keep all instances of Kickers uniquely named */
	private static int index = 0;
	
	private boolean fifoIO = false;

	/**
	 * The top module name. Used to uniquify global modules and such.
	 */
	private String topModuleName = "";

	/**
	 * Create a LIMNamer and make it name the given design.
	 * 
	 * @param design
	 */
	public static void setNames(Design design, boolean fifoIO) {
		LIMLogicalNamer namer = new LIMLogicalNamer(fifoIO);
		design.accept(namer);
	}

	public static void setNames(Design design) {
		setNames(design, false);
	}

	/**
	 * Construct a new LIMNamer.
	 */
	private LIMLogicalNamer() {
		this(false);
	}

	private LIMLogicalNamer(boolean fifoIO) {
		this.fifoIO = fifoIO;
	}

	/**
	 * @see org.xronos.openforge.lim.Visitor#visit(org.xronos.openforge.lim.Design)
	 */
	@SuppressWarnings("deprecation")
	@Override
	public void visit(Design design) {
		// Make the TOP_MODULE_NAME setting the absolute name
		Option op = EngineThread.getGenericJob().getOption(
				OptionRegistry.TOP_MODULE_NAME);
		topModuleName = op.getValue(design.getSearchLabel()).toString();

		super.visit(design);

		String designName = topModuleName;

		if (designName.length() == 0) {
			String baseName = "";
			if (EngineThread.getGenericJob().getTargetFiles().length > 0) {
				baseName = EngineThread.getGenericJob().getTargetFiles()[0]
						.getName();
				if (baseName.lastIndexOf(".") >= 0) {
					// prune out the file type suffix
					baseName = baseName.substring(0, baseName.lastIndexOf("."));
				}
				baseName += "_";
			}

			String derivedModuleName = "";
			// Check if the entry function is specified, and if so
			// the name becomes the entry function name
			op = EngineThread.getGenericJob().getOption(
					OptionRegistry.NO_BLOCK_IO);
			if (!((OptionBoolean) op).getValueAsBoolean(CodeLabel.UNSCOPED)) // If
																				// Do
																				// BlockIO
			{
				derivedModuleName = BlockIOInterface.getFunctionNames()
						.iterator().next();
			} else {
				if (design.getTasks().iterator().hasNext()) {
					derivedModuleName = design.getTasks().iterator().next()
							.getCall().getProcedure().showIDLogical();
				}
			}

			if (derivedModuleName.length() == 0) {
				// the top module name is not supplied, and no entry
				// function is specified, default to the non-static
				// function name.
				derivedModuleName = design.getIDSourceInfo()
						.getFullyQualifiedName();
			}

			if (fifoIO) {
				derivedModuleName += "_core_impl";
			}

			designName = baseName + derivedModuleName;
		}

		design.setIDLogical(designName);

		if (DEBUG) {
			debugln(ID.showDebug(design) + " given logical name \""
					+ ID.showLogical(design) + "\"");
		}
		namePins(design.getInputPins(), "IN");
		namePins(design.getOutputPins(), "OUT");
		namePins(design.getBidirectionalPins(), "INOUT");

		design.accept(new DesignElementNamingVisitor());
	}

	/** Maps simple pin names to a list of pins with that name. */
	private Map<String, List<Pin>> pinNameMap = new HashMap<String, List<Pin>>();

	/**
	 * Because this code uses the IDSourceInfo to generate a unique name, all
	 * pins should have the fieldName in the IDSourceInfo set.
	 */
	private void namePins(Collection<Pin> pins, String baseName) {
		Set<String> newNames = new HashSet<String>();
		for (Pin pin : pins) {
			IDSourceInfo info = pin.getIDSourceInfo();
			String simplePinName = baseName;
			if (pin.hasExplicitName()) {
				simplePinName = ID.showLogical(pin);
			} else if (info.getFieldName() != null) {
				simplePinName = info.getFieldName();
			}
			List<Pin> commonlyNamedPins = pinNameMap.get(simplePinName);
			if (commonlyNamedPins == null) {
				commonlyNamedPins = new ArrayList<Pin>();
				pinNameMap.put(simplePinName, commonlyNamedPins);
			}
			newNames.add(simplePinName);
			commonlyNamedPins.add(pin);
		}

		for (String simplePinName : newNames) {
			List<Pin> commonlyNamedPins = pinNameMap.get(simplePinName);
			if (commonlyNamedPins.size() > 1) {
				int pIndex = 0;
				for (Pin pin : commonlyNamedPins) {
					if (simplePinName == baseName) {
						pin.setIDLogical(simplePinName + pIndex++);
					} else {
						pin.setIDLogical(pin.getIDSourceInfo()
								.getFullyQualifiedName());
					}
				}
			} else {
				Pin pin = commonlyNamedPins.get(0);
				pin.setIDLogical(simplePinName);
			}
		}
	}

	private IDDb taskDb = new IDDb();

	/**
	 * @see org.xronos.openforge.lim.Visitor#visit(org.xronos.openforge.lim.Task)
	 */
	@Override
	public void visit(Task task) {
		Procedure procedure = task.getCall().getProcedure();
		IDSourceInfo info = procedure.getIDSourceInfo();
		String taskName = "task_" + info.getMethodName() + info.getSignature();
		long nextID = taskDb.getNextID(taskName);
		if (nextID > 0) {
			taskName += nextID;
		}
		task.setIDLogical(taskName);

		if (DEBUG) {
			debugln(ID.showDebug(task) + " given logical name \""
					+ ID.showLogical(task) + "\"");
		}

		super.visit(task);
	}

	/**
	 * Makes an appropriate logical name for a Procedure. This can be controlled
	 * in a limited way with {@link Procedure#shouldUseSignatureInName()} and
	 * {@link Procedure#shouldUseSimpleNaming()}.
	 * 
	 * @param procedure
	 *            the Procedure to be named.
	 * @return a logical name for the procedure
	 */
	private String makeMethodName(Procedure procedure) {
		Option op;
		IDSourceInfo info = procedure.getIDSourceInfo();

		String methodName = info.getMethodName();
		if (methodName == null) {
			methodName = procedure.showIDLogical();
			if (procedure.getSourceName() != null) {
				methodName = methodName.replaceAll(
						"_" + procedure.getSourceName(), "");
			}
		}

		String signature = info.getSignature();
		if (signature == null) {
			signature = "";
		}

		op = procedure.getGenericJob().getOption(
				OptionRegistry.SIGNATURE_IN_NAMES);
		if (((OptionBoolean) op).getValueAsBoolean(procedure.getSearchLabel())) {
			methodName = new String(methodName + signature);
		}

		op = procedure.getGenericJob().getOption(
				OptionRegistry.SIMPLE_MODULE_NAMES);
		String returnedMethodName = "";
		if (((OptionBoolean) op).getValueAsBoolean(procedure.getSearchLabel())) {
			returnedMethodName = methodName;
		} else {
			StringBuffer fullName = new StringBuffer();
			String packageName = info.getSourcePackageName();
			String className = info.getSourceClassName();
			if (packageName != null) {
				fullName.append(packageName);
			}
			if (className != null) {
				if (fullName.length() > 0) {
					fullName.append(".");
				}
				fullName.append(className);
			}
			fullName.append(methodName + signature);
			returnedMethodName = fullName.toString();
		}

		return uniquifyCoreName(returnedMethodName);
	}

	private IDDb callDb = new IDDb();

	/**
	 * @see org.xronos.openforge.lim.Visitor#visit(org.xronos.openforge.lim.Call)
	 */
	@SuppressWarnings("deprecation")
	@Override
	public void visit(Call call) {
		// all call naming is based on the procedure, so name that first.
		super.visit(call);

		Procedure procedure = call.getProcedure();

		String callName = ID.showLogical(procedure) + "_instance";
		long nextID = callDb.getNextID(callName);
		if (nextID > 0) {
			callName += nextID;
		}
		call.setIDLogical(callName);

		/** set unique id logical for all call DONE exit databus */
		int nameCounter = 0;
		Exit exit = call.getExit(Exit.DONE);
		Bus done = exit.getDoneBus();
		done.setIDLogical(ID.showLogical(call) + "_DONE");

		for (Bus exitBus : exit.getDataBuses()) {
			exitBus.setIDLogical(ID.showLogical(call) + "_"
					+ procedure.getResultName()
					+ ((nameCounter > 0) ? Integer.toString(nameCounter) : ""));
			nameCounter++;
		}
	}

	/**
	 * @see org.xronos.openforge.lim.Visitor#visit(org.xronos.openforge.lim.Procedure)
	 */
	@SuppressWarnings("deprecation")
	@Override
	public void visit(Procedure procedure) {

		procedure.setIDLogical(makeMethodName(procedure));

		if (DEBUG) {
			debugln(ID.showDebug(procedure) + " given logical name \""
					+ ID.showLogical(procedure) + "\"");
		}

		Block block = procedure.getBody();
		block.accept(this);

		String domainSpec = (String) EngineThread.getGenericJob()
				.getOption(OptionRegistry.CLOCK_DOMAIN)
				.getValue(procedure.getSearchLabel());
		// Design.ClockDomain domain = design.getClockDomain(domainSpec);
		String[] clkrst = Design.ClockDomain.parse(domainSpec);
		// Naming the procedure block's Clock, reset, and go ports
		Port clockPort = block.getClockPort();
		// clockPort.getPeer().setIDLogical(procedure.getClockName());
		clockPort.getPeer().setIDLogical(clkrst[0]);
		Port resetPort = block.getResetPort();
		// resetPort.getPeer().setIDLogical(procedure.getResetName());
		if (clkrst.length > 1 && clkrst[1].length() > 0) {
			resetPort.getPeer().setIDLogical(clkrst[1]);
		} else {
			resetPort.getPeer().setIDLogical("RESET");
		}
		Port goPort = block.getGoPort();
		// goPort.getIDSourceInfo().setFieldName(procedure.getGoName());
		goPort.getIDSourceInfo().setFieldName("GO");
		// goPort.getPeer().setIDLogical(procedure.getGoName());
		goPort.getPeer().setIDLogical("GO");

		// Naming the parameter ports
		for (Port dataPort : block.getDataPorts()) {
			if (DEBUG) {
				debugln("\tport[" + ID.showDebug(dataPort) + "] "
						+ "deriving logical name from "
						+ dataPort.getIDSourceInfo());
			}
			dataPort.setIDLogical(dataPort.getIDSourceInfo().getFieldName());

			/** set the InBuf data bus id logical */
			dataPort.getPeer().setIDLogical(ID.showLogical(dataPort));
		}

		/*
		 * Naming the output Buses of a procedure block. The Procedure
		 * translator requires them.
		 */

		for (Exit exit : block.getExits()) {
			exit.getDoneBus().setIDLogical("DONE");
			for (Bus bus : exit.getDataBuses()) {
				bus.setIDLogical(procedure.getResultName());
			}
		}

		super.visit(procedure);
	}

	/**
	 * @see org.xronos.openforge.lim.Visitor#visit(org.xronos.openforge.lim.Kicker)
	 */
	@Override
	public void visit(Kicker kicker) {
		kicker.setIDLogical("kicker");

		if (DEBUG) {
			debugln(ID.showDebug(kicker) + " given logical name \""
					+ ID.showLogical(kicker) + "\"");
		}

		super.visit(kicker);
	}

	@Override
	public void visit(MemoryBank memBank) {
		memBank.setIDLogical("memBank");

		if (DEBUG) {
			debugln(ID.showDebug(memBank) + " given logical name \""
					+ ID.showLogical(memBank) + "\"");
		}

		super.visit(memBank);
	}

	/**
	 * @see org.xronos.openforge.lim.Visitor#visit(org.xronos.openforge.lim.Block)
	 */
	@Override
	public void visit(Block block) {
		block.setIDLogical("block");

		if (DEBUG) {
			debugln(ID.showDebug(block) + " given logical name \""
					+ ID.showLogical(block) + "\"");
		}

		/*
		 * Name the inBuf's buses of a 'not-procedure' block.
		 */
		if (!block.isProcedureBody()) {
			InBuf ib = block.getInBuf();
			ib.getClockBus().setIDLogical(ID.showLogical(block) + "_CLK");
			ib.getResetBus().setIDLogical(ID.showLogical(block) + "_RESET");
			ib.getGoBus().setIDLogical(ID.showLogical(block) + "_GO");
		}

		super.traverse(block);

		/*
		 * Naming the output Buses of a 'not-procedure' block.
		 */
		if (!block.isProcedureBody()) {
			int i = 0;
			int j = 0;
			for (Exit exit : block.getExits()) {
				exit.getDoneBus().setIDLogical(
						ID.showLogical(block) + "_DONE" + i++);
				for (Bus bus : exit.getDataBuses()) {
					bus.setIDLogical(ID.showLogical(block) + "_OUT" + j++);
				}
			}
		}
	}

	/**
	 * @see org.xronos.openforge.lim.Visitor#visit(org.xronos.openforge.lim.Loop)
	 */
	@Override
	public void visit(Loop loop) {
		loop.setIDLogical("loop");

		if (DEBUG) {
			debugln(ID.showDebug(loop) + " given logical name \""
					+ ID.showLogical(loop) + "\"");
		}

		super.visit(loop);
	}

	/**
	 * @see org.xronos.openforge.lim.Visitor#visit(org.xronos.openforge.lim.WhileBody)
	 */
	@Override
	public void visit(WhileBody whileBody) {
		whileBody.setIDLogical("whileBody");

		if (DEBUG) {
			debugln(ID.showDebug(whileBody) + " given logical name \""
					+ ID.showLogical(whileBody) + "\"");
		}

		super.visit(whileBody);
	}

	/**
	 * @see org.xronos.openforge.lim.Visitor#visit(org.xronos.openforge.lim.UntilBody)
	 */
	@Override
	public void visit(UntilBody untilBody) {
		untilBody.setIDLogical("untilBody");

		if (DEBUG) {
			debugln(ID.showDebug(untilBody) + " given logical name \""
					+ ID.showLogical(untilBody) + "\"");
		}

		super.visit(untilBody);
	}

	/**
	 * @see org.xronos.openforge.lim.Visitor#visit(org.xronos.openforge.lim.ForBody)
	 */
	@Override
	public void visit(ForBody forBody) {
		forBody.setIDLogical("forBody");

		if (DEBUG) {
			debugln(ID.showDebug(forBody) + " given logical name \""
					+ ID.showLogical(forBody) + "\"");
		}

		super.visit(forBody);
	}

	/**
	 * @see org.xronos.openforge.lim.Visitor#visit(org.xronos.openforge.lim.op.AddOp)
	 */
	@Override
	public void visit(AddOp add) {
		add.setIDLogical("add");

		if (DEBUG) {
			debugln(ID.showDebug(add) + " given logical name \""
					+ ID.showLogical(add) + "\"");
		}

		super.visit(add);
	}

	/**
	 * @see org.xronos.openforge.lim.Visitor#visit(org.xronos.openforge.lim.op.AndOp)
	 */
	@Override
	public void visit(AndOp andOp) {
		andOp.setIDLogical("andOp");

		if (DEBUG) {
			debugln(ID.showDebug(andOp) + " given logical name \""
					+ ID.showLogical(andOp) + "\"");
		}

		super.visit(andOp);
	}

	/**
	 * @see org.xronos.openforge.lim.Visitor#visit(org.xronos.openforge.lim.op.CastOp)
	 */
	@Override
	public void visit(CastOp cast) {
		cast.setIDLogical("cast");

		if (DEBUG) {
			debugln(ID.showDebug(cast) + " given logical name \""
					+ ID.showLogical(cast) + "\"");
		}

		super.visit(cast);
	}

	/**
	 * @see org.xronos.openforge.lim.Visitor#visit(org.xronos.openforge.lim.op.ComplementOp)
	 */
	@Override
	public void visit(ComplementOp complement) {
		complement.setIDLogical("complement");

		if (DEBUG) {
			debugln(ID.showDebug(complement) + " given logical name \""
					+ ID.showLogical(complement) + "\"");
		}

		super.visit(complement);
	}

	/**
	 * @see org.xronos.openforge.lim.Visitor#visit(org.xronos.openforge.lim.op.ConditionalAndOp)
	 */
	@Override
	public void visit(ConditionalAndOp conditionalAnd) {
		conditionalAnd.setIDLogical("conditionalAnd");

		if (DEBUG) {
			debugln(ID.showDebug(conditionalAnd) + " given logical name \""
					+ ID.showLogical(conditionalAnd) + "\"");
		}

		super.visit(conditionalAnd);
	}

	/**
	 * @see org.xronos.openforge.lim.Visitor#visit(org.xronos.openforge.lim.op.ConditionalOrOp)
	 */
	@Override
	public void visit(ConditionalOrOp conditionalOr) {
		conditionalOr.setIDLogical("conditionalOr");

		if (DEBUG) {
			debugln(ID.showDebug(conditionalOr) + " given logical name \""
					+ ID.showLogical(conditionalOr) + "\"");
		}

		super.visit(conditionalOr);
	}

	/**
	 * @see org.xronos.openforge.lim.Visitor#visit(org.xronos.openforge.lim.op.Constant)
	 */
	@Override
	public void visit(Constant constant) {
		constant.setIDLogical("constant");

		if (DEBUG) {
			debugln(ID.showDebug(constant) + " given logical name \""
					+ ID.showLogical(constant) + "\"");
		}

		super.visit(constant);
	}

	/**
	 * @see visit(Constant)
	 */
	@Override
	public void visit(LocationConstant loc) {
		visit((Constant) loc);
	}

	/**
	 * @see org.xronos.openforge.lim.Visitor#visit(org.xronos.openforge.lim.op.DivideOp)
	 */
	@Override
	public void visit(DivideOp divide) {
		divide.setIDLogical("divide");

		if (DEBUG) {
			debugln(ID.showDebug(divide) + " given logical name \""
					+ ID.showLogical(divide) + "\"");
		}

		super.visit(divide);
	}

	/**
	 * @see org.xronos.openforge.lim.Visitor#visit(org.xronos.openforge.lim.op.EqualsOp)
	 */
	@Override
	public void visit(EqualsOp equals) {
		equals.setIDLogical("equals");

		if (DEBUG) {
			debugln(ID.showDebug(equals) + " given logical name \""
					+ ID.showLogical(equals) + "\"");
		}

		super.visit(equals);
	}

	/**
	 * @see org.xronos.openforge.lim.Visitor#visit(org.xronos.openforge.lim.op.GreaterThanEqualToOp)
	 */
	@Override
	public void visit(GreaterThanEqualToOp greaterThanEqualTo) {
		greaterThanEqualTo.setIDLogical("greaterThanEqualTo");

		if (DEBUG) {
			debugln(ID.showDebug(greaterThanEqualTo) + " given logical name \""
					+ ID.showLogical(greaterThanEqualTo) + "\"");
		}

		super.visit(greaterThanEqualTo);
	}

	/**
	 * @see org.xronos.openforge.lim.Visitor#visit(org.xronos.openforge.lim.op.GreaterThanOp)
	 */
	@Override
	public void visit(GreaterThanOp greaterThan) {
		greaterThan.setIDLogical("greaterThan");

		if (DEBUG) {
			debugln(ID.showDebug(greaterThan) + " given logical name \""
					+ ID.showLogical(greaterThan) + "\"");
		}

		super.visit(greaterThan);
	}

	/**
	 * @see org.xronos.openforge.lim.Visitor#visit(org.xronos.openforge.lim.op.LeftShiftOp)
	 */
	@Override
	public void visit(LeftShiftOp leftShift) {
		leftShift.setIDLogical("leftShift");

		if (DEBUG) {
			debugln(ID.showDebug(leftShift) + " given logical name \""
					+ ID.showLogical(leftShift) + "\"");
		}

		super.visit(leftShift);
	}

	/**
	 * @see org.xronos.openforge.lim.Visitor#visit(org.xronos.openforge.lim.op.LessThanEqualToOp)
	 */
	@Override
	public void visit(LessThanEqualToOp lessThanEqualTo) {
		lessThanEqualTo.setIDLogical("lessThanEqualTo");

		if (DEBUG) {
			debugln(ID.showDebug(lessThanEqualTo) + " given logical name \""
					+ ID.showLogical(lessThanEqualTo) + "\"");
		}

		super.visit(lessThanEqualTo);
	}

	/**
	 * @see org.xronos.openforge.lim.Visitor#visit(org.xronos.openforge.lim.op.LessThanOp)
	 */
	@Override
	public void visit(LessThanOp lessThan) {
		lessThan.setIDLogical("lessThan");

		if (DEBUG) {
			debugln(ID.showDebug(lessThan) + " given logical name \""
					+ ID.showLogical(lessThan) + "\"");
		}

		super.visit(lessThan);
	}

	/**
	 * @see org.xronos.openforge.lim.Visitor#visit(org.xronos.openforge.lim.op.MinusOp)
	 */
	@Override
	public void visit(MinusOp minus) {
		minus.setIDLogical("minus");

		if (DEBUG) {
			debugln(ID.showDebug(minus) + " given logical name \""
					+ ID.showLogical(minus) + "\"");
		}

		super.visit(minus);
	}

	/**
	 * @see org.xronos.openforge.lim.Visitor#visit(org.xronos.openforge.lim.op.ModuloOp)
	 */
	@Override
	public void visit(ModuloOp modulo) {
		modulo.setIDLogical("modulo");

		if (DEBUG) {
			debugln(ID.showDebug(modulo) + " given logical name \""
					+ ID.showLogical(modulo) + "\"");
		}

		super.visit(modulo);
	}

	/**
	 * @see org.xronos.openforge.lim.Visitor#visit(org.xronos.openforge.lim.op.MultiplyOp)
	 */
	@Override
	public void visit(MultiplyOp multiply) {
		multiply.setIDLogical("multiply");

		if (DEBUG) {
			debugln(ID.showDebug(multiply) + " given logical name \""
					+ ID.showLogical(multiply) + "\"");
		}

		super.visit(multiply);
	}

	/**
	 * @see org.xronos.openforge.lim.Visitor#visit(org.xronos.openforge.lim.op.NotEqualsOp)
	 */
	@Override
	public void visit(NotEqualsOp notEquals) {
		notEquals.setIDLogical("notEquals");

		if (DEBUG) {
			debugln(ID.showDebug(notEquals) + " given logical name \""
					+ ID.showLogical(notEquals) + "\"");
		}

		super.visit(notEquals);
	}

	/**
	 * @see org.xronos.openforge.lim.Visitor#visit(org.xronos.openforge.lim.op.NotOp)
	 */
	@Override
	public void visit(NotOp not) {
		not.setIDLogical("not");

		if (DEBUG) {
			debugln(ID.showDebug(not) + " given logical name \""
					+ ID.showLogical(not) + "\"");
		}

		super.visit(not);
	}

	/**
	 * @see org.xronos.openforge.lim.Visitor#visit(org.xronos.openforge.lim.op.OrOp)
	 */
	@Override
	public void visit(OrOp or) {
		// The SimplePinConnector sets the name of the OrOpMulti and
		// we dont want to override that here.
		if (!or.hasExplicitName()) {
			or.setIDLogical("or");
		}

		if (DEBUG) {
			debugln(ID.showDebug(or) + " given logical name \""
					+ ID.showLogical(or) + "\"");
		}

		super.visit(or);
	}

	/**
	 * @see org.xronos.openforge.lim.Visitor#visit(org.xronos.openforge.lim.op.PlusOp)
	 */
	@Override
	public void visit(PlusOp plus) {
		plus.setIDLogical("plus");

		if (DEBUG) {
			debugln(ID.showDebug(plus) + " given logical name \""
					+ ID.showLogical(plus) + "\"");
		}

		super.visit(plus);
	}

	/**
	 * * @see org.xronos.openforge.lim.Visitor#visit(org.xronos.openforge.lim.op.
	 * ReductionOrOp)
	 * */
	@Override
	public void visit(ReductionOrOp reductionOr) {
		reductionOr.setIDLogical("reductionOr");

		if (DEBUG) {
			debugln(ID.showDebug(reductionOr) + " given logical name \""
					+ ID.showLogical(reductionOr) + "\"");
		}

		super.visit(reductionOr);
	}

	/**
	 * @see org.xronos.openforge.lim.Visitor#visit(org.xronos.openforge.lim.op.RightShiftOp)
	 */
	@Override
	public void visit(RightShiftOp rightShift) {
		rightShift.setIDLogical("rightShift");

		if (DEBUG) {
			debugln(ID.showDebug(rightShift) + " given logical name \""
					+ ID.showLogical(rightShift) + "\"");
		}

		super.visit(rightShift);
	}

	/**
	 * @see org.xronos.openforge.lim.Visitor#visit(org.xronos.openforge.lim.op.RightShiftUnsignedOp)
	 */
	@Override
	public void visit(RightShiftUnsignedOp rightShiftUnsigned) {
		rightShiftUnsigned.setIDLogical("rightShiftUnsigned");

		if (DEBUG) {
			debugln(ID.showDebug(rightShiftUnsigned) + " given logical name \""
					+ ID.showLogical(rightShiftUnsigned) + "\"");
		}

		super.visit(rightShiftUnsigned);
	}

	/**
	 * @see org.xronos.openforge.lim.Visitor#visit(org.xronos.openforge.lim.op.ShortcutIfElseOp)
	 */
	@Override
	public void visit(ShortcutIfElseOp shortcutIfElse) {
		shortcutIfElse.setIDLogical("shortcutIfElse");

		if (DEBUG) {
			debugln(ID.showDebug(shortcutIfElse) + " given logical name \""
					+ ID.showLogical(shortcutIfElse) + "\"");
		}

		super.visit(shortcutIfElse);
	}

	@Override
	public void visit(TaskCall comp) {
		comp.setIDLogical("taskCall");

		if (DEBUG) {
			debugln(ID.showDebug(comp) + " given logical name \""
					+ ID.showLogical(comp) + "\"");
		}

		super.visit(comp);
	}

	@Override
	public void visit(SimplePin comp) {
		// Do nothing.... leave the pins alone, their names are
		// handled elsewhere (they have a setName method).
	}

	@Override
	public void visit(SimplePinAccess comp) {
		comp.setIDLogical("simplePinAccess");

		if (DEBUG) {
			debugln(ID.showDebug(comp) + " given logical name \""
					+ ID.showLogical(comp) + "\"");
		}

		super.visit(comp);
	}

	@Override
	public void visit(SimplePinRead comp) {
		comp.setIDLogical("simplePinRead");

		if (DEBUG) {
			debugln(ID.showDebug(comp) + " given logical name \""
					+ ID.showLogical(comp) + "\"");
		}

		super.visit(comp);
	}

	@Override
	public void visit(SimplePinWrite comp) {
		comp.setIDLogical("simplePinWrite");

		if (DEBUG) {
			debugln(ID.showDebug(comp) + " given logical name \""
					+ ID.showLogical(comp) + "\"");
		}

		super.visit(comp);
	}

	/**
	 * @see org.xronos.openforge.lim.Visitor#visit(org.xronos.openforge.lim.op.SubtractOp)
	 */
	@Override
	public void visit(SubtractOp subtract) {
		subtract.setIDLogical("subtract");

		if (DEBUG) {
			debugln(ID.showDebug(subtract) + " given logical name \""
					+ ID.showLogical(subtract) + "\"");
		}

		super.visit(subtract);
	}

	/**
	 * @see org.xronos.openforge.lim.Visitor#visit(org.xronos.openforge.lim.op.NumericPromotionOp)
	 */
	@Override
	public void visit(NumericPromotionOp numericPromotion) {
		numericPromotion.setIDLogical("numericPromotion");

		if (DEBUG) {
			debugln(ID.showDebug(numericPromotion) + " given logical name \""
					+ ID.showLogical(numericPromotion) + "\"");
		}

		super.visit(numericPromotion);
	}

	/**
	 * @see org.xronos.openforge.lim.Visitor#visit(org.xronos.openforge.lim.op.XorOp)
	 */
	@Override
	public void visit(XorOp xor) {
		xor.setIDLogical("xor");

		if (DEBUG) {
			debugln(ID.showDebug(xor) + " given logical name \""
					+ ID.showLogical(xor) + "\"");
		}

		super.visit(xor);
	}

	/**
	 * @see org.xronos.openforge.lim.Visitor#visit(org.xronos.openforge.lim.Branch)
	 */
	@Override
	public void visit(Branch branch) {
		branch.setIDLogical("branch");

		if (DEBUG) {
			debugln(ID.showDebug(branch) + " given logical name \""
					+ ID.showLogical(branch) + "\"");
		}

		super.visit(branch);
	}

	/**
	 * @see org.xronos.openforge.lim.Visitor#visit(org.xronos.openforge.lim.Decision)
	 */
	@Override
	public void visit(Decision decision) {
		decision.setIDLogical("decision");

		if (DEBUG) {
			debugln(ID.showDebug(decision) + " given logical name \""
					+ ID.showLogical(decision) + "\"");
		}

		super.visit(decision);
	}

	/**
	 * @see org.xronos.openforge.lim.Visitor#visit(org.xronos.openforge.lim.Switch)
	 */
	@Override
	public void visit(Switch sw) {
		sw.setIDLogical("switch");

		if (DEBUG) {
			debugln(ID.showDebug(sw) + " given logical name \""
					+ ID.showLogical(sw) + "\"");
		}

		super.visit(sw);
	}

	/**
	 * @see org.xronos.openforge.lim.Visitor#visit(org.xronos.openforge.lim.InBuf)
	 */
	@Override
	public void visit(InBuf ib) {
		ib.setIDLogical("ib");

		if (DEBUG) {
			debugln(ID.showDebug(ib) + " given logical name \""
					+ ID.showLogical(ib) + "\"");
		}

	}

	/**
	 * @see org.xronos.openforge.lim.Visitor#visit(org.xronos.openforge.lim.OutBuf)
	 */
	@Override
	public void visit(OutBuf ob) {
		ob.setIDLogical("ob");

		if (DEBUG) {
			debugln(ID.showDebug(ob) + " given logical name \""
					+ ID.showLogical(ob) + "\"");
		}

	}

	/**
	 * @see org.xronos.openforge.lim.Visitor#visit(org.xronos.openforge.lim.primitive.Reg)
	 */
	@Override
	public void visit(Reg reg) {
		String regName = ID.showLogical(reg);

		if (!reg.hasExplicitName()) {
			Port dataport = reg.getDataPort();
			Bus sourceBus = dataport.getBus();
			if (sourceBus.hasExplicitName()) {
				regName = ID.showLogical(sourceBus) + "_delayed";
			}
		}

		reg.setIDLogical((regName != null) ? regName : "reg");

		reg.getResultBus().setIDLogical(ID.showLogical(reg) + "_result");

		if (DEBUG) {
			debugln(ID.showDebug(reg) + " given logical name \""
					+ ID.showLogical(reg) + "\"");
		}

	}

	/**
	 * @see org.xronos.openforge.lim.Visitor#visit(org.xronos.openforge.lim.primitive.Mux)
	 */
	@Override
	public void visit(Mux m) {
		m.setIDLogical("mux");

		if (DEBUG) {
			debugln(ID.showDebug(m) + " given logical name \""
					+ ID.showLogical(m) + "\"");
		}

		super.visit(m);
	}

	/**
	 * @see org.xronos.openforge.lim.Visitor#visit(org.xronos.openforge.lim.primitive.EncodedMux)
	 */
	@Override
	public void visit(EncodedMux m) {
		m.setIDLogical("emux");

		if (DEBUG) {
			debugln(ID.showDebug(m) + " given logical name \""
					+ ID.showLogical(m) + "\"");
		}

		super.visit(m);
	}

	/**
	 * @see org.xronos.openforge.lim.Visitor#visit(org.xronos.openforge.lim.PriorityMux)
	 */
	@Override
	public void visit(PriorityMux pmux) {
		pmux.setIDLogical("pmux");

		if (DEBUG) {
			debugln(ID.showDebug(pmux) + " given logical name \""
					+ ID.showLogical(pmux) + "\"");
		}

		super.visit(pmux);
	}

	/**
	 * @see org.xronos.openforge.lim.Visitor#visit(org.xronos.openforge.lim.primitive.And)
	 */
	@Override
	public void visit(And a) {
		a.setIDLogical("and");

		if (DEBUG) {
			debugln(ID.showDebug(a) + " given logical name \""
					+ ID.showLogical(a) + "\"");
		}

		super.visit(a);
	}

	/**
	 * @see org.xronos.openforge.lim.Visitor#visit(org.xronos.openforge.lim.primitive.Not)
	 */
	@Override
	public void visit(Not n) {
		n.setIDLogical("not");

		if (DEBUG) {
			debugln(ID.showDebug(n) + " given logical name \""
					+ ID.showLogical(n) + "\"");
		}

		super.visit(n);
	}

	/**
	 * @see org.xronos.openforge.lim.Visitor#visit(org.xronos.openforge.lim.primitive.Or)
	 */
	@Override
	public void visit(Or o) {
		o.setIDLogical("or");

		if (DEBUG) {
			debugln(ID.showDebug(o) + " given logical name \""
					+ ID.showLogical(o) + "\"");
		}

		super.visit(o);
	}

	/**
	 * @see org.xronos.openforge.lim.Visitor#visit(org.xronos.openforge.lim.Scoreboard)
	 */
	@Override
	public void visit(Scoreboard scoreboard) {
		scoreboard.setIDLogical("scoreboard");

		if (DEBUG) {
			debugln(ID.showDebug(scoreboard) + " given logical name \""
					+ ID.showLogical(scoreboard) + "\"");
		}

		super.visit(scoreboard);
	}

	/**
	 * @see org.xronos.openforge.lim.Visitor#visit(org.xronos.openforge.lim.Latch)
	 */
	@Override
	public void visit(Latch latch) {
		latch.setIDLogical("latch");

		if (DEBUG) {
			debugln(ID.showDebug(latch) + " given logical name \""
					+ ID.showLogical(latch) + "\"");
		}

		super.visit(latch);
	}

	/**
	 * @see org.xronos.openforge.lim.Visitor#visit(org.xronos.openforge.lim.op.NoOp)
	 */
	@Override
	public void visit(NoOp nop) {
		nop.setIDLogical("nop");

		if (DEBUG) {
			debugln(ID.showDebug(nop) + " given logical name \""
					+ ID.showLogical(nop) + "\"");
		}

		super.visit(nop);
	}

	/**
	 * @see org.xronos.openforge.lim.Visitor#visit(org.xronos.openforge.lim.op.TimingOp)
	 */
	@Override
	public void visit(TimingOp nop) {
		nop.setIDLogical("tnop");

		if (DEBUG) {
			debugln(ID.showDebug(nop) + " given logical name \""
					+ ID.showLogical(nop) + "\"");
		}

		super.visit(nop);
	}

	/**
	 * @see org.xronos.openforge.lim.Visitor#visit(org.xronos.openforge.lim.RegisterRead)
	 */
	@Override
	public void visit(RegisterRead regRead) {
		regRead.setIDLogical("regRead");

		if (DEBUG) {
			debugln(ID.showDebug(regRead) + " given logical name \""
					+ ID.showLogical(regRead) + "\"");
		}

		super.visit(regRead);
	}

	/**
	 * @see org.xronos.openforge.lim.Visitor#visit(org.xronos.openforge.lim.RegisterWrite)
	 */
	@Override
	public void visit(RegisterWrite regWrite) {
		regWrite.setIDLogical("regWrite");

		if (DEBUG) {
			debugln(ID.showDebug(regWrite) + " given logical name \""
					+ ID.showLogical(regWrite) + "\"");
		}

		super.visit(regWrite);
	}

	/**
	 * @see org.xronos.openforge.lim.Visitor#visit(org.xronos.openforge.lim.RegisterGateway)
	 */
	@Override
	public void visit(RegisterGateway regGateway) {
		regGateway.setIDLogical("regGateway");

		if (DEBUG) {
			debugln(ID.showDebug(regGateway) + " given logical name \""
					+ ID.showLogical(regGateway) + "\"");
		}

		super.visit(regGateway);
	}

	/**
	 * @see org.xronos.openforge.lim.Visitor#visit(org.xronos.openforge.lim.RegisterReferee)
	 */
	@Override
	public void visit(RegisterReferee regReferee) {
		regReferee.setIDLogical("regReferee");

		if (DEBUG) {
			debugln(ID.showDebug(regReferee) + " given logical name \""
					+ ID.showLogical(regReferee) + "\"");
		}

		super.visit(regReferee);
	}

	/**
	 * @see org.xronos.openforge.lim.Visitor#visit(org.xronos.openforge.lim.memory.MemoryRead)
	 */
	@Override
	public void visit(MemoryRead memRead) {
		memRead.setIDLogical("memRead");

		if (DEBUG) {
			debugln(ID.showDebug(memRead) + " given logical name \""
					+ ID.showLogical(memRead) + "\"");
		}

		super.visit(memRead);
	}

	/**
	 * @see org.xronos.openforge.lim.Visitor#visit(org.xronos.openforge.lim.memory.MemoryWrite)
	 */
	@Override
	public void visit(MemoryWrite memWrite) {
		memWrite.setIDLogical("memWrite");

		if (DEBUG) {
			debugln(ID.showDebug(memWrite) + " given logical name \""
					+ ID.showLogical(memWrite) + "\"");
		}

		super.visit(memWrite);
	}

	/**
	 * @see org.xronos.openforge.lim.Visitor#visit(org.xronos.openforge.lim.memory.MemoryReferee)
	 */
	@Override
	public void visit(MemoryReferee memReferee) {
		memReferee.setIDLogical("memReferee");

		if (DEBUG) {
			debugln(ID.showDebug(memReferee) + " given logical name \""
					+ ID.showLogical(memReferee) + "\"");
		}

		super.visit(memReferee);
	}

	/**
	 * @see org.xronos.openforge.lim.Visitor#visit(org.xronos.openforge.lim.memory.MemoryGateway)
	 */
	@Override
	public void visit(MemoryGateway memGateway) {
		memGateway.setIDLogical("memGateway");

		if (DEBUG) {
			debugln(ID.showDebug(memGateway) + " given logical name \""
					+ ID.showLogical(memGateway) + "\"");
		}

		super.visit(memGateway);
	}

	/**
	 * @see org.xronos.openforge.lim.Visitor#visit(org.xronos.openforge.lim.HeapRead)
	 */
	@Override
	public void visit(HeapRead heapRead) {
		heapRead.setIDLogical("heapRead");

		if (DEBUG) {
			debugln(ID.showDebug(heapRead) + " given logical name \""
					+ ID.showLogical(heapRead) + "\"");
		}

		super.visit(heapRead);
	}

	/**
	 * @see org.xronos.openforge.lim.Visitor#visit(org.xronos.openforge.lim.ArrayRead)
	 */
	@Override
	public void visit(ArrayRead arrayRead) {
		arrayRead.setIDLogical("arrayRead");

		if (DEBUG) {
			debugln(ID.showDebug(arrayRead) + " given logical name \""
					+ ID.showLogical(arrayRead) + "\"");
		}

		super.visit(arrayRead);
	}

	/**
	 * @see org.xronos.openforge.lim.Visitor#visit(org.xronos.openforge.lim.ArrayRead)
	 */
	@Override
	public void visit(AbsoluteMemoryRead read) {
		read.setIDLogical("absoluteRead");

		if (DEBUG) {
			debugln(ID.showDebug(read) + " given logical name \""
					+ ID.showLogical(read) + "\"");
		}

		super.visit(read);
	}

	/**
	 * @see org.xronos.openforge.lim.Visitor#visit(org.xronos.openforge.lim.HeapWrite)
	 */
	@Override
	public void visit(HeapWrite heapWrite) {
		heapWrite.setIDLogical("heapWrite");

		if (DEBUG) {
			debugln(ID.showDebug(heapWrite) + " given logical name \""
					+ ID.showLogical(heapWrite) + "\"");
		}

		super.visit(heapWrite);
	}

	/**
	 * @see org.xronos.openforge.lim.Visitor#visit(org.xronos.openforge.lim.ArrayWrite)
	 */
	@Override
	public void visit(ArrayWrite arrayWrite) {
		arrayWrite.setIDLogical("arrayWrite");

		if (DEBUG) {
			debugln(ID.showDebug(arrayWrite) + " given logical name \""
					+ ID.showLogical(arrayWrite) + "\"");
		}

		super.visit(arrayWrite);
	}

	/**
	 * @see org.xronos.openforge.lim.Visitor#visit(org.xronos.openforge.lim.ArrayWrite)
	 */
	@Override
	public void visit(AbsoluteMemoryWrite write) {
		write.setIDLogical("absoluteWrite");

		if (DEBUG) {
			debugln(ID.showDebug(write) + " given logical name \""
					+ ID.showLogical(write) + "\"");
		}

		super.visit(write);
	}

	/**
	 * @see org.xronos.openforge.lim.Visitor#visit(org.xronos.openforge.lim.PinRead)
	 */
	@Override
	public void visit(PinRead pinRead) {
		pinRead.setIDLogical("pinRead");

		if (DEBUG) {
			debugln(ID.showDebug(pinRead) + " given logical name \""
					+ ID.showLogical(pinRead) + "\"");
		}

		super.visit(pinRead);
	}

	/**
	 * @see org.xronos.openforge.lim.Visitor#visit(org.xronos.openforge.lim.PinWrite)
	 */
	@Override
	public void visit(PinWrite pinWrite) {
		pinWrite.setIDLogical("pinWrite");

		if (DEBUG) {
			debugln(ID.showDebug(pinWrite) + " given logical name \""
					+ ID.showLogical(pinWrite) + "\"");
		}

		super.visit(pinWrite);
	}

	/**
	 * @see org.xronos.openforge.lim.Visitor#visit(org.xronos.openforge.lim.PinStateChange)
	 */
	@Override
	public void visit(PinStateChange pinChange) {
		pinChange.setIDLogical("pinChange");

		if (DEBUG) {
			debugln(ID.showDebug(pinChange) + " given logical name \""
					+ ID.showLogical(pinChange) + "\"");
		}

		super.visit(pinChange);
	}

	/**
	 * @see org.xronos.openforge.lim.Visitor#visit(org.xronos.openforge.lim.primitive.SRL16)
	 */
	@Override
	public void visit(SRL16 srl16) {
		srl16.setIDLogical("srl16");

		if (DEBUG) {
			debugln(ID.showDebug(srl16) + " given logical name \""
					+ ID.showLogical(srl16) + "\"");
		}

		super.visit(srl16);
	}

	/**
	 * @see org.xronos.openforge.lim.Visitor#visit(org.xronos.openforge.lim.PinReferee)
	 */
	@Override
	public void visit(PinReferee pinReferee) {
		pinReferee.setIDLogical("pinReferee");

		if (DEBUG) {
			debugln(ID.showDebug(pinReferee) + " given logical name \""
					+ ID.showLogical(pinReferee) + "\"");
		}

		super.visit(pinReferee);
	}

	/**
	 * @see org.xronos.openforge.lim.Visitor#visit(org.xronos.openforge.lim.TriBuf)
	 */
	@Override
	public void visit(TriBuf tbuf) {
		tbuf.setIDLogical("tbuf");

		if (DEBUG) {
			debugln(ID.showDebug(tbuf) + " given logical name \""
					+ ID.showLogical(tbuf) + "\"");
		}

		super.visit(tbuf);
	}

	@Override
	public void visit(FifoAccess comp) {
		comp.setIDLogical("fifoAccess");

		if (DEBUG) {
			debugln(ID.showDebug(comp) + " given logical name \""
					+ ID.showLogical(comp) + "\"");
		}

		super.visit(comp);
	}

	@Override
	public void visit(FifoRead comp) {
		comp.setIDLogical("fifoRead");

		if (DEBUG) {
			debugln(ID.showDebug(comp) + " given logical name \""
					+ ID.showLogical(comp) + "\"");
		}

		super.visit(comp);
	}

	@Override
	public void visit(FifoWrite comp) {
		comp.setIDLogical("fifoWrite");

		if (DEBUG) {
			debugln(ID.showDebug(comp) + " given logical name \""
					+ ID.showLogical(comp) + "\"");
		}

		super.visit(comp);
	}

	/**
	 * @see org.xronos.openforge.lim.FilteredVisitor#filterAny(org.xronos.openforge.lim.Component)
	 */
	@Override
	public void filterAny(Component c) {
		String baseName = c.showIDLogical();
		for (Exit exit : c.getExits()) {
			int instanceCount = 0;
			for (Bus bus : exit.getDataBuses()) {
				bus.setIDLogical((instanceCount > 0) ? new String(baseName
						+ instanceCount) : baseName);
			}
		}
	}

	private String uniquifyCoreName(String name) {
		if (topModuleName.length() > 0) {
			return topModuleName + "_" + name;
		}
		return name;
	}

	/**
	 * Private convenience method for printing debug.
	 * 
	 * @param string
	 */
	private void debugln(String string) {
		System.out.println(string);
	}

	private class DesignElementNamingVisitor extends FilteredVisitor {

		@Override
		public void visit(Design design) {
			// Avoid all task calls...
			Set<Call> taskCalls = new HashSet<Call>();
			for (Task task : design.getTasks()) {
				taskCalls.add(task.getCall());
			}

			List<Component> elements = new ArrayList<Component>(design
					.getDesignModule().getComponents());

			while (!elements.isEmpty()) {
				Visitable vis = elements.remove(0);
				if (!taskCalls.contains(vis)) {
					try {
						vis.accept(this);
					} catch (UnexpectedVisitationException uve) {
						// Register.Physical and other physical
						// implementation modules may not be expecting
						// visitation, which is OK.
						if (vis instanceof Register.Physical) {
							filter((Module) vis);
							elements.addAll(((Module) vis).getComponents());
						} else if (vis instanceof StructuralMemory) {
							filter((Module) vis);
							elements.addAll(((Module) vis).getComponents());
						}
					}
				}
			}
		}

		@Override
		public void filter(Module mod) {
			// It will likely be instantiated as an hdl module.
			// Give it a name that is unique to this compilation
			String idLogical = mod.showIDLogical();
			if (mod.getSourceName() != null) {
				idLogical = idLogical.replaceAll("_" + mod.getSourceName(), "");
			}
			mod.setIDLogical(uniquifyCoreName(idLogical));
		}

		@Override
		public void visit(MemoryBank vis) {
			String moduleName = "forge_memory_" + vis.getDepth() + "x"
					+ vis.getWidth();
			vis.setIDLogical(uniquifyCoreName(moduleName));
		}
	}

}
