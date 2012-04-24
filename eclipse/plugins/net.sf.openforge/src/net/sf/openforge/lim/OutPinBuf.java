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

import net.sf.openforge.util.naming.ID;

/**
 * An OutPinBuf allows writing to an output {@link Pin}.
 * 
 * @author Stephen Edwards
 * @version $Id: OutPinBuf.java 280 2006-08-11 17:00:32Z imiller $
 */
public class OutPinBuf extends PinBuf {

	private Physical phys;

	OutPinBuf(Pin pin) {
		/*
		 * tbd.
		 */
		super(pin);
	}

	/**
	 * Gets the latency of a reference's exit.
	 */
	@Override
	public Latency getLatency(Exit exit) {
		/*
		 * tbd.
		 */
		return Latency.ZERO;
	}

	@Override
	public boolean consumesReset() {
		// if we have an api pin, yes
		return (getPhysicalComponent() != null);
	}

	@Override
	public boolean consumesClock() {
		return (getPhysicalComponent() != null);
	}

	public Physical getPhysicalComponent() {
		// assert phys!=null;
		return phys;
	}

	/**
	 * Make the Physical implementation of this OutPinBuf.
	 * 
	 * @param initDriven
	 *            true if drive state logic is required
	 */
	public Physical makePhysicalComponent() {
		phys = new Physical(getPin().getWidth(), getPin().getResetValue(),
				getPin().isDriveOnReset());
		return phys;
	}

	/**
	 * Here we have a module with 8 inputs, 1 outputs. Inputs are, in order:
	 * NextValue, NextValueEnable, NowValue, NowValueEnable, NextDriveValue,
	 * NextDriveEnable, NowDriveValue, NowDriveEnable. Output is value currently
	 * being driven
	 * 
	 * @author "C. Schanck" <cschanck@cschanck>
	 * @version 1.0
	 * @since 1.0
	 * @see Module
	 */
	public class Physical extends PhysicalImplementationModule {
		private Bus resultBus;
		private int size;
		private long initValue;
		private boolean initDriven;

		Physical(int size, long initValue, boolean initDriven) {
			super(8);
			getNextValuePort().getPeer().setSize(size, true);
			getNextValueEnablePort().getPeer().setSize(1, true);
			getNowValuePort().getPeer().setSize(size, true);
			getNowValueEnablePort().getPeer().setSize(1, true);
			getNextDrivePort().getPeer().setSize(1, true);
			getNextDriveEnablePort().getPeer().setSize(1, true);
			getNowDrivePort().getPeer().setSize(1, true);
			getNowDriveEnablePort().getPeer().setSize(1, true);

			getClockPort().setUsed(true);
			getResetPort().setUsed(true);

			Exit exit = makeExit(0);

			// make a result bus
			resultBus = exit.makeDataBus();
			resultBus.setSize(size, true);

			this.size = size;
			this.initDriven = initDriven;
			if (initDriven)
				this.initValue = initValue;
			else
				this.initValue = 0;
		}

		public void connect() {
			// now add the tribuf
			TriBuf tbuf = new TriBuf();
			addComponent(tbuf);
			tbuf.getResultBus().setSize(size, true);

			// create the value and drive logic
			Bus res1 = addValueLogic(size, initValue);
			Bus res2 = addDriveLogic(initDriven);

			// hook it up
			tbuf.getInputPort().setBus(res1);
			tbuf.getEnablePort().setBus(res2);

			// and the end!
			resultBus.getPeer().setBus(tbuf.getResultBus());
		}

