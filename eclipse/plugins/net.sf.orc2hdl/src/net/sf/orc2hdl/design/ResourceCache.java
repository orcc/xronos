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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.sf.openforge.frontend.slim.builder.ActionIOHandler;
import net.sf.openforge.lim.TaskCall;
import net.sf.openforge.lim.memory.Location;
import net.sf.orcc.df.Port;
import net.sf.orcc.ir.Block;
import net.sf.orcc.ir.BlockIf;
import net.sf.orcc.ir.BlockWhile;
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

	/** Map of Block and its decision Input **/
	private Map<Block, List<Var>> decisionInput = new HashMap<Block, List<Var>>();

	private final Map<Port, ActionIOHandler> ioHandlers = new HashMap<Port, ActionIOHandler>();

	private Map<BlockWhile, List<Var>> loopInput = new HashMap<BlockWhile, List<Var>>();

	private Map<BlockWhile, List<Var>> loopOtherInput = new HashMap<BlockWhile, List<Var>>();

	private Map<BlockWhile, List<Var>> loopOutput = new HashMap<BlockWhile, List<Var>>();

	/**
	 * Map of BlockWhile and its Map of Target Var and its associated Values Var
	 **/
	private Map<BlockWhile, Map<Var, List<Var>>> loopPhi = new HashMap<BlockWhile, Map<Var, List<Var>>>();

	private final Map<Var, Location> memLocations = new HashMap<Var, Location>();

	private final Map<InstCall, TaskCall> taskCalls = new HashMap<InstCall, TaskCall>();

	public ResourceCache() {
	}

	public void addBranchDecisionInput(BlockIf blockIf, Var var) {
		List<List<Var>> listOfVars = new ArrayList<List<Var>>();
		listOfVars.add(0, Arrays.asList(var));
		branchIfInput.put(blockIf, listOfVars);
	}

	public void addBranchElseInput(BlockIf blockIf, Set<Var> elseVars) {
		List<List<Var>> listOfVars = branchIfInput.get(blockIf);
		List<Var> vars = new ArrayList<Var>();
		for (Var var : elseVars) {
			vars.add(var);
		}
		listOfVars.add(2, vars);
		branchIfInput.put(blockIf, listOfVars);
	}

	public void addBranchElseOutput(BlockIf blockIf, Set<Var> elseVars) {
		List<List<Var>> listOfVars = branchIfOutput.get(blockIf);
		List<Var> vars = new ArrayList<Var>();
		for (Var var : elseVars) {
			vars.add(var);
		}

		listOfVars.add(2, vars);
		branchIfOutput.put(blockIf, listOfVars);
	}

	public void addBranchPhi(BlockIf blockIf, Map<Var, List<Var>> phiMapVar) {
		branchPhi.put(blockIf, phiMapVar);
		List<List<Var>> listOfVars = new ArrayList<List<Var>>();
		List<Var> vars = new ArrayList<Var>();
		for (Var var : phiMapVar.keySet()) {
			vars.add(var);
		}

		listOfVars.add(0, vars);
		branchIfOutput.put(blockIf, listOfVars);
	}

	public void addBranchThenInput(BlockIf blockIf, Set<Var> thenVars) {
		List<List<Var>> listOfVars = branchIfInput.get(blockIf);
		List<Var> vars = new ArrayList<Var>();
		for (Var var : thenVars) {
			vars.add(var);
		}

		listOfVars.add(1, vars);
		branchIfInput.put(blockIf, listOfVars);
	}

	public void addBranchThenOutput(BlockIf blockIf, Set<Var> thenVars) {
		List<List<Var>> listOfVars = branchIfOutput.get(blockIf);
		List<Var> vars = new ArrayList<Var>();
		for (Var var : thenVars) {
			vars.add(var);
		}

		listOfVars.add(1, vars);
		branchIfOutput.put(blockIf, listOfVars);
	}

	public void addDecisionInput(Block block, Set<Var> vars) {
		List<Var> listOfVars = new ArrayList<Var>();
		for (Var var : vars) {
			listOfVars.add(var);
		}
		decisionInput.put(block, listOfVars);
	}

	public void addIOHandler(Port port, ActionIOHandler io) {
		ioHandlers.put(port, io);
	}

	public void addLocation(Var var, Location location) {
		memLocations.put(var, location);
	}

	public void addLoopOtherInputs(BlockWhile blockWhile, Set<Var> blockVars) {
		List<Var> vars = new ArrayList<Var>();
		for (Var var : blockVars) {
			vars.add(var);
		}
		loopOtherInput.put(blockWhile, vars);
	}

	public void addLoopOutput(BlockWhile blockWhile, Set<Var> blockVars) {
		List<Var> vars = new ArrayList<Var>();
		for (Var var : blockVars) {
			vars.add(var);
		}
		loopOutput.put(blockWhile, vars);
	}

	public void addLoopPhi(BlockWhile blockWhile, Map<Var, List<Var>> phiMapVar) {
		loopPhi.put(blockWhile, phiMapVar);
	}

	public void addTaskCall(InstCall instCall, TaskCall taskCall) {
		taskCalls.put(instCall, taskCall);
	}

	public Var getBranchDecision(BlockIf blockIf) {
		// The first value of the first value is always the branch decision Var
		return branchIfInput.get(blockIf).get(0).get(0);
	}

	public List<Var> getBranchElseOutputVars(BlockIf blockIf) {
		if (!blockIf.getElseBlocks().isEmpty()) {
			return branchIfOutput.get(blockIf).get(2);
		} else {
			return Collections.<Var> emptyList();
		}
	}

	public List<Var> getBranchElseVars(BlockIf blockIf) {
		if (!blockIf.getElseBlocks().isEmpty()) {
			if (branchIfInput.get(blockIf).get(2).isEmpty()) {
				return Collections.<Var> emptyList();
			} else {
				return branchIfInput.get(blockIf).get(2);
			}
		} else {
			return Collections.<Var> emptyList();
		}
	}

	public List<Var> getBranchInputs(BlockIf blockIf) {
		List<Var> inputs = new ArrayList<Var>();
		inputs.add(getBranchDecision(blockIf));
		inputs.addAll(getBranchThenVars(blockIf));

		List<Var> vars = inputs;

		for (Var gVar : getBranchElseVars(blockIf)) {
			if (!vars.contains(gVar)) {
				inputs.add(gVar);
			}
		}

		List<Var> outputs = new ArrayList<Var>();
		outputs.addAll(getBranchThenOutputVars(blockIf));
		outputs.addAll(getBranchElseOutputVars(blockIf));

		// Inputs on join node dependency iff the then and else output does not
		// contain the phi value
		for (Var var : branchPhi.get(blockIf).keySet()) {
			List<Var> phiDep = branchPhi.get(blockIf).get(var);
			for (Var phiVar : phiDep) {
				if (!inputs.contains(phiVar) && !outputs.contains(phiVar)) {
					inputs.add(phiVar);
				}
			}
		}
		return inputs;
	}

	public List<Var> getBranchOutputs(BlockIf blockIf) {
		return branchIfOutput.get(blockIf).get(0);
	}

	public Map<Var, List<Var>> getBranchPhiVars(BlockIf blockIf) {
		return branchPhi.get(blockIf);
	}

	public List<Var> getBranchThenOutputVars(BlockIf blockIf) {
		return branchIfOutput.get(blockIf).get(1);
	}

	public List<Var> getBranchThenVars(BlockIf blockIf) {
		return branchIfInput.get(blockIf).get(1);
	}

	public List<Var> getDecisionInput(Block block) {
		if (decisionInput.containsKey(block)) {
			return decisionInput.get(block);
		}
		return null;
	}

	public ActionIOHandler getIOHandler(Port port) {
		return ioHandlers.get(port);
	}

	public Location getLocation(Var var) {
		return memLocations.get(var);
	}

	public List<Var> getLoopBodyInput(Block block) {
		List<Var> listOfVars = new ArrayList<Var>();
		for (Var var : loopPhi.get(block).keySet()) {
			listOfVars.add(var);
		}
		if (loopOtherInput.containsKey(block)) {
			listOfVars.addAll(loopOtherInput.get(block));
		}
		return listOfVars;
	}

	public List<Var> getLoopBodyOutput(Block block) {
		List<Var> listOfVars = new ArrayList<Var>();
		for (Var var : loopPhi.get(block).keySet()) {
			// The Var at the index 1 is the output of the loopBody
			List<Var> out = loopPhi.get(block).get(var);
			listOfVars.add(out.get(1));
		}
		return listOfVars;
	}

	public List<Var> getLoopIntput(Block block) {
		List<Var> listOfVars = new ArrayList<Var>();

		if (loopInput.containsKey(block)) {
			listOfVars = loopInput.get(block);
		}

		for (Var var : loopPhi.get(block).keySet()) {
			// The Var at the index 0 is the output of the loopBody
			List<Var> out = loopPhi.get(block).get(var);
			listOfVars.add(out.get(0));
		}
		if (loopOtherInput.containsKey(block)) {
			listOfVars.addAll(loopOtherInput.get(block));
		}
		return listOfVars;
	}

	public List<Var> getLoopOutput(Block block) {
		List<Var> listOfVars = new ArrayList<Var>();
		for (Var var : loopPhi.get(block).keySet()) {
			listOfVars.add(var);
		}
		return listOfVars;
	}

	public Map<Var, List<Var>> getLoopPhi(BlockWhile blockWhile) {
		return loopPhi.get(blockWhile);
	}

}
