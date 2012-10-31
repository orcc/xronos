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
package net.sf.orc2hdl.design.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sf.openforge.lim.Block;
import net.sf.openforge.lim.Call;
import net.sf.openforge.lim.Component;
import net.sf.openforge.lim.ControlDependency;
import net.sf.openforge.lim.DataDependency;
import net.sf.openforge.lim.Design;
import net.sf.openforge.lim.Exit;
import net.sf.openforge.lim.Module;
import net.sf.openforge.lim.OffsetMemoryAccess;
import net.sf.openforge.lim.Port;
import net.sf.openforge.lim.Task;
import net.sf.openforge.lim.memory.Allocation;
import net.sf.openforge.lim.memory.Location;
import net.sf.openforge.lim.memory.LocationConstant;
import net.sf.openforge.lim.memory.LogicalMemory;
import net.sf.openforge.lim.memory.LogicalValue;
import net.sf.openforge.lim.op.AddOp;
import net.sf.openforge.lim.op.CastOp;
import net.sf.openforge.util.naming.ID;
import net.sf.openforge.util.naming.IDSourceInfo;
import net.sf.orc2hdl.design.ResourceCache;
import net.sf.orcc.ir.Var;

/**
 * This class contains several methods for building branches, loops and its
 * decisions
 * 
 * @author Endri Bezati
 * 
 */
public class DesignUtil {

	/**
	 * This method builds the necessary AddressBlock control of a Memory access
	 * 
	 * @param memAccess
	 *            the memory access
	 * @param targetLocation
	 *            the Location of the target
	 * @param otherComps
	 *            other components to add to the block
	 * @return
	 */
	public static Block buildAddressedBlock(OffsetMemoryAccess memAccess,
			Location targetLocation, List<Component> otherComps) {
		final LocationConstant locationConst = new LocationConstant(
				targetLocation, 32, targetLocation.getAbsoluteBase()
						.getLogicalMemory().getAddressStridePolicy());
		final AddOp adder = new AddOp();
		final CastOp cast = new CastOp(32, false);

		final Block block = new Block(false);
		final Exit done = block.makeExit(0, Exit.DONE);
		final List<Component> comps = new ArrayList<Component>();
		comps.add(locationConst);
		comps.add(cast);
		comps.add(adder);
		comps.add(memAccess);
		comps.addAll(otherComps);
		ModuleUtil.modulePopulate(block, comps);
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

	public static Call createCall(String name, Module module) {
		Block procedureBlock = (Block) module;
		net.sf.openforge.lim.Procedure proc = new net.sf.openforge.lim.Procedure(
				procedureBlock);
		Call call = proc.makeCall();
		proc.setIDSourceInfo(deriveIDSourceInfo(name));
		return call;
	}

	public static Task createTask(String taskName, Module taskModule,
			Boolean requiresKicker) {
		Task task = null;

		// create Call
		Call call = createCall(taskName, taskModule);
		call.setSourceName(taskName);
		call.setIDLogical(taskName);
		topLevelInitialization(call);
		// Create task
		task = new Task(call);
		task.setKickerRequired(requiresKicker);
		task.setSourceName(taskName);
		return task;
	}

	/**
	 * This method allocates each LogicalValue (State Variable) in a memory with
	 * a matching address stride. This provides consistency in the memories and
	 * allows for state vars to be co-located if area is of concern.
	 * 
	 * @param design
	 * @param stateVars
	 * @param resources
	 */
	public static void designAllocateMemory(Design design,
			Map<LogicalValue, Var> stateVars, Integer maxAddressWidth,
			ResourceCache resources) {
		Map<Integer, LogicalMemory> memories = new HashMap<Integer, LogicalMemory>();
		for (LogicalValue lvalue : stateVars.keySet()) {
			int stride = lvalue.getAddressStridePolicy().getStride();
			LogicalMemory mem = memories.get(stride);
			if (mem == null) {
				// 32 should be more than enough for max address
				// width
				mem = new LogicalMemory(maxAddressWidth);
				mem.createLogicalMemoryPort();
				design.addMemory(mem);
				// memories.put(stride, mem);
			}
			// Create a 'location' for the stateVar that is
			// appropriate for its type/size.
			Allocation location = mem.allocate(lvalue);
			Var stateVar = stateVars.get(lvalue);
			setAttributes(stateVar, location);
			resources.addLocation(stateVar, location);
		}
	}

	public static IDSourceInfo deriveIDSourceInfo(String name) {
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
	 * This method sets the sizes of the clock,reset and go ports of a call
	 * 
	 * @param call
	 *            the call
	 */
	public static void topLevelInitialization(Call call) {
		call.getClockPort().setSize(1, false);
		call.getResetPort().setSize(1, false);
		call.getGoPort().setSize(1, false);
	}

	public static void setAttributes(String tag, Component comp) {
		setAttributes(tag, comp, false);
	}

	public static void setAttributes(String tag, Component comp,
			Boolean Removable) {
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
	public static void setAttributes(Var var, ID comp) {
		comp.setSourceName(var.getName());
	}

}
