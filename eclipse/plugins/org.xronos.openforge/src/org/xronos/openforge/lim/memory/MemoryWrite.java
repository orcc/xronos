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
package org.xronos.openforge.lim.memory;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.xronos.openforge.lim.Bus;
import org.xronos.openforge.lim.Component;
import org.xronos.openforge.lim.Exit;
import org.xronos.openforge.lim.Module;
import org.xronos.openforge.lim.PhysicalImplementationModule;
import org.xronos.openforge.lim.Port;
import org.xronos.openforge.lim.Referenceable;
import org.xronos.openforge.lim.StateAccessor;
import org.xronos.openforge.lim.StateHolder;
import org.xronos.openforge.lim.Value;
import org.xronos.openforge.lim.Visitor;
import org.xronos.openforge.lim.primitive.And;
import org.xronos.openforge.lim.primitive.Or;
import org.xronos.openforge.lim.primitive.Reg;
import org.xronos.openforge.util.naming.ID;


/**
 * A {@link MemoryPort} write access.
 * 
 * @author Stephen Edwards
 * @version $Id: MemoryWrite.java 280 2006-08-11 17:00:32Z imiller $
 */
public class MemoryWrite extends MemoryAccess implements StateAccessor {

	/** The full physical implementation of the MemoryWrite. */
	Physical physical = null;

	public MemoryWrite(boolean isVolatile, int width, boolean isSigned) {
		/*
		 * One port for the address, one for the data.
		 */
		super(2, isVolatile, isSigned, width);
		getGoPort().setUsed(true);
		makeExit(0);
	}

	public Port getDataPort() {
		return getDataPorts().get(1);
	}

	/**
	 * Returns the targetted Register which is a StateHolder object.
	 * 
	 * @return the targetted Register
	 */
	@Override
	public StateHolder getStateHolder() {
		return getMemoryPort().getLogicalMemory();
	}

	/**
	 * Performs forward constant propagation through this component. This
	 * component will fetch the incoming {@link Value} from each {@link Port}
	 * using {@link Port#_getValue()}. It will then compute a new outgoing
	 * {@link Value} for each {@link Bus} and set it with
	 * {@link Bus#pushValueForward(Value)}.
	 * 
	 * @return true if any of the bus values was modified, false otherwise
	 */
	@Override
	protected boolean pushValuesForward() {
		/*
		 * The is really handled by the physical implementation.
		 */
		return false;
	}

	/**
	 * Performs reverse constant propagation inside through component. This
	 * component will fetch the incoming {@link Value} from each {@link Bus}
	 * using {@link Bus#_getValue()}. It will then compute a new outgoing
	 * {@link Value} for each {@link Port} and set it with
	 * {@link Port#pushValueBackward(Value)}.
	 * 
	 * @return true if any of the port values was modified, false otherwise
	 */
	@Override
	protected boolean pushValuesBackward() {
		/*
		 * This is really handled by the physical implementation.
		 */
		return false;
	}

	public Physical makePhysicalComponent() {
		assert (physical == null) : "MemoryWrite.physical already exists";
		physical = new Physical();
		return physical;
	}

	@Override
	public Module getPhysicalComponent() {
		return physical;
	}

	/**
	 * returns false
	 */
	@Override
	public boolean isReadAccess() {
		return false;
	}

	/**
	 * returns true
	 */
	@Override
	public boolean isWriteAccess() {
		return true;
	}

	/**
	 * Accept method for the Visitor interface
	 */
	@Override
	public void accept(Visitor visitor) {
		visitor.visit(this);
	}

	/**
	 * This accessor modifies the {@link Referenceable} target state so it may
	 * not execute in parallel with other accesses.
	 */
	@Override
	public boolean isSequencingPoint() {
		return true;
	}

	/**
	 * Returns a copy of this MemoryWrite by creating a new memory write off of
	 * the {@link MemoryPort} associated with this node. We create a new access
	 * instead of cloning because of the way that the MemoryPort stores
	 * references (not in Referent). Creating a new access correctly sets up the
	 * Referent/Reference relationship.
	 * 
	 * @return a MemoryWrite object.
	 */
	@Override
	public Object clone() {
		assert physical == null : "Cloning Physical not implemented";
		final MemoryWrite clone = new MemoryWrite(isVolatile(), getWidth(),
				isSigned());
		clone.setMemoryPort(getMemoryPort());
		copyComponentAttributes(clone);
		return clone;
	}

