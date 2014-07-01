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
 * If you modify this Program, or any covered work, by linking or 
 * combining it with Eclipse libraries (or a modified version of that 
 * library), containing parts covered by the terms of EPL,
 * the licensors of this Program grant you additional permission to convey 
 * the resulting work. {Corresponding Source for a non-source form of such 
 * a combination shall include the source code for the parts of Eclipse 
 * libraries used as well as that of the  covered work.}
 */

package org.xronos.orcc.design.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sf.orcc.ir.Var;

import org.xronos.openforge.app.project.SearchLabel;
import org.xronos.openforge.lim.Block;
import org.xronos.openforge.lim.Call;
import org.xronos.openforge.lim.CodeLabel;
import org.xronos.openforge.lim.Component;
import org.xronos.openforge.lim.ControlDependency;
import org.xronos.openforge.lim.DataDependency;
import org.xronos.openforge.lim.Design;
import org.xronos.openforge.lim.Exit;
import org.xronos.openforge.lim.Module;
import org.xronos.openforge.lim.OffsetMemoryAccess;
import org.xronos.openforge.lim.Port;
import org.xronos.openforge.lim.Task;
import org.xronos.openforge.lim.memory.Allocation;
import org.xronos.openforge.lim.memory.Location;
import org.xronos.openforge.lim.memory.LocationConstant;
import org.xronos.openforge.lim.memory.LogicalMemory;
import org.xronos.openforge.lim.memory.LogicalValue;
import org.xronos.openforge.lim.op.AddOp;
import org.xronos.openforge.lim.op.CastOp;
import org.xronos.openforge.util.naming.ID;
import org.xronos.openforge.util.naming.IDSourceInfo;
import org.xronos.orcc.design.ResourceCache;

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
		org.xronos.openforge.lim.Procedure proc = new org.xronos.openforge.lim.Procedure(
				procedureBlock);
		Call call = proc.makeCall();
		proc.setIDSourceInfo(deriveIDSourceInfo(name));
		SearchLabel sl = new CodeLabel(proc, name);
		proc.setSearchLabel(sl);
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
				mem.setIDLogical("stateVar_" + stateVars.get(lvalue).getName());
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

}
