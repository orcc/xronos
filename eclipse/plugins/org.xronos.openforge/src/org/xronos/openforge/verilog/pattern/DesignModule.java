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
package org.xronos.openforge.verilog.pattern;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.xronos.openforge.app.OptionRegistry;
import org.xronos.openforge.forge.api.internal.Core;
import org.xronos.openforge.lim.BidirectionalPin;
import org.xronos.openforge.lim.Component;
import org.xronos.openforge.lim.Design;
import org.xronos.openforge.lim.InputPin;
import org.xronos.openforge.lim.OutputPin;
import org.xronos.openforge.lim.Pin;
import org.xronos.openforge.lim.Port;
import org.xronos.openforge.lim.Procedure;
import org.xronos.openforge.lim.Visitable;
import org.xronos.openforge.lim.io.SimplePin;
import org.xronos.openforge.util.naming.ID;
import org.xronos.openforge.verilog.mapping.MappedModule;
import org.xronos.openforge.verilog.model.Assign;
import org.xronos.openforge.verilog.model.Expression;
import org.xronos.openforge.verilog.model.InlineComment;
import org.xronos.openforge.verilog.model.Input;
import org.xronos.openforge.verilog.model.Net;
import org.xronos.openforge.verilog.model.NetDeclarationReversed;
import org.xronos.openforge.verilog.model.NetFactory;
import org.xronos.openforge.verilog.model.Output;
import org.xronos.openforge.verilog.model.Statement;


/**
 * A DesignModule is Module based upon a LIM {@link Procedure}.
 * <P>
 * 
 * Created: Tue Mar 12 09:46:58 2002
 * 
 * @author <a href="mailto:andreas.kollegger@xilinx.com">Andy Kollegger</a>
 * @version $Id: DesignModule.java 23 2005-09-09 18:45:32Z imiller $
 */

