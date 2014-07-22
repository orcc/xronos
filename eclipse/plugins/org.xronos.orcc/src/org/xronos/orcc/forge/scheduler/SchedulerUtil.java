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
package org.xronos.orcc.forge.scheduler;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sf.orcc.df.Action;
import net.sf.orcc.df.Actor;
import net.sf.orcc.df.Port;

/**
 * This utility class helps different classes on the scheduler construction
 * 
 * @author endrix
 *
 */
public class SchedulerUtil {

	/**
	 * This method retrieves all ports that might have a repeat on a action
	 * 
	 * @param actor
	 * @return
	 */
	public static Map<Port, Boolean> getPortWithRepeats(Actor actor) {
		Map<Port, Boolean> portHasRepeat = new HashMap<Port, Boolean>();

		// -- Find if a port has repeats on an Action
		for (Action action : actor.getActions()) {
			for (Port port : action.getInputPattern().getPorts()) {
				if (action.getInputPattern().getNumTokens(port) > 1) {
					portHasRepeat.put(port, true);
				} else {
					if (!portHasRepeat.containsKey(port)) {
						portHasRepeat.put(port, false);
					}
				}
			}

			for (Port port : action.getOutputPattern().getPorts()) {
				if (action.getOutputPattern().getNumTokens(port) > 1) {
					portHasRepeat.put(port, true);
				} else {
					if (!portHasRepeat.containsKey(port)) {
						portHasRepeat.put(port, false);
					}
				}
			}
		}
		return portHasRepeat;
	}

	/**
	 * This method retrieves if an actor has a repeat on actor actions input
	 * 
	 * @param actor
	 * @return
	 */
	public static Boolean actorHasInputPortWithRepeats(Actor actor) {
		Boolean has = false;
		Map<Port, Boolean> portHasRepeat = getPortWithRepeats(actor);
		List<Port> inputs = actor.getInputs();
		for (Port port : inputs) {
			if (portHasRepeat.containsKey(port)) {
				has |= portHasRepeat.get(port);
			}
		}
		return has;
	}

	/**
	 * This method retrieves if an actor has a repeat on actor actions output
	 * 
	 * @param actor
	 * @return
	 */
	public static Boolean actorHasOutputPortWithRepeats(Actor actor) {
		Boolean has = false;
		Map<Port, Boolean> portHasRepeat = getPortWithRepeats(actor);
		List<Port> outputs = actor.getOutputs();
		for (Port port : outputs) {
			if (portHasRepeat.containsKey(port)) {
				has |= portHasRepeat.get(port);
			}
		}
		return has;
	}

}
