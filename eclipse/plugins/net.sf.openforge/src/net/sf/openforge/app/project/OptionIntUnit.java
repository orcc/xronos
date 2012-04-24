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
package net.sf.openforge.app.project;

import java.util.List;

import net.sf.openforge.app.NewJob;
import net.sf.openforge.app.OptionKey;

/**
 * An option which allows an integer to be used for the value, with units picked
 * from a pre-defined set of possible strings.
 * 
 * @see net.sf.openforge.app.project.Option
 * @author Andreas Kollegger
 */
public class OptionIntUnit extends OptionPickOne {

	/**
	 * Constructs an Option of OptionIntUnit type.
	 * 
	 * @param key
	 *            the lookup-key for the option (its name)
	 * @param default_value
	 *            the default value related to the key
	 * @param possible_units
	 *            an array of all possible values this option can have
	 * @param hidden
	 *            whether this option should be hidden from the user
	 */
	public OptionIntUnit(OptionKey key, String default_value,
			String[] possible_units, boolean hidden) {
		super(key, default_value, possible_units, hidden);
	} // OptionInUnitt()

	/**
	 * Gets the valid values for this Option.
	 * 
	 * @return a Set of valid String values
	 */
	public String[] getPossibleUnits() {
		return getPossibleValues();
	}

	/**
	 * The expand method takes in a list of command line arguments and converts
	 * every option to "-opt label-key_pair=<value>" format.
	 * 
	 * @param tokens
	 *            list of command line arguments.
	 */
	@Override
	public void expand(List<String> tokens) {
		for (int i = 0; i < tokens.size(); i++) {
			String s = tokens.get(i).toString();
			String key = getOptionKey().getCLASwitch();
			if (s.charAt(0) == '-') {
				if (key.equals(s.substring(1))) {
					// replace the cla switch with -opt and set its value.
					s = "-opt";
					tokens.add(i, s);
					tokens.remove(i + 1);
					if (i < (tokens.size() - 2)) {
						String value1 = tokens.get(i + 1).toString();
						if (!this.isValid(value1)) {
							String value2 = tokens.get(i + 2).toString();
							value1 = value1 + value2;
							if (this.isValid(value1)) {
								s = getOptionKey().getKey() + "=" + value1;
								// remove first token
								tokens.remove(i + 1);
								// remove second token. Note that in the prev
								// step, the elements moved left by 1
								tokens.remove(i + 1);
								tokens.add(i + 1, s);
							} else {
								throw new NewJob.InvalidOptionValueException(
										key, value1);
							}
						} else {
							s = getOptionKey().getKey() + "=" + value1;
							tokens.remove(i + 1);
							tokens.add(i + 1, s);
						}
					} else {
						throw new NewJob.InvalidNumberOfArgsException();
					}
				}
			}
		}
	}

	/**
	 * This method checks to see if the value passed to the option is of the
	 * right type. If true, it sets the option to that value. Otherwise, it
	 * throws and InvalidOptionValue Exception.
	 * 
	 * @param slabel
	 *            SearchLabel that can be used to get the scope.
	 * @param val
	 *            value to the option.
	 */
	@Override
	public void setValue(SearchLabel slabel, Object val) {
		if (!this.isValid(val.toString())) {
			throw new NewJob.InvalidOptionValueException(getOptionKey()
					.getKey(), val.toString());
		} else {
			super.setValue(slabel, val, true);
		}
	}

	/**
	 * This method checks to see if the value passed to the option is of the
	 * right type. If true, it replaces the value of the option with that new
	 * value. Otherwise, it throws and InvalidOptionValue Exception.
	 * 
	 * @param slabel
	 *            SearchLabel that can be used to get the scope.
	 * @param val
	 *            value to the option.
	 */
	@Override
	public void replaceValue(SearchLabel slabel, Object val) {
		if (!this.isValid(val.toString())) {
			throw new NewJob.InvalidOptionValueException(getOptionKey()
					.getKey(), val.toString());
		} else {
			super.replaceValue(slabel, val, true);
		}
	}

	@Override
	public boolean isValid(String s) {
		// valid is either completely empty, or values for both fields
		if (s.length() == 0)
			return true;

		// check the integer component
		String intString = getIntValueString(s);
		if (intString.length() == 0) {
			return false;
		}

		// int int_value = getIntValue(s);

		String units = getUnits(s);
		// check the unit component
		if ((units != null) && (!units.equals(""))) {
			boolean valid = false;
			String[] possible_units = getPossibleUnits();
			for (int i = 0; i < possible_units.length; i++) {
				if (possible_units[i].equalsIgnoreCase(units)) {
					valid = true;
					break;
				}
			}
			return valid;
		} else {
			return false;
		}
	} // isValid()

	public static String getIntValueString(String s) {
		s = s.trim();

		// march through the string collecting all decimal numbers
		// until I find a non decimal digit

		int index = 0;

		String number = "";

		while ((index < s.length()) && Character.isDigit(s.charAt(index))) {
			number += s.charAt(index);
			index++;
		}

		return number;
	}

	public static int getIntValue(String s) {
		s = s.trim();

		int value = 0;

		String number = getIntValueString(s);

		// To allow an empty speed string, we can't throw a number
		// format exception if they didn't give us anything
		if (number.length() > 0) {
			try {
				value = Integer.parseInt(number);
			} catch (NumberFormatException nfe) {
			}
		}

		return value;
	}

	public String getUnits(String s) {
		s = s.trim();

		String units = "";

		int index = 0;

		// find the first non digit character
		while ((index < s.length()) && Character.isDigit(s.charAt(index))) {
			index++;
		}

		// collect up the units, which are whatever is left over
		while (index < s.length()) {
			units += s.charAt(index);
			index++;
		}

		// If the user put in a space between the number and unit, we
		// need to toss it.
		units = units.trim();

		// now search the valid units for a case insensitive match and
		// return it if you find it
		String[] possible_units = getPossibleUnits();
		for (int i = 0; i < possible_units.length; i++) {
			if (possible_units[i].equalsIgnoreCase(units)) {
				return (possible_units[i]);
			}
		}

		return units;
	}

	@Override
	public String getTypeName() {
		return "unit";
	}

} /* end class OptionIntUnit */
