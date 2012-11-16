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
package net.sf.orc2hdl.design.visitors.io;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sf.openforge.lim.Component;
import net.sf.openforge.lim.Design;
import net.sf.openforge.lim.Exit;
import net.sf.openforge.lim.Module;
import net.sf.openforge.lim.Task;
import net.sf.orc2hdl.backend.transform.XronosTransform;
import net.sf.orc2hdl.design.ResourceCache;
import net.sf.orc2hdl.design.ResourceDependecies;
import net.sf.orc2hdl.design.util.DesignUtil;
import net.sf.orc2hdl.design.util.ModuleUtil;
import net.sf.orc2hdl.design.visitors.ComponentCreator;
import net.sf.orc2hdl.ir.InstPortRead;
import net.sf.orc2hdl.ir.InstPortStatus;
import net.sf.orc2hdl.ir.XronosIrSpecificFactory;
import net.sf.orcc.df.Actor;
import net.sf.orcc.df.Port;
import net.sf.orcc.df.util.DfVisitor;
import net.sf.orcc.ir.BlockBasic;
import net.sf.orcc.ir.BlockIf;
import net.sf.orcc.ir.BlockWhile;
import net.sf.orcc.ir.Def;
import net.sf.orcc.ir.ExprBool;
import net.sf.orcc.ir.ExprInt;
import net.sf.orcc.ir.ExprVar;
import net.sf.orcc.ir.Expression;
import net.sf.orcc.ir.InstLoad;
import net.sf.orcc.ir.InstStore;
import net.sf.orcc.ir.IrFactory;
import net.sf.orcc.ir.OpBinary;
import net.sf.orcc.ir.Procedure;
import net.sf.orcc.ir.Type;
import net.sf.orcc.ir.Var;

/**
 * 
 * @author Endri Bezati
 * 
 */
public class CircularBufferProcedure extends DfVisitor<Void> {

	private Map<Port, CircularBuffer> circularBufferPortMap;

	private Design design;

	private ResourceCache resourceCache;

	private ResourceDependecies resourceDependecies;

	/** Component Creator (Instruction Visitor) **/
	private final ComponentCreator componentCreator;

	public CircularBufferProcedure(Design design, ResourceCache resourceCache,
			ResourceDependecies resourceDependecies) {
		super();
		this.design = design;
		this.resourceCache = resourceCache;
		this.resourceDependecies = resourceDependecies;
		circularBufferPortMap = new HashMap<Port, CircularBuffer>();
		componentCreator = new ComponentCreator(resourceCache,
				resourceDependecies);
	}

	@Override
	public Void caseActor(Actor actor) {
		List<Procedure> procedures = new ArrayList<Procedure>();
		// Get Input Ports
		for (Port port : actor.getInputs()) {
			if (resourceCache.getActorInputCircularBuffer(actor).get(port) != null) {
				CircularBuffer circularBuffer = resourceCache
						.getActorInputCircularBuffer(actor).get(port);
				circularBufferPortMap.put(port, circularBuffer);
				procedures.add(createReadProcedure(port, circularBuffer));
			}
		}
		// Get Output Ports
		for (Port port : actor.getOutputs()) {
			if (resourceCache.getActorOutputCircularBuffer(actor).get(port) != null) {
				CircularBuffer circularBuffer = resourceCache
						.getActorOutputCircularBuffer(actor).get(port);
				circularBufferPortMap.put(port, circularBuffer);
				procedures.add(createWriteProcedure(port, circularBuffer));
			}
		}

		// Create a task for each Procedure
		for (Procedure procedure : procedures) {
			List<Component> taskComponents = new ArrayList<Component>();
			taskComponents = componentCreator.doSwitch(procedure);

			Module taskModule = (Module) ModuleUtil.createModule(
					taskComponents, Collections.<Var> emptyList(),
					Collections.<Var> emptyList(),
					procedure.getName() + "Body", false, Exit.RETURN, 0,
					resourceDependecies.getPortDependency(),
					resourceDependecies.getBusDependency(),
					resourceDependecies.getPortGroupDependency(),
					resourceDependecies.getDoneBusDependency());

			Task task = DesignUtil.createTask(procedure.getName(), taskModule,
					true);
			design.addTask(task);
		}

		return null;
	}