public class DesignModule extends org.xronos.openforge.verilog.model.Module
		implements MappedModuleSpecifier {

	@SuppressWarnings("unused")
	private static int call_instanceCount = 0;

	private Design design;

	private Set<MappedModule> mappedModules = new HashSet<MappedModule>();

	/**
	 * When set to true the Ports of this module will be reversed in their Range
	 * notation.
	 */
	private boolean REVERSE_PORTS;

	/**
	 * Construct a DesignModule based on a {@link Design}.
	 * 
	 * @param design
	 *            the Design which is being instantiated
	 */
	public DesignModule(Design design) {
		super(modifyNameForEDK(ID.toVerilogIdentifier(ID.showLogical(design)),
				design));

		REVERSE_PORTS = design
				.getEngine()
				.getGenericJob()
				.getUnscopedBooleanOptionValue(
						OptionRegistry.INVERT_DESIGN_PORT_RANGE);

		this.design = design;
		defineSimplePins(design);

		// still define the 'old' interface for now. It should just
		// contain the CLK and RESET ports.
		defineInterface();

	} // DesignModule

	/**
	 * determine if module name needs to be modified to be compatable with EDK
	 * and return the modified name if so. otherwise return the unmodified name
	 * 
	 * @param name
	 */
	private static String modifyNameForEDK(String name, Design design) {
		boolean noEDK = design.getEngine().getGenericJob()
				.getUnscopedBooleanOptionValue(OptionRegistry.NO_EDK);

		if (noEDK) {
			return name;
		} else {
			return name.toLowerCase();
		}
	}

	/**
	 * For each simple pin defined in this design, create a corresponding port
	 * on the top level application (regardless of whether its connected to
	 * anything). For output ports connect the design port to the bus that
	 * drives it. Input ports are taken care of by name (ie the name of the
	 * source bus of the pin matches the name of the port and so no explicit
	 * assign is needed)
	 */
	private void defineSimplePins(Design des) {
		// for (Iterator iter = des.getSimplePins().iterator(); iter.hasNext();)
		for (Component component : des.getDesignModule().getComponents()) {
			Visitable vis = component;
			if (vis instanceof SimplePin) {
				// SimplePin pin = (SimplePin)iter.next();
				SimplePin pin = (SimplePin) vis;
				if (pin.isPublished()) {
					if (pin.getXLatData().isInput()) {
						addPort(new Input(pin.getName(), pin.getWidth()));
					} else {
						Output out = new Output(pin.getName(), pin.getWidth());
						addPort(out);
						explicitlyConnect(out, pin.getXLatData().getSink());
					}
				} else {
					// Assign the source bus to be the value of the
					// connection to the sink port
					if (pin.getXLatData().getSource() != null
							&& pin.getXLatData().getSink() != null) {
						Net output = NetFactory.makeNet(pin.getXLatData()
								.getSource());
						PortWire input = new PortWire(pin.getXLatData()
								.getSink());
						state(new ForgeStatement(Collections.singleton(output),
								new Assign.Continuous(output, input)));
					} else {
						System.out
								.println("Error in construction!  internal pins must have both source and sink ("
										+ pin.getName() + ")");
					}
				}
			}
		}
	}

	/**
	 * Defines the Module ports based on the Design's InputPins, OutputPins, and
	 * BidirectionalPins.
	 */
	@SuppressWarnings("deprecation")
	private void defineInterface() {
		// define input ports (based on the InputPins)
		for (Pin p : design.getInputPins()) {
			InputPin pin = (InputPin) p;

			if (!Core.hasThisPin(pin.getApiPin())
					|| Core.hasPublished(pin.getApiPin())) {
				// XXX: FIXME - for some reason - designs that do not
				// need a clock have the clock show up in the input
				// pins list anyway.
				if (!design.consumesClock()) {
					// check if this pin is a clock, and if so skip it
					// since the design doesn't need it
					if (!design.getClockPins().contains(pin)) {
						addPort(new InputPinPort(pin));
					}
				} else {
					addPort(new InputPinPort(pin));
				}

			}
			// addPort(new InputPinPort((InputPin)pins.next()));
		}

		// define output ports (based on OutputPins)
		for (Pin p : design.getOutputPins()) {
			OutputPin pin = (OutputPin) p;

			if (!Core.hasThisPin(pin.getApiPin())
					|| Core.hasPublished(pin.getApiPin())) {
				OutputPinPort pin_port = new OutputPinPort(pin);
				addPort(pin_port);
				explicitlyConnect(pin_port, pin.getPort());
			}
		}

		// define in-out ports (based on BidirectionalPins)
		for (Pin p : design.getBidirectionalPins()) {
			BidirectionalPin biPin = (BidirectionalPin) p;

			if (!Core.hasThisPin(biPin.getApiPin())
					|| Core.hasPublished(biPin.getApiPin())) {
				BidirectionalPinPort pinPort = new BidirectionalPinPort(biPin);
				addPort(pinPort);
				explicitlyConnect(pinPort, biPin.getPort());
			}
		}

	} // defineInterface

	private void explicitlyConnect(Net pinPort, Port port) {
		// add explicit assignment to OutputPin
		PortWire pinPortWire = new PortWire(port);
		Expression portWire = pinPortWire;
		// In the case of Characters the wire feeding the Port
		// will be wider than the pin (but not the pin's port).
		// To catch this case, truncate the wire to the pin's width.
		if (pinPort.getWidth() < pinPortWire.getWidth()) {
			portWire = pinPortWire.getRange(pinPort.getWidth() - 1, 0);
		}

		state(new ForgeStatement(Collections.<Net> emptySet(),
				new Assign.Continuous(pinPort, portWire)));
	}

	/**
	 * Depending on the state of the preference
	 * {@link net.sf.openforge.app.project.ProjectDefiner#INVERT_DESIGN_PORT_RANGES}
	 * this method will create a custom declaration for the ports which uses the
	 * [0:n] notation instead of the standard [n:0] notation
	 * 
	 * @param net
	 *            a value of type 'Net'
	 */
	@Override
	protected void declarePort(Net net) {
		if (REVERSE_PORTS) {
			declare(new NetDeclarationReversed(net));
		} else {
			super.declarePort(net);
		}
	}

	/**
	 * Adds a statement to the statement block of the module, and a declaration
	 * for each undeclared Net produced by the statement.
	 */
	@Override
	public void state(Statement statement) {
		assert ((statement instanceof ForgePattern) || (statement instanceof InlineComment)) : "DesignModule only supports stating ForgePatterns.";

		if (statement instanceof ForgePattern) {
			for (Net net : ((ForgePattern) statement).getProducedNets()) {
				if (!isDeclared(net)) {
					declare(net);
				}
			}
		}
		statements.add(statement);

		// bubble bath
		if (statement instanceof MappedModuleSpecifier) {
			mappedModules.addAll(((MappedModuleSpecifier) statement)
					.getMappedModules());
		}

	} // state()

	/**
	 * Provides the Set of MappedModules
	 */
	@Override
	public Set<MappedModule> getMappedModules() {
		return mappedModules;
	}
} // class DesignModule

