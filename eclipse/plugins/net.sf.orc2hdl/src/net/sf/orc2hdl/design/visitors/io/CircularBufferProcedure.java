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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sf.openforge.lim.Component;
import net.sf.openforge.lim.Design;
import net.sf.openforge.lim.Exit;
import net.sf.openforge.lim.Module;
import net.sf.openforge.lim.Task;
import net.sf.orc2hdl.design.ResourceCache;
import net.sf.orc2hdl.design.ResourceDependecies;
import net.sf.orc2hdl.design.util.DesignUtil;
import net.sf.orc2hdl.design.util.ModuleUtil;
import net.sf.orc2hdl.design.visitors.ComponentCreator;
import net.sf.orc2hdl.ir.InstPortStatus;
import net.sf.orc2hdl.ir.XronosIrSpecificFactory;
import net.sf.orcc.df.Actor;
import net.sf.orcc.df.Port;
import net.sf.orcc.df.util.DfVisitor;
import net.sf.orcc.ir.BlockBasic;
import net.sf.orcc.ir.BlockIf;
import net.sf.orcc.ir.Def;
import net.sf.orcc.ir.ExprVar;
import net.sf.orcc.ir.Expression;
import net.sf.orcc.ir.InstLoad;
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

	private List<Procedure> procedures;

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

		/** Block If Start **/
		BlockIf blockIfStart = IrFactory.eINSTANCE.createBlockIf();
		ExprVar exprtmpPortStatus = IrFactory.eINSTANCE
				.createExprVar(tmpPortStatus);
		ExprVar exprtTmpStart = IrFactory.eINSTANCE.createExprVar(cbTmpStart);

		Expression conditionStart = IrFactory.eINSTANCE.createExprBinary(
				exprtTmpStart, OpBinary.LOGIC_AND, exprtmpPortStatus, typeBool);
		// Set If start condition
		blockIfStart.setCondition(conditionStart);

		// get tmpCount and tmpRequestSize and put them to the first Then Block
		// basic
		BlockBasic thenIfStartFirstBlock = IrFactory.eINSTANCE
				.createBlockBasic();
		Var cbTmpCount = circularBuffer.getTmpCount();
		Var cbCount = circularBuffer.getCount();
		InstLoad instLoadCount = IrFactory.eINSTANCE.createInstLoad(cbTmpCount,
				cbCount);
		thenIfStartFirstBlock.add(instLoadCount);

		Var cbTmpRequestSize = circularBuffer.getTmpRequestSize();
		Var cbRequestSize = circularBuffer.getRequestSize();
		InstLoad instLoadRequestSize = IrFactory.eINSTANCE.createInstLoad(
				cbTmpRequestSize, cbRequestSize);
		thenIfStartFirstBlock.add(instLoadRequestSize);

		// Block If request Size

		// Block If full
		// Put the block to the procedure

		return null;
	}

	private Procedure createWriteProcedure(Port port,
			CircularBuffer circularBuffer) {
		return null;
	}

}
