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

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import net.sf.openforge.lim.Bus;
import net.sf.openforge.lim.Component;
import net.sf.openforge.lim.Exit;
import net.sf.openforge.lim.Port;
import net.sf.orcc.ir.Var;

/**
 * This class contains several methods for the Design Ports
 * 
 * @author Endri Bezati
 * 
 */

public class PortUtil {

	public static void mapInDataPorts(List<Var> inVars, Component component,
			Map<Component, Map<Port, Var>> portDependency) {
		Iterator<Port> portIter = component.getDataPorts().iterator();
		Map<Port, Var> portDep = new HashMap<Port, Var>();
		for (Var var : inVars) {
			Port dataPort = portIter.next();
			dataPort.setIDLogical(var.getIndexedName());
			dataPort.setSize(var.getType().getSizeInBits(), var.getType()
					.isInt() || var.getType().isBool());
			portDep.put(dataPort, var);
		}

		// Put Input Port dependency
		portDependency.put(component, portDep);
	}

	public static void mapOutControlPort(Component component, Integer group,
			Map<Component, Map<Bus, Integer>> doneBusDependency) {
		Bus doneBus = component.getExit(Exit.DONE).getDoneBus();
		Map<Bus, Integer> busGroup = new HashMap<Bus, Integer>();
		busGroup.put(doneBus, group);
		doneBusDependency.put(component, busGroup);
	}

	public static void mapOutDataPorts(Component component, Var var,
			Integer group, Map<Component, Map<Bus, List<Var>>> busDependency,
			Map<Component, Map<Bus, Integer>> doneBusDependency) {

		// Get the component dataBus
		Bus dataBus = component.getExit(Exit.DONE).getDataBuses().get(group);

		Map<Bus, List<Var>> busDep = new HashMap<Bus, List<Var>>();

		// Set the bus value
		if (dataBus.getValue() == null) {
			dataBus.setSize(var.getType().getSizeInBits(), var.getType()
					.isInt() || var.getType().isBool());
		}
		// Name the dataBus
		dataBus.setIDLogical(var.getIndexedName());

		busDep.put(dataBus, Arrays.asList(var));
		busDependency.put(component, busDep);

		// Map Out done Bus
		mapOutControlPort(component, group, doneBusDependency);
	}

}
