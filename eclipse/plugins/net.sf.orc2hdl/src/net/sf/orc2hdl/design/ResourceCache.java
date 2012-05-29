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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sf.openforge.frontend.slim.builder.ActionIOHandler;
import net.sf.openforge.lim.TaskCall;
import net.sf.openforge.lim.memory.Location;
import net.sf.orcc.df.Port;
import net.sf.orcc.ir.BlockIf;
import net.sf.orcc.ir.InstCall;
import net.sf.orcc.ir.Var;

/**
 * ResourceCache maintains mappings from the Orcc objects needed to all the
 * Design level resources in the implementation created for these Objects. This
 * includes input/output structures and memory allocated for state variables.
 * 
 * @author Endri Bezati
 * 
 */
public class ResourceCache {

	/** Map of BlockIf and a List of Decision, Then, Else Input and the Phi Vars **/
	private final Map<BlockIf, List<List<Var>>> branchIfInput = new HashMap<BlockIf, List<List<Var>>>();

	/** Map of BlockIf and a List of Then and Else Block outputs **/
	private final Map<BlockIf, List<List<Var>>> branchIfOutput = new HashMap<BlockIf, List<List<Var>>>();

	/** Map of BlockIf and its Map of Target Var and its associated Values Var **/
	private final Map<BlockIf, Map<Var, List<Var>>> branchPhi = new HashMap<BlockIf, Map<Var, List<Var>>>();

	private final Map<Port, ActionIOHandler> ioHandlers = new HashMap<Port, ActionIOHandler>();

	private final Map<Var, Location> memLocations = new HashMap<Var, Location>();

	private final Map<InstCall, TaskCall> taskCalls = new HashMap<InstCall, TaskCall>();

	public ResourceCache() {
	}

	public void addBranchDecisionInput(BlockIf blockIf, Var var) {
		List<Var> vars = new ArrayList<Var>();
		vars.add(var);
		List<List<Var>> listOfVars = new ArrayList<List<Var>>();
		listOfVars.add(0, vars);
		branchIfInput.put(blockIf, listOfVars);
	}

	public void addBranchElseInput(BlockIf blockIf, List<Var> var) {
		List<List<Var>> listOfVars = branchIfInput.get(blockIf);
		listOfVars.add(2, var);
		branchIfInput.put(blockIf, listOfVars);
	}

	public void addBranchElseOutput(BlockIf blockIf, List<Var> var) {
		List<List<Var>> listOfVars = branchIfOutput.get(blockIf);
		listOfVars.add(1, var);
		branchIfOutput.put(blockIf, listOfVars);
	}

	public void addBranchPhi(BlockIf blockIf, Map<Var, List<Var>> phiMapVar) {
		branchPhi.put(blockIf, phiMapVar);
	}

	public void addBranchThenInput(BlockIf blockIf, List<Var> var) {
		List<List<Var>> listOfVars = branchIfInput.get(blockIf);
		listOfVars.add(1, var);
		branchIfInput.put(blockIf, listOfVars);
	}

	public void addBranchThenOutput(BlockIf blockIf, List<Var> var) {
		List<List<Var>> listOfVars = new ArrayList<List<Var>>();
		listOfVars.add(0, var);
		branchIfOutput.put(blockIf, listOfVars);
	}

	public void addIOHandler(Port port, ActionIOHandler io) {
		ioHandlers.put(port, io);
	}

	public void addLocation(Var var, Location location) {
		memLocations.put(var, location);
	}

	public void addTaskCall(InstCall instCall, TaskCall taskCall) {
		taskCalls.put(instCall, taskCall);
	}

	public Var getBranchDecision(BlockIf blockIf) {
		// The first value of the first value is always the branch decision Var
		return branchIfInput.get(blockIf).get(0).get(0);
	}

	public List<Var> getBranchElseOutputVars(BlockIf blockIf) {
		return branchIfOutput.get(blockIf).get(1);
	}

	public List<Var> getBranchElseVars(BlockIf blockIf) {
		if (branchIfInput.get(blockIf).get(2).isEmpty()) {
			return Collections.emptyList();
		} else {
			return branchIfInput.get(blockIf).get(2);
		}
	}

	public Map<Var, List<Var>> getBranchPhiVars(BlockIf blockIf) {
		return branchPhi.get(blockIf);
	}

	public List<Var> getBranchThenOutputVars(BlockIf blockIf) {
		return branchIfOutput.get(blockIf).get(0);
	}

	public List<Var> getBranchThenVars(BlockIf blockIf) {
		return branchIfInput.get(blockIf).get(1);
	}

	public ActionIOHandler getIOHandler(Port port) {
		return ioHandlers.get(port);
	}

	public Location getLocation(Var var) {
		return memLocations.get(var);
	}

}
