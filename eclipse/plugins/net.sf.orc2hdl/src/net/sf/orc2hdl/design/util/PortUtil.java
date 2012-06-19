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

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import net.sf.openforge.frontend.slim.builder.ActionIOHandler;
import net.sf.openforge.frontend.slim.builder.ActionIOHandler.FifoIOHandler;
import net.sf.openforge.frontend.slim.builder.ActionIOHandler.NativeIOHandler;
import net.sf.openforge.lim.Bus;
import net.sf.openforge.lim.Component;
import net.sf.openforge.lim.Design;
import net.sf.openforge.lim.Exit;
import net.sf.openforge.lim.Port;
import net.sf.orc2hdl.design.ResourceCache;
import net.sf.orcc.df.Action;
import net.sf.orcc.df.Actor;
import net.sf.orcc.ir.IrFactory;
import net.sf.orcc.ir.Type;
import net.sf.orcc.ir.Var;

import org.eclipse.emf.common.util.EList;

/**
 * This class contains several methods for the Design Ports
 * 
 * @author Endri Bezati
 * 
 */

public class PortUtil {
	/**
	 * This method takes a List of an {@link Actor} I/O ports and it creates the
	 * associated {@link Design} {@link FifoIOHandler}
	 * 
	 * @param design
	 *            the design
	 * @param ports
	 *            list of the actor port
	 * @param direction
	 *            a string which indicates the direction
	 * @param resources
	 *            the resource cache
	 */
	public static void createDesignPorts(Design design,
			EList<net.sf.orcc.df.Port> ports, String direction,
			ResourceCache resources) {
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

	public static Component createPinReadComponent(Action action,
			net.sf.orcc.df.Port port, ResourceCache resources,
			Map<Bus, Var> busDependency, Map<Bus, Integer> doneBusDependency) {
		// Get the IOHandler of the actors port
		ActionIOHandler ioHandler = resources.getIOHandler(port);

		// Create the pinRead component
		Component pinRead = ioHandler.getReadAccess();

		// Get the Pin Read variable
		Var pinReaVar = action.getInputPattern().getPortToVarMap().get(port);

		// Map out Data Ports

		GroupedVar outVars = new GroupedVar(pinReaVar, 0);
		mapOutDataPorts(pinRead, outVars.getAsList(), busDependency,
				doneBusDependency);

		return pinRead;
	}

	public static Component createPinStatusComponent(net.sf.orcc.df.Port port,
			ResourceCache resources, Map<net.sf.orcc.df.Port, Var> pinStatus,
			Map<Bus, Var> busDependency, Map<Bus, Integer> doneBusDependency) {
		// Create pin Status component from the ioHandler of the Port
		ActionIOHandler ioHandler = resources.getIOHandler(port);
		Component pinStatusComponent = ioHandler.getStatusAccess();

		Type type = IrFactory.eINSTANCE.createTypeBool();
		Var pinStatusVar = IrFactory.eINSTANCE.createVar(0, type,
				port.getName() + "_pinStatus", false, 0);
		pinStatus.put(port, pinStatusVar);

		// Map out pinStatusVar to pinStatusComponent

		GroupedVar outVars = new GroupedVar(pinStatusVar, 0);
		PortUtil.mapOutDataPorts(pinStatusComponent, outVars.getAsList(),
				busDependency, doneBusDependency);

		return pinStatusComponent;
	}

	public static Component createPinWriteComponent(Action action,
			net.sf.orcc.df.Port port, ResourceCache resources,
			Map<Port, Var> portDependency,
			Map<Port, Integer> groupPortDependency,
			Map<Bus, Integer> doneBusDependency) {
		// Get the IOHandler of the actors port
		ActionIOHandler ioHandler = resources.getIOHandler(port);

		// Create the pinWrite component
		Component pinWrite = ioHandler.getWriteAccess();

		// Get the Pin Write variable
		Var pinWriteVar = action.getOutputPattern().getPortToVarMap().get(port);

		// Map in Data Ports

		GroupedVar inVars = new GroupedVar(pinWriteVar, 0);
		mapInDataPorts(pinWrite, inVars.getAsList(), portDependency,
				groupPortDependency);

		// Map out Control Port
		mapOutControlPort(pinWrite, 0, doneBusDependency);

		return pinWrite;
	}

	public static void mapInDataPorts(Component component,
			List<GroupedVar> inVars, Map<Port, Var> portDependency,
			Map<Port, Integer> portGroupDependency) {

		Iterator<Port> portIter = component.getDataPorts().iterator();

		for (GroupedVar groupedVar : inVars) {
			Var var = groupedVar.getVar();
			Integer groupPort = groupedVar.getGroup();
			Port dataPort = portIter.next();
			dataPort.setIDLogical(var.getIndexedName());
			dataPort.setSize(var.getType().getSizeInBits(), var.getType()
					.isInt() || var.getType().isBool());
			// Put Input Port dependency
			portDependency.put(dataPort, var);
			portGroupDependency.put(dataPort, groupPort);
		}
	}

	public static void mapOutControlPort(Component component, Integer group,
			Map<Bus, Integer> doneBusDependency) {
		Bus doneBus = component.getExit(Exit.DONE).getDoneBus();
		doneBusDependency.put(doneBus, group);
	}

	public static void mapOutDataPorts(Component component,
			List<GroupedVar> outVars, Map<Bus, Var> busDependency,
			Map<Bus, Integer> doneBusDependency) {

		Integer group = 0;
		for (GroupedVar groupedVar : outVars) {
			Var var = groupedVar.getVar();
			group = groupedVar.getGroup();
			// Get the component dataBus
			Bus dataBus = component.getExit(Exit.DONE).getDataBuses()
					.get(group);

			// Set the bus value
			if (dataBus.getValue() == null) {
				dataBus.setSize(var.getType().getSizeInBits(), var.getType()
						.isInt() || var.getType().isBool());
			}
			// Name the dataBus
			dataBus.setIDLogical(var.getIndexedName());
			busDependency.put(dataBus, var);
		}
		// Map Out done Bus
		mapOutControlPort(component, group, doneBusDependency);
	}

}