		private Bus addValueLogic(int size, long initValue) {
			// ok. We need a reg ...
			// Reg reg1=new Reg(initValue);
			Reg reg1 = Reg.getConfigurableReg(Reg.REGRE, ID.showLogical(this)
					+ "_value");
			reg1.setInitialValue(Value.getConstantValue(initValue));

			// reg1.setIDLogical(ID.showLogical(reg1)+"_value");
			addComponent(reg1);
			// set up the clock
			reg1.getClockPort().setBus(getClockPort().getPeer());
			// set the data/enable
			reg1.getDataPort().setBus(getNextValuePort().getPeer());
			reg1.getEnablePort().setBus(getNextValueEnablePort().getPeer());
			// reset
			reg1.getResetPort().setBus(getResetPort().getPeer());
			reg1.getInternalResetPort().setBus(getResetPort().getPeer());
			// size bus of reg
			reg1.getResultBus().setSize(size, true);

			// now a mux
			Mux mux1 = new Mux(2);
			mux1.setIDLogical(ID.showLogical(mux1) + "_value");
			addComponent(mux1);
			mux1.getResultBus().setSize(size, true);

			List<Port> l = mux1.getGoPorts();
			//
			// This is a hack like Latch -- we count on the translater
			// ignoring the second select so we connect them themselves.
			//

			// 0th -- now port
			Port sel = l.get(0);
			Port data = mux1.getDataPort(sel);
			sel.setBus(getNowValueEnablePort().getPeer());
			data.setBus(getNowValuePort().getPeer());

			// 1th -- result from the reg above
			sel = l.get(1);
			data = mux1.getDataPort(sel);
			sel.setBus(getNowValueEnablePort().getPeer());
			data.setBus(reg1.getResultBus());

			return mux1.getResultBus();
		}

		private Bus addDriveLogic(boolean isDriven) {
			// ok. We need a reg ...
			// Reg reg1=new Reg(isDriven?1L:0L);
			Reg reg1 = Reg.getConfigurableReg(Reg.REGRE, ID.showLogical(this)
					+ "_drive");
			reg1.setInitialValue(Value.getConstantValue(isDriven ? 1L : 0L));

			addComponent(reg1);
			// set up the clock
			reg1.getClockPort().setBus(getClockPort().getPeer());
			// set the data/enable
			reg1.getDataPort().setBus(getNextDrivePort().getPeer());
			reg1.getEnablePort().setBus(getNextDriveEnablePort().getPeer());
			// reset
			reg1.getResetPort().setBus(getResetPort().getPeer());
			reg1.getInternalResetPort().setBus(getResetPort().getPeer());
			// size bus of reg to one bit
			reg1.getResultBus().setSize(1, false);

			// now a mux
			Mux mux1 = new Mux(2);
			mux1.setIDLogical(ID.showLogical(mux1) + "_drive");
			addComponent(mux1);
			mux1.getResultBus().setSize(1, true);

			List<Port> l = mux1.getGoPorts();
			//
			// This is a hack like Latch -- we count on the translater
			// ignoring the second select so we connect them themselves.
			//

			// 0th -- now port
			Port sel = l.get(0);
			Port data = mux1.getDataPort(sel);
			sel.setBus(getNowDriveEnablePort().getPeer());
			data.setBus(getNowDrivePort().getPeer());

			// 1th -- result from the reg above
			sel = l.get(1);
			data = mux1.getDataPort(sel);
			sel.setBus(getNowDriveEnablePort().getPeer());
			data.setBus(reg1.getResultBus());

			return mux1.getResultBus();
		}

		public Bus getResultBus() {
			return resultBus;
		}

		public Port getNextValuePort() {
			return getDataPorts().get(0);
		}

		public Port getNextValueEnablePort() {
			return getDataPorts().get(1);
		}

		public Port getNowValuePort() {
			return getDataPorts().get(2);
		}

		public Port getNowValueEnablePort() {
			return getDataPorts().get(3);
		}

		public Port getNextDrivePort() {
			return getDataPorts().get(4);
		}

		public Port getNextDriveEnablePort() {
			return getDataPorts().get(5);
		}

		public Port getNowDrivePort() {
			return getDataPorts().get(6);
		}

		public Port getNowDriveEnablePort() {
			return getDataPorts().get(7);
		}

		@Override
		public void accept(Visitor visitor) {
			throw new UnexpectedVisitationException(
					"InPinBuf's should not be visited!");
		}
	}
}
