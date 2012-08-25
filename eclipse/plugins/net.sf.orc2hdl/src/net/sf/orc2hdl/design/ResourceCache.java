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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sf.openforge.frontend.slim.builder.ActionIOHandler;
import net.sf.openforge.lim.TaskCall;
import net.sf.openforge.lim.memory.Location;
import net.sf.orcc.df.Port;
import net.sf.orcc.ir.Block;
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

	/** Map of a Branch Else Block Input Variables **/
	private Map<Block, List<Var>> elseInputs = new HashMap<Block, List<Var>>();

	/** Map of a Branch Else Block Output Variables **/
	private Map<Block, List<Var>> elseOutputs = new HashMap<Block, List<Var>>();

	private final Map<Port, ActionIOHandler> ioHandlers = new HashMap<Port, ActionIOHandler>();

	/** Map containing the join node **/
	private Map<Block, Map<Var, List<Var>>> joinVarMap = new HashMap<Block, Map<Var, List<Var>>>();

	/** Map of a LoopBody Block Input Variables **/
	private Map<Block, List<Var>> loopBodyInputs = new HashMap<Block, List<Var>>();

	/** Map of a LoopBody Block Output Variables **/
	private Map<Block, List<Var>> loopBodyOutputs = new HashMap<Block, List<Var>>();

	private final Map<Var, Location> memLocations = new HashMap<Var, Location>();

	/** Map of a Decision Module Input Variables **/
	private Map<Block, List<Var>> stmDecision = new HashMap<Block, List<Var>>();

	/** Map of a Loop Module Input Variables **/
	private Map<Block, List<Var>> stmInputs = new HashMap<Block, List<Var>>();

	/** Map of a Loop Module Output Variables **/
	private Map<Block, List<Var>> stmOutputs = new HashMap<Block, List<Var>>();

	private final Map<InstCall, TaskCall> taskCalls = new HashMap<InstCall, TaskCall>();

	/** Map of a Branch Then Block Input Variables **/
	private Map<Block, List<Var>> thenInputs = new HashMap<Block, List<Var>>();

	/** Map of a Branch Then Block Output Variables **/
	private Map<Block, List<Var>> thenOutputs = new HashMap<Block, List<Var>>();

	public ResourceCache() {
	}

	public void addBranch(Block block, Map<Block, List<Var>> stmDecision,
			Map<Block, List<Var>> stmInputs, Map<Block, List<Var>> stmOutputs,
			Map<Block, List<Var>> thenInputs,
			Map<Block, List<Var>> thenOutputs,
			Map<Block, List<Var>> elseInputs,
			Map<Block, List<Var>> elseOutputs,
			Map<Block, Map<Var, List<Var>>> joinVarMap) {
		// Initialize
		this.stmDecision.put(block, new ArrayList<Var>());
		this.stmInputs.put(block, new ArrayList<Var>());
		this.stmOutputs.put(block, new ArrayList<Var>());
		this.thenInputs.put(block, new ArrayList<Var>());
		this.thenOutputs.put(block, new ArrayList<Var>());
		this.elseInputs.put(block, new ArrayList<Var>());
		this.elseOutputs.put(block, new ArrayList<Var>());

		// Copy Vars
		copyVars(block, this.stmDecision, stmDecision);
		copyVars(block, this.stmInputs, stmInputs);
		copyVars(block, this.stmOutputs, stmOutputs);
		copyVars(block, this.thenInputs, thenInputs);
		copyVars(block, this.thenOutputs, thenOutputs);
		copyVars(block, this.elseInputs, elseInputs);
		copyVars(block, this.elseOutputs, elseOutputs);

		// Copy JoinVarMap
		Map<Var, List<Var>> copyMap = new HashMap<Var, List<Var>>();
		for (Var var : joinVarMap.get(block).keySet()) {
			List<Var> listVars = new ArrayList<Var>();
			for (Var groupVar : joinVarMap.get(block).get(var)) {
				listVars.add(groupVar);
			}
			copyMap.put(var, listVars);
		}
		this.joinVarMap.put(block, copyMap);
	}

	public void addIOHandler(Port port, ActionIOHandler io) {
		ioHandlers.put(port, io);
	}

	public void addLocation(Var var, Location location) {
		memLocations.put(var, location);
	}

	public void addLoop(Block block, Map<Block, List<Var>> stmDecision,
			Map<Block, List<Var>> stmInputs, Map<Block, List<Var>> stmOutputs,
			Map<Block, List<Var>> loopBodyInputs,
			Map<Block, List<Var>> loopBodyOutputs,
			Map<Block, Map<Var, List<Var>>> joinVarMap) {

		// Initialize
		this.stmDecision.put(block, new ArrayList<Var>());
		this.stmInputs.put(block, new ArrayList<Var>());
		this.stmOutputs.put(block, new ArrayList<Var>());
		this.loopBodyInputs.put(block, new ArrayList<Var>());
		this.loopBodyOutputs.put(block, new ArrayList<Var>());

		// Copy Vars
		copyVars(block, this.stmDecision, stmDecision);
		copyVars(block, this.stmInputs, stmInputs);
		copyVars(block, this.stmOutputs, stmOutputs);
		copyVars(block, this.loopBodyInputs, loopBodyInputs);
		copyVars(block, this.loopBodyOutputs, loopBodyOutputs);

		// Copy JoinVarMap
		Map<Var, List<Var>> copyMap = new HashMap<Var, List<Var>>();
		for (Var var : joinVarMap.get(block).keySet()) {
			List<Var> listVars = new ArrayList<Var>();
			for (Var groupVar : joinVarMap.get(block).get(var)) {
				listVars.add(groupVar);
			}
			copyMap.put(var, listVars);
		}
		this.joinVarMap.put(block, copyMap);
	}

	public void addTaskCall(InstCall instCall, TaskCall taskCall) {
		taskCalls.put(instCall, taskCall);
	}

	public void copyVars(Block block, Map<Block, List<Var>> target,
			Map<Block, List<Var>> source) {
		for (Var var : source.get(block)) {
			if (!target.get(block).contains(var)) {
				target.get(block).add(var);
			}
		}
	}

	public List<Var> getBlockDecisionInput(Block block) {
		return stmDecision.get(block);
	}

	public List<Var> getBlockInput(Block block) {
		return stmInputs.get(block);
	}

	public List<Var> getBlockOutput(Block block) {
		return stmOutputs.get(block);
	}

	public Map<Var, List<Var>> getBlockPhi(Block block) {
		return joinVarMap.get(block);
	}

	public List<Var> getBranchElseInput(Block block) {
		return elseInputs.get(block);
	}

	public List<Var> getBranchElseOutput(Block block) {
		return elseOutputs.get(block);
	}

	public List<Var> getBranchThenInput(Block block) {
		return thenInputs.get(block);
	}

	public List<Var> getBranchThenOutput(Block block) {
		return thenOutputs.get(block);
	}

	public ActionIOHandler getIOHandler(Port port) {
		return ioHandlers.get(port);
	}

	public Location getLocation(Var var) {
		return memLocations.get(var);
	}

	public List<Var> getLoopBodyInput(Block block) {
		return loopBodyInputs.get(block);
	}

	public List<Var> getLoopBodyOutput(Block block) {
		return loopBodyOutputs.get(block);
	}
}
