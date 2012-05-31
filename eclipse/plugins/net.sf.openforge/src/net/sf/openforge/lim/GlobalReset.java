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
package net.sf.openforge.lim;

import java.util.List;

import net.sf.openforge.lim.op.Constant;
import net.sf.openforge.lim.op.SimpleConstant;

public class GlobalReset extends HiddenPin {

	public GlobalReset() {
		super(1, false);

	}

	public Physical makePhysical() {
		return new GlobalReset.Physical(false);
	}

	// public static class Physical extends PhysicalImplementationModule
	public static class Physical extends Block {
		private Port clock;
		private Port publishedReset;
		private Bus reset;

		Reg sampleReg;
		Reg crossReg;
		Reg glitchReg;
		And andGate;
		Not inverter;
		Reg finalReg;

		public Physical(boolean publishReset) {
			// super(0);
			super(false);

			/*
			 * After connecting each Port to a Bus, call Port.pushValueForward()
			 * to initialize the Port with a Value. We can't use
			 * Component.propagateValuesForward(), because that would just push
			 * the constant "1" through all the registers and eliminate the
			 * connections during translation.
			 */

			clock = makeDataPort();
			clock.setUsed(true);
			clock.setIDLogical("CLK");
			clock.setSize(1, false);

			// appropriate the reset signal (output from the pin)
			Exit physicalExit = makeExit(0);
			reset = physicalExit.makeOneBitBus();
			reset.setUsed(true);
			reset.setIDLogical("RESET");

			Bus commonClock = clock.getPeer();
			commonClock.setIDLogical("CLK");
			commonClock.setSize(1, false);

			// make the flops and wire them up
			Constant constantOne = new SimpleConstant(1, 1, false);

			Value zeroValue = new Value(1, false);
			zeroValue.setBit(0, Bit.ZERO);
			// No reset needed... this is a one-time only circuit.
			// Just after initialization. NOT be configurable.
			sampleReg = new Reg(Reg.REG, "sample");
			sampleReg.setInitialValue(zeroValue); // ensures constant prop does
													// not eliminate
			sampleReg.getResultBus().setIDLogical("sampled");
			sampleReg.getResultBus().setSize(1, false);
			sampleReg.getDataPort().setBus(constantOne.getValueBus());
			sampleReg.getDataPort().pushValueForward();
			sampleReg.getClockPort().setBus(commonClock);
			sampleReg.getClockPort().pushValueForward();

			// No reset needed... this is a one-time only circuit.
			// Just after initialization. NOT be configurable.
			crossReg = new Reg(Reg.REG, "cross");
			crossReg.setInitialValue(zeroValue); // ensures constant prop does
													// not eliminate
			crossReg.getResultBus().setIDLogical("crossed");
			crossReg.getResultBus().setSize(1, false);
			crossReg.getDataPort().setBus(sampleReg.getResultBus());
			crossReg.getDataPort().pushValueForward();
			crossReg.getClockPort().setBus(commonClock);
			crossReg.getClockPort().pushValueForward();

			// No reset needed... this is a one-time only circuit.
			// Just after initialization. NOT be configurable.
			glitchReg = new Reg(Reg.REG, "glitch");
			glitchReg.setInitialValue(zeroValue); // ensures constant prop does
													// not eliminate
			glitchReg.getResultBus().setIDLogical("glitched");
			glitchReg.getResultBus().setSize(1, false);
			glitchReg.getDataPort().setBus(crossReg.getResultBus());
			glitchReg.getDataPort().pushValueForward();
			glitchReg.getClockPort().setBus(commonClock);
			glitchReg.getClockPort().pushValueForward();

			andGate = new And(2);
			List<Port> andPorts = andGate.getDataPorts();
			andPorts.get(0).setBus(crossReg.getResultBus());
			andPorts.get(0).pushValueForward();
			andPorts.get(1).setBus(glitchReg.getResultBus());
			andPorts.get(1).pushValueForward();

			andGate.getResultBus().setIDLogical("crossAndGlitch");
			andGate.getResultBus().setSize(1, false);

			inverter = new Not();
			inverter.getDataPort().setBus(andGate.getResultBus());
			inverter.getDataPort().pushValueForward();
			inverter.getResultBus().setIDLogical("inverted");
			inverter.getResultBus().setSize(1, false);

			Value init = new Value(1, false);
			init.setBit(0, Bit.ONE);
			// No reset needed... this is a one-time only circuit.
			// Just after initialization. NOT be configurable.
			finalReg = new Reg(Reg.REG, "final");
			finalReg.setInitialValue(init);
			finalReg.getResultBus().setIDLogical("RESET");
			finalReg.getResultBus().setSize(1, false);
			finalReg.getDataPort().setBus(inverter.getResultBus());
			finalReg.getDataPort().pushValueForward();
			finalReg.getClockPort().setBus(commonClock);
			finalReg.getClockPort().pushValueForward();

			sampleReg.setHardInstantiate(true);
			crossReg.setHardInstantiate(true);
			glitchReg.setHardInstantiate(true);
			finalReg.setHardInstantiate(true);

			// add all the components to the module
			addComponent(sampleReg);
			addComponent(crossReg);
			addComponent(glitchReg);
			addComponent(andGate);
			addComponent(inverter);
			addComponent(finalReg);

			Bus finalReset = finalReg.getResultBus();
			if (publishReset) {
				// Create a new port so that there is no confusion in
				// hooking up resets. This module will be a design
				// leve element and we hook up all resets of design
				// level elements to the appropriate reset module.
				// This could cause a loop.
				publishedReset = makeDataPort();
				publishedReset.setUsed(true);
				publishedReset.setIDLogical("PUBLISHED_RESET");
				publishedReset.setSize(1, false);
				Or orgate = new Or(2);
				List<Port> ports = orgate.getDataPorts();
				ports.get(0).setBus(publishedReset.getPeer());
				ports.get(0).pushValueForward();
				ports.get(1).setBus(finalReset);
				ports.get(1).pushValueForward();
				finalReset = orgate.getResultBus();
				addComponent(orgate);
			}

			// finally, wire the reset
			reset.getPeer().setBus(finalReset);
			reset.getPeer().pushValueForward();

		}

		// public void accept (Visitor v)
		// {
		// assert (false) : "GlobalReset doesn't accept visitors";
		// }

		// public boolean removeDataBus (Bus bus)
		// {
		// assert false : "remove data bus not supported on " + this;
		// return false;
		// }

		// public boolean removeDataPort (Port port)
		// {
		// assert false : "remove data port not supported on " + this;
		// return false;
		// }

		@Override
		public boolean isOpaque() {
			return true;
		}

		public Port getClockInput() {
			return clock;
		}

		public Port getResetInput() {
			return publishedReset;
		}

		public Bus getResetOutput() {
			return reset;
		}

		public Reg getSampleReg() {
			return sampleReg;
		}

		public Reg getCrossReg() {
			return crossReg;
		}

		public Reg getGlitchReg() {
			return glitchReg;
		}

		public And getAndGate() {
			return andGate;
		}

		public Not getInverter() {
			return inverter;
		}

		public Reg getFinalReg() {
			return finalReg;
		}
	}
}
