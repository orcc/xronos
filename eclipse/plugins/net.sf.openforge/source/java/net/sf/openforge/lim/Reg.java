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

import java.util.Collections;
import java.util.Map;

import net.sf.openforge.app.OptionRegistry;
import net.sf.openforge.app.project.OptionBoolean;
import net.sf.openforge.report.FPGAResource;

/**
 * Reg models a register and can be constructed to match any of the
 * configurations which are possible in a Xilinx FPGA. Specific accessor methods
 * are available to retrieve the data, enable, set, reset, clear, and preset
 * ports. It's exit specifies that the Latency of this component is Latency.ONE.
 * 
 * A Reg object always has ports for sync Set, sync Reset, Enable async Preset,
 * async Clear. Depending on the desired configuration (immutably specified
 * during construction) certain ports will be marked used and the others will be
 * marked unused.
 * 
 * <pre>
 * port index         value           behavior
 *     0              data           data input
 *     1              sync set       sets ALL bits of reg to 1 @ next edge
 *     2              sync reset     clears ALL bits @ next edge
 *     3              enable         enables data capture at next edge
 * The async and sync versions of set/reset are now the same port
 * since their behavior is non-overlapping
 * <strike>
 *     4              async preset   sets ALL bits of reg on assertion
 *     5              async clear    clears ALL bits of reg on assertion
 * </strike>
 * </pre>
 * 
 * Created: Fri May 3 15:09:27 2002
 * 
 * @author imiller
 * @version $Id: Reg.java 282 2006-08-14 21:25:33Z imiller $
 */
public class Reg extends Primitive implements Emulatable {
	private static final String _RCS_ = "$Rev: 282 $";
	public static final int SET = 0x1;
	public static final int RESET = 0x2;
	public static final int ENABLE = 0x4;
	public static final int PRESET = 0x8;
	public static final int CLEAR = 0x10;

	/** simple register */
	public static final int REG = 0;
	/** sync register with set port */
	public static final int REGS = SET;
	/** async register with preset port */
	public static final int REGP = PRESET;
	/** enabled sync register with set port */
	public static final int REGSE = SET | ENABLE;
	/** enabled async register with preset port */
	public static final int REGPE = PRESET | ENABLE;
	/** sync register with reset port */
	public static final int REGR = RESET;
	/** async register with clear port */
	public static final int REGC = CLEAR;
	/** enabled sync register with reset port */
	public static final int REGRE = RESET | ENABLE;
	/** enabled async register with clear port */
	public static final int REGCE = CLEAR | ENABLE;
	/** sync register with set & reset ports */
	public static final int REGRS = RESET | SET;
	/** async register with preset & clear port */
	public static final int REGCP = CLEAR | PRESET;
	/** enabled sync register with set & reset ports */
	public static final int REGRSE = RESET | SET | ENABLE;
	/** enabled async register with preset & clear port */
	public static final int REGCPE = CLEAR | PRESET | ENABLE;
	/** enabled async register */
	public static final int REGE = ENABLE;

	private int regState;
	private Value initialValue;

	// If set to true then the type of reset (async vs sync) may be
	// switched during translation based on configuration settings.
	private boolean isConfigurable = false;

	// Identifies whether this Reg is 'doneSynchronous'. Set true by
	// the LatencyTracker if this Reg is part of the control chain.
	private boolean isSyncDone = false;

	/*
	 * If set to false then the isGroupable method will always return false.
	 */
	private boolean mayBeGrouped = true;

	private boolean hardInstantiate = false;

	/**
	 * Constructs a new Reg object which is of type {@link Reg#REG}.
	 */
	public Reg() {
		this(Reg.REG, null);
	}

