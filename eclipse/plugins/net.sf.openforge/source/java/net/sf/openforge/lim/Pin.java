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

import java.util.Collection;

import net.sf.openforge.forge.api.pin.Buffer;

/**
 * A pin of the {@link Design}. Note that a Pin is not a {@link Resource}.
 * Rather, it may contain an {@link InPinBuf}, an {@link OutPinBuf}, or both,
 * which are {@link Resource Resources}.
 * 
 * @author Stephen Edwards
 * @version $Id: Pin.java 282 2006-08-14 21:25:33Z imiller $
 */
public abstract class Pin extends Component {

	int width = 0;
	boolean isSigned = true;
	Buffer apiPin;

	/** the clock that controls this pin */
	// private InputPin clockPin = null;
	/** the reset that controls this pin */
	// private InputPin resetPin = null;

	/** The pin referee used to arbitrate all accesses to this pin. */
	private PinReferee referee = null;

	/**
	 * Construct a new Pin with specified width and signedness.
	 * 
	 * @param width
	 * @param isSigned
	 *            ;
	 */
	public Pin(int width, boolean isSigned) {
		this.width = width;
		this.isSigned = isSigned;
	}

	/**
	 * Returns true if this {@link Pin} was created from a user instantiated pin
	 * (external or IPCore).
	 * 
	 * @return a value of type 'boolean'
	 */
	protected boolean isUserCreatedPin() {
		return apiPin != null;
	}

	public InPinBuf getInPinBuf() {
		return null;
	}

	public OutPinBuf getOutPinBuf() {
		return null;
	}

	public Buffer getApiPin() {
		return apiPin;
	}

	public void setApiPin(Buffer b) {
		apiPin = b;
		b.setSize(getWidth());
	}

	public abstract Collection<? extends PinBuf> getPinBufs();

	public int getWidth() {
		return width;
	}

	/**
	 * Changes the bit width of this pin to be the specified value. Used by
	 * subclasses to update the pin size during constant prop.
	 */
	protected void setWidth(int value) {
		width = value;
	}

	public long getResetValue() {
		return apiPin.getResetValue();
	}

	public boolean isDriveOnReset() {
		return apiPin.getDriveOnReset();
	}

	@Override
	public boolean consumesClock() {
		if ((getInPinBuf() != null) && (getInPinBuf().consumesClock())) {
			return true;
		}
		if ((getOutPinBuf() != null) && (getOutPinBuf().consumesClock())) {
			return true;
		}
		return false;
	}

	@Override
	public boolean consumesReset() {
		if ((getInPinBuf() != null) && (getInPinBuf().consumesReset())) {
			return true;
		}
		if ((getOutPinBuf() != null) && (getOutPinBuf().consumesReset())) {
			return true;
		}
		return false;
	}

	public boolean hasWait() {
		return false;
	}

	@Override
	public void accept(Visitor visitor) {
		// assert (false) : "Pins should not be visited.";
		throw new UnexpectedVisitationException();
	}

	/*
	 * =================================================== Begin new constant
	 * prop rules implementation.
	 */

	/**
	 * Asserts false until rule is supported.
	 */
	@Override
	public boolean pushValuesForward() {
		assert false : "new pushValuesForward propagation of constants through "
				+ this.getClass() + " not yet supported";
		return false;
	}

	/**
	 * Asserts false until rule is supported.
	 */
	@Override
	public boolean pushValuesBackward() {
		assert false : "new pushValuesBackward propagation of constants through "
				+ this.getClass() + " not yet supported";
		return false;
	}

	/**
	 * Retrieves the {@link PinReferee} used to arbitrate all accesses to this
	 * pin.
	 */
	public PinReferee getReferee() {
		if (referee == null) {
			referee = new PinReferee(this);
		}
		return referee;
	}

	/*
	 * End new constant prop rules implementation.
	 * =================================================
	 */
}