	private Procedure createReadProcedure(Port port,
			CircularBuffer circularBuffer) {
		String name = port.getName();
		Procedure read = IrFactory.eINSTANCE.createProcedure(
				"circularBufferRead_" + name, 0,
				IrFactory.eINSTANCE.createTypeVoid());
		/** Get portStatus **/
		BlockBasic blockRead = IrFactory.eINSTANCE.createBlockBasic();
		// Create InstPortStatus
		InstPortStatus instPortStatus = XronosIrSpecificFactory.eINSTANCE
				.createInstPortStatus();
		Type typeBool = IrFactory.eINSTANCE.createTypeBool();
		Var tmpPortStatus = IrFactory.eINSTANCE.createVar(typeBool,
				"tmpPortStatus_" + name, true, 0);
		read.getLocals().add(tmpPortStatus);

		Def defPortStatus = IrFactory.eINSTANCE.createDef(tmpPortStatus);
		instPortStatus.setPort(port);
		instPortStatus.setTarget(defPortStatus);
		blockRead.add(instPortStatus);

		/** Get circularBuffer start **/
		Var cbStart = circularBuffer.getStart();
		Var cbTmpStart = circularBuffer.getTmpStart();
		read.getLocals().add(cbTmpStart);
		InstLoad loadStart = IrFactory.eINSTANCE.createInstLoad(cbTmpStart,
				cbStart);
		blockRead.add(loadStart);
		read.getBlocks().add(blockRead);

		/** Block while Start **/
		BlockWhile blockWhileStart = IrFactory.eINSTANCE.createBlockWhile();
		blockWhileStart.setJoinBlock(IrFactory.eINSTANCE.createBlockBasic());
		ExprVar exprtmpPortStatus = IrFactory.eINSTANCE
				.createExprVar(tmpPortStatus);
		ExprVar exprtTmpStart = IrFactory.eINSTANCE.createExprVar(cbTmpStart);

		Expression conditionStart = IrFactory.eINSTANCE.createExprBinary(
				exprtTmpStart, OpBinary.LOGIC_AND, exprtmpPortStatus, typeBool);
		// Set If start condition
		blockWhileStart.setCondition(conditionStart);

		// get tmpCount and tmpRequestSize and put them to the first Then Block
		// basic
		BlockBasic whileBodyFirstBlock = IrFactory.eINSTANCE.createBlockBasic();
		Var cbTmpCount = circularBuffer.getTmpCount();
		Var cbCount = circularBuffer.getCount();
		InstLoad instLoadCount = IrFactory.eINSTANCE.createInstLoad(cbTmpCount,
				cbCount);
		whileBodyFirstBlock.add(instLoadCount);

		Var cbTmpRequestSize = circularBuffer.getTmpRequestSize();
		Var cbRequestSize = circularBuffer.getRequestSize();
		InstLoad instLoadRequestSize = IrFactory.eINSTANCE.createInstLoad(
				cbTmpRequestSize, cbRequestSize);
		whileBodyFirstBlock.add(instLoadRequestSize);
		blockWhileStart.getBlocks().add(whileBodyFirstBlock);

		/** Block If requestSize **/
		BlockIf ifRequest = IrFactory.eINSTANCE.createBlockIf();
		ifRequest.setJoinBlock(IrFactory.eINSTANCE.createBlockBasic());

		// Block If requestSize condition
		ExprVar evCbTmpCount = IrFactory.eINSTANCE.createExprVar(cbTmpCount);
		ExprVar evCbTmpRequestSize = IrFactory.eINSTANCE
				.createExprVar(cbTmpRequestSize);
		Expression exprBlockIfReqiuest = IrFactory.eINSTANCE.createExprBinary(
				evCbTmpCount, OpBinary.LT, evCbTmpRequestSize, typeBool);
		ifRequest.setCondition(exprBlockIfReqiuest);

		// Block If thenBlock
		BlockBasic tIfRequest = IrFactory.eINSTANCE.createBlockBasic();
		Var cbHead = circularBuffer.getHead();
		Var cbTmpHead = circularBuffer.getTmpHead();
		InstLoad instLoadHead = IrFactory.eINSTANCE.createInstLoad(cbTmpHead,
				cbHead);
		tIfRequest.add(instLoadHead);

		// Create index end Expression
		Type typeInt32 = IrFactory.eINSTANCE.createTypeInt(32);
		ExprVar evCbTmpHead = IrFactory.eINSTANCE.createExprVar(cbTmpHead);
		ExprVar evCbTmpCount2 = IrFactory.eINSTANCE.createExprVar(cbTmpCount);
		Expression headPlusCount = IrFactory.eINSTANCE.createExprBinary(
				evCbTmpHead, OpBinary.PLUS, evCbTmpCount2, typeInt32);
		ExprInt eISizeMinusOne = IrFactory.eINSTANCE
				.createExprInt(circularBuffer.getSize() - 1);
		Expression cbEnd = IrFactory.eINSTANCE.createExprBinary(headPlusCount,
				OpBinary.BITAND, eISizeMinusOne, typeInt32);

		// Create PortRead of the Token
		InstPortRead instPortRead = XronosIrSpecificFactory.eINSTANCE
				.createInstPortRead();
		Var token = IrFactory.eINSTANCE.createVar(port.getType(), "token_"
				+ port.getName(), true, 0);
		read.getLocals().add(token);
		Def defToken = IrFactory.eINSTANCE.createDef(token);
		instPortRead.setPort(port);
		instPortRead.setTarget(defToken);
		tIfRequest.add(instPortRead);
		// Create InstStore on circularBuffer
		InstStore storeCb = IrFactory.eINSTANCE.createInstStore(
				circularBuffer.getBuffer(), Arrays.asList(cbEnd), token);
		tIfRequest.add(storeCb);
		ifRequest.getThenBlocks().add(tIfRequest);

		/** Block If full **/
		BlockIf ifFull = IrFactory.eINSTANCE.createBlockIf();
		ifFull.setJoinBlock(IrFactory.eINSTANCE.createBlockBasic());

		// Create ifFull condition
		ExprVar evCbTmpCount3 = IrFactory.eINSTANCE.createExprVar(cbTmpCount);
		ExprInt eIcbSize = IrFactory.eINSTANCE.createExprInt(circularBuffer
				.getSize());
		Expression ifFullCondition = IrFactory.eINSTANCE.createExprBinary(
				evCbTmpCount3, OpBinary.EQ, eIcbSize, typeBool);
		ifFull.setCondition(ifFullCondition);
		// ifFull Then body
		BlockBasic tIfFull = IrFactory.eINSTANCE.createBlockBasic();

		ExprVar evCbTmpHead2 = IrFactory.eINSTANCE.createExprVar(cbTmpHead);
		Expression evCbHeadPlusOne = IrFactory.eINSTANCE.createExprBinary(
				evCbTmpHead2, OpBinary.PLUS,
				IrFactory.eINSTANCE.createExprInt(1), typeInt32);
		Expression evCbSizeMinusOne = IrFactory.eINSTANCE
				.createExprInt(circularBuffer.getSize() - 1);

		Expression eCbHeadStore = IrFactory.eINSTANCE.createExprBinary(
				evCbHeadPlusOne, OpBinary.BITAND, evCbSizeMinusOne, typeInt32);
		InstStore instStoreHead = IrFactory.eINSTANCE.createInstStore(cbHead,
				eCbHeadStore);
		tIfFull.add(instStoreHead);
		ifFull.getThenBlocks().add(tIfFull);

		ifRequest.getThenBlocks().add(ifFull);

		// ifFull Else body
		BlockBasic eIfFull = IrFactory.eINSTANCE.createBlockBasic();
		ExprVar evCbTmpCount4 = IrFactory.eINSTANCE.createExprVar(cbTmpCount);
		Expression tmpCountPlusOne = IrFactory.eINSTANCE.createExprBinary(
				evCbTmpCount4, OpBinary.PLUS,
				IrFactory.eINSTANCE.createExprInt(1), typeInt32);
		InstStore instStoreCount = IrFactory.eINSTANCE.createInstStore(cbCount,
				tmpCountPlusOne);
		eIfFull.add(instStoreCount);
		ifFull.getElseBlocks().add(eIfFull);

		// Add ifFull to ifRequest
		ifRequest.getThenBlocks().add(ifFull);

		// Block If elseBlock
		BlockBasic eIfRequest = IrFactory.eINSTANCE.createBlockBasic();
		ExprBool exprFalse = IrFactory.eINSTANCE.createExprBool(false);
		InstStore instStoreStart = IrFactory.eINSTANCE.createInstStore(cbStart,
				exprFalse);
		eIfRequest.add(instStoreStart);
		ifRequest.getElseBlocks().add(eIfRequest);

		blockWhileStart.getBlocks().add(ifRequest);
		// Put the block to the procedure
		read.getBlocks().add(blockWhileStart);

		// Transform procedure
		XronosTransform transform = new XronosTransform(read);
		Procedure procedure = transform.transformProcedure(resourceCache);

		return procedure;
	}

	private Procedure createWriteProcedure(Port port,
			CircularBuffer circularBuffer) {
		return null;
	}

}