	/**
	 * The full physical implementation of a MemoryWrite. Physical provides
	 * explicit internal connections for ports and buses, and extra logic to
	 * trap the GO signal so that it can be paired with the returning DONE
	 * signal.
	 * <P>
	 * <img src="doc-files/MemoryWrite.png">
	 */
	public class Physical extends PhysicalImplementationModule {
		Port sideWriteFinished;
		Bus sideAddress;
		Bus sideEnable;
		Bus sideData;
		Reg goRegister;
		Bus sideSize;

		/**
		 * Constructs a new Physical which appropriates all the port-bus
		 * connections of the MemoryWrite.
		 */
		public Physical() {
			super(0);
			//
			// MAKE SURE THAT THE ORDERING OF PORTS HERE AGREES WITH
			// PORT ORDERING OF THE COMPONENT!
			//

			// one normal port for the address
			Port addressIn = makeDataPort();
			Port memWriteAddress = getAddressPort();
			assert (memWriteAddress.getBus() != null) : "MemoryWrite's address port not attached to a bus.";

			// addressIn.getPeer().setSize(memWriteAddress.getBus().getSize(),
			// memWriteAddress.getBus().getValue().isSigned());
			addressIn.setUsed(memWriteAddress.isUsed());
			addressIn.setBus(memWriteAddress.getBus());

			// and one normal port for the data
			Port dataIn = makeDataPort();
			Port memWriteData = getDataPort();
			assert (memWriteData.getBus() != null) : "MemoryWrite's address port not attached to a bus.";

			// and another normal port for the size input.
			Port sizeIn = makeDataPort();
			// sizeIn.getPeer().setSize(getSizePort().getBus().getSize(),
			// getSizePort().getBus().getValue().isSigned());
			sizeIn.setUsed(getSizePort().isUsed());
			sizeIn.setBus(getSizePort().getBus());

			// dataIn.getPeer().setSize(memWriteData.getBus().getValue().getSize(),
			// false);
			dataIn.setUsed(memWriteData.isUsed());
			dataIn.setBus(memWriteData.getBus());

			// appropriate the go signal
			Port memWriteGo = MemoryWrite.this.getGoPort();
			Port go = getGoPort();
			assert (memWriteGo.getBus() != null) : "MemoryWrite's go port not attached to a bus.";
			// assert (memWriteGo.getBus().getValue() != null) :
			// "Bus attached to MemoryWrite's go port has no value.";
			// go.getPeer().setValue(new Value(go.getPeer(),
			// memWriteGo.getBus().getValue()));
			go.setUsed(memWriteGo.isUsed());
			go.setBus(memWriteGo.getBus());

			// appropriate the clock signal
			Port memWriteClock = MemoryWrite.this.getClockPort();
			Port clk = getClockPort();
			assert (memWriteClock.getBus() != null) : "MemoryWrite's clock port not attached to a bus.";
			// assert (memWriteClock.getBus().getValue() != null) :
			// "Bus attached to MemoryWrite's clock port has no value.";
			// clk.getPeer().setValue(new Value(clk.getPeer(),
			// memWriteClock.getBus().getValue()));
			clk.setUsed(memWriteClock.isUsed());
			clk.setBus(memWriteClock.getBus());

			// appropriate the reset signal
			Port memWriteReset = MemoryWrite.this.getResetPort();
			Port reset = getResetPort();
			assert (memWriteReset.getBus() != null) : "MemoryWrite's reset port not attached to a bus.";
			// assert (memWriteReset.getBus().getValue() != null) :
			// "Bus attached to MemoryWrite's reset port has no value.";
			// reset.getPeer().setValue(new Value(reset.getPeer(),
			// memWriteReset.getBus().getValue()));
			reset.setUsed(memWriteReset.isUsed());
			reset.setBus(memWriteReset.getBus());

			// appropriate the done bus
			Exit physicalExit = makeExit(0);
			Bus done = physicalExit.getDoneBus();
			Bus memWriteDone = MemoryWrite.this.getExit(Exit.DONE).getDoneBus();
			done.setUsed(memWriteDone.isUsed());
			done.setIDLogical(ID.showLogical(memWriteDone));
			// done.setValue(new Value(done, memWriteDone.getValue()));
			for (Port consumer : memWriteDone.getPorts()) {
				consumer.setBus(done);
			}

			//
			// this is the problem. WHat is supposed to be wired to???
			// when it hit's the translator it (and the stuff that
			// refs it) has no value, etc...
			//
			sideWriteFinished = makeDataPort(Component.SIDEBAND);
			// sideWriteFinished.getPeer().setSize(1, true);

			sideAddress = physicalExit.makeDataBus(Component.SIDEBAND);
			sideAddress.setIDLogical(ID.showLogical(MemoryWrite.this) + "_WA");
			sideAddress.setSize(memWriteAddress.getBus().getSize(),
					memWriteAddress.getBus().getValue().isSigned());

			sideData = physicalExit.makeDataBus(Component.SIDEBAND);
			sideData.setIDLogical(ID.showLogical(MemoryWrite.this) + "_WD");
			sideData.setSize(memWriteData.getBus().getSize(), memWriteData
					.getBus().getValue().isSigned());

			sideEnable = physicalExit.makeDataBus(Component.SIDEBAND);
			sideEnable.setIDLogical(ID.showLogical(MemoryWrite.this) + "_WE");
			sideEnable.setSize(1, true);

			sideSize = physicalExit.makeDataBus(Component.SIDEBAND);
			sideSize.setIDLogical(ID.showLogical(MemoryWrite.this) + "_WS");
			sideSize.setSize(getSizePort().getBus().getSize(), getSizePort()
					.getBus().getValue().isSigned());
			sideSize.getPeer().setBus(sizeIn.getPeer());

			// create internal done-caching logic
			goRegister = Reg.getConfigurableReg(Reg.REGRS, null);
			Port goRegSet = goRegister.getSetPort();
			Port goRegIn = goRegister.getDataPort();
			Bus goRegOut = goRegister.getResultBus();
			goRegOut.setSize(1, true);
			addComponent(goRegister);

			And doneAnd = new And(2);
			List<Port> doneAndPorts = doneAnd.getDataPorts();
			Bus doneAndResult = doneAnd.getResultBus();
			addComponent(doneAnd);

			Or resetOr = new Or(2);
			List<Port> resetOrPorts = resetOr.getDataPorts();
			Bus resetOrBus = resetOr.getResultBus();
			addComponent(resetOr);

			// now wire everything up
			sideEnable.getPeer().setBus(go.getPeer());
			sideAddress.getPeer().setBus(addressIn.getPeer());
			sideData.getPeer().setBus(dataIn.getPeer());

			resetOrPorts.get(0).setBus(doneAndResult);
			resetOrPorts.get(1).setBus(reset.getPeer());

			/*
			 * XXX: It seems the internal reset is what's intended, but just to
			 * be safe, connect the regular reset as well.
			 */
			goRegister.getInternalResetPort().setBus(resetOrBus);
			goRegister.getResetPort().setBus(resetOrBus);

			goRegister.getClockPort().setBus(clk.getPeer());
			goRegIn.setBus(goRegOut);
			goRegSet.setBus(go.getPeer());

			doneAndPorts.get(0).setBus(goRegOut);
			doneAndPorts.get(1).setBus(sideWriteFinished.getPeer());

			done.getPeer().setBus(doneAndResult);
		}

		public Port getSideWriteFinishedPort() {
			return sideWriteFinished;
		}

		public Bus getSideDataBus() {
			return sideData;
		}

		public Bus getSideAddressBus() {
			return sideAddress;
		}

		public Bus getSideEnableBus() {
			return sideEnable;
		}

		public Bus getSideSizeBus() {
			return sideSize;
		}

		@Override
		public Set<Component> getFeedbackPoints() {
			return Collections.singleton((Component) goRegister);
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
