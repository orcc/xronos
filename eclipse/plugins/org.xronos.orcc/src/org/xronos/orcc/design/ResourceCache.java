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
 * 
 * Additional permission under GNU GPL version 3 section 7
 * 
 * If you modify this Program, or any covered work, by linking or combining it
 * with Eclipse (or a modified version of Eclipse or an Eclipse plugin or 
 * an Eclipse library), containing parts covered by the terms of the 
 * Eclipse Public License (EPL), the licensors of this Program grant you 
 * additional permission to convey the resulting work.  Corresponding Source 
 * for a non-source form of such a combination shall include the source code 
 * for the parts of Eclipse libraries used as well as that of the  covered work.
 * 
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

	private Map<Action, Task> actionToTask;

	private Map<Actor, Boolean> actorContainsRepeat;

	private Map<Actor, Map<Port, CircularBuffer>> actorInputCircularBuffer;

	private Map<Port, ActionIOHandler> ioHandlers;

	/** Map of a LoopBody Block Input Variables **/
	private Map<Block, List<Var>> loopBodyInputs;

	/** Map of a LoopBody Block Output Variables **/
	private Map<Block, List<Var>> loopBodyOutputs;

	private Map<Var, Location> memLocations;

	public ResourceCache() {
		actionToTask = new HashMap<Action, Task>();
		actorContainsRepeat = new HashMap<Actor, Boolean>();
		actorInputCircularBuffer = new HashMap<Actor, Map<Port, CircularBuffer>>();
		ioHandlers = new HashMap<Port, ActionIOHandler>();
		loopBodyInputs = new HashMap<Block, List<Var>>();
		loopBodyOutputs = new HashMap<Block, List<Var>>();
		memLocations = new HashMap<Var, Location>();

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