	/**
	 * Constructs a new Reg of the specified type, where the type defines what
	 * ports are marked as used.
	 * 
	 * @param type
	 *            an int, one of the specified types in {@link Reg}
	 * @param id
	 *            a String to use for the id of this Reg, or null if none
	 *            specified.
	 */
	public Reg(int type, String id) {
		super(4);

		this.regState = type;

		if (((type & RESET) != 0) && ((type & CLEAR) != 0))
			throw new IllegalArgumentException(
					"Cannot have a reg that is both RESET and CLEAR");
		if (((type & SET) != 0) && ((type & PRESET) != 0))
			throw new IllegalArgumentException(
					"Cannot have a reg that is both SET and PRESET");

		// set the ports unused/used state based on the type
		for (int i = 1; i < 4; i++) {
			boolean state = false;
			if ((i == 1) && ((type & (PRESET | SET)) != 0))
				state = true;
			else if ((i == 2) && ((type & (RESET | CLEAR)) != 0))
				state = true;
			else if ((i == 3) && ((type & ENABLE) != 0))
				state = true;
			((Port) getDataPorts().get(i)).setUsed(state);
		}

		// the clock is always used, and must be connected by the user
		getClockPort().setUsed(true);

		getExit(Exit.DONE).setLatency(Latency.ONE);

		if (id != null) {
			setIDLogical(id);
			getResultBus().setIDLogical(id);
		}
	}

	public static Reg getConfigurableReg(int type, String id) {
		Reg reg = new Reg(type, id);
		reg.isConfigurable = true;
		return reg;
	}

	public void setHardInstantiate(boolean value) {
		this.hardInstantiate = value;
	}

	public boolean hardInstantiate() {
		return this.hardInstantiate;
	}

	/**
	 * <code>updateResetType</code> is called by the translation engine to cause
	 * this register to update its type of reset/set based on its context and
	 * the configuration settings. The type of reset (async vs sync) is
	 * determined by the options {@link OptionRegistry#SYNC_RESET}.
	 */
	public void updateResetType() {
		if (!isConfigurable) {
			return;
		}

		final boolean isSync = ((OptionBoolean) this.getGenericJob().getOption(
				OptionRegistry.SYNC_RESET)).getValueAsBoolean(this
				.getSearchLabel());

		int type = getType();
		// RESET and SET are synchronous
		// CLEAR and PRESET are asynchronous
		if (isSync) {
			if ((type & CLEAR) != 0) {
				type &= ~CLEAR;
				type |= RESET;
			}
			if ((type & PRESET) != 0) {
				type &= ~PRESET;
				type |= SET;
			}
		} else {
			if ((type & RESET) != 0) {
				type &= ~RESET;
				type |= CLEAR;
			}
			if ((type & SET) != 0) {
				type &= ~SET;
				type |= PRESET;
			}
		}

		this.regState = type;
	}

	/**
	 * Sets the initial value for this register, which is the value that this
	 * register will contain after programming, and any time the RESET port is
	 * asserted. Default value for power up and/or reset behavior is 0.
	 * 
	 * @param value
	 *            a non-null 'Value'
	 * @throws IllegalArgumentException
	 *             if 'value' is null.
	 */
	public void setInitialValue(Value value) {
		if (value == null)
			throw new IllegalArgumentException(
					"Illegal null initial value for reg");
		this.initialValue = value;
	}

	/**
	 * Gets the data {@link Port Port} which is the 0'th port in the
	 * getDataPorts() list.
	 * 
	 * @return a value of type 'Port'
	 */
	public Port getDataPort() {
		return (Port) getDataPorts().get(0);
	}

	/**
	 * gets the sync set port
	 */
	public Port getSetPort() {
		return (Port) getDataPorts().get(1);
	}

	/** gets the sync reset port */
	public Port getInternalResetPort() {
		return (Port) getDataPorts().get(2);
	}

	/** gets the enable port */
	public Port getEnablePort() {
		return (Port) getDataPorts().get(3);
	}

	/**
	 * Tests whether this component requires a connection to its clock
	 * {@link Port}.
	 */
	public boolean consumesClock() {
		return true;
	}

