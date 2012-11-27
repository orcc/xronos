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
package org.xronos.openforge.lim;

import org.xronos.openforge.util.naming.ID;

/**
 * An {@link InPinBuf} access.
 * 
 * @author Stephen Edwards
 * @version $Id: PinRead.java 88 2006-01-11 22:39:52Z imiller $
 */
public class PinRead extends PinAccess implements Cloneable {

	private Physical physical = null;

	public PinRead() {
		// one port -- the address (object reference) of the pin
		super(1);

		/*
		 * One bus for the data.
		 */
		makeExit(1);
	}

	/*
	 * =================================================== Begin new constant
	 * prop rules implementation.
	 */

	/**
	 * The data out from the pin read should be sized to whatever the pin size
	 * is defined to be.
	 * 
	 * @return a value of type 'boolean'
	 */
	@Override
	public boolean pushValuesForward() {
		/*
		 * The InBuf identity is dynamic. The best we could do here is choose
		 * the max size of any possible InBuf after the ObjectResolver has run.
		 * --SGE
		 */
		return false;
	}

	/**
	 * No port to annotate size back from bus. The input pin size should not be
	 * changed.
	 * 
	 * @return a value of type 'boolean'
	 */
	@Override
	public boolean pushValuesBackward() {
		return false;
	}

	/*
	 * End new constant prop rules implementation.
	 * =================================================
	 */

	@Override
	public void accept(Visitor v) {
		v.visit(this);
	}

	@Override
	public Object clone() throws CloneNotSupportedException {
		final PinRead clone = (PinRead) super.clone();
		copyComponentAttributes(clone);
		return clone;
	}

	public Physical makePhysicalComponent() {
		assert physical == null : "Physical component of PinRead can only be made once.";
		physical = new Physical();
		return physical;
	}

	@Override
	public Module getPhysicalComponent() {
		return physical;
	}

	public class Physical extends PhysicalImplementationModule {
		@SuppressWarnings("unused")
		private Port addressPort;
		@SuppressWarnings("unused")
		private Bus dataBus;
		private Bus sideAddressBus;
		private Port sideDataPort;

		private Physical() {
			super(0);

			// one normal port for the address
			Port addressPort = makeDataPort();
			Port pinReadAddress = PinRead.this.getDataPorts().get(0);
			assert (pinReadAddress.getBus() != null) : "PinRead's address port not attached to a bus.";
			assert (pinReadAddress.getBus().getValue() != null) : "PinRead address port has no value";
			{
				Bus readAddrBus = pinReadAddress.getBus();
				addressPort.getPeer().setSize(readAddrBus.getSize(),
						readAddrBus.getValue().isSigned());
			}
			addressPort.setUsed(pinReadAddress.isUsed());
			addressPort.setBus(pinReadAddress.getBus());

			// appropriate the data out bus
			Exit physicalExit = makeExit(0);
			Bus dataBus = physicalExit.makeDataBus();
			Bus pinRead_data = PinRead.this.getExit(Exit.DONE).getDataBuses()
					.get(0);
			final int dataWidth = pinRead_data.getValue().getSize();
			dataBus.setUsed(pinRead_data.isUsed());
			dataBus.setIDLogical(ID.showLogical(pinRead_data));
			dataBus.setSize(dataWidth, true);

			for (Port consumer : pinRead_data.getPorts()) {
				consumer.setBus(dataBus);
			}

			sideDataPort = makeDataPort(Component.SIDEBAND);
			sideDataPort.getPeer().setSize(dataWidth, true);

			sideAddressBus = physicalExit.makeDataBus(Component.SIDEBAND);
			sideAddressBus.setIDLogical(ID.showLogical(PinRead.this) + "_RA");
			{
				Bus readAddrBus = pinReadAddress.getBus();
				sideAddressBus.setSize(readAddrBus.getSize(), readAddrBus
						.getValue().isSigned());
			}

			/*
			 * Pin reads do not use go, clock, or reset, or done.
			 */
			getGoPort().setUsed(false);
			getClockPort().setUsed(false);
			getResetPort().setUsed(false);
			physicalExit.getDoneBus().setUsed(false);

			// now wire everything up
			sideAddressBus.getPeer().setBus(addressPort.getPeer());
			dataBus.getPeer().setBus(sideDataPort.getPeer());
		}

		public Port getSideDataPort() {
			return sideDataPort;
		}

		public Bus getSideAddressBus() {
			return sideAddressBus;
		}

		@Override
		public void accept(Visitor v) {
		}

		@Override
		public boolean removeDataBus(Bus bus) {
			assert false : "remove data bus not supported on " + this;
			return false;
		}

		@Override
		public boolean removeDataPort(Port port) {
			assert false : "remove data port not supported on " + this;
			return false;
		}

	}
}
