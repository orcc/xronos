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
import net.sf.orc2hdl.design.util.XronosIrUtil;
import net.sf.orc2hdl.design.visitors.ComponentCreator;
import net.sf.orc2hdl.ir.InstPortRead;
import net.sf.orc2hdl.ir.InstPortStatus;
import net.sf.orcc.df.Actor;
import net.sf.orcc.df.Port;
import net.sf.orcc.df.util.DfVisitor;
import net.sf.orcc.ir.Block;
import net.sf.orcc.ir.BlockBasic;
import net.sf.orcc.ir.BlockIf;
import net.sf.orcc.ir.BlockWhile;
import net.sf.orcc.ir.ExprBinary;
import net.sf.orcc.ir.ExprBool;
import net.sf.orcc.ir.ExprInt;
import net.sf.orcc.ir.Expression;
import net.sf.orcc.ir.InstLoad;
import net.sf.orcc.ir.InstReturn;
import net.sf.orcc.ir.InstStore;
import net.sf.orcc.ir.Instruction;
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

	private IrFactory irFactory = IrFactory.eINSTANCE;

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
		// Add all circularBuffer locals to read procedure
		circularBuffer.addToLocals(read);

		// Create a Boolean type and int32 type
		Type typeBool = irFactory.createTypeBool();
		Type typeInt32 = irFactory.createTypeInt(32);

		/** Create the true loop body **/
		List<Block> trueLoopBody = new ArrayList<Block>();

		// Create a BlockBasic, that contains the portStatus and the cbStart
		BlockBasic statusAndStartBlock = irFactory.createBlockBasic();

		// PortStatus(portStatus,port)
		Var varPortStatus = irFactory.createVar(typeBool,
				"portStatus_" + port.getName(), true, 0);
		read.getLocals().add(varPortStatus);
		InstPortStatus instPortStatus = XronosIrUtil.creaInstPortStatus(
				varPortStatus, port);

		// Load(cbTmpStart, cbStart)
		Var cbStart = circularBuffer.getStart();
		Var cbTmpStart = circularBuffer.getTmpStart();
		InstLoad startLoad = irFactory.createInstLoad(cbTmpStart, cbStart);

		statusAndStartBlock.add(instPortStatus);
		statusAndStartBlock.add(startLoad);

		// Add to the true loop body
		trueLoopBody.add(statusAndStartBlock);

		/** Create the start loop body **/
		Expression startLoopCondition = XronosIrUtil.createExprBinaryLogicAnd(
				cbTmpStart, varPortStatus);

		List<Block> startLoopBody = new ArrayList<Block>();

		// Create a blockBasic that contains the load of cbHead,cbCount,
		// portRead, and store of the cb
		BlockBasic initAndRead = irFactory.createBlockBasic();

		// Load(cbTmpHead, cbHead)
		Var cbHead = circularBuffer.getHead();
		Var cbTmpHead = circularBuffer.getTmpHead();
		InstLoad headLoad = irFactory.createInstLoad(cbTmpHead, cbHead);

		// Load(cbTmpCount, cbCount)
		Var cbCount = circularBuffer.getCount();
		Var cbTmpCount = circularBuffer.getTmpCount();
		InstLoad countLoad = irFactory.createInstLoad(cbTmpCount, cbCount);

		// PortRead(token, port)
		Var token = irFactory.createVar(port.getType(),
				"token_" + port.getName(), true, 0);
		read.getLocals().add(token);
		InstPortRead portRead = XronosIrUtil.creaInstPortRead(token, port);

		// Store( circularBuffer[cbHead + cbCount & (cbSize-1), token)
		Integer cbSize = circularBuffer.getSize();
		ExprInt eiSizeMinusOne = irFactory.createExprInt(cbSize - 1);

		ExprBinary ebHeadPlusCount = XronosIrUtil.createExprBinaryPlus(
				cbTmpHead, cbTmpCount, typeInt32);

		Expression cbIndex = irFactory.createExprBinary(ebHeadPlusCount,
				OpBinary.LOGIC_AND, eiSizeMinusOne, typeInt32);

		Var buffer = circularBuffer.getBuffer();

		InstStore bufferStore = irFactory.createInstStore(buffer,
				Arrays.asList(cbIndex), token);

		initAndRead.add(headLoad);
		initAndRead.add(countLoad);
		initAndRead.add(portRead);
		initAndRead.add(bufferStore);

		// Add initAndRead block to start loop body
		startLoopBody.add(initAndRead);

		/** Create the stop if **/

		// Create the stop condition
		Expression condition = XronosIrUtil.createExprBinaryEqual(cbTmpCount,
				cbSize - 1);

		// Then instructions
		List<Instruction> thenInstructions = new ArrayList<Instruction>();

		ExprBinary ebHeadPlusOne = XronosIrUtil.createExprBinaryPlus(cbTmpHead,
				1, typeInt32);
		Expression ebHeadPlusOneAndSizeMinusOne = XronosIrUtil
				.createExprBinaryBitAnd(ebHeadPlusOne, cbSize - 1, typeInt32);
		InstStore headStore = irFactory.createInstStore(cbHead,
				ebHeadPlusOneAndSizeMinusOne);

		ExprBool exprFalse = irFactory.createExprBool(false);
		InstStore startStore = irFactory.createInstStore(cbStart, exprFalse);

		thenInstructions.add(headStore);
		thenInstructions.add(startStore);

		// Else instructions
		List<Instruction> elseInstructions = new ArrayList<Instruction>();

		Expression countPlusOne = XronosIrUtil.createExprBinaryPlus(cbTmpCount,
				1, typeInt32);

		InstStore countStore = IrFactory.eINSTANCE.createInstStore(cbCount,
				countPlusOne);
		elseInstructions.add(countStore);

		// Create the if block
		BlockIf stopIf = XronosIrUtil.createBlockIf(condition,
				thenInstructions, elseInstructions);

		// Add the if block to the start loop body
		startLoopBody.add(stopIf);

		/** Create the Start Loop **/
		BlockWhile startLoop = XronosIrUtil.createBlockWhile(
				startLoopCondition, startLoopBody);

		// Add startLoop to the true loop body
		trueLoopBody.add(startLoop);

		/** Create the true loop body **/
		BlockWhile trueLoop = XronosIrUtil.createTrueBlockWhile(trueLoopBody);
		read.getBlocks().add(trueLoop);

		/** Create Return Block **/
		BlockBasic returnBlock = IrFactory.eINSTANCE.createBlockBasic();
		InstReturn instReturn = IrFactory.eINSTANCE.createInstReturn(null);
		returnBlock.add(instReturn);
		read.getBlocks().add(returnBlock);
		Type returnType = IrFactory.eINSTANCE.createTypeVoid();
		read.setReturnType(returnType);
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
