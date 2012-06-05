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
	private final Map<BlockIf, List<Map<Var, Integer>>> branchIfInput = new HashMap<BlockIf, List<Map<Var, Integer>>>();

	/** Map of BlockIf and a List of Then and Else Block outputs **/
	private final Map<BlockIf, List<Map<Var, Integer>>> branchIfOutput = new HashMap<BlockIf, List<Map<Var, Integer>>>();

	/** Map of BlockIf and its Map of Target Var and its associated Values Var **/
	private final Map<BlockIf, Map<Var, List<Var>>> branchPhi = new HashMap<BlockIf, Map<Var, List<Var>>>();

	private final Map<Port, ActionIOHandler> ioHandlers = new HashMap<Port, ActionIOHandler>();

	private final Map<Var, Location> memLocations = new HashMap<Var, Location>();

	private final Map<InstCall, TaskCall> taskCalls = new HashMap<InstCall, TaskCall>();

	public ResourceCache() {
	}

	public void addBranchDecisionInput(BlockIf blockIf, Var var) {
		Map<Var, Integer> vars = new HashMap<Var, Integer>();
		vars.put(var, 0);

		List<Map<Var, Integer>> listOfVars = new ArrayList<Map<Var, Integer>>();
		listOfVars.add(0, vars);
		branchIfInput.put(blockIf, listOfVars);
	}

	public void addBranchElseInput(BlockIf blockIf, List<Var> elseVars) {
		List<Map<Var, Integer>> listOfVars = branchIfInput.get(blockIf);
		Map<Var, Integer> vars = new HashMap<Var, Integer>();
		for (Var var : elseVars) {
			vars.put(var, 0);
		}
		listOfVars.add(2, vars);
		branchIfInput.put(blockIf, listOfVars);
	}

	public void addBranchElseOutput(BlockIf blockIf, List<Var> elseVars) {
		List<Map<Var, Integer>> listOfVars = branchIfOutput.get(blockIf);
		Map<Var, Integer> vars = new HashMap<Var, Integer>();
		for (Var var : elseVars) {
			vars.put(var, 0);
		}

		listOfVars.add(1, vars);
		branchIfOutput.put(blockIf, listOfVars);
	}

	public void addBranchPhi(BlockIf blockIf, Map<Var, List<Var>> phiMapVar) {
		branchPhi.put(blockIf, phiMapVar);
	}

	public void addBranchThenInput(BlockIf blockIf, List<Var> thenVars) {
		List<Map<Var, Integer>> listOfVars = branchIfInput.get(blockIf);

		Map<Var, Integer> vars = new HashMap<Var, Integer>();
		for (Var var : thenVars) {
			vars.put(var, 0);
		}

		listOfVars.add(1, vars);
		branchIfInput.put(blockIf, listOfVars);
	}

	public void addBranchThenOutput(BlockIf blockIf, List<Var> thenVars) {
		List<Map<Var, Integer>> listOfVars = new ArrayList<Map<Var, Integer>>();
		Map<Var, Integer> vars = new HashMap<Var, Integer>();
		for (Var var : thenVars) {
			vars.put(var, 0);
		}

		listOfVars.add(0, vars);
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

	public Map<Var, Integer> getBranchDecision(BlockIf blockIf) {
		// The first value of the first value is always the branch decision Var
		return branchIfInput.get(blockIf).get(0);
	}

	public Map<Var, Integer> getBranchElseOutputVars(BlockIf blockIf) {
		if (!blockIf.getElseBlocks().isEmpty()) {
			return branchIfOutput.get(blockIf).get(1);
		} else {
			return Collections.emptyMap();
		}
	}

	public Map<Var, Integer> getBranchElseVars(BlockIf blockIf) {
		if (!blockIf.getElseBlocks().isEmpty()) {
			if (branchIfInput.get(blockIf).get(2).isEmpty()) {
				return Collections.emptyMap();
			} else {
				return branchIfInput.get(blockIf).get(2);
			}
		} else {
			return Collections.emptyMap();
		}
	}

	public Map<Var, List<Var>> getBranchPhiVars(BlockIf blockIf) {
		return branchPhi.get(blockIf);
	}

	public Map<Var, Integer> getBranchThenOutputVars(BlockIf blockIf) {
		return branchIfOutput.get(blockIf).get(0);
	}

	public Map<Var, Integer> getBranchThenVars(BlockIf blockIf) {
		return branchIfInput.get(blockIf).get(1);
	}

	public Map<Var, Integer> getBranchInputs(BlockIf blockIf) {
		Map<Var, Integer> inputs = new HashMap<Var, Integer>();
		inputs.putAll(getBranchDecision(blockIf));
		inputs.putAll(getBranchThenVars(blockIf));
		inputs.putAll(getBranchElseVars(blockIf));

		Map<Var, Integer> outputs = new HashMap<Var, Integer>();
		outputs.putAll(getBranchThenOutputVars(blockIf));
		outputs.putAll(getBranchElseOutputVars(blockIf));

		// Inputs on join node dependency iff the then and else output does not
		// contain the phi value
		for (Var var : branchPhi.get(blockIf).keySet()) {
			List<Var> phiDep = branchPhi.get(blockIf).get(var);
			for (Var phiVar : phiDep) {
				if (!inputs.keySet().contains(phiVar)
						&& !outputs.keySet().contains(phiVar)) {
					inputs.put(phiVar, 0);
				}
			}
		}
		return inputs;
	}

	public ActionIOHandler getIOHandler(Port port) {
		return ioHandlers.get(port);
	}

	public Location getLocation(Var var) {
		return memLocations.get(var);
	}

}