	/**
	 * Tests whether this component requires a connection to its reset
	 * {@link Port}. Returns true if this Reg has a 'reset' value or was
	 * constructed to consume reset.
	 */
	public boolean consumesReset() {
		return ((getType() & (RESET | CLEAR)) != 0);
	}

	/**
	 * Allows this register to be define whether it is done synchronous, which
	 * it is if it is part of the control chain of the module that contains it.
	 */
	public void setIsSyncDone(boolean val) {
		this.isSyncDone = val;
	}

	/**
	 * Overrides isDoneSynchronous in Component to return true if this Reg is
	 * part of the control chain, in which case it is producing a Done which is
	 * synchronous.
	 */
	public boolean isDoneSynchronous() {
		return this.isSyncDone;
	}

	/**
	 * Returns the value which is stored in this register after programming of
	 * the device and subsequent to reset, ie the initial value. The SET port
	 * will return the register to the bitwise inverted value.
	 * 
	 * @return a non-null Value
	 */
	public Value getInitialValue() {
		return initialValue;
	}

	/**
	 * looks at each port to decide which are used.
	 * 
	 * @return a constant describing the type of register (see constants defined
	 *         above)
	 */
	public int getType() {
		validate();
		return this.regState;
	}

	/**
	 * Implemented for the {@link Visitable} interface.
	 */
	public void accept(Visitor v) {
		v.visit(this);
	}

	/**
	 * Sets whether or not this Reg can be considered for grouping into an
	 * SRL16, but cannot force the reg to be grouped.
	 * 
	 * @param value
	 *            a value of type 'boolean'
	 */
	public void setGroupableFlag(boolean value) {
		this.mayBeGrouped = value;
	}

	/**
	 * Rules of testing whether this Reg can be qualified for grouping into a
	 * SRL16.
	 */
	public boolean isGroupable() {
		return this.mayBeGrouped;
	}

	/**
	 * Gets the FPGA hardware resource usage of this component.
	 * 
	 * @return a FPGAResource objec
	 */
	public FPGAResource getHardwareResourceUsage() {
		int flopCount = 0;

		Value inputValue = getDataPort().getValue();
		for (int i = 0; i < inputValue.getSize(); i++) {
			Bit inputBit = inputValue.getBit(i);
			if (inputBit.isCare()) {
				flopCount++;
			}
		}

		FPGAResource hwResource = new FPGAResource();
		hwResource.addFlipFlop(flopCount);

		return hwResource;
	}

	/**
	 * Performes a high level numerical emulation of this component.
	 * 
	 * @param portValues
	 *            a map of {@link Port} to
	 *            {@link net.sf.openforge.util.SizedInteger} input value
	 * @return a map of {@link Bus} to
	 *         {@link net.sf.openforge.util.SizedInteger} result value
	 */
	public Map emulate(Map portValues) {
		return Collections.singletonMap(getResultBus(),
				portValues.get(getDataPort()));
	}

	/*
	 * =================================================== Begin new constant
	 * prop rules implementation.
	 */

