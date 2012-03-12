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
/* $Rev: 2 $ */
package net.sf.openforge.forge.api.sim.pin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

/**
 * 
 * This class is used to describe a contiguous set of pin data. For example, if
 * you want to set the next seven pin values as 1 through 7, you can do that by
 * doing:
 * 
 * <code><pre>
 * 
 * SequentialPinData spd=new SequentialPinData();
 * long[] valarray={ 1,2,3,4,5,6,7 };
 * spd.add(valarray);
 * 
 * </pre></code>
 * 
 * This has the effect of setting the next 7 clock ticks to 1 thru 7.
 * <p>
 * Additionally, more SequentialPinData objects can be added to string together
 * values.
 * <p>
 * 
 * This is a CONTIGUOUS representation of pin data; note that that
 * AbsolutePinData can also manipulate data via the set(tick,data) paradigm but
 * does not assure contiguous data as well.
 */
public class SequentialPinData extends PinData {

	private List<SignalValue> pinDataSequence = new ArrayList<SignalValue>();

	/**
	 * Create and empty data set
	 * 
	 */
	public SequentialPinData() {
	}

	/**
	 * Create a sequential, contiguous data set from an array of SignalValues
	 * 
	 * @param values
	 *            values to use
	 */
	public SequentialPinData(SignalValue[] values) {
		add(values);
	}

	/**
	 * Create a sequential, contiguous data set from an array of long's
	 * 
	 * @param values
	 *            values to use
	 */
	public SequentialPinData(long[] values) {
		add(values);
	}

	/**
	 * Create a sequential, contiguous data set from an array of float's
	 * 
	 * @param values
	 *            values to use
	 */
	public SequentialPinData(float[] values) {
		add(values);
	}

	/**
	 * Create a sequential, contiguous data set from an array of double's
	 * 
	 * @param values
	 *            values to use
	 */
	public SequentialPinData(double[] values) {
		add(values);
	}

	/**
	 * Create a sequential, contiguous data set from a string sequence
	 * 
	 * @param values
	 *            values to use
	 */
	public SequentialPinData(String valString) {
		add(valString);
	}

	/**
	 * Create a sequential, contiguous data set from another PinData type. The
	 * pin data type is treated as a contiguous data set.
	 * 
	 * @param pd
	 *            PinData reference to use
	 */
	public SequentialPinData(PinData pd) {
		add(pd);
	}

	/**
	 * Turns an Absolute data set into a Sequential one; "blank spots" are
	 * filled in with SignalValue.X
	 * 
	 * @param apd
	 *            AbsolutePinData set to use
	 */
	public SequentialPinData(AbsolutePinData apd) {
		set(apd);
	}

	/**
	 * Cycle count of elements (contiguos) in this data block
	 * 
	 * @return a value of type 'long'
	 */
	public int getCycleCount() {
		return (int) pinDataSequence.size();
	}

	/**
	 * Return value at particular cycle count.
	 * 
	 * @param clockTick
	 *            tick between 0 and getCycleCount() -1
	 * @return a value of type 'SignalValue'
	 */
	public SignalValue valueAt(int clockTick) {
		return (SignalValue) pinDataSequence.get((int) clockTick);
	}

	/**
	 * Empty this pin data set
	 * 
	 */
	public void clear() {
		pinDataSequence.clear();
	}

	/**
	 * Add another set of pin data to the end of this PinData
	 * 
	 * @param pinData
	 *            a value of type 'PinData'
	 */
	public void add(PinData pinData) {
		for (int i = 0; i < pinData.getCycleCount(); i++)
			add(pinData.valueAt(i));
	}

	/**
	 * Add a list of SignalValues to the end of this PinData
	 * 
	 * @param vals
	 *            a value of type 'SignalValue[]'
	 */
	public void add(SignalValue[] vals) {
		for (int i = 0; i < vals.length; i++)
			add(vals[i]);
	}

	/**
	 * Add a list of values as longs tot he end of this PinData
	 * 
	 * @param vals
	 *            a value of type 'long[]'
	 */
	public void add(long[] vals) {
		for (int i = 0; i < vals.length; i++)
			add(new SignalValue(vals[i]));
	}

