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
 */

package org.xronos.orcc.design.util;

import static net.sf.orcc.ir.util.IrUtil.getNameSSA;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import net.sf.orcc.df.Action;
import net.sf.orcc.df.Actor;
import net.sf.orcc.ir.IrFactory;
import net.sf.orcc.ir.Type;
import net.sf.orcc.ir.Var;

import org.eclipse.emf.common.util.EList;
import org.xronos.openforge.frontend.slim.builder.ActionIOHandler;
import org.xronos.openforge.frontend.slim.builder.ActionIOHandler.FifoIOHandler;
import org.xronos.openforge.frontend.slim.builder.ActionIOHandler.NativeIOHandler;
import org.xronos.openforge.lim.Bus;
import org.xronos.openforge.lim.Component;
import org.xronos.openforge.lim.Design;
import org.xronos.openforge.lim.Exit;
import org.xronos.openforge.lim.Port;
import org.xronos.openforge.lim.io.FifoID;
import org.xronos.orcc.design.ResourceCache;

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
								.getType().getSizeInBits()),
						FifoID.TYPE_ACTION_SCALAR);
				ioHandler.build(design);
				resources.addIOHandler(port, ioHandler);
			}
		}
	}

	public static List<Component> createPinReadComponent(Action action,
			net.sf.orcc.df.Port port, ResourceCache resources,
			Map<Bus, Var> busDependency, Map<Bus, Integer> doneBusDependency) {
		List<Component> pinReadList = new ArrayList<Component>();
		// Get the IOHandler of the actors port
		ActionIOHandler ioHandler = resources.getIOHandler(port);

		Integer repeatPattern = action.getInputPattern().getNumTokensMap()
				.get(port);
		if (repeatPattern == 1) {
			// Create the pinRead component
			Component pinRead = ioHandler.getReadAccess(false);
			pinRead.setNonRemovable();
			// Get the Pin Read variable
			Var pinReadVar = action.getInputPattern().getPortToVarMap()
					.get(port);
			// Map out Data Ports
			mapOutDataPorts(pinRead, pinReadVar, busDependency,
					doneBusDependency);
			pinReadList.add(pinRead);
		} else {
			Var inputReadVar = action.getInputPattern().getPortToVarMap()
					.get(port);
			for (int i = 0; i < repeatPattern; i++) {
				Var pinReadVar = action.getBody().getLocal(
						"pinRead_" + inputReadVar.getName() + "_" + i);
				// Create the pinRead component
				Component pinRead = ioHandler.getReadAccess(false);
				pinRead.setNonRemovable();
				// Map out Data Ports
				mapOutDataPorts(pinRead, pinReadVar, busDependency,
						doneBusDependency);
				pinReadList.add(pinRead);
			}
		}

		return pinReadList;
	}

	public static Component createPinStatusComponent(net.sf.orcc.df.Port port,
			ResourceCache resources, Map<net.sf.orcc.df.Port, Var> pinStatus,
			Map<Bus, Var> busDependency, Map<Bus, Integer> doneBusDependency) {
		// Create pin Status component from the ioHandler of the Port
		ActionIOHandler ioHandler = resources.getIOHandler(port);
		Component pinStatusComponent = ioHandler.getStatusAccess();
		pinStatusComponent.setNonRemovable();

		Type type = IrFactory.eINSTANCE.createTypeBool();
		Var pinStatusVar = IrFactory.eINSTANCE.createVar(0, type,
				port.getName() + "_pinStatus", false, 0);
		pinStatus.put(port, pinStatusVar);

		// Map out pinStatusVar to pinStatusComponent

		PortUtil.mapOutDataPorts(pinStatusComponent, pinStatusVar,
				busDependency, doneBusDependency);

		return pinStatusComponent;
	}

	public static List<Component> createPinWriteComponent(Action action,
			net.sf.orcc.df.Port port, ResourceCache resources,
			Map<Port, Var> portDependency,
			Map<Port, Integer> groupPortDependency,
			Map<Bus, Integer> doneBusDependency) {
		List<Component> pinWriteList = new ArrayList<Component>();
		// Get the IOHandler of the actors port
		ActionIOHandler ioHandler = resources.getIOHandler(port);

		Integer repeatPattern = action.getOutputPattern().getNumTokensMap()
				.get(port);
		if (repeatPattern == 1) {
			// Create the pinWrite component
			Component pinWrite = ioHandler.getWriteAccess(false);
			pinWrite.setNonRemovable();
			// Get the Pin Write variable
			Var pinWriteVar = action.getOutputPattern().getPortToVarMap()
					.get(port);

			// Map in Data Ports
			mapInDataPorts(pinWrite, pinWriteVar, portDependency,
					groupPortDependency);

			// Map out Control Port
			mapOutControlPort(pinWrite, 0, doneBusDependency);
			pinWriteList.add(pinWrite);
		} else {
			Var outputWriteVar = action.getOutputPattern().getPortToVarMap()
					.get(port);
			for (int i = 0; i < repeatPattern; i++) {
				Var pinWriteVar = action.getBody().getLocal(
						"pinWrite_" + outputWriteVar.getName() + "_" + i);
				// Create the pinWrite component
				Component pinWrite = ioHandler.getWriteAccess(false);
				pinWrite.setNonRemovable();
				// Map in Data Ports
				mapInDataPorts(pinWrite, pinWriteVar, portDependency,
						groupPortDependency);

				// Map out Control Port
				mapOutControlPort(pinWrite, 0, doneBusDependency);
				pinWriteList.add(pinWrite);
			}

		}
		return pinWriteList;
	}

	public static void mapInDataPorts(Component component, List<Var> inVars,
			Map<Port, Var> portDependency,
			Map<Port, Integer> portGroupDependency) {

		Iterator<Port> portIter = component.getDataPorts().iterator();

		Integer group = 0; // By default the group is zero
		for (Var var : inVars) {
			Port dataPort = portIter.next();
			dataPort.setIDLogical(getNameSSA(var));
			dataPort.setSize(var.getType().isString() ? 1 : var.getType()
					.getSizeInBits(), var.getType().isInt()
					|| var.getType().isBool());
			// Put Input Port dependency
			portDependency.put(dataPort, var);
			portGroupDependency.put(dataPort, group);
		}
	}

	public static void mapInDataPorts(Component component, Var inVar,
			Map<Port, Var> portDependency,
			Map<Port, Integer> portGroupDependency) {

		Iterator<Port> portIter = component.getDataPorts().iterator();

		Integer group = 0; // By default the group is zero
		Port dataPort = portIter.next();
		dataPort.setIDLogical(getNameSSA(inVar));
		dataPort.setSize(inVar.getType().getSizeInBits(), inVar.getType()
				.isInt() || inVar.getType().isBool());
		// Put Input Port dependency
		portDependency.put(dataPort, inVar);
		portGroupDependency.put(dataPort, group);
	}

	public static void mapOutControlPort(Component component, Integer group,
			Map<Bus, Integer> doneBusDependency) {
		Bus doneBus = component.getExit(Exit.DONE).getDoneBus();
		doneBusDependency.put(doneBus, group);
	}

	public static void mapOutDataPorts(Component component, List<Var> outVars,
			Map<Bus, Var> busDependency, Map<Bus, Integer> doneBusDependency) {

		Integer group = 0; // By default the group is zero
		for (Var var : outVars) {
			// Get the component dataBus
			List<Bus> dataBuses = component.getExit(Exit.DONE).getDataBuses();
			Bus dataBus = dataBuses.get(group);

			// Set the bus value
			if (dataBus.getValue() == null) {
				dataBus.setSize(var.getType().getSizeInBits(), var.getType()
						.isInt() || var.getType().isBool());
			}
			// Name the dataBus
			dataBus.setIDLogical(getNameSSA(var));
			busDependency.put(dataBus, var);
		}
		// Map Out done Bus
		mapOutControlPort(component, group, doneBusDependency);
	}

	public static void mapOutDataPorts(Component component, Var outVar,
			Map<Bus, Var> busDependency, Map<Bus, Integer> doneBusDependency) {

		Integer group = 0; // By default the group is zero
		Bus dataBus = component.getExit(Exit.DONE).getDataBuses().get(group);

		// Set the bus value
		if (dataBus.getValue() == null) {
			dataBus.setSize(outVar.getType().getSizeInBits(), outVar.getType()
					.isInt() || outVar.getType().isBool());
		}
		// Name the dataBus
		dataBus.setIDLogical(getNameSSA(outVar));
		busDependency.put(dataBus, outVar);

		// Map Out done Bus
		mapOutControlPort(component, group, doneBusDependency);

	}
}