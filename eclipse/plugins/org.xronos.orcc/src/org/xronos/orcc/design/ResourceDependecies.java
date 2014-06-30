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

package org.xronos.orcc.design;

import java.util.HashMap;
import java.util.Map;

import org.xronos.openforge.lim.Bus;
import org.xronos.openforge.lim.Port;
import net.sf.orcc.ir.Var;

/**
 * This class contains different Maps needed for the dependencies between LIM
 * components.
 * 
 * @author endrix
 * 
 */
public class ResourceDependecies {

	/** Dependency between Components and Bus-Var **/
	private final Map<Bus, Var> busDependency;

	/** Dependency between Components and Done Bus **/
	private final Map<Bus, Integer> doneBusDependency;

	/** Dependency between Components and Done Bus **/
	private final Map<Port, Integer> portGroupDependency;

	/** Dependency between Components and Port-Var **/
	private final Map<Port, Var> portDependency;

	public ResourceDependecies() {
		super();
		busDependency = new HashMap<Bus, Var>();
		doneBusDependency = new HashMap<Bus, Integer>();
		portDependency = new HashMap<Port, Var>();
		portGroupDependency = new HashMap<Port, Integer>();
	}

	public ResourceDependecies(Map<Bus, Var> busDependency,
			Map<Bus, Integer> doneBusDependency, Map<Port, Var> portDependency,
			Map<Port, Integer> portGroupDependency) {
		super();
		this.busDependency = busDependency;
		this.doneBusDependency = doneBusDependency;
		this.portDependency = portDependency;
		this.portGroupDependency = portGroupDependency;
	}

	public Map<Bus, Var> getBusDependency() {
		return busDependency;
	}

	public Map<Bus, Integer> getDoneBusDependency() {
		return doneBusDependency;
	}

	public Map<Port, Integer> getPortGroupDependency() {
		return portGroupDependency;
	}

	public Map<Port, Var> getPortDependency() {
		return portDependency;
	}

}
