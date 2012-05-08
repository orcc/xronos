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

/**
 * A {@link Register} write access.
 * 
 * @version $Id: RegisterWrite.java 88 2006-01-11 22:39:52Z imiller $
 */
public class RegisterWrite extends Access implements StateAccessor {

	/** records whether this read is a signed memory read. */
	private boolean isSigned;

	RegisterWrite(Register register, boolean isSigned) {
		/*
		 * One port for the data.
		 */
		super(register, 1, register.isVolatile());

		this.isSigned = isSigned;

		getGoPort().setUsed(true);
		makeExit(0).setLatency(Latency.ONE);
	}

	/**
	 * Accept method for the Visitor interface
	 */
	@Override
	public void accept(Visitor visitor) {
		visitor.visit(this);
	}

	/**
	 * Returns the targetted Register which is a StateHolder object.
	 * 
	 * @return the targetted Register
	 */
	@Override
	public StateHolder getStateHolder() {
		return getRegister();
	}

	/**
	 * Returns true if the accessed register is stores a floating point value.
	 */
	@Override
	public boolean isFloat() {
		return getRegister().isFloat();
	}

	/**
	 * Returns true if this is a signed access to the backing register.
	 * 
	 * @return a value of type 'boolean'
	 */
	public boolean isSigned() {
		return isSigned;
	}

	public Port getDataPort() {
		return getDataPorts().get(0);
	}

	public Bus getSidebandWEBus() {
		if (getExit(Exit.SIDEBAND) == null) {
			return null;
		}
		// The ordering here MUST match the sizing applied in
		// makeSidebandConnections
		return getExit(Exit.SIDEBAND).getDataBuses().get(0);
	}

	public Bus getSidebandDataBus() {
		if (getExit(Exit.SIDEBAND) == null) {
			return null;
		}
		// The ordering here MUST match the sizing applied in
		// makeSidebandConnections
		return getExit(Exit.SIDEBAND).getDataBuses().get(1);
	}

	/**
	 * Creates the sideband data/control connections necessary to connect this
	 * Operation to the resource it targets.
	 */
	public void makeSidebandConnections() {
		assert getExit(Exit.SIDEBAND) == null : "Can only create sideband connections once "
				+ this;

		Exit exit = makeExit(0, Exit.SIDEBAND);
		Bus we = exit.makeDataBus(Component.SIDEBAND);
		we.setSize(1, true);

		Bus data = exit.makeDataBus(Component.SIDEBAND);
		data.setSize(getRegister().getInitWidth(), isSigned());
	}

	/**
	 * Tests whether this component requires a connection to its <em>go</em>
	 * {@link Port} in order to commence processing.
	 */
	@Override
	public boolean consumesGo() {
		return true;
	}

	/**
	 * Overwrites the method in {@link Component} to return <em>true</em> since
	 * the {@link RegisterWrite} operation needs a <em>go</em> and a
	 * <em>done</em>.
	 */
	public boolean isControlled() {
		return true;
	}

	/**
	 * This accessor modifies the {@link Referenceable} target state so it may
	 * not execute in parallel with other accesses.
	 */
	@Override
	public boolean isSequencingPoint() {
		return true;
	}

	/*
	 * =================================================== Begin new constant
	 * prop rules implementation.
	 */

	/**
	 * Pushes size, care, and constant information forward through this
	 * RegisterWrite from the Register's access Value.
	 * 
	 * @return a value of type 'boolean'
	 */
	@Override
	public boolean pushValuesForward() {
		boolean mod = false;

		if (getSidebandDataBus() != null) {
			Value goValue = getGoPort().getValue();
			Value weValue = new Value(1, getSidebandWEBus().getValue()
					.isSigned());
			weValue.setBit(0, goValue.getBit(0));
			mod |= getSidebandWEBus().pushValueForward(weValue);

			Value inValue = getDataPort().getValue();
			Value newValue = new Value(inValue.getSize(), isSigned());
			for (int i = 0; i < inValue.getSize(); i++) {
				newValue.setBit(i, inValue.getBit(i));
			}
			mod |= getSidebandDataBus().pushValueForward(newValue);
		}

		return mod;
	}

	/**
	 * Reverse partial constant prop on an RegisterWrite just updates the
	 * consumed Bus Value.
	 * 
	 * @return a value of type 'boolean'
	 */
	@Override
	public boolean pushValuesBackward() {
		boolean mod = false;

		Value newValue;
		if (getSidebandDataBus() == null) {
			newValue = new Value(getRegister().getInitWidth(), isSigned());
		} else {
			Value resValue = getSidebandDataBus().getValue();
			newValue = new Value(resValue.getSize(), isSigned());
			for (int i = 0; i < resValue.getSize(); i++) {
				Bit bit = resValue.getBit(i);
				// if (!bit.isCare() || bit.isConstant())
				if (!bit.isCare()) {
					newValue.setBit(i, Bit.DONT_CARE);
				}
			}
		}
		mod |= getDataPort().pushValueBackward(newValue);

		return mod;
	}

	/*
	 * End new constant prop rules implementation.
	 * =================================================
	 */

	/**
	 * Returns a copy of this RegisterWrite by creating a new register write off
	 * of the {@link Register} associated with this node. We create a new access
	 * instead of cloning because of the way that the Register stores references
	 * (not in Referent). Creating a new access correctly sets up the
	 * Referent/Reference relationship.
	 * 
	 * @return a RegisterWrite object.
	 * @exception CloneNotSupportedException
	 *                if an error occurs
	 */
	@Override
	public Object clone() throws CloneNotSupportedException {
		RegisterWrite clone = getRegister().makeWriteAccess(isSigned);
		copyComponentAttributes(clone);
		return clone;
	}

	/**
	 * Tests whether a given Bus is the sideband write enable Bus that was added
	 * by the global connector.
	 */
	@SuppressWarnings("unused")
	private boolean isWriteEnableBus(Bus bus) {
		// return getExit(Exit.SIDEBAND).getDataBuses().indexOf(bus) == 0;
		return bus != null && bus == getSidebandWEBus();
	}

	/**
	 * Tests whether a given Bus is the sideband data Bus that was added by the
	 * global connector.
	 */
	@SuppressWarnings("unused")
	private boolean isWriteDataBus(Bus bus) {
		// return getExit(Exit.SIDEBAND).getDataBuses().indexOf(bus) == 1;
		return bus != null && bus == getSidebandDataBus();
	}

	/**
	 * Just for convenience...
	 */
	private Register getRegister() {
		return (Register) getResource();
	}
}