	/**
	 * Bit states propagate through after being unioned with the reset value (if
	 * any), except that pass through bits become care bits.
	 */
	public boolean pushValuesForward() {
		boolean mod = false;

		// We need to take reset value into account
		Value drivenValue = getDataPort().getValue();
		Value newValue;

		// If the register has an initial value or reset value
		// specified, it MUST be accounted for in the 'driven' value.
		if (this.initialValue != null) {
			int newSize = Math.max(this.initialValue.getSize(),
					drivenValue.getSize());
			newValue = new Value(newSize, drivenValue.isSigned()
					&& this.initialValue.isSigned());
			Bit drivenExtendBit = drivenValue.isSigned() ? drivenValue
					.getBit(drivenValue.getSize() - 1) : Bit.ZERO;
			Bit initExtendBit = this.initialValue.isSigned() ? this.initialValue
					.getBit(this.initialValue.getSize() - 1) : Bit.ZERO;

			for (int i = 0; i < newValue.getSize(); i++) {
				Bit b1 = ((i >= drivenValue.getSize()) ? drivenExtendBit
						: drivenValue.getBit(i));
				Bit b2 = (i >= this.initialValue.getSize() ? initExtendBit
						: this.initialValue.getBit(i));
				if (b1 == b2) { // otherwise, leave it as a care bit.
					newValue.setBit(i, b1);
				}
			}
		} else {
			newValue = drivenValue;
		}

		// Determine which bit is the true MSB
		int minSize = newValue.getCompactedSize();

		// Eliminate any pass through bits, only constant bits may
		// 'passthrough' a register
		newValue = Value.getGenericValue(newValue);

		// Now, to preserve sign extension, we need to do a bit of
		// trickery here. We cannot downsize a register, but we can
		// replicate the true MSB bit (as determined from the original
		// 'newValue' before we made it generic) from the register
		// output. Note that it is possible that the first time
		// through constant propagation that the result bus may not
		// yet have a Value from which to obtain the MSB bit.
		if (minSize < newValue.getSize() && getResultBus().getValue() != null) {
			assert minSize > 0 : "Illegal minimum size found for register.  Must be at least 1 bit wide.";
			Bit resultMSB = getResultBus().getValue().getBit(minSize - 1);
			for (int i = minSize; i < newValue.getSize(); i++) {
				newValue.setBit(i, resultMSB);
			}
		}

		mod |= getResultBus().pushValueForward(newValue);

		// Don't neglect the set Port.
		final Port setPort = getSetPort();
		if (setPort.isUsed() && setPort.isConnected()) {
			setPort.setBus(setPort.getBus());
			setPort.pushValueForward();
		}

		return mod;
	}

	/**
	 * Any bit of the consumed output that is dont care becomes a dont care on
	 * the inputs, all other bits are care.
	 */
	public boolean pushValuesBackward() {
		return getDataPort().pushValueBackward(getResultBus().getValue());
	}

	/*
	 * End new constant prop rules implementation.
	 * =================================================
	 */

	public String toString() {
		String ret = super.toString();
		return ret;
	}

	private void validate() {
		// Determine that the constructed type state matches the
		// 'used' of the pins.
		if (!getDataPort().isUsed())
			throw new IllegalRegisterConfiguration(
					"Data Port must be used for Reg");
		if (getSetPort().isUsed() != ((regState & (PRESET | SET)) != 0))
			throw new IllegalRegisterConfiguration(
					"Inconsistent internal state for Set Port of Reg "
							+ this.hashCode());
		if (getInternalResetPort().isUsed() != ((regState & (RESET | CLEAR)) != 0))
			throw new IllegalRegisterConfiguration(
					"Inconsistent internal state for Reset/Clear Port of Reg "
							+ this.hashCode());
		if (getEnablePort().isUsed() != ((regState & ENABLE) != 0))
			throw new IllegalRegisterConfiguration(
					"Inconsistent internal state for Enable Port of Reg "
							+ this.hashCode());
		// if (getPresetPort().isUsed() != ((regState & (PRESET|SET)) != 0))
		// throw new
		// IllegalRegisterConfiguration("Inconsistent internal state for Set/Preset port of Reg "
		// + this.hashCode());
		// if (getClearPort().isUsed() != ((regState & (RESET|CLEAR)) != 0))
		// throw new
		// IllegalRegisterConfiguration("Inconsistent internal state for Clear Port of Reg "
		// + this.hashCode());

		// Ensure that we aren't both synchronous AND asynchronous
		int isAsync = (regState & CLEAR) | (regState & PRESET);
		int isSync = (regState & SET) | (regState & RESET);
		if (isAsync != 0 && isSync != 0) {
			throw new IllegalRegisterConfiguration(
					"Inconsistent internal state Reg " + this.hashCode()
							+ " it cannot be both synchronous and asynchronous");
		}
	}

	class IllegalRegisterConfiguration extends RuntimeException {
		public IllegalRegisterConfiguration(String reason) {
			super(reason);
		}
	}

}// Reg
