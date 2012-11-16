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

package net.sf.orc2hdl.design;

import java.util.HashMap;
import java.util.Map;

import net.sf.openforge.lim.Bus;
import net.sf.openforge.lim.Port;
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
