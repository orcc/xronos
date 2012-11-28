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
package org.xronos.openforge.app.project;

import org.xronos.openforge.app.OptionKey;

/**
 * An Option which accepts integer values.
 * 
 * @author Andreas Kollegger
 */
public class OptionRange extends OptionInt {

	int lowest = 0;
	int highest = 0;

	public OptionRange(OptionKey key, int default_value, int lowest,
			int highest, boolean hidden) {
		super(key, default_value, hidden);
		this.lowest = lowest;
		this.highest = highest;
	} // OptionRange()

	/**
	 * Returns the lowest valid value which this range will accept.
	 * 
	 * @return the lowest valid value
	 */
	public int getLowest() {
		return lowest;
	}

	/**
	 * Returns the highest valid value which this range will accept.
	 * 
	 * @return the highest valid value
	 */
	public int getHighest() {
		return highest;
	}

	@Override
	public boolean isValid(String s) {
		int value;
		try {
			Integer valid = Integer.valueOf(s);
			value = valid.intValue();
		} catch (NumberFormatException nfe) {
			return false;
		}

		if ((value < lowest) || (value > highest)) {
			return false;
		}

		return true;

	} // isValid()

	@Override
	public String getTypeName() {
		return "range";
	}

} /* end class OptionRange */
