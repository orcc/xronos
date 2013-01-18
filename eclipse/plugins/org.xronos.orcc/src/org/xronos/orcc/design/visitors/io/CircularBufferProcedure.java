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
package org.xronos.orcc.design.visitors.io;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sf.orcc.df.Actor;
import net.sf.orcc.df.Port;
import net.sf.orcc.df.util.DfVisitor;
import net.sf.orcc.ir.Block;
import net.sf.orcc.ir.BlockBasic;
import net.sf.orcc.ir.BlockWhile;
import net.sf.orcc.ir.ExprBinary;
import net.sf.orcc.ir.ExprInt;
import net.sf.orcc.ir.Expression;
import net.sf.orcc.ir.InstAssign;
import net.sf.orcc.ir.InstLoad;
import net.sf.orcc.ir.InstReturn;
import net.sf.orcc.ir.InstStore;
import net.sf.orcc.ir.IrFactory;
import net.sf.orcc.ir.OpBinary;
import net.sf.orcc.ir.Procedure;
import net.sf.orcc.ir.Type;
import net.sf.orcc.ir.Var;

import org.xronos.openforge.lim.Component;
import org.xronos.openforge.lim.Design;
import org.xronos.openforge.lim.Exit;
import org.xronos.openforge.lim.Module;
import org.xronos.openforge.lim.Task;
import org.xronos.orcc.backend.debug.DebugPrinter;
import org.xronos.orcc.backend.transform.XronosTransform;
import org.xronos.orcc.design.ResourceCache;
import org.xronos.orcc.design.ResourceDependecies;
import org.xronos.orcc.design.util.DesignUtil;
import org.xronos.orcc.design.util.ModuleUtil;
import org.xronos.orcc.design.util.XronosIrUtil;
import org.xronos.orcc.design.visitors.ComponentCreator;
import org.xronos.orcc.ir.InstPortRead;
import org.xronos.orcc.ir.InstPortStatus;

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

	public CircularBufferProcedure(Design design, ResourceCache resourceCache,
			ResourceDependecies resourceDependecies) {
		super();
		this.design = design;
		this.resourceCache = resourceCache;
		this.resourceDependecies = resourceDependecies;
		circularBufferPortMap = new HashMap<Port, CircularBuffer>();

	}

	@Override
	public Void caseActor(Actor actor) {
		this.actor = actor;
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
				// procedures.add(createWriteProcedure(port, circularBuffer));
			}
		}

		// Create a task for each Procedure
		for (Procedure procedure : procedures) {
			List<Component> taskComponents = new ArrayList<Component>();
			ComponentCreator componentCreator = new ComponentCreator(
					resourceCache, resourceDependecies);
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
		InstPortStatus instPortStatus = XronosIrUtil.createInstPortStatus(
				varPortStatus, port);

		// Load(cbTmpStart, cbStart)
		Var cbStart = circularBuffer.getStart();
		Var cbTmpStart = circularBuffer.getTmpStart();
		InstLoad startLoad = irFactory.createInstLoad(cbTmpStart, cbStart);

		// Load(cbTmpCount, cbCount)
		Var cbCount = circularBuffer.getCount();
		Var cbTmpCount = circularBuffer.getTmpCount();
		InstLoad countLoad = irFactory.createInstLoad(cbTmpCount, cbCount);

		statusAndStartBlock.add(instPortStatus);
		statusAndStartBlock.add(startLoad);
		statusAndStartBlock.add(countLoad);

		// Add to the true loop body
		trueLoopBody.add(statusAndStartBlock);

		/** Create the start loop body **/

		List<Block> startLoopBody = new ArrayList<Block>();

		// Create a blockBasic that contains the load of cbHead,cbCount,
		// portRead, and store of the cb
		BlockBasic initAndRead = irFactory.createBlockBasic();

		// Load(cbTmpHead, cbHead)
		Var cbHead = circularBuffer.getHead();
		Var cbTmpHead = circularBuffer.getTmpHead();
		InstLoad headLoad = irFactory.createInstLoad(cbTmpHead, cbHead);

		// PortRead(token, port)
		Var token = irFactory.createVar(port.getType(),
				"token_" + port.getName(), true, 0);
		read.getLocals().add(token);
		InstPortRead portRead = XronosIrUtil.createInstPortRead(token, port, true);

		// Store( circularBuffer[cbHead + cbCount & (cbSizePowTwo-1), token)
		Integer cbSize = circularBuffer.getSizePowTwo();
		ExprInt eiSizeMinusOne = irFactory.createExprInt(cbSize - 1);

		ExprBinary ebHeadPlusCount = XronosIrUtil.createExprBinaryPlus(
				cbTmpHead, cbTmpCount, typeInt32);

		Expression cbIndex = irFactory.createExprBinary(ebHeadPlusCount,
				OpBinary.LOGIC_AND, eiSizeMinusOne, typeInt32);

		Var buffer = circularBuffer.getBuffer();

		InstStore bufferStore = irFactory.createInstStore(buffer,
				Arrays.asList(cbIndex), token);

		Expression countPlusOne = XronosIrUtil.createExprBinaryPlus(cbTmpCount,
				1, typeInt32);

		InstAssign countAssign = irFactory.createInstAssign(cbTmpCount,
				countPlusOne);

		InstStore countStore = irFactory.createInstStore(cbCount, cbTmpCount);

		initAndRead.add(headLoad);
		initAndRead.add(portRead);
		initAndRead.add(bufferStore);
		initAndRead.add(countAssign);
		initAndRead.add(countStore);
		// Add initAndRead block to start loop body
		startLoopBody.add(initAndRead);

		/** Create the Start Loop **/

		// Create the stop condition
		Expression startStatusCondition = XronosIrUtil
				.createExprBinaryLogicAnd(cbTmpStart, varPortStatus);
		Expression isFull = XronosIrUtil.createExprBinaryLessThan(cbTmpCount,
				circularBuffer.getSize());

		Expression startLoopCondition = XronosIrUtil.createExprBinaryLogicAnd(
				startStatusCondition, isFull);

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

		// Debug
		DebugPrinter debugPrinter = new DebugPrinter();
		debugPrinter.printProcedure("/tmp", read,
				actor.getName() + "_" + read.getName());
		// Transform procedure
		XronosTransform transform = new XronosTransform(read);
		Procedure procedure = transform.transformProcedure(resourceCache);

		debugPrinter = new DebugPrinter();
		debugPrinter.printProcedure("/tmp", procedure, actor.getName() + "_"
				+ procedure.getName() + "_tr");

		return procedure;
	}
}
