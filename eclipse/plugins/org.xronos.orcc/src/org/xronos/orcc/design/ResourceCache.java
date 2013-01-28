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

package org.xronos.orcc.design;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sf.orcc.df.Action;
import net.sf.orcc.df.Actor;
import net.sf.orcc.df.Port;
import net.sf.orcc.ir.Block;
import net.sf.orcc.ir.Var;

import org.xronos.openforge.frontend.slim.builder.ActionIOHandler;
import org.xronos.openforge.lim.Task;
import org.xronos.openforge.lim.memory.Location;
import org.xronos.orcc.design.visitors.io.CircularBuffer;

/**
 * ResourceCache maintains mappings from the Orcc objects needed to all the
 * Design level resources in the implementation created for these Objects. This
 * includes input/output structures and memory allocated for state variables.
 * 
 * @author Endri Bezati
 * 
 */
public class ResourceCache {

	private Map<Action, Task> actionToTask = new HashMap<Action, Task>();

	private Map<Actor, Boolean> actorContainsRepeat = new HashMap<Actor, Boolean>();

	private Map<Actor, Map<Port, CircularBuffer>> actorInputCircularBuffer = new HashMap<Actor, Map<Port, CircularBuffer>>();

	private final Map<Port, ActionIOHandler> ioHandlers = new HashMap<Port, ActionIOHandler>();

	/** Map of a LoopBody Block Input Variables **/
	private Map<Block, List<Var>> loopBodyInputs = new HashMap<Block, List<Var>>();

	/** Map of a LoopBody Block Output Variables **/
	private Map<Block, List<Var>> loopBodyOutputs = new HashMap<Block, List<Var>>();

	private final Map<Var, Location> memLocations = new HashMap<Var, Location>();

	public ResourceCache() {
	}

	public void addActorContainsRepeat(Actor actor, Boolean contains) {
		actorContainsRepeat.put(actor, contains);
	}

	public void addIOHandler(Port port, ActionIOHandler io) {
		ioHandlers.put(port, io);
	}

	public void addLocation(Var var, Location location) {
		memLocations.put(var, location);
	}

	public Map<Port, CircularBuffer> getActorInputCircularBuffer(Actor actor) {
		return actorInputCircularBuffer.get(actor);
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

	public Task getTaskFromAction(Action action) {
		return actionToTask.get(action);
	}

	public void setActionToTask(Map<Action, Task> actionToTask) {
		this.actionToTask = actionToTask;
	}

	public void setActorInputCircularBuffer(Actor actor,
			Map<Port, CircularBuffer> portBuffer) {
		this.actorInputCircularBuffer.put(actor, portBuffer);
	}

}
