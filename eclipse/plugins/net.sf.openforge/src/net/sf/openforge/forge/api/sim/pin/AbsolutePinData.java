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

import java.util.Map;
import java.util.TreeMap;

/**
 * 
 * This class is used to describe a set of pin data. For example, in a
 * constructor if you want to set pin values at clock ticks 7, 12 and 15, you
 * can do that by doing:
 * 
 * <code><pre>
 * 
 * PinIn pin=new PinIn("PIN",10);
 * AbsolutePinData apd=new AbsolutePinData();
 * apd.set(7,0x10);
 * apd.set(12,0x11);
 * apd.set(15,0x12);
 * PinSimData.setDriveData(pin,apd);
 * 
 * </pre></code>
 * 
 * This has the effect of setting clock ticks 0-6 as X/Don'tCare, 7 as 0x10,
 * 8-11 as X/Don'tCare, 12 as 0x11, 13-14 as X/Don'tCare, and 15 as 0x12.
 * <p>
 * 
 * This is a SPARSE representation of pin data, note that that SequentialPinData
 * can also manipulate data via the set(tick,data) paradigm but assures
 * contiguous data as well
 * <p>
 * 
 * Be advised that this data set is only as long as the last element specified
 * <p>
 */
public class AbsolutePinData extends PinData {

	private Map<Integer, SignalValue> pinDataMap = new TreeMap<Integer, SignalValue>();

	/**
	 * Construct an empty data set
	 * 
	 */
	public AbsolutePinData() {
	}

	/**
	 * Construct a data set with one value at the specified clock tick. Prior
	 * values will be SignalValue.X
	 * 
	 * @param clockTick
	 *            clock tick to set value at
	 * @param value
	 *            value to use
	 */
	public AbsolutePinData(int clockTick, long value) {
		this(clockTick, new SignalValue(value));
	}

	/**
	 * Construct a data set with one value at the specified clock tick. Prior
	 * values will be SignalValue.X
	 * 
	 * @param clockTick
	 *            clock tick to set value at
	 * @param value
	 *            value to use
	 */
	public AbsolutePinData(int clockTick, float value) {
		this(clockTick, new SignalValue(value));
	}

	/**
	 * Construct a data set with one value at the specified clock tick. Prior
	 * values will be SignalValue.X
	 * 
	 * @param clockTick
	 *            clock tick to set value at
	 * @param value
	 *            value to use
	 */
	public AbsolutePinData(int clockTick, double value) {
		this(clockTick, new SignalValue(value));
	}

	/**
	 * Construct a data set with one value at the specified clock tick. Prior
	 * values will be SignalValue.X
	 * 
	 * @param clockTick
	 *            clock tick to set value at
	 * @param value
	 *            value to use
	 */
	public AbsolutePinData(int clockTick, SignalValue value) {
		set(clockTick, value);
	}

	/**
	 * Construct a data set using a SeqeuentialPinData stream provided with its
	 * first value starting at the specified clock tick. Prior values will be
	 * SignalValue.X
	 * 
	 * @param clockTick
	 *            clock tick to set value at
	 * @param spd
	 *            SequentialPinData to use
	 */
	public AbsolutePinData(int clockTick, SequentialPinData spd) {
		set(clockTick, spd);
	}

	/**
	 * Return the maximum cycle count of this data set
	 * 
	 * @return count
	 */
	@Override
	public int getCycleCount() {
		if (pinDataMap.size() == 0) {
			return 0;
		} else {
			return ((TreeMap<Integer, SignalValue>) pinDataMap).lastKey()
					.intValue() + 1;
		}
	}

	/**
	 * Get the value at the specified count
	 * 
	 * @param clockTick
	 *            clock tick whose value you need
	 * @return value
	 */
	@Override
	public SignalValue valueAt(int clockTick) {
		Integer temp = new Integer(clockTick);
		if (pinDataMap.containsKey(temp)) {
			return pinDataMap.get(temp);
		} else {
			return SignalValue.X;
		}
	}

	/**
	 * Empty this data set
	 * 
	 */
	@Override
	public void clear() {
		pinDataMap.clear();
	}

	/**
	 * Set a specific value at a specific clock tick. This overrides an existing
	 * value. This may extend the size of this data set; intervening clock ticks
	 * will be filled in with SignalValue.X
	 * 
	 * @param clockTick
	 *            clock tick to set data to start at
	 * @param value
	 *            value to set
	 */
	public void set(int clockTick, SignalValue value) {
		pinDataMap.put(new Integer(clockTick), value);
	}

	/**
	 * Set a specific value at a specific clock tick. This overrides an existing
	 * value. This may extend the size of this data set; intervening clock ticks
	 * will be filled in with SignalValue.X
	 * 
	 * @param clockTick
	 *            clock tick to set data to start at
	 * @param value
	 *            value to set
	 */
	public void set(int clockTick, long value) {
		set(clockTick, new SignalValue(value));
	}

	/**
	 * Set a specific value at a specific clock tick. This overrides an existing
	 * value. This may extend the size of this data set; intervening clock ticks
	 * will be filled in with SignalValue.X
	 * 
	 * @param clockTick
	 *            clock tick to set data to start at
	 * @param value
	 *            value to set
	 */
	public void set(int clockTick, float value) {
		set(clockTick, new SignalValue(value));
	}

	/**
	 * Set a specific value at a specific clock tick. This overrides an existing
	 * value. This may extend the size of this data set; intervening clock ticks
	 * will be filled in with SignalValue.X
	 * 
	 * @param clockTick
	 *            clock tick to set data to start at
	 * @param value
	 *            value to set
	 */
	public void set(int clockTick, double value) {
		set(clockTick, new SignalValue(value));
	}

	/**
	 * Set a specific sequence of values starting at a specific clock tick. This
	 * overrides an existing value. This may extend the size of this data set;
	 * intervening clock ticks will be filled in with SignalValue.X
	 * 
	 * @param clockTick
	 *            clock tick to set data to start at
	 * @param value
	 *            value to set
	 */
	public void set(int clockTick, SequentialPinData spd) {
		for (int i = 0; i < spd.getCycleCount(); i++) {
			set(i + clockTick, spd.valueAt(i));
		}
	}

	/**
	 * Merge in another AbsolutePinMapping with this one.
	 * 
	 * @param apd
	 *            absolute pin data to merge in
	 */
	public void set(AbsolutePinData apd) {
		pinDataMap.putAll(apd.getMapping());
	}

	public Map<Integer, SignalValue> getMapping() {
		return pinDataMap;
	}

}
