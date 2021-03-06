/*******************************************************************************
 * Copyright 2002-2009  Xilinx Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
/*
 * 
 *
 * 
 */
package org.xronos.openforge.lim.util;

import java.util.Iterator;
import java.util.Map;

import org.xronos.openforge.lim.Bus;
import org.xronos.openforge.lim.Component;
import org.xronos.openforge.lim.Loop;
import org.xronos.openforge.lim.Module;
import org.xronos.openforge.lim.Port;
import org.xronos.openforge.util.SizedInteger;
import org.xronos.openforge.util.naming.ID;


/**
 * @author gandhij
 * 
 *         This is a helper class to emulate the Loop Decision Block. It is a
 *         speceal case of a Module.
 * 
 */
public class LoopDecisionEmulator extends ModuleEmulator {

	/**
	 * Loop being emulated
	 */
	private Loop loop = null;

	/**
	 * The loop condition has evaluated to true or not ? In other words is the
	 * loop done ?
	 */
	private boolean done = false;

	/**
	 * LoopDecisionEmulator Constructor
	 * 
	 * @param loop
	 *            the loop under consideration
	 * @param decision
	 *            the decision block to be emulated
	 */
	public LoopDecisionEmulator(Loop loop, Module decision) {
		super(decision);
		this.loop = loop;
	}

	/**
	 * Emulate the LoopDecisionBlock with the inputMap provided and return a map
	 * of (output bus, SizedInteger value) pairs.
	 * 
	 * @param inputValues
	 *            map of (input port, SizedInteger value) for input
	 * @return busValuesMap - a map of (output bus, SizedInteger value) pairs.
	 * @throws UnEmulatableLoopException
	 *             - the loop is unEmulatable.
	 */
	@Override
	public Map<Bus, SizedInteger> emulate(Map<Port, SizedInteger> inputValues)
			throws UnEmulatableLoopException {
		// System.out.println("\nEMULATING DECISION -- " + module +
		// " with inputValues = "+ inputValues);
		Map<Bus, SizedInteger> outputValues = null;

		componentList.add(module);
		updateInputMap();
		componentList.remove(module);

		/*
		 * Prime the input bus value map with the given port values.
		 */
		Map<Bus, SizedInteger> busValues = portToBusValues(inputValues);
		// FIXME: Put implicit iterator
		Iterator<ID> iter = componentList.iterator();
		while (iter.hasNext()) {
			Component component = (Component) iter.next();

			Map<Port, SizedInteger> portValues = busToPortValues(component,
					busValues);

			// If component is a testblock emulate it accordingly //
			if (component == loop.getBody().getDecision().getTestBlock()) {
				LoopTestBlockEmulator te = new LoopTestBlockEmulator(loop,
						(Module) component);
				outputValues = te.emulate(portValues);
				done = te.getDone();
			} else {
				outputValues = emulateComponent(component, portValues);
			}

			if (outputValues != null) {
				busValues.putAll(outputValues);
			}
		}
		componentList.removeAll(unEmulatable);
		return busValues;
	}

	/**
	 * check if the loop test condition indicates if the loop is done
	 * 
	 * @return boolean indicating if the loop is done or not
	 */
	public boolean getDone() {
		return done;
	}
}
