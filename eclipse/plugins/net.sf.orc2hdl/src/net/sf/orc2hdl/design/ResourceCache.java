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
import java.util.Set;

import net.sf.openforge.frontend.slim.builder.ActionIOHandler;
import net.sf.openforge.lim.TaskCall;
import net.sf.openforge.lim.memory.Location;
import net.sf.orc2hdl.design.util.GroupedVar;
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
	private final Map<BlockIf, List<List<GroupedVar>>> branchIfInput = new HashMap<BlockIf, List<List<GroupedVar>>>();

	/** Map of BlockIf and a List of Then and Else Block outputs **/
	private final Map<BlockIf, List<List<GroupedVar>>> branchIfOutput = new HashMap<BlockIf, List<List<GroupedVar>>>();

	/** Map of BlockIf and its Map of Target Var and its associated Values Var **/
	private final Map<BlockIf, Map<Var, List<Var>>> branchPhi = new HashMap<BlockIf, Map<Var, List<Var>>>();

	/** Map of Block and its decision Input **/
	private Map<Block, List<GroupedVar>> decisionInput = new HashMap<Block, List<GroupedVar>>();

	private final Map<Port, ActionIOHandler> ioHandlers = new HashMap<Port, ActionIOHandler>();

	private Map<BlockWhile, List<GroupedVar>> loopInput = new HashMap<BlockWhile, List<GroupedVar>>();

	private Map<BlockWhile, List<GroupedVar>> loopOtherInput = new HashMap<BlockWhile, List<GroupedVar>>();

	private Map<BlockWhile, List<GroupedVar>> loopOutput = new HashMap<BlockWhile, List<GroupedVar>>();

	/**
	 * Map of BlockWhile and its Map of Target Var and its associated Values Var
	 **/
	private Map<BlockWhile, Map<Var, List<Var>>> loopPhi = new HashMap<BlockWhile, Map<Var, List<Var>>>();

	private final Map<Var, Location> memLocations = new HashMap<Var, Location>();

	private final Map<InstCall, TaskCall> taskCalls = new HashMap<InstCall, TaskCall>();

	public ResourceCache() {
	}

	public void addBranchDecisionInput(BlockIf blockIf, Var var) {
		GroupedVar groupedVar = new GroupedVar(var, 0);
		List<List<GroupedVar>> listOfVars = new ArrayList<List<GroupedVar>>();
		listOfVars.add(0, groupedVar.getAsList());
		branchIfInput.put(blockIf, listOfVars);
	}

	public void addBranchElseInput(BlockIf blockIf, Set<Var> elseVars) {
		List<List<GroupedVar>> listOfVars = branchIfInput.get(blockIf);
		List<GroupedVar> vars = new ArrayList<GroupedVar>();
		for (Var var : elseVars) {
			vars.add(new GroupedVar(var, 0));
		}
		listOfVars.add(2, vars);
		branchIfInput.put(blockIf, listOfVars);
	}

	public void addBranchElseOutput(BlockIf blockIf, Set<Var> elseVars) {
		List<List<GroupedVar>> listOfVars = branchIfOutput.get(blockIf);
		List<GroupedVar> vars = new ArrayList<GroupedVar>();
		for (Var var : elseVars) {
			vars.add(new GroupedVar(var, 0));
		}

		listOfVars.add(2, vars);
		branchIfOutput.put(blockIf, listOfVars);
	}

	public void addBranchPhi(BlockIf blockIf, Map<Var, List<Var>> phiMapVar) {
		branchPhi.put(blockIf, phiMapVar);
		List<List<GroupedVar>> listOfVars = new ArrayList<List<GroupedVar>>();
		List<GroupedVar> vars = new ArrayList<GroupedVar>();
		for (Var var : phiMapVar.keySet()) {
			vars.add(new GroupedVar(var, 0));
		}

		listOfVars.add(0, vars);
		branchIfOutput.put(blockIf, listOfVars);
	}

	public void addBranchThenInput(BlockIf blockIf, Set<Var> thenVars) {
		List<List<GroupedVar>> listOfVars = branchIfInput.get(blockIf);
		List<GroupedVar> vars = new ArrayList<GroupedVar>();
		for (Var var : thenVars) {
			vars.add(new GroupedVar(var, 0));
		}

		listOfVars.add(1, vars);
		branchIfInput.put(blockIf, listOfVars);
	}

	public void addBranchThenOutput(BlockIf blockIf, Set<Var> thenVars) {
		List<List<GroupedVar>> listOfVars = branchIfOutput.get(blockIf);
		List<GroupedVar> vars = new ArrayList<GroupedVar>();
		for (Var var : thenVars) {
			vars.add(new GroupedVar(var, 0));
		}

		listOfVars.add(1, vars);
		branchIfOutput.put(blockIf, listOfVars);
	}

	public void addDecisionInput(Block block, Set<Var> vars) {
		List<GroupedVar> listOfVars = new ArrayList<GroupedVar>();
		for (Var var : vars) {
			listOfVars.add(new GroupedVar(var, 0));
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
		List<GroupedVar> vars = new ArrayList<GroupedVar>();
		for (Var var : blockVars) {
			vars.add(new GroupedVar(var, 0));
		}
		loopOtherInput.put(blockWhile, vars);
	}

	public void addLoopOutput(BlockWhile blockWhile, Set<Var> blockVars) {
		List<GroupedVar> vars = new ArrayList<GroupedVar>();
		for (Var var : blockVars) {
			vars.add(new GroupedVar(var, 0));
		}
		loopOutput.put(blockWhile, vars);
	}

	public void addLoopPhi(BlockWhile blockWhile, Map<Var, List<Var>> phiMapVar) {
		loopPhi.put(blockWhile, phiMapVar);
	}

	public void addTaskCall(InstCall instCall, TaskCall taskCall) {
		taskCalls.put(instCall, taskCall);
	}

	public GroupedVar getBranchDecision(BlockIf blockIf) {
		// The first value of the first value is always the branch decision Var
		return branchIfInput.get(blockIf).get(0).get(0);
	}

	public List<GroupedVar> getBranchElseOutputVars(BlockIf blockIf) {
		if (!blockIf.getElseBlocks().isEmpty()) {
			return branchIfOutput.get(blockIf).get(2);
		} else {
			return Collections.<GroupedVar> emptyList();
		}
	}

	public List<GroupedVar> getBranchElseVars(BlockIf blockIf) {
		if (!blockIf.getElseBlocks().isEmpty()) {
			if (branchIfInput.get(blockIf).get(2).isEmpty()) {
				return Collections.<GroupedVar> emptyList();
			} else {
				return branchIfInput.get(blockIf).get(2);
			}
		} else {
			return Collections.<GroupedVar> emptyList();
		}
	}

	public List<GroupedVar> getBranchInputs(BlockIf blockIf) {
		List<GroupedVar> inputs = new ArrayList<GroupedVar>();
		inputs.add(getBranchDecision(blockIf));
		inputs.addAll(getBranchThenVars(blockIf));

		List<Var> vars = new ArrayList<Var>();
		for (GroupedVar inVar : inputs) {
			vars.add(inVar.getVar());
		}

		for (GroupedVar gVar : getBranchElseVars(blockIf)) {
			if (!vars.contains(gVar.getVar())) {
				inputs.add(gVar);
			}
		}

		List<GroupedVar> outputs = new ArrayList<GroupedVar>();
		outputs.addAll(getBranchThenOutputVars(blockIf));
		outputs.addAll(getBranchElseOutputVars(blockIf));

		// Inputs on join node dependency iff the then and else output does not
		// contain the phi value
		for (Var var : branchPhi.get(blockIf).keySet()) {
			List<Var> phiDep = branchPhi.get(blockIf).get(var);
			for (Var phiVar : phiDep) {
				if (!GroupedVar.VarContainedInList(inputs, phiVar)
						&& !GroupedVar.VarContainedInList(outputs, phiVar)) {
					inputs.add(new GroupedVar(phiVar, 0));
				}
			}
		}
		return inputs;
	}

	public List<GroupedVar> getBranchOutputs(BlockIf blockIf) {
		return branchIfOutput.get(blockIf).get(0);
	}

	public Map<Var, List<Var>> getBranchPhiVars(BlockIf blockIf) {
		return branchPhi.get(blockIf);
	}

	public List<GroupedVar> getBranchThenOutputVars(BlockIf blockIf) {
		return branchIfOutput.get(blockIf).get(1);
	}

	public List<GroupedVar> getBranchThenVars(BlockIf blockIf) {
		return branchIfInput.get(blockIf).get(1);
	}

	public List<GroupedVar> getDecisionInput(Block block) {
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

	public List<GroupedVar> getLoopBodyInput(Block block) {
		List<GroupedVar> listOfVars = new ArrayList<GroupedVar>();
		for (Var var : loopPhi.get(block).keySet()) {
			GroupedVar gVar = new GroupedVar(var, 0);
			listOfVars.add(gVar);
		}
		if (loopOtherInput.containsKey(block)) {
			listOfVars.addAll(loopOtherInput.get(block));
		}
		return listOfVars;
	}

	public List<GroupedVar> getLoopBodyOutput(Block block) {
		List<GroupedVar> listOfVars = new ArrayList<GroupedVar>();
		for (Var var : loopPhi.get(block).keySet()) {
			// The Var at the index 1 is the output of the loopBody
			List<Var> out = loopPhi.get(block).get(var);
			GroupedVar gVar = new GroupedVar(out.get(1), 0);
			listOfVars.add(gVar);
		}
		return listOfVars;
	}

	public List<GroupedVar> getLoopIntput(Block block) {
		List<GroupedVar> listOfVars = new ArrayList<GroupedVar>();

		if (loopInput.containsKey(block)) {
			listOfVars = loopInput.get(block);
		}

		for (Var var : loopPhi.get(block).keySet()) {
			// The Var at the index 0 is the output of the loopBody
			List<Var> out = loopPhi.get(block).get(var);
			GroupedVar gVar = new GroupedVar(out.get(0), 0);
			listOfVars.add(gVar);
		}
		if (loopOtherInput.containsKey(block)) {
			listOfVars.addAll(loopOtherInput.get(block));
		}
		return listOfVars;
	}

	public List<GroupedVar> getLoopOutput(Block block) {
		return getLoopBodyInput(block);
	}

	public Map<Var, List<Var>> getLoopPhi(BlockWhile blockWhile) {
		return loopPhi.get(blockWhile);
	}

}