	/**
	 * Add a list of values as floats to the end of this PinData
	 * 
	 * @param vals
	 *            a value of type 'float[]'
	 */
	public void add(float[] vals) {
		for (int i = 0; i < vals.length; i++)
			add(new SignalValue(vals[i]));
	}

	/**
	 * Add a list of values as doubles to the end of this PinData
	 * 
	 * @param vals
	 *            a value of type 'double[]'
	 */
	public void add(double[] vals) {
		for (int i = 0; i < vals.length; i++)
			add(new SignalValue(vals[i]));
	}

	/**
	 * Add a single SignalValue to the end of this PinData
	 * 
	 * @param val
	 *            a value of type 'SignalValue'
	 */
	public void add(SignalValue val) {
		pinDataSequence.add(val);
	}

	/**
	 * Add long value to the end of this PinData
	 * 
	 * @param val
	 *            a value of type 'long'
	 */
	public void add(long val) {
		add(new SignalValue(val));
	}

	/**
	 * Add float value to the end of this PinData
	 * 
	 * @param val
	 *            a value of type 'float'
	 */
	public void add(float val) {
		add(new SignalValue(val));
	}

	/**
	 * Add double value to the end of this PinData
	 * 
	 * @param val
	 *            a value of type 'double'
	 */
	public void add(double val) {
		add(new SignalValue(val));
	}

	/**
	 * Add zero or more values to the end of this PinData
	 * 
	 * @param s
	 *            a space delimited sequence of values such "x z 0x10 0b11"
	 */
	public void add(String s) {
		this.add(SignalValue.parse(s));
	}

	/**
	 * Sets an arbitrary value at a particular clock tick. If the size of the
	 * data set needs to be extended, X's will be used to fill in the
	 * intervening space.
	 * 
	 * @param clockTick
	 *            0-N clock tick
	 * @param sv
	 *            value
	 */
	public void set(int clockTick, SignalValue sv) {
		int count = pinDataSequence.size();
		for (int i = count; i <= clockTick; i++) {
			add(SignalValue.X); // add unknwons
		}
		pinDataSequence.set(clockTick, sv);
	}

	/**
	 * Convenience method to use a long
	 * 
	 * @param clockTick
	 *            clock tick
	 * @param value
	 *            value
	 */
	public void set(int clockTick, long value) {
		set(clockTick, new SignalValue(value));
	}

	/**
	 * Convenience method to use a float
	 * 
	 * @param clockTick
	 *            clock tick
	 * @param value
	 *            value
	 */
	public void set(int clockTick, float value) {
		set(clockTick, new SignalValue(value));
	}

	/**
	 * Convenience method to use a double
	 * 
	 * @param clockTick
	 *            clock tick
	 * @param value
	 *            value
	 */
	public void set(int clockTick, double value) {
		set(clockTick, new SignalValue(value));
	}

	/**
	 * Set a sequence of values starting at the specified clock tick
	 * 
	 * @param clockTick
	 *            clock tick
	 * @param pd
	 *            SequentialPinData list
	 */
	public void set(int clockTick, SequentialPinData pd) {
		for (int i = 0; i < pd.getCycleCount(); i++) {
			set(i + clockTick, pd.valueAt(i));
		}
	}

	/**
	 * Merge an AbsolutePinData set into this data set
	 * 
	 * @param apd
	 *            a value of type 'AbsolutePinData'
	 */
	public void set(AbsolutePinData apd) {
		set(0, apd);
	}

	/**
	 * Merge an AbsolutePinData set into this data set.
	 * 
	 * @param clockTick
	 *            a value of type 'int'
	 * @param apd
	 *            a value of type 'AbsolutePinData'
	 */
	public void set(int clockTick, AbsolutePinData apd) {
		for (Entry<Integer, SignalValue> me : apd.getMapping().entrySet()) {
			int tick = ((Integer) me.getKey()).intValue();
			SignalValue sv = (SignalValue) me.getValue();
			set(tick, sv);
		}
	}
}
