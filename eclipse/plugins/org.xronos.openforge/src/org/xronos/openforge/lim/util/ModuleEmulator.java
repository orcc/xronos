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
import org.xronos.openforge.lim.Emulatable;
import org.xronos.openforge.lim.Loop;
import org.xronos.openforge.lim.Module;
import org.xronos.openforge.lim.Port;
import org.xronos.openforge.util.SizedInteger;
import org.xronos.openforge.util.naming.ID;


/**
 * @author gandhij
 * 
 *         This is a helper class to emulate a Module
 * 
 *         NOTE: Components with multiple entries are declared unEmulatable even
 *         though they are emulatable. These components need more carefull
 *         analysis which could be done in future.
 */
public class ModuleEmulator extends Emulator {

	/* Module to be emulated */
	Module module = null;

	/**
	 * ModuleEmulator Constructor
	 * 
	 * @param module
	 *            The Module to be emulated
	 */
	public ModuleEmulator(Module targetModule) {
		super();
		module = targetModule;
		setComponentList(OrderFinder.getOrder(module));
	}

	/**
	 * Emulate the component with the given input values. Care is taken to
	 * emulate Modules/Blocks/Loops/UnrolledLoops as required. If the component
	 * is not a composite entry (like Module/Block/Loop) and if it has more than
	 * one entry, it is declared unEmulatable.
	 * 
	 * @param component
	 *            component to emulate
	 * @param portValues
	 *            input values for the comonent
	 * @return Map of (output bus, SizedInteger value) pairs
	 * @throws UnEmulatableLoopException
	 *             is thrown if the given component or one of its subcomponents
	 *             is a loop that cannot be emulated.
	 */

	protected Map<Bus, SizedInteger> emulateComponent(Component component,
			Map<Port, SizedInteger> portValues)
			throws UnEmulatableLoopException {
		Map<Bus, SizedInteger> outputValues = null;

		if (unEmulatable.contains(component)) {
			// System.out.println("Component " + component +
			// " Not emulatable ");
		} else {
			if (component instanceof Loop) {
				/* component is a loop */
				if (((Loop) component).isIterative()) {
					// System.out.println("emulating as loop");
					LoopEmulator le = new LoopEmulator((Loop) component);
					outputValues = le.emulate(portValues);
				} else {
					// System.out.println("Found an unrolled loop - emulating as module ..");
					ModuleEmulator me = new ModuleEmulator((Module) component);
					outputValues = me.emulate(portValues);
				}
			} else if (component instanceof Module) {
				/* component is a Module */
				// System.out.println("emulating as module");
				ModuleEmulator me = new ModuleEmulator((Module) component);
				outputValues = me.emulate(portValues);
			} else {
				try {
					// System.out.println("emulating as component");
					if (component.getEntries().size() > 1) {
						// NOTE: component has multiple entries -
						// we treat it as unEmulatable
						unEmulatable.add(component);
						return outputValues;
					}
					outputValues = ((Emulatable) component).emulate(portValues);
				} catch (Exception ex) {
					// System.out.println("EMULATION EXCEPTION ON COMPONENT " +
					// component);
				}
			}
		}
		return outputValues;
	}

	/**
	 * Emulate the Module with the inputMap provided and return a map of (output
	 * bus, SizedInteger value) pairs.
	 * 
	 * @param inputValues
	 *            map of (input port, SizedInteger value) for input
	 * @return busValuesMap - a map of (output bus, SizedInteger value) pairs.
	 * @throws UnEmulatableLoopException
	 *             - the module contained a loop inside it and it was
	 *             unemulatable.
	 */
	public Map<Bus, SizedInteger> emulate(Map<Port, SizedInteger> inputValues)
			throws UnEmulatableLoopException {
		// System.out.println("\nEMULATING MODULE -- " + module +
		// " with inputValues = "+ inputValues);
		Map<Bus, SizedInteger> outputValues = null;

		componentList.add(module);
		updateInputMap();
		componentList.remove(module);

		/*
		 * Prime the input bus value map with the given port values.
		 */
		Map<Bus, SizedInteger> busValues = portToBusValues(inputValues);

		Iterator<ID> iter = componentList.iterator();
		while (iter.hasNext()) {
			Component component = (Component) iter.next();

			// System.out.println("EMULATING COMPONENT  - " + component +
			// "with busValues " + busValues);
			Map<Port, SizedInteger> portValues = busToPortValues(component,
					busValues);
			// System.out.println("Port Values = " + portValues);
			outputValues = emulateComponent(component, portValues);
			// System.out.println("Output values = " + outputValues);

			if (outputValues != null) {
				busValues.putAll(outputValues);
			}
		}
		componentList.removeAll(unEmulatable);
		return busValues;
	}

}
