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
 * An {@link OutPinBuf} write access. The write can be synchronous, in which
 * case it takes effect at the next clock edge, or immediate.
 * 
 * @author Stephen Edwards
 * @version $Id: PinWrite.java 88 2006-01-11 22:39:52Z imiller $
 */
public class PinWrite extends PinAccess {

	/** True if effective on the next clock edge; false if immediate */
	private boolean isSynchronous;

	public PinWrite(boolean isSynchronous) {
		// two ports -- the address (object reference) of the pin,
		// and the data
		super(2);
		this.isSynchronous = isSynchronous;
		makeExit(0);
	}

	public boolean isSynchronous() {
		return isSynchronous;
	}

	/**
	 * @return true
	 */
	@Override
	public boolean consumesGo() {
		return true;
	}

	/**
	 * Returns a copy of this PinWrite by creating a new access off of the
	 * {@link OutPinBuf} associated with this node. We create a new access
	 * instead of cloning because of the way that the OutPinBuf stores
	 * references (not in Referent). Creating a new access correctly sets up the
	 * Referent/Reference relationship.
	 * 
	 * @return a PinWrite object.
	 */
	@Override
	public Object clone() throws CloneNotSupportedException {
		PinWrite clone = (PinWrite) super.clone();
		copyComponentAttributes(clone);
		return clone;
	}

	/*
	 * =================================================== Begin new constant
	 * prop rules implementation.
	 */

	/**
	 * No bus to propagate value/size
	 * 
	 * @return A value of type 'boolean'
	 */
	@Override
	public boolean pushValuesForward() {
		return false;
	}

	/**
	 * The data in to the pin write should be sized to that of the the pin size
	 * is defined to be.
	 * 
	 * @return A value of type 'boolean'
	 */
	@Override
	public boolean pushValuesBackward() {
		/*
		 * The OutBuf identity is dynamic. The best we could do here is choose
		 * the max size of any possible OutBuf after the ObjectResolver has run.
		 * --SGE
		 */
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

	boolean physical_made = false;
	Physical physical = null;

	public Physical makePhysicalComponent() {
		assert (!physical_made) : "Physical component of PinWrite can only be made once.";
		physical_made = true;
		physical = new Physical();
		return physical;
	}

	@Override
	public Module getPhysicalComponent() {
		return physical;
	}

	/**
	 * The full physical implementation of a PinWrite. Physical provides
	 * explicit internal connections for ports and buses, and extra logic to
	 * trap the GO signal so that it can be paired with the returning DONE
	 * signal.
	 * <P>
	 */
	public class Physical extends PhysicalImplementationModule {
		Bus side_address;
		Bus side_enable;
		Bus side_data;

		/**
		 * Constructs a new Physical which appropriates all the port-bus
		 * connections of the PinWrite.
		 */
		public Physical() {
			super(0);

			// one normal port for the address
			Port address_in = makeDataPort();
			Port pinWrite_address = PinWrite.this.getDataPorts().get(0);
			assert (pinWrite_address.getBus() != null) : "PinWrite's address port not attached to a bus.";

			{
				Bus writeAddrBus = pinWrite_address.getBus();
				address_in.getPeer().setSize(writeAddrBus.getSize(),
						writeAddrBus.getValue().isSigned());
			}
			address_in.setUsed(pinWrite_address.isUsed());
			address_in.setBus(pinWrite_address.getBus());

			// and one normal port for the data
			Port data_in = makeDataPort();
			Port pinWrite_data = PinWrite.this.getDataPorts().get(1);
			assert (pinWrite_data.getBus() != null) : "PinWrite's data port not attached to a bus.";

			{
				Bus writeDataBus = pinWrite_data.getBus();
				data_in.getPeer().setSize(writeDataBus.getSize(),
						writeDataBus.getValue().isSigned());
			}
			data_in.setUsed(pinWrite_data.isUsed());
			data_in.setBus(pinWrite_data.getBus());

			// appropriate the go signal
			Port pinWrite_go = PinWrite.this.getGoPort();
			Port go = getGoPort();
			assert (pinWrite_go.getBus() != null) : "PinWrite's go port not attached to a bus.";
			go.setUsed(pinWrite_go.isUsed());
			go.setBus(pinWrite_go.getBus());

			Exit physical_exit = makeExit(0);

			side_address = physical_exit.makeDataBus(Component.SIDEBAND);
			side_address.setIDLogical(ID.showLogical(PinWrite.this) + "_WA");
			{
				Bus writeAddrBus = pinWrite_address.getBus();
				side_address.setSize(writeAddrBus.getSize(), writeAddrBus
						.getValue().isSigned());
			}

			side_data = physical_exit.makeDataBus(Component.SIDEBAND);
			side_data.setIDLogical(ID.showLogical(PinWrite.this) + "_WD");
			{
				Bus writeDataBus = pinWrite_data.getBus();
				side_data.setSize(writeDataBus.getSize(), writeDataBus
						.getValue().isSigned());
			}
			side_enable = physical_exit.makeDataBus(Component.SIDEBAND);
			side_enable.setIDLogical(ID.showLogical(PinWrite.this) + "_WE");
			side_enable.setSize(1, true);

			// now wire everything up
			side_enable.getPeer().setBus(go.getPeer());
			side_address.getPeer().setBus(address_in.getPeer());
			side_data.getPeer().setBus(data_in.getPeer());

			/*
			 * Clock, reset, and done are not used.
			 */
			getClockPort().setUsed(false);
			getResetPort().setUsed(false);
			physical_exit.getDoneBus().setUsed(false);
		}

		public Bus getSideDataBus() {
			return side_data;
		}

		public Bus getSideAddressBus() {
			return side_address;
		}

		public Bus getSideEnableBus() {
			return side_enable;
		}

		@Override
		public void accept(Visitor v) {
		}

		@Override
		public boolean removeDataBus(Bus bus) {
			assert false : "remove data port not supported on " + this;
			return false;
		}

		@Override
		public boolean removeDataPort(Port port) {
			assert false : "remove data port not supported on " + this;
			return false;
		}

	} // class Physical
}
